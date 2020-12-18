package com.rnctech.nrdataservice.test.jvm;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.service.RNResult.ResultMessage;
import com.rnctech.nrdataservice.service.javaimpl.JavaExecutor;
import com.rnctech.nrdataservice.test.JobServiceTest;
import com.rnctech.nrdataservice.utils.RNUtilities;
import com.rnctech.nrdataservice.utils.RNCOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @author zilin
 * @2020.04
 */

@Ignore
public class BaseJvmTest extends JobServiceTest {

  @Autowired
  protected JavaExecutor jvmexecutor;

  @Before
  public void setUp() throws RNBaseException {
    Properties properties = new Properties();
    properties.setProperty(JavaExecutor.TIMEOUT_PROPERTY, "300000");
    properties.setProperty(JavaExecutor.WORKING_DIRECTORY_PROPERTY, "/tmp/uploads");
    jvmexecutor.setProperties(properties);
    jvmexecutor.open();
  }

  @After
  public void tearDown() throws RNBaseException {
	  jvmexecutor.close();
  }

  protected RNContext getRNContext() {
    return RNContext.builder()
        .setOutput(new RNCOutputStream(null));
  }
  
	@Test
	public void testJValid() throws RNBaseException {
		boolean yn = jvmexecutor.validate(null);
		assertTrue(yn);
	}
	
	@Test
	public void testJVM() throws RNBaseException, InterruptedException, IOException {
		RNContext context = getRNContext();
		context.setCtxid("JVMTest_"+RNUtilities.getTimestamp());
		context.setSrcname("");
		context.setCodesnap("");
		RNResult result = jvmexecutor.exec("", context);
		assertEquals(RNResult.Code.SUCCESS, result.code());
		Thread.sleep(300);
		List<ResultMessage> retmsgs = context.out.toResultMessage();
		assertTrue(retmsgs.size() > 0);
	}
  
  
  protected boolean isSysWin() {
	 return SystemUtils.IS_OS_WINDOWS;
  }
}
