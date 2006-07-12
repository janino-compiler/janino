
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

import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.codehaus.janino.util.enumerator.EnumeratorSet;

/**
 * Parses a class body and returns it as a <tt>java.lang.Class</tt> object
 * ready for use with <tt>java.lang.reflect</tt>.
 * <p>
 * Example:
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
 * The <code>optionalClassLoader</code> serves two purposes:
 * <ul>
 *   <li>It is used to look for classes referenced by the class body.
 *   <li>It is used to load the generated Java<sup>TM</sup> class
 *   into the JVM; directly if it is a subclass of {@link
 *   ByteArrayClassLoader}, or by creation of a temporary
 *   {@link ByteArrayClassLoader} if not.
 * </ul>
 * To set up a {@link ClassBodyEvaluator} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create the {@link ClassBodyEvaluator} using {@link #ClassBodyEvaluator()}
 *   <li>
 *   Configure the {@link ClassBodyEvaluator} by calling any of the following methods:
 *   <ul>
 *      <li>{@link org.codehaus.janino.SimpleCompiler#setParentClassLoader(ClassLoader)}
 *      <li>{@link #setDefaultImports(String[])}
 *   </ul>
 *   <li>
 *   Call any of the {@link org.codehaus.janino.Cookable#cook(Scanner)} methods to scan,
 *   parse, compile and load the class body into the JVM.
 * </ol>
 * Alternatively, a number of "convenience constructors" exist that execute the steps described
 * above instantly.
 * <p>
 * To compile a class body and immediately instantiate an object, one of the
 * {@link #createFastClassBodyEvaluator(Scanner, Class, ClassLoader)} methods can be used.
 * <p>
 * The generated class may optionally extend/implement a given type; the returned instance can
 * safely be type-casted to that <code>optionalBaseType</code>.
 * <p>
 * Example:
 * <pre>
 * public interface Foo {
 *     int bar(int a, int b);
 * }
 * ...
 * Foo f = (Foo) ClassBodyEvaluator.createFastClassBodyEvaluator(
 *     new Scanner(null, new StringReader("public int bar(int a, int b) { return a + b; }")),
 *     Foo.class,                  // Base type to extend/implement
 *     (ClassLoader) null          // Use current thread's context class loader
 * );
 * System.out.println("1 + 2 = " + f.bar(1, 2));
 * </pre>
 * Notice: The <code>optionalBaseType</code> must be accessible from the generated class,
 * i.e. it must either be declared <code>public</code>, or with default accessibility in the
 * same package as the generated class.
 */
public class ClassBodyEvaluator extends SimpleCompiler {
    public static final String     DEFAULT_CLASS_NAME = "SC";
    protected static final Class[] ZERO_CLASSES = new Class[0];

