package org.hawk.graph.updater.proxies;

import org.hawk.graph.updater.GraphModelUpdater;

/**
 * Immutable parsed version of a proxy reference target, which has a repository,
 * a path to a file within the repository, and optionally a fragment to a
 * specific element within the file.
 */
public class ProxyReferenceTarget {

	private final String repoURL;
	private final String filePath;
	private final String fragment;

	/**
	 * Creates a target from a <code>repo||||path#fragment</code> string.
	 *
	 * @param uri
	 *            String with the target to be parsed.
	 * @param ignoreFragment
	 *            Iff <code>true</code>, the fragment will always be
	 *            <code>null</code> regardless of the actual URI.
	 */
	public ProxyReferenceTarget(String uri, boolean ignoreFragment) {
		final int hashPos = uri.indexOf("#");
		String uriWithoutFragment;
		if (hashPos == -1) {
			uriWithoutFragment = uri;
			fragment = null;
		} else {
			uriWithoutFragment = uri.substring(0, hashPos);
			fragment = ignoreFragment ? null : uri.substring(hashPos + 1);
		}

		// Split full URI into repo URL + filePath, filePath may be simply * in case of fragment-based refs
		final int repoSeparatorPos = uriWithoutFragment.indexOf(GraphModelUpdater.FILEINDEX_REPO_SEPARATOR);
		repoURL = uriWithoutFragment.substring(0, repoSeparatorPos);
		filePath = uriWithoutFragment.substring(repoSeparatorPos + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR.length());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result + ((fragment == null) ? 0 : fragment.hashCode());
		result = prime * result + ((repoURL == null) ? 0 : repoURL.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProxyReferenceTarget other = (ProxyReferenceTarget) obj;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (fragment == null) {
			if (other.fragment != null)
				return false;
		} else if (!fragment.equals(other.fragment))
			return false;
		if (repoURL == null) {
			if (other.repoURL != null)
				return false;
		} else if (!repoURL.equals(other.repoURL))
			return false;
		return true;
	}

	public String getRepositoryURL() {
		return repoURL;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFragment() {
		return fragment;
	}

	public String getFileURI() {
		return repoURL + GraphModelUpdater.FILEINDEX_REPO_SEPARATOR + filePath;
	}

	public String getElementURI() {
		if (fragment == null) {
			throw new IllegalStateException("Cannot provide element URI if fragment is null");
		}
		return getFileURI() + "#" + fragment;
	}

	public boolean isFragmentBased() {
		return filePath.equals(GraphModelUpdater.PROXY_FILE_WILDCARD);
	}

	@Override
	public String toString() {
		return "ProxyReferenceTarget [repoURL=" + repoURL + ", filePath=" + filePath + ", fragment=" + fragment + "]";
	}
}
