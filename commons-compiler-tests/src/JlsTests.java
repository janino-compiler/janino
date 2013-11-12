
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

    @Parameters public static List<Object[]>
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
        assertExpressionEvaluatesTrue("'\\u00e4' == '\u00e4'");

        // 3.2. Lexical Structure -- Translations
        assertScriptUncookable("3--4");

        // 3.3. Lexical Structure -- Unicode Escapes
        assertExpressionUncookable("aaa\\u123gbbb");
        assertExpressionEvaluatesTrue("\"\\u0041\".equals(\"A\")");
        assertExpressionEvaluatesTrue("\"\\uu0041\".equals(\"A\")");
        assertExpressionEvaluatesTrue("\"\\uuu0041\".equals(\"A\")");
        assertExpressionEvaluatesTrue("\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        assertExpressionEvaluatesTrue("\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");

        // 3.3. Lexical Structure -- Line Terminators
        assertExpressionEvaluatesTrue("1//\r+//\r\n2//\n==//\n\r3");

        // 3.6. Lexical Structure -- White Space
        assertExpressionEvaluatesTrue("3\t\r \n==3");

        // 3.7. Lexical Structure -- Comments
        assertExpressionEvaluatesTrue("7/* */==7");
        assertExpressionEvaluatesTrue("7/**/==7");
        assertExpressionEvaluatesTrue("7/***/==7");
        assertExpressionUncookable("7/*/==7");
        assertExpressionEvaluatesTrue("7/*\r*/==7");
        assertExpressionEvaluatesTrue("7//\r==7");
        assertExpressionEvaluatesTrue("7//\n==7");
        assertExpressionEvaluatesTrue("7//\r\n==7");
        assertExpressionEvaluatesTrue("7//\n\r==7");
        assertScriptUncookable("7// /*\n\rXXX*/==7");

        // 3.8. Lexical Structure -- Identifiers
        assertScriptExecutable("int a;");
        assertScriptExecutable("int \u00e4\u00e4\u00e4;");
        assertScriptExecutable("int \\u0391;"); // Greek alpha
        assertScriptExecutable("int _aaa;");
        assertScriptExecutable("int $aaa;");
        assertScriptUncookable("int 9aaa;");
        assertScriptUncookable("int const;");
    }

    @Test public void
    test_3_10_1__IntegerLiterals() throws Exception {
        assertExpressionEvaluatesTrue("17 == 17L");
        assertExpressionEvaluatesTrue("255 == 0xFFl");
        assertExpressionEvaluatesTrue("17 == 021L");
        assertExpressionUncookable("17 == 029"); // Digit "9" not allowed in octal literal.
        assertExpressionEvaluatesTrue("2 * 2147483647 == -2");
        assertExpressionEvaluatesTrue("2 * -2147483648 == 0");
        assertExpressionUncookable("2147483648");
        assertExpressionEvaluatable("-2147483648");
        assertExpressionEvaluatesTrue("-1 == 0xffffffff");
        assertExpressionEvaluatesTrue("1 == -0xffffffff");
        assertExpressionEvaluatesTrue("-0xf == -15");
        assertExpressionEvaluatable("9223372036854775807L");
        assertExpressionUncookable("9223372036854775808L");
        assertExpressionUncookable("9223372036854775809L");
        assertExpressionUncookable("99999999999999999999999999999L");
        assertExpressionEvaluatable("-9223372036854775808L");
        assertExpressionUncookable("-9223372036854775809L");
    }

    @Test public void
    test_3_10_2__FloatingPointLiterals() throws Exception {
        assertExpressionEvaluatesTrue("1e1f == 10f");
        assertExpressionEvaluatesTrue("1E1F == 10f");
        assertExpressionEvaluatesTrue(".3f == 0.3f");
        assertExpressionEvaluatesTrue("0f == (float) 0");
        assertExpressionEvaluatable("3.14f");
        assertExpressionEvaluatable("3.40282347e+38f");
        assertExpressionUncookable("3.40282357e+38f");
        assertExpressionEvaluatable("1.40239846e-45f");
        assertExpressionUncookable("7.0e-46f");
        assertExpressionEvaluatable("1.79769313486231570e+308D");
        assertExpressionUncookable("1.79769313486231581e+308d");
        assertExpressionEvaluatable("4.94065645841246544e-324D");
        assertExpressionUncookable("2e-324D");
    }

    @Test public void
    test_3_10_3__BooleanLiterals() throws Exception {
        assertExpressionEvaluatesTrue("true");
        assertExpressionEvaluatesTrue("! false");
    }

    @Test public void
    test_3_10_4__CharacterLiterals() throws Exception {
        assertExpressionEvaluatesTrue("'a' == 97");
        assertExpressionUncookable("'''");
        assertExpressionUncookable("'\\'");
        assertExpressionUncookable("'\n'");
        assertExpressionUncookable("'ax'");
        assertExpressionUncookable("'a\n'");
        assertExpressionEvaluatesTrue("'\"' == 34"); // Unescaped double quote is allowed!
    }

    @Test public void
    test_3_10_5__StringLiterals() throws Exception {
        assertExpressionEvaluatesTrue("\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        assertExpressionEvaluatesTrue("\"\\b\".charAt(0) == 8");
        assertExpressionUncookable("\"aaa\nbbb\"");
        assertExpressionUncookable("\"aaa\rbbb\"");
        assertExpressionEvaluatesTrue("\"aaa\" == \"aaa\"");
        assertExpressionEvaluatesTrue("\"aaa\" != \"bbb\"");
    }

    @Test public void
    test_3_10_6__EscapeSequencesForCharacterAndStringLiterals() throws Exception {
        assertExpressionUncookable("'\\u000a'"); // 0x000a is LF
        assertExpressionEvaluatesTrue("'\\b' == 8");
        assertExpressionEvaluatesTrue("'\\t' == 9");
        assertExpressionEvaluatesTrue("'\\n' == 10");
        assertExpressionEvaluatesTrue("'\\f' == 12");
        assertExpressionEvaluatesTrue("'\\r' == 13");
        assertExpressionEvaluatesTrue("'\\\"' == 34");
        assertExpressionEvaluatesTrue("'\\'' == 39");
        assertExpressionEvaluatesTrue("'\\\\' == 92");
        assertExpressionEvaluatesTrue("'\\0' == 0");
        assertExpressionEvaluatesTrue("'\\07' == 7");
        assertExpressionEvaluatesTrue("'\\377' == 255");
        assertExpressionUncookable("'\\400'");
        assertExpressionUncookable("'\\1234'");
    }

    @Test public void
    test_3_10_7__TheNullLiteral() throws Exception {
        assertExpressionEvaluatable("null");
    }

    @Test public void
    test_3_11__Separators() throws Exception {
        assertScriptExecutable(";");
    }

    @Test public void
    test_3_12__Operators() throws Exception {
        assertScriptReturnsTrue("int a = -11; a >>>= 2; return a == 1073741821;");
    }
    
    @Test public void
    test_4_5_1__TypeArgumentsAndWildcards() throws Exception {
        assertScriptReturnsTrue(
            ""
            + "import java.util.*;\n"
            + "final List<String> l = new ArrayList();\n"
            + "l.add(\"x\");\n" 
            + "final Iterator<Integer> it = l.iterator();\n"
            + "return it.hasNext();"
        );
    }

    @Test public void
    test_5_1_7__BoxingConversion() throws Exception {
        assertScriptReturnsTrue("Boolean   b = true;        return b.booleanValue();");
        assertScriptReturnsTrue("Boolean   b = false;       return !b.booleanValue();");
        assertScriptReturnsTrue("Byte      b = (byte) 7;    return b.equals(new Byte((byte) 7));");
        assertScriptReturnsTrue("Character c = 'X';         return c.equals(new Character('X'));");
        assertScriptReturnsTrue("Short     s = (short) 322; return s.equals(new Short((short) 322));");
        assertScriptReturnsTrue("Integer   i = 99;          return i.equals(new Integer(99));");
        assertScriptReturnsTrue("Long      l = 733L;        return l.equals(new Long(733L));");
        assertScriptReturnsTrue("Float     f = 12.5F;       return f.equals(new Float(12.5F));");
        assertScriptReturnsTrue("Double    d = 14.3D;       return d.equals(new Double(14.3D));");
    }

    @Test public void
    test_5_1_8__UnboxingConversion() throws Exception {
        assertExpressionEvaluatesTrue("Boolean.TRUE");
        assertExpressionEvaluatesTrue("!Boolean.FALSE");
        assertExpressionEvaluatesTrue("new Byte((byte) 9) == (byte) 9");
        assertExpressionEvaluatesTrue("new Character('Y') == 'Y'");
        assertExpressionEvaluatesTrue("new Short((short) 33) == (short) 33");
        assertExpressionEvaluatesTrue("new Integer(-444) == -444");
        assertExpressionEvaluatesTrue("new Long(987654321L) == 987654321L");
        assertExpressionEvaluatesTrue("new Float(33.3F) == 33.3F");
        assertExpressionEvaluatesTrue("new Double(939.939D) == 939.939D");
    }

    @Test public void
    test_5_2__AssignmentConversion() throws Exception {
        assertScriptReturnsTrue("int i = 7; return i == 7;");
        assertScriptReturnsTrue("String s = \"S\"; return s.equals(\"S\");");
        assertScriptReturnsTrue("long l = 7; return l == 7L;");
        assertScriptReturnsTrue("Object o = \"A\"; return o.equals(\"A\");");
        assertScriptReturnsTrue("Integer i = 7; return i.intValue() == 7;");
        assertScriptReturnsTrue("Object o = 7; return o.equals(new Integer(7));");
        assertScriptReturnsTrue("int i = new Integer(7); return i == 7;");
        assertScriptReturnsTrue("long l = new Integer(7); return l == 7L;");
        assertScriptExecutable("byte b = -128;");
        assertScriptUncookable("byte b = 128;");
        assertScriptExecutable("short s = -32768;");
        assertScriptUncookable("short s = 32768;");
        assertScriptUncookable("char c = -1;");
        assertScriptExecutable("char c = 0;");
        assertScriptExecutable("char c = 65535;");
        assertScriptUncookable("char c = 65536;");
        assertScriptExecutable("Byte b = -128;");
        assertScriptUncookable("Byte b = 128;");
        assertScriptExecutable("Short s = -32768;");
        assertScriptUncookable("Short s = 32768;");
        assertScriptUncookable("Character c = -1;");
        assertScriptExecutable("Character c = 0;");
        assertScriptExecutable("Character c = 65535;");
        assertScriptUncookable("Character c = 65536;");
    }

    @Test public void
    test_5_5__CastingConversion() throws Exception {
        assertExpressionEvaluatesTrue("7 == (int) 7");
        assertExpressionEvaluatesTrue("(int) 'a' == 97");
        assertExpressionEvaluatesTrue("(int) 10000000000L == 1410065408");
        assertExpressionEvaluatesTrue("((Object) \"SS\").equals(\"SS\")");
        assertScriptReturnsTrue("Object o = \"SS\"; return ((String) o).length() == 2;");
        assertExpressionEvaluatesTrue("((Integer) 7).intValue() == 7");
        assertExpressionEvaluatesTrue("(int) new Integer(7) == 7");

        // Boxing conversion followed by widening reference conversion - not described in JLS7, but supported by
        // JAVAC. See JLS7 5.1.7, and JANINO-153.
        assertExpressionEvaluatesTrue("null != (Comparable) 5.0");

        // Unboxing conversion followed by widening primitive conversion - not described in JLS7, but supported by
        // JAVAC. See JLS7 5.1.7, and JANINO-153.
        assertExpressionEvaluatesTrue("0L != (long) new Integer(8)");
    }

    @Test public void
    test_5_6__NumberPromotions() throws Exception {
        // 5.6.1 Unary Numeric Promotion
        assertExpressionEvaluatesTrue("-new Byte((byte) 7) == -7");
        assertExpressionEvaluatesTrue("-new Double(10.0D) == -10.0D");
        assertScriptReturnsTrue("char c = 'a'; return -c == -97;");

        // 5.6.2 Binary Numeric Promotion
        assertExpressionEvaluatesTrue("2.5D * new Integer(4) == 10D");
        assertExpressionEvaluatesTrue("7 % new Float(2.5F) == 2F");
        assertExpressionEvaluatesTrue("2000000000 + 2000000000L == 4000000000L");
        assertExpressionEvaluatesTrue("(short) 32767 + (byte) 100 == 32867");
    }

    @Test public void
    test_7_5__ImportDeclarations() throws Exception {
        // Default imports
        assertExpressionEvaluatesTrue(
            "import java.util.*; new ArrayList().getClass().getName().equals(\"java.util.ArrayList\")"
        );
        assertScriptUncookable("import java.util#;");
        assertScriptUncookable("import java.util.9;");
        assertScriptCookable("import java.util.*;");
        assertClassBodyMainReturnsTrue(
            "import java.util.*; public static boolean main() { return new ArrayList() instanceof List; }"
        );
        assertExpressionUncookable("import java.io.*; new ArrayList()");

        // 7.5.1 Import Declarations -- Single-Type-Import
        assertExpressionEvaluatable("import java.util.ArrayList; new ArrayList()");
        assertExpressionEvaluatable("import java.util.ArrayList; import java.util.ArrayList; new ArrayList()");
        assertScriptUncookable("import java.util.List; import java.awt.List;");

        // 7.5.2 Import Declarations -- Import-on-Demand
        assertExpressionEvaluatable("import java.util.*; new ArrayList()");
        assertExpressionEvaluatable("import java.util.*; import java.util.*; new ArrayList()");

        // 7.5.3 Import Declarations -- Single Static Import
        assertExpressionEvaluatesTrue(
            "import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set"
        );
        assertExpressionEvaluatesTrue(
            "import static java.util.Collections.EMPTY_SET;"
            + "import static java.util.Collections.EMPTY_SET;"
            + "EMPTY_SET instanceof java.util.Set"
        );
        assertScriptExecutable("import static java.util.Map.Entry; Entry e;");
        assertScriptExecutable(
            "import static java.util.Map.Entry;"
            + "import static java.util.Map.Entry;"
            + "Entry e;"
        );
        assertScriptUncookable(
            "import static java.util.Map.Entry;"
            + "import static java.security.KeyStore.Entry;"
            + "Entry e;"
        );
        assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
        assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.asList;"
            + "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
        assertScriptUncookable(
            "import static java.lang.Integer.decode;"
            + "import static java.lang.Long.decode;"
            + "decode(\"4000000000\");"
        );

        // 7.5.4 Import Declarations -- Static-Import-on-Demand
        assertExpressionEvaluatesTrue("import static java.util.Collections.*; EMPTY_SET instanceof java.util.Set");
        assertScriptExecutable("import static java.util.Map.*; Entry e;");
        assertExpressionEvaluatesTrue(
            "import static java.util.Arrays.*;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        );
    }
    
    @Test public void
    test_9_7__Annotations() throws Exception {
        assertClassBodyCookable("class C { @Override public String toString() { return \"foo!\"; } }");
        assertClassBodyCookable("class C {           public String toString() { return \"foo!\"; } }");
        assertClassBodyUncookable("class C { @Override public String meth1()    { return \"foo!\"; } }");
        assertClassBodyCookable("class C {           public String meth1()    { return \"foo!\"; } }");

        assertClassBodyCookable("interface I extends Runnable { @Override void run(); }");
        assertClassBodyCookable("interface I extends Runnable {           void run(); }");
    }

    @Test public void
    test_14_3__LocalClassDeclarations() throws Exception {
        assertScriptReturnsTrue(
            "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;"
        );
    }

    @Test public void
    test_14_8__ExpressionStatements() throws Exception {
        assertScriptReturnsTrue("int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        assertScriptExecutable("System.currentTimeMillis();");
        assertScriptExecutable("new Object();");
        assertScriptUncookable("new Object[3];");
        assertScriptUncookable("int a; a;");
    }
    
    @Test public void
    test_14_10__TheAssertStatement() throws Exception {
        // CHECKSTYLE LineLength:OFF
        assertScriptExecutable("assert true;");
        assertScriptReturnsTrue("try { assert false;                  } catch (AssertionError ae) { return ae.getMessage() == null;       } return false;");
        assertScriptReturnsTrue("try { assert false : \"x\";          } catch (AssertionError ae) { return \"x\".equals(ae.getMessage()); } return false;");
        assertScriptReturnsTrue("try { assert false : 3;              } catch (AssertionError ae) { return \"3\".equals(ae.getMessage()); } return false;");
        assertScriptReturnsTrue("try { assert false : new Integer(8); } catch (AssertionError ae) { return \"8\".equals(ae.getMessage()); } return false;");
        // CHECKSTYLE LineLength:ON
    }

    @Test public void
    test_14_11__TheSwitchStatement() throws Exception {
        assertScriptReturnsTrue("int x = 37; switch (x) {} return x == 37;");
        assertScriptReturnsTrue("int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        assertScriptReturnsTrue("int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;");
        assertScriptReturnsTrue(
            "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;"
        );
        assertScriptReturnsTrue("int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;");
        assertScriptReturnsTrue(
            "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;"
        );
    }

    @Test public void
    test_14_20_1__ExecutionOfTryCatch__1() throws Exception {
        assertClassBodyMainReturnsTrue(
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
        assertClassBodyUncookable(
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
        assertClassBodyMainReturnsTrue(
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
        assertClassBodyUncookable(
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
        assertClassBodyMainReturnsTrue(
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
        assertCompilationUnitCookable(
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
        assertClassBodyUncookable(
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

        assertClassBodyCookable(
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
        assertExpressionEvaluatesTrue("new Object() instanceof Object");
        assertExpressionUncookable("new java.util.List()");
        assertExpressionUncookable("new other_package.PackageClass()");
        assertExpressionUncookable("new java.util.AbstractList()");
        assertExpressionEvaluatesTrue(
            "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass"
        );
        assertExpressionUncookable("new other_package.Foo(3).new Foo.PublicMemberClass()");
        assertExpressionUncookable("new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        assertExpressionUncookable("new other_package.Foo(3).new PackageMemberClass()");
        assertExpressionUncookable("new other_package.Foo(3).new PublicAbstractMemberClass()");
        assertExpressionUncookable("new other_package.Foo(3).new PublicStaticMemberClass()");
        assertExpressionUncookable("new other_package.Foo(3).new PublicMemberInterface()");
        assertExpressionUncookable("new java.util.ArrayList().new PublicMemberClass()");

        // The following one is tricky: A Java 6 JRE declares
        //    public int          File.compareTo(File)
        //    public abstract int Comparable.compareTo(Object)
        // , and yet "File" is not abstract!
        assertCompilationUnitMainReturnsTrue((
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
        assertExpressionEvaluatable("new Integer(3)");
        assertExpressionEvaluatable("new Integer(new Integer(3))");
        assertExpressionEvaluatable("new Integer(new Byte((byte) 3))");
        assertExpressionUncookable("new Integer(new Object())");

        // 15.9.5 Anonymous Class Declarations
        assertCompilationUnitMainExecutable((
            ""
            + "public class Foo {\n"
            + "    public static void main() { new Foo().meth(); }\n"
            + "    private Object meth() {\n"
            + "        return new Object() {};\n"
            + "    }\n"
            + "}\n"
        ), "Foo");
        assertCompilationUnitMainExecutable((
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
        assertCompilationUnitMainReturnsTrue((
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
        assertCompilationUnitUncookable(
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
        assertCompilationUnitMainReturnsTrue((
            ""
            + "public class Main        { public static boolean main()  { return new B().meth(\"x\"); } }\n"
            + "public class A           { public boolean meth(String s) { return true; } }\n"
            + "public class B extends A { public boolean meth(Object o) { return false; } }\n"
        ), "Main");
    }
    
    @Test public void
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__1() throws Exception {
        assertExpressionEvaluatesTrue("\"two one\".equals(String.format(\"%2$s %1$s\", \"one\", \"two\"))");
    }

    @Test public void
    test_15_12_2_4__Phase3IdentifyApplicableVariableArityMethods__2() throws Exception {
        assertClassBodyMainReturnsTrue(
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
        assertClassBodyMainReturnsTrue(
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
        assertScriptReturnsTrue(
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
        assertClassBodyMainReturnsTrue(
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
        
        assertClassBodyUncookable(
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
    test_15_14__PostfixExpressions() throws Exception {
        // "15.14.2 Postfix Increment Operator ++
        assertScriptReturnsTrue("int i = 7; i++; return i == 8;");
        assertScriptReturnsTrue("Integer i = new Integer(7); i++; return i.intValue() == 8;");
        assertScriptReturnsTrue("int i = 7; return i == 7 && i++ == 7 && i == 8;");
        assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;"
        );

        // 15.14.3 Postfix Decrement Operator --
        assertScriptReturnsTrue("int i = 7; i--; return i == 6;");
        assertScriptReturnsTrue("Integer i = new Integer(7); i--; return i.intValue() == 6;");
        assertScriptReturnsTrue("int i = 7; return i == 7 && i-- == 7 && i == 6;");
        assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;"
        );
    }

    @Test public void
    test_15_15__UnaryOperators() throws Exception {
        // 15.15.1 Prefix Increment Operator ++
        assertScriptReturnsTrue("int i = 7; ++i; return i == 8;");
        assertScriptReturnsTrue("Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        assertScriptReturnsTrue("int i = 7; return i == 7 && ++i == 8 && i == 8;");
        assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;"
        );

        // 15.15.2 Prefix Decrement Operator --
        assertScriptReturnsTrue("int i = 7; --i; return i == 6;");
        assertScriptReturnsTrue("Integer i = new Integer(7); --i; return i.intValue() == 6;");
        assertScriptReturnsTrue("int i = 7; return i == 7 && --i == 6 && i == 6;");
        assertScriptReturnsTrue(
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;"
        );

        // 15.15.3 Unary Plus Operator +
        assertExpressionEvaluatesTrue("new Integer(+new Integer(7)).intValue() == 7");

        // 15.15.4 Unary Minus Operator -
        assertExpressionEvaluatesTrue("new Integer(-new Integer(7)).intValue() == -7");
    }

    @Test public void
    test_15_17__MultiplicativeOperators() throws Exception {
        assertExpressionEvaluatesTrue("new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");
    }

    @Test public void
    test_15_18__AdditiveOperators() throws Exception {
        // 15.18 Additive Operators -- Numeric
        assertExpressionEvaluatesTrue("(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        // 15.18.1.3 Additive Operators -- String Concatentation
        assertExpressionEvaluatesTrue(
            "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")"
        );
        assertExpressionEvaluatesTrue("1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        assertExpressionEvaluatesTrue("\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");
        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            assertExpressionEvaluatesTrue("\"" + s1 + "\".length() == " + i);
            assertExpressionEvaluatesTrue("(\"" + s1 + "\" + \"XXX\").length() == " + (i + 3));
        }
    }

    @Test public void
    test_15_20__RelationOperators() throws Exception {
        // 15.20.1 Numerical Comparison Operators <, <=, > and >=
        assertExpressionEvaluatesTrue("new Integer(7) > new Byte((byte) 5)");
    }

    @Test public void
    test_15_21__EqualityOperators() throws Exception {

        // 15.21.1 Numerical Equality Operators == and !=
        assertExpressionUncookable("new Integer(7) != new Byte((byte) 5)");
        assertExpressionEvaluatesTrue("new Integer(7) == 7");
        assertExpressionEvaluatesTrue("new Byte((byte) -7) == -7");
        assertExpressionEvaluatesTrue("5 == new Byte((byte) 5)");

        // 15.21.2 Boolean Equality Operators == and !=
        assertExpressionEvaluatesTrue("new Boolean(true) != new Boolean(true)");
        assertExpressionEvaluatesTrue("new Boolean(true) == true");
        assertExpressionEvaluatesTrue("false == new Boolean(false)");
        assertExpressionEvaluatesTrue("false != true");

        // 15.21.3 Reference Equality Operators == and !=
        assertExpressionEvaluatesTrue("new Object() != new Object()");
        assertExpressionEvaluatesTrue("new Object() != null");
        assertExpressionEvaluatesTrue("new Object() != \"foo\"");
        assertExpressionUncookable("new Integer(3) == \"foo\"");
    }

    @Test public void
    test_15_22__BitwiseAndLogicalOperators() throws Exception {
        // 15.22.2 Boolean Logical Operators &, ^, and |
        assertExpressionEvaluatesTrue("new Boolean(true) & new Boolean(true)");
        assertExpressionEvaluatesTrue("new Boolean(true) ^ false");
        assertExpressionEvaluatesTrue("false | new Boolean(true)");
    }

    @Test public void
    test_15_23__ConditionalAndOperator() throws Exception {
        // 15.23 Conditional-And Operator &&
        assertExpressionEvaluatesTrue("new Boolean(true) && new Boolean(true)");
        assertExpressionEvaluatesTrue("new Boolean(true) && true");
        assertExpressionEvaluatesTrue("true && new Boolean(true)");
    }

    @Test public void
    test_15_24__ConditionalOrOperator() throws Exception {
        // 15.24 Conditional-Or Operator ||
        assertExpressionEvaluatesTrue("new Boolean(true) || new Boolean(false)");
        assertExpressionEvaluatesTrue("new Boolean(false) || true");
        assertExpressionEvaluatesTrue("true || new Boolean(true)");
    }

    @Test public void
    test_15_26__AssignmentOperators() throws Exception {
        // 15.26.2 Compound Assignment Operators
        assertScriptReturnsTrue("int a = 7; a += 3; return a == 10;");
        assertScriptReturnsTrue("int a = 7; a %= 3; return a == 1;");
        assertScriptUncookable("Object a = \"foo\"; a += 3;");
        assertScriptUncookable("int a = 7; a += \"foo\";");
        assertScriptReturnsTrue("String a = \"foo\"; a += 3; return a.equals(\"foo3\");");
        assertScriptReturnsTrue("String a = \"foo\"; a += 'o'; return a.equals(\"fooo\");");
        assertScriptReturnsTrue("String a = \"foo\"; a += 1.0; return a.equals(\"foo1.0\");");
        assertScriptReturnsTrue("String[] a = { \"foo\" }; a[0] += 1.0; return a[0].equals(\"foo1.0\");");
        assertScriptReturnsTrue("Integer a = 7; a += 3; return a == 10;");
        assertScriptReturnsTrue("int a = 7; a += new Integer(3); return a == 10;");
        // JANINO-155: Compound assignment does not implement boxing conversion
        assertScriptReturnsTrue("Double[] a = { 1.0, 2.0 }; a[0] += 1.0; return a[0] == 2.0;");
    }
}
