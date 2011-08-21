
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.unkrig.jdisasm.ConstantPool.ConstantClassInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantNameAndTypeInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantUtf8Info;
import de.unkrig.jdisasm.SignatureParser.SignatureException;

/**
 * Representation of a Java&trade; class file.
 */
public class ClassFile {
    public short                                minorVersion;
    public short                                majorVersion;
    public ConstantPool                         constantPool;
    public short                                accessFlags;
    public String                               thisClassName;
    public String                               superClassName;
    public final List<String>                   interfaceNames = new ArrayList<String>();
    public final List<Field>                    fields = new ArrayList<Field>();
    public final List<Method>                   methods = new ArrayList<Method>();
    public DeprecatedAttribute                  deprecatedAttribute;
    public EnclosingMethodAttribute             enclosingMethodAttribute;
    public InnerClassesAttribute                innerClassesAttribute;
    public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
    public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
    public SignatureAttribute                   signatureAttribute;
    public SourceFileAttribute                  sourceFileAttribute;
    public SyntheticAttribute                   syntheticAttribute;
    public final List<Attribute>                attributes = new ArrayList<Attribute>();

    public static final short ACC_PUBLIC       = 0x00000001;
    public static final short ACC_PRIVATE      = 0x00000002;
    public static final short ACC_PROTECTED    = 0x00000004;
    public static final short ACC_STATIC       = 0x00000008;
    public static final short ACC_FINAL        = 0x00000010;
    public static final short ACC_SYNCHRONIZED = 0x00000020;
    public static final short ACC_VOLATILE     = 0x00000040;
    public static final short ACC_BRIDGE       = 0x00000040;
    public static final short ACC_TRANSIENT    = 0x00000080;
    public static final short ACC_VARARGS      = 0x00000080;
    public static final short ACC_NATIVE       = 0x00000100;
    public static final short ACC_INTERFACE    = 0x00000200;
    public static final short ACC_ABSTRACT     = 0x00000400;
    public static final short ACC_STRICT       = 0x00000800;
    public static final short ACC_SYNTHETIC    = 0x00001000;
    public static final short ACC_ANNOTATION   = 0x00002000;
    public static final short ACC_ENUM         = 0x00004000;
    
