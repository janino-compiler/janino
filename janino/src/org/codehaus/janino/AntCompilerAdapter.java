
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
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

import java.io.File;
import java.io.IOException;


import org.apache.tools.ant.taskdefs.compilers.*;
import org.apache.tools.ant.types.Path;
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.EnumeratorFormatException;

/**
 * A simple {@link org.apache.tools.ant.taskdefs.compilers.CompilerAdapter} for the "ant" tool
 * that silently ignores most of the configuration parameters and attempts to compile all given
 * source files into class files.
 */
public class AntCompilerAdapter extends DefaultCompilerAdapter {

    /**
     * Compile all source files in {@link DefaultCompilerAdapter#compileList} individually and
     * write class files in directory {@link DefaultCompilerAdapter#destDir}.
     * <p>
     * The following fields of {@link DefaultCompilerAdapter} are honored:
     * <ul>
     *   <li>{@link DefaultCompilerAdapter#compileList} - the set of Java<sup>TM</sup> source files to compile
     *   <li>{@link DefaultCompilerAdapter#destDir} - where to store the class files
     *   <li>{@link DefaultCompilerAdapter#compileSourcepath} - where to look for more Java<sup>TM</sup> source files
     *   <li>{@link DefaultCompilerAdapter#compileClasspath} - where to look for required classes
     *   <li>{@link DefaultCompilerAdapter#extdirs}
     *   <li>{@link DefaultCompilerAdapter#bootclasspath}
     *   <li>{@link DefaultCompilerAdapter#encoding} - how the Java<sup>TM</sup> source files are encoded
     *   <li>{@link DefaultCompilerAdapter#verbose}
     *   <li>{@link DefaultCompilerAdapter#debug}
     *   <li>{@link org.apache.tools.ant.taskdefs.Javac#getDebugLevel()}
     *   <li>{@link DefaultCompilerAdapter#src}
     * </ul>
     * The following fields of {@link DefaultCompilerAdapter} are not honored at this time:
     * <ul>
     *   <li>{@link DefaultCompilerAdapter#depend}
     *   <li>{@link DefaultCompilerAdapter#deprecation}
     *   <li>{@link DefaultCompilerAdapter#includeAntRuntime}
     *   <li>{@link DefaultCompilerAdapter#includeJavaRuntime}
     *   <li>{@link DefaultCompilerAdapter#location}
     *   <li>{@link DefaultCompilerAdapter#optimize}
     *   <li>{@link DefaultCompilerAdapter#target}
     * </ul>
     * @return "true" on success
     */
    public boolean execute() {

        // Convert source files into source file names.
        File[] sourceFiles = this.compileList;

        // Determine output directory.
        File optionalDestinationDirectory = this.destDir;

        // Determine the source path.
        File[] optionalSourcePath = AntCompilerAdapter.pathToFiles(
            this.compileSourcepath != null ?
            this.compileSourcepath :
            this.src
        );

        // Determine the class path.
        File[] classPath = AntCompilerAdapter.pathToFiles(this.compileClasspath, new File[] { new File(".") });

        // Determine the ext dirs.
        File[] optionalExtDirs = AntCompilerAdapter.pathToFiles(this.extdirs);

        // Determine the boot class path
        File[] optionalBootClassPath = AntCompilerAdapter.pathToFiles(this.bootclasspath);

        // Determine the encoding.
        String optionalCharacterEncoding = this.encoding;

        // Determine verbosity.
        boolean verbose = this.verbose;

        // Determine debugging information.
        DebuggingInformation debuggingInformation;
        if (!this.debug) {
            debuggingInformation = DebuggingInformation.NONE;
        } else {
            String debugLevel = this.attributes.getDebugLevel();
            if (debugLevel == null) {
                debuggingInformation = DebuggingInformation.LINES.add(DebuggingInformation.SOURCE);
            } else {
                try {
                    debuggingInformation = new DebuggingInformation(debugLevel.toUpperCase());
                } catch (EnumeratorFormatException ex) {
                    debuggingInformation = DebuggingInformation.NONE;
                }
            }
        }

        // Compile all source files.
        try {
            new Compiler(
                optionalSourcePath,
                classPath,
                optionalExtDirs,
                optionalBootClassPath,
                optionalDestinationDirectory,
                optionalCharacterEncoding,
                verbose,
                debuggingInformation,
                (StringPattern[]) null,  // optionalWarningHandlePatterns
                false                    // rebuild
            ).compile(sourceFiles);
        } catch (Scanner.ScanException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (Parser.ParseException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (Java.CompileException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Convert a {@link org.apache.tools.ant.types.Path} into an array of
     * {@link File}.
     * @param path
     * @return The converted path, or <code>null</code> if <code>path</code> is <code>null</code>
     */
    private static File[] pathToFiles(Path path) {
        if (path == null) return null;

        String[] fileNames = path.list();
        File[] files = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; ++i) files[i] = new File(fileNames[i]);
        return files;
    }

    /**
     * Convert a {@link org.apache.tools.ant.types.Path} into an array of
     * {@link File}.
     * @param path
     * @param defaultValue
     * @return The converted path, or, if <code>path</code> is <code>null</code>, the <code>defaultValue</code>
     */
    private static File[] pathToFiles(Path path, File[] defaultValue) {
        if (path == null) return defaultValue;
        return AntCompilerAdapter.pathToFiles(path);
    }
}


