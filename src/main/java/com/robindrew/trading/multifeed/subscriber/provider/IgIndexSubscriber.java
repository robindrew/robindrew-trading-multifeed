package com.robindrew.trading.multifeed.subscriber.provider;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.io.File;

import com.google.common.base.Optional;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.igindex.IIgIndexInstrument;
import com.robindrew.trading.igindex.IgIndexInstrument;
import com.robindrew.trading.igindex.platform.IIgIndexTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.platform.streaming.InstrumentPriceStream;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;
import com.robindrew.trading.provider.TradingProvider;

public class IgIndexSubscriber extends TradingProviderSubscriber<IIgIndexInstrument> {

	private final String tickOutputDirectory;

	public IgIndexSubscriber(String tickOutputDirectory) {
		super(TradingProvider.IGINDEX);
		this.tickOutputDirectory = tickOutputDirectory;
	}

	public IInstrumentPriceStream<IIgIndexInstrument> createSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<IIgIndexInstrument> optional = registry.get(genericInstrument, IIgIndexInstrument.class);
		if (!optional.isPresent()) {
			IgIndexInstrument instrument = new IgIndexInstrument(genericInstrument.getName(), genericInstrument);
			return new InstrumentPriceStream<IIgIndexInstrument>(instrument);
		}
		IIgIndexInstrument instrument = optional.get();
		IIgIndexTradingPlatform platform = getDependency(IIgIndexTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<IIgIndexInstrument> streaming = platform.getStreamingService();
		streaming.subscribeToPrices(instrument);
		IInstrumentPriceStream<IIgIndexInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(tickOutputDirectory));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
