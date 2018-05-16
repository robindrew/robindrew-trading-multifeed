package com.robindrew.trading.multifeed.instrument;

import static com.robindrew.common.dependency.DependencyFactory.setDependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.robindrew.common.service.component.AbstractIdleComponent;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.InstrumentRegistry;
import com.robindrew.trading.fxcm.FxcmInstrument;
import com.robindrew.trading.igindex.IgInstrument;
import com.robindrew.trading.oanda.OandaInstrument;

public class InstrumentComponent extends AbstractIdleComponent {

	private static final Logger log = LoggerFactory.getLogger(InstrumentComponent.class);

	@Override
	protected void startupComponent() throws Exception {

		// Initialise instrument registry
		log.info("Registering Instruments");
		InstrumentRegistry registry = new InstrumentRegistry();

		// Register instruments
		registry.register(IgInstrument.class);
		registry.register(OandaInstrument.class);
		registry.register(FxcmInstrument.class);

		setDependency(IInstrumentRegistry.class, registry);
	}

	@Override
	protected void shutdownComponent() throws Exception {
		// TODO: Cancel all subscriptions here
	}

}
