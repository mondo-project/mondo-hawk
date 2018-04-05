/**
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is modified 
 *       from original code modelio-metamodel-lib by Softeam at 
 *       https://github.com/aabherve/modelio-metamodel-lib.git
 *     Antonio Garcia-Dominguez - extract into .mlib
 * */

package org.hawk.modelio.exml.metamodel.mlib;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isComposition == null) ? 0 : isComposition.hashCode());
		result = prime * result + ((isMany == null) ? 0 : isMany.hashCode());
		result = prime * result + ((isOrdered == null) ? 0 : isOrdered.hashCode());
		result = prime * result + ((isUnique == null) ? 0 : isUnique.hashCode());

		// Use ID to avoid endless loop
		result = prime * result + ((mClass == null) ? 0 : mClass.getId().hashCode());
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
		MDependency other = (MDependency) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isComposition == null) {
			if (other.isComposition != null)
				return false;
		} else if (!isComposition.equals(other.isComposition))
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

		// Use ID to avoid endless loop
		if (mClass == null) {
			if (other.mClass != null)
				return false;
		} else if (!mClass.getId().equals(other.mClass.getId()))
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
