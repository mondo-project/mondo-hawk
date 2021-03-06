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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.query.IQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
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
