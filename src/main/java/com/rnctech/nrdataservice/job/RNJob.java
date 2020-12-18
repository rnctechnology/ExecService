package com.rnctech.nrdataservice.job;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.common.CryptUtils;
import com.rnctech.nrdataservice.exception.ExecException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.RNConsts.AlgorithmType;
import com.rnctech.nrdataservice.RNConsts.LOADTYPE;
import com.rnctech.nrdataservice.RNConsts.STATUS;
import com.rnctech.nrdataservice.RNConsts.PROFILE_ENV;
import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.config.FileConfig;
import com.rnctech.nrdataservice.service.IJobExecutor;
import com.rnctech.nrdataservice.service.JOBService;
import com.rnctech.nrdataservice.service.DummyExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.ResultMessage;
import com.rnctech.nrdataservice.service.javaimpl.JavaExecutor;
import com.rnctech.nrdataservice.service.livyimpl.BaseLivyExecutor;
import com.rnctech.nrdataservice.service.livyimpl.BaseLivyExecutor.LIVYEXE_TYPE;
import com.rnctech.nrdataservice.service.livyimpl.LivyJavaExecutor;
import com.rnctech.nrdataservice.service.livyimpl.LivyPyExecutor;
import com.rnctech.nrdataservice.service.livyimpl.LivySparkSQLExecutor;
import com.rnctech.nrdataservice.service.pyimpl.PythonExecutor;
import com.rnctech.nrdataservice.service.pyimpl.PythonRemoteExecutor;
import com.rnctech.nrdataservice.service.shellimpl.ShellExecutor;
import com.rnctech.nrdataservice.utils.FileUtil;
import com.rnctech.nrdataservice.utils.ConfigClient;
import com.rnctech.nrdataservice.utils.RNUtilities;
import com.rnctech.nrdataservice.utils.RNCOutputStream;
import com.rnctech.nrdataservice.utils.ResourceUtils;
import com.rnctech.nrdataservice.utils.ShellUtils;



/**
 * @author Zilin Chen
 * @since 2020.10
 */

public abstract class RNJob implements InterruptableJob {
	
	public static final String SCRIPT_TYPE = "scriptType"; 
	public static final String LOAD_TYPE = "loadType"; 
	public static final String JOB_TYPE = "jobType";
	public static final String ALGO_TYPE = "algorithmType";
	public static final String CODE_SNAP = "codeSnap";
	public static final String SPARK_URL ="sparkURL"; 
	public static final String EXECUTABLE ="executable"; 
	public static final String INSTNACE_TYPE = "customerInstanceType";
	public static final String MRVERSION = "com.rnctech.version";
	public static final String ENVNAME = "envName";
	public static final String JOB_NAME = "jobname";
	public static final String MODE_NAME = "model.name";
	public static final String SRC_NAME = "ssrcName";
	public static final String ETL_RUN_NUMBER = "ETL_RUN_NUMBER";
	public static final String PARENT_RUN_ID = "PARENT_RUN_ID";
	public static final String LDB_CLIENT_SOURC_Database = "LDB_CLIENT_SOURC_Database";
	public static final String LDB_CLIENT_MYSQL_Database = "LDB_CLIENT_MYSQL_Database";
	public static final String LDB_CLIENT_MYSQL_STG_Database = "LDB_CLIENT_MYSQL_STG_Database";
	public static final String ETL_RUN_ID = "etl_runid";
	public static final String TENANT_MAILTO = "config.email_to";
    public static final String DEFAULT_DOWNLOAD_PATH = "/file/download/";
    public static final String YARN_URL = "sparkapp.yarn.url";
    public static final String HIST_URL = "sparkapp.historyserver.url";
    protected String ssalt = "PASSW@RD_S@!T";
    protected static String STD_ERR = "stderr: ";
    protected static String STD_YARN = "YARN Diagnostics:";
    protected static String STD_USG = "usage: ";
    
	Date dateStarted;
	Date dateFinished;
	volatile Status status;
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
	public static Logger logger = Logger.getLogger(RNJob.class);

	AtomicReference<Thread> runningThread = new AtomicReference<Thread>();
	AtomicBoolean stopFlag = new AtomicBoolean(false);
	protected IJobExecutor executor;
	protected Map<String, String> tps=new HashMap<>();

