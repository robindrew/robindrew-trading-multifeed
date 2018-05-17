package com.robindrew.trading.multifeed.subscriber;

import java.util.Set;

import com.robindrew.trading.IInstrument;

public interface ISubscriberManager {

	Set<IInstrument> getInstruments();

	Set<ISubscriberMap> getSubscriberMaps();

	ISubscriberMap getSubscriberMap(IInstrument instrument);

}
