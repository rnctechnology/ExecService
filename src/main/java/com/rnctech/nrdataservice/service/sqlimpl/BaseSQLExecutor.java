package com.rnctech.nrdataservice.service.sqlimpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNJobExecutor;
import com.rnctech.nrdataservice.service.RNResult;

/**
 * Base class for sql executor.
 * @author zilin chen
 * @since 2020.10
 */

public abstract class BaseSQLExecutor extends RNJobExecutor {
	protected String jdbcurl;
	protected final static String DEFAULT_CONN_TIMEOUT = "120000"; 
	protected int connectionTimeout;
	protected String user;
	protected String password;
	protected String driverClass;
	protected Connection connection;

	public BaseSQLExecutor(Properties properties) {
		super(properties);
	}

	@Override
	public void open() throws RNBaseException {
		try {
			Class.forName(driverClass);
			connection = DriverManager.getConnection(this.jdbcurl, this.user, this.password);
		} catch (Exception e) {
			throw new RNBaseException(e);
		}
	}

	@Override
	public void close() throws RNBaseException {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new RNBaseException(e);
		}
	}
	
	@Override
	public void initProp(Properties property) {
		this.jdbcurl = property.getProperty("jdbc.url");
		this.user = property.getProperty("jdbc.user");
		this.password = property.getProperty("jdbc.password","");
		this.driverClass =  property.getProperty("jdbc.driverClass");
		this.connectionTimeout = Integer.parseInt(property.getProperty("jdbc.connection.timeout", DEFAULT_CONN_TIMEOUT));
	}

	@Override
	public RNResult exec(String st, RNContext context) throws RNBaseException {
		return null;
	}

	@Override
	public void cancel(RNContext context) throws RNBaseException {

	}

	@Override
	public int getProgress(RNContext context) throws RNBaseException {
		return 0;
	}

}
