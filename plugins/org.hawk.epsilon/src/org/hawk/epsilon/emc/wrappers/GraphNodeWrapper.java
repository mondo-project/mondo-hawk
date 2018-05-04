/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 *     Antonio Garcia-Dominguez - add isContainedWithin (for EMF-Splitter integration)
 ******************************************************************************/
package org.hawk.epsilon.emc.wrappers;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.ModelElementNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphNodeWrapper implements IGraphNodeReference {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphNodeWrapper.class);

	protected String id;
	protected EOLQueryEngine containerModel;
	protected WeakReference<IGraphNode> node;

	public GraphNodeWrapper(IGraphNode n, EOLQueryEngine containerModel) {

		node = new WeakReference<IGraphNode>(n);
		this.id = n.getId().toString();
		this.containerModel = containerModel;

	}

	@Override
	public IGraphNode getNode() {
		IGraphNode ret = node.get();
		if (ret == null) {
			ret = containerModel.getBackend().getNodeById(id);
			node = new WeakReference<IGraphNode>(ret);
		}
		return ret;
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
				&& ((GraphNodeWrapper) o).containerModel.equals(this.containerModel))
			return true;
		return false;
	}

	public Object getFeature(String name) throws EolRuntimeException {
		return containerModel.getPropertyGetter().invoke(this, name);
	}

	/**
	 * Returns true if this model element is contained directly or indirectly
	 * within the specified path at the specified repository.
	 */
	public boolean isContainedWithin(String repository, String path) {
		try (IGraphTransaction t = containerModel.getBackend().beginTransaction()) {
			final ModelElementNode men = new ModelElementNode(getNode());
			return men.isContainedWithin(repository, path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String getTypeName() {
		String type = "";

		try (IGraphTransaction t = containerModel.getBackend().beginTransaction()) {
			IGraphNode n = getNode();

			final Iterator<IGraphEdge> itTypeNode = n.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator();
			if (itTypeNode.hasNext()) {
				final IGraphNode typeNode = itTypeNode.next().getEndNode();
				type = typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).toString();
			} else {
				LOGGER.error("No type node found for node {}", n);
			}

			t.success();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return type;
	}

	@Override
	public String toString() {

		String info = "";

		try (IGraphTransaction t = containerModel.getBackend().beginTransaction()) {

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
			System.err.println("error in toString of GraphNodeWrapper");
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

		return "GNW|id:" + id + "|" + (info.equals("") ? "[no meta-info]" : info) + "";

	}

	@Override
	public int hashCode() {
		return id.hashCode() + containerModel.hashCode();
	}
}
