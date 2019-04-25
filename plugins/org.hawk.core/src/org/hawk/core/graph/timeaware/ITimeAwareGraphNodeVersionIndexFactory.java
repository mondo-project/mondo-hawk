/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.core.graph.timeaware;

/**
 * Base interface for a component that can create instances of
 * {@link ITimeAwareGraphNodeVersionIndex}. This will usually be a
 * {@link ITimeAwareGraphDatabase}, but not all time-aware graph databases may
 * offer this service (hence the use of a separate interface).
 */
public interface ITimeAwareGraphNodeVersionIndexFactory {

	/**
	 * Returns an object which exposes the existing version index with label <code>name</code>,
	 * or creates a new index if it does not exist.
	 */
	ITimeAwareGraphNodeVersionIndex getOrCreateVersionIndex(String name);

	/**
	 * Returns <code>true</code> iff the version index with the label <code>name</code> exists.
	 */
	boolean versionIndexExists(String name);

}
