package org.hawk.ifc;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

import nl.tue.buildingsmart.emf.SchemaLoader;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.ifc.step.deserializer.IfcStepDeserializer;
import org.bimserver.ifc.xml.deserializer.IfcXmlDeserializer;
import org.bimserver.plugins.deserializers.Deserializer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.model.IHawkModelResource;
import org.hawk.core.model.IHawkObject;

public class IFCModelResource implements IHawkModelResource {

	File ifc;
	private IModelResourceFactory parser;

	@Override
	public void unload() {
		ifc = null;
	}
	
	public IFCModelResource(File f, IModelResourceFactory p) {
		parser = p;
		ifc = f;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public HashSet<IHawkObject> getAllContentsSet() {

		HashSet<IHawkObject> allElements = new HashSet<IHawkObject>();
				
		Deserializer d = new IfcStepDeserializer();
		
		String[] split = ifc.getPath().split("\\.");
		String ext = split[split.length - 1];

		if (ext.toLowerCase().equals("xml")){
			d = new IfcXmlDeserializer();
		}
		
		URL schemaURL = IFCModelResource.class.getClassLoader().getResource("/org/hawk/ifc/schema/IFC2X3_TC1.exp");
		if (schemaURL != null) {		
			d.init(SchemaLoader.loadSchema(schemaURL));
		} else {
			System.err.println("IFC schema file IFC2X3_TC1.exp cannot be read");
		}

		
		try {
			IfcModelInterface s = d.read(ifc);
			Iterator<IdEObject> it = s.getValues().iterator();
			while (it.hasNext()) {
				allElements.add(new IFCObject(it.next()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return allElements;
	}

	@Override
	public String getType() {
		return parser.getType();
	}

	@Override
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}
}
