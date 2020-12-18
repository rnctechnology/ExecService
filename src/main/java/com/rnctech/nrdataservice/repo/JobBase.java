package com.rnctech.nrdataservice.repo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;

/**
 * @contributor zilin
 * 2020.01
 */

@MappedSuperclass
public abstract class JobBase implements java.io.Serializable {

	public JobBase() {
		this.created = new Date();
		this.lastModified = new Date();
		this.createdby = "RNCTech";
		this.updatedby = "RNCTech";
	}

	Date created;
	String createdby;
	Date lastModified;
	String updatedby;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    @Column(name = "id", updatable = false, nullable = false)
	private long id;

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "created", unique = false, nullable = true, length = 19)
	public Date getCreated() {
		return this.created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	@Column(name = "createdby", unique = false, nullable = true)
	public String getCreatedby() {
		return this.createdby;
	}

	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}
	
	@Column(name = "lastModified", unique = false, nullable = true, length = 19)
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	@Column(name = "updatedby", unique = false, nullable = true)
	public String getUpdatedby() {
		return this.updatedby;
	}

	public void setUpdatedby(String updatedby) {
		this.updatedby = updatedby;
	}
}
