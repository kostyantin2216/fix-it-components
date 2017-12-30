/**
 * 
 */
package com.fixit.components.users;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.AppInstallationDao;
import com.fixit.core.dao.mongo.UserDao;
import com.fixit.core.dao.mongo.impl.UserDaoImpl;
import com.fixit.core.data.mongo.AppInstallation;
import com.fixit.core.data.mongo.User;
import com.fixit.core.utils.Formatter;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/05/16 21:00:07 GMT+3
 */
@Component
public class UserRegistrationController {

	private final UserDao mUserDao;
	private final AppInstallationDao mInstallationDao;
	private final StatisticsCollector mStatsCollector;
	private final ServerEventController mEventController;
	
	@Autowired
	public UserRegistrationController(UserDao userDao, AppInstallationDao appInstallationDao, StatisticsCollector statisticsCollector,
									  ServerEventController serverEventController) {
		mUserDao = userDao;
		mInstallationDao = appInstallationDao;
		mStatsCollector = statisticsCollector;
		mEventController = serverEventController;
	}
	
	UserRegistrationResult findOrRegister(User user, String appInstallationId) {		
		AppInstallation appInstallaition = mInstallationDao.findById(new ObjectId(appInstallationId));
		if(appInstallaition != null) {
			user.setTelephone(Formatter.normalizeTelephone(user.getTelephone()));
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
							mEventController.newUser(user);
							return UserRegistrationResult.newUser(user);
						} else {
							return UserRegistrationResult.emailExists();
						}
					}
				}
			}
			
			if(existingUser.update(user)) {
				mUserDao.update(existingUser);
			}
			mEventController.returningUser(existingUser);
			return UserRegistrationResult.existingUser(existingUser);
		} else {
			return UserRegistrationResult.invalidAppInstallationId();
		}
	}
	
}
