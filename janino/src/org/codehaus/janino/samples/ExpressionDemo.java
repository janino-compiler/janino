
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
 * {@link org.codehaus.janino.ExpressionEvaluator ExpressionEvaluator} class.
 */

public class ExpressionDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        String   expression             = "total >= 100.0 ? 0.0 : 7.95";
        Class    optionalExpressionType = null;
        String[] parameterNames         = { "total", };
        Class[]  parameterTypes         = { double.class, };
        Class[]  thrownExceptions       = new Class[0];
        String[] optionalDefaultImports = null;

        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            if (arg.equals("-e")) {
                expression = args[++i];
            } else
            if (arg.equals("-et")) {
                optionalExpressionType = DemoBase.stringToType(args[++i]);
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
                ExpressionDemo.usage();
                System.exit(0);
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\".");
                ExpressionDemo.usage();
                System.exit(0);
            }
        }

        if (parameterTypes.length != parameterNames.length) {
            System.err.println("Parameter type count and parameter name count do not match.");
            ExpressionDemo.usage();
            System.exit(1);
        }

        // One command line argument for each parameter.
        if (args.length - i != parameterNames.length) {
            System.err.println("Parameter value count and parameter name count do not match.");
            ExpressionDemo.usage();
            System.exit(1);
        }

        // Convert command line arguments to parameter values.
        Object[] parameterValues = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            parameterValues[j] = DemoBase.createObject(parameterTypes[j], args[i + j]);
        }

        // Create "ExpressionEvaluator" object.
        ExpressionEvaluator ee = new ExpressionEvaluator();
        ee.setReturnType(optionalExpressionType);
        ee.setDefaultImports(optionalDefaultImports);
        ee.setParameters(parameterNames, parameterTypes);
        ee.setThrownExceptions(thrownExceptions);
        ee.cook(expression);

        // Evaluate expression with actual parameter values.
        Object res = ee.evaluate(parameterValues);

        // Print expression result.
        System.out.println("Result = " + DemoBase.toString(res));
    }

    private ExpressionDemo() {}

    private static void usage() {
        System.err.println("Usage:  ExpressionDemo { <option> } { <parameter-value> }");
        System.err.println("Valid options are");
        System.err.println(" -e <expression>");
        System.err.println(" -et <expression-type>");
        System.err.println(" -pn <comma-separated-parameter-names>");
        System.err.println(" -pt <comma-separated-parameter-types>");
        System.err.println(" -te <comma-separated-thrown-exception-types>");
        System.err.println(" -di <comma-separated-default-imports>");
        System.err.println(" -help");
        System.err.println("The number of parameter names, types and values must be identical.");
    }
}
