
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
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.tools.JavaCompiler;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.io.Readers;
import org.codehaus.commons.compiler.util.LineAndColumnTracker;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.MultiResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceFinders;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * {@code javax.tools}-based implementation of {@link ISimpleCompiler}.
 */
public
class SimpleCompiler extends Cookable implements ISimpleCompiler {

    private ClassLoader    parentClassLoader = Thread.currentThread().getContextClassLoader();
    private final Compiler compiler;

    /**
     * Is {@code null} iff this {@link SimpleCompiler} is not yet cooked.
     */
    @Nullable private Map<String, byte[]> bytecodes;

    // See "addOffset(String)".
    private final LineAndColumnTracker tracker = LineAndColumnTracker.create();
    private final SortedSet<Location>  offsets = new TreeSet<>(new Comparator<Location>() {

        @Override @NotNullByDefault(false) public int
        compare(Location l1, Location l2) {
            return (
                l1.getLineNumber() < l2.getLineNumber() ? -1 :
                l1.getLineNumber() > l2.getLineNumber() ? 1 :
                l1.getColumnNumber() - l2.getColumnNumber()
            );
        }
    });

    public
    SimpleCompiler() { this.compiler = new Compiler(); }

    /**
     * Initializes with a <em>different</em>, {@code javax.tools.JavaCompiler}-compatible Java compiler.
     */
    public
    SimpleCompiler(JavaCompiler javaCompiler) { this.compiler = new Compiler(javaCompiler); }

    @Override public void
    setSourceVersion(int version) { this.compiler.setSourceVersion(version); }

    @Override public void
    setTargetVersion(int version) { this.compiler.setTargetVersion(version); }

    @Override public Map<String /*className*/, byte[] /*bytecode*/>
    getBytecodes() { return this.assertCooked(); }

    private Map<String /*className*/, byte[] /*bytecode*/>
    assertCooked() {

        Map<String /*className*/, byte[] /*bytecode*/> result = this.bytecodes;
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
    cook(@Nullable String fileName, Reader r) throws CompileException, IOException {

        // Reset the "offsets" and the line-and-column-tracker; see "addOffset(String)".
        this.tracker.reset();
        this.offsets.clear();
        r = Readers.trackLineAndColumn(r, this.tracker);

        // Create one Java source file in memory, which will be compiled later.
        String   text            = Readers.readAll(r);
        Resource compilationUnit = new StringResource(fileName == null ? "simplecompiler" : fileName, text);

        // The default classpath of JAVAC is "." - we don't want that.
        this.compiler.setClassPath(new File[0]);

        // Create and find class files in a HashMap.
        Map<String, byte[]> bcs = (this.bytecodes = new HashMap<>());
        this.compiler.setClassFileFinder(new MultiResourceFinder(
            ResourceFinders.fromClassLoader(SimpleCompiler.this.parentClassLoader),
            new MapResourceFinder(bcs)
        ));
        this.compiler.setClassFileCreator(new MapResourceCreator(bcs));

        this.compiler.compile(new Resource[] { compilationUnit }, this.offsets);
    }

    @Override public void
    setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.compiler.setDebugSource(debugSource);
        this.compiler.setDebugLines(debugLines);
        this.compiler.setDebugVars(debugVars);
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
    setParentClassLoader(@Nullable ClassLoader parentClassLoader, Class<?>[] auxiliaryClasses) {
        this.setParentClassLoader(parentClassLoader);
    }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compiler.setCompileErrorHandler(compileErrorHandler);
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) { this.compiler.setWarningHandler(warningHandler); }

    /**
     * Derived classes call this method to "reset" the current line and column number at the currently read input
     * character, and also changes the "file name" (see {@link #cook(String, Reader)}).
     */
    protected void
    addOffset(@Nullable String fileName) {

        LineAndColumnTracker t = this.tracker;
        assert t != null;

        this.offsets.add(new Location(fileName, t.getLineNumber(), t.getColumnNumber()));
    }
}
