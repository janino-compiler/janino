
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

package org.codehaus.commons.compiler;

import java.lang.reflect.InvocationTargetException;


/**
 * An engine that evaluates expressions in Java<sup>TM</sup> bytecode.
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
 * This feature is not available if you compile many expressions at a time (see below).
 * <p>
 * To set up an {@link IExpressionEvaluator} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create an {@link IExpressionEvaluator}-derived class
 *   <li>
 *   Configure the {@link IExpressionEvaluator} by calling any of the following methods:
 *   <ul>
 *     <li>{@link #setExpressionType(Class)}
 *     <li>{@link #setParameters(String[], Class[])}
 *     <li>{@link #setThrownExceptions(Class[])}
 *     <li>{@link #setParentClassLoader(ClassLoader)}
 *     <li>{@link #setDefaultImports(String[])}
 *   </ul>
 *   <li>
 *   Call any of the {@link #cook(String, java.io.Reader)} methods to scan,
 *   parse, compile and load the expression into the JVM.
 * </ol>
 * After the {@link IExpressionEvaluator} object is set up, the expression can be evaluated as
 * often with different parameter values (see {@link #evaluate(Object[])}). This evaluation is
 * very fast, compared to the compilation.
 * <p>
 * Less common methods exist that allow for the specification of the name of the generated class,
 * the class it extends, the interfaces it implements, the name of the method that executes the
 * expression, the exceptions that this method (i.e. the expression) is allowed to throw, and the
 * {@link ClassLoader} that is used to define the generated class and to load classes referenced by
 * the expression.
 * <p>
 * If you want to compile many expressions at the same time, you have the option to cook an
 * <i>array</i> of expressions in one {@link IExpressionEvaluator} by using the following methods:
 * <ul>
 *   <li>{@link #setMethodNames(String[])}
 *   <li>{@link #setParameters(String[][], Class[][])}
 *   <li>{@link #setExpressionTypes(Class[])}
 *   <li>{@link #setStaticMethod(boolean[])}
 *   <li>{@link #setThrownExceptions(Class[][])}
 *   <li>{@link #cook(String, java.io.Reader)}
 *   <li>{@link #evaluate(int, Object[])}
 * </ul>
 * Notice that these methods have array parameters in contrast to their one-expression brethren.
 * <p>
 * Notice that for <i>functionally</i> identical {@link IExpressionEvaluator}s,
 * {@link java.lang.Object#equals(java.lang.Object)} will return <code>true</code>. E.g. "a+b" and
 * "c + d" are functionally identical if "a" and "c" have the same type, and so do "b" and "d".
 */
public interface IExpressionEvaluator extends IScriptEvaluator {

    public static final Class ANY_TYPE = null;

    /**
     * Define the type of the expression. The special type {@link #ANY_TYPE} allows the expression
     * to return any type (primitive or reference).
     * <p>
     * If <code>expressionType</code> is {@link Void#TYPE}, then the expression must be an
     * invocation of a <code>void</code> method.
     * <p>
     * Defaults to {@link #ANY_TYPE}.
     */
    public abstract void setExpressionType(Class expressionType);

    public abstract void setExpressionTypes(Class[] expressionTypes);

    /**
     * Evaluates the expression with concrete parameter values.
     * <p>
     * Each parameter value must have the same type as specified through the "parameterTypes"
     * parameter of {@link #setParameters(String[], Class[])}.
     * <p>
     * Parameters of primitive type must passed with their wrapper class objects.
     * <p>
     * The object returned has the class as specified through {@link #setExpressionType(Class)}.
     * <p>
     * This method is thread-safe.
     *
     * @param parameterValues The concrete parameter values.
     */
    public abstract Object evaluate(Object[] parameterValues) throws InvocationTargetException;
}