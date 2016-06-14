
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.samples;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/** Common base class for the "...Demo" classes that demostrate Janino. */
@SuppressWarnings({ "rawtypes", "unchecked" }) public
class DemoBase {

    protected DemoBase() {}

    /**
     * Creates an instance of the given {@code type}, by calling the single-string-parameter constructor, or, if
     * the {@code value} equals "", the zero-parameter constructor.
     */
    public static Object
    createObject(Class type, String value)
    throws NoSuchMethodException, InstantiationException, InvocationTargetException, IllegalAccessException {

        // Wrap primitive parameters.
        if (type.isPrimitive()) {
            type = (
                type == boolean.class ? Boolean.class
                : type == char.class ? Character.class
                : type == byte.class ? Byte.class
                : type == short.class ? Short.class
                : type == int.class ? Integer.class
                : type == long.class ? Long.class
                : type == float.class ? Float.class
                : type == double.class ? Double.class
                : void.class
            );
        }

        // Construct object, assuming it has a default constructor or a
        // constructor with one single "String" argument.
        if ("".equals(value)) {
            return type.getConstructor(new Class[0]).newInstance(new Object[0]);
        } else {
            return type.getConstructor(new Class[] { String.class }).newInstance(new Object[] { value });
        }
    }

    /** @return {@code s}, split at the commas */
    public static String[]
    explode(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        List<String>    l  = new ArrayList();
        while (st.hasMoreTokens()) l.add(st.nextToken());
        return (String[]) l.toArray(new String[l.size()]);
    }

    /** @return {@code s}, converted to a Java type */
    public static Class
    stringToType(String s) {

        int brackets = 0;
        while (s.endsWith("[]")) {
            ++brackets;
            s = s.substring(0, s.length() - 2);
        }

        if (brackets == 0) {
            // CHECKSTYLE WhitespaceBefore:OFF
            if ("void".equals(s))    return void.class;
            if ("boolean".equals(s)) return boolean.class;
            if ("char".equals(s))    return char.class;
            if ("byte".equals(s))    return byte.class;
            if ("short".equals(s))   return short.class;
            if ("int".equals(s))     return int.class;
            if ("long".equals(s))    return long.class;
            if ("float".equals(s))   return float.class;
            if ("double".equals(s)) return double.class;
            // CHECKSTYLE WhitespaceBefore:ON
        }

        // Automagically convert primitive type names.
        // CHECKSTYLE WhitespaceBefore:OFF
        if ("void".equals(s))    { s = "V"; } else
        if ("boolean".equals(s)) { s = "Z"; } else
        if ("char".equals(s))    { s = "C"; } else
        if ("byte".equals(s))    { s = "B"; } else
        if ("short".equals(s))   { s = "S"; } else
        if ("int".equals(s))     { s = "I"; } else
        if ("long".equals(s))    { s = "J"; } else
        if ("float".equals(s))   { s = "F"; } else
        if ("double".equals(s))  { s = "D"; }
        // CHECKSTYLE WhitespaceBefore:ON

        while (--brackets >= 0) s = '[' + s;
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            System.exit(1);
            throw new RuntimeException(); // Never reached. // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    /** Converts the given comma-separated list of class names to an array of {@link Class}es. */
    public static Class[]
    stringToTypes(String s) {

        StringTokenizer st = new StringTokenizer(s, ",");
        List<Class>     l  = new ArrayList();
        while (st.hasMoreTokens()) l.add(DemoBase.stringToType(st.nextToken()));
        Class[] res = new Class[l.size()];
        l.toArray(res);
        return res;
    }

    /** A version of {@link Object#toString()} that pretty-prints arrays. */
    public static String
    toString(Object o) {

        if (o == null) return "(null)";

        // Pretty-print array.
        Class clazz = o.getClass();
        if (clazz.isArray()) {
            StringBuilder sb = new StringBuilder(clazz.getComponentType().toString()).append("[] { ");
            for (int i = 0; i < Array.getLength(o); ++i) {
                if (i > 0) sb.append(", ");
                sb.append(DemoBase.toString(Array.get(o, i)));
            }
            sb.append(" }");
            return sb.toString();
        }

        // Apply default "toString()" method.
        return o.toString();
    }
}
