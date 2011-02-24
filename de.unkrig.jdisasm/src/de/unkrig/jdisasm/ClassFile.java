
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

import de.unkrig.jdisasm.ClassFile.RuntimeInvisibleParameterAnnotationsAttribute;
import de.unkrig.jdisasm.ClassFile.RuntimeVisibleParameterAnnotationsAttribute;

public class ClassFile {
    public short                                minorVersion;
    public short                                majorVersion;
    public short                                accessFlags;
    public short                                thisClass;
    public short                                superClass;
    public final List<Short>                    interfaces = new ArrayList<Short>();
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
        constantPool.add(null); // The magic first CP entry.
        for (short i = dis.readShort(); i > 1; --i) {
            ConstantPoolInfo cpi = readConstantPoolInfo(dis);
            constantPool.add(cpi);
            for (int j = cpi.getSizeInConstantPool(); j > 1; --j) {
                constantPool.add(null);
                i--;
            }
        }

        accessFlags = dis.readShort();
        thisClass = dis.readShort();
        superClass = dis.readShort();

        // Implemented interfaces.
        for (short i = dis.readShort(); i > 0; --i) {
            interfaces.add(dis.readShort());
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

    private final List<ConstantPoolInfo> constantPool = new ArrayList<ConstantPoolInfo>();

    public static class SyntheticAttribute implements Attribute {
        public SyntheticAttribute(DataInputStream dis) {
        }
    }

    public static class DeprecatedAttribute implements Attribute {
        public DeprecatedAttribute(DataInputStream dis) {
        }
    }

    public static class ClasS {
        public Object outerClassInfoIndex;
        public Object innerClassInfoIndex;
        public Object innerNameIndex;
        public Object innerClassAccessFlags;

        public ClasS(DataInputStream dis) throws IOException {
            outerClassInfoIndex = dis.readShort();
            innerClassInfoIndex = dis.readShort();
            innerNameIndex = dis.readShort();
            innerClassAccessFlags = dis.readShort();
        }
    }
    public final class InnerClassesAttribute implements Attribute {
        public final List<ClasS> classes = new ArrayList<ClasS>();

        private InnerClassesAttribute(DataInputStream dis) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                classes.add(new ClasS(dis));
            }
        }
    }

    public static class Annotation {
        public static class ElementValuePair {
            public static abstract class ElementValue {
                public abstract String toString(ClassFile cf);
            }

            public short        elementNameIndex;
            public ElementValue elementValue;

            public ElementValuePair(DataInputStream dis) throws IOException {
                this.elementNameIndex = dis.readShort();
                this.elementValue = newElementValue(dis); 
            }

            private static ElementValue newElementValue(DataInputStream dis) throws IOException {
                final byte tag = dis.readByte();
                if ("BCDFTJSZs".indexOf(tag) != -1) {
                    final short constValueIndex = dis.readShort();
                    return new ElementValue() { public String toString(ClassFile cf) { return cf.cpi(constValueIndex); }};
                } else
                if (tag == 'e') {
                    final short typeNameIndex = dis.readShort();
                    final short constNameIndex = dis.readShort();
                    return new ElementValue() { public String toString(ClassFile cf) { return cf.cpi(typeNameIndex) + " " + cf.cpi(constNameIndex); }};
                } else
                if (tag == 'c') {
                    final short classInfoIndex = dis.readShort();
                    return new ElementValue() { public String toString(ClassFile cf) { return cf.cpi(classInfoIndex); }};
                } else
                if (tag == '@') {
                    final Annotation annotation = new Annotation(dis);
                    return new ElementValue() { public String toString(ClassFile cf) { return annotation.toString(); }};
                } else
                if (tag == '[') {
                    final List<ElementValue> values = new ArrayList<ElementValue>();
                    for (int i = dis.readShort(); i > 0; --i) {
                        values.add(newElementValue(dis));
                    }
                    return new ElementValue() { public String toString(ClassFile cf) { return values.toString(); }};
                } else
                {
                    return new ElementValue() { public String toString(ClassFile cf) { return "[Invalid element value tag '" + (char) tag + "']"; }};
                }
            }

            public String toString(ClassFile cf) {
                return cf.cpi(this.elementNameIndex) + " = " + this.elementValue.toString(cf);
            }
        }

        public short                        typeIndex;
        public final List<ElementValuePair> elementValuePairs = new ArrayList<ElementValuePair>();

        public Annotation(DataInputStream dis) throws IOException {
            this.typeIndex = dis.readShort();
            for (int i = dis.readShort(); i > 0; --i) {
                elementValuePairs.add(new ElementValuePair(dis));
            }
        }

        public String toString(ClassFile cf) {
            StringBuilder sb = new StringBuilder("@").append(cf.cpi(this.typeIndex));
            if (!this.elementValuePairs.isEmpty()) {
                Iterator<ElementValuePair> it = this.elementValuePairs.iterator();
                sb.append('(').append(it.next().toString(cf));
                while (it.hasNext()) {
                    sb.append(", ").append(it.next().toString(cf));
                }
                sb.append(')');
            }
            return sb.toString();
        }
    }

    public static class RuntimeVisibleAnnotationsAttribute implements Attribute {

        public final List<Annotation> annotations = new ArrayList<Annotation>();
        
        private RuntimeVisibleAnnotationsAttribute(DataInputStream dis) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                annotations.add(new Annotation(dis));
            }
        }
    }
    public static class RuntimeInvisibleAnnotationsAttribute extends RuntimeVisibleAnnotationsAttribute {
        public RuntimeInvisibleAnnotationsAttribute(DataInputStream  dis) throws IOException { super(dis); }
    }

    public static class RuntimeVisibleParameterAnnotationsAttribute implements Attribute {
        public static class ParameterAnnotation {
            public final List<Annotation> annotations = new ArrayList<Annotation>();

            public ParameterAnnotation(DataInputStream dis) throws IOException {
                for (int i = dis.readShort(); i > 0; --i) {
                    annotations.add(new Annotation(dis));
                }
            }
        }

        public final List<ParameterAnnotation> parameterAnnotations = new ArrayList<ParameterAnnotation>();

        private RuntimeVisibleParameterAnnotationsAttribute(DataInputStream dis) throws IOException {
            for (int j = dis.readShort(); j > 0; --j) {
                for (int i = dis.readShort(); i > 0; --i) {
                    parameterAnnotations.add(new ParameterAnnotation(dis));
                }
            }
        }
    }
    public static class RuntimeInvisibleParameterAnnotationsAttribute extends RuntimeVisibleParameterAnnotationsAttribute {
        public RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream  dis) throws IOException { super(dis); }
    }
    
    public abstract static class ConstantPoolInfo {
        public abstract String toString() ;
        public int             getSizeInConstantPool() { return 1; }
    }

    private final class NameAndTypeConstantPoolInfo extends ConstantPoolInfo {
        final short nameIndex;
        final short descriptorIndex;

        private NameAndTypeConstantPoolInfo(short nameIndex, short descriptorIndex) {
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }

        @Override
        public String toString() {
            return member("", cpi(nameIndex), cpi(descriptorIndex));
        }
    }

    private class ConstantUtf8Info extends ConstantPoolInfo {
        public ConstantUtf8Info(String value) {
            this.value = value;
        }
        @Override
        public String toString() { return this.value; }

        private String value;
    }
    
    public String cpi(short index) {
        return getConstantPoolEntry(index).toString();
    }

    public ConstantPoolInfo getConstantPoolEntry(final short index) {
        if ((index & 0xffff) < constantPool.size()) {
            ConstantPoolInfo cpi = constantPool.get(index);
            if (cpi == null) return new ConstantPoolInfo() {

                @Override
                public String toString() {
                    return "[Error: NULL constant pool entry]";
                }
            };
            return cpi;
        }
        return new ConstantPoolInfo() {

            public String toString() {
                return "[Error: Constant pool index " + (index & 0xffff) + " out of range]";
            }
        };
    }

    private ConstantPoolInfo readConstantPoolInfo(final DataInputStream dis) throws IOException {
        byte tag = dis.readByte();
        switch (tag) {
        case 7: // CONSTANT_Class_info
            {
                final short nameIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return cpi(nameIndex).replace('/', '.');
                    }
                };
            }
        case 9: // CONSTANT_Fieldref_info
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return member(cpi(classIndex), nameAndTypeIndex);
                    }
                };
            }
        case 10: // CONSTANT_Methodref_info
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return member(cpi(classIndex), nameAndTypeIndex);
                    }
                };
            }
        case 11: // CONSTANT_InterfaceMethodref_info
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return member(cpi(classIndex), nameAndTypeIndex);
                    }
                };
            }
        case 8: // CONSTANT_String_info
            {
                final short stringIndex = dis.readShort();
                return new ConstantPoolInfo() {

                    public String toString() {
                        return stringToJavaLiteral(cpi(stringIndex));
                    }
                };
            }
        case 3: // CONSTANT_Integer_info
            {
                final int bytes = dis.readInt();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return Integer.toString(bytes);
                    }
                };
            }
        case 4: // CONSTANT_Float_info
            {
                final float bytes = dis.readFloat();
                return new ConstantPoolInfo() {
                    public String toString() {
                        return bytes + "F";
                    }
                };
            }
        case 5: // CONSTANT_Long_info
            {
                final long bytes = dis.readLong();
                return new ConstantPoolInfo() {
                    public int getSizeInConstantPool() { return 2; }
                    public String toString() {
                        return bytes + "L";
                    }
                };
            }
        case 6: // CONSTANT_Double_info
            {
                final double bytes = dis.readDouble();
                return new ConstantPoolInfo() {
                    public int getSizeInConstantPool() { return 2; }
                    public String toString() {
                        return bytes + "D";
                    }
                };
            }
        case 12: // CONSTANT_NameAndType_info
            return new NameAndTypeConstantPoolInfo(dis.readShort(), dis.readShort());
        case 1: // CONSTANT_Utf8_info
            return new ConstantUtf8Info(dis.readUTF());
        default:
            throw new RuntimeException("Invalid cp_info tag \"" + (int) tag + "\"");
        }
    }

    interface AttributeVisitor {
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
        public short                                nameIndex;
        public short                                descriptorIndex;
        public ConstantValueAttribute               constantValueAttribute;
        public RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute;
        public RuntimeVisibleAnnotationsAttribute   runtimeVisibleAnnotationsAttribute;
        public SignatureAttribute                   signatureAttribute;
        public SyntheticAttribute                   syntheticAttribute;
        public final List<Attribute>                attributes = new ArrayList<Attribute>();

        public Field(DataInputStream dis) throws IOException {
            accessFlags = dis.readShort();
            nameIndex = dis.readShort();
            descriptorIndex = dis.readShort();

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
        public short                                         nameIndex;
        public short                                         descriptorIndex;
        public final List<Attribute>                         attributes = new ArrayList<Attribute>();
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
            nameIndex = dis.readShort();
            descriptorIndex = dis.readShort();
            readAttributes(dis, new AbstractAttributeVisitor() {
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
        
        String attributeName = cpi(dis.readShort());
        
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
            if (av > 0) throw new RuntimeException(av + " extraneous bytes in attribute \"" + attributeName + "\"");
        }
    }

    private void readAttributeBody(
        final String          attributeName,
        final DataInputStream dis,
        AttributeVisitor      visitor
    ) throws IOException {
        if (attributeName.equals("ConstantValue")) {
            visitor.accept(new ConstantValueAttribute(dis));
        } else
        if (attributeName.equals("Code")) {
            visitor.accept(new CodeAttribute(dis));
        } else
        if (attributeName.equals("EnclosingMethod")) {
            visitor.accept(new EnclosingMethodAttribute(dis));
        } else
        if (attributeName.equals("Exceptions")) {
            visitor.accept(new ExceptionsAttribute(dis));
        } else
        if (attributeName.equals("InnerClasses")) {
            visitor.accept(new InnerClassesAttribute(dis));
        } else
        if (attributeName.equals("RuntimeInvisibleAnnotations")) {
            visitor.accept(new RuntimeInvisibleAnnotationsAttribute(dis));
        } else
        if (attributeName.equals("RuntimeInvisibleParameterAnnotations")) {
            visitor.accept(new RuntimeInvisibleParameterAnnotationsAttribute(dis));
        } else
        if (attributeName.equals("RuntimeVisibleAnnotations")) {
            visitor.accept(new RuntimeVisibleAnnotationsAttribute(dis));
        } else
        if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
            visitor.accept(new RuntimeVisibleParameterAnnotationsAttribute(dis));
        } else
        if (attributeName.equals("Synthetic")) {
            visitor.accept(new SyntheticAttribute(dis));
        } else
        if (attributeName.equals("SourceFile")) {
            visitor.accept(new SourceFileAttribute(dis));
        } else
        if (attributeName.equals("LineNumberTable")) {
            visitor.accept(new LineNumberTableAttribute(dis));
        } else
        if (attributeName.equals("LocalVariableTable")) {
            visitor.accept(new LocalVariableTableAttribute(dis));
        } else
        if (attributeName.equals("LocalVariableTypeTable")) {
            visitor.accept(new LocalVariableTypeTableAttribute(dis));
        } else
        if (attributeName.equals("Deprecated")) {
            visitor.accept(new DeprecatedAttribute(dis));
        } else
        if (attributeName.equals("Signature")) {
            visitor.accept(new SignatureAttribute(dis));
        } else
        {
            visitor.accept(new UnknownAttribute(dis));
        }
    }

    public interface Attribute {
    }

    public final class SignatureAttribute implements Attribute {
        public final short index;

        private SignatureAttribute(DataInputStream dis) throws IOException {
            index = dis.readShort();
        }
    }

    public final class UnknownAttribute implements Attribute {
        public byte[] info;

        private UnknownAttribute(DataInputStream dis) throws IOException {
            info = readByteArray(dis, dis.available());
        }
    }
    
    public final class EnclosingMethodAttribute implements Attribute {
        public short classIndex;
        public short methodIndex;
        
        private EnclosingMethodAttribute(DataInputStream dis) throws IOException {
            this.classIndex = dis.readShort();
            this.methodIndex = dis.readShort();
        }
    }
    
    public final class ExceptionsAttribute implements Attribute {
        public final List<Short> exceptions = new ArrayList<Short>();

        private ExceptionsAttribute(DataInputStream dis) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                exceptions.add(dis.readShort());
            }
        }
    }

    public final class CodeAttribute implements Attribute {
        public final short                     maxStack;
        public final short                     maxLocals;
        public final byte[]                    code;
        public final List<ExceptionTableEntry> exceptionTable = new ArrayList<ExceptionTableEntry>();
        public LocalVariableTableAttribute     localVariableTableAttribute;
        public LocalVariableTypeTableAttribute localVariableTypeTableAttribute;
        public LineNumberTableAttribute        lineNumberTableAttribute;
        public final List<Attribute>           attributes = new ArrayList<Attribute>();

        private CodeAttribute(DataInputStream dis) throws IOException {
            maxStack = dis.readShort();
            maxLocals = dis.readShort();

            // Byte code.
            code = readByteArray(dis, dis.readInt());

            // Exception table.
            for (short i = dis.readShort(); i > 0; --i) {
                exceptionTable.add(new ExceptionTableEntry(dis));
            }

            // Code attributes.
            readAttributes(dis, new AbstractAttributeVisitor() {
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

    public class ExceptionTableEntry {
        public short startPC;
        public short endPC;
        public short handlerPC;
        public short catchType;

        private ExceptionTableEntry(DataInputStream dis) throws IOException {
            startPC   = dis.readShort();
            endPC     = dis.readShort();
            handlerPC = dis.readShort();
            catchType = dis.readShort();
        }
    }

    private static byte[] readByteArray(DataInputStream dis, int size) throws IOException {
        byte[] res = new byte[size];
        dis.readFully(res);
        return res;
    }

    public class SourceFileAttribute implements Attribute {
        public final short sourceFileIndex;

        private SourceFileAttribute(DataInputStream dis) throws IOException {
            this.sourceFileIndex = dis.readShort();
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

        private LineNumberTableAttribute(DataInputStream dis) throws IOException {
            for (int i = dis.readShort(); i > 0; --i) {
                entries.add(new LineNumberTableEntry(dis));
            }
        }
    }

    class LocalVariableTableEntry {
        public short startPC;
        public short length;
        public short nameIndex;
        public short descriptorIndex;
        public short index;

        private LocalVariableTableEntry(DataInputStream dis) throws IOException {
            startPC         = dis.readShort();
            length          = dis.readShort();
            nameIndex       = dis.readShort();
            descriptorIndex = dis.readShort();
            index           = dis.readShort();
        }
    }

    public class LocalVariableTableAttribute implements Attribute {
        public final List<LocalVariableTableEntry> entries = new ArrayList<LocalVariableTableEntry>();

        private LocalVariableTableAttribute(DataInputStream dis) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                entries.add(new LocalVariableTableEntry(dis));
            }
        }
    }

    class LocalVariableTypeTableEntry {
        public short startPC;
        public short length;
        public short nameIndex;
        public short signatureIndex;
        public short index;

        private LocalVariableTypeTableEntry(DataInputStream dis) throws IOException {
            startPC        = dis.readShort();
            length         = dis.readShort();
            nameIndex      = dis.readShort();
            signatureIndex = dis.readShort();
            index          = dis.readShort();
        }
    }

    public class LocalVariableTypeTableAttribute implements Attribute {
        public final List<LocalVariableTypeTableEntry> entries = new ArrayList<LocalVariableTypeTableEntry>();

        private LocalVariableTypeTableAttribute(DataInputStream dis) throws IOException {
            for (short i = dis.readShort(); i > 0; --i) {
                entries.add(new LocalVariableTypeTableEntry(dis));
            }
        }
    }
    
    public final class ConstantValueAttribute implements Attribute {
        public final short constantValueIndex;

        private ConstantValueAttribute(DataInputStream dis) throws IOException {
            this.constantValueIndex = dis.readShort();
        }
    }

    private static String stringToJavaLiteral(String s) {
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            int idx = "\r\n\"\t\b".indexOf(c);
            if (idx == -1) {
                ++i;
            } else {
                s = s.substring(0, i) + '\\' + "rn\"tb".charAt(idx) + s.substring(i + 1);
                i += 2;
            }
            if (i >= 80) break;
        }
        return '"' + s + '"';
    }

    /**
     * Returns one of the following:
     * <ul>
     *   <li>TypeName.methodName(parameterType, parameterType) => ReturnType
     *   <li>TypeName(parameterType, parameterType)
     *   <li>""
     *   <li>FieldType TypeName.fieldName
     * </ul>
     */
    private String member(String typeName, short nameAndTypeIndex) {
        NameAndTypeConstantPoolInfo nat = (NameAndTypeConstantPoolInfo) getConstantPoolEntry(nameAndTypeIndex);
        return member(typeName, cpi(nat.nameIndex), cpi(nat.descriptorIndex));
    }

    /**
     * Returns one of the following:
     * <ul>
     *   <li>TypeName.methodName(parameterType, parameterType) => ReturnType
     *   <li>TypeName(parameterType, parameterType)
     *   <li>""
     *   <li>FieldType TypeName.fieldName
     * </ul>
     */
    private String member(String typeName, String name, String descriptor) {
        if (descriptor.startsWith("(")) {
            return Descriptor.decodeMethodDescriptor(descriptor, name, typeName);
        } else {
            return Descriptor.decodeFieldDescriptor(descriptor) + " " + typeName + "." + name;
        }
    }

    public short getConstantPoolSize() {
        return (short) constantPool.size();
    }
}
