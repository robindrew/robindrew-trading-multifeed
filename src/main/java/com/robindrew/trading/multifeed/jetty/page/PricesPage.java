package com.robindrew.trading.multifeed.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.multifeed.jetty.page.view.InstrumentJson;
import com.robindrew.trading.multifeed.subscriber.ISubscriberManager;
import com.robindrew.trading.multifeed.subscriber.ISubscriberMap;

public class PricesPage extends AbstractServicePage {

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
		Set<InstrumentJson> views = new TreeSet<>();
		for (ISubscriberMap map : manager.getSubscriberMaps()) {
			views.add(new InstrumentJson(map));
		}

		// Convert to JSON for AJAX
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(views);
		System.out.println(json);
		return json;
	}

}
