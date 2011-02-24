
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

public class Signature {

    private String        in;
    private int           idx;
    private StringBuilder out;

    /**
     * Converts a method type signature into the form
     * "&lt;K, V> name(K, int, List<V>) => rettype".
     */
    public static String decodeMethodTypeSignature(String s, String methodName, String declaringClassName) {
        Signature signature = new Signature(s);
        signature.parseMethodTypeSignature(methodName, declaringClassName);
        signature.eos();
        return signature.out.toString();
    }
    
    /**
     * Converts a field type signature into the form
     * "List&lt;V>".
     */
    public static String decodeFieldTypeSignature(String s) {
        Signature signature = new Signature(s);
        signature.parseFieldTypeSignature();
        signature.eos();
        return signature.out.toString();
    }

    public static String decodeClassSignature(String s, String className) {
        Signature signature = new Signature(s);
        signature.parseClassSignature(className);
        signature.eos();
        return signature.out.toString();
    }

    private void parseClassSignature(String className) {
        write(className + " ");
        if (peekReadWrite('<')) {
            parseFormalTypeParameter();
            while (!peekReadWrite('>')) {
                write(", ");
                parseFormalTypeParameter();
            }
        }
        write(" extends ");
        parseClassTypeSignature(); // superclass
        if (!peekEos()) {
            write(" implements ");
            parseClassTypeSignature(); // implemented interface
            while (!peekEos()) {
                write(", ");
                parseClassTypeSignature(); // implemented interface
            }
        }
    }

    private void parseMethodTypeSignature(String methodName, String declaringClassName) {
        if (peekReadWrite('<')) {
            parseFormalTypeParameter();
            while (!peekReadWrite('>')) {
                write(", ");
                parseFormalTypeParameter();
            }
        }
        write(
            "<init>".equals(methodName) ? declaringClassName :
            "<clinit>".equals(methodName) ? "" :
            methodName
        );
        readWrite('(');
        if (!peekReadWrite(')')) {
            parseTypeSignature();
            while (!peekReadWrite(')')) {
                write(", ");
                parseTypeSignature();
            }
        }
        if (!peekRead('V')) {
            write(" => ");
            parseTypeSignature();
        }
    }

    private void parseFormalTypeParameter() {
        while (!peekRead(':')) readWrite();
        write(" extends ");
        parseFieldTypeSignature();
        while (peekRead(':')) {
            write(" & ");
            parseFieldTypeSignature();
        }
    }

    private void parseTypeSignature() {
        if (peekRead('B')) { write("byte");    return; }
        if (peekRead('C')) { write("char");    return; }
        if (peekRead('D')) { write("double");  return; }
        if (peekRead('F')) { write("float");   return; }
        if (peekRead('I')) { write("int");     return; }
        if (peekRead('J')) { write("long");    return; }
        if (peekRead('S')) { write("short");   return; }
        if (peekRead('Z')) { write("boolean"); return; }
        parseFieldTypeSignature();
    }

    private void parseFieldTypeSignature() {
        int brackets = 0;
        while (peekRead('[')) brackets++;

        if (peek('L')) {
            parseClassTypeSignature();
        } else
        if (peekRead('T')) {
            while (!peekRead(';')) readWrite();
        }

        for (; brackets > 0; brackets--) write("[]");
    }

    private void parseClassTypeSignature() {
        read('L');
        parseSimpleClassTypeSignature();
        while (peekRead('.')) {
            write('$');
            parseSimpleClassTypeSignature();
        }
        read(';');
    }
    
    private void parseSimpleClassTypeSignature() {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            if (peekRead('/')) {
                sb.append('.');
            } else
            if (peek('<') || peek('.') || peek('>') || peek(';')) {
                break;
            } else
            {
                sb.append(read());
            }
        }
        String s = sb.toString();
        if (s.startsWith("java.lang.")) s = s.substring(10);
        write(s);

        if (peekReadWrite('<')) {
            parseTypeArgument();
            while (!peekReadWrite('>')) {
                write(", ");
                parseTypeArgument();
            }
        }
    }

    private void parseTypeArgument() {
        if (peekRead('+')) {
            write("extends ");
            parseFieldTypeSignature();
        } else
        if (peekRead('-')) {
            write("super ");
            parseFieldTypeSignature();
        } else
        if (peekReadWrite('*')) {
            ;
        } else
        {
            parseFieldTypeSignature();
        }
    }

    private Signature(String s) {
        this.in = s;
        this.out = new StringBuilder();
    }

    private boolean peek(char c) {
        return idx < in.length() && in.charAt(idx) == c;
    }

    private char read() {
        if (idx >= in.length()) throw ex("Unexpected EOS");
        return in.charAt(idx++);
    }

    private void read(char c) {
        if (idx >= in.length()) throw ex("Unexpected EOS");
        if (in.charAt(idx) != c) throw ex("'" + c + "' expected");
        idx++;
    }

    private void readWrite() {
        write(read());
    }
    
    private void readWrite(char c) {
        read(c);
        write(c);
    }
    
    private boolean peekRead(char c) {
        if (idx < in.length() && in.charAt(idx) == c) {
            idx++;
            return true;
        }
        return false;
    }
    
    private boolean peekReadWrite(char c) {
        if (peekRead(c)) {
            write(c);
            return true;
        }
        return false;
    }

    private void eos() {
        if (idx < in.length()) throw ex("Unexpected trailing characters");
    }
    
    private boolean peekEos() {
        return idx >= in.length();
    }

    private void write(char c) {
        out.append(c);
    }
    
    private void write(String s) {
        out.append(s);
    }

    private RuntimeException ex(String message) {
        return new RuntimeException("'" + in + "', at '" + in.substring(idx) + "': " + message + " (parsed '" + out.toString() + "' so far)");
    }
}
