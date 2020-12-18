package com.rnctech.nrdataservice.utils;

import static com.rnctech.nrdataservice.RNConsts.TIME_SUFFIXES;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnctech.nrdataservice.job.JobData;

/**
 * @author zilin chen
 * @since 2020.09
 */

public class ShellUtils {

	public enum JAVA_ARGS_TYPE {
		classpath, jvm, env, main
	}

	public static final String RN_JOB_CONFIG = "jobConfiguration";
	public static final String RN_JOB_POLICY = "policy";
	public static final String RN_JOB_PROPERTY = "jobProperties";
	public static final String RN_ALG_HPARAM = "params";
	public static final String RN_ALGHP_PRE = "hyparam_";
	public static final String RN_ALG_DEPEND = "libraries";
	public static final String RN_ALDEP_PRE = "depend_";
	public static final String SPARK_CONF_PRE = "spark.";
	public static final String PYTHON_CONF_PRE = "PYTHON_";

	public static final String CMD_FILE = "cmd.properties";
	
	public static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
	public static final String shell = isWindows ? "cmd /c" : "bash -c";
	public static final String dseperator = isWindows ? ";" : ":";

	public static String[] buildArgs(JobData config, Logger logger) {
		List<String> arglist = buildArgsList(config, logger);
		String[] args = new String[arglist.size()];
		arglist.toArray(args);
		return args;

	}

	public static String toArgsString(String[] args) {
		if(null == args || 0 == args.length) return "";
		StringBuilder sb = new StringBuilder();
		for(String s : args) {
			sb.append(s).append(" ");
		}
		return sb.toString();
	}
	
	public static String[] toPythonArgsList(Map<String, String> args, String jobType, boolean mockout) {
		List<String> arglist = new ArrayList<>();
		if(null != args && !args.isEmpty()) {
			if(RNUtilities.isMRJob(jobType)) {
				int i = 0;
				StringBuilder sb = new StringBuilder();
				for(Map.Entry<String, String> entry: args.entrySet()) {
					if(null != entry.getKey() && null != entry.getValue()) {
						String p = "--" + entry.getKey().trim()+","+needMockout(mockout, entry.getKey().trim(), entry.getValue());
						if(0 != i)sb.append(",");
						sb.append(p);
						i++;
					}
				}
				arglist.add("\'"+sb.toString()+"\'");
			}else {
				SortedSet<String> skey = new TreeSet<>();
				skey.addAll(args.keySet());
				
				for (String k : skey) {
					if (null != args.get(k) && 0 != args.get(k).trim().length()) {
						arglist.add(needMockout(mockout, k, args.get(k)));
					}
				}
			}		
		}
		String[] ret = new String[arglist.size()];
		arglist.toArray(ret);
		return ret;
	}
	
	public static String[] toArgsList(Map<String, String> args, String jobType) {
		return toArgsList(args, jobType, false);
	}
	
	public static String[] toArgsList(Map<String, String> args, String jobType, boolean mockout) {
		List<String> arglist = new ArrayList<>();
		if(null != args && !args.isEmpty()) {
			if(RNUtilities.isMRJob(jobType)) {
				int i = 0;
				StringBuilder sb = new StringBuilder();
				for(Map.Entry<String, String> entry: args.entrySet()) {
					if(null != entry.getKey() && null != entry.getValue()) {
						String p = "--" + entry.getKey().trim()+","+needMockout(mockout, entry.getKey().trim(), entry.getValue());
						if(0 != i)sb.append(",");
						sb.append(p);
						i++;
					}
				}
				arglist.add("\""+sb.toString()+"\"");
			}else {
				SortedSet<String> skey = new TreeSet<>();
				skey.addAll(args.keySet());
				
				for (String k : skey) {
					if (null != args.get(k) && 0 != args.get(k).trim().length()) {
						arglist.add(needMockout(mockout, k, args.get(k)));
					}
				}
			}		
		}
		String[] ret = new String[arglist.size()];
		arglist.toArray(ret);
		return ret;
	}
	
