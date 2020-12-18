package com.rnctech.nrdataservice.service;

import java.io.IOException;
import java.util.Properties;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;

/*
* @author zilin chen
* @since 2020.09
*/

public interface IJobExecutor {

    public void open() throws RNBaseException;
    public void close() throws RNBaseException;
    public void setProperties(Properties properties);
    
    public RNResult exec(String st, RNContext context) throws RNBaseException;
    
    public RNResult status(RNContext context) throws RNBaseException;
    
    public void cancel(RNContext context) throws RNBaseException;
    
    public IJobExecutor withRepo(JobRepository jobrepo, JobDetailsRepository jobdetailrepo);
    
    public IJobExecutor withAppContext(ApplicationContext appContext);
    
    public boolean validate(String s) throws RNBaseException;
    
}
