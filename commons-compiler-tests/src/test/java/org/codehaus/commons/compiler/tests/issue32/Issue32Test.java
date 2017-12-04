
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2017 Arno Unkrig. All rights reserved.
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

package org.codehaus.commons.compiler.tests.issue32;

import java.lang.reflect.Method;
import java.util.Collection;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

// SUPPRESS CHECKSTYLE Javadoc|LineLength:9999

/**
 * Attempts to reproduce the problems described in <a href="https://github.com/janino-compiler/
 * janino/issues/32">Issue #32</a>.
 */
@RunWith(Parameterized.class) public
class Issue32Test extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    Issue32Test(ICompilerFactory compilerFactory) { super(compilerFactory); }

    public static
    class SpoofRowwise {

        public
        SpoofRowwise(int a, int b, boolean c, int d) {}

        @SuppressWarnings("static-method") public double
        getValue(SideInput a, int b) { return 0; }
    }

    public static
    class LibSpoofPrimitives {

        public static double
        dotProduct(double[] a, double[] b, int c, int d, int e) { return 0; }

        public static double
        dotProduct(double[] a, double[] b, int[] c, int d, int e, int f) { return 0; }
    }

    public static
    class FastMath { public static double exp(double x) { return x; } }

    public static
    class SideInput {
        @SuppressWarnings("static-method") public double[] values(int a) { return null; }
    }

    public static
    class RowType { public static final int ROW_AGG = 3; }

    @Test public void
    test() throws Exception {

        final String cu = (
            ""
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test;\n"
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test.FastMath;\n"
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test.RowType;\n"
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test.SideInput;\n"
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test.SpoofRowwise;\n"
            + "import org.codehaus.commons.compiler.tests.issue32.Issue32Test.LibSpoofPrimitives;\n"
            + "\n"
            + "public final class TMP14 extends SpoofRowwise {\n"
            + "  public TMP14() {\n"
            + "    super(RowType.ROW_AGG, -1, false, 0);\n"
            + "  }\n"
            + "  public void\n"
            + "  genexec(double[] a, int ai, SideInput[] b, double[] scalars, double[] c, int len, int rix) {\n"
            + "    double TMP0 = LibSpoofPrimitives.dotProduct(a, b[1].values(rix), ai, 0, len);\n"
            + "    double TMP1 = 1 / (1 + FastMath.exp(-TMP0));\n"
            + "    double TMP2 = getValue(b[0], rix);\n"
            + "    double TMP3 = (TMP1 < TMP2) ? 1 : 0;\n"
            + "    double TMP4 = 1 - 2 * TMP3;\n"
            + "    double TMP5 = TMP4 + 3;\n"
            + "    double TMP6 = TMP5 / 2;\n"
            + "    c[rix] = TMP6;\n"
            + "  }\n"
            + "  protected void\n"
            + "  genexec(double[] avals, int[] aix, int ai, SideInput[] b, double[] scalars, double[] c, int alen, int len, int rix) {\n"
            + "    double TMP7 = LibSpoofPrimitives.dotProduct(avals, b[1].values(rix), aix, ai, 0, alen);\n"
            + "    double TMP8 = 1 / (1 + FastMath.exp(-TMP7));\n"
            + "    double TMP9 = getValue(b[0], rix);\n"
            + "    double TMP10 = (TMP8 < TMP9) ? 1 : 0;\n"
            + "    double TMP11 = 1 - 2 * TMP10;\n"
            + "    double TMP12 = TMP11 + 3;\n"
            + "    double TMP13 = TMP12 / 2;\n"
            + "    c[rix] = TMP13;\n"
            + "  }\n"
            + "}"
        );

        final Throwable[] firstThrowable = new Throwable[1];
        Runnable r = new Runnable() {

            @Override public void
            run() {
                try {
                    this.run2();
                } catch (Throwable t) { // SUPPRESS CHECKSTYLE IllegalCatch
                    if (firstThrowable[0] == null) firstThrowable[0] = t;
                }
            }

            public void
            run2() throws Exception {
                for (int i = 0; i < 10 && firstThrowable[0] == null; i++) {

                    ISimpleCompiler sc = Issue32Test.this.compilerFactory.newSimpleCompiler();
                    sc.cook(cu);

                    Class<?> c = sc.getClassLoader().loadClass("TMP14");
                    Method   m = c.getDeclaredMethod("genexec", double[].class, int.class, SideInput[].class, double[].class, double[].class, int.class, int.class);

                    m.invoke(c.newInstance(), new double[1], 1, new SideInput[] { new SideInput(), new SideInput() }, new double[1], new double[4], 2, 3);
                }
            }
        };

        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            (threads[i] = new Thread(r)).start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        if (firstThrowable[0] != null) {
            AssertionError ae = new AssertionError();
            ae.initCause(firstThrowable[0]);
            throw ae;
        }
    }
}
