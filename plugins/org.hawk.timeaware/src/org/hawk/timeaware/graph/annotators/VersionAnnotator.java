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
package org.hawk.timeaware.graph.annotators;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.timeaware.graph.TimeAwareMetaModelUpdater;

/**
 * Contains the actual implementations of the version annotation algorithms,
 * unlike {@link VersionAnnotatorSpec} which is only used to describe the
 * annotator for basic CRUD.
 *
 * This class does not handle persistence of {@link VersionAnnotatorSpec}: that
 * is up to the {@link TimeAwareMetaModelUpdater}.
 */
public class VersionAnnotator {

	/**
	 * Name of a node index which keeps track of which model element nodes were
	 * annotated for which label.
	 */
	private static final String LASTANN_IDXNAME = "lastAnnotated";

	private final IModelIndexer indexer;

	public VersionAnnotator(IModelIndexer indexer) {
		this.indexer = indexer;
	}

	/**
	 * Annotates the full existing history of every instance of the type in the
	 * <code>spec</code>: since we no longer have incremental access to changes, we
	 * will simply go through every of the type at each version of the entire model
	 * and compute away. This can be potentially slow: it would be better to add the
	 * annotators before any models are indexed.
	 *
	 * NOTE: this will not create any new versions in the graph - e.g. if element B
	 * changed between two versions of element A and that intermediate version would
	 * cause the first version of element A to be annotated from then on, we would
	 * simply annotate the second version of A rather than create a new intermediate
	 * version of A and annotate it. This would work differently if the annotator
	 * had been registered from the start, in which case we could use property
	 * access logs to find and annotate this intermediate timepoint.
	 *
	 * TODO: optimize by detecting changes through history.
	 */
	public void annotateFullHistory(VersionAnnotatorSpec spec) throws Exception {
		final IGraphDatabase graph = indexer.getGraph();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(graph);
			final MetamodelNode mmNode = gw.getMetamodelNodeByNsURI(spec.getMetamodelURI());
			final TypeNode tNode = mmNode.getTypeNode(spec.getTypeName());
			final ITimeAwareGraphNode taTNode = (ITimeAwareGraphNode) tNode.getNode();

			final IGraphNodeIndex lastAnnotated = indexer.getGraph().getOrCreateNodeIndex(LASTANN_IDXNAME);

			for (ITimeAwareGraphNode typeVersion : taTNode.getAllVersions()) {
				final TypeNode tnVersion = new TypeNode(typeVersion);
				for (ModelElementNode instance : tnVersion.getAll()) {
					
					
					if (instance.getNode().getPropertyKeys().contains( ) .getSlotValue(slot))
				}
			}
			

			tx.success();
		}
	}

}
