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
package org.hawk.examples.docgen.model.document;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tag</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.Tag#getName <em>Name</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Tag#getIsKindOf <em>Is Kind Of</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Tag#getHasKinds <em>Has Kinds</em>}</li>
 * </ul>
 *
 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getTag()
 * @model
 * @generated
 */
public interface Tag extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getTag_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.hawk.examples.docgen.model.document.Tag#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Is Kind Of</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Tag}.
	 * It is bidirectional and its opposite is '{@link org.hawk.examples.docgen.model.document.Tag#getHasKinds <em>Has Kinds</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Is Kind Of</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Is Kind Of</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getTag_IsKindOf()
	 * @see org.hawk.examples.docgen.model.document.Tag#getHasKinds
	 * @model opposite="hasKinds"
	 * @generated
	 */
	EList<Tag> getIsKindOf();

	/**
	 * Returns the value of the '<em><b>Has Kinds</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Tag}.
	 * It is bidirectional and its opposite is '{@link org.hawk.examples.docgen.model.document.Tag#getIsKindOf <em>Is Kind Of</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Has Kinds</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Has Kinds</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getTag_HasKinds()
	 * @see org.hawk.examples.docgen.model.document.Tag#getIsKindOf
	 * @model opposite="isKindOf"
	 * @generated
	 */
	EList<Tag> getHasKinds();

} // Tag
