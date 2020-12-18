package com.rnctech.nrdataservice.service.javaimpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.utils.ShellUtils;

/**
 * Base class for java/scala executor. 
 * Not support yet @2020.10
 * @author zilin chen
 */
public abstract class BaseJavaExecutor extends RNJobExecutor {

	private static final Logger logger = LoggerFactory.getLogger(BaseJavaExecutor.class);
	ConcurrentHashMap<String, DefaultExecutor> executors;
	protected static String JAVA_HOME = System.getenv("JAVA_HOME");
	public static final String WORKING_DIRECTORY_PROPERTY = "jvm.working.directory";
	public static final String TIMEOUT_PROPERTY = "jvm.timeout.millisecs";
	private static String TIMEOUT_DEFAULT = "300000";
	private static String USER_HOME = System.getProperty("user.home");
	protected Long jvmtimeout;
	protected String workingdir;
	
	public BaseJavaExecutor(Properties properties) {
		super(properties);
	}

	@Override
	public void initProp(Properties property) {
		jvmtimeout = Long.valueOf(property.getProperty(TIMEOUT_PROPERTY, TIMEOUT_DEFAULT));
		workingdir = property.getProperty(WORKING_DIRECTORY_PROPERTY, USER_HOME);
	}
	
	@Override
	public void open() {
		logger.info("Command timeout property: {}", jvmtimeout);
		executors = new ConcurrentHashMap<String, DefaultExecutor>();
	}

	@Override
	public void close() {
		for (String executorKey : executors.keySet()) {
			DefaultExecutor executor = executors.remove(executorKey);
			if (executor != null) {
				try {
					executor.getWatchdog().destroyProcess();
				} catch (Exception e) {
					logger.error("error destroying executor for: " + executorKey, e);
				}
			}
		}
	}
	
	@Override
	public void cancel(RNContext context) {
		DefaultExecutor executor = executors.remove(context.getCtxid());
		if (executor != null) {
			try {
				executor.getWatchdog().destroyProcess();
			} catch (Exception e) {
				logger.error("error destroying executor for job context: " + context.getCtxid(), e);
			}
		}
	}

	@Override
	public RNResult exec(String originalCmd, RNContext ctx) throws RNBaseException {
		String cmd = originalCmd;
		logger.debug("Run java command '" + cmd + "'");
		OutputStream outStream = new ByteArrayOutputStream();
		CommandLine cmdLine = CommandLine.parse(JAVA_HOME+"/bin/java");

		if (ShellUtils.isWindows) {
			String[] lines = StringUtils.split(cmd, "\n");
			cmd = StringUtils.join(lines, " && ");
		}
		cmdLine.addArgument(cmd, false);

		try {
			DefaultExecutor executor = new DefaultExecutor();
			executor.setStreamHandler(new PumpStreamHandler(ctx.out, ctx.out));
			executor.setWatchdog(new ExecuteWatchdog(jvmtimeout));
			executor.setWorkingDirectory(new File(workingdir));
			ctx.setProgressing(50);
			executors.put(ctx.getCtxid(), executor);
			int exitVal = executor.execute(cmdLine);
			logger.info("Process for " + ctx.getCtxid() + " return with exit value: " + exitVal);
			this.setProgress(50);
			return new RNResult(Code.SUCCESS, outStream.toString());
		} catch (ExecuteException e) {
			int exitValue = e.getExitValue();
			logger.error("Can not run " + cmd, e);
			String message = outStream.toString()+ "ExitValue: " + exitValue;
			return new RNResult(Code.ERROR, message);
		} catch (Exception e) {
			logger.error("Can not run " + cmd, e);
			return new RNResult(Code.ERROR, e.getMessage());
		} finally {
			executors.remove(ctx.getCtxid());
		}
	}

	@Override
	public int getProgress(RNContext context) throws RNBaseException {
		return 0;
	}

}
