/**
 * 
 */
package com.fixit.components.search;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.MapAreaDao;
import com.fixit.core.data.MapAreaType;
import com.fixit.core.data.MutableLatLng;
import com.fixit.core.data.mongo.MapArea;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/08/22 23:06:57 GMT+3
 */
@Component
public class SearchController {

	private final SearchExecutor mSearchExec;
	private final MapAreaDao mMapAreaDao;
	private final StatisticsCollector mStatsCollector;
	
	public SearchController(SearchExecutor searchExecutor, MapAreaDao mapAreaDao, StatisticsCollector statisticsCollector) {
		mSearchExec = searchExecutor;
		mMapAreaDao = mapAreaDao;
		mStatsCollector = statisticsCollector;
	}
	
	public Optional<String> createSearch(Integer professionId, MutableLatLng location) {
		MapArea mapArea = mMapAreaDao.getMapAreaAtLocationForType(
				location.getLng(), 
				location.getLat(), 
				MapAreaType.Ward
		);
		
		if(mapArea != null) {
			SearchParams searchParams = new SearchParams(professionId, mapArea);
			String searchId = mSearchExec.createSearch(searchParams);
			return Optional.of(searchId);
		} else {
			return Optional.empty();
		}
	}
	
	public SearchResult getSearchResult(String searchKey) {
		SearchResult searchResult = mSearchExec.getResult(searchKey);
		
		if(searchResult.isComplete && searchResult.errors.isEmpty()) {
			mStatsCollector.searchResultsReceived(searchResult.tradesmen);
		}
		
		return searchResult;
	}
	
}
