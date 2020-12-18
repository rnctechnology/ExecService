package com.rnctech.nrdataservice.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.config.RNScheduleConfig;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.JobPolicy;
import com.rnctech.nrdataservice.service.RNResult;

/**
 * @author Zilin Chen
 * @since 2020.10
 *
 */

@DisallowConcurrentExecution
public class FraqenceJob extends RNJob {

	public FraqenceJob() {	
	}
	
	public FraqenceJob(JobPolicy policy) {
		super();
		this.policy = policy;
	}

	private JobPolicy policy;

	public SimpleTriggerFactoryBean getRNTrigger(JobDetail jobDetail) {
		SimpleTriggerFactoryBean stfb = RNScheduleConfig.createTrigger(jobDetail, policy.getFrequency());
		stfb.setBeanName(getTriggerKeyName(FraqenceJob.class.getClass()));
		stfb.afterPropertiesSet();
		return stfb;
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
	public RNResult executeInternal(JobExecutionContext jobExecutionContext, RNContext ctx) throws JobExecutionException {
		logger.info("Running Job With Trigger " + policy.getFrequency());
		try {
			RNResult res = executor.exec("", ctx);
			return res;
		} catch (RNBaseException e) {
			throw new JobExecutionException(e);
		}	
		
	}

}
