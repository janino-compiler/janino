
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

import java.io.*;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.commons.compiler.*;

import for_sandbox_tests.OverridesWithDifferingVisibility;

public class EvaluatorTests extends TestCase {

    private final ICompilerFactory compilerFactory;

//    public static final ClassLoader BOOT_CLASS_LOADER = new ClassLoader(null) {};

    public static TestSuite suite(ICompilerFactory compilerFactory) {
        TestSuite s = new TestSuite("EvaluatorTests");

        s.addTest(new EvaluatorTests("testMultiScriptEvaluator", compilerFactory));
        s.addTest(new EvaluatorTests("testExpressionEvaluator", compilerFactory));
        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator1", compilerFactory));
        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator2", compilerFactory));
        s.addTest(new EvaluatorTests("testFastExpressionEvaluator", compilerFactory));
        s.addTest(new EvaluatorTests("testManyEEs", compilerFactory));
//        s.addTest(new EvaluatorTests("testGuessParameterNames", compilerFactory));
        s.addTest(new EvaluatorTests("testAssertNotCooked", compilerFactory));
        s.addTest(new EvaluatorTests("testAccessingCompilingClass", compilerFactory));
        s.addTest(new EvaluatorTests("testProtectedAccessAcrossPackages", compilerFactory));

        // The following three are known to fail because of JANINO-113:
//        s.addTest(new EvaluatorTests("testProtectedAccessWithinPackage", compilerFactory));
//        s.addTest(new EvaluatorTests("testComplicatedSyntheticAccess", compilerFactory));
//        s.addTest(new EvaluatorTests("testStaticInitAccessProtected", compilerFactory));
        s.addTest(new EvaluatorTests("testDivByZero", compilerFactory));
        s.addTest(new EvaluatorTests("test32kBranchLimit", compilerFactory));
        s.addTest(new EvaluatorTests("test32kConstantPool", compilerFactory));
        s.addTest(new EvaluatorTests("testHugeIntArray", compilerFactory));
        s.addTest(new EvaluatorTests("testStaticFieldAccess", compilerFactory));
        s.addTest(new EvaluatorTests("testWideInstructions", compilerFactory));
        s.addTest(new EvaluatorTests("testHandlingNaN", compilerFactory));
        s.addTest(new EvaluatorTests("testInstanceOf", compilerFactory));
        s.addTest(new EvaluatorTests("testOverrideVisibility", compilerFactory));
        s.addTest(new EvaluatorTests("testCovariantReturns", compilerFactory));
        s.addTest(new EvaluatorTests("testNonExistentImport", compilerFactory));
        s.addTest(new EvaluatorTests("testAnonymousFieldInitializedByCapture", compilerFactory));
        s.addTest(new EvaluatorTests("testNamedFieldInitializedByCapture", compilerFactory));
        s.addTest(new EvaluatorTests("testAbstractGrandParentsWithCovariantReturns", compilerFactory));
        s.addTest(new EvaluatorTests("testStringBuilderLength", compilerFactory));
        s.addTest(new EvaluatorTests("testBaseClassAccess", compilerFactory));
        return s;
    }

    public EvaluatorTests(String name, ICompilerFactory compilerFactory) {
        super(name);
        this.compilerFactory = compilerFactory;
    }

    public void testMultiScriptEvaluator() throws Exception {
        String funct2 = "return a + b;";
        String funct3 = "return 0;";
        IScriptEvaluator se2 = compilerFactory.newScriptEvaluator();
        se2.setReturnTypes(new Class[] { double.class , double.class});
        se2.setMethodNames(new String[] { "funct2", "funct3" });
        String[][] params2 = { {"a", "b"}, {} };
        Class<?>[][] paramsType2 = { {double.class, double.class}, {} };
        se2.setParameters(params2, paramsType2);
        se2.setStaticMethod(new boolean[] { true, true });
        se2.cook(new String[] {funct2, funct3});
        assertEquals(se2.getMethod(0).invoke(null, new Object[] { new Double(3.0), new Double(4.0) }), new Double(7.0));
        assertEquals(se2.getMethod(1).invoke(null, new Object[0]), new Double(0.0));
    }

