
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
 * An {@link java.util.Iterator} that transforms its elements on-the-fly.
 */
public abstract class TransformingIterator extends FilterIterator {
    public TransformingIterator(Iterator delegate) {
        super(delegate);
    }

    public final Object next() {
        return this.transform(this.delegate.next());
    }

    /**
     * Derived classes must implement this method such that it does the
     * desired transformation.
     */
    protected abstract Object transform(Object o);
}
