
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
 * A {@link java.util.Collection} that lazily reads its elements from an
 * {@link java.util.Iterator}.
 */
public class IteratorCollection extends AbstractCollection {
    private final Iterator iterator;
    private final List     elements = new ArrayList();

    public IteratorCollection(Iterator iterator) {
        this.iterator = iterator;
    }

    public Iterator iterator() {
        return new Iterator() {
            Iterator elementsIterator = IteratorCollection.this.elements.iterator(); 
            public Object next() {
                if (this.elementsIterator != null) {
                    if (this.elementsIterator.hasNext()) return this.elementsIterator.next();
                    this.elementsIterator = null;
                }
                Object o = IteratorCollection.this.iterator.next();
                IteratorCollection.this.elements.add(o);
                return o;
            }
            public boolean hasNext() {
                return (
                    (this.elementsIterator != null && this.elementsIterator.hasNext())
                    || IteratorCollection.this.iterator.hasNext()
                );
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    public int size() {
        int size = 0;
        for (Iterator it = this.iterator(); it.hasNext(); it.next()) ++size;
        return size;
    }
}
