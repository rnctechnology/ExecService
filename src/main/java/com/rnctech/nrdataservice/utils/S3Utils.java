package com.rnctech.nrdataservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.rnctech.nrdataservice.common.Messages;
import com.rnctech.nrdataservice.exception.RNBaseException;

/**
 * @author zilin chen
 * @since 2020.09
 */

public class S3Utils {

	private static Logger logger = LoggerFactory.getLogger(S3Utils.class);

	private String accessKey;
	private String secretKey;
	private String bucketName;

	public S3Utils() {
	}

	public S3Utils(String bucketName) {
		this.bucketName = bucketName;
	}

	public S3Utils(String accessKey, String secretKey, String bucketName) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.bucketName = bucketName;
	}

	public void upload(AmazonS3 s3Client, String directoryName, String fileName, String filePath) {
		uploadToS3(s3Client, directoryName, fileName, filePath);
	}

	public void upload(String directoryName, String fileName, String filePath) {
		AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
		uploadToS3(s3Client, directoryName, fileName, filePath);
	}

	public void uploadDirectory(AmazonS3 s3Client, String directoryName, String filePath) throws Exception {
		uploadDirectoryToS3(s3Client, directoryName, filePath);
	}

	private void uploadDirectoryToS3(AmazonS3 s3Client, String directoryName, String filePath) throws Exception {
		File file = new File(filePath);
		TransferManager tm = new TransferManager(s3Client);
		MultipleFileUpload upload = tm.uploadDirectory(bucketName, directoryName, file, true);

		try {
			upload.waitForCompletion();
		} catch (Exception e) {
			logger.error("Uploading file to s3 failed");
			throw e;
		}

		tm.shutdownNow();
		logger.info("Files uploaded to S3 location");
	}

	private void uploadToS3(AmazonS3 s3Client, String directoryName, String fileName, String filePath) {
		String keyName = directoryName + "/" + fileName;
		List<PartETag> partETags = new ArrayList<PartETag>();
		InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
		InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

		File file = new File(filePath);
		long contentLength = file.length();
		long partSize = 5242880;
		logger.info("Started Uploading to AWS S3");

		try {
			long filePosition = 0;
			for (int i = 1; filePosition < contentLength; i++) {
				partSize = Math.min(partSize, (contentLength - filePosition));
				UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName).withKey(keyName)
						.withUploadId(initResponse.getUploadId()).withPartNumber(i).withFileOffset(filePosition)
						.withFile(file).withPartSize(partSize);

				partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());
				filePosition += partSize;
			}

			CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
					initResponse.getUploadId(), partETags);

			s3Client.completeMultipartUpload(compRequest);
		} catch (Exception e) {
			s3Client.abortMultipartUpload(
					new AbortMultipartUploadRequest(bucketName, keyName, initResponse.getUploadId()));
			logger.error("Uploading file to s3 failed");
			throw e;
		}

		logger.info("File is successfully uploaded to S3 location");
	}

	public void download(String directoryName, String fileName, String filePath) throws IOException {
		FileOutputStream fileOutputStream = null;
		AmazonS3 s3Client = null;
		try {
			s3Client = AWSConnUtil.getS3Connection();
		} catch (RNBaseException e) {
			logger.error(e.getMessage());
		}

		String keyName = directoryName + "/" + fileName;
		GetObjectRequest request = new GetObjectRequest(bucketName, keyName);
		S3Object object = s3Client.getObject(request);
		S3ObjectInputStream objectContent = object.getObjectContent();
		fileOutputStream = new FileOutputStream(filePath);
		IOUtils.copy(objectContent, fileOutputStream);
		fileOutputStream.close();
	}

	public static File downloadArtifactsFromS3(String bucketName, String s3KeyName) throws Exception {
		File downloadedFile = null;
		AmazonS3 s3client = null;
		try {
			s3client = AWSConnUtil.getS3Connection();
		} catch (RNBaseException e) {
			logger.error(e.getMessage());
		}

		try {
			logger.info("Downloading artifacts from S3");
			TransferManager transferManager = new TransferManager(s3client);
			File dir = new File("destDir");

			MultipleFileDownload download = transferManager.downloadDirectory(bucketName, s3KeyName, dir);
			download.waitForCompletion();
			List<File> fileList = new ArrayList<File>();
			getAllFiles(dir, fileList);
			String zipFileLocation = writeZipFile(dir, fileList);

			if (zipFileLocation != null) {
				downloadedFile = new File(zipFileLocation);
			}

			transferManager.shutdownNow();

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
		return downloadedFile;
	}

	public static void writeStringToS3(String bucketName, String keyName, String pySparkScript)
			throws UnsupportedEncodingException {
		AmazonS3 s3client = null;
		try {
			s3client = AWSConnUtil.getS3Connection();
		} catch (RNBaseException e) {
			logger.error(e.getMessage());
		}

		try {
			logger.info("Uploading file to S3");
			byte[] data = pySparkScript.getBytes("US-ASCII");
			ObjectMetadata om = new ObjectMetadata();
			om.setContentLength(data.length);
			om.setContentType("text/plain");
			PutObjectRequest request = new PutObjectRequest(bucketName, keyName, new ByteArrayInputStream(data), om);
			PutObjectResult response = s3client.putObject(request);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}

	public void uploadFile(File file, AmazonS3 s3client, String bucketName, String keyName) throws IOException {
		long contentLength = file.length();
		long partSize = 5 * 1024 * 1024;

		try {
			List<PartETag> partETags = new ArrayList<PartETag>();
			InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
			InitiateMultipartUploadResult initResponse = s3client.initiateMultipartUpload(initRequest);
			
			long filePosition = 0;
			for (int i = 1; filePosition < contentLength; i++) {
				partSize = Math.min(partSize, (contentLength - filePosition));
				UploadPartRequest uploadRequest = new UploadPartRequest().withBucketName(bucketName).withKey(keyName)
						.withUploadId(initResponse.getUploadId()).withPartNumber(i).withFileOffset(filePosition)
						.withFile(file).withPartSize(partSize);

				UploadPartResult uploadResult = s3client.uploadPart(uploadRequest);
				partETags.add(uploadResult.getPartETag());
				filePosition += partSize;
			}

			CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
					initResponse.getUploadId(), partETags);
			s3client.completeMultipartUpload(compRequest);
		} catch (SdkClientException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private static void getAllFiles(File dir, List<File> fileList) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				fileList.add(file);
				if (file.isDirectory()) {
					getAllFiles(file, fileList);
				}
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private static String writeZipFile(File directoryToZip, List<File> fileList) {

		try {
			String zipFileNameWithLocation = System.getProperty("user.dir") + "/" + directoryToZip.getName() + ".zip";
			FileOutputStream fos = new FileOutputStream(directoryToZip.getName() + ".zip");
			ZipOutputStream zos = new ZipOutputStream(fos);

			for (File file : fileList) {
				if (!file.isDirectory()) {
					addToZip(directoryToZip, file, zos);
				}
			}
			zos.close();
			fos.close();
			return zipFileNameWithLocation;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			return null;
		}
	}

	private static void addToZip(File directoryToZip, File file, ZipOutputStream zos)
			throws FileNotFoundException, IOException {

		FileInputStream fis = new FileInputStream(file);
		String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
				file.getCanonicalPath().length());

		ZipEntry zipEntry = new ZipEntry(zipFilePath);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}

}