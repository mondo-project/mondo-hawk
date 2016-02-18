/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphTypeNodeReference;
import org.hawk.core.query.IQueryEngine;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;

public class TypeNodeWrapper implements IGraphTypeNodeReference {

	private final TypeNode typeNode;
	private final EOLQueryEngine model;

	public TypeNodeWrapper(TypeNode node, EOLQueryEngine model) {
		this.typeNode = node;
		this.model = model;
	}

	@Override
	public String getId() {
		return typeNode.getNode().getId().toString();
	}

	@Override
	public IGraphNode getNode(IGraphDatabase g) {
		return typeNode.getNode();
	}

	@Override
	public IQueryEngine getContainerModel() {
		return model;
	}

	@Override
	public String getTypeName() {
		return "_hawkTypeNode";
	}

	public String getName() {
		return typeNode.getTypeName();
	}

	public Collection<Object> getAll() {
		final Collection<Object> allOf = model.getAllOf(typeNode.getNode(), ModelElementNode.EDGE_LABEL_OFKIND);
		allOf.addAll(model.getAllOf(typeNode.getNode(), ModelElementNode.EDGE_LABEL_OFTYPE));
		return allOf;
	}

	public List<Slot> getAttributes() {
		List<Slot> attributes = new ArrayList<>();
		for (Slot s : typeNode.getSlots()) {
			if (s.isAttribute()) {
				attributes.add(s);
			}
		}
		return attributes;
	}

	public List<Slot> getReferences() {
		List<Slot> attributes = new ArrayList<>();
		for (Slot s : typeNode.getSlots()) {
			if (s.isReference()) {
				attributes.add(s);
			}
		}
		return attributes;
	}

	public List<Slot> getFeatures() {
		return typeNode.getSlots();
	}

	public MetamodelNodeWrapper getMetamodel() {
		return new MetamodelNodeWrapper(typeNode.getMetamodel(), model);
	}

	@Override
	public String toString() {
		return String.format("TNW|id:%s|type:%s", getId(), getTypeName());
	}
}