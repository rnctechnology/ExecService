package com.rnctech.nrdataservice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @contributor zilin
 * 2020.10
 */

public interface RNConsts {

	public final static String MR_URL = "rnctech.login.url";
	public final static String MR_USER = "rnctech.login.user";

	public final static String RUN_ID = "rnctech.runId";
	public final static String TXN_ID = "rnctech.transactionId";
	public static final String PASSWORD_SALT = "password.salt";

	public final static String javahome = "JAVA_HOME";
	public final static String sparkhome = "SPARK_HOME";
	public final static String hadoophome = "HADOOP_HOME";
	public final static String scalahome = "SCALA_HOME";
	public final static String pypath = "PYTHONPATH";

	public static final String API_PROTCOL = "api.protocol";  //http:// or https://
	public static final String API_PORT = "api.port";
	public static final String API_HOST = "api.host";
	public static final String API_USER = "api.user";
	public static final String API_PWD = "api.password";
	
	public static final String EXISTING_PROCESS = "existing_process";
	public static final int API_DEFAUlT_PORT = 10000;
	public static final int API_OUTPUT_LIMIT = 1024 * 100;
	public static final Map<String, TimeUnit> TIME_SUFFIXES = new HashMap<>();
	
	public static final String DEFAULT_PYTHON_CODESNAP = "import sys\nprint(sys.version)";
	public static final String DEFAULT_PYSPARK_CODESNAP = "sc.version";
	public static final String DEFAULT_MYSQL_CODESNAP = "SHOW VARIABLES LIKE \"%version%\";";
	public static final String DEFAULT_JAVA_CODESNAP = "sc.version";
	public static final String DEFAULT_SHELL_CODESNAP = "uname -a";
	
	public static final String JOB_PROC_ID = "processid";

	public static final String groupName = "RN-Group";
	public static final String bridgrGrpName = "RN-Bridge-Group";
	public static final String FCONFIG = "fconfig";	

	public static enum PROFILE_ENV {
		DEV, QA, PROD, PROD_DEV, PROD_QA, TEST
	}
	
	public static enum SCHEDULETYPE {
		cron, repeat, now, simple
	}
	
	public static String[] AWSINSTANCETYPE = {
			"r5.2xlarge","r5.4xlarge","r5.8xlarge","r5.16xlarge","x1.32xlarge"
	};
	
	/*adminjob and small job will run with service itself (32GB/4vCPU)
	 * medium - 64G/8vCPU(r5.2xlarge), large - 128G/16vCPU(r5.4xlarge), XLARGE - 256G/32vCPU(r5.8xlarge), XLARGE16 - 512G/64vCPU(r5.16xlarge), 
	 * XLARGE32 - x1.32xlarge(1,952GB/128vCPU) 
	 */
	public static enum LOADTYPE {
		ADMINJOB, SMALL, MEDIUM, LARGE, XLARGE, XLARGE16, XLARGE32
	}
	
	public static enum JOBTYPE {
		VALIDATE, SCRIPT, CLASS, AAJOB
	}

	public static enum TechType {
		python, spark, pyspark, sparkr, sparkql, sql, java, scala, shell
	}
	
	public static enum Algorithm {
		DecisionTree, RandomForest, GradientBoostedTree 
	}
	
	public static enum AlgorithmType {
		CUSTOM, BUILTIN, COMBINED
	}
	
	public static enum STATUS {
		PROGRESSING, CANCELLED, COMPLETED, COMPLETED_WITH_ERRORS, COMPLETED_WITH_WARNINGS, FAILED, INITIALIZED
	}
	
	public static enum PROCESSTYPE {
		local, container, remote
	}

	public static final Object[] synkey = new Object[0];
	public static final String LIVY_SPARK_SQL_FIELD_TRUNCATE = "livy.sparkql.field.truncate";
	
	public static class RNCTechImg {
		public static String imgName = "rnctech-AMI-PY35";
		public static String instanceid = "i-099999999999";
		public static String privateip = "10.0.0.8";
		public static String securitygroup = "DEV-VPN";
		public static String itype = "r5.xlarge";
		public static String vpc ="vpc-222222";
		public static String iam = "fullRole";
		public static String meminfo = "32GB";
		public static int cpu_no = 4;
		public static String cpuinfo="Intel(R) Xeon(R) Platinum 8175M CPU @ 2.50GHz";
		public static String sysinfo = "#103-Ubuntu SMP Tue Aug 27 10:21:48 UTC 2020";
		public static String pyinfo = "3.6.2";
		public static String jvminfo = "1.8.0_221";
		public static String execserviceinfo = "http://10.0.0.8:8082/exec";
		public static String pubkey = "pkeys"; 
		public final static String default_s3_key = "";
		public final static String default_s3_secret = "";
	}
}
