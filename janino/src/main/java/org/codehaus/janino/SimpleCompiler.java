
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.TypeVisitor;
import org.codehaus.janino.util.ClassFile;

/**
 * To set up a {@link SimpleCompiler} object, proceed as described for {@link ISimpleCompiler}. Alternatively, a number
 * of "convenience constructors" exist that execute the described steps instantly.
 */
public
class SimpleCompiler extends Cookable implements ISimpleCompiler {

    private static final Logger LOGGER = Logger.getLogger(SimpleCompiler.class.getName());

    private ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();

    // Set while "cook()"ing.
    @Nullable private ClassLoaderIClassLoader classLoaderIClassLoader;

//    @Nullable private ClassLoader    result;
    @Nullable private ErrorHandler   compileErrorHandler;
    @Nullable private WarningHandler warningHandler;

    private boolean debugSource   = Boolean.getBoolean(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE);
    private boolean debugLines    = this.debugSource;
    private boolean debugVars     = this.debugSource;
    private int     sourceVersion = -1;
    private int     targetVersion = -1;

    private EnumSet<JaninoOption> options = EnumSet.noneOf(JaninoOption.class);

    /**
     * {@code Null} before cooking, non-{@code null} after cooking.
     */
    @Nullable private ClassFile[] classFiles;

    public static void // SUPPRESS CHECKSTYLE JavadocMethod
    main(String[] args) throws Exception {
        if (args.length >= 1 && "-help".equals(args[0])) {
            System.out.println("Usage:");
            System.out.println("    org.codehaus.janino.SimpleCompiler <source-file> <class-name> { <argument> }");
            System.out.println("Reads a compilation unit from the given <source-file> and invokes method");
            System.out.println("\"public static void main(String[])\" of class <class-name>, passing the");
            System.out.println("given <argument>s.");
            System.exit(1);
        }

        if (args.length < 2) {
            System.err.println("Source file and/or class name missing; try \"-help\".");
            System.exit(1);
        }

        // Get source file.
        String sourceFileName = args[0];

        // Get class name.
        String className = args[1];

        // Get arguments.
        String[] arguments = new String[args.length - 2];
        System.arraycopy(args, 2, arguments, 0, arguments.length);

        // Compile the source file.
        ClassLoader cl = new SimpleCompiler(sourceFileName, new FileInputStream(sourceFileName)).getClassLoader();

        // Load the class.
        Class<?> c = cl.loadClass(className);

        // Invoke the "public static main(String[])" method.
        Method m = c.getMethod("main", String[].class);
        m.invoke(null, (Object) arguments);
    }

    /**
     * Equivalent to
     * <pre>
     *     SimpleCompiler sc = new SimpleCompiler();
     *     sc.cook(fileName, in);
     * </pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cook(String, Reader)
     */
    public
    SimpleCompiler(@Nullable String fileName, Reader in) throws IOException, CompileException {
        this.cook(fileName, in);
    }

    /**
     * Equivalent to
     * <pre>
     *     SimpleCompiler sc = new SimpleCompiler();
     *     sc.cook(fileName, is);
     * </pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cook(String, InputStream)
     */
    public
    SimpleCompiler(@Nullable String fileName, InputStream is) throws IOException, CompileException {
        this.cook(fileName, is);
    }

    /**
     * Equivalent to
     * <pre>
     *     SimpleCompiler sc = new SimpleCompiler();
     *     sc.cook(fileName);
     * </pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cookFile(String)
     */
    public
    SimpleCompiler(String fileName) throws IOException, CompileException {
        this.cookFile(fileName);
    }

    /**
     * Equivalent to
     * <pre>
     *     SimpleCompiler sc = new SimpleCompiler();
     *     sc.setParentClassLoader(parentClassLoader);
     *     sc.cook(scanner);
     * </pre>
     *
     * @see #SimpleCompiler()
     * @see #setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Reader)
     */
    public
    SimpleCompiler(Scanner scanner, @Nullable ClassLoader parentClassLoader)
    throws IOException, CompileException {
        this.setParentClassLoader(parentClassLoader);
        this.cook(scanner);
    }

