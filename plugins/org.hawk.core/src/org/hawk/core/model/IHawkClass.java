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
package org.hawk.core.model;

import java.util.Set;

public interface IHawkClass extends IHawkClassifier, IHawkObject {

	String getName();

	/**
	 * Returns all directly defined and inherited attributes of this type.
	 */
	Set<? extends IHawkAttribute> getAllAttributes();

	/**
	 * Returns all direct and indirect supertypes of this type.
	 */
	Set<? extends IHawkClass> getAllSuperTypes();

	/**
	 * Old name for {@link #getAllSuperTypes()}: this will be removed in future versions.
	 * Kept for interim compatibility while updating binary artefacts.
	 */
	@Deprecated
	Set<? extends IHawkClass> getSuperTypes();

	/**
	 * Returns all directly defined and inherited references of this type.
	 */
	Set<? extends IHawkReference> getAllReferences();

	boolean isAbstract();

	boolean isInterface();

	IHawkStructuralFeature getStructuralFeature(String name);
}
