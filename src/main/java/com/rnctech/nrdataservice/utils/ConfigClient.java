package com.rnctech.nrdataservice.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import com.rnctech.nrdataservice.job.JobData;
import com.rnctech.nrdataservice.RNApplication;
import com.rnctech.nrdataservice.RNConsts;
import com.rnctech.nrdataservice.common.Messages;

/**
 * Communicate with Configure Service
 * @author zilin @2020.09
 */

public class ConfigClient implements RNConsts {

	public static Logger logger = Logger.getLogger(ConfigClient.class);
	public static Object[] sk = new Object[] {};
	public static String modelapi = "/api/v1/models";
	public static String contextapi = "/api/v1/context";

	private static Map<String, ConfigClient> configs = new ConcurrentHashMap<>();
	private static Map<String, Map<String, String>> userProperties = new ConcurrentHashMap<>();

	public ConfigClient(String configurl, String user, String password) {
		// TODO Auto-generated constructor stub
	}

	public static String getJobKey(JobData jobdata) {
		return jobdata.getJobConfiguration().getName() + "_" + jobdata.getJobid();
	}

	public static void close(JobData jobdata) {
		if (RNApplication.isPyEngine())
			return;
		String k = getJobKey(jobdata);
		synchronized (sk) {
			userProperties.remove(k);
			configs.remove(k);
		}
	}

	public static String getConfig(String propName, JobData jobdata, String dft) throws Exception {
		if (RNApplication.isPyEngine())
			return dft;
		initConfig(jobdata);
		return configs.get(getJobKey(jobdata)).getConfig(propName);
	}

	public static String getConfig(String propName) {
		// TODO
		return null;
	}

	public static boolean authWithAccount(String configurl, String user, String pwd, boolean isSSL, String tenant) {
		if (RNApplication.isPyEngine())
			return true;
		try {
			// @TODO implement here
			ConfigClient config = new ConfigClient(configurl, user, pwd);
			config.authenticate();
		} catch (Throwable t) {
			logger.error("Authenticate user " + user + " failed with error: " + t.getMessage());
			return false;
		}
		return true;
	}

	private void authenticate() {
		// TODO Auto-generated method stub

	}

	public static void initConfig(JobData jobdata) throws Exception {
		if (RNApplication.isPyEngine())
			return;
		String k = getJobKey(jobdata);
		if (null == configs.get(k)) {
			synchronized (synkey) {
				try {
					ConfigClient config = new ConfigClient(jobdata.getconfigurl(), jobdata.getUser(),
							jobdata.getPassword());
					configs.put(k, config);
				} catch (Throwable t) {
					String errmsg = ConfigClient.class + " " + jobdata.getJobConfiguration().getName() + " "
							+ jobdata.getconfigurl() + "\n" + t.getMessage();
					logger.error(errmsg);
					throw new Exception(errmsg);
				}
			}
		}
	}

	public static String getNameConfig(JobData jobdata, String key) throws Exception {
		return getNameConfig(jobdata, key, null);
	}

	public static String getNameConfig(JobData jobdata, String key, String defv) throws Exception {
		if (RNApplication.isPyEngine())
			return defv;
		String p = getNameConfigs(jobdata).get(key);
		return (null == p) ? defv : p;
	}

	public static Map<String, String> getNameConfigs(JobData jobdata) throws Exception {
		if (RNApplication.isPyEngine())
			return new HashMap<String, String>();
		String k = getJobKey(jobdata);
		if (userProperties.containsKey(k))
			return userProperties.get(k);

		initConfig(jobdata);
		Map<String, String> properties = configs.get(k).getAllProperties();
		// properties.putAll(configs.get(k).getAllJobProperties());
		userProperties.put(k, properties);
		return userProperties.get(k);

	}

	private Map<String, String> getAllProperties() {
		//@TODO Auto-generated method stub
		return null;
	}

	public static void updateProperties(JobData jobdata, STATUS mrstatus, String msg, Date start, Date end)
			throws Exception {
		STATUS status = STATUS.valueOf(mrstatus.name());
		updateProperties(jobdata, status, msg, start, end, false);
	}

