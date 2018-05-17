package com.robindrew.trading.multifeed.jetty.page;

import static com.robindrew.common.dependency.DependencyFactory.getDependency;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.http.servlet.request.IHttpRequest;
import com.robindrew.common.http.servlet.response.IHttpResponse;
import com.robindrew.common.service.component.jetty.handler.page.AbstractServicePage;
import com.robindrew.trading.multifeed.jetty.page.view.InstrumentJson;
import com.robindrew.trading.multifeed.jetty.page.view.PriceJson;
import com.robindrew.trading.multifeed.subscriber.ISubscriberManager;
import com.robindrew.trading.multifeed.subscriber.ISubscriberMap;

public class FeedsPage extends AbstractServicePage {

	public FeedsPage(IVelocityHttpContext context, String templateName) {
		super(context, templateName);
	}

	@Override
	protected void execute(IHttpRequest request, IHttpResponse response, Map<String, Object> dataMap) {
		super.execute(request, response, dataMap);

		ISubscriberManager manager = getDependency(ISubscriberManager.class);

		Set<String> providers = new LinkedHashSet<>();
		dataMap.put("instruments", getInstruments(manager, providers));
		dataMap.put("providers", providers);
	}

	private Set<InstrumentJson> getInstruments(ISubscriberManager manager, Set<String> providers) {
		Set<InstrumentJson> views = new TreeSet<>();
		for (ISubscriberMap map : manager.getSubscriberMaps()) {
			InstrumentJson json = new InstrumentJson(map);
			views.add(json);
			for (PriceJson price : json.getPrices()) {
				providers.add(price.getProvider());
			}
		}
		return views;
	}

}
