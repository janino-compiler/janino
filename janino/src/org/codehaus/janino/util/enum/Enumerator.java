
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

package org.codehaus.janino.util.enum;

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
