
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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for parsing signatures and descriptors.
 */
public class SignatureParser {

    private SignatureParser() {}

    public static ClassSignature decodeClassSignature(String s) throws IOException {
        CharStream cs = new StringCharStream(s);
        ClassSignature cls = parseClassSignature(cs);
        cs.eos();
        return cls;
    }

    public static MethodTypeSignature decodeMethodTypeSignature(String s) throws IOException {
        CharStream cs = new StringCharStream(s);
        try {
            MethodTypeSignature mts = parseMethodTypeSignature(cs);
            cs.eos();
            return mts;
        } catch (IOException ioe) {
            IOException ioe2 = new IOException(cs.toString() + ": " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2;
        }
    }

    public static FieldTypeSignature decodeFieldTypeSignature(String s) throws IOException {
        CharStream cs = new StringCharStream(s);
        FieldTypeSignature fts = parseFieldTypeSignature(cs);
        cs.eos();
        return fts;
    }

    public static MethodTypeSignature decodeMethodDescriptor(String s) throws IOException {
        CharStream cs = new StringCharStream(s);
        try {
            MethodTypeSignature mts = parseMethodDescriptor(cs);
            cs.eos();
            return mts;
        } catch (IOException ioe) {
            IOException ioe2 = new IOException(cs.toString() + ": " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2;
        }
    }
    
    public static TypeSignature decodeFieldDescriptor(String s) throws EOFException, IOException {
        CharStream cs = new StringCharStream(s);
        try {
            TypeSignature ts = parseFieldDescriptor(cs);
            cs.eos();
            return ts;
        } catch (IOException ioe) {
            IOException ioe2 = new IOException(cs.toString() + ": " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2;
        }
    }

    public static class MethodTypeSignature {
    
        public final List<FormalTypeParameter> formalTypeParameters = new ArrayList<SignatureParser.FormalTypeParameter>();
        public final List<TypeSignature>       parameterTypes = new ArrayList<SignatureParser.TypeSignature>();
        public TypeSignature                   returnType;
        public final List<ThrowsSignature>     thrownTypes = new ArrayList<SignatureParser.ThrowsSignature>();
    
        public String toString(String declaringClassName, String methodName) {
            StringBuilder sb = new StringBuilder();
    
            // Formal type parameters.
            if (!formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = formalTypeParameters.iterator();
                sb.append('<' + it.next().toString());
                while (it.hasNext()) sb.append(", " + it.next().toString());
                sb.append('>');
            }
    
            // Name.
            if ("<init>".equals(methodName) && returnType == SignatureParser.VOID) {
                sb.append(declaringClassName);
            } else
            {
                sb.append(declaringClassName).append('.').append(methodName);
            }
            sb.append('(');
            Iterator<TypeSignature> it = parameterTypes.iterator();
            if (it.hasNext()) {
                for (;;) {
                    sb.append(it.next().toString());
                    if (!it.hasNext()) break;
                    sb.append(", ");
                }
            }
            sb.append(')');
            if (returnType != VOID) sb.append(" => ").append(returnType.toString());
            return sb.toString();
        }
    
        public String toString() {
            return "*** Cannot easily be converted into String ***";
        }
    }

    public static class ClassSignature {
        public final List<FormalTypeParameter> formalTypeParameters = new ArrayList<FormalTypeParameter>();
        public ClassTypeSignature              superclassSignature;
        public final List<ClassTypeSignature>  superinterfaceSignatures = new ArrayList<ClassTypeSignature>();
    
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = formalTypeParameters.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
                sb.append('>');
            }
            sb.append(" extends ").append(superclassSignature.toString());
            if (!superinterfaceSignatures.isEmpty()) {
                Iterator<ClassTypeSignature> it = superinterfaceSignatures.iterator();
                sb.append(" implements ").append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
            }
            return sb.toString();
        }
    }

    public static class ClassTypeSignature implements ThrowsSignature, FieldTypeSignature {
        public String                               packageSpecifier = "";
        public String                               simpleClassName;
        public final List<TypeArgument>             typeArguments = new ArrayList<TypeArgument>();
        public final List<SimpleClassTypeSignature> suffixes = new ArrayList<SignatureParser.SimpleClassTypeSignature>();
    
        public String toString() {
            StringBuilder sb = new StringBuilder(packageSpecifier.replace('/', '.')).append(simpleClassName);
            if (!typeArguments.isEmpty()) {
                Iterator<TypeArgument> it = typeArguments.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString());
                }
                sb.append('>');
            }
            for (SimpleClassTypeSignature suffix : suffixes) {
                sb.append(suffix.toString());
            }
            return sb.toString();
        }
    }

    public static class SimpleClassTypeSignature {
        public String                   simpleClassName;
        public final List<TypeArgument> typeArguments = new ArrayList<TypeArgument>();
    
        public String toString() {
            StringBuilder sb = new StringBuilder(simpleClassName);
            if (!typeArguments.isEmpty()) {
                Iterator<TypeArgument> it = typeArguments.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString());
                }
                sb.append('>');
            }
            return sb.toString();
        }
    }

    public static class ArrayTypeSignature implements FieldTypeSignature {
        public TypeSignature typeSignature;
        public String toString() { return typeSignature.toString() + "[]"; }
    }

    public static class TypeVariableSignature implements ThrowsSignature, FieldTypeSignature {
        public String identifier;
        public String toString() { return identifier; }
    }

    public interface TypeSignature {
        public abstract String toString();
    }

    public interface ThrowsSignature {
    }

    public static class FormalTypeParameter {
    
        public String                         identifier;
        public FieldTypeSignature             optionalClassBound;
        public final List<FieldTypeSignature> interfaceBounds = new ArrayList<SignatureParser.FieldTypeSignature>();
    
        public String toString() {
            StringBuilder sb = new StringBuilder(identifier);
            if (optionalClassBound == null) {
                if (interfaceBounds.isEmpty()) return identifier;
                Iterator<FieldTypeSignature> it = interfaceBounds.iterator();
                sb.append(" extends ").append(it.next().toString());
                while (it.hasNext()) sb.append(" & ").append(it.next().toString());
            } else {
                sb.append(" extends ").append(optionalClassBound.toString());
                for (FieldTypeSignature ib : interfaceBounds) sb.append(" & ").append(ib.toString());
            }
            return sb.toString();
        }
    }

    public static class PrimitiveTypeSignature implements TypeSignature {
        public final String typeName;
        private PrimitiveTypeSignature(String typeName) {
            this.typeName = typeName;
        }
        public String toString() { return typeName; };
    }

    public static class TypeArgument {
        enum Mode { EXTENDS, SUPER, ANY, NONE };
        public Mode               mode;
        public FieldTypeSignature fieldTypeSignature;
    
        public String toString() {
            switch (mode) {
            case EXTENDS:
                return "extends " + fieldTypeSignature.toString();
            case SUPER:
                return "super " + fieldTypeSignature.toString();
            case ANY:
                return "*";
            case NONE:
                return fieldTypeSignature.toString();
            }
            throw new IllegalStateException();
        }
    }

    public interface FieldTypeSignature extends TypeSignature {
        String toString();
    }

    public static final PrimitiveTypeSignature BYTE    = new PrimitiveTypeSignature("byte");

    public static final PrimitiveTypeSignature CHAR    = new PrimitiveTypeSignature("char");

    public static final PrimitiveTypeSignature DOUBLE  = new PrimitiveTypeSignature("double");

    public static final PrimitiveTypeSignature FLOAT   = new PrimitiveTypeSignature("float");

    public static final PrimitiveTypeSignature INT     = new PrimitiveTypeSignature("int");

    public static final PrimitiveTypeSignature LONG    = new PrimitiveTypeSignature("long");

    public static final PrimitiveTypeSignature SHORT   = new PrimitiveTypeSignature("short");

    public static final PrimitiveTypeSignature BOOLEAN = new PrimitiveTypeSignature("boolean");

    public static final TypeSignature VOID = new TypeSignature() { public String toString() { return "void"; }};

    private static TypeSignature parseFieldDescriptor(CharStream cs) throws EOFException, IOException {
        return parseTypeSignature(cs);
    }

    private static MethodTypeSignature parseMethodDescriptor(CharStream cs) throws EOFException, IOException {
        return parseMethodTypeSignature(cs);
    }

    private static ClassSignature parseClassSignature(CharStream cs) throws EOFException, IOException {
        ClassSignature cls = new ClassSignature();
        if (cs.peekRead('<')) {
            while (!cs.peekRead('>')) {
                cls.formalTypeParameters.add(parseFormalTypeParameter(cs));
            }
        }
        cls.superclassSignature = parseClassTypeSignature(cs);
        while (!cs.peekEos()) cls.superinterfaceSignatures.add(parseClassTypeSignature(cs));
        return cls;
    }

    private static MethodTypeSignature parseMethodTypeSignature(CharStream cs) throws EOFException, IOException {
        MethodTypeSignature mts = new MethodTypeSignature();
        
        if (cs.peekRead('<')) {
            while (!cs.peekRead('>')) {
                mts.formalTypeParameters.add(parseFormalTypeParameter(cs));
            }
        }

        cs.read('(');
        while (!cs.peekRead(')')) {
            mts.parameterTypes.add(parseTypeSignature(cs));
        }
        mts.returnType = parseReturnType(cs);
        while (!cs.peekEos()) mts.thrownTypes.add(parseThrowsSignature(cs));

        return mts;
    }

    private static TypeSignature parseReturnType(CharStream cs) throws EOFException, IOException {
        if (cs.peekRead('V')) return VOID;
        return parseTypeSignature(cs);
    }
    private static ThrowsSignature parseThrowsSignature(CharStream cs) throws EOFException, IOException {
        cs.read('^');
        return cs.peek('T') ? parseTypeVariableSignature(cs) : parseClassTypeSignature(cs);
    }

    private static ClassTypeSignature parseClassTypeSignature(CharStream cs) throws EOFException, IOException {
        ClassTypeSignature cts = new ClassTypeSignature();
        cs.read('L');

        PART1:
        for (;;) {
            String s = parseIdentifier(cs);
            switch (cs.peek("<./;")) {
            case 0: // '<'
                cs.read('<');
                cts.simpleClassName = s;
                while (!cs.peekRead('>')) {
                    cts.typeArguments.add(parseTypeArgument(cs));
                }
                break PART1;
            case 1: // '.'
                cts.simpleClassName = s;
                break PART1;
            case 2: // '/'
                cts.packageSpecifier += s + '/';
                cs.read();
                break;
            case 3: // ';'
                cts.simpleClassName = s;
                break PART1;
            default:
                throw new IOException("Unexpected character '" + cs.peek() + "'");
            }
        }

        while (cs.peekRead('.')) {
            cts.suffixes.add(parseSimpleClassTypeSignature(cs));
        }
        cs.read(';');

        return cts;
    }

    private static String parseIdentifier(CharStream cs) throws IOException {
        char c = cs.read();
        if (!Character.isJavaIdentifierStart(c)) throw new IOException("Identifier expected instead of '" + c + "'");
        StringBuilder sb = new StringBuilder().append(c);
        for (;;) {
            if (Character.isJavaIdentifierPart(cs.peek())) {
                sb.append(cs.read());
            } else {
                return sb.toString();
            }
        }
    }

    private static TypeVariableSignature parseTypeVariableSignature(CharStream cs) throws EOFException, IOException {
        TypeVariableSignature tvs = new TypeVariableSignature();
        cs.read('T');
        tvs.identifier = parseIdentifier(cs);
        cs.read(';');
        return tvs;
    }

    private static FormalTypeParameter parseFormalTypeParameter(CharStream cs) throws IOException {
        FormalTypeParameter ftp = new FormalTypeParameter();
        ftp.identifier = parseIdentifier(cs);
        cs.read(':');
        if (!cs.peek(':')) {
            ftp.optionalClassBound = parseFieldTypeSignature(cs);
        }
        while (cs.peekRead(':')) {
            ftp.interfaceBounds.add(parseFieldTypeSignature(cs));
        }
        return ftp;
    }

    private static TypeSignature parseTypeSignature(CharStream cs) throws EOFException, IOException {
        int idx = cs.peek("BCDFIJSZ");
        if (idx != -1) {
            cs.read();
            return PRIMITIVE_TYPES[idx];
        }
        return parseFieldTypeSignature(cs);
    }
    private static final PrimitiveTypeSignature[] PRIMITIVE_TYPES = {
        BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT, BOOLEAN
    };
    
    private static FieldTypeSignature parseFieldTypeSignature(CharStream cs) throws EOFException, IOException {
        switch (cs.peek("L[T")) {
        case 0:
            return parseClassTypeSignature(cs);
        case 1:
            return parseArrayTypeSignature(cs);
        case 2:
            return parseTypeVariableSignature(cs);
        default:
            throw new IOException("Class type signature, array type signature or type variable signature expected");
        }
    }

    private static FieldTypeSignature parseArrayTypeSignature(CharStream cs) throws EOFException, IOException {
        ArrayTypeSignature ats = new ArrayTypeSignature();
        cs.read('[');
        ats.typeSignature = parseTypeSignature(cs);
        return ats;
    }

    private static SimpleClassTypeSignature parseSimpleClassTypeSignature(CharStream cs) throws IOException {
        SimpleClassTypeSignature scts = new SimpleClassTypeSignature();

        scts.simpleClassName = parseIdentifier(cs);
        if (cs.peekRead('<')) {
            while (!cs.peekRead('>')) {
                scts.typeArguments.add(parseTypeArgument(cs));
            }
        }
        return scts;
    }

    private static TypeArgument parseTypeArgument(CharStream cs) throws EOFException, IOException {
        TypeArgument ta = new TypeArgument();
        if (cs.peekRead('+')) {
            ta.mode = TypeArgument.Mode.EXTENDS;
            ta.fieldTypeSignature = parseFieldTypeSignature(cs);
        } else
        if (cs.peekRead('-')) {
            ta.mode = TypeArgument.Mode.SUPER;
            ta.fieldTypeSignature = parseFieldTypeSignature(cs);
        } else
        if (cs.peekRead('*')) {
            ta.mode = TypeArgument.Mode.ANY;
        } else
        {
            ta.mode = TypeArgument.Mode.NONE;
            ta.fieldTypeSignature = parseFieldTypeSignature(cs);
        }
        return ta;
    }
}
