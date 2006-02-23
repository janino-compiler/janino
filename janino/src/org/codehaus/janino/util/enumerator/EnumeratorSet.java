
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

package org.codehaus.janino.util.enumerator;

import java.util.*;

/**
 * A class that represents an immutable set of {@link Enumerator}s.
 * <p>
 * Its main features are its constructor, which initializes the object from a clear-text string,
 * and its {@link #toString()} method, which reconstructs the clear text values.
 * <p>
 * Sample code can be found in the documentation of {@link Enumerator}.
 */
public class EnumeratorSet {
    private final Class enumeratorClass;
    private final Set   values;  // Enumerator-derived class
    /*package*/ String  optionalName = null;

    private EnumeratorSet(Class enumeratorClass, Set values) {
        this.enumeratorClass = enumeratorClass;
        this.values          = values;
    }

    /**
     * Construct an empty set for values of the given {@link Enumerator}-derived type.
     */
    public EnumeratorSet(Class enumeratorClass) {
        this(enumeratorClass, new HashSet());
    }

    /**
     * Construct a set for values of the given {@link Enumerator}-derived type. If the
     * <code>full</code> flag is <code>true</code>, all possible values are added to the set.
     */
    public EnumeratorSet(Class enumeratorClass, boolean full) {
        this(enumeratorClass, new HashSet());
        if (full) this.values.addAll(Enumerator.getInstances(enumeratorClass).values());
    }

    /**
     * Construct a set for values of the given {@link Enumerator}-derived type and initialize it
     * from a string.
     * <p>
     * Equivalent to <code>EnumeratorSet(enumeratorClass, s, ",")</code>.
     */
    public EnumeratorSet(Class enumeratorClass, String s) throws EnumeratorFormatException {
        this(enumeratorClass, s, ",");
    }

    /**
     * Construct a set for values of the given {@link Enumerator}-derived type and initialize it
     * from a string.
     * <p>
     * The given string is parsed into tokens; each token is converted into a value as
     * {@link Enumerator#fromString(String, Class)} does and added to this set. Named {@link EnumeratorSet}s
     * declared in the <code>enumeratorClass</code> are also recognized and added. If the string names exactly one
     * of those {@link EnumeratorSet}s declared in the <code>enumeratorClass</code>, then the resulting set
     * inherits the name of theat {@link EnumeratorSet}.
     * 
     * @throws EnumeratorFormatException if a token cannot be identified
     */
    public EnumeratorSet(
        Class   enumeratorClass,
        String  s,
        String  delimiter
    ) throws EnumeratorFormatException {
        Set vs = new HashSet();

        Map des  = Enumerator.getInstances(enumeratorClass);
        Map dess = EnumeratorSet.getNamedEnumeratorSets(enumeratorClass);

        StringTokenizer st = new StringTokenizer(s, delimiter);
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            Enumerator value = (Enumerator) des.get(name);
            if (value != null) {
                vs.add(value);
                continue;
            }

            EnumeratorSet es = (EnumeratorSet) dess.get(name);
            if (es != null) {
                if (vs.isEmpty()) {
                    vs = es.values;
                    this.optionalName = es.optionalName;
                } else
                {
                    vs.addAll(es.values);
                }
                continue;
            }

            throw new EnumeratorFormatException(name);
        }

