/**
 * 
 */
package com.fixit.components.registration.users;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.AppInstallationDao;
import com.fixit.core.dao.mongo.UserDao;
import com.fixit.core.dao.mongo.impl.UserDaoImpl;
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
	private final StatisticsCollector mStatsCollector;
	
	@Autowired
	public UserRegistrant(UserDao userDao, AppInstallationDao appInstallationDao, StatisticsCollector statisticsCollector) {
		mUserDao = userDao;
		mInstallationDao = appInstallationDao;
		mStatsCollector = statisticsCollector;
	}
	
	public RegistrationResult findOrRegister(User user, String appInstallationId) {		
		AppInstallation appInstallaition = mInstallationDao.findById(new ObjectId(appInstallationId));
		if(appInstallaition != null) {
			User existingUser = null;
			String facebookId = user.getFacebookId();
			if(!StringUtils.isEmpty(facebookId)) {
				existingUser = mUserDao.findOneByProperty(UserDaoImpl.PROP_FACEBOOK_ID, facebookId);
			}
			if(existingUser == null) {
				String googleId = user.getGoogleId();
				if(!StringUtils.isEmpty(googleId)) {
					existingUser = mUserDao.findOneByProperty(UserDaoImpl.PROP_GOOGLE_ID, googleId);
				}
				if(existingUser == null) {
					Map<String, Object> properties = new HashMap<>();
					properties.put(UserDaoImpl.PROP_EMAIL, user.getEmail());
					properties.put(UserDaoImpl.PROP_TELEPHONE, user.getTelephone());
					existingUser = mUserDao.findOneByMap(properties);
					
					if(existingUser == null) {
						existingUser = mUserDao.findOneByProperty(UserDaoImpl.PROP_EMAIL, user.getEmail());
						
						if(existingUser == null) {
							mUserDao.save(user);
							mStatsCollector.userRegistered(user);
							return new RegistrationResult(user, true);
						} else {
							return new RegistrationResult(false, true);
						}
					} else {
						boolean update = false;
						if(!StringUtils.isEmpty(facebookId)) {
							existingUser.setFacebookId(facebookId);
							update = true;
						}
						
						if(!StringUtils.isEmpty(googleId)) {
							existingUser.setGoogleId(googleId);
							update = true;
						}
						
						if(update) {
							mUserDao.update(existingUser);
						}
					}
				}
			}
			
			return new RegistrationResult(existingUser, false);
		} else {
			return new RegistrationResult(true, false);
		}
	}
		
	public static class RegistrationResult {
		public final User user;
		public final boolean invalidAppInstallationId;
		public final boolean emailExists;
		public final boolean newUser;
		
		public RegistrationResult(User user, boolean newUser) {
			this(user, newUser, false, false);
		}
		
		public RegistrationResult(boolean invalidAppInstallationId, boolean emailExists) {
			this(null, false, invalidAppInstallationId, emailExists);
		}
		
		public RegistrationResult(User user, boolean newUser, boolean invalidAppInstallationId, boolean emailExists) {
			this.user = user;
			this.invalidAppInstallationId = invalidAppInstallationId;
			this.emailExists = emailExists;
			this.newUser = newUser;
		}
	}
	
}
