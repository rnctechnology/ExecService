package com.rnctech.nrdataservice.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedTransferQueue;
import org.slf4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.entity.ContentType;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.rnctech.nrdataservice.exception.RNBaseException;
import com.rnctech.nrdataservice.exception.ResourceException;
import com.rnctech.nrdataservice.utils.AWSConnUtil;
import com.rnctech.nrdataservice.utils.FileUtil;

/**
 * connector for AWS s3 storage
 * @author zilin 2020.09
 */

public class AWSResConnector extends CloudResConnector {
	
	private AmazonS3 s3client;
	private TransferManager transfermanager;
	private ConcurrentSkipListMap <String, File> filesToUpload = new ConcurrentSkipListMap <String, File> ();
	private LinkedTransferQueue <String> filesUploaded = new LinkedTransferQueue <String> ();
	private ConcurrentSkipListMap <String, InputStream> streamsToUpload = new ConcurrentSkipListMap <String, InputStream>();
	private LinkedTransferQueue <String> streamsUploaded = new LinkedTransferQueue <String> ();
	private ConcurrentSkipListMap <String, File> filesToDownload = new ConcurrentSkipListMap <String, File>();
	private LinkedTransferQueue <String> filesDownloaded = new LinkedTransferQueue <String> ();

	public AWSResConnector() {
		super();
	}
	
	public AWSResConnector(ConnectInfo connction) {
		super(connction);
	}

	public AWSResConnector(String httpurl, String username, String password) {
		super(httpurl, username, password);
	}

	public AWSResConnector(String httpurl, String urlpath, String username, String password, boolean encrypted) {
		super(httpurl, urlpath, username, password, encrypted);
	}
	
	@Override
	public void init() {
		super.init();
		try {
			if(null == connction || null == connction.key || null == connction.secret) {
				s3client = AWSConnUtil.getS3Connection();
			}else {
				BasicAWSCredentials awsCreds = new BasicAWSCredentials(connction.key, connction.secret);
				s3client = AmazonS3ClientBuilder.standard()
				                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).withRegion(getRegion())
				                        .build();
			}
		} catch (RNBaseException e) {
			logger.warn(e.getMessage());
			DefaultAWSCredentialsProviderChain providerchain = DefaultAWSCredentialsProviderChain.getInstance();
			s3client = AmazonS3ClientBuilder.standard().withCredentials(providerchain).withRegion(getRegion()).build();
		}
		
		transfermanager = TransferManagerBuilder.standard().withS3Client(s3client).build();
		try {
			createBucket();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}
	
	private Regions getRegion() {
		Regions r= Regions.DEFAULT_REGION;
		try {
			if(null != connction.region) r = Regions.valueOf(connction.region);
		}catch(Exception e) {}
		return r;
	}
	
	private boolean createBucket() throws Exception {
		boolean result = !s3client.doesBucketExistV2(connction.basename);
		if (result) {
                s3client.createBucket(new CreateBucketRequest(connction.basename));
                String bucketLocation = s3client.getBucketLocation(new GetBucketLocationRequest(connction.basename));
                logger.debug("Bucket craeted at location: " + bucketLocation);
                result = true;
		}
		return result;
	}

	@Override
	ResourceSet getAllResources() {
		return null;
	}

	@Override
	public Object readResource(ResourceId id) {
		logger.info("load resource " + id.getName() + " of type " + id.getSourceType());
		String keyname = id.getName(); //id.getSourceType().toLowerCase() + '/' + id.getName();
		try {
			if (id.isTextStyle()) {
				return downloadTextFileAsString(keyname);
			}
			File tmpf = null;
			tmpf = FileUtil.createTempFile(id.getName(), null);
			downloadFile(keyname, tmpf);
			return tmpf;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}
	
	@Override
	public Object readResource(ResourceId id, String methodName, Object[] params) {
		return null;
	}
	
	@Override
	public void writeResource(ResourceId id, Object obj) throws ResourceException {
		super.writeResource(id, obj);
		try {
			String keyname = id.getSourceType().toLowerCase()+'/'+id.getName();
			if(obj instanceof File) {
				uploadFile(keyname, ((File)obj));
			}else if(obj instanceof InputStream){
				InputStream is = (InputStream)obj;
				String contentType = ContentType.DEFAULT_BINARY.getMimeType();			
				uploadStream(keyname, is, -1, contentType);
			}else {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream); 
			    objectOutputStream.writeObject(obj);
			    byte[] serializedObjectToSave = outputStream.toByteArray();

			   ByteArrayInputStream objectAsInputStream = new ByteArrayInputStream(serializedObjectToSave);
			   String contentType = ContentType.DEFAULT_BINARY.getMimeType();
			   
			   uploadStream(keyname, objectAsInputStream, serializedObjectToSave.length, contentType);
			   
/*	        ObjectMetadata objectMetadata = new ObjectMetadata();
			    objectMetadata.setContentLength(serializedObjectToSave.length);
			    objectMetadata.setContentType(contentType);
			    s3client.putObject(new PutObjectRequest(connction.basename, connction.name + '/' + keyname, objectAsInputStream, objectMetadata));*/
			
			}
		} catch (Exception e) {
			throw new ResourceException(e);
		}
	}
	
