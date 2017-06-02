/**
 * 
 */
package com.fixit.components.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.utils.CommonUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 14:24:55 GMT+3
 */
@Component
public class SearchExecutor {
	
	private final TradesmanDao mTradesmanDao;
	private final StatisticsCollector mStatsCollector;

	private final ListeningExecutorService mExecutor;
	private final Cache<String, SearchResult> mCache;
	
	private final Set<String> mOnGoingSearches = new HashSet<>();
	private final Object mLock = new Object();
	
	@Autowired
	public SearchExecutor(TradesmanDao tradesmanDao, StatisticsCollector statisticsCollector) {
		mTradesmanDao = tradesmanDao;
		mStatsCollector = statisticsCollector;
		
		int maxTasks = (int) Math.floor(CommonUtils.percent(70, Runtime.getRuntime().availableProcessors()));
		mExecutor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(maxTasks));
		mCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
	}
	
	public String createSearch(SearchParams searchParams) {
		final String key = createKey(searchParams);
		
		boolean startSearch = false;
		
		synchronized (mLock) {
			if(mCache.getIfPresent(key) == null && !mOnGoingSearches.contains(key)) {
				mOnGoingSearches.add(key);
				startSearch = true;
			}
		}
		
		if(startSearch) {
			ListenableFuture<SearchResult> future = mExecutor.submit(new SearchTask(mTradesmanDao, searchParams));
			Futures.addCallback(future, new FutureCallback<SearchResult>() {

				@Override
				public void onSuccess(SearchResult result) {
					synchronized (mLock) {
						mCache.put(key, result);
						mOnGoingSearches.remove(key);
					}
				}

				@Override
				public void onFailure(Throwable t) {
					synchronized(mLock) {
						mOnGoingSearches.remove(key);
					}
				}
			});
		}
		
		return key;
	}
	
	public SearchResult getResult(String searchKey) {
		SearchResult result = mCache.getIfPresent(searchKey);
		
		if(result == null) {
			boolean isOnGoing;
			synchronized (mLock) {
				isOnGoing = mOnGoingSearches.contains(searchKey);
			}
			
			SearchResult.Builder resultBuilder = new SearchResult.Builder();
			if(!isOnGoing) {
				resultBuilder.setComplete().addError(SearchResult.Error.NO_SEARCH_EXISTS);
			}
			result = resultBuilder.build();
		} else {
			mStatsCollector.searchResultsRequested(result.tradesmen);
		}
		
		return result;
	}
	
	private String createKey(SearchParams searchParams) {
		return searchParams.professionId + "-" + searchParams.location.get_id().toString();
	}
	
}
