package com.rnctech.nrdataservice.job;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Zilin Chen
 * @since 2020.09
 */

@JsonIgnoreProperties({ "jobkey" })
public class JobData {
	String configurl = null;
	String user = null;
	String password = null;
	JobConfig jobConfiguration = null;
	Long jobid = -1l;
	String jobName = null;
	String description;
	Boolean allowssl = false;
	@JsonIgnore String jobkey;
	@JsonIgnore String status = "INIT";	
	Long pid = -1l;
	@JsonIgnore String exeinfo = null;
	@JsonIgnore JobPolicy policy = new JobPolicy();

	public JobData() {
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getJobid() {
		return jobid;
	}

	public void setJobid(Long jobid) {
		this.jobid = jobid;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getconfigurl() {
		return configurl;
	}

	public void setconfigurl(String configurl) {
		this.configurl = configurl.trim();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getJobkey() {
		return jobkey;
	}

	public void setJobkey(String jobkey) {
		this.jobkey = jobkey;
	}

	public String toString() {
		return "configurl="+configurl+" user="+user+" jobid="+jobid+" "+((null != jobConfiguration)?jobConfiguration.toString():"");
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public JobConfig getJobConfiguration() {
		return jobConfiguration;
	}

	public void setJobConfiguration(JobConfig jobData) {
		this.jobConfiguration = jobData;
	}

	public Long getPid() {
		return pid;
	}

	public void setPid(Long pid) {
		this.pid = pid;
	}

	public JobPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(JobPolicy policy) {
		this.policy = policy;
	}

	public Boolean getAllowssl() {
		return allowssl;
	}

	public void setAllowssl(Boolean allowssl) {
		this.allowssl = allowssl;
	}

	public String getExeinfo() {
		return exeinfo;
	}

	public void setExeinfo(String exeinfo) {
		this.exeinfo = exeinfo;
	}

}