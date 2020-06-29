
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

package org.codehaus.janino;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.CompilationUnit;

public
class ClassBodyEvaluator extends Cookable implements IClassBodyEvaluator {

    private static final Class<?>[] ZERO_CLASSES = new Class[0];

    @Nullable private WarningHandler warningHandler;
    private final SimpleCompiler     sc = new SimpleCompiler();

    private String[]           defaultImports = new String[0];
    private int                sourceVersion  = -1;
    private String             className      = IClassBodyEvaluator.DEFAULT_CLASS_NAME;
    @Nullable private Class<?> extendedType;
    private Class<?>[]         implementedTypes = ClassBodyEvaluator.ZERO_CLASSES;
    @Nullable private Class<?> result; // null=uncooked


    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.cook(classBody);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String)
     */
    public
    ClassBodyEvaluator(String classBody) throws CompileException { this.cook(classBody); }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.cook(fileName, is);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, InputStream)
     */
    public
    ClassBodyEvaluator(@Nullable String fileName, InputStream is) throws CompileException, IOException {
        this.cook(fileName, is);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.cook(fileName, reader);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, Reader)
     */
    public
    ClassBodyEvaluator(@Nullable String fileName, Reader reader) throws CompileException, IOException {
        this.cook(fileName, reader);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setParentClassLoader(parentClassLoader);
     *     cbe.cook(scanner);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ClassBodyEvaluator(Scanner scanner, @Nullable ClassLoader parentClassLoader)
    throws CompileException, IOException {
        this.setParentClassLoader(parentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setExtendedType(extendedType);
     *     cbe.setImplementedTypes(implementedTypes);
     *     cbe.setParentClassLoader(parentClassLoader);
     *     cbe.cook(scanner);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see #setExtendedClass(Class)
     * @see #setImplementedInterfaces(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ClassBodyEvaluator(
        Scanner               scanner,
        @Nullable Class<?>    extendedType,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        this.setExtendedClass(extendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(parentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setClassName(className);
     *     cbe.setExtendedType(extendedType);
     *     cbe.setImplementedTypes(implementedTypes);
     *     cbe.setParentClassLoader(parentClassLoader);
     *     cbe.cook(scanner);</pre>
     * </p>
     *
     * @see #ClassBodyEvaluator()
     * @see #setClassName(String)
     * @see #setExtendedClass(Class)
     * @see #setImplementedInterfaces(Class[])
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ClassBodyEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    extendedType,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        this.setClassName(className);
        this.setExtendedClass(extendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(parentClassLoader);
        this.cook(scanner);
    }

    public ClassBodyEvaluator() {}

    // ====================== CONFIGURATION SETTERS AND GETTERS ======================

    @Override public void
    setDefaultImports(String... defaultImports) { this.defaultImports = (String[]) defaultImports.clone(); }

    @Override public String[]
    getDefaultImports() { return (String[]) this.defaultImports.clone(); }

    @Override public void
    setClassName(String className) { this.className = className; }

    @Override public void
    setExtendedClass(@Nullable Class<?> extendedType) { this.extendedType = extendedType; }

    @Override public void
    setExtendedType(@Nullable Class<?> extendedClass) { this.setExtendedClass(extendedClass); }

    @Override public void
    setImplementedInterfaces(Class<?>[] implementedTypes) { this.implementedTypes = implementedTypes; }

    @Override public void
    setImplementedTypes(Class<?>[] implementedInterfaces) { this.setImplementedInterfaces(implementedInterfaces); }

    // Configuration setters and getters that delegate to the SimpleCompiler

    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) { this.sc.setParentClassLoader(parentClassLoader); }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.sc.setDebuggingInformation(debugSource, debugLines, debugVars);
    }

    @Override public void
    setSourceVersion(int version) {
        this.sc.setSourceVersion(version);
        this.sourceVersion = version;
    }

    @Override public void
    setTargetVersion(int version) { this.sc.setTargetVersion(version); }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.sc.setCompileErrorHandler(compileErrorHandler);
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.sc.setWarningHandler(warningHandler);
        this.warningHandler = warningHandler;
    }

    // JANINO-specific configuration setters and getters

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.sc.options(); }

    /**
     * Sets the options for all future compilations.
     */
    public ClassBodyEvaluator
    options(EnumSet<JaninoOption> options) {
        this.sc.options(options);
        return this;
    }

    // ================================= END OF CONFIGURATION SETTERS AND GETTERS =================================

    @Override public final void
    cook(@Nullable String fileName, Reader r) throws CompileException, IOException {
        this.cook(new Scanner(fileName, r));
    }

    public void
    cook(Scanner scanner) throws CompileException, IOException {

        Parser parser = new Parser(scanner);
        parser.setSourceVersion(this.sourceVersion);

        Java.AbstractCompilationUnit.ImportDeclaration[] importDeclarations = this.makeImportDeclarations(parser);

        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(scanner.getFileName(), importDeclarations);

        // Add class declaration.
        Java.AbstractClassDeclaration
        acd = this.addPackageMemberClassDeclaration(scanner.location(), compilationUnit);

        // Parse class body declarations (member declarations) until EOF.
        while (!parser.peek(TokenType.END_OF_INPUT)) parser.parseClassBodyDeclaration(acd);

        // Compile and load it.
        this.cook(compilationUnit);
    }

    void
    cook(CompilationUnit compilationUnit) throws CompileException {

        this.sc.cook(compilationUnit);

        // Find the generated class by name.
        Class<?> c;
        try {
            c = this.sc.getClassLoader().loadClass(this.className);
        } catch (ClassNotFoundException ex) {
            throw new InternalCompilerException((
                "SNO: Generated compilation unit does not declare class '"
                + this.className
                + "'"
            ), ex);
        }

        this.result = c;
    }

    @Override public Class<?>
    getClazz() { return this.assertCooked(); }

    @Override public Map<String, byte[]>
    getBytecodes() { return this.sc.getBytecodes(); }

    /**
     * @return                                  The {@link #setDefaultImports(String...) default imports}, concatenated
     *                                          with the import declarations that can be parsed from the
     *                                          <var>parser</var>
     * @see Parser#parseImportDeclarationBody()
     */
    final Java.AbstractCompilationUnit.ImportDeclaration[]
    makeImportDeclarations(@Nullable Parser parser) throws CompileException, IOException {

        List<Java.AbstractCompilationUnit.ImportDeclaration>
        l = new ArrayList<Java.AbstractCompilationUnit.ImportDeclaration>();

        // Honor the default imports.
        for (String defaultImport : this.defaultImports) {

            final Parser p = new Parser(new Scanner(null, new StringReader(defaultImport)));
            p.setSourceVersion(this.sourceVersion);
            p.setWarningHandler(this.warningHandler);

            l.add(p.parseImportDeclarationBody());
            p.read(TokenType.END_OF_INPUT);
        }

        // Parse all available IMPORT declarations.
        if (parser != null) {
            while (parser.peek("import")) l.add(parser.parseImportDeclaration());
        }

        return (Java.AbstractCompilationUnit.ImportDeclaration[]) l.toArray(
            new AbstractCompilationUnit.ImportDeclaration[l.size()]
        );
    }

    /**
     * To the given {@link Java.CompilationUnit}, add
     * <ul>
     *   <li>A class declaration with the configured name, superclass and interfaces
     *   <li>A method declaration with the given return type, name, parameter names and values and thrown exceptions
     * </ul>
     *
     * @return The created {@link Java.AbstractClassDeclaration} object
     */
    protected Java.PackageMemberClassDeclaration
    addPackageMemberClassDeclaration(Location location, Java.CompilationUnit compilationUnit) {
        String cn  = this.className;
        int    idx = cn.lastIndexOf('.');
        if (idx != -1) {
            compilationUnit.setPackageDeclaration(new Java.PackageDeclaration(location, cn.substring(0, idx)));
            cn = cn.substring(idx + 1);
        }
        Java.PackageMemberClassDeclaration tlcd = new Java.PackageMemberClassDeclaration(
            location,                                                            // location
            null,                                                                // docComment
            new Java.Modifier[] { new Java.AccessModifier("public", location) }, // modifiers
            cn,                                                                  // name
            null,                                                                // typeParameters
            this.optionalClassToType(location, this.extendedType),               // extendedType
            this.sc.classesToTypes(location, this.implementedTypes)              // implementedTypes
        );
        compilationUnit.addPackageMemberTypeDeclaration(tlcd);
        return tlcd;
    }

    /**
     * @see SimpleCompiler#optionalClassToType(Location, Class)
     */
    @Nullable protected Java.Type
    optionalClassToType(final Location location, @Nullable final Class<?> clazz) {
        return this.sc.optionalClassToType(location, clazz);
    }

    protected Java.Type
    classToType(final Location location, final Class<?> clazz) { return this.sc.classToType(location, clazz); }

    public Java.Type[]
    classesToTypes(Location location, Class<?>[] classes) { return this.sc.classesToTypes(location, classes); }

//    /**
//     * Compiles the given compilation unit, load all generated classes, and return the class with the given name.
//     *
//     * @param compilationUnit
//     * @return The loaded class
//     */
//    protected final Class<?>
//    compileToClass(Java.CompilationUnit compilationUnit) throws CompileException {
//
//        // Compile and load the compilation unit.
//        ClassLoader cl = this.compileToClassLoader(compilationUnit);
//
//        // Find the generated class by name.
//        try {
//            return cl.loadClass(this.className);
//        } catch (ClassNotFoundException ex) {
//            throw new InternalCompilerException((
//                "SNO: Generated compilation unit does not declare class '"
//                + this.className
//                + "'"
//            ), ex);
//        }
//    }

    private Class<?>
    assertCooked() {

        if (this.result != null) return this.result;

        throw new IllegalStateException("Must only be called after 'cook()'");
    }

    @Override public Object
    createInstance(Reader reader) throws CompileException, IOException {
        this.cook(reader);

        try {
            return this.getClazz().newInstance();
        } catch (InstantiationException ie) {
            CompileException ce = new CompileException((
                "Class is abstract, an interface, an array class, a primitive type, or void; "
                + "or has no zero-parameter constructor"
            ), null);
            ce.initCause(ie);
            throw ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        } catch (IllegalAccessException iae) {
            CompileException ce = new CompileException(
                "The class or its zero-parameter constructor is not accessible",
                null
            );
            ce.initCause(iae);
            throw ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    /**
     * Use {@link #createInstance(Reader)} instead:
     * <pre>
     *     IClassBodyEvaluator cbe = {@link CompilerFactoryFactory}.{@link
     *     CompilerFactoryFactory#getDefaultCompilerFactory() getDefaultCompilerFactory}().{@link
     *     ICompilerFactory#newClassBodyEvaluator() newClassBodyEvaluator}();
     *     if (baseType != null) {
     *         if (baseType.isInterface()) {
     *             cbe.{@link #setImplementedInterfaces setImplementedInterfaces}(new Class[] { baseType });
     *         } else {
     *             cbe.{@link #setExtendedClass(Class) setExtendedClass}(baseType);
     *         }
     *     }
     *     cbe.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(parentClassLoader);
     *     cbe.{@link IClassBodyEvaluator#createInstance(Reader) createInstance}(reader);
     * </pre>
     *
     * @see #createInstance(Reader)
     */
    public static Object
    createFastClassBodyEvaluator(
        Scanner               scanner,
        @Nullable Class<?>    baseType,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        return ClassBodyEvaluator.createFastClassBodyEvaluator(
            scanner,                                // scanner
            IClassBodyEvaluator.DEFAULT_CLASS_NAME, // className
            (                                       // extendedType
                baseType != null && !baseType.isInterface()
                ? baseType
                : null
            ),
            (                                       // implementedTypes
                baseType != null && baseType.isInterface()
                ? new Class[] { baseType }
                : new Class[0]
            ),
            parentClassLoader                       // parentClassLoader
        );
    }

    /**
     * Use {@link #createInstance(Reader)} instead:
     * <pre>
     *     IClassBodyEvaluator cbe = {@link CompilerFactoryFactory}.{@link
     *     CompilerFactoryFactory#getDefaultCompilerFactory() getDefaultCompilerFactory}().{@link
     *     ICompilerFactory#newClassBodyEvaluator() newClassBodyEvaluator}();
     *     cbe.{@link #setExtendedClass(Class) setExtendedClass}(extendedClass);
     *     cbe.{@link #setImplementedInterfaces(Class[]) setImplementedInterfaces}(implementedInterfaces);
     *     cbe.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(parentClassLoader);
     *     cbe.{@link IClassBodyEvaluator#createInstance(Reader) createInstance}(reader);
     * </pre>
     *
     * @see        #createInstance(Reader)
     * @deprecated Use {@link #createInstance(Reader)} instead
     */
    @Deprecated public static Object
    createFastClassBodyEvaluator(
        Scanner               scanner,
        String                className,
        @Nullable Class<?>    extendedClass,
        Class<?>[]            implementedInterfaces,
        @Nullable ClassLoader parentClassLoader
    ) throws CompileException, IOException {
        ClassBodyEvaluator cbe = new ClassBodyEvaluator();
        cbe.setClassName(className);
        cbe.setExtendedClass(extendedClass);
        cbe.setImplementedInterfaces(implementedInterfaces);
        cbe.setParentClassLoader(parentClassLoader);
        cbe.cook(scanner);
        Class<?> c = cbe.getClazz();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            throw new CompileException( // SUPPRESS CHECKSTYLE AvoidHidingCause
                e.getMessage(),
                null
            );
        } catch (IllegalAccessException e) {
            // SNO - type and default constructor of generated class are PUBLIC.
            throw new InternalCompilerException(e.toString(), e);
        }
    }
}
