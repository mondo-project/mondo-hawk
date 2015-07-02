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
package org.hawk.ui2.util;

import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import org.hawk.core.IAbstractConsole;

public class HConsole implements IAbstractConsole {

	static IOConsole console = null;
	static PrintStream out = null;
	static PrintStream err = null;

	/**
	 * 
	 * @param name
	 */
	public HConsole(String name) {

		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();

		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				console = (IOConsole) existing[i];
				conMan.removeConsoles(new IConsole[] { console });
				break;
			}
		}

		console = new IOConsole(name, null);

		conMan.addConsoles(new IConsole[] { console });

		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		//
		// console.setBackground((PlatformUI.getWorkbench().getDisplay()
		// .getSystemColor(SWT.COLOR_BLACK)));
		//
		// }
		// });

		conMan.addConsoles(new IConsole[] { console });

		if (console.getBackground() != null
				&& console.getBackground().equals(
						(PlatformUI.getWorkbench().getDisplay()
								.getSystemColor(SWT.COLOR_BLACK))))
			// if black background console
			out = newPrintStream();
		else
			out = newPrintStream();

		err = newPrintStream();

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

	public PrintStream newPrintStream(Color color) {
		IOConsoleOutputStream mcs = console.newOutputStream();
		mcs.setActivateOnWrite(true);
		mcs.setColor(color);
		return new PrintStream(mcs);
	}
	public PrintStream newPrintStream() {
		IOConsoleOutputStream mcs = console.newOutputStream();
		mcs.setActivateOnWrite(true);
		return new PrintStream(mcs);
	}

}
