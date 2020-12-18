package com.rnctech.nrdataservice.service.sqlimpl;

import java.util.Properties;
import com.rnctech.nrdataservice.exception.RNBaseException;

/**
 * hive sql executor.
 * @author zilin chen
 * @since 2020.10
 */

public class HiveSQLExecutor extends BaseSQLExecutor {
	private static String DEFAULT_HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
	private static String DEFAULT_HIVE_URL = "jdbc:hive2://127.0.0.1:10000/default";
	private static String DEFAULT_USER = "hive";
	private static String DEFAULT_JAR_CMD = "export CLASSPATH=$CLASSPATH:$HADOOP_HOME/lib/hive-jdbc.jar";
	
	@Override
	public void initProp(Properties property) {
		super.initProp(property);
		if(null == this.driverClass)
			driverClass = DEFAULT_HIVE_DRIVER;
		if(null == this.jdbcurl)
			jdbcurl = DEFAULT_HIVE_URL;
		if(null == this.user)
			user = DEFAULT_USER;
	}
	
	public HiveSQLExecutor(Properties properties) {
		super(properties);
	}

}
