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
package org.hawk.core.util;

import java.io.PrintStream;

import org.hawk.core.IAbstractConsole;

public class DefaultConsole implements IAbstractConsole {

	static PrintStream out = null;
	static PrintStream err = null;

	/**
	 * 
	 * @param name
	 */
	public DefaultConsole() {

		out = System.out;
		err = System.err;

	}

	@Override
	public void println(String s) {

		out.println(s);

	}

	@Override
	public void printerrln(final String s) {

		err.println(s);

	}

	@Override
	public void print(String s) {

		out.print(s);

	}

}
