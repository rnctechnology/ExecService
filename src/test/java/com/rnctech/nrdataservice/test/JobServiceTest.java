package com.rnctech.nrdataservice.test;

import org.apache.commons.lang3.SystemUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
//import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
//import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import com.rnctech.nrdataservice.exception.RNBaseException;

/**
 * @contributor zilin
 * 2020.09
 */

@RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(initializers =
// YamlFileApplicationContextInitializer.class, classes = {TestAppConfig.class})
@ContextConfiguration(initializers = { ConfigFileApplicationContextInitializer.class }, classes = {
		TestAppConfig.class, TestWebConfig.class })
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class,
		TransactionalTestExecutionListener.class, JobServiceTest.class })
@WebAppConfiguration
@ActiveProfiles("test")

@Ignore
public class JobServiceTest extends AbstractTestExecutionListener {

	protected static final Logger logger = LoggerFactory.getLogger(JobServiceTest.class);

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
	}

	@BeforeClass
	public static void setUpRN() throws RNBaseException {
		System.setProperty("jasypt.encryptor.password", "rnctech123!");  
	}


	protected boolean isSysWin() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	protected boolean isLinux() {
		return SystemUtils.IS_OS_LINUX;
	}

}
