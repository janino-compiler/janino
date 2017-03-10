
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

package org.codehaus.commons.compiler.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.Sandbox;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
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
    @Nullable private Permissions    permissions;

    /**
     * @throws IllegalStateException This {@link Cookable} is not yet cooked
     */
    @Override public ClassLoader
    getClassLoader() {

        ClassLoader cl = this.result;
        if (cl == null) throw new IllegalStateException("Not yet cooked");

        return cl;
    }

    @Override public void
    setPermissions(Permissions permissions) { this.permissions = permissions;  }

    @Override public void
    setNoPermissions() { this.setPermissions(new Permissions()); }

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

        // Set up a JavaFileManager that reads .class files through the this.parentClassLoader, and stores .class
        // files in byte arrays
        final JavaFileManager fileManager = new JavaFileManager() {

            @NotNullByDefault(false) @Override public ClassLoader
            getClassLoader(JavaFileManager.Location location) { return null; }

            @NotNullByDefault(false) @Override public Iterable<JavaFileObject>
            list(JavaFileManager.Location location, String packageName, Set<Kind> kinds, boolean recurse)
            throws IOException {

                // We support only listing of ".class" resources.
                if (!kinds.contains(Kind.CLASS)) return Collections.emptyList();

                final String namePrefix = packageName.isEmpty() ? "" : packageName.replace('.', '/') + '/';

                Map<String, URL> allSubresources = ClassLoaders.getSubresources(
                    SimpleCompiler.this.parentClassLoader,
                    namePrefix,
                    false, // includeDirectories
                    recurse
                );

                Collection<JavaFileObject> result = new ArrayList<JavaFileObject>(allSubresources.size());
                for (Entry<String, URL> e : allSubresources.entrySet()) {
                    final String name = e.getKey();
                    final URL    url  = e.getValue();

                    if (!name.endsWith(".class")) continue;

                    final URI subresourceUri;
                    try {
                        subresourceUri = url.toURI();
                    } catch (URISyntaxException use) {
                        throw new AssertionError(use);
                    }

                    // Cannot use "javax.tools.SimpleJavaFileObject" here, because that requires a URI with a "path".
                    result.add(new JavaFileObject() {

                        @Override public URI
                        toUri() { return subresourceUri; }

                        @Override public String
                        getName() { return name; }

                        @Override public InputStream
                        openInputStream() throws IOException { return url.openStream(); }

                        @Override public Kind
                        getKind() { return Kind.CLASS; }

                        // SUPPRESS CHECKSTYLE LineLength:9
                        @Override public OutputStream openOutputStream()                             { throw new UnsupportedOperationException(); }
                        @Override public Reader       openReader(boolean ignoreEncodingErrors)       { throw new UnsupportedOperationException(); }
                        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors)   { throw new UnsupportedOperationException(); }
                        @Override public Writer       openWriter()                                   { throw new UnsupportedOperationException(); }
                        @Override public long         getLastModified()                              { throw new UnsupportedOperationException(); }
                        @Override public boolean      delete()                                       { throw new UnsupportedOperationException(); }
                        @Override public boolean      isNameCompatible(String simpleName, Kind kind) { throw new UnsupportedOperationException(); }
                        @Override public NestingKind  getNestingKind()                               { throw new UnsupportedOperationException(); }
                        @Override public Modifier     getAccessLevel()                               { throw new UnsupportedOperationException(); }

                        @Override public String
                        toString() { return name + " from " + this.getClass().getSimpleName(); }
                    });
                }

                return result;
            }

            @NotNullByDefault(false) @Override public String
            inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {
                String result = file.getName();
                return result.substring(0, result.lastIndexOf('.')).replace('/', '.');
            }

            @Override public boolean
            hasLocation(@Nullable JavaFileManager.Location location) {
                return location == StandardLocation.CLASS_PATH;
            }

            @NotNullByDefault(false) @Override public JavaFileObject
            getJavaFileForInput(JavaFileManager.Location location, String className, Kind kind)
            throws IOException {

                if (location != StandardLocation.CLASS_OUTPUT) throw new UnsupportedOperationException();

                return this.classFiles.get(className);
            }

            Map<String /*className*/, JavaFileObject> classFiles = new HashMap<String, JavaFileObject>();

            @NotNullByDefault(false) @Override public JavaFileObject
            getJavaFileForOutput(
                JavaFileManager.Location location,
                String                   className,
                Kind                     kind,
                FileObject               sibling
            ) throws IOException {

                if (location != StandardLocation.CLASS_OUTPUT) throw new UnsupportedOperationException();
                if (kind != Kind.CLASS) throw new UnsupportedOperationException();

                JavaFileObject fileObject = new SimpleJavaFileObject(
                    URI.create("bytearray:///" + className.replace('.', '/') + kind.extension),
                    Kind.CLASS
                ) {

                    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                    @Override public OutputStream
                    openOutputStream() throws IOException { return this.buffer; }

                    @Override public InputStream
                    openInputStream() throws IOException { return new ByteArrayInputStream(this.buffer.toByteArray()); }
                };

                this.classFiles.put(className, fileObject);

                return fileObject;
            }

            @NotNullByDefault(false) @Override public boolean
            isSameFile(FileObject a, FileObject b) { throw new UnsupportedOperationException(); }

            @NotNullByDefault(false) @Override public boolean
            handleOption(String current, Iterator<String> remaining) { throw new UnsupportedOperationException(); }

            @NotNullByDefault(false) @Override public int
            isSupportedOption(String option) { throw new UnsupportedOperationException(); }

            @NotNullByDefault(false) @Override public FileObject
            getFileForInput(JavaFileManager.Location location, String packageName, String relativeName) {
                throw new UnsupportedOperationException();
            }

            @NotNullByDefault(false) @Override public FileObject
            getFileForOutput(
                JavaFileManager.Location location,
                String                   packageName,
                String                   relativeName,
                FileObject               sibling
            ) throws IOException { throw new UnsupportedOperationException(); }

            @Override public void
            flush() {}

            @Override public void
            close() {}
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
        } finally {
            fileManager.close();
        }

        // Create a ClassLoader that reads class files from our FM.
        ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<JavaFileManagerClassLoader>() {

            @Override public JavaFileManagerClassLoader
            run() { return new JavaFileManagerClassLoader(fileManager, SimpleCompiler.this.parentClassLoader); }
        });

        // Apply any configured permissions.
        if (this.permissions != null) Sandbox.confine(cl, this.permissions);

        this.result = cl;
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
