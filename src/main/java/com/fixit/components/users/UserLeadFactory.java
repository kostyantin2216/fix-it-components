/**
 * 
 */
package com.fixit.components.users;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.mongo.OrderDataDao;
import com.fixit.core.data.UserLead;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.OrderData;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/12/01 22:15:03 GMT+2
 */
@Component
public class UserLeadFactory {

	private final OrderDataDao mOrderDao;
	private final UserFactory mUserFactory;
	
	@Autowired
	public UserLeadFactory(OrderDataDao orderDataDao, UserFactory userFactory) {
		mOrderDao = orderDataDao;
		mUserFactory = userFactory;
	}
	
	public List<UserLead> getLeadsForTradesman(ObjectId tradesmanId) {
		List<OrderData> orders = mOrderDao.getOrdersForTradesman(tradesmanId);
		
		List<UserLead> userLeads = new ArrayList<>();
		
		for(OrderData order : orders) {
			CommonUser user = mUserFactory.getUserForOrder(order);
			userLeads.add(new UserLead(user, order));
		}
		
		return userLeads;
	}
	
}
