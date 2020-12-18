package com.rnctech.nrdataservice.apis;


import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rnctech.nrdataservice.service.StatusService;
import com.rnctech.nrdataservice.utils.RNUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @contributor zilin
 * @since 2020.08
 */

@RestController
public class RNController {

	private static final Logger log = LoggerFactory.getLogger(RNController.class);

	@Autowired
	private Environment env;
	
	@Autowired
	private StatusService statusService;
	
	@RequestMapping("/ping")
	public String ping() {
		return "Ping Successed!";
	}
	
	@RequestMapping(value = "/logger", method = RequestMethod.GET)
	public String getloginfo(@RequestParam(value = "loggerName") Optional<String> loggerName) {
		String lName = loggerName.orElse(RNUtilities.rnctech_PACKAGE);
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(lName);
		return lName + ": " + logger.getLevel().toString() + " " + logger.getAllAppenders().nextElement().toString();
	}

	@RequestMapping(value = "/testlog", method = RequestMethod.GET)
	public String testloglevel(@RequestParam(value = "loggerName") Optional<String> loggerName) {
		String lName = loggerName.orElse(RNUtilities.rnctech_PACKAGE);
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(lName);
		logger.trace("This is trace??");
		logger.debug("This is debug message?");
		logger.info("This is information.");
		logger.warn("This is warning..");
		logger.error("This is error message!");
		logger.fatal("This is fatal case!!");
		return getloginfo(Optional.of(lName));
	}
	
	@RequestMapping(value = "/logger/{loglevel}", method = RequestMethod.PUT)
	public String loglevel(@PathVariable("loglevel") String logLevel,  
			@RequestParam(value = "loggerName") Optional<String> loggerName) throws Exception {
			String lName = loggerName.orElse(RNUtilities.rnctech_PACKAGE);
			if (lName.equalsIgnoreCase("ALL") || lName.equalsIgnoreCase("*"))
				lName = "root";
			String l = RNUtilities.setLogLevel(lName, logLevel);
		log.warn("Reset to level " + l + " for logger " + lName);
			
		return getloginfo(Optional.of(lName));
	}
	
	@RequestMapping(value = "/validate", method = RequestMethod.GET)
	public String validate(@RequestParam("sparkurl") String sparkurl) {
		if(null == sparkurl || sparkurl.trim().isEmpty())
			return "sparkurl can't be empty, pass request param as ?sparkurl=http://10.0.0.8:8998";
		return statusService.validateSparkExecutor(sparkurl);
	}

	@RequestMapping(value="/about", method = RequestMethod.GET)
	public Map<?,?> about() throws IOException {
		Properties props = new Properties();
		props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("build.properties"));
		return props;
	}
}
