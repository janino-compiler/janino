
package org.codehaus.commons.compiler.tests.issue212.base;

public abstract class BaseClass implements IBase {

    // To reproduce the problem, there must be two or more constructors, with zero or more parameters:

//    public BaseClass() {}
    public BaseClass(long l) {}
    public BaseClass(int i) {}

    @Override public abstract BaseClass
    path();

    @Override public String
    toString() {
        return "BaseClass";
    }
}
