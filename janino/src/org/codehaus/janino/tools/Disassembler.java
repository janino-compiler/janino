
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino.tools;

import java.io.*;
import java.util.*;

/**
 * A Java bytecode disassembler, comparable to JAVAP, which is part of
 * Sun's JDK.
 * <p>
 * Notice that this tool does not depend on any other classes or libraries (other than the
 * standard JDK library).
 */

public class Disassembler {

    /**
     * Usage:
     * <pre>
     *   java new.janino.tools.Disassembler [ -o <i>output-file</i> ] [ -help ]
     * </pre>
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-') break;
            if (arg.equals("-o")) {
                Disassembler.out = new IndentPrintWriter(new FileOutputStream(args[++i]));
            } else
            if (arg.equals("-verbose")) {
                Disassembler.verbose = true;
            } else
            if (arg.equals("-help")) {
                System.out.println("Usage:  java jexpr.Jdisasm [ -o <output-file> ] [ -verbose ] <class-file> ...");
                System.exit(0);
            } else
            {
                System.err.println("Unrecognized command line option \"" + arg + "\"; try \"-help\".");
            }
        }
        if (i == args.length) {
            System.err.println("Class file name missing, try \"-help\".");
            System.exit(1);
        }
        try {
            for (; i < args.length; ++i) {
                Disassembler.disasm(args[i]);
            }
        } finally {
            out.flush();
        }
    }

    private Disassembler() {}

    /**
     * Disassemble one Java<sup>TM</sup> class file to {@link System#out}.
     * 
     * @param classFileName
     * @throws IOException
     */
    private static void disasm(String classFileName) throws IOException {
        FileInputStream fis = new FileInputStream(classFileName);
        try {
            DataInputStream dis = new DataInputStream(fis);
            Disassembler.disasmClassFile(dis);
            out.println();
        } finally {
            fis.close();
        }
    }

    private static void disasmClassFile(DataInputStream dis) throws IOException {
        out.println("ClassFile {");
        out.indent(); {
            out.println("magic = 0x" + Integer.toHexString(dis.readInt()));
            out.println("minor_version = " + dis.readShort());
            out.println("major_version = " + dis.readShort());

            short constantPoolCount = dis.readShort();
            constantPool = new ConstantPoolInfo[constantPoolCount];
            {
                short i = 1;
                while (i < constantPoolCount) {
                    ConstantPoolInfo cpi = readConstantPoolInfo(dis);
                    constantPool[i++] = cpi;
                    for (int j = cpi.getSizeInConstantPool(); j > 1; --j) {
                        constantPool[i++] = null;
                    }
                }
            }
            out.println("constant_pool[] = {");
            out.indent(); {
                for (short i = 1; i < constantPoolCount; ++i) {
                    out.print(i + ": ");
                    ConstantPoolInfo cpi = constantPool[i];
                    if (cpi == null) {
                        out.print("(null entry)");
                    } else {
                        cpi.print();
                    }
                    out.println(",");
                }
            } out.unindent();
            out.println("}");

            out.println("access_flags = " + decodeAccess(dis.readShort()));

            short thisClass = dis.readShort();
            out.print("this_class = " + thisClass + " (");
            out.indent(); {
                Disassembler.printConstantPoolEntry(thisClass);
            } out.unindent();
            out.println(")");

            short superClass = dis.readShort();
            out.print("super_class = " + superClass + " (");
            out.indent(); {
                Disassembler.printConstantPoolEntry(superClass);
            } out.unindent();
            out.println(")");

            out.println("interfaces[] = {");
            out.indent(); {
                short interfacesCount = dis.readShort();
                for (short i = 0; i < interfacesCount; ++i) {
                    short interfacE = dis.readShort();
                    out.print(interfacE + " (");
                    out.indent(); {
                        Disassembler.printConstantPoolEntry(interfacE);
                    } out.unindent();
                    out.println("),");
                }
            } out.unindent();
            out.println("}");

            out.println("fields[] = {");
            out.indent(); {
                short fieldsCount = dis.readShort();
                for (short i = 0; i < fieldsCount; ++i) {
                    disasmFieldInfo(dis);
                    out.println(",");
                }
            } out.unindent();
            out.println("}");

            out.println("methods[] = {");
            out.indent(); {
                short methodsCount = dis.readShort();
                for (short i = 0; i < methodsCount; ++i) {
                    disasmMethodInfo(dis);
                    out.println(",");
                }
            } out.unindent();
            out.println("}");

            out.println("attributes[] = {");
            out.indent(); {
                short attributesCount = dis.readShort();
                for (short i = 0; i < attributesCount; ++i) {
                    disasmAttributeInfo(dis);
                    out.println(",");
                }
            } out.unindent();
            out.println("}");
        } out.unindent();
        out.print("}");
    }

