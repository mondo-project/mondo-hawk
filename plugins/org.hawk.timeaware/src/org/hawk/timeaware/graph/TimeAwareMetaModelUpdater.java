/*******************************************************************************
 * Copyright (c) 2019 Aston University.
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
package org.hawk.timeaware.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndexFactory;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.hawk.timeaware.graph.annotators.VersionAnnotator;
import org.hawk.timeaware.graph.annotators.VersionAnnotatorSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended version of the original metamodel updater which adds support for
 * management of version annotators.
 */
public class TimeAwareMetaModelUpdater extends GraphMetaModelUpdater {

	public static final String VA_TYPES_IDXKEY = "label";
	public static final String VA_TYPES_IDXNAME = "vaTypes";
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeAwareMetaModelUpdater.class);

	/**
	 * Adds a new version annotator to the graph. Types with version annotators are
	 * indexed in a {@link IGraphNodeIndex}, for simple retrieval.
	 *
	 * The version annotator information is stored as an additional slot in the type
	 * node.
	 */
	public void addVersionAnnotator(IModelIndexer indexer, VersionAnnotatorSpec definition) {
		if (!(indexer.getGraph() instanceof ITimeAwareGraphNodeVersionIndexFactory)) {
			LOGGER.error("Indexer is not compatible, ignoring");
			return;
		}
		
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(definition.getMetamodelURI());
			final TypeNode tNode = mmNode.getTypeNode(definition.getTypeName());

			if (tNode.getSlot(definition.getVersionLabel()) != null) {
				throw new IllegalArgumentException("Cannot create an annotator with the same name as an existing slot");
			}

			tNode.getNode().setProperty(definition.getVersionLabel(), new String[] {
				"va", // version annotator
				"f",  // !isMany
				"f",  // !isOrdered
				"f",  // !isUnique
				"f",  // !isIndexed (as in, indexed attribute)
				definition.getExpressionLanguage(),
				definition.getExpression()
			});

			indexer.getGraph().getOrCreateNodeIndex(VA_TYPES_IDXNAME)
				.add(tNode.getNode(), VA_TYPES_IDXKEY, definition.getVersionLabel());

			new VersionAnnotator(indexer).annotateFullHistory(definition); 

			tx.success();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Removes a version annotator from the graph.
	 */
	public void removeVersionAnnotator(IModelIndexer indexer, String metamodel, String typeName, String label) {
		try (IGraphTransaction tx = indexer.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(metamodel);
			final TypeNode tNode = mmNode.getTypeNode(typeName);

			final Slot slot = tNode.getSlot(label);
			if (slot == null) {
				throw new NoSuchElementException(
					String.format("Could not find version annotator %s in %s::%s", label, metamodel, typeName)
				);
			} else if (!slot.isVersionAnnotator()) {
				throw new IllegalArgumentException(
					String.format("Slot %s is not a version annotator in %s::%s", label, metamodel, typeName)
				);
			}

			tNode.getNode().setProperty(label, null);
			indexer.getGraph().getOrCreateNodeIndex(VA_TYPES_IDXNAME)
				.remove(tNode.getNode(), VA_TYPES_IDXKEY, label);

			// TODO: drop version index

			// TODO: clean derived property index if type node has no more derived attrs / VAs?

			tx.success();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Lists all available version annotators for a type.
	 */
	public Collection<VersionAnnotatorSpec> listVersionAnnotators(IModelIndexer indexer, String metamodel, String typeName) {
		final Set<VersionAnnotatorSpec> results = new HashSet<>();

		final IGraphDatabase graph = indexer.getGraph();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(indexer.getGraph());
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(metamodel);
			final TypeNode tNode = mmNode.getTypeNode(typeName);

			for (Slot slot : tNode.getSlots().values()) {
				if (slot.isVersionAnnotator()) {
					results.add(VersionAnnotatorSpec.from(tNode, slot));
				}
			}

			tx.success();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return results;
	}

	/**
	 * Lists all available version annotators.
	 */
	public Collection<VersionAnnotatorSpec> listVersionAnnotators(IModelIndexer indexer) {
		final Set<VersionAnnotatorSpec> results = new HashSet<>();

		final IGraphDatabase graph = indexer.getGraph();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			if  (!graph.nodeIndexExists(VA_TYPES_IDXNAME)) {
				return Collections.emptyList();
			}
			final IGraphNodeIndex nodeIndex = graph.getOrCreateNodeIndex(VA_TYPES_IDXNAME);

			for (IGraphNode node : nodeIndex.query(VA_TYPES_IDXKEY, "*")) {
				final TypeNode typeNode = new TypeNode(node);
				for (Slot slot : typeNode.getSlots().values()) {
					if (slot.isVersionAnnotator()) {
						results.add(VersionAnnotatorSpec.from(typeNode, slot));
					}
				}
			}

			tx.success();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return results;
	}

}
