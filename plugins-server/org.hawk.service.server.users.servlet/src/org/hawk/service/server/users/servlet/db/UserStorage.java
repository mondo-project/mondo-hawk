/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.users.servlet.db;

import java.io.File;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.HashingPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Sha512Hash;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.TxBlock;
import org.mapdb.TxMaker;
import org.mapdb.TxRollbackException;

public class UserStorage {
	private TxMaker txMaker;

	private static final int HASH_ITERATIONS = 10000;
	private static final String DEFAULT_USER = "admin";
	private static final String DEFAULT_USER_PW = "password";

	public static HashingPasswordService getPasswordService() {
		DefaultHashService hashService = new DefaultHashService();
		hashService.setHashIterations(HASH_ITERATIONS);
		hashService.setHashAlgorithmName(Sha512Hash.ALGORITHM_NAME);
		hashService.setGeneratePublicSalt(true);

		DefaultPasswordService passwordService = new DefaultPasswordService();
		passwordService.setHashService(hashService);
		return passwordService;
	}

	public UserStorage(File dataFile) {
		final DBMaker<?> dbMaker = DBMaker.newFileDB(dataFile).closeOnJvmShutdown().checksumEnable();
		final DB db = dbMaker.make();
		getUserMap(db);
		txMaker = dbMaker.makeTxMaker();

		// Create default user if the database is empty
		txMaker.execute(new TxBlock(){
			@Override
			public void tx(DB db) throws TxRollbackException {
				 final BTreeMap<String, User> userMap = getUserMap(db);
				 if (userMap.isEmpty()) {
					final String hashed = getPasswordService().encryptPassword(DEFAULT_USER_PW);
					final User user = User.builder().username(DEFAULT_USER).hashedPassword(hashed).build();
					userMap.put(DEFAULT_USER, user);
				 }
			}
		});
	}

	public TxMaker getTxMaker() {
		return txMaker;
	}

	public void close() {
		txMaker.close();
	}

	public BTreeMap<String, User> getUserMap(DB db) {
		 return db.createTreeMap("users")
			.keySerializer(BTreeKeySerializer.STRING)
			.valueSerializer(Serializer.JAVA)
			.counterEnable()
			.makeOrGet();
	}
}
