
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

import java.io.StringWriter;
import java.io.Writer;

import org.codehaus.janino.util.AutoIndentWriter;
import org.junit.Assert;
import org.junit.Test;

public
class AutoIndentWriterTest {

    private static final String LS = System.getProperty("line.separator");

    @Test public void
    testIndent() throws Exception {

        StringWriter sw  = new StringWriter();
        Writer       aiw = new AutoIndentWriter(sw);

        aiw.write(
            (
                ""
                +                             "aaa\n"
                + AutoIndentWriter.INDENT   + "bbb\n"
                + AutoIndentWriter.INDENT   + "ccc\n"
                + AutoIndentWriter.UNINDENT + "ddd\n"
                + AutoIndentWriter.UNINDENT + "eee\n"
                +                             "fff\n"
            ).replace("\n", AutoIndentWriterTest.LS)
        );
        aiw.close();

        Assert.assertEquals(
            (
                ""
                + "aaa\n"
                + "    bbb\n"
                + "        ccc\n"
                + "    ddd\n"
                + "eee\n"
                + "fff\n"
            ).replace("\n", AutoIndentWriterTest.LS),
            sw.toString()
        );
    }

    @Test public void
    testTabulator() throws Exception {

        StringWriter sw  = new StringWriter();
        Writer       aiw = new AutoIndentWriter(sw);

        // SUPPRESS CHECKSTYLE Whitespace:15
        aiw.write(
            (
                ""
                +                                     "a b c\n"
                +                                     "aa \tbb \tcc\n"
                +                                     "aaa \tbbb \tccc\n"
                + AutoIndentWriter.CLEAR_TABULATORS + "aaaa \tbbbb \tcccc\n"
                +                                     "aaaaa \tbbbbb \tccccc\n"
                + AutoIndentWriter.INDENT           + "a b c\n"
                +                                     "aa \tbb \tcc\n"
                +                                     "aaa \tbbb \tccc\n"
                + AutoIndentWriter.CLEAR_TABULATORS + "aaaa \tbbbb \tcccc\n"
                +                                     "aaaaa \tbbbbb \tccccc\n"
                + AutoIndentWriter.UNINDENT         + "a b c\n"
                +                                     "aa \tbb \tcc\n"
                +                                     "aaa \tbbb \tccc\n"
                + AutoIndentWriter.CLEAR_TABULATORS + "a \tb \tc\n"
                +                                     "aa \tbb \tcc\n"
            )
            .replace("\n", AutoIndentWriterTest.LS)
            .replace("\t", Character.toString(AutoIndentWriter.TABULATOR))
        );
        aiw.close();

        Assert.assertEquals(
            (
                ""
                + "a b c\n"
                + "aa  bb  cc\n"            // Tabbing #1
                + "aaa bbb ccc\n"           // Tabbing #1
                + "aaaa  bbbb  cccc\n"      // Tabbing #2
                + "aaaaa bbbbb ccccc\n"     // Tabbing #2
                + "    a b c\n"
                + "    aa  bb  cc\n"        // Tabbing #3
                + "    aaa bbb ccc\n"       // Tabbing #3
                + "    aaaa  bbbb  cccc\n"  // Tabbing #4
                + "    aaaaa bbbbb ccccc\n" // Tabbing #4
                + "a b c\n"
                + "aa    bb    cc\n"        // Tabbing #2 continued
                + "aaa   bbb   ccc\n"       // Tabbing #2
                + "a  b  c\n"               // Tabbing #5
                + "aa bb cc\n"              // Tabbing #5
            ).replace("\n", AutoIndentWriterTest.LS),
            sw.toString()
        );
    }
}
