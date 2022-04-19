
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

package org.codehaus.commons.compiler;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * An engine that executes a script in JVM bytecode.
 * <p>
 *   The syntax of the script to compile is a sequence of import declarations (not allowed if you compile many scripts
 *   at a time, see below) followed by a sequence of statements, as defined in the <a
 *   href="http://docs.oracle.com/javase/specs/">Java Language Specification, Java SE 7 Edition</a>, sections 7.5 and
 *   14.
 * </p>
 * <p>
 *   An implementation may or may not implement the concept of "local methods", i.e. method declarations being freely
 *   intermixed with statements.
 * </p>
 * <p>
 *   Example:
 * </p>
 * <pre>
 *   import java.text.*;
 *
 *   System.out.println("HELLO");
 *   System.out.println(new DecimalFormat("####,###.##").format(a));
 * </pre>
 * <p>
 *   (Notice that this expression refers to a parameter "a", as explained below.)
 * </p>
 * <p>
 *   The script may complete abnormally, e.g. through a RETURN statement:
 * </p>
 * <pre>
 *   if (a == null) {
 *       System.out.println("Oops!");
 *       return;
 *   }
 * </pre>
 * <p>
 *   Optionally, the script may be declared with a non-void return type. In this case, the last statement of the script
 *   must be a RETURN statement (or a THROW statement), and all RETURN statements in the script must return a value
 *   with the given type.
 * </p>
 * <p>
 *   The script evaluator is implemented by creating and compiling a temporary compilation unit defining one class with
 *   one method the body of which consists of the statements of the script.
 * </p>
 * <p>
 *   To set up a {@link IScriptEvaluator} object, proceed as follows:
 * </p>
 * <ol>
 *   <li>Create an {@link IScriptEvaluator}-implementing class.</li>
 *   <li>
 *     Configure the {@link IScriptEvaluator} by calling any of the following methods:
 *     <ul>
 *        <li>{@link #setReturnType(Class)}
 *        <li>{@link #setParameters(String[], Class[])}
 *        <li>{@link #setThrownExceptions(Class[])}
 *        <li>{@link ISimpleCompiler#setParentClassLoader(ClassLoader)}
 *        <li>{@link IClassBodyEvaluator#setDefaultImports(String[])}
 *     </ul>
 *   </li>
 *   <li>
 *     Call any of the {@link org.codehaus.commons.compiler.Cookable#cook(Reader)} methods to scan, parse, compile and
 *     load the script into the JVM.
 *   </li>
 * </ol>
 * <p>
 *   After the {@link IScriptEvaluator} object is created, the script can be executed as often with different parameter
 *   values (see {@link #evaluate(Object[])}). This execution is very fast, compared to the compilation.
 * </p>
 * <p>
 *   Less common methods exist that allow for the specification of the name of the generated class, the class it
 *   extends, the interfaces it implements, the name of the method that executes the script, the exceptions that this
 *   method (i.e. the script) is allowed to throw, and the {@link ClassLoader} that is used to define the generated
 *   class and to load classes referenced by the script.
 * </p>
 * <p>
 *   If you want to compile many scripts at the same time, you have the option to cook an <em>array</em> of scripts in
 *   one {@link IScriptEvaluator} by using the following methods:
 * </p>
 * <ul>
 *   <li>{@link #setMethodNames(String[])}
 *   <li>{@link #setParameters(String[][], Class[][])}
 *   <li>{@link #setReturnTypes(Class[])}
 *   <li>{@link #setStaticMethod(boolean[])}
 *   <li>{@link #setThrownExceptions(Class[][])}
 *   <li>{@link #cook(Reader)}
 *   <li>{@link #evaluate(int, Object[])}
 * </ul>
 * <p>
 *   Notice that these methods have array parameters in contrast to their one-script brethren.
 * </p>
 */
public
interface IScriptEvaluator extends ICookable, IMultiCookable {

    /**
     * The name of the generated method(s), if no custom method name is configured with {@link
     * #setMethodNames(String[])}.
     * <p>
     *   The {@code '*'} in this string is replaced with the method index, starting at 0.
     * </p>
     */
    String DEFAULT_METHOD_NAME = "eval*";

    /**
     * The return type for any script for which no return type is explicitly configured.
     */
    Class<?> DEFAULT_RETURN_TYPE = void.class;

    /**
     * The "parent class loader" is used to load referenced classes. Useful values are:
     * <table border="1"><tr>
     *   <td>{@code System.getSystemClassLoader()}</td>
     *   <td>The running JVM's class path</td>
     * </tr><tr>
     *   <td>{@code Thread.currentThread().getContextClassLoader()} or {@code null}</td>
     *   <td>The class loader effective for the invoking thread</td>
     * </tr><tr>
     *   <td>{@link ClassLoaders#BOOTCLASSPATH_CLASS_LOADER}</td>
     *   <td>The running JVM's boot class path</td>
     * </tr></table>
     * <p>
     *   The parent class loader defaults to the current thread's context class loader.
     * </p>
     */
    void setParentClassLoader(@Nullable ClassLoader parentClassLoader);

    /**
     * Determines what kind of debugging information is included in the generates classes. The default is typically
     * "{@code -g:none}".
     */
    void setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars);

    /**
     * Installs an {@link ErrorHandler} which is invoked during compilation on each error. (By default, the compilation
     * throws a {@link CompileException} on the first error and terminates.)
     * <p>
     *   If the given {@link ErrorHandler} throws a {@link CompileException}, then the compilation terminates and
     *   the exception is propagated.
     * </p>
     * <p>
     *   If the given {@link ErrorHandler} does not throw a {@link CompileException} but completes normally, then the
     *   compilation may or may not continue, depending on the error. Iff the compilation
     *   completes normally but errors were reported, then it will throw a {@link CompileException} indicating the
     *   number of errors.
     * </p>
     * <p>
     *   In other words: The {@link ErrorHandler} may throw a {@link CompileException} or not, but the compilation will
     *   definitely throw a {@link CompileException} if one or more compile errors have occurred.
     * </p>
     *
     * @param compileErrorHandler {@code null} to restore the default behavior (throwing a {@link CompileException})
     */
    void setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler);

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    void setWarningHandler(@Nullable WarningHandler warningHandler);

    /** @see IClassBodyEvaluator#setClassName(String) */
    void setClassName(String className);

    /** @see IClassBodyEvaluator#setImplementedInterfaces(Class[]) */
    void setImplementedInterfaces(Class<?>[] implementedInterfaces);

    /** @see IClassBodyEvaluator#setExtendedClass(Class) */
    void setExtendedClass(Class<?> extendedClass);

    /**
     * When this {@link IScriptEvaluator} is coooked, then the <var>defaultReturnType</var> applies to all scripts for
     * which no explicit return type was configured.
     * <p>
     *   The initial default return type (if you want, the "default-default" return type) is {@code void.class}.
     * </p>
     *
     * @see #setReturnType(Class)
     * @see #setReturnTypes(Class[])
     */
    void setDefaultReturnType(Class<?> defaultReturnType);

    /**
     * @return The default return type that was previously configured with {@link #setDefaultReturnType(Class)}, or
     *         {@link #DEFAULT_RETURN_TYPE}
     */
    Class<?> getDefaultReturnType();

    /**
     * Defines whether the generated method overrides a methods declared in a supertype.
     */
    void setOverrideMethod(boolean overrideMethod);

    /**
     * Defines whether the generated method should be STATIC or not. Defaults to {@code true}.
     */
    void setStaticMethod(boolean staticMethod);

    /**
     * Defines the return type of the generated method. Value {@code null} means "use the default return type".
     *
     * @see #setDefaultReturnType(Class)
     */
    void setReturnType(Class<?> returnType);

    /**
     * Defines the name of the generated method. {@code null} means use a reasonable {@value #DEFAULT_METHOD_NAME}.
     */
    void setMethodName(@Nullable String methodName);

    /**
     * Defines the names and types of the parameters of the generated method.
     * <p>
     *   <var>names</var>{@code .length} and <var>types</var>{@code .length} must be equal. This invariant may be
     *   checked immediately, or later when the script is cooked.
     * </p>
     * <p>
     *   The parameters can be of primitive type, e.g. {@code double.class}.
     * </p>
     * <p>
     *   The default is to have zero parameters.
     * </p>
     */
    void setParameters(String[] names, Class<?>[] types);

    /**
     * Defines the exceptions that the generated method may throw.
     */
    void setThrownExceptions(Class<?>[] thrownExceptions);

    /**
     * Calls the script with concrete parameter values.
     * <p>
     *   Each argument must have the same type as specified through the {@code parameterTypes} parameter of {@link
     *   #setParameters(String[], Class[])}.
     * </p>
     * <p>
     *   Arguments of primitive type must passed with their wrapper class objects.
     * </p>
     * <p>
     *   The object returned has the class as specified through {@link #setReturnType(Class)}.
     * </p>
     * <p>
     *   This method is thread-safe.
     * </p>
     *
     * @param arguments              The actual parameter values
     * @throws IllegalStateException This IScriptEvaluator is not yet cooked
     */
    @Nullable Object evaluate(@Nullable Object[] arguments) throws InvocationTargetException;

    /**
     * @return The generated and loaded {@link java.lang.reflect.Method}
     * @throws IllegalStateException This IScriptEvaluator is not yet cooked
     */
    Method getMethod();

    /**
     * Same as {@link #setOverrideMethod(boolean)}, but for multiple scripts.
     */
    void setOverrideMethod(boolean[] overrideMethod);

    /**
     * Same as {@link #setStaticMethod(boolean)}, but for multiple scripts.
     */
    void setStaticMethod(boolean[] staticMethod);

    /**
     * Configures the return types of the generated methods. If an element of the array is {@code null}, then use
     * the "default return type" for that script.
     *
     * @param returnTypes                The methods' return types; {@code null} elements mean "use the default return
     *                                   type"
     * @see #setDefaultReturnType(Class)
     * @see #getDefaultReturnType()
     */
    void setReturnTypes(Class<?>[] returnTypes);

    /**
     * Same as {@link #setMethodName(String)}, but for multiple scripts.
     * <p>
     *   Define the names of the generated methods. By default the methods have distinct and implementation-specific
     *   names.
     * </p>
     * <p>
     *   If two scripts have the same name, then they must have different parameter types (see {@link
     *   #setParameters(String[][], Class[][])}).
     * </p>
     */
    void setMethodNames(String[] methodNames);

    /**
     * Same as {@link #setParameters(String[], Class[])}, but for multiple scripts.
     */
    void setParameters(String[][] names, Class<?>[][] types);

    /**
     * Same as {@link #setThrownExceptions(Class[])}, but for multiple scripts.
     */
    void setThrownExceptions(Class<?>[][] thrownExceptions);

    /**
     * Same as {@link #cook(Reader)}, but for multiple scripts.
     */
    @Override void
    cook(Reader... readers) throws CompileException, IOException;

    /**
     * Same as {@link #cook(String, Reader)}, but cooks a <em>set</em> of scripts into one class. Notice that
     * if <em>any</em> of the scripts causes trouble, the entire compilation will fail. If you
     * need to report <em>which</em> of the scripts causes the exception, you may want to use the
     * {@code fileNames} parameter to distinguish between the individual token sources.
     * <p>
     *   Iff the number of scanners is one, then that single script may contain leading IMPORT directives.
     * </p>s
     *
     * @throws IllegalStateException if any of the preceding {@code set...()} had an array
     *                               size different from that of {@code scanners}
     */
    @Override void
    cook(String[] fileNames, Reader[] readers) throws CompileException, IOException;

    /**
     * Same as {@link #cook(String)}, but for multiple scripts.
     */
    @Override void
    cook(String[] strings) throws CompileException;

    /**
     * Same as {@link #cook(String, String)}, but for multiple scripts.
     */
    @Override void
    cook(String[] fileNames, String[] strings) throws CompileException;

    /**
     * Same as {@link #evaluate(Object[])}, but for multiple scripts.
     */
    @Nullable Object evaluate(int idx, @Nullable Object[] arguments) throws InvocationTargetException;

    /**
     * Same as {@link #getMethod()}, but for multiple scripts.
     */
    Method getMethod(int idx);

    /**
     * @param script Contains the sequence of script tokens
     * @see          #createFastEvaluator(Reader, Class, String[])
     */
    <T> T
    createFastEvaluator(
        String   script,
        Class<T> interfaceToImplement,
        String[] parameterNames
    ) throws CompileException;

    /**
     * If the parameter and return types of the expression are known at compile time, then a "fast" script evaluator
     * can be instantiated through this method.
     * <p>
     *   Script evaluation is faster than through {@link #evaluate(Object[])}, because it is not done through
     *   reflection but through direct method invocation.
     * </p>
     * <p>
     *   Example:
     * </p>
     * <pre>
     * public interface Foo {
     *     int bar(int a, int b);
     * }
     * ...
     * IScriptEvaluator se = {@link CompilerFactoryFactory}.{@link
     * CompilerFactoryFactory#getDefaultCompilerFactory getDefaultCompilerFactory}().{@link
     * ICompilerFactory#newScriptEvaluator newScriptEvaluator}();
     *
     * // Optionally configure the SE her:
     * se.{@link #setClassName(String) setClassName}("Bar");
     * se.{@link #setDefaultImports(String[]) setDefaultImports}(new String[] { "java.util.*" });
     * se.{@link #setExtendedClass(Class) setExtendedClass}(SomeOtherClass.class);
     * se.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(someClassLoader);
     *
     * Foo f = (Foo) se.{@link #createFastEvaluator(String, Class, String[]) createFastScriptEvaluator}(
     *     "return a - b;",
     *     Foo.class,
     *     new String[] { "a", "b" }
     * );
     * System.out.println("1 - 2 = " + f.bar(1, 2));
     * </pre>
     * <p>
     *   All other configuration (implemented type, static method, return type, method name, parameter names and types,
     *   thrown exceptions) are predetermined by the {@code interfaceToImplement}.
     * </p>
     * <p>
     *   Notice: The {@code interfaceToImplement} must either be declared {@code public}, or with package scope in the
     *   same package as the generated class (see {@link #setClassName(String)}).
     * </p>s
     *
     * @param reader               Produces the stream of script tokens
     * @param interfaceToImplement Must declare exactly one method
     * @param parameterNames       The names of the parameters of that method
     * @return                     An object that implements the given interface
     */
    <T> T
    createFastEvaluator(
        Reader   reader,
        Class<T> interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, IOException;

    /** @see IClassBodyEvaluator#setDefaultImports(String...) */
    void setDefaultImports(String... defaultImports);

    /** @see IClassBodyEvaluator#getDefaultImports() */
    String[] getDefaultImports();

    /** @see IClassBodyEvaluator#getClazz() */
    Class<?> getClazz();

    /**
     * @return                       The generated and loaded methods that implement the cooked scripts
     * @throws IllegalStateException This {@link IScriptEvaluator} is not yet cooked
     */
    Method[] getResult();
}
