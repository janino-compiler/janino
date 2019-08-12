
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.AbstractCompiler;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleFinder;
import org.codehaus.commons.compiler.java9.java.lang.module.ModuleReference;
import org.codehaus.commons.compiler.util.Benchmark;
import org.codehaus.commons.compiler.util.StringPattern;
import org.codehaus.commons.compiler.util.StringUtil;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.codehaus.commons.compiler.util.resource.FileResource;
import org.codehaus.commons.compiler.util.resource.FileResourceCreator;
import org.codehaus.commons.compiler.util.resource.JarDirectoriesResourceFinder;
import org.codehaus.commons.compiler.util.resource.MultiResourceFinder;
import org.codehaus.commons.compiler.util.resource.PathResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile;

/**
 * JANINO implementation of {@link ICompiler}.
 */
public
class Compiler extends AbstractCompiler {

    private static final Logger LOGGER = Logger.getLogger(Compiler.class.getName());

    @Nullable private WarningHandler  warningHandler;
    @Nullable private ErrorHandler    compileErrorHandler;
    private EnumSet<JaninoOption>     options = EnumSet.noneOf(JaninoOption.class);

    private IClassLoader              iClassLoader     = new ClassLoaderIClassLoader();
    private Benchmark                 benchmark        = new Benchmark(false);

    { this.updateIClassLoader(); }

    // Compile time state:

    private final List<UnitCompiler> parsedCompilationUnits = new ArrayList<UnitCompiler>();

    /**
     * Initializes a new compiler.
     */
    public
    Compiler() {}

    /** @deprecated Use {@link #Compiler()} and the various configuration setters instead */
    @Deprecated public
    Compiler(ResourceFinder sourceFinder, IClassLoader parentIClassLoader) {
        this();
        this.setSourceFinder(sourceFinder);
        this.setIClassLoader(parentIClassLoader);
    }

    /** @deprecated Use {@link #Compiler()} and the various configuration setters instead */
    @Deprecated public
    Compiler(
        final File[]           sourcePath,
        final File[]           classPath,
        @Nullable final File[] extDirs,
        @Nullable final File[] bootClassPath,
        @Nullable final File   destinationDirectory,
        @Nullable final String characterEncoding,
        boolean                verbose,
        boolean                debugSource,
        boolean                debugLines,
        boolean                debugVars,
        StringPattern[]        warningHandlePatterns,
        boolean                rebuild
    ) {
        this.setSourcePath(sourcePath);
        this.setClassPath(classPath);
        this.setExtensionDirectories((File[]) Compiler.nullToEmptyArray(extDirs, File.class));
        this.setBootClassPath((File[]) Compiler.nullToEmptyArray(bootClassPath, File.class));
        this.setDestinationDirectory(destinationDirectory, rebuild);
        this.setCharacterEncoding(characterEncoding);
        this.setVerbose(verbose);
        this.setDebugSource(debugSource);
        this.setDebugLines(debugLines);
        this.setDebugVars(debugVars);
        this.setClassFileFinder(
            rebuild
            ? ResourceFinder.EMPTY_RESOURCE_FINDER
            : destinationDirectory == null // Compiler.NO_DESTINATION_DIRECTORY
            ? ICompiler.FIND_NEXT_TO_SOURCE_FILE
            : new DirectoryResourceFinder(destinationDirectory)
        );
        this.setVerbose(verbose);
        this.setDebugSource(debugSource);
        this.setDebugLines(debugLines);
        this.setDebugVars(debugVars);
        this.setCharacterEncoding(characterEncoding);
        this.setWarningHandler(
            new FilterWarningHandler(
                warningHandlePatterns,
                new WarningHandler() {

                    @Override public void
                    handleWarning(@Nullable String handle, String message, @Nullable Location location) {

                        StringBuilder sb = new StringBuilder();

                        if (location != null) sb.append(location).append(": ");

                        if (handle == null) {
                            sb.append("Warning: ");
                        } else {
                            sb.append("Warning ").append(handle).append(": ");
                        }

                        sb.append(message);

                        System.err.println(sb.toString());
                    }
                }
            )
        );

        this.benchmark.report("*** JANINO - an embedded compiler for the Java(TM) programming language");
        this.benchmark.report("*** For more information visit http://janino.codehaus.org");
        this.benchmark.report("Source path",             sourcePath);
        this.benchmark.report("Class path",              classPath);
        this.benchmark.report("Ext dirs",                extDirs);
        this.benchmark.report("Boot class path",         bootClassPath);
        this.benchmark.report("Destination directory",   destinationDirectory);
        this.benchmark.report("Character encoding",      characterEncoding);
        this.benchmark.report("Verbose",                 new Boolean(verbose));
        this.benchmark.report("Debug source",            new Boolean(debugSource));
        this.benchmark.report("Debug lines",             new Boolean(debugSource));
        this.benchmark.report("Debug vars",              new Boolean(debugSource));
        this.benchmark.report("Warning handle patterns", warningHandlePatterns);
        this.benchmark.report("Rebuild",                 new Boolean(rebuild));
    }

