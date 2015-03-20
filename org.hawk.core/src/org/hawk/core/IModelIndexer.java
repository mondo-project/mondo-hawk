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
package org.hawk.core;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;

public interface IModelIndexer {

	/**
	 * When called, attempts to synchronise the index with any changes to any
	 * vcs connected to it
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract boolean synchronise() throws Exception;

	// /**
	// * shuts down, not persisting any metadata
	// *
	// * @throws Exception
	// */
	// public abstract void shutdown() throws Exception;

	/**
	 * shuts down, persisting metadata (used in default ui plugin to load) also
	 * has the option to delete the back-end (boolean delete = true) for quicker
	 * testing
	 * 
	 * @throws Exception
	 */
	public abstract void shutdown(File metadata, boolean delete)
			throws Exception;

	/**
	 * 
	 * @return running back-end
	 */
	public abstract IGraphDatabase getGraph();

	/**
	 * 
	 * @return running vcs managers
	 */
	public abstract Set<IVcsManager> getRunningVCSManagers();

	/**
	 * 
	 * @return current known metamodels in hawk
	 */
	public abstract Set<String> getKnownMMUris();

	public abstract String getId();

	public abstract void registerMetamodel(File[] f) throws Exception;

	public abstract void registerMetamodel(File f) throws Exception;

	public abstract void removeMetamodel(File[] metamodel) throws Exception;

	public abstract void removeMetamodel(File metamodel) throws Exception;

	/**
	 * 
	 * @return the name of the Hawk instance (not that of its back-end)
	 */
	public abstract String getName();

	public abstract IAbstractConsole getConsole();

	// deprecated -- internal method now
	// public abstract void saveIndexer(File f);

	public abstract void addVCSManager(IVcsManager vcs);

	public abstract void addModelUpdater(IModelUpdater updater);

	public abstract void addMetaModelResourceFactory(
			IMetaModelResourceFactory metaModelParser);

	public abstract void addModelResourceFactory(
			IModelResourceFactory modelParser);

	public abstract void setDB(IGraphDatabase db);

	public abstract void addQueryEngine(IQueryEngine q);

	/**
	 * starts hawk -- given an admit password for de-serialising any stored
	 * passwords for saved version control systems NOTE: run init() with no
	 * parameters to disregard serialisation of hawk metadata. NOTE: do not call
	 * this method before setting all the required factories and parsers you
	 * wish to use in this hawk!
	 * 
	 * @param adminpw
	 * @throws Exception
	 */
	public abstract void init(char[] adminpw) throws Exception;

	// /**
	// * NOTE: do not call this method before setting all the required factories
	// * and parsers you wish to use in this hawk!
	// *
	// * @throws Exception
	// */
	// public abstract void init() throws Exception;

	public abstract IModelResourceFactory getModelParser(String type);

	public abstract IMetaModelResourceFactory getMetaModelParser(
			String metaModelType);

	public abstract Map<String, IQueryEngine> getKnownQueryLanguages();

	/**
	 * 
	 * @return the folder the entire hawk structure is stored in
	 */
	public abstract File getParentFolder();

	// deprecated -- use IQueryEngine.contextlessQuery(IGraphDatabase g, String
	// query) with a specific query engine (such as epsilon's eol) to run a
	// query on hawk -- to find them use getKnownQueryLanguages() -- keyset
	// gives their identifiers and get() the runtime classes

	// public abstract void runEOL();

	/**
	 * creates a comprehensive log of the entire contents of hawk -- use
	 * sparingly as may take a very long time for large hawk instances
	 * 
	 * @throws Exception
	 */
	public abstract void logFullStore() throws Exception;

	/**
	 * resets the timer for running synchronise() on hawk
	 */
	public abstract void resetScheduler();

	public abstract void setMetaModelUpdater(IMetaModelUpdater metaModelUpdater);

	public abstract void addDerivedAttribute(String metamodeluri,
			String typename, String attributename, String attributetype,
			boolean isMany, boolean isOrdered, boolean isUnique,
			String derivationlanguage, String derivationlogic);

	public abstract void addIndexedAttribute(String metamodeluri,
			String typename, String attributename);

	public abstract Object query(File query, String queryLangID)
			throws Exception;

	public abstract Object query(String query, String queryLangID)
			throws Exception;

	public abstract Collection<String> getDerivedAttributes();

	public abstract Collection<String> getIndexedAttributes();

	public abstract Collection<String> getIndexes();

	public abstract List<String> validateExpression(String derivationlanguage,
			String derivationlogic);

}