package org.hawk.modelio.exml.metamodel;

/**
 * BaseType of Modleio 3.4.1 metamodel
 * Class generated with  ModelioMetaGenerator 1.0.0 Tool
 * 
 * @author Antonin Abherv?
 */
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
