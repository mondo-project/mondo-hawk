/*******************************************************************************
 * Copyright (c) 2011-2016 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - code cleanup
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
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.osgiserver.HModel;
import org.hawk.ui2.util.HUIManager;

public class HawkModel extends ModelReference {

	public static String PROPERTY_INDEXER_NAME = "databaseName";

	private IGraphDatabase database;
	private IGraphTransaction t;

	public HawkModel() {
		super(new JavaModel(Collections.emptyList(), new ArrayList<Class<?>>()));
	}

	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver)
			throws EolModelLoadingException {

		this.name = properties.getProperty(Model.PROPERTY_NAME);
		final String aliasString = properties.getProperty(Model.PROPERTY_ALIASES);
		if (aliasString != null && aliasString.trim().length() > 0) {
			for (String elem : aliasString.split(",")) {
				this.aliases.add(elem.trim());
			}
		}

		final EOLQueryEngine eolQueryEngine = new EOLQueryEngine();

		final String namespaces = (properties.getProperty(IQueryEngine.PROPERTY_DEFAULTNAMESPACES) + "").trim();
		if (namespaces.length() > 0) {
			eolQueryEngine.setDefaultNamespaces(namespaces);
		}
		this.target = eolQueryEngine;

		final HUIManager m = HUIManager.getInstance();
		final String hn = properties.getProperty(PROPERTY_INDEXER_NAME);
		if (hn == null) {
			throw new EolModelLoadingException(
					new Exception(
						"The launch configuration lacks the name of the Hawk instance (PROPERTY_INDEXER_NAME)"),
					this);
		}

		final HModel hawkModel = m.getHawkByName(hn);
		if (hawkModel == null) {
			throw new EolModelLoadingException(new Exception("The selected Hawk (" + hn + ") cannot be found"), this);
		}

		final HawkState s = hawkModel.getStatus();
		switch (s) {
		case UPDATING:
			throw new EolModelLoadingException(new Exception(
				"The selected Hawk cannot be currently queried as it is updating, please try again later"), this);
		case STOPPED:
			throw new EolModelLoadingException(new Exception(
				"The selected Hawk cannot be currently queried as it is stopped, please start it first"), this);
		case RUNNING:
			// nothing to do
			break;
		default:
			throw new EolModelLoadingException(new Exception(
				String.format("The selected Hawk cannot be currently queried (state=%s)", s)), this);
		}

		database = hawkModel.getGraph();
		if (database != null) {
			try {
				t = database.beginTransaction();
			} catch (Exception e) {
				throw new EolModelLoadingException(
						new Exception(
								"The selected Hawk cannot connect to its back-end (transaction error)"),
						this);
			}
			eolQueryEngine.load(hawkModel.getIndexer());
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
}
