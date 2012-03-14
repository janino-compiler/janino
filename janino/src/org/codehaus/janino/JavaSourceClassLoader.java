
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
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

package org.codehaus.janino;

import java.io.File;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.resource.DirectoryResourceFinder;
import org.codehaus.janino.util.resource.PathResourceFinder;
import org.codehaus.janino.util.resource.ResourceFinder;

/**
 * A {@link ClassLoader} that, unlike usual {@link ClassLoader}s,
 * does not load byte code, but reads Java&trade; source code and then scans, parses,
 * compiles and loads it into the virtual machine.
 * <p>
 * As with any {@link ClassLoader}, it is not possible to "update" classes after they've been
 * loaded. The way to achieve this is to give up on the {@link JavaSourceClassLoader} and create
 * a new one.
 */
public class JavaSourceClassLoader extends AbstractJavaSourceClassLoader {

    public JavaSourceClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }

    public JavaSourceClassLoader(ClassLoader parentClassLoader) {
        this(
            parentClassLoader,
            (File[]) null,     // optionalSourcePath
            null               // optionalCharacterEncoding
        );
    }

    /**
     * Set up a {@link JavaSourceClassLoader} that finds Java&trade; source code in a file
     * that resides in either of the directories specified by the given source path.
     *
     * @param parentClassLoader         See {@link ClassLoader}
     * @param optionalSourcePath        A collection of directories that are searched for Java&trade; source files in
     *                                  the given order
     * @param optionalCharacterEncoding The encoding of the Java&trade; source files (<code>null</code> for platform
     *                                  default encoding)
     */
    public JavaSourceClassLoader(
        ClassLoader parentClassLoader,
        File[]      optionalSourcePath,
        String      optionalCharacterEncoding
    ) {
        this(
            parentClassLoader,        // parentClassLoader
            (                         // sourceFinder
                optionalSourcePath == null ?
                (ResourceFinder) new DirectoryResourceFinder(new File(".")) :
                (ResourceFinder) new PathResourceFinder(optionalSourcePath)
            ),
            optionalCharacterEncoding // optionalCharacterEncoding
        );
    }

    /**
     * Set up a {@link JavaSourceClassLoader} that finds Java&trade; source code through
     * a given {@link ResourceFinder}.
     * <p>
     * You can specify to include certain debugging information in the generated class files, which
     * is useful if you want to debug through the generated classes (see
     * {@link Scanner#Scanner(String, Reader)}).
     *
     * @param parentClassLoader         See {@link ClassLoader}
     * @param sourceFinder              Used to locate additional source files
     * @param optionalCharacterEncoding The encoding of the Java&trade; source files (<code>null</code> for platform
     *                                  default encoding)
     */
    public JavaSourceClassLoader(
        ClassLoader    parentClassLoader,
        ResourceFinder sourceFinder,
        String         optionalCharacterEncoding
    ) {
        super(parentClassLoader);

        this.iClassLoader = new JavaSourceIClassLoader(
            sourceFinder,                                  // sourceFinder
            optionalCharacterEncoding,                     // optionalCharacterEncoding
            this.unitCompilers,                            // unitCompilers
            new ClassLoaderIClassLoader(parentClassLoader) // optionalParentIClassLoader
        );
    }

    public void setSourcePath(File[] sourcePath) {
        this.iClassLoader.setSourceFinder(new PathResourceFinder(sourcePath));
    }

    public void setSourceFileCharacterEncoding(String optionalCharacterEncoding) {
        this.iClassLoader.setCharacterEncoding(optionalCharacterEncoding);
    }

    public void setDebuggingInfo(boolean debugSource, boolean debugLines, boolean debugVars) {
        this.debugSource = debugSource;
        this.debugLines = debugLines;
        this.debugVars = debugVars;
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
    protected /*synchronized <- No need to synchronize, because 'loadClass()' is synchronized */ Class
    findClass(String name) throws ClassNotFoundException {

        // Check if the bytecode for that class was generated already.
        byte[] bytecode = (byte[]) this.precompiledClasses.remove(name);
        if (bytecode == null) {

            // Read, scan, parse and compile the right compilation unit.
            {
                Map bytecodes = this.generateBytecodes(name);
                if (bytecodes == null) throw new ClassNotFoundException(name);
                this.precompiledClasses.putAll(bytecodes);
            }

            // Now the bytecode for our class should be available.
            bytecode = (byte[]) this.precompiledClasses.remove(name);
            if (bytecode == null) {
                throw new JaninoRuntimeException(
                    "SNO: Scanning, parsing and compiling class \""
                    + name
                    + "\" did not create a class file!?"
                );
            }
        }

        return this.defineBytecode(name, bytecode);
    }

    /**
     * This {@link Map} keeps those classes which were already compiled, but not
     * yet defined i.e. which were not yet passed to
     * {@link ClassLoader#defineClass(java.lang.String, byte[], int, int)}.
     */
    private Map precompiledClasses = new HashMap(); // String name => byte[] bytecode

    /**
     * Find, scan, parse the right compilation unit. Compile the parsed compilation unit to
     * bytecode. This may cause more compilation units being scanned and parsed. Continue until
     * all compilation units are compiled.
     *
     * @return String name => byte[] bytecode, or <code>null</code> if no source code could be found
     * @throws ClassNotFoundException on compilation problems
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
                        cfs = uc.compileUnit(this.debugSource, this.debugLines, this.debugVars);
                    } catch (CompileException ex) {
                        throw new ClassNotFoundException(
                            "Compiling unit \"" + uc.compilationUnit.optionalFileName + "\"",
                            ex
                        );
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
     * @see #setProtectionDomainFactory
     *
     * @throws ClassFormatError
     */
    private Class defineBytecode(String className, byte[] ba) {

        return this.defineClass(className, ba, 0, ba.length, (
            this.optionalProtectionDomainFactory == null
            ? null
            : this.optionalProtectionDomainFactory.getProtectionDomain(ClassFile.getSourceResourceName(className))
        ));
    }

    private final JavaSourceIClassLoader iClassLoader;

    private boolean debugSource = Boolean.getBoolean(ICookable.SYSTEM_PROPERTY_SOURCE_DEBUGGING_ENABLE);
    private boolean debugLines = this.debugSource;
    private boolean debugVars = this.debugSource;

    /**
     * Collection of parsed, but uncompiled compilation units.
     */
    private final Set unitCompilers = new HashSet(); // UnitCompiler
}
