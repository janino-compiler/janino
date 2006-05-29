
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

package org.codehaus.janino;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.codehaus.janino.tools.Disassembler;
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;

/**
 * A simplified version of {@link Compiler} that can compile only a single
 * compilation unit. (A "compilation unit" is the characters stored in a
 * ".java" file.)
 * <p>
 * Opposed to a normal ".java" file, you can declare multiple public classes
 * here.
 * <p>
 * To set up a {@link SimpleCompiler} object, proceed as follows:
 * <ol>
 *   <li>
 *   Create the {@link SimpleCompiler} using {@link #SimpleCompiler()}
 *   <li>
 *   Optionally set an alternate parent class loader through
 *   {@link #setParentClassLoader(ClassLoader)}.
 *   <li>
 *   Call any of the {@link org.codehaus.janino.Cookable#cook(Scanner)} methods to scan,
 *   parse, compile and load the compilation unit into the JVM.
 * </ol>
 * Alternatively, a number of "convenience constructors" exist that execute the steps described
 * above instantly.
 */
public class SimpleCompiler extends Cookable {
    private final static boolean DEBUG = false;

    private final ClassLoaderIClassLoader DEFAULT_ICLASSLOADER = new ClassLoaderIClassLoader(Thread.currentThread().getContextClassLoader());
    private ClassLoaderIClassLoader       classLoaderIClassLoader = this.DEFAULT_ICLASSLOADER;

