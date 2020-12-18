package com.rnctech.nrdataservice.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.rnctech.nrdataservice.RNContext;
import com.rnctech.nrdataservice.exception.APIException;
import com.rnctech.nrdataservice.exception.APILivyException;
import com.rnctech.nrdataservice.exception.APINotFoundException;
import com.rnctech.nrdataservice.exception.APISessionException;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.job.JobConfig;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.repo.JobDetailsRepository;
import com.rnctech.nrdataservice.repo.JobRepository;
import com.rnctech.nrdataservice.resource.MemResourcePool;
import com.rnctech.nrdataservice.resource.ResourcePool;
import com.rnctech.nrdataservice.service.RNResult.ResultCompletion;
import com.rnctech.nrdataservice.utils.RNUtilities;

import static com.rnctech.nrdataservice.RNConsts.*;

/*
 * base class for Job Executor
* @author zilin chen
* @since 2020.09
*/

public abstract class RNJobExecutor implements IJobExecutor {

	protected  static final String SESSION_NOT_FOUND_PATTERN = "\"Session '\\d+' not found.\"";
	@Autowired
	private ApplicationContext springCtx;

	@Autowired
	protected ThreadPoolTaskExecutor executor;

	@Autowired
	private Environment env;

	protected Properties properties;
	protected String tenantName;
	protected String userName;
	protected boolean jobinited = false;
	private URL[] classloaderUrls;
	private ResourcePool resourcePool = new MemResourcePool();
	private int progress = 0;

	static {
		TIME_SUFFIXES.put("us", TimeUnit.MICROSECONDS);
		TIME_SUFFIXES.put("ms", TimeUnit.MILLISECONDS);
		TIME_SUFFIXES.put("s", TimeUnit.SECONDS);
		TIME_SUFFIXES.put("m", TimeUnit.MINUTES);
		TIME_SUFFIXES.put("min", TimeUnit.MINUTES);
		TIME_SUFFIXES.put("h", TimeUnit.HOURS);
		TIME_SUFFIXES.put("d", TimeUnit.DAYS);
	}

	public static Logger logger = LoggerFactory.getLogger(RNJobExecutor.class);

	public abstract void initProp(Properties property);

	public abstract void cancel(RNContext context) throws RNBaseException;

	public int getProgress(RNContext context) throws RNBaseException{
		return context.getProgressing() + this.progress;
	}

	public List<ResultCompletion> completion(String buf, int cursor, RNContext context) {
		return Collections.emptyList();
	}

	public RNJobExecutor(Properties properties) {
		this.properties = properties;
		//this.properties.putAll(loadEnvProperties());
	}

	public void setProperties(Properties properties) {
		initJobProperties();
		properties.forEach((k, v) -> {
			if (this.properties.containsKey(k))
				this.properties.replace(k, v);
			else
				this.properties.put(k, v);
		});

		initProp(this.properties);
	}

	public synchronized Properties initJobProperties() {
		if (!jobinited) {
			this.properties.putAll(loadTenantProperties());
			this.properties.putAll(loadJobProperties());
			jobinited = true;
		}
		return this.properties;
	}

	protected Map<? extends Object, ? extends Object> loadEnvProperties() {
		return System.getenv();
	}

	protected Map<? extends Object, ? extends Object> loadTenantProperties() {
		return Collections.emptyMap();
	}

	protected Map<? extends Object, ? extends Object> loadJobProperties() {
		Map<String, String> envp = new HashMap<>();
		try {
			if(null != System.getenv(javahome)) envp.put(javahome, System.getenv(javahome));
			if(null != System.getenv(pypath)) envp.put(pypath, System.getenv(pypath));
			if(null != System.getenv(scalahome)) envp.put(scalahome, System.getenv(scalahome));
		} catch (Exception e) { //track sudo or env related issue
			logger.error("Get JAVA_HOME failed: "+e.getMessage());
		}
		return envp;
	}

	public String getAppName(RNContext ctx) {
		String jobname = (null != ctx.getJobname())?ctx.getJobname():"";		
		if(!jobname.isEmpty()) {
			return jobname;
		}else {
			String ts = "_"+RNUtilities.getUTCTimeStringShort(new Date());
			if(null != ctx.getSrcname()) {
				int idx = ctx.getSrcname().lastIndexOf(".");
				return (-1 == idx)?jobname+ctx.getSrcname()+ts: jobname+ctx.getSrcname().substring(0, idx)+ts;
			}else {
				return "RN"+ts;
			}			
		}
	}
	
