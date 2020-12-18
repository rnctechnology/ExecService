package com.rnctech.nrdataservice.service.pyimpl;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.exception.TechException;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.service.IJobExecutor;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.service.RNResult.ResultCompletion;
import com.rnctech.nrdataservice.service.RNResult.ResultMessage;
import com.rnctech.nrdataservice.service.RNResult.Type;
import com.rnctech.nrdataservice.utils.RNCLogOutputStream;
import com.rnctech.nrdataservice.utils.RNCOutputStream;
import com.rnctech.nrdataservice.utils.ShellUtils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import py4j.GatewayServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base Python Executor
 * 
 * @author zilin chen
 * @since 2020.10
 */

@Component
public class PythonExecutor extends RNJobExecutor implements ExecuteResultHandler {
	private static final Logger logger = LoggerFactory.getLogger(PythonExecutor.class);
	private static final int MAX_TIMEOUT_SEC = 30;
	public static final String PYEXEMEMKEY = "PYTHON_EXECUTOR_MEMORY_MB";
	private static final int PYEXEMEMMAXDEF = 65536;  //default max to 64G
	private String pyExec;
	private String pypath;
	private GatewayServer gatewayServer;
	private DefaultExecutor executor;
	private File pyWorkDir;
	protected boolean useBuiltinPy4j = true;
	JobRepository jobrepo;
	JobDetailsRepository jobdetailrepo;
	private RNCLogOutputStream outputStream;
	private AtomicBoolean pyscriptRunning = new AtomicBoolean(false);
	private AtomicBoolean pyscriptInitialized = new AtomicBoolean(false);
	private long pythonPid = -1;

	private boolean usepy4jAuth = true;
	private PythonContext pyCtx;

	public PythonExecutor(Properties property) {
		super(property);
	}

	@Override
	public void open() throws TechException {
		try {
			super.initJobProperties();
			this.usepy4jAuth = Boolean.parseBoolean(getProperty("py4j.useAuth", "true"));
			
			createGateway();
		} catch (IOException e) {
			logger.error("Fail to open Pythoner", e);
			throw new TechException("Fail to open Pythoner", e);
		}
	}
	
