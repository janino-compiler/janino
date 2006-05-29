
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
import java.util.*;

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.resource.*;


/**
 * This {@link org.codehaus.janino.IClassLoader} finds, scans and parses compilation units.
 * <p>
 * Notice that it does not compile them!
 */
final class JavaSourceIClassLoader extends IClassLoader {
    private static final boolean DEBUG = false;

    private final ResourceFinder      sourceFinder;
    private final String              optionalCharacterEncoding;
    private final Set                 unitCompilers; // UnitCompiler
    private UnitCompiler.ErrorHandler optionalCompileErrorHandler = null;
    private WarningHandler            optionalWarningHandler = null;

    /**
     * Notice that the <code>unitCompilers</code> set is both read and written
     * by the {@link JavaSourceIClassLoader}: As it searches for {@link IClass}es, it looks
     * into <code>unitCompilers</code> for class declarations, and as it opens,
     * scans and parses compilation units on-the-fly, it adds them to
     * <code>unitCompilers</code>.
     */
    public JavaSourceIClassLoader(
        ResourceFinder sourceFinder,
        String         optionalCharacterEncoding,
        Set            unitCompilers, // UnitCompiler
        IClassLoader   optionalParentIClassLoader
    ) {
        super(optionalParentIClassLoader);

        this.sourceFinder               = sourceFinder;
        this.optionalCharacterEncoding  = optionalCharacterEncoding;
        this.unitCompilers              = unitCompilers;
        super.postConstruct();
    }

    /**
     * @see UnitCompiler#setCompileErrorHandler(ErrorHandler)
     */
    public void setCompileErrorHandler(UnitCompiler.ErrorHandler optionalCompileErrorHandler) {
        this.optionalCompileErrorHandler = optionalCompileErrorHandler;
    }

    /**
     * @see Parser#setWarningHandler(WarningHandler)
     * @see UnitCompiler#setCompileErrorHandler(ErrorHandler)
     */
    public void setWarningHandler(WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    /**
     * @param type field descriptor of the {@link IClass} to load, e.g. "Lpkg1/pkg2/Outer$Inner;"
     * @throws ClassNotFoundException if an exception was raised while loading the {@link IClass}
     */
    public IClass findIClass(final String type) throws ClassNotFoundException {
        if (JavaSourceIClassLoader.DEBUG) System.out.println("type = " + type);
    
        // Class type.
        String className = Descriptor.toClassName(type); // E.g. "pkg1.pkg2.Outer$Inner"
        if (JavaSourceIClassLoader.DEBUG) System.out.println("2 className = \"" + className + "\"");
    
        // Do not attempt to load classes from package "java".
        if (className.startsWith("java.")) return null;
    
        // Check the already-parsed compilation units.
        for (Iterator it = this.unitCompilers.iterator(); it.hasNext();) {
            UnitCompiler uc = (UnitCompiler) it.next();
            IClass res = uc.findClass(className);
            if (res != null) {
                this.defineIClass(res);
                return res;
            } 
        }

        // Find source file.
        Resource sourceResource = this.sourceFinder.findResource(ClassFile.getSourceResourceName(className));
        if (sourceResource == null) return null;
        if (JavaSourceIClassLoader.DEBUG) System.out.println("sourceURL=" + sourceResource);

        try {

            // Scan and parse the source file.
            InputStream inputStream = sourceResource.open();
            Java.CompilationUnit cu;
            try {
                Scanner scanner = new Scanner(sourceResource.getFileName(), inputStream, this.optionalCharacterEncoding);
                scanner.setWarningHandler(this.optionalWarningHandler);
                Parser parser = new Parser(scanner);
                parser.setWarningHandler(this.optionalWarningHandler);
                cu = parser.parseCompilationUnit();
            } finally {
                try { inputStream.close(); } catch (IOException ex) {}
            }
            UnitCompiler uc = new UnitCompiler(cu, this);
            uc.setCompileErrorHandler(this.optionalCompileErrorHandler);
            uc.setWarningHandler(this.optionalWarningHandler);

            // Remember compilation unit for later compilation.
            this.unitCompilers.add(uc);

            // Find the class/interface declaration in the com
            IClass res = uc.findClass(className);
            if (res == null) throw new Parser.ParseException("Source file \"" + sourceResource.getFileName() + "\" does not declare class \"" + className + "\"", (Location) null);
            this.defineIClass(res);
            return res;
        } catch (Scanner.ScanException e) {
            throw new ClassNotFoundException("Parsing compilation unit \"" + sourceResource + "\"", e);
        } catch (Parser.ParseException e) {
            throw new ClassNotFoundException("Parsing compilation unit \"" + sourceResource + "\"", e);
        } catch (IOException e) {
            throw new ClassNotFoundException("Parsing compilation unit \"" + sourceResource + "\"", e);
        }
    }
}