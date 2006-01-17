
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

import java.security.ProtectionDomain;

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.*;
import org.codehaus.janino.util.resource.*;


/**
 * A {@link ClassLoader} that, unlike usual {@link ClassLoader}s,
 * does not load byte code, but reads Java<sup>TM</sup> source code and then scans, parses,
 * compiles and loads it into the virtual machine. 
 */
public class JavaSourceClassLoader extends ClassLoader {

    public interface ProtectionDomainFactory {
        ProtectionDomain getProtectionDomain(String name) throws Exception;
    }
    
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
     *       -g                           Generate all debugging info");
     *       -g:none                      Generate no debugging info");
     *       -g:{lines,vars,source}       Generate only some debugging info");
     *       -cache <i>dir</i>                      Cache compiled classes here");

     * </pre>
     */
    public static void main(String[] args) {
        File[]        optionalSourcePath = null;
        String        optionalCharacterEncoding = null;
        EnumeratorSet debuggingInformation = DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION;
        String        optionalCacheDirName = null;

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
                    debuggingInformation = new EnumeratorSet(DebuggingInformation.class, arg.substring(3));
                } catch (EnumeratorFormatException ex) {
                    debuggingInformation = DebuggingInformation.NONE;
                }
            } else
            if ("-cache".equals(arg)) {
                optionalCacheDirName = args[++i]; 
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
                System.out.println("    -cache <dir>           Cache compiled classes here");
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

        // Set up a JavaSourceClassLoader or a CachingJavaSourceClassLoader.
        ClassLoader cl;
        if (optionalCacheDirName == null) {
            cl = new JavaSourceClassLoader(
                ClassLoader.getSystemClassLoader(),
                optionalSourcePath,
                optionalCharacterEncoding,
                debuggingInformation
            );
        } else {
            File cacheDirectory = new File(optionalCacheDirName);
            cl = new CachingJavaSourceClassLoader(
                ClassLoader.getSystemClassLoader(),
                optionalSourcePath,
                optionalCharacterEncoding,
                cacheDirectory,
                debuggingInformation
            );
        }

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
     * @param debuggingInformation What kind of debugging information to generate
     */
    public JavaSourceClassLoader(
        ClassLoader   parentClassLoader,
        File[]        optionalSourcePath,
        String        optionalCharacterEncoding,
        EnumeratorSet debuggingInformation
    ) {
        this(
            parentClassLoader,         // parentClassLoader
            (                          // sourceFinder
                optionalSourcePath == null ?
                (ResourceFinder) new DirectoryResourceFinder(new File(".")) :
                (ResourceFinder) new PathResourceFinder(optionalSourcePath)
            ),
            optionalCharacterEncoding, // optionalCharacterEncoding
            debuggingInformation       // debuggingInformation
        );
    }

    /**
     * Set up a {@link JavaSourceClassLoader} that finds Java<sup>TM</sup> source code through
     * a given {@link ResourceFinder}.
     * 
     * @param parentClassLoader See {@link ClassLoader}
     * @param sourceFinder Used to locate additional source files 
     * @param optionalCharacterEncoding The encoding of the Java<sup>TM</sup> source files (<code>null</code> for platform default encoding)
     * @param debuggingInformation What kind of debugging information to generate
     */
    public JavaSourceClassLoader(
        ClassLoader    parentClassLoader,
        ResourceFinder sourceFinder,
        String         optionalCharacterEncoding,
        EnumeratorSet  debuggingInformation
    ) {
        super(parentClassLoader);

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
     * 
     * @throws ClassNotFoundException
     * @throws TunnelException wraps a {@link Scanner.ScanException}
     * @throws TunnelException wraps a {@link Parser.ParseException}
     * @throws TunnelException wraps a {@link CompileException}
     * @throws TunnelException wraps a {@link IOException}
     */
    protected Class findClass(String name) throws ClassNotFoundException {

        // Find the ClassFile object.
        ClassFile cf;
        try {
            cf = this.findClassFile(name);
        } catch (Scanner.ScanException ex) {
            throw new TunnelException(ex);
        } catch (Parser.ParseException ex) {
            throw new TunnelException(ex);
        } catch (CompileException ex) {
            throw new TunnelException(ex);
        } catch (IOException ex) {
            throw new TunnelException(ex);
        }
        if (cf == null) throw new ClassNotFoundException(name);

        // Load the byte code into the virtual machine.
        ProtectionDomain domain = null;
        if (this.protectionDomainFactory != null) {
            String sourceName = ClassFile.getSourceResourceName(cf.getThisClassName());
            try {
                domain = this.protectionDomainFactory.getProtectionDomain(sourceName);
            } catch (Exception ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }

        byte[] ba = cf.toByteArray();
        return (
            domain != null ?
            this.defineClass(name, ba, 0, ba.length, domain) :
            this.defineClass(name, ba, 0, ba.length)
        );
    }

    /**
     * @return <code>null</code> if a class file could not be found
     */
    protected ClassFile findClassFile(String className) throws
        Scanner.ScanException,
        Parser.ParseException,
        CompileException,
        IOException
    {

        // Check precompiled classes (classes that were parsed and compiled, but were not yet needed).
        ClassFile cf = (ClassFile) this.precompiledClasses.get(className);

        if (cf == null) {

            // Check parsed, but uncompiled compilation units.
            UnitCompiler compilationUnitToCompile = null;
            for (Iterator i = this.uncompiledCompilationUnits.iterator(); i.hasNext();) {
                UnitCompiler uc = (UnitCompiler) i.next();
                if (uc.findClass(className) != null) {
                    compilationUnitToCompile = uc;
                    break;
                }
            }

            // Find and parse the right compilation unit.
            if (compilationUnitToCompile == null) {

                // Find and parse the right compilation unit, and add it to
                // "this.uncompiledCompilationUnits".
                if (this.iClassLoader.loadIClass(Descriptor.fromClassName(className)) == null) return null;

                for (Iterator i = this.uncompiledCompilationUnits.iterator(); i.hasNext();) {
                    UnitCompiler uc = (UnitCompiler) i.next();
                    if (uc.findClass(className) != null) {
                        compilationUnitToCompile = uc;
                        break;
                    }
                }
                if (compilationUnitToCompile == null) throw new RuntimeException(); // SNO: Loading IClass does not parse the CU that defines the IClass.
            }

            // Compile the compilation unit.
            ClassFile[] cfs;
            try {
                cfs = compilationUnitToCompile.compileUnit(this.debuggingInformation);
            } catch (CompileException e) {
                throw new TunnelException(e);
            }

            // Now that the CU is compiled, remove it from the set of uncompiled CUs.
            this.uncompiledCompilationUnits.remove(compilationUnitToCompile);

            // Get the generated class file.
            for (int i = 0; i < cfs.length; ++i) {
                if (cfs[i].getThisClassName().equals(className)) {
                    if (cf != null) throw new RuntimeException(); // SNO: Multiple CFs with the same name.
                    cf = cfs[i];
                } else {
                    if (this.precompiledClasses.containsKey(cfs[i].getThisClassName())) throw new TunnelException(new CompileException("Class or interface \"" + className + "\" is defined in more than one compilation unit", null));
                    this.precompiledClasses.put(cfs[i].getThisClassName(), cfs[i]);
                }
            }
            if (cf == null) throw new RuntimeException(); // SNO: Compilation of CU does not generate CF with requested name.
        }
        return cf;
    }

    public void setProtectionDomainFactory(ProtectionDomainFactory protectionDomainFactory) {
        this.protectionDomainFactory = protectionDomainFactory;
    }

    private final IClassLoader      iClassLoader;
    private final EnumeratorSet     debuggingInformation;
    private ProtectionDomainFactory protectionDomainFactory;

    /**
     * Collection of parsed, but uncompiled compilation units.
     */
    private final Set uncompiledCompilationUnits = new HashSet(); // UnitCompiler

    /**
     * Map of classes that were parsed and compiled into class files, but not yet loaded into the
     * virtual machine.
     */
    private final Map precompiledClasses = new HashMap(); // class name => Java.ClassFile
}
