package org.hawk.epsilon.emc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	public IQueryEngine getContainerModel() {
		return model;
	}

	@Override
	public String getTypeName() {
		return typeNode.getTypeName();
	}

	public String getName() {
		return getTypeName();
	}

	public Iterable<GraphNodeWrapper> getAll() {
		Iterable<ModelElementNode> unwrapped = typeNode.getAll();
		return new Iterable<GraphNodeWrapper>() {

			@Override
			public Iterator<GraphNodeWrapper> iterator() {
				final Iterator<ModelElementNode> itUnwrapped = unwrapped.iterator();
				return new Iterator<GraphNodeWrapper>() {

					@Override
					public boolean hasNext() {
						return itUnwrapped.hasNext();
					}

					@Override
					public GraphNodeWrapper next() {
						return new GraphNodeWrapper(itUnwrapped.next().getId(), model);
					}

					@Override
					public void remove() {
						itUnwrapped.remove();
					}
				};
			};
		};
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

	@Override
	public String toString() {
		return String.format("TNW|id:%s|type:%s", getId(), getTypeName());
	}
}