/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 ******************************************************************************/
package org.hawk.core;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.CompositeGraphChangeListener;
import org.hawk.core.runtime.CompositeStateListener;
import org.hawk.core.util.IndexedAttributeParameters;

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

	void registerMetamodels(File... f) throws Exception;

	void removeMetamodels(String... metamodelURI) throws Exception;

	IConsole getConsole();

	void addVCSManager(IVcsManager vcs, boolean persist);

	void addModelUpdater(IModelUpdater updater);

	void addMetaModelResourceFactory(IMetaModelResourceFactory metaModelParser);

	void addModelResourceFactory(IModelResourceFactory modelParser);

	boolean addGraphChangeListener(IGraphChangeListener changeListener);

	boolean removeGraphChangeListener(IGraphChangeListener changeListener);

	CompositeGraphChangeListener getCompositeGraphChangeListener();

	boolean addStateListener(IStateListener stateListener);

	boolean removeStateListener(IStateListener stateListener);

	CompositeStateListener getCompositeStateListener();

	/**
	 * Convenience method for {@link #waitFor(HawkState)} that waits
	 * indefinitely.
	 */
	void waitFor(HawkState targetState) throws InterruptedException;

	/**
	 * On a running Hawk, blocks the current thread until the state of Hawk
	 * changes to the target state or a certain amount of time passes.
	 *
	 * @param targetState
	 *            State that we should wait for.
	 * @param timeoutMillis
	 *            Milliseconds to wait (or 0 to wait indefinitely).
	 * @throws InterruptedException
	 *             The wait was interrupted.
	 * @throws IllegalStateException
	 *             if the Hawk instance is not running altogether.
	 */
	void waitFor(HawkState targetState, long timeoutMillis) throws InterruptedException;

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

	IModelResourceFactory getModelParser(String type);

	Set<String> getKnownMetaModelParserTypes();

	IMetaModelResourceFactory getMetaModelParser(String metaModelType);

	/**
	 * Returns a set with all the supported metamodel extensions, in the
	 * format of ".ext". For instance, for Ecore we would return ".ecore".
	 */
	Set<String> getKnownMetamodelFileExtensions();

	Map<String, IQueryEngine> getKnownQueryLanguages();

	/**
	 * 
	 * @return the folder the entire hawk structure is stored in
	 */
	File getParentFolder();

	void setMetaModelUpdater(IMetaModelUpdater metaModelUpdater);

	void addDerivedAttribute(String metamodeluri, String typename, String attributename, String attributetype,
			boolean isMany, boolean isOrdered, boolean isUnique, String derivationlanguage, String derivationlogic);

	void addIndexedAttribute(String metamodeluri, String typename, String attributename);

	Collection<IndexedAttributeParameters> getDerivedAttributes();

	Collection<IndexedAttributeParameters> getIndexedAttributes();

	Collection<String> getIndexes();

	List<String> validateExpression(String derivationlanguage, String derivationlogic);

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

	void removeVCSManager(IVcsManager vcs) throws Exception;

	/**
	 * Sets the periodic synchronisation of Hawk's index. This interval will
	 * start at base and will keep doubling until it reaches max every time no
	 * changes are found, resetting to base when a change is found. If
	 * {@code max} is set to 0, periodic polling is disabled.
	 * 
	 * @param base
	 * @param max
	 */
	void setPolling(int base, int max);

	boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename);

	boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName);

	/**
	 * Schedules a task on the Hawk update thread. This avoids unwanted
	 * concurrent accesses on an instance of Hawk. Clients are suggested to make
	 * any changes on a Hawk configuration through tasks scheduled this way. Hawk
	 * is not designed to be thread safe.
	 */
	<T> ScheduledFuture<T> scheduleTask(Callable<T> task, long delayMillis);
}
