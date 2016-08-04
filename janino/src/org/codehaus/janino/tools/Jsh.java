
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016, Arno Unkrig
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

package org.codehaus.janino.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.samples.DemoBase;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.Java;
import org.codehaus.janino.Mod;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.ScriptEvaluator;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ScriptEvaluator ScriptEvaluator} class.
 */
public final
class Jsh extends DemoBase {

    /***/
    public static void
    main(String[] args) throws Exception {

        Class<?>       returnType        = void.class;
        List<String>   parameterNames    = new ArrayList<String>();
        List<Class<?>> parameterTypes    = new ArrayList<Class<?>>();
        List<Class<?>> thrownExceptions  = new ArrayList<Class<?>>();
        List<String>   defaultImports    = new ArrayList<String>();
        String         optionalEncoding  = null;
        String         compilerFactoryId = null;

        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            if ("--return-type".equals(arg)) {
                returnType = DemoBase.stringToType(args[++i]);
            } else
            if ("--parameter".equals(arg)) {
                parameterTypes.add(DemoBase.stringToType(args[++i]));
                parameterNames.add(args[++i]);
            } else
            if ("--thrown-exception".equals(arg)) {
                thrownExceptions.add(DemoBase.stringToType(args[++i]));
            } else
            if ("--default-import".equals(arg)) {
                defaultImports.add(args[++i]);
            } else
            if ("--encoding".equals(arg)) {
                optionalEncoding = args[++i];
            } else
            if ("--compiler-factory".equals(arg)) {
                compilerFactoryId = args[++i];
            } else
           if ("--help".equals(arg)) {
               System.err.println("Usage:");
               System.err.println("  Jsh { <option> } <script-file> { <argument> }");
               System.err.println("Valid options are:");
               System.err.println(" --return-type <return-type>         (default: void)");
               System.err.println(" --parameter <type> <name>           (multiple allowed)");
               System.err.println(" --thrown-exception <exception-type> (multiple allowed)");
               System.err.println(" --default-import <imports>          (multiple allowed)");
               System.err.println(" --encoding <encoding>");
               System.err.println(" --compiler-factory <id>             (One of " + Arrays.toString(CompilerFactoryFactory.getAllCompilerFactories()) + ")"); // SUPPRESS CHECKSTYLE LineLength
               System.err.println(" --help");
               System.err.println("If no \"--parameter\"s are specified, then the <argument>s are passed as a single");
               System.err.println("parameter \"String[] args\".");
               System.err.println("Otherwise, the number of <argument>s must exactly match the number of");
               System.err.println("parameters, and each <argument> is converted to the respective parameter's");
               System.err.println("type.");
               System.err.println("Iff the return type is not \"void\", then the return value is printed to STDOUT.");
               System.exit(0);
           } else
           {
               System.err.println("Invalid command line option \"" + arg + "\"; try \"--help\".");
               System.exit(1);
           }
        }

        if (i == args.length) {
            if (!parameterTypes.isEmpty()) {
                System.err.println("Parameters are not possible if the script is read from STDIN");
                System.exit(1);
            }
            if (returnType != void.class) {
                System.err.println("Return type not possible if reading from STDIN");
                System.exit(1);
            }
            if (compilerFactoryId != null) {
                System.err.println("Compiler factory by cannot be set if reading from STDIN");
                System.exit(1);
            }

            Jsh.interactiveJsh(thrownExceptions, defaultImports, optionalEncoding);
            return;
        }

        ICompilerFactory compilerFactory;
        COMPILER_FACTORY_BY_ID:
        if (compilerFactoryId != null) {
            for (ICompilerFactory cf : CompilerFactoryFactory.getAllCompilerFactories()) {
                if (cf.getId().equals(compilerFactoryId)) {
                    compilerFactory = cf;
                    break COMPILER_FACTORY_BY_ID;
                }
            }
            System.err.println("Invalid compiler factory id \"" + compilerFactoryId + "\"; try \"--help\".");
            System.exit(1);
            return;
        } else {
            compilerFactory = CompilerFactoryFactory.getDefaultCompilerFactory();
        }

        IScriptEvaluator se = compilerFactory.newScriptEvaluator();
        se.setExtendedClass(JshBase.class);

