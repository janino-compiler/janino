
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link java.util.Iterator} that traverses a {@link java.util.Collection} of
 * {@link java.util.Iterator}s.
 */
public class MultiIterator implements Iterator {
    private static final Iterator AT_END = new Iterator() {
        public boolean hasNext() { return false; }
        public Object  next()    { throw new NoSuchElementException(); }
        public void    remove()  { throw new UnsupportedOperationException(); }
    };

    private final Iterator outer; // Over Iterators, Collections or arrays
    private Iterator       inner = MultiIterator.AT_END;

    /**
     * @param iterators An array of {@link Iterator}s
     */
    public MultiIterator(Iterator[] iterators) {
        this.outer = Arrays.asList(iterators).iterator();
    }

    /**
     * @param collections An array of {@link Collection}s
     */
    public MultiIterator(Collection[] collections) {
        this.outer = Arrays.asList(collections).iterator();
    }

    /**
     * @param arrays An array of arrays
     */
    public MultiIterator(Object[][] arrays) {
        this.outer = Arrays.asList(arrays).iterator();
    }

    /**
     * @param collection A {@link Collection} of {@link Collection}s, {@link Iterator}s and/or arrays
     */
    public MultiIterator(Collection collection) {
        this.outer = collection.iterator();
    }

    /**
     * @param iterator An iterator over {@link Collection}s, {@link Iterator}s and/or arrays
     */
    public MultiIterator(Iterator iterator) {
        this.outer = iterator;
    }

    /**
     * @param array An array of {@link Collection}s, {@link Iterator}s and/or arrays
     */
    public MultiIterator(Object[] array) {
        this.outer = Arrays.asList(array).iterator();
    }

    /**
     * Iterates over the given {@link Collection}, prepended with the given {@link Object}.
     */
    public MultiIterator(Object object, Collection collection) {
        this.outer = Arrays.asList(new Object[] {
            new Object[] { object },
            collection
        }).iterator();
    }

    /**
     * Iterates over the given {@link Collection}, appended with the given {@link Object}.
     */
    public MultiIterator(Collection collection, Object object) {
        this.outer = Arrays.asList(new Object[] {
            collection,
            new Object[] { object }
        }).iterator();
    }

    /**
     * Iterates over the given {@link Iterator}, prepended with the given <code>prefix</code>.
     */
    public MultiIterator(Object prefix, Iterator iterator) {
        this.outer = Arrays.asList(new Object[] {
            new Object[] { prefix },
            iterator
        }).iterator();
    }

    /**
     * Iterates over the given {@link Iterator}, appended with the given <code>suffix</code>.
     */
    public MultiIterator(Iterator iterator, Object suffix) {
        this.outer = Arrays.asList(new Object[] {
            iterator,
            new Object[] { suffix }
        }).iterator();
    }

    public boolean hasNext() {
        for (;;) {
            if (this.inner.hasNext()) return true;
            if (!this.outer.hasNext()) return false;
            Object o = this.outer.next();
            if (o instanceof Iterator) {
                this.inner = (Iterator) o;
            } else
            if (o instanceof Collection) {
                this.inner = ((Collection) o).iterator();
            } else
            if (o instanceof Object[]) {
                this.inner = Arrays.asList((Object[]) o).iterator();
            } else
            {
                throw new RuntimeException("Unexpected element type \"" + o.getClass().getName() + "\"");
            }
        } 
    }

    public Object next() {
        if (this.hasNext()) return this.inner.next();
        throw new NoSuchElementException();
    }

    public void remove() {
        this.inner.remove();
    }
}
