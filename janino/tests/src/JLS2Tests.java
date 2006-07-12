
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
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
        exp(EXEC, "8", "9223372036854775807L");
        exp(COMP, "9", "9223372036854775808L");
        sca(INVA, "10", "9223372036854775809L");
        exp(EXEC, "11", "-9223372036854775808L");
        exp(SCAN, "12", "-9223372036854775809L");
        
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
        
        section("6 Names");
        
        section("6.1 Declarations");
        
        section("7 Packages");
        
        section("7.1 Package Members");
        
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
        
        section("16 Definite Assignment");
        
        section("16.1 Definite Assignment and Expressions");
    }
}
