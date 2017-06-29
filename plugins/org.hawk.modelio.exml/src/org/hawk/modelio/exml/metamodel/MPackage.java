package org.hawk.modelio.exml.metamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Package of Modleio 3.4.1 metamodel
 * Class generated with  ModelioMetaGenerator 1.0.0 Tool
 * 
 * @author Antonin Abherv?
 */
public class MPackage {
    private String id;

    private String name;

    private String exml;

    private List<MPackage> mPackages;

    private List<MClass> mClass;

    public MPackage(String id, String name, String exml) {
        this.mPackages = new ArrayList<>();
        this.mClass = new ArrayList<>();
        this.id = id;
        this.name = name;
        this.exml = exml;
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

    public List<MPackage> getMPackages() {
        return mPackages;
    }

    public List<MClass> getMClass() {
        return mClass;
    }

}
