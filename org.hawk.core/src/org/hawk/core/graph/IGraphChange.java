/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.graph;

public interface IGraphChange {

	// element types:
	public final static String FILE = "FILE";
	public final static String METAMODEL = "METAMODEL";
	public final static String TYPE = "TYPE";
	public final static String INSTANCE = "INSTANCE";
	public final static String PROPERTY = "PROPERTY";
	public final static String REFERENCE = "REFERENCE";

	public boolean getChangeType();

	public String getElementType();

	public String getIdentifier();

	public Object getValue();

	public boolean isTransient();

}