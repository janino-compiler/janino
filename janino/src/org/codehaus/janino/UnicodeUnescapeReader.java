
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

package org.codehaus.janino;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A {@link FilterReader} that unescapes the "Unicode Escapes"
 * as described in
 * <a href="http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#100850">the
 * Java Language Specification, 2nd edition</a>.
 */
public class UnicodeUnescapeReader extends FilterReader {

    /**
     * @param in
     */
    public UnicodeUnescapeReader(Reader in) {
        super(in);
    }

    /**
     * Override {@link FilterReader#read()}.
     */
    public int read() throws IOException {
        int c;

        // Read next character.
        if (this.unreadChar == -1) {
            c = this.in.read();
        } else {
            c = this.unreadChar;
            this.unreadChar = -1;
        }

        // Check for backslash-u escape sequence, preceeded with an even number
        // of backslashes.
        if (c != '\\' || this.oddPrecedingBackslashes) {
            this.oddPrecedingBackslashes = false;
            return c;
        }

        // Read one character ahead and check if it is a "u".
        c = this.in.read();
        if (c != 'u') {
            this.unreadChar = c;
            this.oddPrecedingBackslashes = true;
            return '\\';
        }

        // Skip redundant "u"s.
        do {
            c = this.in.read();
            if (c == -1) throw new IOException("Incomplete escape sequence");
        } while (c == 'u');

        // Decode escape sequence.
        char[] ca = new char[4];
        ca[0] = (char) c;
        if (this.in.read(ca, 1, 3) != 3) throw new IOException("Incomplete escape sequence");
        try {
            return 0xffff & Integer.parseInt(new String(ca), 16);
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid escape sequence \"\\u" + new String(ca) + "\"");
        }
    }

    /**
     * Override {@link FilterReader#read(char[], int, int)}.
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len == 0) return 0;
        int res = 0;
        do {
            int c = this.read();
            if (c == -1) break;
            cbuf[off++] = (char) c;
        } while (++res < len);
        return res == 0 ? -1 : res;
    }

    /**
     * Simple unit testing.
     */
    public static void main(String[] args) throws IOException {
        Reader r = new UnicodeUnescapeReader(new StringReader(args[0]));
        for (;;) {
            int c = r.read();
            if (c == -1) break;
            System.out.print((char) c);
        }
        System.out.println();
    }

    private int     unreadChar = -1; // -1 == none
    private boolean oddPrecedingBackslashes = false;
}
