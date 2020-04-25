package com.robindrew.trading.multifeed.subscriber.provider;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;
import static com.robindrew.trading.cityindex.InstrumentCategory.DFT;

import java.io.File;

import com.google.common.base.Optional;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.IInstrumentRegistry;
import com.robindrew.trading.cityindex.CityIndexInstrument;
import com.robindrew.trading.cityindex.ICityIndexInstrument;
import com.robindrew.trading.cityindex.platform.ICityIndexTradingPlatform;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.platform.streaming.IStreamingService;
import com.robindrew.trading.platform.streaming.InstrumentPriceStream;
import com.robindrew.trading.price.candle.io.stream.sink.PriceCandleFileSink;
import com.robindrew.trading.provider.TradingProvider;

public class CityIndexSubscriber extends TradingProviderSubscriber<ICityIndexInstrument> {

	private final String tickOutputDirectory;

	public CityIndexSubscriber(String tickOutputDirectory) {
		super(TradingProvider.CITYINDEX);
		this.tickOutputDirectory = tickOutputDirectory;
	}

	public IInstrumentPriceStream<ICityIndexInstrument> createSubscription(IInstrument genericInstrument) {
		IInstrumentRegistry registry = getDependency(IInstrumentRegistry.class);
		Optional<ICityIndexInstrument> optional = registry.get(genericInstrument, ICityIndexInstrument.class);
		if (!optional.isPresent()) {
			CityIndexInstrument instrument = new CityIndexInstrument(genericInstrument.hashCode(), DFT, genericInstrument, 2);
			return new InstrumentPriceStream<ICityIndexInstrument>(instrument);
		}
		ICityIndexInstrument instrument = optional.get();
		ICityIndexTradingPlatform platform = getDependency(ICityIndexTradingPlatform.class);

		// Register the stream to make it available through the platform
		IStreamingService<ICityIndexInstrument> streaming = platform.getStreamingService();
		streaming.subscribeToPrices(instrument);
		IInstrumentPriceStream<ICityIndexInstrument> priceStream = streaming.getPriceStream(instrument);

		// Create the output file
		PriceCandleFileSink priceFileSink = new PriceCandleFileSink(instrument, new File(tickOutputDirectory));
		priceFileSink.start();
		priceStream.register(priceFileSink);

		return priceStream;
	}

}
