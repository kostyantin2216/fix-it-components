/**
 * 
 */
package com.fixit.components.synchronization;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fixit.core.data.DataModelObject;
import com.fixit.core.data.SynchronizationAction;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/03/29 22:02:33 GMT+3
 */
public class SynchronizationResult<DMO extends DataModelObject<ID>, ID extends Serializable> {
	
	private String name;
    private Set<ResultData<DMO, ID>> results = new HashSet<>();

    public SynchronizationResult() { }

    public SynchronizationResult(String name, Set<ResultData<DMO, ID>> result) {
        this.name = name;
        this.results = result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Set<ResultData<DMO, ID>> getResults() {
		return results;
	}

	public void setResults(Set<ResultData<DMO, ID>> result) {
		this.results = result;
	}

	public void addAction(SynchronizationAction action, Set<ID> ids) {
    	this.results.add(new ResultData<>(action, ids));
    }
    
    public void addAction(SynchronizationAction action, List<DMO> data) {
    	this.results.add(new ResultData<>(action,  data));
    }

	@Override
	public String toString() {
		return "SynchronizationResult [name=" + name + ", results=" + results + "]";
	}
	
	public static class ResultData<DMO extends DataModelObject<ID>, ID extends Serializable> {
		public final SynchronizationAction action;
		public final List<DMO> data;
		public final Set<ID> ids;
		
		public ResultData(SynchronizationAction action, List<DMO> data) {
			this.action = action;
			this.data = data;
			this.ids = null;
		}
		
		public ResultData(SynchronizationAction action, Set<ID> ids) {
			this.action = action;
			this.ids = ids;
			this.data = null;
		}
	}
    
}