	public static void updateProperties(JobData jobdata, STATUS status, String msg, Date start, Date end,
			boolean checkfirst) throws Exception {
		if (RNApplication.isPyEngine())
			return;
		initConfig(jobdata);
		String k = getJobKey(jobdata);
		ConfigClient config = configs.get(k);
		new Thread(() -> {
			boolean isJobCompleted = false;
			try {
				if (checkfirst) {
					String sts = config.getStatus();
					if (sts.equalsIgnoreCase(STATUS.CANCELLED.name()) || sts.equalsIgnoreCase(STATUS.COMPLETED.name())
							|| sts.equalsIgnoreCase(STATUS.COMPLETED_WITH_WARNINGS.name())
							|| sts.equalsIgnoreCase(STATUS.COMPLETED_WITH_ERRORS.name())
							|| sts.equalsIgnoreCase(STATUS.FAILED.name()))
						isJobCompleted = true;
				}
				if (!isJobCompleted) {
					config.updateStatus(status, msg);
					Map<String, String> jobp = new HashMap<>();
					jobp.put("status", status.name());
					if (null != start)
						jobp.put("starttime", RNUtilities.getLocalSysTimeString(start));
					if (null != end)
						jobp.put("endtime", RNUtilities.getLocalSysTimeString(end));
					if (null != msg)
						jobp.put("status", msg);
					config.updateProperties(jobp);
				}
			} catch (Throwable e) {
				if (null != e.getMessage() && e.getMessage().contains("Authentication failed")) {
					try {
						Thread.sleep(30000);
					} catch (InterruptedException ex) {
					}
				}
				try {
					// try reconnect to MR
					ConfigClient configclient = new ConfigClient(jobdata.getconfigurl(), jobdata.getUser(), jobdata.getPassword());
					configs.put(k, configclient);
					if (checkfirst) {
						String sts = configclient.getStatus();
						if (sts.equalsIgnoreCase(STATUS.CANCELLED.name())
								|| sts.equalsIgnoreCase(STATUS.COMPLETED.name())
								|| sts.equalsIgnoreCase(STATUS.COMPLETED_WITH_WARNINGS.name())
								|| sts.equalsIgnoreCase(STATUS.COMPLETED_WITH_ERRORS.name())
								|| sts.equalsIgnoreCase(STATUS.FAILED.name()))
							isJobCompleted = true;
					}
					if (!isJobCompleted) {
						config.updateStatus(status, msg);
						Map<String, String> jobp = new HashMap<>();
						jobp.put("status", status.name());
						if (null != start)
							jobp.put("starttime", RNUtilities.getLocalSysTimeString(start));
						if (null != end)
							jobp.put("endtime", RNUtilities.getLocalSysTimeString(end));
						if (null != msg)
							jobp.put("status", msg);
						configclient.updateProperties(jobp);
					}
				} catch (Throwable e1) {
					logger.error(e1);
				}
			}
		}).start();

	}

	public static void updateStatus(JobData jobdata, STATUS status, Date start, Date end) throws Exception {
		updateProperties(jobdata, status, null, start, end);
	}

	public static void updatePropertiesInternal(JobData jobdata, long jobId, Map<String, String> jobp) {
		try {
			ConfigClient config = configs.get(getJobKey(jobdata));
			config.updateProperties(jobp);
			logger.info(String.join(""+jobId, jobdata.getJobConfiguration().getName()));
			config.close();
		} catch (Throwable t) {
			logger.error(ConfigClient.class + " " + jobdata.getJobConfiguration().getName() + " "
					+ jobdata.getconfigurl() + "\n" + t.getMessage());
		}
	}

	private void close() {
		// TODO Auto-generated method stub

	}

	private void updateProperties(Map<String, String> jobp) {
		// TODO Auto-generated method stub

	}

	private void updateStatus(STATUS status, String msg) {
		// TODO Auto-generated method stub

	}

	private String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTemplateFile(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
