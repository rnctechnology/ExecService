package com.rnctech.nrdataservice.test;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

/**
 * @author zilin
 * @2020.10
 */

public class YamlFileApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
	  @Override
	  public void initialize(ConfigurableApplicationContext applicationContext) {
	    try {
	        Resource resource = applicationContext.getResource("classpath:application-test.yml");
	        YamlPropertySourceLoader sourceLoader = new YamlPropertySourceLoader();
	        List<PropertySource<?>> yamlTestProperties = sourceLoader.load("rnTestProperties", resource);
	        yamlTestProperties.forEach(p -> applicationContext.getEnvironment().getPropertySources().addFirst(p));
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
	  }
	}