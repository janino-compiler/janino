
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.ClassFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public // SUPPRESS CHECKSTYLE Javadoc:9999
class GithubPullRequestsTest {

    @Before public void
    setUp() throws Exception {

        // Optionally print class file disassemblies to the console.
        if (Boolean.parseBoolean(System.getProperty("disasm"))) {
            Logger scl = Logger.getLogger(UnitCompiler.class.getName());
            for (Handler h : scl.getHandlers()) {
                h.setLevel(Level.FINEST);
            }
            scl.setLevel(Level.FINEST);
        }
    }

    /**
     * <a href="https://github.com/janino-compiler/janino/pull/10">Replace if condition with
     * literal if possible to simplify if statement</a>
     */
    @Test public void
    testPullRequest10a() throws Exception {

        GithubPullRequestsTest.assertDisassemblyDoesNotContain(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true;\n"
                + "        if (test) {\n"
                + "            return \"true\";\n"
                + "        } else {\n"
                + "            return \"FAIL\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "FAIL"
        );
    }

    @Test public void
    testPullRequest10b() throws Exception {

        GithubPullRequestsTest.assertDisassemblyDoesNotContain(
            (
                ""
                + "public\n"
                + "class Foo {\n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true;\n"
                + "        int     test2 = 1;\n"
                + "        if (test) {\n"
                + "            return \"true\";\n"
                + "        } else {\n"
                + "            return \"FAIL\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "FAIL"
        );
    }

    @Test public void
    testPullRequest10c() throws Exception {

        GithubPullRequestsTest.assertDisassemblyDoesNotContain(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true, test1 = false;\n"
                + "        int test3 = 1;\n"
                + "        if (test) {\n"
                + "            return \"true\";\n"
                + "        } else {\n"
                + "            return \"FAIL\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "FAIL"
        );
    }

    @Test public void
    testPullRequest10d() throws Exception {

        GithubPullRequestsTest.assertDisassemblyDoesNotContain(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true, test1 = false;\n"
                + "        boolean test3 = test;\n"
                + "        if (test) {\n"
                + "            return \"true\";\n"
                + "        } else {\n"
                + "            return \"FAIL\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "FAIL"
        );
    }

    @Test public void
    testPullRequest10e() throws Exception {

        GithubPullRequestsTest.assertDisassemblyContainsAllOf(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true, test1 = false;\n"
                + "        boolean test3 = (test = false);\n"
                + "        if (test) {\n"
                + "            return \"SUCCEED1\";\n"
                + "        } else {\n"
                + "            return \"SUCCEED2\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "SUCCEED1", "SUCCEED2"
        );
    }

    /**
     * There <em>are</em> initializers with side effects between the offending variable declarator and the IF statement.
     */
    @Test public void
    testPullRequest10f() throws Exception {

        GithubPullRequestsTest.assertDisassemblyContainsAllOf(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        boolean test = true, test1 = false;\n"
                + "        Object o = new Object();\n"
                + "        if (test) {\n"
                + "            return \"SUCCEED1\";\n"
                + "        } else {\n"
                + "            return \"SUCCEED2\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "SUCCEED1", "SUCCEED2"
        );
    }

    /**
     * There <em>are</em> initializers with side effects between the offending variable declarator and the IF statement,
     * but the variable declarator is FINAL.
     */
    @Test public void
    testPullRequest10g() throws Exception {

        GithubPullRequestsTest.assertDisassemblyDoesNotContain(
            (
                ""
                + "public\n"
                + "class Foo { \n"
                + "\n"
                + "    public static String\n"
                + "    meth() {\n"
                + "\n"
                + "        final boolean test = true, test1 = false;\n"
                + "        Object o = new Object();\n"
                + "        if (test) {\n"
                + "            return \"true\";\n"
                + "        } else {\n"
                + "            return \"FAIL\";\n"
                + "        }\n"
                + "    }\n"
                + "}\n"
            ),
            "FAIL"
        );
    }

    private static void
    assertDisassemblyContainsAllOf(String cu, String... infixes) throws Exception {
        String s = GithubPullRequestsTest.compileAndDisassemble(cu);
        for (String infix : infixes) {
            Assert.assertTrue("Disassembly does not contain \"" + infix + "\": " + s, s.contains(infix));
        }
    }

    private static void
    assertDisassemblyDoesNotContain(String cu, String... infixes) throws Exception {
        String s = GithubPullRequestsTest.compileAndDisassemble(cu);
        for (String infix : infixes) {
            Assert.assertFalse("Disassembly contains \"" + infix + "\": " + s, s.contains(infix));
        }
    }

    private static String
    compileAndDisassemble(String cu)
    throws CompileException, IOException, Exception {
        AbstractCompilationUnit
        acu = new Parser(new Scanner(null, new StringReader(cu))).parseAbstractCompilationUnit();

        IClassLoader icl        = new ClassLoaderIClassLoader(GithubPullRequestsTest.class.getClassLoader());
        UnitCompiler uc         = new UnitCompiler(acu, icl);
        ClassFile[]  classFiles = uc.compileUnit(
            false, // debugSource
            false, // debugLines
            false  // debugVars
        );

        Assert.assertEquals(1, classFiles.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        classFiles[0].store(baos);

        String s = GithubPullRequestsTest.disassemble(new ByteArrayInputStream(baos.toByteArray()));
        return s;
    }

    private static String
    disassemble(InputStream is) throws Exception {
        Class<?> dc = GithubPullRequestsTest.class.getClassLoader().loadClass("de.unkrig.jdisasm.Disassembler");

        Object       d  = dc.getConstructor().newInstance();             // Disassembler d = new Disassemlber();
        StringWriter sw = new StringWriter();
        dc.getDeclaredMethod("setOut", Writer.class).invoke(d, sw);      // dd.setOut(sw);
        dc.getDeclaredMethod("disasm", InputStream.class).invoke(d, is); // d.disasm(is);
        return sw.toString();
    }

    @Test public void
    testPullRequest148() throws Exception {
        String cu = (
            ""
            + "public class MyClass {\n"
            + "  private boolean b1;\n"
            + "\n"
            + "  public MyClass(boolean b1) {\n"
            + "    this.b1 = b1;\n"
            + "  }\n"
            + "\n"
            + "  public Object apply(Object i) {\n"
            + "    final int value_0;\n"
            + "    // The bug still exist if the if condition is 'true'. Here we use a variable\n"
            + "    // to make the test more robust, in case the compiler can eliminate the else branch.\n"
            + "    if (b1) {\n"
            + "    } else {\n"
            + "      int field_0 = 1;\n"
            + "    }\n"
            + "    // The second if-else is necessary to trigger the bug.\n"
            + "    if (b1) {\n"
            + "    } else {\n"
            + "      // The bug disappear if it's an int variable.\n"
            + "      long field_1 = 2;\n"
            + "    }\n"
            + "    value_0 = 1;\n"
            + "\n"
            + "    // The second final variable is necessary to trigger the bug.\n"
            + "    final int value_2;\n"
            + "    if (b1) {\n"
            + "    } else {\n"
            + "      int field_2 = 3;\n"
            + "    }\n"
            + "    value_2 = 2;\n"
            + "\n"
            + "    return null;\n"
            + "  }\n"
            + "}\n"
        );

        SimpleCompiler sc = new SimpleCompiler();
        sc.cook(cu);
        Class<?> c = sc.getClassLoader().loadClass("MyClass");
        Object   o = c.getConstructor(boolean.class).newInstance(true);
        Assert.assertEquals(null, c.getDeclaredMethod("apply", Object.class).invoke(o, new Object()));
    }

    /**
     * Asserts that <var>actual</var> equals one of the <var>expected</var>.
     */
    public static void
    assertEqualsOneOf(String message, Object[] expected, Object actual) {

        for (Object e : expected) {
            if (GithubPullRequestsTest.equals(e, actual)) return;
        }

        GithubPullRequestsTest.failNotEquals(message, Arrays.toString(expected), actual);
    }

    private static boolean
    equals(@Nullable Object o1, @Nullable Object o2) { return o1 == null ? o2 == null : o1.equals(o2); }

    private static void
    failNotEquals(String message, Object expected, Object actual) {
        Assert.fail(message + " " + "expected:<" + expected + "> but was:<" + actual + ">");
    }
}
