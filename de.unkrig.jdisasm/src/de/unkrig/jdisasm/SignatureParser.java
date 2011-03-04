
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.unkrig.io.charstream.StringCharStream;
import de.unkrig.io.charstream.UnexpectedCharacterException;

/**
 * Helper class for parsing signatures and descriptors. See
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/ClassFileFormat-Java5.pdf">Java 5 class file format</a>,
 * section 4.4.4, "Signatures".
 */
public class SignatureParser {

    private SignatureParser() {}

    public static ClassSignature decodeClassSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            ClassSignature cls;
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

    public static MethodTypeSignature decodeMethodTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
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

    public static FieldTypeSignature decodeFieldTypeSignature(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
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

    public static MethodTypeSignature decodeMethodDescriptor(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
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
    
    public static TypeSignature decodeFieldDescriptor(String s) throws SignatureException {
        try {
            StringCharStream scs = new StringCharStream(s);
            TypeSignature ts = parseFieldDescriptor(scs);
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

    /** Representation of the "MethodTypeSignature" clause. */
    public static class MethodTypeSignature {
    
        public final List<FormalTypeParameter> formalTypeParameters = (
            new ArrayList<SignatureParser.FormalTypeParameter>()
        );
        public final List<TypeSignature>       parameterTypes = new ArrayList<SignatureParser.TypeSignature>();
        public TypeSignature                   returnType;
        public final List<ThrowsSignature>     thrownTypes = new ArrayList<SignatureParser.ThrowsSignature>();
    
        public String toString(String declaringClassName, String methodName) {
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
    
        public String toString() {
            return "*** Cannot easily be converted into String ***";
        }
    }

    /** Representation of the "ClassSignature" clause. */
    public static class ClassSignature {
        public final List<FormalTypeParameter> formalTypeParameters = new ArrayList<FormalTypeParameter>();
        public ClassTypeSignature              superclassSignature;
        public final List<ClassTypeSignature>  superinterfaceSignatures = new ArrayList<ClassTypeSignature>();
    
        public String toString(String className) {
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
    public static class ClassTypeSignature implements ThrowsSignature, FieldTypeSignature {
        public String                               packageSpecifier = "";
        public String                               simpleClassName;
        public final List<TypeArgument>             typeArguments = new ArrayList<TypeArgument>();
        public final List<SimpleClassTypeSignature> suffixes = (
            new ArrayList<SignatureParser.SimpleClassTypeSignature>()
        );
    
        public String toString() {
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

    public static final ClassTypeSignature OBJECT = new ClassTypeSignature();
    static { OBJECT.simpleClassName = "java.lang.Object"; }

    /** Representation of the "SimpleClassTypeSignature" clause. */
    public static class SimpleClassTypeSignature {
        public String                   simpleClassName;
        public final List<TypeArgument> typeArguments = new ArrayList<TypeArgument>();
    
        public String toString() {
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
    public static class ArrayTypeSignature implements FieldTypeSignature {
        public TypeSignature typeSignature;
        public String toString() { return this.typeSignature.toString() + "[]"; }
    }

    /** Representation of the "TypeVariableSignature" clause. */
    public static class TypeVariableSignature implements ThrowsSignature, FieldTypeSignature {
        public String identifier;
        public String toString() { return this.identifier; }
    }

    /** Representation of the "TypeSignature" clause. */
    public interface TypeSignature {
        String toString();
    }

    /** Representation of the "ThrowsSignature" clause. */
    public interface ThrowsSignature {
    }

    /** Representation of the "FormalTypeParameter" clause. */
    public static class FormalTypeParameter {
    
        public String                         identifier;
        public FieldTypeSignature             optionalClassBound;
        public final List<FieldTypeSignature> interfaceBounds = new ArrayList<SignatureParser.FieldTypeSignature>();
    
        public String toString() {
            StringBuilder sb = new StringBuilder(this.identifier);
            if (this.optionalClassBound == null) {
                if (this.interfaceBounds.isEmpty()) return this.identifier;
                Iterator<FieldTypeSignature> it = this.interfaceBounds.iterator();
                sb.append(" extends ").append(it.next().toString());
                while (it.hasNext()) sb.append(" & ").append(it.next().toString());
            } else {
                sb.append(" extends ").append(this.optionalClassBound.toString());
                for (FieldTypeSignature ib : this.interfaceBounds) sb.append(" & ").append(ib.toString());
            }
            return sb.toString();
        }
    }

    /** Representation of the "PrimitiveTypeSignature" clause. */
    public static class PrimitiveTypeSignature implements TypeSignature {
        public final String typeName;
        private PrimitiveTypeSignature(String typeName) {
            this.typeName = typeName;
        }
        public String toString() { return this.typeName; };
    }

    /** Representation of the "TypeArgument" clause. */
    public static class TypeArgument {
        /***/
        enum Mode { EXTENDS, SUPER, ANY, NONE };
        public Mode               mode;
        public FieldTypeSignature fieldTypeSignature;
    
        public String toString() {
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

    private static TypeSignature parseFieldDescriptor(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        return parseTypeSignature(scs);
    }

    private static MethodTypeSignature parseMethodDescriptor(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        return parseMethodTypeSignature(scs);
    }

    private static ClassSignature parseClassSignature(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        ClassSignature cls = new ClassSignature();
        if (scs.peekRead('<')) {
            while (!scs.peekRead('>')) {
                cls.formalTypeParameters.add(parseFormalTypeParameter(scs));
            }
        }
        cls.superclassSignature = parseClassTypeSignature(scs);
        while (!scs.atEoi()) cls.superinterfaceSignatures.add(parseClassTypeSignature(scs));
        return cls;
    }

    private static MethodTypeSignature parseMethodTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        MethodTypeSignature mts = new MethodTypeSignature();
        
        if (scs.peekRead('<')) {
            while (!scs.peekRead('>')) {
                mts.formalTypeParameters.add(parseFormalTypeParameter(scs));
            }
        }

        scs.read('(');
        while (!scs.peekRead(')')) {
            mts.parameterTypes.add(parseTypeSignature(scs));
        }
        mts.returnType = parseReturnType(scs);
        while (!scs.atEoi()) mts.thrownTypes.add(parseThrowsSignature(scs));

        return mts;
    }

    private static TypeSignature parseReturnType(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        if (scs.peekRead('V')) return VOID;
        return parseTypeSignature(scs);
    }
    private static ThrowsSignature parseThrowsSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        scs.read('^');
        return scs.peek('T') ? parseTypeVariableSignature(scs) : parseClassTypeSignature(scs);
    }

    private static ClassTypeSignature parseClassTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        ClassTypeSignature cts = new ClassTypeSignature();
        scs.read('L');

        PART1:
        for (;;) {
            String s = parseIdentifier(scs);
            switch (scs.peek("<./;")) {
            case 0: // '<'
                scs.read('<');
                cts.simpleClassName = s;
                while (!scs.peekRead('>')) {
                    cts.typeArguments.add(parseTypeArgument(scs));
                }
                break PART1;
            case 1: // '.'
                cts.simpleClassName = s;
                break PART1;
            case 2: // '/'
                cts.packageSpecifier += s + '/';
                scs.read();
                break;
            case 3: // ';'
                cts.simpleClassName = s;
                break PART1;
            default:
                scs.read("<./;");
            }
        }

        while (scs.peekRead('.')) {
            cts.suffixes.add(parseSimpleClassTypeSignature(scs));
        }
        scs.read(';');

        return cts;
    }

    private static String parseIdentifier(StringCharStream scs) throws EOFException, SignatureException {
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

    private static TypeVariableSignature parseTypeVariableSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        TypeVariableSignature tvs = new TypeVariableSignature();
        scs.read('T');
        tvs.identifier = parseIdentifier(scs);
        scs.read(';');
        return tvs;
    }

    private static FormalTypeParameter parseFormalTypeParameter(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        FormalTypeParameter ftp = new FormalTypeParameter();
        ftp.identifier = parseIdentifier(scs);
        scs.read(':');
        if (!scs.peek(':')) {
            ftp.optionalClassBound = parseFieldTypeSignature(scs);
        }
        while (scs.peekRead(':')) {
            ftp.interfaceBounds.add(parseFieldTypeSignature(scs));
        }
        return ftp;
    }

    private static TypeSignature parseTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        int idx = scs.peekRead("BCDFIJSZ");
        if (idx != -1) {
            return PRIMITIVE_TYPES[idx];
        }
        return parseFieldTypeSignature(scs);
    }
    private static final PrimitiveTypeSignature[] PRIMITIVE_TYPES = {
        BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, SHORT, BOOLEAN
    };
    
    private static FieldTypeSignature parseFieldTypeSignature(StringCharStream scs)
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

    private static FieldTypeSignature parseArrayTypeSignature(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        ArrayTypeSignature ats = new ArrayTypeSignature();
        scs.read('[');
        ats.typeSignature = parseTypeSignature(scs);
        return ats;
    }

    private static SimpleClassTypeSignature parseSimpleClassTypeSignature(StringCharStream scs)
    throws EOFException, SignatureException, UnexpectedCharacterException {
        SimpleClassTypeSignature scts = new SimpleClassTypeSignature();

        scts.simpleClassName = parseIdentifier(scs);
        if (scs.peekRead('<')) {
            while (!scs.peekRead('>')) {
                scts.typeArguments.add(parseTypeArgument(scs));
            }
        }
        return scts;
    }

    private static TypeArgument parseTypeArgument(StringCharStream scs)
    throws EOFException, UnexpectedCharacterException, SignatureException {
        TypeArgument ta = new TypeArgument();
        if (scs.peekRead('+')) {
            ta.mode = TypeArgument.Mode.EXTENDS;
            ta.fieldTypeSignature = parseFieldTypeSignature(scs);
        } else
        if (scs.peekRead('-')) {
            ta.mode = TypeArgument.Mode.SUPER;
            ta.fieldTypeSignature = parseFieldTypeSignature(scs);
        } else
        if (scs.peekRead('*')) {
            ta.mode = TypeArgument.Mode.ANY;
        } else
        {
            ta.mode = TypeArgument.Mode.NONE;
            ta.fieldTypeSignature = parseFieldTypeSignature(scs);
        }
        return ta;
    }

    /** Signalizes am malformed signature. */
    public static class SignatureException extends Exception {

        private static final long serialVersionUID = 1L;

        public SignatureException(String message) {
            super(message);
        }
        
        public SignatureException(String message, Throwable t) {
            super(message, t);
        }
    }
}
