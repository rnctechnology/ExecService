package com.rnctech.nrdataservice.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.log4j.Level;
import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.job.JobData;

/**
 * @contributor zilin
 * @since 2020.09
 */

public class RNUtilities implements RNConsts {
	public static final String rnctech_LOGLEVEL_KEY = "com.rnctech.loglevel";
	public static final String rnctech_PACKAGE = "com.rnctech";
	public static final String ROOT_NAME = "root";
	public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	public static SimpleDateFormat sdfzone = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
	public static SimpleDateFormat sdfs = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
	
	public Map<String, Object> toMap(JobData job) {
		ObjectMapper oMapper = new ObjectMapper();
		try {
			Map<String, Object> jconfig = oMapper.convertValue(job, Map.class);
			return jconfig;
		}catch(Exception e) {
			return Collections.EMPTY_MAP;
		}
	}
	
	public static boolean isMRJob(String jobType) {
		return jobType.equalsIgnoreCase("SCRIPT") || jobType.equalsIgnoreCase("CLASS") || jobType.equalsIgnoreCase("ANALYSIS");
	}
	
 	public static String toString(Object o) throws IOException{
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		StringWriter stringEmp = new StringWriter();
		objectMapper.writeValue(stringEmp, o);
		return stringEmp.toString();
 	}
 	
 	public static <T> T getObjectFromJsonString(String jsonData, Class<T> contentClass) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
	    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);				
	    return (T) objectMapper.readValue(jsonData, contentClass);	    
 	}
		
	public static String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	public static String aTos(String[] args){
		StringBuilder argsb = new StringBuilder();
		for(String s : args){
			argsb.append(s).append(" ");
		}
		return argsb.toString();
	}
	
	public static String getRunID(String tenant, long id) {
		return tenant+ "_" + id + "-"+ getUTCTimeStringShort(new Date());
	}
	
	public static String getTimestamp() {
		return "_"+ sdf.format(new Date());
	}
	
	public static void putTransactionId() {
		MDC.put(TXN_ID, RNUtilities.getUUID());
	}
	
	public static String getTransactionId() {
		if(null == MDC.get(TXN_ID)) {
			putTransactionId();
		}
		return MDC.get(TXN_ID);
	}
	
	public static void clearTransactionId() {
		MDC.remove(TXN_ID);
	}
	
	private static boolean isCollectionEmpty(Collection<?> collection) {
		if (collection == null || collection.isEmpty()) {
			return true;
		}
		return false;
	}
	
	public static boolean isObjectEmpty(Object object) {
		if(object == null) return true;
		else if(object instanceof String) {
			if (((String)object).trim().length() == 0) {
				return true;
			}
		} else if(object instanceof Collection) {
			return isCollectionEmpty((Collection<?>)object);
		}
		return false;
	}
	

	public static String getBeanToJsonString(Object beanClass) {
		return new Gson().toJson(beanClass);
	}
	
	public static String getBeanToJsonString(Object... beanClasses) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Object beanClass : beanClasses) {
			stringBuilder.append(getBeanToJsonString(beanClass));
			stringBuilder.append(", ");
		}
		return stringBuilder.toString();
	}
	
	public static String concatenate(List<String> listOfItems, String separator) {
		StringBuilder sb = new StringBuilder();
		Iterator<String> stit = listOfItems.iterator();
		
		while (stit.hasNext()) {
			sb.append(stit.next());
			if(stit.hasNext()) {
				sb.append(separator);
			}
		}
		
		return sb.toString();		
	}
	
	public static String getUTCTimeString(Date d) {
		return getUTCTimeString(d.getTime(), false);
	}
	
	public static String getLocalSysTimeString(Date d) {
		return d.toString();
	}
	
	public static String getUTCTimeString(Long l, boolean s) {
	    Calendar calendar = Calendar.getInstance();	 
	    if(s) {
	    	sdfs.setTimeZone(TimeZone.getTimeZone("GMT"));
		    if(null == l) return sdfs.format(calendar.getTime());
		    return sdfs.format(l);
	    }else {
		    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		    if(null == l) return dateFormat.format(calendar.getTime());
		    return dateFormat.format(l);
	    }
	}
	
	public static String getUTCTimeWithZone(Long l) {
	    Calendar calendar = Calendar.getInstance();	 
	    sdfzone.setTimeZone(TimeZone.getTimeZone("UTC"));
		if(null == l) return sdfzone.format(calendar.getTime());
		return sdfzone.format(l);
	}	
	
	public static String getUTCTimeStringShort(Date d) {
		return getUTCTimeString(d.getTime(), true);
	}
	
	public static String setLogLevel(String loggerName, String loggerLevel) {
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(loggerName);

		if (logger == null) {
			logger = org.apache.log4j.Logger.getLogger(ROOT_NAME);
		}
		Level level;
		if ("debug".equalsIgnoreCase(loggerLevel))
			level = Level.DEBUG;
		else if ("info".equalsIgnoreCase(loggerLevel))
			level = Level.INFO;
		else if ("error".equalsIgnoreCase(loggerLevel))
			level = Level.ERROR;
		else if ("warn".equalsIgnoreCase(loggerLevel))
			level = Level.WARN;
		else if ("trace".equalsIgnoreCase(loggerLevel))
			level = Level.TRACE;
		else {
			level = Level.DEBUG;
		}
		logger.setLevel(level);
		return logger.getLevel().toString();
	}
	
	public static String getString(Map<String, String> dataMap, boolean toArglist) {
		StringBuilder arglist = new StringBuilder(); 
		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			Object o = entry.getValue();
			if (null != o) {
				if(toArglist) {
					arglist.append("--" + entry.getKey().trim().toLowerCase()+" ");
					arglist.append(entry.getValue().toString()+" ");
				} else {
					arglist.append(entry.getKey().trim().toLowerCase()+"=");
					arglist.append(entry.getValue().toString()+"\n");
				}
			}
		}
		return arglist.toString();
	}

	public static String getPreString(String s, int i) {
		if(null == s || s.isEmpty()) return "";
		if(s.trim().length() <= i) return s.trim();
		return s.trim().substring(0, i)+"...";
	}

	public static String getSufString(String s, int i) {
		if(null == s || s.isEmpty()) return "";
		if(i < 10) i = 10;
		int len = s.trim().length();
		if(len <= i) return s.trim();
		return "..."+s.trim().substring(len-i, len);
	}
	
	
}
