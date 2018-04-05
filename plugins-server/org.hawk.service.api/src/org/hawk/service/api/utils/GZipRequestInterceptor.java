/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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