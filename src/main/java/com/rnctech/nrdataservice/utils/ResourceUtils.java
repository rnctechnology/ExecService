package com.rnctech.nrdataservice.utils;

import java.io.File;

import com.rnctech.nrdataservice.resource.AWSResConnector;
import com.rnctech.nrdataservice.resource.ConnectInfo;
import com.rnctech.nrdataservice.resource.HTTPResConnector;
import com.rnctech.nrdataservice.resource.MemResourcePool;
import com.rnctech.nrdataservice.resource.PathResourcePool;
import com.rnctech.nrdataservice.resource.Resource;
import com.rnctech.nrdataservice.resource.ResourceConnector;
import com.rnctech.nrdataservice.resource.ResourceId;
import com.rnctech.nrdataservice.resource.ResourcePool;
import com.rnctech.nrdataservice.resource.ResourceId.ResourceType;

public class ResourceUtils {
	
	public static String getTemplate(String path) throws Exception {
		return getResourceAsString(path);
	}

	public static String getResourceAsString(String path) throws Exception {
		ResourceConnector resconnector = null;
		ResourcePool rpool = null;
		int idx = path.lastIndexOf("/");
		if(-1 == idx) throw new Exception("Invalid path as "+path);
		String basepath = path.substring(0, idx); 
		String fileName = path.substring(idx+1);
		String bpath = basepath.substring(basepath.indexOf("://")+3);
		if (path.startsWith("http:") || path.startsWith("https:")) {			
			ConnectInfo connection = new ConnectInfo(basepath, "/", null, null, false);
			resconnector = new HTTPResConnector(connection);
			resconnector.init();
			rpool = new MemResourcePool(resconnector);
			Resource o = rpool.get(fileName, ResourceType.template.name(), fileName);
			return toString(o);	
		} else if (path.startsWith("file:")) {
			rpool = new PathResourcePool(resconnector, bpath);
			Resource o = rpool.get(ResourceId.generateId(fileName), ResourceType.template.name(), fileName);
			return toString(o);			
		} else if (path.startsWith("s3a:")) {
			String bucketname = "rncbucket";
			String basefolder = "dataanalysis/";
			int index = bpath.indexOf("/");
			bucketname = bpath.substring(0, index);
			basefolder = bpath.substring(index+1);
			ConnectInfo connection = new ConnectInfo(bucketname, basefolder, null, null, null, false, false);
			//ConnectInfo connection = new ConnectInfo(bucketname, basefolder, key, secret, null, false, false);			
			resconnector = new AWSResConnector(connection);
			resconnector.init();
			rpool = new MemResourcePool(resconnector);
			Resource o = rpool.get(fileName, ResourceType.template.name(), fileName);
			return toString(o);
		} 
		throw new Exception("Not support protocol as " + basepath);		
	}
	
	
	  protected static String toString(Object o) {
		  if(o instanceof Resource) {
			  return toString(((Resource)o).get());
		  }	  
		  	if(o instanceof byte[]) {
		  		return new String((byte[])o);
		  	}
			if(o instanceof String) {
				return (String)o;
			}else if(o instanceof File) {
				File f = (File)o;
				return f.getAbsolutePath();
			}else {
				return "Get resource object "+o.toString();
			}
	  }
}
