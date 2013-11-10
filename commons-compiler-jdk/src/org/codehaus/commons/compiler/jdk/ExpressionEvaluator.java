
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;

/**
 * This {@link IExpressionEvaluator} is implemented by creating and compiling a temporary compilation unit defining one
 * class with one static method with one RETURN statement.
 * <p>
 * A number of "convenience constructors" exist that execute the set-up steps described for {@link
 * IExpressionEvaluator} instantly.
 * <p>
 * If the parameter and return types of the expression are known at compile time, then a "fast" expression evaluator can
 * be instantiated through {@link #createFastEvaluator(String, Class, String[])}. Expression evaluation is faster than
 * through {@link #evaluate(Object[])}, because it is not done through reflection but through direct method invocation.
 * <p>
 * Example:
 * <pre>
 * public interface Foo {
 *     int bar(int a, int b);
 * }
 * ...
 * Foo f = (Foo) ExpressionEvaluator.createFastExpressionEvaluator(
 *     "a + b",                    // expression to evaluate
 *     Foo.class,                  // interface that describes the expression's signature
 *     new String[] { "a", "b" },  // the parameters' names
 *     (ClassLoader) null          // Use current thread's context class loader
 * );
 * System.out.println("1 + 2 = " + f.bar(1, 2)); // Evaluate the expression
 * </pre>
 * Notice: The {@code interfaceToImplement} must either be declared {@code public},
 * or with package scope in the root package (i.e. "no" package).
 * <p>
 * On my system (Intel P4, 2 GHz, MS Windows XP, JDK 1.4.1), expression "x + 1"
 * evaluates as follows:
 * <table>
 *   <tr><td></td><th>Server JVM</th><th>Client JVM</th></td></tr>
 *   <tr><td>Normal EE</td><td>23.7 ns</td><td>64.0 ns</td></tr>
 *   <tr><td>Fast EE</td><td>31.2 ns</td><td>42.2 ns</td></tr>
 * </table>
 * (How can it be that interface method invocation is slower than reflection for
 * the server JVM?)
 */
public
class ExpressionEvaluator extends ScriptEvaluator implements IExpressionEvaluator {

    private Class<?>[] optionalExpressionTypes;

    /**
     * Equivalent to<pre>
     * ExpressionEvaluator ee = new ExpressionEvaluator();
     * ee.setExpressionType(expressionType);
     * ee.setParameters(parameterNames, parameterTypes);
     * ee.cook(expression);</pre>
     *
     * @see #ExpressionEvaluator()
     * @see ExpressionEvaluator#setExpressionType(Class)
     * @see ScriptEvaluator#setParameters(String[], Class[])
     * @see Cookable#cook(String)
     */
    public
    ExpressionEvaluator(
        String     expression,
        Class<?>   expressionType,
        String[]   parameterNames,
        Class<?>[] parameterTypes
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.cook(expression);
    }

    /**
     * Equivalent to<pre>
     * ExpressionEvaluator ee = new ExpressionEvaluator();
     * ee.setExpressionType(expressionType);
     * ee.setParameters(parameterNames, parameterTypes);
     * ee.setThrownExceptions(thrownExceptions);
     * ee.setParentClassLoader(optionalParentClassLoader);
     * ee.cook(expression);</pre>
     *
     * @see #ExpressionEvaluator()
     * @see ExpressionEvaluator#setExpressionType(Class)
     * @see ScriptEvaluator#setParameters(String[], Class[])
     * @see ScriptEvaluator#setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String)
     */
    public
    ExpressionEvaluator(
        String      expression,
        Class<?>    expressionType,
        String[]    parameterNames,
        Class<?>[]  parameterTypes,
        Class<?>[]  thrownExceptions,
        ClassLoader optionalParentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(expression);
    }

    /**
     * Equivalent to<pre>
     * ExpressionEvaluator ee = new ExpressionEvaluator();
     * ee.setExpressionType(expressionType);
     * ee.setParameters(parameterNames, parameterTypes);
     * ee.setThrownExceptions(thrownExceptions);
     * ee.setExtendedType(optionalExtendedType);
     * ee.setImplementedTypes(implementedTypes);
     * ee.setParentClassLoader(optionalParentClassLoader);
     * ee.cook(expression);</pre>
     *
     * @see #ExpressionEvaluator()
     * @see ExpressionEvaluator#setExpressionType(Class)
     * @see ScriptEvaluator#setParameters(String[], Class[])
     * @see ScriptEvaluator#setThrownExceptions(Class[])
     * @see ClassBodyEvaluator#setExtendedClass(Class)
     * @see ClassBodyEvaluator#setImplementedInterfaces(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String)
     */
    public
    ExpressionEvaluator(
        String      expression,
        Class<?>    expressionType,
        String[]    parameterNames,
        Class<?>[]  parameterTypes,
        Class<?>[]  thrownExceptions,
        Class<?>    optionalExtendedType,
        Class<?>[]  implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(expression);
    }

    public ExpressionEvaluator() {}

    @Override public void
    setExpressionType(@SuppressWarnings("rawtypes") Class expressionType) {
        this.setExpressionTypes(new Class[] { expressionType });
    }

    @Override public void
    setExpressionTypes(@SuppressWarnings("rawtypes") Class[] expressionTypes) {
        this.assertNotCooked();
        this.optionalExpressionTypes = expressionTypes;

        Class<?>[] returnTypes = new Class[expressionTypes.length];
        for (int i = 0; i < returnTypes.length; ++i) {
            Class<?> et = expressionTypes[i];
            returnTypes[i] = et == ANY_TYPE ? Object.class : et;
        }
        super.setReturnTypes(returnTypes);
    }

    /** @deprecated */
    @Override @Deprecated public final void
    setReturnType(@SuppressWarnings("rawtypes") Class returnType) {
        throw new AssertionError("Must not be used on an ExpressionEvaluator; use 'setExpressionType()' instead");
    }

    /** @deprecated */
    @Override @Deprecated public final void
    setReturnTypes(@SuppressWarnings("rawtypes") Class[] returnTypes) {
        throw new AssertionError("Must not be used on an ExpressionEvaluator; use 'setExpressionTypes()' instead");
    }

    /** The default return type of an expression is {@code Object}. */
    @Override protected Class<?>
    getDefaultReturnType() { return Object.class; }

    @Override public void
    cook(String[] optionalFileNames, Reader[] readers) throws CompileException, IOException {

        readers = readers.clone(); // Don't modify the argument array.

        String[] imports;
        if (readers.length == 1) {
            if (!readers[0].markSupported()) readers[0] = new BufferedReader(readers[0]);
            imports = parseImportDeclarations(readers[0]);
        } else
        {
            imports = new String[0];
        }

        Class<?>[] returnTypes = new Class[readers.length];
        for (int i = 0; i < readers.length; ++i) {
            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);

            if (this.optionalExpressionTypes == null || this.optionalExpressionTypes[i] == ANY_TYPE) {
                returnTypes[i] = Object.class;
                pw.print("return org.codehaus.commons.compiler.PrimitiveWrapper.wrap(");
                pw.write(readString(readers[i]));
                pw.println(");");
            } else {
                returnTypes[i] = this.optionalExpressionTypes[i];
                if (returnTypes[i] != void.class && returnTypes[i] != Void.class) {
                    pw.print("return ");
                }
                pw.write(readString(readers[i]));
                pw.println(";");
            }
            pw.close();
            readers[i] = new StringReader(sw.toString());
        }
        super.setReturnTypes(returnTypes);
        this.cook(optionalFileNames, readers, imports);
    }
}
