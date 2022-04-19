
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.codehaus.commons.compiler.util.Predicate;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * Utility method around {@link Iterable}s and {@link Iterator}s.
 */
public final
class Iterables {

    private Iterables() {}

    /**
     * @return An {@link Iterable} that filters the <var>delegate</var>'s elements by base class
     */
    public static <T> Iterable<T>
    filterByClass(Object[] delegate, final Class<T> qualifyingClass) {
        return Iterables.filterByClass(Arrays.asList(delegate), qualifyingClass);
    }

    /**
     * @return An {@link Iterable} that filters the <var>delegate</var>'s elements by base class
     */
    public static <T> Iterable<T>
    filterByClass(final Iterable<?> delegate, final Class<T> qualifyingClass) {

        return new Iterable<T>() {

            @Override public Iterator<T>
            iterator() { return Iterables.filterByClass(delegate.iterator(), qualifyingClass); }
        };
    }

    /**
     * @return An {@link Iterator} that filters the <var>delegate</var>'s products by base class
     */
    public static <T> Iterator<T>
    filterByClass(Iterator<?> delegate, final Class<T> qualifyingClass) {

        @SuppressWarnings("unchecked") Iterator<T>
        result = (Iterator<T>) Iterables.<Object>filter(
            delegate,
            new Predicate<Object>() {

                @Override public boolean
                evaluate(@Nullable Object o) { return o != null && qualifyingClass.isAssignableFrom(o.getClass()); }
            }
        );

        return result;
    }

    /**
     * @return An {@link Iterable} that discriminates the <var>delegate</var>'s elements with the <var>predicate</var>
     */
    public static <T> Iterable<T>
    filter(T[] delegate, final Predicate<? super T> predicate) {
        return Iterables.filter(Arrays.asList(delegate), predicate);
    }

    /**
     * @return An {@link Iterable} that discriminates the <var>delegate</var>'s elements with the <var>predicate</var>
     */
    public static <T> Iterable<T>
    filter(final Iterable<? extends T> delegate, final Predicate<? super T> predicate) {

        return new Iterable<T>() {

            @Override public Iterator<T>
            iterator() { return Iterables.filter(delegate.iterator(), predicate); }
        };
    }

    /**
     * @return An {@link Iterator} that discriminates the <var>delegate</var>'s products with the <var>predicate</var>
     */
    public static <T> Iterator<T>
    filter(final Iterator<? extends T> delegate, final Predicate<? super T> predicate) {

        return new Iterator<T>() {

            State       state = State.DEFAULT;
            @Nullable T nextElement; // Valid iff state==READ_AHEAD;

            @Override public boolean
            hasNext() {
                switch (this.state) {

                case DEFAULT:
                    while (delegate.hasNext()) {
                        T ne = delegate.next();
                        if (predicate.evaluate(ne)) {
                            this.nextElement = ne;
                            this.state       = State.READ_AHEAD;
                            return true;
                        }
                    }
                    this.state = State.AT_END;
                    return false;

                case READ_AHEAD:
                    return true;

                case AT_END:
                    return false;

                default:
                    throw new AssertionError(this.state);
                }
            }

            @Override @Nullable public T
            next() {

                if (!this.hasNext()) throw new NoSuchElementException();

                this.state = State.DEFAULT;

                T result = this.nextElement;
                this.nextElement = null;
                return result;
            }

            @Override public void
            remove() { delegate.remove(); }
        };
    }
    private
    enum State {

        /**
         * No element has been read ahead.
         */
        DEFAULT,

        /**
         * The next element has been read ahead.
         */
        READ_AHEAD,

        /**
         * The delegate Iterator is at its end.
         */
        AT_END,
    }

    /**
     * @return All elements of the <var>delegate</var>
     */
    public static <T> T[]
    toArray(Iterable<T> delegate, Class<T> elementType) {
        return Iterables.toArray(delegate.iterator(), elementType);
    }

    /**
     * @return All products of the <var>delegate</var>
     */
    public static <T> T[]
    toArray(Iterator<T> delegate, Class<T> componentType) {

        List<T> l = new ArrayList<>();
        while (delegate.hasNext()) l.add(delegate.next());

        @SuppressWarnings("unchecked") T[] array = (T[]) Array.newInstance(componentType, l.size());
        return l.toArray(array);
    }
}
