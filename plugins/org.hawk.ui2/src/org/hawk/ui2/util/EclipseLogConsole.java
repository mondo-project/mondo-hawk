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
 *     Dimitrios Kolovos - initial API and implementation
 *     Konstantinos Barmpis - updates and maintenance
 ******************************************************************************/
package org.hawk.ui2.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.hawk.core.IConsole;
import org.hawk.ui2.Activator;

public class EclipseLogConsole implements IConsole {

	@Override
	public void println(String s) {
		log(s, IStatus.INFO);
	}

	@Override
	public void printerrln(String s) {
		log(s, IStatus.ERROR);
	}

	@Override
	public void print(String s) {
		println(s);
	}

	protected void log(String s, int severity) {
		Activator.getDefault().getLog()
				.log(new Status(severity, "org.hawk.ui2", 0, s, null));
	}

	@Override
	public void printerrln(Throwable t) {
		Activator
				.getDefault()
				.getLog()
				.log(new Status(IStatus.ERROR, "org.hawk.ui2", 0, t
						.getMessage(), t));
	}

}
