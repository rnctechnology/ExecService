package com.rnctech.nrdataservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.rnctech.nrdataservice.RNConsts.TechType;
import com.rnctech.nrdataservice.resource.ResourcePool;
import com.rnctech.nrdataservice.utils.RNCOutputStream;

/**
 * @author Zilin Chen
 * @since 2018 09
 */

public class RNContext {

	private static final ThreadLocal<RNContext> threadlocalctx = new ThreadLocal<>();

	public RNCOutputStream out;
	protected RNContext ctx;
	protected int maxResult;
	private ResourcePool resourcePool;
	private String ctxid;
	private String algorithm;
	private String jobname;
	private String codesnap;
	private String jobType;
	private String srcname; 
	private TechType srctype = TechType.python;
	private List<String> dependenics = new ArrayList<>();
	private Map<String, String> params = new HashMap<>();
	private List<String> attfiles = new ArrayList<>();
	private List<String> pyfiles = new ArrayList<>();
	private Map<String, String> conf = new HashMap<>();
	private int progressing = 0;
	private int keepSession = Integer.MIN_VALUE;

	public static RNContext get() {
		return threadlocalctx.get();
	}

	public static void set(RNContext ic) {
		threadlocalctx.set(ic);
	}

	public static void remove() {
		threadlocalctx.remove();
	}

	public RNContext(int maxResult) {
		this.maxResult = maxResult;
	}

	public ResourcePool getResourcePool() {
		return resourcePool;
	}

	public String getCtxid() {
		return ctxid;
	}

	public void setRNContext(RNContext context) {
		this.ctx = context;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}

	public int getMaxResult() {
		return this.maxResult;
	}

	public static RNContext builder() {
		return new RNContext(100);
	}

	public RNContext setOutput(RNCOutputStream rnctechOutputStream) {
		this.out = rnctechOutputStream;
		return this;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public String getCodesnap() {
		return codesnap;
	}

	public RNContext setCodesnap(String mainname) {
		this.codesnap = mainname;
		return this;
	}

	public RNCOutputStream getOut() {
		return out;
	}

	public RNContext setOut(RNCOutputStream out) {
		this.out = out;
		return this;
	}

	public static ThreadLocal<RNContext> getThreadlocalctx() {
		return threadlocalctx;
	}

	public RNContext setResourcePool(ResourcePool resourcePool) {
		this.resourcePool = resourcePool;
		return this;
	}

	public RNContext setCtxid(String cid) {
		this.ctxid = cid;
		return this;
	}

	public RNContext setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		return this;
	}

	public RNContext build() {
		return this;
	}

	public TechType getSrctype() {
		return srctype;
	}

	public void setSrctype(TechType srctype) {
		this.srctype = srctype;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public List<String> getDependenics() {
		return dependenics;
	}

	public void addDependency(String d) {
		this.dependenics.add(d);
	}
	
	public String getSrcname() {
		return srcname;
	}

	public void setSrcname(String srcname) {
		this.srcname = srcname;
	}

	public void setDependenics(List<String> dependenics) {
		this.dependenics = dependenics;
	}

	public Map<String, String> getParams() {
		return params;
	}
	
	public List<String> getAttfiles() {
		return attfiles;
	}

	public void setAttfiles(List<String> attfiles) {
		this.attfiles = attfiles;
	}

	public List<String> getPyfiles() {
		return pyfiles;
	}

	public void setPyfiles(List<String> pyfiles) {
		this.pyfiles = pyfiles;
	}

	public Map<String, String> getConf() {
		return conf;
	}

	public void setConf(Map<String, String> conf) {
		this.conf = conf;
	}
	
	public void setConfv(String k, String replv) {
		this.conf.put(k, replv);		
	}

	public int getKeepSession() {
		return keepSession;
	}

	public void setKeepSession(int keepSession) {
		this.keepSession = keepSession;
	}

	public void setParam(String k, String v) {
		this.params.put(k, v);
	}
	
	public String getParam(String k) {
		return this.params.get(k);
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public int getProgressing() {
		return progressing;
	}

	public void setProgressing(int progressing) {
		this.progressing = progressing;
	}
	
	public String getJobname() {
		return jobname;
	}

	public void setJobname(String jobname) {
		this.jobname = jobname;
	}



}
