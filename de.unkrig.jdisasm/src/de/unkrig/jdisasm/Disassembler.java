
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

import static de.unkrig.jdisasm.ClassFile.ACC_ABSTRACT;
import static de.unkrig.jdisasm.ClassFile.ACC_ANNOTATION;
import static de.unkrig.jdisasm.ClassFile.ACC_BRIDGE;
import static de.unkrig.jdisasm.ClassFile.ACC_ENUM;
import static de.unkrig.jdisasm.ClassFile.ACC_FINAL;
import static de.unkrig.jdisasm.ClassFile.ACC_INTERFACE;
import static de.unkrig.jdisasm.ClassFile.ACC_NATIVE;
import static de.unkrig.jdisasm.ClassFile.ACC_PRIVATE;
import static de.unkrig.jdisasm.ClassFile.ACC_PROTECTED;
import static de.unkrig.jdisasm.ClassFile.ACC_PUBLIC;
import static de.unkrig.jdisasm.ClassFile.ACC_STATIC;
import static de.unkrig.jdisasm.ClassFile.ACC_STRICT;
import static de.unkrig.jdisasm.ClassFile.ACC_SYNCHRONIZED;
import static de.unkrig.jdisasm.ClassFile.ACC_SYNTHETIC;
import static de.unkrig.jdisasm.ClassFile.ACC_TRANSIENT;
import static de.unkrig.jdisasm.ClassFile.ACC_VARARGS;
import static de.unkrig.jdisasm.ClassFile.ACC_VOLATILE;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.unkrig.jdisasm.ClassFile.Annotation;
import de.unkrig.jdisasm.ClassFile.AnnotationDefaultAttribute;
import de.unkrig.jdisasm.ClassFile.Attribute;
import de.unkrig.jdisasm.ClassFile.AttributeVisitor;
import de.unkrig.jdisasm.ClassFile.CodeAttribute;
import de.unkrig.jdisasm.ClassFile.ConstantValueAttribute;
import de.unkrig.jdisasm.ClassFile.DeprecatedAttribute;
import de.unkrig.jdisasm.ClassFile.EnclosingMethodAttribute;
import de.unkrig.jdisasm.ClassFile.ExceptionTableEntry;
import de.unkrig.jdisasm.ClassFile.ExceptionsAttribute;
import de.unkrig.jdisasm.ClassFile.Field;
import de.unkrig.jdisasm.ClassFile.InnerClassesAttribute;
import de.unkrig.jdisasm.ClassFile.LineNumberTableAttribute;
import de.unkrig.jdisasm.ClassFile.LineNumberTableEntry;
import de.unkrig.jdisasm.ClassFile.LocalVariableTableAttribute;
import de.unkrig.jdisasm.ClassFile.LocalVariableTypeTableAttribute;
import de.unkrig.jdisasm.ClassFile.Method;
import de.unkrig.jdisasm.ClassFile.ParameterAnnotation;
import de.unkrig.jdisasm.ClassFile.RuntimeInvisibleAnnotationsAttribute;
import de.unkrig.jdisasm.ClassFile.RuntimeInvisibleParameterAnnotationsAttribute;
import de.unkrig.jdisasm.ClassFile.RuntimeVisibleAnnotationsAttribute;
import de.unkrig.jdisasm.ClassFile.RuntimeVisibleParameterAnnotationsAttribute;
import de.unkrig.jdisasm.ClassFile.SignatureAttribute;
import de.unkrig.jdisasm.ClassFile.SourceFileAttribute;
import de.unkrig.jdisasm.ClassFile.SyntheticAttribute;
import de.unkrig.jdisasm.ClassFile.UnknownAttribute;
import de.unkrig.jdisasm.ConstantPool.ConstantClassInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantFieldrefInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantInterfaceMethodrefInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantMethodrefInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantPoolEntry;
import de.unkrig.jdisasm.SignatureParser.ArrayTypeSignature;
import de.unkrig.jdisasm.SignatureParser.ClassSignature;
import de.unkrig.jdisasm.SignatureParser.FieldTypeSignature;
import de.unkrig.jdisasm.SignatureParser.FormalTypeParameter;
import de.unkrig.jdisasm.SignatureParser.MethodTypeSignature;
import de.unkrig.jdisasm.SignatureParser.SignatureException;
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
    boolean             verbose = false;

    /**
     * {@code null} means "do not attempt to find the source file".
     */
    private File        sourceDirectory = null;
    private boolean     hideLines;
    private boolean     hideVars;

    /**
     * "" for the default package; with a trailing period otherwise.
     */
    private String thisClassPackageName;

    Set<Integer> branchTargets;

    private static final List<ParameterAnnotation> NO_PARAMETER_ANNOTATIONS = (
        Collections.<ParameterAnnotation>emptyList()
    );

    public static void main(String[] args) throws IOException {
        Disassembler d = new Disassembler();
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-' || arg.length() == 1) break;
            if (arg.equals("-o")) {
                d.setOut(new FileOutputStream(args[++i]));
            } else
            if (arg.equals("-verbose")) {
                d.setVerbose(true);
            } else
            if (arg.equals("-src")) {
                d.setSourceDirectory(new File(args[++i]));
            } else
            if (arg.equals("-hide-lines")) {
                d.setHideLines(true);
            } else
            if (arg.equals("-hide-vars")) {
                d.setHideVars(true);
            } else
            if (arg.equals("-help")) {
                System.out.println("Prints a disassembly listing of the given JAVA[TM] class files (or STDIN) to ");
                System.out.println("STDOUT.");
                System.out.println("Usage:");
                System.out.println("  java " + Disassembler.class.getName() + " [ <option> ] ... [ <class-file> ] ...");
                System.out.println("Valid options are:");
                System.out.println("  -o <output-file>   Store disassembly output in a file.");
                System.out.println("  -verbose");
                System.out.println("  -src <source-dir>  Interweave the output with the class file's source code.");
                System.out.println("  -hide-lines        Don't print the line numbers.");
                System.out.println("  -hide-vars         Don't print the local variable names.");
                System.exit(0);
            } else
            {
                System.err.println("Unrecognized command line option \"" + arg + "\"; try \"-help\".");
                System.exit(1);
            }
        }
        if (i == args.length) {
            d.disasm(System.in);
        } else {
            Pattern IS_URL = Pattern.compile("\\w\\w+:.*");
            for (; i < args.length; ++i) {
                String name = args[i];
                if ("-".equals(name)) {
                    d.disasm(System.in);
                } else
                if (IS_URL.matcher(name).matches()) {
                    d.disasm(new URL(name));
                } else
                {
                    d.disasm(new File(name));
                }
            }
        }
    }

    public Disassembler() {}

    public void setOut(Writer writer) {
        this.pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public void setOut(OutputStream stream) {
        this.pw = new PrintWriter(stream);
    }

    public void setOut(OutputStream stream, String charsetName) throws UnsupportedEncodingException {
        this.pw = new PrintWriter(new OutputStreamWriter(stream, charsetName));
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Where to look for source files; {@code null} disables source file loading. Source file loading is disabled by
     * default.
     */
    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void setHideLines(boolean hideLines) {
        this.hideLines = hideLines;
    }
    public void setHideVars(boolean hideVars) {
        this.hideVars = hideVars;
    }

    void print(String s)                       { this.pw.print(s); }
    void println()                             { this.pw.println(); }
    void println(String s)                     { this.pw.println(s); }
    void printf(String format, Object... args) { this.pw.printf(format, args); }

    /**
     * Disassemble one Java&trade; class file to {@link System#out}.
     */
    public void disasm(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.pw.println();
            this.pw.println("// *** Disassembly of '" + file + "'.");
            disasm(is);
        } catch (IOException ioe) {
            IOException ioe2 = new IOException("Disassembling '" + file + "': " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2;
        } catch (RuntimeException re) {
            throw new RuntimeException("Disassembling '" + file + "': " + re.getMessage(), re);
        } finally {
            try { is.close(); } catch (IOException ex) { }
        }
    }

    public void disasm(URL location) throws IOException {
        InputStream is = location.openConnection().getInputStream();
        try {
            this.pw.println();
            this.pw.println("// *** Disassembly of '" + location + "'.");
            disasm(is);
        } catch (IOException ioe) {
            IOException ioe2 = new IOException("Disassembling '" + location + "': " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2;
        } catch (RuntimeException re) {
            throw new RuntimeException("Disassembling '" + location + "': " + re.getMessage(), re);
        } finally {
            try { is.close(); } catch (IOException ex) { }
        }
    }

    /**
     * @param is A Java&trade; class file
     */
    public void disasm(InputStream is) throws IOException {
        try {
            this.disasmClassFile(new DataInputStream(is));
        } finally {
            this.pw.flush();
        }
    }

    /**
     * @param dis A Java&trade; class file
     */
    private void disasmClassFile(DataInputStream dis) throws IOException {

        // Load the class file.
        ClassFile cf = new ClassFile(dis);

        // Print JDK version.
        println();
        println("// Class file version = " + cf.getJdkName());

        this.thisClassPackageName = cf.thisClassName.substring(0, cf.thisClassName.lastIndexOf('.') + 1);

        // Print package declaration.
        if (this.thisClassPackageName.length() > 0) {
            println();
            println("package " + this.thisClassPackageName.substring(0, this.thisClassPackageName.length() - 1) + ";");
        }

        // Print enclosing method info.
        if (cf.enclosingMethodAttribute != null) {
            String methodName = (
                cf.enclosingMethodAttribute.optionalMethod == null
                ? "[initializer]"
                : cf.enclosingMethodAttribute.optionalMethod.name.bytes
            );
            String className = cf.enclosingMethodAttribute.clasS.name;
            println();
            println(
                "// This class is enclosed by method '"
                + beautify(className)
                + ("<init>".equals(methodName) ? "(...)" : "." + methodName + "(...)")
                + "'."
            );
        }

        println();

        // Print SYNTHETIC notice.
        if ((cf.accessFlags & ACC_SYNTHETIC) != 0 || cf.syntheticAttribute != null) {
            this.println("// This is a synthetic class.");
        }

        // Print DEPRECATED notice.
        if (cf.deprecatedAttribute != null) this.println("/** @deprecated */");

        // Print type annotations.
        if (cf.runtimeInvisibleAnnotationsAttribute != null) {
            for (Annotation a : cf.runtimeInvisibleAnnotationsAttribute.annotations) {
                println(a.toString());
            }
        }
        if (cf.runtimeVisibleAnnotationsAttribute != null) {
            for (Annotation a : cf.runtimeVisibleAnnotationsAttribute.annotations) {
                println(a.toString());
            }
        }

        // Print type access flags.
        this.print(
            decodeAccess((short) (
                cf.accessFlags
                & ~ACC_SYNCHRONIZED // Has no meaning but is always set for backwards compatibility
                & ~ACC_SYNTHETIC // SYNTHETIC has already been printed as a comment.
                // Suppress redundant "abstract" modifier for interfaces.
                & ((cf.accessFlags & ACC_INTERFACE) != 0 ? ~ACC_ABSTRACT : 0xffff)
                // Suppress redundant "final" modifier for enums.
                & ((cf.accessFlags & ACC_ENUM) != 0 ? ~ACC_FINAL : 0xffff)
            ))
            + ((cf.accessFlags & (ACC_ENUM | ACC_ANNOTATION | ACC_INTERFACE)) == 0 ? "class " : "")
        );

        // Print name, EXTENDS and IMPLEMENTS clauses.
        if (cf.signatureAttribute != null) {
            this.print(beautify(decodeClassSignature(cf.signatureAttribute.signature).toString(cf.thisClassName)));
        } else {
            this.print(beautify(cf.thisClassName));
            if (cf.superClassName != null && !"java.lang.Object".equals(cf.superClassName)) {
                this.print(" extends " + beautify(cf.superClassName));
            }
            List<String> ifs = cf.interfaceNames;
            if ((cf.accessFlags & ACC_ANNOTATION) != 0 && ifs.contains("java.lang.annotation.Annotation")) {
                ifs = new ArrayList<String>(ifs);
                ifs.remove("java.lang.annotation.Annotation");
            }
            if (!ifs.isEmpty()) {
                Iterator<String> it = ifs.iterator();
                this.print(" implements " + beautify(it.next()));
                while (it.hasNext()) {
                    this.print(", " + beautify(it.next()));
                }
            }
        }

        this.println(" {");

        // Dump the constant pool.
        {
            if (this.verbose) {
                println();
                println("    // Constant pool dump:");
                ConstantPool cp = cf.constantPool;
                for (int i = 0; i < cp.getSize(); i++) {
                    ConstantPoolEntry constantPoolEntry = cp.get((short) i);
                    if (constantPoolEntry == null) continue;
                    println("    //   #" + i + ": " + beautify(constantPoolEntry.toString()));
                }
            } else {
                println();
                println("    // Constant pool size: " + cf.constantPool.getSize());
            }
        }

        // Print enclosing/enclosed types.
        if (cf.innerClassesAttribute != null) {
            println();
            println("    // Enclosing/enclosed types:");
            for (InnerClassesAttribute.ClasS c : cf.innerClassesAttribute.classes) {
                println("    //   " + toString(c));
            }
        }

        // Print fields.
        disasm(cf.fields);

        // Read source file.
        Map<Integer, String> sourceLines = new HashMap<Integer, String>();
        READ_SOURCE_LINES:
        if (cf.sourceFileAttribute != null && this.sourceDirectory != null) {
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
                    sourceLines.put(lnr.getLineNumber(), sl);
                }
            } finally {
                lnr.close();
            }
        }

        // Methods.
        for (Method m : cf.methods) {
            disasm(m, cf, sourceLines);
        }

        println("}");

        // Print class attributes.
        print(cf.attributes, "// ", new Attribute[] {
            cf.deprecatedAttribute,
            cf.enclosingMethodAttribute,
            cf.innerClassesAttribute,
            cf.runtimeInvisibleAnnotationsAttribute,
            cf.runtimeVisibleAnnotationsAttribute,
            cf.signatureAttribute,
            cf.sourceFileAttribute,
            cf.syntheticAttribute,
        }, AttributeVisitor.Context.CLASS);
    }

    /**
     * Disassemble one method.
     */
    private void disasm(Method method, ClassFile cf, Map<Integer, String> sourceLines) {
        try {
        println();

        // Print SYNTHETIC notice.
        if ((method.accessFlags & ACC_SYNTHETIC) != 0 || method.syntheticAttribute != null) {
            println("    // (Synthetic method)");
        }

        // Print BRIDGE notice.
        if ((method.accessFlags & ACC_BRIDGE) != 0) {
            println("    // (Bridge method)");
        }

        // Print DEPRECATED notice.
        if (method.deprecatedAttribute != null) this.println("    /** @deprecated */");

        // Print method annotations.
        if (method.runtimeInvisibleAnnotationsAttribute != null) {
            for (Annotation a : method.runtimeInvisibleAnnotationsAttribute.annotations) {
                println("    " + a.toString());
            }
        }
        if (method.runtimeVisibleAnnotationsAttribute != null) {
            for (Annotation a : method.runtimeVisibleAnnotationsAttribute.annotations) {
                println("    " + a.toString());
            }
        }

        // Print method access flags.
        String functionName = method.name;
        Disassembler.this.print(
            "    "
            + decodeAccess((short) (
                method.accessFlags
                & ~ACC_SYNTHETIC
                & ~ACC_BRIDGE
                & ~ACC_VARARGS
                & ((cf.accessFlags & ACC_INTERFACE) != 0 ? ~(ACC_PUBLIC | ACC_ABSTRACT) : 0xffff)
            ))
        );

        // Print formal type parameters.
        MethodTypeSignature mts;
        {
            mts = (
                method.signatureAttribute == null
                ? decodeMethodDescriptor(method.descriptor)
                : decodeMethodTypeSignature(method.signatureAttribute.signature)
            );
            if (!mts.formalTypeParameters.isEmpty()) {
                    Iterator<FormalTypeParameter> it = mts.formalTypeParameters.iterator();
                print("<" + beautify(it.next().toString()));
                while (it.hasNext()) print(", " + beautify(it.next().toString()));
                print(">");
            }
        }

        // Print method name.
        if (
            "<clinit>".equals(functionName)
            && (method.accessFlags & ACC_STATIC) != 0
            && (method.exceptionsAttribute == null || method.exceptionsAttribute.exceptionNames.isEmpty())
            && mts.formalTypeParameters.isEmpty()
            && mts.parameterTypes.isEmpty()
            && mts.returnType == SignatureParser.VOID
            && mts.thrownTypes.isEmpty()
        ) {
            ;
        } else
        if (
            "<init>".equals(functionName)
            && (method.accessFlags & (ACC_ABSTRACT | ACC_FINAL | ACC_INTERFACE | ACC_STATIC)) == 0
            && mts.returnType == SignatureParser.VOID
        ) {
            print(beautify(cf.thisClassName));
            print(
                method.runtimeInvisibleParameterAnnotationsAttribute,
                method.runtimeVisibleParameterAnnotationsAttribute,
                mts.parameterTypes,
                method,
                (short) 1,
                (method.accessFlags & ACC_VARARGS) != 0
            );
        } else
        {
            print(beautify(mts.returnType.toString()) + ' ');
            print(functionName);
            print(
                method.runtimeInvisibleParameterAnnotationsAttribute,
                method.runtimeVisibleParameterAnnotationsAttribute,
                mts.parameterTypes,
                method,
                (method.accessFlags & ACC_STATIC) == 0 ? (short) 1 : (short) 0,
                (method.accessFlags & ACC_VARARGS) != 0
            );
        }

        // Thrown types.
        if (mts.thrownTypes != null && !mts.thrownTypes.isEmpty()) {
            Iterator<ThrowsSignature> it = mts.thrownTypes.iterator();
            print(" throws " + beautify(it.next().toString()));
            while (it.hasNext()) print(", " + beautify(it.next().toString()));
        } else
        if (method.exceptionsAttribute != null && !method.exceptionsAttribute.exceptionNames.isEmpty()) {
            Iterator<ConstantClassInfo> it = method.exceptionsAttribute.exceptionNames.iterator();
            print(" throws " + beautify(it.next().name));
            while (it.hasNext()) print(", " + beautify(it.next().name));
        }

        // Annotation default.
        if (method.annotationDefaultAttribute != null) {
            print("default " + method.annotationDefaultAttribute.defaultValue);
        }

        // Code.
        if (method.codeAttribute == null) {
            println(";");
        } else {
            println(" {");
            CodeAttribute ca = method.codeAttribute;
            try {
                disasmBytecode(
                    new ByteArrayInputStream(ca.code),
                    ca.exceptionTable,
                    ca.lineNumberTableAttribute,
                    sourceLines,
                    cf.constantPool,
                    method
                );
            } catch (IOException ignored) {
                ;
            }
            println("    }");
        }

        // Print method attributes.
        print(method.attributes, "    // ", new Attribute[] {
            method.annotationDefaultAttribute,
            method.codeAttribute,
            method.deprecatedAttribute,
            method.exceptionsAttribute,
            method.runtimeInvisibleAnnotationsAttribute,
            method.runtimeInvisibleParameterAnnotationsAttribute,
            method.runtimeVisibleAnnotationsAttribute,
            method.runtimeVisibleParameterAnnotationsAttribute,
            method.signatureAttribute,
            method.syntheticAttribute,
            }, AttributeVisitor.Context.METHOD);
        } catch (RuntimeException rte) {
            throw new RuntimeException("Method '" + method.name + "' " + method.descriptor, rte);
        }
    }

    private void disasm(List<Field> fields) {
        for (Field field : fields) {
            println();

            // Print field annotations.
            if (field.runtimeInvisibleAnnotationsAttribute != null) {
                for (Annotation a : field.runtimeInvisibleAnnotationsAttribute.annotations) {
                    println("    " + a.toString());
                }
            }
            if (field.runtimeVisibleAnnotationsAttribute != null) {
                for (Annotation a : field.runtimeVisibleAnnotationsAttribute.annotations) {
                    println("    " + a.toString());
                }
            }

            // print SYNTHETIC notice.
            if ((field.accessFlags & ACC_SYNTHETIC) != 0 || field.syntheticAttribute != null) {
                println("    // (Synthetic field)");
            }

            // Print DEPRECATED notice.
            if (field.deprecatedAttribute != null) println("    /** @deprecated */");

            // Print field access flags and field type.
            String parametrizedType = beautify(
                field.signatureAttribute == null
                ? decodeFieldDescriptor(field.descriptor).toString()
                : decodeFieldTypeSignature(field.signatureAttribute.signature).toString()
            );
            String prefix = (
                "    "
                + decodeAccess((short) (field.accessFlags & ~ACC_SYNTHETIC))
                + parametrizedType
                + " "
            );

            // Print field name and initializer.
            if (field.constantValueAttribute == null) {
                printf("%-40s %s;%n", prefix, field.name);
            } else {
                printf("%-40s %-15s = %s;%n", prefix, field.name, field.constantValueAttribute.constantValue);
            }

            // Print field attributes.
            print(field.attributes, "    // ", new Attribute[] {
                field.constantValueAttribute,
                field.deprecatedAttribute,
                field.runtimeInvisibleAnnotationsAttribute,
                field.runtimeVisibleAnnotationsAttribute,
                field.signatureAttribute,
                field.syntheticAttribute,
            }, AttributeVisitor.Context.FIELD);
        }
    }

    String toString(InnerClassesAttribute.ClasS c) {
        return (
            (c.outerClassInfo == null ? "[local class]" : beautify(c.outerClassInfo.name))
            + " { "
            + decodeAccess((short) (c.innerClassAccessFlags & ( // Hide ABSTRACT and STATIC for interfaces
                (c.innerClassAccessFlags & ACC_INTERFACE) != 0
                ? (~ACC_ABSTRACT & ~ACC_STATIC)
                : 0xffff
            )))
            + (c.innerClassInfo == null ? "[???]" : beautify(c.innerClassInfo.name)) + " }"
        );
    }

    private void print(
        List<Attribute> attributes,
        String          prefix,
        Attribute[]              excludedAttributes,
        AttributeVisitor.Context context
    ) {
        List<Attribute> tmp = new ArrayList<Attribute>(attributes);

        // Strip excluded attributes.
        if (!this.verbose) {
            tmp.removeAll(Arrays.asList(excludedAttributes));
            }
        if (tmp.isEmpty()) return;

        Collections.sort(tmp, new Comparator<Attribute>() {
            public int compare(Attribute a1, Attribute a2) { return a1.getName().compareTo(a2.getName()); }
        });

        println(prefix + (this.verbose ? "Attributes:" : "Unprocessed attributes:"));
        PrintAttributeVisitor visitor = new PrintAttributeVisitor(prefix + "  ", context);
        for (Attribute a : tmp) {
            a.accept(visitor);
        }
    }

    /**
     * Prints an {@link Attribute}.
     */
    public class PrintAttributeVisitor implements AttributeVisitor {

        private final String prefix;
        private final Context context;

        public PrintAttributeVisitor(String prefix, Context context) {
            this.prefix = prefix;
            this.context = context;
        }

        public void visit(AnnotationDefaultAttribute ada) {
            println(this.prefix + "AnnotationDefault:");
            println(this.prefix + "  " + ada.defaultValue.toString());
        }

        public void visit(CodeAttribute ca) {
            println(this.prefix + "Code:");
            println(this.prefix + "  max_locals = " + ca.maxLocals);
            println(this.prefix + "  max_stack = " + ca.maxStack);

            if (ca.code != null) {
                println(this.prefix + "  code = {");
                print(ca.code);
                println(this.prefix + "  }");
            }

            if (!ca.attributes.isEmpty()) {
                println(this.prefix + "  attributes = {");
                PrintAttributeVisitor pav = new PrintAttributeVisitor(this.prefix + "    ", Context.METHOD);
                List<Attribute> tmp = ca.attributes;
                Collections.sort(tmp, new Comparator<Attribute>() {
                    public int compare(Attribute a1, Attribute a2) { return a1.getName().compareTo(a2.getName()); }
                });
                for (Attribute a : tmp) {
                    a.accept(pav);
                }
                println(this.prefix + "  }");
            }
        }

        private void print(byte[] data) {
            for (int i = 0; i < data.length; i += 32) {
                Disassembler.this.print(this.prefix + "   ");
                for (int j = 0; j < 32; ++j) {
                    int idx = i + j;
                    if (idx >= data.length) break;
                    printf("%c%02x", j == 16 ? '-' : ' ', 0xff & data[idx]);
                }
                println();
            }
        }

        public void visit(ConstantValueAttribute cva) {
            println(this.prefix + "ConstantValue:");
            println(this.prefix + "  constant_value = " + cva.constantValue);
        }

        public void visit(DeprecatedAttribute da) {
            println(this.prefix + "DeprecatedAttribute:");
            println(this.prefix + "  -");
        }

        public void visit(EnclosingMethodAttribute ema) {
            println(this.prefix + "EnclosingMethod:");
            println(this.prefix + "  class/method = " + (
                ema.optionalMethod == null
                ? "(none)"
                : beautify(decodeMethodDescriptor(
                    ema.optionalMethod.descriptor.bytes
                ).toString(ema.clasS.name, ema.optionalMethod.name.bytes))
            ));
        }

        public void visit(ExceptionsAttribute ea) {
            println(this.prefix + "Exceptions:");
            for (ConstantClassInfo en : ea.exceptionNames) {
                println(this.prefix + "  " + en.name);
            }
        }

        public void visit(InnerClassesAttribute ica) {
            println(this.prefix + "InnerClasses:");
            for (InnerClassesAttribute.ClasS c : ica.classes) {
                println(this.prefix + "  " + Disassembler.this.toString(c));
            }
        }

        public void visit(LineNumberTableAttribute lnta) {
            println(this.prefix + "LineNumberTable:");
            for (LineNumberTableEntry e : lnta.entries) {
                println(this.prefix + "  " + e.startPC + " => Line " + e.lineNumber);
            }
        }

        public void visit(LocalVariableTableAttribute lvta) {
            println(this.prefix + "LocalVariableTable:");
            for (LocalVariableTableAttribute.Entry e : lvta.entries) {
                println(
                    this.prefix
                    + "  "
                    + (0xffff & e.startPC)
                    + "+"
                    + e.length
                    + ": "
                    + e.index
                    + " = "
                    + beautify(decodeFieldDescriptor(e.descriptor).toString())
                    + " "
                    + e.name
                );
            }
        }

        public void visit(LocalVariableTypeTableAttribute lvtta) {
            println(this.prefix + "LocalVariableTypeTable:");
            for (LocalVariableTypeTableAttribute.Entry e : lvtta.entries) {
                println(
                    this.prefix
                    + "  "
                    + e.startPC
                    + "+"
                    + e.length
                    + ": "
                    + e.index
                    + " = "
                    + beautify(decodeFieldTypeSignature(e.signature).toString())
                    + " "
                    + e.name
                );
            }
        }

        public void visit(RuntimeInvisibleAnnotationsAttribute riaa) {
            println(this.prefix + "RuntimeInvisibleAnnotations:");
            for (Annotation a : riaa.annotations) {
                println(this.prefix + "  " + a.toString());
            }
        }

        public void visit(RuntimeVisibleAnnotationsAttribute rvaa) {
            println(this.prefix + "RuntimeVisibleAnnotations:");
            for (Annotation a : rvaa.annotations) {
                println(this.prefix + "  " + a.toString());
            }
        }

        public void visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
            println(this.prefix + "RuntimeInvisibleParameterAnnotations:");
            for (ParameterAnnotation pa : ripaa.parameterAnnotations) {
                for (Annotation a : pa.annotations) {
                    println(this.prefix + "  " + a.toString());
                }
            }
        }

        public void visit(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
            println(this.prefix + "RuntimeVisibleParameterAnnotations:");
            for (ParameterAnnotation pa : rvpaa.parameterAnnotations) {
                for (Annotation a : pa.annotations) {
                    println(this.prefix + "  " + a.toString());
                }
            }
        }

        public void visit(SignatureAttribute sa) {
            println(this.prefix + "Signature:");
            switch (this.context) {
            case CLASS:
                println(this.prefix + "  " + decodeClassSignature(sa.signature).toString("[this-class]"));
                break;
            case FIELD:
                println(this.prefix + "  " + decodeFieldTypeSignature(sa.signature).toString());
                break;
            case METHOD:
                println(this.prefix + "  " + decodeMethodTypeSignature(sa.signature).toString("[declaring-class]", "[this-method]"));
                break;
            }
        }

        public void visit(SourceFileAttribute sfa) {
            println(this.prefix + "SourceFile:");
            println(this.prefix + "  " + sfa.sourceFile);
        }

        public void visit(SyntheticAttribute sa) {
            println(this.prefix + "Synthetic:");
            println(this.prefix + " -");
        }

        public void visit(UnknownAttribute ua) {
            println(this.prefix + ua.name + ":");
            println(this.prefix + "  data = {");
            print(ua.info);
            println(this.prefix + "}");
        }
    }

    private void print(
        RuntimeInvisibleParameterAnnotationsAttribute ripaa,
        RuntimeVisibleParameterAnnotationsAttribute   rvpaa,
        List<TypeSignature>                           parameterTypes,
        Method                                        method,
        short                                         firstIndex,
        boolean                                       varargs
    ) {
        Iterator<ParameterAnnotation> ipas = (
            ripaa == null
            ? NO_PARAMETER_ANNOTATIONS
            : ripaa.parameterAnnotations
        ).iterator();
        Iterator<ParameterAnnotation> vpas = (
            rvpaa == null
            ? NO_PARAMETER_ANNOTATIONS
            : rvpaa.parameterAnnotations
        ).iterator();

        print("(");
        Iterator<TypeSignature> it = parameterTypes.iterator();
        if (it.hasNext()) {
            for (;;) {
                TypeSignature pts = it.next();

                // Parameter annotations.
                if (ipas.hasNext()) for (Annotation a : ipas.next().annotations) print(a.toString() + ' ');
                if (vpas.hasNext()) for (Annotation a : vpas.next().annotations) print(a.toString() + ' ');

                // Parameter type.
                if (varargs && !it.hasNext() && pts instanceof ArrayTypeSignature) {
                    print(beautify(((ArrayTypeSignature) pts).typeSignature.toString()) + "...");
                } else {
                    print(beautify(pts.toString()));
                }

                // Parameter name.
                print(' ' + getLocalVariable(firstIndex, 0, method).name);

                if (!it.hasNext()) break;
                firstIndex++;
                print(", ");
            }
        }
        print(")");
    }

    /**
     * Read byte code from the given {@link InputStream} and disassemble it.
     */
    private void disasmBytecode(
        InputStream               is,
        List<ExceptionTableEntry> exceptionTable,
        LineNumberTableAttribute  lineNumberTableAttribute,
        Map<Integer, String>      sourceLines,
        ConstantPool              cp,
        Method                    method
    ) throws IOException {
        CountingInputStream cis = new CountingInputStream(is);
        DataInputStream     dis = new DataInputStream(cis);

        this.branchTargets = new HashSet<Integer>();
        try {

            // Analyze TRY bodies.

            // startPC => [ endPC ]
            SortedMap<Integer, List<Integer>> tryStarts = new TreeMap<Integer, List<Integer>>();

            // endPC => startPC => [ ExceptionTableEntry ]
            SortedMap<Integer, SortedMap<Integer, List<ExceptionTableEntry>>> tryEnds = (
                new TreeMap<Integer, SortedMap<Integer, List<ExceptionTableEntry>>>()
            );

            for (ExceptionTableEntry e : exceptionTable) {

                // Register the entry in "tryStarts".
                {
                    List<Integer> l = tryStarts.get(e.startPC);
                    if (l == null) {
                        l = new ArrayList<Integer>();
                        tryStarts.put(e.startPC, l);
                    }
                    l.add(e.endPC);
                }

                // Register the entry in "tryEnds".
                {
                    SortedMap<Integer, List<ExceptionTableEntry>> m = tryEnds.get(e.endPC);
                    if (m == null) {
                        m = new TreeMap<Integer, List<ExceptionTableEntry>>(Collections.reverseOrder());
                        tryEnds.put(e.endPC, m);
                    }
                    List<ExceptionTableEntry> l = m.get(e.startPC);
                    if (l == null) {
                        l = new ArrayList<ExceptionTableEntry>();
                        m.put(e.startPC, l);
                    }
                    l.add(e);
                }
                this.branchTargets.add(e.handlerPC);
            }

            // Disassemble the byte code into a sequence of lines.
            SortedMap<Integer /*instructionOffset*/, String /*text*/> lines = new TreeMap<Integer, String>();
            for (;;) {
                int instructionOffset = (int) cis.getCount();

                int opcode = dis.read();
                if (opcode == -1) break;

                Instruction instruction = OPCODE_TO_INSTRUCTION[opcode];
                if (instruction == null) {
                    lines.put(instructionOffset, "??? (invalid opcode \"" + opcode + "\")");
                } else {
                    try {
                    lines.put(instructionOffset, instruction.getMnemonic() + disasmOperands(
                        instruction.getOperands(),
                        dis,
                        instructionOffset,
                        method,
                        cp
                    ));
                    } catch (RuntimeException rte) {
                        for (Iterator<Entry<Integer, String>> it = lines.entrySet().iterator(); it.hasNext();) {
                            Entry<Integer, String> e = it.next();
                            println("#" + e.getKey() + " " + e.getValue());
                        }
                        throw new RuntimeException("Instruction '" + instruction + "', pc=" + instructionOffset, rte);
                    }
                }
            }

            // Format and print the disassembly lines.
            String indentation = "        ";
            for (Entry<Integer, String> e : lines.entrySet()) {
                int instructionOffset = e.getKey();
                String text = e.getValue();

                // Print ends of TRY bodies.
                for (Iterator<Entry<Integer, SortedMap<Integer, List<ExceptionTableEntry>>>> it = tryEnds.entrySet().iterator(); it.hasNext();) {
                    Entry<Integer, SortedMap<Integer, List<ExceptionTableEntry>>> e2 = it.next();
                    int endPC = e2.getKey().intValue();
                    if (endPC > instructionOffset) break;

                    SortedMap<Integer, List<ExceptionTableEntry>> startPC2ETE = e2.getValue();
                    for (Entry<Integer, List<ExceptionTableEntry>> e3 : startPC2ETE.entrySet()) {
                        List<ExceptionTableEntry> etes = e3.getValue();
                        if (endPC < instructionOffset) {
                            error("Exception table entry ends at invalid code array index " + endPC + " (current instruction offset is " + instructionOffset + ")");
                        }
                        indentation = indentation.substring(4);
                        print(indentation + "} catch (");
                        for (Iterator<ExceptionTableEntry> it2 = etes.iterator();;) {
                            ExceptionTableEntry ete = it2.next();
                            print(
                                (ete.catchType == null ? "[all exceptions]" : beautify(ete.catchType.name))
                                + " => "
                                + ete.handlerPC
                            );
                            if (!it2.hasNext()) break;
                            print(", ");
                        }
                        println(")");
                    }
                    it.remove();
                }

                // Print instruction offsets only for branch targets.
                if (this.branchTargets.contains(instructionOffset)) {
                    this.println("#" + instructionOffset);
                }

                // Print beginnings of TRY bodies.
                for (Iterator<Entry<Integer, List<Integer>>> it = tryStarts.entrySet().iterator(); it.hasNext();) {
                    Entry<Integer, List<Integer>> sc = it.next();
                    Integer startPC = sc.getKey();
                    if (startPC > instructionOffset) break;

                    for (int i = sc.getValue().size(); i > 0; i--) {
                        if (startPC < instructionOffset) {
                            error("Exception table entry starts at invalid code array index " + startPC + " (current instruction offset is " + instructionOffset + ")");
                        }
                        println(indentation + "try {");
                        indentation += "    ";
                    }
                    it.remove();
                }

                // Print source line and/or line number.
                PRINT_SOURCE_LINE: {
                    if (lineNumberTableAttribute == null) break PRINT_SOURCE_LINE;

                    int lineNumber = findLineNumber(lineNumberTableAttribute, instructionOffset);
                    if (lineNumber == -1) break PRINT_SOURCE_LINE;

                    String sourceLine = sourceLines.get(lineNumber);
                    if (sourceLine == null && this.hideLines) break PRINT_SOURCE_LINE;

                    StringBuilder sb = new StringBuilder(indentation);
                    if (sourceLine == null) {
                        sb.append("// Line ").append(lineNumber);
                    } else {
                        sb.append("// ");
                        if (sb.length() < 40) {
                            char[] spc = new char[40 - sb.length()];
                            Arrays.fill(spc, ' ');
                            sb.append(spc);
                        }
                        if (!this.hideLines) {
                            sb.append("Line ").append(lineNumber).append(": ");
                        }
                        sb.append(sourceLine);
                    }
                    println(sb.toString());
                }

                this.println(indentation + text);
            }
        } finally {
            this.branchTargets = null;
        }
    }

    /**
     * @return -1 iff the offset is not associated with a line number
     */
    private static int findLineNumber(
        LineNumberTableAttribute lnta,
        int                      offset
    ) {
        for (LineNumberTableEntry lnte : lnta.entries) {
            if (lnte.startPC == offset) return lnte.lineNumber;
        }
        return -1;
    }

    /**
     * @return The {@code instruction} converted into one line of text.
     */
    String disasmOperands(
        Operand[]       operands,
        DataInputStream dis,
        int             instructionOffset,
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

    private static final Instruction[] OPCODE_TO_INSTRUCTION = compileInstructions(new String[] {
        "50  aaload",
        "83  aastore",
        "1   aconst_null",
        "25  aload           localvariableindex1",
        "42  aload_0         implicitlocalvariableindex",
        "43  aload_1         implicitlocalvariableindex",
        "44  aload_2         implicitlocalvariableindex",
        "45  aload_3         implicitlocalvariableindex",
        "189 anewarray       class2",
        "176 areturn",
        "190 arraylength",
        "58  astore          localvariableindex1",
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
        "24  dload           localvariableindex1",
        "38  dload_0         implicitlocalvariableindex",
        "39  dload_1         implicitlocalvariableindex",
        "40  dload_2         implicitlocalvariableindex",
        "41  dload_3         implicitlocalvariableindex",
        "107 dmul",
        "119 dneg",
        "115 drem",
        "175 dreturn",
        "57  dstore          localvariableindex1",
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
        "23  fload           localvariableindex1",
        "34  fload_0         implicitlocalvariableindex",
        "35  fload_1         implicitlocalvariableindex",
        "36  fload_2         implicitlocalvariableindex",
        "37  fload_3         implicitlocalvariableindex",
        "106 fmul",
        "118 fneg",
        "114 frem",
        "174 freturn",
        "56  fstore          localvariableindex1",
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
        "132 iinc            localvariableindex1 signedbyte",
        "21  iload           localvariableindex1",
        "26  iload_0         implicitlocalvariableindex",
        "27  iload_1         implicitlocalvariableindex",
        "28  iload_2         implicitlocalvariableindex",
        "29  iload_3         implicitlocalvariableindex",
        "104 imul",
        "116 ineg",
        "193 instanceof      class2",
    //      "186 invokedynamic   invokedynamic2", // For Java 7; see http://cr.openjdk.java.net/~jrose/pres/indy-javadoc-mlvm/java/lang/invoke/package-summary.html
        "185 invokeinterface interfacemethodref2",
        "183 invokespecial   methodref2",
        "184 invokestatic    methodref2",
        "182 invokevirtual   methodref2",
        "128 ior",
        "112 irem",
        "172 ireturn",
        "120 ishl",
        "122 ishr",
        "54  istore          localvariableindex1",
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
        "22  lload           localvariableindex1",
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
        "55  lstore          localvariableindex1",
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
        "169 ret             localvariableindex1",
        "177 return",
        "53  saload",
        "86  sastore",
        "17  sipush          signedshort",
        "95  swap",
        "170 tableswitch     tableswitch",
        "196 wide            wide",
    });

    static final Instruction[] OPCODE_TO_WIDE_INSTRUCTION = compileInstructions(new String[] {
        "21  iload           localvariableindex2",
        "23  fload           localvariableindex2",
        "25  aload           localvariableindex2",
        "22  lload           localvariableindex2",
        "24  dload           localvariableindex2",
        "54  istore          localvariableindex2",
        "56  fstore          localvariableindex2",
        "58  astore          localvariableindex2",
        "55  lstore          localvariableindex2",
        "57  dstore          localvariableindex2",
        "169 ret             localvariableindex2",
        "132 iinc            localvariableindex2 signedshort",
    });

    private static Instruction[] compileInstructions(String[] instructions) {
        Instruction[] result = new Instruction[256];

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
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = (short) (0xff & dis.readByte());
                                String t = cp.getIntegerFloatClassString(index);
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("intfloatclassstring2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                String t = cp.getIntegerFloatClassString(index);
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("longdouble2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                String t = cp.getLongDoubleString(index);
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("fieldref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                ConstantFieldrefInfo fr = cp.getConstantFieldrefInfo(index);
                                String t = (
                                    d.beautify(d.decodeFieldDescriptor(fr.nameAndType.descriptor.bytes).toString())
                                    + ' '
                                    + d.beautify(fr.clasS.name)
                                    + '.'
                                    + fr.nameAndType.name.bytes
                                );
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("methodref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                ConstantMethodrefInfo mr = cp.getConstantMethodrefInfo(index);
                                String t = d.beautify(
                                    d.decodeMethodDescriptor(mr.nameAndType.descriptor.bytes).toString(
                                        mr.clasS.name,
                                        mr.nameAndType.name.bytes
                                    )
                                );
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("interfacemethodref2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                ConstantInterfaceMethodrefInfo imr = cp.getConstantInterfaceMethodrefInfo(index);
                                dis.readByte();
                                dis.readByte();
                                String t = d.beautify(
                                    d.decodeMethodDescriptor(imr.nameAndType.descriptor.bytes).toString(
                                        imr.clasS.name,
                                        imr.nameAndType.name.bytes
                                    )
                                );
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("class2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                String name = cp.getConstantClassInfo(index).name;
                                String t = d.beautify(
                                    name.startsWith("[")
                                    ? d.decodeFieldDescriptor(name).toString()
                                    : name.replace('/', '.')
                                );
                                if (d.verbose) t += " (#" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if (s.equals("localvariableindex1")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                byte index = dis.readByte();
                                // For an initial assignment (e.g. 'istore 7'), the local variable is only visible
                                // AFTER this instruction.
                                LocalVariable lv = d.getLocalVariable(
                                    (short) (0xff & index),
                                    instructionOffset + 2,
                                    method
                                );
                                return d.beautify(lv.toString());
                            }
                        };
                    } else
                    if (s.equals("localvariableindex2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short index = dis.readShort();
                                // For an initial assignment (e.g. 'wide istore 300'), the local variable is only
                                // visible AFTER this instruction.
                                LocalVariable lv = d.getLocalVariable(index, instructionOffset + 4, method);
                                return d.beautify(lv.toString());
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
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) {
                                // For an initial assignment (e.g. 'istore_3'), the local variable is only visible
                                // AFTER this instruction.
                                this.lv = d.getLocalVariable(index, instructionOffset + 1, method);
                                return d.beautify(this.lv.toString());
                            }
                        };
                    } else
                    if (s.equals("branchoffset2")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int branchTarget = instructionOffset + dis.readShort();
                                d.branchTargets.add(branchTarget);
                                return " #" + branchTarget;
                            }
                        };
                    } else
                    if (s.equals("branchoffset4")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int branchTarget = instructionOffset + dis.readInt();
                                d.branchTargets.add(branchTarget);
                                return " #" + branchTarget;
                            }
                        };
                    } else
                    if (s.equals("signedbyte")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
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
                                int             instructionOffset,
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
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                byte b = dis.readByte();
                                return (
                                    b ==  4 ? " BOOLEAN"
                                    : b ==  5 ? " CHAR"
                                    : b ==  6 ? " FLOAT"
                                    : b ==  7 ? " DOUBLE"
                                    : b ==  8 ? " BYTE"
                                    : b ==  9 ? " SHORT"
                                    : b == 10 ? " INT"
                                    : b == 11 ? " LONG"
                                    : " " + (0xff & b)
                                );
                            }
                        };
                    } else
                    if (s.equals("signedshort")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
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
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != 0) {
                                        throw new RuntimeException(
                                            "'tableswitch' pad byte #"
                                            + i
                                            + " is not zero, but "
                                            + (0xff & padByte)
                                        );
                                    }
                                }
                                StringBuilder sb = new StringBuilder(" default => ");
                                {
                                    int defaultOffset = instructionOffset + dis.readInt();
                                    sb.append(defaultOffset);
                                    d.branchTargets.add(defaultOffset);
                                }
                                int low = dis.readInt();
                                int high = dis.readInt();
                                for (int i = low; i <= high; ++i) {
                                    int offset = instructionOffset + dis.readInt();
                                    d.branchTargets.add(offset);
                                    sb.append(", ").append(i).append(" => ").append(offset);
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if (s.equals("lookupswitch")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int npads = 3 - (instructionOffset % 4);
                                for (int i = 0; i < npads; ++i) {
                                    byte padByte = dis.readByte();
                                    if (padByte != (byte) 0) {
                                        throw new RuntimeException(
                                            "'tableswitch' pad byte #"
                                            + i
                                            + " is not zero, but "
                                            + (0xff & padByte)
                                        );
                                    }
                                }
                                StringBuilder sb = new StringBuilder(" default => ");
                                {
                                    int defaultOffset = instructionOffset + dis.readInt();
                                    sb.append(defaultOffset);
                                    d.branchTargets.add(defaultOffset);
                                }
                                int npairs = dis.readInt();
                                for (int i = 0; i < npairs; ++i) {
                                    int match  = dis.readInt();
                                    int offset = instructionOffset + dis.readInt();
                                    sb.append(", ").append(match).append(" => ").append(offset);
                                    d.branchTargets.add(offset);
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if (s.equals("wide")) {
                        operand = new Operand() {
                            public String disasm(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int subopcode = 0xff & dis.readByte();
                                Instruction wideInstruction = OPCODE_TO_WIDE_INSTRUCTION[subopcode];
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
            result[opcode] = new Instruction(mnemonic, operands);
        }
        return result;
    }

    LocalVariable getLocalVariable(
        short  localVariableIndex,
        int    instructionOffset,
        Method method
    ) {
        LocalVariable lv = new LocalVariable();
        int firstParameter = (method.accessFlags & ACC_STATIC) == 0 ? 1 : 0;
        if (localVariableIndex < firstParameter) {
            lv.name = "this";
            return lv;
        }
        MethodTypeSignature mts = (
            method.signatureAttribute != null
            ? decodeMethodTypeSignature(method.signatureAttribute.signature)
            : decodeMethodDescriptor(method.descriptor)
        );
        if (localVariableIndex < firstParameter + mts.parameterTypes.size()) {
            lv.name = "p" + (1 + localVariableIndex - firstParameter);
            lv.optionalTypeSignature = mts.parameterTypes.get(localVariableIndex - firstParameter);
        } else
        {
            lv.name = "v" + (1 + localVariableIndex - firstParameter - mts.parameterTypes.size());
        }
        if (method.codeAttribute != null) {
            if (method.codeAttribute.localVariableTypeTableAttribute != null) {
                for (
                    LocalVariableTypeTableAttribute.Entry lvtte
                    : method.codeAttribute.localVariableTypeTableAttribute.entries
                ) {
                    if (
                        instructionOffset >= lvtte.startPC
                        && instructionOffset <= lvtte.startPC + lvtte.length
                        && localVariableIndex == lvtte.index
                    ) {
                        lv.optionalTypeSignature = decodeFieldTypeSignature(lvtte.signature);
                        if (!this.hideVars) lv.name =  lvtte.name;
                        return lv;
                    }
                }
            }
            if (method.codeAttribute.localVariableTableAttribute != null) {
                for (
                    LocalVariableTableAttribute.Entry lvte
                    : method.codeAttribute.localVariableTableAttribute.entries
                ) {
                    if (
                        instructionOffset >= lvte.startPC
                        && instructionOffset <= lvte.startPC + lvte.length
                        && localVariableIndex == lvte.index
                    ) {
                        String fd = lvte.descriptor;
                        lv.optionalTypeSignature = decodeFieldDescriptor(fd);
                        if (!this.hideVars) lv.name = lvte.name;
                        return lv;
                    }
                }
            }
        }
        return lv;
    }

    ClassSignature decodeClassSignature(String cs) {
        try {
            return SignatureParser.decodeClassSignature(cs);
        } catch (SignatureException e) {
            error("Decoding class signature '" + cs + "': " + e.getMessage());

            ClassSignature res = new ClassSignature();
            res.superclassSignature = SignatureParser.OBJECT;
            return res;
        }
    }

    FieldTypeSignature decodeFieldTypeSignature(String fs) {
        try {
            return SignatureParser.decodeFieldTypeSignature(fs);
        } catch (SignatureException e) {
            error("Decoding field type signature '" + fs + "': " + e.getMessage());
            return SignatureParser.OBJECT;
        }
    }

    MethodTypeSignature decodeMethodTypeSignature(String ms) {
        try {
            return SignatureParser.decodeMethodTypeSignature(ms);
        } catch (SignatureException e) {
            error("Decoding method type signature '" + ms + "': " + e.getMessage());

            MethodTypeSignature res = new MethodTypeSignature();
            res.returnType = SignatureParser.VOID;
            return res;
        }
    }

    TypeSignature decodeFieldDescriptor(String fd) {
        try {
            return SignatureParser.decodeFieldDescriptor(fd);
        } catch (SignatureException e) {
            error("Decoding field descriptor '" + fd + "': " + e.getMessage());
            return SignatureParser.INT;
        }
    }

    MethodTypeSignature decodeMethodDescriptor(String md) {
        try {
            return SignatureParser.decodeMethodDescriptor(md);
        } catch (SignatureException e) {
            error("Decoding method descriptor '" + md + "': " + e.getMessage());

            MethodTypeSignature res = new MethodTypeSignature();
            res.returnType = SignatureParser.VOID;
            return res;
        }
    }

    /**
     * Representation of a local variable reference in the {@code Code} attribute.
     */
    class LocalVariable {
        TypeSignature optionalTypeSignature;
        String        name;

        @Override
        public String toString() {
            return (
                this.optionalTypeSignature == null
                ? " [" + this.name + ']'
                : " [" + this.optionalTypeSignature.toString() + ' ' + this.name + ']'
            );

        }
    }

    public void error(String message) {
        this.pw.println("*** Error: " + message);
    }

    /**
     * Static description of a Java&trade; byte code instruction.
     */
    private static class Instruction {

        /**
         * @param operands <code>null</code> is equivalent to "zero operands"
         */
        public Instruction(String mnemonic, Operand[] operands) {
            this.mnemonic = mnemonic;
            this.operands = operands;
        }
        public String    getMnemonic() { return this.mnemonic; }
        public Operand[] getOperands() { return this.operands; }

        @Override
        public String toString() {
            return this.mnemonic;
        }
        private final String    mnemonic;
        private final Operand[] operands;
    }

    /**
     * Static description of an operand of a Java&trade; byte code instruction.
     */
    private interface Operand {
        String disasm(
            DataInputStream dis,
            int             instructionOffset,
            Method          method,
            ConstantPool    cp,
            Disassembler    d
        ) throws IOException;
    }

    /**
     * An {@link InputStream} that counts how many bytes have been read so far.
     */
    private static class CountingInputStream extends FilterInputStream {
        public CountingInputStream(InputStream is) {
            super(is);
        }

        @Override
        public int read() throws IOException {
            int res = super.read();
            if (res != -1) ++this.count;
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res != -1) this.count += res;
            return res;
        }

        public long getCount() { return this.count; }

        private long count = 0L;
    }

    /**
     * Returns a series of words, in canonical order, separated with one space, and with one trailing space.
     */
    private static String decodeAccess(short n) {
        StringBuilder sb = new StringBuilder();
        if ((n & ACC_PUBLIC) != 0)       { sb.append("public ");       n &= ~ACC_PUBLIC; }
        if ((n & ACC_PRIVATE) != 0)      { sb.append("private ");      n &= ~ACC_PRIVATE; }
        if ((n & ACC_PROTECTED) != 0)    { sb.append("protected ");    n &= ~ACC_PROTECTED; }

        if ((n & ACC_ABSTRACT) != 0)     { sb.append("abstract ");     n &= ~ACC_ABSTRACT; }
        if ((n & ACC_STATIC) != 0)       { sb.append("static ");       n &= ~ACC_STATIC; }
        if ((n & ACC_FINAL) != 0)        { sb.append("final ");        n &= ~ACC_FINAL; }
        if ((n & ACC_TRANSIENT) != 0)    { sb.append("transient ");    n &= ~ACC_TRANSIENT; }
        if ((n & ACC_VOLATILE) != 0)     { sb.append("volatile ");     n &= ~ACC_VOLATILE; }
        if ((n & ACC_SYNCHRONIZED) != 0) { sb.append("synchronized "); n &= ~ACC_SYNCHRONIZED; }
        if ((n & ACC_NATIVE) != 0)       { sb.append("native ");       n &= ~ACC_NATIVE; }
        if ((n & ACC_STRICT) != 0)       { sb.append("strictfp ");     n &= ~ACC_STRICT; }
        if ((n & ACC_SYNTHETIC) != 0)    { sb.append("synthetic ");    n &= ~ACC_SYNTHETIC; }

        if ((n & ACC_ANNOTATION) != 0)   { sb.append("@");             n &= ~ACC_ANNOTATION; }
        if ((n & ACC_INTERFACE) != 0)    { sb.append("interface ");    n &= ~ACC_INTERFACE; }
        if ((n & ACC_ENUM) != 0)         { sb.append("enum ");         n &= ~ACC_ENUM; }

        if (n != 0) sb.append("+ " + n + " ");
        return sb.toString();
    }

    String beautify(String s) {
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
