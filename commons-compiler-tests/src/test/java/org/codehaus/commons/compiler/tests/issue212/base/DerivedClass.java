
package org.codehaus.commons.compiler.tests.issue212.base;

public
class DerivedClass extends BaseClass {

    private String name;

    public
    DerivedClass() {
        super();
    }

    public
    DerivedClass(String baseId, String name) {
        super(baseId);
        this.name = name;
    }

    @Override public BaseClass
    path() {
        return new DerivedClass("0", "default");
    }

    @Override public String
    toString() {
        return super.toString() + "DerivedClass [name=" + this.name + "]";
    }
}
