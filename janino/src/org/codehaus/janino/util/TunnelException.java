
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util;

import java.io.*;


/**
 * Redirects all methods to the delegate {@link Throwable}.
 * <p>
 * This class is useful if you want to throw a non-{@link RuntimeException}-derived
 * Exception, but cannot declare it in the "throws" clause of your method, because your method
 * overrides a method of a base class. The classical example is
 * <code>ClassLoader.findClass(String)</code>, which allows only
 * {@link ClassNotFoundException} to be thrown, but you want to implement a more
 * elaborate exception handling.
 * <p>
 * To make clear which exceptions wrapped in {@link TunnelException} your method would throw, it
 * is recommended that you declare them with a "<code>throws </code>{@link TunnelException}" clause and that
 * you document them in JAVADOC with <code>&#64;throws</code> clauses like this:
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


