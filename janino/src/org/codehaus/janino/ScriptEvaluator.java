
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
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
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.codehaus.janino.util.Traverser;

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
 * To set up a {@link ScriptEvaluator} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create the {@link ScriptEvaluator} using {@link #ScriptEvaluator()}
 *   <li>
 *   Configure the {@link ScriptEvaluator} by calling any of the following methods:
 *   <ul>
 *      <li>{@link #setReturnType(Class)}
 *      <li>{@link #setParameters(String[], Class[])}
 *      <li>{@link #setThrownExceptions(Class[])}
 *      <li>{@link org.codehaus.janino.SimpleCompiler#setParentClassLoader(ClassLoader)}
 *      <li>{@link org.codehaus.janino.ClassBodyEvaluator#setDefaultImports(String[])}
 *   </ul>
 *   <li>
 *   Call any of the {@link org.codehaus.janino.Cookable#cook(Scanner)} methods to scan,
 *   parse, compile and load the script into the JVM.
 * </ol>
 * After the {@link ScriptEvaluator} object is created, the script can be executed as often with
 * different parameter values (see {@link #evaluate(Object[])}). This execution is very fast,
 * compared to the compilation.
 * <p>
 * Less common methods exist that allow for the specification of the name of the generated class,
 * the class it extends, the interfaces it implements, the name of the method that executes the
 * script, the exceptions that this method (i.e. the script) is allowed to throw, and the
 * {@link ClassLoader} that is used to define the generated class and to load classes referenced by
 * the script.
 * <p>
 * Alternatively, a number of "convenience constructors" exist that execute the steps described
 * above instantly. Their use is discouraged.
 * <p>
 * If you want to compile many scripts at the same time, you have the option to cook an
 * <i>array</i> of scripts in one {@link ScriptEvaluator} by using the following methods:
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
public class ScriptEvaluator extends ClassBodyEvaluator {

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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
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
     * @see Cookable#cook(Scanner)
     */
    public ScriptEvaluator(
        Scanner     scanner,
        Class       returnType,
        String[]    parameterNames,
        Class[]     parameterTypes,
        Class[]     thrownExceptions,
        ClassLoader optionalParentClassLoader // null = use current thread's context class loader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
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
     * @see ClassBodyEvaluator#setExtendedType(Class)
     * @see ClassBodyEvaluator#setImplementedTypes(Class[])
     * @see #setReturnType(Class)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
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
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
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
     * @see ClassBodyEvaluator#setExtendedType(Class)
     * @see ClassBodyEvaluator#setImplementedTypes(Class[])
     * @see #setStaticMethod(boolean)
     * @see #setReturnType(Class)
     * @see #setMethodName(String)
     * @see #setParameters(String[], Class[])
     * @see #setThrownExceptions(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
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
    ) throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        this.setClassName(className);
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
        this.setStaticMethod(staticMethod);
        this.setReturnType(returnType);
        this.setMethodName(methodName);
        this.setParameters(parameterNames, parameterTypes);
        this.setThrownExceptions(thrownExceptions);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public ScriptEvaluator() {}

    /**
     * Define whether the generated method should be STATIC or not. Defaults to <code>true</code>.
     */
    public void setStaticMethod(boolean staticMethod) {
        this.setStaticMethod(new boolean[] { staticMethod });
    }

    /**
     * Define the return type of the generated method. Defaults to <code>void.class</code>.
     */
    public void setReturnType(Class returnType) {
        this.setReturnTypes(new Class[] { returnType });
    }

    /**
     * Define the name of the generated method. Defaults to an unspecified name.
     */
    public void setMethodName(String methodName) {
        this.setMethodNames(new String[] { methodName });
    }

    /**
     * Define the names and types of the parameters of the generated method.
     */
    public void setParameters(String[] parameterNames, Class[] parameterTypes) {
        this.setParameters(new String[][] { parameterNames }, new Class[][] {parameterTypes });
    }

    /**
     * Define the exceptions that the generated method may throw.
     */
    public void setThrownExceptions(Class[] thrownExceptions) {
        this.setThrownExceptions(new Class[][] { thrownExceptions });
    }

    public final void cook(Scanner scanner)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(new Scanner[] { scanner });
    }

    /**
     * Calls the generated method with concrete parameter values.
     * <p>
     * Each parameter value must have the same type as specified through
     * the "parameterTypes" parameter of
     * {@link #setParameters(String[], Class[])}.
     * <p>
     * Parameters of primitive type must passed with their wrapper class
     * objects.
     * <p>
     * The object returned has the class as specified through
     * {@link #setReturnType(Class)}.
     * <p>
     * This method is thread-safe.
     *
     * @param parameterValues The concrete parameter values.
     */
    public Object evaluate(Object[] parameterValues) throws InvocationTargetException {
        return this.evaluate(0, parameterValues);
    }

    /**
     * Returns the loaded {@link java.lang.reflect.Method}.
     * <p>
     * This method must only be called after {@link #cook(Scanner)}.
     * <p>
     * This method must not be called for instances of derived classes.
     */
    public Method getMethod() {
        return this.getMethod(0);
    }

    /**
     * Define whether the methods implementing each script should be STATIC or not. By default
     * all scripts are compiled into STATIC methods.
     */
    public void setStaticMethod(boolean[] staticMethod) {
        assertNotCooked();
        this.optionalStaticMethod = (boolean[]) staticMethod.clone();
    }

    /**
     * Define the return types of the scripts. By default all scripts have VOID return type.
     */
    public void setReturnTypes(Class[] returnTypes) {
        assertNotCooked();
        this.optionalReturnTypes = (Class[]) returnTypes.clone();
    }

    /**
     * Define the names of the generated methods. By default the methods have distinct and
     * implementation-specific names.
     * <p>
     * If two scripts have the same name, then they must have different parameter types
     * (see {@link #setParameters(String[][], Class[][])}).
     */
    public void setMethodNames(String[] methodNames) {
        assertNotCooked();
        this.optionalMethodNames = (String[]) methodNames.clone();
    }

    /**
     * Define the names and types of the parameters of the generated methods.
     */
    public void setParameters(String[][] parameterNames, Class[][] parameterTypes) {
        assertNotCooked();
        this.optionalParameterNames = (String[][]) parameterNames.clone();
        this.optionalParameterTypes = (Class[][]) parameterTypes.clone();
    }

    /**
     * Define the exceptions that the generated methods may throw.
     */
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
     * @throws IllegalStateException if any of the preceeding <code>set...()</code> had an array size different from that of <code>scanners</code>
     */
    public final void cook(Scanner[] scanners)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        if (scanners == null) throw new NullPointerException();

        // The "dimension" of this ScriptEvaluator, i.e. how many scripts are cooked at the same
        // time.
        int count = scanners.length;

        // Check array sizes.
        if (this.optionalMethodNames      != null && this.optionalMethodNames.length      != count) throw new IllegalStateException("methodName");
        if (this.optionalParameterNames   != null && this.optionalParameterNames.length   != count) throw new IllegalStateException("parameterNames");
        if (this.optionalParameterTypes   != null && this.optionalParameterTypes.length   != count) throw new IllegalStateException("parameterTypes");
        if (this.optionalReturnTypes      != null && this.optionalReturnTypes.length      != count) throw new IllegalStateException("returnTypes");
        if (this.optionalStaticMethod     != null && this.optionalStaticMethod.length     != count) throw new IllegalStateException("staticMethod");
        if (this.optionalThrownExceptions != null && this.optionalThrownExceptions.length != count) throw new IllegalStateException("thrownExceptions");

        this.setUpClassLoaders();

        // Create compilation unit.
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(count == 1 ? scanners[0] : null);

        // Create class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(scanners[0].location(), compilationUnit);

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
            Scanner scanner = scanners[i];

            Java.Block block = this.makeBlock(i, scanner);

            // Determine the following script properties AFTER the call to "makeBlock()",
            // because "makeBlock()" may modify these script properties on-the-fly.
            boolean  staticMethod     = this.optionalStaticMethod     == null ? true : this.optionalStaticMethod[i];
            Class    returnType       = this.optionalReturnTypes      == null ? this.getDefaultReturnType() : this.optionalReturnTypes[i];
            String[] parameterNames   = this.optionalParameterNames   == null ? new String[0] : this.optionalParameterNames[i];
            Class[]  parameterTypes   = this.optionalParameterTypes   == null ? new Class[0] : this.optionalParameterTypes[i];
            Class[]  thrownExceptions = this.optionalThrownExceptions == null ? new Class[0] : this.optionalThrownExceptions[i];

            cd.addDeclaredMethod(this.makeMethodDeclaration(
                scanner.location(), // location
                staticMethod,       // staticMethod
                returnType,         // returnType
                methodNames[i],     // methodName
                parameterTypes,     // parameterTypes
                parameterNames,     // parameterNames
                thrownExceptions,   // thrownExceptions
                block               // optionalBody
            ));
        }

        // Compile and load the compilation unit.
        Class c = this.compileToClass(
            compilationUnit,                                    // compilationUnit
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
            this.className
        );
        
        // Find the script methods by name.
        this.result = new Method[count];
        if (count <= 10) {
            for (int i = 0; i < count; ++i) {
                try {
                    this.result[i] = c.getDeclaredMethod(methodNames[i], this.optionalParameterTypes == null ? new Class[0] : this.optionalParameterTypes[i]);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("SNO: Loaded class does not declare method \"" + methodNames[i] + "\"");
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
                Method m = (Method) dms.get(new MethodWrapper(methodNames[i], this.optionalParameterTypes == null ? new Class[0] : this.optionalParameterTypes[i]));
                if (m == null) throw new RuntimeException("SNO: Loaded class does not declare method \"" + methodNames[i] + "\"");
                this.result[i] = m;
            }
        }
    }

    public final void cook(Reader[] readers)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(new String[readers.length], readers);
    }

    /**
     * @param optionalFileNames Used when reporting errors and warnings.
     */
    public final void cook(String[] optionalFileNames, Reader[] readers)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        Scanner[] scanners = new Scanner[readers.length];
        for (int i = 0; i < readers.length; ++i) scanners[i] = new Scanner(optionalFileNames[i], readers[i]);
        this.cook(scanners);
    }

    /**
     * Cook tokens from {@link java.lang.String}s.
     */
    public final void cook(String[] strings)
    throws CompileException, Parser.ParseException, Scanner.ScanException {
        Reader[] readers = new Reader[strings.length];
        for (int i = 0; i < strings.length; ++i) readers[i] = new StringReader(strings[i]);
        try {
            this.cook(readers);
        } catch (IOException ex) {
            throw new RuntimeException("SNO: IOException despite StringReader");
        }
    }

    protected Class getDefaultReturnType() {
        return void.class;
    }

    /**
     * Fill the given <code>block</code> by parsing statements until EOF and adding
     * them to the block.
     */
    protected Java.Block makeBlock(int idx, Scanner scanner) throws ParseException, ScanException, IOException {
        Java.Block block = new Java.Block(scanner.location());
        Parser parser = new Parser(scanner);
        while (!scanner.peek().isEOF()) {
            block.addStatement(parser.parseBlockStatement());
        }

        return block;
    }

    protected void compileToMethods(
        Java.CompilationUnit compilationUnit,
        String[]             methodNames,
        Class[][]            parameterTypes
    ) throws CompileException {

        // Compile and load the compilation unit.
        Class c = this.compileToClass(
            compilationUnit,                                    // compilationUnit
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
            this.className
        );

        // Find the script method by name.
        this.result = new Method[methodNames.length];
        for (int i = 0; i < this.result.length; ++i) {
            try {
                this.result[i] = c.getMethod(methodNames[i], parameterTypes[i]);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("SNO: Loaded class does not declare method \"" + this.optionalMethodNames[i] + "\"");
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
        Location   location,
        boolean    staticMethod,
        Class      returnType,
        String     methodName,
        Class[]    parameterTypes,
        String[]   parameterNames,
        Class[]    thrownExceptions,
        Java.Block optionalBody
    ) {
        if (parameterNames.length != parameterTypes.length) throw new RuntimeException("Lengths of \"parameterNames\" (" + parameterNames.length + ") and \"parameterTypes\" (" + parameterTypes.length + ") do not match");

        Java.FunctionDeclarator.FormalParameter[] fps = new Java.FunctionDeclarator.FormalParameter[parameterNames.length];
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
            optionalBody                                     // optionalBody
        );
    }

    /**
     * Simplified version of
     * {@link #createFastScriptEvaluator(Scanner, Class, String[], ClassLoader)}.
     * 
     * @param script Contains the sequence of script tokens
     * @param interfaceToImplement Must declare exactly the one method that defines the expression's signature
     * @param parameterNames The expression references the parameters through these names
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     */
    public static Object createFastScriptEvaluator(
        String   script,
        Class    interfaceToImplement,
        String[] parameterNames
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        ScriptEvaluator se = new ScriptEvaluator();
        return ScriptEvaluator.createFastEvaluator(se, script, parameterNames, interfaceToImplement);
    }

    /**
     * If the parameter and return types of the expression are known at compile time,
     * then a "fast" script evaluator can be instantiated through this method.
     * <p>
     * Script evaluation is faster than through {@link #evaluate(Object[])}, because
     * it is not done through reflection but through direct method invocation.
     * <p>
     * Example:
     * <pre>
     * public interface Foo {
     *     int bar(int a, int b);
     * }
     * ...
     * Foo f = (Foo) ScriptEvaluator.createFastScriptEvaluator(
     *     new Scanner(null, new StringReader("return a + b;")),
     *     Foo.class,
     *     new String[] { "a", "b" },
     *     (ClassLoader) null          // Use current thread's context class loader
     * );
     * System.out.println("1 + 2 = " + f.bar(1, 2));
     * </pre>
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the root package (i.e. "no" package).
     * 
     * @param scanner Source of script tokens
     * @param interfaceToImplement Must declare exactly one method
     * @param parameterNames
     * @param optionalParentClassLoader
     * @return an object that implements the given interface
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(se, scanner, parameterNames, interfaceToImplement);
    }

    /**
     * Like {@link #createFastScriptEvaluator(Scanner, Class, String[], ClassLoader)},
     * but gives you more control over the generated class (rarely needed in practice).
     * <p> 
     * Notice: The <code>interfaceToImplement</code> must either be declared <code>public</code>,
     * or with package scope in the same package as <code>className</code>.
     * 
     * @param scanner                   Source of script tokens
     * @param className                 Name of generated class
     * @param optionalExtendedType      Class to extend
     * @param interfaceToImplement      Must declare exactly the one method that defines the expression's signature
     * @param parameterNames            The expression references the parameters through these names
     * @param optionalParentClassLoader Used to load referenced classes, defaults to the current thread's "context class loader"
     * @return an object that implements the given interface and extends the <code>optionalExtendedType</code>
     */
    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setClassName(className);
        se.setExtendedType(optionalExtendedType);
        se.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(se, scanner, parameterNames, interfaceToImplement);
    }

    public static Object createFastScriptEvaluator(
        Scanner     scanner,
        String[]    optionalDefaultImports,
        String      className,
        Class       optionalExtendedType,
        Class       interfaceToImplement,
        String[]    parameterNames,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.setClassName(className);
        se.setExtendedType(optionalExtendedType);
        se.setDefaultImports(optionalDefaultImports);
        se.setParentClassLoader(optionalParentClassLoader);
        return ScriptEvaluator.createFastEvaluator(se, scanner, parameterNames, interfaceToImplement);
    }

    public static Object createFastEvaluator(
        ScriptEvaluator se,
        String          s,
        String[]        parameterNames,
        Class           interfaceToImplement
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        try {
            return ScriptEvaluator.createFastEvaluator(
                se,
                new Scanner(null, new StringReader(s)),
                parameterNames,
                interfaceToImplement
            );
        } catch (IOException ex) {
            throw new RuntimeException("IOException despite StringReader");
        }
    }

    /**
     * Create and return an object that implements the exactly one method of the given
     * <code>interfaceToImplement</code>.
     *
     * @param se A pre-configured {@link ScriptEvaluator} object
     * @param scanner Source of tokens to read
     * @param parameterNames The names of the parameters of the one abstract method of <code>interfaceToImplement</code>
     * @param interfaceToImplement A type with exactly one abstract method
     * @return an instance of the created {@link Object}
     */
    public static Object createFastEvaluator(
        ScriptEvaluator se,
        Scanner         scanner,
        String[]        parameterNames,
        Class           interfaceToImplement
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        if (!interfaceToImplement.isInterface()) throw new RuntimeException("\"" + interfaceToImplement + "\" is not an interface");

        Method[] methods = interfaceToImplement.getDeclaredMethods();
        if (methods.length != 1) throw new RuntimeException("Interface \"" + interfaceToImplement + "\" must declare exactly one method");
        Method methodToImplement = methods[0];

        se.setImplementedTypes(new Class[] { interfaceToImplement });
        se.setStaticMethod(false);
        se.setReturnType(methodToImplement.getReturnType());
        se.setMethodName(methodToImplement.getName());
        se.setParameters(parameterNames, methodToImplement.getParameterTypes());
        se.setThrownExceptions(methodToImplement.getExceptionTypes());
        se.cook(scanner);
        Class c = se.getMethod().getDeclaringClass();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            // SNO - Declared class is always non-abstract.
            throw new RuntimeException(e.toString());
        } catch (IllegalAccessException e) {
            // SNO - interface methods are always PUBLIC.
            throw new RuntimeException(e.toString());
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
    public static String[] guessParameterNames(Scanner scanner) throws ParseException, ScanException, IOException {
        Parser parser = new Parser(scanner);

        // Eat optional leading import declarations.
        while (scanner.peek().isKeyword("import")) parser.parseImportDeclaration();

        // Parse the script statements into a block.
        Java.Block block = new Java.Block(scanner.location());
        while (!scanner.peek().isEOF()) block.addStatement(parser.parseBlockStatement());

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

    /**
     * Calls the generated method with concrete parameter values.
     * <p>
     * Each parameter value must have the same type as specified through
     * the "parameterTypes" parameter of
     * {@link #setParameters(String[], Class[])}.
     * <p>
     * Parameters of primitive type must passed with their wrapper class
     * objects.
     * <p>
     * The object returned has the class as specified through
     * {@link #setReturnType(Class)}.
     * <p>
     * This method is thread-safe.
     *
     * @param idx The index of the script (0 ... <code>scripts.length - 1</code>)
     * @param parameterValues The concrete parameter values.
     */
    public Object evaluate(int idx, Object[] parameterValues) throws InvocationTargetException {
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        try {
            return this.result[idx].invoke(null, parameterValues);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Returns the loaded {@link java.lang.reflect.Method}.
     * <p>
     * This method must only be called after {@link #cook(Scanner)}.
     * <p>
     * This method must not be called for instances of derived classes.
     *
     * @param idx The index of the script (0 ... <code>scripts.length - 1</code>)
     */
    public Method getMethod(int idx) {
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.result[idx];
    }
}
