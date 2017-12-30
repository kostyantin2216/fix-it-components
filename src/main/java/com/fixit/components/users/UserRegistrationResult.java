/**
 * 
 */
package com.fixit.components.users;

import com.fixit.core.data.mongo.User;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/30 23:07:23 GMT+2
 */
public class UserRegistrationResult {
	static UserRegistrationResult invalidAppInstallationId() {
		return new UserRegistrationResult(null, false, true, false, false);
	}
	
	static UserRegistrationResult emailExists() {
		return new UserRegistrationResult(null, false, false, true, false);
	}
	
	static UserRegistrationResult newUser(User user) {
		return new UserRegistrationResult(user, true, false, false, false);
	}
	
	static UserRegistrationResult existingUser(User user) {
		return new UserRegistrationResult(user, false, false, false, false);
	}
	
	static UserRegistrationResult tempUsersFound(UserRegistrationResult r) {
		return new UserRegistrationResult(r.user, r.newUser, r.invalidAppInstallationId, r.emailExists, true);
	}
	
	public final User user;
	public final boolean invalidAppInstallationId;
	public final boolean emailExists;
	public final boolean newUser;
	public final boolean wasTemp;
	
	private UserRegistrationResult(User user, boolean newUser, boolean invalidAppInstallationId, boolean emailExists, boolean wasTemp) {
		this.user = user;
		this.invalidAppInstallationId = invalidAppInstallationId;
		this.emailExists = emailExists;
		this.newUser = newUser;
		this.wasTemp = wasTemp;
	}
	
}
