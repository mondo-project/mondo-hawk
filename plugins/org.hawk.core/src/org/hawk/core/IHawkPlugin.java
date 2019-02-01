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
package org.hawk.core;

/**
 * Generic interface for any Hawk plugin which can be enabled, disabled or
 * selected at will by the user for an instance.
 */
public interface IHawkPlugin {

	enum Category {
		BACKEND,
		METAMODEL_INTROSPECTOR,
		METAMODEL_RESOURCE_FACTORY,
		METAMODEL_UPDATER,
		MODEL_RESOURCE_FACTORY,
		MODEL_UPDATER,
		INDEX_FACTORY,
		GRAPH_CHANGE_LISTENER,
		VCS_MANAGER,
		QUERY_ENGINE;
	}

	/**
	 * Returns a unique identifier for the plugin. Useful for configuration files.
	 */
	default String getType() {
		return getClass().getCanonicalName();
	}

	/**
	 * Returns a human-friendly description of the plugin. May be localized.
	 */
	String getHumanReadableName();

	/**
	 * Returns the category of plugin that this implementation belongs to. This
	 * method is useful for remote instances, which will use dummy implementations
	 * that do not implement the base interface of that category of plugin.
	 */
	Category getCategory();
}
