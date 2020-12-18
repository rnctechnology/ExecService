package com.rnctech.nrdataservice.service;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.resource.EmailRequest;
import com.rnctech.nrdataservice.utils.RNUtilities;
import com.rnctech.nrdataservice.RNApplication;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.job.RNJob;

/*
* @author zilin chen
* @since 2020.09
*/

@Service
public class RNJobsListener implements JobListener {
    private final Log logger = LogFactory.getLog(RNJobsListener.class);
    
    @Autowired
    MailService mailService;
    
    @Autowired
    JobRepository jobrepo;
    
	final String mreq = "{\"email\":\"alan.chen@rnctech.com\",\"subject\":\"RNCtech job failed.\",\"body\":\"Exec Service with job process id (pid) \"}";
	final String mend = " by rnctech @"; 
    
    @Override
    public String getName() {
        return "rnctech Job Execution Listener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
    	JobDataMap datamap = context.getJobDetail().getJobDataMap();
		Long jobpid = datamap.getLong(RNConsts.JOB_PROC_ID);
    	Job job = jobrepo.findById(jobpid).get();
    	if(null != job) {
	    	job.setStatus(Status.RUNNING.ordinal());    		
	    	job.setActive(true);
	    	job.setLastModified(new Date());
	    	job.setUpdatedby(this.getClass().getSimpleName());
	    	String msg =  "Job to be executed " + context.getJobDetail().getKey().getName();	    	
	    	job.setExecutionInfo(msg);
	    	jobrepo.saveAndFlush(job);
	        logger.debug(msg); 
    	}else {
    		logger.error("job with "+jobpid+" not found!");
    	}
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        logger.info("Job execution vetoed " + context.getJobDetail().getKey().getName());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {  	
		if(null != jobException && null != jobException.getMessage() && jobException.getMessage().startsWith("InterruptedException: ")) {
			JobDataMap datamap = context.getJobDetail().getJobDataMap();
			Long jobpid = datamap.getLong(RNConsts.JOB_PROC_ID);
			Job job = jobrepo.findById(jobpid).get();
			if(!RNApplication.isPyEngine()) {
				try {
					EmailRequest mailreq = RNUtilities.getObjectFromJsonString(mreq, EmailRequest.class);
					String mailto = datamap.getString(RNJob.TENANT_MAILTO);
					if(null != mailto && !mailto.trim().isEmpty())
						mailreq.setEmail(mailto);
			
					StringBuilder sb = new StringBuilder();
					sb.append(mailreq.getBody()+jobpid+"\n<br /> ");
					if(null != job) {
						mailreq.setSubject(mailreq.getSubject()+" Name: "+job.getTenant());
						sb.append(" Job Name: "+job.getJobname()+"\n<br /> ");
						sb.append(" Config URL: "+job.getconfigurl() + " Job Id: "+job.getJobid()+"\n<br /> ");
						sb.append(" Job Type: "+job.getJobtype()+"\n<br /> ");
						sb.append(" Instance Type: " + job.getInstanceType()+"\n<br /> Executed By: "+job.getJobExecutedBy()+"\n<br /> ");
					}
					sb.append(jobException+"\n<br /><br /> "+mend+RNUtilities.getUTCTimeString(new Date()));
					mailreq.setBody(sb.toString());					
					mailService.sendMail(mailreq);
			} catch (Exception e) {
					//
			}
			}
    	}
		
		logger.info("Job was executed " +  context.getJobDetail().getKey().getName() + (jobException != null ? " failed." : " successfully!"));
    	
/*    	JobDataMap datamap = context.getJobDetail().getJobDataMap();
		Long jobpid = datamap.getLong(RNConsts.JOB_PROC_ID);
 		Job job = jobrepo.findById(jobpid).get();
    	RNJob nj = null;
    	if(null != context.getJobInstance() && context.getJobInstance() instanceof RNJob) {
    		 nj = (RNJob)context.getJobInstance();
    	}
    	
    	if(null != job) {
	    	if(jobException == null) {	    		   
	    		if(null != nj && null != nj.getJob()) {
	    			String desc =  nj.getJob().getDescription();
	    			if(desc.startsWith("Job completed at ")) {
	    				job.setStatus(Status.FINISHED.ordinal());
	    			}else {
	    				job.setStatus(Status.FINISHEDWITHERROR.ordinal());
	    				
	    			}
		    		job.setExecutionInfo(nj.getJob().getExeinfo());
		    	}else {
		    		job.setStatus(Status.FINISHED.ordinal());
		    	}
	    	}else {
	    		if(null != jobException.getMessage() && jobException.getMessage().startsWith("InterruptedException: ")) {
	    			job.setStatus(Status.ABORT.ordinal());
	    		}else {
	    			job.setStatus(Status.ERROR.ordinal());
	    		}
		    	if(null != nj && null != nj.getJob()) {
		    		job.setExecutionInfo(nj.getJob().getExeinfo());
		    	}else {
		    		job.setExecutionInfo(jobException.getMessage());
		    	}
	    		try {
					EmailRequest mailreq = RNUtilities.getObjectFromJsonString(mreq, EmailRequest.class);
					String mailto = datamap.getString(RNJob.TENANT_MAILTO);
					if(null != mailto && !mailto.trim().isEmpty())
						mailreq.setEmail(mailto);
					mailreq.setBody(mailreq.getBody()+jobpid+"\n"+jobException+"\n"+mend+RNUtilities.getUTCTimeString(new Date()));
					mailService.sendMail(mailreq);
				} catch (Exception e) {
					//
				}
	    	}
	    	job.setActive(false);
	    	job.setLastModified(new Date());
	    	job.setUpdatedby(this.getClass().getSimpleName());
	    	String desc =  "Job was executed " +  context.getJobDetail().getKey().getName() + (jobException != null ? ", with error: " +jobException.getMessage() : " successfully!");
	    	job.setDescription((desc.length() > 2000)?desc.substring(0, 2000):desc);
	    	jobrepo.saveAndFlush(job);
	    	jobrepo.flush();
	        logger.info("Job was executed " +  context.getJobDetail().getKey().getName() + (jobException != null ? " failed." : " successfully!"));
    	}else {
    		logger.error("job with "+jobpid+" not found!");
    	}*/
    }
}
