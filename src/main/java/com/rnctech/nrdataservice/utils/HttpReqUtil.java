package com.rnctech.nrdataservice.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import javax.security.auth.login.Configuration;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.Principal;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

/**
 * @author zilin
 * @2020.03
 */

public class HttpReqUtil {


	public static HttpClientConnectionManager CONNECTION_MANAGER = null;

	public static void loadClass(String[] jars) throws Exception {
		URL[] urls = new URL[jars.length];
		int i = 0;
		for(String jar : jars){
			urls[i] = new File(jars[i]).toURL();
			i++;
		}
		ClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		cl.loadClass("org.apache.commons.codec.binary.Base64");
		cl.loadClass("org.apache.http.impl.auth.BasicScheme");
		cl.loadClass("org.apache.http.impl.auth.BasicSchemeFactory");
	}

	public static boolean isValidStatus(int status){
		    return (HttpStatus.OK.ordinal() == status) || 
		        (HttpStatus.NO_CONTENT.ordinal() == status);
	}
	  
	public static byte[] loadBytesFrom(String name, String srcPath)
			throws Exception {
		File src = new File(srcPath + File.separator + name);
		InputStream is = null;
		if (!src.exists())
			throw new Exception("File not exists " + src.getAbsolutePath());
		Path path = Paths.get(src.getAbsolutePath());
		byte[] data = Files.readAllBytes(path);
		return data;
	}

	// utilities for ssl connection
	public void initSSL(String keyStoreFile, String keyStorePass,
			String trustStoreFile, String trustStorePass) throws Exception {		
		InputStream ksis = new FileInputStream(new File(keyStoreFile));// private key certificate
		InputStream tsis = new FileInputStream(new File(trustStoreFile));// trust certificate store

		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(ksis, keyStorePass.toCharArray());

		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(tsis, trustStorePass.toCharArray());

		SSLContext sslContext = SSLContexts.custom()
				.loadKeyMaterial(ks, keyStorePass.toCharArray())
				// if there is server certificate
				.loadTrustMaterial(ts, new TrustSelfSignedStrategy())
				.build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslContext, new String[] { "TLSv1" }, null,
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		Registry registry = RegistryBuilder.create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", sslsf).build();
		ksis.close();
		tsis.close();
		CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(registry);

	}

	/**
	 * do post
	 * 
	 * @param url
	 * @param jsonstring
	 * @throws Exception
	 */
	public void postWithSSL(String url, String jsonstring) throws Exception {
		if (CONNECTION_MANAGER == null) {
			return;
		}
		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(CONNECTION_MANAGER).build();
		HttpPost httpPost = new HttpPost(url);

		httpPost.setEntity(
				new StringEntity(jsonstring, ContentType.APPLICATION_JSON));

		CloseableHttpResponse resp = httpClient.execute(httpPost);
		InputStream respIs = resp.getEntity().getContent();
		String content = convertStreamToString(respIs);
		EntityUtils.consume(resp.getEntity());
	}

