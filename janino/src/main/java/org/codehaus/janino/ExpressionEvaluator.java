
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

package org.codehaus.janino;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.MultiCookable;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.AbstractTraverser;

/**
 * This {@link IExpressionEvaluator} is implemented by creating and compiling a temporary compilation unit defining one
 * class with one static method with one RETURN statement.
 * <p>
 *   A number of "convenience constructors" exist that execute the set-up steps described for {@link
 *   IExpressionEvaluator} instantly.
 * </p>
 * <p>
 *   If the expression type and the parameters' types are known at compile time, then a "fast" expression evaluator
 *   can be instantiated through {@link #createFastExpressionEvaluator(String, Class, String[], ClassLoader)}.
 *   Expression evaluation is faster than through {@link #evaluate(Object[])}, because it is not done through
 *   reflection but through direct method invocation.
 * </p>
 * <p>
 *   Example:
 * </p>
 * <pre>
 *     public interface Foo {
 *         int bar(int a, int b);
 *     }
 *     ...
 *     Foo f = (Foo) ExpressionEvaluator.createFastExpressionEvaluator(
 *         "a + b",                    // expression to evaluate
 *         Foo.class,                  // interface that describes the expression's signature
 *         new String[] { "a", "b" },  // the parameters' names
 *         (ClassLoader) null          // Use current thread's context class loader
 *     );
 *     System.out.println("1 + 2 = " + f.bar(1, 2)); // Evaluate the expression
 * </pre>
 * <p>
 *   Notice: The {@code interfaceToImplement} must either be declared {@code public}, or with package scope in the root
 *   package (i.e. "no" package).
 * </p>
 * <p>
 *   On my system (Intel P4, 2 GHz, MS Windows XP, JDK 1.4.1), expression "x + 1" evaluates as follows:
 * </p>
 * <table>
 *   <tr><td></td><th>Server JVM</th><th>Client JVM</th></tr>
 *   <tr><td>Normal EE</td><td>23.7 ns</td><td>64.0 ns</td></tr>
 *   <tr><td>Fast EE</td><td>31.2 ns</td><td>42.2 ns</td></tr>
 * </table>
 */
public
class ExpressionEvaluator extends MultiCookable implements IExpressionEvaluator {

//    private static final Logger     LOGGER                  = Logger.getLogger(ExpressionEvaluator.class.getName());
//    private static final Class<?>   DEFAULT_EXPRESSION_TYPE = Object.class;
//    private static final Class<?>[] ZERO_CLASSES            = new Class<?>[0];

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
    ExpressionEvaluator(String expression, Class<?> expressionType, String[] parameterNames, Class<?>[] parameterTypes)
    throws CompileException {
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
     *     ee.setParentClassLoader(optionalParentClassLoader);
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
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(expression);
    }

