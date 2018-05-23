package com.robindrew.trading.multifeed.provider.cityindex.connection;

import static com.robindrew.common.dependency.DependencyFactory.clearDependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.util.Check;
import com.robindrew.trading.cityindex.platform.ICityIndexTradingPlatform;
import com.robindrew.trading.cityindex.platform.rest.ICityIndexRestService;
import com.robindrew.trading.cityindex.platform.rest.executor.login.LoginResponse;
import com.robindrew.trading.cityindex.platform.streaming.ICityIndexStreamingService;

public class CityIndexConnectionManager implements ICityIndexConnectionManager, CityIndexConnectionManagerMBean {

	private static final Logger log = LoggerFactory.getLogger(CityIndexConnectionManager.class);

	private final ICityIndexRestService rest;
	private final ICityIndexTradingPlatform platform;
	private volatile LoginResponse details;

	public CityIndexConnectionManager(ICityIndexRestService rest, ICityIndexTradingPlatform platform) {
		this.rest = Check.notNull("rest", rest);
		this.platform = Check.notNull("platform", platform);
	}

	@Override
	public boolean isLoggedIn() {
		return details != null;
	}

	@Override
	public LoginResponse getLoginDetails() {
		if (details == null) {
			throw new IllegalStateException("Not logged in");
		}
		return details;
	}

	@Override
	public boolean login() {
		try {
			details = rest.login();

			log.info("Registering Subscriptions");
			ICityIndexStreamingService service = platform.getStreamingService();
			service.connect();
			return true;

		} catch (Exception e) {
			log.warn("Login Failed", e);
			return false;
		}
	}

	@Override
	public boolean logout() {
		try {
			details = null;
			clearDependency(ICityIndexTradingPlatform.class);
			rest.logout();
			return true;
		} catch (Exception e) {
			log.warn("Login Failed", e);
			return false;
		}
	}

}
