/*******************************************************************************
 * Copyright (c) 2018 Aston University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.greycat;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.hawk.core.IConsole;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.NodeIndex;
import greycat.Type;
import greycat.rocksdb.RocksDBStorage;

public class GreycatDatabase implements IGraphDatabase {

	/**
	 * Apparently deletion is the one thing we cannot roll back from through a
	 * forced reconnection to the DB. Instead, we will set a property marking this
	 * node as deleted, and all querying will ignore this node. After the next
	 * proper save, we will go through the DB and do some garbage collection.
	 */
	protected static final String SOFT_DELETED_KEY = "h_softDeleted";

	/**
	 * Greycat doesn't seem to have node labels by itself, but we can easily emulate
	 * this with a custom index.
	 */
	protected static final String NODE_LABEL_IDX = "h_nodeLabel";


	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatDatabase.class);

	private File storageFolder, tempFolder;
	private IConsole console;
	private Graph graph;
	private NodeIndex nodeLabelIndex, softDeleteIndex;
	private Mode mode = Mode.NO_TX_MODE;

	private int world = 0;
	private int time = 0;

	public int getWorld() {
		return world;
	}

	public void setWorld(int world) {
		this.world = world;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	@Override
	public String getPath() {
		return storageFolder.getAbsolutePath();
	}

	@Override
	public void run(File parentFolder, IConsole c) {
		this.storageFolder = parentFolder;
		this.tempFolder = new File(storageFolder, "temp");
		this.console = c;

		reconnect();
	}

	@Override
	public void shutdown() throws Exception {
		graph.disconnect(result -> {
			console.println("Disconnected from GreyCat graph");
		});
	}

	@Override
	public void delete() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphEdgeIndex getOrCreateEdgeIndex(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphNodeIndex getMetamodelIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphNodeIndex getFileIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IGraphTransaction beginTransaction() throws Exception {
		if (mode == Mode.NO_TX_MODE) {
			exitBatchMode();
		}
		return new GreycatTransaction(this);
	}

	@Override
	public boolean isTransactional() {
		return true;
	}

	@Override
	public void enterBatchMode() {
		mode = Mode.NO_TX_MODE;
	}

	@Override
	public void exitBatchMode() {
		mode = Mode.TX_MODE;
	}

	@Override
	public IGraphIterable<IGraphNode> allNodes(String label) {
		return new GreycatNodeIterable(this, () -> {
			CompletableFuture<Node[]> nodes = new CompletableFuture<>();
			nodeLabelIndex.find(result -> {
				nodes.complete(result);
			}, world, time, label);
			return nodes.get();
		});
	}

	@Override
	public GreycatNode createNode(Map<String, Object> props, String label) {
		final Node node = graph.newNode(world, time);
		node.set(NODE_LABEL_IDX, Type.STRING, label);

		final GreycatNode n = new GreycatNode(this, node);
		if (props != null) {
			n.setProperties(props);
		}
		nodeLabelIndex.update(node);
		saveOutsideTx(new CompletableFuture<>()).join();

		return new GreycatNode(this, node);
	}

	protected CompletableFuture<Boolean> saveOutsideTx(CompletableFuture<Boolean> result) {
		if (mode == Mode.NO_TX_MODE) {
			save(result);
		} else {
			result.complete(true);
		}
		return result;
	}

	protected void save(CompletableFuture<Boolean> result) {
		graph.save(saved -> {
			softDeleteIndex.find(results -> {
				Semaphore sem = new Semaphore(-results.length + 1);
				for (Node n : results) {
					hardDelete(new GreycatNode(this, n), dropped -> sem.release());
				}

				// Wait until all nodes have been dropped
				try {
					sem.acquire();
				} catch (InterruptedException e) {
					LOGGER.error(e.getMessage(), e);
				}
				result.complete(saved);
			}, world, time);
		});
	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		return createRelationship(start, end, type, Collections.emptyMap());
	}

	@Override
	public IGraphEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props) {
		final GreycatNode gStart = (GreycatNode)start;
		final GreycatNode gEnd = (GreycatNode)end;
		return gStart.addEdge(type, gEnd, props);
	}

	@Override
	public Graph getGraph() {
		return graph;
	}

	@Override
	public GreycatNode getNodeById(Object id) {
		CompletableFuture<GreycatNode> result = new CompletableFuture<>();
		graph.lookup(world, time, (long) id, (node) -> {
			result.complete(new GreycatNode(this, node));
		});

		try {
			return result.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public boolean nodeIndexExists(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean edgeIndexExists(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getType() {
		return getClass().getCanonicalName();
	}

	@Override
	public String getHumanReadableName() {
		return "GreyCat Database";
	}

	@Override
	public String getTempDir() {
		return tempFolder.getAbsolutePath();
	}

	@Override
	public File logFull() throws Exception {
		File logFolder = new File(storageFolder, "logs");
		logFolder.mkdir();
		// TODO print something here
		return logFolder;
	}

	@Override
	public Mode currentMode() {
		return mode;
	}

	@Override
	public Set<String> getNodeIndexNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getEdgeIndexNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getKnownMMUris() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean reconnect() {
		CompletableFuture<Boolean> connected = new CompletableFuture<>();

		if (graph != null) {
			// Only disconnect storage - we want to release locks on the storage *without*
			// saving.
			// Seems to be the only simple way to do a rollback to the latest saved state.
			graph.storage().disconnect(disconnectedStorage -> {
				connect(connected);
			});
		} else {
			connect(connected);
		}

		try {
			return connected.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	protected void connect(CompletableFuture<Boolean> cConnected) {
		this.graph = new GraphBuilder().withStorage(new RocksDBStorage(storageFolder.getAbsolutePath())).build();

		graph.connect((connected) -> {
			if (connected) {
				console.println("Connected to Greycat DB at " + storageFolder);
				graph.declareIndex(world, NODE_LABEL_IDX, nodeIndex -> {
					this.nodeLabelIndex = nodeIndex;
					graph.declareIndex(world, SOFT_DELETED_KEY, softDeleteIndex -> {
						this.softDeleteIndex = softDeleteIndex;
						cConnected.complete(true);
					}, SOFT_DELETED_KEY);
				}, NODE_LABEL_IDX);
			} else {
				LOGGER.error("Could not connect to Greycat DB");
				cConnected.complete(false);
			}
		});
	}

	protected void hardDelete(GreycatNode gn, Callback<?> callback) {
		// Remove all edges (otherwise, future reads may fail)
		final Node n = gn.getNode();
		for (IGraphEdge e : gn.getEdges()) {
			e.delete();
		}

		softDeleteIndex.unindex(n);
		nodeLabelIndex.unindex(n);
		n.drop(callback);
	}

	/**
	 * Marks a node to be ignored within this transaction, and deleted after the
	 * next save.
	 */
	protected void softDelete(GreycatNode gn) {
		// Soft delete, to make definitive after next save
		final Node node = gn.getNode();
		node.set(SOFT_DELETED_KEY, Type.BOOL, true);
		softDeleteIndex.update(node);
	}

}
