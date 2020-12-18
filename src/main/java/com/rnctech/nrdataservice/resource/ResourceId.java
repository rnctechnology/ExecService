package com.rnctech.nrdataservice.resource;

import java.security.SecureRandom;
import java.util.UUID;

import com.google.gson.Gson;

/**
 * Identifying resource with id, sourxce type and name
 * @author zilin 2020.09
 */
public class ResourceId {
  
	private static final Gson gson = new Gson();
 public	static enum ResourceType {
		data, text, template, jars, eggs, other
	}
  
  private final String resourcePoolId;
  private final String name;  //
  private final String id;
  private String sourceType = ResourceType.text.name();

  public ResourceId(String resourcePoolId, String name) {
    this.resourcePoolId = resourcePoolId;
    this.id = generateId();
    this.name = name;
  }

  public ResourceId(String resourcePoolId, String id, String sourceType, String name) {
    this.resourcePoolId = resourcePoolId;
    this.id = id;
    this.sourceType = sourceType;
    this.name = name;
  }

  public boolean isTextStyle() {
	return !this.sourceType.equals(ResourceType.jars.name()) && !this.sourceType.equals(ResourceType.eggs.name());
  }
  
  public String getResourcePoolId() {
    return resourcePoolId;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getSourceType() {
    return sourceType;
  }

	public static String generateId() {
		return "Resource_" + System.currentTimeMillis() + "_" + new SecureRandom().nextInt();
	}
	
	public static String generateId(String name) {
		return name.replace('.', '_') + "_" + UUID.nameUUIDFromBytes(name.getBytes()).toString();
	}
  
  @Override
  public int hashCode() {
    return (resourcePoolId + id + sourceType + name).hashCode();
  }
  
  @Override
  public String toString() {
    return (name + id + sourceType + resourcePoolId );
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ResourceId) {
      ResourceId r = (ResourceId) o;
      return equals(r.name, name) && equals(r.resourcePoolId, resourcePoolId) &&
          equals(r.id, id) && equals(r.sourceType, sourceType);
    } else {
      return false;
    }
  }

  private boolean equals(String a, String b) {
    if (a == null && b == null) {
      return true;
    } else if (a != null && b != null) {
      return a.equals(b);
    } else {
      return false;
    }
  }

  public String toJson() {
    return gson.toJson(this);
  }

  public static ResourceId fromJson(String json) {
    return gson.fromJson(json, ResourceId.class);
  }
}
