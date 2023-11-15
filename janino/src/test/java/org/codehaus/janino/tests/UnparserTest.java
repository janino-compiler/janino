
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino.tests;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Located;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.TextBlock;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.TokenType;
import org.codehaus.janino.Unparser;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.util.AbstractTraverser;
import org.junit.Assert;
import org.junit.Test;

// SUPPRESS CHECKSTYLE JavadocMethod:9999

/**
 * Unit tests for the {@link Unparser}.
 */
public
class UnparserTest {

    private static void
    helpTestExpr(String input, String expected, boolean simplify) throws Exception {
        Parser p    = new Parser(new Scanner(null, new StringReader(input)));
        Rvalue expr = p.parseExpression();
        if (simplify) {
            expr = UnparserTest.stripUnnecessaryParenExprs(expr);
        }

        StringWriter sw = new StringWriter();
        Unparser     u  = new Unparser(sw);
        u.unparseRvalue(expr);
        u.close();
        String s = sw.toString();
        s = UnparserTest.replace(s, "((( ", "(");
        s = UnparserTest.replace(s, " )))", ")");
        Assert.assertEquals(input, expected, s);
    }

    private static void
    helpTestScript(String input) throws Exception {
        UnparserTest.helpTestScript(input, input);
    }

    private static void
    helpTestScript(String expected, String input) throws Exception {

        Parser p = new Parser(new Scanner(null, new StringReader(input)));

        Block block = new Block(Location.NOWHERE);
        block.addStatements(p.parseBlockStatements());

        String s;
        {
            StringWriter sw = new StringWriter();
            Unparser     u  = new Unparser(sw);
            u.unparseStatements(block.statements);
            u.close();
            s = sw.toString();
        }

        Assert.assertEquals(input, UnparserTest.normalizeWhitespace(expected), UnparserTest.normalizeWhitespace(s));
    }

    private static void
    helpTestClassBody(String input) throws Exception {
        UnparserTest.helpTestClassBody(input, input);
    }

    private static void
    helpTestClassBody(String expected, String input) throws Exception {

        Parser p = new Parser(new Scanner(null, new StringReader(input)));

        PackageMemberClassDeclaration pmcd = new PackageMemberClassDeclaration(
            Location.NOWHERE,
            null,             // docComment
            new Modifier[0],  // modifiers
            "SC",             // name
            null,             // typeParameters
            null,             // extendedType
            new Type[0]       // implementedTypes
        );
        while (!p.peek(TokenType.END_OF_INPUT)) p.parseClassBodyDeclaration(pmcd);

        String s;
        {
            StringWriter sw = new StringWriter();
            Unparser     u  = new Unparser(sw);
            u.unparseClassDeclarationBody(pmcd);
            u.close();
            s = sw.toString();
        }

        Assert.assertEquals(input, UnparserTest.normalizeWhitespace(expected), UnparserTest.normalizeWhitespace(s));
    }

    private static void
    helpTestCu(String cu) throws Exception { UnparserTest.helpTestCu(cu, cu); }

    private static void
    helpTestCu(String input, String expected) throws Exception {

        Parser                  p   = new Parser(new Scanner(null, new StringReader(input)));
        AbstractCompilationUnit acu = p.parseAbstractCompilationUnit();

        StringWriter sw = new StringWriter();
        Unparser     u  = new Unparser(sw);
        u.unparseAbstractCompilationUnit(acu);
        u.close();

        String actual = UnparserTest.normalizeWhitespace(sw.toString());
        Assert.assertEquals(expected, actual);
    }

    public static String
    normalizeWhitespace(String input) { return input.replaceAll("\\s+", " ").trim(); }

    private static String
    replace(String s, String from, String to) {
        for (;;) {
            int idx = s.indexOf(from);
            if (idx == -1) break;
            s = s.substring(0, idx) + to + s.substring(idx + from.length());
        }
        return s;
    }

    private static Java.Rvalue[]
    stripUnnecessaryParenExprs(Java.Rvalue[] rvalues) {
        Java.Rvalue[] res = new Java.Rvalue[rvalues.length];
        for (int i = 0; i < res.length; ++i) {
            res[i] = UnparserTest.stripUnnecessaryParenExprs(rvalues[i]);
        }
        return res;
    }

    @Nullable private static Java.Atom
    stripUnnecessaryParenExprsOpt(@Nullable Java.Atom atom) {
        return atom == null ? null : UnparserTest.stripUnnecessaryParenExprs(atom);
    }

