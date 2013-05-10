
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001, Arno Unkrig
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.io.charstream.StringCharStream;
import de.unkrig.io.charstream.UnexpectedCharacterException;

/**
 * Helper class for parsing signatures and descriptors. See
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/ClassFileFormat-Java5.pdf">Java 5 class file format</a>,
 * section 4.4.4, "Signatures".
 */
public final
class SignatureParser {

    private SignatureParser() {}

    public static ClassSignature
    decodeClassSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            ClassSignature   cls;
            cls = parseClassSignature(scs);
            scs.eoi();
            return cls;
        } catch (SignatureException e) {
            throw new SignatureException("Class signature '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Class signature '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Class signature '" + s + "': " + e.getMessage(), e);
        }
    }

    public static MethodTypeSignature
    decodeMethodTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            MethodTypeSignature mts = parseMethodTypeSignature(scs);
            scs.eoi();
            return mts;
        } catch (SignatureException e) {
            throw new SignatureException("Method type signature '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Method type signature '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Method type signature '" + s + "': " + e.getMessage(), e);
        }
    }

    public static TypeSignature
    decodeTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            TypeSignature    ts  = parseTypeSignature(scs);
            scs.eoi();
            return ts;
        } catch (SignatureException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        }
    }

    public static FieldTypeSignature
    decodeFieldTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream   scs = new StringCharStream(s);
            FieldTypeSignature fts = parseFieldTypeSignature(scs);
            scs.eoi();
            return fts;
        } catch (SignatureException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Field type signature '" + s + "': " + e.getMessage(), e);
        }
    }

    public static MethodTypeSignature
    decodeMethodDescriptor(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            MethodTypeSignature mts = parseMethodDescriptor(scs);
            scs.eoi();
            return mts;
        } catch (SignatureException e) {
            throw new SignatureException("Method descriptor '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Method descriptor '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Method descriptor '" + s + "': " + e.getMessage(), e);
        }
    }

    public static TypeSignature
    decodeFieldDescriptor(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            TypeSignature    ts  = parseFieldDescriptor(scs);
            scs.eoi();
            return ts;
        } catch (SignatureException e) {
            throw new SignatureException("Field descriptor '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Field descriptor '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Field descriptor '" + s + "': " + e.getMessage(), e);
        }
    }

    public static TypeSignature
    decodeReturnType(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            TypeSignature    ts  = parseReturnType(scs);
            scs.eoi();
            return ts;
        } catch (SignatureException e) {
            throw new SignatureException("Return type '" + s + "': " + e.getMessage(), e);
        } catch (EOFException e) {
            throw new SignatureException("Return type '" + s + "': " + e.getMessage(), e);
        } catch (UnexpectedCharacterException e) {
            throw new SignatureException("Return type '" + s + "': " + e.getMessage(), e);
        }
    }

    /** Representation of the "MethodTypeSignature" clause. */
    public static
    class MethodTypeSignature {

        public final List<FormalTypeParameter> formalTypeParameters;
        public final List<TypeSignature>       parameterTypes;
        public final TypeSignature             returnType;
        public final List<ThrowsSignature>     thrownTypes;

        public
        MethodTypeSignature(
            List<FormalTypeParameter> formalTypeParameters,
            List<TypeSignature>       parameterTypes,
            TypeSignature             returnType,
            List<ThrowsSignature>     thrownTypes
        ) {
            this.formalTypeParameters = formalTypeParameters;
            this.parameterTypes       = parameterTypes;
            this.returnType           = returnType;
            this.thrownTypes          = thrownTypes;
        }

        public String
        toString(String declaringClassName, String methodName) {
            StringBuilder sb = new StringBuilder();

            // Formal type parameters.
            if (!this.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = this.formalTypeParameters.iterator();
                sb.append('<' + it.next().toString());
                while (it.hasNext()) sb.append(", " + it.next().toString());
                sb.append('>');
            }

            // Name.
            if ("<init>".equals(methodName) && this.returnType == SignatureParser.VOID) {
                sb.append(declaringClassName);
            } else
            {
                sb.append(declaringClassName).append('.').append(methodName);
            }
            sb.append('(');
            Iterator<TypeSignature> it = this.parameterTypes.iterator();
            if (it.hasNext()) {
                for (;;) {
                    sb.append(it.next().toString());
                    if (!it.hasNext()) break;
                    sb.append(", ");
                }
            }
            sb.append(')');
            if (this.returnType != VOID) sb.append(" => ").append(this.returnType.toString());
            return sb.toString();
        }

        @Override public String
        toString() {
            return "*** Cannot easily be converted into String ***";
        }
    }

    /** Representation of the "ClassSignature" clause. */
    public static
    class ClassSignature {

        public final List<FormalTypeParameter> formalTypeParameters;
        public ClassTypeSignature              superclassSignature;
        public final List<ClassTypeSignature>  superinterfaceSignatures;

        public
        ClassSignature(
            List<FormalTypeParameter> formalTypeParameters,
            ClassTypeSignature        superclassSignature,
            List<ClassTypeSignature>  superinterfaceSignatures
        ) {
            this.formalTypeParameters     = formalTypeParameters;
            this.superclassSignature      = superclassSignature;
            this.superinterfaceSignatures = superinterfaceSignatures;
        }
        public String
        toString(String className) {
            StringBuilder sb = new StringBuilder();
            if (!this.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = this.formalTypeParameters.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
                sb.append('>');
            }
            sb.append(className).append(" extends ").append(this.superclassSignature.toString());
            if (!this.superinterfaceSignatures.isEmpty()) {
                Iterator<ClassTypeSignature> it = this.superinterfaceSignatures.iterator();
                sb.append(" implements ").append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
            }
            return sb.toString();
        }
    }

    /** Representation of the "ClassTypeSignature" clause. */
    public static
    class ClassTypeSignature implements ThrowsSignature, FieldTypeSignature {
        public final String                         packageSpecifier;
        public final String                         simpleClassName;
        public final List<TypeArgument>             typeArguments;
        public final List<SimpleClassTypeSignature> suffixes;

        public
        ClassTypeSignature(
            String                         packageSpecifier,
            String                         simpleClassName,
            List<TypeArgument>             typeArguments,
            List<SimpleClassTypeSignature> suffixes
        ) {
            this.packageSpecifier = packageSpecifier;
            this.simpleClassName  = simpleClassName;
            this.typeArguments    = typeArguments;
            this.suffixes         = suffixes;
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder(this.packageSpecifier.replace('/', '.')).append(this.simpleClassName);
            if (!this.typeArguments.isEmpty()) {
                Iterator<TypeArgument> it = this.typeArguments.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString());
                }
                sb.append('>');
            }
            for (SimpleClassTypeSignature suffix : this.suffixes) {
                sb.append(suffix.toString());
            }
            return sb.toString();
        }
    }

    public static final ClassTypeSignature OBJECT = new ClassTypeSignature(
        "java.lang.",
        "Object",
        Collections.<TypeArgument>emptyList(),
        Collections.<SimpleClassTypeSignature>emptyList()
    );

    /** Representation of the "SimpleClassTypeSignature" clause. */
    public static
    class SimpleClassTypeSignature {
        public final String             simpleClassName;
        public final List<TypeArgument> typeArguments;

        public
        SimpleClassTypeSignature(String simpleClassName, List<TypeArgument> typeArguments) {
            this.simpleClassName = simpleClassName;
            this.typeArguments   = typeArguments;
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder(this.simpleClassName);
            if (!this.typeArguments.isEmpty()) {
                Iterator<TypeArgument> it = this.typeArguments.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString());
                }
                sb.append('>');
            }
            return sb.toString();
        }
    }

    /** Representation of the "ArrayTypeSignature" clause. */
    public static
    class ArrayTypeSignature implements FieldTypeSignature {

        public final TypeSignature typeSignature;

        public
        ArrayTypeSignature(TypeSignature typeSignature) { this.typeSignature = typeSignature; }

        @Override public String toString() { return this.typeSignature.toString() + "[]"; }
    }

    /** Representation of the "TypeVariableSignature" clause. */
    public static
    class TypeVariableSignature implements ThrowsSignature, FieldTypeSignature {

        public String identifier;

        public
        TypeVariableSignature(String identifier) { this.identifier = identifier; }

        @Override public String toString() { return this.identifier; }
    }

    /** Representation of the "TypeSignature" clause. */
    public
    interface TypeSignature {
        @Override String toString();
    }

    /** Representation of the "ThrowsSignature" clause. */
    public
    interface ThrowsSignature {
    }

    /** Representation of the "FormalTypeParameter" clause. */
    public static
    class FormalTypeParameter {

        public final String                       identifier;
        @Nullable public final FieldTypeSignature classBound;
        public final List<FieldTypeSignature>     interfaceBounds;

        public
        FormalTypeParameter(
            String                       identifier,
            @Nullable FieldTypeSignature classBound,
            List<FieldTypeSignature>     interfaceBounds
        ) {
            this.identifier      = identifier;
            this.classBound      = classBound;
            this.interfaceBounds = interfaceBounds;
        }

        @Override public String
        toString() {
            FieldTypeSignature cb = this.classBound;
            if (cb == null) {
                Iterator<FieldTypeSignature> it = this.interfaceBounds.iterator();
                if (!it.hasNext()) return this.identifier;

                StringBuilder sb = new StringBuilder(this.identifier).append(" extends ").append(it.next().toString());
                while (it.hasNext()) sb.append(" & ").append(it.next().toString());
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder(this.identifier).append(" extends ").append(cb.toString());
                for (FieldTypeSignature ib : this.interfaceBounds) sb.append(" & ").append(ib.toString());
                return sb.toString();
            }
        }
    }

    /** Representation of the "PrimitiveTypeSignature" clause. */
    public static
    class PrimitiveTypeSignature implements TypeSignature {
        public final String typeName;
        PrimitiveTypeSignature(String typeName) {
            this.typeName = typeName;
        }
        @Override public String toString() { return this.typeName; }
    }

    /** Representation of the "TypeArgument" clause. */
    public static
    class TypeArgument {

        enum Mode { EXTENDS, SUPER, ANY, NONE }

        public final Mode                         mode;
        @Nullable public final FieldTypeSignature fieldTypeSignature;

        /**
         * @param fieldTypeSignature {@code null} iff {@code mode == ANY}
         */
        public
        TypeArgument(Mode mode, @Nullable FieldTypeSignature fieldTypeSignature) {
            assert mode == Mode.ANY ^ fieldTypeSignature == null;
            this.mode               = mode;
            this.fieldTypeSignature = fieldTypeSignature;
        }

        @SuppressWarnings("null") @Override public String
        toString() {
            switch (this.mode) {
            case EXTENDS:
                return "extends " + this.fieldTypeSignature.toString();
            case SUPER:
                return "super " + this.fieldTypeSignature.toString();
            case ANY:
                return "*";
            case NONE:
                return this.fieldTypeSignature.toString();
            }
            throw new IllegalStateException();
        }
    }

    /** Representation of the "FieldTypeSignature" clause. */
    public
    interface FieldTypeSignature extends TypeSignature {
        @Override String toString();
    }

    public static final PrimitiveTypeSignature BYTE    = new PrimitiveTypeSignature("byte");
    public static final PrimitiveTypeSignature CHAR    = new PrimitiveTypeSignature("char");
    public static final PrimitiveTypeSignature DOUBLE  = new PrimitiveTypeSignature("double");
    public static final PrimitiveTypeSignature FLOAT   = new PrimitiveTypeSignature("float");
    public static final PrimitiveTypeSignature INT     = new PrimitiveTypeSignature("int");
    public static final PrimitiveTypeSignature LONG    = new PrimitiveTypeSignature("long");
    public static final PrimitiveTypeSignature SHORT   = new PrimitiveTypeSignature("short");
    public static final PrimitiveTypeSignature BOOLEAN = new PrimitiveTypeSignature("boolean");

    public static final TypeSignature VOID = new TypeSignature() {
        @Override public String toString() { return "void"; }
    };

    private static TypeSignature
    parseFieldDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        return parseTypeSignature(scs);
    }

    private static MethodTypeSignature
    parseMethodDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        return parseMethodTypeSignature(scs);
    }

    private static ClassSignature
    parseClassSignature(StringCharStream scs) throws EOFException, SignatureException, UnexpectedCharacterException {
        List<FormalTypeParameter> ftps = new ArrayList<SignatureParser.FormalTypeParameter>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ftps.add(parseFormalTypeParameter(scs));

        ClassTypeSignature cts = parseClassTypeSignature(scs);

        List<ClassTypeSignature> siss = new ArrayList<SignatureParser.ClassTypeSignature>();
        while (!scs.atEoi()) siss.add(parseClassTypeSignature(scs));

        return new ClassSignature(ftps, cts, siss);
    }

    private static MethodTypeSignature
    parseMethodTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {

        List<FormalTypeParameter> ftps = new ArrayList<SignatureParser.FormalTypeParameter>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ftps.add(parseFormalTypeParameter(scs));

        scs.read('(');
        List<TypeSignature> pts = new ArrayList<SignatureParser.TypeSignature>();
        while (!scs.peekRead(')')) pts.add(parseTypeSignature(scs));

        TypeSignature rt = parseReturnType(scs);

        List<ThrowsSignature> tts = new ArrayList<SignatureParser.ThrowsSignature>();
        while (!scs.atEoi()) tts.add(parseThrowsSignature(scs));

        return new MethodTypeSignature(ftps, pts, rt, tts);
    }

    private static TypeSignature
    parseReturnType(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        if (scs.peekRead('V')) return VOID;
        return parseTypeSignature(scs);
    }

    private static ThrowsSignature
    parseThrowsSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('^');
        return scs.peek('T') ? parseTypeVariableSignature(scs) : parseClassTypeSignature(scs);
    }

    private static ClassTypeSignature
    parseClassTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('L');

        String                         ps = "";
        String                         scn;
        List<TypeArgument>             tas = new ArrayList<SignatureParser.TypeArgument>();
        List<SimpleClassTypeSignature> ss  = new ArrayList<SignatureParser.SimpleClassTypeSignature>();

        PART1:
        for (;;) {
            String s = parseIdentifier(scs);
            switch (scs.peek("<./;")) {
            case 0: // '<'
                scs.read('<');
                scn = s;
                while (!scs.peekRead('>')) tas.add(parseTypeArgument(scs));
                break PART1;
            case 1: // '.'
                scn = s;
                break PART1;
            case 2: // '/'
                ps += s + '/';
                scs.read();
                break;
            case 3: // ';'
                scn = s;
                break PART1;
            default:
                scs.read("<./;");
            }
        }

        while (scs.peekRead('.')) ss.add(parseSimpleClassTypeSignature(scs));

        scs.read(';');

        return new ClassTypeSignature(ps, scn, tas, ss);
    }

    private static String
    parseIdentifier(StringCharStream scs) throws EOFException, SignatureException {
        char c = scs.read();
        if (!Character.isJavaIdentifierStart(c)) {
            throw new SignatureException("Identifier expected instead of '" + c + "'");
        }
        StringBuilder sb = new StringBuilder().append(c);
        for (;;) {
            if (Character.isJavaIdentifierPart(scs.peek())) {
                sb.append(scs.read());
            } else {
                return sb.toString();
            }
        }
    }

    private static TypeVariableSignature
    parseTypeVariableSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('T');
        String identifier = parseIdentifier(scs);
        scs.read(';');
        return new TypeVariableSignature(identifier);
    }

    private static FormalTypeParameter
    parseFormalTypeParameter(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        String identifier = parseIdentifier(scs);
        scs.read(':');

        FieldTypeSignature cb = !scs.peek(':') ? parseFieldTypeSignature(scs) : null;

        List<FieldTypeSignature> ibs = new ArrayList<SignatureParser.FieldTypeSignature>();
        while (scs.peekRead(':')) ibs.add(parseFieldTypeSignature(scs));

        return new FormalTypeParameter(identifier, cb, ibs);
    }

    private static TypeSignature
    parseTypeSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        int idx = scs.peekRead("BCDFIJSZ");
        if (idx != -1) {
            return PRIMITIVE_TYPES[idx];
        }
        return parseFieldTypeSignature(scs);
    }
    private static final PrimitiveTypeSignature[] PRIMITIVE_TYPES = {
        BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT, BOOLEAN
    };

    private static FieldTypeSignature
    parseFieldTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        switch (scs.peek("L[T")) {
        case 0:
            return parseClassTypeSignature(scs);
        case 1:
            return parseArrayTypeSignature(scs);
        case 2:
            return parseTypeVariableSignature(scs);
        default:
            throw new SignatureException(
                "Class type signature, array type signature or type variable signature expected"
            );
        }
    }

    private static FieldTypeSignature
    parseArrayTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('[');
        return new ArrayTypeSignature(parseTypeSignature(scs));
    }

    private static SimpleClassTypeSignature
    parseSimpleClassTypeSignature(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {

        String scn = parseIdentifier(scs);

        List<TypeArgument> ta = new ArrayList<SignatureParser.TypeArgument>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ta.add(parseTypeArgument(scs));

        return new SimpleClassTypeSignature(scn, ta);
    }

    private static TypeArgument
    parseTypeArgument(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {

        if (scs.peekRead('+')) return new TypeArgument(TypeArgument.Mode.EXTENDS, parseFieldTypeSignature(scs));
        if (scs.peekRead('-')) return new TypeArgument(TypeArgument.Mode.SUPER, parseFieldTypeSignature(scs));
        if (scs.peekRead('*')) return new TypeArgument(TypeArgument.Mode.ANY, null);
        return new TypeArgument(TypeArgument.Mode.NONE, parseFieldTypeSignature(scs));
    }

    /** Signalizes am malformed signature. */
    public static
    class SignatureException extends Exception {

        private static final long serialVersionUID = 1L;

        public
        SignatureException(String message) {
            super(message);
        }

        public
        SignatureException(String message, Throwable t) {
            super(message, t);
        }
    }
}
