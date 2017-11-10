/**
 * 
 */
package com.fixit.components.orders;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.OrderDataDao;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.OrderMessageDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.OrderType;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.JobReason;
import com.fixit.core.data.sql.OrderMessage;
import com.fixit.core.data.sql.Profession;
import com.fixit.core.general.PropertyGroup;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.logging.FILog;
import com.fixit.core.messaging.SimpleMessageSender;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 21:56:25 GMT+3
 */
@Component
public class OrderController {
	
	private final PropertyGroup mProperties;
	private final OrderDataDao mOrderDataDao;
	private final OrderMessageDao mOrderMsgDao;
	private final TradesmanDao mTradesmanDao;
	private final SimpleMessageSender mMsgSender;
	private final StatisticsCollector mStatsCollector;
	private final OrderMessageFactory mMsgFactory;
	private final ServerEventController mEventsController;
	
	@Autowired
	public OrderController(StoredPropertyDao storedPropertyDao, OrderDataDao orderDataDao, OrderMessageDao orderMessageDao, 
									TradesmanDao tradesmanDao, SimpleMessageSender messageSender, 
									StatisticsCollector statisticsCollector, ServerEventController serverEventsController) {
		mProperties = storedPropertyDao.getPropertyGroup(Group.orders);
		mOrderDataDao = orderDataDao;
		mOrderMsgDao = orderMessageDao;
		mTradesmanDao = tradesmanDao;
		mMsgSender = messageSender;
		mStatsCollector = statisticsCollector;
		mMsgFactory = new OrderMessageFactory(mProperties);
		mEventsController = serverEventsController;
	}
	
	public OrderData orderTradesmen(OrderType orderType, Profession profession, CommonUser user, Tradesman[] tradesmen, 
									JobLocation location) {
		return orderTradesmen(orderType, profession, user, tradesmen, location, new JobReason[0], null);
	}
	
	public OrderData orderTradesmen(OrderType orderType, Profession profession, CommonUser user, Tradesman[] tradesmen, 
									JobLocation location, JobReason[] jobReasons, String comment) {		
		FILog.i("Creating order type: " + orderType + ", for " + profession.getName() 
				+ " at " + location.getGoogleAddress());
		
		OrderLine orderLine = createOrderLine(profession.getId(), tradesmen, jobReasons);
		
		OrderData order = new OrderData(
				orderLine.tradesmenIds, 
				user.get_id(), 
				orderLine.professionId, 
				location, 
				orderLine.jobReasonIds, 
				comment, 
				false, 
				orderType,
				new Date()
		);
		
		mOrderDataDao.save(order);
		
		sendOrderMessages(user, order, orderLine.tradesmenIds, orderLine.telephones, location, jobReasons, comment);
		
		mStatsCollector.newOrder(user, tradesmen);
		mEventsController.newOrder(user, tradesmen, profession, location, jobReasons, comment);
		
		return order;
	}	
	
	private OrderLine createOrderLine(int professionId, Tradesman[] tradesmen, JobReason[] jobReasons) {
		int tradesmenCount = tradesmen.length;
		
		ObjectId[] tradesmenIds = new ObjectId[tradesmenCount];
		String[] telephones = new String[tradesmenCount];
		
		for(int i = 0; i < tradesmenCount; i++) {
			tradesmenIds[i] = tradesmen[i].get_id();
			String telephone = tradesmen[i].getTelephone();
			if(StringUtils.isEmpty(telephone)) {
				telephones[i] = mTradesmanDao.getTelephoneForTradsman(tradesmenIds[i]);
			} else {
				telephones[i] = telephone;
			}
		}
		
		int[] jobReasonIds = new int[jobReasons.length];
		for(int i = 0; i < jobReasonIds.length; i++) {
			jobReasonIds[i] = jobReasons[i].getId();
		}
		
		return new OrderLine(professionId, jobReasonIds, tradesmenIds, telephones);
	}
	
	private void sendOrderMessages(CommonUser user, OrderData order, ObjectId[] tradesmenIds, 
								  String[] telephones, JobLocation location, JobReason[] jobReasons, 
								  String comment) {

		String userId = user.get_id().toHexString();
		String orderId = order.get_id().toHexString();
		String content = mMsgFactory.createMessage(
				user, mStatsCollector.getUserStatistics(user.get_id()), location, jobReasons, comment
		);
		
		for(int i = 0; i < tradesmenIds.length; i++) {
			String messageSid = mMsgSender.sendMessage(telephones[i], content);
			OrderMessage orderMessage = new OrderMessage(
					messageSid, 
					orderId, 
					userId, 
					tradesmenIds[i].toHexString(), 
					content
			);
			
			mOrderMsgDao.save(orderMessage);
		}
	}
	
	public List<OrderData> getUserOrderHistory(ObjectId userId) {
		return mOrderDataDao.getOrdersForUser(userId);
	}
	
	private static class OrderLine {
		final int professionId;
		final int[] jobReasonIds;
		final ObjectId[] tradesmenIds;
		final String[] telephones;
		
		OrderLine(int professionId, int[] jobReasonIds, ObjectId[] tradesmenIds, String[] telephones) {
			this.professionId = professionId;
			this.jobReasonIds = jobReasonIds;
			this.tradesmenIds = tradesmenIds;
			this.telephones = telephones;
		}
	}
	
}
