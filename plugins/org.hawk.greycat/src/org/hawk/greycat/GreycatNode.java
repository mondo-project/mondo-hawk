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
package org.hawk.greycat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.timeaware.ITimeAwareGraphNode;
import org.hawk.greycat.GreycatDatabase.NodeCacheWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import greycat.Constants;
import greycat.Graph;
import greycat.Node;
import greycat.Type;
import greycat.plugin.NodeState;
import greycat.plugin.Resolver;
import greycat.struct.DoubleArray;
import greycat.struct.IntArray;
import greycat.struct.LongArray;
import greycat.struct.Relation;
import greycat.struct.StringArray;
import greycat.struct.StringIntMap;

public class GreycatNode implements ITimeAwareGraphNode {

	private enum Direction {
		IN {
			@Override
			public String getPrefix() {
				return "in_";
			}

			public IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other) {
				if (GreycatHeavyEdge.NODETYPE.equals(other.getNodeLabel())) {
					return new GreycatHeavyEdge(other, type);
				}
				return new GreycatLightEdge(other, current, type);
			}
		}, OUT {
			@Override
			public String getPrefix() {
				return "out_";
			}

			@Override
			public IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other) {
				if (GreycatHeavyEdge.NODETYPE.equals(other.getNodeLabel())) {
					return new GreycatHeavyEdge(other, type);
				}
				return new GreycatLightEdge(current, other, type);
			}
		};

		public abstract String getPrefix();
		public abstract IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other);
	}

	private final class LazyNode {
		private Graph _graph;
		private Node _node;
		private NodeCacheWrapper _cacheWrapper = null;
		private boolean _isDirty = false;

		public LazyNode(Graph graph, Node node) {
			this._graph = graph;
			this._node = node;
		}

		public Node get() {
			if (db.getGraph() == _graph && _node != null) {
				return _node;
			} else {
				_cacheWrapper = db.lookup(world, time, id);
				_cacheWrapper.inUse = true;
				_node = _cacheWrapper.node;
			}

			return _node;
		}

		public void markDirty() {
			this._isDirty = true;
			db.markDirty(GreycatNode.this);
		}

		public void release() {
			if (_cacheWrapper != null && !_isDirty) {
				// Get it from the cache next time
				_node = null;
				_cacheWrapper.inUse = false;
			}
		}

		public void free() {
			if (_node != null) {
				_node.free();
				_node = null;
				_isDirty = false;
			}
		}
	}

	/**
	 * Encapsulates an access to the underlying node, with implicit free unless
	 * changed since the last save, and implicit saves upon full closing of all
	 * nodes open so far. Allows for nested calls: should be safe as long as we
	 * stick to try-with-resources.
	 */
	private int nestingLevel = 0;
	protected final class NodeReader implements AutoCloseable, Supplier<Node> {
		private Node _node = nodeProvider.get();

		private NodeReader() {
			++nestingLevel;
			db.markOpen(GreycatNode.this);
		}

		public Node get() {
			return _node;
		}

		public void markDirty() {
			nodeProvider.markDirty();
		}

		@Override
		public void close() {
			if (--nestingLevel <= 0) {
				db.markClosed(GreycatNode.this);
				nodeProvider.release();
			}
		}
	}

	/** Prefix for all attribute names. Prevents clashes with reserved names. */
	private static final String ATTRIBUTE_PREFIX = "a_";

	/**
	 * Special property used to record actual Java types. Greycat V11 only stores
	 * int, long, double and String scalars and arrays. We have to keep the real
	 * type here and then convert back.
	 * 
	 * Only problem is that we can't really modify arrays in place - we have to
	 * replace them entirely with {@link #setProperty(String, Object)}.
	 */
	private static final String JAVATYPE_PROPERTY = "h_javatypes";
	private static final BiMap<String, Integer> javaTypes = HashBiMap.create();
	static {
		int counter = 0;
		for (Class<?> klass : Arrays.asList(Float.class,
			Double[].class, Float[].class, Long[].class, Integer[].class, Short[].class, Byte[].class,
			double[].class, float[].class, long[].class, int[].class, short[].class, byte[].class)) {
			javaTypes.put(klass.getSimpleName(), counter++);
		}
	}

	/** Greycat does not save empty strings into nodes, instead returning just null for them. We replace them with this placeholder value and map it back afterwards. */
	private static final String EMPTY_STRING_MARKER = "_@_h_empty_@_";

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNode.class);

	private final GreycatDatabase db;
	private final long world, time, id;
	private final LazyNode nodeProvider;

	/** lazily initialized node label */
	private String nodeLabel;

	protected static int getValueType(Object value) {
		if (value == null) {
			return Type.STRING;
		}

		switch (value.getClass().getSimpleName()) {
		case "boolean":
		case "Boolean":
			return Type.BOOL;
		case "Short":
		case "Byte":
		case "Integer":
			return Type.INT;
		case "Long":
			return Type.LONG;
		case "Float":
		case "Double":
			return Type.DOUBLE;
		case "String":
			return Type.STRING;
		}

		LOGGER.warn("Unknown type: {}, returning Type.STRING", value.getClass().getSimpleName());
		return Type.STRING;
	}

	/**
	 * For new nodes produced through queries.
	 */
	public GreycatNode(GreycatDatabase db, long world, long time, long id) {
		this.db = db;

		this.world = world;
		this.time = time;
		this.id = id;

		this.nodeProvider = new LazyNode(db.getGraph(), null);
	}

	/**
	 * For nodes that have been newly created and still need to be saved for the first time.
	 */
	public GreycatNode(GreycatDatabase db, Node node) {
		this.db = db;

		this.world = node.world();
		this.time = node.time();
		this.id = node.id();

		this.nodeProvider = new LazyNode(db.getGraph(), node);
		this.nodeProvider.markDirty();
	}

	@Override
	public Long getId() {
		return id;
	}

	public long getWorld() {
		return world;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public void end() {
		try (NodeReader rn = getNodeReader()) {
			// Unlink node from next timepoint, and then end its lifespan

			/*
			 * end() means that the edges and node should still be available at *this*
			 * precise timepoint, but not from the next timepoint onwards.
			 */
			for (IGraphEdge out : travelInTime(time + 1).getOutgoing()) {
				out.delete();
			}
			for (IGraphEdge in : travelInTime(time + 1).getIncoming()) {
				in.delete();
			}

			rn.get().end();
		}
	}

	@Override
	public List<ITimeAwareGraphNode> getAllVersions() throws Exception {
		return getVersionsBetween(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME);
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsFrom(long fromInclusive) throws Exception {
		return getVersionsBetween(fromInclusive, Constants.END_OF_TIME);
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsUpTo(long toInclusive) throws Exception {
		return getVersionsBetween(Constants.END_OF_TIME, toInclusive);
	}

	@Override
	public List<ITimeAwareGraphNode> getVersionsBetween(long fromInclusive, long toInclusive) throws Exception {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			CompletableFuture<long[]> result = new CompletableFuture<>();
			n.timepoints(fromInclusive, toInclusive, (value) -> {
				result.complete(value);
			});

			List<ITimeAwareGraphNode> versions = new ArrayList<>();
			for (long timepoint : result.get()) {
				versions.add(new GreycatNode(db, world, timepoint, id));
			}
			return versions;
		}
	}

	@Override
	public ITimeAwareGraphNode travelInTime(long time) {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			final CompletableFuture<Node> completable = new CompletableFuture<Node>();
			n.travelInTime(time, (node) -> {
				completable.complete(node);
			});
			final Node on = completable.join();

			return on == null ? null : new GreycatNode(db, on);
		}
	}

	public String getNodeLabel() {
		if (nodeLabel == null) {
			try (NodeReader rn = getNodeReader()) {
				nodeLabel = rn.get().get(GreycatDatabase.NODE_LABEL_IDX).toString();
			}
		}

		return nodeLabel;
	}

	@Override
	public Set<String> getPropertyKeys() {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();
			final Resolver resolver = n.graph().resolver();
			NodeState state = resolver.resolveState(n);

			final Set<String> results = new HashSet<>();
			state.each((attributeKey, elemType, elem) -> {
				final String resolveName = resolver.hashToString(attributeKey);
				if (resolveName != null && resolveName.startsWith(ATTRIBUTE_PREFIX)) {
					results.add(resolveName.substring(ATTRIBUTE_PREFIX.length()));
				}
			});

			return results;
		}
	}

	protected NodeReader getNodeReader() {
		return new NodeReader();
	}

	@Override
	public boolean isAlive() {
		try (NodeReader rn = getNodeReader()) {
			return rn.get() != null;
		}
	}

	@Override
	public Object getProperty(String name) {
		Object rawValue;
		try (NodeReader rn = getNodeReader()) {
			rawValue = rn.get().get(ATTRIBUTE_PREFIX + name);

			if (rawValue instanceof StringArray) {
				return ((StringArray) rawValue).extract();
			} else if (rawValue instanceof LongArray) {
				final int javaTypeID = getJavaTypesMap(rn).getValue(name);
				final String javaType = javaTypes.inverse().get(javaTypeID);
				final LongArray lArray = (LongArray) rawValue;

				if ("Long[]".equals(javaType)) {
					Long[] ret = new Long[lArray.size()];
					for (int i = 0; i < lArray.size(); i++) {
						ret[i] = lArray.get(i);
					}
					return ret;
				}

				return lArray.extract();
			} else if (rawValue instanceof DoubleArray) {
				final int javaTypeID = getJavaTypesMap(rn).getValue(name);
				final String javaType = javaTypes.inverse().get(javaTypeID);
				final DoubleArray dArray = (DoubleArray) rawValue;

				switch (javaType) {
				case "Double[]": {
					Double[] ret = new Double[dArray.size()];
					for (int i = 0; i < dArray.size(); i++) {
						ret[i] = dArray.get(i);
					}
					return ret;
				}
				case "Float[]": {
					Float[] ret = new Float[dArray.size()];
					for (int i = 0; i < dArray.size(); i++) {
						ret[i] = (float) dArray.get(i);
					}
					return ret;
				}
				case "float[]": {
					float[] ret = new float[dArray.size()];
					for (int i = 0; i < dArray.size(); i++) {
						ret[i] = (float) dArray.get(i);
					}
					return ret;
				}
				}

				return dArray.extract();
			} else if (rawValue instanceof IntArray) {
				final int javaTypeID = getJavaTypesMap(rn).getValue(name);
				final String javaType = javaTypes.inverse().get(javaTypeID);
				final IntArray iArray = (IntArray) rawValue;

				switch (javaType) {
				case "Integer[]": {
					Integer[] ret = new Integer[iArray.size()];
					for (int i = 0; i < iArray.size(); i++) {
						ret[i] = iArray.get(i);
					}
					return ret;
				}
				case "Short[]": {
					Short[] ret = new Short[iArray.size()];
					for (int i = 0; i < iArray.size(); i++) {
						ret[i] = (short) iArray.get(i);
					}
					return ret;
				}
				case "short[]": {
					short[] ret = new short[iArray.size()];
					for (int i = 0; i < iArray.size(); i++) {
						ret[i] = (short) iArray.get(i);
					}
					return ret;
				}
				case "Byte[]": {
					Byte[] ret = new Byte[iArray.size()];
					for (int i = 0; i < iArray.size(); i++) {
						ret[i] = (byte) iArray.get(i);
					}
					return ret;
				}
				case "byte[]": {
					byte[] ret = new byte[iArray.size()];
					for (int i = 0; i < iArray.size(); i++) {
						ret[i] = (byte) iArray.get(i);
					}
					return ret;
				}
				}

				return iArray.extract();
			} else if (rawValue instanceof Double) {
				final int javaTypeID = getJavaTypesMap(rn).getValue(name);
				final String javaType = javaTypes.inverse().get(javaTypeID);

				if ("Float".equals(javaType)) {
					return ((Double) rawValue).floatValue();
				}
			} else if (EMPTY_STRING_MARKER.equals(rawValue)) {
				return "";
			}

			return rawValue;
		}
	}

	@Override
	public void setProperty(String name, Object value) {
		try (NodeReader rn = getNodeReader()) {
			setPropertyRaw(rn, name, value);
		}
	}

	/**
	 * Allows for setting multiple properties at once, in a slightly more efficient way.
	 */
	public void setProperties(Map<String, Object> props) {
		try (NodeReader rn = getNodeReader()) {
			setPropertiesRaw(rn, props);
		}
	}

	protected void setPropertiesRaw(NodeReader rn, Map<String, Object> props) {
		for (Entry<String, Object> entry : props.entrySet()) {
			setPropertyRaw(rn, entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Saves the property, marking as dirty the node but without saving.
	 */
	protected void setPropertyRaw(NodeReader rn, String name, Object value) {
		if (value != null && value.getClass().isArray()) {
			setArrayPropertyRaw(rn, name, value);
		} else if (value instanceof Float) {
			getJavaTypesMap(rn).put(name, javaTypes.get(value.getClass().getSimpleName()));
			rn.get().set(ATTRIBUTE_PREFIX + name, Type.DOUBLE, value);
		} else {
			getJavaTypesMap(rn).remove(name);
			if ("".equals(value)) {
				value = EMPTY_STRING_MARKER;
			}
			rn.get().set(ATTRIBUTE_PREFIX + name, getValueType(value), value);
		}
		rn.markDirty();
	}

	protected void setArrayPropertyRaw(NodeReader rn, String name, Object value) {
		// Save real array type here, so we can convert back in getProperty()
		final String  javaType = value.getClass().getSimpleName();
		final Integer javaTypeID = javaTypes.get(javaType);
		if (javaTypeID != null) {
			final StringIntMap arrayTypes = getJavaTypesMap(rn);
			arrayTypes.put(name, javaTypeID);
		}
		final Node n = rn.get();

		switch (javaType) {
		case "Double[]":
		case "double[]":
		case "Float[]":
		case "float[]":
			DoubleArray dArray = (DoubleArray) n.getOrCreate(ATTRIBUTE_PREFIX + name, Type.DOUBLE_ARRAY);
			dArray.clear();

			switch (javaType) {
			case "Double[]":
				for (Double d : (Double[]) value) {
					if (d != null) {
						dArray.addElement(d);
					}
				}
				break;
			case "double[]":
				for (double d : (double[]) value) {
					dArray.addElement(d);
				}
				break;
			case "Float[]":
				for (Float f : (Float[]) value) {
					if (f != null) {
						dArray.addElement(f);
					}
				}
				break;
			case "float[]":
				for (float f : (float[]) value) {
					dArray.addElement(f);
				}
				break;
			}

			break;
		case "Long[]":
		case "long[]":
			LongArray lArray = (LongArray) n.getOrCreate(ATTRIBUTE_PREFIX + name, Type.LONG_ARRAY);
			lArray.clear();

			if ("Long".equals(javaType)) {
				for (long l : (Long[]) value) {
					lArray.addElement(l);
				}
			} else {
				for (long l : (long[]) value) {
					lArray.addElement(l);
				}
			}

			break;
		case "Integer[]":
		case "int[]":
		case "Short[]":
		case "short[]":
		case "Byte[]":
		case "byte[]":
			IntArray iArray = (IntArray) n.getOrCreate(ATTRIBUTE_PREFIX + name, Type.INT_ARRAY);
			iArray.clear();

			switch (javaType) {
			case "Integer[]":
				for (Integer i : (Integer[])value) {
					if (i != null) {
						iArray.addElement(i);
					}
				}
				break;
			case "int[]":
				for (int i : (int[])value) {
					iArray.addElement(i);
				}
				break;
			case "Short[]":
				for (Short i : (Short[])value) {
					if (i != null) {
						iArray.addElement(i);
					}
				}
				break;
			case "short[]":
				for (short i : (short[])value) {
					iArray.addElement(i);
				}
				break;
			case "Byte[]":
				for (Byte i : (Byte[])value) {
					if (i != null) {
						iArray.addElement(i);
					}
				}
				break;
			case "byte[]":
				for (byte i : (byte[])value) {
					iArray.addElement(i);
				}
				break;
			}

			break;
		case "String[]": {
			StringArray sArray = (StringArray) n.getOrCreate(ATTRIBUTE_PREFIX + name, Type.STRING_ARRAY);
			sArray.clear();
			sArray.addAll((String[]) value);
			break;
		}
		}
	}

	protected StringIntMap getJavaTypesMap(NodeReader rn) {
		StringIntMap value = (StringIntMap) rn.get().get(JAVATYPE_PROPERTY);
		if (value == null) {
			value = (StringIntMap) rn.get().getOrCreate(JAVATYPE_PROPERTY, Type.STRING_TO_INT_MAP);
			rn.markDirty();
		}
		return value;
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		try (NodeReader rn = getNodeReader()) {
			return getAllEdges(rn, getAllEdges(rn, new ArrayList<>(), Direction.OUT), Direction.IN);
		}
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		try (NodeReader rn = getNodeReader()) {
			return getEdgesWithType(rn, getEdgesWithType(rn, new ArrayList<>(), Direction.IN, type), Direction.OUT, type);
		}
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		try (NodeReader rn = getNodeReader()) {
			return getEdgesWithType(rn, new ArrayList<>(), Direction.OUT, type);
		}
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		try (NodeReader rn = getNodeReader()) {
			return getEdgesWithType(rn, new ArrayList<>(), Direction.IN, type);
		}
	}

	protected List<IGraphEdge> getEdgesWithType(final NodeReader rn, final List<IGraphEdge> results, final Direction dir, String type) {
		final int relationPosition = db.getGraph().resolver().stringToHash(dir.getPrefix() + type, false);
		final Relation relation = (Relation) rn.get().getAt(relationPosition);

		if (relation != null) {
			final int relSize = relation.size();
			for (int i = 0; i < relSize; i++) {
				/*
				 * Do NOT preload all target nodes, unlike traverse - doing so balloons memory
				 * usage in some cases (e.g. going from the file node to the instance nodes).
				 */
				final long nodeId = relation.get(i);
				final GreycatNode target = new GreycatNode(getGraph(), world, time, nodeId);
				results.add(dir.convertToEdge(type, this, target));
			}
		}

		return results;
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		try (NodeReader rn = getNodeReader()) {
			return getAllEdges(rn, new ArrayList<>(), Direction.IN);
		}
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		try (NodeReader rn = getNodeReader()) {
			return getAllEdges(rn, new ArrayList<>(), Direction.OUT);
		}
	}

	protected List<IGraphEdge> getAllEdges(final NodeReader rn, final List<IGraphEdge> results, final Direction dir) {
		final Node n = rn.get();
		final Resolver resolver = n.graph().resolver();
		final NodeState state = resolver.resolveState(n);
		final String prefix = dir.getPrefix();

		state.each((attributeKey, elemType, elem) -> {
			if (elemType == Type.RELATION) {
				final String resolveName = resolver.hashToString(attributeKey);
				if (resolveName.startsWith(prefix)) {
					final String edgeType = resolveName.substring(prefix.length());
					Relation castedRelArr = (Relation) elem;
					for (int j = 0; j < castedRelArr.size(); j++) {
						final GreycatNode targetNode = new GreycatNode(db, world, time, castedRelArr.get(j));
						results.add(dir.convertToEdge(edgeType, this, targetNode));
					}
				}
			}
		});
		return results;
	}

	@Override
	public void delete() {
		if (db.currentMode() == Mode.NO_TX_MODE) {
			CompletableFuture<Boolean> cSaved = new CompletableFuture<>();
			db.hardDelete(this, dropped -> cSaved.complete(true));
			cSaved.join();
		} else {
			db.softDelete(this);
		}
	}

	@Override
	public GreycatDatabase getGraph() {
		return db;
	}

	@Override
	public void removeProperty(String name) {
		try (NodeReader rn = getNodeReader()) {
			rn.get().remove(ATTRIBUTE_PREFIX + name);
			rn.markDirty();
		}
	}

	protected void free() {
		nodeProvider.free();
	}

	/**
	 * Returns <code>true</code> if this element has been soft deleted.
	 * If so, it should be ignored by any queries and iterables.
	 */
	protected boolean isSoftDeleted() {
		try (NodeReader rn = getNodeReader()) {
			final Boolean softDeleted = (Boolean) rn.get().get(GreycatDatabase.SOFT_DELETED_KEY);
			return softDeleted == Boolean.TRUE;
		}
	}

	protected IGraphEdge addEdge(String type, GreycatNode end, Map<String, Object> props) {
		if (props == null || props.isEmpty()) {
			return GreycatLightEdge.create(type, this, end);
		} else {
			return GreycatHeavyEdge.create(type, this, end, props);
		}
	}

	protected static void addOutgoing(String type, final NodeReader rn, final NodeReader ro) {
		rn.get().addToRelation(Direction.OUT.getPrefix() + type, ro.get());
		rn.markDirty();
	}

	protected static void addIncoming(String type, final NodeReader rn, final NodeReader ro) {
		rn.get().addToRelation(Direction.IN.getPrefix() + type, ro.get());
		rn.markDirty();
	}

	protected static void removeOutgoing(String type, final NodeReader rn, final NodeReader ro) {
		rn.get().removeFromRelation(Direction.OUT.getPrefix() + type, ro.get());
		rn.markDirty();
	}

	protected static void removeIncoming(String type, final NodeReader rn, final NodeReader ro) {
		rn.get().removeFromRelation(Direction.IN.getPrefix() + type, ro.get());
		rn.markDirty();
	}

	@Override
	public String toString() {
		return "GreycatNode [world=" + world + ", time=" + time + ", id=" + id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (int) (time ^ (time >>> 32));
		result = prime * result + (int) (world ^ (world >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GreycatNode other = (GreycatNode) obj;
		if (id != other.id)
			return false;
		if (time != other.time)
			return false;
		if (world != other.world)
			return false;
		return true;
	}

	@Override
	public long getLatestInstant() throws Exception {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			CompletableFuture<long[]> result = new CompletableFuture<>();
			n.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, (value) -> {
				result.complete(value);
			});

			long latest = 0;
			for (long timepoint : result.get()) {
				latest = Math.max(latest, timepoint);
			}
			return latest;
		}
	}

	@Override
	public long getEarliestInstant() throws Exception {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			CompletableFuture<long[]> result = new CompletableFuture<>();
			n.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, (value) -> {
				result.complete(value);
			});

			long latest = 0;
			for (long timepoint : result.get()) {
				latest = Math.min(latest, timepoint);
			}
			return latest;
		}
	}

	@Override
	public long getPreviousInstant() throws Exception {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			CompletableFuture<long[]> result = new CompletableFuture<>();
			n.timepoints(Constants.BEGINNING_OF_TIME, getTime() - 1, (value) -> {
				result.complete(value);
			});

			/*
			 * We assume timepoints(...) produces elements from newest to oldest. The
			 * previous instant is the most recent moment before the current one.
			 */
			final long[] instants = result.get();
			if (instants.length == 0) {
				return NO_SUCH_INSTANT;
			}
			return instants[0];
		}
	}

	@Override
	public long getNextInstant() throws Exception {
		try (NodeReader rn = getNodeReader()) {
			final Node n = rn.get();

			CompletableFuture<long[]> result = new CompletableFuture<>();
			n.timepoints(getTime() + 1, Constants.END_OF_TIME, (value) -> {
				result.complete(value);
			});

			// See getPreviousInstant() for our assumptions
			final long[] instants = result.get();
			if (instants.length == 0) {
				return NO_SUCH_INSTANT;
			}
			return instants[instants.length - 1];
		}
	}

}
