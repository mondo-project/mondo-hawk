/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.servlet.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServlet;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TTransport;
import org.hawk.service.servlet.processors.IAuthenticatedProcessorFactory;

/**
 * Specialized version of a {@link TServlet} which creates new
 * processors on the fly for each request, so they can take into
 * account the {@link HttpServletRequest} object in addition to
 * the Thrift message itself. This is useful for making the
 * operations aware of the authenticated user, for instance.
 */
public class RequestAwareThriftServlet extends HttpServlet {
	private static final long serialVersionUID = 2824688119881763947L;

	private final IAuthenticatedProcessorFactory processorFactory;
	private final TProtocolFactory inProtocolFactory;
	private final TProtocolFactory outProtocolFactory;
	private final Collection<Map.Entry<String, String>> customHeaders;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RequestAwareThriftServlet(IAuthenticatedProcessorFactory processorFactory, TProtocolFactory inProtocolFactory,
			TProtocolFactory outProtocolFactory) {
		this.processorFactory = processorFactory;
		this.inProtocolFactory = inProtocolFactory;
		this.outProtocolFactory = outProtocolFactory;
		this.customHeaders = new ArrayList<Map.Entry<String, String>>();
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public RequestAwareThriftServlet(IAuthenticatedProcessorFactory processorFactory, TProtocolFactory protocolFactory) {
		this(processorFactory, protocolFactory, protocolFactory);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		TTransport inTransport = null;
		TTransport outTransport = null;

		try {
			response.setContentType("application/x-thrift");

			if (null != this.customHeaders) {
				for (Map.Entry<String, String> header : this.customHeaders) {
					response.addHeader(header.getKey(), header.getValue());
				}
			}
			InputStream in = request.getInputStream();
			OutputStream out = response.getOutputStream();

			TTransport transport = new TIOStreamTransport(in, out);
			inTransport = transport;
			outTransport = transport;

			TProtocol inProtocol = inProtocolFactory.getProtocol(inTransport);
			TProtocol outProtocol = outProtocolFactory.getProtocol(outTransport);

			final TProcessor processor = processorFactory.create(request);
			processor.process(inProtocol, outProtocol);
			out.flush();
		} catch (TException te) {
			throw new ServletException(te);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	public void addCustomHeader(final String key, final String value) {
		this.customHeaders.add(new Map.Entry<String, String>() {
			public String getKey() {
				return key;
			}

			public String getValue() {
				return value;
			}

			public String setValue(String value) {
				return null;
			}
		});
	}

	public void setCustomHeaders(Collection<Map.Entry<String, String>> headers) {
		this.customHeaders.clear();
		this.customHeaders.addAll(headers);
	}
}
