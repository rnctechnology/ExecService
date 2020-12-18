package com.rnctech.nrdataservice.resource;

import com.rnctech.nrdataservice.exception.ResourceException;

/**
 * resource pool always get from Rest API / Cloud storage
 * @author zilin 2020.09
 */
public class RemoteResourcePool extends ResourcePool {

	public RemoteResourcePool(ResourceConnector conn) {
		this();
		this.connector = conn;
	}

	public RemoteResourcePool() {
		resourcePoolId = generateId();
	}

	@Override
	public String id() {
		return resourcePoolId;
	}

	@Override
	public Resource get(String name) {
		ResourceSet resources = connector.getAllResources().filterByName(name);
		if (resources.isEmpty()) {
			return null;
		} else {
			return resources.get(0);
		}
	}

	@Override
	public Resource get(String id, String sourcetype, String name) {
		ResourceId resourceId = new ResourceId(resourcePoolId, id, sourcetype, name);
		return getResourceInternal(resourceId);
	}

	private Resource getResourceInternal(ResourceId rid) {
		try {
			ResourceSet resources = connector.getAllResources().filterByNoteId(rid.getId())
					.filterBySourceType(rid.getSourceType()).filterByName(rid.getName());

			if (resources.isEmpty()) {
				Object rsobj = connector.readResource(rid);
				Resource rs = new Resource(this, rid, rsobj);
				return rs;
			} else {
				return resources.get(0);
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return null;
	}

	@Override
	public ResourceSet getAll() {
		ResourceSet all = new ResourceSet();
		all.addAll(connector.getAllResources());
		return all;

	}

	@Override
	public void put(String name, Object obj) throws ResourceException {
		ResourceId resourceId = new ResourceId(resourcePoolId, name);
		putInternal(resourceId, obj);
	}

	@Override
	public void put(String id, String sourcetype, String name, Object obj) throws ResourceException {
		ResourceId resourceId = new ResourceId(resourcePoolId, id, sourcetype, name);
		putInternal(resourceId, obj);
	}
	
	public void putInternal(ResourceId resourceId, Object obj) throws ResourceException {
		if(connector.isWritable) {
			connector.writeResource(resourceId, obj);
		}else {
			throw new ResourceException(" Resource Connector is not writable.");
		}
	}

	@Override
	public void remove(String id, String sourcetype, String name) throws ResourceException {
		ResourceId resourceId = new ResourceId(resourcePoolId, id, sourcetype, name);
		if(connector.isWritable) {
			connector.removeResource(resourceId);
		}else {
			throw new ResourceException(" Resource Connector is not writable.");
		}
	}

}
