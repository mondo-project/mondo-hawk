/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York.
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
package org.hawk.graph.updater;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache that can be shared across multiple {@link GraphModelBatchInjector}
 * classes to avoid retrieving the same type node again and again. Designed to
 * be simply GC'ed during the {@link GraphModelUpdater#updateProxies()} stage.
 */
public class TypeCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(TypeCache.class);

	private Map<IHawkClass, IGraphNode> hashedEClasses = new HashMap<>();
	private Map<IGraphNode, Map<String, Slot>> hashedEClassSlots = new HashMap<>();

	public IGraphNode getEClassNode(IGraphDatabase graph, IHawkClassifier e) throws Exception {
		IHawkClass eClass = null;

		if (e instanceof IHawkClass)
			eClass = ((IHawkClass) e);
		else
			System.err.println("getEClassNode called on a non-class classifier:\n" + e);

		IGraphNode classnode = hashedEClasses.get(eClass);

		if (classnode == null) {
			final String packageNSURI = eClass.getPackageNSURI();
			IGraphNode ePackageNode = null;
			try {
				ePackageNode = graph.getMetamodelIndex().get("id", packageNSURI).getSingle();
			} catch (NoSuchElementException ex) {
				throw new Exception(String.format(
						"Metamodel %s does not have a Node associated with it in the store, please make sure it has been inserted",
						packageNSURI));
			} catch (Exception e2) {
				LOGGER.error("Error while finding metamodel node", e2);
			}

			for (IGraphEdge r : ePackageNode.getEdges()) {
				final IGraphNode otherNode = r.getStartNode();
				if (otherNode.equals(ePackageNode)) {
					continue;
				}

				final Object id = otherNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
				if (id.equals(eClass.getName())) {
					classnode = otherNode;
					break;
				}
			}

			if (classnode != null) {
				hashedEClasses.put(eClass, classnode);
			} else {
				throw new Exception(String.format(
						"eClass: %s (%s) does not have a Node associated with it in the store, please make sure the metamodel %s has been inserted",
						eClass.getName(), eClass.getUri(), packageNSURI));
			}

			hashedEClassSlots.put(classnode, new TypeNode(classnode).getSlots());
		}

		return classnode;
	}

	public Map<String, Slot> getEClassNodeSlots(IGraphDatabase graph, IHawkClassifier e) throws Exception {
		return hashedEClassSlots.get(getEClassNode(graph, e));
	}
}
