/**
 * 
 */
package com.fixit.components.orders;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.OrderDao;
import com.fixit.core.dao.sql.OrderMessageDao;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.mongo.Order;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.mongo.User;
import com.fixit.core.data.sql.OrderMessage;
import com.fixit.core.messaging.SimpleMessageSender;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 21:56:25 GMT+3
 */
@Component
public class TradesmanOrderController {
	
	private final OrderDao mOrderDao;
	private final OrderMessageDao mOrderMsgDao;
	private final SimpleMessageSender mMsgSender;
	private final StatisticsCollector mStatsCollector;
	
	@Autowired
	public TradesmanOrderController(OrderDao orderDao, OrderMessageDao orderMessageDao, SimpleMessageSender messageSender, StatisticsCollector statisticsCollector) {
		mOrderDao = orderDao;
		mOrderMsgDao = orderMessageDao;
		mMsgSender = messageSender;
		mStatsCollector = statisticsCollector;
	}

	public void orderTradesmen(User user, Tradesman[] tradesmen, JobLocation location, String reason) {
		String content = OrderMessageFactory.createMessage(user, location, reason);
		
		int tradesmenCount = tradesmen.length;
		ObjectId[] tradesmenIds = new ObjectId[tradesmenCount];
		String[] telephones = new String[tradesmenCount];
		for(int i = 0; i < tradesmenCount; i++) {
			tradesmenIds[i] = tradesmen[i].get_id();
			telephones[i] = tradesmen[i].getTelephone();
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
