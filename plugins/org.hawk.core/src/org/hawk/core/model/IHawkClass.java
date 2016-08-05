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
package org.hawk.core.model;

import java.util.Set;

public interface IHawkClass extends IHawkClassifier, IHawkObject {

	String getName();

	String getPackageNSURI();

	/**
	 * Returns all directly defined and inherited attributes of this type.
	 */
	Set<IHawkAttribute> getAllAttributes();

	/**
	 * Returns all direct and indirect supertypes of this type.
	 */
	Set<IHawkClass> getAllSuperTypes();

	/**
	 * Old name for {@link #getAllSuperTypes()}: this will be removed in future versions.
	 * Kept for interim compatibility while updating binary artefacts.
	 */
	@Deprecated
	Set<IHawkClass> getSuperTypes();

	/**
	 * Returns all directly defined and inherited references of this type.
	 */
	Set<IHawkReference> getAllReferences();

	boolean isAbstract();

	boolean isInterface();

	IHawkStructuralFeature getStructuralFeature(String name);
}
