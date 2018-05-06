/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.servlet.utils;

import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.hawk.service.api.ModelElementType;
import org.hawk.service.api.SlotMetadata;

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

		for (Slot s : me.getSlots().values()) {
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