    private static Java.Atom
    stripUnnecessaryParenExprs(Java.Atom atom) {
        if (atom instanceof Java.Rvalue) {
            return UnparserTest.stripUnnecessaryParenExprs((Java.Rvalue) atom);
        }
        return atom;
    }

    private static Java.Lvalue
    stripUnnecessaryParenExprs(Java.Lvalue lvalue) {
        return (Java.Lvalue) UnparserTest.stripUnnecessaryParenExprs((Java.Rvalue) lvalue);
    }

    @Nullable private static Java.Rvalue
    stripUnnecessaryParenExprsOpt(@Nullable Java.Rvalue rvalue) {
        return rvalue == null ? null : UnparserTest.stripUnnecessaryParenExprs(rvalue);
    }

    private static Java.Rvalue
    stripUnnecessaryParenExprs(Java.Rvalue rvalue) {

        Java.Rvalue result = rvalue.accept(new RvalueVisitor<Rvalue, RuntimeException>() {

            @Override @Nullable public Rvalue
            visitLvalue(Lvalue lv) {
                return lv.accept(new LvalueVisitor<Rvalue, RuntimeException>() {

                    @Override public Rvalue
                    visitAmbiguousName(AmbiguousName an) { return an; }

                    @Override public Rvalue
                    visitArrayAccessExpression(ArrayAccessExpression aae) {
                        return new Java.ArrayAccessExpression(
                            aae.getLocation(),
                            UnparserTest.stripUnnecessaryParenExprs(aae.lhs),
                            UnparserTest.stripUnnecessaryParenExprs(aae.index)
                        );
                    }

                    @Override public Rvalue
                    visitFieldAccess(FieldAccess fa) {
                        return new Java.FieldAccess(
                            fa.getLocation(),
                            UnparserTest.stripUnnecessaryParenExprs(fa.lhs),
                            fa.field
                        );
                    }

                    @Override public Rvalue
                    visitFieldAccessExpression(FieldAccessExpression fae) {
                        return new Java.FieldAccessExpression(
                            fae.getLocation(),
                            UnparserTest.stripUnnecessaryParenExprs(fae.lhs),
                            fae.fieldName
                        );
                    }

                    @Override public Rvalue
                    visitLocalVariableAccess(LocalVariableAccess lva) { return lva; }

                    @Override public Rvalue
                    visitParenthesizedExpression(ParenthesizedExpression pe) {
                        return UnparserTest.stripUnnecessaryParenExprs(pe.value);
                    }

                    @Override public Rvalue
                    visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { return scfae; }
                });
            }

            @Override public Rvalue
            visitArrayLength(ArrayLength al) {
                return new Java.ArrayLength(
                    al.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprs(al.lhs)
                );
            }

            @Override public Rvalue
            visitAssignment(Assignment a) {
                return new Java.Assignment(
                    a.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprs(a.lhs),
                    a.operator,
                    UnparserTest.stripUnnecessaryParenExprs(a.rhs)
                );
            }

            @Override public Rvalue
            visitBinaryOperation(BinaryOperation bo) {
                return new Java.BinaryOperation(
                    bo.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprs(bo.lhs),
                    bo.operator,
                    UnparserTest.stripUnnecessaryParenExprs(bo.rhs)
                );
            }

            @Override public Rvalue
            visitCast(Cast c) {
                return new Java.Cast(
                    c.getLocation(),
                    c.targetType,
                    UnparserTest.stripUnnecessaryParenExprs(c.value)
                );
            }

            @Override public Rvalue
            visitClassLiteral(ClassLiteral cl) {
                return cl; //too much effort
            }

            @Override public Rvalue
            visitConditionalExpression(ConditionalExpression ce) {
                return new Java.ConditionalExpression(
                    ce.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprs(ce.lhs),
                    UnparserTest.stripUnnecessaryParenExprs(ce.mhs),
                    UnparserTest.stripUnnecessaryParenExprs(ce.rhs)
                );
            }

            @Override public Rvalue
            visitCrement(Crement c) {
                if (c.pre) {
                    return new Java.Crement(
                        c.getLocation(),
                        c.operator,
                        UnparserTest.stripUnnecessaryParenExprs(c.operand)
                    );
                } else {
                    return new Java.Crement(
                        c.getLocation(),
                        UnparserTest.stripUnnecessaryParenExprs(c.operand),
                        c.operator
                    );
                }
            }

            @Override public Rvalue
            visitInstanceof(Instanceof io) {
                return new Java.Instanceof(
                    io.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprs(io.lhs),
                    io.rhs
                );
            }

            @Override public Rvalue visitIntegerLiteral(IntegerLiteral il)              { return il;  }
            @Override public Rvalue visitFloatingPointLiteral(FloatingPointLiteral fpl) { return fpl; }
            @Override public Rvalue visitBooleanLiteral(BooleanLiteral bl)              { return bl;  }
            @Override public Rvalue visitCharacterLiteral(CharacterLiteral cl)          { return cl;  }
            @Override public Rvalue visitStringLiteral(StringLiteral sl)                { return sl;  }
            @Override public Rvalue visitTextBlock(TextBlock tb)                        { return tb;  }
            @Override public Rvalue visitNullLiteral(NullLiteral nl)                    { return nl;  }

            @Override public Rvalue visitSimpleConstant(SimpleConstant sl) { return sl; }

            @Override public Rvalue
            visitMethodInvocation(MethodInvocation mi) {
                return new Java.MethodInvocation(
                    mi.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprsOpt(mi.target),
                    mi.methodName,
                    UnparserTest.stripUnnecessaryParenExprs(mi.arguments)
                );
            }

            @Override public Rvalue
            visitNewAnonymousClassInstance(NewAnonymousClassInstance naci) {
                return naci; //too much effort
            }

            @Override public Rvalue
            visitNewArray(NewArray na) {
                return new Java.NewArray(
                    na.getLocation(),
                    na.type,
                    UnparserTest.stripUnnecessaryParenExprs(na.dimExprs),
                    na.dims
                );
            }

            @Override public Rvalue
            visitNewClassInstance(NewClassInstance nci) {
                Type type = nci.type;
                assert type != null;
                return new Java.NewClassInstance(
                    nci.getLocation(),
                    UnparserTest.stripUnnecessaryParenExprsOpt(nci.qualification),
                    type,
                    UnparserTest.stripUnnecessaryParenExprs(nci.arguments)
                );
            }

            @Override public Rvalue
            visitNewInitializedArray(NewInitializedArray nia) {
                return nia; //too much effort
            }

            @Override public Rvalue
            visitParameterAccess(ParameterAccess pa) { return pa; }

            @Override public Rvalue
            visitQualifiedThisReference(QualifiedThisReference qtr) { return qtr; }

            @Override public Rvalue
            visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) {
                return new Java.SuperclassMethodInvocation(
                    smi.getLocation(),
                    smi.methodName,
                    UnparserTest.stripUnnecessaryParenExprs(smi.arguments)
                );
            }

            @Override public Rvalue
            visitThisReference(ThisReference tr) { return tr; }

            @Override @Nullable public Rvalue
            visitLambdaExpression(LambdaExpression lr) { return lr; }

            @Override @Nullable public Rvalue
            visitMethodReference(MethodReference mr) { return mr; }

            @Override @Nullable public Rvalue
            visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return cicr; }

            @Override @Nullable public Rvalue
            visitArrayCreationReference(ArrayCreationReference acr) { return acr; }

            @Override public Rvalue
            visitUnaryOperation(UnaryOperation uo) {
                return new Java.UnaryOperation(
                    uo.getLocation(),
                    uo.operator,
                    UnparserTest.stripUnnecessaryParenExprs(uo.operand)
                );
            }
        });

        assert result != null;
        return result;
    }

    @Test public void
    testInterface() throws Exception {
        UnparserTest.testInterfaceHelper(false);
        UnparserTest.testInterfaceHelper(true);
    }

    private static void
    testInterfaceHelper(boolean interfaceMod) {
        Java.PackageMemberInterfaceDeclaration decl = new Java.PackageMemberInterfaceDeclaration(
            Location.NOWHERE,                                                            // location
            "foo",                                                                       // docComment
            new Java.Modifier[] { new Java.AccessModifier("public", Location.NOWHERE) }, // modifiers
            "Foo",                                                                       // name
            null,                                                                        // typeParameters
            new Type[0]                                                                  // extendedTypes
        );
        StringWriter sw = new StringWriter();
        Unparser     u  = new Unparser(sw);
        u.unparseTypeDeclaration(decl);
        u.close();
        String s             = sw.toString();
        String correctString = "/**foo */ public interface Foo { }";
        Assert.assertEquals(correctString, UnparserTest.normalizeWhitespace(s));
    }

    @Test public void
    testLiterals() throws Exception {
        Object[][] tests = {
            { new FloatingPointLiteral(Location.NOWHERE, "-0.0D"), "-0.0D" },
            { new FloatingPointLiteral(Location.NOWHERE, "-0.0F"), "-0.0F" },
            { new TextBlock(Location.NOWHERE, "\"\"\"  \r   Line 1\r    Line 2\r\n   \t Line 3 \"\"\""), "\"\"\"  \r   Line 1\r    Line 2\r\n   \t Line 3 \"\"\"" },
        };
        for (Object[] test : tests) {
            assert test.length == 2;
            final Atom   expr     = (Atom)   test[0];
            final String expected = (String) test[1];

            StringWriter sw = new StringWriter();
            Unparser     u  = new Unparser(sw);
            u.unparseAtom(expr);
            u.close();
            Assert.assertEquals(expected, sw.toString());
        }
    }

    @Test public void
    testSimple() throws Exception {
        UnparserTest.helpTestExpr("1 + 2*3", "1 + 2 * 3", false);
        UnparserTest.helpTestExpr("1 + 2*3", "1 + 2 * 3", true);
    }

    @Test public void
    testParens() throws Exception {
        UnparserTest.helpTestExpr("(1 + 2)*3", "(1 + 2) * 3", false);
        UnparserTest.helpTestExpr("(1 + 2)*3", "(1 + 2) * 3", true);
    }

    @Test public void
    testMany() throws Exception {
        final String[][] exprs = {
            //input                                    expected simplified                    expect non-simplified
            { "((1)+2)",                               "1 + 2",                               "((1) + 2)"            },
            { "1 - 2 + 3",                             null,                                  null                   },
            { "(1 - 2) + 3",                           "1 - 2 + 3",                           null                   },
            { "1 - (2 + 3)",                           "1 - (2 + 3)",                         null                   },
            { "1 + 2 * 3",                             null,                                  null                   },
            { "1 + (2 * 3)",                           "1 + 2 * 3",                           null                   },
            { "3 - (2 - 1)",                           null,                                  null                   },
            { "true ? 1 : 2",                          null,                                  null                   },
            { "(true ? false : true) ? 1 : 2",         null,                                  null                   },
            { "true ? false : (true ? false : true)",  "true ? false : true ? false : true",  null                   },
            { "-(-(2))",                               "-(-2)",                               "-(-(2))"              },
            { "- - 2",                                 "-(-2)",                               "-(-2)"                },
            { "x && (y || z)",                         null,                                  null                   },
            { "(x && y) || z",                         "x && y || z",                         null                   },
            { "x = (y = z)",                           "x = y = z",                           null                   },
            { "x *= (y *= z)",                         "x *= y *= z",                         null                   },
            { "(--x) + 3",                             "--x + 3",                             null                   },
            { "(baz.bar).foo(x, (3 + 4) * 5)",         "baz.bar.foo(x, (3 + 4) * 5)",         null                   },
            { "!(bar instanceof Integer)",             null,                                  null                   },
            { "(true ? foo : bar).baz()",              null,                                  null                   },
            { "((String) foo).length()",               null,                                  null                   },
            { "-~2",                                   "-(~2)",                               "-(~2)"                },
            { "(new String[1])[0]",                    null,                                  null                   },
            { "(new String()).length()",               "new String().length()",               null                   },
            { "(new int[] { 1, 2 })[0]",               "new int[] { 1, 2 }[0]",               null                   },
            { "(\"asdf\" + \"qwer\").length()",        null,                                  null                   },
            { "-(a++)",                                "-a++",                                null                   },
            { "-1",                                    null,                                  null                   },
            { "-0x1",                                  "-0x1",                                "-0x1"                 },
            { "-0x7fffffff",                           "-0x7fffffff",                         "-0x7fffffff"          },
            { "-0x80000000",                           "-0x80000000",                         "-0x80000000"          },
            { "-0x80000001",                           "-0x80000001",                         "-0x80000001"          },
            { "-0x1l",                                 "-0x1l",                               "-0x1l",               },
            { "-0x7fffffffffffffffl",                  "-0x7fffffffffffffffl",                "-0x7fffffffffffffffl" },
            { "-0x8000000000000000l",                  "-0x8000000000000000l",                "-0x8000000000000000l" },
        };

        for (String[] expr : exprs) {
            String input          = expr[0];
            String expectSimplify = expr[1];
            if (expectSimplify == null) {
                expectSimplify = input;
            }

            String expectNoSimplify = expr[2];
            if (expectNoSimplify == null) {
                expectNoSimplify = input;
            }

            UnparserTest.helpTestExpr(input, expectSimplify, true);
            UnparserTest.helpTestExpr(input, expectNoSimplify, false);
        }
    }

    @Test public void
    testParseUnparseEnumDeclaration() throws Exception {
        final String[][] cus = {
            //input:                                                      expected output (if different from input):
            { "enum Foo { A, B ; }",                                      null },
            { "public enum Foo { A, B ; }",                               null },
            { "public enum Foo implements Serializable { A, B ; }",       null },
            { "enum Foo { A, B ; int meth() { return 0; } }",             null },
            { "enum Foo { @Deprecated A ; }",                             null },
            { "enum Foo { @Bar(foo = \"bar\", cls = String.class) A ; }", null },
            { "enum Foo { A(1, 2, 3) ; }",                                null },
            { "enum Foo { A { void meth() {} } ; }",                      null },
        };

        for (String[] expr : cus) {

            String input = expr[0];

            String expect = expr.length >= 2 && expr[1] != null ? expr[1] : input;

            UnparserTest.helpTestCu(input, expect);
        }
    }

    @Test public void
    testParseUnparseAnnotationTypeDeclaration() throws Exception {
        final String[][] cus = {
            //input:                                        expected output (if different from input):
            { "@interface Foo { }",                         null },
            { "public @interface Foo { }",                  null },
            { "@Deprecated @interface Foo { }",             null },
            { "@interface Foo { int value(); }",            null },
            { "@interface Foo { int[] value(); }",          null },
//            { "@interface Foo { int value() default 99; }", null },  NYI
        };

        for (String[] expr : cus) {

            String input = expr[0];

            String expect = expr.length >= 2 && expr[1] != null ? expr[1] : input;

            UnparserTest.helpTestCu(input, expect);
        }
    }

    @Test public void
    testParseUnparseJava5() throws Exception {

        // Type arguments.
        UnparserTest.helpTestScript("{ java.util.Set<String> set = new java.util.HashSet<String>(); }");
    }

    @Test public void
    testParseUnparseJava7() throws Exception {

        // Catching and rethrowing multiple exception types.
        UnparserTest.helpTestScript("{ try {} catch (java.io.IOException | RuntimeException e) { throw e; } }");

        // Type inference for generic instance creation.
        UnparserTest.helpTestScript("{ java.util.Map<String, Integer> map = new java.util.HashMap<>(); }");
    }

    @Test public void
    testParseUnparseJava8() throws Exception {

        // Lambda expressions.
        UnparserTest.helpTestScript("{ new Thread(()         -> {});   }");
        UnparserTest.helpTestScript("{ return ()             -> {};    }");
        UnparserTest.helpTestScript("{ return a              -> {};    }");
        UnparserTest.helpTestScript("{ return (int a, int b) -> {};    }");
        UnparserTest.helpTestScript("{ return ()             -> 3 + 7; }");
    }

    @Test public void
    testParseUnparseJava9() throws Exception {

        // Method references.
        UnparserTest.helpTestScript("Runnable r = new Runnable() { @Override public void run() {} }; Runnable s = r::run;");
        UnparserTest.helpTestScript("Runnable r = new Runnable() { @Override public void run() {} }; Runnable s = (r)::run;");
        UnparserTest.helpTestScript("Runnable r = new Runnable() { @Override public void run() {} }; Runnable t = java.util.Collections::emptySet;");
        UnparserTest.helpTestScript("x.map(this.productConverter::convert);");
        // TODO: 'super' '::' [ TypeArguments ] Identifier
        // TODO: TypeName '.' 'super' '::' [ TypeArguments ] Identifier
        UnparserTest.helpTestScript("Runnable r4 = java.util.HashMap::new;");
        UnparserTest.helpTestScript("java.util.function.Consumer<Integer> c1 = int[]::new;");

        // Modular compilation unit.
        UnparserTest.helpTestCu("import java.io.*; @Deprecated open module a.b.c()");
        UnparserTest.helpTestCu("module a(requires transitive static a.b;)");
        UnparserTest.helpTestCu("module a( requires a; requires b; )");
        UnparserTest.helpTestCu("module a(exports a.b to c.d, e.f;)");
        UnparserTest.helpTestCu("module a(opens a.b to c.d, e.f;)");
        UnparserTest.helpTestCu("module a(uses java.lang.String;)");
        UnparserTest.helpTestCu("module a(provides java.lang.String with a.b, c.d;)");

        // Default methods.
        UnparserTest.helpTestCu(
            ""
            + "interface Foo {"
            +    " default <T> T convertJsonToObject(String jsonFile, Class<T> classType) throws ServiceException {} "
            + "}"
        );

        // Type annotations.
        UnparserTest.helpTestClassBody(
            ""
            + "private Map<@notblank String, @notblank String> extensions;\n"
            + "private List<@NotNull AssetRelationship>        sources = new ArrayList<>();\n"
        );
        UnparserTest.helpTestScript("@Foo int a;");
    }

    @Test public void
    testParseUnparseJava15() throws Exception {

        // Text blocks.
        UnparserTest.helpTestExpr(
            (                        // input
                ""
                + "   \"\"\" \r"
                + "Line 1\n"
                + "Line 2\r\n"
                + "Line3   \"\"\"  "
            ),
            (                        // expected
                ""
                + "\"\"\" \r"
                + "Line 1\n"
                + "Line 2\r\n"
                + "Line3   \"\"\""
            ),
            false
        );
    }

    @Test public void
    testParseUnparseParseJanino() throws Exception {

        for (File f : UnparserTest.findJaninoJavaFiles()) {

            try {

                // Parse the source file once.
                AbstractCompilationUnit acu1;
                {
                    InputStream is = new FileInputStream(f);
                    acu1 = new Parser(new Scanner(f.toString(), is)).parseAbstractCompilationUnit();
                    is.close();
                }

                // Unparse the compilation unit.
                String text;
                {
                    StringWriter sw = new StringWriter();
                    Unparser.unparse(acu1, sw);
                    text = sw.toString();
                }

                // Then parse again.
                AbstractCompilationUnit acu2  = new Parser(
                    new Scanner(f.toString(), new StringReader(text))
                ).parseAbstractCompilationUnit();

                // Compare the two ASTs.
                Java.Locatable[] elements1 = UnparserTest.listSyntaxElements(acu1);
                Java.Locatable[] elements2 = UnparserTest.listSyntaxElements(acu2);
                for (int i = 0;; ++i) {
                    if (i == elements1.length) {
                        if (i == elements2.length) break;
                        Assert.fail("Extra element " + elements2[i]);
                    }
                    Locatable locatable1 = elements1[i];

                    if (i == elements2.length) {
                        Assert.fail("Element missing: " + locatable1);
                    }
                    Locatable locatable2 = elements2[i];

                    String s1 = locatable1.toString();
                    String s2 = locatable2.toString();
                    if (!s1.equals(s2)) {
                        Assert.fail(
                            locatable1.getLocation().toString()
                            + ": Expected \""
                            + s1
                            + "\", was \""
                            + s2
                            + "\""
                        );
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Collection<File>
    findJaninoJavaFiles() {
        final Collection<File> result = new ArrayList<>();

        // Process all "*.java" files in the JANINO source tree.
        // Must use the "janino" project directory, because that is pre-Java 5.
        UnparserTest.find(new File("../janino/src/main/java"), new FileFilter() {

            @Override public boolean
            accept(@Nullable File f) {
                assert f != null;

                if (f.isDirectory()) return true;

                if (f.getName().endsWith(".java") && f.isFile()) result.add(f);

                return false;
            }
        });

        return result;
    }

    /**
     * Traverses the given {@link AbstractCompilationUnit} and collect a list of all its syntactical elements.
     */
    private static Locatable[]
    listSyntaxElements(AbstractCompilationUnit cu) {

        final List<Locatable> locatables = new ArrayList<>();
        new AbstractTraverser<RuntimeException>() {

            // Two implementations of "Locatable": "Located" and "AbstractTypeDeclaration".
            @Override public void
            traverseLocated(Located l) {
                locatables.add(l);
                super.traverseLocated(l);
            }

            @Override public void
            traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd) {
                locatables.add(atd);
                super.traverseAbstractTypeDeclaration(atd);
            }
        }.visitAbstractCompilationUnit(cu);

        return locatables.toArray(new Java.Locatable[locatables.size()]);
    }

    /**
     * Invokes <var>fileFilter</var> for all files and subdirectories in the given <var>directory</var>. If {@link
     * FileFilter#accept(File)} returns {@code true}, recurse with that file/directory.
     */
    private static void
    find(File directory, FileFilter fileFilter) {
        File[] subDirectories = directory.listFiles(fileFilter);
        if (subDirectories == null) throw new AssertionError(directory + " is not a directory");
        for (File subDirectorie : subDirectories) UnparserTest.find(subDirectorie, fileFilter);
    }
}
