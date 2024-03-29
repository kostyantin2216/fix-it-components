/**
 * 
 */
package com.fixit.components.statistics;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.sql.TradesmanStatisticsDao;
import com.fixit.core.dao.sql.UserStatisticsDao;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.mongo.User;
import com.fixit.core.data.sql.TradesmanStatistics;
import com.fixit.core.data.sql.UserStatistics;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/29 13:03:19 GMT+3
 */
@Component
public class StatisticsCollector {
		
	private final UserStatisticsDao mUserStatsDao;
	private final TradesmanStatisticsDao mTradesmanStatsDao;
	
	public StatisticsCollector(UserStatisticsDao userStatisticsDao, TradesmanStatisticsDao tradesmanStatisticsDao) {
		mUserStatsDao = userStatisticsDao;
		mTradesmanStatsDao = tradesmanStatisticsDao;
	}
	
	public UserStatistics getUserStatistics(ObjectId userId) {
		return getUserStatistics(userId.toHexString());
	}
	
	public UserStatistics getUserStatistics(String userId) {
		UserStatistics userStatistics = mUserStatsDao.findById(userId);
		if(userStatistics == null) {
			userStatistics = new UserStatistics(userId);
			mUserStatsDao.save(userStatistics);
		}
		return userStatistics;
	}
	
	public TradesmanStatistics getTradesmanStatistics(ObjectId tradesmanId) {
		return getTradesmanStatistics(tradesmanId.toHexString());
	}
	
	public TradesmanStatistics getTradesmanStatistics(String tradesmanId) {
		TradesmanStatistics tradesmanStatistics = mTradesmanStatsDao.findById(tradesmanId);
		if(tradesmanStatistics == null) {
			tradesmanStatistics = new TradesmanStatistics(tradesmanId);
			mTradesmanStatsDao.save(tradesmanStatistics);
		}
		return tradesmanStatistics;
	}
	
	public void tradesmanRegistered(Tradesman tradesman) {
		mTradesmanStatsDao.save(new TradesmanStatistics(tradesman.get_id().toHexString()));
	}
	
	public void userRegistered(User user) {
		mUserStatsDao.save(new UserStatistics(user.get_id().toHexString()));
	}
	
	public void searchResultsReceived(List<Tradesman> tradesmen) {
		for(Tradesman tradesman : tradesmen) {
			TradesmanStatistics tradesmanStats = getTradesmanStatistics(tradesman.get_id());
			tradesmanStats.setTimesShown(tradesmanStats.getTimesShown() + 1);
			mTradesmanStatsDao.update(tradesmanStats);
		}
	}
	
	public void newOrder(CommonUser user, Tradesman[] tradesmen) {
		UserStatistics userStats = getUserStatistics(user.get_id());
		userStats.setJobsOrdered(userStats.getJobsOrdered() + 1);
		mUserStatsDao.update(userStats);
		for(Tradesman tradesman : tradesmen) {
			TradesmanStatistics tradesmanStats = getTradesmanStatistics(tradesman.get_id());
			tradesmanStats.setJobsSent(tradesmanStats.getJobsSent() + 1);
			mTradesmanStatsDao.update(tradesmanStats);
		}
	}
}
