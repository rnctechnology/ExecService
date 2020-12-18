package com.rnctech.nrdataservice.service.javaimpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.utils.ShellUtils;

/**
 * Base class for java/scala executor. 
 * @author zilin chen
 */

public class JavaExecutor extends BaseJavaExecutor {
	private static final Logger logger = LoggerFactory.getLogger(JavaExecutor.class);
	protected final String jvmcmd = "java";
	private static final String dsep = ShellUtils.dseperator;
	
	public JavaExecutor(Properties properties) {
		super(properties);
	}
	
	@Override
	public RNResult exec(String originalCmd, RNContext ctx) throws RNBaseException {
		String cmd = originalCmd;
		logger.debug("Run java command '" + cmd + "'");
		OutputStream outStream = new ByteArrayOutputStream();
		CommandLine cmdLine = CommandLine.parse(getJVMExec());

		if (ShellUtils.isWindows) {
			String[] lines = StringUtils.split(cmd, "\n");
			cmd = StringUtils.join(lines, " && ");
		}
		cmdLine.addArgument(cmd, false);
		String cp = getClasspath(ctx);
		if(null != cp) {
			cmdLine.addArgument("-cp", false);
			cmdLine.addArgument(getClasspath(ctx), false);
		}
		
		cmdLine.addArgument("-jar", false);
		cmdLine.addArgument(ctx.getCodesnap().trim(), false);
		cmdLine.addArgument(ctx.getSrcname().trim(), false);

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
	
	private String getClasspath(RNContext ctx) {
		if(null != ctx.getDependenics() && !ctx.getDependenics().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(String d: ctx.getDependenics()) {
				if(sb.length() > 0) sb.append(dsep);
				sb.append(d.trim());
			}
			return sb.toString();
		}
		return null;
	}

	@Override
	public boolean validate(String path) throws RNBaseException{
		try {
			DefaultExecutor executor = new DefaultExecutor();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			executor.setWatchdog(new ExecuteWatchdog(20000));
			Map<String, String> env = EnvironmentUtils.getProcEnvironment();
			String javaExec = getJVMExec();
			CommandLine cmd = CommandLine.parse(javaExec);
			cmd.addArgument("--version", false);
			int errcode = executor.execute(cmd, env);
			logger.info(cmd+"  "+errcode+": "+outputStream.toString());
			if(0 != errcode) {
				return false;
			}else {	
				return true;
			}
		} catch (Exception e) {
			logger.info(path+" validate failed: "+e.getMessage());
			return false;
		}
	}

	private String getJVMExec() {
		return JAVA_HOME+"/bin/java";
	}


}
