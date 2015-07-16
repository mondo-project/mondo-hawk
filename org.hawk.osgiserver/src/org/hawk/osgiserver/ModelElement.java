package org.hawk.osgiserver;

import java.util.List;
import java.util.Map;

public class ModelElement {

	private final String id;
	private final String metamodelUri;
	private final String typeName;

	private final Map<String, List<String>> references;
	private final Map<String, List<String>> attributes;

	public ModelElement(String id, String mmUri, String typeName,
			Map<String, List<String>> refs, Map<String, List<String>> attrs) {
		this.id = id;
		this.metamodelUri = mmUri;
		this.typeName = typeName;
		this.references = refs;
		this.attributes = attrs;
	}

	public String getId() {
		return id;
	}

	public String getMetamodelUri() {
		return metamodelUri;
	}

	public String getTypeName() {
		return typeName;
	}

	public Map<String, List<String>> getReferences() {
		return references;
	}

	public Map<String, List<String>> getAttributes() {
		return attributes;
	}

}
