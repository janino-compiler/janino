
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

package org.codehaus.janino;

import java.io.*;

/**
 * An expression evaluator that evaluates expressions in Java<sup>TM</sup> bytecode.
 * <p>
 * The syntax of the expression to compile is that of a Java<sup>TM</sup> expression, as defined
 * in the <a href="http://java.sun.com/docs/books/jls/second_edition">Java Language Specification,
 * 2nd edition</a>, section
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#44393">15</a>.
 * Notice that a Java<sup>TM</sup> expression does not have a concluding semicolon.
 * <p>
 * Example:<pre>
 *   a + 7 * b</pre>
 * (Notice that this expression refers to two parameters "a" and "b", as explained below.)
 * <p>
 * The expression may optionally be preceeded with a sequence of import directives like
 * <pre>
 *   import java.text.*;
 *   new DecimalFormat("####,###.##").format(10200020.345345)
 * </pre>
 * (Notice that the import directive is concluded with a semicolon, while the expression is not.)
 * <p>
 * The expression evaluator is implemented by creating and compiling a temporary compilation unit
 * defining one class with one static method with one return statement.
 * <p>
 * To set up an {@link ExpressionEvaluator} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create the {@link ExpressionEvaluator} using {@link #ExpressionEvaluator()}
 *   <li>
 *   Configure the {@link ExpressionEvaluator} by calling any of the following methods:
 *   <ul>
 *      <li>{@link #setExpressionType(Class)}
 *      <li>{@link org.codehaus.janino.ScriptEvaluator#setParameters(String[], Class[])}
 *      <li>{@link org.codehaus.janino.ScriptEvaluator#setThrownExceptions(Class[])}
 *      <li>{@link org.codehaus.janino.SimpleCompiler#setParentClassLoader(ClassLoader)}
 *      <li>{@link org.codehaus.janino.ClassBodyEvaluator#setDefaultImports(String[])}
 *   </ul>
 *   <li>
 *   Call any of the {@link org.codehaus.janino.Cookable#cook(Scanner)} methods to scan,
 *   parse, compile and load the expression into the JVM.
 * </ol>
 * Alternatively, a number of "convenience constructors" exist that execute the steps described
 * above instantly.
 * <p>
 * After the {@link ExpressionEvaluator} object is set up, the expression can be evaluated as
 * often with different parameter values (see {@link #evaluate(Object[])}). This evaluation is
 * very fast, compared to the setup.
 * <p>
 * Notice that for <i>functionally</i> identical {@link ExpressionEvaluator}s,
 * {@link java.lang.Object#equals(java.lang.Object)} will return <code>true</code>. E.g. "a+b" and
 * "c + d" are functionally identical if "a" and "c" have the same type, and so do "b" and "d".
 * <p>
 * If the parameter and return types of the expression are known at compile time, then a "fast"
 * expression evaluator can be instantiated through
 * {@link #createFastExpressionEvaluator(String, Class, String[], ClassLoader)}. Expression
 * evaluation is faster than through {@link #evaluate(Object[])}, because it is not done through
 * reflection but through direct method invocation.
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
 * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
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
 * <p>
 * The expression may refer to a set of parameters with the given
 * <code>parameterNames</code> and <code>parameterTypes</code>.
 * <p>
 * <code>parameterNames</code> and <code>parameterTypes</code> must have the
 * same number of elements.
 * <p>
 * The parameters and/or the return value can be of primitive type, e.g.
 * {@link Double#TYPE}.
 * <p>
 * The <code>optionalClassLoader</code> serves two purposes:
 * <ul>
 *   <li>It is used to look for classes referenced by the script.
 *   <li>It is used to load the generated Java<sup>TM</sup> class
 *   into the JVM; directly if it is a subclass of {@link
 *   ByteArrayClassLoader}, or by creation of a temporary
 *   {@link ByteArrayClassLoader} if not.
 * </ul>
 * If the <code>optionalClassLoader</code> is <code>null</code>, then the
 * current thread's context class loader is used.
 * <p>
 * A number of constructors exist that provide useful default values for
 * the various parameters, or parse their script from a {@link String}
 * instead of a {@link Scanner}. (You hardly want to use a scanner other than
 * the default scanner.)
 * <p>
 * If the type of the expression is not fixed, you can pass a <code>null</code>
 * <code>optionalExpressionType<code> argument; in this case, references are
 * returned as {@link Object}s, and primitive values are wrapped in their
 * wrapper classes.
 * <p>
 * If <code>optionalExpressionType</code> is {@link Void#TYPE}, then the
 * expression must be an invocation of a <code>void</code> method.
 */
