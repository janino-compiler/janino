
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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;

import org.codehaus.commons.compiler.util.resource.ListableResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;
import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A simplified substitute for the <tt>javac</tt> tool.
 */
public
interface ICompiler {

    /**
     * Special value for {@link #setDestinationDirectory(File, boolean)}'s first parameter: Indicates that .class files
     * are to be created in the directory of the .java file from which they are generated.
     */
    @Nullable File NO_DESTINATION_DIRECTORY = null;

    /**
     * Special value for {@link #setClassFileFinder(ResourceFinder)}.
     *
     * @see #setClassFileFinder(ResourceFinder)
     */
    ResourceFinder FIND_NEXT_TO_SOURCE_FILE = new ListableResourceFinder() {
        @Override @Nullable public Resource           findResource(String resourceName)                { throw new UnsupportedOperationException("FIND_NEXT_TO_SOUJRCE_FILE"); } // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Iterable<Resource> list(String resourceNamePrefix, boolean recurse) { return Collections.emptyList();                                       }
        @Override public String                       toString()                                       { return "FIND_NEXT_TO_SOUJRCE_FILE";                                   }
    };

    /**
     * Special value for {@link #setClassFileCreator(ResourceCreator)}: Indicates that .class resources are to be
     * created in the directory of the .java resource from which they are generated.
     */
    ResourceCreator CREATE_NEXT_TO_SOURCE_FILE = new ResourceCreator() {
        @Override public boolean      deleteResource(String resourceName) { throw new UnsupportedOperationException("CREATE_NEXT_TO_SOURCE_FILE"); } // SUPPRESS CHECKSTYLE LineLength:2
        @Override public OutputStream createResource(String resourceName) { throw new UnsupportedOperationException("CREATE_NEXT_TO_SOURCE_FILE"); }
        @Override public String       toString()                          { return "CREATE_NEXT_TO_SOURCE_FILE";                                   }
    };

    /**
     * The equivalent of JAVA's "{@code -encoding}" command line option.
     *
     * @see #setSourceCharset(Charset)
     */
    void setEncoding(Charset encoding);

    /**
     * Same as {@link #setEncoding(Charset)}, but with a more precise name.
     */
    void setSourceCharset(Charset charset);

    /**
     * @deprecated Use {@link #setSourceCharset(Charset)} instead
     */
    @Deprecated void setCharacterEncoding(@Nullable String characterEncoding);

    /**
     * Equivalent of {@code -g:lines}.
     */
    void setDebugLines(boolean value);

    /**
     * Equivalent of {@code -g:vars}.
     */
    void setDebugVars(boolean value);

    /**
     * Equivalent of {@code -g:source}.
     */
    void setDebugSource(boolean value);

    /**
     * Finds more {@code .java} resources that need to be compiled, i.e. implements JAVAC's {@code -sourcepath} option.
     */
    void setSourceFinder(ResourceFinder sourceFinder);

    /**
     * Equivalent of {@code --source-path}.
     * <p>
     *   Equivalent with {@code setSourceFinder(new PathResourceFinder(directoriesAndArchives))}.
     * </p>
     */
    void setSourcePath(File[] directoriesAndArchives);

    /**
     * Equivalent of <a href="https://docs.oracle.com/en/java/javase/11/tools/javac.html#
     *GUID-AEEC9F07-CB49-4E96-8BC7-BCC2C7F725C9__GUID-38BC1737-2F22-4288-8DC9-933C37D471AB">
     *{@code --boot-class-path}</a>.
     */
    void setBootClassPath(File[] directoriesAndArchives);

    /**
     * Equivalent of {@code -extdirs}.
     */
    void setExtensionDirectories(File[] directories);

    /**
     * Equivalent of <a href="https://docs.oracle.com/en/java/javase/11/tools/javac.html#
     *GUID-AEEC9F07-CB49-4E96-8BC7-BCC2C7F725C9__GUID-45DC2932-19BD-435B-B14C-A230D0A4EC87">
     *--class-path</a>.
     */
    void setClassPath(File[] directoriesAndArchives);

    /**
     * Equivalent of <a href="https://docs.oracle.com/en/java/javase/11/tools/javac.html#
     *GUID-AEEC9F07-CB49-4E96-8BC7-BCC2C7F725C9__GUID-45DC2932-19BD-435B-B14C-A230D0A4EC87">
     *-d</a> and {@code -rebuild}.
     * <p>
     *   Overrides any previously configured {@link #setClassFileFinder(ResourceFinder) class file finder} and {@link
     *   #setClassFileCreator(ResourceCreator) class file creator}.
     * </p>
     *
     * @param destinationDirectory                {@link #NO_DESTINATION_DIRECTORY} means "create .class resources in
     *                                            the directory of the .java resource from which they are generated"
     * @see #NO_DESTINATION_DIRECTORY
     * @see #setClassFileFinder(ResourceFinder)
     * @see #setClassFileCreator(ResourceCreator)
     */
    void setDestinationDirectory(@Nullable File destinationDirectory, boolean rebuild);

