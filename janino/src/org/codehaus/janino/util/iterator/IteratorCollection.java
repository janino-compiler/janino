
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
