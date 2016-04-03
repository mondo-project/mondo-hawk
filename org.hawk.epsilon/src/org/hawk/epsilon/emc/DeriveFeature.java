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
package org.hawk.epsilon.emc;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphNode;
import org.hawk.graph.internal.util.GraphUtil;

public class DeriveFeature {

	public final static String REFERENCETARGETPREFIX = "GNW::";

	public DeriveFeature() throws Exception {
	}

	public Object deriveFeature(Map<String, EolModule> cachedModules, IModelIndexer indexer, IGraphNode n,
			EOLQueryEngine containerModel, String propertyName, String EOLScript) throws Exception {

		// remove prefix (_NYD)
		String actualEOLScript = EOLScript.startsWith("_NYD##") ? EOLScript.substring(6) : EOLScript;

		EolModule currentModule = null;

		// FIXMEdone currently containerModel == null (from: Neo4JMonitorMInsert
		// :
		// resolveDerivedAttributeProxies), maybe thats a problem.
		try {

			// not already parsed
			if (!cachedModules.containsKey(actualEOLScript)) {
				// if (cashedModules == null) {
				// bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true'))
				System.err.println("adding new module to cache, key:" + (actualEOLScript.length() > 100
						? actualEOLScript.substring(0, 100) + "\n[! long script, snipped !]" : actualEOLScript));
				currentModule = initModule(indexer, containerModel);

				currentModule.parse(actualEOLScript);

				List<ParseProblem> pps = currentModule.getParseProblems();
				for (ParseProblem p : pps)
					System.err.println("PARSE PROBLEM: " + p);

				if (pps.size() > 0) {
					System.err.println("There were parse problems, returning \"DERIVATION_PARSE_ERROR\" as value\n");
					return "DERIVATION_PARSE_ERROR";
				} else
					cachedModules.put(actualEOLScript, currentModule);

			} else {

				// already parsed
				currentModule = cachedModules.get(actualEOLScript);

			}

			GraphNodeWrapper gnw = new GraphNodeWrapper(n.getIncoming().iterator().next().getStartNode(),
					containerModel);

			currentModule.getContext().getFrameStack().put(Variable.createReadOnlyVariable("self", gnw));

			((GraphPropertyGetter) containerModel.getPropertyGetter()).getAccessListener()
					.setSourceObject(n.getId() + "");

			// System.out.println("-\n" + actualEOLScript + "\n-");
			// printAST(module.getAst());
			// System.out.println("-");
			Object ret = null;
			try {

				ret = currentModule.execute();

			} catch (Exception e) {
				System.err.println("----------------\nerror in derive feature on: " + n.getId()
				// + "\n" + n.getPropertyKeys()
				// + "\n" + containerModel + "\n" + EOLScript
						+ "\n------------\nreturning \"DERIVATION_EXECUTION_ERROR\" as value\n");
				// e.printStackTrace();
				return "DERIVATION_EXECUTION_ERROR";
			}

			// System.out.println(ret.getClass());
			// System.out.println(ret);

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
				boolean primitiveOrWrapperClass = false;
				if (!srcCollection.isEmpty()) {
					final Object first = srcCollection.iterator().next();
					elemClass = first.getClass();
					primitiveOrWrapperClass = new GraphUtil().isPrimitiveOrWrapperType(elemClass);
					for (Object o : srcCollection)
						collection.add(toPrimitive(o));
				}

				Object r = null;
				if (primitiveOrWrapperClass && elemClass != null) {
					r = Array.newInstance(elemClass, collection.size());
				} else {
					r = Array.newInstance(String.class, collection.size());
				}
				return collection.toArray((Object[]) r);

			}

		} catch (Exception e) {
			System.err.println("ERROR IN DERIVING ATTRIBUTE, returning \"DERIVATION_OTHER_ERROR\" as value:");
			e.printStackTrace();
		}

		// System.out.println("DERIVATION_ERROR");
		return "DERIVATION_OTHER_ERROR";
		// ...parse like:
		// this.bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true'))

	}

	protected static Object toPrimitive(Object ret) {
		if (ret instanceof Collection<?>)
			return "Hawk collection error: nested collections are not supported for derived/indexed attributes";
		if (new GraphUtil().isPrimitiveOrWrapperType(ret.getClass()))
			return ret;
		else if (ret instanceof GraphNodeWrapper)
			return REFERENCETARGETPREFIX + ((GraphNodeWrapper) ret).getId();
		else
			return ret.toString();
	}

	private EolModule initModule(IModelIndexer m, EOLQueryEngine model) throws Exception {

		EolModule currentModule = new EolModule();

		StringProperties configuration = new StringProperties();

		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
		// configuration.put("DUMP_DATABASE_CONFIG_ON_EXIT", true);
		// configuration.put("DUMP_MODEL_CONFIG_ON_EXIT", true);
		// configuration.put("DUMP_FULL_DATABASE_CONFIG_ON_EXIT", true);
		configuration.put(AbstractEpsilonModel.databaseLocation, m.getGraph().getPath());
		configuration.put("name", "Model");
		configuration.put(AbstractEpsilonModel.enableCaching, true);

		configuration.put("neostore.nodestore.db.mapped_memory", 3 * x + "M");
		configuration.put("neostore.relationshipstore.db.mapped_memory", 14 * x + "M");
		configuration.put("neostore.propertystore.db.mapped_memory", x + "M");
		configuration.put("neostore.propertystore.db.strings.mapped_memory", 2 * x + "M");
		configuration.put("neostore.propertystore.db.arrays.mapped_memory", x + "M");

		model.setDatabaseConfig(configuration);

		model.load(m);

		currentModule.getContext().getModelRepository().addModel(model);

		// run("Model.find(td:TypeDeclaration|td.bodyDeclarations.select(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public='true')
		// and md.modifiers.exists(mod:Modifier|mod.static='true') and
		// md.returnType.isTypeOf(SimpleType) and
		// md.returnType.name.fullyQualifiedName = td.name.fullyQualifiedName)
		// ).size().println();");

		// run("Model.find(td:TypeDeclaration|td.bodyDeclarations.select(" +
		// "md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true')
		// "
		// +
		// "and md.modifiers.exists(mod:Modifier|mod.static=='true') " +
		// "and md.returnType.isTypeOf(SimpleType) " +
		// "and md.returnType.name.fullyQualifiedNam

		return currentModule;

	}

}
