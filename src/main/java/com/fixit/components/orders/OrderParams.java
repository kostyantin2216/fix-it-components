/**
 * 
 */
package com.fixit.components.orders;

import com.fixit.core.data.JobLocation;
import com.fixit.core.data.OrderType;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.JobReason;
import com.fixit.core.data.sql.Profession;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/12/02 21:27:49 GMT+2
 */
public class OrderParams {
	
	public static OrderParams searchOrder(Profession profession, CommonUser user, Tradesman[] tradesmen, 
			JobLocation location, JobReason[] jobReasons, String comment) {
		return new OrderParams(OrderType.SEARCH, profession, user, tradesmen, location, null, jobReasons, comment);
	}
	
	public static OrderParams directOrder(CommonUser user, Tradesman tradesman, Profession profession, String address, String comment) {
		return new OrderParams(OrderType.DIRECT, profession, user, new Tradesman[] { tradesman }, null, address, null, comment);
	}
	
	public static OrderParams quickOrder(CommonUser user, Profession profession, String address, String comment) {
		return new OrderParams(OrderType.QUICK, profession, user, null, null, address, null, comment);
	}
	
	public static OrderParams quickOrder(CommonUser user, Profession profession, JobLocation location, JobReason[] reasons, String comment) {
		return new OrderParams(OrderType.QUICK, profession, user, null, location, null, reasons, comment);
	}
	
	private final OrderType orderType;
	private final Profession profession;
	private final CommonUser user;
	private final Tradesman[] tradesmen; 
	private final JobLocation location;
	private final String address;
	private final JobReason[] reasons;
	private final String comment;

	private OrderParams(OrderType orderType, Profession profession, CommonUser user, Tradesman[] tradesmen,
			JobLocation location, String address, JobReason[] reasons, String comment) {
		this.orderType = orderType;
		this.profession = profession;
		this.user = user;
		this.tradesmen = tradesmen;
		this.location = location;
		this.address = address;
		this.reasons = reasons;
		this.comment = comment;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public Profession getProfession() {
		return profession;
	}

	public CommonUser getUser() {
		return user;
	}

	public Tradesman[] getTradesmen() {
		return tradesmen;
	}

	public JobLocation getLocation() {
		return location;
	}
	
	public String getAddress() {
		return address;
	}

	public JobReason[] getReasons() {
		return reasons;
	}

	public String getComment() {
		return comment;
	}
	
	public OrderParams extendQuickOrder(Tradesman[] tradesmen, JobLocation location) {
		return new OrderParams(OrderType.QUICK, profession, user, tradesmen, location, address, reasons, comment);
	}
	
	public OrderParams extendDirectOrder(JobLocation location) {
		return new OrderParams(OrderType.DIRECT, profession, user, tradesmen, location, address, reasons, comment);
	}

}