    private String[]               optionalDefaultImports = null;
    protected String               className = ClassBodyEvaluator.DEFAULT_CLASS_NAME;
    private Class                  optionalExtendedType = null;
    private Class[]                implementedTypes = ClassBodyEvaluator.ZERO_CLASSES;
    private Class                  clazz = null; // null=uncooked

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.cook(classBody);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String)
     */
    public ClassBodyEvaluator(
        String classBody
    ) throws CompileException, Parser.ParseException, Scanner.ScanException {
        this.cook(classBody);
    }

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.cook(optionalFileName, is);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, InputStream)
     */
    public ClassBodyEvaluator(
        String      optionalFileName,
        InputStream is
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(optionalFileName, is);
    }

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.cook(optionalFileName, reader);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, Reader)
     */
    public ClassBodyEvaluator(
        String   optionalFileName,
        Reader   reader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.cook(optionalFileName, reader);
    }

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.setParentClassLoader(optionalParentClassLoader);
     * cbe.cook(scanner);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.setExtendedType(optionalExtendedType);
     * cbe.setImplementedTypes(implementedTypes);
     * cbe.setParentClassLoader(optionalParentClassLoader);
     * cbe.cook(scanner);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see #setExtendedType(Class)
     * @see #setImplementedTypes(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to<pre>
     * ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     * cbe.setClassName(className);
     * cbe.setExtendedType(optionalExtendedType);
     * cbe.setImplementedTypes(implementedTypes);
     * cbe.setParentClassLoader(optionalParentClassLoader);
     * cbe.cook(scanner);</pre>
     *
     * @see #ClassBodyEvaluator()
     * @see #setClassName(String)
     * @see #setExtendedType(Class)
     * @see #setImplementedTypes(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.setClassName(className);
        this.setExtendedType(optionalExtendedType);
        this.setImplementedTypes(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public ClassBodyEvaluator() {}

    /**
     * "Default imports" add to the system import "java.lang", i.e. the evaluator may refer to
     * classes imported by default imports without having to explicitly declare IMPORT statements.
     * <p>
     * Example: <code>sc.setDefaultImports(new String[] { "java.util.Map", "java.io.*" });</code>
     */
    public void setDefaultImports(String[] optionalDefaultImports) {
        this.optionalDefaultImports = optionalDefaultImports;
    }

    /**
     * Set the name of the generated class. Defaults to {@link #DEFAULT_CLASS_NAME}. In most cases,
     * there is no need to set this name, because the generated class is loaded into its own
     * {@link java.lang.ClassLoader} where its name cannot collide with classes generated by
     * other evaluators.
     * <p>
     * One reason to use this function is to have a class name in a non-default package, which
     * can be relevant when types and members with DEFAULT accessibility are accessed.
     */
    public void setClassName(String className) { this.className = className; }

    /**
     * Set a particular superclass that the generated class will extend. If <code>null</code> is
     * passed, the generated class will extend {@link Object}.
     * <p>
     * The common reason to set a base class for an evaluator is that the generated class can
     * directly access the base superclass's (non-private) members.
     */
    public void setExtendedType(Class optionalExtendedType) {
        this.optionalExtendedType = optionalExtendedType;
    }

    /**
     * Set a particular set of interfaces that the generated class will implement.
     */
    public void setImplementedTypes(Class[] implementedTypes) {
        if (implementedTypes == null) throw new NullPointerException("Zero implemented types must be specified as \"new Class[0]\", not \"null\"");
        this.implementedTypes = implementedTypes;
    }

    protected void internalCook(Scanner scanner) throws CompileException, ParseException, ScanException, IOException {
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(scanner);

        // Add class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(
            scanner.location(),
            compilationUnit
        );
        
        // Parse class body declarations (member declarations) until EOF.
        Parser parser = new Parser(scanner);
        while (!scanner.peek().isEOF()) {
            parser.parseClassBodyDeclaration(cd);
        }

        // Compile and load it.
        this.clazz = this.compileToClass(
            compilationUnit,              // compilationUnit
            DebuggingInformation.ALL,     // debuggingInformation
            this.className                // className
        );
    }

    /**
     * Create a {@link Java.CompilationUnit}, set the default imports, and parse the import
     * declarations.
     */
    protected Java.CompilationUnit makeCompilationUnit(Scanner scanner)
    throws Parser.ParseException, Scanner.ScanException, IOException {
        Java.CompilationUnit cu = new Java.CompilationUnit(scanner.getFileName());
        
        // Set default imports.
        if (this.optionalDefaultImports != null) {
            for (int i = 0; i < this.optionalDefaultImports.length; ++i) {
                Scanner s = new Scanner(null, new StringReader(this.optionalDefaultImports[i]));
                cu.addImportDeclaration(new Parser(s).parseImportDeclarationBody());
                if (!s.peek().isEOF()) throw new ParseException("Unexpected token \"" + s.peek() + "\" in default import", s.location());
            }
        }

        // Parse all available IMPORT declarations.
        Parser parser = new Parser(scanner);
        while (scanner.peek().isKeyword("import")) {
            cu.addImportDeclaration(parser.parseImportDeclaration());
        }

        return cu;
    }

    /**
     * To the given {@link Java.CompilationUnit}, add
     * <ul>
     *   <li>A class declaration with the configured name, superclass and interfaces
     *   <li>A method declaration with the given return type, name, parameter
     *       names and values and thrown exceptions
     * </ul> 
     * @param location
     * @param compilationUnit
     * @param className
     * @param implementedTypes
     * @return The created {@link Java.ClassDeclaration} object
     * @throws Parser.ParseException
     */
    protected Java.PackageMemberClassDeclaration addPackageMemberClassDeclaration(
        Location             location,
        Java.CompilationUnit compilationUnit
    ) throws Parser.ParseException {
        String cn = this.className;
        int idx = cn.lastIndexOf('.');
        if (idx != -1) {
            compilationUnit.setPackageDeclaration(new Java.PackageDeclaration(location, cn.substring(0, idx)));
            cn = cn.substring(idx + 1);
        }
        Java.PackageMemberClassDeclaration tlcd = new Java.PackageMemberClassDeclaration(
            location,                                              // location
            null,                                                  // optionalDocComment
            Mod.PUBLIC,                                            // modifiers
            cn,                                                    // name
            this.classToType(location, this.optionalExtendedType), // optionalExtendedType
            this.classesToTypes(location, this.implementedTypes)   // implementedTypes
        );
        compilationUnit.addPackageMemberTypeDeclaration(tlcd);
        return tlcd;
    }

    /**
     * Compile the given compilation unit, load all generated classes, and
     * return the class with the given name. 
     * @param compilationUnit
     * @param debuggingInformation TODO
     * @param newClassName The fully qualified class name
     * @return The loaded class
     */
    protected Class compileToClass(
        Java.CompilationUnit compilationUnit,
        EnumeratorSet        debuggingInformation,
        String               newClassName
    ) throws CompileException {

        // Compile and load the compilation unit.
        ClassLoader cl = this.compileToClassLoader(compilationUnit, debuggingInformation);

        // Find the generated class by name.
        try {
            return cl.loadClass(newClassName);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("SNO: Generated compilation unit does not declare class \"" + newClassName + "\"");
        }
    }

    /**
     * Returns the loaded {@link Class}.
     * <p>
     * This method must only be called after {@link #cook(Scanner)}.
     * <p>
     * This method must not be called for instances of derived classes.
     */
    public Class getClazz() {
        if (this.getClass() != ClassBodyEvaluator.class) throw new IllegalStateException("Must not be called on derived instances");
        if (this.clazz == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.clazz;
    }

    /**
     * Scans, parses and compiles a class body from the tokens delivered by the the given
     * {@link Scanner}.
     * The generated class has the {@link #DEFAULT_CLASS_NAME} and extends the given
     * <code>optionalBaseType</code> (if that is a class), and implements the given
     * <code>optionalBaseType</code> (if that is an interface).
     * <p>
     * For an explanation of the "fast class body evaluator" concept, see the class description.
     *
     * @param scanner                   Source of class body tokens
     * @param optionalBaseType          Base type to extend/implement
     * @param optionalParentClassLoader Used to load referenced classes, defaults to the current thread's "context class loader"
     * @return an object that extends/implements the given <code>optionalBaseType</code>
     * @see ClassBodyEvaluator
     */
    public static Object createFastClassBodyEvaluator(
        Scanner     scanner,
        Class       optionalBaseType,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        return ClassBodyEvaluator.createFastClassBodyEvaluator(
            scanner,
            ClassBodyEvaluator.DEFAULT_CLASS_NAME, // className
            (                                      // optionalExtendedType
                optionalBaseType != null && !optionalBaseType.isInterface() ?
                optionalBaseType : null
            ),
            (                                      // implementedTypes
                optionalBaseType != null && optionalBaseType.isInterface() ?
                new Class[] { optionalBaseType } : new Class[0]
            ),
            optionalParentClassLoader
        );
    }

    /**
     * Scans, parses and compiles a class body from the tokens delivered by the the given
     * {@link Scanner} with no default imports.
     * <p>
     * For an explanation of the "fast class body evaluator" concept, see the class description.
     * 
     * @param scanner Source of class body tokens
     * @param className Name of generated class
     * @param optionalExtendedType Class to extend
     * @param implementedTypes Interfaces to implement
     * @param optionalParentClassLoader Used to load referenced classes, defaults to the current thread's "context class loader"
     * @return an object that extends the <code>optionalExtendedType</code> and implements the given <code>implementedTypes</code>
     * @see ClassBodyEvaluator
     */
    public static Object createFastClassBodyEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        Class c = new ClassBodyEvaluator(
            scanner,
            className,
            optionalExtendedType,
            implementedTypes,
            optionalParentClassLoader
        ).getClazz();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            throw new CompileException("Cannot instantiate abstract class -- one or more method implementations are missing", null);
        } catch (IllegalAccessException e) {
            // SNO - type and default constructor of generated class are PUBLIC.
            throw new RuntimeException(e.toString());
        }
    }
}
