package com.robindrew.trading.multifeed.provider.igindex.session;

import com.robindrew.trading.igindex.platform.IgSession;

public class IgSessionManager implements IgSessionManagerMBean {

	private final IgSession session;

	public IgSessionManager(IgSession session) {
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
	public String getApiKey() {
		return session.getCredentials().getApiKey();
	}

}
