
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
 * An {@link java.util.Iterator} that iterates over a delegate, which produces
 * arrays, {@link java.util.Collection}s, {link Enumeration}s or
 * {@link java.util.Iterator}s. This {@link java.util.Iterator} returns the
 * elements of these objects.
 * <p>
 * The count of dimensions is declared at construction. Count "1" produces an
 * {@link java.util.Iterator} that adds no functionality to its delegate, count
 * "2" produces an {@link Iterator} that behaves as explained above, and so
 * forth.
 */
public class MultiDimensionalIterator implements Iterator {
    private final Iterator[] nest;
    private static final Iterator EMPTY_ITERATOR = new Iterator() {
        public boolean hasNext() { return false; }
        public Object  next()    { throw new NoSuchElementException(); }
        public void    remove()  { throw new UnsupportedOperationException("remove"); }
    };

    public MultiDimensionalIterator(Iterator delegate, int dimensionCount) {
        this.nest = new Iterator[dimensionCount];
        this.nest[0] = delegate;
        for (int i = 1; i < dimensionCount; ++i) this.nest[i] = MultiDimensionalIterator.EMPTY_ITERATOR;
    }

    /**
     * @throws UniterableElementException
     */
    public boolean hasNext() {

        // Unroll this check because it is so performance critical:
        if (this.nest[this.nest.length - 1].hasNext()) return true;

        int i = this.nest.length - 2;
        if (i < 0) return false;

        for (;;) {
            if (!this.nest[i].hasNext()) {
                if (i == 0) return false;
                --i;
            } else {
                if (i == this.nest.length - 1) return true;
                Object o = this.nest[i].next();
                if (o instanceof Iterator) {
                    this.nest[++i] = (Iterator) o;
                } else
                if (o instanceof Object[]) {
                    this.nest[++i] = Arrays.asList((Object[]) o).iterator();
                } else
                if (o instanceof Collection) {
                    this.nest[++i] = ((Collection) o).iterator();
                } else
                if (o instanceof Enumeration) {
                    this.nest[++i] = new EnumerationIterator((Enumeration) o);
                } else
                {
                    throw new UniterableElementException();
                }
            }
        }
    }

    public Object next() {
        if (!this.hasNext()) throw new NoSuchElementException();
        return this.nest[this.nest.length - 1].next();
    }

    public void remove() { throw new UnsupportedOperationException("remove"); }
}
