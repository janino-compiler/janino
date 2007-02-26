
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import junit.framework.*;

import org.codehaus.janino.*;
import org.codehaus.janino.Scanner;

public class EvaluatorTests extends TestCase {
    public static Test suite() {
        TestSuite s = new TestSuite(EvaluatorTests.class.getName());
        s.addTest(new EvaluatorTests("testExpressionEvaluator"));
        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator"));
        s.addTest(new EvaluatorTests("testManyEEs"));
        return s;
    }

    public EvaluatorTests(String name) { super(name); }

    public void testExpressionEvaluator() throws Exception {
        ExpressionEvaluator ee = new ExpressionEvaluator();

        ee.setClassName("Foo");
        ee.setDefaultImports(new String[] { "java.io.*", "for_sandbox_tests.*", });
        ee.setExpressionTypes(new Class[] { ExpressionEvaluator.ANY_TYPE, InputStream.class, void.class, });
        ee.setExtendedType(Properties.class);
        ee.setImplementedTypes(new Class[] { Runnable.class, });
        ee.setMethodNames(new String[] { "a", "b", "run", });
        ee.setParameters(new String[][] { { "a", "b" }, {}, {} }, new Class[][] { { int.class, int.class}, {}, {} });
        ee.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ExternalClass.class });
        ee.setStaticMethod(new boolean[] { false, true, true });
        ee.setThrownExceptions(new Class[][] { {}, { IOException.class }, {} });

        ee.cook(new String[] {
            "a + b",
            "new FileInputStream(\"xyz\")",
            "ExternalClass.m1()",
        });

        {
            Method m = ee.getMethod(0);
            assertEquals(new Integer(5), m.invoke(m.getDeclaringClass().newInstance(), new Object[] { new Integer(2), new Integer(3) }));
        }

        try {
            ee.evaluate(1, new Object[0]);
            fail("Should have thrown an InvocationTargetException");
        } catch (InvocationTargetException ex) {
            assertTrue("FileNotFoundException", ex.getTargetException() instanceof FileNotFoundException);
        }

        ee.evaluate(2, new Object[0]);
    }

    public void testFastClassBodyEvaluator() throws Exception {
        ((Runnable) ClassBodyEvaluator.createFastClassBodyEvaluator(
            new Scanner(null, new StringReader(
                "import java.util.*;\n" +
                "public void run() {\n" +
                "    new ArrayList();\n" +
                "    new other_package.Foo(7);\n" +
                "}\n"
            )),
            Runnable.class,
            Thread.currentThread().getContextClassLoader()
        )).run();
    }

    public void testManyEEs() throws Exception {
        ExpressionEvaluator ee = new ExpressionEvaluator();
        final int COUNT = 10000;

        String[]   expressions = new String[COUNT];
        String[][] parameterNames = new String[COUNT][2];
        Class[][]  parameterTypes = new Class[COUNT][2];
        for (int i = 0; i < expressions.length; ++i) {
            expressions[i] = "a + b";
            parameterNames[i][0] = "a";
            parameterNames[i][1] = "b";
            parameterTypes[i][0] = int.class;
            parameterTypes[i][1] = int.class;
        }
        ee.setParameters(parameterNames, parameterTypes);

        ee.cook(expressions);
        assertEquals(new Integer(165), ee.evaluate(3 * COUNT / 4, new Object[] { new Integer(77), new Integer(88) }));
    }
}
