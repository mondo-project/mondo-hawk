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

import java.util.HashSet;

public interface IHawkClass extends IHawkClassifier, IHawkObject {

	String getName();

	String getPackageNSURI();

	HashSet<IHawkAttribute> getAllAttributes();

	HashSet<IHawkClass> getSuperTypes();

	HashSet<IHawkReference> getAllReferences();

	boolean isAbstract();

	boolean isInterface();

	IHawkStructuralFeature getStructuralFeature(String name);

	//HashSet<HawkClass> eAllContents();

	//String eContainingFeatureName();

	//boolean isContained();

}
