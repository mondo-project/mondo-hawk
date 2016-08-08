package org.hawk.service.servlet.servlets;

import org.hawk.service.servlet.Activator;
import org.hawk.service.servlet.processors.HawkThriftProcessorFactory;

public abstract class HawkThriftServlet extends RequestAwareThriftServlet {
	private static final long serialVersionUID = 1L;

	public HawkThriftServlet(HawkThriftProcessorFactory factory) {
		super(factory, factory.getProtocol().getProtocolFactory());
		factory.setArtemisServer(Activator.getInstance().getArtemisServer());
	}
}
