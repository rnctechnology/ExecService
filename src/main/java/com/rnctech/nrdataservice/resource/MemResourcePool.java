package com.rnctech.nrdataservice.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ResourcePool - everything persistence in memory for unit test / dev
 * @contributor zilin
 * 2020.10
 */
public class MemResourcePool extends ResourcePool {

  private final Map<ResourceId, Resource> resources = Collections.synchronizedMap(new HashMap<ResourceId, Resource>());

  public MemResourcePool(String id) {
    resourcePoolId = id;
  }
  
  public MemResourcePool(ResourceConnector conn) {
	    this(generateId());
	    this.connector = conn;
	  }
  
  public MemResourcePool() {
	    this(generateId());
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
  
  private Resource getResourceInternal(ResourceId resourceId) {
	  if(resources.containsKey(resourceId) && null != resources.get(resourceId))
		  return resources.get(resourceId);
	  
	  try {
		  Object rsobj = connector.readResource(resourceId);
		  if(null == rsobj) throw new Exception("No resource found for "+resourceId.getName()+" with type "+resourceId.getSourceType());
		  Resource rs = new Resource(this, resourceId, rsobj);
		  resources.put(resourceId, rs);
		  return rs;
	  }catch(Exception e) {
		  logger.error(e.getMessage());
	  }
	  
	  return null;
  }

  @Override
  public ResourceSet getAll() {
    return new ResourceSet(resources.values());
  }

  @Override
  public void put(String name, Object object) {
    ResourceId resourceId = new ResourceId(resourcePoolId, name);

    Resource resource = new Resource(this, resourceId, object);
    resources.put(resourceId, resource);
  }

  @Override
  public void put(String id, String sourcetype, String name, Object object) {
    ResourceId resourceId = new ResourceId(resourcePoolId, id, sourcetype, name);

    Resource resource = new Resource(this, resourceId, object);
    resources.put(resourceId, resource);
  }

  @Override
  public void remove(String id, String sourcetype, String name) {
	  Resource r = resources.remove(new ResourceId(resourcePoolId, id, sourcetype, name));
	  r = null;
  }
}
