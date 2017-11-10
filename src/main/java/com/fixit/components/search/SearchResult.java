/**
 * 
 */
package com.fixit.components.search;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fixit.core.data.mongo.Tradesman;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 15:59:55 GMT+3
 */
public class SearchResult {

	public final SearchParams params;
	public final ImmutableSet<Error> errors;
	public final ImmutableList<Tradesman> tradesmen;
	public final ImmutableMap<String, Long> reviewCountForTradesmen;
	public final Date completedAt;
	
	public enum Error {
		NO_SEARCH_EXISTS;
	}
	
	private SearchResult(Builder builder) {
		params = builder.params;
		tradesmen = ImmutableList.copyOf(builder.tradesmen);
		errors = ImmutableSet.copyOf(builder.errors);
		reviewCountForTradesmen = ImmutableMap.copyOf(builder.reviewCountForTradesmen);
		completedAt = builder.completedAt;
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	public boolean isComplete() {
		return completedAt != null;
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
		private final SearchParams params;
		private Set<Tradesman> tradesmen = new HashSet<>();
		private Set<Error> errors = new HashSet<>();
		private Map<String, Long> reviewCountForTradesmen = new HashMap<>();
		private Date completedAt;
		
		public Builder(SearchParams searchParams) {
			this.params = searchParams;
		}
		
		public Builder addError(Error error) {
			this.errors.add(error);
			return this;
		}
		
		public Builder addTradesman(Tradesman tradesman) {
			this.tradesmen.add(tradesman);
			return this;
		}
		
		public Builder addTradesmen(List<Tradesman> tradesmen) {
			this.tradesmen.addAll(tradesmen);
			return this;
		}
		
		public Builder setReviewCountForTradesmen(Map<String, Long> reviewCountForTradesman) {
			this.reviewCountForTradesmen = reviewCountForTradesman;
			return this;
		}
		
		public Builder setComplete() {
			this.completedAt = new Date();
			return this;
		}
		
		public SearchResult build() {
			return new SearchResult(this);
		}
	}
	
}
