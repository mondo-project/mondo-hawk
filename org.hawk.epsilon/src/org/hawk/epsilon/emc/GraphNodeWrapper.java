/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.ModelElementNode;

public class GraphNodeWrapper implements IGraphNodeReference {

	// private IGraphDatabase container;
	protected String id;
	protected EOLQueryEngine containerModel;

	public GraphNodeWrapper(String id, EOLQueryEngine containerModel) {

		this.id = id;
		this.containerModel = containerModel;

	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public EOLQueryEngine getContainerModel() {
		return containerModel;
	}

	@Override
	public boolean equals(Object o) {
		// System.out.println(o+" >< "+this);
		if (!(o instanceof GraphNodeWrapper))
			return false;
		if (((GraphNodeWrapper) o).id.equals(this.id)
				&& ((GraphNodeWrapper) o).containerModel
						.equals(this.containerModel))
			return true;
		return false;
	}

	public Object getFeature(String name) throws EolRuntimeException {
		return containerModel.getPropertyGetter().invoke(this, name);
	}

	@Override
	public String getTypeName() {
		String type = "";

		try (IGraphTransaction t = containerModel.getBackend()
				.beginTransaction()) {

			IGraphNode n = containerModel.getBackend().getNodeById(id);

			type = n.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator().next()
					.getEndNode().getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();

			// HawkClass e = new MetamodelUtils().getTypeOfFromNode(n,
			// containerModel.parser);
			//
			// type = (e != null ? e.getName() : new MetamodelUtils()
			// .getClassFromNode(container.getNodeById(id),
			// containerModel.parser).getName())
			// + "";

			t.success();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return type;
	}

	@Override
	public String toString() {

		String info = "";

		try (IGraphTransaction t = containerModel.getBackend()
				.beginTransaction()) {

			// Node n = container.getNodeById(id);

			// HawkClass e = new MetamodelUtils().getTypeOfFromNode(n,
			// containerModel.parser);

			info += "type:" + getTypeName();
			info += "";

			// + (e != null ? e.getName() : new MetamodelUtils()
			// .getClassFromNode(container.getNodeById(id),
			// containerModel.parser).getName()) + "";

			t.success();
		} catch (Exception e) {
			System.err.println("error in tostring of GraphNodeWrapper");
			e.printStackTrace();
		}

		// return "Wrapper with id: "
		// + id
		// + " $ in model: "
		// + (containerModel != null ? containerModel.toString()
		// : "(null container model!)")
		// + " (name: "
		// + (containerModel != null ? containerModel.getName()
		// : "(null container model!)") + ") $ "
		// + (info.equals("") ? "[no meta-info]" : info) + "";

		return "GNW|id:" + id + "|"
				+ (info.equals("") ? "[no meta-info]" : info) + "";

	}

	@Override
	public int hashCode() {
		return id.hashCode() + containerModel.hashCode();
	}
}
