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
package uk.ac.york.mondo.integration.hawk.remote.thrift;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class Activator implements BundleActivator {

	private static Activator instance;
	private BundleContext context;

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.context = null;
		instance = null;
	}

	public BundleContext getContext() {
		return context;
	}

	public static Activator getInstance() {
		return instance;
	}

	public static void logError(Throwable t) {
		final Bundle bundle = FrameworkUtil.getBundle(Activator.class);
		final Status status = new Status(IStatus.ERROR, bundle.getSymbolicName(), t.getMessage(), t);
		Platform.getLog(bundle).log(status);
	}
}
