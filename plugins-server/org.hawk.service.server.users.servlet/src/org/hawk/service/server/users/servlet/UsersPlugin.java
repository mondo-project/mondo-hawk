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
package org.hawk.service.server.users.servlet;

import java.io.File;

import org.hawk.service.server.users.servlet.db.UserStorage;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class UsersPlugin implements BundleActivator {

	private static BundleContext context;
	private static UsersPlugin instance;

	static BundleContext getContext() {
		return context;
	}

	private UserStorage storage;

	public void start(BundleContext bundleContext) throws Exception {
		UsersPlugin.context = bundleContext;
		UsersPlugin.instance = this;
		File dataFile = FrameworkUtil.getBundle(UsersPlugin.class).getDataFile("users.db");
		storage = new UserStorage(dataFile);

	}

	public void stop(BundleContext bundleContext) throws Exception {
		UsersPlugin.context = null;
		UsersPlugin.instance = null;
		storage.close();
	}

	public static UsersPlugin getInstance() {
		return instance;
	}

	public UserStorage getStorage() {
		return storage;
	}
}
