
package org.codehaus.commons.compiler.tests.issue212.base;

public
class DerivedClass extends BaseClass {

    @Override public BaseClass
    path() {
        return new DerivedClass();
    }

    @Override public String
    toString() {
        return super.toString() + " DerivedClass";
    }
}
