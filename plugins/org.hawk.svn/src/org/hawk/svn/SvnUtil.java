/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Ossmeter team (https://opensourceprojects.eu/p/ossmeter) - SVN delta computation algorithm
 ******************************************************************************/
package org.hawk.svn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnUtil {

	static {
		SvnUtil.setupLibrary();
	}

	public static SVNRepository connectToSVNInstance(String url) {
		SVNRepository repository = null;
		try {
			repository = SvnUtil.createRepository(url);
		} catch (SVNException svne) {
			System.err
					.println("error while creating an SVNRepository for location '"
							+ url + "': " + svne.getMessage());
		}

		try {
			SvnUtil.verifySVNLocation(repository, url);
		} catch (SVNException e) {
			e.printStackTrace();
		}

		return repository;
	}

	public static SVNRepository connectToSVNInstance(String url, String usr,
			String pass) {
		SVNRepository repository = null;
		try {
			repository = SvnUtil.createRepository(url);
		} catch (SVNException svne) {
			System.err
					.println("error while creating an SVNRepository for location '"
							+ url + "': " + svne.getMessage());
		}

		ISVNAuthenticationManager authManager = SVNWCUtil
				.createDefaultAuthenticationManager(usr, pass);
		repository.setAuthenticationManager(authManager);

		try {
			SvnUtil.verifySVNLocation(repository, url);
		} catch (SVNException e) {
			e.printStackTrace();
		}

		return repository;
	}

	public static void authenticate(String usr, String pass, SVNRepository repo) {
		ISVNAuthenticationManager authManager = SVNWCUtil
				.createDefaultAuthenticationManager(usr, pass);
		repo.setAuthenticationManager(authManager);
	}

	public static void setupLibrary() {
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
		FSRepositoryFactory.setup();
	}

	public static SVNRepository createRepository(String url)
			throws SVNException {
		return SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
	}

	public static void verifySVNLocation(SVNRepository repository, String url)
			throws SVNException {
		SVNNodeKind nodeKind = repository.checkPath("", -1);
		if (nodeKind == SVNNodeKind.NONE) {
			System.err.println("There is no entry at '" + url + "'.");
		} else if (nodeKind == SVNNodeKind.FILE) {
			System.err.println("The entry at '" + url
					+ "' is a file while a directory was expected.");
		}
	}

	public static void listEntries(SVNRepository repository, String path)
			throws SVNException {
		Collection<?> entries = repository.getDir(path, -1, null,
				(Collection<?>) null);
		Iterator<?> iterator = entries.iterator();
		while (iterator.hasNext()) {
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			System.out.println("/" + (path.equals("") ? "" : path + "/")
					+ entry.getName() + " (author: '" + entry.getAuthor()
					+ "'; revision: " + entry.getRevision() + "; date: "
					+ entry.getDate() + ")");
			if (entry.getKind() == SVNNodeKind.DIR) {
				listEntries(repository, (path.equals("")) ? entry.getName()
						: path + "/" + entry.getName());
			}
		}
	}

	public static List<SVNDirEntry> getEntries(SVNRepository repository,
			String path) throws SVNException {
		Collection<?> entries = repository.getDir(path, -1, null,
				(Collection<?>) null);
		List<SVNDirEntry> entryURLs = new ArrayList<SVNDirEntry>();
		Iterator<?> iterator = entries.iterator();

		while (iterator.hasNext()) {
			SVNDirEntry entry = (SVNDirEntry) iterator.next();
			entryURLs.add(entry);
			if (entry.getKind() == SVNNodeKind.DIR) {
				entryURLs.addAll(getEntries(repository,
						(path.equals("")) ? entry.getName() : path + "/"
								+ entry.getName()));
			}
		}

		return entryURLs;
	}
}
