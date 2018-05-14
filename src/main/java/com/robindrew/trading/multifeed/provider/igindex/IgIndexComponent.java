package com.robindrew.trading.multifeed.provider.igindex;

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
import com.robindrew.trading.igindex.platform.IIgSession;
import com.robindrew.trading.igindex.platform.IgCredentials;
import com.robindrew.trading.igindex.platform.IgEnvironment;
import com.robindrew.trading.igindex.platform.IgSession;
import com.robindrew.trading.igindex.platform.IgTradingPlatform;
import com.robindrew.trading.igindex.platform.rest.IIgRestService;
import com.robindrew.trading.igindex.platform.rest.IgRestService;
import com.robindrew.trading.igindex.platform.rest.executor.getmarketnavigation.cache.IMarketNavigationCache;
import com.robindrew.trading.igindex.platform.streaming.IgStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.igindex.connection.ConnectionManager;
import com.robindrew.trading.multifeed.provider.igindex.connection.IConnectionManager;
import com.robindrew.trading.multifeed.provider.igindex.session.IgSessionManager;
import com.robindrew.trading.platform.ITradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;

public class IgIndexComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(IgIndexComponent.class);

	private static final IProperty<String> propertyApiKey = new StringProperty("igindex.api.key");
	private static final IProperty<String> propertyUsername = new StringProperty("igindex.username");
	private static final IProperty<String> propertyPassword = new StringProperty("igindex.password");
	private static final IProperty<IgEnvironment> propertyEnvironment = new EnumProperty<>(IgEnvironment.class, "igindex.environment");
	private static final IProperty<String> propertyTickOutputDir = new StringProperty("igindex.tick.output.dir");
	private static final IProperty<File> propertyTransactionLogDir = new FileProperty("igindex.transaction.log.dir");

	private volatile IgStreamingServiceMonitor monitor;

	@Override
	protected void startupComponent() throws Exception {
		IMBeanRegistry registry = new AnnotatedMBeanRegistry();

		String apiKey = propertyApiKey.get();
		String username = propertyUsername.get();
		String password = propertyPassword.get();
		IgEnvironment environment = propertyEnvironment.get();
		File transactionLogDir = propertyTransactionLogDir.get();

		IgCredentials credentials = new IgCredentials(apiKey, username, password);

		log.info("Creating Session", environment);
		log.info("Environment: {}", environment);
		log.info("User: {}", credentials.getUsername());
		IgSession session = new IgSession(credentials, environment);
		setDependency(IIgSession.class, session);

		log.info("Creating Account Manager");
		IgSessionManager sessionManager = new IgSessionManager(session);
		registry.register(sessionManager);

		log.info("Creating Transaction Log");
		TransactionLog transactionLog = new TransactionLog(transactionLogDir);
		transactionLog.start();

		log.info("Creating REST Service");
		IgRestService rest = new IgRestService(session, transactionLog);
		setDependency(IIgRestService.class, rest);
		setDependency(IMarketNavigationCache.class, rest.getMarketNavigationCache());

		log.info("Creating Trading Platform");
		IgTradingPlatform platform = new IgTradingPlatform(rest);
		setDependency(ITradingPlatform.class, platform);

		log.info("Creating Connection manager");
		IConnectionManager connectionManager = new ConnectionManager(rest, platform);
		registry.register(connectionManager);
		setDependency(IConnectionManager.class, connectionManager);

		log.info("Logging in ...");
		connectionManager.login();

		log.info("Subscribing ...");
		createStreamingSubscriptions();

		log.info("Creating Streaming Service Monitor");
		monitor = new IgStreamingServiceMonitor(platform);
		monitor.start();
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
