package com.rnctech.nrdataservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.services.s3.model.Region;

/**
 * @author Zilin Chen
 * @since 2020.09
 */

@Configuration
@ConfigurationProperties(prefix = "file")
public class FileConfig {
    private String uploadDir;
    private boolean backups3;
    private String bucketname;
    private String basefolder;
    private String key;
    private String scret;
    private String region = Region.US_West_2.name();
	
    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

	public boolean isBackups3() {
		return backups3;
	}

	public void setBackups3(boolean backups3) {
		this.backups3 = backups3;
	}

	public String getBucketname() {
		return bucketname;
	}

	public void setBucketname(String bucketname) {
		this.bucketname = bucketname;
	}

	public String getBasefolder() {
		return basefolder;
	}

	public void setBasefolder(String basefolder) {
		this.basefolder = basefolder;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getScret() {
		return scret;
	}

	public void setScret(String scret) {
		this.scret = scret;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
}
