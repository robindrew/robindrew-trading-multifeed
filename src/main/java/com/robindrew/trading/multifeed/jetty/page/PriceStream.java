package com.robindrew.trading.multifeed.jetty.page;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import com.google.common.collect.ImmutableList;
import com.robindrew.common.text.LineBuilder;
import com.robindrew.common.util.Check;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.multifeed.subscriber.ISubscriberManager;
import com.robindrew.trading.multifeed.subscriber.ISubscriberMap;
import com.robindrew.trading.platform.streaming.IInstrumentPriceStream;
import com.robindrew.trading.price.candle.IPriceCandle;
import com.robindrew.trading.price.candle.ITickPriceCandle;
import com.robindrew.trading.price.candle.io.stream.sink.IPriceCandleStreamSink;
import com.robindrew.trading.provider.ITradingProvider;

public class PriceStream implements AutoCloseable {

	private final BlockingDeque<Update> updateQueue = new LinkedBlockingDeque<>();
	private final Map<IInstrumentPriceStream<?>, UpdateSink> sinkRegistry = new ConcurrentHashMap<>();

	private final ITradingProvider provider;
	private final List<IInstrument> instruments;

	public PriceStream(ITradingProvider provider, List<IInstrument> instruments, ISubscriberManager manager) {
		Check.notNull("manager", manager);
		this.provider = Check.notNull("provider", provider);
		this.instruments = ImmutableList.copyOf(Check.notEmpty("instruments", instruments));

		for (IInstrument instrument : instruments) {
			ISubscriberMap map = manager.getSubscriberMap(instrument);
			IInstrumentPriceStream<?> stream = map.getStream(provider);

			UpdateSink sink = new UpdateSink(provider, instrument);
			stream.register(sink);
			sinkRegistry.put(stream, sink);
		}
	}

	public ITradingProvider getProvider() {
		return provider;
	}

	public List<IInstrument> getInstruments() {
		return instruments;
	}

	/**
	 * It is important this class is closed to unregister price subscriptions.
	 */
	@Override
	public void close() {
		for (Entry<IInstrumentPriceStream<?>, UpdateSink> entry : sinkRegistry.entrySet()) {
			IInstrumentPriceStream<?> stream = entry.getKey();
			UpdateSink sink = entry.getValue();
			stream.unregister(sink);
		}
		sinkRegistry.clear();
	}

	/**
	 * Blocking call to get the next JSON formatted price line.
	 */
	public String nextLine() throws InterruptedException {
		Update update = updateQueue.takeFirst();
		return update.toJsonLine();
	}

	/**
	 * A sink that feeds price updates direct to the blocking queue.
	 */
	private class UpdateSink implements IPriceCandleStreamSink {

		private final ITradingProvider provider;
		private final IInstrument instrument;

		public UpdateSink(ITradingProvider provider, IInstrument instrument) {
			this.provider = provider;
			this.instrument = instrument;
		}

		@Override
		public String getName() {
			return provider + "[" + instrument + "]";
		}

		@Override
		public void close() {
		}

		@Override
		public void putNextCandle(IPriceCandle candle) {
			updateQueue.addLast(new Update(instrument, candle));
		}
	}

	/**
	 * An atomic price update, with a method that converts it to a JSON formatted line.
	 */
	private class Update {

		private final IInstrument instrument;
		private final IPriceCandle candle;

		private Update(IInstrument instrument, IPriceCandle candle) {
			this.instrument = instrument;
			this.candle = candle;
		}

		public String toJsonLine() {
			LineBuilder line = new LineBuilder();
			line.append("{");

			// Tick
			if (candle instanceof ITickPriceCandle) {
				line.append("\"type\":\"tick\", ");
				line.append("\"provider\":\"").append(getProvider().name()).append("\", ");
				line.append("\"instrument\":\"").append(instrument.getName()).append("\", ");
				line.append("\"price\":").append(candle.getMidClose());
			}

			// Candle
			else {
				line.append("\"type\":\"candle\", ");
				line.append("\"provider\":\"").append(getProvider().name()).append("\", ");
				line.append("\"instrument\":\"").append(instrument.getName()).append("\", ");
				line.append("\"open\":").append(candle.getMidOpen()).append(", ");
				line.append("\"high\":").append(candle.getMidHigh()).append(", ");
				line.append("\"low\":").append(candle.getMidLow()).append(", ");
				line.append("\"close\":").append(candle.getMidClose());
			}

			line.append("}\r\n");
			return line.toString();
		}
	}

}