    private static ConstantPoolInfo readConstantPoolInfo(final DataInputStream dis) throws IOException {
        byte tag = dis.readByte();
        switch (tag) {
        case 7:
            {
                final short nameIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        if (verbose) {
                            out.print("CONSTANT_Class_info { name_index = " + nameIndex + " (");
                            Disassembler.printConstantPoolEntry(nameIndex);
                            out.print(") }");
                        } else {
                            Disassembler.printConstantPoolEntry(nameIndex);
                        }
                    }
                };
            }
        case 9:
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        if (verbose) {
                            out.println("CONSTANT_Fieldref_info {");
                            out.indent(); {
                                out.print("class_index = " + classIndex + " (");
                                Disassembler.printConstantPoolEntry(classIndex);
                                out.println(")");
    
                                out.print("name_and_type_index = " + nameAndTypeIndex + " (");
                                Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                                out.println(")");
                            } out.unindent();
                            out.print("}");
                        } else {
                            Disassembler.printConstantPoolEntry(classIndex);
                            out.print('.');
                            Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                        }
                    }
                };
            }
        case 10:
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        if (verbose) {
                            out.println("CONSTANT_Methodref_info {");
                            out.indent(); {
                                out.print("class_index = " + classIndex + " (");
                                Disassembler.printConstantPoolEntry(classIndex);
                                out.println(")");
    
                                out.print("name_and_type_index = " + nameAndTypeIndex + " (");
                                Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                                out.println(")");
                            } out.unindent();
                            out.print("}");
                        } else {
                            Disassembler.printConstantPoolEntry(classIndex);
                            out.print('.');
                            Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                        }
                    }
                };
            }
        case 11:
            {
                final short classIndex = dis.readShort();
                final short nameAndTypeIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        if (verbose) {
                            out.println("CONSTANT_InterfaceMethodref_info {");
                            out.indent(); {
                                out.print("class_index = " + classIndex + " (");
                                Disassembler.printConstantPoolEntry(classIndex);
                                out.println(")");
    
                                out.print("name_and_type_index = " + nameAndTypeIndex + " (");
                                Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                                out.println(")");
                            } out.unindent();
                            out.print("}");
                        } else {
                            Disassembler.printConstantPoolEntry(classIndex);
                            out.print('.');
                            Disassembler.printConstantPoolEntry(nameAndTypeIndex);
                        }
                    }
                };
            }
        case 8:
            {
                final short stringIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        ConstantPoolInfo cpi = Disassembler.constantPool[stringIndex];
                        String s;
                        if (!(cpi instanceof ConstantUtf8Info)) {
                            s = "STRING CONSTANT VALUE IS NOT UTF8";
                        } else {
                            s = ((ConstantUtf8Info) cpi).getValue();
                            if (s.length() > 80) {
                                s = Disassembler.stringToJavaLiteral(s.substring(0, 80)) + "... (" + s.length() + " chars)"; 
                            } else {
                                s = Disassembler.stringToJavaLiteral(s);
                            }
                        }
                        if (verbose) {
                            out.print("CONSTANT_String_info { string_index = " + stringIndex + " (" + s + ") }");
                        } else {
                            out.print(s);
                        }
                    }
                };
            }
        case 3:
            {
                final int bytes = dis.readInt();
                return new ConstantPoolInfo() {
                    public void print() {
                        out.print("CONSTANT_Integer_info { bytes = " + bytes + " }");
                    }
                };
            }
        case 4:
            {
                final float bytes = dis.readFloat();
                return new ConstantPoolInfo() {
                    public void print() {
                        out.print("CONSTANT_Float_info { bytes = " + bytes + " }");
                    }
                };
            }
        case 5:
            {
                final long bytes = dis.readLong();
                return new ConstantPoolInfo() {
                    public void print() {
                        out.print("CONSTANT_Long_info { high_bytes/low_bytes = " + bytes + " }");
                    }
                    public int getSizeInConstantPool() { return 2; }
                };
            }
        case 6:
            {
                final double bytes = dis.readDouble();
                return new ConstantPoolInfo() {
                    public void print() {
                        out.print("CONSTANT_Double_info { high_bytes/low_bytes = " + bytes + " }");
                    }
                    public int getSizeInConstantPool() { return 2; }
                };
            }
        case 12:
            {
                final short nameIndex = dis.readShort();
                final short descriptorIndex = dis.readShort();
                return new ConstantPoolInfo() {
                    public void print() {
                        if (verbose) {
                            out.println("CONSTANT_NameAndType_info {");
                            out.indent(); {
                                out.print("name_index = " + nameIndex + " (");
                                Disassembler.printConstantPoolEntry(nameIndex);
                                out.println(")");
    
                                out.print("descriptor_index = " + descriptorIndex + " (");
                                Disassembler.printConstantPoolEntry(descriptorIndex);
                                out.println(")");
                            } out.unindent();
                            out.print("}");
                        } else {
                            Disassembler.printConstantPoolEntry(nameIndex);
                            out.print(' ');
                            Disassembler.printConstantPoolEntry(descriptorIndex);
                        }
                    }
                };
            }
        case 1:
            return new ConstantUtf8Info(dis.readUTF());
        default:
            throw new RuntimeException("Invalid cp_info tag \"" + (int) tag + "\"");
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

    private static void disasmFieldInfo(DataInputStream dis) throws IOException {
        out.println("field_info {");
        out.indent(); {
            out.println("access_flags = " + decodeAccess(dis.readShort()));

            short nameIndex = dis.readShort();
            out.print("name_index = " + nameIndex + " (");
            Disassembler.printConstantPoolEntry(nameIndex);
            out.println(")");

            short descriptorIndex = dis.readShort();
            out.print("descriptor_index = " + descriptorIndex + " (");
            Disassembler.printConstantPoolEntry(descriptorIndex);
            out.println(")");

            out.println("attributes[] = {");
            out.indent(); {
                short attributesCount = dis.readShort();
                for (short i = 0; i < attributesCount; ++i) {
                    disasmAttributeInfo(dis);
                    out.println(",");
                }
            } out.unindent();
            out.println("}");
        } out.unindent();
        out.print("}");
    }

    private static void disasmMethodInfo(DataInputStream dis) throws IOException {
        out.println("method_info {");
        out.indent(); {
            out.println("access_flags = " + decodeAccess(dis.readShort()));

            short nameIndex = dis.readShort();
            out.print("name_index = " + nameIndex + " (");
            out.indent(); {
                Disassembler.printConstantPoolEntry(nameIndex);
            } out.unindent();
            out.println(")");

            short descriptorIndex = dis.readShort();
            out.print("descriptor_index = " + descriptorIndex + " (");
            out.indent(); {
                Disassembler.printConstantPoolEntry(descriptorIndex);
            } out.unindent();
            out.println(")");

            out.println("attributes[] = {");
            out.indent(); {
                short attributesCount = dis.readShort();
                for (short i = 0; i < attributesCount; ++i) {
                    disasmAttributeInfo(dis);
                    out.println(",");
                }
            } out.unindent();
            out.println("}");
        } out.unindent();
        out.print("}");
    }

    private static void disasmAttributeInfo(DataInputStream dis) throws IOException {
        readAttributeInfo(dis).print();
    }

    private static AttributeInfo readAttributeInfo(DataInputStream dis) throws IOException {

        // Determine attribute name.
        short attributeNameIndex = dis.readShort();
        String attributeName;
        if ((attributeNameIndex & 0xffff) >= Disassembler.constantPool.length) {
            attributeName = "ATTRIBUTE NAME INDEX " + (attributeNameIndex & 0xffff) + " OUT OF RANGE";
        } else {
           ConstantPoolInfo cpi = Disassembler.constantPool[attributeNameIndex];
           if (!(cpi instanceof ConstantUtf8Info)) {
               attributeName = "ATTRIBUTE NAME IS NOT UTF8";
           } else {
               attributeName = ((ConstantUtf8Info) cpi).getValue();
           }
        }

        // Read attribute body into byte array and create a DataInputStream.
        int attributeLength = dis.readInt();
        final byte[] ba = new byte[attributeLength];
        if (dis.read(ba) != ba.length) throw new EOFException();
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis2 = new DataInputStream(bais);

        // Parse the attribute body.
        AttributeInfo res = readAttributeBody(attributeName, dis2);

        // Check for extraneous bytes.
        int av = bais.available();
        if (av > 0) throw new RuntimeException(av + " extraneous bytes in attribute");

        return res;
    }
    private static AttributeInfo readAttributeBody(
        final String          attributeName,
        final DataInputStream dis
    ) throws IOException {
        if (attributeName.equals("ConstantValue")) {
            final short constantValueIndex = dis.readShort();
            return new AttributeInfo() {
                public void print() {
                    out.print("\"ConstantValue\" attribute " + constantValueIndex + " (");
                    out.indent(); {
                        Disassembler.printConstantPoolEntry(constantValueIndex);
                    } out.unindent();
                    out.print(")");
                }
            };
        } else
        if (attributeName.equals("Code")) {
            final short                 maxStack       = dis.readShort();
            final short                 maxLocals      = dis.readShort();
            final byte[]                code           = readByteArray(dis, dis.readInt());
            final ExceptionTableEntry[] exceptionTable = readExceptionTable(dis);
            final AttributeInfo[]       attributes     = readAttributes(dis);
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"Code\"");
                        out.println("max_stack = " + maxStack);
                        out.println("max_locals = " + maxLocals);

                        out.println("code = { ");
                        out.indent(); {
                            LocalVariableTableAttribute localVariableTableAttribute = null;
                            for (int i = 0; i < attributes.length; ++i) {
                                AttributeInfo a = attributes[i];
                                if (a instanceof LocalVariableTableAttribute) {
                                    if (localVariableTableAttribute != null) throw new RuntimeException("Duplicate LocalVariableTable attribute");
                                    localVariableTableAttribute = (LocalVariableTableAttribute) a;
                                }
                            }
                            try {
                                disasmBytecode(
                                    new ByteArrayInputStream(code),
                                    localVariableTableAttribute
                                );
                            } catch (IOException ignored) {
                                ;
                            }
                        } out.unindent();
                        out.println("}");

                        out.println("exception_table = {");
                        out.indent(); {
                            for (int i = 0; i < exceptionTable.length; ++i) {
                                exceptionTable[i].print();
                                out.println(",");
                            }
                        } out.unindent();
                        out.println("}");

                        out.println("attributes[] = {");
                        out.indent(); {
                            for (int i = 0; i < attributes.length; ++i) {
                                attributes[i].print();
                                out.println(",");
                            }
                        } out.unindent();
                        out.println("}");
                    } out.unindent();
                    out.print("}");
                }
            };
        } else
        if (attributeName.equals("Exceptions")) {
            final short[] exceptionIndexTable = readShortArray(dis, dis.readShort());
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"Exceptions\"");
                        out.println("exception_index_table = {");
                        out.indent(); {
                            for (short i = 0; i < exceptionIndexTable.length; ++i) {
                                short exceptionIndex = exceptionIndexTable[i];
                                out.print(exceptionIndex + " (");
                                Disassembler.printConstantPoolEntry(exceptionIndex);
                                out.println("),");
                            }
                        } out.unindent();
                        out.println("}");
                    } out.unindent();
                    out.print("}");
                }
            };
        } else
        if (attributeName.equals("InnerClasses")) {
            final short[] data = readShortArray(dis, 4 * dis.readShort());
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"InnerClasses\"");
                        out.println("classes = {");
                        out.indent(); {
                            for (int i = 0; i < data.length; i += 4) {
                                out.println("{");
                                out.indent(); {
                                    short innerClassInfoIndex = data[i];
                                    out.print("inner_class_info_index = " + innerClassInfoIndex + " (");
                                    Disassembler.printConstantPoolEntry(innerClassInfoIndex);
                                    out.println("),");
    
                                    short outerClassInfoIndex = data[i + 1];
                                    out.print("outer_class_info_index = " + outerClassInfoIndex + " (");
                                    if (outerClassInfoIndex == 0) {
                                        out.print("not a member");
                                    } else {
                                        Disassembler.printConstantPoolEntry(outerClassInfoIndex);
                                    }
                                    out.println("),");
    
                                    short innerNameIndex = data[i + 2];
                                    out.print("inner_name_index = " + innerNameIndex + " (");
                                    if (innerNameIndex == 0) {
                                        out.print("anonymous");
                                    } else {
                                        Disassembler.printConstantPoolEntry(innerNameIndex);
                                    }
                                    out.println("),");
    
                                    out.println("inner_class_access_flags = " + decodeAccess(data[i + 3]));
                                } out.unindent();
                                out.println(i == data.length - 1 ? "}" : "},");
                            }
                        } out.unindent();
                        out.println("}");
                    } out.unindent();
                    out.print("}");
                }
            };
        } else
        if (attributeName.equals("Synthetic")) {
            return new AttributeInfo() {
                public void print() {
                    out.print("attribute_name = \"Synthetic\"");
                }
            };
        } else
        if (attributeName.equals("SourceFile")) {
            final short sourceFileIndex = dis.readShort();
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"SourceFile\"");
                        out.print("sourcefile_index = " + sourceFileIndex + " (");
                        Disassembler.printConstantPoolEntry(sourceFileIndex);
                        out.println(")");
                    } out.unindent();
                    out.print("}");
                }
            };
        } else
        if (attributeName.equals("LineNumberTable")) {
            final short[] data = readShortArray(dis, 2 * dis.readShort());
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"LineNumberTable\"");
                        out.println("line_number_table = {");
                        out.indent(); {
                            for (short i = 0; i < data.length; i += 2) {
                                out.println("start_pc = " + data[i] + ", line_number = " + data[i + 1]);
                            }
                        } out.unindent();
                        out.println("}");
                    } out.unindent();
                    out.print("}");
                }
            };
        } else
        if (attributeName.equals("LocalVariableTable")) {
            return new LocalVariableTableAttribute(readShortArray(dis, 5 * dis.readShort()));
        } else
        if (attributeName.equals("Deprecated")) {
            return new AttributeInfo() {
                public void print() {
                    out.print("attribute_name = \"Deprecated\"");
                }
            };
        } else
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (;;) {
                int c = dis.read();
                if (c == -1) break;
                baos.write(c);
            }
            return new AttributeInfo() {
                public void print() {
                    out.println("{");
                    out.indent(); {
                        out.println("attribute_name = \"" + attributeName + "\"");
                        out.print("info = { ");
                        for (int i = 0; i < info.length; ++i) {
                            out.print("0x" + Integer.toHexString(0xff & info[i]) + ", ");
                        }
                        out.println("}");
                    } out.unindent();
                    out.print("}");
                }
                private byte[] info          = baos.toByteArray();
            };
        }
    }
    private interface AttributeInfo {
        public void print();
    }
    private static class LocalVariableTableAttribute implements AttributeInfo {
        public LocalVariableTableAttribute(short[] data) { this.data = data; }
        public void print() {
            out.println("{");
            out.indent(); {
                out.println("attribute_name = \"LocalVariableTable\"");
                out.println("local_variable_table = {");
                out.indent(); {
                    for (int i = 0; i < data.length; i += 5) {
                        out.print("start_pc = " + data[i] + ", length = " + data[i + 1] + ", ");

                        short nameIndex = data[i + 2];
                        out.print("name_index = " + nameIndex + " (");
                        Disassembler.printConstantPoolEntry(nameIndex);
                        out.print("), ");

                        short descriptorIndex = data[i + 3];
                        out.print("descriptor_index = " + descriptorIndex + " (");
                        Disassembler.printConstantPoolEntry(descriptorIndex);
                        out.print("), ");

                        out.println("index = " + data[i + 4]);
                    }
                } out.unindent();
                out.println("}");
            } out.unindent();
            out.print("}");
        }
        public void findLocalVariable(short index, int instructionOffset) {
            for (int i = 0; i < data.length; i += 5) {
                short startPC = data[i];
                short length  = data[i + 1];
                if (
                    instructionOffset >= startPC &&
                    instructionOffset <= startPC + length &&
                    index == data[i + 4]
                ) {
                    short nameIndex       = data[i + 2];
                    short descriptorIndex = data[i + 3];
                    out.print("name_index = " + nameIndex + " (");
                    Disassembler.printConstantPoolEntry(nameIndex);
                    out.print("), ");
                    out.print("descriptor_index = " + descriptorIndex + " (");
                    Disassembler.printConstantPoolEntry(descriptorIndex);
                    out.print(")");
                    return;
                }
            }
            out.print("local variable name unknown");
        }
        private short[] data;
    }
    private static byte[] readByteArray(InputStream is, int size) throws IOException {
        byte[] res = new byte[size];
        if (is.read(res) != size) throw new EOFException("EOF in byte array");
        return res;
    }
    private static ExceptionTableEntry[] readExceptionTable(DataInputStream dis) throws IOException {
        ExceptionTableEntry[] res = new ExceptionTableEntry[dis.readShort()];
        for (short i = 0; i < res.length; ++i) {
            res[i] = new ExceptionTableEntry(
                dis.readShort(),
                dis.readShort(),
                dis.readShort(),
                dis.readShort()
            );
        }
        return res;
    }
    private static class ExceptionTableEntry {
        ExceptionTableEntry(short startPC, short endPC, short handlerPC, short catchType) {
            this.startPC   = startPC;
            this.endPC     = endPC;
            this.handlerPC = handlerPC;
            this.catchType = catchType;
        }
        public void print() {
            out.print("start_pc = " + startPC + ", end_pc = " + endPC + ", handler_pc = " + handlerPC + ", catch_type = " + catchType + " (");
            if (catchType == 0) {
                out.print("finally");
            } else {
                if (catchType < 0 || catchType >= constantPool.length) {
                    out.print("Constant pool index " + catchType + " out of range");
                } else {
                    Disassembler.printConstantPoolEntry(catchType);
                }
            }
            out.print(")");
        }
        private short startPC, endPC, handlerPC, catchType;
    }

    private static AttributeInfo[] readAttributes(DataInputStream dis) throws IOException {
        AttributeInfo[] res = new AttributeInfo[dis.readShort()];
        for (int i = 0; i < res.length; ++i) {
            res[i] = readAttributeInfo(dis);
        }
        return res;
    }

    private static short[] readShortArray(DataInputStream dis, int size) throws IOException {
        short[] res = new short[size];
        for (int i = 0; i < res.length; ++i) res[i] = dis.readShort();
        return res;
    }

    private static void disasmBytecode(
        InputStream                 is,
        LocalVariableTableAttribute localVariableTableAttribute
    ) throws IOException {
        CountingInputStream cis = new CountingInputStream(is);
        DataInputStream     dis = new DataInputStream(cis);

        for (;;) {
            int instructionOffset = (int) cis.getCount();

            int opcode = dis.read();
            if (opcode == -1) return; // EOF

            out.print(instructionOffset + ": ");

            Instruction instruction = opcodeToInstruction[opcode];
            if (instruction == null) {
                out.println("??? (invalid opcode \"" + opcode + "\")");
                continue;
            }
            disasmInstruction(instruction, dis, instructionOffset, localVariableTableAttribute);
            out.println();
        }
    }
    private static void disasmInstruction(
        Instruction                 instruction,
        DataInputStream             dis,
        int                         instructionOffset,
        LocalVariableTableAttribute localVariableTableAttribute
    ) throws IOException {
        out.print(instruction.getMnemonic());
        Operand[] operands = instruction.getOperands();
        if (operands != null) {
            for (int i = 0; i < operands.length; ++i) {
                Operand operand = operands[i];
                out.print(" ");
                operand.disasm(dis, instructionOffset, localVariableTableAttribute);
            }
        }
    }

    private static final String[] instructions = new String[] {
        "50  aaload",
        "83  aastore",
        "1   aconst_null",
        "25  aload         localvariablearrayindex1",
        "42  aload_0       localvariablearrayindex_0",
        "43  aload_1       localvariablearrayindex_1",
        "44  aload_2       localvariablearrayindex_2",
        "45  aload_3       localvariablearrayindex_3",
        "189 anewarray     constantpoolindex2",
        "176 areturn",
        "190 arraylength",
        "58  astore        localvariablearrayindex1",
        "75  astore_0      localvariablearrayindex_0",
        "76  astore_1      localvariablearrayindex_1",
        "77  astore_2      localvariablearrayindex_2",
        "78  astore_3      localvariablearrayindex_3",
        "191 athrow",
        "51  baload",
        "84  bastore",
        "16  bipush        signedbyte",
        "52  caload",
        "85  castore",
        "192 checkcast     constantpoolindex2",
        "144 d2f",
        "142 d2i",
        "143 d2l",
        "99  dadd",
        "49  daload",
        "82  dastore",
        "152 dcmpg",
        "151 dcmpl",
        "14  dconst_0",
        "15  dconst_1",
        "111 ddiv",
        "24  dload         localvariablearrayindex1",
        "38  dload_0       localvariablearrayindex_0",
        "39  dload_1       localvariablearrayindex_1",
        "40  dload_2       localvariablearrayindex_2",
        "41  dload_3       localvariablearrayindex_3",
        "107 dmul",
        "119 dneg",
        "115 drem",
        "175 dreturn",
        "57  dstore        localvariablearrayindex1",
        "71  dstore_0      localvariablearrayindex_0",
        "72  dstore_1      localvariablearrayindex_1",
        "73  dstore_2      localvariablearrayindex_2",
        "74  dstore_3      localvariablearrayindex_3",
        "103 dsub",
        "89  dup",
        "90  dup_x1",
        "91  dup_x2",
        "92  dup2",
        "93  dup2_x1",
        "94  dup2_x2",
        "141 f2d",
        "139 f2i",
        "140 f2l",
        "98  fadd",
        "48  faload",
        "81  fastore",
        "150 fcmpg",
        "149 fcmpl",
        "11  fconst_0",
        "12  fconst_1",
        "13  fconst_2",
        "110 fdiv",
        "23  fload         localvariablearrayindex1",
        "34  fload_0       localvariablearrayindex_0",
        "35  fload_1       localvariablearrayindex_1",
        "36  fload_2       localvariablearrayindex_2",
        "37  fload_3       localvariablearrayindex_3",
        "106 fmul",
        "118 fneg",
        "114 frem",
        "174 freturn",
        "56  fstore        localvariablearrayindex1",
        "67  fstore_0      localvariablearrayindex_0",
        "68  fstore_1      localvariablearrayindex_1",
        "69  fstore_2      localvariablearrayindex_2",
        "70  fstore_3      localvariablearrayindex_3",
        "102 fsub",
        "180 getfield      constantpoolindex2",
        "178 getstatic     constantpoolindex2",
        "167 goto          branchoffset2",
        "200 goto_w        branchoffset4",
        "145 i2b",
        "146 i2c",
        "135 i2d",
        "134 i2f",
        "133 i2l",
        "147 i2s",
        "96  iadd",
        "46  iaload",
        "126 iand",
        "79  iastore",
        "2   iconst_m1",
        "3   iconst_0",
        "4   iconst_1",
        "5   iconst_2",
        "6   iconst_3",
        "7   iconst_4",
        "8   iconst_5",
        "108 idiv",
        "165 if_acmpeq",
        "166 if_acmpne",
        "159 if_icmpeq     branchoffset2",
        "160 if_icmpne     branchoffset2",
        "161 if_icmplt     branchoffset2",
        "162 if_icmpge     branchoffset2",
        "163 if_icmpgt     branchoffset2",
        "164 if_icmple     branchoffset2",
        "153 ifeq          branchoffset2",
        "154 ifne          branchoffset2",
        "155 iflt          branchoffset2",
        "156 ifge          branchoffset2",
        "157 ifgt          branchoffset2",
        "158 ifle          branchoffset2",
        "199 ifnonnull     branchoffset2",
        "198 ifnull        branchoffset2",
        "132 iinc          localvariablearrayindex1 signedbyte",
        "21  iload         localvariablearrayindex1",
        "26  iload_0       localvariablearrayindex_0",
        "27  iload_1       localvariablearrayindex_1",
        "28  iload_2       localvariablearrayindex_2",
        "29  iload_3       localvariablearrayindex_3",
        "104 imul",
        "116 ineg",
        "193 instanceof      constantpoolindex2",
        "185 invokeinterface constantpoolindex2 signedbyte signedbyte",
        "183 invokespecial   constantpoolindex2",
        "184 invokestatic    constantpoolindex2",
        "182 invokevirtual   constantpoolindex2",
        "128 ior",
        "112 irem",
        "172 ireturn",
        "120 ishl",
        "122 ishr",
        "54  istore          localvariablearrayindex1",
        "59  istore_0        localvariablearrayindex_0",
        "60  istore_1        localvariablearrayindex_1",
        "61  istore_2        localvariablearrayindex_2",
        "62  istore_3        localvariablearrayindex_3",
        "100 isub",
        "124 iushr",
        "130 ixor",
        "168 jsr             branchoffset2",
        "201 jsr_w           branchoffset4",
        "138 l2d",
        "137 l2f",
        "136 l2i",
        "97  ladd",
        "47  laload",
        "127 land",
        "80  lastore",
        "148 lcmp",
        "9   lconst_0",
        "10  lconst_1",
        "18  ldc           constantpoolindex1",
        "19  ldc_w         constantpoolindex2",
        "20  ldc2_w        constantpoolindex2",
        "109 ldiv",
        "22  lload         localvariablearrayindex1",
        "30  lload_0       localvariablearrayindex_0",
        "31  lload_1       localvariablearrayindex_1",
        "32  lload_2       localvariablearrayindex_2",
        "33  lload_3       localvariablearrayindex_3",
        "105 lmul",
        "117 lneg",
        "171 lookupswitch  lookupswitch",
        "129 lor",
        "113 lrem",
        "173 lreturn",
        "121 lshl",
        "123 lshr",
        "55  lstore        localvariablearrayindex1",
        "63  lstore_0      localvariablearrayindex_0",
        "64  lstore_1      localvariablearrayindex_1",
        "65  lstore_2      localvariablearrayindex_2",
        "66  lstore_3      localvariablearrayindex_3",
        "101 lsub",
        "125 lushr",
        "131 lxor",
        "194 monitorenter",
        "195 monitorexit",
        "197 multianewarray constantpoolindex2 unsignedbyte",
        "187 new           constantpoolindex2",
        "188 newarray      atype",
        "0   nop",
        "87  pop",
        "88  pop2",
        "181 putfield      constantpoolindex2",
        "179 putstatic     constantpoolindex2",
        "169 ret           localvariablearrayindex1",
        "177 return",
        "53  saload",
        "86  sastore",
        "17  sipush        signedshort",
        "95  swap",
        "170 tableswitch   tableswitch",
        "196 wide          wide",
    };
    private static final String[] wideInstructions = new String[] {
        "21  iload         localvariablearrayindex2",
        "23  fload         localvariablearrayindex2",
        "25  aload         localvariablearrayindex2",
        "22  lload         localvariablearrayindex2",
        "24  dload         localvariablearrayindex2",
        "54  istore        localvariablearrayindex2",
        "56  fstore        localvariablearrayindex2",
        "58  astore        localvariablearrayindex2",
        "55  lstore        localvariablearrayindex2",
        "57  dstore        localvariablearrayindex2",
        "169 ret           localvariablearrayindex2",
        "132 iinc          localvariablearrayindex2 signedshort",
    };
    private static final Instruction[] opcodeToInstruction     = new Instruction[256];
    private static final Instruction[] opcodeToWideInstruction = new Instruction[256];
    static {
        compileInstructions(instructions, opcodeToInstruction);
        compileInstructions(wideInstructions, opcodeToWideInstruction);
    }
    private static void compileInstructions(String[] instructions, Instruction[] opcodeToInstruction) {
        for (int j = 0; j < instructions.length; ++j) {
            StringTokenizer st = new StringTokenizer(instructions[j]);
            String os = st.nextToken();
            int opcode = Integer.parseInt(os);
            String mnemonic = st.nextToken();
            Operand[] operands = null;
            if (st.hasMoreTokens()) {
                List l = new ArrayList();
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    Operand operand;
                    if (s.equals("constantpoolindex1")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                short index = (short) (0xff & dis.readByte());
                                Disassembler.printConstantPoolEntry(index);
                            }
                        };
                    } else
                    if (s.equals("constantpoolindex2")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                printConstantPoolEntry(dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex1")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                short index = dis.readByte();
                                out.print(index);
                                if (localVariableTableAttribute != null) {
                                    out.print(" (");
                                    localVariableTableAttribute.findLocalVariable(index, instructionOffset);
                                    out.print(")");
                                }
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex2")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                short index = dis.readShort();
                                out.print(index);
                                if (localVariableTableAttribute != null) {
                                    out.print(" (");
                                    localVariableTableAttribute.findLocalVariable(index, instructionOffset);
                                    out.print(")");
                                }
                            }
                        };
                    } else
                    if (s.startsWith("localvariablearrayindex_")) {
                        final short index = Short.parseShort(s.substring(s.length() - 1));
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                if (localVariableTableAttribute != null) {
                                    out.print("(");
                                    localVariableTableAttribute.findLocalVariable(index, instructionOffset);
                                    out.print(")");
                                }
                            }
                        };
                    } else
                    if (s.equals("branchoffset2")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                out.print(instructionOffset + dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("branchoffset4")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                out.print(instructionOffset + dis.readInt());
                            }
                        };
                    } else
                    if (s.equals("signedbyte")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                out.print(dis.readByte());
                            }
                        };
                    } else
                    if (s.equals("unsignedbyte")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                out.print(0xff & dis.readByte());
                            }
                        };
                    } else
                    if (s.equals("atype")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                byte b = dis.readByte();
                                out.print(
                                    b ==  4 ? "BOOLEAN" :
                                    b ==  5 ? "CHAR" :
                                    b ==  6 ? "FLOAT" :
                                    b ==  7 ? "DOUBLE" :
                                    b ==  8 ? "BYTE" :
                                    b ==  9 ? "SHORT" :
                                    b == 10 ? "INT" :
                                    b == 11 ? "LONG" :
                                    new Integer(0xff & b).toString()
                                );
                            }
                        };
                    } else
                    if (s.equals("signedshort")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                out.print(dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("tableswitch")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) if (dis.readByte() != (byte) 0) throw new RuntimeException("Non-zero pad byte in \"tableswitch\"");
                                out.print("default => " + (instructionOffset + dis.readInt()));
                                int low = dis.readInt();
                                int high = dis.readInt();
                                for (int i = low; i <= high; ++i) {
                                    int offset = dis.readInt();
                                    out.print(", " + i + " => " + (instructionOffset + offset));
                                }
                            }
                        };
                    } else
                    if (s.equals("lookupswitch")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte b = dis.readByte();
                                    if (b != (byte) 0) out.print("Padding byte #" + i + " is " + (b & 0xff));
                                } 
                                out.print("default => " + (instructionOffset + dis.readInt()));
                                int npairs = dis.readInt();
                                for (int i = 0; i < npairs; ++i) {
                                    int match  = dis.readInt();
                                    int offset = dis.readInt();
                                    out.print(", " + match + " => " + (instructionOffset + offset));
                                }
                            }
                        };
                    } else
                    if (s.equals("wide")) {
                        operand = new Operand() {
                            public void disasm(DataInputStream dis, int instructionOffset, LocalVariableTableAttribute localVariableTableAttribute) throws IOException {
                                int subopcode = 0xff & dis.readByte();
                                Instruction wideInstruction = opcodeToWideInstruction[subopcode];
                                if (wideInstruction == null) throw new RuntimeException("Invalid opcode " + subopcode + " after opcode WIDE");
                                disasmInstruction(wideInstruction, dis, instructionOffset, localVariableTableAttribute);
                            }
                        };
                    } else
                    {
                        throw new RuntimeException("Unknown operand \"" + s + "\"");
                    }
                    l.add(operand);
                }
                operands = (Operand[]) l.toArray(new Operand[l.size()]);
            }
            opcodeToInstruction[opcode] = new Instruction(mnemonic, operands);
        }
    }

    private static void printConstantPoolEntry(short index) {
        if (verbose) out.print(index + " (");
        if ((index & 0xffff) < constantPool.length) {
            ConstantPoolInfo cpi = constantPool[index];
            if (cpi == null) {
                out.print("NULL CONSTANT POOL ENTRY");
            } else {
                cpi.print();
            }
        } else {
            out.print("CONSTANT POOL INDEX OUT OF RANGE");
        }
        if (verbose) out.print(")");
    }

    private static class Instruction {

        /**
         * 
         * @param mnemonic
         * @param operands <code>null</code> is equivalent to "zero operands"
         */
        public Instruction(String mnemonic, Operand[] operands) {
            this.mnemonic = mnemonic;
            this.operands = operands;
        }
        public String    getMnemonic() { return mnemonic; }
        public Operand[] getOperands() { return operands; }

        private final String    mnemonic;
        private final Operand[] operands;
    }
    private interface Operand {
        void disasm(
            DataInputStream             dis,
            int                         instructionOffset,
            LocalVariableTableAttribute localVariableTableAttribute
        ) throws IOException;
    }

    private static IndentPrintWriter out = new IndentPrintWriter(System.out);
    private static boolean           verbose = false;

    /**
     * A variant of {@link PrintWriter} that allows for convenient indentation
     * of output lines.
     */
    private static class IndentPrintWriter extends PrintWriter {
        public IndentPrintWriter(OutputStream os) { super(os); }
        public void write(char[] cbuf, int off, int len) {
            this.handleIndentation();
            super.write(cbuf, off, len);
        }
        public void write(String str, int off, int len) {
            this.handleIndentation();
            super.write(str, off, len);
        }
        public void println() { super.println(); this.atBOL = true; }
        public void indent() { ++indentation; }
        public void unindent() { --indentation; }

        private void handleIndentation() {
            if (this.atBOL) {
                for (int i = 0; i < this.indentation; ++i) {
                    super.write(INDENTATION_CHARS, 0, INDENTATION_CHARS.length);
                } 
                this.atBOL = false;
            }
        }
        private final static char[] INDENTATION_CHARS = new char[] { ' ', ' ' };

        private boolean atBOL = true;
        private int      indentation = 0;
    }

    private abstract static class ConstantPoolInfo {
        public abstract void print();
        public int           getSizeInConstantPool() { return 1; }
    }
    private static class ConstantUtf8Info extends ConstantPoolInfo {
        public ConstantUtf8Info(String value) {
            this.value = value;
        }
        public void print() {
            out.print(this.value);
        }
        public String getValue() { return value; }
        private String value;
    }

    private static ConstantPoolInfo[] constantPool = null;

    private static class CountingInputStream extends InputStream {
        public CountingInputStream(InputStream is) { this.is = is; }
        public int read() throws IOException {
            int res = is.read();
            if (res != -1) ++count;
            return res;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res != -1) count += res;
            return res;
        }
        public long getCount() { return count; }

        private InputStream is;
        private long count = 0L;
    }

    private static String decodeAccess(short n) {
        StringBuffer sb = new StringBuffer();
        if ((n & 0x0007) == 0) { sb.append("package "           );               }
        if ((n & 0x0007) == 1) { sb.append("public "            ); n &= ~0x0007; }
        if ((n & 0x0007) == 2) { sb.append("private "           ); n &= ~0x0007; }
        if ((n & 0x0007) == 4) { sb.append("protected "         ); n &= ~0x0007; }
        if ((n & 0x0008) != 0) { sb.append("static "            ); n &= ~0x0008; }
        if ((n & 0x0010) != 0) { sb.append("final "             ); n &= ~0x0010; }
        if ((n & 0x0020) != 0) { sb.append("super/synchronized "); n &= ~0x0020; }
        if ((n & 0x0040) != 0) { sb.append("volatile "          ); n &= ~0x0040; }
        if ((n & 0x0080) != 0) { sb.append("transient "         ); n &= ~0x0080; }
        if ((n & 0x0100) != 0) { sb.append("native "            ); n &= ~0x0100; }
        if ((n & 0x0200) != 0) { sb.append("interface "         ); n &= ~0x0200; }
        if ((n & 0x0400) != 0) { sb.append("abstract "          ); n &= ~0x0400; }
        if ((n & 0x0800) != 0) { sb.append("strict "            ); n &= ~0x0800; }
        if (n != 0) sb.append("+ " + n + " ");
        return sb.substring(0, sb.length() - 1);
    }
}
