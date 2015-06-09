package org.hawk.core.runtime;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.hawk.core.IAbstractConsole;
import org.hawk.core.IHawk;
import org.hawk.core.IModelIndexer;
import org.hawk.core.runtime.util.SecurityManager;

public class LocalHawk implements IHawk {

	private File location;

	private IModelIndexer indexer;

	private String dbtype;

	// not accessible or changeable as security is not yet handled
	private static String apw = "admin";

	public LocalHawk(String name, File loc, IAbstractConsole c)
			throws Exception {

		location = loc;

		indexer = new ModelIndexerImpl(name, location, c);

	}

	@Override
	public IModelIndexer getModelIndexer() {
		return indexer;
	}

	@Override
	public String getDbtype() {
		return dbtype;
	}

	@Override
	public void setDbtype(String dbtype) {
		this.dbtype = dbtype;
	}

	@Override
	public void init() throws Exception {
		indexer.init(apw.toCharArray());
	}

	@Override
	public boolean exists() {
		return location.exists();
	}

	@Override
	public String decrypt(String pw) throws GeneralSecurityException, IOException {
		return SecurityManager.decrypt(pw, apw.toCharArray());
	}

}
