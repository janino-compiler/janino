
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

import de.unkrig.jdisasm.ClassFile.Method;
import de.unkrig.jdisasm.ConstantPool.ConstantClassInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantFieldrefInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantInterfaceMethodrefInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantMethodrefInfo;
import de.unkrig.jdisasm.SignatureParser.FormalTypeParameter;
import de.unkrig.jdisasm.SignatureParser.MethodTypeSignature;
import de.unkrig.jdisasm.SignatureParser.ThrowsSignature;
import de.unkrig.jdisasm.SignatureParser.TypeSignature;

/**
 * A Java bytecode disassembler, comparable to JAVAP, which is part of SUN's JDK.
 * <p>
 * Notice that this tool does not depend on any other classes or libraries (other than the
 * standard JDK library).
 * <p>
 * The disassembly is optimized to produce minimal DIFFs for changed class files: E.g. code offsets and local
 * variable indexes are only printed if really necessary.
 */
public class Disassembler {

    // Configuration variables.

    private PrintWriter pw = new PrintWriter(System.out);
    private boolean     verbose = false;
    private File        sourceDirectory = null;
    private boolean     hideLineNumbers;

    // "" for the default package; with a trailing period otherwise.
    private String         thisClassPackageName;
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
                System.out.println("Prints a disassembly listing of the given JAVA[TM] class files to STDOUT.");
                System.out.println("Usage:");
                System.out.println("  java " + Disassembler.class.getName() + " [ <option> ] ... <class-file> ...");
                System.out.println("Valid options are:");
                System.out.println("  -o <output-file>    Store disassembly output in a file.");
                System.out.println("  -verbose");
                System.out.println("  -src <source-dir>   Interweave the output with the class file's source code.");
                System.out.println("  -hide-line-numbers  Don't print the line numbers.");
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
            this.pw.println("// Disassembly of '" + file + "'.");
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

        this.thisClassPackageName = cf.thisClassName.substring(0, cf.thisClassName.lastIndexOf('.') + 1);

        // Package declaration.
        if (thisClassPackageName.length() > 0) {
            println("package " + thisClassPackageName.substring(0, thisClassPackageName.length() - 1) + ";");
        }

        if (cf.enclosingMethodAttribute != null) {
            String methodName = cf.enclosingMethodAttribute.method.name.bytes;
            String className = cf.enclosingMethodAttribute.clasS.name;
            println(
                "// This class is enclosed by method '"
                + beautify(className + (methodName.startsWith("(") ? "" : ".") + methodName)
                + "'."
            );
        }

        // Type declaration.
        {

            if ((cf.accessFlags & 0x1000 /*SYNTHETIC*/) != 0 || cf.syntheticAttribute != null) this.println("// This is a synthetic class.");

            // Annotations.
            if (cf.runtimeInvisibleAnnotationsAttribute != null) {
                for (ClassFile.Annotation a : cf.runtimeInvisibleAnnotationsAttribute.annotations) {
                    println(a.toString());
                }
            }
            if (cf.runtimeVisibleAnnotationsAttribute != null) {
                for (ClassFile.Annotation a : cf.runtimeVisibleAnnotationsAttribute.annotations) {
                    println(a.toString());
                }
            }

            // Modifiers, name.
            this.print(
                decodeAccess((short) (
                    cf.accessFlags
                    & ~Modifier.SYNCHRONIZED // Has no meaning but is always set for backwards compatibility
                    & ~0x1000/*SYNTHETIC*/ // SYNTHETIC has already been printed as a comment.
                    & ((cf.accessFlags & Modifier.INTERFACE) != 0 ? ~Modifier.ABSTRACT : 0xffff) // Suppress redundant "abstract" modifier for interfaces.
                    & ((cf.accessFlags & 0x4000 /*ENUM*/) != 0 ? ~Modifier.FINAL : 0xffff) // Suppress redundant "final" modifier for enums.
                ))
                + ((cf.accessFlags & 0x6200) == 0 ? "class " : "")
            );

            // EXTENDS and IMPLEMENTS clauses.
            if (cf.signatureAttribute != null) {
                this.print(beautify(SignatureParser.decodeClassSignature(cf.signatureAttribute.signature).toString()));
            } else {
                this.print(cf.thisClassName);
                if (!"java.lang.Object".equals(cf.superClassName)) {
                    this.print(" extends " + beautify(cf.superClassName));
                }
                List<String> ifs = cf.interfaceNames;
                if ((cf.accessFlags & 0x2000 /*ANNOTATION*/) != 0 && ifs.contains("java.lang.annotation.Annotation")) {
                    ifs = new ArrayList<String>(ifs);
                    ifs.remove("java.lang.annotation.Annotation");
                }
                if (!ifs.isEmpty()) {
                    Iterator<String> it = ifs.iterator();
                    this.print(" implements " + it.next());
                    while (it.hasNext()) {
                        this.print(", " + it.next());
                    }
                }
            }

            this.println(" {");
        }