        se.setReturnType(returnType);
        se.setDefaultImports(defaultImports.toArray(new String[0]));
        se.setThrownExceptions(thrownExceptions.toArray(new Class[0]));

        final File scriptFile = new File(args[i++]);

        Object[] arguments;
        if (parameterTypes.isEmpty()) {

            parameterTypes.add(String[].class);
            parameterNames.add("args");

            arguments = new Object[] { Arrays.copyOfRange(args, i, args.length) };

            if (thrownExceptions.isEmpty()) thrownExceptions.add(Exception.class);
        } else {

            // One command line argument for each parameter.
            if (args.length - i != parameterTypes.size()) {
                System.err.println(
                    "Argument count ("
                    + (args.length - i)
                    + ") and parameter count ("
                    + parameterTypes.size()
                    + ") do not match; try \"--help\"."
                );
                System.exit(1);
            }

            // Convert command line arguments to call arguments.
            arguments = new Object[parameterTypes.size()];
            for (int j = 0; j < arguments.length; ++j) {
                arguments[j] = DemoBase.createObject(parameterTypes.get(j), args[i + j]);
            }
        }

        // Create and configure the "ScriptEvaluator" object.
        se.setParameters(parameterNames.toArray(new String[0]), parameterTypes.toArray(new Class[0]));
        se.setThrownExceptions(thrownExceptions.toArray(new Class[0]));

        // Scan, parse and compile the script file.
        InputStream is = new FileInputStream(scriptFile);
        try {

            se.cook(scriptFile.toString(), is, optionalEncoding);
            is.close();
        } finally {
            try { is.close(); } catch (Exception e) {}
        }

        // Evaluate script with actual parameter values.
        Object res = se.evaluate(arguments);

