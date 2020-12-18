package com.rnctech.nrdataservice.utils;

public class GInfo {
	GitProp git;
	BuildProp build;
	
	public GitProp getGit() {
		return git;
	}
	public void setGit(GitProp git) {
		this.git = git;
	}
	public BuildProp getBuild() {
		return build;
	}
	public void setBuild(BuildProp build) {
		this.build = build;
	}
	
	public class Commit{
		String time;
		String id;
		public String getTime() {
			return time;
		}
		public void setTime(String time) {
			this.time = time;
		}
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		} 
	}
	public class GitProp{
		Commit commit;
		String tag;
		String branch;
		public Commit getCommit() {
			return commit;
		}
		public void setCommit(Commit commit) {
			this.commit = commit;
		}
		public String getTag() {
			return tag;
		}
		public void setTag(String tag) {
			this.tag = tag;
		}
		public String getBranch() {
			return branch;
		}
		public void setBranch(String branch) {
			this.branch = branch;
		}
	}
	
	public class BuildProp {
		String artifact;
		String name;
		String group;
		String version;
		String time;
		public String getArtifact() {
			return artifact;
		}
		public void setArtifact(String artifact) {
			this.artifact = artifact;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getGroup() {
			return group;
		}
		public void setGroup(String group) {
			this.group = group;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public String getTime() {
			return time;
		}
		public void setTime(String time) {
			this.time = time;
		}
	}
	
}