        // Fields.
        {
            List<String[]> lines = new ArrayList<String[]>();
            for (ClassFile.Field field : cf.fields) {
                
                // Annotations.
                if (field.runtimeInvisibleAnnotationsAttribute != null) {
                    for (ClassFile.Annotation a : field.runtimeInvisibleAnnotationsAttribute.annotations) {
                        println("    " + a.toString());
                    }
                }
                if (field.runtimeVisibleAnnotationsAttribute != null) {
                    for (ClassFile.Annotation a : field.runtimeVisibleAnnotationsAttribute.annotations) {
                        println("    " + a.toString());
                    }
                }

                // Pretty-print the field type.
                String parametrizedType = beautify(
                    field.signatureAttribute == null
                    ? SignatureParser.decodeFieldDescriptor(field.descriptor).toString()
                    : SignatureParser.decodeFieldTypeSignature(field.signatureAttribute.signature).toString()
                );

                // Print the field declaration.
                if ((field.accessFlags & 0x1000 /*SYNTHETIC*/) != 0 || field.syntheticAttribute != null) {
                    lines.add(new String[] { "    // Synthetic field:" });
                }
                String prefix = "    " + decodeAccess((short) (field.accessFlags & ~0x1000 /*SYNTHETIC*/)) + parametrizedType + " ";
                if (field.constantValueAttribute == null) {
                    lines.add(new String[] {
                        prefix,
                        field.name + ";"
                    });
                } else {
                    lines.add(new String[] {
                        prefix,
                        field.name,
                        " = " + field.constantValueAttribute.constantValue + ";"
                    });
                }
            }
            println(lines);
        }

