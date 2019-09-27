/*******************************************************************************
 * Copyright (c) 2020 Aston University.
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
 *     Antonio Garcia-Dominguez - original idea and implementation
 ******************************************************************************/
package org.hawk.core;

public class FailedMetamodelRegistrationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5729317672040153337L;

	public FailedMetamodelRegistrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public FailedMetamodelRegistrationException(String message) {
		super(message);
	}

	public FailedMetamodelRegistrationException(Throwable cause) {
		super(cause);
	}
	
}
