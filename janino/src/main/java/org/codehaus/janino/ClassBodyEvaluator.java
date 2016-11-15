
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

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * The {@code optionalClassLoader} serves two purposes:
 * <ul>
 *   <li>It is used to look for classes referenced by the class body.
 *   <li>It is used to load the generated Java class
 *   into the JVM; directly if it is a subclass of {@link
 *   ByteArrayClassLoader}, or by creation of a temporary
 *   {@link ByteArrayClassLoader} if not.
 * </ul>
 * <p>
 *   A number of "convenience constructors" exist that execute the setup steps instantly.
 * </p>
 */
public
class ClassBodyEvaluator extends SimpleCompiler implements IClassBodyEvaluator {

    private static final Class<?>[] ZERO_CLASSES = new Class[0];

    @Nullable private String[] optionalDefaultImports;
    private String             className = IClassBodyEvaluator.DEFAULT_CLASS_NAME;
    @Nullable private Class<?> optionalExtendedType;
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
     *     cbe.cook(optionalFileName, is);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, InputStream)
     */
    public
    ClassBodyEvaluator(@Nullable String optionalFileName, InputStream is) throws CompileException, IOException {
        this.cook(optionalFileName, is);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.cook(optionalFileName, reader);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see Cookable#cook(String, Reader)
     */
    public
    ClassBodyEvaluator(@Nullable String optionalFileName, Reader reader) throws CompileException, IOException {
        this.cook(optionalFileName, reader);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setParentClassLoader(optionalParentClassLoader);
     *     cbe.cook(scanner);
     * </pre>
     *
     * @see #ClassBodyEvaluator()
     * @see SimpleCompiler#setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    ClassBodyEvaluator(Scanner scanner, @Nullable ClassLoader optionalParentClassLoader)
    throws CompileException, IOException {
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setExtendedType(optionalExtendedType);
     *     cbe.setImplementedTypes(implementedTypes);
     *     cbe.setParentClassLoader(optionalParentClassLoader);
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
        @Nullable Class<?>    optionalExtendedType,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    /**
     * Equivalent to
     * <pre>
     *     ClassBodyEvaluator cbe = new ClassBodyEvaluator();
     *     cbe.setClassName(className);
     *     cbe.setExtendedType(optionalExtendedType);
     *     cbe.setImplementedTypes(implementedTypes);
     *     cbe.setParentClassLoader(optionalParentClassLoader);
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
        @Nullable Class<?>    optionalExtendedType,
        Class<?>[]            implementedTypes,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        this.setClassName(className);
        this.setExtendedClass(optionalExtendedType);
        this.setImplementedInterfaces(implementedTypes);
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public ClassBodyEvaluator() {}

    @Override public void
    setDefaultImports(@Nullable String[] optionalDefaultImports) {
        this.optionalDefaultImports = optionalDefaultImports;
    }

    @Override public void
    setClassName(String className) {
        this.className = className;
    }

    @Override public void
    setExtendedClass(@Nullable Class<?> optionalExtendedType) { this.optionalExtendedType = optionalExtendedType; }

    /**
     * @deprecated Use {@link #setExtendedClass(Class)} instead
     */
    @Deprecated @Override public void
    setExtendedType(@Nullable Class<?> optionalExtendedClass) { this.setExtendedClass(optionalExtendedClass); }

    @Override public void
    setImplementedInterfaces(Class<?>[] implementedTypes) { this.implementedTypes = implementedTypes; }

    /**
     * @deprecated Use {@link #setImplementedInterfaces(Class[])} instead
     */
    @Deprecated @Override public void
    setImplementedTypes(Class<?>[] implementedInterfaces) { this.setImplementedInterfaces(implementedInterfaces); }

    @Override public void
    cook(Scanner scanner) throws CompileException, IOException {

        Parser               parser          = new Parser(scanner);
        Java.CompilationUnit compilationUnit = this.makeCompilationUnit(parser);

        // Add class declaration.
        Java.AbstractClassDeclaration cd = this.addPackageMemberClassDeclaration(scanner.location(), compilationUnit);

        // Parse class body declarations (member declarations) until EOF.
        while (!parser.peek(TokenType.END_OF_INPUT)) {
            parser.parseClassBodyDeclaration(cd);
        }

        // Compile and load it.
        this.result = this.compileToClass(compilationUnit);
    }

    /**
     * Creates a {@link Java.CompilationUnit}, sets the default imports, and parses the import declarations.
     * <p>
     *   If the {@code optionalParser} is given, a sequence of IMPORT directives is parsed from it and added to the
     *   compilation unit.
     * </p>
     */
    protected final Java.CompilationUnit
    makeCompilationUnit(@Nullable Parser optionalParser) throws CompileException, IOException {
        Java.CompilationUnit cu = (
            new Java.CompilationUnit(optionalParser == null
            ? null
            : optionalParser.getScanner().getFileName())
        );

        // Set default imports.
        if (this.optionalDefaultImports != null) {
            for (String defaultImport : this.optionalDefaultImports) {
                Scanner s       = new Scanner(null, new StringReader(defaultImport));
                Parser  parser2 = new Parser(s);
                cu.addImportDeclaration(parser2.parseImportDeclarationBody());
                parser2.read(TokenType.END_OF_INPUT);
            }
        }

        // Parse all available IMPORT declarations.
        if (optionalParser != null) {
            while (optionalParser.peek("import")) {
                cu.addImportDeclaration(optionalParser.parseImportDeclaration());
            }
        }

        return cu;
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
    addPackageMemberClassDeclaration(Location location, Java.CompilationUnit compilationUnit) throws CompileException {
        String cn  = this.className;
        int    idx = cn.lastIndexOf('.');
        if (idx != -1) {
            compilationUnit.setPackageDeclaration(new Java.PackageDeclaration(location, cn.substring(0, idx)));
            cn = cn.substring(idx + 1);
        }
        Java.PackageMemberClassDeclaration tlcd = new Java.PackageMemberClassDeclaration(
            location,                                                      // location
            null,                                                          // optionalDocComment
            new Java.Modifiers(Mod.PUBLIC),                                // modifiers
            cn,                                                            // name
            null,                                                          // optionalTypeParameters
            this.optionalClassToType(location, this.optionalExtendedType), // optionalExtendedType
            this.classesToTypes(location, this.implementedTypes)           // implementedTypes
        );
        compilationUnit.addPackageMemberTypeDeclaration(tlcd);
        return tlcd;
    }

    /**
     * Compiles the given compilation unit, load all generated classes, and return the class with the given name.
     *
     * @param compilationUnit
     * @return The loaded class
     */
    protected final Class<?>
    compileToClass(Java.CompilationUnit compilationUnit) throws CompileException {

        // Compile and load the compilation unit.
        ClassLoader cl = this.compileToClassLoader(compilationUnit);

        // Find the generated class by name.
        try {
            return cl.loadClass(this.className);
        } catch (ClassNotFoundException ex) {
            throw new JaninoRuntimeException((
                "SNO: Generated compilation unit does not declare class '"
                + this.className
                + "'"
            ), ex);
        }
    }

    @Override public Class<?>
    getClazz() {

        if (this.getClass() != ClassBodyEvaluator.class) {
            throw new IllegalStateException("Must not be called on derived instances");
        }

        return this.assertCooked();
    }

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
     *     if (optionalBaseType != null) {
     *         if (optionalBaseType.isInterface()) {
     *             cbe.{@link #setImplementedInterfaces setImplementedInterfaces}(new Class[] { optionalBaseType });
     *         } else {
     *             cbe.{@link #setExtendedClass(Class) setExtendedClass}(optionalBaseType);
     *         }
     *     }
     *     cbe.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(optionalParentClassLoader);
     *     cbe.{@link IClassBodyEvaluator#createInstance(Reader) createInstance}(reader);
     * </pre>
     *
     * @see #createInstance(Reader)
     */
    public static Object
    createFastClassBodyEvaluator(
        Scanner               scanner,
        @Nullable Class<?>    optionalBaseType,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        return ClassBodyEvaluator.createFastClassBodyEvaluator(
            scanner,                                // scanner
            IClassBodyEvaluator.DEFAULT_CLASS_NAME, // className
            (                                       // optionalExtendedType
                optionalBaseType != null && !optionalBaseType.isInterface()
                ? optionalBaseType
                : null
            ),
            (                                       // implementedTypes
                optionalBaseType != null && optionalBaseType.isInterface()
                ? new Class[] { optionalBaseType }
                : new Class[0]
            ),
            optionalParentClassLoader               // optionalParentClassLoader
        );
    }

    /**
     * Use {@link #createInstance(Reader)} instead:
     * <pre>
     *     IClassBodyEvaluator cbe = {@link CompilerFactoryFactory}.{@link
     *     CompilerFactoryFactory#getDefaultCompilerFactory() getDefaultCompilerFactory}().{@link
     *     ICompilerFactory#newClassBodyEvaluator() newClassBodyEvaluator}();
     *     cbe.{@link #setExtendedClass(Class) setExtendedClass}(optionalExtendedClass);
     *     cbe.{@link #setImplementedInterfaces(Class[]) setImplementedInterfaces}(implementedInterfaces);
     *     cbe.{@link #setParentClassLoader(ClassLoader) setParentClassLoader}(optionalParentClassLoader);
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
        @Nullable Class<?>    optionalExtendedClass,
        Class<?>[]            implementedInterfaces,
        @Nullable ClassLoader optionalParentClassLoader
    ) throws CompileException, IOException {
        ClassBodyEvaluator cbe = new ClassBodyEvaluator();
        cbe.setClassName(className);
        cbe.setExtendedClass(optionalExtendedClass);
        cbe.setImplementedInterfaces(implementedInterfaces);
        cbe.setParentClassLoader(optionalParentClassLoader);
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
            throw new JaninoRuntimeException(e.toString(), e);
        }
    }
}
