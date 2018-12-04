
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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import org.codehaus.commons.nullanalysis.NotNullByDefault;

public final
class Writers {

    private Writers() {}

    /**
     * @return A {@link FilterWriter} that runs the <var>runnable</var> right before the first character is written
     */
    public static Writer
    onFirstChar(Writer out, final Runnable runnable) {

        return new FilterWriter(out) {

            private boolean hadChars;

            @Override public void
            write(int c) throws IOException {
                this.aboutToWrite();
                super.write(c);
            }

            @Override @NotNullByDefault(false) public void
            write(char[] cbuf, int off, int len) throws IOException {
                this.aboutToWrite();
                super.write(cbuf, off, len);
            }

            @Override @NotNullByDefault(false) public void
            write(String str, int off, int len) throws IOException {
                this.aboutToWrite();
                super.write(str, off, len);
            }

            private void
            aboutToWrite() {
                if (!this.hadChars) {
                    runnable.run();
                    this.hadChars = true;
                }
            }
        };
    }

    /**
     * @return A {@link FilterWriter} that tracks kline and column numbers while characters are written
     */
    public static Writer
    trackLineAndColumn(Writer out, final LineAndColumnTracker tracker) {

        return new FilterWriter(out) {

            @Override public void
            write(int c) throws IOException {
                super.write(c);
                tracker.consume((char) c);
            }

            @Override @NotNullByDefault(false) public void
            write(char[] cbuf, int off, int len) throws IOException {
                for (; len > 0; len--) this.write(cbuf[off++]);
            }

            @Override @NotNullByDefault(false) public void
            write(String str, int off, int len) throws IOException {
                for (; len > 0; len--) this.write(str.charAt(off++));
            }
        };
    }
}
