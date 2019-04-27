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

import org.hawk.core.graph.timeaware.ITimeAwareGraphNodeVersionIndex;
import org.hawk.graph.Slot;
import org.hawk.graph.TypeNode;

/**
 * Java bean which stores all the information related to a version annotator.
 * This is a Boolean query that is evaluated on model elements of a certain type
 * on every relevant version: if it evaluates to <code>true</code>, that version
 * of the node will be tagged and will be retrievable through a
 * {@link ITimeAwareGraphNodeVersionIndex}.
 *
 * Creating instances of this class is done through a fluent API, to avoid
 * constructors with too many unnamed arguments.
 */
public class VersionAnnotatorSpec {

	private final String metamodelURI;
	private final String typeName;
	private final String versionLabel;
	private final String expressionLanguage;
	private final String expression;

	private VersionAnnotatorSpec(String metamodelURI, String typeName, String versionLabel, String expressionLanguage, String expression) {
		this.metamodelURI = metamodelURI;
		this.typeName = typeName;
		this.versionLabel = versionLabel;
		this.expressionLanguage = expressionLanguage;
		this.expression = expression;
	}

	public static VersionAnnotatorSpec from(TypeNode typeNode, Slot slot) {
		return new Builder()
			.metamodelURI(typeNode.getMetamodelURI())
			.typeName(typeNode.getTypeName())
			.label(slot.getName())
			.language(slot.getDerivationLanguage())
			.expression(slot.getDerivationLogic()).build();
	}

	public static class Builder {

		private String metamodelURI, typeName, versionLabel, expressionLanguage, expression;

		public Builder metamodelURI(String uri) {
			this.metamodelURI = uri;
			return this;
		}

		public Builder typeName(String type) {
			this.typeName = type;
			return this;
		}

		public Builder label(String l) {
			this.versionLabel = l;
			return this;
		}

		public Builder language(String el) {
			this.expressionLanguage = el;
			return this;
		}

		public Builder expression(String expr) {
			this.expression = expr;
			return this;
		}

		public VersionAnnotatorSpec build() {
			if (this.metamodelURI == null) {
				throw new IllegalStateException("Metamodel URI is required");
			}
			if (this.typeName == null) {
				throw new IllegalStateException("Type name is required");
			}
			if (this.versionLabel == null) {
				throw new IllegalStateException("Label is required");
			}

			return new VersionAnnotatorSpec(
				metamodelURI, typeName, versionLabel,
				expressionLanguage, expression
			);
		}
	}

	public String getMetamodelURI() {
		return metamodelURI;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public String getExpressionLanguage() {
		return expressionLanguage;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public String toString() {
		return "VersionAnnotator [metamodelURI=" + metamodelURI + ", typeName=" + typeName + ", versionLabel="
				+ versionLabel + ", expressionLanguage=" + expressionLanguage + ", expression=" + expression + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((expressionLanguage == null) ? 0 : expressionLanguage.hashCode());
		result = prime * result + ((metamodelURI == null) ? 0 : metamodelURI.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		result = prime * result + ((versionLabel == null) ? 0 : versionLabel.hashCode());
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
		VersionAnnotatorSpec other = (VersionAnnotatorSpec) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (expressionLanguage == null) {
			if (other.expressionLanguage != null)
				return false;
		} else if (!expressionLanguage.equals(other.expressionLanguage))
			return false;
		if (metamodelURI == null) {
			if (other.metamodelURI != null)
				return false;
		} else if (!metamodelURI.equals(other.metamodelURI))
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		if (versionLabel == null) {
			if (other.versionLabel != null)
				return false;
		} else if (!versionLabel.equals(other.versionLabel))
			return false;
		return true;
	}
	
}
