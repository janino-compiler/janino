
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

package org.codehaus.commons.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * "Cooking" means scanning a sequence of characters and turning them into some
 * JVM-executable artifact. For example, if you cook a {@link
 * org.codehaus.janino.ClassBodyEvaluator}, then the tokens are interpreted as a class body and
 * compiled into a {@link Class} which is accessible through {@link IClassBodyEvaluator#getClazz()}.
 * <p>
 * The <code>cook*()</code> methods eventually invoke the abstract {@link #cook(String, Reader)}
 * method.
 */
public
interface ICookable {

    /**
     * The {@link ClassLoader} that loads this classes on the boot class path, i.e. the JARs in the JRE's "lib" and
     * "lib/ext" directories, but not the JARs and class directories specified through the class path.
     */
    ClassLoader BOOT_CLASS_LOADER = ClassLoader.getSystemClassLoader().getParent();

    /**
     * The "parent class loader" is used to load referenced classes. Useful values are:
     * <table border="1"><tr>
     *   <td><code>System.getSystemClassLoader()</code></td>
     *   <td>The running JVM's class path</td>
     * </tr><tr>
     *   <td><code>Thread.currentThread().getContextClassLoader()</code> or <code>null</code></td>
     *   <td>The class loader effective for the invoking thread</td>
     * </tr><tr>
     *   <td>{@link #BOOT_CLASS_LOADER}</td>
     *   <td>The running JVM's boot class path</td>
     * </tr></table>
     * The parent class loader defaults to the current thread's context class loader.
     */
    void setParentClassLoader(@Nullable ClassLoader optionalParentClassLoader);

    /**
     * Value 'org.codehaus.janino.source_debugging.enable'.
     * <p>
     * Setting this system property to 'true' enables source-level debugging. Typically, this means that compilation
     * is executed with '-g:all' instead of '-g:none'.
     */
    String SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE = "org.codehaus.janino.source_debugging.enable";

    /**
     * Value 'org.codehaus.janino.source_debugging.dir'.
     * <p>
     * If source code is not compiled from a file, debuggers have a hard time locating the source file for source-level
     * debugging. As a workaround, a copy of the source code is written to a temporary file, which must be included
     * in the debugger's source path. If this system property is set, the temporary source file is created in that
     * directory, otherwise in the default temporary-file directory.
     *
     * @see File#createTempFile(String, String, File)
     */
    String SYSTEM_PROPERTY_SOURCE_DEBUGGING_DIR = "org.codehaus.janino.source_debugging.dir";

    /**
     * Determines what kind of debugging information is included in the generates classes. The default is typically
     * '-g:none', and '-g:all' if the system property 'org.codehaus.janino.source_debugging.enable' is set to 'true'.
     */
    void setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars);

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link Reader}.
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    void cook(@Nullable String optionalFileName, Reader r) throws CompileException, IOException;

    /**Reads, scans, parses and compiles Java tokens from the given {@link Reader}. */
    void cook(Reader r) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream}, encoded
     * in the "platform default encoding".
     */
    void cook(InputStream is) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream}, encoded
     * in the "platform default encoding".
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    void cook(@Nullable String optionalFileName, InputStream is) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the
     * given <code>encoding</code>.
     */
    void cook(InputStream is, @Nullable String optionalEncoding) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the
     * given <code>encoding</code>.
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    void
    cook(
        @Nullable String optionalFileName,
        InputStream      is,
        @Nullable String optionalEncoding
    ) throws CompileException, IOException;

    /** Reads, scans, parses and compiles Java tokens from the given {@link String}. */
    void cook(String s) throws CompileException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link String}.
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    void cook(@Nullable String optionalFileName, String s) throws CompileException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File}, encoded
     * in the "platform default encoding".
     */
    void cookFile(File file) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File} with the
     * given <code>encoding</code>.
     */
    void
    cookFile(File file, @Nullable String optionalEncoding) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the named file, encoded in the "platform
     * default encoding".
     */
    void cookFile(String fileName) throws CompileException, IOException;

    /** Reads, scans, parses and compiles Java tokens from the named file with the given {@code optionalEncoding}. */
    void
    cookFile(String fileName, @Nullable String optionalEncoding) throws CompileException, IOException;

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application my install its own
     * {@link ErrorHandler}.
     * <p>
     * Be aware that a single problem during compilation often causes a bunch of compile errors, so a good {@link
     * ErrorHandler} counts errors and throws a {@link CompileException} when a limit is reached.
     * <p>
     * If the given {@link ErrorHandler} throws {@link CompileException}s, then the compilation is terminated and
     * the exception is propagated.
     * <p>
     * If the given {@link ErrorHandler} does not throw {@link CompileException}s, then the compiler may or may not
     * continue compilation, but must eventually throw a {@link CompileException}.
     * <p>
     * In other words: The {@link ErrorHandler} may throw a {@link CompileException} or not, but the compiler must
     * definitely throw a {@link CompileException} if one or more compile errors have occurred.
     *
     * @param optionalCompileErrorHandler {@code null} to restore the default behavior (throwing a {@link
     *                                    CompileException}
     */
    void setCompileErrorHandler(@Nullable ErrorHandler optionalCompileErrorHandler);

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param optionalWarningHandler {@code null} to indicate that no warnings be issued
     */
    void setWarningHandler(@Nullable WarningHandler optionalWarningHandler);
}
