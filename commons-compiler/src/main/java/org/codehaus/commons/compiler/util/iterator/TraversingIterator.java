
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

package org.codehaus.commons.compiler.util.iterator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.codehaus.commons.nullanalysis.Nullable;

/**
 * An {@link java.util.Iterator} that iterates over a delegate, and while it encounters an array, a {@link
 * java.util.Collection}, an {@link java.util.Enumeration} or a {@link java.util.Iterator} element, it iterates over it
 * recursively.
 * <p>
 *   Be aware that {@link #hasNext()} must read ahead one element.
 * </p>
 */
public
class TraversingIterator implements Iterator<Object> {

    private final Stack<Iterator<?>> nest = new Stack<Iterator<?>>(); // Iterator
    @Nullable private Object         nextElement;
    private boolean                  nextElementRead;                 // Have we read ahead?

    public
    TraversingIterator(Iterator<?> delegate) { this.nest.push(delegate); }

    @Override public boolean
    hasNext() { return this.nextElementRead || this.readNext(); }

    @Override public Object
    next() {
        if (!this.nextElementRead && !this.readNext()) throw new NoSuchElementException();
        this.nextElementRead = false;
        assert this.nextElement != null;
        return this.nextElement;
    }

    /**
     * Reads the next element and stores it in {@link #nextElement}.
     *
     * @return {@code false} if no more element can be read
     */
    private boolean
    readNext() {
        while (!this.nest.empty()) {
            Iterator<?> it = this.nest.peek();
            if (!it.hasNext()) {
                this.nest.pop();
                continue;
            }
            Object o = it.next();
            if (o instanceof Iterator) {
                this.nest.push((Iterator<?>) o);
            } else
            if (o instanceof Object[]) {
                this.nest.push(Arrays.asList((Object[]) o).iterator());
            } else
            if (o instanceof Collection) {
                this.nest.push(((Collection<?>) o).iterator());
            } else
            if (o instanceof Enumeration) {
                this.nest.push(new EnumerationIterator<Object>((Enumeration<?>) o));
            } else
            {
                this.nextElement     = o;
                this.nextElementRead = true;
                return true;
            }
        }
        return false;
    }

    /**
     * @throws UnsupportedOperationException The {@link Iterator} currently being traversed doesn't support element
     *                                       removal
     * @see Iterator#remove()
     */
    @Override public void
    remove() { this.nest.peek().remove(); }
}
