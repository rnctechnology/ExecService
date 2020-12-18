package com.rnctech.nrdataservice.repo;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.utils.RNUtilities;

/**
 * @contributor zilin 
 * 2020.01
 */

@Entity
@Table(name = "job_detail", uniqueConstraints = { @UniqueConstraint(columnNames = { "jobid", "version" }) })
public class JobDetails extends JobBase {

	public JobDetails() {
		super();
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "jobid", referencedColumnName = "id")
	private Job job;

	@Column(name = "jobname")
	String jobname;

	@Column(name = "tenant")
	String tenant;

	@Column(name = "sessionid")
	int sessionid = -1;
	
	@Column(name = "statementid")
	int statementid = -1;
	
	@Column(name = "appid", length = 128)
	String appid;
	
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	
	@Column(name = "status")
	int status = -1;

	@Column(name = "sourceName", length = 32)
	String sourceName;

	@Column(name = "loadType", length = 32)
	String loadType;

	@Column(name = "jobType", length = 32)
	String jobType;

	@Column(name = "scriptType", length = 32)
	String scriptType;

	@Column(name = "executable", length = 255)
	String executable;

	@Column(name = "sparkUrl", length = 255)
	String sparkUrl;

	@Column(name = "deployMode", length = 32)
	String deployMode;

	@Column(name = "codeSnap", columnDefinition = "TEXT")
	private String codeSnap;

	@Column(name = "jobProperties", columnDefinition = "TEXT")
	private String jobProperties;

	@Column(name = "params", columnDefinition = "TEXT")
	private String params;

	@Column(name = "libraries", columnDefinition = "TEXT")
	private String libraries;

	@Column(name = "active")
	boolean active = false;

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "version", unique = true, nullable = false)
	long version = 0l;

	@Lob
	@Column(name = "zips", columnDefinition = "BLOB")
	private byte[] zips;

	public JobDetails(JobData jobdata) {
		super();
		this.jobname = jobdata.getJobName()+"_"+jobdata.getJobkey();
		JobConfig jc = jobdata.getJobConfiguration();
		if (null != jc) {
			this.codeSnap = jc.getCodeSnap();
			this.jobType = jc.getJobType();
			this.deployMode = jc.getDeployMode();
			this.executable = jc.getExecutable();
			this.loadType = jc.getLoadType();
			this.tenant = jc.getName();
			this.scriptType = jc.getScriptType();
			this.sourceName = jc.getSourceName();
			this.sparkUrl = jc.getSparkURL();
			this.jobProperties = RNUtilities.getString(jc.getJobProperties(), false);
			this.params = RNUtilities.getString(jc.getParams(), false);
			this.libraries = RNUtilities.getString(jc.getLibraries(), false);
		}
	}

	public JobDetails(RNContext ctx) {
		super();
		this.version = System.currentTimeMillis();
		this.jobType = ctx.getJobType();
		this.scriptType = ctx.getSrctype().name();
		this.executable = ctx.getSrcname();
		this.status = Status.RUNNING.ordinal();
		this.loadType = ctx.getAlgorithm();
		this.params = RNUtilities.getString(ctx.getParams(), false);
		this.jobProperties = RNUtilities.getString(ctx.getConf(), false);
		this.libraries = RNUtilities.concatenate(ctx.getDependenics(), ":");
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public String getJobname() {
		return jobname;
	}

	public void setJobname(String jobname) {
		this.jobname = jobname;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public int getSessionid() {
		return sessionid;
	}

	public void setSessionid(int sessionid) {
		this.sessionid = sessionid;
	}

	public int getStatementid() {
		return statementid;
	}

	public void setStatementid(int statementid) {
		this.statementid = statementid;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getSystemName() {
		return sourceName;
	}

	public void setSystemName(String sourceSystemName) {
		this.sourceName = sourceSystemName;
	}

	public String getLoadType() {
		return loadType;
	}

	public void setLoadType(String loadType) {
		this.loadType = loadType;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getScriptType() {
		return scriptType;
	}

	public void setScriptType(String scriptType) {
		this.scriptType = scriptType;
	}

	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public String getSparkUrl() {
		return sparkUrl;
	}

	public void setSparkUrl(String sparkURL) {
		this.sparkUrl = sparkURL;
	}

	public String getDeployMode() {
		return deployMode;
	}

	public void setDeployMode(String deployMode) {
		this.deployMode = deployMode;
	}

	public String getCodeSnap() {
		return codeSnap;
	}

	public void setCodeSnap(String codeSnap) {
		this.codeSnap = codeSnap;
	}

	public String getJobProperties() {
		return jobProperties;
	}

	public void setJobProperties(String jobProperties) {
		this.jobProperties = jobProperties;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getLibraries() {
		return libraries;
	}

	public void setLibraries(String libraries) {
		this.libraries = libraries;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public byte[] getZips() {
		return zips;
	}

	public void setZips(byte[] zips) {
		this.zips = zips;
	}
}
