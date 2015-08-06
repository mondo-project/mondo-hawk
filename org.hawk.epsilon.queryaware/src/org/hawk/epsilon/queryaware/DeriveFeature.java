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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.epsilon.common.parse.problem.ParseProblem;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.labs.effectivemetamodel.impl.EffectiveType;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.epsilon.emc.AbstractEpsilonModel;

public class DeriveFeature {

	public DeriveFeature() throws Exception {
	}

	public Object deriveFeature(HashMap<String, EolModule> cashedModules,
			IGraphDatabase g, IGraphNode n,
			QueryAwareEOLQueryEngine containerModel, String propertyName,
			String EOLScript) throws Exception {

		// remove prefix (_NYD)
		String actualEOLScript = EOLScript.startsWith("_NYD##") ? EOLScript
				.substring(6) : EOLScript;

		actualEOLScript = "return " + actualEOLScript + ";";

		EolModule currentModule = null;

		Map<String, EffectiveType> etm = null;

		// FIXMEdone currently containerModel == null (from: Neo4JMonitorMInsert
		// :
		// resolveDerivedAttributeProxies), maybe thats a problem.
		try {

			// not already parsed
			if (!cashedModules.containsKey(actualEOLScript)) {
				// if (cashedModules == null) {
				// bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true'))
				System.err.println("adding new module to cashe, key:"
						+ actualEOLScript);
				currentModule = initModule(g, containerModel);

				currentModule.parse(actualEOLScript);

				etm = new QueryAnalysis().analyze(currentModule);

				for (ParseProblem p : currentModule.getParseProblems())
					System.err.println("PARSE PROBLEM: " + p);

				cashedModules.put(actualEOLScript, currentModule);

			} else {

				// already parsed
				currentModule = cashedModules.get(actualEOLScript);

			}

			IGraphNode node = n.getIncoming().iterator().next().getStartNode();

			QueryAwareGraphNodeWrapper gnw = new QueryAwareGraphNodeWrapper(
					node.getId().toString(), containerModel,
					new QueryAnalysis().computeAttributesToBeCached(etm, node),
					new QueryAnalysis().computeReferencesToBeCached(etm, node));

			currentModule.getContext().getFrameStack()
					.put(Variable.createReadOnlyVariable("self", gnw));

			((QueryAwareGraphPropertyGetter) containerModel.getPropertyGetter())
					.getAccessListener().setSourceObject(n.getId() + "");

			// System.out.println("-\n" + actualEOLScript + "\n-");
			// printAST(module.getAst());
			// System.out.println("-");
			Object ret = null;
			try {
				ret = currentModule.execute();
			} catch (Exception e) {
				System.err
						.println("----------------\nerror in derive feature on: "
								+ n
								+ "\n"
								+ n.getPropertyKeys()
								+ "\n"
								+ containerModel
								+ "\n"
								+ EOLScript
								+ "\n------------\nreturning null as value\n");
				e.printStackTrace();
			}

			// System.out.println(ret.getClass());
			// System.out.println(ret);

			if (!(ret instanceof Collection<?>)) {
				if (ret instanceof Boolean || ret instanceof String
						|| ret instanceof Integer || ret instanceof Float
						|| ret instanceof Double)
					return ret;
				else
					return ret.toString();
			} else {
				// TODO handle collections as returns to derived features --
				// need to type cast them for storage in the db
				System.err
						.println("Derivefeature got a collection back from EOL, this is not supported yet! returning \"HAWK_ERROR\"");
				return "HAWK_ERROR";
			}

		} catch (Exception e) {
			System.err.println("ERROR IN DERIVING ATTRIBUTE:");
			e.printStackTrace();
		}

		// System.out.println("DERIVATION_ERROR");
		return "DERIVATION_ERROR";
		// ...parse like:
		// this.bodyDeclarations.exists(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true'))

	}

	private EolModule initModule(IGraphDatabase g,
			QueryAwareEOLQueryEngine model) throws Exception {

		EolModule currentModule = new EolModule();

		StringProperties configuration = new StringProperties();

		long x = Runtime.getRuntime().maxMemory() / 1000000 / 60;
		// configuration.put("DUMP_DATABASE_CONFIG_ON_EXIT", true);
		// configuration.put("DUMP_MODEL_CONFIG_ON_EXIT", true);
		// configuration.put("DUMP_FULL_DATABASE_CONFIG_ON_EXIT", true);
		configuration.put(AbstractEpsilonModel.databaseLocation, g.getPath());
		configuration.put("name", "Model");
		configuration.put(AbstractEpsilonModel.enableCaching, true);

		configuration.put("neostore.nodestore.db.mapped_memory", 3 * x + "M");
		configuration.put("neostore.relationshipstore.db.mapped_memory", 14 * x
				+ "M");
		configuration.put("neostore.propertystore.db.mapped_memory", x + "M");
		configuration.put("neostore.propertystore.db.strings.mapped_memory", 2
				* x + "M");
		configuration.put("neostore.propertystore.db.arrays.mapped_memory", x
				+ "M");

		model.setDatabaseConfig(configuration);

		model.load(g);

		currentModule.getContext().getModelRepository().addModel(model);

		// run("Model.find(td:TypeDeclaration|td.bodyDeclarations.select(md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public='true') and md.modifiers.exists(mod:Modifier|mod.static='true') and md.returnType.isTypeOf(SimpleType) and md.returnType.name.fullyQualifiedName = td.name.fullyQualifiedName) ).size().println();");

		// run("Model.find(td:TypeDeclaration|td.bodyDeclarations.select(" +
		// "md:MethodDeclaration|md.modifiers.exists(mod:Modifier|mod.public=='true') "
		// +
		// "and md.modifiers.exists(mod:Modifier|mod.static=='true') " +
		// "and md.returnType.isTypeOf(SimpleType) " +
		// "and md.returnType.name.fullyQualifiedNam

		return currentModule;

	}

}
