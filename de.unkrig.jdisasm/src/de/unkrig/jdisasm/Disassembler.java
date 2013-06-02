
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

import de.unkrig.commons.nullanalysis.NotNullByDefault;
import de.unkrig.commons.nullanalysis.Nullable;
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
import de.unkrig.jdisasm.ConstantPool.ConstantNameAndTypeInfo;
import de.unkrig.jdisasm.ConstantPool.ConstantPoolEntry;
import de.unkrig.jdisasm.SignatureParser.ArrayTypeSignature;
import de.unkrig.jdisasm.SignatureParser.ClassSignature;
import de.unkrig.jdisasm.SignatureParser.ClassTypeSignature;
import de.unkrig.jdisasm.SignatureParser.FieldTypeSignature;
import de.unkrig.jdisasm.SignatureParser.FormalTypeParameter;
import de.unkrig.jdisasm.SignatureParser.MethodTypeSignature;
import de.unkrig.jdisasm.SignatureParser.SignatureException;
import de.unkrig.jdisasm.SignatureParser.ThrowsSignature;
import de.unkrig.jdisasm.SignatureParser.TypeSignature;

/**
 * A Java bytecode disassembler, comparable to JAVAP, which is part of ORACLE's JDK.
 * <p>
 * Notice that this tool does not depend on any other classes or libraries (other than the standard JDK library).
 * <p>
 * The disassembly is optimized to produce minimal DIFFs for changed class files: E.g. code offsets and local
 * variable indexes are only printed if really necessary.
 */
public
class Disassembler {

    // CHECKSTYLE LineLengthCheck:OFF
    private static final List<ConstantClassInfo>   NO_CONSTANT_CLASS_INFOS   = Collections.<ConstantClassInfo>emptyList();
    private static final List<ThrowsSignature>     NO_THROWS_SIGNATURES      = Collections.<ThrowsSignature>emptyList();
    private static final List<TypeSignature>       NO_TYPE_SIGNATURES        = Collections.<TypeSignature>emptyList();
    private static final List<ClassTypeSignature>  NO_CLASS_TYPE_SIGNATURES  = Collections.<ClassTypeSignature>emptyList();
    private static final List<FormalTypeParameter> NO_FORMAL_TYPE_PARAMETERS = Collections.<FormalTypeParameter>emptyList();
    private static final List<ParameterAnnotation> NO_PARAMETER_ANNOTATIONS  = Collections.<ParameterAnnotation>emptyList();
    // CHECKSTYLE LineLengthCheck:ON

    // Configuration variables.

    private PrintWriter pw      = new PrintWriter(System.out);
    boolean             verbose = false;

    /**
     * {@code null} means "do not attempt to find the source file".
     */
    @Nullable private File sourceDirectory = null;
    private boolean        hideLines;
    private boolean        hideVars;
    private boolean        symbolicLabels;

    /**
     * "" for the default package; with a trailing period otherwise.
     */
    @Nullable private String thisClassPackageName;

    @Nullable private Map<Integer /*offset*/, String /*label*/> branchTargets;
    
    private static enum AttributeContext { CLASS, FIELD, METHOD }

