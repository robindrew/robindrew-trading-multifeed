package com.robindrew.trading.multifeed.subscriber;

import java.util.Set;

import com.robindrew.trading.IInstrument;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.provider.ITradingProvider;

public interface ISubscriberMap extends Comparable<ISubscriberMap> {

	IInstrument getInstrument();

	Set<ITradingProvider> getProviders();

	IInstrumentPriceStream<?> getStream(ITradingProvider provider);

	void register(ITradingProvider provider, IInstrumentPriceStream<?> stream);

}
