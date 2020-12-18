package com.rnctech.nrdataservice.job;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.job.RNJob.Status;

/**
 * @contributor zilin
 * 2020.01
 */

@JsonIgnoreProperties({"jobkey"})
public class JobInfo {
	
	String name;
	Long jobrunId = -1l;
	Long jobId = -1l;
	String configurl;
	String mrUser;
	String mrPassword;
	String status = Status.UNKNOWN.name();
	String executionInfo;
	Boolean localJob = false;
	String lastStatusCheckedBy;  //ec2 instance or service name
	Instant lastStatusCheckedOn = Instant.now();
	Instant jobStartAt = Instant.now();
	String jobkey;
	String environment;
	String instanceType; 
	String description = "initialized";
	
	public JobInfo(String name, Long jobId, String configurl, String mrUser, String mrPassword, String status,
			String executionInfo, String lastStatusCheckedBy, String jobkey) {
		super();
		this.name = name;
		this.jobId = jobId;
		this.configurl = configurl;
		this.mrUser = mrUser;
		//this.mrPassword = configPassword;
		this.status = status;
		this.executionInfo = executionInfo;
		this.lastStatusCheckedBy = lastStatusCheckedBy;
		this.jobkey = jobkey;
	}
	
	public JobInfo(Job j) {
		this.name = j.getJobname();
		this.jobId = j.getJobid();
		this.configurl = j.getconfigurl();
		this.mrUser = j.getMrUser();
		//this.mrPassword = j.getMrPassword();
		this.status = Status.values()[j.getStatus()].name();
		this.executionInfo = j.getExecutionInfo();
		this.lastStatusCheckedBy = j.getJobExecutedBy();
		this.jobkey = j.getJobkey()+" of "+j.getJobGroup();
		this.lastStatusCheckedOn = j.getLastModified().toInstant();
		this.jobStartAt = j.getCreated().toInstant();
		this.jobrunId = j.getId();
		this.description = j.getDescription();	
		this.instanceType = j.getInstanceType();
		this.environment = j.getEnvname();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getJobId() {
		return jobId;
	}
	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}
	public String getconfigurl() {
		return configurl;
	}
	public void setconfigurl(String configurl) {
		this.configurl = configurl;
	}
	public String getMrUser() {
		return mrUser;
	}
	public void setMrUser(String mrUser) {
		this.mrUser = mrUser;
	}
	public String getMrPassword() {
		return mrPassword;
	}
	public void setMrPassword(String mrPassword) {
		this.mrPassword = mrPassword;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getExecutionInfo() {
		return executionInfo;
	}
	public void setExecutionInfo(String executionInfo) {
		this.executionInfo = executionInfo;
	}
	public Boolean getLocalJob() {
		return localJob;
	}
	public Long getJobrunId() {
		return jobrunId;
	}

	public void setJobrunId(Long jobrunId) {
		this.jobrunId = jobrunId;
	}

	public Instant getJobStartAt() {
		return jobStartAt;
	}

	public void setJobStartAt(Instant jobStartAt) {
		this.jobStartAt = jobStartAt;
	}

	public void setLocalJob(Boolean localJob) {
		this.localJob = localJob;
	}
	public String getLastStatusCheckedBy() {
		return lastStatusCheckedBy;
	}
	public void setLastStatusCheckedBy(String lastStatusCheckedBy) {
		this.lastStatusCheckedBy = lastStatusCheckedBy;
	}
	public Instant getLastStatusCheckedOn() {
		return lastStatusCheckedOn;
	}
	public void setLastStatusCheckedOn(Instant lastStatusCheckedOn) {
		this.lastStatusCheckedOn = lastStatusCheckedOn;
	}
	public String getJobkey() {
		return jobkey;
	}
	public void setJobkey(String jobkey) {
		this.jobkey = jobkey;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}	


}
