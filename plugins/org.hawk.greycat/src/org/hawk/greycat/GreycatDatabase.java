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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.hawk.core.IConsole;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.greycat.lucene.GreycatLuceneIndexer;
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
	private Mode mode = Mode.TX_MODE;
	private GreycatLuceneIndexer luceneIndexer;

	// Keeps nodes in current tx reachable, to avoid GC: note
	// that Greycat has a cache capacity and bails out when
	// exceeded
	private List<GreycatNode> currentTxNodes = new ArrayList<>();

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
		try {
			this.luceneIndexer = new GreycatLuceneIndexer(this, new File(storageFolder, "lucene"));
		} catch (IOException e) {
			LOGGER.error("Could not set up Lucene indexing", e);
		}

		reconnect();
	}

	@Override
	public void shutdown() throws Exception {
		luceneIndexer.shutdown();
		graph.disconnect(result -> {
			console.println("Disconnected from GreyCat graph");
		});
	}

	@Override
	public void delete() throws Exception {
		CompletableFuture<Boolean> done = new CompletableFuture<>();
		graph.disconnect(result -> {
			try {
				deleteRecursively(storageFolder);
			} catch (IOException e) {
				LOGGER.error("Error while deleting Greycat storage", e);
			}
			done.complete(true);
		});
		done.join();

		graph = null;
	}

	private static void deleteRecursively(File f) throws IOException {
		if (!f.exists()) return;

		Files.walkFileTree(f.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	@Override
	public IGraphNodeIndex getOrCreateNodeIndex(String name) {
		return luceneIndexer.getIndex(name);
	}

	@Override
	public IGraphNodeIndex getMetamodelIndex() {
		return getOrCreateNodeIndex("_hawkMetamodelIndex");
	}

	@Override
	public IGraphNodeIndex getFileIndex() {
		return getOrCreateNodeIndex("_hawkFileIndex");
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
		if (mode != Mode.NO_TX_MODE) {
			commitLuceneIndex();
			mode = Mode.NO_TX_MODE;
		}
	}

	@Override
	public void exitBatchMode() {
		if (mode != Mode.TX_MODE) {
			commitLuceneIndex();
			mode = Mode.TX_MODE;
		}
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
		save(n);

		return n;
	}

	protected Boolean save(GreycatNode n) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		if (mode == Mode.NO_TX_MODE) {
			save(result);
		} else {
			currentTxNodes.add(n);
			result.complete(true);
		}
		return result.join();
	}

	protected void save(CompletableFuture<Boolean> result) {
		// First stage save
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

				if (results.length > 0) {
					// Second stage (for fully dropping those nodes)
					graph.save(savedAgain -> result.complete(savedAgain));
				} else {
					result.complete(saved);
				}
			}, world, time);
		});

		currentTxNodes.clear();
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
		if (id instanceof String) {
			id = Long.valueOf((String) id);
		}

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
		return luceneIndexer.indexExists(name);
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
	public Mode currentMode() {
		return mode;
	}

	@Override
	public Set<String> getNodeIndexNames() {
		return luceneIndexer.getIndexNames();
	}

	@Override
	public Set<String> getKnownMMUris() {
		final Set<String> mmURIs = new HashSet<>();
		for (IGraphNode node : getMetamodelIndex().query("*", "*")) {
			String mmURI = (String)node.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
			mmURIs.add(mmURI);
		}
		return mmURIs;
	}

	public boolean reconnect() {
		CompletableFuture<Boolean> connected = new CompletableFuture<>();

		if (graph != null) {
			try {
				luceneIndexer.rollback();
			} catch (IOException e) {
				LOGGER.error("Failed to roll back changes to Lucene", e);
			}

			/*
			 * Only disconnect storage - we want to release locks on the storage *without*
			 * saving. Seems to be the only simple way to do a rollback to the latest saved
			 * state.
			 */
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
		this.graph = new GraphBuilder()
			.withStorage(new RocksDBStorage(storageFolder.getAbsolutePath()))
			.build();

		exitBatchMode();
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
		unlink(gn);
		gn.getNode().drop(callback);
	}

	/**
	 * Marks a node to be ignored within this transaction, and deleted after the
	 * next save.
	 */
	protected void softDelete(GreycatNode gn) {
		unlink(gn);

		// Soft delete, to make definitive after next save
		final Node node = gn.getNode();
		node.set(SOFT_DELETED_KEY, Type.BOOL, true);
		softDeleteIndex.update(node);
	}

	/**
	 * Changes a node so it is disconnected from the graph and cannot be found
	 * through the internal indices.
	 */
	private void unlink(GreycatNode gn) {
		for (IGraphEdge e : gn.getEdges()) {
			e.delete();
		}

		final Node n = gn.getNode();
		softDeleteIndex.unindex(n);
		nodeLabelIndex.unindex(n);
		luceneIndexer.remove(gn);
	}

	protected void commitLuceneIndex() {
		try {
			luceneIndexer.commit();
		} catch (IOException ex) {
			LOGGER.error("Failed to commit Lucene index", ex);
		}
	}
}
