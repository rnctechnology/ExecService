package com.rnctech.nrdataservice.service.sqlimpl;

import java.util.Properties;

/**
 * rdbms sql executor. will integrate DM engine here
 * @author zilin chen
 * @since 2020.10
 * 
 */

public class JDBCSQLExecutor extends BaseSQLExecutor {

	public JDBCSQLExecutor(Properties properties) {
		super(properties);
	}

}
