
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

package org.codehaus.janino.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Mod;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnparseVisitor;
import org.codehaus.janino.Visitor;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Located;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.util.Traverser;
import org.junit.Test;

public class UnparseTests {

    private static void helpTestExpr(String input, String expect, boolean simplify) throws Exception {
        Parser p = new Parser(new Scanner(null, new StringReader(input)));
        Atom expr = p.parseExpression();
        if (simplify) {
            expr = UnparseTests.stripUnnecessaryParenExprs(expr);
        }

        StringWriter sw = new StringWriter();
        UnparseVisitor uv = new UnparseVisitor(sw);
        expr.accept(uv);
        uv.close();
        String s = sw.toString();
        s = UnparseTests.replace(s, "((( ", "(");
        s = UnparseTests.replace(s, " )))", ")");
        assertEquals(expect, s);
    }

    private static String normalizeWhitespace(String input) {
        return input.replaceAll("\\s+", " ").trim();
    }
    private static String replace(String s, String from, String to) {
        for (;;) {
            int idx = s.indexOf(from);
            if (idx == -1) break;
            s = s.substring(0, idx) + to + s.substring(idx + from.length());
        }
        return s;
    }

    private static Java.Rvalue[] stripUnnecessaryParenExprs(Java.Rvalue[] rvalues) {
        Java.Rvalue[] res = new Java.Rvalue[rvalues.length];
        for (int i = 0; i < res.length; ++i) {
            res[i] = UnparseTests.stripUnnecessaryParenExprs(rvalues[i]);
        }
        return res;
    }

    private static Java.Atom stripUnnecessaryParenExprs(Java.Atom atom) {
        if (atom instanceof Java.Rvalue) {
            return UnparseTests.stripUnnecessaryParenExprs((Java.Rvalue) atom);
        }
        return atom;
    }

    private static Java.Lvalue stripUnnecessaryParenExprs(Java.Lvalue lvalue) {
        return (Java.Lvalue) UnparseTests.stripUnnecessaryParenExprs((Java.Rvalue) lvalue);
    }

