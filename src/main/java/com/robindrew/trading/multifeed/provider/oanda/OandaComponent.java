package com.robindrew.trading.multifeed.provider.oanda;

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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.mbean.IMBeanRegistry;
import com.robindrew.common.mbean.annotated.AnnotatedMBeanRegistry;
import com.robindrew.common.properties.map.type.EnumProperty;
import com.robindrew.common.properties.map.type.FileProperty;
import com.robindrew.common.properties.map.type.IProperty;
import com.robindrew.common.properties.map.type.StringProperty;
import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.igindex.platform.streaming.IgStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.oanda.session.OandaSessionManager;
import com.robindrew.trading.oanda.IOandaInstrument;
import com.robindrew.trading.oanda.platform.IOandaSession;
import com.robindrew.trading.oanda.platform.OandaCredentials;
import com.robindrew.trading.oanda.platform.OandaEnvironment;
import com.robindrew.trading.oanda.platform.OandaSession;
import com.robindrew.trading.oanda.platform.OandaTradingPlatform;
import com.robindrew.trading.platform.ITradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;

public class OandaComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(OandaComponent.class);

	private static final IProperty<String> propertyAccountId = new StringProperty("oanda.account.id");
	private static final IProperty<String> propertyToken = new StringProperty("oanda.token");
	private static final IProperty<OandaEnvironment> propertyEnvironment = new EnumProperty<>(OandaEnvironment.class, "oanda.environment");
	private static final IProperty<String> propertyTickOutputDir = new StringProperty("oanda.tick.output.dir");
	private static final IProperty<File> propertyTransactionLogDir = new FileProperty("oanda.transaction.log.dir");

	private volatile IgStreamingServiceMonitor monitor;

	@Override
	protected void startupComponent() throws Exception {
		IMBeanRegistry registry = new AnnotatedMBeanRegistry();

		String accountId = propertyAccountId.get();
		String token = propertyToken.get();
		OandaEnvironment environment = propertyEnvironment.get();
		File transactionLogDir = propertyTransactionLogDir.get();

		OandaCredentials credentials = new OandaCredentials(accountId, token);

		log.info("Creating Session", environment);
		log.info("Environment: {}", environment);
		log.info("Account: {}", credentials.getAccountId());
		OandaSession session = new OandaSession(credentials, environment);
		setDependency(IOandaSession.class, session);

		log.info("Creating Session Manager");
		OandaSessionManager sessionManager = new OandaSessionManager(session);
		registry.register(sessionManager);

		log.info("Creating Transaction Log");
		TransactionLog transactionLog = new TransactionLog(transactionLogDir);
		transactionLog.start();

		log.info("Creating Trading Platform");
		OandaTradingPlatform platform = new OandaTradingPlatform(session);
		setDependency(ITradingPlatform.class, platform);

		log.info("Subscribing ...");
		createStreamingSubscriptions();
	}

	public IgStreamingServiceMonitor getMonitor() {
		return monitor;
	}

	private void createStreamingSubscriptions() {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);

		// Currencies
		createStreamingSubscription(registry.get(AUD_USD, IOandaInstrument.class));
		createStreamingSubscription(registry.get(EUR_JPY, IOandaInstrument.class));
		createStreamingSubscription(registry.get(EUR_USD, IOandaInstrument.class));
		createStreamingSubscription(registry.get(GBP_USD, IOandaInstrument.class));
		createStreamingSubscription(registry.get(USD_CHF, IOandaInstrument.class));
		createStreamingSubscription(registry.get(USD_JPY, IOandaInstrument.class));

		// Indices
		createStreamingSubscription(registry.get(FTSE_100, IOandaInstrument.class));
		createStreamingSubscription(registry.get(DOW_JONES_30, IOandaInstrument.class));

		// Commodities
		createStreamingSubscription(registry.get(XAU_USD, IOandaInstrument.class));
		createStreamingSubscription(registry.get(XAG_USD, IOandaInstrument.class));
		createStreamingSubscription(registry.get(US_CRUDE_OIL, IOandaInstrument.class));
		createStreamingSubscription(registry.get(BRENT_CRUDE_OIL, IOandaInstrument.class));

	}

	@SuppressWarnings("unchecked")
	private void createStreamingSubscription(IOandaInstrument instrument) {
		ITradingPlatform<IOandaInstrument> platform = getDependency(ITradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IOandaInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IOandaInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(propertyTickOutputDir.get()));
		priceFileSink.start();
		priceStream.register(priceFileSink);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
