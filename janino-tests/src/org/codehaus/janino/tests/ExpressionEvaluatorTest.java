
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

package org.codehaus.janino.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.ScriptEvaluator;
import org.junit.Test;

public
class ExpressionEvaluatorTest {

    @Test public void
    testGuessParameterNames() throws Exception {
        Set parameterNames = new HashSet(
            Arrays.asList(ExpressionEvaluator.guessParameterNames(new Scanner(null, new StringReader(
                ""
                + "import o.p;\n"
                + "a + b.c + d.e() + f() + g.h.I.j() + k.l.M"
            ))))
        );
        assertEquals(new HashSet(Arrays.asList(new String[] { "a", "b", "d" })), parameterNames);

        parameterNames = new HashSet(
            Arrays.asList(ScriptEvaluator.guessParameterNames(new Scanner(null, new StringReader(
                ""
                + "import o.p;\n"
                + "int a;\n"
                + "return a + b.c + d.e() + f() + g.h.I.j() + k.l.M;"
            ))))
        );
        assertEquals(new HashSet(Arrays.asList(new String[] { "b", "d" })), parameterNames);
    }

    /**
     * JANINO (as of now) does not support generics, and should clearly state the fact instead of throwing
     * mysterious {@link CompileException}s like 'Identifier expected'.
     */
    @Test public void
    testGenerics() {
        try {
            new ExpressionEvaluator().cook("new java.util.HashMap<String, String>()");
        } catch (CompileException ce) {
            if (ce.getMessage().contains("does not support generics")) return;
            fail("Unexpected CompileException message '" + ce.getMessage() + "'");
        }
        fail("Usage of generics should cause a CompileException");
    }
}
