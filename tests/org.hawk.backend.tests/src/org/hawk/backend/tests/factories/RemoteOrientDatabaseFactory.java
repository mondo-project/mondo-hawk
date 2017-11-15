/*******************************************************************************
 * Copyright (c) 2015-2017 The University of York, Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.backend.tests.factories;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.orientdb.RemoteOrientDatabase;

public final class RemoteOrientDatabaseFactory implements IGraphDatabaseFactory {
	@Override
	public IGraphDatabase create() {
		return new RemoteOrientDatabase();
	}

	@Override
	public String toString() {
		return "RemoteOrientDB";
	}
}