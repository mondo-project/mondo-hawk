package org.hawk.orientdb;

import java.io.File;
import java.io.IOException;

import org.hawk.core.IConsole;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.client.remote.OStorageRemote;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * Alternate backend that uses a remote OrientDB instance instead of a local one.
 * Needed for multi-master configuration.
 */
public class RemoteOrientDatabase extends OrientDatabase {
	private static final String DBTYPE_DOC = "document";

	public static final String DBSTORAGE_PLOCAL = "plocal";
	public static final String DBSTORAGE_MEMORY = "memory";

	private String rootUsername = "root";
	private String rootPassword = "root";
	private String dbUsername = "admin";
	private String dbPassword = "admin";
	private String storageType = DBSTORAGE_PLOCAL;

	@Override
	public void run(File parentfolder, IConsole c) {
		try {
			run("remote:localhost/" + parentfolder.getName(), parentfolder, c);
		} catch (IOException e) {
			c.printerrln(e);
		}
	}

	@Override
	public void delete() throws Exception {
		getServerAdmin(getGraphAsIs()).dropDatabase(storageType);
	}

	@Override
	public ODatabaseDocumentTx getGraph() {
		ODatabaseDocumentTx db = getGraphAsIs();

		if (db.isClosed()) {
			try {
				final OServerAdmin admin = getServerAdmin(db);
				if (!admin.existsDatabase(storageType)) {
					admin.createDatabase(DBTYPE_DOC, storageType);
				}
				admin.close();

				// need to reconnect - otherwise isClosed flag is not updated
				db = new ODatabaseDocumentTx(dbURL);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}

			db.open(dbUsername, dbPassword);
		}
		return db;
	}

	protected OServerAdmin getServerAdmin(ODatabaseDocumentTx db) throws Exception {
		OStorage underlying = getUnderlyingStorage(db.getStorage());
		OServerAdmin admin = new OServerAdmin((OStorageRemote)underlying);

		// TODO: add options for dbUsername/pw (and a specific remote: URL)
		admin.connect(rootUsername, rootPassword);

		return admin;
	}

	protected OStorage getUnderlyingStorage(OStorage storage) {
		OStorage underlying = storage.getUnderlying();
		if (underlying != storage) {
			return getUnderlyingStorage(underlying);
		} else {
			return underlying;
		}
	}

	public String getRootUsername() {
		return rootUsername;
	}

	public void setRootUsername(String rootUsername) {
		this.rootUsername = rootUsername;
	}

	public String getRootPassword() {
		return rootPassword;
	}

	public void setRootPassword(String rootPassword) {
		this.rootPassword = rootPassword;
	}

	public String getDbUsername() {
		return dbUsername;
	}

	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	/**
	 * Returns the storage type, which can be either {@link #DBSTORAGE_PLOCAL} or {@link #DBSTORAGE_MEMORY}.
	 */
	public String getStorageType() {
		return storageType;
	}

	/**
	 * Changes the storage type, which should be either {@link #DBSTORAGE_PLOCAL} or {@link #DBSTORAGE_MEMORY}.
	 */
	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}
}
