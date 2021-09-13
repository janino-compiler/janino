
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2019 Arno Unkrig. All rights reserved.
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

// SUPPRESS CHECKSTYLE JavadocMethod|MethodName:9999

package org.codehaus.commons.compiler.tests;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.CommonsCompilerTestSuite;
import util.TestUtil;

/**
 * Tests against the <a href="http://docs.oracle.com/javase/specs/">Java Language Specification, Java SE 7 Edition</a>.
 */
@RunWith(Parameterized.class) public
class JlsTest extends CommonsCompilerTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    JlsTest(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    @SuppressWarnings("static-method") // JUNIT does not like it when "setUp()" is STATIC.
    @Before public void
    setUp() throws Exception {

        // Optionally print class file disassemblies to the console.
        if (Boolean.getBoolean("disasm")) {
            Logger scl = Logger.getLogger("org.codehaus.janino.UnitCompiler");
            for (Handler h : scl.getHandlers()) h.setLevel(Level.FINEST);
            scl.setLevel(Level.FINEST);
        }
    }

    @Test public void
    test_3__Lexical_Structure() throws Exception {
        // 3.1. Lexical Structure -- Unicode
        this.assertExpressionEvaluatesTrue("'\\u00e4' == '\u00e4'");

        // 3.2. Lexical Structure -- Translations
        this.assertScriptUncookable("3--4");

        // 3.3. Lexical Structure -- Unicode Escapes
        this.assertExpressionUncookable("aaa\\u123gbbb");
        this.assertExpressionEvaluatesTrue("\"\\u0041\".equals(\"A\")");
        this.assertExpressionEvaluatesTrue("\"\\uu0041\".equals(\"A\")");
        this.assertExpressionEvaluatesTrue("\"\\uuu0041\".equals(\"A\")");
        this.assertExpressionEvaluatesTrue("\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        this.assertExpressionEvaluatesTrue("\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");

        // 3.3. Lexical Structure -- Line Terminators
        this.assertExpressionEvaluatesTrue("1//\r+//\r\n2//\n==//\n\r3");

        // 3.6. Lexical Structure -- White Space
        this.assertExpressionEvaluatesTrue("3\t\r \n==3");

        // 3.7. Lexical Structure -- Comments
        this.assertExpressionEvaluatesTrue("7/* */==7");
        this.assertExpressionEvaluatesTrue("7/**/==7");
        this.assertExpressionEvaluatesTrue("7/***/==7");
        this.assertExpressionUncookable("7/*/==7");
        this.assertExpressionEvaluatesTrue("7/*\r*/==7");
        this.assertExpressionEvaluatesTrue("7//\r==7");
        this.assertExpressionEvaluatesTrue("7//\n==7");
        this.assertExpressionEvaluatesTrue("7//\r\n==7");
        this.assertExpressionEvaluatesTrue("7//\n\r==7");
        this.assertScriptUncookable("7// /*\n\rXXX*/==7");

        // 3.8. Lexical Structure -- Identifiers
        this.assertScriptExecutable("int a;");
        this.assertScriptExecutable("int \u00e4\u00e4\u00e4;");
        this.assertScriptExecutable("int \\u0391;"); // Greek alpha
        this.assertScriptExecutable("int _aaa;");
        this.assertScriptExecutable("int $aaa;");
        this.assertScriptUncookable("int 9aaa;");
        this.assertScriptUncookable("int const;");
    }

    @Test public void
    test_3_10_1__Integer_Literals_decimal() throws Exception {
        this.assertExpressionEvaluatesTrue("17 == 17L");
    }

    @Test public void
    test_3_10_1__Integer_Literals_hex() throws Exception {
        this.assertExpressionEvaluatesTrue("255 == 0xFFl");
    }

    @Test public void
    test_3_10_1__Integer_Literals_octal() throws Exception {
        this.assertExpressionEvaluatesTrue("17 == 021L");
        this.assertExpressionUncookable(
            "17 == 029",
            "Digit '9' not allowed in octal literal|compiler.err.int.number.too.large"
        );
    }

    @Test public void
    test_3_10_1__Integer_Literals_int_range() throws Exception {
        this.assertExpressionEvaluatesTrue("2 * 2147483647 == -2");
        this.assertExpressionEvaluatesTrue("2 * -2147483648 == 0");
        this.assertExpressionUncookable("2147483648");
        this.assertExpressionEvaluatable("-2147483648");
        this.assertExpressionEvaluatesTrue("-1 == 0xffffffff");
        this.assertExpressionEvaluatesTrue("1 == -0xffffffff");
        this.assertExpressionEvaluatesTrue("-0xf == -15");

        // https://github.com/janino-compiler/janino/issues/41 :
        this.assertExpressionEvaluatesTrue("-(-2147483648) == -2147483648");
        this.assertExpressionEvaluatesTrue("- -2147483648  == -2147483648");

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION >= 7) {
            this.assertExpressionEvaluatesTrue("- -2147_483648  == -2147483648");
        }
    }

    @Test public void
    test_3_10_1__Integer_Literals_long_range() throws Exception {
        this.assertExpressionEvaluatable("9223372036854775807L");
        this.assertExpressionUncookable("9223372036854775808L");
        this.assertExpressionUncookable("9223372036854775809L");
        this.assertExpressionUncookable("99999999999999999999999999999L");
        this.assertExpressionEvaluatable("-9223372036854775808L");
        this.assertExpressionUncookable("-9223372036854775809L");

        // https://github.com/janino-compiler/janino/issues/41 :
        this.assertExpressionEvaluatesTrue("-(-9223372036854775808L) == -9223372036854775808L");
        this.assertExpressionEvaluatesTrue("- -9223372036854775808L  == -9223372036854775808L");
        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION >= 7) {
            this.assertExpressionEvaluatesTrue("- -922337_2036854775808L == -9223372036854775808L");
            this.assertExpressionUncookable("- -9223372036854775808_L == -9223372036854775808L");
        }
    }

    @Test public void
    test_3_10_1__Integer_Literals_binary() throws Exception {

        Assume.assumeFalse(this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7);

        this.assertExpressionEvaluatable("0b111");
        this.assertExpressionEvaluatesTrue("0b111 == 7");
        this.assertExpressionEvaluatesTrue("0b0000111 == 7");
        this.assertExpressionEvaluatesTrue("0b00 == 0");
        this.assertExpressionEvaluatesTrue("0b1111111111111111111111111111111 == 0x7fffffff");
        this.assertExpressionEvaluatesTrue("0b10000000000000000000000000000000 == 0x80000000");
        this.assertExpressionEvaluatesTrue("0b11111111111111111111111111111111 == 0xffffffff");
        this.assertExpressionUncookable("0b100000000000000000000000000000000");
        this.assertExpressionEvaluatesTrue("-0b1111111111111111111111111111111 == 0x80000001");
        this.assertExpressionEvaluatesTrue("-0b10000000000000000000000000000000 == 0x80000000");
        this.assertExpressionEvaluatesTrue("-0b11111111111111111111111111111111 == 1");
        this.assertExpressionUncookable("-0b100000000000000000000000000000000");
        this.assertExpressionEvaluatable("0b111111111111111111111111111111111111111111111111111111111111111L");
        this.assertExpressionEvaluatable("0b1000000000000000000000000000000000000000000000000000000000000000L");
        this.assertExpressionEvaluatable("0b1111111111111111111111111111111111111111111111111111111111111111L");
        this.assertExpressionUncookable("0b10000000000000000000000000000000000000000000000000000000000000000L");
    }

    @Test public void
    test_3_10_1__Integer_Literals_underscores() throws Exception {

        Assume.assumeFalse(this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7);

        this.assertExpressionEvaluatesTrue("1_23 == 12_3");
        this.assertExpressionEvaluatesTrue("1__3 == 13");
        this.assertExpressionUncookable("_13 == 13"); // Leading underscor not allowed
        this.assertExpressionUncookable("13_ == 13"); // Trailing underscore not allowed
        this.assertExpressionEvaluatesTrue("1_23L == 12_3L");
        this.assertExpressionEvaluatesTrue("1__3L == 13L");
        this.assertExpressionUncookable("_13L == 13L"); // Leading underscor not allowed
        this.assertExpressionUncookable("13_L == 13L"); // Trailing underscore not allowed
    }

    @Test public void
    test_3_10_2__Floating_Point_Literals_float() throws Exception {
        this.assertExpressionEvaluatesTrue("1e1f == 10f");
        this.assertExpressionEvaluatesTrue("1E1F == 10f");
        this.assertExpressionEvaluatesTrue(".3f == 0.3f");
        this.assertExpressionEvaluatesTrue("0f == (float) 0");
        this.assertExpressionEvaluatable("3.14f");
        this.assertExpressionEvaluatable("3.40282347e+38f");
        this.assertExpressionUncookable("3.40282357e+38f");
        this.assertExpressionEvaluatable("1.40239846e-45f");
        this.assertExpressionUncookable("7.0e-46f");
    }

    @Test public void
    test_3_10_2__Floating_Point_Literals_double() throws Exception {
        this.assertExpressionEvaluatable("1.79769313486231570e+308D");
        this.assertExpressionUncookable("1.79769313486231581e+308d");
        this.assertExpressionEvaluatable("4.94065645841246544e-324D");
        this.assertExpressionUncookable("2e-324D");
    }

    /**
     * Hex float literals, JLS8 3.10.2
     */
    @Test public void
    test_3_10_2__Floating_Point_Literals_hexadecimal() throws Exception {
        this.assertExpressionEvaluatesTrue("0x1D           == 29"); // "D" is NOT a float type suffix, but a hex digit!
        this.assertExpressionEvaluatesTrue("0x1p0D         == 1");
        this.assertExpressionEvaluatesTrue("0x.8p0         == 0.5");
        this.assertExpressionEvaluatesTrue("0x1p0          == 1");
        this.assertExpressionEvaluatesTrue("0xfp1          == 30");
        this.assertExpressionEvaluatesTrue("0xfp+1         == 30");
        this.assertExpressionEvaluatesTrue("0xfp-1         == 7.5");
        this.assertExpressionEvaluatesTrue("0x1.0004p0F    == 0x1.0004p0");
        this.assertExpressionEvaluatesTrue("0x1.0000004p0F != 0x1.0000004p0");
    }

    @Test public void
    test_3_10_2__Floating_Point_Literals_underscores() throws Exception {

        Assume.assumeFalse(this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7);

        this.assertExpressionEvaluatesTrue("1___0.1___0 == 10.1");
    }

    @Test public void
    test_3_10_3__Boolean_Literals() throws Exception {
        this.assertExpressionEvaluatesTrue("true");
        this.assertExpressionEvaluatesTrue("! false");
    }

    @Test public void
    test_3_10_4__Character_Literals() throws Exception {
        this.assertExpressionEvaluatesTrue("'a' == 97");
        this.assertExpressionUncookable("'''");
        this.assertExpressionUncookable("'\\'");
        this.assertExpressionUncookable("'\n'");
        this.assertExpressionUncookable("'ax'");
        this.assertExpressionUncookable("'a\n'");
        this.assertExpressionEvaluatesTrue("'\"' == 34"); // Unescaped double quote is allowed!
    }

    @Test public void
    test_3_10_5__String_Literals() throws Exception {
        this.assertExpressionEvaluatesTrue("\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        this.assertExpressionEvaluatesTrue("\"\\b\".charAt(0) == 8");
        this.assertExpressionUncookable("\"aaa\nbbb\"");
        this.assertExpressionUncookable("\"aaa\rbbb\"");
        this.assertExpressionEvaluatesTrue("\"aaa\" == \"aaa\"");
        this.assertExpressionEvaluatesTrue("\"aaa\" != \"bbb\"");
    }

    @Test public void
    test_3_10_6__Escape_sequences_for_character_and_string_literals() throws Exception {
        this.assertExpressionUncookable("'\\u000a'"); // 0x000a is LF
        this.assertExpressionEvaluatesTrue("'\\b' == 8");
        this.assertExpressionEvaluatesTrue("'\\t' == 9");
        this.assertExpressionEvaluatesTrue("'\\n' == 10");
        this.assertExpressionEvaluatesTrue("'\\f' == 12");
        this.assertExpressionEvaluatesTrue("'\\r' == 13");
        this.assertExpressionEvaluatesTrue("'\\\"' == 34");
        this.assertExpressionEvaluatesTrue("'\\'' == 39");
        this.assertExpressionEvaluatesTrue("'\\\\' == 92");
        this.assertExpressionEvaluatesTrue("'\\0' == 0");
        this.assertExpressionEvaluatesTrue("'\\07' == 7");
        this.assertExpressionEvaluatesTrue("'\\377' == 255");
        this.assertExpressionUncookable("'\\400'");
        this.assertExpressionUncookable("'\\1234'");
    }

    @Test public void
    test_3_10_7__The_null_literal() throws Exception {
        this.assertExpressionEvaluatable("null");
    }

    @Test public void
    test_3_11__Separators() throws Exception {
        this.assertScriptExecutable(";");
    }

    @Test public void
    test_3_12__Operators() throws Exception {
        this.assertScriptReturnsTrue("int a = -11; a >>>= 2; return a == 1073741821;");
    }

    @Test public void
    test_4_5_1__Type_arguments_and_wildcards() throws Exception {
        this.assertScriptReturnsTrue(
            ""
            + "import java.util.*;\n"
            + "final List<String> l = new ArrayList();\n"
            + "l.add(\"x\");\n"
            + "final Iterator<String> it = l.iterator();\n"
            + "return it.hasNext() && \"x\".equals(it.next()) && !it.hasNext();"
        );
    }

    @Test public void
    test_5_1_7__Boxing_conversion() throws Exception {
        this.assertScriptReturnsTrue("Boolean   b = true;        return b.booleanValue();");
        this.assertScriptReturnsTrue("Boolean   b = false;       return !b.booleanValue();");
        this.assertScriptReturnsTrue("Byte      b = (byte) 7;    return b.equals(new Byte((byte) 7));");
        this.assertScriptReturnsTrue("Character c = 'X';         return c.equals(new Character('X'));");
        this.assertScriptReturnsTrue("Short     s = (short) 322; return s.equals(new Short((short) 322));");
        this.assertScriptReturnsTrue("Integer   i = 99;          return i.equals(new Integer(99));");
        this.assertScriptReturnsTrue("Long      l = 733L;        return l.equals(new Long(733L));");
        this.assertScriptReturnsTrue("Float     f = 12.5F;       return f.equals(new Float(12.5F));");
        this.assertScriptReturnsTrue("Double    d = 14.3D;       return d.equals(new Double(14.3D));");
    }

    @Test public void
    test_5_1_8__Unboxing_conversion() throws Exception {
        this.assertExpressionEvaluatesTrue("Boolean.TRUE");
        this.assertExpressionEvaluatesTrue("!Boolean.FALSE");
        this.assertExpressionEvaluatesTrue("new Byte((byte) 9) == (byte) 9");
        this.assertExpressionEvaluatesTrue("new Character('Y') == 'Y'");
        this.assertExpressionEvaluatesTrue("new Short((short) 33) == (short) 33");
        this.assertExpressionEvaluatesTrue("new Integer(-444) == -444");
        this.assertExpressionEvaluatesTrue("new Long(987654321L) == 987654321L");
        this.assertExpressionEvaluatesTrue("new Float(33.3F) == 33.3F");
        this.assertExpressionEvaluatesTrue("new Double(939.939D) == 939.939D");
    }

    @Test public void
    test_5_2__Assignment_conversion() throws Exception {
        this.assertScriptReturnsTrue("int i = 7; return i == 7;");
        this.assertScriptReturnsTrue("String s = \"S\"; return s.equals(\"S\");");
        this.assertScriptReturnsTrue("long l = 7; return l == 7L;");
        this.assertScriptReturnsTrue("Object o = \"A\"; return o.equals(\"A\");");
        this.assertScriptReturnsTrue("Integer i = 7; return i.intValue() == 7;");
        this.assertScriptReturnsTrue("Object o = 7; return o.equals(new Integer(7));");
        this.assertScriptReturnsTrue("int i = new Integer(7); return i == 7;");
        this.assertScriptReturnsTrue("long l = new Integer(7); return l == 7L;");
        this.assertScriptExecutable("byte b = -128;");
        this.assertScriptUncookable("byte b = 128;");
        this.assertScriptExecutable("short s = -32768;");
        this.assertScriptUncookable("short s = 32768;");
        this.assertScriptUncookable("char c = -1;");
        this.assertScriptExecutable("char c = 0;");
        this.assertScriptExecutable("char c = 65535;");
        this.assertScriptUncookable("char c = 65536;");
        this.assertScriptExecutable("Byte b = -128;");
        this.assertScriptUncookable("Byte b = 128;");
        this.assertScriptExecutable("Short s = -32768;");
        this.assertScriptUncookable("Short s = 32768;");
        this.assertScriptUncookable("Character c = -1;");
        this.assertScriptExecutable("Character c = 0;");
        this.assertScriptExecutable("Character c = 65535;");
        this.assertScriptUncookable("Character c = 65536;");
    }

    @Test public void
    test_5_5__Casting_conversion() throws Exception {
        this.assertExpressionEvaluatesTrue("7 == (int) 7");
        this.assertExpressionEvaluatesTrue("(int) 'a' == 97");
        this.assertExpressionEvaluatesTrue("(int) 10000000000L == 1410065408");
        this.assertExpressionEvaluatesTrue("((Object) \"SS\").equals(\"SS\")");
        this.assertScriptReturnsTrue("Object o = \"SS\"; return ((String) o).length() == 2;");
        this.assertExpressionEvaluatesTrue("((Integer) 7).intValue() == 7");
        this.assertExpressionEvaluatesTrue("(int) new Integer(7) == 7");

        // Boxing conversion followed by widening reference conversion - not described in JLS7, but supported by
        // JAVAC. See JLS7 5.1.7, and JANINO-153.
        this.assertExpressionEvaluatesTrue("null != (Comparable) 5.0");

        // Unboxing conversion followed by widening primitive conversion - not described in JLS7, but supported by
        // JAVAC. See JLS7 5.1.7, and JANINO-153.
        this.assertExpressionEvaluatesTrue("0L != (long) new Integer(8)");
    }

    @Test public void
    test_5_6__Number_promotions() throws Exception {
        // 5.6.1 Unary Numeric Promotion
        this.assertExpressionEvaluatesTrue("-new Byte((byte) 7) == -7");
        this.assertExpressionEvaluatesTrue("-new Double(10.0D) == -10.0D");
        this.assertScriptReturnsTrue("char c = 'a'; return -c == -97;");

        // 5.6.2 Binary Numeric Promotion
        this.assertExpressionEvaluatesTrue("2.5D * new Integer(4) == 10D");
        this.assertExpressionEvaluatesTrue("7 % new Float(2.5F) == 2F");
        this.assertExpressionEvaluatesTrue("2000000000 + 2000000000L == 4000000000L");
        this.assertExpressionEvaluatesTrue("(short) 32767 + (byte) 100 == 32867");
    }

    @Test public void
    test_6_6_1__Determining_Accessibility_member_access() throws Exception {

        // SUPPRESS CHECKSTYLE Whitespace|LineLength:4
        this.assertExpressionEvaluatesTrue("for_sandbox_tests.ClassWithFields.publicField        == 1");
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.protectedField     == 2", "Protected member cannot be accessed|compiler.err.report.access");
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.packageAccessField == 3", "Member with \"package\" access cannot be accessed|compiler.err.not.def.public.cant.access");
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.privateField       == 4", "Private member cannot be accessed|compiler.err.report.access");
    }

    @Test public void
    test_7_5__Import_declarations() throws Exception {

        // Default imports
        this.assertExpressionEvaluatesTrue(
            "import java.util.*; new ArrayList().getClass().getName().equals(\"java.util.ArrayList\")"
        );
        this.assertScriptUncookable("import java.util#;");
        this.assertScriptUncookable("import java.util.9;");
        this.assertScriptCookable("import java.util.*;");
        this.assertClassBodyMainReturnsTrue(
            "import java.util.*; public static boolean main() { return new ArrayList() instanceof List; }"
        );
        this.assertExpressionUncookable("import java.io.*; new ArrayList()");

        // 7.5.1 Import Declarations -- Single-Type-Import
        this.assertExpressionEvaluatable("import java.util.ArrayList; new ArrayList()");
        this.assertExpressionEvaluatable("import java.util.ArrayList; import java.util.ArrayList; new ArrayList()");
        this.assertScriptUncookable("import java.util.List; import java.awt.List;");

        // 7.5.2 Import Declarations -- Import-on-Demand
        this.assertExpressionEvaluatable("import java.util.*; new ArrayList()");
        this.assertExpressionEvaluatable("import java.util.*; import java.util.*; new ArrayList()");

        // 7.5.3 Import Declarations -- Single Static Import
        this.assertExpressionEvaluatesTrue(
            "import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set"
        );
        this.assertExpressionEvaluatesTrue(
            "import static java.util.Collections.EMPTY_SET;"
            + "import static java.util.Collections.EMPTY_SET;"
            + "EMPTY_SET instanceof java.util.Set"
        );
        this.assertScriptExecutable("import static java.util.Map.Entry; Entry e;");
        this.assertScriptExecutable(
            "import static java.util.Map.Entry;"
            + "import static java.util.Map.Entry;"
            + "Entry e;"
        );
        this.assertScriptUncookable(
            "import static java.util.Map.Entry;"
            + "import static java.security.KeyStore.Entry;"
            + "Entry e;"
        );
        this.assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
        this.assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.asList;"
            + "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
        this.assertScriptUncookable(
            "import static java.lang.Integer.decode;"
            + "import static java.lang.Long.decode;"
            + "decode(\"4000000000\");"
        );

        // 7.5.4 Import Declarations -- Static-Import-on-Demand
        this.assertExpressionEvaluatesTrue("import static java.util.Collections.*; EMPTY_SET instanceof java.util.Set");
        this.assertScriptExecutable("import static java.util.Map.*; Entry e;");
        this.assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.*;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
    }

    @Test public void
    test_8_1_1__Class_Modifiers() throws Exception {

        // Modifiers for package member class: SUPPRESS CHECKSTYLE LineLength:18
        this.assertCompilationUnitCookable("@SuppressWarnings(\"foo\") class Foo {}");
        this.assertCompilationUnitCookable("abstract                   class Foo {}");
        this.assertCompilationUnitCookable("final                      class Foo {}");
        this.assertCompilationUnitCookable("public                     class Foo {}");
        this.assertCompilationUnitCookable("strictfp                   class Foo {}");
        this.assertCompilationUnitUncookable("default                    class Foo {}", "default not allowed|expected");
        this.assertCompilationUnitUncookable("native                     class Foo {}", "native not allowed");
        this.assertCompilationUnitUncookable("private                    class Foo {}", "private not allowed");
        this.assertCompilationUnitUncookable("protected                  class Foo {}", "protected not allowed");
        this.assertCompilationUnitUncookable("static                     class Foo {}", "static not allowed");
        this.assertCompilationUnitUncookable("synchronized               class Foo {}", "synchronized not allowed");
        this.assertCompilationUnitUncookable("transient                  class Foo {}", "transient not allowed");
        this.assertCompilationUnitUncookable("volatile                   class Foo {}", "volatile not allowed");
        this.assertCompilationUnitUncookable("@SuppressWarnings(\"foo\") @SuppressWarnings(\"bar\")  class Foo {}", "(?i)duplicate annotation|is not .* repeatable");
        this.assertCompilationUnitUncookable("public protected           class Foo {}", "allowed");
        this.assertCompilationUnitUncookable("protected private          class Foo {}", "allowed");
        this.assertCompilationUnitUncookable("private public             class Foo {}", "allowed");
        this.assertCompilationUnitUncookable("abstract final             class Foo {}", "Only one of abstract final is allowed|illegal combination");
    }

    @Test public void
    test_8_1_2__Generic_Classes_and_Type_Parameters() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class Bag<T> {\n"
            + "\n"
            + "    private T contents;\n"
            + "\n"
            + "    void put(T element) { this.contents = element; }\n"
            + "\n"
            + "    T get() { return this.contents; }\n"
            + "\n"
            + "    public static boolean main() {\n"
            + "        Bag<String> b = new Bag<String>();\n"
            + "        b.put(\"FOO\");\n"
            + "        String s = (String) b.get();\n"
            + "        return \"FOO\".equals(s);\n"
            + "    }\n"
            + "}\n"
        ), "Bag");
    }

    @Test public void
    test_8_4_8_3__Requirements_in_Overriding_and_Hiding() throws Exception {

        this.assertClassBodyExecutable(
            ""
            + "public static interface FirstCloneable extends Cloneable {\n"
            + "   public Object clone() throws CloneNotSupportedException;\n"
            + "}\n"
            + "\n"
            + "public static interface SecondCloneable extends Cloneable {\n"
            + "    public SecondCloneable clone() throws CloneNotSupportedException;\n"
            + "}\n"
            + "\n"
            + "public static abstract class BaseClone implements FirstCloneable, SecondCloneable {\n"
            + "    @Override public BaseClone clone() throws CloneNotSupportedException {\n"
            + "        return (BaseClone)super.clone();\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "public static class KidClone extends BaseClone {}\n"
            + "\n"
            + "public static void main() throws Exception {\n"
            + "    new KidClone().clone();\n"
            + "}\n"
        );

        this.assertExpressionUncookable("new Object() { public void toString() {}}.toString()", "incompatible");
    }

    @Test public void
    test_8_6__Instance_Initializers() throws Exception {

        this.assertClassBodyMainReturnsTrue((
            ""
            + "public static boolean main() { return new " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + "().inited; }\n"
            + "boolean inited;\n"
            + "{ this.inited = true; }\n"
        ));

        // Inistance initializer with local variable.
        // See issue #89.
        this.assertClassBodyMainReturnsTrue((
            ""
            + "public static boolean main() { return new " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + "().inited; }\n"
            + "boolean inited;\n"
            + "{ boolean b = true; this.inited = b; }\n"
        ));
    }

    @Test public void
    test_8_7__Static_Initializers() throws Exception {

        this.assertClassBodyMainReturnsTrue((
            ""
            + "public static boolean main() { return " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + ".inited; }\n"
            + "static boolean inited;\n"
            + "static { " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + ".inited = true; }\n"
        ));

        // Static initializer with local variable.
        this.assertClassBodyMainReturnsTrue((
            ""
            + "public static boolean main() { return " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + ".inited; }\n"
            + "static boolean inited;\n"
            + "static { boolean b = true; " + IClassBodyEvaluator.DEFAULT_CLASS_NAME + ".inited = b; }\n"
        ));
    }

    @Test public void
    test_8_9__Enums__1() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public\n"
            + "enum Color { RED, GREEN, BLACK }\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static Object\n"
            + "    main() {\n"
            + "        if (Color.RED.ordinal()   != 0) return 1;\n"
            + "        if (Color.GREEN.ordinal() != 1) return 2;\n"
            + "        if (Color.BLACK.ordinal() != 2) return 3;\n"
            + "        if (!\"RED\".equals(Color.RED.toString()))     return Color.RED.toString();\n"
            + "        if (!\"GREEN\".equals(Color.GREEN.toString())) return 5;\n"
            + "        if (!\"BLACK\".equals(Color.BLACK.toString())) return 6;\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_8_9__Enums__2() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public\n"
            + "enum Shape {\n"
            + "\n"
            + "    SQUARE(1, 2),\n"
            + "    CIRCLE(3),\n"
            + "    ;\n"
            + "\n"
            + "    private final int length;\n"
            + "    private final int width;\n"
            + "\n"
            + "    Shape(int length, int width) {\n"
            + "        this.length = length;\n"
            + "        this.width = width;\n"
            + "    }\n"
            + "\n"
            + "    Shape(int size) {\n"
            + "        this.length =  this.width = size;\n"
            + "    }\n"
            + "    public int length() { return this.length; }\n"
            + "    public int width() { return this.width; }\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static Object\n"
            + "    main() {\n"
            + "        if (Shape.SQUARE.ordinal() != 0) return 100 + Shape.SQUARE.ordinal();\n"
            + "        if (Shape.CIRCLE.ordinal() != 1) return 200 + Shape.CIRCLE.ordinal();\n"
            + "\n"
            + "        if (!\"SQUARE\".equals(Shape.SQUARE.toString())) return 3;\n"
            + "        if (!\"CIRCLE\".equals(Shape.CIRCLE.toString())) return 4;\n"
            + "\n"
            + "        if (Shape.SQUARE.length() != 1) return 5;\n"
            + "        if (Shape.SQUARE.width()  != 2) return 6;\n"
            + "        if (Shape.CIRCLE.length() != 3) return 7;\n"
            + "        if (Shape.CIRCLE.width()  != 3) return 8;\n"
            + "\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_8_9__Enums__valueOf() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public\n"
            + "enum Shape {\n"
            + "\n"
            + "    SQUARE,\n"
            + "    CIRCLE,\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static Object\n"
            + "    main() {\n"
            + "        if (Shape.valueOf(\"SQUARE\") != Shape.SQUARE) return \"100\" + Shape.valueOf(\"SQUARE\");\n"
            + "        if (Shape.valueOf(\"CIRCLE\") != Shape.CIRCLE) return \"200\" + Shape.valueOf(\"CIRCLE\");\n"
            + "        try {\n"
            + "            Shape.valueOf(\"SQUARE \");\n"
            + "            return 500;\n"
            + "        } catch (IllegalArgumentException iae) {\n"
            + "            ;\n"
            + "        }\n"
            + "\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_8_9__Enums__values() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public\n"
            + "enum Shape {\n"
            + "\n"
            + "    SQUARE,\n"
            + "    CIRCLE,\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static Object\n"
            + "    main() {\n"
            + "        Shape[] ss = Shape.values();\n"
            + "        if (ss == null)            return 100;\n"
            + "        if (ss.length != 2)        return 200 + ss.length;\n"
            + "        if (ss[0] == null)         return 300;\n"
            + "        if (ss[0] != Shape.SQUARE) return 400 + ss[0].toString();\n"
            + "        if (ss[1] == null)         return 500;\n"
            + "        if (ss[1] != Shape.CIRCLE) return 600 + ss[0].toString();\n"
            + "\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_4__Method_Declarations__1() throws Exception {

        this.assertCompilationUnitCookable((
            ""
            + "public interface A {\n"
            + "    A meth1();\n"
            + "}\n"
        ));
    }

    @Test public void
    test_9_4__Method_Declarations__2() throws Exception {

        // Static interface methods MUST declare a body.
        this.assertCompilationUnitUncookable((
            ""
            + "public interface A {\n"
            + "    static A meth1();\n"
            + "}\n"
        ));
    }

    @Test public void
    test_9_4__Method_Declarations__3() throws Exception {

        String cu = (
            ""
            + "public interface A {\n"
            + "    static A meth1() { return null; }\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static Object\n"
            + "    main() {\n"
            + "        return A.meth1() == null;\n"
            + "    }\n"
            + "}\n"
        );

        this.assertCompilationUnitCookable(cu, "only available for target version 8\\+|compiler\\.err\\.mod\\.not\\.allowed\\.here");
    }

    @Test public void
    test_9_4__Method_Declarations__4() throws Exception {

        // Default interface methods - a Java 8 feature.

    	String cu = (
    	    ""
            + "public interface A { default boolean isTrue() { return true; } }\n"
            + "public class B implements A {}\n"
            + "public class Foo { public static boolean main() { return new B().isTrue(); } }\n"
        );

    	{
    	    SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(7);
            sct.assertUncookable("Default interface methods only available for source version 8+|default methods are not supported in -source (1\\.)?7");
    	}

    	{
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(8);
            sct.setTargetVersion(8);
            if (CommonsCompilerTestSuite.JVM_VERSION < 8) {
                sct.assertCookable();
            } else {
                sct.assertResultTrue();
            }
        }
    }

    @Test public void
    test_9_4__Method_Declarations__5() throws Exception {

        // Modifiers for interface method: SUPPRESS CHECKSTYLE LineLength:18
        this.assertCompilationUnitCookable("interface Foo { @SuppressWarnings(\"foo\") void meth();   }");
        this.assertCompilationUnitCookable("interface Foo { abstract                   void meth();   }");
        this.assertCompilationUnitCookable("interface Foo { public                     void meth();   }");
        this.assertCompilationUnitCookable("interface Foo { static                     void meth() {} }", "only available for target version 8\\+|modifier static not allowed");
        this.assertCompilationUnitUncookable("interface Foo { final                      void meth();   }", "final not allowed|illegal combination");
        this.assertCompilationUnitUncookable("interface Foo { native                     void meth();   }", "native not allowed");
        this.assertCompilationUnitUncookable("interface Foo { protected                  void meth();   }", "protected not allowed");
        this.assertCompilationUnitUncookable("interface Foo { strictfp                   void meth();   }", "strictfp (not|only) allowed");
        this.assertCompilationUnitUncookable("interface Foo { synchronized               void meth();   }", "synchronized not allowed");
        this.assertCompilationUnitUncookable("interface Foo { transient                  void meth();   }", "transient not allowed");
        this.assertCompilationUnitUncookable("interface Foo { volatile                   void meth();   }", "volatile not allowed");
        this.assertCompilationUnitUncookable("interface Foo { @SuppressWarnings(\"foo\") @SuppressWarnings(\"bar\") void meth(); }", "(?i)duplicate.*annotation");
        this.assertCompilationUnitUncookable("interface Foo { public protected           void meth();   }", "allowed");
        this.assertCompilationUnitUncookable("interface Foo { protected private          void meth();   }", "allowed");
        this.assertCompilationUnitUncookable("interface Foo { private public             void meth();   }", "allowed|(?i)illegal\\.combination");
        this.assertCompilationUnitUncookable("interface Foo { abstract final             void meth();   }", "Only one of abstract final is allowed|illegal combination|not allowed");
    }

    @Test public void
    test_9_4__Method_Declarations__Default_methods() throws Exception {

        // Default methods (a Java 8 feature).
        String cu = (
            ""
            + "public interface MyInterface { default boolean isTrue() { return true; } }\n"
            + "public class Foo { public static boolean main() { return new MyInterface() {}.isTrue(); } }\n"
        );

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(7);
            sct.assertUncookable("Default interface methods only available for source version 8+|default methods are not supported in -source (1\\.)?7");
        }

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(8);
            sct.setTargetVersion(8);
            if (CommonsCompilerTestSuite.JVM_VERSION < 8) {
                sct.assertCookable();
            } else {
                sct.assertResultTrue();
            }
        }
    }

    @Test public void
    test_9_4__Method_Declarations__Static_interface_methods() throws Exception {

        // Static interface methods (a Java 8 feature).

        String cu = (
            ""
            + "public interface MyInterface { static boolean isTrue() { return true; } }\n"
            + "public class Foo { public static boolean main() { return MyInterface.isTrue(); } }\n"
        );

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(7);
            sct.assertUncookable("Static interface methods only available for source version 8+|static interface methods are not supported in -source (1\\.)?7");
        }

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(8);
            sct.setTargetVersion(8);
            if (CommonsCompilerTestSuite.JVM_VERSION < 8) {
                sct.assertCookable();
            } else {
                sct.assertResultTrue();
            }
        }
    }

    @Test public void
    test_9_4__Method_Declarations__Private_interface_methods() throws Exception {

        // Static interface methods (a Java 8 feature).

        String cu = (
            ""
                + "public interface MyInterface {\n"
                + "    private boolean isTrue2() { return true; }\n"
                + "    default boolean isTrue() { return isTrue2(); }\n"
                + "}\n"
                + "public class Foo { public static boolean main() { return new MyInterface() {}.isTrue(); } }\n"
            );

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(8);
            sct.assertUncookable("Private interface methods only available for target version 9\\+|private interface methods are not supported in -source 8");
        }

        {
            SimpleCompilerTest sct = new SimpleCompilerTest(cu, "Foo");
            sct.setSourceVersion(9);
            sct.setTargetVersion(9);
            if (CommonsCompilerTestSuite.JVM_VERSION < 9) {
                sct.assertCookable();
            } else {
                sct.assertResultTrue();
            }
        }
    }

    @Test public void
    test_9_6__Annotation_Types() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public\n"
            + "@interface MyAnno {\n"
            + "}\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() throws Exception {\n"
            + "        Class c = Main.class.getClassLoader().loadClass(\"MyAnno\");\n"
            + "//        System.out.println(c.getModifiers());\n"
            + "        return c.getModifiers() == 0x2601;\n" // 2000=ANNOTATION, 400=ABSTRACT, 200=INTERFACE, 1=PUBLIC
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_6_2__Defaults_for_annotation_type_elements() throws Exception {
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import java.lang.annotation.Retention;\n"
            + "import java.lang.annotation.RetentionPolicy;\n"
            + "\n"
            + "@Retention(RetentionPolicy.RUNTIME) public\n"
            + "@interface MyAnno { boolean value() default true; }\n"
            + "\n"
            + "@MyAnno public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() throws Exception {\n"
            + "        Class mc = Main.class;\n"
            + "//        System.out.printf(\"mc=%s%n\", mc.toString());\n"
            + "        Class ac = MyAnno.class;\n"
            + "//        System.out.printf(\"ac=%s%n\", ac.toString());\n"
            + "        Object a = mc.getAnnotation(ac);\n"
            + "//        System.out.printf(\"a=%s%n\", a);\n"
            + "        return ((MyAnno) a).value();\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_2__Marker_Annotations() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation1;\n"
            + "\n"
            + "@RuntimeRetainedAnnotation1\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() {\n"
            + "        RuntimeRetainedAnnotation1 anno = (\n"
            + "            (RuntimeRetainedAnnotation1) Main.class.getAnnotation(RuntimeRetainedAnnotation1.class)\n"
            + "        );\n"
            + "        return anno != null;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_3__Single_Element_Annotations() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation2;\n"
            + "\n"
            + "@RuntimeRetainedAnnotation2(\"Foo\")\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() {\n"
            + "        RuntimeRetainedAnnotation2 anno = (\n"
            + "            (RuntimeRetainedAnnotation2) Main.class.getAnnotation(RuntimeRetainedAnnotation2.class)\n"
            + "        );\n"
            + "        if (anno == null) throw new AssertionError(1);\n"
            + "        if (!anno.value().equals(\"Foo\")) throw new AssertionError(2);\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_1__Normal_Annotations1() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation2;\n"
            + "\n"
            + "@RuntimeRetainedAnnotation2(value = \"Bar\")\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() {\n"
            + "        RuntimeRetainedAnnotation2 anno = (\n"
            + "            (RuntimeRetainedAnnotation2) Main.class.getAnnotation(RuntimeRetainedAnnotation2.class)\n"
            + "        );\n"
            + "        if (anno == null) throw new AssertionError(1);\n"
            + "        if (!anno.value().equals(\"Bar\")) throw new AssertionError(2);\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_1__Normal_Annotations2() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import java.util.Arrays;\n"
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation3;\n"
            + "\n"
            + "@RuntimeRetainedAnnotation3(\n"
            + "    booleanValue     = true,\n"
            + "    byteValue        = (byte) 127,\n"
            + "    shortValue       = (short) 32767,\n"
            + "    intValue         = 99999,\n"
            + "    longValue        = 9999999999L,\n"
            + "    floatValue       = 123.5F,\n"
            + "    doubleValue      = 3.1415927,\n"
            + "    charValue        = 'X',\n"
            + "    stringValue      = \"Foo\",\n"
            + "    classValue       = String.class,\n"
            + "    annotationValue  = @Override,\n"
            + "    stringArrayValue = { \"Foo\", \"Bar\" },\n"
            + "    intArrayValue    = { 1, 2, 3 }\n"
            + ")\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() {\n"
            + "        RuntimeRetainedAnnotation3 anno = (\n"
            + "            (RuntimeRetainedAnnotation3) Main.class.getAnnotation(RuntimeRetainedAnnotation3.class)\n"
            + "        );\n"
            + "        if (anno == null) throw new AssertionError(1);\n"
            + "\n"
            // SUPPRESS CHECKSTYLE LineLength:13
            + "        if (!anno.booleanValue())                                                       throw new AssertionError(2);\n"
            + "        if (anno.byteValue() != 127)                                                    throw new AssertionError(3);\n"
            + "        if (anno.shortValue() != 32767)                                                 throw new AssertionError(4);\n"
            + "        if (anno.intValue() != 99999)                                                   throw new AssertionError(5);\n"
            + "        if (anno.longValue() != 9999999999L)                                            throw new AssertionError(6);\n"
            + "        if (anno.floatValue() != 123.5F)                                                throw new AssertionError(7);\n"
            + "        if (anno.doubleValue() != 3.1415927)                                            throw new AssertionError(8);\n"
            + "        if (anno.charValue() != 'X')                                                    throw new AssertionError(9);\n"
            + "        if (!anno.stringValue().equals(\"Foo\"))                                        throw new AssertionError(10);\n"
            + "        if (anno.classValue() != String.class)                                          throw new AssertionError(11);\n"
            + "        if (!(anno.annotationValue() instanceof Override))                              throw new AssertionError(12);\n"
            + "        if (!Arrays.equals(anno.stringArrayValue(), new String[] { \"Foo\", \"Bar\" })) throw new AssertionError(13);\n"
            + "        if (!Arrays.equals(anno.intArrayValue(), new int[] { 1, 2, 3 }))                throw new AssertionError(14);\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_4__Where_Annotations_May_Appear_field() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation2;\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    @RuntimeRetainedAnnotation2(\"Foo\") public int field;\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() throws Exception {\n"
            + "        RuntimeRetainedAnnotation2 anno = ((RuntimeRetainedAnnotation2) Main.class.getField(\n"
            + "            \"field\"\n"
            + "        ).getAnnotation(RuntimeRetainedAnnotation2.class));\n"
            + "        if (anno == null) throw new AssertionError(1);\n"
            + "        if (!anno.value().equals(\"Foo\")) throw new AssertionError(2);\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_9_7_4__Where_Annotations_May_Appear_method() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "import org.codehaus.commons.compiler.tests.annotation.RuntimeRetainedAnnotation2;\n"
            + "\n"
            + "public\n"
            + "class Main {\n"
            + "\n"
            + "    @RuntimeRetainedAnnotation2(\"Foo\") public void method() {}\n"
            + "\n"
            + "    public static boolean\n"
            + "    main() throws Exception {\n"
            + "        RuntimeRetainedAnnotation2 anno = ((RuntimeRetainedAnnotation2) Main.class.getMethod(\n"
            + "            \"method\"\n"
            + "        ).getAnnotation(RuntimeRetainedAnnotation2.class));\n"
            + "        if (anno == null) throw new AssertionError(1);\n"
            + "        if (!anno.value().equals(\"Foo\")) throw new AssertionError(2);\n"
            + "        return true;\n"
            + "    }\n"
            + "}"
        ), "Main");
    }

    @Test public void
    test_14_3__Local_class_declarations() throws Exception {
        this.assertScriptReturnsTrue(
            "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;"
        );
    }

    @Test public void
    test_14_4__Local_Variable_Declaration_Statements() throws Exception {
        this.assertScriptReturnsTrue(
            "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;"
        );

        String script = "var f = java.util.function.Function.<String>identity();\n";
        if (this.isJanino)                                            this.assertScriptUncookable(script, "NYI");
        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION >= 10) this.assertScriptExecutable(script);
    }

    @Test public void
    test_14_8__Expression_statements() throws Exception {
        this.assertScriptReturnsTrue("int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        this.assertScriptExecutable("System.currentTimeMillis();");
        this.assertScriptExecutable("new Object();");
        this.assertScriptUncookable("new Object[3];");
        this.assertScriptUncookable("int a; a;");
    }

    @Test public void
    test_14_10__The_assert_statement() throws Exception {

        // SUPPRESS CHECKSTYLE LineLength:5
        this.assertScriptExecutable("assert true;");
        this.assertScriptReturnsTrue("try { assert false;                  } catch (AssertionError ae) { return true;                          } return false;");
        this.assertScriptReturnsTrue("try { assert false : \"x\";          } catch (AssertionError ae) { return \"x\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : 3;              } catch (AssertionError ae) { return \"3\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : new Integer(8); } catch (AssertionError ae) { return \"8\".equals(ae.getMessage()); } return false;");
    }

    @Test public void
    test_14_11__The_switch_statement() throws Exception {
        this.assertScriptReturnsTrue("int x = 37; switch (x) {} return x == 37;");
        this.assertScriptReturnsTrue("int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        this.assertScriptReturnsTrue(
            "int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;"
        );
        this.assertScriptReturnsTrue(
            "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;"
        );
        this.assertScriptReturnsTrue(
            "int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;"
        );
        this.assertScriptReturnsTrue(
            "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_enum() throws Exception {
        this.assertScriptReturnsTrue(
            ""
            + "import java.lang.annotation.ElementType;\n"
            + "\n"
            + "ElementType x = ElementType.FIELD;\n"
            + "switch (x) {\n"
            + "case ANNOTATION_TYPE:\n"
            + "    return false;\n"
            + "case FIELD:\n"
            + "    return true;\n"
            + "default:\n"
            + "    break;\n"
            + "}\n"
            + "return false;"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String1() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "String s = \"a\";\n"
            + "\n"
            + "switch (s) {\n"
            + "case \"a\": case \"b\": case \"c\":\n"
            + "    return true;\n"
            + "case \"d\": case \"e\": case \"f\":\n"
            + "    return false;\n"
            + "default:\n"
            + "    return false;"
            + "}\n"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String2() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "String s = \"f\";\n"
            + "\n"
            + "switch (s) {\n"
            + "case \"a\": case \"b\": case \"c\":\n"
            + "    return false;\n"
            + "case \"d\": case \"e\": case \"f\":\n"
            + "    return true;\n"
            + "default:\n"
            + "    return false;"
            + "}\n"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String3() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "String s = \"g\";\n"
            + "\n"
            + "switch (s) {\n"
            + "case \"a\": case \"b\": case \"c\":\n"
            + "    return false;\n"
            + "case \"d\": case \"e\": case \"f\":\n"
            + "    return false;\n"
            + "default:\n"
            + "    return true;"
            + "}\n"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String4() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "String s = \"g\";\n"
            + "\n"
            + "switch (s) {\n"
            + "case \"a\": case \"b\": case \"c\":\n"
            + "    return false;\n"
            + "case \"d\": case \"e\": case \"f\":\n"
            + "    return false;\n"
            + "}\n"
            + "return true;"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String5() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        String s1 = "AaAaAa", s2 = "AaAaBB";
        Assert.assertEquals(s1.hashCode(), s2.hashCode());

        this.assertScriptReturnsTrue(
            ""
            + "switch (\"" + s1 + "\") {\n"
            + "case \"" + s1 + "\":\n"
            + "    return true;\n"
            + "}\n"
            + "return false;"
        );

        this.assertScriptReturnsTrue(
            ""
            + "switch (\"" + s1 + "\") {\n"
            + "case \"" + s1 + "\":\n"
            + "case \"" + s2 + "\":\n"
            + "    return true;\n"
            + "}\n"
            + "return false;"
        );

        this.assertScriptReturnsTrue(
            ""
            + "switch (\"" + s1 + "\") {\n"
            + "case \"" + s1 + "\":\n"
            + "    return true;\n"
            + "case \"" + s2 + "\":\n"
            + "    return false;\n"
            + "}\n"
            + "return false;"
        );

        this.assertScriptReturnsTrue(
            ""
            + "switch (\"" + s1 + "\") {\n"
            + "case \"" + s2 + "\":\n"
            + "    return false;\n"
            + "}\n"
            + "return true;"
        );
    }

    @Test public void
    test_14_11__The_switch_statement_String_DuplicateCaseValue() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptUncookable(
            ""
            + "String s = \"c\";\n"
            + "\n"
            + "switch (s) {\n"
            + "case \"a\": case \"b\": case \"c\":\n"
            + "    return false;\n"
            + "case \"c\": case \"d\": case \"e\":\n"
            + "    return false;\n"
            + "default:\n"
            + "    return false;"
            + "}\n"
        );
    }

    @Test public void
    test_14_14_2_1__The_enhanced_for_statement_Iterable() throws Exception {
        this.assertScriptReturnsTrue(
            "String x = \"A\";\n"
            + "for (Object y : java.util.Arrays.asList(new String[] { \"B\", \"C\" })) x += y;\n"
            + "return x.equals(\"ABC\");"
        );
        this.assertScriptReturnsTrue(
            "String x = \"A\";\n"
            + "for (String y : java.util.Arrays.asList(new String[] { \"B\", \"C\" })) x += y.length();\n"
            + "return x.equals(\"A11\");"
        );

        String script = (
            ""
            + "String x = \"A\";\n"
            + "for (var y : java.util.Arrays.asList(new String[] { \"B\", \"C\" })) x += y.length();\n"
            + "return x.equals(\"A11\");"
        );
        if (this.isJanino)                   this.assertScriptUncookable(script, "NYI");
        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION >= 10) this.assertScriptReturnsTrue(script);
    }

    @Test public void
    test_14_14_2_2__The_enhanced_for_statement_Array() throws Exception {

        // Primitive array.
        this.assertScriptReturnsTrue(
            "int x = 1; for (int y : new int[] { 1, 2, 3 }) x += x * y; return x == 24;"
        );
        this.assertScriptReturnsTrue(
            "int x = 1; for (int y : new short[] { 1, 2, 3 }) x += x * y; return x == 24;"
        );
        this.assertScriptUncookable(
            "int x = 1; for (short y : new int[] { 1, 2, 3 }) x += x * y;",
            "conversion not possible|possible loss of precision|possible lossy conversion"
        );

        // Object array.
        this.assertScriptReturnsTrue(
            "String x = \"A\"; for (String y : new String[] { \"B\", \"C\" }) x += y; return x.equals(\"ABC\");"
        );
        this.assertScriptReturnsTrue(
            "String x = \"A\"; for (Object y : new String[] { \"B\", \"C\" }) x += y; return x.equals(\"ABC\");"
        );
        this.assertScriptUncookable(
            "String x = \"A\"; for (Number y : new String[] { \"B\", \"C\" }) x += y; return x.equals(\"ABC\");",
            "conversion not possible|incompatible types"
        );
        this.assertScriptReturnsTrue(
            "String x = \"A\"; String[] sa = { \"B\",\"C\" }; for (String y : sa) x += y; return x.equals(\"ABC\");"
        );
        this.assertScriptReturnsTrue(
            ""
            + "final StringBuilder sb = new StringBuilder();\n"
            + "for (final String y : new String[] { \"A\", \"B\", \"C\" }) {\n"
            + "    new Runnable() {\n"
            + "        public void run() { sb.append(y); }\n"
            + "    }.run();\n"
            + "}\n"
            + "return sb.toString().equals(\"ABC\");"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__1() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "static void meth() throws Throwable {\n"
            + "}\n"
            + "\n"
            + "public static boolean main() { \n"
            + "    try {\n"
            + "        meth();\n"
            + "    } catch (java.io.FileNotFoundException fnfe) {\n"
            + "        return false;\n"
            + "    } catch (java.io.IOException ioe) {\n"
            + "        return false;\n"
            + "    } catch (Exception e) {\n"
            + "        return false;\n"
            + "    } catch (Throwable t) {\n"
            + "        return false;\n"
            + "    }\n"
            + "    return true;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__2() throws Exception {
        this.assertClassBodyUncookable(
            ""
            + "static void meth() throws Throwable {\n"
            + "}\n"
            + "\n"
            + "public static boolean main() { \n"
            + "    try {\n"
            + "        meth();\n"
            + "    } catch (java.io.FileNotFoundException fnfe) {\n"
            + "        return false;\n"
            + "    } catch (Exception e) {\n"
            + "        return false;\n"
            + "    } catch (java.io.IOException ioe) {\n"  // <= Hidden by preceding "catch (Exception)"
            + "        return false;\n"
            + "    } catch (Throwable t) {\n"
            + "        return false;\n"
            + "    }\n"
            + "    return true;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__3() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "static void meth() throws java.io.IOException {\n"
            + "}\n"
            + "\n"
            + "public static boolean main() { \n"
            + "    try {\n"
            + "        meth();\n"
            + "    } catch (java.io.FileNotFoundException fnfe) {\n"
            + "        return false;\n"
            + "    } catch (java.io.IOException ioe) {\n"
            + "        return false;\n"
            + "    } catch (Exception e) {\n"
            + "        return false;\n"
            + "    } catch (Throwable t) {\n"
            + "        return false;\n"
            + "    }\n"
            + "    return true;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__4() throws Exception {

        // JAVAC does not detect this condition although, I believe, it should, according to the
        // JLS.
        Assume.assumeFalse(this.isJdk);

        this.assertClassBodyUncookable(
            ""
            + "static void meth() throws java.io.FileNotFoundException {\n"
            + "}\n"
            + "\n"
            + "public static boolean main() { \n"
            + "    try {\n"
            + "        meth();\n"
            + "    } catch (java.io.FileNotFoundException fnfe) {\n"
            + "        return false;\n"
            + "    } catch (java.io.IOException ioe) {\n" // <= Not thrown by 'meth()', but JDKs 6...8 don't detect that
            + "        return false;\n"
            + "    } catch (Exception e) {\n"
            + "        return false;\n"
            + "    } catch (Throwable t) {\n"
            + "        return false;\n"
            + "    }\n"
            + "    return true;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__5() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean main() { \n"
            + "    try {\n"
            + "        if (true) throw new java.io.IOException();\n"
            + "    } catch (java.io.FileNotFoundException fnfe) {\n" // <= Not thrown by TRY block, but neither JDK 6
            + "        return false;\n"                              //    nor JANINO detect that
            + "    } catch (java.io.IOException ioe) {\n"
            + "        return true;\n"
            + "    } catch (Exception e) {\n"
            + "        return false;\n"
            + "    } catch (Throwable t) {\n"
            + "        return false;\n"
            + "    }\n"
            + "    return false;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_1__Execution_of_try_catch__6() throws Exception {
        this.assertCompilationUnitCookable(
            ""
            + "public class TestIt {\n"
            + "    public static class MyException extends Exception implements Runnable {\n"
            + "        public MyException(String st) {\n"
            + "            super(st);\n"
            + "        }\n"
            + "\n"
            + "        public void run() {\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public void foo() throws MyException {\n"
            + "        if (true) {\n"
            + "            try {\n"
            + "                if (false != false) {\n"
            + "                    throw new MyException(\"my exc\");\n"
            + "                }\n"
            + "                System.out.println(\"abc\");\n"
            + "                System.out.println(\"xyz\");\n"
            + "\n"
            + "            } catch (MyException e) {\n"
            + "                throw new java.lang.RuntimeException(e);\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "\n"
            + "    public static boolean main() {\n"
            + "        try {\n"
            + "            new TestIt().foo();\n"
            + "        } catch (MyException e) {\n"
            + "            System.out.println(\"caught\");\n"
            + "        }\n"
            + "        return true;\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_2__Execution_of_try_finally_and_try_catch_finally__1() throws Exception {

        this.assertScriptReturnsNull(
            ""
            + "int x = 7;\n"
            + "//System.out.println(\"A\" + x);\n"
            + "try {\n"
            + "    //System.out.println(\"B\" + x);\n"
            + "    if (x != 7) return x;\n"
            + "    //System.out.println(\"C\" + x);\n"
            + "    x++;\n"
            + "    //System.out.println(\"D\" + x);\n"
            + "} finally {\n"
            + "    //System.out.println(\"E\" + x);\n"
            + "    if (x != 8) return x;\n"
            + "    //System.out.println(\"F\" + x);\n"
            + "    x++;\n"
            + "    //System.out.println(\"G\" + x);\n"
            + "}\n"
            + "//System.out.println(\"H\" + x);\n"
            + "if (x != 9) return x;\n"
            + "return null;\n"
        );
    }

    @Test public void
    test_14_20_2__Execution_of_try_finally_and_try_catch_finally__2() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptExecutable(
            ""
            + "try {\n"
            + "    int a = 7;\n"
            + "} finally {\n"
            + "    ;\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_3__try_with_resources__1() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsNull(
            ""
            + "final int[] closed = new int[1];\n"
            + "class MyCloseable implements java.io.Closeable {\n"
            + "    public void close() { closed[0]++; }\n"
            + "}\n"
            + "\n"
            + "try (MyCloseable mc = new MyCloseable()) {\n"
            + "    if (closed[0] != 0) return closed[0];\n"
            + "    System.currentTimeMillis();\n"
            + "}\n"
            + "if (closed[0] != 1) return closed[0];\n"
            + "return null;\n"
        );
    }

    @Test public void
    test_14_20_3__try_with_resources__2() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "final int[] closed = new int[1];\n"
            + "class MyCloseable implements java.io.Closeable {\n"
            + "    public void close() { closed[0]++; }\n"
            + "}\n"
            + "\n"
            + "try (\n"
            + "    MyCloseable mc1 = new MyCloseable();\n"
            + "    MyCloseable mc2 = new MyCloseable();\n"
            + "    MyCloseable mc3 = new MyCloseable()\n"
            + ") {\n"
            + "    if (closed[0] != 0) return false;\n"
            + "    System.currentTimeMillis();\n"
            + "}\n"
            + "if (closed[0] != 3) return false;\n"
            + "return true;\n"
        );
    }

    @Test public void
    test_14_20_3__try_with_resources__2a() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptExecutable(
            ""
            + "class MyCloseable implements java.io.Closeable {\n"
            + "    public void close() {}\n"
            + "}\n"
            + "\n"
            + "try (\n"
            + "    MyCloseable mc1 = new MyCloseable();\n"
            + "    MyCloseable mc2 = new MyCloseable()\n"
            + ") {\n"
            + "    System.currentTimeMillis();\n"
            + "}\n"
        );
    }

    @Test public void
    test_14_20_3__try_with_resources__3() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 7) return;

        this.assertScriptReturnsTrue(
            ""
            + "final int[] closed = new int[1];\n"
            + "class MyCloseable implements java.io.Closeable {\n"
            + "    public void close() { closed[0]++; }\n"
            + "}\n"
            + "\n"
            + "try (\n"
            + "    MyCloseable mc1 = new MyCloseable();\n"
            + "    MyCloseable mc2 = null;\n"
            + "    MyCloseable mc3 = new MyCloseable()\n"
            + ") {\n"
            + "    if (closed[0] != 0) return false;\n"
            + "    System.currentTimeMillis();\n"
            + "}\n"
            + "if (closed[0] != 2) return false;\n"
            + "return true;\n"
        );
    }

    /**
     * Tests the "enhanced try-with-resources statement" that was introduced with Java 9 with a "local variable
     * declarator resource" with a local variable access.
     */
    @Test public void
    test_14_20_3__try_with_resources__10a() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 9) return;

        this.assertScriptExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "try {\n"
            + "    final int[] x = new int[1];\n"
            + "    Closeable lv = new Closeable() {\n"
            + "        public void close() { if (++x[0] != 2) throw new AssertionError(); }\n"
            + "    };\n"
            + "    \n"
            + "    try (lv) {\n"
            + "        if (++x[0] != 1) throw new AssertionError();\n"
            + "    }\n"
            + "    if (++x[0] != 3) throw new AssertionError();\n"
            + "} catch (IOException ioe) {\n"
            + "    throw new AssertionError(ioe);\n"
            + "}\n"
        );
    }

    /**
     * Tests the "enhanced try-with-resources statement" that was introduced with Java 9 with a "variable access
     * resource" with a static field access.
     */
    @Test public void
    test_14_20_3__try_with_resources__10b() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 9) return;

        this.assertClassBodyExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "\n"
            + "public static final Closeable sf = new Closeable() {\n"
            + "    public void close() { if (++x[0] != 2) throw new AssertionError(); }\n"
            + "};\n"
            + "public static final int[] x = new int[1];\n"
            + "\n"
            + "public static void main() {\n"
            + "    try {\n"
            + "        \n"
            + "        try (SC.sf) {\n"
            + "            if (++x[0] != 1) throw new AssertionError();\n"
            + "        }\n"
            + "        if (++x[0] != 3) throw new AssertionError();\n"
            + "    } catch (IOException ioe) {\n"
            + "        throw new AssertionError(ioe.toString());\n"
            + "    }\n"
            + "}\n"
        );
    }

    /**
     * Tests the "enhanced try-with-resources statement" that was introduced with Java 9 with a "variable access
     * resource" with a non-static field access.
     */
    @Test public void
    test_14_20_3__try_with_resources__10c() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 9) return;

        this.assertClassBodyExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "\n"
            + "public final Closeable sf = new Closeable() {\n"
            + "    public void close() { if (++SC.this.x[0] != 2) throw new AssertionError(); }\n"
            + "};\n"
            + "public final int[] x = new int[1];\n"
            + "\n"
            + "public void main() {\n"
            + "    try {\n"
            + "        \n"
            + "        try (this.sf) {\n"
            + "            if (++this.x[0] != 1) throw new AssertionError();\n"
            + "        }\n"
            + "        if (++this.x[0] != 3) throw new AssertionError();\n"
            + "    } catch (IOException ioe) {\n"
            + "        throw new AssertionError(ioe.toString());\n"
            + "    }\n"
            + "}\n"
        );
    }

    /**
     * Tests the "enhanced try-with-resources statement" that was introduced with Java 9 with a "local variable
     * declarator resource" with an invalid variable access.
     */
    @Test public void
    test_14_20_3__try_with_resources__10d() throws Exception {

        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION < 9) return;

        this.assertScriptUncookable(
            (
                ""
                + "import java.io.Closeable;\n"
                + "import java.io.IOException;\n"
                + "import org.junit.Assert;\n"
                + "try {\n"
                + "    try (new Closeable() { public void close() {} }) {\n"
                + "    }\n"
                + "} catch (Exception ioe) {\n"
                + "    Assert.fail(ioe.toString());\n"
                + "}\n"
            ),
            (
                "compiler.err.try.with.resources.expr.needs.var"
                + "|"
                + "NewAnonymousClassInstance rvalue not allowed as a resource"
            )
        );
    }

    @Test public void
    test_14_21__Unreachable_statements() throws Exception {
        this.assertClassBodyUncookable(
            ""
            + "public void test() throws Exception {}\n"
            + "public void test2() {\n"
            + "    try {\n"
            + "        test();\n"
            + "    } catch (Exception e) {\n"
            + "        ;\n"
            + "    } catch (java.io.IOException e) {\n"
            + "        // This CATCH clause is unreachable.\n"
            + "    }\n"
            + "}\n"
        );

        this.assertClassBodyCookable(
            ""
            + "public void test2() {\n"
            + "    try {\n"
            + "        throw new java.io.IOException();\n"
            + "    } catch (java.io.IOException e) {\n"
            + "        ;\n"
            + "    } catch (NullPointerException e) {\n"
            + "        ;\n"
            + "    } catch (RuntimeException e) {\n"
            + "        ;\n"
            + "    } catch (Exception e) {\n"
            + "        ;\n"
            + "    } catch (NoClassDefFoundError e) {\n"
            + "        ;\n"
            + "    } catch (Throwable e) {\n"
            + "        ;\n"
            + "    }\n"
            + "}\n"
        );
    }

    @Test public void
    test_15_2_2_5__Choosing_the_most_specific_vararg_method_1() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean main() {\n"
            + "    return meth(new int[][] { { 3 } }) == 1;\n"
            + "}\n"
            + "\n"
            + "static int meth(int[][]...a) {\n"
            + "    return 0;\n"
            + "}\n"
            + "\n"
            + "static int meth(int[]... b) {\n"
            + "    return 1;\n"
            + "}\n"
        );
    }

    @Test public void
    test_15_9_1__Determining_the_class_being_Instantiated() throws Exception {

        this.assertExpressionEvaluatesTrue("new Object() instanceof Object");
        this.assertExpressionUncookable("new java.util.List()");
        this.assertExpressionUncookable("new other_package.PackageClass()");
        this.assertExpressionUncookable("new java.util.AbstractList()");
        this.assertExpressionEvaluatesTrue(
            "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass"
        );
        this.assertExpressionUncookable("new other_package.Foo(3).new Foo.PublicMemberClass()");
        this.assertExpressionUncookable("new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        this.assertExpressionUncookable("new other_package.Foo(3).new PackageMemberClass()");
        this.assertExpressionUncookable("new other_package.Foo(3).new PublicAbstractMemberClass()");
        this.assertExpressionUncookable("new other_package.Foo(3).new PublicStaticMemberClass()");
        this.assertExpressionUncookable("new other_package.Foo(3).new PublicMemberInterface()");
        this.assertExpressionUncookable("new java.util.ArrayList().new PublicMemberClass()");

        // The following one is tricky: A Java 6 JRE declares
        //    public int          File.compareTo(File)
        //    public abstract int Comparable.compareTo(Object)
        // , and yet "File" is not abstract!
        this.assertCompilationUnitMainReturnsTrue((
            "class MyFile extends java.io.File {\n"
            + "    public MyFile() { super(\"/my/file\"); }\n"
            + "}\n"
            + "public class Main {\n"
            + "    public static boolean main() {\n"
            + "        return 0 == new MyFile().compareTo(new MyFile());\n"
            + "    }\n"
            + "}"
        ), "Main");

        // "Type interference for generic instance creation" (a.k.a. the "diamond operator"); a Java 7 feature.
        if (this.isJdk && CommonsCompilerTestSuite.JVM_VERSION >= 7) {
            this.assertScriptReturnsTrue(
                "java.util.Map<String, Integer> map = new java.util.HashMap<>(); return !map.containsKey(\"\");"
            );
        }
    }

    @Test public void
    test_15_9_3a__Choosing_the_Constructor_and_its_Arguments() throws Exception {

        this.assertExpressionEvaluatable("new Integer(3)");
        this.assertExpressionEvaluatable("new Integer(new Integer(3))");
        this.assertExpressionEvaluatable("new Integer(new Byte((byte) 3))");
        this.assertExpressionUncookable("new Integer(new Object())");
    }

    @Test public void
    test_15_9_3b__Choosing_the_Constructor_and_its_Arguments() throws Exception {

        // "Diamond operator".
        this.assertScriptExecutable(
            ""
            + "import java.util.*;\n"
            + "List<String> l = new ArrayList<>();\n"
        );
    }

    @Test public void
    test_15_9_5a__Anonymous_Class_Declarations() throws Exception {

        this.assertCompilationUnitMainExecutable((
            ""
            + "public class Foo {\n"
            + "    public static void main() { new Foo().meth(); }\n"
            + "    private Object meth() {\n"
            + "        return new Object() {};\n"
            + "    }\n"
            + "}\n"
        ), "Foo");
    }

    @Test public void
    test_15_9_5b__Anonymous_Class_Declarations() throws Exception {

        this.assertCompilationUnitMainExecutable((
            ""
            + "public class A {\n"
            + "    public static void main() { new A(); }\n"
            + "    public A(Object o) {}\n"
            + "    public A() {\n"
            + "        this(new Object() {});\n"
            + "    }\n"
            + "}\n"
        ), "A");
    }

    /**
     * Notice: In JLS2 and JLS7, this section had number "15.3" (which, since JLS8, is the number for section "Method
     * Reference Expressions"). Since JLS8 it has number "15.10.3".
     */
    @Test public void
    test_15_10_3__Array_Access_Expressions() throws Exception {
        this.assertExpressionCookable("(new int[3])[(byte) 0]");
        this.assertExpressionCookable("(new int[3])[(char) 0]");
        this.assertExpressionCookable("(new int[3])[(short) 0]");
        this.assertExpressionCookable("(new int[3])[0]");
        this.assertExpressionUncookable("(new int[3])[0L]");
    }

    @Test public void
    test_15_11_2__Accessing_Superclass_Members_using_super() throws Exception {

        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class T1            { int x = 1; }\n"
            + "public class T2 extends T1 { int x = 2; }\n"
            + "public class T3 extends T2 {\n"
            + "    int x = 3;\n"
            + "    public static boolean main() {\n"
            + "        return new T3().test2();\n"
            + "    }\n"
            + "    public boolean test2() {\n"
            + "        return (\n"
            + "            x == 3\n"
            + "            && super.x == 2\n"
            + "            && T3.super.x == 2\n"
            + "//            && T2.super.x == 1\n" // <= Does not work with the SUN JDK 1.6 compiler
            + "            && ((T2) this).x == 2\n"
            + "            && ((T1) this).x == 1\n"
            + "        );\n"
            + "    }\n"
            + "}\n"
        ), "T3");
    }

    @Test public void
    test_15_12_2_4__Phase3Identify_applicable_variable_arity_methods__1() throws Exception {
        this.assertExpressionEvaluatesTrue("\"two one\".equals(String.format(\"%2$s %1$s\", \"one\", \"two\"))");
    }

    @Test public void
    test_15_12_2_4__Phase3Identify_applicable_variable_arity_methods__2() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean\n"
            + "main() {\n"
            + "    return (\n"
            + "        meth(6,  1, 2, 3)\n"
            + "        && meth(10, 1, 2, 3, 4)\n"
            + "        && meth(15, 1, 2, 3, 4, 5)\n"
            + "        && meth(21, 1, 2, 3, 4, 5, 6)\n"
            + "    );\n"
            + "}\n"
            + "\n"
            + "static boolean\n"
            + "meth(int expected, int... operands) {\n"
            + "    int sum = 0;\n"
            + "    for (int i = 0; i < operands.length; i++) sum += operands[i];\n"
            + "    return sum == expected;\n"
            + "}\n"
        );
    }

    @Test public void
    test_15_12_2_4__Phase3Identify_applicable_variable_arity_methods__3() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean main() {\n"
            + "    if (meth(1, 2, 3) != 5) return false;\n"
            + "    if (meth(1) != 0) return false;\n"
            + "    if (meth(1, null) != 99) return false;\n"
            + "    return true;\n"
            + "}\n"
            + "\n"
            + "static double meth(int x, double... va) {\n"
            + "    if (va == null) return 99;\n"
            + "\n"
            + "    double sum = 0;\n"
            + "    for (int i = 0; i < va.length; i++) sum += va[i];\n"
            + "    return sum;\n"
            + "}\n"
        );
    }

    @Test public void
    test_15_12_2_4__Phase3Identify_applicable_variable_arity_methods__4() throws Exception {
        this.assertScriptReturnsTrue(
            ""
            + "class LocalClass {\n"
            + "    int x;\n"
            + "\n"
            + "    LocalClass(String s, Object... oa) {\n"
            + "        x = oa.length;\n"
            + "    }\n"
            + "}\n"
            + "\n"
            + "if (new LocalClass(\"\").x != 0) return false;\n"
            + "if (new LocalClass(\"\", 1, 2).x != 2) return false;\n"
            + "return true;\n"
        );
    }

    @Test public void
    test_15_12_2_5__Choose_the_Most_Specific_Method() throws Exception {

        this.assertCompilationUnitUncookable(
            ""
            + "public class Main { public static boolean test() { return new A().meth(\"x\", \"y\"); } }\n"
            + "public class A {\n"
            + "    public boolean meth(String s, Object o) { return true; }\n"
            + "    public boolean meth(Object o, String s) { return false; }\n"
            + "}\n"
        );

        // The following case is tricky: JLS7 says that the invocation is AMBIGUOUS, but only JAVAC 1.2 issues an
        // error; JAVAC 1.4.1, 1.5.0 and 1.6.0 obviously ignore the declaring type and invoke "A.meth(String)".
        // JLS7 is not clear about this. For compatibility with JAVA 1.4.1, 1.5.0 and 1.6.0, JANINO also ignores the
        // declaring type.
        //
        // See also JANINO-79 and "IClass.IInvocable.isMoreSpecificThan()".
        this.assertCompilationUnitMainReturnsTrue((
            ""
            + "public class Main        { public static boolean main()  { return new B().meth(\"x\"); } }\n"
            + "public class A           { public boolean meth(String s) { return true; } }\n"
            + "public class B extends A { public boolean meth(Object o) { return false; } }\n"
        ), "Main");
    }

    @Test public void
    test_15_12_2_6__Identify_applicable_variable_arity_methods__4() throws Exception {
        this.assertClassBodyMainReturnsTrue(
            ""
            + "public static boolean main() {\n"
            + "    return meth((byte)1, (byte)2) == 1;\n"
            + "}\n"
            + "\n"
            + "static int meth(byte...a) {\n"
            + "    return 0;\n"
            + "}\n"
            + "\n"
            + "static int meth(int a, int b){\n"
            + "    return 1;\n"
            + "}\n"
            + "\n"
            + "static int meth(int a, double b){\n"
            + "     return 2;\n"
            + "}\n"
        );
    }

    @Test public void
    test_15_12_2_7__Identify_applicable_variable_arity_methods__4() throws Exception {

        // The resolution phases should go like this:
        //  - all three methods are *applicable*, but fixed-arity ones have higher priority
        //    (meaning the chosen one, if any, must be a fixed-arity)
        //  - now we are left with (int, int) and (byte, double)
        //  - neither of these is more specific than the other,
        //    therefore it is an ambiguous case
        //  Ref: http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.1
        //
        // (Note: Some versions of javac choose the variable-arity method ("return 0"). Their reasoning seems to be
        // that there is ambiguity amongst fixed-arity applicables, so picking a vararg is acceptable if that means
        // there is no ambiguity. I have not been able to find any piece of documentation about this in the docs.)

        // JDK 1.7.0_17 and _21 do _not_ issue an error, although they should!?
        Assume.assumeFalse(this.isJdk && CommonsCompilerTestSuite.JVM_VERSION == 7);

        this.assertClassBodyUncookable((
            ""
            + "public static Object main() {\n"
            + "    return meth((byte) 1, (byte) 2);\n"
            + "}\n"
            + "\n"
            + "static int meth(byte...a) {\n"
            + "    return 0;\n"
            + "}\n"
            + "\n"
            + "static int meth(int a, int b){\n"
            + "    return 1;\n"
            + "}\n"
            + "\n"
            + "static int meth(byte a, double b){\n"
            + "    return 2;\n"
            + "}\n"
        ), "Invocation of.*method.*is ambiguous|compiler\\.err\\.ref\\.ambiguous");
    }

    @Test public void
    test_15_13__Method_reference_expressions() throws Exception {

        if (CommonsCompilerTestSuite.JVM_VERSION < 9) return;

        // ExpressionName '::' [ TypeArguments ] Identifier  (ExpressionName = a{.b})
        this.assertScriptExecutable(
            "Runnable r = new Runnable() { @Override public void run() { } }; Runnable s = r::run;"
        );

        // Primary '::' [ TypeArguments ] Identifier
        this.assertScriptExecutable(
            "Runnable r = new Runnable() { @Override public void run() { } }; Runnable s = (r)::run;"
        );

        // ReferenceType '::' [ TypeArguments ] Identifier
        this.assertScriptExecutable(
            ""
            + "Runnable r = new Runnable() { @Override public void run() { } };\n"
            + "Runnable t = java.util.Collections::emptySet;\n"
        );

        // 'super' '::' [ TypeArguments ] Identifier
        // TODO

        // TypeName '.' 'super' '::' [ TypeArguments ] Identifier
        // TODO

        // ClassType '::' [ TypeArguments ] 'new'
        this.assertScriptExecutable("Runnable r4 = java.util.HashMap::new;");

        // ArrayType '::' 'new'
        this.assertScriptExecutable("java.util.function.Consumer<Integer> c1 = int[]::new;");
    }

    @Test public void
    test_15_14_2__Postfix_Increment_Operator() throws Exception {

        this.assertScriptReturnsTrue("int i = 7; i++; return i == 8;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); i++; return i.intValue() == 8;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && i++ == 7 && i == 8;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;"
        );

        // byte

        this.assertScriptReturnsTrue("byte b = -1;  b++; return b == 0;");
        this.assertScriptReturnsTrue("byte b = 0;   b++; return b == 1;");
        this.assertScriptReturnsTrue("byte b = 127; b++; return b == -128;");

        this.assertScriptReturnsTrue("byte b = 0;    return b++ == 0;");
        this.assertScriptReturnsTrue("byte b = 127;  return b++ == 127;");
        this.assertScriptReturnsTrue("byte b = -128; return b++ == -128;");

        // short

        this.assertScriptReturnsTrue("short s = -1;    s++; return s == 0;");
        this.assertScriptReturnsTrue("short s = 0;     s++; return s == 1;");
        this.assertScriptReturnsTrue("short s = 127;   s++; return s == 128;");
        this.assertScriptReturnsTrue("short s = 32767; s++; return s == -32768;");

        this.assertScriptReturnsTrue("short s = 0;      return s++ == 0;");
        this.assertScriptReturnsTrue("short s = 32767;  return s++ == 32767;");
        this.assertScriptReturnsTrue("short s = -32768; return s++ == -32768;");

        // int

        this.assertScriptReturnsTrue("int i = -1;                i++; return i == 0;");
        this.assertScriptReturnsTrue("int i = 0;                 i++; return i == 1;");
        this.assertScriptReturnsTrue("int i = 127;               i++; return i == 128;");
        this.assertScriptReturnsTrue("int i = 32767;             i++; return i == 32768;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; i++; return i == Integer.MIN_VALUE;");

        this.assertScriptReturnsTrue("int i = 0;                 return i++ == 0;");
        this.assertScriptReturnsTrue("int i = 32767;             return i++ == 32767;");
        this.assertScriptReturnsTrue("int i = -32768;            return i++ == -32768;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; return i++ == Integer.MIN_VALUE;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; return i++ == Integer.MAX_VALUE;");

        // long

        this.assertScriptReturnsTrue("long i = -1;                i++; return i == 0;");
        this.assertScriptReturnsTrue("long i = 0;                 i++; return i == 1;");
        this.assertScriptReturnsTrue("long i = 127;               i++; return i == 128;");
        this.assertScriptReturnsTrue("long i = 32767;             i++; return i == 32768;");
        this.assertScriptReturnsTrue("long i = Integer.MAX_VALUE; i++; return i == Integer.MAX_VALUE + 1L;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;    i++; return i == Long.MIN_VALUE;");

        this.assertScriptReturnsTrue("long i = 0;                 return i++ == 0;");
        this.assertScriptReturnsTrue("long i = 32767;             return i++ == 32767;");
        this.assertScriptReturnsTrue("long i = -32768;            return i++ == -32768;");
        this.assertScriptReturnsTrue("long i = Integer.MIN_VALUE; return i++ == Integer.MIN_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE;    return i++ == Long.MIN_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;    return i++ == Long.MAX_VALUE;");

        // char

        this.assertScriptReturnsTrue("char c = 0;     c++; return c == 1;");
        this.assertScriptReturnsTrue("char c = 127;   c++; return c == 128;");
        this.assertScriptReturnsTrue("char c = 255;   c++; return c == 256;");
        this.assertScriptReturnsTrue("char c = 32767; c++; return c == 32768;");
        this.assertScriptReturnsTrue("char c = 65535; c++; return c == 0;");

        this.assertScriptReturnsTrue("char c = 0;     return c++ == 0;");
        this.assertScriptReturnsTrue("char c = 127;   return c++ == 127;");
        this.assertScriptReturnsTrue("char c = 128;   return c++ == 128;");
        this.assertScriptReturnsTrue("char c = 255;   return c++ == 255;");
        this.assertScriptReturnsTrue("char c = 256;   return c++ == 256;");
        this.assertScriptReturnsTrue("char c = 32767; return c++ == 32767;");
        this.assertScriptReturnsTrue("char c = 32768; return c++ == 32768;");
        this.assertScriptReturnsTrue("char c = 65535; return c++ == 65535;");

        // float

        this.assertScriptReturnsTrue("float f = 3.0F; f++; return f == 4.0F;");

        // double

        this.assertScriptReturnsTrue("double d = 17.9; d++; return d == 18.9;");

        // boolean

        this.assertScriptUncookable("boolean b = true; b++;");
    }

    @Test public void
    test_15_14_3__Postfix_Decrement_Operator() throws Exception {
        this.assertScriptReturnsTrue("int i = 7; i--; return i == 6;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); i--; return i.intValue() == 6;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && i-- == 7 && i == 6;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;"
        );

        // byte

        this.assertScriptReturnsTrue("byte b = 0;    b--; return b == -1;");
        this.assertScriptReturnsTrue("byte b = 1;    b--; return b == 0;");
        this.assertScriptReturnsTrue("byte b = -128; b--; return b == 127;");

        this.assertScriptReturnsTrue("byte b = 0;    return b-- == 0;");
        this.assertScriptReturnsTrue("byte b = 127;  return b-- == 127;");
        this.assertScriptReturnsTrue("byte b = -128; return b-- == -128;");

        // short

        this.assertScriptReturnsTrue("short s = 0;      s--; return s == -1;");
        this.assertScriptReturnsTrue("short s = 1;      s--; return s == 0;");
        this.assertScriptReturnsTrue("short s = 128;    s--; return s == 127;");
        this.assertScriptReturnsTrue("short s = -32768; s--; return s == 32767;");

        this.assertScriptReturnsTrue("short s = 0;      return s-- == 0;");
        this.assertScriptReturnsTrue("short s = 32767;  return s-- == 32767;");
        this.assertScriptReturnsTrue("short s = -32768; return s-- == -32768;");

        // int

        this.assertScriptReturnsTrue("int i = 0;                 i--; return i == -1;");
        this.assertScriptReturnsTrue("int i = 1;                 i--; return i == 0;");
        this.assertScriptReturnsTrue("int i = 128;               i--; return i == 127;");
        this.assertScriptReturnsTrue("int i = 32768;             i--; return i == 32767;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; i--; return i == Integer.MAX_VALUE;");

        this.assertScriptReturnsTrue("int i = 0;                 return i-- == 0;");
        this.assertScriptReturnsTrue("int i = 32767;             return i-- == 32767;");
        this.assertScriptReturnsTrue("int i = -32768;            return i-- == -32768;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; return i-- == Integer.MIN_VALUE;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; return i-- == Integer.MAX_VALUE;");

        // long

        this.assertScriptReturnsTrue("long i = 0;                      i--; return i == -1;");
        this.assertScriptReturnsTrue("long i = 1;                      i--; return i == 0;");
        this.assertScriptReturnsTrue("long i = 128;                    i--; return i == 127;");
        this.assertScriptReturnsTrue("long i = 32768;                  i--; return i == 32767;");
        this.assertScriptReturnsTrue("long i = Integer.MAX_VALUE + 1L; i--; return i == Integer.MAX_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE     ;    i--; return i == Long.MAX_VALUE;");

        this.assertScriptReturnsTrue("long i = 0;                 return i-- == 0;");
        this.assertScriptReturnsTrue("long i = 32767;             return i-- == 32767;");
        this.assertScriptReturnsTrue("long i = -32768;            return i-- == -32768;");
        this.assertScriptReturnsTrue("long i = Integer.MIN_VALUE; return i-- == Integer.MIN_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE;    return i-- == Long.MIN_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;    return i-- == Long.MAX_VALUE;");

        // char

        this.assertScriptReturnsTrue("char c = 1;     c--; return c == 0;");
        this.assertScriptReturnsTrue("char c = 128;   c--; return c == 127;");
        this.assertScriptReturnsTrue("char c = 256;   c--; return c == 255;");
        this.assertScriptReturnsTrue("char c = 32768; c--; return c == 32767;");
        this.assertScriptReturnsTrue("char c = 0;     c--; return c == 65535;");

        this.assertScriptReturnsTrue("char c = 0;     return c-- == 0;");
        this.assertScriptReturnsTrue("char c = 127;   return c-- == 127;");
        this.assertScriptReturnsTrue("char c = 128;   return c-- == 128;");
        this.assertScriptReturnsTrue("char c = 255;   return c-- == 255;");
        this.assertScriptReturnsTrue("char c = 256;   return c-- == 256;");
        this.assertScriptReturnsTrue("char c = 32767; return c-- == 32767;");
        this.assertScriptReturnsTrue("char c = 32768; return c-- == 32768;");
        this.assertScriptReturnsTrue("char c = 65535; return c-- == 65535;");

        // float

        this.assertScriptReturnsTrue("float f = 4.0F; f--; return f == 3.0F;");

        // double

        this.assertScriptReturnsTrue("double d = 18.9; d--; return d == 17.9;");

        // boolean

        this.assertScriptUncookable("boolean b = true; b--;");
    }

    @Test public void
    test_15_15_1__Prefix_Increment_Operator() throws Exception {

        this.assertScriptReturnsTrue("int i = 7; ++i; return i == 8;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && ++i == 8 && i == 8;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;"
        );

        // byte

        this.assertScriptReturnsTrue("byte b = -1;  ++b; return b == 0;");
        this.assertScriptReturnsTrue("byte b = 0;   ++b; return b == 1;");
        this.assertScriptReturnsTrue("byte b = 127; ++b; return b == -128;");

        this.assertScriptReturnsTrue("byte b = 0;    return ++b == 1;");
        this.assertScriptReturnsTrue("byte b = 127;  return ++b == -128;");
        this.assertScriptReturnsTrue("byte b = -128; return ++b == -127;");

        // short

        this.assertScriptReturnsTrue("short s = -1;    ++s; return s == 0;");
        this.assertScriptReturnsTrue("short s = 0;     ++s; return s == 1;");
        this.assertScriptReturnsTrue("short s = 127;   ++s; return s == 128;");
        this.assertScriptReturnsTrue("short s = 32767; ++s; return s == -32768;");

        this.assertScriptReturnsTrue("short s = 0;      return ++s == 1;");
        this.assertScriptReturnsTrue("short s = 32767;  return ++s == -32768;");
        this.assertScriptReturnsTrue("short s = -32768; return ++s == -32767;");

        // int

        this.assertScriptReturnsTrue("int i = -1;                ++i; return i == 0;");
        this.assertScriptReturnsTrue("int i = 0;                 ++i; return i == 1;");
        this.assertScriptReturnsTrue("int i = 127;               ++i; return i == 128;");
        this.assertScriptReturnsTrue("int i = 32767;             ++i; return i == 32768;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; ++i; return i == Integer.MIN_VALUE;");

        this.assertScriptReturnsTrue("int i = 0;                 return ++i == 1;");
        this.assertScriptReturnsTrue("int i = 32767;             return ++i == 32768;");
        this.assertScriptReturnsTrue("int i = -32769;            return ++i == -32768;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; return ++i == Integer.MIN_VALUE + 1;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; return ++i == Integer.MIN_VALUE;");

        // long

        this.assertScriptReturnsTrue("long i = -1;                ++i; return i == 0;");
        this.assertScriptReturnsTrue("long i = 0;                 ++i; return i == 1;");
        this.assertScriptReturnsTrue("long i = 127;               ++i; return i == 128;");
        this.assertScriptReturnsTrue("long i = 32767;             ++i; return i == 32768;");
        this.assertScriptReturnsTrue("long i = Integer.MAX_VALUE; ++i; return i == Integer.MAX_VALUE + 1L;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;    ++i; return i == Long.MIN_VALUE;");

        this.assertScriptReturnsTrue("long i = 0;                      return ++i == 1;");
        this.assertScriptReturnsTrue("long i = 32767;                  return ++i == 32768;");
        this.assertScriptReturnsTrue("long i = -32769;                 return ++i == -32768;");
        this.assertScriptReturnsTrue("long i = Integer.MIN_VALUE - 1L; return ++i == Integer.MIN_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE;         return ++i == Long.MIN_VALUE + 1;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;         return ++i == Long.MIN_VALUE;");

        // char

        this.assertScriptReturnsTrue("char c = 0;     ++c; return c == 1;");
        this.assertScriptReturnsTrue("char c = 127;   ++c; return c == 128;");
        this.assertScriptReturnsTrue("char c = 255;   ++c; return c == 256;");
        this.assertScriptReturnsTrue("char c = 32767; ++c; return c == 32768;");
        this.assertScriptReturnsTrue("char c = 65535; ++c; return c == 0;");

        this.assertScriptReturnsTrue("char c = 0;     return ++c == 1;");
        this.assertScriptReturnsTrue("char c = 127;   return ++c == 128;");
        this.assertScriptReturnsTrue("char c = 128;   return ++c == 129;");
        this.assertScriptReturnsTrue("char c = 255;   return ++c == 256;");
        this.assertScriptReturnsTrue("char c = 256;   return ++c == 257;");
        this.assertScriptReturnsTrue("char c = 32767; return ++c == 32768;");
        this.assertScriptReturnsTrue("char c = 32768; return ++c == 32769;");
        this.assertScriptReturnsTrue("char c = 65535; return ++c == 0;");

        // float

        this.assertScriptReturnsTrue("float f = 3.0F; ++f; return f == 4.0F;");

        // double

        this.assertScriptReturnsTrue("double d = 17.9; ++d; return d == 18.9;");

        // boolean

        this.assertScriptUncookable("boolean b = true; ++b;");
    }

    @Test public void
    test_15_15_2__Prefix_Decrement_Operator() throws Exception {

        this.assertScriptReturnsTrue("int i = 7; --i; return i == 6;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); --i; return i.intValue() == 6;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && --i == 6 && i == 6;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;"
        );

        // byte

        this.assertScriptReturnsTrue("byte b = 0;    --b; return b == -1;");
        this.assertScriptReturnsTrue("byte b = 1;    --b; return b == 0;");
        this.assertScriptReturnsTrue("byte b = -128; --b; return b == 127;");

        this.assertScriptReturnsTrue("byte b = 0;    return --b == -1;");
        this.assertScriptReturnsTrue("byte b = 127;  return --b == 126;");
        this.assertScriptReturnsTrue("byte b = -128; return --b == 127;");

        // short

        this.assertScriptReturnsTrue("short s = 0;      --s; return s == -1;");
        this.assertScriptReturnsTrue("short s = 1;      --s; return s == 0;");
        this.assertScriptReturnsTrue("short s = 128;    --s; return s == 127;");
        this.assertScriptReturnsTrue("short s = -32768; --s; return s == 32767;");

        this.assertScriptReturnsTrue("short s = 0;      return --s == -1;");
        this.assertScriptReturnsTrue("short s = 32767;  return --s == 32766;");
        this.assertScriptReturnsTrue("short s = -32768; return --s == 32767;");

        // int

        this.assertScriptReturnsTrue("int i = 0;                 --i; return i == -1;");
        this.assertScriptReturnsTrue("int i = 1;                 --i; return i == 0;");
        this.assertScriptReturnsTrue("int i = 128;               --i; return i == 127;");
        this.assertScriptReturnsTrue("int i = 32768;             --i; return i == 32767;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; --i; return i == Integer.MAX_VALUE;");

        this.assertScriptReturnsTrue("int i = 0;                 return --i == -1;");
        this.assertScriptReturnsTrue("int i = 127;               return --i == 126;");
        this.assertScriptReturnsTrue("int i = 128;               return --i == 127;");
        this.assertScriptReturnsTrue("int i = -128;              return --i == -129;");
        this.assertScriptReturnsTrue("int i = 32767;             return --i == 32766;");
        this.assertScriptReturnsTrue("int i = -32768;            return --i == -32769;");
        this.assertScriptReturnsTrue("int i = Integer.MIN_VALUE; return --i == Integer.MAX_VALUE;");
        this.assertScriptReturnsTrue("int i = Integer.MAX_VALUE; return --i == Integer.MAX_VALUE - 1;");

        // long

        this.assertScriptReturnsTrue("long i = 0;                      --i; return i == -1;");
        this.assertScriptReturnsTrue("long i = 1;                      --i; return i == 0;");
        this.assertScriptReturnsTrue("long i = 128;                    --i; return i == 127;");
        this.assertScriptReturnsTrue("long i = 32768;                  --i; return i == 32767;");
        this.assertScriptReturnsTrue("long i = Integer.MAX_VALUE + 1L; --i; return i == Integer.MAX_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE;         --i; return i == Long.MAX_VALUE;");

        this.assertScriptReturnsTrue("long i = 0;                 return --i == -1;");
        this.assertScriptReturnsTrue("long i = 32767;             return --i == 32766;");
        this.assertScriptReturnsTrue("long i = -32768;            return --i == -32769;");
        this.assertScriptReturnsTrue("long i = Integer.MIN_VALUE; return --i == Integer.MIN_VALUE - 1L;");
        this.assertScriptReturnsTrue("long i = Long.MIN_VALUE;    return --i == Long.MAX_VALUE;");
        this.assertScriptReturnsTrue("long i = Long.MAX_VALUE;    return --i == Long.MAX_VALUE - 1;");

        // char

        this.assertScriptReturnsTrue("char c = 1;     --c; return c == 0;");
        this.assertScriptReturnsTrue("char c = 128;   --c; return c == 127;");
        this.assertScriptReturnsTrue("char c = 256;   --c; return c == 255;");
        this.assertScriptReturnsTrue("char c = 32768; --c; return c == 32767;");
        this.assertScriptReturnsTrue("char c = 0;     --c; return c == 65535;");

        this.assertScriptReturnsTrue("char c = 0;     return --c == 65535;");
        this.assertScriptReturnsTrue("char c = 127;   return --c == 126;");
        this.assertScriptReturnsTrue("char c = 128;   return --c == 127;");
        this.assertScriptReturnsTrue("char c = 255;   return --c == 254;");
        this.assertScriptReturnsTrue("char c = 256;   return --c == 255;");
        this.assertScriptReturnsTrue("char c = 32767; return --c == 32766;");
        this.assertScriptReturnsTrue("char c = 32768; return --c == 32767;");
        this.assertScriptReturnsTrue("char c = 65535; return --c == 65534;");

        // float

        this.assertScriptReturnsTrue("float f = 4.0F; --f; return f == 3.0F;");

        // double

        this.assertScriptReturnsTrue("double d = 18.9; --d; return d == 17.9;");

        // boolean

        this.assertScriptUncookable("boolean b = true; --b;");
    }

    @Test public void
    test_15_15_3__Unary_Plus_Operator() throws Exception {
        this.assertExpressionEvaluatesTrue("new Integer(+new Integer(7)).intValue() == 7");
    }

    @Test public void
    test_15_15_4__Unary_Minus_Operator() throws Exception {
        this.assertExpressionEvaluatesTrue("new Integer(-new Integer(7)).intValue() == -7");
    }

    @Test public void
    test_15_17__Multiplicative_operators() throws Exception {
        this.assertExpressionEvaluatesTrue("new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");
    }

    @Test public void
    test_15_18_1__String_Concatenation_Operator_plus() throws Exception {
        this.assertScriptExecutable(
//            "            final IClassLoader\n" +
            "            final Object\n" +
//            "            iClassLoader = new CompilerIClassLoader(this.sourceFinder, this.classFileFinder, this.getIClassLoader());\n" +
            "            iClassLoader = new Object();\n" +
            "\n" +
            "            // Initialize compile time fields.\n" +
//            "            this.parsedCompilationUnits.clear();\n" +
            "            Object.class.hashCode();\n" +
            "\n" +
            "            // Parse all source files.\n" +
            "            for (Object sourceResource : new Object[1]) {\n" +
//            "//            for (int ii = 0; ii < sourceResources.length; ii++) {\n" +
//            "//                Resource sourceResource = sourceResources[ii];\n" +
            "\n" +
//            "                Compiler.LOGGER.log(Level.FINE, \"Compiling \\\"{0}\\\"\", sourceResource);\n" +
            "                Object.class.hashCode();\n" +
            "\n" +
            "                Object uc = new Object();\n" +
//            "                UnitCompiler uc = new UnitCompiler(\n" +
//            "                    this.parseAbstractCompilationUnit(\n" +
//            "                        sourceResource.getFileName(),                   // fileName\n" +
//            "                        new BufferedInputStream(sourceResource.open()), // inputStream\n" +
//            "                        this.sourceCharset                              // charset\n" +
//            "                    ),\n" +
//            "                    iClassLoader\n" +
//            "                );\n" +
//            "                uc.setTargetVersion(this.targetVersion);\n" +
//            "                uc.setCompileErrorHandler(this.compileErrorHandler);\n" +
//            "                uc.setWarningHandler(this.warningHandler);\n" +
//            "                uc.options(this.options);\n" +
            "                uc.hashCode();\n" +
//            "\n" +
//            "                this.parsedCompilationUnits.add(uc);\n" +
            "            }\n" +
            "\n" +
            "            // Compile all parsed compilation units. The vector of parsed CUs may grow while they are being compiled,\n" +
            "            // but eventually all CUs will be compiled.\n" +
            "            for (int i = 0; i < 3; ++i) {\n" +
//            "                UnitCompiler unitCompiler = (UnitCompiler) this.parsedCompilationUnits.get(i);\n" +
            "                Object unitCompiler = (Object) \"\";\n" +
//            "\n" +
//            "                File sourceFile;\n" +
            "                Object sourceFile;\n" +
            "                {\n" +
//            "                    Java.AbstractCompilationUnit acu = unitCompiler.getAbstractCompilationUnit();\n" +
//            "                    if (acu.fileName == null) throw new InternalCompilerException();\n" +
//            "                    sourceFile = new File(acu.fileName);\n" +
            "                    sourceFile = new Object();\n" +
            "                }\n" +
//            "\n" +
//            "                unitCompiler.setTargetVersion(this.targetVersion);\n" +
//            "                unitCompiler.setCompileErrorHandler(this.compileErrorHandler);\n" +
//            "                unitCompiler.setWarningHandler(this.warningHandler);\n" +
//            "\n" +
//            "                this.benchmark.beginReporting(\"Compiling compilation unit \\\"\" + sourceFile + \"\\\"\");\n" +
//            "                ClassFile[] classFiles;\n" +
            "                Object[] classFiles;\n" +
            "\n" +
            "                // Compile the compilation unit.\n" +
//            "                classFiles = unitCompiler.compileUnit(this.debugSource, this.debugLines, this.debugVars);\n" +
            "                classFiles = new Object[] { \"\" };\n" +
//            "\n" +
            "                // Store the compiled classes and interfaces into class files.\n" +
            "                new String(\n" +
            "                    \"Storing \"\n" +
//            "                    + classFiles.length\n" +
            "                    + 7\n" +
            "                    + \" class file(s) resulting from compilation unit \\\"\"\n" +
//            "                    + sourceFile\n" +
            "                    + new Object()\n" +
            "//                    + \"\\\"\"\n" +
            "                );\n" +
//            "                for (ClassFile classFile : classFiles) this.storeClassFile(classFile, sourceFile);\n" +
//            "                for (Object classFile : new Object[1]) System.out.printf(\"%s%s\", classFile, sourceFile);\n" +
            "            }\n" +
            ""
        );
    }

    @Test public void
    test_15_18__Additive_operators() throws Exception {
        // 15.18 Additive Operators -- Numeric
        this.assertExpressionEvaluatesTrue("(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        // 15.18.1.3 Additive Operators -- String Concatentation
        this.assertExpressionEvaluatesTrue(
            "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")"
        );
        this.assertExpressionEvaluatesTrue("1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        this.assertExpressionEvaluatesTrue("\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");

        // JAVAC does not supports "super-long string literals".
        Assume.assumeFalse(this.isJdk);

        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            this.assertExpressionEvaluatesTrue("\"" + s1 + "\".length() == " + i);
            this.assertExpressionEvaluatesTrue("(\"" + s1 + "\" + \"XXX\").length() == " + (i + 3));
        }
    }

    @Test public void
    test_15_20__Relation_operators() throws Exception {
        // 15.20.1 Numerical Comparison Operators <, <=, > and >=
        this.assertExpressionEvaluatesTrue("new Integer(7) > new Byte((byte) 5)");
    }

    @Test public void
    test_15_21__Equality_operators() throws Exception {

        // 15.21.1 Numerical Equality Operators == and !=
        this.assertExpressionUncookable("new Integer(7) != new Byte((byte) 5)");
        this.assertExpressionEvaluatesTrue("new Integer(7) == 7");
        this.assertExpressionEvaluatesTrue("new Byte((byte) -7) == -7");
        this.assertExpressionEvaluatesTrue("5 == new Byte((byte) 5)");

        // 15.21.2 Boolean Equality Operators == and !=
        this.assertExpressionEvaluatesTrue("new Boolean(true) != new Boolean(true)");
        this.assertExpressionEvaluatesTrue("new Boolean(true) == true");
        this.assertExpressionEvaluatesTrue("false == new Boolean(false)");
        this.assertExpressionEvaluatesTrue("false != true");

        // 15.21.3 Reference Equality Operators == and !=
        this.assertExpressionEvaluatesTrue("new Object() != new Object()");
        this.assertExpressionEvaluatesTrue("new Object() != null");
        this.assertExpressionEvaluatesTrue("new Object() != \"foo\"");
        this.assertExpressionUncookable("new Integer(3) == \"foo\"");
    }

    @Test public void
    test_15_22__Bitwise_and_logical_operators() throws Exception {

        // 15.22.1 Integer Bitwise Operators &, ^, and |
        this.assertExpressionEvaluatesTrue("(7 & 12) == 4");
        this.assertExpressionEvaluatesTrue("(7L & 12) == 4");
        this.assertExpressionUncookable("(7.0 & 12) == 4");
        this.assertExpressionEvaluatesTrue("(new Long(7L) & 12) == 4");
        this.assertExpressionEvaluatesTrue("(Long.valueOf(7) & Byte.valueOf((byte) 12)) == Short.valueOf((short) 4)");
        this.assertExpressionUncookable("(7 & Boolean.TRUE) == 4");

        // 15.22.2 Boolean Logical Operators &, ^, and |
        this.assertExpressionEvaluatesTrue("new Boolean(true) & new Boolean(true)");
        this.assertExpressionEvaluatesTrue("new Boolean(true) ^ false");
        this.assertExpressionEvaluatesTrue("false | new Boolean(true)");
    }

    @Test public void
    test_15_23__Conditional_and_operator() throws Exception {
        // 15.23 Conditional-And Operator &&
        this.assertExpressionEvaluatesTrue("new Boolean(true) && new Boolean(true)");
        this.assertExpressionEvaluatesTrue("new Boolean(true) && true");
        this.assertExpressionEvaluatesTrue("true && new Boolean(true)");
    }

    @Test public void
    test_15_24__Conditional_or_operator() throws Exception {
        // 15.24 Conditional-Or Operator ||
        this.assertExpressionEvaluatesTrue("new Boolean(true) || new Boolean(false)");
        this.assertExpressionEvaluatesTrue("new Boolean(false) || true");
        this.assertExpressionEvaluatesTrue("true || new Boolean(true)");
    }

    /**
     * 15.25 Conditional Operator ? :
     */
    @Test public void
    test_15_25__Conditional_operator__1() throws Exception {

        this.assertExpressionEvaluatesTrue("99 == (true ? 99 : -1)");
        this.assertExpressionEvaluatesTrue("-1 == (false ? 99 : -1)");

        this.assertExpressionEvaluatesTrue("99   == (true  ? 99   : null)");
        this.assertExpressionEvaluatesTrue("null == (false ? 99   : null)");
        this.assertExpressionEvaluatesTrue("null == (true  ? null : 99)");
        this.assertExpressionEvaluatesTrue("99   == (false ? null : 99)");

        // Related to "#85 Ternary expression resolves to strange supertype":
        this.assertScriptCookable(
            ""
            + "import java.util.*;\n"
            + "List list = true ? new ArrayList() : Arrays.asList(new String [] {});"
        );

        this.assertExpressionEvaluatesTrue("7 == (true ? 7 : 9)");
        this.assertExpressionEvaluatesTrue("9 == (Boolean.FALSE ? 7 : 9)");
        this.assertExpressionUncookable("1 ? 2 : 3)");
        this.assertExpressionUncookable("true ? 2 : System.currentTimeMillis())");

        // List 1, bullet 1
        this.assertExpressionEvaluatesTrue("(true ? 2 : 3) == 2");
        this.assertExpressionEvaluatesTrue("(true ? null : null) == null");

        // List 1, bullet 2
        this.assertExpressionEvaluatesTrue("(true ? 'a' : Character.valueOf('b')) == 'a'");

        // List 1, bullet 3
        this.assertExpressionEvaluatesTrue("(true ? \"\" : null).getClass() == String.class");

        // List 1, bullet 4, bullet 1
        this.assertScriptExecutable("short s = true ? (byte) 1 : (short) 2;");

        // List 1, bullet 4, bullet 2
        this.assertScriptUncookable("byte b = false ? (byte) 1 : -129;");
        this.assertScriptExecutable("byte b = false ? (byte) 1 : -128;");
        this.assertScriptExecutable("byte b = false ? (byte) 1 : 127;");
        this.assertScriptUncookable("byte b = false ? (byte) 1 : 128;");
        this.assertScriptUncookable("short s = false ? (short) 1 : -32769;");
        this.assertScriptExecutable("short s = false ? (short) 1 : -32768;");
        this.assertScriptExecutable("short s = false ? (short) 1 : 32767;");
        this.assertScriptUncookable("short s = false ? (short) 1 : 32768;");
        this.assertScriptUncookable("char c = false ? 'A' : -1;");
        this.assertScriptExecutable("char c = false ? 'A' : 0;");
        this.assertScriptExecutable("char c = false ? 'A' : 65535;");
        this.assertScriptUncookable("char c = false ? 'A' : 65536;");

        // List 1, bullet 4, bullet 3
        this.assertScriptUncookable("byte b = false ? Byte.valueOf((byte) 1) : -129;");
        this.assertScriptExecutable("byte b = false ? Byte.valueOf((byte) 1) : -128;");
        this.assertScriptExecutable("byte b = false ? Byte.valueOf((byte) 1) : 127;");
        this.assertScriptUncookable("byte b = false ? Byte.valueOf((byte) 1) : 128;");
        this.assertScriptUncookable("short s = false ? Short.valueOf((short) 1) : -32769;");
        this.assertScriptExecutable("short s = false ? Short.valueOf((short) 1) : -32768;");
        this.assertScriptExecutable("short s = false ? Short.valueOf((short) 1) : 32767;");
        this.assertScriptUncookable("short s = false ? Short.valueOf((short) 1) : 32768;");
        this.assertScriptUncookable("char c = false ? Character.valueOf('A') : -1;");
        this.assertScriptExecutable("char c = false ? Character.valueOf('A') : 0;");
        this.assertScriptExecutable("char c = false ? Character.valueOf('A') : 65535;");
        this.assertScriptUncookable("char c = false ? Character.valueOf('A') : 65536;");

        // List 1, bullet 4, bullet 4
        this.assertScriptExecutable("long l = false ? 1 : 1L;");
        this.assertScriptUncookable("int i = false ? 1 : 1L;");

        // List 1, bullet 5
        this.assertExpressionEvaluatesTrue("(true ? new Object() : \"\") != null");
        this.assertExpressionEvaluatesTrue("(true ? new Object() : 7).getClass().getName().equals(\"java.lang.Object\")"); // SUPPRESS CHECKSTYLE LineLength:11
        this.assertExpressionEvaluatesTrue("(true ? new Object() : Integer.valueOf(7)).getClass().getName().equals(\"java.lang.Object\")");
        this.assertExpressionEvaluatesTrue("(true ? Integer.valueOf(9) : Integer.valueOf(7)).getClass().getName().equals(\"java.lang.Integer\")");
        this.assertExpressionEvaluatesTrue("(true ? Integer.valueOf(9) : Long.valueOf(7)) == 9L");
        this.assertScriptCookable("import org.codehaus.commons.compiler.tests.JlsTest; (true ? new JlsTest.D1() : new JlsTest.D2()).c1();");
        this.assertScriptCookable("import org.codehaus.commons.compiler.tests.JlsTest; (true ? new JlsTest.D3() : new JlsTest.D4()).c1();");
        // Why, for god's sake, can JAVAC compile these assignments?? Some kind of type inference must happen here...
        if (this.isJdk) {
            this.assertScriptCookable("import org.codehaus.commons.compiler.tests.JlsTest; JlsTest.I1 i1 = (\"\".equals(\"\") ? new JlsTest.D3() : new JlsTest.D4());");
            this.assertScriptCookable("import org.codehaus.commons.compiler.tests.JlsTest; JlsTest.I2 i2 = (true ? new JlsTest.D3() : new JlsTest.D4());");
        }
        this.assertScriptUncookable("import org.codehaus.commons.compiler.tests.JlsTest; JlsTest.I3 i3 = (true ? new JlsTest.D3() : new JlsTest.D4());");

        // List 2, bullet 1
        this.assertScriptReturnsTrue("int a = 3; return (a == 0 ? ++a : a + a) == 6;");

        // List 2, bullet 2
        this.assertScriptReturnsTrue("int a = 3; return (a != 0 ? ++a : a + a) == 4;");
    }
    public static class C1                              { public void c1() {} } // SUPPRESS CHECKSTYLE Javadoc|Align:7
    public static class D1 extends C1                   {}
    public static class D2 extends C1                   {}
    public        interface I1                          { void                  i1(); }
    public        interface I2                          { void                  i2(); }
    public        interface I3                          { void                  i3(); }
    public static class D3 extends C1 implements I1, I2 { @Override public void i1() {} @Override public void i2() {} }
    public static class D4 extends C1 implements I1, I2 { @Override public void i1() {} @Override public void i2() {} } // SUPPRESS CHECKSTYLE Align|LineLength

    @Test public void
    test_15_25__Conditional_operator__2() throws Exception {
//        IScriptEvaluator eval = new ScriptEvaluator();
//        eval.setReturnType(Object[].class);
        String script = (
            ""
            + "class A {\n"
            + "    private Integer val;\n"
            + "    public A(Integer v) {\n"
            + "         val = v;\n"
            + "    }\n"
            + "    public boolean isNull() {\n"
            + "        return val == null;\n"
            + "    }\n"
            + "    public int getInt() {\n"
            + "        return val;\n"
            + "    }\n"
            + "}\n"
            + "A a = new A(3);\n"
            + "Object[] c = new Object[] {\n"
            + "    !a.isNull() ? (Object) a.getInt() : null,\n" // auto boxing & casting in LHS
            + "    !a.isNull() ? a.getInt() : null,\n"          // auto boxing & no explicit casting in LHS
            + "    a.isNull() ? null : (Object) a.getInt(),\n"  // auto boxing & casting in RHS
            + "    a.isNull() ? null : a.getInt(),\n"           // auto boxing & no explicit casting in RHS
            + "    (Object) \"hello\",\n"                       // simple casting
            + "};\n"
            + "return c;"
        );
        final Object[] result = (Object[]) this.assertScriptExecutable(script, Object[].class);
        Assert.assertArrayEquals(new Object[] {
            3,
            3,
            3,
            3,
            "hello",
        }, result);
    }

    /**
     * 15.26 Assignment Operators
     */
    @Test public void
    test_15_26__Assignment_operators() throws Exception {

        // 15.26.2 Compound Assignment Operators
        this.assertScriptReturnsTrue("int a = 7; a += 3; return a == 10;");
        this.assertScriptReturnsTrue("int a = 7; a %= 3; return a == 1;");
        this.assertScriptUncookable("Object a = \"foo\"; a += 3;");
        this.assertScriptUncookable("int a = 7; a += \"foo\";");
        this.assertScriptReturnsTrue("String a = \"foo\"; a += 3; return a.equals(\"foo3\");");
        this.assertScriptReturnsTrue("String a = \"foo\"; a += 'o'; return a.equals(\"fooo\");");
        this.assertScriptReturnsTrue("String a = \"foo\"; a += 1.0; return a.equals(\"foo1.0\");");
        this.assertScriptReturnsTrue("String[] a = { \"foo\" }; a[0] += 1.0; return a[0].equals(\"foo1.0\");");
        this.assertScriptReturnsTrue("Integer a = 7; a += 3; return a == 10;");
        this.assertScriptReturnsTrue("int a = 7; a += new Integer(3); return a == 10;");
        // JANINO-155: Compound assignment does not implement boxing conversion
        this.assertScriptReturnsTrue("Double[] a = { 1.0, 2.0 }; a[0] += 1.0; return a[0] == 2.0;");
    }

    @Test public void
    test_15_27_1__Lambda_parameters() throws Exception {

        // "java.util.Function" only since Java 10.
        if (CommonsCompilerTestSuite.JVM_VERSION < 10) return;

        this.assertScriptExecutable("java.util.function.Function<String, Integer> f = (var s) -> s.length();\n");
    }
}
