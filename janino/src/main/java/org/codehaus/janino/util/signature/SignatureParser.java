
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2021 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util.signature;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.charstream.StringCharStream;
import org.codehaus.janino.util.charstream.UnexpectedCharacterException;

/**
 * Helper class for parsing signatures and descriptors. See
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/ClassFileFormat-Java5.pdf">Java 5 class file format</a>,
 * section 4.4.4, "Signatures".
 * <p>
 *   The various structures that the parser returns (e.g. {@link ClassTypeSignature}) all have {@link
 *   Object#toString()} methods that convert them into nice, human-readable strings. This conversion can be customized
 *   using {@link #SignatureParser(Options)} and passing a custom {@link Options} object.
 * </p>
 */
public
class SignatureParser {

    /**
     * @see SignatureParser
     * @see SignatureParser#SignatureParser(Options)
     */
    public
    interface Options {

        /**
         * Optionally modifies package name prefixes before they are used in the various {@link #toString()} methods.
         *
         * @param  packageSpecifier E.g. {@code ""} (the root package) or {@code "java.lang."}
         * @return                  The <var>packageSpecifier</var>, or a beautified version thereof
         */
        String beautifyPackageNamePrefix(String packageSpecifier);
    }

    /**
     * A trivial implementation of {@link Options}.
     */
    public static final Options DEFAULT_OPTIONS = new Options() {

        @Override public String
        beautifyPackageNamePrefix(String packageSpecifier) {
            return packageSpecifier;
        }
    };

    private Options options = SignatureParser.DEFAULT_OPTIONS; // Initialize early to avoid an NPE!

    public
    SignatureParser() {}

    public
    SignatureParser(Options options) { this.options = options; }

