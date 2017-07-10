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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import org.hawk.modelio.metamodel.parser.*;

public class ModelioMetamodel {
	//private static final Logger LOGGER = LoggerFactory.getLogger(ModelioMetaModelResource.class);

	private List<MPackage> mPackages;
	private MMetamodelDescriptor mDescriptor;    

	private Map<String, MDataType> mBaseTypes;

	private Map<String, MClass> mClass;

	public ModelioMetamodel(MMetamodelDescriptor metamodelDescriptor) {
		this.mBaseTypes = new HashMap<>();
		this.mPackages = new ArrayList<MPackage>();
		this.mClass = new HashMap<>();

		if(metamodelDescriptor != null) {
			this.mDescriptor = metamodelDescriptor;
			initMetamodel(this.mDescriptor);
		}
	}

	public void initMetamodel(MMetamodelDescriptor mDescriptor) {

		//	 Step 1: populate Data types 
		for ( MFragment fragment : mDescriptor.getFragments().values()) {
			for(MAttributeType attributeType : fragment.getDataTypes().values()) {
				MDataType dataType = null;

				if(attributeType instanceof MEnumeration) {
					dataType =  new MEnum(attributeType.getName(), attributeType.getName(), "enum", ((MEnumeration)attributeType).getValues());
				} 

				if(attributeType instanceof MAttributeType) {
					dataType = new MDataType(attributeType.getName(), attributeType.getName(), attributeType.getName());
				}

				this.mBaseTypes.put(dataType.getName(), dataType);
			}
		}

		//	 Step 2:  add Classes and Attributes
		for ( MFragment fragment : mDescriptor.getFragments().values()) {
			MPackage pkg = new MPackage(getMPackageId(fragment), fragment.getName(), fragment.getXmlString());
			mPackages.add(pkg);

			// get classes
			for (MMetaclass metaclass : fragment.getMetaclasses().values()) {
				String mcId = getMClassId(fragment.getName(), metaclass.getName());

				MClass mc = new MClass(mcId, metaclass.getName());

				pkg.getMClass().add(mc);

				this.mClass.put(mcId, mc);

				// attributes
				for(MMetaclassAttribute attribute :metaclass.getAttributes()) {
					String attrId = mc.getId() +"." + attribute.getName();
					MAttribute attr = new MAttribute(attrId, attribute.getName(), this.mBaseTypes.get(attribute.getType().getName()), false, true, false);
					mc.getMAttributes().add(attr);
				}
			}
		}

		//	 Step 3: add dependencies, supertypes and subtypes
		for (MFragment fragment : mDescriptor.getFragments().values()) {

			for (MMetaclass metaclass : fragment.getMetaclasses().values()) {
				MClass currentClass = this.getMClass(fragment.getName(), metaclass.getName());

				// get parent
				if(metaclass.getParent() != null) {
					currentClass.getMSuperType().add(getMClass(metaclass.getParent()));
				}

				// dependencies
				for( MMetaclassDependency dependency  : metaclass.getDependencies().values()) {
					MDependency dep = new MDependency(dependency.getName(), dependency.getName(), getMClass(dependency.getTarget()), !(dependency.getMax() == 1), true, true, (dependency.getAggregation() == MAggregationType.Composition));
					currentClass.getMDependencys().add(dep);
				}

				// get children
				for(MMetaclassReference childMetaclassRef : metaclass.getChildren()) {
					currentClass.getMSubTypes().add(getMClass(childMetaclassRef));
				}				
			}
		}
	}

	private String getMPackageId(MFragment fragment) {

		String pkgId = fragment.getProvider() + "." + fragment.getName();
		//+ fragment.getProviderVersion() 
		//+ fragment.getVersion();

		return pkgId;
	}

	private MClass getMClass(String fragment, String metaclass) {
		return this.mClass.get(getMClassId(fragment, metaclass));		
	}

	private MClass getMClass(MMetaclassReference ref) {
		return getMClass(ref.getFragmentName(), ref.getName());		
	}

	private String getMClassId(String fragment, String metaclass) {
		return fragment + "." + metaclass;
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


