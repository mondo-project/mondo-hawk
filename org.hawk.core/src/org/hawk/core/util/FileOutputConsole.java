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
package org.hawk.core.util;

import java.io.FileWriter;
import java.io.IOException;

import org.hawk.core.IAbstractConsole;

public class FileOutputConsole implements IAbstractConsole {

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

}
