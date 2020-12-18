package com.rnctech.nrdataservice.service.pyimpl;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.google.gson.Gson;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.RNConsts.JOBTYPE;
import com.rnctech.nrdataservice.RNConsts.LOADTYPE;
import com.rnctech.nrdataservice.RNConsts.PROFILE_ENV;
import com.rnctech.nrdataservice.RNConsts.RNCTechImg;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.exception.APISessionException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.exception.TechException;
import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobInfo;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.service.IJobExecutor;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;

import com.rnctech.nrdataservice.service.RNResult.ResultMessage;
import com.rnctech.nrdataservice.service.RNResult.Type;
import com.rnctech.nrdataservice.utils.AWSUtil;
import com.rnctech.nrdataservice.utils.GInfo;
import com.rnctech.nrdataservice.utils.RNUtilities;
import com.rnctech.nrdataservice.utils.RNCLogOutputStream;
import com.rnctech.nrdataservice.utils.ShellUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Python Executor for provision new instance 
 * and run python code on new instance
 * 
 * @author zilin chen
 * @since 2020.09
 */

@Component
public class PythonRemoteExecutor extends RNJobExecutor { 
	private static final Logger logger = LoggerFactory.getLogger(PythonRemoteExecutor.class);
	public static final String PYEXETYPEKEY = "python.executor.resource.type";
	public static final String PYEXEMEMKEY = "python.executor.memory";
	public static final String PYEXECOREKEY = "python.executor.cores";
	protected int pullStatusInterval;
	protected long pyengineTimeout;
	JobRepository jobrepo;
	JobDetailsRepository jobdetailrepo;
	private RNCLogOutputStream outputStream;
	private AtomicBoolean pyEngineRunning = new AtomicBoolean(false);
	private AtomicBoolean pyEngineInitialized = new AtomicBoolean(false);
	private long sessionid = -1;
	private long remoteRunid = -1;
	private AmazonEC2 awsclient;
	private Instance instance;
	private String imgid = RNCTechImg.imgName;
	private String kname = RNCTechImg.pubkey;
	private String gname = RNCTechImg.securitygroup;
	private String pyEngineUrl = "";
	private boolean usessl = false;
	private PythonContext pyCtx;
	private RestTemplate restTemplate;
	private Map<String, String> customHeaders = new HashMap<>();
	
	@Value("${rnctech.profile:QA}")
    private String profile;
	
	@Value("${rnctech.version}")
    private String rnversion;
    
	public PythonRemoteExecutor(Properties property) {
		super(property);
	}

	@Override
	public void initProp(Properties property) {
		this.pullStatusInterval = Integer.parseInt(property.getProperty("python.pull_status.interval.millis", "600000"));  //10 mins
		this.pyengineTimeout = Integer.parseInt(property.getProperty("python.timeout.hours", "100")) * 3600000;
	}
	
	@Override
	public void open() throws TechException {
		try {
			super.initJobProperties();
			this.usessl = Boolean.parseBoolean(getProperty("http.useSSL", "false"));
			this.awsclient = AWSUtil.getEC2Client() ;
			provisionInstance(this.properties);
			startPyEngin();
		} catch (IOException e) {
			logger.error("Fail to open Pythoner", e);
			throw new TechException("Fail to open Pythoner", e);
		}
	}
	
	private void provisionInstance(Properties properties) {
		String ec2type = getEc2Type(properties);		
		if(profile.equalsIgnoreCase(PROFILE_ENV.PROD.name()) || profile.equalsIgnoreCase(PROFILE_ENV.PROD_QA.name())) {
			this.instance = AWSUtil.reqestInstance(awsclient, imgid, kname, gname, ec2type);
			String iid = AWSUtil.startInstance(awsclient, this.instance.getInstanceId());			
		}else {// dev or qa env, we assume instance already imaged and started
			this.instance = new Instance();
			this.instance.setImageId(RNCTechImg.instanceid);
			this.instance.setPrivateIpAddress(RNCTechImg.privateip);
		}
		
		if(null != this.instance && null != this.instance.getInstanceId()) {
			pyEngineInitialized.set(true);
			logger.info("Python engine initialized "+this.instance.getPrivateIpAddress());
		}else {
			logger.error("No instance initialized in env of "+profile);
		}			
	}
	