	public String getProperty(String key) {
		return getProperty(key, null);
	}

	public String getProperty(String key, String defaultValue) {
		if (null != env && env.containsProperty(key))
			return env.getProperty(key);
		return this.properties.getProperty(key, defaultValue);
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public Properties getProperties() {
		return this.properties;
	}

	protected <T> T lookupBean(String beanName, Class<T> clazz) throws RNBaseException {
		T bean = springCtx.getBean(beanName, clazz);
		if (bean == null) {
			throw new RNBaseException("Mandatory Spring bean '" + beanName + "' missing! Aborting");
		}
		return bean;
	}

	@Override
	public IJobExecutor withAppContext(ApplicationContext appContext) {
		this.springCtx = appContext;
		return this;
	}
	
	@Override
	public IJobExecutor withRepo(JobRepository jobrepo, JobDetailsRepository jobdetailrepo) {
		return this;
	}
	
	@Override
	public RNResult status(RNContext context) throws RNBaseException {
		return null;
	}
	
	@Override
	public boolean validate(String s) throws RNBaseException{
		return true;
	}
	
	public RNResult executePrecode(RNContext interpreterContext) throws RNBaseException {
		String simpleName = this.getClass().getSimpleName();
		String precode = getProperty(String.format("exec.%s.precode", simpleName));
		if (StringUtils.isNotBlank(precode)) {
			return exec(precode, interpreterContext);
		}
		return null;
	}

	public static Authentication getAuthentication() {
		Authentication authentication = null;
		SecurityContext context = SecurityContextHolder.getContext();
		authentication = (context != null) ? context.getAuthentication() : null;

		return authentication;
	}

	public static void setContext(Authentication auth) {
		SecurityContext ctx = SecurityContextHolder.createEmptyContext();
		ctx.setAuthentication(auth);
		SecurityContextHolder.setContext(ctx);
	}
	
	public static JobData getJSD(RNContext context) {
		Map<String, String> params = context.getParams();
		if(null == params || params.isEmpty()) return null;
		if(!params.containsKey("configurl")) return null;		
		JobData jsd = new JobData();
		JobConfig jc = new JobConfig();
		jc.setName(params.get("name"));
		jsd.setJobConfiguration(jc);
		jsd.setJobid(Long.valueOf(params.get("jobid")));
		jsd.setAllowssl(Boolean.valueOf(params.get("allowssl")));
		jsd.setconfigurl(params.get("configurl"));
		jsd.setUser(params.get("user"));
		jsd.setPassword(params.get("password"));
		return jsd;
	}
	
	public HttpClient createHttpClientTrustAll()
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		/*
		 * SSLContext sslContext = SSLContexts.custom() // if no server certificate
		 * .loadTrustMaterial(null, new TrustStrategy() {
		 * 
		 * @Override public boolean isTrusted(X509Certificate[] arg0, String arg1)
		 * throws CertificateException { return true; } }).build();
		 * SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
		 * sslContext, new String[] { "TLSv1" }, null,
		 * SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER); Registry registry =
		 * RegistryBuilder.create() .register("http",
		 * PlainConnectionSocketFactory.INSTANCE) .register("https", sslsf).build();
		 * return new PoolingHttpClientConnectionManager(registry);
		 */

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
	
	protected RestTemplate createRestTemplate(boolean usessl, String keystoreFile, String password) {
		HttpClient httpClient = null;
		boolean isTrustall = false;
		if (usessl) {
			if (StringUtils.isBlank(keystoreFile)) {
				isTrustall = true;
			}
			if (StringUtils.isBlank(password)) {
				password = "";
			}
			try {
				if (isTrustall) {
					httpClient = createHttpClientTrustAll();
				} else {
					httpClient = createAcceptSelfSignedCertificateClient(keystoreFile, password);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to create SSL HttpClient", e);
			}

		}
		RestTemplate restTemplate = null;
		if (httpClient == null) {
			restTemplate = new RestTemplate();
		} else {
			restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
		}

		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		return restTemplate;
	}

	protected static CloseableHttpClient createAcceptSelfSignedCertificateClient(String keystoreFile, String pwd)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException,
			IOException {

		/*
		 * inputStream = new FileInputStream(keystoreFile); KeyStore trustStore =
		 * KeyStore.getInstance(KeyStore.getDefaultType()); trustStore.load(new
		 * FileInputStream(keystoreFile), password.toCharArray()); SSLContext sslContext
		 * = SSLContexts.custom() .loadTrustMaterial(trustStore) .build();
		 * SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
		 * HttpClientBuilder httpClientBuilder =
		 * HttpClients.custom().setSSLSocketFactory(csf); RequestConfig reqConfig = new
		 * RequestConfig() {
		 * 
		 * @Override public boolean isAuthenticationEnabled() { return true; } };
		 * httpClientBuilder.setDefaultRequestConfig(reqConfig); Credentials credentials
		 * = new Credentials() {
		 * 
		 * @Override public String getPassword() { return null; }
		 * 
		 * @Override public Principal getUserPrincipal() { return null; } };
		 * CredentialsProvider credsProvider = new BasicCredentialsProvider();
		 * credsProvider.setCredentials(AuthScope.ANY, credentials);
		 * httpClientBuilder.setDefaultCredentialsProvider(credsProvider); httpClient =
		 * httpClientBuilder.build();
		 */

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

	protected String callRestAPI(RestTemplate restTemplate, Map<String, String> customHeaders, String targetURL, String method, String jsonData) throws APIException {
		//logger.debug("Call rest api in {}, method: {}, jsonData: {}", targetURL, method, jsonData);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
		headers.add("X-Requested-By", "rnctech");
		if(null != customHeaders && !customHeaders.isEmpty()) {
			for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
				headers.add(entry.getKey(), entry.getValue());
			}
		}
		ResponseEntity<String> response = null;
		try {
			if (method.equals("POST")) {
				HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);
				response = restTemplate.exchange(targetURL, HttpMethod.POST, entity, String.class);
			} else if (method.equals("GET")) {
				HttpEntity<String> entity = new HttpEntity<>(headers);
				response = restTemplate.exchange(targetURL, HttpMethod.GET, entity, String.class);
			} else if (method.equals("DELETE")) {
				HttpEntity<String> entity = new HttpEntity<>(headers);
				response = restTemplate.exchange(targetURL, HttpMethod.DELETE, entity, String.class);
			}
		} catch (HttpClientErrorException e) {
			response = new ResponseEntity(e.getResponseBodyAsString(), e.getStatusCode());
			logger.error(String.format("Error with %s StatusCode: %s", response.getStatusCode().value(),
					e.getResponseBodyAsString()));
		} catch (RestClientException e) {
			if (e instanceof ResourceAccessException) {
				throw new APILivyException(e.getMessage());
			}
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException cause = (HttpClientErrorException) e.getCause();
				if (cause.getResponseBodyAsString().matches(SESSION_NOT_FOUND_PATTERN)) {
					throw new APISessionException(cause.getResponseBodyAsString());
				}
				throw new APIException(cause.getResponseBodyAsString() + "\n"
						+ ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(e)));
			}
			if (e instanceof HttpServerErrorException) {
				HttpServerErrorException errorException = (HttpServerErrorException) e;
				String errorResponse = errorException.getResponseBodyAsString();
				if (errorResponse.contains("Session is in state dead")) {
					throw new APISessionException();
				}
				throw new APIException(errorResponse, e);
			}
			throw new APILivyException(e);
		}
		if (response == null) {
			throw new APIException("No http response returned");
		}
		logger.trace("Get response, StatusCode: {}, responseBody: {}", response.getStatusCode(), response.getBody());
		if (response.getStatusCode().value() == 200 || response.getStatusCode().value() == 201) {
			return response.getBody();
		} else if (response.getStatusCode().value() == 404) {
			if (response.getBody().matches(SESSION_NOT_FOUND_PATTERN)) {
				throw new APISessionException(response.getBody());
			} else {
				throw new APINotFoundException("No rest api found for " + targetURL + ", " + response.getStatusCode());
			}
		} else {
			String responseString = response.getBody();
			if (responseString.contains("CreateInteractiveRequest[\\\"master\\\"]")) {
				return responseString;
			}
			throw new APIException(
					String.format("Error with %s StatusCode: %s", response.getStatusCode().value(), responseString));
		}
	}
	public String getClassName() {
		return this.getClass().getName();
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}

	public URL[] getClassloaderUrls() {
		return classloaderUrls;
	}

	public void setClassloaderUrls(URL[] classloaderUrls) {
		this.classloaderUrls = classloaderUrls;
	}

	public ResourcePool getResourcePool() {
		return resourcePool;
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public void setResourcePool(ResourcePool resourcePool) {
		this.resourcePool = resourcePool;
	}

	public Environment getEnv() {
		return env;
	}

	public boolean isJobinited() {
		return jobinited;
	}
}
