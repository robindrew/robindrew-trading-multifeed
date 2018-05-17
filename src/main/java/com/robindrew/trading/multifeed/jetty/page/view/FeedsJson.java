package com.robindrew.trading.multifeed.jetty.page.view;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class FeedsJson {

	private final List<InstrumentJson> instruments;

	public FeedsJson(Collection<? extends InstrumentJson> instruments) {
		this.instruments = ImmutableList.copyOf(instruments);
	}

	public List<InstrumentJson> getInstruments() {
		return instruments;
	}

}