    /**
     * Equivalent to
     * <pre>
     *     ExpressionEvaluator ee = new ExpressionEvaluator();
     *     ee.setExpressionType(expressionType);
     *     ee.setParameters(parameterNames, parameterTypes);
     *     ee.setThrownExceptions(thrownExceptions);
     *     ee.setExtendedClass(extendedClass);
     *     ee.setImplementedTypes(implementedTypes);
     *     ee.setParentClassLoader(optionalParentClassLoader);
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
        Class<?>              extendedClass,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setExtendedClass(extendedClass);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(expression);
    }

    /**
     * Creates an expression evaluator with the full configurability.
     * <p>
     *   Equivalent to:
     * </p>
     * <pre>
     *     ExpressionEvaluator ee = new ExpressionEvaluator();
     *     ee.setClassName(className);
     *     ee.setExtendedType(optionalExtendedType);
     *     ee.setImplementedTypes(implementedTypes);
     *     ee.setStaticMethod(staticMethod);
     *     ee.setExpressionType(expressionType);
     *     ee.setMethodName(methodName);
     *     ee.setParameters(parameterNames, parameterTypes);
     *     ee.setThrownExceptions(thrownExceptions);
     *     ee.setParentClassLoader(optionalParentClassLoader);
     *     ee.cook(scanner);
     * </pre>
     *
     * @see IExpressionEvaluator
     * @see IClassBodyEvaluator#setClassName(String)
     * @see IClassBodyEvaluator#setExtendedClass(Class)
     * @see IClassBodyEvaluator#setImplementedInterfaces(Class[])
     * @see IScriptEvaluator#setStaticMethod(boolean)
     * @see IExpressionEvaluator#setExpressionType(Class)
     * @see IScriptEvaluator#setMethodName(String)
     * @see IScriptEvaluator#setParameters(String[], Class[])
     * @see IScriptEvaluator#setThrownExceptions(Class[])
     * @see ISimpleCompiler#setParentClassLoader(ClassLoader)
     * @see ICookable#cook(Reader)
     */
    public
    ExpressionEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    optionalExtendedType,
        Class<?>[]            implementedTypes,
        boolean               staticMethod,
        Class<?>              expressionType,
        String                methodName,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        this.setClassName(className);
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setStaticMethod(staticMethod);
        this.setExpressionType(expressionType);
        this.setMethodName(methodName);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public ExpressionEvaluator() {}


    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) {
        this.se.setParentClassLoader(parentClassLoader);
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.se.setDebuggingInformation(debugSource, debugLines, debugVars);
    }

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

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.se.options(); }

    /**
     * Sets the options for all future compilations.
     */
    public ExpressionEvaluator
    options(EnumSet<JaninoOption> options) {
        this.se.options(options);
        return this;
    }

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

//    /**
//     * @throws IllegalArgumentException <var>count</var> is different from previous invocations of
//     *                                  this method
//     */
//    public void
//    setExpressionCount(int count) {
//
//        Expression[] ss = this.expressions;
//
//        if (ss == null) {
//            this.expressions = (ss = new Expression[count]);
//            for (int i = 0; i < count; i++) {
//                ss[i] = new Expression(ScriptEvaluator.DEFAULT_METHOD_NAME.replace("*", Integer.toString(i)));
//            }
//        } else {
//            if (count != ss.length) {
//                throw new IllegalArgumentException(
//                    "Inconsistent script count; previously " + ss.length + ", now " + count
//                );
//            }
//        }
//    }

