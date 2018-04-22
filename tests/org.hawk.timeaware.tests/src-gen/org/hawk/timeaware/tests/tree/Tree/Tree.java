/**
 */
package org.hawk.timeaware.tests.tree.Tree;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tree</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.hawk.timeaware.tests.tree.Tree.Tree#getChildren <em>Children</em>}</li>
 *   <li>{@link org.hawk.timeaware.tests.tree.Tree.Tree#getParent <em>Parent</em>}</li>
 *   <li>{@link org.hawk.timeaware.tests.tree.Tree.Tree#getLabel <em>Label</em>}</li>
 * </ul>
 *
 * @see org.hawk.timeaware.tests.tree.Tree.TreePackage#getTree()
 * @model
 * @generated
 */
public interface Tree extends EObject {
	/**
	 * Returns the value of the '<em><b>Children</b></em>' containment reference list.
	 * The list contents are of type {@link org.hawk.timeaware.tests.tree.Tree.Tree}.
	 * It is bidirectional and its opposite is '{@link org.hawk.timeaware.tests.tree.Tree.Tree#getParent <em>Parent</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Children</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Children</em>' containment reference list.
	 * @see org.hawk.timeaware.tests.tree.Tree.TreePackage#getTree_Children()
	 * @see org.hawk.timeaware.tests.tree.Tree.Tree#getParent
	 * @model opposite="parent" containment="true"
	 * @generated
	 */
	EList<Tree> getChildren();

	/**
	 * Returns the value of the '<em><b>Parent</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link org.hawk.timeaware.tests.tree.Tree.Tree#getChildren <em>Children</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Parent</em>' container reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Parent</em>' container reference.
	 * @see #setParent(Tree)
	 * @see org.hawk.timeaware.tests.tree.Tree.TreePackage#getTree_Parent()
	 * @see org.hawk.timeaware.tests.tree.Tree.Tree#getChildren
	 * @model opposite="children" transient="false"
	 * @generated
	 */
	Tree getParent();

	/**
	 * Sets the value of the '{@link org.hawk.timeaware.tests.tree.Tree.Tree#getParent <em>Parent</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Parent</em>' container reference.
	 * @see #getParent()
	 * @generated
	 */
	void setParent(Tree value);

	/**
	 * Returns the value of the '<em><b>Label</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Label</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Label</em>' attribute.
	 * @see #setLabel(String)
	 * @see org.hawk.timeaware.tests.tree.Tree.TreePackage#getTree_Label()
	 * @model
	 * @generated
	 */
	String getLabel();

	/**
	 * Sets the value of the '{@link org.hawk.timeaware.tests.tree.Tree.Tree#getLabel <em>Label</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Label</em>' attribute.
	 * @see #getLabel()
	 * @generated
	 */
	void setLabel(String value);

} // Tree
