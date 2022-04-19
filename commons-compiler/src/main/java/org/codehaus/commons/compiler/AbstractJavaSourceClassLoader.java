
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.codehaus.commons.compiler.util.resource.ResourceFinder;
import org.codehaus.commons.nullanalysis.NotNullByDefault;
import org.codehaus.commons.nullanalysis.Nullable;

/**
 * A {@link ClassLoader} that, unlike usual {@link ClassLoader}s, does not load byte code, but reads Java source
 * code and then scans, parses, compiles and loads it into the virtual machine.
 * <p>
 *   As with any {@link ClassLoader}, it is not possible to "update" classes after they've been loaded. The way to
 *   achieve this is to give up on the {@link AbstractJavaSourceClassLoader} and create a new one.
 * </p>
 */
public abstract
class AbstractJavaSourceClassLoader extends ClassLoader {

    /**
     * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
     */
    @Nullable protected ProtectionDomainFactory protectionDomainFactory;

    public
    AbstractJavaSourceClassLoader() {}

    public
    AbstractJavaSourceClassLoader(ClassLoader parentClassLoader) { super(parentClassLoader); }

    // Override and delegate to parent.
    @Override @NotNullByDefault(false) public InputStream
    getResourceAsStream(String name) { return super.getParent().getResourceAsStream(name); }

    /**
     * @param sourcePath The sequence of directories to search for Java source files
     */
    public abstract void setSourcePath(File[] sourcePath);

    /**
     * @param sourceFinder Is used when searching for Java source files
     */
    public abstract void setSourceFinder(ResourceFinder sourceFinder);

    /**
     * @param charsetName if {@code null}, use platform default encoding
     */
    public void
    setSourceFileCharacterEncoding(@Nullable String charsetName) {
        this.setSourceCharset(charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName));
    }

    /**
     * @param charset The character set to using when reading characters from a source file
     */
    public abstract void setSourceCharset(Charset charset);

    /**
     * @param lines  Whether line number debugging information should be generated
     * @param vars   Whether variables debugging information should be generated
     * @param source Whether source file debugging information should be generated
     */
    public abstract void setDebuggingInfo(boolean lines, boolean vars, boolean source);

    /**
     * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
     */
    public final void
    setProtectionDomainFactory(@Nullable ProtectionDomainFactory protectionDomainFactory) {
        this.protectionDomainFactory = protectionDomainFactory;
    }

    /**
     * @see AbstractJavaSourceClassLoader#setProtectionDomainFactory
     */
    public
    interface ProtectionDomainFactory {

        /**
         * @param sourceResourceName E.g. "pkg1/pkg2/Outer.java"
         */
        ProtectionDomain getProtectionDomain(String sourceResourceName);
    }

    /**
     * Reads Java source code for a given class name, scan, parse, compile and load it into the virtual machine,
     * and invoke its "main()" method with the given arguments.
     * <p>
     *   Usage is as follows:
     * </p>
     * <pre>
     *   java {@link AbstractJavaSourceClassLoader} [ <i>option</i> ] ... <i>class-name</i> [ <i>argument</i> ] ...
     *
     *   <i>option</i>:
     *     -sourcepath <i>colon-separated-list-of-source-directories</i>
     *     -encoding <i>character-encoding</i>
     *     -g                           Generate all debugging info
     *     -g:none                      Generate no debugging info
     *     -g:{source,lines,vars}       Generate only some debugging info
     * </pre>
     */
    public static void
    main(String[] args) throws Exception {
        File[]  sourcePath        = null;
        String  characterEncoding = null;

        boolean debuggingInfoLines  = false;
        boolean debuggingInfoVars   = false;
        boolean debuggingInfoSource = false;
        boolean haveDebuggingInfo   = false;

        // Scan command line options.
        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;

            if ("-sourcepath".equals(arg)) {
                sourcePath = AbstractJavaSourceClassLoader.splitPath(args[++i]);
            } else
            if ("-encoding".equals(arg)) {
                characterEncoding = args[++i];
            } else
            if ("-g".equals(arg)) {
                debuggingInfoLines  = true;
                debuggingInfoVars   = true;
                debuggingInfoSource = true;
                haveDebuggingInfo   = true;
            } else
            if ("-g:none".equals(arg)) {
                debuggingInfoLines  = false;
                debuggingInfoVars   = false;
                debuggingInfoSource = false;
                haveDebuggingInfo   = true;
            } else
            if ("-g:".startsWith(arg)) {
                debuggingInfoLines  = arg.indexOf("lines") != -1;
                debuggingInfoVars   = arg.indexOf("vars") != -1;
                debuggingInfoSource = arg.indexOf("source") != -1;
                haveDebuggingInfo   = true;
            } else
            if ("-help".equals(arg)) {
                System.out.println("Usage:");
                System.out.println(
                    "  java "
                    + AbstractJavaSourceClassLoader.class.getName()
                    + " { <option> } <class-name> { <argument> }"
                );
                System.out.println("Loads the named class by name and invoke its \"main(String[])\" method, passing");
                System.out.println("the given <argument>s.");
                System.out.println("  <option>:");
                System.out.println("    -sourcepath <" + File.pathSeparator + "-separated-list-of-source-directories>");
                System.out.println("    -encoding <character-encoding>");
                System.out.println("    -g                     Generate all debugging info");
                System.out.println("    -g:none                Generate no debugging info");
                System.out.println("    -g:{source,lines,vars} Generate only some debugging info");
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
        final String className = args[i++];

        // Determine arguments passed to "main()".
        String[] mainArgs = new String[args.length - i];
        System.arraycopy(args, i, mainArgs, 0, args.length - i);

        // Set up a JavaSourceClassLoader or a CachingJavaSourceClassLoader.
        AbstractJavaSourceClassLoader ajscl = (
            CompilerFactoryFactory.getDefaultCompilerFactory().newJavaSourceClassLoader()
        );
        if (haveDebuggingInfo) ajscl.setDebuggingInfo(debuggingInfoLines, debuggingInfoVars, debuggingInfoSource);
        if (characterEncoding != null) ajscl.setSourceFileCharacterEncoding(characterEncoding);
        if (sourcePath != null) ajscl.setSourcePath(sourcePath);

        // Load the given class.
        Class<?> clazz = ajscl.loadClass(className);

        // Find its "main" method.
        Method mainMethod;
        try {
            mainMethod = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException ex) {
            System.err.println("Class \"" + className + "\" has not public method \"main(String[])\".");
            System.exit(1);
            return; // NEVER REACHED
        }

        // Invoke the "main" method.
        mainMethod.invoke(null, (Object) mainArgs);
    }

    private static File[]
    splitPath(String string) {

        List<File> l = new ArrayList<>();
        for (StringTokenizer st = new StringTokenizer(string, File.pathSeparator); st.hasMoreTokens();) {
            l.add(new File(st.nextToken()));
        }
        return (File[]) l.toArray(new File[l.size()]);
    }
}
