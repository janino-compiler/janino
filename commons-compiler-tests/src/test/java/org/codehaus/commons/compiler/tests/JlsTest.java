
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

// SUPPRESS CHECKSTYLE JavadocMethod|MethodName:9999

package org.codehaus.commons.compiler.tests;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    private static double
    javaSpecificationVersion = Double.parseDouble(System.getProperty("java.specification.version"));

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
            for (Handler h : scl.getHandlers()) {
                h.setLevel(Level.FINEST);
            }
            scl.setLevel(Level.FINEST);
        }
    }

    @Test public void
    test_3__LexicalStructure() throws Exception {
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
    test_3_10_1__IntegerLiterals_decimal() throws Exception {
        this.assertExpressionEvaluatesTrue("17 == 17L");
    }

    @Test public void
    test_3_10_1__IntegerLiterals_hex() throws Exception {
        this.assertExpressionEvaluatesTrue("255 == 0xFFl");
    }

    @Test public void
    test_3_10_1__IntegerLiterals_octal() throws Exception {
        this.assertExpressionEvaluatesTrue("17 == 021L");
        this.assertExpressionUncookable("17 == 029", Pattern.compile(
            ""
            + "Digit '9' not allowed in octal literal"
            + "|"
            + "compiler.err.int.number.too.large"
        ));
    }

    @Test public void
    test_3_10_1__IntegerLiterals_int_range() throws Exception {
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

        if (!this.isJdk6()) {
            this.assertExpressionEvaluatesTrue("- -2147_483648  == -2147483648");
        }
    }

    @Test public void
    test_3_10_1__IntegerLiterals_long_range() throws Exception {
        this.assertExpressionEvaluatable("9223372036854775807L");
        this.assertExpressionUncookable("9223372036854775808L");
        this.assertExpressionUncookable("9223372036854775809L");
        this.assertExpressionUncookable("99999999999999999999999999999L");
        this.assertExpressionEvaluatable("-9223372036854775808L");
        this.assertExpressionUncookable("-9223372036854775809L");

        // https://github.com/janino-compiler/janino/issues/41 :
        this.assertExpressionEvaluatesTrue("-(-9223372036854775808L) == -9223372036854775808L");
        this.assertExpressionEvaluatesTrue("- -9223372036854775808L  == -9223372036854775808L");
        if (!this.isJdk6()) {
            this.assertExpressionEvaluatesTrue("- -922337_2036854775808L == -9223372036854775808L");
            this.assertExpressionUncookable("- -9223372036854775808_L == -9223372036854775808L");
        }
    }

    @Test public void
    test_3_10_1__IntegerLiterals_binary() throws Exception {

        // "Binary numeric literals" is a Java 7 feature, so JDKs before 1.7 don't support it.
        Assume.assumeFalse(this.isJdk6());

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
    test_3_10_1__IntegerLiterals_underscores() throws Exception {

        // "Underscores in numeric literals" is a Java 7 feature, so JDKs before 1.7 don't support
        // it.
        Assume.assumeFalse(this.isJdk6());

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
    test_3_10_2__FloatingPointLiterals_float() throws Exception {
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
    test_3_10_2__FloatingPointLiterals_double() throws Exception {
        this.assertExpressionEvaluatable("1.79769313486231570e+308D");
        this.assertExpressionUncookable("1.79769313486231581e+308d");
        this.assertExpressionEvaluatable("4.94065645841246544e-324D");
        this.assertExpressionUncookable("2e-324D");
    }

    @Test public void
    test_3_10_2__FloatingPointLiterals_hexadecimal() throws Exception {
        this.assertExpressionEvaluatesTrue("0x1D == 29"); // "D" is NOT a float fixxif, but a hex digit!
        this.assertExpressionEvaluatesTrue("0x1p0D == 1");
        this.assertExpressionEvaluatesTrue("0x.8p0 == 0.5");
        this.assertExpressionEvaluatesTrue("0x1p0 == 1");
        this.assertExpressionEvaluatesTrue("0xfp1 == 30");
        this.assertExpressionEvaluatesTrue("0xfp+1 == 30");
        this.assertExpressionEvaluatesTrue("0xfp-1 == 7.5");
        this.assertExpressionEvaluatesTrue("0x1.0004p0F    == 0x1.0004p0");
        this.assertExpressionEvaluatesTrue("0x1.0000004p0F != 0x1.0000004p0");
    }

    @Test public void
    test_3_10_2__FloatingPointLiterals_underscores() throws Exception {

        // "Underscores in numeric literals" is a Java 7 feature, so JDKs before 1.7 don't support
        // it.
        Assume.assumeFalse(this.isJdk6());

        this.assertExpressionEvaluatesTrue("1___0.1___0 == 10.1");
    }

    @Test public void
    test_3_10_3__BooleanLiterals() throws Exception {
        this.assertExpressionEvaluatesTrue("true");
        this.assertExpressionEvaluatesTrue("! false");
    }

    @Test public void
    test_3_10_4__CharacterLiterals() throws Exception {
        this.assertExpressionEvaluatesTrue("'a' == 97");
        this.assertExpressionUncookable("'''");
        this.assertExpressionUncookable("'\\'");
        this.assertExpressionUncookable("'\n'");
        this.assertExpressionUncookable("'ax'");
        this.assertExpressionUncookable("'a\n'");
        this.assertExpressionEvaluatesTrue("'\"' == 34"); // Unescaped double quote is allowed!
    }

    @Test public void
    test_3_10_5__StringLiterals() throws Exception {
        this.assertExpressionEvaluatesTrue("\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        this.assertExpressionEvaluatesTrue("\"\\b\".charAt(0) == 8");
        this.assertExpressionUncookable("\"aaa\nbbb\"");
        this.assertExpressionUncookable("\"aaa\rbbb\"");
        this.assertExpressionEvaluatesTrue("\"aaa\" == \"aaa\"");
        this.assertExpressionEvaluatesTrue("\"aaa\" != \"bbb\"");
    }

    @Test public void
    test_3_10_6__EscapeSequencesForCharacterAndStringLiterals() throws Exception {
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
    test_3_10_7__TheNullLiteral() throws Exception {
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
    test_4_5_1__TypeArgumentsAndWildcards() throws Exception {
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
    test_5_1_7__BoxingConversion() throws Exception {
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
    test_5_1_8__UnboxingConversion() throws Exception {
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
    test_5_2__AssignmentConversion() throws Exception {
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
    test_5_5__CastingConversion() throws Exception {
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
    test_5_6__NumberPromotions() throws Exception {
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
    test_6_6_1_Determining_Accessibility_member_access() throws Exception {

        // SUPPRESS CHECKSTYLE Whitespace|LineLength:4
        this.assertExpressionEvaluatesTrue("for_sandbox_tests.ClassWithFields.publicField        == 1");
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.protectedField     == 2", Pattern.compile("Protected member cannot be accessed|compiler.err.report.access"));
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.packageAccessField == 3", Pattern.compile("Member with \"package\" access cannot be accessed|compiler.err.not.def.public.cant.access"));
        this.assertExpressionUncookable   ("for_sandbox_tests.ClassWithFields.privateField       == 4", Pattern.compile("Private member cannot be accessed|compiler.err.report.access"));
    }

    @Test public void
    test_7_5__ImportDeclarations() throws Exception {
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

        this.assertExpressionUncookable(
            "new Object() { public void toString() {}}.toString()",
            Pattern.compile(
                ""
                + "The return type of.*is incompatible with"
                + "|"
                + "compiler.err.override.incompatible.ret"
                + "|"
                + "attempting to use incompatible return type"
            )
        );
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

        if (this.isJanino()) {
            this.assertCompilationUnitUncookable(cu, "Static interface methods not implemented");
        } else {
            this.assertCompilationUnitMainReturnsTrue(cu, "Main");
        }
    }

    @Test public void
    test_9_4__Method_Declarations__4() throws Exception {

        String cu = (
            ""
            + "public interface A {\n"
            + "    default A meth1() { return null; }\n"
            + "}\n"
        );

        if (this.isJanino()) {
            this.assertCompilationUnitUncookable(cu, "Default interface methods not implemented");
        } else {
            this.assertCompilationUnitCookable(cu);
        }
    }

    @Test public void
    test_9_6_Annotation_Types() throws Exception {

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
    test_9_6_2__DefaultsForAnnotationTypeElements() throws Exception {
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
    test_9_7_2_Marker_Annotations() throws Exception {

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
    test_9_7_3_Single_Element_Annotations() throws Exception {

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
    test_9_7_1_Normal_Annotations1() throws Exception {

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
    test_9_7_1_Normal_Annotations2() throws Exception {

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
    test_9_7_4_Where_Annotations_May_Appear_field() throws Exception {

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
    test_9_7_4_Where_Annotations_May_Appear_method() throws Exception {

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
    test_14_3__LocalClassDeclarations() throws Exception {
        this.assertScriptReturnsTrue(
            "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;"
        );
    }

    @Test public void
    test_14_8__ExpressionStatements() throws Exception {
        this.assertScriptReturnsTrue("int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        this.assertScriptExecutable("System.currentTimeMillis();");
        this.assertScriptExecutable("new Object();");
        this.assertScriptUncookable("new Object[3];");
        this.assertScriptUncookable("int a; a;");
    }

    @Test public void
    test_14_10__TheAssertStatement() throws Exception {

        // SUPPRESS CHECKSTYLE LineLength:5
        this.assertScriptExecutable("assert true;");
        Assert.assertNull(this.assertScriptExecutable("try { assert false;                  } catch (AssertionError ae) { return ae.getMessage();       } return \"nope\";", String.class));
        this.assertScriptReturnsTrue("try { assert false : \"x\";          } catch (AssertionError ae) { return \"x\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : 3;              } catch (AssertionError ae) { return \"3\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : new Integer(8); } catch (AssertionError ae) { return \"8\".equals(ae.getMessage()); } return false;");
    }

    @Test public void
    test_14_11__TheSwitchStatement() throws Exception {
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
    test_14_11__TheSwitchStatement_enum() throws Exception {
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
    test_14_11__TheSwitchStatement_String1() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_11__TheSwitchStatement_String2() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_11__TheSwitchStatement_String3() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_11__TheSwitchStatement_String4() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_11__TheSwitchStatement_String5() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_11__TheSwitchStatement_String_DuplicateCaseValue() throws Exception {

        // JDK 6 does not support string SWITCH.
        if (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && JlsTest.javaSpecificationVersion == 1.6
        ) return;

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
    test_14_14_2_1__TheEnhancedForStatement_Iterable() throws Exception {
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
    }

    @Test public void
    test_14_14_2_2__TheEnhancedForStatement_Array() throws Exception {

        // Primitive array.
        this.assertScriptReturnsTrue(
            "int x = 1; for (int y : new int[] { 1, 2, 3 }) x += x * y; return x == 24;"
        );
        this.assertScriptReturnsTrue(
            "int x = 1; for (int y : new short[] { 1, 2, 3 }) x += x * y; return x == 24;"
        );
        this.assertScriptUncookable(
            "int x = 1; for (short y : new int[] { 1, 2, 3 }) x += x * y;",
            Pattern.compile(
                "Assignment conversion not possible from type \"int\" to type \"short\""
                + "|"
                + "possible loss of precision"
                + "|"
                + "possible lossy conversion from int to short"
            )
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
            Pattern.compile(
                "Assignment conversion not possible from type \"java.lang.String\" to type \"java.lang.Number\""
                + "|"
                + "incompatible types"
            )
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
    test_14_20_1__ExecutionOfTryCatch__1() throws Exception {
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
    test_14_20_1__ExecutionOfTryCatch__2() throws Exception {
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
    test_14_20_1__ExecutionOfTryCatch__3() throws Exception {
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
    test_14_20_1__ExecutionOfTryCatch__4() throws Exception {

        // JAVAC does not detect this condition although, I believe, it should, according to the
        // JLS.
        Assume.assumeFalse(this.compilerFactory.getId().equals("org.codehaus.commons.compiler.jdk"));

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
    test_14_20_1__ExecutionOfTryCatch__5() throws Exception {
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
    test_14_20_1__ExecutionOfTryCatch__6() throws Exception {
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
    test_14_20_3__try_with_resources__1() throws Exception {

        if (this.isJdk6()) return;

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

        if (this.isJdk6()) return;

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

        if (this.isJdk6()) return;

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

        if (this.isJdk6()) return;

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

        if (this.isJdk678()) return;

        this.assertScriptExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "import org.junit.Assert;\n"
            + "try {\n"
            + "    final int[] x = new int[1];\n"
            + "    Closeable lv = new Closeable() {\n"
            + "        public void close() { Assert.assertEquals(2, ++x[0]); }\n"
            + "    };\n"
            + "    \n"
            + "    try (lv) {\n"
            + "        Assert.assertEquals(1, ++x[0]);\n"
            + "    }\n"
            + "    Assert.assertEquals(3, ++x[0]);\n"
            + "} catch (IOException ioe) {\n"
            + "    Assert.fail(ioe.toString());\n"
            + "}\n"
        );
    }

    /**
     * Tests the "enhanced try-with-resources statement" that was introduced with Java 9 with a "variable access
     * resource" with a static field access.
     */
    @Test public void
    test_14_20_3__try_with_resources__10b() throws Exception {

        if (this.isJdk678()) return;

        this.assertClassBodyExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "import org.junit.Assert;\n"
            + "\n"
            + "public static final Closeable sf = new Closeable() {\n"
            + "    public void close() { Assert.assertEquals(2, ++x[0]); }\n"
            + "};\n"
            + "public static final int[] x = new int[1];\n"
            + "\n"
            + "public static void main() {\n"
            + "    try {\n"
            + "        \n"
            + "        try (SC.sf) {\n"
            + "            Assert.assertEquals(1, ++x[0]);\n"
            + "        }\n"
            + "        Assert.assertEquals(3, ++x[0]);\n"
            + "    } catch (IOException ioe) {\n"
            + "        Assert.fail(ioe.toString());\n"
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

        if (this.isJdk678()) return;

        this.assertClassBodyExecutable(
            ""
            + "import java.io.Closeable;\n"
            + "import java.io.IOException;\n"
            + "import org.junit.Assert;\n"
            + "\n"
            + "public final Closeable sf = new Closeable() {\n"
            + "    public void close() { Assert.assertEquals(2, ++SC.this.x[0]); }\n"
            + "};\n"
            + "public final int[] x = new int[1];\n"
            + "\n"
            + "public void main() {\n"
            + "    try {\n"
            + "        \n"
            + "        try (this.sf) {\n"
            + "            Assert.assertEquals(1, ++this.x[0]);\n"
            + "        }\n"
            + "        Assert.assertEquals(3, ++this.x[0]);\n"
            + "    } catch (IOException ioe) {\n"
            + "        Assert.fail(ioe.toString());\n"
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

        if (this.isJdk678()) return;

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
            "NewAnonymousClassInstance rvalue not allowed as a resource"
        );
    }

    @Test public void
    test_14_21__UnreachableStatements() throws Exception {
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
    test_15_9__ClassInstanceCreationExpressions() throws Exception {
        // 15.9.1 Determining the class being Instantiated
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

        // 15.9.3 Choosing the Constructor and its Arguments
        this.assertExpressionEvaluatable("new Integer(3)");
        this.assertExpressionEvaluatable("new Integer(new Integer(3))");
        this.assertExpressionEvaluatable("new Integer(new Byte((byte) 3))");
        this.assertExpressionUncookable("new Integer(new Object())");

        // 15.9.5 Anonymous Class Declarations
        this.assertCompilationUnitMainExecutable((
            ""
            + "public class Foo {\n"
            + "    public static void main() { new Foo().meth(); }\n"
            + "    private Object meth() {\n"
            + "        return new Object() {};\n"
            + "    }\n"
            + "}\n"
        ), "Foo");
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
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__1() throws Exception {
        this.assertExpressionEvaluatesTrue("\"two one\".equals(String.format(\"%2$s %1$s\", \"one\", \"two\"))");
    }

    @Test public void
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__2() throws Exception {
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
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__3() throws Exception {
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
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__4() throws Exception {
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
    test_15_12_2_6__IdentifyApplicableVariableArityMethods__4() throws Exception {
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
    test_15_12_2_7__IdentifyApplicableVariableArityMethods__4() throws Exception {

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
        Assume.assumeFalse(this.isJdk7());

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
            + "     return 2;\n"
            + "}\n"
        ), Pattern.compile((
            ""
            + "Invocation of constructor/method with argument type\\(s\\) \"byte, byte\" is ambiguous"
            + "|"
            + "compiler\\.err\\.ref\\.ambiguous"
        )));
    }

    @Test public void
    test_15_2_2_5_ChoosingTheMostSpecificVarargMethod_1() throws Exception {
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
    test_15_14__PostfixExpressions() throws Exception {
        // "15.14.2 Postfix Increment Operator ++
        this.assertScriptReturnsTrue("int i = 7; i++; return i == 8;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); i++; return i.intValue() == 8;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && i++ == 7 && i == 8;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;"
        );

        // 15.14.3 Postfix Decrement Operator --
        this.assertScriptReturnsTrue("int i = 7; i--; return i == 6;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); i--; return i.intValue() == 6;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && i-- == 7 && i == 6;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;"
        );
    }

    @Test public void
    test_15_15__UnaryOperators() throws Exception {
        // 15.15.1 Prefix Increment Operator ++
        this.assertScriptReturnsTrue("int i = 7; ++i; return i == 8;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && ++i == 8 && i == 8;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;"
        );

        // 15.15.2 Prefix Decrement Operator --
        this.assertScriptReturnsTrue("int i = 7; --i; return i == 6;");
        this.assertScriptReturnsTrue("Integer i = new Integer(7); --i; return i.intValue() == 6;");
        this.assertScriptReturnsTrue("int i = 7; return i == 7 && --i == 6 && i == 6;");
        this.assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;"
        );

        // 15.15.3 Unary Plus Operator +
        this.assertExpressionEvaluatesTrue("new Integer(+new Integer(7)).intValue() == 7");

        // 15.15.4 Unary Minus Operator -
        this.assertExpressionEvaluatesTrue("new Integer(-new Integer(7)).intValue() == -7");
    }

    @Test public void
    test_15_17__MultiplicativeOperators() throws Exception {
        this.assertExpressionEvaluatesTrue("new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");
    }

    @Test public void
    test_15_18__AdditiveOperators() throws Exception {
        // 15.18 Additive Operators -- Numeric
        this.assertExpressionEvaluatesTrue("(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        // 15.18.1.3 Additive Operators -- String Concatentation
        this.assertExpressionEvaluatesTrue(
            "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")"
        );
        this.assertExpressionEvaluatesTrue("1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        this.assertExpressionEvaluatesTrue("\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");

        // JAVAC does not supports "super-long string literals".
        Assume.assumeFalse(this.compilerFactory.getId().equals("org.codehaus.commons.compiler.jdk"));

        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            this.assertExpressionEvaluatesTrue("\"" + s1 + "\".length() == " + i);
            this.assertExpressionEvaluatesTrue("(\"" + s1 + "\" + \"XXX\").length() == " + (i + 3));
        }
    }

    @Test public void
    test_15_20__RelationOperators() throws Exception {
        // 15.20.1 Numerical Comparison Operators <, <=, > and >=
        this.assertExpressionEvaluatesTrue("new Integer(7) > new Byte((byte) 5)");
    }

    @Test public void
    test_15_21__EqualityOperators() throws Exception {

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
    test_15_22__BitwiseAndLogicalOperators() throws Exception {
        // 15.22.2 Boolean Logical Operators &, ^, and |
        this.assertExpressionEvaluatesTrue("new Boolean(true) & new Boolean(true)");
        this.assertExpressionEvaluatesTrue("new Boolean(true) ^ false");
        this.assertExpressionEvaluatesTrue("false | new Boolean(true)");
    }

    @Test public void
    test_15_23__ConditionalAndOperator() throws Exception {
        // 15.23 Conditional-And Operator &&
        this.assertExpressionEvaluatesTrue("new Boolean(true) && new Boolean(true)");
        this.assertExpressionEvaluatesTrue("new Boolean(true) && true");
        this.assertExpressionEvaluatesTrue("true && new Boolean(true)");
    }

    @Test public void
    test_15_24__ConditionalOrOperator() throws Exception {
        // 15.24 Conditional-Or Operator ||
        this.assertExpressionEvaluatesTrue("new Boolean(true) || new Boolean(false)");
        this.assertExpressionEvaluatesTrue("new Boolean(false) || true");
        this.assertExpressionEvaluatesTrue("true || new Boolean(true)");
    }

    /**
     * 15.25 Conditional Operator ? :
     */
    @Test public void
    test_15_25__ConditionalOperator() throws Exception {

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

        // List 2, bullet 1
        this.assertScriptReturnsTrue("int a = 3; return (a == 0 ? ++a : a + a) == 6;");

        // List 2, bullet 2
        this.assertScriptReturnsTrue("int a = 3; return (a != 0 ? ++a : a + a) == 4;");
    }

    /**
     * 15.26 Assignment Operators
     */
    @Test public void
    test_15_26__AssignmentOperators() throws Exception {

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

    /**
     * @return Whether we're running the JDK 6 (or earlier) compiler factory
     */
    private boolean
    isJdk6() {
        return (
            this.compilerFactory.getId().equals("org.codehaus.commons.compiler.jdk")
            && System.getProperty("java.version").matches("1\\.[1-6].*")
        );
    }

    /**
     * @return Whether we're running the JDK 7 compiler factory
     */
    private boolean
    isJdk7() {
        return (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && System.getProperty("java.version").startsWith("1.7.0")
        );
    }

    /**
     * @return Whether we're running the JDK 8 compiler factory
     */
    private boolean
    isJdk8() {
        return (
            "org.codehaus.commons.compiler.jdk".equals(this.compilerFactory.getId())
            && System.getProperty("java.version").startsWith("1.8.0")
        );
    }

    /**
     * @return Whether we're running the JDK 6, 7 or 8 compiler factory
     */
    private boolean
    isJdk678() { return this.isJdk6() || this.isJdk7() || this.isJdk8(); }

    /**
     * @return Whether we're using the JANINO compiler factory
     */
    private boolean
    isJanino() { return "org.codehaus.janino".equals(this.compilerFactory.getId()); }
}
