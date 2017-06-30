/**
 * 
 */
package com.fixit.components.orders;

import com.fixit.core.data.JobLocation;
import com.fixit.core.data.mongo.User;
import com.fixit.core.data.sql.UserStatistics;
import com.fixit.core.general.PropertyGroup;
import com.fixit.core.general.StoredProperties;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 22:14:41 GMT+3
 */
public class OrderMessageFactory {
	
	private final String firstOrderDiscount;
	
	public OrderMessageFactory(PropertyGroup ordersPropertyGroup) {
		firstOrderDiscount = ordersPropertyGroup.getString(StoredProperties.ORDERS_FIRST_ORDER_DISCOUNT, "0");
	}

	public String createMessage(User user, UserStatistics userStatistics, JobLocation jobLocation, String reason) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("New order from ").append(user.getName())
		  .append(", telephone: ").append(user.getTelephone())
		  .append(", location: ").append(jobLocation.toReadableAddress());
		
		if(!firstOrderDiscount.equals("0") && userStatistics.getJobsOrdered() == 0) {
			sb.append("\nFirst order! ")
			  .append(firstOrderDiscount)
			  .append(" discount.");
		}
		sb.append("\nComment:\n").append(reason);
		
		return sb.toString();
	}
	
}
