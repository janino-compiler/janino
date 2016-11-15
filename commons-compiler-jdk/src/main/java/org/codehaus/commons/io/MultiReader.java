
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Similar to {@link FilterReader}, but when the first delegate is at end-of-input, it continues with reading from the
 * next delegate.
 * <p>
 *   This {@link Reader} does not support MARK.
 * </p>
 */
public
class MultiReader extends Reader {
    private static final Reader EMPTY_READER = new StringReader("");

    private final List<Reader>     delegates;
    private final Iterator<Reader> delegateIterator;
    private Reader                 currentDelegate = MultiReader.EMPTY_READER;

    public
    MultiReader(List<Reader> delegates) {
        this.delegates        = delegates;
        this.delegateIterator = delegates.iterator();
    }

    public
    MultiReader(Reader[] delegates) { this(Arrays.asList(delegates)); }

    /**
     * Closes all delegates.
     */
    @Override public void
    close() throws IOException { for (Reader delegate : this.delegates) delegate.close(); }

    @Override public int
    read() throws IOException {
        for (;;) {
            int result = this.currentDelegate.read();
            if (result != -1) return result;
            if (!this.delegateIterator.hasNext()) return -1;
            this.currentDelegate = this.delegateIterator.next();
        }
    }

    @Override public long
    skip(long n) throws IOException {
        long skipped = 0L;
        for (;;) {
            long result = this.currentDelegate.skip(n - skipped);
            if (result != -1L) {
                skipped += result;
                if (skipped == n) return skipped;
                continue;
            }
            if (!this.delegateIterator.hasNext()) return skipped;
            this.currentDelegate = this.delegateIterator.next();
        }
    }

    @Override public int
    read(@Nullable final char[] cbuf, final int off, final int len) throws IOException {
        int read = 0;
        for (;;) {
            long result = this.currentDelegate.read(cbuf, off + read, len - read);
            if (result != -1L) {
                read += result;
                if (read == len) return read;
                continue;
            }
            if (!this.delegateIterator.hasNext()) return read == 0 ? -1 : read;
            this.currentDelegate = this.delegateIterator.next();
        }
    }
}
