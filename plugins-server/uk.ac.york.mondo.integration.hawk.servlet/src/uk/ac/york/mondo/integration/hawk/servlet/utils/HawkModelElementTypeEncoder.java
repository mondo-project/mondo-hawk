package uk.ac.york.mondo.integration.hawk.servlet.utils;

import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;

import uk.ac.york.mondo.integration.api.ModelElementType;
import uk.ac.york.mondo.integration.api.SlotMetadata;

public class HawkModelElementTypeEncoder {

	private final GraphWrapper graph;

	public HawkModelElementTypeEncoder(GraphWrapper gw) {
		this.graph = gw;
	}

	public ModelElementType encode(final String id) {
		final TypeNode me = graph.getTypeNodeById(id);
		return encode(me);
	}

	public ModelElementType encode(TypeNode me) {
		final ModelElementType t = new ModelElementType();
		t.setMetamodelUri(me.getMetamodelURI());
		t.setTypeName(me.getTypeName());
		t.setId(me.getNode().getId().toString());

		for (Slot s : me.getSlots()) {
			final SlotMetadata sm = new SlotMetadata();
			sm.setIsMany(s.isMany());
			sm.setIsOrdered(s.isOrdered());
			sm.setIsUnique(s.isUnique());
			sm.setName(s.getName());
			sm.setType(s.getType());

			if (s.isAttribute()) {
				t.addToAttributes(sm);
			} else {
				t.addToReferences(sm);
			}
		}

		return t;
	}

	public Object encode(IGraphNode n) {
		return encode(new TypeNode(n));
	}
}
