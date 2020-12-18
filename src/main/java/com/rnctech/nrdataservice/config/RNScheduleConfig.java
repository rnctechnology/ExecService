package com.rnctech.nrdataservice.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;
import javax.sql.DataSource;
import org.joda.time.LocalDateTime;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import com.rnctech.nrdataservice.service.RNJobsListener;
import liquibase.integration.spring.SpringLiquibase;

/**
 * @author Zilin Chen
 * @since 2020.10
 */

@Configuration
@ConditionalOnProperty(name = "quartz.enabled")
@EnableAsync
@EnableScheduling
public class RNScheduleConfig {

    @Autowired
    DataSource dataSource;
    
	@Bean
	public JobFactory jobFactory(ApplicationContext applicationContext, SpringLiquibase springLiquibase) {
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

	@Bean
	public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource,
			JobFactory jobFactory, RNJobsListener jobsListenerService) throws IOException {
		SchedulerFactoryBean factory = new SchedulerFactoryBean();
		factory.setOverwriteExistingJobs(true);
		factory.setSchedulerName("Exec_Scheduler");
		factory.setAutoStartup(true);  //If you need to disable launching of jobs, set it to false.
		factory.setDataSource(dataSource);
		factory.setJobFactory(jobFactory);
		factory.setQuartzProperties(quartzProperties());
		factory.setGlobalJobListeners(jobsListenerService);
		return factory;
	}
	
/*    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.mariadb.jdbc.Driver");
        dataSourceBuilder.url("jdbc:mariadb://localhost:3307/rnjob?createDatabaseIfNotExist=true&useSSL=false");
        dataSourceBuilder.username("root");
        dataSourceBuilder.password("");
        return dataSourceBuilder.build();
    }*/

	public Properties quartzProperties() throws IOException {
		PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
		propertiesFactoryBean
				.setLocation(new ClassPathResource("/quartz.properties"));
		propertiesFactoryBean.afterPropertiesSet();
		return propertiesFactoryBean.getObject();
	}

	
	//Trigger runOnceTrigger = newTrigger().withIdentity(job.getJobkey() + "-trigger", groupName).startNow().withSchedule(simpleSchedule().withRepeatCount(0)).build();
	public static SimpleTriggerFactoryBean onetimeTrigger(JobDetail job) {
	    SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
	    factoryBean.setJobDetail(job);
	    factoryBean.setName(job.getKey().getName()+"-trigger"+System.currentTimeMillis());
	    factoryBean.setRepeatCount(0);
	    factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
	    return factoryBean;
	}
	
	
	//default Triggers
	public static SimpleTriggerFactoryBean createTrigger(JobDetail jobDetail,
			long pollFrequencyMs) {
		SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
		factoryBean.setName("exec-simple-trigger"+System.currentTimeMillis());
		factoryBean.setJobDetail(jobDetail);
		factoryBean.setStartDelay(100L);
		factoryBean.setRepeatInterval(pollFrequencyMs);
		factoryBean.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		// in case of misfire, ignore all missed triggers and continue :
		factoryBean.setMisfireInstruction(
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        try {
            factoryBean.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
		return factoryBean;
	}
	
	public static CronTriggerFactoryBean createCronTrigger(JobDetail jobDetail,
			String cronExpression) {
		RNCronTriggerFactoryBean factoryBean = new RNCronTriggerFactoryBean();
		factoryBean.setJobDetail(jobDetail);
		factoryBean.setCronExpression(cronExpression);		
        factoryBean.setName("exec-cron-trigger"+System.currentTimeMillis());
        factoryBean.setStartTime(LocalDateTime.now().toDate());
        factoryBean.setEndTime(LocalDateTime.now().plusMinutes(30).toDate());
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);

        try {
            factoryBean.afterPropertiesSet();
        } catch (ParseException e) {
            e.printStackTrace();
        }
		return factoryBean;
	}

	public static JobDetailFactoryBean createJobDetail(Class jobClass) {
		JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
		factoryBean.setJobClass(jobClass);		
		factoryBean.setDurability(true);  // job has to be durable to be stored in DB.
        factoryBean.setName("exec-job"+System.currentTimeMillis());
		factoryBean.afterPropertiesSet();
		return factoryBean;
	}

	public static class RNCronTriggerFactoryBean extends CronTriggerFactoryBean {
	    private Date endTime;
	    public void setEndTime(Date endTime) {
	        this.endTime = endTime;
	    }

	    @Override
	    public void afterPropertiesSet() throws ParseException {
	        super.afterPropertiesSet();


	        if (super.getObject() != null) {
	            CronTriggerImpl object = (CronTriggerImpl) super.getObject();
	            object.setEndTime(endTime);
	        }
	    }
	}
	
	public final class AutowiringSpringBeanJobFactory
			extends SpringBeanJobFactory implements ApplicationContextAware {

		private transient AutowireCapableBeanFactory beanFactory;

		@Override
		public void setApplicationContext(final ApplicationContext context) {
			beanFactory = context.getAutowireCapableBeanFactory();
		}

		@Override
		protected Object createJobInstance(final TriggerFiredBundle bundle)
				throws Exception {
			final Object job = super.createJobInstance(bundle);
			beanFactory.autowireBean(job);
			return job;
		}
	}
}
