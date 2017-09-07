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

public class MDataType {
    private String id;

    private String name;

    private String javaEquivalent;

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

}
