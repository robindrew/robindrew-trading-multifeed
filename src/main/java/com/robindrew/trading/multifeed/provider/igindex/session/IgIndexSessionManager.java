package com.robindrew.trading.multifeed.provider.igindex.session;

import com.robindrew.trading.igindex.platform.IIgIndexSession;

public class IgIndexSessionManager implements IgIndexSessionManagerMBean {

	private final IIgIndexSession session;

	public IgIndexSessionManager(IIgIndexSession session) {
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