    @SuppressWarnings("unchecked") private static <T> T[]
    nullToEmptyArray(@Nullable T[] a, Class<T> elementType) {
        return a != null ? a : (T[]) Array.newInstance(elementType, 0);
    }

    /**
     * The default value for the <var>warningHandlerPatterns</var> parameter of {@link Compiler#Compiler(File[], File[],
     * File[], File[], File, String, boolean, boolean, boolean, boolean, StringPattern[], boolean)}.
     */
    public static final StringPattern[] DEFAULT_WARNING_HANDLE_PATTERNS = StringPattern.PATTERNS_NONE;

    /**
     * Installs a custom {@link ErrorHandler}. The default {@link ErrorHandler} prints the first 20 compile errors to
     * {@link System#err} and then throws a {@link CompileException}.
     * <p>
     *   Passing {@code null} restores the default {@link ErrorHandler}.
     * </p>
     * <p>
     *   Notice that scan and parse errors are <em>not</em> redirected to this {@link ErrorHandler}, instead, they
     *   cause a {@link CompileException} to be thrown. Also, the {@link Compiler} may choose to throw {@link
     *   CompileException}s in certain, fatal compile error situations, even if an {@link ErrorHandler} is installed.
     * </p>
     * <p>
     *   In other words: In situations where compilation can reasonably continue after a compile error, the {@link
     *   ErrorHandler} is called; all other error conditions cause a {@link CompileException} to be thrown.
     * </p>
     */
    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) { this.warningHandler = warningHandler; }

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.options; }

    /**
     * Sets the options for all future compilations.
     */
    public Compiler
    options(EnumSet<JaninoOption> options) {
        this.options = options;
        return this;
    }

    @Override public void
    compile(Resource[] sourceResources) throws CompileException, IOException {

        this.benchmark.beginReporting();
        try {

            final IClassLoader
            iClassLoader = new CompilerIClassLoader(this.sourceFinder, this.classFileFinder, this.iClassLoader);

            // Initialize compile time fields.
            this.parsedCompilationUnits.clear();

            // Parse all source files.
            for (Resource sourceResource : sourceResources) {
                Compiler.LOGGER.log(Level.FINE, "Compiling \"{0}\"", sourceResource);

                UnitCompiler uc = new UnitCompiler(
                    this.parseAbstractCompilationUnit(
                        sourceResource.getFileName(),                   // fileName
                        new BufferedInputStream(sourceResource.open()), // inputStream
                        this.encoding                                   // encoding
                    ),
                    iClassLoader
                );
                uc.setCompileErrorHandler(this.compileErrorHandler);
                uc.setWarningHandler(this.warningHandler);
                uc.options(this.options);

                this.parsedCompilationUnits.add(uc);
            }

            // Compile all parsed compilation units. The vector of parsed CUs may grow while they are being compiled,
            // but eventually all CUs will be compiled.
            for (int i = 0; i < this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler unitCompiler = (UnitCompiler) this.parsedCompilationUnits.get(i);

                File sourceFile;
                {
                    Java.AbstractCompilationUnit acu = unitCompiler.getAbstractCompilationUnit();
                    if (acu.optionalFileName == null) throw new InternalCompilerException();
                    sourceFile = new File(acu.optionalFileName);
                }

                unitCompiler.setCompileErrorHandler(this.compileErrorHandler);
                unitCompiler.setWarningHandler(this.warningHandler);

                this.benchmark.beginReporting("Compiling compilation unit \"" + sourceFile + "\"");
                ClassFile[] classFiles;
                try {

                    // Compile the compilation unit.
                    classFiles = unitCompiler.compileUnit(this.debugSource, this.debugLines, this.debugVars);
                } finally {
                    this.benchmark.endReporting();
                }

                // Store the compiled classes and interfaces into class files.
                this.benchmark.beginReporting(
                    "Storing "
                    + classFiles.length
                    + " class file(s) resulting from compilation unit \""
                    + sourceFile
                    + "\""
                );
                try {
                    for (ClassFile classFile : classFiles) this.storeClassFile(classFile, sourceFile);
                } finally {
                    this.benchmark.endReporting();
                }
            }
        } finally {
            this.benchmark.endReporting("Compiled " + this.parsedCompilationUnits.size() + " compilation unit(s)");
        }
    }

    /**
     * Reads one compilation unit from a file and parses it.
     * <p>
     *   The <var>inputStream</var> is closed before the method returns.
     * </p>
     *
     * @return the parsed compilation unit
     */
    private Java.AbstractCompilationUnit
    parseAbstractCompilationUnit(
        String            fileName,
        InputStream       inputStream,
        @Nullable Charset encoding
    ) throws CompileException, IOException {
        try {

            Scanner scanner = new Scanner(
                fileName,
                new InputStreamReader(inputStream, encoding != null ? encoding : Charset.defaultCharset())
            );

            Parser parser = new Parser(scanner);
            parser.setWarningHandler(this.warningHandler);

            this.benchmark.beginReporting("Parsing \"" + fileName + "\"");
            try {
                return parser.parseAbstractCompilationUnit();
            } finally {
                this.benchmark.endReporting();
            }
        } finally {
            inputStream.close();
        }
    }

    /**
     * Constructs the name of a file that could store the byte code of the class with the given name.
     * <p>
     *   If <var>destinationDirectory</var> is non-{@code null}, the returned path is the
     *   <var>destinationDirectory</var> plus the package of the class (with dots replaced with file separators) plus
     *   the class name plus ".class". Example: "destdir/pkg1/pkg2/Outer$Inner.class"
     * </p>
     * <p>
     *   If <var>destinationDirectory</var> is null, the returned path is the directory of the <var>sourceFile</var>
     *   plus the class name plus ".class". Example: "srcdir/Outer$Inner.class"
     * </p>
     *
     * @param className            E.g. {@code "pkg1.pkg2.Outer$Inner"}
     * @param sourceFile           E.g. {@code "srcdir/Outer.java"}
     * @param destinationDirectory E.g. {@code "destdir"}
     */
    public static File
    getClassFile(String className, File sourceFile, @Nullable File destinationDirectory) {
        if (destinationDirectory != null) {
            return new File(destinationDirectory, ClassFile.getClassFileResourceName(className));
        } else {
            int idx = className.lastIndexOf('.');
            return new File(
                sourceFile.getParentFile(),
                ClassFile.getClassFileResourceName(className.substring(idx + 1))
            );
        }
    }

    /**
     * Stores the byte code of this {@link ClassFile} in the file system. Directories are created as necessary.
     *
     * @param classFile
     * @param sourceFile Required to compute class file path if no destination directory given
     */
    public void
    storeClassFile(ClassFile classFile, final File sourceFile) throws IOException {
        String classFileResourceName = ClassFile.getClassFileResourceName(classFile.getThisClassName());

        // Determine where to create the class file.
        ResourceCreator rc;
        if (this.classFileCreator != ICompiler.CREATE_NEXT_TO_SOURCE_FILE) {
            rc = this.classFileCreator;
            assert rc != null;
        } else {

            // If the JAVAC option "-d" is given, place the class file next
            // to the source file, irrespective of the package name.
            rc = new FileResourceCreator() {

                @Override protected File
                getFile(String resourceName) {
                    return new File(
                        sourceFile.getParentFile(),
                        resourceName.substring(resourceName.lastIndexOf('/') + 1)
                    );
                }
            };
        }
        OutputStream os = rc.createResource(classFileResourceName);
        try {
            classFile.store(os);
        } catch (IOException ioe) {
            try { os.close(); } catch (IOException e) {}
            os = null;
            if (!rc.deleteResource(classFileResourceName)) {
                IOException ioe2 = new IOException(
                    "Could not delete incompletely written class file \""
                    + classFileResourceName
                    + "\""
                );
                ioe2.initCause(ioe);
                throw ioe2; // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
            throw ioe;
        } finally {
            if (os != null) try { os.close(); } catch (IOException e) {}
        }
    }

    /**
     * Loads "auxiliary classes", typically from BOOTCLASSPATH + EXTDIR + CLASSPATH (but <em>not</em> from the
     * "destination directory"!).
     */
    public void
    setIClassLoader(IClassLoader iClassLoader) { this.iClassLoader = iClassLoader; }

    @Override public void
    setVerbose(boolean verbose) { this.benchmark = new Benchmark(verbose); }

    @Override public void
    setBootClassPath(File[] directoriesAndArchives) {
        super.setBootClassPath(directoriesAndArchives);
        this.updateIClassLoader();
    }

    @Override public void
    setExtensionDirectories(File[] directories) {
        super.setExtensionDirectories(directories);
        this.updateIClassLoader();
    }

    @Override public void
    setClassPath(File[] directoriesAndArchives) {
        super.setClassPath(directoriesAndArchives);
        this.updateIClassLoader();
    }

    private void
    updateIClassLoader() {

        File[] bcp = this.bootClassPath;

        if (bcp != null) {

            // JVM 1.0-1.8; BOOTCLASSPATH supported:
            this.setIClassLoader(new ResourceFinderIClassLoader(
                new MultiResourceFinder(Arrays.asList(
                    new PathResourceFinder(bcp),
                    new JarDirectoriesResourceFinder(this.extensionDirectories),
                    new PathResourceFinder(this.classPath)
                )),
                null
            ));
        } else {

            // JVM 9+: "Modules" replace the BOOTCLASSPATH:
            String sbcp = System.getProperty("sun.boot.class.path");
            if (sbcp != null) {
                this.bootClassPath = StringUtil.parsePath(sbcp);
            } else {
                URL r = ClassLoader.getSystemClassLoader().getResource("java/lang/Object.class");
                assert r != null;

                assert "jrt".equalsIgnoreCase(r.getProtocol()) : r.toString();

                ResourceFinder rf = new ResourceFinder() {

                    @Override @Nullable public Resource
                    findResource(final String resourceName) {

                        try {
                            final Set<ModuleReference> mrs = ModuleFinder.ofSystem().findAll();

                            for (final ModuleReference mr : mrs) {
                                final URI           moduleContentLocation = (URI) mr.location().get();
                                final URL           classFileUrl          = new URL(moduleContentLocation + "/" + resourceName); // SUPPRESS CHECKSTYLE LineLength
                                final URLConnection uc                    = classFileUrl.openConnection();
                                try {
                                    uc.connect();
                                    return new Resource() {

                                        @Override public InputStream
                                        open() throws IOException {
                                            try {
                                                return uc.getInputStream();
                                            } catch (IOException ioe) {
                                                throw new IOException(moduleContentLocation + ", " + resourceName, ioe);
                                            }
                                        }

                                        @Override public String getFileName()  { return resourceName;         }
                                        @Override public long   lastModified() { return uc.getLastModified(); }
                                    };
                                } catch (IOException ioe) {
                                    ;
                                }
                            }
                            return null;
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }
                };

                this.setIClassLoader(new ResourceFinderIClassLoader(
                    new MultiResourceFinder(Arrays.asList(
                        rf,
                        new JarDirectoriesResourceFinder(this.extensionDirectories),
                        new PathResourceFinder(this.classPath)
                    )),
                    null
                ));
            }
        }
    }

    /**
     * A specialized {@link IClassLoader} that loads {@link IClass}es from the following sources:
     * <ol>
     *   <li>An already-parsed compilation unit
     *   <li>A class file in the output directory (if existent and younger than source file)
     *   <li>A source file in any of the source path directories
     *   <li>The parent class loader
     * </ol>
     * <p>
     *   Notice that the {@link CompilerIClassLoader} is an inner class of {@link Compiler} and heavily uses {@link
     *   Compiler}'s members.
     * </p>
     */
    private
    class CompilerIClassLoader extends IClassLoader {

        private final ResourceFinder           sourceFinder;
        @Nullable private final ResourceFinder classFileFinder;

        /**
         * @param sourceFinder       Where to look for more source files
         * @param classFileFinder    Where to look for previously generated .class resources, or {@link
         *                           #FIND_NEXT_TO_SOURCE_FILE}
         * @param parentIClassLoader {@link IClassLoader} through which {@link IClass}es are to be loaded
         */
        CompilerIClassLoader(
            ResourceFinder           sourceFinder,
            @Nullable ResourceFinder classFileFinder,
            IClassLoader             parentIClassLoader
        ) {
            super(parentIClassLoader);
            this.sourceFinder    = sourceFinder;
            this.classFileFinder = classFileFinder;
            super.postConstruct();
        }

        /**
         * @param type                    field descriptor of the {@link IClass} to load, e.g. {@code
         *                                "Lpkg1/pkg2/Outer$Inner;"}
         * @return                        {@code null} if a the type could not be found
         * @throws ClassNotFoundException An exception was raised while loading the {@link IClass}
         */
        @Override @Nullable protected IClass
        findIClass(final String type) throws ClassNotFoundException {
            Compiler.LOGGER.entering(null, "findIClass", type);

            // Determine the class name.
            String className = Descriptor.toClassName(type); // E.g. "pkg1.pkg2.Outer$Inner"
            Compiler.LOGGER.log(Level.FINE, "className={0}", className);

            // Do not attempt to load classes from package "java".
            if (className.startsWith("java.")) return null;

            // Determine the name of the top-level class.
            String topLevelClassName;
            {
                int idx = className.indexOf('$');
                topLevelClassName = idx == -1 ? className : className.substring(0, idx);
            }

            // Check the already-parsed compilation units.
            for (int i = 0; i < Compiler.this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler uc  = (UnitCompiler) Compiler.this.parsedCompilationUnits.get(i);
                IClass       res = uc.findClass(topLevelClassName);
                if (res != null) {
                    if (!className.equals(topLevelClassName)) {
                        res = uc.findClass(className);
                        if (res == null) return null;
                    }
                    this.defineIClass(res);
                    return res;
                }
            }

            // Search source path for uncompiled class.
            final Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
            if (sourceResource == null) return null;

            // Find an existing class file.
            ResourceFinder cff = this.classFileFinder;

            Resource classFileResource;
            if (cff != ICompiler.FIND_NEXT_TO_SOURCE_FILE) {
                assert cff != null;
                classFileResource = cff.findResource(
                    ClassFile.getClassFileResourceName(className)
                );
            } else {
                if (!(sourceResource instanceof FileResource)) return null;
                File classFile = new File(
                    ((FileResource) sourceResource).getFile().getParentFile(),
                    ClassFile.getClassFileResourceName(className.substring(className.lastIndexOf('.') + 1))
                );
                classFileResource = classFile.exists() ? new FileResource(classFile) : null;
            }

            // Compare source modification time against class file modification time.
            if (classFileResource != null && sourceResource.lastModified() <= classFileResource.lastModified()) {

                // The class file is up-to-date; load it.
                return this.defineIClassFromClassFileResource(classFileResource);
            } else {

                // Source file not yet compiled or younger than class file.
                return this.defineIClassFromSourceResource(sourceResource, className);
            }
        }

        /**
         * Parses the compilation unit stored in the given <var>sourceResource</var>, remembers it in {@code
         * Compiler.this.parsedCompilationUnits} (it may declare other classes that are needed later), finds the
         * declaration of the type with the given <var>className</var>, and defines it in the {@link IClassLoader}.
         * <p>
         *   Notice that the compilation unit is not compiled here!
         * </p>
         */
        private IClass
        defineIClassFromSourceResource(Resource sourceResource, String className) throws ClassNotFoundException {

            // Parse the source file.
            UnitCompiler uc;
            try {
                Java.AbstractCompilationUnit acu = Compiler.this.parseAbstractCompilationUnit(
                    sourceResource.getFileName(),                   // fileName
                    new BufferedInputStream(sourceResource.open()), // inputStream
                    Compiler.this.encoding
                );
                uc = new UnitCompiler(acu, this).options(Compiler.this.options);
            } catch (IOException ex) {
                throw new ClassNotFoundException("Parsing compilation unit \"" + sourceResource + "\"", ex);
            } catch (CompileException ex) {
                throw new ClassNotFoundException("Parsing compilation unit \"" + sourceResource + "\"", ex);
            }

            // Remember compilation unit for later compilation.
            Compiler.this.parsedCompilationUnits.add(uc);

            // Define the class.
            IClass res = uc.findClass(className);
            if (res == null) {

                // This is a really complicated case: We may find a source file on the source
                // path that seemingly contains the declaration of the class we are looking
                // for, but doesn't. This is possible if the underlying file system has
                // case-insensitive file names and/or file names that are limited in length
                // (e.g. DOS 8.3).
                throw new ClassNotFoundException("\"" + sourceResource + "\" does not declare \"" + className + "\"");
            }
            this.defineIClass(res);
            return res;
        }

        /**
         * Opens the given <var>classFileResource</var>, reads its contents, defines it in the {@link IClassLoader},
         * and resolves it (this step may involve loading more classes).
         */
        private IClass
        defineIClassFromClassFileResource(Resource classFileResource) throws ClassNotFoundException {
            Compiler.this.benchmark.beginReporting("Loading class file \"" + classFileResource.getFileName() + "\"");
            try {
                InputStream is = null;
                ClassFile   cf;
                try {
                    is = classFileResource.open();
                    cf = new ClassFile(new BufferedInputStream(is));
                } catch (IOException ex) {
                    throw new ClassNotFoundException("Opening class file resource \"" + classFileResource + "\"", ex);
                } finally {
                    if (is != null) try { is.close(); } catch (IOException e) {}
                }
                ClassFileIClass result = new ClassFileIClass(
                    cf,                       // classFile
                    CompilerIClassLoader.this // iClassLoader
                );

                // Important: We must FIRST call "defineIClass()" so that the
                // new IClass is known to the IClassLoader, and THEN
                // "resolveAllClasses()", because otherwise endless recursion could
                // occur.
                this.defineIClass(result);
                result.resolveAllClasses();

                return result;
            } finally {
                Compiler.this.benchmark.endReporting();
            }
        }
    }
}
