
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

import java.io.File;
import java.io.IOException;


import org.apache.tools.ant.taskdefs.compilers.*;
import org.apache.tools.ant.types.Path;
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enum.EnumeratorFormatException;

/**
 * A simple {@link CompilerAdapter} for the "ant" tool that silently ignores most of the
 * configuration parameters and attempts to compile all given source files into class files.
 */
public class AntCompilerAdapter extends DefaultCompilerAdapter {

    /**
     * Compile all source files in {@link #compileList} individually and write class
     * files in directory {@link #destDir}.
     * <p>
     * The following fields of {@link DefaultCompilerAdapter} are honored:
     * <ul>
     *   <li>{@link #compileList} - the set of Java<sup>TM</sup> source files to compile
     *   <li>{@link #destDir} - where to store the class files
     *   <li>{@link #compileSourcepath} - where to look for more Java<sup>TM</sup> source files
     *   <li>{@link #compileClasspath} - where to look for required classes
     *   <li>{@link #extdirs}
     *   <li>{@link #bootclasspath}
     *   <li>{@link #encoding} - how the Java<sup>TM</sup> source files are encoded
     *   <li>{@link #verbose}
     *   <li>{@link #debug}
     *   <li>{@link org.apache.tools.ant.taskdefs.Javac#getDebugLevel()}
     *   <li>{@link #src}
     * </ul>
     * The following fields of {@link DefaultCompilerAdapter} are not honored at this time:
     * <ul>
     *   <li>{@link #depend}
     *   <li>{@link #deprecation}
     *   <li>{@link #includeAntRuntime}
     *   <li>{@link #includeJavaRuntime}
     *   <li>{@link #location}
     *   <li>{@link #optimize}
     *   <li>{@link #target}
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


