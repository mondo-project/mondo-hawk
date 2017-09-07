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

public class MAttribute {
    private String id;

    private String name;

    private String exml;

    private Boolean isMany;

    private Boolean isUnique;

    private Boolean isOrdered;

    private MDataType mBaseType;

    public MAttribute(String id, String name, String exml, MDataType mBaseType, Boolean isMany, Boolean isUnique, Boolean isOrdered) {
        this.id = id;
        this.name = name;
        this.exml = exml;
        this.mBaseType = mBaseType;
        this.isMany = isMany;
        this.isUnique = isUnique;
        this.isOrdered = isOrdered;
    }

    public MAttribute(String id, String name, MDataType mBaseType, Boolean isMany, Boolean isUnique, Boolean isOrdered) {
        this.id = id;
        this.name = name;
        this.mBaseType = mBaseType;
        this.isMany = isMany;
        this.isUnique = isUnique;
        this.isOrdered = isOrdered;
    }

	public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExml() {
        return exml;
    }

    public MDataType getMDataType() {
        return mBaseType;
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

}
