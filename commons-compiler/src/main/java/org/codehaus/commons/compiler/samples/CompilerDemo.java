
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2019 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.samples;

import java.io.File;
import java.nio.charset.Charset;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.util.StringUtil;

public final
class CompilerDemo {

    private
    CompilerDemo() {}

    /**
     * Command line interface.
     */
    public static void
    main(String[] args) throws Exception {
        File            destinationDirectory  = ICompiler.NO_DESTINATION_DIRECTORY;
        File[]          sourcePath            = new File[0];
        File[]          classPath             = { new File(".") };
        File[]          extDirs               = new File[0];
        File[]          bootClassPath         = new File[0];
        Charset         encoding              = Charset.defaultCharset();
        boolean         verbose               = false;
        boolean         debugSource           = true;
        boolean         debugLines            = true;
        boolean         debugVars             = false;
        boolean         rebuild               = false;

        // Process command line options.
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) != '-') break;
            if ("-d".equals(arg)) {
                destinationDirectory = new File(args[++i]);
            } else
            if ("-sourcepath".equals(arg)) {
                sourcePath = StringUtil.parsePath(args[++i]);
            } else
            if ("-classpath".equals(arg)) {
                classPath = StringUtil.parsePath(args[++i]);
            } else
            if ("-extdirs".equals(arg)) {
                extDirs = StringUtil.parsePath(args[++i]);
            } else
            if ("-bootclasspath".equals(arg)) {
                bootClassPath = StringUtil.parsePath(args[++i]);
            } else
            if ("-encoding".equals(arg)) {
                encoding = Charset.forName(args[++i]);
            } else
            if ("-verbose".equals(arg)) {
                verbose = true;
            } else
            if ("-g".equals(arg)) {
                debugSource = true;
                debugLines  = true;
                debugVars   = true;
            } else
            if (arg.startsWith("-g:")) {
                if (arg.indexOf("none")   != -1) debugSource = (debugLines = (debugVars = false));
                if (arg.indexOf("source") != -1) debugSource = true;
                if (arg.indexOf("lines")  != -1) debugLines = true;
                if (arg.indexOf("vars")   != -1) debugVars = true;
            } else
            if ("-rebuild".equals(arg)) {
                rebuild = true;
            } else
            if ("-help".equals(arg)) {
                System.out.printf(CompilerDemo.USAGE, (Object[]) null);
                System.exit(1);
            } else
            {
                System.err.println("Unrecognized command line option \"" + arg + "\"; try \"-help\".");
                System.exit(1);
            }
        }

        // Get source file names.
        if (i == args.length) {
            System.err.println("No source files given on command line; try \"-help\".");
            System.exit(1);
        }
        File[] sourceFiles = new File[args.length - i];
        for (int j = i; j < args.length; ++j) sourceFiles[j - i] = new File(args[j]);

        // Create the compiler object.
        ICompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory().newCompiler();
        compiler.setSourcePath(sourcePath);
        compiler.setClassPath(classPath);
        compiler.setExtensionDirectories(extDirs);
        compiler.setBootClassPath(bootClassPath);
        compiler.setDestinationDirectory(destinationDirectory);
        compiler.setEncoding(encoding);
        compiler.setVerbose(verbose);
        compiler.setDebugSource(debugSource);
        compiler.setDebugLines(debugLines);
        compiler.setDebugVars(debugVars);
        compiler.setRebuild(rebuild);

        // Compile source files.
        try {
            compiler.compile(sourceFiles);
        } catch (Exception e) {

            if (verbose) {
                e.printStackTrace();
            } else {
                System.err.println(e.toString());
            }

            System.exit(1);
        }
    }

    private static final String USAGE = (
        ""
        + "Usage:%n"
        + "%n"
        + "  java " + Compiler.class.getName() + " [ <option> ] ... <source-file> ...%n"
        + "%n"
        + "Supported <option>s are:%n"
        + "  -d <output-dir>           Where to save class files%n"
        + "  -sourcepath <dirlist>     Where to look for other source files%n"
        + "  -classpath <dirlist>      Where to look for other class files%n"
        + "  -extdirs <dirlist>        Where to look for other class files%n"
        + "  -bootclasspath <dirlist>  Where to look for other class files%n"
        + "  -encoding <encoding>      Encoding of source files, e.g. \"UTF-8\" or \"ISO-8859-1\"%n"
        + "  -verbose%n"
        + "  -g                        Generate all debugging info%n"
        + "  -g:none                   Generate no debugging info%n"
        + "  -g:{source,lines,vars}    Generate only some debugging info%n"
        + "  -rebuild                  Compile all source files, even if the class files%n"
        + "                            seems up-to-date%n"
        + "  -help%n"
        + "%n"
        + "The default encoding in this environment is \"" + Charset.defaultCharset().toString() + "\".%n"
    );

}
