
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.jdk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.codehaus.commons.compiler.AbstractCompiler;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.jdk.util.JavaFileManagers;
import org.codehaus.commons.compiler.jdk.util.JavaFileObjects;
import org.codehaus.commons.compiler.jdk.util.JavaFileObjects.ResourceJavaFileObject;
import org.codehaus.commons.compiler.util.reflect.ApiLog;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * {@code javax.tools}-based implementation of the {@link ICompiler}.
 */
public
class Compiler extends AbstractCompiler {

    private Collection<String> compilerOptions = new ArrayList<>();

    private final JavaCompiler compiler;

    public
    Compiler() {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        if (c == null) {
            throw new RuntimeException(
                "JDK Java compiler not available - probably you're running a JRE, not a JDK",
                null
            );
        }

        this.compiler = c;
    }

    /**
     * Initializes with a <em>different</em>, {@code javax.tools.JavaCompiler}-compatible Java compiler.
     */
    public
    Compiler(JavaCompiler compiler) { this.compiler = compiler; }

    @Override public void
    setVerbose(boolean verbose) {}

    /**
     * Adds command line options that are passed unchecked to the {@link java.lang.Compiler}.
     * <p>
     *   Notice: Don't use the '-g' options - these are controlled through {@link #setDebugLines(boolean)}, {@link
     *   #setDebugVars(boolean)} and {@link #setDebugSource(boolean)}.
     * </p>
     *
     * @param compilerOptions All command line options supported by the JDK JAVAC tool
     */
    public void
    setCompilerOptions(String[] compilerOptions) { this.compilerOptions = Arrays.asList(compilerOptions); }

    @Override public void
    compile(final Resource[] sourceResources) throws CompileException, IOException {
        this.compile(sourceResources, null);
    }

    public void
    compile(final Resource[] sourceResources, @Nullable SortedSet<Location> offsets) throws CompileException, IOException {

        // Compose the effective compiler options.
        List<String> options = new ArrayList<>(this.compilerOptions);

        // Debug options.
        {
            List<String> l = new ArrayList<>();
            if (this.debugLines)  l.add("lines");
            if (this.debugSource) l.add("source");
            if (this.debugVars)   l.add("vars");
            if (l.isEmpty()) l.add("none");

            Iterator<String> it = l.iterator();
            String           o  = "-g:" + it.next();
            while (it.hasNext()) o += "," + it.next();

            options.add(o);
        }

        // Source / target version options.
        {
            if (this.sourceVersion != -1) {
                options.add("-source");
                options.add(Integer.toString(this.sourceVersion));
            }
            if (this.targetVersion != -1) {
                options.add("-target");
                options.add(Integer.toString(this.targetVersion));
            }
        }

        // Bootclasspath.
        {
            File[] bcp = this.bootClassPath;
            if (bcp != null) {
                options.add("-bootclasspath");
                options.add(Compiler.filesToPath(bcp));
            }
        }

        // Classpath.
        options.add("-classpath");
        options.add(Compiler.filesToPath(this.classPath));

        Compiler.compile(
            this.compiler,
            options,
            this.sourceFinder,
            this.sourceCharset,
            this.classFileFinder,
            this.classFileCreator,
            sourceResources,
            this.compileErrorHandler,
            this.warningHandler,
            offsets
        );
    }

    static void
    compile(
        JavaCompiler                  compiler,
        List<String>                  options,
        ResourceFinder                sourceFinder,
        Charset                       sourceFileCharset,
        ResourceFinder                classFileFinder,
        ResourceCreator               classFileCreator,
        Resource[]                    sourceFiles,
        @Nullable ErrorHandler        compileErrorHandler,
        @Nullable WarningHandler      warningHandler,
        @Nullable SortedSet<Location> offsets
    ) throws CompileException, IOException {

        // Wrap the source files in JavaFileObjects.
        Collection<JavaFileObject> sourceFileObjects = new ArrayList<>();
        for (int i = 0; i < sourceFiles.length; i++) {
            Resource sourceResource = sourceFiles[i];

            String fn        = sourceResource.getFileName();
            String className = fn.substring(fn.lastIndexOf(File.separatorChar) + 1).replace('/', '.');
            if (className.endsWith(".java")) className = className.substring(0, className.length() - 5);
            sourceFileObjects.add(JavaFileObjects.fromResource(
                sourceResource,
                className,
                Kind.SOURCE,
                sourceFileCharset
            ));
        }

        final JavaFileManager fileManager = Compiler.getJavaFileManager(
            compiler,
            sourceFinder,
            sourceFileCharset,
            classFileFinder,
            classFileCreator
        );
        try {
            Compiler.compile(
                compiler,
                options,
                sourceFileObjects,
                fileManager,
                compileErrorHandler,
                warningHandler,
                offsets
            );
        } finally {
            fileManager.close();
        }
    }

