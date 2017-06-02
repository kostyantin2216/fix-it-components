/**
 * 
 */
package com.fixit.components.registration.tradesmen;

import javax.mail.Session;
import javax.mail.internet.AddressException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.dao.sql.TradesmanLeadDao;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.shopify.ShopifyCustomer;
import com.fixit.core.data.sql.TradesmanLead;
import com.fixit.core.general.PropertyGroup;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.general.StoredProperties;
import com.fixit.core.logging.FILog;
import com.fixit.core.messaging.SimpleEmailSender;
import com.fixit.core.utils.Constants;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/25 23:45:35 GMT+3
 */
@Component
public class TradesmanRegistrant {
	
	private final TradesmanDao mTradesmanDao;
	private final TradesmanLeadDao mLeadDao;
	private final StoredPropertyDao mPropertyDao;
	private final StatisticsCollector mStatsCollector;
	private final SimpleEmailSender mMailSender;
	
	@Autowired
	public TradesmanRegistrant(TradesmanDao tradesmanDao, TradesmanLeadDao tradesmanLeadDao, 
							   StoredPropertyDao storedPropertyDao, StatisticsCollector statisticsCollector, 
							   Session session) {
		mTradesmanDao = tradesmanDao;
		mLeadDao = tradesmanLeadDao;
		mPropertyDao = storedPropertyDao;
		mStatsCollector = statisticsCollector;
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
			FILog.w(Constants.LT_TRADESMAN_REGISTRATION, "Couldn't store lead: " + lead, true);
		}
	}
	
	private void registerLead(TradesmanLead lead) {
		mLeadDao.save(lead);
		
		String subject = "Fix It Registration";
		String content = "Hello " + lead.getFirstName() + " " + lead.getLastName() + "\n"
						+ "please complete your registration here http://www.fixxit.com/web/registration/" + lead.getId(); 

		mMailSender.sendMail(subject, content, lead.getEmail());
	}
	
	public TradesmanLead findLead(long leadId) {
		return mLeadDao.findById(leadId);
	}
	
	public void registerTradesman(Tradesman tradesman) {
		mTradesmanDao.save(tradesman);
		mStatsCollector.tradesmanRegistered(tradesman);
	}
	
}
