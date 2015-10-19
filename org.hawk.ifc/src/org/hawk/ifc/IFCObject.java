/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - updates and maintenance
 ******************************************************************************/
package org.hawk.ifc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class IFCObject implements IHawkObject {

	protected EObject eob;

	public IFCObject(EObject o) {
		eob = o;
	}

	public EObject getEObject() {
		return eob;
	}

	@Override
	public String getUri() {
		String uri = EcoreUtil.getURI(eob).toString();
		if (uri == null || uri == "" || uri == "/" || uri == "//")
			System.err.println("URI error on: " + eob);
		return uri;

	}

	@Override
	public String getUriFragment() {
		String frag = EcoreUtil.getURI(eob).fragment();
		if (frag == null || frag == "" || frag == "/")
			System.err.println("fragment error on: "
					+ EcoreUtil.getURI(eob).toString());

		return frag;
	}

	@Override
	public IHawkClassifier getType() {

		return new IFCClass(eob.eClass());
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		return eob.eIsSet(eob.eClass().getEStructuralFeature(hsf.getName()));
	}

	@Override
	public Object get(IHawkAttribute attribute) {
		return eob
				.eGet(eob.eClass().getEStructuralFeature(attribute.getName()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object get(IHawkReference reference, boolean b) {

		Object ret;

		Object source = eob.eGet(
				eob.eClass().getEStructuralFeature(reference.getName()), b);

		if (source instanceof Iterable<?>) {
			ret = new HashSet<EObject>();
			for (EObject e : ((Iterable<EObject>) source)) {
				((HashSet<IFCObject>) ret).add(new IFCObject(e));
			}
		} else
			ret = new IFCObject((EObject) source);

		return ret;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IFCObject)
			return eob.equals(((IFCObject) o).getEObject());
		else
			return false;
	}

	@Override
	public int hashCode() {
		//System.err.println("WARNING HASHCODE CALLED ON IFCOBJECT -- this is inaccuarate, use signature() instead!");
		return eob.hashCode();
	}

	byte[] signature = null;

	@Override
	public byte[] signature() {

		if (signature == null) {

			if (eob.eIsProxy()) {

				System.err
						.println("signature called on proxy object returning null");
				return null;

			} else {

				MessageDigest md = null;

				try {
					md = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					System.err
							.println("signature() tried to create a SHA-1 digest but a NoSuchAlgorithmException was thrown, returning null");
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

					for (IHawkAttribute eAttribute : ((IHawkClass) type)
							.getAllAttributes()) {
						if (eAttribute.isDerived() || isSet(eAttribute)) {

							md.update(eAttribute.getName().getBytes());

							if (!eAttribute.isDerived())
								// XXX NOTE: using toString for hashcode of
								// attribute values as primitives in java have
								// different hashcodes each time, not fullproof
								// true == "true" here
								md.update(get(eAttribute).toString().getBytes());
							else {

								// handle derived attributes for metamodel
								// evolution

							}
						}
					}

					for (IHawkReference eRef : ((IHawkClass) type)
							.getAllReferences()) {
						if (isSet(eRef)) {

							md.update(eRef.getName().getBytes());

							Object destinationObjects = get(eRef, false);
							if (destinationObjects instanceof Iterable<?>) {
								for (IHawkObject o : ((Iterable<IHawkObject>) destinationObjects)) {
									md.update(o.getUriFragment().getBytes());
								}
							} else {
								md.update(((IHawkObject) destinationObjects)
										.getUriFragment().getBytes());
							}
						}
					}
				} else {
					System.err
							.println("warning emf object tried to create signature, but found type: "
									+ type);
				}
				signature = md.digest();
			}
		}
		return signature;
	}

	@Override
	public String toString() {

		String ret = "";

		ret += ">" + eob + "\n";

		for (EAttribute e : eob.eClass().getEAllAttributes())
			ret += e + " : " + eob.eGet(e);
		ret += "\n";

		return ret;

	}

	@Override
	public boolean isRoot() {
		return eob.eContainer() == null;
	}

	@Override
	public boolean URIIsRelative() {

		return EcoreUtil.getURI(eob).isRelative();

	}

	@Override
	public boolean isInDifferentResourceThan(IHawkObject o) {
		if (o instanceof IFCObject) {
			final IFCObject otherR = (IFCObject)o;
			return eob.eIsProxy() || otherR.eob.eResource() != eob.eResource();
		}
		return false;
	}

}
