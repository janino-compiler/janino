
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.util.Benchmark;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.StringPattern;
import org.codehaus.janino.util.resource.DirectoryResourceCreator;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.FileResource;
import org.codehaus.janino.util.resource.FileResourceCreator;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.Resource;
import org.codehaus.janino.util.resource.ResourceCreator;
import org.codehaus.janino.util.resource.ResourceFinder;


/**
 * A simplified substitute for the <tt>javac</tt> tool.
 * <p>
 *   Usage:
 * </p>
 * <pre>
 *     java org.codehaus.janino.Compiler \
 *               [ -d <em>destination-dir</em> ] \
 *               [ -sourcepath <em>dirlist</em> ] \
 *               [ -classpath <em>dirlist</em> ] \
 *               [ -extdirs <em>dirlist</em> ] \
 *               [ -bootclasspath <em>dirlist</em> ] \
 *               [ -encoding <em>encoding</em> ] \
 *               [ -verbose ] \
 *               [ -g:none ] \
 *               [ -g:{source,lines,vars} ] \
 *               [ -warn:<em>pattern-list</em> ] \
 *               <em>source-file</em> ...
 *     java org.codehaus.janino.Compiler -help
 * </pre>
 */
public
class Compiler {

    private static final Logger LOGGER = Logger.getLogger(Compiler.class.getName());

    /**
     * Command line interface.
     */
    public static void
    main(String[] args) {
        File            destinationDirectory      = Compiler.NO_DESTINATION_DIRECTORY;
        File[]          optionalSourcePath        = null;
        File[]          classPath                 = { new File(".") };
        File[]          optionalExtDirs           = null;
        File[]          optionalBootClassPath     = null;
        String          optionalCharacterEncoding = null;
        boolean         verbose                   = false;
        boolean         debugSource               = true;
        boolean         debugLines                = true;
        boolean         debugVars                 = false;
        StringPattern[] warningHandlePatterns     = Compiler.DEFAULT_WARNING_HANDLE_PATTERNS;
        boolean         rebuild                   = false;

        // Process command line options.
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-') break;
            if ("-d".equals(arg)) {
                destinationDirectory = new File(args[++i]);
            } else
            if ("-sourcepath".equals(arg)) {
                optionalSourcePath = PathResourceFinder.parsePath(args[++i]);
            } else
            if ("-classpath".equals(arg)) {
                classPath = PathResourceFinder.parsePath(args[++i]);
            } else
            if ("-extdirs".equals(arg)) {
                optionalExtDirs = PathResourceFinder.parsePath(args[++i]);
            } else
            if ("-bootclasspath".equals(arg)) {
                optionalBootClassPath = PathResourceFinder.parsePath(args[++i]);
            } else
            if ("-encoding".equals(arg)) {
                optionalCharacterEncoding = args[++i];
            } else
            if ("-verbose".equals(arg)) {
                verbose = true;
            } else
            if ("-g".equals(arg)) {
                debugSource = true;
                debugLines  = true;
                debugVars   = true;
            } else
            if (arg.startsWith("-g:")) {
                if (arg.indexOf("none")   != -1) debugSource = (debugLines = (debugVars = false));
                if (arg.indexOf("source") != -1) debugSource = true;
                if (arg.indexOf("lines")  != -1) debugLines = true;
                if (arg.indexOf("vars")   != -1) debugVars = true;
            } else
            if (arg.startsWith("-warn:")) {
                warningHandlePatterns = StringPattern.parseCombinedPattern(arg.substring(6));
            } else
            if ("-rebuild".equals(arg)) {
                rebuild = true;
            } else
            if ("-help".equals(arg)) {
                System.out.printf(Compiler.USAGE, (Object[]) null);
                System.exit(1);
            } else
            {
                System.err.println("Unrecognized command line option \"" + arg + "\"; try \"-help\".");
                System.exit(1);
            }
        }

        // Get source file names.
        if (i == args.length) {
            System.err.println("No source files given on command line; try \"-help\".");
            System.exit(1);
        }
        File[] sourceFiles = new File[args.length - i];
        for (int j = i; j < args.length; ++j) sourceFiles[j - i] = new File(args[j]);

        // Create the compiler object.
        final Compiler compiler = new Compiler(
            optionalSourcePath,
            classPath,
            optionalExtDirs,
            optionalBootClassPath,
            destinationDirectory,
            optionalCharacterEncoding,
            verbose,
            debugSource,
            debugLines,
            debugVars,
            warningHandlePatterns,
            rebuild
        );

