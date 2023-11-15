
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

package org.codehaus.janino;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.io.Readers;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Splits up a character stream into tokens and returns them as {@link java.lang.String String} objects.
 */
public
class Scanner {

    /**
     * Setting this system property to 'true' enables source-level debugging. Typically, this means that compilation
     * is executed with "{@code -g:all}" instead of "{@code -g:none}".
     */
    public static final String SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE = "org.codehaus.janino.source_debugging.enable";

    /**
     * If the source code is not read from a file, debuggers have a hard time locating the source file for source-level
     * debugging. As a workaround, a copy of the source code is written to a temporary file, which must be included
     * in the debugger's source path. If this system property is set, the temporary source file is created in that
     * directory, otherwise in the default temporary-file directory.
     *
     * @see File#createTempFile(String, String, File)
     */
    public static final String SYSTEM_PROPERTY_SOURCE_DEBUGGING_DIR = "org.codehaus.janino.source_debugging.dir";

    /**
     * If set to "true", then the temporary source code files are <em>not</em> deleted on exit. That may be useful to
     * interpret stack traces offline.
     */
    public static final String SYSTEM_PROPERTY_SOURCE_DEBUGGING_KEEP = "org.codehaus.janino.source_debugging.keep";

    // Public Scanners that read from a file.

    /**
     * @deprecated This method is deprecated because it leaves the input file open
     */
    @Deprecated public
    Scanner(String fileName) throws IOException {
        this(
            fileName,                     // fileName
            new FileInputStream(fileName) // is
        );
    }

    /**
     * @deprecated This method is deprecated because it leaves the input file open
     */
    @Deprecated public
    Scanner(String fileName, String encoding) throws IOException {
        this(
            fileName,                      // fileName
            new FileInputStream(fileName), // is
            encoding                       // encoding
        );
    }

    /**
     * @deprecated This method is deprecated because it leaves the input file open
     */
    @Deprecated public
    Scanner(File file) throws IOException {
        this(
            file.getAbsolutePath(),    // fileName
            new FileInputStream(file), // is
            null                       // encoding
        );
    }

    /**
     * @deprecated This method is deprecated because it leaves the input file open
     */
    @Deprecated public
    Scanner(File file, @Nullable String encoding) throws IOException {
        this(
            file.getAbsolutePath(),    // fileName
            new FileInputStream(file), // is
            encoding                   // encoding
        );
    }

    // Public Scanners that read from an InputStream

    /**
     * Sets up a scanner that reads tokens from the given {@link InputStream} in the platform default encoding.
     * <p>
     *   The <var>fileName</var> is solely used for reporting in thrown exceptions.
     * </p>
     */
    public
    Scanner(@Nullable String fileName, InputStream is) throws IOException {
        this(
            fileName,                  // fileName
            new InputStreamReader(is), // in
            1,                         // initialLineNumber
            0                          // initialColumnNumber
        );
    }

    /**
     * Sets up a scanner that reads tokens from the given {@link InputStream} with the given
     * <var>encoding</var> ({@code null} means platform default encoding).
     * <p>
     *   The <var>fileName</var> is used for reporting errors during compilation and for source level
     *   debugging, and should name an existing file. If {@code null} is passed, and the system property
     *   {@code org.codehaus.janino.source_debugging.enable} is set to "true", then a temporary file in
     *   {@code org.codehaus.janino.source_debugging.dir} or the system's default temp dir is created in order to
     *   make the source code available to a debugger.
     * </p>
     */
    public
    Scanner(@Nullable String fileName, InputStream is, @Nullable String encoding)
    throws IOException {
        this(
            fileName, // fileName
            (         // in
                encoding == null
                ? new InputStreamReader(is)
                : new InputStreamReader(is, encoding)
            ),
            1,        // initialLineNumber
            0         // initialColumnNumber
        );
    }

    // Public Scanners that read from a Reader.

    /**
     * Sets up a scanner that reads tokens from the given {@link Reader}.
     * <p>
     *   The <var>fileName</var> is used for reporting errors during compilation and for source level
     *   debugging, and should name an existing file. If {@code null} is passed, and the system property {@code
     *   org.codehaus.janino.source_debugging.enable} is set to "true", then a temporary file in {@code
     *   org.codehaus.janino.source_debugging.dir} or the system's default temp dir is created in order to make the
     *   source code available to a debugger.
     * </p>
     */
    public
    Scanner(@Nullable String fileName, Reader in) throws IOException {
        this(
            fileName, // fileName
            in,       // in
            1,        // initialLineNumber
            0         // initialColumnNumber
        );
    }