    /**
     * Decodes a 'class signature' as defined in JVMS7 4.3.4 / JVMS8 4.7.9.1.
     */
    public ClassSignature
    decodeClassSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);

            ClassSignature cls = this.parseClassSignature(scs);
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

    /**
     * Decodes a 'method type signature' as defined in JVMS7 4.3.4 / JVMS8 4.7.9.1.
     */
    public MethodTypeSignature
    decodeMethodTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream          scs = new StringCharStream(s);
            final MethodTypeSignature mts = this.parseMethodTypeSignature(scs);
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

    /**
     * Decodes a 'type signature' as defined in JVMS7 4.3.4 / JVMS8 4.7.9.1.
     */
    public TypeSignature
    decodeTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = this.parseTypeSignature(scs);
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

    /**
     * Decodes a 'field type signature' as defined in JVMS7 4.3.4 / JVMS8 4.7.9.1.
     */
    public FieldTypeSignature
    decodeFieldTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream         scs = new StringCharStream(s);
            final FieldTypeSignature fts = this.parseFieldTypeSignature(scs);
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

    /**
     * Decodes a 'method descriptor' as defined in JVMS[78] 4.3.3.
     *
     * @return E.g. {@code "(Object[]) => java.util.stream.Stream"} or {@code "join()"} (void method)
     */
    public MethodTypeSignature
    decodeMethodDescriptor(String s) throws SignatureException {
        try {
            StringCharStream          scs = new StringCharStream(s);
            final MethodTypeSignature mts = this.parseMethodDescriptor(scs);
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

    private TypeSignature
    decodeClassName(String internalName) {
        String className = internalName.replace('/', '.');

        int idx = className.lastIndexOf('.') + 1;

        final String packageNamePrefix = className.substring(0, idx);
        final String simpleClassName   = className.substring(idx);

        return new TypeSignature() {

            @Override public String
            toString() {
                return SignatureParser.this.options.beautifyPackageNamePrefix(packageNamePrefix) + simpleClassName;
            }
        };
    }

    /**
     * Decodes a 'field descriptor' as defined in JLS7 4.3.2.
     */
    public TypeSignature
    decodeFieldDescriptor(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = this.parseFieldDescriptor(scs);
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

    /**
     * Decodes a "class-name-or-field-descriptor" as defined in JLS8 4.4.1 ("name_index").
     */
    public TypeSignature
    decodeClassNameOrFieldDescriptor(String s) throws SignatureException {

        if (Character.isJavaIdentifierStart(s.charAt(0))) {
            return this.decodeClassName(s);
        }

        return this.decodeFieldDescriptor(s);
    }

    /**
     * Decodes a 'return type' as defined in JVMS7 4.3.4 / JVMS8 4.7.9.1.
     */
    public TypeSignature
    decodeReturnType(String s) throws SignatureException {
        try {
            StringCharStream    scs = new StringCharStream(s);
            final TypeSignature ts  = this.parseReturnType(scs);
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

    /**
     * Representation of the "MethodTypeSignature" clause.
     */
    public static
    class MethodTypeSignature {

        /**
         * The formal types of the method, e.g. '{@code void <T, U> int meth(T t, U u) ...}'.
         */
        public final List<FormalTypeParameter> formalTypeParameters;

        /**
         * The types of the method's parameters.
         */
        public final List<TypeSignature> parameterTypes;

        /**
         * The return type of the method.
         */
        public final TypeSignature returnType;

        /**
         * The exceptions declared for the method.
         */
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
         * Combines the name of the declaring class, the name of the method and this method type signature into a nice,
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

            if (!this.thrownTypes.isEmpty()) {
                Iterator<ThrowsSignature> it2 = this.thrownTypes.iterator();
                sb.append(" throws ").append(it2.next());
                while (it.hasNext()) sb.append(", ").append(it2.next());
            }

            if (this.returnType != SignatureParser.VOID) sb.append(" => ").append(this.returnType.toString());
            return sb.toString();
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder();

            // Formal type parameters.
            if (!this.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = this.formalTypeParameters.iterator();
                sb.append('<' + it.next().toString());
                while (it.hasNext()) sb.append(", " + it.next().toString());
                sb.append("> ");
            }

            sb.append('(');
            Iterator<TypeSignature> it = this.parameterTypes.iterator();
            if (it.hasNext()) {
                sb.append(it.next());
                while (it.hasNext()) sb.append(", ").append(it.next());
            }
            sb.append(')');

            if (this.returnType != SignatureParser.VOID) sb.append(" => ").append(this.returnType.toString());

            return sb.toString();
        }
    }

    /**
     * Representation of the "ClassSignature" clause.
     */
    public static
    class ClassSignature {

        /**
         * The class's formal type parameters, e.g. '{@code class MyMap<K, V> ...}'.
         */
        public final List<FormalTypeParameter> formalTypeParameters;

        /**
         * The class's superclass type.
         */
        public ClassTypeSignature superclassSignature;

        /**
         * The interfaces that the class implements.
         */
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
         * Combines the name of the class and this class signature into a nice, human-readable string like '{@code
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

    /**
     * Representation of the "ClassTypeSignature" clause, e.g. '{@code pkg.Outer<T>.Inner<U>}'.
     */
    public static
    class ClassTypeSignature implements ThrowsSignature, FieldTypeSignature {

        /**
         * <pre>{ identifier '/' }</pre>
         */
        public final String packageSpecifier;

        /**
         * <pre>identifier</pre>
         */
        public final String simpleClassName;

        /**
         * The {@link TypeArgument}s of this class.
         */
        public final List<TypeArgument> typeArguments;

        /**
         * The nested types.
         */
        public final List<SimpleClassTypeSignature> suffixes;

        private final Options options;

        /**
         * @param packageSpecifier <code>{ identifier '/' }</code>
         */
        public
        ClassTypeSignature(
            String                         packageSpecifier,
            String                         simpleClassName,
            List<TypeArgument>             typeArguments,
            List<SimpleClassTypeSignature> suffixes,
            Options                        options
        ) {
            this.packageSpecifier = packageSpecifier;
            this.simpleClassName  = simpleClassName;
            this.typeArguments    = typeArguments;
            this.suffixes         = suffixes;
            this.options          = options;
        }

        @Override public <T, EX extends Throwable> T
        accept(FieldTypeSignatureVisitor<T, EX> visitor) throws EX { return visitor.visitClassTypeSignature(this); }

        /**
         * Converts this class type signature into a nice, human-readable string, e.g. {@code "pkg.Outer<T>.Inner<U>"}.
         */
        @Override public String
        toString() {

            String packageNamePrefix = this.packageSpecifier.replace('/', '.');

            StringBuilder sb = (
                new StringBuilder()
                .append(this.options.beautifyPackageNamePrefix(packageNamePrefix))
                .append(this.simpleClassName)
            );

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

    /**
     * The class type signature of the {@link Object} class.
     */
    public final ClassTypeSignature
    object = new ClassTypeSignature(
        "java/lang/",
        "Object",
        Collections.<TypeArgument>emptyList(),
        Collections.<SimpleClassTypeSignature>emptyList(),
        this.options
    );

    /**
     * Representation of the "SimpleClassTypeSignature" clause, e.g. '{@code MyMap<K, V>}'.
     */
    public static
    class SimpleClassTypeSignature {

        /**
         * The simple name of the class.
         */
        public final String simpleClassName;

        /**
         * The type arguments of the class, e.g. '{@code <A extends x, B super x, *, x>}'.
         */
        public final List<TypeArgument> typeArguments;

        public
        SimpleClassTypeSignature(String simpleClassName, List<TypeArgument> typeArguments) {
            this.simpleClassName = simpleClassName;
            this.typeArguments   = typeArguments;
        }

        /**
         * Converts this simple class type signature into a nice, human-readable string like '{@code MyClass<U>}'.
         */
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

    /**
     * Representation of the "ArrayTypeSignature" clause. The array's component have a {@link TypeSignature}.
     */
    public static
    class ArrayTypeSignature implements FieldTypeSignature {

        /**
         * The type of the array components.
         */
        public final TypeSignature componentTypeSignature;

        public
        ArrayTypeSignature(TypeSignature componentTypeSignature) {
            this.componentTypeSignature = componentTypeSignature;
        }

        @Override public <T, EX extends Throwable> T
        accept(FieldTypeSignatureVisitor<T, EX> visitor) throws EX { return visitor.visitArrayTypeSignature(this); }

        @Override public String toString() { return this.componentTypeSignature.toString() + "[]"; }
    }

    /**
     * Representation of the "TypeVariableSignature" clause, e.g. '{@code T}'.
     */
    public static
    class TypeVariableSignature implements ThrowsSignature, FieldTypeSignature {

        /**
         * The name of the type variable, e.g. '{@code T}'.
         */
        public String identifier;

        public
        TypeVariableSignature(String identifier) { this.identifier = identifier; }

        @Override public <T, EX extends Throwable> T
        accept(FieldTypeSignatureVisitor<T, EX> visitor) throws EX { return visitor.visitTypeVariableSignature(this); }

        @Override public String toString() { return this.identifier; }
    }

    /**
     * Representation of the "TypeSignature" clause. A 'type signature' is one of
     * <dl>
     *   <dt>{@link PrimitiveTypeSignature}:</dt>
     *   <dd>'{@code byte}', '{@code int}', etc., but <em>not</em> '{@code void}'</dd>
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

    /**
     * Representation of the "ThrowsSignature" clause.
     */
    public
    interface ThrowsSignature {
    }

    /**
     * Representation of the "FormalTypeParameter" clause, e.g. '{@code T extends MyClass & MyInterface}'.
     */
    public static
    class FormalTypeParameter {

        /**
         * The name of the formal type parameter, e.g. '{@code T}'.
         */
        public final String identifier;

        /**
         * The class that this formal type parameter (optionally) extends.
         */
        @Nullable public final FieldTypeSignature classBound;

        /**
         * The interfaces that this formal type parameter (optionally) extends.
         */
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
     * Representation of the "PrimitiveTypeSignature" clause, i.e. '{@code byte}', '{@code int}', etc., but <em>not</em>
     * '{@code void}'.
     */
    public static
    class PrimitiveTypeSignature implements TypeSignature {

        /**
         * The name of the primitive type, e.g. '{@code int}'.
         */
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

        /**
         * @see TypeArgument
         */
        enum Mode { EXTENDS, SUPER, ANY, NONE }

        /**
         * @see TypeArgument
         */
        public final Mode mode;

        /**
         * Must be {@code} for {@link SignatureParser.TypeArgument.Mode#ANY}, non-{@code null} otherwise.
         *
         * @see TypeArgument
         */
        @Nullable public final FieldTypeSignature fieldTypeSignature;

        /**
         * @param fieldTypeSignature {@code null} iff {@code mode == ANY}
         */
        public
        TypeArgument(Mode mode, @Nullable FieldTypeSignature fieldTypeSignature) {
            assert mode == Mode.ANY ^ fieldTypeSignature != null;
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
        public <T, EX extends Throwable> T accept(FieldTypeSignatureVisitor<T, EX> visitor) throws EX;
        @Override String toString();
    }

    public
    interface FieldTypeSignatureVisitor<T, EX extends Throwable> {
        T visitArrayTypeSignature(ArrayTypeSignature ats) throws EX;
        T visitClassTypeSignature(ClassTypeSignature cts) throws EX;
        T visitTypeVariableSignature(TypeVariableSignature tvs) throws EX;
    }

    /**
     * The primitive '{@code byte}' type.
     */
    public static final PrimitiveTypeSignature BYTE = new PrimitiveTypeSignature("byte");

    /**
     * The primitive '{@code char}' type.
     */
    public static final PrimitiveTypeSignature CHAR = new PrimitiveTypeSignature("char");

    /**
     * The primitive '{@code double}' type.
     */
    public static final PrimitiveTypeSignature DOUBLE = new PrimitiveTypeSignature("double");

    /**
     * The primitive '{@code float}' type.
     */
    public static final PrimitiveTypeSignature FLOAT = new PrimitiveTypeSignature("float");

    /**
     * The primitive '{@code int}' type.
     */
    public static final PrimitiveTypeSignature INT = new PrimitiveTypeSignature("int");

    /**
     * The primitive '{@code long}' type.
     */
    public static final PrimitiveTypeSignature LONG = new PrimitiveTypeSignature("long");

    /**
     * The primitive '{@code short}' type.
     */
    public static final PrimitiveTypeSignature SHORT = new PrimitiveTypeSignature("short");

    /**
     * The primitive '{@code boolean}' type.
     */
    public static final PrimitiveTypeSignature BOOLEAN = new PrimitiveTypeSignature("boolean");

    /**
     * Representation of the 'void' type.
     */
    public static final TypeSignature
    VOID = new TypeSignature() { @Override public String toString() { return "void"; } };

    private TypeSignature
    parseFieldDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {

        // A type signatures is a superset of a type descriptor, so use "parseTypeSignature()" for simplicity.
        return this.parseTypeSignature(scs);
    }

    private MethodTypeSignature
    parseMethodDescriptor(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        return this.parseMethodTypeSignature(scs);
    }

    private ClassSignature
    parseClassSignature(StringCharStream scs) throws EOFException, SignatureException, UnexpectedCharacterException {

        List<FormalTypeParameter> ftps = new ArrayList<>();
        if (scs.peekRead('<')) {
            while (!scs.peekRead('>')) ftps.add(this.parseFormalTypeParameter(scs));
        }

        final ClassTypeSignature cts = this.parseClassTypeSignature(scs);

        List<ClassTypeSignature> siss = new ArrayList<>();
        while (!scs.atEoi()) siss.add(this.parseClassTypeSignature(scs));

        return new ClassSignature(ftps, cts, siss);
    }

    private MethodTypeSignature
    parseMethodTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {

        List<FormalTypeParameter> ftps = new ArrayList<>();
        if (scs.peekRead('<')) {
            while (!scs.peekRead('>')) ftps.add(this.parseFormalTypeParameter(scs));
        }

        scs.read('(');

        List<TypeSignature> pts = new ArrayList<>();
        while (!scs.peekRead(')')) pts.add(this.parseTypeSignature(scs));

        final TypeSignature rt = this.parseReturnType(scs);

        List<ThrowsSignature> tts = new ArrayList<>();
        while (!scs.atEoi()) tts.add(this.parseThrowsSignature(scs));

        return new MethodTypeSignature(ftps, pts, rt, tts);
    }

    private TypeSignature
    parseReturnType(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        if (scs.peekRead('V')) return SignatureParser.VOID;
        return this.parseTypeSignature(scs);
    }

    private ThrowsSignature
    parseThrowsSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('^');
        return (
            scs.peek('T')
            ? SignatureParser.parseTypeVariableSignature(scs)
            : this.parseClassTypeSignature(scs)
        );
    }

    private ClassTypeSignature
    parseClassTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('L');

        String             ps = "";
        String             scn;
        List<TypeArgument> tas = new ArrayList<>();

        PART1:
        for (;;) {
            String s = SignatureParser.parseIdentifier(scs);
            switch (scs.peek("<./;")) {
            case 0: // '<'
                scs.read('<');
                scn = s;
                while (!scs.peekRead('>')) tas.add(this.parseTypeArgument(scs));
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

        List<SimpleClassTypeSignature> ss = new ArrayList<>();
        while (scs.peekRead('.')) {

            final String ir = SignatureParser.parseIdentifierRest(scs);

            List<TypeArgument> ta = new ArrayList<>();
            if (scs.peekRead('<')) while (!scs.peekRead('>')) ta.add(this.parseTypeArgument(scs));

            ss.add(new SimpleClassTypeSignature(ir, ta));
        }

        scs.read(';');

        return new ClassTypeSignature(ps, scn, tas, ss, this.options);
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

    private static String
    parseIdentifierRest(StringCharStream scs) throws EOFException, SignatureException {
        char c = scs.read();
        if (!Character.isJavaIdentifierPart(c)) {
            throw new SignatureException("Identifier rest expected instead of '" + c + "'");
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

    private FormalTypeParameter
    parseFormalTypeParameter(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        final String identifier = SignatureParser.parseIdentifier(scs);
        scs.read(':');

        final FieldTypeSignature cb = !scs.peek(':') ? this.parseFieldTypeSignature(scs) : null;

        List<FieldTypeSignature> ibs = new ArrayList<>();
        while (scs.peekRead(':')) ibs.add(this.parseFieldTypeSignature(scs));

        return new FormalTypeParameter(identifier, cb, ibs);
    }

    private TypeSignature
    parseTypeSignature(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {
        int idx = scs.peekRead("BCDFIJSZ");
        if (idx != -1) {
            return SignatureParser.PRIMITIVE_TYPES[idx];
        }
        return this.parseFieldTypeSignature(scs);
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

    private FieldTypeSignature
    parseFieldTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        switch (scs.peek("L[T")) {
        case 0:
            return this.parseClassTypeSignature(scs);
        case 1:
            return this.parseArrayTypeSignature(scs);
        case 2:
            return SignatureParser.parseTypeVariableSignature(scs);
        default:
            throw new SignatureException(
                "Parsing field type signature \""
                + scs
                + "\": Class type signature, array type signature or type variable signature expected"
            );
        }
    }

    private FieldTypeSignature
    parseArrayTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('[');
        return new ArrayTypeSignature(this.parseTypeSignature(scs));
    }

    private TypeArgument
    parseTypeArgument(StringCharStream scs) throws EOFException, UnexpectedCharacterException, SignatureException {

        if (scs.peekRead('+')) {
            return new TypeArgument(TypeArgument.Mode.EXTENDS, this.parseFieldTypeSignature(scs));
        }

        if (scs.peekRead('-')) {
            return new TypeArgument(TypeArgument.Mode.SUPER, this.parseFieldTypeSignature(scs));
        }

        if (scs.peekRead('*')) {
            return new TypeArgument(TypeArgument.Mode.ANY, null);
        }

        // E.g. "Comparable<java.lang.Boolean>"
        return new TypeArgument(TypeArgument.Mode.NONE, this.parseFieldTypeSignature(scs));
    }

    /**
     * Signalizes am malformed signature.
     */
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
