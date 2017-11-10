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

import com.fixit.components.events.ServerEventController;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.ReviewDao;
import com.fixit.core.utils.Formatter;
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
	private final ReviewDao mReviewDao;

	private final ServerEventController mEventController;
	
	private final ListeningExecutorService mExecutor;
	private final Cache<String, SearchResult> mCache;
	
	private final Set<String> mOnGoingSearches = new HashSet<>();
	private final Object mLock = new Object();
	
	
	@Autowired
	public SearchExecutor(TradesmanDao tradesmanDao, ReviewDao reviewDao, ServerEventController serverEventController) {
		mTradesmanDao = tradesmanDao;
		mReviewDao = reviewDao;
	
		mEventController = serverEventController;
		
		int maxTasks = (int) Math.floor(Formatter.percent(70, Runtime.getRuntime().availableProcessors()));
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
			ListenableFuture<SearchResult> future = mExecutor.submit(new SearchTask(mTradesmanDao, mReviewDao, searchParams));
			Futures.addCallback(future, new FutureCallback<SearchResult>() {

				@Override
				public void onSuccess(SearchResult result) {
					synchronized (mLock) {
						mCache.put(key, result);
						mOnGoingSearches.remove(key);
					}
					mEventController.searchComplete(result);
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
		SearchResult result;
		boolean onGoingSearch;
		
		synchronized (mLock) {
			result = mCache.getIfPresent(searchKey);
			onGoingSearch = mOnGoingSearches.contains(searchKey);
		}
		
		if(result == null) {			
			SearchResult.Builder resultBuilder = new SearchResult.Builder(null);
			if(!onGoingSearch) {
				resultBuilder.setComplete().addError(SearchResult.Error.NO_SEARCH_EXISTS);
			}
			result = resultBuilder.build();
		}
		
		return result;
	}
	
	private String createKey(SearchParams searchParams) {
		return searchParams.profession.getId() + "-" + searchParams.location.get_id().toHexString();
	}
	
}
