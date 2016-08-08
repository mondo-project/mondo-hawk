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
package uk.ac.york.mondo.integration.server.users.servlet.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SaltedAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.mapdb.DB;

import uk.ac.york.mondo.integration.server.users.servlet.UsersPlugin;
import uk.ac.york.mondo.integration.server.users.servlet.db.User;
import uk.ac.york.mondo.integration.server.users.servlet.db.UserStorage;

/**
 * Apache Shiro security realm based on our internal storage.
 */
public class UsersRealm extends AuthenticatingRealm {

	private static final class UserInfo implements SaltedAuthenticationInfo {
		private final Hash hash;
		private final String username;
		private static final long serialVersionUID = 1L;

		public UserInfo(String username, Hash hash) {
			this.username = username;
			this.hash = hash;
		}

		@Override
		public PrincipalCollection getPrincipals() {
			return new SimplePrincipalCollection(username, UsersRealm.class.getCanonicalName());
		}

		@Override
		public Object getCredentials() {
			return hash;
		}

		@Override
		public ByteSource getCredentialsSalt() {
			return hash.getSalt();
		}
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (token instanceof UsernamePasswordToken) {
			final UsernamePasswordToken upToken = (UsernamePasswordToken)token;
			final String username = upToken.getUsername();

			final UserStorage storage = UsersPlugin.getInstance().getStorage();
			final DB db = storage.getTxMaker().makeTx();
			try {
				final User user = storage.getUserMap(db).get(username);
				if (user == null) {
					return null;
				}
				Hash hash = new Shiro1CryptFormat().parse(user.getHashedPassword());
				return new UserInfo(user.getUsername(), hash);
			} finally {
				db.close();
			}
		}
		return null;
	}

}
