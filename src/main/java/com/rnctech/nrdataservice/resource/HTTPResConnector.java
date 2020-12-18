package com.rnctech.nrdataservice.resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.transform.Source;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.rnctech.nrdataservice.exception.ResourceException;
import com.rnctech.nrdataservice.utils.FileUtil;

/**
 * connector for HTTP/HTTPS
 * @author zilin 2020.06
 */

public class HTTPResConnector extends RestResConnector {
	
	RestTemplate restTemplate;

	public HTTPResConnector(ConnectInfo connction) {
		super(connction);
	}

	public HTTPResConnector(String httpurl, String username, String password) {
		super(httpurl, username, password);
	}

	public HTTPResConnector(String httpurl, String urlpath, String username, String password, boolean encrypted) {
		super(httpurl, urlpath, username, password, encrypted);
	}
	
	@Override
	public void init() {
		super.init();
		HttpClient httpClient = null;
		boolean isTrustall = false;
		if(connction.baseUrl.startsWith("https")){
				if (StringUtils.isBlank(connction.key)) {
					isTrustall = true;
				}
				String password  = "";
				if (!StringUtils.isBlank(connction.secret)) {
					password = connction.secret;
				}
				try {
					if (isTrustall) {
						httpClient = createHttpClientTrustAll();
					} else {
						httpClient = createAcceptSelfSignedCertificateClient(connction.key, password);
					}
				} catch (Exception e) {
					throw new RuntimeException("Failed to create SSL HttpClient", e);
				}

			}

			if (httpClient == null) {
				restTemplate = new RestTemplate();
			} else {
				restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
			}

			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
			restTemplate.getMessageConverters().add(1, new ByteArrayHttpMessageConverter());
		    restTemplate.getMessageConverters().add(2, new ResourceHttpMessageConverter());
		    restTemplate.getMessageConverters().add(3, new SourceHttpMessageConverter<Source>());
		    restTemplate.getMessageConverters().add(4, new AllEncompassingFormHttpMessageConverter());

	}
	
	
	@Override
	public Object readResource(ResourceId rid) {

	    HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));

	    HttpEntity<String> entity = new HttpEntity<String>(headers);
	    String requrl = connction.baseUrl + connction.basename + rid.getName();
	    ResponseEntity<byte[]> response = restTemplate.exchange(requrl, HttpMethod.GET, entity, byte[].class, "1");

	    if (response.getStatusCode() == HttpStatus.OK) {
			byte[] ret = response.getBody();
			return ret;			
	    }else {
	    	return response.getStatusCode().name();
	    }		
	}
	
	
	public Object readResourceAsFile(ResourceId rid) throws ResourceException{
		Object o = readResource(rid);
		if(o instanceof String) {
			throw new ResourceException("http failed with code: "+o);
		}else {
			File tmpf = null;
			try {
				String fname = rid.getName();
				String ext = null;
				int indx = fname.lastIndexOf(".");
				if(-1 != indx) {
					ext = fname.substring(indx);  //ext as .py
					fname = fname.substring(0, indx).replace(".", "_");					
				}
					
				tmpf = FileUtil.createTempFile(fname, ext);
				//FileUtil.saveFile(tmpf.getAbsolutePath(), response.getBody());
				Files.write(Paths.get(tmpf.getAbsolutePath()), (byte[])o);
				return tmpf;
			} catch (IOException e) {
				throw new ResourceException("faile to save to tmp: "+e.getMessage());
			}
		}
	}
	
	
	public HttpClient createHttpClientTrustAll()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		HttpClientBuilder b = HttpClientBuilder.create();
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				return true;
			}
		}).build();
		b.setSslcontext(sslContext);

		HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory)
				.build();

		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		b.setConnectionManager(connMgr);

		HttpClient client = b.build();
		return client;
	}

	private static CloseableHttpClient createAcceptSelfSignedCertificateClient(String keystoreFile, String pwd)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
			IOException {
		// Trust own CA and all self-signed certs
		SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(new File(keystoreFile), pwd.toCharArray(), new TrustSelfSignedStrategy()).build();

		// Disable hostname verification.
		HostnameVerifier allowAllHosts = new NoopHostnameVerifier();
		// Allow TLSv1 protocol only
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null,
				allowAllHosts);
		CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
		return httpclient;
	}
	

}
