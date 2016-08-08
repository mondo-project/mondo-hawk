/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.server.users.servlet;

import java.io.File;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import uk.ac.york.mondo.integration.server.users.servlet.db.UserStorage;

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
