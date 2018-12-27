
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

package org.codehaus.janino;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.Modifiers;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Parser.ClassDeclarationContext;
import org.codehaus.janino.util.AbstractTraverser;

/**
 * An implementation of {@link IScriptEvaluator} that utilizes the JANINO Java compiler.
 * <p>
 *   This implementation implements the concept of "Local methods", i.e. statements may be freely intermixed with
 *   method declarations. These methods are typically called by the "main code" of the script evaluator. One limitation
 *   exists: When cooking <em>multiple</em> scripts in one {@link ScriptEvaluator}, then local method signatures
 *   (names and parameter types) must not collide between scripts.
 * </p>
 * <p>
 *   A plethora of "convenience constructors" exist that execute the setup steps instantly. Their use is discouraged,
 *   in favor of using the default constructor, plus calling a number of setters, and then one of the {@code cook()}
 *   methods.
 * </p>
 */
public
class ScriptEvaluator extends ClassBodyEvaluator implements IScriptEvaluator {

    /**
     * The name of the generated method(s), if no custom method name is configured with {@link
     * #setMethodNames(String[])}.
     * <p>
     *   The {@code '*'} in this string is replaced with the method index, starting at 0.
     * </p>
     */
    public static final String DEFAULT_METHOD_NAME = "eval*";

    /**
     * Represents one script that this {@link ScriptEvaluator} declares. Typically there exactly <em>one</em> such
     * script, but there can be two or more - see {@link ScriptEvaluator#ScriptEvaluator()}.
     */
    class Script {

        /**
         * Whether the generated method overrides a method declared by a supertype; defaults to {@code false}.
         */
        protected boolean overrideMethod;

        /**
         * Whether the method is generated {@code static}; defaults to {@code true}.
         */
        protected boolean staticMethod = true;

        /**
         * The generated method's return type. Defaults to {@link ScriptEvaluator#getDefaultReturnType()}.
         */
        protected Class<?> returnType = ScriptEvaluator.this.getDefaultReturnType();

        /**
         * The name of the generated method.
         */
        private String methodName;

        private String[] parameterNames = new String[0];

        private Class<?>[] parameterTypes = new Class<?>[0];

        private Class<?>[] thrownExceptions = new Class<?>[0];

        @Nullable private Method result; // null=uncooked

        Script(String methodName) { this.methodName = methodName; }

        /**
         * @return                       The generated method
         * @throws IllegalStateException The {@link ScriptEvaluator} has not yet be cooked
         */
        public Method
        getResult() {
            if (this.result != null) return this.result;
            throw new IllegalStateException("Script is not yet cooked");
        }
    }

    /**
     * The scripts to compile. Is initialized on the first call to {@link
     * #setStaticMethod(boolean[])} or one of its friends.
     */
    @Nullable private Script[] scripts;

    /**
     * @throws IllegalArgumentException <var>count</var> is different from previous invocations of
     *                                  this method
     */
    public void
    setScriptCount(int count) {

        Script[] ss = this.scripts;

        if (ss == null) {
            this.scripts = (ss = new Script[count]);
            for (int i = 0; i < count; i++) {
                ss[i] = new Script(ScriptEvaluator.DEFAULT_METHOD_NAME.replace("*", Integer.toString(i)));
            }
        } else {
            if (count != ss.length) {
                throw new IllegalArgumentException(
                    "Inconsistent script count; previously " + ss.length + ", now " + count
                );
            }
        }
    }

