/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import java.io.File;
import java.util.Collection;

import org.hawk.core.model.IHawkModelResource;

/**
 * 
 * @author kb
 * 
 */
public interface IModelResourceFactory {

	String getType();

	String getHumanReadableName();

	IHawkModelResource parse(File f) throws Exception;

	void shutdown();

	// void init(String t, String t2);

	boolean canParse(File f);

	Collection<String> getModelExtensions();

}
