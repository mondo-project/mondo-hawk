/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
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
