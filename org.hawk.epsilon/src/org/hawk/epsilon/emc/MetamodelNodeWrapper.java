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
package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.query.IQueryEngine;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.TypeNode;

public class MetamodelNodeWrapper implements IGraphNodeReference {

	private final MetamodelNode metamodelNode;
	private final EOLQueryEngine model;

	public MetamodelNodeWrapper(MetamodelNode pn, EOLQueryEngine eolQueryEngine) {
		this.metamodelNode = pn;
		this.model = eolQueryEngine;
	}

	@Override
	public String getId() {
		return metamodelNode.getNode().getId().toString();
	}

	@Override
	public IGraphNode getNode() {
		return metamodelNode.getNode();
	}

	@Override
	public String getTypeName() {
		return "_hawkPackageNode";
	}

	@Override
	public IQueryEngine getContainerModel() {
		return model;
	}

	public String getUri() {
		return metamodelNode.getUri();
	}

	public String getMetamodelType() {
		return metamodelNode.getType();
	}

	public String getResource() {
		return metamodelNode.getResource();
	}

	public List<TypeNodeWrapper> getTypes() {
		final List<TypeNodeWrapper> types = new ArrayList<>();
		for (TypeNode tn : metamodelNode.getTypes()) {
			types.add(new TypeNodeWrapper(tn, model));
		}
		return types;
	}

	public List<MetamodelNodeWrapper> getDependencies() {
		final List<MetamodelNodeWrapper> deps = new ArrayList<>();
		for (MetamodelNode mn : metamodelNode.getDependencies()) {
			deps.add(new MetamodelNodeWrapper(mn, model));
		}
		return deps;
	}
}
