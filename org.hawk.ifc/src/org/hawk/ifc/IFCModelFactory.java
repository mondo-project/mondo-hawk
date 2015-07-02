package org.hawk.ifc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;



import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;

public class IFCModelFactory implements IModelResourceFactory {

	String type = "uk.ac.york.cs.mde.hawk.ifc.model.IFCModelFactory";
	String metamodeltype = "com.googlecode.hawk.emf.metamodel.EMFMetaModelParser";
	HashSet<String> modelExtensions;

	public IFCModelFactory() {
		modelExtensions = new HashSet<String>();

		modelExtensions.add("ifc");
		modelExtensions.add("ifcxml");

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

		return new IFCModelResource(f, this);

		// FIXME possibly keep metadata about failure to aid users

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
	public boolean canParse(File f) {
		boolean parseable = false;
		
		String[] split = f.getPath().split("\\.");
		String ext = split[split.length - 1];
		if(getModelExtensions().contains(ext.toLowerCase())){
			FileReader namereader;
			try {
				namereader = new FileReader(f);
				BufferedReader in = new BufferedReader(namereader);
				String s = in.readLine();
				//IFC part 12 TXT
				if(s.toLowerCase().contains("iso-10303-21")){
					parseable=true;
					
					//FIXME: test this code
					for (int i=0;i<3;i++)
						in.readLine();
					//line #5 == FILE_SCHEMA(('IFC2X3'));
					s = in.readLine();
					if(s.toLowerCase().equalsIgnoreCase("FILE_SCHEMA(('IFC2X3'));")){
						parseable=true;						
					}
				} else{
					s = in.readLine();
					//IFC part 28 xml 
					if(s.toLowerCase().contains("urn:iso.org:standard:10303:part(28)"))
						parseable=true;
				}
				in.close();
				namereader.close();
			} catch (IOException e) {
				System.err.println("file can not be read to check parseability");
			} 
			
		}

		return parseable;

	}

	
	public String getMetaModelType() {
		return metamodeltype;
	}

	@Override
	public String getHumanReadableName() {
		return "BIM IFC 2x3 Parser for Hawk";
	}


}
