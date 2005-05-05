
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
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

package org.codehaus.janino.util.enumerator;

import java.lang.reflect.*;
import java.util.*;

/**
 * A class that represents a set of enumerated values.
 * <p>
 * Its main features are its constructor, which initializes the object from a clear-text string,
 * and its {@link #toString()} method, which reconstructs the clear text values. Both is done
 * through reflection.
 * <p>
 * Use this class as follows:
 * <pre>
 * public class Food extends EnumeratorSet {
 *     public static final Food BEEF    = new Food(1);
 *     public static final Food LETTUCE = new Food(2);
 *     public static final Food BREAD   = new Food(4);
 *
 *     public static final Food NONE    = new Food(0);
 *     public static final Food ALL     = new Food(7);
 *
 *     public Food(String s) throws EnumeratorFormatException { super(s); }
 *     public Food add(Food that) { return new Food(super.add(that)); }
 *     public Food remove(Food that) { return new Food(super.remove(that)); }
 *
 *     private Food(int values) { super(values); }
 * }
 * </pre>
 */
public abstract class EnumeratorSet {
    private final int values;

    /**
     * Initialize the enumerator to the given OR'ed set of values.
     */
    protected EnumeratorSet(int values) {
        this.values = values;
    }

    /**
     * Add to the object's values the given OR'ed set of values.
     */
    protected int add(EnumeratorSet that) {
        return this.values | that.values;
    }

    /**
     * Remove an OR'ed set of values from the object's values.
     */
    protected int remove(EnumeratorSet that) {
        return this.values & ~that.values;
    }

    /**
     * Check if the object contains any of the given values.
     */
    public boolean contains(EnumeratorSet that) {
        if (this.getClass() != that.getClass()) throw new RuntimeException("EnumeratorSet class mismatch");
        return (this.values & that.values) != 0;
    }

    /**
     * Check if the object contain any of the given values.
     */
    public boolean containsAnyOf(EnumeratorSet that) {
        if (this.getClass() != that.getClass()) throw new RuntimeException("EnumeratorSet class mismatch");
        return (this.values & that.values) != 0;
    }

    /**
     * Check if the object contain all of the given values.
     */
    public boolean containsAllOf(EnumeratorSet that) {
        if (this.getClass() != that.getClass()) throw new RuntimeException("EnumeratorSet class mismatch");
        return (this.values & that.values) == that.values;
    }

    /**
     * Check the values' identity.
     */
    public boolean equals(Object that) {
        return that.getClass() == this.getClass() && this.values == ((EnumeratorSet) that).values;
    }

    /**
     * Examines the given {@link Class} and its superclasses for <code>public static final</code>
     * fields of the same type as <code>this</code>, and maps their names to their values.
     * @return {@link String} name => {@link Integer} values
     */
    private Map getMapping() {
        Class clazz = this.getClass();

        // Look up the mappings cache.
        {
            Map m = (Map) EnumeratorSet.mappings.get(clazz);
            if (m != null) return m;
        }

        // Cache miss, create a new mapping.
        Map m = new HashMap();
        Field[] fields = clazz.getFields();
        for (int i = 0; i < fields.length; ++i) {
            Field f = fields[i];
            if (
                f.getType() == clazz &&
                (f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == (Modifier.STATIC | Modifier.FINAL)
            ) {
                try {
                    EnumeratorSet es = (EnumeratorSet) f.get(null);
                    m.put(f.getName(), new Integer(es.values));
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("SNO: Field \"" + f + "\" is inaccessible");
                }
            }
        }

        // Cache it.
        EnumeratorSet.mappings.put(this.getClass(), m);
        return m;
    }
    private static final Map mappings = new HashMap(); // Class => Map String name => Integer value

    /**
     * Initialize an {@link EnumeratorSet} from a string.
     * <p>
     * Equivalent to <code>EnumeratorSet(s, ",")</code>.
     */
    protected EnumeratorSet(String s) throws EnumeratorFormatException {
        this(s, ",");
    }

    /**
     * Initialize an {@link EnumeratorSet} from a string.
     * <p>
     * The given string is parsed into tokens; each token is converted into a value by looking
     * at the class's <code>public static final</code> fields which have the same type as
     * the class itself. The values are OR'ed together.
     * 
     * @throws EnumeratorFormatException if a token cannot be identified
     */
    protected EnumeratorSet(
        String  s,
        String  delimiter
    ) throws EnumeratorFormatException {
        Map m = this.getMapping();

        StringTokenizer st = new StringTokenizer(s, delimiter);
        int values = 0;
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            Integer vs = (Integer) m.get(token);
            if (vs == null) throw new EnumeratorFormatException(token);
            values |= vs.intValue();
        }
        this.values = values;
    }

    /**
     * Convert an {@link EnumeratorSet} to a clear-text string.
     * <p>
     * Identical with <code>toString(",")</code>.
     */
    public String toString() {
        return this.toString(",");
    }

    /**
     * Convert an {@link EnumeratorSet} into a clear-text string.
     * <p>
     * Examine the object through reflection for <code>public static final</code>
     * fields that have the same type as this object, and collect the names of all fields who's
     * values are contained in the object's values. Return the names of these fields, concatenated
     * with the given <code>delimiter</code>.
     */
    public String toString(String delimiter) {
        int v = this.values;
        StringBuffer sb = new StringBuffer();

        String zeroName = null;
        for (Iterator esi = this.getMapping().entrySet().iterator(); esi.hasNext();) {
            Map.Entry me = (Map.Entry) esi.next();
            int fv = ((Integer) me.getValue()).intValue();
            if (fv == 0) {
                zeroName = (String) me.getKey();
            } else {
                if ((v & fv) == fv) {
                    if (sb.length() > 0) sb.append(delimiter);
                    sb.append((String) me.getKey());
                    v &= ~fv;
                }
            }
        }
        if (this.values == 0) return zeroName == null ? "0" : zeroName;
        if (v != 0) {
            if (sb.length() > 0) sb.append(delimiter);
            sb.append(v);
        }
        return sb.toString();
    }
}
