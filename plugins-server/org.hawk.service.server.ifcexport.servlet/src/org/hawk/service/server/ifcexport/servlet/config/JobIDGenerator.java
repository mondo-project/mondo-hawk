/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Ran Wei - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.ifcexport.servlet.config;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class JobIDGenerator {

	  private SecureRandom random = new SecureRandom();

	  public String nextSessionId() {
	    return new BigInteger(130, random).toString(32);
	  }

}
