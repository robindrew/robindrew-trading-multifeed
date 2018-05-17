package com.robindrew.trading.multifeed.subscriber;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;
import static com.robindrew.common.dependency.DependencyFactory.setDependency;
import static com.robindrew.trading.Instruments.AUD_USD;
import static com.robindrew.trading.Instruments.BRENT_CRUDE_OIL;
import static com.robindrew.trading.Instruments.DOW_JONES_30;
import static com.robindrew.trading.Instruments.EUR_JPY;
import static com.robindrew.trading.Instruments.EUR_USD;
import static com.robindrew.trading.Instruments.FTSE_100;
import static com.robindrew.trading.Instruments.GBP_USD;
import static com.robindrew.trading.Instruments.USD_CHF;
import static com.robindrew.trading.Instruments.USD_JPY;
import static com.robindrew.trading.Instruments.US_CRUDE_OIL;
import static com.robindrew.trading.Instruments.XAG_USD;
import static com.robindrew.trading.Instruments.XAU_USD;
import static com.robindrew.trading.provider.TradingProvider.IGINDEX;
import static com.robindrew.trading.provider.TradingProvider.OANDA;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.InstrumentRegistry;
import com.robindrew.trading.fxcm.FxcmInstrument;
import com.robindrew.trading.igindex.IIgInstrument;
import com.robindrew.trading.igindex.IgInstrument;
import com.robindrew.trading.igindex.platform.IIgTradingPlatform;
import com.robindrew.trading.oanda.IOandaInstrument;
import com.robindrew.trading.oanda.OandaInstrument;
import com.robindrew.trading.oanda.platform.IOandaTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;

public class SubscriberComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(SubscriberComponent.class);

	private static final IProperty<String> propertyOandaTickOutputDir = new StringProperty("oanda.tick.output.dir");
	private static final IProperty<String> propertyIgIndexTickOutputDir = new StringProperty("igindex.tick.output.dir");

	@Override
	protected void startupComponent() throws Exception {

		// Initialise instrument registry
		log.info("Registering Instruments");
		InstrumentRegistry registry = new InstrumentRegistry();
		registry.register(IgInstrument.class);
		registry.register(OandaInstrument.class);
		registry.register(FxcmInstrument.class);
		setDependency(IInstrumentRegistry.class, registry);

		// Initialise manager
		SubscriberManager manager = new SubscriberManager();
		setDependency(ISubscriberManager.class, manager);

		// Currencies
		manager.register(AUD_USD);
		manager.register(EUR_JPY);
		manager.register(EUR_USD);
		manager.register(GBP_USD);
		manager.register(USD_CHF);
		manager.register(USD_JPY);

		// Indices
		manager.register(FTSE_100);
		manager.register(DOW_JONES_30);

		// Commodities
		manager.register(XAU_USD);
		manager.register(XAG_USD);
		manager.register(US_CRUDE_OIL);
		manager.register(BRENT_CRUDE_OIL);

		// Initialise subscriptions
		createOandaSubscriptions(manager);
		createIgSubscriptions(manager);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

	private void createOandaSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			IInstrumentPriceStream<IOandaInstrument> stream = createOandaSubscription(instrument);
			manager.getSubscriberMap(instrument).register(OANDA, stream);
		}
	}

	private void createIgSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			IInstrumentPriceStream<IIgInstrument> stream = createIgSubscription(instrument);
			manager.getSubscriberMap(instrument).register(IGINDEX, stream);
		}
	}

	private IInstrumentPriceStream<IOandaInstrument> createOandaSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		IOandaInstrument instrument = registry.get(genericInstrument, IOandaInstrument.class);

		IOandaTradingPlatform platform = getDependency(IOandaTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IOandaInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IOandaInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyOandaTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

	private IInstrumentPriceStream<IIgInstrument> createIgSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		IIgInstrument instrument = registry.get(genericInstrument, IIgInstrument.class);

		IIgTradingPlatform platform = getDependency(IIgTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IIgInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IIgInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyIgIndexTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