	@Override
	public void removeResource(ResourceId id) throws ResourceException {
		super.removeResource(id);
		try {
			String keyname = connction.name + '/'+id.getSourceType().toLowerCase()+'/'+id.getName();
			s3client.deleteObject(new DeleteObjectRequest(connction.basename, keyname));
		} catch (Exception e) {
			throw new ResourceException(e);
		}
	}
	
	public void uploadFile (String keyName, File fileToUpload) throws Exception {
		Upload currentUpload = transfermanager.upload(connction.basename, connction.name + '/' + keyName, fileToUpload);
		filesToUpload.put(keyName, fileToUpload);		
		currentUpload.addProgressListener(new TransferCleaner(logger, keyName, filesUploaded));
	}

	public void uploadStream(String keyName, InputStream inputStream, long contentLength, String contentType) throws Exception {
		ObjectMetadata objectMetadata = new ObjectMetadata ();
		if(contentLength > 0)objectMetadata.setContentLength(contentLength);
		objectMetadata.setContentType(contentType);
		Upload currentUpload = transfermanager.upload(connction.basename, connction.name + '/' + keyName, inputStream, objectMetadata);
		streamsToUpload.put(keyName, inputStream);		
		currentUpload.addProgressListener(new TransferCleaner(logger, keyName, streamsUploaded));
	}

	public void downloadFile(String keyName, File fileToDownload) throws Exception {
		Download currentDownload = transfermanager.download(connction.basename, connction.name + '/' + keyName, fileToDownload);
		filesToDownload.put(keyName, fileToDownload);
		currentDownload.addProgressListener(new TransferCleaner(logger, keyName, filesDownloaded));
	}
	
	public String downloadTextFileAsString(String keyName) throws Exception {
		S3Object s3object = null;
		s3object = s3client.getObject(new GetObjectRequest(connction.basename, connction.name + '/' + keyName));
		InputStream inputStream = s3object.getObjectContent();
		return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
	}
	
