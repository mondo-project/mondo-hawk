/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.graph;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.runtime.CompositeGraphChangeListener;
import org.hawk.graph.internal.updater.GraphMetaModelUpdater;
import org.hawk.graph.internal.updater.GraphModelUpdater;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

	private static BundleContext context;
	private static Plugin instance;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		//System.out.println("kjsdbgksdjgbsdkgjbsdgkjsdbgkjsdbgksdbg");
		super.start(bundleContext);
		Activator.context = bundleContext;
		Activator.instance = this;

		if (Platform.isRunning()) {
			final CompositeGraphChangeListener listeners = new CompositeGraphChangeListener();
			IConfigurationElement[] elems = Platform.getExtensionRegistry()
					.getConfigurationElementsFor("org.hawk.graph.graphChangeListener");
			for (IConfigurationElement elem : elems) {
				listeners.add((IGraphChangeListener)elem.createExecutableExtension("class"));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		Activator.context = null;
		Activator.instance = null;
	}

	public static Plugin getInstance() {
		//System.out.println("getIntance() : " + (instance == null));
		return instance;
	}

}