    private static Java.Rvalue stripUnnecessaryParenExprs(Java.Rvalue rvalue) {
        if (rvalue == null) { return null; }
        final Java.Rvalue[] res = new Java.Rvalue[1];
        Visitor.RvalueVisitor rv = new Visitor.RvalueVisitor() {
            public void visitArrayLength(ArrayLength al) {
                res[0] = new Java.ArrayLength(
                    al.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(al.lhs)
                );
            }

            public void visitAssignment(Assignment a) {
                res[0] = new Java.Assignment(
                    a.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(a.lhs),
                    a.operator,
                    UnparseTests.stripUnnecessaryParenExprs(a.rhs)
                );
            }

            public void visitBinaryOperation(BinaryOperation bo) {
                res[0] = new Java.BinaryOperation(
                    bo.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(bo.lhs),
                    bo.op,
                    UnparseTests.stripUnnecessaryParenExprs(bo.rhs)
                );
            }

            public void visitCast(Cast c) {
                res[0] = new Java.Cast(
                    c.getLocation(),
                    c.targetType,
                    UnparseTests.stripUnnecessaryParenExprs(c.value)
                );
            }

            public void visitClassLiteral(ClassLiteral cl) {
                res[0] = cl; //too much effort
            }

            public void visitConditionalExpression(ConditionalExpression ce) {
                res[0] = new Java.ConditionalExpression(
                    ce.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(ce.lhs),
                    UnparseTests.stripUnnecessaryParenExprs(ce.mhs),
                    UnparseTests.stripUnnecessaryParenExprs(ce.rhs)
                );
            }

            public void visitCrement(Crement c) {
                if (c.pre) {
                    res[0] = new Java.Crement(
                        c.getLocation(),
                        c.operator,
                        UnparseTests.stripUnnecessaryParenExprs(c.operand)
                    );
                } else {
                    res[0] = new Java.Crement(
                        c.getLocation(),
                        UnparseTests.stripUnnecessaryParenExprs(c.operand),
                        c.operator
                    );
                }
            }

            public void visitInstanceof(Instanceof io) {
                res[0] = new Java.Instanceof(
                    io.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(io.lhs),
                    io.rhs
                );
            }

            public void visitIntegerLiteral(IntegerLiteral il) {
                res[0] = il;
            }

            public void visitFloatingPointLiteral(FloatingPointLiteral fpl) {
                res[0] = fpl;
            }

            public void visitBooleanLiteral(BooleanLiteral bl) {
                res[0] = bl;
            }

            public void visitCharacterLiteral(CharacterLiteral cl) {
                res[0] = cl;
            }

            public void visitStringLiteral(StringLiteral sl) {
                res[0] = sl;
            }

            public void visitNullLiteral(NullLiteral nl) {
                res[0] = nl;
            }

            public void visitMethodInvocation(MethodInvocation mi) {
                res[0] = new Java.MethodInvocation(
                    mi.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(mi.optionalTarget),
                    mi.methodName,
                    UnparseTests.stripUnnecessaryParenExprs(mi.arguments)
                );
            }

            public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci) {
                res[0] = naci; //too much effort
            }

            public void visitNewArray(NewArray na) {
                res[0] = new Java.NewArray(
                    na.getLocation(),
                    na.type,
                    UnparseTests.stripUnnecessaryParenExprs(na.dimExprs),
                    na.dims
                );
            }

            public void visitNewClassInstance(NewClassInstance nci) {
                res[0] = new Java.NewClassInstance(
                    nci.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(nci.optionalQualification),
                    nci.type,
                    UnparseTests.stripUnnecessaryParenExprs(nci.arguments)
                );
            }

            public void visitNewInitializedArray(NewInitializedArray nia) {
                res[0] = nia; //too much effort
            }

            public void visitParameterAccess(ParameterAccess pa) {
                res[0] = pa;
            }

            public void visitQualifiedThisReference(QualifiedThisReference qtr) {
                res[0] = qtr;
            }

            public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) {
                res[0] = new Java.SuperclassMethodInvocation(
                    smi.getLocation(),
                    smi.methodName,
                    UnparseTests.stripUnnecessaryParenExprs(smi.arguments)
                );
            }

            public void visitThisReference(ThisReference tr) {
                res[0] = tr;
            }

            public void visitUnaryOperation(UnaryOperation uo) {
                res[0] = new Java.UnaryOperation(
                    uo.getLocation(),
                    uo.operator,
                    UnparseTests.stripUnnecessaryParenExprs(uo.operand)
                );
            }

            public void visitAmbiguousName(AmbiguousName an) {
                res[0] = an;
            }

            public void visitArrayAccessExpression(ArrayAccessExpression aae) {
                res[0] = new Java.ArrayAccessExpression(
                    aae.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(aae.lhs),
                    UnparseTests.stripUnnecessaryParenExprs(aae.index)
                );
            }
            public void visitFieldAccess(FieldAccess fa) {
                res[0] = new Java.FieldAccess(
                    fa.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(fa.lhs),
                    fa.field
                );
            }

            public void visitFieldAccessExpression(FieldAccessExpression fae) {
                res[0] = new Java.FieldAccessExpression(
                    fae.getLocation(),
                    UnparseTests.stripUnnecessaryParenExprs(fae.lhs),
                    fae.fieldName
                );
            }

            public void visitLocalVariableAccess(LocalVariableAccess lva) {
                res[0] = lva;
            }

            public void visitParenthesizedExpression(ParenthesizedExpression pe) {
                res[0] = UnparseTests.stripUnnecessaryParenExprs(pe.value);
            }

