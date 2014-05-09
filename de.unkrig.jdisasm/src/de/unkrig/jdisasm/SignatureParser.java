
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

    /** Decodes a 'class signature' as defined in JLS7 4.3.4. */
    public static ClassSignature
    decodeClassSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            ClassSignature   cls;
            cls = SignatureParser.parseClassSignature(scs);
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

    /** Decodes a 'method type signature' as defined in JLS7 4.3.4. */
    public static MethodTypeSignature
    decodeMethodTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream          scs = new StringCharStream(s);
            final MethodTypeSignature mts = SignatureParser.parseMethodTypeSignature(scs);
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

    /** Decodes a 'type signature' as defined in JLS7 4.3.4. */
    public static TypeSignature
    decodeTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = SignatureParser.parseTypeSignature(scs);
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

    /** Decodes a 'field type signature' as defined in JLS7 4.3.4. */
    public static FieldTypeSignature
    decodeFieldTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream         scs = new StringCharStream(s);
            final FieldTypeSignature fts = SignatureParser.parseFieldTypeSignature(scs);
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

    /** Decodes a 'method descriptor' as defined in JLS7 4.3.3. */
    public static MethodTypeSignature
    decodeMethodDescriptor(String s) throws SignatureException {
        try {
            StringCharStream          scs = new StringCharStream(s);
            final MethodTypeSignature mts = SignatureParser.parseMethodDescriptor(scs);
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

    /** Decodes a 'field descriptor' as defined in JLS7 4.3.2. */
    public static TypeSignature
    decodeFieldDescriptor(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = SignatureParser.parseFieldDescriptor(scs);
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

    /** Decodes a 'return type' as defined in JLS7 4.3.4. */
    public static TypeSignature
    decodeReturnType(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = SignatureParser.parseReturnType(scs);
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

        /** The formal types of the method, e.g. '{@code void <T, U> int meth(T t, U u) ...}'. */
        public final List<FormalTypeParameter> formalTypeParameters;

        /** The types of the method's parameters. */
        public final List<TypeSignature> parameterTypes;

        /** The return type of the method. */
        public final TypeSignature returnType;

        /** The exceptions declared for the method. */
        public final List<ThrowsSignature> thrownTypes;

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

        /**
         * Combine the name of the declaring class, the name of the method and this method type signature into a nice,
         * human-readable string like '{@code <T> MyClass.meth(List<T> l, int i) => double}'.
         */
        public String
        toString(String declaringClassName, String methodName) {
            StringBuilder sb = new StringBuilder();

            // Formal type parameters.
            if (!this.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = this.formalTypeParameters.iterator();
                sb.append('<' + it.next().toString());
                while (it.hasNext()) sb.append(", " + it.next().toString());
                sb.append("> ");
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
            if (this.returnType != SignatureParser.VOID) sb.append(" => ").append(this.returnType.toString());
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

        /** The class's formal type parameters, e.g. '{@code class MyMap<K, V> ...}'. */
        public final List<FormalTypeParameter> formalTypeParameters;

        /** The class's superclass type. */
        public ClassTypeSignature superclassSignature;

        /** The interfaces that the class implements. */
        public final List<ClassTypeSignature> superinterfaceSignatures;

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

        /**
         * Combine the name of the class and this class signature into a nice, human-readable string like '{@code
         * MyMap<K, V> extends SomeClass implements Interface1, Interface2}'.
         */
        public String
        toString(String className) {
            StringBuilder sb = new StringBuilder(className);
            if (!this.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = this.formalTypeParameters.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
                sb.append('>');
            }
            sb.append(" extends ").append(this.superclassSignature.toString());
            if (!this.superinterfaceSignatures.isEmpty()) {
                Iterator<ClassTypeSignature> it = this.superinterfaceSignatures.iterator();
                sb.append(" implements ").append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
            }
            return sb.toString();
        }
    }

    /** Representation of the "ClassTypeSignature" clause, e.g. '{@code pkg.Outer<T>.Inner<U>}'. */
    public static
    class ClassTypeSignature implements ThrowsSignature, FieldTypeSignature {

        /** <pre>{ identifier '/' }</pre> */
        public final String packageSpecifier;

        /** <pre>identifier</pre> */
        public final String simpleClassName;

        /** The {@link TypeArgument}s of this class. */
        public final List<TypeArgument> typeArguments;

        /** The nested types. */
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

        /**
         * Converts this class type signature into a nice, human-readable string like '{@code pkg.Outer<T>.Inner<U>}'.
         */
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
                sb.append('.').append(suffix.toString());
            }
            return sb.toString();
        }
    }

    /** The class type signature of the {@link Object} class. */
    public static final ClassTypeSignature OBJECT = new ClassTypeSignature(
        "java.lang.",
        "Object",
        Collections.<TypeArgument>emptyList(),
        Collections.<SimpleClassTypeSignature>emptyList()
    );

    /** Representation of the "SimpleClassTypeSignature" clause, e.g. '{@code MyMap<K, V>}'. */
    public static
    class SimpleClassTypeSignature {

        /** The simple name of the class. */
        public final String simpleClassName;

        /** The type arguments of the class, e.g. '{@code <A extends x, B super x, *, x>}'. */
        public final List<TypeArgument> typeArguments;

        public
        SimpleClassTypeSignature(String simpleClassName, List<TypeArgument> typeArguments) {
            this.simpleClassName = simpleClassName;
            this.typeArguments   = typeArguments;
        }

        /** Converts this simple class type signature into a nice, human-readable string like '{@code MyClass<U>}'. */
        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder(this.simpleClassName);
            if (!this.typeArguments.isEmpty()) {
                Iterator<TypeArgument> it = this.typeArguments.iterator();
                sb.append('<').append(it.next().toString());
                while (it.hasNext()) sb.append(", ").append(it.next().toString());
                sb.append('>');
            }
            return sb.toString();
        }
    }

    /** Representation of the "ArrayTypeSignature" clause. The array's component have a {@link TypeSignature}. */
    public static
    class ArrayTypeSignature implements FieldTypeSignature {

        /** The type of the array components. */
        public final TypeSignature componentTypeSignature;

        public
        ArrayTypeSignature(TypeSignature componentTypeSignature) {
            this.componentTypeSignature = componentTypeSignature;
        }

        @Override public String toString() { return this.componentTypeSignature.toString() + "[]"; }
    }

    /** Representation of the "TypeVariableSignature" clause, e.g. '{@code T}'. */
    public static
    class TypeVariableSignature implements ThrowsSignature, FieldTypeSignature {

        /** The name of the type variable, e.g. '{@code T}'. */
        public String identifier;

        public
        TypeVariableSignature(String identifier) { this.identifier = identifier; }

        @Override public String toString() { return this.identifier; }
    }

    /**
     * Representation of the "TypeSignature" clause. A 'type signature' is one of
     * <dl>
     *   <dt>{@link PrimitiveTypeSignature}:</dt>
     *   <dd>'{@code byte}', '{@code int}', etc., but <i>not</i> '{@code void}'</dd>
     *   <dt>{@link FieldTypeSignature}:</dt>
     *   <dd>
     *     One of:
     *     <dl>
     *       <dd>{@link ClassTypeSignature} (e.g. '{@code pkg.Outer<T>.Inner<U>}')</dd>
     *       <dd>{@link ArrayTypeSignature}</dd>
     *       <dd>{@link TypeVariableSignature} (e.g. '{@code T}')</dd>
     *     </dl>
     *   </dd>
     *   <dt>{@link SignatureParser#VOID}</dt>
     * </dl>
     */
    public
    interface TypeSignature {
        @Override String toString();
    }

    /** Representation of the "ThrowsSignature" clause. */
    public
    interface ThrowsSignature {
    }

    /** Representation of the "FormalTypeParameter" clause, e.g. '{@code T extends MyClass & MyInterface}'. */
    public static
    class FormalTypeParameter {

        /** The name of the formal type parameter, e.g. '{@code T}'. */
        public final String identifier;

        /** The class that this formal type parameter (optionally) extends. */
        @Nullable public final FieldTypeSignature classBound;

        /** The interfaces that this formal type parameter (optionally) extends. */
        public final List<FieldTypeSignature> interfaceBounds;

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

    /**
     * Representation of the "PrimitiveTypeSignature" clause, i.e. '{@code byte}', '{@code int}', etc., but <i>not</i>
     * '{@code void}'.
     */
    public static
    class PrimitiveTypeSignature implements TypeSignature {

        /** The name of the primitive type, e.g. '{@code int}'. */
        public final String typeName;

        PrimitiveTypeSignature(String typeName) {
            this.typeName = typeName;
        }

        @Override public String toString() { return this.typeName; }
    }

    /**
     * Representation of the "TypeArgument" clause.
     * <pre>
     *   type-argument :=
     *     'extends' {@link FieldTypeSignature field-type-signature}
     *     | 'super' {@link FieldTypeSignature field-type-signature}
     *     | '*'
     *     | {@link FieldTypeSignature field-type-signature}
     * </pre>
     */
    public static
    class TypeArgument {

        /** @see TypeArgument */
        enum Mode { EXTENDS, SUPER, ANY, NONE }

        /** @see TypeArgument */
        public final Mode mode;

        /**
         * Must be {@code} for {@link Mode#ANY}, non-{@code null} otherwise.
         *
         * @see TypeArgument
         */
        @Nullable public final FieldTypeSignature fieldTypeSignature;

        /** @param fieldTypeSignature {@code null} iff {@code mode == ANY} */
        public
        TypeArgument(Mode mode, @Nullable FieldTypeSignature fieldTypeSignature) {
            assert mode == Mode.ANY ^ fieldTypeSignature == null;
            this.mode               = mode;
            this.fieldTypeSignature = fieldTypeSignature;
        }

        @Override public String
        toString() {

            FieldTypeSignature fts = this.fieldTypeSignature;

            switch (this.mode) {

            case EXTENDS:
                assert fts != null;
                return "extends " + fts.toString();

            case SUPER:
                assert fts != null;
                return "super " + fts.toString();

            case ANY:
                return "*";

            case NONE:
                assert fts != null;
                return fts.toString();

            default:
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Representation of the "FieldTypeSignature" clause. A 'field type signature' is one of
     * <dl>
     *   <dt>{@link ClassTypeSignature}:
     *   <dd>E.g. '{@code pkg.Outer<T>.Inner<U>}'</dd>
     *   <dt>{@link ArrayTypeSignature}
     *   <dt>{@link TypeVariableSignature}:
     *   <dd>E.g. '{@code T}'</dd>
     * </dl>
     */
    public
    interface FieldTypeSignature extends TypeSignature {
        @Override String toString();
    }

    /** The primitive '{@code byte}' type. */
    public static final PrimitiveTypeSignature BYTE = new PrimitiveTypeSignature("byte");

    /** The primitive '{@code char}' type. */
    public static final PrimitiveTypeSignature CHAR = new PrimitiveTypeSignature("char");

    /** The primitive '{@code double}' type. */
    public static final PrimitiveTypeSignature DOUBLE = new PrimitiveTypeSignature("double");

    /** The primitive '{@code float}' type. */
    public static final PrimitiveTypeSignature FLOAT = new PrimitiveTypeSignature("float");

    /** The primitive '{@code int}' type. */
    public static final PrimitiveTypeSignature INT = new PrimitiveTypeSignature("int");

    /** The primitive '{@code long}' type. */
    public static final PrimitiveTypeSignature LONG = new PrimitiveTypeSignature("long");

    /** The primitive '{@code short}' type. */
    public static final PrimitiveTypeSignature SHORT = new PrimitiveTypeSignature("short");

    /** The primitive '{@code boolean}' type. */
    public static final PrimitiveTypeSignature BOOLEAN = new PrimitiveTypeSignature("boolean");

    /** Representation of the 'void' type. */
    public static final TypeSignature VOID = new TypeSignature() {
        @Override public String toString() { return "void"; }
    };

    private static TypeSignature
    parseFieldDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        return SignatureParser.parseTypeSignature(scs);
    }

    private static MethodTypeSignature
    parseMethodDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        return SignatureParser.parseMethodTypeSignature(scs);
    }

    private static ClassSignature
    parseClassSignature(StringCharStream scs) throws EOFException, SignatureException, UnexpectedCharacterException {
        List<FormalTypeParameter> ftps = new ArrayList<SignatureParser.FormalTypeParameter>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ftps.add(SignatureParser.parseFormalTypeParameter(scs));

        final ClassTypeSignature cts = SignatureParser.parseClassTypeSignature(scs);

        List<ClassTypeSignature> siss = new ArrayList<SignatureParser.ClassTypeSignature>();
        while (!scs.atEoi()) siss.add(SignatureParser.parseClassTypeSignature(scs));

        return new ClassSignature(ftps, cts, siss);
    }

    private static MethodTypeSignature
    parseMethodTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {

        List<FormalTypeParameter> ftps = new ArrayList<SignatureParser.FormalTypeParameter>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ftps.add(SignatureParser.parseFormalTypeParameter(scs));

        scs.read('(');
        List<TypeSignature> pts = new ArrayList<SignatureParser.TypeSignature>();
        while (!scs.peekRead(')')) pts.add(SignatureParser.parseTypeSignature(scs));

        final TypeSignature rt = SignatureParser.parseReturnType(scs);

        List<ThrowsSignature> tts = new ArrayList<SignatureParser.ThrowsSignature>();
        while (!scs.atEoi()) tts.add(SignatureParser.parseThrowsSignature(scs));

        return new MethodTypeSignature(ftps, pts, rt, tts);
    }

    private static TypeSignature
    parseReturnType(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        if (scs.peekRead('V')) return SignatureParser.VOID;
        return SignatureParser.parseTypeSignature(scs);
    }

    private static ThrowsSignature
    parseThrowsSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('^');
        return (
            scs.peek('T')
            ? SignatureParser.parseTypeVariableSignature(scs)
            : SignatureParser.parseClassTypeSignature(scs)
        );
    }

    private static ClassTypeSignature
    parseClassTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('L');

        String             ps = "";
        String             scn;
        List<TypeArgument> tas = new ArrayList<SignatureParser.TypeArgument>();

        PART1:
        for (;;) {
            String s = SignatureParser.parseIdentifier(scs);
            switch (scs.peek("<./;")) {
            case 0: // '<'
                scs.read('<');
                scn = s;
                while (!scs.peekRead('>')) tas.add(SignatureParser.parseTypeArgument(scs));
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

        List<SimpleClassTypeSignature> ss  = new ArrayList<SignatureParser.SimpleClassTypeSignature>();
        while (scs.peekRead('.')) ss.add(SignatureParser.parseSimpleClassTypeSignature(scs));

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
        final String identifier = SignatureParser.parseIdentifier(scs);
        scs.read(';');
        return new TypeVariableSignature(identifier);
    }

    private static FormalTypeParameter
    parseFormalTypeParameter(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        final String identifier = SignatureParser.parseIdentifier(scs);
        scs.read(':');

        final FieldTypeSignature cb = !scs.peek(':') ? SignatureParser.parseFieldTypeSignature(scs) : null;

        List<FieldTypeSignature> ibs = new ArrayList<SignatureParser.FieldTypeSignature>();
        while (scs.peekRead(':')) ibs.add(SignatureParser.parseFieldTypeSignature(scs));

        return new FormalTypeParameter(identifier, cb, ibs);
    }

    private static TypeSignature
    parseTypeSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        int idx = scs.peekRead("BCDFIJSZ");
        if (idx != -1) {
            return SignatureParser.PRIMITIVE_TYPES[idx];
        }
        return SignatureParser.parseFieldTypeSignature(scs);
    }
    private static final PrimitiveTypeSignature[] PRIMITIVE_TYPES = {
        SignatureParser.BYTE,
        SignatureParser.CHAR,
        SignatureParser.DOUBLE,
        SignatureParser.FLOAT,
        SignatureParser.INT,
        SignatureParser.LONG,
        SignatureParser.SHORT,
        SignatureParser.BOOLEAN,
    };

    private static FieldTypeSignature
    parseFieldTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        switch (scs.peek("L[T")) {
        case 0:
            return SignatureParser.parseClassTypeSignature(scs);
        case 1:
            return SignatureParser.parseArrayTypeSignature(scs);
        case 2:
            return SignatureParser.parseTypeVariableSignature(scs);
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
        return new ArrayTypeSignature(SignatureParser.parseTypeSignature(scs));
    }

    private static SimpleClassTypeSignature
    parseSimpleClassTypeSignature(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {

        final String scn = SignatureParser.parseIdentifier(scs);

        List<TypeArgument> ta = new ArrayList<SignatureParser.TypeArgument>();
        if (scs.peekRead('<')) while (!scs.peekRead('>')) ta.add(SignatureParser.parseTypeArgument(scs));

        return new SimpleClassTypeSignature(scn, ta);
    }

    private static TypeArgument
    parseTypeArgument(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {

        if (scs.peekRead('+')) {
            return new TypeArgument(TypeArgument.Mode.EXTENDS, SignatureParser.parseFieldTypeSignature(scs));
        }

        if (scs.peekRead('-')) {
            return new TypeArgument(TypeArgument.Mode.SUPER, SignatureParser.parseFieldTypeSignature(scs));
        }

        if (scs.peekRead('*')) {
            return new TypeArgument(TypeArgument.Mode.ANY, null);
        }

        return new TypeArgument(TypeArgument.Mode.NONE, SignatureParser.parseFieldTypeSignature(scs));
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
