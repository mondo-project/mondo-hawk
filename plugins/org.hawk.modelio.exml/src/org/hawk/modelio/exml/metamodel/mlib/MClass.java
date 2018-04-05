/**
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is based
 *       on code of modelio-metamodel-lib by Softeam at 
 *       https://github.com/aabherve/modelio-metamodel-lib.git
 *     Antonio Garcia-Dominguez - add equals/hashCode, extract into .mlib
 * */

package org.hawk.modelio.exml.metamodel.mlib;

import java.util.ArrayList;
import java.util.List;


public class MClass {
    private final String id, name;
    private final List<MClass> mSuperTypes, mSubTypes;
    private final List<MDependency> mDependencies;
    private final List<MAttribute> mAttributes;

    public MClass(String id, String name) {
        this.mSuperTypes = new ArrayList<>();
        this.mSubTypes = new ArrayList<>();
        this.mDependencies = new ArrayList<>();
        this.mAttributes = new ArrayList<>();
        this.id = id;
        this.name = name;
   }

	public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MClass> getMSuperType() {
        return mSuperTypes;
    }

    public List<MClass> getMSubTypes() {
        return mSubTypes;
    }

    public List<MDependency> getMDependencys() {
        return mDependencies;
    }

    public List<MAttribute> getMAttributes() {
        return this.mAttributes;
    }

    public MAttribute getAttributByName(String name) {
        for(MAttribute attr : this.mAttributes){
            if(attr.getName().equals(name)){
                return attr;
            }
        }
        return null;
    }

	@Override
	public int hashCode() {
		// We use only identity-based equality to speed up lookups in HashMaps/HashSets:
		// we assume that if id and name are equal, the rest will be equal within the
		// metamodel
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		MClass other = (MClass) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

    
}
