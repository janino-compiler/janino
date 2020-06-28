
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

package org.codehaus.commons.compiler;

import java.io.Reader;

import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A simplified Java compiler that can compile only a single compilation unit. (A "compilation unit" is the document
 * stored in a ".java" file.)
 * <p>
 *   Opposed to a normal ".java" file, you can declare multiple public classes here.
 * </p>
 * <p>
 *   To set up an {@link ISimpleCompiler} object, proceed as follows:
 * </p>
 * <ol>
 *   <li>Create an {@link ISimpleCompiler}-implementing object</li>
 *   <li>Optionally set an alternate parent class loader through {@link #setParentClassLoader(ClassLoader)}.</li>
 *   <li>
 *     Call any of the {@link ICookable#cook(String, Reader)} methods to scan, parse, compile and load the compilation
 *     unit into the JVM.
 *   </li>
 *   <li>
 *     Call {@link #getClassLoader()} to obtain a {@link ClassLoader} that you can use to access the compiled classes.
 *   </li>
 * </ol>
 * <h3>Comptibility notice:</h3>
 * <p>
 *   The methods {@code setPermissions(Permissions permissions)} and {@code void setNoPermissions()} were removed in
 *   version 3.1.1 (2020-03-09) and replaced by the {@link Sandbox}.
 * </p>
 */
public
interface ISimpleCompiler extends ICookable {

    /**
     * The "parent class loader" is used to load referenced classes. Useful values are:
     * <table border="1"><tr>
     *   <td>{@code System.getSystemClassLoader()}</td>
     *   <td>The running JVM's class path</td>
     * </tr><tr>
     *   <td>{@code Thread.currentThread().getContextClassLoader()} or {@code null}</td>
     *   <td>The class loader effective for the invoking thread</td>
     * </tr><tr>
     *   <td>{@link ClassLoaders#BOOTCLASSPATH_CLASS_LOADER}</td>
     *   <td>The running JVM's boot class path</td>
     * </tr></table>
     * <p>
     *   The parent class loader defaults to the current thread's context class loader.
     * </p>
     */
    void setParentClassLoader(@Nullable ClassLoader parentClassLoader);

    /**
     * Determines what kind of debugging information is included in the generates classes. The default is typically
     * "{@code -g:none}".
     */
    void setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars);

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
     * @param compileErrorHandler {@code null} to restore the default behavior (throwing a {@link CompileException}
     */
    void setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler);

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    void setWarningHandler(@Nullable WarningHandler warningHandler);

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can be accessed. This {@link
     * ClassLoader} can be used for subsequent {@link ISimpleCompiler}s in order to compile compilation units that use
     * types (e.g. declare derived types) declared in the previous one.
     * <p>
     *   This method must only be called after exactly one of the {@link #cook(String, java.io.Reader)} methods was
     *   called.
     * </p>
     */
    ClassLoader getClassLoader();
}
