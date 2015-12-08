/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a single Modelio object, as parsed from the <code>.exml</code>
 * file.
 */
public class ExmlObject extends ExmlReference {

	private String parentName, parentMClassName, parentUid;
	private final Map<String, String> attributes = new LinkedHashMap<>();
	private final Map<String, List<ExmlReference>> compositions = new LinkedHashMap<>();
	private final Map<String, List<ExmlReference>> links = new LinkedHashMap<>();

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	public String getParentMClassName() {
		return parentMClassName;
	}

	public void setParentMClassName(String parentMClassName) {
		this.parentMClassName = parentMClassName;
	}

	public String getParentUID() {
		return parentUid;
	}

	public void setParentUID(String parentUid) {
		this.parentUid = parentUid;
	}

	/**
	 * Returns the value of the attribute <code>key</code> if set, or <code>null</code> if it is not set.
	 */
	public String getAttribute(String key) {
		return attributes.get(key);
	}

	/**
	 * If <code>val</code> is not <code>null</code>, sets the value of the
	 * attribute <code>key</code> to it. If it is <code>null</code>, unsets the
	 * value.
	 */
	public void setAttribute(String key, String val) {
		if (val != null) {
			this.attributes.put(key, val);
		} else {
			this.attributes.remove(key);
		}
	}

	/**
	 * Returns an unmodifiable view of the attributes of the object.
	 */
	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * Adds <code>ref</code> as part of the composition dependency <code>key</code>.
	 */
	public void addToComposition(String key, ExmlReference ref) {
		List<ExmlReference> entries = compositions.get(key);
		if (entries == null) {
			entries = new ArrayList<>();
			compositions.put(key, entries);
		}
		entries.add(ref);
	}

	/**
	 * Returns an unmodifiable view of the composition dependencies of the object.
	 */
	public Map<String, List<ExmlReference>> getCompositions() {
		return Collections.unmodifiableMap(compositions);
	}

	/**
	 * Adds <code>ref</code> as part of the link dependency <code>key</code>.
	 */
	public void addToLink(String key, ExmlReference ref) {
		List<ExmlReference> entries = links.get(key);
		if (entries == null) {
			entries = new ArrayList<>();
			links.put(key, entries);
		}
		entries.add(ref);
	}

	/**
	 * Returns an unmodifiable view of the link dependencies of the object.
	 */
	public Map<String, List<ExmlReference>> getLinks() {
		return Collections.unmodifiableMap(links);
	}

	@Override
	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		buildString(sbuf, "\n  ", this);
		return sbuf.toString();
	}

	private static void buildString(final StringBuffer sbuf, final String indent, final ExmlObject o) {
		sbuf.append("ExmlObject [");

		sbuf.append(indent); sbuf.append("name = "); sbuf.append(o.getName());
		sbuf.append(indent); sbuf.append("mClassName = "); sbuf.append(o.getMClassName());
		sbuf.append(indent); sbuf.append("uid = "); sbuf.append(o.getUID());
		sbuf.append(indent); sbuf.append("parentName = "); sbuf.append(o.parentName);
		sbuf.append(indent); sbuf.append("parentMClassName = "); sbuf.append(o.parentMClassName);
		sbuf.append(indent); sbuf.append("parentUid = "); sbuf.append(o.parentUid);

		sbuf.append(indent);
		sbuf.append("attributes = {");
		if (!o.attributes.isEmpty()) {
			for (Entry<String, String> entry : o.attributes.entrySet()) {
				sbuf.append(indent);
				sbuf.append("  ");
				sbuf.append(entry.getKey());
				sbuf.append(" = ");
				sbuf.append(entry.getValue());
			}
			sbuf.append(indent);
		}
		sbuf.append("}");

		sbuf.append(indent);
		sbuf.append("compositions = {");
		if (!o.compositions.isEmpty()) {
			for (Entry<String, List<ExmlReference>> entry : o.compositions.entrySet()) {
				sbuf.append(indent);
				sbuf.append("  ");
				sbuf.append(entry.getKey());
				sbuf.append(" = [");
				if (!entry.getValue().isEmpty()) {
					for (ExmlReference ref : entry.getValue()) {
						sbuf.append(indent);
						sbuf.append("    ");
						if (ref instanceof ExmlObject) {
							buildString(sbuf, indent + "      ", (ExmlObject)ref);
						} else {
							sbuf.append(ref);
						}
					}
				}
				sbuf.append("]");
			}
			sbuf.append(indent);
		}
		sbuf.append("}");

		sbuf.append(indent);
		sbuf.append("links = {");
		if (!o.links.isEmpty()) {
			for (Entry<String, List<ExmlReference>> entry : o.links.entrySet()) {
				sbuf.append(indent);
				sbuf.append("  ");
				sbuf.append(entry.getKey());
				sbuf.append(" = [");
				if (!entry.getValue().isEmpty()) {
					for (ExmlReference ref : entry.getValue()) {
						sbuf.append(indent);
						sbuf.append("    ");
						sbuf.append(ref);
					}
				}
				sbuf.append("]");
			}
			sbuf.append(indent);
		}
		sbuf.append("}");

		sbuf.append("]");		
	}

}
