
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.MultiCookable;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.io.Readers;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * This {@link IExpressionEvaluator} is implemented by creating and compiling a temporary compilation unit defining one
 * class with one static method with one RETURN statement.
 * <p>
 *   A number of "convenience constructors" exist that execute the set-up steps described for {@link
 *   IExpressionEvaluator} instantly.
 * </p>
 * <p>
 *   If the parameter and return types of the expression are known at compile time, then a "fast" expression evaluator
 *   can be instantiated through {@link #createFastEvaluator(String, Class, String[])}. Expression evaluation is faster
 *   than through {@link #evaluate(Object[])}, because it is not done through reflection but through direct method
 *   invocation.
 * </p>
 * <p>
 *   Example:
 * </p>
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
 * <p>
 *   Notice: The <var>interfaceToImplement</var> must either be declared <var>public</var>, or with package scope in
 *   the root package (i.e. "no" package).
 * </p>
 * <p>
 *   On my system (Intel P4, 2 GHz, MS Windows XP, JDK 1.4.1), expression "x + 1" evaluates as follows:
 * </p>
 * <table>
 *   <tr><td></td><th>Server JVM</th><th>Client JVM</th></tr>
 *   <tr><td>Normal EE</td><td>23.7 ns</td><td>64.0 ns</td></tr>
 *   <tr><td>Fast EE</td><td>31.2 ns</td><td>42.2 ns</td></tr>
 * </table>
 * <p>
 *   (How can it be that interface method invocation is slower than reflection for the server JVM?)
 * </p>
 */
public
class ExpressionEvaluator extends MultiCookable implements IExpressionEvaluator {

    private final ScriptEvaluator se = new ScriptEvaluator();
    {
        this.se.setClassName(IExpressionEvaluator.DEFAULT_CLASS_NAME);
        this.se.setDefaultReturnType(IExpressionEvaluator.DEFAULT_EXPRESSION_TYPE);
    }

    /**
     * Equivalent to
     * <pre>
     *     ExpressionEvaluator ee = new ExpressionEvaluator();
     *     ee.setExpressionType(expressionType);
     *     ee.setParameters(parameterNames, parameterTypes);
     *     ee.cook(expression);
     * </pre>
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
     * Equivalent to
     * <pre>
     *     ExpressionEvaluator ee = new ExpressionEvaluator();
     *     ee.setExpressionType(expressionType);
     *     ee.setParameters(parameterNames, parameterTypes);
     *     ee.setThrownExceptions(thrownExceptions);
     *     ee.setParentClassLoader(parentClassLoader);
     *     ee.cook(expression);
     * </pre>
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
        String                expression,
        Class<?>              expressionType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(parentClassLoader);
        this.cook(expression);
    }

    /**
     * Equivalent to
     * <pre>
     *     ExpressionEvaluator ee = new ExpressionEvaluator();
     *     ee.setExpressionType(expressionType);
     *     ee.setParameters(parameterNames, parameterTypes);
     *     ee.setThrownExceptions(thrownExceptions);
     *     ee.setExtendedType(extendedType);
     *     ee.setImplementedTypes(implementedTypes);
     *     ee.setParentClassLoader(parentClassLoader);
     *     ee.cook(expression);
     * </pre>
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
        String                expression,
        Class<?>              expressionType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable Class<?>    extendedType,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setExtendedClass(extendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(parentClassLoader);
        this.cook(expression);
    }

    public ExpressionEvaluator() {}

    // ============================= CONFIGURATION SETTS AND GETTERS =============================

    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) {
        this.se.setParentClassLoader(parentClassLoader);
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.se.setDebuggingInformation(debugSource, debugLines, debugVars);
    }

    @Override public void
    setSourceVersion(int version) { this.se.setSourceVersion(version); }

    @Override public void
    setTargetVersion(int version) { this.se.setTargetVersion(version); }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.se.setCompileErrorHandler(compileErrorHandler);
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.se.setWarningHandler(warningHandler);
    }

    @Override public void
    setDefaultImports(String... defaultImports) { this.se.setDefaultImports(defaultImports); }

    @Override public String[]
    getDefaultImports() { return this.se.getDefaultImports(); }

    @Override public void
    setDefaultExpressionType(Class<?> defaultExpressionType) { this.se.setDefaultReturnType(defaultExpressionType); }

    @Override public Class<?>
    getDefaultExpressionType() { return this.se.getDefaultReturnType(); }

    @Override public void
    setImplementedInterfaces(Class<?>[] implementedTypes) { this.se.setImplementedInterfaces(implementedTypes); }

    @Override public void
    setReturnType(Class<?> returnType) { this.se.setReturnType(returnType); }

    @Override public void
    setExpressionType(Class<?> expressionType) { this.se.setReturnType(expressionType); }

    @Override public void
    setExpressionTypes(Class<?>[] expressionTypes) { this.se.setReturnTypes(expressionTypes); }

    @Override public void
    setOverrideMethod(boolean overrideMethod) { this.se.setOverrideMethod(overrideMethod); }

    @Override public void
    setOverrideMethod(boolean[] overrideMethod) { this.se.setOverrideMethod(overrideMethod); }

    @Override public void
    setParameters(String[] parameterNames, Class<?>[] parameterTypes) {
        this.se.setParameters(parameterNames, parameterTypes);
    }

    @Override public void
    setParameters(String[][] parameterNames, Class<?>[][] parameterTypes) {
        this.se.setParameters(parameterNames, parameterTypes);
    }

    @Override public void
    setClassName(String className) { this.se.setClassName(className); }

    @Override public void
    setExtendedClass(@Nullable Class<?> extendedType) { this.se.setExtendedClass(extendedType); }

    @Override public void
    setStaticMethod(boolean staticMethod) { this.se.setStaticMethod(staticMethod); }

    @Override public void
    setStaticMethod(boolean[] staticMethod) { this.se.setStaticMethod(staticMethod); }

    @Override public void
    setMethodName(String methodName) { this.se.setMethodName(methodName); }

    @Override public void
    setMethodNames(String[] methodNames) { this.se.setMethodNames(methodNames); }

    @Override public void
    setThrownExceptions(Class<?>[] thrownExceptions) { this.se.setThrownExceptions(thrownExceptions); }

    @Override public void
    setThrownExceptions(Class<?>[][] thrownExceptions) { this.se.setThrownExceptions(thrownExceptions); }

    @Override public void
    cook(@Nullable String fileName, Reader reader) throws CompileException, IOException {

        this.se.setScriptCount(1);

        if (!reader.markSupported()) reader = new BufferedReader(reader);
        final String[] imports = ClassBodyEvaluator.parseImportDeclarations(reader);

        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(sw);
        try {

            Class<?> returnType = this.se.getReturnType(0);
            if (returnType != void.class && returnType != Void.class) pw.print("return ");

            Readers.copy(reader, pw);
            pw.println(";");

            pw.close();
        } finally {
            try { pw.close(); } catch (Exception e) {}
        }

        reader = new StringReader(sw.toString());

        this.se.cook(new String[] { fileName }, new Reader[] { reader }, imports);
    }

    @Override public void
    cook(String[] fileNames, Reader[] readers) throws CompileException, IOException {

        readers = readers.clone(); // Don't modify the argument array.

        String[] imports;
        if (readers.length == 1) {
            if (!readers[0].markSupported()) readers[0] = new BufferedReader(readers[0]);
            imports = ClassBodyEvaluator.parseImportDeclarations(readers[0]);
        } else
        {
            imports = new String[0];
        }

        Class<?>[] returnTypes = new Class[readers.length];
        for (int i = 0; i < readers.length; ++i) {

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);

            returnTypes[i] = this.se.getReturnType(i);
            if (returnTypes[i] != void.class && returnTypes[i] != Void.class) {
                pw.print("return ");
            }
            Readers.copy(readers[i], pw);
            pw.println(";");

            pw.close();
            readers[i] = new StringReader(sw.toString());
        }

        this.se.cook(fileNames, readers, imports);
    }

    @Override @Nullable public Object
    evaluate() throws InvocationTargetException { return this.evaluate(new Object[0]); }

    @Override @Nullable public Object
    evaluate(@Nullable Object[] arguments) throws InvocationTargetException { return this.se.evaluate(arguments); }

    @Override @Nullable public Object
    evaluate(int idx, @Nullable Object[] arguments) throws InvocationTargetException {
        return this.se.evaluate(idx, arguments);
    }

    @Override public Method
    getMethod() { return this.se.getMethod(); }

    @Override public Method
    getMethod(int idx) { return this.se.getMethod(idx); }

    @Override public Class<?>
    getClazz() { return this.se.getClazz(); }

    @Override public Method[]
    getResult() { return this.se.getResult(); }

    @Override public Map<String, byte[]>
    getBytecodes() { return this.se.getBytecodes(); }

    @Override public <T> T
    createFastEvaluator(String expression, Class<? extends T> interfaceToImplement, String... parameterNames)
    throws CompileException {
        try {
            return this.createFastEvaluator(
                new StringReader(expression),
                interfaceToImplement,
                parameterNames
            );
        } catch (IOException ex) {
            throw new InternalCompilerException("IOException despite StringReader", ex);
        }
    }

    @Override public <T> T
    createFastEvaluator(Reader reader, Class<? extends T> interfaceToImplement, String... parameterNames)
    throws CompileException, IOException {

        if (!interfaceToImplement.isInterface()) {
            throw new InternalCompilerException("\"" + interfaceToImplement + "\" is not an interface");
        }

        Method methodToImplement;
        {
            Method[] methods = interfaceToImplement.getDeclaredMethods();
            if (methods.length != 1) {
                throw new InternalCompilerException(
                    "Interface \""
                    + interfaceToImplement
                    + "\" must declare exactly one method"
                );
            }
            methodToImplement = methods[0];
        }

        this.setImplementedInterfaces(new Class[] { interfaceToImplement });
        this.setOverrideMethod(true);
        this.setStaticMethod(false);
        this.setExpressionType(methodToImplement.getReturnType());
        this.setMethodName(methodToImplement.getName());
        this.setParameters(parameterNames, methodToImplement.getParameterTypes());
        this.setThrownExceptions(methodToImplement.getExceptionTypes());
        this.cook(reader);

        @SuppressWarnings("unchecked") Class<? extends T>
        actualClass = (Class<? extends T>) this.getMethod().getDeclaringClass();

        try {
            return actualClass.newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new InternalCompilerException(e.toString(), e);
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new InternalCompilerException(e.toString(), e);
        }
    }
}
