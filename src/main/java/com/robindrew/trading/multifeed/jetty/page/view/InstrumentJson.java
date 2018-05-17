package com.robindrew.trading.multifeed.jetty.page.view;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.CompareToBuilder;

import com.google.common.collect.ImmutableList;
import com.robindrew.trading.multifeed.subscriber.ISubscriberMap;
import com.robindrew.trading.provider.ITradingProvider;

public class InstrumentJson implements Comparable<InstrumentJson> {

	private final String name;
	private final String type;
	private final List<PriceJson> prices = new ArrayList<>();

	public InstrumentJson(ISubscriberMap map) {
		this.name = map.getInstrument().getName();
		this.type = map.getInstrument().getType().name();

		for (ITradingProvider provider : map.getProviders()) {
			prices.add(new PriceJson(provider, map.getStream(provider)));
		}
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public List<PriceJson> getPrices() {
		return ImmutableList.copyOf(prices);
	}

	@Override
	public int compareTo(InstrumentJson that) {
		CompareToBuilder compare = new CompareToBuilder();
		compare.append(this.getType(), that.getType());
		compare.append(this.getName(), that.getName());
		return compare.toComparison();
	}

}
