package com.rnctech.nrdataservice.service.livyimpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.rnctech.nrdataservice.RNConsts.STATUS;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.exception.APILivyException;
import com.rnctech.nrdataservice.exception.APISessionException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.job.RNJob.Status;
import com.rnctech.nrdataservice.repo.Job;
import com.rnctech.nrdataservice.repo.JobDetails;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.service.IJobExecutor;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.service.RNResult.Type;
import com.rnctech.nrdataservice.utils.RNUtilities;
import com.rnctech.nrdataservice.utils.ShellUtils;
import com.rnctech.nrdataservice.utils.ConfigClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for livy executor.
 * @author zilin chen
 * @since 2020.10
 */

public abstract class BaseLivyExecutor extends RNJobExecutor {

	protected static final Logger logger = LoggerFactory.getLogger(BaseLivyExecutor.class);
	protected static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public final static String SPARK_EXECUTOR_CORES = "spark.executor.cores";
	public final static String SPARK_EXECUTOR_MEMORY = "spark.executor.memory";
	public final static String SPARK_DRIVER_CORES = "spark.driver.cores";
	public final static String SPARK_DRIVER_MEMORY = "spark.driver.memory";
	public final static String SPARK_PYSPARK_MEMORY = "spark.executor.pyspark.memory";
	public final static String SPARK_NUMBER_EXECUTORS = "spark.executor.instances";
	
	protected volatile SessionInfo sessionInfo;
	protected String livyURL;
	private int sessionCreationTimeout;
	protected int pullStatusInterval;
	protected int maxQueuedCount;
	protected int maxLogLines;
	protected boolean displayAppInfo;
	protected boolean retry = true;  //by default, retry once
	protected int progress = -1;
	protected int sessionid = -1;
	protected boolean closeSession = true;
	protected boolean cancelSession = false;
	JobRepository jobrepo;
	JobDetailsRepository jobdetailrepo;
	protected boolean isUnitest = false;

	public static enum LIVYEXE_TYPE {
		interactive, batch
	}

	protected LIVYEXE_TYPE exetype = LIVYEXE_TYPE.interactive;

	private RestTemplate restTemplate;
	private Map<String, String> customHeaders = new HashMap<>();

	public BaseLivyExecutor(Properties property) {
		super(property);
	}

	public void initProp(Properties property) {
		this.livyURL = property.getProperty("livy.url");
		this.displayAppInfo = Boolean.parseBoolean(property.getProperty("livy.displayAppInfo", "false"));
		this.retry = Boolean.parseBoolean(property.getProperty("livy.restart_dead_session", "true"));
		this.cancelSession = Boolean.parseBoolean(property.getProperty("livy.cancel_queued_session", "false"));
		this.sessionCreationTimeout = Integer.parseInt(property.getProperty("livy.session.create_timeout", "120"));
		this.pullStatusInterval = Integer.parseInt(property.getProperty("livy.pull_status.interval.millis", "60000"));
		this.maxQueuedCount = Integer.parseInt(property.getProperty("livy.max_queued.count", "30"));
		this.maxLogLines = Integer.parseInt(property.getProperty("livy.maxLogLines", "200"));
		if(property.contains("rnctech.sessionid")) {
			this.sessionid = Integer.parseInt(property.getProperty("rnctech.sessionid", "-1"));
			this.closeSession = Boolean.parseBoolean(property.getProperty("rnctech.closesession", "true")); //default will close the session
		}
		logger.info(BaseLivyExecutor.class.getName()+" initized.");
	}
	
	public Map<String, String> getCustomHeaders() {
		return customHeaders;
	}

	public abstract String getSessionKind();

	@Override
	public void open() throws RNBaseException {
		try {
			this.restTemplate = createRestTemplate();
			if (this.exetype.equals(LIVYEXE_TYPE.interactive)) {
				initLivySession();
			} 
		} catch (APIException e) {
			String msg = "Fail to create session, please check livy server log";
			throw new RNBaseException(msg, e);
		}
	}

	@Override
	public void close() {
		if (closeSession && sessionInfo != null) {
			closeForcely();
		}
		sessionid = -1;
	}

	protected void closeForcely() {
		closeSession(sessionInfo.id);
		sessionInfo = null;
	}
	
	
	protected void initLivySession() throws APIException {
		boolean sessionready = false;
		if(-1 != this.sessionid) {
			try {
				sessionInfo = getSessionInfo(this.sessionid);
				if(sessionInfo.isActive())
					sessionready = true;
			}catch(Throwable t) {}
		}
		
		if(!sessionready) {
			initLivySession(true);
		}
	}
	