    public SimpleCompiler() {}

    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) {
        this.parentClassLoader = (
            parentClassLoader != null
            ? parentClassLoader
            : Thread.currentThread().getContextClassLoader()
        );
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;
    }

    /**
     * Scans, parses and compiles a given compilation unit from the given {@link Reader}. After completion, {@link
     * #getClassLoader()} returns a {@link ClassLoader} that allows for access to the compiled classes.
     */
    @Override public final void
    cook(@Nullable String fileName, Reader r) throws CompileException, IOException {
        this.cook(new Scanner(fileName, r));
    }

    /**
     * Scans, parses and compiles a given compilation unit from the given scanner. After completion, {@link
     * #getClassLoader()} returns a {@link ClassLoader} that allows for access to the compiled classes.
     */
    public void
    cook(Scanner scanner) throws CompileException, IOException {

        Parser parser = new Parser(scanner);
        parser.setSourceVersion(this.sourceVersion);
        parser.setWarningHandler(this.warningHandler);

        AbstractCompilationUnit acu;
        try {
            acu = parser.parseAbstractCompilationUnit();
        } catch (CompileException ce) {
            this.classFiles = new ClassFile[0]; // Mark this SimpleCompiler as "cooked".
            throw ce;
        }

        this.compileToClassLoader(acu);
    }

    /**
     * Cooks this compilation unit directly.
     */
    public void
    cook(Java.AbstractCompilationUnit abstractCompilationUnit) throws CompileException {

        SimpleCompiler.LOGGER.entering(null, "cook", abstractCompilationUnit);

        this.assertUncooked();

        IClassLoader icl = (this.classLoaderIClassLoader = new ClassLoaderIClassLoader(this.parentClassLoader));
        try {

            // Compile compilation unit to class files.
            UnitCompiler unitCompiler = new UnitCompiler(abstractCompilationUnit, icl).options(this.options);
            unitCompiler.setTargetVersion(this.targetVersion);
            unitCompiler.setCompileErrorHandler(this.compileErrorHandler);
            unitCompiler.setWarningHandler(this.warningHandler);

            this.classFiles = unitCompiler.compileUnit(this.debugSource, this.debugLines, this.debugVars);
        } catch (CompileException ce) {
            this.classFiles = new ClassFile[0]; // Mark this SimpleCompiler as "cooked".
            throw ce;
        } finally {
            this.classLoaderIClassLoader = null;
        }
    }

    /**
     * @return The {@link ClassFile}s that were generated during cooking
     */
    public ClassFile[]
    getClassFiles() { return this.assertCooked(); }

    /**
     * Controls the language elements that are accepted by the {@link Parser}.
     *
     * @see Parser#setSourceVersion(int)
     */
    @Override public void
    setSourceVersion(int version) { this.sourceVersion = version; }

    /**
     * Controls the .class files that are generated by the {@link UnitCompiler}.
     *
     * @see UnitCompiler#setTargetVersion(int)
     */
    @Override public void
    setTargetVersion(int version) { this.targetVersion = version; }

    @Override public Map<String /*className*/, byte[] /*bytecode*/>
    getBytecodes() {
        if (this.getBytecodesCache != null) return this.getBytecodesCache;
        return (this.getBytecodesCache = this.getBytecodes2());
    }
    @Nullable private Map<String /*className*/, byte[] /*bytecode*/> getBytecodesCache;

    private Map<String /*className*/, byte[] /*bytecode*/>
    getBytecodes2() {

        Map<String /*className*/, byte[] /*bytecode*/> result = new HashMap<>();
        for (ClassFile cf : this.getClassFiles()) {
            result.put(cf.getThisClassName(), cf.toByteArray());
        }

        return result;
    }

    @Override public ClassLoader
    getClassLoader() {
        if (this.getClassLoaderCache != null) return this.getClassLoaderCache;
        return (this.getClassLoaderCache = this.getClassLoader2());
    }
    @Nullable private ClassLoader getClassLoaderCache;

    private ClassLoader
    getClassLoader2() {

        final Map<String, byte[]> bytecode = this.getBytecodes();

        // Create a ClassLoader that loads the generated classes.
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override public ClassLoader
            run() {
                return new ByteArrayClassLoader(
                    bytecode,                             // classes
                    SimpleCompiler.this.parentClassLoader // parent
                );
            }
        });
    }

    /**
     * Two {@link SimpleCompiler}s are regarded equal iff
     * <ul>
     *   <li>Both are objects of the same class (e.g. both are {@link ScriptEvaluator}s)
     *   <li>Both generated functionally equal classes as seen by {@link ByteArrayClassLoader#equals(Object)}
     * </ul>
     */
    @Override public boolean
    equals(@Nullable Object o) {

        if (!(o instanceof SimpleCompiler)) return false;

        SimpleCompiler that = (SimpleCompiler) o;

        if (this.getClass() != that.getClass()) return false;

        return this.assertCooked().equals(that.assertCooked());
    }

    @Override public int
    hashCode() { return this.parentClassLoader.hashCode(); }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.options; }

    /**
     * Sets the options for all future compilations.
     */
    public SimpleCompiler
    options(EnumSet<JaninoOption> options) {
        this.options = options;
        return this;
    }

    /**
     * Wraps a reflection {@link Class} in a {@link Java.Type} object.
     */
    @Nullable protected Java.Type
    optionalClassToType(final Location location, @Nullable final Class<?> clazz) {
        if (clazz == null) return null;
        return this.classToType(location, clazz);
    }

    /**
     * Wraps a reflection {@link Class} in a {@link Java.Type} object.
     */
    protected Java.Type
    classToType(final Location location, final Class<?> clazz) {

        // Can't use a SimpleType here because the classLoaderIClassLoader is not yet set up. Instead, create a
        // Type that lazily creates a delegate Type at COMPILE TIME.
        return new Java.Type(location) {

            @Nullable private Java.SimpleType delegate;

            @Override @Nullable public <R, EX extends Throwable> R
            accept(AtomVisitor<R, EX> visitor) throws EX { return visitor.visitType(this.getDelegate()); }

            @Override @Nullable public <R, EX extends Throwable> R
            accept(TypeVisitor<R, EX> visitor) throws EX { return this.getDelegate().accept(visitor); }

            @Override public String
            toString() { return this.getDelegate().toString(); }

            private Java.Type
            getDelegate() {

                if (this.delegate != null) return this.delegate;

                ClassLoaderIClassLoader icl = SimpleCompiler.this.classLoaderIClassLoader;
                assert icl != null;

                IClass iClass;
                try {
                    iClass = icl.loadIClass(
                        Descriptor.fromClassName(clazz.getName())
                    );
                } catch (ClassNotFoundException ex) {
                    throw new InternalCompilerException("Loading IClass \"" + clazz.getName() + "\": " + ex);
                }
                if (iClass == null) {
                    throw new InternalCompilerException(
                        "Cannot load class '"
                        + clazz.getName()
                        + "' through the parent loader"
                    );
                }

                // Verify that the class loaders match.
                IClass   iClass2 = iClass;
                Class<?> class2  = clazz;
                for (;;) {
                    IClass ct = iClass2.getComponentType();
                    if (ct == null) {
                        if (class2.getComponentType() != null) {
                            throw new InternalCompilerException("Array type/class inconsistency");
                        }
                        break;
                    }
                    iClass2 = ct;
                    class2  = class2.getComponentType();
                    if (class2 == null) throw new InternalCompilerException("Array type/class inconsistency");
                }
                if (class2.isPrimitive()) {
                    if (!iClass2.isPrimitive()) {
                        throw new InternalCompilerException("Primitive type/class inconsistency");
                    }
                } else {
                    if (iClass2.isPrimitive()) {
                        throw new InternalCompilerException("Primitive type/class inconsistency");
                    }
                    if (((ReflectionIClass) iClass2).getClazz() != class2) {
                        throw new InternalCompilerException(
                            "Class '"
                            + class2.getName()
                            + "' was loaded through a different loader"
                        );
                    }
                }

                return (this.delegate = new Java.SimpleType(location, iClass));
            }
        };
    }

    /**
     * Converts an array of {@link Class}es into an array of{@link Java.Type}s.
     */
    protected Java.Type[]
    classesToTypes(Location location, @Nullable Class<?>[] classes) {

        if (classes == null) return new Java.Type[0];

        Java.Type[] types = new Java.Type[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            types[i] = this.classToType(location, classes[i]);
        }
        return types;
    }

    /**
     * Compiles the given compilation unit. (A "compilation unit" is typically the contents of a Java source file.)
     *
     * @param abstractCompilationUnit The parsed compilation unit
     * @return                        The {@link ClassLoader} into which the compiled classes were defined
     * @throws CompileException
     */
    protected final ClassLoader
    compileToClassLoader(Java.AbstractCompilationUnit abstractCompilationUnit) throws CompileException {
        this.cook(abstractCompilationUnit);
        return this.getClassLoader();
    }

    /**
     * @throws IllegalStateException This SimpleCompiler is already cooked
     */
    private void
    assertUncooked() {
        if (this.classFiles != null) throw new IllegalStateException("Must only be called before \"cook()\"");
    }

    /**
     * @return The {@link ClassFile}s that were created when this {@link SimpleCompiler} was {@link #cook(Reader)}ed
     *
     * @throws IllegalStateException This SimpleCompiler is not yet cooked
     */
    private ClassFile[]
    assertCooked() {

        ClassFile[] result = this.classFiles;
        if (result == null) throw new IllegalStateException("Must only be called after \"cook()\"");

        return result;
    }
}
