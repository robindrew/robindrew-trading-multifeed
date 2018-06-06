package com.robindrew.trading.multifeed.subscriber;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.InstrumentRegistry;
import com.robindrew.trading.cityindex.CityIndexInstrument;
import com.robindrew.trading.fxcm.FxcmInstrument;
import com.robindrew.trading.igindex.IgIndexInstrument;
import com.robindrew.trading.multifeed.subscriber.provider.CityIndexSubscriber;
import com.robindrew.trading.multifeed.subscriber.provider.FxcmSubscriber;
import com.robindrew.trading.multifeed.subscriber.provider.IgIndexSubscriber;
import com.robindrew.trading.multifeed.subscriber.provider.OandaSubscriber;
import com.robindrew.trading.oanda.OandaInstrument;

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
		new FxcmSubscriber(propertyFxcmTickOutputDir.get()).createSubscriptions(manager);
		new OandaSubscriber(propertyOandaTickOutputDir.get()).createSubscriptions(manager);
		new IgIndexSubscriber(propertyIgIndexTickOutputDir.get()).createSubscriptions(manager);
		new CityIndexSubscriber(propertyCityIndexTickOutputDir.get()).createSubscriptions(manager);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
