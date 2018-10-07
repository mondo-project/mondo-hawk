/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Generic interface for a service that lists the available metamodels, types
 * and attributes for specific combinations of indexer + updater. The updater
 * can be figured out by looking at the enabled plugins within the indexer.
 *
 * Updaters should generally contribute their own implementations.
 */
public interface IMetaModelIntrospector {

	public interface Factory {
		/**
		 * Returns <code>true</code> if this implementation can introspect the provided
		 * {@link IModelIndexer}.
		 * 
		 * @param idx Indexer to report on.
		 */
		boolean canIntrospect(IModelIndexer idx);

		/**
		 * Returns a new introspector locked to a specific indexer. Assumes that the
		 * caller has checked {@link #canIntrospect(IModelIndexer)} first.
		 */
		IMetaModelIntrospector createFor(IModelIndexer idx);
	}

	/**
	 * Returns a list of the metamodel URIs registered at this indexer, in ascending
	 * lexicographical order.
	 */
	List<String> getMetamodels();

	/**
	 * Returns a list of the names of the types registered at the specified indexer
	 * and metamodel, in ascending lexicographical order.
	 * 
	 * @throws NoSuchElementException No such metamodel is registered in the
	 *                                indexer.
	 */
	List<String> getTypes(String metamodelURI) throws NoSuchElementException;

	/**
	 * Returns a list of the attribute names of the specified type in the specified
	 * metamodel, within the specified indexer. The list is sorted in ascending
	 * lexicographical order.
	 * 
	 * @throws NoSuchElementException No such metamodel is registered in the
	 *                                indexer, or the metamodel does not have the
	 *                                specified type.
	 */
	List<String> getAttributes(String metamodelURI, String typeName) throws NoSuchElementException;

}
