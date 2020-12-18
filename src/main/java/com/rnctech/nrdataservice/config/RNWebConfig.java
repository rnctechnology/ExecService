package com.rnctech.nrdataservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * @contributor zilin
 * @since 2020.03
 */

@EnableWebMvc
@Configuration
public class RNWebConfig implements WebMvcConfigurer {
	
	
/*	@Autowired
	RNReqInterceptor rninterceptor;*/
    
    @Bean
    public ViewResolver getViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setSuffix(".html");
        return resolver;
    }
    
    @Bean
    public RNReqInterceptor rnReqInterceptor() {
    	return new RNReqInterceptor();
    }
	
    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
    
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rnReqInterceptor());
	}
	

}
