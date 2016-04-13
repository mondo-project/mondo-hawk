package org.hawk.orientdb.remote;

import org.hawk.core.util.DefaultConsole;
import org.hawk.orientdb.GraphPopulationTest;
import org.hawk.orientdb.RemoteOrientDatabase;

public class RemoteGraphPopulationIT extends GraphPopulationTest {

	@Override
	public void setup(String testCase) throws Exception {
		final RemoteOrientDatabase remoteDB = new RemoteOrientDatabase();
		db = remoteDB;
		remoteDB.setStorageType(RemoteOrientDatabase.DBSTORAGE_MEMORY);

		db.run("remote:localhost/" + testCase, null, new DefaultConsole());
	}

}
