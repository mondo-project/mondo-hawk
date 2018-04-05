/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
