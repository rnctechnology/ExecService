package com.rnctech.nrdataservice.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.rnctech.nrdataservice.config.FileConfig;
import com.rnctech.nrdataservice.exception.FileStorageException;
import com.rnctech.nrdataservice.resource.AWSResConnector;
import com.rnctech.nrdataservice.resource.ConnectInfo;
import com.rnctech.nrdataservice.resource.JResource;
import com.rnctech.nrdataservice.resource.PathResourcePool;
import com.rnctech.nrdataservice.resource.Resource.SourceType;
import com.rnctech.nrdataservice.resource.ResourceId;
import com.rnctech.nrdataservice.resource.ResourcePool;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * @author Zilin Chen
 * @since 2020.09
 */

@Service
public class AWSStorageService {
	
	private static ResourcePool rpool;
	private static ConnectInfo connection;
	private AWSResConnector resconnector;
	
	public static Logger logger = Logger.getLogger(AWSStorageService.class);
			
    @Autowired
    public AWSStorageService(FileConfig fileconfig) throws FileStorageException {
		connection = new ConnectInfo(fileconfig.getBucketname(), fileconfig.getBasefolder(), fileconfig.getKey(), fileconfig.getScret(), fileconfig.getRegion(), false, true);
		resconnector = new AWSResConnector(connection);
		resconnector.init();
		rpool = new PathResourcePool(resconnector, fileconfig.getUploadDir());
    }

    public String storeFile(MultipartFile file) throws FileStorageException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            
            String sourcetype = getSourceType(fileName);            
            String keyname = sourcetype+'/'+fileName;
            resconnector.uploadStream(keyname, file.getInputStream(), file.getSize(), file.getContentType());
            return fileName;
        } catch (Exception ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) throws Exception {
        try {
        	String id = ResourceId.generateId(fileName);
        	String sourcetype = getSourceType(fileName);
        	Object obj = rpool.get(id, sourcetype, fileName).get();
            if(null != obj) {
            	if(obj instanceof InputStream)
            		return new InputStreamResource((InputStream)obj);
            	if(obj instanceof String)
            		return new InMemoryResource((String)obj);
            	if(obj instanceof File)           
            		return new FileSystemResource((File)obj);
            	return new ByteArrayResource(JResource.toByteArray(obj));
            } else {
                throw new FileNotFoundException("File not found " + fileName);
            }
        } catch (Exception ex) {
            throw new Exception("File not found " + fileName, ex);
        }
    }
    
    private String getSourceType(String fileName) {
        String sourcetype = SourceType.py.name();
        try {
			String fileext = fileName.substring(fileName.lastIndexOf(".")+1);
			sourcetype = SourceType.valueOf(fileext.trim().toLowerCase()).name();
		} catch (Exception e) {
		}
        return sourcetype;
    }
}