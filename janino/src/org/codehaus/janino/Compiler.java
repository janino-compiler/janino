
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

import java.util.*;
import java.io.*;

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.resource.*;


/**
 * A simplified substitute for the <tt>javac</tt> tool.
 *
 * Usage:
 * <pre>
 * java org.codehaus.janino.Compiler \
 *           [ -d <i>destination-dir</i> ] \
 *           [ -sourcepath <i>dirlist</i> ] \
 *           [ -classpath <i>dirlist</i> ] \
 *           [ -extdirs <i>dirlist</i> ] \
 *           [ -bootclasspath <i>dirlist</i> ] \
 *           [ -encoding <i>encoding</i> ] \
 *           [ -verbose ] \
 *           [ -g:none ] \
 *           [ -g:{lines,vars,source} ] \
 *           [ -warn:<i>pattern-list</i> ] \
 *           <i>source-file</i> ...
 * java org.codehaus.janino.Compiler -help
 * </pre>
 */
public class Compiler {
    private static final boolean DEBUG = false;

    /**
     * Command line interface.
     */
    public static void main(String[] args) {
        File            optionalDestinationDirectory = null;
        File[]          optionalSourcePath = null;
        File[]          classPath = new File[] { new File(".") };
        File[]          optionalExtDirs = null;
        File[]          optionalBootClassPath = null;
        String          optionalCharacterEncoding = null;
        boolean         verbose = false;
        EnumeratorSet   debuggingInformation = DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION;
        StringPattern[] optionalWarningHandlePatterns = null;
        boolean         rebuild = false;

        // Process command line options.
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-') break;
            if (arg.equals("-d")) {
                optionalDestinationDirectory = new File(args[++i]);
            } else
            if (arg.equals("-sourcepath")) {
                optionalSourcePath = PathResourceFinder.parsePath(args[++i]);
            } else
            if (arg.equals("-classpath")) {
                classPath = PathResourceFinder.parsePath(args[++i]);
            } else
            if (arg.equals("-extdirs")) {
                optionalExtDirs = PathResourceFinder.parsePath(args[++i]);
            } else
            if (arg.equals("-bootclasspath")) {
                optionalBootClassPath = PathResourceFinder.parsePath(args[++i]);
            } else
            if (arg.equals("-encoding")) {
                optionalCharacterEncoding = args[++i];
            } else
            if (arg.equals("-verbose")) {
                verbose = true;
            } else
            if (arg.equals("-g")) {
                debuggingInformation = DebuggingInformation.ALL;
            } else
            if (arg.startsWith("-g:")) {
                try {
                    debuggingInformation = new EnumeratorSet(DebuggingInformation.class, arg.substring(3));
                } catch (EnumeratorFormatException ex) {
                    System.err.println("Invalid debugging option \"" + arg + "\", only \"" + DebuggingInformation.ALL + "\" allowed");
                    System.exit(1);
                }
            } else
            if (arg.startsWith("-warn:")) {
                optionalWarningHandlePatterns = StringPattern.parseCombinedPattern(arg.substring(6));
            } else
            if (arg.equals("-rebuild")) {
                rebuild = true;
            } else
            if (arg.equals("-help")) {
                for (int j = 0; j < Compiler.USAGE.length; ++j) System.out.println(Compiler.USAGE[j]);
                System.exit(1);
            } else {
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
            optionalDestinationDirectory,
            optionalCharacterEncoding,
            verbose,
            debuggingInformation,
            optionalWarningHandlePatterns,
            rebuild
        );

        // Compile source files.
        try {
            if (!compiler.compile(sourceFiles)) System.exit(1);
        } catch (Exception e) {
            System.err.println(e.toString());
            System.exit(1);
        }
    }

