
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

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnitCompiler;
import org.codehaus.janino.util.ClassFile;
import org.junit.Assert;
import org.junit.Test;

public class GithubPullRequestsTest {

    /**
     * <a href="https://github.com/janino-compiler/janino/pull/10">Replace if condition with
     * literal if possible to simplify if statement</a>
     */
    private void
    helpTestPullRequest10(String cut, int size) throws CompileException, IOException {
        CompilationUnit cu  = new Parser(new Scanner(null, new StringReader(cut))).parseCompilationUnit();
        IClassLoader    icl = new ClassLoaderIClassLoader(this.getClass().getClassLoader());
        UnitCompiler    uc  = new UnitCompiler(cu, icl);
        ClassFile[] classFiles = uc.compileUnit(
            false, // debugSource
            false, // debugLines
            false  // debugVars
        );

        Assert.assertEquals(1, classFiles.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        classFiles[0].store(baos);

        Assert.assertEquals(size, baos.size());
    }

    @Test public void
    testPullRequest10() throws CompileException, IOException {

        String cut1 = (
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

        // The 200-byte class file disassembles to:
        //
        //     // *** Disassembly of 'C:\workspaces\janino\janino\Foo.class'.
        //
        //     // Class file version = 49.0 (J2SE 5.0)
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
 
        helpTestPullRequest10(cut1, 200);

        String cut2 = (
            ""
            + "public\n"
            + "class Foo { \n"
            + "\n"
            + "    public static String\n"
            + "    meth() {\n"
            + "\n"
            + "        boolean test = true;\n"
            + "        int test2 = 1;\n"
            + "        if (test) {\n"
            + "            return \"true\";\n"
            + "        } else {\n"
            + "            return \"false\";\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        );

        // With another local variable, the class file size increases by 2-byte.
        helpTestPullRequest10(cut2, 202);

        String cut3 = (
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
            + "            return \"false\";\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
        );

        // With another local variable, the class file size increases by 4-byte.
        helpTestPullRequest10(cut3, 204);

    }
}
