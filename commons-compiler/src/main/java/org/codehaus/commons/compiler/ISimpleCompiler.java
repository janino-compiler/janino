
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

package org.codehaus.commons.compiler;

import java.io.Reader;
import java.security.Permissions;

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
 */
public
interface ISimpleCompiler extends ICookable {

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can be accessed. This {@link
     * ClassLoader} can be used for subsequent {@link ISimpleCompiler}s in order to compile compilation units that use
     * types (e.g. declare derived types) declared in the previous one.
     * <p>
     *   This method must only be called after exactly on of the {@link #cook(String, java.io.Reader)} methods was
     *   called.
     * </p>
     */
    ClassLoader getClassLoader();

    /**
     * Installs a security manager in the running JVM such that all generated code, when executed, will be checked
     * against the given <var>permissions</var>.
     * <p>
     *   By default, generated code is executed with the <em>same</em> permissions as the rest of the running JVM,
     *   which typically means that any generated code can easily compromise the system, e.g. by reading or deleting
     *   files on the local file system.
     * </p>
     * <p>
     *   Thus, if you compile and execute <em>any user-written code</em> and cannot be sure that the code is written
     *   carefully, then you should definitely restrict the permissions for the generated code.
     * </p>
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/environment/security.html">ORACLE: Java
     *      Essentials: The Security Manager</a>
     */
    void setPermissions(Permissions permissions);

    /**
     * Installs a security manager in the running JVM such that all generated code, when executed, is not allowed to
     * execute any checked operations.
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/essential/environment/security.html">ORACLE: Java
     *      Essentials: The Security Manager</a>
     */
    void setNoPermissions();
}
