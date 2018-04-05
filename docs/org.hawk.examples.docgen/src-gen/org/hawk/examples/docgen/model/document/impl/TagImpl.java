/**
 * Copyright (c) 2018 Aston University.
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
 *   Antonio Garcia-Dominguez - initial API and implementation
 */
package org.hawk.examples.docgen.model.document.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.hawk.examples.docgen.model.document.DocumentPackage;
import org.hawk.examples.docgen.model.document.Tag;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tag</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.TagImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.TagImpl#getIsKindOf <em>Is Kind Of</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.TagImpl#getHasKinds <em>Has Kinds</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TagImpl extends MinimalEObjectImpl.Container implements Tag {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getIsKindOf() <em>Is Kind Of</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIsKindOf()
	 * @generated
	 * @ordered
	 */
	protected EList<Tag> isKindOf;

	/**
	 * The cached value of the '{@link #getHasKinds() <em>Has Kinds</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getHasKinds()
	 * @generated
	 * @ordered
	 */
	protected EList<Tag> hasKinds;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TagImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return DocumentPackage.Literals.TAG;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, DocumentPackage.TAG__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Tag> getIsKindOf() {
		if (isKindOf == null) {
			isKindOf = new EObjectWithInverseResolvingEList.ManyInverse<Tag>(Tag.class, this, DocumentPackage.TAG__IS_KIND_OF, DocumentPackage.TAG__HAS_KINDS);
		}
		return isKindOf;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Tag> getHasKinds() {
		if (hasKinds == null) {
			hasKinds = new EObjectWithInverseResolvingEList.ManyInverse<Tag>(Tag.class, this, DocumentPackage.TAG__HAS_KINDS, DocumentPackage.TAG__IS_KIND_OF);
		}
		return hasKinds;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case DocumentPackage.TAG__IS_KIND_OF:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getIsKindOf()).basicAdd(otherEnd, msgs);
			case DocumentPackage.TAG__HAS_KINDS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getHasKinds()).basicAdd(otherEnd, msgs);
		}
		return super.eInverseAdd(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case DocumentPackage.TAG__IS_KIND_OF:
				return ((InternalEList<?>)getIsKindOf()).basicRemove(otherEnd, msgs);
			case DocumentPackage.TAG__HAS_KINDS:
				return ((InternalEList<?>)getHasKinds()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case DocumentPackage.TAG__NAME:
				return getName();
			case DocumentPackage.TAG__IS_KIND_OF:
				return getIsKindOf();
			case DocumentPackage.TAG__HAS_KINDS:
				return getHasKinds();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case DocumentPackage.TAG__NAME:
				setName((String)newValue);
				return;
			case DocumentPackage.TAG__IS_KIND_OF:
				getIsKindOf().clear();
				getIsKindOf().addAll((Collection<? extends Tag>)newValue);
				return;
			case DocumentPackage.TAG__HAS_KINDS:
				getHasKinds().clear();
				getHasKinds().addAll((Collection<? extends Tag>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case DocumentPackage.TAG__NAME:
				setName(NAME_EDEFAULT);
				return;
			case DocumentPackage.TAG__IS_KIND_OF:
				getIsKindOf().clear();
				return;
			case DocumentPackage.TAG__HAS_KINDS:
				getHasKinds().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case DocumentPackage.TAG__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case DocumentPackage.TAG__IS_KIND_OF:
				return isKindOf != null && !isKindOf.isEmpty();
			case DocumentPackage.TAG__HAS_KINDS:
				return hasKinds != null && !hasKinds.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (name: ");
		result.append(name);
		result.append(')');
		return result.toString();
	}

} //TagImpl
