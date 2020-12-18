package com.rnctech.nrdataservice.apis;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.rnctech.nrdataservice.exception.AuthException;
import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobInfo;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.service.AuthService;
import com.rnctech.nrdataservice.service.JOBService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @contributor zilin
 * @since 2020.03
 * APIs for Super user
 */

@RestController
@RequestMapping(value = "/api/v1/admin")
public class AdminController {

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	@Autowired
	private Environment env;

	@Autowired
	private AuthService authService;
	
	@Autowired
	private JOBService jobservice;
	
	@RequestMapping(value = "/encrypt", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> execute(@RequestBody JobData jobdata , HttpServletRequest request,
			HttpServletResponse response) throws ClientProtocolException, IOException {
		try {
			if(null == jobdata.getJobConfiguration()) throw new Exception("Not valid payload");
			JobConfig jc = jobdata.getJobConfiguration();
			
			if(null == jc.getLoadType() || !jc.getLoadType().equals("ADMINJOB") || !jc.getJobType().equals("ENCDEC")) throw new Exception("Not valid payload");
			 
			if(null == jc.getName())  throw new Exception("Not valid payload");
			
			String url = jobdata.getconfigurl();
			String user = jobdata.getUser();
			String pwd = jobdata.getPassword();
			boolean yn= authService.adminlogin(url, user, pwd, jc.getName());
			if(yn) {
				if(null == jobdata.getJobConfiguration().getJobProperties() || null == jobdata.getJobConfiguration().getJobProperties().get("TEXT"))
					throw new Exception("Not valid payload");
								
				String text = jobdata.getJobConfiguration().getJobProperties().get("TEXT");
				String enc = authService.encryptString(text, env.getProperty("jasypt.encryptor.password"));
				String encd = text;
				String decd = text;
				try {
					encd = authService.encryptString(text);
				}catch(Throwable t) {}
				try {
					decd = authService.decryptString(text);
				}catch(Throwable t) {}
				String dec = authService.decryptString(enc, env.getProperty("jasypt.encryptor.password"));
	
				jobdata.setDescription(text+" encrypted as: "+enc+" | "+dec+" | "+encd+" | "+decd);
				jobdata.setUser(null);
				jobdata.setPassword(null);
				return new ResponseEntity<JobData>(jobdata, HttpStatus.OK);			
			}else {
				return asBadReguest(jobdata, "Not authenticated user!", HttpStatus.UNAUTHORIZED);
			}
		}catch(Exception e) {
			return asBadReguest(jobdata, e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
		
	@RequestMapping(value = "/runningjobs", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> listAllRunningJobs(@RequestBody JobData jobdata, HttpServletRequest request, HttpServletResponse response) {		
		try {
			if(null == jobdata.getJobConfiguration() || null == jobdata.getJobConfiguration().getName()) {
				return asBadReguest(jobdata, " No name found.", HttpStatus.BAD_REQUEST);
			}
			JobConfig jc = jobdata.getJobConfiguration();
			if(null == jc.getLoadType() || !jc.getLoadType().equals("ADMINJOB")) {
				return asBadReguest(jobdata, " Invalid payload.", HttpStatus.BAD_REQUEST);
			}
			
			boolean yn= authService.adminlogin(jobdata.getconfigurl(), jobdata.getUser(), jobdata.getPassword(), jobdata.getJobConfiguration().getName());
			if(!yn) throw new AuthException("Authenticate failed with "+jobdata.getconfigurl());
			List<JobInfo> jobs = jobservice.listRunningJobs();
			jobs.forEach(j -> {
				logger.info(j.getJobId()+" "+j.getStatus()+" "+j.getExecutionInfo());
			});
			return new ResponseEntity<List<JobInfo>>(jobs, HttpStatus.OK);
		}catch(AuthException e) {
			return asBadReguest(jobdata, e.getMessage(), HttpStatus.UNAUTHORIZED);
		}catch(Throwable t) {
			return asBadReguest(jobdata, t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/jobs", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> listAllJobs(@RequestBody JobData jobdata, HttpServletRequest request, HttpServletResponse response) {		
		try {
			if(null == jobdata.getJobConfiguration() || null == jobdata.getJobConfiguration().getName()) {
				return asBadReguest(jobdata, " No name found.", HttpStatus.BAD_REQUEST);
			}
			
			JobConfig jc = jobdata.getJobConfiguration();
			if(null == jc.getLoadType() || !jc.getLoadType().equals("ADMINJOB")) {
				return asBadReguest(jobdata, " Invalid payload.", HttpStatus.BAD_REQUEST);
			}
			
			boolean yn= authService.adminlogin(jobdata.getconfigurl(), jobdata.getUser(), jobdata.getPassword(), jobdata.getJobConfiguration().getName());
			if(!yn) throw new AuthException("Authenticate failed with "+jobdata.getconfigurl());
			List<JobInfo> jobs = jobservice.listAllJobs();
			return new ResponseEntity<List<JobInfo>>(jobs, HttpStatus.OK);
		}catch(AuthException e) {
			return asBadReguest(jobdata, e.getMessage(), HttpStatus.UNAUTHORIZED);
		}catch(Throwable t) {
			return asBadReguest(jobdata, t.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private ResponseEntity<JobData> asBadReguest(JobData jobdata, String msg, HttpStatus hstat){
		jobdata.setDescription(msg);
		//jobdata.setUser(StringUtils.repeat("U", jobdata.getUser().length()));
		jobdata.setPassword(StringUtils.repeat("*", jobdata.getPassword().length()));
		logger.error(msg);
		return new ResponseEntity<JobData>(jobdata, hstat);
	}
}
