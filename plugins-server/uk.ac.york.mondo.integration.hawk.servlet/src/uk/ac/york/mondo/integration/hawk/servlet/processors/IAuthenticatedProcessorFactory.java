/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.servlet.processors;

import javax.servlet.http.HttpServletRequest;

import org.apache.thrift.TProcessor;

/**
 * Interface for a factory which creates lightweight per-request Thrift
 * processors. The main use for this is making the Thrift service operation
 * implementations aware of the current user, so they may perform access control
 * checks.
 */
public interface IAuthenticatedProcessorFactory {
	TProcessor create(HttpServletRequest p);
}
