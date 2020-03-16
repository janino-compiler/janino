
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

package org.codehaus.commons.compiler;

import java.io.IOException;
import java.io.Reader;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Parses a class body and returns it as a {@link Class} object ready for use with
 * <tt>java.lang.reflect</tt>.
 * <p>
 *   Example:
 * </p>
 * <pre>
 *   import java.util.*;
 *
 *   static private int a = 1;
 *   private int b = 2;
 *
 *   public void func(int c, int d) {
 *       return func2(c, d);
 *   }
 *
 *   private static void func2(int e, int f) {
 *       return e * f;
 *   }
 * </pre>
 * <p>
 *   To set up an {@link IClassBodyEvaluator} object, proceed as follows:
 * </p>
 * <ol>
 *   <li>Create an {@link IClassBodyEvaluator}-implementing class.</li>
 *   <li>Configure the {@link IClassBodyEvaluator} by calling any of the following methods:</li>
 *   <ul>
 *      <li>{@link #setDefaultImports(String[])}</li>
 *      <li>{@link #setClassName(String)}</li>
 *      <li>{@link #setExtendedClass(Class)}</li>
 *      <li>{@link #setImplementedInterfaces(Class[])}</li>
 *      <li>{@link #setParentClassLoader(ClassLoader)}</li>
 *   </ul>
 *   <li>
 *     Call any of the {@link ICookable#cook(String, java.io.Reader)} methods to scan, parse, compile and load the
 *     class body into the JVM.
 *   </li>
 * </ol>
 * <p>
 *   To compile a class body and immediately instantiate an object, the {@link #createInstance(Reader)} methods can be
 *   used.
 * </p>
 * <p>
 *   The generated class may optionally extend/implement a given type; the returned instance can safely be type-cast
 *   to that {@code optionalBaseType}.
 * </p>
 * <p>
 *   Example:
 * </p>
 * <pre>
 * public interface Foo {
 *     int bar(int a, int b);
 * }
 * ...
 * {@link IClassBodyEvaluator} cbe = {@link CompilerFactoryFactory}.{@link
 * CompilerFactoryFactory#getDefaultCompilerFactory() getDefaultCompilerFactory}(). {@link
 * ICompilerFactory#newClassBodyEvaluator() newClassBodyEvaluator}();
 * Foo f = (Foo) ClassBodyEvaluator.createFastClassBodyEvaluator(
 *     new Scanner(null, new StringReader("public int bar(int a, int b) { return a + b; }")),
 *     Foo.class,                  // Base type to extend/implement
 *     (ClassLoader) null          // Use current thread's context class loader
 * );
 * System.out.println("1 + 2 = " + f.bar(1, 2));
 * </pre>
 */
public
interface IClassBodyEvaluator extends ISimpleCompiler {

    /**
     * Default name for the generated class.
     */
    String DEFAULT_CLASS_NAME = "SC";

    /**
     * "Default imports" add to the system import "java.lang", i.e. the evaluator may refer to classes imported by
     * default imports without having to explicitly declare IMPORT statements.
     * <p>
     *   Notice that JDK 5 "static imports" are also supported, as shown here:
     * </p>
     * <pre>
     *     sc.setDefaultImports(
     *         "java.util.Map",                          // Single type import
     *         "java.io.*",                              // Type-import-on-demand
     *         "static java.util.Collections.EMPTY_MAP", // Single static import
     *         "static java.util.Collections.*",         // Static-import-on-demand
     *     );</pre>
     * <p>
     *   Passing {@code null} as the argument is equivalent with {@code new String[0]}, i.e. <em>no</em> default
     *   imports.
     * </p>
     */
    void setDefaultImports(@Nullable String... optionalDefaultImports);

    /**
     * Sets the name of the generated class. Defaults to {@link #DEFAULT_CLASS_NAME}. In most cases, there is no need
     * to set this name, because the generated class is loaded into its own {@link java.lang.ClassLoader} where its
     * name cannot collide with classes generated by other evaluators.
     * <p>
     *   One reason to use this function is to have a class name in a non-default package, which can be relevant when
     *   types and members with DEFAULT accessibility are accessed.
     * </p>
     */
    void setClassName(String className);

    /**
     * Sets a particular superclass that the generated class will extend. If <var>optionalExtendedClass</var> is {@code
     * null}, the generated class will extend {@link Object}.
     * <p>
     *   The usual reason to set a base class for an evaluator is that the generated class can directly access the
     *   superclass's (non-private) members.
     * </p>
     */
    void setExtendedClass(@Nullable Class<?> optionalExtendedClass);

    /**
     * @deprecated Use {@link #setExtendedClass(Class)} instead
     */
    @Deprecated void
    setExtendedType(@Nullable Class<?> optionalExtendedClass);

    /**
     * Sets a particular set of interfaces that the generated class will implement.
     */
    void setImplementedInterfaces(Class<?>[] implementedInterfaces);

    /**
     * @deprecated Use {@link #setImplementedInterfaces(Class[])} instead
     */
    @Deprecated void
    setImplementedTypes(Class<?>[] implementedInterfaces);

    /**
     * Returns the loaded {@link Class}.
     * <p>
     *   This method must only be called after exactly one of the {@link #cook(String, java.io.Reader)} methods was
     *   called.
     * </p>
     */
    Class<?> getClazz();

    /**
     * Scans, parses and compiles a class body from the tokens delivered by the the given {@link Reader}, then creates
     * and returns an instance of that class.
     *
     * @param reader Source of class body tokens
     * @return       An object that extends the {@code optionalExtendedType} and implements the given
     *               {@code implementedTypes}
     */
    Object createInstance(Reader reader) throws CompileException, IOException;
}
