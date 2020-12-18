package com.rnctech.nrdataservice.utils;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.MonitorInstancesRequest;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest;
import com.rnctech.nrdataservice.RNConsts.RNCTechImg;
import com.rnctech.nrdataservice.exception.RNBaseException;

/**
 * AWS utility to provision new instance, stop/remove instance
 * 
 * @author zilin chen
 * @since 2020.07
 */

public class AWSUtil {


	private static AWSCredentials credentials;

	static {
		try {
			credentials = AWSConnUtil.getCredentials();
		} catch (RNBaseException e) {
			String key = System.getProperty("S3_Key",RNCTechImg.default_s3_key);
			String secret = System.getProperty("S3.secret",RNCTechImg.default_s3_secret);
			credentials = new BasicAWSCredentials(key, secret);
		}
	}

	public static AmazonEC2 getEC2Client() {
		AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_WEST_2).build();
		return ec2Client;
	}

	public static String createSecurityGroup(AmazonEC2 ec2Client, String gname, String desc) {
		CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest().withGroupName(gname)
				.withDescription(desc);
		CreateSecurityGroupResult csgr = ec2Client.createSecurityGroup(createSecurityGroupRequest);

		// Allow HTTP and SSH traffic
		IpRange ipRange1 = new IpRange().withCidrIp("0.0.0.0/0");

		IpPermission ipPermission1 = new IpPermission().withIpv4Ranges(Arrays.asList(new IpRange[] { ipRange1 }))
				.withIpProtocol("tcp").withFromPort(8082).withToPort(8082);

		IpPermission ipPermission2 = new IpPermission().withIpv4Ranges(Arrays.asList(new IpRange[] { ipRange1 }))
				.withIpProtocol("tcp").withFromPort(22).withToPort(22);

		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest()
				.withGroupName(gname).withIpPermissions(ipPermission1, ipPermission2);

		AuthorizeSecurityGroupIngressResult asgir = ec2Client
				.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		return csgr.getGroupId();
	}

	public static String createKeyPair(AmazonEC2 ec2Client, String kname, String desc) throws Exception {

		CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest().withKeyName(kname);
		CreateKeyPairResult createKeyPairResult = ec2Client.createKeyPair(createKeyPairRequest);
		
		KeyPair kp = createKeyPairResult.getKeyPair();
		String privateKey = kp.getKeyMaterial(); 
		File tempFile = new File(FileUtils.getTempDirectory(), kp.getKeyName().replaceAll(" ", "_")+UUID.randomUUID() + ".tmp");
		FileUtils.writeStringToFile(tempFile, privateKey);

		DescribeKeyPairsRequest describeKeyPairsRequest = new DescribeKeyPairsRequest();
		DescribeKeyPairsResult describeKeyPairsResult = ec2Client.describeKeyPairs(describeKeyPairsRequest);
		return describeKeyPairsResult.getKeyPairs().get(0).getKeyName();
	}

	public static String describeKeyPair(AmazonEC2 ec2Client, String kname) throws Exception {
		try {
			DescribeKeyPairsResult result;
			if (null != kname) {
				DescribeKeyPairsRequest dkp = new DescribeKeyPairsRequest().withKeyNames(kname);
				result = ec2Client.describeKeyPairs(dkp);
			} else {
				result = ec2Client.describeKeyPairs();
			}
			return result.getKeyPairs().get(0).toString();
		} catch (Exception e) {
			throw e;
		}

	}

	public static Instance reqestInstance(AmazonEC2 ec2Client, String imgid, String kname, String gname,
			String ec2type) {
		// Launch an Amazon Instance with imgid such as "ami-97785bed" 
		//https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html
		//https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/usingsharedamis-finding.html
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest().withImageId(imgid) 
				.withInstanceType(ec2type) 
				.withMinCount(1).withMaxCount(1).withKeyName(kname) 
				.withSecurityGroups(gname);

		Instance instance = ec2Client.runInstances(runInstancesRequest).getReservation().getInstances().get(0);

		return instance;
	}

	public static String startInstance(AmazonEC2 ec2Client, String instanceId) {
		StartInstancesRequest startInstancesRequest = new StartInstancesRequest().withInstanceIds(instanceId);
		ec2Client.startInstances(startInstancesRequest);
		return instanceId;

	}

	public static String startInstance(AmazonEC2 ec2Client, String imgid, String kname, String gname, String ec2type) {

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest().withImageId(imgid) 
				.withInstanceType(ec2type)  	// https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-types.html
				.withMinCount(1).withMaxCount(1).withKeyName(kname) 
				.withSecurityGroups(gname);

		String yourInstanceId = ec2Client.runInstances(runInstancesRequest).getReservation().getInstances().get(0)
				.getInstanceId();

		// Start an Instance
		StartInstancesRequest startInstancesRequest = new StartInstancesRequest().withInstanceIds(yourInstanceId);

		ec2Client.startInstances(startInstancesRequest);
		return yourInstanceId;
	}

	public static void monitorInstance(AmazonEC2 ec2Client, String yourInstanceId) {

		MonitorInstancesRequest monitorInstancesRequest = new MonitorInstancesRequest().withInstanceIds(yourInstanceId);

		ec2Client.monitorInstances(monitorInstancesRequest);

		UnmonitorInstancesRequest unmonitorInstancesRequest = new UnmonitorInstancesRequest()
				.withInstanceIds(yourInstanceId);

		ec2Client.unmonitorInstances(unmonitorInstancesRequest);
		RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest().withInstanceIds(yourInstanceId);

		ec2Client.rebootInstances(rebootInstancesRequest);
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		DescribeInstancesResult response = ec2Client.describeInstances(describeInstancesRequest);

		System.out.println(response.getReservations().get(0).getInstances().get(0).getKernelId());
	}

	public static void stopInstance(AmazonEC2 ec2Client, String yourInstanceId) {
		StopInstancesRequest stopInstancesRequest = new StopInstancesRequest().withInstanceIds(yourInstanceId);

		ec2Client.stopInstances(stopInstancesRequest).getStoppingInstances().get(0).getPreviousState().getName();

	}

}
