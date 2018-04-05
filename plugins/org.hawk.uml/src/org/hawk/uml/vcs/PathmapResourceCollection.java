/*******************************************************************************
 * Copyright (c) 2017 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.uml.vcs;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes a set of predefined UML resources as models, so Hawk may index them normally.
 */
public abstract class PathmapResourceCollection implements IVcsManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PathmapResourceCollection.class);

	protected ResourceSet rs = new ResourceSetImpl();

	private String baseURI;
	private static final String FIRST_REV = "0";
	private boolean isFrozen;

	public PathmapResourceCollection(String baseURI) {
		this.baseURI = baseURI;
		UMLUtil.init(rs);
	}

	protected String getRootNsURI(final String referenceResourceURI) {
		final URI uri = URI.createURI(referenceResourceURI);
		final EList<EObject> eob = rs.getResource(uri, false).getContents();
		final String revision = eob.get(0).eClass().getEPackage().getNsURI();
		return revision;
	}

	/**
	 * Returns the resource set backing this virtual repository.
	 */
	public ResourceSet getResourceSet() {
		return rs;
	}

	@Override
	public String getFirstRevision() throws Exception {
		return FIRST_REV;
	}

	@Override
	public Collection<VcsCommitItem> getDelta(String endRevision) throws Exception {
		return getDelta(FIRST_REV, endRevision).getCompactedCommitItems();
	}

	@Override
	public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {
		VcsRepositoryDelta delta = new VcsRepositoryDelta();
		delta.setManager(this);

		final String currentRevision = getCurrentRevision();
		if (!currentRevision.equals(startRevision)) {
			for (Resource r : rs.getResources()) {
				if (!r.getURI().toString().startsWith(baseURI)) {
					// Skip over the profiles for now
					continue;
				}

				VcsCommit commit = new VcsCommit();
				commit.setAuthor(getHumanReadableName() + " - no authors recorded");
				commit.setDelta(delta);
				commit.setJavaDate(null);
				commit.setMessage(getHumanReadableName() + " - no messages recorded");
				commit.setRevision(currentRevision);
				delta.getCommits().add(commit);

				VcsCommitItem c = new VcsCommitItem();
				c.setChangeType(VcsChangeType.UPDATED);
				c.setCommit(commit);
				c.setPath(r.getURI().path());

				commit.getItems().add(c);
			}
		}
		delta.setLatestRevision(currentRevision);

		return delta;
	}

	@Override
	public File importFiles(String path, File optionalTemp) {
		final URI uri = URI.createURI(baseURI + path.substring(1));
		try {
			final InputStream is = rs.getURIConverter().createInputStream(uri);
			if (is == null) {
				throw new IllegalArgumentException("Could not find " + path + " in the UML libraries");
			}
			Files.copy(is, optionalTemp.toPath());
			return optionalTemp;
		} catch (Exception ex) {
			LOGGER.error("Error while importing predefined UML library " + path, ex);
			return null;
		}
	}

	@Override
	public boolean isActive() {
		return !rs.getResources().isEmpty();
	}

	@Override
	public void run() throws Exception {
		// nothing to do
	}

	@Override
	public void shutdown() {
		for (Resource resource : rs.getResources()) {
			resource.unload();
		}
		rs = null;
	}

	@Override
	public String getLocation() {
		return baseURI;
	}

	@Override
	public String getUsername() {
		// nothing meaningful here
		return null;
	}

	@Override
	public String getPassword() {
		// nothing meaningful here
		return null;
	}

	@Override
	public void setCredentials(String username, String password, ICredentialsStore credStore) {
		// nothing meaningful here
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public boolean isAuthSupported() {
		return false;
	}

	@Override
	public boolean isPathLocationAccepted() {
		return true;
	}

	@Override
	public boolean isURLLocationAccepted() {
		return true;
	}

	@Override
	public boolean isFrozen() {
		return isFrozen;
	}

	@Override
	public void setFrozen(boolean f) {
		isFrozen = f;
	}

}