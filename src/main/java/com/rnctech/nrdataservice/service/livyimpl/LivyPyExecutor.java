package com.rnctech.nrdataservice.service.livyimpl;

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.exception.APIException;

/**
 * Livy spark executor for PySpark
 * 
 * @author zilin chen
 * @since 2020.10
 */

@Component
public class LivyPyExecutor extends BaseLivyExecutor {

	public LivyPyExecutor(Properties property) {
		super(property);
	}

	@Override
	protected String extractAppId() throws APIException {
		// sc.getConf.getAppId
		return extractStatementResult(
				exec("sc.getConf.getAppId", null, TechType.pyspark.name(), false).getMsg());
	}

	@Override
	/*
	 * see SPARK-17437
	 */
	protected String extractWebUIAddress() throws APIException {
		try {
			return extractStatementResult(
					exec("sc.uiWebUrl", null, TechType.pyspark.name(), false).getMsg());
		} catch (Exception e) {
			return extractStatementResult(
					exec("sc._jsc.sc().uiWebUrl().get()", null, TechType.spark.name(), false).getMsg());
		}
	}

	/**
	 * @param result
	 * @return
	 */
	private String extractStatementResult(String result) {
		int pos = -1;
		if ((pos = result.indexOf("'")) >= 0) {
			return result.substring(pos + 1, result.length() - 1).trim();
		} else {
			throw new RuntimeException(
					"No result can be extracted from '" + result + "', " + "something must be wrong");
		}
	}

	@Override
	public String getSessionKind() {
		return "pyspark";
	}
}
