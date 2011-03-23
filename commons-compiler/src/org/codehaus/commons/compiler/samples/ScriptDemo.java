
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

package org.codehaus.commons.compiler.samples;

import org.codehaus.commons.compiler.*;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ScriptEvaluator ScriptEvaluator} class.
 */
public final class ScriptDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        Class    returnType             = void.class;
        String[] parameterNames         = {};
        Class[]  parameterTypes         = {};
        Class[]  thrownExceptions       = {};
        String[] optionalDefaultImports = null;

        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            if (arg.equals("-rt")) {
                returnType = DemoBase.stringToType(args[++i]);
            } else
            if (arg.equals("-pn")) {
                parameterNames = DemoBase.explode(args[++i]);
            } else
            if (arg.equals("-pt")) {
                parameterTypes = DemoBase.stringToTypes(args[++i]);
            } else
            if (arg.equals("-te")) {
                thrownExceptions = DemoBase.stringToTypes(args[++i]);
            } else
            if (arg.equals("-di")) {
                optionalDefaultImports = DemoBase.explode(args[++i]);
            } else
           if (arg.equals("-help")) {
               System.err.println("Usage:");
               System.err.println("  ScriptDemo { <option> } <script> { <parameter-value> }");
               System.err.println("Valid options are");
               System.err.println(" -rt <return-type>                            (default: void)");
               System.err.println(" -pn <comma-separated-parameter-names>        (default: none)");
               System.err.println(" -pt <comma-separated-parameter-types>        (default: none)");
               System.err.println(" -te <comma-separated-thrown-exception-types> (default: none)");
               System.err.println(" -di <comma-separated-default-imports>        (default: none)");
               System.err.println(" -help");
               System.err.println("The number of parameter names, types and values must be identical.");
               System.exit(0);
           } else
           {
               System.err.println("Invalid command line option \"" + arg + "\"; try \"-help\".");
               System.exit(1);
           }
        }

        if (i == args.length) {
            System.err.println("Script missing on command line; try \"-help\".");
            System.exit(1);
        }
        String script = args[i++];

        if (parameterTypes.length != parameterNames.length) {
            System.err.println("Parameter type count and parameter name count do not match; try \"-help\".");
            System.exit(1);
        }

        // One command line argument for each parameter.
        if (args.length - i != parameterNames.length) {
            System.err.println("Argument and parameter count do not match; try \"-help\".");
            System.exit(1);
        }

        // Convert command line arguments to call arguments.
        Object[] arguments = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            arguments[j] = DemoBase.createObject(parameterTypes[j], args[i + j]);
        }

        // Create "ScriptEvaluator" object.
        IScriptEvaluator se = CompilerFactoryFactory.getDefaultCompilerFactory().newScriptEvaluator();
        se.setReturnType(returnType);
        se.setDefaultImports(optionalDefaultImports);
        se.setParameters(parameterNames, parameterTypes);
        se.setThrownExceptions(thrownExceptions);
        se.cook(script);

        // Evaluate script with actual parameter values.
        Object res = se.evaluate(arguments);

        // Print script return value.
        System.out.println("Result = " + DemoBase.toString(res));
    }

    private ScriptDemo() {}
}
