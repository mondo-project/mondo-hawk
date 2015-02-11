/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

	private static BundleContext context;
	private static Activator instance;
	//private static ModelIndexer indexer;

	public static Plugin getInstance(){		
		return instance;		
	}
	
	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		context = bundleContext;
		instance = this;
		//instantiate a test indexer:
		//indexer = new ModelIndexer("neodb", "neo4j", "https://svn.cs.york.ac.uk/svn/sosym/kb_temp_svn_test_folder", "svn",un,pw);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		Activator.context = null;
		instance = null;
		//indexer.shutdown();
		//indexer = null;
		//System.err.println("ended");
	}

	public static Activator getDefault() {
		return instance;
	}

}
