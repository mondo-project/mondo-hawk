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
package org.hawk.ui2.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.ui2.view.HView;

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
			loadFromWorkspace();
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
			loadFromWorkspace();
		return all;
	}

	public static void addHawk(HModel e) {
		all.add(e);
	}

	public static void delete(HModel o) {
		if (all.contains(o)) {
			o.stop();
			o.delete();
			all.remove(o);
		}
	}

	private static boolean firstRun = true;

	private static void loadFromWorkspace() {

		System.out.println("Loading saved hawk indexers from workspace...");

		PlatformUI.getWorkbench().addWorkbenchListener(getInstance());

		File workspace = ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toFile();

		System.out.println(workspace);

		if (workspace.exists() && workspace.isDirectory()
				&& workspace.canRead()) {
			String[] subs = workspace.list();

			try {
				for (String sub : subs) {
					File subFolder = new File(workspace, sub);
					if (subFolder.exists() && subFolder.isDirectory()
							&& subFolder.canRead()) {
						if (subFolder.list().length >= 2) {
							// check if the names match up
							// /x
							// /x/x
							// /.metadata_x
							boolean isHFolder = false;
							boolean isHMetadata = false;
							File metadata = null;
							for (String hawkfile : subFolder.list()) {
								if (hawkfile.equals(".metadata_" + sub)) {
									isHMetadata = true;
									metadata = new File(subFolder
											+ File.separator + hawkfile);
								}

								if (hawkfile.equals(sub))
									isHFolder = true;
							}

							if (isHFolder && isHMetadata) {
								// assume it is an HModel
								BufferedReader r = new BufferedReader(
										new FileReader(metadata));

								String line = r.readLine();

								r.close();

								String[] split = line.split("\t");

								HModel hm = HModel.createFromFolder(sub,
										ResourcesPlugin.getWorkspace()
												.getRoot().getLocation()
												.toString()
												+ File.separator + sub,
										split[2]);
								all.add(hm);
							}

						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public static IGraphDatabase createGraph(String s) throws Exception {

		for (IConfigurationElement i : getBackends()) {
			if (i.getAttribute("store").equals(s)) {

				return (IGraphDatabase) i.createExecutableExtension("store");

			}
		}
		throw new Exception("cannot instatate this type of graph: " + s);

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
		for (HModel hm : all)
			if (hm.isRunning())
				hm.stop();
		return true;
	}

	@Override
	public void postShutdown(IWorkbench workbench) {
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
