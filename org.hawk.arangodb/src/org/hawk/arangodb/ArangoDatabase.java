package org.hawk.arangodb;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hawk.core.IConsole;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdgeIndex;
import org.hawk.core.graph.IGraphIterable;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeIndex;
import org.hawk.core.model.IHawkClass;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.DocumentEntity;
import com.arangodb.entity.EdgeEntity;

public class ArangoDatabase implements IGraphDatabase {

	private File storageFolder;
	private File tempFolder;
	private IConsole console;

	private ArangoDriver driver;
	private ArangoConfigure config;

	@Override
	public String getPath() {
		return storageFolder.getPath();
	}

	@Override
	public void run(File parentfolder, IConsole c) {
		this.storageFolder = parentfolder;
		this.tempFolder = new File(storageFolder, "temp");
		this.console = c;

		config = new ArangoConfigure();
		config.setUser("root");
		config.setPassword("root");
		config.setDefaultDatabase(storageFolder.getName());
		config.init();
		driver = new ArangoDriver(config);

		try {
			driver.createDatabase(storageFolder.getName());
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		driver.setDefaultDatabase(storageFolder.getName());
	}

	@Override
	public void shutdown() throws Exception {
		config.shutdown();
	}

	@Override
	public void delete() throws Exception {
		driver.deleteDatabase(storageFolder.getName());
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
		return getOrCreateNodeIndex("hawkFiles");
	}

	@Override
	public ArangoTransaction beginTransaction() throws Exception {
		return new ArangoTransaction();
	}

	@Override
	public boolean isTransactional() {
		return false;
	}

	@Override
	public void enterBatchMode() {
		try {
			driver.startBatchMode();
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void exitBatchMode() {
		try {
			driver.cancelBatchMode();
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public IGraphIterable<IGraphNode> allNodes(String label) {
		return new AllNodesIterable(this, label);
	}

	public class MyValue {
		public Object o;
		public MyValue(Object o) {
			this.o = o;
		}
		public Object getO() {
			return o;
		}
		public void setO(Object o) {
			this.o = o;
		}
	}
	
	@Override
	public ArangoNode createNode(Map<String, Object> props, String label) {
		try {
			if (!driver.getCollections().getNames().containsKey(label)) {
				driver.createCollection(label);
			}

			DocumentEntity<MyValue> doc = driver.createDocument(label, new MyValue(props), true);
			DocumentEntity<BaseDocument> entity = driver.getDocument(doc.getDocumentHandle(), BaseDocument.class);
			return new ArangoNode(this, entity.getEntity());
		} catch (ArangoException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ArangoEdge createRelationship(IGraphNode start, IGraphNode end, String type) {
		return createRelationship(start, end, type, Collections.emptyMap());
	}

	@Override
	public ArangoEdge createRelationship(IGraphNode start, IGraphNode end, String type, Map<String, Object> props) {
		try {
			if (!driver.getCollections().getNames().containsKey(type)) {
				driver.createCollection(type);
			}

			EdgeEntity<BaseDocument> edge = driver.createEdge(type, new BaseDocument(props), start.getId().toString(), end.getId().toString(), true);
			edge.getEntity().setDocumentHandle(edge.getDocumentHandle());
			edge.getEntity().setDocumentKey(edge.getDocumentKey());
			edge.getEntity().setDocumentRevision(edge.getDocumentRevision());
			edge.getEntity().addAttribute("__type", type);
			return new ArangoEdge(this, edge.getEntity());
		} catch (ArangoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void registerNodeClass(String label, IHawkClass schema) {
		// nothing to do
	}

	@Override
	public IGraphNode createNode(Map<String, Object> properties, String label, IHawkClass schema) {
		return createNode(properties, label);
	}

	@Override
	public ArangoDriver getGraph() {
		return driver;
	}

	@Override
	public IGraphNode getNodeById(Object id) {
		// TODO Auto-generated method stub
		return null;
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
		return ArangoDatabase.class.getCanonicalName();
	}

	@Override
	public String getHumanReadableName() {
		return "ArangoDB";
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
		return Mode.NO_TX_MODE;
	}

	@Override
	public Set<String> getNodeIndexNames() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public Set<String> getEdgeIndexNames() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public Set<String> getKnownMMUris() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

}
