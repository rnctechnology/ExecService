package com.rnctech.nrdataservice.resource;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

/**
 * resource object
 * @author zilin 2020.09
 */

public class Resource {

	private static final Gson gson = new Gson();
	Logger logger = LoggerFactory.getLogger(Resource.class);
	
	public static enum SourceType {
		sql, ddl, dml, query, java, scala, python, py, sh, csh, lib, egg, jar, json, txt, zip
	}

	protected final transient Object r;
	protected final ResourcePool pool;
	protected final boolean serializable;
	protected final ResourceId resourceId;
	protected ResourceConnector resourceConnector;

	Resource(ResourcePool pool, ResourceId resourceId, Object r) {
		this.r = r;
		this.pool = pool;
		this.resourceId = resourceId;
		this.serializable = r instanceof Serializable;
	}

	Resource(ResourcePool pool, ResourceId resourceId, boolean serializable) {
		this.r = null;
		this.pool = pool;
		this.resourceId = resourceId;
		this.serializable = serializable;
	}
	
	public String getAString() {
		if(isTextResource() && null != r)
			return (String)r;
		return "";
	}
	
	public boolean isTextResource() {
		String st = this.resourceId.getSourceType();
		return !st.equalsIgnoreCase(SourceType.lib.name()) &&  !st.equalsIgnoreCase(SourceType.jar.name()) && 
				!st.equalsIgnoreCase(SourceType.egg.name());
	}

	public ResourceId getResourceId() {
		return resourceId;
	}

	public Object get() {
		if (isLocal() || isSerializable()) {
			return r;
		} else {
			return pool.get(resourceId.getId(), resourceId.getSourceType(), resourceId.getName());
		}
	}

	public boolean isSerializable() {
		return serializable;
	}

	public boolean isRemote() {
		return !isLocal();
	}

	public boolean isLocal() {
		return null == resourceConnector;
	}

	public ResourceConnector getResourceConnector() {
		return resourceConnector;
	}

	public void setResourceConnector(ResourceConnector resourceConnector) {
		this.resourceConnector = resourceConnector;
	}

	public Resource read(ResourcePool pool) {
		ResourceId resourceId = getResourceId();
		Object o = resourceConnector.readResource(resourceId);
		return new Resource(pool, resourceId, o);
	}
	
	public Resource read(ResourcePool pool, String methodName, String[] params) {
		ResourceId resourceId = getResourceId();
		Object o = resourceConnector.readResource(resourceId, methodName, params);
		return new Resource(pool, resourceId, o);
	}

	public String toJson() {
		return gson.toJson(this);
	}

	public static Resource fromJson(String json) {
		return gson.fromJson(json, Resource.class);
	}
}
