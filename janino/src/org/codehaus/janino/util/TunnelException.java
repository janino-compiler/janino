
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.util;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Redirects all methods to the delegate {@link Throwable}.
 * <p>
 * This class is useful if you want to throw a non-{@link RuntimeException}-derived
 * Exception, but cannot declare it in the "throws" clause of your method, because your method
 * overrides a method of a base class. The classical example is
 * {@link ClassLoader#findClass(String)}, which allows only
 * {@link ClassNotFoundException} to be thrown, but you want to implement a more
 * elaborate exception handling.
 * <p>
 * To make clear which exceptions wrapped in {@link TunnelException} your method would throw, it
 * is recommended that you declare them with a "<code>throws </code>{@link TunnelException}" clause and that
 * you document them in JAVADOC with <code>@<b></b>throws</code> clauses like this:
 * <pre>
 * &#47;**
 *  * ...
 *  * &#64;throws TunnelException Wraps a {&#64;link FooException} - Problems writing a Foo
 *  * &#64;throws TunnelException Wraps a {&#64;link BarException} - The Bar could not be opened
 *  *&#47;
 * </pre>
 */
public class TunnelException extends RuntimeException {
    private final static String CLASS_NAME = TunnelException.class.getName(); // Fully qualified name of the class.

    private final Throwable delegate;

    public TunnelException(Throwable delegate) {
        this.delegate = delegate;
    }
    public Throwable getDelegate() {
        return this.delegate;
    }

    // Redirect all methods to delegate.
    // Only JDK 1.2.2 methods (for compatibility).
    public String    getLocalizedMessage()           { return TunnelException.CLASS_NAME + ": " + this.delegate.getLocalizedMessage(); }
    public String    getMessage()                    { return TunnelException.CLASS_NAME + ": " + this.delegate.getMessage(); }
    public void      printStackTrace()               { this.delegate.printStackTrace(); }
    public void      printStackTrace(PrintStream ps) { ps.println(TunnelException.CLASS_NAME + " caused by:"); this.delegate.printStackTrace(ps); }
    public void      printStackTrace(PrintWriter pw) { pw.println(TunnelException.CLASS_NAME + " caused by:"); this.delegate.printStackTrace(pw); }
    public String    toString()                      { return TunnelException.CLASS_NAME + ": " + this.delegate.toString(); }
}


