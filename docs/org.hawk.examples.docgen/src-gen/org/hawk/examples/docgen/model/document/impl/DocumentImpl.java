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

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import org.hawk.examples.docgen.model.document.Author;
import org.hawk.examples.docgen.model.document.Document;
import org.hawk.examples.docgen.model.document.DocumentPackage;
import org.hawk.examples.docgen.model.document.Tag;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Document</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.DocumentImpl#getWrittenBy <em>Written By</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.DocumentImpl#getTags <em>Tags</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.DocumentImpl#getCites <em>Cites</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.impl.DocumentImpl#getText <em>Text</em>}</li>
 * </ul>
 *
 * @generated
 */
public class DocumentImpl extends MinimalEObjectImpl.Container implements Document {
	/**
	 * The cached value of the '{@link #getWrittenBy() <em>Written By</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWrittenBy()
	 * @generated
	 * @ordered
	 */
	protected EList<Author> writtenBy;

	/**
	 * The cached value of the '{@link #getTags() <em>Tags</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTags()
	 * @generated
	 * @ordered
	 */
	protected EList<Tag> tags;

	/**
	 * The cached value of the '{@link #getCites() <em>Cites</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCites()
	 * @generated
	 * @ordered
	 */
	protected EList<Document> cites;

	/**
	 * The default value of the '{@link #getText() <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getText()
	 * @generated
	 * @ordered
	 */
	protected static final String TEXT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getText() <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getText()
	 * @generated
	 * @ordered
	 */
	protected String text = TEXT_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected DocumentImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return DocumentPackage.Literals.DOCUMENT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Author> getWrittenBy() {
		if (writtenBy == null) {
			writtenBy = new EObjectResolvingEList<Author>(Author.class, this, DocumentPackage.DOCUMENT__WRITTEN_BY);
		}
		return writtenBy;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Tag> getTags() {
		if (tags == null) {
			tags = new EObjectResolvingEList<Tag>(Tag.class, this, DocumentPackage.DOCUMENT__TAGS);
		}
		return tags;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Document> getCites() {
		if (cites == null) {
			cites = new EObjectResolvingEList<Document>(Document.class, this, DocumentPackage.DOCUMENT__CITES);
		}
		return cites;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getText() {
		return text;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setText(String newText) {
		String oldText = text;
		text = newText;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, DocumentPackage.DOCUMENT__TEXT, oldText, text));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case DocumentPackage.DOCUMENT__WRITTEN_BY:
				return getWrittenBy();
			case DocumentPackage.DOCUMENT__TAGS:
				return getTags();
			case DocumentPackage.DOCUMENT__CITES:
				return getCites();
			case DocumentPackage.DOCUMENT__TEXT:
				return getText();
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
			case DocumentPackage.DOCUMENT__WRITTEN_BY:
				getWrittenBy().clear();
				getWrittenBy().addAll((Collection<? extends Author>)newValue);
				return;
			case DocumentPackage.DOCUMENT__TAGS:
				getTags().clear();
				getTags().addAll((Collection<? extends Tag>)newValue);
				return;
			case DocumentPackage.DOCUMENT__CITES:
				getCites().clear();
				getCites().addAll((Collection<? extends Document>)newValue);
				return;
			case DocumentPackage.DOCUMENT__TEXT:
				setText((String)newValue);
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
			case DocumentPackage.DOCUMENT__WRITTEN_BY:
				getWrittenBy().clear();
				return;
			case DocumentPackage.DOCUMENT__TAGS:
				getTags().clear();
				return;
			case DocumentPackage.DOCUMENT__CITES:
				getCites().clear();
				return;
			case DocumentPackage.DOCUMENT__TEXT:
				setText(TEXT_EDEFAULT);
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
			case DocumentPackage.DOCUMENT__WRITTEN_BY:
				return writtenBy != null && !writtenBy.isEmpty();
			case DocumentPackage.DOCUMENT__TAGS:
				return tags != null && !tags.isEmpty();
			case DocumentPackage.DOCUMENT__CITES:
				return cites != null && !cites.isEmpty();
			case DocumentPackage.DOCUMENT__TEXT:
				return TEXT_EDEFAULT == null ? text != null : !TEXT_EDEFAULT.equals(text);
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
		result.append(" (text: ");
		result.append(text);
		result.append(')');
		return result.toString();
	}

} //DocumentImpl
