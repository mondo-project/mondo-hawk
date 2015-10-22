/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.hawk.core.Activator;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IHawk;
import org.hawk.core.IHawkFactory;
import org.hawk.core.util.HawkProperties;
import org.osgi.framework.FrameworkUtil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class LocalHawkFactory implements IHawkFactory {

	/**
	 * Unique identifier for this factory, as defined in the <code>plugin.xml</code> file.
	 */
	public final static String ID = "org.hawk.core.hawkFactory.local";

	@Override
	public IHawk create(String name, File localStorageFolder, String location, ICredentialsStore credStore, IConsole console) throws Exception {
		return new LocalHawk(name, localStorageFolder, credStore, console);
	}

	@Override
	public boolean instancesAreExtensible() {
		return true;
	}

	@Override
	public boolean instancesCreateGraph() {
		return true;
	}

	@Override
	public boolean instancesUseLocation() {
		return false;
	}

	@Override
	public InstanceInfo[] listInstances(String location) {
		final File basePath =
			ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();

		final List<InstanceInfo> entries = new ArrayList<>();
		for (File f : basePath.listFiles()) {
			if (f.isDirectory()) {
				File fProps = new File(f, "properties.xml");
				if (fProps.canRead()) {
					try {
						XStream stream = new XStream(new DomDriver());
						stream.processAnnotations(HawkProperties.class);
						stream.setClassLoader(HawkProperties.class.getClassLoader());

						HawkProperties hp = (HawkProperties) stream.fromXML(fProps);
						entries.add(new InstanceInfo(f.getName(), hp.getDbType(), false));
					} catch (Exception ex) {
						final String bundleName = FrameworkUtil.getBundle(LocalHawkFactory.class).getSymbolicName();
						final Status warnStatus = new Status(IStatus.WARNING, bundleName, ex.getMessage(), ex);
						Activator.getDefault().getLog().log(warnStatus);
					}
				}
			}
		}
		return entries.toArray(new InstanceInfo[entries.size()]);
	}

}
