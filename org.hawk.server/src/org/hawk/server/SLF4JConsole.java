/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - move to servlet project, switch to SLF4J
 ******************************************************************************/
package org.hawk.server;

import org.hawk.core.IAbstractConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

public class SLF4JConsole implements IAbstractConsole {
	private static final String FQCN = SLF4JConsole.class.getName();

	@Override
	public void println(String s) {
		final Logger logger = getCallerLogger();
		if (logger instanceof LocationAwareLogger) {
			((LocationAwareLogger) logger).log(null, FQCN, LocationAwareLogger.INFO_INT, s, null, null);
		} else {
			logger.info(s);
		}
	}

	@Override
	public void printerrln(String s) {
		final Logger logger = getCallerLogger();
		if (logger instanceof LocationAwareLogger) {
			((LocationAwareLogger) logger).log(null, FQCN, LocationAwareLogger.ERROR_INT, s, null, null);
		} else {
			logger.error(s);
		}
	}

	@Override
	public void print(String s) {
		println(s);
	}

	private static String getCallerClassName() {
		// From http://stackoverflow.com/questions/11306811/
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		for (int i = 1; i < stElements.length; i++) {
			StackTraceElement ste = stElements[i];
			if (!ste.getClassName().equals(SLF4JConsole.class.getName())
					&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
				return ste.getClassName();
			}
		}
		return null;
	}

	private static Logger getCallerLogger() {
		return LoggerFactory.getLogger(getCallerClassName());
	}
}
