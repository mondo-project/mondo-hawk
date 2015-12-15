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

public interface IHawkObject {

	/**
	 * Returns <code>true</code> if this object belongs to the same resource
	 * as the other, <code>false</code> otherwise.
	 */
	boolean isInDifferentResourceThan(IHawkObject other);

	/**
	 * Returns <code>true</code> if this object is not contained in any other.
	 */
	boolean isRoot();

	/** Returns the full URI of the element. */
	String getUri();

	/**
	 * Returns whether the uri of this element is relative or absolute.
	 *
	 * TODO cross-file references of absolute URIs are not supported
	 */
	boolean URIIsRelative();

	/** URI of the element itself without its containers */
	String getUriFragment();

	/**
	 * Returns <code>true</code> if the URI fragment is unique across files
	 * (as in Modelio). It should be <code>false</code> for most EMF-based
	 * parsers.
	 */
	boolean isFragmentUnique();

	// the type of the object
	IHawkClassifier getType();

	// returns whether the structural feature hsf is set in this object
	boolean isSet(IHawkStructuralFeature hsf);

	// gets the value of attribute attr of this object
	Object get(IHawkAttribute attr);

	// gets the value of reference ref of this object, with boolean b for
	// resolving proxies (if needed)
	Object get(IHawkReference ref, boolean b);

	// sha1 identifier of state:
	// use the following identifying features to calculate this signature:
	// getUri(), type.getName(), type.getPackageNSURI(), forAll attributes
	// eAttribute.getName(), eAttribute-value, forAll references eRef.getName(),
	// ref-value.getUriFragment()
	public byte[] signature();
}
