/**
 * 
 */
package com.fixit.components.search;

import java.util.List;
import java.util.concurrent.Callable;

import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.data.mongo.Tradesman;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 15:05:53 GMT+3
 */
public class SearchTask implements Callable<SearchResult> {

	private final TradesmanDao mTradesmanDao;
	private final SearchParams mParams;
	
	public SearchTask(TradesmanDao tradesmanDao, SearchParams searchParams) {
		mTradesmanDao = tradesmanDao;
		mParams = searchParams;
	}

	@Override
	public SearchResult call() throws Exception {
		SearchResult.Builder resultBuilder = new SearchResult.Builder();

		List<Tradesman> tradesmen = mTradesmanDao.getTradesmenForArea(mParams.professionId, mParams.location);
		if(tradesmen != null) {
			resultBuilder.addTradesmen(tradesmen);
		}

		return resultBuilder.setComplete().build();
	}

}
