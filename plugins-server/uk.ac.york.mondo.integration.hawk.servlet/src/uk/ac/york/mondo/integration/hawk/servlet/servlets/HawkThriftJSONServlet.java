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
package uk.ac.york.mondo.integration.hawk.servlet.servlets;

import org.apache.thrift.protocol.TJSONProtocol;

import uk.ac.york.mondo.integration.api.utils.APIUtils.ThriftProtocol;
import uk.ac.york.mondo.integration.hawk.servlet.processors.HawkThriftProcessorFactory;

/**
 * Servlet that exposes {@link HawkThriftIface} through the {@link TJSONProtocol}.
 */
public class HawkThriftJSONServlet extends HawkThriftServlet {
	private static final long serialVersionUID = 1L;

	public HawkThriftJSONServlet() throws Exception {
		super(new HawkThriftProcessorFactory(ThriftProtocol.JSON));
	}
}
