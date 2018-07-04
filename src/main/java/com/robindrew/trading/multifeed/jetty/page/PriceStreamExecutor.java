package com.robindrew.trading.multifeed.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.robindrew.common.http.servlet.executor.IHttpExecutor;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.text.parser.IStringParser;
import com.robindrew.common.util.Java;
import com.robindrew.trading.IInstrument;
import com.robindrew.trading.Instruments;
import com.robindrew.trading.multifeed.subscriber.ISubscriberManager;
import com.robindrew.trading.provider.TradingProvider;

public class PriceStreamExecutor implements IHttpExecutor {

	private static final Logger log = LoggerFactory.getLogger(PriceStreamExecutor.class);

	@Override
	public void execute(IHttpRequest request, IHttpResponse response) {

		// EXAMPLE URL: "/PriceStream?provider=FXCM&instruments=EURUSD,GBPUSD,XAUUSD,AUDUSD"

		// Parameters
		TradingProvider provider = request.getEnum(TradingProvider.class, "provider");
		List<IInstrument> instruments = request.getValue(new InstrumentParser(), "instruments");

		// Lookup the manager
		ISubscriberManager manager = getDependency(ISubscriberManager.class);

		// Create the stream
		PriceStream stream = new PriceStream(provider, instruments, manager);
		log.info("[PriceStream] provider={}, instruments={}", provider, instruments);

		try {

			// Response meta data
			response.setStatus(200);
			response.setContentType("text/plain; charset=UTF-8");

			// Write to response output stream
			ServletOutputStream output = response.getOutputStream();
			Writer writer = new OutputStreamWriter(output, Charsets.UTF_8);

			// Streaming prices (one JSON formatted line per update)
			while (true) {
				String line = stream.nextLine();
				writer.write(line);
				writer.flush();
			}

		} catch (Exception e) {
			throw Java.propagate(e);

		} finally {

			// Make sure to unregister the subscriptions when the connection is closed / lost
			stream.close();
		}
	}

	private class InstrumentParser implements IStringParser<List<IInstrument>> {

		@Override
		public List<IInstrument> parse(String text) {

			// Parse the instruments
			List<IInstrument> list = new ArrayList<>();
			Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();
			for (String element : splitter.split(text)) {
				IInstrument instrument = Instruments.valueOf(element);
				list.add(instrument);
			}
			if (list.isEmpty()) {
				throw new IllegalArgumentException("Unable to parse instruments from: '" + text + "'");
			}

			return list;
		}
	}

}
