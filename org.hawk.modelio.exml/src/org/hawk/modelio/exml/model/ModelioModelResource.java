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
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.ModelioMetaModelResource;
import org.hawk.modelio.exml.parser.ExmlObject;
import org.hawk.modelio.exml.parser.ExmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelResource implements IHawkModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelResource.class);
	private final ModelioMetaModelResource metamodel;
	private List<ExmlObject> exmls;
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
		exmls = null;
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
		ModelioClass mc = metamodel.getModelioClass(exml.getMClassName());
		if (mc == null) {
			LOGGER.warn("Could not find class '{}', skipping", exml.getMClassName());
		} else {
			contents.add(new ModelioObject(mc, exml));
			for (Entry<String, List<ExmlReference>> composition : exml.getCompositions().entrySet()) {
				for (ExmlReference r : composition.getValue()) {
					if (r instanceof ExmlObject) {
						final ExmlObject exmlObject = (ExmlObject)r;
						if (exmlObject.getParentUID() == null) {
							// Implicit containment - e.g. TagTypes (don't have explicit PID)
							exmlObject.setParentMClassName(exml.getMClassName());
							exmlObject.setParentName(exml.getName());
							exmlObject.setParentUID(exml.getUID());
						}
						addObjectToContents(exmlObject);
					}
				}
			}
		}
	}

	@Override
	public boolean providesSingletonElements() {
		return true;
	}
}
