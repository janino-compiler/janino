
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.TeeReader;

/**
 * Splits up a character stream into tokens and returns them as
 * {@link java.lang.String String} objects.
 * <p>
 * The <code>optionalFileName</code> parameter passed to many
 * constructors should point
 */
public
class Scanner {

    // Public Scanners that read from a file.

    /**
     * Set up a scanner that reads tokens from the given file in the default charset.
     * <p>
     * <b>This method is deprecated because it leaves the input file open.</b>
     *
     * @deprecated // SUPPRESS CHECKSTYLE MissingDeprecated
     */
    @Deprecated public
    Scanner(String fileName) throws IOException {
        this(
            fileName,                     // optionalFileName
            new FileInputStream(fileName) // is
        );
    }

    /**
     * Set up a scanner that reads tokens from the given file in the given encoding.
     * <p>
     * <b>This method is deprecated because it leaves the input file open.</b>
     *
     * @deprecated // SUPPRESS CHECKSTYLE MissingDeprecated
     */
    @Deprecated public
    Scanner(String fileName, String encoding) throws IOException {
        this(
            fileName,                      // optionalFileName
            new FileInputStream(fileName), // is
            encoding                       // optionalEncoding
        );
    }

    /**
     * Set up a scanner that reads tokens from the given file in the platform
     * default encoding.
     * <p>
     * <b>This method is deprecated because it leaves the input file open.</b>
     *
     * @deprecated // SUPPRESS CHECKSTYLE MissingDeprecated
     */
    @Deprecated public
    Scanner(File file) throws IOException {
        this(
            file.getAbsolutePath(),    // optionalFileName
            new FileInputStream(file), // is
            null                       // optionalEncoding
        );
    }

    /**
     * Set up a scanner that reads tokens from the given file in the given encoding.
     * <p>
     * <b>This method is deprecated because it leaves the input file open.</b>
     *
     * @deprecated // SUPPRESS CHECKSTYLE MissingDeprecated
     */
    @Deprecated public
    Scanner(File file, @Nullable String optionalEncoding) throws IOException {
        this(
            file.getAbsolutePath(),    // optionalFileName
            new FileInputStream(file), // is
            optionalEncoding           // optionalEncoding
        );
    }

    // Public Scanners that read from an InputStream

    /**
     * Set up a scanner that reads tokens from the given
     * {@link InputStream} in the platform default encoding.
     * <p>
     * The <code>fileName</code> is solely used for reporting in thrown
     * exceptions.
     */
    public
    Scanner(@Nullable String optionalFileName, InputStream is) throws IOException {
        this(
            optionalFileName,
            new InputStreamReader(is), // in
            (short) 1,                 // initialLineNumber
            (short) 0                  // initialColumnNumber
        );
    }

    /**
     * Set up a scanner that reads tokens from the given
     * {@link InputStream} with the given <code>optionalEncoding</code>
     * (<code>null</code> means platform default encoding).
     * <p>
     * The <code>optionalFileName</code> is used for reporting errors during
     * compilation and for source level debugging, and should name an existing
     * file. If <code>null</code> is passed, and the system property
     * <code>org.codehaus.janino.source_debugging.enable</code> is set to "true", then
     * a temporary file in <code>org.codehaus.janino.source_debugging.dir</code> or the
     * system's default temp dir is created in order to make the source code
     * available to a debugger.
     */
    public
    Scanner(@Nullable String optionalFileName, InputStream is, @Nullable String optionalEncoding)
    throws IOException {
        this(
            optionalFileName,                  // optionalFileName
            (                                  // in
                optionalEncoding == null
                ? new InputStreamReader(is)
                : new InputStreamReader(is, optionalEncoding)
            ),
            (short) 1,                         // initialLineNumber
            (short) 0                          // initialColumnNumber
        );
    }

    // Public Scanners that read from a Reader.

