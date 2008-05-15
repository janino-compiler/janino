
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
        s.addTest(new EvaluatorTests("testMultiScriptEvaluator"));
        s.addTest(new EvaluatorTests("testExpressionEvaluator"));
        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator1"));
        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator2"));
        s.addTest(new EvaluatorTests("testFastExpressionEvaluator"));
        s.addTest(new EvaluatorTests("testManyEEs"));
        s.addTest(new EvaluatorTests("testGuessParameterNames"));
        s.addTest(new EvaluatorTests("testAssertNotCooked"));
        s.addTest(new EvaluatorTests("testAccessingCompilingClass"));
        s.addTest(new EvaluatorTests("testProtectedAccessToParentSuperClassVar"));
        return s;
    }

    public EvaluatorTests(String name) { super(name); }

    public void testMultiScriptEvaluator() throws Exception {
        String funct2 = "return a + b;";
        String funct3 = "return 0;";
        ScriptEvaluator se2 = new ScriptEvaluator();
        se2.setReturnTypes(new Class[] { double.class , double.class});
        se2.setMethodNames(new String[] { "funct2", "funct3" });
        String[][] params2 = { {"a", "b"}, {} };
        Class[][] paramsType2 = { {double.class, double.class}, {} };
        se2.setParameters(params2, paramsType2);
        se2.setStaticMethod(new boolean[] { true, true });
        se2.cook(new String[] {funct2, funct3});
        assertEquals(se2.getMethod(0).invoke(null, new Object[] { new Double(3.0), new Double(4.0) }), new Double(7.0));
        assertEquals(se2.getMethod(1).invoke(null, null), new Double(0.0));
    }
    
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

    public void testFastClassBodyEvaluator1() throws Exception {
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

    public void testFastClassBodyEvaluator2() throws Exception {
        try {
            ((Runnable) ClassBodyEvaluator.createFastClassBodyEvaluator(
                new Scanner(null, new StringReader(
                    "public void m() { // Implement \"m()\" instead of \"run()\".\n" +
                    "    System.out.println(\"Got here\");\n" +
                    "}"
                )),
                Runnable.class,
                Thread.currentThread().getContextClassLoader()
            )).run();
            fail("CompileException expected");
        } catch (CompileException ex) {
            ;
        }
    }
    
    public void testFastExpressionEvaluator() throws Exception {
        ((Comparable) ExpressionEvaluator.createFastExpressionEvaluator(
            "o == null ? 3 : 4",  // expression
            Comparable.class,     // interfaceToImplement
            new String[] { "o" }, // parameterNames
            null                  // optionalParentClassLoader
        )).compareTo("");
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

    public void testGuessParameterNames() throws Exception {
        Set parameterNames = new HashSet(Arrays.asList(ExpressionEvaluator.guessParameterNames(new Scanner(null, new StringReader(
            "import o.p;\n" +
            "a + b.c + d.e() + f() + g.h.I.j() + k.l.M"
        )))));
        assertEquals(new HashSet(Arrays.asList(new String[] { "a", "b", "d" })), parameterNames);

        parameterNames = new HashSet(Arrays.asList(ScriptEvaluator.guessParameterNames(new Scanner(null, new StringReader(
            "import o.p;\n" +
            "int a;\n" +
            "return a + b.c + d.e() + f() + g.h.I.j() + k.l.M;"
        )))));
        assertEquals(new HashSet(Arrays.asList(new String[] { "b", "d" })), parameterNames);
    }

    public void testAssertNotCooked() throws Exception {
        ClassBodyEvaluator temp = new ClassBodyEvaluator("");
        try {
            temp.setExtendedType(String.class); // Must throw an ISE because the CBS is already cooked.
        } catch (IllegalStateException ex) {
            return;
        }
        fail();
    }
    
    public void testAccessingCompilingClass() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook("package test.simple;\n" +
                "public class L0 {\n" +
                "    public static class L1 {\n" +
                "        public static class L2 { }\n" +
                "    } \n" +
                "    public Class getL0_1() {\n" +
                "        return L0.class;\n" +
                "    }\n" +
                "    public Class getL0_2() {\n" +
                "        return test.simple.L0.class;\n" +
                "    }\n" +
                "    public Class getL1_1() {\n" +
                "        return L1.class;\n" +
                "    }\n" +
                "    public Class getL1_2() {\n" +
                "        return L0.L1.class;\n" +
                "    }\n" +
                "    public Class getL1_3() {\n" +
                "        return test.simple.L0.L1.class;\n" +
                "    }\n" +
                "    public Class getL2_1() {\n" +
                "        return L1.L2.class;\n" +
                "    }\n" +
                "    public Class getL2_2() {\n" +
                "        return L0.L1.L2.class;\n" +
                "    }\n" +
                "    public Class getL2_3() {\n" +
                "        return test.simple.L0.L1.L2.class;\n" +
                "    }\n" +
                "}"
        );
        Class[] exp = new Class[] {
                sc.getClassLoader().loadClass("test.simple.L0"),
                sc.getClassLoader().loadClass("test.simple.L0$L1"),
                sc.getClassLoader().loadClass("test.simple.L0$L1$L2"),
        };
        
        
        Method[] m = exp[0].getMethods();
        Object inst = exp[0].newInstance();
        int numTests = 0;
        for(int i = 0; i < m.length; ++i) {
            for(int j = 0; j < exp.length; ++j) {
                if(m[i].getName().startsWith("getL"+j)) {
                    Class res = (Class)m[i].invoke(inst, null);
                    assertEquals(exp[j], res);
                    ++numTests;
                }
            }
        }
        //we count tests just to make sure things didn't go horrifically wrong and
        //the above loops become empty
        assertEquals(8, numTests);
    }
    
    public void testProtectedAccessToParentSuperClassVar() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ProtectedVariable.class });
        sc.cook("package test;\n" +
                "public class Top extends for_sandbox_tests.ProtectedVariable {\n" +
                "    public class Inner {\n" +
                "        public int get() {\n" +
                "            return var;\n" +
                "        }\n" +
                "    } \n" +
                "}"
        );
    }
}
