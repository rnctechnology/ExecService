package com.rnctech.nrdataservice.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;    
import javax.net.*;
import javax.net.ssl.*;
import javax.naming.*;
import javax.naming.directory.*;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.log4j.Logger;
import org.jasypt.intf.service.JasyptStatelessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rnctech.nrdataservice.exception.AuthException;
import com.rnctech.nrdataservice.utils.ConfigClient;


/**
 * @author Zilin Chen
 * @since 2020.02
 */

@Service
public class AuthService {
	
	public static final String DEFAULT_DOMAIN = "rnctech.com";
	public static final String KTAB_LOCATION = "shell.keytab.location";
	public static Logger logger = Logger.getLogger(AuthService.class);
	public static String DFL_ALGO = "PBEWithMD5AndDES";
	
    @Value("${security.user.password}")
    private String jspwd;
			
    public boolean login(String url, String user, String pwd, String tenant){
    	if(url.toLowerCase().startsWith("ldap"))
    		return adminlogin(url, user, pwd, tenant);
    	else
    		return tenantlogin(url, user, pwd, tenant);
    }
	
    private LdapContextSource contextSource(String ldapurl, String principal, String password) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapurl);
       // contextSource.setBase(env.getRequiredProperty("ldap.partitionSuffix"));
        contextSource.setUserDn(principal);
        contextSource.setPassword(password);
        return contextSource;
    }
    
    private LdapTemplate ldapTemplate(String ldapurl, String principal, String password) {
        return new LdapTemplate(contextSource(ldapurl, principal, password));
    }
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    
    public String encryptString(String s) {
    	return encryptString(s,jspwd);
    }
    
    public String encryptString(String s, String seed) {
    	final JasyptStatelessService jsservice = new JasyptStatelessService();
    	String ens = jsservice.encrypt(s, seed, null, null, DFL_ALGO, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    	return ens;
    }
    
    public String decryptString(String s) {
    	return decryptString(s, jspwd);
    }
    
    public String decryptString(String s, String seed) {
    	final JasyptStatelessService jsservice = new JasyptStatelessService();
    	String des = jsservice.decrypt(s, seed, null, null, DFL_ALGO, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    	return des;
    }

    private String digestSHA(final String password) {
        String base64;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(password.getBytes());
            base64 = Base64.getEncoder()
                .encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return "{SHA}" + base64;
    }    
    
    public boolean tenantlogin(String url, String user, String pwd, String tenant) {
    	try {
			boolean isSSL = url.toUpperCase().startsWith("HTTPS://");
			return ConfigClient.authWithAccount(url, user, pwd, isSSL, tenant);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
    	return false;
    }
    
    public boolean adminlogin(String url, String user, String pwd, String tenant){
    	try {
			return ldaplogin(url, user, null, pwd, tenant);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
    	return false;
    }
    
	public static void kerberosConfiguration(String kdcurl, String principalName, String domainName, String cmd, String ktabloc) throws AuthException {		    
		    CommandLine cmdLine = CommandLine.parse(cmd);
		    cmdLine.addArgument("-c", false);
		    String kinitCommand = String.format("kinit -k -t %s %s", ((null == ktabloc)?"/tmp":ktabloc), principalName);
		    cmdLine.addArgument(kinitCommand, false);
		    DefaultExecutor executor = new DefaultExecutor();
		    try {
		      executor.execute(cmdLine);
		    } catch (Exception e) {
		    	logger.error("Unable to run kinit for user " + kinitCommand, e);
		      throw new AuthException(e);
		    }
		  }

	public boolean ldaplogin(String ldapurl, String principalName, String domainName, String password, String tenant)
			throws Exception {
		boolean isauthed = false;
		if (domainName == null || "".equals(domainName)) {
			if(-1 != ldapurl.indexOf("/")) {
				int delim = ldapurl.lastIndexOf('/');
				domainName = ldapurl.substring(delim + 1);
			}
		}
		
		if (domainName == null || "".equals(domainName)) {
			domainName = DEFAULT_DOMAIN;
		}
		Properties props = new Properties();
		props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		props.put(Context.PROVIDER_URL, ldapurl);
		props.put(Context.SECURITY_PRINCIPAL, principalName);
		props.put(Context.SECURITY_CREDENTIALS, password);
		if (ldapurl.toUpperCase().startsWith("LDAPS://")) {
			props.put(Context.SECURITY_PROTOCOL, "ssl");
			props.put(Context.SECURITY_AUTHENTICATION, "simple");
			props.put("java.naming.ldap.factory.socket", "com.rnctech.nrdataservice.service.TrustAllSSLSocketFactory");
		}

		InitialDirContext context = new InitialDirContext(props);
		try {

			Attributes matchAttrs = new BasicAttributes(true);
			matchAttrs.put(new BasicAttribute("uid"));
			NamingEnumeration<?> results = context.search("ou=People", matchAttrs);
			while (results.hasMore()) {
				SearchResult sr = (SearchResult) results.next();
				Attributes attrs = sr.getAttributes();
				String name = attrs.get("o").get().toString();
				if (name.equals(tenant)) {
					logger.debug("tenant found: " + attrs);
					isauthed = true;
					break;
				}
			}
		} catch (Throwable t) {
			logger.error(t.getMessage());
		} finally {
			try {
				context.close();
			} catch (Exception ex) {
			}
		}
		return isauthed;
	}

    private static Map<String,String> createParams(String[] args) {
        Map<String,String> params = new HashMap<String,String>();  
        for(String str : args) {
            int delim = str.indexOf('=');
            if (delim>0) params.put(str.substring(0, delim).trim(), str.substring(delim+1).trim());
            else if (delim==0) params.put("", str.substring(1).trim());
            else params.put(str, null);
        }
        return params;
    }
    
    public static class TrustAllSSLSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory socketFactory;
        public TrustAllSSLSocketFactory() {
            try {
              SSLContext ctx = SSLContext.getInstance("TLS");
              ctx.init(null, new TrustManager[]{ new X509TrustManager() {
                  @Override
                  public X509Certificate[] getAcceptedIssuers() {
                      return null;
                  }

                  @Override
                  public void checkClientTrusted(X509Certificate[] certs, String authType) {
                      // Trust always
                  }

                  @Override
                  public void checkServerTrusted(X509Certificate[] certs, String authType) {
                      // Trust always
                  }
              }}, new SecureRandom());
              socketFactory = ctx.getSocketFactory();
            } catch ( Exception ex ){ throw new IllegalArgumentException(ex); }
        }

          public static SocketFactory getDefault() { return new TrustAllSSLSocketFactory(); }

          @Override public String[] getDefaultCipherSuites() { return socketFactory.getDefaultCipherSuites(); }
          @Override public String[] getSupportedCipherSuites() { return socketFactory.getSupportedCipherSuites(); }

          @Override public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
            return socketFactory.createSocket(socket, string, i, bln);
          }
          @Override public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
            return socketFactory.createSocket(string, i);
          }
          @Override public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
            return socketFactory.createSocket(string, i, ia, i1);
          }
          @Override public Socket createSocket(InetAddress ia, int i) throws IOException {
            return socketFactory.createSocket(ia, i);
          }
          @Override public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
            return socketFactory.createSocket(ia, i, ia1, i1);
          }
    }



}
