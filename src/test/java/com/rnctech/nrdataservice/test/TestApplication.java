package com.rnctech.nrdataservice.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.AbstractEnvironment;
import com.rnctech.nrdataservice.ExcludeFromTest;
import org.springframework.context.annotation.FilterType;

/**
 * @contributor zilin
 * 2020.09
 */

@Configuration
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = ExcludeFromTest.class))
@EnableAutoConfiguration
public class TestApplication {

	     public static void main(String[] args) throws Exception {
	    	 System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "test");
	    	 System.setProperty("jasypt.encryptor.password", "rnctech123!");
	    	 SpringApplication.run(TestApplication.class, args);
	     }
	 }
