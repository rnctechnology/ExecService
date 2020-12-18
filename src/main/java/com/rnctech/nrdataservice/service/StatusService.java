package com.rnctech.nrdataservice.service;

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.service.livyimpl.BaseLivyExecutor.LIVYEXE_TYPE;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.service.RNResult.Type;
import com.rnctech.nrdataservice.service.javaimpl.JavaExecutor;
import com.rnctech.nrdataservice.service.livyimpl.LivyJavaExecutor;
import com.rnctech.nrdataservice.service.livyimpl.LivyPyExecutor;
import com.rnctech.nrdataservice.service.livyimpl.LivySparkSQLExecutor;
import com.rnctech.nrdataservice.service.pyimpl.PythonExecutor;
import com.rnctech.nrdataservice.service.shellimpl.ShellExecutor;
import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.job.RNJob;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.utils.ConfigClient;
import com.rnctech.nrdataservice.utils.RNCOutputStream;

/*
* @author zilin chen
* @since 2020.02
*/

@Service
public class StatusService implements RNConsts {

	public static Logger logger = Logger.getLogger(StatusService.class);

	@Autowired
	private ApplicationContext appContext;
	@Autowired
	private JobRepository jobrepo;
	@Autowired
	private JobDetailsRepository jdrepo;
	@Autowired
	private SchedulerFactoryBean schedFactory;
	@Autowired
	private AuthService authService;

	private Scheduler getScheduler() throws SchedulerException {
		Scheduler scheduler = schedFactory.getScheduler();
		return scheduler;
	}
	
	public RNResult getJobStatus(String tenant, Long pid) throws Exception {
		try {
			Job job = jobrepo.findById(pid).get();
			if(job.getStatus() >= 5) {				
				RNResult res = new RNResult(Code.COMPLETED, job.getDescription());
				res.setRetType(Type.TEXT);
				return res;
			}
			JobDetails jdl = jdrepo.findByTenantAndJobid(tenant, pid);
			JobDataMap jdp = null;
			if(null != job && null != jdl) {
				String jobkeyname = job.getJobkey();
				Scheduler scheduler = schedFactory.getScheduler();
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(tenant))) {
					if(jobKey.getName().equals(jobkeyname)) {
						JobDetail jobDetail = scheduler.getJobDetail(jobKey);
						jdp = jobDetail.getJobDataMap();
						break;
					}					
				}
				if(null == jdp) throw new RNBaseException("No Job found for "+jobkeyname);
				IJobExecutor executor = getJobExecutor(jdp, jdl);
				RNContext ctx = RNContext.builder().setOutput(new RNCOutputStream(null));
				ctx.setCtxid(String.valueOf(jdp.get(RNConsts.JOB_PROC_ID)));
				ctx.setParam("tenant", tenant);
				ctx.setParam("jobid", jdp.getString("jobid"));
				RNResult res = executor.status(ctx);
				if(null == res) throw new RNBaseException("No result found for running id "+pid);
				return res;
			} else {
				throw new RNBaseException("Job with running id "+pid+" not found yet.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw e;
		}

	}
	
	public String cancelJob(String tenant, Long pid) throws Exception {
		try {
			Job job = jobrepo.findById(pid).get();
			if (job.getStatus() >= 5) {
				return "The job " + pid + " of name " + tenant + " already done!";
			}

			String jobkeyname = job.getJobkey();
			Scheduler scheduler = getScheduler();
			Set<JobKey> jkeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(tenant));
			if(null == jkeys || jkeys.isEmpty()) {
				return "The job " + pid + " of name " + tenant + " not found!";
			}
			JobKey runningjob = jkeys.stream().filter(jk -> jk.getName().equals(jobkeyname)).findAny().get();
			JobDataMap jdp = null;
			if (null != runningjob) {
				JobDetails jdl = jdrepo.findByTenantAndJobid(tenant, pid);
				JobDetail jobDetail = scheduler.getJobDetail(runningjob);
				jdp = jobDetail.getJobDataMap();
				if (null == jdp)
					throw new RNBaseException("No Job triggered for " + jobkeyname);
				if (null == jdl)
					throw new RNBaseException("No Job found for running id " + pid);

				IJobExecutor executor = getJobExecutor(jdp, jdl);
				RNContext ctx = RNContext.builder().setOutput(new RNCOutputStream(null));
				ctx.setCtxid(String.valueOf(jdp.get(RNConsts.JOB_PROC_ID)));
				ctx.setParam("tenant", tenant);
				ctx.setParam("jobid", jdp.getString("jobid"));
				executor.cancel(ctx);
				try {
				// scheduler.unscheduleJob(runningjob);
				scheduler.interrupt(runningjob);
				scheduler.deleteJob(runningjob);
				} catch (Exception e) {  //in case of JES service restart
					updateJobStatusCanneled(job);
				}
				return "cancelled";

			} else {
				return "No running job found for " + tenant + " with key " + jobkeyname;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw e;
		}
	}

