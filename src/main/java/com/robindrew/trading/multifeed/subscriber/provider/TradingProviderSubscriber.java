package com.robindrew.trading.multifeed.subscriber.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.trading.IInstrument;
import com.robindrew.trading.multifeed.subscriber.SubscriberManager;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.provider.ITradingProvider;

public abstract class TradingProviderSubscriber<I extends IInstrument> {

	private static final Logger log = LoggerFactory.getLogger(TradingProviderSubscriber.class);

	private final ITradingProvider provider;

	protected TradingProviderSubscriber(ITradingProvider provider) {
		this.provider = provider;
	}

	public ITradingProvider getProvider() {
		return provider;
	}

	public void createSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			try {
				IInstrumentPriceStream<I> stream = createSubscription(instrument);
				manager.getSubscriberMap(instrument).register(provider, stream);
			} catch (Exception e) {
				log.warn("Unable to subscribe instrument: " + instrument, e);
			}
		}
	}

	protected abstract IInstrumentPriceStream<I> createSubscription(IInstrument instrument);

}
