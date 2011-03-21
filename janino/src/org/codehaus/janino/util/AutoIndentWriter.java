
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

package org.codehaus.janino.util;

import java.io.*;
import java.util.*;

/**
 * A {@link java.io.FilterWriter} that automatically indents lines by looking at
 * trailing opening braces ('{') and leading closing braces ('}').
 */
public class AutoIndentWriter extends FilterWriter {
    public static final char TABULATOR        = 0xffff;
    public static final char CLEAR_TABULATORS = 0xfffe;
    public static final char INDENT           = 0xfffd;
    public static final char UNINDENT         = 0xfffc;

    StringBuffer           lineBuffer = new StringBuffer();
    int                    indentation = 0;
    List/*<StringBuffer>*/ tabulatorBuffer = null;
    int                    tabulatorIndentation;

    public AutoIndentWriter(Writer out) {
        super(out);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        for (; len > 0; --len) this.write(cbuf[off++]);
    }

    public void write(String str, int off, int len) throws IOException {
        for (; len > 0; --len) this.write(str.charAt(off++));
    }

    public void write(int c) throws IOException {
        if (c == '\n') {
            this.lineBuffer.append('\n');
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setLength(0);
            return;
        }
        if (this.lineBuffer.length() > 0 && this.lineBuffer.charAt(this.lineBuffer.length() - 1) == '\r') {
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setCharAt(0, (char) c);
            this.lineBuffer.setLength(1);
            return;
        }
        this.lineBuffer.append((char) c);
    }

    private void line(String line) throws IOException {
        if (this.tabulatorBuffer != null) {
            this.tabulatorBuffer.add(new StringBuffer(line.length()).append(line));
            if (line.charAt(0) == INDENT) { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == UNINDENT && --this.indentation < this.tabulatorIndentation) {
                this.flushTabulatorBuffer();
            }
        } else
        if (line.indexOf(TABULATOR) != -1) {
            if (line.charAt(0) == INDENT)   { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == UNINDENT) { --this.indentation; line = line.substring(1); }
            this.tabulatorBuffer = new ArrayList/*<StringBuffer>*/();
            this.tabulatorBuffer.add(new StringBuffer(line.length()).append(line));
            this.tabulatorIndentation = this.indentation;
        } else
        {
            if (line.charAt(0) == CLEAR_TABULATORS) line = line.substring(1);
            if (line.charAt(0) == INDENT)           { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == UNINDENT)         { --this.indentation; line = line.substring(1); }
            if ("\r\n".indexOf(line.charAt(0)) == -1) {
                for (int i = 0; i < this.indentation; ++i) this.out.write("    ");
            }
            this.out.write(line);
        }
    }

    private void flushTabulatorBuffer() throws IOException {
        List/*<List<StringBuffer>>*/ lineGroups = new ArrayList();
        lineGroups.add(new ArrayList/*<StringBuffer>*/());

        for (Iterator/*<StringBuffer>*/ it = this.tabulatorBuffer.iterator(); it.hasNext();) {
            StringBuffer line = (StringBuffer) it.next();
            int idx = 0;
            if (line.charAt(0) == INDENT) {
                lineGroups.add(new ArrayList/*<StringBuffer>*/());
                ++idx;
            }
            if (line.charAt(idx) == UNINDENT) {
                this.resolveTabs((List/*<StringBuffer>*/) lineGroups.remove(lineGroups.size() - 1));
                ++idx;
            }
            if (line.charAt(idx) == CLEAR_TABULATORS) {
                List/*<StringBuffer>*/ lg = (List/*<LineGroup>*/) lineGroups.get(lineGroups.size() - 1);
                this.resolveTabs(lg);
                lg.clear();
                line.deleteCharAt(idx);
            }
            for (int i = 0; i < line.length(); ++i) {
                if (line.charAt(i) == TABULATOR) {
                    ((List/*<StringBuffer>*/) lineGroups.get(lineGroups.size() - 1)).add(line);
                }
            }
        }
        for (Iterator/*<List<StringBuffer>>*/ it = lineGroups.iterator(); it.hasNext();) {
            this.resolveTabs((List/*<StringBuffer)*/) it.next());
        }
        int ind = this.tabulatorIndentation;
        for (Iterator/*<StringBuffer>*/ it = this.tabulatorBuffer.iterator(); it.hasNext();) {
            String line = ((StringBuffer) it.next()).toString();
            if (line.charAt(0) == INDENT) {
                ++ind;
                line = line.substring(1);
            }
            if (line.charAt(0) == UNINDENT) {
                --ind;
                line = line.substring(1);
            }
            if ("\r\n".indexOf(line.charAt(0)) == -1) {
                for (int i = 0; i < ind; ++i) this.out.write("    ");
            }
            this.out.write(line.toString());
        }
        this.tabulatorBuffer = null;
    }

