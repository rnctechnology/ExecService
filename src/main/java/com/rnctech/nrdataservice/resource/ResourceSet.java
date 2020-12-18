package com.rnctech.nrdataservice.resource;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Set of resources
 * 
 * @author zilin 2020.09
 */
public class ResourceSet extends LinkedList<Resource> {

	private static final long serialVersionUID = -8374688173015469744L;
	private static final Gson gson = new Gson();

	public ResourceSet(Collection<Resource> resources) {
		super(resources);
	}

	public ResourceSet() {
		super();
	}

	public ResourceSet filterByNameRegex(String regex) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (Pattern.matches(regex, r.getResourceId().getName())) {
				result.add(r);
			}
		}
		return result;
	}

	public ResourceSet filterByName(String name) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (r.getResourceId().getName().equals(name)) {
				result.add(r);
			}
		}
		return result;
	}

	public ResourceSet filterByClassnameRegex(String regex) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (r instanceof JResource && Pattern.matches(regex, ((JResource) r).getClassName())) {
				result.add(r);
			}
		}
		return result;
	}

	public ResourceSet filterByClassname(String className) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (r instanceof JResource && ((JResource) r).getClassName().equals(className)) {
				result.add(r);
			}
		}
		return result;
	}

	public ResourceSet filterByNoteId(String noteId) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (equals(r.getResourceId().getId(), noteId)) {
				result.add(r);
			}
		}
		return result;
	}

	public ResourceSet filterBySourceType(String sourcetype) {
		ResourceSet result = new ResourceSet();
		for (Resource r : this) {
			if (equals(r.getResourceId().getSourceType(), sourcetype)) {
				result.add(r);
			}
		}
		return result;
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

	public static ResourceSet fromJson(String json) {
		return gson.fromJson(json, ResourceSet.class);
	}
}
