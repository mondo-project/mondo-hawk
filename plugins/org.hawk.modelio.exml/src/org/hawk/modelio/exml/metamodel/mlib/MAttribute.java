/**
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is based 
 *       on code from modelio-metamodel-lib by Softeam at 
 *       https://github.com/aabherve/modelio-metamodel-lib.git
 *     Antonio Garcia-Dominguez - hashCode/equals, extract into .mlib
 */

package org.hawk.modelio.exml.metamodel.mlib;

public class MAttribute {
    private String id, name, exml;
    private Boolean isMany, isUnique, isOrdered;
    private MDataType mBaseType;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exml == null) ? 0 : exml.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isMany == null) ? 0 : isMany.hashCode());
		result = prime * result + ((isOrdered == null) ? 0 : isOrdered.hashCode());
		result = prime * result + ((isUnique == null) ? 0 : isUnique.hashCode());
		result = prime * result + ((mBaseType == null) ? 0 : mBaseType.hashCode());
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
		MAttribute other = (MAttribute) obj;
		if (exml == null) {
			if (other.exml != null)
				return false;
		} else if (!exml.equals(other.exml))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isMany == null) {
			if (other.isMany != null)
				return false;
		} else if (!isMany.equals(other.isMany))
			return false;
		if (isOrdered == null) {
			if (other.isOrdered != null)
				return false;
		} else if (!isOrdered.equals(other.isOrdered))
			return false;
		if (isUnique == null) {
			if (other.isUnique != null)
				return false;
		} else if (!isUnique.equals(other.isUnique))
			return false;
		if (mBaseType == null) {
			if (other.mBaseType != null)
				return false;
		} else if (!mBaseType.equals(other.mBaseType))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
