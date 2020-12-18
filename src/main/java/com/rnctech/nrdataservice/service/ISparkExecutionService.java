package com.rnctech.nrdataservice.service;

import org.json.JSONObject;

public interface ISparkExecutionService {
	String launch(String fileName, JSONObject properties) throws Exception;
	String checkConfig(String yarnURL) throws Exception;
}
