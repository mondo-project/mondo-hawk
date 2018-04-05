/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.server.users.servlet.servlets;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.server.TServlet;
import org.hawk.service.api.UserExists;
import org.hawk.service.api.UserNotFound;
import org.hawk.service.api.UserProfile;
import org.hawk.service.api.Users;
import org.hawk.service.api.Users.Iface;
import org.hawk.service.server.users.servlet.UsersPlugin;
import org.hawk.service.server.users.servlet.db.User;
import org.hawk.service.server.users.servlet.db.UserStorage;
import org.mapdb.BTreeMap;
import org.mapdb.DB;

public class UserServlet extends TServlet {
	private static final long serialVersionUID = 1L;

	private static class UsersIface implements Iface {

		@Override
		public void createUser(String username, String password, UserProfile profile) throws UserExists, TException {
			final UserStorage storage = UsersPlugin.getInstance().getStorage();
			final DB db = storage.getTxMaker().makeTx();
			try {
				BTreeMap<String, User> userMap = storage.getUserMap(db);
				if (userMap.containsKey(username)) {
					throw new UserExists();
				}

				final String hashed = UserStorage.getPasswordService().encryptPassword(password);
				final User user = User.builder()
						.username(username)
						.hashedPassword(hashed)
						.realName(profile.realName)
						.isAdmin(profile.admin)
						.build();
				userMap.put(username, user);
				db.commit();
			} finally {
				db.close();
			}
		}

		@Override
		public void updateProfile(String username, UserProfile profile) throws UserNotFound, TException {
			final UserStorage storage = UsersPlugin.getInstance().getStorage();
			final DB db = storage.getTxMaker().makeTx();
			try {
				final BTreeMap<String, User> userMap = storage.getUserMap(db);
				final User oldUser = userMap.get(username);
				if (oldUser == null) {
					throw new UserNotFound();
				}

				final User newUser = User.builder(oldUser)
						.realName(profile.realName)
						.isAdmin(profile.admin)
						.build();
				userMap.put(username, newUser);
				db.commit();
			} finally {
				db.close();
			}
		}

		@Override
		public void updatePassword(String username, String password) throws UserNotFound, TException {
			final UserStorage storage = UsersPlugin.getInstance().getStorage();
			final DB db = storage.getTxMaker().makeTx();
			try {
				final BTreeMap<String, User> userMap = storage.getUserMap(db);
				final User oldUser = userMap.get(username);
				if (oldUser == null) {
					throw new UserNotFound();
				}

				final String hashed = UserStorage.getPasswordService().encryptPassword(password);
				final User newUser = User.builder(oldUser).hashedPassword(hashed).build();
				userMap.put(username, newUser);
				db.commit();
			} finally {
				db.close();
			}
		}

		@Override
		public void deleteUser(String username) throws UserNotFound, TException {
			final UserStorage storage = UsersPlugin.getInstance().getStorage();
			final DB db = storage.getTxMaker().makeTx();
			try {
				final BTreeMap<String, User> userMap = storage.getUserMap(db);
				if (!userMap.containsKey(username)) {
					throw new UserNotFound();
				}
				userMap.remove(username);
				db.commit();
			} finally {
				db.close();
			}
		}
	}

	public UserServlet() throws Exception {
		super(new Users.Processor<Users.Iface>(new UsersIface()), new TJSONProtocol.Factory());
	}
}
