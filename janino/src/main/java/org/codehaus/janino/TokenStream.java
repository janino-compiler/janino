
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016, Arno Unkrig
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

package org.codehaus.janino;

import java.io.IOException;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * An interface that allows for peeking and consuming a stream of {@link Token}s.
 */
public
interface TokenStream {

    /**
     * @return The next token, but does not consume it
     */
    Token
    peek() throws CompileException, IOException;

    /**
     * @return The next-but-one token, but consumes neither the next nor the next-but-one token
     */
    Token
    peekNextButOne() throws CompileException, IOException;

    /**
     * @return The next and also consumes it, or {@code null} iff the scanner is at end-of-input
     */
    Token
    read() throws CompileException, IOException;

    // Peek/read/peekRead convenience methods.

    /**
     * @return Whether the value of the next token equals <var>suspected</var>; does not consume the next token
     */
    boolean
    peek(String suspected) throws CompileException, IOException;

    /**
     * Checks whether the value of the next token equals any of the <var>suspected</var>; does not consume the next
     * token.
     *
     * @return The index of the first of the <var>suspected</var> that equals the value of the next token, or -1 if the
     *         value of the next token equals none of the <var>suspected</var>
     */
    int
    peek(String... suspected) throws CompileException, IOException;

    /**
     * Checks whether the type of the next token is any of the <var>suspected</var>; does not consume the next token.
     *
     * @return The index of the first of the <var>suspected</var> types that is the next token's type, or -1 if the type
     *         of the next token is none of the <var>suspected</var> types
     */
    int
    peek(TokenType... suspected) throws CompileException, IOException;

    /**
     * @return Whether the value of the next-but-one token equals the <var>suspected</var>; consumes neither the next
     *         nor the next-but-one token
     */
    boolean
    peekNextButOne(String suspected) throws CompileException, IOException;

    /**
     * Verifies that the value of the next token equals <var>expected</var>, and consumes the token.
     *
     * @throws CompileException The value of the next token does not equal <var>expected</var> (this includes the case
     *                          that  the scanner is at end-of-input)
     */
    void
    read(String expected) throws CompileException, IOException;

    /**
     * Verifies that the value of the next token equals one of the <var>expected</var>, and consumes the token.
     *
     * @return                  The index of the consumed token within <var>expected</var>
     * @throws CompileException The value of the next token does not equal any of the <var>expected</var> (this includes
     *                          the case where the scanner is at end-of-input)
     */
    int
    read(String... expected) throws CompileException, IOException;

    /**
     * @return Whether the value of the next token equals the <var>suspected</var>; if so, it consumes the next token
     * @throws CompileException
     * @throws IOException
     */
    boolean
    peekRead(String suspected) throws CompileException, IOException;

    /**
     * @return The index of the elements that equals the next token, or -1 iff the next token is none of the
     *         <var>suspected</var>
     */
    int
    peekRead(String... suspected) throws CompileException, IOException;

    /**
     * @return Whether the scanner is at end-of-input
     */
    boolean
    peekEof() throws CompileException, IOException;

    /**
     * @return {@code null} iff the next token is not an identifier, otherwise the value of the identifier token
     */
    @Nullable String
    peekIdentifier() throws CompileException, IOException;

    /**
     * @return Whether the next token is a literal
     */
    boolean
    peekLiteral() throws CompileException, IOException;

    /**
     * @return                  The value of the next token, which is an indentifier
     * @throws CompileException The next token is not an identifier
     */
    String
    readIdentifier() throws CompileException, IOException;

    /**
     * @return                  The value of the next token, which is an operator
     * @throws CompileException The next token is not an operator
     */
    String
    readOperator() throws CompileException, IOException;

    /**
     * By default, warnings are discarded, but an application my install a {@link WarningHandler}.
     * <p>
     *   Notice that there is no {@code Parser.setErrorHandler()} method, but parse errors always throw a {@link
     *   CompileException}. The reason being is that there is no reasonable way to recover from parse errors and
     *   continue parsing, so there is no need to install a custom parse error handler.
     * </p>
     *
     * @param optionalWarningHandler {@code null} to indicate that no warnings be issued
     */
    void
    setWarningHandler(@Nullable WarningHandler optionalWarningHandler);
}
