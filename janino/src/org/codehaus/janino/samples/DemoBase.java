
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
                type == Boolean.TYPE   ? Boolean.class   :
                type == Character.TYPE ? Character.class :
                type == Byte.TYPE      ? Byte.class      :
                type == Short.TYPE     ? Short.class     :
                type == Integer.TYPE   ? Integer.class   :
                type == Long.TYPE      ? Long.class      :
                type == Float.TYPE     ? Float.class     :
                type == Double.TYPE    ? Double.class    : type
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
            if (s.equals("void"   )) return Void.TYPE;
            if (s.equals("boolean")) return Boolean.TYPE;
            if (s.equals("char"   )) return Character.TYPE;
            if (s.equals("byte"   )) return Byte.TYPE;
            if (s.equals("short"  )) return Short.TYPE;
            if (s.equals("int"    )) return Integer.TYPE;
            if (s.equals("long"   )) return Long.TYPE;
            if (s.equals("float"  )) return Float.TYPE;
            if (s.equals("double" )) return Double.TYPE;
        }

        // Automagically convert primitive type names.
        s = (
            s.equals("void"   ) ? "V" :
            s.equals("boolean") ? "Z" :
            s.equals("char"   ) ? "C" :
            s.equals("byte"   ) ? "B" :
            s.equals("short"  ) ? "S" :
            s.equals("int"    ) ? "I" :
            s.equals("long"   ) ? "J" :
            s.equals("float"  ) ? "F" :
            s.equals("double" ) ? "D" :
            s
        );
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
