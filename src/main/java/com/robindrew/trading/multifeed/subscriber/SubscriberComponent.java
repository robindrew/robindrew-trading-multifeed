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
import static com.robindrew.trading.cityindex.InstrumentCategory.DFT;
import static com.robindrew.trading.provider.TradingProvider.CITYINDEX;
import static com.robindrew.trading.provider.TradingProvider.FXCM;
import static com.robindrew.trading.provider.TradingProvider.IGINDEX;
import static com.robindrew.trading.provider.TradingProvider.OANDA;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.InstrumentRegistry;
import com.robindrew.trading.cityindex.CityIndexInstrument;
import com.robindrew.trading.cityindex.ICityIndexInstrument;
import com.robindrew.trading.cityindex.platform.ICityIndexTradingPlatform;
import com.robindrew.trading.fxcm.FxcmInstrument;
import com.robindrew.trading.fxcm.IFxcmInstrument;
import com.robindrew.trading.fxcm.platform.IFxcmTradingPlatform;
import com.robindrew.trading.igindex.IIgIndexInstrument;
import com.robindrew.trading.igindex.IgIndexInstrument;
import com.robindrew.trading.igindex.platform.IIgIndexTradingPlatform;
import com.robindrew.trading.oanda.IOandaInstrument;
import com.robindrew.trading.oanda.OandaInstrument;
import com.robindrew.trading.oanda.platform.IOandaTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.platform.streaming.InstrumentPriceStream;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;

public class SubscriberComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(SubscriberComponent.class);

	private static final IProperty<String> propertyFxcmTickOutputDir = new StringProperty("fxcm.tick.output.dir");
	private static final IProperty<String> propertyOandaTickOutputDir = new StringProperty("oanda.tick.output.dir");
	private static final IProperty<String> propertyIgIndexTickOutputDir = new StringProperty("igindex.tick.output.dir");
	private static final IProperty<String> propertyCityIndexTickOutputDir = new StringProperty("cityindex.tick.output.dir");

	@Override
	protected void startupComponent() throws Exception {

		// Initialise instrument registry
		log.info("Registering Instruments");
		InstrumentRegistry registry = new InstrumentRegistry();
		registry.register(IgIndexInstrument.class);
		registry.register(OandaInstrument.class);
		registry.register(FxcmInstrument.class);
		registry.register(CityIndexInstrument.class);
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
		createFxcmSubscriptions(manager);
		createOandaSubscriptions(manager);
		createIgIndexSubscriptions(manager);
		createCityIndexSubscriptions(manager);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

	private void createCityIndexSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			try {
				IInstrumentPriceStream<ICityIndexInstrument> stream = createCityIndexSubscription(instrument);
				manager.getSubscriberMap(instrument).register(CITYINDEX, stream);
			} catch (Exception e) {
				log.warn("Unable to subscribe instrument: " + instrument, e);
			}
		}
	}

	private void createOandaSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			try {
				IInstrumentPriceStream<IOandaInstrument> stream = createOandaSubscription(instrument);
				manager.getSubscriberMap(instrument).register(OANDA, stream);
			} catch (Exception e) {
				log.warn("Unable to subscribe instrument: " + instrument, e);
			}
		}
	}

	private void createIgIndexSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			try {
				IInstrumentPriceStream<IIgIndexInstrument> stream = createIgIndexSubscription(instrument);
				manager.getSubscriberMap(instrument).register(IGINDEX, stream);
			} catch (Exception e) {
				log.warn("Unable to subscribe instrument: " + instrument, e);
			}
		}
	}

	private void createFxcmSubscriptions(SubscriberManager manager) {
		for (IInstrument instrument : manager.getInstruments()) {
			try {
				IInstrumentPriceStream<IFxcmInstrument> stream = createFxcmSubscription(instrument);
				manager.getSubscriberMap(instrument).register(FXCM, stream);
			} catch (Exception e) {
				log.warn("Unable to subscribe instrument: " + instrument, e);
			}
		}
	}

	private IInstrumentPriceStream<IFxcmInstrument> createFxcmSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<IFxcmInstrument> optional = registry.get(genericInstrument, IFxcmInstrument.class);
		if (!optional.isPresent()) {
			FxcmInstrument instrument = new FxcmInstrument(genericInstrument.getName(), genericInstrument, 5);
			return new InstrumentPriceStream<IFxcmInstrument>(instrument);
		}
		IFxcmInstrument instrument = optional.get();
		IFxcmTradingPlatform platform = getDependency(IFxcmTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IFxcmInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IFxcmInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyFxcmTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

	private IInstrumentPriceStream<IOandaInstrument> createOandaSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<IOandaInstrument> optional = registry.get(genericInstrument, IOandaInstrument.class);
		if (!optional.isPresent()) {
			OandaInstrument instrument = new OandaInstrument(genericInstrument.getName(), genericInstrument);
			return new InstrumentPriceStream<IOandaInstrument>(instrument);
		}
		IOandaInstrument instrument = optional.get();
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

	private IInstrumentPriceStream<IIgIndexInstrument> createIgIndexSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<IIgIndexInstrument> optional = registry.get(genericInstrument, IIgIndexInstrument.class);
		if (!optional.isPresent()) {
			IgIndexInstrument instrument = new IgIndexInstrument(genericInstrument.getName(), genericInstrument);
			return new InstrumentPriceStream<IIgIndexInstrument>(instrument);
		}
		IIgIndexInstrument instrument = optional.get();
		IIgIndexTradingPlatform platform = getDependency(IIgIndexTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IIgIndexInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IIgIndexInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyIgIndexTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

	private IInstrumentPriceStream<ICityIndexInstrument> createCityIndexSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<ICityIndexInstrument> optional = registry.get(genericInstrument, ICityIndexInstrument.class);
		if (!optional.isPresent()) {
			CityIndexInstrument instrument = new CityIndexInstrument(genericInstrument.hashCode(), DFT, genericInstrument);
			return new InstrumentPriceStream<ICityIndexInstrument>(instrument);
		}
		ICityIndexInstrument instrument = optional.get();
		ICityIndexTradingPlatform platform = getDependency(ICityIndexTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<ICityIndexInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<ICityIndexInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyCityIndexTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
