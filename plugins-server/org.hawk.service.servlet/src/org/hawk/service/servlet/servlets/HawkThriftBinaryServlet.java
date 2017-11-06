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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.servlet.processors.HawkThriftProcessorFactory;

/**
 * Servlet that exposes {@link HawkThriftIface} through the {@link TBinaryProtocol}.
 */
public class HawkThriftBinaryServlet extends HawkThriftServlet {
	private static final long serialVersionUID = 1L;

	public HawkThriftBinaryServlet() throws Exception {
		super(new HawkThriftProcessorFactory(ThriftProtocol.BINARY));
	}
}