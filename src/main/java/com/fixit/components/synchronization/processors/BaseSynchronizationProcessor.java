/**
 * 
 */
package com.fixit.components.synchronization.processors;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.fixit.components.synchronization.SynchronizationResult;
import com.fixit.core.dao.UpdateDateDao;
import com.fixit.core.data.SynchronizationAction;
import com.fixit.core.data.SynchronizationAction.Action;
import com.fixit.core.data.UpdateDateDataModelObject;
import com.fixit.core.data.mongo.SynchronizationParams;
import com.fixit.core.logging.FILog;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/03/29 22:43:05 GMT+3
 */
public abstract class BaseSynchronizationProcessor<DAO extends UpdateDateDao<DMO, ID>, DMO extends UpdateDateDataModelObject<ID>, ID extends Serializable>
	implements SynchronizationProcessor<DAO, DMO, ID> {

	@Autowired
	private DAO dao;
	
	public BaseSynchronizationProcessor(DAO dao) {
		this.dao = dao;
	}
	
	/**
	 *
	 * 
	 * @param lastSynchronization the date of the first synchronization
	 * @param params Params from the database indicating what needs to be synchronized and how.
	 * @param history Previous actions performed in previous synchronizations.
	 */
	@Override
	public SynchronizationResult<DMO, ID> process(Date lastSynchronization, SynchronizationParams params,
												  Set<SynchronizationAction> history) {
		SynchronizationResult<DMO, ID> result = new SynchronizationResult<>();
		
		boolean isFirstSynchronization = history.isEmpty();
		if(isFirstSynchronization) {
			List<DMO> data = dao.findAll();
			result.addAction(new SynchronizationAction(Action.INSERT.name().toLowerCase()), data);
		} else if(params != null) {
			for(SynchronizationAction action : params.getActions()) {
				Map<String, SynchronizationAction> mappedHistory = mapActions(history);
				Action actionEnum = action.getActionEnum();
				String actionName = actionEnum.toLowerCaseName();
				
				if(actionEnum == Action.DELETE) {
					Set<ID> ids = performDelete(lastSynchronization, mappedHistory.get(actionName), params);
					if(ids != null) {
						result.addAction(new SynchronizationAction(actionName), ids);
					}
				} else {
					List<DMO> data;
					switch (actionEnum) {
						case INSERT:
						case UPDATE:
							Date dateRestriction = getDateRestriction(lastSynchronization, action);
							data = dao.getAfterUpdateDate(dateRestriction);
							break;
							
						case OVERRIDE:
							data = dao.findAll();
							break;
	
						default:
							data = null;
							break;
					}
					
					if(data != null) {
						result.addAction(new SynchronizationAction(actionName), data);
					} else {
						FILog.e("Could not get data for action: " + actionName);
					}
				}
			}
		}
		
		return result;
	}
	
	private Map<String, SynchronizationAction> mapActions(Set<SynchronizationAction> actions) {
		Map<String, SynchronizationAction> result = new HashMap<>();
		for(SynchronizationAction action : actions) {
			result.put(action.getActionEnum().toLowerCaseName(), action);
		}
		return result;
	}
	
	public Set<ID> performDelete(Date firstSynchronization, SynchronizationAction action, SynchronizationParams params) {
		// TODO:
		return null;
	}
	
	private Date getDateRestriction(Date firstSynchronization, SynchronizationAction action) {
		if(action == null) {
			return firstSynchronization;
		} else {
			return action.getDate();
		}
	}
	
}
