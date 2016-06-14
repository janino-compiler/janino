
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
import java.io.Reader;

/**
 * This interface produces a sequence of {@code char}s. These can either be "read", which means basically the same as
 * {@link Reader#read()}; or they can be "peeked", which means that the next character is returned, but not consumed,
 * i.e. the next call to {@link #read} or {@link #peek} will return that character again.
 */
public
interface CharStream {

    /**
     * A special value for the values returned by {@link #peek()} and {@link #peekRead(char)} indicating end-of-input.
     */
    int EOI = -1;

    /**
     * Returns the next character on this stream but does <b>not</b> consume it.
     *
     * @return {@link #EOI} This stream is at end-of-input
     */
    int peek() throws IOException;

    /**
     * Returns whether the character stream is not at end-of-input <i>and</i> the next character on this stream equals
     * the given character. Does <b>not</b> consume any characters.
     */
    boolean peek(char c) throws IOException;

    /**
     * Checks whether the next character on this stream equals any of the characters of the given {@link String}. Does
     * <b>not</b> consume any characters.
     *
     * @return The position of the next character in the given {@link String}, or -1
     */
    int peek(String chars) throws IOException;

    /**
     * Consumes and returns the next character on this stream.
     *
     * @throws EOFException This stream is at end-of-input
     */
    char read() throws EOFException, IOException;

    /**
     * Consumes the next character on this stream and verifies that it equals the given character.
     *
     * @throws EOFException                 This stream is at end-of-input
     * @throws UnexpectedCharacterException The next character does not equal the given character
     */
    void read(char c) throws EOFException, UnexpectedCharacterException;

    /**
     * Consumes the nect character and verifies that it matches one of the characters of the given {@link String}.
     *
     * @return                              The position of the next character in the given {@link String}
     * @throws EOFException                 This stream is at end-of-input
     * @throws UnexpectedCharacterException The next character on this stream is not in the given {@link String}
     */
    int read(String chars) throws EOFException, IOException, UnexpectedCharacterException;

    /**
     * If the next character on this stream equals the given character, it is consumed.
     *
     * @return {@codo true} iff the next character on this stream equals the given character
     */
    boolean peekRead(char c) throws IOException;

    /**
     * If the next character on this stream is in the given {@link String}, it is consumed.
     *
     * @return The position of the next character in the given {@link String}, or -1
     */
    int peekRead(String chars) throws IOException;

    /** @return Whether this stream is at end-of-input */
    boolean atEoi() throws IOException;

    /** @throws UnexpectedCharacterException This stream is <i>not</i> at end-of-input */
    void eoi() throws UnexpectedCharacterException;
}
