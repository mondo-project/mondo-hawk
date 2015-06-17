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
package org.hawk.bpmn;

import java.util.LinkedList;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;

public class EMFobject implements IHawkObject {

	protected EObject eob;

	public EMFobject(EObject o) {

		eob = o;

	}

	public EObject getEObject() {
		return eob;

	}

	@Override
	public boolean isProxy() {
		return eob.eIsProxy();
	}

	@Override
	public String proxyURIFragment() {
		return ((InternalEObject) eob).eProxyURI().fragment();
	}

	@Override
	public String proxyURI() {
		return ((InternalEObject) eob).eProxyURI().toString();
	}
	
	@Override
	public String getUri() {
		String uri = org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob)
				.toString();
		if (uri == null || uri == "" || uri == "/" || uri == "//")
			System.err.println("URI error on: " + eob);
		return uri;

	}

	@Override
	public String getUriFragment() {
		URI uri = null;
		try {
			uri = org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob);
			String frag = uri.fragment();
			if (frag == null || frag == "" || frag == "/")
				System.err.println("fragment error on: "
						+ org.eclipse.emf.ecore.util.EcoreUtil.getURI(eob)
								.toString() + " fragment: '" + frag
						+ "' on eobject: " + eob + " (isproxy:" + isProxy()
						+ ")");

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

		return new EMFclass(eob.eClass());
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
				((LinkedList<EMFobject>) ret).add(new EMFobject(e));
			}
		} else
			ret = new EMFobject((EObject) source);

		return ret;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof EMFobject)
			return eob.equals(((EMFobject) o).getEObject());
		else
			return false;
	}

	private int hashCode = 0;

	@Override
	public int hashCode() {
		//
		// return eob.hashCode();
		//
		if (hashCode == 0) {

			if (isProxy()) {

				System.err.println("hashCode called on proxy object - 0");
				hashCode = 0;
				return 0;

			} else {

				hashCode = Integer.MIN_VALUE;

				hashCode += getUri().hashCode();
				hashCode += getUriFragment().hashCode();

				IHawkClassifier type = getType();

				hashCode += type.getName().hashCode();
				hashCode += type.getPackageNSURI().hashCode();

				if (type instanceof IHawkDataType) {

					//

				} else if (type instanceof IHawkClass) {

					for (IHawkAttribute eAttribute : ((IHawkClass) type)
							.getAllAttributes()) {
						if (eAttribute.isDerived() || isSet(eAttribute)) {

							hashCode += eAttribute.getName().hashCode();

							if (!eAttribute.isDerived())
								// XXX NOTE: using toString for hashcode of
								// attribute values as primitives in java have
								// different hashcodes each time, not fullproof
								// true == "true" here
								hashCode += get(eAttribute).toString()
										.hashCode();
							else {

								// handle derived attributes for metamodel
								// evolution

							}
						}
					}

					// cyclic reference loop?
					for (IHawkReference eRef : ((IHawkClass) type)
							.getAllReferences()) {
						if (isSet(eRef)) {

							hashCode += eRef.getName().hashCode();

							Object destinationObjects = get(eRef, false);
							if (destinationObjects instanceof Iterable<?>) {
								for (IHawkObject o : ((Iterable<IHawkObject>) destinationObjects)) {
									hashCode += o.getUriFragment().hashCode();
								}
							} else {
								hashCode += ((IHawkObject) destinationObjects)
										.getUriFragment().hashCode();
							}
						}
					}
				} else {
					System.err
							.println("warning emf object tried to create hashcode, but found type: "
									+ type);
				}
			}
		}
		return hashCode;

	}

	@Override
	public String toString() {

		String ret = "";

		ret += "> " + eob + " :::with attributes::: ";

		for (EAttribute e : eob.eClass().getEAllAttributes())
			ret += e + " : " + eob.eGet(e);
		ret += "";

		return ret;

	}

	// @Override
	// public HawkResource getResource() {
	//
	// return eob.eResource();
	// }

	// @Override
	// public HashSet<?> getAllSetAttributes() {
	//
	// return null;
	// }
	//
	// @Override
	// public HashSet<?> getAllSetReferences() {
	//
	// return null;
	// }

}
