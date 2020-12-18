package com.rnctech.nrdataservice.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.rnctech.nrdataservice.config.FileConfig;
import com.rnctech.nrdataservice.exception.FileStorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zilin Chen
 * @since 2020.09
 */

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private boolean backups3 = false;
    public static Logger logger = Logger.getLogger(FileStorageService.class);
    		
    @Autowired
    AWSStorageService awsStorageService;

    @Autowired
    public FileStorageService(FileConfig fileconfig) throws FileStorageException {
        this.fileStorageLocation = Paths.get(fileconfig.getUploadDir())
                .toAbsolutePath().normalize();
        this.backups3 = fileconfig.isBackups3();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    
    public List<String> listUpload(String ext) throws FileStorageException{
    	ArrayList<String> fs = new ArrayList<>();
    	try {
    		if(null == ext) {
    			Files.list(this.fileStorageLocation).filter(Files::isRegularFile).forEach(f -> fs.add(f.getFileName().toString()));
    		}else {
    			Files.list(this.fileStorageLocation).filter(p -> Files.isRegularFile(p) && p.toString().endsWith(ext)).forEach(f -> fs.add(f.getFileName().toString()));
    		}
    	} catch (Exception ex) {
    		throw new FileStorageException("Could not list the directory of "+fileStorageLocation, ex);
        }
    	return fs;
    }

    public String storeFile(MultipartFile file) throws FileStorageException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            
            if(this.backups3) {
				new Thread(() -> {
				try {
					awsStorageService.storeFile(file);
				} catch (Throwable t) {
					logger.warn("backup to s3 failed "+t.getMessage());
				}
				}).start();
            	
            }
            
            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) throws Exception {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
            	resource = awsStorageService.loadFileAsResource(fileName);
            	if(resource.exists()) {
                    return resource;
                } else {
                	 throw new Exception("File not found " + fileName);
                }
            }
        } catch (Exception ex) {
            throw new Exception("File not found " + fileName, ex);
        }
    }
}