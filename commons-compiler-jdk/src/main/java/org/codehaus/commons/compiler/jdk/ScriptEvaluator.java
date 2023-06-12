
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
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.MultiCookable;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.io.Readers;
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
 *   {@link ClassBodyEvaluator#parseImportDeclarations(Reader)}.</b>
 * </p>
 */
public
class ScriptEvaluator extends MultiCookable implements IScriptEvaluator {

    final ClassBodyEvaluator cbe = new ClassBodyEvaluator();

    private static
    class Script {

        /**
         * Whether the generated method overrides a method declared by a supertype.
         */
        protected boolean overrideMethod;

        /**
         * Whether the method is generated {@code static}; defaults to {@code true}.
         */
        protected boolean staticMethod = true;

        /**
         * The generated method's return type. {@code null} means "use the {@link
         * IScriptEvaluator#getDefaultReturnType() default return type}".
         *
         * @see ScriptEvaluator#setDefaultReturnType(Class)
         */
        @Nullable private Class<?> returnType;

        /**
         * The name of the generated method. {@code null} means to use a reasonable default.
         */
        @Nullable private String methodName;

        private String[]   parameterNames   = {};
        private Class<?>[] parameterTypes   = {};
        private Class<?>[] thrownExceptions = {};
    }

    /**
     * The scripts to compile. Is initialized on the first call to {@link #setStaticMethod(boolean[])} or one of its
     * friends.
     */
    @Nullable private Script[] scripts;

    private Class<?> defaultReturnType = IScriptEvaluator.DEFAULT_RETURN_TYPE;

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
     *     se.setParentClassLoader(parentClassLoader);
     *     se.cook(fileName, is);
     * </pre>
     *
     * @param parentClassLoader {@code null} means use current thread's context class loader
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, InputStream)
     */
    public
    ScriptEvaluator(
        @Nullable String      fileName,
        InputStream           is,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(parentClassLoader);
        this.cook(fileName, is);
    }

    /**
     * Equivalent to
     * <pre>
     *     ScriptEvaluator se = new ScriptEvaluator();
     *     se.setReturnType(returnType);
     *     se.setParameters(parameterNames, parameterTypes);
     *     se.setThrownExceptions(thrownExceptions);
     *     se.setParentClassLoader(parentClassLoader);
     *     se.cook(reader);
     * </pre>
     *
     * @param parentClassLoader {@code null} means use current thread's context class loader
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, Reader)
     */
    public
    ScriptEvaluator(
        @Nullable String      fileName,
        Reader                reader,
        Class<?>              returnType,
        String[]              parameterNames,
        Class<?>[]            parameterTypes,
        Class<?>[]            thrownExceptions,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(parentClassLoader);
        this.cook(fileName, reader);
    }

    public ScriptEvaluator() {}

    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) { this.cbe.setParentClassLoader(parentClassLoader); }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.cbe.setDebuggingInformation(debugSource, debugLines, debugVars);
    }

    @Override public void
    setSourceVersion(int version) { this.cbe.setSourceVersion(version); }

    @Override public void
    setTargetVersion(int version) { this.cbe.setTargetVersion(version); }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.cbe.setCompileErrorHandler(compileErrorHandler);
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.cbe.setWarningHandler(warningHandler);
    }

    /**
     * @throws IllegalArgumentException <var>count</var> is different from previous invocations of
     *                                  this method
     */
    public void
    setScriptCount(int count) {

        Script[] ss = this.scripts;

        if (ss == null) {
            this.scripts = (ss = new Script[count]);
            for (int i = 0; i < count; i++) ss[i] = new Script();
        } else
        if (count != ss.length) {
            throw new IllegalArgumentException(
                "Inconsistent script count; previously " + ss.length + ", now " + count
            );
        }
    }

    private Script
    getScript(int index) {
        if (this.scripts != null) return this.scripts[index];
        throw new IllegalStateException("\"getScript()\" invoked before \"setScriptCount()\"");
    }

    @Override public void
    setClassName(String className) {
        this.cbe.setClassName(className);
    }

    @Override public void
    setImplementedInterfaces(Class<?>[] implementedInterfaces) {
        this.cbe.setImplementedInterfaces(implementedInterfaces);
    }

    @Override public void
    setExtendedClass(@Nullable Class<?> extendedClass) { this.cbe.setExtendedClass(extendedClass); }

    @Override public void
    setDefaultReturnType(Class<?> defaultReturnType) { this.defaultReturnType = defaultReturnType; }

    @Override public Class<?>
    getDefaultReturnType() { return this.defaultReturnType; }

    @Override public void
    setDefaultImports(String... defaultImports) { this.cbe.setDefaultImports(defaultImports); }

    @Override public String[]
    getDefaultImports() { return this.cbe.getDefaultImports(); }

    @Override public void
    setOverrideMethod(boolean overrideMethod) { this.setOverrideMethod(new boolean[] { overrideMethod }); }

    @Override public void
    setStaticMethod(final boolean staticMethod) { this.setStaticMethod(new boolean[] { staticMethod }); }

    @Override public void
    setReturnType(Class<?> returnType) { this.setReturnTypes(new Class<?>[] { returnType }); }

    @Override public void
    setMethodName(@Nullable String methodName) { this.setMethodNames(new String[] { methodName }); }

    @Override public void
    setParameters(String[] names, Class<?>[] types) {
        this.setParameters(new String[][] { names }, new Class<?>[][] { types });
    }

    @Override public void
    setThrownExceptions(Class<?>[] thrownExceptions) {
        this.setThrownExceptions(new Class[][] { thrownExceptions });
    }

    @Override @Nullable public Object
    evaluate() throws InvocationTargetException { return this.evaluate(new Object[0]); }

    @Override @Nullable public Object
    evaluate(@Nullable Object[] arguments) throws InvocationTargetException { return this.evaluate(0, arguments); }

    @Override public Method
    getMethod() { return this.getMethod(0); }

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

    @Override public void
    setReturnTypes(Class<?>[] returnTypes) {
        this.setScriptCount(returnTypes.length);
        for (int i = 0; i < returnTypes.length; i++) this.getScript(i).returnType = returnTypes[i];
    }

    @Override public void
    setMethodNames(String[] methodNames) {
        this.setScriptCount(methodNames.length);
        for (int i = 0; i < methodNames.length; i++) this.getScript(i).methodName = methodNames[i];
    }

    @Override public void
    setParameters(String[][] parameterNames, Class<?>[][] parameterTypes) {

        this.setScriptCount(parameterNames.length);
        this.setScriptCount(parameterTypes.length);

        for (int i = 0; i < parameterNames.length; i++) {
            final Script script = this.getScript(i);
            script.parameterNames = parameterNames[i].clone();
            script.parameterTypes = parameterTypes[i].clone();
        }
    }

    @Override public void
    setThrownExceptions(Class<?>[][] thrownExceptions) {
        this.setScriptCount(thrownExceptions.length);
        for (int i = 0; i < thrownExceptions.length; i++) this.getScript(i).thrownExceptions = thrownExceptions[i];
    }

    // ---------------------------------------------------------------

    @Override public void
    cook(@Nullable String fileName, Reader reader) throws CompileException, IOException {
        String[] imports;

        if (!reader.markSupported()) reader = new BufferedReader(reader);
        imports = ClassBodyEvaluator.parseImportDeclarations(reader);

        this.cook(new String[] { fileName }, new Reader[] { reader }, imports);
    }

    @Override public void
    cook(String[] fileNames, Reader[] readers) throws CompileException, IOException {

        if (readers.length == 1) {

            Reader r = readers[0];
            if (!r.markSupported()) r = new BufferedReader(r);

            this.cook(fileNames, new Reader[] { r }, ClassBodyEvaluator.parseImportDeclarations(r));
        } else {
            this.cook(fileNames, readers, new String[0]);
        }
    }

    /**
     * @param readers The scripts to cook
     */
    protected final void
    cook(String[] fileNames, Reader[] readers, String[] imports)
    throws CompileException, IOException {

        this.setScriptCount(fileNames.length);
        this.setScriptCount(readers.length);

        // The "dimension" of this ScriptEvaluator, i.e. how many scripts are cooked at the same
        // time.
        int count = readers.length;

        // Create compilation unit.
        List<Reader> classBody = new ArrayList<>();

        // Create methods with one block each.
        for (int idx = 0; idx < count; ++idx) {

            Script s = this.getScript(idx);

            final boolean    overrideMethod   = s.overrideMethod;
            final boolean    staticMethod     = s.staticMethod;
            final Class<?>   returnType       = s.returnType != null ? s.returnType : this.getDefaultReturnType();
            final String     methodName       = this.getMethodName(idx);
            final String[]   parameterNames   = s.parameterNames;
            final Class<?>[] parameterTypes   = s.parameterTypes;
            final Class<?>[] thrownExceptions = s.thrownExceptions;

            {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);

                if (overrideMethod) pw.print("@Override ");
                pw.print("public ");
                if (staticMethod) pw.print("static ");
                pw.print(returnType.getCanonicalName());
                pw.print(" ");
                pw.print(methodName);
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

            classBody.add(this.cbe.newFileName((
                fileNames[idx] != null ? fileNames[idx] :
                idx == 0               ? null           :
                "[" + idx + "]"
            ), readers[idx]));

            {
                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);
                pw.println("}");
                pw.close();
                classBody.add(new StringReader(sw.toString()));
            }
        }

        this.cbe.cook(fileNames[0], imports, Readers.concat(classBody));

        Class<?> c = this.getClazz();

        // Find the script methods by name.
        Method[] methods = (this.result = new Method[count]);
        if (count <= 10) {
            for (int idx = 0; idx < count; ++idx) {

                Script sript      = this.getScript(idx);
                String methodName = this.getMethodName(idx);

                try {
                    methods[idx] = c.getDeclaredMethod(methodName, sript.parameterTypes);
                } catch (NoSuchMethodException nsme) {
                    throw new RuntimeException(
                        "SNO: Loaded class does not declare method \"" + methodName + "\"",
                        nsme
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
            Map<MethodWrapper, Method> dms = new HashMap<>(2 * count);
            for (Method m : ma) {
                dms.put(new MethodWrapper(m.getName(), m.getParameterTypes()), m);
            }
            for (int idx = 0; idx < count; ++idx) {
                String     methodName     = this.getMethodName(idx);
                Class<?>[] parameterTypes = this.getScript(idx).parameterTypes;

                Method m = dms.get(new MethodWrapper(methodName, parameterTypes));
                if (m == null) {
                    throw new RuntimeException("SNO: Loaded class does not declare method \"" + methodName + "\"");
                }

                methods[idx] = m;
            }
        }
    }

    private String
    getMethodName(int idx) {

        String result = this.getScript(idx).methodName;

        if (result == null) result = IScriptEvaluator.DEFAULT_METHOD_NAME.replace("*", Integer.toString(idx));

        return result;
    }

    /**
     * @return The return type of the <var>i</var>th script
     */
    protected final Class<?>
    getReturnType(int i) {

        Class<?> returnType = this.getScript(i).returnType;

        return returnType != null ? returnType : this.getDefaultReturnType();
    }

    /**
     * @param script Contains the sequence of script tokens
     * @see #createFastEvaluator(String, Class, String[])
     */
    @Override public <T> T
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

    @Override public <T> T
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
            @SuppressWarnings("unchecked") T instance = (T) c.getConstructor().newInstance();
            return instance;
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

    @Override public Method[]
    getResult() { return this.getMethods(); }

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

    @Override public Class<?>
    getClazz() { return this.cbe.getClazz(); }

    @Override public Map<String, byte[]>
    getBytecodes() { return this.cbe.getBytecodes(); }
}
