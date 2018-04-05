/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.core.query;

public class InvalidQueryException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidQueryException() {
		super();
	}

	public InvalidQueryException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidQueryException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidQueryException(String message) {
		super(message);
	}

	/**
	 * Wraps an existing throwable as an invalid query exception.
	 */
	public InvalidQueryException(Throwable cause) { 
		super(cause);
	}
	
}