    private ClassLoader                   classLoader = null; // null=uncooked

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("    org.codehaus.janino.SimpleCompiler <source-file> <class-name> <arg> [ ... ]");
            System.err.println("Reads a compilation unit from the given <source-file> and invokes method");
            System.err.println("\"public static void main(String[])\" of class <class-name>.");
            System.exit(1);
        }

        String sourceFileName = args[0];
        String className = args[1];
        String[] mainArgs = new String[args.length - 2];
        System.arraycopy(args, 2, mainArgs, 0, mainArgs.length);

        ClassLoader cl = new SimpleCompiler(sourceFileName, new FileInputStream(sourceFileName)).getClassLoader();
        Class c = cl.loadClass(className);
        Method m = c.getMethod("main", new Class[] { String[].class });
        m.invoke(null, new Object[] { mainArgs });
    }

    /**
     * Equivalent to<pre>
     * SimpleCompiler sc = new SimpleCompiler();
     * sc.cook(optionalFileName, in);</pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cook(String, Reader)
     */
    public SimpleCompiler(
        String optionalFileName,
        Reader in
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this.cook(optionalFileName, in);
    }

    /**
     * Equivalent to<pre>
     * SimpleCompiler sc = new SimpleCompiler();
     * sc.cook(optionalFileName, is);</pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cook(String, InputStream)
     */
    public SimpleCompiler(
        String      optionalFileName,
        InputStream is
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this.cook(optionalFileName, is);
    }

    /**
     * Equivalent to<pre>
     * SimpleCompiler sc = new SimpleCompiler();
     * sc.cook(fileName);</pre>
     *
     * @see #SimpleCompiler()
     * @see Cookable#cookFile(String)
     */
    public SimpleCompiler(
        String fileName
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this.cookFile(fileName);
    }

    /**
     * Equivalent to<pre>
     * SimpleCompiler sc = new SimpleCompiler();
     * sc.setParentClassLoader(optionalParentClassLoader);
     * sc.cook(scanner);</pre>
     *
     * @see #SimpleCompiler()
     * @see #setParentClassLoader(ClassLoader)
     * @see Cookable#cook(Scanner)
     */
    public SimpleCompiler(
        Scanner     scanner,
        ClassLoader optionalParentClassLoader
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this.setParentClassLoader(optionalParentClassLoader);
        this.cook(scanner);
    }

    public SimpleCompiler() {}

    /**
     * The "parent class loader" is used to load referenced classes. It defaults to the current
     * thread's "context class loader".
     */
    public void setParentClassLoader(ClassLoader optionalParentClassLoader) {
        this.classLoaderIClassLoader = (
            optionalParentClassLoader != null ?
            new ClassLoaderIClassLoader(optionalParentClassLoader) :
            this.DEFAULT_ICLASSLOADER
        );
    }

    /**
     * Parse tokens delivered by the <code>scanner</code>, compile them and load them into the
     * JVM.
     * <p>
     * This method must be called exactly once.
     */
    protected void internalCook(Scanner scanner) throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {

        // Parse the compilation unit.
        Java.CompilationUnit compilationUnit = new Parser(scanner).parseCompilationUnit();

        // Compile the classes and load them.
        this.compileToClassLoader(
            compilationUnit,
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION
        );
    }

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can
     * be accessed. This {@link ClassLoader} can be used for subsequent calls to
     * {@link #SimpleCompiler(Scanner, ClassLoader)} in order to compile compilation units that
     * use types (e.g. declare derived types) declared in the previous one.
     * <p>
     * This method must only be called after {@link #cook(Scanner)}.
     * <p>
     * This method must not be called for instances of derived classes.
     */
    public ClassLoader getClassLoader() {
        if (this.getClass() != SimpleCompiler.class) throw new IllegalStateException("Must not be called on derived instances");
        if (this.classLoader == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.classLoader;
    }

    /**
     * Two {@link SimpleCompiler}s are regarded equal iff
     * <ul>
     *   <li>Both are objects of the same class (e.g. both are {@link ScriptEvaluator}s)
     *   <li>Both generated functionally equal classes as seen by {@link ByteArrayClassLoader#equals(Object)}
     * </ul>
     */
    public boolean equals(Object o) {
        if (!(o instanceof SimpleCompiler)) return false;
        SimpleCompiler that = (SimpleCompiler) o;
        if (this.getClass() != that.getClass()) return false;
        if (this.classLoader == null || that.classLoader == null) throw new IllegalStateException("Equality can only be checked after cooking");
        return this.classLoader.equals(that.classLoader);
    }

    public int hashCode() {
        return this.classLoader.hashCode();
    }

    /**
     * Wrap a reflection {@link Class} in a {@link Java.Type} object.
     */
    protected Java.Type classToType(
        Location    location,
        final Class optionalClass
    ) {
        if (optionalClass == null) return null;

        IClass iClass;
        try {
            iClass = this.classLoaderIClassLoader.loadIClass(Descriptor.fromClassName(optionalClass.getName()));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Loading IClass \"" + optionalClass.getName() + "\": " + ex);
        }
        if (iClass == null) throw new RuntimeException("Cannot load class \"" + optionalClass.getName() + "\" through the given ClassLoader");

        return new Java.SimpleType(location, iClass);
    }

    /**
     * Convert an array of {@link Class}es into an array of{@link Java.Type}s.
     */
    protected Java.Type[] classesToTypes(
        Location location,
        Class[]  classes
    ) {
        Java.Type[] types = new Java.Type[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            types[i] = this.classToType(location, classes[i]);
        }
        return types;
    }

    /**
     * Compile the given compilation unit. (A "compilation unit" is typically the contents
     * of a Java<sup>TM</sup> source file.)
     * 
     * @param compilationUnit The parsed compilation unit
     * @param debuggingInformation What kind of debugging information to generate in the class file
     * @return The {@link ClassLoader} into which the compiled classes were defined
     * @throws CompileException
     */
    protected ClassLoader compileToClassLoader(
        Java.CompilationUnit compilationUnit,
        EnumeratorSet        debuggingInformation
    ) throws CompileException {
        if (SimpleCompiler.DEBUG) {
            UnparseVisitor.unparse(compilationUnit, new OutputStreamWriter(System.out));
        }

        // Compile compilation unit to class files.
        ClassFile[] classFiles = new UnitCompiler(
            compilationUnit,
            this.classLoaderIClassLoader
        ).compileUnit(debuggingInformation);

        // Convert the class files to bytes and store them in a Map.
        Map classes = new HashMap(); // String className => byte[] data
        for (int i = 0; i < classFiles.length; ++i) {
            ClassFile cf = classFiles[i];
            classes.put(cf.getThisClassName(), cf.toByteArray());
        }

        // Disassemble all generated classes (for debugging).
        if (SimpleCompiler.DEBUG) {
//            try {
//                Disassembler.disasm(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }));
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            for (Iterator it = classes.entrySet().iterator(); it.hasNext();) {
                Map.Entry me = (Map.Entry) it.next();
                String className = (String) me.getKey();
                byte[] bytecode = (byte[]) me.getValue();
                System.out.println("*** Disassembly of class \"" + className + "\":");
                try {
                    new Disassembler().disasm(new ByteArrayInputStream(bytecode));
                    System.out.flush();
                } catch (IOException ex) {
                    throw new RuntimeException("SNO: IOException despite ByteArrayInputStream");
                }
            }
        }

        // Create a ClassLoader that loads the generated classes.
        this.classLoader = new ByteArrayClassLoader(
            classes,                                      // classes
            this.classLoaderIClassLoader.getClassLoader() // parent
        );
        return this.classLoader;
    }
}
