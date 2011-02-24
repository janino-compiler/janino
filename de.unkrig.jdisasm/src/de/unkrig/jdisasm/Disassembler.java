
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import de.unkrig.jdisasm.ClassFile.Attribute;
import de.unkrig.jdisasm.ClassFile.ExceptionTableEntry;
import de.unkrig.jdisasm.ClassFile.Field;
import de.unkrig.jdisasm.ClassFile.LineNumberTableAttribute;
import de.unkrig.jdisasm.ClassFile.LineNumberTableEntry;
import de.unkrig.jdisasm.ClassFile.LocalVariableTableEntry;

/**
 * A Java bytecode disassembler, comparable to JAVAP, which is part of SUN's JDK.
 * <p>
 * Notice that this tool does not depend on any other classes or libraries (other than the
 * standard JDK library).
 */
public class Disassembler {
    private PrintWriter    pw = new PrintWriter(System.out);
    private boolean        verbose = false;
    private File           sourceDirectory = new File(".");
    private boolean        hideLineNumbers;
    private HashSet<Short> branchTargets;

    public static void main(String[] args) throws IOException {
        Disassembler d = new Disassembler();
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-') break;
            if (arg.equals("-o")) {
                d.setOut(new FileOutputStream(args[++i]));
            } else
            if (arg.equals("-verbose")) {
                d.setVerbose(true);
            } else
            if (arg.equals("-src")) {
                d.setSourceDirectory(new File(args[++i]));
            } else
            if (arg.equals("-hide-line-numbers")) {
                d.setHideLineNumbers(true);
            } else
            if (arg.equals("-help")) {
                System.out.println("Usage:");
                System.out.println("  java " + Disassembler.class.getName() + " [ <option> ] ... <class-file> ...");
                System.out.println("Valid options are:");
                System.out.println("  -o <output-file>");
                System.out.println("  -verbose");
                System.out.println("  -src <source-dir>");
                System.out.println("  -hide-line-numbers");
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
        for (; i < args.length; ++i) {
            d.disasm(new File(args[i]));
        }
    }

    public Disassembler() {}

    public void setOut(OutputStream os) {
        this.pw = new PrintWriter(os);
    }
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }
    public void setHideLineNumbers(boolean hideLineNumbers) {
        this.hideLineNumbers = hideLineNumbers;
    }

    void print(String s)   { this.pw.print(s); }
    void println()         { this.pw.println(); }
    void println(String s) { this.pw.println(s); }

