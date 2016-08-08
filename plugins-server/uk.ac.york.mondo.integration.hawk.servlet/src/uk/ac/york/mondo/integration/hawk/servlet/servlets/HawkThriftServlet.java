package uk.ac.york.mondo.integration.hawk.servlet.servlets;

import uk.ac.york.mondo.integration.hawk.servlet.Activator;
import uk.ac.york.mondo.integration.hawk.servlet.processors.HawkThriftProcessorFactory;

public abstract class HawkThriftServlet extends RequestAwareThriftServlet {
	private static final long serialVersionUID = 1L;

	public HawkThriftServlet(HawkThriftProcessorFactory factory) {
		super(factory, factory.getProtocol().getProtocolFactory());
		factory.setArtemisServer(Activator.getInstance().getArtemisServer());
	}
}
