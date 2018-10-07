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
package org.hawk.service.server.gzip;

import java.util.Dictionary;
import java.util.zip.Deflater;

import org.eclipse.equinox.http.jetty.JettyCustomizer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

public class Customizer extends JettyCustomizer {

	@Override
	public Object customizeContext(Object context, Dictionary<String, ?> settings) {
		if (context instanceof ContextHandler) {
			final GzipHandler gzipHandler = new GzipHandler();
			gzipHandler.setCompressionLevel(Deflater.BEST_COMPRESSION);
			gzipHandler.addIncludedMimeTypes("application/x-thrift");
			final ContextHandler contextHandler = (ContextHandler)context;
			contextHandler.insertHandler(gzipHandler);
		}
		return super.customizeContext(context, settings);
	}

}
