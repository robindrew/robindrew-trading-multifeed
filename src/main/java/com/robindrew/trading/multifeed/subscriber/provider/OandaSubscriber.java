package com.robindrew.trading.multifeed.subscriber.provider;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.io.File;

import com.google.common.base.Optional;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.oanda.IOandaInstrument;
import com.robindrew.trading.oanda.OandaInstrument;
import com.robindrew.trading.oanda.platform.IOandaTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.platform.streaming.InstrumentPriceStream;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;
import com.robindrew.trading.provider.TradingProvider;

public class OandaSubscriber extends TradingProviderSubscriber<IOandaInstrument> {

	private final String tickOutputDirectory;

	public OandaSubscriber(String tickOutputDirectory) {
		super(TradingProvider.OANDA);
		this.tickOutputDirectory = tickOutputDirectory;
	}

	public IInstrumentPriceStream<IOandaInstrument> createSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<IOandaInstrument> optional = registry.get(genericInstrument, IOandaInstrument.class);
		if (!optional.isPresent()) {
			OandaInstrument instrument = new OandaInstrument(genericInstrument.getName(), genericInstrument);
			return new InstrumentPriceStream<IOandaInstrument>(instrument);
		}
		IOandaInstrument instrument = optional.get();
		IOandaTradingPlatform platform = getDependency(IOandaTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IOandaInstrument> streaming = platform.getStreamingService();
		streaming.subscribe(instrument);
		IInstrumentPriceStream<IOandaInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(tickOutputDirectory));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
