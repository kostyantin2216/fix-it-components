/**
 * 
 */
package com.fixit.components.maps;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fixit.core.dao.mongo.MapAreaDao;
import com.fixit.core.dao.mongo.OrderDataDao;
import com.fixit.core.dao.mongo.TradesmanDao;
import com.fixit.core.data.Address;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.MapAreaType;
import com.fixit.core.data.mongo.MapArea;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.logging.FILog;
import com.fixit.core.services.GeocoderService;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/17 15:18:41 GMT+2
 */
@Component
public class MapAreaController {

	private final MapAreaDao mMapAreaDao;
	private final TradesmanDao mTradesmanDao;
	private final OrderDataDao mOrderDao;
	
	@Autowired
	public MapAreaController(MapAreaDao mapAreaDao, TradesmanDao tradesmanDao, OrderDataDao orderDataDao) {
		mMapAreaDao = mapAreaDao;
		mTradesmanDao = tradesmanDao;
		mOrderDao = orderDataDao;
	}
	
	public Map<String, Integer> getTradesmenCountPerArea() {
		List<Tradesman> tradesmen = mTradesmanDao.findAll();
		
		Map<String, Integer> result = new HashMap<>();
		
		for(Tradesman tradesman : tradesmen) {
			for(String areaId : tradesman.getWorkingAreas()) {
				Integer count = result.get(areaId);
				if(count == null) {
					count = 1;
				} else {
					count++;
				}
				result.put(areaId, count);
			}
		}
		
		return result;
	}
	
	public MapArea getAreaOfJobLocation(JobLocation location) {
		normalizeJobLocation(location);
		return mMapAreaDao.findById(new ObjectId(location.getMapAreaId()));
	}
	
	public Optional<JobLocation> createLocation(String addressStr) {
		Optional<Address> addressOptional;
		try {
			addressOptional = GeocoderService.getAddress(addressStr);
		} catch (IOException e) {
			addressOptional = Optional.empty();
			FILog.e("Error while trying to get geocode address: " + addressStr);
		}

		if(addressOptional.isPresent()) {
			Address address = addressOptional.get();
			
			MapArea mapArea = mMapAreaDao.getMapAreaAtLocationForType(
					address.getLongitude(), 
					address.getLatitude(), 
					MapAreaType.Ward
			);
			
			return Optional.of(JobLocation.create(mapArea, address));
		}
		
		return Optional.empty();
	}
	
	public void normalizeJobLocation(JobLocation location) {
		if(StringUtils.isEmpty(location.getMapAreaId())) {
			MapArea mapArea = mMapAreaDao.getMapAreaAtLocationForType(location.getLng(), location.getLat(), MapAreaType.Ward);
			location.setMapAreaId(mapArea.get_id().toHexString());
		}
	}
	
}