    public static void
    main(String[] args) throws IOException {
        Disassembler d = new Disassembler();
        int          i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-' || arg.length() == 1) break;
            if ("-o".equals(arg)) {
                d.setOut(new FileOutputStream(args[++i]));
            } else
            if ("-verbose".equals(arg)) {
                d.setVerbose(true);
            } else
            if ("-src".equals(arg)) {
                d.setSourceDirectory(new File(args[++i]));
            } else
            if ("-hide-lines".equals(arg)) {
                d.setHideLines(true);
            } else
            if ("-hide-vars".equals(arg)) {
                d.setHideVars(true);
            } else
            if ("-symbolic-labels".equals(arg)) {
                d.setSymbolicLabels(true);
            } else
            if ("-help".equals(arg)) {

                // CHECKSTYLE LineLengthCheck:OFF
                System.out.println("Prints a disassembly listing of the given JAVA[TM] class files (or STDIN) to ");
                System.out.println("STDOUT.");
                System.out.println("Usage:");
                System.out.println("  java " + Disassembler.class.getName() + " [ <option> ] ... [ <class-file-name> | <class-file-url> | '-' ] ...");
                System.out.println("Valid options are:");
                System.out.println("  -o <output-file>   Store disassembly output in a file.");
                System.out.println("  -verbose");
                System.out.println("  -src <source-dir>  Interweave the output with the class file's source code.");
                System.out.println("  -hide-lines        Don't print the line numbers.");
                System.out.println("  -hide-vars         Don't print the local variable names.");
                System.out.println("  -symbolic-labels   Use symbolic labels instead of offsets.");
                // CHECKSTYLE LineLengthCheck:ON

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
    private static final Pattern IS_URL = Pattern.compile("\\w\\w+:.*");

    public Disassembler() {}

    public void
    setOut(Writer writer) {
        this.pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
    }

    public void
    setOut(OutputStream stream) {
        this.pw = new PrintWriter(stream);
    }

    public void
    setOut(OutputStream stream, String charsetName) throws UnsupportedEncodingException {
        this.pw = new PrintWriter(new OutputStreamWriter(stream, charsetName));
    }

    public void
    setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Where to look for source files; {@code null} disables source file loading. Source file loading is disabled by
     * default.
     */
    public void
    setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public void
    setHideLines(boolean hideLines) {
        this.hideLines = hideLines;
    }
    public void
    setHideVars(boolean hideVars) {
        this.hideVars = hideVars;
    }

    public void
    setSymbolicLabels(boolean symbolicLabels) {
        this.symbolicLabels = symbolicLabels;
    }

    void print(String s)                       { this.pw.print(s); }
    void println()                             { this.pw.println(); }
    void println(String s)                     { this.pw.println(s); }
    void printf(String format, Object... args) { this.pw.printf(format, args); }

    /**
     * Disassemble one Java&trade; class file to {@link System#out}.
     */
    public void
    disasm(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            this.pw.println();
            this.pw.println("// *** Disassembly of '" + file + "'.");
            this.disasm(is);
        } catch (IOException ioe) {
            IOException ioe2 = new IOException("Disassembling '" + file + "': " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2; // SUPPRESS CHECKSTYLE AvoidLosing
        } catch (RuntimeException re) {
            throw new RuntimeException("Disassembling '" + file + "': " + re.getMessage(), re);
        } finally {
            try { is.close(); } catch (IOException ex) { }
        }
    }

    public void
    disasm(URL location) throws IOException {
        InputStream is = location.openConnection().getInputStream();
        try {
            this.pw.println();
            this.pw.println("// *** Disassembly of '" + location + "'.");
            this.disasm(is);
        } catch (IOException ioe) {
            IOException ioe2 = new IOException("Disassembling '" + location + "': " + ioe.getMessage());
            ioe2.initCause(ioe);
            throw ioe2; // SUPPRESS CHECKSTYLE AvoidLosing
        } catch (RuntimeException re) {
            throw new RuntimeException("Disassembling '" + location + "': " + re.getMessage(), re);
        } finally {
            try { is.close(); } catch (IOException ex) { }
        }
    }

    /**
     * @param is A Java&trade; class file
     */
    public void
    disasm(InputStream is) throws IOException {
        try {
            this.disassembleClassFile(new DataInputStream(is));
        } finally {
            this.pw.flush();
        }
    }

    /**
     * @param dis A Java&trade; class file
     */
    private void
    disassembleClassFile(DataInputStream dis) throws IOException {

        // Load the class file.
        ClassFile cf = new ClassFile(dis);

        // Print JDK version.
        this.println();
        this.println("// Class file version = " + cf.getJdkName());

        String tcpn = (
            this.thisClassPackageName = cf.thisClassName.substring(0, cf.thisClassName.lastIndexOf('.') + 1)
        );

        // Print package declaration.
        if (tcpn.length() > 0) {
            this.println();
            this.println("package " + tcpn.substring(0, tcpn.length() - 1) + ";");
        }

        // Print enclosing method info.
        EnclosingMethodAttribute ema = cf.enclosingMethodAttribute;
        if (ema != null) {
            ConstantNameAndTypeInfo m          = ema.method;
            String                  methodName = m == null ? "[initializer]" : m.name.bytes;
            String                  className  = ema.clasS.name;
            this.println();
            this.println(
                "// This class is enclosed by method '"
                + beautify(className)
                + ("<init>".equals(methodName) ? "(...)" : "." + methodName + "(...)")
                + "'."
            );
        }

        this.println();

        // Print SYNTHETIC notice.
        if ((cf.accessFlags & ACC_SYNTHETIC) != 0 || cf.syntheticAttribute != null) {
            this.println("// This is a synthetic class.");
        }

        // Print DEPRECATED notice.
        if (cf.deprecatedAttribute != null) this.println("/** @deprecated */");

        // Print type annotations.
        {
            RuntimeInvisibleAnnotationsAttribute riaa = cf.runtimeInvisibleAnnotationsAttribute;
            if (riaa != null) {
                for (Annotation a : riaa.annotations) this.println(a.toString());
            }
        }
        {
            RuntimeVisibleAnnotationsAttribute rvaa = cf.runtimeVisibleAnnotationsAttribute;
            if (rvaa != null) {
                for (Annotation a : rvaa.annotations) this.println(a.toString());
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
        {
            SignatureAttribute sa = cf.signatureAttribute;
            if (sa != null) {
                this.print(beautify(decodeClassSignature(sa.signature).toString(cf.thisClassName)));
            } else {
                this.print(beautify(cf.thisClassName));
                {
                    String scn = cf.superClassName;
                    if (scn != null && !"java.lang.Object".equals(scn)) this.print(" extends " + beautify(scn));
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
        }

        this.println(" {");

        // Dump the constant pool.
        if (this.verbose) {
            this.println();
            this.println("    // Constant pool dump:");
            ConstantPool cp = cf.constantPool;
            for (int i = 0; i < cp.getSize(); i++) {
                ConstantPoolEntry constantPoolEntry = cp.getOptional((short) i, ConstantPoolEntry.class);
                if (constantPoolEntry == null) continue;
                this.println("    //   #" + i + ": " + beautify(constantPoolEntry.toString()));
            }
        }

        // Print enclosing/enclosed types.
        {
            InnerClassesAttribute ica = cf.innerClassesAttribute;
            if (ica != null) {
                this.println();
                this.println("    // Enclosing/enclosed types:");
                for (InnerClassesAttribute.ClasS c : ica.classes) {
                    this.println("    //   " + toString(c));
                }
            }
        }

        // Print fields.
        disassembleFields(cf.fields);

        // Read source file.
        Map<Integer, String> sourceLines = new HashMap<Integer, String>();
        READ_SOURCE_LINES:
        if (this.sourceDirectory != null) {
            SourceFileAttribute sfa = cf.sourceFileAttribute;
            if (sfa == null) break READ_SOURCE_LINES;

            File sourceFile = new File(this.sourceDirectory, sfa.sourceFile);

            if (!sourceFile.exists()) {
                String toplevelClassName;
                {
                    toplevelClassName = cf.thisClassName;
                    int idx = toplevelClassName.indexOf('$');
                    if (idx != -1) toplevelClassName = toplevelClassName.substring(0, idx);
                }
                sourceFile = new File(
                    this.sourceDirectory,
                    toplevelClassName.replace('.', File.separatorChar) + ".java"
                );
            }
            if (!sourceFile.exists()) break READ_SOURCE_LINES;

            LineNumberReader lnr = new LineNumberReader(new FileReader(sourceFile));
            try {
                for (;;) {
                    String sl = lnr.readLine();
                    if (sl == null) break;
                    sourceLines.put(lnr.getLineNumber(), sl);
                }
            } finally {
                try { lnr.close(); } catch (Exception e) {}
            }
        }

        // Methods.
        for (Method m : cf.methods) {
            disassembleMethod(m, cf, sourceLines);
        }

        this.println("}");

        // Print class attributes.
        printAttributes(cf.attributes, "// ", new Attribute[] {
            cf.deprecatedAttribute,
            ema,
            cf.innerClassesAttribute,
            cf.runtimeInvisibleAnnotationsAttribute,
            cf.runtimeVisibleAnnotationsAttribute,
            cf.signatureAttribute,
            cf.sourceFileAttribute,
            cf.syntheticAttribute,
        }, AttributeContext.CLASS);
    }

    /**
     * Disassemble one method.
     */
    private void
    disassembleMethod(Method method, ClassFile cf, Map<Integer, String> sourceLines) {
        try {

            // One blank line before each method declaration.
            this.println();

            // Print SYNTHETIC notice.
            if ((method.accessFlags & ACC_SYNTHETIC) != 0 || method.syntheticAttribute != null) {
                this.println("    // (Synthetic method)");
            }

            // Print BRIDGE notice.
            if ((method.accessFlags & ACC_BRIDGE) != 0) this.println("    // (Bridge method)");

            // Print DEPRECATED notice.
            if (method.deprecatedAttribute != null) this.println("    /** @deprecated */");

            // Print method annotations.
            {
                RuntimeInvisibleAnnotationsAttribute riaa = method.runtimeInvisibleAnnotationsAttribute;
                if (riaa != null) {
                    for (Annotation a : riaa.annotations) this.println("    " + a.toString());
                }
            }
            {
                RuntimeVisibleAnnotationsAttribute rvaa = method.runtimeVisibleAnnotationsAttribute;
                if (rvaa != null) {
                    for (Annotation a : rvaa.annotations) this.println("    " + a.toString());
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
                SignatureAttribute sa = method.signatureAttribute;
                mts = sa == null ? decodeMethodDescriptor(method.descriptor) : decodeMethodTypeSignature(sa.signature);
                if (!mts.formalTypeParameters.isEmpty()) {
                    Iterator<FormalTypeParameter> it = mts.formalTypeParameters.iterator();
                    this.print("<" + beautify(it.next().toString()));
                    while (it.hasNext()) this.print(", " + beautify(it.next().toString()));
                    this.print(">");
                }
            }

            // Print method name.
            List<ConstantClassInfo> exceptionNames;
            {
                ExceptionsAttribute ea = method.exceptionsAttribute;
                exceptionNames = ea == null ? NO_CONSTANT_CLASS_INFOS : ea.exceptionNames;
            }
            if (
                "<clinit>".equals(functionName)
                && (method.accessFlags & ACC_STATIC) != 0
                && exceptionNames.isEmpty()
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
                && mts.formalTypeParameters.isEmpty()
                && mts.returnType == SignatureParser.VOID
            ) {
                this.print(beautify(cf.thisClassName));
                printParameters(
                    method.runtimeInvisibleParameterAnnotationsAttribute,
                    method.runtimeVisibleParameterAnnotationsAttribute,
                    mts.parameterTypes,
                    method,
                    (short) 1,
                    (method.accessFlags & ACC_VARARGS) != 0
                );
            } else
            {
                this.print(beautify(mts.returnType.toString()) + ' ');
                this.print(functionName);
                printParameters(
                    method.runtimeInvisibleParameterAnnotationsAttribute,
                    method.runtimeVisibleParameterAnnotationsAttribute,
                    mts.parameterTypes,
                    method,
                    (method.accessFlags & ACC_STATIC) == 0 ? (short) 1 : (short) 0,
                    (method.accessFlags & ACC_VARARGS) != 0
                );
            }

            // Thrown types.
            if (!mts.thrownTypes.isEmpty()) {
                Iterator<ThrowsSignature> it = mts.thrownTypes.iterator();
                this.print(" throws " + beautify(it.next().toString()));
                while (it.hasNext()) this.print(", " + beautify(it.next().toString()));
            } else
            if (!exceptionNames.isEmpty()) {
                Iterator<ConstantClassInfo> it = exceptionNames.iterator();
                this.print(" throws " + beautify(it.next().name));
                while (it.hasNext()) this.print(", " + beautify(it.next().name));
            }

            // Annotation default.
            {
                AnnotationDefaultAttribute ada = method.annotationDefaultAttribute;
                if (ada != null) this.print("default " + ada.defaultValue);
            }

            // Code.
            {
                CodeAttribute ca = method.codeAttribute;
                if (ca == null) {
                    this.println(";");
                } else {
                    this.println(" {");
                    try {
                        disassembleBytecode(
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
                    this.println("    }");
                }
            }

            // Print method attributes.
            printAttributes(method.attributes, "    // ", new Attribute[] {
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
            }, AttributeContext.METHOD);
        } catch (RuntimeException rte) {
            throw new RuntimeException("Method '" + method.name + "' " + method.descriptor, rte);
        }
    }

    private void
    disassembleFields(List<Field> fields) {
        for (Field field : fields) {
            this.println();

            // Print field annotations.
            {
                RuntimeInvisibleAnnotationsAttribute riaa = field.runtimeInvisibleAnnotationsAttribute;
                if (riaa != null) {
                    for (Annotation a : riaa.annotations) this.println("    " + a.toString());
                }
            }
            {
                RuntimeVisibleAnnotationsAttribute rvaa = field.runtimeVisibleAnnotationsAttribute;
                if (rvaa != null) {
                    for (Annotation a : rvaa.annotations) this.println("    " + a.toString());
                }
            }

            // print SYNTHETIC notice.
            if ((field.accessFlags & ACC_SYNTHETIC) != 0 || field.syntheticAttribute != null) {
                this.println("    // (Synthetic field)");
            }

            // Print DEPRECATED notice.
            if (field.deprecatedAttribute != null) this.println("    /** @deprecated */");

            // Print field access flags and field type.
            String parametrizedType;
            {
                SignatureAttribute sa = field.signatureAttribute;
                parametrizedType = beautify(
                    sa == null
                    ? decodeFieldDescriptor(field.descriptor).toString()
                    : decodeFieldTypeSignature(sa.signature).toString()
                );
            }

            // Print field name and initializer.
            {
                String prefix = (
                    "    "
                    + decodeAccess((short) (field.accessFlags & ~ACC_SYNTHETIC))
                    + parametrizedType
                    + " "
                );
                ConstantValueAttribute cva = field.constantValueAttribute;
                if (cva == null) {
                    this.printf("%-40s %s;%n", prefix, field.name);
                } else {
                    this.printf("%-40s %-15s = %s;%n", prefix, field.name, cva.constantValue);
                }
            }

            // Print field attributes.
            printAttributes(field.attributes, "    // ", new Attribute[] {
                field.constantValueAttribute,
                field.deprecatedAttribute,
                field.runtimeInvisibleAnnotationsAttribute,
                field.runtimeVisibleAnnotationsAttribute,
                field.signatureAttribute,
                field.syntheticAttribute,
            }, AttributeContext.FIELD);
        }
    }

    String
    toString(InnerClassesAttribute.ClasS c) {
        ConstantClassInfo oci = c.outerClassInfo;
        ConstantClassInfo ici = c.innerClassInfo;
        return (
            (oci == null ? "[local class]" : beautify(oci.name))
            + " { "
            + decodeAccess((short) (c.innerClassAccessFlags & ( // Hide ABSTRACT and STATIC for interfaces
                (c.innerClassAccessFlags & ACC_INTERFACE) != 0
                ? (~ACC_ABSTRACT & ~ACC_STATIC)
                : 0xffff
            )))
            + ((c.innerClassAccessFlags & (ACC_ENUM | ACC_ANNOTATION | ACC_INTERFACE)) == 0 ? "class " : "")
            + beautify(ici.name) + " }"
        );
    }

    private void
    printAttributes(
        List<Attribute>  attributes,
        String           prefix,
        Attribute[]      excludedAttributes,
        AttributeContext context
    ) {
        List<Attribute> tmp = new ArrayList<Attribute>(attributes);

        // Strip excluded attributes.
        if (!this.verbose) {
            tmp.removeAll(Arrays.asList(excludedAttributes));
        }
        if (tmp.isEmpty()) return;

        Collections.sort(tmp, new Comparator<Attribute>() {

            @Override public int
            compare(Attribute a1, Attribute a2) { return a1.getName().compareTo(a2.getName()); }
        });

        this.println(prefix + (this.verbose ? "Attributes:" : "Unprocessed attributes:"));
        PrintAttributeVisitor visitor = new PrintAttributeVisitor(prefix + "  ", context);
        for (Attribute a : tmp) a.accept(visitor);
    }

    /**
     * Prints an {@link Attribute}.
     */
    public
    class PrintAttributeVisitor implements AttributeVisitor {

        private final String           prefix;
        private final AttributeContext context;

        public
        PrintAttributeVisitor(String prefix, AttributeContext context) {
            this.prefix  = prefix;
            this.context = context;
        }

        @Override public void
        visit(AnnotationDefaultAttribute ada) {
            Disassembler.this.println(this.prefix + "AnnotationDefault:");
            Disassembler.this.println(this.prefix + "  " + ada.defaultValue.toString());
        }

        @Override public void
        visit(CodeAttribute ca) {
            Disassembler.this.println(this.prefix + "Code:");
            Disassembler.this.println(this.prefix + "  max_locals = " + ca.maxLocals);
            Disassembler.this.println(this.prefix + "  max_stack = " + ca.maxStack);

            Disassembler.this.println(this.prefix + "  code = {");
            this.print(ca.code);
            Disassembler.this.println(this.prefix + "  }");

            if (!ca.attributes.isEmpty()) {
                Disassembler.this.println(this.prefix + "  attributes = {");
                PrintAttributeVisitor pav = new PrintAttributeVisitor(this.prefix + "    ", AttributeContext.METHOD);
                List<Attribute>       tmp = ca.attributes;
                Collections.sort(tmp, new Comparator<Attribute>() {

                    @Override public int
                    compare(Attribute a1, Attribute a2) { return a1.getName().compareTo(a2.getName()); }
                });
                for (Attribute a : tmp) {
                    a.accept(pav);
                }
                Disassembler.this.println(this.prefix + "  }");
            }
        }

        private void
        print(byte[] data) {
            for (int i = 0; i < data.length; i += 32) {
                Disassembler.this.print(this.prefix + "   ");
                for (int j = 0; j < 32; ++j) {
                    int idx = i + j;
                    if (idx >= data.length) break;
                    Disassembler.this.printf("%c%02x", j == 16 ? '-' : ' ', 0xff & data[idx]);
                }
                Disassembler.this.println();
            }
        }

        @Override public void
        visit(ConstantValueAttribute cva) {
            Disassembler.this.println(this.prefix + "ConstantValue:");
            Disassembler.this.println(this.prefix + "  constant_value = " + cva.constantValue);
        }

        @Override public void
        visit(DeprecatedAttribute da) {
            Disassembler.this.println(this.prefix + "DeprecatedAttribute:");
            Disassembler.this.println(this.prefix + "  -");
        }

        @Override public void
        visit(EnclosingMethodAttribute ema) {
            Disassembler.this.println(this.prefix + "EnclosingMethod:");
            ConstantNameAndTypeInfo m = ema.method;
            Disassembler.this.println(this.prefix + "  class/method = " + (
                m == null
                ? "(none)"
                : beautify(decodeMethodDescriptor(m.descriptor.bytes).toString(ema.clasS.name, m.name.bytes))
            ));
        }

        @Override public void
        visit(ExceptionsAttribute ea) {
            Disassembler.this.println(this.prefix + "Exceptions:");
            for (ConstantClassInfo en : ea.exceptionNames) {
                Disassembler.this.println(this.prefix + "  " + en.name);
            }
        }

        @Override public void
        visit(InnerClassesAttribute ica) {
            Disassembler.this.println(this.prefix + "InnerClasses:");
            for (InnerClassesAttribute.ClasS c : ica.classes) {
                Disassembler.this.println(this.prefix + "  " + Disassembler.this.toString(c));
            }
        }

        @Override public void
        visit(LineNumberTableAttribute lnta) {
            Disassembler.this.println(this.prefix + "LineNumberTable:");
            for (LineNumberTableEntry e : lnta.entries) {
                Disassembler.this.println(this.prefix + "  " + e.startPc + " => Line " + e.lineNumber);
            }
        }

        @Override public void
        visit(LocalVariableTableAttribute lvta) {
            Disassembler.this.println(this.prefix + "LocalVariableTable:");
            for (LocalVariableTableAttribute.Entry e : lvta.entries) {
                Disassembler.this.println(
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

        @Override public void
        visit(LocalVariableTypeTableAttribute lvtta) {
            Disassembler.this.println(this.prefix + "LocalVariableTypeTable:");
            for (LocalVariableTypeTableAttribute.Entry e : lvtta.entries) {
                Disassembler.this.println(
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

        @Override public void
        visit(RuntimeInvisibleAnnotationsAttribute riaa) {
            Disassembler.this.println(this.prefix + "RuntimeInvisibleAnnotations:");
            for (Annotation a : riaa.annotations) {
                Disassembler.this.println(this.prefix + "  " + a.toString());
            }
        }

        @Override public void
        visit(RuntimeVisibleAnnotationsAttribute rvaa) {
            Disassembler.this.println(this.prefix + "RuntimeVisibleAnnotations:");
            for (Annotation a : rvaa.annotations) {
                Disassembler.this.println(this.prefix + "  " + a.toString());
            }
        }

        @Override public void
        visit(RuntimeInvisibleParameterAnnotationsAttribute ripaa) {
            Disassembler.this.println(this.prefix + "RuntimeInvisibleParameterAnnotations:");
            for (ParameterAnnotation pa : ripaa.parameterAnnotations) {
                for (Annotation a : pa.annotations) {
                    Disassembler.this.println(this.prefix + "  " + a.toString());
                }
            }
        }

        @Override public void
        visit(RuntimeVisibleParameterAnnotationsAttribute rvpaa) {
            Disassembler.this.println(this.prefix + "RuntimeVisibleParameterAnnotations:");
            for (ParameterAnnotation pa : rvpaa.parameterAnnotations) {
                for (Annotation a : pa.annotations) {
                    Disassembler.this.println(this.prefix + "  " + a.toString());
                }
            }
        }

        @Override public void
        visit(SignatureAttribute sa) {
            Disassembler.this.println(this.prefix + "Signature:");
            switch (this.context) {
            case CLASS:
                Disassembler.this.println(
                    this.prefix
                    + "  "
                    + decodeClassSignature(sa.signature).toString("[this-class]")
                );
                break;
            case FIELD:
                Disassembler.this.println(this.prefix + "  " + decodeFieldTypeSignature(sa.signature).toString());
                break;
            case METHOD:
                Disassembler.this.println(
                    this.prefix
                    + "  "
                    + decodeMethodTypeSignature(sa.signature).toString("[declaring-class]", "[this-method]")
                );
                break;
            }
        }

        @Override public void
        visit(SourceFileAttribute sfa) {
            Disassembler.this.println(this.prefix + "SourceFile:");
            Disassembler.this.println(this.prefix + "  " + sfa.sourceFile);
        }

        @Override public void
        visit(SyntheticAttribute sa) {
            Disassembler.this.println(this.prefix + "Synthetic:");
            Disassembler.this.println(this.prefix + " -");
        }

        @Override public void
        visit(UnknownAttribute ua) {
            Disassembler.this.println(this.prefix + ua.name + ":");
            Disassembler.this.println(this.prefix + "  data = {");
            this.print(ua.info);
            Disassembler.this.println(this.prefix + "}");
        }
    }

    private void
    printParameters(
        @Nullable RuntimeInvisibleParameterAnnotationsAttribute ripaa,
        @Nullable RuntimeVisibleParameterAnnotationsAttribute   rvpaa,
        List<TypeSignature>                                     parameterTypes,
        Method                                                  method,
        short                                                   firstIndex,
        boolean                                                 varargs
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

        this.print("(");
        Iterator<TypeSignature> it = parameterTypes.iterator();
        if (it.hasNext()) {
            for (;;) {
                TypeSignature pts = it.next();

                // Parameter annotations.
                if (ipas.hasNext()) for (Annotation a : ipas.next().annotations) this.print(a.toString() + ' ');
                if (vpas.hasNext()) for (Annotation a : vpas.next().annotations) this.print(a.toString() + ' ');

                // Parameter type.
                if (varargs && !it.hasNext() && pts instanceof ArrayTypeSignature) {
                    this.print(beautify(((ArrayTypeSignature) pts).typeSignature.toString()) + "...");
                } else {
                    this.print(beautify(pts.toString()));
                }

                // Parameter name.
                this.print(' ' + getLocalVariable(firstIndex, 0, method).name);

                if (!it.hasNext()) break;
                firstIndex++;
                this.print(", ");
            }
        }
        this.print(")");
    }

    /**
     * Read byte code from the given {@link InputStream} and disassemble it.
     */
    private void
    disassembleBytecode(
        InputStream                        is,
        List<ExceptionTableEntry>          exceptionTable,
        @Nullable LineNumberTableAttribute lineNumberTableAttribute,
        Map<Integer, String>               sourceLines,
        ConstantPool                       cp,
        Method                             method
    ) throws IOException {
        CountingInputStream cis = new CountingInputStream(is);
        DataInputStream     dis = new DataInputStream(cis);

        this.branchTargets = new HashMap<Integer, String>();
        try {

            // Analyze TRY bodies.

            // startPC => { endPC }
            SortedMap<Integer, Set<Integer>> tryStarts = new TreeMap<Integer, Set<Integer>>();

            // endPC => startPC => [ ExceptionTableEntry ]
            SortedMap<Integer, SortedMap<Integer, List<ExceptionTableEntry>>> tryEnds = (
                new TreeMap<Integer, SortedMap<Integer, List<ExceptionTableEntry>>>()
            );

            for (ExceptionTableEntry e : exceptionTable) {

                // Register the entry in "tryStarts".
                {
                    Set<Integer> s = tryStarts.get(e.startPc);
                    if (s == null) {
                        s = new HashSet<Integer>();
                        tryStarts.put(e.startPc, s);
                    }
                    s.add(e.endPc);
                }

                // Register the entry in "tryEnds".
                {
                    SortedMap<Integer, List<ExceptionTableEntry>> m = tryEnds.get(e.endPc);
                    if (m == null) {
                        m = new TreeMap<Integer, List<ExceptionTableEntry>>(Collections.reverseOrder());
                        tryEnds.put(e.endPc, m);
                    }
                    List<ExceptionTableEntry> l = m.get(e.startPc);
                    if (l == null) {
                        l = new ArrayList<ExceptionTableEntry>();
                        m.put(e.startPc, l);
                    }
                    l.add(e);
                }
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
                        lines.put(instructionOffset, instruction.getMnemonic() + disassembleOperands(
                            instruction.getOperands(),
                            dis,
                            instructionOffset,
                            method,
                            cp
                        ));
                    } catch (RuntimeException rte) {
                        for (Iterator<Entry<Integer, String>> it = lines.entrySet().iterator(); it.hasNext();) {
                            Entry<Integer, String> e = it.next();
                            this.println("#" + e.getKey() + " " + e.getValue());
                        }
                        throw new RuntimeException("Instruction '" + instruction + "', pc=" + instructionOffset, rte);
                    }
                }
            }

            // Format and print the disassembly lines.
            String indentation = "        ";
            for (Entry<Integer, String> e : lines.entrySet()) {
                int    instructionOffset = e.getKey();
                String text              = e.getValue();

                // Print ends of TRY bodies.
                for (Iterator<Entry<Integer, SortedMap<Integer, List<ExceptionTableEntry>>>> it = (
                    tryEnds.entrySet().iterator()
                ); it.hasNext();) {
                    Entry<Integer, SortedMap<Integer, List<ExceptionTableEntry>>> e2    = it.next();
                    int                                                           endPc = e2.getKey().intValue();
                    if (endPc > instructionOffset) break;

                    SortedMap<Integer, List<ExceptionTableEntry>> startPc2Ete = e2.getValue();
                    for (Entry<Integer, List<ExceptionTableEntry>> e3 : startPc2Ete.entrySet()) {
                        List<ExceptionTableEntry> etes = e3.getValue();
                        if (endPc < instructionOffset) {
                            error(
                                "Exception table entry ends at invalid code array index "
                                + endPc
                                + " (current instruction offset is "
                                + instructionOffset
                                + ")"
                            );
                        }
                        indentation = indentation.substring(4);
                        this.print(indentation + "} catch (");
                        for (Iterator<ExceptionTableEntry> it2 = etes.iterator();;) {
                            ExceptionTableEntry ete = it2.next();
                            ConstantClassInfo   ct  = ete.catchType;
                            this.print(
                                (ct == null ? "[all exceptions]" : beautify(ct.name))
                                + " => "
                                + branchTarget(ete.handlerPc)
                            );
                            if (!it2.hasNext()) break;
                            this.print(", ");
                        }
                        this.println(")");
                    }
                    it.remove();
                }

                // Print instruction offsets only for branch targets.
                {
                    String label = this.branchTargets.get(instructionOffset);
                    if (label != null) this.println(label);
                }

                // Print beginnings of TRY bodies.
                for (Iterator<Entry<Integer, Set<Integer>>> it = tryStarts.entrySet().iterator(); it.hasNext();) {
                    Entry<Integer, Set<Integer>> sc      = it.next();
                    Integer                      startPc = sc.getKey();
                    if (startPc > instructionOffset) break;

                    for (int i = sc.getValue().size(); i > 0; i--) {
                        if (startPc < instructionOffset) {
                            error(
                                "Exception table entry starts at invalid code array index "
                                + startPc
                                + " (current instruction offset is "
                                + instructionOffset
                                + ")"
                            );
                        }
                        this.println(indentation + "try {");
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
                    this.println(sb.toString());
                }

                this.println(indentation + text);
            }
        } finally {
            this.branchTargets = null;
        }
    }

    @Nullable private String
    branchTarget(int offset) {
        Map<Integer, String> bts = this.branchTargets;
        assert bts != null;
        String label = bts.get(offset);
        if (label == null) {
            label = this.symbolicLabels ? "L" + (1 + bts.size()) : "#" + offset;
            bts.put(offset, label);
        }
        return label;
    }

    /**
     * @return -1 iff the offset is not associated with a line number
     */
    private static int
    findLineNumber(LineNumberTableAttribute lnta, int offset) {
        for (LineNumberTableEntry lnte : lnta.entries) {
            if (lnte.startPc == offset) return lnte.lineNumber;
        }
        return -1;
    }

    /**
     * @return The {@code operands} converted into one line of text, prefixed and separated by one space.
     */
    String
    disassembleOperands(
        Operand[]       operands,
        DataInputStream dis,
        int             instructionOffset,
        Method          method,
        ConstantPool    cp
    ) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < operands.length; ++i) {
            sb.append(operands[i].disassemble(
                dis,
                instructionOffset,
                method,
                cp,
                this
            ));
        }
        return sb.toString();
    }

    private static final Instruction[] OPCODE_TO_INSTRUCTION = compileInstructions(
        "50  aaload\n"
        + "83  aastore\n"
        + "1   aconst_null\n"
        + "25  aload           localvariableindex1\n"
        + "42  aload_0         implicitlocalvariableindex\n"
        + "43  aload_1         implicitlocalvariableindex\n"
        + "44  aload_2         implicitlocalvariableindex\n"
        + "45  aload_3         implicitlocalvariableindex\n"
        + "189 anewarray       class2\n"
        + "176 areturn\n"
        + "190 arraylength\n"
        + "58  astore          localvariableindex1\n"
        + "75  astore_0        implicitlocalvariableindex\n"
        + "76  astore_1        implicitlocalvariableindex\n"
        + "77  astore_2        implicitlocalvariableindex\n"
        + "78  astore_3        implicitlocalvariableindex\n"
        + "191 athrow\n"
        + "51  baload\n"
        + "84  bastore\n"
        + "16  bipush          signedbyte\n"
        + "52  caload\n"
        + "85  castore\n"
        + "192 checkcast       class2\n"
        + "144 d2f\n"
        + "142 d2i\n"
        + "143 d2l\n"
        + "99  dadd\n"
        + "49  daload\n"
        + "82  dastore\n"
        + "152 dcmpg\n"
        + "151 dcmpl\n"
        + "14  dconst_0\n"
        + "15  dconst_1\n"
        + "111 ddiv\n"
        + "24  dload           localvariableindex1\n"
        + "38  dload_0         implicitlocalvariableindex\n"
        + "39  dload_1         implicitlocalvariableindex\n"
        + "40  dload_2         implicitlocalvariableindex\n"
        + "41  dload_3         implicitlocalvariableindex\n"
        + "107 dmul\n"
        + "119 dneg\n"
        + "115 drem\n"
        + "175 dreturn\n"
        + "57  dstore          localvariableindex1\n"
        + "71  dstore_0        implicitlocalvariableindex\n"
        + "72  dstore_1        implicitlocalvariableindex\n"
        + "73  dstore_2        implicitlocalvariableindex\n"
        + "74  dstore_3        implicitlocalvariableindex\n"
        + "103 dsub\n"
        + "89  dup\n"
        + "90  dup_x1\n"
        + "91  dup_x2\n"
        + "92  dup2\n"
        + "93  dup2_x1\n"
        + "94  dup2_x2\n"
        + "141 f2d\n"
        + "139 f2i\n"
        + "140 f2l\n"
        + "98  fadd\n"
        + "48  faload\n"
        + "81  fastore\n"
        + "150 fcmpg\n"
        + "149 fcmpl\n"
        + "11  fconst_0\n"
        + "12  fconst_1\n"
        + "13  fconst_2\n"
        + "110 fdiv\n"
        + "23  fload           localvariableindex1\n"
        + "34  fload_0         implicitlocalvariableindex\n"
        + "35  fload_1         implicitlocalvariableindex\n"
        + "36  fload_2         implicitlocalvariableindex\n"
        + "37  fload_3         implicitlocalvariableindex\n"
        + "106 fmul\n"
        + "118 fneg\n"
        + "114 frem\n"
        + "174 freturn\n"
        + "56  fstore          localvariableindex1\n"
        + "67  fstore_0        implicitlocalvariableindex\n"
        + "68  fstore_1        implicitlocalvariableindex\n"
        + "69  fstore_2        implicitlocalvariableindex\n"
        + "70  fstore_3        implicitlocalvariableindex\n"
        + "102 fsub\n"
        + "180 getfield        fieldref2\n"
        + "178 getstatic       fieldref2\n"
        + "167 goto            branchoffset2\n"
        + "200 goto_w          branchoffset4\n"
        + "145 i2b\n"
        + "146 i2c\n"
        + "135 i2d\n"
        + "134 i2f\n"
        + "133 i2l\n"
        + "147 i2s\n"
        + "96  iadd\n"
        + "46  iaload\n"
        + "126 iand\n"
        + "79  iastore\n"
        + "2   iconst_m1\n"
        + "3   iconst_0\n"
        + "4   iconst_1\n"
        + "5   iconst_2\n"
        + "6   iconst_3\n"
        + "7   iconst_4\n"
        + "8   iconst_5\n"
        + "108 idiv\n"
        + "165 if_acmpeq       branchoffset2\n"
        + "166 if_acmpne       branchoffset2\n"
        + "159 if_icmpeq       branchoffset2\n"
        + "160 if_icmpne       branchoffset2\n"
        + "161 if_icmplt       branchoffset2\n"
        + "162 if_icmpge       branchoffset2\n"
        + "163 if_icmpgt       branchoffset2\n"
        + "164 if_icmple       branchoffset2\n"
        + "153 ifeq            branchoffset2\n"
        + "154 ifne            branchoffset2\n"
        + "155 iflt            branchoffset2\n"
        + "156 ifge            branchoffset2\n"
        + "157 ifgt            branchoffset2\n"
        + "158 ifle            branchoffset2\n"
        + "199 ifnonnull       branchoffset2\n"
        + "198 ifnull          branchoffset2\n"
        + "132 iinc            localvariableindex1 signedbyte\n"
        + "21  iload           localvariableindex1\n"
        + "26  iload_0         implicitlocalvariableindex\n"
        + "27  iload_1         implicitlocalvariableindex\n"
        + "28  iload_2         implicitlocalvariableindex\n"
        + "29  iload_3         implicitlocalvariableindex\n"
        + "104 imul\n"
        + "116 ineg\n"
        + "193 instanceof      class2\n"
        // For Java 7; see
        // http://cr.openjdk.java.net/~jrose/pres/indy-javadoc-mlvm/java/lang/invoke/package-summary.html
//        + "186 invokedynamic   invokedynamic2\n"
        + "185 invokeinterface interfacemethodref2\n"
        + "183 invokespecial   methodref2\n"
        + "184 invokestatic    methodref2\n"
        + "182 invokevirtual   methodref2\n"
        + "128 ior\n"
        + "112 irem\n"
        + "172 ireturn\n"
        + "120 ishl\n"
        + "122 ishr\n"
        + "54  istore          localvariableindex1\n"
        + "59  istore_0        implicitlocalvariableindex\n"
        + "60  istore_1        implicitlocalvariableindex\n"
        + "61  istore_2        implicitlocalvariableindex\n"
        + "62  istore_3        implicitlocalvariableindex\n"
        + "100 isub\n"
        + "124 iushr\n"
        + "130 ixor\n"
        + "168 jsr             branchoffset2\n"
        + "201 jsr_w           branchoffset4\n"
        + "138 l2d\n"
        + "137 l2f\n"
        + "136 l2i\n"
        + "97  ladd\n"
        + "47  laload\n"
        + "127 land\n"
        + "80  lastore\n"
        + "148 lcmp\n"
        + "9   lconst_0\n"
        + "10  lconst_1\n"
        + "18  ldc             intfloatclassstring1\n"
        + "19  ldc_w           intfloatclassstring2\n"
        + "20  ldc2_w          longdouble2\n"
        + "109 ldiv\n"
        + "22  lload           localvariableindex1\n"
        + "30  lload_0         implicitlocalvariableindex\n"
        + "31  lload_1         implicitlocalvariableindex\n"
        + "32  lload_2         implicitlocalvariableindex\n"
        + "33  lload_3         implicitlocalvariableindex\n"
        + "105 lmul\n"
        + "117 lneg\n"
        + "171 lookupswitch    lookupswitch\n"
        + "129 lor\n"
        + "113 lrem\n"
        + "173 lreturn\n"
        + "121 lshl\n"
        + "123 lshr\n"
        + "55  lstore          localvariableindex1\n"
        + "63  lstore_0        implicitlocalvariableindex\n"
        + "64  lstore_1        implicitlocalvariableindex\n"
        + "65  lstore_2        implicitlocalvariableindex\n"
        + "66  lstore_3        implicitlocalvariableindex\n"
        + "101 lsub\n"
        + "125 lushr\n"
        + "131 lxor\n"
        + "194 monitorenter\n"
        + "195 monitorexit\n"
        + "197 multianewarray  class2 unsignedbyte\n"
        + "187 new             class2\n"
        + "188 newarray        atype\n"
        + "0   nop\n"
        + "87  pop\n"
        + "88  pop2\n"
        + "181 putfield        fieldref2\n"
        + "179 putstatic       fieldref2\n"
        + "169 ret             localvariableindex1\n"
        + "177 return\n"
        + "53  saload\n"
        + "86  sastore\n"
        + "17  sipush          signedshort\n"
        + "95  swap\n"
        + "170 tableswitch     tableswitch\n"
        + "196 wide            wide\n"
    );

    static final Instruction[] OPCODE_TO_WIDE_INSTRUCTION = compileInstructions(
        "21  iload           localvariableindex2\n"
        + "23  fload           localvariableindex2\n"
        + "25  aload           localvariableindex2\n"
        + "22  lload           localvariableindex2\n"
        + "24  dload           localvariableindex2\n"
        + "54  istore          localvariableindex2\n"
        + "56  fstore          localvariableindex2\n"
        + "58  astore          localvariableindex2\n"
        + "55  lstore          localvariableindex2\n"
        + "57  dstore          localvariableindex2\n"
        + "169 ret             localvariableindex2\n"
        + "132 iinc            localvariableindex2 signedshort\n"
    );

    private static Instruction[]
    compileInstructions(String instructions) {
        Instruction[] result = new Instruction[256];

        for (StringTokenizer st1 = new StringTokenizer(instructions, "\n"); st1.hasMoreTokens();) {
            StringTokenizer st2      = new StringTokenizer(st1.nextToken());
            String          os       = st2.nextToken();
            int             opcode   = Integer.parseInt(os);
            String          mnemonic = st2.nextToken();
            Operand[]       operands;
            if (!st2.hasMoreTokens()) {
                operands = new Operand[0];
            } else {
                List<Operand> l = new ArrayList<Operand>();
                while (st2.hasMoreTokens()) {
                    String  s = st2.nextToken();
                    Operand operand;
                    if ("intfloatclassstring1".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short  index = (short) (0xff & dis.readByte());
                                String t     = cp.getIntegerFloatClassString(index);
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("intfloatclassstring2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short  index = dis.readShort();
                                String t     = cp.getIntegerFloatClassString(index);
                                if (Character.isJavaIdentifierStart(t.charAt(0))) t = d.beautify(t);
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("longdouble2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short  index = dis.readShort();
                                String t     = cp.getLongDoubleString(index);
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("fieldref2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short                index = dis.readShort();
                                ConstantFieldrefInfo fr    = cp.get(index, ConstantFieldrefInfo.class);
                                String               t     = (
                                    d.beautify(d.decodeFieldDescriptor(fr.nameAndType.descriptor.bytes).toString())
                                    + ' '
                                    + d.beautify(fr.clasS.name)
                                    + '.'
                                    + fr.nameAndType.name.bytes
                                );
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("methodref2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short                 index = dis.readShort();
                                ConstantMethodrefInfo mr    = cp.get(index, ConstantMethodrefInfo.class);
                                String                t     = d.beautify(
                                    d.decodeMethodDescriptor(mr.nameAndType.descriptor.bytes).toString(
                                        mr.clasS.name,
                                        mr.nameAndType.name.bytes
                                    )
                                );
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("interfacemethodref2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short                          index = dis.readShort();
                                ConstantInterfaceMethodrefInfo imr   = cp.get(
                                    index,
                                    ConstantInterfaceMethodrefInfo.class
                                );
                                dis.readByte();
                                dis.readByte();
                                String t = d.beautify(
                                    d.decodeMethodDescriptor(imr.nameAndType.descriptor.bytes).toString(
                                        imr.clasS.name,
                                        imr.nameAndType.name.bytes
                                    )
                                );
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("class2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                short  index = dis.readShort();
                                String name  = cp.get(index, ConstantClassInfo.class).name;
                                String t     = d.beautify(
                                    name.startsWith("[")
                                    ? d.decodeFieldDescriptor(name).toString()
                                    : name.replace('/', '.')
                                );
                                if (d.verbose) t += " (" + (0xffff & index) + ")";
                                return ' ' + t;
                            }
                        };
                    } else
                    if ("localvariableindex1".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("localvariableindex2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("implicitlocalvariableindex".equals(s)) {
                        // Strip the lv index from the mnemonic
                        final short index = Short.parseShort(mnemonic.substring(mnemonic.length() - 1));
                        mnemonic = mnemonic.substring(0, mnemonic.length() - 2);
                        operand  = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) {

                                // For an initial assignment (e.g. 'istore_3'), the local variable is only visible
                                // AFTER this instruction.
                                LocalVariable lv = d.getLocalVariable(index, instructionOffset + 1, method);
                                return d.beautify(lv.toString());
                            }
                        };
                    } else
                    if ("branchoffset2".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return " " + d.branchTarget(instructionOffset + dis.readShort());
                            }
                        };
                    } else
                    if ("branchoffset4".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                return " " + d.branchTarget(instructionOffset + dis.readInt());
                            }
                        };
                    } else
                    if ("signedbyte".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("unsignedbyte".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("atype".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("signedshort".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                    if ("tableswitch".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                                sb.append(d.branchTarget(instructionOffset + dis.readInt()));

                                int low  = dis.readInt();
                                int high = dis.readInt();
                                for (int i = low; i <= high; ++i) {
                                    sb.append(", ").append(i).append(" => ");
                                    sb.append(d.branchTarget(instructionOffset + dis.readInt()));
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if ("lookupswitch".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
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
                                sb.append(d.branchTarget(instructionOffset + dis.readInt()));

                                int npairs = dis.readInt();
                                for (int i = 0; i < npairs; ++i) {
                                    int match  = dis.readInt();
                                    int offset = instructionOffset + dis.readInt();
                                    sb.append(", ").append(match).append(" => ").append(d.branchTarget(offset));
                                }
                                return sb.toString();
                            }
                        };
                    } else
                    if ("wide".equals(s)) {
                        operand = new Operand() {

                            @Override public String
                            disassemble(
                                DataInputStream dis,
                                int             instructionOffset,
                                Method          method,
                                ConstantPool    cp,
                                Disassembler    d
                            ) throws IOException {
                                int         subopcode       = 0xff & dis.readByte();
                                Instruction wideInstruction = OPCODE_TO_WIDE_INSTRUCTION[subopcode];
                                if (wideInstruction == null) {
                                    return (
                                        "Invalid opcode "
                                        + subopcode
                                        + " after opcode WIDE"
                                    );
                                }
                                return wideInstruction.getMnemonic() + d.disassembleOperands(
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

            // Pad the mnemonic to 15 characters so the first operands are vertically aligned.
            mnemonic += "               ".substring(mnemonic.length());

            result[opcode] = new Instruction(mnemonic, operands);
        }
        return result;
    }

    LocalVariable
    getLocalVariable(short localVariableIndex, int  instructionOffset, Method method) {

        // Calculate index of first parameter.
        int firstParameter = (method.accessFlags & ACC_STATIC) == 0 ? 1 : 0;
        if (localVariableIndex < firstParameter) {
            return new LocalVariable(null, "this");
        }

        List<TypeSignature> parameterTypes;
        {
            SignatureAttribute  sa  = method.signatureAttribute;
            MethodTypeSignature mts = (
                sa != null
                ? decodeMethodTypeSignature(sa.signature)
                : decodeMethodDescriptor(method.descriptor)
            );
            parameterTypes = mts.parameterTypes;
        }

        // Calculate index of first local variable.
        int firstLocalVariable = firstParameter + parameterTypes.size();

        String defaultName = (
            localVariableIndex < firstLocalVariable
            ? "p" + (1 + localVariableIndex - firstParameter)
            : "v" + (1 + localVariableIndex - firstLocalVariable)
        );

        CodeAttribute ca = method.codeAttribute;
        if (ca != null && (localVariableIndex >= firstLocalVariable || !this.hideVars)) {
            LocalVariableTypeTableAttribute lvtta = ca.localVariableTypeTableAttribute;
            if (lvtta != null) {
                for (LocalVariableTypeTableAttribute.Entry lvtte : lvtta.entries) {
                    if (
                        instructionOffset >= lvtte.startPC
                        && instructionOffset <= lvtte.startPC + lvtte.length
                        && localVariableIndex == lvtte.index
                    ) {
                        return new LocalVariable(
                            decodeFieldTypeSignature(lvtte.signature),
                            this.hideVars ? defaultName : lvtte.name
                        );
                    }
                }
            }

            LocalVariableTableAttribute lvta = ca.localVariableTableAttribute;
            if (lvta != null) {
                for (LocalVariableTableAttribute.Entry lvte : lvta.entries) {
                    if (
                        instructionOffset >= lvte.startPC
                        && instructionOffset <= lvte.startPC + lvte.length
                        && localVariableIndex == lvte.index
                    ) {
                        return new LocalVariable(
                            decodeFieldDescriptor(lvte.descriptor),
                            this.hideVars ? defaultName : lvte.name
                        );
                    }
                }
            }
        }

        if (localVariableIndex < firstLocalVariable) {
            return new LocalVariable(parameterTypes.get(localVariableIndex - firstParameter), defaultName);
        } else {
            return new LocalVariable(null, defaultName);
        }
    }

    ClassSignature
    decodeClassSignature(String cs) {
        try {
            return SignatureParser.decodeClassSignature(cs);
        } catch (SignatureException e) {
            error("Decoding class signature '" + cs + "': " + e.getMessage());
            return new ClassSignature(NO_FORMAL_TYPE_PARAMETERS, SignatureParser.OBJECT, NO_CLASS_TYPE_SIGNATURES);
        }
    }

    FieldTypeSignature
    decodeFieldTypeSignature(String fs) {
        try {
            return SignatureParser.decodeFieldTypeSignature(fs);
        } catch (SignatureException e) {
            error("Decoding field type signature '" + fs + "': " + e.getMessage());
            return SignatureParser.OBJECT;
        }
    }

    MethodTypeSignature
    decodeMethodTypeSignature(String ms) {
        try {
            return SignatureParser.decodeMethodTypeSignature(ms);
        } catch (SignatureException e) {
            error("Decoding method type signature '" + ms + "': " + e.getMessage());
            return new MethodTypeSignature(
                NO_FORMAL_TYPE_PARAMETERS,
                NO_TYPE_SIGNATURES,
                SignatureParser.VOID,
                NO_THROWS_SIGNATURES
            );
        }
    }

    TypeSignature
    decodeFieldDescriptor(String fd) {
        try {
            return SignatureParser.decodeFieldDescriptor(fd);
        } catch (SignatureException e) {
            error("Decoding field descriptor '" + fd + "': " + e.getMessage());
            return SignatureParser.INT;
        }
    }

    MethodTypeSignature
    decodeMethodDescriptor(String md) {
        try {
            return SignatureParser.decodeMethodDescriptor(md);
        } catch (SignatureException e) {
            error("Decoding method descriptor '" + md + "': " + e.getMessage());
            return new MethodTypeSignature(
                NO_FORMAL_TYPE_PARAMETERS,
                NO_TYPE_SIGNATURES,
                SignatureParser.VOID,
                NO_THROWS_SIGNATURES
            );
        }
    }

    /**
     * Representation of a local variable reference in the {@code Code} attribute.
     */
    class LocalVariable {
        @Nullable final TypeSignature typeSignature;
        final String                  name;

        public
        LocalVariable(@Nullable TypeSignature typeSignature, String name) {
            this.typeSignature = typeSignature;
            this.name          = name;
        }

        @Override public String
        toString() {
            TypeSignature ts = this.typeSignature;
            return ts == null ? " [" + this.name + ']' : " [" + ts.toString() + ' ' + this.name + ']';
        }
    }

    public void
    error(String message) {
        this.pw.println("*** Error: " + message);
    }

    /**
     * Static description of a Java&trade; byte code instruction.
     */
    private static
    class Instruction {

        /**
         * @param operands <code>null</code> is equivalent to "zero operands"
         */
        public
        Instruction(String mnemonic, Operand[] operands) {
            this.mnemonic = mnemonic;
            this.operands = operands;
        }
        public String    getMnemonic() { return this.mnemonic; }
        public Operand[] getOperands() { return this.operands; }

        @Override public String
        toString() {
            return this.mnemonic;
        }
        private final String    mnemonic;
        private final Operand[] operands;
    }

    /**
     * Static description of an operand of a Java&trade; byte code instruction.
     */
    private
    interface Operand {

        /**
         * @return One space and this operand disassembled
         */
        String
        disassemble(
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
    @NotNullByDefault(false) private static
    class CountingInputStream extends FilterInputStream {

        public
        CountingInputStream(InputStream is) {
            super(is);
        }

        @Override public int
        read() throws IOException {
            int res = super.read();
            if (res != -1) ++this.count;
            return res;
        }

        @Override public int
        read(byte[] b, int off, int len) throws IOException {
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
    private static String
    decodeAccess(short n) {
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

    private String
    beautify(String s) {
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