        // Compile source files.
        try {
            compiler.compile(sourceFiles);
        } catch (Exception e) {

            if (verbose) {
                e.printStackTrace();
            } else {
                System.err.println(e.toString());
            }

            System.exit(1);
        }
    }

    private static final String USAGE = (
        ""
        + "Usage:%n"
        + "%n"
        + "  java " + Compiler.class.getName() + " [ <option> ] ... <source-file> ...%n"
        + "%n"
        + "Supported <option>s are:%n"
        + "  -d <output-dir>           Where to save class files%n"
        + "  -sourcepath <dirlist>     Where to look for other source files%n"
        + "  -classpath <dirlist>      Where to look for other class files%n"
        + "  -extdirs <dirlist>        Where to look for other class files%n"
        + "  -bootclasspath <dirlist>  Where to look for other class files%n"
        + "  -encoding <encoding>      Encoding of source files, e.g. \"UTF-8\" or \"ISO-8859-1\"%n"
        + "  -verbose%n"
        + "  -g                        Generate all debugging info%n"
        + "  -g:none                   Generate no debugging info%n"
        + "  -g:{source,lines,vars}    Generate only some debugging info%n"
        + "  -warn:<pattern-list>      Issue certain warnings; examples:%n"
        + "    -warn:*                 Enables all warnings%n"
        + "    -warn:IASF              Only warn against implicit access to static fields%n"
        + "    -warn:*-IASF            Enables all warnings, except those against implicit%n"
        + "                            access to static fields%n"
        + "    -warn:*-IA*+IASF        Enables all warnings, except those against implicit%n"
        + "                            accesses, but do warn against implicit access to%n"
        + "                            static fields%n"
        + "  -rebuild                  Compile all source files, even if the class files%n"
        + "                            seems up-to-date%n"
        + "  -help%n"
        + "%n"
        + "The default encoding in this environment is \"" + Charset.defaultCharset().toString() + "\"."
    );

    /**
     * Special value for "classFileResourceFinder".
     */
    @Nullable public static final ResourceFinder  FIND_NEXT_TO_SOURCE_FILE = null;

    /**
     * Special value for "classFileResourceCreator".
     */
    @Nullable public static final ResourceCreator CREATE_NEXT_TO_SOURCE_FILE = null;

    @Nullable private final ResourceCreator classFileCreator;
    @Nullable private final ResourceFinder  classFileFinder;
    @Nullable private final String          optionalCharacterEncoding;
    private final Benchmark                 benchmark;
    private final boolean                   debugSource;
    private final boolean                   debugLines;
    private final boolean                   debugVars;
    @Nullable private WarningHandler        optionalWarningHandler;
    @Nullable private ErrorHandler          optionalCompileErrorHandler;

    private final IClassLoader       iClassLoader;
    private final List<UnitCompiler> parsedCompilationUnits = new ArrayList<UnitCompiler>();

    /**
     * Initializes a Java compiler with the given parameters.
     * <p>
     *   Classes are searched in the following order:
     * <p>
     * <ul>
     *   <li>
     *     <b>If <var>optionalBootClassPath</var> is {@code null}:</b> Through the system class loader of the JVM
     *     that runs JANINO
     *   </li>
     *   <li>
     *     <b>If <var>optionalBootClassPath</var> is not {@code null}:</b> Through the
     *     <var>optionalBootClassPath</var>
     *   </li>
     *   <li>
     *     <b>If <var>optionalExtDirs</var> is not {@code null}:</b> Through the <var>optionalExtDirs</var>
     *   </li>
     *   <li>
     *     Through the <var>classPath</var>
     *   </li>
     *   <li>
     *     <b>If <var>optionalSourcePath</var> is {@code null}:</b> Through source files found on the
     *     <var>classPath</var>
     *   </li>
     *   <li>
     *     <b>If <var>optionalSourcePath</var> is not {@code null}:</b> Through source files found on the {@code
     *     sourcePath}
     *   </li>
     * </ul>
     * <p>
     *   The file name of a class file that represents class "pkg.Example" is determined as follows:
     * </p>
     * <ul>
     *   <li>
     *     <b>If <var>optionalDestinationDirectory</var> is not {@link #NO_DESTINATION_DIRECTORY}:</b>
     *     <var>optionalDestinationDirectory</var>{@code /pkg/Example.class}
     *   </li>
     *   <li>
     *     <b>If <var>optionalDestinationDirectory</var> is {@link #NO_DESTINATION_DIRECTORY}:</b> {@code
     *     dir1/dir2/Example.class} (Assuming that the file name of the source file that declares the class was {@code
     *     dir1/dir2/Any.java}.)
     *   </li>
     * </ul>
     *
     * @see #DEFAULT_WARNING_HANDLE_PATTERNS
     */
    public
    Compiler(
        @Nullable final File[] optionalSourcePath,
        final File[]           classPath,
        @Nullable final File[] optionalExtDirs,
        @Nullable final File[] optionalBootClassPath,
        @Nullable final File   destinationDirectory,
        @Nullable final String optionalCharacterEncoding,
        boolean                verbose,
        boolean                debugSource,
        boolean                debugLines,
        boolean                debugVars,
        StringPattern[]        warningHandlePatterns,
        boolean                rebuild
    ) {
        this(
            new PathResourceFinder(                       // sourceFinder
                optionalSourcePath == null ? classPath : optionalSourcePath
            ),
            IClassLoader.createJavacLikePathIClassLoader( // iClassLoader
                optionalBootClassPath,
                optionalExtDirs,
                classPath
            ),
            (                                             // classFileFinder
                rebuild
                ? ResourceFinder.EMPTY_RESOURCE_FINDER
                : destinationDirectory == null // Compiler.NO_DESTINATION_DIRECTORY
                ? Compiler.FIND_NEXT_TO_SOURCE_FILE
                : new DirectoryResourceFinder(destinationDirectory)
            ),
            (                                             // classFileCreator
                destinationDirectory == null // Compiler.NO_DESTINATION_DIRECTORY
                ? Compiler.CREATE_NEXT_TO_SOURCE_FILE
                : new DirectoryResourceCreator(destinationDirectory)
            ),
            optionalCharacterEncoding,                    // optionalCharacterEncoding
            verbose,                                      // verbose
            debugSource,                                  // debugSource
            debugLines,                                   // debugLines
            debugVars,                                    // debugVars
            new FilterWarningHandler(                     // optionalWarningHandler
                warningHandlePatterns,
                new SimpleWarningHandler() // <= Anonymous class here is complicated because the enclosing instance is
                                           // not fully initialized yet
            )
        );

        this.benchmark.report("*** JANINO - an embedded compiler for the Java(TM) programming language");
        this.benchmark.report("*** For more information visit http://janino.codehaus.org");
        this.benchmark.report("Source path",             optionalSourcePath);
        this.benchmark.report("Class path",              classPath);
        this.benchmark.report("Ext dirs",                optionalExtDirs);
        this.benchmark.report("Boot class path",         optionalBootClassPath);
        this.benchmark.report("Destination directory",   destinationDirectory);
        this.benchmark.report("Character encoding",      optionalCharacterEncoding);
        this.benchmark.report("Verbose",                 new Boolean(verbose));
        this.benchmark.report("Debug source",            new Boolean(debugSource));
        this.benchmark.report("Debug lines",             new Boolean(debugSource));
        this.benchmark.report("Debug vars",              new Boolean(debugSource));
        this.benchmark.report("Warning handle patterns", warningHandlePatterns);
        this.benchmark.report("Rebuild",                 new Boolean(rebuild));
    }

    /**
     * Backwards compatibility -- previously, "null" was officially documented.
     */
    @Nullable public static final File NO_DESTINATION_DIRECTORY = null;

    /**
     * Prints warnings to STDERR.
     */
    public static
    class SimpleWarningHandler implements WarningHandler {

        @Override public void
        handleWarning(@Nullable String handle, String message, @Nullable Location optionalLocation) {

            StringBuilder sb = new StringBuilder();

            if (optionalLocation != null) sb.append(optionalLocation).append(": ");

            if (handle == null) {
                sb.append("Warning: ");
            } else {
                sb.append("Warning ").append(handle).append(": ");
            }

            sb.append(message);

            System.err.println(sb.toString());
        }
    }

    /**
     * The default value for the <var>warningHandlerPatterns</var> parameter of {@link Compiler#Compiler(File[], File[],
     * File[], File[], File, String, boolean, boolean, boolean, boolean, StringPattern[], boolean)}.
     */
    public static final StringPattern[] DEFAULT_WARNING_HANDLE_PATTERNS = StringPattern.PATTERNS_NONE;

    /**
     * To mimic the behavior of JAVAC with a missing "-d" command line option, pass {@link #FIND_NEXT_TO_SOURCE_FILE}
     * as the <var>classFileResourceFinder</var> and {@link #CREATE_NEXT_TO_SOURCE_FILE} as the
     * <var>classFileResourceCreator</var>.
     * <p>
     *   If it is impossible to check whether an already-compiled class file exists, or if you want to enforce
     *   recompilation, pass {@link ResourceFinder#EMPTY_RESOURCE_FINDER} as the <var>classFileResourceFinder</var>.
     * </p>
     *
     * @param sourceFinder           Finds extra Java compilation units that need to be compiled (a.k.a. "-sourcepath")
     * @param iClassLoader           Loads auxiliary {@link IClass}es (a.k.a. "-classpath"), e.g. <code>new
     *                               ClassLoaderIClassLoader(ClassLoader)</code>
     * @param classFileFinder        Where to look for up-to-date class files that need not be compiled (a.k.a. "-d")
     * @param classFileCreator       Used to store generated class files (a.k.a. "-d")
     * @param optionalWarningHandler Used to issue warnings
     */
    public
    Compiler(
        ResourceFinder            sourceFinder,
        IClassLoader              iClassLoader,
        @Nullable ResourceFinder  classFileFinder,
        @Nullable ResourceCreator classFileCreator,
        @Nullable final String    optionalCharacterEncoding,
        boolean                   verbose,
        boolean                   debugSource,
        boolean                   debugLines,
        boolean                   debugVars,
        @Nullable WarningHandler  optionalWarningHandler
    ) {
        this.classFileFinder           = classFileFinder;
        this.classFileCreator          = classFileCreator;
        this.optionalCharacterEncoding = optionalCharacterEncoding;
        this.benchmark                 = new Benchmark(verbose);
        this.debugSource               = debugSource;
        this.debugLines                = debugLines;
        this.debugVars                 = debugVars;
        this.optionalWarningHandler    = optionalWarningHandler;

        // Set up the IClassLoader.
        this.iClassLoader = new CompilerIClassLoader(sourceFinder, iClassLoader);
    }

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
    public void
    setCompileErrorHandler(@Nullable ErrorHandler optionalCompileErrorHandler) {
        this.optionalCompileErrorHandler = optionalCompileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param optionalWarningHandler {@code null} to indicate that no warnings be issued
     */
    public void
    setWarningHandler(@Nullable WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    /**
     * Reads a set of Java compilation units (a.k.a. "source files") from the file system, compiles them into a set of
     * "class files" and stores these in the file system. Additional source files are parsed and compiled on demand
     * through the "source path" set of directories.
     * <p>
     *   For example, if the source path comprises the directories "A/B" and "../C", then the source file for class
     *   "com.acme.Main" is searched in
     * </p>
     * <dl>
     *   <dd>A/B/com/acme/Main.java
     *   <dd>../C/com/acme/Main.java
     * </dl>
     * <p>
     *   Notice that it does make a difference whether you pass multiple source files to {@link #compile(File[])} or if
     *   you invoke {@link #compile(File[])} multiply: In the former case, the source files may contain arbitrary
     *   references among each other (even circular ones). In the latter case, only the source files on the source path
     *   may contain circular references, not the <var>sourceFiles</var>.
     * </p>
     * <p>
     *   This method must be called exactly once after object construction.
     * </p>
     * <p>
     *   Compile errors are reported as described at {@link #setCompileErrorHandler(ErrorHandler)}.
     * </p>
     *
     * @param sourceFiles       Contain the compilation units to compile
     * @return                  {@code true} for backwards compatibility (return value can safely be ignored)
     * @throws CompileException Fatal compilation error, or the {@link CompileException} thrown be the installed
     *                          compile error handler
     * @throws IOException      Occurred when reading from the <var>sourceFiles</var>
     */
    public boolean
    compile(File[] sourceFiles) throws CompileException, IOException {
        this.benchmark.report("Source files", sourceFiles);

        Resource[] sourceFileResources = new Resource[sourceFiles.length];
        for (int i = 0; i < sourceFiles.length; ++i) sourceFileResources[i] = new FileResource(sourceFiles[i]);
        this.compile(sourceFileResources);
        return true;
    }

    /**
     * See {@link #compile(File[])}.
     *
     * @param sourceResources Contain the compilation units to compile
     * @return {@code true} for backwards compatibility (return value can safely be ignored)
     */
    public boolean
    compile(Resource[] sourceResources) throws CompileException, IOException {

        // Set up the compile error handler as described at "setCompileErrorHandler()".
        final ErrorHandler ceh = (
            this.optionalCompileErrorHandler != null
            ? this.optionalCompileErrorHandler
            : new ErrorHandler() {

                int compileErrorCount;

                @Override public void
                handleError(String message, @Nullable Location optionalLocation) throws CompileException {
                    CompileException ex = new CompileException(message, optionalLocation);
                    if (++this.compileErrorCount >= 20) throw ex;
                    System.err.println(ex.getMessage());
                }
            }
        );

        this.benchmark.beginReporting();
        try {

            // Parse all source files.
            this.parsedCompilationUnits.clear();
            for (Resource sourceResource : sourceResources) {
                Compiler.LOGGER.log(Level.FINE, "Compiling \"{0}\"", sourceResource);

                this.parsedCompilationUnits.add(new UnitCompiler(this.parseCompilationUnit(
                    sourceResource.getFileName(),                   // fileName
                    new BufferedInputStream(sourceResource.open()), // inputStream
                    this.optionalCharacterEncoding                  // optionalCharacterEncoding
                ), this.iClassLoader));
            }

            // Compile all parsed compilation units. The vector of parsed CUs may grow while they are being compiled,
            // but eventually all CUs will be compiled.
            for (int i = 0; i < this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler unitCompiler = (UnitCompiler) this.parsedCompilationUnits.get(i);

                File sourceFile;
                {
                    CompilationUnit compilationUnit = unitCompiler.getCompilationUnit();
                    if (compilationUnit.optionalFileName == null) throw new JaninoRuntimeException();
                    sourceFile = new File(compilationUnit.optionalFileName);
                }

                unitCompiler.setCompileErrorHandler(ceh);
                unitCompiler.setWarningHandler(this.optionalWarningHandler);

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
        return true;
    }

    /**
     * Reads one compilation unit from a file and parses it.
     * <p>
     *   The <var>inputStream</var> is closed before the method returns.
     * </p>
     *
     * @return the parsed compilation unit
     */
    private Java.CompilationUnit
    parseCompilationUnit(
        String           fileName,
        InputStream      inputStream,
        @Nullable String optionalCharacterEncoding
    ) throws CompileException, IOException {
        try {

            Scanner scanner = new Scanner(fileName, inputStream, optionalCharacterEncoding);

            Parser parser = new Parser(scanner);
            parser.setWarningHandler(this.optionalWarningHandler);

            this.benchmark.beginReporting("Parsing \"" + fileName + "\"");
            try {
                return parser.parseCompilationUnit();
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
     *   If <var>optionalDestinationDirectory</var> is non-null, the returned path is the
     *   <var>optionalDestinationDirectory</var> plus the package of the class (with dots replaced with file
     *   separators) plus the class name plus ".class". Example: "destdir/pkg1/pkg2/Outer$Inner.class"
     * </p>
     * <p>
     *   If <var>optionalDestinationDirectory</var> is null, the returned path is the directory of the
     *   <var>sourceFile</var> plus the class name plus ".class". Example: "srcdir/Outer$Inner.class"
     * </p>
     *
     * @param className E.g. "pkg1.pkg2.Outer$Inner"
     * @param sourceFile E.g. "srcdir/Outer.java"
     * @param optionalDestinationDirectory E.g. "destdir"
     */
    public static File
    getClassFile(String className, File sourceFile, @Nullable File optionalDestinationDirectory) {
        if (optionalDestinationDirectory != null) {
            return new File(optionalDestinationDirectory, ClassFile.getClassFileResourceName(className));
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
        if (this.classFileCreator != Compiler.CREATE_NEXT_TO_SOURCE_FILE) {
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
     * A specialized {@link IClassLoader} that loads {@link IClass}es from the following sources:
     * <ol>
     *   <li>An already-parsed compilation unit
     *   <li>A class file in the output directory (if existant and younger than source file)
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
        private final ResourceFinder sourceFinder;

        /**
         * @param sourceFinder Where to look for source files
         * @param optionalParentIClassLoader {@link IClassLoader} through which {@link IClass}es are to be loaded
         */
        CompilerIClassLoader(ResourceFinder sourceFinder, @Nullable IClassLoader optionalParentIClassLoader) {
            super(optionalParentIClassLoader);
            this.sourceFinder = sourceFinder;
            super.postConstruct();
        }

        /**
         * @param type                    field descriptor of the {@IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
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
            ResourceFinder cff = Compiler.this.classFileFinder;

            Resource classFileResource;
            if (cff != Compiler.FIND_NEXT_TO_SOURCE_FILE) {
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
         *   Notice that the CU is not compiled here!
         * </p>
         */
        private IClass
        defineIClassFromSourceResource(Resource sourceResource, String className) throws ClassNotFoundException {

            // Parse the source file.
            UnitCompiler uc;
            try {
                Java.CompilationUnit cu = Compiler.this.parseCompilationUnit(
                    sourceResource.getFileName(),                   // fileName
                    new BufferedInputStream(sourceResource.open()), // inputStream
                    Compiler.this.optionalCharacterEncoding         // optionalCharacterEncoding
                );
                uc = new UnitCompiler(cu, Compiler.this.iClassLoader);
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
