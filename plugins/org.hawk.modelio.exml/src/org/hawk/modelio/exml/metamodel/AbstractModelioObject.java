/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModelioObject implements IHawkObject {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractModelioObject.class);

	public abstract String getExml();

	@Override
	public boolean isInDifferentResourceThan(IHawkObject other) {
		if (other instanceof AbstractModelioObject) {
			final String otherExml = ((AbstractModelioObject) other).getExml();
			return !getExml().equals(otherExml);
		}
		return false;
	}

	@Override
	public boolean URIIsRelative() {
		// TODO change this?
		return false;
	}

	byte[] signature = null;

	@SuppressWarnings("unchecked")
	@Override
	public byte[] signature() {
		if (signature == null) {

			MessageDigest md = null;

			try {
				md = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				System.err.println(
						"signature() tried to create a SHA-1 digest but a NoSuchAlgorithmException was thrown, returning null");
				return null;
			}

			md.update(getUri().getBytes());
			md.update(getUriFragment().getBytes());

			IHawkClassifier type = getType();

			md.update(type.getName().getBytes());
			md.update(type.getPackageNSURI().getBytes());

			if (type instanceof IHawkDataType) {

				//

			} else if (type instanceof IHawkClass) {

				for (IHawkAttribute eAttribute : ((IHawkClass) type).getAllAttributes()) {
					if (eAttribute.isDerived() || isSet(eAttribute)) {

						md.update(eAttribute.getName().getBytes());

						if (!eAttribute.isDerived())
							// XXX NOTE: using toString for hashcode of
							// attribute values as primitives in java have
							// different hashcodes each time, not foolproof
							// true == "true" here
							md.update(get(eAttribute).toString().getBytes());
						else {

							// handle derived attributes for metamodel
							// evolution

						}
					}
				}

				for (IHawkReference eRef : ((IHawkClass) type).getAllReferences()) {
					if (isSet(eRef)) {
						md.update(eRef.getName().getBytes());

						Object destinationObjects = get(eRef, false);
						if (destinationObjects instanceof Iterable<?>) {
							for (IHawkObject o : ((Iterable<IHawkObject>) destinationObjects)) {
								md.update(o.getUriFragment().getBytes());
							}
						} else if (destinationObjects != null) {
							md.update(((IHawkObject) destinationObjects).getUriFragment().getBytes());
						} else {
							LOGGER.warn("Destination object for feature {} of {} is null", eRef.getName(), this);
						}
					}
				}
			} else {
				System.err.println("warning emf object tried to create signature, but found type: " + type);
			}
			signature = md.digest();
		}
		return signature;
	}
}