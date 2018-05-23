package com.robindrew.trading.multifeed.provider.cityindex.connection;

public interface CityIndexConnectionManagerMBean {

	boolean isLoggedIn();

	boolean login();

	boolean logout();

}
