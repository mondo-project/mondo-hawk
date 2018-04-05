/**
 * Copyright (c) 2018 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
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

import org.hawk.examples.docgen.model.document.Author;
import org.hawk.examples.docgen.model.document.DocumentPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Author</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.AuthorImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.AuthorImpl#getKnows <em>Knows</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.AuthorImpl#getIsKnownBy <em>Is Known By</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AuthorImpl extends MinimalEObjectImpl.Container implements Author {
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
	 * The cached value of the '{@link #getKnows() <em>Knows</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getKnows()
	 * @generated
	 * @ordered
	 */
	protected EList<Author> knows;

	/**
	 * The cached value of the '{@link #getIsKnownBy() <em>Is Known By</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIsKnownBy()
	 * @generated
	 * @ordered
	 */
	protected EList<Author> isKnownBy;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AuthorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return DocumentPackage.Literals.AUTHOR;
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
			eNotify(new ENotificationImpl(this, Notification.SET, DocumentPackage.AUTHOR__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Author> getKnows() {
		if (knows == null) {
			knows = new EObjectWithInverseResolvingEList.ManyInverse<Author>(Author.class, this, DocumentPackage.AUTHOR__KNOWS, DocumentPackage.AUTHOR__IS_KNOWN_BY);
		}
		return knows;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Author> getIsKnownBy() {
		if (isKnownBy == null) {
			isKnownBy = new EObjectWithInverseResolvingEList.ManyInverse<Author>(Author.class, this, DocumentPackage.AUTHOR__IS_KNOWN_BY, DocumentPackage.AUTHOR__KNOWS);
		}
		return isKnownBy;
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
			case DocumentPackage.AUTHOR__KNOWS:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getKnows()).basicAdd(otherEnd, msgs);
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				return ((InternalEList<InternalEObject>)(InternalEList<?>)getIsKnownBy()).basicAdd(otherEnd, msgs);
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
			case DocumentPackage.AUTHOR__KNOWS:
				return ((InternalEList<?>)getKnows()).basicRemove(otherEnd, msgs);
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				return ((InternalEList<?>)getIsKnownBy()).basicRemove(otherEnd, msgs);
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
			case DocumentPackage.AUTHOR__NAME:
				return getName();
			case DocumentPackage.AUTHOR__KNOWS:
				return getKnows();
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				return getIsKnownBy();
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
			case DocumentPackage.AUTHOR__NAME:
				setName((String)newValue);
				return;
			case DocumentPackage.AUTHOR__KNOWS:
				getKnows().clear();
				getKnows().addAll((Collection<? extends Author>)newValue);
				return;
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				getIsKnownBy().clear();
				getIsKnownBy().addAll((Collection<? extends Author>)newValue);
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
			case DocumentPackage.AUTHOR__NAME:
				setName(NAME_EDEFAULT);
				return;
			case DocumentPackage.AUTHOR__KNOWS:
				getKnows().clear();
				return;
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				getIsKnownBy().clear();
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
			case DocumentPackage.AUTHOR__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case DocumentPackage.AUTHOR__KNOWS:
				return knows != null && !knows.isEmpty();
			case DocumentPackage.AUTHOR__IS_KNOWN_BY:
				return isKnownBy != null && !isKnownBy.isEmpty();
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

} //AuthorImpl
