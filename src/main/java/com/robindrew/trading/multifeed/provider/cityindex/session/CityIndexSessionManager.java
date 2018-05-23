package com.robindrew.trading.multifeed.provider.cityindex.session;

import com.robindrew.trading.cityindex.platform.ICityIndexSession;

public class CityIndexSessionManager implements CityIndexSessionManagerMBean {

	private final ICityIndexSession session;

	public CityIndexSessionManager(ICityIndexSession session) {
		this.session = session;
	}

	@Override
	public String getEnvironment() {
		return session.getEnvironment().name();
	}

	@Override
	public String getUsername() {
		return session.getCredentials().getUsername();
	}

	@Override
	public String getAppKey() {
		return session.getCredentials().getAppKey();
	}

}
