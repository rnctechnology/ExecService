package com.rnctech.nrdataservice.repo;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import com.rnctech.nrdataservice.RNConsts.PROCESSTYPE;
import com.rnctech.nrdataservice.RNConsts.SCHEDULETYPE;
import com.rnctech.nrdataservice.job.JobData;

/**
 * @contributor zilin 2020.01
 */

@Entity
@Table(name = "job_policy")
public class JobPolicy extends JobBase {

	private static final long serialVersionUID = 2562823868473155043L;

	public JobPolicy() {
		super();
	}

	@OneToOne(cascade = { CascadeType.MERGE, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jobid", referencedColumnName = "id")
	private Job job;

	@Column(name = "isschedule")
	boolean isschedule = false;

	@Column(name = "scheduletype")
	String scheduletype = SCHEDULETYPE.now.name();

	@Column(name = "frequency")
	long frequency = 0l;

	@Column(name = "exectype")
	int exectype = PROCESSTYPE.remote.ordinal();

	@Column(name = "cronexpr")
	String cronexpr = "";

	@Column(name = "delay")
	int delay = 10000;

	public JobPolicy(JobData jobdata) {
		super();
		if (null != jobdata.getPolicy()) {
			this.isschedule = jobdata.getPolicy().isSchedule();
			this.scheduletype = jobdata.getPolicy().getScheduleType();
			this.frequency = jobdata.getPolicy().getFrequency();
			try {
				this.exectype = PROCESSTYPE.valueOf(jobdata.getPolicy().getExecType()).ordinal();
			} catch (Exception e) {
			}
			this.cronexpr = jobdata.getPolicy().getCronexpr();
			this.delay = jobdata.getPolicy().getDelay();
		}

	}

	public JobPolicy(boolean isschedule, String scheduletype, long frequency, int exectype, String cronexpr,
			int delay) {
		super();
		this.isschedule = isschedule;
		this.scheduletype = scheduletype;
		this.frequency = frequency;
		this.exectype = exectype;
		this.cronexpr = cronexpr;
		this.delay = delay;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public boolean isIsschedule() {
		return isschedule;
	}

	public void setIsschedule(boolean isschedule) {
		this.isschedule = isschedule;
	}

	public String getScheduletype() {
		return scheduletype;
	}

	public void setScheduletype(String scheduletype) {
		this.scheduletype = scheduletype;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public int getExectype() {
		return exectype;
	}

	public void setExectype(int exectype) {
		this.exectype = exectype;
	}

	public String getCronexpr() {
		return cronexpr;
	}

	public void setCronexpr(String cronexpr) {
		this.cronexpr = cronexpr;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

}
