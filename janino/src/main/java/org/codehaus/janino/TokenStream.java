
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino;

import java.io.IOException;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
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
     * @return Whether the type of the next token is <var>suspected</var>
     */
    boolean
    peek(TokenType suspected) throws CompileException, IOException;

    /**
     * Checks whether the type of the next token is any of the <var>suspected</var>; does not consume the next token.
     *
     * @return The index of the first of the <var>suspected</var> types that is the next token's type, or -1 if the type
     *         of the next token is none of the <var>suspected</var> types
     */
    int
    peek(TokenType... suspected) throws CompileException, IOException;

    /**
     * @return The next-but-one token, but consumes neither the next nor the next-but-one token
     */
    Token
    peekNextButOne() throws CompileException, IOException;

    /**
     * @return Whether the value of the next-but-one token equals the <var>suspected</var>; consumes neither the next
     *         nor the next-but-one token
     */
    boolean
    peekNextButOne(String suspected) throws CompileException, IOException;

    /**
     * @return The next token, which it also consumes, or {@code null} iff the scanner is at end-of-input
     */
    Token
    read() throws CompileException, IOException;

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
     * Verifies that the type of the next token is the <var>expected</var>, and consumes the token.
     *
     * @return                  The value of the next token; an {@link String#intern() interned} String iff the token
     *                          represents an identifier, {@code true}, {@code false}, {@code null}, or an operator
     * @throws CompileException The next token's type is not the <var>expected</var>
     */
    String
    read(TokenType expected) throws CompileException, IOException;

    /**
     * Verifies that the type of the next token is one of the <var>expected</var>, and consumes the token.
     *
     * @return                  The index of the first of the <var>expected</var> types that is the next token's type;
     *                          -1 if the type of the next token is none of the <var>expected</var>
     * @throws CompileException The next token's type is none of the <var>expected</var>
     */
    int
    read(TokenType... expected) throws CompileException, IOException;

    /**
     * Checks whether the value of the next token equals the <var>suspected</var>; if so, consumes the token.
     */
    boolean
    peekRead(String suspected) throws CompileException, IOException;

    /**
     * Checks whether the value of the next token is one of the <var>suspected</var>; if so, consumes the token.
     *
     * @return The index of the first of the <var>suspected</var> that equals the next token's value; -1 iff the next
     *         token's value equals none of the <var>suspected</var>
     */
    int
    peekRead(String... suspected) throws CompileException, IOException;

    /**
     * Checks whether the type of the next token is the <var>suspected</var>; if so, consumes the token.
     *
     * @return The value of the next token; an {@link String#intern() interned} String iff the token represents an
     *         identifier, {@code true}, {@code false}, {@code null}, or an operator
     */
    @Nullable String
    peekRead(TokenType suspected) throws CompileException, IOException;

    /**
     * Checks whether the type of the next token is one of the <var>suspected</var>; if so, consumes the token.
     *
     * @return The index of the elements that is the next token's type, or -1 iff the type of next token is none of
     *         the <var>suspected</var>
     */
    int
    peekRead(TokenType... suspected) throws CompileException, IOException;

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

    /**
     * @return The location of the first character of the previously <em>read</em> (not <em>peek</em>ed!) token
     */
    Location
    location();
}
