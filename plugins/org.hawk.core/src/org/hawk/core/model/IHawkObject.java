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

import java.util.Collections;
import java.util.Set;

public interface IHawkObject {

	/**
	 * Returns <code>true</code> if this object belongs to the same resource as
	 * the other, <code>false</code> otherwise.
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
	 * Returns <code>true</code> if the URI fragment is unique across files (as
	 * in Modelio). It should be <code>false</code> for most EMF-based parsers.
	 * Note that if multiple files contain a unique element Hawk will honour
	 * insertion/updates assuming the various files contain an identical state
	 * for this element
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
	Object get(IHawkReference ref, boolean resolveProxies);

	// sha1 identifier of state:
	// use the following identifying features to calculate this signature:
	// getUri(), type.getName(), type.getPackageNSURI(), forAll attributes
	// eAttribute.getName(), eAttribute-value, forAll references eRef.getName(),
	// ref-value.getUriFragment()
	public byte[] signature();

	default Set<IHawkAnnotation> getAnnotations() {
		return Collections.emptySet();
	}
}
