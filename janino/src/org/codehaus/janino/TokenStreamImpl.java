
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

package org.codehaus.janino;

import java.io.IOException;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Scanner.Token;
import org.codehaus.janino.Scanner.TokenType;

/**
 * Standard implementation for the {@link TokenStream}.
 */
public
class TokenStreamImpl implements TokenStream {

    private final Scanner scanner;

    public
    TokenStreamImpl(Scanner scanner) { (this.scanner = scanner).setIgnoreWhiteSpace(true); }

    /**
     * The optional JAVADOC comment preceding the {@link #nextToken}.
     */
    @Nullable private String optionalDocComment;

    /**
     * Whether the scanner is in 'expect greater' mode: If so, it parses character sequences like "{@code >>>=}" as
     * "{@code >}", "{@code >}", "{@code >}", "{@code =}".
     */
    private boolean expectGreater;

    /**
     * While the scanner is in "expect greater mode", then the char sequence ">>" is scanned into two ">" tokens.
     *
     * @return Whether the scanner is currently in 'expect greater' mode
     */
    @Override public boolean
    getExpectGreater() { return this.expectGreater; }

    /**
     * While the scanner is in "expect greater mode", then the char sequence ">>" is scanned into two ">" tokens.
     *
     * @param value Whether "expect greater mode" should be activated
     * @return      Whether "expect greater mode" was previously active
     */
    @Override public boolean
    setExpectGreater(boolean value) {
        boolean result = this.expectGreater;
        this.expectGreater = value;
        return result;
    }

    private Scanner.Token
    produceToken() throws CompileException, IOException {

        if (this.pushback != null) {
            Token result = this.pushback;
            this.pushback = null;
            return result;
        }

        for (;;) {
            Token token = this.scanner.produce();

            switch (token.type) {

            case WHITE_SPACE:
            case C_PLUS_PLUS_STYLE_COMMENT:
                break;

            case C_STYLE_COMMENT:
                if (token.value.startsWith("/**")) {
                    if (TokenStreamImpl.this.optionalDocComment != null) {
                        TokenStreamImpl.this.warning("MDC", "Misplaced doc comment", this.scanner.location());
                        TokenStreamImpl.this.optionalDocComment = null;
                    }
                }
                break;

            default:
                if (
                    TokenStreamImpl.this.expectGreater
                    && token.type == TokenType.OPERATOR
                    && token.value.startsWith(">>")
                ) {
                    this.pushback = this.scanner.new Token(TokenType.OPERATOR, token.value.substring(1));
                    return this.scanner.new Token(TokenType.OPERATOR, ">");
                }
                return token;
            }
        }
    }
    @Nullable private Token pushback;

    /**
     * Gets the text of the doc comment (a.k.a. "JAVADOC comment") preceeding the next token.
     *
     * @return {@code null} if the next token is not preceeded by a doc comment
     */
    @Nullable public String
    doc() {
        String s = this.optionalDocComment;
        this.optionalDocComment = null;
        return s;
    }

    @Nullable private Token nextToken, nextButOneToken;

    // Token-level methods.

    @Override public Token
    peek() throws CompileException, IOException {
        if (this.nextToken == null) this.nextToken = this.produceToken();
        assert this.nextToken != null;
        return this.nextToken;
    }

    @Override public Token
    peekNextButOne() throws CompileException, IOException {
        if (this.nextToken == null) this.nextToken = this.produceToken();
        if (this.nextButOneToken == null) this.nextButOneToken = this.produceToken();
        assert this.nextButOneToken != null;
        return this.nextButOneToken;
    }

    @Override public Token
    read() throws CompileException, IOException {

        if (this.nextToken == null) return this.produceToken();

        final Token result = this.nextToken;
        assert result != null;

        this.nextToken       = this.nextButOneToken;
        this.nextButOneToken = null;
        return result;
    }

    // Peek/read/peekRead convenience methods.

    @Override public boolean
    peek(String suspected) throws CompileException, IOException {
        return this.peek().value.equals(suspected);
    }

    @Override public int
    peek(String[] suspected) throws CompileException, IOException {
        return TokenStreamImpl.indexOf(suspected, this.peek().value);
    }

    @Override public int
    peek(TokenType[] suspected) throws CompileException, IOException {
        return TokenStreamImpl.indexOf(suspected, this.peek().type);
    }

