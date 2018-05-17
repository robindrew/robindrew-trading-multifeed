package com.robindrew.trading.multifeed.subscriber;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import com.robindrew.trading.IInstrument;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.provider.ITradingProvider;

public class SubscriberMap implements ISubscriberMap {

	private final IInstrument instrument;
	private final Map<ITradingProvider, IInstrumentPriceStream<?>> providerToStreamMap = new ConcurrentSkipListMap<>();

	public SubscriberMap(IInstrument instrument) {
		this.instrument = instrument;
	}

	public IInstrument getInstrument() {
		return instrument;
	}

	public void register(ITradingProvider provider, IInstrumentPriceStream<?> stream) {
		providerToStreamMap.put(provider, stream);
	}

	@Override
	public int compareTo(ISubscriberMap that) {
		return this.getInstrument().compareTo(that.getInstrument());
	}

	@Override
	public IInstrumentPriceStream<?> getStream(ITradingProvider provider) {
		IInstrumentPriceStream<?> stream = providerToStreamMap.get(provider);
		// TODO: Handle null? create dummy stream?
		return stream;
	}

	@Override
	public Set<ITradingProvider> getProviders() {
		return new TreeSet<>(providerToStreamMap.keySet());
	}
}
