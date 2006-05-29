
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
        ProtectionDomain getProtectionDomain(String name);
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
                ClassLoader.getSystemClassLoader(), // parentClassLoader
                optionalSourcePath,                 // optionalSourcePath
                optionalCharacterEncoding,          // optionalCharacterEncoding
                debuggingInformation                // debuggingInformation
            );
        } else {
            cl = new CachingJavaSourceClassLoader(
                new ClassLoader(null) {}, // parentClassLoader
                optionalSourcePath,                 // optionalSourcePath
                optionalCharacterEncoding,          // optionalCharacterEncoding
                new File(optionalCacheDirName),     // cacheDirectory
                debuggingInformation                // debuggingInformation
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
            sourceFinder,                                  // sourceFinder
            optionalCharacterEncoding,                     // optionalCharacterEncoding
            this.unitCompilers,                            // unitCompilers
            new ClassLoaderIClassLoader(parentClassLoader) // optionalParentIClassLoader
        );

        this.debuggingInformation = debuggingInformation;
    }

    /**
     * @see UnitCompiler#setCompileErrorHandler
     */
    public void setCompileErrorHandler(UnitCompiler.ErrorHandler optionalCompileErrorHandler) {
        this.iClassLoader.setCompileErrorHandler(optionalCompileErrorHandler);
    }

    /**
     * @see Parser#setWarningHandler(WarningHandler)
     * @see UnitCompiler#setCompileErrorHandler
     */
    public void setWarningHandler(WarningHandler optionalWarningHandler) {
        this.iClassLoader.setWarningHandler(optionalWarningHandler);
    }

    /**
     * Implementation of {@link ClassLoader#findClass(String)}.
     * 
     * @throws ClassNotFoundException
     */
    protected Class findClass(String name) throws ClassNotFoundException {

        Map bytecodes = this.generateBytecodes(name);
        if (bytecodes == null) throw new ClassNotFoundException(name);

        Class clazz = this.defineBytecodes(name, bytecodes);
        if (clazz == null) throw new RuntimeException("Scanning, parsing and compiling class \"" + name + "\" did not create a class file!?");
        return clazz;
    }

    /**
     * Find, scan, parse the right compilation unit. Compile the parsed compilation unit to
     * bytecode. This may cause more compilation units being scanned and parsed. Continue until
     * all compilation units are compiled.
     *
     * @return <code>null</code> if no source code could be found
     */
    protected Map generateBytecodes(String name) throws ClassNotFoundException {
        if (this.iClassLoader.loadIClass(Descriptor.fromClassName(name)) == null) return null;

        Map bytecodes = new HashMap(); // String name => byte[] bytecode
        Set compiledUnitCompilers = new HashSet();
        COMPILE_UNITS:
        for (;;) {
            for (Iterator it = this.unitCompilers.iterator(); it.hasNext();) {
                UnitCompiler uc = (UnitCompiler) it.next();
                if (!compiledUnitCompilers.contains(uc)) {
                    ClassFile[] cfs;
                    try {
                        cfs = uc.compileUnit(this.debuggingInformation);
                    } catch (CompileException ex) {
                        throw new ClassNotFoundException("Compiling unit \"" + uc + "\"", ex);
                    }
                    for (int i = 0; i < cfs.length; ++i) {
                        ClassFile cf = cfs[i];
                        bytecodes.put(cf.getThisClassName(), cf.toByteArray());
                    }
                    compiledUnitCompilers.add(uc);
                    continue COMPILE_UNITS;
                }
            }
            return bytecodes;
        }
    }

    /**
     * Define a set of classes, like
     * {@link java.lang.ClassLoader#defineClass(java.lang.String, byte[], int, int)}.
     * If the <code>bytecodes</code> contains an entry for <code>name</code>, then the
     * {@link Class} defined for that name is returned.
     *
     * @param bytecodes String name => byte[] bytecode
     */
    protected Class defineBytecodes(String name, Map bytecodes) throws ClassFormatError {
        Class clazz = null;
        for (Iterator it = bytecodes.entrySet().iterator(); it.hasNext();) {
            Map.Entry me = (Map.Entry) it.next();
            String name2 = (String) me.getKey();
            byte[] ba = (byte[]) me.getValue();

            Class c = this.defineBytecode(name2, ba);
            if (name2.equals(name)) clazz = c;
        }
        return clazz;
    }

    /**
     * Calls {@link java.lang.ClassLoader#defineClass(java.lang.String, byte[], int, int)}
     * or {@link java.lang.ClassLoader#defineClass(java.lang.String, byte[], int, int, java.security.ProtectionDomain)},
     * depending on whether or not a {@link ProtectionDomainFactory} was set.
     *
     * @see #setProtectionDomainFactory
     */
    protected Class defineBytecode(String className, byte[] ba) throws ClassFormatError {
        if (this.protectionDomainFactory == null) {
            return this.defineClass(className, ba, 0, ba.length);
        } else
        {
            String sourceName = ClassFile.getSourceResourceName(className);
            ProtectionDomain domain = this.protectionDomainFactory.getProtectionDomain(sourceName);
            return this.defineClass(className, ba, 0, ba.length, domain);
        }
    }

    public void setProtectionDomainFactory(ProtectionDomainFactory protectionDomainFactory) {
        this.protectionDomainFactory = protectionDomainFactory;
    }

    private final JavaSourceIClassLoader iClassLoader;
    private final EnumeratorSet          debuggingInformation;
    private ProtectionDomainFactory      protectionDomainFactory;

    /**
     * Collection of parsed, but uncompiled compilation units.
     */
    private final Set unitCompilers = new HashSet(); // UnitCompiler
}
