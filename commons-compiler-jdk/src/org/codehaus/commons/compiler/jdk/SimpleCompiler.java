
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
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

package org.codehaus.commons.compiler.jdk;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

import org.codehaus.commons.compiler.*;

public class SimpleCompiler extends Cookable implements ISimpleCompiler {

    private ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
    private ClassLoader result;

    @Override
    public ClassLoader getClassLoader() {
        if (this.result == null) throw new IllegalStateException("Must only be called after \"cook()\"");
        return this.result;
    }

    @Override
    public void cook(
        String       optionalFileName,
        final Reader r
    ) throws CompileException, ParseException, ScanException, IOException {

        // Create one Java source file in memory, which will be compiled later.
        JavaFileObject compilationUnit;
        {
            URI uri;
            try {
                uri = new URI("simplecompiler");
            } catch (URISyntaxException use) {
                throw new RuntimeException(use);
            }
            compilationUnit = new SimpleJavaFileObject(uri, Kind.SOURCE) {

                @Override
                public boolean isNameCompatible(String simpleName, Kind kind) {
                    return true;
                }

                @Override
                public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
                    return r;
                }

                @Override
                public CharSequence getCharContent(
                    boolean ignoreEncodingErrors
                ) throws IOException {
                    return readString(this.openReader(ignoreEncodingErrors));
                }
            };
        }

        // Find the JDK Java compiler.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompileException(
                "JDK Java compiler not available - probably you're running a JRE, not a JDK",
                null
            );
        }

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        JavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Wrap it so that the output files (in our case class files) are stored in memory rather
        // than in files.
        fileManager = new ByteArrayJavaFileManager<JavaFileManager>(fileManager);

        // Run the compiler.
        try {
            if (!compiler.getTask(
                null,                                  // out
                fileManager,                           // fileManager
                new DiagnosticListener<JavaFileObject>() { // diagnosticListener

                    @Override
                    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
//System.err.println("*** " + diagnostic.toString() + " *** " + diagnostic.getCode());

                        Location loc = new Location(
                            diagnostic.getSource().toString(),
                            (short) diagnostic.getLineNumber(),
                            (short) diagnostic.getColumnNumber()
                        );
                        String code = diagnostic.getCode();
                        String message = diagnostic.getMessage(null) + " (" + code + ")";

                        // The JDK compiler does not distinguish between scan, parse and compile
                        // errors, thus we determine the error type heuristically through the
                        // diagnostic code.
                        LocatedException cause = (
                            PARSE_ERROR_CODES.contains(code)
                            ? new ParseException(message, loc)
                            : SCAN_ERROR_CODES.contains(code)
                            ? new ScanException(message, loc)
                            : new CompileException(message, loc)
                        );

                        // Wrap the exception in a RuntimeException, because "report()" does not
                        // declare checked exceptions.
                        throw new RuntimeException(cause);
                    }
                },
                null,                                  // options
                null,                                  // classes
                Collections.singleton(compilationUnit) // compilationUnits
            ).call()) {
                throw new CompileException("Compilation failed", null);
            }
        } catch (RuntimeException rte) {

            // Unwrap the compilation exception and throw it.
            Throwable cause = rte.getCause();
            if (cause != null) {
                cause = cause.getCause();
                if (cause instanceof CompileException) throw (CompileException) cause;
                if (cause instanceof ParseException)   throw (ParseException)   cause;
                if (cause instanceof ScanException)    throw (ScanException)    cause;
                if (cause instanceof IOException)      throw (IOException)      cause;
            }
            throw rte;
        }

        // Create a ClassLoader that reads class files from our FM.
        this.result = new JavaFileManagerClassLoader(fileManager, this.parentClassLoader);
    }

    protected void cook(
        JavaFileObject compilationUnit
    ) throws CompileException, ParseException, ScanException, IOException {

        // Find the JDK Java compiler.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompileException(
                "JDK Java compiler not available - probably you're running a JRE, not a JDK",
                null
            );
        }

        // Get the original FM, which reads class files through this JVM's BOOTCLASSPATH and
        // CLASSPATH.
        JavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Wrap it so that the output files (in our case class files) are stored in memory rather
        // than in files.
        fileManager = new ByteArrayJavaFileManager<JavaFileManager>(fileManager);

        // Run the compiler.
        try {
            if (!compiler.getTask(
                null,                                  // out
                fileManager,                           // fileManager
                new DiagnosticListener<JavaFileObject>() { // diagnosticListener

                    @Override
                    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                        System.err.println("*** " + diagnostic.toString() + " *** " + diagnostic.getCode());

                        Location loc = new Location(
                            diagnostic.getSource().toString(),
                            (short) diagnostic.getLineNumber(),
                            (short) diagnostic.getColumnNumber()
                        );
                        String code = diagnostic.getCode();
                        String message = diagnostic.getMessage(null) + " (" + code + ")";

                        // The JDK compiler does not distinguish between scan, parse and compile
                        // errors, thus we determine the error type heuristically through the
                        // diagnostic code.
                        LocatedException cause = (
                            PARSE_ERROR_CODES.contains(code)
                            ? new ParseException(message, loc)
                            : SCAN_ERROR_CODES.contains(code)
                            ? new ScanException(message, loc)
                            : new CompileException(message, loc)
                        );

                        // Wrap the exception in a RuntimeException, because "report()" does not
                        // declare checked exceptions.
                        throw new RuntimeException(cause);
                    }
                },
                null,                                  // options
                null,                                  // classes
                Collections.singleton(compilationUnit) // compilationUnits
            ).call()) {
                throw new CompileException("Compilation failed", null);
            }
        } catch (RuntimeException rte) {

            // Unwrap the compilation exception and throw it.
            Throwable cause = rte.getCause();
            if (cause != null) {
                cause = cause.getCause();
                if (cause instanceof CompileException) throw (CompileException) cause;
                if (cause instanceof ParseException)   throw (ParseException)   cause;
                if (cause instanceof ScanException)    throw (ScanException)    cause;
                if (cause instanceof IOException)      throw (IOException)      cause;
            }
            throw rte;
        }

        // Create a ClassLoader that reads class files from our FM.
        this.result = new JavaFileManagerClassLoader(fileManager, this.parentClassLoader);
    }

    static final Set<String> PARSE_ERROR_CODES = new HashSet<String>(Arrays.asList(
        "compiler.err.not.stmt",
        "compiler.err.expected",
        "compiler.err.illegal.start.of.expr"
    ));
    static final Set<String> SCAN_ERROR_CODES = new HashSet<String>(Arrays.asList(
        "compiler.err.illegal.char",
        "compiler.err.unclosed.comment",
        "compiler.err.int.number.too.large"
    ));

    @Override
    public void setParentClassLoader(ClassLoader optionalParentClassLoader) {
        assertNotCooked();
        this.parentClassLoader = (
            optionalParentClassLoader != null
            ? optionalParentClassLoader
            : Thread.currentThread().getContextClassLoader()
        );
    }

    /**
     * Auxiliary classes never really worked... don't use them.
     *
     * @param optionalParentClassLoader
     * @param auxiliaryClasses
     */
    @Deprecated
    public void setParentClassLoader(ClassLoader optionalParentClassLoader, Class<?>[] auxiliaryClasses) {
        this.setParentClassLoader(optionalParentClassLoader);
    }

    /**
     * Throw an {@link IllegalStateException} if this {@link Cookable} is already cooked.
     */
    protected void assertNotCooked() {
        if (this.result != null) throw new IllegalStateException("Already cooked");
    }
}
