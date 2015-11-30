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
package org.hawk.ui.emc.dt2;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.eol.models.ModelReference;
import org.eclipse.epsilon.eol.models.java.JavaModel;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.ui2.util.HUIManager;

public class HawkModel extends ModelReference {

	public static String PROPERTY_INDEXER_NAME = "databaseName";
	protected IGraphDatabase database = null;
	IGraphTransaction t;

	public HawkModel() {
		super(new JavaModel(Collections.emptyList(), new ArrayList<Class<?>>()));
	}

	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver)
			throws EolModelLoadingException {

		this.name = properties.getProperty(Model.PROPERTY_NAME);

		EOLQueryEngine eolQueryEngine;

		String rip = properties
				.getProperty(IQueryEngine.PROPERTY_REPOSITORYCONTEXT);

		String fip = properties.getProperty(IQueryEngine.PROPERTY_FILECONTEXT);

		// System.err.println(rip);
		// System.err.println(fip);

		if (rip != null && fip != null && rip.equals("") && fip.equals(""))
			eolQueryEngine = new EOLQueryEngine();
		else
			eolQueryEngine = new CEOLQueryEngine();

		String namespaces = properties
				.getProperty(HawkModelConfigurationDialog.PROPERTY_DEFAULTNAMESPACES);

		if (namespaces != null && !namespaces.equals(""))
			eolQueryEngine.setDefaultNamespaces(namespaces);

		target = eolQueryEngine;
		eolQueryEngine.setDatabaseConfig(properties);

		//
		database = HUIManager.getInstance().getGraphByIndexerName(
				properties.getProperty(PROPERTY_INDEXER_NAME));

		String[] aliases = properties.getProperty("aliases").split(",");
		for (int i = 0; i < aliases.length; i++) {
			this.aliases.add(aliases[i].trim());
		}

		// String location = properties.getProperty(PROPERTY_DATABASE_LOCATION);
		//
		// System.out.println("Location: " + location);
		//
		// String name = location.substring(location.lastIndexOf("/") + 1);
		// String loc = location.substring(0, location.lastIndexOf("/"));
		//
		// System.out.println(name);
		// System.out.println(loc);
		//
		// database.run(name, new File(loc), new DefaultConsole());
		if (database != null) {
			try {
				t = database.beginTransaction();
			} catch (Exception e) {
				throw new EolModelLoadingException(
						new Exception(
								"The selected Hawk cannot connect to its back-end (transaction error)"),
						this);
			}
			eolQueryEngine.load(database);
		} else
			throw new EolModelLoadingException(
					new Exception(
							"The selected Hawk cannot connect to its back-end, are you sure it is not stopped?"),
					this);
	}

	@Override
	public void dispose() {
		if (t != null) {
			t.success();
			t.close();
		}
		super.dispose();
	}

	// @Override
	// public void dispose() {
	// System.out.println("disposed connection to db");
	// super.dispose();
	// try {
	// database.shutdown(false);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
