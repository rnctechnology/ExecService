package com.rnctech.nrdataservice.service.livyimpl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.Code;
import com.rnctech.nrdataservice.service.RNResult.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Livy SparkSQL executor
 * 
 * @author zilin chen
 * @since 2020.10
 */

@Component
public class LivySparkSQLExecutor extends BaseLivyExecutor {
	public static final String LIVY_SPARK_SQL_FIELD_TRUNCATE = "livy.spark.sql.field.truncate";

	public static final String LIVY_SPARK_SQL_MAX_RESULT = "livy.spark.sql.maxResult";

	private LivyJavaExecutor sparkJavaExecutor;
	private int maxResult = 1000;
	private boolean truncate = true;

	public LivySparkSQLExecutor(Properties property) {
		super(property);
	}

	public void initProp(Properties property) {
		super.initProp(property);
		this.maxResult = Integer.parseInt(property.getProperty(LIVY_SPARK_SQL_MAX_RESULT, "100"));
		if (property.getProperty(LIVY_SPARK_SQL_FIELD_TRUNCATE) != null) {
			this.truncate = Boolean.parseBoolean(property.getProperty(LIVY_SPARK_SQL_FIELD_TRUNCATE));
		}

	}

	@Override
	public String getSessionKind() {
		return "spark";
	}

	@Override
	public void open() throws RNBaseException {
		try {
			this.sparkJavaExecutor = lookupBean("LivyJavaExecutor", LivyJavaExecutor.class);
		} catch (Exception e) {
			this.sparkJavaExecutor = new LivyJavaExecutor(this.getProperties());
		}
		this.sparkJavaExecutor.setProperties(getProperties());
		this.sparkJavaExecutor.open();
	}

	@Override
	public RNResult exec(String sqlqry, RNContext context) {
		try {
			if (StringUtils.isEmpty(sqlqry)) {
				return new RNResult(RNResult.Code.SUCCESS, "");
			}

			String sqlQuery = "spark.sql(\"\"\"" + sqlqry + "\"\"\").show(" + maxResult + ", " + truncate + ")";

			RNResult result = sparkJavaExecutor.exec(sqlQuery, context);
			if (result.code() == RNResult.Code.SUCCESS) {
				RNResult result2 = new RNResult(Code.SUCCESS, "");
				if (result.getRetType() == RNResult.Type.TEXT) {
					result2.setRetType(Type.TABLE);
					result2.setMsg(StringUtils.join(result.getMsg(), "\n"));
				} else {
					result2.setRetType(Type.TEXT);
					result2.add(result.getRetType(), result.getMsg());
				}

				return result2;
			} else {
				return result;
			}
		} catch (Exception e) {
			logger.error("Exception in LivySparkSQLExecutor while execute ", e);
			return new RNResult(RNResult.Code.ERROR, ExceptionUtils.getRootCauseMessage(e));
		}
	}

	public List<String> parseSQLOutput(String output) {
		List<String> rows = new ArrayList<>();
		String firstLine = output.split("\n", 2)[0];
		String[] tokens = StringUtils.split(firstLine, "\\+");
		List<Pair> pairs = new ArrayList<>();
		int start = 0;
		int end = 0;
		for (String token : tokens) {
			start = end + 1;
			end = start + token.length();
			pairs.add(new Pair(start, end));
		}

		int lineStart = 0;
		int lineEnd = firstLine.length();
		while (lineEnd < output.length()) {
			String line = output.substring(lineStart, lineEnd);
			if (line.matches("(?s)^\\|.*\\|$")) {
				List<String> cells = new ArrayList<>();
				for (Pair pair : pairs) {
					cells.add(StringEscapeUtils.escapeEcmaScript(line.substring(pair.start, pair.end)).trim());
				}
				rows.add(StringUtils.join(cells, "\t"));
			}
			lineStart += firstLine.length() + 1;
			lineEnd = lineStart + firstLine.length();
		}
		return rows;
	}

	private static class Pair {
		private int start;
		private int end;

		Pair(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}
	
	@Override
	public void cancel(RNContext context) throws RNBaseException {
		if (this.sparkJavaExecutor != null) {
			sparkJavaExecutor.cancel(context);
		}
	}

	@Override
	public void close() {
		if (this.sparkJavaExecutor != null) {
			this.sparkJavaExecutor.close();
		}
	}

	@Override
	public int getProgress(RNContext context) throws RNBaseException {
		if (this.sparkJavaExecutor != null) {
			return this.sparkJavaExecutor.getProgress(context);
		} else {
			return 0;
		}
	}

	@Override
	protected String extractAppId() throws APIException {
		return sparkJavaExecutor.extractAppId();
	}

	@Override
	protected String extractWebUIAddress() throws APIException {
		return sparkJavaExecutor.extractWebUIAddress();
	}
}
