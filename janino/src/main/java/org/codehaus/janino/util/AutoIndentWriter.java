
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

package org.codehaus.janino.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link java.io.FilterWriter} that indents lines by processing some control characters in the character stream.
 * <p>
 *   {@link #INDENT} or {@link #UNINDENT} may precede lines and indicate that the line and all following lines should
 *   be (un)indented by one position.
 * </p>
 * <p>
 *   {@link #TABULATOR}s may appear anywhere in lines and dictate that portions of all following lines should be
 *   vertically aligned (see {@link #resolveTabs(List)}).
 * </p>
 */
public
class AutoIndentWriter extends FilterWriter {

    /**
     * Special character indicating a tabular layout of all following lines until {@link #UNINDENT}.
     */
    public static final char TABULATOR = 0xffff;

    /**
     * Special character at the beginning of a line that flushes a tabular layout.
     */
    public static final char CLEAR_TABULATORS = 0xfffe;

    /**
     * Special character at the beginning of a line that indents the following text by one position.
     */
    public static final char INDENT = 0xfffd;

    /**
     * Special character at the beginning of a line that unindents the following text by one position.
     */
    public static final char UNINDENT = 0xfffc;

    /**
     * Buffer for the "current line", including the trailing line break (CR, LF or CRLF).
     */
    private final StringBuilder lineBuffer = new StringBuilder();

    /**
     * The current indentation level; incremented by a {@link #INDENT} char at the beginning of a line, and decremented
     * by a {@link #UNINDENT} char at the beginning of a line.
     */
    private int indentation;

    /**
     * Iff non-null, then we are in "tab mode". While in tab mode, lines are not printed immediately, but stored in
     * this buffer. Tab mode starts when a line contains a {@link #TABULATOR}. Tab mode ends when output is unindented
     * beyond the level when tab mode started, or when this writer is closed.
     * <p>
     *   When tab mode ends, all buffered lines are vertically aligned at the {@link #TABULATOR}s and printed.
     * </p>
     */
    @Nullable private List<StringBuilder> tabulatorBuffer;

    /**
     * The {@link #indentation} when tab mode started.
     */
    private int tabulatorIndentation;

    public
    AutoIndentWriter(Writer out) { super(out); }

    @Override public void
    write(@Nullable char[] cbuf, int off, int len) throws IOException {
        assert cbuf != null;
        for (; len > 0; --len) this.write(cbuf[off++]);
    }

    @Override public void
    write(@Nullable String str, int off, int len) throws IOException {
        assert str != null;
        for (; len > 0; --len) this.write(str.charAt(off++));
    }

    @Override public void
    write(int c) throws IOException {
        if (c == '\n') {
            this.lineBuffer.append('\n');
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setLength(0);
            return;
        }
        if (this.lineBuffer.length() > 0 && this.lineBuffer.charAt(this.lineBuffer.length() - 1) == '\r') {

            // Non-LF char after CR, i.e. a "Mac line break", "\r":
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setCharAt(0, (char) c);
            this.lineBuffer.setLength(1);
            return;
        }
        this.lineBuffer.append((char) c);
    }

    private void
    line(String line) throws IOException {
        if (this.tabulatorBuffer != null) {
            this.tabulatorBuffer.add(new StringBuilder(line.length()).append(line));
            if (line.charAt(0) == AutoIndentWriter.INDENT) { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == AutoIndentWriter.UNINDENT && --this.indentation < this.tabulatorIndentation) {
                this.flushTabulatorBuffer();
            }
        } else
        if (line.indexOf(AutoIndentWriter.TABULATOR) != -1) {
            if (line.charAt(0) == AutoIndentWriter.INDENT)   { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == AutoIndentWriter.UNINDENT) { --this.indentation; line = line.substring(1); }

            this.tabulatorBuffer = new ArrayList<>();
            this.tabulatorBuffer.add(new StringBuilder(line.length()).append(line));
            this.tabulatorIndentation = this.indentation;
        } else
        {
            if (line.charAt(0) == AutoIndentWriter.CLEAR_TABULATORS) line = line.substring(1);
            if (line.charAt(0) == AutoIndentWriter.INDENT)           { ++this.indentation; line = line.substring(1); }
            if (line.charAt(0) == AutoIndentWriter.UNINDENT)         { --this.indentation; line = line.substring(1); }
            if ("\r\n".indexOf(line.charAt(0)) == -1) {
                for (int i = 0; i < this.indentation; ++i) this.out.write("    ");
            }
            this.out.write(line);
        }
    }

    private void
    flushTabulatorBuffer() throws IOException {
        List<List<StringBuilder>> lineGroups = new ArrayList<>();
        lineGroups.add(new ArrayList<StringBuilder>());

        List<StringBuilder> tb = this.tabulatorBuffer;
        assert tb != null;
        for (StringBuilder line : tb) {

            int idx = 0;
            if (line.charAt(0) == AutoIndentWriter.INDENT) {
                lineGroups.add(new ArrayList<StringBuilder>());
                ++idx;
            }
            if (line.charAt(idx) == AutoIndentWriter.UNINDENT) {
                AutoIndentWriter.resolveTabs((List<StringBuilder>) lineGroups.remove(lineGroups.size() - 1));
                ++idx;
            }
            if (line.charAt(idx) == AutoIndentWriter.CLEAR_TABULATORS) {
                List<StringBuilder> lg = (List<StringBuilder>) lineGroups.get(lineGroups.size() - 1);
                AutoIndentWriter.resolveTabs(lg);
                lg.clear();
                line.deleteCharAt(idx);
            }
            for (int i = 0; i < line.length(); ++i) {
                if (line.charAt(i) == AutoIndentWriter.TABULATOR) {
                    ((List<StringBuilder>) lineGroups.get(lineGroups.size() - 1)).add(line);
                }
            }
        }
        for (List<StringBuilder> lg : lineGroups) AutoIndentWriter.resolveTabs(lg);
        int ind = this.tabulatorIndentation;
        for (StringBuilder sb : tb) {
            String line = sb.toString();
            if (line.charAt(0) == AutoIndentWriter.INDENT) {
                ++ind;
                line = line.substring(1);
            }
            if (line.charAt(0) == AutoIndentWriter.UNINDENT) {
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
     * Expands all {@link #TABULATOR}s in the given {@link List} of {@link StringBuilder}s with
     * spaces, so that the characters immediately following the {@link #TABULATOR}s are vertically
     * aligned, like this:
     * <p>
     *   Input:
     * </p>
     * <pre>
     *   a @b @c\r\n
     *   aa @bb @cc\r\n
     *   aaa @bbb @ccc\r\n
     * </pre>
     * <p>
     *   Output:
     * </p>
     * <pre>
     *   a   b   c\r\n
     *   aa  bb  cc\r\n
     *   aaa bbb ccc\r\n
     * </pre>
     */
    private static void
    resolveTabs(List<StringBuilder> lineGroup) {

        // Determine the tabulator offsets for this line group.
        List<Integer> tabulatorOffsets = new ArrayList<>(); // 4, 4
        for (StringBuilder line : lineGroup) {
            int previousTab = 0;
            if (line.charAt(previousTab) == AutoIndentWriter.INDENT) ++previousTab;
            if (line.charAt(previousTab) == AutoIndentWriter.UNINDENT) ++previousTab;
            int tabCount    = 0;
            for (int i = previousTab; i < line.length(); ++i) {
                if (line.charAt(i) == AutoIndentWriter.TABULATOR) {
                    int tabOffset = i - previousTab;
                    previousTab = i;
                    if (tabCount >= tabulatorOffsets.size()) {
                        tabulatorOffsets.add(Integer.valueOf(tabOffset));
                    } else
                    {
                        if (tabOffset > ((Integer) tabulatorOffsets.get(tabCount)).intValue()) {
                            tabulatorOffsets.set(tabCount, Integer.valueOf(tabOffset));
                        }
                    }
                    ++tabCount;
                }
            }
        }

        // Replace tabulators with spaces.
        for (Iterator<StringBuilder> it = lineGroup.iterator(); it.hasNext();) {
            StringBuilder line        = (StringBuilder) it.next();
            int           tabCount    = 0;
            int           previousTab = 0;
            if (line.charAt(previousTab) == AutoIndentWriter.INDENT) ++previousTab;
            if (line.charAt(previousTab) == AutoIndentWriter.UNINDENT) ++previousTab;
            for (int i = previousTab; i < line.length(); ++i) {
                if (line.charAt(i) == AutoIndentWriter.TABULATOR) {
                    int tabOffset = i - previousTab;
                    int n         = ((Integer) tabulatorOffsets.get(tabCount++)).intValue() - tabOffset;
                    line.replace(i, i + 1, AutoIndentWriter.spaces(n));
                    i           += n - 1;
                    previousTab = i;
                }
            }
        }
    }

    /**
     * @return a {@link String} of {@code n} spaces
     */
    private static String
    spaces(int n) {
        if (n < 30) return "                              ".substring(0, n);
        char[] data = new char[n];
        Arrays.fill(data, ' ');
        return String.valueOf(data);
    }

    @Override public void
    close() throws IOException {
        if (this.lineBuffer.length() > 0) {
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setLength(0);
        }
        if (this.tabulatorBuffer != null) this.flushTabulatorBuffer();
        this.out.close();
    }

    @Override public void
    flush() throws IOException {
        if (this.lineBuffer.length() > 0) {
            this.line(this.lineBuffer.toString());
            this.lineBuffer.setLength(0);
        }
        if (this.tabulatorBuffer != null) this.flushTabulatorBuffer();
        this.out.flush();
    }
}
