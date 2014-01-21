
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

import java.util.Arrays;
import java.util.List;

import org.codehaus.commons.compiler.ICompilerFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import util.JaninoTestSuite;
import util.TestUtil;

// CHECKSTYLE MethodName:OFF
// CHECKSTYLE JavadocMethod:OFF

/**
 * Tests against the <a href="http://docs.oracle.com/javase/specs/">Java Language Specification, Java SE 7 Edition</a>.
 */
@RunWith(Parameterized.class) public
class JlsTests extends JaninoTestSuite {

    @Parameters(name = "CompilerFactory={0}") public static List<Object[]>
    compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public
    JlsTests(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
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
    test_3_10_1__IntegerLiterals() throws Exception {
        this.assertExpressionEvaluatesTrue("17 == 17L");
        this.assertExpressionEvaluatesTrue("255 == 0xFFl");
        this.assertExpressionEvaluatesTrue("17 == 021L");
        this.assertExpressionUncookable("17 == 029"); // Digit "9" not allowed in octal literal.
        this.assertExpressionEvaluatesTrue("2 * 2147483647 == -2");
        this.assertExpressionEvaluatesTrue("2 * -2147483648 == 0");
        this.assertExpressionUncookable("2147483648");
        this.assertExpressionEvaluatable("-2147483648");
        this.assertExpressionEvaluatesTrue("-1 == 0xffffffff");
        this.assertExpressionEvaluatesTrue("1 == -0xffffffff");
        this.assertExpressionEvaluatesTrue("-0xf == -15");
        this.assertExpressionEvaluatable("9223372036854775807L");
        this.assertExpressionUncookable("9223372036854775808L");
        this.assertExpressionUncookable("9223372036854775809L");
        this.assertExpressionUncookable("99999999999999999999999999999L");
        this.assertExpressionEvaluatable("-9223372036854775808L");
        this.assertExpressionUncookable("-9223372036854775809L");
    }

    @Test public void
    test_3_10_2__FloatingPointLiterals() throws Exception {
        this.assertExpressionEvaluatesTrue("1e1f == 10f");
        this.assertExpressionEvaluatesTrue("1E1F == 10f");
        this.assertExpressionEvaluatesTrue(".3f == 0.3f");
        this.assertExpressionEvaluatesTrue("0f == (float) 0");
        this.assertExpressionEvaluatable("3.14f");
        this.assertExpressionEvaluatable("3.40282347e+38f");
        this.assertExpressionUncookable("3.40282357e+38f");
        this.assertExpressionEvaluatable("1.40239846e-45f");
        this.assertExpressionUncookable("7.0e-46f");
        this.assertExpressionEvaluatable("1.79769313486231570e+308D");
        this.assertExpressionUncookable("1.79769313486231581e+308d");
        this.assertExpressionEvaluatable("4.94065645841246544e-324D");
        this.assertExpressionUncookable("2e-324D");
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
            + "final Iterator<Integer> it = l.iterator();\n"
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
        // CHECKSTYLE LineLength:OFF
        this.assertScriptExecutable("assert true;");
        this.assertScriptReturnsTrue("try { assert false;                  } catch (AssertionError ae) { return ae.getMessage() == null;       } return false;");
        this.assertScriptReturnsTrue("try { assert false : \"x\";          } catch (AssertionError ae) { return \"x\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : 3;              } catch (AssertionError ae) { return \"3\".equals(ae.getMessage()); } return false;");
        this.assertScriptReturnsTrue("try { assert false : new Integer(8); } catch (AssertionError ae) { return \"8\".equals(ae.getMessage()); } return false;");
        // CHECKSTYLE LineLength:ON
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
            "Assignment conversion not possible from type \"int\" to type \"short\""
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
            "Assignment conversion not possible from type \"java.lang.String\" to type \"java.lang.Number\""
        );
        this.assertScriptReturnsTrue(
            "String x = \"A\"; String[] sa = { \"B\",\"C\" }; for (String y : sa) x += y; return x.equals(\"ABC\");"
        );
        this.assertScriptReturnsTrue(
            "final StringBuilder sb = new StringBuilder();\n"
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
            + "    } catch (java.io.IOException ioe) {\n" // <= Not thrown by 'meth()', but JDK 6 doesn't detect that
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
    test_15_11__FieldAccessExpressions() throws Exception {
        // 15.11.2 Accessing Superclass Members using super
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
    test_15_12__MethodInvocationExpressions() throws Exception {
        // 15.12.2.2 Choose the Most Specific Method
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
        //  (Note: Some versions of javac tries a bit harder,
        //         choosing the only existing variable-arity method.
        //         Their reasoning seems to be that
        //         there is ambiguity amongst fixed-arity applicables, so
        //         picking a vararg is acceptable if that means there is
        //         no ambiguity.
        //         I have not been able to find any piece of documentation
        //         about this in the docs)

        this.assertClassBodyUncookable(
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
            + "static int meth(byte a, double b){\n"
            + "     return 2;\n"
            + "}\n"
        );
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
}
