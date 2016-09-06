
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

/**
 * Enumeration of the types of {@link Token}s that the {@link Scanner} produces.
 */
public
enum TokenType {

    /**
     * Indicates the "end-of-input" condition.
     */
    END_OF_INPUT,

    /**
     * The token represents a Java identifier.
     */
    IDENTIFIER,

    /**
     * The token represents a Java keyword. ({@code "true"}, {@code "false"} and {@code "null"} are <em>not</em>
     * Java keywords, but {@link #BOOLEAN_LITERAL}s and {@link #NULL_LITERAL}s.)
     */
    KEYWORD,

    /**
     * The token represents an integer literal; its {@link Token#value} is the text of the integer literal exactly
     * as it appears in the source code (e.g. "0", "123", "123L", "03ff", "0xffff", "0b10101010").
     */
    INTEGER_LITERAL,

    /**
     * The token represents a floating-point literal; its {@link Token#value} is the text of the floating-point
     * literal exactly as it appears in the source code (e.g. "1.23", "1.23F", "1.23D", "1.", ".1", "1E13").
     */
    FLOATING_POINT_LITERAL,

    /**
     * The token represents a boolean literal; its {@link Token#value} is either 'true' or 'false'.
     */
    BOOLEAN_LITERAL,

    /**
     * The token represents a character literal; its {@link Token#value} is the text of the character literal
     * exactly as it appears in the source code (including the single quotes around it).
     */
    CHARACTER_LITERAL,

    /**
     * The token represents a string literal; its {@link Token#value} is the text of the string literal exactly as
     * it appears in the source code (including the double quotes around it).
     */
    STRING_LITERAL,

    /**
     * The token represents the {@code null} literal; its {@link Token#value} is 'null'.
     */
    NULL_LITERAL,

    /**
     * The token represents an operator; its {@link Token#value} is exactly the particular operator (e.g.
     * "&lt;&lt;&lt;=").
     */
    OPERATOR,

    /**
     * The token represents "white space"; i.e. a non-empty sequence of whitespace characters. Specifically, any
     * line terminators appear exactly as in the inut stream. JLS8 3.6
     */
    WHITE_SPACE,

    /**
     * The token represents a C++-style comment like "{@code // This is a C++-style comment.}". Notice that the
     * line terminator is <em>not</em> part of the comment; hence, this token is always followed by a {@link
     * #WHITE_SPACE} token (or by {@link #END_OF_INPUT}).
     */
    C_PLUS_PLUS_STYLE_COMMENT,

    /**
     * The token represents a C-style comment, like "{@code /* This is a C-style comment. &#42;/}", which may
     * span multiple lines. In the latter case, the enclosed line terminators appear exactly as in the input
     * stream.
     */
    C_STYLE_COMMENT,
}
