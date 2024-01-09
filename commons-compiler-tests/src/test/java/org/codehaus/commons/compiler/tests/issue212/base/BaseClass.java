
package org.codehaus.commons.compiler.tests.issue212.base;

public abstract class BaseClass implements IBase {

    private String baseId;

    public
    BaseClass() {}

    public
    BaseClass(String baseId) {
        super();
        this.baseId = baseId;
    }

    @Override public abstract BaseClass
    path();

    @Override public String
    toString() {
        return "BaseClass [baseId=" + this.baseId + "]";
    }
}
