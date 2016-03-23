package org.hawk.graph.internal.updater;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkClassifier;

/**
 * Cache that can be shared across multiple {@link GraphModelBatchInjector}
 * classes to avoid retrieving the same type node again and again. Designed to
 * be simply GC'ed during the {@link GraphModelUpdater#updateProxies()} stage.
 */
public class TypeCache {

	private Map<IHawkClass, IGraphNode> hashedEClasses = new HashMap<>();
	private Map<IGraphNode, Map<String, Object>> hashedEClassProperties = new HashMap<>();

	public IGraphNode getEClassNode(IGraphDatabase graph, IHawkClassifier e) throws Exception {
		IHawkClass eClass = null;

		if (e instanceof IHawkClass)
			eClass = ((IHawkClass) e);
		else
			System.err.println("getEClassNode called on a non-class classifier:\n" + e);

		IGraphNode classnode = hashedEClasses.get(eClass);

		if (classnode == null) {

			final String packageNSURI = eClass.getPackageNSURI();
			IGraphNode epackagenode = null;
			try {
				epackagenode = graph.getMetamodelIndex().get("id", packageNSURI).getSingle();
			} catch (NoSuchElementException ex) {
				throw new Exception("Metamodel " + packageNSURI
						+ " does not have a Node associated with it in the store, please make sure it has been inserted");
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			for (IGraphEdge r : epackagenode.getEdges()) {

				IGraphNode othernode = r.getStartNode();

				if (!othernode.equals(epackagenode)
						&& othernode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY).equals(eClass.getName())) {
					classnode = othernode;
					break;
				}
			}

			if (classnode != null)
				hashedEClasses.put(eClass, classnode);
			else {
				throw new Exception("eClass: " + eClass.getName() + "(" + eClass.getUri()
						+ ") does not have a Node associated with it in the store, please make sure the relevant metamodel has been inserted");

			}

			// typeCache properties
			Hashtable<String, Object> properties = new Hashtable<>();
			for (String s : classnode.getPropertyKeys()) {
				Object prop = classnode.getProperty(s);
				if (prop instanceof String[])
					properties.put(s, prop);
			}
			hashedEClassProperties.put(classnode, properties);
		}

		return classnode;

	}

	public Map<String, Object> getEClassNodeProperties(IGraphDatabase graph, IHawkClassifier e) throws Exception {
		return hashedEClassProperties.get(getEClassNode(graph, e));
	}
}
