package org.hawk.timeaware.graph;

import java.util.NoSuchElementException;
import java.util.Set;

import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeIndex;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;

/**
 * Version of {@link GraphWrapper} which adds a default timepoint to requests
 * which do not specify one.
 */
public class DefaultTimeGraphWrapper extends GraphWrapper {

	private final long timepoint;

	public DefaultTimeGraphWrapper(IGraphDatabase graph, long timepoint) {
		super(graph);
		this.timepoint = timepoint;
	}

	@Override
	public Set<FileNode> getFileNodes(IGraphNodeIndex fileIndex, Iterable<String> repoPatterns,	Iterable<String> filePatterns) {
		if (fileIndex instanceof ITimeAwareGraphNodeIndex) {
			ITimeAwareGraphNodeIndex taIndex = (ITimeAwareGraphNodeIndex) fileIndex;
			return super.getFileNodes(taIndex.travelInTime(timepoint), repoPatterns, filePatterns);
		}
		return super.getFileNodes(fileIndex, repoPatterns, filePatterns);
	}

	@Override
	public ModelElementNode getModelElementNodeById(Object id) {
		final IGraphNode rawNode = graph.getNodeById(id);
		
		if (rawNode instanceof ITimeAwareGraphNode) {
			final ITimeAwareGraphNode taNode = (ITimeAwareGraphNode) rawNode;
			return new ModelElementNode(taNode.travelInTime(timepoint));
		} else if (rawNode == null) {
			throw new NoSuchElementException("No node exists with id " + id);
		}

		return new ModelElementNode(rawNode);
	}

}
