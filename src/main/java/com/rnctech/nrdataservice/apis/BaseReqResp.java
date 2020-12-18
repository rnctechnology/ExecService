package com.rnctech.nrdataservice.apis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.rnctech.nrdataservice.job.JobData;

/*
 * @contributor zilin
 * 2020.03
 */

public class BaseReqResp {
	
	private static final Logger logger = LoggerFactory.getLogger(BaseReqResp.class);
	
	protected ResponseEntity<JobData> asBadReguest(JobData jobdata, String msg, HttpStatus hstat){
		jobdata.setDescription(msg);
		//jobdata.setUser(StringUtils.repeat("U", jobdata.getUser().length()));
		jobdata.setPassword(StringUtils.repeat("*", jobdata.getPassword().length()));
		logger.error(msg);
		return new ResponseEntity<JobData>(jobdata, hstat);
	}
}
