package com.robindrew.trading.multifeed.jetty;

import com.google.common.base.Supplier;
import com.robindrew.common.html.Bootstrap;
import com.robindrew.common.http.servlet.executor.IHttpExecutor;
import com.robindrew.common.http.servlet.executor.IVelocityHttpContext;
import com.robindrew.common.service.component.jetty.JettyVelocityComponent;
import com.robindrew.common.service.component.jetty.handler.MatcherHttpHandler;
import com.robindrew.common.service.component.jetty.handler.page.BeanConsolePage;
import com.robindrew.common.service.component.jetty.handler.page.BeanOperationPage;
import com.robindrew.common.service.component.jetty.handler.page.BeanViewPage;
import com.robindrew.common.service.component.jetty.handler.page.GetBeanAttributePage;
import com.robindrew.common.service.component.jetty.handler.page.IndexPage;
import com.robindrew.common.service.component.jetty.handler.page.SetBeanAttributePage;
import com.robindrew.common.service.component.jetty.handler.page.SystemPage;
import com.robindrew.common.template.ITemplateLocator;
import com.robindrew.common.template.velocity.VelocityTemplateLocatorSupplier;
import com.robindrew.trading.multifeed.jetty.page.FeedsPage;
import com.robindrew.trading.multifeed.jetty.page.PriceStreamExecutor;
import com.robindrew.trading.multifeed.jetty.page.PricesPage;

public class JettyComponent extends JettyVelocityComponent {

	@Override
	protected Supplier<ITemplateLocator> getTemplateLocator() {
		return new VelocityTemplateLocatorSupplier();
	}

	@Override
	protected void populate(MatcherHttpHandler handler) {

		// Register standard pages
		handler.uri("/", newIndexPage(getContext(), "site/common/Index.html"));
		handler.uri("/System", new SystemPage(getContext(), "site/common/System.html"));
		handler.uri("/BeanConsole", new BeanConsolePage(getContext(), "site/common/BeanConsole.html"));
		handler.uri("/BeanView", new BeanViewPage(getContext(), "site/common/BeanView.html"));
		handler.uri("/GetBeanAttribute", new GetBeanAttributePage(getContext(), "site/common/GetBeanAttribute.html"));
		handler.uri("/SetBeanAttribute", new SetBeanAttributePage(getContext(), "site/common/SetBeanAttribute.html"));
		handler.uri("/BeanOperation", new BeanOperationPage(getContext(), "site/common/BeanOperation.html"));

		// Register extra pages
		handler.uri("/Feeds", new FeedsPage(getContext(), "site/multifeed/Feeds.html"));
		handler.uri("/Prices", new PricesPage(getContext(), "site/multifeed/Prices.json"));
		handler.uri("/PriceStream", new PriceStreamExecutor());
	}

	private IHttpExecutor newIndexPage(IVelocityHttpContext context, String templateName) {
		IndexPage page = new IndexPage(context, templateName);
		page.addLink("Feeds", "/Feeds", Bootstrap.COLOR_DEFAULT);
		return page;
	}

}
