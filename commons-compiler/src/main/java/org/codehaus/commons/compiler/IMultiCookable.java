
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
import java.io.InputStream;
import java.io.Reader;

public
interface IMultiCookable extends ICookable {

    /**
     * Same as {@link #cook(Reader)}, but cooks a <em>set</em> of documents into one class.
     */
    void cook(Reader... readers) throws CompileException, IOException;

    /**
     * Same as {@link #cook(String, Reader)}, but cooks a <em>set</em> of documents into one class.
     * Notice that if <em>any</em> of the documents causes trouble, the entire compilation will fail. If you
     * need to report <em>which</em> of the documents causes the exception, you may want to use the
     * {@code optionalFileNames} parameter to distinguish between the individual token sources.
     */
    void cook(String[] fileNames, Reader[] readers) throws CompileException, IOException;

    /**
     * Same as {@link #cook(String)}, but cooks a <em>set</em> of documents into one class.
     */
    void cook(String... strings) throws CompileException;

    /**
     * Same as {@link #cook(String, String)}, but cooks a <em>set</em> of documents into one class.
     */
    void cook(String[] fileNames, String[] strings) throws CompileException;

    /**
     * Reads, scans, parses and compiles Java tokens from the given {@link InputStream}s, encoded
     * in the "platform default encoding".
     */
    void cook(InputStream[] inputStreams) throws CompileException, IOException;

    void cook(InputStream[] inputStreams, String[] encodings) throws CompileException, IOException;

    void cook(String[] fileNames, InputStream[] inputStreams) throws CompileException, IOException;

    void cook(String[] fileNames, InputStream[] inputStreams, String[] encodings) throws CompileException, IOException;

    void cookFiles(File[] files) throws CompileException, IOException;

    void cookFiles(File[] files, String[] encodings) throws CompileException, IOException;

    void cookFiles(String[] fileNames) throws CompileException, IOException;

    void cookFiles(String[] fileNames, String[] encodings) throws CompileException, IOException;
}
