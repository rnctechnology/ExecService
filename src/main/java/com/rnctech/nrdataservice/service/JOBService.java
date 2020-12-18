package com.rnctech.nrdataservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.apis.JobStatus;
import com.rnctech.nrdataservice.config.AppConfig;
import com.rnctech.nrdataservice.config.FileConfig;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.BridgeJob;
import com.rnctech.nrdataservice.job.CronJob;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.job.SimpleJob;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobPolicy;
import com.rnctech.nrdataservice.repo.JobPolicyRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.job.FraqenceJob;
import com.rnctech.nrdataservice.job.JobInfo;
import com.rnctech.nrdataservice.job.RNJob;
import com.rnctech.nrdataservice.utils.AWSConnUtil;
import com.rnctech.nrdataservice.utils.ConfigClient;
import com.rnctech.nrdataservice.utils.RNUtilities;

/*
* @author zilin chen
* @since 2020.09
*/

@Service
public class JOBService implements RNConsts {

	public static Logger logger = Logger.getLogger(JOBService.class);
	public static Map<String, Process> tp = new ConcurrentHashMap<>();
	public static int MAX_WAITS = 6; 
	public static int MAX_WAITS_SECONDS = 120;  //2 mins
	
	@Autowired
	private FileConfig fileconfig;
	@Autowired
	private AppConfig appconfig;
	@Autowired
	private JobRepository jobrepo;
	@Autowired
	private JobDetailsRepository jdrepo;
	@Autowired
	private JobPolicyRepository jprepo;
	@Autowired
	private RNJobsListener rnjobListener;
	@Autowired
	private AuthService authService;
	
	private static String ctxpath;


	@Autowired
	private SchedulerFactoryBean schedFactory;

	private Scheduler getScheduler() throws SchedulerException {
		Scheduler scheduler = schedFactory.getScheduler();
		scheduler.getContext().put(FCONFIG, fileconfig);
		return scheduler;
	}
	
	public Job saveJob(JobData jobdata, boolean upsert) {
		if(upsert && -1 != jobdata.getPid()) {
			Job job = jobrepo.findById(jobdata.getPid()).get();
			if(null != job) {
				job.setStatus(Status.valueOf(jobdata.getStatus()).ordinal());
				if(null != jobdata.getDescription())job.setDescription(jobdata.getDescription());
				job.setLastModified(new Date());
				return jobrepo.saveAndFlush(job);
			}
		}
		
		Job job = new Job(jobdata);
		job.setJobExecutedBy(getCtxpath());	
		job.setJobGroup(groupName);
		job.setMrPassword(authService.encryptString(jobdata.getPassword()));
		JobPolicy jp = new JobPolicy(jobdata);
		//job.setJobpolicy(jp);
		Job j = jobrepo.saveAndFlush(job);
		jp.setJob(j);
		jprepo.saveAndFlush(jp);
		return j;
	}
	
	public JobDetails saveJobDetail(JobData jobdata, String zipfile) {
		Job job = saveJob(jobdata, true);
		JobDetails jd = new JobDetails(jobdata);
		jd.setJob(job);
		 if(null != zipfile) {
			  InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(zipfile);
			  if(inputStream != null) {
				  try {
					jd.setZips(IOUtils.toByteArray(inputStream));
				} catch (IOException e) {
					//
				}
			  }
		  }
		return jdrepo.save(jd);
	}

	public List<JobInfo> listRunningJobs(){
		List<Job> jobs = jobrepo.findRunningJob();
		List<JobInfo> js = jobs.stream().map(job -> new JobInfo(job)).collect(Collectors.toList()); 
		return js;
	}
	
	public List<JobInfo> listAllJobs(){
		List<Job> jobs = jobrepo.findAll();
		List<JobInfo> js = jobs.stream().map(job -> new JobInfo(job)).collect(Collectors.toList()); 
		return js;
	}
	
	public List<JobInfo> listTenantJobs(String tenant){
		List<Job> jobs = jobrepo.findByTenant(tenant);
		List<JobInfo> js = jobs.stream().map(job -> new JobInfo(job)).collect(Collectors.toList()); 
		return js;
	}
	
