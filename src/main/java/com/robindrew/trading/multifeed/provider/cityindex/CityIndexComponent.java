package com.robindrew.trading.multifeed.provider.cityindex;

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
import com.robindrew.trading.cityindex.platform.CityIndexCredentials;
import com.robindrew.trading.cityindex.platform.CityIndexEnvironment;
import com.robindrew.trading.cityindex.platform.CityIndexSession;
import com.robindrew.trading.cityindex.platform.CityIndexTradingPlatform;
import com.robindrew.trading.cityindex.platform.ICityIndexSession;
import com.robindrew.trading.cityindex.platform.ICityIndexTradingPlatform;
import com.robindrew.trading.cityindex.platform.rest.CityIndexRestService;
import com.robindrew.trading.cityindex.platform.rest.ICityIndexRestService;
import com.robindrew.trading.cityindex.platform.streaming.CityIndexStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.cityindex.connection.CityIndexConnectionManager;
import com.robindrew.trading.multifeed.provider.cityindex.connection.ICityIndexConnectionManager;
import com.robindrew.trading.multifeed.provider.cityindex.session.CityIndexSessionManager;

public class CityIndexComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(CityIndexComponent.class);

	private static final IProperty<String> propertyAppKey = new StringProperty("cityindex.app.key");
	private static final IProperty<String> propertyUsername = new StringProperty("cityindex.username");
	private static final IProperty<String> propertyPassword = new StringProperty("cityindex.password");
	private static final IProperty<CityIndexEnvironment> propertyEnvironment = new EnumProperty<>(CityIndexEnvironment.class, "cityindex.environment");
	private static final IProperty<File> propertyTransactionLogDir = new FileProperty("cityindex.transaction.log.dir");

	private volatile CityIndexStreamingServiceMonitor monitor;

	@Override
	protected void startupComponent() throws Exception {
		IMBeanRegistry registry = new AnnotatedMBeanRegistry();

		String appKey = propertyAppKey.get();
		String username = propertyUsername.get();
		String password = propertyPassword.get();
		CityIndexEnvironment environment = propertyEnvironment.get();
		File transactionLogDir = propertyTransactionLogDir.get();

		CityIndexCredentials credentials = new CityIndexCredentials(appKey, username, password);

		log.info("Creating Session", environment);
		log.info("Environment: {}", environment);
		log.info("User: {}", credentials.getUsername());
		ICityIndexSession session = new CityIndexSession(credentials, environment);
		setDependency(ICityIndexSession.class, session);

		log.info("Creating Account Manager");
		CityIndexSessionManager sessionManager = new CityIndexSessionManager(session);
		registry.register(sessionManager);

		log.info("Creating Transaction Log");
		TransactionLog transactionLog = new TransactionLog(transactionLogDir);
		transactionLog.start();

		log.info("Creating REST Service");
		CityIndexRestService rest = new CityIndexRestService(session, transactionLog);
		setDependency(ICityIndexRestService.class, rest);

		log.info("Creating Trading Platform");
		ICityIndexTradingPlatform platform = new CityIndexTradingPlatform(rest);
		setDependency(ICityIndexTradingPlatform.class, platform);

		log.info("Creating Connection manager");
		ICityIndexConnectionManager connectionManager = new CityIndexConnectionManager(rest, platform);
		registry.register(connectionManager);
		setDependency(ICityIndexConnectionManager.class, connectionManager);

		log.info("Logging in ...");
		connectionManager.login();

		log.info("Creating Streaming Service Monitor");
		monitor = new CityIndexStreamingServiceMonitor(platform);
		monitor.start();
	}

	public CityIndexStreamingServiceMonitor getMonitor() {
		return monitor;
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
