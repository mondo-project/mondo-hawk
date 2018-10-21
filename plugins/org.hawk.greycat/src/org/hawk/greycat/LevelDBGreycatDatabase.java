package org.hawk.greycat;

import greycat.leveldb.LevelDBStorage;
import greycat.plugin.Storage;

public class LevelDBGreycatDatabase extends GreycatDatabase {

	@Override
	protected Storage createStorage() {
		return new LevelDBStorage(storageFolder.getAbsolutePath());
	}
	
}
