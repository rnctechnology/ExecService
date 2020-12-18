package com.rnctech.nrdataservice.service.pyimpl;

import java.util.Map;
import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.ResourceException;
import com.rnctech.nrdataservice.resource.Resource;
import com.rnctech.nrdataservice.resource.ResourcePool;
import com.rnctech.nrdataservice.resource.ResourceSet;


/**
 * python Context for Python environment
 * @author zilin chen
 * @since 2020.10
 */

public class PythonContext extends RNContext {

	protected RNContext rnCtx;
	protected int maxResult;


	public PythonContext(int maxResult) {
		super(maxResult);
	}

	public Map<String, String> getExecutorClassMap() {
		return null;
	}

	public int getMaxResult() {
		return this.maxResult;
	}

	public RNContext getRNContext() {
		return rnCtx;
	}

	public void setRNContext(RNContext interpreterContext) {
		this.rnCtx = interpreterContext;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}

	public void put(String name, Object value) throws ResourceException {
		ResourcePool resourcePool = rnCtx.getResourcePool();
		resourcePool.put(name, value);
	}

	public Object get(String name) {
		ResourcePool resourcePool = rnCtx.getResourcePool();
		Resource resource = resourcePool.get(name);
		if (resource != null) {
			return resource.get();
		} else {
			return null;
		}
	}

	public void remove(String id, String sourcetype, String name) throws ResourceException {
		ResourcePool resourcePool = rnCtx.getResourcePool();
		resourcePool.remove(id, sourcetype, name);
	}

	public boolean containsKey(String name) {
		ResourcePool resourcePool = rnCtx.getResourcePool();
		Resource resource = resourcePool.get(name);
		return resource != null;
	}

	public ResourceSet getAll() {
		ResourcePool resourcePool = rnCtx.getResourcePool();
		return resourcePool.getAll();
	}

}
