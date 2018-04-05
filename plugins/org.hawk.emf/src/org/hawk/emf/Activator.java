/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia Dominguez - register known EMF extensions on activation, add SLF4J
 ******************************************************************************/
package org.hawk.emf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Descriptor;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends Plugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

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
		super.start(bundleContext);
		Activator.context = bundleContext;
		Activator.instance = this;

		enableRegisteredXMIExtensions();
	}

	/**
	 * Enable this plugin for all the registered extensions that are backed by
	 * XMI-based factories.
	 */
	private void enableRegisteredXMIExtensions() {
		final Registry factoryRegistry = Resource.Factory.Registry.INSTANCE;
		final Set<String> registeredExtensions = new HashSet<>();

		/*
		 * Do a defensive copy of the extension factory registry map, in case some of the triggered plugins
		 * try to add their own extensions. This happened to SOFT-MAINT with the OCL pivot plugins when invoking
		 * descriptor.createFactory.
		 */
		final Map<String, Object> extensionFactoryMapCopy = new HashMap<>(factoryRegistry.getExtensionToFactoryMap());
		for (Entry<String, Object> entry : extensionFactoryMapCopy.entrySet()) {
			switch (entry.getKey()) {
			case Resource.Factory.Registry.DEFAULT_EXTENSION:
			case "ecore":
				continue;
			}

			if (entry.getValue() instanceof Resource.Factory.Descriptor) {
				Descriptor descriptor = (Resource.Factory.Descriptor)entry.getValue();
				try {
					if (descriptor.createFactory() instanceof XMIResourceFactoryImpl) {
						registeredExtensions.add(entry.getKey());
					}
				} catch (Throwable ex) {
					LOGGER.error(String.format("Could not enable Hawk EMF driver for .%s files", entry.getKey()), ex);
				}
			}
			if (entry.getValue() instanceof XMIResourceFactoryImpl) {
				registeredExtensions.add(entry.getKey());
			}
		}

		final String sManualExtensions = System.getProperty(EMFModelResourceFactory.PROPERTY_EXTRA_EXTENSIONS);
		if (sManualExtensions != null) {
			for (String sExtension : sManualExtensions.split(",")) {
				sExtension = sExtension.trim();
				if (sExtension.startsWith(".")) {
					sExtension = sExtension.substring(1);
				}
				if (!sExtension.isEmpty()) {
					registeredExtensions.add(sExtension);
				}
			}
		}

		if (!registeredExtensions.isEmpty()) {
			final StringBuffer sbuf = new StringBuffer();
			boolean first = true;
			for (String sExtension : registeredExtensions) {
				if (first) {
					first = false;
				} else {
					sbuf.append(',');
				}
				sbuf.append('.');
				sbuf.append(sExtension);
			}
			final String newValue = sbuf.toString();
			System.setProperty(EMFModelResourceFactory.PROPERTY_EXTRA_EXTENSIONS, newValue);
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
		return instance;
	}

}
