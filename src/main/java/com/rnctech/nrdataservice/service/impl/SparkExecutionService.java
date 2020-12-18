package com.rnctech.nrdataservice.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.service.ISparkExecutionService;
import com.rnctech.nrdataservice.utils.AWSConnUtil;

@Service
public class SparkExecutionService implements ISparkExecutionService {

	static final String SPARK_SUBMIT = "/bin/spark-submit";
	static final String DOWNLOAD_DIRECTORY = "tempDir";
	static final String AWS_S3_BUCKET_NAME = "S3_BUCKET_NAME";
	static final String AWS_S3_DIRECTORY = "S3_DIRECTORY_NAME";

	protected static final Logger logger = LoggerFactory.getLogger(SparkExecutionService.class);

	@Override
	public String launch(String fileName, JSONObject properties) throws Exception {
		String bucketName = properties.getString(AWS_S3_BUCKET_NAME);
		String directoryName = properties.getString(AWS_S3_DIRECTORY);

		String keyName = directoryName + "/" + fileName;
		S3Object s3object = null;
		AmazonS3 s3client = null;
		int exitVal = 1;
		try {
			s3client = AWSConnUtil.getS3Connection();
		} catch (RNBaseException e1) {
			logger.error(e1.getMessage());
		}
		try {
			s3object = s3client.getObject(new GetObjectRequest(bucketName, keyName));
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}

		Path scriptTempDir = Files.createTempDirectory(DOWNLOAD_DIRECTORY);
		File file = null;
		FileOutputStream out = null;
		InputStream byteStream = s3object.getObjectContent();
		byte[] byteBuff = null;
		int bytesRead;

		String scriptName = fileName;
		File shellfile = null;
		try {
			file = new File(scriptTempDir.toString() + "/" + scriptName);
			file.createNewFile();
			if (file.exists()) {
				out = new FileOutputStream(file);
				byteBuff = new byte[4096];
				bytesRead = 0;
				while ((bytesRead = byteStream.read(byteBuff)) != -1) {
					out.write(byteBuff, 0, bytesRead);
				}
				out.close();

				setAllPermissions(file);
				List<String> inputList = new ArrayList<String>();
				inputList.add(getSparkHomePath() + SPARK_SUBMIT);
				inputList.add("--master yarn");
				inputList.add("--name " + fileName);
				inputList.add("--deploy-mode cluster");
				inputList.add("--conf spark.yarn.submit.waitAppCompletion=false");
				inputList.add("--packages com.amazonaws:aws-java-sdk-pom:1.11.333,org.apache.hadoop:hadoop-aws:2.7.0");
				inputList.add("--py-files /tmp/py.zip");
				inputList.add(file.getAbsolutePath());

				shellfile = writeShscript(inputList);
				List<String> cmdlist = new ArrayList<String>();
				cmdlist.add(shellfile.getAbsolutePath());

				exitVal = runScript(cmdlist);
				if (exitVal == 0) {
					logger.info("Executed successfully");
				} else {
					logger.error("Execute with error " + exitVal);
					throw new Exception("Spark submit job failed.");
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (shellfile != null) {
				shellfile.delete();
			}
		}

		if (exitVal == 0)
			return "Success";
		else
			return "Failed";
	}

	public void setAllPermissions(File file) throws IOException {

		if (file.exists()) {
			HashSet<PosixFilePermission> set = new HashSet<PosixFilePermission>();
			set.add(PosixFilePermission.OWNER_EXECUTE);
			set.add(PosixFilePermission.OWNER_READ);
			set.add(PosixFilePermission.OWNER_WRITE);
			set.add(PosixFilePermission.GROUP_EXECUTE);
			set.add(PosixFilePermission.GROUP_READ);
			set.add(PosixFilePermission.GROUP_WRITE);
			set.add(PosixFilePermission.OTHERS_EXECUTE);
			set.add(PosixFilePermission.OTHERS_READ);
			set.add(PosixFilePermission.OTHERS_WRITE);
			Files.setPosixFilePermissions(Paths.get(file.getAbsolutePath()), set);
		} else {
			logger.info("File doesn't exist.");
		}
	}

	private File writeShscript(List<String> commands) throws Exception {
		File tempFile = File.createTempFile("temp" + System.currentTimeMillis(), ".sh");

		setAllPermissions(tempFile);

		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/bash\n");
		sb.append("export SPARK_HOME=" + getSparkHomePath()).append("\n");

		for (String cmd : commands) {
			sb.append(cmd).append(" ");
		}

		FileUtils.writeStringToFile(tempFile, sb.toString().trim());
		return tempFile;
	}

	private int runScript(List<String> commandList) {
		int exitVal = 1;

		try {
			String line;

			InputStream stderr = null;
			InputStream stdout = null;

			Process process = Runtime.getRuntime().exec(commandList.stream().toArray(String[]::new));

			stderr = process.getErrorStream();
			stdout = process.getInputStream();

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stderr));
			while ((line = bufferedReader.readLine()) != null) {
				logger.info(line);
			}
			bufferedReader.close();

			bufferedReader = new BufferedReader(new InputStreamReader(stdout));
			while ((line = bufferedReader.readLine()) != null) {
				logger.info(line);
			}
			bufferedReader.close();

			exitVal = process.waitFor();
			logger.info("PySpark script exited with code " + exitVal);
		} catch (Exception err) {
			err.printStackTrace();
		}
		return exitVal;
	}

	@Override
	public String checkConfig(String yarnURL) throws Exception {
		String sparkStatus = "FALSE";
		String yarnClusterInfoURL = yarnURL + "/ws/v1/cluster/info";
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(yarnClusterInfoURL);
		int statusCode = 0;
		String response = null;
		try {
			logger.info("Call to Yarn Cluster:" + yarnClusterInfoURL);
			HttpResponse resp = client.execute(request);
			statusCode = resp.getStatusLine().getStatusCode();
			if ((statusCode / 100) != 2) {
				logger.error("Error with status code " + statusCode);
			} else {
				response = EntityUtils.toString(resp.getEntity());
				JSONObject mainObject = new JSONObject(response);
				if (mainObject.get("clusterInfo").equals(JSONObject.NULL)) {
					logger.error("No Cluster Info Returned.");
				} else {
					JSONObject clusterInfoObject = mainObject.getJSONObject("clusterInfo");
					if (clusterInfoObject == null) {
						logger.error("Cluster Info Object is Null.");
					} else {
						String state = clusterInfoObject.getString("state");
						if (state.equalsIgnoreCase("STARTED")) { // If Status Code is Started set sparkStatus to True.
							sparkStatus = "TRUE";
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Call to Cluster Failed :" + ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return sparkStatus;
	}

	private String getSparkHomePath() throws Exception {
		String sparkHome = System.getenv("SPARK_HOME");
		if (sparkHome == null) {
			throw new Exception("SPARK_HOME not set");
		}
		logger.info("SPARK_HOME variable path set to :" + sparkHome);

		if (sparkHome.endsWith("/")) {
			sparkHome = sparkHome.substring(0, sparkHome.length() - 1);
		}

		if (sparkHome.endsWith("bin")) {
			sparkHome = sparkHome.substring(0, sparkHome.length() - 4);
		}

		logger.info("SPARK_HOME:" + sparkHome);
		return sparkHome;
	}
}
