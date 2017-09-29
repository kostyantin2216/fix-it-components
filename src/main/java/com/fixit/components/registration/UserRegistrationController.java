/**
 * 
 */
package com.fixit.components.registration;

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
public class UserRegistrationController {

	private final UserDao mUserDao;
	private final AppInstallationDao mInstallationDao;
	private final StatisticsCollector mStatsCollector;
	
	@Autowired
	public UserRegistrationController(UserDao userDao, AppInstallationDao appInstallationDao, StatisticsCollector statisticsCollector) {
		mUserDao = userDao;
		mInstallationDao = appInstallationDao;
		mStatsCollector = statisticsCollector;
	}
	
	public RegistrationResult findOrRegister(User user, String appInstallationId) {		
		AppInstallation appInstallaition = mInstallationDao.findById(new ObjectId(appInstallationId));
		if(appInstallaition != null) {
			User existingUser = mUserDao.findOneByProperty(UserDao.PROP_TELEPHONE, user.getTelephone());
			if(existingUser == null) {
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
						existingUser = mUserDao.findOneByProperty(UserDaoImpl.PROP_EMAIL, user.getEmail());

						if(existingUser == null) {
							mUserDao.save(user);
							mStatsCollector.userRegistered(user);
							return RegistrationResult.newUser(user);
						} else {
							return RegistrationResult.emailExists();
						}
					}
				}
			}
			
			if(existingUser.update(user)) {
				mUserDao.update(existingUser);
			}
			return RegistrationResult.existingUser(existingUser);
		} else {
			return RegistrationResult.invalidAppInstallationId();
		}
	}
		
	public static class RegistrationResult {
		
		static RegistrationResult invalidAppInstallationId() {
			return new RegistrationResult(null, false, true, false);
		}
		
		static RegistrationResult emailExists() {
			return new RegistrationResult(null, false, false, true);
		}
		
		static RegistrationResult newUser(User user) {
			return new RegistrationResult(user, true, false, false);
		}
		
		static RegistrationResult existingUser(User user) {
			return new RegistrationResult(user, false, false, false);
		}
		
		public final User user;
		public final boolean invalidAppInstallationId;
		public final boolean emailExists;
		public final boolean newUser;
		
		public RegistrationResult(User user, boolean newUser, boolean invalidAppInstallationId, boolean emailExists) {
			this.user = user;
			this.invalidAppInstallationId = invalidAppInstallationId;
			this.emailExists = emailExists;
			this.newUser = newUser;
		}
	}
	
}
