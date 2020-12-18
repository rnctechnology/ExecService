package com.rnctech.nrdataservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
//import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * @author zilin
 * @since 2020.09
 */

@Configuration
@ComponentScan({ "com.rnctech.nrdataservice"})
@EnableAspectJAutoProxy
@EnableConfigurationProperties
@ConfigurationProperties
public class AppConfig {

	@Autowired
	Environment environment;
	
	private String env;
	
    @Value("${server.servlet.context-path}")
    private String ctxpath;
    
    @Value("${server.port}")
    private int port;

	@Bean
	public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
	   return builder.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
	
	@Bean(name = "threadPoolTaskExecutor")
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(estimateThreads());
	    executor.setMaxPoolSize(2*estimateThreads());
	    executor.setThreadNamePrefix("rnctechJS_exec");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.setAwaitTerminationSeconds(30);
	    return executor;
	}
	
	public static int estimateThreads() {
		int ps = Runtime.getRuntime().availableProcessors();
		long fm = Runtime.getRuntime().freeMemory() / (1024 * 1024);
		if( fm > ps * 128 )
			return ps * 4;
		else
			return ps * 2;
		
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public String getCtxpath() {
		return ctxpath;
	}

	public int getPort() {
		return port;
	}
	
/*    @Bean(name = "threadPoolTaskScheduler")
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(estimateThreads());
        threadPoolTaskScheduler.setThreadNamePrefix("rnctechJS_sche");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }*/

}
