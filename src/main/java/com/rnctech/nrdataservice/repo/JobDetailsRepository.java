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
public interface JobDetailsRepository extends JpaRepository<JobDetails,  Long> {

	  @Query("SELECT jd FROM JobDetails jd WHERE LOWER(jd.tenant) = LOWER(:tenant) AND jd.job.id = :jobid")
	  public JobDetails findByTenantAndJobid(@Param("tenant") String tenant, @Param("jobid") Long jobid);
	  
	  public List<JobDetails> findByJobname(String jobname);
	  
	  public Iterable<JobDetails> findByJob(Job job);
	  
}