    public void testExpressionEvaluator() throws Exception {
        IExpressionEvaluator ee = compilerFactory.newExpressionEvaluator();

        ee.setClassName("Foo");
        ee.setDefaultImports(new String[] { "java.io.*", "for_sandbox_tests.*", });
        ee.setExpressionTypes(new Class[] { IExpressionEvaluator.ANY_TYPE, InputStream.class, void.class, });
        ee.setExtendedClass(Properties.class);
        ee.setImplementedInterfaces(new Class[] { Runnable.class, });
        ee.setMethodNames(new String[] { "a", "b", "run", });
        ee.setParameters(new String[][] { { "a", "b" }, {}, {} }, new Class[][] { { int.class, int.class}, {}, {} });
//        ee.setParentClassLoader(BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ExternalClass.class });
        ee.setStaticMethod(new boolean[] { false, true, true });
        ee.setThrownExceptions(new Class[][] { {}, { IOException.class }, {} });

        ee.cook(new String[] {
            "a + b",
            "new FileInputStream(\"xyz\")",
            "ExternalClass.m1()",
        });

        {
            Method m = ee.getMethod(0);
            assertEquals(5, m.invoke(m.getDeclaringClass().newInstance(), new Object[] { 2, 3 }));
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
        IClassBodyEvaluator cbe = compilerFactory.newClassBodyEvaluator();
        cbe.setImplementedInterfaces(new Class[] { Runnable.class });
        cbe.cook(
            "import java.util.*;\n" +
            "public void run() {\n" +
            "    new ArrayList();\n" +
            "    new other_package.Foo(7);\n" +
            "}\n"
        );
        ((Runnable) cbe.getClazz().newInstance()).run();
    }

    public void testFastClassBodyEvaluator2() throws Exception {
        try {
            IClassBodyEvaluator cbe = compilerFactory.newClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[] { Runnable.class });
            cbe.cook(
                "public void m() { // Implement \"m()\" instead of \"run()\".\n" +
                "    System.out.println(\"Got here\");\n" +
                "}"
            );
            fail("CompileException expected");
        } catch (CompileException ex) {
            ;
        }
    }

    @SuppressWarnings("unchecked")
    public void testFastExpressionEvaluator() throws Exception {
        IExpressionEvaluator ee = compilerFactory.newExpressionEvaluator();
        ee.setImplementedInterfaces(new Class[] { Comparable.class });
        ((Comparable<String>) ee.createFastEvaluator(
            "o == null ? 3 : 4",
            Comparable.class,
            new String[] { "o" }
        )).compareTo("");
    }

    public void testManyEEs() throws Exception {
        IExpressionEvaluator ee = compilerFactory.newExpressionEvaluator();
        final int COUNT = 10000;

        String[]     expressions = new String[COUNT];
        String[][]   parameterNames = new String[COUNT][2];
        Class<?>[][] parameterTypes = new Class[COUNT][2];
        for (int i = 0; i < expressions.length; ++i) {
            expressions[i] = "a + b";
            parameterNames[i][0] = "a";
            parameterNames[i][1] = "b";
            parameterTypes[i][0] = int.class;
            parameterTypes[i][1] = int.class;
        }
        ee.setParameters(parameterNames, parameterTypes);

        ee.cook(expressions);
        assertEquals(165, ee.evaluate(3 * COUNT / 4, new Object[] { 77, 88 }));
    }

    public void testAssertNotCooked() throws Exception {
        IClassBodyEvaluator temp = compilerFactory.newClassBodyEvaluator();
        temp.cook("");
        try {
            temp.setExtendedClass(String.class); // Must throw an ISE because the CBE is already cooked.
        } catch (IllegalStateException ex) {
            return;
        }
        fail();
    }

