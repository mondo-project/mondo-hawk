/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.queryaware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.analysis.optimisation.loading.impl.LoadingOptimisationAnalyser;
import org.eclipse.epsilon.eol.ast2eol.Ast2EolContext;
import org.eclipse.epsilon.eol.metamodel.EolElement;
import org.eclipse.epsilon.eol.visitor.resolution.type.tier1.impl.TypeResolver;
import org.eclipse.epsilon.eol.visitor.resolution.variable.impl.VariableResolver;
import org.eclipse.epsilon.labs.effectivemetamodel.impl.EffectiveFeature;
import org.eclipse.epsilon.labs.effectivemetamodel.impl.EffectiveMetamodel;
import org.eclipse.epsilon.labs.effectivemetamodel.impl.EffectiveType;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphEdge;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.ModelElementNode;

public class QueryAnalysis {

	public Map<String, EffectiveType> analyze(EolModule module) {

		Ast2EolContext astContext = new Ast2EolContext();
		EolElement dom = astContext.getEolElementCreatorFactory()
				.createDomElement(module.getAst(), null, astContext);

		VariableResolver vr = new VariableResolver();
		vr.run(dom);

		TypeResolver tr = new TypeResolver();
		tr.getTypeResolutionContext().setModule(module);
		tr.run(dom);

		LoadingOptimisationAnalyser loa = new LoadingOptimisationAnalyser();
		loa.run(dom);

		Collection<EffectiveMetamodel> col = loa.getTypeResolutionContext()
				.getEffectiveMetamodels();

		Map<String, EffectiveType> emm = new HashMap<>();

		for (EffectiveMetamodel em : col) {

			for (EffectiveType et : em.getAllOfType()) {

				emm.put(em.getNsuri() + "#" + et.getName(), et);

			}

			for (EffectiveType et : em.getAllOfKind()) {

				emm.put(em.getNsuri() + "#" + et.getName(), et);

			}

			for (EffectiveType et : em.getTypes()) {

				emm.put(em.getNsuri() + "#" + et.getName(), et);

			}

		}

		return emm;

	}

	private EffectiveType getEffectiveType(
			Map<String, EffectiveType> effectiveTypes, IGraphNode node) {
		IGraphNode typeNode = node.getOutgoingWithType(ModelElementNode.EDGE_LABEL_OFTYPE).iterator()
				.next().getEndNode();
		String typeName = (String) typeNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);
		IGraphNode packageNode = typeNode.getOutgoingWithType("epackage")
				.iterator().next().getEndNode();
		String packageURI = (String) packageNode.getProperty(IModelIndexer.IDENTIFIER_PROPERTY);

		EffectiveType et = effectiveTypes.get(packageURI + "#" + typeName);
		return et;
	}

	public Map<String, Object> computeAttributesToBeCached(
			Map<String, EffectiveType> effectiveTypes, IGraphNode node) {

		if (effectiveTypes.size() != 0) {

			EffectiveType et = getEffectiveType(effectiveTypes, node);

			Map<String, Object> properties = new HashMap<>();

			for (EffectiveFeature ef : et.getAttributes()) {
				properties.put(ef.getName(), node.getProperty(ef.getName()));
			}

			return properties;

		}

		else
			return new HashMap<String, Object>();
	}

	public Map<String, Object> computeReferencesToBeCached(
			Map<String, EffectiveType> effectiveTypes, IGraphNode node) {

		if (effectiveTypes.size() != 0) {

			EffectiveType et = getEffectiveType(effectiveTypes, node);

			Map<String, Object> properties = new HashMap<>();

			for (EffectiveFeature ef : et.getReferences()) {

				final List<String> ids = new ArrayList<>();

				for (IGraphEdge e : node.getOutgoingWithType(ef.getName())) {

					IGraphNode n = e.getEndNode();
					String id = (String) n.getId().toString();

					ids.add(id);

				}
				properties.put(ef.getName(), ids);

			}

			return properties;
		} else
			return new HashMap<String, Object>();

	}

}
