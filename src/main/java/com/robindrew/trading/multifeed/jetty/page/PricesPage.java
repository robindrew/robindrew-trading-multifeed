package com.robindrew.trading.multifeed.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.multifeed.jetty.page.view.FeedsJson;
import com.robindrew.trading.multifeed.jetty.page.view.InstrumentJson;
import com.robindrew.trading.multifeed.subscriber.ISubscriberManager;
import com.robindrew.trading.multifeed.subscriber.ISubscriberMap;

public class PricesPage extends AbstractServicePage {

	private static final Logger log = LoggerFactory.getLogger(PricesPage.class);

	public PricesPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		ISubscriberManager manager = getDependency(ISubscriberManager.class);
		dataMap.put("instruments", getInstrumentsJson(manager));
	}

	private String getInstrumentsJson(ISubscriberManager manager) {
		List<InstrumentJson> views = new ArrayList<>();
		for (ISubscriberMap map : manager.getSubscriberMaps()) {
			views.add(new InstrumentJson(map));
		}

		// Convert to JSON for AJAX
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(new FeedsJson(views));
		log.debug("[JSON]\n{}", json);
		return json;
	}

}
