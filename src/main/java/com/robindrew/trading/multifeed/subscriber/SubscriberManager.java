package com.robindrew.trading.multifeed.subscriber;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.robindrew.trading.IInstrument;

public class SubscriberManager implements ISubscriberManager {

	private final Map<IInstrument, SubscriberMap> instruments = new ConcurrentHashMap<>();

	public void register(IInstrument instrument) {
		instruments.put(instrument, new SubscriberMap(instrument));
	}

	public Set<IInstrument> getInstruments() {
		return ImmutableSet.copyOf(instruments.keySet());
	}

	public ISubscriberMap getSubscriberMap(IInstrument instrument) {
		SubscriberMap map = instruments.get(instrument);
		if (map == null) {
			throw new IllegalArgumentException("instrument not registered: " + instrument);
		}
		return map;
	}

	@Override
	public Set<ISubscriberMap> getSubscriberMaps() {
		return new TreeSet<>(instruments.values());
	}

}
