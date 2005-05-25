
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
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

import java.io.IOException;

import org.codehaus.janino.*;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;


public class JaninoTests {
    public static Test suite() {
        TestSuite suite;

        suite = new TestSuite("Janino - Test numbers and titles refer to the Java  Language Specification; 2nd edition");

        // 3 Lexical structure
        {
            SimpleTestSuite suite3 = new SimpleTestSuite("3 Lexical structure");
            suite.addTest(suite3);

            suite3.aet("3.1 Unicode", "'\\u00e4' == 'ה'");
            suite3.ast("3.2 Lexical Translations", "3--4", ScriptTest.THROWS_PARSE_EXCEPTION);
            suite3.ast("3.3 Unicode Escapes/1", "aaa\\u123gbbb", ScriptTest.THROWS_IO_EXCEPTION);
            suite3.aet("3.3 Unicode Escapes/2", "\"\\u0041\".equals(\"A\")");
            suite3.aet("3.3 Unicode Escapes/3", "\"\\uu0041\".equals(\"A\")");
            suite3.aet("3.3 Unicode Escapes/4", "\"\\uuu0041\".equals(\"A\")");
            suite3.aet("3.3 Unicode Escapes/5", "\"\\\\u0041\".equals(\"\\\\\" + \"u0041\")");
            suite3.aet("3.3 Unicode Escapes/6", "\"\\\\\\u0041\".equals(\"\\\\\" + \"A\")");
            suite3.aet("3.4 Line Terminators", "1//\r+//\r\n2//\n==//\n\r3");
            suite3.aet("3.6 White Space", "3\t\r \n==3");
            suite3.aet("3.7 Comments/1", "7/* */==7");
            suite3.aet("3.7 Comments/2", "7/**/==7");
            suite3.aet("3.7 Comments/3", "7/***/==7");
            suite3.ast("3.7 Comments/4", "7/*/==7", ScriptTest.THROWS_SCAN_EXCEPTION);
            suite3.aet("3.7 Comments/5", "7/*\r*/==7");
            suite3.aet("3.7 Comments/6", "7//\r==7");
            suite3.aet("3.7 Comments/7", "7//\n==7");
            suite3.aet("3.7 Comments/8", "7//\r\n==7");
            suite3.aet("3.7 Comments/9", "7//\n\r==7");
            suite3.ast("3.7 Comments/10", "7// /*\n\rXXX*/==7", ScriptTest.THROWS_PARSE_EXCEPTION);
            suite3.ast("3.8 Identifiers/1", "int a;");
            suite3.ast("3.8 Identifiers/2", "int ההה;");
            suite3.ast("3.8 Identifiers/3", "int \\u0391;"); // Greek alpha
            suite3.ast("3.8 Identifiers/4", "int _aaa;");
            suite3.ast("3.8 Identifiers/5", "int $aaa;");
            suite3.ast("3.8 Identifiers/6", "int 9aaa;", ScriptTest.THROWS_PARSE_EXCEPTION);
            suite3.ast("3.8 Identifiers/7", "int const;", ScriptTest.THROWS_PARSE_EXCEPTION);
            suite3.aet("3.10.1 Integer Literals/1", "17 == 17L");
            suite3.aet("3.10.1 Integer Literals/2", "255 == 0xFFl");
            suite3.aet("3.10.1 Integer Literals/3", "17 == 021L");
            suite3.ast("3.10.1 Integer Literals/4", "17 == 029", ScriptTest.THROWS_PARSE_EXCEPTION);
            suite3.aet("3.10.1 Integer Literals/5", "2 * 2147483647 == -2");
            suite3.aet("3.10.1 Integer Literals/6", "2 * -2147483648 == 0");
            suite3.ast("3.10.1 Integer Literals/7", "2147483648;", ScriptTest.THROWS_COMPILE_EXCEPTION);
            suite3.ast("3.10.1 Integer Literals/8", "9223372036854775807L;");
            suite3.ast("3.10.1 Integer Literals/9", "9223372036854775808L;", ScriptTest.THROWS_COMPILE_EXCEPTION);
            suite3.ast("3.10.1 Integer Literals/10", "9223372036854775809L;", ScriptTest.THROWS_SCAN_EXCEPTION);
            suite3.ast("3.10.1 Integer Literals/11", "-9223372036854775808L;");
            suite3.ast("3.10.1 Integer Literals/12", "-9223372036854775809L;", ScriptTest.THROWS_SCAN_EXCEPTION);
//          suite3.ast("3.10.2 Floating-Point Literals", "");
//          suite3.ast("3.10.3 Boolean Literals", "");
//          suite3.ast("3.10.4 Character Literals", "");
//          suite3.ast("3.10.5 String Literals", "");
//          suite3.ast("3.10.6 Escape Sequences for Character and String Literals", "");
//          suite3.ast("3.10.7 The Null Literal", "");
//          suite3.ast("3.11 Separators", "");
//          suite3.ast("3.12 Operators", "");
        }

        // 4 Types, Values, and Variables
        {
            SimpleTestSuite suite4 = new SimpleTestSuite("4 Types, Values, and Variables");
            suite.addTest(suite4);

//          suite4.ast("4.1 The Kinds of Types and Values", "");
//          ...
        }

        // 5 Conversions and Promotions
        {
            SimpleTestSuite suite5 = new SimpleTestSuite("5 Conversions and Promotions");
            suite.addTest(suite5);

//          suite5.ast("5.1 Kinds of Conversions", "");
//          ...
        }

        // 6 Names
        {
            SimpleTestSuite suite6 = new SimpleTestSuite("6 Names");
            suite.addTest(suite6);

//          suite4.ast("6.1 Declarations", "");
//          ...
        }

        // 7 Packages
        {
            SimpleTestSuite suite7 = new SimpleTestSuite("7 Packages");
            suite.addTest(suite7);

//          suite7.ast("7.1 Package Members", "");
//          ...
        }

        // 8 Classes
        {
            SimpleTestSuite suite8 = new SimpleTestSuite("8 Classes");
            suite.addTest(suite8);

//          suite8.ast("8.1 Class Declaration", "");
//          ...
        }

        // 9 Interfaces
        {
            SimpleTestSuite suite9 = new SimpleTestSuite("9 Interfaces");
            suite.addTest(suite9);

//          suite4.ast("9.1 Interface Declarations", "");
//          ...
        }

        // 10 Arrays
        {
            SimpleTestSuite suite10 = new SimpleTestSuite("10 Arrays");
            suite.addTest(suite10);

//          suite4.ast("10.1 Array Types", "");
//          ...
        }

        // 11 Exceptions
        {
            SimpleTestSuite suite11 = new SimpleTestSuite("11 Exceptions");
            suite.addTest(suite11);

//          suite11.ast("11.1 The Causes of Exceptions", "");
//          ...
        }

        // 12 Execution
        {
            SimpleTestSuite suite12 = new SimpleTestSuite("12 Execution");
            suite.addTest(suite12);

//          suite12.ast("12.1 Virtual Machine Start-Up", "");
//          ...
        }

        // 13 Binary Compatibility
        {
            SimpleTestSuite suite13 = new SimpleTestSuite("13 Binary Compatibility");
            suite.addTest(suite13);

//          suite13.ast("13.1 The Form of a Binary", "");
//          ...
        }

        // 14 Blocks and Statements
        {
            SimpleTestSuite suite14 = new SimpleTestSuite("14 Blocks and Statements");
            suite.addTest(suite14);

//          suite14.ast("14.1 Normal and Abrupt Completion of Statements", "");
//          ...
        }

        // 15 Expressions
        {
            SimpleTestSuite suite15 = new SimpleTestSuite("15 Expressions");
            suite.addTest(suite15);

//          suite15.ast("15.1 Evaluation, Denotation, and Result", "");
//          ...
        }

        // 16 Definite Assignment
        {
            SimpleTestSuite suite16 = new SimpleTestSuite("16 Definite Assignment");
            suite.addTest(suite16);

//          suite16.ast("16.1 Definite Assignment and Expressions", "");
//          ...
        }

        return suite;
    }
}

