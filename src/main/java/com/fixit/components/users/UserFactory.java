/**
 * 
 */
package com.fixit.components.users;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.mongo.TempUserDao;
import com.fixit.core.dao.mongo.UserDao;
import com.fixit.core.data.UserType;
import com.fixit.core.data.mongo.CommonUser;
import com.fixit.core.data.mongo.OrderData;
import com.fixit.core.data.mongo.TempUser;
import com.fixit.core.data.mongo.User;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/30 21:50:01 GMT+2
 */
@Component
public class UserFactory {

	private final UserDao mUserDao;
	private final TempUserDao mTempUserDao;
	private final UserRegistrationController mUserRegistrationCntrl;
	private final UserConverter mUserCnvrtr;
	
	@Autowired
	public UserFactory(UserDao userDao, TempUserDao tempUserDao, UserRegistrationController userRegistrationController, UserConverter userConverter) {
		mUserDao = userDao;
		mTempUserDao = tempUserDao;
		mUserRegistrationCntrl = userRegistrationController;
		mUserCnvrtr = userConverter;
	}
	
	public UserRegistrationResult createOrFindAppUser(User user, String appInstallationId) {
		UserRegistrationResult registrationResult = mUserRegistrationCntrl.findOrRegister(user, appInstallationId);
		
		if(!registrationResult.invalidAppInstallationId) {
			user = registrationResult.user;
			List<TempUser> tempUsers = mTempUserDao.findByTelephone(user.getTelephone());
			
			if(!tempUsers.isEmpty()) {
				registrationResult = UserRegistrationResult.tempUsersFound(registrationResult);
				mUserCnvrtr.convert(user, tempUsers);
			}
		}
		
		return registrationResult;
	}
	
	public CommonUser createOrFindCommonUser(String name, String email, String telephone) {
		return createOrFindCommonUser(new TempUser(name, email, telephone));
	}
	
	public CommonUser createOrFindCommonUser(TempUser tempUser) {
		String telephone = tempUser.getTelephone();
		User user = mUserDao.findFirstWithTelephone(telephone);
		
		if(user != null) {
			return user;
		} else {
			mTempUserDao.save(tempUser);
			return tempUser;
		}
	}
	
	public CommonUser getUserForOrder(OrderData orderData) {
		if(orderData.getUserType() == UserType.REGULAR) {
			return mUserDao.findById(orderData.getUserId());
		} else {
			return mTempUserDao.findById(orderData.getUserId());
		}
	}
	
	public CommonUser tryFindUser(ObjectId id) {
		CommonUser user = mUserDao.findById(id);
		if(user == null) {
			user = mTempUserDao.findById(id);
		}
		return user;
	}
	
	public List<CommonUser> findUsersWithContainingTelepone(String telephoneQuery) {
		List<CommonUser> result = new ArrayList<>();
		result.addAll(mUserDao.findByStartingWith(UserDao.PROP_TELEPHONE, telephoneQuery));
		result.addAll(mTempUserDao.findByStartingWith(TempUserDao.PROP_TELEPHONE, telephoneQuery));
		return result;
	}
	
}