    private static final String[] USAGE = {
        "Usage:",
        "",
        "  java org.codehaus.janino.Compiler [ <option> ] ... <source-file> ...",
        "",
        "Supported <option>s are:",
        "  -d <output-dir>           Where to save class files",
        "  -sourcepath <dirlist>     Where to look for other source files",
        "  -classpath <dirlist>      Where to look for other class files",
        "  -extdirs <dirlist>        Where to look for other class files",
        "  -bootclasspath <dirlist>  Where to look for other class files",
        "  -encoding <encoding>      Encoding of source files, e.g. \"UTF-8\" or \"ISO-8859-1\"",
        "  -verbose",
        "  -g                        Generate all debugging info",
        "  -g:none                   Generate no debugging info",
        "  -g:{lines,vars,source}    Generate only some debugging info",
        "  -warn:<pattern-list>      Issue certain warnings; examples:",
        "    -warn:*                 Enables all warnings",
        "    -warn:IASF              Only warn against implicit access to static fields",
        "    -warn:*-IASF            Enables all warnings, except those against implicit",
        "                            access to static fields",
        "    -warn:*-IA*+IASF        Enables all warnings, except those against implicit",
        "                            accesses, but do warn against implicit access to",
        "                            static fields",
        "  -rebuild                  Compile all source files, even if the class files",
        "                            seems up-to-date",
        "  -help",
        "",
        "The default encoding in this environment is \"" + new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding() + "\".",
    };

    private /*final*/ ResourceFinder  optionalClassFileResourceFinder; // null == finds existing class files next to source file
    private /*final*/ ResourceCreator optionalClassFileResourceCreator; // null == create class file next to source file
    private /*final*/ String          optionalCharacterEncoding;
    private /*final*/ Benchmark       benchmark;
    private /*final*/ EnumeratorSet   debuggingInformation;
    private /*final*/ WarningHandler  warningHandler;

    private /*final*/ IClassLoader iClassLoader;
    private final ArrayList    parsedCompilationUnits = new ArrayList(); // UnitCompiler
    private boolean            storingClassFiles = true;

    /**
     * Initialize a Java<sup>TM</sup> compiler with the given parameters.
     * <p>
     * Classes are searched in the following order:
     * <ul>
     *   <li>If <code>optionalBootClassPath</code> is <code>null</code>:
     *   <ul>
     *     <li>Through the system class loader of the JVM that runs JANINO
     *   </ul>
     *   <li>If <code>optionalBootClassPath</code> is not <code>null</code>:
     *   <ul>
     *     <li>Through the <code>optionalBootClassPath</code>
     *   </ul>
     *   <li>If <code>optionalExtDirs</code> is not <code>null</code>:
     *   <ul>
     *     <li>Through the <code>optionalExtDirs</code>
     *   </ul>
     *   <li>Through the <code>classPath</code>
     *   <li>If <code>optionalSourcePath</code> is <code>null</code>:
     *   <ul>
     *     <li>Through source files found on the <code>classPath</code>
     *   </ul>
     *   <li>If <code>optionalSourcePath</code> is not <code>null</code>:
     *   <ul>
     *     <li>Through source files found on the <code>sourcePath</code>
     *   </ul>
     * </ul>
     * <p>
     * The file name of a class file that represents class "pkg.Example"
     * is determined as follows:
     * <ul>
     *   <li>
     *   If <code>optionalDestinationDirectory</code> is not <code>null</code>:
     *   <code><i>optionalDestinationDirectory</i>/pkg/Example.class</code>
     *   <li>
     *   If <code>optionalDestinationDirectory</code> is <code>null</code>:
     *   <code>dir1/dir2/Example.class</code> (Assuming that the file name of the
     *   source file that declares the class was
     *   <code>dir1/dir2/Any.java</code>.)
     * </ul>
     */
    public Compiler(
        final File[]    optionalSourcePath,
        final File[]    classPath,
        final File[]    optionalExtDirs,
        final File[]    optionalBootClassPath,
        final File      optionalDestinationDirectory,
        final String    optionalCharacterEncoding,
        boolean         verbose,
        EnumeratorSet   debuggingInformation,
        StringPattern[] optionalWarningHandlePatterns,
        boolean         rebuild
    ) {
        this(
            new PathResourceFinder(                   // sourceFinder
                optionalSourcePath == null ? classPath : optionalSourcePath
            ),
            Compiler.createJavacLikePathIClassLoader( // iClassLoader
                optionalBootClassPath,
                optionalExtDirs,
                classPath
            ),
            (                                         // optionalClassFileResourceFinder
                rebuild ? ResourceFinder.NO_RESOURCE_FINDER :
                optionalDestinationDirectory == null ? null :
                new DirectoryResourceFinder(optionalDestinationDirectory)
            ),
            (                                         // optionalClassFileResourceCreator
                optionalDestinationDirectory == null ? null :
                new DirectoryResourceCreator(optionalDestinationDirectory)
            ),
            optionalCharacterEncoding,                // optionalCharacterEncoding
            verbose,                                  // verbose
            debuggingInformation,                     // debuggingInformation
            new FilterWarningHandler(                 // warningHandler
                optionalWarningHandlePatterns,
                new SimpleWarningHandler() // <= Anonymous class here is complicated because the enclosing instance is not fully initialized yet 
            )
        );

        this.benchmark.report("*** JANINO - an embedded compiler for the Java(TM) programming language");
        this.benchmark.report("*** For more information visit http://janino.codehaus.org");
        this.benchmark.report("Source path",             optionalSourcePath           );
        this.benchmark.report("Class path",              classPath                    );
        this.benchmark.report("Ext dirs",                optionalExtDirs              );
        this.benchmark.report("Boot class path",         optionalBootClassPath        );
        this.benchmark.report("Destination directory",   optionalDestinationDirectory );
        this.benchmark.report("Character encoding",      optionalCharacterEncoding    );
        this.benchmark.report("Verbose",                 new Boolean(verbose)         );
        this.benchmark.report("Debugging information",   debuggingInformation         );
        this.benchmark.report("Warning handle patterns", optionalWarningHandlePatterns);
        this.benchmark.report("Rebuild",                 new Boolean(rebuild)         );
    }
    static class SimpleWarningHandler implements WarningHandler {
        public void handleWarning(String handle, String message, Location optionalLocation) {
            StringBuffer sb = new StringBuffer();
            if (optionalLocation != null) sb.append(optionalLocation).append(": ");
            sb.append("Warning ").append(handle).append(": ").append(message);
            System.err.println(sb.toString());
        }
    }

