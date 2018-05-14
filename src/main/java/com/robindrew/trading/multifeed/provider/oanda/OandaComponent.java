package com.robindrew.trading.multifeed.provider.oanda;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;
import static com.robindrew.common.dependency.DependencyFactory.setDependency;

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
import com.robindrew.trading.igindex.IIgInstrument;
import com.robindrew.trading.igindex.IgInstrument;
import com.robindrew.trading.igindex.platform.streaming.IgStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.oanda.session.OandaSessionManager;
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

		// Currencies
		createStreamingSubscription(IgInstrument.SPOT_AUD_USD);
		createStreamingSubscription(IgInstrument.SPOT_EUR_JPY);
		createStreamingSubscription(IgInstrument.SPOT_EUR_USD);
		createStreamingSubscription(IgInstrument.SPOT_GBP_USD);
		createStreamingSubscription(IgInstrument.SPOT_USD_CHF);
		createStreamingSubscription(IgInstrument.SPOT_USD_JPY);

		// Indices
		createStreamingSubscription(IgInstrument.WEEKDAY_FTSE_100);
		createStreamingSubscription(IgInstrument.WEEKDAY_DOW_JONES);

		// Commodities
		createStreamingSubscription(IgInstrument.SPOT_SILVER);
		createStreamingSubscription(IgInstrument.SPOT_GOLD);
		createStreamingSubscription(IgInstrument.SPOT_US_CRUDE);
		createStreamingSubscription(IgInstrument.SPOT_BRENT_CRUDE);

	}

	@SuppressWarnings("unchecked")
	private void createStreamingSubscription(IIgInstrument instrument) {
		ITradingPlatform<IIgInstrument> platform = getDependency(ITradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IIgInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IIgInstrument> priceStream = streaming.getPriceStream(instrument);

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
