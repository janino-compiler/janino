
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

package org.codehaus.janino;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractPackageMemberClassDeclaration;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.util.AutoIndentWriter;

/**
 * Unparses (un-compiles) an AST to a {@link Writer}. See {@link #main(String[])} for a usage example.
 */
public
class Unparser {

    private final Visitor.ImportVisitor<Void, RuntimeException>
    importUnparser = new Visitor.ImportVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) {
            Unparser.this.pw.println("import " + Java.join(stid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) {
            Unparser.this.pw.println("import " + Java.join(tiodd.identifiers, ".") + ".*;");
            return null;
        }

        @Override @Nullable public Void
        visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) {
            Unparser.this.pw.println("import static " + Java.join(ssid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) {
            Unparser.this.pw.println("import static " + Java.join(siodd.identifiers, ".") + ".*;");
            return null;
        }
    };

    private final Visitor.TypeDeclarationVisitor<Void, RuntimeException>
    typeDeclarationUnparser = new Visitor.TypeDeclarationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
            Unparser.this.unparseNamedClassDeclaration(lcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration apmcd) {
            Unparser.this.unparseNamedClassDeclaration(apmcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
            Unparser.this.unparseInterfaceDeclaration(pmid);
            return null;
        }

        @Override @Nullable public Void
        visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
            Unparser.this.unparseType(acd.baseType);
            Unparser.this.pw.println(" {");
            Unparser.this.pw.print(AutoIndentWriter.INDENT);
            Unparser.this.unparseClassDeclarationBody(acd);
            Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitEnumConstant(EnumConstant ec) {

            Unparser.this.unparseAnnotations(ec.getAnnotations());

            Unparser.this.pw.append(ec.name);

            if (ec.optionalArguments != null) {
                Unparser.this.unparseFunctionInvocationArguments(ec.optionalArguments);
            }

            if (!Unparser.classDeclarationBodyIsEmpty(ec)) {
                Unparser.this.pw.println(" {");
                Unparser.this.pw.print(AutoIndentWriter.INDENT);
                Unparser.this.unparseClassDeclarationBody(ec);
                Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            }

            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) {
            Unparser.this.unparseEnumDeclaration(pmed);
            return null;
        }

        @Override @Nullable public Void
        visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) {
            Unparser.this.unparseAnnotationTypeDeclaration(matd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) {
            Unparser.this.unparseAnnotationTypeDeclaration(pmatd);
            return null;
        }

        @Override @Nullable public Void
        visitMemberEnumDeclaration(MemberEnumDeclaration med) {
            Unparser.this.unparseEnumDeclaration(med);
            return null;
        }

        @Override @Nullable public Void
        visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
            Unparser.this.unparseInterfaceDeclaration(mid);
            return null;
        }

        @Override @Nullable public Void
        visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
            Unparser.this.unparseNamedClassDeclaration(mcd);
            return null;
        }
    };

    private final Visitor.TypeBodyDeclarationVisitor<Void, RuntimeException>
    typeBodyDeclarationUnparser = new Visitor.TypeBodyDeclarationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitMemberEnumDeclaration(MemberEnumDeclaration med) {
            Unparser.this.unparseEnumDeclaration(med);
            return null;
        }

        @Override @Nullable public Void
        visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
            Unparser.this.unparseNamedClassDeclaration(mcd);
            return null;
        }

        @Override @Nullable public Void
        visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
            Unparser.this.unparseInterfaceDeclaration(mid);
            return null;
        }

        @Override @Nullable public Void
        visitFunctionDeclarator(FunctionDeclarator fd) {
            return fd.accept(new Visitor.FunctionDeclaratorVisitor<Void, RuntimeException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitConstructorDeclarator(ConstructorDeclarator cd) { Unparser.this.unparseConstructorDeclarator(cd); return null; }
                @Override @Nullable public Void visitMethodDeclarator(MethodDeclarator md)           { Unparser.this.unparseMethodDeclarator(md);      return null; }
            });
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitInitializer(Initializer i)            { Unparser.this.unparseInitializer(i);       return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd) { Unparser.this.unparseFieldDeclaration(fd); return null; }
    };

    private final Visitor.BlockStatementVisitor<Void, RuntimeException>
    blockStatementUnparser = new Visitor.BlockStatementVisitor<Void, RuntimeException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitFieldDeclaration(Java.FieldDeclaration fd) { Unparser.this.unparseFieldDeclaration(fd); return null; }
        @Override @Nullable public Void visitInitializer(Java.Initializer i)            { Unparser.this.unparseInitializer(i);       return null; }

        @Override @Nullable public Void
        visitBlock(Java.Block b) {
            if (b.statements.isEmpty()) {
                Unparser.this.pw.print("{}");
                return null;
            }
            Unparser.this.pw.println('{');
            Unparser.this.pw.print(AutoIndentWriter.INDENT);
            Unparser.this.unparseStatements(b.statements);
            Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitBreakStatement(Java.BreakStatement bs) {
            Unparser.this.pw.print("break");
            if (bs.optionalLabel != null) Unparser.this.pw.print(' ' + bs.optionalLabel);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitContinueStatement(Java.ContinueStatement cs) {
            Unparser.this.pw.print("continue");
            if (cs.optionalLabel != null) Unparser.this.pw.print(' ' + cs.optionalLabel);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitAssertStatement(Java.AssertStatement as) {

            Unparser.this.pw.print("assert ");
            Unparser.this.unparseAtom(as.expression1);

            Rvalue oe2 = as.optionalExpression2;
            if (oe2 != null) {
                Unparser.this.pw.print(" : ");
                Unparser.this.unparseAtom(oe2);
            }
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitDoStatement(Java.DoStatement ds) {
            Unparser.this.pw.print("do ");
            Unparser.this.unparseBlockStatement(ds.body);
            Unparser.this.pw.print("while (");
            Unparser.this.unparseAtom(ds.condition);
            Unparser.this.pw.print(");");
            return null;
        }

        @Override @Nullable public Void
        visitEmptyStatement(Java.EmptyStatement es) {
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitExpressionStatement(Java.ExpressionStatement es) {
            Unparser.this.unparseAtom(es.rvalue);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitForStatement(Java.ForStatement fs) {
            Unparser.this.pw.print("for (");
            if (fs.optionalInit != null) {
                Unparser.this.unparseBlockStatement(fs.optionalInit);
            } else {
                Unparser.this.pw.print(';');
            }

            Rvalue oc = fs.optionalCondition;
            if (oc != null) {
                Unparser.this.pw.print(' ');
                Unparser.this.unparseAtom(oc);
            }

            Unparser.this.pw.print(';');

            Rvalue[] ou = fs.optionalUpdate;
            if (ou != null) {
                Unparser.this.pw.print(' ');
                for (int i = 0; i < ou.length; ++i) {
                    if (i > 0) Unparser.this.pw.print(", ");
                    Unparser.this.unparseAtom(ou[i]);
                }
            }

            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(fs.body);
            return null;
        }

        @Override @Nullable public Void
        visitForEachStatement(Java.ForEachStatement fes) {
            Unparser.this.pw.print("for (");
            Unparser.this.unparseFormalParameter(fes.currentElement, false);
            Unparser.this.pw.print(" : ");
            Unparser.this.unparseAtom(fes.expression);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(fes.body);
            return null;
        }

        @Override @Nullable public Void
        visitIfStatement(Java.IfStatement is) {
            Unparser.this.pw.print("if (");
            Unparser.this.unparseAtom(is.condition);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(is.thenStatement);

            BlockStatement oes = is.optionalElseStatement;
            if (oes != null) {
                Unparser.this.pw.println(" else");
                Unparser.this.unparseBlockStatement(oes);
            }

            return null;
        }

        @Override @Nullable public Void
        visitLabeledStatement(Java.LabeledStatement ls) {
            Unparser.this.pw.println(ls.label + ':');
            Unparser.this.unparseBlockStatement(ls.body);
            return null;
        }

        @Override @Nullable public Void
        visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
            Unparser.this.unparseTypeDeclaration(lcds.lcd);
            return null;
        }

        @Override @Nullable public Void
        visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
            Unparser.this.unparseAnnotations(lvds.modifiers.annotations);
            Unparser.this.unparseModifiers(lvds.modifiers.accessFlags);
            Unparser.this.unparseType(lvds.type);
            Unparser.this.pw.print(' ');
            Unparser.this.pw.print(AutoIndentWriter.TABULATOR);
            Unparser.this.unparseVariableDeclarator(lvds.variableDeclarators[0]);
            for (int i = 1; i < lvds.variableDeclarators.length; ++i) {
                Unparser.this.pw.print(", ");
                Unparser.this.unparseVariableDeclarator(lvds.variableDeclarators[i]);
            }
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitReturnStatement(Java.ReturnStatement rs) {

            Unparser.this.pw.print("return");

            Rvalue orv = rs.optionalReturnValue;
            if (orv != null) {
                Unparser.this.pw.print(' ');
                Unparser.this.unparseAtom(orv);
            }

            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitSwitchStatement(Java.SwitchStatement ss) {
            Unparser.this.pw.print("switch (");
            Unparser.this.unparseAtom(ss.condition);
            Unparser.this.pw.println(") {");
            for (Java.SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
                Unparser.this.pw.print(AutoIndentWriter.UNINDENT);
                try {
                    for (Java.Rvalue rv : sbsg.caseLabels) {
                        Unparser.this.pw.print("case ");
                        Unparser.this.unparseAtom(rv);
                        Unparser.this.pw.println(':');
                    }
                    if (sbsg.hasDefaultLabel) Unparser.this.pw.println("default:");
                } finally {
                    Unparser.this.pw.print(AutoIndentWriter.INDENT);
                }
                for (Java.BlockStatement bs : sbsg.blockStatements) {
                    Unparser.this.unparseBlockStatement(bs);
                    Unparser.this.pw.println();
                }
            }
            Unparser.this.pw.print('}');
            return null;
        }

        @Override @Nullable public Void
        visitSynchronizedStatement(Java.SynchronizedStatement ss) {
            Unparser.this.pw.print("synchronized (");
            Unparser.this.unparseAtom(ss.expression);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(ss.body);
            return null;
        }

        @Override @Nullable public Void
        visitThrowStatement(Java.ThrowStatement ts) {
            Unparser.this.pw.print("throw ");
            Unparser.this.unparseAtom(ts.expression);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitTryStatement(Java.TryStatement ts) {
            Unparser.this.pw.print("try ");
            Unparser.this.unparseBlockStatement(ts.body);
            for (Java.CatchClause cc : ts.catchClauses) {
                Unparser.this.pw.print(" catch (");
                Unparser.this.unparseFormalParameter(cc.caughtException, false);
                Unparser.this.pw.print(") ");
                Unparser.this.unparseBlockStatement(cc.body);
            }

            Block of = ts.optionalFinally;
            if (of != null) {
                Unparser.this.pw.print(" finally ");
                Unparser.this.unparseBlockStatement(of);
            }

            return null;
        }

        @Override @Nullable public Void
        visitWhileStatement(Java.WhileStatement ws) {
            Unparser.this.pw.print("while (");
            Unparser.this.unparseAtom(ws.condition);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(ws.body);
            return null;
        }

        @Override @Nullable public Void
        visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
            Unparser.this.pw.print("this");
            Unparser.this.unparseFunctionInvocationArguments(aci.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
            if (sci.optionalQualification != null) {
                Unparser.this.unparseLhs(sci.optionalQualification, ".");
                Unparser.this.pw.print('.');
            }
            Unparser.this.pw.print("super");
            Unparser.this.unparseFunctionInvocationArguments(sci.arguments);
            return null;
        }
    };

    private final Visitor.AtomVisitor<Void, RuntimeException>
    atomUnparser = new Visitor.AtomVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitType(Type t) {
            t.accept(new Visitor.TypeVisitor<Void, RuntimeException>() {

                @Override @Nullable public Void
                visitArrayType(Java.ArrayType at) {
                    Unparser.this.unparseType(at.componentType);
                    Unparser.this.pw.print("[]");
                    return null;
                }

                @Override @Nullable public Void
                visitPrimitiveType(Java.PrimitiveType bt) {
                    Unparser.this.pw.print(bt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitReferenceType(Java.ReferenceType rt) {
                    Unparser.this.pw.print(rt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitRvalueMemberType(Java.RvalueMemberType rmt) {
                    Unparser.this.pw.print(rmt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitSimpleType(Java.SimpleType st) {
                    Unparser.this.pw.print(st.toString());
                    return null;
                }
            });
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Java.Package p) { Unparser.this.pw.print(p.toString()); return null; }

        @Override @Nullable public Void
        visitRvalue(Java.Rvalue rv) {
            rv.accept(new Visitor.RvalueVisitor<Void, RuntimeException>() {

                @Override @Nullable public Void
                visitLvalue(Lvalue lv) {
                    lv.accept(new Visitor.LvalueVisitor<Void, RuntimeException>() {

                        @Override @Nullable public Void
                        visitAmbiguousName(Java.AmbiguousName an) {
                            Unparser.this.pw.print(an.toString());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
                            Unparser.this.unparseLhs(aae.lhs, "[ ]");
                            Unparser.this.pw.print('[');
                            Unparser.this.unparseAtom(aae.index);
                            Unparser.this.pw.print(']');
                            return null;
                        }

                        @Override @Nullable public Void
                        visitFieldAccess(Java.FieldAccess fa) {
                            Unparser.this.unparseLhs(fa.lhs, ".");
                            Unparser.this.pw.print('.' + fa.field.getName());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitFieldAccessExpression(Java.FieldAccessExpression fae) {
                            Unparser.this.unparseLhs(fae.lhs, ".");
                            Unparser.this.pw.print('.' + fae.fieldName);
                            return null;
                        }

                        @Override @Nullable public Void
                        visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {
                            if (scfae.optionalQualification != null) {
                                Unparser.this.unparseType(scfae.optionalQualification);
                                Unparser.this.pw.print(".super." + scfae.fieldName);
                            } else
                            {
                                Unparser.this.pw.print("super." + scfae.fieldName);
                            }
                            return null;
                        }

                        @Override @Nullable public Void
                        visitLocalVariableAccess(Java.LocalVariableAccess lva) {
                            Unparser.this.pw.print(lva.toString());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
                            Unparser.this.pw.print('(');
                            Unparser.this.unparseAtom(pe.value);
                            Unparser.this.pw.print(')');
                            return null;
                        }
                    });
                    return null;
                }

                @Override @Nullable public Void
                visitMethodInvocation(Java.MethodInvocation mi) {
                    if (mi.optionalTarget != null) {
                        Unparser.this.unparseLhs(mi.optionalTarget, ".");
                        Unparser.this.pw.print('.');
                    }
                    Unparser.this.pw.print(mi.methodName);
                    Unparser.this.unparseFunctionInvocationArguments(mi.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitNewClassInstance(Java.NewClassInstance nci) {
                    if (nci.optionalQualification != null) {
                        Unparser.this.unparseLhs(nci.optionalQualification, ".");
                        Unparser.this.pw.print('.');
                    }
                    assert nci.type != null;
                    Unparser.this.pw.print("new " + nci.type.toString());
                    Unparser.this.unparseFunctionInvocationArguments(nci.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitAssignment(Java.Assignment a) {
                    Unparser.this.unparseLhs(a.lhs, a.operator);
                    Unparser.this.pw.print(' ' + a.operator + ' ');
                    Unparser.this.unparseRhs(a.rhs, a.operator);
                    return null;
                }

                @Override @Nullable public Void
                visitArrayLength(Java.ArrayLength al) {
                    Unparser.this.unparseLhs(al.lhs, ".");
                    Unparser.this.pw.print(".length");
                    return null;
                }

                @Override @Nullable public Void
                visitBinaryOperation(Java.BinaryOperation bo) {
                    Unparser.this.unparseLhs(bo.lhs, bo.op);
                    Unparser.this.pw.print(' ' + bo.op + ' ');
                    Unparser.this.unparseRhs(bo.rhs, bo.op);
                    return null;
                }

                @Override @Nullable public Void
                visitCast(Java.Cast c) {
                    Unparser.this.pw.print('(');
                    Unparser.this.unparseType(c.targetType);
                    Unparser.this.pw.print(") ");
                    Unparser.this.unparseRhs(c.value, "cast");
                    return null;
                }

                @Override @Nullable public Void
                visitClassLiteral(Java.ClassLiteral cl) {
                    Unparser.this.unparseType(cl.type);
                    Unparser.this.pw.print(".class");
                    return null;
                }

                @Override @Nullable public Void
                visitConditionalExpression(Java.ConditionalExpression ce) {
                    Unparser.this.unparseLhs(ce.lhs, "?:");
                    Unparser.this.pw.print(" ? ");
                    Unparser.this.unparseLhs(ce.mhs, "?:");
                    Unparser.this.pw.print(" : ");
                    Unparser.this.unparseRhs(ce.rhs, "?:");
                    return null;
                }

                @Override @Nullable public Void
                visitCrement(Java.Crement c) {
                    if (c.pre) {
                        Unparser.this.pw.print(c.operator);
                        Unparser.this.unparseUnaryOperation(c.operand, c.operator + "x");
                    } else
                    {
                        Unparser.this.unparseUnaryOperation(c.operand, "x" + c.operator);
                        Unparser.this.pw.print(c.operator);
                    }
                    return null;
                }

                @Override @Nullable public Void
                visitInstanceof(Java.Instanceof io) {
                    Unparser.this.unparseLhs(io.lhs, "instanceof");
                    Unparser.this.pw.print(" instanceof ");
                    Unparser.this.unparseType(io.rhs);
                    return null;
                }

                // SUPPRESS CHECKSTYLE LineLength:8
                @Override @Nullable public Void visitIntegerLiteral(Java.IntegerLiteral il)              { Unparser.this.pw.print(il.value);             return null; }
                @Override @Nullable public Void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) { Unparser.this.pw.print(fpl.value);            return null; }
                @Override @Nullable public Void visitBooleanLiteral(Java.BooleanLiteral bl)              { Unparser.this.pw.print(bl.value);             return null; }
                @Override @Nullable public Void visitCharacterLiteral(Java.CharacterLiteral cl)          { Unparser.this.pw.print(cl.value);             return null; }
                @Override @Nullable public Void visitStringLiteral(Java.StringLiteral sl)                { Unparser.this.pw.print(sl.value);             return null; }
                @Override @Nullable public Void visitNullLiteral(Java.NullLiteral nl)                    { Unparser.this.pw.print(nl.value);             return null; }
                @Override @Nullable public Void visitSimpleConstant(Java.SimpleConstant sl)              { Unparser.this.pw.print("[" + sl.value + ']'); return null; }

                @Override @Nullable public Void
                visitNewArray(Java.NewArray na) {
                    Unparser.this.pw.print("new ");
                    Unparser.this.unparseType(na.type);
                    for (Rvalue dimExpr : na.dimExprs) {
                        Unparser.this.pw.print('[');
                        Unparser.this.unparseAtom(dimExpr);
                        Unparser.this.pw.print(']');
                    }
                    for (int i = 0; i < na.dims; ++i) {
                        Unparser.this.pw.print("[]");
                    }
                    return null;
                }

                @Override @Nullable public Void
                visitNewInitializedArray(Java.NewInitializedArray nai) {

                    Unparser.this.pw.print("new ");

                    ArrayType at = nai.arrayType;
                    assert at != null;
                    Unparser.this.unparseType(at);

                    Unparser.this.pw.print(" ");

                    Unparser.this.unparseArrayInitializerOrRvalue(nai.arrayInitializer);

                    return null;
                }

                @Override @Nullable public Void
                visitParameterAccess(Java.ParameterAccess pa) {
                    Unparser.this.pw.print(pa.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
                    Unparser.this.unparseType(qtr.qualification);
                    Unparser.this.pw.print(".this");
                    return null;
                }

                @Override @Nullable public Void
                visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
                    Unparser.this.pw.print("super." + smi.methodName);
                    Unparser.this.unparseFunctionInvocationArguments(smi.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitThisReference(Java.ThisReference tr) { Unparser.this.pw.print("this"); return null; }

                @Override @Nullable public Void
                visitUnaryOperation(Java.UnaryOperation uo) {
                    Unparser.this.pw.print(uo.operator);
                    Unparser.this.unparseUnaryOperation(uo.operand, uo.operator + "x");
                    return null;
                }

                @Override @Nullable public Void
                visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
                    if (naci.optionalQualification != null) {
                        Unparser.this.unparseLhs(naci.optionalQualification, ".");
                        Unparser.this.pw.print('.');
                    }
                    Unparser.this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
                    for (int i = 0; i < naci.arguments.length; ++i) {
                        if (i > 0) Unparser.this.pw.print(", ");
                        Unparser.this.unparseAtom(naci.arguments[i]);
                    }
                    Unparser.this.pw.println(") {");
                    Unparser.this.pw.print(AutoIndentWriter.INDENT);
                    Unparser.this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
                    Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
                    return null;
                }
            });
            return null;
        }
    };

    private final Visitor.ElementValueVisitor<Void, RuntimeException>
    elementValueUnparser = new Visitor.ElementValueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws RuntimeException {
            rv.accept(Unparser.this.atomUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitAnnotation(Annotation a) {
            a.accept(Unparser.this.annotationUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) {
            if (evai.elementValues.length == 0) {
                Unparser.this.pw.append("{}");
                return null;
            }

            Unparser.this.pw.append("{ ");
            evai.elementValues[0].accept(this);
            for (int i = 1; i < evai.elementValues.length; i++) {
                Unparser.this.pw.append(", ");
                evai.elementValues[i].accept(this);
            }
            Unparser.this.pw.append(" }");
            return null;
        }
    };

    private final AnnotationVisitor<Void, RuntimeException>
    annotationUnparser = new AnnotationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitMarkerAnnotation(Java.MarkerAnnotation ma) {
            Unparser.this.pw.append('@').append(ma.type.toString()).append(' ');
            return null;
        }

        @Override @Nullable public Void
        visitNormalAnnotation(Java.NormalAnnotation na) {
            Unparser.this.pw.append('@').append(na.type.toString()).append('(');
            for (int i = 0; i < na.elementValuePairs.length; i++) {
                Java.ElementValuePair evp = na.elementValuePairs[i];

                if (i > 0) Unparser.this.pw.print(", ");

                Unparser.this.pw.append(evp.identifier).append(" = ");

                evp.elementValue.accept(Unparser.this.elementValueUnparser);
            }
            Unparser.this.pw.append(") ");
            return null;
        }

        @Override @Nullable public Void
        visitSingleElementAnnotation(Java.SingleElementAnnotation sea) {
            Unparser.this.pw.append('@').append(sea.type.toString()).append('(');
            sea.elementValue.accept(Unparser.this.elementValueUnparser);
            Unparser.this.pw.append(") ");
            return null;
        }
    };

    private void
    unparseInitializer(Java.Initializer i) {
        if (i.statiC) Unparser.this.pw.print("static ");
        Unparser.this.unparseBlockStatement(i.block);
    }

    private void
    unparseFieldDeclaration(Java.FieldDeclaration fd) {
        Unparser.this.unparseDocComment(fd);
        Unparser.this.unparseAnnotations(fd.modifiers.annotations);
        Unparser.this.unparseModifiers(fd.modifiers.accessFlags);
        Unparser.this.unparseType(fd.type);
        Unparser.this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) Unparser.this.pw.print(", ");
            Unparser.this.unparseVariableDeclarator(fd.variableDeclarators[i]);
        }
        Unparser.this.pw.print(';');
    }

    /**
     * Where the {@code visit...()} methods print their text. Noice that this {@link PrintWriter} does not print to
     * the output directly, but through an {@link AutoIndentWriter}.
     */
    protected final PrintWriter pw;

    /**
     * Testing of parsing/unparsing.
     * <p>
     * Reads compilation units from the files named on the command line
     * and unparses them to {@link System#out}.
     */
    public static void
    main(String[] args) throws Exception {

        Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
        for (String fileName : args) {

            // Parse each compilation unit.
            FileReader           r = new FileReader(fileName);
            Java.CompilationUnit cu;
            try {
                cu = new Parser(new Scanner(fileName, r)).parseCompilationUnit();
            } finally {
                r.close();
            }

            // Unparse each compilation unit.
            Unparser.unparse(cu, w);
        }
        w.flush();
    }

    /** Unparses the given {@link Java.CompilationUnit} to the given {@link Writer}. */
    public static void
    unparse(Java.CompilationUnit cu, Writer w) {
        Unparser uv = new Unparser(w);
        uv.unparseCompilationUnit(cu);
        uv.close();
    }

    public
    Unparser(Writer w) {
        this.pw  = new PrintWriter(new AutoIndentWriter(w), true);
    }

    /** Flushes all generated code. */
    public void
    close() { this.pw.flush(); }

    /** @param cu The compilation unit to unparse */
    public void
    unparseCompilationUnit(Java.CompilationUnit cu) {

        PackageDeclaration opd = cu.optionalPackageDeclaration;
        if (opd != null) {
            this.pw.println();
            this.pw.println("package " + opd.packageName + ';');
        }

        if (!cu.importDeclarations.isEmpty()) {
            this.pw.println();
            for (Java.CompilationUnit.ImportDeclaration id : cu.importDeclarations) {
                id.accept(this.importUnparser);
            }
        }

        for (Java.PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            this.pw.println();
            this.unparseTypeDeclaration(pmtd);
            this.pw.println();
        }
    }

    public void
    unparseImportDeclaration(ImportDeclaration id) { id.accept(this.importUnparser); }

    private void
    unparseConstructorDeclarator(Java.ConstructorDeclarator cd) {

        this.unparseDocComment(cd);
        this.unparseAnnotations(cd.modifiers.annotations);
        this.unparseModifiers(cd.modifiers.accessFlags);
        Java.AbstractClassDeclaration declaringClass = cd.getDeclaringClass();
        this.pw.print(
            declaringClass instanceof Java.NamedClassDeclaration
            ? ((Java.NamedClassDeclaration) declaringClass).name
            : "UNNAMED"
        );
        this.unparseFunctionDeclaratorRest(cd);

        List<? extends BlockStatement> oss = cd.optionalStatements;
        if (oss == null) {
            this.pw.print(';');
            return;
        }

        this.pw.print(' ');

        ConstructorInvocation oci = cd.optionalConstructorInvocation;
        if (oci != null) {
            this.pw.println('{');
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseBlockStatement(oci);
            this.pw.println(';');

            if (!oss.isEmpty()) {
                this.pw.println();
                this.unparseStatements(oss);
            }
            this.pw.print(AutoIndentWriter.UNINDENT + "}");
        } else
        if (oss.isEmpty()) {
            this.pw.print("{}");
        } else
        {
            this.pw.println('{');
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseStatements(oss);
            this.pw.print(AutoIndentWriter.UNINDENT + "}");
        }
    }

    private void
    unparseMethodDeclarator(Java.MethodDeclarator md) {

        // For methods declared as members of an *interface*, the default access flags are "public abstract".
        boolean declaringTypeIsInterface = md.getDeclaringType() instanceof Java.InterfaceDeclaration;

        final List<? extends BlockStatement> oss = md.optionalStatements;

        this.unparseDocComment(md);
        this.unparseAnnotations(md.modifiers.annotations);
        this.unparseModifiers(
            md.modifiers.remove(declaringTypeIsInterface ? Mod.PUBLIC | Mod.ABSTRACT : 0).accessFlags
        );
        this.unparseType(md.type);
        this.pw.print(' ' + md.name);
        this.unparseFunctionDeclaratorRest(md);
        if (oss == null) {
            this.pw.print(';');
        } else
        if (oss.isEmpty()) {
            this.pw.print(" {}");
        } else
        {
            this.pw.println(" {");
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseStatements(oss);
            this.pw.print(AutoIndentWriter.UNINDENT);
            this.pw.print('}');
        }
    }

    private void
    unparseStatements(List<? extends Java.BlockStatement> statements) {

        int state = -1;
        for (Java.BlockStatement bs : statements) {
            int x  = (
                bs instanceof Java.Block                             ? 1 :
                bs instanceof Java.LocalClassDeclarationStatement    ? 2 :
                bs instanceof Java.LocalVariableDeclarationStatement ? 3 :
                bs instanceof Java.SynchronizedStatement             ? 4 :
                99
            );
            if (state != -1 && state != x) this.pw.println(AutoIndentWriter.CLEAR_TABULATORS);
            state = x;

            this.unparseBlockStatement(bs);
            this.pw.println();
        }
    }

    private void
    unparseVariableDeclarator(Java.VariableDeclarator vd) {
        this.pw.print(vd.name);
        for (int i = 0; i < vd.brackets; ++i) this.pw.print("[]");

        ArrayInitializerOrRvalue oi = vd.optionalInitializer;
        if (oi != null) {
            this.pw.print(" = ");
            this.unparseArrayInitializerOrRvalue(oi);
        }
    }

    private void
    unparseFormalParameter(Java.FunctionDeclarator.FormalParameter fp, boolean hasEllipsis) {
        if (fp.finaL) this.pw.print("final ");
        this.unparseType(fp.type);
        if (hasEllipsis) this.pw.write("...");
        this.pw.print(" " + AutoIndentWriter.TABULATOR + fp.name);
    }

    // Helpers

    public void
    unparseBlockStatement(Java.BlockStatement bs) { bs.accept(this.blockStatementUnparser); }

    public void
    unparseTypeDeclaration(Java.TypeDeclaration td) { td.accept(this.typeDeclarationUnparser); }

    public void
    unparseType(Java.Type t) { t.accept(this.atomUnparser); }

    public void
    unparseAtom(Java.Atom a) { a.accept(this.atomUnparser); }

    /**
     * Iff the <code>operand</code> is unnatural for the <code>unaryOperator</code>, enclose the
     * <code>operand</code> in parentheses. Example: "a+b" is an unnatural operand for unary "!x".
     *
     * @param unaryOperator ++x --x +x -x ~x !x x++ x--
     */
    private void
    unparseUnaryOperation(Java.Rvalue operand, String unaryOperator) {
        int cmp = Unparser.comparePrecedence(unaryOperator, operand);
        this.unparse(operand, cmp < 0);
    }

    /**
     * Iff the <code>lhs</code> is unnatural for the <code>binaryOperator</code>, enclose the
     * <code>lhs</code> in parentheses. Example: "a+b" is an unnatural lhs for operator "*".
     *
     * @param binaryOperator = +=... ?: || && | ^ & == != < > <= >= instanceof << >> >>> + - * / % cast
     */
    private void
    unparseLhs(Java.Atom lhs, String binaryOperator) {
        int cmp = Unparser.comparePrecedence(binaryOperator, lhs);
        this.unparse(lhs, cmp < 0 || (cmp == 0 && Unparser.isLeftAssociate(binaryOperator)));
    }


    /**
     * Iff the <code>rhs</code> is unnatural for the <code>binaryOperator</code>, enclose the
     * <code>rhs</code> in parentheses. Example: "a+b" is an unnatural rhs for operator "*".
     */
    private void
    unparseRhs(Java.Rvalue rhs, String binaryOperator) {
        int cmp = Unparser.comparePrecedence(binaryOperator, rhs);
        this.unparse(rhs, cmp < 0 || (cmp == 0 && Unparser.isRightAssociate(binaryOperator)));
    }

    private void
    unparse(Java.Atom operand, boolean natural) {
        if (!natural) this.pw.print("((( ");
        this.unparseAtom(operand);
        if (!natural) this.pw.print(" )))");
    }

    /**
     * Return true iff operator is right associative e.g. <code>a = b = c</code> evaluates as
     * <code>a = (b = c)</code>.
     *
     * @return Return true iff operator is right associative
     */
    private static boolean
    isRightAssociate(String op) { return Unparser.RIGHT_ASSOCIATIVE_OPERATORS.contains(op); }

    /**
     * Return true iff operator is left associative e.g. <code>a - b - c</code> evaluates as
     * <code>(a - b) - c</code>.
     *
     * @return Return true iff operator is left associative
     */
    private static boolean
    isLeftAssociate(String op) { return Unparser.LEFT_ASSOCIATIVE_OPERATORS.contains(op); }

    /**
     * Returns a value
     * <ul>
     *   <li>&lt; 0 iff the <code>operator</code> has lower precedence than the <code>operand</code>
     *   <li>==; 0 iff the <code>operator</code> has equal precedence than the <code>operand</code>
     *   <li>&gt; 0 iff the <code>operator</code> has higher precedence than the <code>operand</code>
     * </ul>
     */
    private static int
    comparePrecedence(String operator, Java.Atom operand) {
        if (operand instanceof Java.BinaryOperation) {
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(((Java.BinaryOperation) operand).op)
            );
        } else
        if (operand instanceof Java.UnaryOperation) {
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(((Java.UnaryOperation) operand).operator + "x")
            );
        } else
        if (operand instanceof Java.ConditionalExpression) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("?:");
        } else
        if (operand instanceof Java.Instanceof) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("instanceof");
        } else
        if (operand instanceof Java.Cast) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("cast");
        } else
        if (operand instanceof Java.MethodInvocation || operand instanceof Java.FieldAccess) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence(".");
        } else
        if (operand instanceof Java.NewArray) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("new");
        } else
        if (operand instanceof Java.Crement) {
            Java.Crement c = (Java.Crement) operand;
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(c.pre ? c.operator + "x" : "x" + c.operator)
            );
        } else
        {
            // All other rvalues (e.g. literal) have higher precedence than any operator.
            return -1;
        }
    }

    private static int
    getOperatorPrecedence(String operator) {
        return ((Integer) Unparser.OPERATOR_PRECEDENCE.get(operator)).intValue();
    }

    private static final Set<String>          LEFT_ASSOCIATIVE_OPERATORS  = new HashSet<String>();
    private static final Set<String>          RIGHT_ASSOCIATIVE_OPERATORS = new HashSet<String>();
    private static final Set<String>          UNARY_OPERATORS             = new HashSet<String>();
    private static final Map<String, Integer> OPERATOR_PRECEDENCE         = new HashMap<String, Integer>();
    static {
        Object[] ops = {
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", ">>>=",
                                                        "&=", "^=", "|=", // SUPPRESS CHECKSTYLE WrapAndIndent
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "?:",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "||",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "&&",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "|",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "^",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "&",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "==", "!=",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "<", ">", "<=", ">=", "instanceof",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "<<", ">>", ">>>",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "+", "-",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "*", "/", "%",
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "cast",
            Unparser.UNARY_OPERATORS,             "++x", "--x", "+x", "-x", "~x", "!x",
            Unparser.UNARY_OPERATORS,             "x++", "x--",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "new",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  ".", "[ ]",
        };
        int precedence = 0;
        LOOP1: for (int i = 0;;) {
            @SuppressWarnings("unchecked") final Set<String> s = (Set<String>) ops[i++];

            precedence++;
            for (;;) {
                if (i == ops.length) break LOOP1;
                if (!(ops[i] instanceof String)) break;
                String op = (String) ops[i++];
                s.add(op);
                Unparser.OPERATOR_PRECEDENCE.put(op, precedence);
            }
        }
    }

    private void
    unparseNamedClassDeclaration(Java.NamedClassDeclaration ncd) {
        this.unparseDocComment(ncd);
        this.unparseAnnotations(ncd.getAnnotations());
        this.unparseModifiers(ncd.getModifierFlags());
        this.pw.print("class " + ncd.name);

        Type oet = ncd.optionalExtendedType;
        if (oet != null) {
            this.pw.print(" extends ");
            this.unparseType(oet);
        }

        if (ncd.implementedTypes.length > 0) this.pw.print(" implements " + Java.join(ncd.implementedTypes, ", "));
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseClassDeclarationBody(ncd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    private void
    unparseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) {
        if (aiorv instanceof Java.Rvalue) {
            this.unparseAtom((Java.Rvalue) aiorv);
        } else
        if (aiorv instanceof Java.ArrayInitializer) {
            Java.ArrayInitializer ai = (Java.ArrayInitializer) aiorv;
            if (ai.values.length == 0) {
                this.pw.print("{}");
            } else
            {
                this.pw.print("{ ");
                this.unparseArrayInitializerOrRvalue(ai.values[0]);
                for (int i = 1; i < ai.values.length; ++i) {
                    this.pw.print(", ");
                    this.unparseArrayInitializerOrRvalue(ai.values[i]);
                }
                this.pw.print(" }");
            }
        } else
        {
            throw new JaninoRuntimeException(
                "Unexpected array initializer or rvalue class "
                + aiorv.getClass().getName()
            );
        }
    }

    // Multi-line!
    private void
    unparseClassDeclarationBody(Java.AbstractClassDeclaration cd) {
        for (Java.ConstructorDeclarator ctord : cd.constructors) {
            this.pw.println();
            ctord.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        this.unparseTypeDeclarationBody(cd);
        for (Java.BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            this.pw.println();
            vdoi.accept(this.blockStatementUnparser);
            this.pw.println();
        }
    }

    /**
     * @return Whether {@link #unparseClassDeclarationBody(Java.AbstractClassDeclaration)} will produce <em>no</em>
     *         output
     */
    private static boolean
    classDeclarationBodyIsEmpty(Java.AbstractClassDeclaration cd) {
        return (
            cd.constructors.isEmpty()
            && cd.getMethodDeclarations().isEmpty()
            && cd.getMemberTypeDeclarations().isEmpty()
            && cd.variableDeclaratorsAndInitializers.isEmpty()
        );
    }
    private void
    unparseInterfaceDeclaration(Java.InterfaceDeclaration id) {
        this.unparseDocComment(id);
        this.unparseAnnotations(id.getAnnotations());
        this.unparseModifiers(id.getModifierFlags());
        this.pw.print("interface ");
        this.pw.print(id.name);
        if (id.extendedTypes.length > 0) this.pw.print(" extends " + Java.join(id.extendedTypes, ", "));
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(id);
        for (Java.TypeBodyDeclaration cnstd : id.constantDeclarations) {
            cnstd.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    // Multi-line!
    private void
    unparseTypeDeclarationBody(Java.TypeDeclaration td) {
        for (Java.MethodDeclarator md : td.getMethodDeclarations()) {
            this.pw.println();
            md.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        for (Java.MemberTypeDeclaration mtd : td.getMemberTypeDeclarations()) {
            this.pw.println();
            ((Java.TypeBodyDeclaration) mtd).accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
    }
    private void
    unparseFunctionDeclaratorRest(Java.FunctionDeclarator fd) {
        boolean big = fd.formalParameters.parameters.length >= 4;
        this.pw.print('(');
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.INDENT); }
        for (int i = 0; i < fd.formalParameters.parameters.length; ++i) {
            if (i > 0) {
                if (big) {
                    this.pw.println(',');
                } else
                {
                    this.pw.print(", ");
                }
            }
            this.unparseFormalParameter(
                fd.formalParameters.parameters[i],
                i == fd.formalParameters.parameters.length - 1 && fd.formalParameters.variableArity
            );
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
        if (fd.thrownExceptions.length > 0) this.pw.print(" throws " + Java.join(fd.thrownExceptions, ", "));
    }

    private void
    unparseDocComment(Java.DocCommentable dc) {
        String optionalDocComment = dc.getDocComment();
        if (optionalDocComment != null) {
            this.pw.print("/**");
            BufferedReader br = new BufferedReader(new StringReader(optionalDocComment));
            for (;;) {
                String line;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new JaninoRuntimeException(null, e);
                }
                if (line == null) break;
                this.pw.println(line);
                this.pw.print(" *");
            }
            this.pw.println("/");
        }
    }

    private void
    unparseAnnotations(Java.Annotation[] annotations) {
        for (Annotation a : annotations) a.accept(this.annotationUnparser);
    }

    private void
    unparseModifiers(short modifiers) {
        if (modifiers != 0) {
            this.pw.print(Mod.shortToString(modifiers) + ' ');
        }
    }

    private void
    unparseFunctionInvocationArguments(Java.Rvalue[] arguments) {
        boolean big = arguments.length >= 5;
        this.pw.print('(');
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.INDENT); }
        for (int i = 0; i < arguments.length; ++i) {
            if (i > 0) {
                if (big) {
                    this.pw.println(',');
                } else
                {
                    this.pw.print(", ");
                }
            }
            this.unparseAtom(arguments[i]);
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
    }

    private void
    unparseEnumDeclaration(Java.EnumDeclaration ed) {

        this.unparseDocComment(ed);
        this.unparseAnnotations(ed.getAnnotations());
        this.unparseModifiers(ed.getModifierFlags());
        this.pw.print("enum " + ed.getName());

        Type[] its = ed.getImplementedTypes();
        if (its.length > 0) this.pw.print(" implements " + Java.join(its, ", "));

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        Iterator<EnumConstant> it = ed.getConstants().iterator();
        if (it.hasNext()) {
            for (;;) {
                this.typeDeclarationUnparser.visitEnumConstant(it.next());

                if (!it.hasNext()) break;
                this.pw.print(", ");
            }
        }
        this.pw.println();
        this.pw.println(';');
        this.unparseClassDeclarationBody((Java.AbstractClassDeclaration) ed);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    private void
    unparseAnnotationTypeDeclaration(Java.AnnotationTypeDeclaration atd) {
        this.unparseDocComment(atd);
        this.unparseAnnotations(atd.getAnnotations());
        this.unparseModifiers(atd.getModifierFlags());
        this.pw.print("@interface ");
        this.pw.print(atd.getName());

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(atd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }
}
