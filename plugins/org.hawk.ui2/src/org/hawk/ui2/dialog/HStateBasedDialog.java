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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.ui2.dialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hawk.core.IStateListener;
import org.hawk.osgiserver.HModel;

public abstract class HStateBasedDialog extends TitleAreaDialog implements IStateListener {

	protected HModel hawkModel;

	public HStateBasedDialog(HModel hawkModel, Shell parentShell) {
		super(parentShell);
		this.hawkModel = hawkModel;
		hawkModel.getHawk().getModelIndexer().addStateListener(this);
	}

	@Override
	public boolean close() {
		hawkModel.getHawk().getModelIndexer().removeStateListener(this);
		return super.close();
	}

	protected boolean enableIfRunning(final HawkState s) {
		final boolean ret = s == HawkState.RUNNING;

		Shell shell = getShell();
		if (shell != null) {
			Display display = shell.getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						if (!ret) {
							setErrorMessage(String.format(
								"The index is %s - cannot confirm changes",
								s.toString().toLowerCase()));
						} else {
							setErrorMessage(null);
						}
						getButton(IDialogConstants.OK_ID).setEnabled(ret);
					}
				});
			}
		}
	
		return ret;
	}

	@Override
	public void state(HawkState state) {
		enableIfRunning(state);
	}

	@Override
	public void info(String s) {
		// nothing
	}

	@Override
	public void error(String s) {
		// nothing
	}

	@Override
	public void removed() {
		// nothing
	}

	
}