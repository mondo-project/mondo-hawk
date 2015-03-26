package org.hawk.core;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface IHawk {

	public void loadPropertiesXML();

	public void savePropertiesXML();

	public IModelIndexer getModelIndexer();

	String getDbtype();

	void setDbtype(String dbtype);

	public void init() throws Exception;

	public String decrypt(String user) throws GeneralSecurityException,
			IOException;

	public String encrypt(String user) throws GeneralSecurityException,
			IOException;

}
