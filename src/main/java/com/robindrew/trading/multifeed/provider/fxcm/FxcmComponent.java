package com.robindrew.trading.multifeed.provider.fxcm;

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
import com.robindrew.trading.fxcm.platform.FxcmCredentials;
import com.robindrew.trading.fxcm.platform.FxcmEnvironment;
import com.robindrew.trading.fxcm.platform.FxcmSession;
import com.robindrew.trading.fxcm.platform.FxcmTradingPlatform;
import com.robindrew.trading.fxcm.platform.IFxcmSession;
import com.robindrew.trading.fxcm.platform.IFxcmTradingPlatform;
import com.robindrew.trading.fxcm.platform.api.java.FxcmJavaService;
import com.robindrew.trading.fxcm.platform.api.java.gateway.FxcmGateway;
import com.robindrew.trading.fxcm.platform.api.java.streaming.IFxcmStreamingService;
import com.robindrew.trading.igindex.platform.streaming.IgStreamingServiceMonitor;
import com.robindrew.trading.log.TransactionLog;
import com.robindrew.trading.multifeed.provider.fxcm.session.FxcmSessionManager;

public class FxcmComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(FxcmComponent.class);

	private static final IProperty<String> propertyUsername = new StringProperty("fxcm.username");
	private static final IProperty<String> propertyPassword = new StringProperty("fxcm.password");
	private static final IProperty<FxcmEnvironment> propertyEnvironment = new EnumProperty<>(FxcmEnvironment.class, "oanda.environment");
	private static final IProperty<File> propertyTransactionLogDir = new FileProperty("fxcm.transaction.log.dir");

	private volatile IgStreamingServiceMonitor monitor;

	@Override
	protected void startupComponent() throws Exception {
		IMBeanRegistry registry = new AnnotatedMBeanRegistry();

		String username = propertyUsername.get();
		String password = propertyPassword.get();
		FxcmEnvironment environment = propertyEnvironment.get();
		File transactionLogDir = propertyTransactionLogDir.get();

		FxcmCredentials credentials = new FxcmCredentials(username, password);

		log.info("Creating Session", environment);
		log.info("Environment: {}", environment);
		log.info("Username: {}", credentials.getUsername());
		FxcmSession session = new FxcmSession(credentials, environment);
		setDependency(IFxcmSession.class, session);

		log.info("Creating Session Manager");
		FxcmSessionManager sessionManager = new FxcmSessionManager(session);
		registry.register(sessionManager);

		log.info("Creating Transaction Log");
		TransactionLog transactionLog = new TransactionLog(transactionLogDir);
		transactionLog.start();

		log.info("Creating Gateway");
		FxcmGateway gateway = new FxcmGateway(transactionLog);
		FxcmJavaService service = new FxcmJavaService(session, gateway, transactionLog);
		service.login();

		log.info("Creating Trading Platform");
		FxcmTradingPlatform platform = new FxcmTradingPlatform(service);
		setDependency(IFxcmTradingPlatform.class, platform);

		log.info("Register Streaming Service");
		IFxcmStreamingService streaming = platform.getStreamingService();
		gateway.setTickHandler(streaming);
	}

	public IgStreamingServiceMonitor getMonitor() {
		return monitor;
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
