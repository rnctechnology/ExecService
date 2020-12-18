This project is target to provide execution service through RestApis 
such as 1, run Java Jar, Python, SQL queries locally.
        2, run pyspack / spark / sparkQL on Spark Cluster
        3, run sqoop etc tool import data into Hadoop Cluster  


The service as MicroService, which you can deploy in Cloud (GCP / AWS etc.)
And using Rest calls from locally browser to execute remote jobs on the CLoud Hadoop / Spark Cluster 
or Cloud Instance

1, Run service at specified port

java -jar execservice.jar --server.port=8088

2, Send execution payload which type as below: 
Python, Java, SQL, pySpark, Spark, SparkQL, Sqoop 

3, set logger level to debug at runtime

PUT
http://localhost:8082/exec/logger/DEBUG

4, tools for encrypt text for configuration in application.yml
	cd to folder of platform/JobExecutionService and run below maven command

	mvn test -Dtest=com.rnctech.nrdataservice.test.TestAuth#testEnc -Dauthurl=ldap://.....com -Dauthuser=uid=a...m -Dauthpwd=yourpwd -Dauthtenant=confirmtenant -Dauthtext=yourtext | grep "TestAuth encrypted as:"	

