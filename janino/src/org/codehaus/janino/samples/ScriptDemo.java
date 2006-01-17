
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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

package org.codehaus.janino.samples;

import org.codehaus.janino.*;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ScriptEvaluator ScriptEvaluator} class.
 */

public class ScriptDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        String   script         = "System.out.println(\"Hello \" + a);";
        Class    returnType     = void.class;
        String[] parameterNames = { "a", };
        Class[]  parameterTypes = { String.class, };

        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            if (arg.equals("-s")) {
                script = args[++i];
            } else
            if (arg.equals("-rt")) {
                returnType = DemoBase.stringToType(args[++i]);
            } else
            if (arg.equals("-pn")) {
                parameterNames = DemoBase.explode(args[++i]);
            } else
            if (arg.equals("-pt")) {
                parameterTypes = DemoBase.stringToTypes(args[++i]);
            } else
            if (arg.equals("-help")) {
                ScriptDemo.usage();
                System.exit(0);
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\".");
                ScriptDemo.usage();
                System.exit(0);
            }
        }

        if (parameterTypes.length != parameterNames.length) {
            System.err.println("Parameter type count and parameter name count do not match.");
            ScriptDemo.usage();
            System.exit(1);
        }

        // One command line argument for each parameter.
        if (args.length - i != parameterNames.length) {
            System.err.println("Argument and parameter count do not match.");
            ScriptDemo.usage();
            System.exit(1);
        }

        // Convert command line arguments to parameter values.
        Object[] parameterValues = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            parameterValues[j] = DemoBase.createObject(parameterTypes[j], args[i + j]);
        }

        // Create "ScriptEvaluator" object.
        ScriptEvaluator se = new ScriptEvaluator(
            script,
            returnType,
            parameterNames,
            parameterTypes
        );

        // Evaluate script with actual parameter values.
        Object res = se.evaluate(parameterValues);

        // Print script return value.
        System.out.println("Result = " + (res == null ? "(null)" : res.toString()));
    }

    private ScriptDemo() {}

    private static void usage() {
        System.err.println("Usage:  ScriptDemo { <option> } { <parameter-value> }");
        System.err.println("Valid options are");
        System.err.println(" -s <script>");
        System.err.println(" -rt <return-type>");
        System.err.println(" -pn <comma-separated-parameter-names>");
        System.err.println(" -pt <comma-separated-parameter-types>");
        System.err.println(" -help");
        System.err.println("The number of parameter names, types and values must be identical.");
    }
}
