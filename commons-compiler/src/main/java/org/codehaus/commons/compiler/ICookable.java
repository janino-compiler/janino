
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * "Cooking" means scanning a sequence of characters and turning them into some JVM-executable artifact. For example,
 * if you cook an {@link IClassBodyEvaluator}, then the tokens are interpreted as a class body and
 * compiled into a {@link Class} which is accessible through {@link IClassBodyEvaluator#getClazz()}.
 */
public
interface ICookable {

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link Reader}.
     *
     * @param fileName Used when reporting errors and warnings
     */
    void cook(@Nullable String fileName, Reader r) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link Reader}.
     */
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
     * @param fileName Used when reporting errors and warnings
     */
    void cook(@Nullable String fileName, InputStream is) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the given {@code
     * encoding}.
     */
    void cook(InputStream is, @Nullable String encoding) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream} with the given {@code
     * encoding}.
     *
     * @param fileName Used when reporting errors and warnings
     */
    void
    cook(
        @Nullable String fileName,
        InputStream      is,
        @Nullable String encoding
    ) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link String}.
     */
    void cook(String s) throws CompileException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link String}.
     *
     * @param fileName Used when reporting errors and warnings
     */
    void cook(@Nullable String fileName, String s) throws CompileException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File}, encoded in the "platform default
     * encoding".
     */
    void cookFile(File file) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link File} with the given {@code encoding}.
     */
    void
    cookFile(File file, @Nullable String encoding) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the named file, encoded in the "platform default encoding".
     */
    void cookFile(String fileName) throws CompileException, IOException;

    /**
     * Reads, scans, parses and compiles Java tokens from the named file with the given <var>encoding</var>.
     */
    void
    cookFile(String fileName, @Nullable String encoding) throws CompileException, IOException;
}
