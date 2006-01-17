
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

package org.codehaus.janino.samples;

import java.util.*;
import java.lang.reflect.*;

/**
 * Common base class for the "...Demo" classes that demostrate Janino.
 */

public class DemoBase {
    protected DemoBase() {}

    public static Object createObject(
        Class type,
        String value
    ) throws
    NoSuchMethodException,
    InstantiationException,
    InvocationTargetException,
    IllegalAccessException {

        // Wrap primitive parameters.
        if (type.isPrimitive()) {
            type = (
                type == boolean.class ? Boolean.class   :
                type == char.class    ? Character.class :
                type == byte.class    ? Byte.class      :
                type == short.class   ? Short.class     :
                type == int.class     ? Integer.class   :
                type == long.class    ? Long.class      :
                type == float.class   ? Float.class     :
                type == double.class  ? Double.class    : void.class
            );
        }

        // Construct object, assuming it has a default constructor or a
        // constructor with one single "String" argument.
        if (value.equals("")) {
            return type.getConstructor(new Class[0]).newInstance(new Object[0]);
        } else {
            return type.getConstructor(new Class[] { String.class }).newInstance(new Object[] { value });
        }
    }

    public static String[] explode(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        List l = new ArrayList();
        while (st.hasMoreTokens()) l.add(st.nextToken());
        return (String[]) l.toArray(new String[l.size()]);
    }

    public static Class stringToType(String s) {
        int brackets = 0;
        while (s.endsWith("[]")) {
            ++brackets;
            s = s.substring(0, s.length() - 2);
        }

        if (brackets == 0) {
            // "Class.forName("C")" does not work.
            if (s.equals("void"   )) return void.class;
            if (s.equals("boolean")) return boolean.class;
            if (s.equals("char"   )) return char.class;
            if (s.equals("byte"   )) return byte.class;
            if (s.equals("short"  )) return short.class;
            if (s.equals("int"    )) return int.class;
            if (s.equals("long"   )) return long.class;
            if (s.equals("float"  )) return float.class;
            if (s.equals("double" )) return double.class;
        }

        // Automagically convert primitive type names.
        if (s.equals("void"   )) { s = "V"; } else
        if (s.equals("boolean")) { s = "Z"; } else
        if (s.equals("char"   )) { s = "C"; } else
        if (s.equals("byte"   )) { s = "B"; } else
        if (s.equals("short"  )) { s = "S"; } else
        if (s.equals("int"    )) { s = "I"; } else
        if (s.equals("long"   )) { s = "J"; } else
        if (s.equals("float"  )) { s = "F"; } else
        if (s.equals("double" )) { s = "D"; }

        while (--brackets >= 0) s = '[' + s;
        try {
            return Class.forName(s);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            System.exit(1);
            throw new RuntimeException(); // Never reached.
        }
    }

    public static Class[] stringToTypes(String s) {
        StringTokenizer st = new StringTokenizer(s, ",");
        List l = new ArrayList();
        while (st.hasMoreTokens()) l.add(DemoBase.stringToType(st.nextToken()));
        Class[] res = new Class[l.size()];
        l.toArray(res);
        return res;
    }

    public static String toString(Object o) {
        if (o == null) return "(null)";

        // Pretty-print array.
        Class clazz = o.getClass();
        if (clazz.isArray()) {
            StringBuffer sb = new StringBuffer(clazz.getComponentType().toString()).append("[] { ");
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
