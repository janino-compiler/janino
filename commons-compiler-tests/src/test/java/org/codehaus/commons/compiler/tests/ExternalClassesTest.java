
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

// SUPPRESS CHECKSTYLE JavadocMethod:9999

package org.codehaus.commons.compiler.tests;

import java.util.Collection;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.IExpressionEvaluator;
import org.codehaus.commons.compiler.lang.ClassLoaders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import for_sandbox_tests.ExternalClass;
import util.TestUtil;

/**
 * Tests for accessing and subclassing other classes on the classpath.
 */
@RunWith(Parameterized.class) public
class ExternalClassesTest {

    private final ICompilerFactory compilerFactory;

    @Parameters(name = "CompilerFactory={0}") public static Collection<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    ExternalClassesTest(ICompilerFactory compilerFactory) {
        this.compilerFactory = compilerFactory;
    }

    @Test public void
    testForbiddenClass() throws Exception {

        // Invoke method of a class that is on the CLASSPATH of this JVM, but not on the BOOTCLASSPATH.
        try {
            IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
            ee.setParentClassLoader(ClassLoaders.BOOTCLASSPATH_CLASS_LOADER);
            ee.cook("for_sandbox_tests.ExternalClass.m1()");
            Assert.fail("Should have thrown a CompileException");
        } catch (CompileException ex) {
            ;
        }
    }

    @Test public void
    testAuxiliaryClass() throws Exception {

        // Invoke method of allowed external class.
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
        ee.cook("for_sandbox_tests.ExternalClass.m1()");

        Integer result = (Integer) ee.evaluate(new Object[0]);
        assert result != null;
        Assert.assertEquals(7, result.intValue());
    }

    @Test public void
    testExternalBaseClass() throws Exception {

        // Invoke method of base class.
        IExpressionEvaluator ee = this.compilerFactory.newExpressionEvaluator();
        ee.setExtendedClass(ExternalClass.class);
        ee.cook("m1()");

        Integer result = (Integer) ee.evaluate(new Object[0]);
        assert result != null;
        Assert.assertEquals(7, result.intValue());
    }
}

