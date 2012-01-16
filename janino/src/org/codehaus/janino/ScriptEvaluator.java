
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.util.Traverser;

/**
 * A number of "convenience constructors" exist that execute the setup steps instantly. Their use is discouraged.
 */
public class ScriptEvaluator extends ClassBodyEvaluator implements IScriptEvaluator {

    protected boolean[]  optionalStaticMethod = null;
    protected Class[]    optionalReturnTypes = null;
    protected String[]   optionalMethodNames = null;
    protected String[][] optionalParameterNames = null;
    protected Class[][]  optionalParameterTypes = null;
    protected Class[][]  optionalThrownExceptions = null;

    private Method[]     result = null; // null=uncooked

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.cook(script);</pre>
     *
     * @see #ScriptEvaluator()
     * @see Cookable#cook(String)
     */
    public ScriptEvaluator(
        String   script
    ) throws CompileException {
        this.cook(script);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.cook(script);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see Cookable#cook(String)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType
    ) throws CompileException {
        this.setReturnType(returnType);
        this.cook(script);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.cook(script);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see Cookable#cook(String)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType,
        String[] parameterNames,
        Class[]  parameterTypes
    ) throws CompileException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.cook(script);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.cook(script);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see Cookable#cook(String)
     */
    public ScriptEvaluator(
        String   script,
        Class    returnType,
        String[] parameterNames,
        Class[]  parameterTypes,
        Class[]  thrownExceptions
    ) throws CompileException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.cook(script);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.setParentClassLoader(optionalParentClassLoader);
     * se.cook(optionalFileName, is);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, InputStream)
     */
    public ScriptEvaluator(
        String      optionalFileName,
        InputStream is,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(optionalFileName, is);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.setParentClassLoader(optionalParentClassLoader);
     * se.cook(reader);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(String, Reader)
     */
    public ScriptEvaluator(
        String      optionalFileName,
        Reader      reader,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(optionalFileName, reader);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.setParentClassLoader(optionalParentClassLoader);
     * se.cook(scanner);</pre>
     *
     * @see #ScriptEvaluator()
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public ScriptEvaluator(
        Scanner     scanner,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, IOException {
        this.setReturnType(returnType);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setExtendedType(optionalExtendedType);
     * se.setImplementedTypes(implementedTypes);
     * se.setReturnType(returnType);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.setParentClassLoader(optionalParentClassLoader);
     * se.cook(scanner);</pre>
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
    public ScriptEvaluator(
        Scanner     scanner,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
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
     * Equivalent to<pre>
     * ScriptEvaluator se = new ScriptEvaluator();
     * se.setClassName(className);
     * se.setExtendedType(optionalExtendedType);
     * se.setImplementedTypes(implementedTypes);
     * se.setStaticMethod(staticMethod);
     * se.setReturnType(returnType);
     * se.setMethodName(methodName);
     * se.setParameters(parameterNames, parameterTypes);
     * se.setThrownExceptions(thrownExceptions);
     * se.setParentClassLoader(optionalParentClassLoader);
     * se.cook(scanner);</pre>
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
    public ScriptEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        boolean     staticMethod,
        Class       returnType,
        String      methodName,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
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

    public ScriptEvaluator() {}

    public void setStaticMethod(boolean staticMethod) {
        this.setStaticMethod(new boolean[] { staticMethod });
    }

    public void setReturnType(Class returnType) {
        this.setReturnTypes(new Class[] { returnType });
    }

    public void setMethodName(String methodName) {
        this.setMethodNames(new String[] { methodName });
    }

    public void setParameters(String[] parameterNames, Class[] parameterTypes) {
        this.setParameters(new String[][] { parameterNames }, new Class[][] {parameterTypes });
    }

    public void setThrownExceptions(Class[] thrownExceptions) {
        this.setThrownExceptions(new Class[][] { thrownExceptions });
    }

    public final void cook(Scanner scanner) throws CompileException, IOException {
        this.cook(new Scanner[] { scanner });
    }

    public Object evaluate(Object[] arguments) throws InvocationTargetException {
        return this.evaluate(0, arguments);
    }

    public Method getMethod() {
        return this.getMethod(0);
    }

    public void setStaticMethod(boolean[] staticMethod) {
        assertNotCooked();
        this.optionalStaticMethod = (boolean[]) staticMethod.clone();
    }

    public void setReturnTypes(Class[] returnTypes) {
        assertNotCooked();
        this.optionalReturnTypes = (Class[]) returnTypes.clone();
    }

    public void setMethodNames(String[] methodNames) {
        assertNotCooked();
        this.optionalMethodNames = (String[]) methodNames.clone();
    }

    public void setParameters(String[][] parameterNames, Class[][] parameterTypes) {
        assertNotCooked();
        this.optionalParameterNames = (String[][]) parameterNames.clone();
        this.optionalParameterTypes = (Class[][]) parameterTypes.clone();
    }

    public void setThrownExceptions(Class[][] thrownExceptions) {
        assertNotCooked();
        this.optionalThrownExceptions = (Class[][]) thrownExceptions.clone();
    }

    /**
     * Like {@link #cook(Scanner)}, but cooks a <i>set</i> of scripts into one class. Notice that
     * if <i>any</i> of the scripts causes trouble, the entire compilation will fail. If you
     * need to report <i>which</i> of the scripts causes the exception, you may want to use the
     * <code>optionalFileName</code> argument of {@link Scanner#Scanner(String, Reader)} to
     * distinguish between the individual token sources.
     * <p>
     * On a 2 GHz Intel Pentium Core Duo under Windows XP with an IBM 1.4.2 JDK, compiling
     * 10000 expressions "a + b" (integer) takes about 4 seconds and 56 MB of main memory.
     * The generated class file is 639203 bytes large.
     * <p>
     * The number and the complexity of the scripts is restricted by the
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88659">Limitations
     * of the Java Virtual Machine</a>, where the most limiting factor is the 64K entries limit
     * of the constant pool. Since every method with a distinct name requires one entry there,
     * you can define at best 32K (very simple) scripts.
     *
     * If and only if the number of scanners is one, then that single script may contain leading
     * IMPORT directives.
     *
     * @throws IllegalStateException Any of the preceeding <code>set...()</code> had an array size different from that
     *                               of <code>scanners</code>
     */
    public final void cook(Scanner[] scanners) throws CompileException, IOException {
        if (scanners == null) throw new NullPointerException();

        Parser[] parsers = new Parser[scanners.length];
        for (int i = 0; i < scanners.length; ++i) {
            parsers[i] = new Parser(scanners[i]);
        }
        cook(parsers);
    }
    public final void cook(Parser[] parsers) throws CompileException, IOException {

        // The "dimension" of this ScriptEvaluator, i.e. how many scripts are cooked at the same
        // time.
        int count = parsers.length;

        // Check array sizes.
        if (this.optionalMethodNames != null && this.optionalMethodNames.length != count) {
            throw new IllegalStateException("methodName count");
        }
        if (this.optionalParameterNames != null && this.optionalParameterNames.length != count) {
            throw new IllegalStateException("parameterNames count");
        }
        if (this.optionalParameterTypes != null && this.optionalParameterTypes.length != count) {
            throw new IllegalStateException("parameterTypes count");
        }
        if (this.optionalReturnTypes != null && this.optionalReturnTypes.length != count) {
            throw new IllegalStateException("returnTypes count");
        }
        if (this.optionalStaticMethod != null && this.optionalStaticMethod.length != count) {
            throw new IllegalStateException("staticMethod count");
        }
        if (this.optionalThrownExceptions != null && this.optionalThrownExceptions.length != count) {
            throw new IllegalStateException("thrownExceptions count");
        }

        // Create compilation unit.
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(count == 1 ? parsers[0] : null);

        // Create class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(parsers[0].location(), compilationUnit);

        // Determine method names.
        String[] methodNames;
        if (this.optionalMethodNames == null) {
            methodNames = new String[count];
            for (int i = 0; i < count; ++i) methodNames[i] = "eval" + i;
        } else
        {
            methodNames = this.optionalMethodNames;
        }

        // Create methods with one block each.
        for (int i = 0; i < count; ++i) {
            Parser parser = parsers[i];

            List statements = this.makeStatements(i, parser);

            // Determine the following script properties AFTER the call to "makeBlock()",
            // because "makeBlock()" may modify these script properties on-the-fly.
            boolean  staticMethod = this.optionalStaticMethod == null || this.optionalStaticMethod[i];
            Class returnType = (
                this.optionalReturnTypes == null
                ? this.getDefaultReturnType()
                : this.optionalReturnTypes[i]
            );
            String[] parameterNames = (
                this.optionalParameterNames == null
                ? new String[0]
                : this.optionalParameterNames[i]
            );
            Class[] parameterTypes = (
                this.optionalParameterTypes == null
                ? new Class[0]
                : this.optionalParameterTypes[i]
            );
            Class[] thrownExceptions = (
                this.optionalThrownExceptions == null
                ? new Class[0]
                : this.optionalThrownExceptions[i]
            );

            cd.addDeclaredMethod(this.makeMethodDeclaration(
                parser.location(), // location
                staticMethod,       // staticMethod
                returnType,         // returnType
                methodNames[i],     // methodName
                parameterTypes,     // parameterTypes
                parameterNames,     // parameterNames
                thrownExceptions,   // thrownExceptions
                statements          // statements
            ));
        }

        // Compile and load the compilation unit.
        Class c = this.compileToClass(compilationUnit, this.className);

        // Find the script methods by name.
        this.result = new Method[count];
        if (count <= 10) {
            for (int i = 0; i < count; ++i) {
                try {
                    this.result[i] = c.getDeclaredMethod(
                        methodNames[i],
                        this.optionalParameterTypes == null ? new Class[0] : this.optionalParameterTypes[i]
                    );
                } catch (NoSuchMethodException ex) {
                    throw new JaninoRuntimeException(
                        "SNO: Loaded class does not declare method \""
                        + methodNames[i]
                        + "\""
                    );
                }
            }
        } else
        {
            class MethodWrapper {
                private final String  name;
                private final Class[] parameterTypes;
                MethodWrapper(String name, Class[] parameterTypes) {
                    this.name = name;
                    this.parameterTypes = parameterTypes;
                }
                public boolean equals(Object o) {
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
                public int hashCode() {
                    int hc = this.name.hashCode();
                    for (int i = 0; i < this.parameterTypes.length; ++i) {
                        hc ^= this.parameterTypes[i].hashCode();
                    }
                    return hc;
                }
            }
            Method[] ma = c.getDeclaredMethods();
            Map dms = new HashMap(2 * count);
            for (int i = 0; i < ma.length; ++i) {
                Method m = ma[i];
                dms.put(new MethodWrapper(m.getName(), m.getParameterTypes()), m);
            }
            for (int i = 0; i < count; ++i) {
                Method m = (Method) dms.get(new MethodWrapper(
                    methodNames[i],
                    this.optionalParameterTypes == null ? new Class[0] : this.optionalParameterTypes[i]
                ));
                if (m == null) {
                    throw new JaninoRuntimeException(
                        "SNO: Loaded class does not declare method \""
                        + methodNames[i]
                        + "\""
                    );
                }
                this.result[i] = m;
            }
        }
    }

    public final void cook(Reader[] readers) throws CompileException, IOException {
        this.cook(new String[readers.length], readers);
    }

    /**
     * On a 2 GHz Intel Pentium Core Duo under Windows XP with an IBM 1.4.2 JDK, compiling
     * 10000 expressions "a + b" (integer) takes about 4 seconds and 56 MB of main memory.
     * The generated class file is 639203 bytes large.
     * <p>
     * The number and the complexity of the scripts is restricted by the
     * <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#88659">Limitations
     * of the Java Virtual Machine</a>, where the most limiting factor is the 64K entries limit
     * of the constant pool. Since every method with a distinct name requires one entry there,
     * you can define at best 32K (very simple) scripts.
     */
    public final void cook(
        String[] optionalFileNames,
        Reader[] readers
    ) throws CompileException, IOException {
        Scanner[] scanners = new Scanner[readers.length];
        for (int i = 0; i < readers.length; ++i) {
            scanners[i] = new Scanner(optionalFileNames == null ? null : optionalFileNames[i], readers[i]);
        }
        this.cook(scanners);
    }

    public final void cook(String[] strings) throws CompileException {
        this.cook(null, strings);
    }

    public final void cook(
        String[] optionalFileNames,
        String[] strings
    ) throws CompileException {
        Reader[] readers = new Reader[strings.length];
        for (int i = 0; i < strings.length; ++i) readers[i] = new StringReader(strings[i]);
        try {
            this.cook(optionalFileNames, readers);
        } catch (IOException ex) {
            throw new JaninoRuntimeException("SNO: IOException despite StringReader");
        }
    }

    protected Class getDefaultReturnType() {
        return void.class;
    }

    /**
     * Fill the given <code>block</code> by parsing statements until EOF and adding
     * them to the block.
     */
    protected List/*<BlockStatement>*/ makeStatements(
        int    idx,
        Parser parser
    ) throws CompileException, IOException {
        List/*<BlockStatement>*/ statements = new ArrayList();
        while (!parser.peekEOF()) {
            statements.add(parser.parseBlockStatement());
        }

        return statements;
    }

    protected void compileToMethods(
        Java.CompilationUnit compilationUnit,
        String[]             methodNames,
        Class[][]            parameterTypes
    ) throws CompileException {

        // Compile and load the compilation unit.
        Class c = this.compileToClass(
            compilationUnit,                                    // compilationUnit
            this.className
        );

        // Find the script method by name.
        this.result = new Method[methodNames.length];
        for (int i = 0; i < this.result.length; ++i) {
            try {
                this.result[i] = c.getMethod(methodNames[i], parameterTypes[i]);
            } catch (NoSuchMethodException ex) {
                throw new JaninoRuntimeException(
                    "SNO: Loaded class does not declare method \""
                    + this.optionalMethodNames[i]
                    + "\""
                );
            }
        }
    }

    /**
     * To the given {@link Java.ClassDeclaration}, add
     * <ul>
     *   <li>A public method declaration with the given return type, name, parameter
     *       names and values and thrown exceptions
     *   <li>A block
     * </ul>
     *
     * @param returnType Return type of the declared method
     */
    protected Java.MethodDeclarator makeMethodDeclaration(
        Location                 location,
        boolean                  staticMethod,
        Class                    returnType,
        String                   methodName,
        Class[]                  parameterTypes,
        String[]                 parameterNames,
        Class[]                  thrownExceptions,
        List/*<BlockStatement>*/ statements
    ) {
        if (parameterNames.length != parameterTypes.length) {
            throw new JaninoRuntimeException(
                "Lengths of \"parameterNames\" ("
                + parameterNames.length
                + ") and \"parameterTypes\" ("
                + parameterTypes.length
                + ") do not match"
            );
        }

        Java.FunctionDeclarator.FormalParameter[] fps = new Java.FunctionDeclarator.FormalParameter[
            parameterNames.length
        ];
        for (int i = 0; i < fps.length; ++i) {
            fps[i] = new Java.FunctionDeclarator.FormalParameter(
                location,                                      // location
                true,                                          // finaL
                this.classToType(location, parameterTypes[i]), // type
                parameterNames[i]                              // name
            );
        }

        return new Java.MethodDeclarator(
            location,                                        // location
            null,                                            // optionalDocComment
            (                                                // modifiers
                staticMethod ?
                (short) (Mod.PUBLIC | Mod.STATIC) :
                (short) Mod.PUBLIC
            ),
            this.classToType(location, returnType),          // type
            methodName,                                      // name
            fps,                                             // formalParameters
            this.classesToTypes(location, thrownExceptions), // thrownExceptions
            statements                                       // optionalStatements
        );
    }

    /**
     * @deprecated
     * @see #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[], ClassLoader)
     */
    public static Object createFastScriptEvaluator(
        String   script,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException {
        ScriptEvaluator se = new ScriptEvaluator();
        return se.createFastEvaluator(script, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated
     * @see #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[], ClassLoader)
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setParentClassLoader(optionalParentClassLoader);
        return se.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * @deprecated
     * @see #createFastScriptEvaluator(Scanner, String[], String, Class, Class, String[], ClassLoader)
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setClassName(className);
        se.setExtendedClass(optionalExtendedType);
        se.setParentClassLoader(optionalParentClassLoader);
        return se.createFastEvaluator(scanner, interfaceToImplement, parameterNames);
    }

    /**
     * Use {@link #createFastEvaluator(Scanner,Class,String[])} instead:
     * <pre>
     * {@link ScriptEvaluator} se = new {@link ScriptEvaluator#ScriptEvaluator() ScriptEvaluator}();
     * se.{@link #setDefaultImports(String[]) setDefaultImports}.(optionalDefaultImports);
     * se.{@link #setClassName(String) setClassName}.(className);
     * se.{@link #setExtendedClass(Class) setExtendedClass}.(optionalExtendedClass);
     * se.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(optionalParentClassLoader);
     * return se.{@link #createFastEvaluator(Scanner, Class, String[]) createFastEvaluator}(scanner,
     * interfaceToImplement, parameterNames);
     * </pre>
     *
     * @deprecated
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        String[]    optionalDefaultImports,
        String      className,
        Class       optionalExtendedClass,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
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
    public final Object createInstance(Reader reader) {
        throw new UnsupportedOperationException("createInstance");
    }

    public Object createFastEvaluator(
        Reader   reader,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, IOException {
        return this.createFastEvaluator(new Scanner(null, reader), interfaceToImplement, parameterNames);
    }

    public Object createFastEvaluator(
        String   script,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException {
        try {
            return this.createFastEvaluator(
                new StringReader(script),
                interfaceToImplement,
                parameterNames
            );
        } catch (IOException ex) {
            throw new JaninoRuntimeException("IOException despite StringReader");
        }
    }

    /**
     * Notice: This method is not declared in {@link IScriptEvaluator}, and is hence only available in <i>this</i>
     * implementation of <code>org.codehaus.commons.compiler</code>. To be independent from this particular
     * implementation, try to switch to {@link #createFastEvaluator(Reader, Class, String[])}.
     *
     * @param scanner Source of tokens to read
     * @see #createFastEvaluator(Reader, Class, String[])
     */
    public Object createFastEvaluator(
        Scanner  scanner,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, IOException {
        if (!interfaceToImplement.isInterface()) {
            throw new JaninoRuntimeException("\"" + interfaceToImplement + "\" is not an interface");
        }

        Method[] methods = interfaceToImplement.getDeclaredMethods();
        if (methods.length != 1) {
            throw new JaninoRuntimeException(
                "Interface \""
                + interfaceToImplement
                + "\" must declare exactly one method"
            );
        }
        Method methodToImplement = methods[0];

        this.setImplementedInterfaces(new Class[] { interfaceToImplement });
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
        Class c = this.getMethod().getDeclaringClass();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new JaninoRuntimeException(e.toString());
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new JaninoRuntimeException(e.toString());
        }
    }

    /**
     * Guess the names of the parameters used in the given expression. The strategy is to look
     * at all "ambiguous names" in the expression (e.g. in "a.b.c.d()", the ambiguous name
     * is "a.b.c"), and then at the components of the ambiguous name.
     * <ul>
     *   <li>If any component starts with an upper-case letter, then ambiguous name is assumed to
     *       be a type name.
     *   <li>Otherwise, if the first component of the ambiguous name matches the name of a
     *       previously defined local variable, then the first component of the ambiguous name is
     *       assumed to be a local variable name. (Notice that this strategy does not consider that
     *       the scope of a local variable declaration may end before the end of the script.)
     *   <li>Otherwise, the first component of the ambiguous name is assumed to be a parameter name.
     * </ul>
     *
     * @see Scanner#Scanner(String, Reader)
     */
    public static String[] guessParameterNames(Scanner scanner) throws CompileException, IOException {
        Parser parser = new Parser(scanner);

        // Eat optional leading import declarations.
        while (parser.peek("import")) parser.parseImportDeclaration();

        // Parse the script statements into a block.
        Java.Block block = new Java.Block(scanner.location());
        while (!parser.peekEOF()) block.addStatement(parser.parseBlockStatement());

        // Traverse the block for ambiguous names and guess which of them are parameter names.
        final Set localVariableNames = new HashSet();
        final Set parameterNames = new HashSet();
        new Traverser() {
            public void traverseLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) {
                for (int i = 0; i < lvds.variableDeclarators.length; ++i) {
                    localVariableNames.add(lvds.variableDeclarators[i].name);
                }
                super.traverseLocalVariableDeclarationStatement(lvds);
            }

            public void traverseAmbiguousName(AmbiguousName an) {

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
        }.traverseBlock(block);

        return (String[]) parameterNames.toArray(new String[parameterNames.size()]);
    }

    public Object evaluate(int idx, Object[] arguments) throws InvocationTargetException {
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        try {
            return this.result[idx].invoke(null, arguments);
        } catch (IllegalAccessException ex) {
            throw new JaninoRuntimeException(ex.toString());
        }
    }

    public Method getMethod(int idx) {
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.result[idx];
    }
}
