
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2023 Arno Unkrig. All rights reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.janino.util.AutoIndentWriter;
import org.junit.Assert;
import org.junit.Test;

public
class AutoIndentWriterTest {

    private static final String LS = System.getProperty("line.separator");

    @Test public void
    testIndent() {

        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(new AutoIndentWriter(sw));

        pw.println("aaa");
        pw.println(AutoIndentWriter.INDENT + "bbb");
        pw.println(AutoIndentWriter.INDENT + "ccc");
        pw.println(AutoIndentWriter.UNINDENT + "ddd");
        pw.println(AutoIndentWriter.UNINDENT + "eee");
        pw.println("fff");
        pw.close();

        Assert.assertEquals((
            ""
            + "aaa\n"
            + "    bbb\n"
            + "        ccc\n"
            + "    ddd\n"
            + "eee\n"
            + "fff\n"
        ).replace("\n", AutoIndentWriterTest.LS), sw.toString());
    }

    @Test public void
    testTabulator() {

        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(new AutoIndentWriter(sw));

        pw.println(                                    "a b c");
        pw.println(AutoIndentWriter.INDENT           + "a b c");
        pw.println(                                    "aa "    + AutoIndentWriter.TABULATOR + "bb "    + AutoIndentWriter.TABULATOR + "cc");
        pw.println(                                    "aaa "   + AutoIndentWriter.TABULATOR + "bbb "   + AutoIndentWriter.TABULATOR + "ccc");
        pw.println(AutoIndentWriter.CLEAR_TABULATORS + "aaaa "  + AutoIndentWriter.TABULATOR + "bbbb "  + AutoIndentWriter.TABULATOR + "cccc");
        pw.println(                                    "aaaaa " + AutoIndentWriter.TABULATOR + "bbbbb " + AutoIndentWriter.TABULATOR + "ccccc");
        pw.println(AutoIndentWriter.UNINDENT         + "a b c");
        pw.println(                                    "aa "    + AutoIndentWriter.TABULATOR + "bb "    + AutoIndentWriter.TABULATOR + "cc");
        pw.println(                                    "aaa "   + AutoIndentWriter.TABULATOR + "bbb "   + AutoIndentWriter.TABULATOR + "ccc");
        pw.println(AutoIndentWriter.CLEAR_TABULATORS + "aaaa "  + AutoIndentWriter.TABULATOR + "bbbb "  + AutoIndentWriter.TABULATOR + "cccc");
        pw.println(                                    "aaaaa " + AutoIndentWriter.TABULATOR + "bbbbb " + AutoIndentWriter.TABULATOR + "ccccc");
        pw.close();

        Assert.assertEquals((
            ""
            + "a b c\n"
            + "    a b c\n"
            + "    aa  bb  cc\n"
            + "    aaa bbb ccc\n"
            + "    aaaa  bbbb  cccc\n"
            + "    aaaaa bbbbb ccccc\n"
            + "a b c\n"
            + "aa  bb  cc\n"
            + "aaa bbb ccc\n"
            + "aaaa  bbbb  cccc\n"
            + "aaaaa bbbbb ccccc\n"
        ).replace("\n", AutoIndentWriterTest.LS), sw.toString());
    }
}
