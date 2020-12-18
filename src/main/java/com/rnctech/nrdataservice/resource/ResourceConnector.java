package com.rnctech.nrdataservice.resource;

import com.rnctech.nrdataservice.exception.ResourceException;

/**
 * Connect resource pools running in remote process
 * @author zilin 2020.09
 */

public abstract class ResourceConnector {

	public boolean isWritable = true; 
	
	public ResourceConnector(ConnectInfo connction) {
		this.connction = connction;
	}

	public ResourceConnector() {
	}

	ConnectInfo connction;

	abstract ResourceSet getAllResources();

	abstract public void init();

	abstract public void close();

	public abstract Object readResource(ResourceId id);

	public abstract Object readResource(ResourceId id, String methodName, Object[] params);

	public abstract void writeResource(ResourceId resourceId, Object obj) throws ResourceException;

	public abstract void removeResource(ResourceId resourceId) throws ResourceException;
	
	

}