    /**
     * To mimic the behavior of JAVAC with a missing "-d" command line option,
     * pass <code>optionalClassFileResourceFinder</code> and
     * <code>optionalClassFileResourceCreator</code> as <code>null</code>.
     * <p>
     * If it is impossible to check whether an already-compiled class file
     * exists, or if you want to enforce recompilation, pass
     * {@link ResourceFinder#NO_RESOURCE_FINDER} as the
     * <code>optionalClassFileResourceFinder</code>.
     * 
     * @param sourceFinder Finds extra Java compilation units that need to be compiled (a.k.a. "sourcepath")
     * @param iClassLoader loads auxiliary {@link IClass}es; e.g. <code>new ClassLoaderIClassLoader(ClassLoader)</code>
     * @param optionalClassFileResourceFinder Where to look for up-to-date class files that need not be compiled
     * @param optionalClassFileResourceCreator Used to store generated class files
     * @param optionalCharacterEncoding
     * @param verbose
     * @param debuggingInformation a combination of <code>Java.DEBUGGING_...</code>
     * @param warningHandler used to issue warnings
     */
    public Compiler(
        ResourceFinder  sourceFinder,
        IClassLoader    iClassLoader,
        ResourceFinder  optionalClassFileResourceFinder,
        ResourceCreator optionalClassFileResourceCreator,
        final String    optionalCharacterEncoding,
        boolean         verbose,
        EnumeratorSet   debuggingInformation,
        WarningHandler  warningHandler
    ) {
    	this.optionalClassFileResourceFinder  = optionalClassFileResourceFinder;
    	this.optionalClassFileResourceCreator = optionalClassFileResourceCreator;
        this.optionalCharacterEncoding        = optionalCharacterEncoding;
        this.benchmark                        = new Benchmark(verbose);
        this.debuggingInformation             = debuggingInformation;
        this.warningHandler                   = warningHandler;

        // Set up the IClassLoader.
        this.iClassLoader = new CompilerIClassLoader(
            sourceFinder,
            iClassLoader
        );
    }

