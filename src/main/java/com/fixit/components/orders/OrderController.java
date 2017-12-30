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
	
	public Optional<OrderData> quickOrder(OrderParams op) {
		Optional<JobLocation> jobLocation;
		if(op.getLocation() == null) {
			jobLocation = mMapAreaController.createLocation(op.getAddress());
		} else {
			jobLocation = Optional.of(op.getLocation());
		}
		
		if(jobLocation.isPresent()) {
			JobLocation location = jobLocation.get();
			MapArea mapArea = mMapAreaController.getAreaOfJobLocation(location);
			if(mapArea != null) {
				SearchResult searchResult = mSearchController.blockingSearch(op.getProfession(), mapArea);
				
				int maxTradesmen = mProperties.get(Group.orders.name(), StoredProperties.ORDERS_MAX_QUICK_ORDER_TRADESMEN, 3);
				Tradesman[] tradesmen = ImmutableList.sortedCopyOf(Tradesman.PRIORITY_COMPARATOR, searchResult.tradesmen)
							 						 .stream()
							 						 .limit(maxTradesmen)
							 						 .toArray(Tradesman[]::new);
				return Optional.of(orderTradesmen(
						op.extendQuickOrder(tradesmen, location)
				));
			} else {
				mEventsController.orderFailed(op, "Could not find map area for location: " + location);
			}
		} else {
			mEventsController.orderFailed(op, "Could not create job location for address");
		}
		
		return Optional.empty();
	}
	
	public Optional<OrderData> directOrder(OrderParams op) {
		Optional<JobLocation> jobLocation = mMapAreaController.createLocation(op.getAddress());
		
		if(jobLocation.isPresent()) {
			return Optional.of(orderTradesmen(
					op.extendDirectOrder(jobLocation.get())
			));
		} else {
			mEventsController.orderFailed(op, "Could not create job location for address");
		}
		
		return Optional.empty();
	}
	
	public OrderData orderTradesmen(OrderParams op) {		
		FILog.i("Creating order type: " + op.getOrderType() + ", for " + op.getProfession().getName() 
				+ " at " + op.getLocation().getGoogleAddress());
		
		OrderLine orderLine = createOrderLine(op.getProfession().getId(), op.getTradesmen(), op.getReasons());
		
		mMapAreaController.normalizeJobLocation(op.getLocation());
		
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
				new Date()
		);
		
		mOrderDataDao.save(order);
		
		sendOrderMessages(order, op, orderLine.telephonesForTradesmen);
		
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
