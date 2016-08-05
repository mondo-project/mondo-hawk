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
package org.hawk.ui.emfresource;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class Activator extends AbstractUIPlugin {

	private static Activator plugin;

	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		super.stop(bundleContext);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Logs an exception to the Error view.
	 */
	public static void logError(String msg, Throwable t) {
		final String bundleName = FrameworkUtil.getBundle(Activator.class).getSymbolicName();
		getDefault().getLog().log(new Status(IStatus.ERROR, bundleName, msg, t));
	}

	/**
	 * Logs a warning to the Error view.
	 */
	public static void logWarn(String msg) {
		log(msg, IStatus.WARNING);
	}

	/**
	 * Logs an information message to the Error view.
	 */
	public static void logInfo(String msg) {
		log(msg, IStatus.INFO);
	}

	/**
	 * Logs a message to the Error view.
	 */
	public static void log(String msg, final int level) {
		final String bundleName = FrameworkUtil.getBundle(Activator.class).getSymbolicName();
		getDefault().getLog().log(new Status(level, bundleName, msg));
	}
}