	public JobInfo getJobStatus(Long jid) throws Exception{
		Job job = jobrepo.findById(jid).get();
		if(null != job) {
			return new JobInfo(job);
		}else {
			throw new Exception("No Job found for run id as "+jid);
		}
	}
	
	public List<JobInfo> listTenantJob(String tenant, Long jobid) throws Exception {
		List<Job> jobs = jobrepo.findByTenantAndJobid(tenant, jobid);
		if(null != jobs && !jobs.isEmpty()) {
			List<JobInfo> js = jobs.stream().map(job -> new JobInfo(job)).collect(Collectors.toList()); 
			return js;
		}else {
			throw new Exception("No Job found for tenant "+tenant+ " and jobid "+jobid);
		}
	}
	
	public JobInfo getTenantRunningJobStatus(String tenant, Long jobid) throws Exception {
		List<Job> jobs = jobrepo.findByTenantAndJobid(tenant, jobid);
		if(null != jobs && !jobs.isEmpty()) {
			Job js = jobs.stream().filter(j -> j.getStatus() < 5).findFirst().get(); 
			if(null != js) {
				switch (js.getStatus()) {
				case 4:{
					Set<JobDetails> jds = js.getJobdetails();
					if(null != jds && !jds.isEmpty()) {
						JobDetails jd = jds.iterator().next();
						if(jd.getScriptType().equalsIgnoreCase(TechType.pyspark.name())) {
							int sid = jd.getSessionid();
							if(-1 != sid) {
							
							}
						}
					}
				}
				default:{
					return new JobInfo(js);
				}
				}
			}
		}
		throw new Exception("No Job running for tenant "+tenant+ " and jobid "+jobid);
	
	}

