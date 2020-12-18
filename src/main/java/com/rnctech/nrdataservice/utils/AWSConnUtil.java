package com.rnctech.nrdataservice.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.util.EC2MetadataUtils;
import com.rnctech.nrdataservice.exception.RNBaseException;
import org.ini4j.Ini;

@SuppressWarnings("deprecation")
public class AWSConnUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(AWSConnUtil.class);    
	
    public static AmazonS3 getS3Connection()
                    throws RNBaseException {
        return new AmazonS3Client(new InstanceProfileCredentialsProvider(true));
    }
   
    public static AmazonSimpleDB getSimpleDBConnection() throws RNBaseException {
        return new AmazonSimpleDBClient(new InstanceProfileCredentialsProvider(true));
    }

    public static BasicAWSCredentials getCredentials() throws RNBaseException {
		InstanceProfileCredentialsProvider ipcp = new InstanceProfileCredentialsProvider(true);
		return new BasicAWSCredentials(ipcp.getCredentials().getAWSAccessKeyId(), ipcp.getCredentials().getAWSSecretKey());
    }
	
    public static String getRoleArn() throws FileNotFoundException {
        return EC2MetadataUtils.getIAMInstanceProfileInfo().instanceProfileArn;
    }
     public static String getRoleArnFromFile(String tenantName, String awsProfile) throws FileNotFoundException {
        logger.info("Trying to get the roleArn from credentials File");
        if(awsProfile != null)
            System.setProperty("aws.profile", awsProfile);

        Ini ini = null;
        try {
            if(System.getenv("AWS_CREDENTIAL_PROFILES_FILE")!=null){
            ini = new Ini(new FileInputStream(
                System.getenv("AWS_CREDENTIAL_PROFILES_FILE")));
            logger.info("Env variable found "+System.getenv("AWS_CREDENTIAL_PROFILES_FILE"));
            }
            else{
                ini = new Ini(new FileInputStream(
                System.getProperty("user.home") + "/.aws/credentials"));
                logger.info("Env variable not found and going with user home " + System.getProperty("user.home") );
            }
        } catch (IOException e) {
            logger.error("Unable to find roleArn from credentials file ");
            throw new FileNotFoundException("Unable to find roleArn from credentials file in ~/.aws");
        }

        logger.info("Role obtained from ini file  "+ ini.get(awsProfile, "aws_role_arn"));
        return ini.get(awsProfile, "aws_role_arn");
        
    }
     
    /**
     * @return the IP Address for this AWS instance.
     */
    public static String getInstancePrivateIpAddress(){
    	return EC2MetadataUtils.getInstanceInfo().getPrivateIp();
    }
}