	private static String needMockout(boolean mockout, String k, String v) {
		if(null == v) return v;		
		if(!mockout)
			return v.trim();
		
		if(-1 != k.indexOf("password") && !v.isEmpty())	return StringUtils.repeat("*", v.length());
		if(-1 != k.indexOf("secret") && !v.isEmpty())	return StringUtils.repeat("*", v.length());
		else return v.trim();
	}

	public static String[] getDependencies(List<String> deps, String[] exts) {
		List<String> arglist = new ArrayList<>();
		for(String suffix : exts) {
			deps.forEach(s -> {if(s.endsWith(suffix)){arglist.add(s);}});
		}
		if(arglist.isEmpty())
			return null;
		
		String[] ret = new String[arglist.size()];
		arglist.toArray(ret);
		return ret;
	}
	
	public static String[] toArgsList(Map<String, String> args, List<String> keys) {
		List<String> arglist = new ArrayList<>();
		if(null != keys && !keys.isEmpty()) {
			for(String k: keys) {
				if(args.containsKey(k)) 
					arglist.add(args.get(k).trim());
			}
		}
		
		if(arglist.isEmpty() && !args.isEmpty())
			for (Map.Entry<String, String> entry : args.entrySet()) {
				if (null != entry.getValue() && 0 != entry.getValue().trim().length()) {
					//arglist.add("--" + entry.getKey().trim());
					arglist.add(entry.getValue().trim());
				}
			}
		
	
		String[] ret = new String[arglist.size()];
		arglist.toArray(ret);
		return ret;
	}

	public static List<String> buildArgsList(JobData job, Logger logger) {
		ObjectMapper oMapper = new ObjectMapper();
		Map<String, Object> amap = oMapper.convertValue(job.getJobConfiguration(), Map.class);
		try {
			amap.remove(RN_JOB_CONFIG);
			amap.put(RN_JOB_CONFIG, oMapper.writer().writeValueAsString(job.getJobConfiguration()).replaceAll("\"", "\\\""));

		} catch (JsonProcessingException e) {
			logger.error(e.getMessage());
		}
		List<String> arglist = new ArrayList<>();
		for (Map.Entry<String, Object> entry : amap.entrySet()) {
			Object o = entry.getValue();
			if (null != o) {
				arglist.add("--" + entry.getKey().trim().toLowerCase());
				arglist.add(entry.getValue().toString());
			}
		}
		return arglist;
	}

	public static Object executeShellCommand(String command, boolean waitForResponse, Logger logger) {
		ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
		return executeCommand(pb, waitForResponse, logger);
	}

	public static Object executeJava(JobData config, boolean waitForResponse, Logger logger) {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("java");
		Properties props = setJavaArgs(cmd, CMD_FILE, logger);
		cmd.addAll(buildArgsList(config, logger));
		logger.info("build java command to run as:\n" + RNUtilities.aTos(cmd.toArray(new String[cmd.size()])));
		ProcessBuilder pb = new ProcessBuilder(cmd.toArray(new String[cmd.size()]));

		String envprops = (String) props.get(JAVA_ARGS_TYPE.env.name());
		Map<String, String> env = pb.environment();
		env.put("JOB_NAME", config.getJobkey());
		if (null != envprops && !StringUtils.isEmpty(envprops)) {
			// add extra env parameters for general case
		}
		return executeCommand(pb, waitForResponse, logger);
	}

	public static Properties setJavaArgs(ArrayList<String> cmd, String propfile, Logger logger) {
		Properties props = new Properties();
		InputStream is = ShellUtils.class.getResourceAsStream(propfile);
		try {
			props.load(is);
		} catch (IOException e) {
			is = ShellUtils.class.getClassLoader().getResourceAsStream(propfile);
			try {
				props.load(is);
			} catch (IOException e1) {
				is = ClassLoader.getSystemResourceAsStream(propfile);
				try {
					props.load(is);
				} catch (IOException e2) {
					logger.error(ShellUtils.class.getName() + ": " + e.getMessage());
				}
			}
		}
		return setJavaArgs(cmd, props, logger);
	}

