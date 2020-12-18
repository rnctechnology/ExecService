package com.rnctech.nrdataservice.service.livyimpl;

import java.util.Properties;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.exception.APILivyException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.utils.ShellUtils;

/**
 * Livy Spark java executor for java/scala.
 * 
 * @author zilin chen
 * @since 2020.10
 */

@Primary
@Component
public class LivyJavaExecutor extends BaseLivyExecutor {

	public LivyJavaExecutor(Properties property) {
		super(property);
	}

	@Override
	public String getSessionKind() {
		return "spark";
	}

	@Override
	public RNResult execBatch(String code, RNContext context, String codeType, boolean displayAppInfo) throws APIException {
		RNResult res = null;
		StatementInfo stmtInfo = null;
		try {
			closeSession = true;
			String[] argvs = ShellUtils.toArgsList(context.getParams(), context.getJobType());
			String jname = getAppName(context); //codeType +"_"+ context.getSrcname();
			jname = jname.replace('.', '_');
			
				try {	
					logger.info("try to run batch "+jname+" with params "+ ShellUtils.toArgsString(ShellUtils.toArgsList(context.getParams(), context.getJobType(), true)));
					BatchRequest req = null;
					if(null != context.getCodesnap() && !context.getCodesnap().isEmpty()) {
						req = new BatchRequest(context, getSparkConfig(), jname, argvs, context.getSrcname());
						setSparkConf(context, req);
					}else {
						this.retry = false;
						throw new RNBaseException("No jar file found for java/scala run.");
					}
					stmtInfo = executeBatch(req);
					logger.info("kicked spark batch run "+jname+" with batch id as "+stmtInfo.id);
				} catch (Exception e) {
					if(e instanceof APILivyException)
						throw e;
					
					if (retry) {
						logger.warn("Livy batch run failed, retry it.", e.getMessage());
						stmtInfo = executeBatch(new BatchRequest(context.getCodesnap(), new String[] {}, getSparkConfig(), jname, argvs));
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
						if(queuedtimes > maxQueuedCount) handleQueuedSession(context, jname, stmtInfo, true);
						if(queuedtimes % 3 == 0) updateMessage(context, jname, stmtInfo);
					}
				}

				res = getResultFromStatementInfo(stmtInfo, displayAppInfo);
			} catch (Throwable t) {
				res = getResultFromStatementInfo(new StatementInfo(), false);
			} finally {
				this.progress = 100;
			}
			res.setSessionid(stmtInfo.id);
			return res;
	}
	
	@Override
	protected String extractAppId() throws APIException {
		//sc.getConf.getAppId 
		return extractStatementResult(
				exec("sc.applicationId()", null, TechType.spark.name(), false).getMsg());
	}

	@Override
	protected String extractWebUIAddress() throws APIException {
		try {
			return extractStatementResult(
					exec("sc._jsc.sc().uiWebUrl().get()", null, TechType.spark.name(), false).getMsg());
		} catch (Exception e) {
			return extractStatementResult(exec("webui.getClass.getMethod(\"appUIAddress\").invoke(webui)", null,
					TechType.spark.name(), false).getMsg());
		}
	}

	/**
	 * @param result
	 * @return
	 */
	public String extractStatementResult(String result) {
		int pos = -1;
		if ((pos = result.indexOf("=")) >= 0) {
			return result.substring(pos + 1).trim();
		} else {
			throw new RuntimeException(
					"No result can be extracted from '" + result + "', " + "something must be wrong");
		}
	}
}
