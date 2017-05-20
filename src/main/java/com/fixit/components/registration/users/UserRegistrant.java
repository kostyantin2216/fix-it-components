/**
 * 
 */
package com.fixit.components.registration.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.mongo.AppInstallationDao;
import com.fixit.core.dao.mongo.UserDao;
import com.fixit.core.data.mongo.AppInstallation;
import com.fixit.core.data.mongo.User;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/16 21:00:07 GMT+3
 */
@Component
public class UserRegistrant {

	private final UserDao mUserDao;
	private final AppInstallationDao mInstallationDao;
	
	@Autowired
	public UserRegistrant(UserDao userDao, AppInstallationDao appInstallationDao) {
		mUserDao = userDao;
		mInstallationDao = appInstallationDao;
	}
	
	public User findOrRegister(User user, AppInstallation appInstallation) {
		
		
		return null;
	}
	
	
	
	
}
