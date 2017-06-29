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
