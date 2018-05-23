package com.robindrew.trading.multifeed.provider.igindex;

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
import com.robindrew.trading.igindex.platform.IIgIndexSession;
import com.robindrew.trading.igindex.platform.IIgIndexTradingPlatform;
import com.robindrew.trading.igindex.platform.IgIndexCredentials;
import com.robindrew.trading.igindex.platform.IgIndexEnvironment;
import com.robindrew.trading.igindex.platform.IgIndexSession;
import com.robindrew.trading.igindex.platform.IgIndexTradingPlatform;
import com.robindrew.trading.igindex.platform.rest.IIgIndexRestService;
import com.robindrew.trading.igindex.platform.rest.IgIndexRestService;
import com.robindrew.trading.igindex.platform.rest.executor.getmarketnavigation.cache.IMarketNavigationCache;
import com.robindrew.trading.igindex.platform.streaming.IgIndexStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.igindex.connection.IIgIndexConnectionManager;
import com.robindrew.trading.multifeed.provider.igindex.connection.IgIndexConnectionManager;
import com.robindrew.trading.multifeed.provider.igindex.session.IgIndexSessionManager;

public class IgIndexComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(IgIndexComponent.class);

	private static final IProperty<String> propertyApiKey = new StringProperty("igindex.api.key");
	private static final IProperty<String> propertyUsername = new StringProperty("igindex.username");
	private static final IProperty<String> propertyPassword = new StringProperty("igindex.password");
	private static final IProperty<IgIndexEnvironment> propertyEnvironment = new EnumProperty<>(IgIndexEnvironment.class, "igindex.environment");
	private static final IProperty<File> propertyTransactionLogDir = new FileProperty("igindex.transaction.log.dir");

	private volatile IgIndexStreamingServiceMonitor monitor;

	@Override
	protected void startupComponent() throws Exception {
		IMBeanRegistry registry = new AnnotatedMBeanRegistry();

		String apiKey = propertyApiKey.get();
		String username = propertyUsername.get();
		String password = propertyPassword.get();
		IgIndexEnvironment environment = propertyEnvironment.get();
		File transactionLogDir = propertyTransactionLogDir.get();

		IgIndexCredentials credentials = new IgIndexCredentials(apiKey, username, password);

		log.info("Creating Session", environment);
		log.info("Environment: {}", environment);
		log.info("User: {}", credentials.getUsername());
		IIgIndexSession session = new IgIndexSession(credentials, environment);
		setDependency(IIgIndexSession.class, session);

		log.info("Creating Account Manager");
		IgIndexSessionManager sessionManager = new IgIndexSessionManager(session);
		registry.register(sessionManager);

		log.info("Creating Transaction Log");
		TransactionLog transactionLog = new TransactionLog(transactionLogDir);
		transactionLog.start();

		log.info("Creating REST Service");
		IgIndexRestService rest = new IgIndexRestService(session, transactionLog);
		setDependency(IIgIndexRestService.class, rest);
		setDependency(IMarketNavigationCache.class, rest.getMarketNavigationCache());

		log.info("Creating Trading Platform");
		IgIndexTradingPlatform platform = new IgIndexTradingPlatform(rest);
		setDependency(IIgIndexTradingPlatform.class, platform);

		log.info("Creating Connection manager");
		IIgIndexConnectionManager connectionManager = new IgIndexConnectionManager(rest, platform);
		registry.register(connectionManager);
		setDependency(IIgIndexConnectionManager.class, connectionManager);

		log.info("Logging in ...");
		connectionManager.login();

		log.info("Creating Streaming Service Monitor");
		monitor = new IgIndexStreamingServiceMonitor(platform);
		monitor.start();
	}

	public IgIndexStreamingServiceMonitor getMonitor() {
		return monitor;
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
