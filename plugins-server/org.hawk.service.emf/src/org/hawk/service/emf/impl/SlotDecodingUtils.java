/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.emf.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEStoreEObjectImpl;
import org.hawk.service.api.AttributeSlot;
import org.hawk.service.api.SlotValue;
import org.hawk.service.api.SlotValue._Fields;

/**
 * Utility methods for setting {@link EAttribute}s from {@link AttributeSlot}s.
 */
public final class SlotDecodingUtils {

	private SlotDecodingUtils() {}

	public static Object setFromSlot(final EFactory eFactory, final EClass eClass, final EObject eObject, final AttributeSlot slot) throws IOException {
		final EStructuralFeature feature = eClass.getEStructuralFeature(slot.name);
		if (feature == null) {
			return null;
		}
		if (!feature.isChangeable() || feature.isDerived() && !(eObject instanceof DynamicEStoreEObjectImpl)) {
			return null;
		}
		if (!slot.isSetValue()) {
			return null;
		}

		// isSet=true and many=false means that we should have exactly one value
		final EClassifier eType = feature.getEType();
		if (eType.eContainer() == EcorePackage.eINSTANCE) {
			return fromEcoreType(eClass, eObject, slot, feature, eType);
		} else if (eType instanceof EEnum) {
			return fromEnum(eFactory, eClass, eObject, slot, feature, (EEnum)eType);
		} else {
			return fromInstanceClass(eClass, eObject, slot, feature, eType);
		}
	}