    public ClassFile(DataInputStream dis) throws IOException {

        // Magic number.
        if (dis.readInt() != 0xcafebabe) {
            throw new IOException("Wrong magic number - this is not a valid class file");
        }

        // JDK version.
        this.minorVersion = dis.readShort();
        this.majorVersion = dis.readShort();

        // Load constant pool.
        this.constantPool = new ConstantPool(dis);

        this.accessFlags = dis.readShort();
        this.thisClassName = this.constantPool.getConstantClassInfo(dis.readShort()).name;
        this.superClassName = this.constantPool.getConstantClassInfo(dis.readShort()).name;

        // Implemented interfaces.
        for (short i = dis.readShort(); i > 0; --i) {
            this.interfaceNames.add(this.constantPool.getConstantClassInfo(dis.readShort()).name);
        }

        // Fields.
        {
            short n = dis.readShort();
            for (short i = 0; i < n; i++) {
                try {
                    this.fields.add(new Field(dis));
                } catch (IOException ioe) {
                    IOException ioe2 = new IOException("Reading field #" + i + " of " + n + ": " + ioe.getMessage());
                    ioe2.initCause(ioe);
                    throw ioe2;
                } catch (RuntimeException re) {
                    throw new RuntimeException("Reading field #" + i + " of " + n + ": " + re.getMessage(), re);
                }
            }
        }

        // Methods.
        {
            short n = dis.readShort();
            for (short i = 0; i < n; i++) {
                try {
                    this.methods.add(new Method(dis));
                } catch (IOException ioe) {
                    IOException ioe2 = new IOException("Reading method #" + i + " of " + n + ": " + ioe.getMessage());
                    ioe2.initCause(ioe);
                    throw ioe2;
                } catch (RuntimeException re) {
                    throw new RuntimeException("Reading method #" + i + " of " + n + ": " + re.getMessage(), re);
                }
            }
        }

        // Class attributes.
        readAttributes(dis, new AbstractAttributeVisitor() {

            public void visit(DeprecatedAttribute da) {
                ClassFile.this.deprecatedAttribute = da;
                ClassFile.this.attributes.add(da);
            }

            public void visit(EnclosingMethodAttribute ema) {
                ClassFile.this.enclosingMethodAttribute = ema;
                ClassFile.this.attributes.add(ema);
            }

            public void visit(InnerClassesAttribute ica) {
                ClassFile.this.innerClassesAttribute = ica;
                ClassFile.this.attributes.add(ica);
            }

            public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                ClassFile.this.runtimeInvisibleAnnotationsAttribute = riaa;
                ClassFile.this.attributes.add(riaa);
            }

            public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                ClassFile.this.runtimeVisibleAnnotationsAttribute = rvaa;
                ClassFile.this.attributes.add(rvaa);
            }

            public void visit(SignatureAttribute sa) {
                ClassFile.this.signatureAttribute = sa;
                ClassFile.this.attributes.add(sa);
            }

            public void visit(SourceFileAttribute sfa) {
                ClassFile.this.sourceFileAttribute = sfa;
                ClassFile.this.attributes.add(sfa);
            }

            public void visit(SyntheticAttribute sa) {
                ClassFile.this.syntheticAttribute = sa;
                ClassFile.this.attributes.add(sa);
            }

            public void acceptOther(Attribute a) {
                ClassFile.this.attributes.add(a);
            }
        });
    }

    public String getJdkName() {
        switch (this.majorVersion) {
        case 50:
            return "J2SE 6.0";
        case 49:
            return "J2SE 5.0";
        case 48:
            return "JDK 1.4";
        case 47:
            return "JDK 1.3";
        case 46:
            return "JDK 1.2";
        case 45:
            return "JDK 1.1";
        default:
            return "Version " + this.majorVersion + "/" + this.minorVersion + " does not match any known JDK";
        }
    }

    /** Representation of a field description in a Java&trade; class file. */
    public class Field {
        public short                                accessFlags;
        public String                               name;
        public String                               descriptor;
        public ConstantValueAttribute               constantValueAttribute;
        public DeprecatedAttribute                  deprecatedAttribute;
        public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
        public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
        public SignatureAttribute                   signatureAttribute;
        public SyntheticAttribute                   syntheticAttribute;
        public final List<Attribute>                attributes = new ArrayList<Attribute>();
    
        public Field(DataInputStream dis) throws IOException {
            this.accessFlags = dis.readShort();
            this.name = ClassFile.this.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            this.descriptor = ClassFile.this.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
    
            // Read field attributes.
            readAttributes(dis, new AbstractAttributeVisitor() {
    
                public void visit(ConstantValueAttribute cva) {
                    Field.this.constantValueAttribute = cva;
                    Field.this.attributes.add(cva);
                }
    
                public void visit(DeprecatedAttribute da) {
                    Field.this.deprecatedAttribute = da;
                    Field.this.attributes.add(da);
                }
    
                public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                    Field.this.runtimeInvisibleAnnotationsAttribute = riaa;
                    Field.this.attributes.add(riaa);
                }
    
                public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                    Field.this.runtimeVisibleAnnotationsAttribute = rvaa;
                    Field.this.attributes.add(rvaa);
                }
    
                public void visit(SignatureAttribute sa) {
                    Field.this.signatureAttribute = sa;
                    Field.this.attributes.add(sa);
                }
    
                public void visit(SyntheticAttribute sa) {
                    Field.this.syntheticAttribute = sa;
                    Field.this.attributes.add(sa);
                }
    
                public void acceptOther(Attribute ai) {
                    Field.this.attributes.add(ai);
                }
            });
        }
    }

    /** Representation of a method in a Java&trade; class file. */
    public class Method {
        public short                                         accessFlags;
        public String                                        name;
        public String                                        descriptor;
        public final List<Attribute>                         attributes = new ArrayList<Attribute>();
        public AnnotationDefaultAttribute                    annotationDefaultAttribute;
        public CodeAttribute                                 codeAttribute;
        public DeprecatedAttribute                           deprecatedAttribute;
        public ExceptionsAttribute                           exceptionsAttribute;
        public RuntimeInvisibleAnnotationsAttribute          runtimeInvisibleAnnotationsAttribute;
        public RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute;
        public RuntimeVisibleAnnotationsAttribute            runtimeVisibleAnnotationsAttribute;
        public RuntimeVisibleParameterAnnotationsAttribute   runtimeVisibleParameterAnnotationsAttribute;
        public SignatureAttribute                            signatureAttribute;
        public SyntheticAttribute                            syntheticAttribute;
    
        public Method(DataInputStream dis) throws IOException {
            this.accessFlags = dis.readShort();
            this.name = ClassFile.this.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            this.descriptor = ClassFile.this.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
    
            try {
                // Read method attributes.
                readAttributes(dis, new AbstractAttributeVisitor() {
                    public void visit(AnnotationDefaultAttribute ada) {
                        Method.this.annotationDefaultAttribute = ada;
                        Method.this.attributes.add(ada);
                    }
                    public void visit(CodeAttribute ca) {
                        Method.this.codeAttribute = ca;
                        Method.this.attributes.add(ca);
                    }
   
                    public void visit(DeprecatedAttribute da) {
                        Method.this.deprecatedAttribute = da;
                        Method.this.attributes.add(da);
                    }
   
                    public void visit(ExceptionsAttribute ea) {
                        Method.this.exceptionsAttribute = ea;
                        Method.this.attributes.add(ea);
                    }
                    public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                        Method.this.runtimeInvisibleAnnotationsAttribute = riaa;
                        Method.this.attributes.add(riaa);
                    }
                    public void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
                        Method.this.runtimeInvisibleParameterAnnotationsAttribute = ripaa;
                        Method.this.attributes.add(ripaa);
                    }
                    public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                        Method.this.runtimeVisibleAnnotationsAttribute = rvaa;
                        Method.this.attributes.add(rvaa);
                    }
                    public void visit(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
                        Method.this.runtimeVisibleParameterAnnotationsAttribute = rvpaa;
                        Method.this.attributes.add(rvpaa);
                    }
                    public void visit(SignatureAttribute sa) {
                        Method.this.signatureAttribute = sa;
                        Method.this.attributes.add(sa);
                    }
                    public void visit(SyntheticAttribute sa) {
                        Method.this.syntheticAttribute = sa;
                        Method.this.attributes.add(sa);
                    }
                    public void acceptOther(Attribute ai) {
                        Method.this.attributes.add(ai);
                    }
                });
            } catch (IOException ioe) {
                IOException ioe2 = new IOException("Parsing method '" + this.name + "' [" + this.descriptor + "]: " + ioe.getMessage());
                ioe2.initCause(ioe);
                throw ioe2;
            } catch (RuntimeException re) {
                throw new RuntimeException("Parsing method '" + this.name + "' [" + this.descriptor + "]: " + re.getMessage(), re);
            }
        }
    }

    /** Representation of an attribute in a Java&trade; class file. */
    public interface Attribute {
        void   accept(AttributeVisitor visitor);
        String getName();
    }

    private void readAttributes(DataInputStream dis, AttributeVisitor visitor) throws IOException {
        short n = dis.readShort();
        for (int i = 0; i < n; ++i) {
            try {
                readAttribute(dis, visitor);
            } catch (IOException ioe) {
                IOException ioe2 = new IOException("Reading attribute #" + i + " of " + n + ": " + ioe.getMessage());
                ioe2.initCause(ioe);
                throw ioe2;
            } catch (RuntimeException re) {
                throw new RuntimeException("Reading attribute #" + i + " of " + n + ": " + re.getMessage(), re);
            }
        }
    }

    private void readAttribute(DataInputStream dis, AttributeVisitor visitor) throws IOException {
        
        String attributeName = this.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        
        // Read attribute body into byte array and create a DataInputStream.
        ByteArrayInputStream bais;
        {
            int attributeLength = dis.readInt();
            final byte[] ba = new byte[attributeLength];
            dis.readFully(ba);
            bais = new ByteArrayInputStream(ba);
        }
        
        // Parse the attribute body.
        this.readAttributeBody(attributeName, new DataInputStream(bais), visitor);
        
        // Check for extraneous bytes.
        {
            int av = bais.available();
            if (av > 0) {
                throw new RuntimeException(av + " extraneous bytes in attribute \"" + attributeName + "\"");
            }
        }
    }

    private void readAttributeBody(
        final String          attributeName,
        final DataInputStream dis,
        AttributeVisitor      visitor
    ) throws IOException {
        if (attributeName.equals("AnnotationDefault")) {
            visitor.visit(new AnnotationDefaultAttribute(dis, this));
        } else
        if (attributeName.equals("ConstantValue")) {
            visitor.visit(new ConstantValueAttribute(dis, this));
        } else
        if (attributeName.equals("Code")) {
            visitor.visit(new CodeAttribute(dis, this));
        } else
        if (attributeName.equals("Deprecated")) {
            visitor.visit(new DeprecatedAttribute(dis, this));
        } else
        if (attributeName.equals("EnclosingMethod")) {
            visitor.visit(new EnclosingMethodAttribute(dis, this));
        } else
        if (attributeName.equals("Exceptions")) {
            visitor.visit(new ExceptionsAttribute(dis, this));
        } else
        if (attributeName.equals("InnerClasses")) {
            visitor.visit(new InnerClassesAttribute(dis, this));
        } else
        if (attributeName.equals("LineNumberTable")) {
            visitor.visit(new LineNumberTableAttribute(dis, this));
        } else
        if (attributeName.equals("LocalVariableTable")) {
            visitor.visit(new LocalVariableTableAttribute(dis, this));
        } else
        if (attributeName.equals("LocalVariableTypeTable")) {
            visitor.visit(new LocalVariableTypeTableAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeInvisibleAnnotations")) {
            visitor.visit(new RuntimeInvisibleAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeInvisibleParameterAnnotations")) {
            visitor.visit(new RuntimeInvisibleParameterAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeVisibleAnnotations")) {
            visitor.visit(new RuntimeVisibleAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
            visitor.visit(new RuntimeVisibleParameterAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("Signature")) {
            visitor.visit(new SignatureAttribute(dis, this));
        } else
        if (attributeName.equals("SourceFile")) {
            visitor.visit(new SourceFileAttribute(dis, this));
        } else
        if (attributeName.equals("Synthetic")) {
            visitor.visit(new SyntheticAttribute(dis, this));
        } else
        {
            visitor.visit(new UnknownAttribute(attributeName, dis, this));
        }
    }

    /** The visitor for {@Attribute}. */
    public interface AttributeVisitor {
        enum Context { CLASS, FIELD, METHOD };

        void visit(AnnotationDefaultAttribute                    ada);
        void visit(CodeAttribute                                 ca);
        void visit(ConstantValueAttribute                        cva);
        void visit(DeprecatedAttribute                           da);
        void visit(EnclosingMethodAttribute                      ema);
        void visit(ExceptionsAttribute                           ea);
        void visit(InnerClassesAttribute                         ica);
        void visit(LineNumberTableAttribute                      lnta);
        void visit(LocalVariableTableAttribute                   lvta);
        void visit(LocalVariableTypeTableAttribute               lvtta);
        void visit(RuntimeInvisibleAnnotationsAttribute          riaa);
        void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa);
        void visit(RuntimeVisibleAnnotationsAttribute            rvaa);
        void visit(RuntimeVisibleParameterAnnotationsAttribute   rvpaa);
        void visit(SignatureAttribute                            sa);
        void visit(SourceFileAttribute                           sfa);
        void visit(SyntheticAttribute                            sa);
    
        void visit(UnknownAttribute unknownAttribute);
    }

    /** Default implementation of the {@link AttributeVisitor}. */
    public static abstract class AbstractAttributeVisitor implements AttributeVisitor {
    
        public abstract void acceptOther(Attribute ai);
    
        public void visit(AnnotationDefaultAttribute                    ada)   { acceptOther(ada); }
        public void visit(CodeAttribute                                 ca)    { acceptOther(ca); }
        public void visit(ConstantValueAttribute                        cva)   { acceptOther(cva); }
        public void visit(DeprecatedAttribute                           da)    { acceptOther(da); }
        public void visit(EnclosingMethodAttribute                      ema)   { acceptOther(ema); }
        public void visit(ExceptionsAttribute                           ea)    { acceptOther(ea); }
        public void visit(InnerClassesAttribute                         ica)   { acceptOther(ica); }
        public void visit(LineNumberTableAttribute                      lnta)  { acceptOther(lnta); }
        public void visit(LocalVariableTableAttribute                   lvta)  { acceptOther(lvta); }
        public void visit(LocalVariableTypeTableAttribute               lvtta) { acceptOther(lvtta); }
        public void visit(RuntimeInvisibleAnnotationsAttribute          riaa)  { acceptOther(riaa); }
        public void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) { acceptOther(ripaa); }
        public void visit(RuntimeVisibleAnnotationsAttribute            rvaa)  { acceptOther(rvaa); }
        public void visit(RuntimeVisibleParameterAnnotationsAttribute   rvpaa) { acceptOther(rvpaa); }
        public void visit(SignatureAttribute                            sa)    { acceptOther(sa); }
        public void visit(SourceFileAttribute                           sfa)   { acceptOther(sfa); }
        public void visit(SyntheticAttribute                            sa)    { acceptOther(sa); }
        public void visit(UnknownAttribute                              a)     { acceptOther(a); }
    }

    /** Representation of an attribute with an unknown name. */
    public static final class UnknownAttribute implements Attribute {
        public final String name;
        public byte[]       info;
    
        private UnknownAttribute(String name, DataInputStream dis, ClassFile cf) throws IOException {
            this.name = name;
            this.info = readByteArray(dis, dis.available());
        }
    
        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return this.name; }
    }

    /** Representation of a {@code Synthetic} attribute. */
    public static class SyntheticAttribute implements Attribute {
        public SyntheticAttribute(DataInputStream dis, ClassFile cf) {
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "Synthetic"; }
    }

    /** Representation of a {@code Deprecated} attribute. */
    public static class DeprecatedAttribute implements Attribute {
        public DeprecatedAttribute(DataInputStream dis, ClassFile cf) {
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "Deprecated"; }
    }

    /** Representation of a {@code InnerClasses} attribute. */
    public static final class InnerClassesAttribute implements Attribute {

        /** Helper class for {@link InnerClassesAttribute}. */
        public static class ClasS {
            public ConstantClassInfo innerClassInfo;
            public ConstantClassInfo outerClassInfo;
            public ConstantUtf8Info  innerName;
            public short             innerClassAccessFlags;

            public ClasS(DataInputStream dis, ClassFile cf) throws IOException {
                this.innerClassInfo = cf.constantPool.getConstantClassInfo(dis.readShort());
                this.outerClassInfo = cf.constantPool.getConstantClassInfo(dis.readShort());
                this.innerName = cf.constantPool.getConstantUtf8Info(dis.readShort());
                this.innerClassAccessFlags = dis.readShort();
            }
        }

        public final List<ClasS> classes = new ArrayList<ClasS>();

        private InnerClassesAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.classes.add(new ClasS(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "InnerClasses"; }
    }

    /** Representation of the {@code RuntimeVisibleAnnotations} attribute. */
    public static class RuntimeVisibleAnnotationsAttribute implements Attribute {

        public final List<Annotation> annotations = new ArrayList<Annotation>();
        
        private RuntimeVisibleAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = 0xffff & dis.readShort(); i > 0; --i) {
                this.annotations.add(new Annotation(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "RuntimeVisibleAnnotations"; }
    }

    /** Representation of the {@code RuntimeInvisibleAnnotations} attribute. */
    public static class RuntimeInvisibleAnnotationsAttribute extends RuntimeVisibleAnnotationsAttribute {
        public RuntimeInvisibleAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    /** Helper class for {@link RuntimeVisibleParameterAnnotationsAttribute}. */
    public static class ParameterAnnotation {
        public final List<Annotation> annotations = new ArrayList<Annotation>();

        public ParameterAnnotation(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.annotations.add(new Annotation(dis, cf));
            }
        }
    }

    /** Representation of the {@code RuntimeVisibleParameterAnnotations} attribute. */
    public static class RuntimeVisibleParameterAnnotationsAttribute implements Attribute {

        public final List<ParameterAnnotation> parameterAnnotations = new ArrayList<ParameterAnnotation>();

        private RuntimeVisibleParameterAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readByte(); i > 0; --i) {
                this.parameterAnnotations.add(new ParameterAnnotation(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "RuntimeVisibleParameterAnnotations"; }
    }

    /** Representation of the {@code RuntimeInvisibleParameterAnnotations} attribute. */
    public static class RuntimeInvisibleParameterAnnotationsAttribute
    extends RuntimeVisibleParameterAnnotationsAttribute {
        public RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    
    /** Representation of the {@code AnnotationDefault} attribute. */
    public static class AnnotationDefaultAttribute implements Attribute {

        public ElementValue defaultValue;
        
        private AnnotationDefaultAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.defaultValue = newElementValue(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "AnnotationDefault"; }
    }

    /** Helper class for the {@code Runtime*visible*Annotations} attributes. */
    public static class Annotation {
    
        /** Helper class for the {@code Runtime*visible*Annotations} attributes. */
        public static class ElementValuePair {
    
            public String       elementName;
            public ElementValue elementValue;
    
            public ElementValuePair(DataInputStream dis, ClassFile cf) throws IOException {
                this.elementName = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes; // elementNameIndex
                this.elementValue = newElementValue(dis, cf); 
            }
    
            public String toString() {
                return (
                    "value".equals(this.elementName)
                    ? this.elementValue.toString()
                    : this.elementName + " = " + this.elementValue.toString()
                );
            }
        }
    
        public String                       typeName;
        public final List<ElementValuePair> elementValuePairs = new ArrayList<ElementValuePair>();
    
        public Annotation(DataInputStream dis, ClassFile cf) throws IOException {
            short typeIndex = dis.readShort();
            try {
                this.typeName = SignatureParser.decodeFieldDescriptor(
                    cf.constantPool.getConstantUtf8Info(typeIndex).bytes
                ).toString();
            } catch (SignatureException e) {
                IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
            for (int i = dis.readShort(); i > 0; --i) {
                this.elementValuePairs.add(new ElementValuePair(dis, cf));
            }
        }
    
        public String toString() {
            StringBuilder sb = new StringBuilder("@").append(this.typeName).append('(');
            Iterator<ElementValuePair> it = this.elementValuePairs.iterator();
            if (it.hasNext()) {
                sb.append(it.next().toString());
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString());
                }
            }
            return sb.append(')').toString();
        }
    }

    /**
     * Representation of an annotation element value.
     */
    public interface ElementValue {
    }

    private static ElementValue newElementValue(DataInputStream dis, ClassFile cf) throws IOException {
        final byte tag = dis.readByte();
        if ("BCDFIJSZ".indexOf(tag) != -1) {
            final String s = cf.constantPool.getIntegerFloatLongDouble(dis.readShort());
            return new ElementValue() { public String toString() { return s; }};
        } else
        if (tag == 's') {
            final String s = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            return new ElementValue() { public String toString() { return ConstantPool.stringToJavaLiteral(s); }};
        } else
        if (tag == 'e') {
            String typeName = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            String constName = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            try {
                final String s = SignatureParser.decodeFieldDescriptor(typeName) + "." + constName;
                return new ElementValue() { public String toString() { return s; }};
            } catch (SignatureException e) {
                IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
        } else
        if (tag == 'c') {
            final String classInfo = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            try {
                final String s = SignatureParser.decodeFieldDescriptor(classInfo) + ".class";
                return new ElementValue() { public String toString() { return s; }};
            } catch (SignatureException e) {
                IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
        } else
        if (tag == '@') {
            final Annotation annotation = new Annotation(dis, cf);
            return new ElementValue() { public String toString() { return annotation.toString(); }};
        } else
        if (tag == '[') {
            final List<ElementValue> values = new ArrayList<ElementValue>();
            for (int i = dis.readShort(); i > 0; --i) {
                values.add(newElementValue(dis, cf));
            }
            return new ElementValue() { public String toString() {
                Iterator<ElementValue> it = values.iterator();
                if (!it.hasNext()) return "{}";
    
                ElementValue firstValue = it.next();
                if (!it.hasNext()) return firstValue.toString();
    
                StringBuilder sb = new StringBuilder("{ ").append(firstValue.toString());
                do {
                    sb.append(", ").append(it.next().toString());
                } while (it.hasNext());
                return sb.append(" }").toString();
            }};
        } else
        {
            return new ElementValue() {
                public String toString() { return "[Invalid element value tag '" + (char) tag + "']"; }
            };
        }
    }

    /** Representation of the {@code Signature} attribute. */
    public static final class SignatureAttribute implements Attribute {
        public final String signature;

        private SignatureAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.signature = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "Signature"; }
    }
    
    /** Representation of the {@code EnclosingMethod} attribute. */
    public static final class EnclosingMethodAttribute implements Attribute {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo optionalMethod;
        
        private EnclosingMethodAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.clasS = cf.constantPool.getConstantClassInfo(dis.readShort());  // classIndex
            this.optionalMethod = cf.constantPool.getConstantNameAndTypeInfo(dis.readShort());  // methodIndex
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "EnclosingMethod"; }
    }
    
    /** Representation of the {@code Exceptions} attribute. */
    public static final class ExceptionsAttribute implements Attribute {
        public final List<ConstantClassInfo> exceptionNames = new ArrayList<ConstantClassInfo>();

        private ExceptionsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.exceptionNames.add(cf.constantPool.getConstantClassInfo(dis.readShort()));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "Exceptions"; }
    }

    /** Representation of the {@code Code} attribute. */
    public static final class CodeAttribute implements Attribute {
        public final short                     maxStack;
        public final short                     maxLocals;
        public final byte[]                    code;
        public final List<ExceptionTableEntry> exceptionTable = new ArrayList<ExceptionTableEntry>();
        public LocalVariableTableAttribute     localVariableTableAttribute;
        public LocalVariableTypeTableAttribute localVariableTypeTableAttribute;
        public LineNumberTableAttribute        lineNumberTableAttribute;
        public final List<Attribute>           attributes = new ArrayList<Attribute>();

        private CodeAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.maxStack = dis.readShort();
            this.maxLocals = dis.readShort();

            // Byte code.
            this.code = readByteArray(dis, dis.readInt());

            // Exception table.
            for (short i = dis.readShort(); i > 0; --i) {
                this.exceptionTable.add(new ExceptionTableEntry(dis, cf));
            }

            // Code attributes.
            cf.readAttributes(dis, new AbstractAttributeVisitor() {
                public void visit(LineNumberTableAttribute lnta) {
                    CodeAttribute.this.lineNumberTableAttribute = lnta;
                    CodeAttribute.this.attributes.add(lnta);
                }
                public void visit(LocalVariableTableAttribute lvta) {
                    CodeAttribute.this.localVariableTableAttribute = lvta;
                    CodeAttribute.this.attributes.add(lvta);
                }
                public void visit(LocalVariableTypeTableAttribute lvtta) {
                    CodeAttribute.this.localVariableTypeTableAttribute = lvtta;
                    CodeAttribute.this.attributes.add(lvtta);
                }
                public void acceptOther(Attribute ai) {
                    CodeAttribute.this.attributes.add(ai);
                }
            });
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "Code"; }
    }

    /** helper class for {@link CodeAttribute}. */
    public static class ExceptionTableEntry {
        public int               startPC;
        public int               endPC;
        public int               handlerPC;
        public ConstantClassInfo catchType;

        private ExceptionTableEntry(DataInputStream dis, ClassFile cf) throws IOException {
            this.startPC   = 0xffff & dis.readShort();
            this.endPC     = 0xffff & dis.readShort();
            this.handlerPC = 0xffff & dis.readShort();
            this.catchType = cf.constantPool.getConstantClassInfo(dis.readShort());
        }

        @Override
        public String toString() {
            return "startPC=" + this.startPC + " endPC=" + this.endPC + " handlerPC=" + this.handlerPC + " catchType=" + this.catchType;
        }
    }

    /** Representation of the {@code SourceFile} attribute. */
    public static class SourceFileAttribute implements Attribute {
        public String sourceFile;

        private SourceFileAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.sourceFile = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "SourceFile"; }
    }

    /** Representation of the {@code LineNumberTable} attribute. */
    public class LineNumberTableAttribute implements Attribute {

        public final List<LineNumberTableEntry> entries = new ArrayList<LineNumberTableEntry>();

        private LineNumberTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.entries.add(new LineNumberTableEntry(dis));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "LineNumberTable"; }
    }

    /** Helper class for {@link LineNumberTableAttribute}. */
    public static class LineNumberTableEntry {
        public int startPC;
        public int lineNumber;

        private LineNumberTableEntry(DataInputStream dis) throws IOException {
            this.startPC    = 0xffff & dis.readShort();
            this.lineNumber = 0xffff & dis.readShort();
        }
    }

    /** Representation of the {@code LocalVariableTable} attribute. */
    public class LocalVariableTableAttribute implements Attribute {

        /** Helper class for {@link LocalVariableTableAttribute}. */
        class Entry {
            public short  startPC;
            public short  length;
            public String name;
            public String descriptor;
            public short  index;

            private Entry(DataInputStream dis, ClassFile cf) throws IOException {
                this.startPC    = dis.readShort();
                this.length     = dis.readShort();
                this.name       = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
                this.descriptor = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
                this.index      = dis.readShort();
            }
        }

        public final List<Entry> entries = new ArrayList<Entry>();

        private LocalVariableTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                this.entries.add(new Entry(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "LocalVariableTable"; }
    }

    /** Representation of the {@code LocalVariableTypeTable} attribute. */
    public static class LocalVariableTypeTableAttribute implements Attribute {

        /** Helper class for {@link LocalVariableTypeTableAttribute}. */
        public static class Entry {
            public int    startPC;
            public int    length;
            public String name;
            public String signature;
            public short  index;

            private Entry(DataInputStream dis, ClassFile cf) throws IOException {
                this.startPC   = 0xffff & dis.readShort();
                this.length    = 0xffff & dis.readShort();
                this.name      = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
                this.signature = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
                this.index     = dis.readShort();
            }
        }

        public final List<Entry> entries = new ArrayList<Entry>();

        private LocalVariableTypeTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                this.entries.add(new Entry(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "LocalVariableTypeTable"; }
    }
    
    /** Representation of the {@code ConstantValue} attribute. */
    public static final class ConstantValueAttribute implements Attribute {
        public final String constantValue;

        private ConstantValueAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.constantValue = cf.constantPool.getIntegerFloatLongDoubleString(dis.readShort());
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }

        public String getName() { return "ConstantValue"; }
    }
    
    private static byte[] readByteArray(DataInputStream dis, int size) throws IOException {
        byte[] res = new byte[size];
        dis.readFully(res);
        return res;
    }
}
