/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlReference;

public class ModelioModelResource implements IHawkModelResource {

	private final ModelioMetaModelResource metamodel;
	private final List<ExmlObject> exmls;
	private Set<IHawkObject> contents;

	public ModelioModelResource(ModelioMetaModelResource metamodel, ExmlObject exml) {
		this.metamodel = metamodel;
		this.exmls = Collections.singletonList(exml);
	}

	public ModelioModelResource(ModelioMetaModelResource metamodel, Iterable<ExmlObject> objects) {
		this.metamodel = metamodel;
		this.exmls = new ArrayList<>();
		for (ExmlObject o : objects) {
			exmls.add(o);
		}
	}

	@Override
	public void unload() {
		exmls.clear();
		contents = null;
	}

	@Override
	public String getType() {
		return getClass().getName();
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		if (contents == null) {
			contents = new HashSet<>();
			for (ExmlObject exml : exmls) {
				addObjectToContents(exml);
			}
		}
		return contents;
	}

	private void addObjectToContents(ExmlObject exml) {
		contents.add(new ModelioObject(metamodel, exml));
		for (List<ExmlReference> composition : exml.getCompositions().values()) {
			for (ExmlReference r : composition) {
				if (r instanceof ExmlObject) {
					addObjectToContents((ExmlObject)r);
				}
			}
		}
	}
}
