
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

package org.codehaus.janino;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.TunnelException;
import org.codehaus.janino.util.enum.EnumeratorFormatException;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;


/**
 * A {@link ClassLoader} that, unlike usual {@link ClassLoader}s,
 * does not load byte code, but reads Java<sup>TM</sup> source code and then scans, parses,
 * compiles and loads it into the virtual machine. 
 */
public class JavaSourceClassLoader extends ClassLoader {

    /**
     * Read Java<sup>TM</sup> source code for a given class name, scan, parse, compile and load
     * it into the virtual machine, and invoke its "main()" method.
     * <p>
     * Usage is as follows:
     * <pre>
     *   java [ <i>java-option</i> ] org.codehaus.janino.JavaSourceClassLoader [ <i>option</i> ] ... <i>class-name</i> [ <i>arg</i> ] ... 
     *     <i>java-option</i> Any valid option for the Java Virtual Machine (e.g. "-classpath <i>colon-separated-list-of-class-directories</i>") 
     *     <i>option</i>:
     *       -sourcepath <i>colon-separated-list-of-source-directories</i> 
     *       -encoding <i>character-encoding</i>
     * </pre>
     */
    public static void main(String[] args) {
        File[]               optionalSourcePath = { new File(".") };
        String               optionalCharacterEncoding = null;
        DebuggingInformation debuggingInformation = DebuggingInformation.LINES.add(DebuggingInformation.SOURCE);

        // Scan command line options.
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;

            if ("-sourcepath".equals(arg)) {
                optionalSourcePath = PathResourceFinder.parsePath(args[++i]);
            } else
            if ("-encoding".equals(arg)) {
                optionalCharacterEncoding = args[++i]; 
            } else
            if (arg.equals("-g")) {
                debuggingInformation = DebuggingInformation.ALL;
            } else
            if (arg.equals("-g:none")) {
                debuggingInformation = DebuggingInformation.NONE;
            } else
            if (arg.startsWith("-g:")) {
                try {
                    debuggingInformation = new DebuggingInformation(arg.substring(3).toUpperCase());
                } catch (EnumeratorFormatException ex) {
                    debuggingInformation = DebuggingInformation.NONE;
                }
            } else
            if ("-help".equals(arg)) {
                System.out.println("Usage:"); 
                System.out.println("  java [ <java-option> ] " + JavaSourceClassLoader.class.getName() + " [ <option>] ... <class-name> [ <arg> ] ..."); 
                System.out.println("Load a Java class by name and invoke its \"main(String[])\" method,"); 
                System.out.println("pass"); 
                System.out.println("  <java-option> Any valid option for the Java Virtual Machine (e.g. \"-classpath <dir>\")"); 
                System.out.println("  <option>:"); 
                System.out.println("    -sourcepath <" + File.pathSeparator + "-separated-list-of-source-directories>"); 
                System.out.println("    -encoding <character-encoding>");
                System.out.println("    -g                     Generate all debugging info");
                System.out.println("    -g:none                Generate no debugging info");
                System.out.println("    -g:{lines,vars,source} Generate only some debugging info");
                System.exit(0); 
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\"; try \"-help\"");
                System.exit(1);
            }
        }

        // Determine class name.
        if (i == args.length) {
            System.err.println("No class name given, try \"-help\"");
            System.exit(1);
        }
        String className = args[i++];

        // Determine arguments passed to "main()".
        String[] mainArgs = new String[args.length - i];
        System.arraycopy(args, i, mainArgs, 0, args.length - i);

        // Set up a JavaSourceClassLoader.
        ClassLoader cl = new JavaSourceClassLoader(
            ClassLoader.getSystemClassLoader(),
            optionalSourcePath,
            optionalCharacterEncoding,
            debuggingInformation
        );

        // Load the given class.
        Class clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException ex) {
            System.err.println("Loading class \"" + className + "\": " + ex.getMessage());
            System.exit(1);
            return; // NEVER REACHED
        }

        // Find its "main" method.
        Method mainMethod;
        try {
            mainMethod = clazz.getMethod("main", new Class[] { String[].class });
        } catch (NoSuchMethodException ex) {
            System.err.println("Class \"" + className + "\" has not public method \"main(String[])\".");
            System.exit(1);
            return; // NEVER REACHED
        }

        // Invoke the "main" method.
        try {
            mainMethod.invoke(null, new Object[] { mainArgs });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Set up a {@link JavaSourceClassLoader} that finds Java<sup>TM</sup> source code in a file
     * that resides in either of the directories specified by the given source path.
     * 
     * @param parentClassLoader See {@link ClassLoader}
     * @param optionalSourcePath A collection of directories that are searched for Java<sup>TM</sup> source files in the given order 
     * @param optionalCharacterEncoding The encoding of the Java<sup>TM</sup> source files (<code>null</code> for platform default encoding)
     */
    public JavaSourceClassLoader(
        ClassLoader          parentClassLoader,
        File[]               optionalSourcePath,
        String               optionalCharacterEncoding,
        DebuggingInformation debuggingInformation
    ) {
        super(parentClassLoader);

        // Process the source path.
        ResourceFinder sourceFinder = (
            optionalSourcePath == null ?
            (ResourceFinder) new DirectoryResourceFinder(new File(".")) :
            (ResourceFinder) new PathResourceFinder(optionalSourcePath)
        );

        this.iClassLoader = new JavaSourceIClassLoader(
            sourceFinder,
            optionalCharacterEncoding,
            this.uncompiledCompilationUnits,
            new ClassLoaderIClassLoader(parentClassLoader)
        );

        this.debuggingInformation = debuggingInformation;
    }

    /**
     * Implementation of {@link ClassLoader#findClass(String)}.
     * @throws ClassNotFoundException
     * @throws TunnelException wraps a {@link Scanner.ScanException}
     * @throws TunnelException wraps a {@link Parser.ParseException}
     * @throws TunnelException wraps a {@link Java.CompileException}
     * @throws TunnelException wraps a {@link IOException}
     */
    protected Class findClass(String name) throws ClassNotFoundException {

        // Check precompiled classes (classes that were parsed and compiled, but were not yet needed).
        ClassFile cf = (ClassFile) this.precompiledClasses.get(name);

        if (cf == null) {

            // Check parsed, but uncompiled compilation units.
            Java.CompilationUnit compilationUnitToCompile = null;
            for (Iterator i = this.uncompiledCompilationUnits.iterator(); i.hasNext();) {
                Java.CompilationUnit cu = (Java.CompilationUnit) i.next();
                if (cu.findClass(name) != null) {
                    compilationUnitToCompile = cu;
                    break;
                }
            }

            // Find and parse the right compilation unit.
            if (compilationUnitToCompile == null) {

                // Find and parse the right compilation unit, and add it to
                // "this.uncompiledCompilationUnits".
                if (this.iClassLoader.loadIClass(Descriptor.fromClassName(name)) == null) throw new ClassNotFoundException(name);

                for (Iterator i = this.uncompiledCompilationUnits.iterator(); i.hasNext();) {
                    Java.CompilationUnit cu = (Java.CompilationUnit) i.next();
                    if (cu.findClass(name) != null) {
                        compilationUnitToCompile = cu;
                        break;
                    }
                }
                if (compilationUnitToCompile == null) throw new RuntimeException(); // SNO: Loading IClass does not parse the CU that defines the IClass.
            }

            // Compile the compilation unit.
            ClassFile[] cfs;
            try {
                cfs = compilationUnitToCompile.compile(this.iClassLoader, this.debuggingInformation);
            } catch (Java.CompileException e) {
                throw new TunnelException(e);
            }

            // Now that the CU is compiled, remove it from the set of uncompiled CUs.
            this.uncompiledCompilationUnits.remove(compilationUnitToCompile);

            // Get the generated class file.
            for (int i = 0; i < cfs.length; ++i) {
                if (cfs[i].getThisClassName().equals(name)) {
                    if (cf != null) throw new RuntimeException(); // SNO: Multiple CFs with the same name.
                    cf = cfs[i];
                } else {
                    if (this.precompiledClasses.containsKey(cfs[i].getThisClassName())) throw new TunnelException(new Java.CompileException("Class or interface \"" + name + "\" is defined in more than one compilation unit", null));
                    this.precompiledClasses.put(cfs[i].getThisClassName(), cfs[i]);
                }
            }
            if (cf == null) throw new RuntimeException(); // SNO: Compilation of CU does not generate CF with requested name.
        }

        if (cf == null) throw new ClassNotFoundException(name);

        // Store the class file's byte code into a byte array.
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            cf.store(bos);
        } catch (IOException ex) {
            throw new RuntimeException(); // SNO: ByteArrayOutputStream thows IOException,
        }
        byte[] ba = bos.toByteArray();

        // Load the byte code into the virtual machine.
        return this.defineClass(name, ba, 0, ba.length);
    }

    private final IClassLoader         iClassLoader;
    private final DebuggingInformation debuggingInformation;

    /**
     * Collection of parsed, but uncompiled compilation units.
     */
    private final Set uncompiledCompilationUnits = new HashSet(); // Java.CompilationUnit

    /**
     * Map of classes that were parsed and compiled into class files, but not yet loaded into the
     * virtual machine.
     */
    private final Map precompiledClasses = new HashMap(); // class name => Java.ClassFile
}
