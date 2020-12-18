package com.rnctech.nrdataservice.repo;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * @contributor zilin
 * 2020.01
 */

@Transactional
public interface JobRepository extends JpaRepository<Job,  Long> {

	  @Query("SELECT j FROM Job j WHERE LOWER(j.tenant) = LOWER(:tenant) AND j.jobid = :jobid")
	  public List<Job> findByTenantAndJobid(@Param("tenant") String tenant, @Param("jobid") Long jobid);

	  @Query("SELECT j FROM Job j WHERE LOWER(j.tenant) = LOWER(:tenant) AND j.jobid = :jobid AND j.status < 5")
	  public List<Job> findByRunningJobid(@Param("tenant") String tenant, @Param("jobid") Long jobid);
	  
	  @Query("SELECT j FROM Job j WHERE j.status < 5 order by j.tenant")
	  public List<Job> findRunningJob();
	  
	  public List<Job> findByTenant(String tenant);
}
