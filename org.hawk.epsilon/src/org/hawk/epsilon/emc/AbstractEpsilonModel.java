/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.epsilon.emc;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.introspection.IPropertySetter;
import org.eclipse.epsilon.eol.models.Model;
import org.hawk.graph.ModelElementNode;

public abstract class AbstractEpsilonModel extends Model {

	protected HashSet<String> cachedTypes = new HashSet<String>();
	protected StringProperties config = null;

	// TODO try re-enable the use of a cache
	// protected boolean enableCache = true;

	// protected ModelParser parser;

	// protected ModelIndexer hawkContainer;

	/**
	 * Returns all of the contents of the database in the form of lightweight
	 * NoSQLWrapper objects - implemented for each backend
	 */
	@Override
	abstract public Collection<?> allContents();

	/**
	 * Creates a node and inserts it into the database
	 */
	@Override
	abstract public Object createInstance(String metaClassName)
			throws EolModelElementTypeNotFoundException,
			EolNotInstantiableModelElementTypeException;

	/**
	 * Deletes the element from the database
	 */
	@Override
	abstract public void deleteElement(Object arg0) throws EolRuntimeException;

	abstract public Collection<Object> getAllOf(String arg0,
			final String typeorkind)
			throws EolModelElementTypeNotFoundException;

	@Override
	public Collection<Object> getAllOfKind(String arg0)
			throws EolModelElementTypeNotFoundException {

		Collection<Object> ofType = (Collection<Object>) getAllOf(arg0,
				ModelElementNode.EDGE_LABEL_OFTYPE);
		Collection<Object> ofKind = (Collection<Object>) getAllOf(arg0,
				ModelElementNode.EDGE_LABEL_OFKIND);
		ofKind.addAll(ofType);

		return ofKind;
	}

	@Override
	public Collection<Object> getAllOfType(String arg0)
			throws EolModelElementTypeNotFoundException {
		return getAllOf(arg0, ModelElementNode.EDGE_LABEL_OFTYPE);
	}

	@Override
	abstract public Object getElementById(String arg0);

	@Override
	abstract public String getElementId(Object arg0);

	@Override
	public Object getEnumerationValue(String arg0, String arg1)
			throws EolEnumerationValueNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	abstract public String getTypeNameOf(Object arg0);

	@Override
	abstract public Object getTypeOf(Object arg0);

	@Override
	abstract public boolean hasType(String arg0);

	@Override
	public boolean isInstantiable(String arg0) {

		System.err
				.println("isInstantiable called on a hawk model, this is not supported, returning false");
		return false;

	}

	@Override
	abstract public boolean isModelElement(Object arg0);

	@Override
	abstract public void load() throws EolModelLoadingException;

	/**
	 * The full path of the database using '/' as separators (if the database
	 * folder does not exist a new one will be created at that location)
	 */
	public final static String databaseLocation = "DATABASE_LOCATION";
	public final static String enableCaching = "ENABLE_CACHING";
	public final static String dumpModelConfig = "DUMP_MODEL_CONFIG_ON_EXIT";
	public final static String dumpDatabaseConfig = "DUMP_FULL_DATABASE_CONFIG_ON_EXIT";

	@Override
	public void load(StringProperties properties, String basePath)
			throws EolModelLoadingException {

		super.load(properties, basePath);

		setDatabaseConfig(properties);

		load();

	}

	@Override
	public void dispose() {
		String dump1 = (String) config.get(dumpDatabaseConfig);
		String dump2 = (String) config.get(dumpModelConfig);
		if (dump1 != null && dump1.equalsIgnoreCase("true") || dump2 != null
				&& dump2.equalsIgnoreCase("true"))
			System.out.println("\n--dumping configuration--");
		if (dump1 != null && dump1.equalsIgnoreCase("true"))
			dumpDatabaseConfig();
		if (dump2 != null && dump2.equalsIgnoreCase("true"))
			dumpModelConfig();
		if (dump1 != null && dump1.equalsIgnoreCase("true") || dump2 != null
				&& dump2.equalsIgnoreCase("true"))
			System.out.println("----------\n");
		super.dispose();
		// System.err.println(types);
	}

	@Override
	abstract public boolean owns(Object arg0);

	@Override
	public void setElementId(Object arg0, String arg1) {
		System.err
				.println("This impelementation of IModel does not allow for ElementId to be changed after creation, hence this method does nothing");
	}

	@Override
	public boolean store() {
		// current implementation stores on create so this method is
		// deprecated - maybe change to store in memory and only flush on method
		// call
		// throw new UnsupportedOperationException();
		return true;
	}

	@Override
	public boolean store(String arg0) {
		// current implementation stores on create so this method is
		// deprecated - maybe change to store in memory and only flush on method
		// call
		// throw new UnsupportedOperationException();
		return true;
	}

	abstract public boolean isOf(Object instance, String metaClass,
			final String typeorkind)
			throws EolModelElementTypeNotFoundException;

	@Override
	public boolean isOfKind(Object instance, String metaClass)
			throws EolModelElementTypeNotFoundException {

		return isOf(instance, metaClass, ModelElementNode.EDGE_LABEL_OFKIND)
				|| isOf(instance, metaClass, ModelElementNode.EDGE_LABEL_OFTYPE);

	}

	@Override
	public boolean isOfType(Object instance, String metaClass)
			throws EolModelElementTypeNotFoundException {

		return isOf(instance, metaClass, ModelElementNode.EDGE_LABEL_OFTYPE);

	}

	public void setDatabaseConfig(StringProperties configuration) {
		this.config = configuration;
	}

	public StringProperties getDatabaseConfig() {
		if (config == null) {
			System.err
					.println("warning: null properties used for loading, defaulting to model named: \"Model\"");
			config = getDefaultDatabaseConfig();
		}
		return config;
	}

	abstract protected StringProperties getDefaultDatabaseConfig();

	@Override
	public abstract boolean knowsAboutProperty(Object instance, String property);

	@Override
	abstract public IPropertyGetter getPropertyGetter();

	@Override
	abstract public IPropertySetter getPropertySetter();

	abstract public Object getBackend();

	public void dumpModelConfig() {

		for (Object c : config.keySet())
			System.out.println(c + " = " + config.get(c));

	}

	abstract public void dumpDatabaseConfig();
	
}
