package com.rnctech.nrdataservice.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.RNConsts.AlgorithmType;
import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.config.RNScheduleConfig;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.JobPolicy;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.service.IJobExecutor;
import com.rnctech.nrdataservice.service.JOBService;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.livyimpl.BaseLivyExecutor;

/**
 * @author Zilin Chen
 * @since 2020.10
 */

@DisallowConcurrentExecution
public class SimpleJob extends RNJob {
	
	public SimpleJob() {};

	public SimpleJob(JobPolicy policy){
		super();
		this.policy = policy;
	}	
	
    private JobPolicy policy;

	private static final Logger logger = Logger.getLogger(SimpleJob.class);

	@Override
	public RNResult executeInternal(JobExecutionContext jobContext, RNContext ctx)
			throws JobExecutionException {
		try {
			executor.open();
			logger.info("Running Job With " + executor.getClass());
			String sc = getCodeSnap(jobContext.getJobDetail().getJobDataMap(), ctx);
			persistStatus(jobContext, executor, sc, ctx);
			RNResult res = executor.exec(sc, ctx);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			executor.close();
			stopFlag.set(true);
			return res;
		} catch (RNBaseException e) {
			throw new JobExecutionException(e);
		}
	}
	
	private void persistStatus(JobExecutionContext context, IJobExecutor executor, String sc, RNContext ctx) {
		try {
			JobDataMap datamap = context.getJobDetail().getJobDataMap();
			Long jobpid = datamap.getLong(RNConsts.JOB_PROC_ID);
			Job job = jobrepo.findById(jobpid).get();
			ctx.setCtxid(String.valueOf(jobpid));
			if(null != job) {
				JobDetails jdl = new JobDetails(ctx);
				jdl.setJobname(job.getJobname());
				if((executor instanceof BaseLivyExecutor)) {
					BaseLivyExecutor livyexector =(BaseLivyExecutor)executor;
					if(-1 != livyexector.getSessionid()) jdl.setSessionid(livyexector.getSessionid());
					if(null != livyexector.getSessionInfo()) {					
						jdl.setAppid(livyexector.getSessionInfo().appId);
					}
					jdl.setLoadType(livyexector.getExetype().name());					
					jdl.setSparkUrl(livyexector.getProperty("livy.url"));
				}
				jdl.setCodeSnap(sc);
				jdl.setTenant(datamap.getString("name"));
					/*if(null == job.getJobdetails())
						job.setJobdetails(new HashSet<JobDetails>());
					job.getJobdetails().add(jdl);*/
				jdl.setJob(job);
				jobdetailrepo.save(jdl);
				job.setStatus(Status.RUNNING.ordinal());    		
				job.setActive(true);
				job.setLastModified(new Date());				
				jobrepo.saveAndFlush(job);
			}
		} catch (Exception e) {
			logger.info("update job status failed: "+e.getMessage());
		}		
	}

	private String getCodeSnap(JobDataMap jobDataMap, RNContext ctx) {	
		
		TechType techtype = TechType.pyspark;
		if(null != jobDataMap.getString(SCRIPT_TYPE))
			techtype = TechType.valueOf(jobDataMap.getString(SCRIPT_TYPE).toLowerCase());
			
		String code = RNConsts.DEFAULT_PYSPARK_CODESNAP;
		if(null != ctx.getCodesnap()) {
			code = ctx.getCodesnap();
			return code;
		}
		logger.info("Try to execute: "+code);
		return code;
	}

	public SimpleTriggerFactoryBean getRNTrigger(JobDetail jobDetail) {
		SimpleTriggerFactoryBean factoryBean = RNScheduleConfig.onetimeTrigger(jobDetail);    //new SimpleTriggerFactoryBean();
		factoryBean.setGroup(JOBService.groupName);
		factoryBean.setStartDelay(100l);
		factoryBean.setBeanName(getTriggerKeyName(SimpleJob.class.getClass()));
		factoryBean.afterPropertiesSet();
		return factoryBean;
	}
	
	public JobDetailFactoryBean getRNJob(JobData config) {
		JobDetailFactoryBean jBean = RNScheduleConfig.createJobDetail(this.getClass());
		jBean.setBeanName(config.getJobkey());
		jBean.setDurability(true);
		JobDataMap jobMap = buildJobMap(config);
		jBean.setJobDataMap(jobMap);
		jBean.setGroup(config.getJobConfiguration().getName());
		if(null != config.getJobkey()) {
			jBean.setName(config.getJobkey());
		}else {
			jBean.setName(config.getJobName());
		}
		jBean.setDescription(config.getDescription());
		jBean.afterPropertiesSet();
        return jBean;
    }
}
