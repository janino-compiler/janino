
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
     *   The {@code "*"} in this string is replaced with the method index, starting at 0.
     * </p>
     */
    public static final String DEFAULT_METHOD_NAME = "eval*";

    /**
     * Whether methods override a method declared by a supertype; {@code null} means "none".
     */
    @Nullable protected boolean[] optionalOverrideMethod;

    /**
     * Whether methods are static; {@code null} means "all".
     */
    @Nullable protected boolean[] optionalStaticMethod;

    /**
     * The methods' return types; {@code null} means "none".
     */
    @Nullable protected Class<?>[] optionalReturnTypes;

    @Nullable private String[]     optionalMethodNames;
    @Nullable private String[][]   optionalParameterNames;
    @Nullable private Class<?>[][] optionalParameterTypes;
    @Nullable private Class<?>[][] optionalThrownExceptions;

    @Nullable private Method[] result; // null=uncooked

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
    setThrownExceptions(Class<?>[] thrownExceptions) { this.setThrownExceptions(new Class[][] { thrownExceptions }); }

    @Override public void
    setOverrideMethod(boolean[] overrideMethod) { this.optionalOverrideMethod = overrideMethod.clone(); }

    @Override public void
    setStaticMethod(boolean[] staticMethod) { this.optionalStaticMethod = staticMethod.clone(); }

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
    setReturnTypes(Class<?>[] returnTypes) { this.optionalReturnTypes = returnTypes.clone(); }

    @Override public void
    setMethodNames(String[] methodNames) { this.optionalMethodNames = methodNames.clone(); }

    @Override public void
    setParameters(String[][] parameterNames, Class<?>[][] parameterTypes) {
        this.optionalParameterNames = parameterNames.clone();
        this.optionalParameterTypes = parameterTypes.clone();
    }

    @Override public void
    setThrownExceptions(Class<?>[][] thrownExceptions) { this.optionalThrownExceptions = thrownExceptions.clone(); }

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
     * @throws IllegalStateException Any of the preceding {@code set...()} had an array size different from that
     *                               of {@code scanners}
     */
    public final void
    cook(Scanner[] scanners) throws CompileException, IOException {

        Parser[] parsers = new Parser[scanners.length];
        for (int i = 0; i < scanners.length; ++i) {
            parsers[i] = new Parser(scanners[i]);
        }
        this.cook(parsers);
    }

    /**
     * @see #cook(Scanner[])
     */
    public final void
    cook(Parser[] parsers) throws CompileException, IOException {

        // The "dimension" of this ScriptEvaluator, i.e. how many scripts are cooked at the same
        // time.
        int count = parsers.length;

        // SUPPRESS CHECKSTYLE LineLength:7
        final String[]     mns = ScriptEvaluator.array(this.optionalMethodNames,      count, ScriptEvaluator.DEFAULT_METHOD_NAME);
        final String[][]   pns = ScriptEvaluator.array(this.optionalParameterNames,   count, new String[0],               String[].class);
        final Class<?>[][] pts = ScriptEvaluator.array(this.optionalParameterTypes,   count, new Class[0],                Class[].class);
        final boolean[]    oms = ScriptEvaluator.array(this.optionalOverrideMethod,   count, false);
        final Class<?>[]   rts = ScriptEvaluator.array(this.optionalReturnTypes,      count, this.getDefaultReturnType(), Class.class);
        final boolean[]    sms = ScriptEvaluator.array(this.optionalStaticMethod,     count, true);
        final Class<?>[][] tes = ScriptEvaluator.array(this.optionalThrownExceptions, count, new Class[0],                Class[].class);

        // Create compilation unit.
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(count == 1 ? parsers[0] : null);

        // Create class declaration.
        final Java.AbstractClassDeclaration
        cd = this.addPackageMemberClassDeclaration(parsers[0].location(), compilationUnit);

        // Create methods with one block each.
        for (int i = 0; i < count; ++i) {

            Parser parser = parsers[i];

            // Create the statements of the method.
            List<Java.BlockStatement>   statements   = new ArrayList<Java.BlockStatement>();
            List<Java.MethodDeclarator> localMethods = new ArrayList<Java.MethodDeclarator>();
            this.makeStatements(i, parser, statements, localMethods);

            // Create the method that holds the statements.
            Location loc = parser.location();
            cd.addDeclaredMethod(this.makeMethodDeclaration(
                loc,       // location
                (          // annotations
                    oms[i]     // If the method is non-static, assume that it overrides a method in a supertype
                    ? new Java.Annotation[] { new Java.MarkerAnnotation(this.classToType(loc, Override.class)) }
                    : new Java.Annotation[0]
                ),
                sms[i],    // staticMethod
                rts[i],    // returnType
                mns[i],    // methodName
                pts[i],    // parameterTypes
                pns[i],    // parameterNames
                tes[i],    // thrownExceptions
                statements // statements
            ));

            // Also add the "local methods" that a script my declare.
            for (MethodDeclarator method : localMethods) {
                cd.addDeclaredMethod(method);
            }
        }

        this.cook2(count, compilationUnit);
    }

    /**
     * Compiles the given <var>compilationUnit</var>, defines it into a {@link ClassLoader}, loads the generated class,
     * gets the script methods from that class, and makes them available through {@link #getMethod(int)}.
     */
    protected void
    cook2(int count, CompilationUnit compilationUnit) throws CompileException {

        // Compile and load the compilation unit.
        Class<?> c = this.compileToClass(compilationUnit);

        // SUPPRESS CHECKSTYLE LineLength:2
        final String[]     mns = ScriptEvaluator.array(this.optionalMethodNames,    count, ScriptEvaluator.DEFAULT_METHOD_NAME);
        final Class<?>[][] pts = ScriptEvaluator.array(this.optionalParameterTypes, count, new Class[0], Class[].class);

        // Find the script methods by name.
        Method[] methods = new Method[count];
        if (count <= 10) {
            for (int i = 0; i < count; ++i) {

                String     methodName     = mns[i];
                Class<?>[] parameterTypes = pts[i];

                try {
                    methods[i] = c.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ex) {
                    throw new InternalCompilerException((
                        "SNO: Loaded class does not declare method \""
                        + methodName
                        + "\""
                    ), ex);
                }
            }
        } else
        {
            class MethodWrapper {

                private final String     name;
                private final Class<?>[] parameterTypes;

                MethodWrapper(String name, Class<?>[] parameterTypes) {
                    this.name           = name;
                    this.parameterTypes = parameterTypes;
                }

                @Override public boolean
                equals(@Nullable Object o) {
                    if (!(o instanceof MethodWrapper)) return false;
                    MethodWrapper that = (MethodWrapper) o;
                    if (!this.name.equals(that.name)) return false;
                    int cnt = this.parameterTypes.length;
                    if (cnt != that.parameterTypes.length) return false;
                    for (int i = 0; i < cnt; ++i) {
                        if (!this.parameterTypes[i].equals(that.parameterTypes[i])) return false;
                    }
                    return true;
                }

                @Override public int
                hashCode() {
                    int hc = this.name.hashCode();
                    for (Class<?> parameterType : this.parameterTypes) hc ^= parameterType.hashCode();
                    return hc;
                }
            }
            Method[]                   ma  = c.getDeclaredMethods();
            Map<MethodWrapper, Method> dms = new HashMap<MethodWrapper, Method>(2 * count);
            for (Method m : ma) dms.put(new MethodWrapper(m.getName(), m.getParameterTypes()), m);
            for (int i = 0; i < count; ++i) {

                String     methodName     = mns[i];
                Class<?>[] parameterTypes = pts[i];

                Method m = (Method) dms.get(new MethodWrapper(methodName, parameterTypes));
                if (m == null) {
                    throw new InternalCompilerException(
                        "SNO: Loaded class does not declare method \""
                        + methodName
                        + "\""
                    );
                }
                methods[i] = m;
            }
        }

        this.result = methods;
    }

    private static boolean[]
    array(@Nullable boolean[] subject, int count, boolean defaultValue) {

        boolean[] result;

        if (subject == null) {
            result = new boolean[count];
            if (defaultValue) {
                for (int i = 0; i < count; i++) result[i] = true;
            }
        } else {
            if (subject.length != count) throw new IllegalStateException();
            result = subject;
        }

        return result;
    }

    private static String[]
    array(@Nullable String[] subject, int count, @Nullable String defaultValue) {

        String[] result;

        if (subject == null) {
            result = new String[count];
        } else {
            if (subject.length != count) throw new IllegalStateException();
            result = subject;
        }

        if (defaultValue != null) {
            for (int i = 0; i < count; i++) {
                if (result[i] == null) result[i] = defaultValue.replace("*", Integer.toString(i));
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked") private static <T> T[]
    array(@Nullable T[] subject, int count, @Nullable T defaultValue, Class<T> componentType) {

        T[] result;

        if (subject == null) {
            result = (T[]) Array.newInstance(componentType, count);
        } else {
            if (subject.length != count) throw new IllegalStateException();
            result = subject.clone();
        }

        if (defaultValue != null) {
            for (int i = 0; i < count; i++) {
                if (result[i] == null) result[i] = defaultValue;
            }
        }

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
    getMethod(int idx) { return this.assertCooked()[idx]; }

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
    getReturnType(int i) {

        if (this.optionalReturnTypes != null) {
            Class<?> rt = this.optionalReturnTypes[i];
            assert rt != null;
            return rt;
        }

        return this.getDefaultReturnType();
    }

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

    /**
     * To the given {@link Java.AbstractClassDeclaration}, adds
     * <ul>
     *   <li>
     *     A public method declaration with the given return type, name, parameter names and values and thrown
     *     exceptions
     *   </li>
     *   <li>A block</li>
     * </ul>
     *
     * @param returnType Return type of the declared method
     */
    protected Java.MethodDeclarator
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
                staticMethod ? (short) (Mod.PUBLIC | Mod.STATIC) : (short) Mod.PUBLIC,
                annotations
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

    private Method[]
    assertCooked() {

        if (this.result != null) return this.result;

        throw new IllegalStateException("Must only be called after \"cook()\"");
    }
}
