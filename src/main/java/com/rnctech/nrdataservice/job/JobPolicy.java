package com.rnctech.nrdataservice.job;

import com.rnctech.nrdataservice.RNConsts.SCHEDULETYPE;

/**
 * @contributor zilin
 * 2020.10
 */

public class JobPolicy {
	boolean isSchedule = false;
	String scheduleType = SCHEDULETYPE.now.name();
	String execType = "";
	private long frequency = -1;
    private String cronexpr = "";
    private int delay = 30000; //default delay 30s;
    int retry = 0;
    
    public String getCronexpr() {
		return cronexpr;
	}
	public void setCronexpr(String cronexpr) {
		this.cronexpr = cronexpr;
	}
	
	public boolean isSchedule() {
		return isSchedule;
	}
	public void setSchedule(boolean isSchedule) {
		this.isSchedule = isSchedule;
	}
	public String getScheduleType() {
		return scheduleType;
	}
	public void setScheduleType(String scheduleType) {
		this.scheduleType = scheduleType;
	}
	public String getExecType() {
		return execType;
	}
	public void setExecType(String execType) {
		this.execType = execType;
	}
	public long getFrequency() {
		return frequency;
	}
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}
	public int getRetry() {
		return this.retry;
	}
	public void setRetry(int retry) {
		this.retry = retry;
	}
	public int getDelay() {
		return this.delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
	}
    
}
