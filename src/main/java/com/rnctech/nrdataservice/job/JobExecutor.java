package com.rnctech.nrdataservice.job;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNJobExecutor;
/**
 * @contributor zilin
 * 2020.10
 */

public class JobExecutor {
	
	public static Map<String, RunnableFuture<RNJobExecutor>> tasks = new ConcurrentHashMap<>();
	public static Logger logger = Logger.getLogger(JobExecutor.class);
	
	public interface IdentifiableCallable extends Callable<RNJobExecutor> {
	    void cancelTask();
	    RunnableFuture<RNJobExecutor> newTask();
	}
	
	public class CustomFutureReturningExecutor extends ThreadPoolExecutor {

	    public CustomFutureReturningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
	        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	    }

	    public CustomFutureReturningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
	        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	    }

	    public CustomFutureReturningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
	        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	    }

	    public CustomFutureReturningExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
	        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	    }

	    @Override
	    protected RunnableFuture newTaskFor(Callable callable) {
	        if (callable instanceof IdentifiableCallable) {
	            return ((IdentifiableCallable) callable).newTask();
	        } else {
	            return super.newTaskFor(callable); // A regular Callable
	        }
	    }
	}
	
	public abstract class FutureTaskWrapper<T> extends FutureTask<T> {

	    public FutureTaskWrapper(Callable<T> c) {
	        super(c);
	    }

	    abstract long getTaskId();
	}
	
	public class rnctechTask implements IdentifiableCallable {
  
	    volatile boolean cancelled;  
	    RNJobExecutor handler;

	    public rnctechTask(RNJobExecutor handler) {
	    	this.handler = handler;
	    }


	    @Override
	    public RunnableFuture<RNJobExecutor> newTask() {
	        return new FutureTaskWrapper<RNJobExecutor>(this) {

	            @Override
	            public boolean cancel(boolean mayInterruptIfRunning) {
	            	rnctechTask.this.cancelTask();
	                return super.cancel(mayInterruptIfRunning);
	            }

	            @Override
	            public long getTaskId() {
	                return handler.hashCode();
	            }
	        };
	    }

	    @Override
	    public synchronized void cancelTask() {
	        cancelled = true;
	    }

	    @Override
	    public RNJobExecutor call() throws RNBaseException {
	        while (!cancelled) {
	        	handler.exec("", RNContext.builder());
	        }
	        return handler;
	    }
	}
	
	public void executeJob(JobData jobdata){
		RNJobExecutor handler = getExecutor(jobdata);
		
		ExecutorService exec = new CustomFutureReturningExecutor(1,1, 
	                Long.MAX_VALUE, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
		rnctechTask task = new rnctechTask(handler);
		
        Future<?> f = exec.submit(task);
        FutureTaskWrapper<RNJobExecutor> ftw = (FutureTaskWrapper)f;		
		
		RunnableFuture<RNJobExecutor> t = task.newTask();
        t.run();
		tasks.put(getHandlerid(jobdata), ftw);

	}

	private RNJobExecutor getExecutor(JobData jobdata) {
		// TODO Auto-generated method stub
		return null;
	}

	public long cancelJob(String handlerid){
		RunnableFuture<RNJobExecutor> ftw = tasks.get(handlerid);
		if(null != ftw)
			ftw.cancel(true);
		return ((FutureTaskWrapper<RNJobExecutor>)ftw).getTaskId();
	}
	
	private String getHandlerid(JobData jobdata) {
		return jobdata.getJobConfiguration().name+"_"+jobdata.getJobid();
	}
	
}
