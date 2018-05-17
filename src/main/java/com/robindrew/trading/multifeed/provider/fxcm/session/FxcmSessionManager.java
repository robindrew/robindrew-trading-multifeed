package com.robindrew.trading.multifeed.provider.fxcm.session;

import com.robindrew.trading.fxcm.platform.FxcmSession;

public class FxcmSessionManager implements FxcmSessionManagerMBean {

	private final FxcmSession session;

	public FxcmSessionManager(FxcmSession session) {
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

}
