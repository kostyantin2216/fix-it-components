/**
 * 
 */
package com.fixit.components.orders;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.users.UserFactory;
import com.fixit.core.dao.mongo.OrderRequestDao;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.JobReasonDao;
import com.fixit.core.dao.sql.ProfessionDao;
import com.fixit.core.dao.sql.TrafficSourceDao;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.OrderRequest;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.JobReason;
import com.fixit.core.data.sql.Profession;
import com.fixit.core.data.sql.TrafficSource;

/**
 * @author 		Kostyantin
 * @createdAt 	2018/01/17 18:46:10 GMT+2
 */
@Component
public class OrderRequestController {
	
	private final OrderRequestDao mOrderRequestDao;
	private final TradesmanDao mTradesmanDao;
	private final ProfessionDao mProfessionDao;
	private final JobReasonDao mJobReasonDao;
	private final TrafficSourceDao mTrafficSourceDao;
	private final UserFactory mUserFactory;
	private final OrderController mOrderController;
	private final ServerEventController mEventsController;

	@Autowired
	public OrderRequestController(OrderRequestDao orderRequestDao, TradesmanDao tradesmanDao, ProfessionDao professionDao, 
								  JobReasonDao jobReasonDao, TrafficSourceDao trafficSourceDao, UserFactory userFactory,
								  OrderController orderController, ServerEventController serverEventController) {
		mOrderRequestDao = orderRequestDao;
		mTradesmanDao = tradesmanDao;
		mProfessionDao = professionDao;
		mJobReasonDao = jobReasonDao;
		mTrafficSourceDao = trafficSourceDao;
		mUserFactory = userFactory;
		mOrderController = orderController;		
		mEventsController = serverEventController;
	}
	
	public List<OrderRequest> getAllRequests() {
		return mOrderRequestDao.findAll();
	}
	
	public OrderRequest newRequest(OrderParams orderParams) {
		OrderRequest request = transformParams(orderParams);
		request.setNewRequest(true);
		
		mOrderRequestDao.save(request);
		mEventsController.newOrderRequest(orderParams, request);
		
		return request;
	}
	
	public OrderData completeRequest(OrderRequest request) {
		OrderParams params = transformRequest(request);
		
		Optional<OrderData> orderData = mOrderController.orderTradesmen(params);
		
		if(orderData.isPresent()) {
			request.setFulfilledOrderId(orderData.get().get_id());
			mOrderRequestDao.update(request);
			
			mEventsController.orderRequestComplete(params, request);
		} else {
			mEventsController.orderRequestFailed(params, request);
		}
		
		return orderData.get();
	}
	
	public void denyRequest(OrderRequest request, String reason) {
		request.setReasonDenied(reason);
		mOrderRequestDao.update(request);
		
		mEventsController.orderRequestDenied(request, reason);
	}
	
	private OrderRequest transformParams(OrderParams orderParams) {
		OrderRequest request = new OrderRequest();
		request.setComment(orderParams.getComment());
		request.setCreatedAt(new Date());
		request.setNotifyTradesmen(orderParams.isNotifyTradesmen());
		
		JobReason[] reasons = orderParams.getReasons();
		if(reasons != null && reasons.length > 0) {
			request.setJobReasons(
					Arrays.stream(reasons)
						.mapToInt(jr -> jr.getId())
						.toArray()
			);
		} else {
			request.setJobReasons(new int[0]);
		}
		
		request.setAddress(orderParams.getAddress());
		request.setLocation(orderParams.getLocation());
		request.setOrderType(orderParams.getOrderType());
		request.setProfessionId(orderParams.getProfession().getId());
		request.setTradesmen(
				Arrays.stream(orderParams.getTradesmen())
					.map(t -> t.get_id())
					.toArray(ObjectId[]::new)
		);
		request.setTrafficSourceId(orderParams.getTrafficSource().getId());
		request.setUserId(orderParams.getUser().get_id());
		
		return request;
	}
	
	private OrderParams transformRequest(OrderRequest request) {
		OrderParams.Builder opBuilder = new OrderParams.Builder(request.getOrderType())
				.withComment(request.getComment())
				.atAddress(request.getAddress())
				.atLocation(request.getLocation())
				.notifyTradesmen(request.isNotifyTradesmen());
		
		Profession profession = mProfessionDao.findById(request.getProfessionId());
		assert profession != null;
		opBuilder.forProfession(profession);
		
		TrafficSource trafficSource = mTrafficSourceDao.findById(request.getTrafficSourceId());
		assert trafficSource != null;
		opBuilder.fromTrafficSource(trafficSource);
		
		CommonUser user = mUserFactory.tryFindUser(request.getUserId());
		assert user != null;
		opBuilder.byUser(user);
	
		int[] jobReasonIds = request.getJobReasons();
		if(jobReasonIds != null && jobReasonIds.length > 0) {
			opBuilder.forReasons(
					Arrays.stream(jobReasonIds)
						  .mapToObj(jrid -> mJobReasonDao.findById(jrid))
						  .toArray(JobReason[]::new)
			);
		}
		
		ObjectId[] tradesmenIds = request.getTradesmen();
		if(tradesmenIds != null && tradesmenIds.length > 0) {
			opBuilder.withTradesmen(
					Arrays.stream(tradesmenIds)
						  .map(tid -> mTradesmanDao.findById(tid))
						  .toArray(Tradesman[]::new)
			);
		}
		
		return opBuilder.build();
	}
	
	public void updateRequest(OrderRequest request) {
		mOrderRequestDao.update(request);
	}

}