	@Override
	public boolean validate(String gwserver) throws RNBaseException{
		DefaultExecutor executor = new DefaultExecutor();
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
			executor.setStreamHandler(streamHandler);
			executor.setWatchdog(new ExecuteWatchdog(20000));
			Map<String, String> env = EnvironmentUtils.getProcEnvironment();
			String pythonExec = getPyExec();
			CommandLine cmd = CommandLine.parse(pythonExec);
			cmd.addArgument("-V", false);
			int errcode = executor.execute(cmd, env);
			logger.info(cmd+"  "+errcode+": "+outputStream.toString());
			if(0 != errcode) {
				return false;
			}else {	
				return true;
			}
		} catch (Exception e) {
			logger.info(gwserver+" validate failed: "+e.getMessage());
			return false;
		}finally {
			if (null != executor && null != executor.getWatchdog()) {
				try {
					executor.getWatchdog().destroyProcess();
				} catch (Throwable e) {
				}
			}
		}
	}

	private void createGateway() throws IOException {
		int port = ShellUtils.findFirstAvailablePort();
		String serverAddress = PythonUtils.getLocalIP(properties);
		String secret = PythonUtils.createSecret(256);
		this.gatewayServer = PythonUtils.createGatewayServer(this, serverAddress, port, secret, usepy4jAuth);
		gatewayServer.start();

		createPyScript();
		String pythonExec = getPyExec();				
		CommandLine cmd = CommandLine.parse(pythonExec);		
		if (!pythonExec.endsWith(".py")) {
			cmd.addArgument(pyWorkDir + "/rnpython.py", false);
		}
		cmd.addArgument(serverAddress, false);
		cmd.addArgument(Integer.toString(port), false);

		executor = new DefaultExecutor();
		outputStream = new RNCLogOutputStream(logger);
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
		executor.setStreamHandler(streamHandler);
		executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
		Map<String, String> env = setupPyEnv();
		if (usepy4jAuth) {
			env.put("PY4J_GATEWAY_SECRET", secret);
		}
		if(null != this.properties.get(PYEXEMEMKEY)) {
			int maxmb = PYEXEMEMMAXDEF;
			try {
				maxmb= Integer.parseInt(this.properties.get(PYEXEMEMKEY).toString().trim());
				env.put(PYEXEMEMKEY, String.valueOf(maxmb));				
			}catch(Exception e){
				//ignore
			}
		}
		logger.info("Launching Python Process Command: " + cmd.getExecutable() + " "
				+ StringUtils.join(cmd.getArguments(), " "));
		executor.execute(cmd, env, this);
		pyscriptRunning.set(true);
	}

	protected PythonContext createPyContext() {
		PythonContext pyctx = new PythonContext(Integer.parseInt(getProperty("rnctech.maxResult", "1000")));
		pyctx.setOutput(new RNCOutputStream(null));
		return pyctx;
	}

	public PythonContext getRNContext() {
		if (pyCtx == null) {
			pyCtx = createPyContext();
		}
		return pyCtx;
	}

	private void createPyScript() throws IOException {
		if (null == System.getProperty("java.io.tmpdir")) {
			System.setProperty("java.io.tmpdir", "/tmp");
		}
		this.pyWorkDir = Files.createTempDir();
		this.pyWorkDir.deleteOnExit();
		logger.debug("Create Python working dir: " + pyWorkDir.getAbsolutePath());
		cpResToWorkDir("python/rnpython.py", "rnpython.py");
		cpResToWorkDir("python/rncontext.py", "rncontext.py");
		cpResToWorkDir("python/backend_zinline.py", "backend_zinline.py");
		cpResToWorkDir("python/mpl_config.py", "mpl_config.py");
		cpResToWorkDir("python/py4j-src-0.10.7.zip", "py4j-src-0.10.7.zip");
	}

	private void cpResToWorkDir(String srcResourceName, String dstFileName) throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(pyWorkDir.getAbsoluteFile() + "/" + dstFileName);
			IOUtils.copy(getClass().getClassLoader().getResourceAsStream(srcResourceName), out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	protected Map<String, String> setupPyEnv() throws IOException {
		Map<String, String> env = EnvironmentUtils.getProcEnvironment();
		appendToPyPath(env, pyWorkDir.getAbsolutePath());
		if (useBuiltinPy4j) {
			appendToPyPath(env, pyWorkDir.getAbsolutePath() + "/py4j-src-0.10.7.zip");
		}
		pypath = env.get("PYTHONPATH");
		logger.info("PYTHONPATH: " + pypath);
		return env;
	}

	private void appendToPyPath(Map<String, String> env, String path) {
		if (!env.containsKey("PYTHONPATH")) {
			env.put("PYTHONPATH", path);
		} else {
			env.put("PYTHONPATH", env.get("PYTHONPATH") + ":" + path);
		}
	}

	public void setPyExec(String pythonExec) {
		logger.info("Set Python Command : {}", pythonExec);
		this.pyExec = pythonExec;
	}

	protected String getPyExec() {
		if (pyExec != null) {
			return pyExec;
		} else {
			return getProperty("python", "python");
		}
	}

	public File getPythonWorkDir() {
		return pyWorkDir;
	}

	@Override
	public void close() throws TechException {
		pyscriptRunning.set(false);
		pyscriptInitialized.set(false);
		gatewayServer.shutdown();
		statementSetNotifier = new Integer(0);
		statementFinishedNotifier = new Integer(0);
		pythonPid = -1;
/*		if (null != executor && null != executor.getWatchdog()) {
			try {
				executor.getWatchdog().destroyProcess();
			} catch (Throwable e) {
			}
		}*/
	}

	private PythonRequest pythonRequest = null;
	private Integer statementSetNotifier = new Integer(0);
	private Integer statementFinishedNotifier = new Integer(0);
	private String statementOutput = null;
	private boolean statementError = false;

	public class PythonRequest {
		public String argv = "\"--a0 a0\"";
		public String name = "T";
		public String statements;
		public boolean isForCompletion;
		public boolean isCallHooks;

		public PythonRequest(String name, String statements, boolean isForCompletion) {
			this(statements, isForCompletion, true);
			this.name = name;
		}

		public PythonRequest(String statements, boolean isForCompletion, boolean isCallHooks) {
			this.statements = statements;
			this.isForCompletion = isForCompletion;
			this.isCallHooks = isCallHooks;
		}

		public String statements() {
			return statements;
		}

		public String argv() {
			return argv;
		}

		public void setArgv(String argv) {
			this.argv = argv;
		}

		public String name() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isForCompletion() {
			return isForCompletion;
		}

		public boolean isCallHooks() {
			return isCallHooks;
		}
	}

	public PythonRequest getStatements() {
		synchronized (statementSetNotifier) {
			while (pythonRequest == null) {
				try {
					statementSetNotifier.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			PythonRequest req = pythonRequest;
			pythonRequest = null;
			return req;
		}
	}

	public void setStatementsFinished(String out, boolean error) {
		synchronized (statementFinishedNotifier) {
			//logger.debug("Setting python statement output: " + out + ", error: " + error);
			statementOutput = out;
			statementError = error;
			statementFinishedNotifier.notify();
		}
	}

	public void onPyScriptInitialized(long pid) {
		pythonPid = pid;
		synchronized (pyscriptInitialized) {
			pyscriptInitialized.set(true);			
			pyscriptInitialized.notifyAll();
			logger.info("onPyScriptInitialized is called");
		}
	}

	public void appendOutput(String message) throws IOException {
		outputStream.getExecOutput().write(message);
	}

	protected void preCallPython(RNContext context) {
		// do pre-process if need
	}

	protected void callPython(PythonRequest request) {
		synchronized (statementSetNotifier) {
			this.pythonRequest = request;
			statementOutput = null;
			statementSetNotifier.notify();
		}

		synchronized (statementFinishedNotifier) {
			while (statementOutput == null) {
				try {
					statementFinishedNotifier.wait(2000);
				} catch (InterruptedException e) {
					// ignore this exception
				}
			}
		}
	}

	@Override
	public RNResult exec(String st, RNContext context) throws TechException {
		if (!pyscriptRunning.get()) {
			return new RNResult(Code.ERROR, "python process not running " + outputStream.toString());
		}

		outputStream.setExecOutput(context.out);

		synchronized (pyscriptInitialized) {
			long startTime = System.currentTimeMillis();
			while (!pyscriptInitialized.get() && System.currentTimeMillis() - startTime < MAX_TIMEOUT_SEC * 1000) {
				try {
					logger.info("Wait for PyScript initialized");
					pyscriptInitialized.wait(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		List<ResultMessage> errorMessage;
		try {
			context.out.flush();
			errorMessage = context.out.toResultMessage();
		} catch (IOException e) {
			throw new TechException(e);
		}

		if (!pyscriptInitialized.get()) { // timeout. didn't get initialized message
			errorMessage.add(new ResultMessage(Type.TEXT, "Failed to initialize Python"));
			return new RNResult(Code.ERROR, errorMessage);
		}
		context.setProgressing(20);
		preCallPython(context);
		String pname = getAppName(context);
		PythonRequest req = new PythonRequest(pname, st, false);
		if(null != context.getSrcname()) req.setName(context.getSrcname());
		if(null != context.getParams() && !context.getParams().isEmpty()) {
			String[] argvs = ShellUtils.toPythonArgsList(context.getParams(), context.getJobType(), false);
			req.setArgv(ShellUtils.toArgsString(argvs).trim());
			logger.info("try to run python "+req.name()+" with params "+ShellUtils.toArgsString(ShellUtils.toPythonArgsList(context.getParams(), context.getJobType(), true)));
		}		
		updateJobDetails(context, pname);
		callPython(req);		
		context.setProgressing(50);
		if (statementError) {
			logger.error(statementOutput);
			return new RNResult(Code.ERROR, statementOutput);
		} else {
			try {
				context.out.flush();
			} catch (IOException e) {
				throw new TechException(e);
			}
			try {
				return new RNResult(Code.SUCCESS, context.out.toResultMessage());
			} catch (IOException e) {
				return new RNResult(Code.SUCCESS, "Successed " + context.out.toString());
			}
		}
	}
	
	@Transactional
	protected void updateJobDetails(RNContext ctx, String pname) {
		try {
			String tenant = ctx.getParam("tenant"); 
			long jobid = Long.parseLong(ctx.getParam("jobid"));
			Long id = Long.parseLong(ctx.getCtxid());
			
			if(null == jobrepo) {
				jobrepo = (JobRepository) lookupBean("jobRepository", JobRepository.class);
				jobdetailrepo = (JobDetailsRepository)lookupBean("jobDetailsRepository", JobDetailsRepository.class);
			}
			
			JobDetails jdl = jobdetailrepo.findByTenantAndJobid(tenant, id);			
			Job job = jobrepo.findById(id).get();
			
			if(job.getJobid() != jobid || !job.getTenant().equalsIgnoreCase(tenant)) {
				throw new RNBaseException("Job not match with teant "+tenant+" and jobid "+jobid);
			}
			if(null != jdl) {
				if(null != this.gatewayServer)
					jdl.setSessionid(this.getGatewayServer().getPort());					
				if(-1 != pythonPid)
					jdl.setStatementid((int)pythonPid);
				
				if(null != pname)jdl.setAppid(pname);
				
				jdl.setStatus(Status.RUNNING.ordinal());
				jdl.setLastModified(new Date());
				jobdetailrepo.saveAndFlush(jdl);
			}	
			job.setStatus(Status.RUNNING.ordinal());
			String msg = getPyExec()+" "+ ((null != pypath)?"":"PYTHONPATH: " + pypath);
			job.setDescription(msg);
			job.setLastModified(new Date());
			jobrepo.saveAndFlush(job);
		} catch (Exception e) {
			logger.error("Update job failed:\n"+e.getMessage());
		}
	}

	@Override
	public IJobExecutor withRepo(JobRepository jobrepo, JobDetailsRepository jobdetailrepo) {
		this.jobrepo = jobrepo;
		this.jobdetailrepo = jobdetailrepo;
		return this;
	}

	public void interrupt() throws IOException, TechException {
		if (pythonPid > -1) {
			logger.info("Sending SIGINT signal to PID : " + pythonPid);
			Runtime.getRuntime().exec("kill -SIGINT " + pythonPid);
		} else {
			logger.warn("Non UNIX/Linux system, close the interpreter");
			close();
		}
	}

	@Override
	public void cancel(RNContext context) throws TechException {
		try {
			interrupt();
		} catch (IOException e) {
			logger.error("Error", e);
		}
	}

	@Override
	public List<ResultCompletion> completion(String buf, int cursor, RNContext context) {
		if (buf.length() < cursor) {
			cursor = buf.length();
		}
		String completionString = getCompletionTargetString(buf, cursor);
		String completionCommand = "__rn_completion__.getCompletion('" + completionString + "')";
		logger.debug("completionCommand: " + completionCommand);

		pythonRequest = new PythonRequest("completed",completionCommand, true);
		statementOutput = null;

		synchronized (statementSetNotifier) {
			statementSetNotifier.notify();
		}

		String[] completionList = null;
		synchronized (statementFinishedNotifier) {
			long startTime = System.currentTimeMillis();
			while (statementOutput == null && pyscriptRunning.get()) {
				try {
					if (System.currentTimeMillis() - startTime > MAX_TIMEOUT_SEC * 1000) {
						logger.error("Python completion didn't have response for {}sec.", MAX_TIMEOUT_SEC);
						break;
					}
					statementFinishedNotifier.wait(1000);
				} catch (InterruptedException e) {
					// not working
					logger.info("wait drop");
					return new LinkedList<>();
				}
			}
			if (statementError) {
				return new LinkedList<>();
			}
			Gson gson = new Gson();
			completionList = gson.fromJson(statementOutput, String[].class);
		}
		// end code for completion
		if (completionList == null) {
			return new LinkedList<>();
		}

		List<ResultCompletion> results = new LinkedList<>();
		for (String name : completionList) {
			results.add(new ResultCompletion(name, name, StringUtils.EMPTY));
		}
		return results;
	}

	private String getCompletionTargetString(String text, int cursor) {
		String[] completionSeqCharaters = { " ", "\n", "\t" };
		int completionEndPosition = cursor;
		int completionStartPosition = cursor;
		int indexOfReverseSeqPostion = cursor;

		String resultCompletionText = "";
		String completionScriptText = "";
		try {
			completionScriptText = text.substring(0, cursor);
		} catch (Exception e) {
			logger.error(e.toString());
			return null;
		}
		completionEndPosition = completionScriptText.length();

		String tempReverseCompletionText = new StringBuilder(completionScriptText).reverse().toString();

		for (String seqCharacter : completionSeqCharaters) {
			indexOfReverseSeqPostion = tempReverseCompletionText.indexOf(seqCharacter);

			if (indexOfReverseSeqPostion < completionStartPosition && indexOfReverseSeqPostion > 0) {
				completionStartPosition = indexOfReverseSeqPostion;
			}

		}

		if (completionStartPosition == completionEndPosition) {
			completionStartPosition = 0;
		} else {
			completionStartPosition = completionEndPosition - completionStartPosition;
		}
		resultCompletionText = completionScriptText.substring(completionStartPosition, completionEndPosition);

		return resultCompletionText;
	}
	
	@Override
	public RNResult status(RNContext ctx) throws RNBaseException {
		StringBuilder sb = new StringBuilder();
		try {
			String tenant = ctx.getParam("tenant"); 
			Long id = Long.parseLong(ctx.getCtxid());
			Job job = jobrepo.findById(id).get();
			sb.append(job.getTenant()+" with jobid "+job.getJobid()+" run at "+job.getJobExecutedBy());
			JobDetails jdl = jobdetailrepo.findByTenantAndJobid(tenant, id);
			if(null != jdl) {	
				sb.append(" with process id as "+jdl.getStatementid()+" ");
				sb.append(" with params as "+jdl.getParams()+" ");				
				RNResult res = new RNResult(RNResult.Code.ERROR, "Empty output");
				res.setSessionid(jdl.getStatementid());
				res.setRetType(Type.TEXT);
				boolean stillRunning = isProcessRunning(jdl.getStatementid(), 1, TimeUnit.SECONDS, sb);
				if(stillRunning) {
					res.setRetCode(RNResult.Code.PROCESSING);
				}else {
					res.setRetCode(RNResult.Code.COMPLETED);
				}
				res.setMsg(sb.toString());
				return res;
			}
		} catch (Exception e) {
			sb.append(e.getMessage());
			logger.info("Error: "+sb.toString());
			throw new RNBaseException(e.getMessage());
		}
				
		return null;
	}
	
	
	public static boolean isProcessRunning(int pid, int timeout, TimeUnit timeunit, StringBuilder sb) throws java.io.IOException {
	    String line;
	    String OS = System.getProperty("os.name").toLowerCase();
	    if (SystemUtils.IS_OS_WINDOWS) {
	        //tasklist exit code is always 0. Parse output
	        //findstr exit code 0 if found pid, 1 if it doesn't
	        line = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
	    }
	    else {
	        //ps exit code 0 if process exists, 1 if it doesn't   //`-p` is POSIX/BSD-compliant
	        line = "ps -fp " + pid;      
	    }
	    CommandLine cmdLine = CommandLine.parse(line);
	    DefaultExecutor executor = new DefaultExecutor();
	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    executor.setStreamHandler(new PumpStreamHandler(outputStream));
	    executor.setExitValues(new int[]{0, 1});
	    ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
	    executor.setWatchdog(timeoutWatchdog);
	    Map<String, String> env = EnvironmentUtils.getProcEnvironment();
	    int exitValue = executor.execute(cmdLine, env);
	    sb.append("\n").append(outputStream.toString());
	    // 0 is the default exit code which means the process exists
	    return exitValue == 0;
	}

	protected void bootstraper(String resourceName) throws IOException {
		logger.info("Bootstrap interpreter via " + resourceName);
		String bootstrapCode = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceName));
		try {
			// Add hook explicitly, otherwise python will fail to execute the statement
			RNResult result = exec(bootstrapCode + "\n" + "__rn__._displayhook()", RNContext.get());
			if (result.code() != Code.SUCCESS) {
				throw new IOException("Fail to run bootstrap script: " + resourceName);
			}
		} catch (TechException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void onProcessComplete(int exitValue) {
		logger.info("python process terminated. exit code " + exitValue);
		pyscriptRunning.set(false);
		pyscriptInitialized.set(false);
	}

	@Override
	public void onProcessFailed(ExecuteException e) {
		if(e.getExitValue() != 134 && e.getExitValue() > 0) //SIGABRT 
			logger.error("python process failed", e);
		pyscriptRunning.set(false);
		pyscriptInitialized.set(false);
	}

	// Called by Python Process, used for debugging purpose
	public void logPythonOutput(String message) {
		logger.debug("Python Process Output: " + message);
	}

	public RNCLogOutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(RNCLogOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public AtomicBoolean getPyScriptRunning() {
		return pyscriptRunning;
	}

	public AtomicBoolean getPyScriptInitialized() {
		return pyscriptInitialized;
	}

	public GatewayServer getGatewayServer() {
		return gatewayServer;
	}

	public void setGatewayServer(GatewayServer gatewayServer) {
		this.gatewayServer = gatewayServer;
	}

	@Override
	public void initProp(Properties property) {
		// TODO Auto-generated method stub

	}
}
