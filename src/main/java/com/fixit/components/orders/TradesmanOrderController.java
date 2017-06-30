/**
 * 
 */
package com.fixit.components.orders;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.OrderDao;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.OrderMessageDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.mongo.Order;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.mongo.User;
import com.fixit.core.data.sql.OrderMessage;
import com.fixit.core.general.PropertyGroup;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.messaging.SimpleMessageSender;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 21:56:25 GMT+3
 */
@Component
public class TradesmanOrderController {
	
	private final PropertyGroup mProperties;
	private final OrderDao mOrderDao;
	private final OrderMessageDao mOrderMsgDao;
	private final TradesmanDao mTradesmanDao;
	private final SimpleMessageSender mMsgSender;
	private final StatisticsCollector mStatsCollector;
	private final OrderMessageFactory mMsgFactory;
	
	@Autowired
	public TradesmanOrderController(StoredPropertyDao storedPropertyDao, OrderDao orderDao, OrderMessageDao orderMessageDao, 
									TradesmanDao tradesmanDao, SimpleMessageSender messageSender, 
									StatisticsCollector statisticsCollector) {
		mProperties = storedPropertyDao.getPropertyGroup(Group.orders);
		mOrderDao = orderDao;
		mOrderMsgDao = orderMessageDao;
		mTradesmanDao = tradesmanDao;
		mMsgSender = messageSender;
		mStatsCollector = statisticsCollector;
		mMsgFactory = new OrderMessageFactory(mProperties);
	}

	public void orderTradesmen(User user, Tradesman[] tradesmen, JobLocation location, String reason) {
		String content = mMsgFactory.createMessage(
				user, mStatsCollector.getUserStatistics(user.get_id()), location, reason
		);
		
		int tradesmenCount = tradesmen.length;
		ObjectId[] tradesmenIds = new ObjectId[tradesmenCount];
		String[] telephones = new String[tradesmenCount];
		for(int i = 0; i < tradesmenCount; i++) {
			tradesmenIds[i] = tradesmen[i].get_id();
			String telephone = tradesmen[i].getTelephone();
			if(StringUtils.isEmpty(telephone)) {
				telephones[i] = mTradesmanDao.getTelephoneForTradsman(tradesmen[i].get_id());
			} else {
				telephones[i] = telephone;
			}
		}
		
		Order order = new Order(tradesmenIds, user.get_id(), location, reason);
		mOrderDao.save(order);
		
		String userId = user.get_id().toString();
		String orderId = order.get_id().toString();
		
		for(int i = 0; i < tradesmenCount; i++) {
			String messageSid = mMsgSender.sendMessage(telephones[i], content);
			OrderMessage orderMessage = new OrderMessage(
					messageSid, 
					orderId, 
					userId, 
					tradesmenIds[i].toString(), 
					content
			);
			
			mOrderMsgDao.save(orderMessage);
		}
		mStatsCollector.newOrder(user, tradesmen);
	}
}
