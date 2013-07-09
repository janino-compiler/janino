
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.StringReader;
import java.util.Collection;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.JaninoTestSuite;
import util.TestUtil;

// CHECKSTYLE MethodName:OFF

@RunWith(Parameterized.class) public
class ReportedBugs extends JaninoTestSuite {

    @Parameters public static Collection<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    ReportedBugs(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    @Test public void
    testBug48() throws Exception {
        sim(EXEC, (
            ""
            + "package demo;\n"
            + "public class Service {\n"
            + "    public static void test() {\n"
            + "        Broken[] dummy = new Broken[5];\n"
            + "    }\n"
            + "    class Broken {\n"
            + "    }\n"
            + "}"
        ), "demo.Service");
        sim(EXEC, (
            ""
            + "package demo;\n"
            + "public class Service {\n"
            + "    public static Broken[] test() {\n"
            + "        return null;\n"
            + "    }\n"
            + "}\n"
            + "class Broken {\n"
            + "}"
        ), "demo.Service");
    }

    @Test public void
    testBug54() throws Exception {
        scr(TRUE, (
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
        ));
        clb(COOK, (
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
        ));
        clb(COOK, (
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
        ));
        clb(COOK, (
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
        ));
        clb(COOK, (
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        break;\n"
            + "    }\n"
            + "}\n"
        ));
        clb(COOK, (
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        if (true) { break; }\n"
            + "    }\n"
            + "}\n"
        ));
        clb(COOK, (
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
        ));
        scr(EXEC, (
            ""
            + "int c = 5;\n"
            + "if (c == 5) {\n"
            + "    if (true) return;\n"
            + "} else {\n"
            + "    return;\n"
            + "}\n"
            + "int b = 3;\n" // Physically unreachable, but logically reachable, hence not a compile error.
        ));
    }

    @Test public void
    testBug55() throws Exception {
        sim(COOK, (
            ""
            + "class Junk {" + "\n"
            + "    double[][] x = { { 123.4 } };" + "\n"
            + "}"
        ), null);
    }

    @Test public void
    testBug56() throws Exception {
        scr(COOK, (
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
        ));
    }

    @Test public void
    testBug63() throws Exception {
        clb(COMP, (
            ""
            + "public static boolean main() {\n"
            + "    IPred p = new Pred();\n"
            + "    return !p.filter();\n"
            + "}\n"
        ));
        clb(TRUE, (
            ""
            + "public static boolean main() {\n"
            + "    Pred p = new Pred();\n"
            + "    return !p.filter();\n"
            + "}\n"
        ));
    }

    @Test public void
    testBug69() throws Exception {
        sim(EXEC, (
            ""
            + "public class Test {\n"
            + "    public static void test() {\n"
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
        clb(COOK, (
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
        ));
    }

    @Test public void
    testBug71() throws Exception {
        sim(TRUE, (
            ""
            + "public class ACI {\n"
            + "    public static boolean test() {\n"
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
        sim(TRUE, (
            ""
            + "public class SCI {\n"
            + "    public static boolean test() {\n"
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
        exp(COMP, "(10).total >= 100.0 ? 0.0 : 7.95");
    }

    @Test public void
    testBug81() throws Exception {
        // IncompatibleClassChangeError when invoking getClass() on interface references
        scr(EXEC, (
            ""
            + "import java.util.ArrayList;\n"
            + "import java.util.List;\n"
            + "\n"
            + "List list = new ArrayList();\n"
            + "System.out.println(list.getClass());\n"
        ));
    }

    @Test public void
    testBug99() throws Exception {
        // ConcurrentModificationException due to instance variable of Class type initialized using a class literal
        sim(COOK, "class Test{Class c = String.class;}", "Test");
    }

    @Test public void
    testBug102() throws Exception {
        // Static initializers are not executed
        sim(TRUE, (
            ""
            + "public class Test{\n"
            + "  static String x = \"\";\n"
            + "  static { x += 0; }\n"
            + "  static int a = 7;\n"
            + "  static { x += a; }\n"
            + "  static { System.out.println(\"HELLO\");\n }\n"
            + "  public static boolean test() {\n"
            + "    System.out.println(\">>>\" + x + \"<<<\");\n"
            + "    return x.equals(\"07\");\n"
            + "  }\n"
            + "}"
        ), "Test");

        ISimpleCompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler();
        compiler.cook(new StringReader("public class Test{static{System.setProperty(\"foo\", \"bar\");}}"));
        Class<?> testClass = compiler.getClassLoader().loadClass("Test"); // Only loads the class (JLS2 12.2)
        assertNull(System.getProperty("foo"));
        testClass.newInstance(); // Initializes the class (JLS2 12.4)
        assertEquals("bar", System.getProperty("foo"));
        System.getProperties().remove("foo");
        assertNull(System.getProperty("foo"));
    }

    @Test public void
    testBug105() throws Exception {
        // Possible to call a method of an enclosing class as if it was a member of an inner class
        clb(COMP, (
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
        ));
    }

    @Test public void
    testBug106() throws Exception {
        jscl("Bug 106", new File("aux-files/Bug 106"), "b.C3");
        sim(TRUE, (
            "class MyFile extends java.io.File {\n"
            + "    public MyFile() { super(\"/my/file\"); }\n"
            + "}\n"
            + "public class Main {\n"
            + "    public static boolean test() {\n"
            + "        return 0 == new MyFile().compareTo(new MyFile());\n"
            + "    }\n"
            + "}"
        ), "Main");
        scr(TRUE, (
            "StringBuffer sb = new StringBuffer();\n"
            + "sb.append('(');\n"
            + "return sb.length() == 1;\n"
        ));
    }

    @Test public void
    testBug147() throws Exception {
        sim(COOK, (
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
        ), "Foo");
    }

    @Test public void
    testBug149() throws Exception {

        // JLS3 3.10.6: "aaa\/bbb" contains an invalid escape sequence: "\/".
        exp(COMP, "\"aaa\\/bbb\"");
    }
    
    @SuppressWarnings("deprecation") @Test(expected = AssertionError.class) public void
    testBug157() throws Exception {
        IExpressionEvaluator evaluator = CompilerFactoryFactory.getDefaultCompilerFactory().newExpressionEvaluator();
        evaluator.setReturnType(Long.class);
    }

    @Test public void
    testBug153_1() throws Exception {
        scr(EXEC, "Comparable x = 5.0;");
    }

    @Test public void
    testBug153_2() throws Exception {

        // JLS3 says about casting conversion
        // (http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.5):
        //
        //    Casting contexts allow the use of:
        // ...
        //     * a boxing conversion (�5.1.7)
        //
        // , but obviously (JAVAC) a boxing conversion followed by a widening reference conversion is also
        // permitted (as for assignment conversion).
        scr(EXEC, "Comparable x = (Comparable) 5.0;");
    }

    @Test public void
    testBug153_3() throws Exception {
        scr(EXEC, "long x = new Integer(8);");
    }

    @Test public void
    testBug153_4() throws Exception {

        // JLS3 says about casting conversion
        // (http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.5):
        //
        //    Casting contexts allow the use of:
        // ...
        //     * an unboxing conversion (�5.1.8)
        //
        // , but obviously (JAVAC) an unboxing conversion followed by a widening primitive conversion is also
        // permitted (as for assignment conversion).
        scr(EXEC, "long x = (long) new Integer(8);");
    }
}