    /**
     * Creates a {@link JavaFileManager} that implements the given <var>sourceFileFinder</var>, <var>sourceFileCharset</var>,
     * <var>classFileFinder</var> and <var>classFileCreator</var>.
     */
    private static JavaFileManager
    getJavaFileManager(
        JavaCompiler    compiler,
        ResourceFinder  sourceFileFinder,
        Charset         sourceFileCharset,
        ResourceFinder  classFileFinder,
        ResourceCreator classFileCreator
    ) {

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        JavaFileManager jfm = compiler.getStandardFileManager(null, null, null);

        // Store .class file via the classFileCreator.
        jfm = JavaFileManagers.fromResourceCreator(
            jfm,
            StandardLocation.CLASS_OUTPUT,
            Kind.CLASS,
            classFileCreator,
            Charset.defaultCharset()
        );

//        classFileFinder = ResourceFinders.debugResourceFinder(classFileFinder);

        // Find existing .class files through the classFileFinder.
        jfm = JavaFileManagers.fromResourceFinder(
            jfm,
            StandardLocation.CLASS_PATH,
            Kind.CLASS,
            classFileFinder,
            Charset.defaultCharset() // irrelevant
        );

//        sourceFileFinder = ResourceFinders.debugResourceFinder(sourceFileFinder);

        // Wrap it in a file manager that finds source files through the .sourceFinder.
        jfm = JavaFileManagers.fromResourceFinder(
            jfm,
            StandardLocation.SOURCE_PATH,
            Kind.SOURCE,
            sourceFileFinder,
            sourceFileCharset
        );

        return jfm;
    }

    /**
     * Compiles on the {@link JavaFileManager} / {@link JavaFileObject} level.
     */
    static void
    compile(
        JavaCompiler                        compiler,
        List<String>                        options,
        Collection<JavaFileObject>          sourceFileObjects,
        JavaFileManager                     fileManager,
        @Nullable final ErrorHandler        compileErrorHandler,
        @Nullable final WarningHandler      warningHandler,
        @Nullable final SortedSet<Location> offsets
    ) throws CompileException, IOException {

        fileManager = (JavaFileManager) ApiLog.logMethodInvocations(fileManager);

        final int[] compileErrorCount = new int[1];

        final DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {

            @Override public void
            report(@Nullable Diagnostic<? extends JavaFileObject> diagnostic) {
                assert diagnostic != null;

                JavaFileObject source = diagnostic.getSource();
                Location       loc    = new Location(
                    (                                     // fileName
                        source == null                           ? null :
                        source instanceof ResourceJavaFileObject ? ((ResourceJavaFileObject) source).getResourceFileName() :
                        source.getName()
                    ),
                    (short) diagnostic.getLineNumber(),
                    (short) diagnostic.getColumnNumber()
                );

                // Manipulate the diagnostic location to accomodate for the "offsets" (see "addOffset(String)"):
                if (offsets != null) {
                    SortedSet<Location> hs = offsets.headSet(loc);
                    if (!hs.isEmpty()) {
                        Location co = hs.last();
                        loc = new Location(
                            co.getFileName(),
                            loc.getLineNumber() - co.getLineNumber() + 1,
                            (
                                loc.getLineNumber() == co.getLineNumber()
                                ? loc.getColumnNumber() - co.getColumnNumber() + 1
                                : loc.getColumnNumber()
                            )
                        );
                    }
                }

                String message = diagnostic.getMessage(null) + " (" + diagnostic.getCode() + ")";

                try {
                    switch (diagnostic.getKind()) {

                    case ERROR:
                        compileErrorCount[0]++;

                        if (compileErrorHandler == null) throw new CompileException(message, loc);

                        compileErrorHandler.handleError(diagnostic.toString(), loc);
                        break;

                    case MANDATORY_WARNING:
                    case WARNING:
                        if (warningHandler != null) warningHandler.handleWarning(null, message, loc);
                        break;

                    case NOTE:
                    case OTHER:
                    default:
                        break;
                    }
                } catch (CompileException ce) {

                    // Wrap the CompileException in a RuntimeException in order to "tunnel" it through the JAVAC
                    // error handling.
                    //
                    // Unfortunately this does not work in VERY specific circumstances, namely test case
                    // "org.codehaus.commons.compiler.tests.JlsTest.test_9_3_1__Initialization_of_Fields_in_Interfaces__2()".
                    // The reason being is that "com.sun.tools.javac.api.ClientCodeWrapper.WrappedDiagnosticListener.report(Diagnostic<? extends T>)"
                    // wraps the RuntimeException in a com.sun.tools.javac.util.ClientCodeException, and
                    // "com.sun.tools.javac.code.Symbol.VarSymbol.getConstValue()" catches that and throws an
                    // AssertionError, which leads to a stack trace on STDERR.
                    //
                    // There is no obvious way to fix this.
                    // This problem exists for (at least) JAVA 7, 8, 11 an 17.
                    throw new RuntimeException(ce);
                }
            }
        };

        // Run the compiler.
        try {
            if (!compiler.getTask(
                null,             // out
                fileManager,      // fileManager
                dl,               // diagnosticListener
                options,          // options
                null,             // classes
                sourceFileObjects // compilationUnits
            ).call() && compileErrorCount[0] == 0) throw new CompileException("Compilation failed for an unknown reason", null);
        } catch (RuntimeException rte) {

            // Unwrap the compilation exception and throw it.
            for (Throwable t = rte.getCause(); t != null; t = t.getCause()) {
                if (t instanceof CompileException) {
                    throw (CompileException) t; // SUPPRESS CHECKSTYLE AvoidHidingCause
                }
                if (t instanceof IOException) {
                    throw (IOException) t; // SUPPRESS CHECKSTYLE AvoidHidingCause
                }
            }
            throw rte;
        }

        if (compileErrorCount[0] > 0) {
            throw new CompileException("Compilation failed with " + compileErrorCount[0] + " errors", null);
        }
    }

    private static String
    filesToPath(File[] files) {
        StringBuilder sb = new StringBuilder();
        for (File cpe : files) {
            if (sb.length() > 0) sb.append(File.pathSeparatorChar);
            sb.append(cpe.getPath());
        }
        return sb.toString();
    }
}
