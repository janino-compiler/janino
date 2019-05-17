
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
import java.nio.charset.Charset;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A simplified substitute for the <tt>javac</tt> tool.
 */
public
interface ICompiler {

    /**
     * Special value for {@link #setDestinationDirectory(File)}'s parameter: Indicates that .class files are to be
     * created in the directory of the .java file from which they are generated.
     */
    @Nullable File NO_DESTINATION_DIRECTORY = null;

    /**
     * Equivalent of -encoding.
     */
    void setEncoding(@Nullable Charset encoding);

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
     * Equivalent of {@code --source-path}.
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
     *-d</a>.
     *
     * @see #NO_DESTINATION_DIRECTORY
     */
    void setDestinationDirectory(@Nullable File destinationDirectory);

    /**
     * Equivalent of {@code -verbose}.
     */
    void setVerbose(boolean verbose);

    /**
     * Equivalent of {@code -rebuild}.
     */
    void setRebuild(boolean value);

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
}
