package com.rnctech.nrdataservice.test;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author zilin
 * @2020.09
 */

public class CommonTest extends JobServiceTest {

	@Autowired
	private DataSource dataSource;

	@Test
	public void getDBTest() {
		try {
			Assert.assertTrue(null != dataSource);
			Connection cnn = dataSource.getConnection();
			Assert.assertTrue(null != cnn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
