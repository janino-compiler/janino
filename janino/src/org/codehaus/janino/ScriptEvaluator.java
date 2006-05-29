
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

/**
 * A script evaluator that executes a script in Java<sup>TM</sup> bytecode.
 * <p>
 * The syntax of the script to compile is a sequence of import declarations followed by a
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
 * Alternatively, a number of "convenience constructors" exist that execute the steps described
 * above instantly.
 * <p>
 * After the {@link ScriptEvaluator} object is created, the script can be executed as often with
 * different parameter values (see {@link #evaluate(Object[])}). This execution is very fast,
 * compared to the compilation.
 * <p>
 * The more elaborate constructors of {@link ScriptEvaluator} also allow for the specification
 * of the name of the generated class, the class it extends, the interfaces it implements, the
 * name of the method that executes the script, the exceptions that this method is allowed
 * to throw, and the {@link ClassLoader} that is used to define the generated
 * class and to load classes referenced by the expression. This degree of flexibility is usually
 * not required; the most commonly used constructor is
 * {@link #ScriptEvaluator(String, Class, String[], Class[])}.
 */
public class ScriptEvaluator extends ClassBodyEvaluator {
    public static final String   DEFAULT_METHOD_NAME = "eval";
    public static final String[] ZERO_STRINGS = new String[0];

    private boolean              staticMethod = true;
    private Class                returnType = void.class;
    private String               methodName = ScriptEvaluator.DEFAULT_METHOD_NAME;
    private String[]             parameterNames = ScriptEvaluator.ZERO_STRINGS;
    private Class[]              parameterTypes = ClassBodyEvaluator.ZERO_CLASSES;
    private Class[]              thrownExceptions = ClassBodyEvaluator.ZERO_CLASSES;

    private Method               method = null; // null=uncooked

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
        this.staticMethod = staticMethod;
    }

    /**
     * Define the return type of the generated method. Defaults to <code>void.class</code>.
     */
    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }

    /**
     * Define the name of the generated method. Defaults to {@link #DEFAULT_METHOD_NAME}.
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Define the names and types of the parameters of the generated method.
     */
    public void setParameters(String[] parameterNames, Class[] parameterTypes) {
        if (parameterNames.length != parameterTypes.length) throw new IllegalArgumentException("Parameter names and types counts do not match");
        this.parameterNames = parameterNames;
        this.parameterTypes = parameterTypes;
    }

    /**
     * Define the exceptions that the generated method may throw.
     */
    public void setThrownExceptions(Class[] thrownExceptions) {
        if (thrownExceptions == null) throw new NullPointerException("Zero thrown exceptions must be specified as \"new Class[0]\", not \"null\"");
        this.thrownExceptions = thrownExceptions;
    }

    protected void internalCook(Scanner scanner) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(scanner);

        // Create class, method and block.
        Java.Block block = this.addClassMethodBlockDeclaration(
            scanner.location(), // location
            compilationUnit,    // compilationUnit
            this.returnType     // returnType
        );
        
        // Parse block statements.
        Parser parser = new Parser(scanner);
        while (!scanner.peek().isEOF()) {
            block.addStatement(parser.parseBlockStatement());
        }

        this.compileToMethod(compilationUnit);
    }

    protected void compileToMethod(Java.CompilationUnit compilationUnit) throws CompileException {

        // Compile and load the compilation unit.
        Class c = this.compileToClass(
            compilationUnit,                                    // compilationUnit
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION, // debuggingInformation
            this.className                                      // className
        );

        // Find the script method by name.
        try {
            this.method = c.getMethod(this.methodName, this.parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("SNO: Loaded class does not declare method \"" + this.methodName + "\"");
        }
    }

    /**
     * To the given {@link Java.CompilationUnit}, add
     * <ul>
     *   <li>A package member class declaration with the given name, superclass and interfaces
     *   <li>A public method declaration with the given return type, name, parameter
     *       names and values and thrown exceptions
     *   <li>A block 
     * </ul> 
     * @param location
     * @param compilationUnit
     * @param className
     * @param optionalExtendedType (null == {@link Object})
     * @param implementedTypes
     * @param staticMethod Whether the method should be declared "static"
     * @param returnType Return type of the declared method
     * @param methodName
     * @param parameterNames
     * @param parameterTypes
     * @param thrownExceptions
     * @return The created {@link Java.Block} object
     * @throws Parser.ParseException
     */
    protected Java.Block addClassMethodBlockDeclaration(
        Location             location,
        Java.CompilationUnit compilationUnit,
        Class                returnType
    ) throws Parser.ParseException {
        if (this.parameterNames.length != this.parameterTypes.length) throw new RuntimeException("Lengths of \"parameterNames\" and \"parameterTypes\" do not match");

        // Add class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(
            location,
            compilationUnit
        );

        // Add method declaration.
        Java.Block b = new Java.Block(location);
        Java.FunctionDeclarator.FormalParameter[] fps = new Java.FunctionDeclarator.FormalParameter[this.parameterNames.length];
        for (int i = 0; i < fps.length; ++i) {
            fps[i] = new Java.FunctionDeclarator.FormalParameter(
                location,                                           // location
                true,                                               // finaL
                this.classToType(location, this.parameterTypes[i]), // type
                this.parameterNames[i]                              // name
            );
        }
        Java.MethodDeclarator md = new Java.MethodDeclarator(
            location,                                             // location
            null,                                                 // optionalDocComment
            (                                                     // modifiers
                this.staticMethod ?
                (short) (Mod.PUBLIC | Mod.STATIC) :
                (short) Mod.PUBLIC
            ),
            this.classToType(location, returnType),               // type
            this.methodName,                                      // name
            fps,                                                  // formalParameters
            this.classesToTypes(location, this.thrownExceptions), // thrownExceptions
            b                                                     // optionalBody
        );
        cd.addDeclaredMethod(md);

        return b;
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
        if (this.method == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        try {
            return this.method.invoke(null, parameterValues);
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
     */
    public Method getMethod() {
        if (this.method == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.method;
    }
}
