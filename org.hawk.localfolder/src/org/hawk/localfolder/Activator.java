/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.localfolder;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

	private static BundleContext context;
	private static Plugin instance;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		Activator.context = bundleContext;
		instance = this;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		Activator.context = null;
		instance = null;
	}

	public static Plugin getInstance() {

		return instance;

	}

}
