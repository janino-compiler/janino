
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
 * An {@link java.util.Iterator} that iterates over the elements of an
 * {@link java.util.Enumeration}.
 */
public class EnumerationIterator implements Iterator {
    private final Enumeration e;

    public EnumerationIterator(Enumeration e) { this.e = e; }

    public boolean hasNext() { return this.e.hasMoreElements(); }
    public Object  next()    { return this.e.nextElement(); }
    public void    remove()  { throw new UnsupportedOperationException("remove"); }
}
