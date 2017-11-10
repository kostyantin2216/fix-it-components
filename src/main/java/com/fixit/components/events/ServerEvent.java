/**
 * 
 */
package com.fixit.components.events;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/10/02 20:18:05 GMT+3
 */
public class ServerEvent {

	private final String event;
	private final ImmutableMap<String, Object> params;
	private final boolean notify;
	
	private ServerEvent(Builder builder) {
		this.event = builder.event;
		this.params = builder.params == null ? ImmutableMap.of() : ImmutableMap.copyOf(builder.params);
		this.notify = builder.notify;
	}
	
	public String getEvent() {
		return event;
	}
	
	public Map<String, Object> getParams() {
		return params;
	}
	
	public boolean shouldNotify() {
		return notify;
	}
	
	public static class Builder {
		private String event;
		
		private Map<String, Object> params;
		private boolean notify = false;
		
		public Builder(String event) {
			this.event = event;
		}
		
		public Builder event(String event) {
			this.event = event;
			return this;
		}
		
		public Builder doNotify() {
			this.notify = true;
			return this;
		}
		
		public Builder addParam(String key, Object value) {
			if(params == null) {
				params = new HashMap<>();
			}
			params.put(key, value);
			return this;
		}
		
		public Builder addParams(Map<String, Object> params) {
			if(this.params == null) {
				this.params = new HashMap<>();
			}
			for(Map.Entry<String, Object> entry : params.entrySet()) {
				this.params.put(entry.getKey(), entry.getValue());
			}
			return this;
		}
		
		public ServerEvent build() {
			return new ServerEvent(this);
		}
	}
}
