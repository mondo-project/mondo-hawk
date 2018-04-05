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
