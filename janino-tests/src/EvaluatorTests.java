
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.SimpleCompiler;

import for_sandbox_tests.OverridesWithDifferingVisibility;

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
        s.addTest(new EvaluatorTests("testProtectedAccessAcrossPackages"));

        // The following three are known to fail because of JANINO-113:
//        s.addTest(new EvaluatorTests("testProtectedAccessWithinPackage"));
//        s.addTest(new EvaluatorTests("testComplicatedSyntheticAccess"));
//        s.addTest(new EvaluatorTests("testStaticInitAccessProtected"));
        s.addTest(new EvaluatorTests("testDivByZero"));
        s.addTest(new EvaluatorTests("test32kBranchLimit"));
        s.addTest(new EvaluatorTests("test32kConstantPool"));
        s.addTest(new EvaluatorTests("testHugeIntArray"));
        s.addTest(new EvaluatorTests("testStaticFieldAccess"));
        s.addTest(new EvaluatorTests("testWideInstructions"));
        s.addTest(new EvaluatorTests("testHandlingNaN"));
        s.addTest(new EvaluatorTests("testInstanceOf"));
        s.addTest(new EvaluatorTests("testOverrideVisibility"));
        s.addTest(new EvaluatorTests("testCovariantReturns"));
        s.addTest(new EvaluatorTests("testNonExistentImport"));
        s.addTest(new EvaluatorTests("testAnonymousFieldInitializedByCapture"));
        s.addTest(new EvaluatorTests("testNamedFieldInitializedByCapture"));
        s.addTest(new EvaluatorTests("testAbstractGrandParentsWithCovariantReturns"));
        s.addTest(new EvaluatorTests("testStringBuilderLength"));
        s.addTest(new EvaluatorTests("testBaseClassAccess"));
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
        assertEquals(se2.getMethod(1).invoke(null, new Object[0]), new Double(0.0));
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
                    Class res = (Class)m[i].invoke(inst, new Object[0]);
                    assertEquals(exp[j], res);
                    ++numTests;
                }
            }
        }
        //we count tests just to make sure things didn't go horrifically wrong and
        //the above loops become empty
        assertEquals(8, numTests);
    }

    public void testProtectedAccessAcrossPackages() throws Exception {
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

    public void testProtectedAccessWithinPackage() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ProtectedVariable.class });
        sc.cook("package for_sandbox_tests;\n" +
                "public class Top extends for_sandbox_tests.ProtectedVariable {\n" +
                "    public class Inner {\n" +
                "        public int get() {\n" +
                "            return var;\n" +
                "        }\n" +
                "        public void set() {\n" +
                "            var += 10;\n" +
                "        }\n" +
                "        public int getS() {\n" +
                "            return svar;\n" +
                "        }\n" +
                "        public void setS() {\n" +
                "            svar += 10;\n" +
                "        }\n" +
                "    } \n" +
                "    public Inner createInner() {\n" +
                "        return new Inner();\n" +
                "    }\n" +
                "}"
        );

        Class topClass = sc.getClassLoader().loadClass("for_sandbox_tests.Top");
        Method createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object t = topClass.newInstance();
        Object i = createInner.invoke(t, new Object[0]);

        Class innerClass = sc.getClassLoader().loadClass("for_sandbox_tests.Top$Inner");
        Method get = innerClass.getDeclaredMethod("get", new Class[0]);
        Method getS = innerClass.getDeclaredMethod("getS", new Class[0]);
        Method set = innerClass.getDeclaredMethod("set", new Class[0]);
        Method setS = innerClass.getDeclaredMethod("setS", new Class[0]);

        Object res;
        {   // non-static
            res = get.invoke(i, new Object[0]);
            assertEquals(new Integer(1), res);
            set.invoke(i, new Object[0]);
            res = get.invoke(i, new Object[0]);
            assertEquals(new Integer(11), res);
        }
        {   //static
            res = getS.invoke(i, new Object[0]);
            assertEquals(new Integer(2), res);
            setS.invoke(i, new Object[0]);
            res = getS.invoke(i, new Object[0]);
            assertEquals(new Integer(12), res);
        }
    }

    public void testComplicatedSyntheticAccess() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.setParentClassLoader(SimpleCompiler.BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ProtectedVariable.class });
        sc.cook("package for_sandbox_tests;\n" +
                "public class L0 extends for_sandbox_tests.ProtectedVariable {\n" +
                "    public class L1 extends for_sandbox_tests.ProtectedVariable {\n" +
                "        public class L2 extends for_sandbox_tests.ProtectedVariable {\n" +
                "            public class Inner {\n" +
                "                public int getL2() { return L0.L1.L2.this.var; }\n" +
                "                public int getL1() { return L0.L1.this.var; }\n" +
                "                public int getL0() { return L0.this.var; }\n" +
                "                public int setL2() { return L2.this.var = 2; }\n" +
                "                public int setL1() { return L1.this.var = 1; }\n" +
                "                public int setL0() { return L0.this.var = 0; }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    public L0.L1.L2.Inner createInner() {\n" +
                "        return new L0().new L1().new L2().new Inner();\n" +
                "    }\n" +
                "}"
        );

        Class topClass = sc.getClassLoader().loadClass("for_sandbox_tests.L0");
        Method createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object t = topClass.newInstance();
        Object inner = createInner.invoke(t, new Object[0]);

        Class innerClass = inner.getClass();
        Method[] gets = new Method[] {
                innerClass.getMethod("getL0", new Class[0]),
                innerClass.getMethod("getL1", new Class[0]),
                innerClass.getMethod("getL2", new Class[0]),
        };
        Method[] sets = new Method[] {
                innerClass.getMethod("setL0", new Class[0]),
                innerClass.getMethod("setL1", new Class[0]),
                innerClass.getMethod("setL2", new Class[0]),
        };
        for(int i = 0; i < 3; ++i) {
            Object g1 = gets[i].invoke(inner, new Object[0]);
            assertEquals(new Integer(1), g1);
            Object s1 = sets[i].invoke(inner, new Object[0]);
            assertEquals(new Integer(i), s1);
            Object g2 = gets[i].invoke(inner, new Object[0]);
            assertEquals(new Integer(i), g2);
        }
    }

    public void testStaticInitAccessProtected() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook("package test;\n" +
                "public class Outer extends for_sandbox_tests.ProtectedVariable  {\n" +
                "    public class Inner {\n" +
                "        {\n" +
                "            int t = var;\n" +
                "            var = svar;\n" +
                "            svar = t;\n" +
                "        }\n" +
                "        private final int i = var;\n" +
                "        private final int j = svar;\n" +
                "        {\n" +
                "            int t = var;\n" +
                "            var = svar;\n" +
                "            svar = t;\n" +
                "        }\n" +
                "        private final int[] a = new int[] { i, j };\n" +
                "    }\n" +
                "    public Inner createInner() {\n" +
                "        return new Inner();\n" +
                "    }\n" +
                "}"
        );

        Class topClass = sc.getClassLoader().loadClass("test.Outer");
        Method createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object t = topClass.newInstance();
        assertNotNull(t);
        Object inner = createInner.invoke(t, new Object[0]);
        assertNotNull(inner);
    }

    public void testDivByZero() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(
            "package test;\n" +
            "public class Test {\n" +
            "    public int runIntDiv() {\n" +
            "        return 1 / 0;\n" +
            "    }\n" +
            "    public int runIntMod() {\n" +
            "        return 1 % 0;\n" +
            "    }\n" +
            "    public long runLongDiv() {\n" +
            "        return 1L / 0;\n" +
            "    }\n" +
            "    public long runLongMod() {\n" +
            "        return 1L % 0;\n" +
            "    }\n" +
            "}"
        );


        Class c = sc.getClassLoader().loadClass("test.Test");
        Object o = c.newInstance();

        Method[] m = c.getMethods();
        for(int i = 0; i < m.length; ++i) {
            if(m[i].getName().startsWith("run")) {
                try {
                    Object res = m[i].invoke(o, new Object[0]);
                    fail("Method " + m[i] + " should have failed, but got " + res);
                } catch(InvocationTargetException ae) {
                    assertTrue(ae.getTargetException() instanceof ArithmeticException);
                }
            }
        }
    }

    public static boolean compare(double lhs, double rhs, String comp) {
        if (comp == "==") { return lhs == rhs; }
        if (comp == "!=") { return lhs != rhs; }
        if (comp == "<" ) { return lhs < rhs; }
        if (comp == "<=") { return lhs <= rhs; }
        if (comp == ">" ) { return lhs < rhs; }
        if (comp == ">=") { return lhs <= rhs; }
        throw new RuntimeException("Unsupported comparison");
    }
    public static boolean compare(float lhs, float rhs, String comp) {
        if (comp == "==") { return lhs == rhs; }
        if (comp == "!=") { return lhs != rhs; }
        if (comp == "<" ) { return lhs < rhs; }
        if (comp == "<=") { return lhs <= rhs; }
        if (comp == ">" ) { return lhs < rhs; }
        if (comp == ">=") { return lhs <= rhs; }
        throw new RuntimeException("Unsupported comparison");
    }

    public void testHandlingNaN() throws Exception {
        String prog =
            "package test;\n" +
            "public class Test {\n" +
    "public static boolean compare(double lhs, double rhs, String comp) {" +
        "if (comp == \"==\") { return lhs == rhs; }" +
        "if (comp == \"!=\") { return lhs != rhs; }" +
        "if (comp == \"<\" ) { return lhs < rhs; }" +
        "if (comp == \"<=\") { return lhs <= rhs; }" +
        "if (comp == \">\" ) { return lhs < rhs; }" +
        "if (comp == \">=\") { return lhs <= rhs; }" +
        "throw new RuntimeException(\"Unsupported comparison\");" +
    "}" +
    "public static boolean compare(float lhs, float rhs, String comp) {" +
        "if (comp == \"==\") { return lhs == rhs; }" +
        "if (comp == \"!=\") { return lhs != rhs; }" +
        "if (comp == \"<\" ) { return lhs < rhs; }" +
        "if (comp == \"<=\") { return lhs <= rhs; }" +
        "if (comp == \">\" ) { return lhs < rhs; }" +
        "if (comp == \">=\") { return lhs <= rhs; }" +
        "throw new RuntimeException(\"Unsupported comparison\");" +
    "}" +
            "}";
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(prog);

        Class c = sc.getClassLoader().loadClass("test.Test");
        Method dm = c.getMethod("compare", new Class[] { double.class, double.class, String.class });
        Method fm = c.getMethod("compare", new Class[] { float.class, float.class, String.class });
        Double[][] args = new Double[][] {
                { new Double(Double.NaN), new Double(Double.NaN) },
                { new Double(Double.NaN), new Double(1.0) },
                { new Double(1.0), new Double(Double.NaN) },
                { new Double(1.0), new Double(2.0) },
                { new Double(2.0), new Double(1.0) },
                { new Double(1.0), new Double(1.0) },
        };
        String[] opcode = new String[] { "==", "!=", "<", "<=", ">", ">=" };
        for (int opIdx = 0; opIdx < opcode.length; ++opIdx) {
            for (int argIdx = 0; argIdx < args.length; ++argIdx) {
                String msg = "\""+ args[argIdx][0] +" "+ opcode[opIdx] +" "+ args[argIdx][1] +"\"";
                {
                    boolean exp = compare(args[argIdx][0].doubleValue(), args[argIdx][1].doubleValue(), opcode[opIdx]);
                    Object[] objs = new Object[] { args[argIdx][0], args[argIdx][1], opcode[opIdx] };
                    Object actual = dm.invoke(null, objs);
                    assertEquals(msg, new Boolean(exp), actual);
                }

                {
                    msg = "float: " + msg;
                    boolean exp = compare(args[argIdx][0].floatValue(), args[argIdx][1].floatValue(), opcode[opIdx]);
                    Object[] objs = new Object[] {
                            new Float(args[argIdx][0].floatValue()),
                            new Float(args[argIdx][1].floatValue()),
                            opcode[opIdx]
                    };
                    Object actual = fm.invoke(null, objs);
                    assertEquals(msg, new Boolean(exp), actual);
                }
            }
        }
    }

    public void test32kBranchLimit() throws Exception {
        String preamble =
            "package test;\n" +
            "public class Test {\n" +
            "    public int run() {\n" +
            "        int res = 0;\n" +
            "        for(int i = 0; i < 2; ++i) {\n";
        String middle =
            "            ++res;\n";
        String postamble =
            "        }\n" +
            "        return res;\n" +
            "    }\n" +
            "}";

        int[] tests = new int[] { 1, 10, 100, Short.MAX_VALUE/5, Short.MAX_VALUE/4, Short.MAX_VALUE/2 };
        for(int i = 0; i < tests.length; ++i) {
            int repititions = tests[i];

            StringBuffer sb = new StringBuffer();
            sb.append(preamble);
            for(int j = 0; j < repititions; ++j) {
                sb.append(middle);
            }
            sb.append(postamble);

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(sb.toString());

            Class c = sc.getClassLoader().loadClass("test.Test");
            Method m = c.getDeclaredMethod("run", new Class[0]);
            Object o = c.newInstance();
            Object res = m.invoke(o, new Object[0]);
            assertEquals(new Integer(2*repititions), res);
        }

    }
    public void test32kConstantPool() throws Exception {
        String preamble =
            "package test;\n" +
            "public class Test {\n";
        String postamble =
            "}";


        int[] tests = new int[] { 1, 100, 13020 };
        for(int i = 0; i < tests.length; ++i) {
            int repititions = tests[i];

            StringBuffer sb = new StringBuffer();
            sb.append(preamble);
            for(int j = 0; j < repititions; ++j) {
                sb.append("boolean _v").append(Integer.toString(j)).append(" = false;\n");
            }
            sb.append(postamble);

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(sb.toString());

            Class c = sc.getClassLoader().loadClass("test.Test");
            Object o = c.newInstance();
            assertNotNull(o);
        }
    }


    public void testHugeIntArray() throws Exception {
        String preamble =
            "package test;\n" +
            "public class Test {\n" +
            "    public int[] run() {\n" +
            "        return 1.0 > 2.0 ? null : new int[] {";
        String middle = " 123,";
        String postamble =
            "        };\n" +
            "    }\n" +
            "}";

        int[] tests = new int[] { 1, 10, 8192};
        for(int i = 0; i < tests.length; ++i) {
            int repititions = tests[i];

            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for(int j = 0; j < repititions; ++j) {
                sb.append(middle);
            }
            sb.append(postamble);

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(sb.toString());

            Class c = sc.getClassLoader().loadClass("test.Test");
            Object o = c.newInstance();
            assertNotNull(o);
        }
    }


    public void testStaticFieldAccess() throws Exception {
        assertCompiles(true,
            "package test;\n" +
            "public class Test {\n" +
            "    public static class Inner {\n" +
            "        public static int i = 0;\n" +
            "    }\n" +
            "    public int runTest(Inner in) {\n" +
            "        return in.i;\n" +
            "    }\n" +
            "}"
        );
    }

    public void testWideInstructions() throws Exception {
        String preamble =
            "package test;\n" +
            "public class Test {\n" +
            "    public static String run() {\n";
        String middle =
            "Object o_{0,number,#}; int i_{0,number,#};\n";
        String postamble =
            "        int i = (int)0; ++i; i = (int)(i*i);\n" +
            "        double d = (double)0.0; ++d; d = (double)(d*d);\n" +
            "        float f = (float)0.0; ++f; f = (float)(f*f);\n" +
            "        short s = (short)0; ++s; s = (short)(s*s);\n" +
            "        long l = (long)0; ++l; l = (long)(l*l);\n" +
            "        boolean b = false; b = !b;\n" +
            "        Object o = \"hi\"; o = o.toString();\n" +
            "        String res = o.toString() +\" \"+ i +\" \"+ d +\" \"+  f +\" \"+ s +\" \"+ l +\" \"+ b;\n" +
            "        try { res = res + \" try\"; } finally { res = res + \" finally\"; }\n" +
            "        return res;\n" +
            "    };\n" +
            "}";

        int[] tests = new int[] { 1, 128, };
        for(int i = 0; i < tests.length; ++i) {
            int repititions = tests[i];

            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for(int j = 0; j < repititions; ++j) {
                sb.append(MessageFormat.format(middle, new Object[] { new Integer(j) }));
            }
            sb.append(postamble);

            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(sb.toString());
            Class c = sc.getClassLoader().loadClass("test.Test");
            Method m = c.getDeclaredMethod("run", new Class[0]);
            Object o = m.invoke(null, new Object[0]);
            assertEquals("hi 1 1.0 1.0 1 1 true try finally", o);
        }

    }


    public void testInstanceOf() throws Exception {
        String test =
            "package test;\n" +
            "public class Test {\n" +
            "    public static boolean run(String o) {\n" +
            "        return o instanceof String;" +
            "    };\n" +
            "    public static boolean run(Object o) {\n" +
            "        return o instanceof String;" +
            "    };\n" +
            "    public static boolean run() {\n" +
            "        return null instanceof String;" +
            "    };\n" +
            "}";

        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(test);
        Class c = sc.getClassLoader().loadClass("test.Test");
        Method m0 = c.getDeclaredMethod("run", new Class[] { });
        assertEquals(new Boolean(false), m0.invoke(null, new Object[0]));

        Method mStr = c.getDeclaredMethod("run", new Class[] { String.class });
        Method mObj = c.getDeclaredMethod("run", new Class[] { Object.class });

        assertEquals(new Boolean(true),  mObj.invoke(null, new Object[] { "" }));
        assertEquals(new Boolean(false), mObj.invoke(null, new Object[] { new Integer(1) }));
        assertEquals(new Boolean(false), mObj.invoke(null, new Object[] { null }));

        assertEquals(new Boolean(true),  mStr.invoke(null, new Object[] { "" }));
        assertEquals(new Boolean(false), mStr.invoke(null, new Object[] { null }));
    }

    public void testOverrideVisibility() throws Exception {
        // note that this compiles without problem
        OverridesWithDifferingVisibility.test(new Object[] { "asdf"} );

        // so should this
        assertCompiles(true,
            "package test;\n" +
            "public class Test {\n" +
            "    public void runTest() {\n" +
            "       for_sandbox_tests.OverridesWithDifferingVisibility.test(new Object[] { \"asdf\"} );\n" +
            "    }\n" +
            "}"
        );
    }

    public void testCovariantReturns() throws Exception {
        assertCompiles(true,
                "package test;\n" +
                "public class Test extends for_sandbox_tests.CovariantReturns {\n" +
                "    public Test overrideMe() { return this; }\n" +
                "}"
        );
        assertCompiles(false,
                "package test;\n" +
                "public class Test2 extends for_sandbox_tests.CovariantReturns {\n" +
                "    public Integer overrideMe() { return null; }\n" +
                "}"
        );
    }

    public void testNonExistentImport() throws Exception {
        assertCompiles(false, "import does.not.Exist; public class Test { private final Exist e = null; }");
        assertCompiles(false, "import does.not.Exist; public class Test { }");
    }

    public void testAnonymousFieldInitializedByCapture() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook("public class Top {\n" +
                "  public Runnable get() {\n" +
                "    final String foo = \"foo\";\n" +
                "    final String cow = \"cow\";\n" +
                "    final String moo = \"moo\";\n" +
                "    return new Runnable() {\n" +
                "      public void run() {\n" +
                "        if (bar == null) {\n" +
                "          throw new RuntimeException();\n" +
                "      } }\n" +
                "      private String bar = foo;\n" +
                "      private String[] cowparts = { moo, cow };\n" +
                "    };\n" +
                "} }"
        );

        Class topClass = sc.getClassLoader().loadClass("Top");
        Method get = topClass.getDeclaredMethod("get", new Class[0]);
        Object t = topClass.newInstance();
        Object res = get.invoke(t, new Object[0]);
        ((Runnable)res).run();
    }


    public void testNamedFieldInitializedByCapture() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook("public class Top {\n" +
                "  public Runnable get() {\n" +
                "    final String foo = \"foo\";\n" +
                "    final String cow = \"cow\";\n" +
                "    final String moo = \"moo\";\n" +
                "    class R implements Runnable {\n" +
                "      public void run() {\n" +
                "        if (bar == null) {\n" +
                "          throw new RuntimeException();\n" +
                "      } }\n" +
                "      private String bar = foo;\n" +
                "      private String[] cowparts = { moo, cow };\n" +
                "    }\n" +
                "    return new R();" +
                "} }"
        );

        Class topClass = sc.getClassLoader().loadClass("Top");
        Method get = topClass.getDeclaredMethod("get", new Class[0]);
        Object t = topClass.newInstance();
        Object res = get.invoke(t, new Object[0]);
        ((Runnable)res).run();
    }


    public void testAbstractGrandParentsWithCovariantReturns() throws Exception {
        assertCompiles(true,
                "public class Top {\n" +
                "  private static class IndentPrintWriter extends java.io.PrintWriter { " +
                "    public IndentPrintWriter(java.io.OutputStream os) { super(os); }" +
                "  }" +
                "}"
        );
    }

    public void testStringBuilderLength() throws Exception {
        SimpleCompiler sc = new SimpleCompiler();
        sc.cook("public class Top {\n" +
                "  public int len(StringBuilder sb) { return sb.length(); }" +
                "}"
        );

        Class topClass = sc.getClassLoader().loadClass("Top");
        Method get = topClass.getDeclaredMethod("len", new Class[] { StringBuilder.class });
        Object t = topClass.newInstance();

        StringBuilder sb = new StringBuilder();
        assertEquals(new Integer(sb.length()), get.invoke(t, new Object[] { sb }));
        sb.append("asdf");
        assertEquals(new Integer(sb.length()), get.invoke(t, new Object[] { sb }));
        sb.append("qwer");
        assertEquals(new Integer(sb.length()), get.invoke(t, new Object[] { sb }));
    }



    public void testBaseClassAccess() throws Exception {
        assertCompiles(true,
                "    class top extends other_package.ScopingRules {\n" +
                "        class Inner extends other_package.ScopingRules.ProtectedInner {\n" +
                "            public void test() {\n" +
                "                publicMethod();\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        public void test() {\n" +
                "            Inner i = new Inner();\n" +
                "            i.publicMethod();\n" +
                "        }\n" +
                "    }"
        );
    }

    public SimpleCompiler assertCompiles(boolean shouldCompile, CharSequence prog) throws Exception {
        try {
            SimpleCompiler sc = new SimpleCompiler();
            sc.cook(prog.toString());
            assertTrue("Compilation should have failed for:\n" + prog, shouldCompile);
            return sc;
        } catch (CompileException ce) {
            if (shouldCompile) {
                ce.printStackTrace();
            }
            assertFalse("Compilation should have succeeded for:\n" + prog, shouldCompile);
        }
        return null;
    }
}
