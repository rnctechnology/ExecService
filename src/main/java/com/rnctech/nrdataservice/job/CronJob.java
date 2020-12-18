package com.rnctech.nrdataservice.job;

import java.text.ParseException;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.config.RNScheduleConfig;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.JobPolicy;
import com.rnctech.nrdataservice.service.RNResult;

/**
 * @author Zilin Chen
 * @since 2020.10
 */

@DisallowConcurrentExecution
public class CronJob extends RNJob {
	
	public CronJob(){}
	
	public CronJob(JobPolicy poclicy) {
		super();
		this.policy = policy;
	}
	
    private JobPolicy policy;


	public CronTriggerFactoryBean getRNTrigger(JobDetail jobDetail) {
		CronTriggerFactoryBean ctfb = RNScheduleConfig.createCronTrigger(jobDetail,policy.getCronexpr());
		ctfb.setBeanName(getTriggerKeyName(FraqenceJob.class.getClass()));
		try {
			ctfb.afterPropertiesSet();
		} catch (ParseException e) {
		}
		return ctfb;
    }

	public JobDetailFactoryBean getRNJob(JobData config) {
		JobDetailFactoryBean jBean = RNScheduleConfig.createJobDetail(this.getClass());
		jBean.setBeanName(config.getJobkey());
		JobDataMap jobMap = buildJobMap(config);
		jBean.setJobDataMap(jobMap);
		jBean.afterPropertiesSet();
        return jBean;
    }

	@Override
	public RNResult executeInternal(JobExecutionContext jobContext, RNContext ctx) throws JobExecutionException {
		logger.info("Running Job With Trigger " + policy.getCronexpr());
		try {
			RNResult res = executor.exec("", ctx);
			return res;
		} catch (RNBaseException e) {
			throw new JobExecutionException(e);
		}	
	}

}
