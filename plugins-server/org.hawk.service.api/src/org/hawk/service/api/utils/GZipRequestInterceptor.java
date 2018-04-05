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
package org.hawk.service.api.utils;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * One of the two gzip interceptors that were obtained from this example in the
 * Apache HTTP Components library:
 *
 * http://hc.apache.org/httpcomponents-client-4.2.x/httpclient/examples/org/
 * apache/http/examples/client/ClientGZipContentCompression.java
 *
 * This interceptor will ensure that the client always asks for gzip compression.
 */
class GZipRequestInterceptor implements HttpRequestInterceptor {
	@Override
	public void process(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		if (!request.containsHeader("Accept-Encoding")) {
			request.addHeader("Accept-Encoding", "gzip");
		}
	}
}