
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
 * A {@link java.util.ListIterator} that reverses the direction of all operations
 * of a delegate {@link java.util.ListIterator}.
 */
public class ReverseListIterator extends FilterListIterator {
    public ReverseListIterator(ListIterator delegate) {
        super(delegate);
    }

    public boolean hasNext()       { return super.hasPrevious(); }
    public boolean hasPrevious()   { return super.hasNext(); }
    public Object  next()          { return super.previous(); }
    public Object  previous()      { return super.next(); }
    public int     nextIndex()     { throw new UnsupportedOperationException(); }
    public int     previousIndex() { throw new UnsupportedOperationException(); }
}
