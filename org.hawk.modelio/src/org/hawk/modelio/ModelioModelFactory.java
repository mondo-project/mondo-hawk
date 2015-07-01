package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.modelio.gproject.data.project.DefinitionScope;
import org.modelio.gproject.data.project.ProjectDescriptor;
import org.modelio.gproject.data.project.ProjectDescriptorReader;
import org.modelio.gproject.gproject.GProject;
import org.modelio.gproject.gproject.GProjectFactory;
import org.modelio.gproject.module.catalog.FileModuleStore;
import org.modelio.metamodel.data.MetamodelLoader;
import org.modelio.vbasic.auth.NoneAuthData;
import org.modelio.vbasic.progress.NullProgress;

public class ModelioModelFactory implements IModelResourceFactory {

	String type = "uk.ac.york.cs.mde.hawk.ifc.model.ModelioModelFactory";
	String metamodeltype = "com.googlecode.hawk.emf.metamodel.EMFMetaModelParser";
	HashSet<String> modelExtensions;

	public ModelioModelFactory() {
		modelExtensions = new HashSet<String>();

		modelExtensions.add("conf");

	}

	public void init(String t, String t2) {
		type = t;
		metamodeltype = t2;
	}

	@Override
	public String getType() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public IHawkModelResource parse(File f) {

		return new ModelioModelResource(f, this);

	}



	@Override
	public void shutdown() {
		type = null;
		modelExtensions = null;
	}

	@Override
	public HashSet<String> getModelExtensions() {

		return modelExtensions;

	}

	@Override
	public boolean canParse(File f)  {
		boolean parseable = false;
		
		if(!f.canRead() && !f.isFile() && !f.getName().endsWith("conf")){
			return false;
		}
        //Load a Modelio project 
        
        //Start by loading the Modelio Metamodel
        MetamodelLoader.Load();


        ProjectDescriptor pd;
		try { 
			String path = f.getCanonicalPath();
             
			//Load the description of a given project

			Path p = Paths.get(path);
			pd = new ProjectDescriptorReader().read(p, DefinitionScope.LOCAL);
		
			//Load the project
			Path fms = Paths.get("/home/shah/.modelio/3.0/modules");
        	@SuppressWarnings("deprecation")
			GProject gp = GProjectFactory.openProject(pd, new NoneAuthData(), new FileModuleStore(fms), new NullProgress());
			gp.close();
			parseable= true;
		} catch (IOException e) {
			parseable= false;
		}

		return parseable;

	}

	
	public String getMetaModelType() {
		return metamodeltype;
	}

	@Override
	public String getHumanReadableName() {
		// TODO Auto-generated method stub
		return "Modelio parser for Hawk";
	}


}
