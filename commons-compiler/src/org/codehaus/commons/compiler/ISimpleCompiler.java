
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

/**
 * A simplified Java&trade; compiler that can compile only a single compilation unit. (A "compilation unit" is the
 * document stored in a ".java" file.)
 * <p>
 * Opposed to a normal ".java" file, you can declare multiple public classes here.
 * <p>
 * To set up an {@link ISimpleCompiler} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create an {@link ISimpleCompiler}-implementing object
 *   <li>
 *   Optionally set an alternate parent class loader through {@link #setParentClassLoader(ClassLoader)}.
 *   <li>
 *   Call any of the {@link ICookable#cook(String, Reader)} methods to scan, parse, compile and load the compilation
 *   unit into the JVM.
 *   <li>
 *   Call {@link #getClassLoader()} to obtain a {@link ClassLoader} that you can use to access the compiled classes.
 * </ol>
 */
public interface ISimpleCompiler extends ICookable {

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can be accessed. This {@link
     * ClassLoader} can be used for subsequent {@link ISimpleCompiler}s in order to compile compilation units that use
     * types (e.g. declare derived types) declared in the previous one.
     * <p>
     * This method must only be called after exactly on of the {@link #cook(String, java.io.Reader)} methods was called.
     */
    ClassLoader getClassLoader();

}
