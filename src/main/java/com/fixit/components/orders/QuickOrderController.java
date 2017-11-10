/**
 * 
 */
package com.fixit.components.orders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.components.events.ServerEventController;
import com.fixit.components.search.SearchController;
import com.fixit.components.search.SearchResult;
import com.fixit.core.dao.mongo.MapAreaDao;
import com.fixit.core.dao.mongo.TempUserDao;
import com.fixit.core.dao.sql.StoredPropertyDao;
import com.fixit.core.data.Address;
import com.fixit.core.data.JobLocation;
import com.fixit.core.data.MapAreaType;
import com.fixit.core.data.OrderType;
import com.fixit.core.data.mongo.MapArea;
import com.fixit.core.data.mongo.TempUser;
import com.fixit.core.data.mongo.Tradesman;
import com.fixit.core.data.sql.Profession;
import com.fixit.core.general.PropertyGroup.Group;
import com.fixit.core.general.StoredProperties;
import com.fixit.core.logging.FILog;
import com.fixit.core.services.GeocoderService;
import com.google.common.collect.ImmutableList;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/11/10 16:14:16 GMT+2
 */
@Component
public class QuickOrderController {
	
	private final static String LOG_TAG = "QuickOrder";

	@Autowired
	private OrderController orderController;
	
	@Autowired
	private SearchController searchController;
	
	@Autowired
	private ServerEventController serverEventController;
	
	@Autowired
	private MapAreaDao mapAreaDao;
	
	@Autowired
	private TempUserDao tempUserDao;
	
	@Autowired
	private StoredPropertyDao storedPropertyDao;
	
	
	public void doOrder(final QuickOrder order) {
		Observable.fromCallable(newOrderTransformationCallable(order))
			.subscribeOn(Schedulers.io())
			.doOnError(e -> onError(order, "Unexpected error", e))
			.doOnNext(params -> FILog.i(LOG_TAG, "processed order and received: " + params))
			.filter(params -> validate(order, params))
			.map(params -> params.get())
			.subscribe(params -> orderController.orderTradesmen(
					OrderType.QUICK,
					params.profession,
					params.user,
					params.tradesmen,
					params.location
			));
	}
	
	private Callable<Optional<OrderParams>> newOrderTransformationCallable(final QuickOrder quickOrder) {
		return () -> {	
			Optional<Address> addressOptional = GeocoderService.getAddress(quickOrder.getAddress());
			
			if(addressOptional.isPresent()) {
				Profession profession = quickOrder.getProfession();
				Address address = addressOptional.get();
				
				MapArea mapArea = mapAreaDao.getMapAreaAtLocationForType(
						address.getLongitude(), 
						address.getLatitude(), 
						MapAreaType.Ward
				);
				
				if(mapArea != null) {
					SearchResult searchResult = searchController.blockingSearch(profession, mapArea);
						
					int tradesmenCount = searchResult.tradesmen.size();
					if(searchResult.hasErrors()) {
						onError(quickOrder, "Errors during search: " + searchResult.errorToString(), null);
					} else if(tradesmenCount > 0) {
						JobLocation jobLocation = JobLocation.create(address);
						TempUser user = createTempUser(quickOrder);
						
						List<Tradesman> sortedTradesmen = ImmutableList.sortedCopyOf(Tradesman.PRIORITY_COMPARATOR, searchResult.tradesmen);
						Integer maxTradesmen = storedPropertyDao.getPropertyGroup(Group.orders).getInteger(StoredProperties.ORDERS_MAX_QUICK_ORDER_TRADESMEN, 3);
						Tradesman[] tradesmen = new Tradesman[maxTradesmen > tradesmenCount ? tradesmenCount : maxTradesmen];
						for(int i = 0; i < tradesmen.length; i++) {
							tradesmen[i] = sortedTradesmen.get(i);
						}
						
						return Optional.of(new OrderParams(profession, user, tradesmen, jobLocation));
					}
				} else {
					onError(quickOrder, "Could not find map area for coordinates: lat = " 
								+ address.getLatitude() + ", lng = " + address.getLongitude(), null);
				}
			} else {
				onError(quickOrder, "could not get address from geocoder", null);
			}
			return Optional.empty();
		};
	}	
	
	private TempUser createTempUser(QuickOrder quickOrder) {
		TempUser user = quickOrder.toUser();
		tempUserDao.save(user);
		return user;
	}
	
	private boolean validate(final QuickOrder order, final Optional<OrderParams> paramsOptional) {
		if(paramsOptional.isPresent()) {
			OrderParams params = paramsOptional.get();
			String prefix = "Cannot create an order without ";
			String error;
			if(params.profession == null) {
				error = prefix + "a profession";
			} else if(params.user == null) {
				error = prefix + "a user";
			} else if(params.tradesmen == null || params.tradesmen.length == 0) {
				error = prefix + "tradesmen";
			} else if(params.location == null) {
				error = prefix + "a location";
			} else {
				return true;
			}	
			onError(order, error, null);
		} 
		return false;		
	}
	
	private void onError(final QuickOrder quickOrder, final String error, final Throwable t) {
		serverEventController.quickOrderFailed(quickOrder, error);
		FILog.e(LOG_TAG, error, t, true);
	}
	
	private static class OrderParams {
		final Profession profession;
		final TempUser user;
		final Tradesman[] tradesmen;
		final JobLocation location;
		public OrderParams(Profession profession, TempUser user, Tradesman[] tradesmen, JobLocation location) {
			this.profession = profession;
			this.user = user;
			this.tradesmen = tradesmen;
			this.location = location;
		}
		@Override
		public String toString() {
			return "OrderParams [profession=" + profession + ", user=" + user + ", tradesmen="
					+ Arrays.toString(tradesmen) + ", location=" + location + "]";
		}
	}
	
}
