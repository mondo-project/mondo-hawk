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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;

public interface IModelIndexer {

	enum ShutdownRequestType {
		/**
		 * Request came straight from the user and must be honored no matter
		 * what.
		 */
		ALWAYS,
		/**
		 * Request came from an automated process and may be ignored in some
		 * cases (e.g. remote instances).
		 */
		ONLY_LOCAL;
	}

	/**
	 * Name of the property that is used by Hawk for its internal identifiers
	 * (e.g. type names for EClasses, URI fragments for EObjects and so on).
	 * Should be reasonably unique and avoid potential collisions that names
	 * that might be used in a metamodel.
	 */
	String IDENTIFIER_PROPERTY = "_hawkid";
	String SIGNATURE_PROPERTY = "_hawksignature";
	String METAMODEL_TYPE_PROPERTY = "type";
	String METAMODEL_RESOURCE_PROPERTY = "resource";
	String METAMODEL_DEPENDENCY_EDGE = "dependency";

	boolean VERBOSE = false;

	/**
	 * Forces a synchronisation to be performed immediately.
	 * 
	 * The synchronisation process may run in a separate thread. Users wishing
	 * to run code when synchronisation really starts or ends should register an
	 * {@link IGraphChangeListener}.
	 */
	void requestImmediateSync() throws Exception;

	// /**
	// * shuts down, not persisting any metadata
	// *
	// * @throws Exception
	// */
	// void shutdown() throws Exception;

	/**
	 * Shuts down, persisting metadata.
	 */
	void shutdown(ShutdownRequestType requestType) throws Exception;

	/**
	 * Shuts down and deletes the backend.
	 */
	void delete() throws Exception;

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

	// void removeMetamodel(File[] metamodel) throws Exception;

	void removeMetamodels(String[] metamodelURI) throws Exception;

	IConsole getConsole();

	// deprecated -- internal method now
	// void saveIndexer(File f);

	void addVCSManager(IVcsManager vcs, boolean persist);

	void addModelUpdater(IModelUpdater updater);

	void addMetaModelResourceFactory(IMetaModelResourceFactory metaModelParser);

	void addModelResourceFactory(IModelResourceFactory modelParser);

	boolean addGraphChangeListener(IGraphChangeListener changeListener);

	boolean removeGraphChangeListener(IGraphChangeListener changeListener);

	IGraphChangeListener getCompositeGraphChangeListener();

	boolean addStateListener(IStateListener messageListener);

	boolean removeStateListener(IStateListener messageListener);

	IStateListener getCompositeStateListener();

	void setDB(IGraphDatabase db, boolean persist);

	void addQueryEngine(IQueryEngine q);

	/**
	 * starts hawk -- given an admit password for de-serialising any stored
	 * passwords for saved version control systems NOTE: run init() with no
	 * parameters to disregard serialisation of hawk metadata. NOTE: do not call
	 * this method before setting all the required factories and parsers you
	 * wish to use in this hawk!
	 *
	 * If <code>maxDelay</code> is 0, periodic synchronisation will be disabled,
	 * and only manual synchronisation through {@link #requestImmediateSync()}
	 * will be available.
	 * 
	 * @param minDelay
	 * @param maxDelay
	 */
	void init(int minDelay, int maxDelay) throws Exception;

	/**
	 * Returns <code>true</code> if this indexer is running, <code>false</code>
	 * otherwise.
	 */
	boolean isRunning();

	// /**
	// * NOTE: do not call this method before setting all the required factories
	// * and parsers you wish to use in this hawk!
	// *
	// * @throws Exception
	// */
	// void init() throws Exception;

	IModelResourceFactory getModelParser(String type);

	IMetaModelResourceFactory getMetaModelParser(String metaModelType);

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

	void setMetaModelUpdater(IMetaModelUpdater metaModelUpdater);

	void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic);

	void addIndexedAttribute(String metamodeluri, String typename,
			String attributename);

	Collection<String> getDerivedAttributes();

	/**
	 * Returns a collection of strings of the form
	 * <code>mmuri##typename##attrname</code>, where <code>mmuri</code> is the
	 * URI of the EPackage, <code>typename</code> is the unqualified name of the
	 * type, and <code>attrname</code> is the name of the attribute being
	 * indexed.
	 */
	Collection<String> getIndexedAttributes();

	Collection<String> getIndexes();

	List<String> validateExpression(String derivationlanguage,
			String derivationlogic);

	public String getName();

	/**
	 * if set to true hawk will not unload resources between synchronise calls,
	 * or within the calls, so that they can be used by listeners. NB: this will
	 * greatly affect performance as the memory hawk will need will be increased
	 * 
	 * @param enable
	 */
	void setSyncMetricsEnabled(Boolean enable);

	ICredentialsStore getCredentialsStore();

	String getDerivedAttributeExecutionEngine();

}
