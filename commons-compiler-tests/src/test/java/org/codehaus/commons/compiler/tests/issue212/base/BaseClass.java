
package org.codehaus.commons.compiler.tests.issue212.base;

public abstract class BaseClass implements IBase {

    @Override public abstract BaseClass
    path();

    @Override public String
    toString() {
        return "BaseClass";
    }
}
