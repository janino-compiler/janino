
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

package org.codehaus.janino.samples;

import org.codehaus.janino.*;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ScriptEvaluator ScriptEvaluator} class.
 */

public class ScriptDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        String   script         = "System.out.println(\"Hello \" + a);";
        Class    returnType     = Void.TYPE;
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
                usage();
                System.exit(0);
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\".");
                usage();
                System.exit(0);
            }
        }

        if (parameterTypes.length != parameterNames.length) {
            System.err.println("Parameter type count and parameter name count do not match.");
            usage();
            System.exit(1);
        }

        // One command line argument for each parameter.
        if (args.length - i != parameterNames.length) {
            System.err.println("Argument and parameter count do not match.");
            usage();
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
