package com.rnctech.nrdataservice.job;

import java.util.Map;
import com.rnctech.nrdataservice.RNConsts.JOBTYPE;
import com.rnctech.nrdataservice.RNConsts.LOADTYPE;
import com.rnctech.nrdataservice.RNConsts.TechType;

/**
 * @author Zilin Chen
 * @since 2020.10
 *
 */

public class JobConfig {
	String name = null;
	String sourceName = null;
	String loadType = LOADTYPE.SMALL.name();
	String jobType = JOBTYPE.SCRIPT.name();
	String scriptType = TechType.pyspark.name();
	String codeSnap;
	String executable;
	String sparkURL;
	String deployMode;
	Map<String, String> jobProperties = null;
	Map<String, String> params = null;
	Map<String, String> libraries = null;

	public String toString() {
		return "name=" + name + " jobType=" + jobType + " scriptType=" + scriptType + " codeSnap=" + codeSnap;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getLoadType() {
		return loadType;
	}

	public void setLoadType(String loadType) {
		this.loadType = loadType;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getScriptType() {
		return scriptType;
	}

	public void setScriptType(String techType) {
		this.scriptType = techType;
	}

	public Map<String, String> getJobProperties() {
		return jobProperties;
	}

	public void setJobProperties(Map<String, String> jobProperties) {
		this.jobProperties = jobProperties;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> jobParameters) {
		this.params = jobParameters;
	}

	public Map<String, String> getLibraries() {
		return libraries;
	}

	public String getExecutable() {
		return executable;
	}

	public void setExecutable(String executable) {
		this.executable = executable;
	}

	public String getSparkURL() {
		return sparkURL;
	}

	public void setSparkURL(String sparkURL) {
		this.sparkURL = sparkURL;
	}

	public String getDeployMode() {
		return deployMode;
	}

	public void setDeployMode(String deployMode) {
		this.deployMode = deployMode;
	}

	public void setLibraries(Map<String, String> libraries) {
		this.libraries = libraries;
	}

	public String getCodeSnap() {
		return codeSnap;
	}

	public void setCodeSnap(String codeSnap) {
		this.codeSnap = codeSnap;
	}

	public void setJobLibraries(Map<String, String> jobDependencies) {
		this.libraries = jobDependencies;
	}
}
