/**
 * 
 */
package com.fixit.components.orders;

import com.fixit.core.data.JobLocation;
import com.fixit.core.data.mongo.User;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/28 22:14:41 GMT+3
 */
public class OrderMessageFactory {

	public static String createMessage(User user, JobLocation jobLocation, String reason) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("New order from ").append(user.getName())
		  .append(", telephone: ").append(user.getTelephone())
		  .append(", location: ").append(jobLocation.toReadableAddress())
		  .append("\nComment:\n").append(reason);
		
		return sb.toString();
	}
	
}
