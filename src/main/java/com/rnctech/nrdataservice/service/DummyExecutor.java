package com.rnctech.nrdataservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.config.AppConfig;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNResult.Code;

/*
* @author zilin chen
* @since 2020.09
*/

public class DummyExecutor extends RNJobExecutor {

	List<Future<?>> futures = new ArrayList<>();
	int execCount = 0;
	int totalTaks = 10;

	public DummyExecutor(Properties properties) {
		super(properties);
	}

	@Override
	public void open() throws RNBaseException {
		initJobProperties();
	}

	@Override
	public void close() throws RNBaseException {
		try {
			for (Future<?> future : futures) {
				future.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public RNResult exec(String st, RNContext context) throws RNBaseException {
		RNResult res = new RNResult(Code.INITED, "");
		Authentication auth = getAuthentication();
		logger.info("get estimated no of threads as " + AppConfig.estimateThreads());
		for (int i = 0; i < totalTaks; i++) {
			// executor.execute(getTask(auth, i, "TT", res));
			futures.add(executor.submit(getTask(auth, i, "TT", res)));
		}
		return res;
	}

	@Override
	public void cancel(RNContext context) throws RNBaseException {
		try {
			for (Future<?> future : futures) {
				if(!future.isDone())
					future.cancel(true);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	@Override
	public int getProgress(RNContext context) throws RNBaseException {
		return (int)(100.0 * (execCount / totalTaks));
	}

	@Transactional
	private Runnable getTask(Authentication auth, int i, String tname, RNResult res) {
		return () -> {
			try {
				setContext(auth);
				Thread.currentThread().setName(tname);
				String msg = String.format("running task %d. Thread: %s%n", i, Thread.currentThread().getName());
				// real
				System.out.printf(msg);
				logger.info(Thread.currentThread().getId() + ": executed!");
				res.setRetCode(Code.SUCCESS);
				res.setMsg(msg);
			} catch (Exception e) {
				logger.info("Task Failed:\n" + e.getMessage());
			}
			execCount ++;
		};
	}

	@Override
	public void initProp(Properties property) {
		// TODO Auto-generated method stub
		
	}


}
