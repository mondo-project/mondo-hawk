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
package org.hawk.core.model;

public interface IHawkObject {

	boolean isProxy();

	// only to be called after isProxy() returns true
	String proxyURI();

	// full uri of element
	String getUri();

	// uri of the element itself without its containers
	String getUriFragment();

	// the type of the object
	IHawkClassifier getType();

	// returns whether the structural feature hsf is set in this object
	boolean isSet(IHawkStructuralFeature hsf);

	// gets the value of attribute attr of this object
	Object get(IHawkAttribute attr);

	// gets the value of reference ref of this object, with boolean b for
	// resolving proxies (if needed)
	Object get(IHawkReference ref, boolean b);

	// override of default hashcode used to identify an object throughout
	// different factory loading operations - an implementation is encouraged to
	// use the following identifying features to calculate this hashcode:
	// getUri(), type.getName(), type.getPackageNSURI(), forAll attributes
	// eAttribute.getName(), eAttribute-value, forAll references eRef.getName(),
	// ref-value.getUriFragment()
	@Override
	public int hashCode();

}
