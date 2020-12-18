package com.rnctech.nrdataservice.service.livyimpl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.service.RNResult;

import java.util.Properties;


/**
 * Livy Executor for shared SparkContext
 * @author zilin chen
 * @since 2020.10 
 */

@Component
public class LivyExecutor extends BaseLivyExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(LivyExecutor.class);

  public LivyExecutor(Properties property) {
    super(property);
  }

  public RNResult exec(String st, String codeType, RNContext context) {
    if (StringUtils.isEmpty(st)) {
      return new RNResult(RNResult.Code.SUCCESS, "No code executed! "+st);
    }

    try {
      return exec(st, context, codeType, this.displayAppInfo);
    } catch (APIException e) {
      LOGGER.error("Fail to interpret:" + st, e);
      return new RNResult(RNResult.Code.ERROR,
          ExceptionUtils.getRootCauseMessage(e));
    }
  }

  @Override
  public String getSessionKind() {
    return "shared";
  }

  @Override
  protected String extractAppId() throws APIException {
    return null;
  }

  @Override
  protected String extractWebUIAddress() throws APIException {
    return null;
  }

  public static void main(String[] args) {
    ExecuteRequest request = new ExecuteRequest("1+1", TechType.pyspark.name());
    System.out.println(request.toJson());
  }


}