    /**
     * Set up a scanner that reads tokens from the given
     * {@link Reader}.
     * <p>
     * The <code>optionalFileName</code> is used for reporting errors during
     * compilation and for source level debugging, and should name an existing
     * file. If <code>null</code> is passed, and the system property
     * <code>org.codehaus.janino.source_debugging.enable</code> is set to "true", then
     * a temporary file in <code>org.codehaus.janino.source_debugging.dir</code> or the
     * system's default temp dir is created in order to make the source code
     * available to a debugger.
     */
    public
    Scanner(@Nullable String optionalFileName, Reader in) throws IOException {
        this(
            optionalFileName, // optionalFileName
            in,               // in
            (short) 1,        // initialLineNumber
            (short) 0         // initialColumnNumber
        );
    }

    /** Creates a {@link Scanner} that counts lines and columns from non-default initial values. */
    public
    Scanner(
        @Nullable String optionalFileName,
        Reader           in,
        short            initialLineNumber,        // "1" is a good idea
        short            initialColumnNumber       // "0" is a good idea
    ) throws IOException {

        // Debugging on source code level is only possible if the code comes from
        // a "real" Java source file which the debugger can read. If this is not the
        // case, and we absolutely want source code level debugging, then we write
        // a verbatim copy of the source code into a temporary file in the system
        // temp directory.
        // This behavior is controlled by the two system properties
        //     org.codehaus.janino.source_debugging.enable
        //     org.codehaus.janino.source_debugging.dir
        // JANINO is designed to compile in memory to save the overhead of disk
        // I/O, so writing this file is only recommended for source code level
        // debugging purposes.
        if (optionalFileName == null && Boolean.getBoolean(ICookable.SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE)) {
            String dirName       = System.getProperty(ICookable.SYSTEM_PROPERTY_SOURCE_DEBUGGING_DIR);
            File   dir           = dirName == null ? null : new File(dirName);
            File   temporaryFile = File.createTempFile("janino", ".java", dir);
            temporaryFile.deleteOnExit();
            in = new TeeReader(
                in,                            // in
                new FileWriter(temporaryFile), // out
                true                           // closeWriterOnEOF
            );
            optionalFileName = temporaryFile.getAbsolutePath();
        }

        this.optionalFileName     = optionalFileName;
        this.in                   = new UnicodeUnescapeReader(in);
        this.nextCharLineNumber   = initialLineNumber;
        this.nextCharColumnNumber = initialColumnNumber;
    }

    /** @return The file name optionally passed to the constructor */
    @Nullable public String
    getFileName() { return this.optionalFileName; }

    /**
     * Closes the character source (file, {@link InputStream}, {@link Reader}) associated with this object. The results
     * of future calls to {@link #produce()} are undefined.
     *
     * @deprecated This method is deprecated, because the concept described above is confusing. An application should
     *             close the underlying {@link InputStream} or {@link Reader} itself
     */
    @Deprecated public void
    close() throws IOException { this.in.close(); }

    /**
     * Get the text of the doc comment (a.k.a. "JAVADOC comment") preceeding
     * the next token.
     * @return <code>null</code> if the next token is not preceeded by a doc comment
     */
    @Nullable public String
    doc() {
        String s = this.optionalDocComment;
        this.optionalDocComment = null;
        return s;
    }

    /** @return The {@link Location} of the next character */
    public Location
    location() {
        return new Location(this.optionalFileName, this.nextCharLineNumber, this.nextCharColumnNumber);
    }

    /** Representation of a Java&trade; token. */
    public final
    class Token {

        @Nullable private final String optionalFileName;
        private final short            lineNumber;
        private final short            columnNumber;
        @Nullable private Location     location; // Created lazily.

        /** The type of this token; legal values are the various public constant declared in this class. */
        public final int type;

        /** Indication of the 'end-of-input' condition. */
        public static final int EOF = 0;

        /** The token represents an identifier. */
        public static final int IDENTIFIER = 1;

        /** The token represents a keyword. */
        public static final int KEYWORD = 2;

