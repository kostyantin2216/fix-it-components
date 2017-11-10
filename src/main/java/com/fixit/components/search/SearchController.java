/**
 * 
 */
package com.fixit.components.search;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.data.mongo.MapArea;
import com.fixit.core.data.sql.Profession;

import io.reactivex.Observable;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/08/22 23:06:57 GMT+3
 */
@Component
public class SearchController {

	private final SearchExecutor mSearchExec;
	private final StatisticsCollector mStatsCollector;
	
	public SearchController(SearchExecutor searchExecutor, StatisticsCollector statisticsCollector) {
		mSearchExec = searchExecutor;
		mStatsCollector = statisticsCollector;
	}
	
	public Observable<SearchResult> doSearch(Profession profession, MapArea location) {
		return Observable.just(createSearch(profession, location))
		  .flatMap(searchKey -> 
		    Observable.interval(1, TimeUnit.SECONDS)
		    	.map(i -> getSearchResult(searchKey))
		    	.filter(result -> result.isComplete())
		    	.take(1)
		  );
	}
	
	public SearchResult blockingSearch(Profession profession, MapArea location) {
		return doSearch(profession, location).blockingFirst();
	}
	
	public String createSearch(Profession profession, MapArea location) {
		SearchParams searchParams = new SearchParams(profession, location);
		return mSearchExec.createSearch(searchParams);
	}
	
	public SearchResult getSearchResult(String searchKey) {
		SearchResult searchResult = mSearchExec.getResult(searchKey);
		
		if(searchResult.isComplete() && searchResult.errors.isEmpty()) {
			mStatsCollector.searchResultsReceived(searchResult.tradesmen);
		}
		
		return searchResult;
	}
	
}
