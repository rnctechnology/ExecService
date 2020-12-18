package com.rnctech.nrdataservice.service.livyimpl;

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.service.RNResult;

/**
 * Livy R executor for SparkR. Not support yet @2020.10
 * @author zilin chen
 */

@Component
public class LivyRExecutor extends BaseLivyExecutor {

	public LivyRExecutor(Properties property) {
		super(property);
	}

	@Override
	public String getSessionKind() {
		return "sparkr";
	}

	@Override
	public RNResult exec(String st, RNContext context) throws APIException {
		throw new APIException("No implementation yet");
	}

	@Override
	protected String extractWebUIAddress() throws APIException {
		throw new APIException("No implementation yet");
	}

	@Override
	protected String extractAppId() throws APIException {
		// TODO Auto-generated method stub
		throw new APIException("No implementation yet");
	}
}