            public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) {
                res[0] = scfae;
            }

        };
        rvalue.accept(rv);
        return res[0];
    }

    @Test
    public void testInterface() throws Exception {
        testInterfaceHelper(false);
        testInterfaceHelper(true);
    }

    private void testInterfaceHelper(boolean interfaceMod) throws CompileException {
        short modifier = Mod.PUBLIC;
        if (interfaceMod) {
            modifier |= Mod.INTERFACE;
        }
        Java.PackageMemberInterfaceDeclaration decl = new Java.PackageMemberInterfaceDeclaration(
            null,
            "foo",
            modifier,
            "Foo",
            new Type[0]
        );
        StringWriter sw = new StringWriter();
        UnparseVisitor uv = new UnparseVisitor(sw);
        decl.accept(uv);
        uv.close();
        String s = sw.toString();
        String correctString = "/**foo */ public interface Foo { }";
        assertEquals(correctString, normalizeWhitespace(s));
    }

    @Test
    public void testLiterals() throws Exception {
        Object[][] tests = new Object[][] {
            { new FloatingPointLiteral(null, "-0.0D"), "-0.0D" },
            { new FloatingPointLiteral(null, "-0.0F"), "-0.0F" },
        };
        for (int i = 0; i < tests.length; ++i) {
            Atom expr = (Atom) tests[i][0];
            String expected = (String) tests[i][1];

            StringWriter sw = new StringWriter();
            UnparseVisitor uv = new UnparseVisitor(sw);
            expr.accept(uv);
            uv.close();
            assertEquals(expected, sw.toString());
        }
    }

    @Test
    public void testSimple() throws Exception {
        UnparseTests.helpTestExpr("1 + 2*3", "1 + 2 * 3", false);
        UnparseTests.helpTestExpr("1 + 2*3", "1 + 2 * 3", true);
    }

    @Test
    public void testParens() throws Exception {
        UnparseTests.helpTestExpr("(1 + 2)*3", "(1 + 2) * 3", false);
        UnparseTests.helpTestExpr("(1 + 2)*3", "(1 + 2) * 3", true);
    }

    @Test
    public void testMany() throws Exception {
        final String[][] exprs = new String[][] {
              //input                                  expected simplified                    expect non-simplified
            { "((1)+2)",                               "1 + 2",                               "((1) + 2)"           },
            { "1 - 2 + 3",                             null,                                  null                  },
            { "(1 - 2) + 3",                           "1 - 2 + 3",                           null                  },
            { "1 - (2 + 3)",                           "1 - (2 + 3)",                         null                  },
            { "1 + 2 * 3",                             null,                                  null                  },
            { "1 + (2 * 3)",                           "1 + 2 * 3",                           null                  },
            { "3 - (2 - 1)",                           null,                                  null                  },
            { "true ? 1 : 2",                          null,                                  null                  },
            { "(true ? false : true) ? 1 : 2",         null,                                  null                  },
            { "true ? false : (true ? false : true)",  "true ? false : true ? false : true",  null                  },
            { "-(-(2))",                               "-(-2)",                               "-(-(2))"             },
            { "- - 2",                                 "-(-2)",                               "-(-2)"               },
            { "x && (y || z)",                         null,                                  null                  },
            { "(x && y) || z",                         "x && y || z",                         null                  },
            { "x = (y = z)",                           "x = y = z",                           null                  },
            { "x *= (y *= z)",                         "x *= y *= z",                         null                  },
            { "(--x) + 3",                             "--x + 3",                             null                  },
            { "(baz.bar).foo(x, (3 + 4) * 5)",         "baz.bar.foo(x, (3 + 4) * 5)",         null                  },
            { "!(bar instanceof Integer)",             null,                                  null                  },
            { "(true ? foo : bar).baz()",              null,                                  null                  },
            { "((String) foo).length()",               null,                                  null                  },
            { "-~2",                                   "-(~2)",                               "-(~2)"               },
            { "(new String[1])[0]",                    null,                                  null                  },
            { "(new String()).length()",               "new String().length()",               null                  },
            { "(new int[] { 1, 2 })[0]",               "new int[] { 1, 2 }[0]",               null                  },
            { "(\"asdf\" + \"qwer\").length()",        null,                                  null                  },
            { "-(a++)",                                "-a++",                                null                  },
            { "-1",                                    null,                                  null                  },
            { "-0x1",                                  "-0x1",                                "-0x1"                },
            { "-0x7fffffff",                           "-0x7fffffff",                         "-0x7fffffff"         },
            { "-0x80000000",                           "-0x80000000",                         "-0x80000000"         },
            { "-0x80000001",                           "-0x80000001",                         "-0x80000001"         },
            { "-0x1l",                                 "-0x1l",                               "-0x1l",              },
            { "-0x7fffffffffffffffl",                  "-0x7fffffffffffffffl",                "-0x7fffffffffffffffl"},
            { "-0x8000000000000000l",                  "-0x8000000000000000l",                "-0x8000000000000000l"},
        };

        for (int i = 0; i < exprs.length; ++i) {
            String input = exprs[i][0];
            String expectSimplify = exprs[i][1];
            if (expectSimplify == null) {
                expectSimplify = input;
            }

            String expectNoSimplify = exprs[i][2];
            if (expectNoSimplify == null) {
                expectNoSimplify = input;
            }

            UnparseTests.helpTestExpr(input, expectSimplify, true);
            UnparseTests.helpTestExpr(input, expectNoSimplify, false);
        }
    }

    @Test
    public void testParseUnparseParseJanino() throws Exception {

        // Process all "*.java" files in the JANINO source tree.
        // Must use the "janino" project directory, because that is pre-Java 5.
        this.find(new File("../janino/src"), new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) return true;

                if (f.getName().endsWith(".java") && f.isFile()) {

                    try {

                        // Parse the source file once.
                        InputStream is = new FileInputStream(f);
                        CompilationUnit cu1 = new Parser(new Scanner(f.toString(), is)).parseCompilationUnit();
                        is.close();

                        // Unparse the compilation unit, then parse again.
                        StringWriter sw = new StringWriter();
                        UnparseVisitor.unparse(cu1, sw);
                        String text = sw.toString();
                        CompilationUnit cu2 = new Parser(
                            new Scanner(f.toString(), new StringReader(text))
                        ).parseCompilationUnit();

                        // Compare the two ASTs.
                        Java.Locatable[] elements1 = this.listSyntaxElements(cu1);
                        Java.Locatable[] elements2 = this.listSyntaxElements(cu2);
                        for (int i = 0;; ++i) {
                            if (i == elements1.length) {
                                if (i == elements2.length) break;
                                fail("Extra element " + elements2[i]);
                            }
                            Locatable locatable1 = elements1[i];

                            if (i == elements2.length) {
                                fail("Element missing: " + locatable1);
                            }
                            Locatable locatable2 = elements2[i];

                            String s1 = locatable1.toString();
                            String s2 = locatable2.toString();
                            if (!s1.equals(s2)) {
                                fail(
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
                return false;
            }

            /**
             * Traverse the given {@link CompilationUnit} and collect a list of all its
             * syntactical elements.
             */
            private Locatable[] listSyntaxElements(CompilationUnit cu) {
                final List locatables = new ArrayList();
                new Traverser() {

                    // Two implementations of "Locatable": "Located" and "AbstractTypeDeclaration".
                    public void traverseLocated(Located l) {
                        locatables.add(l);
                        super.traverseLocated(l);
                    }
                    public void traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd) {
                        locatables.add(atd);
                        super.traverseAbstractTypeDeclaration(atd);
                    }
                }.traverseCompilationUnit(cu);
                return (Locatable[]) locatables.toArray(new Java.Locatable[locatables.size()]);
            }
        });
    }

    /**
     * Invoke {@code fileFilter} for all files and subdirectories in the given
     * {@code directory}. If {@link FileFilter#accept(File)} returns {@code true},
     * recurse with that file/directory.
     */
    private void find(File directory, FileFilter fileFilter) {
        File[] subDirectories = directory.listFiles(fileFilter);
        if (subDirectories == null) fail(directory + " is not a directory");
        for (int i = 0; i < subDirectories.length; ++i) this.find(subDirectories[i], fileFilter);
    }
}
