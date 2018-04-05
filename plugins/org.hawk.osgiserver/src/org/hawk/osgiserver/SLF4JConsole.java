/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - switch to SLF4J
 ******************************************************************************/
package org.hawk.osgiserver;

import org.hawk.core.IConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

public class SLF4JConsole implements IConsole {
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

	@Override
	public void printerrln(Throwable t) {
		final Logger logger = getCallerLogger();
		if (logger instanceof LocationAwareLogger) {
			((LocationAwareLogger) logger).log(null, FQCN, LocationAwareLogger.ERROR_INT, t.getMessage(), null, t);
		} else {
			logger.error(t.getMessage(), t);
		}
	}
}
