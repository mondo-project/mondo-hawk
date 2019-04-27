/*******************************************************************************
 * Copyright (c) 2015-2018 The University of York, Aston University.
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
package org.hawk.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only abstraction of the known metadata about a slot (an attribute or a
 * reference) in the graph populated by this updater.
 */
public class Slot {

	private static final Logger LOGGER = LoggerFactory.getLogger(Slot.class);

	private final TypeNode typeNode;
	private final String propertyName, propertyType;
	private final boolean isAttribute, isReference, isMixed, isDerived, isIndexed, isVersionAnnotator;
	private final boolean isMany, isOrdered, isUnique;

	// Only valid if this is derived
	private final String derivationLanguage, derivationLogic;

	public Slot(TypeNode typeNode, String propertyName) {
		this.typeNode = typeNode;
		this.propertyName = propertyName;

		final String[] propertyMetadata = (String[]) typeNode.getNode().getProperty(propertyName);
		this.isAttribute = "a".equals(propertyMetadata[0]);
		this.isReference = "r".equals(propertyMetadata[0]);
		this.isMixed = "m".equals(propertyMetadata[0]);
		this.isDerived = "d".equals(propertyMetadata[0]);
		this.isVersionAnnotator = "va".equals(propertyMetadata[0]);

		this.isMany = "t".equals(propertyMetadata[1]);
		this.isOrdered = "t".equals(propertyMetadata[2]);
		this.isUnique = "t".equals(propertyMetadata[3]);
		this.isIndexed = isAttribute && "t".equals(propertyMetadata[5]);
		this.propertyType = propertyMetadata[4];

		if (isDerived || isVersionAnnotator) {
			this.derivationLanguage = propertyMetadata[5];
			this.derivationLogic = propertyMetadata[6];
		} else {
			this.derivationLanguage = null;
			this.derivationLogic = null;
		}
	}

	/**
	 * Returns the collection container that should be used for the value of
	 * this slot.
	 */
	protected Collection<Object> getCollection() {
		if (isOrdered && isUnique) {
			return new LinkedHashSet<Object>(); // ordered set
		} else if (isOrdered) {
			return new ArrayList<Object>(); // sequence
		} else if (isUnique) {
			return new HashSet<Object>(); // set
		} else {
			return new ArrayList<Object>(); // bag
		}
	}

	public TypeNode getTypeNode() {
		return typeNode;
	}

	public String getName() {
		return propertyName;
	}

	public boolean isAttribute() {
		return isAttribute;
	}

	public boolean isReference() {
		return isReference;
	}

	public boolean isMixed() {
		return isMixed;
	}

	public boolean isDerived() {
		return isDerived;
	}

	public boolean isMany() {
		return isMany;
	}

	public boolean isOrdered() {
		return isOrdered;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public boolean isIndexed() {
		return isIndexed;
	}

	public boolean isVersionAnnotator() {
		return isVersionAnnotator;
	}

	public String getType() {
		return propertyType;
	}

	/**
	 * Returns the full name of the node index that tracks this slot in all instances
	 * of this type, if any.
	 */
	public String getNodeIndexName() {
		if (!isDerived && !isIndexed) {
			return null;
		}
		
		String result = null;
		final IGraphDatabase graph = typeNode.getNode().getGraph();
		try (IGraphTransaction ignored = graph.beginTransaction()) {
			final String indexname = String.format("%s##%s##%s", typeNode.getMetamodelURI(), typeNode.getTypeName(), this.propertyName);
			if (graph.nodeIndexExists(indexname)) {
				result = indexname;
			}
			ignored.success();
		} catch (Exception e) {
			LOGGER.warn("Error while locating the index for this derived property", e);
		}

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result
				+ ((typeNode == null) ? 0 : typeNode.hashCode());
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
		Slot other = (Slot) obj;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		if (typeNode == null) {
			if (other.typeNode != null)
				return false;
		} else if (!typeNode.equals(other.typeNode))
			return false;
		return true;
	}

	public String getDerivationLanguage() {
		return derivationLanguage;
	}

	public String getDerivationLogic() {
		return derivationLogic;
	}

	@Override
	public String toString() {
		return "Slot [typeNode=" + typeNode + ", propertyName=" + propertyName + ", propertyType=" + propertyType
				+ ", isAttribute=" + isAttribute + ", isReference=" + isReference + ", isMixed=" + isMixed
				+ ", isDerived=" + isDerived + ", isIndexed=" + isIndexed + ", isMany=" + isMany + ", isOrdered="
				+ isOrdered + ", isUnique=" + isUnique + ", derivationLanguage=" + derivationLanguage
				+ ", derivationLogic=" + derivationLogic + "]";
	}
}