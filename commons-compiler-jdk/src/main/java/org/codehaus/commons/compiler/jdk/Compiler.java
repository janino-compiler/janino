
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
import org.codehaus.commons.compiler.util.reflect.ApiLog;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * JDK-base implementation of the {@link ICompiler}.
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

        Collection<JavaFileObject> sourceFileObjects = new ArrayList<>();
        for (int i = 0; i < sourceResources.length; i++) {
            Resource sourceResource = sourceResources[i];

            String fn        = sourceResource.getFileName();
            String className = fn.substring(fn.lastIndexOf(File.separatorChar) + 1, fn.length() - 5).replace('/', '.');
            sourceFileObjects.add(JavaFileObjects.fromResource(
                sourceResource,
                className,           // className
                Kind.SOURCE,
                this.sourceCharset
            ));
        }

        final int[] compileErrorCount = new int[1];
        final DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {

            @Override public void
            report(@Nullable Diagnostic<? extends JavaFileObject> diagnostic) {
                assert diagnostic != null;

            	JavaFileObject source = diagnostic.getSource();
				Location loc = new Location(
        			source != null ? source.toUri().getPath() : null, // fileName
        			(short) diagnostic.getLineNumber(),
        			(short) diagnostic.getColumnNumber()
    			);

                String message = diagnostic.getMessage(null) + " (" + diagnostic.getCode() + ")";

            	try {
                    switch (diagnostic.getKind()) {

                    case ERROR:
                    	compileErrorCount[0]++;

                    	ErrorHandler eh = Compiler.this.compileErrorHandler;

                    	if (eh == null) throw new CompileException(message, loc);

                    	eh.handleError(diagnostic.toString(), loc);
                        break;

                    case MANDATORY_WARNING:
                    case WARNING:
                        WarningHandler wh = Compiler.this.warningHandler;
                        if (wh != null) wh.handleWarning(null, message, loc);
                        break;

                    case NOTE:
                    case OTHER:
                    default:
                        break;
                    }
                } catch (CompileException ce) {
                    throw new RuntimeException(ce);
                }
            }
        };

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

        JavaFileManager fileManager = this.getJavaFileManager();
        try {

	        fileManager = (JavaFileManager) ApiLog.logMethodInvocations(fileManager);

	        // Run the compiler.
	        try {
	            if (!this.compiler.getTask(
	                null,             // out
	                fileManager,      // fileManager
	                dl,               // diagnosticListener
	                options,          // options
	                null,             // classes
	                sourceFileObjects // compilationUnits
	            ).call() || compileErrorCount[0] > 0) {
	                throw new CompileException("Compilation failed with " + compileErrorCount[0] + " errors", null);
	            }
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
        } finally {
            fileManager.close();
        }
    }

    /**
     * Creates the underlying {@link JavaFileManager} lazily, because {@link #setSourcePath(File[])} and consorts are
     * called <em>after</em> initialization.
     */
    private JavaFileManager
    getJavaFileManager() {

        if (this.fileManagerEnn != null) return this.fileManagerEnn;

        return (this.fileManagerEnn = this.getJavaFileManager2());
    }
    @Nullable private JavaFileManager fileManagerEnn;

    private JavaFileManager
    getJavaFileManager2() {

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        JavaFileManager jfm = this.compiler.getStandardFileManager(null, null, null);

        // Store .class file via the classFileCreator.
        jfm = JavaFileManagers.fromResourceCreator(
            jfm,
            StandardLocation.CLASS_OUTPUT,
            Kind.CLASS,
            this.classFileCreator,
            Charset.defaultCharset()
        );

        // Find existing .class files through the classFileFinder.
        jfm = JavaFileManagers.fromResourceFinder(
            jfm,
            StandardLocation.CLASS_PATH,
            Kind.CLASS,
            this.classFileFinder,
            Charset.defaultCharset() // irrelevant
        );

        // Wrap it in a file manager that finds source files through the .sourceFinder.
        jfm = JavaFileManagers.fromResourceFinder(
            jfm,
            StandardLocation.SOURCE_PATH,
            Kind.SOURCE,
            this.sourceFinder,
            this.sourceCharset
        );

        return (this.fileManagerEnn = jfm);
    }
}
