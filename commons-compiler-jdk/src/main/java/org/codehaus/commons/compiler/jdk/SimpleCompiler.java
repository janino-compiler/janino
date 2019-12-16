
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

package org.codehaus.commons.compiler.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
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
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.io.InputStreams;
import org.codehaus.commons.compiler.io.Readers;
import org.codehaus.commons.compiler.jdk.util.ClassLoaders;
import org.codehaus.commons.compiler.jdk.util.JavaFileObjects;
import org.codehaus.commons.compiler.util.LineAndColumnTracker;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * The JDK-based implementation of {@link ISimpleCompiler}.
 */
public
class SimpleCompiler extends Cookable implements ISimpleCompiler {

    private ClassLoader              parentClassLoader = Thread.currentThread().getContextClassLoader();
    private boolean                  debugSource;
    private boolean                  debugLines;
    private boolean                  debugVars;
    @Nullable private ErrorHandler   compileErrorHandler;
    @Nullable private WarningHandler warningHandler;

    @Nullable Map<String, byte[]> bytecodes;

    // See "addOffset(String)".
    private final LineAndColumnTracker tracker = LineAndColumnTracker.create();
    private final SortedSet<Location>  offsets = new TreeSet<Location>(new Comparator<Location>() {

        @Override @NotNullByDefault(false) public int
        compare(Location l1, Location l2) {
            return (
                l1.getLineNumber() < l2.getLineNumber() ? -1 :
                l1.getLineNumber() > l2.getLineNumber() ? 1 :
                l1.getColumnNumber() - l2.getColumnNumber()
            );
        }
    });

    @Override public Map<String /*className*/, byte[] /*bytes*/>
    getBytecodes() { return this.assertCooked(); }

    private Map<String /*className*/, byte[] /*bytes*/>
    assertCooked() {

        Map<String /*className*/, byte[] /*bytes*/> result = this.bytecodes;
        if (result == null) throw new IllegalStateException("Must only be called after \"cook()\"");

        return result;
    }

