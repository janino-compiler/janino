
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.util;

import java.io.*;

/**
 * A {@link java.io.FilterReader} that copies the bytes being passed through
 * to a given {@link java.io.Writer}. This is in analogy with the UNIX "tee" command.
 */
public class TeeReader extends FilterReader {
    private final Writer  out;
    private final boolean closeWriterOnEOF;

    public TeeReader(Reader in, Writer out, boolean closeWriterOnEOF) {
        super(in);
        this.out              = out;
        this.closeWriterOnEOF = closeWriterOnEOF;
    }
    public void close() throws IOException {
        this.in.close();
        this.out.close();
    }
    public int read() throws IOException {
        int c = this.in.read();
        if (c == -1) {
            if (this.closeWriterOnEOF) {
                this.out.close();
            } else {
                this.out.flush();
            }
        } else {
            this.out.write(c);
        }
        return c;
    }
    public int read(char[] cbuf, int off, int len) throws IOException {
        int bytesRead = this.in.read(cbuf, off, len);
        if (bytesRead == -1) {
            if (this.closeWriterOnEOF) {
                this.out.close();
            } else {
                this.out.flush();
            }
        } else {
            this.out.write(cbuf, off, bytesRead);
        }
        return bytesRead;
    }
}
