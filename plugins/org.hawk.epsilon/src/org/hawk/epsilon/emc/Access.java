/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import org.hawk.core.query.IAccess;

public class Access implements IAccess {

	// id
	private String sourceObject;
	// id
	private String accessObject;
	// name
	private String property;

	public Access(String sourceObject, String accessObject2,
			String property) {
		this.setSourceObjectID(sourceObject);
		this.setAccessObjectID(accessObject2);
		this.setProperty(property);
	}

	public String getSourceObjectID() {
		return sourceObject;
	}

	public void setSourceObjectID(String sourceObject) {
		this.sourceObject = sourceObject;
	}

	public String getAccessObjectID() {
		return accessObject;
	}

	public void setAccessObjectID(String accessObject) {
		this.accessObject = accessObject;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
