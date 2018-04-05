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
package org.hawk.examples.docgen.model.document;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Document</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.examples.docgen.model.document.Document#getWrittenBy <em>Written By</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Document#getTags <em>Tags</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Document#getCites <em>Cites</em>}</li>
 *   <li>{@link org.hawk.examples.docgen.model.document.Document#getText <em>Text</em>}</li>
 * </ul>
 *
 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getDocument()
 * @model
 * @generated
 */
public interface Document extends EObject {
	/**
	 * Returns the value of the '<em><b>Written By</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Author}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Written By</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Written By</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getDocument_WrittenBy()
	 * @model
	 * @generated
	 */
	EList<Author> getWrittenBy();

	/**
	 * Returns the value of the '<em><b>Tags</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Tag}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Tags</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tags</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getDocument_Tags()
	 * @model
	 * @generated
	 */
	EList<Tag> getTags();

	/**
	 * Returns the value of the '<em><b>Cites</b></em>' reference list.
	 * The list contents are of type {@link org.hawk.examples.docgen.model.document.Document}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Cites</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cites</em>' reference list.
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getDocument_Cites()
	 * @model
	 * @generated
	 */
	EList<Document> getCites();

	/**
	 * Returns the value of the '<em><b>Text</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Text</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Text</em>' attribute.
	 * @see #setText(String)
	 * @see org.hawk.examples.docgen.model.document.DocumentPackage#getDocument_Text()
	 * @model
	 * @generated
	 */
	String getText();

	/**
	 * Sets the value of the '{@link org.hawk.examples.docgen.model.document.Document#getText <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Text</em>' attribute.
	 * @see #getText()
	 * @generated
	 */
	void setText(String value);

} // Document
