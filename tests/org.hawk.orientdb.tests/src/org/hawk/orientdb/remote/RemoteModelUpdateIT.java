/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb.remote;

import java.io.File;

import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.ModelUpdateTest;
import org.hawk.orientdb.RemoteOrientDatabase;

public class RemoteModelUpdateIT extends ModelUpdateTest {

	@Override
	protected void createDB(File dbFolder) throws Exception {
		final RemoteOrientDatabase remoteDB = new RemoteOrientDatabase();
		db = remoteDB;
		remoteDB.setStorageType(RemoteOrientDatabase.DBSTORAGE_MEMORY);

		db.run("remote:localhost/" + dbFolder.getName(), null, new DefaultConsole());
	}
	
}