	private void startPyEngin() throws IOException {
		if(this.pyEngineInitialized.get()) {
			this.restTemplate = createRestTemplate();
			this.pyEngineUrl = "http://"+this.instance.getPrivateIpAddress()+":8082/exec/";
			//check service start or not
			String sinfourl = this.pyEngineUrl+"manage/info";
			int cc = 0;
			boolean started = false;
			while(!started && cc < 5) {
				try {
					String minfo = callRestAPI(restTemplate, null, sinfourl, "GET", "");
					Gson g = new Gson();
					GInfo prop = g.fromJson(minfo, GInfo.class);
					if(null != prop && null != prop.getBuild()) {
						String version = prop.getBuild().getVersion();
						if(!version.equalsIgnoreCase(rnversion)) {
							logger.info(String.format("Version not matched $s expected $s", version, rnversion));
						}
					}
					started = true;
				} catch (APIException e) {
					cc += 1;
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e1) {}
					logger.info("waiting service start on "+this.pyEngineUrl);
				}
			}
			
			if(started) {
				this.sessionid = genSessionId(this.instance);
				pyEngineRunning.set(true);
			}else {
				logger.error("No engine service running on "+ this.instance.getPrivateIpAddress());
			}
		}
		
	}
	
	private long genSessionId(Instance instance) {
		//return instance.getInstanceId()+"_"+instance.getPrivateIpAddress();
		return System.currentTimeMillis();
	}

	private String getEc2Type(Properties properties) {
		String instancetype = RNConsts.AWSINSTANCETYPE[0];
		String restype = properties.getProperty(PYEXETYPEKEY);
		if(null != restype) {
			LOADTYPE loadtype = LOADTYPE.MEDIUM;
			try {
				loadtype = LOADTYPE.valueOf(restype.trim().toUpperCase());
			}catch(Exception e) {}
			switch(loadtype) {
				case LARGE:{
					instancetype = RNConsts.AWSINSTANCETYPE[1];
					break;
				}
				case XLARGE:{
					instancetype = RNConsts.AWSINSTANCETYPE[2];
					break;
				}
				case XLARGE16:{
					instancetype = RNConsts.AWSINSTANCETYPE[3];
					break;
				}
				case XLARGE32:{
					instancetype = RNConsts.AWSINSTANCETYPE[4];
					break;
				}
				default:{
					
				}
			}
		}else {
			String vcores = properties.getProperty(PYEXECOREKEY);
			int icore = Integer.parseInt(vcores);
			if(icore > 64) instancetype = RNConsts.AWSINSTANCETYPE[4];
			if(icore > 32) instancetype = RNConsts.AWSINSTANCETYPE[3];
			if(icore > 16) instancetype = RNConsts.AWSINSTANCETYPE[2];
			if(icore > 8) instancetype = RNConsts.AWSINSTANCETYPE[1];
		}
		return instancetype;
	}

	private void shutdownInstance(Properties properties) {
		if(null != instance) {
			AWSUtil.stopInstance(awsclient, instance.getInstanceId());
			logger.info(String.format("Instance with id %s stopped.",instance.getInstanceId()));
		}		
	}
	
	private RestTemplate createRestTemplate() {
		return createRestTemplate(usessl, getProperty("exec.ssl.trustStore"), getProperty("exec.ssl.trustStorePassword"));
	}

	@Override
	public boolean validate(String s) throws RNBaseException{
		this.awsclient = AWSUtil.getEC2Client();
		try {
			AWSUtil.describeKeyPair(this.awsclient, RNCTechImg.pubkey);
		} catch (Exception e) {
			logger.info("Validattion failed. "+e.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	public void close() throws TechException {
		shutdownInstance(this.properties);
		pyEngineRunning.set(false);
		pyEngineInitialized.set(false);
		remoteRunid = -1;
		sessionid = -1;
	}


	public void appendOutput(String message) throws IOException {
		outputStream.getExecOutput().write(message);
	}


	@Override
	public RNResult exec(String st, RNContext context) throws TechException {
		if (!pyEngineRunning.get()) {
			return new RNResult(Code.ERROR, "python process not running " + outputStream.toString());
		}
		outputStream.setExecOutput(context.out);

		List<ResultMessage> errorMessage;

		context.setProgressing(20);
		String pname = getAppName(context);
		if(null != context.getParams() && !context.getParams().isEmpty()) {
			String[] argvs = ShellUtils.toPythonArgsList(context.getParams(), context.getJobType(), false);
			logger.info("try to run python with params "+ShellUtils.toArgsString(ShellUtils.toPythonArgsList(context.getParams(), context.getJobType(), true)));
		}	
		JobData jsd = generateRNJob(st, context);
		
		
		String sjoburl = this.pyEngineUrl+"jobs/execute/"+jsd.getJobConfiguration().getName();
		customHeaders.put("Content-Type", "application/json");		
		try {
			String jsonData = RNUtilities.toString(jsd);
			String jsdret = callRestAPI(restTemplate, customHeaders, sjoburl, "POST", jsonData);
			JobData jsdr = RNUtilities.getObjectFromJsonString(jsdret, JobData.class);
			this.remoteRunid = jsdr.getPid();
		}catch(Exception e) {
			
		}
		updateJobDetails(context, pname);
		
		context.setProgressing(25);
		boolean isJobCompleted = false;
		String statusurl = this.pyEngineUrl+"jobs/status/"+jsd.getJobConfiguration().getName()+"/"+this.remoteRunid;
		long stime = System.currentTimeMillis();
		long exectime = 0;
		Code retcode = Code.PROCESSING;
		try {
			while (!isJobCompleted) {
				try {
					Thread.sleep(pullStatusInterval);
				} catch (InterruptedException e) {
					logger.error("InterruptedException when pulling statement status.", e);
					throw new TechException(e);
				}
				
				String ss = callRestAPI(restTemplate, customHeaders, statusurl, "POST", null);
				RNResult rnret = RNUtilities.getObjectFromJsonString(ss, RNResult.class);			
				isJobCompleted = rnret.getRetCode().ordinal() > 3;
				if(isJobCompleted) {
					context.out.write(rnret.toString());
					retcode = Code.SUCCESS;
				}else {
					exectime = System.currentTimeMillis() - stime;
					if(exectime > this.pyengineTimeout) {
						isJobCompleted = true;
						retcode = Code.INCOMPLETE;
						interrupt(jsd, this.remoteRunid);
						context.out.write("Job run timeout ("+(exectime / 1000)+"seconds)\n" + rnret.toString());
					}
				}				
			}	
			return new RNResult(retcode, context.out.toResultMessage());
		} catch (Exception e) {
			return new RNResult(Code.ERROR, "Failed " + e.getMessage()+"\n"+context.out.toString());
		}

	}
	
	private String interrupt(JobData jsd, long rpid) throws RNBaseException, IOException {
		String sjoburl = this.pyEngineUrl+"jobs/cancel/"+jsd.getJobConfiguration().getName()+"/"+rpid;
		customHeaders.put("Content-Type", "application/json");		

		String jsonData = RNUtilities.toString(jsd);
		String cancelret = callRestAPI(restTemplate, customHeaders, sjoburl, "PUT", jsonData);		
		return cancelret;
	}
	

	@Transactional
	protected void updateJobDetails(RNContext ctx, String pname) {
		try {
			String tenant = ctx.getParam("tenant"); 
			long jobid = Long.parseLong(ctx.getParam("jobid"));
			Long id = Long.parseLong(ctx.getCtxid());
			
			if(null == jobrepo) {
				jobrepo = (JobRepository) lookupBean("jobRepository", JobRepository.class);
				jobdetailrepo = (JobDetailsRepository)lookupBean("jobDetailsRepository", JobDetailsRepository.class);
			}
			
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);			
			Job job = jobrepo.findById(id).get();
			
			if(job.getJobid() != jobid || !job.getTenant().equalsIgnoreCase(tenant)) {
				throw new RNBaseException("Job not match with teant "+tenant+" and jobid "+jobid);
			}
			if(null != jdl) {
				if(-1l != this.sessionid)
					jdl.setSessionid((int)sessionid);					
				if(-1 != remoteRunid)
					jdl.setStatementid((int)remoteRunid);
				
				if(null != pname)jdl.setAppid(pname);
				jdl.setSparkUrl(this.pyEngineUrl);
				jdl.setStatus(Status.RUNNING.ordinal());
				jdl.setLastModified(new Date());
				jobdetailrepo.saveAndFlush(jdl);
			}	
			job.setStatus(Status.RUNNING.ordinal());
			String msg = getPyEngineDetail();
			job.setDescription(msg);
			job.setLastModified(new Date());
			jobrepo.saveAndFlush(job);
		} catch (Exception e) {
			logger.error("Update job failed:\n"+e.getMessage());
		}
	}

	private String getPyEngineDetail() {
		if(null != this.instance)
			return this.instance.toString();
		else
			return "py engine not found.";
	}

	@Override
	public IJobExecutor withRepo(JobRepository jobrepo, JobDetailsRepository jobdetailrepo) {
		this.jobrepo = jobrepo;
		this.jobdetailrepo = jobdetailrepo;
		return this;
	}

	@Override
	public void cancel(RNContext ctx) throws RNBaseException {
		JobData jsd = generateRNJob("", ctx);
		if(null == restTemplate)
			restTemplate = createRestTemplate();
		StringBuilder sb = new StringBuilder();
		try {
			String tenant = ctx.getParam("tenant"); 
			Long id = Long.parseLong(ctx.getCtxid());
			Job job = jobrepo.findById(id).get();
			sb.append(job.getTenant()+" cancal job "+job.getJobid()+" run at "+job.getJobExecutedBy());
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);
			if(null != jdl) {	
				try {
					sb.append(" with statement id as "+jdl.getStatementid()+" ");
					String cancelret = interrupt(jsd, jdl.getStatementid());
					boolean cancelled = cancelret.equalsIgnoreCase("cancelled");
					if(cancelled) {
						logger.info(String.format("Job %s cacnelled.", id));
					}else {
						throw new TechException(cancelret);
					}
				} catch (Exception e) {
					if(e instanceof APISessionException) {
						//session already done!
					}else {
						throw e;
					}					
				}	
			}else {
				throw new Exception("No job found for tenant "+job.getTenant()+" with jobid as "+job.getJobid());
			}
		}catch(Exception e) {
			logger.error("get error during cancel job "+jsd.getJobid()+"\n"+e.getMessage());
			throw new RNBaseException(e.getMessage());
		}finally {
			close();
		}	
	}

	@Override
	public RNResult status(RNContext ctx) throws RNBaseException {
		StringBuilder sb = new StringBuilder();		
		try {			
			String tenant = ctx.getParam("tenant"); 
			Long id = Long.parseLong(ctx.getCtxid());
			Job job = jobrepo.findById(id).get();
			sb.append(job.getTenant()+" with jobid "+job.getJobid()+" run at "+job.getJobExecutedBy());
			
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);
			if(null != jdl) {	
				String statusurl = this.pyEngineUrl+"jobs/status/"+jdl.getStatementid();
				String ss = callRestAPI(restTemplate, customHeaders, statusurl, "POST", null);
				customHeaders.put("Content-Type", "application/json");			
				JobInfo jobinfo = RNUtilities.getObjectFromJsonString(ss, JobInfo.class);
				
				sb.append(" with process id as "+jdl.getStatementid()+" ");
				sb.append(" with params as "+jdl.getParams()+" ");				
				
				RNResult res = new RNResult(RNResult.Code.PROCESSING, "");
				res.setSessionid(jdl.getStatementid());
				res.setRetType(Type.TEXT);
				boolean stillRunning = Status.valueOf(jobinfo.getStatus()).isRunning();
				if(stillRunning) {
					res.setRetCode(RNResult.Code.PROCESSING);
				}else {
					res.setRetCode(RNResult.Code.COMPLETED);
				}
				sb.append(jobinfo.getDescription());
				res.setMsg(sb.toString());
				return res;
			}
		} catch (Exception e) {
			sb.append(e.getMessage());
			logger.info("Error: "+sb.toString());
			throw new RNBaseException(e.getMessage());
		}
				
		return null;
	}