    @Override public boolean
    peekNextButOne(String suspected) throws CompileException, IOException {
        return this.peekNextButOne().value.equals(suspected);
    }

    @Override public void
    read(String expected) throws CompileException, IOException {
        String s = this.read().value;
        if (!s.equals(expected)) throw this.compileException("'" + expected + "' expected instead of '" + s + "'");
    }

    @Override public int
    read(String[] expected) throws CompileException, IOException {

        String s = this.read().value;

        int idx = TokenStreamImpl.indexOf(expected, s);
        if (idx == -1) {
            throw this.compileException(
                "One of '"
                + TokenStreamImpl.join(expected, " ")
                + "' expected instead of '"
                + s
                + "'"
            );
        }
        return idx;
    }

    @Override public boolean
    peekRead(String suspected) throws CompileException, IOException {

        if (this.nextToken != null) {
            if (!this.nextToken.value.equals(suspected)) return false;
            this.nextToken       = this.nextButOneToken;
            this.nextButOneToken = null;
            return true;
        }

        Token t = this.produceToken();
        if (t.value.equals(suspected)) return true;
        this.nextToken = t;
        return false;
    }

    @Override public int
    peekRead(String[] suspected) throws CompileException, IOException {
        if (this.nextToken != null) {
            int idx = TokenStreamImpl.indexOf(suspected, this.nextToken.value);
            if (idx == -1) return -1;
            this.nextToken       = this.nextButOneToken;
            this.nextButOneToken = null;
            return idx;
        }
        Token t   = this.produceToken();
        int   idx = TokenStreamImpl.indexOf(suspected, t.value);
        if (idx != -1) return idx;
        this.nextToken = t;
        return -1;
    }

    @Override public boolean
    peekEof() throws CompileException, IOException {
        return this.peek().type == TokenType.END_OF_INPUT;
    }

    @Override @Nullable public String
    peekIdentifier() throws CompileException, IOException {
        Token t = this.peek();
        return t.type == TokenType.IDENTIFIER ? t.value : null;
    }

    @Override public boolean
    peekLiteral() throws CompileException, IOException {
        return this.peek(new TokenType[] {
            TokenType.INTEGER_LITERAL, TokenType.FLOATING_POINT_LITERAL, TokenType.BOOLEAN_LITERAL,
            TokenType.CHARACTER_LITERAL, TokenType.STRING_LITERAL, TokenType.NULL_LITERAL,
        }) != -1;
    }

    @Override public String
    readIdentifier() throws CompileException, IOException {
        Token t = this.read();
        if (t.type != TokenType.IDENTIFIER) {
            throw this.compileException("Identifier expected instead of '" + t.value + "'");
        }
        return t.value;
    }

    @Override public String
    readOperator() throws CompileException, IOException {
        Token t = this.read();
        if (t.type != TokenType.OPERATOR) {
            throw this.compileException("Operator expected instead of '" + t.value + "'");
        }
        return t.value;
    }

    private static int
    indexOf(String[] strings, String subject) {
        for (int i = 0; i < strings.length; ++i) {
            if (strings[i].equals(subject)) return i;
        }
        return -1;
    }

    private static int
    indexOf(TokenType[] tta, TokenType subject) {
        for (int i = 0; i < tta.length; ++i) {
            if (tta[i].equals(subject)) return i;
        }
        return -1;
    }

    @Override public void
    setWarningHandler(@Nullable WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    // Used for elaborate warning handling.
    @Nullable private WarningHandler optionalWarningHandler;

    /**
     * Issues a warning with the given message and location and returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The {@code handle} argument qualifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     *
     * @throws CompileException The optionally installed {@link WarningHandler} decided to throw a {@link
     *                          CompileException}
     */
    private void
    warning(String handle, String message, @Nullable Location optionalLocation) throws CompileException {
        if (this.optionalWarningHandler != null) {
            this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
        }
    }

    /**
     * Convenience method for throwing a {@link CompileException}.
     */
    protected final CompileException
    compileException(String message) { return new CompileException(message, this.scanner.location()); }

    private static String
    join(@Nullable String[] sa, String separator) {

        if (sa == null) return ("(null)");

        if (sa.length == 0) return ("(zero length array)");
        StringBuilder sb = new StringBuilder(sa[0]);
        for (int i = 1; i < sa.length; ++i) {
            sb.append(separator).append(sa[i]);
        }
        return sb.toString();
    }
}
