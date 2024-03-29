/**
 * 
 */
package com.fixit.components.registration;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.statistics.StatisticsCollector;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.dao.sql.TradesmanLeadDao;
import com.fixit.core.dao.sql.impl.TradesmanLeadDaoImpl;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.TradesmanLead;
import com.fixit.core.general.FileManager;
import com.fixit.core.logging.FILog;
import com.fixit.core.messaging.SimpleEmailSender;
import com.fixit.core.utils.Constants;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/25 23:45:35 GMT+3
 */
@Component
public class TradesmanRegistrationController {
	
	private final FileManager mFileManager;
	private final TradesmanDao mTradesmanDao;
	private final TradesmanLeadDao mLeadDao;
	private final StoredPropertyDao mPropertyDao;
	private final StatisticsCollector mStatsCollector;
	private final SimpleEmailSender mMailSender;
	private final ServerEventController mEventController;
	
	@Autowired
	public TradesmanRegistrationController(FileManager fileManager, TradesmanDao tradesmanDao, TradesmanLeadDao tradesmanLeadDao, 
							   StoredPropertyDao storedPropertyDao, StatisticsCollector statisticsCollector, 
							   SimpleEmailSender simpleEmailSender, ServerEventController serverEventController) {
		mFileManager = fileManager;
		mTradesmanDao = tradesmanDao;
		mLeadDao = tradesmanLeadDao;
		mPropertyDao = storedPropertyDao;
		mStatsCollector = statisticsCollector;
		mMailSender = simpleEmailSender;
		mEventController = serverEventController;
	}
	
	public void newLead(TradesmanLead lead) {
		if(mLeadDao.isNewLead(lead)) {
			registerLead(lead);
		} else {
			TradesmanLead registeredLead = mLeadDao.findOneByProperty(TradesmanLeadDaoImpl.PROP_EMAIL, lead.getEmail());
			if(!registeredLead.isEmailSent()) {
			//	sendEmail(lead);
				mLeadDao.update(lead);
			}
			FILog.w(Constants.LT_TRADESMAN_REGISTRATION, "Couldn't store lead: " + lead, true);
		}
	}
	
	private void sendEmail(TradesmanLead lead) {
		String subject = "Fix It Registration";
		String content = "Hello " + lead.getFirstName() + " " + lead.getLastName() + "\n"
						+ "please complete your registration here http://fixxit-web.co.za/web/tradesmanRegistration?leadId=" + lead.getId(); 

		boolean sentSuccessfully = mMailSender.sendMail(subject, content, lead.getEmail());
		lead.setEmailSent(sentSuccessfully);
	}
	
	private void registerLead(TradesmanLead lead) {
		mLeadDao.save(lead);
		
		mEventController.newLead(lead);
		//sendEmail(lead);
	}
	
	public TradesmanLead findLead(long leadId) {
		return mLeadDao.findById(leadId);
	}
	
	public void registerTradesman(long leadId, Tradesman tradesman, InputStream logoInputStream, String logoFileExtension) throws IOException {
		mTradesmanDao.save(tradesman);
		
		String tradesmanId = tradesman.get_id().toHexString();
		String logoPath = mFileManager.storeTradesmanLogo(tradesmanId, logoFileExtension, logoInputStream);
		tradesman.setLogoUrl(logoPath);
		
		mTradesmanDao.update(tradesman);
		
		mStatsCollector.tradesmanRegistered(tradesman);
		
		TradesmanLead lead = findLead(leadId);
		lead.setTradesmanId(tradesmanId);
		mLeadDao.update(lead);
	}
	
}