	// @Todo need validate jar load, jvm setting etc. in future
	public static Properties setJavaArgs(ArrayList<String> cmd, Properties props, Logger logger) {
		String maincls = (String) props.get(JAVA_ARGS_TYPE.main.name());
		String cps = (String) props.get(JAVA_ARGS_TYPE.classpath.name());
		if (null != cps && !StringUtils.isEmpty(cps)) {
			cmd.add("-cp");
			cmd.add(cps.trim());
		}
		String jvmargs = (String) props.get(JAVA_ARGS_TYPE.jvm.name());
		if (null != jvmargs && !StringUtils.isEmpty(jvmargs)) {
			cmd.add(jvmargs.trim());
		}
		if (null != maincls && !StringUtils.isEmpty(maincls)) {
			cmd.add(maincls);
		}
		return props;
	}

	public static Object executeCommand(ProcessBuilder pb, boolean waitForResponse, Logger logger) {
		String response = "";
		pb.directory(new File(System.getenv("user.home")));
		pb.redirectErrorStream(true);
		logger.info("Run command: " + pb.command());
		try {
			Process aprocess = pb.start();
			if (waitForResponse) {
				InputStream shellIn = aprocess.getInputStream();
				int shellExitStatus = aprocess.waitFor();
				logger.info("Exit status" + shellExitStatus);
				response = printStream(shellIn);
				shellIn.close();
			} else {
				return aprocess;
			}
		} catch (Exception e) {
			logger.error("Error occured while executing command " + pb.command() + ".\n" + e.getMessage());
		}
		return response;
	}

	public static String printStream(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	public static void printFile(File file, Logger logger) throws IOException {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null) {
			if (null != logger)
				logger.info(line + "\n");
			else
				System.out.println(line);
		}
		br.close();
		fr.close();
	}

	public static int findFirstAvailablePort() throws IOException {
		int port;
		try (ServerSocket socket = new ServerSocket(0);) {
			port = socket.getLocalPort();
			socket.close();
		}
		return port;
	}

	public static String findAvailableHostAddress() throws UnknownHostException, SocketException {
		InetAddress address = InetAddress.getLocalHost();
		if (address.isLoopbackAddress()) {
			for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!networkInterface.isLoopback()) {
					for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
						InetAddress a = interfaceAddress.getAddress();
						if (a instanceof Inet4Address) {
							return a.getHostAddress();
						}
					}
				}
			}
		}
		return address.getHostAddress();
	}

	public static boolean checkIfRemoteEndpointAccessible(String host, int port) {
		try {
			Socket discover = new Socket();
			discover.setSoTimeout(1000);
			discover.connect(new InetSocketAddress(host, port), 1000);
			discover.close();
			return true;
		} catch (ConnectException cne) {
			return false;
		} catch (IOException ioe) {
			return false;
		}
	}

	public static String getInterpreterSettingId(String intpGrpId) {
		String settingId = null;
		if (intpGrpId != null) {
			int indexOfColon = intpGrpId.indexOf(":");
			settingId = intpGrpId.substring(0, indexOfColon);
		}
		return settingId;
	}

	public static boolean isEnvString(String key) {
		if (key == null || key.length() == 0) {
			return false;
		}

		return key.matches("^[A-Z_0-9]*");
	}
	
	public static Long getTimeAsMs(String time) {
		if (time == null) {
			time = "1d";
		}

		Matcher m = Pattern.compile("(-?[0-9]+)([a-z]+)?").matcher(time.toLowerCase());
		if (!m.matches()) {
			throw new IllegalArgumentException("Invalid time string: " + time);
		}

		long val = Long.parseLong(m.group(1));
		String suffix = m.group(2);

		if (suffix != null && !TIME_SUFFIXES.containsKey(suffix)) {
			throw new IllegalArgumentException("Invalid suffix: \"" + suffix + "\"");
		}

		return TimeUnit.MILLISECONDS.convert(val, suffix != null ? TIME_SUFFIXES.get(suffix) : TimeUnit.MILLISECONDS);
	}

	public static String toEncryptArgsList(Map<String, String> params, String jobType) {
		// TODO Auto-generated method stub
		return null;
	}
}
