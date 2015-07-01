package org.hawk.core.runtime;

import java.io.File;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IHawk;
import org.hawk.core.IModelIndexer;

public class RemoteHawk implements IHawk {

	private File location;

	private IModelIndexer indexer;

	public RemoteHawk(String name, File loc, IAbstractConsole c)
			throws Exception {

		// FIXME

	}

	@Override
	public IModelIndexer getModelIndexer() {
		return indexer;
	}

	@Override
	public String getDbtype() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDbtype(String dbtype) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String decrypt(String user) {
		// TODO Auto-generated method stub
		return null;
	}

}
