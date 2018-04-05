/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.util;

import java.io.PrintStream;

import org.hawk.core.IConsole;

public class DefaultConsole implements IConsole {

	PrintStream out = null;
	PrintStream err = null;

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

	@Override
	public void printerrln(Throwable t) {
		err.println(t.getMessage());
		t.printStackTrace(err);
	}

}