        /**
         * The token represents an integer literal; its {@link #value} is the text of the integer literal exactly as it
         * appears in the source code (e.g. "0", "123", "123L", "03ff", "0xffff", "0b10101010").
         */
        public static final int INTEGER_LITERAL = 3;

        /**
         * The token represents a floating-point literal; its {@link #value} is the text of the floating-point literal
         * exactly as it appears in the source code (e.g. "1.23", "1.23F", "1.23D", "1.", ".1", "1E13").
         */
        public static final int FLOATING_POINT_LITERAL = 4;

        /** The token represents a boolean literal; its {@link #value} is either 'true' or 'false'. */
        public static final int BOOLEAN_LITERAL = 5;

        /**
         * The token represents a character literal; its {@link #value} is the text of the character literal exactly as
         * it appears in the source code (including the single quotes around it).
         */
        public static final int CHARACTER_LITERAL = 6;

        /**
         * The token represents a string literal; its {@link #value} is the text of the string literal exactly as it
         * appears in the source code (including the double quotes around it).
         */
        public static final int STRING_LITERAL = 7;

        /** The token represents the {@code null} literal; its {@link #value} is 'null'. */
        public static final int NULL_LITERAL = 8;

        /**
         * The token represents an operator; its {@link #value} is exactly the particular operator (e.g.
         * "&lt;&lt;&lt;=").
         */
        public static final int OPERATOR = 9;

        /** The text of the token exactly as it appears in the source code. */
        public final String value;

        private
        Token(int type, String value) {
            this.optionalFileName = Scanner.this.optionalFileName;
            this.lineNumber       = Scanner.this.tokenLineNumber;
            this.columnNumber     = Scanner.this.tokenColumnNumber;
            this.type             = type;
            this.value            = value;
        }

        /** @return The location of the first character of this token */
        public Location
        getLocation() {

            if (this.location != null) return this.location;

            return (this.location = new Location(this.optionalFileName, this.lineNumber, this.columnNumber));
        }

        @Override public String
        toString() { return this.value; }
    }

