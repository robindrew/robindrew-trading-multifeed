package com.robindrew.trading.multifeed;

import com.robindrew.common.service.AbstractService;
import com.robindrew.common.service.component.heartbeat.HeartbeatComponent;
import com.robindrew.common.service.component.logging.LoggingComponent;
import com.robindrew.common.service.component.properties.PropertiesComponent;
import com.robindrew.common.service.component.stats.StatsComponent;
import com.robindrew.trading.multifeed.instrument.InstrumentComponent;
import com.robindrew.trading.multifeed.jetty.JettyComponent;
import com.robindrew.trading.multifeed.provider.igindex.IgIndexComponent;
import com.robindrew.trading.multifeed.provider.oanda.OandaComponent;

public class MultiFeedService extends AbstractService {

	/**
	 * Entry point for the IG Index Feed Service.
	 */
	public static void main(String[] args) {
		MultiFeedService service = new MultiFeedService(args);
		service.startAsync();
	}

	private final JettyComponent jetty = new JettyComponent();
	private final HeartbeatComponent heartbeat = new HeartbeatComponent();
	private final PropertiesComponent properties = new PropertiesComponent();
	private final LoggingComponent logging = new LoggingComponent();
	private final StatsComponent stats = new StatsComponent();

	private final InstrumentComponent instrument = new InstrumentComponent();
	private final IgIndexComponent igindex = new IgIndexComponent();
	private final OandaComponent oanda = new OandaComponent();

	public MultiFeedService(String[] args) {
		super(args);
	}

	@Override
	protected void startupService() throws Exception {
		start(properties);
		start(logging);
		start(heartbeat);
		start(stats);
		start(jetty);
		start(instrument);
		start(oanda);
		start(igindex);
	}

	@Override
	protected void shutdownService() throws Exception {
		stop(jetty);
		stop(heartbeat);
	}
}
