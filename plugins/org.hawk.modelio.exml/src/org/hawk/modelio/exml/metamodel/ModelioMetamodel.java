/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - 
 ******************************************************************************/
package org.hawk.modelio.exml.metamodel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hawk.modelio.metamodel.parser.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ModelioMetamodel {
	//private static final Logger LOGGER = LoggerFactory.getLogger(ModelioMetaModelResource.class);

	private List<MPackage> mPackages;
    private MMetamodelDescriptor mDescriptor;    

    private Map<String, MDataType> mBaseTypes;

    private Map<String, MClass> mClass;
	
    public ModelioMetamodel() {
        this.mBaseTypes = new HashMap<>();
        this.mPackages = new ArrayList<MPackage>();
        this.mClass = new HashMap<>();
    }

    
    public ModelioMetamodel(File file) {
        this.mBaseTypes = new HashMap<>();
        this.mPackages = new ArrayList<MPackage>();
        this.mClass = new HashMap<>();
        
		MMetamodelParser parser = new MMetamodelParser();
        
		
		/* Step 1 */
		
		//mDescriptor = parser.parse(file);
		
    	initMetamodel(parser.parse(file));
    }
    

    
    public ModelioMetamodel(MMetamodelDescriptor metamodelDescriptor) {
    	  this.mBaseTypes = new HashMap<>();
          this.mPackages = new ArrayList<MPackage>();
          this.mClass = new HashMap<>();
          
  		/* Step 1 */
  		
      	initMetamodel(metamodelDescriptor);
    
    }
    
    
	public void initMetamodel(MMetamodelDescriptor mDescriptor) {
		this.mDescriptor = mDescriptor;

		
		/* Step 2 */
		
		// add DataTypes & enumerations
		for (Entry<String, MAttributeType> typeEntry : mDescriptor.getDataTypes().entrySet()) {
			MDataType dataType = new MDataType("", typeEntry.getValue().getName(), typeEntry.getValue().getName());
			this.mBaseTypes.put(dataType.getName(), dataType);
		}
		
		for (Entry<String, MFragment> fragmentEntry : mDescriptor.getFragments().entrySet()) {
			MFragment fragment = fragmentEntry.getValue();
			// get enums
			for(Entry<String, MEnumeration> enumEntry : fragment.getEnumerations().entrySet()) {
				MEnum enumType =  new MEnum("", enumEntry.getValue().getName(), "enum", enumEntry.getValue().getValues());
				this.mBaseTypes.put(enumType.getName(), enumType);
			}
		}
		
		/* Step 3 */
		
		// add Classes and Attributes
		for (Entry<String, MFragment> fragmentEntry : mDescriptor.getFragments().entrySet()) {
			MFragment fragment = fragmentEntry.getValue();
			String pkgId = fragment.getProvider()+"."+fragment.getProviderVersion()+"."+fragment.getName()+"."+fragment.getVersion();
			MPackage pkg = new MPackage(pkgId, fragment.getName(), "");
			mPackages.add(pkg);
			
			// get classes
			
			for (Entry<String, MMetaclass> metaclassEntry : fragmentEntry.getValue().getMetaclasses().entrySet()) {
				MMetaclass metaclass = metaclassEntry.getValue();
				String mcId = fragment.getName()+"." + metaclass.getName() + "." + metaclass.getVersion();

				MClass mc = new MClass(mcId, getMClassName(fragment.getName(), metaclass.getName()), "");
				pkg.getMClass().add(mc);
				
				this.mClass.put(mc.getName(), mc);
				//addmClass(fragment, metaclass, mc);
				
				// attributes
				for(MMetaclassAttribute attribute : metaclassEntry.getValue().getAttributes()) {
					MAttribute attr = new MAttribute("", attribute.getName(), "", this.mBaseTypes.get(attribute.getType().getName()), false, true, false);
					mc.getMAttributes().add(attr);
				}
			}
			
			
		}
		
		/* Step 4 */
		
		// add dependencies, supertypes and subtypes
		for (Entry<String, MFragment> fragmentEntry : mDescriptor.getFragments().entrySet()) {
			
			for (Entry<String, MMetaclass> metaclassEntry : fragmentEntry.getValue().getMetaclasses().entrySet()) {
				MMetaclass metaclass = metaclassEntry.getValue();
				//MClass currentClass = this.getClassByName(metaclass.getName());
				MClass currentClass = this.getMClass(fragmentEntry.getValue().getName(), metaclass.getName());
				
				
				// get parent
				if(metaclass.getParent() != null) {
					//MClass parentClass = this.getClassByName(metaclass.getParent().getMetaclass().getName());
					MClass parentClass = getMClass(metaclass.getParent().getFragmentName(), metaclass.getParent().getName());
					currentClass.getMSuperType().add(parentClass);
				}
				
				// dependencies
				for( Entry<String, MMetaclassDependency> dependencyEntry  : metaclassEntry.getValue().getDependencies().entrySet()) {
					MMetaclassDependency dependency = dependencyEntry.getValue();
					//MClass depClass = this.getClassByName(dependency.getTarget().getMetaclass().getName());
					MClass depClass = getMClass(dependency.getTarget().getFragmentName(), dependency.getTarget().getName());
					MDependency dep = new MDependency("", dependency.getName(), "", depClass, !(dependency.getMax() == 1), false, false, (dependency.getAggregation() == MAggregationType.Composition));
					currentClass.getMDependencys().add(dep);
				}
				
				// get children
				for(MMetaclassReference childMetaclassRef : metaclassEntry.getValue().getChildren()) {

					MClass childClass = getMClass(childMetaclassRef.getFragmentName(), childMetaclassRef.getName());
					//MClass childClass = this.getClassByName(childMetaclass.getName());
					
					currentClass.getMSubTypes().add(childClass);
				}				
			}
		}
		
	}
    
    private MClass getMClass(String fragment, String metaclass) {
    	return this.mClass.get(fragment+"."+ metaclass);		
	}
    
    private String getMClassName(String fragment, String metaclass) {
    	return (fragment+"."+ metaclass);		
	}
    
	public List<MPackage> getMPackages() {
        return mPackages;
    }

    public MDataType getDataTypeByName(String name) {
        return this.mBaseTypes.get(name);
    }

	public String GetFormat() {
		return mDescriptor.getMetamodelFormat();
	}
}


