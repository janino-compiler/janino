
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
import java.lang.reflect.*;

/**
 * An expression evaluator that evaluates expressions in Java<sup>TM</sup> bytecode.
 * <p>
 * The syntax of the expression to compile is that of a Java<sup>TM</sup> expression, as defined
 * in the <a href="http://java.sun.com/docs/books/jls/second_edition">Java Language Specification,
 * 2nd edition</a>, section
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#44393">15</a>.
 * Notice that a Java<sup>TM</sup> expression does not have a
 * concluding semicolon.
 * <p>
 * Example:
 * <pre>
 *   a + 7 * b
 * </pre>
 * (Notice that this expression refers to two parameters "a" and "b", as explained below.)
 * <p>
 * The expression may optionally be preceeded with a sequence of import directives like
 * <pre>
 *   import java.text.*;
 *   new DecimalFormat("####,###.##").format(10200020.345345)
 * </pre>
 * (Notice that the import directive is concluded with a semicolon, while the expression is not.)
 * <p>
 * The expression is compiled when the {@link ExpressionEvaluator} object is instantiated. The
 * expression, its type, and its parameter names and types are specified at compile time.
 * <p>
 * The expression evaluator is implemented by creating and compiling a temporary compilation unit
 * defining one class with one static method with one return statement.
 * <p>
 * After the {@link ExpressionEvaluator} object is created, the expression can be evaluated as
 * often with different parameter values (see {@link #evaluate(Object[])}). This evaluation is
 * very fast, compared to the compilation.
 * <p>
 * The more elaborate constructors of {@link ExpressionEvaluator} also allow for the specification
 * of the name of the generated class, the class it extends, the interfaces it implements, the
 * name of the method that evaluates the expression, the exceptions that this method is allowed
 * to throw, and the {@link ClassLoader} that is used to define the generated class
 * and to load classes referenced by the expression. This degree of flexibility is usually not
 * required; the most commonly used constructor is
 * {@link #ExpressionEvaluator(String, Class, String[], Class[])}.
 * <p>
 * Notice that for <i>functionally</i> identical {@link ExpressionEvaluator}s,
 * {@link java.lang.Object#equals(java.lang.Object)} will return <code>true</code>. E.g. "a+b" and
 * "c + d" are functionally identical if "a" and "c" have the same type, and so do "b" and "d". 
 */
public class ExpressionEvaluator extends EvaluatorBase {
    private static final String DEFAULT_METHOD_NAME = "eval";
    private static final String DEFAULT_CLASS_NAME = "SC";