	protected void initLivySession(boolean recreate) throws APIException {
		this.sessionInfo = createSession(getUserName(), getSessionKind());
		if (displayAppInfo) {
			if (sessionInfo.appId == null) {
				sessionInfo.appId = extractAppId();
			}

			if (sessionInfo.appInfo == null || StringUtils.isEmpty(sessionInfo.appInfo.get("sparkUiUrl"))) {
				sessionInfo.webUIAddress = extractWebUIAddress();
			} else {
				sessionInfo.webUIAddress = sessionInfo.appInfo.get("sparkUiUrl");
			}
			logger.info("Create livy session successfully with sessionId: {}, appId: {}, webUI: {}", sessionInfo.id,
					sessionInfo.appId, sessionInfo.webUIAddress);
		} else {
			logger.info("Create livy session successfully with sessionId: {}", this.sessionInfo.id);
		}
		this.sessionid = sessionInfo.id;

	}

	protected abstract String extractAppId() throws APIException;

	protected abstract String extractWebUIAddress() throws APIException;

	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}

	public int getSessionid() {
		return sessionid;
	}

	public String getCodeType() {
		if (getSessionKind().equalsIgnoreCase("pyspark3")) {
			return "pyspark";
		}
		return getSessionKind();
	}

	public RNResult exec(String st, RNContext context) throws APIException {
		if (StringUtils.isEmpty(st)) {
			return new RNResult(RNResult.Code.ERROR, "No snapcode fefined to run.");
		}

		try {
			return exec(st, context, getCodeType(), this.displayAppInfo);
		} catch (APIException e) {
			logger.error("Fail to interpret:" + st+":\n"+e.getMessage());
			if(e instanceof APILivyException) {
				return new RNResult(RNResult.Code.ERROR, ExceptionUtils.getRootCauseMessage(e));
			}else {
				return new RNResult(RNResult.Code.INCOMPLETE, ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public void cancel(RNContext ctx) throws RNBaseException {
		if(null == restTemplate)
			restTemplate = createRestTemplate();
		StringBuilder sb = new StringBuilder();
		try {
			String tenant = ctx.getParam("tenant"); 
			Long id = Long.parseLong(ctx.getCtxid());
			Job job = jobrepo.findById(id).get();
			sb.append(job.getTenant()+" cancal job "+job.getJobid()+" run at "+job.getJobExecutedBy());
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);
			if(null != jdl) {	
				try {
					sb.append(" with statement id as "+jdl.getStatementid()+" ");
					cancelStatementOrKillBatch(jdl.getSessionid(), jdl.getStatementid());
				} catch (Exception e) {
					if(e instanceof APISessionException) {
						//session already done!
					}else {
						throw e;
					}					
				}	
			}else {
				throw new Exception("No job found for tenant "+job.getTenant()+" with jobid as "+job.getJobid());
			}
		}catch(Exception e) {
			throw new RNBaseException(e.getMessage());
		}finally {
			close();
		}
	}

	@Override
	public int getProgress(RNContext context) throws RNBaseException {
		return this.progress;
	}

	private SessionInfo createSession(String user, String kind) throws APIException {
		try {
			Map<String, String> conf = new HashMap<>();
			for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
				if (entry.getKey().toString().startsWith("livy.spark.") && !entry.getValue().toString().isEmpty()) {
					conf.put(entry.getKey().toString().substring(5), entry.getValue().toString());
				}
			}

			CreateSessionRequest request = new CreateSessionRequest(kind,
					user == null || user.equals("anonymous") ? null : user, conf);
			
			logger.debug("Call rest api in {}, method: {}, jsonData: {}", livyURL +"/sessions", "POST", request.toJson());
			SessionInfo sessionInfo = SessionInfo.fromJson(callRestAPI("/sessions", "POST", request.toJson()));
			long start = System.currentTimeMillis();
			// pull the session status until it is idle or timeout
			while (!sessionInfo.isReady()) {
				if ((System.currentTimeMillis() - start) / 1000 > sessionCreationTimeout) {
					String msg = "The creation of session " + sessionInfo.id + " is timeout within "
							+ sessionCreationTimeout + " seconds, appId: " + sessionInfo.appId + ", log:\n"
							+ StringUtils.join(getSessionLog(sessionInfo.id).log, "\n");
					throw new APIException(msg);
				}
				Thread.sleep(pullStatusInterval);
				sessionInfo = getSessionInfo(sessionInfo.id);
				logger.info("Session {} is in state {}, appId {}", sessionInfo.id, sessionInfo.state,
						sessionInfo.appId);
				if (sessionInfo.isFinished()) {
					String msg = "Session " + sessionInfo.id + " is finished, appId: " + sessionInfo.appId + ", log:\n"
							+ StringUtils.join(getSessionLog(sessionInfo.id).log, "\n");
					throw new APIException(msg);
				}
			}
			this.progress = 20;
			return sessionInfo;
		} catch (Exception e) {
			logger.error("Error when creating livy session for user " + user, e);
			throw new APIException(e);
		}
	}
	
	@Override
	public boolean validate(String livyurl) throws RNBaseException{
		try {
			if((null == livyurl || livyurl.trim().isEmpty()) && null == this.livyURL)
				throw new APIException("No url found.");
			
			if(null != livyurl && !livyurl.trim().isEmpty())
				this.livyURL = livyurl;
			
			if(null == restTemplate) restTemplate = createRestTemplate();
			String ret = callRestAPI("/sessions", "GET");
			logger.info(this.livyURL+" validated as "+ret);
			return true;
		} catch (APIException e) {
			logger.info(livyurl+" validate failed: "+e.getMessage());
			return false;
		}
	}

	private SessionInfo getSessionInfo(int sessionId) throws APIException {
		return SessionInfo.fromJson(callRestAPI("/sessions/" + sessionId, "GET"));
	}

	private SessionLog getSessionLog(int sessionId) throws APIException {
		if (this.exetype.equals(LIVYEXE_TYPE.interactive)) {
			return SessionLog.fromJson(callRestAPI("/sessions/" + sessionId + "/log?size=" + maxLogLines, "GET"));
		}else {
			return SessionLog.fromJson(callRestAPI("/batches/" + sessionId + "/log?size=" + maxLogLines, "GET"));
		}
	}

	public RNResult exec(String code, RNContext context, String codeType, boolean displayAppInfo) throws APIException {
		if (this.exetype.equals(LIVYEXE_TYPE.interactive)) {
			return execInteractive(code, context, codeType, displayAppInfo);
		} else { 
			return execBatch(code, context, codeType, displayAppInfo);			
		}
	}
	
	protected RNResult execBatch(String code, RNContext context, String codeType, boolean displayAppInfo) throws APIException {
		RNResult res = null;
		StatementInfo stmtInfo = null;
		closeSession = true;
		String[] argvs = ShellUtils.toArgsList(context.getParams(), context.getJobType());
		String jname = getAppName(context); //codeType + context.getCodesnap().substring(context.getCodesnap().lastIndexOf("/")+1);
		try {
			BatchRequest req = null;
			if(null != context.getDependenics() && !context.getDependenics().isEmpty()) {
				req = new BatchRequest(context, getSparkConfig(), jname, argvs, null);
			}else {
				req = new BatchRequest(context.getCodesnap(), null, getSparkConfig(), jname, argvs);
			}
			setSparkConf(context, req);
			try {
				logger.info("try to run batch "+jname+" with params "+ShellUtils.toArgsString(ShellUtils.toArgsList(context.getParams(), context.getJobType(), true)));
				stmtInfo = executeBatch(req);
				logger.info("kicked spark batch run "+jname+" with batch id as "+stmtInfo.id);
			} catch (Exception e) {
				if(e instanceof APILivyException)
					throw e;
				
				if (retry) {
					logger.warn("Livy batch run failed, retry it.", e.getMessage());
					stmtInfo = executeBatch(req);
				} else {
					throw new APIException(e);
				}
			}
			
			if(!isUnitest) updateJobDetails(context, stmtInfo, exetype, jname);		
			int queuedtimes = 0;
			while (!stmtInfo.isCompleted()) {
				try {
					Thread.sleep(pullStatusInterval);
				} catch (InterruptedException e) {
					logger.error("InterruptedException when pulling statement status.", e);
					throw new APIException(e);
				}
				stmtInfo = getStatementInfo(-1, stmtInfo.id);
				if(stmtInfo.isQuenued()) {
					queuedtimes++;
					if(queuedtimes > maxQueuedCount) 
						handleQueuedSession(context, jname, stmtInfo, true);
					
					if(queuedtimes % 3 == 0)
						updateMessage(context, jname, stmtInfo);
				}
			}

			res = getResultFromStatementInfo(stmtInfo, displayAppInfo);			
		} catch (Throwable t) {
			throw t;
			//res = getResultFromStatementInfo(new StatementInfo(), false);
		} finally {
			this.progress = 100;
		}
		res.setSessionid(stmtInfo.id);
		return res;
	}
	
	protected void updateMessage(RNContext context, String jname, StatementInfo stmtInfo) {
		JobData jsd = getJSD(context);
		if(null != jsd) { // try to update status of the job if it's MR job
			try {
				String msg = "Job "+jname + " with appId " + ((null != sessionInfo.appId)?sessionInfo.appId:stmtInfo.appId) + " is queued." ;
				ConfigClient.updateProperties(jsd, STATUS.PROGRESSING, msg, null, null);
			} catch (Exception e) {
			}
		}		
	}

	protected void handleQueuedSession(RNContext context, String jname, StatementInfo sessionInfo, boolean isBatch) throws APIException {
			String msg = "The session " + sessionInfo.id + " is queued within "
					+ (pullStatusInterval * maxQueuedCount / 1000) + " seconds, appId: " + sessionInfo.appId + ", log:\n"
					+ sessionInfo.toLogString() + "\n";
			
			if(this.cancelSession) {
				try {
					if(isBatch) cancelStatementOrKillBatch(-1, sessionInfo.id);
					else cancelStatementOrKillBatch(sessionInfo.id, -1);
					updateJobDetails(context, sessionInfo, exetype, jname, Status.ABORT.ordinal());
				} catch (Exception e) {
					if(e instanceof APISessionException) {
						//session already done!
					}else {
						throw new APIException(e);
					}	
				}
				throw new APIException(msg);
			}else {				
				logger.warn(msg);
			}
	}
		
	protected void setSparkConf(RNContext context, BatchRequest req) {
		if(null != context.getConf() && !context.getConf().isEmpty()) {
			Map<String, String> conf = context.getConf();
			for(Map.Entry<String, String> entry : conf.entrySet()) {				
				String v = entry.getValue();
				if(null != v && 0 != v.trim().length()) {
					try {
						String key = entry.getKey();
						if(key.equalsIgnoreCase(SPARK_DRIVER_CORES)) {
							req.setDriverCores(Integer.parseInt(v.trim()));
							continue;
						}
						if(key.equalsIgnoreCase(SPARK_DRIVER_MEMORY)) {
							req.setDriverMemory(v.trim());
							continue;
						}
						if(key.equalsIgnoreCase(SPARK_EXECUTOR_CORES)) {
							req.setExecutorCores(Integer.parseInt(v.trim()));
							continue;
						}
						if(key.equalsIgnoreCase(SPARK_EXECUTOR_MEMORY)) {
							req.setExecutorMemory(v.trim());
							continue;
						}
						if(key.equalsIgnoreCase(SPARK_NUMBER_EXECUTORS)) {
							req.setNumExecutors(Integer.parseInt(v.trim()));
							continue;
						}					
						req.conf.put(key, v.trim());
					} catch (Exception e) {
						//ignore in case of can't fomat and continue
					}
				}
			}
		}
	}

	protected RNResult execInteractive(String code, RNContext context, String codeType, boolean displayAppInfo) throws APIException {
		RNResult res = null;
		StatementInfo stmtInfo = null;
		try {
			try {
				stmtInfo = executeStatement(new ExecuteRequest(code, codeType));
			} catch (APISessionException e) {
				logger.warn("Livy session {} is expired, new session will be created.", sessionInfo.id);
				synchronized (this) {
					if (isSessionExpired()) {
						initLivySession(true);
					}
				}
				stmtInfo = executeStatement(new ExecuteRequest(code, codeType));
			} catch (Exception e) {
				if(e instanceof APILivyException)
					throw e;
				
				if (retry) {
					logger.warn("Livy session {} is dead, new session will be created.", sessionInfo.id);
					closeForcely();
					try {
						open();
					} catch (RNBaseException ie) {
						throw new APIException("Fail to restart livy session", ie);
					}
					stmtInfo = executeStatement(new ExecuteRequest(code, codeType));
				} else {
					throw new APIException("Livy session is dead somehow, "
							+ "please check log to see why it is dead, and then restart livy");
				}
			}

			if(!isUnitest) updateJobDetails(context, stmtInfo, exetype, null);				
			// pull the statement status
			int queuedtimes = 0;
			while (!stmtInfo.isAvailable()) {
				try {
					Thread.sleep(pullStatusInterval);
				} catch (InterruptedException e) {
					logger.error("InterruptedException when pulling statement status.", e);
					throw new APIException(e);
				}
				stmtInfo = getStatementInfo(sessionInfo.id, stmtInfo.id);
				if(stmtInfo.isQuenued()) {
					queuedtimes++;
					if(queuedtimes > maxQueuedCount) handleQueuedSession(context, sessionInfo.appId, stmtInfo, false);
				}
			}
			res = getResultFromStatementInfo(stmtInfo, displayAppInfo);
			res.setSessionid(this.sessionid);
			return res;
		} finally {
			this.progress = 100;
		}
	}

	@Override
	public RNResult status(RNContext ctx) throws RNBaseException {
		if(null == restTemplate)
			restTemplate = createRestTemplate();
		StringBuilder sb = new StringBuilder();
		try {
			String tenant = ctx.getParam("tenant"); 
			Long id = Long.parseLong(ctx.getCtxid());
			Job job = jobrepo.findById(id).get();
			sb.append(job.getTenant()+" with jobid "+job.getJobid()+" run at "+job.getJobExecutedBy());
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);
			if(null != jdl) {	
				sb.append(" with statement id as "+jdl.getStatementid()+" ");
				sb.append(" with params as "+jdl.getParams()+" ");				
				StatementInfo stmtInfo = getStatementInfo(jdl.getSessionid(), jdl.getStatementid());
				String logmsg = (null != stmtInfo.log)?stmtInfo.toLogString():((null != stmtInfo.output && null != stmtInfo.output.data)?stmtInfo.output.data.plainText:"");
				
				RNResult res = getResultFromStatementInfo(stmtInfo, false);
				res.setRetCode(getRetCode(stmtInfo));
				try {
					SessionLog slog = getSessionLog(jdl.getSessionid());
					res.setSessionid(jdl.getSessionid());
					res.toResultMessage(slog.log);
				} catch (Exception e) {
				}	
				res.setRetType(Type.TEXT);
				res.setMsg(sb.toString()+": "+res.toString()+" "+logmsg);
				return res;
			}
		} catch (Exception e) {
			sb.append(e.getMessage());
			logger.info("Error: "+sb.toString());
			throw new RNBaseException(e.getMessage());
		}
				
		return null;
	}

	private Code getRetCode(StatementInfo stmtInfo) {
		if(null == stmtInfo || null == stmtInfo.state || stmtInfo.state.isEmpty())
			return Code.INITED;
		if(stmtInfo.isFailed()) return Code.FAILED;
		if(stmtInfo.isCompleted()) return Code.SUCCESS;
		if(stmtInfo.isRunning()) return Code.PROCESSING;
		if(stmtInfo.isCancelled()) return Code.CANCELLED;
		if(stmtInfo.isQuenued()) return Code.QUEUED;
		return Code.INITED;
	}
	
	protected void updateJobDetails(RNContext ctx, StatementInfo stmtInfo, LIVYEXE_TYPE ltype, String jname) {
		int status = Status.RUNNING.ordinal();
		updateJobDetails(ctx, stmtInfo, ltype, jname, status);
	}

	@Transactional
	protected void updateJobDetails(RNContext ctx, StatementInfo stmtInfo, LIVYEXE_TYPE ltype, String jname, int status) {
		try {
			String tenant = ctx.getParam("tenant"); 
			long jobid = Long.parseLong(ctx.getParam("jobid"));
			Long id = Long.parseLong(ctx.getCtxid());
			
			if(null == jobrepo) {
				jobrepo = (JobRepository) lookupBean("jobRepository", JobRepository.class);
				jobdetailrepo = (JobDetailsRepository)lookupBean("jobDetailsRepository", JobDetailsRepository.class);
			}
			
			JobDetails jdl = jobdetailrepo.findByNameAndJobid(tenant, id);			
			Job job = jobrepo.findById(id).get();
			
			if(job.getJobid() != jobid || !job.getTenant().equalsIgnoreCase(tenant)) {
				throw new RNBaseException("Job not match with teant "+tenant+" and jobid "+jobid);
			}
			//Set<JobDetails> jdls = job.getJobdetails();
			//JobDetails jdl = jdls.iterator().next();
			if(null != jdl) {
				if(ltype.equals(LIVYEXE_TYPE.interactive)) {					
					jdl.setSessionid(sessionInfo.id);					
				}
				if(null != jname)jdl.setAppid(jname);
				jdl.setStatementid(stmtInfo.id);
				jdl.setStatus(status);
				jdl.setLastModified(new Date());
				jobdetailrepo.saveAndFlush(jdl);
			}	
			job.setStatus(status);
			String msg = (null != stmtInfo.log)?stmtInfo.toLogString():((null != stmtInfo.output && null != stmtInfo.output.data)?stmtInfo.output.data.plainText:"");
			job.setDescription(msg);
			job.setLastModified(new Date());
			jobrepo.saveAndFlush(job);
		} catch (Exception e) {
			logger.error("Update job failed:\n"+e.getMessage());
		}
	}

	public RNResult getResult(int sid, int bid, boolean displayAppInfo) {		
		RNResult res = null;
		try{
			StatementInfo stmtInfo = getStatementInfo(sid, bid);
			res = getResultFromStatementInfo(stmtInfo, displayAppInfo);
		}catch(Exception e) {
			res = getResultFromStatementInfo(new StatementInfo(), false);
		}
		return res;
	}
	
	protected Map<String, String> getSparkConfig() {
		Map<String, String> sconf = new HashMap<>();
		sconf.put("spark.jars.packages", "mysql:mysql-connector-java:5.1.38,julioasotodv:spark-tree-plotting:0.2,org.apache.hadoop:hadoop-aws:2.7.1");
		sconf.put("spark.driver.extraJavaOptions","-Dlog4jspark.root.logger=WARN,console");
		return sconf;
	}

	protected boolean isSessionExpired() throws APIException {
		try {
			getSessionInfo(sessionInfo.id);
			return false;
		} catch (APISessionException e) {
			return true;
		} catch (APIException e) {
			throw e;
		}
	}

	protected RNResult getResultFromStatementInfo(StatementInfo stmtInfo, boolean displayAppInfo) {
		if (stmtInfo.output != null && stmtInfo.output.isError()) {
			RNResult result = new RNResult(RNResult.Code.ERROR, stmtInfo.state);
			StringBuilder sb = new StringBuilder();
			sb.append(stmtInfo.output.evalue);
			if (!stmtInfo.output.evalue.contains("\n")) {
				sb.append("\n");
			}

			if (stmtInfo.output.traceback != null) {
				for(String l : stmtInfo.output.traceback) {
					if(!l.isEmpty() && !l.contains(RNResult.LOG_FILTER))
						sb.append(l);
				}
					//sb.append(StringUtils.join(stmtInfo.output.traceback));
			}
			result.setMsg(sb.toString());
			result.add(Type.TEXT, sb.toString());
			return result;
		} else if (stmtInfo.isCancelled()) {
			// corner case, output might be null if it is cancelled.
			return new RNResult(RNResult.Code.ERROR, "Job is cancelled");
		} else if (stmtInfo.output == null) {
			// This case should never happen, just in case
			return new RNResult(RNResult.Code.ERROR, "Empty output");
		} else {			
			if (this.exetype.equals(LIVYEXE_TYPE.interactive)) {
			  String result = (null != stmtInfo.output.data)?stmtInfo.output.data.plainText:"";

			  // check table magic result first
			  if (stmtInfo.output.data.applicationLivyTableJson != null) {
				StringBuilder outputBuilder = new StringBuilder();
				boolean notFirstColumn = false;

				for (Map header : stmtInfo.output.data.applicationLivyTableJson.headers) {
					if (notFirstColumn) {
						outputBuilder.append("\t");
					}
					outputBuilder.append(header.get("name"));
					notFirstColumn = true;
				}

				outputBuilder.append("\n");
				for (List<Object> row : stmtInfo.output.data.applicationLivyTableJson.records) {
					outputBuilder.append(StringUtils.join(row, "\t"));
					outputBuilder.append("\n");
				}
				RNResult res = new RNResult(RNResult.Code.SUCCESS, RNResult.Type.TABLE, outputBuilder.toString());
				res.add(Type.TABLE, outputBuilder.toString());
				return res;
			} else if (stmtInfo.output.data.imagePng != null) {
				return new RNResult(RNResult.Code.SUCCESS, RNResult.Type.IMG,
						(String) stmtInfo.output.data.imagePng);
			} else if (result != null) {
				result = result.trim();
				if (result.startsWith("<link") || result.startsWith("<script") || result.startsWith("<style")
						|| result.startsWith("<div")) {
					result = "%html " + result;
				}
			}

			if (displayAppInfo) {
				RNResult res = new RNResult(Code.SUCCESS, "success");
				res.setMsg(result);
				String appInfoHtml = "<hr/>Spark Application Id: " + sessionInfo.appId + "<br/>"
						+ "Spark WebUI: <a href=\"" + sessionInfo.webUIAddress + "\">" + sessionInfo.webUIAddress
						+ "</a>";
				res.add(Type.HTML, appInfoHtml);
				return res;
			} else {
				RNResult res = new RNResult(RNResult.Code.SUCCESS, result);
				res.add(Type.TEXT, result);
				return res;
			}		
		}else {	
			RNResult res = new RNResult(Code.SUCCESS, "success");
			if(null != stmtInfo.log && !stmtInfo.log.isEmpty())res.toResultMessage(stmtInfo.log);
			String result = (null != stmtInfo.output.data)?stmtInfo.output.data.plainText:"";
			res.setMsg(result);
			if (displayAppInfo) {				
				String appInfoHtml = "<hr/>Spark Application Id: " + sessionInfo.appId + "<br/>"
						+ "Spark WebUI: <a href=\"" + sessionInfo.webUIAddress + "\">" + sessionInfo.webUIAddress
						+ "</a>";
				res.add(Type.HTML, appInfoHtml);
			}else {
				res.add(Type.TEXT, result);
			}
			
			if(stmtInfo.isFailed()) {	
				res.setRetCode(Code.ERROR);
				try {					
					SessionLog slog = getSessionLog(stmtInfo.id);
					res.resetResMsg(slog.log);
					if(0 == res.message().size())
						res.toResultMessage(slog.log);
				} catch (APIException e) {
					res.setMsg(stmtInfo.toLogString());
				}
				
				return res;
			}else if(stmtInfo.isCompleted()) {
				res.setRetCode(Code.SUCCESS);
				try {
					SessionLog slog = getSessionLog(stmtInfo.id);
					res.resetResMsg(slog.log);
					if(0 == res.message().size())
						res.toResultMessage(stmtInfo.log);
				} catch (Exception e) {
				}
				return res;
			} else {
				res.setRetCode(Code.SUCCESS);
				return res;
			}
		}
	}
	}

	protected StatementInfo executeStatement(ExecuteRequest executeReq) throws APIException {
		String rapi= "/sessions/" + sessionInfo.id + "/statements";
		logger.debug("Call rest api in {}, method: {}, jsonData: {}", livyURL + rapi, "POST", executeReq.toJson());
			return StatementInfo.fromJson(
					callRestAPI(rapi, "POST", executeReq.toJson()));
	}
	
	protected StatementInfo executeBatch(BatchRequest batchReq) throws APIException {
		logger.debug("Call rest api in {}, method: {}, jsonData: {}", livyURL +"/batches", "POST", batchReq.toString());
		return StatementInfo.fromJson(
					callRestAPI("/batches", "POST", batchReq.toJson()));
	}

	protected StatementInfo getStatementInfo(int sid, int id) throws APIException {
		StatementInfo sinfo = null;
		if(-1 == id) throw new APIException("no statement/batch id "+ id +" found.");
		if (-1 != sid) { //this.exetype.equals(LIVYEXE_TYPE.interactive)) {
			sinfo = StatementInfo.fromJson(callRestAPI("/sessions/" + sid + "/statements/" + id, "GET"));
		} else {
			
			sinfo = StatementInfo.fromJson(callRestAPI("/batches/" + id, "GET"));
		}

		return sinfo;
	}

	private void cancelStatementOrKillBatch(int sid, int id) throws APIException {
		if (-1 != sid) {  //(this.exetype.equals(LIVYEXE_TYPE.interactive)) {
			callRestAPI("/sessions/" + sid + "/statements/" + id + "/cancel", "POST");
		} else {
			callRestAPI("/batches/" + id, "DELETE");
		}
	}

	public LIVYEXE_TYPE getExetype() {
		return exetype;
	}

	public void setExetype(LIVYEXE_TYPE exetype) {
		this.exetype = exetype;
	}

	private RestTemplate createRestTemplate() {
		boolean usessl = livyURL.startsWith("https:");
		return createRestTemplate(usessl, getProperty("livy.ssl.trustStore"), getProperty("livy.ssl.trustStorePassword"));
	}

	protected String callRestAPI(String targetURL, String method) throws APIException {
		targetURL = livyURL + targetURL;
		return callRestAPI(restTemplate, customHeaders, targetURL, method, "");
	}

	protected String callRestAPI(String targetURL, String method, String jsonData) throws APIException {
		targetURL = livyURL + targetURL;
		return callRestAPI(restTemplate, customHeaders, targetURL, method, jsonData);
	}

	private void closeSession(int id) {
		try {
			if (this.exetype.equals(LIVYEXE_TYPE.interactive)) {
				callRestAPI("/sessions/" + id, "DELETE");
			}
		} catch (Exception e) {
			logger.error(String.format("Error closing session for user with session ID: %s", id), e);
		}
	}
	
	@Override
	public IJobExecutor withRepo(JobRepository jobrepo, JobDetailsRepository jobdetailrepo) {
		this.jobrepo = jobrepo;
		this.jobdetailrepo = jobdetailrepo;
		return this;
	}
	
	private static class CreateSessionRequest {
		public final String kind;
		@SerializedName("proxyUser")
		public final String user;
		public final Map<String, String> conf;

		CreateSessionRequest(String kind, String user, Map<String, String> conf) {
			this.kind = kind;
			this.user = user;
			this.conf = conf;
		}

		public String toJson() {
			return gson.toJson(this);
		}
	}

	public static class SessionInfo {
		public final int id;
		public String appId;
		public String webUIAddress;
		public final String owner;
		public final String proxyUser;
		public final String state;
		public final String kind;
		public final Map<String, String> appInfo;
		public final List<String> log;

		public SessionInfo(int id, String appId, String owner, String proxyUser, String state, String kind,
				Map<String, String> appInfo, List<String> log) {
			this.id = id;
			this.appId = appId;
			this.owner = owner;
			this.proxyUser = proxyUser;
			this.state = state;
			this.kind = kind;
			this.appInfo = appInfo;
			this.log = log;
		}

		public boolean isReady() {
			return state.equals("idle");
		}

		public boolean isFinished() {
			return state.equals("error") || state.equals("dead") || state.equals("success");
		}
		
		public boolean isActive() {
			return !state.equals("dead");
		}

		public static SessionInfo fromJson(String json) {
			return gson.fromJson(json, SessionInfo.class);
		}
	}

	private static class SessionLog {
		public int id;
		public int from;
		public int size;
		public int total;
		public List<String> log;

		SessionLog() {
		}

		public static SessionLog fromJson(String json) {
			return gson.fromJson(json, SessionLog.class);
		}
		
	}

	static class BatchRequest {
		public final String file;
		public final String[] pyFiles;
		public final Map<String, String> conf;
		public final String name;
		public final String[] args;
		public final String[] jars;
		public final String[] files;
		public final String[] archives;
		public final String className; 
		public String driverMemory;
		public Integer driverCores;
		public String executorMemory;
		public Integer executorCores;
		public Integer numExecutors;

		BatchRequest(String fs, String[] pyFiles, Map<String, String> spkconf, String name, String[] argv) {
			this(fs, pyFiles, spkconf,name, argv, null, null, null, null);
		}
		
		BatchRequest(String fs, String[] pyFiles, Map<String, String> spkconf, String name, String[] argv, String[] ljars) {
			this(fs, pyFiles, spkconf,name, argv, ljars, null, null, null);
		}
		
		BatchRequest(String fs, String[] pyFiles, Map<String, String> spkconf, String name, String[] argv, String[] ljars, String[] lfiles) {
			this(fs, pyFiles, spkconf,name, argv, ljars, lfiles, null, null);
		}
		
		BatchRequest(String fs, String[] pyFiles, Map<String, String> spkconf, String name, String[] argv, String[] ljars, String[] lfiles, String jclsname, String[] avs) {
			this.file = fs;
			this.pyFiles = pyFiles;
			this.conf = spkconf;
			this.name = name;
			this.args = argv;
			this.jars = ljars;
			this.files = lfiles;
			this.className = jclsname;
			this.archives = avs;
		}

		public BatchRequest(RNContext context, Map<String, String> sparkConfig, String jname, String[] argvs, String jclsname) {			
			this.file = context.getCodesnap();
			this.conf = sparkConfig;
			this.name = jname;
			this.args = argvs;
			if(null == jclsname) {				
				this.pyFiles = ShellUtils.getDependencies(context.getDependenics(), new String[]{".py",".zip"});				
			}else {
				this.pyFiles = null;
			}
			this.files = ShellUtils.getDependencies(context.getDependenics(), new String[]{".txt", ".csv"});
			this.jars = ShellUtils.getDependencies(context.getDependenics(), new String[]{".jar"});
			this.className = jclsname;
			this.archives = null;
		}

		public void setDriverMemory(String driverMemory) {
			this.driverMemory = driverMemory;
		}

		public void setDriverCores(int driverCores) {
			this.driverCores = driverCores;
		}

		public void setExecutorMemory(String executorMemory) {
			this.executorMemory = executorMemory;
		}

		public void setExecutorCores(int executorCores) {
			this.executorCores = executorCores;
		}

		public void setNumExecutors(int numExecutors) {
			this.numExecutors = numExecutors;
		}

		public String toJson() {
			return gson.toJson(this);
		}
		
		public String toString() {
			return "name: "+this.name+", file: "+this.file+((null != this.className)?", className: "+this.className:"")+", conf: "+RNUtilities.getString(this.conf, false);
		}
	}

	static class ExecuteRequest {
		public final String code;
		public final String kind;

		ExecuteRequest(String code, String kind) {
			this.code = code;
			this.kind = kind;
		}

		public String toJson() {
			return gson.toJson(this);
		}
	}

	static class StatementInfo {
		public Integer id = -1;
		public String state = "error";
		public String appId;
		public Map<String, String> appInfo;
		public List<String> log;
		public StatementOutput output;

		public StatementInfo() {
			output = new StatementOutput("failed");
		}

		public static StatementInfo fromJson(String json) {
			String rightJson = "";
			try {
				gson.fromJson(json, StatementInfo.class);
				rightJson = json;
			} catch (Exception e) {
				if (json.contains("\"traceback\":{}")) {
					logger.debug("traceback type mismatch, replacing the mismatching part ");
					rightJson = json.replace("\"traceback\":{}", "\"traceback\":[]");
					logger.debug("new json string is {}", rightJson);
				}
			}
			return gson.fromJson(rightJson, StatementInfo.class);
		}

		public boolean isAvailable() {
			return state.equals("available") || state.equals("cancelled");
		}

		public boolean isCancelled() {
			return state.equals("cancelled");
		}
		
		public boolean isRunning() {
			return state.equals("running");
		}
		
		public boolean isCompleted() {
			return state.equals("success") || state.equals("dead") || state.equals("failed");
		}
		
		public boolean isFailed() {
			return state.equals("dead") || state.equals("failed");
		}
		
		public boolean isQuenued() {
			return state.equals("waiting") || state.equals("accepted");
		}

		public String toLogString() {
			if(null == log || log.isEmpty()) return "";
			StringBuilder sb = new StringBuilder();
			log.stream().forEach(l -> {if(!l.isEmpty() && !l.contains(RNResult.LOG_FILTER)) sb.append(l).append("\n");});
			return sb.toString();
		}
		
		private static class StatementOutput {
			public String status;
			public String executionCount;
			public Data data;
			public String ename;
			public String evalue;
			public String[] traceback;
			public TableMagic tableMagic;

			public StatementOutput(String status) {
				this.status = status;
			}

			public boolean isError() {
				return status.equals("error");
			}

			public String toJson() {
				return gson.toJson(this);
			}

			private static class Data {
				@SerializedName("text/plain")
				public String plainText;
				@SerializedName("image/png")
				public String imagePng;
				@SerializedName("application/json")
				public String applicationJson;
				@SerializedName("application/vnd.livy.table.v1+json")
				public TableMagic applicationLivyTableJson;
			}

			private static class TableMagic {
				@SerializedName("headers")
				List<Map> headers;

				@SerializedName("data")
				List<List> records;
			}
		}
	}

	static class CompletionRequest {
		public final String code;
		public final String kind;
		public final int cursor;

		CompletionRequest(String code, String kind, int cursor) {
			this.code = code;
			this.kind = kind;
			this.cursor = cursor;
		}

		public String toJson() {
			return gson.toJson(this);
		}
	}

	static class CompletionResponse {
		public final String[] candidates;

		CompletionResponse(String[] candidates) {
			this.candidates = candidates;
		}

		public static CompletionResponse fromJson(String json) {
			return gson.fromJson(json, CompletionResponse.class);
		}
	}

	public boolean isUnitest() {
		return isUnitest;
	}

	public void setUnitest(boolean isUnitest) {
		this.isUnitest = isUnitest;
	}

}
