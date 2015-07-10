/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

public interface IAbstractConsole {

	/**
	 * Outputs String s and then a new line
	 * @param s
	 */
	public abstract void println(String s);

	/**
	 * Outputs String s and then a new line, attempting to use red as the colour used 
	 * @param s
	 */
	public abstract void printerrln(String s);

	/**
	 * Outputs String s
	 * @param s
	 */
	public abstract void print(String s);
	
}