        // Print script return value.
        if (returnType != void.class) {
            System.out.println(res instanceof Object[] ? Arrays.toString((Object[]) res) : String.valueOf(res));
        }
    }

    private static void
    interactiveJsh(List<Class<?>> thrownExceptions, List<String> defaultImports, @Nullable String optionalEncoding)
    throws IOException {

        // Use than JANINO implementation of IScriptEvaluator, because only that offers the "setMinimal()" feature.
        StatementEvaluator se = new StatementEvaluator();

        se.setExtendedClass(JshBase.class);
        se.setDefaultImports(defaultImports.toArray(new String[0]));

        System.err.println("Welcome, stranger, and speak!");

        for (;;) {

            System.err.println(">");
            System.err.flush();

            // Scan, parse, compile and load one statement.
            try {
                se.cook("stdin", System.in, optionalEncoding);
            } catch (CompileException ce) {
                System.err.println(ce.getLocalizedMessage());
                continue;
            }

            // Evaluate script with actual parameter values.
            try {
                se.execute();
            } catch (Exception e) {
                System.err.println(e.getLocalizedMessage());
                continue;
            }
        }
    }

    private Jsh() {}

    public static
    class JshBase {

        public static Collection<File>
        ls() { return JshBase.ls("."); }

        public static Collection<File>
        ls(String... globs) {
            Collection<File> result = new ArrayList<File>();
            for (String glob : globs) {
                for (File f : JshBase.glob(glob)) result.add(f);
            }
            return result;
        }

        public static Collection<? extends File>
        glob(String glob) {
            Collection<File> result = new ArrayList<File>();
            Jsh.glob(null, glob, result);
            return result;
        }
    }

    private static void
    glob(@Nullable File parent, String glob, Collection<File> result) {

        final String prefix, suffix;
        {
            int idx = glob.indexOf('/');
            if (idx == -1) {
                prefix = glob;
                suffix = null;
            } else {
                prefix = glob.substring(0, idx);
                for (idx++; idx < glob.length() && glob.charAt(idx) == '/'; idx++);
                suffix = idx == glob.length() ? null : glob.substring(idx);
            }
        }

        File[] children;
        if (Jsh.containsWildcards(glob)) {
            children = (parent == null ? new File(".") : parent).listFiles(new FilenameFilter() {

                @Override public boolean
                accept(@Nullable File dir, @Nullable String name) {
                    assert name != null;
                    return Jsh.wildmatch(prefix, name);
                }
            });
        } else {
            children = new File[] { new File(parent, glob) };
        }

        if (suffix == null) {
            result.addAll(Arrays.asList(children));
        } else {
            for (File child : children) Jsh.glob(child, suffix, result);
        }
    }

    private static boolean
    wildmatch(@Nullable String pattern, String text) {

        if (pattern == null) return true;

        int i;
        for (i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            switch (c) {

            case '?':
                if (i == text.length()) return false;
                break;

            case '*':
                if (pattern.length() == i + 1) return true; // Optimization for trailing '*'.
                pattern = pattern.substring(i + 1);
                for (; i <= text.length(); ++i) {
                    if (Jsh.wildmatch(pattern, text.substring(i))) return true;
                }
                return false;

            default:
                if (i == text.length()) return false;
                if (text.charAt(i) != c) return false;
                break;
            }
        }
        return text.length() == i;
    }

    private static boolean
    containsWildcards(@Nullable String pattern) {
        return pattern != null && (pattern.indexOf('*') != -1 || pattern.indexOf('?') != -1);
    }

    /**
     * A variant of {@link ScriptEvaluator} which does not parse until end-of-input, but only one {@link
     * Parser#parseStatement() statement}.
     *
     * @see Parser#parseStatement()
     */
    private static final
    class StatementEvaluator extends ClassBodyEvaluator {

        private static final String METHOD_NAME = "sc";

        @Nullable private Method result; // null=uncooked

        /**
         * Override {@link ClassBodyEvaluator#cook(Scanner)} so that the evaluator does parse a class body, but
         * a stateement.
         */
        @Override public void
        cook(Scanner scanner) throws CompileException, IOException {

            Parser parser = new Parser(scanner);

            // Create a compilation unit.
            Java.CompilationUnit compilationUnit = this.makeCompilationUnit(parser);

            // Add one class declaration.
            final Java.AbstractClassDeclaration
            cd = this.addPackageMemberClassDeclaration(parser.location(), compilationUnit);

            // Add one single-statement method to the class declaration.
            cd.addDeclaredMethod(this.makeMethodDeclaration(parser.location(), parser.parseStatement()));

            // Compile and load the compilation unit.
            Class<?> c = this.compileToClass(compilationUnit);

            // Find the statementmethod by name.
            try {
                this.result = c.getDeclaredMethod(StatementEvaluator.METHOD_NAME);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException((
                    "SNO: Loaded class does not declare method \""
                    + StatementEvaluator.METHOD_NAME
                    + "\""
                ), ex);
            }
        }

        /**
         * To the given {@link Java.AbstractClassDeclaration}, adds
         * <ul>
         *   <li>
         *     A public method declaration with the given return type, name, parameter names and values and thrown
         *     exceptions
         *   </li>
         *   <li>A block</li>
         * </ul>
         *
         * @param returnType Return type of the declared method
         */
        protected Java.MethodDeclarator
        makeMethodDeclaration(Location location, Java.BlockStatement statement) {

            Java.FunctionDeclarator.FormalParameters fps = new Java.FunctionDeclarator.FormalParameters(
                location,
                new Java.FunctionDeclarator.FormalParameter[0],
                false
            );

            return new Java.MethodDeclarator(
                location,                                                        // location
                null,                                                            // optionalDocComment
                new Java.Modifiers(                                              // modifiers
                    (short) (Mod.PUBLIC | Mod.STATIC),
                    new Java.Annotation[0]
                ),
                null,                                                            // optionalTypeParameters
                this.classToType(location, void.class),                          // type
                StatementEvaluator.METHOD_NAME,                                   // name
                fps,                                                             // formalParameters
                new Java.Type[] { this.classToType(location, Exception.class) }, // thrownExceptions
                Collections.singletonList(statement)                             // optionalStatements
            );
        }

        private void
        execute() throws Exception { this.assertCooked().invoke(null); }

        private Method
        assertCooked() {

            if (this.result != null) return this.result;

            throw new IllegalStateException("Must only be called after \"cook()\"");
        }

        private Method
        getMethod() { return this.assertCooked(); }
    }
}
