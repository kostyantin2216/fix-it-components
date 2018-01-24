/**
 * 
 */
package com.fixit.components.orders;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.maps.MapAreaController;
import com.fixit.components.search.SearchController;
import com.fixit.components.search.SearchResult;
import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.OrderDataDao;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.OrderMessageDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.OrderType;
import com.fixit.core.data.mongo.MapArea;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.JobReason;
import com.fixit.core.data.sql.OrderMessage;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.general.StoredProperties;
import com.fixit.core.logging.FILog;
import com.fixit.core.messaging.SimpleMessageSender;
import com.google.common.collect.ImmutableList;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 21:56:25 GMT+3
 */
@Component
public class OrderController {
	
	private final StoredPropertyDao mProperties;
	private final OrderDataDao mOrderDataDao;
	private final OrderMessageDao mOrderMsgDao;
	private final TradesmanDao mTradesmanDao;
	private final SimpleMessageSender mMsgSender;
	private final StatisticsCollector mStatsCollector;
	private final OrderMessageFactory mMsgFactory;
	private final ServerEventController mEventsController;
	private final MapAreaController mMapAreaController;
	private final SearchController mSearchController;
	
	@Autowired
	public OrderController(StoredPropertyDao storedPropertyDao, OrderDataDao orderDataDao, OrderMessageDao orderMessageDao, 
									TradesmanDao tradesmanDao, SimpleMessageSender messageSender, 
									StatisticsCollector statisticsCollector, ServerEventController serverEventsController,
									MapAreaController mapAreaController, SearchController searchController) {
		mProperties = storedPropertyDao;
		mOrderDataDao = orderDataDao;
		mOrderMsgDao = orderMessageDao;
		mTradesmanDao = tradesmanDao;
		mMsgSender = messageSender;
		mStatsCollector = statisticsCollector;
		mMsgFactory = new OrderMessageFactory(mProperties.getPropertyGroup(Group.orders));
		mEventsController = serverEventsController;
		mMapAreaController = mapAreaController;
		mSearchController = searchController;
	}
	
	public Optional<OrderData> orderTradesmen(OrderParams op) {		
		FILog.i("Creating order type: " + op.getOrderType() + ", for " + op.getProfession().getName() 
				+ " at " + op.getAddress());
	
		Optional<JobLocation> jobLocation = getJobLocation(op);
		
		OrderData orderData = null;
		String error = null;
		if(jobLocation.isPresent()) {
			JobLocation location = jobLocation.get();
			mMapAreaController.normalizeJobLocation(location);
			String mapAreaId = location.getMapAreaId();
			if(mapAreaId != null && ObjectId.isValid(mapAreaId)) {
				op = op.updateLocation(location);
				op = processOrderType(op);
				
				orderData = completeOrder(op);
			} else {
				error = "Location is not supported";
			}
		} else {
			error = "Could not create job location for address";
		}
		
		if(error != null) {
			mEventsController.orderFailed(op, error);
		}
		
		return Optional.ofNullable(orderData);
	}	
	
	private Optional<JobLocation> getJobLocation(OrderParams op) {
		if(op.getLocation() != null) {
			return Optional.of(op.getLocation());
		} else if(!StringUtils.isEmpty(op.getAddress())) {
			return mMapAreaController.createLocation(op.getAddress());
		} else {
			return Optional.empty();
		}
	}
	
	private OrderParams processOrderType(OrderParams op) {
		if(op.getOrderType() == OrderType.QUICK && !op.hasTradesmen()) {
			JobLocation location = op.getLocation();
			MapArea mapArea = mMapAreaController.getAreaOfJobLocation(location);
			SearchResult searchResult = mSearchController.blockingSearch(op.getProfession(), mapArea);
			
			int maxTradesmen = mProperties.get(Group.orders.name(), StoredProperties.ORDERS_MAX_QUICK_ORDER_TRADESMEN, 3);
			Tradesman[] tradesmen = ImmutableList.sortedCopyOf(Tradesman.PRIORITY_COMPARATOR, searchResult.tradesmen)
						 						 .stream()
						 						 .limit(maxTradesmen)
						 						 .toArray(Tradesman[]::new);
			
			return op.extendQuickOrder(tradesmen, location);
		}
		return op;
	}
	
	private OrderData completeOrder(OrderParams op) {
		OrderLine orderLine = createOrderLine(op.getProfession().getId(), op.getTradesmen(), op.getReasons());

		OrderData order = new OrderData(
				orderLine.telephonesForTradesmen.keySet().stream().toArray(ObjectId[]::new), 
				op.getUser().get_id(), 
				orderLine.professionId, 
				op.getLocation(), 
				orderLine.jobReasonIds, 
				op.getComment(), 
				false, 
				op.getUser().getType(),
				op.getOrderType(),
				new Date(),
				op.getTrafficSource().getId()
		);
		
		mOrderDataDao.save(order);
		
		if(op.isNotifyTradesmen()) {
			sendOrderMessages(order, op, orderLine.telephonesForTradesmen);
		}
		
		mStatsCollector.newOrder(op.getUser(), op.getTradesmen());
		mEventsController.newOrder(op, order);
		
		return order;
	}
	
	private OrderLine createOrderLine(int professionId, Tradesman[] tradesmen, JobReason[] jobReasons) {
		int tradesmenCount = tradesmen.length;
		
		Map<ObjectId, String> telephonesForTradesmen = new HashMap<>();
		
		for(int i = 0; i < tradesmenCount; i++) {
			ObjectId tradesmanId = tradesmen[i].get_id();
			String telephone = tradesmen[i].getTelephone();
			if(StringUtils.isEmpty(telephone)) {
				telephone = mTradesmanDao.getTelephoneForTradsman(tradesmanId);
			}
			telephonesForTradesmen.put(tradesmanId, telephone);
		}
		
		int[] jobReasonIds;
		if(jobReasons != null) {
			jobReasonIds = Arrays.stream(jobReasons)
								 .mapToInt(jr -> jr.getId())
								 .toArray();
		} else {
			jobReasonIds = new int[0];
		}
		
		return new OrderLine(professionId, jobReasonIds, telephonesForTradesmen);
	}
	
	private void sendOrderMessages(OrderData order, OrderParams op, Map<ObjectId, String> telephonesForTradesmen) {
		String userId = op.getUser().get_id().toHexString();
		String orderId = order.get_id().toHexString();
		String content = mMsgFactory.createMessage(
				op.getUser(), mStatsCollector.getUserStatistics(op.getUser().get_id()), op.getLocation(), op.getReasons(), op.getComment()
		);
		
		for(Map.Entry<ObjectId, String> entry : telephonesForTradesmen.entrySet()) {
			String messageSid = mMsgSender.sendMessage(entry.getValue(), content);
			OrderMessage orderMessage = new OrderMessage(
					messageSid, 
					orderId, 
					userId, 
					entry.getKey().toHexString(), 
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
		final Map<ObjectId, String> telephonesForTradesmen;
		
		OrderLine(int professionId, int[] jobReasonIds, Map<ObjectId, String> telephonesForTradesmen) {
			this.professionId = professionId;
			this.jobReasonIds = jobReasonIds;
			this.telephonesForTradesmen = telephonesForTradesmen;
		}
	}
	
}
