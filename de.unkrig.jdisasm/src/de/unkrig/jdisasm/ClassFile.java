
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

            public void accept(EnclosingMethodAttribute ema) {
                enclosingMethodAttribute = ema;
                attributes.add(ema);
            }

            public void accept(RuntimeInvisibleAnnotationsAttribute riaa) {
                runtimeInvisibleAnnotationsAttribute = riaa;
                attributes.add(riaa);
            }

            public void accept(RuntimeVisibleAnnotationsAttribute rvaa) {
                runtimeVisibleAnnotationsAttribute = rvaa;
                attributes.add(rvaa);
            }

            public void accept(SignatureAttribute sa) {
                signatureAttribute = sa;
                attributes.add(sa);
            }

            public void accept(SourceFileAttribute sfa) {
                sourceFileAttribute = sfa;
                attributes.add(sfa);
            }

            public void accept(SyntheticAttribute sa) {
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
    }

    public static class DeprecatedAttribute implements Attribute {
        public DeprecatedAttribute(DataInputStream dis, ClassFile cf) {
        }
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
            this.typeName = SignatureParser.decodeFieldDescriptor(cf.constantPool.getConstantUtf8Info(typeIndex).bytes).toString();
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
            final String s = SignatureParser.decodeFieldDescriptor(typeName) + "." + constName;
            return new ElementValue() { public String toString() { return s; }};
        } else
        if (tag == 'c') {
            final String classInfo = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
            final String s = SignatureParser.decodeFieldDescriptor(classInfo) + ".class";
            return new ElementValue() { public String toString() { return s; }};
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
    }
    public static class RuntimeInvisibleAnnotationsAttribute extends RuntimeVisibleAnnotationsAttribute {
        public RuntimeInvisibleAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }
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
            for (int j = dis.readShort(); j > 0; --j) {
                for (int i = dis.readShort(); i > 0; --i) {
                    parameterAnnotations.add(new ParameterAnnotation(dis, cf));
                }
            }
        }
    }
    public static class RuntimeInvisibleParameterAnnotationsAttribute extends RuntimeVisibleParameterAnnotationsAttribute {
        public RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream  dis, ClassFile cf) throws IOException {
            super(dis, cf);
        }
    }
    
    public static class AnnotationDefaultAttribute implements Attribute {

        public ElementValue defaultValue;
        
        private AnnotationDefaultAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.defaultValue = newElementValue(dis, cf);
        }
    }

    interface AttributeVisitor {
        void accept(AnnotationDefaultAttribute                    ada);
        void accept(CodeAttribute                                 ca);
        void accept(ConstantValueAttribute                        cva);
        void accept(DeprecatedAttribute                           da);
        void accept(EnclosingMethodAttribute                      ema);
        void accept(ExceptionsAttribute                           ea);
        void accept(InnerClassesAttribute                         ica);
        void accept(LineNumberTableAttribute                      lnta);
        void accept(LocalVariableTableAttribute                   lvta);
        void accept(LocalVariableTypeTableAttribute               lvtta);
        void accept(RuntimeInvisibleAnnotationsAttribute          riaa);
        void accept(RuntimeInvisibleParameterAnnotationsAttribute ripaa);
        void accept(RuntimeVisibleAnnotationsAttribute            rvaa);
        void accept(RuntimeVisibleParameterAnnotationsAttribute   rvpaa);
        void accept(SignatureAttribute                            sa);
        void accept(SourceFileAttribute                           sfa);
        void accept(SyntheticAttribute                            sa);

        void accept(UnknownAttribute unknownAttribute);
    }

    public static abstract class AbstractAttributeVisitor implements AttributeVisitor {

        public abstract void acceptOther(Attribute ai);

        public void accept(AnnotationDefaultAttribute                    ada)   { acceptOther(ada); }
        public void accept(CodeAttribute                                 ca)    { acceptOther(ca); }
        public void accept(ConstantValueAttribute                        cva)   { acceptOther(cva); }
        public void accept(DeprecatedAttribute                           da)    { acceptOther(da); }
        public void accept(EnclosingMethodAttribute                      ema)   { acceptOther(ema); }
        public void accept(ExceptionsAttribute                           ea)    { acceptOther(ea); }
        public void accept(InnerClassesAttribute                         ica)   { acceptOther(ica); }
        public void accept(LineNumberTableAttribute                      lnta)  { acceptOther(lnta); }
        public void accept(LocalVariableTableAttribute                   lvta)  { acceptOther(lvta); }
        public void accept(LocalVariableTypeTableAttribute               lvtta) { acceptOther(lvtta); }
        public void accept(RuntimeInvisibleAnnotationsAttribute          riaa)  { acceptOther(riaa); }
        public void accept(RuntimeInvisibleParameterAnnotationsAttribute ripaa) { acceptOther(ripaa); }
        public void accept(RuntimeVisibleAnnotationsAttribute            rvaa)  { acceptOther(rvaa); }
        public void accept(RuntimeVisibleParameterAnnotationsAttribute   rvpaa) { acceptOther(rvpaa); }
        public void accept(SignatureAttribute                            sa)    { acceptOther(sa); }
        public void accept(SourceFileAttribute                           sfa)   { acceptOther(sfa); }
        public void accept(SyntheticAttribute                            sa)    { acceptOther(sa); }
        public void accept(UnknownAttribute                              a)     { acceptOther(a); }
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
                public void accept(ConstantValueAttribute cva) {
                    constantValueAttribute = cva;
                    attributes.add(cva);
                }
                public void accept(RuntimeInvisibleAnnotationsAttribute riaa) {
                    runtimeInvisibleAnnotationsAttribute = riaa;
                    attributes.add(riaa);
                }
                public void accept(RuntimeVisibleAnnotationsAttribute rvaa) {
                    runtimeVisibleAnnotationsAttribute = rvaa;
                    attributes.add(rvaa);
                }
                public void accept(SignatureAttribute sa) {
                    signatureAttribute = sa;
                    attributes.add(sa);
                }
                public void accept(SyntheticAttribute sa) {
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
                public void accept(AnnotationDefaultAttribute ada) {
                    annotationDefaultAttribute = ada;
                    attributes.add(ada);
                }
                public void accept(CodeAttribute ca) {
                    codeAttribute = ca;
                    attributes.add(ca);
                }
                public void accept(ExceptionsAttribute ea) {
                    exceptionsAttribute = ea;
                    attributes.add(ea);
                }
                public void accept(RuntimeInvisibleAnnotationsAttribute riaa) {
                    runtimeInvisibleAnnotationsAttribute = riaa;
                    attributes.add(riaa);
                }
                public void accept(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
                    runtimeInvisibleParameterAnnotationsAttribute = ripaa;
                    attributes.add(ripaa);
                }
                public void accept(RuntimeVisibleAnnotationsAttribute rvaa) {
                    runtimeVisibleAnnotationsAttribute = rvaa;
                    attributes.add(rvaa);
                }
                public void accept(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
                    runtimeVisibleParameterAnnotationsAttribute = rvpaa;
                    attributes.add(rvpaa);
                }
                public void accept(SignatureAttribute sa) {
                    signatureAttribute = sa;
                    attributes.add(sa);
                }
                public void accept(SyntheticAttribute sa) {
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
            visitor.accept(new AnnotationDefaultAttribute(dis, this));
        } else
        if (attributeName.equals("ConstantValue")) {
            visitor.accept(new ConstantValueAttribute(dis, this));
        } else
        if (attributeName.equals("Code")) {
            visitor.accept(new CodeAttribute(dis, this));
        } else
        if (attributeName.equals("Deprecated")) {
            visitor.accept(new DeprecatedAttribute(dis, this));
        } else
        if (attributeName.equals("EnclosingMethod")) {
            visitor.accept(new EnclosingMethodAttribute(dis, this));
        } else
        if (attributeName.equals("Exceptions")) {
            visitor.accept(new ExceptionsAttribute(dis, this));
        } else
        if (attributeName.equals("InnerClasses")) {
            visitor.accept(new InnerClassesAttribute(dis, this));
        } else
        if (attributeName.equals("LineNumberTable")) {
            visitor.accept(new LineNumberTableAttribute(dis, this));
        } else
        if (attributeName.equals("LocalVariableTable")) {
            visitor.accept(new LocalVariableTableAttribute(dis, this));
        } else
        if (attributeName.equals("LocalVariableTypeTable")) {
            visitor.accept(new LocalVariableTypeTableAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeInvisibleAnnotations")) {
            visitor.accept(new RuntimeInvisibleAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeInvisibleParameterAnnotations")) {
            visitor.accept(new RuntimeInvisibleParameterAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeVisibleAnnotations")) {
            visitor.accept(new RuntimeVisibleAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
            visitor.accept(new RuntimeVisibleParameterAnnotationsAttribute(dis, this));
        } else
        if (attributeName.equals("Signature")) {
            visitor.accept(new SignatureAttribute(dis, this));
        } else
        if (attributeName.equals("SourceFile")) {
            visitor.accept(new SourceFileAttribute(dis, this));
        } else
        if (attributeName.equals("Synthetic")) {
            visitor.accept(new SyntheticAttribute(dis, this));
        } else
        {
            visitor.accept(new UnknownAttribute(dis, this));
        }
    }

    public interface Attribute {
    }

    public static final class SignatureAttribute implements Attribute {
        public final String signature;

        private SignatureAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            signature = cf.constantPool.getConstantUtf8Info(dis.readShort()).bytes;
        }
    }

    public static final class UnknownAttribute implements Attribute {
        public byte[] info;

        private UnknownAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            info = readByteArray(dis, dis.available());
        }
    }
    
    public static final class EnclosingMethodAttribute implements Attribute {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo method;
        
        private EnclosingMethodAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.clasS = cf.constantPool.getConstantClassInfo(dis.readShort());  // classIndex
            this.method = cf.constantPool.getConstantNameAndTypeInfo(dis.readShort());  // methodIndex
        }
    }
    
    public static final class ExceptionsAttribute implements Attribute {
        public final List<ConstantClassInfo> exceptionNames = new ArrayList<ConstantClassInfo>();

        private ExceptionsAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                exceptionNames.add(cf.constantPool.getConstantClassInfo(dis.readShort()));
            }
        }
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
                public void accept(LineNumberTableAttribute lnta) {
                    lineNumberTableAttribute = lnta;
                    attributes.add(lnta);
                }
                public void accept(LocalVariableTableAttribute lvta) {
                    localVariableTableAttribute = lvta;
                    attributes.add(lvta);
                }
                public void accept(LocalVariableTypeTableAttribute lvtta) {
                    localVariableTypeTableAttribute = lvtta;
                    attributes.add(lvtta);
                }
                public void acceptOther(Attribute ai) {
                    attributes.add(ai);
                }
            });
        }
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
    }
    
    public static final class ConstantValueAttribute implements Attribute {
        public final String constantValue;

        private ConstantValueAttribute(DataInputStream dis, ClassFile cf) throws IOException {
            this.constantValue = cf.constantPool.getIntegerFloatLongDoubleString(dis.readShort());
        }
    }
}
