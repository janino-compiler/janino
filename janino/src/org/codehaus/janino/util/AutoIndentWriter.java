
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * A {@link java.io.FilterWriter} that automatically indents lines by looking at
 * trailing opening braces ('{') and leading closing braces ('}').
 */
public class AutoIndentWriter extends FilterWriter {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private int previousChar = -1;
    private int indentation = 0;

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
