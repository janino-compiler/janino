
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
import java.io.IOException;

import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Path;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A simple {@link org.apache.tools.ant.taskdefs.compilers.CompilerAdapter} for the "ant" tool that silently ignores
 * most of the configuration parameters and attempts to compile all given source files into class files.
 */
public
class AntCompilerAdapter extends DefaultCompilerAdapter {

    /**
     * Compiles all source files in {@code DefaultCompilerAdapter.compileList} individually and creates class files in
     * the {@code DefaultCompilerAdapter.destDir}.
     * <p>
     * The following fields of {@link DefaultCompilerAdapter} are honored:
     * <ul>
     *   <li>{@code DefaultCompilerAdapter.compileList} - the set of Java source files to compile</li>
     *   <li>{@code DefaultCompilerAdapter.destDir} - where to store the class files</li>
     *   <li>{@code DefaultCompilerAdapter.compileSourcepath} - where to look for more Java source files</li>
     *   <li>{@code DefaultCompilerAdapter.compileClasspath} - where to look for required classes</li>
     *   <li>{@code DefaultCompilerAdapter.extdirs}</li>
     *   <li>{@code DefaultCompilerAdapter.bootclasspath}</li>
     *   <li>{@code DefaultCompilerAdapter.encoding} - how the Java source files are encoded</li>
     *   <li>{@code DefaultCompilerAdapter.verbose}</li>
     *   <li>{@code DefaultCompilerAdapter.debug}</li>
     *   <li>{@code org.apache.tools.ant.taskdefs.Javac.getDebugLevel()}</li>
     *   <li>{@code DefaultCompilerAdapter.src}</li>
     * </ul>
     * <p>
     *   The following fields of {@link DefaultCompilerAdapter} are not honored at this time:
     * </p>
     * <ul>
     *   <li>{@code DefaultCompilerAdapter.depend}</li>
     *   <li>{@code DefaultCompilerAdapter.deprecation}</li>
     *   <li>{@code DefaultCompilerAdapter.includeAntRuntime}</li>
     *   <li>{@code DefaultCompilerAdapter.includeJavaRuntime}</li>
     *   <li>{@code DefaultCompilerAdapter.location}</li>
     *   <li>{@code DefaultCompilerAdapter.optimize}</li>
     *   <li>{@code DefaultCompilerAdapter.target}</li>
     * </ul>
     *
     * @return "true" on success
     */
    @Override public boolean
    execute() {

        // Convert source files into source file names.
        File[] sourceFiles = this.compileList;

        // Determine output directory.
        File destinationDirectory = this.destDir == null ? Compiler.NO_DESTINATION_DIRECTORY : this.destDir;

        // Determine the source path.
        @Nullable File[] optionalSourcePath = AntCompilerAdapter.pathToFiles(
            this.compileSourcepath != null
            ? this.compileSourcepath
            : this.src
        );

        // Determine the class path.
        File[] classPath = AntCompilerAdapter.pathToFiles(this.compileClasspath, new File[] { new File(".") });

        // Determine the ext dirs.
        @Nullable File[] optionalExtDirs = AntCompilerAdapter.pathToFiles(this.extdirs);

        // Determine the boot class path
        @Nullable File[] optionalBootClassPath = AntCompilerAdapter.pathToFiles(this.bootclasspath);

        // Determine the encoding.
        @Nullable String optionalCharacterEncoding = this.encoding;

        // Determine verbosity.
        boolean verbose = this.verbose;

        // Determine debugging information.
        boolean debugSource, debugLines, debugVars;
        if (!this.debug) {
            debugSource = false;
            debugLines  = false;
            debugVars   = false;
        } else {
            String debugLevel = this.attributes.getDebugLevel();
            if (debugLevel == null) {
                debugSource = true;
                debugLines  = true;
                debugVars   = false;
            } else {
                debugSource = false;
                debugLines  = false;
                debugVars   = false;
                if (debugLevel.indexOf("source") != -1) debugSource = true;
                if (debugLevel.indexOf("lines") != -1)  debugLines = true;
                if (debugLevel.indexOf("vars") != -1)   debugVars = true;
            }
        }

        // Compile all source files.
        try {
            new Compiler(
                optionalSourcePath,
                classPath,
                optionalExtDirs,
                optionalBootClassPath,
                destinationDirectory,
                optionalCharacterEncoding,
                verbose,
                debugSource,
                debugLines,
                debugVars,
                Compiler.DEFAULT_WARNING_HANDLE_PATTERNS,
                false                        // rebuild
            ).compile(sourceFiles);
        } catch (CompileException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Converts a {@link org.apache.tools.ant.types.Path} into an array of {@link File}s.
     *
     * @return The converted path, or {@code null} if {@code path} is {@code null}
     */
    @Nullable private static File[]
    pathToFiles(@Nullable Path path) {
        if (path == null) return null;

        String[] fileNames = path.list();
        File[]   files     = new File[fileNames.length];
        for (int i = 0; i < fileNames.length; ++i) files[i] = new File(fileNames[i]);
        return files;
    }

    /**
     * Converts a {@link org.apache.tools.ant.types.Path} into an array of {@link File}s.
     *
     * @return The converted path, or, if {@code path} is {@code null}, the {@code defaultValue}
     */
    private static File[]
    pathToFiles(@Nullable Path path, File[] defaultValue) {

        if (path == null) return defaultValue;

        File[] result = AntCompilerAdapter.pathToFiles(path);
        assert result != null;

        return result;
    }
}


