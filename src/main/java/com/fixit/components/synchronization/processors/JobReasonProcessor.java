/**
 * 
 */
package com.fixit.components.synchronization.processors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.sql.JobReasonDao;
import com.fixit.core.data.sql.JobReason;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/08/30 22:39:23 GMT+3
 */
@Component
public class JobReasonProcessor extends BaseSynchronizationProcessor<JobReasonDao, JobReason, Integer> {

	@Autowired
	public JobReasonProcessor(JobReasonDao jobReasonDao) {
		super(jobReasonDao);
	}
	
	@Override
	public String getDmoName() {
		return JobReason.class.getSimpleName();
	}

}
