
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
import java.lang.reflect.Method;

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
    ) throws IOException, Scanner.ScanException, Parser.ParseException, Java.CompileException {
        this(
            new Scanner(optionalFileName, in), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String      optionalFileName,
        InputStream is
    ) throws IOException, Scanner.ScanException, Parser.ParseException, Java.CompileException {
        this(
            new Scanner(optionalFileName, is), // scanner
            (ClassLoader) null                 // optionalParentClassLoader
        );
    }

    public SimpleCompiler(
        String fileName
    ) throws IOException, Scanner.ScanException, Parser.ParseException, Java.CompileException {
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
    ) throws IOException, Scanner.ScanException, Parser.ParseException, Java.CompileException {
        super(optionalParentClassLoader);

        // Parse the compilation unit.
        Java.CompilationUnit compilationUnit = new Parser(scanner).parseCompilationUnit();

        // Compile the classes and load them.
        this.classLoader = this.compileAndLoad(
            compilationUnit,
            DebuggingInformation.SOURCE.add(DebuggingInformation.LINES)
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
