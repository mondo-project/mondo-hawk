/**
 * Copyright (c) 2017 Aston University
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is based 
 *       on modelio-metamodel-lib by Softeam at 
 *       https://github.com/aabherve/modelio-metamodel-lib.git
 *     Antonio Garcia-Dominguez - extract into .mlib
 * */
package org.hawk.modelio.exml.metamodel.mlib;

import java.util.ArrayList;
import java.util.List;

public class MPackage {
    private final String id, name, xml, version;

    private List<MPackage> mPackages;
    private List<MClass> mClass;

    public MPackage(String id, String name, String version, String xml) {
        this.mPackages = new ArrayList<>();
        this.mClass = new ArrayList<>();

        this.id = id;
        this.name = name;
		this.version = version;
        this.xml = xml;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<MPackage> getMPackages() {
        return mPackages;
    }

    public List<MClass> getMClass() {
        return mClass;
    }

	public String getXml() {
		return xml;
	}
	
	public String getVersion() {
		return version;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mClass == null) ? 0 : mClass.hashCode());
		result = prime * result + ((mPackages == null) ? 0 : mPackages.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		MPackage other = (MPackage) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mClass == null) {
			if (other.mClass != null)
				return false;
		} else if (!mClass.equals(other.mClass))
			return false;
		if (mPackages == null) {
			if (other.mPackages != null)
				return false;
		} else if (!mPackages.equals(other.mPackages))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;

		return true;
	}

}
