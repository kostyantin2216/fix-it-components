/**
 * 
 */
package com.fixit.components.orders;

import org.springframework.util.StringUtils;

import com.fixit.core.data.JobLocation;
import com.fixit.core.data.OrderType;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.JobReason;
import com.fixit.core.data.sql.Profession;
import com.fixit.core.data.sql.TrafficSource;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/12/02 21:27:49 GMT+2
 */
public class OrderParams {
	
	private final OrderType orderType;
	private final Profession profession;
	private final CommonUser user;
	private final Tradesman[] tradesmen; 
	private final JobLocation location;
	private final String address;
	private final JobReason[] reasons;
	private final String comment;
	private final TrafficSource trafficSource;
	private final boolean notifyTradesmen;

	private OrderParams(OrderType orderType, Profession profession, CommonUser user, Tradesman[] tradesmen,
			JobLocation location, String address, JobReason[] reasons, String comment, TrafficSource trafficSrc,
			boolean notifyTradesmen) {
		this.orderType = orderType;
		this.profession = profession;
		this.user = user;
		this.tradesmen = tradesmen;
		this.location = location;
		this.address = address;
		this.reasons = reasons;
		this.comment = comment;
		this.trafficSource = trafficSrc;
		this.notifyTradesmen = notifyTradesmen;
	}
	
	private OrderParams(Builder builder) {
		this.orderType = builder.orderType;
		this.profession = builder.profession;
		this.user = builder.user;
		this.tradesmen = builder.tradesmen;
		this.location = builder.location;
		if(StringUtils.isEmpty(builder.address)) {
			this.address = this.location.getGoogleAddress();
		} else {
			this.address = builder.address;
		}
		this.reasons = builder.reasons;
		this.comment = builder.comment;
		this.trafficSource = builder.trafficSource;
		this.notifyTradesmen = builder.notifyTradesmen;
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
	
	public TrafficSource getTrafficSource() {
		return trafficSource;
	}
	
	public boolean isNotifyTradesmen() {
		return notifyTradesmen;
	}
	
	public boolean hasTradesmen() {
		return tradesmen != null && tradesmen.length > 0;
	}

	public OrderParams extendQuickOrder(Tradesman[] tradesmen, JobLocation location) {
		return new OrderParams(OrderType.QUICK, profession, user, tradesmen, location, address, reasons, comment, trafficSource, notifyTradesmen);
	}
	
	public OrderParams updateLocation(JobLocation location) {
		return new OrderParams(orderType, profession, user, tradesmen, location, address, reasons, comment, trafficSource, notifyTradesmen);
	}
	
	public static class Builder {
		private OrderType orderType;
		private Profession profession;
		private CommonUser user;
		private Tradesman[] tradesmen; 
		private JobLocation location;
		private String address;
		private JobReason[] reasons;
		private String comment;
		private TrafficSource trafficSource;
		private boolean notifyTradesmen = true;
		
		public Builder(OrderType orderType) {
			this.orderType = orderType;
		}
		
		public Builder forProfession(Profession profession) {
			this.profession = profession;
			return this;
		}
		
		public Builder byUser(CommonUser user) {
			this.user = user;
			return this;
		}
		
		public Builder withTradesmen(Tradesman[] tradesmen) {
			this.tradesmen = tradesmen;
			return this;
		}
		
		public Builder withTradesman(Tradesman tradesman) {
			this.tradesmen = new Tradesman[] { tradesman };
			return this;
		}
		
		public Builder atLocation(JobLocation location) {
			this.location = location;
			return this;
		}
		
		public Builder atAddress(String address) {
			this.address = address;
			return this;
		}
		
		public Builder forReasons(JobReason[] jobReasons) {
			this.reasons = jobReasons;
			return this;
		}
		
		public Builder withComment(String comment) {
			this.comment = comment;
			return this;
		}
		
		public Builder fromTrafficSource(TrafficSource trafficSource) {
			this.trafficSource = trafficSource;
			return this;
		}
		
		public Builder notifyTradesmen(boolean notifyTradesmen) {
			this.notifyTradesmen = notifyTradesmen;
			return this;
		}
		
		public OrderParams build() {
			if(profession == null) {
				throw new IllegalArgumentException("Cannot build order params without a profession");
			}
			if(user == null) {
				throw new IllegalArgumentException("Cannot build order params without a user");
			}
			if(trafficSource == null) {
				throw new IllegalArgumentException("Cannot build order params without a traffic source");
			}
			if((location == null || StringUtils.isEmpty(location.getGoogleAddress())) && StringUtils.isEmpty(address)) {
				throw new IllegalArgumentException("Cannot build order params without location google address or address");
			}
			if(orderType != OrderType.QUICK && (tradesmen == null || tradesmen.length == 0)) {
				throw new IllegalArgumentException("Cannot build order params without tradesmen unless doing a quick order");
			}
			return new OrderParams(this);
		}
		
		
	}


}
