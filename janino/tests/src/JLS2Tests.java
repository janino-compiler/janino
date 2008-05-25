
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Arrays;

import junit.framework.*;

import util.*;

public class JLS2Tests extends JaninoTestSuite {
    public static Test suite() {
        return new JLS2Tests();
    }

    public JLS2Tests() {
        super("Tests against the \"Java Language Specification\"; 2nd edition");

        section("3 Lexical structure");

        section("3.1 Unicode");
        exp(TRUE, "1", "'\\u00e4' == 'ה'");

        section("3.2 Lexical Translations");
        scr(PARS, "1", "3--4");

        section("3.3 Unicode Escapes");
        sca(INVA, "1", "aaa\\u123gbbb");
        exp(TRUE, "2", "\"\\u0041\".equals(\"A\")");
        exp(TRUE, "3", "\"\\uu0041\".equals(\"A\")");
        exp(TRUE, "4", "\"\\uuu0041\".equals(\"A\")");
        exp(TRUE, "5", "\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        exp(TRUE, "6", "\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");
        
        section("3.4 Line Terminators");
        exp(TRUE, "1", "1//\r+//\r\n2//\n==//\n\r3");
        
        section("3.6 White Space");
        exp(TRUE, "1", "3\t\r \n==3");
        
        section("3.7 Comments");
        exp(TRUE, "1", "7/* */==7");
        exp(TRUE, "2", "7/**/==7");
        exp(TRUE, "3", "7/***/==7");
        exp(SCAN, "4", "7/*/==7");
        exp(TRUE, "5", "7/*\r*/==7");
        exp(TRUE, "6", "7//\r==7");
        exp(TRUE, "7", "7//\n==7");
        exp(TRUE, "8", "7//\r\n==7");
        exp(TRUE, "9", "7//\n\r==7");
        scr(PARS, "10", "7// /*\n\rXXX*/==7");
        
        section("3.8 Identifiers");
        scr(EXEC, "1", "int a;");
        scr(EXEC, "2", "int ההה;");
        scr(EXEC, "3", "int \\u0391;"); // Greek alpha
        scr(EXEC, "4", "int _aaa;");
        scr(EXEC, "5", "int $aaa;");
        scr(PARS, "6", "int 9aaa;");
        scr(PARS, "7", "int const;");
        
        section("3.10 Literals");
        
        section("3.10.1 Integer Literals");
        exp(TRUE, "1", "17 == 17L");
        exp(TRUE, "2", "255 == 0xFFl");
        exp(TRUE, "3", "17 == 021L");
        scr(PARS, "4", "17 == 029");
        exp(TRUE, "5", "2 * 2147483647 == -2");
        exp(TRUE, "6", "2 * -2147483648 == 0");
        exp(COMP, "7", "2147483648");
        exp(EXEC, "8", "-2147483648");
        exp(TRUE, "9", "-1 == 0xffffffff");
        exp(TRUE, "10", "1 == -0xffffffff");
        exp(TRUE, "11", "-0xf == -15");
        exp(EXEC, "12", "9223372036854775807L");
        exp(COMP, "13", "9223372036854775808L");
        sca(INVA, "14", "9223372036854775809L");
        sca(INVA, "15", "99999999999999999999999999999L");
        exp(EXEC, "16", "-9223372036854775808L");
        exp(SCAN, "17", "-9223372036854775809L");
        
        section("3.10.2 Floating-Point Literals");
        exp(TRUE, "1", "1e1f == 10f");
        exp(TRUE, "2", "1E1F == 10f");
        exp(TRUE, "3", ".3f == 0.3f");
        exp(TRUE, "4", "0f == (float) 0");
        sca(VALI, "5", "3.14f");
        sca(VALI, "float_big",        "3.40282347e+38f");
        sca(INVA, "float_too_big",    "3.40282357e+38f");
        sca(VALI, "float_small",      "1.40239846e-45f");
        sca(INVA, "float_too_small",  "7.0e-46f");
        sca(VALI, "double_big",       "1.79769313486231570e+308D");
        sca(INVA, "double_too_big",   "1.79769313486231581e+308d");
        sca(VALI, "double_small",     "4.94065645841246544e-324D");
        sca(INVA, "double_too_small", "2e-324D");
        
        section("3.10.3 Boolean Literals");
        exp(TRUE, "1", "true");
        
        section("3.10.4 Character Literals");
        exp(TRUE, "1", "'a' == 97");
        sca(INVA, "2", "'''");
        sca(INVA, "3", "'\\'");
        sca(INVA, "4", "'\n'");
        sca(INVA, "5", "'ax'");
        sca(INVA, "6", "'a\n'");
        exp(TRUE, "7", "'\"' == 34"); // Unescaped double quote is allowed!
        
        section("3.10.5 String Literals");
        exp(TRUE, "1",  "\"'\".charAt(0) == 39"); // Unescaped single quote is allowed!
        // Escape sequences already tested above for character literals.
        exp(TRUE, "2", "\"\\b\".charAt(0) == 8");
        sca(INVA, "3", "\"aaa\nbbb\"");
        sca(INVA, "4", "\"aaa\rbbb\"");
        exp(TRUE, "5", "\"aaa\" == \"aaa\"");
        exp(TRUE, "6", "\"aaa\" != \"bbb\"");
        
        section("3.10.6 Escape Sequences for Character and String Literals");
        sca(INVA, "1",  "'\\u000a'"); // 0x000a is LF
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
        sca(INVA, "13", "'\\400'");
        sca(INVA, "14", "'\\1234'");

        section("3.10.7 The Null Literal");
        sca(VALI, "1", "null");

        section("3.11 Separators");
        sca(VALI, "1", ";");
        
        section("3.12 Operators");
        scr(TRUE, "1", "int a = -11; a >>>= 2; return a == 1073741821;");
        
        section("4 Types, Values, and Variables");
        
        section("4.1 The Kinds of Types and Values");
        
        section("5 Conversions and Promotions");
        
        section("5.1 Kinds of Conversions");

        section("5.1.7 Boxing Conversion");
        scr(TRUE, "boolean 1", "Boolean   b = true;        return b.booleanValue();");
        scr(TRUE, "boolean 2", "Boolean   b = false;       return !b.booleanValue();");
        scr(TRUE, "byte",      "Byte      b = (byte) 7;    return b.equals(new Byte((byte) 7));");
        scr(TRUE, "char",      "Character c = 'X';         return c.equals(new Character('X'));");
        scr(TRUE, "short",     "Short     s = (short) 322; return s.equals(new Short((short) 322));");
        scr(TRUE, "int",       "Integer   i = 99;          return i.equals(new Integer(99));");
        scr(TRUE, "long",      "Long      l = 733L;        return l.equals(new Long(733L));");
        scr(TRUE, "float",     "Float     f = 12.5F;       return f.equals(new Float(12.5F));");
        scr(TRUE, "double",    "Double    d = 14.3D;       return d.equals(new Double(14.3D));");
        
        section("5.1.8 Unboxing Conversion");
        exp(TRUE, "boolean 1", "Boolean.TRUE");
        exp(TRUE, "boolean 2", "!Boolean.FALSE");
        exp(TRUE, "byte",      "new Byte((byte) 9) == (byte) 9");
        exp(TRUE, "char",      "new Character('Y') == 'Y'");
        exp(TRUE, "short",     "new Short((short) 33) == (short) 33");
        exp(TRUE, "int",       "new Integer(-444) == -444");
        exp(TRUE, "long",      "new Long(987654321L) == 987654321L");
        exp(TRUE, "float",     "new Float(33.3F) == 33.3F");
        exp(TRUE, "double",    "new Double(939.939D) == 939.939D");

        section("5.2 Assignment Conversion");
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
        
        section("5.5 Casting Conversion");
        exp(TRUE, "Identity", "7 == (int) 7");
        exp(TRUE, "Widening Primitive", "(int) 'a' == 97");
        exp(TRUE, "Narrowing Primitive", "(int) 10000000000L == 1410065408");
        exp(TRUE, "Widening Reference", "((Object) \"SS\").equals(\"SS\")");
        scr(TRUE, "Narrowing Reference", "Object o = \"SS\"; return ((String) o).length() == 2;");
        exp(TRUE, "Boxing", "((Integer) 7).intValue() == 7");
        exp(TRUE, "Unboxing", "(int) new Integer(7) == 7");

        section("5.6 Numeric Promotions");
        section("5.6.1 Unary Numeric Promotion");
        exp(TRUE, "Unboxing 1", "-new Byte((byte) 7) == -7");
        exp(TRUE, "Unboxing 2", "-new Double(10.0D) == -10.0D");
        scr(TRUE, "Char", "char c = 'a'; return -c == -97;");

        section("5.6.2 Binary Numeric Promotion");
        exp(TRUE, "Unboxing Double", "2.5D * new Integer(4) == 10D");
        exp(TRUE, "float", "7 % new Float(2.5F) == 2F");
        exp(TRUE, "long", "2000000000 + 2000000000L == 4000000000L");
        exp(TRUE, "byte", "(short) 32767 + (byte) 100 == 32867");

        section("6 Names");
        
        section("6.1 Declarations");

        section("7 Packages");
        
        section("7.1 Package Members");
        
        section("7.5 Import Declarations");

        section("7.5.1 Single-Type-Import Declaration");
        exp(EXEC, "1", "import java.util.ArrayList; new ArrayList()");
        exp(EXEC, "Duplicate", "import java.util.ArrayList; import java.util.ArrayList; new ArrayList()");
        scr(COMP, "Inconsistent", "import java.util.List; import java.awt.List;");

        section("7.5.2 Type-Import-on-Demand Declaration");
        exp(EXEC, "1", "import java.util.*; new ArrayList()");
        exp(EXEC, "Duplicate", "import java.util.*; import java.util.*; new ArrayList()");
        
        section("7.5.3 Single Static Import Declaration");
        exp(TRUE, "Static field", "import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set");
        exp(TRUE, "Static field/duplicate", "import static java.util.Collections.EMPTY_SET; import static java.util.Collections.EMPTY_SET; EMPTY_SET instanceof java.util.Set");
        scr(EXEC, "Member type", "import static java.util.Map.Entry; Entry e;");
        scr(EXEC, "Member type/duplicate", "import static java.util.Map.Entry; import static java.util.Map.Entry; Entry e;");
        scr(COMP, "Member type/inconsistent", "import static java.util.Map.Entry; import static java.security.KeyStore.Entry; Entry e;");
        exp(TRUE, "Static method", "import static java.util.Arrays.asList; asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2");
        exp(TRUE, "Static method/duplicate", "import static java.util.Arrays.asList; import static java.util.Arrays.asList; asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2");
        scr(COMP, "Static method/inconsistent", "import static java.lang.Integer.toString; import static java.lang.Long.toString;");
        
        section("7.5.4 Static-Import-on-Demand Declaration");
        exp(TRUE, "Static field", "import static java.util.Collections.*; EMPTY_SET instanceof java.util.Set");
        scr(EXEC, "Member type", "import static java.util.Map.*; Entry e;");
        exp(TRUE, "Static method", "import static java.util.Arrays.*; asList(new String[] { \"HELLO\", \"WORLD\" }).size() == 2");

        section("8 Classes");
        
        section("8.1 Class Declaration");

        section("9 Interfaces");
        
        section("9.1 Interface Declarations");
        
        section("10 Arrays");

        section("10.1 Array Types");
        
        section("11 Exceptions");
        
        section("11.1 The Causes of Exceptions");
        
        section("12 Execution");

        section("12.1 Virtual Machine Start-Up");

        section("13 Binary Compatibility");

        section("13.1 The Form of a Binary");

        section("14 Blocks and Statements");

        section("14.3 Local Class Declarations");
        scr(TRUE, "1", "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;");

        section("14.8 Expression Statements");
        scr(TRUE, "1", "int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        scr(EXEC, "2", "System.currentTimeMillis();");
        scr(EXEC, "3", "new Object();");
        scr(PARS, "4", "new Object[3];");
        scr(PARS, "5", "int a; a;");
        
        section("14.10 The switch Statement");
        scr(TRUE, "1", "int x = 37; switch (x) {} return x == 37;");
        scr(TRUE, "2", "int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        scr(TRUE, "3", "int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;");
        scr(TRUE, "4", "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;");
        scr(TRUE, "5", "int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;");
        scr(TRUE, "6", "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;");

        section("15 Expressions");
        
        section("15.1 Evaluation, Denotation, and Result");

        section("15.9 Class Instance Creation Expressions");
        
        section("15.9.1 Determining the class being Instantiated");
        exp(TRUE, "3a", "new Object() instanceof Object");
        exp(COMP, "3b", "new java.util.List()");
        exp(COMP, "3c", "new other_package.PackageClass()");
        exp(COMP, "3d", "new java.util.AbstractList()");
        exp(TRUE, "4a", "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass");
        exp(PARS, "4b", "new other_package.Foo(3).new Foo.PublicMemberClass()");
        exp(PARS, "4c", "new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        exp(COMP, "4d", "new other_package.Foo(3).new PackageMemberClass()");
        exp(COMP, "4e", "new other_package.Foo(3).new PublicAbstractMemberClass()");
        exp(COMP, "4f", "new other_package.Foo(3).new PublicStaticMemberClass()");
        exp(COMP, "4g", "new other_package.Foo(3).new PublicMemberInterface()");
        exp(COMP, "4h", "new java.util.ArrayList().new PublicMemberClass()");
        
        section("15.9.3 Choosing the Constructor and its Arguments");
        exp(EXEC, "1", "new Integer(3)");
        exp(EXEC, "2", "new Integer(new Integer(3))");
        exp(EXEC, "3", "new Integer(new Byte((byte) 3))");
        exp(COMP, "4", "new Integer(new Object())");

        section("15.9.5 Anonymous Class Declarations");
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

        section("15.11 Field Access Expressions");
        section("15.11.2 Accessing Superclass Members using super");
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
            "            && T2.super.x == 1\n" +
            "            && ((T2) this).x == 2\n" +
            "            && ((T1) this).x == 1\n" +
            "        );\n" +
            "    }\n" +
            "}\n"
         ), "T3");
        
        section("15.12 Method Invocation Expressions");
        section("15.12.2 Compile-Time Step 2: Determine Method Signature");
        section("15.12.2.2 Choose the Most Specific Method");
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

        section("15.14 Postfix Expressions");
        section("15.14.2 Postfix Increment Operator ++");
        scr(TRUE, "1", "int i = 7; i++; return i == 8;");
        scr(TRUE, "2", "Integer i = new Integer(7); i++; return i.intValue() == 8;");
        scr(TRUE, "3", "int i = 7; return i == 7 && i++ == 7 && i == 8;");
        scr(TRUE, "4", "Integer i = new Integer(7); return i.intValue() == 7 && (i++).intValue() == 7 && i.intValue() == 8;");

        section("15.14.3 Postfix Decrement Operator --");
        scr(TRUE, "1", "int i = 7; i--; return i == 6;");
        scr(TRUE, "2", "Integer i = new Integer(7); i--; return i.intValue() == 6;");
        scr(TRUE, "3", "int i = 7; return i == 7 && i-- == 7 && i == 6;");
        scr(TRUE, "4", "Integer i = new Integer(7); return i.intValue() == 7 && (i--).intValue() == 7 && i.intValue() == 6;");

        section("15.15 Unary Operators");
        section("15.15.1 Prefix Increment Operator ++");
        scr(TRUE, "1", "int i = 7; ++i; return i == 8;");
        scr(TRUE, "2", "Integer i = new Integer(7); ++i; return i.intValue() == 8;");
        scr(TRUE, "3", "int i = 7; return i == 7 && ++i == 8 && i == 8;");
        scr(TRUE, "4", "Integer i = new Integer(7); return i.intValue() == 7 && (++i).intValue() == 8 && i.intValue() == 8;");

        section("15.15.2 Prefix Decrement Operator --");
        scr(TRUE, "1", "int i = 7; --i; return i == 6;");
        scr(TRUE, "2", "Integer i = new Integer(7); --i; return i.intValue() == 6;");
        scr(TRUE, "3", "int i = 7; return i == 7 && --i == 6 && i == 6;");
        scr(TRUE, "4", "Integer i = new Integer(7); return i.intValue() == 7 && (--i).intValue() == 6 && i.intValue() == 6;");

        section("15.15.3 Unary Plus Operator +");
        exp(TRUE, "1", "new Integer(+new Integer(7)).intValue() == 7");

        section("15.15.4 Unary Minus Operator -");
        exp(TRUE, "1", "new Integer(-new Integer(7)).intValue() == -7");

        section("15.17 Multiplicative Operators");
        exp(TRUE, "1", "new Integer(new Byte((byte) 2) * new Short((short) 3)).intValue() == 6");

        section("15.18 Additive Operators");
        exp(TRUE, "1", "(new Byte((byte) 7) - new Double(1.5D) + \"x\").equals(\"5.5x\")");

        section("15.18 Additive Operators");
        section("15.18.1 String Concatenation Operator +");
        section("15.18.1.3 Examples of String Concatenation");
        exp(TRUE, "1", "(\"The square root of 6.25 is \" + Math.sqrt(6.25)).equals(\"The square root of 6.25 is 2.5\")");
        exp(TRUE, "2", "1 + 2 + \" fiddlers\" == \"3 fiddlers\"");
        exp(TRUE, "3", "\"fiddlers \" + 1 + 2 == \"fiddlers 12\"");
        for (int i = 65530; i <= 65537; ++i) {
            char[] ca = new char[i];
            Arrays.fill(ca, 'x');
            String s1 = new String(ca);
            exp(TRUE, "4/" + i, "\"" + s1 +"\".length() == " + i);
            exp(TRUE, "5/" + i, "(\"" + s1 +"\" + \"XXX\").length() == " + (i + 3));
        }

        section("15.20 Relational Operators");

        section("15.20.1 Numerical Comparison Operators <, <=, > and >=");
        exp(TRUE, "1", "new Integer(7) > new Byte((byte) 5)");

        section("15.21 Equality Operators");

        section("15.21.1 Numerical Equality Operators == and !=");
        exp(TRUE, "1", "new Integer(7) != new Byte((byte) 5)");
        exp(TRUE, "2", "new Integer(7) == 7");
        exp(TRUE, "3", "5 == new Byte((byte) 5)");

        section("15.21.2 Boolean Equality Operators == and !=");
        exp(TRUE, "1", "new Boolean(true) != new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) == true");
        exp(TRUE, "3", "false == new Boolean(false)");
        exp(TRUE, "4", "false != true");

        section("15.22.2 Boolean Logical Operators &, ^, and |");
        exp(COMP, "1", "new Boolean(true) & new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) ^ false");
        exp(TRUE, "3", "false | new Boolean(true)");

        section("15.23 Conditional-And Operator &&");
        exp(TRUE, "1", "new Boolean(true) && new Boolean(true)");
        exp(TRUE, "2", "new Boolean(true) && true");
        exp(TRUE, "3", "true && new Boolean(true)");

        section("15.24 Conditional-Or Operator ||");
        exp(TRUE, "1", "new Boolean(true) || new Boolean(false)");
        exp(TRUE, "2", "new Boolean(false) || true");
        exp(TRUE, "3", "true || new Boolean(true)");

        section("16 Definite Assignment");

        section("16.1 Definite Assignment and Expressions");
    }
}
