package org.hawk.core;

import java.io.IOException;

public interface IHawk {

	public IModelIndexer getModelIndexer();

	String getDbtype();

	void setDbtype(String dbtype);

	public void init() throws Exception;

	public boolean exists();

	public String decrypt(String user) throws Exception, IOException;

}
