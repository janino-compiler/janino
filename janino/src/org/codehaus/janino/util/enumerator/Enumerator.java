
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
 * A class that represents an enumerated value. Its main feature is its
 * {@link #toString()} method, which reconstructs the clear text value
 * through reflection.
 * <p>
 * To use this class, derive from it and define one or more
 * <code>public static final</code> fields, as follows:
 * <pre>
 * public class Color extends Enumerator {
 *     public static final Color RED   = new Color(0);
 *     public static final Color GREEN = new Color(1);
 *     public static final Color BLUE  = new Color(2);
 *
 *     public Color(String s) throws EnumeratorFormatException { super(s); }
 *
 *     private Color(int value) { super(value); }
 * }
 * </pre>
 */
public abstract class Enumerator {
    private final int value;

    /**
     * Initialize the enumerator to the given value.
     */
    protected Enumerator(int value) {
        this.value = value;
    }

    /**
     * Check the object's value
     */
    public boolean equals(Object that) {
        return that.getClass() == this.getClass() && this.value == ((Enumerator) that).value;
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
            Map m = (Map) Enumerator.mappings.get(clazz);
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
                    Enumerator e = (Enumerator) f.get(null);
                    m.put(f.getName(), new Integer(e.value));
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException("SNO: Field \"" + f + "\" is inaccessible");
                }
            }
        }

        // Cache it.
        Enumerator.mappings.put(this.getClass(), m);
        return m;
    }
    private static final Map mappings = new HashMap(); // Class => Map String name => Integer value

    /**
     * Initialize an {@link Enumerator} from a string.
     * <p>
     * The given string is converted into a value by looking at the class's
     * <code>public static final</code> fields which have the same type as the class itself.
     * 
     * @throws EnumeratorFormatException if the string cannot be identified
     */
    protected Enumerator(String s) throws EnumeratorFormatException {
        Integer v = (Integer) this.getMapping().get(s);
        if (v == null) throw new EnumeratorFormatException(s);
        this.value = v.intValue();
    }

    /**
     * Convert an {@link Enumerator} into a clear-text string.
     * <p>
     * Examine the object through reflection for <code>public static final</code>
     * fields that have the same type as this object, and return the name of the fields who's value
     * matches the object's value.
     */
    public String toString() {
        for (Iterator esi = this.getMapping().entrySet().iterator(); esi.hasNext();) {
            Map.Entry me = (Map.Entry) esi.next();
            if (this.value == ((Integer) me.getValue()).intValue()) {
                return (String) me.getKey();
            }
        }
        return Integer.toString(this.value);
    }
}
