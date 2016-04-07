/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - use Java 7 Path instead of File+string processing
 ******************************************************************************/
package org.hawk.localfolder;

import java.io.File;
import java.io.IOException;

import org.mapdb.DB;
import org.mapdb.DBMaker;

public class MapDBLocalFolder extends LocalFolder {

	private DB db;

	public MapDBLocalFolder() throws IOException {
		File fMapDB = File.createTempFile("localfolder", "mapdb");
		db = DBMaker.newFileDB(fMapDB).deleteFilesAfterClose().closeOnJvmShutdown().make();
		previousFiles = db.createHashSet("previousFiles").make();
		recordedModifiedDates = db.createHashMap("recordedModifiedDates").make();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		db.close();
	}
	
}
