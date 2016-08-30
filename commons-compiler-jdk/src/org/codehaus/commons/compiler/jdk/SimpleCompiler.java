
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.jdk;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * The JDK-based implementation of {@link ISimpleCompiler}.
 */
public
class SimpleCompiler extends Cookable implements ISimpleCompiler {

    private ClassLoader              parentClassLoader = Thread.currentThread().getContextClassLoader();
    @Nullable private ClassLoader    result;
    private boolean                  debugSource;
    private boolean                  debugLines;
    private boolean                  debugVars;
    @Nullable private ErrorHandler   optionalCompileErrorHandler;
    @Nullable private WarningHandler optionalWarningHandler;

    /**
     * @throws IllegalStateException This {@link Cookable} is not yet cooked
     */
    @Override public ClassLoader
    getClassLoader() {
        if (this.result != null) return this.result;
        throw new IllegalStateException("Not yet cooked");
    }

    @Override public void
    cook(@Nullable String optionalFileName, final Reader r) throws CompileException, IOException {

        // Create one Java source file in memory, which will be compiled later.
        JavaFileObject compilationUnit;
        {
            URI uri;
            try {
                uri = new URI("simplecompiler");
            } catch (URISyntaxException use) {
                throw new RuntimeException(use);
            }
            compilationUnit = new SimpleJavaFileObject(uri, Kind.SOURCE) {

                @Override public boolean
                isNameCompatible(@Nullable String simpleName, @Nullable Kind kind) { return true; }

                @Override public Reader
                openReader(boolean ignoreEncodingErrors) throws IOException { return r; }

                @Override public CharSequence
                getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return Cookable.readString(this.openReader(ignoreEncodingErrors));
                }

                @Override public String
                toString() { return String.valueOf(this.uri); }
            };
        }

        // Find the JDK Java compiler.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompileException(
                "JDK Java compiler not available - probably you're running a JRE, not a JDK",
                null
            );
        }

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        final JavaFileManager fm = compiler.getStandardFileManager(null, null, null);

        // Wrap it so that the output files (in our case class files) are stored in memory rather
        // than in files.
        final JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(
            new ByteArrayJavaFileManager<JavaFileManager>(fm)
        ) {

            @Override public ClassLoader
            getClassLoader(@Nullable javax.tools.JavaFileManager.Location location) {
                return SimpleCompiler.this.parentClassLoader;
            }
        };

        // Run the compiler.
        try {
            final CompileException[] caughtCompileException = new CompileException[1];
            if (!compiler.getTask(
                null,                                      // out
                fileManager,                               // fileManager
                new DiagnosticListener<JavaFileObject>() { // diagnosticListener

                    @Override public void
                    report(@Nullable Diagnostic<? extends JavaFileObject> diagnostic) {
                        assert diagnostic != null;

                        Location loc = new Location(
                            null,
                            (short) diagnostic.getLineNumber(),
                            (short) diagnostic.getColumnNumber()
                        );
                        String message = diagnostic.getMessage(null) + " (" + diagnostic.getCode() + ")";

                        try {
                            switch (diagnostic.getKind()) {

                            case ERROR:
                                ErrorHandler oceh = SimpleCompiler.this.optionalCompileErrorHandler;
                                if (oceh == null) throw new CompileException(message, loc);
                                oceh.handleError(message, loc);
                                break;

                            case MANDATORY_WARNING:
                            case WARNING:
                                WarningHandler owh = SimpleCompiler.this.optionalWarningHandler;
                                if (owh != null) owh.handleWarning(null, message, loc);
                                break;

                            case NOTE:
                            case OTHER:
                            default:
                                break;
                            }
                        } catch (CompileException ce) {
                            if (caughtCompileException[0] == null) caughtCompileException[0] = ce;
                        }
                    }
                },
                Collections.singletonList(                 // options
                    this.debugSource
                    ? "-g:source" + (this.debugLines ? ",lines" : "") + (this.debugVars ? ",vars" : "")
                    : this.debugLines
                    ? "-g:lines" + (this.debugVars ? ",vars" : "")
                    : this.debugVars
                    ? "-g:vars"
                    : "-g:none"
                ),
                null,                                      // classes
                Collections.singleton(compilationUnit)     // compilationUnits
            ).call()) {
                if (caughtCompileException[0] != null) throw caughtCompileException[0];
                throw new CompileException("Compilation failed", null);
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

        // Create a ClassLoader that reads class files from our FM.
        this.result = AccessController.doPrivileged(new PrivilegedAction<JavaFileManagerClassLoader>() {

            @Override public JavaFileManagerClassLoader
            run() { return new JavaFileManagerClassLoader(fileManager, SimpleCompiler.this.parentClassLoader); }
        });
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;
    }

    @Override public void
    setParentClassLoader(@Nullable ClassLoader optionalParentClassLoader) {
        this.parentClassLoader = (
            optionalParentClassLoader != null
            ? optionalParentClassLoader
            : Thread.currentThread().getContextClassLoader()
        );
    }

    /**
     * @deprecated Auxiliary classes never really worked... don't use them.
     */
    @Deprecated public void
    setParentClassLoader(@Nullable ClassLoader optionalParentClassLoader, Class<?>[] auxiliaryClasses) {
        this.setParentClassLoader(optionalParentClassLoader);
    }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler optionalCompileErrorHandler) {
        this.optionalCompileErrorHandler = optionalCompileErrorHandler;
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }
}
