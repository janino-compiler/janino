
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
 * Sample application which demonstrates how to use the
 * {@link org.codehaus.janino.ExpressionEvaluator ExpressionEvaluator} class.
 */

public class ShippingCost {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: <total>");
            System.err.println("Computes the shipping costs from the double value \"total\".");
            System.err.println("If \"total\" is less than 100.0, then the result is 7.95, else the result is 0.");
            System.exit(1);
        }

        // Convert command line argument to parameter "total".
        Object[] parameterValues = new Object[] { new Double(args[0]) };

        // Create "ExpressionEvaluator" object.
        ExpressionEvaluator ee = new ExpressionEvaluator(
            "total >= 100.0 ? 0.0 : 7.95", // expression
            double.class,                  // optionalExpressionType
            new String[] { "total" },      // parameterNames,
            new Class[] { double.class },  // parameterTypes
            new Class[0],                  // thrownExceptions
            null                           // optionalClassLoader
        );

        // Evaluate expression with actual parameter values.
        Object res = ee.evaluate(parameterValues);

        // Print expression result.
        System.out.println("Result = " + (res == null ? "(null)" : res.toString()));
    }
}
