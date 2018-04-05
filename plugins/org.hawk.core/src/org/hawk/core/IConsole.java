/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

public interface IConsole {

	/**
	 * Outputs String s and then a new line
	 * 
	 * @param s
	 */
	void println(String s);

	/**
	 * Outputs String s and then a new line, attempting to use red as the colour
	 * used
	 * 
	 * @param s
	 */
	void printerrln(String s);

	/**
	 * Outputs String s
	 * 
	 * @param s
	 */
	void print(String s);

	/**
	 * Outputs Throwable t (if possible) and then a new line, attempting to use
	 * red as the colour used
	 * 
	 * @param s
	 */
	void printerrln(Throwable t);

}
