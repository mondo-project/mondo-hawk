/*******************************************************************************
 * Copyright (c) 2011-2018 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - further improvements, clean up
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.Collection;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.EolInternalException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.Model;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.util.GraphUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHawkModel extends Model {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHawkModel.class);

	public static Object toPrimitive(Object ret) {
		if (ret == null) {
			return null;
		} else if (ret instanceof Collection<?>) {
			return "Hawk collection error: nested collections are not supported for derived/indexed attributes";
		} else if (GraphUtil.isPrimitiveOrWrapperType(ret.getClass())) {
			return ret;
		} else if (ret instanceof GraphNodeWrapper) {
			return ret;
		} else {
			return ret.toString();
		}
	}

	abstract public Collection<Object> getAllOf(String arg0, final String typeorkind)
			throws EolModelElementTypeNotFoundException, EolInternalException;

	abstract public Object getFileOf(Object arg0);

	abstract public Object getFilesOf(Object arg0);

	abstract public Object getBackend();

	@Override
	public Collection<Object> getAllOfKind(String metaclass) throws EolModelElementTypeNotFoundException {
		try {
			Collection<Object> ofType = (Collection<Object>) getAllOf(metaclass, ModelElementNode.EDGE_LABEL_OFTYPE);
			Collection<Object> ofKind = (Collection<Object>) getAllOf(metaclass, ModelElementNode.EDGE_LABEL_OFKIND);
			ofKind.addAll(ofType);
			return ofKind;
		} catch (EolInternalException ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new EolModelElementTypeNotFoundException(this.getName(), metaclass);
		}
	}

	@Override
	public Collection<Object> getAllOfType(String metaclass) throws EolModelElementTypeNotFoundException {
		try {
			return getAllOf(metaclass, ModelElementNode.EDGE_LABEL_OFTYPE);
		} catch (EolInternalException e) {
			LOGGER.error(e.getMessage(), e);
			throw new EolModelElementTypeNotFoundException(this.getName(), metaclass);
		}
	}

	@Override
	public Object getEnumerationValue(String arg0, String arg1) throws EolEnumerationValueNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInstantiable(String arg0) {
		LOGGER.warn("isInstantiable called on a hawk model, this is not supported, returning false");

		return false;
	}

	@Override
	public void load(StringProperties properties, String basePath) throws EolModelLoadingException {
		super.load(properties, basePath);

		name = (String) properties.get(EOLQueryEngine.PROPERTY_NAME);
		if (name == null) {
			name = "Model";
		}

		String aliasString = properties.getProperty(Model.PROPERTY_ALIASES);
		if (aliasString != null && aliasString.trim().length() > 0) {
			for (String alias : aliasString.split(",")) {
				aliases.add(alias.trim());
			}
		}

		load();
	}

	@Override
	public void setElementId(Object arg0, String arg1) {
		LOGGER.warn("This impelementation of IModel does not "
			+ "allow for ElementId to be changed after creation, hence this method does nothing");
	}

	@Override
	public boolean store() {
		// Saving is not supported by this backend - tooling generally does not expect
		// this to fail, so just produce a warning and ignore this request.
		LOGGER.warn("Hawk indices are read-only: ignoring request to store");
		return true;
	}

	@Override
	public boolean store(String arg0) {
		return store();
	}
}
