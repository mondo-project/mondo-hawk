package org.hawk.uml.vcs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsChangeType;
import org.hawk.core.VcsCommit;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes all the libraries currently registered with UML, so they may be indexed by Hawk.
 */
public class UMLLibraries implements IVcsManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(UMLLibraries.class);
	private static final String FIRST_REV = "0";

	private ResourceSet rs;
	private boolean isFrozen;

	public UMLLibraries() throws IOException {
		rs = new ResourceSetImpl();
		for (String uri : Arrays.asList(
			UMLResource.ECORE_PRIMITIVE_TYPES_LIBRARY_URI,
			UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI,
			UMLResource.JAVA_PRIMITIVE_TYPES_LIBRARY_URI,
			UMLResource.XML_PRIMITIVE_TYPES_LIBRARY_URI
		)) {
			rs.createResource(URI.createURI(uri)).load(null);
		}
	}

	@Override
	public String getCurrentRevision() throws Exception {
		final URI uri = URI.createURI(UMLResource.ECORE_PRIMITIVE_TYPES_LIBRARY_URI);
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
				if (!r.getURI().toString().startsWith(UMLResource.LIBRARIES_PATHMAP)) {
					// Skip over the profiles for now
					continue;
				}

				VcsCommit commit = new VcsCommit();
				commit.setAuthor("UML library driver - no authors recorded");
				commit.setDelta(delta);
				commit.setJavaDate(null);
				commit.setMessage("UML library driver - no messages recorded");
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
		final URI uri = URI.createURI(UMLResource.LIBRARIES_PATHMAP + path.substring(1));
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
	public void init(String vcsloc, IModelIndexer hawk) throws Exception {
		// TODO move constructor code here
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
		return UMLResource.LIBRARIES_PATHMAP;
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
	public String getHumanReadableName() {
		return "UML Predefined Libraries";
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
	public Set<String> getPrefixesToBeStripped() {
		return Collections.emptySet();
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
