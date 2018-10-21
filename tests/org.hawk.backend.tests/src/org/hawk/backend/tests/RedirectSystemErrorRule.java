/*******************************************************************************
 * Copyright (c) 2015-2018 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.backend.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.junit.rules.ExternalResource;

/**
 * JUnit 4 test rule that redirects standard error to another file.
 * Needed to avoid the Travis 4MB log limit.
 */
public class RedirectSystemErrorRule extends ExternalResource {
	private PrintStream oldErrorStream;
	private PrintStream newErrorStream;

	@Override
	protected void before() throws Throwable {
		oldErrorStream = System.err;

		final boolean append = true;
		newErrorStream = new PrintStream(new FileOutputStream(new File("stderr.txt"), append));
		System.setErr(newErrorStream);
	}

	@Override
	protected void after() {
		System.setErr(oldErrorStream);
		newErrorStream.close();
	}
}
