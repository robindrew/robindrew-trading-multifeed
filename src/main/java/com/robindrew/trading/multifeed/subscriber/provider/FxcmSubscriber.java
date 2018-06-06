package com.robindrew.trading.multifeed.subscriber.provider;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.io.File;

import com.google.common.base.Optional;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.fxcm.FxcmInstrument;
import com.robindrew.trading.fxcm.IFxcmInstrument;
import com.robindrew.trading.fxcm.platform.IFxcmTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.platform.streaming.InstrumentPriceStream;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;
import com.robindrew.trading.provider.TradingProvider;

public class FxcmSubscriber extends TradingProviderSubscriber<IFxcmInstrument> {

	private final String tickOutputDirectory;

	public FxcmSubscriber(String tickOutputDirectory) {
		super(TradingProvider.FXCM);
		this.tickOutputDirectory = tickOutputDirectory;
	}

	public IInstrumentPriceStream<IFxcmInstrument> createSubscription(IInstrument genericInstrument) {
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
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(tickOutputDirectory));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