    /**
     * Preduces and returns the next token. Notice that end-of-input is <i>not</i> signalized with a {@code null}
     * product, but by the special {@link Token#EOF} token.
     */
    public Token
    produce() throws CompileException, IOException {
        if (this.optionalDocComment != null) {
            this.warning("MDC", "Misplaced doc comment", this.location());
            this.optionalDocComment = null;
        }

        // Skip whitespace and process comments.
        int           state = 0;
        StringBuilder dcsb  = null; // For doc comment

        PROCESS_COMMENTS:
        for (;;) {
            switch (state) {

            case 0:  // Outside any comment
                if (this.peek() == -1) {
                    return new Token(Token.EOF, "EOF");
                } else
                if (Character.isWhitespace(this.peek())) {
                    this.read();
                } else
                if (this.peekRead('/')) {
                    if (this.peekRead(-1)) return new Token(Token.OPERATOR, "/");
                    if (this.peekRead('=')) return new Token(Token.OPERATOR, "/=");
                    if (this.peekRead('/')) {
                        state = 2;
                        break;
                    }
                    if (this.peekRead('*')) {
                        state = 3;
                        break;
                    }
                    return new Token(Token.OPERATOR, "/");
                } else
                {
                    break PROCESS_COMMENTS;
                }
                break;

            case 2:  // After "//..."
                if (this.peek() == -1) return new Token(Token.EOF, "EOF");
                if (this.peek("\r\n")) {
                    state = 0;
                } else {
                    this.read();
                }
                break;

            case 3:  // After "/*"
                if (this.peek() == -1) throw new CompileException("EOF in traditional comment", this.location());
                state = this.peek() == '*' ? 4 : 9;
                break;

            case 4:  // After "/**"
                if (this.peek() == -1) throw new CompileException("EOF in doc comment", this.location());
                if (this.peekRead('/')) {
                    state = 0;
                } else
                {
                    if (this.optionalDocComment != null) {
                        this.warning(
                            "MDC",
                            "Multiple doc comments",
                            new Location(this.optionalFileName, this.nextCharLineNumber, this.nextCharColumnNumber)
                        );
                    }
                    int firstDocCommentChar = this.read();
                    dcsb  = new StringBuilder().append((char) firstDocCommentChar);
                    state = (
                        firstDocCommentChar == '\r' || firstDocCommentChar == '\n' ? 6
                        : firstDocCommentChar == '*' ? 8
                        : 5
                    );
                }
                break;

            case 5:  // After "/**..."
                assert dcsb != null;
                if (this.peek() == -1) {
                    throw new CompileException("EOF in doc comment", this.location());
                } else
                if (this.peekRead('*')) {
                    state = 8;
                } else
                if (this.peek("\r\n")) {
                    dcsb.append((char) this.read());
                    state = 6;
                } else
                {
                    dcsb.append((char) this.read());
                }
                break;

            case 6:  // After "/**...\n"
                assert dcsb != null;
                if (this.peek() == -1) {
                    throw new CompileException("EOF in doc comment", this.location());
                } else
                if (this.peekRead('*')) {
                    state = 7;
                } else
                if (this.peek("\r\n")) {
                    dcsb.append((char) this.read());
                } else
                if (this.peek(" \t")) {
                    this.read();
                } else
                {
                    dcsb.append((char) this.read());
                    state = 5;
                }
                break;

            case 7:  // After "/**...\n *"
                assert dcsb != null;
                if (this.peek() == -1) {
                    throw new CompileException("EOF in doc comment", this.location());
                } else
                if (this.peekRead('*')) {
                    ;
                } else
                if (this.peekRead('/')) {
                    this.optionalDocComment = dcsb.toString();
                    state                   = 0;
                } else
                {
                    dcsb.append((char) this.peek());
                    state = 5;
                }
                break;

            case 8:  // After "/**...*"
                assert dcsb != null;
                if (this.peek() == -1) {
                    throw new CompileException("EOF in doc comment", this.location());
                } else
                if (this.peekRead('/')) {
                    this.optionalDocComment = dcsb.toString();
                    state                   = 0;
                } else
                if (this.peekRead('*')) {
                    dcsb.append('*');
                } else
                {
                    dcsb.append('*');
                    dcsb.append((char) this.read());
                    state = 5;
                }
                break;

            case 9:  // After "/*..."
                if (this.peek() == -1) throw new CompileException("EOF in traditional comment", this.location());
                if (this.read() == '*') state = 10;
                break;

            case 10: // After "/*...*"
                if (this.peek() == -1) throw new CompileException("EOF in traditional comment", this.location());
                if (this.peekRead('/')) {
                    state = 0;
                } else
                if (this.peekRead('*')) {
                    ;
                } else
                {
                    state = 9;
                }
                break;

            default:
                throw new JaninoRuntimeException(Integer.toString(state));
            }
        }

        /*
         * Whitespace and comments are now skipped; "nextChar" is definitely
         * the first character of the token.
         */
        this.tokenLineNumber   = this.nextCharLineNumber;
        this.tokenColumnNumber = this.nextCharColumnNumber;

        // Scan identifier.
        if (Character.isJavaIdentifierStart((char) this.peek())) {
            StringBuilder sb = new StringBuilder();
            sb.append((char) this.read());
            for (;;) {
                if (this.peek() == -1 || !Character.isJavaIdentifierPart((char) this.peek())) break;
                sb.append((char) this.read());
            }
            String s = sb.toString();
            if ("true".equals(s))  return new Token(Token.BOOLEAN_LITERAL, "true");
            if ("false".equals(s)) return new Token(Token.BOOLEAN_LITERAL, "false");
            if ("null".equals(s))  return new Token(Token.NULL_LITERAL,    "null");
            {
                String v = (String) Scanner.JAVA_KEYWORDS.get(s);
                if (v != null) return new Token(Token.KEYWORD, v);
            }
            return new Token(Token.IDENTIFIER, s);
        }

        // Scan numeric literal.
        if (
            Character.isDigit((char) this.peek())
            || (this.peek() == '.' && Character.isDigit(this.peekButOne())) // .999
        ) return this.scanNumericLiteral();

        // Scan string literal.
        if (this.peekRead('"')) {
            StringBuilder sb = new StringBuilder("\"");
            while (!this.peekRead('"')) {
                this.scanLiteralCharacter(sb);
            }
            return new Token(Token.STRING_LITERAL, sb.append('"').toString());
        }

        // Scan character literal.
        if (this.peekRead('\'')) {
            if (this.peek() == '\'') {
                throw new CompileException(
                    "Single quote must be backslash-escaped in character literal",
                    this.location()
                );
            }

            StringBuilder sb = new StringBuilder("'");
            this.scanLiteralCharacter(sb);
            if (!this.peekRead('\'')) throw new CompileException("Closing single quote missing", this.location());

            return new Token(Token.CHARACTER_LITERAL, sb.append('\'').toString());
        }

        // Scan separator / operator.
        {
            String v = (String) Scanner.JAVA_OPERATORS.get(new String(new char[] { (char) this.peek() }));
            if (v != null) {
                for (;;) {
                    this.read();
                    String v2 = (String) (
                        this.expectGreater
                        ? Scanner.JAVA_OPERATORS_EXPECT_GREATER
                        : Scanner.JAVA_OPERATORS
                    ).get(v + (char) this.peek());
                    if (v2 == null) return new Token(Token.OPERATOR, v);
                    v = v2;
                }
            }
        }

        throw new CompileException(
            "Invalid character input \"" + (char) this.peek() + "\" (character code " + this.peek() + ")",
            this.location()
        );
    }

