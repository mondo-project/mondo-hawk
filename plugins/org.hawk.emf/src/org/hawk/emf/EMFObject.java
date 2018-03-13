/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - cleanup, use covariant return types, add SLF4J
 ******************************************************************************/
package org.hawk.emf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMap.ValueListIterator;
import org.hawk.core.model.IHawkAttribute;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.core.model.IHawkDataType;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkReference;
import org.hawk.core.model.IHawkStructuralFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMFObject implements IHawkObject {
	private static final Logger LOGGER = LoggerFactory.getLogger(EMFObject.class);

	protected EMFWrapperFactory wf;
	protected EObject eob;

	public EMFObject(EObject o, EMFWrapperFactory wf) {
		this.eob = o;
		this.wf = wf;
	}

	public EObject getEObject() {
		return eob;
	}

	@Override
	public boolean isInDifferentResourceThan(IHawkObject o) {
		if (o instanceof EMFObject) {
			final EMFObject otherR = (EMFObject) o;
			return eob.eIsProxy() || otherR.eob.eResource() != eob.eResource();
		}
		return false;
	}

	@Override
	public String getUri() {
		return EcoreUtil.getURI(eob).toString();
	}

	@Override
	public String getUriFragment() {
		URI uri = null;
		try {
			uri = EcoreUtil.getURI(eob);
			String frag = uri.fragment();

			return frag;
		} catch (Exception e) {
			LOGGER.error(
				"For object:{}\nwithin: {}\nof type:{}\nCould not find URI {}, returning null",
				eob, eob.eResource(), eob.eClass(), uri);
			return null;
		}

	}

	@Override
	public boolean isFragmentUnique() {
		return false;
	}

	@Override
	public IHawkClassifier getType() {
		return wf.createClass(eob.eClass());
	}

	@Override
	public boolean isSet(IHawkStructuralFeature hsf) {
		final EStructuralFeature sf = eob.eClass().getEStructuralFeature(hsf.getName());

		// NOTE: we need to say 'yes' for default values in order to handle
		// cases like Segment#length = 0 in the Train Benchmark queries.
		return eob.eIsSet(sf) || sf.getDefaultValue() != null;
	}

	@Override
	public Object get(IHawkAttribute attribute) {

		final Object ret = eob.eGet(eob.eClass().getEStructuralFeature(
				attribute.getName()));

		if (ret instanceof FeatureMap) {
			List<Object> subset = new LinkedList<>();

			for (ValueListIterator<Object> it = ((FeatureMap) ret)
					.valueListIterator(); it.hasNext();) {
				final Object next = it.next();
				if (!(next instanceof EObject))
					subset.add(next);
			}
			return subset;

		}

		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object get(IHawkReference reference, boolean resolve) {

		Object ret;

		Object source = eob.eGet(
				eob.eClass().getEStructuralFeature(reference.getName()),
				resolve);

		if (source instanceof Iterable<?>) {

			ret = new LinkedList<EObject>();

			if (source instanceof FeatureMap) {
				for (ValueListIterator<Object> it = ((FeatureMap) source)
						.valueListIterator(); it.hasNext();) {
					final Object next = it.next();
					if (next instanceof EObject)
						((LinkedList<EMFObject>) ret).add(wf.createObject((EObject) next));
				}
			}
			// ordered ref retainment
			else
				for (EObject e : ((Iterable<EObject>) source)) {
					((LinkedList<EMFObject>) ret).add(wf.createObject(e));
				}
		} else {
			ret = wf.createObject((EObject) source);
		}

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
		return eob.hashCode();
	}

	@Override
	public String toString() {
		String ret = "";

		ret += "> " + eob + " :::with attributes::: ";
		for (EAttribute e : eob.eClass().getEAllAttributes()) {
			ret += e + " : " + eob.eGet(e);
		}
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

	@SuppressWarnings("unchecked")
	@Override
	public byte[] signature() {

		if (signature == null) {

			if (eob.eIsProxy()) {
				LOGGER.error("Signature called on proxy object returning null");
				return null;

			} else {

				MessageDigest md = null;

				try {
					md = MessageDigest.getInstance("SHA-1");
				} catch (NoSuchAlgorithmException e) {
					LOGGER.error("signature() tried to create a SHA-1 digest but a NoSuchAlgorithmException was thrown, returning null", e);
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
					LOGGER.warn("Unknown type {} while creating signature", type);
				}
				signature = md.digest();
			}
		}
		return signature;
	}
}
