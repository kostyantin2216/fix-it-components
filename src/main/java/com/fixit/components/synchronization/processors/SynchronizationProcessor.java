/**
 * 
 */
package com.fixit.components.synchronization.processors;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import com.fixit.components.synchronization.SynchronizationResult;
import com.fixit.core.dao.UpdateDateDao;
import com.fixit.core.data.SynchronizationAction;
import com.fixit.core.data.UpdateDateDataModelObject;
import com.fixit.core.data.mongo.SynchronizationParams;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/03/30 22:01:12 GMT+3
 */
public interface SynchronizationProcessor<DAO extends UpdateDateDao<DMO, ID>, DMO extends UpdateDateDataModelObject<ID>, ID extends Serializable> {

	public SynchronizationResult<DMO, ID> process(Date firstSynchronization, SynchronizationParams params, Set<SynchronizationAction> actions);
	public String getDmoName();
	
}
