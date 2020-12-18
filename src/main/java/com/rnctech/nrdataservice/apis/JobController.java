package com.rnctech.nrdataservice.apis;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.rnctech.nrdataservice.job.JobInfo;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.service.JOBService;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.StatusService;
import com.rnctech.nrdataservice.utils.ConfigClient;
import com.rnctech.nrdataservice.utils.RNUtilities;

/*
 * @contributor zilin
 * 2020.10
 * 
 * Main controller
 */

@RestController
@RequestMapping(value = "/api/v1/rnc")
public class JobController {
	
	private static final Logger logger = LoggerFactory.getLogger(JobController.class);

	@Autowired
	private JOBService jobservice;
	
	@Autowired
	private StatusService statusService;
	
	@Autowired
	private Environment env;
	
	@RequestMapping(value = "/execute/{name}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> execute(@PathVariable("name") String tenantName,
			@RequestBody JobData jobdata , HttpServletRequest request,
			HttpServletResponse response) throws ClientProtocolException, IOException {
				
		logger.debug("\n"+RNUtilities.toString(jobdata));
		MDC.put("tenant", tenantName);
		Long jobid = jobdata.getJobid();
		if (null == jobid || jobid == -1l) {
			return asBadReguest(jobdata, "No job id found in your request.", HttpStatus.BAD_REQUEST);			
		}
		
		MDC.put("jobid", String.valueOf(jobid));
		if(null == jobdata.getJobConfiguration()) {
			return asBadReguest(jobdata, "No job configuration found in your request.", HttpStatus.BAD_REQUEST);
		}
		
		logger.info("Get job request from "+ tenantName+" with mr as "+jobdata.getconfigurl()+ " for jobid="+jobdata.getJobid()+ " @"+RNUtilities.getUTCTimeString(new Date()));
		if(null == jobdata.getJobConfiguration().getName())
			jobdata.getJobConfiguration().setName(tenantName);
		
		String jobtype = jobdata.getJobConfiguration().getJobType();
		if(jobtype.equalsIgnoreCase("SCRIPT") || jobtype.equalsIgnoreCase("CLASS") ||jobtype.equalsIgnoreCase("ANALYSIS") ) {
			String codesnap =  jobdata.getJobConfiguration().getCodeSnap();
			if(null == codesnap || codesnap.isEmpty()) {
				return asBadReguest(jobdata, "No script or uri found for job type "+jobtype, HttpStatus.BAD_REQUEST);
			}
		}	
			
		String runIdentifier = RNUtilities.getRunID(tenantName, jobdata.getJobid());
		String runip = JOBService.getSystemIpAddress();
		if(null == jobdata.getJobName()) {			
			jobdata.setJobName(runIdentifier+"_"+runip);
		}else {
			jobdata.setJobName(jobdata.getJobName().trim()+"_"+RNUtilities.getUTCTimeStringShort(new Date()));
		}
		jobdata.setJobkey(runIdentifier);
		jobdata.setDescription("Get Job request for tenant " + tenantName + " @ " + new Date());
		logger.info(tenantName+" "+jobdata.getJobid()+" "+jobtype+" "+jobdata.getJobConfiguration().getScriptType()+" "+RNUtilities.getSufString(jobdata.getJobConfiguration().getCodeSnap(), 50));
		if(null != jobdata.getJobConfiguration().getJobProperties()) logger.info("with JobProperties:\n"+RNUtilities.toString(jobdata.getJobConfiguration().getJobProperties()));
		if(null != jobdata.getJobConfiguration().getParams()) logger.info("and Params:\n"+RNUtilities.toString(jobdata.getJobConfiguration().getParams()));
		String ret = jobservice.schedulernctechJob(jobdata);
		jobdata.setDescription(ret);
		MDC.remove("tenant");
		MDC.remove("jobid");
		ConfigClient.close(jobdata);
		jobdata.setPassword(StringUtils.repeat("*", jobdata.getUser().length()));
		return new ResponseEntity<JobData>(jobdata, HttpStatus.OK);
	}

