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
package uk.ac.york.mondo.integration.server.users.servlet.db;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Java POJO with the information we want to store about each user.
 * Must be serializable and immutable.
 */
public class User implements Serializable {

	private static final long serialVersionUID = -8897748864090665228L;

	private String username;
	private String hashedPassword;
	private Calendar expirationDate;
	private boolean isAdmin;
	private String realName;

	/**
	 * Default constructor.
	 */
	public User(){}

	/**
	 * Copy constructor.
	 */
	public User(User copyFrom) {
		this.username = copyFrom.username;
		this.hashedPassword = copyFrom.hashedPassword;
		this.expirationDate = copyFrom.expirationDate;
		this.isAdmin = copyFrom.isAdmin;
		this.realName = copyFrom.realName;
	}

	/**
	 * Fluent builder used to create new Users from scratch or from existing
	 * instances. We use this style as MapDB requires keys and values to be
	 * immutable, disallowing us from using simple setter methods (their changes
	 * would not be noticed by MapDB and would not be part of the commits).
	 */
	public static class Builder {
		private User user;

		private Builder(User u) {
			this.user = u;
		}

		public User build() {
			User ret = user;
			user = null;
			return ret;
		}

		public Builder username(String s) {
			user.username = s;
			return this;
		}

		public Builder hashedPassword(String s) {
			user.hashedPassword = s;
			return this;
		}

		public Builder expirationDate(Calendar c) {
			user.expirationDate = c;
			return this;
		}

		public Builder isAdmin(boolean b) {
			user.isAdmin = b;
			return this;
		}

		public Builder realName(String s) {
			user.realName = s;
			return this;
		}
	}

	public static Builder builder(User copyFrom) {
		return new Builder(new User(copyFrom));
	}

	public static Builder builder() {
		return new Builder(new User());
	}

	public String getUsername() {
		return username;
	}

	public String getHashedPassword() {
		return hashedPassword;
	}

	public Calendar getExpirationDate() {
		return expirationDate;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public String getRealName() {
		return realName;
	}
}
