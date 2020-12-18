package com.rnctech.nrdataservice.repo;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * @contributor zilin
 * 2020.01
 */

@Transactional
public interface JobPolicyRepository extends JpaRepository<JobPolicy,  Long> {

	  @Query("SELECT jp FROM JobPolicy jp WHERE jp.job.id = :jobid")
	  public JobPolicy findByJobid(@Param("jobid") Long jobid);
	  
	  public Iterable<JobPolicy> findByJob(Job job);
	  
}