	public List<String> getScheduledJobs() throws SchedulerException {
		return getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName)).stream().map(Key::getName)
				.sorted(Comparator.naturalOrder()).collect(Collectors.toList());
	}

	public List<JobStatus> getTriggerJobsStatuses() throws SchedulerException {
		Scheduler scheduler = getScheduler();
		LinkedList<JobStatus> list = new LinkedList<>();
		for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
			JobDetail jobDetail = scheduler.getJobDetail(jobKey);
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
			for (Trigger trigger : triggers) {
				Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
				if (Trigger.TriggerState.COMPLETE.equals(triggerState)) {
					list.add(new JobStatus(jobKey.getName(), true));
				} else {
					list.add(new JobStatus(jobKey.getName(), false));
				}
			}
		}
		list.sort(Comparator.comparing(o -> o.id));
		return list;
	}

	public String schedulernctechJob(JobData job) {
		// Check if the job is already scheduled or running, prevent same job multiple run at same time
		boolean isrunning = checkIfJobRunning(job.getJobConfiguration().getName(), job.getJobid());
		int count = 0;
		while(isrunning && count < MAX_WAITS) {
			try {
				Thread.sleep(MAX_WAITS_SECONDS * 1000);
			} catch (InterruptedException e) {
			}
			isrunning = checkIfJobRunning(job.getJobConfiguration().getName(), job.getJobid());
			count ++;
		}
		
		if(isrunning) {
			job.setDescription("Job has been previously scheduled or is already running for tenant "+job.getJobConfiguration().getName()+" and jobid "+job.getJobid());
			return job.getDescription();
		}

		Job pjob = saveJob(job, false);	
		job.setPid(pjob.getId());
		
		String description = "Job is Scheduled!!";
		try {
			String scheduleType = job.getPolicy().getScheduleType();
			RNJob ajob = null;
			if (scheduleType.equalsIgnoreCase(SCHEDULETYPE.cron.name())) {
				ajob = new CronJob(job.getPolicy());
			} else if (scheduleType.equalsIgnoreCase(SCHEDULETYPE.repeat.name())) {
				ajob = new FraqenceJob(job.getPolicy());
			} else { // run once
				String loadtype = job.getJobConfiguration().getLoadType();
				int loadt = LOADTYPE.MEDIUM.ordinal();
				if(null != loadtype) {
					LOADTYPE lt = LOADTYPE.valueOf(loadtype.trim().toUpperCase());
					if(null != lt)
						loadt = lt.ordinal();
				}
				if(loadt > 2) {
					ajob = new BridgeJob(job.getPolicy());
				}else {
					ajob = new SimpleJob(job.getPolicy());
				}
			}
			ajob.setJob(job);
			
			if (job.getPolicy().isSchedule()) {
				description = job.getJobkey() + " scheduled " + scheduleType + " @" + new Date();
				JobDetail jd = ajob.getRNJob(job).getObject();
				logger.info("ready for job " + job.getJobkey() + " as detail " + jd + " for type " + scheduleType);
				Trigger t = ajob.getRNTrigger(jd).getObject();
				logger.info("Trigger as " + t + " start @ " + t.getStartTime());
				getScheduler().scheduleJob(jd, t);
			} else {
				boolean success = false;
				int retry = job.getPolicy().getRetry();
				while (retry >= 0 && !success) {
					try {
						description = "job scheduled @ " + new Date();
						logger.info(description);
						job.setDescription(description);
						JobDetail jd = ajob.getRNJob(job).getObject();	
						
						Trigger runOnceTrigger = ajob.getRNTrigger(jd).getObject();
						Scheduler scheduler = getScheduler();					
						scheduler.getListenerManager().addJobListener(
								rnjobListener, KeyMatcher.keyEquals(jd.getKey())
					    	);
						//logger.info("Trigger job " + job.getJobkey() + " as detail " + jd + " @ " + new Date());
						scheduler.scheduleJob(jd, runOnceTrigger);
						job.setDescription(description);
						ConfigClient.updateProperties(job, STATUS.INITIALIZED, description, new Date(), null);
						success = true;
					} catch (Throwable t) {
						logger.error("The job run as exception: " + t.getMessage());
						retry --;
						if(0 >= retry) {
							job.setDescription(t.getMessage());
							job.setStatus(Status.FAILED.name());
							saveJob(job, true);
							ConfigClient.updateProperties(job, STATUS.FAILED, t.getMessage(), null, new Date());
						}else { 
							try {
								Thread.sleep(job.getPolicy().getDelay());
							} catch (Exception e) {}
							logger.info("Retry(" + retry + ") job.");
						}
						
					}
				}
			}

		} catch (Exception e) {
			description = "Could not schedule a job. " + e.getMessage();
			try {
				job.setStatus(Status.FAILED.name());
				job.setDescription(description);
				saveJob(job, true);
				ConfigClient.updateProperties(job, STATUS.FAILED, description, null, new Date());
			} catch (Exception e1) {}
		}
		return description;
	}

	public String getExecutingJobs() throws Exception {
		StringBuilder rj = new StringBuilder();
		
		/*SchedulerFactoryBean schedFactory = appContext.getBean(SchedulerFactoryBean.class);
		List<JobExecutionContext> jobctxs = schedFactory.getScheduler().getCurrentlyExecutingJobs();*/
		List<JobExecutionContext> jobctxs = getScheduler().getCurrentlyExecutingJobs();
		jobctxs.forEach(jec -> rj.append(jec.getJobDetail().getKey()).append(","));
					
		/*List<JobData> jobs = tmap.entrySet().stream().map(x -> x.getValue()).collect(Collectors.toList());
		jobs.forEach(j -> rj.append(j.getJobkey()).append(","));*/
		
		return rj.toString();
	}

	public String getJobHistory(String tenantName) {
		StringBuilder sb = new StringBuilder();
		try {
			Scheduler scheduler = getScheduler();
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
					String jobName = jobKey.getName();
					String jobGroup = jobKey.getGroup();
					if (null == tenantName || (null != tenantName && jobGroup.startsWith(tenantName))) {
						List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
						Date nextFireTime = null;
						if(null != triggers && !triggers.isEmpty())
							nextFireTime = triggers.get(0).getNextFireTime();
						sb.append("[jobName] : " + jobName + " [groupName] : " + jobGroup + ((null == nextFireTime)?"":" - " + nextFireTime)+"\n");
					}
				}
			}
		} catch (SchedulerException e) {
			sb.append("No history available for tenant " + tenantName);
		}

		return sb.toString();
	}

	public boolean checkIfJobRunning(String tenantName, Long jobId) {
		List<Job> jobs = jobrepo.findByRunningJobid(tenantName, jobId);
		if(null != jobs && !jobs.isEmpty()) {
			Job j = jobs.get(0);
			JobDetails jd = jdrepo.findByTenantAndJobid(tenantName, j.getId());
			if(null != jd) {
				Status sts = Status.values()[jd.getStatus()];
				return !sts.isCompleted();
			}
		}
		
		return false; 
	}
	
	public JobInfo cancelScheduleJob(String tenant, String jobid) throws Exception {
		StringBuilder ret = new StringBuilder("Ready to cancel job ");
		long jid = Long.parseLong(jobid);
		List<Job> jobs = jobrepo.findByTenantAndJobid(tenant, jid);
		if(null != jobs && !jobs.isEmpty()) {
			Job jobdata = jobs.stream().filter(j -> j.getStatus() < 5).findFirst().get(); //.collect(Collectors.toList());
			//should only one running job found as we prevent multiple same job kicked off

		if (null != jobdata) {
			Scheduler scheduler = getScheduler();
			JobInfo jobinfo = new JobInfo(jobdata);
			logger.debug("Get job to cancel " + RNUtilities.getBeanToJsonString(jobdata));
			JobPolicy jc = jobdata.getJobpolicy();
			boolean isSchedule = jc.isIsschedule();
			String scheduleType = jc.getScheduletype();
			boolean unscheduled = false;
			if (isSchedule) {
				TriggerKey tkey = new TriggerKey(RNJob.getTriggerKeyName(FraqenceJob.class));
				if (scheduleType.equalsIgnoreCase("cron")) {
					tkey = new TriggerKey(RNJob.getTriggerKeyName(CronJob.class));
				}
				JobKey jkey = new JobKey(jobdata.getJobkey());
				ret.append(jkey.getName() + " trigger " + tkey.getName());
				try {
					scheduler.unscheduleJob(tkey);
					scheduler.deleteJob(jkey);
					ret.append("... successful unscheduled.");
					unscheduled = true;
					jobinfo.setStatus(Status.ABORT.name());
				} catch (SchedulerException e) {
					ret.append("\nError while unscheduling " + e.getMessage());
					logger.error(ret);
				}
			}

			if(!unscheduled) {
				try {
					JobKey toCancelJob = new JobKey(jobdata.getJobkey());
					ret.append(jobdata.getJobkey());
					List<JobExecutionContext> currentJobs = scheduler.getCurrentlyExecutingJobs();
					for (JobExecutionContext jobExecutionContext : currentJobs) {
						if (jobExecutionContext.getJobDetail().getKey().equals(toCancelJob)) {
							scheduler.interrupt(toCancelJob);
							scheduler.deleteJob(toCancelJob);
						}
					}
					jobinfo.setStatus(Status.ABORT.name());
					ret.append("... successful cancelled.");					
				} catch (Exception e) {
					ret.append("\nError while cancelling " + e.getMessage());
					logger.error(ret);
				}
			}
			jobinfo.setDescription(ret.toString());
			return jobinfo;
		}
		}
		throw new RNBaseException("No running job found for tenant "+tenant+" with jobid as "+jobid);
	}

	public static String getSystemIpAddress() {
		String ipAddr = "0.0.0.0";
		try {
			ipAddr = AWSConnUtil.getInstancePrivateIpAddress();
		} catch (Exception e) {
			try {
				InetAddress addr = InetAddress.getLocalHost();
				ipAddr = addr.getHostAddress();
			} catch (UnknownHostException e1) {
				ipAddr = "127.0.0.1";
			}
		}
		return ipAddr;
	}

	public String getBaseUrl(HttpServletRequest request) {
		return request.getScheme() + "://" + getSystemIpAddress() + ":" + request.getServerPort()
				+ request.getContextPath();
	}
	
	@PostConstruct
	public void setCtxpath() {
		this.ctxpath = "http://"+getSystemIpAddress() + ":" +appconfig.getPort()+appconfig.getCtxpath();
		logger.debug("server url: "+this.ctxpath);
	}

	public static String getCtxpath() {
		return ctxpath;
	}

	public static String getServerPath(String p) {
		return getCtxpath()+p;
	}
	
	
}