    /** @return Whether the scanner is currently in 'expect greater' mode */
    public boolean
    getExpectGreater() { return this.expectGreater; }

    /**
     * Sets or resets the 'expect greater' mode.
     *
     * @return Whether the 'expect greater' mode was previously active
     */
    public boolean
    setExpectGreater(boolean value) {
        boolean tmp = this.expectGreater;
        this.expectGreater = value;
        return tmp;
    }

    private Token
    scanNumericLiteral() throws CompileException, IOException {
        StringBuilder sb    = new StringBuilder();
        int           state = 0;
        for (;;) {
            switch (state) {

            case 0: // First character.
                if (this.peekRead('0')) {
                    sb.append('0');
                    state = 6;
                } else
                if (this.peek() == '.' && Scanner.isDecimalDigit(this.peekButOne())) {
                    this.read();
                    sb.append('.');
                    state = 2;
                } else
                if (Scanner.isDecimalDigit(this.peek())) {
                    sb.append((char) this.read());
                    state = 1;
                } else
                {
                    throw new CompileException(
                        "Numeric literal begins with invalid character '" + (char) this.peek() + "'",
                        this.location()
                    );
                }
                break;

            case 1: // Decimal digits.
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("lL")) {
                    sb.append((char) this.read());
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                } else
                if (this.peek("fFdD")) {
                    sb.append((char) this.read());
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                } else
                if (this.peekRead('.')) {
                    sb.append('.');
                    state = 2;
                } else
                if (this.peek("Ee")) {
                    sb.append((char) this.read());
                    state = 3;
                } else
                {
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                }
                break;

            case 2: // After decimal point.
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("eE")) {
                    sb.append((char) this.read());
                    state = 3;
                } else
                if (this.peek("fFdD")) {
                    sb.append((char) this.read());
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                } else
                {
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                }
                break;

            case 3: // After 'e'.
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                    state = 5;
                } else
                if (this.peek("-+")) {
                    sb.append((char) this.read());
                    state = 4;
                } else
                {
                    throw new CompileException("Exponent missing after \"E\"", this.location());
                }
                break;

