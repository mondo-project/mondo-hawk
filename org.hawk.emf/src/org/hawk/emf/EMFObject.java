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
package org.hawk.emf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import org.eclipse.emf.common.util.URI;
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

public class EMFObject implements IHawkObject {

	protected EObject eob;

	public EMFObject(EObject o) {
		eob = o;
	}

	public EObject getEObject() {
		return eob;
	}

	@Override
	public boolean isInDifferentResourceThan(IHawkObject o) {
		if (o instanceof EMFObject) {
			final EMFObject otherR = (EMFObject)o;
			return eob.eIsProxy() || otherR.eob.eResource() != eob.eResource();
		}
		return false;
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
		URI uri = null;
		try {
			uri = EcoreUtil.getURI(eob);
			String frag = uri.fragment();
			if (frag == null || frag == "" || frag == "/")
				System.err.println("fragment error on: "
						+ EcoreUtil.getURI(eob).toString() + " fragment: '"
						+ frag + "' on eobject: " + eob + " (isproxy:"
						+ eob.eIsProxy() + ")");

			return frag;
		} catch (Exception e) {
			System.err.println(eob);
			System.err.println(eob.eResource());
			System.err.println(eob.eClass());
			System.err.println("Error in finding URI: " + uri
					+ ", returning null");
			return null;
		}

	}

	@Override
	public IHawkClassifier getType() {

		return new EMFClass(eob.eClass());
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
	public Object get(IHawkReference reference, boolean resolve) {

		Object ret;

		Object source = eob.eGet(
				eob.eClass().getEStructuralFeature(reference.getName()),
				resolve);

		if (source instanceof Iterable<?>) {
			// ordered ref retainment
			ret = new LinkedList<EObject>();
			for (EObject e : ((Iterable<EObject>) source)) {
				((LinkedList<EMFObject>) ret).add(new EMFObject(e));
			}
		} else
			ret = new EMFObject((EObject) source);

		return ret;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof EMFObject)
			return eob.equals(((EMFObject) o).getEObject());
		else
			return false;
	}

	private byte[] signature;

	@Override
	public int hashCode() {
		//System.err.println("WARNING HASHCODE CALLED ON EMFOBJECT -- this is inaccuarate, use signature() instead!");
		return eob.hashCode();
	}
	
	// @Override
	// public int hashCode() {
	// //
	// // return eob.hashCode();
	// //
	// if (hashCode == 0) {
	//
	// if (isProxy()) {
	//
	// System.err.println("hashCode called on proxy object - 0");
	// hashCode = 0;
	// return 0;
	//
	// } else {
	//
	// hashCode = Integer.MIN_VALUE;
	//
	// hashCode += getUri().hashCode();
	// hashCode += getUriFragment().hashCode();
	//
	// IHawkClassifier type = getType();
	//
	// hashCode += type.getName().hashCode();
	// hashCode += type.getPackageNSURI().hashCode();
	//
	// if (type instanceof IHawkDataType) {
	//
	// //
	//
	// } else if (type instanceof IHawkClass) {
	//
	// for (IHawkAttribute eAttribute : ((IHawkClass) type)
	// .getAllAttributes()) {
	// if (eAttribute.isDerived() || isSet(eAttribute)) {
	//
	// hashCode += eAttribute.getName().hashCode();
	//
	// if (!eAttribute.isDerived())
	// // NOTE: using toString for hashcode of
	// // attribute values as primitives in java have
	// // different hashcodes each time, not fullproof
	// // true == "true" here
	// hashCode += get(eAttribute).toString()
	// .hashCode();
	// else {
	//
	// // handle derived attributes for metamodel
	// // evolution
	//
	// }
	// }
	// }
	//
	// // cyclic reference loop? -- no we only access the urifragment of
	// references
	// for (IHawkReference eRef : ((IHawkClass) type)
	// .getAllReferences()) {
	// if (isSet(eRef)) {
	//
	// hashCode += eRef.getName().hashCode();
	//
	// Object destinationObjects = get(eRef, false);
	// if (destinationObjects instanceof Iterable<?>) {
	// for (IHawkObject o : ((Iterable<IHawkObject>) destinationObjects)) {
	// hashCode += o.getUriFragment().hashCode();
	// }
	// } else {
	// hashCode += ((IHawkObject) destinationObjects)
	// .getUriFragment().hashCode();
	// }
	// }
	// }
	// } else {
	// System.err
	// .println("warning emf object tried to create hashcode, but found type: "
	// + type);
	// }
	// }
	// }
	// return hashCode;
	//
	// }

	@Override
	public String toString() {

		String ret = "";

		ret += "> " + eob + " :::with attributes::: ";

		for (EAttribute e : eob.eClass().getEAllAttributes())
			ret += e + " : " + eob.eGet(e);
		ret += "";

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
								// NOTE: using toString for hashcode of
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
}
