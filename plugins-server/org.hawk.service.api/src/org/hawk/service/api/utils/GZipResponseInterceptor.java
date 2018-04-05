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

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

/**
 * One of the two gzip interceptors that were obtained from this example in the
 * Apache HTTP Components library:
 *
 * http://hc.apache.org/httpcomponents-client-4.2.x/httpclient/examples/org/
 * apache/http/examples/client/ClientGZipContentCompression.java
 *
 * This interceptor will decompress the contents of the response on the fly
 * if the "Content-Encoding" header has been set to "gzip".
 */
class GZipResponseInterceptor implements HttpResponseInterceptor {
	@Override
	public void process(HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			Header ceheader = entity.getContentEncoding();
			if (ceheader != null) {
				HeaderElement[] codecs = ceheader.getElements();
				for (int i = 0; i < codecs.length; i++) {
					if (codecs[i].getName().equalsIgnoreCase("gzip")) {
						response.setEntity(new GzipDecompressingEntity(
								response.getEntity()));
						return;
					}
				}
			}
		}
	}
}