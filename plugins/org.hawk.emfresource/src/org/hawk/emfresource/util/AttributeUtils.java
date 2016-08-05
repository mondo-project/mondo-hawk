/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.emfresource.util;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEStoreEObjectImpl;

/**
 * Utility methods for setting {@link EAttribute}s from {@link AttributeSlot}s.
 */
public final class AttributeUtils {

	private AttributeUtils() {}

	public static Object setAttribute(final EFactory eFactory, final EClass eClass, final EObject eObject, final String featureName, final Object value) {
		final EStructuralFeature feature = eClass.getEStructuralFeature(featureName);
		if (feature == null) {
			return null;
		}
		if (!feature.isChangeable() || feature.isDerived() && !(eObject instanceof DynamicEStoreEObjectImpl)) {
			return null;
		}

		if (feature.isMany()) {
			return setListAttribute(eFactory, eObject, value, feature);
		} else {
			return setScalarAttribute(eFactory, eObject, value, feature);
		}
	}

	private static Object setScalarAttribute(final EFactory eFactory, final EObject eObject, final Object value,
			final EStructuralFeature feature) {
		final Object singleValue = normalizeIntoScalar(value);
		if (singleValue == null) {
			eObject.eUnset(feature);
			return null;
		}

		final EClassifier eType = feature.getEType();
		if (eType instanceof EEnum) {
			final EEnum enumType = (EEnum)eType;
			final Object literal = eFactory.createFromString(enumType, singleValue.toString());
			eObject.eSet(feature, literal);
			return literal;
		} else {
			eObject.eSet(feature, singleValue);
			return singleValue;
		}
	}

	private static Object setListAttribute(final EFactory eFactory, final EObject eObject, final Object value,
			final EStructuralFeature feature) {
		final EList<Object> manyValue = normalizeIntoList(value);
		if (manyValue == null) {
			eObject.eUnset(feature);
			return null;
		}

		final EClassifier eType = feature.getEType();
		if (eType instanceof EEnum) {
			final EEnum enumType = (EEnum)eType;
			final EList<Object> literals = new BasicEList<>();
			for (final Object o : manyValue) {
				literals.add(eFactory.createFromString(enumType, o.toString()));
			}
			eObject.eSet(feature, literals);
			return literals;
		} else {
			eObject.eSet(feature, manyValue);
			return manyValue;
		}
	}

	private static Object normalizeIntoScalar(final Object value) {
		if (value instanceof Object[]) {
			final Object[] arr = (Object[])value;
			if (arr.length > 0) {
				return arr[0];
			}
		} else if (value instanceof Collection) {
			final Collection<?> coll = (Collection<?>)value;
			if (!coll.isEmpty()) {
				return coll.iterator().next();
			}
		}
		return value;
	}

	private static EList<Object> normalizeIntoList(final Object value) {
		if (value instanceof Object[]) {
			return new BasicEList<>(Arrays.asList((Object[])value));
		} else if (value instanceof Collection) {
			return new BasicEList<>((Collection<?>)value);
		} else if (value != null) {
			EList<Object> manyValue = new BasicEList<>();
			manyValue.add(value);
			return manyValue;
		}
		return null;
	}

}