	public static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "/n");
			}
		} catch (IOException e) {
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		return sb.toString();
	}

	public static HttpClientConnectionManager initTrustAll() {
		try {
			SSLContext sslContext = SSLContexts.custom()
					// if no server certificate
					.loadTrustMaterial(null, new TrustStrategy() {
						@Override
						public boolean isTrusted(X509Certificate[] arg0,
								String arg1) throws CertificateException {
							// TODO Auto-generated method stub
							return true;
						}
					}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslContext, new String[] { "TLSv1" }, null,
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			Registry registry = RegistryBuilder.create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", sslsf).build();
			return new PoolingHttpClientConnectionManager(registry);
		} catch (Exception e) {
		}
		return null;
	}

	
    public static void getWithSSL(String sslurl, String keyStoreFile, String keyStorePass,
			String trustStoreFile, String trustStorePass) throws Exception {
		InputStream ksis = new FileInputStream(new File(keyStoreFile));// private key certificate
		InputStream tsis = new FileInputStream(new File(trustStoreFile));// trust certificate store

		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(ksis, keyStorePass.toCharArray());

		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(tsis, trustStorePass.toCharArray());
		
        // Trust own CA and all self-signed certs
        SSLContext sslcontext = SSLContexts.custom()
        		.loadKeyMaterial(ks, keyStorePass.toCharArray())
				.loadTrustMaterial(ts, new TrustSelfSignedStrategy())
                .build();
        // Allow TLSv1 protocol only
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                new String[] { "TLSv1" },
                null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
        try {
            HttpGet httpget = new HttpGet(sslurl);
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
                HttpEntity entity = response.getEntity();
                EntityUtils.consume(entity);
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }

 	public static String getRequest(String url, String user, String pwd) { //throws HttpException, IOException {

 		
 		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(new HttpHost(url)),
                new UsernamePasswordCredentials(user, pwd));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        try {
            HttpGet httpget = new HttpGet(url);
            httpget.setHeader("Accept", "application/json");
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
        		if (200 != response.getStatusLine().getStatusCode()) {
        			String errorMessage = "Call to get record for " + url + " failed. The message was " 
        					+ response.getStatusLine().getReasonPhrase();
        			throw new UnexpectedException (errorMessage);
        		}
        		//String responseBody = EntityUtils.toString(response.getEntity());
        		InputStreamReader inputStreamReader = new InputStreamReader ((InputStream)response.getEntity());
        		ReaderInputStream readerInputStream = new ReaderInputStream (inputStreamReader, Charset.forName("UTF-8"));
        		
        		String jsonPayload = IOUtils.toString(readerInputStream, StandardCharsets.UTF_8);
        		readerInputStream.close();
        		return jsonPayload;
            }catch(Exception e){   
            } finally {
                response.close();
            }
        } catch(Throwable t){
        } finally {
            try {
				httpclient.close();
			} catch (IOException e) {
			}
        }
        return null;
 	}
 	
 	
 	private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 3128;

    public static void authKerberos(String url) throws IOException {
         HttpClient httpclient = getHttpClient();
        try {
            HttpUriRequest request = new HttpGet(url);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            System.out.println("STATUS >> " + response.getStatusLine());
            if (entity != null) {
                System.out.println("RESULT >> " + EntityUtils.toString(entity));
            }
            EntityUtils.consume(entity);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

    }

    private static HttpClient getHttpClient() {
        Credentials use_jaas_creds = new Credentials() {
            public String getPassword() {
                return null;
            }

            public Principal getUserPrincipal() {
                return null;
            }
        };

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(null, -1, null), use_jaas_creds);
        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create().register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory(true)).build();
        CloseableHttpClient httpclient = HttpClients.custom()
                // set our proxy - httpclient doesn't use ProxySelector
                .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost(PROXY_HOST, PROXY_PORT)))
                .setDefaultAuthSchemeRegistry(authSchemeRegistry)
                .setDefaultCredentialsProvider(credsProvider).build();

        return httpclient;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");
        System.setProperty("java.security.auth.login.config", "login.conf");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("sun.security.jgss.debug", "true");

        // Setting default callback handler to avoid prompting for password on command line
        // check https://github.com/frohoff/jdk8u-dev-jdk/blob/master/src/share/classes/sun/security/jgss/GSSUtil.java#L241
        Security.setProperty("auth.login.defaultCallbackHandler", "net.curiousprogrammer.auth.kerberos.example.KerberosCallBackHandler");
       // autoconfigureProxy();
        authKerberos("http://example.com");
        authKerberos("https://example.com");
    }
   
	public static void main2(String[] args) {
		if(args.length < 3) {
			System.out.println("Usage: HttpReqUtil url user password [schema]");
			System.exit(1);
		}
		//	loadClass();
		
		String baseurl = args[0];
		String user = args[1];
		String pwd = args[2]; 
		String reqpath = "";
		if(args.length > 3)
			reqpath = args[3];
		
		HttpClientConnectionManager clientConnectionManager = initTrustAll();		
 		CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(new HttpHost(baseurl)),
                new UsernamePasswordCredentials(user, pwd));
        CloseableHttpClient httpclient = HttpClients.custom()
        		.setConnectionManager(clientConnectionManager)
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        
        Map<String, JsonObject> origtables = new HashMap<String, JsonObject>();
        
			String url = baseurl + reqpath;
			try {
				HttpGet httpget = new HttpGet(url);
				httpget.setHeader("Accept", "application/json");
				CloseableHttpResponse resp = httpclient.execute(httpget);
				InputStream respIs = resp.getEntity().getContent();
				String content = convertStreamToString(respIs);
				
				String jsonstring = content.substring(0, content.lastIndexOf("}")+1);
				JsonParser parser = new JsonParser();		
				JsonObject json = parser.parse(jsonstring).getAsJsonObject();
				JsonArray records = json.getAsJsonArray("records");
				records.forEach(j->{
					JsonObject table = j.getAsJsonObject();
					String name = table.get("name").getAsString();
						origtables.put(name, table);
				});
				//EntityUtils.consume(resp.getEntity());
				for(Map.Entry<String, JsonObject> entry: origtables.entrySet()){
					System.out.println(entry.getKey()+ entry.getValue()); 
				}
			} catch (Exception e) {
			}
			
			try {
				httpclient.close();
			} catch (IOException e) {
			}

	}
	
	public static class UserPasswordCallBackHandler implements CallbackHandler {
	    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
	        // call database or retrieve credentials by other means
	        String user = "rncadmin";
	        String password = "Admin123";
	        for (Callback callback : callbacks) {
	            if (callback instanceof NameCallback) {
	                NameCallback nc = (NameCallback) callback;
	                nc.setName(user);
	            } else if (callback instanceof PasswordCallback) {
	                PasswordCallback pc = (PasswordCallback) callback;
	                pc.setPassword(password.toCharArray());
	            } else {
	                throw new UnsupportedCallbackException(callback, "Unknown Callback");
	            }
	        }
	    }
	}
	
	public static class CustomLoginConfiguration extends Configuration {

	//pass certain parameters to its constructor
	//define an config entry

	private AppConfigurationEntry configEntry;

	//define a map of params you wish to pass and fill them up
	//the map contains entries similar to one you have in login.conf
	Map<String, String> params = new HashMap<String, String>();


	//implement getappconfig method
	public AppConfigurationEntry[] getAppConfigurationEntry() {
		configEntry = new AppConfigurationEntry(
	            "com.sun.security.auth.module.Krb5LoginModule",
	            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);
	    return new AppConfigurationEntry[] { configEntry };
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		AppConfigurationEntry configEntry = new AppConfigurationEntry(name,  AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, params);
		return new AppConfigurationEntry[] { configEntry };
	}
}

}
