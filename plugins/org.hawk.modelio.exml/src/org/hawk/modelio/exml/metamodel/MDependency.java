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

public class MDependency {
    private String id;

    private String name;

    private Boolean isMany;

    private Boolean isUnique;

    private Boolean isOrdered;

    private Boolean isComposition;

    private MClass mClass;

    public MDependency(String id, String name, MClass mClass, Boolean isMany, Boolean isUnique, Boolean isOrdered, Boolean isComposition) {
        this.id = id;
        this.name = name;
        this.mClass = mClass;
        this.isMany = isMany;
        this.isUnique = isUnique;
        this.isOrdered = isOrdered;
        this.isComposition = isComposition;
	}

	public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MClass getMClass() {
        return mClass;
    }

    public Boolean getIsMany() {
        return isMany;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public Boolean getIsOrdered() {
        return isOrdered;
    }

    public Boolean getisComposition() {
        return isComposition;
    }

}
