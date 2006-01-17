
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

/**
 * A simplified version of {@link Compiler} that can compile only a single
 * compilation unit. (A "compilation unit" is the characters stored in a
 * ".java" file.)
 * <p>
 * Opposed to a normal ".java" file, you can declare multiple public classes
 * here.
 */
public class SimpleCompiler extends EvaluatorBase {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("    org.codehaus.janino.SimpleCompiler <class-name> <arg> [ ... ]");
            System.err.println("Reads a compilation unit from STDIN and invokes method \"public static void main(String[])\" of");
            System.err.println("class <class-name>.");
            System.exit(1);
        }

        String className = args[0];
        String[] mainArgs = new String[args.length - 1];
        System.arraycopy(args, 1, mainArgs, 0, mainArgs.length);

        ClassLoader cl = new SimpleCompiler("STDIN", System.in).getClassLoader();
        Class c = cl.loadClass(className);
        Method m = c.getMethod("main", new Class[] { String[].class });
        m.invoke(null, new Object[] { mainArgs });
    }

    public SimpleCompiler(
        String optionalFileName,
        Reader in
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(optionalFileName, in), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String      optionalFileName,
        InputStream is
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(optionalFileName, is), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String fileName
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        this(
            new Scanner(fileName), // scanner
            (ClassLoader) null     // optionalParentClassLoader
        );
    }

    /**
     * Parse a compilation unit from the given {@link Scanner} object and
     * compile it to a set of Java<sup>TM</sup> classes.
     * 
     * @param scanner Source of tokens
     * @param optionalParentClassLoader Loads referenced classes
     */
    public SimpleCompiler(
        Scanner     scanner,
        ClassLoader optionalParentClassLoader
    ) throws IOException, Scanner.ScanException, Parser.ParseException, CompileException {
        super(optionalParentClassLoader);

        // Parse the compilation unit.
        Java.CompilationUnit compilationUnit = new Parser(scanner).parseCompilationUnit();

        // Compile the classes and load them.
        this.classLoader = this.compileAndLoad(
            compilationUnit,
            DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION
        );
    }

    /**
     * Returns a {@link ClassLoader} object through which the previously compiled classes can
     * be accessed. This {@link ClassLoader} can be used for subsequent calls to
     * {@link #SimpleCompiler(Scanner, ClassLoader)} in order to compile compilation units that
     * use types (e.g. declare derived types) declared in the previous one.
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    private final ClassLoader classLoader;
}
