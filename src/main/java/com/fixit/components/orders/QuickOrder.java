/**
 * 
 */
package com.fixit.components.orders;

import org.springframework.util.StringUtils;

import com.fixit.core.data.mongo.TempUser;
import com.fixit.core.data.sql.Profession;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/10 16:36:12 GMT+2
 */
public class QuickOrder {

	private final String userName;
	private final String userEmail;
	private final String userTelephone;
	private final Profession profession;
	private final String address;
	private final Float latitude;
	private final Float longitude;
	
	public QuickOrder(String userName, String userEmail, String userTelephone, Profession profession, String address,
			Float latitude, Float longitude) {
		this.userName = userName;
		this.userEmail = userEmail;
		if(!userTelephone.startsWith("+")) {
			userTelephone = "+" + userTelephone;
		}
		this.userTelephone = userTelephone;
		this.profession = profession;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public String getUserName() {
		return userName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public String getUserTelephone() {
		return userTelephone;
	}

	public Profession getProfession() {
		return profession;
	}

	public String getAddress() {
		return address;
	}

	public Float getLatitude() {
		return latitude;
	}

	public Float getLongitude() {
		return longitude;
	}
	
	public TempUser toUser() {
		return new TempUser(userName, userEmail, userTelephone);
	}

	public boolean isValid() {
		if(StringUtils.isEmpty(userName)) {
			return false;
		} else if(profession == null) {
			return false;
		} else if(StringUtils.isEmpty(userTelephone)) {
			return false;
		} else if(StringUtils.isEmpty(address) && (latitude == null || longitude == null)) {
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public String toString() {
		return "QuickOrder [userName=" + userName + ", userEmail=" + userEmail + ", userTelephone=" + userTelephone
				+ ", profession=" + profession + ", address=" + address + ", latitude=" + latitude + ", longiture="
				+ longitude + "]";
	}
	
}
