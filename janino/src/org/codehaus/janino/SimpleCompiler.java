
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
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

    private ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
    private Class[]     optionalAuxiliaryClasses = null;

    // Set when "cook()"ing.
    private AuxiliaryClassLoader classLoader = null;
    private IClassLoader         iClassLoader = null;

    private ClassLoader result = null;

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("-help")) {
            System.out.println("Usage:");
            System.out.println("    org.codehaus.janino.SimpleCompiler <source-file> <class-name> { <argument> }");
            System.out.println("Reads a compilation unit from the given <source-file> and invokes method");
            System.out.println("\"public static void main(String[])\" of class <class-name>, passing the.");
            System.out.println("given <argument>s.");
            System.exit(1);
        }
        
        if (args.length < 2) {
            System.err.println("Source file and/or class name missing; try \"-help\".");
            System.exit(1);
        }

        // Get source file.
        String sourceFileName = args[0];

        // Get class name.
        String className = args[1];

        // Get arguments.
        String[] arguments = new String[args.length - 2];
        System.arraycopy(args, 2, arguments, 0, arguments.length);

        // Compile the source file.
        ClassLoader cl = new SimpleCompiler(sourceFileName, new FileInputStream(sourceFileName)).getClassLoader();

        // Load the class.
        Class c = cl.loadClass(className);

        // Invoke the "public static main(String[])" method.
        Method m = c.getMethod("main", new Class[] { String[].class });
        m.invoke(null, new Object[] { arguments });
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
     * The "parent class loader" is used to load referenced classes. Useful values are:
     * <table border="1"><tr>
     *   <td><code>System.getSystemClassLoader()</code></td>
     *   <td>The running JVM's class path</td>
     * </tr><tr>
     *   <td><code>Thread.currentThread().getContextClassLoader()</code> or <code>null</code></td>
     *   <td>The class loader effective for the invoking thread</td>
     * </tr><tr>
     *   <td>{@link #BOOT_CLASS_LOADER}</td>
     *   <td>The running JVM's boot class path</td>
     * </tr></table>
     * The parent class loader defaults to the current thread's context class loader.
     */
    public void setParentClassLoader(ClassLoader optionalParentClassLoader) {
        this.setParentClassLoader(optionalParentClassLoader, null);
    }

    /**
     * A {@link ClassLoader} that finds the classes on the JVM's <i>boot class path</i> (e.g.
     * <code>java.io.*</code>), but not the classes on the JVM's <i>class path</i>.
     */
    public static final ClassLoader BOOT_CLASS_LOADER = new ClassLoader(null) {};

    /**
     * Allow references to the classes loaded through this parent class loader
     * (@see {@link #setParentClassLoader(ClassLoader)}), plus the extra
     * <code>auxiliaryClasses</code>.
     * <p>
     * Notice that the <code>auxiliaryClasses</code> must either be loadable through the
     * <code>optionalParentClassLoader</code> (in which case they have no effect), or
     * <b>no class with the same name</b> must be loadable through the
     * <code>optionalParentClassLoader</code>.
     */
    public void setParentClassLoader(ClassLoader optionalParentClassLoader, Class[] auxiliaryClasses) {
        assertNotCooked();
        this.parentClassLoader = (
            optionalParentClassLoader != null
            ? optionalParentClassLoader
            : Thread.currentThread().getContextClassLoader()
        );
        this.optionalAuxiliaryClasses = auxiliaryClasses;
    }

    public void cook(Scanner scanner)
    throws CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this.setUpClassLoaders();

        // Parse the compilation unit.
        Java.CompilationUnit compilationUnit = new Parser(scanner).parseCompilationUnit();

        // Compile the classes and load them.
        this.compileToClassLoader(
            compilationUnit,
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION
        );
    }
    
    /**
     * Cook this compilation unit directly. 
     *  See {@link Cookable#cook}
     */
    public void cook(Java.CompilationUnit compilationUnit) throws CompileException {
        this.setUpClassLoaders();

        // Compile the classes and load them.
        this.compileToClassLoader(
            compilationUnit,
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION
        );
    }

    /**
     * Initializes {@link #classLoader} and {@link #iClassLoader} from the configured
     * {@link #parentClassLoader} and {@link #optionalAuxiliaryClasses}. These are needed by
     * {@link #classToType(Location, Class)} and friends which are used when creating the AST.
     */
    protected final void setUpClassLoaders() {
        assertNotCooked();

        // Set up the ClassLoader for the compilation and the loading.
        this.classLoader = new AuxiliaryClassLoader(this.parentClassLoader);
        if (this.optionalAuxiliaryClasses != null) {
            for (int i = 0; i < this.optionalAuxiliaryClasses.length; ++i) {
                this.classLoader.addAuxiliaryClass(this.optionalAuxiliaryClasses[i]);
            }
        }

        this.iClassLoader = new ClassLoaderIClassLoader(this.classLoader);
    }

    /**
     * A {@link ClassLoader} that intermixes that classes loaded by its parent with a map of
     * "auxiliary classes".
     */
    private static final class AuxiliaryClassLoader extends ClassLoader {
        private final Map auxiliaryClasses = new HashMap(); // String name => Class

        private AuxiliaryClassLoader(ClassLoader parent) {
            super(parent);
        }

        protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class c = (Class) this.auxiliaryClasses.get(name);
            if (c != null) return c;
        
            return super.loadClass(name, resolve);
        }

        private void addAuxiliaryClass(Class c) {
            if (this.auxiliaryClasses.containsKey(c.getName())) return;

            // Check whether the auxiliary class is conflicting with this ClassLoader.
            try {
                Class c2 = super.loadClass(c.getName(), false);
                if (c2 != c) throw new RuntimeException("Trying to add an auxiliary class \"" + c.getName() + "\" while another class with the same name is already loaded");
            } catch (ClassNotFoundException ex) {
                ;
            }

            this.auxiliaryClasses.put(c.getName(), c);

            {
                Class sc = c.getSuperclass();
                if (sc != null) this.addAuxiliaryClass(sc);
            }

            {
                Class[] ifs = c.getInterfaces();
                for (int i = 0; i < ifs.length; ++i) this.addAuxiliaryClass(ifs[i]);
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof AuxiliaryClassLoader)) return false;
            AuxiliaryClassLoader that = (AuxiliaryClassLoader) o;

            {
                final ClassLoader parentOfThis = this.getParent();
                final ClassLoader parentOfThat = that.getParent();
                if (parentOfThis == null ? parentOfThat != null : !parentOfThis.equals(parentOfThat)) return false;
            }

            return this.auxiliaryClasses.equals(that.auxiliaryClasses);
        }

        public int hashCode() {
            ClassLoader parent = this.getParent();
            return (parent == null ? 0 : parent.hashCode()) ^ this.auxiliaryClasses.hashCode();
        }
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
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.result;
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
        if (this.result == null || that.result == null) throw new IllegalStateException("Equality can only be checked after cooking");
        return this.result.equals(that.result);
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

        this.classLoader.addAuxiliaryClass(optionalClass);

        IClass iClass;
        try {
            iClass = this.iClassLoader.loadIClass(Descriptor.fromClassName(optionalClass.getName()));
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
    protected final ClassLoader compileToClassLoader(
        Java.CompilationUnit compilationUnit,
        EnumeratorSet        debuggingInformation
    ) throws CompileException {
        if (SimpleCompiler.DEBUG) {
            UnparseVisitor.unparse(compilationUnit, new OutputStreamWriter(System.out));
        }

        // Compile compilation unit to class files.
        ClassFile[] classFiles = new UnitCompiler(
            compilationUnit,
            this.iClassLoader
        ).compileUnit(debuggingInformation);

        // Convert the class files to bytes and store them in a Map.
        Map classes = new HashMap(); // String className => byte[] data
        for (int i = 0; i < classFiles.length; ++i) {
            ClassFile cf = classFiles[i];
            classes.put(cf.getThisClassName(), cf.toByteArray());
        }

        // Disassemble all generated classes (for debugging).
        if (SimpleCompiler.DEBUG) {
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
        this.result = new ByteArrayClassLoader(
            classes,         // classes
            this.classLoader // parent
        );
        return this.result;
    }

    /**
     * Throw an {@link IllegalStateException} if this {@link Cookable} is already cooked.
     */
    protected void assertNotCooked() {
        if (this.classLoader != null) throw new IllegalStateException("Already cooked");
    }
}
