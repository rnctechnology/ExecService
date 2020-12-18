package com.rnctech.nrdataservice.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rnctech.nrdataservice.exception.ResourceException;

/**
 * @contributor zilin
 * 2020.10
 */

public class RestResConnector extends ResourceConnector {

	Logger logger = LoggerFactory.getLogger(RestResConnector.class);

	public RestResConnector(ConnectInfo connction) {
		super(connction);
	}

	public RestResConnector(String httpurl, String username, String password) {
		this(httpurl, "", username, password, false);
	}

	public RestResConnector(String httpurl, String urlpath, String username, String password, boolean encrypted) {
		super();
		this.connction = new ConnectInfo(httpurl, urlpath, username, password, encrypted);
	}

	@Override
	ResourceSet getAllResources() {
		return null;
	}

	@Override
	public Object readResource(ResourceId id) {
		return null;
	}

	@Override
	public Object readResource(ResourceId id, String methodName, Object[] params) {
		return null;
	}

	@Override
	public void init() {
		logger.info("Initializing....");
	}

	@Override
	public void close() {
		logger.info("Closing connection of " + RestResConnector.class.getName());
	}

	@Override
	public void writeResource(ResourceId resourceId, Object obj) throws ResourceException {
		logger.info("Try to store resource " + resourceId.getId() + "[" + resourceId.getName() + "] by "
				+ CloudResConnector.class.getName());
	}

	@Override
	public void removeResource(ResourceId resourceId) throws ResourceException {
		logger.info("Try to remove resource " + resourceId.getId() + "[" + resourceId.getName() + "] by "
				+ CloudResConnector.class.getName());
	}

}
