
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
    public EnclosingMethodAttribute             enclosingMethodAttribute;
    public InnerClassesAttribute                innerClassesAttribute;
    public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
    public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
    public SignatureAttribute                   signatureAttribute;
    public SourceFileAttribute                  sourceFileAttribute;
    public SyntheticAttribute                   syntheticAttribute;
    public final List<Attribute>                attributes = new ArrayList<Attribute>();

    public ClassFile(DataInputStream dis) throws IOException {

        // Magic number.
        if (dis.readInt() != 0xcafebabe) {
            throw new IOException("Wrong magic number - this is not a valid class file");
        }

        // JDK version.
        minorVersion = dis.readShort();
        majorVersion = dis.readShort();

        // Load constant pool.
        constantPool = new ConstantPool(dis);

        accessFlags = dis.readShort();
        thisClassName = constantPool.getConstantClassInfo(dis.readShort()).name;
        superClassName = constantPool.getConstantClassInfo(dis.readShort()).name;

        // Implemented interfaces.
        for (short i = dis.readShort(); i > 0; --i) {
            interfaceNames.add(constantPool.getConstantClassInfo(dis.readShort()).name);
        }

        // Fields.
        for (short i = dis.readShort(); i > 0; --i) {
           fields.add(new Field(dis));
        }

        // Methods.
        for (short i = dis.readShort(); i > 0; --i) {
            methods.add(new Method(dis));
        }

        // Class attributes.
        readAttributes(dis, new AbstractAttributeVisitor() {

            public void visit(EnclosingMethodAttribute ema) {
                enclosingMethodAttribute = ema;
                attributes.add(ema);
            }

            public void visit(InnerClassesAttribute ica) {
                innerClassesAttribute = ica;
                attributes.add(ica);
            }

            public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                runtimeInvisibleAnnotationsAttribute = riaa;
                attributes.add(riaa);
            }

            public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                runtimeVisibleAnnotationsAttribute = rvaa;
                attributes.add(rvaa);
            }

            public void visit(SignatureAttribute sa) {
                signatureAttribute = sa;
                attributes.add(sa);
            }

            public void visit(SourceFileAttribute sfa) {
                sourceFileAttribute = sfa;
                attributes.add(sfa);
            }

            public void visit(SyntheticAttribute sa) {
                syntheticAttribute = sa;
                attributes.add(sa);
            }

            public void acceptOther(Attribute a) {
                attributes.add(a);
            }
        });
    }

    public String getJdkName() {
        switch (majorVersion) {
        case 50: return "J2SE 6.0";
        case 49: return "J2SE 5.0";
        case 48: return "JDK 1.4";
        case 47: return "JDK 1.3";
        case 46: return "JDK 1.2";
        case 45: return "JDK 1.1";
        default:
            return "Major/minor version " + majorVersion + "/" + minorVersion + " does not match any known JDK";
        }
    }

    public static class SyntheticAttribute implements Attribute {
        public SyntheticAttribute(DataInputStream dis, ClassFile cf) {
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class DeprecatedAttribute implements Attribute {
        public DeprecatedAttribute(DataInputStream dis, ClassFile cf) {
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class ClasS {
        public ConstantClassInfo innerClassInfo;
        public ConstantClassInfo outerClassInfo;
        public ConstantUtf8Info  innerName;
        public short             innerClassAccessFlags;

        public ClasS(DataInputStream dis, ClassFile cf) throws IOException {
            innerClassInfo = cf.constantPool.getConstantClassInfo(dis.readShort());
            outerClassInfo = cf.constantPool.getConstantClassInfo(dis.readShort());
            innerName = cf.constantPool.getConstantUtf8Info(dis.readShort());
            innerClassAccessFlags = dis.readShort();
        }
    }
    public static final class InnerClassesAttribute implements Attribute {
        public final List<ClasS> classes = new ArrayList<ClasS>();

        private InnerClassesAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                classes.add(new ClasS(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class Annotation {
        public static class ElementValuePair {

            public String       elementName;
            public ElementValue elementValue;

            public ElementValuePair(DataInputStream dis, ClassFile cf) throws IOException {
                this.elementName = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes; // elementNameIndex
                this.elementValue = newElementValue(dis, cf); 
            }

            public String toString() {
                return (
                    "value".equals(elementName)
                    ? this.elementValue.toString()
                    : elementName + " = " + this.elementValue.toString()
                );
            }
        }

        public String                       typeName;
        public final List<ElementValuePair> elementValuePairs = new ArrayList<ElementValuePair>();

        public Annotation(DataInputStream dis, ClassFile cf) throws IOException {
            short typeIndex = dis.readShort();
            try {
                this.typeName = SignatureParser.decodeFieldDescriptor(cf.constantPool.getConstantUtf8Info(typeIndex).bytes).toString();
            } catch (SignatureException e) {
                IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
            for (int i = dis.readShort(); i > 0; --i) {
                elementValuePairs.add(new ElementValuePair(dis, cf));
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

    public static abstract class ElementValue {
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
            return new ElementValue() { public String toString() { return "[Invalid element value tag '" + (char) tag + "']"; }};
        }
    }

    public static class RuntimeVisibleAnnotationsAttribute implements Attribute {

        public final List<Annotation> annotations = new ArrayList<Annotation>();
        
        private RuntimeVisibleAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                annotations.add(new Annotation(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class RuntimeInvisibleAnnotationsAttribute extends RuntimeVisibleAnnotationsAttribute {
        public RuntimeInvisibleAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class ParameterAnnotation {
        public final List<Annotation> annotations = new ArrayList<Annotation>();

        public ParameterAnnotation(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                annotations.add(new Annotation(dis, cf));
            }
        }
    }

    public static class RuntimeVisibleParameterAnnotationsAttribute implements Attribute {

        public final List<ParameterAnnotation> parameterAnnotations = new ArrayList<ParameterAnnotation>();

        private RuntimeVisibleParameterAnnotationsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readByte(); i > 0; --i) {
                parameterAnnotations.add(new ParameterAnnotation(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    public static class RuntimeInvisibleParameterAnnotationsAttribute extends RuntimeVisibleParameterAnnotationsAttribute {
        public RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    
    public static class AnnotationDefaultAttribute implements Attribute {

        public ElementValue defaultValue;
        
        private AnnotationDefaultAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.defaultValue = newElementValue(dis, cf);
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public interface AttributeVisitor {
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
    
    public class Field {
        public short                                accessFlags;
        public String                               name;
        public String                               descriptor;
        public ConstantValueAttribute               constantValueAttribute;
        public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
        public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
        public SignatureAttribute                   signatureAttribute;
        public SyntheticAttribute                   syntheticAttribute;
        public final List<Attribute>                attributes = new ArrayList<Attribute>();

        public Field(DataInputStream dis) throws IOException {
            accessFlags = dis.readShort();
            name = constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            descriptor = constantPool.getConstantUtf8Info(dis.readShort()).bytes;

            readAttributes(dis, new AbstractAttributeVisitor() {
                public void visit(ConstantValueAttribute cva) {
                    constantValueAttribute = cva;
                    attributes.add(cva);
                }
                public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                    runtimeInvisibleAnnotationsAttribute = riaa;
                    attributes.add(riaa);
                }
                public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                    runtimeVisibleAnnotationsAttribute = rvaa;
                    attributes.add(rvaa);
                }
                public void visit(SignatureAttribute sa) {
                    signatureAttribute = sa;
                    attributes.add(sa);
                }
                public void visit(SyntheticAttribute sa) {
                    syntheticAttribute = sa;
                    attributes.add(sa);
                }
                public void acceptOther(Attribute ai) {
                    attributes.add(ai);
                }
            });
        }
    }

    public class Method {
        public short                                         accessFlags;
        public String                                        name;
        public String                                        descriptor;
        public final List<Attribute>                         attributes = new ArrayList<Attribute>();
        public AnnotationDefaultAttribute                    annotationDefaultAttribute;
        public CodeAttribute                                 codeAttribute;
        public ExceptionsAttribute                           exceptionsAttribute;
        public RuntimeInvisibleAnnotationsAttribute          runtimeInvisibleAnnotationsAttribute;
        public RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotationsAttribute;
        public RuntimeVisibleAnnotationsAttribute            runtimeVisibleAnnotationsAttribute;
        public RuntimeVisibleParameterAnnotationsAttribute   runtimeVisibleParameterAnnotationsAttribute;
        public SignatureAttribute                            signatureAttribute;
        public SyntheticAttribute                            syntheticAttribute;

        public Method(DataInputStream dis) throws IOException {
            accessFlags = dis.readShort();
            name = constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            descriptor = constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            readAttributes(dis, new AbstractAttributeVisitor() {
                public void visit(AnnotationDefaultAttribute ada) {
                    annotationDefaultAttribute = ada;
                    attributes.add(ada);
                }
                public void visit(CodeAttribute ca) {
                    codeAttribute = ca;
                    attributes.add(ca);
                }
                public void visit(ExceptionsAttribute ea) {
                    exceptionsAttribute = ea;
                    attributes.add(ea);
                }
                public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
                    runtimeInvisibleAnnotationsAttribute = riaa;
                    attributes.add(riaa);
                }
                public void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
                    runtimeInvisibleParameterAnnotationsAttribute = ripaa;
                    attributes.add(ripaa);
                }
                public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
                    runtimeVisibleAnnotationsAttribute = rvaa;
                    attributes.add(rvaa);
                }
                public void visit(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
                    runtimeVisibleParameterAnnotationsAttribute = rvpaa;
                    attributes.add(rvpaa);
                }
                public void visit(SignatureAttribute sa) {
                    signatureAttribute = sa;
                    attributes.add(sa);
                }
                public void visit(SyntheticAttribute sa) {
                    syntheticAttribute = sa;
                    attributes.add(sa);
                }
                public void acceptOther(Attribute ai) {
                    attributes.add(ai);
                }
            });
        }
    }

    private void readAttributes(DataInputStream dis, AttributeVisitor visitor) throws IOException {
        short n = dis.readShort();
        for (int i = 0; i < n; ++i) readAttribute(dis, visitor);
    }

    private void readAttribute(DataInputStream dis, AttributeVisitor visitor) throws IOException {
        
        String attributeName = constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        
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

    public interface Attribute {
        void accept(AttributeVisitor visitor);
    }

    public static final class SignatureAttribute implements Attribute {
        public final String signature;

        private SignatureAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            signature = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static final class UnknownAttribute implements Attribute {
        public final String name;
        public byte[]       info;

        private UnknownAttribute(String name, DataInputStream dis, ClassFile cf) throws IOException {
            this.name = name;
            info = readByteArray(dis, dis.available());
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    
    public static final class EnclosingMethodAttribute implements Attribute {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo method;
        
        private EnclosingMethodAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.clasS = cf.constantPool.getConstantClassInfo(dis.readShort());  // classIndex
            this.method = cf.constantPool.getConstantNameAndTypeInfo(dis.readShort());  // methodIndex
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    
    public static final class ExceptionsAttribute implements Attribute {
        public final List<ConstantClassInfo> exceptionNames = new ArrayList<ConstantClassInfo>();

        private ExceptionsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                exceptionNames.add(cf.constantPool.getConstantClassInfo(dis.readShort()));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

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
            maxStack = dis.readShort();
            maxLocals = dis.readShort();

            // Byte code.
            code = readByteArray(dis, dis.readInt());

            // Exception table.
            for (short i = dis.readShort(); i > 0; --i) {
                exceptionTable.add(new ExceptionTableEntry(dis, cf));
            }

            // Code attributes.
            cf.readAttributes(dis, new AbstractAttributeVisitor() {
                public void visit(LineNumberTableAttribute lnta) {
                    lineNumberTableAttribute = lnta;
                    attributes.add(lnta);
                }
                public void visit(LocalVariableTableAttribute lvta) {
                    localVariableTableAttribute = lvta;
                    attributes.add(lvta);
                }
                public void visit(LocalVariableTypeTableAttribute lvtta) {
                    localVariableTypeTableAttribute = lvtta;
                    attributes.add(lvtta);
                }
                public void acceptOther(Attribute ai) {
                    attributes.add(ai);
                }
            });
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class ExceptionTableEntry {
        public short             startPC;
        public short             endPC;
        public short             handlerPC;
        public ConstantClassInfo catchType;

        private ExceptionTableEntry(DataInputStream dis, ClassFile cf) throws IOException {
            startPC   = dis.readShort();
            endPC     = dis.readShort();
            handlerPC = dis.readShort();
            catchType = cf.constantPool.getConstantClassInfo(dis.readShort());
        }
    }

    private static byte[] readByteArray(DataInputStream dis, int size) throws IOException {
        byte[] res = new byte[size];
        dis.readFully(res);
        return res;
    }

    public static class SourceFileAttribute implements Attribute {
        public String sourceFile;

        private SourceFileAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.sourceFile = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    class LineNumberTableEntry {
        public short startPC;
        public short lineNumber;

        private LineNumberTableEntry(DataInputStream dis) throws IOException {
            startPC = dis.readShort();
            lineNumber = dis.readShort();
        }
    }
    public class LineNumberTableAttribute implements Attribute {
        public final List<LineNumberTableEntry> entries = new ArrayList<LineNumberTableEntry>();

        private LineNumberTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                entries.add(new LineNumberTableEntry(dis));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    class LocalVariableTableEntry {
        public short  startPC;
        public short  length;
        public String name;
        public String descriptor;
        public short  index;

        private LocalVariableTableEntry(DataInputStream dis, ClassFile cf) throws IOException {
            startPC    = dis.readShort();
            length     = dis.readShort();
            name       = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            descriptor = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            index      = dis.readShort();
        }
    }

    public class LocalVariableTableAttribute implements Attribute {
        public final List<LocalVariableTableEntry> entries = new ArrayList<LocalVariableTableEntry>();

        private LocalVariableTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                entries.add(new LocalVariableTableEntry(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }

    public static class LocalVariableTypeTableEntry {
        public short  startPC;
        public short  length;
        public String name;
        public String signature;
        public short  index;

        private LocalVariableTypeTableEntry(DataInputStream dis, ClassFile cf) throws IOException {
            startPC   = dis.readShort();
            length    = dis.readShort();
            name      = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            signature = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            index     = dis.readShort();
        }
    }

    public static class LocalVariableTypeTableAttribute implements Attribute {
        public final List<LocalVariableTypeTableEntry> entries = new ArrayList<LocalVariableTypeTableEntry>();

        private LocalVariableTypeTableAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                entries.add(new LocalVariableTypeTableEntry(dis, cf));
            }
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
    
    public static final class ConstantValueAttribute implements Attribute {
        public final String constantValue;

        private ConstantValueAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.constantValue = cf.constantPool.getIntegerFloatLongDoubleString(dis.readShort());
        }

        public void accept(AttributeVisitor visitor) { visitor.visit(this); }
    }
}
