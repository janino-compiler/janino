
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

package org.codehaus.janino.util;

import java.io.*;

/**
 * A {@link java.io.FilterWriter} that automatically indents lines by looking at
 * trailing opening braces ('{') and leading closing braces ('}').
 */
public class AutoIndentWriter extends FilterWriter {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private int                 previousChar = -1;
    private int                 indentation = 0;
    private String              prefix = null;

    public AutoIndentWriter(Writer out) {
        super(out);
    }

    public void write(int c) throws IOException {
        if (AutoIndentWriter.isLineSeparatorChar(c)) {
            if (this.previousChar == '{') this.indent();
        } else
        if (AutoIndentWriter.isLineSeparatorChar(this.previousChar)) {
            if (c == '}') this.unindent();
            for (int i = 0; i < this.indentation; ++i) this.out.write("    ");
            if (this.prefix != null) this.out.write(this.prefix);
        }
        super.write(c);
        this.previousChar = c;
    }

    public void unindent() {
        --this.indentation;
    }

    public void indent() {
        ++this.indentation;
    }

    /**
     * The prefix, if non-null, is printed between the indentation space and
     * the line data.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (; len > 0; --len) this.write(cbuf[off++]);
    }
    public void write(String str, int off, int len) throws IOException {
        for (; len > 0; --len) this.write(str.charAt(off++));
    }

    private static boolean isLineSeparatorChar(int c) {
        return AutoIndentWriter.LINE_SEPARATOR.indexOf(c) != -1;
    }
}
