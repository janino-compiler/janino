
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.jdk.util.JavaFileManagers;
import org.codehaus.commons.compiler.jdk.util.JavaFileObjects.ByteArrayJavaFileObject;
import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.compiler.util.Disassembler;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.codehaus.commons.compiler.util.resource.PathResourceFinder;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link ClassLoader} that loads classes by looking for their source files through a "source path" and compiling
 * them on-the-fly.
 * <p>
 *   Notice that this class loader does not support resoures in the sense of {@link ClassLoader#getResource(String)},
 *   {@link ClassLoader#getResourceAsStream(String)} nd {@link ClassLoader#getResources(String)}.
 * </p>
 *
 * @see ClassLoaders
 */
public
class JavaSourceClassLoader extends AbstractJavaSourceClassLoader {

    private static final JavaCompiler SYSTEM_JAVA_COMPILER = JavaSourceClassLoader.getSystemJavaCompiler();

    private ResourceFinder     sourceFinder   = new DirectoryResourceFinder(new File("."));
    private Charset            sourceCharset  = Charset.defaultCharset();
    private boolean            debuggingInfoLines;
    private boolean            debuggingInfoVars;
    private boolean            debuggingInfoSource;
    private Collection<String> compilerOptions = new ArrayList<>();

    @Nullable private JavaFileManager fileManager;


    /**
     * @see ICompilerFactory#newJavaSourceClassLoader()
     */
    public
    JavaSourceClassLoader() { this.init(); }

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
     * @see ICompilerFactory#newJavaSourceClassLoader(ClassLoader)
     */
    public
    JavaSourceClassLoader(ClassLoader parentClassLoader) {
        super(parentClassLoader);
        this.init();
    }

    private void
    init() {
    }

    /**
     * Creates the underlying {@link JavaFileManager} lazily, because {@link #setSourcePath(File[])} and consorts are
     * called <em>after</em> initialization.
     */
    private JavaFileManager
    getJavaFileManager() {

        if (this.fileManager != null) return this.fileManager;

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        JavaFileManager jfm = JavaSourceClassLoader.SYSTEM_JAVA_COMPILER.getStandardFileManager(null, null, null);

        // Wrap it so that the output files (in our case .class files) are stored in memory rather
        // than in files.
        jfm = JavaFileManagers.inMemory(jfm, Charset.defaultCharset());

        // Wrap it in a file manager that finds source files through the source path.
        jfm = JavaFileManagers.fromResourceFinder(
            jfm,
            StandardLocation.SOURCE_PATH,
            Kind.SOURCE,
            this.sourceFinder,
            this.sourceCharset
        );

        return (this.fileManager = jfm);
    }

    @Override public void
    setSourcePath(File[] sourcePath) { this.setSourceFinder(new PathResourceFinder(sourcePath)); }

    @Override public void
    setSourceFinder(ResourceFinder sourceFinder) { this.sourceFinder = sourceFinder; }

    @Override public void
    setSourceCharset(Charset charset) { this.sourceCharset = charset; }

    @Override public void
    setDebuggingInfo(boolean lines, boolean vars, boolean source) {
        this.debuggingInfoLines  = lines;
        this.debuggingInfoVars   = vars;
        this.debuggingInfoSource = source;
    }

    /**
     * Notice: Don't use the '-g' options - these are controlled through {@link #setDebuggingInfo(boolean, boolean,
     * boolean)}.
     *
     * @param compilerOptions All command line options supported by the JDK JAVAC tool
     */
    public void
    setCompilerOptions(String[] compilerOptions) { this.compilerOptions = Arrays.asList(compilerOptions); }

    /**
     * Implementation of {@link ClassLoader#findClass(String)}.
     *
     * @throws ClassNotFoundException
     */
    @NotNullByDefault(false) @Override protected Class<?>
    findClass(String className) throws ClassNotFoundException {

        try {
            return this.findClass2(className);
        } catch (IOException ioe) {
            throw new DiagnosticException(ioe);
        }

    }

    private Class<?>
    findClass2(String className) throws IOException {

        // Find or generate the .class file.
        JavaFileObject classFileObject = this.findClassFile(className);

        // Load the .class file into memory.
        byte[] ba;
        int    size;
        if (classFileObject instanceof ByteArrayJavaFileObject) {
            ba   = ((ByteArrayJavaFileObject) classFileObject).toByteArray();
            size = ba.length;
        } else
        {
            ba   = new byte[4096];
            size = 0;
            InputStream is = classFileObject.openInputStream();
            try {
                for (;;) {
                    int res = is.read(ba, size, ba.length - size);
                    if (res == -1) break;
                    size += res;
                    if (size == ba.length) {
                        byte[] tmp = new byte[2 * size];
                        System.arraycopy(ba, 0, tmp, 0, size);
                        ba = tmp;
                    }
                }
                is.close();
            } finally {
                is.close();
            }
        }

        if (Boolean.getBoolean("disasm")) Disassembler.disassembleToStdout(ba);

        // Invoke "ClassLoader.defineClass()", as the ClassLoader API requires.
        return this.defineClass(className, ba, 0, size, (
            this.protectionDomainFactory != null
            ? this.protectionDomainFactory.getProtectionDomain(
                JavaSourceClassLoader.getSourceResourceName(className)
            )
            : null
        ));
    }

    private JavaFileObject
    findClassFile(String className) throws IOException {

        // Maybe the .class file is already there as a side effect of a previous compilation.
        JavaFileObject classFileObject = this.getJavaFileManager().getJavaFileForInput(
            StandardLocation.CLASS_OUTPUT,
            className,
            Kind.CLASS
        );
        if (classFileObject != null) return classFileObject;

        // .class file does not yet exist - get the .java file.
        JavaFileObject sourceFileObject;
        {
            String topLevelClassName;
            {
                int idx = className.indexOf('$');
                topLevelClassName = idx == -1 ? className : className.substring(0, idx);
            }
            sourceFileObject = this.getJavaFileManager().getJavaFileForInput(
                StandardLocation.SOURCE_PATH,
                topLevelClassName,
                Kind.SOURCE
            );
            if (sourceFileObject == null) throw new DiagnosticException("Source for '" + className + "' not found");
        }

        // Compose the effective compiler options.
        List<String> options = new ArrayList<>(this.compilerOptions);
        {
            List<String> l = new ArrayList<>();
            if (this.debuggingInfoLines)  l.add("lines");
            if (this.debuggingInfoSource) l.add("source");
            if (this.debuggingInfoVars)   l.add("vars");
            if (l.isEmpty()) l.add("none");

            Iterator<String> it = l.iterator();
            String           o  = "-g:" + it.next();
            while (it.hasNext()) o += "," + it.next();

            options.add(o);
        }

        // Run the compiler.
        if (!JavaSourceClassLoader.SYSTEM_JAVA_COMPILER.getTask(
            null,                                      // out
            this.getJavaFileManager(),                 // fileManager
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
            Collections.singleton(sourceFileObject)    // compilationUnits
        ).call()) throw new DiagnosticException(className + ": Compilation failed");

        // That should have created the .class file.
        classFileObject = this.getJavaFileManager().getJavaFileForInput(
            StandardLocation.CLASS_OUTPUT,
            className,
            Kind.CLASS
        );
        if (classFileObject == null) {
            throw new DiagnosticException(className + ": Class file not created by compilation");
        }

        return classFileObject;
    }

    /**
     * Constructs the name of a resource that could contain the source code of the class with the given name.
     * <p>
     *   Notice that member types are declared inside a different type, so the relevant source file is that of the
     *   outermost declaring class.
     * </p>
     *
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return the name of the resource, e.g. "pkg1/pkg2/Outer.java"
     */
    private static String
    getSourceResourceName(String className) {

        // Strip nested type suffixes.
        {
            int idx = className.lastIndexOf('.') + 1;
            idx = className.indexOf('$', idx);
            if (idx != -1) className = className.substring(0, idx);
        }

        return className.replace('.', '/') + ".java";
    }

    /**
     * Container for a {@link Diagnostic} object.
     */
    public static
    class DiagnosticException extends RuntimeException {

        private static final long serialVersionUID = 5589635876875819926L;

        DiagnosticException(String message) { super(message); }

        DiagnosticException(Throwable cause) { super(cause); }

        DiagnosticException(Diagnostic<? extends JavaFileObject> diagnostic) { super(diagnostic.toString()); }
    }
}