	public String downloadTextFileAsStringFile(String keyName) {
		File tmpfile = null;
		try {
			tmpfile = FileUtil.createTempFile(keyName, "txt");
		} catch (IOException e1) {
			logger.error(e1.getMessage());
		}
		
		try {
			InputStream inputStream = new FileInputStream(tmpfile);
			downloadFile(keyName, tmpfile);
			completeTransfer();
			return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;
	}
    
	@Override
	public void close() {
		super.close();	
		abortTransfer();
		transfermanager.shutdownNow();
	}
	
	public void abortTransfer() {
		int size = filesToDownload.size() + filesToUpload.size() + streamsToUpload.size();
		if (size > 0) {
			transfermanager.abortMultipartUploads(connction.basename, new Date(System.currentTimeMillis()));
		}
	}
	
	public Map<String, String> listObjects(String prefix) throws Exception {		
		ObjectListing objectListing;
		if (null == prefix) prefix = connction.name;

		Map<String, String> entries = new HashMap<>();
		int i = 0;
		do {
			objectListing = s3client
					.listObjects(new ListObjectsRequest().withBucketName(connction.basename).withPrefix(prefix));
			for (S3ObjectSummary obSummary : objectListing.getObjectSummaries()) {
				String key = obSummary.getKey();
					String details = obSummary.getLastModified() + "\t"
							+ obSummary.getOwner().getDisplayName() +" "+ obSummary.getSize();
					entries.put(key, details);
					i++;
			}
		} while (i <= 10000 && objectListing.isTruncated());
		return entries;
	}
	
	private String takeTransferredKey (Set <String> keysToBeTransferred, LinkedTransferQueue <String>  keysTransferred, boolean blocking)  throws Exception {
		String result = null;
		if (keysToBeTransferred.size() > 0) {
			result = blocking ? keysTransferred.take() : keysTransferred.poll();
		}
		return result;
	}

	public File takeDownloadedFileNonBlocking () throws Exception {
		Set <String> keySet = filesToDownload.keySet();
		String transferredKey = takeTransferredKey(keySet, filesDownloaded, false);
		return (transferredKey != null) ? filesToDownload.remove(transferredKey) : null;
	}

	public File takeUploadedFileNonBlocking () throws Exception {
		Set <String> keySet = filesToUpload.keySet();
		String transferredKey = takeTransferredKey(keySet, filesUploaded, false);
		return (transferredKey != null) ? filesToUpload.remove(transferredKey) : null;
	}

	public InputStream takeUploadedStreamNonBlocking () throws Exception {
		Set <String> keySet = streamsToUpload.keySet();
		String transferredKey = takeTransferredKey(keySet, streamsUploaded, false);
		return (transferredKey != null) ? streamsToUpload.remove(transferredKey) : null;
	}

	public File takeDownloadedFileBlocking () throws Exception {
		Set <String> keySet = filesToDownload.keySet();
		String transferredKey = takeTransferredKey(keySet, filesDownloaded, true);
		return (transferredKey != null) ? filesToDownload.remove(transferredKey) : null;
	}

	public File takeUploadedFileBlocking () throws Exception {
		Set <String> keySet = filesToUpload.keySet();
		String transferredKey = takeTransferredKey(keySet, filesUploaded, true);
		return (transferredKey != null) ? filesToUpload.remove(transferredKey) : null;
	}

	public InputStream takeUploadedStreamBlocking () throws Exception {
		Set <String> keySet = streamsToUpload.keySet();
		String transferredKey = takeTransferredKey(keySet, streamsUploaded, true);
		return (transferredKey != null) ? streamsToUpload.remove(transferredKey) : null;
	}
	
	public void completeTransfer () throws Exception {
		int size = filesToDownload.size() + filesToUpload.size() + streamsToUpload.size();
		while (size > 0) {
			takeDownloadedFileNonBlocking();
			takeUploadedFileNonBlocking();
			takeUploadedStreamNonBlocking();
			size = filesToDownload.size() + filesToUpload.size() + streamsToUpload.size();
			Thread.sleep (RandomUtils.nextInt(4096, 8192));			
		}
		filesToDownload.clear();
		filesDownloaded.clear();
		streamsToUpload.clear();
		streamsUploaded.clear();
		filesToUpload.clear();
		filesUploaded.clear();
	}
	
/*	private void lifeCycleRule(int expireAfterDays) throws Exception {
		BucketLifecycleConfiguration bucketLifecycleConfiguration = s3client.getBucketLifecycleConfiguration(connction.basename);
		if (bucketLifecycleConfiguration == null) bucketLifecycleConfiguration = new BucketLifecycleConfiguration();
		if (bucketLifecycleConfiguration.getRules() == null) bucketLifecycleConfiguration.setRules (new ArrayList<BucketLifecycleConfiguration.Rule>());
		boolean addRule = true;
		for (Rule rule : bucketLifecycleConfiguration.getRules()) {
			if (StringUtils.startsWith(rule.getPrefix(), connction.name)) {
				addRule = false;
				break;
			}
		}
		
		if (addRule) {
			BucketLifecycleConfiguration.Rule expireRule = new BucketLifecycleConfiguration.Rule().withId(connction.name + " delete rule");
			bucketLifecycleConfiguration.getRules().add(expireRule.withPrefix(connction.name).withExpirationInDays(expireAfterDays).withStatus(BucketLifecycleConfiguration.ENABLED.toString()));
			s3client.setBucketLifecycleConfiguration(connction.basename, bucketLifecycleConfiguration);
		}
	}*/
	
	private static final class TransferCleaner implements ProgressListener {
		private String key;
		private Queue <String> streamsToBeTransferred;
		private Logger logger;

		public TransferCleaner(Logger logger, String key, Queue <String> streamsToBeTransferred) {
			this.logger = logger;
			this.key = key;
			this.streamsToBeTransferred = streamsToBeTransferred;
		}

		@Override
		public void progressChanged(ProgressEvent arg0) {
			String event;
			switch (arg0.getEventType()) {
			case TRANSFER_FAILED_EVENT:
				event = "failed";
				break;
			case TRANSFER_CANCELED_EVENT:
				event = "canceled";
				break;
			case TRANSFER_COMPLETED_EVENT:
				event = "completed";
				break;
			default:
				event = "progress";
				break;
			}
			String eventDiagnostic = "Received " + event + " event for key " + key;
			if (event.equals("progress")) {
				eventDiagnostic += " Bytes transferred " + arg0.getBytesTransferred();
			} else {
				if (event.equals("completed")) {
					logger.debug("Completed and "+eventDiagnostic);
				} else {
					logger.error("Failed or Canceled after "+eventDiagnostic);
				}
				streamsToBeTransferred.add(key);
			}
		}		
	}

}
