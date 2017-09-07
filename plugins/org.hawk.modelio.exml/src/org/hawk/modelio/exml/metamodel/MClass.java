/**
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is modified 
 *     from original code modelio-metamodel-lib by Softeam at 
 *     https://github.com/aabherve/modelio-metamodel-lib.git
 * */

package org.hawk.modelio.exml.metamodel;

import java.util.ArrayList;
import java.util.List;


public class MClass {
    private String id;

    private String name;

    private List<MClass> mSuperType;

    private List<MClass> mSubTypes;

    private List<MDependency> mDependencys;

    private ArrayList<MAttribute> mAttributes;

    public MClass(String id, String name) {
        this.mSuperType = new ArrayList<>();
        this.mSubTypes = new ArrayList<>();
        this.mDependencys = new ArrayList<>();
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
        return mSuperType;
    }

    public List<MClass> getMSubTypes() {
        return mSubTypes;
    }

    public List<MDependency> getMDependencys() {
        return mDependencys;
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

}