//    private Expression
//    getExpression(int index) {
//        if (this.expressions != null) return this.expressions[index];
//        throw new IllegalStateException("\"getScript()\" invoked befor \"setScriptCount()\"");
//    }

    @Override public Method
    getMethod() { return this.se.getMethod(); }

    @Override public Method
    getMethod(int idx) { return this.se.getMethod(idx); }

    /**
     * @return                       The generated method
     * @throws IllegalStateException The {@link ScriptEvaluator} has not yet be cooked
     */
    @Override public Method[]
    getResult() { return this.se.getResult(); }

    @Override public void
    cook(String[] fileNames, Reader[] readers) throws CompileException, IOException {

        final int count = fileNames.length;

        Scanner[] scanners = new Scanner[count];
        for (int i = 0; i < count; i++) scanners[i] = new Scanner(fileNames[i], readers[i]);

        this.cook(scanners);
    }

    public final void
    cook(Scanner scanner) throws CompileException, IOException { this.cook(new Scanner[] { scanner }); }

    /**
     * Like {@link #cook(Scanner)}, but cooks a <em>set</em> of scripts into one class. Notice that if <em>any</em> of
     * the scripts causes trouble, the entire compilation will fail. If you need to report <em>which</em> of the
     * scripts causes the exception, you may want to use the {@code optionalFileName} argument of {@link
     * Scanner#Scanner(String, Reader)} to distinguish between the individual token sources.
     * <p>
     *   On a 2 GHz Intel Pentium Core Duo under Windows XP with an IBM 1.4.2 JDK, compiling 10000 expressions "a + b"
     *   (integer) takes about 4 seconds and 56 MB of main memory. The generated class file is 639203 bytes large.
     * </p>
     * <p>
     *   The number and the complexity of the scripts is restricted by the <a
     *   href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88659">Limitations of the Java
     *   Virtual Machine</a>, where the most limiting factor is the 64K entries limit of the constant pool. Since every
     *   method with a distinct name requires one entry there, you can define at best 32K (very simple) scripts.
     * </p>
     * <p>
     *   If and only if the number of scanners is one, then that single script may contain leading IMPORT directives.
     * </p>
     *
     * @throws IllegalStateException Any of the preceding {@code set...()} had an array size different from that of
     *                               {@code scanners}
     */
    public final void
    cook(Scanner[] scanners) throws CompileException, IOException {

        Parser[] parsers = new Parser[scanners.length];
        for (int i = 0; i < scanners.length; ++i) parsers[i] = new Parser(scanners[i]);

        this.cook(parsers);
    }

    /**
     * @see #cook(Scanner[])
     */
    public final void
    cook(Parser[] parsers) throws CompileException, IOException {

        int count = parsers.length;
        this.se.setScriptCount(count);

        String fileName = parsers.length >= 1 ? parsers[0].getScanner().getFileName() : null;

        // Parse import declarations.
        final Java.AbstractCompilationUnit.ImportDeclaration[]
        importDeclarations = this.se.parseImports(parsers.length == 1 ? parsers[0] : null);

        Java.BlockStatement[][]   statementss   = new Java.BlockStatement[count][];
        Java.MethodDeclarator[][] localMethodss = new Java.MethodDeclarator[count][];

        // Create methods with one block each.
        for (int i = 0; i < parsers.length; ++i) {

            Class<?> et     = this.se.getReturnType(i);
            Parser   parser = parsers[i];

            // Parse the expression.
            Java.Rvalue value = parser.parseExpression().toRvalueOrCompileException();

            Java.BlockStatement statement;
            if (et == void.class) {
                statement = new Java.ExpressionStatement(value);
            } else {
                statement = new Java.ReturnStatement(parser.location(), value);
            }

            if (!parser.peek(TokenType.END_OF_INPUT)) {
                throw new CompileException("Unexpected token \"" + parser.peek() + "\"", parser.location());
            }

            statementss[i]   = new Java.BlockStatement[] { statement };
            localMethodss[i] = new Java.MethodDeclarator[0];
        }

        this.se.cook(fileName, importDeclarations, statementss, localMethodss);
    }

    /**
     * Converts an array of {@link Class}es into an array of{@link Java.Type}s.
     */
    protected Java.Type[]
    classesToTypes(Location location, @Nullable Class<?>[] classes) {

        if (classes == null) return new Java.Type[0];

        Java.Type[] types = new Java.Type[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            types[i] = this.classToType(location, classes[i]);
        }
        return types;
    }

    /**
     * Wraps a reflection {@link Class} in a {@link Java.Type} object.
     */
    @Nullable protected Java.Type
    optionalClassToType(final Location location, @Nullable final Class<?> clazz) {
        if (clazz == null) return null;
        return this.classToType(location, clazz);
    }

    /**
     * Wraps a reflection {@link Class} in a {@link Java.Type} object.
     */
    protected Java.Type
    classToType(final Location location, final Class<?> clazz) { return this.se.classToType(location, clazz); }

    @Override @Nullable public Object
    evaluate(@Nullable Object[] arguments) throws InvocationTargetException { return this.evaluate(0, arguments); }

    @Override @Nullable public Object
    evaluate(int idx, @Nullable Object[] arguments) throws InvocationTargetException {

        Method method = this.getMethod(idx);

        try {
            return method.invoke(null, arguments);
        } catch (IllegalAccessException ex) {
            throw new InternalCompilerException(ex.toString(), ex);
        }
    }

    @Override public Class<?>
    getClazz() { return this.se.getClazz(); }

    @Override public Map<String, byte[]>
    getBytecodes() { return this.se.getBytecodes(); }

    /**
     * @deprecated Use {@link #createFastEvaluator(String, Class, String[])} instead:
     */
    @Deprecated public static Object
    createFastExpressionEvaluator(
        String                expression,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException {
        IExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setParentClassLoader(optionalParentClassLoader);
        return ee.createFastEvaluator(expression, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated Use {@link #createFastEvaluator(Reader, Class, String[])} instead
     */
    @Deprecated public static Object
    createFastExpressionEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    optionalExtendedType,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setClassName(className);
        ee.setExtendedClass(optionalExtendedType);
        ee.setParentClassLoader(optionalParentClassLoader);
        return ee.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated Use {@link #createFastEvaluator(Reader, Class, String[])} instead
     */
    @Deprecated public static Object
    createFastExpressionEvaluator(
        Scanner               scanner,
        String[]              defaultImports,
        String                className,
        @Nullable Class<?>    extendedType,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setClassName(className);
        ee.setExtendedClass(extendedType);
        ee.setDefaultImports(defaultImports);
        ee.setParentClassLoader(optionalParentClassLoader);
        return ee.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    @Override public <T> T
    createFastEvaluator(Reader reader, Class<T> interfaceToImplement, String... parameterNames)
    throws CompileException, IOException {
        return this.createFastEvaluator(new Scanner(null, reader), interfaceToImplement, parameterNames);
    }

    @Override public <T> T
    createFastEvaluator(String script, Class<T> interfaceToImplement, String... parameterNames) throws CompileException {
        try {
            return this.createFastEvaluator(
                new StringReader(script),
                interfaceToImplement,
                parameterNames
            );
        } catch (IOException ex) {
            throw new InternalCompilerException("IOException despite StringReader", ex);
        }
    }

    /**
     * Notice: This method is not declared in {@link IScriptEvaluator}, and is hence only available in <em>this</em>
     * implementation of {@code org.codehaus.commons.compiler}. To be independent from this particular
     * implementation, try to switch to {@link #createFastEvaluator(Reader, Class, String[])}.
     *
     * @param scanner Source of tokens to read
     * @see #createFastEvaluator(Reader, Class, String[])
     */
    public <T> T
    createFastEvaluator(Scanner scanner, Class<T> interfaceToImplement, String[] parameterNames)
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
        if (this instanceof IExpressionEvaluator) {

            // Must not call "IExpressionEvaluator.setReturnType()".
            ((IExpressionEvaluator) this).setExpressionType(methodToImplement.getReturnType());
        } else {
            this.setExpressionType(methodToImplement.getReturnType());
        }
        this.setMethodName(methodToImplement.getName());
        this.setParameters(parameterNames, methodToImplement.getParameterTypes());
        this.setThrownExceptions(methodToImplement.getExceptionTypes());
        this.cook(scanner);

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

    /**
     * Guess the names of the parameters used in the given expression. The strategy is to look
     * at all "ambiguous names" in the expression (e.g. in "a.b.c.d()", the ambiguous name
     * is "a.b.c"), and then at the first components of the ambiguous name.
     * <ul>
     *   <li>If any component starts with an upper-case letter, then ambiguous name is assumed to be a type name.</li>
     *   <li>Otherwise, it is assumed to be a parameter name.</li>
     * </ul>
     *
     * @see Scanner#Scanner(String, Reader)
     */
    public static String[]
    guessParameterNames(Scanner scanner) throws CompileException, IOException {
        Parser parser = new Parser(scanner);

        // Eat optional leading import declarations.
        while (parser.peek("import")) parser.parseImportDeclaration();

        // Parse the expression.
        Java.Rvalue rvalue = parser.parseExpression().toRvalueOrCompileException();
        if (!parser.peek(TokenType.END_OF_INPUT)) {
            throw new CompileException("Unexpected token \"" + parser.peek() + "\"", scanner.location());
        }

        // Traverse the expression for ambiguous names and guess which of them are parameter names.
        final Set<String> parameterNames = new HashSet<String>();
        new AbstractTraverser<RuntimeException>() {

            @Override public void
            traverseAmbiguousName(Java.AmbiguousName an) {

                // If any of the components starts with an upper-case letter, then the ambiguous
                // name is most probably a type name, e.g. "System.out" or "java.lang.System.out".
                for (String identifier : an.identifiers) {
                    if (Character.isUpperCase(identifier.charAt(0))) return;
                }

                // It's most probably a parameter name (although it could be a field name as well).
                parameterNames.add(an.identifiers[0]);
            }
        }.visitAtom(rvalue);

        return (String[]) parameterNames.toArray(new String[parameterNames.size()]);
    }
}
