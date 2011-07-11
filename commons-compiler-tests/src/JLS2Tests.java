
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

/**
 * Tests against the \"Java Language Specification\"; 2nd edition
 */
@RunWith(Parameterized.class)
public class JLS2Tests extends JaninoTestSuite {
    @Parameters
    public static List<Object[]> compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }

    public JLS2Tests(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }

    @Test
    public void test_3__LexicalStructure() throws Exception {
        // 3.1. Lexical Structure -- Unicode
        exp(TRUE, "'\\u00e4' == '\u00e4'");

        // 3.2. Lexical Structure -- Translations
        scr(COMP, "3--4");

        // 3.3. Lexical Structure -- Unicode Escapes
        exp(COMP, "aaa\\u123gbbb");
        exp(TRUE, "\"\\u0041\".equals(\"A\")");
        exp(TRUE, "\"\\uu0041\".equals(\"A\")");
        exp(TRUE, "\"\\uuu0041\".equals(\"A\")");
        exp(TRUE, "\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        exp(TRUE, "\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");

        // 3.3. Lexical Structure -- Line Terminators
        exp(TRUE, "1//\r+//\r\n2//\n==//\n\r3");

        // 3.6. Lexical Structure -- White Space
        exp(TRUE, "3\t\r \n==3");

        // 3.7. Lexical Structure -- Comments
        exp(TRUE, "7/* */==7");
        exp(TRUE, "7/**/==7");
        exp(TRUE, "7/***/==7");
        exp(COMP, "7/*/==7");
        exp(TRUE, "7/*\r*/==7");
        exp(TRUE, "7//\r==7");
        exp(TRUE, "7//\n==7");
        exp(TRUE, "7//\r\n==7");
        exp(TRUE, "7//\n\r==7");
        scr(COMP, "7// /*\n\rXXX*/==7");

        // 3.8. Lexical Structure -- Identifiers
        scr(EXEC, "int a;");
        scr(EXEC, "int \u00e4\u00e4\u00e4;");
        scr(EXEC, "int \\u0391;"); // Greek alpha
        scr(EXEC, "int _aaa;");
        scr(EXEC, "int $aaa;");
        scr(COMP, "int 9aaa;");
        scr(COMP, "int const;");
    }

    @Test
    public void test_3_10_1__Literals_Integer() throws Exception {
        exp(TRUE, "17 == 17L");
        exp(TRUE, "255 == 0xFFl");
        exp(TRUE, "17 == 021L");
        exp(COMP, "17 == 029"); // Digit "9" not allowed in octal literal.
        exp(TRUE, "2 * 2147483647 == -2");
        exp(TRUE, "2 * -2147483648 == 0");
        exp(COMP, "2147483648");
        exp(EXEC, "-2147483648");
        exp(TRUE, "-1 == 0xffffffff");
        exp(TRUE, "1 == -0xffffffff");
        exp(TRUE, "-0xf == -15");
        exp(EXEC, "9223372036854775807L");
        exp(COMP, "9223372036854775808L");
        exp(COMP, "9223372036854775809L");
        exp(COMP, "99999999999999999999999999999L");
        exp(EXEC, "-9223372036854775808L");
        exp(COMP, "-9223372036854775809L");
    }

    @Test
    public void test_3_10_2__Literals_FloatingPoint() throws Exception {
        exp(TRUE, "1e1f == 10f");
        exp(TRUE, "1E1F == 10f");
        exp(TRUE, ".3f == 0.3f");
        exp(TRUE, "0f == (float) 0");
        exp(EXEC, "3.14f");
        exp(EXEC, "3.40282347e+38f");
        exp(COMP, "3.40282357e+38f");
        exp(EXEC, "1.40239846e-45f");
        exp(COMP, "7.0e-46f");
        exp(EXEC, "1.79769313486231570e+308D");
        exp(COMP, "1.79769313486231581e+308d");
        exp(EXEC, "4.94065645841246544e-324D");
        exp(COMP, "2e-324D");
    }

    @Test
    public void test_3_10_3__Literals_Boolean() throws Exception {
        exp(TRUE, "true");
        exp(TRUE, "! false");
    }

    @Test
    public void test_3_10_4__Literals_Character() throws Exception {
        exp(TRUE, "'a' == 97");
        exp(COMP, "'''");
        exp(COMP, "'\\'");
        exp(COMP, "'\n'");
        exp(COMP, "'ax'");
        exp(COMP, "'a\n'");
        exp(TRUE, "'\"' == 34"); // Unescaped double quote is allowed!
    }

    @Test
    public void test_3_10_5__Literals_String() throws Exception {
        exp(TRUE, "\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        exp(TRUE, "\"\\b\".charAt(0) == 8");
        exp(COMP, "\"aaa\nbbb\"");
        exp(COMP, "\"aaa\rbbb\"");
        exp(TRUE, "\"aaa\" == \"aaa\"");
        exp(TRUE, "\"aaa\" != \"bbb\"");
    }

    @Test
    public void test_3_10_6__Literals_EscapeSequences() throws Exception {
        exp(COMP, "'\\u000a'"); // 0x000a is LF
        exp(TRUE, "'\\b' == 8");
        exp(TRUE, "'\\t' == 9");
        exp(TRUE, "'\\n' == 10");
        exp(TRUE, "'\\f' == 12");
        exp(TRUE, "'\\r' == 13");
        exp(TRUE, "'\\\"' == 34");
        exp(TRUE, "'\\'' == 39");
        exp(TRUE, "'\\\\' == 92");
        exp(TRUE, "'\\0' == 0");
        exp(TRUE, "'\\07' == 7");
        exp(TRUE, "'\\377' == 255");
        exp(COMP, "'\\400'");
        exp(COMP, "'\\1234'");
    }

    @Test
    public void test_3_10_7__Literals_Null() throws Exception {
        exp(EXEC, "null");
    }

    @Test
    public void test_3_11__Separators() throws Exception {
        scr(EXEC, ";");
    }

    @Test
    public void test_3_12__Operators() throws Exception {
        scr(TRUE, "int a = -11; a >>>= 2; return a == 1073741821;");
    }

    @Test
    public void test_5_1_7__Conversion_Boxing() throws Exception {
        scr(TRUE, "Boolean   b = true;        return b.booleanValue();");
        scr(TRUE, "Boolean   b = false;       return !b.booleanValue();");
        scr(TRUE, "Byte      b = (byte) 7;    return b.equals(new Byte((byte) 7));");
        scr(TRUE, "Character c = 'X';         return c.equals(new Character('X'));");
        scr(TRUE, "Short     s = (short) 322; return s.equals(new Short((short) 322));");
        scr(TRUE, "Integer   i = 99;          return i.equals(new Integer(99));");
        scr(TRUE, "Long      l = 733L;        return l.equals(new Long(733L));");
        scr(TRUE, "Float     f = 12.5F;       return f.equals(new Float(12.5F));");
        scr(TRUE, "Double    d = 14.3D;       return d.equals(new Double(14.3D));");
    }

    @Test
    public void test_5_1_8__Conversion_UnBoxing() throws Exception {
        exp(TRUE, "Boolean.TRUE");
        exp(TRUE, "!Boolean.FALSE");
        exp(TRUE, "new Byte((byte) 9) == (byte) 9");
        exp(TRUE, "new Character('Y') == 'Y'");
        exp(TRUE, "new Short((short) 33) == (short) 33");
        exp(TRUE, "new Integer(-444) == -444");
        exp(TRUE, "new Long(987654321L) == 987654321L");
        exp(TRUE, "new Float(33.3F) == 33.3F");
        exp(TRUE, "new Double(939.939D) == 939.939D");
    }

    @Test
    public void test_5_2__Conversion_Assignment() throws Exception {
        scr(TRUE, "int i = 7; return i == 7;");
        scr(TRUE, "String s = \"S\"; return s.equals(\"S\");");
        scr(TRUE, "long l = 7; return l == 7L;");
        scr(TRUE, "Object o = \"A\"; return o.equals(\"A\");");
        scr(TRUE, "Integer i = 7; return i.intValue() == 7;");
        scr(TRUE, "Object o = 7; return o.equals(new Integer(7));");
        scr(TRUE, "int i = new Integer(7); return i == 7;");
        scr(TRUE, "long l = new Integer(7); return l == 7L;");
        scr(EXEC, "byte b = -128;");
        scr(COMP, "byte b = 128;");
        scr(EXEC, "short s = -32768;");
        scr(COMP, "short s = 32768;");
        scr(COMP, "char c = -1;");
        scr(EXEC, "char c = 0;");
        scr(EXEC, "char c = 65535;");
        scr(COMP, "char c = 65536;");
        scr(EXEC, "Byte b = -128;");
        scr(COMP, "Byte b = 128;");
        scr(EXEC, "Short s = -32768;");
        scr(COMP, "Short s = 32768;");
        scr(COMP, "Character c = -1;");
        scr(EXEC, "Character c = 0;");
        scr(EXEC, "Character c = 65535;");
        scr(COMP, "Character c = 65536;");
    }

    @Test
    public void test_5_5__Conversion_Casting() throws Exception {
        exp(TRUE, "7 == (int) 7");
        exp(TRUE, "(int) 'a' == 97");
        exp(TRUE, "(int) 10000000000L == 1410065408");
        exp(TRUE, "((Object) \"SS\").equals(\"SS\")");
        scr(TRUE, "Object o = \"SS\"; return ((String) o).length() == 2;");
        exp(TRUE, "((Integer) 7).intValue() == 7");
        exp(TRUE, "(int) new Integer(7) == 7");

        // Boxing conversion followed by widening reference conversion - not described in the JLS3, but supported
        // by JAVAC. See JLS3 5.5, and JANINO-153.
        exp(TRUE, "null != (Comparable) 5.0");

        // Unboxing conversion followed by widening primitive conversion - not described in the JLS3, but supported
        // by JAVAC. See JLS3 5.5, and JANINO-153.
        exp(TRUE, "0L != (long) new Integer(8)");
    }

    @Test
    public void test_5_6__Conversion_NumberPromotions() throws Exception {
        // 5.6.1 Unary Numeric Promotion
        exp(TRUE, "-new Byte((byte) 7) == -7");
        exp(TRUE, "-new Double(10.0D) == -10.0D");
        scr(TRUE, "char c = 'a'; return -c == -97;");

        // 5.6.2 Binary Numeric Promotion
        exp(TRUE, "2.5D * new Integer(4) == 10D");
        exp(TRUE, "7 % new Float(2.5F) == 2F");
        exp(TRUE, "2000000000 + 2000000000L == 4000000000L");
        exp(TRUE, "(short) 32767 + (byte) 100 == 32867");
    }

    @Test
    public void test_7_5__ImportDeclarations() throws Exception {
        // Default imports
        exp(
            TRUE,
            "new ArrayList().getClass().getName().equals(\"java.util.ArrayList\")",
            new String[] { "java.util.*" }
        );
        scr(COMP, "xxx", new String[] { "java.util#" });
        scr(COMP, "xxx", new String[] { "java.util.9" });
        scr(COMP, "xxx", new String[] { "java.util.*;" });
        clb(
            TRUE,
            "public static boolean main() { return new ArrayList() instanceof List; }",
            new String[] { "java.util.*" }
        );
        exp(COMP, "new ArrayList()", new String[] { "java.io.*" });

        // 7.5.1 Import Declarations -- Single-Type-Import
        exp(EXEC, "import java.util.ArrayList; new ArrayList()");
        exp(EXEC, "import java.util.ArrayList; import java.util.ArrayList; new ArrayList()");
        scr(COMP, "import java.util.List; import java.awt.List;");

        // 7.5.2 Import Declarations -- Import-on-Demand
        exp(EXEC, "import java.util.*; new ArrayList()");
        exp(EXEC, "import java.util.*; import java.util.*; new ArrayList()");

        // 7.5.3 Import Declarations -- Single Static Import
        exp(TRUE, "import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set");
        exp(TRUE, (
            "import static java.util.Collections.EMPTY_SET;"
            + "import static java.util.Collections.EMPTY_SET;"
            + "EMPTY_SET instanceof java.util.Set"
        ));
        scr(EXEC, "import static java.util.Map.Entry; Entry e;");
        scr(EXEC, (
            "import static java.util.Map.Entry;"
            + "import static java.util.Map.Entry;"
            + "Entry e;"
        ));
        scr(COMP, (
            "import static java.util.Map.Entry;"
            + "import static java.security.KeyStore.Entry;"
            + "Entry e;"
        ));
        exp(TRUE, (
            "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
        exp(TRUE, (
            "import static java.util.Arrays.asList;"
            + "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
        scr(COMP, (
            "import static java.lang.Integer.decode;"
            + "import static java.lang.Long.decode;"
            + "decode(\"4000000000\");"
        ));

        // 7.5.4 Import Declarations -- Static-Import-on-Demand
        exp(TRUE, "import static java.util.Collections.*; EMPTY_SET instanceof java.util.Set");
        scr(EXEC, "import static java.util.Map.*; Entry e;");
        exp(TRUE, (
            "import static java.util.Arrays.*;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
    }

    @Test
    public void test_14_3__LocalClassDeclarations() throws Exception {
        scr(TRUE, "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;");
    }

    @Test
    public void test_14_8__ExpressionStatements() throws Exception {
        scr(TRUE, "int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        scr(EXEC, "System.currentTimeMillis();");
        scr(EXEC, "new Object();");
        scr(COMP, "new Object[3];");
        scr(COMP, "int a; a;");
    }

    @Test
    public void test_14_10__TheSwitchStatement() throws Exception {
        scr(TRUE, "int x = 37; switch (x) {} return x == 37;");
        scr(TRUE, "int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        scr(TRUE, "int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;");
        scr(TRUE, "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;");
        scr(TRUE, "int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;");
        scr(TRUE, "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;");
    }

    @Test
    public void test_14_20__UnreachableStatements() throws Exception {
        clb(COMP, (
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
        ));
    }

    @Test
    public void test_15_9__Expressions__ClassInstanceCreationExpressions() throws Exception {
        // 15.9.1 Determining the class being Instantiated
        exp(TRUE, "new Object() instanceof Object");
        exp(COMP, "new java.util.List()");
        exp(COMP, "new other_package.PackageClass()");
        exp(COMP, "new java.util.AbstractList()");
        exp(TRUE, (
            "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass"
        ));
        exp(COMP, "new other_package.Foo(3).new Foo.PublicMemberClass()");
        exp(COMP, "new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        exp(COMP, "new other_package.Foo(3).new PackageMemberClass()");
        exp(COMP, "new other_package.Foo(3).new PublicAbstractMemberClass()");
        exp(COMP, "new other_package.Foo(3).new PublicStaticMemberClass()");
        exp(COMP, "new other_package.Foo(3).new PublicMemberInterface()");
        exp(COMP, "new java.util.ArrayList().new PublicMemberClass()");

        // The following one is tricky: A Java 6 JRE declares
        //    public int          File.compareTo(File)
        //    public abstract int Comparable.compareTo(Object)
        // , and yet "File" is not abstract!
        sim(TRUE, (
            "class MyFile extends java.io.File {\n"
            + "    public MyFile() { super(\"/my/file\"); }\n"
            + "}\n"
            + "public class Main {\n"
            + "    public static boolean test() {\n"
            + "        return 0 == new MyFile().compareTo(new MyFile());\n"
            + "    }\n"
            + "}"
        ), "Main");

        // 15.9.3 Choosing the Constructor and its Arguments
        exp(EXEC, "new Integer(3)");
        exp(EXEC, "new Integer(new Integer(3))");
        exp(EXEC, "new Integer(new Byte((byte) 3))");
        exp(COMP, "new Integer(new Object())");

        // 15.9.5 Anonymous Class Declarations
        sim(EXEC, (
            ""
            + "public class Foo {\n"
            + "    public static void test() { new Foo().meth(); }\n"
            + "    private Object meth() {\n"
            + "        return new Object() {};\n"
            + "    }\n"
            + "}\n"
        ), "Foo");
        sim(EXEC, (
            ""
            + "public class A {\n"
            + "    public static void test() { new A(); }\n"
            + "    public A(Object o) {}\n"
            + "    public A() {\n"
            + "        this(new Object() {});\n"
            + "    }\n"
            + "}\n"
        ), "A");
    }

    @Test
    public void test_15_11__FieldAccessExpressions() throws Exception {
        // 15.11.2 Accessing Superclass Members using super
        sim(TRUE, (
            ""
            + "public class T1            { int x = 1; }\n"
            + "public class T2 extends T1 { int x = 2; }\n"
            + "public class T3 extends T2 {\n"
            + "    int x = 3;\n"
            + "    public static boolean test() {\n"
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

    @Test
    public void test_15_12__MethodInvocationExpressions() throws Exception {
        // 15.12.2.2 Choose the Most Specific Method
        sim(COMP, (
            ""
            + "public class Main { public static boolean test() { return new A().meth(\"x\", \"y\"); } }\n"
            + "public class A {\n"
            + "    public boolean meth(String s, Object o) { return true; }\n"
            + "    public boolean meth(Object o, String s) { return false; }\n"
            + "}\n"
        ), "Main");

        // The following case is tricky: JLS2 says that the invocation is AMBIGUOUS, but only
        // JAVAC 1.2 issues an error; JAVAC 1.4.1, 1.5.0 and 1.6.0 obviously ignore the declaring
        // type and invoke "A.meth(String)".
        // JLS3 is not clear about this. For compatibility with JAVA 1.4.1, 1.5.0 and 1.6.0,
        // JANINO also ignores the declaring type.
        //
        // See also JANINO-79 and "IClass.IInvocable.isMoreSpecificThan()".
        sim(TRUE, (
            ""
            + "public class Main        { public static boolean test()  { return new B().meth(\"x\"); } }\n"
            + "public class A           { public boolean meth(String s) { return true; } }\n"
            + "public class B extends A { public boolean meth(Object o) { return false; } }\n"
        ), "Main");
    }

    @Test
    public void test_15_14__PostfixExpressions() throws Exception {
        // "15.14.2 Postfix Increment Operator ++
        scr(TRUE, "int i = 7; i++; return i == 8;");
        scr(TRUE, "Integer i = new Integer(7); i++; return i.intValue() == 8;");
        scr(TRUE, "int i = 7; return i == 7 && i++ == 7 && i == 8;");
        scr(TRUE, (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;"
        ));

        // 15.14.3 Postfix Decrement Operator --
        scr(TRUE, "int i = 7; i--; return i == 6;");
        scr(TRUE, "Integer i = new Integer(7); i--; return i.intValue() == 6;");
        scr(TRUE, "int i = 7; return i == 7 && i-- == 7 && i == 6;");
        scr(TRUE, (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;"
        ));
    }

    @Test
    public void test_15_15__UnaryOperators() throws Exception {
        // 15.15.1 Prefix Increment Operator ++
        scr(TRUE, "int i = 7; ++i; return i == 8;");
        scr(TRUE, "Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        scr(TRUE, "int i = 7; return i == 7 && ++i == 8 && i == 8;");
        scr(TRUE, (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;"
        ));

        // 15.15.2 Prefix Decrement Operator --
        scr(TRUE, "int i = 7; --i; return i == 6;");
        scr(TRUE, "Integer i = new Integer(7); --i; return i.intValue() == 6;");
        scr(TRUE, "int i = 7; return i == 7 && --i == 6 && i == 6;");
        scr(TRUE, (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;"
        ));

        // 15.15.3 Unary Plus Operator +
        exp(TRUE, "new Integer(+new Integer(7)).intValue() == 7");

        // 15.15.4 Unary Minus Operator -
        exp(TRUE, "new Integer(-new Integer(7)).intValue() == -7");
    }

    @Test
    public void test_15_17__MultiplicativeOperators() throws Exception {
        exp(TRUE, "new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");
    }

    @Test
    public void test_15_18__AdditiveOperators() throws Exception {
        // 15.18 Additive Operators -- Numeric
        exp(TRUE, "(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        // 15.18.1.3 Additive Operators -- String Concatentation
        exp(TRUE, (
            "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")"
        ));
        exp(TRUE, "1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        exp(TRUE, "\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");
        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            exp(TRUE, "\"" + s1 + "\".length() == " + i);
            exp(TRUE, "(\"" + s1 + "\" + \"XXX\").length() == " + (i + 3));
        }
    }

    @Test
    public void test_15_20__RelationOperators() throws Exception {
        // 15.20.1 Numerical Comparison Operators <, <=, > and >=
        exp(TRUE, "new Integer(7) > new Byte((byte) 5)");
    }

    @Test
    public void test_15_21__EqualityOperators() throws Exception {

        // 15.21.1 Numerical Equality Operators == and !=
        exp(COMP, "new Integer(7) != new Byte((byte) 5)");
        exp(TRUE, "new Integer(7) == 7");
        exp(TRUE, "new Byte((byte) -7) == -7");
        exp(TRUE, "5 == new Byte((byte) 5)");

        // 15.21.2 Boolean Equality Operators == and !=
        exp(TRUE, "new Boolean(true) != new Boolean(true)");
        exp(TRUE, "new Boolean(true) == true");
        exp(TRUE, "false == new Boolean(false)");
        exp(TRUE, "false != true");

        // 15.21.3 Reference Equality Operators == and !=
        exp(TRUE, "new Object() != new Object()");
        exp(TRUE, "new Object() != null");
        exp(TRUE, "new Object() != \"foo\"");
        exp(COMP, "new Integer(3) == \"foo\"");
    }

    @Test
    public void test_15_22__BitwiseAndLogicalOperators() throws Exception {
        // 15.22.2 Boolean Logical Operators &, ^, and |
        exp(TRUE, "new Boolean(true) & new Boolean(true)");
        exp(TRUE, "new Boolean(true) ^ false");
        exp(TRUE, "false | new Boolean(true)");
    }

    @Test
    public void test_15_23__ConditionalAndOperator() throws Exception {
        // 15.23 Conditional-And Operator &&
        exp(TRUE, "new Boolean(true) && new Boolean(true)");
        exp(TRUE, "new Boolean(true) && true");
        exp(TRUE, "true && new Boolean(true)");
    }

    @Test
    public void test_15_24__ConditionalOrOperator() throws Exception {
        // 15.24 Conditional-Or Operator ||
        exp(TRUE, "new Boolean(true) || new Boolean(false)");
        exp(TRUE, "new Boolean(false) || true");
        exp(TRUE, "true || new Boolean(true)");
    }

    @Test
    public void test_15_26__ConditionalOrOperator() throws Exception {
        // 15.26.2 Compound Assignment Operators
        scr(TRUE, "int a = 7; a += 3; return a == 10;");
        scr(TRUE, "int a = 7; a %= 3; return a == 1;");
        scr(COMP, "Object a = \"foo\"; a += 3;");
        scr(COMP, "int a = 7; a += \"foo\";");
        scr(TRUE, "String a = \"foo\"; a += 3; return a.equals(\"foo3\");");
        scr(TRUE, "String a = \"foo\"; a += 'o'; return a.equals(\"fooo\");");
        scr(TRUE, "String a = \"foo\"; a += 1.0; return a.equals(\"foo1.0\");");
        scr(TRUE, "String[] a = { \"foo\" }; a[0] += 1.0; return a[0].equals(\"foo1.0\");");
        scr(TRUE, "Integer a = 7; a += 3; return a == 10;");
        scr(TRUE, "int a = 7; a += new Integer(3); return a == 10;");
        // JANINO-155: Compound assignment does not implement boxing conversion
        scr(TRUE, "Double[] a = { 1.0, 2.0 }; a[0] += 1.0; return a[0] == 2.0;");
    }
}
