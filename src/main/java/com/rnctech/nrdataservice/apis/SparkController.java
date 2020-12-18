package com.rnctech.nrdataservice.apis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.rnctech.nrdataservice.service.ISparkExecutionService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/v1")
public class SparkController {

	private static final Logger log = LoggerFactory.getLogger(SparkController.class);
	
	@Autowired
	ISparkExecutionService sparkExecutionService;

	@RequestMapping(value = "/checkconfig", method = RequestMethod.GET)
	@ResponseBody
	public String checkSparkConfig(@RequestParam(value = "yarnurl") String yarnURL) throws Exception {
		return sparkExecutionService.checkConfig(yarnURL);
	}

	@RequestMapping(value = "/heartbeat", method = RequestMethod.GET)
	@ResponseBody
	public String checkHeartbeat() throws Exception {
		return "Service is up";
	}

	@RequestMapping(value = "/getenvs", method = RequestMethod.GET)
	@ResponseBody
	public String getenvvariablevalue(@RequestParam(value = "variableName") String variableName) throws Exception {
		return System.getenv(variableName);
	}
	
	@RequestMapping(value = "/launch", method = RequestMethod.POST)
	@ResponseBody
	public String launchPrediction(@RequestParam(value = "fileName") String fileName, @RequestBody String properties) throws Exception {
		return sparkExecutionService.launch(fileName, new JSONObject(properties));
	}
	
	
}