    /**
     * Creates a {@link Scanner} that counts lines and columns from non-default initial values.
     */
    public
    Scanner(
        @Nullable String fileName,
        Reader           in,
        int              initialLineNumber,        // "1" is a good idea
        int              initialColumnNumber       // "0" is a good idea
    ) throws IOException {

        // Debugging on source code level is only possible if the code comes from a "real" Java source file which the
        // debugger can read. If this is not the case, and we absolutely want source code level debugging, then we
        // write a verbatim copy of the source code into a temporary file in the system temp directory.
        // This behavior is controlled by the two system properties
        //     org.codehaus.janino.source_debugging.enable
        //     org.codehaus.janino.source_debugging.dir
        // JANINO is designed to compile in memory to save the overhead of disk I/O, so writing this file is only
        // recommended for source code level debugging purposes.
        if (fileName == null && Boolean.getBoolean(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE)) {

            String  dirName = System.getProperty(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_DIR);
            boolean keep    = Boolean.getBoolean(Scanner.SYSTEM_PROPERTY_SOURCE_DEBUGGING_KEEP);

            File dir           = dirName == null ? null : new File(dirName);
            File temporaryFile = File.createTempFile("janino", ".java", dir);

            if (!keep) temporaryFile.deleteOnExit();

            in = Readers.teeReader(
                in,                            // in
                new FileWriter(temporaryFile), // out
                true                           // closeWriterOnEoi
            );
            fileName = temporaryFile.getAbsolutePath();
        }

        this.fileName             = fileName;
        this.in                   = new UnicodeUnescapeReader(in);
        this.nextCharLineNumber   = initialLineNumber;
        this.nextCharColumnNumber = initialColumnNumber;
    }

    /**
     * If <var>value</var> is {@code true}, then white space in the input stream is <em>ignored</em>, rather than
     * scanned as a {@link TokenType#WHITE_SPACE} token. Since white space is typically quite numerous, this
     * optimization may save considerable overhead.
     */
    public void
    setIgnoreWhiteSpace(boolean value) { this.ignoreWhiteSpace = value; }

    /**
     * @return The file name optionally passed to the constructor
     */
    @Nullable public String
    getFileName() { return this.fileName; }

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
     * @return The {@link Location} of the previously read (or peeked) token.
     */
    public Location
    location() { return new Location(this.fileName, this.tokenLineNumber, this.tokenColumnNumber); }

    private Token
    token(TokenType type, String value) {
        return new Token(this.fileName, this.tokenLineNumber, this.tokenColumnNumber, type, value);
    }

    /**
     * Holds the characters of the currently scanned token.
     */
    private final StringBuilder sb = new StringBuilder();

    /**
     * Produces and returns the next token. Notice that end-of-input is <em>not</em> signalized with a {@code null}
     * product, but by an {@link TokenType#END_OF_INPUT}-type token.
     */
    public Token
    produce() throws CompileException, IOException {

        if (this.peek() == -1) return this.token(TokenType.END_OF_INPUT, "end-of-input");

        // Funny... the JLS calls it "white space", and the JRE calls it "whitespace"!?
        if (this.ignoreWhiteSpace && Character.isWhitespace(this.peek())) {
            do {
                this.read();
                if (this.peek() == -1) return this.token(TokenType.END_OF_INPUT, "end-of-input");
            } while (Character.isWhitespace(this.peek()));
        }

        this.tokenLineNumber   = this.nextCharLineNumber;
        this.tokenColumnNumber = this.nextCharColumnNumber;

        this.sb.setLength(0);

        TokenType tokenType  = this.scan();
        String    tokenValue = this.sb.toString();

        // We want to be able to use REFERENCE EQUALITY for these...
        if (
            tokenType == TokenType.KEYWORD
            || tokenType == TokenType.BOOLEAN_LITERAL
            || tokenType == TokenType.NULL_LITERAL
            || tokenType == TokenType.OPERATOR
        ) tokenValue = tokenValue.intern();

        return this.token(tokenType, tokenValue);
    }

