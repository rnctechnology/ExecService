package com.rnctech.nrdataservice.apis;

import com.rnctech.nrdataservice.job.RNJob.Status;

/**
 * @contributor zilin
 * @since 2020.08
 */

public class JobStatus {
	
    public final String id;
    public final boolean running;
    public Status status = Status.UNKNOWN;

    public JobStatus(String id, boolean running) {
        this.id = id;
        this.running = running;
    }
}
