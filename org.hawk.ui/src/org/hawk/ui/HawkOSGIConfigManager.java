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
package org.hawk.ui;

import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hawk.core.IHawkUI;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;

public class HawkOSGIConfigManager {

	LinkedList<IConfigurationElement> backends = getBackends();
	LinkedList<IConfigurationElement> vcs = getVCS();
	LinkedList<IConfigurationElement> modpar = getMps();
	LinkedList<IConfigurationElement> metamodpar = getMmps();
	LinkedList<IConfigurationElement> languages = getLanguages();
	LinkedList<IConfigurationElement> ups = getUps();

	public IMetaModelUpdater getMetaModelUpdater() throws CoreException {

		IConfigurationElement[] e = Platform
				.getExtensionRegistry()
				.getConfigurationElementsFor(
						"org.hawk.core.MetaModelUpdaterExtensionPoint");

		IConfigurationElement i = null;

		for (IConfigurationElement ii : e) {
			if (i == null)
				i = ii;
			else
				System.err
						.println("more than one metamodel updater found, only one allowed");

		}

		if (i != null)
			return (IMetaModelUpdater) i
					.createExecutableExtension("metamodelupdater");
		else
			return null;

	}

	public HawkOSGIConfigManager(IHawkUI h) {

		System.err.println("osgi config called:");

		try {

			System.err.println("adding metamodel updater:");
			h.addMetaModelUpdater(getMetaModelUpdater());

			System.err.println(metamodpar.size() + " metamodel parsers found");
			for (IConfigurationElement i : metamodpar) {

				// String type = i.getAttribute("MetaModelParser");

				IMetaModelResourceFactory parser = (IMetaModelResourceFactory) i
						.createExecutableExtension("MetaModelParser");

				h.addMetaModelParser(parser);

			}
			System.err.println(modpar.size() + " model parsers found");
			for (IConfigurationElement i : modpar) {

				// String type = i.getAttribute("ModelParser");

				IModelResourceFactory parser = (IModelResourceFactory) i
						.createExecutableExtension("ModelParser");

				h.addModelParser(parser);

			}
			System.err.println(ups.size() + " model updaters parsers found");
			for (IConfigurationElement i : ups) {

				IModelUpdater up = (IModelUpdater) i
						.createExecutableExtension("ModelUpdater");

				h.addUpdater(up);

			}
			System.err.println(languages.size() + " query languages found");
			for (IConfigurationElement i : languages) {

				h.addQueryLanguage((IQueryEngine) i
						.createExecutableExtension("query_language"));
			}

			System.err.println(backends.size() + " back-ends found");
			HashSet<String> knownbackends = new HashSet<>();
			for (IConfigurationElement i : backends) {
				knownbackends.add(i.getAttribute("store"));
			}
			h.setKnownBackends(knownbackends);

			System.err.println(vcs.size() + " vcs managers found");
			HashSet<String> knownvcsmanagers = new HashSet<>();
			for (IConfigurationElement i : vcs) {
				knownvcsmanagers.add(i.getAttribute("VCSManager"));
			}
			h.setKnownVCSManagerTypes(knownvcsmanagers);

		} catch (Exception e) {
			System.err.println("error in initialising osgi config:");
			e.printStackTrace();
		}
	}

	public IGraphDatabase createGraph(String s) throws Exception {

		for (IConfigurationElement i : backends) {
			if (i.getAttribute("store").equals(s)) {

				return (IGraphDatabase) i.createExecutableExtension("store");

			}
		}
		throw new Exception("cannot instatate this type of graph: " + s);

	}

	public IVcsManager createVCSManager(String s) throws Exception {

		for (IConfigurationElement i : vcs) {
			if (i.getAttribute("VCSManager").equals(s)) {

				return (IVcsManager) i.createExecutableExtension("VCSManager");

			}
		}
		throw new Exception("cannot instatate this type of manager: " + s);

	}

	public static HashSet<String> getUpdaterTypes() {

		HashSet<String> indexes = new HashSet<String>();

		for (IConfigurationElement i : getUps()) {

			indexes.add(i.getAttribute("ModelUpdater"));

		}

		return indexes;

	}

	public static HashSet<String> getIndexTypes() {

		HashSet<String> indexes = new HashSet<String>();

		for (IConfigurationElement i : getBackends()) {

			indexes.add(i.getAttribute("store"));

		}

		return indexes;

	}

	public static HashSet<String> getVCSTypes() {

		HashSet<String> indexes = new HashSet<String>();

		for (IConfigurationElement i : getVCS()) {

			indexes.add(i.getAttribute("VCSManager"));

		}

		return indexes;

	}

	public static HashSet<String> getModelTypes() {

		HashSet<String> indexes = new HashSet<String>();

		for (IConfigurationElement i : getMps()) {

			indexes.add(i.getAttribute("ModelParser"));

		}

		return indexes;

	}

	public static HashSet<String> getMetaModelTypes() {

		HashSet<String> indexes = new HashSet<String>();

		for (IConfigurationElement i : getMmps()) {

			indexes.add(i.getAttribute("MetaModelParser"));

		}

		return indexes;

	}

	private static LinkedList<IConfigurationElement> getBackends() {
		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {
			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.BackEndExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);

		}
		return els;
	}

	private static LinkedList<IConfigurationElement> getLanguages() {

		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.QueryExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	private static LinkedList<IConfigurationElement> getUps() {

		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform
					.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.ModelUpdaterExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	private static LinkedList<IConfigurationElement> getVCS() {

		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.VCSExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	private static LinkedList<IConfigurationElement> getMps() {

		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.ModelExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	private static LinkedList<IConfigurationElement> getMmps() {

		LinkedList<IConfigurationElement> els = new LinkedList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.MetaModelExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

}