	public void updateJobStatusCanneled(Job job) {
		try {
			job.setStatus(Status.ABORT.ordinal());
			job.setExecutionInfo("Cancelled.");
			job.setLastModified(new Date());
			job.setUpdatedby(StatusService.class.getSimpleName());
			jobrepo.saveAndFlush(job);
			jobrepo.flush();
			JobData jsd = new JobData();
			JobConfig jc = new JobConfig();
			jc.setName(job.getTenant());
			jsd.setJobConfiguration(jc);
			jsd.setJobid(job.getJobid());
			jsd.setAllowssl(false);
			jsd.setconfigurl(job.getconfigurl());
			jsd.setUser(job.getMrUser());
			jsd.setPassword(authService.encryptString(job.getMrPassword()));
			ConfigClient.updateProperties(jsd, STATUS.CANCELLED, job.getDescription(),null, new Date(), true);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public String validateSparkExecutor(String sparkurl) {
		return validateExecutor(sparkurl, TechType.pyspark);
	}
	
	public String validatePythonExecutorLocally() {
		return validateExecutor(null, TechType.python);
	}
	
	public String validateExecutor(String url, TechType ttype) {
		String ret = null;
		try {
			Map<String, Object> dataMap = System.getenv().entrySet().stream()
			         .filter(m -> m.getKey() != null && m.getValue() !=null)
			         .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));
			dataMap.put(RNJob.SCRIPT_TYPE, ttype);
			IJobExecutor executor = getJobExecutor(dataMap, url, LIVYEXE_TYPE.batch);
			if(executor.validate(url)) {
				ret = "Validate passed for type of "+ttype.name();
			}else {
				throw new RNBaseException(ttype.name()+" "+dataMap);
			}
		} catch (RNBaseException e) {
			logger.info("Validate failed: "+e.getMessage());
			ret = "Validate failed: "+e.getMessage();
		}
		return ret;
	}
	
	
	private IJobExecutor getJobExecutor(JobDataMap dataMap, JobDetails jdl) throws RNBaseException {
		LIVYEXE_TYPE ltype = LIVYEXE_TYPE.batch;
		try {
			ltype = LIVYEXE_TYPE.valueOf(jdl.getLoadType());
		}catch(Exception e) {}
		return getJobExecutor(dataMap.getWrappedMap(), jdl.getSparkUrl(), ltype);
	}	
	
	private IJobExecutor getJobExecutor(Map<String, Object> dataMap, String sparkurl, LIVYEXE_TYPE loadtype) throws RNBaseException {
		IJobExecutor executor;		
		Properties prop = new Properties();
		if(null != dataMap) {
			for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
				prop.put(entry.getKey(), entry.getValue().toString());
			}
		}
		
		if (!prop.contains("rnctech.maxResult"))
			prop.setProperty("rnctech.maxResult", "10");

		TechType techtype = TechType.pyspark;
		if (null != dataMap.get(RNJob.SCRIPT_TYPE))
			techtype = TechType.valueOf(dataMap.get(RNJob.SCRIPT_TYPE).toString().toLowerCase());

		switch (techtype) {
		case python: {
			if (!prop.contains("rnctech.gatewayserver_address"))
				prop.setProperty("rnctech.gatewayserver_address", "127.0.0.1");
			executor = new PythonExecutor(prop);
			break;
		}
		case pyspark: {
			prop.setProperty("livy.url", sparkurl);
			LivyPyExecutor lexr = new LivyPyExecutor(prop);
			lexr.setExetype(loadtype);
			executor = lexr;
			break;
		}
		case spark: {
			prop.setProperty("livy.url", sparkurl);
			LivyJavaExecutor ljexr = new LivyJavaExecutor(prop);
			ljexr.setExetype(loadtype);
			executor = ljexr;
			break;
		}
		case java:
		case scala: {
			executor = new JavaExecutor(prop);
			break;
		}
		case sql: {
			if (!prop.contains("livy.spark.sql.maxResult"))
				prop.setProperty("livy.spark.sql.maxResult", "100");
			executor = new LivySparkSQLExecutor(prop);
			break;
		}
		case shell: {
			executor = new ShellExecutor(prop);
			break;
		}
		default:
			executor = new DummyExecutor(prop);
		}
		executor.withAppContext(appContext);
		executor.withRepo(jobrepo, jdrepo);
		executor.setProperties(prop);
		return executor;
	}
}