    @Override public ClassLoader
    getClassLoader() {

        ClassLoader result = this.getClassLoaderCache;
        if (result != null) return result;

        final Map<String, byte[]> bytecode = this.getBytecodes();

        // Create a ClassLoader that loads the generated classes.
        result = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override public ClassLoader
            run() {
                return new ByteArrayClassLoader(
                    bytecode,                             // classes
                    SimpleCompiler.this.parentClassLoader // parent
                );
            }
        });

        return (this.getClassLoaderCache = result);
    }
    @Nullable private ClassLoader getClassLoaderCache;

    @Override public void
    cook(@Nullable final String optionalFileName, Reader r) throws CompileException, IOException {

        // Reset the "offsets" and the line-and-column-tracker; see "addOffset(String)".
        this.tracker.reset();
        this.offsets.clear();
        r = Readers.trackLineAndColumn(r, this.tracker);

        // Create one Java source file in memory, which will be compiled later.
        JavaFileObject compilationUnit;
        {
            URI uri;
            try {
                uri = new URI("simplecompiler");
            } catch (URISyntaxException use) {
                throw new RuntimeException(use);
            }

            // Must read source code in advance so that "openReader()" and "getCharContent()" are idempotent. If they
            // are not, then "diagnostic.get(Line|Column)Number()" will return wrong results.
            final String text = Cookable.readString(r);

            compilationUnit = new SimpleJavaFileObject(uri, Kind.SOURCE) {

                @Override public boolean
                isNameCompatible(@Nullable String simpleName, @Nullable Kind kind) { return true; }

                @Override public Reader
                openReader(boolean ignoreEncodingErrors) throws IOException { return new StringReader(text); }

                @Override public CharSequence
                getCharContent(boolean ignoreEncodingErrors) throws IOException { return text; }

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

        final CompileException[] caughtCompileException = new CompileException[1];

        final DiagnosticListener<JavaFileObject>
        dl = new DiagnosticListener<JavaFileObject>() {

            @Override public void
            report(@Nullable Diagnostic<? extends JavaFileObject> diagnostic) {
                assert diagnostic != null;

                Location loc = new Location(
                    optionalFileName,
                    (short) diagnostic.getLineNumber(),
                    (short) diagnostic.getColumnNumber()
                );

                // Manipulate the diagnostic location to accomodate for the "offsets" (see "addOffset(String)"):
                SortedSet<Location> hs = SimpleCompiler.this.offsets.headSet(loc);
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

                String message = diagnostic.getMessage(null) + " (" + diagnostic.getCode() + ")";

                try {
                    switch (diagnostic.getKind()) {

                    case ERROR:
                        ErrorHandler oceh = SimpleCompiler.this.compileErrorHandler;
                        if (oceh == null) throw new CompileException(message, loc);
                        oceh.handleError(message, loc);
                        break;

                    case MANDATORY_WARNING:
                    case WARNING:
                        WarningHandler owh = SimpleCompiler.this.warningHandler;
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
        };

        // Set up a JavaFileManager that reads .class files through the this.parentClassLoader, and stores .class
        // files in byte arrays
        final Map<String /*className*/, JavaFileObject> classFiles = new HashMap<String, JavaFileObject>();
        final JavaFileManager
        fileManager = new ForwardingJavaFileManager<JavaFileManager>(
            ToolProvider
            .getSystemJavaCompiler()
            .getStandardFileManager(dl, Locale.US, Charset.forName("UTF-8"))
        ) {

            @NotNullByDefault(false) @Override public Iterable<JavaFileObject>
            list(JavaFileManager.Location location, String packageName, Set<Kind> kinds, boolean recurse)
            throws IOException {

                // We support only listing of ".class" resources.
                if (!kinds.contains(Kind.CLASS)) return super.list(location, packageName, kinds, recurse);

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

                    result.add(JavaFileObjects.fromUrl(url, name, Kind.CLASS));
                }

                return result;
            }

            @NotNullByDefault(false) @Override public String
            inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {
                String result = file.getName();
                return result.substring(0, result.lastIndexOf('.')).replace('/', '.');
            }

            @NotNullByDefault(false) @Override public JavaFileObject
            getJavaFileForInput(JavaFileManager.Location location, String className, Kind kind)
            throws IOException {

                if (location == StandardLocation.CLASS_OUTPUT) {
                    return classFiles.get(className);
                }

                return super.getJavaFileForInput(location, className, kind);
            }

            @NotNullByDefault(false) @Override public JavaFileObject
            getJavaFileForOutput(
                JavaFileManager.Location location,
                String                   className,
                Kind                     kind,
                FileObject               sibling
            ) throws IOException {

                if (location != StandardLocation.CLASS_OUTPUT) {
                    return super.getJavaFileForOutput(location, className, kind, sibling);
                }

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

                classFiles.put(className, fileObject);

                return fileObject;
            }
        };

        // Run the compiler.
        try {

            if (!compiler.getTask(
                null,                                      // out
                fileManager,                               // fileManager
                dl,                                        // diagnosticListener
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

        // Fill "this.bytecodes" from the JavaFileManager.
        Map<String /*className*/, byte[] /*bytes*/> bytecodes = new HashMap<String /*className*/, byte[] /*bytes*/>();
        for (Entry<String, JavaFileObject> e : classFiles.entrySet()) {
            String         className = e.getKey();
            JavaFileObject jfo       = e.getValue();

            byte[] bytes;

            final InputStream is = jfo.openInputStream();
            try {
                bytes = InputStreams.readAll(is);
            } finally {
                try { is.close(); } catch (Exception ex) {}
            }

            bytecodes.put(className, bytes);
        }

        this.bytecodes = bytecodes;
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;
    }

    @Override public void
    setParentClassLoader(@Nullable ClassLoader parentClassLoader) {
        this.parentClassLoader = (
            parentClassLoader != null
            ? parentClassLoader
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
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    /**
     * Derived classes call this method to "reset" the current line and column number at the currently read input
     * character, and also changes the "file name" (see {@link #cook(String, Reader)}).
     */
    protected void
    addOffset(@Nullable String optionalFileName) {

        LineAndColumnTracker t = this.tracker;
        assert t != null;

        this.offsets.add(new Location(optionalFileName, t.getLineNumber(), t.getColumnNumber()));
    }
}
