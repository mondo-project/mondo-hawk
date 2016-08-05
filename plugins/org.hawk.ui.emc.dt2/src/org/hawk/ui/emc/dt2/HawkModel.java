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
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.query.IQueryEngine;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.osgiserver.HModel;
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
		String aliasString = properties.getProperty(Model.PROPERTY_ALIASES);
		boolean aliasStringIsValid = aliasString != null
				&& aliasString.trim().length() > 0;
		String[] aliasArray = aliasStringIsValid ? aliasString.split(",")
				: new String[0];
		for (int i = 0; i < aliasArray.length; i++) {
			this.aliases.add(aliasArray[i].trim());
		}

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
				.getProperty(IQueryEngine.PROPERTY_DEFAULTNAMESPACES);

		if (namespaces != null && !namespaces.equals(""))
			eolQueryEngine.setDefaultNamespaces(namespaces);

		target = eolQueryEngine;
		eolQueryEngine.setDatabaseConfig(properties);

		HUIManager m = HUIManager.getInstance();

		String hn = properties.getProperty(PROPERTY_INDEXER_NAME);

		if (hn == null)
			throw new EolModelLoadingException(
					new Exception(
							"The selected Hawk has a null name property (PROPERTY_INDEXER_NAME)"),
					this);

		HModel hm = m.getHawkByName(hn);

		if (hm == null)
			throw new EolModelLoadingException(new Exception(
					"The selected Hawk (" + hn
							+ ") cannot be found [HModel == null]"), this);

		database = hm.getGraph();

		HawkState s = hm.getStatus();

		if (s.equals(HawkState.UPDATING))
			throw new EolModelLoadingException(
					new Exception(
							"The selected Hawk cannot be currently queried as it is updating, please try again later"),
					this);
		else if (s.equals(HawkState.STOPPED))
			throw new EolModelLoadingException(
					new Exception(
							"The selected Hawk cannot be currently queried as it is stopped, please start it first"),
					this);
		// catching other new states which may be added
		else if (!s.equals(HawkState.RUNNING))
			throw new EolModelLoadingException(new Exception(
					"The selected Hawk cannot be currently queried (state=" + s
							+ ")"), this);

		if (database != null) {
			try {
				t = database.beginTransaction();
			} catch (Exception e) {
				throw new EolModelLoadingException(
						new Exception(
								"The selected Hawk cannot connect to its back-end (transaction error)"),
						this);
			}
			eolQueryEngine.load(hm.getIndexer());
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
