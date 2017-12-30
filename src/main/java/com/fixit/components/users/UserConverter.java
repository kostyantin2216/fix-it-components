/**
 * 
 */
package com.fixit.components.users;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.mongo.OrderDataDao;
import com.fixit.core.dao.mongo.TempUserDao;
import com.fixit.core.data.UserType;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.TempUser;
import com.fixit.core.data.mongo.User;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/30 23:29:23 GMT+2
 */
@Component
public class UserConverter {

	private final TempUserDao mTempUserDao;
	private final OrderDataDao mOrderDataDao;
	
	@Autowired
	public UserConverter(TempUserDao tempUserDao, OrderDataDao orderDataDao) {
		mTempUserDao = tempUserDao;
		mOrderDataDao = orderDataDao;
	}
	
	public void convert(final User user, List<TempUser> tempUsers) {
		String[] tempUserIds = tempUsers.stream()
										  .map(tempUser -> tempUser.get_id().toHexString())
										  .toArray(String[]::new);
		List<OrderData> orderData = mOrderDataDao.getOrdersForUsersOfType(tempUserIds, UserType.TEMP);
		if(!orderData.isEmpty()) {
			orderData.forEach(order -> {
				order.setUserId(user.get_id());
				order.setUserType(UserType.REGULAR);
			});
			mOrderDataDao.updateUsersForOrders(orderData);
		}
		mTempUserDao.deleteMany(tempUsers);
	}
	
}
