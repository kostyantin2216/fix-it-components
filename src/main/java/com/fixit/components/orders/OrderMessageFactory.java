/**
 * 
 */
package com.fixit.components.orders;

import org.springframework.util.StringUtils;

import com.fixit.core.data.JobLocation;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.sql.JobReason;
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

	public String createMessage(CommonUser user, UserStatistics userStatistics, JobLocation jobLocation, JobReason[] jobReasons, String comment) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Order from ").append(user.getName())
		  .append(", ").append(user.getTelephone())
		  .append(", ").append(jobLocation.toReadableAddress(false));
		
		if(!firstOrderDiscount.equals("0") && userStatistics.getJobsOrdered() == 0) {
			sb.append("\nFirst order! ")
			  .append(firstOrderDiscount)
			  .append(" discount.");
		}
		
		sb.append("\n");
		if(!StringUtils.isEmpty(comment)) {
			sb.append(comment).append(";");
		}
		
		int charCount = sb.length();
		
		for(JobReason jobReason : jobReasons) {
			if(sb.length() > charCount) {
				sb.append(",");
			}
			sb.append(jobReason.getName());
		}
		
		return sb.toString();
	}
	
}
