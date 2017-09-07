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

public class MPackage {
    private String id;

    private String name;

    //private String exml;
    
    private String xml;
    
    private String version;


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

	public void setXml(String xml) {
		this.xml = xml;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
