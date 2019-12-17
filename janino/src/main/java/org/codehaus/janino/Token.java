
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016 Arno Unkrig. All rights reserved.
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

import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Immutable representation of a Java token.
 */
public final
class Token {

    @Nullable private final String fileName;
    private final int              lineNumber;
    private final int              columnNumber;

    @Nullable private Location location; // Created lazily to save overhead.

    /**
     * The type of this token.
     * <p>
     *   Strictly speaking, this field is redundant, because the token type can always be deduced from the token
     *   value, e.g. iff the value begins with "'", then the type is {@link TokenType#CHARACTER_LITERAL}.
     * </p>
     */
    public final TokenType type;

    /**
     * The text of the token exactly as it appears in the source code.
     * <p>
     *   Iff this token represents a <em>keyword</em>, {@code true}, {@code false}, {@code null} or an operator, then
     *   this string is {@link String#intern() interned}, so that it can be reference-compared with other interned
     *   strings.
     * </p>
     */
    public final String value;

    public
    Token(@Nullable String fileName, int lineNumber, int columnNumber, TokenType type, String value) {
        this.fileName = fileName;
        this.lineNumber       = lineNumber;
        this.columnNumber     = columnNumber;
        this.type             = type;
        this.value            = value;
    }

    public
    Token(Location location, TokenType type, String value) {
        this.fileName = location.getFileName();
        this.lineNumber       = location.getLineNumber();
        this.columnNumber     = location.getColumnNumber();
        this.location         = location;
        this.type             = type;
        this.value            = value;
    }

    /**
     * @return The location of the first character of this token
     */
    public Location
    getLocation() {

        if (this.location != null) return this.location;

        return (this.location = new Location(this.fileName, this.lineNumber, this.columnNumber));
    }

    @Override public String
    toString() { return this.value; }
}