        this.enumeratorClass = enumeratorClass;
        this.values = vs;
    }

    /**
     * Construct a copy of the given set.
     */
    public EnumeratorSet(EnumeratorSet that) {
        this(that.enumeratorClass, that.values);
    }

    /**
     * Add the given value to the set.
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public EnumeratorSet add(Enumerator value) {
        if (value.getClass() != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot add value of type \"" + value.getClass() + "\" to set of different type \"" + this.enumeratorClass + "\"");
        Set vs = new HashSet(this.values);
        vs.add(value);
        return new EnumeratorSet(this.enumeratorClass, vs);
    }

    /**
     * Add the values of the given set to this set.
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public EnumeratorSet add(EnumeratorSet that) {
        if (that.enumeratorClass != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot add set of type \"" + that.enumeratorClass + "\" to set of different type \"" + this.enumeratorClass + "\"");
        Set vs;
        if (this.values.isEmpty()) {
            vs = that.values;
        } else
        if (that.values.isEmpty()) {
            vs = this.values;
        } else
        {
            vs = new HashSet(this.values);
            vs.addAll(that.values);
        }
        return new EnumeratorSet(this.enumeratorClass, vs);
    }

    /**
     * Remove the given value from the set.
     * 
     * @return the reduced set
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public EnumeratorSet remove(Enumerator value) {
        if (value.getClass() != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot remove value of type \"" + value.getClass() + "\" from set of different type \"" + this.enumeratorClass + "\"");
        if (!this.values.contains(value)) return this;
        Set vs = new HashSet(this.values);
        vs.remove(value);
        return new EnumeratorSet(this.enumeratorClass, vs);
    }

    /**
     * Remove the values of the given set from this set.
     * 
     * @return true if this set changed as a result of the call
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public EnumeratorSet remove(EnumeratorSet that) {
        if (that.enumeratorClass != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot remove set of type \"" + that.enumeratorClass + "\" from set of different type \"" + this.enumeratorClass + "\"");
        Set vs = new HashSet(this.values);
        vs.removeAll(that.values);
        return new EnumeratorSet(this.enumeratorClass, vs);
    }

    /**
     * Check whether this set contains the given value
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public boolean contains(Enumerator value) {
        if (value.getClass() != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot check value of type \"" + value.getClass() + "\" within set of different type \"" + this.enumeratorClass + "\"");
        return this.values.contains(value);
    }

    /**
     * Check if this set contains any of the values of the given set.
     * <p>
     * Returns <code>false</code> if either of the two sets is empty.
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public boolean containsAnyOf(EnumeratorSet that) {
        if (that.enumeratorClass != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot compare set of type \"" + that.enumeratorClass + "\" with set of different type \"" + this.enumeratorClass + "\"");
        for (Iterator it = that.values.iterator(); it.hasNext();) {
            if (this.values.contains(it.next())) return true;
        }
        return false;
    }

    /**
     * Check if this set contains all values of the given set.
     * 
     * @throws EnumeratorSetTypeException if this set was constructed for a different {@link Enumerator}-derived type
     */
    public boolean containsAllOf(EnumeratorSet that) {
        if (that.enumeratorClass != this.enumeratorClass) throw new EnumeratorSetTypeException("Cannot compare set of type \"" + that.enumeratorClass + "\" with set of different type \"" + this.enumeratorClass + "\"");
        return this.values.containsAll(that.values);
    }

    /**
     * An {@link EnumeratorSet} can optionally be assigned a name, which is used by
     * {@link #toString()}.
     * 
     * @return this object
     */
    public EnumeratorSet setName(String optionalName) {

        // Check for non-change.
        if (
            (this.optionalName == optionalName)
            || (this.optionalName != null && this.optionalName.equals(optionalName))
        ) return this;

        // Track all named EnumeratorSet instances.
        Map namedEnumeratorSets = EnumeratorSet.getNamedEnumeratorSets(this.enumeratorClass);
        if (this.optionalName != null) namedEnumeratorSets.remove(this.optionalName);
        this.optionalName = optionalName;
        if (this.optionalName != null) namedEnumeratorSets.put(this.optionalName, this);

        return this;
    }

    /**
     * Returns a map of all {@link EnumeratorSet}s instantiated for the given <code>enumeratorClass</code>.
     * 
     * @return String name => EnumeratorSet
     */
    private static Map getNamedEnumeratorSets(Class enumeratorClass) {
        Map m = (Map) EnumeratorSet.namedEnumeratorSets.get(enumeratorClass);
        if (m == null) {
            m = new HashMap();
            EnumeratorSet.namedEnumeratorSets.put(enumeratorClass, m);
        }
        return m;
    }
    private static final Map namedEnumeratorSets = new HashMap(); // Class enumeratorClass => String name => EnumeratorSet

    /**
     * Check the values' identity. Notice that the objects' names (see {@link #setName(String)} is
     * not considered.
     */
    public boolean equals(Object that) {
        return that instanceof EnumeratorSet && this.values.equals(((EnumeratorSet) that).values);
    }

    public int hashCode() {
        return this.values.hashCode();
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
     * If this {@link EnumeratorSet} has a name (see {@link #setName(String)}, then this name is
     * returned.
     * <p>
     * Otherwise, if this {@link EnumeratorSet} is empty, an empty {@link String} is returned.
     * <p>
     * Otherwise, the values' names are concatenated, separated by the given delimiter, and returned.
     */
    public String toString(String delimiter) {

        // If this EnumeratorSet has a name, then everything is very simple.
        if (this.optionalName != null) return this.optionalName;

        // Return "" for an empty set.
        Iterator it = this.values.iterator();
        if (!it.hasNext()) return "";

        // Concatenate the enumerators' names.
        StringBuffer sb = new StringBuffer(((Enumerator) it.next()).name);
        while (it.hasNext()) {
            sb.append(delimiter).append(((Enumerator) it.next()).name);
        }
        return sb.toString();
    }
}
