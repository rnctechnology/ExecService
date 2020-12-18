package com.rnctech.nrdataservice.resource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.springframework.util.SerializationUtils;

/**
 * ResourcePool for get / put in local system
 *  @author zilin 2020.09
 */
public class PathResourcePool extends ResourcePool {

	private String resourcePoolPath = "/tmp";
	private Map<ResourceId, String> resources = Collections.synchronizedMap(new HashMap<ResourceId, String>());

	public PathResourcePool(ResourceConnector conn, String path) {
		this();
		this.connector = conn;
		this.resourcePoolPath = path;
	}

	public PathResourcePool(ResourceConnector conn) {
		this();
		this.connector = conn;
	}

	public PathResourcePool() {
		resourcePoolId = generateId();
	}

	@Override
	public String id() {
		return resourcePoolId;
	}

	@Override
	public Resource get(String name) {
		ResourceId resourceId = new ResourceId(resourcePoolId, name);
		return getResourceInternal(resourceId);
	}

	@Override
	public Resource get(String id, String sourcetype, String name) {
		ResourceId resourceId = new ResourceId(resourcePoolId, id, sourcetype, name);
		return getResourceInternal(resourceId);
	}

	public Resource getResourceInternal(ResourceId resourceId) {
		if (resources.containsKey(resourceId)) {
			String path = resources.get(resourceId);
			File f = new File(path);
			if (!f.exists() || !f.canRead()) {
				logger.error("local file is not accessible " + f.getAbsolutePath());
				return null;
			}
			try {
				byte[] bytes = FileUtils.readFileToByteArray(f);
				return new Resource(this, resourceId, bytes);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}

		} else {
			Object rsobj = null;
			try {
				if(null != connector) {
					rsobj = connector.readResource(resourceId);
				}else {
					File file = new File(resourcePoolPath+"/"+resourceId.getName());
					rsobj = FileUtils.readFileToByteArray(file);
				}
			} catch (IOException e) {
				rsobj = e.getMessage();
			}
			Resource rs = new Resource(this, resourceId, rsobj);
			put(resourceId, rsobj);
			String path = getTmpPath(resourceId);
			resources.put(resourceId, path);
			return rs;
		}
		return null;
	}

	@Override
	public ResourceSet getAll() {
		Collection<String> paths = resources.values();
		ResourceSet rs = new ResourceSet();

		return rs;
	}

	@Override
	public void put(String name, Object object) {
		ResourceId resourceId = new ResourceId(resourcePoolId, name);
		put(resourceId, object);

	}
	
	//for testing
	public void addResource(ResourceId resourceId, String path) {
		resources.put(resourceId, path);
	}

	private void put(ResourceId resourceId, Object object) {
		String filepath = getTmpPath(resourceId);

		File f = new File(filepath);
			try {
				FileUtils.writeByteArrayToFile(f, SerializationUtils.serialize(object));
				resources.put(resourceId, filepath);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}

	}

	@Override
	public void put(String noteId, String sourcetype, String name, Object object) {
		ResourceId resourceId = new ResourceId(resourcePoolId, noteId, sourcetype, name);
		put(resourceId, object);
	}

	private void remove(ResourceId rid) {
		String filepath = getTmpPath(rid);
		File f = new File(filepath);
		if (f.exists() && f.canWrite()) {
			FileUtils.deleteQuietly(f);
		}
		resources.remove(rid);
	}

	@Override
	public void remove(String id, String sourcetype, String name) {
		remove(new ResourceId(resourcePoolId, id, sourcetype, name));
	}

	public String getTmpPath(ResourceId resourceId) {
		String filepath = resourcePoolPath + File.separator + resourceId.getId();
		if (filepath.length() > 512)
			filepath = resourcePoolPath + File.separator + resourceId.getName();
		return filepath;
	}
}