class SimpleTestSuite extends TestSuite {
    SimpleTestSuite(String name) { super(name); }

    /**
     * Shorthand for "add expression test".
     */
    public void aet(String title, String expression) {
        this.addTest(new ExpressionTest(title, expression));
    }

    /**
     * Shorthand for "add script test".
     */
    public void ast(String title, String script) {
        this.addTest(new ScriptTest(title, script));
    }

    /**
     * Shorthand for "add script test".
     */
    public void ast(String title, String script, int mode) {
        this.addTest(new ScriptTest(title, script, mode));
    }
}

/**
 * A test case that compiles and evaluates a Janino expression, and verifies
 * that it evaluates to "true".
 */
class ExpressionTest extends TestCase {
    private String expression;

    public ExpressionTest(String name, String expression) {
        // Notice: JUnit 3.8.1 gets confused if the name contains "(" and/or ",".
        super(name);
        this.expression = expression;
    }

    /**
     * Compile and evaluate a Janino expression, and check its boolean return
     * value.
     */
    protected void runTest() throws Throwable {
        ExpressionEvaluator ee = new ExpressionEvaluator(this.expression, Boolean.TYPE, new String[0], new Class[0]);
        Object result = ee.evaluate(new Object[0]);
        assertTrue("Resulting expression is \"false\"", ((Boolean) result).booleanValue());
    }

}

