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
package org.hawk.service.cli;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

	private static Activator instance;
	private Set<Closeable> closeables = new HashSet<>();

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		for (Closeable c : closeables) {
			try {
				c.close();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	public static Activator getInstance() {
		return instance;
	}

	public static void setInstance(Activator instance) {
		Activator.instance = instance;
	}

	public boolean addCloseable(Closeable c) {
		return closeables.add(c);
	}

	public boolean removeCloseable(Closeable c) {
		return closeables.remove(c);
	}
}
