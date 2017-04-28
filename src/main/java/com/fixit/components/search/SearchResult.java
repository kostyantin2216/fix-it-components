/**
 * 
 */
package com.fixit.components.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fixit.core.data.mongo.Tradesman;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 15:59:55 GMT+3
 */
public class SearchResult {

	public final ImmutableSet<Error> errors;
	public final ImmutableList<Tradesman> tradesmen;
	public final boolean isComplete;
	
	public enum Error {
		NO_SEARCH_EXISTS;
	}
	
	private SearchResult(Builder builder) {
		tradesmen = ImmutableList.copyOf(builder.tradesmen);
		errors = ImmutableSet.copyOf(builder.errors);
		isComplete = builder.isComplete;
	}
	
	public String errorToString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Error> itr = errors.iterator();
		
		while(itr.hasNext()) {
			sb.append(" - ").append(itr.next().name().toLowerCase());
			if(itr.hasNext()) {
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	public static class Builder {
		private List<Tradesman> tradesmen = new ArrayList<>();
		private Set<Error> errors = new HashSet<>();
		private boolean isComplete = false;
		
		public Builder() { }
		
		public Builder addError(Error error) {
			errors.add(error);
			return this;
		}
		
		public Builder addTradesman(Tradesman tradesman) {
			tradesmen.add(tradesman);
			return this;
		}
		
		public Builder addTradesmen(List<Tradesman> tradesmen) {
			this.tradesmen.addAll(tradesmen);
			return this;
		}
		
		public Builder setComplete() {
			this.isComplete = true;
			return this;
		}
		
		public SearchResult build() {
			return new SearchResult(this);
		}
	}
	
}
