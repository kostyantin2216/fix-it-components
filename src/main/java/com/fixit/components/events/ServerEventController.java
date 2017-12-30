/**
 * 
 */
package com.fixit.components.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.orders.OrderParams;
import com.fixit.components.search.SearchResult;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.User;
import com.fixit.core.data.sql.Review;
import com.fixit.core.data.sql.TradesmanLead;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/10/02 20:12:10 GMT+3
 */
@Component
public class ServerEventController {

	private final static String EVENT_NEW_LEAD = "New Lead";
	private final static String EVENT_NEW_USER = "New User";
	private final static String EVENT_NEW_ORDER = "New Order";
	private final static String EVENT_NEW_REVIEW = "New Review";
	private final static String EVENT_RETURNING_USER = "Returning User";
	private final static String EVENT_REVIEW_UPDATED = "Review Updated";
	private final static String EVENT_EMPTY_SEARCH = "Empty Search";
	private final static String EVENT_ORDER_FAILED = "Order Failed";
	
	private final static String PARAM_TRADESMAN_LEAD = "tradesmanLead";
	private final static String PARAM_USER = "user";
	private final static String PARAM_PROFESSION = "profession";
	private final static String PARAM_LOCATION = "location";
	private final static String PARAM_REVIEW = "review";
	private final static String PARAM_COMPLETED_AT = "completedAt";
	private final static String PARAM_ORDER_PARAMS = "orderParams";
	private final static String PARAM_ORDER = "order";
	private final static String PARAM_ERROR = "error";
	private final static String PARAM_USER_TYPE = "userType";
	
	private final ServerEventNotifier mNotifier;
	
	@Autowired
	public ServerEventController(ServerEventNotifier notifier) {
		mNotifier = notifier;
	}

	private void newEvent(ServerEvent event) {
		mNotifier.notify(event);
	}
	
	public void newLead(TradesmanLead lead) {
		newEvent(new ServerEvent.Builder(EVENT_NEW_LEAD)
				.doNotify()
				.addParam(PARAM_TRADESMAN_LEAD, lead)
				.build());
	}
	
	public void newUser(User user) {
		newEvent(new ServerEvent.Builder(EVENT_NEW_USER)
				.doNotify()
				.addParam(PARAM_USER, user)
				.build());
	}
	
	public void newReview(Review review) {
		newEvent(new ServerEvent.Builder(EVENT_NEW_REVIEW)
				.doNotify()
				.addParam(PARAM_REVIEW, review)
				.build());
	}
	
	public void returningUser(User user) {
		newEvent(new ServerEvent.Builder(EVENT_RETURNING_USER)
				.doNotify()
				.addParam(PARAM_USER, user)
				.build());
	}
	
	public void reviewUpdated(Review review) {
		newEvent(new ServerEvent.Builder(EVENT_REVIEW_UPDATED)
				.doNotify()
				.addParam(PARAM_REVIEW, review)
				.build());
	}
	
	public void newOrder(OrderParams params, OrderData order) {
		newEvent(new ServerEvent.Builder(EVENT_NEW_ORDER)
				.doNotify()
				.addParam(PARAM_ORDER_PARAMS, params)
				.addParam(PARAM_ORDER, order)
				.build());
	}

	public void orderFailed(OrderParams orderParams, String error) {
		newEvent(new ServerEvent.Builder(EVENT_ORDER_FAILED)
				.doNotify()
				.addParam(PARAM_ORDER_PARAMS, orderParams)
				.addParam(PARAM_USER_TYPE, orderParams.getUser().getType())
				.addParam(PARAM_ERROR, error)
				.build());
	}
	
	public void searchComplete(SearchResult searchResult) {
		if(searchResult.isComplete() && searchResult.tradesmen.isEmpty()) {
			emptySearch(searchResult);
		}
	}
	
	private void emptySearch(SearchResult searchResult) {
		newEvent(new ServerEvent.Builder(EVENT_EMPTY_SEARCH)
				.doNotify()
				.addParam(PARAM_PROFESSION, searchResult.params.profession)
				.addParam(PARAM_LOCATION, searchResult.params.location)
				.addParam(PARAM_COMPLETED_AT, searchResult.completedAt)
				.build());
	}
	
}
