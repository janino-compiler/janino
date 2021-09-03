
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

// SUPPRESS CHECKSTYLE JavadocMethod|MethodName:9999

package org.codehaus.commons.compiler.tests;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ICookable;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.IScriptEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.tests.bug172.First;
import org.codehaus.commons.compiler.tests.bug180.UnaryDoubleFunction;
import org.codehaus.commons.compiler.tests.bug63.IPred;
import org.codehaus.commons.compiler.tests.bug63.Pred;
import org.codehaus.commons.compiler.util.reflect.ByteArrayClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Test cases for the bugs reported on CODEHAUS JIRA (which ceased to exist in 2015) and <a
 * href="https://github.com/janino-compiler/janino/issues">issues</a> and <a
 * href="https://github.com/janino-compiler/janino/pulls">pull requests</a> on GITHUB for project JANINO.
 */
@RunWith(Parameterized.class) public
class ReportedBugsTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    ReportedBugsTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    @SuppressWarnings("static-method") // JUNIT does not like it when "setUp()" is STATIC.
    @Before public void
    setUp() throws Exception {

        // Optionally print class file disassemblies to the console.
        if (Boolean.getBoolean("disasm")) {
            Logger scl = Logger.getLogger("org.codehaus.janino.UnitCompiler");
//            for (Handler h : scl.getHandlers()) {
//                h.setLevel(Level.FINEST);
//            }
            scl.setLevel(Level.FINEST);
        }
    }

    /**
     * StackOverflowError when using field annotations with enum members.
     *
     * @see <a href="https://github.com/janino-compiler/janino/pull/31">GITHUB pull request #31</a>
     */
    @Test public void
    testPullRequest31() throws Exception {
        this.assertCompilationUnitCookable((
            ""
            + "package demo;\n"
            + "\n"
            + "import java.io.Serializable;\n"
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation2;\n"
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation4;\n"
            + "import static org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation4Enum.SECOND;\n"
            + "\n"
            + "public class JaninoTestComponent implements Serializable {\n"
            + "\n"
            + "    public static final String CONST = \"HOWDY\";\n"
            + "    private static final long serialVersionUID = 1L;\n"
            + "\n"
            + "    @RuntimeRetainedAnnotation2(CONST)\n"
            + "    protected java.util.Date lastModifiedDate;\n"
            + "\n"
            + "    public JaninoTestComponent() {\n"
            + "            // default constructor;\n"
            + "    }\n"
            + "}\n"
        ));
    }

    @Test public void
    testPullRequest46() throws Exception {
        String params254 = (
            ""
            + "long arg000, long arg001, long arg002, long arg003, long arg004, long arg005, long arg006, long arg007, "
            + "long arg008, long arg009, long arg010, long arg011, long arg012, long arg013, long arg014, long arg015, "
            + "long arg016, long arg017, long arg018, long arg019, long arg020, long arg021, long arg022, long arg023, "
            + "long arg024, long arg025, long arg026, long arg027, long arg028, long arg029, long arg030, long arg031, "
            + "long arg032, long arg033, long arg034, long arg035, long arg036, long arg037, long arg038, long arg039, "
            + "long arg040, long arg041, long arg042, long arg043, long arg044, long arg045, long arg046, long arg047, "
            + "long arg048, long arg049, long arg050, long arg051, long arg052, long arg053, long arg054, long arg055, "
            + "long arg056, long arg057, long arg058, long arg059, long arg060, long arg061, long arg062, long arg063, "
            + "long arg064, long arg065, long arg066, long arg067, long arg068, long arg069, long arg070, long arg071, "
            + "long arg072, long arg073, long arg074, long arg075, long arg076, long arg077, long arg078, long arg079, "
            + "long arg080, long arg081, long arg082, long arg083, long arg084, long arg085, long arg086, long arg087, "
            + "long arg088, long arg089, long arg090, long arg091, long arg092, long arg093, long arg094, long arg095, "
            + "long arg096, long arg097, long arg098, long arg099, long arg100, long arg101, long arg102, long arg103, "
            + "long arg104, long arg105, long arg106, long arg107, long arg108, long arg109, long arg110, long arg111, "
            + "long arg112, long arg113, long arg114, long arg115, long arg116, long arg117, long arg118, long arg119, "
            + "long arg120, long arg121, long arg122, long arg123, long arg124, long arg125, long arg126"
        );

        this.assertClassBodyCookable("public void foo(" + params254 + ") {}");
        this.assertClassBodyUncookable("public void foo(" + params254 + ", int a) {}");
        this.assertClassBodyCookable("public static void foo(" + params254 + ", int a) {}");
        this.assertClassBodyUncookable("public static void foo(" + params254 + ", long a) {}");
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
    testBug54a() throws Exception {
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
    }

    @Test public void
    testBug54b() throws Exception {
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
    }

    @Test public void
    testBug54c() throws Exception {
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
    }

    @Test public void
    testBug54d() throws Exception {
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
    }

    @Test public void
    testBug54e() throws Exception {
        this.assertClassBodyCookable(
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        break;\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testBug54f() throws Exception {
        this.assertClassBodyCookable(
            ""
            + "public void foo() throws Exception {\n"
            + "    for (int i = 0 ; true; i++) {\n"
            + "        if (true) { break; }\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    testBug54g() throws Exception {
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
    }

    @Test public void
    testBug54h() throws Exception {
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
            + "//        System.out.println(\"hello\");\n"
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
            + "/*System.out.println(*/list.getClass()/*)*/;\n"
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
            + "//    static { System.out.println(\"HELLO\"); }\n"
            + "    public static boolean main() {\n"
            + "//        System.out.println(\">>>\" + x + \"<<<\");\n"
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
        this.assertJavaSourceLoadable(new File("src/test/resources/Bug 106"), "b.C3");
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
            ""
            + "public class Foo {\n"
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

    @SuppressWarnings("deprecation") @Test public void
    testBug157() throws Exception {
        this.compilerFactory.newExpressionEvaluator().setReturnType(Long.class);
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
            + "        } catch (IOException e) {\n"        // <= "Catch clause is unreachable"
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
            + "    final Object fld1 = \"\";\n"
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
            + "    // Unknown var or type  lvar5a.hashCode();\n"
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

    @Test public void
    testBug182() throws Exception {
        this.assertExpressionCookable("System.currentTimeMillis() == 7 ? Double.valueOf(1) : Float.valueOf(2)");
    }

    @Test public void
    testBug184() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class Test {\n"
            + "    public void foo() {\n"
//            + "        byte a = 1;\n"
//            + "        if (true) {\n"
            + "            Label: {\n"
            + "                if (false) {\n"
            + "                    break Label;\n"
            + "                }\n"
            + "                if (true) {\n"
            + "                    break Label;\n"
            + "                }\n"
            + "            }\n"
//            + "        }\n"
            + "    }\n"
            + "}\n"
        );
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/6">Issue #6: Assertion error for public static final
     * field declaration</a>
     */
    @Test public void
    testIssue6() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public enum MyEnum {\n"
            + "  A, B;\n"
            + "  Object notAnEnumConstant = new Object();\n"
            + "}\n"
        );
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/53">Issue #53: Cannot compile enums</a>
     * @throws Exception
     */
    @Test public void
    testIssue53() throws Exception {
        AbstractJavaSourceClassLoader jscl = this.compilerFactory.newJavaSourceClassLoader();

        jscl.setSourcePath(new File[] { new File("src/test/resources/issue53") });

        String cfid      = this.compilerFactory.getId();
        String fieldName = (
            "org.codehaus.janino".equals(cfid)               ? "ENUM$VALUES" :
            "org.codehaus.commons.compiler.jdk".equals(cfid) ? "$VALUES"     :
            "???"
        );

        Assert.assertTrue(jscl.loadClass("pkg1.Enum1").getDeclaredField(fieldName).getType().isArray());
        Assert.assertTrue(jscl.loadClass("pkg2.Enum1").getDeclaredField(fieldName).getType().isArray());
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/65">Issue #65:Compilation Error Messages Generated by
     * JDK</a>
     */
    @Test public void
    testIssue65() throws Exception {
        ReportedBugsTest.assertUncookableLocation(
            "File 'FILENAME', Line 5, Column 9",
            this.compilerFactory.newSimpleCompiler(),
            (
                ""
                + "public class Example {\n"
                + "    public static void main(String[] unused) {\n"
                + "\n"
                + "\n"
                + "        )\n"
                + "\n"
                + "\n"
                + "    }\n"
                + "\n"
                + "\n"
                + "}\n"
            )
        );
    }

    @Test public void
    testIssue65_tabWidth() throws Exception {
        ReportedBugsTest.assertUncookableLocation(
            "File 'FILENAME', Line 5, Column 25",
            this.compilerFactory.newSimpleCompiler(),
            (
                ""
                + "public class Example {\n"
                + "    public static void main(String[] unused) {\n"
                + "\n"
                + "\n"
                + "  \t\t\t)\n" // <= Three TABs expand to 3x8=24 columns
                + "\n"
                + "\n"
                + "    }\n"
                + "\n"
                + "\n"
                + "}\n"
            )
        );
    }

    @Test public void
    testIssue65_classBodyEvaluator() throws Exception {
        ReportedBugsTest.assertUncookableLocation(
            "File 'FILENAME', Line 4, Column 5",
            this.compilerFactory.newClassBodyEvaluator(),
            (
                ""
                + "public static void main(String[] unused) {\n"
                + "\n"
                + "\n"
                + "    )\n"
                + "\n"
                + "\n"
                + "}\n"
            )
        );
    }

    @Test public void
    testIssue65_scriptEvaluator() throws Exception {
        ReportedBugsTest.assertUncookableLocation(
            "File 'FILENAME', Line 1, Column 7",
            this.compilerFactory.newScriptEvaluator(),
            "      )   \n"
        );
    }

    @Test public void
    testIssue65_scriptEvaluators() throws Exception {
        ReportedBugsTest.assertUncookableLocation(
            "File 'FILENAME2', Line 4, Column 7",
            this.compilerFactory.newScriptEvaluator(),
            ";",
            "   ;",
            "   \r \n \r\n      )" // <= FILENAME2
        );
    }

    @Test public void
    testIssue69_IncompatibleClassChangeError_when_evaluating_against_janino9plus() throws Exception {

        try {
            IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();

            if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 8) return;
            ee.setSourceVersion(8);
            ee.setTargetVersion(8);

            ee.cook("java.util.stream.Stream.of(1, 2, 3).toArray()");
            Assert.assertArrayEquals(new Object[] { 1, 2, 3 }, (Object[]) ee.evaluate());
        } catch (CompileException ce) {
            if (ce.getMessage().contains("only available for target version")) return;
            throw ce;
        }
    }

//    @Test public void
//    testIssue69() throws Exception {
//
//        if (CommonsCompilerTestSuite.JVM_VERSION < 8) return;
//
//        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
//        ee.cook("java.util.stream.Stream.of(1, 2, 3)");
//        Assert.assertEquals("xxx", ee.evaluate());
//    }

    @Test public void
    testIssue73_Operator_Ampersand_not_defined_on_types_java_lang_Integer_int() throws Exception {
        this.assertExpressionCookable("new Integer(1)&2");
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/47">Issue #47: UnitCompiler fails with
     * CompileException when parent interface method is overridden in child interface with subtype return type</a>
     */
    @Test public void
    testIssue47() throws Exception {
        ISimpleCompiler s = this.compilerFactory.newSimpleCompiler();
        s.setDebuggingInformation(true, true, true);
        s.cook(
            ""
            + "package pkg;\n"
            + "\n"
            + "import org.codehaus.commons.compiler.tests.ReportedBugsTest.B;\n"
            + "\n"
            + "public class D {\n"
            + "    public Object one(B b) {\n"
            + "       return b.theFirstMethod().theSecondMethod();\n"
            + "    }\n"
            + "}\n"
        );
        s.getClassLoader().loadClass("pkg.D");
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/90">Issue #90: "Incompatible expression types" or
     * verification errors when using ternary expressions with null in one branch</a>
     */
    @Test public void
    testIssue90() throws Exception {
        this.assertExpressionEvaluatesTrue("1 == (false ? null : 1)");
        this.assertScriptExecutable("boolean cond = true; Integer result = cond ? null : 1;");
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/issues/102">Issue #102: "$" in class name can't be handled by
     * janino</a>
     */
    @Test public void
    testIssue102__1() throws Exception {
        this.assertExpressionEvaluatable("import java.util.Map; Map.class");
    }

    @Test public void
    testIssue102__2() throws Exception {
        this.assertExpressionEvaluatable("import java.util.Map; Map.Entry.class");
    }

    @Test public void
    testIssue102__4() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "class A$B {}\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        return A$B.class.getName().equals(\"A$B\");\n"
            + "    }\n"
            + "}\n"
        ), "Main");
    }

    @Test public void
    testIssue102__5() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "class A$$B {}\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        new A$$B();\n"
            + "        return A$$B.class.getName().equals(\"A$$B\");\n"
            + "    }\n"
            + "}\n"
        ), "Main");
    }

    @Test public void
    testIssue102__6() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "class A$B { class C$D {} }\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        return A$B.C$D.class.getName().equals(\"A$B$C$D\");\n"
            + "    }\n"
            + "}\n"
        ), "Main");
    }

    @Test public void
    testIssue102__7() throws Exception {
        this.assertCompilationUnitUncookable((
            ""
            + "class A$B { class C$D {} }\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        return A$B$C$D.class.getName().equals(\"A$B$C$D\");\n"
            + "    }\n"
            + "}\n"
        ));
    }

    @Test public void
    testIssue102__8() throws Exception {
        CompileUnit unit1 = new CompileUnit("demo.pkg1", "A$$1", (
            ""
            + "package demo.pkg1;\n"
            + "import demo.pkg2.*;\n"
            + "public class A$$1 {\n"
            + "    public static String main() { return B$1.hello(); }\n"
            + "    public static String hello() { return \"HELLO\"; }\n"
            + "}"
        ));
        CompileUnit unit2 = new CompileUnit("demo.pkg2", "B$1", (
            ""
            + "package demo.pkg2;\n"
            + "import demo.pkg1.*;\n"
            + "public class B$1 {\n"
            + "    public static String hello() { return \"HELLO\" /*A$$1.hello()*/; }\n"
            + "}"
        ));

        ClassLoader
        classLoader = this.compile(Thread.currentThread().getContextClassLoader(), unit1, unit2);

        Assert.assertEquals(
            "HELLO",
            classLoader.loadClass("demo.pkg1.A$$1").getMethod("main").invoke(null)
        );
    }

    @Test public void
    testIssue126() throws Exception {

        if (CommonsCompilerTestSuite.JVM_VERSION < 8) return;

        ISimpleCompiler s = this.compilerFactory.newSimpleCompiler();
        s.setSourceVersion(8);
        s.setTargetVersion(8);
        s.cook((
            ""
            + "package issue126;\n"
            + "\n"
            + "public interface BaseTestable {\n"
            + "   public void test();\n"
            + "}\n"
            + "\n"
            + "public interface Testable extends BaseTestable {\n"
            + "   @Override\n"
            + "   default public void test() {\n"
            + "      System.out.println(\"test\");\n"
            + "   }\n"
            + "}\n"
            + "\n"
            + "public class Test implements Testable {\n"
            + "   //blank implementation\n"
            + "}\n"
        ));

        Assert.assertNotNull(s.getClassLoader().loadClass("issue126.Test").getConstructor().newInstance());
    }

    @Test public void
    testIssue144() throws Exception {
        this.assertScriptCookable("int a = 7, b = 5; Object o = new Object[] { a,     b     };");
        this.assertScriptCookable("int a = 7, b = 5; Object o = new Object[] { a < b, b     };");
        this.assertScriptCookable("int a = 7, b = 5; Object o = new Object[] { a,     b > a };");
        this.assertScriptCookable("int a = 7, b = 5; Object o = new Object[] { a < b, b > a };");
        this.assertScriptCookable("import java.util.Map; Map<Long, Long> a;");
    }

    @Test public void
    testPullRequest146() throws Exception {
        this.assertCompilationUnitCookable((
            ""
            + "public class JaninoTest {\n"
            + "    public int func(int v) {\n"
            + "        long local_var0 = 0;\n"
            + "        switch (v) {\n"
            + "            case 0:\n"
            + "                long local_var1 = 1;\n"
            + "                local_var0 = local_var1;\n"
            + "                break;\n"
            + "\n"
            + "            default:\n"
            + "                long local_var2 = 2;\n"
            + "                local_var0 = local_var2;\n"
            + "                break;\n"
            + "        }\n"
            + "\n"
            + "        long local_var3 = 1;\n"
            + "        if (v % 4 == 0) {\n"
            + "            local_var3 += 2;\n"
            + "        }\n"
            + "        if ((local_var0 + local_var3) % 2 == 0) {\n"
            + "            return 0;\n"
            + "        } else {\n"
            + "            return 1;\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        ));
    }

    @Test public void
    testIssue151() throws Exception {

        // The original code was reduced to this snippet:
        this.assertScriptCookable((
            "double  a;\n" +       // <= Must be LONG or DOUBLE, and must not be initialized.
            "boolean b;\n" +       // <= Must be declared *after* "a".
            "b = true; a = 1;\n" + // <= In *that* order.
            "System.out.println(b);//if (b) {}\n"
        ));
    }

    public ClassLoader
    compile(ClassLoader parentClassLoader, CompileUnit... compileUnits) {

        MapResourceFinder sourceFinder = new MapResourceFinder();
        for (CompileUnit unit : compileUnits) {
            String stubFileName = unit.pkg.replace(".", "/") + "/" + unit.mainClassName + ".java";
            sourceFinder.addResource(stubFileName, unit.getCode());
        }

        // Storage for generated bytecode
        final Map<String, byte[]> classes = new HashMap<>();
        // Set up the compiler.
        ICompiler compiler = this.compilerFactory.newCompiler();
//        compiler.setClassFileFinder(ClassLoaders.getsResourceAsStream(parentClassLoader));
        compiler.setClassFileCreator(new MapResourceCreator(classes));
        compiler.setClassFileFinder(new MapResourceFinder(classes));

        // Compile all sources
        try {
            compiler.compile(sourceFinder.resources().toArray(new Resource[0]));
        } catch (CompileException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set up a class loader that finds and defined the generated classes.
        return new ByteArrayClassLoader(classes, parentClassLoader);
    }

    interface Supplier<T> { T get(); }

    public
    class CompileUnit {
        String               pkg;
        String               mainClassName;
        private final String code;

        public
        CompileUnit(String pkg, String mainClassName, String code) {
            this.pkg           = pkg;
            this.mainClassName = mainClassName;
            this.code          = code;
        }

        public String
        getCode() { return this.code; }

        @Override public String
        toString() { return "CompileUnit{ pkg=" + this.pkg + ", mainClassName='" + this.mainClassName + " }"; }
    }

    // SUPPRESS CHECKSTYLE Javadoc:2
    public interface A           { A           theFirstMethod();                           }
    public interface B extends A { @Override B theFirstMethod(); Object theSecondMethod(); }

    private static void
    assertUncookableLocation(String expected, ICookable cookable, String text) {
        try {
            cookable.cook("FILENAME", text);
            Assert.fail();
        } catch (CompileException ce) {
            Location loc = ce.getLocation();
            Assert.assertEquals(ce.toString(), expected, String.valueOf(loc));
        }
    }

    private static void
    assertUncookableLocation(String expected, IScriptEvaluator cookable, String... text) {
        try {
            if (text.length == 1) {
                cookable.cook("FILENAME", text[0]);
            } else {
                String[] fileNames = new String[text.length];
                for (int i = 0; i < text.length; i++) fileNames[i] = "FILENAME" + i;
                cookable.cook(fileNames, text);
            }
            Assert.fail();
        } catch (CompileException ce) {
            Location loc = ce.getLocation();
            Assert.assertEquals(expected, String.valueOf(loc));
        }
    }
}