    private Script
    getScript(int index) {
        if (this.scripts != null) return this.scripts[index];
        throw new IllegalStateException("\"getScript()\" invoked befor \"setScriptCount()\"");
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.cook(script);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see Cookable#cook(String)
     */
    public
    ScriptEvaluator(String script) throws CompileException {
        this.cook(script);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.cook(script);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see Cookable#cook(String)
     */
    public
    ScriptEvaluator(String script, Class<?> returnType) throws CompileException {
        this.setReturnType(returnType);
        this.cook(script);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.cook(script);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see Cookable#cook(String)
     */
    public
    ScriptEvaluator(String script, Class<?> returnType, String[] parameterNames, Class<?>[] parameterTypes)
    throws CompileException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.cook(script);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.cook(script);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see Cookable#cook(String)
     */
    public
    ScriptEvaluator(
        String     script,
        Class<?>   returnType,
        String[]   parameterNames,
        Class<?>[] parameterTypes,
        Class<?>[] thrownExceptions
    ) throws CompileException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.cook(script);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(optionalParentClassLoader);
     *     se.cook(optionalFileName, is);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, InputStream)
     */
    public
    ScriptEvaluator(
        @Nullable String      optionalFileName,
        InputStream           is,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(optionalFileName, is);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(optionalParentClassLoader);
     *     se.cook(reader);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, Reader)
     */
    public
    ScriptEvaluator(
        @Nullable String      optionalFileName,
        Reader                reader,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(optionalFileName, reader);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(optionalParentClassLoader);
     *     se.cook(scanner);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ScriptEvaluator(
        Scanner               scanner,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setExtendedType(optionalExtendedType);
     *     se.setImplementedTypes(implementedTypes);
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(optionalParentClassLoader);
     *     se.cook(scanner);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see ClassBodyEvaluator#setExtendedClass(Class)
     * @see ClassBodyEvaluator#setImplementedInterfaces(Class[])
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ScriptEvaluator(
        Scanner               scanner,
        @Nullable Class<?>    optionalExtendedType,
        Class<?>[]            implementedTypes,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setClassName(className);
     *     se.setExtendedType(optionalExtendedType);
     *     se.setImplementedTypes(implementedTypes);
     *     se.setStaticMethod(staticMethod);
     *     se.setReturnType(returnType);
     *     se.setMethodName(methodName);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(optionalParentClassLoader);
     *     se.cook(scanner);
     * </pre>
     *
     * @see #ScriptEvaluator()
     * @see ClassBodyEvaluator#setClassName(String)
     * @see ClassBodyEvaluator#setExtendedClass(Class)
     * @see ClassBodyEvaluator#setImplementedInterfaces(Class[])
     * @see #setStaticMethod(boolean)
     * @see #setReturnType(Class)
     * @see #setMethodName(String)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ScriptEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    optionalExtendedType,
        Class<?>[]            implementedTypes,
        boolean               staticMethod,
        Class<?>              returnType,
        String                methodName,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setClassName(className);
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setStaticMethod(staticMethod);
        this.setReturnType(returnType);
        this.setMethodName(methodName);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Constructs a script evaluator with all the default settings (return type {@code void}
     */
    public ScriptEvaluator() {}

    /**
     * Constructs a script evaluator with the given number of scripts.
     * <p>
     *   The argument of all following invocations of
     *   {@link #setMethodNames(String[])},
     *   {@link #setOverrideMethod(boolean[])},
     *   {@link #setParameters(String[][], Class[][])},
     *   {@link #setReturnTypes(Class[])},
     *   {@link #setStaticMethod(boolean[])},
     *   {@link #setThrownExceptions(Class[][])},
     *   {@link #cook(org.codehaus.janino.util.ClassFile[])},
     *   {@link #cook(Parser[])},
     *   {@link #cook(Reader[])},
     *   {@link #cook(Scanner[])},
     *   {@link #cook(String[])},
     *   {@link #cook(String[], Reader[])} and
     *   {@link #cook(String[], String[])}
     *   must be arrays with exactly that length.
     * </p>
     * <p>
     *   If a different constructor is used, then the first invocation of one of the above method implicitly sets the
     *   script count.
     * </p>
     */
    public ScriptEvaluator(int count) { this.setScriptCount(count); }

    // ================= SINGLE SCRIPT CONFIGURATION SETTERS =================

    @Override public void
    setOverrideMethod(boolean overrideMethod) { this.setOverrideMethod(new boolean[] { overrideMethod }); }

    @Override public void
    setStaticMethod(boolean staticMethod) { this.setStaticMethod(new boolean[] { staticMethod }); }

    /**
     * Defines the return types of the generated methods.
     *
     * @param returnType The method's return type; {@code null} means the "default return type", which is the type
     *                   returned by {@link #getDefaultReturnType()} ({@code void.class} for {@link ScriptEvaluator}
     *                   and {@code Object.class} for {@link ExpressionEvaluator})
     * @see              ScriptEvaluator#getDefaultReturnType()
     * @see              ExpressionEvaluator#getDefaultReturnType()
     */
    @Override public void
    setReturnType(Class<?> returnType) { this.setReturnTypes(new Class[] { returnType }); }

    @Override public void
    setMethodName(String methodName) { this.setMethodNames(new String[] { methodName }); }

    @Override public void
    setParameters(String[] parameterNames, Class<?>[] parameterTypes) {
        this.setParameters(new String[][] { parameterNames }, new Class[][] { parameterTypes });
    }

    @Override public void
    setThrownExceptions(Class<?>[] thrownExceptions) {
        this.setThrownExceptions(new Class<?>[][] { thrownExceptions });
    }

    // ================= MULTIPLE SCRIPT CONFIGURATION SETTERS =================

    @Override public void
    setOverrideMethod(boolean[] overrideMethod) {
        this.setScriptCount(overrideMethod.length);
        for (int i = 0; i < overrideMethod.length; i++) this.getScript(i).overrideMethod = overrideMethod[i];
    }

    @Override public void
    setStaticMethod(boolean[] staticMethod) {
        this.setScriptCount(staticMethod.length);
        for (int i = 0; i < staticMethod.length; i++) this.getScript(i).staticMethod = staticMethod[i];
    }

    /**
     * Defines the return types of the generated methods.
     *
     * @param returnTypes The methods' return types; {@code null} elements mean the "default return type", which is the
     *                    type returned by {@link #getDefaultReturnType()} ({@code void.class} for {@link
     *                    ScriptEvaluator} and {@code Object.class} for {@link ExpressionEvaluator})
     * @see               ScriptEvaluator#getDefaultReturnType()
     * @see               ExpressionEvaluator#getDefaultReturnType()
     */
    @Override public void
    setReturnTypes(Class<?>[] returnTypes) {
        this.setScriptCount(returnTypes.length);
        for (int i = 0; i < returnTypes.length; i++) {
            this.getScript(i).returnType = (Class<?>) ScriptEvaluator.or(
                returnTypes[i],
                ScriptEvaluator.this.getDefaultReturnType()
            );
        }
    }

    private static <T> T
    or(@Nullable T lhs, T rhs) { return lhs != null ? lhs : rhs; }

    @Override public void
    setMethodNames(String[] methodNames) {
        this.setScriptCount(methodNames.length);
        for (int i = 0; i < methodNames.length; i++) this.getScript(i).methodName = methodNames[i];
    }

    @Override public void
    setParameters(String[][] parameterNames, Class<?>[][] parameterTypes) {

        this.setScriptCount(parameterNames.length);
        for (int i = 0; i < parameterNames.length; i++) {
            this.getScript(i).parameterNames = (String[]) parameterNames[i].clone();
        }

        this.setScriptCount(parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            this.getScript(i).parameterTypes = (Class[]) parameterTypes[i].clone();
        }
    }

    @Override public void
    setThrownExceptions(Class<?>[][] thrownExceptions) {
        this.setScriptCount(thrownExceptions.length);
        for (int i = 0; i < thrownExceptions.length; i++) this.getScript(i).thrownExceptions = thrownExceptions[i];
    }

    // ---------------------------------------------------------------

    @Override public final void
    cook(String[] strings) throws CompileException { this.cook(null, strings); }

    @Override public final void
    cook(@Nullable String[] optionalFileNames, String[] strings) throws CompileException {
        Reader[] readers = new Reader[strings.length];
        for (int i = 0; i < strings.length; ++i) readers[i] = new StringReader(strings[i]);
        try {
            this.cook(optionalFileNames, readers);
        } catch (IOException ex) {
            throw new InternalCompilerException("SNO: IOException despite StringReader", ex);
        }
    }

    @Override public final void
    cook(Reader[] readers) throws CompileException, IOException {
        this.cook(null, readers);
    }

    /**
     * On a 2 GHz Intel Pentium Core Duo under Windows XP with an IBM 1.4.2 JDK, compiling 10000 expressions "a + b"
     * (integer) takes about 4 seconds and 56 MB of main memory. The generated class file is 639203 bytes large.
     * <p>
     *   The number and the complexity of the scripts is restricted by the <a
     *   href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88659">Limitations of the Java
     *   Virtual Machine</a>, where the most limiting factor is the 64K entries limit of the constant pool. Since every
     *   method with a distinct name requires one entry there, you can define at best 32K (very simple) scripts.
     * </p>
     */
    @Override public final void
    cook(@Nullable String[] optionalFileNames, Reader[] readers) throws CompileException, IOException {

        if (optionalFileNames != null) this.setScriptCount(optionalFileNames.length);
        this.setScriptCount(readers.length);

        Scanner[] scanners = new Scanner[readers.length];
        for (int i = 0; i < readers.length; ++i) {
            scanners[i] = new Scanner(optionalFileNames == null ? null : optionalFileNames[i], readers[i]);
        }
        this.cook(scanners);
    }

    @Override public final void
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

        this.setScriptCount(scanners.length);

        Parser[] parsers = new Parser[scanners.length];
        for (int i = 0; i < scanners.length; ++i) parsers[i] = new Parser(scanners[i]);

        this.cook(parsers);
    }

    /**
     * @see #cook(Scanner[])
     */
    public final void
    cook(Parser[] parsers) throws CompileException, IOException {

        this.setScriptCount(parsers.length);

        // Create compilation unit.
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(parsers.length == 1 ? parsers[0] : null);

        // Create class declaration.
        final Java.AbstractClassDeclaration
        cd = this.addPackageMemberClassDeclaration(parsers[0].location(), compilationUnit);

        // Create methods with one block each.
        for (int i = 0; i < parsers.length; ++i) {

            Script es     = this.getScript(i);
            Parser parser = parsers[i];

            // Create the statements of the method.
            List<Java.BlockStatement>   statements   = new ArrayList<Java.BlockStatement>();
            List<Java.MethodDeclarator> localMethods = new ArrayList<Java.MethodDeclarator>();
            this.makeStatements(i, parser, statements, localMethods);

            // Create the method that holds the statements.
            Location loc = parser.location();
            cd.addDeclaredMethod(this.makeMethodDeclaration(
                loc,                 // location
                (                    // annotations
                    es.overrideMethod
                    ? new Java.Annotation[] { new Java.MarkerAnnotation(this.classToType(loc, Override.class)) }
                    : new Java.Annotation[0]
                ),
                es.staticMethod,     // staticMethod
                es.returnType,       // returnType
                es.methodName,       // methodName
                es.parameterTypes,   // parameterTypes
                es.parameterNames,   // parameterNames
                es.thrownExceptions, // thrownExceptions
                statements           // statements
            ));

            // Also add the "local methods" that a script my declare.
            for (MethodDeclarator method : localMethods) {
                cd.addDeclaredMethod(method);
            }
        }

        this.cook2(compilationUnit);
    }

    /**
     * Compiles the given <var>compilationUnit</var>, defines it into a {@link ClassLoader}, loads the generated class,
     * gets the script methods from that class, and makes them available through {@link #getMethod(int)}.
     */
    protected void
    cook2(CompilationUnit compilationUnit) throws CompileException {

        // Compile and load the compilation unit.
        final Class<?> c = this.compileToClass(compilationUnit);

        // Find the script methods by name and parameter types.
        assert this.scripts != null;
        int count = this.scripts.length;

        // Clear the generated methods.
        for (int i = 0; i < count; ++i) this.getScript(i).result = null;

        // "Class.getDeclaredMethod(name, parameterTypes)" is slow when the class declares MANY methods (say, in
        // the thousands). So let's use "Class.getDeclaredMethods()" instead.

        // Create a (temporary) mapping of method key to method index.
        Map<Object /*methodKey*/, Integer /*methodIndex*/> dms = new HashMap<Object, Integer>(2 * count);
        for (int i = 0; i < count; ++i) {
            Script  es   = this.getScript(i);
            Integer prev = (Integer) dms.put(ScriptEvaluator.methodKey(es.methodName, es.parameterTypes), i);
            assert prev == null;
        }

        // Now invoke "Class.getDeclaredMethods()" and filter "our" methods from the result.
        for (Method m : c.getDeclaredMethods()) {

            Integer idx = (Integer) dms.get(ScriptEvaluator.methodKey(m.getName(), m.getParameterTypes()));
            if (idx == null) continue;

            Script es = this.getScript(idx);
            assert es.result == null;
            es.result = m;
        }

        // Verify that the class declared "all our" methods.
        for (int i = 0; i < count; ++i) {
            Script es = this.getScript(i);
            if (es.result == null) {
                throw new InternalCompilerException(
                    "SNO: Generated class does not declare method \""
                    + es.methodName
                    + "\" (index "
                    + i
                    + ")"
                );
            }
        }
    }

    private static Object
    methodKey(String methodName, Class<?>[] parameterTypes) {
        return Arrays.asList(ScriptEvaluator.cat(methodName, parameterTypes, Object.class));
    }

    /**
     * @return A copy of the <var>followingElements</var>, prepended with the <var>firstElement</var>
     */
    private static <T> T[]
    cat(T firstElement, T[] followingElements, Class<T> componentType) {

        @SuppressWarnings("unchecked") T[]
        result = (T[]) Array.newInstance(componentType, 1 + followingElements.length);

        result[0] = firstElement;
        System.arraycopy(followingElements, 0, result, 1, followingElements.length);

        return result;
    }

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

    @Override public Method
    getMethod() { return this.getMethod(0); }

    @Override public Method
    getMethod(int idx) { return this.getScript(idx).getResult(); }

    /**
     * @return {@code void.class}
     * @see    #setReturnTypes(Class[])
     */
    protected Class<?>
    getDefaultReturnType() { return void.class; }

    /**
     * @return The return type of the indexed script.
     */
    protected final Class<?>
    getReturnType(int index) { return this.getScript(index).returnType; }

    /**
     * Parses statements from the <var>parser</var> until end-of-input.
     *
     * @param resultStatements Is filled with the generated statements
     * @param resultMethods    Is filled with any local methods that the script declares
     */
    protected void
    makeStatements(
        int                    idx,
        Parser                 parser,
        List<BlockStatement>   resultStatements,
        List<MethodDeclarator> resultMethods
    ) throws CompileException, IOException {

        while (!parser.peek(TokenType.END_OF_INPUT)) {
            ScriptEvaluator.parseScriptStatement(parser, resultStatements, resultMethods);
        }
    }

    /**
     * <pre>
     *   ScriptStatement :=
     *     Statement                                               (1)
     *     | 'class' ...                                           (2)
     *     | [ Modifiers ] 'void' Identifier MethodDeclarationRest (3a)
     *     | Modifiers Type Identifier MethodDeclarationRest ';'   (3b)
     *     | Expression Identifier MethodDeclarationRest           (3c) (5)
     *     | Modifiers Type VariableDeclarators ';'                (4a)
     *     | Expression VariableDeclarators ';'                    (4b) (5)
     *     | Expression ';'
     * </pre>
     * <p> (1): Includes the "labeled statement".</p>
     * <p> (2): Local class declaration.</p>
     * <p> (3a), (3b), (3c): Local method declaration statement.</p>
     * <p> (4) Local variable declaration statement.</p>
     * <p> (5) "Expression" must pose a type.</p>
     *
     * @param localMethods Filled with the methods that the script declares
     */
    private static void
    parseScriptStatement(
        Parser                      parser,
        List<Java.BlockStatement>   mainStatements,
        List<Java.MethodDeclarator> localMethods
    ) throws CompileException, IOException {

        // Statement?
        if (
            (parser.peek(TokenType.IDENTIFIER) && parser.peekNextButOne(":"))
            || parser.peek(
                "if", "for", "while", "do", "try", "switch", "synchronized", // SUPPRESS CHECKSTYLE Wrap|LineLength:2
                "return", "throw", "break", "continue", "assert",
                "{", ";"
            ) != -1
        ) {
            mainStatements.add(parser.parseStatement());
            return;
        }

        // Local class declaration?
        if (parser.peekRead("class")) {

            final LocalClassDeclaration lcd = (LocalClassDeclaration) parser.parseClassDeclarationRest(
                null,                         // optionalDocComment
                new Modifiers(),              // modifiers
                ClassDeclarationContext.BLOCK // context
            );
            mainStatements.add(new LocalClassDeclarationStatement(lcd));
            return;
        }

        Modifiers modifiers = parser.parseModifiers();

        // "void" method declaration (without type parameters).
        if (parser.peekRead("void")) {
            String name = parser.read(TokenType.IDENTIFIER);
            localMethods.add(parser.parseMethodDeclarationRest(
                null,                                                 // optionalDocComment
                modifiers,                                            // modifiers
                null,                                                 // optionalTypeParameters
                new PrimitiveType(parser.location(), Primitive.VOID), // type
                name,                                                 // name
                false                                                 // allowDefaultClause
            ));
            return;
        }

        if (!modifiers.isBlank()) {

            Type methodOrVariableType = parser.parseType();

            // Modifiers Type Identifier MethodDeclarationRest ';'
            if (parser.peek(TokenType.IDENTIFIER) && parser.peekNextButOne("(")) {
                localMethods.add(parser.parseMethodDeclarationRest(
                    null,                              // optionalDocComment
                    modifiers,                         // modifiers
                    null,                              // optionalTypeParameters
                    methodOrVariableType,              // type
                    parser.read(TokenType.IDENTIFIER), // name
                    false                              // allowDefaultClause
                ));
                return;
            }

            // Modifiers Type VariableDeclarators ';'
            mainStatements.add(new LocalVariableDeclarationStatement(
                parser.location(),                // location
                modifiers,                        // modifiers
                methodOrVariableType,             // type
                parser.parseVariableDeclarators() // variableDeclarators
            ));
            parser.read(";");
            return;
        }

        // It's either a non-final local variable declaration or an expression statement, or a non-void method
        // declaration. We can only tell after parsing an expression.

        Atom a = parser.parseExpression();

        // Expression ';'
        if (parser.peekRead(";")) {
            mainStatements.add(new ExpressionStatement(a.toRvalueOrCompileException()));
            return;
        }

        Type methodOrVariableType = a.toTypeOrCompileException();

        // [ Modifiers ] Expression identifier MethodDeclarationRest
        if (parser.peek(TokenType.IDENTIFIER) && parser.peekNextButOne("(")) {
            localMethods.add(parser.parseMethodDeclarationRest(
                null,                              // optionalDocComment
                modifiers,                         // modifiers
                null,                              // optionalTypeParameters
                methodOrVariableType,              // type
                parser.read(TokenType.IDENTIFIER), // name
                false                              // allowDefaultClause
            ));
            return;
        }

        // [ Modifiers ] Expression VariableDeclarators ';'
        mainStatements.add(new LocalVariableDeclarationStatement(
            a.getLocation(),                  // location
            modifiers,                        // modifiers
            methodOrVariableType,             // type
            parser.parseVariableDeclarators() // variableDeclarators
        ));
        parser.read(";");
    }

    private Java.MethodDeclarator
    makeMethodDeclaration(
        Location                  location,
        Java.Annotation[]         annotations,
        boolean                   staticMethod,
        Class<?>                  returnType,
        String                    methodName,
        Class<?>[]                parameterTypes,
        String[]                  parameterNames,
        Class<?>[]                thrownExceptions,
        List<Java.BlockStatement> statements
    ) {
        if (parameterNames.length != parameterTypes.length) {
            throw new InternalCompilerException(
                "Lengths of \"parameterNames\" ("
                + parameterNames.length
                + ") and \"parameterTypes\" ("
                + parameterTypes.length
                + ") do not match"
            );
        }

        Java.FunctionDeclarator.FormalParameters fps = new Java.FunctionDeclarator.FormalParameters(
            location,
            new Java.FunctionDeclarator.FormalParameter[parameterNames.length],
            false
        );

        for (int i = 0; i < fps.parameters.length; ++i) {
            fps.parameters[i] = new Java.FunctionDeclarator.FormalParameter(
                location,                                      // location
                true,                                          // finaL
                this.classToType(location, parameterTypes[i]), // type
                parameterNames[i]                              // name
            );
        }

        return new Java.MethodDeclarator(
            location,                                        // location
            null,                                            // optionalDocComment
            new Java.Modifiers(                              // modifiers
                staticMethod ? (short) (Mod.PUBLIC | Mod.STATIC) : Mod.PUBLIC,
                annotations,
                false // isDefault
            ),
            null,                                            // optionalTypeParameters
            this.classToType(location, returnType),          // type
            methodName,                                      // name
            fps,                                             // formalParameters
            this.classesToTypes(location, thrownExceptions), // thrownExceptions
            null,                                            // defaultValue
            statements                                       // optionalStatements
        );
    }

    /**
     * @deprecated Use {@link #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[],
     *             ClassLoader)} instead
     */
    @Deprecated public static Object
    createFastScriptEvaluator(String script, Class<?> interfaceToImplement, String[] parameterNames)
    throws CompileException {
        ScriptEvaluator se = new ScriptEvaluator();
        return se.createFastEvaluator(script, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated Use {@link #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[],
     *             ClassLoader)} instead
     */
    @Deprecated public static Object
    createFastScriptEvaluator(
        Scanner               scanner,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setParentClassLoader(optionalParentClassLoader);
        return se.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated Use {@link #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[],
     *             ClassLoader)} instead
     */
    @Deprecated public static Object
    createFastScriptEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    optionalExtendedType,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setClassName(className);
        se.setExtendedClass(optionalExtendedType);
        se.setParentClassLoader(optionalParentClassLoader);
        return se.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * <pre>
     *     {@link ScriptEvaluator} se = new {@link ScriptEvaluator#ScriptEvaluator() ScriptEvaluator}();
     *     se.{@link #setDefaultImports(String[]) setDefaultImports}.(optionalDefaultImports);
     *     se.{@link #setClassName(String) setClassName}.(className);
     *     se.{@link #setExtendedClass(Class) setExtendedClass}.(optionalExtendedClass);
     *     se.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(optionalParentClassLoader);
     *     return se.{@link #createFastEvaluator(Scanner, Class, String[]) createFastEvaluator}(scanner,
     *     interfaceToImplement, parameterNames);
     * </pre>
     *
     * @deprecated Use {@link #createFastEvaluator(Scanner,Class,String[])} instead:
     */
    @Deprecated public static Object
    createFastScriptEvaluator(
        Scanner               scanner,
        @Nullable String[]    optionalDefaultImports,
        String                className,
        @Nullable Class<?>    optionalExtendedClass,
        Class<?>              interfaceToImplement,
        String[]              parameterNames,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setDefaultImports(optionalDefaultImports);
        se.setClassName(className);
        se.setExtendedClass(optionalExtendedClass);
        se.setParentClassLoader(optionalParentClassLoader);
        return se.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * Don't use.
     */
    @Override public final Object
    createInstance(Reader reader) {
        throw new UnsupportedOperationException("createInstance");
    }

    @Override public <T> Object
    createFastEvaluator(Reader reader, Class<T> interfaceToImplement, String[] parameterNames)
    throws CompileException, IOException {
        return this.createFastEvaluator(new Scanner(null, reader), interfaceToImplement, parameterNames);
    }

    @Override public <T> Object
    createFastEvaluator(String script, Class<T> interfaceToImplement, String[] parameterNames) throws CompileException {
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
    public Object
    createFastEvaluator(Scanner scanner, Class<?> interfaceToImplement, String[] parameterNames)
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
            this.setReturnType(methodToImplement.getReturnType());
        }
        this.setMethodName(methodToImplement.getName());
        this.setParameters(parameterNames, methodToImplement.getParameterTypes());
        this.setThrownExceptions(methodToImplement.getExceptionTypes());
        this.cook(scanner);
        Class<?> c = this.getMethod().getDeclaringClass();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new InternalCompilerException(e.toString(), e);
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new InternalCompilerException(e.toString(), e);
        }
    }

    /**
     * Guesses the names of the parameters used in the given expression. The strategy is to look at all "ambiguous
     * names" in the expression (e.g. in "a.b.c.d()", the ambiguous name is "a.b.c"), and then at the components of the
     * ambiguous name.
     * <ul>
     *   <li>
     *     If any component starts with an upper-case letter, then ambiguous name is assumed to be a type name.
     *   </li>
     *   <li>
     *     Otherwise, if the first component of the ambiguous name matches the name of a previously defined local
     *     variable, then the first component of the ambiguous name is assumed to be a local variable name. (Notice
     *     that this strategy does not consider that the scope of a local variable declaration may end before the end
     *     of the script.)
     *   </li>
     *   <li>
     *     Otherwise, the first component of the ambiguous name is assumed to be a parameter name.
     *   </li>
     * </ul>
     *
     * @see Scanner#Scanner(String, Reader)
     */
    public static String[]
    guessParameterNames(Scanner scanner) throws CompileException, IOException {
        Parser parser = new Parser(scanner);

        // Eat optional leading import declarations.
        while (parser.peek("import")) parser.parseImportDeclaration();

        // Parse the script statements into a block.
        Java.Block block = new Java.Block(scanner.location());
        while (!parser.peek(TokenType.END_OF_INPUT)) block.addStatement(parser.parseBlockStatement());

        // Traverse the block for ambiguous names and guess which of them are parameter names.
        final Set<String> localVariableNames = new HashSet<String>();
        final Set<String> parameterNames     = new HashSet<String>();
        new AbstractTraverser<RuntimeException>() {

            @Override public void
            traverseLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
                for (VariableDeclarator vd : lvds.variableDeclarators) localVariableNames.add(vd.name);
                super.traverseLocalVariableDeclarationStatement(lvds);
            }

            @Override public void
            traverseAmbiguousName(Java.AmbiguousName an) {

                // If any of the components starts with an upper-case letter, then the ambiguous
                // name is most probably a type name, e.g. "System.out" or "java.lang.System.out".
                for (int i = 0; i < an.identifiers.length; ++i) {
                    if (Character.isUpperCase(an.identifiers[i].charAt(0))) return;
                }

                // Is it a local variable's name?
                if (localVariableNames.contains(an.identifiers[0])) return;

                // It's most probably a parameter name (although it could be a field name as well).
                parameterNames.add(an.identifiers[0]);
            }
        }.visitBlockStatement(block);

        return (String[]) parameterNames.toArray(new String[parameterNames.size()]);
    }
}