    /**
     * Equivalent with {@code setClassFileFinder(rebuild ? ResourceFinder.EMPTY_RESOURCE_FINDER : classFileFinder)}.
     */
    void setClassFileFinder(ResourceFinder classFileFinder, boolean rebuild);

    /**
     * This {@link ResourceFinder} is used to check whether a {@code .class} resource already exists and is younger
     * than the {@code .java} resource from which it was generated.
     * <p>
     *   If it is impossible to check whether an already-compiled class file exists, or if you want to enforce
     *   recompilation, pass {@link ResourceFinder#EMPTY_RESOURCE_FINDER} as the <var>classFileFinder</var>.
     * </p>
     * <p>
     *   The default is, as for JAVAC, {@link #FIND_NEXT_TO_SOURCE_FILE}.
     * </p>
     *
     * @param classFileFinder         Special value {@link #FIND_NEXT_TO_SOURCE_FILE} means ".class file is next to
     *                                its source file, <em>not</em> in the destination directory"
     * @see #FIND_NEXT_TO_SOURCE_FILE
     */
    void setClassFileFinder(ResourceFinder classFileFinder);

    /**
     * This {@link ResourceCreator} is used to store generated {@code .class} files.
     * <p>
     *   The default is, as for JAVAC, {@link #CREATE_NEXT_TO_SOURCE_FILE}.
     * </p>
     *
     * @param classFileCreator          Special value {@link #CREATE_NEXT_TO_SOURCE_FILE} means "create .class file
     *                                  next to its source file, <em>not</em> in the destination directory"
     * @see #CREATE_NEXT_TO_SOURCE_FILE
     *
     * @param classFileCreator
     */
    void setClassFileCreator(ResourceCreator classFileCreator);

    /**
     * Equivalent of {@code -verbose}.
     */
    void setVerbose(boolean verbose);

    /**
     * Reads a set of Java compilation units (a.k.a. "source files") from the file system, compiles them into a set of
     * "class files" and stores these in the file system. Additional source files are parsed and compiled on demand
     * through the "source path" set of directories.
     * <p>
     *   For example, if the source path comprises the directories "A/B" and "../C", then the source file for class
     *   "com.acme.Main" is searched in
     * </p>
     * <dl>
     *   <dd>A/B/com/acme/Main.java
     *   <dd>../C/com/acme/Main.java
     * </dl>
     * <p>
     *   Notice that it does make a difference whether you pass multiple source files to {@link #compile(File[])} or if
     *   you invoke {@link #compile(File[])} multiply: In the former case, the source files may contain arbitrary
     *   references among each other (even circular ones). In the latter case, only the source files on the source path
     *   may contain circular references, not the <var>sourceFiles</var>.
     * </p>
     * <p>
     *   This method must be called exactly once after object construction.
     * </p>
     * <p>
     *   Compile errors are reported as described at {@link #setCompileErrorHandler(ErrorHandler)}.
     * </p>
     *
     * @param sourceFiles       Contain the compilation units to compile
     * @return                  {@code true} for backwards compatibility (return value can safely be ignored)
     * @throws CompileException Fatal compilation error, or the {@link CompileException} thrown be the installed
     *                          compile error handler
     * @throws IOException      Occurred when reading from the <var>sourceFiles</var>
     */
    boolean compile(File[] sourceFiles) throws CompileException, IOException;

    /**
     * See {@link #compile(File[])}.
     *
     * @param sourceResources Contain the compilation units to compile
     */
    void compile(Resource[] sourceResources) throws CompileException, IOException;

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application my install its own
     * {@link ErrorHandler}.
     * <p>
     *   Be aware that a single problem during compilation often causes a bunch of compile errors, so a good {@link
     *   ErrorHandler} counts errors and throws a {@link CompileException} when a limit is reached.
     * </p>
     * <p>
     *   If the given {@link ErrorHandler} throws {@link CompileException}s, then the compilation is terminated and
     *   the exception is propagated.
     * </p>
     * <p>
     *   If the given {@link ErrorHandler} does not throw {@link CompileException}s, then the compiler may or may not
     *   continue compilation, but must eventually throw a {@link CompileException}.
     * </p>
     * <p>
     *   In other words: The {@link ErrorHandler} may throw a {@link CompileException} or not, but the compiler must
     *   definitely throw a {@link CompileException} if one or more compile errors have occurred.
     * </p>
     *
     * @param errorHandler {@code null} to restore the default behavior (throwing a {@link CompileException}
     */
    void setCompileErrorHandler(@Nullable ErrorHandler errorHandler);

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    void setWarningHandler(WarningHandler warningHandler);
}