/*	public rnctechLogOutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(rnctechLogOutputStream outputStream) {
		this.outputStream = outputStream;
	}*/

/*	public AtomicBoolean getPyScriptRunning() {
		return pyEngineRunning;
	}

	public AtomicBoolean getPyScriptInitialized() {
		return pyEngineInitialized;
	}*/
	
	private JobData generateRNJob(String st, RNContext context) {
		String tenant = context.getParam("tenant");
		JobData job = new JobData();
		Long jid = Long.parseLong(context.getCtxid());
		job.setJobid(-1l);
		job.setUser("");
		job.setPassword("");
		job.setconfigurl("");
		job.setJobName("PyEngineJob"+tenant+System.currentTimeMillis());
		job.setPid(jid);
		JobConfig jobConfiguration = new JobConfig();
		jobConfiguration.setLoadType(LOADTYPE.SMALL.name());
		jobConfiguration.setScriptType("PYTHON");
		jobConfiguration.setJobType(JOBTYPE.ANALYSIS.name());
		jobConfiguration.setCodeSnap(st);
		jobConfiguration.setName(tenant);
		if(null == st || st.isEmpty()) { //cancel, status 
			
		}else{
			Map<String, String> jobProperties = new HashMap<>();
			Map<String, String> libraries = new HashMap<>();
			jobProperties.put("algorithmType", "CUSTOM");
			jobProperties.put("algorithm", "LDA");
			jobConfiguration.setJobProperties(jobProperties);
			jobConfiguration.setParams(context.getParams());
		}
		job.setJobConfiguration(jobConfiguration);
		return job;
	}
	
	public class PyEngine {
		LOADTYPE loadType = LOADTYPE.MEDIUM;
		String memory = "32gb";
		int core = 8;
		String hdisk = "100gb";
		
		String imgName = "RN-Image";
		String pyEnv="3.5";
		boolean isVenv = false;
		//for pip install mysql-connector==2.1.7
		List<String> pypis = new ArrayList<String>();
		//for apt install apache2=2.3.35-4ubuntu1
		List<String> packages = new ArrayList<String>();
		
		Instance instance;
		
	}

}