            case 4: // After exponent sign.
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne()))
                ) {
                    sb.append((char) this.read());
                    state = 5;
                } else
                {
                    throw new CompileException("Exponent missing after 'E' and sign", this.location());
                }
                break;

            case 5: // After first exponent digit.
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne()))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("fFdD")) {
                    sb.append((char) this.read());
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                } else
                {
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                }
                break;

            case 6: // After leading zero.
                if (
                    Scanner.isOctalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isOctalDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                    state = 7;
                } else
                if (this.peek("lL")) {
                    sb.append((char) this.read());
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                } else
                if (this.peek("fFdD")) {
                    sb.append((char) this.read());
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                } else
                if (this.peekRead('.')) {
                    sb.append('.');
                    state = 2;
                } else
                if (this.peek("Ee")) {
                    sb.append((char) this.read());
                    state = 3;
                } else
                if (this.peek("xX")) {
                    sb.append((char) this.read());
                    state = 8;
                } else
                if (this.peek("bB")) {
                    sb.append((char) this.read());
                    state = 10;
                } else
                {
                    return new Token(Token.INTEGER_LITERAL, "0");
                }
                break;

            case 7: // In octal literal.
                if (
                    Scanner.isOctalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isOctalDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("89")) {
                    throw new CompileException(
                        "Digit '" + (char) this.peek() + "' not allowed in octal literal",
                        this.location()
                    );
                } else
                if (this.peek("lL")) {

                    // Octal long literal.
                    sb.append((char) this.read());
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                } else
                {
                    // Octal int literal
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                }
                break;

            case 8: // After '0x'.
                if (Scanner.isHexDigit(this.peek())) {
                    sb.append((char) this.read());
                    state = 9;
                } else
                if (this.peekRead('.')) {
                    sb.append('.');
                    state = 12;
                } else
                {
                    throw new CompileException("Hex digit expected after \"0x\"", this.location());
                }
                break;

            case 9: // After "0x1".
                if (
                    Scanner.isHexDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isHexDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peekRead('.')) {
                    sb.append('.');
                    state = 12;
                } else
                if (this.peek("pP")) {
                    sb.append((char) this.read());
                    state = 13;
                } else
                if (this.peek("lL")) {
                    // Hex long literal
                    sb.append((char) this.read());
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                } else
                {
                    // Hex int literal
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                }
                break;

            case 10: // After '0b'.
                if (!Scanner.isBinaryDigit(this.peek())) {
                    throw new CompileException("Binary digit expected after \"0b\"", this.location());
                }
                sb.append((char) this.read());
                state = 11;
                break;

            case 11: // After first binary digit.
                if (
                    Scanner.isBinaryDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isBinaryDigit(this.peekButOne())))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("lL")) {
                    // Hex long literal
                    sb.append((char) this.read());
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                } else
                {
                    // Hex int literal
                    return new Token(Token.INTEGER_LITERAL, sb.toString());
                }
                break;

            case 12: // After "0x." or after "0xabc.".
                if (Scanner.isHexDigit(this.peek())) {
                    sb.append((char) this.read());
                } else
                if (this.peek("pP")) {
                    sb.append((char) this.read());
                    state = 13;
                } else
                {
                    throw new CompileException("\"p\" missing in hexadecimal floating-point literal", this.location());
                }
                break;

            case 13: // After "0x111p".
                if (this.peek("-+")) {
                    sb.append((char) this.read());
                    state = 14;
                } else
                if (Scanner.isDecimalDigit(this.peek())) {
                    sb.append((char) this.read());
                    state = 15;
                } else
                {
                    throw new CompileException(
                        "Unexpected character \"" + (char) this.peek() + "\" in hexadecimal floating point literal",
                        this.location()
                    );
                }
                break;

            case 14: // After "0x111p-".
                if (!Scanner.isDecimalDigit(this.peek())) {
                    throw new CompileException(
                        "Unexpected character \"" + (char) this.peek() + "\" in hexadecimal floating point literal",
                        this.location()
                    );
                }
                sb.append((char) this.read());
                state = 15;
                break;

            case 15: // After "0x111p-1".
                if (
                    Scanner.isDecimalDigit(this.peek())
                    || (this.peek() == '_' && (Scanner.isDecimalDigit(this.peekButOne()) || this.peekButOne() == '_'))
                ) {
                    sb.append((char) this.read());
                } else
                if (this.peek("fFdD")) {
                    sb.append((char) this.read());
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                } else
                {
                    return new Token(Token.FLOATING_POINT_LITERAL, sb.toString());
                }
                break;

            default:
                throw new AssertionError(state);
            }
        }
    }

    /**
     * To comply with the JLS, this method does <em>not</em> allow for non-latin digit (like {@link
     * Character#isDigit(char)} does).
     */
    private static boolean
    isDecimalDigit(int c) { return c >= '0' && c <= '9'; }

    /**
     * To comply with the JLS, this method does <em>not</em> allow for non-latin digit (like {@link
     * Character#isDigit(char)} does).
     */
    private static boolean
    isHexDigit(int c) { return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'); }

    private static boolean
    isOctalDigit(int c) { return c >= '0' && c <= '7'; }

    private static boolean
    isBinaryDigit(int c) { return c == '0' || c == '1'; }

    /** Scans the next literal character into a {@link StringBuilder}. */
    private void
    scanLiteralCharacter(StringBuilder sb) throws CompileException, IOException {
        if (this.peek() == -1) throw new CompileException("EOF in literal", this.location());

        if (this.peek() == '\r' || this.peek() == '\n') {
            throw new CompileException("Line break in literal not allowed", this.location());
        }

        if (!this.peekRead('\\')) {

            // Not an escape sequence.
            sb.append((char) this.read());
            return;
        }

        // JLS7 3.10.6: Escape sequences for character and string literals.
        sb.append('\\');

        {
            int idx = "btnfr\"'\\".indexOf(this.peek());
            if (idx != -1) {

                // "\t" and friends.
                sb.append((char) this.read());
                return;
            }
        }

        if (this.peek("01234567")) {

            // Octal escapes: "\0" through "\3ff".
            char firstChar = (char) this.read();
            sb.append(firstChar);

            if (!this.peek("01234567")) return;
            sb.append((char) this.read());

            if (!this.peek("01234567")) return;
            sb.append((char) this.read());

            if ("0123".indexOf(firstChar) == -1) {
                throw new CompileException("Invalid octal escape", this.location());
            }

            return;
        }

        throw new CompileException("Invalid escape sequence", this.location());
    }

    /**
     * Returns the next character, but does not consume it.
     */
    private int
    peek() throws CompileException, IOException {
        if (this.nextChar != -1) return this.nextChar;
        try {
            return (this.nextChar = this.internalRead());
        } catch (UnicodeUnescapeException ex) {
            throw new CompileException(ex.getMessage(), this.location(), ex);
        }
    }

    /**
     * @return Whether the next character is one of the <var>expectedCharacters</var>
     */
    private boolean
    peek(String expectedCharacters) throws CompileException, IOException {
        return expectedCharacters.indexOf((char) this.peek()) != -1;
    }

    /**
     * Returns the next-but-one character, but does not consume any characters.
     */
    private int
    peekButOne() throws CompileException, IOException {
        if (this.nextButOneChar != -1) return this.nextButOneChar;
        this.peek();
        try {
            return (this.nextButOneChar = this.internalRead());
        } catch (UnicodeUnescapeException ex) {
            throw new CompileException(ex.getMessage(), this.location(), ex);
        }
    }

    /**
     * Consumes and returns the next character.
     *
     * @return Whether the next character equalled the <var>expected</var> character
     */
    private int
    read() throws CompileException, IOException {

        this.peek();

        final int result = this.nextChar;

        this.nextChar       = this.nextButOneChar;
        this.nextButOneChar = -1;

        return result;
    }

    /**
     * Consumes the next character iff it equals the <var>expected</var> character.
     *
     * @return Whether the next character equalled the <var>expected</var> character
     */
    private boolean
    peekRead(int expected) throws CompileException, IOException {

        if (this.peek() == expected) {
            this.nextChar       = this.nextButOneChar;
            this.nextButOneChar = -1;
            return true;
        }

        return false;
    }

    private int
    internalRead() throws IOException, CompileException {

        int result;
        try {
            result = this.in.read();
        } catch (UnicodeUnescapeException ex) {
            throw new CompileException(ex.getMessage(), this.location(), ex);
        }
        if (result == '\r') {
            ++this.nextCharLineNumber;
            this.nextCharColumnNumber = 0;
            this.crLfPending          = true;
        } else
        if (result == '\n') {
            if (this.crLfPending) {
                this.crLfPending = false;
            } else {
                ++this.nextCharLineNumber;
                this.nextCharColumnNumber = 0;
            }
        } else
        {
            ++this.nextCharColumnNumber;
        }

        return result;
    }

    @Nullable private final String optionalFileName;
    private final Reader           in;
    private int                    nextChar       = -1;
    private int                    nextButOneChar = -1;
    private boolean                crLfPending;
    private short                  nextCharLineNumber;
    private short                  nextCharColumnNumber;

    /** Line number of the previously produced token (typically starting at one). */
    private short tokenLineNumber;

    /**
     * Column number of the first character of the previously produced token (1 if token is immediately preceded by a
     * line break).
     */
    private short tokenColumnNumber;

    /** The optional JAVADOC comment preceding the {@link #nextToken}. */
    @Nullable private String optionalDocComment;

    /**
     * Whether the scanner is in 'expect greater' mode: If so, it parses character sequences like ">>>=" as
     * ">", ">", ">", "=".
     */
    private boolean expectGreater;

    private static final Map<String, String> JAVA_KEYWORDS = new HashMap<String, String>();
    static {
        String[] ks = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "extends", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
        };
        for (int i = 0; i < ks.length; ++i) Scanner.JAVA_KEYWORDS.put(ks[i], ks[i]);
    }
    private static final Map<String, String> JAVA_OPERATORS                = new HashMap<String, String>();
    private static final Map<String, String> JAVA_OPERATORS_EXPECT_GREATER = new HashMap<String, String>();
    static {
        String[] ops = {
            // Separators:
            "(", ")", "{", "}", "[", "]", ";", ",", ".", "@",
            // Operators:
            "=",  ">",  "<",  "!",  "~",  "?",  ":",
            "==", "<=", ">=", "!=", "&&", "||", "++", "--",
            "+",  "-",  "*",  "/",  "&",  "|",  "^",  "%",  "<<",  ">>",  ">>>",
            "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=",
        };
        for (String op : ops) {
            Scanner.JAVA_OPERATORS.put(op, op);
            if (!op.startsWith(">>")) Scanner.JAVA_OPERATORS_EXPECT_GREATER.put(op, op);
        }
    }

    /**
     * By default, warnings are discarded, but an application my install a
     * {@link WarningHandler}.
     * <p>
     * Notice that there is no <code>Scanner.setErrorHandler()</code> method, but scan errors
     * always throw a {@link CompileException}. The reason being is that there is no reasonable
     * way to recover from scan errors and continue scanning, so there is no need to install
     * a custom scan error handler.
     *
     * @param optionalWarningHandler <code>null</code> to indicate that no warnings be issued
     */
    public void
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
     * The <code>handle</code> argument qulifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     * @throws CompileException
     */
    private void
    warning(String handle, String message, @Nullable Location optionalLocation) throws CompileException {
        if (this.optionalWarningHandler != null) {
            this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
        }
    }
}
