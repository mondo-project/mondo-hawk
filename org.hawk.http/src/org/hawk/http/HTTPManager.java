/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.ICredentialsStore.Credentials;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;

@SuppressWarnings("restriction")
public class HTTPManager implements IVcsManager {

	private static final String HEADER_LAST_MODIFIED = "Last-Modified";
	private static final String HEADER_NONE_MATCH = "If-None-Match";
	private static final String HEADER_ETAG = "ETag";
	private static final String FIRST_REV = "0";

	private boolean isActive;
	private IConsole console;
	private URI repositoryURL;

	private String username;
	private String password;
	private String lastETag;

	private String lastDelta;
	private IModelIndexer indexer;

	@Override
	public String getCurrentRevision() throws Exception {
		try (final CloseableHttpClient cl = createClient()) {
			/*
			 * Since HTTP servers can be quite varied, we try several methods in sequence:
			 *
			 * - ETag headers are preferred, since these explicitly check for changes.
			 * - Otherwise, Last-Modified dates are used.
			 * - Otherwise, Content-Length is used.
			 */

			final HttpHead request = new HttpHead(repositoryURL);
			if (lastETag != null) {
				request.setHeader(HEADER_ETAG, lastETag);
				request.setHeader(HEADER_NONE_MATCH, "*");
			}

			try (CloseableHttpResponse response = cl.execute(request)) {
				if (response.getStatusLine().getStatusCode() == 304) {
					// Not-Modified, as told by the server
					return lastETag;
				} else if (response.getStatusLine().getStatusCode() != 200) {
					// Request failed for some reason (4xx, 5xx: we already
					// handle 3xx redirects)
					return FIRST_REV;
				}

				final Header etagHeader = response.getFirstHeader(HEADER_ETAG);
				if (etagHeader != null) {
					lastETag = etagHeader.getValue();
					return lastETag;
				}

				final Header lmHeader = response.getFirstHeader(HEADER_LAST_MODIFIED);
				if (lmHeader != null) {
					final Date lmDate = DateUtils.parseDate(lmHeader.getValue());
					return lmDate.getTime() + "";
				}

				final long cLength = response.getEntity().getContentLength();
				if (cLength >= 0) {
					return cLength + "";
				}
			}
		}

		// No way to detect changes - just fetch the file once
		return "1";
	}

	private CloseableHttpClient createClient() {
		final HttpClientBuilder builder = HttpClientBuilder.create();

		// Provide username and password if specified
		if (username != null) {
			final BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
			credProvider.setCredentials(new AuthScope(new HttpHost(repositoryURL.getHost())), new UsernamePasswordCredentials(username, password));
			builder.setDefaultCredentialsProvider(credProvider);
		}

		// Follow redirects
		builder.setRedirectStrategy(new LaxRedirectStrategy());

		return builder.build();
	}

	@Override
	public String getFirstRevision() throws Exception {
		return FIRST_REV;
	}

	@Override
	public List<VcsCommitItem> getDelta(String endRevision) throws Exception {
		return getDelta(FIRST_REV, endRevision).getCompactedCommitItems();
	}

	@Override
	public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {
		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setManager(this);
		if (lastDelta == null || !lastDelta.equals(endRevision)) {
			VcsCommit c = new VcsCommit();
			c.setAuthor("Unknown");
			c.setJavaDate(new Date());
			c.setMessage("HTTP file changed: " + repositoryURL);
			c.setRevision(endRevision);
			c.setDelta(delta);
			delta.getCommits().add(c);

			VcsCommitItem ci = new VcsCommitItem();
			ci.setChangeType(VcsChangeType.UPDATED);
			ci.setPath(repositoryURL.getPath());
			ci.setCommit(c);
			c.getItems().add(ci);
		}
		delta.setLatestRevision(getCurrentRevision());

		return delta;
	}

	@Override
	public void importFiles(String path, File temp) {
		try (CloseableHttpClient cl = createClient()) {
			try (CloseableHttpResponse response = cl.execute(new HttpGet(repositoryURL))) {
				Files.copy(response.getEntity().getContent(), temp.toPath());
			}
		} catch (IOException e) {
			console.printerrln(e);
		}
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void init(String vcsloc, IModelIndexer indexer) throws URISyntaxException {
		console = indexer.getConsole();
		this.repositoryURL = new URI(vcsloc);
		this.indexer = indexer;
	}

	@Override
	public void run() throws Exception {
		try {
			final ICredentialsStore credStore = indexer.getCredentialsStore();
			if (username != null) {
				// The credentials were provided by a previous setCredentials
				// call: retry the change to the credentials store.
				setCredentials(username, password, credStore);
			} else {
				final Credentials credentials = credStore.get(repositoryURL.toString());
				if (credentials != null) {
					this.username = credentials.getUsername();
					this.password = credentials.getPassword();
				} else {
					/*
					 * If we use null for the default username/password, SVNKit
					 * will try to use the GNOME keyring in Linux, and that will
					 * lock up our Eclipse instance in some cases.
					 */
					console.printerrln("No username/password recorded for the repository " + repositoryURL);
					this.username = "";
					this.password = "";
				}
			}

			isActive = true;
		} catch (Exception e) {
			console.printerrln("exception in svnmanager run():");
			console.printerrln(e);
		}
	}

	@Override
	public void shutdown() {
		repositoryURL = null;
		console = null;
	}

	@Override
	public String getLocation() {
		return repositoryURL.toString();
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public void setCredentials(String username, String password,
			ICredentialsStore credStore) {
		if (username != null && password != null && repositoryURL != null
				&& (!username.equals(this.username)
				|| !password.equals(this.password))) {
			try {
				credStore.put(repositoryURL.toString(),
						new Credentials(username, password));
			} catch (Exception e) {
				console.printerrln("Could not save new username/password");
				console.printerrln(e);
			}
		}
		this.username = username;
		this.password = password;
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public String getHumanReadableName() {
		return "HTTP Monitor";
	}

	@Override
	public boolean isAuthSupported() {
		return true;
	}

	@Override
	public boolean isPathLocationAccepted() {
		return false;
	}

	@Override
	public boolean isURLLocationAccepted() {
		return true;
	}

	@Override
	public Set<String> getPrefixesToBeStripped() {
		return Collections.emptySet();
	}

}
