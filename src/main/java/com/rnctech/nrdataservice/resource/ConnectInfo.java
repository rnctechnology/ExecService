package com.rnctech.nrdataservice.resource;

/**
 * connection info for ResourcePoolConnector
 * @contributor zilin
 * 2020.09
 */
public class ConnectInfo {
	
	public ConnectInfo(String basename, String name, String key, String secret, String region,
			boolean encrypted, boolean localProvider) {
		super();
		this.basename = basename;
		this.name = name;
		this.key = key;
		this.secret = secret;
		this.region = region;
		this.encrypted = encrypted;
		this.localProvider = localProvider;
	}
	public ConnectInfo(String baseUrl, String basename, String key, String secret, boolean encrypted) {
		super();
		this.baseUrl = baseUrl;
		this.basename = basename;
		this.key = key;
		this.secret = secret;
		this.encrypted = encrypted;
	}

	String baseUrl;
	String basename;
	String name;
	
	String key;
	String secret;
	String token;
	String region;
	
	boolean encrypted = false;
	boolean localProvider = true; 
	
}
