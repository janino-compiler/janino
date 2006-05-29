
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
        aet(TRUE, "1", "'\\u00e4' == 'ה'");

        section("3.2 Lexical Translations");
        ast(PARS, "1", "3--4");

        section("3.3 Unicode Escapes");
        ast(SCAN, "1", "aaa\\u123gbbb");
        aet(TRUE, "2", "\"\\u0041\".equals(\"A\")");
        aet(TRUE, "3", "\"\\uu0041\".equals(\"A\")");
        aet(TRUE, "4", "\"\\uuu0041\".equals(\"A\")");
        aet(TRUE, "5", "\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
        aet(TRUE, "6", "\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");
        
        section("3.4 Line Terminators");
        aet(TRUE, "1", "1//\r+//\r\n2//\n==//\n\r3");
        
        section("3.6 White Space");
        aet(TRUE, "1", "3\t\r \n==3");
        
        section("3.7 Comments");
        aet(TRUE, "1", "7/* */==7");
        aet(TRUE, "2", "7/**/==7");
        aet(TRUE, "3", "7/***/==7");
        ast(SCAN, "4", "7/*/==7");
        aet(TRUE, "5", "7/*\r*/==7");
        aet(TRUE, "6", "7//\r==7");
        aet(TRUE, "7", "7//\n==7");
        aet(TRUE, "8", "7//\r\n==7");
        aet(TRUE, "9", "7//\n\r==7");
        ast(PARS, "10", "7// /*\n\rXXX*/==7");
        
        section("3.8 Identifiers");
        ast(EXEC, "1", "int a;");
        ast(EXEC, "2", "int ההה;");
        ast(EXEC, "3", "int \\u0391;"); // Greek alpha
        ast(EXEC, "4", "int _aaa;");
        ast(EXEC, "5", "int $aaa;");
        ast(PARS, "6", "int 9aaa;");
        ast(PARS, "7", "int const;");
        
        section("3.10 Literals");
        
        section("3.10.1 Integer Literals");
        aet(TRUE, "1", "17 == 17L");
        aet(TRUE, "2", "255 == 0xFFl");
        aet(TRUE, "3", "17 == 021L");
        ast(PARS, "4", "17 == 029");
        aet(TRUE, "5", "2 * 2147483647 == -2");
        aet(TRUE, "6", "2 * -2147483648 == 0");
        aet(COMP, "7", "2147483648");
        aet(EXEC, "8", "9223372036854775807L");
        aet(COMP, "9", "9223372036854775808L");
        aet(SCAN, "10", "9223372036854775809L");
        aet(EXEC, "11", "-9223372036854775808L");
        aet(SCAN, "12", "-9223372036854775809L");
        
        section("3.10.2 Floating-Point Literals");
        
        section("3.10.3 Boolean Literals");
        
        section("3.10.4 Character Literals");
        
        section("3.10.5 String Literals");
        
        section("3.10.6 Escape Sequences for Character and String Literals");
        
        section("3.10.7 The Null Literal");
        
        section("3.11 Separators");
        
        section("3.12 Operators");
        
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
        ast(TRUE, "1", "class S2 extends SC { public int foo() { return 37; } }; return new S2().foo() == 37;");

        section("14.8 Expression Statements");
        ast(TRUE, "1", "int a; a = 8; ++a; a++; if (a != 10) return false; --a; a--; return a == 8;");
        ast(EXEC, "2", "System.currentTimeMillis();");
        ast(EXEC, "3", "new Object();");
        ast(PARS, "4", "new Object[3];");
        ast(PARS, "5", "int a; a;");
        
        section("14.10 The switch Statement");
        ast(TRUE, "1", "int x = 37; switch (x) {} return x == 37;");
        ast(TRUE, "2", "int x = 37; switch (x) { default: ++x; break; } return x == 38;");
        ast(TRUE, "3", "int x = 37; switch (x) { case 36: case 37: case 38: x += x; break; } return x == 74;");
        ast(TRUE, "4", "int x = 37; switch (x) { case 36: case 37: case 1000: x += x; break; } return x == 74;");
        ast(TRUE, "5", "int x = 37; switch (x) { case -10000: break; case 10000: break; } return x == 37;");
        ast(TRUE, "6", "int x = 37; switch (x) { case -2000000000: break; case 2000000000: break; } return x == 37;");

        section("15 Expressions");
        
        section("15.1 Evaluation, Denotation, and Result");
        
        section("15.9 Class Instance Creation Expressions");
        
        section("15.9.1 Determining the class being Instantiated");
        aet(TRUE, "3a", "new Object() instanceof Object");
        aet(COMP, "3b", "new java.util.List()");
        aet(COMP, "3c", "new other_package.PackageClass()");
        aet(COMP, "3d", "new java.util.AbstractList()");
        aet(TRUE, "4a", "new other_package.Foo(3).new PublicMemberClass() instanceof other_package.Foo.PublicMemberClass");
        aet(PARS, "4b", "new other_package.Foo(3).new Foo.PublicMemberClass()");
        aet(PARS, "4c", "new other_package.Foo(3).new other_package.Foo.PublicMemberClass()");
        aet(COMP, "4d", "new other_package.Foo(3).new PackageMemberClass()");
        aet(COMP, "4e", "new other_package.Foo(3).new PublicAbstractMemberClass()");
        aet(COMP, "4f", "new other_package.Foo(3).new PublicStaticMemberClass()");
        aet(COMP, "4g", "new other_package.Foo(3).new PublicMemberInterface()");
        aet(COMP, "4h", "new java.util.ArrayList().new PublicMemberClass()");
        
        section("16 Definite Assignment");
        
        section("16.1 Definite Assignment and Expressions");
    }
}
