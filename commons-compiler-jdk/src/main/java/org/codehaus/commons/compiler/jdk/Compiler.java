
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
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.jdk.JavaSourceClassLoader.DiagnosticException;
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

    private static final JavaCompiler SYSTEM_JAVA_COMPILER = Compiler.getSystemJavaCompiler();

    private Collection<String> compilerOptions = new ArrayList<String>();

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

        Collection<JavaFileObject> sourceFileObjects = new ArrayList<JavaFileObject>();
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

        // Compose the effective compiler options.
        List<String> options = new ArrayList<String>(this.compilerOptions);
        {
            List<String> l = new ArrayList<String>();
            if (this.debugLines)  l.add("lines");
            if (this.debugSource) l.add("source");
            if (this.debugVars)   l.add("vars");
            if (l.isEmpty()) l.add("none");

            Iterator<String> it = l.iterator();
            String           o  = "-g:" + it.next();
            while (it.hasNext()) o += "," + it.next();

            options.add(o);
        }

        JavaFileManager fileManager = this.getJavaFileManager();

        fileManager = (JavaFileManager) ApiLog.logMethodInvocations(fileManager);

        // Run the compiler.
        try {
            if (!Compiler.SYSTEM_JAVA_COMPILER.getTask(
                null,                                      // out
                fileManager,                               // fileManager
                new DiagnosticListener<JavaFileObject>() { // diagnosticListener

                    @Override public void
                    report(@Nullable final Diagnostic<? extends JavaFileObject> diagnostic) {
                        assert diagnostic != null;

                        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                            throw new DiagnosticException(diagnostic);
                        }
                    }
                },
                options,                                   // options
                null,                                      // classes
                sourceFileObjects                          // compilationUnits
            ).call()) throw new CompileException("Compilation failed", null);
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            if (cause instanceof DiagnosticException) {
                final CompileException ce = new CompileException(((DiagnosticException) cause).getMessage(), null);
                ce.initCause(re);
                throw ce;
            }
            throw re;
        }
    }

    private static JavaCompiler
    getSystemJavaCompiler() {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        if (c == null) {
            throw new UnsupportedOperationException(
                "JDK Java compiler not available - probably you're running a JRE, not a JDK"
            );
        }
        return c;
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
        JavaFileManager jfm = Compiler.SYSTEM_JAVA_COMPILER.getStandardFileManager(null, null, null);

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
