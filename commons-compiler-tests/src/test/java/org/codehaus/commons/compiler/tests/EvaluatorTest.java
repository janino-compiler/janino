
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests for JANINO's {@link ExpressionEvaluator}, {@link ScriptEvaluator}, {@link ClassBodyEvaluator} and {@link
 * SimpleCompiler}.
 */
@RunWith(Parameterized.class) public
class EvaluatorTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    EvaluatorTest(ICompilerFactory compilerFactory) { super(compilerFactory); }

    @SuppressWarnings({ "unused", "static-method" })
    @Before
    public void
    setUp() throws Exception {

        // Enable this code snippet to print class file disassemblies to the console.
        if (false) {
            Logger scl = Logger.getLogger("org.codehaus.janino.SimpleCompiler");
            for (Handler h : scl.getHandlers()) {
                h.setLevel(Level.FINEST);
            }
            scl.setLevel(Level.FINEST);
        }
    }

    @Test public void
    testMultiScriptEvaluator() throws Exception {
        IScriptEvaluator se    = this.compilerFactory.newScriptEvaluator();
        se.setOverrideMethod(new boolean[] { false, false });
        se.setReturnTypes(new Class[] { double.class, double.class });
        se.setMethodNames(new String[] { "funct2", "funct3" });

        se.setParameters(new String[][] { { "a", "b" }, {} }, new Class<?>[][] { { double.class, double.class }, {} });
        se.setStaticMethod(new boolean[] { true, true });
        se.cook(new String[] { "return a + b;", "return 0;" });
        Assert.assertEquals(
            new Double(7.0),
            se.getMethod(0).invoke(null, new Object[] { new Double(3.0), new Double(4.0) })
        );
        Assert.assertEquals(new Double(0.0), se.getMethod(1).invoke(null, new Object[0]));
    }

    @Test public void
    testExpressionEvaluator() throws Exception {
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();

        ee.setClassName("Foo");
        ee.setDefaultImports(new String[] { "java.io.*", "for_sandbox_tests.*", });
        ee.setOverrideMethod(new boolean[] {
            false,
            false,
            true,
        });
        ee.setStaticMethod(new boolean[] {
            false,
            true,
            false,
        });
        ee.setExpressionTypes(new Class[] {
            Object.class,
            InputStream.class,
            void.class,
        });
        ee.setExtendedClass(Properties.class);
        ee.setImplementedInterfaces(new Class[] { Runnable.class, });
        ee.setMethodNames(new String[] {
            "a",
            "b",
            "run",
        });
        ee.setParameters(new String[][] {
            { "a", "b" },
            {},
            {},
        }, new Class[][] {
            { int.class, int.class },
            {},
            {},
        });
//        ee.setParentClassLoader(BOOT_CLASS_LOADER, new Class[] { for_sandbox_tests.ExternalClass.class });
        ee.setThrownExceptions(new Class[][] {
            {},
            { IOException.class },
            {},
        });

        ee.cook(new String[] {
            "a + b",
            "new FileInputStream(\"xyz\")",
            "ExternalClass.m1()",
        });

        {
            Method m        = ee.getMethod(0);
            Object instance = m.getDeclaringClass().newInstance();
            Assert.assertEquals(5, m.invoke(instance, new Object[] { 2, 3 }));
        }

        try {
            ee.evaluate(1, new Object[0]);
            Assert.fail("Should have thrown an InvocationTargetException");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue("FileNotFoundException", ex.getTargetException() instanceof FileNotFoundException);
        }

        try {
            Method m        = ee.getMethod(2);
            Object instance = m.getDeclaringClass().newInstance();
            m.invoke(instance, new Object[0]);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test public void
    testFastClassBodyEvaluator1() throws Exception {
        IClassBodyEvaluator cbe = this.compilerFactory.newClassBodyEvaluator();
        cbe.setImplementedInterfaces(new Class[] { Runnable.class });
        cbe.cook(
            ""
            + "import java.util.*;\n"
            + "@Override public void run() {\n"
            + "    new ArrayList();\n"
            + "    new other_package.Foo(7);\n"
            + "}\n"
        );
        ((Runnable) cbe.getClazz().newInstance()).run();
    }

    @Test public void
    testFastClassBodyEvaluator2() throws Exception {
        try {
            IClassBodyEvaluator cbe = this.compilerFactory.newClassBodyEvaluator();
            cbe.setImplementedInterfaces(new Class[] { Runnable.class });
            cbe.cook(
                ""
                + "public void m() { // Implement \"m()\" instead of \"run()\".\n"
                + "    System.out.println(\"Got here\");\n"
                + "}"
            );
            Assert.fail("CompileException expected");
        } catch (CompileException ex) {
            ;
        }
    }

    @Test @SuppressWarnings("unchecked") public void
    testFastExpressionEvaluator() throws Exception {
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
        ee.setImplementedInterfaces(new Class[] { Comparable.class });
        ee.setExpressionTypes(new Class[] { int.class });
        ((Comparable<String>) ee.createFastEvaluator(
            "o == null ? 3 : 4",
            Comparable.class,
            new String[] { "o" }
        )).compareTo("");
    }

    private static final int COUNT = 10000;

    @Test public void
    testManyEEs() throws Exception {
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();

        String[]     expressions    = new String[EvaluatorTest.COUNT];
        String[][]   parameterNames = new String[EvaluatorTest.COUNT][2];
        Class<?>[][] parameterTypes = new Class[EvaluatorTest.COUNT][2];
        for (int i = 0; i < expressions.length; ++i) {
            expressions[i]       = "a + b";
            parameterNames[i][0] = "a";
            parameterNames[i][1] = "b";
            parameterTypes[i][0] = int.class;
            parameterTypes[i][1] = int.class;
        }
        ee.setParameters(parameterNames, parameterTypes);

        ee.cook(expressions);
        Assert.assertEquals(165, ee.evaluate(3 * EvaluatorTest.COUNT / 4, new Object[] { 77, 88 }));
    }

    @Test public void
    testAccessingCompilingClass() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package test.simple;\n"
            + "public class L0 {\n"
            + "    public static class L1 {\n"
            + "        public static class L2 { }\n"
            + "    } \n"
            + "    public Class getL0_1() {\n"
            + "        return L0.class;\n"
            + "    }\n"
            + "    public Class getL0_2() {\n"
            + "        return test.simple.L0.class;\n"
            + "    }\n"
            + "    public Class getL1_1() {\n"
            + "        return L1.class;\n"
            + "    }\n"
            + "    public Class getL1_2() {\n"
            + "        return L0.L1.class;\n"
            + "    }\n"
            + "    public Class getL1_3() {\n"
            + "        return test.simple.L0.L1.class;\n"
            + "    }\n"
            + "    public Class getL2_1() {\n"
            + "        return L1.L2.class;\n"
            + "    }\n"
            + "    public Class getL2_2() {\n"
            + "        return L0.L1.L2.class;\n"
            + "    }\n"
            + "    public Class getL2_3() {\n"
            + "        return test.simple.L0.L1.L2.class;\n"
            + "    }\n"
            + "}"
        );
        Class<?>[] exp = new Class[] {
            sc.getClassLoader().loadClass("test.simple.L0"),
            sc.getClassLoader().loadClass("test.simple.L0$L1"),
            sc.getClassLoader().loadClass("test.simple.L0$L1$L2"),
        };

        Method[] methods  = exp[0].getMethods();
        Object   inst     = exp[0].newInstance();
        int      numTests = 0;
        for (Method method : methods) {
            for (int j = 0; j < exp.length; ++j) {
                if (method.getName().startsWith("getL" + j)) {
                    Class<?> res = (Class<?>) method.invoke(inst, new Object[0]);
                    Assert.assertEquals(exp[j], res);
                    ++numTests;
                }
            }
        }
        //we count tests just to make sure things didn't go horrifically wrong and
        //the above loops become empty
        Assert.assertEquals(8, numTests);
    }

    @Test public void
    testDivByZero() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public int runIntDiv() {\n"
            + "        return 1 / 0;\n"
            + "    }\n"
            + "    public int runIntMod() {\n"
            + "        return 1 % 0;\n"
            + "    }\n"
            + "    public long runLongDiv() {\n"
            + "        return 1L / 0;\n"
            + "    }\n"
            + "    public long runLongMod() {\n"
            + "        return 1L % 0;\n"
            + "    }\n"
            + "}"
        );

        Class<?> c = sc.getClassLoader().loadClass("test.Test");
        Object   o = c.newInstance();

        for (Method method : c.getMethods()) {
            if (method.getName().startsWith("run")) {
                try {
                    Object res = method.invoke(o, new Object[0]);
                    Assert.fail("Method " + method + " should have failed, but got " + res);
                } catch (InvocationTargetException ae) {
                    Assert.assertTrue(ae.getTargetException() instanceof ArithmeticException);
                }
            }
        }
    }

    @Test public void
    testTrinaryOptimize() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public int runTrue() {\n"
            + "        return true ? -1 : 1;\n"
            + "    }\n"
            + "    public int runFalse() {\n"
            + "        return false ? -1 : 1;\n"
            + "    }\n"
            + "}"
        );

        Class<?> c         = sc.getClassLoader().loadClass("test.Test");
        Method   trueMeth  = c.getMethod("runTrue");
        Method   falseMeth = c.getMethod("runFalse");

        Object   o = c.newInstance();
        Assert.assertEquals(-1, trueMeth.invoke(o, new Object[0]));
        Assert.assertEquals(1, falseMeth.invoke(o, new Object[0]));
    }

    public static boolean
    compare(double lhs, double rhs, String comp) {
        // CHECKSTYLE StringLiteralEquality:OFF
        if (comp == "==") { return lhs == rhs; }
        if (comp == "!=") { return lhs != rhs; }
        if (comp == "<")  { return lhs < rhs; }
        if (comp == "<=") { return lhs <= rhs; }
        if (comp == ">")  { return lhs < rhs; }
        if (comp == ">=") { return lhs <= rhs; }
        // CHECKSTYLE StringLiteralEquality:ON
        throw new RuntimeException("Unsupported comparison");
    }
    public static boolean
    compare(float lhs, float rhs, String comp) {
        // CHECKSTYLE StringLiteralEquality:OFF
        if (comp == "==") { return lhs == rhs; }
        if (comp == "!=") { return lhs != rhs; }
        if (comp == "<")  { return lhs < rhs; }
        if (comp == "<=") { return lhs <= rhs; }
        if (comp == ">")  { return lhs < rhs; }
        if (comp == ">=") { return lhs <= rhs; }
        // CHECKSTYLE StringLiteralEquality:ON
        throw new RuntimeException("Unsupported comparison");
    }

    @Test public void
    testHandlingNaN() throws Exception {
        String prog = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public static boolean compare(double lhs, double rhs, String comp) {\n"
            + "        if (comp == \"==\") { return lhs == rhs; }\n"
            + "        if (comp == \"!=\") { return lhs != rhs; }\n"
            + "        if (comp == \"<\" ) { return lhs < rhs; }\n"
            + "        if (comp == \"<=\") { return lhs <= rhs; }\n"
            + "        if (comp == \">\" ) { return lhs < rhs; }\n"
            + "        if (comp == \">=\") { return lhs <= rhs; }\n"
            + "        throw new RuntimeException(\"Unsupported comparison\");\n"
            + "    }\n"
            + "    public static boolean compare(float lhs, float rhs, String comp) {\n"
            + "        if (comp == \"==\") { return lhs == rhs; }\n"
            + "        if (comp == \"!=\") { return lhs != rhs; }\n"
            + "        if (comp == \"<\" ) { return lhs < rhs; }\n"
            + "        if (comp == \"<=\") { return lhs <= rhs; }\n"
            + "        if (comp == \">\" ) { return lhs < rhs; }\n"
            + "        if (comp == \">=\") { return lhs <= rhs; }\n"
            + "        throw new RuntimeException(\"Unsupported comparison\");\n"
            + "    }\n"
            + "}\n"
        );
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(prog);

        final Class<?>   c     = sc.getClassLoader().loadClass("test.Test");
        final Method     dm    = c.getMethod("compare", new Class[] { double.class, double.class, String.class });
        final Method     fm    = c.getMethod("compare", new Class[] { float.class, float.class, String.class });
        final Double[][] argss = new Double[][] {
            { new Double(Double.NaN), new Double(Double.NaN) },
            { new Double(Double.NaN), new Double(1.0) },
            { new Double(1.0), new Double(Double.NaN) },
            { new Double(1.0), new Double(2.0) },
            { new Double(2.0), new Double(1.0) },
            { new Double(1.0), new Double(1.0) },
        };
        String[] operators = new String[] { "==", "!=", "<", "<=", ">", ">=" };
        for (String operator : operators) {
            for (Double[] args : argss) {
                String msg = "\"" + args[0] + " " + operator + " " + args[1] + "\"";
                {
                    boolean exp = EvaluatorTest.compare(
                        args[0].doubleValue(),
                        args[1].doubleValue(),
                        operator
                    );
                    Object[] objs   = new Object[] { args[0], args[1], operator };
                    Object   actual = dm.invoke(null, objs);
                    Assert.assertEquals(msg, exp, actual);
                }

                {
                    msg = "float: " + msg;
                    boolean exp = EvaluatorTest.compare(
                        args[0].floatValue(),
                        args[1].floatValue(),
                        operator
                    );
                    Object[] objs = new Object[] {
                        new Float(args[0].floatValue()),
                        new Float(args[1].floatValue()),
                        operator
                    };
                    Object actual = fm.invoke(null, objs);
                    Assert.assertEquals(msg, exp, actual);
                }
            }
        }
    }

    @Test public void
    test32kBranchLimit() throws Exception {
        String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public int run() {\n"
            + "        int res = 0;\n"
            + "        for(int i = 0; i < 2; ++i) {\n"
        );
        String middle = (
            ""
            + "            ++res;\n"
        );
        final String postamble = (
            ""
            + "        }\n"
            + "        return res;\n"
            + "    }\n"
            + "}"
        );

        int[] repetitionss = new int[] { 1, 10, 100, Short.MAX_VALUE / 5, Short.MAX_VALUE / 4, Short.MAX_VALUE / 2 };
        for (int repetitions : repetitionss) {
            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for (int j = 0; j < repetitions; ++j) {
                sb.append(middle);
            }
            sb.append(postamble);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c   = sc.getClassLoader().loadClass("test.Test");
            Method   m   = c.getDeclaredMethod("run", new Class[0]);
            Object   o   = c.newInstance();
            Object   res = m.invoke(o, new Object[0]);
            Assert.assertEquals(2 * repetitions, res);
        }

    }
    @Test public void
    test64kConstantPool() throws Exception {
        String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
        );
        final String postamble = (
            "}"
        );

        /* == expected contant pool ==
        ( 0) fake entry
        [ 1] ConstantUtf8Info        "test/Test"
        [ 2] ConstantClassInfo       1
        [ 3] ConstantUtf8Info        "java/lang/Object"
        [ 4] ConstantClassInfo       3
        [ 5] ConstantUtf8Info        "<init>"
        [ 6] ConstantUtf8Info        "()V"
        [ 7] ConstantNameAndTypeInfo 5
        [ 8] ConstantMethodrefInfo   4
        [ 9] ConstantUtf8Info        "Code"
        [10] ConstantUtf8Info        "_v0"
        [11] ConstantUtf8Info        "Z"
        [12] ConstantUtf8Info        "_v1"
        [13] ConstantUtf8Info        "_v2"
        ...
        [65534] ConstantUtf8Info     "_v65523"
        */

        final int[]     repetitionss = new int[]     { 1,    100,  65524, 65525 };
        final boolean[] cookable     = new boolean[] { true, true, true,  false };

        for (int i = 0; i < repetitionss.length; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for (int j = 0; j < repetitionss[i]; ++j) {
                sb.append("boolean _v").append(j).append(";\n");
            }
            sb.append(postamble);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            if (cookable[i]) {
                sc.cook(sb.toString());
                Class<?> c = sc.getClassLoader().loadClass("test.Test");
                Object   o = c.newInstance();
                Assert.assertNotNull(o);
            } else {
                try {
                    sc.cook(sb.toString());
                    Assert.fail("Should have issued an error, but compiled successfully");
                } catch (CompileException ce) {
                    Assert.assertTrue(ce.getMessage(), (
                        ce.getMessage().contains("too many constants (compiler.err.limit.pool)")
                        || ce.getMessage().contains("grown past JVM limit of 0xFFFF")
                    ));
                }
            }
        }
    }
    @Test public void
    testManyArgumentsCall() throws Exception {
        final String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    double test(\n"
        );
        final String middle1 = (
            ""
            + "      double d_{0,number,#},\n"
        );
        final String middle2 = (
            ""
            + "    public double run() {\n"
            + "      boolean b = false;\n"
            + "      double d = b ? 2.3D : test(\n"
        );
        final String postamble = (
            ""
            + "      );\n"
            + "      return d;\n"
            + "    }\n"
            + "}"
        );

        int[] repetitionss = new int[] { 1, 16, 127 };
        for (int repetitions : repetitionss) {
            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for (int j = 0; j < repetitions - 1; ++j) {
                sb.append(MessageFormat.format(middle1, new Object[] { j }));
            }
            sb.append("double d_" + (repetitions - 1) + ") { return 1.2D; }\n");
            sb.append(middle2);
            for (int j = 0; j < repetitions - 1; ++j) {
                sb.append("0, ");
            }
            sb.append("0");
            sb.append(postamble);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c   = sc.getClassLoader().loadClass("test.Test");
            Method   m   = c.getDeclaredMethod("run", new Class[0]);
            Object   o   = c.newInstance();
            Object   res = m.invoke(o, new Object[0]);
            Assert.assertEquals(1.2D, res);
        }
    }
    @Test public void
    test64kOperandStackHeight() throws Exception {
        String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public double run() {\n"
            + "      double d = 1;\n"
            + "      double r = "
        );
        final String epilog = (
            ";\n"
            + "      return r;\n"
            + "    }\n"
            + "}"
        );

        // If more than 500, then we'd have to increase the JVM stack size.
        for (int repetitions : new int[] { 5, 50, 500 }) {

            StringBuilder sb = new StringBuilder().append(preamble);

            for (int j = 0; j < repetitions; ++j) sb.append("+ (d");

            for (int j = 0; j < repetitions; ++j) sb.append(")");

            sb.append(epilog);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c   = sc.getClassLoader().loadClass("test.Test");
            Method   m   = c.getDeclaredMethod("run", new Class[0]);
            Object   o   = c.newInstance();
            Object   res = m.invoke(o);
            Assert.assertEquals((double) repetitions, res);
        }
    }

    @Test public void
    testHugeIntArray() throws Exception {
        String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public int[] run() {\n"
            + "        return 1.0 > 2.0 ? null : new int[] {"
        );
        String middle = (
            ""
            + "            123,"
        );
        final String postamble = (
            ""
            + "        };\n"
            + "    }\n"
            + "}"
        );

        int[] repetitionss = new int[] { 1, 10, 8192 };
        for (int repetitions : repetitionss) {
            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for (int j = 0; j < repetitions; ++j) {
                sb.append(middle);
            }
            sb.append(postamble);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());

            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Object   o = c.newInstance();
            Assert.assertNotNull(o);
        }
    }

    @Test public void
    testStaticFieldAccess() throws Exception {
        this.assertCompilationUnitCookable((
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public static class Inner {\n"
            + "        public static int i = 0;\n"
            + "    }\n"
            + "    public int runTest(Inner in) {\n"
            + "        return in.i;\n"
            + "    }\n"
            + "}"
        ));
    }

    @Test public void
    testWideInstructions() throws Exception {
        String preamble = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public static String run() {\n"
        );
        String middle = (
            ""
            + "        Object o_{0,number,#}; int i_{0,number,#};\n"
        );
        final String postamble = (
            ""
            + "        int i = (int)0; ++i; i = (int)(i*i);\n"
            + "        double d = (double)0.0; ++d; d = (double)(d*d);\n"
            + "        float f = (float)0.0; ++f; f = (float)(f*f);\n"
            + "        short s = (short)0; ++s; s = (short)(s*s);\n"
            + "        long l = (long)0; ++l; l = (long)(l*l);\n"
            + "        boolean b = false; b = !b;\n"
            + "        Object o = \"hi\"; o = o.toString();\n"
            + "        String res = o.toString() +\" \"+ i +\" \"+ d +\" \"+  f +\" \"+ s +\" \"+ l +\" \"+ b;\n"
            + "        try { res = res + \" try\"; } finally { res = res + \" finally\"; }\n"
            + "        return res;\n"
            + "    };\n"
            + "}"
        );

        int[] repetitionss = new int[] { 1, 128, };
        for (int repetitions : repetitionss) {
            StringBuilder sb = new StringBuilder();
            sb.append(preamble);
            for (int j = 0; j < repetitions; ++j) {
                sb.append(MessageFormat.format(middle, new Object[] { j }));
            }
            sb.append(postamble);

            ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
            sc.cook(sb.toString());
            Class<?> c = sc.getClassLoader().loadClass("test.Test");
            Method   m = c.getDeclaredMethod("run", new Class[0]);
            Object   o = m.invoke(null, new Object[0]);
            Assert.assertEquals("hi 1 1.0 1.0 1 1 true try finally", o);
        }

    }

    @Test public void
    testInstanceOf() throws Exception {
        String test = (
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public static boolean run(String o) {\n"
            + "        return o instanceof String;"
            + "    };\n"
            + "    public static boolean run(Object o) {\n"
            + "        return o instanceof String;"
            + "    };\n"
            + "    public static boolean run() {\n"
            + "        return null instanceof String;"
            + "    };\n"
            + "}"
        );

        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(test);
        Class<?> c  = sc.getClassLoader().loadClass("test.Test");
        Method   m0 = c.getDeclaredMethod("run", new Class[] {});
        Assert.assertEquals(false, m0.invoke(null, new Object[0]));

        Method mStr = c.getDeclaredMethod("run", new Class[] { String.class });
        Method mObj = c.getDeclaredMethod("run", new Class[] { Object.class });

        Assert.assertEquals(true,  mObj.invoke(null, new Object[] { "" }));
        Assert.assertEquals(false, mObj.invoke(null, new Object[] { 1 }));
        Assert.assertEquals(false, mObj.invoke(null, new Object[] { null }));

        Assert.assertEquals(true,  mStr.invoke(null, new Object[] { "" }));
        Assert.assertEquals(false, mStr.invoke(null, new Object[] { null }));
    }

    @Test public void
    testOverrideVisibility() throws Exception {

        // so should this
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "package test;\n"
            + "public class Test {\n"
            + "    public static boolean main() {\n"
            + "       return \"A\".equals(test.OverridesWithDifferingVisibility.test(new Object[] { \"asdf\"} ));\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class OverridesWithDifferingVisibility {\n"
            + "\n"
            + "    public static String  test(Object o)     { return \"A\"; }\n"
            + "    private static String test(Object[] arr) { return \"B\"; }\n"
            + "}"
        ), "test.Test");
    }

    @Test public void
    testCovariantReturns() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "package test;\n"
            + "public class Test extends CovariantReturns {\n"
            + "    @Override public Test overrideMe() { return this; }\n"
            + "}\n"
            + "public abstract\n"
            + "class CovariantReturns {\n"
            + "    public abstract CovariantReturns overrideMe();\n"
            + "}"
        );
        this.assertCompilationUnitUncookable(
            ""
            + "package test;\n"
            + "public class Test2 extends CovariantReturns {\n"
            + "    public Integer overrideMe() { return null; }\n"
            + "}\n"
            + "public abstract\n"
            + "class CovariantReturns {\n"
            + "    public abstract CovariantReturns overrideMe();\n"
            + "}"
        );
    }

    @Test public void
    testNonExistentImport() throws Exception {
        this.assertCompilationUnitUncookable(
            "import does.not.Exist; public class Test { private final Exist e = null; }"
        );
        this.assertCompilationUnitUncookable("import does.not.Exist; public class Test { }");
    }

    @Test public void
    testAnonymousFieldInitializedByCapture() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "public class Top {\n"
            + "    public Runnable get() {\n"
            + "        final String foo = \"foo\";\n"
            + "        final String cow = \"cow\";\n"
            + "        final String moo = \"moo\";\n"
            + "        return new Runnable() {\n"
            + "            @Override public void run() {\n"
            + "               if (bar == null) {\n"
            + "                   throw new RuntimeException(\"bar is NULL\");\n"
            + "               }\n"
            + "            }\n"
            + "            private String bar = foo;\n"
            + "            private String[] cowparts = { moo, cow };\n"
            + "        };\n"
            + "    }\n"
            + "}\n"
        );

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get      = topClass.getDeclaredMethod("get", new Class[0]);
        Object   t        = topClass.newInstance();
        Object   res      = get.invoke(t, new Object[0]);
        ((Runnable) res).run();
    }

    @Test public void
    testNamedFieldInitializedByCapture() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "public class Top {\n"
            + "    public Runnable get() {\n"
            + "        final String foo = \"foo\";\n"
            + "        final String cow = \"cow\";\n"
            + "        final String moo = \"moo\";\n"
            + "        class R implements Runnable {\n"
            + "            @Override public void run() {\n"
            + "                if (bar == null) {\n"
            + "                    throw new RuntimeException();\n"
            + "                }\n"
            + "            }\n"
            + "            private String bar = foo;\n"
            + "            private String[] cowparts = { moo, cow };\n"
            + "        }\n"
            + "        return new R();"
            + "    }\n"
            + "}\n"
        );

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get      = topClass.getDeclaredMethod("get", new Class[0]);
        Object   t        = topClass.newInstance();
        Object   res      = get.invoke(t, new Object[0]);
        ((Runnable) res).run();
    }

    @Test public void
    testAbstractGrandParentsWithCovariantReturns() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class Top {\n"
            + "    private static class IndentPrintWriter extends java.io.PrintWriter { "
            + "        public IndentPrintWriter(java.io.OutputStream os) { super(os); }"
            + "    }"
            + "}"
        );
    }

    @Test public void
    testStringBuilderLength() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "public class Top {\n"
            + "  public int len(StringBuilder sb) { return sb.length(); }\n"
            + "}\n"
        );

        Class<?> topClass = sc.getClassLoader().loadClass("Top");
        Method   get      = topClass.getDeclaredMethod("len", new Class[] { StringBuilder.class });
        Object   t        = topClass.newInstance();

        StringBuilder sb = new StringBuilder();
        Assert.assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
        sb.append("asdf");
        Assert.assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
        sb.append("qwer");
        Assert.assertEquals(sb.length(), get.invoke(t, new Object[] { sb }));
    }

    @Test public void
    testCovariantClone() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package covariant_clone;\n"
            + "\n"
            + "public class Child extends Middle implements java.lang.Cloneable {\n"
            + "    @Override public Child clone() throws java.lang.CloneNotSupportedException {\n"
            + "        return new Child();\n"
            + "    }\n"
            + "    @Override public Child other(long i, Object o) {\n"
            + "        return this;\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Middle extends Base {\n"
            + "\n"
            + "    public Base\n"
            + "    cloneWithOutArguments() {\n"
            + "        try {\n"
            + "            return this.clone();\n"
            + "        } catch (CloneNotSupportedException e) {\n"
            + "            throw new RuntimeException(\n"
            + "                \"Clone not supported on class: \" + this.getClass().getName(),\n"
            + "                e\n"
            + "            );\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public Middle\n"
            + "    cloneWithArguments() {\n"
            + "        return this.other(1, null);\n"
            + "    }\n"
            + "\n"
            + "    @Override public Middle\n"
            + "    other(long i, Object o) {\n"
            + "        throw new RuntimeException(\"Middle() called\");\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "public abstract\n"
            + "class Base implements Cloneable, CloneableData {\n"
            + "\n"
            + "    /**\n"
            + "     * Clone this tuple, the new tuple will not share any buffers or data with the original.\n"
            + "     * @return A copy of this Tuple\n"
            + "     */\n"
            + "    @Override public Base\n"
            + "    clone() throws CloneNotSupportedException {\n"
            + "        //subclasses must implement\n"
            + "        throw new CloneNotSupportedException();\n"
            + "    }\n"
            + "\n"
            + "    public Base\n"
            + "    other(long i, Object o) {\n"
            + "        throw new RuntimeException(\"Base.other() called\");\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "interface CloneableData extends Cloneable {\n"
            + "    CloneableData clone() throws CloneNotSupportedException;\n"
            + "}"
        );

        // calling clone directly here would work, we need to trigger a call into the
        // covariant version of clone from a less described version of it.
        Class<?> topClass = sc.getClassLoader().loadClass("covariant_clone.Child");
        Method   foo      = topClass.getMethod("cloneWithArguments");
        Method   bar      = topClass.getMethod("cloneWithOutArguments");
        Object   t        = topClass.newInstance();

        Assert.assertNotNull(foo.invoke(t, new Object[0]));
        Assert.assertNotNull(bar.invoke(t, new Object[0]));
    }

    @Test public void
    testBaseClassAccess() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "class top extends other_package.ScopingRules {\n"
            + "    class Inner extends other_package.ScopingRules.ProtectedInner {\n"
            + "        public void test() {\n"
            + "            publicMethod();\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public void test() {\n"
            + "        Inner i = new Inner();\n"
            + "        i.publicMethod();\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testNullComparator() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "class Test {\n"
            + "    public void test() {\n"
            + "        if (null == null) {\n"
            + "            // success\n"
            + "        } else if (null != null) {\n"
            + "            throw new RuntimeException();\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        );
    }
}
