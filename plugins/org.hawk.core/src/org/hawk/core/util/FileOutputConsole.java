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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.hawk.core.IConsole;

public class FileOutputConsole implements IConsole {

	FileWriter r = null;

	/**
	 * 
	 * @param name
	 */
	public FileOutputConsole() {

		try {
			r = new FileWriter("log_" + System.currentTimeMillis() + ".txt",
					true);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void println(String s) {

		try {
			r.append(s + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void printerrln(final String s) {

		try {
			r.append(s + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void print(String s) {

		try {
			r.append(s);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void flush() {
		try {
			r.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void printerrln(Throwable t) {
		printerrln(t.getMessage());
		StringWriter sr = new StringWriter();
		t.printStackTrace(new PrintWriter(sr));
		printerrln(sr.toString());
	}

}
