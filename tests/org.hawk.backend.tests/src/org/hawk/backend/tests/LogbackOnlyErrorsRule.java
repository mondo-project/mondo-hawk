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

import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * JUnit 4 test rule that changes logging so it will only report errors.
 * This is needed so we won't hit the 4MB log size limit from Travis and
 * have the job terminated.
 */
public class LogbackOnlyErrorsRule extends ExternalResource {
	private Level oldLevel;
	private Logger logger;

	@Override
	protected void before() throws Throwable {
		Object rawLogger = LoggerFactory.getLogger("org.hawk");
		if (rawLogger instanceof Logger) {
			logger = (Logger) LoggerFactory.getLogger("org.hawk");
			oldLevel = logger.getLevel();
			logger.setLevel(Level.ERROR);
		}
	}

	@Override
	protected void after() {
		if (logger != null) {
			logger.setLevel(oldLevel);
		}
	}
}
