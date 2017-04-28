/**
 * 
 */
package com.fixit.components.registration.tradesmen;

import javax.mail.Session;
import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.dao.sql.TradesmanLeadDao;
import com.fixit.core.data.shopify.ShopifyCustomer;
import com.fixit.core.data.sql.TradesmanLead;
import com.fixit.core.general.PropertyGroup;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.general.SimpleEmailSender;
import com.fixit.core.general.StoredProperties;
import com.fixit.core.logging.FILog;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/25 23:45:35 GMT+3
 */
@Component
public class TradesmanRegistrant {
	
	private final static String LOG_TAG = TradesmanRegistrant.class.getSimpleName();

	private final TradesmanLeadDao mLeadDao;
	private final StoredPropertyDao mPropertyDao;
	private final SimpleEmailSender mMailSender;
	
	@Autowired
	public TradesmanRegistrant(TradesmanLeadDao tradesmanLeadDao, StoredPropertyDao storedPropertyDao, Session session) {
		mLeadDao = tradesmanLeadDao;
		mPropertyDao = storedPropertyDao;
		PropertyGroup pg = mPropertyDao.getPropertyGroup(Group.mail);
		String from = pg.getString(StoredProperties.MAIL_USERNAME, "");
		try {
			mMailSender = new SimpleEmailSender(session, from);
		} catch (AddressException e) {
			// this should not happen unless an illegal email is entered in stored properties.
			throw new IllegalArgumentException("Could not create mail sender with from email: " + from);
		}
	}
	
	public void newRegistration(ShopifyCustomer shopifyCustomer) {
		TradesmanLead lead = TradesmanLead.newLead(
				shopifyCustomer.getId(),
				shopifyCustomer.getFirst_name(),
				shopifyCustomer.getLast_name(),
				shopifyCustomer.getEmail()
		);
		
		if(mLeadDao.isNewLead(lead)) {
			registerLead(lead);
		} else {
			FILog.w(LOG_TAG, "Couldn't store lead: " + lead, true);
		}
	}
	
	private void registerLead(TradesmanLead lead) {
		mLeadDao.save(lead);
		
		String subject = "Fix It Registration";
		String content = "Hello " + lead.getFirstName() + " " + lead.getLastName() + "\n"
						+ "please complete your registration here http://www.fixxit.com/web/registration/" + lead.getId(); 

		mMailSender.sendMail(subject, content, lead.getEmail());
	}
	
}