/**
 * A test case that compiles and runs a Janino script, and optionally checks
 * it boolean return value for thruthness.
 */
class ScriptTest extends TestCase {
    private String script;
    int      mode;

    public static final int COMPILE_AND_EXECUTE      = 0;
    public static final int RETURNS_TRUE             = 1;
    public static final int THROWS_IO_EXCEPTION      = 2;
    public static final int THROWS_SCAN_EXCEPTION    = 3;
    public static final int THROWS_PARSE_EXCEPTION   = 4;
    public static final int THROWS_COMPILE_EXCEPTION = 5;

    public ScriptTest(String name, String script) {
        this(name, script, COMPILE_AND_EXECUTE);
    }

    public ScriptTest(String name, String script, int mode) {
        // Notice: JUnit 3.8.1 gets confused if the name contains "(" and/or ",".
        super(name);
        this.script = script;
        this.mode = mode;
    }

    /**
     * Compile and run a Janino script, and optionally check its boolean return
     * value.
     */
    protected void runTest() throws Throwable {
        ScriptEvaluator se;
        try {
            se = new ScriptEvaluator(this.script, this.mode == RETURNS_TRUE ? Boolean.TYPE : Void.TYPE);
        } catch (IOException ex) {
            if (this.mode == THROWS_IO_EXCEPTION) return;
            throw ex;
        } catch (Scanner.ScanException ex) {
            if (this.mode == THROWS_SCAN_EXCEPTION) return;
            throw ex;
        } catch (Parser.ParseException ex) {
            if (this.mode == THROWS_PARSE_EXCEPTION) return;
            throw ex;
        } catch (CompileException ex) {
            if (this.mode == THROWS_COMPILE_EXCEPTION) return;
            throw ex;
        }

        if (this.mode == THROWS_IO_EXCEPTION) fail("Should have thrown IOException");
        if (this.mode == THROWS_SCAN_EXCEPTION) fail("Should have thrown Scanner.ScanException");
        if (this.mode == THROWS_PARSE_EXCEPTION) fail("Should have thrown Parser.ParseException");
        if (this.mode == THROWS_COMPILE_EXCEPTION) fail("Should have thrown Java.CompileException");

        Object result = se.evaluate(new Object[0]);
        if (this.mode == RETURNS_TRUE) {
            assertTrue("Script did not return \"true\"", ((Boolean) result).booleanValue());
        }
    }
}
