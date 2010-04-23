
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
import java.util.Collection;

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
    public static Collection<Object[]> compilerFactories() throws Exception {
        return TestUtil.getCompilerFactoriesForParameters();
    }
    
    public JLS2Tests(ICompilerFactory compilerFactory) throws Exception {
        super(compilerFactory);
    }
    
    @Test
    public void test_3__LexicalStructure() throws Exception {
        // 3.1. Lexical Structure -- Unicode
        exp(TRUE, "1", "'\\u00e4' == '\u00e4'");
        
        // 3.2. Lexical Structure -- Translations
        scr(COMP, "1", "3--4");
        
        // 3.3. Lexical Structure -- Unicode Escapes
        exp(COMP, "1", "aaa\\u123gbbb");
        exp(TRUE, "2", "\"\\u0041\".equals(\"A\")");
        exp(TRUE, "3", "\"\\uu0041\".equals(\"A\")");
        exp(TRUE, "4", "\"\\uuu0041\".equals(\"A\")");
        exp(TRUE, "5", "\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        exp(TRUE, "6", "\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");
        
        // 3.3. Lexical Structure -- Line Terminators
        exp(TRUE, "1", "1//\r+//\r\n2//\n==//\n\r3");
        
        // 3.6. Lexical Structure -- White Space
        exp(TRUE, "1", "3\t\r \n==3");
        
        // 3.7. Lexical Structure -- Comments
        exp(TRUE, "1", "7/* */==7");
        exp(TRUE, "2", "7/**/==7");
        exp(TRUE, "3", "7/***/==7");
        exp(COMP, "4", "7/*/==7");
        exp(TRUE, "5", "7/*\r*/==7");
        exp(TRUE, "6", "7//\r==7");
        exp(TRUE, "7", "7//\n==7");
        exp(TRUE, "8", "7//\r\n==7");
        exp(TRUE, "9", "7//\n\r==7");
        scr(COMP, "10", "7// /*\n\rXXX*/==7");
        
        // 3.8. Lexical Structure -- Identifiers
        scr(EXEC, "1", "int a;");
        scr(EXEC, "2", "int \u00e4\u00e4\u00e4;");
        scr(EXEC, "3", "int \\u0391;"); // Greek alpha
        scr(EXEC, "4", "int _aaa;");
        scr(EXEC, "5", "int $aaa;");
        scr(COMP, "6", "int 9aaa;");
        scr(COMP, "7", "int const;");
    }

    @Test
    public void test_3_10_1__Literals_Integer() throws Exception {
        exp(TRUE, "1", "17 == 17L");
        exp(TRUE, "2", "255 == 0xFFl");
        exp(TRUE, "3", "17 == 021L");
        exp(COMP, "4", "17 == 029"); // Digit "9" not allowed in octal literal.
        exp(TRUE, "5", "2 * 2147483647 == -2");
        exp(TRUE, "6", "2 * -2147483648 == 0");
        exp(COMP, "7", "2147483648");
        exp(EXEC, "8", "-2147483648");
        exp(TRUE, "9", "-1 == 0xffffffff");
        exp(TRUE, "10", "1 == -0xffffffff");
        exp(TRUE, "11", "-0xf == -15");
        exp(EXEC, "12", "9223372036854775807L");
        exp(COMP, "13", "9223372036854775808L");
        exp(COMP, "14", "9223372036854775809L");
        exp(COMP, "15", "99999999999999999999999999999L");
        exp(EXEC, "16", "-9223372036854775808L");
        exp(COMP, "17", "-9223372036854775809L");
    }

    @Test
    public void test_3_10_2__Literals_FloatingPoint() throws Exception {
        exp(TRUE, "1", "1e1f == 10f");
        exp(TRUE, "2", "1E1F == 10f");
        exp(TRUE, "3", ".3f == 0.3f");
        exp(TRUE, "4", "0f == (float) 0");
        exp(EXEC, "5", "3.14f");
        exp(EXEC, "float_big",        "3.40282347e+38f");
        exp(COMP, "float_too_big",    "3.40282357e+38f");
        exp(EXEC, "float_small",      "1.40239846e-45f");
        exp(COMP, "float_too_small",  "7.0e-46f");
        exp(EXEC, "double_big",       "1.79769313486231570e+308D");
        exp(COMP, "double_too_big",   "1.79769313486231581e+308d");
        exp(EXEC, "double_small",     "4.94065645841246544e-324D");
        exp(COMP, "double_too_small", "2e-324D");
    }

    @Test
    public void test_3_10_3__Literals_Boolean() throws Exception {
        exp(TRUE, "1", "true");
        exp(TRUE, "1", "! false");
    }

    @Test
    public void test_3_10_4__Literals_Character() throws Exception {
        exp(TRUE, "1", "'a' == 97");
        exp(COMP, "2", "'''");
        exp(COMP, "3", "'\\'");
        exp(COMP, "4", "'\n'");
        exp(COMP, "5", "'ax'");
        exp(COMP, "6", "'a\n'");
        exp(TRUE, "7", "'\"' == 34"); // Unescaped double quote is allowed!
    }

    @Test
    public void test_3_10_5__Literals_String() throws Exception {
        exp(TRUE, "1",  "\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        exp(TRUE, "2", "\"\\b\".charAt(0) == 8");
        exp(COMP, "3", "\"aaa\nbbb\"");
        exp(COMP, "4", "\"aaa\rbbb\"");
        exp(TRUE, "5", "\"aaa\" == \"aaa\"");
        exp(TRUE, "6", "\"aaa\" != \"bbb\"");
    }

    @Test
    public void test_3_10_6__Literals_EscapeSequences() throws Exception {
        exp(COMP, "1",  "'\\u000a'"); // 0x000a is LF
        exp(TRUE, "2",  "'\\b' == 8");
        exp(TRUE, "3",  "'\\t' == 9");
        exp(TRUE, "4",  "'\\n' == 10");
        exp(TRUE, "5",  "'\\f' == 12");
        exp(TRUE, "6",  "'\\r' == 13");
        exp(TRUE, "7",  "'\\\"' == 34");
        exp(TRUE, "8",  "'\\'' == 39");
        exp(TRUE, "9",  "'\\\\' == 92");
        exp(TRUE, "10", "'\\0' == 0");
        exp(TRUE, "11", "'\\07' == 7");
        exp(TRUE, "12", "'\\377' == 255");
        exp(COMP, "13", "'\\400'");
        exp(COMP, "14", "'\\1234'");
    }

    @Test
    public void test_3_10_7__Literals_Null() throws Exception {
        exp(EXEC, "1", "null");
    }

    @Test
    public void test_3_11__Separators() throws Exception {
        scr(EXEC, "1", ";");
    }

    @Test
    public void test_3_12__Operators() throws Exception {
        scr(TRUE, "1", "int a = -11; a >>>= 2; return a == 1073741821;");
    }

    @Test
    public void test_5_1_7__Conversion_Boxing() throws Exception {
        scr(TRUE, "boolean 1", "Boolean   b = true;        return b.booleanValue();");
        scr(TRUE, "boolean 2", "Boolean   b = false;       return !b.booleanValue();");
        scr(TRUE, "byte",      "Byte      b = (byte) 7;    return b.equals(new Byte((byte) 7));");
        scr(TRUE, "char",      "Character c = 'X';         return c.equals(new Character('X'));");
        scr(TRUE, "short",     "Short     s = (short) 322; return s.equals(new Short((short) 322));");
        scr(TRUE, "int",       "Integer   i = 99;          return i.equals(new Integer(99));");
        scr(TRUE, "long",      "Long      l = 733L;        return l.equals(new Long(733L));");
        scr(TRUE, "float",     "Float     f = 12.5F;       return f.equals(new Float(12.5F));");
        scr(TRUE, "double",    "Double    d = 14.3D;       return d.equals(new Double(14.3D));");
    }

    @Test
    public void test_5_1_8__Conversion_UnBoxing() throws Exception {
        exp(TRUE, "boolean 1", "Boolean.TRUE");
        exp(TRUE, "boolean 2", "!Boolean.FALSE");
        exp(TRUE, "byte",      "new Byte((byte) 9) == (byte) 9");
        exp(TRUE, "char",      "new Character('Y') == 'Y'");
        exp(TRUE, "short",     "new Short((short) 33) == (short) 33");
        exp(TRUE, "int",       "new Integer(-444) == -444");
        exp(TRUE, "long",      "new Long(987654321L) == 987654321L");
        exp(TRUE, "float",     "new Float(33.3F) == 33.3F");
        exp(TRUE, "double",    "new Double(939.939D) == 939.939D");
    }

    @Test
    public void test_5_2__Conversion_Assignment() throws Exception {
        scr(TRUE, "Identity 1", "int i = 7; return i == 7;");
        scr(TRUE, "Identity 2", "String s = \"S\"; return s.equals(\"S\");");
        scr(TRUE, "Widening Primitive", "long l = 7; return l == 7L;");
        scr(TRUE, "Widening Reference", "Object o = \"A\"; return o.equals(\"A\");");
        scr(TRUE, "Boxing", "Integer i = 7; return i.intValue() == 7;");
        scr(TRUE, "Boxing; Widening Reference", "Object o = 7; return o.equals(new Integer(7));");
        scr(TRUE, "Unboxing", "int i = new Integer(7); return i == 7;");
        scr(TRUE, "Unboxing; Widening Primitive", "long l = new Integer(7); return l == 7L;");
        scr(EXEC, "Constant Assignment byte 1", "byte b = -128;");
        scr(COMP, "Constant Assignment byte 2", "byte b = 128;");
        scr(EXEC, "Constant Assignment short 1", "short s = -32768;");
        scr(COMP, "Constant Assignment short 2", "short s = 32768;");
        scr(COMP, "Constant Assignment char 1", "char c = -1;");
        scr(EXEC, "Constant Assignment char 2", "char c = 0;");
        scr(EXEC, "Constant Assignment char 3", "char c = 65535;");
        scr(COMP, "Constant Assignment char 4", "char c = 65536;");
        scr(EXEC, "Constant Assignment Byte 1", "Byte b = -128;");
        scr(COMP, "Constant Assignment Byte 2", "Byte b = 128;");
        scr(EXEC, "Constant Assignment Short 1", "Short s = -32768;");
        scr(COMP, "Constant Assignment Short 2", "Short s = 32768;");
        scr(COMP, "Constant Assignment Character 1", "Character c = -1;");
        scr(EXEC, "Constant Assignment Character 2", "Character c = 0;");
        scr(EXEC, "Constant Assignment Character 3", "Character c = 65535;");
        scr(COMP, "Constant Assignment Character 4", "Character c = 65536;");
    }

    @Test
    public void test_5_5__Conversion_Casting() throws Exception {
        exp(TRUE, "Identity", "7 == (int) 7");
        exp(TRUE, "Widening Primitive", "(int) 'a' == 97");
        exp(TRUE, "Narrowing Primitive", "(int) 10000000000L == 1410065408");
        exp(TRUE, "Widening Reference", "((Object) \"SS\").equals(\"SS\")");
        scr(TRUE, "Narrowing Reference", "Object o = \"SS\"; return ((String) o).length() == 2;");
        exp(TRUE, "Boxing", "((Integer) 7).intValue() == 7");
        exp(TRUE, "Unboxing", "(int) new Integer(7) == 7");
    }

    @Test
    public void test_5_6__Conversion_NumberPromotions() throws Exception {
        // 5.6.1 Unary Numeric Promotion
        exp(TRUE, "Unboxing 1", "-new Byte((byte) 7) == -7");
        exp(TRUE, "Unboxing 2", "-new Double(10.0D) == -10.0D");
        scr(TRUE, "Char", "char c = 'a'; return -c == -97;");

        // 5.6.2 Binary Numeric Promotion
        exp(TRUE, "Unboxing Double", "2.5D * new Integer(4) == 10D");
        exp(TRUE, "float", "7 % new Float(2.5F) == 2F");
        exp(TRUE, "long", "2000000000 + 2000000000L == 4000000000L");
        exp(TRUE, "byte", "(short) 32767 + (byte) 100 == 32867");
    }

    @Test
    public void test_7_5__ImportDeclarations() throws Exception {
        // Default imports
        exp(TRUE, "1", "new ArrayList().getClass().getName().equals(\"java.util.ArrayList\")", new String[] { "java.util.*" });
        scr(COMP, "2", "xxx", new String[] { "java.util#" });
        scr(COMP, "3", "xxx", new String[] { "java.util.9" });
        scr(COMP, "4", "xxx", new String[] { "java.util.*;" });
        clb(TRUE, "5", "public static boolean main() { return new ArrayList() instanceof List; }", new String[] { "java.util.*" });
        exp(COMP, "6", "new ArrayList()", new String[] { "java.io.*" });
        
        // 7.5.1 Import Declarations -- Single-Type-Import
        exp(EXEC, "1", "import java.util.ArrayList; new ArrayList()");
        exp(EXEC, "Duplicate", "import java.util.ArrayList; import java.util.ArrayList; new ArrayList()");
        scr(COMP, "Inconsistent", "import java.util.List; import java.awt.List;");

        // 7.5.2 Import Declarations -- Import-on-Demand
        exp(EXEC, "1", "import java.util.*; new ArrayList()");
        exp(EXEC, "Duplicate", "import java.util.*; import java.util.*; new ArrayList()");

        // 7.5.3 Import Declarations -- Single Static Import
        exp(TRUE, "Static field", "import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set");
        exp(TRUE, "Static field/duplicate", (
            "import static java.util.Collections.EMPTY_SET;"
            + "import static java.util.Collections.EMPTY_SET;"
            + "EMPTY_SET instanceof java.util.Set"
        ));
        scr(EXEC, "Member type", "import static java.util.Map.Entry; Entry e;");
        scr(EXEC, "Member type/duplicate", (
            "import static java.util.Map.Entry;"
            + "import static java.util.Map.Entry;"
            + "Entry e;"
        ));
        scr(COMP, "Member type/inconsistent", (
            "import static java.util.Map.Entry;"
            + "import static java.security.KeyStore.Entry;"
            + "Entry e;"
        ));
        exp(TRUE, "Static method", (
            "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
        exp(TRUE, "Static method/duplicate", (
            "import static java.util.Arrays.asList;"
            + "import static java.util.Arrays.asList;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
        scr(COMP, "Static method/inconsistent", (
            "import static java.lang.Integer.decode;"
            + "import static java.lang.Long.decode;"
            + "decode(\"4000000000\");"
        ));

        // 7.5.4 Import Declarations -- Static-Import-on-Demand
        exp(TRUE, "Static field", "import static java.util.Collections.*; EMPTY_SET instanceof java.util.Set");
        scr(EXEC, "Member type", "import static java.util.Map.*; Entry e;");
        exp(TRUE, "Static method", (
            "import static java.util.Arrays.*;"
            + "asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2"
        ));
    }

    @Test
    public void test_14__BlocksAndStatements() throws Exception {
        // 14.3 Blocks and Statements -- Local Class Declarations
        scr(TRUE, "1", "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;");

        // 14.8 Blocks and Statements -- Expression Statements
        scr(TRUE, "1", "int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        scr(EXEC, "2", "System.currentTimeMillis();");
        scr(EXEC, "3", "new Object();");
        scr(COMP, "4", "new Object[3];");
        scr(COMP, "5", "int a; a;");

        // 14.10 Blocks and Statements -- The switch Statement
        scr(TRUE, "1", "int x = 37; switch (x) {} return x == 37;");
        scr(TRUE, "2", "int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        scr(TRUE, "3", "int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;");
        scr(TRUE, "4", "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;");
        scr(TRUE, "5", "int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;");
        scr(TRUE, "6", "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;");
    }

    @Test
    public void test_15_9__Expressions__ClassInstanceCreationExpressions() throws Exception {
        // 15.9.1 Determining the class being Instantiated
        exp(TRUE, "3a", "new Object() instanceof Object");
        exp(COMP, "3b", "new java.util.List()");
        exp(COMP, "3c", "new other_package.PackageClass()");
        exp(COMP, "3d", "new java.util.AbstractList()");
        exp(TRUE, "4a", (
            "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass"
        ));
        exp(COMP, "4b", "new other_package.Foo(3).new Foo.PublicMemberClass()");
        exp(COMP, "4c", "new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        exp(COMP, "4d", "new other_package.Foo(3).new PackageMemberClass()");
        exp(COMP, "4e", "new other_package.Foo(3).new PublicAbstractMemberClass()");
        exp(COMP, "4f", "new other_package.Foo(3).new PublicStaticMemberClass()");
        exp(COMP, "4g", "new other_package.Foo(3).new PublicMemberInterface()");
        exp(COMP, "4h", "new java.util.ArrayList().new PublicMemberClass()");
        
        // The following one is tricky: A Java 6 JRE declares
        //    public int          File.compareTo(File)
        //    public abstract int Comparable.compareTo(Object)
        // , and yet "File" is not abstract!
        sim(TRUE, "5", (
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
        exp(EXEC, "1", "new Integer(3)");
        exp(EXEC, "2", "new Integer(new Integer(3))");
        exp(EXEC, "3", "new Integer(new Byte((byte) 3))");
        exp(COMP, "4", "new Integer(new Object())");

        // 15.9.5 Anonymous Class Declarations
        sim(EXEC, "Static anonymous class", (
            "public class Foo {\n" +
            "    public static void test() { new Foo().meth(); }\n" +
            "    private Object meth() {\n" +
            "        return new Object() {};\n" +
            "    }\n" +
            "}\n"
        ), "Foo");
        sim(EXEC, "Anonymous class in explicit constructor invocation", (
            "public class A {\n" +
            "    public static void test() { new A(); }\n" +
            "    public A(Object o) {}\n" +
            "    public A() {\n" +
            "        this(new Object() {});\n" +
            "    }\n" +
            "}\n"
        ), "A");
    }

    @Test
    public void test_15_11__FieldAccessExpressions() throws Exception {
        // 15.11.2 Accessing Superclass Members using super
        sim(TRUE, "1", (
            "public class T1            { int x = 1; }\n" +
            "public class T2 extends T1 { int x = 2; }\n" +
            "public class T3 extends T2 {\n" +
            "    int x = 3;\n" +
            "    public static boolean test() {\n" +
            "        return new T3().test2();\n" +
            "    }\n" +
            "    public boolean test2() {\n" +
            "        return (\n" +
            "            x == 3\n" +
            "            && super.x == 2\n" +
            "            && T3.super.x == 2\n" +
            "//            && T2.super.x == 1\n" + // <= Does not work with the SUN JDK 1.6 compiler
            "            && ((T2) this).x == 2\n" +
            "            && ((T1) this).x == 1\n" +
            "        );\n" +
            "    }\n" +
            "}\n"
         ), "T3");
    }

    @Test
    public void test_15_12__MethodInvocationExpressions() throws Exception {
        // 15.12.2.2 Choose the Most Specific Method
        sim(COMP, "Ambiguity 1", (
            "public class Main { public static boolean test() { return new A().meth(\"x\", \"y\"); } }\n" +
            "public class A {\n" +
            "    public boolean meth(String s, Object o) { return true; }\n" +
            "    public boolean meth(Object o, String s) { return false; }\n" +
            "}\n"
        ), "Main");
        
        // The following case is tricky: JLS2 says that the invocation is AMBIGUOUS, but only
        // JAVAC 1.2 issues an error; JAVAC 1.4.1, 1.5.0 and 1.6.0 obviously ignore the declaring
        // type and invoke "A.meth(String)".
        // JLS3 is not clear about this. For compatibility with JAVA 1.4.1, 1.5.0 and 1.6.0,
        // JANINO also ignores the declaring type.
        //
        // See also JANINO-79 and "IClass.IInvocable.isMoreSpecificThan()".
        sim(TRUE, "Ambiguity 2", (
            "public class Main        { public static boolean test()  { return new B().meth(\"x\"); } }\n" +
            "public class A           { public boolean meth(String s) { return true; } }\n" +
            "public class B extends A { public boolean meth(Object o) { return false; } }\n"
        ), "Main");
    }

    @Test
    public void test_15_14__PostfixExpressions() throws Exception {
        // "15.14.2 Postfix Increment Operator ++
        scr(TRUE, "1", "int i = 7; i++; return i == 8;");
        scr(TRUE, "2", "Integer i = new Integer(7); i++; return i.intValue() == 8;");
        scr(TRUE, "3", "int i = 7; return i == 7 && i++ == 7 && i == 8;");
        scr(TRUE, "4", (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;"
        ));

        // 15.14.3 Postfix Decrement Operator --
        scr(TRUE, "1", "int i = 7; i--; return i == 6;");
        scr(TRUE, "2", "Integer i = new Integer(7); i--; return i.intValue() == 6;");
        scr(TRUE, "3", "int i = 7; return i == 7 && i-- == 7 && i == 6;");
        scr(TRUE, "4", (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;"
        ));
    }

    @Test
    public void test_15_15__UnaryOperators() throws Exception {
        // 15.15.1 Prefix Increment Operator ++
        scr(TRUE, "1", "int i = 7; ++i; return i == 8;");
        scr(TRUE, "2", "Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        scr(TRUE, "3", "int i = 7; return i == 7 && ++i == 8 && i == 8;");
        scr(TRUE, "4", (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;"
        ));

        // 15.15.2 Prefix Decrement Operator --
        scr(TRUE, "1", "int i = 7; --i; return i == 6;");
        scr(TRUE, "2", "Integer i = new Integer(7); --i; return i.intValue() == 6;");
        scr(TRUE, "3", "int i = 7; return i == 7 && --i == 6 && i == 6;");
        scr(TRUE, "4", (
            "Integer i = new Integer(7);"
            + "return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;"
        ));

        // 15.15.3 Unary Plus Operator +
        exp(TRUE, "1", "new Integer(+new Integer(7)).intValue() == 7");

        // 15.15.4 Unary Minus Operator -
        exp(TRUE, "1", "new Integer(-new Integer(7)).intValue() == -7");
    }

    @Test
    public void test_15_17__MultiplicativeOperators() throws Exception {
        exp(TRUE, "1", "new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");
    }

    @Test
    public void test_15_18__AdditiveOperators() throws Exception {
        // 15.18 Additive Operators -- Numeric
        exp(TRUE, "1", "(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        // 15.18.1.3 Additive Operators -- String Concatentation
        exp(TRUE, "1", (
            "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")"
        ));
        exp(TRUE, "2", "1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        exp(TRUE, "3", "\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");
        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            exp(TRUE, "4/" + i, "\"" + s1 + "\".length() == " + i);
            exp(TRUE, "5/" + i, "(\"" + s1 + "\" + \"XXX\").length() == " + (i + 3));
        }
    }

    @Test
    public void test_15_20__RelationOperators() throws Exception {
        // 15.20.1 Numerical Comparison Operators <, <=, > and >=
        exp(TRUE, "1", "new Integer(7) > new Byte((byte) 5)");

        // 15.21.1 Numerical Equality Operators == and !=
        exp(TRUE, "1", "new Integer(7) != new Byte((byte) 5)");
        exp(TRUE, "2", "new Integer(7) == 7");
        exp(TRUE, "3", "5 == new Byte((byte) 5)");

        // 15.21.2 Boolean Equality Operators == and !=
        exp(TRUE, "1", "new Boolean(true) != new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) == true");
        exp(TRUE, "3", "false == new Boolean(false)");
        exp(TRUE, "4", "false != true");

        // 15.22.2 Boolean Logical Operators &, ^, and |
        exp(COMP, "1", "new Boolean(true) & new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) ^ false");
        exp(TRUE, "3", "false | new Boolean(true)");

        // 15.23 Conditional-And Operator &&
        exp(TRUE, "1", "new Boolean(true) && new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) && true");
        exp(TRUE, "3", "true && new Boolean(true)");

        // 15.24 Conditional-Or Operator ||
        exp(TRUE, "1", "new Boolean(true) || new Boolean(false)");
        exp(TRUE, "2", "new Boolean(false) || true");
        exp(TRUE, "3", "true || new Boolean(true)");
    }
}