public class ExpressionEvaluator extends ScriptEvaluator {
    public static final Class ANY_TYPE = null;
    private Class             expressionType = ExpressionEvaluator.ANY_TYPE;

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
    public ExpressionEvaluator(
        String   expression,
        Class    expressionType,
        String[] parameterNames,
        Class[]  parameterTypes
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
    public ExpressionEvaluator(
        String      expression,
        Class       expressionType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
     * @see ClassBodyEvaluator#setExtendedType(Class)
     * @see ClassBodyEvaluator#setImplementedTypes(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String)
     */
    public ExpressionEvaluator(
        String      expression,
        Class       expressionType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        this.setExpressionType(expressionType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(expression);
    }

    /**
     * Equivalent to<pre>
     * ExpressionEvaluator ee = new ExpressionEvaluator();
     * ee.setClassName(className);
     * ee.setExtendedType(optionalExtendedType);
     * ee.setImplementedTypes(implementedTypes);
     * ee.setStaticMethod(staticMethod);
     * ee.setExpressionType(expressionType);
     * ee.setMethodName(methodName);
     * ee.setParameters(parameterNames, parameterTypes);
     * ee.setThrownExceptions(thrownExceptions);
     * ee.setParentClassLoader(optionalParentClassLoader);
     * ee.cook(scanner);
     *
     * @see #ExpressionEvaluator()
     * @see ClassBodyEvaluator#setClassName(String)
     * @see ClassBodyEvaluator#setExtendedType(Class)
     * @see ClassBodyEvaluator#setImplementedTypes(Class[])
     * @see ScriptEvaluator#setStaticMethod(boolean)
     * @see ExpressionEvaluator#setExpressionType(Class)
     * @see ScriptEvaluator#setMethodName(String)
     * @see ScriptEvaluator#setParameters(String[], Class[])
     * @see ScriptEvaluator#setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
     */
    public ExpressionEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        boolean     staticMethod,
        Class       expressionType,
        String      methodName,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        this.setClassName(className);
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
        this.setStaticMethod(staticMethod);
        this.setExpressionType(expressionType);
        this.setMethodName(methodName);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public ExpressionEvaluator() {}

    /**
     * Define the type of the expression. The special type {@link #ANY_TYPE} allows the expression
     * to return any type (primitive or reference).
     * <p>
     * Defaults to {@link #ANY_TYPE}.
     */
    public void setExpressionType(Class expressionType) {
        this.expressionType = expressionType;
    }

    public final void setReturnType(Class returnType) {
        throw new RuntimeException("Don't call \"setReturnType()\" on an ExpressionEvaluator, user \"setExpressionType()\" instead");
    }

    protected void internalCook(Scanner scanner) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(scanner);

        // Create class, method and block.
        Java.Block block = this.addClassMethodBlockDeclaration(
            scanner.location(),        // location
            compilationUnit,           // compilationUnit
            (                          // returnType
                this.expressionType == null ?
                Object.class :
                this.expressionType
            )
        );

        Parser parser = new Parser(scanner);
        if (this.expressionType == void.class) {

            // ExpressionEvaluator with an expression type "void" is a simple expression statement.
            block.addStatement(new Java.ExpressionStatement(parser.parseExpression().toRvalueOrPE()));
        } else {

            // Compute expression value
            Java.Rvalue value = parser.parseExpression().toRvalueOrPE();

            // Special case: A "null" expression type means return type "Object" and automatic
            // wrapping of primitive types.
            if (this.expressionType == null) {
                value = new Java.MethodInvocation(
                    scanner.location(),           // location
                    new Java.ReferenceType(       // optionalTarget
                        scanner.location(),                                                      // location
                        new String[] { "org", "codehaus", "janino", "util", "PrimitiveWrapper" } // identifiers
                    ),
                    "wrap",
                    new Java.Rvalue[] { value }   // arguments
                );
                org.codehaus.janino.util.PrimitiveWrapper.wrap(99); // Make sure "PrimitiveWrapper" is compiled.
            }

            // Add a return statement.
            block.addStatement(new Java.ReturnStatement(scanner.location(), value));
        }
        if (!scanner.peek().isEOF()) throw new Parser.ParseException("Unexpected token \"" + scanner.peek() + "\"", scanner.location());

        this.compileToMethod(compilationUnit);
    }

    /**
     * Creates a "fast expression evaluator" from the given {@link java.lang.String}
     * <code>expression</code>, generating a class with the {@link #DEFAULT_CLASS_NAME} that
     * extends {@link Object}.
     * <p>
     * See the class description for an explanation of the "fast expression evaluator" concept.
     *
     * @see #createFastExpressionEvaluator(Scanner, String[], String, Class, Class, String[], ClassLoader)
     * @see ExpressionEvaluator
     */
    public static Object createFastExpressionEvaluator(
        String      expression,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        return ScriptEvaluator.createFastEvaluator(ee, expression, parameterNames, interfaceToImplement);
    }

    /**
     * Creates a "fast expression evaluator" from the given {@link Scanner} with no default
     * imports.
     * <p>
     * See the class description for an explanation of the "fast expression evaluator" concept.
     *
     * @see #createFastExpressionEvaluator(Scanner, String[], String, Class, Class, String[], ClassLoader)
     * @see ExpressionEvaluator
     */
    public static Object createFastExpressionEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setClassName(className);
        ee.setExtendedType(optionalExtendedType);
        ee.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(ee, scanner, parameterNames, interfaceToImplement);
    }

    /**
     * Creates a "fast expression evaluator".
     * <p>
     * See the class description for an explanation of the "fast expression evaluator" concept.
     * <p> 
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the same package as <code>className</code>.
     * 
     * @param scanner                   Source of expression tokens
     * @param optionalDefaultImports    Default imports, e.g. <code>{ "java.util.Map", "java.io.*" }</code>
     * @param className                 Name of generated class
     * @param optionalExtendedType      Class to extend
     * @param interfaceToImplement      Must declare exactly the one method that defines the expression's signature
     * @param parameterNames            The expression references the parameters through these names
     * @param optionalParentClassLoader Used to load referenced classes, defaults to the current thread's "context class loader"
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     * @see ExpressionEvaluator
     */
    public static Object createFastExpressionEvaluator(
        Scanner     scanner,
        String[]    optionalDefaultImports,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setClassName(className);
        ee.setExtendedType(optionalExtendedType);
        ee.setDefaultImports(optionalDefaultImports);
        ee.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(ee, scanner, parameterNames, interfaceToImplement);
    }
}
