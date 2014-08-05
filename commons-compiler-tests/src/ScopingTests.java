
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

import java.lang.reflect.Method;
import java.util.Collection;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.TestUtil;

// CHECKSTYLE JavadocMethod:OFF

/** Tests for the handling of 'scopes' within JANINO. */
@RunWith(Parameterized.class) public
class ScopingTests {
    private final ICompilerFactory compilerFactory;

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception { return TestUtil.getCompilerFactoriesForParameters(); }

    public
    ScopingTests(ICompilerFactory compilerFactory) { this.compilerFactory = compilerFactory; }

    @Test public void
    testProtectedAccessAcrossPackages() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package test;\n"
            + "public class Top extends for_sandbox_tests.ProtectedVariable {\n"
            + "    public class Inner {\n"
            + "        public int get() {\n"
            + "            return var;\n"
            + "        }\n"
            + "    } \n"
            + "}"
        );
    }

    @Test @Ignore("Known failure - JANINO-113") public void
    testProtectedAccessWithinPackage() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package for_sandbox_tests;\n"
            + "public class Top extends for_sandbox_tests.ProtectedVariable {\n"
            + "    public class Inner {\n"
            + "        public int get() {\n"
            + "            return var;\n"
            + "        }\n"
            + "        public void set() {\n"
            + "            var += 10;\n"
            + "        }\n"
            + "        public int getS() {\n"
            + "            return svar;\n"
            + "        }\n"
            + "        public void setS() {\n"
            + "            svar += 10;\n"
            + "        }\n"
            + "    } \n"
            + "    public Inner createInner() {\n"
            + "        return new Inner();\n"
            + "    }\n"
            + "}"
        );

        final Class<?> topClass    = sc.getClassLoader().loadClass("for_sandbox_tests.Top");
        final Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        final Object   t           = topClass.newInstance();
        final Object   i           = createInner.invoke(t, new Object[0]);

        final Class<?> innerClass = sc.getClassLoader().loadClass("for_sandbox_tests.Top$Inner");
        final Method   get        = innerClass.getDeclaredMethod("get", new Class[0]);
        final Method   getS       = innerClass.getDeclaredMethod("getS", new Class[0]);
        final Method   set        = innerClass.getDeclaredMethod("set", new Class[0]);
        final Method   setS       = innerClass.getDeclaredMethod("setS", new Class[0]);

        Object res;
        {   // non-static
            res = get.invoke(i, new Object[0]);
            Assert.assertEquals(1, res);
            set.invoke(i, new Object[0]);
            res = get.invoke(i, new Object[0]);
            Assert.assertEquals(11, res);
        }
        {   //static
            res = getS.invoke(i, new Object[0]);
            Assert.assertEquals(2, res);
            setS.invoke(i, new Object[0]);
            res = getS.invoke(i, new Object[0]);
            Assert.assertEquals(12, res);
        }
    }

    @Test @Ignore("Known failure - JANINO-113") public void
    testComplicatedSyntheticAccess() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package for_sandbox_tests;\n"
            + "public class L0 extends for_sandbox_tests.ProtectedVariable {\n"
            + "    public class L1 extends for_sandbox_tests.ProtectedVariable {\n"
            + "        public class L2 extends for_sandbox_tests.ProtectedVariable {\n"
            + "            public class Inner {\n"
            + "                public int getL2() { return L0.L1.L2.this.var; }\n"
            + "                public int getL1() { return L0.L1.this.var; }\n"
            + "                public int getL0() { return L0.this.var; }\n"
            + "                public int setL2() { return L2.this.var = 2; }\n"
            + "                public int setL1() { return L1.this.var = 1; }\n"
            + "                public int setL0() { return L0.this.var = 0; }\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "    public L0.L1.L2.Inner createInner() {\n"
            + "        return new L0().new L1().new L2().new Inner();\n"
            + "    }\n"
            + "}"
        );

        Class<?> topClass    = sc.getClassLoader().loadClass("for_sandbox_tests.L0");
        Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object   t           = topClass.newInstance();
        Object   inner       = createInner.invoke(t, new Object[0]);

        Class<?> innerClass = inner.getClass();
        Method[] gets       = new Method[] {
            innerClass.getMethod("getL0", new Class[0]),
            innerClass.getMethod("getL1", new Class[0]),
            innerClass.getMethod("getL2", new Class[0]),
        };
        Method[] sets = new Method[] {
            innerClass.getMethod("setL0", new Class[0]),
            innerClass.getMethod("setL1", new Class[0]),
            innerClass.getMethod("setL2", new Class[0]),
        };
        for (int i = 0; i < 3; ++i) {
            Object g1 = gets[i].invoke(inner, new Object[0]);
            Assert.assertEquals(1, g1);
            Object s1 = sets[i].invoke(inner, new Object[0]);
            Assert.assertEquals(i, s1);
            Object g2 = gets[i].invoke(inner, new Object[0]);
            Assert.assertEquals(i, g2);
        }
    }

    @Test @Ignore("Known failure - JANINO-113") public void
    testStaticInitAccessProtected() throws Exception {
        ISimpleCompiler sc = this.compilerFactory.newSimpleCompiler();
        sc.cook(
            ""
            + "package test;\n"
            + "public class Outer extends for_sandbox_tests.ProtectedVariable  {\n"
            + "    public class Inner {\n"
            + "        {\n"
            + "            int t = var;\n"
            + "            var = svar;\n"
            + "            svar = t;\n"
            + "        }\n"
            + "        private final int i = var;\n"
            + "        private final int j = svar;\n"
            + "        {\n"
            + "            int t = var;\n"
            + "            var = svar;\n"
            + "            svar = t;\n"
            + "        }\n"
            + "        private final int[] a = new int[] { i, j };\n"
            + "    }\n"
            + "    public Inner createInner() {\n"
            + "        return new Inner();\n"
            + "    }\n"
            + "}"
        );

        Class<?> topClass    = sc.getClassLoader().loadClass("test.Outer");
        Method   createInner = topClass.getDeclaredMethod("createInner", new Class[0]);
        Object   t           = topClass.newInstance();
        Assert.assertNotNull(t);
        Object inner = createInner.invoke(t, new Object[0]);
        Assert.assertNotNull(inner);
    }
}
