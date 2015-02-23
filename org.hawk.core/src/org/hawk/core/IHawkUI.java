/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import java.util.Set;

import org.hawk.core.query.IQueryEngine;

public interface IHawkUI {

	void addMetaModelParser(IMetaModelResourceFactory parser);

	void addModelParser(IModelResourceFactory parser);

	void addUpdater(IModelUpdater up);

	void addQueryLanguage(IQueryEngine q);

	void setKnownVCSManagerTypes(Set<String> knownvcsmanagers);

	void setKnownBackends(Set<String> knownbackends);

	void addMetaModelUpdater(IMetaModelUpdater metaModelUpdater);

}