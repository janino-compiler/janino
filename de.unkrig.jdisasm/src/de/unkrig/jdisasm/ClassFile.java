
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.jdisasm.ConstantPool.ConstantClassInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantNameAndTypeInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantUtf8Info;
import de.unkrig.jdisasm.SignatureParser.SignatureException;

/** Representation of a Java&trade; class file. */
public
class ClassFile {

    /**
     * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1-200-B">minor and major
     * version numbers of this class file</a>.
     */
    public short minorVersion, majorVersion;

    /**
     * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1-200-C">number of entries in
     * the constant pool table plus one</a>.
     */
    public ConstantPool constantPool;

    /**
     * A <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1-200-E">mask of flags used to
     * denote access permissions to and properties of this class or interface</a>.
     */
    public short accessFlags;

    /** The fully qualified (dot-separated) name of this type. */
    public String thisClassName;

    /**
     * The fully qualified (dot-separated) name of the superclass of this type; "java.lang.Object" iff this type is an
     * interface; {@code null} iff this type is {@link Object}.
     */
    @Nullable public String superClassName;

    /** Fully qualified (dot-separated) names of the interfaces that this type implements. */
    public final List<String> interfaceNames = new ArrayList<String>();

    /**
     * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1-200-K">complete description
     * of a field in this class or interface</a>. The fields table includes only those fields that are declared by this
     * class or interface. It does not include items representing fields that are inherited from superclasses or
     * superinterfaces.
     */
    public final List<Field> fields = new ArrayList<Field>();

    /**
     * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.1-200-M">complete description
     * of a method in this class or interface</a>. If neither of the ACC_NATIVE and ACC_ABSTRACT flags are set in
     * {@link Method#accessFlags}, the Java Virtual Machine instructions implementing the method are also supplied.
     * <p>
     * The {@link Method} structures represent all methods declared by this class or interface type, including instance
     * methods, class methods, instance initialization methods, and any class or interface initialization method.
     * The methods table does not include items representing methods that are inherited from superclasses or
     * superinterfaces.
     */
    public final List<Method> methods = new ArrayList<Method>();

    /** The optional {@code Deprecated} attribute of this class or interface. */
    @Nullable public DeprecatedAttribute deprecatedAttribute;

    /** The optional {@code EnclosingMethod} attribute of this class or interface. */
    @Nullable public EnclosingMethodAttribute enclosingMethodAttribute;

    /** The optional {@code InnerClasses} attribute of this class or interface. */
    @Nullable public InnerClassesAttribute innerClassesAttribute;

    /** The optional {@code RuntimeInvisibleAnnotations} attribute of this class or interface. */
    @Nullable public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;

    /** The optional {@code RuntimeVisibleAnnotations} attribute of this class or interface. */
    @Nullable public RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotationsAttribute;

    /** The optional {@code Signature} attribute of this class or interface. */
    @Nullable public SignatureAttribute signatureAttribute;

    /** The optional {@code Signature} attribute of this class or interface. */
    @Nullable public SourceFileAttribute sourceFileAttribute;

    /** The optional {@code Synthetic} attribute of this class or interface. */
    @Nullable public SyntheticAttribute syntheticAttribute;

    /** The other attributes of this class or interface. */
    public final List<Attribute> attributes = new ArrayList<Attribute>();

    // Class, nested class, field and method access and property flags.

    // CHECKSTYLE JavadocVariable:OFF
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
    // CHECKSTYLE JavadocVariable:ON

