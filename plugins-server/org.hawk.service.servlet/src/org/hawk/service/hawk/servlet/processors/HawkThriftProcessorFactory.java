package org.hawk.service.servlet.processors;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TBase;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TProcessor;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.Hawk.Iface;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.artemis.server.Server;

public class HawkThriftProcessorFactory implements IAuthenticatedProcessorFactory {
	private final ThriftProtocol protocol;

	private Server artemisServer;

	@SuppressWarnings("rawtypes")
	private Map<String, ProcessFunction<Iface, ? extends TBase>> processMap;

	public HawkThriftProcessorFactory(ThriftProtocol protocol) {
		this.protocol = protocol;
		this.processMap = new Hawk.Processor<Hawk.Iface>(new HawkThriftIface()).getProcessMapView();
	}

	@Override
	public TProcessor create(HttpServletRequest request) {
		return new TBaseProcessor<Hawk.Iface>(
			new HawkThriftIface(protocol, request, artemisServer), processMap) {};
	}

	public Server getArtemisServer() {
		return artemisServer;
	}

	public void setArtemisServer(Server artemisServer) {
		this.artemisServer = artemisServer;
	}

	public ThriftProtocol getProtocol() {
		return protocol;
	}
}
