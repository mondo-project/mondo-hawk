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
 * */

package org.hawk.modelio.exml.metamodel.mlib;

import java.util.List;

public class MEnum extends MDataType {
    private List<String> values;

    public MEnum(String id, String name, String javaEquivalent, List<String> values) {
        super(id, name, javaEquivalent);
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		MEnum other = (MEnum) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}
}