	public static List<String> extraTPkey = new ArrayList<>();
	static {
		extraTPkey.add(ETL_RUN_NUMBER);
		extraTPkey.add(PARENT_RUN_ID);
		extraTPkey.add(LDB_CLIENT_SOURC_Database);
		extraTPkey.add(LDB_CLIENT_MYSQL_Database);
		extraTPkey.add(LDB_CLIENT_MYSQL_STG_Database);
	}
	public abstract FactoryBean<? extends Trigger> getRNTrigger(JobDetail jobDetail);

	public abstract JobDetailFactoryBean getRNJob(JobData job);

	protected JobData job;
	
    @Autowired
    JobRepository jobrepo;
    
    @Autowired
    JobDetailsRepository jobdetailrepo;
    
	@Autowired
	private ApplicationContext appContext;
	
	JobExecutionContext jobExecutionContext;

	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		
		this.runningThread.set(Thread.currentThread());		
		try {
			if (jobContext.getJobDetail().getJobDataMap().entrySet().isEmpty()) {
				logger.info("No job execution info found!");
				return;
			}
			this.jobExecutionContext = jobContext;
			job = getJSD(jobContext.getJobDetail().getJobDataMap());
			Thread.currentThread().setName(jobContext.getJobDetail().getJobDataMap().getString("name") + " | " + jobContext.getJobDetail().getJobDataMap().getString("jobid"));
			MDC.put("tenant", jobContext.getJobDetail().getJobDataMap().getString("name"));
			MDC.put("jobid", jobContext.getJobDetail().getJobDataMap().getString("jobid"));

			try {
				tps.putAll(ConfigClient.getNameConfigs(job));
				ssalt = ConfigClient.getConfig(RNConsts.PWDSALT,job, "");
			} catch (Exception e) {
				logger.error(jobContext.getJobDetail().getJobDataMap().getString("name")+e.getMessage());
			}
			if(tps.isEmpty()) {
				logger.warn("failed to get MR properties!! "+job.toString());
			}

			while (!stopFlag.get()) {
				RNContext ctx = setupContext(jobContext, tps);
				if (stopFlag.get())
					break;
				
				RNResult res = executeInternal(jobContext, ctx);
				if (RNResult.Code.SUCCESS.equals(res.code())) {
					List<ResultMessage> retmsgs = ctx.getOut().toResultMessage();
					String ress = res.getMsg();
					if(null == ress || ress.isEmpty()) ress = res.toString();
					if (null != retmsgs && retmsgs.size() >= 1) {
						ress = retmsgs.get(0).getData();
					}
					
					job.setExeinfo(ress);	
					String errmsg = ress.contains(STD_ERR)?ress.substring(ress.indexOf(STD_ERR)+STD_ERR.length()).trim():null;
					if(null == errmsg || errmsg.isEmpty() || errmsg.trim().startsWith(STD_YARN)) {
						job.setDescription("Job completed at " + new Date() + " with result as " + RNUtilities.getPreString(ress,1536));
						job.setStatus(Status.FINISHED.name());
						logger.info(job.getDescription());
						ConfigClient.updateProperties(job, STATUS.COMPLETED, "Job Completed Successfully.",null, new Date(), true);
						updateJobDetail(jobContext, ctx, ress, Status.FINISHED.ordinal(), job);
					}else {
						//spark exception
						job.setDescription("Job run and completed at " + new Date() + " with error\n" + errmsg);
						updateJobDetail(jobContext, ctx, res.toString(), Status.FINISHEDWITHERROR.ordinal(), job);
						throw new ExecException(errmsg);
					}
				}else if (RNResult.Code.ERROR.equals(res.code()) || RNResult.Code.FAILED.equals(res.code())) {					
					List<ResultMessage> retmsgs = ctx.getOut().toResultMessage();
					String ress = res.getMsg();
					if(null == ress || ress.isEmpty()) ress = res.toString();
					if (null != retmsgs && retmsgs.size() >= 1) {
						ress = retmsgs.get(0).getData();
					}
					
					job.setExeinfo(ress);
					String errmsg = "Job run failed at " + new Date() + "\n" + ress;
					logger.error(errmsg);
					job.setDescription(errmsg);
					updateJobDetail(jobContext, ctx, ress, Status.ERROR.ordinal(), job);
					throw new ExecException(ress);
				} else {	
					String errmsg = "Job run finished at " + new Date() + " with error\n" + res.toString();
					job.setExeinfo(errmsg);
					logger.info(errmsg);
					job.setDescription(errmsg);
					updateJobDetail(jobContext, ctx, errmsg, Status.FINISHEDWITHERROR.ordinal(), job);
					throw new ExecException(errmsg);
				}
			}
			
		} catch (Exception e) {
			job.setExeinfo(e.getMessage());
			job.setStatus(Status.ERROR.name());
			String errmsg= "Error";
			if(null != e.getMessage()) {
				errmsg = e.getMessage().contains(STD_USG)?e.getMessage().substring(e.getMessage().indexOf(STD_USG)+STD_USG.length()).trim():null;
				if(null == errmsg || errmsg.isEmpty())
					errmsg = e.getMessage().contains(STD_ERR)?e.getMessage().substring(e.getMessage().indexOf(STD_ERR)+STD_ERR.length()).trim():e.getMessage();
			}
			job.setDescription("Job Failed at " + new Date() + "\n" + errmsg);
			try {
				if(!(e instanceof ExecException)) updateJobDetail(jobContext, null, errmsg, Status.ERROR.ordinal(), job);
			} catch (Exception exec) {
				logger.error("Update job status Failed: "+exec.getMessage());
			}
			try {
				ConfigClient.updateProperties(job, STATUS.FAILED, job.getDescription(),null, new Date(), true);
			}catch(Exception exec) {
				logger.error("Update MR job status Failed: "+exec.getMessage());
			}
			throw new JobExecutionException(errmsg);
		} finally {
			MDC.remove("tenant");
			MDC.remove("jobid");
		}
		return;
	}

	public JobData getJSD(JobDataMap jobDataMap) {
		JobData jsd = new JobData();
		JobConfig jc = new JobConfig();
		jc.setName(jobDataMap.getString("name"));
		jsd.setJobConfiguration(jc);
		jsd.setJobid(Long.valueOf(jobDataMap.getString("jobid")));
		jsd.setAllowssl(Boolean.valueOf(jobDataMap.getString("allowssl")));
		jsd.setconfigurl(jobDataMap.getString("configurl"));
		jsd.setUser(jobDataMap.getString("user"));
		jsd.setPassword(jobDataMap.getString("password"));
		return jsd;
	}

	public RNContext setupContext(JobExecutionContext jobExecutionContext, Map<String, String> tps) throws RNBaseException {
		JobDataMap jdm = jobExecutionContext.getJobDetail().getJobDataMap();
		//add extra key-value pairs from DM
		extraTPkey.forEach(extratpk ->{
			if(jdm.containsKey(extratpk)) {
				String extraTPValue = jdm.getString(extratpk);
				if(null != extraTPValue && extraTPValue.trim().length() > 1) {
					logger.debug("Get key and vaule: "+extratpk+":"+extraTPValue.trim());
					if(tps.containsKey(extratpk)) {
						tps.replace(extratpk, extraTPValue.trim());
					}else {
						tps.put(extratpk, extraTPValue.trim());
					}
				}
			}
		});
		RNContext ctx = RNContext.builder().setOutput(new RNCOutputStream(null));
		String alg = "test";
		if(null != jdm.getString("algorithm"))
			alg = jdm.getString("algorithm").toLowerCase();
		else if (null != jdm.getString("loadType"))
			alg = jdm.getString("loadType").toLowerCase();
		
		ctx.setAlgorithm(alg);
		String jobtype=jdm.getString(JOB_TYPE);
		ctx.setJobType(jobtype);
		ctx.setSrctype(TechType.valueOf(jdm.getString(SCRIPT_TYPE).toLowerCase()));
		
		if(null != jdm.get(JOB_NAME)) ctx.setJobname(jdm.get(JOB_NAME).toString());
		ctx.setCtxid(String.valueOf(jdm.get(RNConsts.JOB_PROC_ID)));

		//@Todo add validation as param run need with local/HDFS fs
		final int prelen = ShellUtils.RN_ALGHP_PRE.length();

		jdm.entrySet().stream().filter(entry -> entry.getKey().startsWith(ShellUtils.RN_ALGHP_PRE)).forEach(ety -> {
			if (ety.getKey().startsWith(ShellUtils.RN_ALGHP_PRE) && (null != ety.getValue())) {
				String k =  ety.getKey().substring(prelen);
				String v =  ety.getValue().toString();
				try {
					String replv = replaceParameters(v, tps);
					try {
						if(k.endsWith("password"))
							replv = CryptUtils.decrypt(replv, ssalt);
					} catch (Exception e) {
					}
					ctx.setParam(k, replv);
				} catch (RNBaseException e) {
					logger.error("Can't set parameter for "+k+ " and "+v+"\n"+e.getMessage());
				}
			}			
		});
		
		if(null != jdm.getString(ETL_RUN_NUMBER)) {
			ctx.setParam(ETL_RUN_ID, jdm.getString(ETL_RUN_NUMBER));
		}else if(null != jdm.getString(PARENT_RUN_ID)) {
			ctx.setParam(ETL_RUN_ID, jdm.getString(PARENT_RUN_ID));
		}
		ctx.setParam("tenant", jdm.getString("name"));
		ctx.setParam("jobid", jdm.getString("jobid"));
		if(RNUtilities.isMRJob(jobtype)) 
			addMRinfo(ctx, jdm, tps);		
		
		final int predeplen = ShellUtils.RN_ALDEP_PRE.length();
		jdm.entrySet().stream().filter(entry -> entry.getKey().startsWith(ShellUtils.RN_ALDEP_PRE)).forEach(ety -> {
			if (ety.getKey().startsWith(ShellUtils.RN_ALDEP_PRE) && (null != ety.getValue())) {				
				try {
					String k =  ety.getKey().substring(predeplen);
					String v =  ety.getValue().toString().trim();
					if(v.startsWith("http:") || v.startsWith("ftp:") || v.startsWith("s3a:")) {
						String replv = replaceParameters(v, tps);
						ctx.getDependenics().add(replv);
						logger.info("dependency of "+v+" add.");
					}else {
						throw new RNBaseException("Not support file "+v+". please upload the first and add download link.");
					}
				} catch (RNBaseException e) {
					logger.error("Can't add dependency.\n"+e.getMessage());
				}
			}			
		});
		
		jdm.entrySet().stream().filter(entry -> entry.getKey().startsWith(ShellUtils.SPARK_CONF_PRE)).forEach(ety -> {
			String k =  ety.getKey();
			String v =  ety.getValue().toString().trim();
			try {
				if(v.length() < 6)
					ctx.setConfv(k, v);
				else {
					String replv = replaceParameters(v, tps);
					ctx.setConfv(k, replv);
				}
			} catch (RNBaseException e) {
				logger.error("Can't set parameter for "+k+ " and "+v+"\n"+e.getMessage());
			}
		});
		
		String bfolder = getFConfig(jobExecutionContext);
		getJobExecutor(jdm, ctx, bfolder, tps);
		return ctx;
	}

	private void addMRinfo(RNContext ctx, JobDataMap jdm, Map<String, String> tps) {
		ctx.setParam("configurl", jdm.getString("configurl"));
		String ss =  tps.get("com.ldb.mr.tenantsecretstring");
		if(null == ss || ss.equals("null"))
			ss="";
		else {
			try {
				ss = CryptUtils.decrypt(ss, ssalt);
			} catch (Exception e) {
			}
		}
		ctx.setParam("secretstring",ss);		
		String mailto = tps.get("com.apps.common.etl.EMAIL_TO");
		if(null != mailto && !mailto.trim().isEmpty() && mailto.contains("@")) {
			jdm.put(TENANT_MAILTO, mailto);
		}
	}

	public abstract RNResult executeInternal(JobExecutionContext jobExecutionContext, RNContext ctx)
			throws JobExecutionException;

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		stopFlag.set(true);
		Thread thread = runningThread.getAndSet(null);
		if (thread != null) {
			String imsg = "Job interrupt at " + new Date();
			logger.info(imsg);
			job.setDescription(imsg);
			thread.interrupt();
			try {
				if(null != this.jobExecutionContext) {
					job.setExeinfo("Cancalled.");
					updateJobDetail(this.jobExecutionContext, null, imsg, Status.ABORT.ordinal(), job);
				}
				ConfigClient.updateProperties(job, STATUS.CANCELLED, job.getDescription(),null, new Date(), true);
			} catch (Exception e) {
				logger.error("Update job status failed for Cancalling: "+e.getMessage());
				throw new UnableToInterruptJobException("Update job status failed for Cancalling: "+e.getMessage());
			}
		}

	}

	public String getJobKeyName(JobData job) {
		return job.getJobConfiguration().getName() + "_RNJob";
	}

	public static String getTriggerKeyName(Class clz) {
		return clz.getName() + "_RNTrigger";
	}

	public JobDataMap buildJobMap(JobData job) {
		ObjectMapper oMapper = new ObjectMapper();
		JobDataMap jobMap = new JobDataMap();
		try {
			Map<String, Object> jconfig = oMapper.convertValue(job.getJobConfiguration(), Map.class);
			jconfig.remove(ShellUtils.RN_JOB_PROPERTY);
			jconfig.remove(ShellUtils.RN_ALG_HPARAM);
			jconfig.remove(ShellUtils.RN_ALG_DEPEND);
			jconfig.entrySet().stream().forEach(x -> {
				if ((null != x.getValue()))
					jobMap.put(x.getKey(), x.getValue().toString());
			});
			if(null != job.getJobConfiguration().getJobProperties())
			job.getJobConfiguration().getJobProperties().entrySet().stream().forEach(ety -> {
				if ((null != ety.getValue()))
					jobMap.put(ety.getKey(), ety.getValue().toString());
			});
			if(null != job.getJobConfiguration().getParams())
			job.getJobConfiguration().getParams().entrySet().stream().forEach(ety -> {
				if ((null != ety.getValue())) {
					if(ety.getKey().startsWith(ShellUtils.PYTHON_CONF_PRE) || ety.getKey().startsWith(ShellUtils.SPARK_CONF_PRE)) {
						jobMap.put(ety.getKey(), ety.getValue().toString());
					}else {
						jobMap.put(ShellUtils.RN_ALGHP_PRE+ety.getKey(), ety.getValue().toString());
					}
				}
			});
			if(null != job.getJobConfiguration().getLibraries())
			job.getJobConfiguration().getLibraries().entrySet().stream().forEach(ety -> {
				if ((null != ety.getValue()))
					jobMap.put(ShellUtils.RN_ALDEP_PRE+ety.getKey(), ety.getValue().toString());
			});
			jobMap.put("jobid", String.valueOf(job.getJobid()));
			if(null != job.getJobName()) jobMap.put(JOB_NAME, job.getJobName());
			jobMap.put("allowssl", String.valueOf(job.getAllowssl()));
			jobMap.put("configurl", job.getconfigurl());
			jobMap.put("user", job.getUser());
			jobMap.put("password", job.getPassword());
			jobMap.put(RNConsts.JOB_PROC_ID, String.valueOf(job.getPid()));
			
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return jobMap;
	}

	// @Todo build RNConext based on JobDataMap
	private IJobExecutor getJobExecutor(JobDataMap dataMap, RNContext ctx, String fbase, Map<String, String> tps) throws RNBaseException {
		Properties prop = new Properties();
		for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
			prop.put(entry.getKey(), entry.getValue());
		}

		if (!prop.contains("rnctech.exec.maxResult"))
			prop.setProperty("rnctech.exec.maxResult", "20");

		TechType techtype = TechType.pyspark;
		if (null != dataMap.getString(SCRIPT_TYPE))
			techtype = TechType.valueOf(dataMap.getString(SCRIPT_TYPE).toLowerCase());

		LOADTYPE ltype = LOADTYPE.MEDIUM;
		if(null != dataMap.getString(LOAD_TYPE))
			ltype = LOADTYPE.valueOf(dataMap.getString(LOAD_TYPE).trim().toUpperCase());
		
		String codesnap = prop.getProperty(CODE_SNAP);
		String srcSuffix = ".py";
		
		switch (techtype) {
		case python: {
			if (!prop.contains("rnctech.exec.gatewayserver_address"))
				prop.setProperty("rnctech.exec.gatewayserver_address", "127.0.0.1");
			
			if(null != ltype && ltype.ordinal() > 2)
				executor = new PythonRemoteExecutor(prop);
			else	
				executor = new PythonExecutor(prop);
			break;
		}
		case pyspark: {
			setSharedLivyProperties(prop);
			executor = new LivyPyExecutor(prop);
			break;
		}
		case spark: {
			srcSuffix = ".scala";
			setSharedLivyProperties(prop);
			executor = new LivyJavaExecutor(prop);
			break;
		}
		case java:
		case scala: {
			srcSuffix = ".scala";
			executor = new JavaExecutor(prop);
			break;
		}
		case sql: {
			srcSuffix = ".sql";
			setSharedLivyProperties(prop);
			if (!prop.contains("livy.spark.sql.maxResult"))
				prop.setProperty("livy.spark.sql.maxResult", "100");
			executor = new LivySparkSQLExecutor(prop);
			break;
		}
		case shell: {
			srcSuffix = ".sh";
			executor = new ShellExecutor(prop);
			break;
		}
		default:
			executor = new DummyExecutor(prop);
		}
		
		String modelname = (null == dataMap.getString(MODE_NAME))?"":dataMap.getString(MODE_NAME)+"_";
		String srcname = techtype+"_"+dataMap.getString("name")+"_"+modelname+dataMap.getString("jobid")+srcSuffix;
		ctx.setSrcname(srcname);
		dataMap.put(SRC_NAME, srcname);
		
		executor.withAppContext(appContext);
		executor.withRepo(jobrepo, jobdetailrepo);
		logger.info("init executor "+executor.getClass());
		executor.setProperties(prop);

		String algorithmType = prop.getProperty(ALGO_TYPE);
		if(null == algorithmType) algorithmType = AlgorithmType.BUILTIN.name();
		
		if(techtype == TechType.pyspark || techtype == TechType.python) {	
			if(null == codesnap) {
				codesnap = setDefault(techtype);
				logger.warn("No code to run, set as default "+codesnap);
			}	

			//logger.info("algorithmType="+algorithmType+" and codeSnap="+codesnap);
			if(algorithmType.equals(AlgorithmType.CUSTOM.name()) || techtype == TechType.python){
				if(codesnap.startsWith("http:") || codesnap.startsWith("file:") || codesnap.startsWith("s3a:")) {
			    	//s3a://com.rnctech.nrt/exec/py/params_test.py
					try {
						codesnap = replaceParameters(codesnap, tps, prop);
						logger.info("try to load template source from " + codesnap);
						String tempsrc = ResourceUtils.getTemplate(codesnap);
						codesnap = replaceParameters(tempsrc, tps, prop);
					} catch (Exception e) {
						throw new RNBaseException("try python codesnap failed: "+codesnap);
					}
				}else {
					//run interactive mode with raw code
					codesnap = replaceParameters(codesnap, tps, prop);
				}

				String fname = fbase +"/"+ ctx.getSrcname();
				try {
					FileUtil.saveFile(fname, codesnap);
					String accesspath = JOBService.getServerPath(DEFAULT_DOWNLOAD_PATH+ctx.getSrcname());
					logger.debug("file saved and access url as: "+accesspath);
					if(executor instanceof PythonRemoteExecutor) {
						codesnap = accesspath;
					}
				} catch (IOException e) {
					logger.warn("try to save temp python as "+fname+" failed: "+e.getMessage());
				}
		    }else if(algorithmType.equals(AlgorithmType.BUILTIN.name()) && (codesnap.startsWith("http:") || codesnap.startsWith("file:") || codesnap.startsWith("s3a:"))) {
				//http://10.0.0.234:8082/exec/file/download/pi.py
				//file:///tmp/argtest.py
		    	codesnap = replaceParameters(codesnap, tps, prop);
				((BaseLivyExecutor)executor).setExetype(LIVYEXE_TYPE.batch);
				logger.info("set pyspark type as "+LIVYEXE_TYPE.batch+ " with "+codesnap);
			} else {
				codesnap = replaceParameters(codesnap, tps, prop);
				logger.debug("\n"+codesnap+"\n");
				if(null != ctx.getParams() && !ctx.getParams().isEmpty()) {				
					try {
						String fname = fbase +"/"+ ctx.getSrcname();
						String fpath=FileUtil.saveFile(fname, codesnap);
						String accesspath = JOBService.getServerPath(DEFAULT_DOWNLOAD_PATH+ctx.getSrcname());
						//logger.debug("file save as: "+fpath+"\n access url as: "+accesspath);
						((BaseLivyExecutor)executor).setExetype(LIVYEXE_TYPE.batch);
						codesnap = accesspath;
						dataMap.put(ALGO_TYPE, AlgorithmType.BUILTIN.name());
						logger.info("set pyspark type as "+LIVYEXE_TYPE.batch);
					} catch (IOException e) {
						logger.warn("try batch run failed as: "+e.getMessage());
					}
				}
			}
			ctx.setCodesnap(codesnap);
			logger.debug("Code to run: "+codesnap);
		}else if(techtype == TechType.spark) {
			if(null == codesnap || codesnap.isEmpty()) {
				throw new RNBaseException("no code or main jar defined in codesnap "+codesnap);
			}
			if(algorithmType.equals(AlgorithmType.CUSTOM.name())){
				((BaseLivyExecutor)executor).setExetype(LIVYEXE_TYPE.interactive);	
				codesnap = replaceParameters(codesnap, tps, prop);
			} else {			
				String clsname = prop.getProperty(EXECUTABLE);
				codesnap = replaceParameters(codesnap, tps);
				if(null == clsname || clsname.trim().isEmpty())
					throw new RNBaseException("no class name defined for type "+techtype);
				
				((BaseLivyExecutor)executor).setExetype(LIVYEXE_TYPE.batch);				
					
				ctx.setSrcname(clsname);
				logger.info("set spark type as "+LIVYEXE_TYPE.batch+ " with "+codesnap+ " and class "+clsname);
			}
			ctx.setCodesnap(codesnap);		
		}else if(techtype == TechType.java || techtype == TechType.scala) {
			if(null == codesnap || codesnap.isEmpty()) {
				throw new RNBaseException("no code or main jar defined in codesnap "+codesnap);
			}
			String clsname = prop.getProperty(EXECUTABLE);
			if(null == clsname || clsname.trim().isEmpty())
				throw new RNBaseException("no class name defined for type "+techtype);

			ctx.setSrcname(clsname);
			logger.info("Run "+codesnap+ " with class "+clsname);
			
			ctx.setCodesnap(codesnap);		
		}else if(techtype == TechType.shell) {
			String scmd = prop.getProperty(EXECUTABLE);
			if(null == scmd || scmd.trim().isEmpty())
				scmd = replaceParameters(codesnap, tps, prop);
			
			if(null == scmd || scmd.trim().isEmpty())
				throw new RNBaseException("no command found for type "+techtype);

			logger.info("Run "+scmd+ " with type "+ techtype);			
			ctx.setCodesnap(scmd);		
/*		}else if(techtype == TechType.sql) {
			if(null == codesnap || codesnap.isEmpty()) {
				throw new RNException("no sql code defined in codesnap "+codesnap);
			}
			logger.info("Run "+codesnap+ " with type "+ techtype);			
			ctx.setCodesnap(codesnap);	*/	
		}
		
		logger.info("Run "+executor.getClass().getName());
		
		return executor;
	}

	public static String replaceParameters(String codesnap, Map<String, String> props) throws RNBaseException {
		return replaceParameters(codesnap, props, null);
	}
	
	//replace all runtime parameters with value in tenant properties
	public static String replaceParameters(String codesnap, Map<String, String> props,  Properties jobprop) throws RNBaseException {
		if(null == props || props.isEmpty()) {
			return codesnap;
		}
 
		Pattern p = Pattern.compile("(.+?)");

		String x = new String(codesnap);		
		Matcher m = p.matcher(x);
		while(m.find()) {
			String g = m.group();
			String gg = g.substring(0, g.length());
			if(null != jobprop && jobprop.containsKey(gg)) { //passing param has higher priority
				String replacement = jobprop.getProperty(gg); 
				x = x.replaceAll(g, replacement);
				logger.info(g+" replaced by "+replacement);
			}else if(props.containsKey(gg)) {
				String replacement = props.get(gg); 
				x = x.replaceAll(g, replacement);
				logger.info(g+" replaced by "+replacement);
			}else {
				logger.error("Not found replacement for key "+gg);
				throw new RNBaseException("No parameter found for key "+gg);
			}
			m = p.matcher(x);
		}
		
		codesnap = x;
		//logger.debug("Get replacment as:\n"+codesnap);		
		return codesnap;
	}

	private void setSharedLivyProperties(Properties prop) throws RNBaseException {
		String deflivyurl = "http://10.0.0.8:8998";
		if (null != prop.getProperty(SPARK_URL)) {
			int lindex = prop.getProperty(SPARK_URL).lastIndexOf(":");
			deflivyurl = prop.getProperty(SPARK_URL).substring(0, lindex)+":8998";
		} else if(null != tps) {
			if(null != tps.get(YARN_URL)) {
				int lindex = tps.get(YARN_URL).lastIndexOf(":");
				deflivyurl = tps.get(YARN_URL).substring(0, lindex)+":8998";
			}else if(null != tps.get(HIST_URL)) {
				int lindex = tps.get(HIST_URL).lastIndexOf(":");
				deflivyurl = tps.get(HIST_URL).substring(0, lindex)+":8998";
			}else {
				if(null != tps.get(INSTNACE_TYPE) && !"-".equals(tps.get(INSTNACE_TYPE).trim())) {
					String envtype = tps.get(INSTNACE_TYPE);
					if(envtype.toUpperCase().contains(PROFILE_ENV.PROD.name())){
						throw new RNBaseException("No spark url found("+YARN_URL+") in MR for "+INSTNACE_TYPE+": "+envtype);
					}
				}
			}
		}
		prop.setProperty("livy.url", deflivyurl);
		logger.info("Run on "+prop.getProperty("livy.url"));
		if (null == prop.getProperty("livy.session.create_timeout"))
			prop.setProperty("livy.session.create_timeout", "1000");
		prop.put("livy.displayAppInfo", "false");
	}

	public String setDefault(TechType techtype) {
		String defsnap = RNConsts.DEFAULT_PYSPARK_CODESNAP;
		switch (techtype) {
		case python:
			return RNConsts.DEFAULT_PYTHON_CODESNAP;
		case pyspark:
			return RNConsts.DEFAULT_PYSPARK_CODESNAP;
		case java:
			return RNConsts.DEFAULT_JAVA_CODESNAP;
		case sql:
			return RNConsts.DEFAULT_JAVA_CODESNAP;
		case shell:
			return RNConsts.DEFAULT_SHELL_CODESNAP;
		default:
			return defsnap;
		}
	}

	public String getFConfig(JobExecutionContext context) {
		try {
			FileConfig fileconfig = (FileConfig)context.getScheduler().getContext().get(JOBService.FCONFIG);
			return fileconfig.getUploadDir();
		} catch (Exception e) {
			//throw new RNException(e);
		}
		return "/tmp/uploads";
	}
	
	public static String getTenantJobId(JobData job) {
		return getTenantJobId(job.getJobConfiguration().name, job.getJobid());
	}

	public static String getTenantJobId(String tenantName, long jobid) {
		return tenantName + "_" + jobid;
	}

	//update job status and JobDetail for Spark job
	protected void updateJobDetail(JobExecutionContext context, RNContext ctx, String ress, int status, JobData jsd) throws ExecException {
		try {
			JobDataMap datamap = context.getJobDetail().getJobDataMap();
			Long jobpid = datamap.getLong(RNConsts.JOB_PROC_ID);			
			Job job = jobrepo.findById(jobpid).get();
			if(null != tps && !tps.isEmpty()) {
				job.setEnvname(tps.get(ENVNAME));
				if(null != tps.get(INSTNACE_TYPE) && !"-".equals(tps.get(INSTNACE_TYPE).trim())) job.setInstanceType(tps.get(INSTNACE_TYPE));
				job.setMrversion(tps.get(MRVERSION));
			}

			updateJob(job, jsd, status);
			if(null != job && (executor instanceof BaseLivyExecutor)) {
				BaseLivyExecutor livyexector =(BaseLivyExecutor)executor;
				JobDetails jdl = jobdetailrepo.findByNameAndJobid(datamap.getString("name"), jobpid);
				if(null != jdl) {				
					if(-1 != livyexector.getSessionid())jdl.setSessionid(livyexector.getSessionid());
					if(null != livyexector.getSessionInfo() && null != livyexector.getSessionInfo().appId) jdl.setAppid(livyexector.getSessionInfo().appId);
					jdl.setStatus(status);
					jdl.setLastModified(new Date());
					jobdetailrepo.save(jdl);
					jobdetailrepo.flush();
				}
			}else if(null != job && (executor instanceof PythonExecutor)) {
				JobDetails jdl = jobdetailrepo.findByNameAndJobid(datamap.getString("name"), jobpid);
				if(null != jdl) {
					jdl.setStatus(status);
					jdl.setLastModified(new Date());
					jobdetailrepo.save(jdl);
					jobdetailrepo.flush();
				}
			}
		} catch (Exception e) {
			logger.info("update job status failed: "+e.getMessage());
			throw new ExecException(e);
		}		
	}
	

	private void updateJob(Job job, JobData jsd, int status) {
		//Job job = jobrepo.findById(jsd.getPid()).get();
		job.setExecutionInfo(jsd.getExeinfo());
		job.setStatus(status);
    	job.setActive(false);
    	job.setLastModified(new Date());
    	job.setUpdatedby(RNJob.class.getSimpleName());
    	String desc =  jsd.getDescription();
    	job.setDescription((desc.length() > 2000)?desc.substring(0, 2000):desc);
    	jobrepo.saveAndFlush(job);
    	jobrepo.flush();
		
	}
	
	public enum Status {
		UNKNOWN, INIT, READY, PENDING, RUNNING, FINISHED, FINISHEDWITHERROR, ERROR, ABORT, FAILED;

		public boolean isReady() {
			return this == READY;
		}

		public boolean isRunning() {
			return this == RUNNING;
		}

		public boolean isPending() {
			return this == PENDING;
		}

		public boolean isCompleted() {
			return this == FINISHED || this == FINISHEDWITHERROR || this == ERROR || this == ABORT || this == FAILED;
		}

	}
	
	public JobData getJob() {
		return job;
	}

	public void setJob(JobData job) {
		this.job = job;
	}
}
