
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

/**
 * Represents the location of a character in a file, as defined by
 * file name, line number and column number.
 */
public class Location {
    public Location(String optionalFileName, short lineNumber, short columnNumber) {
        this.optionalFileName = optionalFileName;
        this.lineNumber       = lineNumber;
        this.columnNumber     = columnNumber;
    }

    public String getFileName()     { return this.optionalFileName; }
    public short  getLineNumber()   { return this.lineNumber; }
    public short  getColumnNumber() { return this.columnNumber; }

    /**
     * Converts this {@link Location} into an english text, like<pre>
     * File Main.java, Line 23, Column 79</pre>
     */
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