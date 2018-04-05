/*******************************************************************************
 * Copyright (c) 2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 *     Orjuwan Al-Wadeai - Changes to Integrate Modelio Metamodel 3.6
 ******************************************************************************/
package org.hawk.modelio.exml.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;
import org.hawk.modelio.exml.metamodel.ModelioClass;
import org.hawk.modelio.exml.metamodel.register.MetamodelRegister;
import org.hawk.modelio.exml.model.parser.ExmlObject;
import org.hawk.modelio.exml.model.parser.ExmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelioModelResource implements IHawkModelResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelioModelResource.class);
	private Iterable<ExmlObject> exmls;
	private Set<IHawkObject> contents;
	private IModelResourceFactory parser;

	public ModelioModelResource(ExmlObject exml, IModelResourceFactory p) {
		parser = p;
		this.exmls = Collections.singletonList(exml);
	}

	public ModelioModelResource(Iterable<ExmlObject> objects, IModelResourceFactory p) {
		parser = p;
		this.exmls = objects;
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
	public Iterable<IHawkObject> getAllContents() {
		return new Iterable<IHawkObject>() {
			@Override
			public Iterator<IHawkObject> iterator() {
				// Iterator through all the model fragments - makes it
				// possible to have only one fragment in memory at once,
				// but requires reading it again on every iteration.
				final Iterator<ExmlObject> itExmls = exmls.iterator();
				final List<IHawkObject> currentFragment = new LinkedList<>();

				return new Iterator<IHawkObject>() {
					@Override
					public boolean hasNext() {
						while (currentFragment.isEmpty() && itExmls.hasNext()) {
							ExmlObject nextRoot = itExmls.next();
							addObjectToContents(nextRoot, currentFragment);
						}
						return !currentFragment.isEmpty();
					}

					@Override
					public IHawkObject next() {
						return currentFragment.remove(0);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public Set<IHawkObject> getAllContentsSet() {
		if (contents == null) {
			contents = new HashSet<>();
			for (ExmlObject exml : exmls) {
				addObjectToContents(exml, contents);
			}
		}
		return contents;
	}

	private void addObjectToContents(ExmlObject exml, Collection<IHawkObject> contents) {

		Map<String, String> mmversions = ((ModelioModelResourceFactory) this.parser).getMmPackageVersions();
		ModelioClass mc = MetamodelRegister.INSTANCE.getModelioClass(exml.getMClassName(), mmversions);
		
		if (mc == null) {
			LOGGER.warn("Could not find class '{}', skipping", exml.getMClassName());
		} else {
			contents.add(new ModelioObject(mc, exml, mmversions));
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
						addObjectToContents(exmlObject, contents);
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