        // Read source file.
        Map<Short, String> sourceLines = new HashMap<Short, String>();
        READ_SOURCE_LINES:
        if (cf.sourceFileAttribute != null) {
            String sourceFile = cf.sourceFileAttribute.sourceFile;
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
            if ((m.accessFlags & 0x1000 /*SYNTHETIC*/) != 0 || m.syntheticAttribute != null) {
                println("    // Synthetic method:");
            }
            
            // Annotations.
            if (m.runtimeInvisibleAnnotationsAttribute != null) {
                for (ClassFile.Annotation a : m.runtimeInvisibleAnnotationsAttribute.annotations) {
                    println("    " + a.toString());
                }
            }
            if (m.runtimeVisibleAnnotationsAttribute != null) {
                for (ClassFile.Annotation a : m.runtimeVisibleAnnotationsAttribute.annotations) {
                    println("    " + a.toString());
                }
            }

            // Parameter annotations.
            if (m.runtimeInvisibleParameterAnnotationsAttribute != null) {
                for (ClassFile.ParameterAnnotation pa : m.runtimeInvisibleParameterAnnotationsAttribute.parameterAnnotations) {
                    for (ClassFile.Annotation a : pa.annotations) {
                        println("    " + a.toString());
                    }
                }
            }
            if (m.runtimeVisibleParameterAnnotationsAttribute != null) {
                for (ClassFile.ParameterAnnotation pa : m.runtimeVisibleParameterAnnotationsAttribute.parameterAnnotations) {
                    for (ClassFile.Annotation a : pa.annotations) {
                        println("    " + a.toString());
                    }
                }
            }

            // Access flags.
            String functionName = m.name;
            Disassembler.this.print(
                "    "
                + decodeAccess((short) (
                    m.accessFlags
                    & ~0x1000 /*SYNTHETIC*/
                    & ~0x0080 /*TRANSIENT/VARARGS*/
                    & ((cf.accessFlags & Modifier.INTERFACE) != 0 ? ~(Modifier.PUBLIC | Modifier.ABSTRACT) : 0xffff)
                ))
            );

            // Formal type parameters, name, parameters.
            MethodTypeSignature mts = (
                m.signatureAttribute == null
                ? SignatureParser.decodeMethodDescriptor(m.descriptor)
                : SignatureParser.decodeMethodTypeSignature(m.signatureAttribute.signature)
            );

            // Formal type parameters.
            if (!mts.formalTypeParameters.isEmpty()) {
                Iterator<FormalTypeParameter> it = mts.formalTypeParameters.iterator();
                print("<" + beautify(it.next().toString()));
                while (it.hasNext()) print(", " + beautify(it.next().toString()));
                print(">");
            }

            // Name.
            if (
                "<clinit>".equals(functionName)
                && (m.accessFlags & Modifier.STATIC) != 0
                && (m.exceptionsAttribute == null || m.exceptionsAttribute.exceptionNames.isEmpty())
                && mts.formalTypeParameters.isEmpty()
                && mts.parameterTypes.isEmpty()
                && mts.returnType == SignatureParser.VOID
                && mts.thrownTypes.isEmpty()
            ) {
                ;
            } else
            if (
                "<init>".equals(functionName)
                && (m.accessFlags & (Modifier.ABSTRACT | Modifier.FINAL | Modifier.INTERFACE | Modifier.STATIC)) == 0
                && mts.returnType == SignatureParser.VOID
            ) {
                print(cf.thisClassName);
                print(
                    mts.parameterTypes,
                    m,
                    (short) 1,
                    (m.accessFlags & 0x0080 /*VARARGS*/) != 0
                );
            } else
            {
                print(mts.returnType.toString() + ' ');
                print(functionName);
                print(
                    mts.parameterTypes,
                    m,
                    (m.accessFlags & Modifier.STATIC) == 0 ? (short) 1 : (short) 0,
                    (m.accessFlags & 0x0080 /*VARARGS*/) != 0
                );
            }
            if (mts.thrownTypes != null && !mts.thrownTypes.isEmpty()) {
                Iterator<ThrowsSignature> it = mts.thrownTypes.iterator();
                print(" throws " + beautify(it.next().toString()));
                while (it.hasNext()) print(", " + beautify(it.next().toString()));
            } else
            if (m.exceptionsAttribute != null && !m.exceptionsAttribute.exceptionNames.isEmpty()) {
                Iterator<ConstantClassInfo> it = m.exceptionsAttribute.exceptionNames.iterator();
                print(" throws " + beautify(it.next().name));
                while (it.hasNext()) print(", " + beautify(it.next().name));
            }
            if (m.annotationDefaultAttribute != null) {
                print("default " + m.annotationDefaultAttribute.defaultValue);
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
                        ca.lineNumberTableAttribute,
                        sourceLines,
                        cf.constantPool,
                        m
                    );
                } catch (IOException ignored) {
                    ;
                }
                println("    }");
            }
            if (verbose) {
                for (ClassFile.Attribute a : m.attributes) {
                    println("        // " + a);
                }
            }
        }

        if (verbose) {
            for (ClassFile.Attribute a : cf.attributes) {
                println("    // " + a.toString());
            }
        }
        println("}");
    }

    private void print(
        List<TypeSignature> parameterTypes,
        ClassFile.Method    method,
        short               firstIndex,
        boolean             varargs
    ) {
        print("(");
        Iterator<TypeSignature> it = parameterTypes.iterator();
        if (it.hasNext()) {
            for (;;) {
                TypeSignature pts = it.next();

                // Parameter type.
                if (varargs && !it.hasNext() && pts instanceof SignatureParser.ArrayTypeSignature) {
                    print(((SignatureParser.ArrayTypeSignature) pts).typeSignature.toString() + "...");
                } else {
                    print(pts.toString());
                }

                // Parameter name.
                print(' ' + getLocalVariable(firstIndex, (short) 0, method).name);

                if (!it.hasNext()) break;
                firstIndex++;
                print(", ");
            }
        }
        print(")");
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
     * Read byte code from the given {@link InputStream} and disassemble it.
     */
    private void disasmBytecode(
        InputStream                         is,
        List<ClassFile.ExceptionTableEntry> exceptionTable,
        ClassFile.LineNumberTableAttribute  lineNumberTableAttribute,
        Map<Short, String>                  sourceLines,
        ConstantPool                        cp,
        ClassFile.Method                    method
    ) throws IOException {
        CountingInputStream cis = new CountingInputStream(is);
        DataInputStream     dis = new DataInputStream(cis);

        this.branchTargets = new HashSet<Short>();
        try {

            // Disassemble the byte code into a sequence of lines.
            SortedMap<Short, String> lines = new TreeMap<Short, String>();
            for (;;) {
                short instructionOffset = (short) cis.getCount();

                int opcode = dis.read();
                if (opcode == -1) break;

                Instruction instruction = opcodeToInstruction[opcode];
                if (instruction == null) {
                    lines.put(instructionOffset, "??? (invalid opcode \"" + opcode + "\")");
                } else {
                    lines.put(instructionOffset, instruction.getMnemonic() + disasmOperands(
                        instruction.getOperands(),
                        dis,
                        instructionOffset,
                        method,
                        cp
                    ));
                }
            }

            // Format and print the disassembly lines.
            for (Iterator<Entry<Short, String>> it = lines.entrySet().iterator(); it.hasNext();) {
                Entry<Short, String> e = it.next();
                short instructionOffset = e.getKey();
                String text = e.getValue();

                // Print instruction offsets only for branch targets.
                if (this.branchTargets.contains(instructionOffset)) {
                    this.println();
                    this.println("#" + instructionOffset);
                }

                if (lineNumberTableAttribute != null) {
                    short lineNumber = findLineNumber(lineNumberTableAttribute, instructionOffset);
                    if (lineNumber != -1) {
                        String sourceLine = sourceLines.get(lineNumber);
                        if (sourceLine == null) {
                            if (!this.hideLineNumbers) this.println("              *** Line " + lineNumber);
                        } else {
                            if (this.hideLineNumbers) {
                                this.println("              *** " + sourceLine);
                            } else {
                                this.println("              *** Line " + lineNumber + ": " + sourceLine);
                            }
                        }
                    }
                }

                this.println("        " + text);
            }
        } finally {
            this.branchTargets = null;
        }
    }

    /**
     * @return -1 iff the offset is not associated with a line number
     */
    private static short findLineNumber(
        ClassFile.LineNumberTableAttribute lnta,
        short                              offset
    ) {
        for (ClassFile.LineNumberTableEntry lnte : lnta.entries) {
            if (lnte.startPC == offset) return lnte.lineNumber;
        }
        return -1;
    }

    /**
     * @return The {@code instruction} converted into one line of text.
     */
    private String disasmOperands(
        Operand[]       operands,
        DataInputStream dis,
        short           instructionOffset,
        Method          method,
        ConstantPool    cp
    ) throws IOException {
        if (operands == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < operands.length; ++i) {
            sb.append(operands[i].disasm(
                dis,
                instructionOffset,
                method,
                cp,
                this
            ));
        }
        return sb.toString();
    }

    private static final String[] instructions = new String[] {
        "50  aaload",
        "83  aastore",
        "1   aconst_null",
        "25  aload           localvariablearrayindex1",
        "42  aload_0         implicitlocalvariableindex",
        "43  aload_1         implicitlocalvariableindex",
        "44  aload_2         implicitlocalvariableindex",
        "45  aload_3         implicitlocalvariableindex",
        "189 anewarray       class2",
        "176 areturn",
        "190 arraylength",
        "58  astore          localvariablearrayindex1",
        "75  astore_0        implicitlocalvariableindex",
        "76  astore_1        implicitlocalvariableindex",
        "77  astore_2        implicitlocalvariableindex",
        "78  astore_3        implicitlocalvariableindex",
        "191 athrow",
        "51  baload",
        "84  bastore",
        "16  bipush          signedbyte",
        "52  caload",
        "85  castore",
        "192 checkcast       class2",
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
        "24  dload           localvariablearrayindex1",
        "38  dload_0         implicitlocalvariableindex",
        "39  dload_1         implicitlocalvariableindex",
        "40  dload_2         implicitlocalvariableindex",
        "41  dload_3         implicitlocalvariableindex",
        "107 dmul",
        "119 dneg",
        "115 drem",
        "175 dreturn",
        "57  dstore          localvariablearrayindex1",
        "71  dstore_0        implicitlocalvariableindex",
        "72  dstore_1        implicitlocalvariableindex",
        "73  dstore_2        implicitlocalvariableindex",
        "74  dstore_3        implicitlocalvariableindex",
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
        "23  fload           localvariablearrayindex1",
        "34  fload_0         implicitlocalvariableindex",
        "35  fload_1         implicitlocalvariableindex",
        "36  fload_2         implicitlocalvariableindex",
        "37  fload_3         implicitlocalvariableindex",
        "106 fmul",
        "118 fneg",
        "114 frem",
        "174 freturn",
        "56  fstore          localvariablearrayindex1",
        "67  fstore_0        implicitlocalvariableindex",
        "68  fstore_1        implicitlocalvariableindex",
        "69  fstore_2        implicitlocalvariableindex",
        "70  fstore_3        implicitlocalvariableindex",
        "102 fsub",
        "180 getfield        fieldref2",
        "178 getstatic       fieldref2",
        "167 goto            branchoffset2",
        "200 goto_w          branchoffset4",
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
        "165 if_acmpeq       branchoffset2",
        "166 if_acmpne       branchoffset2",
        "159 if_icmpeq       branchoffset2",
        "160 if_icmpne       branchoffset2",
        "161 if_icmplt       branchoffset2",
        "162 if_icmpge       branchoffset2",
        "163 if_icmpgt       branchoffset2",
        "164 if_icmple       branchoffset2",
        "153 ifeq            branchoffset2",
        "154 ifne            branchoffset2",
        "155 iflt            branchoffset2",
        "156 ifge            branchoffset2",
        "157 ifgt            branchoffset2",
        "158 ifle            branchoffset2",
        "199 ifnonnull       branchoffset2",
        "198 ifnull          branchoffset2",
        "132 iinc            localvariablearrayindex1 signedbyte",
        "21  iload           localvariablearrayindex1",
        "26  iload_0         implicitlocalvariableindex",
        "27  iload_1         implicitlocalvariableindex",
        "28  iload_2         implicitlocalvariableindex",
        "29  iload_3         implicitlocalvariableindex",
        "104 imul",
        "116 ineg",
        "193 instanceof      class2",
        "185 invokeinterface interfacemethodref2 signedbyte signedbyte",
        "183 invokespecial   methodref2",
        "184 invokestatic    methodref2",
        "182 invokevirtual   methodref2",
        "128 ior",
        "112 irem",
        "172 ireturn",
        "120 ishl",
        "122 ishr",
        "54  istore          localvariablearrayindex1",
        "59  istore_0        implicitlocalvariableindex",
        "60  istore_1        implicitlocalvariableindex",
        "61  istore_2        implicitlocalvariableindex",
        "62  istore_3        implicitlocalvariableindex",
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
        "18  ldc             intfloatclassstring1",
        "19  ldc_w           intfloatclassstring2",
        "20  ldc2_w          longdouble2",
        "109 ldiv",
        "22  lload           localvariablearrayindex1",
        "30  lload_0         implicitlocalvariableindex",
        "31  lload_1         implicitlocalvariableindex",
        "32  lload_2         implicitlocalvariableindex",
        "33  lload_3         implicitlocalvariableindex",
        "105 lmul",
        "117 lneg",
        "171 lookupswitch    lookupswitch",
        "129 lor",
        "113 lrem",
        "173 lreturn",
        "121 lshl",
        "123 lshr",
        "55  lstore          localvariablearrayindex1",
        "63  lstore_0        implicitlocalvariableindex",
        "64  lstore_1        implicitlocalvariableindex",
        "65  lstore_2        implicitlocalvariableindex",
        "66  lstore_3        implicitlocalvariableindex",
        "101 lsub",
        "125 lushr",
        "131 lxor",
        "194 monitorenter",
        "195 monitorexit",
        "197 multianewarray  class2 unsignedbyte",
        "187 new             class2",
        "188 newarray        atype",
        "0   nop",
        "87  pop",
        "88  pop2",
        "181 putfield        fieldref2",
        "179 putstatic       fieldref2",
        "169 ret             localvariablearrayindex1",
        "177 return",
        "53  saload",
        "86  sastore",
        "17  sipush          signedshort",
        "95  swap",
        "170 tableswitch     tableswitch",
        "196 wide            wide",
    };
    private static final String[] wideInstructions = new String[] {
        "21  iload           localvariablearrayindex2",
        "23  fload           localvariablearrayindex2",
        "25  aload           localvariablearrayindex2",
        "22  lload           localvariablearrayindex2",
        "24  dload           localvariablearrayindex2",
        "54  istore          localvariablearrayindex2",
        "56  fstore          localvariablearrayindex2",
        "58  astore          localvariablearrayindex2",
        "55  lstore          localvariablearrayindex2",
        "57  dstore          localvariablearrayindex2",
        "169 ret             localvariablearrayindex2",
        "132 iinc            localvariablearrayindex2 signedshort",
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
                    if (s.equals("intfloatclassstring1")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                String t = cp.getIntegerFloatClassString((short) (0xff & dis.readByte()));
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("intfloatclassstring2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                String t = cp.getIntegerFloatClassString(dis.readShort());
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("longdouble2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return ' ' + cp.getLongDoubleString(dis.readShort());
                            }
                        };
                    } else
                    if (s.equals("fieldref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                ConstantFieldrefInfo fr = cp.getConstantFieldrefInfo(dis.readShort());
                                return (
                                    ' '
                                    + SignatureParser.decodeFieldDescriptor(fr.nameAndType.descriptor.bytes).toString()
                                    + ' '
                                    + d.beautify(fr.clasS.name)
                                    + '.'
                                    + fr.nameAndType.name.bytes
                                );
                            }
                        };
                    } else
                    if (s.equals("methodref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                ConstantMethodrefInfo mr = cp.getConstantMethodrefInfo(dis.readShort());
                                return ' ' + d.beautify(SignatureParser.decodeMethodDescriptor(mr.nameAndType.descriptor.bytes).toString(
                                    mr.clasS.name,
                                    mr.nameAndType.name.bytes
                                ));
                            }
                        };
                    } else
                    if (s.equals("interfacemethodref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                ConstantInterfaceMethodrefInfo imr = cp.getConstantInterfaceMethodrefInfo(dis.readShort());
                                return ' ' + d.beautify(SignatureParser.decodeMethodDescriptor(imr.nameAndType.descriptor.bytes).toString(
                                    imr.clasS.name,
                                    imr.nameAndType.name.bytes
                                ));
                            }
                        };
                    } else
                    if (s.equals("class2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                String name = cp.getConstantClassInfo(dis.readShort()).name;
                                return ' ' + d.beautify(
                                    name.startsWith("[")
                                    ? SignatureParser.decodeFieldDescriptor(name).toString()
                                    : name.replace('/', '.')
                                );
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex1")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                byte index = dis.readByte();
                                LocalVariable lv = d.getLocalVariable((short) (0xff & index), (short) (instructionOffset + 2), method);
                                return " [" + lv.typeSignature.toString() + ' ' + lv.name + ']';
                            }
                        };
                    } else
                    if (s.equals("localvariablearrayindex2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readByte();
                                LocalVariable lv = d.getLocalVariable(index, (short) (instructionOffset + 2), method);
                                return " [" + lv.typeSignature.toString() + ' ' + lv.name + ']';
                            }
                        };
                    } else
                    if (s.equals("implicitlocalvariableindex")) {
                        // Strip the lv index from the mnemonic
                        final short index = Short.parseShort(mnemonic.substring(mnemonic.length() - 1));
                        mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
                        operand = new Operand() {
                            private LocalVariable lv;

                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                lv = d.getLocalVariable(index, (short) (instructionOffset + 2), method);
                                return " [" + d.beautify(lv.typeSignature.toString()) + ' ' + lv.name + ']';
                            }
                        };
                    } else
                    if (s.equals("branchoffset2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short branchTarget = (short) (instructionOffset + dis.readShort());
                                d.branchTargets.add(branchTarget);
                                return " " + (0xffff & branchTarget);
                            }
                        };
                    } else
                    if (s.equals("branchoffset4")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short branchTarget = (short) (instructionOffset + dis.readInt());
                                d.branchTargets.add(branchTarget);
                                return " " + (0xffff & branchTarget);
                            }
                        };
                    } else
                    if (s.equals("signedbyte")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return " " + dis.readByte();
                            }
                        };
                    } else
                    if (s.equals("unsignedbyte")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return " " + (0xff & dis.readByte());
                            }
                        };
                    } else
                    if (s.equals("atype")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                byte b = dis.readByte();
                                return (
                                    b ==  4 ? " BOOLEAN" :
                                    b ==  5 ? " CHAR" :
                                    b ==  6 ? " FLOAT" :
                                    b ==  7 ? " DOUBLE" :
                                    b ==  8 ? " BYTE" :
                                    b ==  9 ? " SHORT" :
                                    b == 10 ? " INT" :
                                    b == 11 ? " LONG" :
                                    " " + (0xff & b)
                                );
                            }
                        };
                    } else
                    if (s.equals("signedshort")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return " " + dis.readShort();
                            }
                        };
                    } else
                    if (s.equals("tableswitch")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != 0) {
                                        throw new RuntimeException("'tableswitch' pad byte #" + i + " is not zero, but " + (0xff & padByte));
                                    }
                                }
                                StringBuilder sb = new StringBuilder(" default => " + (instructionOffset + dis.readInt()));
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
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != (byte) 0) {
                                        throw new RuntimeException("'tableswitch' pad byte #" + i + " is not zero, but " + (0xff & padByte));
                                    }
                                }
                                StringBuilder sb = new StringBuilder(" default => " + (instructionOffset + dis.readInt()));
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
                                DataInputStream dis,
                                short           instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int subopcode = 0xff & dis.readByte();
                                Instruction wideInstruction = opcodeToWideInstruction[subopcode];
                                if (wideInstruction == null) {
                                    return (
                                        "Invalid opcode "
                                        + subopcode
                                        + " after opcode WIDE"
                                    );
                                }
                                return wideInstruction.getMnemonic() + d.disasmOperands(
                                    wideInstruction.getOperands(),
                                    dis,
                                    instructionOffset,
                                    method,
                                    cp
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

    private LocalVariable getLocalVariable(
        short            localVariableIndex,
        short            instructionOffset,
        ClassFile.Method method
    ) {
        LocalVariable lv = new LocalVariable();
        try {
            if (method.codeAttribute != null) {
                if (method.codeAttribute.localVariableTypeTableAttribute != null) {
                    for (ClassFile.LocalVariableTypeTableEntry lvtte : method.codeAttribute.localVariableTypeTableAttribute.entries) {
                        if (
                            instructionOffset >= lvtte.startPC &&
                            instructionOffset <= lvtte.startPC + lvtte.length &&
                            localVariableIndex == lvtte.index
                        ) {
                            lv.typeSignature = SignatureParser.decodeFieldTypeSignature(lvtte.signature);
                            lv.name          =  lvtte.name;
                            return lv;
                        }
                    }
                }
                if (method.codeAttribute.localVariableTableAttribute != null) {
                    for (ClassFile.LocalVariableTableEntry lvte : method.codeAttribute.localVariableTableAttribute.entries) {
                        if (
                            instructionOffset >= lvte.startPC &&
                            instructionOffset <= lvte.startPC + lvte.length &&
                            localVariableIndex == lvte.index
                        ) {
                            lv.typeSignature = SignatureParser.decodeFieldDescriptor(lvte.descriptor);
                            lv.name          = lvte.name;
                            return lv;
                        }
                    }
                }
            }
            MethodTypeSignature mts = (
                method.signatureAttribute != null
                ? SignatureParser.decodeMethodTypeSignature(method.signatureAttribute.signature)
                : SignatureParser.decodeMethodDescriptor(method.descriptor)
            );
            int firstParameter = (method.accessFlags & Modifier.STATIC) == 0 ? 1 : 0;
            if (localVariableIndex < firstParameter) {
                lv.name = "this";
                lv.typeSignature = SignatureParser.UNKNOWN;
            } else
                if (localVariableIndex < firstParameter + mts.parameterTypes.size()) {
                lv.name = "p" + (localVariableIndex - firstParameter);
                lv.typeSignature = mts.parameterTypes.get(localVariableIndex - firstParameter);
            } else
            {
                lv.name = "v" + (localVariableIndex - firstParameter - mts.parameterTypes.size());
                lv.typeSignature = SignatureParser.UNKNOWN;
            }
        } catch (IOException e) {
            lv.name = "l" + localVariableIndex;
            lv.typeSignature = SignatureParser.UNKNOWN;
        }
        return lv;
    }
    private class LocalVariable {
        TypeSignature typeSignature;
        String        name;
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
            DataInputStream dis,
            short           instructionOffset,
            Method          method,
            ConstantPool    cp,
            Disassembler    d
        ) throws IOException;
    }

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

    /**
     * Returns a series of words, in canonical order, separated with one space, and with one trailing space.
     */
    private static String decodeAccess(short n) {
        StringBuilder sb = new StringBuilder();
        if ((n & 0x0007) == 1) { sb.append("public ");       n &= ~0x0007; }
        if ((n & 0x0007) == 2) { sb.append("private ");      n &= ~0x0007; }
        if ((n & 0x0007) == 4) { sb.append("protected ");    n &= ~0x0007; }

        if ((n & 0x0400) != 0) { sb.append("abstract ");     n &= ~0x0400; }
        if ((n & 0x0008) != 0) { sb.append("static ");       n &= ~0x0008; }
        if ((n & 0x0010) != 0) { sb.append("final ");        n &= ~0x0010; }
        if ((n & 0x0080) != 0) { sb.append("transient ");    n &= ~0x0080; }
        if ((n & 0x0040) != 0) { sb.append("volatile ");     n &= ~0x0040; }
        if ((n & 0x0020) != 0) { sb.append("synchronized "); n &= ~0x0020; }
        if ((n & 0x0100) != 0) { sb.append("native ");       n &= ~0x0100; }
        if ((n & 0x0800) != 0) { sb.append("strictfp ");     n &= ~0x0800; }
        if ((n & 0x1000) != 0) { sb.append("synthetic ");    n &= ~0x1000; }

        if ((n & 0x2000) != 0) { sb.append("@");             n &= ~0x2000; }
        if ((n & 0x0200) != 0) { sb.append("interface ");    n &= ~0x0200; }
        if ((n & 0x4000) != 0) { sb.append("enum ");         n &= ~0x4000; }

        if (n != 0) sb.append("+ " + n + " ");
        return sb.toString();
    }

    private String beautify(String s) {
        int i = 0;
        for (;;) {

            // Find the next type name.
            for (;;) {
                if (i == s.length()) return s;
                if (Character.isJavaIdentifierStart(s.charAt(i))) break;
                i++;
            }

            // Strip redundant prefixes from the type name.
            for (String pkg : new String[] { "java.lang.", this.thisClassPackageName }) {
                if (s.substring(i).startsWith(pkg)) {
                    s = s.substring(0, i) + s.substring(i + pkg.length());
                    break;
                }
            }

            // Skip the rest of the type name.
            for (;;) {
                if (i == s.length()) return s;
                char c = s.charAt(i);
                if (c != '.' && !Character.isJavaIdentifierPart(c)) break;
                i++;
            }
        }
    }
}
