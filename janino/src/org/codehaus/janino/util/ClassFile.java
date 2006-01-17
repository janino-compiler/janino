
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.util;

import java.io.*;
import java.util.*;

import org.codehaus.janino.Descriptor;


/**
 * An object that represents the Java<sup>TM</sup> "class file" format.
 * <p>
 * {@link #ClassFile(InputStream)} creates a {@link ClassFile} object from the bytecode
 * read from the given {@link InputStream}.
 * <p>
 * {@link #store(OutputStream)} generates Java<sup>TM</sup> bytecode
 * which is suitable for being processed by a Java<sup>TM</sup> virtual
 * machine.
 */
public class ClassFile {

    /**
     * Construct from parsed components.
     * @param accessFlags as defined by {@link org.codehaus.janino.Mod}
     * @param thisClassFD the field descriptor for this class
     * @param superclassFD the field descriptor for the extended class (e.g. "Ljava/lang/Object;")
     * @param interfaceFDs the field descriptors for the implemented interfaces
     */
    public ClassFile(
        short    accessFlags,
        String   thisClassFD,
        String   superclassFD,
        String[] interfaceFDs
    ) {
        this.majorVersion  = ClassFile.MAJOR_VERSION_JDK_1_1;
        this.minorVersion  = ClassFile.MINOR_VERSION_JDK_1_1;

        this.constantPool  = new ArrayList();
        this.constantPool.add(null); // Add fake "0" index entry.
        this.constantPoolMap = new HashMap();

        this.accessFlags   = accessFlags;
        this.thisClass     = this.addConstantClassInfo(thisClassFD);
        this.superclass    = this.addConstantClassInfo(superclassFD);
        this.interfaces    = new short[interfaceFDs.length];
        for (int i = 0; i < interfaceFDs.length; ++i) {
            this.interfaces[i] = this.addConstantClassInfo(interfaceFDs[i]);
        }

        this.fieldInfos    = new ArrayList();
        this.methodInfos   = new ArrayList();
        this.attributes    = new ArrayList();
    }

    /**
     * Adds a "SourceFile" attribute to this class file. (Does not check whether one exists already.)
     * @param sourceFileName
     */
    public void addSourceFileAttribute(String sourceFileName) {
        this.attributes.add(new SourceFileAttribute(
            this.addConstantUtf8Info("SourceFile"),  // attributeNameIndex
            this.addConstantUtf8Info(sourceFileName) // sourceFileIndex
        ));
    }

    public void addDeprecatedAttribute() {
        this.attributes.add(new DeprecatedAttribute(this.addConstantUtf8Info("Deprecated")));
    }

    /**
     * Find the "InnerClasses" attribute of this class file
     * @return <code>null</code> if this class has no "InnerClasses" attribute
     */
    public InnerClassesAttribute getInnerClassesAttribute() {
        Short ni = (Short) this.constantPoolMap.get(new ConstantUtf8Info("InnerClasses"));
        if (ni == null) return null;

        for (Iterator it = this.attributes.iterator(); it.hasNext();) {
            AttributeInfo ai = (AttributeInfo) it.next();
            if (ai.nameIndex == ni.shortValue() && ai instanceof InnerClassesAttribute) return (InnerClassesAttribute) ai;
        }
        return null;
    }

    /**
     * Create an "InnerClasses" attribute if it does not exist, then add the given entry
     * to the "InnerClasses" attribute.
     * @param e
     */
    public void addInnerClassesAttributeEntry(InnerClassesAttribute.Entry e) {
        InnerClassesAttribute ica = this.getInnerClassesAttribute();
        if (ica == null) {
            ica = new InnerClassesAttribute(this.addConstantUtf8Info("InnerClasses"));
            this.attributes.add(ica);
        }
        ica.getEntries().add(e);
        return;
    }

    /**
     * Read "class file" data from a {@link InputStream} and construct a
     * {@link ClassFile} object from it.
     * <p>
     * If the {@link ClassFile} is created with this constructor, then most modifying operations
     * lead to a {@link UnsupportedOperationException}; only fields, methods and
     * attributes can be added. 
     * @param inputStream
     * @throws IOException
     * @throws ClassFormatError
     */
    public ClassFile(InputStream inputStream) throws IOException, ClassFormatError {
        DataInputStream dis = inputStream instanceof DataInputStream ? (DataInputStream) inputStream : new DataInputStream(inputStream);

        int magic = dis.readInt();                           // magic
        if (magic != ClassFile.CLASS_FILE_MAGIC) throw new ClassFormatError("Invalid magic number");

        this.minorVersion = dis.readShort();                 // minor_version
        this.majorVersion = dis.readShort();                 // major_version
        if (!ClassFile.isRecognizedVersion(this.majorVersion, this.minorVersion)) throw new ClassFormatError("Unrecognized class file format version " + this.majorVersion + "/" + this.minorVersion);

        this.constantPool = new ArrayList();
        this.constantPoolMap = new HashMap();
        this.loadConstantPool(dis);                          // constant_pool_count, constant_pool

        this.accessFlags  = dis.readShort();                 // access_flags
        this.thisClass    = dis.readShort();                 // this_class
        this.superclass   = dis.readShort();                 // super_class
        this.interfaces   = ClassFile.readShortArray(dis);   // interfaces_count, interfaces

        this.fieldInfos  = Collections.unmodifiableList(this.loadFields(dis));     // fields_count, fields
        this.methodInfos = Collections.unmodifiableList(this.loadMethods(dis));    // methods_count, methods
        this.attributes  = Collections.unmodifiableList(this.loadAttributes(dis)); // attributes_count, attributes
    }

    /**
     * @return The fully qualified name of this class, e.g. "pkg1.pkg2.Outer$Inner"
     */
    public String getThisClassName() {
        return this.getConstantClassName(this.thisClass).replace('/', '.');
    }

