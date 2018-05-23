package com.robindrew.trading.multifeed.provider.cityindex.connection;

import com.robindrew.trading.cityindex.platform.rest.executor.login.LoginResponse;

public interface ICityIndexConnectionManager {

	boolean login();

	boolean isLoggedIn();

	LoginResponse getLoginDetails();

}