    /**
     * Expands all {@link #TABULATOR}s in the given {@link List} of {@link StringBuffer}s with
     * spaces, so that the characters immediately following the {@link #TABULATOR}s are vertically
     * aligned, like this:
     * <p>
     * Input:<pre>
     *   a @b @c\r\n
     *   aa @bb @cc\r\n
     *   aaa @bbb @ccc\r\n</pre>Output:<pre>
     *   a   b   c\r\n
     *   aa  bb  cc\r\n
     *   aaa bbb ccc\r\n</pre>
     */
    private void resolveTabs(List/*<StringBuffer>*/ lineGroup) {

        // Determine the tabulator offsets for this line group.
        List/*<Integer>*/ tabulatorOffsets = new ArrayList/*<Integer>*/(); // 4, 4
        for (Iterator/*<StringBuffer>*/ it = lineGroup.iterator(); it.hasNext();) {
            StringBuffer line = (StringBuffer) it.next();
            int          tabCount = 0;
            int          previousTab = 0;
            if (line.charAt(previousTab) == INDENT) ++previousTab;
            if (line.charAt(previousTab) == UNINDENT) ++previousTab;
            for (int i = previousTab; i < line.length(); ++i) {
                if (line.charAt(i) == TABULATOR) {
                    int tabOffset = i - previousTab;
                    previousTab = i;
                    if (tabCount >= tabulatorOffsets.size()) {
                        tabulatorOffsets.add(new Integer(tabOffset));
                    } else
                    {
                        if (tabOffset > ((Integer) tabulatorOffsets.get(tabCount)).intValue()) {
                            tabulatorOffsets.set(tabCount, new Integer(tabOffset));
                        }
                    }
                    ++tabCount;
                }
            }
        }

        // Replace tabulators with spaces.
        for (Iterator/*<StringBuffer>*/ it = lineGroup.iterator(); it.hasNext();) {
            StringBuffer line = (StringBuffer) it.next();
            int tabCount = 0;
            int previousTab = 0;
            if (line.charAt(previousTab) == INDENT) ++previousTab;
            if (line.charAt(previousTab) == UNINDENT) ++previousTab;
            for (int i = previousTab; i < line.length(); ++i) {
                if (line.charAt(i) == TABULATOR) {
                    int tabOffset = i - previousTab;
                    int n = ((Integer) tabulatorOffsets.get(tabCount++)).intValue() - tabOffset;
                    line.replace(i, i + 1, spaces(n));
                    i += n - 1;
                    previousTab = i;
                }
            }
        }
    }

    /**
     * @return a {@link String} of <code>n</code> spaces
     */
    private String spaces(int n) {
        if (n < 30) return "                              ".substring(0, n);
        char[] data = new char[n];
        Arrays.fill(data, ' ');
        return String.valueOf(data);
    }

    public void close() throws IOException {
        if (this.tabulatorBuffer != null) this.flushTabulatorBuffer();
        if (this.lineBuffer.length() > 0) this.line(this.lineBuffer.toString());
        this.out.close();
    }

    public void flush() throws IOException {
        if (this.tabulatorBuffer != null) this.flushTabulatorBuffer();
        if (this.lineBuffer.length() > 0) {
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setLength(0);
        }
        this.out.flush();
    }
}
