package com.rnctech.nrdataservice.service.shellimpl;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.AuthService;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.utils.ShellUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shell interpreter
 * 
 * @author zilin chen
 * @since 2020.10
 */

@Component
public class ShellExecutor extends RNJobExecutor {

	private static final Logger logger = LoggerFactory.getLogger(ShellExecutor.class);

	public static final String TIMEOUT_PROPERTY = "shell.timeout.millisecs";
	private static String TIMEOUT_DEFAULT = "300000";
	public static final String WORKING_DIRECTORY_PROPERTY = "shell.working.directory";
	private static String USER_HOME = System.getProperty("user.home");
	protected final String shell = ShellUtils.shell;

	ConcurrentHashMap<String, DefaultExecutor> executors;
	private Long shelltimeout;
	private String workingdir;

	public ShellExecutor(Properties property) {
		super(property);
	}

	@Override
	public void initProp(Properties property) {
		shelltimeout = Long.valueOf(property.getProperty(TIMEOUT_PROPERTY, TIMEOUT_DEFAULT));
		workingdir = property.getProperty(WORKING_DIRECTORY_PROPERTY, USER_HOME);
	}

	@Override
	public void open() {
		logger.info("Command timeout property: {}", shelltimeout);
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
	public RNResult exec(String originalCmd, RNContext ctx) throws RNBaseException {
		String cmd = originalCmd;
		logger.debug("Run shell command '" + cmd + "'");
		OutputStream outStream = new ByteArrayOutputStream();
		CommandLine cmdLine = CommandLine.parse(shell);

		if (ShellUtils.isWindows) {
			String[] lines = StringUtils.split(cmd, "\n");
			cmd = StringUtils.join(lines, " && ");
		}
		cmdLine.addArgument(cmd, false);
		String[] argvs = ShellUtils.toArgsList(ctx.getParams(), ctx.getJobType());
		if(argvs.length > 0) {
			for(String arg: argvs) 
				cmdLine.addArgument(arg, false);
		}
		try {
			DefaultExecutor executor = new DefaultExecutor();
			executor.setStreamHandler(new PumpStreamHandler(ctx.out, ctx.out));
			executor.setWatchdog(new ExecuteWatchdog(shelltimeout));
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
			Code code = Code.ERROR;
			String message = outStream.toString();
			if (exitValue == 143) {
				code = Code.INCOMPLETE;
				message += "Received a SIGTERM\n";
				logger.info("Process for " + ctx.getCtxid() + " stopped executing: " + message);
			}
			message += "ExitValue: " + exitValue;
			return new RNResult(code, message);
		} catch (IOException e) {
			logger.error("Can not run " + cmd, e);
			return new RNResult(Code.ERROR, e.getMessage());
		} finally {
			executors.remove(ctx.getCtxid());
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
	
	protected boolean getKerberosTicket() {
		try {
			Properties properties = getProperties();
			AuthService.kerberosConfiguration(properties.getProperty("kerberos.kdcurl"),
					properties.getProperty("kerberos.principal"), properties.getProperty("kerberos.domainname"), shell,
					properties.getProperty(AuthService.KTAB_LOCATION));
			return true;
		} catch (Exception e) {
			logger.error("Unable to run kinit for ", e);
		}
		return false;
	}

}
