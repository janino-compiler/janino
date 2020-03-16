
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2016 Arno Unkrig. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
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
    testPullRequest10() throws CompileException, IOException {

        String cut = (
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
            + "            return \"false\";\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        );

        AbstractCompilationUnit
        acu = new Parser(new Scanner(null, new StringReader(cut))).parseAbstractCompilationUnit();

        IClassLoader            icl        = new ClassLoaderIClassLoader(this.getClass().getClassLoader());
        UnitCompiler            uc         = new UnitCompiler(acu, icl);
        ClassFile[]             classFiles = uc.compileUnit(
            false, // debugSource
            false, // debugLines
            false  // debugVars
        );

        Assert.assertEquals(1, classFiles.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        classFiles[0].store(baos);

        // The 200-byte class file disassembles to:
        //
        //     // *** Disassembly of 'C:\workspaces\janino\janino\Foo.class'.
        //
        //     // Class file version = 50.0 (J2SE 6.0)
        //
        //     public class Foo {
        //
        //         public static String meth() {
        //             iconst_1
        //             istore          [v1]
        //             ldc             "true"
        //             areturn
        //         }
        //
        //         public Foo() {
        //             aload           [this]
        //             invokespecial   Object()
        //             return
        //         }
        //     }
        //
        // As you see, the IF statement has been optimized away.
        Assert.assertEquals(216, baos.size());
    }
}
