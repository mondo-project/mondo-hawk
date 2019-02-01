/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.graph.introspector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.hawk.core.IMetaModelIntrospector;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMetaModelIntrospector implements IMetaModelIntrospector {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphMetaModelIntrospector.class);

	public static class Factory implements IMetaModelIntrospector.Factory {
		@Override
		public boolean canIntrospect(IModelIndexer idx) {
			if (idx.getGraph() == null) {
				// Cannot introspect remote indexers
				return false;
			}

			// Must have used the metamodel updater in this plugin
			return idx.getMetaModelUpdater() instanceof GraphMetaModelUpdater;
		}

		@Override
		public IMetaModelIntrospector createFor(IModelIndexer idx) {
			return new GraphMetaModelIntrospector(idx);
		}

		@Override
		public String getHumanReadableName() {
			return "Local metamodel introspector factory";
		}
	}

	private final IModelIndexer indexer;

	protected GraphMetaModelIntrospector(IModelIndexer idx) {
		this.indexer = idx;
	}

	@Override
	public List<String> getMetamodels() {
		final List<String> plugins = new ArrayList<>(indexer.getKnownMMUris());
		Collections.sort(plugins);
		return plugins;
	}

	@Override
	public List<String> getTypes(String metamodelURI) throws NoSuchElementException {
		final IGraphDatabase db = indexer.getGraph();
		final List<String> typeNames = new ArrayList<>();

		try (IGraphTransaction tx = db.beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(db);
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(metamodelURI);
			for (TypeNode tn : mmNode.getTypes()) {
				typeNames.add(tn.getTypeName());
			}
			Collections.sort(typeNames);
			tx.success();
			return typeNames;
		} catch (Exception e1) {
			LOGGER.error(e1.getMessage(), e1);
		}

		return Collections.emptyList();
	}

	@Override
	public List<String> getAttributes(String metamodelURI, String typeName) throws NoSuchElementException {
		final IGraphDatabase db = indexer.getGraph();

		try (IGraphTransaction tx = db.beginTransaction()) {
			final List<String> attributeSlots = new ArrayList<>();

			final GraphWrapper gw = new GraphWrapper(db);
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(metamodelURI);
			if (mmNode == null) {
				throw new NoSuchElementException("Metamodel " + metamodelURI + " not registered at this indexer");
			}

			boolean foundType = false;
			for (TypeNode tn : mmNode.getTypes()) {
				if (typeName.equals(tn.getTypeName())) {
					foundType = true;
					for (Slot slot : tn.getSlots().values()) {
						if (slot.isAttribute()) {
							attributeSlots.add(slot.getName());
						}
					}
				}
			}
			if (!foundType) {
				throw new NoSuchElementException("Type " + typeName + " not found within metamodel " + metamodelURI);
			}

			Collections.sort(attributeSlots);
			tx.success();
			return attributeSlots;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return Collections.emptyList();
	}

}
