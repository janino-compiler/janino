
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.janino.util.TeeReader;


/**
 * Splits up a character stream into tokens and returns them as
 * {@link java.lang.String String} objects.
 * <p>
 * The <code>optionalFileName</code> parameter passed to many
 * constructors should point 
 */

public class Scanner {

    /**
     * Set up a scanner that reads tokens from the given file in the platform
     * default encoding.
     * <p>
     * Don't forget to {@link #close()} when you're done, so the input file is closed.
     * @param fileName
     * @throws ScanException
     * @throws IOException
     */
    public Scanner(String fileName) throws ScanException, IOException {
        this(
            fileName,
            new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(fileName)))),
            true,                  // closeReaderOnConstructorException
            (short) 1, (short) 0   // initialLineNumber, initialColumnNumber
        );
    }

    /**
     * Set up a scanner that reads tokens from the given file in the platform
     * default encoding.
     * <p>
     * Don't forget to {@link #close()} when you're done, so the input file is closed.
     * @param file
     * @throws ScanException
     * @throws IOException
     */
    public Scanner(File file) throws ScanException, IOException {
        this(
            file.getAbsolutePath(),
            new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)))),
            true,                  // closeReaderOnConstructorException
            (short) 1, (short) 0   // initialLineNumber, initialColumnNumber
        );
    }

    /**
     * Set up a scanner that reads tokens from the given file in the given encoding.
     * <p>
     * Don't forget to {@link #close()} when you're done, so the input file is closed.
     * @param file
     * @param encoding
     * @throws ScanException
     * @throws IOException
     */
    public Scanner(File file, String encoding) throws ScanException, IOException {
        this(
            file.getAbsolutePath(),
            new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), encoding)),
            true,                  // closeReaderOnConstructorException
            (short) 1, (short) 0   // initialLineNumber, initialColumnNumber
        );
    }

    /**
     * Set up a scanner that reads tokens from the given
     * {@link InputStream} in the platform default encoding.
     * <p>
     * The <code>fileName</code> is solely used for reporting in thrown
     * exceptions.
     * @param fileName
     * @param is
     * @throws ScanException
     * @throws IOException
     */
    public Scanner(String fileName, InputStream is) throws ScanException, IOException {
        this(
            fileName,
            new BufferedReader(new InputStreamReader(is)),
            false,                 // closeReaderOnConstructorException
            (short) 1, (short) 0   // initialLineNumber, initialColumnNumber
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
    public Scanner(String optionalFileName, InputStream is, String optionalEncoding) throws ScanException, IOException {
        this(
            optionalFileName,
            new BufferedReader(
                optionalEncoding == null ?
                new InputStreamReader(is) :
                new InputStreamReader(is, optionalEncoding)
            ),
            false,                 // closeReaderOnConstructorException
            (short) 1, (short) 0   // initialLineNumber, initialColumnNumber
        );
    }

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
    public Scanner(String optionalFileName, Reader in) throws ScanException, IOException {
        this(optionalFileName, in, false, (short) 1, (short) 0);
    }

    /**
     * Creates a {@link Scanner} that counts lines and columns from non-default initial
     * values.
     */
    public Scanner(
        String optionalFileName,
        Reader in,
        short  initialLineNumber,        // "1" is a good idea
        short  initialColumnNumber       // "0" is a good idea
    ) throws ScanException, IOException {
        this(optionalFileName, in, false, initialLineNumber, initialColumnNumber);
    }

    /**
     * Some public constructors open a file on-the-fly. For these constructors it is
     * important that this private constructor closes the {@link Reader} if
     * it throws an exception.
     */
    private Scanner(
        String  optionalFileName,
        Reader  in,
        boolean closeReaderOnConstructorException,
        short   initialLineNumber,        // "1" is a good idea
        short   initialColumnNumber       // "0" is a good idea
    ) throws ScanException, IOException {
        try {

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
            if (optionalFileName == null && Boolean.getBoolean("org.codehaus.janino.source_debugging.enable")) {
                String dirName = System.getProperty("org.codehaus.janino.source_debugging.dir");
                File dir = dirName == null ? null : new File(dirName);
                File temporaryFile = File.createTempFile("janino", ".java", dir);
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

            this.readNextChar();
            this.nextToken       = this.internalRead();
            this.nextButOneToken = this.internalRead();
        } catch (ScanException e) {
            if (closeReaderOnConstructorException) try { in.close(); } catch (IOException e2) {}
            throw e;
        } catch (IOException e) {
            if (closeReaderOnConstructorException) try { in.close(); } catch (IOException e2) {}
            throw e;
        }
    }

    /**
     * Closes the character source (file, {@link InputStream}, {@link Reader}) associated
     * with this object. The results of future calls to {@link #peek()} and
     * {@link #read()} are undefined.
     */
    public void close() throws IOException {
        this.in.close();
    }

    /**
     * Read the next token from the input.
     */
    public Token read() throws ScanException, IOException {
        Token res = this.nextToken;
        this.nextToken       = this.nextButOneToken;
        this.nextButOneToken = this.internalRead();
        return res;
    }

    /**
     * Peek the next token, but don't remove it from the input.
     */
    public Token peek() {
        if (Scanner.DEBUG) System.err.println("peek() => \"" + this.nextToken + "\"");
        return this.nextToken;
    }

    /**
     * Peek the next but one token, neither remove the next nor the next
     * but one token from the input.
     */
    public Token peekNextButOne() {
        return this.nextButOneToken;
    }

    public abstract class Token {
        private /*final*/ String optionalFileName;
        private /*final*/ short  lineNumber;
        private /*final*/ short  columnNumber;
        private Location location = null;

        private Token() {
            this.optionalFileName = Scanner.this.optionalFileName;
            this.lineNumber       = Scanner.this.tokenLineNumber;
            this.columnNumber     = Scanner.this.tokenColumnNumber;
        }

        public Location getLocation() {
            if (this.location == null) this.location = new Location(this.optionalFileName, this.lineNumber, this.columnNumber);
            return this.location;
        }

        public boolean isKeyword() { return false; }
        public boolean isKeyword(String k) { return false; }
        public boolean isKeyword(String[] ks) { return false; }
        public String getKeyword() throws ScanException { throw new ScanException("Not a keyword token"); }

        public boolean isIdentifier() { return false; }
        public boolean isIdentifier(String id) { return false; }
        public String getIdentifier() throws ScanException { throw new ScanException("Not an identifier token"); }

        public boolean isLiteral() { return false; }
        public Class getLiteralType() throws ScanException { throw new ScanException("Not a literal token"); }
        public Object getLiteralValue() throws ScanException { throw new ScanException("Not a literal token"); }
        public Object getNegatedLiteralValue() throws ScanException { throw new ScanException("Not a literal token"); }

        public boolean isOperator() { return false; }
        public boolean isOperator(String o) { return false; }
        public boolean isOperator(String[] os) { return false; }
        public String getOperator() throws ScanException { throw new ScanException("Not an operator token"); }

        public boolean isEOF() { return false; }
    }

    public class KeywordToken extends Token {
        private final String keyword;

        /**
         * @param keyword Must be in interned string!
         */
        private KeywordToken(String keyword) {
            this.keyword = keyword;
        }

        public boolean isKeyword() { return true; }
        public boolean isKeyword(String k) { return this.keyword == k; }
        public boolean isKeyword(String[] ks) {
            for (int i = 0; i < ks.length; ++i) {
                if (this.keyword == ks[i]) return true;
            }
            return false;
        }
        public String getKeyword() { return this.keyword; }

        public String toString() { return this.keyword; }
    }

    public class IdentifierToken extends Token {
        private final String identifier;

        private IdentifierToken(String identifier) {
            this.identifier = identifier;
        }

        public boolean isIdentifier() { return true; }
        public boolean isIdentifier(String id) { return this.identifier.equals(id); }
        public String getIdentifier() { return this.identifier; }

        public String toString() { return this.identifier; }
    }

    /**
     * The type of the <code>value</code> parameter determines the type of the literal
     * token:
     * <table>
     *   <tr><th>Type/value returned by {@link #getLiteralValue()} and {@link #getNegatedLiteralValue()}</th><th>Literal</th></tr>
     *   <tr><td>{@link String}</td><td>STRING literal</td></tr>
     *   <tr><td>{@link Character}</td><td>CHAR literal</td></tr>
     *   <tr><td>{@link Integer}</td><td>INT literal</td></tr>
     *   <tr><td>{@link Long}</td><td>LONG literal</td></tr>
     *   <tr><td>{@link Float}</td><td>FLOAT literal</td></tr>
     *   <tr><td>{@link Double}</td><td>DOUBLE literal</td></tr>
     *   <tr><td>{@link Boolean}</td><td>BOOLEAN literal</td></tr>
     *   <tr><td><code>null</code></td><td>NULL literal</td></tr>
     * </table>
     */
    public abstract class LiteralToken extends Token {
        private final Class literalType;

        public LiteralToken(Class literalType) {
            this.literalType = literalType;
        }

        // Implement {@link Literal}.
        public final boolean isLiteral()      { return true; }
        public final Class   getLiteralType() { return this.literalType; }

        /**
         * The literals "2147483648" and "9223372036854775808L" only have a
         * negated value and thus throw a {@link Scanner.ScanException} if this method is
         * invoked.
         */
        public abstract Object getLiteralValue() throws ScanException;

        /**
         * Numeric literals have a negated value, the others throw a
         * {@link Scanner.ScanException}.
         */
        public Object getNegatedLiteralValue() throws ScanException {
            throw new ScanException("Literal " + this.toString() + " cannot be negated");
        }

        // Force subclasses to implement "toString()".
        public abstract String toString();
    }

    /**
     * Base class for literal tokens that have a determined value at creation
     * time (all but <code>void</code>, <code>2147483648</code> and
     * <code>9223372036854775808L</code>).
     */
    public abstract class ValuedLiteralToken extends LiteralToken {
        protected final Object value;

        public ValuedLiteralToken(Class literalType, Object value) {
            super(literalType);
            this.value = value;
        }
        public final Object getLiteralValue() { return this.value; }
    }

    /**
     * Base class for numeric literal tokens (integer, long, float, double).
     */
    public abstract class NumericLiteralToken extends ValuedLiteralToken {
        protected final Number negatedValue;

        public NumericLiteralToken(Class literalType, Number value, Number negatedValue) {
            super(literalType, value);
            this.negatedValue = negatedValue;
        }
        public final Object getNegatedLiteralValue() { return this.negatedValue; }
    }

    public final class StringLiteralToken extends ValuedLiteralToken {
        public StringLiteralToken(String s) { super(String.class, s); }
        public String toString()            {
            StringBuffer sb = new StringBuffer();
            sb.append('"');
            String s = (String) this.value;
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);

                if (c == '"') {
                    sb.append("\\\"");
                } else {
                    Scanner.escapeCharacter(c, sb);
                }
            }
            sb.append('"');
            return sb.toString();
        }
    }
    public final class CharacterLiteralToken extends ValuedLiteralToken {
        public CharacterLiteralToken(char c) { super(Character.TYPE, new Character(c)); }
        public String toString() {
            char c = ((Character) this.value).charValue();
            if (c == '\'') return "'\\''";
            StringBuffer sb = new StringBuffer("'");
            Scanner.escapeCharacter(c, sb);
            return sb.append('\'').toString();
        }
    }
    public final class IntegerLiteralToken extends NumericLiteralToken {
        public IntegerLiteralToken(int i) { super(Integer.TYPE, new Integer(i), new Integer(-i)); }
        public String toString()          { return this.value.toString(); }
    }
    public final class LongLiteralToken extends NumericLiteralToken {
        public LongLiteralToken(long l) { super(Long.TYPE, new Long(l), new Long(-l)); }
        public String toString()        { return this.value.toString() + 'L'; }
    }
    public final class FloatLiteralToken extends NumericLiteralToken {
        public FloatLiteralToken(float f) { super(Float.TYPE, new Float(f), new Float(-f)); }
        public String toString()          { return this.value.toString() + 'F'; }
    }
    public final class DoubleLiteralToken extends NumericLiteralToken {
        public DoubleLiteralToken(double d) { super(Double.TYPE, new Double(d), new Double(-d)); }
        public String toString()            { return this.value.toString() + 'D'; }
    }
    public final class BooleanLiteralToken extends ValuedLiteralToken {
        public BooleanLiteralToken(boolean b) { super(Boolean.TYPE, b ? Boolean.TRUE : Boolean.FALSE); }
        public String toString()              { return this.value.toString(); }
    }
    public final class NullLiteralToken extends LiteralToken {
        public NullLiteralToken()       { super(Void.TYPE); }
        public Object getLiteralValue() { return null; }
        public String toString()        { return "null"; }
    }

    public class OperatorToken extends Token {
        private final String operator;

        /**
         * 
         * @param operator Must be an interned string!
         */
        private OperatorToken(String operator) {
            this.operator = operator;
        }

        public boolean isOperator() { return true; }
        public boolean isOperator(String o) { return this.operator == o; }
        public boolean isOperator(String[] os) {
            for (int i = 0; i < os.length; ++i) {
                if (this.operator == os[i]) return true;
            }
            return false;
        }
        public String getOperator() { return this.operator; }

        public String toString() { return this.operator; }
    }

    public class EOFToken extends Token {
        public boolean isEOF() { return true; }
        public String toString() { return "End-Of-File"; }
    }

    /**
     * Escape unprintable characters appropriately, i.e. as
     * backslash-letter or backslash-U-four-hex-digits.
     * <p>
     * Notice: Single and double quotes are not escaped!
     */
    private static void escapeCharacter(char c, StringBuffer sb) {

        // Backslash escape sequences.
        int idx = "\b\t\n\f\r\\".indexOf(c);
        if (idx != -1) {
            sb.append('\\').append("btnfr\\".charAt(idx));
        } else

        // Printable characters.
        if (c >= ' ' && c < 255 && c != 127) {
            sb.append(c);
        } else

        // Backslash-U escape sequences.
        {
            sb.append("\\u");
            String hs = Integer.toHexString(0xffff & c);
            for (int j = hs.length(); j < 4; ++j) sb.append('0');
            sb.append(hs);
        }
    }

    private Token internalRead() throws ScanException, IOException {
        if (this.nextChar == -1) { return new EOFToken(); }

        // Skip whitespace and comments.
        for (;;) {

            // Eat whitespace.
            while (Character.isWhitespace((char) this.nextChar)) {
                this.readNextChar();
                if (this.nextChar == -1) { return new EOFToken(); }
            }

            // Skip comment.
            if (this.nextChar != '/') break;
            this.readNextChar();
            switch (this.nextChar) {

            case -1:
            default:
                return new OperatorToken("/");

            case '=':
                this.readNextChar();
                return new OperatorToken("/=");

            case '/':
                for (;;) {
                    this.readNextChar();
                    if (this.nextChar == -1) { return new EOFToken(); }
                    if (this.nextChar == '\r' || this.nextChar == '\n') break;
                }
                break;

            case '*':
                boolean gotStar = false;
                for (;;) {
                    this.readNextChar();
                    if (this.nextChar == -1) throw new ScanException("EOF in C-style comment");
                    if (this.nextChar == '*') {
                        gotStar = true;
                    } else
                    if (gotStar && this.nextChar == '/') {
                        this.readNextChar();
                        break;
                    } else {
                        gotStar = false;
                    }
                }
                break;
            }
        }

        /*
         * Whitespace and comments are now skipped; "nextChar" is definitely
         * the first character of the token.
         */
        this.tokenLineNumber   = this.nextCharLineNumber;
        this.tokenColumnNumber = this.nextCharColumnNumber;

        if (this.nextChar == -1) return new EOFToken();

        // Scan identifier.
        if (Character.isJavaIdentifierStart((char) this.nextChar)) {
            StringBuffer sb = new StringBuffer();
            sb.append((char) this.nextChar);
            for (;;) {
                this.readNextChar();
                if (
                    this.nextChar == -1 ||
                    !Character.isJavaIdentifierPart((char) this.nextChar)
                ) break;
                sb.append((char) this.nextChar);
            }
            String s = sb.toString();
            if (s.equals("true") || s.equals("false")) {
                return new BooleanLiteralToken("true".equals(s));
            }
            if (s.equals("null")) return new NullLiteralToken();
            {
                String v = (String) Scanner.JAVA_KEYWORDS.get(s);
                if (v != null) return new KeywordToken(v);
            }
            return new IdentifierToken(s);
        }

        // Scan numeric literal.
        if (Character.isDigit((char) this.nextChar)) {
            return this.scanNumericLiteral(0);
        }

        // A "." is special: Could either be a floating-point constant like ".001", or the "."
        // operator.
        if (this.nextChar == '.') {
            this.readNextChar();
            if (Character.isDigit((char) this.nextChar)) {
                return this.scanNumericLiteral(2);
            } else {
                return new OperatorToken(".");
            }
        }

        // Scan string literal.
        if (this.nextChar == '"') {
            StringBuffer sb = new StringBuffer("");
            this.readNextChar();
            if (this.nextChar == -1) throw new ScanException("EOF in string literal");
            if (this.nextChar == '\r' || this.nextChar == '\n') throw new ScanException("Line break in string literal");
            while (this.nextChar != '"') {
                sb.append(unescapeCharacterLiteral());
            }
            this.readNextChar();
            return new StringLiteralToken(sb.toString());
        }

        // Scan character literal.
        if (this.nextChar == '\'') {
            this.readNextChar();
            char lit = unescapeCharacterLiteral();
            if (this.nextChar != '\'') throw new ScanException("Closing single quote missing");
            this.readNextChar();

            return new CharacterLiteralToken(lit);
        }

        // Scan separator / operator.
        {
            String v = (String) Scanner.JAVA_OPERATORS.get(
                new String(new char[] { (char) this.nextChar })
            );
            if (v != null) {
                for (;;) {
                    this.readNextChar();
                    String v2 = (String) Scanner.JAVA_OPERATORS.get(v + (char) this.nextChar);
                    if (v2 == null) return new OperatorToken(v);
                    v = v2;
                }
            }
        }

        throw new ScanException("Invalid character input \"" + (char) this.nextChar + "\" (character code " + this.nextChar + ")");
    }

    private Token scanNumericLiteral(int initialState) throws ScanException, IOException {
        StringBuffer sb = (initialState == 2) ? new StringBuffer("0.") : new StringBuffer();
        int state = initialState;
        for (;;) {
            switch (state) {
    
            case 0: // First character.
                if (this.nextChar == '0') {
                    state = 6;
                } else
                /* if (Character.isDigit((char) this.nextChar)) */ {
                    sb.append((char) this.nextChar);
                    state = 1;
                }
                break;
    
            case 1: // Decimal digits.
                if (Character.isDigit((char) this.nextChar)) {
                    sb.append((char) this.nextChar);
                } else
                if (this.nextChar == 'l' || this.nextChar == 'L') {
                    this.readNextChar();
                    return this.stringToLongLiteralToken(sb.toString(), 10);
                } else
                if (this.nextChar == 'f' || this.nextChar == 'F') {
                    this.readNextChar();
                    return this.stringToFloatLiteralToken(sb.toString());
                } else
                if (this.nextChar == 'd' || this.nextChar == 'D') {
                    this.readNextChar();
                    return this.stringToDoubleLiteralToken(sb.toString());
                } else
                if (this.nextChar == '.') {
                    sb.append('.');
                    state = 2;
                } else
                if (this.nextChar == 'E' || this.nextChar == 'e') {
                    sb.append('E');
                    state = 3;
                } else
                {
                    return this.stringToIntegerLiteralToken(sb.toString(), 10);
                }
                break;
    
            case 2: // After decimal point.
                if (Character.isDigit((char) this.nextChar)) {
                    sb.append((char) this.nextChar);
                } else
                if (this.nextChar == 'e' || this.nextChar == 'E') {
                    sb.append('E');
                    state = 3;
                } else
                if (this.nextChar == 'f' || this.nextChar == 'F') {
                    this.readNextChar();
                    return this.stringToFloatLiteralToken(sb.toString());
                } else
                if (this.nextChar == 'd' || this.nextChar == 'D') {
                    this.readNextChar();
                    return this.stringToDoubleLiteralToken(sb.toString());
                } else
                {
                    return this.stringToDoubleLiteralToken(sb.toString());
                }
                break;
    
            case 3: // Read exponent.
                if (Character.isDigit((char) this.nextChar)) {
                    sb.append((char) this.nextChar);
                    state = 5;
                } else
                if (this.nextChar == '-' || this.nextChar == '+') {
                    sb.append((char) this.nextChar);
                    state = 4;
                } else
                {
                    throw new ScanException("Exponent missing after \"E\"");
                }
                break;
    
            case 4: // After exponent sign.
                if (Character.isDigit((char) this.nextChar)) {
                    sb.append((char) this.nextChar);
                    state = 5;
                } else
                {
                    throw new ScanException("Exponent missing after \"E\" and sign");
                }
                break;
    
            case 5: // After first exponent digit.
                if (Character.isDigit((char) this.nextChar)) {
                    sb.append((char) this.nextChar);
                } else
                if (this.nextChar == 'f' || this.nextChar == 'F') {
                    this.readNextChar();
                    return this.stringToFloatLiteralToken(sb.toString());
                } else
                if (this.nextChar == 'd' || this.nextChar == 'D') {
                    this.readNextChar();
                    return this.stringToDoubleLiteralToken(sb.toString());
                } else
                {
                    return this.stringToDoubleLiteralToken(sb.toString());
                }
                break;
    
            case 6: // After leading zero
                if ("01234567".indexOf(this.nextChar) != -1) {
                    sb.append((char) this.nextChar);
                    state = 7;
                } else
                if (this.nextChar == 'l' || this.nextChar == 'L') {
                    this.readNextChar();
                    return this.stringToLongLiteralToken("0", 10);
                } else
                if (this.nextChar == 'f' || this.nextChar == 'F') {
                    this.readNextChar();
                    return this.stringToFloatLiteralToken("0");
                } else
                if (this.nextChar == 'd' || this.nextChar == 'D') {
                    this.readNextChar();
                    return this.stringToDoubleLiteralToken("0");
                } else
                if (this.nextChar == '.') {
                    sb.append("0.");
                    state = 2;
                } else
                if (this.nextChar == 'E' || this.nextChar == 'e') {
                    sb.append('E');
                    state = 3;
                } else
                if (this.nextChar == 'x' || this.nextChar == 'X') {
                    state = 8;
                } else
                {
                    return this.stringToIntegerLiteralToken("0", 10);
                }
                break;
    
            case 7: // In octal literal.
                if ("01234567".indexOf(this.nextChar) != -1) {
                    sb.append((char) this.nextChar);
                } else
                if (this.nextChar == 'l' || this.nextChar == 'L') {
                    // Octal long literal.
                    this.readNextChar();
                    return this.stringToLongLiteralToken(sb.toString(), 8);
                } else
                {
                    // Octal int literal
                    return this.stringToIntegerLiteralToken(sb.toString(), 8);
                }
                break;
    
            case 8: // First hex digit
                if (Character.digit((char) this.nextChar, 16) != -1) {
                    sb.append((char) this.nextChar);
                    state = 9;
                } else
                {
                    throw new ScanException("Hex digit expected after \"0x\"");
                }
                break;
    
            case 9:
                if (Character.digit((char) this.nextChar, 16) != -1) {
                    sb.append((char) this.nextChar);
                } else
                if (this.nextChar == 'l' || this.nextChar == 'L') {
                    // Hex long literal
                    this.readNextChar();
                    return this.stringToLongLiteralToken(sb.toString(), 16);
                } else
                {
                    // Hex long literal
                    return this.stringToIntegerLiteralToken(sb.toString(), 16);
                }
                break;
            }
            this.readNextChar();
        }
    }

    private LiteralToken stringToIntegerLiteralToken(final String s, int radix) throws ScanException {
        int x;
        switch (radix) {

        case 10:
            // Special case: Decimal literal 2^31 must only appear in "negated" context, i.e.
            // "-2147483648" is a valid long literal, but "2147483648" is not.
            if (s.equals("2147483648")) return new LiteralToken(Integer.TYPE) {
                public Object getLiteralValue() throws ScanException { throw new ScanException("This value may only appear in a negated literal"); }
                public Object getNegatedLiteralValue()               { return new Integer(Integer.MIN_VALUE); }
                public String toString()                             { return s; }
            };
            try {
                x = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new ScanException("Value of decimal integer literal \"" + s + "\" is out of range");
            }
            break;

        case 8:
            // Cannot use "Integer.parseInt(s, 8)" because that parses SIGNED values.
            x = 0;
            for (int i = 0; i < s.length(); ++i) {
                if ((x & 0xe0000000) != 0) throw new ScanException("Value of octal integer literal \"" + s + "\" is out of range");
                x = (x << 3) + Character.digit(s.charAt(i), 8);
            }
            break;

        case 16:
            // Cannot use "Integer.parseInt(s, 16)" because that parses SIGNED values.
            x = 0;
            for (int i = 0; i < s.length(); ++i) {
                if ((x & 0xf0000000) != 0) throw new ScanException("Value of hexadecimal integer literal \"" + s + "\" is out of range");
                x = (x << 4) + Character.digit(s.charAt(i), 16);
            }
            break;

        default:
            throw new RuntimeException("Illegal radix " + radix);
        }
        return new IntegerLiteralToken(x);
    }

    private LiteralToken stringToLongLiteralToken(final String s, int radix) throws ScanException {
        long x;
        switch (radix) {

        case 10:
            // Special case: Decimal literal 2^63 must only appear in "negated" context, i.e.
            // "-9223372036854775808" is a valid long literal, but "9223372036854775808" is not.
            if (s.equals("9223372036854775808")) return new LiteralToken(Long.TYPE) {
                public Object getLiteralValue() throws ScanException { throw new ScanException("This value may only appear in a negated literal"); }
                public Object getNegatedLiteralValue()               { return new Long(Long.MIN_VALUE); }
                public String toString()                             { return "9223372036854775808L"; }
            };
    
            try {
                x = Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new ScanException("Value of decimal long literal \"" + s + "\" is out of range");
            }
            break;

        case 8:
            // Cannot use "Long.parseLong(s, 8)" because that parses SIGNED values.
            x = 0L;
            for (int i = 0; i < s.length(); ++i) {
                if ((x & 0xe000000000000000L) != 0L) throw new ScanException("Value of octal long literal \"" + s + "\" is out of range");
                x = (x << 3) + Character.digit(s.charAt(i), 8);
            }
            break;

        case 16:
            // Cannot use "Long.parseLong(s, 16)" because that parses SIGNED values.
            x = 0L;
            for (int i = 0; i < s.length(); ++i) {
                if ((x & 0xf000000000000000L) != 0L) throw new ScanException("Value of hexadecimal long literal \"" + s + "\" is out of range");
                x = (x << 4) + (long) Character.digit(s.charAt(i), 16);
            }
            break;

        default:
            throw new RuntimeException("Illegal radix " + radix);
        }
        return new LongLiteralToken(x);
    }

    private LiteralToken stringToFloatLiteralToken(final String s) throws ScanException {
        float f;
        try {
            f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new ScanException("Value of float literal \"" + s + "\" is out of range");
        }

        return new FloatLiteralToken(f);
    }

    private LiteralToken stringToDoubleLiteralToken(final String s) throws ScanException {
        double d;
        try {
            d = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new ScanException("Value of double literal \"" + s + "\" is out of range");
        }

        return new DoubleLiteralToken(d);
    }

    private char unescapeCharacterLiteral() throws ScanException, IOException {
        if (this.nextChar == -1) throw new ScanException("EOF in character literal");

        if (this.nextChar != '\\') {
            char res = (char) this.nextChar;
            this.readNextChar();
            return res;
        }
        this.readNextChar();
        int idx = "btnfr".indexOf(this.nextChar);
        if (idx != -1) {
            char res = "\b\t\n\f\r".charAt(idx);
            this.readNextChar();
            return res;
        }
        idx = "01234567".indexOf(this.nextChar);
        if (idx != -1) {
            int code = idx;
            this.readNextChar();
            idx = "01234567".indexOf(this.nextChar);
            if (idx == -1) return (char) code;
            code = 8 * code + idx;
            this.readNextChar();
            idx = "01234567".indexOf(this.nextChar);
            if (idx == -1) return (char) code;
            code = 8 * code + idx;
            if (code > 255) throw new ScanException("Invalid octal escape");
            this.readNextChar();
            return (char) code;
        }

        char res = (char) this.nextChar;
        this.readNextChar();
        return res;
    }

    // Read one character and store in "nextChar".
    private void readNextChar() throws IOException {
        this.nextChar = this.in.read();
        if (this.nextChar == '\r') {
            ++this.nextCharLineNumber;
            this.nextCharColumnNumber = 0;
            this.crLfPending = true;
        } else
        if (this.nextChar == '\n') {
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
//System.out.println("'" + (char) nextChar + "' = " + (int) nextChar);
    }

    public static class Location {
        private Location(String optionalFileName, short lineNumber, short columnNumber) {
            this.optionalFileName = optionalFileName;
            this.lineNumber       = lineNumber;
            this.columnNumber     = columnNumber;
        }
        public String getFileName() { return this.optionalFileName; }
        public short getLineNumber() { return this.lineNumber; }
        public short getColumnNumber() { return this.columnNumber; }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalFileName != null) {
                sb.append("File ").append(this.optionalFileName).append(", ");
            }
            sb.append("Line ").append(this.lineNumber).append(", ");
            sb.append("Column ").append(this.columnNumber);
            return sb.toString();
        }
        private /*final*/ String optionalFileName;
        private /*final*/ short  lineNumber;
        private /*final*/ short  columnNumber;
    }

    private static final boolean DEBUG = false;

    private /*final*/ String     optionalFileName;
    private /*final*/ Reader     in;
    private int              nextChar  = -1;
    private boolean          crLfPending = false;
    private short            nextCharLineNumber;
    private short            nextCharColumnNumber;

    private Token nextToken, nextButOneToken;
    private short tokenLineNumber;
    private short tokenColumnNumber;

    private static final Map JAVA_KEYWORDS = new HashMap();
    static {
        String[] ks = {
            "abstract", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else",
            "extends", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "try",
            "void", "volatile", "while"
        };
        for (int i = 0; i < ks.length; ++i) Scanner.JAVA_KEYWORDS.put(ks[i], ks[i]);
    }
    private static final Map JAVA_OPERATORS = new HashMap();
    static {
        String[] ops = {
            // Separators:
            "(", ")", "{", "}", "[", "]", ";", ",", ".",
            // Operators:
            "=",  ">",  "<",  "!",  "~",  "?",  ":",
            "==", "<=", ">=", "!=", "&&", "||", "++", "--",
            "+",  "-",  "*",  "/",  "&",  "|",  "^",  "%",  "<<",  ">>",  ">>>",
            "+=", "-=", "*=", "/=", "&=", "|=", "^=", "%=", "<<=", ">>=", ">>>=",
        };
        for (int i = 0; i < ops.length; ++i) Scanner.JAVA_OPERATORS.put(ops[i], ops[i]);
    }


    /**
     * An exception that reflects an error during parsing.
     */
    public class ScanException extends LocatedException {
        public ScanException(String message) {
            super(message, new Location(
                Scanner.this.optionalFileName,
                Scanner.this.nextCharLineNumber,
                Scanner.this.nextCharColumnNumber
            ));
        }
    }

    public static class LocatedException extends Exception {
        LocatedException(String message, Scanner.Location optionalLocation) {
            super(message);
            this.optionalLocation = optionalLocation;
        }

        /**
         * Returns the message specified at creation time, preceeded
         * with nicely formatted location information (if any).
         */
        public String getMessage() {
            return (this.optionalLocation == null) ? super.getMessage() : this.optionalLocation.toString() + ": " + super.getMessage();
        }

        /**
         * Returns the {@link Scanner.Location} object specified at
         * construction time (may be <code>null</code>).
         */
        public Scanner.Location getLocation() {
            return this.optionalLocation;
        }

        private final Scanner.Location optionalLocation;
    }
}
