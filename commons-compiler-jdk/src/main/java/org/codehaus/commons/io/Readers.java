
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2018 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

public final
class Readers {

    private Readers() {}

    /**
     * @return {@link FilterReader} that runs the <var>runnable</var> right before the first character is read
     */
    public static Reader
    onFirstChar(Reader in, final Runnable runnable) {

        return new FilterReader(in) {

            private boolean hadChars;

            @Override public int
            read() throws IOException {
                this.aboutToRead();
                return super.read();
            }

            @Override @NotNullByDefault(false) public int
            read(char[] cbuf, int off, int len) throws IOException {
                this.aboutToRead();
                return super.read(cbuf, off, len);
            }

            @Override public long
            skip(long n) throws IOException {
                this.aboutToRead();
                return super.skip(n);
            }

            private void
            aboutToRead() {
                if (!this.hadChars) {
                    runnable.run();
                    this.hadChars = true;
                }
            }
        };
    }

    /**
     * @return A {@link FilterReader} that tracks line and column numbers while characters are being read
     */
    public static Reader
    trackLineAndColumn(Reader in, final LineAndColumnTracker tracker) {

        return new FilterReader(in) {

            @Override public int
            read() throws IOException {
                int c = super.read();
                if (c >= 0) tracker.consume((char) c);
                return c;
            }

            @Override @NotNullByDefault(false) public int
            read(char[] cbuf, final int off, int len) throws IOException {
                if (len <= 0) return 0;
                int c = this.read();
                if (c < 0) return -1;
                cbuf[off] = (char) c;
                return 1;
            }

            @Override public long
            skip(final long n) throws IOException {
                if (n <= 0) return 0;
                int c = this.read();
                if (c < 0) return 0;
                return 1;
            }

            @Override public boolean markSupported()                             { return false;            }
            @Override public void    mark(int readAheadLimit) throws IOException { throw new IOException(); }
            @Override public void    reset() throws IOException                  { throw new IOException(); }
        };
    }
}
