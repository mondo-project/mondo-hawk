package org.hawk.modelio.exml.metamodel;

/**
 * Dependency of Modleio 3.4.1 metamodel
 * Class generated with  ModelioMetaGenerator 1.0.0 Tool
 * 
 * @author Antonin Abherv?
 */
public class MDependency {
    private String id;

    private String name;

    private String exml;

    private Boolean isMany;

    private Boolean isUnique;

    private Boolean isOrdered;

    private Boolean isComposition;

    private MClass mClass;

    public MDependency(String id, String name, String exml, MClass mClass, Boolean isMany, Boolean isUnique, Boolean isOrdered, Boolean isComposition) {
        this.id = id;
        this.name = name;
        this.exml = exml;
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

    public String getExml() {
        return exml;
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

}
