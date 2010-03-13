
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
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
import java.util.*;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.ParseException;
import org.codehaus.commons.compiler.ScanException;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.util.PrimitiveWrapper;
import org.codehaus.janino.util.Traverser;

/**
 * This {@link IExpressionEvaluator} is implemented by creating and compiling a temporary
 * compilation unit defining one class with one static method with one RETURN statement.
 * <p>
 * A number of "convenience constructors" exist that execute the set-up steps described for {@link
 * IExpressionEvaluator} instantly.
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
 */
public class ExpressionEvaluator extends ScriptEvaluator implements IExpressionEvaluator {
    private Class[] optionalExpressionTypes = null;

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
    ) throws CompileException, ParseException, ScanException {
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
    ) throws CompileException, ParseException, ScanException {
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
    ) throws CompileException, ParseException, ScanException {
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
    ) throws ScanException, ParseException, CompileException, IOException {
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

    public void setExpressionType(Class expressionType) {
        this.setExpressionTypes(new Class[] { expressionType });
    }

    public void setExpressionTypes(Class[] expressionTypes) {
        assertNotCooked();
        this.optionalExpressionTypes = expressionTypes;

        Class[] returnTypes = new Class[expressionTypes.length];
        for (int i = 0; i < returnTypes.length; ++i) {
            Class et = expressionTypes[i];
            returnTypes[i] = et == ANY_TYPE ? Object.class : et;
        }
        super.setReturnTypes(returnTypes);
    }

    protected Class getDefaultReturnType() {
        return Object.class;
    }

    protected List/*<BlockStatement>*/ makeStatements(
        int     idx,
        Scanner scanner
    ) throws ParseException, ScanException, IOException {
        List/*<BlockStatement>*/ statements = new ArrayList();

        Parser parser = new Parser(scanner);

        // Parse the expression.
        Rvalue value = parser.parseExpression().toRvalueOrPE();

        Class et = this.optionalExpressionTypes == null ? ANY_TYPE : this.optionalExpressionTypes[idx];
        if (et == void.class) {

            // ExpressionEvaluator with an expression type "void" is a simple expression statement.
            statements.add(new Java.ExpressionStatement(value));
        } else {

            // Special case: Expression type "ANY_TYPE" means return type "Object" and automatic
            // wrapping of primitive types.
            if (et == ANY_TYPE) {
                value = new Java.MethodInvocation(
                    scanner.location(),         // location
                    new Java.ReferenceType(     // optionalTarget
                        scanner.location(),                                                      // location
                        new String[] { "org", "codehaus", "janino", "util", "PrimitiveWrapper" } // identifiers
                    ),
                    "wrap",                     // methodName
                    new Java.Rvalue[] { value } // arguments
                );

                // Make sure "PrimitiveWrapper" is compiled.
                PrimitiveWrapper.wrap(99);

                // Add "PrimitiveWrapper" as an auxiliary class.
                this.classToType(null, PrimitiveWrapper.class);
            }

            // Add a return statement.
            statements.add(new Java.ReturnStatement(scanner.location(), value));
        }
        if (!scanner.peek().isEOF()) throw new ParseException("Unexpected token \"" + scanner.peek() + "\"", scanner.location());

        return statements;
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
        ClassLoader optionalParentClassLoader
    ) throws CompileException, ParseException, ScanException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setParentClassLoader(optionalParentClassLoader);
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
    ) throws CompileException, ParseException, ScanException, IOException {
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
    ) throws CompileException, ParseException, ScanException, IOException {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setClassName(className);
        ee.setExtendedType(optionalExtendedType);
        ee.setDefaultImports(optionalDefaultImports);
        ee.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(ee, scanner, parameterNames, interfaceToImplement);
    }

    /**
     * Guess the names of the parameters used in the given expression. The strategy is to look
     * at all "ambiguous names" in the expression (e.g. in "a.b.c.d()", the ambiguous name
     * is "a.b.c"), and then at the first components of the ambiguous name.
     * <ul>
     *   <li>If any component starts with an upper-case letter, then ambiguous name is assumed to
     *       be a type name.
     *   <li>Otherwise, it is assumed to be a parameter name.
     * </ul>
     *
     * @see Scanner#Scanner(String, Reader)
     */
    public static String[] guessParameterNames(Scanner scanner) throws ParseException, ScanException, IOException {
        Parser parser = new Parser(scanner);

        // Eat optional leading import declarations.
        while (scanner.peek().isKeyword("import")) parser.parseImportDeclaration();

        // Parse the expression.
        Rvalue rvalue = parser.parseExpression().toRvalueOrPE();
        if (!scanner.peek().isEOF()) throw new ParseException("Unexpected token \"" + scanner.peek() + "\"", scanner.location());

        // Traverse the expression for ambiguous names and guess which of them are parameter names.
        final Set parameterNames = new HashSet();
        rvalue.accept((RvalueVisitor) new Traverser() {
            public void traverseAmbiguousName(AmbiguousName an) {

                // If any of the components starts with an upper-case letter, then the ambiguous
                // name is most probably a type name, e.g. "System.out" or "java.lang.System.out".
                for (int i = 0; i < an.identifiers.length; ++i) {
                    if (Character.isUpperCase(an.identifiers[i].charAt(0))) return;
                }

                // It's most probably a parameter name (although it could be a field name as well).
                parameterNames.add(an.identifiers[0]);
            }
        }.comprehensiveVisitor());

        return (String[]) parameterNames.toArray(new String[parameterNames.size()]);
    }
}