    /**
     * Disassemble one Java&trade; class file to {@link System#out}.
     */
    public void disasm(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            disasm(is);
        } finally {
            try { is.close(); } catch (IOException ex) { }
        }
    }

    public void disasm(InputStream is) throws IOException {
        try {
            this.disasmClassFile(new DataInputStream(is));
        } finally {
            pw.flush();
        }
    }

    private void disasmClassFile(DataInputStream dis) throws IOException {

        // Load the class file.
        ClassFile cf = new ClassFile(dis);

        // JDK version.
        this.println("// Class file version = " + cf.getJdkName());

        // Dump constant pool.
        if (this.verbose) {
            this.println("// Constant pool:");
            for (short i = 1; i < cf.getConstantPoolSize();) {
                ClassFile.ConstantPoolInfo cpi = cf.getConstantPoolEntry(i);
                this.println("//   " + i + ": " + cpi.toString());
                i += cpi.getSizeInConstantPool();
            }
        }

        String thisClassPackageName, thisClassSimpleName;
        {
            String thisClassName = cf.cpi(cf.thisClass);
            int periodIndex = thisClassName.lastIndexOf('.');
            thisClassPackageName = thisClassName.substring(0, periodIndex + 1);
            thisClassSimpleName = thisClassName.substring(periodIndex + 1);

        }

        // Package declaration.
        if (thisClassPackageName.length() > 0) {
            println("package " + thisClassPackageName.substring(0, thisClassPackageName.length() - 1) + ";");
        }

        {

            // Type declaration.
            this.print(
                decodeAccess(cf.accessFlags)
                + ((cf.accessFlags & Modifier.INTERFACE) == 0 ? "class " : "")
                + (cf.signatureAttribute != null ? Signature.decodeClassSignature(cf.cpi(cf.signatureAttribute.index), thisClassSimpleName) : thisClassSimpleName)
            );

            // EXTENDS clause.
            {
                String superClassName = cf.cpi(cf.superClass);
                if (!"Object".equals(superClassName)) {
                    this.print(" extends " + superClassName);
                }
            }

            // IMPLEMENTS clause.
            if (!cf.interfaces.isEmpty()) {
                Iterator<Short> it = cf.interfaces.iterator();
                this.print(" implements " + cf.cpi(it.next()));
                while (it.hasNext()) {
                    this.print(", " + cf.cpi(it.next()));
                }
            }

            this.println(" {");
        }

        // Fields.
        {
            List<String[]> lines = new ArrayList<String[]>();
            for (Field field : cf.fields) {

                // Pretty-print the field type.
                String parametrizedType = (
                    field.signatureAttribute == null
                    ? Descriptor.decodeFieldDescriptor(cf.cpi(field.descriptorIndex))
                    : Signature.decodeFieldTypeSignature(cf.cpi(field.signatureAttribute.index))
                );

                // Print the field declaration.
                if (field.constantValueAttribute == null) {
                    lines.add(new String[] {
                        "    " + decodeAccess(field.accessFlags) + parametrizedType + " ",
                        cf.cpi(field.nameIndex) + ";"
                    });
                } else {
                    lines.add(new String[] {
                        "    " + decodeAccess(field.accessFlags) + parametrizedType + " ",
                        cf.cpi(field.nameIndex),
                        " = " + cf.cpi(field.constantValueAttribute.constantValueIndex) + ";"
                    });
                }
            }
            println(lines);
        }

        // Read source file.
        Map<Short, String> sourceLines = new HashMap<Short, String>();
        READ_SOURCE_LINES:
        if (cf.sourceFileAttribute != null) {
            String sourceFile = cf.cpi(cf.sourceFileAttribute.sourceFileIndex);
            LineNumberReader lnr;
            try {
                lnr = new LineNumberReader(new FileReader(new File(this.sourceDirectory, sourceFile)));
            } catch (FileNotFoundException ex) {
                ;
                break READ_SOURCE_LINES;
            }
            try {
                for (;;) {
                    String sl = lnr.readLine();
                    if (sl == null) break;
                    sourceLines.put((short) lnr.getLineNumber(), sl);
                }
            } finally {
                lnr.close();
            }

        }

        // Methods.
        for (ClassFile.Method m : cf.methods) {
            String functionName = cf.cpi(m.nameIndex);
            String functionDescriptor = cf.cpi(m.descriptorIndex);
            Disassembler.this.print(
                "    "
                + decodeAccess(m.accessFlags)
                + (
                    m.signatureAttribute == null
                    ? Descriptor.returnType(functionDescriptor) + " " + functionName + Descriptor.parameters(functionDescriptor)
                    : Signature.decodeMethodTypeSignature(cf.cpi(m.signatureAttribute.index), functionName, thisClassSimpleName)
                )
            );
            if (m.exceptionsAttribute != null) {
                List<Short> exceptions = m.exceptionsAttribute.exceptions;
                if (!exceptions.isEmpty()) {
                    Iterator<Short> it = exceptions.iterator();
                    print(" throws " + cf.cpi(it.next()));
                    while (it.hasNext()) {
                        print(", " + cf.cpi(it.next()));
                    }
                }
            }
            if (m.codeAttribute == null) {
                println(";");
            } else {
                println(" {");
                ClassFile.CodeAttribute ca = m.codeAttribute;
                if (verbose) {
                    Disassembler.this.println("        // max_stack = " + ca.maxStack);
                    Disassembler.this.println("        // max_locals = " + ca.maxLocals);
                }

                try {
                    disasmBytecode(
                        new ByteArrayInputStream(ca.code),
                        ca.exceptionTable,
                        ca.localVariableTableAttribute,
                        ca.lineNumberTableAttribute,
                        sourceLines,
                        cf
                    );
                } catch (IOException ignored) {
                    ;
                }

            }
            if (verbose) {
                for (Attribute a : m.attributes) {
                    println("        // " + a);
                }
            }
            println("    }");

        }

        if (verbose) {
            for (Attribute a : cf.attributes) {
                println("    // " + a.toString());
            }
        }
        println("}");
    }

    private void println(List<String[]> lines) {
        int maxLen[] = new int[10];
        for (String[] line : lines) {
            for (int i = 0; i < line.length; ++i) {
                String column = line[i];
                if (column == null) continue;
                int len = column.length();
                if (len > maxLen[i]) {
                    maxLen[i] = len;
                }
            }
        }
        for (String[] line : lines) {
            for (int i = 0; i < line.length - 1; ++i) {
                String column = line[i];
                print(column);
                print("                                                                                 ".substring(0, maxLen[i] - column.length()));
            }
            println(line[line.length - 1]);
        }
    }

    /**
     * Prints the disassembled byte code.
     * @param exceptionTable TODO
     */
    private void disasmBytecode(
        InputStream                           is,
        List<ExceptionTableEntry>             exceptionTable,
        ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
        ClassFile.LineNumberTableAttribute    lineNumberTableAttribute,
        Map<Short, String>                    sourceLines,
        ClassFile                             cf
    ) throws IOException {
        CountingInputStream cis = new CountingInputStream(is);
        DataInputStream     dis = new DataInputStream(cis);

        this.branchTargets = new HashSet<Short>();
        try {

            SortedMap<Short, String> lines = new TreeMap<Short, String>();
            for (;;) {
                short instructionOffset = (short) cis.getCount();
    
                int opcode = dis.read();
                if (opcode == -1) break; // EOF
    
                Instruction instruction = opcodeToInstruction[opcode];
                if (instruction == null) {
                    this.println("??? (invalid opcode \"" + opcode + "\")");
                    continue;
                }
                lines.put(instructionOffset, disasmInstruction(instruction, dis, instructionOffset, localVariableTableAttribute, cf));
            }

            for (Iterator<Entry<Short, String>> it = lines.entrySet().iterator(); it.hasNext();) {
                Entry<Short, String> e = it.next();
                short instructionOffset = e.getKey();
                String text = e.getValue();

                if (this.branchTargets.contains(instructionOffset)) {
                    this.println();
                    this.println("#" + instructionOffset);
                }

                if (!this.hideLineNumbers && lineNumberTableAttribute != null) {
                    short lineNumber = findLineNumber(lineNumberTableAttribute, instructionOffset);
                    if (lineNumber != -1) {
                        String sourceLine = (String) sourceLines.get(lineNumber);
                        if (sourceLine == null) sourceLine = "(Source line not available)";
                        this.println("              *** Line " + lineNumber + ": " + sourceLine);
                    }
                }

                this.println("        " + text);
            }
        } finally {
            this.branchTargets = null;
        }
    }

    private static short findLineNumber(
        LineNumberTableAttribute lnta,
        short                    offset
    ) {
        for (LineNumberTableEntry lnte : lnta.entries) {
            if (lnte.startPC == offset) return lnte.lineNumber;
        }
        return -1;
    }

    /**
     * @return The {@code instruction} converted into one line of text.
     */
    private String disasmInstruction(
        Instruction                           instruction,
        DataInputStream                       dis,
        short                                 instructionOffset,
        ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
        ClassFile                             cf
    ) throws IOException {
        StringBuilder sb = new StringBuilder(instruction.getMnemonic());
        Operand[] operands = instruction.getOperands();
        if (operands != null) {
            for (int i = 0; i < operands.length; ++i) {
                sb.append(' ').append(operands[i].disasm(dis, instructionOffset, localVariableTableAttribute, cf, this));
            }
        }
        return sb.toString();
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
        "165 if_acmpeq     branchoffset2",
        "166 if_acmpne     branchoffset2",
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
                List<Operand> l = new ArrayList<Operand>();
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    Operand operand;
                    if (s.equals("constantpoolindex1")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                short index = (short) (0xff & dis.readByte());
                                return cf.cpi(index);
                            }
                        };
                    } else
                    if (s.equals("constantpoolindex2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                return cf.cpi(dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex1")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                short index = dis.readByte();
                                String s = Short.toString(index);
                                if (localVariableTableAttribute != null) {
                                    s += " (" + getLocalVariableName(localVariableTableAttribute, index, (short) (instructionOffset + 2), cf) + ")";
                                }
                                return s;
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                short index = dis.readShort();
                                String s = Short.toString(index);
                                if (localVariableTableAttribute != null) {
                                    s += " (" + getLocalVariableName(localVariableTableAttribute, index, (short) (instructionOffset + 3), cf) + ")";
                                }
                                return s;
                            }
                        };
                    } else
                    if (s.startsWith("localvariablearrayindex_")) {
                        final short index = Short.parseShort(s.substring(s.length() - 1));
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                if (localVariableTableAttribute == null) return "";
                                return "(" + getLocalVariableName(localVariableTableAttribute, index, (short) (instructionOffset + 1), cf) + ")";
                            }
                        };
                    } else
                    if (s.equals("branchoffset2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                short branchTarget = (short) (instructionOffset + dis.readShort());
                                d.branchTargets.add(branchTarget);
                                return Integer.toString(0xffff & branchTarget);
                            }
                        };
                    } else
                    if (s.equals("branchoffset4")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                short branchTarget = (short) (instructionOffset + dis.readInt());
                                d.branchTargets.add(branchTarget);
                                return Integer.toString(branchTarget);
                            }
                        };
                    } else
                    if (s.equals("signedbyte")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                return Integer.toString(dis.readByte());
                            }
                        };
                    } else
                    if (s.equals("unsignedbyte")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                return Integer.toString(0xff & dis.readByte());
                            }
                        };
                    } else
                    if (s.equals("atype")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                byte b = dis.readByte();
                                return (
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
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                return Integer.toString(dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("tableswitch")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != 0) {
                                        throw new RuntimeException("'tableswitch' pad byte #" + i + " is not zero, but " + (0xff & padByte));
                                    }
                                }
                                StringBuilder sb = new StringBuilder("default => " + (instructionOffset + dis.readInt()));
                                int low = dis.readInt();
                                int high = dis.readInt();
                                for (int i = low; i <= high; ++i) {
                                    int offset = dis.readInt();
                                    sb.append(", ").append(i).append(" => ").append(instructionOffset + offset);
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if (s.equals("lookupswitch")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != (byte) 0) {
                                        throw new RuntimeException("'tableswitch' pad byte #" + i + " is not zero, but " + (0xff & padByte));
                                    }
                                }
                                StringBuilder sb = new StringBuilder("default => " + (instructionOffset + dis.readInt()));
                                int npairs = dis.readInt();
                                for (int i = 0; i < npairs; ++i) {
                                    int match  = dis.readInt();
                                    int offset = dis.readInt();
                                    sb.append(", ").append(match).append(" => ").append(instructionOffset + offset);
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if (s.equals("wide")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream                       dis,
                                short                                 instructionOffset,
                                ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
                                ClassFile                             cf,
                                Disassembler                          d
                            ) throws IOException {
                                int subopcode = 0xff & dis.readByte();
                                Instruction wideInstruction = opcodeToWideInstruction[subopcode];
                                if (wideInstruction == null) {
                                    throw new RuntimeException(
                                        "Invalid opcode "
                                        + subopcode
                                        + " after opcode WIDE"
                                    );
                                }
                                return d.disasmInstruction(
                                    wideInstruction,
                                    dis,
                                    instructionOffset,
                                    localVariableTableAttribute,
                                    cf
                                );
                            }
                        };
                    } else
                    {
                        throw new RuntimeException("Unknown operand \"" + s + "\"");
                    }
                    l.add(operand);
                }
                operands = l.toArray(new Operand[l.size()]);
            }
            opcodeToInstruction[opcode] = new Instruction(mnemonic, operands);
        }
    }

    private static String getLocalVariableName(
        ClassFile.LocalVariableTableAttribute lvta,
        short                                 localVariableIndex,
        short                                 instructionOffset,
        ClassFile                             cf
    ) {
        for (LocalVariableTableEntry lvte : lvta.entries) {
            if (
                instructionOffset >= lvte.startPC &&
                instructionOffset <= lvte.startPC + lvte.length &&
                localVariableIndex == lvte.index
            ) {
                String name = cf.cpi(lvte.nameIndex);
                String descriptor = cf.cpi(lvte.descriptorIndex);
                return Descriptor.decodeFieldDescriptor(descriptor) + " " + name;
            }
        }
        return "anonymous";

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
        public String    getMnemonic() { return this.mnemonic; }
        public Operand[] getOperands() { return this.operands; }

        private final String    mnemonic;
        private final Operand[] operands;
    }
    private interface Operand {
        String disasm(
            DataInputStream                       dis,
            short                                 instructionOffset,
            ClassFile.LocalVariableTableAttribute localVariableTableAttribute,
            ClassFile                             cf,
            Disassembler                          d
        ) throws IOException;
    }
