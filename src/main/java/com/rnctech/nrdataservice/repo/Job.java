package com.rnctech.nrdataservice.repo;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.job.JobData;

/**
 * @contributor zilin
 * 2019.01
 */

@Entity
@Table(name = "job", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"jobExecutedBy","jobkey"})}
)
public class Job extends JobBase {

	public Job() {
		super();
	}

	@OneToMany(cascade = {
			CascadeType.REMOVE }, fetch = FetchType.LAZY, mappedBy = "job")
	private Set<JobDetails> jobdetails = new HashSet<>(0);
	
	
    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, optional = true)
  private JobPolicy jobpolicy;
	
	
	  @Column(name = "jobname")
	  String jobname;
	 
	  @Column(name = "jobid")
	  long jobid = -1l;
	  
	  @Column(name = "tenant")
	  String tenant;
	  
	  @Column(name = "status")
	  int status= -1;
	  
	  @Column(name = "executionInfo", columnDefinition = "TEXT")
	  String executionInfo;
	  
	  @Column(name = "configurl")
	  String configurl;
	  
	  @Column(name = "configuser")
	  String configUser;
	  
	  @Column(name = "configpassword")
	  String configPassword;
	  
	  @Column(name = "localJob")
	  boolean localJob = false;
	  
	  @Column(name = "jobGroup")
	  String jobGroup = RNConsts.groupName;
	  
	  @Column(name = "active")
	  boolean active = true;
	  
	  @Column(name = "jobtype")
	  String jobtype;
	  
	  @Column(name = "jobExecutedBy", unique = false, nullable = false)
	  String jobExecutedBy = "127.0.0.1";
	  
	  @Column(name = "jobStatusChecked", columnDefinition="DATETIME")
	  @Temporal(TemporalType.TIMESTAMP)
	  Date jobStatusChecked; 
	  
	  @Column(name = "jobkey", unique = false, nullable = false)  //DEFAULT '000000' NOT NULL
	  String jobkey;
	  
	  @Column(name = "description", length = 2048)
	  String description;
	  
	  @Column(name = "envname")
	  String envname;
	  
	  @Column(name = "mrversion")
	  String mrversion;
	  
	  @Column(name = "instanceType", unique = false, nullable = false)
	  String instanceType = "";
	  
	public Job(JobData jobdata) {
		super();
		this.jobid = jobdata.getJobid();
		this.configurl = jobdata.getconfigurl();
		this.configUser = jobdata.getUser();
		this.configPassword = jobdata.getPassword();
		this.jobkey = jobdata.getJobkey();
		this.status = Status.valueOf(jobdata.getStatus()).ordinal();
		this.jobname = jobdata.getJobName();
		this.description = jobdata.getDescription();
		if(null != jobdata.getJobConfiguration()) this.tenant = jobdata.getJobConfiguration().getName();
		if(null != jobdata.getJobConfiguration()) this.jobtype = jobdata.getJobConfiguration().getJobType();
	}
	  
		public Job(String jobname, long jobid, int status, String configurl, String mrUser, String mrPassword,
				String jobExecutedBy, String jobkey, String description, String instanceType) {
			super();
			this.jobname = jobname;
			this.jobid = jobid;
			this.status = status;
			this.configurl = configurl;
			this.configUser = mrUser;
			this.configPassword = mrPassword;
			this.jobExecutedBy = jobExecutedBy;
			this.jobkey = jobkey;
			this.description = description;
			this.instanceType = instanceType;
		}  

	public Set<JobDetails> getJobdetails() {
		return jobdetails;
	}

	public void setJobdetails(Set<JobDetails> jobdetails) {
		this.jobdetails = jobdetails;
	}

	public JobPolicy getJobpolicy() {
		return jobpolicy;
	}

	public void setJobpolicy(JobPolicy jobpolicy) {
		this.jobpolicy = jobpolicy;
	}

	public String getJobname() {
		return jobname;
	}

	public void setJobname(String jobname) {
		this.jobname = jobname;
	}

	public long getJobid() {
		return jobid;
	}

	public void setJobid(long jobid) {
		this.jobid = jobid;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getExecutionInfo() {
		return executionInfo;
	}

	public void setExecutionInfo(String executionInfo) {
		this.executionInfo = executionInfo;
	}

	public String getconfigurl() {
		return configurl;
	}

	public void setconfigurl(String configurl) {
		this.configurl = configurl;
	}

	public String getMrUser() {
		return configUser;
	}

	public void setMrUser(String mrUser) {
		this.configUser = mrUser;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public String getMrPassword() {
		return configPassword;
	}

	public void setMrPassword(String mrPassword) {
		this.configPassword = mrPassword;
	}

	public boolean isLocalJob() {
		return localJob;
	}

	public void setLocalJob(boolean localJob) {
		this.localJob = localJob;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getJobtype() {
		return jobtype;
	}

	public void setJobtype(String jobtype) {
		this.jobtype = jobtype;
	}

	public String getJobExecutedBy() {
		return jobExecutedBy;
	}

	public void setJobExecutedBy(String jobExecutedBy) {
		this.jobExecutedBy = jobExecutedBy;
	}

	public Date getJobStatusChecked() {
		return jobStatusChecked;
	}

	public void setJobStatusChecked(Date jobStatusChecked) {
		this.jobStatusChecked = jobStatusChecked;
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

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getEnvname() {
		return envname;
	}

	public void setEnvname(String envname) {
		this.envname = envname;
	}

	public String getMrversion() {
		return mrversion;
	}

	public void setMrversion(String mrversion) {
		this.mrversion = mrversion;
	}

}
