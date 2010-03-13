
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

import java.io.*;
import java.lang.reflect.*;

/**
 * An engine that executes a script in Java<sup>TM</sup> bytecode.
 * <p>
 * The syntax of the script to compile is a sequence of import declarations (not allowed if you
 * compile many scripts at a time, see below) followed by a
 * sequence of statements, as defined in the
 * <a href="http://java.sun.com/docs/books/jls/second_edition">Java Language Specification, 2nd
 * edition</a>, sections
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/packages.doc.html#70209">7.5</a>
 * and
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/statements.doc.html#101241">14</a>.
 * <p>
 * Example:
 * <pre>
 *   import java.text.*;
 *
 *   System.out.println("HELLO");
 *   System.out.println(new DecimalFormat("####,###.##").format(a));
 * </pre>
 * (Notice that this expression refers to a parameter "a", as explained below.)
 * <p>
 * The script may complete abnormally, e.g. through a RETURN statement:
 * <pre>
 *   if (a == null) {
 *       System.out.println("Oops!");
 *       return;
 *   }
 * </pre>
 * Optionally, the script may be declared with a non-void return type. In this case, the last
 * statement of the script must be a RETURN statement (or a THROW statement), and all RETURN
 * statements in the script must return a value with the given type.
 * <p>
 * The script evaluator is implemented by creating and compiling a temporary compilation unit
 * defining one class with one method the body of which consists of the statements of the
 * script.
 * <p>
 * To set up a {@link IScriptEvaluator} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create an {@link IScriptEvaluator}-implementing class.
 *   <li>
 *   Configure the {@link IScriptEvaluator} by calling any of the following methods:
 *   <ul>
 *      <li>{@link #setReturnType(Class)}
 *      <li>{@link #setParameters(String[], Class[])}
 *      <li>{@link #setThrownExceptions(Class[])}
 *      <li>{@link org.codehaus.janino.SimpleCompiler#setParentClassLoader(ClassLoader)}
 *      <li>{@link org.codehaus.janino.ClassBodyEvaluator#setDefaultImports(String[])}
 *   </ul>
 *   <li>
 *   Call any of the {@link org.codehaus.commons.compiler.Cookable#cook(Scanner)} methods to scan,
 *   parse, compile and load the script into the JVM.
 * </ol>
 * After the {@link IScriptEvaluator} object is created, the script can be executed as often with
 * different parameter values (see {@link #evaluate(Object[])}). This execution is very fast,
 * compared to the compilation.
 * <p>
 * Less common methods exist that allow for the specification of the name of the generated class,
 * the class it extends, the interfaces it implements, the name of the method that executes the
 * script, the exceptions that this method (i.e. the script) is allowed to throw, and the
 * {@link ClassLoader} that is used to define the generated class and to load classes referenced by
 * the script.
 * <p>
 * If you want to compile many scripts at the same time, you have the option to cook an
 * <i>array</i> of scripts in one {@link IScriptEvaluator} by using the following methods:
 * <ul>
 *   <li>{@link #setMethodNames(String[])}
 *   <li>{@link #setParameters(String[][], Class[][])}
 *   <li>{@link #setReturnTypes(Class[])}
 *   <li>{@link #setStaticMethod(boolean[])}
 *   <li>{@link #setThrownExceptions(Class[][])}
 *   <li>{@link #cook(Scanner[])}
 *   <li>{@link #evaluate(int, Object[])}
 * </ul>
 * Notice that these methods have array parameters in contrast to their one-script brethren.
 */
public interface IScriptEvaluator extends IClassBodyEvaluator {

    /**
     * Define whether the generated method should be STATIC or not. Defaults to <code>true</code>.
     */
    public abstract void setStaticMethod(boolean staticMethod);

    /**
     * Define the return type of the generated method. Defaults to <code>void.class</code>.
     */
    public abstract void setReturnType(Class returnType);

    /**
     * Define the name of the generated method. Defaults to an unspecified name.
     */
    public abstract void setMethodName(String methodName);

    /**
     * Define the names and types of the parameters of the generated method.
     * <p>
     * <code>parameterNames</code> and <code>parameterTypes</code> must have the same number of
     * elements.
     * <p>
     * The parameters and/or the return value can be of primitive type, e.g. {@link Double#TYPE}.
     */
    public abstract void setParameters(String[] parameterNames, Class[] parameterTypes);

    /**
     * Define the exceptions that the generated method may throw.
     */
    public abstract void setThrownExceptions(Class[] thrownExceptions);

    /**
     * Calls the script with concrete parameter values.
     * <p>
     * Each parameter value must have the same type as specified through the "parameterTypes"
     * parameter of {@link #setParameters(String[], Class[])}.
     * <p>
     * Parameters of primitive type must passed with their wrapper class objects.
     * <p>
     * The object returned has the class as specified through {@link #setReturnType(Class)}.
     * <p>
     * This method is thread-safe.
     *
     * @param parameterValues The concrete parameter values.
     */
    public abstract Object evaluate(Object[] parameterValues) throws InvocationTargetException;

    /**
     * Returns the loaded {@link java.lang.reflect.Method}.
     * <p>
     * This method must only be called after exactly one of the {@link #cook(String, Reader)}
     * methods was called.
     */
    public abstract Method getMethod();

    /**
     * Same as {@link #setStaticMethod(boolean)}, but for multiple scripts.
     */
    public abstract void setStaticMethod(boolean[] staticMethod);

    /**
     * Same as {@link #setReturnType(Class)}, but for multiple scripts.
     */
    public abstract void setReturnTypes(Class[] returnTypes);

    /**
     * Same as {@link #setMethodName(String)}, but for multiple scripts.
     * <p>
     * Define the names of the generated methods. By default the methods have distinct and
     * implementation-specific names.
     * <p>
     * If two scripts have the same name, then they must have different parameter types
     * (see {@link #setParameters(String[][], Class[][])}).
     */
    public abstract void setMethodNames(String[] methodNames);

    /**
     * Same as {@link #setParameters(String[], Class[])}, but for multiple scripts.
     */
    public abstract void setParameters(String[][] parameterNames, Class[][] parameterTypes);

    /**
     * Same as {@link #setThrownExceptions(Class[])}, but for multiple scripts.
     */
    public abstract void setThrownExceptions(Class[][] thrownExceptions);

    /**
     * Same as {@link #cook(Reader)}, but for multiple scripts.
     */
    public abstract void cook(Reader[] readers) throws CompileException, ParseException,
        ScanException, IOException;

    /**
     * Same as {@link #cook(String, Reader)}, but for multiple scripts.
     */
    public abstract void cook(String[] optionalFileNames, Reader[] readers)
        throws CompileException, ParseException, ScanException, IOException;

    /**
     * Same as {@link #cook(String)}, but for multiple scripts.
     */
    public abstract void cook(String[] strings) throws CompileException, ParseException,
        ScanException;

    /**
     * Same as {@link #evaluate(Object[])}, but for multiple scripts.
     */
    public abstract Object evaluate(int idx, Object[] parameterValues)
        throws InvocationTargetException;

    /**
     * Same as {@link #getMethod()}, but for multiple scripts.
     */
    public abstract Method getMethod(int idx);
}