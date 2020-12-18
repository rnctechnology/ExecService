package com.rnctech.nrdataservice.apis;


import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobInfo;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.service.AuthService;
import com.rnctech.nrdataservice.service.JOBService;
import com.rnctech.nrdataservice.service.StatusService;

/*
 * @contributor zilin
 * 2020.01
 * 
 * Job Schedule controller
 */

@RestController
@RequestMapping(value = "/api/v1/schedule")
public class ScheduleController extends BaseReqResp {
	
	private static final Logger logger = LoggerFactory.getLogger(ScheduleController.class);

	@Autowired
	private JOBService jobservice;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private StatusService statusService;
	
	@Autowired
	private Environment env;
	

	private ResponseEntity<JobData> asBadReguest(JobData jobdata, String msg){
		jobdata.setDescription(msg);	
		logger.error(msg);
		return new ResponseEntity<JobData>(jobdata, HttpStatus.BAD_REQUEST);
	}
	
	@RequestMapping(value="/cancel/{name}/{jobid}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<?> cancel(@PathVariable("name") String name, @PathVariable("jobid") String jobid, HttpServletRequest request,
			HttpServletResponse response) {
		
		if(StringUtils.isEmpty(name) || StringUtils.isEmpty(jobid)){
			String errmsg = " No customer or jobid found.";
			logger.error(this.getClass().getCanonicalName()+errmsg);
			return new ResponseEntity<String>(errmsg, HttpStatus.BAD_REQUEST);
		}
			
		try {
			JobInfo jobdata = jobservice.cancelScheduleJob(name, jobid);
			if(null == jobdata || -1l == jobdata.getJobId()){
				String infomsg = " No Job found for id = "+ jobid+ " either job already been completed or it never been scheduled.";
				logger.info(this.getClass().getCanonicalName()+infomsg);
				return new ResponseEntity<String>(infomsg, HttpStatus.OK);
			}
			return new ResponseEntity<JobInfo>(jobdata, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<String>> getJobs()
        throws SchedulerException {
        List<String> ids = jobservice.getScheduledJobs();
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ids);
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<JobStatus>> getJobsStatuses()
        throws SchedulerException {
        List<JobStatus> jobs = jobservice.getTriggerJobsStatuses();
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(jobs);
    }
    
	@RequestMapping(value = "/status/{name}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> getJobHistory(@PathVariable("name") String name, HttpServletRequest request, HttpServletResponse response) {	
		
		String execjobs = (name.equalsIgnoreCase("ALL"))?jobservice.getJobHistory(null):jobservice.getJobHistory(name);		
		return new ResponseEntity<String>(execjobs, HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/validate/{type}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> validateExecitor(@PathVariable("type") String type, @RequestBody JobData jobdata , HttpServletRequest request,
			HttpServletResponse response) {	
		
		try {
			if(null == jobdata.getJobConfiguration()) throw new Exception("Not valid payload");
			JobConfig jc = jobdata.getJobConfiguration();			
			if(null == jc.getLoadType() || !jc.getJobType().equals("VALIDATE")) throw new Exception("Not valid json.");
					
			String url = jobdata.getconfigurl();
			String user = jobdata.getUser();
			String pwd = jobdata.getPassword();
			
			boolean yn= authService.login(url, user, pwd, jc.getName());
			if(yn) {
				String vs = "";
				if(type.equalsIgnoreCase("config") && url.startsWith("http:")) {
					vs = "Validate passed for name "+jc.getName()+ " with url "+url;
				}else {
					String sparkurl = jc.getSparkURL();
					if(null == sparkurl || sparkurl.trim().isEmpty()) {
						vs = statusService.validatePythonExecutorLocally();
					}else {
						vs = statusService.validateSparkExecutor(sparkurl);
					}
				}
				return new ResponseEntity<String>(vs, HttpStatus.OK);
			
		}else {
			return asBadReguest(jobdata, "Not authenticated user!", HttpStatus.UNAUTHORIZED);
		}
	}catch(Exception e) {
		return asBadReguest(jobdata, e.getMessage(), HttpStatus.BAD_REQUEST);
	}
		
		
		
	}
}
