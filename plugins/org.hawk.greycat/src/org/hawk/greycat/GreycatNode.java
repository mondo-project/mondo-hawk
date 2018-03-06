package org.hawk.greycat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.hawk.core.graph.IGraphDatabase.Mode;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

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

public class GreycatNode implements IGraphNode {

	private enum Direction {
		IN {
			@Override
			public String getPrefix() {
				return "in_";
			}

			public IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other) {
				if (GreycatHeavyEdge.NODETYPE.equals(other.getNodeLabel())) {
					return new GreycatHeavyEdge(other);
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
					return new GreycatHeavyEdge(other);
				}
				return new GreycatLightEdge(current, other, type);
			}
		};

		public abstract String getPrefix();
		public abstract IGraphEdge convertToEdge(String type, GreycatNode current, GreycatNode other);
	}

	protected final class LazyNode implements Supplier<Node> {
		private Graph _graph;
		private Node _node;

		public LazyNode(Graph graph, Node node) {
			this._graph = graph;
			this._node = node;
		}

		@Override
		public Node get() {
			if (db.getGraph() == _graph) {
				return _node;
			} else {
				// there was a reconnection: reload the Node
				CompletableFuture<Node> c = new CompletableFuture<>();
				db.getGraph().lookup(world, time, id, node -> {
					c.complete(node);
				});

				try {
					_node = c.get();
				} catch (InterruptedException | ExecutionException e) {
					LOGGER.error(e.getMessage(), e);
					_node = null;
				}
			}

			return _node;
		}

		@Override
		public void finalize() {
			_node.free();
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
		for (Class<?> klass : Arrays.asList(String[].class, Float.class,
			Double[].class, Float[].class, Long[].class, Integer[].class, Short[].class, Byte[].class,
			double[].class, float[].class, long[].class, int[].class, short[].class, byte[].class)) {
			javaTypes.put(klass.getSimpleName(), counter++);
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(GreycatNode.class);

	private final GreycatDatabase db;
	private final long world, time, id;
	private final Supplier<Node> node;

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

	public GreycatNode(GreycatDatabase db, Node node) {
		this.db = db;

		this.world = node.world();
		this.time = node.time();
		this.id = node.id();
		this.node = new LazyNode(node.graph(), node);
	}

	@Override
	public Object getId() {
		return id;
	}

	public long getWorld() {
		return getNode().world();
	}

	public long getTime() {
		return getNode().time();
	}

	public String getNodeLabel() {
		return getNode().get(GreycatDatabase.NODE_LABEL_IDX).toString();
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Node n = getNode();
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

	@Override
	public Object getProperty(String name) {
		final Object rawValue = getNode().get(ATTRIBUTE_PREFIX + name);

		if (rawValue instanceof StringArray) {
			return ((StringArray)rawValue).extract();
		} else if (rawValue instanceof LongArray) {
			final int javaTypeID = getJavaTypesMap().getValue(name);
			final String javaType = javaTypes.inverse().get(javaTypeID);
			final LongArray lArray = (LongArray)rawValue;

			if ("Long[]".equals(javaType)) {
				Long[] ret = new Long[lArray.size()];
				for (int i = 0; i < lArray.size(); i++) {
					ret[i] = lArray.get(i);
				}
				return ret;
			}

			return lArray.extract();
		} else if (rawValue instanceof DoubleArray) {
			final int javaTypeID = getJavaTypesMap().getValue(name);
			final String javaType = javaTypes.inverse().get(javaTypeID);
			final DoubleArray dArray = (DoubleArray)rawValue;

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
			final int javaTypeID = getJavaTypesMap().getValue(name);
			final String javaType = javaTypes.inverse().get(javaTypeID);
			final IntArray iArray = (IntArray)rawValue;

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
			final int javaTypeID = getJavaTypesMap().getValue(name);
			final String javaType = javaTypes.inverse().get(javaTypeID);

			if ("Float".equals(javaType)) {
				return ((Double) rawValue).floatValue();
			}
		}

		return rawValue;
	}

	@Override
	public void setProperty(String name, Object value) {
		setPropertyRaw(name, value);
		save();
	}

	/**
	 * Allows for setting multiple properties at once, in a slightly more efficient way.
	 */
	public void setProperties(Map<String, Object> props) {
		for (Entry<String, Object> entry : props.entrySet()) {
			setPropertyRaw(entry.getKey(), entry.getValue());
		}
		save();
	}

	/**
	 * Saves the property, without saving.
	 */
	protected void setPropertyRaw(String name, Object value) {
		if (value != null && value.getClass().isArray()) {
			setArrayPropertyRaw(name, value);
		} else if (value instanceof Float) {
			getJavaTypesMap().put(name, javaTypes.get(value.getClass().getSimpleName()));
			getNode().set(ATTRIBUTE_PREFIX + name, Type.DOUBLE, value);
		} else {
			getJavaTypesMap().remove(name);
			getNode().set(ATTRIBUTE_PREFIX + name, getValueType(value), value);
		}
	}

	protected void setArrayPropertyRaw(String name, Object value) {
		// Save real array type here, so we can convert back in getProperty()
		final String  javaType = value.getClass().getSimpleName();
		final Integer javaTypeID = javaTypes.get(javaType);
		if (javaTypeID == null) {
			throw new IllegalArgumentException("Unknown array component type: " + javaType);
		}

		final StringIntMap arrayTypes = getJavaTypesMap();
		arrayTypes.put(name, javaTypeID);

		switch (javaType) {
		case "Double[]":
		case "double[]":
		case "Float[]":
		case "float[]":
			DoubleArray dArray = (DoubleArray) getNode().getOrCreate(ATTRIBUTE_PREFIX + name, Type.DOUBLE_ARRAY);
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
			LongArray lArray = (LongArray) getNode().getOrCreate(ATTRIBUTE_PREFIX + name, Type.LONG_ARRAY);
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
			IntArray iArray = (IntArray) getNode().getOrCreate(ATTRIBUTE_PREFIX + name, Type.INT_ARRAY);
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
			StringArray sArray = (StringArray) getNode().getOrCreate(ATTRIBUTE_PREFIX + name, Type.STRING_ARRAY);
			sArray.clear();
			sArray.addAll((String[]) value);
			break;
		}
		}
	}

	protected StringIntMap getJavaTypesMap() {
		return (StringIntMap) getNode().getOrCreate(JAVATYPE_PROPERTY, Type.STRING_TO_INT_MAP);
	}

	@Override
	public Iterable<IGraphEdge> getEdges() {
		return getAllEdges(getAllEdges(new ArrayList<>(), Direction.OUT), Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getEdgesWithType(String type) {
		return getEdgesWithType(getEdgesWithType(new ArrayList<>(), Direction.IN, type), Direction.OUT, type);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoingWithType(String type) {
		return getEdgesWithType(new ArrayList<>(), Direction.OUT, type);
	}

	@Override
	public Iterable<IGraphEdge> getIncomingWithType(String type) {
		return getEdgesWithType(new ArrayList<>(), Direction.IN, type);
	}

	protected List<IGraphEdge> getEdgesWithType(final List<IGraphEdge> results, final Direction dir, String type) {
		final CompletableFuture<Boolean> done = new CompletableFuture<>();
		node.get().traverse(dir.getPrefix() + type, (Node[] targets) -> {
			if (targets != null) {
				for (Node target : targets) {
					results.add(dir.convertToEdge(type, this,
						new GreycatNode(getGraph(), target)));
				}
			}
			done.complete(true);
		});
		done.join();
	
		return results;
	}

	@Override
	public Iterable<IGraphEdge> getIncoming() {
		return getAllEdges(new ArrayList<>(), Direction.IN);
	}

	@Override
	public Iterable<IGraphEdge> getOutgoing() {
		return getAllEdges(new ArrayList<>(), Direction.OUT);
	}

	protected List<IGraphEdge> getAllEdges(final List<IGraphEdge> results, final Direction dir) {
		final Node n = getNode();
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
	                	GreycatNode targetNode = db.getNodeById(castedRelArr.get(j));
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
		node.get().remove(ATTRIBUTE_PREFIX + name);
		save();
	}

	protected void save() {
		db.save(this);
	}

	public Node getNode() {
		return node.get();
	}

	/**
	 * Returns <code>true</code> if this element has been soft deleted.
	 * If so, it should be ignored by any queries and iterables.
	 */
	protected boolean isSoftDeleted() {
		final Boolean softDeleted = (Boolean) node.get().get(GreycatDatabase.SOFT_DELETED_KEY);
		return softDeleted == Boolean.TRUE;
	}

	protected IGraphEdge addEdge(String type, GreycatNode end, Map<String, Object> props) {
		if (props == null || props.isEmpty()) {
			return GreycatLightEdge.create(type, this, end);
		} else {
			return GreycatHeavyEdge.create(type, this, end, props);
		}
	}

	protected void addOutgoing(String type, final GreycatNode endNode) {
		getNode().addToRelation(Direction.OUT.getPrefix() + type, endNode.getNode());
	}

	protected void addIncoming(String type, final GreycatNode endNode) {
		getNode().addToRelation(Direction.IN.getPrefix() + type, endNode.getNode());
	}

	protected void removeOutgoing(String type, final GreycatNode endNode) {
		getNode().removeFromRelation(Direction.OUT.getPrefix() + type, endNode.getNode());
	}

	protected void removeIncoming(String type, final GreycatNode endNode) {
		getNode().removeFromRelation(Direction.IN.getPrefix() + type, endNode.getNode());
	}

	@Override
	public String toString() {
		return "GreycatNode [world=" + world + ", time=" + time + ", id=" + id + ", getNode()=" + getNode() + "]";
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

}