    /**
     * Create an {@link IClassLoader} that looks for classes in the given "boot class
     * path", then in the given "extension directories", and then in the given
     * "class path".
     * <p>
     * The default for the <code>optionalBootClassPath</code> is the path defined in
     * the system property "sun.boot.class.path", and the default for the
     * <code>optionalExtensionDirs</code> is the path defined in the "java.ext.dirs"
     * system property.
     */
    private static IClassLoader createJavacLikePathIClassLoader(
        final File[] optionalBootClassPath,
        final File[] optionalExtDirs,
        final File[] classPath
    ) {
        ResourceFinder bootClassPathResourceFinder = new PathResourceFinder(
            optionalBootClassPath == null ?
            PathResourceFinder.parsePath(System.getProperty("sun.boot.class.path")):
            optionalBootClassPath
        );
        ResourceFinder extensionDirectoriesResourceFinder = new JarDirectoriesResourceFinder(
            optionalExtDirs == null ?
            PathResourceFinder.parsePath(System.getProperty("java.ext.dirs")):
            optionalExtDirs
        );
        ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);

        // We can load classes through "ResourceFinderIClassLoader"s, which means
        // they are read into "ClassFile" objects, or we can load classes through
        // "ClassLoaderIClassLoader"s, which means they are loaded into the JVM.
        //
        // In my environment, the latter is slightly faster. No figures about
        // resource usage yet.
        //
        // In applications where the generated classes are not loaded into the
        // same JVM instance, we should avoid to use the
        // ClassLoaderIClassLoader, because that assumes that final fields have
        // a constant value, even if not compile-time-constant but only
        // initialization-time constant. The classical example is
        // "File.separator", which is non-blank final, but not compile-time-
        // constant.
        if (true) {
            IClassLoader icl;
            icl = new ResourceFinderIClassLoader(bootClassPathResourceFinder, null);
            icl = new ResourceFinderIClassLoader(extensionDirectoriesResourceFinder, icl);
            icl = new ResourceFinderIClassLoader(classPathResourceFinder, icl);
            return icl;
        } else {
            ClassLoader cl;

            // This is the only way to instantiate a "bootstrap" class loader, i.e. a class loader
            // that finds the bootstrap classes like "Object", "String" and "Throwable", but not
            // the classes on the JVM's class path.
            cl = new ClassLoader(null) {};
            cl = new ResourceFinderClassLoader(bootClassPathResourceFinder, cl);
            cl = new ResourceFinderClassLoader(extensionDirectoriesResourceFinder, cl);
            cl = new ResourceFinderClassLoader(classPathResourceFinder, cl);

            return new ClassLoaderIClassLoader(cl);
        }
    }

    /**
     * When called with a <code>false</code> argument, then compilation
     * is not terminated, but generated class files are no longer
     * stored in files.
     */
    public void setStoringClassFiles(boolean storingClassFiles) {
        this.storingClassFiles = storingClassFiles;
    }

    /**
     * Reads a set of Java<sup>TM</sup> compilation units (a.k.a. "source
     * files") from the file system, compiles them into a set of "class
     * files" and stores these in the file system. Additional source files are
     * parsed and compiled on demand through the "source path" set of
     * directories.
     * <p>
     * For example, if the source path comprises the directories "A/B" and "../C",
     * then the source file for class "com.acme.Main" is searched in
     * <dl>
     *   <dd>A/B/com/acme/Main.java
     *   <dd>../C/com/acme/Main.java
     * </dl>
     * Notice that it does make a difference whether you pass multiple source
     * files to {@link #compile(File[])} or if you invoke
     * {@link #compile(File[])} multiply: In the former case, the source
     * files may contain arbitrary references among each other (even circular
     * ones). In the latter case, only the source files on the source path
     * may contain circular references, not the <code>sourceFiles</code>.
     *
     * @return <code>false</code> if compile errors have occurred
     */
    public boolean compile(File[] sourceFiles)
    throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        Resource[] sourceFileResources = new Resource[sourceFiles.length];
        for (int i = 0; i < sourceFiles.length; ++i) sourceFileResources[i] = new FileResource(sourceFiles[i]);
        return this.compile(sourceFileResources);
    }

    /**
     * See {@link #compile(File[])}.
     * 
     * @param sourceResources Contain the compilation units to compile
     * @return <code>false</code> if compile errors have occurred
     */
    public boolean compile(Resource[] sourceResources)
    throws Scanner.ScanException, Parser.ParseException, CompileException, IOException {
        this.benchmark.report("Source files", sourceResources);

        // Set up a custom error handler that reports compile errors on "System.err".
        final int[] compileExceptionCount = new int[1];
        UnitCompiler.ErrorHandler compileErrorHandler = new UnitCompiler.ErrorHandler() {
            public void handleError(String message, Location optionalLocation) throws CompileException {
                StringBuffer sb = new StringBuffer();
                if (optionalLocation != null) sb.append(optionalLocation).append(": ");
                sb.append("Error: ").append(message);
                System.err.println(sb.toString());
                Compiler.this.setStoringClassFiles(false);
                if (++compileExceptionCount[0] >= 20) throw new CompileException("Too many compile errors", null);
            }
        };

        this.benchmark.beginReporting();
        try {

            // Parse all source files.
            this.parsedCompilationUnits.clear();
            for (int i = 0; i < sourceResources.length; ++i) {
                if (Compiler.DEBUG) System.out.println("Compiling \"" + sourceResources[i] + "\"");
                this.parsedCompilationUnits.add(new UnitCompiler(this.parseCompilationUnit(
                    sourceResources[i].getFileName(),                   // fileName
                    new BufferedInputStream(sourceResources[i].open()), // inputStream
                    this.optionalCharacterEncoding                          // optionalCharacterEncoding
                ), this.iClassLoader));
            }
    
            // Compile all parsed compilation units. The vector of parsed CUs may
            // grow while they are being compiled, but eventually all CUs will
            // be compiled.
            for (int i = 0; i < this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler unitCompiler = (UnitCompiler) this.parsedCompilationUnits.get(i);
                Java.CompilationUnit cu = unitCompiler.compilationUnit;
                if (cu.optionalFileName == null) throw new RuntimeException();
                File sourceFile = new File(cu.optionalFileName);

                unitCompiler.setCompileErrorHandler(compileErrorHandler);
                unitCompiler.setWarningHandler(this.warningHandler);

                this.benchmark.beginReporting("Compiling compilation unit \"" + sourceFile + "\"");
                ClassFile[] classFiles;
                try {
    
                    // Compile the compilation unit.
                    try {
                        classFiles = unitCompiler.compileUnit(this.debuggingInformation);
                    } catch (TunnelException ex) {
                        Throwable t = ex.getDelegate();
                        if (t instanceof Scanner.ScanException) throw (Scanner.ScanException) t;
                        if (t instanceof Parser.ParseException) throw (Parser.ParseException) t;
                        if (t instanceof IOException          ) throw (IOException          ) t;
                        throw ex;
                    }
                } finally {
                    this.benchmark.endReporting();
                }
    
                // Store the compiled classes and interfaces into class files.
                if (this.storingClassFiles) {
                    this.benchmark.beginReporting("Storing " + classFiles.length + " class file(s) resulting from compilation unit \"" + sourceFile + "\"");
                    try {
                        for (int j = 0; j < classFiles.length; ++j) {
                            this.storeClassFile(classFiles[j], sourceFile);
                        }
                    } finally {
                        this.benchmark.endReporting();
                    }
                } else {
                    this.benchmark.report("Not storing " + classFiles.length + " class files resulting from compilation unit \"" + sourceFile + "\"");
                }
            }
        } finally {
            this.benchmark.endReporting("Compiled " + this.parsedCompilationUnits.size() + " compilation unit(s)");
        }

        return compileExceptionCount[0] == 0;
    }

    /**
     * Read one compilation unit from a file and parse it.
     * <p>
     * The <code>inputStream</code> is closed before the method returns.
     * @return the parsed compilation unit
     */
    /*private*/ Java.CompilationUnit parseCompilationUnit(
        String      fileName,
        InputStream inputStream,
        String      optionalCharacterEncoding
    ) throws Scanner.ScanException, Parser.ParseException, IOException {
        Scanner scanner = new Scanner(fileName, inputStream, optionalCharacterEncoding);
        try {
            Parser parser = new Parser(scanner);
            parser.setWarningHandler(this.warningHandler);

            this.benchmark.beginReporting("Parsing \"" + fileName + "\"");
            try {
                return parser.parseCompilationUnit();
            } finally {
                this.benchmark.endReporting();
            }
        } finally {

            // This closes the "inputStream".
            scanner.close();
        }
    }

    /**
     * Construct the name of a file that could store the byte code of the class with the given
     * name.
     * <p>
     * If <code>optionalDestinationDirectory</code> is non-null, the returned path is the
     * <code>optionalDestinationDirectory</code> plus the package of the class (with dots replaced
     * with file separators) plus the class name plus ".class". Example:
     * "destdir/pkg1/pkg2/Outer$Inner.class"
     * <p>
     * If <code>optionalDestinationDirectory</code> is null, the returned path is the
     * directory of the <code>sourceFile</code> plus the class name plus ".class". Example:
     * "srcdir/Outer$Inner.class"
     * @param className E.g. "pkg1.pkg2.Outer$Inner"
     * @param sourceFile E.g. "srcdir/Outer.java"
     * @param optionalDestinationDirectory E.g. "destdir"
     */
    public static File getClassFile(String className, File sourceFile, File optionalDestinationDirectory) {
        if (optionalDestinationDirectory != null) {
            return new File(optionalDestinationDirectory, ClassFile.getClassFileResourceName(className));
        } else {
            int idx = className.lastIndexOf('.');
            return new File(sourceFile.getParentFile(), ClassFile.getClassFileResourceName(className.substring(idx + 1)));
        }
    }

    /**
     * Store the byte code of this {@link ClassFile} in the file system. Directories are created
     * as necessary.
     * @param classFile
     * @param sourceFile Required to compute class file path if no destination directory given
     */
    private void storeClassFile(ClassFile classFile, final File sourceFile) throws IOException {
    	String classFileResourceName = ClassFile.getClassFileResourceName(classFile.getThisClassName());

        // Determine where to create the class file.
    	ResourceCreator rc;
    	if (this.optionalClassFileResourceCreator != null) {
    		rc = this.optionalClassFileResourceCreator;
    	} else {

            // If the JAVAC option "-d" is given, place the class file next
            // to the source file, irrespective of the package name.
            rc = new FileResourceCreator() {
				protected File getFile(String resourceName) {
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
        } catch (IOException ex) {
            try { os.close(); } catch (IOException e) {}
            os = null;
            if (!rc.deleteResource(classFileResourceName)) throw new IOException("Could not delete incompletely written class file \"" + classFileResourceName + "\"");
            throw ex;
        } finally {
            if (os != null) try { os.close(); } catch (IOException e) {}
        }
    }

    /**
     * A specialized {@link IClassLoader} that loads {@link IClass}es from the following
     * sources:
     * <ol>
     *   <li>An already-parsed compilation unit
     *   <li>A class file in the output directory (if existant and younger than source file)
     *   <li>A source file in any of the source path directories
     *   <li>The parent class loader
     * </ol>
     * Notice that the {@link CompilerIClassLoader} is an inner class of {@link Compiler} and
     * heavily uses {@link Compiler}'s members.
     */
    private class CompilerIClassLoader extends IClassLoader {
        private final ResourceFinder sourceFinder;

        /**
         * @param sourceFinder Where to look for source files
         * @param optionalParentIClassLoader {@link IClassLoader} through which {@link IClass}es are to be loaded
         */
        public CompilerIClassLoader(
            ResourceFinder sourceFinder,
            IClassLoader   optionalParentIClassLoader
        ) {
            super(optionalParentIClassLoader);
            this.sourceFinder = sourceFinder;
            super.postConstruct();
        }

        /**
         * @param type field descriptor of the {@IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
         * @throws TunnelException wraps a {@link Scanner.ScanException}
         * @throws TunnelException wraps a {@link Parser.ParseException}
         * @throws TunnelException wraps a {@link IOException}
         */
        protected IClass findIClass(final String type) throws TunnelException {
            if (Compiler.DEBUG) System.out.println("type = " + type);

            // Class type.
            String className = Descriptor.toClassName(type); // E.g. "pkg1.pkg2.Outer$Inner"
            if (Compiler.DEBUG) System.out.println("2 className = \"" + className + "\"");

            // Do not attempt to load classes from package "java".
            if (className.startsWith("java.")) return null;

            // Check the already-parsed compilation units.
            for (int i = 0; i < Compiler.this.parsedCompilationUnits.size(); ++i) {
                UnitCompiler uc = (UnitCompiler) Compiler.this.parsedCompilationUnits.get(i);
                IClass res = uc.findClass(className);
                if (res != null) {
                    this.defineIClass(res);
                    return res;
                } 
            }

            // Search source path for uncompiled class.
            final Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
            if (sourceResource == null) return null;

            // Find an existing class file.
            Resource classFileResource;
            if (Compiler.this.optionalClassFileResourceFinder != null) {
                classFileResource = Compiler.this.optionalClassFileResourceFinder.findResource(ClassFile.getClassFileResourceName(className));
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
         * Parse the compilation unit stored in the given <code>sourceResource</code>, remember it in
         * <code>Compiler.this.parsedCompilationUnits</code> (it may declare other classes that
         * are needed later), find the declaration of the type with the given
         * <code>className</code>, and define it in the {@link IClassLoader}.
         * <p>
         * Notice that the CU is not compiled here!
         */
        private IClass defineIClassFromSourceResource(
            Resource sourceResource,
            String                  className
        ) throws TunnelException {

            // Parse the source file.
            Java.CompilationUnit cu;
            try {
                cu = Compiler.this.parseCompilationUnit(
                    sourceResource.getFileName(),                   // fileName
                    new BufferedInputStream(sourceResource.open()), // inputStream
                    Compiler.this.optionalCharacterEncoding         // optionalCharacterEncoding
                );
            } catch (IOException ex) {
                throw new TunnelException(ex);
            } catch (Parser.ParseException ex) {
                throw new TunnelException(ex);
            } catch (Scanner.ScanException ex) {
                throw new TunnelException(ex);
            }

            // Remember compilation unit for later compilation.
            UnitCompiler uc = new UnitCompiler(cu, Compiler.this.iClassLoader);
            Compiler.this.parsedCompilationUnits.add(uc);

            // Define the class.
            IClass res = uc.findClass(className);
            if (res == null) {

                // This is a really complicated case: We may find a source file on the source
                // path that seemingly contains the declaration of the class we are looking
                // for, but doesn't. This is possible if the underlying file system has
                // case-insensitive file names and/or file names that are limited in length
                // (e.g. DOS 8.3).
                return null;
            }
            this.defineIClass(res);
            return res;
        }

        /**
         * Open the given <code>classFileResource</code>, read its contents, define it in the
         * {@link IClassLoader}, and resolve it (this step may involve loading more classes).
         */
        private IClass defineIClassFromClassFileResource(Resource classFileResource) {
            Compiler.this.benchmark.beginReporting("Loading class file \"" + classFileResource.getFileName() + "\"");
            try {
                InputStream is = null;
                ClassFile cf;
                try {
                    cf = new ClassFile(new BufferedInputStream(classFileResource.open()));
                } catch (IOException ex) {
                    throw new TunnelException(ex);
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
                try {
                    result.resolveAllClasses();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not resolve class \"" + e.getMessage() + "\"");
                }

                return result;
            } finally {
                Compiler.this.benchmark.endReporting();
            }
        }
    }
}


