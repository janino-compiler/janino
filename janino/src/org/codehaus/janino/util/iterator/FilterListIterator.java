
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
 * An @link java.util.ListIterator that retrieves its elements from a delegate
 * {@link java.util.ListIterator}. The default implementation simply passes
 * all method invocations to the delegate.
 */
public abstract class FilterListIterator implements ListIterator {
    protected final ListIterator delegate;

    public FilterListIterator(ListIterator delegate) {
        this.delegate = delegate;
    }

    public boolean hasNext()       { return this.delegate.hasNext(); }
    public Object  next()          { return this.delegate.next(); }
    public boolean hasPrevious()   { return this.delegate.hasPrevious(); }
    public Object  previous()      { return this.delegate.previous(); }
    public int     nextIndex()     { return this.delegate.nextIndex(); }
    public int     previousIndex() { return this.delegate.previousIndex(); }
    public void    remove()        { this.delegate.remove(); }
    public void    set(Object o)   { this.delegate.set(o); }
    public void    add(Object o)   { this.delegate.add(o); }
}
