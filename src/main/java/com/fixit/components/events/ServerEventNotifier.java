/**
 * 
 */
package com.fixit.components.events;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.config.GsonManager;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.general.StoredProperties;
import com.fixit.core.messaging.SimpleEmailSender;
import com.google.gson.Gson;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/10/02 20:59:52 GMT+3
 */
@Component
public class ServerEventNotifier {
	
	private final SimpleEmailSender mEmailSender;
	private final StoredPropertyDao mStoredPropertyDao;
	private final Gson mGson;
	
	@Autowired
	public ServerEventNotifier(SimpleEmailSender emailSender, StoredPropertyDao storedPropertyDao, GsonManager gsonManager) {
		this.mEmailSender = emailSender;
		this.mStoredPropertyDao = storedPropertyDao;
		this.mGson = gsonManager.getServerNotifierGson();
	}
	
	public void notify(ServerEvent event) {
		if(event.shouldNotify()) {
			String[] emails = mStoredPropertyDao.getPropertyGroup(Group.events)
						.getJsonProperty(StoredProperties.EVENTS_NOTIFY_ADDRESSES, String[].class);
		
			if(emails != null) {
				mEmailSender.sendMail(
						"New Fixxit Server Event: " + event.getEvent(), 
						mGson.toJson(event.getParams()), 
						Arrays.asList(emails));
			}
		}
	}

}
