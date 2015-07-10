/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation, updates and maintenance
 *     Seyyed Shah - adaption to *.ui2 plugin
 ******************************************************************************/
package org.hawk.ui2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.hawk.core.IHawk;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.util.HawkConfig;
import org.hawk.core.util.HawksConfig;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HManager implements IStructuredContentProvider, IWorkbenchListener {

	ArrayList<IConfigurationElement> backends = getBackends();
	ArrayList<IConfigurationElement> vcs = getVCS();
	ArrayList<IConfigurationElement> modpar = getMps();
	ArrayList<IConfigurationElement> metamodpar = getMmps();
	ArrayList<IConfigurationElement> languages = getLanguages();
	ArrayList<IConfigurationElement> ups = getUps();

	public static IMetaModelUpdater getMetaModelUpdater() throws CoreException {

		IConfigurationElement[] e = Platform.getExtensionRegistry()
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

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		if (firstRun)
			loadHawksFromMetadata();
		return all.toArray();
	}

	private static HManager inst;

	public static HManager getInstance() {
		if (inst == null)
			inst = new HManager();
		return inst;
	}

	private static Set<HModel> all = new HashSet<HModel>();

	public static Set<HModel> getHawks() {
		if (firstRun)
			loadHawksFromMetadata();
		return all;
	}

	public static void addHawk(HModel e) {
		all.add(e);
	}

	public static void delete(HModel o, boolean exists) {
		// System.err.println("delete called on " + o.getName() + " " +
		// o.getFolder());
		if (all.contains(o)) {
			if (exists) {
				o.stop();
				o.delete();
			} else
				o.removeHawkFromMetadata(new HawkConfig(o.getName(), o
						.getFolder()));
			all.remove(o);
		} else
			o.removeHawkFromMetadata(new HawkConfig(o.getName(), o.getFolder()));
	}

	private static boolean firstRun = true;

	private static void loadHawksFromMetadata() {

		// ...

		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode("org.hawk.ui2");

		try {

			Collection<HawkConfig> hawks = new HashSet<HawkConfig>();

			String xml = preferences.get("config", null);

			if (xml != null) {
				XStream stream = new XStream(new DomDriver());
				stream.processAnnotations(HawksConfig.class);
				stream.processAnnotations(HawkConfig.class);
				stream.setClassLoader(HawksConfig.class.getClassLoader());
				HawksConfig hc = (HawksConfig) stream.fromXML(xml);
				for (HawkConfig s : hc.getConfigs())
					hawks.add(s);
			}

			for (HawkConfig s : hawks) {
				addHawk(HModel.createFromFolder(s));
			}

		} catch (Exception e) {
			e.printStackTrace();
			preferences.remove("config");
		}
		firstRun = false;
	}

	private HManager() {

		// System.err.println("osgi config called:");

		try {

			// System.err.println("adding metamodel updater:");
			// h.addMetaModelUpdater(getMetaModelUpdater());

			// System.err.println(metamodpar.size() +
			// " metamodel parsers found");
			for (IConfigurationElement i : metamodpar) {

				// String type = i.getAttribute("MetaModelParser");

				IMetaModelResourceFactory parser = (IMetaModelResourceFactory) i
						.createExecutableExtension("MetaModelParser");

				// h.addMetaModelParser(parser);

			}
			// System.err.println(modpar.size() + " model parsers found");
			for (IConfigurationElement i : modpar) {

				// String type = i.getAttribute("ModelParser");

				IModelResourceFactory parser = (IModelResourceFactory) i
						.createExecutableExtension("ModelParser");

				// h.addModelParser(parser);

			}
			// System.err.println(ups.size() + " model updaters parsers found");
			for (IConfigurationElement i : ups) {

				IModelUpdater up = (IModelUpdater) i
						.createExecutableExtension("ModelUpdater");

				// h.addUpdater(up);

			}
			// System.err.println(languages.size() + " query languages found");
			for (IConfigurationElement i : languages) {

				// h.addQueryLanguage((IQueryEngine) i
				// .createExecutableExtension("query_language"));
			}

			// System.err.println(backends.size() + " back-ends found");
			HashSet<String> knownbackends = new HashSet<>();
			for (IConfigurationElement i : backends) {
				knownbackends.add(i.getAttribute("store"));
			}
			// h.setKnownBackends(knownbackends);

			// System.err.println(vcs.size() + " vcs managers found");
			HashSet<String> knownvcsmanagers = new HashSet<>();
			for (IConfigurationElement i : vcs) {
				knownvcsmanagers.add(i.getAttribute("VCSManager"));
			}
			// h.setKnownVCSManagerTypes(knownvcsmanagers);

		} catch (Exception e) {
			System.err.println("error in initialising osgi config:");
			e.printStackTrace();
		}
	}

	public static IGraphDatabase createGraph(IHawk hawk) throws Exception {

		for (IConfigurationElement i : getBackends()) {
			if (i.getAttribute("store").equals(hawk.getDbtype())) {

				return (IGraphDatabase) i.createExecutableExtension("store");

			}
		}
		throw new Exception("cannot instatate this type of graph: "
				+ hawk.getDbtype());

	}

	public static IVcsManager createVCSManager(String s) throws Exception {

		for (IConfigurationElement i : getVCS()) {
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

	public static ArrayList<IConfigurationElement> getBackends() {
		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {
			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.BackEndExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);

		}
		return els;
	}

	public static ArrayList<IConfigurationElement> getLanguages() {

		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.QueryExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	public static ArrayList<IConfigurationElement> getUps() {

		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.ModelUpdaterExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	public static ArrayList<IConfigurationElement> getVCS() {

		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.VCSExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	public static ArrayList<IConfigurationElement> getMps() {

		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.ModelExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	public static ArrayList<IConfigurationElement> getMmps() {

		ArrayList<IConfigurationElement> els = new ArrayList<IConfigurationElement>();
		if (Platform.isRunning()) {

			IConfigurationElement[] e = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(
							"org.hawk.core.MetaModelExtensionPoint");

			for (IConfigurationElement el : e)
				els.add(el);
		}
		return els;

	}

	@Override
	public boolean preShutdown(IWorkbench workbench, boolean forced) {
		System.out.println("shutting down hawk:");
		for (HModel hm : all) {
			if (hm.isRunning()) {
				System.out.println("stopping: " + hm.getName() + " : "
						+ hm.getFolder());
				hm.stop();
			}
		}
		return true;
	}

	@Override
	public void postShutdown(IWorkbench workbench) {
		System.out.println("(POST SHUTDOWN) hawk terminated");
		for (HModel hm : all)
			if (hm.isRunning())
				hm.stop();
	}

	public static IGraphDatabase getGraphByIndexerName(String indexerName) {
		for (HModel hm : all)
			if (hm.getName().equals(indexerName))
				return hm.getGraph();
		return null;
	}

	public static Collection<String> getIndexerNames() {
		ArrayList<String> name = new ArrayList<String>();
		for (HModel hm : all)
			if (hm.isRunning())
				name.add(hm.getName());
		return name;
	}

}
