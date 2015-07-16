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
package org.hawk.core;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
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
	boolean synchronise() throws Exception;

	// /**
	// * shuts down, not persisting any metadata
	// *
	// * @throws Exception
	// */
	// void shutdown() throws Exception;

	/**
	 * shuts down, persisting metadata (used in default ui plugin to load) also
	 * has the option to delete the back-end (boolean delete = true) for quicker
	 * testing
	 * 
	 * @throws Exception
	 */
	void shutdown(boolean delete) throws Exception;

	/**
	 * 
	 * @return running back-end
	 */
	IGraphDatabase getGraph();

	/**
	 * 
	 * @return running vcs managers
	 */
	Set<IVcsManager> getRunningVCSManagers();

	/**
	 * 
	 * @return current known metamodels in hawk
	 */
	Set<String> getKnownMMUris();

	String getId();

	void registerMetamodel(File[] f) throws Exception;

	void registerMetamodel(File f) throws Exception;

	void removeMetamodel(File[] metamodel) throws Exception;

	void removeMetamodel(File metamodel) throws Exception;

	IAbstractConsole getConsole();

	// deprecated -- internal method now
	// void saveIndexer(File f);

	void addVCSManager(IVcsManager vcs);

	void addModelUpdater(IModelUpdater updater);

	void addMetaModelResourceFactory(
			IMetaModelResourceFactory metaModelParser);

	void addModelResourceFactory(
			IModelResourceFactory modelParser);

	void setDB(IGraphDatabase db);

	void addQueryEngine(IQueryEngine q);

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
	void init() throws Exception;

	// /**
	// * NOTE: do not call this method before setting all the required factories
	// * and parsers you wish to use in this hawk!
	// *
	// * @throws Exception
	// */
	// void init() throws Exception;

	IModelResourceFactory getModelParser(String type);

	IMetaModelResourceFactory getMetaModelParser(
			String metaModelType);

	Map<String, IQueryEngine> getKnownQueryLanguages();

	/**
	 * 
	 * @return the folder the entire hawk structure is stored in
	 */
	File getParentFolder();

	// deprecated -- use IQueryEngine.contextlessQuery(IGraphDatabase g, String
	// query) with a specific query engine (such as epsilon's eol) to run a
	// query on hawk -- to find them use getKnownQueryLanguages() -- keyset
	// gives their identifiers and get() the runtime classes

	// void runEOL();

	/**
	 * creates a comprehensive log of the entire contents of hawk -- use
	 * sparingly as may take a very long time for large hawk instances
	 * 
	 * @throws Exception
	 */
	void logFullStore() throws Exception;

	/**
	 * resets the timer for running synchronise() on hawk
	 */
	void resetScheduler();

	void setMetaModelUpdater(IMetaModelUpdater metaModelUpdater);

	void addDerivedAttribute(String metamodeluri,
			String typename, String attributename, String attributetype,
			boolean isMany, boolean isOrdered, boolean isUnique,
			String derivationlanguage, String derivationlogic);

	void addIndexedAttribute(String metamodeluri,
			String typename, String attributename);

	Object query(File query, String queryLangID)
			throws Exception;

	Object query(String query, String queryLangID)
			throws Exception;

	Collection<String> getDerivedAttributes();

	/**
	 * Returns a collection of strings of the form <code>mmuri##typename##attrname</code>,
	 * where <code>mmuri</code> is the URI of the EPackage, <code>typename</code> is the
	 * unqualified name of the type, and <code>attrname</code> is the name of the attribute
	 * being indexed.
	 */
	Collection<String> getIndexedAttributes();

	Collection<String> getIndexes();

	List<String> validateExpression(String derivationlanguage, String derivationlogic);

	public String getName();

	/**
	 * Only sets the admin password if it is currently unset.
	 * @param pw
	 */
	void setAdminPassword(char[] pw);

	String decrypt(String pw) throws GeneralSecurityException, IOException;

}