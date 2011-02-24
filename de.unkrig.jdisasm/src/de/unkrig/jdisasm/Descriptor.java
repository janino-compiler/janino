
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001-2011, Arno Unkrig
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

package de.unkrig.jdisasm;

public class Descriptor {

    private Descriptor() {}

    /**
     * Converts a field descriptor into a java type.
     */
    public static String decodeFieldDescriptor(String fieldDescriptor) {
        return parseFieldDescriptor(fieldDescriptor, new int[1]);
    }

    /**
     * Extracts the return type from a function descriptor.
     */
    public static String returnType(String functionDescriptor) {
        return parseFieldDescriptor(functionDescriptor, new int[] { functionDescriptor.indexOf(')') + 1 });
    }

    /**
     * Extracts the parameters from a fuction descriptor.
     *
     * @return E.g. "(Object, int)"
     */
    public static String parameters(String functionDescriptor) {
        StringBuilder sb = new StringBuilder("(");
        if (functionDescriptor.charAt(1) != ')') {
            int[] idx = new int[] { 1 };
            sb.append(parseFieldDescriptor(functionDescriptor, idx));
            while (functionDescriptor.charAt(idx[0]) != ')') {
                sb.append(", ").append(parseFieldDescriptor(functionDescriptor, idx));
            }
        }
        return sb.append(')').toString();
    }

    private static String parseFieldDescriptor(String d, int[] idx) {
        int brackets = 0;
        while (d.charAt(idx[0]) == '[') {
            ++brackets;
            idx[0]++;
        }
        {
            int i = "BCDFIJSZVLT".indexOf(d.charAt(idx[0]));
            if (i == -1)  {
                d = "[Invalid type '" + d.charAt(idx[0]) + "']";
                idx[0]++;
            } else
            if (i == 9) {
                int j = d.indexOf(';', idx[0] + 1);
                int k = d.indexOf('<', idx[0] + 1);
                if (k == -1 || k > j) {
                    d = beautifyTypeName(d.substring(idx[0] + 1, j).replace('/', '.'));
                    idx[0] = j + 1;
                } else {
                    StringBuilder sb = new StringBuilder(beautifyTypeName(d.substring(idx[0] + 1, k).replace('/', '.'))).append('<');
                    idx[0] = k + 1;
                    sb.append(parseFieldDescriptor(d, idx));
                    while (d.charAt(idx[0]) != '>') {
                        sb.append(", ").append(parseFieldDescriptor(d, idx));
                    }
                    idx[0]++;
                    d = sb.append('>').toString();
                }
            } else
            if (i == 10) {
                int j = d.indexOf(';', idx[0] + 1);
                d = d.substring(idx[0] + 1, j);
                idx[0] = j + 1;
            } else {
                d = PRIMITIVES[i];
                idx[0]++;
            }
        }
        for (; brackets > 0; --brackets) d += "[]";
        return d;
    }
    private static final String[] PRIMITIVES = { "byte", "char", "double", "float", "int", "long", "short", "boolean", "void" };

    private static String beautifyTypeName(String name) {
//        if (compilationUnitPackageName.length() > 0 && name.startsWith(compilationUnitPackageName)) {
//            name = name.substring(compilationUnitPackageName.length());
//        } else
        if (name.startsWith("java.lang.")) {
            name = name.substring(10);
        }
        return name;
    }
}
