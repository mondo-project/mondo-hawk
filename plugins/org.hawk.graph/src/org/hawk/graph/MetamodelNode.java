package org.hawk.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;

public class MetamodelNode {

	private IGraphNode node;

	public MetamodelNode(IGraphNode n) {
		this.node = n;
	}

	public Iterable<TypeNode> getTypes() {
		final Iterable<IGraphEdge> iterableEdges = node.getIncomingWithType("epackage");
		return new Iterable<TypeNode>() {
			@Override
			public Iterator<TypeNode> iterator() {
				final Iterator<IGraphEdge> itEdges = iterableEdges.iterator();
				return new Iterator<TypeNode>() {

					@Override
					public boolean hasNext() {
						return itEdges.hasNext();
					}

					@Override
					public TypeNode next() {
						return new TypeNode(itEdges.next().getStartNode());
					}

					@Override
					public void remove() {
						itEdges.remove();
					}};
			}
		};
	}

	public IGraphNode getNode() {
		return node;
	}

	public String getUri() {
		return (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
	}

	public String getType() {
		return (String)node.getProperty(IModelIndexer.METAMODEL_TYPE_PROPERTY);
	}

	public String getResource() {
		return (String)node.getProperty(IModelIndexer.METAMODEL_RESOURCE_PROPERTY);
	}

	public List<MetamodelNode> getDependencies() {
		final List<MetamodelNode> nodes = new ArrayList<>();
		for (IGraphEdge e : node.getOutgoingWithType(IModelIndexer.METAMODEL_DEPENDENCY_EDGE)) {
			final IGraphNode depNode = e.getEndNode();
			nodes.add(new MetamodelNode(depNode));
		}
		return nodes;
	}
}
