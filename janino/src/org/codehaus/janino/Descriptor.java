
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

package org.codehaus.janino;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class that defines useful methods for handling "field descriptors"
 * (JVMS 4.3.2) and "method descriptors" (JVMS 4.3.3).<p>
 * Typical descriptors are:
 * <ul>
 *   <li><code>I</code> Integer
 *   <li><code>[I</code> Array of integer
 *   <li><code>Lpkg1/pkg2/Cls;</code> Class
 *   <li><code>Lpkg1/pkg2/Outer$Inner;</code> Member class
 * </ul>
 */
public final class Descriptor {
    private Descriptor() {}

    public static boolean isReference(String d) {
        return d.length() > 1;
    }
    public static boolean isClassOrInterfaceReference(String d) {
        return d.charAt(0) == 'L';
    }
    public static boolean isArrayReference(String d) {
        return d.charAt(0) == '[';
    }
    public static String getComponentDescriptor(String d) {
        if (d.charAt(0) != '[') {
            throw new JaninoRuntimeException(
                "Cannot determine component descriptor from non-array descriptor \""
                + d
                + "\""
            );
        }
        return d.substring(1);
    }
    public static short size(String d) {
        if (d.equals(Descriptor.VOID_)) return 0;
        if (Descriptor.hasSize1(d)) return 1;
        if (Descriptor.hasSize2(d)) return 2;
        throw new JaninoRuntimeException("No size defined for type \"" + Descriptor.toString(d) + "\"");
    }
    public static boolean hasSize1(String d) {
        if (d.length() == 1) return "BCFISZ".indexOf(d) != -1;
        return Descriptor.isReference(d);
    }
    public static boolean hasSize2(String d) {
        return d.equals(Descriptor.LONG_) || d.equals(Descriptor.DOUBLE_);
    }

    // Pretty-print.
    public static String toString(String d) {
        int idx = 0;
        StringBuffer sb = new StringBuffer();
        if (d.charAt(0) == '(') {
            ++idx;
            sb.append("(");
            while (idx < d.length() && d.charAt(idx) != ')') {
                if (idx != 1) sb.append(", ");
                idx = Descriptor.toString(d, idx, sb);
            }
            if (idx >= d.length()) throw new JaninoRuntimeException("Invalid descriptor \"" + d + "\"");
            sb.append(") => ");
            ++idx;
        }
        Descriptor.toString(d, idx, sb);
        return sb.toString();
    }
    private static int toString(String d, int idx, StringBuffer sb) {
        int dimensions = 0;
        while (idx < d.length() && d.charAt(idx) == '[') {
            ++dimensions;
            ++idx;
        }
        if (idx >= d.length()) throw new JaninoRuntimeException("Invalid descriptor \"" + d + "\"");
        switch (d.charAt(idx)) {
        case 'L':
            {
                int idx2 = d.indexOf(';', idx);
                if (idx2 == -1) throw new JaninoRuntimeException("Invalid descriptor \"" + d + "\"");
                sb.append(d.substring(idx + 1, idx2).replace('/', '.'));
                idx = idx2;
            }
            break;
        case 'V':
            sb.append("void");
            break;
        case 'B':
            sb.append("byte");
            break;
        case 'C':
            sb.append("char");
            break;
        case 'D':
            sb.append("double");
            break;
        case 'F':
            sb.append("float");
            break;
        case 'I':
            sb.append("int");
            break;
        case 'J':
            sb.append("long");
            break;
        case 'S':
            sb.append("short");
            break;
        case 'Z':
            sb.append("boolean");
            break;
        default:
            throw new JaninoRuntimeException("Invalid descriptor \"" + d + "\"");
        }
        for (; dimensions > 0; --dimensions) sb.append("[]");
        return idx + 1;
    }

    /**
     * Convert a class name as defined by "Class.getName()" into a
     * descriptor.
     */
    public static String fromClassName(String className) {
        String res = (String) Descriptor.classNameToDescriptor.get(className);
        if (res != null) { return res; }
        if (className.startsWith("[")) return className.replace('.', '/');
        return 'L' + className.replace('.', '/') + ';';
    }

    /**
     * Convert a class name in the "internal form" as described in JVMS 4.2 into a descriptor.
     * <p>
     * Also implement the encoding of array types as described in JVMS 4.4.1.
     */
    public static String fromInternalForm(String internalForm) {
        if (internalForm.charAt(0) == '[') return internalForm;
        return 'L' + internalForm + ';';
    }

    /**
     * Convert a field descriptor into a class name as defined by {@link
     * Class#getName()}.
     */
    public static String toClassName(String d) {
        String res = (String) Descriptor.descriptorToClassName.get(d);
        if (res != null) { return res; }

        char firstChar = d.charAt(0);
        if (firstChar == 'L' && d.endsWith(";")) {
            // Class or interface -- convert "Ljava/lang/String;" to "java.lang.String".
            return d.substring(1, d.length() - 1).replace('/', '.');
        }
        if (firstChar == '[') {
            // Array type -- convert "[Ljava/lang/String;" to "[Ljava.lang.String;".
            return d.replace('/', '.');
        }
        throw new JaninoRuntimeException("(Invalid field descriptor \"" + d + "\")");
    }

    /**
     * Convert a descriptor into the "internal form" as defined by JVMS 4.2.
     */
    public static String toInternalForm(String d) {
        if (d.charAt(0) != 'L') {
            throw new JaninoRuntimeException(
                "Attempt to convert non-class descriptor \""
                + d
                + "\" into internal form"
            );
        }
        return d.substring(1, d.length() - 1);
    }

    public static boolean isPrimitive(String d) {
        return d.length() == 1 && "VBCDFIJSZ".indexOf(d.charAt(0)) != -1;
    }

    public static boolean isPrimitiveNumeric(String d) {
        return d.length() == 1 && "BDFIJSC".indexOf(d.charAt(0)) != -1;
    }

    /**
     * Returns the package name of a class or interface reference descriptor,
     * or <code>null</code> if the class or interface is declared in the
     * default package.
     */
    public static String getPackageName(String d) {
        if (d.charAt(0) != 'L') {
            throw new JaninoRuntimeException("Attempt to get package name of non-class descriptor \"" + d + "\"");
        }
        int idx = d.lastIndexOf('/');
        return idx == -1 ? null : d.substring(1, idx).replace('/', '.');
    }

    /**
     * Check whether two reference types are declared in the same package.
     */
    public static boolean areInSamePackage(String d1, String d2) {
        String packageName1 = Descriptor.getPackageName(d1);
        String packageName2 = Descriptor.getPackageName(d2);
        return packageName1 == null ? packageName2 == null : packageName1.equals(packageName2);
    }

    public static final String VOID_    = "V";
    public static final String BYTE_    = "B";
    public static final String CHAR_    = "C";
    public static final String DOUBLE_  = "D";
    public static final String FLOAT_   = "F";
    public static final String INT_     = "I";
    public static final String LONG_    = "J";
    public static final String SHORT_   = "S";
    public static final String BOOLEAN_ = "Z";
    public static final String OBJECT            = "Ljava/lang/Object;";
    public static final String STRING            = "Ljava/lang/String;";
    public static final String STRING_BUFFER     = "Ljava/lang/StringBuffer;";
    public static final String STRING_BUILDER    = "Ljava/lang/StringBuilder;"; // Since 1.5!
    public static final String CLASS             = "Ljava/lang/Class;";
    public static final String THROWABLE         = "Ljava/lang/Throwable;";
    public static final String RUNTIME_EXCEPTION = "Ljava/lang/RuntimeException;";
    public static final String ERROR             = "Ljava/lang/Error;";
    public static final String CLONEABLE         = "Ljava/lang/Cloneable;";
    public static final String SERIALIZABLE      = "Ljava/io/Serializable;";
    public static final String BOOLEAN   = "Ljava/lang/Boolean;";
    public static final String BYTE      = "Ljava/lang/Byte;";
    public static final String CHARACTER = "Ljava/lang/Character;";
    public static final String SHORT     = "Ljava/lang/Short;";
    public static final String INTEGER   = "Ljava/lang/Integer;";
    public static final String LONG      = "Ljava/lang/Long;";
    public static final String FLOAT     = "Ljava/lang/Float;";
    public static final String DOUBLE    = "Ljava/lang/Double;";

    private static final Map classNameToDescriptor = new HashMap();
    private static final Map descriptorToClassName = new HashMap();
    static {
        descriptorToClassName.put(Descriptor.VOID_,             "void");
        descriptorToClassName.put(Descriptor.BYTE_,             "byte");
        descriptorToClassName.put(Descriptor.CHAR_,             "char");
        descriptorToClassName.put(Descriptor.DOUBLE_,           "double");
        descriptorToClassName.put(Descriptor.FLOAT_,            "float");
        descriptorToClassName.put(Descriptor.INT_,              "int");
        descriptorToClassName.put(Descriptor.LONG_,             "long");
        descriptorToClassName.put(Descriptor.SHORT_,            "short");
        descriptorToClassName.put(Descriptor.BOOLEAN_,          "boolean");
        descriptorToClassName.put(Descriptor.OBJECT,            "java.lang.Object");
        descriptorToClassName.put(Descriptor.STRING,            "java.lang.String");
        descriptorToClassName.put(Descriptor.STRING_BUFFER,     "java.lang.StringBuffer");
        descriptorToClassName.put(Descriptor.STRING_BUILDER,    "java.lang.StringBuilder");
        descriptorToClassName.put(Descriptor.CLASS,             "java.lang.Class");
        descriptorToClassName.put(Descriptor.THROWABLE,         "java.lang.Throwable");
        descriptorToClassName.put(Descriptor.RUNTIME_EXCEPTION, "java.lang.RuntimeException");
        descriptorToClassName.put(Descriptor.ERROR,             "java.lang.Error");
        descriptorToClassName.put(Descriptor.CLONEABLE,         "java.lang.Cloneable");
        descriptorToClassName.put(Descriptor.SERIALIZABLE,      "java.io.Serializable");
        descriptorToClassName.put(Descriptor.BOOLEAN,           "java.lang.Boolean");
        descriptorToClassName.put(Descriptor.BYTE,              "java.lang.Byte");
        descriptorToClassName.put(Descriptor.CHARACTER,         "java.lang.Character");
        descriptorToClassName.put(Descriptor.SHORT,             "java.lang.Short");
        descriptorToClassName.put(Descriptor.INTEGER,           "java.lang.Integer");
        descriptorToClassName.put(Descriptor.LONG,              "java.lang.Long");
        descriptorToClassName.put(Descriptor.FLOAT,             "java.lang.Float");
        descriptorToClassName.put(Descriptor.DOUBLE,            "java.lang.Double");

        for (Iterator it = descriptorToClassName.entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            classNameToDescriptor.put(e.getValue(), e.getKey());
        }
    }
}
