
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

package org.codehaus.commons.compiler;

import java.io.Serializable;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Immutable representation of the location of a character in a document, as defined by an (optional) file name, a
 * line number and a column number.
 */
public
class Location implements Serializable {

    /**
     * Representation of an unspecified location.
     */
    public static final Location NOWHERE = new Location("<internally generated location>", -1, -1);

    @Nullable private final String fileName;
    private final int              lineNumber;
    private final int              columnNumber;

    /**
     * @param fileName A human-readable indication where the document related to this {@link Location} can be
     *                         found
     */
    public
    Location(@Nullable String fileName, int lineNumber, int columnNumber) {
        this.fileName = fileName;
        this.lineNumber       = lineNumber;
        this.columnNumber     = columnNumber;
    }

    /**
     * @return The "file name" associated with this location, or {@code null}
     */
    @Nullable public String getFileName() { return this.fileName; }

    /**
     * @return The line number associated with this location, or -1
     */
    public int getLineNumber() { return this.lineNumber; }

    /**
     * @return The column number associated with this location, or -1
     */
    public int getColumnNumber() { return this.columnNumber; }

    /**
     * Converts this {@link Location} into an english text, like '{@code File Main.java, Line 23, Column 79}'.
     */
    @Override public String
    toString() {

        StringBuilder sb = new StringBuilder();
        if (this.fileName != null) {
            sb.append("File '").append(this.fileName).append("', ");
        }
        sb.append("Line ").append(this.lineNumber).append(", ");
        sb.append("Column ").append(this.columnNumber);
        return sb.toString();
    }
}
