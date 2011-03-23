
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

import java.io.*;

/**
 * Base class for a simple {@link ICookable}.
 */
public abstract class Cookable implements ICookable {

    public abstract void cook(
        String optionalFileName,
        Reader r
    ) throws CompileException, IOException;

    public final void cook(Reader r) throws CompileException, IOException {
        this.cook(null, r);
    }

    public final void cook(InputStream is) throws CompileException, IOException {
        this.cook(null, is);
    }

    public final void cook(
        String      optionalFileName,
        InputStream is
    ) throws CompileException, IOException {
        this.cook(optionalFileName, is, null);
    }

    public final void cook(
        InputStream is,
        String      optionalEncoding
    ) throws CompileException, IOException {
        this.cook(optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding));
    }

    public final void cook(
        String      optionalFileName,
        InputStream is,
        String      optionalEncoding
    ) throws CompileException, IOException {
        this.cook(
            optionalFileName,
            optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding)
        );
    }

    public void cook(String s) throws CompileException {
        this.cook((String) null, s);
    }

    public void cook(
        String optionalFileName,
        String s
    ) throws CompileException {
        try {
            this.cook(optionalFileName, new StringReader(s));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException("SNO: StringReader throws IOException");
        }
    }

    public final void cookFile(File file) throws CompileException, IOException {
        this.cookFile(file, null);
    }

    public final void cookFile(
        File   file,
        String optionalEncoding
    ) throws CompileException, IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.cook(
                file.getAbsolutePath(),
                optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding)
            );
            is.close();
            is = null;
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) { }
        }
    }

    public final void cookFile(String fileName) throws CompileException, IOException {
        this.cookFile(fileName, null);
    }

    public final void cookFile(
        String fileName,
        String optionalEncoding
    ) throws CompileException, IOException {
        this.cookFile(new File(fileName), optionalEncoding);
    }

    /**
     * Reads all characters from the given {@link Reader} into a {@link String}.
     */
    public static String readString(Reader r) throws IOException {
        StringBuffer sb = new StringBuffer();
        char[] ca = new char[4096];
        for (;;) {
            int count = r.read(ca);
            if (count == -1) break;
            sb.append(ca, 0, count);
        }
        String s = sb.toString();
        return s;
    }
}