    public void testAccessingCompilingClass() throws Exception {
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
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
        Class<?>[] exp = new Class[] {
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
                    Class<?> res = (Class<?>) m[i].invoke(inst, new Object[0]);
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
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
//        sc.setParentClassLoader(BOOT_CLASS_LOADER /*, new Class[] { for_sandbox_tests.ProtectedVariable.class }*/);
        sc.cook(
            "package test;\n" +
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
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
//        sc.setParentClassLoader(BOOT_CLASS_LOADER /*, new Class[] { for_sandbox_tests.ProtectedVariable.class }*/);
        sc.cook(
            "package for_sandbox_tests;\n" +
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

        Class<?> topClass = sc.getClassLoader().loadClass("for_sandbox_tests.Top");
        Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object   t = topClass.newInstance();
        Object   i = createInner.invoke(t, new Object[0]);

        Class<?> innerClass = sc.getClassLoader().loadClass("for_sandbox_tests.Top$Inner");
        Method   get = innerClass.getDeclaredMethod("get", new Class[0]);
        Method   getS = innerClass.getDeclaredMethod("getS", new Class[0]);
        Method   set = innerClass.getDeclaredMethod("set", new Class[0]);
        Method   setS = innerClass.getDeclaredMethod("setS", new Class[0]);

        Object res;
        {   // non-static
            res = get.invoke(i, new Object[0]);
            assertEquals(1, res);
            set.invoke(i, new Object[0]);
            res = get.invoke(i, new Object[0]);
            assertEquals(11, res);
        }
        {   //static
            res = getS.invoke(i, new Object[0]);
            assertEquals(2, res);
            setS.invoke(i, new Object[0]);
            res = getS.invoke(i, new Object[0]);
            assertEquals(12, res);
        }
    }

    public void testComplicatedSyntheticAccess() throws Exception {
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
//        sc.setParentClassLoader(BOOT_CLASS_LOADER /*, new Class[] { for_sandbox_tests.ProtectedVariable.class }*/);
        sc.cook(
            "package for_sandbox_tests;\n" +
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

        Class<?> topClass = sc.getClassLoader().loadClass("for_sandbox_tests.L0");
        Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object   t = topClass.newInstance();
        Object   inner = createInner.invoke(t, new Object[0]);

        Class<?> innerClass = inner.getClass();
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
            assertEquals(1, g1);
            Object s1 = sets[i].invoke(inner, new Object[0]);
            assertEquals(i, s1);
            Object g2 = gets[i].invoke(inner, new Object[0]);
            assertEquals(i, g2);
        }
    }

    public void testStaticInitAccessProtected() throws Exception {
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
        sc.cook(
            "package test;\n" +
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

        Class<?> topClass = sc.getClassLoader().loadClass("test.Outer");
        Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object   t = topClass.newInstance();
        assertNotNull(t);
        Object inner = createInner.invoke(t, new Object[0]);
        assertNotNull(inner);
    }

    public void testDivByZero() throws Exception {
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
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


        Class<?> c = sc.getClassLoader().loadClass("test.Test");
        Object   o = c.newInstance();

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
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
        sc.cook(prog);

        Class<?>   c = sc.getClassLoader().loadClass("test.Test");
        Method     dm = c.getMethod("compare", new Class[] { double.class, double.class, String.class });
        Method     fm = c.getMethod("compare", new Class[] { float.class, float.class, String.class });
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
                    assertEquals(msg, exp, actual);
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
                    assertEquals(msg, exp, actual);
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

            ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Method   m = c.getDeclaredMethod("run", new Class[0]);
            Object   o = c.newInstance();
            Object   res = m.invoke(o, new Object[0]);
            assertEquals(2 * repititions, res);
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

            ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Object   o = c.newInstance();
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

            ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Object   o = c.newInstance();
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
                sb.append(MessageFormat.format(middle, new Object[] { j }));
            }
            sb.append(postamble);

            ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());
            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Method   m = c.getDeclaredMethod("run", new Class[0]);
            Object   o = m.invoke(null, new Object[0]);
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

        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
        sc.cook(test);
        Class<?> c = sc.getClassLoader().loadClass("test.Test");
        Method   m0 = c.getDeclaredMethod("run", new Class[] { });
        assertEquals(false, m0.invoke(null, new Object[0]));

        Method mStr = c.getDeclaredMethod("run", new Class[] { String.class });
        Method mObj = c.getDeclaredMethod("run", new Class[] { Object.class });

        assertEquals(true,  mObj.invoke(null, new Object[] { "" }));
        assertEquals(false, mObj.invoke(null, new Object[] { 1 }));
        assertEquals(false, mObj.invoke(null, new Object[] { null }));

        assertEquals(true,  mStr.invoke(null, new Object[] { "" }));
        assertEquals(false, mStr.invoke(null, new Object[] { null }));
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
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
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

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get = topClass.getDeclaredMethod("get", new Class[0]);
        Object   t = topClass.newInstance();
        Object   res = get.invoke(t, new Object[0]);
        ((Runnable)res).run();
    }


    public void testNamedFieldInitializedByCapture() throws Exception {
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
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

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get = topClass.getDeclaredMethod("get", new Class[0]);
        Object   t = topClass.newInstance();
        Object   res = get.invoke(t, new Object[0]);
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
        ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
        sc.cook("public class Top {\n" +
                "  public int len(StringBuilder sb) { return sb.length(); }" +
                "}"
        );

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get = topClass.getDeclaredMethod("len", new Class[] { StringBuilder.class });
        Object   t = topClass.newInstance();

        StringBuilder sb = new StringBuilder();
        assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
        sb.append("asdf");
        assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
        sb.append("qwer");
        assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
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

    public ISimpleCompiler assertCompiles(boolean shouldCompile, CharSequence prog) throws Exception {
        try {
            ISimpleCompiler sc = compilerFactory.newSimpleCompiler();
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
