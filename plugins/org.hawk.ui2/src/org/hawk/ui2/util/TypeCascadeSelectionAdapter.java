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
package org.hawk.ui2.util;

import java.util.List;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.hawk.core.IMetaModelIntrospector;
import org.hawk.osgiserver.HModel;

public class TypeCascadeSelectionAdapter extends SelectionAdapter {
	private final Combo cmbType;
	private final HModel hawkModel;

	public TypeCascadeSelectionAdapter(HModel hawkModel, Combo cmbType) {
		this.hawkModel = hawkModel;
		this.cmbType = cmbType;
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		cmbType.removeAll();

		final String metamodelURI = ((Combo) e.getSource()).getText().trim();
		if (!metamodelURI.isEmpty()) {
			final IMetaModelIntrospector introspector = hawkModel.getIntrospector();
			if (introspector != null) {
				List<String> types = introspector.getTypes(metamodelURI);
				for (String typeName : types) {
					cmbType.add(typeName);
				}
				cmbType.select(0);
			}
		}
	}
}