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
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is
 *       based on code from modelio-metamodel-lib by Softeam at 
 *       https://github.com/aabherve/modelio-metamodel-lib.git
 *     Antonio Garcia-Dominguez - hashCode/equals, extract into .mlib
 */

package org.hawk.modelio.exml.metamodel.mlib;

public class MDataType {
    private String id, name, javaEquivalent;

    public MDataType(String id, String name, String javaEquivalent) {
        this.id = id;
        this.name = name;
        this.javaEquivalent = javaEquivalent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJavaEquivalent() {
        return javaEquivalent;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((javaEquivalent == null) ? 0 : javaEquivalent.hashCode());
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
		MDataType other = (MDataType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (javaEquivalent == null) {
			if (other.javaEquivalent != null)
				return false;
		} else if (!javaEquivalent.equals(other.javaEquivalent))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
