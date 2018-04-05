/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.backend.tests.factories;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.orientdb.OrientDatabase;

public final class OrientDatabaseFactory implements IGraphDatabaseFactory {
	@Override
	public IGraphDatabase create() {
		return new OrientDatabase();
	}

	@Override
	public String toString() {
		return "OrientDB";
	}
}