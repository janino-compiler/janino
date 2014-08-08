
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

import java.io.File;
import java.io.StringReader;
import java.util.Collection;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.tests.bug172.First;
import org.codehaus.commons.compiler.tests.bug180.UnaryDoubleFunction;
import org.codehaus.commons.compiler.tests.bug63.IPred;
import org.codehaus.commons.compiler.tests.bug63.Pred;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.JaninoTestSuite;
import util.TestUtil;

// CHECKSTYLE MethodName:OFF
// CHECKSTYLE JavadocMethod:OFF

/** Test cases for the bug reported on <a href="http://jira.codehaus.org/">CODEHAUS JIRA</a> for project JANINO. */
@RunWith(Parameterized.class) public
class ReportedBugs extends JaninoTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    ReportedBugs(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    @Test public void
    testBug48() throws Exception {
        this.assertCompilationUnitMainExecutable((
            ""
            + "package demo;\n"
            + "public class Service {\n"
            + "    public static boolean main() {\n"
            + "        Broken[] dummy = new Broken[5];\n"
            + "        return true;\n"
            + "    }\n"
            + "    class Broken {\n"
            + "    }\n"
            + "}"
        ), "demo.Service");
        this.assertCompilationUnitMainExecutable((
            ""
            + "package demo;\n"
            + "public class Service {\n"
            + "    public static Broken[] main() {\n"
            + "        return null;\n"
            + "    }\n"
            + "}\n"
            + "class Broken {\n"
            + "}"
        ), "demo.Service");
    }

    @Test public void
    testBug54() throws Exception {
        this.assertScriptReturnsTrue(
            ""
            + "String s = \"\";\n"
            + "try {\n"
            + "    {\n"
            + "        s += \"set1\";\n"
            + "    }\n"
            + "    {\n"
            + "        boolean tmp4 = false;\n"
            + "        if (tmp4) {\n"
            + "            {\n"
            + "                s += \"if true\";\n"
            + "                if (true) return false;\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "    {\n"
            + "        s += \"return\";\n"
            + "    }\n"
            + "} catch (Exception e) {\n"
            + "    s += \"exception\";\n"
            + "} finally {\n"
            + "    s +=\"finally\";\n"
            + "}\n"
            + "return \"set1returnfinally\".equals(s);"
        );
        this.assertClassBodyCookable(
            ""
            + "void foo() {\n"
            + "    while (true) {\n"
            + "        if (true) {\n"
            + "            break;\n"
            + "        }\n"
            + "        return;\n"
            + "    }\n"
            + "}\n"
            + "void bar() {\n"
            + "    while (true) {\n"
            + "        {\n"
            + "            if (true) {\n"
            + "                break;\n"
            + "            }\n"
            + "        }\n"
            + "        return;\n"
            + "    }\n"
            + "}\n"
        );
        this.assertClassBodyCookable(
            ""
            + "void baz1() {\n"
            + "    for (int i = 0; i < 100;) {\n"
            + "        {\n"
            + "            if (true) {\n"
            + "                break;\n"
            + "            }\n"
            + "        }\n"
            + "        i += 2;\n"
            + "    }\n"
            + "}\n"
        );
        this.assertClassBodyCookable(
            ""
            + "void baz2() {\n"
            + "    for (int i = 0; i < 100; i++) {\n"
            + "        {\n"
            + "            if (true) {\n"
            + "                break;\n"
            + "            }\n"
            + "        }\n"
            + "        i += 2;\n"
            + "    }\n"
            + "}\n"
        );
        this.assertClassBodyCookable(
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        break;\n"
            + "    }\n"
            + "}\n"
        );
        this.assertClassBodyCookable(
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        if (true) { break; }\n"
            + "    }\n"
            + "}\n"
        );
        this.assertClassBodyCookable(
            ""
            + "public void foo() throws Exception {\n"
            + "    {\n"
            + "        try {\n"
            + "            int i = 0;\n"
            + "            for (; true;) {\n"
            + "                try {\n"
            + "                    {\n"
            + "                        {\n" // Invoke: break
            + "                            if (true) { break; }\n"
            + "                        }\n" // End Invoke: break
            + "                    }\n"
            + "                    i++;\n"
            + "                } finally {}\n"
            + "                i++;\n"
            + "            }\n"
            + "            return;\n"
            + "        } finally {}\n"
            + "    }\n"
            + "}\n"
        );
        this.assertScriptExecutable(
            ""
            + "int c = 5;\n"
            + "if (c == 5) {\n"
            + "    if (true) return;\n"
            + "} else {\n"
            + "    return;\n"
            + "}\n"
            + "int b = 3;\n" // Physically unreachable, but logically reachable, hence not a compile error.
        );
    }

    @Test public void
    testBug55() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "class Junk {" + "\n"
            + "    double[][] x = { { 123.4 } };" + "\n"
            + "}"
        );
    }

    @Test public void
    testBug56() throws Exception {
        this.assertScriptCookable(
            ""
            + "int dummy3 = 3;\n"
            + "try {\n"
            + "    // 3 vars must be declared in try block\n"
            + "    int dummy5 = 5;\n"
            + "    int dummy4 = 4;\n"
            + "    boolean b = true;\n"
            + "\n"
            + "    while (b) {\n"
            + "        try {\n"
            + "            ++dummy5;\n"                // Optional
            + "            return;\n"                  // <= Required
            + "        } catch (Exception ex) {\n"
            + "            ++dummy5;\n"
            + "        }\n"
            + "    }\n"
            + "} finally {\n"
            + "    ++dummy3;\n"
            + "}\n"
        );
    }

    @Test public void
    testBug63() throws Exception {
        String cnIPred = IPred.class.getCanonicalName();
        String cnPred  = Pred.class.getCanonicalName();
        this.assertClassBodyUncookable(
            ""
            + "public static boolean main() {\n"
            + "    " + cnIPred + " p = new " + cnPred + "();\n"
            + "    return !p.filter();\n" // Comile error, because 'IPred.filter()' throws 'Exception'
            + "}\n"
        );
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean main() {\n"
            + "    " + cnPred + " p = new " + cnPred + "();\n"
            + "    return !p.filter();\n"
            + "}\n"
        );
    }

    @Test public void
    testBug69() throws Exception {
        this.assertCompilationUnitMainExecutable((
            ""
            + "public class Test {\n"
            + "    public static void main() {\n"
            + "        Object foo = baz();\n"
            + "        System.out.println(\"hello\");\n"
            + "    }\n"
            + "    public static Object baz() {\n"
            + "        final Test test = new Test();\n"
            + "        return new Foo() {\n"
            + "            private final void bar() {\n"
            + "                try {\n"
            + "                    whee();\n"
            + "                } catch (Throwable ex) {\n"
            + "                    throw test.choke();\n"
            + "                }\n"
            + "            }\n"
            + "            private void whee() {\n"
            + "            }\n"
            + "        };\n"
            + "    }\n"
            + "    public RuntimeException choke() {\n"
            + "        return new RuntimeException(\"ack\");\n"
            + "    }\n"
            + "    private static abstract class Foo {\n"
            + "    }\n"
            + "}\n"
        ), "Test");
    }

    @Test public void
    testBug70() throws Exception {
        this.assertClassBodyCookable(
            ""
            + "public String result = \"allow\", email = null, anno = null, cnd = null, transactionID = null;\n"
            + "public String treeCode(String root) {\n"
            + "    try {\n"
            + "        return null;\n"
            + "    } catch (Exception treeEx) {\n"
            + "        treeEx.printStackTrace();\n"
            + "        result = \"allow\";\n"
            + "    }\n"
            + "    return result;\n"
            + "}\n"
        );
    }

    @Test public void
    testBug71() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class ACI {\n"
            + "    public static boolean main() {\n"
            + "        Sub s = new ACI().new Sub(new int[] { 1, 2 });\n"
            + "        return s.x == 1 && s.y == 2;\n"
            + "    }\n"
            + "    class Sub {\n"
            + "        int x, y;\n"
            + "        public Sub(int[] a) {\n"
            + "            this(a[0], a[1]);\n"
            + "        }\n"
            + "        public Sub(int x, int y) {\n"
            + "            this.x = x;\n"
            + "            this.y = y;\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        ), "ACI");
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class SCI {\n"
            + "    public static boolean main() {\n"
            + "        Sub s = new SCI().new Sub(1, 2);\n"
            + "        return s.x == 1 && s.y == 2;\n"
            + "    }\n"
            + "    class Sub extends Foo.Subb {\n"
            + "        public Sub(int x, int y) {\n"
            + "            new Foo().super(x, y);\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
            + "class Foo {\n"
            + "    class Subb {\n"
            + "        int x, y;\n"
            + "        public Subb(int x, int y) {\n"
            + "            this.x = x;\n"
            + "            this.y = y;\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        ), "SCI");
    }

    @Test public void
    testBug80() throws Exception {
        // Expression compilation is said to throw StackOverflowError!?
        this.assertExpressionUncookable("(10).total >= 100.0 ? 0.0 : 7.95");
    }

    @Test public void
    testBug81() throws Exception {
        // IncompatibleClassChangeError when invoking getClass() on interface references
        this.assertScriptExecutable(
            ""
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "\n"
            + "List list = new ArrayList();\n"
            + "System.out.println(list.getClass());\n"
        );
    }

    @Test public void
    testBug99() throws Exception {
        // ConcurrentModificationException due to instance variable of Class type initialized using a class literal
        this.assertCompilationUnitCookable("class Test{Class c = String.class;}");
    }

    @Test public void
    testBug102() throws Exception {
        // Static initializers are not executed
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class Test{\n"
            + "    static String x = \"\";\n"
            + "    static { x += 0; }\n"
            + "    static int a = 7;\n"
            + "    static { x += a; }\n"
            + "    static { System.out.println(\"HELLO\");\n }\n"
            + "    public static boolean main() {\n"
            + "        System.out.println(\">>>\" + x + \"<<<\");\n"
            + "        return x.equals(\"07\");\n"
            + "    }\n"
            + "}"
        ), "Test");

        ISimpleCompiler compiler = this.compilerFactory.newSimpleCompiler();
        compiler.cook(new StringReader("public class Test{static{System.setProperty(\"foo\", \"bar\");}}"));
        Class<?> testClass = compiler.getClassLoader().loadClass("Test"); // Only loads the class (JLS7 12.2).
        Assert.assertNull(System.getProperty("foo"));
        testClass.newInstance(); // Initializes the class (JLS7 12.4).
        Assert.assertEquals("bar", System.getProperty("foo"));
        System.getProperties().remove("foo");
        Assert.assertNull(System.getProperty("foo"));
    }

    @Test public void
    testBug105() throws Exception {
        // Possible to call a method of an enclosing class as if it was a member of an inner class
        this.assertClassBodyUncookable(
            ""
            + "class Price {\n"
            + "  public int getPriceA() {\n"
            + "    return 1;\n"
            + "  }\n"
            + "\n"
            + "  public int getPriceB() {\n"
            + "    return 2;\n"
            + "  }\n"
            + "}\n"
            + "\n"
            + "Price price;\n"
            + "\n"
            + "public int assign() {\n"
            + "  return price.rate();\n" // This should not compile.
            + "}\n"
            + "\n"
            + "int rate() {\n"
            + "  return 17;\n"
            + "}\n"
        );
    }

    @Test public void
    testBug106() throws Exception {
        this.assertJavaSourceLoadable(new File("aux-files/Bug 106"), "b.C3");
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "class MyFile extends java.io.File {\n"
            + "    public MyFile() { super(\"/my/file\"); }\n"
            + "}\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        return 0 == new MyFile().compareTo(new MyFile());\n"
            + "    }\n"
            + "}"
        ), "Main");
        this.assertScriptReturnsTrue(
            ""
            + "StringBuffer sb = new StringBuffer();\n"
            + "sb.append('(');\n"
            + "return sb.length() == 1;\n"
        );
    }

    @Test public void
    testBug147() throws Exception {
        this.assertCompilationUnitCookable(
            "public class Foo {\n"
            + "    public static void main(String[] args) {\n"
            + "        new Object() {\n"
            + "            Object bar = new Object() {\n"
            + "                public Object getObject(int i8) {\n"
            + "                    switch (i8) { case 0: return \"sss\"; }\n"
            + "                    return null;\n"
            + "                }\n"
            + "            };\n"
            + "        };\n"
            + "    }\n"
            + "}"
        );
    }

    @Test public void
    testBug149() throws Exception {

        // JLS7 3.10.6: "aaa\/bbb" contains an invalid escape sequence: "\/".
        this.assertExpressionUncookable("\"aaa\\/bbb\"");
    }

    @SuppressWarnings("deprecation") @Test(expected = AssertionError.class) public void
    testBug157() throws Exception {
        IExpressionEvaluator evaluator = this.compilerFactory.newExpressionEvaluator();
        evaluator.setReturnType(Long.class);
    }

    @Test public void
    testBug153_1() throws Exception {
        this.assertScriptExecutable("Comparable x = 5.0;");
    }

    @Test public void
    testBug153_2() throws Exception {

        // JLS7 5.5 says about casting conversion:
        //
        //    Casting contexts allow the use of:
        // ...
        //     * a boxing conversion (�5.1.7)
        //
        // , but obviously (JAVAC) a boxing conversion followed by a widening reference conversion is also
        // permitted (as for assignment conversion).
        this.assertScriptExecutable("Comparable x = (Comparable) 5.0;");
    }

    @Test public void
    testBug153_3() throws Exception {
        this.assertScriptExecutable("long x = new Integer(8);");
    }

    @Test public void
    testBug153_4() throws Exception {

        // JLS7 5.5 says about casting conversion:
        //
        //    Casting contexts allow the use of:
        // ...
        //     * an unboxing conversion (�5.1.8)
        //
        // , but obviously (JAVAC) an unboxing conversion followed by a widening primitive conversion is also
        // permitted (as for assignment conversion).
        this.assertScriptExecutable("long x = (long) new Integer(8);");
    }

    @Test public void
    testBug161_1() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class Test {\n"
            + "    public static void test2(boolean x, boolean y) {\n"
            + "        boolean v4 = x || null == null;\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testBug161_2() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class Test\n"
            + "{\n"
            + "    boolean bar  = false;\n"
            + "    public void test() {\n"
            + "        boolean v4 = bar || null == null;\n"
            + "    }\n"
            + "\n"
            + "    public static void main(String[] args) {\n"
            + "        new Test().test();\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testBug162() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class BridgeTest {\n"
            + "\n"
            + "    public static class Dad {\n"
            + "        public Object foo() { return 1; } \n"
            + "    }\n"
            + "\n"
            + "    public static class Kid extends Dad {\n"
            + "        @Override public Double foo() { return 2.3; }\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "    public static void main() {\n"
            + "        new Kid().foo();\n"
            + "    }\n"
            + "}"
        );
    }

    @Test public void
    testBug163() throws Exception {
        this.assertClassBodyCookable(
            ""
            + "import java.io.*;\n"
            + "\n"
            + "public void foo() throws IOException {\n"
            + "    if (true) {\n"
            + "        try {\n"
            + "            if (false) {\n"
            + "                throw new IOException(\"my exc\");\n"
            + "            }\n"
            + "            System.out.println(\"xyz\");\n" // <= At least one stmt.
            + "\n"
            + "        } catch (IOException e) {\n"  // <= "Catch clause is unreachable"
            + "            throw new java.lang.RuntimeException(e);\n"
            + "        }\n"
            + "    }\n"
            + "}"
        );
    }

    @Test public void
    testBug169() throws Exception {
        // SUPPRESS CHECKSTYLE LineLength:94
        this.assertCompilationUnitCookable(
            ""
            + "import java.util.*;\n"
            + "\n"
            + "class Test2 {\n"
            + "    final Object fld1 = null;\n"
            + "    public void bind(final Object param2) {\n"
            + "        fld1.hashCode();\n"
            + "        final Object lvar3 = param2;\n"
            + "        new Object() {\n"
            + "            {\n"
            + "                fld1.hashCode();\n"
            + "                param2.hashCode();\n"
            + "            }\n"
            + "            final Object fld4 = fld1.hashCode() + param2.hashCode() + lvar3.hashCode();\n"
            + "            public void meth(final Object parm5) {\n"
            + "                fld1.hashCode();\n"
            + "                param2.hashCode();\n"
            + "                lvar3.hashCode();\n"
            + "                fld4.hashCode();\n"
            + "                parm5.hashCode();\n"
            + "                final Object lvar5a = null;\n"
            + "                final Object lvar6 = new Object() {\n"
            + "                    void meth() {\n"
            + "                        fld1.hashCode();\n"
            + "                        param2.hashCode();\n"
            + "                        lvar3.hashCode();\n"
            + "                        fld4.hashCode();\n"
            + "                        parm5.hashCode();\n"
            + "                    }\n"
            + "                    final Object fld7 = fld1.hashCode() + param2.hashCode() + lvar3.hashCode() + fld4.hashCode() + parm5.hashCode() + lvar5a.hashCode() /*+ SNO lvar6.hashCode()*/;\n"
            + "                    private void dummy() { throw new AssertionError(lvar5a); }\n"
            + "                    Object fld8 = new Object() {\n"
            + "                        final Object fld9 = fld1.hashCode() + /*param2.hashCode() + lvar3.hashCode() +*/ fld4.hashCode() + /*parm5.hashCode() + lvar5a.hashCode() + lvar6.hashCode()*/ + fld7.hashCode() + fld8.hashCode();\n"
            + "                        public void apply(final Object parm10) {\n"
            + "                            fld1.hashCode();\n"
            + "    // Unknown var or type  param2.hashCode();\n"
            + "    // Unknown var or type  lvar3.hashCode();\n"
            + "                            fld4.hashCode();\n"
            + "    // Unknown var or type  parm5.hashCode();\n"
            + "    /* Unknown var or type*/  lvar5a.hashCode();\n"
            + "    // Unknown var or type  lvar6.hashCode();\n"
            + "                            fld7.hashCode();\n"
            + "                            fld8.hashCode();\n"
            + "                            fld9.hashCode();\n"
            + "                            parm10.hashCode();\n"
            + "                            final Object lvar11 = null;\n"
            + "                            final Object lvar12 = new Object() {\n"
            + "                                final Object fld13 = fld1.hashCode() + fld4.hashCode() + fld7.hashCode() + fld8.hashCode();\n"
            + "                                public void meth(final Object parm14) {\n"
            + "                                    fld1.hashCode();\n"
            + "            // Unknown var or type  param2.hashCode();\n"
            + "            // Unknown var or type  lvar3.hashCode();\n"
            + "                                    fld4.hashCode();\n"
            + "            // Unknown var or type  parm5.hashCode();\n"
            + "            // Unknown var or type  lvar6.hashCode();\n"
            + "                                    fld7.hashCode();\n"
            + "                                    fld8.hashCode();\n"
            + "                                    fld9.hashCode();\n"
            + "                                    parm10.hashCode();\n"
            + "                                    lvar11.hashCode();\n"
            + "                        // SNO      lvar12.hashCode();\n"
            + "                                    fld13.hashCode();\n"
            + "                                    parm14.hashCode();\n"
            + "                                    final Object lvar15 = fld1.hashCode() + fld4.hashCode() + fld7.hashCode() + fld8.hashCode();\n"
            + "                                    new Object() {\n"
            + "                                        void meth() {\n"
            + "                                            fld1.hashCode();\n"
            + "                   // Unknown var or type   param2.hashCode();\n"
            + "                   // Unknown var or type   lvar3.hashCode();\n"
            + "                                            fld4.hashCode();\n"
            + "                   // Unknown var or type   parm5.hashCode();\n"
            + "                   // Unknown var or type   lvar6.hashCode();\n"
            + "                                            fld7.hashCode();\n"
            + "                                            fld8.hashCode();\n"
            + "                                            fld9.hashCode();\n"
            + "                                            parm10.hashCode();\n"
            + "                                            lvar11.hashCode();\n"
            + "                          // SNO            lvar12.hashCode();\n"
            + "                                            fld13.hashCode();\n"
            + "                                            parm14.hashCode();\n"
            + "                                            lvar15.hashCode();\n"
            + "                                        }\n"
            + "                                    };\n"
            + "                                }\n"
            + "                            };\n"
            + "                        }\n"
            + "                    };\n"
            + "                };\n"
            + "            }\n"
            + "        };\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testBug172() throws Exception {
        String cnFirst = First.class.getCanonicalName();
        this.assertCompilationUnitCookable(
            ""
            + "public class Base {\n"
            + "\n"
            + "    public String foo = \"foo_field\";\n"
            + "\n"
            + "    static Object obj = new " + cnFirst + "();\n"
            + "    static int ref = ((" + cnFirst + ") obj).field1;\n"
            + "}\n"
        );
    }

    @Test public void
    testBug179() throws Exception {
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
        ee.setExpressionType(Object[].class);
        ee.cook("new Object[] { 1, \"foo\", 3, 4 }");
    }

    @Test public void
    testBug180() throws Exception {

//        System.out.println(System.getProperty("java.version"));
        String interfaceName = UnaryDoubleFunction.class.getCanonicalName();
        String script        = (
            ""
            + "package test.compiled;\n"
            + "import static java.lang.Math.*;\n"
            + "public final class JaninoCompiledFastexpr1 implements " + interfaceName + " {\n"
            + "    public double evaluate(double x) {\n"
            + "        return (2 + (7-5) * 3.14159 * x + sin(0));\n"
            + "    }\n"
            + "}"
        );
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(script);
    }
}
