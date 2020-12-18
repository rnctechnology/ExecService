package com.rnctech.nrdataservice.config;


import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.rnctech.nrdataservice.RNApplication;
import com.rnctech.nrdataservice.RNConsts.PROFILE_ENV;
import com.rnctech.nrdataservice.exception.AuthException;
import com.rnctech.nrdataservice.service.AuthService;
import com.rnctech.nrdataservice.utils.AuthUtil;
import com.rnctech.nrdataservice.utils.AuthUtil.RNAuthenticationToken;

/**
 * @contributor zilin
 * @since 2020.03
 */

@Controller
public class RNReqInterceptor extends HandlerInterceptorAdapter {


	@Value("${rnctech.profile:QA}")
	private String profile;
	
	private String pathPrefix ="/exec/api/v1";
	
	@Autowired
	AuthService authService;

	private static final Logger logger = LoggerFactory.getLogger(RNReqInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		logger.debug(request.getContextPath()+": "+request.getRequestURI());
		
		if(RNApplication.isPyEngine()) {
			logger.info("PyEngine deployment "+RNApplication.isPyEngine());
			return true; //no auth need for PyEngine
		}
		
		X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		if (certs != null && certs.length > 0) {
			try {
				certs[0].checkValidity();
			} catch (CertificateExpiredException | CertificateNotYetValidException ex) {
				logger.error(ex.getMessage());
				return false;
			}
		} else 
		if (request.getRequestURI().startsWith(pathPrefix+"/jobs")) {
			String[] teantinfo = getHeaderInfo(request, logger);
			if(null != teantinfo) return true;
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authenticate failed!");
			return false;
		} else if (request.getRequestURI().startsWith(pathPrefix+"/admin") || request.getRequestURI().startsWith(pathPrefix+"/fs/upload")) {
			// not support now except Dev env
			if(profile.equalsIgnoreCase(PROFILE_ENV.DEV.name())) {
				String[] teantinfo = getHeaderInfo(request, logger);
				if(null != teantinfo) {
					String url = teantinfo[0];
					String loginName = teantinfo[1];
					String tenantName = teantinfo[2];
					logger.info("Authenticated "+loginName+" with "+url+" at env "+ profile+" "+tenantName);
					return true;
				}else {
					logger.info("Authenticated failed at env "+ profile);
				}
			}
			logger.info("Not support for env "+ profile);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, request.getRequestURI()+" not support!");
			return false;
		}

		return true;
	}

	public String[] getHeaderInfo(HttpServletRequest request, Logger logger) {
		String[] headers = null;
		try {
			SecurityContext context = SecurityContextHolder.getContext();
			Authentication authentication = context.getAuthentication();
			if (authentication != null && authentication instanceof RNAuthenticationToken) {
				headers = new String[3];
				RNAuthenticationToken token = (RNAuthenticationToken) authentication;
				headers[0] = token.getUrl().toString();
				headers[1] = token.getName();
				headers[2] = token.getUname().toString();
				logger.debug("Login session detected as" + headers);
			}else {
				RNAuthenticationToken rntoken = AuthUtil.getAuthRequest(request, logger);
	        	String url = rntoken.getUrl().toString();
	        	String t = rntoken.getName().toString();
	        	boolean yn = authService.login(url, rntoken.getName(), rntoken.getCredentials().toString(), t);
	        	if(yn) {
	        		headers = new String[3];
	        		context.setAuthentication(authentication);
	        		logger.info("Authenticate successed for " + t);
	        		headers[0] = url;
					headers[1] = rntoken.getName();
					headers[2] = t;
	        	}else {
	        		throw new AuthException("Authenticated failed for " +t+" @ "+url);
	        	}	        	
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
		return headers;
	}

}