    public
    ClassFile(DataInputStream dis) throws IOException {

        // Magic number.
        {
            int magic = dis.readInt();
            if (magic != 0xcafebabe) {
                throw new ClassFileFormatException("Wrong magic number 0x" + Integer.toHexString(magic));
            }
        }

        // JDK version.
        this.minorVersion = dis.readShort();
        this.majorVersion = dis.readShort();

        // Load constant pool.
        this.constantPool = new ConstantPool(dis);

        // Access flags.
        this.accessFlags = dis.readShort();

        // Class name.
        this.thisClassName = this.constantPool.get(dis.readShort(), ConstantClassInfo.class).name;

        // Superclass.
        {
            ConstantClassInfo superclassCci = this.constantPool.getOptional(dis.readShort(), ConstantClassInfo.class);
            this.superClassName = superclassCci == null ? null : superclassCci.name;
        }

        // Implemented interfaces.
        for (short i = dis.readShort(); i > 0; --i) {
            this.interfaceNames.add(this.constantPool.get(dis.readShort(), ConstantClassInfo.class).name);
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
                    throw ioe2; // SUPPRESS CHECKSTYLE Avoid Hiding Cause of the Exception
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
                    throw ioe2; // SUPPRESS CHECKSTYLE Avoid Hiding Cause of the Exception
                } catch (RuntimeException re) {
                    throw new RuntimeException("Reading method #" + i + " of " + n + ": " + re.getMessage(), re);
                }
            }
        }

        // Class attributes.
        this.readAttributes(dis, new AbstractAttributeVisitor() {

            @Override public void
            visit(DeprecatedAttribute da) {
                ClassFile.this.deprecatedAttribute = da;
                ClassFile.this.attributes.add(da);
            }

            @Override public void
            visit(EnclosingMethodAttribute ema) {
                ClassFile.this.enclosingMethodAttribute = ema;
                ClassFile.this.attributes.add(ema);
            }

            @Override public void
            visit(InnerClassesAttribute ica) {
                ClassFile.this.innerClassesAttribute = ica;
                ClassFile.this.attributes.add(ica);
            }

            @Override public void
            visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                ClassFile.this.runtimeInvisibleAnnotationsAttribute = riaa;
                ClassFile.this.attributes.add(riaa);
            }

            @Override public void
            visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                ClassFile.this.runtimeVisibleAnnotationsAttribute = rvaa;
                ClassFile.this.attributes.add(rvaa);
            }

            @Override public void
            visit(SignatureAttribute sa) {
                ClassFile.this.signatureAttribute = sa;
                ClassFile.this.attributes.add(sa);
            }

            @Override public void
            visit(SourceFileAttribute sfa) {
                ClassFile.this.sourceFileAttribute = sfa;
                ClassFile.this.attributes.add(sfa);
            }

            @Override public void
            visit(SyntheticAttribute sa) {
                ClassFile.this.syntheticAttribute = sa;
                ClassFile.this.attributes.add(sa);
            }

            @Override public void
            visitOther(Attribute a) {
                ClassFile.this.attributes.add(a);
            }
        });
    }

    /** @return The major/minor version of this class file translated into a human-readable JDK name */
    public String
    getJdkName() {
        switch (this.majorVersion) {
        case 51:
            return "J2SE 7";
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
    public
    class Field {

        // CHECKSTYLE VariableCheck:OFF
        public short                                          accessFlags;
        public String                                         name;
        public String                                         descriptor;
        @Nullable public ConstantValueAttribute               constantValueAttribute;
        @Nullable public DeprecatedAttribute                  deprecatedAttribute;
        @Nullable public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
        @Nullable public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
        @Nullable public SignatureAttribute                   signatureAttribute;
        @Nullable public SyntheticAttribute                   syntheticAttribute;
        public final List<Attribute>                          attributes = new ArrayList<Attribute>();
        // CHECKSTYLE VariableCheck:ON

        public
        Field(DataInputStream dis) throws IOException {
            this.accessFlags = dis.readShort();
            this.name        = ClassFile.this.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            this.descriptor  = ClassFile.this.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;

            // Read field attributes.
            ClassFile.this.readAttributes(dis, new AbstractAttributeVisitor() {

                @Override public void
                visit(ConstantValueAttribute cva) {
                    Field.this.constantValueAttribute = cva;
                    Field.this.attributes.add(cva);
                }

                @Override public void
                visit(DeprecatedAttribute da) {
                    Field.this.deprecatedAttribute = da;
                    Field.this.attributes.add(da);
                }

                @Override public void
                visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                    Field.this.runtimeInvisibleAnnotationsAttribute = riaa;
                    Field.this.attributes.add(riaa);
                }

                @Override public void
                visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                    Field.this.runtimeVisibleAnnotationsAttribute = rvaa;
                    Field.this.attributes.add(rvaa);
                }

                @Override public void
                visit(SignatureAttribute sa) {
                    Field.this.signatureAttribute = sa;
                    Field.this.attributes.add(sa);
                }

                @Override public void
                visit(SyntheticAttribute sa) {
                    Field.this.syntheticAttribute = sa;
                    Field.this.attributes.add(sa);
                }

                @Override public void
                visitOther(Attribute ai) {
                    Field.this.attributes.add(ai);
                }
            });
        }
    }

    /** Representation of a method in a Java&trade; class file. */
    public
    class Method {

        // CHECKSTYLE VariableCheck:OFF
        public short                                                   accessFlags;
        public String                                                  name;
        public String                                                  descriptor;
        public final List<Attribute>                                   attributes = new ArrayList<Attribute>();
        @Nullable public AnnotationDefaultAttribute                    annotationDefaultAttribute;
        @Nullable public CodeAttribute                                 codeAttribute;
        @Nullable public DeprecatedAttribute                           deprecatedAttribute;
        @Nullable public ExceptionsAttribute                           exceptionsAttribute;
        @Nullable public RuntimeInvisibleAnnotationsAttribute          runtimeInvisibleAnnotationsAttribute;
        @Nullable public RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute;
        @Nullable public RuntimeVisibleAnnotationsAttribute            runtimeVisibleAnnotationsAttribute;
        @Nullable public RuntimeVisibleParameterAnnotationsAttribute   runtimeVisibleParameterAnnotationsAttribute;
        @Nullable public SignatureAttribute                            signatureAttribute;
        @Nullable public SyntheticAttribute                            syntheticAttribute;
        // CHECKSTYLE VariableCheck:ON

        public
        Method(DataInputStream dis) throws IOException {
            this.accessFlags = dis.readShort();
            this.name        = ClassFile.this.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            this.descriptor  = ClassFile.this.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;

            try {

                // Read method attributes.
                ClassFile.this.readAttributes(dis, new AbstractAttributeVisitor() {

                    @Override public void
                    visit(AnnotationDefaultAttribute ada) {
                        Method.this.annotationDefaultAttribute = ada;
                        Method.this.attributes.add(ada);
                    }

                    @Override public void
                    visit(CodeAttribute ca) {
                        Method.this.codeAttribute = ca;
                        Method.this.attributes.add(ca);
                    }

                    @Override public void
                    visit(DeprecatedAttribute da) {
                        Method.this.deprecatedAttribute = da;
                        Method.this.attributes.add(da);
                    }

                    @Override public void
                    visit(ExceptionsAttribute ea) {
                        Method.this.exceptionsAttribute = ea;
                        Method.this.attributes.add(ea);
                    }

                    @Override public void
                    visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                        Method.this.runtimeInvisibleAnnotationsAttribute = riaa;
                        Method.this.attributes.add(riaa);
                    }

                    @Override public void
                    visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
                        Method.this.runtimeInvisibleParameterAnnotationsAttribute = ripaa;
                        Method.this.attributes.add(ripaa);
                    }

                    @Override public void
                    visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                        Method.this.runtimeVisibleAnnotationsAttribute = rvaa;
                        Method.this.attributes.add(rvaa);
                    }

                    @Override public void
                    visit(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
                        Method.this.runtimeVisibleParameterAnnotationsAttribute = rvpaa;
                        Method.this.attributes.add(rvpaa);
                    }

                    @Override public void
                    visit(SignatureAttribute sa) {
                        Method.this.signatureAttribute = sa;
                        Method.this.attributes.add(sa);
                    }

                    @Override public void
                    visit(SyntheticAttribute sa) {
                        Method.this.syntheticAttribute = sa;
                        Method.this.attributes.add(sa);
                    }

                    @Override public void
                    visitOther(Attribute ai) {
                        Method.this.attributes.add(ai);
                    }
                });
            } catch (IOException ioe) {
                IOException ioe2 = new IOException(
                    "Parsing method '" + this.name + "' [" + this.descriptor + "]: " + ioe.getMessage()
                );
                ioe2.initCause(ioe);
                throw ioe2; // SUPPRESS CHECKSTYLE AvoidHidingCause
            } catch (RuntimeException re) {
                throw new RuntimeException(
                    "Parsing method '" + this.name + "' [" + this.descriptor + "]: " + re.getMessage(),
                    re
                );
            }
        }
    }

    /** Representation of an attribute in a Java&trade; class file. */
    public
    interface Attribute {

        /** Accepts the {@code visitor}. */
        void accept(AttributeVisitor visitor);

        /** @return This attribute's name */
        String getName();
    }

    /** Reads a set of attributs and has them accept the {@code visitor}. */
    final void
    readAttributes(DataInputStream dis, AttributeVisitor visitor) throws IOException {
        short n = dis.readShort();
        for (int i = 0; i < n; ++i) {
            try {
                this.readAttribute(dis, visitor);
            } catch (IOException ioe) {
                IOException ioe2 = new IOException("Reading attribute #" + i + " of " + n + ": " + ioe.getMessage());
                ioe2.initCause(ioe);
                throw ioe2; // SUPPRESS CHECKSTYLE AvoidHidingCause
            } catch (RuntimeException re) {
                throw new RuntimeException("Reading attribute #" + i + " of " + n + ": " + re.getMessage(), re);
            }
        }
    }

    private void
    readAttribute(DataInputStream dis, AttributeVisitor visitor) throws IOException {

        String attributeName = this.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;

        try {
            // Read attribute body into byte array and create a DataInputStream.
            ByteArrayInputStream bais;
            {
                int          attributeLength = dis.readInt();
                final byte[] ba              = new byte[attributeLength];
                dis.readFully(ba);
                bais = new ByteArrayInputStream(ba);
            }

            // Parse the attribute body.
            this.readAttributeBody(attributeName, new DataInputStream(bais), visitor);

            // Check for extraneous bytes.
            {
                int av = bais.available();
                if (av > 0) {
                    throw new RuntimeException(av + " extraneous bytes in attribute body");
                }
            }
        } catch (IOException ioe) {
            IOException ioe2 = new IOException("Reading attribute '" + attributeName + "': " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2; // SUPPRESS CHECKSTYLE AvoidHidingCause
        } catch (RuntimeException re) {
            throw new RuntimeException("Reading attribute '" + attributeName + "': " + re.getMessage(), re);
        }
    }

    private void
    readAttributeBody(
        final String          attributeName,
        final DataInputStream dis,
        AttributeVisitor      visitor
    ) throws IOException {
        if ("AnnotationDefault".equals(attributeName)) {
            visitor.visit(new AnnotationDefaultAttribute(dis, this));
        } else
        if ("ConstantValue".equals(attributeName)) {
            visitor.visit(new ConstantValueAttribute(dis, this));
        } else
        if ("Code".equals(attributeName)) {
            visitor.visit(new CodeAttribute(dis, this));
        } else
        if ("Deprecated".equals(attributeName)) {
            visitor.visit(new DeprecatedAttribute(dis, this));
        } else
        if ("EnclosingMethod".equals(attributeName)) {
            visitor.visit(new EnclosingMethodAttribute(dis, this));
        } else
        if ("Exceptions".equals(attributeName)) {
            visitor.visit(new ExceptionsAttribute(dis, this));
        } else
        if ("InnerClasses".equals(attributeName)) {
            visitor.visit(new InnerClassesAttribute(dis, this));
        } else
        if ("LineNumberTable".equals(attributeName)) {
            visitor.visit(new LineNumberTableAttribute(dis, this));
        } else
        if ("LocalVariableTable".equals(attributeName)) {
            visitor.visit(new LocalVariableTableAttribute(dis, this));
        } else
        if ("LocalVariableTypeTable".equals(attributeName)) {
            visitor.visit(new LocalVariableTypeTableAttribute(dis, this));
        } else
        if ("RuntimeInvisibleAnnotations".equals(attributeName)) {
            visitor.visit(new RuntimeInvisibleAnnotationsAttribute(dis, this));
        } else
        if ("RuntimeInvisibleParameterAnnotations".equals(attributeName)) {
            visitor.visit(new RuntimeInvisibleParameterAnnotationsAttribute(dis, this));
        } else
        if ("RuntimeVisibleAnnotations".equals(attributeName)) {
            visitor.visit(new RuntimeVisibleAnnotationsAttribute(dis, this));
        } else
        if ("RuntimeVisibleParameterAnnotations".equals(attributeName)) {
            visitor.visit(new RuntimeVisibleParameterAnnotationsAttribute(dis, this));
        } else
        if ("Signature".equals(attributeName)) {
            visitor.visit(new SignatureAttribute(dis, this));
        } else
        if ("SourceFile".equals(attributeName)) {
            visitor.visit(new SourceFileAttribute(dis, this));
        } else
        if ("Synthetic".equals(attributeName)) {
            visitor.visit(new SyntheticAttribute(dis, this));
        } else
        {
            visitor.visit(new UnknownAttribute(attributeName, dis, this));
        }
    }

    /** The visitor for {@Attribute}. */
    public
    interface AttributeVisitor {

        // CHECKSTYLE MethodCheck:OFF
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
        // CHECKSTYLE MethodCheck:ON

        /** An unknown attribute accepted this visitor. */
        void visit(UnknownAttribute unknownAttribute);
    }

    /** Default implementation of the {@link AttributeVisitor}. */
    public abstract static
    class AbstractAttributeVisitor implements AttributeVisitor {

        /** Called by the default implementations of the {@code visit(...)} methods. */
        public abstract void visitOther(Attribute ai);

        @Override public void visit(AnnotationDefaultAttribute                    ada)   { this.visitOther(ada); }
        @Override public void visit(CodeAttribute                                 ca)    { this.visitOther(ca); }
        @Override public void visit(ConstantValueAttribute                        cva)   { this.visitOther(cva); }
        @Override public void visit(DeprecatedAttribute                           da)    { this.visitOther(da); }
        @Override public void visit(EnclosingMethodAttribute                      ema)   { this.visitOther(ema); }
        @Override public void visit(ExceptionsAttribute                           ea)    { this.visitOther(ea); }
        @Override public void visit(InnerClassesAttribute                         ica)   { this.visitOther(ica); }
        @Override public void visit(LineNumberTableAttribute                      lnta)  { this.visitOther(lnta); }
        @Override public void visit(LocalVariableTableAttribute                   lvta)  { this.visitOther(lvta); }
        @Override public void visit(LocalVariableTypeTableAttribute               lvtta) { this.visitOther(lvtta); }
        @Override public void visit(RuntimeInvisibleAnnotationsAttribute          riaa)  { this.visitOther(riaa); }
        @Override public void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) { this.visitOther(ripaa); }
        @Override public void visit(RuntimeVisibleAnnotationsAttribute            rvaa)  { this.visitOther(rvaa); }
        @Override public void visit(RuntimeVisibleParameterAnnotationsAttribute   rvpaa) { this.visitOther(rvpaa); }
        @Override public void visit(SignatureAttribute                            sa)    { this.visitOther(sa); }
        @Override public void visit(SourceFileAttribute                           sfa)   { this.visitOther(sfa); }
        @Override public void visit(SyntheticAttribute                            sa)    { this.visitOther(sa); }
        @Override public void visit(UnknownAttribute                              a)     { this.visitOther(a); }
    }

    /** Representation of an attribute with an unknown name. */
    public static final
    class UnknownAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7-120">name of the
         * attribute</a>.
         */
        public final String name;

        /** The (unstructured) attribute information. */
        public byte[] info;

        UnknownAttribute(String name, DataInputStream dis, ClassFile cf) throws IOException {
            this.name = name;
            this.info = readByteArray(dis, dis.available());
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return this.name; }
    }

    /** Representation of a {@code Synthetic} attribute. */
    public static
    class SyntheticAttribute implements Attribute {
        public SyntheticAttribute(DataInputStream dis, ClassFile cf) {}

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "Synthetic"; }
    }

    /** Representation of a {@code Deprecated} attribute. */
    public static
    class DeprecatedAttribute implements Attribute {
        public DeprecatedAttribute(DataInputStream dis, ClassFile cf) {}

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "Deprecated"; }
    }

    /** Representation of a {@code InnerClasses} attribute. */
    public static final
    class InnerClassesAttribute implements Attribute {

        /** Helper class for {@link InnerClassesAttribute}. */
        public static
        class ClasS {

            /**
             * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.6-300-D.2-1">inner
             * class</a> that is described by the remaining items.
             */
            public final ConstantClassInfo innerClassInfo;

            /**
             * The class or interface of which the {@link #innerClassInfo inner class} is a member.
             * <p>
             * {@code null} == top-level type or anonymous class
             */
            @Nullable public final ConstantClassInfo outerClassInfo;

            /**
             * The original simple name of the {@link #innerClassInfo inner class}.
             * <p>
             * {@code null} == anonymous
             */
            @Nullable public final ConstantUtf8Info innerName;

            /**
             * A mask of flags used to denote access permissions to and properties of the {@link #innerClassInfo inner
             * class}.
             */
            public final short innerClassAccessFlags;

            public
            ClasS(DataInputStream dis, ClassFile cf) throws IOException {
                this.innerClassInfo        = cf.constantPool.get(dis.readShort(), ConstantClassInfo.class);
                this.outerClassInfo        = cf.constantPool.getOptional(dis.readShort(), ConstantClassInfo.class);
                this.innerName             = cf.constantPool.getOptional(dis.readShort(), ConstantUtf8Info.class);
                this.innerClassAccessFlags = dis.readShort();
            }
        }

        /** The inner/outer class relationship relevant for this class. */
        public final List<ClasS> classes = new ArrayList<ClasS>();

        InnerClassesAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.classes.add(new ClasS(dis, cf));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "InnerClasses"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16">{@code
     * RuntimeVisibleAnnotations} attribute</a>.
     * */
    public static
    class RuntimeVisibleAnnotationsAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D">annotations
         * table<a>.
         */
        public final List<Annotation> annotations = new ArrayList<Annotation>();

        /** Reads and populates this object from the given {@link DataInputStream}. */
        RuntimeVisibleAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = 0xffff & dis.readShort(); i > 0; --i) {
                this.annotations.add(new Annotation(dis, cf));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "RuntimeVisibleAnnotations"; }
    }

    /** Representation of the {@code RuntimeInvisibleAnnotations} attribute. */
    public static
    class RuntimeInvisibleAnnotationsAttribute extends RuntimeVisibleAnnotationsAttribute {

        public
        RuntimeInvisibleAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        @Override public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    /** Helper class for {@link RuntimeVisibleParameterAnnotationsAttribute}. */
    public static
    class ParameterAnnotation {

        /**
         * Each value represents a single run-time-visible annotation on the parameter corresponding to the sequence
         * number of this {@link ParameterAnnotation}.
         */
        public final List<Annotation> annotations = new ArrayList<Annotation>();

        public
        ParameterAnnotation(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.annotations.add(new Annotation(dis, cf));
            }
        }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.18">{@code
     * RuntimeVisibleParameterAnnotations} attribute</a>.
     */
    public static
    class RuntimeVisibleParameterAnnotationsAttribute implements Attribute {

        /**
         *  All of the <a
         *  href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.18-300-D">run-time-visible
         *  annotations on all parameters</a>.
         */
        public final List<ParameterAnnotation> parameterAnnotations = new ArrayList<ParameterAnnotation>();

        RuntimeVisibleParameterAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readByte(); i > 0; --i) {
                this.parameterAnnotations.add(new ParameterAnnotation(dis, cf));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "RuntimeVisibleParameterAnnotations"; }
    }

    /** Representation of the {@code RuntimeInvisibleParameterAnnotations} attribute. */
    public static
    class RuntimeInvisibleParameterAnnotationsAttribute
    extends RuntimeVisibleParameterAnnotationsAttribute {

        public
        RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        @Override public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.20">{@code
     * AnnotationDefault} attribute</a>.
     */
    public static
    class AnnotationDefaultAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.20-300-C">default value
         * of the annotation type element</a> whose default value is represented by this {@link
         * AnnotationDefaultAttribute}.
         */
        public ElementValue defaultValue;

        AnnotationDefaultAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.defaultValue = newElementValue(dis, cf);
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "AnnotationDefault"; }
    }

    /**
     * Helper class for the {@code Runtime*visible*Annotations} attributes.
     * <p>
     * Represents a single <a
     * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D">run-time-visible
     * annotation</a> on a program element.
     */
    public static
    class Annotation {

        /**
         * Helper class for the {@code Runtime*visible*Annotations} attributes.
         * <p>
         * Represents a single <a
         * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D.2-3">element-value
         * pair</a> in the annotation represented by this {@link Annotation}.
         */
        public static
        class ElementValuePair {

            /**
             * A valid field descriptor that denotes the <a
             * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D.2-3-1">name of the
             * annotation type element</a> represented by this {@link ElementValuePair}.
             */
            public final String elementName;

            /**
             * The <a
             * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D.2-3-2">value of
             * the value item represents the value of the element-value pair</a> represented by this {@link
             * ElementValuePair}.
             */
            public final ElementValue elementValue;

            public
            ElementValuePair(DataInputStream dis, ClassFile cf) throws IOException {
                this.elementName  = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
                this.elementValue = newElementValue(dis, cf);
            }

            @Override public String
            toString() {
                return (
                    "value".equals(this.elementName)
                    ? this.elementValue.toString()
                    : this.elementName + " = " + this.elementValue.toString()
                );
            }
        }

        /**
         * A <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.16-300-D.2-1">field
         * descriptor representing the annotation type</a> corresponding to this {@link Annotation}.
         */
        public String typeName;

        /** Each value of this list represents a single element-value pair in the {@link Annotation}. */
        public final List<ElementValuePair> elementValuePairs = new ArrayList<ElementValuePair>();

        public
        Annotation(DataInputStream dis, ClassFile cf) throws IOException {
            short typeIndex = dis.readShort();
            try {
                this.typeName = SignatureParser.decodeFieldDescriptor(
                    cf.constantPool.get(typeIndex, ConstantUtf8Info.class).bytes
                ).toString();
            } catch (SignatureException e) {
                throw new ClassFileFormatException("Decoding annotation type: " + e.getMessage(), e);
            }
            for (int i = dis.readShort(); i > 0; --i) {
                this.elementValuePairs.add(new ElementValuePair(dis, cf));
            }
        }

        @Override public String
        toString() {
            StringBuilder sb = new StringBuilder("@").append(this.typeName);
            if (!this.elementValuePairs.isEmpty()) {
                Iterator<ElementValuePair> it = this.elementValuePairs.iterator();
                sb.append('(').append(it.next());
                while (it.hasNext()) sb.append(", ").append(it.next());
                return sb.append(')').toString();
            }
            return sb.toString();
        }
    }

    /** Representation of an annotation element value. */
    public
    interface ElementValue {
    }

    /** Reads one {@link ElementValue} from the given {@link DataInputStream}. */
    static ElementValue
    newElementValue(DataInputStream dis, ClassFile cf) throws IOException {
        final byte tag = dis.readByte();
        if ("BCDFIJSZ".indexOf(tag) != -1) {
            final String s = cf.constantPool.getIntegerFloatLongDouble(dis.readShort());
            return new ElementValue() { @Override public String toString() { return s; } };
        } else
        if (tag == 's') {
            final String s = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            return new ElementValue() {
                @Override public String toString() { return ConstantPool.stringToJavaLiteral(s); }
            };
        } else
        if (tag == 'e') {
            String typeName  = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            String constName = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            try {
                final String s = SignatureParser.decodeFieldDescriptor(typeName) + "." + constName;
                return new ElementValue() { @Override public String toString() { return s; } };
            } catch (SignatureException se) {
                throw new ClassFileFormatException("Decoding enum constant element value: " + se.getMessage(), se);
            }
        } else
        if (tag == 'c') {
            final String classInfo = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
            try {
                final String s = SignatureParser.decodeReturnType(classInfo) + ".class";
                return new ElementValue() { @Override public String toString() { return s; } };
            } catch (SignatureException se) {
                throw new ClassFileFormatException("Decoding class element value: " + se.getMessage(), se);
            }
        } else
        if (tag == '@') {
            final Annotation annotation = new Annotation(dis, cf);
            return new ElementValue() { @Override public String toString() { return annotation.toString(); } };
        } else
        if (tag == '[') {
            final List<ElementValue> values = new ArrayList<ElementValue>();
            for (int i = dis.readShort(); i > 0; --i) {
                values.add(newElementValue(dis, cf));
            }
            return new ElementValue() {

                @Override public String
                toString() {
                    Iterator<ElementValue> it = values.iterator();
                    if (!it.hasNext()) return "{}";

                    ElementValue firstValue = it.next();
                    if (!it.hasNext()) return firstValue.toString();

                    StringBuilder sb = new StringBuilder("{ ").append(firstValue.toString());
                    do {
                        sb.append(", ").append(it.next().toString());
                    } while (it.hasNext());
                    return sb.append(" }").toString();
                }
            };
        } else
        {
            return new ElementValue() {
                @Override public String toString() { return "[Invalid element value tag '" + (char) tag + "']"; }
            };
        }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.9">{@code
     * Signature} attribute</a>.
     */
    public static final
    class SignatureAttribute implements Attribute {

        /**
         * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.9-300-C">A class signature
         * if this {@link SignatureAttribute} is an attribute of a {@link ClassFile}; a method signature if this
         * {@link SignatureAttribute} attribute is an attribute of a {@link Method}; or a field type signature
         * otherwise</a>.
         */
        public final String signature;

        SignatureAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.signature = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "Signature"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.7">{@code
     * EnclosingMethod} attribute</a>.
     */
    public static final
    class EnclosingMethodAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.7-300-C">innermost class
         * that encloses the declaration of the current class</a>.
         */
        public ConstantClassInfo clasS;

        /** null == not enclosed by a constructor or a method, i.e. a field initializer */
        @Nullable public ConstantNameAndTypeInfo method;

        EnclosingMethodAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.clasS  = cf.constantPool.get(dis.readShort(), ConstantClassInfo.class);
            this.method = cf.constantPool.getOptional(dis.readShort(), ConstantNameAndTypeInfo.class);
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "EnclosingMethod"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.5">{@code
     * Exceptions} attribute</a>.
     */
    public static final
    class ExceptionsAttribute implements Attribute {

        /**
         * Each value of the list represents a <a
         * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.5-300-D">class type that this
         * method is declared to throw</a>.
         */
        public final List<ConstantClassInfo> exceptionNames = new ArrayList<ConstantClassInfo>();

        ExceptionsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.exceptionNames.add(cf.constantPool.get(dis.readShort(), ConstantClassInfo.class));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "Exceptions"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3">{@code
     * Code} attribute</a>.
     */
    public static final
    class CodeAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-C">maximum depth
         * of the operand stack of this method at any point during execution of the method</a>.
         */
        public final short maxStack;

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-D">number of local
         * variables in the local variable array allocated upon invocation of this method, including the local
         * variables used to pass parameters to the method on its invocation</a>.
         */
        public final short maxLocals;

        /**
         * Gives <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-F">the actual
         * bytes of Java Virtual Machine code that implement the method</a>.
         */
        public final byte[] code;

        /**
         * Each entry in the list describes <a
         * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-H">one exception handler
         * in the code array</a>. The order of the handlers in the exception_table array is significant.
         */
        public final List<ExceptionTableEntry> exceptionTable = new ArrayList<ExceptionTableEntry>();

        /** The {@code Code} attribute's optional {@code LocalVariableTable} attribute. */
        @Nullable public LocalVariableTableAttribute localVariableTableAttribute;

        /** The {@code Code} attribute's optional {@code LocalVariableTypeTable} attribute. */
        @Nullable public LocalVariableTypeTableAttribute localVariableTypeTableAttribute;

        /** The {@code Code} attribute's optional {@code LineNumberTable} attribute. */
        @Nullable public LineNumberTableAttribute lineNumberTableAttribute;

        /** The attributes of the {@code Code} attribute. */
        public final List<Attribute> attributes = new ArrayList<Attribute>();

        CodeAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.maxStack  = dis.readShort();
            this.maxLocals = dis.readShort();

            // Byte code.
            this.code = readByteArray(dis, dis.readInt());

            // Exception table.
            for (short i = dis.readShort(); i > 0; --i) {
                this.exceptionTable.add(new ExceptionTableEntry(dis, cf));
            }

            // Code attributes.
            cf.readAttributes(dis, new AbstractAttributeVisitor() {

                @Override public void
                visit(LineNumberTableAttribute lnta) {
                    CodeAttribute.this.lineNumberTableAttribute = lnta;
                    CodeAttribute.this.attributes.add(lnta);
                }

                @Override public void
                visit(LocalVariableTableAttribute lvta) {
                    CodeAttribute.this.localVariableTableAttribute = lvta;
                    CodeAttribute.this.attributes.add(lvta);
                }
                @Override public void
                visit(LocalVariableTypeTableAttribute lvtta) {
                    CodeAttribute.this.localVariableTypeTableAttribute = lvtta;
                    CodeAttribute.this.attributes.add(lvtta);
                }

                @Override public void
                visitOther(Attribute ai) {
                    CodeAttribute.this.attributes.add(ai);
                }
            });
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "Code"; }
    }

    /**
     * Helper class for {@link CodeAttribute}.
     * <p>
     * Describes one exception handler in the code array.
     */
    public static
    class ExceptionTableEntry {

        /**
         * The values <a
         * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-H.1-1">indicate the range
         * in the code array at which the exception handler is active</a>. The value of {@link #startPc} must be a
         * valid index into the code array of the opcode of an instruction. The value of {@link #endPc} either must be
         * a valid index into the code array of the opcode of an instruction or must be equal to {@code code.length},
         * the length of the code array. The value of {@link #startPc} must be less than the value of {@link #endPc}.
         * <p>
         * The {@link #startPc} is inclusive and {@link #endPc} is exclusive; that is, the exception handler must be
         * active while the program counter is within the interval [{@link #startPc}, {@link #endPc}).
         */
        public int startPc, endPc;

        /**
         * The value <a
         * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-H.1-2">indicates the
         * start of the exception handler</a>. The value of the item must be a valid index into the code array and must
         * be the index of the opcode of an instruction.
         */
        public int handlerPc;

        /**
         * A <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3-300-H.1-3">class of
         * exceptions that this exception handler is designated to catch</a>. The exception handler will be called only
         * if the thrown exception is an instance of the given class or one of its subclasses.
         * <p>
         * If the value is {@code null}, this exception handler is called for all exceptions. This is used to implement
         * {@code finally}
         */
        @Nullable public ConstantClassInfo catchType;

        ExceptionTableEntry(DataInputStream dis, ClassFile cf) throws IOException {
            this.startPc   = 0xffff & dis.readShort();
            this.endPc     = 0xffff & dis.readShort();
            this.handlerPc = 0xffff & dis.readShort();
            this.catchType = cf.constantPool.getOptional(dis.readShort(), ConstantClassInfo.class);
        }

        @Override public String
        toString() {
            return (
                "startPC="
                + this.startPc
                + " endPC="
                + this.endPc
                + " handlerPC="
                + this.handlerPc
                + " catchType="
                + this.catchType
            );
        }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.10">{@code
     * SourceFile} attribute</a>.
     */
    public static
    class SourceFileAttribute implements Attribute {

        /**
         * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.10-300-C">name of the
         * source file from which this class file was compiled</a>. It will not be interpreted as indicating the name
         * of a directory containing the file or an absolute path name for the file; such platform-specific additional
         * information must be supplied by the run-time interpreter or development tool at the time the file name is
         * actually used.
         */
        public String sourceFile;

        SourceFileAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.sourceFile = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "SourceFile"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.12">{@code
     * LineNumberTable} attribute</a>.
     */
    public
    class LineNumberTableAttribute implements Attribute {

        /**
         * Each entry in the list indicates that the line number in the original source file changes at a given point
         * in the code array.
         */
        public final List<LineNumberTableEntry> entries = new ArrayList<LineNumberTableEntry>();

        LineNumberTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                this.entries.add(new LineNumberTableEntry(dis));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "LineNumberTable"; }
    }

    /** Helper class for {@link LineNumberTableAttribute}. */
    public static
    class LineNumberTableEntry {

        /**
         * The value must indicate the index into the code array at which the code for a new line in the original
         * source file begins. The value must be less than {@link CodeAttribute#code CodeAttribute.code.length}.
         */
        public int startPc;

        /** The value must give the corresponding line number in the original source file. */
        public int lineNumber;

        LineNumberTableEntry(DataInputStream dis) throws IOException {
            this.startPc    = 0xffff & dis.readShort();
            this.lineNumber = 0xffff & dis.readShort();
        }
    }

    /**
     * The <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.13">LocalVariableTable
     * attribute</a> is an optional variable-length attribute in the attributes table of a {@link CodeAttribute}. It
     * may be used by debuggers to determine the value of a given local variable during the execution of a method.
     * <p>
     * If {@link LocalVariableTableAttribute}s are present in the attributes table of a given {@link CodeAttribute},
     * then they may appear in any order. There may be no more than one {@link LocalVariableTableAttribute} per local
     * variable in the {@link CodeAttribute}.
     */
    public
    class LocalVariableTableAttribute implements Attribute {

        /** Helper class for {@link LocalVariableTableAttribute}. */
        class Entry {

            /**
             * The given local variable must have a value at indices into the code array in the interval [{@link
             * #startPC}, {@link #startPC} + {@link #length}), that is, between {@link #startPC} inclusive and {@link
             * #startPC} + {@link #length} exclusive.
             * <p>
             * The value of {@link #startPC} must be a valid index into the code array of this {@link CodeAttribute}
             * and must be the index of the opcode of an instruction.
             * <p>
             * The value of {@link #startPC} + {@link #length} must either be a valid index into the code array of this
             * {@link CodeAttribute} and be the index of the opcode of an instruction, or it must be the first index
             * beyond the end of that code array.
             */
            public final short startPC, length;

            /** Represents a valid unqualified name denoting a local variable. */
            public final String name;

            /** Representation of a field descriptor encoding the type of a local variable in the source program. */
            public final String descriptor;

            /**
             * The given local variable must be at {@link #index} in the local variable array of the current frame. If
             * the local variable at index is of type double or long, it occupies both {@link #index} and {@link
             * #index} + 1.
             */
            public final short index;

            Entry(DataInputStream dis, ClassFile cf) throws IOException {
                this.startPC    = dis.readShort();
                this.length     = dis.readShort();
                this.name       = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
                this.descriptor = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
                this.index      = dis.readShort();
            }
        }

        /**
         * Each entry in the list indicates a range of code array offsets within which a local variable has a value.
         * It also indicates the index into the local variable array of the current frame at which that local variable
         * can be found.
         */
        public final List<Entry> entries = new ArrayList<Entry>();

        LocalVariableTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                this.entries.add(new Entry(dis, cf));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "LocalVariableTable"; }
    }

    /**
     * Representation of the <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.14">{@code
     * LocalVariableTypeTable} attribute</a>.
     */
    public static
    class LocalVariableTypeTableAttribute implements Attribute {

        /**
         * Indicates a range of code array offsets within which a local variable has a value. It also indicates the
         * index into the local variable array of the current frame at which that local variable can be found.
         */
        public static
        class Entry {

            /**
             * The given local variable must have a value at indices into the code array in the interval [{@link
             * #startPC}, {@link #startPC} + {@link #length}), that is, between {@link #startPC} inclusive and
             * {@link #startPC} + {@link #length} exclusive.
             * <p>
             * The value of {@link #startPC} must be a valid index into the code array of this {@link CodeAttribute}
             * and must be the index of the opcode of an instruction.
             * <p>
             * The value of {@link #startPC} + {@link #length} must either be a valid index into the code array of this
             * {@link CodeAttribute} and be the index of the opcode of an instruction, or it must be the first index
             * beyond the end of that code array.
             */
            public final int startPC, length;

            /** Represents a valid unqualified name denoting a local variable. */
            public final String name;

            /**
             * Representation of a field type signature encoding the type of a local variable in the source program.
             */
            public final String signature;

            /**
             * The given local variable must be at {@link #index} in the local variable array of the current frame. If
             * the local variable at {@link #index} is of type double or long, it occupies both {@link #index} and
             * {@link #index} + 1.
             */
            public final short index;

            Entry(DataInputStream dis, ClassFile cf) throws IOException {
                this.startPC   = 0xffff & dis.readShort();
                this.length    = 0xffff & dis.readShort();
                this.name      = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
                this.signature = cf.constantPool.get(dis.readShort(), ConstantUtf8Info.class).bytes;
                this.index     = dis.readShort();
            }
        }

        /**
         * Each entry in the list indicates a range of code array offsets within which a local variable has a value.
         * It also indicates the index into the local variable array of the current frame at which that local variable
         * can be found.
         */
        public final List<Entry> entries = new ArrayList<Entry>();

        LocalVariableTypeTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                this.entries.add(new Entry(dis, cf));
            }
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "LocalVariableTypeTable"; }
    }

    /**
     * Representation of the <a
     * href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.2-100">{@code ConstantValue}
     * attribute</a>.
     */
    public static final
    class ConstantValueAttribute implements Attribute {

        /**
         * Gives the constant value represented by this attribute. The constant pool entry must be of a type
         * appropriate to the field.
         */
        public final String constantValue;

        ConstantValueAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.constantValue = cf.constantPool.getIntegerFloatLongDoubleString(dis.readShort());
        }

        @Override public void   accept(AttributeVisitor visitor) { visitor.visit(this); }
        @Override public String getName()                        { return "ConstantValue"; }
    }

    private static byte[]
    readByteArray(DataInputStream dis, int size) throws IOException {
        byte[] res = new byte[size];
        dis.readFully(res);
        return res;
    }
}
