
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001, Arno Unkrig
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

package de.unkrig.jdisasm.io.charstream;

import java.io.EOFException;
import java.io.IOException;

/** Reads from a {@link String}. Notice that none of the overridden methods throw {@link IOException}. */
public
class StringCharStream implements CharStream {

    private final String in;
    private int          pos;

    public
    StringCharStream(String in) {
        this.in = in;
    }

    @Override public int
    peek() {
        return this.pos == this.in.length() ? -1 : this.in.charAt(this.pos);
    }

    @Override public boolean
    peek(char c) {
        return this.pos < this.in.length() && this.in.charAt(this.pos) == c;
    }

    @Override public int
    peek(String chars) {
        return this.pos == this.in.length() ? -1 : chars.indexOf(this.in.charAt(this.pos));
    }

    @Override public char
    read() throws EOFException {
        if (this.pos == this.in.length()) throw new EOFException("Unexpected end-of-input");
        return this.in.charAt(this.pos++);
    }

    @Override public void
    read(char c) throws EOFException, UnexpectedCharacterException {
        if (this.pos == this.in.length()) throw new EOFException("Expected '" + c + "' instead of end-of-input");
        if (this.in.charAt(this.pos) != c) {
            throw new UnexpectedCharacterException(
                "'"
                + c
                + "' expected instead of '"
                + this.in.substring(this.pos)
                + "'"
            );
        }
        this.pos++;
    }

    @Override public int
    read(String chars) throws EOFException, UnexpectedCharacterException {
        if (this.pos == this.in.length()) {
            throw new EOFException("Expected one of '" + chars + "' instead of end-of-input");
        }
        int res = chars.indexOf(this.in.charAt(this.pos));
        if (res == -1) {
            throw new UnexpectedCharacterException(
                "One of '"
                + chars
                + "' expected instead of '"
                + this.in.charAt(this.pos)
                + "'"
            );
        }
        this.pos++;
        return res;
    }

    @Override public boolean
    peekRead(char c) {
        if (this.pos >= this.in.length()) return false;
        if (this.in.charAt(this.pos) == c) {
            this.pos++;
            return true;
        }
        return false;
    }

    @Override public int
    peekRead(String chars) {
        if (this.pos >= this.in.length()) return -1;
        int res = chars.indexOf(this.in.charAt(this.pos));
        if (res != -1) this.pos++;
        return res;
    }

    @Override public void
    eoi() throws UnexpectedCharacterException {
        if (this.pos < this.in.length()) {
            throw new UnexpectedCharacterException(
                "Unexpected trailing characters '"
                + this.in.substring(this.pos)
                + "'"
            );
        }
    }

    @Override public boolean
    atEoi() {
        return this.pos >= this.in.length();
    }

    @Override public String
    toString() {
        return "'" + this.in + "' at offset " + this.pos;
    }
}
