
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

package org.codehaus.janino.util.iterator;

import java.util.*;

/**
 * An {@link java.util.Iterator} that iterates over a delegate, and while
 * it encounters an array, a {@link java.util.Collection}, an
 * {link Enumeration} or a {@link java.util.Iterator} element, it iterates
 * into it recursively.
 * <p>
 * Be aware that {@link #hasNext()} must read ahead one element.
 */
public class TraversingIterator implements Iterator {
    private final Stack nest = new Stack(); // Iterator
    private Object      nextElement = null;
    private boolean     nextElementRead = false; // Have we read ahead?

    public TraversingIterator(Iterator delegate) {
        this.nest.push(delegate);
    }

    public boolean hasNext() {
        return this.nextElementRead || this.readNext();
    }

    public Object next() {
        if (!this.nextElementRead && !this.readNext()) throw new NoSuchElementException();
        this.nextElementRead = false;
        return this.nextElement;
    }

    /**
     * Reads the next element and stores it in {@link #nextElement}.
     * @return <code>false</code> if no more element can be read.
     */
    private boolean readNext() {
        while (!this.nest.empty()) {
            Iterator it = (Iterator) this.nest.peek();
            if (!it.hasNext()) {
                this.nest.pop();
                continue;
            }
            Object o = it.next();
            if (o instanceof Iterator) {
                this.nest.push(o);
            } else
            if (o instanceof Object[]) {
                this.nest.push(Arrays.asList((Object[]) o).iterator());
            } else
            if (o instanceof Collection) {
                this.nest.push(((Collection) o).iterator());
            } else
            if (o instanceof Enumeration) {
                this.nest.push(new EnumerationIterator((Enumeration) o));
            } else
            {
                this.nextElement = o;
                this.nextElementRead = true;
                return true;
            }
        }
        return false;
    }

    public void remove() { throw new UnsupportedOperationException("remove"); }
}
