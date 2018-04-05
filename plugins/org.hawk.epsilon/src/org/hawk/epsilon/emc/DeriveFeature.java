/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
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
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.internal.updater.DirtyDerivedAttributesListener;
import org.hawk.graph.internal.util.GraphUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeriveFeature {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeriveFeature.class);

	public Object deriveFeature(Map<String, EolModule> cachedModules, IModelIndexer indexer, IGraphNode n,
			EOLQueryEngine containerModel, String propertyName, String EOLScript) throws Exception {

		// remove prefix (_NYD)
		final String actualEOLScript = EOLScript.startsWith(DirtyDerivedAttributesListener.NOT_YET_DERIVED_PREFIX)
				? EOLScript.substring(DirtyDerivedAttributesListener.NOT_YET_DERIVED_PREFIX.length())
				: EOLScript;

		EolModule currentModule = null;

		// FIXMEdone currently containerModel == null (from: Neo4JMonitorMInsert
		// :
		// resolveDerivedAttributeProxies), maybe thats a problem.
		try {

			// not already parsed
			if (!cachedModules.containsKey(actualEOLScript)) {
				// if (cashedModules == null) {
				// bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true'))
				LOGGER.debug("adding new module to cache, key: {}",
					actualEOLScript.length() > 100
						? actualEOLScript.substring(0, 100) + "\n[! long script, snipped !]"
						: actualEOLScript
				);
				currentModule = initModule(indexer, containerModel);

				currentModule.parse(actualEOLScript);

				List<ParseProblem> pps = currentModule.getParseProblems();
				for (ParseProblem p : pps) {
					LOGGER.error("Parsing problem: {}", p);
				}

				if (pps.size() > 0) {
					LOGGER.error("There were parse problems, returning \"DERIVATION_PARSE_ERROR\" as value\n");
					return "DERIVATION_PARSE_ERROR";
				} else {
					cachedModules.put(actualEOLScript, currentModule);
				}

			} else {
				// already parsed
				currentModule = cachedModules.get(actualEOLScript);
			}

			GraphNodeWrapper gnw = new GraphNodeWrapper(n.getIncoming().iterator().next().getStartNode(),
					containerModel);

			currentModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("self", gnw));

			((GraphPropertyGetter) containerModel.getPropertyGetter()).getAccessListener()
					.setSourceObject(n.getId() + "");

			Object ret = null;
			try {

				ret = currentModule.execute();

			} catch (Exception e) {
				LOGGER.error("error in derive feature on {}, returning derivation execution error", n.getId());
				LOGGER.error(e.getMessage(), e);
				return "DERIVATION_EXECUTION_ERROR";
			}

			if (!(ret instanceof Collection<?>)) {
				return toPrimitive(ret);
			} else {

				Collection<Object> collection = null;
				// check for uniqueness
				if (ret instanceof Set<?>)
					collection = new LinkedHashSet<>();
				else
					collection = new LinkedList<>();

				final Collection<?> srcCollection = (Collection<?>) ret;
				Class<?> elemClass = null;
				for (Object o : srcCollection) {
					Object converted = toPrimitive(o);
					if (converted != null) {
						collection.add(converted);
					}
					if (elemClass == null) {
						elemClass = converted.getClass();
					}
				}
				if (elemClass == null) {
					elemClass = String.class;
				}

				Object r = Array.newInstance(elemClass, collection.size());
				return collection.toArray((Object[]) r);
			}

		} catch (Exception e) {
			LOGGER.error("ERROR IN DERIVING ATTRIBUTE, returning \"DERIVATION_OTHER_ERROR\" as value", e);
		}

		return "DERIVATION_OTHER_ERROR";
	}

	protected static Object toPrimitive(Object ret) {
		if (ret == null) {
			return null;
		} else if (ret instanceof Collection<?>) {
			return "Hawk collection error: nested collections are not supported for derived/indexed attributes";
		} else if (GraphUtil.isPrimitiveOrWrapperType(ret.getClass())) {
			return ret;
		} else if (ret instanceof GraphNodeWrapper) {
			return ret;
		} else {
			return ret.toString();
		}
	}

	private EolModule initModule(IModelIndexer m, EOLQueryEngine model) throws Exception {
		EolModule currentModule = new EolModule();
		model.load(m);
		currentModule.getContext().getModelRepository().addModel(model);
		return currentModule;
	}

}
