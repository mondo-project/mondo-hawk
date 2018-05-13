/*******************************************************************************
 * Copyright (c) 2018 Aston University.
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
package org.hawk.workspace;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

/**
 * Variant of {@link Workspace} which uses local history for each file rather
 * than {@link IFile#getModificationStamp()}. The IFile modification timestamps
 * are apparently incremented whenever you request a full build, while local
 * history is only updated with meaningful changes from Eclipse editors.
 */
public class LocalHistoryWorkspace extends Workspace {

	@Override
	protected long getModificationTimestamp(IFile f) throws CoreException {
		/*
		 * These are more expensive, but these don't get incremented automatically on a
		 * full build even if they didn't change.
		 */
		final IFileState[] pastStates = f.getHistory(null);
		final long latestRev = pastStates.length > 0 ? pastStates[0].getModificationTime() : 0;
		return latestRev;
	}

	@Override
	public String getHumanReadableName() {
		return "Workspace driver - local history based";
	}
	
}
