
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Base class for a simple {@link ICookable}.
 * <p>
 *   Basically, it implements all those {@code cook()} convewnience methods, and leaves only {@link #cook(String, Reader)}
 *   unimplemented.
 * </p>
 */
public abstract
class Cookable implements ICookable {

    @Override public final void
    cook(Reader r) throws CompileException, IOException { this.cook(null, r); }

    @Override public final void
    cook(InputStream is) throws CompileException, IOException { this.cook(null, is); }

    @Override public final void
    cook(@Nullable String optionalFileName, InputStream is) throws CompileException, IOException {
        this.cook(optionalFileName, is, null);
    }

    @Override public final void
    cook(InputStream is, @Nullable String optionalEncoding) throws CompileException, IOException {
        this.cook(optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding));
    }

    @Override public final void
    cook(@Nullable String optionalFileName, InputStream is, @Nullable String optionalEncoding)
    throws CompileException, IOException {
        this.cook(
            optionalFileName,
            optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding)
        );
    }

    @Override public final void
    cook(String s) throws CompileException { this.cook((String) null, s); }

    @Override public final void
    cook(@Nullable String optionalFileName, String s) throws CompileException {
        try {
            this.cook(optionalFileName, new StringReader(s));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // SUPPRESS CHECKSTYLE AvoidHidingCause
            throw new RuntimeException("SNO: StringReader throws IOException");
        }
    }

    @Override public final void
    cookFile(File file) throws CompileException, IOException { this.cookFile(file, null); }

    @Override public final void
    cookFile(File file, @Nullable String optionalEncoding) throws CompileException, IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.cook(
                file.getAbsolutePath(),
                optionalEncoding == null ? new InputStreamReader(is) : new InputStreamReader(is, optionalEncoding)
            );
            is.close();
            is = null;
        } finally {
            if (is != null) try { is.close(); } catch (IOException ex) {}
        }
    }

    @Override public final void
    cookFile(String fileName) throws CompileException, IOException {
        this.cookFile(fileName, null);
    }

    @Override public final void
    cookFile(String fileName, @Nullable String optionalEncoding) throws CompileException, IOException {
        this.cookFile(new File(fileName), optionalEncoding);
    }

    /**
     * Reads all characters from the given {@link Reader} into a {@link String}.
     */
    public static String
    readString(Reader r) throws IOException {

        StringBuilder sb = new StringBuilder();
        char[]        ca = new char[4096];
        for (;;) {
            int count = r.read(ca);
            if (count == -1) break;
            sb.append(ca, 0, count);
        }
        String s = sb.toString();
        return s;
    }
}
