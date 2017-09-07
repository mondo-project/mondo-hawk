/**
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser. This file is acquired
 *     from original code modelio-metamodel-lib by Softeam at 
 *     https://github.com/aabherve/modelio-metamodel-lib.git
 * */

package org.hawk.modelio.exml.metamodel;

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

}