	private ResponseEntity<JobData> asBadReguest(JobData jobdata, String msg, HttpStatus hstat){
		jobdata.setDescription(msg);
		jobdata.setPassword(StringUtils.repeat("*", jobdata.getPassword().length()));
		logger.error(msg);
		return new ResponseEntity<JobData>(jobdata, hstat);
	}
	
	@RequestMapping(value = "/list/{name}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> listJobs(@PathVariable("name") String tenantName, HttpServletRequest request, HttpServletResponse response) {		
		try {
			List<JobInfo> jobs = jobservice.listTenantJobs(tenantName);
			jobs.forEach(j -> {
				logger.info(j.getJobId()+" "+j.getExecutionInfo());
			});
			return new ResponseEntity<List<JobInfo>>(jobs, HttpStatus.OK);
		}catch(Throwable t) {
			return new ResponseEntity<String>(tenantName+": "+t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value="/list/{name}/{jobid}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> listJobById(@PathVariable("name") String tenantName, @PathVariable("jobid") String jobid, HttpServletRequest request,
			HttpServletResponse response) {
		
		if(StringUtils.isEmpty(tenantName) || StringUtils.isEmpty(jobid)){
			return new ResponseEntity<String>(" No tenant or jobid found.", HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobInfo> jobs = jobservice.listTenantJob(tenantName, Long.parseLong(jobid));
			return new ResponseEntity<List<JobInfo>>(jobs, HttpStatus.OK);			
		}catch(Throwable t) {
			return new ResponseEntity<String>("No Job found for "+tenantName+" with jobid as "+jobid, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/status/{jobrunid}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> getJob(@PathVariable("jobrunid") String jobrid, HttpServletRequest request, HttpServletResponse response) {			
		try {
			JobInfo job = jobservice.getJobStatus(Long.parseLong(jobrid.trim()));
			return new ResponseEntity<JobInfo>(job, HttpStatus.OK);			
		}catch(Throwable t) {
			return new ResponseEntity<String>(jobrid + ": " +t.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/status/{name}/{jobrunid}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> getJobStatus(@PathVariable("name") String tenant, @PathVariable("jobrunid") String jobrid, HttpServletRequest request, HttpServletResponse response) {		
		/*if(null == jobdata.getJobConfiguration() || null == jobdata.getJobConfiguration().getTenantName()) {
			return asBadReguest(jobdata, " No tenant found.", HttpStatus.BAD_REQUEST);
		}*/
		try {
			RNResult res = statusService.getJobStatus(tenant, Long.parseLong(jobrid.trim()));
			return new ResponseEntity<RNResult>(res, HttpStatus.OK);			
		}catch(Throwable t) {
			return new ResponseEntity<String>(tenant+" with runid "+jobrid + ": " +t.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value="/cancel/{name}/{jobrunid}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<?> cancel(@PathVariable("name") String tenantName, @PathVariable("jobrunid") String jobrid, @RequestBody JobData jobdata, HttpServletRequest request,
			HttpServletResponse response) {
		
		if(StringUtils.isEmpty(tenantName) || StringUtils.isEmpty(jobrid)){
			String errmsg = " No tenant or jobid found.";
			logger.error(this.getClass().getCanonicalName()+errmsg);
			return asBadReguest(jobdata, errmsg, HttpStatus.BAD_REQUEST);
		}
		
		try {
			String ret = statusService.cancelJob(tenantName, Long.parseLong(jobrid.trim()));				
	/*		JobData jobdata = jobservice.cancelAdapterJob(name, jobrid);
			if(null == jobdata || -1l == jobdata.getJobid()){
				String infomsg = " No Job found for id = "+ jobrid+ " either job already been completed or it never been scheduled.";
				logger.info(this.getClass().getCanonicalName()+infomsg);
				return new ResponseEntity<String>(infomsg, HttpStatus.OK);
			}else{
				logger.info(Messages.getMessage("LDB041070", jobrid));
			}*/
			return new ResponseEntity<String>(ret, HttpStatus.OK);				
		}catch(Throwable t) {
			return asBadReguest(jobdata, t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}
