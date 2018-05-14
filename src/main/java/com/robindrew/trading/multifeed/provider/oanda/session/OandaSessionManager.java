package com.robindrew.trading.multifeed.provider.oanda.session;

import com.robindrew.trading.oanda.platform.OandaSession;

public class OandaSessionManager implements OandaSessionManagerMBean {

	private final OandaSession session;

	public OandaSessionManager(OandaSession session) {
		this.session = session;
	}

	@Override
	public String getEnvironment() {
		return session.getEnvironment().name();
	}

	@Override
	public String getAccountId() {
		return session.getCredentials().getAccountId();
	}

	@Override
	public String getToken() {
		return session.getCredentials().getToken();
	}

}
