package test;

import java.io.File;


import nl.tue.buildingsmart.emf.SchemaLoader;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.ifc.step.deserializer.IfcStepDeserializer;
import org.bimserver.ifc.xml.deserializer.IfcXmlDeserializer;
import org.bimserver.plugins.deserializers.DeserializeException;


public class EmbeddedReader2 {
	
	

	/**
	 * @param args
	 * @throws DeserializeException 
	 */
	public static void main(String[] args) throws DeserializeException{
		IfcStepDeserializer d = new IfcStepDeserializer();
		IfcXmlDeserializer e = new IfcXmlDeserializer();
		d.init(SchemaLoader.loadSchema(new File("schema/IFC2X3_TC1.exp")));

		//must check the format (STEP|XML) befor this point
		//use IfcXmlDeserializer for IFC XML
		//IfcStepDeserializer will never return from read if given .ifcxml file
		IfcModelInterface s = d.read(new File("/media/titan-data/experiments-parsers/ifc/ifc-samples/DDS-DuplexHouse_Sanitary_V1.0.ifc/DDS-DuplexHouse_Sanitary_V1.0.ifc.txt"));
		
		int n=0;
		for(IdEObject o: s.getValues()){
			if (n%10000==0)
				System.out.println(o.toString());
			n++;
		}
		

		System.out.println(n+" Number of objects in IFC file");
	}

}