    /**
     * Parse an expression from a {@link String} and compile it.
     * <p>
     * The expression must not throw any Exceptions other than {@link RuntimeException}.
     * @see #ExpressionEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ExpressionEvaluator(
        String   expression,
        Class    optionalExpressionType,
        String[] parameterNames,
        Class[]  parameterTypes
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super((ClassLoader) null);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(expression)), // scanner
                ExpressionEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                    // optionalExtendedType
                new Class[0],                                    // implementedTypes
                true,                                            // staticMethod
                optionalExpressionType,                          // optionalExpressionType
                ExpressionEvaluator.DEFAULT_METHOD_NAME,         // methodName
                parameterNames,                                  // parameterNames
                parameterTypes,                                  // parameterTypes
                new Class[0]                                     // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse an expression from a {@link String} and compile it.
     * @see #ExpressionEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ExpressionEvaluator(
        String      expression,
        Class       optionalExpressionType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader     // null == use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super(optionalParentClassLoader);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(expression)), // scanner
                ExpressionEvaluator.DEFAULT_CLASS_NAME,          // className
                (Class) null,                                    // optionalExtendedType
                new Class[0],                                    // implementedTypes
                true,                                            // staticMethod
                optionalExpressionType,                          // optionalExpressionType
                ExpressionEvaluator.DEFAULT_METHOD_NAME,         // methodName
                parameterNames,                                  // parameterNames
                parameterTypes,                                  // parameterTypes
                thrownExceptions                                 // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse an expression from a {@link String} and compile it into a public static
     * method "eval" with the given return and parameter types.
     * 
     * @see #ExpressionEvaluator(Scanner, String, Class, Class[], boolean, Class, String, String[], Class[], Class[], ClassLoader)
     */
    public ExpressionEvaluator(
        String      expression,
        Class       optionalExpressionType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader    // null == use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        super(optionalParentClassLoader);
        try {
            this.scanParseCompileLoad(
                new Scanner(null, new StringReader(expression)), // scanner
                ExpressionEvaluator.DEFAULT_CLASS_NAME,          // className
                optionalExtendedType,                            // optionalExtendedType
                implementedTypes,                                // implementedTypes
                true,                                            // staticMethod
                optionalExpressionType,                          // optionalExpressionType
                ExpressionEvaluator.DEFAULT_METHOD_NAME,         // methodName
                parameterNames,                                  // parameterNames
                parameterTypes,                                  // parameterTypes
                thrownExceptions                                 // thrownExceptions
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Parse an expression from a sequence of {@link Scanner.Token}s delivered
     * by the given <code>scanner</code> and compile it.
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
     * If the <code>optionalClassLoader</code> is <code>null</code>, then the current thread's context
     * class loader is used.
     * <p>
     * A number of constructors exist that provide useful default values for
     * the various parameters, or parse their script from a {@link String}
     * instead of a {@link Scanner}. (You hardly want to use a scanner other than the default
     * scanner.)
     * <p>
     * If the type of the expression is not fixed, you can pass a <code>null</code>
     * <code>optionalExpressionType<code> argument; in this case, references are returned as
     * {@link Object}s, and primitive values are wrapped in their wrapper classes.
     * <p>
     * If <code>optionalExpressionType</code> is {@link Void#TYPE}, then the expression
     * must be an invocation of a <code>void</code> method.
     *
     * @param scanner Source of tokens to parse
     * @param className The name of the temporary class (uncritical)
     * @param optionalExtendedType The base class of the generated object or <code>null</code>
     * @param implementedTypes The interfaces that the the generated object implements (all methods must be implemented by the <code>optionalExtendedType</code>)
     * @param staticMethod Whether the generated method should be declared "static"
     * @param optionalExpressionType The type of the expression, e.g. {@link Double#TYPE}, <code>double.class</code>, {@link String}<code>.class</code>, {@link Void#TYPE}, or <code>null</code>.
     * @param methodName The name of the temporary method (uncritical)
     * @param parameterNames The names of the expression parameters, e.g. "i" and "j".
     * @param parameterTypes The types of the expression parameters, e.g. {@link Integer#TYPE}, {@link Writer}<code>.class</code> or {@link Double#TYPE}.
     * @param thrownExceptions The exceptions that the expression is allowed to throw, e.g. {@link IOException}<code>.class</code>.
     * @param optionalParentClassLoader Loads referenced classes
     */
    public ExpressionEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        boolean     staticMethod,
        Class       optionalExpressionType, // null == automagically wrap the expression result
        String      methodName,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader    // null == use current thread's context class loader
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        super(optionalParentClassLoader);
        this.scanParseCompileLoad(
            scanner,                // scanner
            className,              // className
            optionalExtendedType,   // optionalExtendedType
            implementedTypes,       // implementedTypes
            staticMethod,           // staticMethod
            optionalExpressionType, // optionalExpressionType
            methodName,             // methodName
            parameterNames,         // parameterNames
            parameterTypes,         // parameterTypes
            thrownExceptions        // thrownExceptions
        );
    }

    private void scanParseCompileLoad(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        boolean     staticMethod,
        Class       optionalExpressionType, // null == automagically wrap the expression result
        String      methodName,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        if (parameterNames.length != parameterTypes.length) throw new RuntimeException("Lengths of \"parameterNames\" and \"parameterTypes\" do not match");

        // Create a temporary compilation unit.
        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(scanner.peek().getLocation().getFileName());

        // Parse import declarations.
        this.parseImportDeclarations(compilationUnit, scanner);

        // Create class, method and block.
        Java.Block block = this.addClassMethodBlockDeclaration(
            scanner.peek().getLocation(),           // location
            compilationUnit,                        // compilationUnit
            className,                              // className
            optionalExtendedType, implementedTypes, // optionalExtendedType, implementedTypes
            staticMethod,                           // staticMethod
            (                                       // returnType
                optionalExpressionType == null ?
                Object.class :
                optionalExpressionType
            ),
            methodName,                             // methodName
            parameterNames, parameterTypes,         // parameterNames, parameterTypes
            thrownExceptions                        // thrownExceptions
        );

        Parser parser = new Parser(scanner);
        if (optionalExpressionType == void.class) {

            // ExpressionEvaluator with a expression type "void" is a simple expression statement.
            block.addStatement(new Java.ExpressionStatement(
                parser.parseExpression(block).toRvalueOrPE(),
                block
            ));
        } else {

            // Compute expression value
            Java.Rvalue value = parser.parseExpression((Java.BlockStatement) block).toRvalueOrPE();

            // Special case: A "null" expression type means return type "Object" and automatic
            // wrapping of primitive types.
            if (optionalExpressionType == null) {
                value = new Java.MethodInvocation(
                    scanner.peek().getLocation(), // location
                    (Java.BlockStatement) block,  // enclosingBlockStatement
                    new Java.ReferenceType(       // optionalTarget
                        scanner.peek().getLocation(),
                        (Java.Scope) block,
                        new String[] { "org", "codehaus", "janino", "util", "PrimitiveWrapper" }
                    ),
                    "wrap",
                    new Java.Rvalue[] { value }   // arguments
                );
                org.codehaus.janino.util.PrimitiveWrapper.wrap(99); // Make sure "PrimitiveWrapper" is compiled.
            }

            // Add a return statement.
            block.addStatement(new Java.ReturnStatement(
                scanner.peek().getLocation(), // location
                block,                        // enclosingBlock
                value                         // returnValue
            ));
        }
        if (!scanner.peek().isEOF()) throw new Parser.ParseException("Unexpected token \"" + scanner.peek() + "\"", scanner.peek().getLocation());

        // Compile and load it.
        Class c;
        try {
            c = this.compileAndLoad(compilationUnit, DebuggingInformation.NONE, className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }

        // Find method by name.
        try {
            this.method = c.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * If the parameter and return types of the expression are known at compile time,
     * then a "fast" expression evaluator can be instantiated through this method.
     * <p>
     * Expression evaluation is faster than through {@link #evaluate(Object[])}, because
     * it is not done through reflection but through direct method invocation.
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
     * @param expression Uses the parameters of the method declared by "interfaceToImplement"
     * @param interfaceToImplement Must declare exactly one method
     * @param parameterNames The expression references the parameters through these names
     * @param optionalClassLoader
     * @return an object that implements the given interface
     */
    public static Object createFastExpressionEvaluator(
        String      expression,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        try {
            return ExpressionEvaluator.createFastExpressionEvaluator(
                new Scanner(null, new StringReader(expression)), // scanner
                ExpressionEvaluator.DEFAULT_CLASS_NAME,          // className
                null,                                            // optionalExtendedType
                interfaceToImplement,                            // interfaceToImplement
                parameterNames,                                  // parameterNames
                optionalClassLoader                              // optionalClassLoader
            );
        } catch (IOException ex) {
            throw new RuntimeException("SNO: StringReader throws IOException!?");
        }
    }

    /**
     * Like {@link #createFastExpressionEvaluator(String, Class, String[], ClassLoader)},
     * but gives you more control over the generated class (rarely needed in practice).
     * <p> 
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the same package as <code>className</code>.
     * 
     * @param scanner Source of expression tokens
     * @param className Name of generated class
     * @param optionalExtendedType Class to extend
     * @param interfaceToImplement Must declare exactly the one method that defines the expression's signature
     * @param parameterNames The expression references the parameters through these names
     * @param optionalParentClassLoader Loads referenced classes
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     */
    public static Object createFastExpressionEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        if (!interfaceToImplement.isInterface()) throw new RuntimeException("\"" + interfaceToImplement + "\" is not an interface");

        Method[] methods = interfaceToImplement.getDeclaredMethods();
        if (methods.length != 1) throw new RuntimeException("Interface \"" + interfaceToImplement + "\" must declare exactly one method");
        Method methodToImplement = methods[0];

        ExpressionEvaluator ee = new ExpressionEvaluator(
            scanner,                               // scanner
            className,                             // className
            optionalExtendedType,                  // optionalExtendedType
            new Class[] { interfaceToImplement },  // implementedTypes
            false,                                 // staticMethod
            methodToImplement.getReturnType(),     // optionalExpressionType
            methodToImplement.getName(),           // methodName
            parameterNames,                        // parameterNames
            methodToImplement.getParameterTypes(), // parameterTypes
            methodToImplement.getExceptionTypes(), // thrownExceptions
            optionalParentClassLoader              // optionalParentClassLoader
        );
        try {
            return ee.getMethod().getDeclaringClass().newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new RuntimeException(e.toString());
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Evaluates an expression with concrete parameter values.
     *
     * <p>
     *   Each parameter value must have the same type as specified through
     *   the "parameterTypes" parameter of {@link #ExpressionEvaluator(String,
     *   Class, String[], Class[], Class[], ClassLoader)}.
     * </p>
     * <p>
     *   Parameters of primitive type must passed with their wrapper class
     *   objects.
     * </p>
     * <p>
     *   The object returned has the class specified through the "returnType"
     *   parameter of {@link #ExpressionEvaluator(String, Class, String[],
     *   Class[], Class[], ClassLoader)}. If the "returnType" is primitive
     *   (e.g. "int.class"), the return value is returned through a wrapper
     *   object ("Integer").
     * </p>
     * <p>
     *   This method is thread-safe.
     * </p>
     *
     * @param parameterValues The concrete parameter values.
     */
    public Object evaluate(Object[] parameterValues) throws InvocationTargetException {
        try {
            return this.method.invoke(null, parameterValues);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * If, for any reason, somebody needs the
     * {@link Method} object...
     */
    public Method getMethod() {
        return this.method;
    }

    private Method method;
}
