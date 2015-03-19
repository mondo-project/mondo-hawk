package org.hawk.ui.emc.dt2;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.eol.models.ModelReference;
import org.eclipse.epsilon.eol.models.java.JavaModel;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.epsilon.emc.CEOLQueryEngine;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.ui2.util.HManager;

public class HawkModel extends ModelReference {

	public static String PROPERTY_INDEXER_NAME = "databaseName";
	public static String PROPERTY_FILE_INCLUSION_PATTERN = "FILE";
	protected IGraphDatabase database = null;

	public HawkModel() {
		super(new JavaModel(Collections.emptyList(), new ArrayList<Class<?>>()));
	}

	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver)
			throws EolModelLoadingException {

		this.name = properties.getProperty(Model.PROPERTY_NAME);

		EOLQueryEngine eolQueryEngine;

		String fip = properties.getProperty(PROPERTY_FILE_INCLUSION_PATTERN);

		// System.err.println(fip);

		if (fip.equals(""))
			eolQueryEngine = new EOLQueryEngine();
		else
			eolQueryEngine = new CEOLQueryEngine();

		target = eolQueryEngine;
		eolQueryEngine.setDatabaseConfig(properties);

		//
		database = HManager.getGraphByIndexerName(properties
				.getProperty(PROPERTY_INDEXER_NAME));

		String[] aliases = properties.getProperty("aliases").split(",");
		for (int i = 0; i < aliases.length; i++) {
			this.aliases.add(aliases[i].trim());
		}

		// String location = properties.getProperty(PROPERTY_DATABASE_LOCATION);
		//
		// System.out.println("Location: " + location);
		//
		// String name = location.substring(location.lastIndexOf("/") + 1);
		// String loc = location.substring(0, location.lastIndexOf("/"));
		//
		// System.out.println(name);
		// System.out.println(loc);
		//
		// database.run(name, new File(loc), new DefaultConsole());
		if (database != null)
			eolQueryEngine.load(database);
		else
			throw new EolModelLoadingException(
					new Exception(
							"The selected Hawk cannot connect to its back-end, are you sure it is not paused?"),
					this);
	}

	// @Override
	// public void dispose() {
	// System.out.println("disposed connection to db");
	// super.dispose();
	// try {
	// database.shutdown(false);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
