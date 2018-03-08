
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2018 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino.tests;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.JaninoOption;
import org.codehaus.janino.ScriptEvaluator;
import org.codehaus.janino.SimpleCompiler;
import org.codehaus.janino.UnitCompiler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

/**
 * Unit tests for the {@link SimpleCompiler}.
 */
public
class OptionsTest {

    @Before
    public void
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
     * Tests {@link JaninoOption#EXPRESSIONS_IN_TRY_WITH_RESOURCES_ALLOWED}.
     */
    @Test public void
    testExpressionsInTryWithResourcesAllowed() throws Exception {
        String script = (
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "import org.junit.Assert;\n"
            + "final int[] x = new int[1];\n"
            + "try {\n"
            + "    try (new Closeable() { public void close() { Assert.assertEquals(99, ++x[0]); } }) {\n"
            + "        Assert.assertEquals(1, ++x[0]);\n"
            + "    }\n"
            + "} catch (Exception ioe) {\n"
            + "    Assert.fail(ioe.toString());\n"
            + "}\n"
            + "Assert.assertEquals(3, ++x[0]);\n"
        );

        OptionsTest.assertScriptCompilationError("NewAnonymousClassInstance rvalue not allowed as a resource", script);

        OptionsTest.assertScriptExecutable(script, JaninoOption.EXPRESSIONS_IN_TRY_WITH_RESOURCES_ALLOWED);
    }

    private static void
    assertScriptExecutable(String script, JaninoOption... options)
        throws CompileException, InvocationTargetException {
        ScriptEvaluator se = new ScriptEvaluator();
        se.options(EnumSet.copyOf(Arrays.asList(options)));
        se.cook(script);
        se.evaluate(null);
    }

    private static void
    assertScriptCompilationError(String expectedInfix, String script, JaninoOption... options) {
        ScriptEvaluator se = new ScriptEvaluator();
        if (options.length >= 1) se.options(EnumSet.copyOf(Arrays.asList(options)));
        try {
            se.cook(script);
            Assert.fail("CompileException expected");
        } catch (CompileException ce) {
            Assert.assertTrue(
                "Compilation error message\"" + ce.getMessage() + "\" does not contain \"" + expectedInfix + "\"",
                ce.getMessage().contains(expectedInfix)
            );
        }
    }
}
