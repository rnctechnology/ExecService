package com.rnctech.nrdataservice.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import com.rnctech.nrdataservice.exception.AuthException;

/**
 * @author zilin
 * @2020.03
 */

public class AuthUtil {
		
    public static final String SPRING_SECURITY_HEADER_URL_KEY = "configurl";
    public static final String SPRING_SECURITY_HEADER_TENANT_KEY = "name";
    public static final String DUMMY_USER = "anonymous";
    
	public static RNAuthenticationToken getAuthRequest(HttpServletRequest request, Logger logger)
			throws AuthException {
		String p = request.getRequestURI();

		String url = request.getHeader(SPRING_SECURITY_HEADER_URL_KEY);
		String tenant = request.getHeader(SPRING_SECURITY_HEADER_TENANT_KEY);
		String user = null;
		String pwd = null;
		try {
			final String authorization = request.getHeader("Authorization");
			if (authorization == null || !authorization.toLowerCase().startsWith("basic"))
				throw new AuthException("No header of Authorization found!");
			String base64Credentials = authorization.substring("Basic".length()).trim();
			byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
			String credentials = new String(credDecoded, StandardCharsets.UTF_8);
			int lidx = credentials.lastIndexOf(":");
			if(-1 == lidx) throw new AuthException("No enough auth info found!");
			pwd = credentials.substring(lidx+1).trim();
			user = credentials.substring(0, lidx).trim();
			if (null == url) {				
				final String[] vv = splitHeader(user);
				url = vv[0].trim();
				tenant = vv[1].trim();
				user = vv[2].trim();
			} else {
				logger.info("get from header url=" + url + " and tenant=" + tenant);								
			}
		} catch (Exception e) {
			throw new AuthException(e.getMessage());
		}
		if (null == user || null == pwd || null == url || null == tenant)
			throw new AuthException("No enough auth info found!");
		return new RNAuthenticationToken(user, pwd, url, tenant);
	}
    
	public static class RNAuthenticationToken extends UsernamePasswordAuthenticationToken {
		private static final long serialVersionUID = -8668157011768133828L;
		Object url;
		Object uname;

		public Object getUrl() {
			return url;
		}

		public Object getUname() {
			return uname;
		}

		public RNAuthenticationToken(Object principal, Object credentials) {
			super(principal, credentials);
		}

		public RNAuthenticationToken(Object principal, Object credentials, Object url, Object tenant) {
			super(principal, credentials);
			this.url = url;
			this.uname = tenant;
		}

		public RNAuthenticationToken(Object principal, Object credentials,
				Collection<? extends GrantedAuthority> authorities) {
			super(principal, credentials, authorities);
		}

	}
	
	public static String[] splitHeader(String s) throws Exception {
		final String[] v = s.split("!");
		if (v.length < 3)
			throw new Exception("no enough auth info found!");
		return v;
	}
	
}