    private TokenType
    scan() throws CompileException, IOException {

        // Whitespace token?
        if (Character.isWhitespace(this.peek())) {
            do {
                this.read();
            } while (this.peek() != -1 && Character.isWhitespace(this.peek()));
            return TokenType.WHITE_SPACE;
        }

        // Scan a token that begins with "/".
        if (this.peekRead('/')) {

            if (this.peekRead(-1)) return TokenType.OPERATOR; // E.g. "/"

            if (this.peekRead('=')) return TokenType.OPERATOR; // E.g. "/="

            if (this.peekRead('/')) { // C++-style comment.

                // Consume all chars before EOL or EOI:
                while (!this.peek("\r\n") && !this.peekRead(-1)) this.read();

                return TokenType.C_PLUS_PLUS_STYLE_COMMENT;
            }

            if (this.peekRead('*')) { // C-style comment.
                boolean asteriskPending = false;
                for (;;) {
                    if (this.peek() == -1) {
                        throw new CompileException("Unexpected end-of-input in C-style comment", this.location());
                    }
                    char c = this.read();
                    if (asteriskPending) {
                        if (c == '/') return TokenType.C_STYLE_COMMENT;
                        if (c != '*') asteriskPending = false;
                    } else {
                        if (c == '*') asteriskPending = true;
                    }
                }
            }

            return TokenType.OPERATOR; // E.g. "/"
        }

        // Scan identifier.
        if (Character.isJavaIdentifierStart((char) this.peek())) {
            this.read();
            while (Character.isJavaIdentifierPart((char) this.peek())) this.read();
            String s = this.sb.toString();
            if ("true".equals(s))  return TokenType.BOOLEAN_LITERAL;
            if ("false".equals(s)) return TokenType.BOOLEAN_LITERAL;
            if ("null".equals(s))  return TokenType.NULL_LITERAL;

            if (Scanner.JAVA_KEYWORDS.contains(s)) return TokenType.KEYWORD;

            return TokenType.IDENTIFIER;
        }

        // Scan numeric literal.
        if (
            Character.isDigit((char) this.peek())
            || (this.peek() == '.' && Character.isDigit(this.peekButOne())) // .999
        ) return this.scanNumericLiteral();

        if (this.peekRead('"')) {
            if (this.peek() == '"' && this.peekButOne() == '"') {

                // Scan text block.
                this.read();                  // Consume second quote.
                this.read();                  // Consume third quote.
                while (this.peekRead(" \t")); // Consume space.
                if (this.peekRead('\r')) {    // Consume line break.
                    this.peekRead('\n');
                } else
                if (this.peekRead('\n')) {
                    ;
                } else
                {
                    throw new CompileException("Line break expected after \"\"\"", this.location());
                }
                // Consume the following chars including the trailing """.
                for (;;) {
                    if (this.peekRead('"') && this.peekRead('"') && this.peekRead('"')) return TokenType.TEXT_BLOCK;
                    this.read();
                }
            }

            // Scan string literal.
            while (!this.peekRead('"')) this.scanLiteralCharacter();
            return TokenType.STRING_LITERAL;
        }

        // Scan character literal.
        if (this.peekRead('\'')) {
            if (this.peek() == '\'') {
                throw new CompileException(
                    "Single quote must be backslash-escaped in character literal",
                    this.location()
                );
            }

            this.scanLiteralCharacter();
            if (!this.peekRead('\'')) throw new CompileException("Closing single quote missing", this.location());

            return TokenType.CHARACTER_LITERAL;
        }

        // Scan operator (including what Java calls "separators").
        if (Scanner.JAVA_OPERATORS.contains(String.valueOf((char) this.peek()))) {
            do {
                this.read();
            } while (Scanner.JAVA_OPERATORS.contains(this.sb.toString() + (char) this.peek()));
            return TokenType.OPERATOR;
        }

        throw new CompileException(
            "Invalid character input \"" + (char) this.peek() + "\" (character code " + this.peek() + ")",
            this.location()
        );
    }

