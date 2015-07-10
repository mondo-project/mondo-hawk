/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 *     Ossmeter team (https://opensourceprojects.eu/p/ossmeter) - SVN delta computation algorithm
 ******************************************************************************/
package org.hawk.svn;

import org.hawk.core.IVcsManager;
import org.hawk.core.VcsRepository;
import org.hawk.core.VcsRepositoryDelta;


public abstract class AbstractVcsManager implements IVcsManager {
	
	@Override
	public VcsRepositoryDelta getDelta(VcsRepository repository, String startRevision) throws Exception {
		return getDelta(repository, startRevision, getCurrentRevision(repository));
	}

	// public VcsRepositoryDelta getDelta(VcsRepository repository, String
	// startRevision,
	// String endRevision) throws Exception {
	// // TODOdeleted Auto-generated method stub
	// return null;
	// }
	
}
