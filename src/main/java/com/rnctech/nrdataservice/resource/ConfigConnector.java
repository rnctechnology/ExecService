package com.rnctech.nrdataservice.resource;

import com.rnctech.nrdataservice.utils.ConfigClient;


/**
 * connector for Configure Service
 * @author zilin 2020.09
 */

public class ConfigConnector extends RestResConnector {
	
	ConfigClient cclient;

	public ConfigConnector(ConnectInfo connction) {
		super(connction);
	}

	public ConfigConnector(String httpurl, String username, String password) {
		super(httpurl, username, password);
	}

	public ConfigConnector(String httpurl, String urlpath, String username, String password, boolean encrypted) {
		super(httpurl, urlpath, username, password, encrypted);
	}
	
	@Override
	public void init() {
		super.init();
		if(connction.baseUrl.startsWith("https")){
			cclient = new ConfigClient(connction.baseUrl, connction.key, connction.secret);
		}
		cclient = new ConfigClient(connction.baseUrl,  connction.key, connction.secret);
	}
	
	
	@Override
	public Object readResource(ResourceId rid) {
		String r = cclient.getTemplateFile(rid.getName());
		return r;
	}

}
