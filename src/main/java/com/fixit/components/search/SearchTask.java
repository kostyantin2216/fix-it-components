/**
 * 
 */
package com.fixit.components.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.ReviewDao;
import com.fixit.core.data.mongo.Tradesman;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 15:05:53 GMT+3
 */
public class SearchTask implements Callable<SearchResult> {

	private final TradesmanDao mTradesmanDao;
	private final ReviewDao mReviewDao;
	private final SearchParams mParams;
	
	public SearchTask(TradesmanDao tradesmanDao, ReviewDao reviewDao, SearchParams searchParams) {
		mTradesmanDao = tradesmanDao;
		mReviewDao = reviewDao;
		mParams = searchParams;
	}

	@Override
	public SearchResult call() throws Exception {
		SearchResult.Builder resultBuilder = new SearchResult.Builder(mParams);

		List<Tradesman> tradesmen = mTradesmanDao.findTradesmenForArea(mParams.profession.getId(), mParams.location);
		if(tradesmen != null) {		
			Map<String, Long> reviewCountForTradesmen = new HashMap<>();
			for(Tradesman tradesman : tradesmen) {
				if(tradesman.isActive()) {
					String tradesmanId = tradesman.get_id().toHexString();
					reviewCountForTradesmen.put(tradesmanId, mReviewDao.getCountForTradesman(tradesmanId));
				
					resultBuilder.addTradesman(tradesman);
				}
			}
			resultBuilder.setReviewCountForTradesmen(reviewCountForTradesmen);
		}
		
		return resultBuilder.setComplete().build();
	}

}
