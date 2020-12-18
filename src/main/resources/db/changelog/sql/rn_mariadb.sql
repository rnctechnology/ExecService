-- author: alan.chen@rnctech.com at 2020.07

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
use rnjob;

DROP TABLE IF EXISTS `job_run`;
DROP TABLE IF EXISTS `job_policy`;
DROP TABLE IF EXISTS `job_resource`;
DROP TABLE IF EXISTS `job_workflow`;
DROP TABLE IF EXISTS `job_detail`;
DROP TABLE IF EXISTS `job`;

/*!40101 SET @saved_cs_client=@@character_set_client */;
/*!40101 SET character_set_client=utf8 */;

CREATE TABLE `job` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL,  
  `jobname` varchar(255) NOT NULL,
  `tenant` varchar(64) DEFAULT NULL,
  `jobid` bigint(20) DEFAULT NULL,
  `status` int(5) DEFAULT '-1',
  `execution_info` text,
  `config_url` varchar(255) DEFAULT NULL,
  `configuser` varchar(255) DEFAULT NULL,
  `configpassword` varchar(255) DEFAULT NULL,
  `local_job` bit(1) DEFAULT b'0',
  `job_group` varchar(32) NOT NULL,  
  `active` bit(1) DEFAULT b'0',  
  `jobtype` varchar(32) DEFAULT NULL,
  `job_executed_by` varchar(255) DEFAULT '127.0.0.1' NOT NULL,
  `job_status_checked` datetime DEFAULT NULL,  
  `jobkey` varchar(255) DEFAULT '000000' NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `envname` varchar(64) DEFAULT NULL,
  `instance_type` varchar(64) DEFAULT 'DEV' NOT NULL,
  `mrversion` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_JOB` (`job_executed_by`, `jobkey`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

CREATE TABLE `job_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL,  
  `jobname` varchar(255) NOT NULL,
  `jobid` bigint(20) NOT NULL,
  `sessionid` int DEFAULT '-1',
  `statementid` int DEFAULT '-1',
  `appid` varchar(128) DEFAULT NULL,
  `status` int(5) DEFAULT '-1',
  `tenant` varchar(32) DEFAULT NULL,
  `source_system_name` varchar(32) DEFAULT NULL,
  `load_type` varchar(32) DEFAULT NULL,
  `job_type` varchar(32) DEFAULT NULL,
  `script_type` varchar(32) DEFAULT NULL,
  `code_snap` text,
  `executable` varchar(255) DEFAULT NULL,
  `spark_url` varchar(255) DEFAULT NULL,
  `deploy_mode` varchar(32) DEFAULT NULL,  
  `job_properties` text,
  `params` text,  
  `libraries` text,
  `zips` blob,
  `active` bit(1) DEFAULT b'1',  
  `version` bigint(20) DEFAULT 0 NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_JOBDETAIL` FOREIGN KEY (`jobid`) REFERENCES `job` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;

CREATE TABLE `job_run` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL,  
  `jobdid` bigint(20) NOT NULL,
  `jobStart` datetime DEFAULT NULL,  
  `jobEnd` datetime DEFAULT NULL,  
  `sessionid` bigint(20) NOT NULL,
  `result` text,
  `loginfo` text,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_JOBRUN` FOREIGN KEY (`jobdid`) REFERENCES `job_detail` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8;

CREATE TABLE `job_policy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL, 
  `jobid` bigint(20) NOT NULL,
  `isschedule` bit(1) DEFAULT b'0',
  `scheduletype` varchar(255) NOT NULL,
  `frequency` bigint(20) DEFAULT NULL,
  `exectype` int(5) DEFAULT '-1',
  `cronexpr` varchar(255) DEFAULT NULL,
  `delay` int(6) DEFAULT '-1',
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_JOBPOLICY` FOREIGN KEY (`jobid`) REFERENCES `job` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

CREATE TABLE `job_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL,  
  `active` bit(1) NOT NULL,
  `resname` varchar(255) DEFAULT NULL,
  `restype` varchar(31) DEFAULT NULL,  
  `conntype` varchar(31) DEFAULT NULL,
  `uri` varchar(255) DEFAULT NULL,
  `basename` varchar(255) DEFAULT NULL,
  `srckey` varchar(255) DEFAULT NULL,
  `srcsecret` varchar(255) DEFAULT NULL,
  `region` varchar(255) DEFAULT NULL,
  `encrypted` bit(1) DEFAULT b'0', 
  `poolres` bit(1) DEFAULT b'1', 
  `poolloc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `job_workflow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime DEFAULT NULL,
  `createdby` varchar(255) DEFAULT NULL,
  `last_modified` datetime DEFAULT NULL,
  `updatedby` varchar(255) DEFAULT NULL,  
  `jobname` varchar(255) NOT NULL,
  `jobid` bigint(20) NOT NULL,
  `job_group` varchar(255) DEFAULT NULL,
  `parent` bigint(20) NOT NULL,
  `childjobs` bigint(20) NOT NULL,
  `previous` bigint(20) NOT NULL,
  `next` bigint(20) NOT NULL,  
  `parallel` bit(1) DEFAULT NULL,
  `schedule` bit(1) DEFAULT NULL,
  `currentOn` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_JOBWORKFLOW` FOREIGN KEY (`jobid`) REFERENCES `job` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