//
//    /**
//     * A variant of {@link PrintWriter} that allows for convenient indentation
//     * of output lines.
//     */
//    private static class IndentPrintWriter extends PrintWriter {
//        public IndentPrintWriter(OutputStream os) { super(os); }
//        public void write(char[] cbuf, int off, int len) {
//            this.handleIndentation();
//            super.write(cbuf, off, len);
//        }
//        public void write(String str, int off, int len) {
//            this.handleIndentation();
//            super.write(str, off, len);
//        }
//        public void println() { super.println(); this.atBOL = true; }
//        public void indent() { ++this.indentation; }
//        public void unindent() { --this.indentation; }
//
//        private void handleIndentation() {
//            if (this.atBOL) {
//                for (int i = 0; i < this.indentation; ++i) {
//                    super.write(INDENTATION_CHARS, 0, INDENTATION_CHARS.length);
//                }
//                this.atBOL = false;
//            }
//        }
//        private static final char[] INDENTATION_CHARS = new char[] { ' ', ' ' };
//
//        private boolean atBOL = true;
//        private int      indentation = 0;
//    }

    private static class CountingInputStream extends InputStream {
        public CountingInputStream(InputStream is) { this.is = is; }
        public int read() throws IOException {
            int res = this.is.read();
            if (res != -1) ++this.count;
            return res;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res != -1) this.count += res;
            return res;
        }
        public long getCount() { return this.count; }

        private InputStream is;
        private long count = 0L;
    }

    private static String decodeAccess(short n) {
        StringBuilder sb = new StringBuilder();
        if ((n & 0x0007) == 0) { sb.append("package ");                          }
        if ((n & 0x0007) == 1) { sb.append("public ");             n &= ~0x0007; }
        if ((n & 0x0007) == 2) { sb.append("private ");            n &= ~0x0007; }
        if ((n & 0x0007) == 4) { sb.append("protected ");          n &= ~0x0007; }
        if ((n & 0x0008) != 0) { sb.append("static ");             n &= ~0x0008; }
        if ((n & 0x0010) != 0) { sb.append("final ");              n &= ~0x0010; }
        if ((n & 0x0020) != 0) { sb.append("super/synchronized "); n &= ~0x0020; }
        if ((n & 0x0040) != 0) { sb.append("volatile ");           n &= ~0x0040; }
        if ((n & 0x0080) != 0) { sb.append("transient ");          n &= ~0x0080; }
        if ((n & 0x0100) != 0) { sb.append("native ");             n &= ~0x0100; }
        if ((n & 0x0400) != 0) { sb.append("abstract ");           n &= ~0x0400; }
        if ((n & 0x0800) != 0) { sb.append("strict ");             n &= ~0x0800; }
        if ((n & 0x0200) != 0) { sb.append("interface ");          n &= ~0x0200; }
        if (n != 0) sb.append("+ " + n + " ");
        return sb.toString();
    }
}
