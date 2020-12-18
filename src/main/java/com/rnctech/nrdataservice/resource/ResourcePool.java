package com.rnctech.nrdataservice.resource;

import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rnctech.nrdataservice.exception.ResourceException;

/**
 * Interface for ResourcePool, memory, local at tmp/, or no cache(remote) 
 * @author zilin 2020.09
 */

public abstract class ResourcePool {
	
	ResourceConnector connector;
	Logger logger = LoggerFactory.getLogger(ResourcePool.class);
	String resourcePoolId;
	boolean isLocal = true;

	public abstract String id();

	public abstract Resource get(String name);

	public boolean isLocal() {
		return isLocal;
	}

	public abstract Resource get(String id, String sourcetype, String name);

	public abstract ResourceSet getAll();

	public static String generateId() {
		return "Pool_" + System.currentTimeMillis() + "_" + new SecureRandom().nextInt();
	}

	public abstract void put(String name, Object object) throws ResourceException;

	public abstract void put(String id, String sourcetype, String name, Object object) throws ResourceException;

	public abstract void remove(String id, String sourcetype, String name) throws ResourceException;
}
