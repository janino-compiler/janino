
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

package org.codehaus.commons.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.commons.compiler.samples.DemoBase;

/**
 * A test program that allows you to play around with the {@link org.codehaus.janino.ScriptEvaluator ScriptEvaluator}
 * class.
 */
public final
class Jsh extends DemoBase {

    /***/
    public static void
    main(String[] args) throws Exception {

        Class<?>       returnType       = void.class;
        List<String>   parameterNames   = new ArrayList<String>();
        List<Class<?>> parameterTypes   = new ArrayList<Class<?>>();
        List<Class<?>> thrownExceptions = new ArrayList<Class<?>>();
        List<String>   defaultImports   = new ArrayList<String>();
        String         optionalEncoding = null;

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
           if ("--help".equals(arg)) {
               System.err.println("Usage:");
               System.err.println("  Jsh { <option> } <script-file> { <argument> }");
               System.err.println("Valid options are:");
               System.err.println(" --return-type <return-type>         (default: void)");
               System.err.println(" --parameter <type> <name>           (multiple allowed)");
               System.err.println(" --thrown-exception <exception-type> (multiple allowed)");
               System.err.println(" --default-import <imports>          (multiple allowed)");
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
            System.err.println("Script missing on command line; try \"--help\".");
            System.exit(1);
        }
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
        IScriptEvaluator se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
        se.setReturnType(returnType);
        se.setDefaultImports(defaultImports.toArray(new String[0]));
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

    private Jsh() {}
}