    /**
     * Sets the major and minor class file version numbers (JVMS 4.1). The class file version
     * defaults to the JDK 1.1 values (45.3) which execute on virtually every JVM.
     * @param majorVersion
     * @param minorVersion
     */
    public void setVersion(short majorVersion, short minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    /**
     * Returns the current major class file version number.
     */
    public short getMajorVersion() {
        return this.majorVersion;
    }

    /**
     * Returns the current minor class file version number.
     */
    public short getMinorVersion() {
        return this.minorVersion;
    }

    public static boolean isRecognizedVersion(short majorVersion, short minorVersion) {
        return (
            (majorVersion == ClassFile.MAJOR_VERSION_JDK_1_1 && minorVersion == ClassFile.MINOR_VERSION_JDK_1_1) ||
            (majorVersion == ClassFile.MAJOR_VERSION_JDK_1_2 && minorVersion == ClassFile.MINOR_VERSION_JDK_1_2) ||
            (majorVersion == ClassFile.MAJOR_VERSION_JDK_1_3 && minorVersion == ClassFile.MINOR_VERSION_JDK_1_3) ||
            (majorVersion == ClassFile.MAJOR_VERSION_JDK_1_4 && minorVersion == ClassFile.MINOR_VERSION_JDK_1_4) ||
            (majorVersion == ClassFile.MAJOR_VERSION_JDK_1_5 && minorVersion == ClassFile.MINOR_VERSION_JDK_1_5)
        );
    }

    /**
     * Add a "CONSTANT_Class_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#1221">JVM specification, section 4.4.1</a>
     */
    public short addConstantClassInfo(String typeFD) {
        String s;
        if (Descriptor.isClassOrInterfaceReference(typeFD)) {
            s = Descriptor.toInternalForm(typeFD);
        } else
        if (Descriptor.isArrayReference(typeFD)) {
            s = typeFD;
        } else
        {
            throw new RuntimeException("\"" + Descriptor.toString(typeFD) + "\" is neither a class nor an array");
        }

        return this.addToConstantPool(new ConstantClassInfo(this.addConstantUtf8Info(s)));
    }

    /**
     * Add a "CONSTANT_Fieldref_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#42041">JVM specification, section 4.4.2</a>
     */
    public short addConstantFieldrefInfo(
        String classFD,
        String fieldName,
        String fieldFD
    ) {
        return this.addToConstantPool(new ConstantFieldrefInfo(
            this.addConstantClassInfo(classFD),
            this.addConstantNameAndTypeInfo(fieldName, fieldFD)
        ));
    }

    /**
     * Add a "CONSTANT_Methodref_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#42041">JVM specification, section 4.4.2</a>
     */
    public short addConstantMethodrefInfo(
        String classFD,
        String methodName,
        String methodMD
    ) {
        return this.addToConstantPool(new ConstantMethodrefInfo(
            this.addConstantClassInfo(classFD),
            this.addConstantNameAndTypeInfo(methodName, methodMD)
        ));
    }

    /**
     * Add a "CONSTANT_InterfaceMethodref_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#42041">JVM specification, section 4.4.2</a>
     */
    public short addConstantInterfaceMethodrefInfo(
        String classFD,
        String methodName,
        String methodMD
    ) {
        return this.addToConstantPool(new ConstantInterfaceMethodrefInfo(
            this.addConstantClassInfo(classFD),
            this.addConstantNameAndTypeInfo(methodName, methodMD)
        ));
    }

    /**
     * Add a "CONSTANT_String_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#29297">JVM specification, section 4.4.3</a>
     */
    public short addConstantStringInfo(String string) {
        return this.addToConstantPool(new ConstantStringInfo(this.addConstantUtf8Info(string)));
    }

    /**
     * Add a "CONSTANT_Integer_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#21942">JVM specification, section 4.4.4</a>
     */
    public short addConstantIntegerInfo(final int value) {
        return this.addToConstantPool(new ConstantIntegerInfo(value));
    }

    /**
     * Add a "CONSTANT_Float_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#21942">JVM specification, section 4.4.4</a>
     */
    public short addConstantFloatInfo(final float value) {
        return this.addToConstantPool(new ConstantFloatInfo(value));
    }

    /**
     * Add a "CONSTANT_Long_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#1348">JVM specification, section 4.4.5</a>
     */
    public short addConstantLongInfo(final long value) {
        return this.addToConstantPool(new ConstantLongInfo(value));
    }

    /**
     * Add a "CONSTANT_Double_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#1348">JVM specification, section 4.4.5</a>
     */
    public short addConstantDoubleInfo(final double value) {
        return this.addToConstantPool(new ConstantDoubleInfo(value));
    }

    /**
     * Add a "CONSTANT_NameAndType_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#1327">JVM specification, section 4.4.6</a>
     */
    private short addConstantNameAndTypeInfo(String name, String descriptor) {
        return this.addToConstantPool(new ConstantNameAndTypeInfo(
            this.addConstantUtf8Info(name),
            this.addConstantUtf8Info(descriptor)
        ));
    }

    /**
     * Add a "CONSTANT_Utf8_info" structure to the class file.
     * 
     * @see <a href="http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#7963">JVM specification, section 4.4.7</a>
     */
    public short addConstantUtf8Info(final String s) {
        return this.addToConstantPool(new ConstantUtf8Info(s));
    }

    /**
     * Convenience method that adds a String, Integer, Float, Long or Double ConstantInfo.
     */
    private short addConstantSIFLDInfo(Object cv) {
        if (cv instanceof String) {
            return this.addConstantStringInfo((String) cv);
        } else
        if (
            cv instanceof Byte    ||
            cv instanceof Short   ||
            cv instanceof Integer
        ) {
            return this.addConstantIntegerInfo(((Number) cv).intValue());
        } else
        if (cv instanceof Boolean) {
            return this.addConstantIntegerInfo(((Boolean) cv).booleanValue() ? 1 : 0);
        } else
        if (cv instanceof Character) {
            return this.addConstantIntegerInfo(((Character) cv).charValue());
        } else
        if (cv instanceof Float) {
            return this.addConstantFloatInfo(((Float) cv).floatValue());
        } else
        if (cv instanceof Long) {
            return this.addConstantLongInfo(((Long) cv).longValue());
        } else
        if (cv instanceof Double) {
            return this.addConstantDoubleInfo(((Double) cv).doubleValue());
        } else
        {
            throw new RuntimeException("Unexpected constant value type \"" + cv.getClass().getName() + "\"");
        }
    }

    private short addToConstantPool(ConstantPoolInfo cpi) {
        Short index = (Short) this.constantPoolMap.get(cpi);
        if (index != null) return index.shortValue();

        short res = (short) this.constantPool.size();
        this.constantPool.add(cpi);
        if (cpi.isWide()) this.constantPool.add(null);

        this.constantPoolMap.put(cpi, new Short(res));
        return res;
    }

    public FieldInfo addFieldInfo(
        short  accessFlags,
        String fieldName,
        String fieldTypeFD,
        Object optionalConstantValue
    ) {
        List attributes = new ArrayList();
        if (optionalConstantValue != null) {
            attributes.add(new ConstantValueAttribute(
                this.addConstantUtf8Info("ConstantValue"),
                this.addConstantSIFLDInfo(optionalConstantValue)
            ));
        }
        FieldInfo fi = new FieldInfo(
            accessFlags,                           // accessFlags
            this.addConstantUtf8Info(fieldName),   // nameIndex
            this.addConstantUtf8Info(fieldTypeFD), // descriptorIndex
            attributes                             // attributes
        );
        this.fieldInfos.add(fi);
        return fi;
    }

    public MethodInfo addMethodInfo(
        short  accessFlags,
        String methodName,
        String methodMD
    ) {
        MethodInfo mi = new MethodInfo(
            accessFlags,                          // accessFlags
            this.addConstantUtf8Info(methodName), // nameIndex
            this.addConstantUtf8Info(methodMD),   // desriptorIndex
            new ArrayList()                       // attributes
        );
        this.methodInfos.add(mi);
        return mi;
    }

    public ConstantPoolInfo getConstantPoolInfo(short index) {
        return (ConstantPoolInfo) this.constantPool.get(index);
    }

    /**
     * @param index Index to a <code>CONSTANT_Class_info</code> in the constant pool
     * @return The name of the denoted class in "internal form" (see JVMS 4.2)
     */
    public String getConstantClassName(short index) {
        ConstantClassInfo cci = (ConstantClassInfo) this.getConstantPoolInfo(index);
        ConstantUtf8Info  cui = (ConstantUtf8Info) this.getConstantPoolInfo(cci.nameIndex);
        return cui.s;
    }

    /**
     * @param index Index to a <code>CONSTANT_Utf8_info</code> in the constant pool
     * @return The string represented by the structure
     */
    public String getConstantUtf8(short index) {
        ConstantUtf8Info cui = (ConstantUtf8Info) this.getConstantPoolInfo(index);
        return cui.s;
    }

    /**
     * u4 length, u1[length]
     */
    private static byte[] readLengthAndBytes(DataInputStream dis) throws IOException {
        byte[] ba = new byte[dis.readInt()];
        dis.readFully(ba);
        return ba;
    }

    /**
     * u2 length, u2[length]
     */
    private static short[] readShortArray(DataInputStream dis) throws IOException {
        short count = dis.readShort();
        short[] result = new short[count];
        for (int i = 0; i < count; ++i) result[i] = dis.readShort();
        return result;
    }

    /**
     * u2 constant_pool_count, constant_pool[constant_pool_count]
     */
    private void loadConstantPool(DataInputStream dis) throws IOException {
        this.constantPool.clear();
        this.constantPoolMap.clear();

        short constantPoolCount = dis.readShort(); // constant_pool_count
        this.constantPool.add(null);
        for (short i = 1; i < constantPoolCount; ++i) {
            ConstantPoolInfo cpi = ConstantPoolInfo.loadConstantPoolInfo(dis);
            this.constantPool.add(cpi);
            this.constantPoolMap.put(cpi, new Short(i));
            if (cpi instanceof ConstantLongInfo || cpi instanceof ConstantDoubleInfo) {
                this.constantPool.add(null);
                ++i;
            }
        }
    }

    /**
     * u2 fields_count, fields[fields_count]
     */
    private List loadFields(DataInputStream dis) throws IOException {
        short fieldsCount = dis.readShort();
        List fields = new ArrayList(fieldsCount);
        for (int i = 0; i < fieldsCount; ++i) {
            fields.add(new FieldInfo(
                dis.readShort(),         // accessFlags
                dis.readShort(),         // nameIndex
                dis.readShort(),         // descriptorIndex
                this.loadAttributes(dis) // attributes
            ));
        }
        return fields;
    }

    /**
     * u2 methods_count, methods[methods_count]
     */
    private List loadMethods(DataInputStream dis) throws IOException {
        short methodsCount = dis.readShort();
        List methods = new ArrayList(methodsCount);
        for (int i = 0; i < methodsCount; ++i) methods.add(this.loadMethodInfo(dis));
        return methods;
    }

    /**
     * u2 attributes_count, attributes[attributes_count]
     */
    private List loadAttributes(DataInputStream dis) throws IOException {
        short attributesCount = dis.readShort();
        List attributes = new ArrayList(attributesCount);
        for (int i = 0; i < attributesCount; ++i) attributes.add(this.loadAttribute(dis));
        return attributes;
    }

    /**
     * Write {@link ClassFile} to an {@link OutputStream}, in "class file" format.
     * <p>
     * Notice that if an {@link IOException} is thrown, the class file is
     * probably written incompletely and thus invalid. The calling method must take
     * care of this situation, e.g. by closing the output stream and then deleting the
     * file.
     * @param os
     * @throws IOException
     */
    public void store(OutputStream os) throws IOException {
        DataOutputStream dos = os instanceof DataOutputStream ? (DataOutputStream) os : new DataOutputStream(os);

        dos.writeInt(ClassFile.CLASS_FILE_MAGIC);            // magic
        dos.writeShort(this.minorVersion);                   // minor_version
        dos.writeShort(this.majorVersion);                   // major_version
        ClassFile.storeConstantPool(dos, this.constantPool); // constant_pool_count, constant_pool
        dos.writeShort(this.accessFlags);                    // access_flags
        dos.writeShort(this.thisClass);                      // this_class
        dos.writeShort(this.superclass);                     // super_class
        ClassFile.storeShortArray(dos, this.interfaces);     // interfaces_count, interfaces
        ClassFile.storeFields(dos, this.fieldInfos);         // fields_count, fields
        ClassFile.storeMethods(dos, this.methodInfos);       // methods_count, methods
        ClassFile.storeAttributes(dos, this.attributes);     // attributes_count, attributes
    }

    /**
     * u2 constant_pool_count, constant_pool[constant_pool_count - 1]
     */
    private static void storeConstantPool(DataOutputStream dos, List constantPool) throws IOException {
        dos.writeShort(constantPool.size());
        for (int i = 1; i < constantPool.size(); ++i) {
            ConstantPoolInfo cpi = (ConstantPoolInfo) constantPool.get(i);
            if (cpi == null) continue; // (Double or Long CPI.)
            cpi.store(dos);
        }
    }

    /**
     * u2 count, u2[count]
     */
    private static void storeShortArray(DataOutputStream dos, short[] sa) throws IOException {
        dos.writeShort(sa.length);
        for (int i = 0; i < sa.length; ++i) dos.writeShort(sa[i]);
    }

    /**
     * u2 fields_count, fields[fields_count]
     */
    private static void storeFields(DataOutputStream dos, List fieldInfos) throws IOException {
        dos.writeShort(fieldInfos.size());
        for (int i = 0; i < fieldInfos.size(); ++i) ((FieldInfo) fieldInfos.get(i)).store(dos);
    }

    /**
     * u2 methods_count, methods[methods_count]
     */
    private static void storeMethods(DataOutputStream dos, List methodInfos) throws IOException {
        dos.writeShort(methodInfos.size());
        for (int i = 0; i < methodInfos.size(); ++i) ((MethodInfo) methodInfos.get(i)).store(dos);
    }

    /**
     * u2 attributes_count, attributes[attributes_count]
     */
    private static void storeAttributes(DataOutputStream dos, List attributeInfos) throws IOException {
        dos.writeShort(attributeInfos.size());
        for (int i = 0; i < attributeInfos.size(); ++i) ((AttributeInfo) attributeInfos.get(i)).store(dos);
    }

    /**
     * Construct the name of a resource that could contain the source code of
     * the class with the given name.
     * <p>
     * Notice that member types are declared inside a different type, so the relevant source file
     * is that of the outermost declaring class.
     * 
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return the name of the resource, e.g. "pkg1/pkg2/Outer.java"
     */
    public static String getSourceResourceName(String className) {

        // Strip nested type suffixes.
        {
            int idx = className.lastIndexOf('.') + 1;
            idx = className.indexOf('$', idx);
            if (idx != -1) className = className.substring(0, idx);
        }

        return className.replace('.', '/') + ".java";
    }

    /**
     * Construct the name of a resource that could contain the class file of the
     * class with the given name.
     * 
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return the name of the resource, e.g. "pkg1/pkg2/Outer$Inner.class"
     */
    public static String getClassFileResourceName(String className) {
        return className.replace('.', '/') + ".class";
    }

    /**
     * Return the byte code of this {@link ClassFile} as a byte array.
     */
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            this.store(baos);
        } catch (IOException ex) {
            // ByteArrayOutputStream should never throw IOExceptions.
            throw new RuntimeException(ex.toString());
        }
        return baos.toByteArray();
    }

    private static final int CLASS_FILE_MAGIC = 0xcafebabe;

    public final static short MAJOR_VERSION_JDK_1_1 = 45; 
    public final static short MINOR_VERSION_JDK_1_1 = 3; 
    public final static short MAJOR_VERSION_JDK_1_2 = 46;
    public final static short MINOR_VERSION_JDK_1_2 = 0;
    public final static short MAJOR_VERSION_JDK_1_3 = 47;
    public final static short MINOR_VERSION_JDK_1_3 = 0;
    public final static short MAJOR_VERSION_JDK_1_4 = 48;
    public final static short MINOR_VERSION_JDK_1_4 = 0;
    public final static short MAJOR_VERSION_JDK_1_5 = 49;
    public final static short MINOR_VERSION_JDK_1_5 = 0;

    private short        majorVersion;
    private short        minorVersion;
    public /*final*/ List    constantPool; // ConstantPoolInfo
    public /*final*/ short   accessFlags;
    public /*final*/ short   thisClass;
    public /*final*/ short   superclass;
    public /*final*/ short[] interfaces;
    public /*final*/ List    fieldInfos;   // FieldInfo
    public /*final*/ List    methodInfos;  // MethodInfo
    private /*final*/ List   attributes;   // AttributeInfo

    // Convenience.
    private /*final*/ Map constantPoolMap; // ConstantPoolInfo => Short

    public static abstract class ConstantPoolInfo {
        abstract public void store(DataOutputStream dos) throws IOException;
        abstract public boolean isWide();

        private static ConstantPoolInfo loadConstantPoolInfo(DataInputStream dis) throws IOException {
            byte tag = dis.readByte();
//System.out.println("tag=" + tag);
            switch (tag) {

            case 7:
                return new ConstantClassInfo(dis.readShort());

            case 9:
                return new ConstantFieldrefInfo(dis.readShort(), dis.readShort());

            case 10:
                return new ConstantMethodrefInfo(dis.readShort(), dis.readShort());

            case 11:
                return new ConstantInterfaceMethodrefInfo(dis.readShort(), dis.readShort());

            case 8:
                return new ConstantStringInfo(dis.readShort());

            case 3:
                return new ConstantIntegerInfo(dis.readInt());

            case 4:
                return new ConstantFloatInfo(dis.readFloat());

            case 5:
                return new ConstantLongInfo(dis.readLong());

            case 6:
                return new ConstantDoubleInfo(dis.readDouble());

            case 12:
                return new ConstantNameAndTypeInfo(dis.readShort(), dis.readShort());

            case 1:
                return new ConstantUtf8Info(dis.readUTF());

            default:
                throw new ClassFormatError("Invalid constant pool tag " + tag);
            }
        }
    }

    public static abstract class ConstantValuePoolInfo extends ConstantPoolInfo {
        public abstract Object getValue(ClassFile classFile);
    }

    public static class ConstantClassInfo extends ConstantPoolInfo {
        private final short nameIndex;

        public ConstantClassInfo(short nameIndex) { this.nameIndex = nameIndex; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(7);
            dos.writeShort(this.nameIndex);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantClassInfo && ((ConstantClassInfo) o).nameIndex == this.nameIndex;
        }
        public int hashCode() { return this.nameIndex; }
    }
    public static class ConstantFieldrefInfo extends ConstantPoolInfo {
        private final short classIndex;
        private final short nameAndTypeIndex;

        public ConstantFieldrefInfo(short classIndex, short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }

        public short getNameAndTypeIndex() { return this.nameAndTypeIndex; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(9);
            dos.writeShort(this.classIndex);
            dos.writeShort(this.nameAndTypeIndex);
        }
        public boolean equals(Object o) {
            return (
                o instanceof ConstantFieldrefInfo &&
                ((ConstantFieldrefInfo) o).classIndex       == this.classIndex &&
                ((ConstantFieldrefInfo) o).nameAndTypeIndex == this.nameAndTypeIndex
            );
        }
        public int hashCode() { return this.classIndex + (this.nameAndTypeIndex << 16); }
    }
    public static class ConstantMethodrefInfo extends ConstantPoolInfo {
        private final short classIndex;
        private final short nameAndTypeIndex;

        public ConstantMethodrefInfo(short classIndex, short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }

        public short getNameAndTypeIndex() { return this.nameAndTypeIndex; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(10);
            dos.writeShort(this.classIndex);
            dos.writeShort(this.nameAndTypeIndex);
        }
        public boolean equals(Object o) {
            return (
                o instanceof ConstantMethodrefInfo &&
                ((ConstantMethodrefInfo) o).classIndex       == this.classIndex &&
                ((ConstantMethodrefInfo) o).nameAndTypeIndex == this.nameAndTypeIndex
            );
        }
        public int hashCode() { return this.classIndex + (this.nameAndTypeIndex << 16); }
    }
    public static class ConstantInterfaceMethodrefInfo extends ConstantPoolInfo {
        private final short classIndex;
        private final short nameAndTypeIndex;

        public ConstantInterfaceMethodrefInfo(short classIndex, short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }

        public short getNameAndTypeIndex() { return this.nameAndTypeIndex; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(11);
            dos.writeShort(this.classIndex);
            dos.writeShort(this.nameAndTypeIndex);
        }
        public boolean equals(Object o) {
            return (
                o instanceof ConstantInterfaceMethodrefInfo &&
                ((ConstantInterfaceMethodrefInfo) o).classIndex ==       this.classIndex &&
                ((ConstantInterfaceMethodrefInfo) o).nameAndTypeIndex == this.nameAndTypeIndex
            );
        }
        public int hashCode() { return this.classIndex + (this.nameAndTypeIndex << 16); }
    }
    static class ConstantStringInfo extends ConstantValuePoolInfo {
        private final short stringIndex;

        public ConstantStringInfo(short stringIndex) {
            this.stringIndex = stringIndex;
        }

        // Implement ConstantValuePoolInfo.
        public Object getValue(ClassFile classFile) {
            return classFile.getConstantUtf8(this.stringIndex);
        }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(8);
            dos.writeShort(this.stringIndex);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantStringInfo && ((ConstantStringInfo) o).stringIndex == this.stringIndex;
        }
        public int hashCode() { return this.stringIndex; }
    }
    private static class ConstantIntegerInfo extends ConstantValuePoolInfo {
        private final int value;

        public ConstantIntegerInfo(int value) {
            this.value = value;
        }

        // Implement ConstantValuePoolInfo.
        public Object getValue(ClassFile classFile) { return new Integer(this.value); }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(3);
            dos.writeInt(this.value);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantIntegerInfo && ((ConstantIntegerInfo) o).value == this.value;
        }
        public int hashCode() { return this.value; }
    }
    private static class ConstantFloatInfo extends ConstantValuePoolInfo {
        private final float value;

        public ConstantFloatInfo(float value) {
            this.value = value;
        }

        // Implement ConstantValuePoolInfo.
        public Object getValue(ClassFile classFile) { return new Float(this.value); }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(4);
            dos.writeFloat(this.value);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantFloatInfo && ((ConstantFloatInfo) o).value == this.value;
        }
        public int hashCode() { return Float.floatToIntBits(this.value); }
    }
    private static class ConstantLongInfo extends ConstantValuePoolInfo {
        private final long value;

        public ConstantLongInfo(long value) {
            this.value = value;
        }

        // Implement ConstantValuePoolInfo.
        public Object getValue(ClassFile classFile) { return new Long(this.value); }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return true; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(5);
            dos.writeLong(this.value);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantLongInfo && ((ConstantLongInfo) o).value == this.value;
        }
        public int hashCode() { return (int) this.value ^ (int) (this.value >> 32); }
    }
    private static class ConstantDoubleInfo extends ConstantValuePoolInfo {
        private final double value;

        public ConstantDoubleInfo(double value) {
            this.value = value;
        }

        // Implement ConstantValuePoolInfo.
        public Object getValue(ClassFile classFile) { return new Double(this.value); }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return true; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(6);
            dos.writeDouble(this.value);
        }
        public boolean equals(Object o) {
            return o instanceof ConstantDoubleInfo && ((ConstantDoubleInfo) o).value == this.value;
        }
        public int hashCode() {
            long bits = Double.doubleToLongBits(this.value);
            return (int) bits ^ (int) (bits >> 32);
        }
    }
    public static class ConstantNameAndTypeInfo extends ConstantPoolInfo {
        private final short nameIndex;
        private final short descriptorIndex;

        public ConstantNameAndTypeInfo(short nameIndex, short descriptorIndex) {
            this.nameIndex       = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }

        public short getDescriptorIndex() { return this.descriptorIndex; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(12);
            dos.writeShort(this.nameIndex);
            dos.writeShort(this.descriptorIndex);
        }
        public boolean equals(Object o) {
            return (
                o instanceof ConstantNameAndTypeInfo &&
                ((ConstantNameAndTypeInfo) o).nameIndex       == this.nameIndex &&
                ((ConstantNameAndTypeInfo) o).descriptorIndex == this.descriptorIndex
            );
        }
        public int hashCode() { return this.nameIndex + (this.descriptorIndex << 16); }
    }
    public static class ConstantUtf8Info extends ConstantPoolInfo {
        private final String s;

        public ConstantUtf8Info(String s) {
            if (s == null) throw new RuntimeException();
            this.s = s;
        }

        public String getString() { return this.s; }

        // Implement ConstantPoolInfo.
        public boolean isWide() { return false; }
        public void store(DataOutputStream dos) throws IOException {
            dos.writeByte(1);
            try {
                dos.writeUTF(this.s);
            } catch (UTFDataFormatException e) {
                throw new ClassFormatError("String constant too long to store in class file");
            }
        }
        public boolean equals(Object o) {
            return o instanceof ConstantUtf8Info && ((ConstantUtf8Info) o).s.equals(this.s);
        }
        public int hashCode() { return this.s.hashCode(); }
    }

    /**
     * This class represents a "method_info" structure, as defined by the
     * JVM specification.
     */
    public class MethodInfo {
        private final short accessFlags;
        private final short nameIndex;
        private final short descriptorIndex;
        private final List  attributes; // AttributeInfo

        /**
         * Initialize the "method_info" structure.
         */
        public MethodInfo(
            short accessFlags,
            short nameIndex,
            short descriptorIndex,
            List  attributes
        ) {
            this.accessFlags     = accessFlags;
            this.nameIndex       = nameIndex;
            this.descriptorIndex = descriptorIndex;
            this.attributes      = attributes;
        }

        public ClassFile getClassFile() { return ClassFile.this; }

        public short           getAccessFlags()     { return this.accessFlags; }
        public short           getNameIndex()       { return this.nameIndex; }
        public short           getDescriptorIndex() { return this.descriptorIndex; }
        public AttributeInfo[] getAttributes()      { return (AttributeInfo[]) this.attributes.toArray(new AttributeInfo[this.attributes.size()]); }

        public void addAttribute(AttributeInfo attribute) {
            this.attributes.add(attribute);
        }

        /**
         * Write this object to a {@link DataOutputStream}, in the format
         * defined by the JVM specification.
         */
        public void store(DataOutputStream dos) throws IOException {
            dos.writeShort(this.accessFlags);                // access_flags
            dos.writeShort(this.nameIndex);                  // name_index
            dos.writeShort(this.descriptorIndex);            // descriptor_index
            ClassFile.storeAttributes(dos, this.attributes); // attributes_count, attributes
        }
    }

    private MethodInfo loadMethodInfo(DataInputStream dis) throws IOException {
        return new MethodInfo(
            dis.readShort(),         // accessFlags
            dis.readShort(),         // nameIndex
            dis.readShort(),         // descriptorIndex
            this.loadAttributes(dis) // attributes
        );
    }

    public static class FieldInfo {
        public FieldInfo(
            short accessFlags,
            short nameIndex,
            short descriptorIndex,
            List  attributes
        ) {
            this.accessFlags     = accessFlags;
            this.nameIndex       = nameIndex;
            this.descriptorIndex = descriptorIndex;
            this.attributes      = attributes;
        }

        public short           getAccessFlags()     { return this.accessFlags; }
        public short           getNameIndex()       { return this.nameIndex; }
        public short           getDescriptorIndex() { return this.descriptorIndex; }
        public AttributeInfo[] getAttributes()      { return (AttributeInfo[]) this.attributes.toArray(new AttributeInfo[this.attributes.size()]); }

        public void addAttribute(AttributeInfo attribute) {
            this.attributes.add(attribute);
        }

        public void store(DataOutputStream dos) throws IOException {
            dos.writeShort(this.accessFlags);                // access_flags
            dos.writeShort(this.nameIndex);                  // name_index
            dos.writeShort(this.descriptorIndex);            // descriptor_index
            ClassFile.storeAttributes(dos, this.attributes); // attibutes_count, attributes
        }

        private final short accessFlags;
        private final short nameIndex;
        private final short descriptorIndex;
        private final List  attributes; // AttributeInfo
    }

    /**
     * Representation of a class file attribute (see JVMS 4.7).
     */
    public abstract static class AttributeInfo {
        public AttributeInfo(short nameIndex) {
            this.nameIndex = nameIndex;
        }
        public void store(DataOutputStream dos) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            this.storeBody(new DataOutputStream(baos));

            dos.writeShort(this.nameIndex); // attribute_name_index;
            dos.writeInt(baos.size());      // attribute_length
            baos.writeTo(dos);              // info
        }
        protected abstract void storeBody(DataOutputStream dos) throws IOException;

        private final short nameIndex;
    }

    /**
     * Load one class file attribute. The returned object will be of
     * {@link AttributeInfo}-derived type, depending on the attribute's name; e.g. if the
     * name of the attribute is "SourceFile", then the returned object will be of type
     * {@link SourceFileAttribute}.
     */
    /*private*/ AttributeInfo loadAttribute(DataInputStream dis) throws IOException {
        short attributeNameIndex = dis.readShort(); // attribute_name_index
        int   attributeLength    = dis.readInt();   // attribute_length

        final byte[] ba = new byte[attributeLength];
        dis.readFully(ba);
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream      bdis = new DataInputStream(bais);

        String attributeName = this.getConstantUtf8(attributeNameIndex);
        AttributeInfo result;
        if ("ConstantValue".equals(attributeName)) {
            result = ConstantValueAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("Code".equals(attributeName)) {
            result = CodeAttribute.loadBody(attributeNameIndex, this, bdis);
        } else
        if ("Exceptions".equals(attributeName)) {
            result = ExceptionsAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("InnerClasses".equals(attributeName)) {
            result = InnerClassesAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("Synthetic".equals(attributeName)) {
            result = SyntheticAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("SourceFile".equals(attributeName)) {
            result = SourceFileAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("LineNumberTable".equals(attributeName)) {
            result = LineNumberTableAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("LocalVariableTable".equals(attributeName)) {
            result = LocalVariableTableAttribute.loadBody(attributeNameIndex, bdis);
        } else
        if ("Deprecated".equals(attributeName)) {
            result = DeprecatedAttribute.loadBody(attributeNameIndex, bdis);
        } else
        {
            return new AttributeInfo(attributeNameIndex) {
                protected void storeBody(DataOutputStream dos) throws IOException { dos.write(ba); }
            };
        }

        if (bais.available() > 0) throw new ClassFormatError((ba.length - bais.available()) + " bytes of trailing garbage in body of attribute \"" + attributeName + "\"");

        return result;
    }

    /**
     * Representation of a "ConstantValue" attribute (see JVMS 4.7.2).
     */
    public static class ConstantValueAttribute extends AttributeInfo {
        private final short constantValueIndex;

        ConstantValueAttribute(short attributeNameIndex, short constantValueIndex) {
            super(attributeNameIndex);
            this.constantValueIndex = constantValueIndex;
        }

        public short getConstantValueIndex() { return this.constantValueIndex; }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            return new ConstantValueAttribute(
                attributeNameIndex, // attributeNameIndex
                dis.readShort()     // constantValueIndex
            );
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.constantValueIndex);
        }
    }

    /**
     * Representation of an "Exceptions" attribute (see JVMS 4.7.4).
     */
    public static class ExceptionsAttribute extends AttributeInfo {
        private final short[] exceptionIndexes;

        public ExceptionsAttribute(short attributeNameIndex, short[] exceptionIndexes) {
            super(attributeNameIndex);
            this.exceptionIndexes = exceptionIndexes;
        }

        public short[] getExceptionIndexes() {
            short[] eis = new short[this.exceptionIndexes.length];
            System.arraycopy(this.exceptionIndexes, 0, eis, 0, eis.length);
            return eis;
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            return new ExceptionsAttribute(
                attributeNameIndex,           // attributeNameIndex
                ClassFile.readShortArray(dis) // exceptionIndexes
            );
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            ClassFile.storeShortArray(dos, this.exceptionIndexes);
        }
    }

    /**
     * Representation of an "InnerClasses" attribute (see JVMS 4.7.5).
     */
    public static class InnerClassesAttribute extends AttributeInfo {
        private final List entries; // InnerClassesAttribute.Entry

        InnerClassesAttribute(short attributeNameIndex) {
            super(attributeNameIndex);
            this.entries = new ArrayList();
        }
        InnerClassesAttribute(short attributeNameIndex, Entry[] entries) {
            super(attributeNameIndex);
            this.entries = new ArrayList(Arrays.asList(entries));
        }

        public List getEntries() {
            return this.entries;
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            Entry[] ics = new Entry[dis.readShort()]; // number_of_classes
            for (short i = 0; i < ics.length; ++i) {  // classes
                ics[i] = new InnerClassesAttribute.Entry(
                    dis.readShort(), // innerClassInfoIndex
                    dis.readShort(), // outerClassInfoIndex
                    dis.readShort(), // innerNameIndex
                    dis.readShort()  // innerClassAccessFlags
                );
            }
            return new InnerClassesAttribute(attributeNameIndex, ics);
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.entries.size());
            for (Iterator it = this.entries.iterator(); it.hasNext();) {
                Entry e = (Entry) it.next();
                dos.writeShort(e.innerClassInfoIndex);
                dos.writeShort(e.outerClassInfoIndex);
                dos.writeShort(e.innerNameIndex);
                dos.writeShort(e.innerClassAccessFlags);
            }
        }

        public static class Entry {
            public final short innerClassInfoIndex, outerClassInfoIndex, innerNameIndex, innerClassAccessFlags;

            public Entry(short innerClassInfoIndex, short outerClassInfoIndex, short innerNameIndex, short innerClassAccessFlags) {
                this.innerClassInfoIndex   = innerClassInfoIndex;
                this.outerClassInfoIndex   = outerClassInfoIndex;
                this.innerNameIndex        = innerNameIndex;
                this.innerClassAccessFlags = innerClassAccessFlags;
            }
        }
    }

    /**
     * Representation of a "Synthetic" attribute (see JVMS 4.7.6).
     */
    public static class SyntheticAttribute extends AttributeInfo {
        SyntheticAttribute(short attributeNameIndex) {
            super(attributeNameIndex);
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) {
            return new SyntheticAttribute(
                attributeNameIndex // attributeNameIndex
            );
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
        }
    }

    /**
     * Representation of a "SourceFile" attribute (see JVMS 4.7.7).
     */
    public static class SourceFileAttribute extends AttributeInfo {
        private final short sourceFileIndex;

        public SourceFileAttribute(short attributeNameIndex, short sourceFileIndex) {
            super(attributeNameIndex);
            this.sourceFileIndex = sourceFileIndex;
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            return new SourceFileAttribute(
                attributeNameIndex, // attributeNameIndex
                dis.readShort()     // sourceFileNameIndex
            );
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.sourceFileIndex);
        }
    }

    /**
     * Representation of a "LineNumberTable" attribute (see JVMS 4.7.8).
     */
    public static class LineNumberTableAttribute extends AttributeInfo {
        private final Entry[] entries;

        public LineNumberTableAttribute(short attributeNameIndex, Entry[] entries) {
            super(attributeNameIndex);
            this.entries = entries;
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            Entry[] lntes = new Entry[dis.readShort()]; // line_number_table_length
            for (short i = 0; i < lntes.length; ++i) {  // line_number_table
                lntes[i] = new LineNumberTableAttribute.Entry(
                    dis.readShort(), // startPC
                    dis.readShort()  // lineNumber
                );
            }
            return new LineNumberTableAttribute(attributeNameIndex, lntes);
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.entries.length);            // line_number_table_length
            for (int i = 0; i < this.entries.length; ++i) {
                dos.writeShort(this.entries[i].startPC);
                dos.writeShort(this.entries[i].lineNumber);
            }
        }

        public static class Entry {
            public final short startPC, lineNumber;
            public Entry(short startPC, short lineNumber) {
                this.startPC    = startPC;
                this.lineNumber = lineNumber;
            }
        }
    }

    /**
     * Representation of a "LocalVariableTable" attribute (see JVMS 4.7.9).
     */
    public static class LocalVariableTableAttribute extends AttributeInfo {
        private final Entry[] entries;

        LocalVariableTableAttribute(short attributeNameIndex, Entry[] entries) {
            super(attributeNameIndex);
            this.entries = entries;
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) throws IOException {
            short localVariableTableLength = dis.readShort();
            Entry[] lvtes = new Entry[localVariableTableLength];   // local_variable_table_length
            for (short i = 0; i < localVariableTableLength; ++i) { // local_variable_table
                lvtes[i] = new LocalVariableTableAttribute.Entry(
                    dis.readShort(), // startPC
                    dis.readShort(), // length
                    dis.readShort(), // nameIndex
                    dis.readShort(), // descriptorIndex
                    dis.readShort()  // index
                );
            }
            return new LocalVariableTableAttribute(attributeNameIndex, lvtes);
        }
        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.entries.length);            // local_variable_table_length
            for (int i = 0; i < this.entries.length; ++i) { // local_variable_table
                Entry lnte = this.entries[i];
                dos.writeShort(lnte.startPC);         // start_pc;
                dos.writeShort(lnte.length);          // length
                dos.writeShort(lnte.nameIndex);       // name_index
                dos.writeShort(lnte.descriptorIndex); // descriptor_index
                dos.writeShort(lnte.index);           // index
            }
        }

        public static class Entry {
            public final short startPC, length, nameIndex, descriptorIndex, index;

            public Entry(
                short startPC,
                short length,
                short nameIndex,
                short descriptorIndex,
                short index
            ) {
                this.startPC         = startPC;
                this.length          = length;
                this.nameIndex       = nameIndex;
                this.descriptorIndex = descriptorIndex;
                this.index           = index;
            }
        }
    }

    /**
     * Representation of a "Deprecated" attribute (see JVMS 4.7.10).
     */
    public static class DeprecatedAttribute extends AttributeInfo {
        public DeprecatedAttribute(short attributeNameIndex) {
            super(attributeNameIndex);
        }

        private static AttributeInfo loadBody(short attributeNameIndex, DataInputStream dis) {
            return new DeprecatedAttribute(attributeNameIndex);
        }

        // Implement "AttributeInfo".
        protected void storeBody(DataOutputStream dos) throws IOException {
        }
    }

    /**
     * Representation of an unmodifiable "Code" attribute, as read from a class file.
     */
    private static class CodeAttribute extends AttributeInfo {
        private final short                 maxStack;
        private final short                 maxLocals;
        private final byte[]                code;
        private final ExceptionTableEntry[] exceptionTableEntries;
        private final AttributeInfo[]       attributes;

        private CodeAttribute(
            short                 attributeNameIndex,
            short                 maxStack,
            short                 maxLocals,
            byte[]                code,
            ExceptionTableEntry[] exceptionTableEntries,
            AttributeInfo[]       attributes
        ) {
            super(attributeNameIndex);
            this.maxStack              = maxStack;
            this.maxLocals             = maxLocals;
            this.code                  = code;
            this.exceptionTableEntries = exceptionTableEntries;
            this.attributes            = attributes;
        }

        public static AttributeInfo loadBody(short attributeNameIndex, ClassFile classFile, DataInputStream dis) throws IOException {
            short  maxStack = dis.readShort();                                     // max_stack
            short  maxLocals = dis.readShort();                                    // max_locals
            byte[] code = ClassFile.readLengthAndBytes(dis);                       // code_length, code

            ExceptionTableEntry[] etes = new ExceptionTableEntry[dis.readShort()]; // exception_table_length
            for (int i = 0; i < etes.length; ++i) {                                // exception_table
                etes[i] = new ExceptionTableEntry(
                    dis.readShort(), // startPC
                    dis.readShort(), // endPC
                    dis.readShort(), // handlerPC
                    dis.readShort()  // catchType
                );
            }

            AttributeInfo[] attributes = new AttributeInfo[dis.readShort()];       // attributes_count
            for (int i = 0; i < attributes.length; ++i) {                          // attributes
                attributes[i] = classFile.loadAttribute(dis);
            } 

            return new CodeAttribute(
                attributeNameIndex, // attributeNameIndex
                maxStack,           // maxStack
                maxLocals,          // maxLocals
                code,               // code
                etes,               // exceptionTableEntries
                attributes          // attributes
            );
        }

        protected void storeBody(DataOutputStream dos) throws IOException {
            dos.writeShort(this.maxStack);                                // max_stack
            dos.writeShort(this.maxLocals);                               // max_locals
            dos.writeInt(this.code.length);                               // code_length
            dos.write(this.code);                                         // code
            dos.writeShort(this.exceptionTableEntries.length);            // exception_table_length
            for (int i = 0; i < this.exceptionTableEntries.length; ++i) { // exception_table
                ExceptionTableEntry ete = this.exceptionTableEntries[i];
                dos.writeShort(ete.startPC  ); // start_pc
                dos.writeShort(ete.endPC    ); // end_pc
                dos.writeShort(ete.handlerPC); // handler_pc
                dos.writeShort(ete.catchType); // catch_type
            }
            dos.writeShort(this.attributes.length);                       // attributes_count
            for (int i = 0; i < this.attributes.length; ++i) {            // attributes
                this.attributes[i].store(dos);
            }
        }

        /**
         * Representation of an entry in the "exception_table" of a "Code" attribute (see JVMS
         * 4.7.3).
         */
        private static class ExceptionTableEntry {
            private final short startPC, endPC, handlerPC, catchType;

            public ExceptionTableEntry(
                short startPC,
                short endPC,
                short handlerPC,
                short catchType
            ) {
                this.startPC   = startPC;
                this.endPC     = endPC;
                this.handlerPC = handlerPC;
                this.catchType = catchType;
            }
        }
    }
}
