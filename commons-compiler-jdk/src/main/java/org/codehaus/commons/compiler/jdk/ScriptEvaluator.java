
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

package org.codehaus.commons.compiler.jdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.io.MultiReader;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * To set up a {@link ScriptEvaluator} object, proceed as described for {@link IScriptEvaluator}. Alternatively, a
 * number of "convenience constructors" exist that execute the described steps instantly.
 * <p>
 *   Alternatively, a number of "convenience constructors" exist that execute the steps described above instantly.
 *   Their use is discouraged.
 * </p>
 * <p>
 *   <b>Notice that this implementation of {@link IClassBodyEvaluator} is prone to "Java injection", i.e. an
 *   application could get more than one class body compiled by passing a bogus input document.</b>
 * </p>
 * <p>
 *   <b>Also notice that the parsing of leading IMPORT declarations is heuristic and has certain limitations; see
 *   {@link #parseImportDeclarations(Reader)}.</b>
 * </p>
 */
public
class ScriptEvaluator extends ClassBodyEvaluator implements IScriptEvaluator {

    /**
     * Whether methods override a method declared by a supertype; {@code null} means "none".
     */
    @Nullable protected boolean[] optionalOverrideMethod;

    /**
     * Whether methods are static; {@code null} means "all".
     */
    @Nullable protected boolean[] optionalStaticMethod;

    @Nullable private Class<?>[]   optionalReturnTypes;
    @Nullable private String[]     optionalMethodNames;
    @Nullable private String[][]   optionalParameterNames;
    @Nullable private Class<?>[][] optionalParameterTypes;
    @Nullable private Class<?>[][] optionalThrownExceptions;

    /**
     * null=uncooked
     */
    @Nullable private Method[] result;

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
    ScriptEvaluator(String script) throws CompileException { this.cook(script); }

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
     * @param optionalParentClassLoader {@code null} means use current thread's context class loader
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
        @Nullable ClassLoader optionalParentClassLoader
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
     * @param optionalParentClassLoader {@code null} means use current thread's context class loader
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
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(optionalFileName, reader);
    }

    public ScriptEvaluator() {}

    @Override public void
    setOverrideMethod(boolean overrideMethod) { this.setOverrideMethod(new boolean[] { overrideMethod }); }

    @Override public void
    setStaticMethod(final boolean staticMethod) { this.setStaticMethod(new boolean[] { staticMethod }); }

    @Override public void
    setReturnType(Class<?> returnType) { this.setReturnTypes(new Class<?>[] { returnType }); }

    @Override public void
    setMethodName(String methodName) { this.setMethodNames(new String[] { methodName }); }

    @Override public void
    setParameters(String[] names, Class<?>[] types) {
        this.setParameters(new String[][] { names }, new Class<?>[][] { types });
    }

    @Override public void
    setThrownExceptions(Class<?>[] thrownExceptions) {
        this.setThrownExceptions(new Class[][] { thrownExceptions });
    }

    @Override public void
    cook(@Nullable String optionalFileName, Reader r) throws CompileException, IOException {
        this.cook(new String[] { optionalFileName }, new Reader[] { r });
    }

    @Override @Nullable public Object
    evaluate(@Nullable Object[] arguments) throws InvocationTargetException { return this.evaluate(0, arguments); }

    @Override public Method
    getMethod() { return this.getMethod(0); }

    @Override public void
    setOverrideMethod(boolean[] overrideMethod) { this.optionalOverrideMethod = overrideMethod.clone(); }

    @Override public void
    setStaticMethod(boolean[] staticMethod) { this.optionalStaticMethod = staticMethod.clone(); }

    @Override public void
    setReturnTypes(Class<?>[] returnTypes) {
        for (Class<?> rt : returnTypes) assert rt != null;
        this.optionalReturnTypes = returnTypes.clone();
    }

    @Override public void
    setMethodNames(String[] methodNames) { this.optionalMethodNames = methodNames.clone(); }

    @Override public void
    setParameters(String[][] names, Class<?>[][] types) {
        this.optionalParameterNames = names.clone();
        this.optionalParameterTypes = types.clone();
    }

    @Override public void
    setThrownExceptions(Class<?>[][] thrownExceptions) { this.optionalThrownExceptions = thrownExceptions.clone(); }

    @Override public final void
    cook(Reader[] readers) throws CompileException, IOException { this.cook(null, readers); }

    @Override public void
    cook(@Nullable String[] optionalFileNames, Reader[] readers) throws CompileException, IOException {
        String[] imports;

        if (readers.length == 1) {
            if (!readers[0].markSupported()) {
                readers = new Reader[] { new BufferedReader(readers[0]) };
            }
            imports = ClassBodyEvaluator.parseImportDeclarations(readers[0]);
        } else
        {
            imports = new String[0];
        }

        this.cook(optionalFileNames, readers, imports);
    }

    @Override public final void
    cook(String[] strings) throws CompileException { this.cook(null, strings); }

    @Override public void
    cook(@Nullable String[] optionalFileNames, String[] strings) throws CompileException {
        Reader[] readers = new Reader[strings.length];
        for (int i = 0; i < strings.length; ++i) readers[i] = new StringReader(strings[i]);
        try {
            this.cook(optionalFileNames, readers);
        } catch (IOException ioe) {
            throw new RuntimeException("SNO: IOException despite StringReader", ioe);
        }
    }

    /**
     * @param readers The scripts to cook
     */
    protected final void
    cook(@Nullable String[] optionalFileNames, Reader[] readers, String[] imports)
    throws CompileException, IOException {

        // SUPPRESS CHECKSTYLE UsageDistance:7
        String[]     omns = this.optionalMethodNames;
        boolean[]    oom  = this.optionalOverrideMethod;
        boolean[]    osm  = this.optionalStaticMethod;
        Class<?>[]   orts = this.optionalReturnTypes;
        String[][]   opns = this.optionalParameterNames;
        Class<?>[][] opts = this.optionalParameterTypes;
        Class<?>[][] otes = this.optionalThrownExceptions;

        // The "dimension" of this ScriptEvaluator, i.e. how many scripts are cooked at the same
        // time.
        int count = readers.length;

        // Check array sizes.
        if (omns != null && omns.length != count) throw new IllegalStateException("methodName");
        if (opns != null && opns.length != count) throw new IllegalStateException("parameterNames");
        if (opts != null && opts.length != count) throw new IllegalStateException("parameterTypes");
        if (orts != null && orts.length != count) throw new IllegalStateException("returnTypes");
        if (oom  != null && oom.length  != count) throw new IllegalStateException("overrideMethod");
        if (osm  != null && osm.length  != count) throw new IllegalStateException("staticMethod");
        if (otes != null && otes.length != count) throw new IllegalStateException("thrownExceptions");

        // Determine method names.
        if (omns == null) {
            omns = new String[count];
            for (int i = 0; i < count; ++i) omns[i] = "eval" + i;
        }

        // Create compilation unit.
        List<Reader> classBody = new ArrayList<Reader>();

        // Create methods with one block each.
        for (int i = 0; i < count; ++i) {
            boolean overrideMethod = oom != null && oom[i];
            boolean staticMethod   = osm   == null || osm[i];

            final Class<?>   returnType       = orts != null ? orts[i] : this.getDefaultReturnType();
            final String[]   parameterNames   = opns != null ? opns[i] : new String[0];
            final Class<?>[] parameterTypes   = opts != null ? opts[i] : new Class<?>[0];
            final Class<?>[] thrownExceptions = otes != null ? otes[i] : new Class<?>[0];

            {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);

                if (overrideMethod) pw.print("@Override ");
                pw.print("public ");
                if (staticMethod) pw.print("static ");
                pw.print(returnType.getCanonicalName());
                pw.print(" ");
                pw.print(omns[i]);
                pw.print("(");
                for (int j = 0; j < parameterNames.length; ++j) {
                    if (j > 0) pw.print(", ");
                    pw.print(parameterTypes[j].getName());
                    pw.print(" ");
                    pw.print(parameterNames[j]);
                }
                pw.print(")");
                for (int j = 0; j < thrownExceptions.length; ++j) {
                    pw.print(j == 0 ? " throws " : ", ");
                    pw.print(thrownExceptions[j].getName());
                }
                pw.println(" {");
                pw.close();
                classBody.add(new StringReader(sw.toString()));
            }

            classBody.add(readers[i]);

            {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);
                pw.println("}");
                pw.close();
                classBody.add(new StringReader(sw.toString()));
            }
        }

        super.cook(optionalFileNames == null ? null : optionalFileNames[0], imports, new MultiReader(classBody));

        Class<?> c = this.getClazz();

        // Find the script methods by name.
        Method[] methods = (this.result = new Method[count]);
        if (count <= 10) {
            for (int i = 0; i < count; ++i) {
                try {
                    methods[i] = c.getDeclaredMethod(
                        omns[i],
                        opts == null ? new Class[0] : opts[i]
                    );
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(
                        "SNO: Loaded class does not declare method \"" + omns[i] + "\"",
                        ex
                    );
                }
            }
        } else
        {

            // "getDeclaredMethod()" implements a linear search which is inefficient like hell for classes with MANY
            // methods (like an ExpressionEvaluator with a 10000 methods). Thus we se "getDeclaredMethods()" and sort
            // things out ourselves with a HashMap.
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
                    return (
                        this.name.equals(that.name)
                        && Arrays.equals(this.parameterTypes, that.parameterTypes)
                    );
                }

                @Override public int
                hashCode() { return this.name.hashCode() ^ Arrays.hashCode(this.parameterTypes); }
            }
            Method[]                   ma  = c.getDeclaredMethods();
            Map<MethodWrapper, Method> dms = new HashMap<MethodWrapper, Method>(2 * count);
            for (Method m : ma) {
                dms.put(new MethodWrapper(m.getName(), m.getParameterTypes()), m);
            }
            for (int i = 0; i < count; ++i) {
                Method m = dms.get(new MethodWrapper(
                    omns[i],
                    opts == null ? new Class[0] : opts[i]
                ));
                if (m == null) {
                    throw new RuntimeException("SNO: Loaded class does not declare method \"" + omns[i] + "\"");
                }
                methods[i] = m;
            }
        }
    }

    /**
     * The default return type of a script is {@code void}.
     */
    protected Class<?>
    getDefaultReturnType() { return void.class; }

    /**
     * @return The return type of the <var>i</var>th script
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
     * @param script Contains the sequence of script tokens
     * @see #createFastEvaluator(String, Class, String[])
     */
    @Override public <T> Object
    createFastEvaluator(
        String   script,
        Class<T> interfaceToImplement,
        String[] parameterNames
    ) throws CompileException {
        try {
            return this.createFastEvaluator(new StringReader(script), interfaceToImplement, parameterNames);
        } catch (IOException ioe) {
            throw new RuntimeException("SNO: IOException despite StringReader", ioe);
        }
    }

    /**
     * Don't use.
     */
    @Override public final Object
    createInstance(Reader reader) { throw new UnsupportedOperationException("createInstance"); }

    @Override public <T> Object
    createFastEvaluator(
        Reader   r,
        Class<T> interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, IOException {
        if (!interfaceToImplement.isInterface()) {
            throw new RuntimeException("\"" + interfaceToImplement + "\" is not an interface");
        }

        this.setImplementedInterfaces(new Class[] { interfaceToImplement });

        this.setStaticMethod(false);

        Method[] methods = interfaceToImplement.getDeclaredMethods();
        if (methods.length != 1) {
            throw new RuntimeException("Interface \"" + interfaceToImplement + "\" must declare exactly one method");
        }
        Method methodToImplement = methods[0];

        if (this instanceof IExpressionEvaluator) {

            // Must not call "IExpressionEvaluator.setReturnType()".
            ((IExpressionEvaluator) this).setExpressionType(methodToImplement.getReturnType());
        } else {
            this.setReturnType(methodToImplement.getReturnType());
        }
        this.setMethodName(methodToImplement.getName());
        this.setParameters(parameterNames, methodToImplement.getParameterTypes());
        this.setThrownExceptions(methodToImplement.getExceptionTypes());
        this.cook(r);
        Class<?> c = this.getMethod().getDeclaringClass();
        try {
            return c.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("SNO - Declared class is always non-abstract", e);
        } catch (Exception e) {
            throw new CompileException("Instantiating \"" + c.getName() + "\"", null, e);
        }
    }

    @Override @Nullable public Object
    evaluate(int idx, @Nullable Object[] arguments) throws InvocationTargetException {
        try {
            return this.getMethods()[idx].invoke(null, arguments);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex.toString(), ex);
        }
    }

    @Override public Method
    getMethod(int idx) {
        return this.getMethods()[idx];
    }

    /**
     * @throws IllegalStateException This {@link ScriptEvaluator} has not yet been {@code cook()}ed
     */
    protected Method[]
    getMethods() {
        if (this.result != null) return this.result;
        throw new IllegalStateException("\"cook()\" has not yet been called");
    }
}
