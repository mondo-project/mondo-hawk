/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.graph.updater.proxies;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphNode;

/**
 * List of proxy references to nodes in a certain file, from a node in the graph.
 *
 * TODO: change proxy reference creation to use these classes as well.
 */
public class ProxyReferenceList {

	public class ProxyReference {
		private final ProxyReferenceTarget target;
		private final String edgeLabel;
		private final boolean isContainment, isContainer;

		public ProxyReference(final ProxyReferenceTarget target, final String edgeLabel, final boolean isContainment, final boolean isContainer) {
			this.target = target;
			this.edgeLabel = edgeLabel;
			this.isContainment = isContainment;
			this.isContainer = isContainer;
		}

		public ProxyReferenceTarget getTarget() {
			return target;
		}

		public String getEdgeLabel() {
			return edgeLabel;
		}

		public boolean isContainment() {
			return isContainment;
		}

		public boolean isContainer() {
			return isContainer;
		}

		public ProxyReferenceList getList() {
			return ProxyReferenceList.this;
		}

		@Override
		public String toString() {
			return "ProxyReference [target=" + target + ", edgeLabel=" + edgeLabel + ", isContainment=" + isContainment
					+ ", isContainer=" + isContainer + "]";
		}
	}

	private final Object sourceNodeID;
	private final ProxyReferenceTarget targetFile;
	private final List<ProxyReference> references = new ArrayList<>();

	public ProxyReferenceList(IGraphNode sourceNode, String[] rawValue) {
		this.sourceNodeID = sourceNode.getId();
		targetFile = new ProxyReferenceTarget(rawValue[0], true);

		// Split subelements
		for (int i = 0; i < rawValue.length; i += 4) {
			final ProxyReferenceTarget target = new ProxyReferenceTarget(rawValue[i], false);
			final String edgeLabel = rawValue[i + 1];
			final boolean isContainment = Boolean.valueOf(rawValue[i + 2]);
			final boolean isContainer = Boolean.valueOf(rawValue[i + 3]);

			references.add(new ProxyReference(target, edgeLabel, isContainment, isContainer));
		}
	}

	public String getFullPathURI() {
		return targetFile.getFileURI();
	}

	public String getRepositoryURL() {
		return targetFile.getRepositoryURL();
	}

	public String getFilePath() {
		return targetFile.getFilePath();
	}

	public ProxyReferenceTarget getTargetFile() {
		return targetFile;
	}

	public List<ProxyReference> getReferences() {
		return references;
	}

	public Object getSourceNodeID() {
		return sourceNodeID;
	}

	public String[] toArray() {
		String[] arr = new String[references.size() * 4];

		int i = 0;
		for (ProxyReference ref : references) {
			arr[i] = ref.getTarget().getElementURI();
			arr[i + 1] = ref.getEdgeLabel();
			arr[i + 2] = ref.isContainment() + "";
			arr[i + 3] = ref.isContainer() + "";

			i += 4;
		}

		return arr;
	}

	@Override
	public String toString() {
		return "ProxyReferenceList [sourceNodeID=" + sourceNodeID + ", targetFile=" + targetFile + ", references="
				+ references + "]";
	}
}