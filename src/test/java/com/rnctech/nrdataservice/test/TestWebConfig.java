package com.rnctech.nrdataservice.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * @contributor zilin
 * @since 2020.03
 */

@EnableWebMvc
@Configuration
public class TestWebConfig implements WebMvcConfigurer {
	
	@Value("${rnctech.profile:QA}")
	private String profile;
	
    public String getProfile() {
		return profile;
	}


	@Bean
    public ViewResolver getViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setSuffix(".html");
        return resolver;
    }
    
	
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
    
	

}
