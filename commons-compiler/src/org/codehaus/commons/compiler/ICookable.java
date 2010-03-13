
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler;

import java.io.*;

/**
 * "Cooking" means scanning a sequence of characters and turning them into some
 * JVM-executable artifact. For example, if you cook a {@link
 * org.codehaus.janino.ClassBodyEvaluator}, then the tokens are interpreted as a class body and
 * compiled into a {@link Class} which is accessible through {@link ClassBodyEvaluator#getClazz()}.
 * <p>
 * The <code>cook*()</code> methods eventually invoke the abstract {@link #cook(String, Reader)}
 * method.
 */
public interface ICookable {

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
    void setParentClassLoader(ClassLoader optionalParentClassLoader);

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link Reader}.
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public abstract void cook(String optionalFileName, Reader r) throws CompileException,
        ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link Reader}.
     */
    public abstract void cook(Reader r) throws CompileException, ParseException, ScanException,
        IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream}, encoded
     * in the "platform default encoding".
     */
    public abstract void cook(InputStream is) throws CompileException, ParseException,
        ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream}, encoded
     * in the "platform default encoding".
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public abstract void cook(
        String      optionalFileName,
        InputStream is
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the
     * given <code>encoding</code>.
     */
    public abstract void cook(
        InputStream is,
        String      optionalEncoding
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the
     * given <code>encoding</code>.
     *
     * @param optionalFileName Used when reporting errors and warnings.
     */
    public abstract void cook(
        String      optionalFileName,
        InputStream is,
        String      optionalEncoding
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link String}.
     */
    public abstract void cook(String s) throws CompileException, ParseException, ScanException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File}, encoded
     * in the "platform default encoding".
     */
    public abstract void cookFile(
        File file
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File} with the
     * given <code>encoding</code>.
     */
    public abstract void cookFile(
        File   file,
        String optionalEncoding
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the named file, encoded in the "platform
     * default encoding".
     */
    public abstract void cookFile(
        String fileName
    ) throws CompileException, ParseException, ScanException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the named file with the given
     * <code>encoding</code>.
     */
    public abstract void cookFile(
        String fileName,
        String optionalEncoding
    ) throws CompileException, ParseException, ScanException, IOException;
}