    private TokenType
    scanNumericLiteral() throws CompileException, IOException {

        if (this.peekRead('0')) {

            if (               // E.g. "01"...
                Scanner.isOctalDigit(this.peek())
                || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isOctalDigit(this.peekButOne())))
            ) {

                this.read();
                while (
                    Scanner.isOctalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isOctalDigit(this.peekButOne())))
                ) this.read();
                if (this.peek("89")) {
                    throw new CompileException(
                        "Digit '" + (char) this.peek() + "' not allowed in octal literal",
                        this.location()
                    );
                }
                if (this.peekRead("lL")) {
                    return TokenType.INTEGER_LITERAL; // Octal long literal, e.g. "0123L".
                }

                return TokenType.INTEGER_LITERAL; // Octal int literal, e.g. "0123".
            }

            if (this.peekRead("lL")) return TokenType.INTEGER_LITERAL; // "0L"

            if (this.peekRead("fFdD")) return TokenType.FLOATING_POINT_LITERAL; // "0F" or "0D"

            if (this.peek(".Ee")) {
                if (this.peekRead('.')) { // "0."...
                    while (
                        Scanner.isDecimalDigit(this.peek())
                        || (
                            this.peek() == '_'
                            && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne()))
                        )
                    ) this.read();
                }
                if (this.peekRead("eE")) { // "0e"... and "0.123e"...

                    this.peekRead("-+");

                    if (!Scanner.isDecimalDigit(this.peek())) {
                        throw new CompileException("Exponent missing after \"E\"", this.location());
                    }
                    this.read();
                    while (
                        Scanner.isDecimalDigit(this.peek())
                        || (
                            this.peek() == '_'
                            && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne()))
                        )
                    ) this.read();
                }

                this.peekRead("fFdD");

                return TokenType.FLOATING_POINT_LITERAL;
            }

            if (this.peekRead("xX")) { // E.g. "0x"

                while (Scanner.isHexDigit(this.peek())) this.read();

                while (
                    Scanner.isHexDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isHexDigit(this.peekButOne())))
                ) this.read();

                if (this.peek(".pP")) {
                    if (this.peekRead('.')) {
                        if (Scanner.isHexDigit(this.peek())) {
                            this.read();
                            while (
                                Scanner.isHexDigit(this.peek())
                                || (
                                    this.peek() == '_'
                                    && (this.peekButOne() == '_' || Scanner.isHexDigit(this.peekButOne()))
                                )
                            ) this.read();
                        }
                    }

                    if (!this.peekRead("pP")) {
                        throw new CompileException(
                            "\"p\" missing in hexadecimal floating-point literal",
                            this.location()
                        );
                    }

                    this.peekRead("-+");

                    if (!Scanner.isDecimalDigit(this.peek())) {
                        throw new CompileException(
                            "Unexpected character \"" + (char) this.peek() + "\" in hexadecimal floating point literal",
                            this.location()
                        );
                    }
                    this.read();

                    while (
                        Scanner.isDecimalDigit(this.peek())
                        || (
                            this.peek() == '_'
                            && (Scanner.isDecimalDigit(this.peekButOne()) || this.peekButOne() == '_')
                        )
                    ) this.read();

                    this.peekRead("fFdD");

                    return TokenType.FLOATING_POINT_LITERAL;
                }

                if (this.peekRead("lL")) return TokenType.INTEGER_LITERAL; // Hex long literal

                // Hex int literal
                return TokenType.INTEGER_LITERAL;
            }

            if (this.peekRead("bB")) { // E.g. "0b"

                if (!Scanner.isBinaryDigit(this.peek())) {
                    throw new CompileException("Binary digit expected after \"0b\"", this.location());
                }
                this.read();

                while (
                    Scanner.isBinaryDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isBinaryDigit(this.peekButOne())))
                ) this.read();

                if (this.peekRead("lL")) return TokenType.INTEGER_LITERAL;

                return TokenType.INTEGER_LITERAL;
            }

            return TokenType.INTEGER_LITERAL;
        }

        if (this.peek() == '.' && Scanner.isDecimalDigit(this.peekButOne())) { // E.g. ".9"
            ;
        } else
        if (Scanner.isDecimalDigit(this.peek())) { // E.g. "123"
            this.read();

            while (
                Scanner.isDecimalDigit(this.peek())
                || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
            ) this.read();

            if (this.peekRead("lL")) return TokenType.INTEGER_LITERAL; // E.g. "123L"

            if (this.peekRead("fFdD")) return TokenType.FLOATING_POINT_LITERAL; // E.g. "123D"

            if (!this.peek(".eE")) return TokenType.INTEGER_LITERAL; // E.g. "123"
        } else
        {
            throw new CompileException(
                "Numeric literal begins with invalid character '" + (char) this.peek() + "'",
                this.location()
            );
        }

        if (this.peekRead('.')) { // E.g. ".9" or "123." or "123e"
            if (Scanner.isDecimalDigit(this.peek())) {
                this.read();
                while (
                    Scanner.isDecimalDigit(this.peek())
                    || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
                ) this.read();
            }
        }

        if (this.peekRead("eE")) { // E.g. "1e3"

            this.peekRead("-+");

            if (!Scanner.isDecimalDigit(this.peek())) {
                throw new CompileException("Exponent missing after \"E\"", this.location());
            }
            this.read();

            while (
                Scanner.isDecimalDigit(this.peek())
                || (this.peek() == '_' && (this.peekButOne() == '_' || Scanner.isDecimalDigit(this.peekButOne())))
            ) this.read();
        }

        this.peekRead("fFdD"); // E.g. "123F"

        return TokenType.FLOATING_POINT_LITERAL;
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

    /**
     * Scans the next literal character into a {@link StringBuilder}.
     */
    private void
    scanLiteralCharacter() throws CompileException, IOException {
        if (this.peek() == -1) throw new CompileException("EOF in literal", this.location());

        if (this.peek() == '\r' || this.peek() == '\n') {
            throw new CompileException("Line break in literal not allowed", this.location());
        }

        if (!this.peekRead('\\')) {

            // Not an escape sequence.
            this.read();
            return;
        }

        // JLS7 3.10.6: Escape sequences for character and string literals.

        {
            int idx = "btnfr\"'\\".indexOf(this.peek());
            if (idx != -1) {

                // "\t" and friends.
                this.read();
                return;
            }
        }

        if (this.peek("01234567")) {

            // Octal escapes: "\0" through "\3ff".
            char firstChar = this.read();

            if (!this.peekRead("01234567")) return;

            if (!this.peekRead("01234567")) return;

            if ("0123".indexOf(firstChar) == -1) {
                throw new CompileException("Invalid octal escape", this.location());
            }

            return;
        }

        throw new CompileException("Invalid escape sequence", this.location());
    }

    /**
     * Returns the next character, but does not consume it.
     *
     * @return -1 iff there is no next character, i.e the input stream is at end-of-input
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
     * Checks whether the next character is one of the <var>expectedCharacters</var>.
     * <p>
     *   It is not possible to peek exactly for "end-of-input"; use {@link #peekRead(int) peekRead}{@code (-1)}
     *   instead.
     * </p>
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
     * @throws CompileException There is no next charcter, i.e. the input stream is at end-of-input
     */
    private char
    read() throws CompileException, IOException {

        this.peek();

        if (this.nextChar == -1) throw new CompileException("Unexpected end-of-input", this.location());

        final char result = (char) this.nextChar;
        this.sb.append(result);

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
            if (this.nextChar != -1) this.sb.append((char) this.nextChar);
            this.nextChar       = this.nextButOneChar;
            this.nextButOneChar = -1;
            return true;
        }

        return false;
    }

    /**
     * Consumes the next character iff it is one of the <var>expectedCharacters</var>
     *
     * @return Whether the next character was one of the <var>expectedCharacters</var>
     */
    private boolean
    peekRead(String expectedCharacters) throws CompileException, IOException {

        if (this.peek() == -1) return false;

        if (expectedCharacters.indexOf((char) this.nextChar) == -1) return false;

        this.sb.append((char) this.nextChar);

        this.nextChar       = this.nextButOneChar;
        this.nextButOneChar = -1;

        return true;
    }

    /**
     * @return -1 iff there is no next character, i.e the input stream is at end-of-input
     */
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
        if (result == '\t') {
            this.nextCharColumnNumber = this.nextCharColumnNumber - this.nextCharColumnNumber % 8 + 8;
            this.crLfPending          = false;
        } else
        {
            ++this.nextCharColumnNumber;
            this.crLfPending = false;
        }

        return result;
    }

    @Nullable private final String fileName;
    private final Reader           in;
    private boolean                ignoreWhiteSpace;
    private int                    nextChar       = -1;
    private int                    nextButOneChar = -1;
    private boolean                crLfPending;
    private int                    nextCharLineNumber;
    private int                    nextCharColumnNumber;

    /**
     * Line number of the previously produced token (typically starting at one).
     */
    private int tokenLineNumber;

    /**
     * Column number of the first character of the previously produced token (1 if token is immediately preceded by a
     * line break).
     */
    private int tokenColumnNumber;

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(

        // SUPPRESS CHECKSTYLE WrapMethod:16

        "abstract", "assert",
        "boolean", "break", "byte",
        "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double",
        "else", "enum", "extends",
        "final", "finally", "float", "for",
        "goto",
        "if", "implements", "import", "instanceof", "int", "interface",
        "long",
        "native", "new",
        "package", "private", "protected", "public",
        "return",
        "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try",
        "void", "volatile",
        "while"
    ));

    private static final Set<String> JAVA_OPERATORS = new HashSet<>(Arrays.asList(

        // SUPPRESS CHECKSTYLE WrapMethod:9

        // Separators:
        "(", ")", "{", "}", "[", "]", ";", ",", ".", "@",
        "::",                                                                // <= Since Java 9

        // Operators:
        "=",  ">",  "<",  "!",  "~",  "?",  ":",  "->",
        "==", "<=", ">=", "!=", "&&", "||", "++", "--",
        "+",  "-",  "*",  "/",  "&",  "|",  "^",  "%",  "<<",  ">>",  ">>>",
        "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=" // SUPPRESS CHECKSTYLE Wrap
    ));
}
