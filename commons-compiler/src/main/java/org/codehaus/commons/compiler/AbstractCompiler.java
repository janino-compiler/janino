
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

package org.codehaus.commons.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.util.Disassembler;
import org.codehaus.commons.compiler.util.StringUtil;
import org.codehaus.commons.compiler.util.SystemProperties;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceCreator;
import org.codehaus.commons.compiler.util.resource.DirectoryResourceFinder;
import org.codehaus.commons.compiler.util.resource.FileResource;
import org.codehaus.commons.compiler.util.resource.PathResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A base class and wrapper for {@link Compiler} that implements all redundant API methods.
 */
public abstract
class AbstractCompiler implements ICompiler {

    private static final boolean
    disassembleClassFilesToStdout = SystemProperties.getBooleanClassProperty(AbstractCompiler.class, "disassembleClassFilesToStdout");
    private static final Pattern
    disassembleClassNames = Pattern.compile(SystemProperties.getClassProperty(AbstractCompiler.class, "disassembleClassNames", ".*"));

    /** Implements the JAVAC {@code -sourcepath} option. */
    protected ResourceFinder sourceFinder = ResourceFinder.EMPTY_RESOURCE_FINDER;

    /** Implements the "read side" of the JAVAC {@code -d} option. */
    protected ResourceFinder classFileFinder = ICompiler.FIND_NEXT_TO_SOURCE_FILE;

    /** Implements the "write side" of the JAVAC {@code -d} option. */
    protected ResourceCreator classFileCreator = ICompiler.CREATE_NEXT_TO_SOURCE_FILE;

    // TODO: "CompilerTest.testSelfCompile()" fails if this field is PROTECTED -- why?
    /** Implements the JAVAC {@code -encoding} option. */
    public Charset sourceCharset = Charset.defaultCharset();

    /** Implements the JAVAC {@code -g:source} option. */
    protected boolean debugSource;

    /** Implements the JAVAC {@code -g:lines} option. */
    protected boolean debugLines;

    /** Implements the JAVAC {@code -g:vars} option. */
    protected boolean debugVars;

    protected int sourceVersion = -1;

    protected int targetVersion = -1;

    // Temporary, for testing:
//    { this.debugSource = (this.debugLines = (this.debugVars = true)); }

    @Override public void
    setSourceFinder(ResourceFinder sourceFinder) { this.sourceFinder = sourceFinder; }

    @Override public final void
    setClassFileFinder(ResourceFinder destination, boolean rebuild) {
        this.setClassFileFinder(rebuild ? ResourceFinder.EMPTY_RESOURCE_FINDER : destination);
    }

    @Override public void
    setClassFileFinder(ResourceFinder classFileFinder) { this.classFileFinder = classFileFinder; }

    /**
     * @param classFileCreator Stores the generated class files (a.k.a. "-d"); special value {@link
     *                         #CREATE_NEXT_TO_SOURCE_FILE} means "create each .class file in the same directory as
     *                         its source file"
     */
    @Override public final void
    setClassFileCreator(ResourceCreator classFileCreator) {

        if (AbstractCompiler.disassembleClassFilesToStdout) {
            final ResourceCreator delegate = classFileCreator;
            classFileCreator = new ResourceCreator() {

                @Override public OutputStream
                createResource(String resourceName) throws IOException {
                    final OutputStream delegateOs = delegate.createResource(resourceName);

                    // Is disassembling enabled for that class name?
                    assert resourceName.endsWith(".class");
                    String className = resourceName.substring(0, resourceName.length() - 6).replace('/', '.');
                    if (AbstractCompiler.disassembleClassNames.matcher(className).matches()) {
                        return new ByteArrayOutputStream() {

                            @Override public void
                            close() throws IOException {
                                byte[] ba = this.toByteArray();
                                Disassembler.disassembleToStdout(ba);
                                delegateOs.write(ba);
                                delegateOs.close();
                            }
                        };
                    } else {
                        return delegateOs;
                    }
                }

                @Override public boolean
                deleteResource(String resourceName) { return delegate.deleteResource(resourceName); }
            };
        }

        this.classFileCreator = classFileCreator;
    }

    @Override
    public final boolean
    compile(File[] sourceFiles) throws CompileException, IOException {

        Resource[] sourceFileResources = new Resource[sourceFiles.length];
        for (int i = 0; i < sourceFiles.length; ++i) sourceFileResources[i] = new FileResource(sourceFiles[i]);
        this.compile(sourceFileResources);

        return true;
    }

    @Override public final void
    setEncoding(Charset encoding) { this.setSourceCharset(encoding); }

    @Override public void
    setSourceCharset(Charset charset) { this.sourceCharset = charset; }

    @Override public final void
    setCharacterEncoding(@Nullable String characterEncoding) {
        this.setSourceCharset((
            characterEncoding == null
            ? Charset.defaultCharset()
            : Charset.forName(characterEncoding)
        ));
    }

    @Override public void
    setDebugLines(boolean value) { this.debugLines = value; }

    @Override public void
    setDebugVars(boolean value) { this.debugVars = value; }

    @Override public void
    setDebugSource(boolean value) { this.debugSource = value; }

    @Override public void
    setSourceVersion(int version) { this.sourceVersion = version; }

    @Override public void
    setTargetVersion(int version) { this.targetVersion = version; }

    @Override public void
    setSourcePath(File[] directoriesAndArchives) {
        this.setSourceFinder(new PathResourceFinder(directoriesAndArchives));
    }

    /**
     * The list of extension directories of the currently executing JRE. Empty for Java 9+,
     * because the system property "java.ext.dirs" is not set in JRE 9+.
     */
    protected File[]
    extensionDirectories = StringUtil.parsePath(System.getProperty("java.ext.dirs", ""));

    /**
     * The classpath of the currently executing JRE.
     */
    protected File[]
    classPath = StringUtil.parsePath(System.getProperty("java.class.path"));

    /**
     * This is <em>always</em> non-{@code null} for JVMs that support BOOTCLASSPATH (1.0-1.8), and
     * this is <em>always</em> {@code null} for JVMs that don't (9+).
     */
    @Nullable protected File[]
    bootClassPath = StringUtil.parseOptionalPath(System.getProperty("sun.boot.class.path"));

    /**
     * Stores the value configured with {@link #setCompileErrorHandler(ErrorHandler)}.
     */
    @Nullable protected ErrorHandler compileErrorHandler;

    /**
     * Stores the value configured with {@link #setWarningHandler(WarningHandler)}.
     */
    @Nullable protected WarningHandler warningHandler;

    @Override public void
    setBootClassPath(File[] directoriesAndArchives) {
        this.bootClassPath = directoriesAndArchives;
    }

    @Override public void
    setExtensionDirectories(File[] directories) {
        this.extensionDirectories = directories;
    }

    @Override public void
    setClassPath(File[] directoriesAndArchives) {
        this.classPath = directoriesAndArchives;
    }

    @Override public final void
    setDestinationDirectory(@Nullable File destinationDirectory, boolean rebuild) {

        if (destinationDirectory == ICompiler.NO_DESTINATION_DIRECTORY) {
            this.setClassFileCreator(ICompiler.CREATE_NEXT_TO_SOURCE_FILE);
            this.setClassFileFinder(ICompiler.FIND_NEXT_TO_SOURCE_FILE, rebuild);
        } else {
            assert destinationDirectory != null;
            this.setClassFileCreator(new DirectoryResourceCreator(destinationDirectory));
            this.setClassFileFinder(new DirectoryResourceFinder(destinationDirectory), rebuild);
        }
    }

    @Override public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }
}