	private static Object fromByte(final EClass eClass,
			final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature) throws IOException {
		// TODO not sure, need to test

		if (!slot.value.isSetVBytes() && !slot.value.isSetVByte()) {
			throw new IOException(
					String.format(
							"Expected to receive bytes for feature '%s' in type '%s', but did not",
							feature.getName(), eClass.getName()));
		} else if (feature.isMany() || feature.getEType() == EcorePackage.Literals.EBYTE_ARRAY) {
			final EList<Byte> bytes = new BasicEList<Byte>();
			if (slot.value.isSetVBytes()) {
				for (final byte b : slot.value.getVBytes()) {
					bytes.add(b);
				}
			} else {
				bytes.add(slot.value.getVByte());
			}
			eObject.eSet(feature, bytes);
			return bytes;
		} else {
			final byte b = slot.value.getVByte();
			eObject.eSet(feature, b);
			return b;
		}
	}
	private static Object fromEcoreType(final EClass eClass,
			final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature, final EClassifier eType)
			throws IOException {
		if (eType == EcorePackage.Literals.EBYTE_ARRAY || eType == EcorePackage.Literals.EBYTE) {
			return fromByte(eClass, eObject, slot, feature);
		} else if (eType == EcorePackage.Literals.EFLOAT) {
			return fromFloat(eClass, eObject, slot, feature);
		} else if (eType == EcorePackage.Literals.EDOUBLE) {
			return fromExpectedType(eClass, eObject, slot,	feature, SlotValue._Fields.V_DOUBLES, SlotValue._Fields.V_DOUBLE);
		} else if (eType == EcorePackage.Literals.EINT) {
			return fromExpectedType(eClass, eObject, slot,	feature, SlotValue._Fields.V_INTEGERS, SlotValue._Fields.V_INTEGER);
		} else if (eType == EcorePackage.Literals.ELONG) {
			return fromExpectedType(eClass, eObject, slot,	feature, SlotValue._Fields.V_LONGS, SlotValue._Fields.V_LONG);
		} else if (eType == EcorePackage.Literals.ESHORT) {
			return fromExpectedType(eClass, eObject, slot,	feature, SlotValue._Fields.V_SHORTS, SlotValue._Fields.V_SHORT);
		} else if (eType == EcorePackage.Literals.ESTRING) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_STRINGS, SlotValue._Fields.V_STRING);
		} else if (eType == EcorePackage.Literals.EBOOLEAN) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_BOOLEANS, SlotValue._Fields.V_BOOLEAN);
		} else {
			throw new IOException(String.format("Unknown ECore data type '%s'", eType));
		}
	}

	private static Object fromEnum(final EFactory eFactory, final EClass eClass,
			final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature, final EEnum enumType)
			throws IOException {
		if (!slot.value.isSetVStrings() && !slot.value.isSetVString()) {
			throw new IOException(
				String.format(
					"Expected to receive strings for feature '%s' in type '%s' with many='%s', but did not",
					feature.getName(), eClass.getName(), feature.isMany()));
		} else if (feature.isMany()) {
			final List<Object> literals = new ArrayList<>();
			if (slot.value.isSetVStrings()) {
				for (final String s : slot.value.getVStrings()) {
					literals.add(eFactory.createFromString(enumType, s));
				}
			} else {
				literals.add(eFactory.createFromString(enumType, slot.value.getVString()));
			}
			eObject.eSet(feature, literals);
			return literals;
		} else {
			final Object enumLiteral = eFactory.createFromString(enumType, slot.value.getVString());
			eObject.eSet(feature, enumLiteral);
			return enumLiteral;
		}
	}

	private static Object fromFloat(final EClass eClass,
			final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature) throws IOException {
		if (!slot.value.isSetVDoubles() && !slot.value.isSetVDouble()) {
			throw new IOException(
					String.format(
							"Expected to receive doubles for feature '%s' in type '%s', but did not",
							feature.getName(), eClass.getName()));

		} else if (feature.isMany()) {
			final EList<Float> floats = new BasicEList<Float>();
			if (slot.value.isSetVDoubles()) {
				for (final double d : slot.value.getVDoubles()) {
					floats.add((float) d);
				}
			} else {
				floats.add((float) slot.value.getVDouble());
			}
			eObject.eSet(feature, floats);
			return floats;
		} else {
			final double d = slot.value.getVDouble();
			eObject.eSet(feature, (float) d);
			return d;
		}
	}

	private static Object fromInstanceClass(
			final EClass eClass, final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature, final EClassifier eType)
			throws IOException {
		// Fall back on using the Java instance classes
		final Class<?> instanceClass = eType.getInstanceClass();
		if (instanceClass == null) {
			throw new IOException(String.format(
					"Cannot set value for feature '%s' with type '%s', as "
					+ "it is not an Ecore data type and it does not have an instance class",
					feature, eType));
		}

		if (Byte.class.isAssignableFrom(instanceClass) || byte.class.isAssignableFrom(instanceClass)) {
			return fromByte(eClass, eObject, slot, feature);
		} else if (Float.class.isAssignableFrom(instanceClass) || float.class.isAssignableFrom(instanceClass)) {
			return fromFloat(eClass, eObject, slot, feature);
		} else if (Double.class.isAssignableFrom(instanceClass) || double.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_DOUBLES, SlotValue._Fields.V_DOUBLE);
		} else if (Integer.class.isAssignableFrom(instanceClass) || int.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_INTEGERS, SlotValue._Fields.V_INTEGER);
		} else if (Long.class.isAssignableFrom(instanceClass) || long.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_LONGS, SlotValue._Fields.V_LONG);
		} else if (Short.class.isAssignableFrom(instanceClass) || short.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_SHORTS, SlotValue._Fields.V_SHORT);
		} else if (String.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_STRINGS, SlotValue._Fields.V_STRING);
		} else if (Boolean.class.isAssignableFrom(instanceClass) || boolean.class.isAssignableFrom(instanceClass)) {
			return fromExpectedType(eClass, eObject, slot, feature, SlotValue._Fields.V_BOOLEANS, SlotValue._Fields.V_BOOLEAN);
		} else {
			throw new IOException(String.format(
					"Unknown data type for %s#%s %s with isMany = false and instance class %s",
					eClass.getName(), feature.getName(), eType.getName(), feature.isMany(), instanceClass));
		}
	}

	private static Object fromExpectedType(
			final EClass eClass, final EObject eObject, final AttributeSlot slot,
			final EStructuralFeature feature, final SlotValue._Fields expectedMultiType, final _Fields expectedSingleType)
			throws IOException {
		if (!slot.value.isSet(expectedMultiType) && !slot.value.isSet(expectedSingleType)) {
			throw new IOException(
					String.format(
							"Expected to receive '%s' for feature '%s' in type '%s' with many='%s', but did not",
							expectedMultiType, feature.getName(), eClass.getName(),
							feature.isMany()));
		} else if (feature.isMany() && slot.value.isSet(expectedMultiType)) {
			final EList<Object> newValue = ECollections.toEList(
				(Iterable<?>) slot.value.getFieldValue(expectedMultiType));
			eObject.eSet(feature, newValue);
			return newValue;
		} else if (feature.isMany()) {
			final EList<Object> newValue = ECollections.asEList(
				slot.value.getFieldValue(expectedSingleType));
			eObject.eSet(feature, newValue);
			return newValue;
		} else {
			final Object elem = slot.value.getFieldValue(expectedSingleType);
			eObject.eSet(feature, elem);
			return elem;
		}
	}

}
