
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
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ElementValue;
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
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.util.AutoIndentWriter;

/**
 * A visitor that unparses (un-compiles) an AST to a {@link Writer}. See {@link #main(String[])} for a usage example.
 */
@SuppressWarnings({ "rawtypes", "unchecked" }) public
class UnparseVisitor implements Visitor.ComprehensiveVisitor<Void, RuntimeException> {

    private final Visitor.ImportVisitor<Void, RuntimeException>
    importUnparser = new Visitor.ImportVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) {
            UnparseVisitor.this.pw.println("import " + Java.join(stid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) {
            UnparseVisitor.this.pw.println("import " + Java.join(tiodd.identifiers, ".") + ".*;");
            return null;
        }

        @Override @Nullable public Void
        visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) {
            UnparseVisitor.this.pw.println("import static " + Java.join(ssid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) {
            UnparseVisitor.this.pw.println("import static " + Java.join(siodd.identifiers, ".") + ".*;");
            return null;
        }
    };
    private final Visitor.TypeDeclarationVisitor<Void, RuntimeException>
    typeDeclarationUnparser = new Visitor.TypeDeclarationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
            UnparseVisitor.this.unparseNamedClassDeclaration(lcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration apmcd) {
            UnparseVisitor.this.unparseNamedClassDeclaration(apmcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
            UnparseVisitor.this.unparseInterfaceDeclaration(pmid);
            return null;
        }

        @Override @Nullable public Void
        visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
            UnparseVisitor.this.unparseType(acd.baseType);
            UnparseVisitor.this.pw.println(" {");
            UnparseVisitor.this.pw.print(AutoIndentWriter.INDENT);
            UnparseVisitor.this.unparseClassDeclarationBody(acd);
            UnparseVisitor.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitEnumConstant(EnumConstant ec) {

            UnparseVisitor.this.unparseAnnotations(ec.getAnnotations());

            UnparseVisitor.this.pw.append(ec.name);

            if (ec.optionalArguments != null) {
                UnparseVisitor.this.unparseFunctionInvocationArguments(ec.optionalArguments);
            }

            if (!UnparseVisitor.classDeclarationBodyIsEmpty(ec)) {
                UnparseVisitor.this.pw.println(" {");
                UnparseVisitor.this.pw.print(AutoIndentWriter.INDENT);
                UnparseVisitor.this.unparseClassDeclarationBody(ec);
                UnparseVisitor.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            }

            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) {
            UnparseVisitor.this.unparseEnumDeclaration(pmed);
            return null;
        }

        @Override @Nullable public Void
        visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) {
            UnparseVisitor.this.unparseAnnotationTypeDeclaration(matd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) {
            UnparseVisitor.this.unparseAnnotationTypeDeclaration(pmatd);
            return null;
        }

        @Override @Nullable public Void
        visitMemberEnumDeclaration(MemberEnumDeclaration med) {
            UnparseVisitor.this.unparseEnumDeclaration(med);
            return null;
        }

        @Override @Nullable public Void
        visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
            UnparseVisitor.this.unparseInterfaceDeclaration(mid);
            return null;
        }

        @Override @Nullable public Void
        visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
            UnparseVisitor.this.unparseNamedClassDeclaration(mcd);
            return null;
        }
    };

    @Override @Nullable public Void
    visitTypeDeclaration(TypeDeclaration td) {
        td.accept(this.typeDeclarationUnparser);
        return null;
    }

    private final Visitor.TypeBodyDeclarationVisitor<Void, RuntimeException>
    typeBodyDeclarationUnparser = new Visitor.TypeBodyDeclarationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitMemberEnumDeclaration(MemberEnumDeclaration med) {
            UnparseVisitor.this.unparseEnumDeclaration(med);
            return null;
        }

        @Override @Nullable public Void
        visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
            UnparseVisitor.this.unparseNamedClassDeclaration(mcd);
            return null;
        }

        @Override @Nullable public Void
        visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
            UnparseVisitor.this.unparseInterfaceDeclaration(mid);
            return null;
        }

        @Override @Nullable public Void
        visitFunctionDeclarator(FunctionDeclarator fd) {
            return fd.accept(new Visitor.FunctionDeclaratorVisitor<Void, RuntimeException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitConstructorDeclarator(ConstructorDeclarator cd) { UnparseVisitor.this.unparseConstructorDeclarator(cd); return null; }
                @Override @Nullable public Void visitMethodDeclarator(MethodDeclarator md)           { UnparseVisitor.this.unparseMethodDeclarator(md);      return null; }
            });
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitInitializer(Initializer i)            { UnparseVisitor.this.unparseInitializer(i);       return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd) { UnparseVisitor.this.unparseFieldDeclaration(fd); return null; }
    };

    @Override @Nullable public Void
    visitTypeBodyDeclaration(TypeBodyDeclaration tbd) {
        tbd.accept(this.typeBodyDeclarationUnparser);
        return null;
    }

    private final Visitor.BlockStatementVisitor<Void, RuntimeException>
    blockStatementUnparser = new Visitor.BlockStatementVisitor<Void, RuntimeException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitFieldDeclaration(Java.FieldDeclaration fd) { UnparseVisitor.this.unparseFieldDeclaration(fd); return null; }
        @Override @Nullable public Void visitInitializer(Java.Initializer i)            { UnparseVisitor.this.unparseInitializer(i);       return null; }

        @Override @Nullable public Void
        visitBlock(Java.Block b) {
            if (b.statements.isEmpty()) {
                UnparseVisitor.this.pw.print("{}");
                return null;
            }
            UnparseVisitor.this.pw.println('{');
            UnparseVisitor.this.pw.print(AutoIndentWriter.INDENT);
            UnparseVisitor.this.unparseStatements(b.statements);
            UnparseVisitor.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitBreakStatement(Java.BreakStatement bs) {
            UnparseVisitor.this.pw.print("break");
            if (bs.optionalLabel != null) UnparseVisitor.this.pw.print(' ' + bs.optionalLabel);
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitContinueStatement(Java.ContinueStatement cs) {
            UnparseVisitor.this.pw.print("continue");
            if (cs.optionalLabel != null) UnparseVisitor.this.pw.print(' ' + cs.optionalLabel);
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitAssertStatement(Java.AssertStatement as) {

            UnparseVisitor.this.pw.print("assert ");
            UnparseVisitor.this.unparse(as.expression1);

            Rvalue oe2 = as.optionalExpression2;
            if (oe2 != null) {
                UnparseVisitor.this.pw.print(" : ");
                UnparseVisitor.this.unparse(oe2);
            }
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitDoStatement(Java.DoStatement ds) {
            UnparseVisitor.this.pw.print("do ");
            UnparseVisitor.this.unparseBlockStatement(ds.body);
            UnparseVisitor.this.pw.print("while (");
            UnparseVisitor.this.unparse(ds.condition);
            UnparseVisitor.this.pw.print(");");
            return null;
        }

        @Override @Nullable public Void
        visitEmptyStatement(Java.EmptyStatement es) {
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitExpressionStatement(Java.ExpressionStatement es) {
            UnparseVisitor.this.unparse(es.rvalue);
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitForStatement(Java.ForStatement fs) {
            UnparseVisitor.this.pw.print("for (");
            if (fs.optionalInit != null) {
                UnparseVisitor.this.unparseBlockStatement(fs.optionalInit);
            } else {
                UnparseVisitor.this.pw.print(';');
            }

            Rvalue oc = fs.optionalCondition;
            if (oc != null) {
                UnparseVisitor.this.pw.print(' ');
                UnparseVisitor.this.unparse(oc);
            }

            UnparseVisitor.this.pw.print(';');

            Rvalue[] ou = fs.optionalUpdate;
            if (ou != null) {
                UnparseVisitor.this.pw.print(' ');
                for (int i = 0; i < ou.length; ++i) {
                    if (i > 0) UnparseVisitor.this.pw.print(", ");
                    UnparseVisitor.this.unparse(ou[i]);
                }
            }

            UnparseVisitor.this.pw.print(") ");
            UnparseVisitor.this.unparseBlockStatement(fs.body);
            return null;
        }

        @Override @Nullable public Void
        visitForEachStatement(Java.ForEachStatement fes) {
            UnparseVisitor.this.pw.print("for (");
            UnparseVisitor.this.unparseFormalParameter(fes.currentElement, false);
            UnparseVisitor.this.pw.print(" : ");
            UnparseVisitor.this.unparse(fes.expression);
            UnparseVisitor.this.pw.print(") ");
            UnparseVisitor.this.unparseBlockStatement(fes.body);
            return null;
        }

        @Override @Nullable public Void
        visitIfStatement(Java.IfStatement is) {
            UnparseVisitor.this.pw.print("if (");
            UnparseVisitor.this.unparse(is.condition);
            UnparseVisitor.this.pw.print(") ");
            UnparseVisitor.this.unparseBlockStatement(is.thenStatement);

            BlockStatement oes = is.optionalElseStatement;
            if (oes != null) {
                UnparseVisitor.this.pw.println(" else");
                UnparseVisitor.this.unparseBlockStatement(oes);
            }

            return null;
        }

        @Override @Nullable public Void
        visitLabeledStatement(Java.LabeledStatement ls) {
            UnparseVisitor.this.pw.println(ls.label + ':');
            UnparseVisitor.this.unparseBlockStatement(ls.body);
            return null;
        }

        @Override @Nullable public Void
        visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
            UnparseVisitor.this.unparseTypeDeclaration(lcds.lcd);
            return null;
        }

        @Override @Nullable public Void
        visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
            UnparseVisitor.this.unparseAnnotations(lvds.modifiers.annotations);
            UnparseVisitor.this.unparseModifiers(lvds.modifiers.accessFlags);
            UnparseVisitor.this.unparseType(lvds.type);
            UnparseVisitor.this.pw.print(' ');
            UnparseVisitor.this.pw.print(AutoIndentWriter.TABULATOR);
            UnparseVisitor.this.unparseVariableDeclarator(lvds.variableDeclarators[0]);
            for (int i = 1; i < lvds.variableDeclarators.length; ++i) {
                UnparseVisitor.this.pw.print(", ");
                UnparseVisitor.this.unparseVariableDeclarator(lvds.variableDeclarators[i]);
            }
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitReturnStatement(Java.ReturnStatement rs) {

            UnparseVisitor.this.pw.print("return");

            Rvalue orv = rs.optionalReturnValue;
            if (orv != null) {
                UnparseVisitor.this.pw.print(' ');
                UnparseVisitor.this.unparse(orv);
            }

            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitSwitchStatement(Java.SwitchStatement ss) {
            UnparseVisitor.this.pw.print("switch (");
            UnparseVisitor.this.unparse(ss.condition);
            UnparseVisitor.this.pw.println(") {");
            for (Java.SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
                UnparseVisitor.this.pw.print(AutoIndentWriter.UNINDENT);
                try {
                    for (Java.Rvalue rv : sbsg.caseLabels) {
                        UnparseVisitor.this.pw.print("case ");
                        UnparseVisitor.this.unparse(rv);
                        UnparseVisitor.this.pw.println(':');
                    }
                    if (sbsg.hasDefaultLabel) UnparseVisitor.this.pw.println("default:");
                } finally {
                    UnparseVisitor.this.pw.print(AutoIndentWriter.INDENT);
                }
                for (Java.BlockStatement bs : sbsg.blockStatements) {
                    UnparseVisitor.this.unparseBlockStatement(bs);
                    UnparseVisitor.this.pw.println();
                }
            }
            UnparseVisitor.this.pw.print('}');
            return null;
        }

        @Override @Nullable public Void
        visitSynchronizedStatement(Java.SynchronizedStatement ss) {
            UnparseVisitor.this.pw.print("synchronized (");
            UnparseVisitor.this.unparse(ss.expression);
            UnparseVisitor.this.pw.print(") ");
            UnparseVisitor.this.unparseBlockStatement(ss.body);
            return null;
        }

        @Override @Nullable public Void
        visitThrowStatement(Java.ThrowStatement ts) {
            UnparseVisitor.this.pw.print("throw ");
            UnparseVisitor.this.unparse(ts.expression);
            UnparseVisitor.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitTryStatement(Java.TryStatement ts) {
            UnparseVisitor.this.pw.print("try ");
            UnparseVisitor.this.unparseBlockStatement(ts.body);
            for (Java.CatchClause cc : ts.catchClauses) {
                UnparseVisitor.this.pw.print(" catch (");
                UnparseVisitor.this.unparseFormalParameter(cc.caughtException, false);
                UnparseVisitor.this.pw.print(") ");
                UnparseVisitor.this.unparseBlockStatement(cc.body);
            }

            Block of = ts.optionalFinally;
            if (of != null) {
                UnparseVisitor.this.pw.print(" finally ");
                UnparseVisitor.this.unparseBlockStatement(of);
            }

            return null;
        }

        @Override @Nullable public Void
        visitWhileStatement(Java.WhileStatement ws) {
            UnparseVisitor.this.pw.print("while (");
            UnparseVisitor.this.unparse(ws.condition);
            UnparseVisitor.this.pw.print(") ");
            UnparseVisitor.this.unparseBlockStatement(ws.body);
            return null;
        }

        @Override @Nullable public Void
        visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
            UnparseVisitor.this.pw.print("this");
            UnparseVisitor.this.unparseFunctionInvocationArguments(aci.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
            if (sci.optionalQualification != null) {
                UnparseVisitor.this.unparseLhs(sci.optionalQualification, ".");
                UnparseVisitor.this.pw.print('.');
            }
            UnparseVisitor.this.pw.print("super");
            UnparseVisitor.this.unparseFunctionInvocationArguments(sci.arguments);
            return null;
        }
    };

    @Override @Nullable public Void
    visitBlockStatement(BlockStatement bs) {
        bs.accept(this.blockStatementUnparser);
        return null;
    }

    private final Visitor.AtomVisitor<Void, RuntimeException>
    atomUnparser = new Visitor.AtomVisitor<Void, RuntimeException>() {


        @Override @Nullable public Void
        visitType(Type t) {
            t.accept(new Visitor.TypeVisitor<Void, RuntimeException>() {

                @Override @Nullable public Void
                visitArrayType(Java.ArrayType at) {
                    UnparseVisitor.this.unparseType(at.componentType);
                    UnparseVisitor.this.pw.print("[]");
                    return null;
                }

                @Override @Nullable public Void
                visitPrimitiveType(Java.PrimitiveType bt) {
                    UnparseVisitor.this.pw.print(bt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitReferenceType(Java.ReferenceType rt) {
                    UnparseVisitor.this.pw.print(rt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitRvalueMemberType(Java.RvalueMemberType rmt) {
                    UnparseVisitor.this.pw.print(rmt.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitSimpleType(Java.SimpleType st) {
                    UnparseVisitor.this.pw.print(st.toString());
                    return null;
                }
            });
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Java.Package p) { UnparseVisitor.this.pw.print(p.toString()); return null; }

        @Override @Nullable public Void
        visitRvalue(Java.Rvalue rv) {
            rv.accept(new Visitor.RvalueVisitor<Void, RuntimeException>() {

                @Override @Nullable public Void
                visitLvalue(Lvalue lv) {
                    lv.accept(new Visitor.LvalueVisitor<Void, RuntimeException>() {

                        @Override @Nullable public Void
                        visitAmbiguousName(Java.AmbiguousName an) {
                            UnparseVisitor.this.pw.print(an.toString());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
                            UnparseVisitor.this.unparseLhs(aae.lhs, "[ ]");
                            UnparseVisitor.this.pw.print('[');
                            UnparseVisitor.this.unparse(aae.index);
                            UnparseVisitor.this.pw.print(']');
                            return null;
                        }

                        @Override @Nullable public Void
                        visitFieldAccess(Java.FieldAccess fa) {
                            UnparseVisitor.this.unparseLhs(fa.lhs, ".");
                            UnparseVisitor.this.pw.print('.' + fa.field.getName());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitFieldAccessExpression(Java.FieldAccessExpression fae) {
                            UnparseVisitor.this.unparseLhs(fae.lhs, ".");
                            UnparseVisitor.this.pw.print('.' + fae.fieldName);
                            return null;
                        }

                        @Override @Nullable public Void
                        visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {
                            if (scfae.optionalQualification != null) {
                                UnparseVisitor.this.unparseType(scfae.optionalQualification);
                                UnparseVisitor.this.pw.print(".super." + scfae.fieldName);
                            } else
                            {
                                UnparseVisitor.this.pw.print("super." + scfae.fieldName);
                            }
                            return null;
                        }

                        @Override @Nullable public Void
                        visitLocalVariableAccess(Java.LocalVariableAccess lva) {
                            UnparseVisitor.this.pw.print(lva.toString());
                            return null;
                        }

                        @Override @Nullable public Void
                        visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
                            UnparseVisitor.this.pw.print('(');
                            UnparseVisitor.this.unparse(pe.value);
                            UnparseVisitor.this.pw.print(')');
                            return null;
                        }
                    });
                    return null;
                }

                @Override @Nullable public Void
                visitMethodInvocation(Java.MethodInvocation mi) {
                    if (mi.optionalTarget != null) {
                        UnparseVisitor.this.unparseLhs(mi.optionalTarget, ".");
                        UnparseVisitor.this.pw.print('.');
                    }
                    UnparseVisitor.this.pw.print(mi.methodName);
                    UnparseVisitor.this.unparseFunctionInvocationArguments(mi.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitNewClassInstance(Java.NewClassInstance nci) {
                    if (nci.optionalQualification != null) {
                        UnparseVisitor.this.unparseLhs(nci.optionalQualification, ".");
                        UnparseVisitor.this.pw.print('.');
                    }
                    assert nci.type != null;
                    UnparseVisitor.this.pw.print("new " + nci.type.toString());
                    UnparseVisitor.this.unparseFunctionInvocationArguments(nci.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitAssignment(Java.Assignment a) {
                    UnparseVisitor.this.unparseLhs(a.lhs, a.operator);
                    UnparseVisitor.this.pw.print(' ' + a.operator + ' ');
                    UnparseVisitor.this.unparseRhs(a.rhs, a.operator);
                    return null;
                }

                @Override @Nullable public Void
                visitArrayLength(Java.ArrayLength al) {
                    UnparseVisitor.this.unparseLhs(al.lhs, ".");
                    UnparseVisitor.this.pw.print(".length");
                    return null;
                }

                @Override @Nullable public Void
                visitBinaryOperation(Java.BinaryOperation bo) {
                    UnparseVisitor.this.unparseLhs(bo.lhs, bo.op);
                    UnparseVisitor.this.pw.print(' ' + bo.op + ' ');
                    UnparseVisitor.this.unparseRhs(bo.rhs, bo.op);
                    return null;
                }

                @Override @Nullable public Void
                visitCast(Java.Cast c) {
                    UnparseVisitor.this.pw.print('(');
                    UnparseVisitor.this.unparseType(c.targetType);
                    UnparseVisitor.this.pw.print(") ");
                    UnparseVisitor.this.unparseRhs(c.value, "cast");
                    return null;
                }

                @Override @Nullable public Void
                visitClassLiteral(Java.ClassLiteral cl) {
                    UnparseVisitor.this.unparseType(cl.type);
                    UnparseVisitor.this.pw.print(".class");
                    return null;
                }

                @Override @Nullable public Void
                visitConditionalExpression(Java.ConditionalExpression ce) {
                    UnparseVisitor.this.unparseLhs(ce.lhs, "?:");
                    UnparseVisitor.this.pw.print(" ? ");
                    UnparseVisitor.this.unparseLhs(ce.mhs, "?:");
                    UnparseVisitor.this.pw.print(" : ");
                    UnparseVisitor.this.unparseRhs(ce.rhs, "?:");
                    return null;
                }

                @Override @Nullable public Void
                visitCrement(Java.Crement c) {
                    if (c.pre) {
                        UnparseVisitor.this.pw.print(c.operator);
                        UnparseVisitor.this.unparseUnaryOperation(c.operand, c.operator + "x");
                    } else
                    {
                        UnparseVisitor.this.unparseUnaryOperation(c.operand, "x" + c.operator);
                        UnparseVisitor.this.pw.print(c.operator);
                    }
                    return null;
                }

                @Override @Nullable public Void
                visitInstanceof(Java.Instanceof io) {
                    UnparseVisitor.this.unparseLhs(io.lhs, "instanceof");
                    UnparseVisitor.this.pw.print(" instanceof ");
                    UnparseVisitor.this.unparseType(io.rhs);
                    return null;
                }

                // SUPPRESS CHECKSTYLE LineLength:8
                @Override @Nullable public Void visitIntegerLiteral(Java.IntegerLiteral il)              { UnparseVisitor.this.pw.print(il.value);             return null; }
                @Override @Nullable public Void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) { UnparseVisitor.this.pw.print(fpl.value);            return null; }
                @Override @Nullable public Void visitBooleanLiteral(Java.BooleanLiteral bl)              { UnparseVisitor.this.pw.print(bl.value);             return null; }
                @Override @Nullable public Void visitCharacterLiteral(Java.CharacterLiteral cl)          { UnparseVisitor.this.pw.print(cl.value);             return null; }
                @Override @Nullable public Void visitStringLiteral(Java.StringLiteral sl)                { UnparseVisitor.this.pw.print(sl.value);             return null; }
                @Override @Nullable public Void visitNullLiteral(Java.NullLiteral nl)                    { UnparseVisitor.this.pw.print(nl.value);             return null; }
                @Override @Nullable public Void visitSimpleConstant(Java.SimpleConstant sl)              { UnparseVisitor.this.pw.print("[" + sl.value + ']'); return null; }

                @Override @Nullable public Void
                visitNewArray(Java.NewArray na) {
                    UnparseVisitor.this.pw.print("new ");
                    UnparseVisitor.this.unparseType(na.type);
                    for (Rvalue dimExpr : na.dimExprs) {
                        UnparseVisitor.this.pw.print('[');
                        UnparseVisitor.this.unparse(dimExpr);
                        UnparseVisitor.this.pw.print(']');
                    }
                    for (int i = 0; i < na.dims; ++i) {
                        UnparseVisitor.this.pw.print("[]");
                    }
                    return null;
                }

                @Override @Nullable public Void
                visitNewInitializedArray(Java.NewInitializedArray nai) {

                    UnparseVisitor.this.pw.print("new ");

                    ArrayType at = nai.arrayType;
                    assert at != null;
                    UnparseVisitor.this.unparseType(at);

                    UnparseVisitor.this.pw.print(" ");

                    UnparseVisitor.this.unparseArrayInitializerOrRvalue(nai.arrayInitializer);

                    return null;
                }

                @Override @Nullable public Void
                visitParameterAccess(Java.ParameterAccess pa) {
                    UnparseVisitor.this.pw.print(pa.toString());
                    return null;
                }

                @Override @Nullable public Void
                visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
                    UnparseVisitor.this.unparseType(qtr.qualification);
                    UnparseVisitor.this.pw.print(".this");
                    return null;
                }

                @Override @Nullable public Void
                visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
                    UnparseVisitor.this.pw.print("super." + smi.methodName);
                    UnparseVisitor.this.unparseFunctionInvocationArguments(smi.arguments);
                    return null;
                }

                @Override @Nullable public Void
                visitThisReference(Java.ThisReference tr) { UnparseVisitor.this.pw.print("this"); return null; }

                @Override @Nullable public Void
                visitUnaryOperation(Java.UnaryOperation uo) {
                    UnparseVisitor.this.pw.print(uo.operator);
                    UnparseVisitor.this.unparseUnaryOperation(uo.operand, uo.operator + "x");
                    return null;
                }

                @Override @Nullable public Void
                visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
                    if (naci.optionalQualification != null) {
                        UnparseVisitor.this.unparseLhs(naci.optionalQualification, ".");
                        UnparseVisitor.this.pw.print('.');
                    }
                    UnparseVisitor.this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
                    for (int i = 0; i < naci.arguments.length; ++i) {
                        if (i > 0) UnparseVisitor.this.pw.print(", ");
                        UnparseVisitor.this.unparse(naci.arguments[i]);
                    }
                    UnparseVisitor.this.pw.println(") {");
                    UnparseVisitor.this.pw.print(AutoIndentWriter.INDENT);
                    UnparseVisitor.this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
                    UnparseVisitor.this.pw.print(AutoIndentWriter.UNINDENT + "}");
                    return null;
                }
            });
            return null;
        }
    };

    @Override @Nullable public Void
    visitAtom(Atom a) {
        a.accept(this.atomUnparser);
        return null;
    }

    private final Visitor.ElementValueVisitor<Void, RuntimeException>
    elementValueUnparser = new Visitor.ElementValueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws RuntimeException {
            rv.accept(UnparseVisitor.this.atomUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitAnnotation(Annotation a) {
            a.accept(UnparseVisitor.this.annotationUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) {
            if (evai.elementValues.length == 0) {
                UnparseVisitor.this.pw.append("{}");
                return null;
            }

            UnparseVisitor.this.pw.append("{ ");
            evai.elementValues[0].accept(this);
            for (int i = 1; i < evai.elementValues.length; i++) {
                UnparseVisitor.this.pw.append(", ");
                evai.elementValues[i].accept(this);
            }
            UnparseVisitor.this.pw.append(" }");
            return null;
        }
    };

    @Override @Nullable public Void
    visitElementValue(ElementValue ev) {
        ev.accept(this.elementValueUnparser);
        return null;
    }

    private final AnnotationVisitor<Void, RuntimeException>
    annotationUnparser = new AnnotationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitMarkerAnnotation(Java.MarkerAnnotation ma) {
            UnparseVisitor.this.pw.append('@').append(ma.type.toString()).append(' ');
            return null;
        }

        @Override @Nullable public Void
        visitNormalAnnotation(Java.NormalAnnotation na) {
            UnparseVisitor.this.pw.append('@').append(na.type.toString()).append('(');
            for (int i = 0; i < na.elementValuePairs.length; i++) {
                Java.ElementValuePair evp = na.elementValuePairs[i];

                if (i > 0) UnparseVisitor.this.pw.print(", ");

                UnparseVisitor.this.pw.append(evp.identifier).append(" = ");

                evp.elementValue.accept(UnparseVisitor.this.elementValueUnparser);
            }
            UnparseVisitor.this.pw.append(") ");
            return null;
        }

        @Override @Nullable public Void
        visitSingleElementAnnotation(Java.SingleElementAnnotation sea) {
            UnparseVisitor.this.pw.append('@').append(sea.type.toString()).append('(');
            sea.elementValue.accept(UnparseVisitor.this.elementValueUnparser);
            UnparseVisitor.this.pw.append(") ");
            return null;
        }
    };

    @Override @Nullable public Void
    visitAnnotation(Annotation a) {
        a.accept(this.annotationUnparser);
        return null;
    }

    private void
    unparseInitializer(Java.Initializer i) {
        if (i.statiC) UnparseVisitor.this.pw.print("static ");
        UnparseVisitor.this.unparseBlockStatement(i.block);
    }

    private void
    unparseFieldDeclaration(Java.FieldDeclaration fd) {
        UnparseVisitor.this.unparseDocComment(fd);
        UnparseVisitor.this.unparseAnnotations(fd.modifiers.annotations);
        UnparseVisitor.this.unparseModifiers(fd.modifiers.accessFlags);
        UnparseVisitor.this.unparseType(fd.type);
        UnparseVisitor.this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) UnparseVisitor.this.pw.print(", ");
            UnparseVisitor.this.unparseVariableDeclarator(fd.variableDeclarators[i]);
        }
        UnparseVisitor.this.pw.print(';');
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
            UnparseVisitor.unparse(cu, w);
        }
        w.flush();
    }

    /** Unparses the given {@link Java.CompilationUnit} to the given {@link Writer}. */
    public static void
    unparse(Java.CompilationUnit cu, Writer w) {
        UnparseVisitor uv = new UnparseVisitor(w);
        uv.unparseCompilationUnit(cu);
        uv.close();
    }

    public
    UnparseVisitor(Writer w) {
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

    @Override @Nullable public Void
    visitImportDeclaration(ImportDeclaration id) { return id.accept(this.importUnparser); }

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

    private void
    unparseBlockStatement(Java.BlockStatement blockStatement) { blockStatement.accept(this.blockStatementUnparser); }

    private void
    unparseTypeDeclaration(Java.TypeDeclaration td) { td.accept(this.typeDeclarationUnparser); }

    private void
    unparseType(Java.Type type) { type.accept(this.atomUnparser); }

    private void
    unparse(Java.Atom operand) { operand.accept(this.atomUnparser); }

    /**
     * Iff the <code>operand</code> is unnatural for the <code>unaryOperator</code>, enclose the
     * <code>operand</code> in parentheses. Example: "a+b" is an unnatural operand for unary "!x".
     *
     * @param unaryOperator ++x --x +x -x ~x !x x++ x--
     */
    private void
    unparseUnaryOperation(Java.Rvalue operand, String unaryOperator) {
        int cmp = UnparseVisitor.comparePrecedence(unaryOperator, operand);
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
        int cmp = UnparseVisitor.comparePrecedence(binaryOperator, lhs);
        this.unparse(lhs, cmp < 0 || (cmp == 0 && UnparseVisitor.isLeftAssociate(binaryOperator)));
    }


    /**
     * Iff the <code>rhs</code> is unnatural for the <code>binaryOperator</code>, enclose the
     * <code>rhs</code> in parentheses. Example: "a+b" is an unnatural rhs for operator "*".
     */
    private void
    unparseRhs(Java.Rvalue rhs, String binaryOperator) {
        int cmp = UnparseVisitor.comparePrecedence(binaryOperator, rhs);
        this.unparse(rhs, cmp < 0 || (cmp == 0 && UnparseVisitor.isRightAssociate(binaryOperator)));
    }

    private void
    unparse(Java.Atom operand, boolean natural) {
        if (!natural) this.pw.print("((( ");
        this.unparse(operand);
        if (!natural) this.pw.print(" )))");
    }

    /**
     * Return true iff operator is right associative e.g. <code>a = b = c</code> evaluates as
     * <code>a = (b = c)</code>.
     *
     * @return Return true iff operator is right associative
     */
    private static boolean
    isRightAssociate(String op) { return UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS.contains(op); }

    /**
     * Return true iff operator is left associative e.g. <code>a - b - c</code> evaluates as
     * <code>(a - b) - c</code>.
     *
     * @return Return true iff operator is left associative
     */
    private static boolean
    isLeftAssociate(String op) { return UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS.contains(op); }

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
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(((Java.BinaryOperation) operand).op)
            );
        } else
        if (operand instanceof Java.UnaryOperation) {
            return (
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(((Java.UnaryOperation) operand).operator + "x")
            );
        } else
        if (operand instanceof Java.ConditionalExpression) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("?:");
        } else
        if (operand instanceof Java.Instanceof) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("instanceof");
        } else
        if (operand instanceof Java.Cast) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("cast");
        } else
        if (operand instanceof Java.MethodInvocation || operand instanceof Java.FieldAccess) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence(".");
        } else
        if (operand instanceof Java.NewArray) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("new");
        } else
        if (operand instanceof Java.Crement) {
            Java.Crement c = (Java.Crement) operand;
            return (
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(c.pre ? c.operator + "x" : "x" + c.operator)
            );
        } else
        {
            // All other rvalues (e.g. literal) have higher precedence than any operator.
            return -1;
        }
    }

    private static int
    getOperatorPrecedence(String operator) {
        return ((Integer) UnparseVisitor.OPERATOR_PRECEDENCE.get(operator)).intValue();
    }

    private static final Set<String>          LEFT_ASSOCIATIVE_OPERATORS  = new HashSet();
    private static final Set<String>          RIGHT_ASSOCIATIVE_OPERATORS = new HashSet();
    private static final Set<String>          UNARY_OPERATORS             = new HashSet();
    private static final Map<String, Integer> OPERATOR_PRECEDENCE         = new HashMap();
    static {
        Object[] ops = {
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", ">>>=",
                                                        "&=", "^=", "|=", // SUPPRESS CHECKSTYLE WrapAndIndent
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "?:",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "||",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "&&",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "|",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "^",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "&",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "==", "!=",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "<", ">", "<=", ">=", "instanceof",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "<<", ">>", ">>>",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "+", "-",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "*", "/", "%",
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "cast",
            UnparseVisitor.UNARY_OPERATORS,             "++x", "--x", "+x", "-x", "~x", "!x",
            UnparseVisitor.UNARY_OPERATORS,             "x++", "x--",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "new",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  ".", "[ ]",
        };
        int precedence = 0;
        LOOP1: for (int i = 0;;) {
            final Set<String> s  = (Set) ops[i++];
            final Integer     pi = new Integer(++precedence);
            for (;;) {
                if (i == ops.length) break LOOP1;
                if (!(ops[i] instanceof String)) break;
                String op = (String) ops[i++];
                s.add(op);
                UnparseVisitor.OPERATOR_PRECEDENCE.put(op, pi);
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
            this.unparse((Java.Rvalue) aiorv);
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
            this.unparse(arguments[i]);
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
