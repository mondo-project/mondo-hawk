/**
 * Copyright (c) 2018 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Antonio Garcia-Dominguez - initial API and implementation
 */
package org.hawk.examples.docgen.model.document;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Author</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.Author#getName <em>Name</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Author#getKnows <em>Knows</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Author#getIsKnownBy <em>Is Known By</em>}</li>
 * </ul>
 *
 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getAuthor()
 * @model
 * @generated
 */
public interface Author extends EObject {
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
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getAuthor_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link org.hawk.examples.docgen.model.document.Author#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Knows</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Author}.
	 * It is bidirectional and its opposite is '{@link org.hawk.examples.docgen.model.document.Author#getIsKnownBy <em>Is Known By</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Knows</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Knows</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getAuthor_Knows()
	 * @see org.hawk.examples.docgen.model.document.Author#getIsKnownBy
	 * @model opposite="isKnownBy"
	 * @generated
	 */
	EList<Author> getKnows();

	/**
	 * Returns the value of the '<em><b>Is Known By</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Author}.
	 * It is bidirectional and its opposite is '{@link org.hawk.examples.docgen.model.document.Author#getKnows <em>Knows</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Is Known By</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Is Known By</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getAuthor_IsKnownBy()
	 * @see org.hawk.examples.docgen.model.document.Author#getKnows
	 * @model opposite="knows"
	 * @generated
	 */
	EList<Author> getIsKnownBy();

} // Author
