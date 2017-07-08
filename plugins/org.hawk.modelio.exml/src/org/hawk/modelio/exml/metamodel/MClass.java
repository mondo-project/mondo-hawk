package org.hawk.modelio.exml.metamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Class of Modleio 3.4.1 metamodel
 * Class generated with  ModelioMetaGenerator 1.0.0 Tool
 * 
 * @author Antonin Abherv?
 */
public class MClass {
    private String id;

    private String name;

    private String exml;

    private List<MClass> mSuperType;

    private List<MClass> mSubTypes;

    private List<MDependency> mDependencys;

    private ArrayList<MAttribute> mAttributes;

    public MClass(String id, String name, String exml) {
        this.mSuperType = new ArrayList<>();
        this.mSubTypes = new ArrayList<>();
        this.mDependencys = new ArrayList<>();
        this.mAttributes = new ArrayList<>();
        this.id = id;
        this.name = name;
        this.exml = exml;
    }

    public MClass(String id, String name) {
        this.mSuperType = new ArrayList<>();
        this.mSubTypes = new ArrayList<>();
        this.mDependencys = new ArrayList<>();
        this.mAttributes = new ArrayList<>();
        this.id = id;
        this.name = name;
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

    public List<MClass> getMSuperType() {
        return mSuperType;
    }

    public List<MClass> getMSubTypes() {
        return mSubTypes;
    }

    public List<MDependency> getMDependencys() {
        return mDependencys;
    }

    public List<MAttribute> getMAttributes() {
        return this.mAttributes;
    }

    public MAttribute getAttributByName(String name) {
        for(MAttribute attr : this.mAttributes){
            if(attr.getName().equals(name)){
                return attr;
            }
        }
        return null;
    }

}
