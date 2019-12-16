
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

package org.codehaus.janino.util;

import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AbstractTypeBodyDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.AssertStatement;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BooleanRvalue;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.BreakableStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinuableStatement;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.Literal;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Located;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.ModularCompilationUnit;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NamedTypeDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.Package;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.TryStatement.LocalVariableDeclaratorResource;
import org.codehaus.janino.Java.TryStatement.VariableAccessResource;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Visitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.TryStatementResourceVisitor;

/**
 * A basic implementation of {@link Traverser}; each {@code traverse*(s)} method invokes the
 * {@code traverse*()} methods of all Java elements subordinate to {@code x}.
 * <p>
 *   Example:
 * </p>
 * <pre>
 *     LocalClassDeclaration lcd = ...;
 *
 *     new AbstractTraverser() {
 *
 *         int n = 0;
 *
 *         public void
 *         traverseMethodDeclarator(MethodDeclarator md) {
 *             ++this.n;
 *             super.traverseMethodDeclarator(md);
 *         }
 *     }.visitTypeDeclaration(lcd);
 * </pre>
 *
 * @param <EX> The exception that the "{@code traverse*()}" and "{@code visit*()}" methods may throw
 * @see #visitAnnotation(Java.Annotation)
 * @see #visitAtom(Java.Atom)
 * @see #visitBlockStatement(Java.BlockStatement)
 * @see #visitElementValue(Java.ElementValue)
 * @see #visitImportDeclaration(Java.AbstractCompilationUnit.ImportDeclaration)
 * @see #visitTypeBodyDeclaration(Java.TypeBodyDeclaration)
 * @see #visitTypeDeclaration(Java.TypeDeclaration)
 * @see #visitAbstractCompilationUnit(Java.AbstractCompilationUnit)
 */
public
class AbstractTraverser<EX extends Throwable> implements Traverser<EX> {

    private final Traverser<EX> delegate;

    public
    AbstractTraverser() { this.delegate = this; }

    public
    AbstractTraverser(Traverser<EX> delegate) { this.delegate = delegate; }

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link AbstractCompilationUnit}.
     */
    private final Visitor.AbstractCompilationUnitVisitor<Void, EX>
    abstractCompilationUnitTraverser = new Visitor.AbstractCompilationUnitVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitCompilationUnit(CompilationUnit cu) throws EX {
            AbstractTraverser.this.delegate.traverseCompilationUnit(cu);
            return null;
        }

        @Override @Nullable public Void
        visitModularCompilationUnit(ModularCompilationUnit mcu) throws EX {
            AbstractTraverser.this.delegate.traverseModularCompilationUnit(mcu);
            return null;
        }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link ImportDeclaration}.
     */
    private final Visitor.ImportVisitor<Void, EX>
    importTraverser = new Visitor.ImportVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:4
        @Override @Nullable public Void visitSingleTypeImportDeclaration(AbstractCompilationUnit.SingleTypeImportDeclaration stid)          throws EX { AbstractTraverser.this.delegate.traverseSingleTypeImportDeclaration(stid);      return null; }
        @Override @Nullable public Void visitTypeImportOnDemandDeclaration(AbstractCompilationUnit.TypeImportOnDemandDeclaration tiodd)     throws EX { AbstractTraverser.this.delegate.traverseTypeImportOnDemandDeclaration(tiodd);   return null; }
        @Override @Nullable public Void visitSingleStaticImportDeclaration(AbstractCompilationUnit.SingleStaticImportDeclaration ssid)      throws EX { AbstractTraverser.this.delegate.traverseSingleStaticImportDeclaration(ssid);    return null; }
        @Override @Nullable public Void visitStaticImportOnDemandDeclaration(AbstractCompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX { AbstractTraverser.this.delegate.traverseStaticImportOnDemandDeclaration(siodd); return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link TypeDeclaration}.
     */
    private final Visitor.TypeDeclarationVisitor<Void, EX>
    typeDeclarationTraverser = new Visitor.TypeDeclarationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:11
        @Override @Nullable public Void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws EX { AbstractTraverser.this.delegate.traverseAnonymousClassDeclaration(acd);                return null; }
        @Override @Nullable public Void visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws EX { AbstractTraverser.this.delegate.traverseLocalClassDeclaration(lcd);                    return null; }
        @Override @Nullable public Void visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)                    throws EX { AbstractTraverser.this.delegate.traversePackageMemberClassDeclaration(pmcd);           return null; }
        @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws EX { AbstractTraverser.this.delegate.traversePackageMemberInterfaceDeclaration(pmid);       return null; }
        @Override @Nullable public Void visitEnumConstant(EnumConstant ec)                                                        throws EX { AbstractTraverser.this.delegate.traverseEnumConstant(ec);                              return null; }
        @Override @Nullable public Void visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws EX { AbstractTraverser.this.delegate.traversePackageMemberEnumDeclaration(pmed);            return null; }
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd)                throws EX { AbstractTraverser.this.delegate.traverseMemberAnnotationTypeDeclaration(matd);         return null; }
        @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX { AbstractTraverser.this.delegate.traversePackageMemberAnnotationTypeDeclaration(pmatd); return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws EX { AbstractTraverser.this.delegate.traverseMemberEnumDeclaration(med);                    return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws EX { AbstractTraverser.this.delegate.traverseMemberInterfaceDeclaration(mid);               return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws EX { AbstractTraverser.this.delegate.traverseMemberClassDeclaration(mcd);                   return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link Rvalue}.
     */
    private final Visitor.RvalueVisitor<Void, EX>
    rvalueTraverser = new Visitor.RvalueVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitLvalue(Lvalue lv) throws EX {
            lv.accept(new Visitor.LvalueVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:7
                @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws EX { AbstractTraverser.this.delegate.traverseAmbiguousName(an);                      return null; }
                @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws EX { AbstractTraverser.this.delegate.traverseArrayAccessExpression(aae);             return null; }
                @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws EX { AbstractTraverser.this.delegate.traverseFieldAccess(fa);                        return null; }
                @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws EX { AbstractTraverser.this.delegate.traverseFieldAccessExpression(fae);             return null; }
                @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX { AbstractTraverser.this.delegate.traverseSuperclassFieldAccessExpression(scfae); return null; }
                @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws EX { AbstractTraverser.this.delegate.traverseLocalVariableAccess(lva);               return null; }
                @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws EX { AbstractTraverser.this.delegate.traverseParenthesizedExpression(pe);            return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:29
        @Override @Nullable public Void visitArrayLength(ArrayLength al)                                    throws EX { AbstractTraverser.this.delegate.traverseArrayLength(al);                      return null; }
        @Override @Nullable public Void visitAssignment(Assignment a)                                       throws EX { AbstractTraverser.this.delegate.traverseAssignment(a);                        return null; }
        @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                              throws EX { AbstractTraverser.this.delegate.traverseUnaryOperation(uo);                   return null; }
        @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                            throws EX { AbstractTraverser.this.delegate.traverseBinaryOperation(bo);                  return null; }
        @Override @Nullable public Void visitCast(Cast c)                                                   throws EX { AbstractTraverser.this.delegate.traverseCast(c);                              return null; }
        @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                                  throws EX { AbstractTraverser.this.delegate.traverseClassLiteral(cl);                     return null; }
        @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)                throws EX { AbstractTraverser.this.delegate.traverseConditionalExpression(ce);            return null; }
        @Override @Nullable public Void visitCrement(Crement c)                                             throws EX { AbstractTraverser.this.delegate.traverseCrement(c);                           return null; }
        @Override @Nullable public Void visitInstanceof(Instanceof io)                                      throws EX { AbstractTraverser.this.delegate.traverseInstanceof(io);                       return null; }
        @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                          throws EX { AbstractTraverser.this.delegate.traverseMethodInvocation(mi);                 return null; }
        @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws EX { AbstractTraverser.this.delegate.traverseSuperclassMethodInvocation(smi);      return null; }
        @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                              throws EX { AbstractTraverser.this.delegate.traverseIntegerLiteral(il);                   return null; }
        @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws EX { AbstractTraverser.this.delegate.traverseFloatingPointLiteral(fpl);            return null; }
        @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                              throws EX { AbstractTraverser.this.delegate.traverseBooleanLiteral(bl);                   return null; }
        @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                          throws EX { AbstractTraverser.this.delegate.traverseCharacterLiteral(cl);                 return null; }
        @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                                throws EX { AbstractTraverser.this.delegate.traverseStringLiteral(sl);                    return null; }
        @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                    throws EX { AbstractTraverser.this.delegate.traverseNullLiteral(nl);                      return null; }
        @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                              throws EX { AbstractTraverser.this.delegate.traverseSimpleLiteral(sl);                    return null; }
        @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws EX { AbstractTraverser.this.delegate.traverseNewAnonymousClassInstance(naci);      return null; }
        @Override @Nullable public Void visitNewArray(NewArray na)                                          throws EX { AbstractTraverser.this.delegate.traverseNewArray(na);                         return null; }
        @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)                   throws EX { AbstractTraverser.this.delegate.traverseNewInitializedArray(nia);             return null; }
        @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                         throws EX { AbstractTraverser.this.delegate.traverseNewClassInstance(nci);                return null; }
        @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                            throws EX { AbstractTraverser.this.delegate.traverseParameterAccess(pa);                  return null; }
        @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)             throws EX { AbstractTraverser.this.delegate.traverseQualifiedThisReference(qtr);          return null; }
        @Override @Nullable public Void visitThisReference(ThisReference tr)                                throws EX { AbstractTraverser.this.delegate.traverseThisReference(tr);                    return null; }
        @Override @Nullable public Void visitLambdaExpression(LambdaExpression le)                          throws EX { AbstractTraverser.this.delegate.traverseLambdaExpression(le);                 return null; }
        @Override @Nullable public Void visitMethodReference(MethodReference mr)                            throws EX { AbstractTraverser.this.delegate.traverseMethodReference(mr);                  return null; }
        @Override @Nullable public Void visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws EX { AbstractTraverser.this.delegate.traverseClassInstanceCreationReference(cicr); return null; }
        @Override @Nullable public Void visitArrayCreationReference(ArrayCreationReference acr)             throws EX { AbstractTraverser.this.delegate.traverseArrayCreationReference(acr);          return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link TypeBodyDeclaration}.
     */
    private final Visitor.TypeBodyDeclarationVisitor<Void, EX>
    typeBodyDeclarationTraverser = new Visitor.TypeBodyDeclarationVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitFunctionDeclarator(FunctionDeclarator fd) throws EX {
            fd.accept(new Visitor.FunctionDeclaratorVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitConstructorDeclarator(ConstructorDeclarator cd) throws EX { AbstractTraverser.this.delegate.traverseConstructorDeclarator(cd); return null; }
                @Override @Nullable public Void visitMethodDeclarator(MethodDeclarator md)           throws EX { AbstractTraverser.this.delegate.traverseMethodDeclarator(md);      return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:6
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX { AbstractTraverser.this.delegate.traverseMemberAnnotationTypeDeclaration(matd); return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)            throws EX { AbstractTraverser.this.delegate.traverseMemberInterfaceDeclaration(mid);       return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                    throws EX { AbstractTraverser.this.delegate.traverseMemberClassDeclaration(mcd);           return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                      throws EX { AbstractTraverser.this.delegate.traverseMemberEnumDeclaration(med);            return null; }
        @Override @Nullable public Void visitInitializer(Initializer i)                                            throws EX { AbstractTraverser.this.delegate.traverseInitializer(i);                        return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                 throws EX { AbstractTraverser.this.delegate.traverseFieldDeclaration(fd);                  return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link BlockStatement}.
     */
    private final Visitor.BlockStatementVisitor<Void, EX>
    blockStatementTraverser = new BlockStatementVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:23
        @Override @Nullable public Void visitInitializer(Initializer i)                                                throws EX { AbstractTraverser.this.delegate.traverseInitializer(i);                          return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                     throws EX { AbstractTraverser.this.delegate.traverseFieldDeclaration(fd);                    return null; }
        @Override @Nullable public Void visitLabeledStatement(LabeledStatement ls)                                     throws EX { AbstractTraverser.this.delegate.traverseLabeledStatement(ls);                    return null; }
        @Override @Nullable public Void visitBlock(Block b)                                                            throws EX { AbstractTraverser.this.delegate.traverseBlock(b);                                return null; }
        @Override @Nullable public Void visitExpressionStatement(ExpressionStatement es)                               throws EX { AbstractTraverser.this.delegate.traverseExpressionStatement(es);                 return null; }
        @Override @Nullable public Void visitIfStatement(IfStatement is)                                               throws EX { AbstractTraverser.this.delegate.traverseIfStatement(is);                         return null; }
        @Override @Nullable public Void visitForStatement(ForStatement fs)                                             throws EX { AbstractTraverser.this.delegate.traverseForStatement(fs);                        return null; }
        @Override @Nullable public Void visitForEachStatement(ForEachStatement fes)                                    throws EX { AbstractTraverser.this.delegate.traverseForEachStatement(fes);                   return null; }
        @Override @Nullable public Void visitWhileStatement(WhileStatement ws)                                         throws EX { AbstractTraverser.this.delegate.traverseWhileStatement(ws);                      return null; }
        @Override @Nullable public Void visitTryStatement(TryStatement ts)                                             throws EX { AbstractTraverser.this.delegate.traverseTryStatement(ts);                        return null; }
        @Override @Nullable public Void visitSwitchStatement(SwitchStatement ss)                                       throws EX { AbstractTraverser.this.delegate.traverseSwitchStatement(ss);                     return null; }
        @Override @Nullable public Void visitSynchronizedStatement(SynchronizedStatement ss)                           throws EX { AbstractTraverser.this.delegate.traverseSynchronizedStatement(ss);               return null; }
        @Override @Nullable public Void visitDoStatement(DoStatement ds)                                               throws EX { AbstractTraverser.this.delegate.traverseDoStatement(ds);                         return null; }
        @Override @Nullable public Void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX { AbstractTraverser.this.delegate.traverseLocalVariableDeclarationStatement(lvds); return null; }
        @Override @Nullable public Void visitReturnStatement(ReturnStatement rs)                                       throws EX { AbstractTraverser.this.delegate.traverseReturnStatement(rs);                     return null; }
        @Override @Nullable public Void visitThrowStatement(ThrowStatement ts)                                         throws EX { AbstractTraverser.this.delegate.traverseThrowStatement(ts);                      return null; }
        @Override @Nullable public Void visitBreakStatement(BreakStatement bs)                                         throws EX { AbstractTraverser.this.delegate.traverseBreakStatement(bs);                      return null; }
        @Override @Nullable public Void visitContinueStatement(ContinueStatement cs)                                   throws EX { AbstractTraverser.this.delegate.traverseContinueStatement(cs);                   return null; }
        @Override @Nullable public Void visitAssertStatement(AssertStatement as)                                       throws EX { AbstractTraverser.this.delegate.traverseAssertStatement(as);                     return null; }
        @Override @Nullable public Void visitEmptyStatement(EmptyStatement es)                                         throws EX { AbstractTraverser.this.delegate.traverseEmptyStatement(es);                      return null; }
        @Override @Nullable public Void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws EX { AbstractTraverser.this.delegate.traverseLocalClassDeclarationStatement(lcds);    return null; }
        @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws EX { AbstractTraverser.this.delegate.traverseAlternateConstructorInvocation(aci);     return null; }
        @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws EX { AbstractTraverser.this.delegate.traverseSuperConstructorInvocation(sci);         return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link Atom}.
     */
    private final Visitor.AtomVisitor<Void, EX>
    atomTraverser = new Visitor.AtomVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws EX {
            rv.accept(AbstractTraverser.this.rvalueTraverser);
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Package p) throws EX {
            AbstractTraverser.this.delegate.traversePackage(p);
            return null;
        }

        @Override @Nullable public Void
        visitType(Type t) throws EX {

            t.accept(new Visitor.TypeVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:5
                @Override @Nullable public Void visitArrayType(ArrayType at)                throws EX { AbstractTraverser.this.delegate.traverseArrayType(at);         return null; }
                @Override @Nullable public Void visitPrimitiveType(PrimitiveType bt)        throws EX { AbstractTraverser.this.delegate.traversePrimitiveType(bt);     return null; }
                @Override @Nullable public Void visitReferenceType(ReferenceType rt)        throws EX { AbstractTraverser.this.delegate.traverseReferenceType(rt);     return null; }
                @Override @Nullable public Void visitRvalueMemberType(RvalueMemberType rmt) throws EX { AbstractTraverser.this.delegate.traverseRvalueMemberType(rmt); return null; }
                @Override @Nullable public Void visitSimpleType(SimpleType st)              throws EX { AbstractTraverser.this.delegate.traverseSimpleType(st);        return null; }
            });
            return null;
        }

        @Override @Nullable public Void
        visitConstructorInvocation(ConstructorInvocation ci) throws EX {

            ci.accept(new Visitor.ConstructorInvocationVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX { AbstractTraverser.this.delegate.traverseAlternateConstructorInvocation(aci); return null; }
                @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)         throws EX { AbstractTraverser.this.delegate.traverseSuperConstructorInvocation(sci);     return null; }
            });
            return null;
        }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link ElementValue}.
     */
    private final ElementValueVisitor<Void, EX>
    elementValueTraverser = new ElementValueVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws EX {
            rv.accept(AbstractTraverser.this.rvalueTraverser);
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX { AbstractTraverser.this.delegate.traverseElementValueArrayInitializer(evai); return null; }
        @Override @Nullable public Void visitAnnotation(Annotation a)                                        throws EX { AbstractTraverser.this.delegate.traverseAnnotation(a);                      return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link Annotation}.
     */
    private final AnnotationVisitor<Void, EX>
    annotationTraverser = new AnnotationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override @Nullable public Void visitMarkerAnnotation(MarkerAnnotation ma)                throws EX { AbstractTraverser.this.delegate.traverseMarkerAnnotation(ma);         return null; }
        @Override @Nullable public Void visitNormalAnnotation(NormalAnnotation na)                throws EX { AbstractTraverser.this.delegate.traverseNormalAnnotation(na);         return null; }
        @Override @Nullable public Void visitSingleElementAnnotation(SingleElementAnnotation sea) throws EX { AbstractTraverser.this.delegate.traverseSingleElementAnnotation(sea); return null; }
    };

    private final TryStatementResourceVisitor<Void, EX>
    resourceTraverser = new TryStatementResourceVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws EX { AbstractTraverser.this.delegate.traverseLocalVariableDeclaratorResource(lvdr);  return null; }
        @Override @Nullable public Void visitVariableAccessResource(VariableAccessResource var)                    throws EX { AbstractTraverser.this.delegate.traverseVariableAccessResource(var);            return null; }
    };

    @Override public void
    visitAbstractCompilationUnit(AbstractCompilationUnit acu) throws EX {
        acu.accept(this.abstractCompilationUnitTraverser);
    }

    @Override public void
    visitImportDeclaration(AbstractCompilationUnit.ImportDeclaration id) throws EX {
        id.accept(this.importTraverser);
    }

    @Override public void
    visitTypeDeclaration(TypeDeclaration td) throws EX {
        td.accept(this.typeDeclarationTraverser);
    }

    @Override public void
    visitTypeBodyDeclaration(TypeBodyDeclaration tbd) throws EX {
        tbd.accept(this.typeBodyDeclarationTraverser);
    }

    @Override public void
    visitBlockStatement(BlockStatement bs) throws EX {
        bs.accept(this.blockStatementTraverser);
    }

    @Override public void
    visitAtom(Atom a) throws EX {
        a.accept(this.atomTraverser);
    }

    @Override public void
    visitElementValue(ElementValue ev) throws EX { ev.accept(this.elementValueTraverser); }

    @Override public void
    visitAnnotation(Annotation a) throws EX { a.accept(this.annotationTraverser); }

    // These may be overridden by derived classes.

    @SuppressWarnings("unused") @Override public void
    traverseAbstractCompilationUnit(AbstractCompilationUnit acu) throws EX {
    }

    @Override public void
    traverseCompilationUnit(CompilationUnit cu) throws EX {

        for (AbstractCompilationUnit.ImportDeclaration id : cu.importDeclarations) id.accept(this.importTraverser);

        for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            pmtd.accept(this.typeDeclarationTraverser);
        }
    }

    @Override public void
    traverseModularCompilationUnit(ModularCompilationUnit mcu) throws EX {
        for (AbstractCompilationUnit.ImportDeclaration id : mcu.importDeclarations) id.accept(this.importTraverser);
    }

    @Override public void
    traverseSingleTypeImportDeclaration(AbstractCompilationUnit.SingleTypeImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    @Override public void
    traverseTypeImportOnDemandDeclaration(AbstractCompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX {
        this.traverseImportDeclaration(tiodd);
    }

    @Override public void
    traverseSingleStaticImportDeclaration(AbstractCompilationUnit.SingleStaticImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    @Override public void
    traverseStaticImportOnDemandDeclaration(AbstractCompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX {
        this.traverseImportDeclaration(siodd);
    }

    @Override public void
    traverseImportDeclaration(AbstractCompilationUnit.ImportDeclaration id) throws EX { this.traverseLocated(id); }

    @Override public void
    traverseAnonymousClassDeclaration(AnonymousClassDeclaration acd) throws EX {
        acd.baseType.accept(this.atomTraverser);
        this.traverseClassDeclaration(acd);
    }

    @Override public void
    traverseLocalClassDeclaration(LocalClassDeclaration lcd) throws EX { this.traverseNamedClassDeclaration(lcd); }

    @Override public void
    traversePackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd) throws EX {
        this.traverseNamedClassDeclaration(pmcd);
    }

    @Override public void
    traverseMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws EX {
        this.traverseInterfaceDeclaration(mid);
    }

    @Override public void
    traversePackageMemberInterfaceDeclaration(final PackageMemberInterfaceDeclaration pmid) throws EX {
        this.traverseInterfaceDeclaration(pmid);
    }

    @Override public void
    traverseMemberClassDeclaration(MemberClassDeclaration mcd) throws EX {
        this.traverseNamedClassDeclaration(mcd);
    }

    @Override public void
    traverseConstructorDeclarator(ConstructorDeclarator cd) throws EX {
        if (cd.optionalConstructorInvocation != null) {
            cd.optionalConstructorInvocation.accept(this.blockStatementTraverser);
        }
        this.traverseFunctionDeclarator(cd);
    }

    @Override public void
    traverseInitializer(Initializer i) throws EX {
        i.block.accept(this.blockStatementTraverser);
        this.traverseAbstractTypeBodyDeclaration(i);
    }

    @Override public void
    traverseMethodDeclarator(MethodDeclarator md) throws EX { this.traverseFunctionDeclarator(md); }

    @Override public void
    traverseFieldDeclaration(FieldDeclaration fd) throws EX {
        fd.type.accept(this.atomTraverser);
        for (VariableDeclarator vd : fd.variableDeclarators) {
            ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(fd);
    }

    @Override public void
    traverseLabeledStatement(LabeledStatement ls) throws EX {
        ls.body.accept(this.blockStatementTraverser);
        this.traverseBreakableStatement(ls);
    }

    @Override public void
    traverseBlock(Block b) throws EX {
        for (BlockStatement bs : b.statements) bs.accept(this.blockStatementTraverser);
        this.traverseStatement(b);
    }

    @Override public void
    traverseExpressionStatement(ExpressionStatement es) throws EX {
        es.rvalue.accept(this.rvalueTraverser);
        this.traverseStatement(es);
    }

    @Override public void
    traverseIfStatement(IfStatement is) throws EX {
        is.condition.accept(this.rvalueTraverser);
        is.thenStatement.accept(this.blockStatementTraverser);
        if (is.elseStatement != null) is.elseStatement.accept(this.blockStatementTraverser);
        this.traverseStatement(is);
    }

    @Override public void
    traverseForStatement(ForStatement fs) throws EX {
        if (fs.optionalInit != null) fs.optionalInit.accept(this.blockStatementTraverser);
        if (fs.optionalCondition != null) fs.optionalCondition.accept(this.rvalueTraverser);
        if (fs.optionalUpdate != null) {
            for (Rvalue rv : fs.optionalUpdate) rv.accept(this.rvalueTraverser);
        }
        fs.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fs);
    }

    @Override public void
    traverseForEachStatement(ForEachStatement fes) throws EX {
        this.traverseFormalParameter(fes.currentElement);
        fes.expression.accept(this.rvalueTraverser);
        fes.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fes);
    }

    @Override public void
    traverseWhileStatement(WhileStatement ws) throws EX {
        ws.condition.accept(this.rvalueTraverser);
        ws.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(ws);
    }

    @Override public void
    traverseTryStatement(TryStatement ts) throws EX {
        for (TryStatement.Resource r : ts.resources) {
            r.accept(this.resourceTraverser);
        }
        ts.body.accept(this.blockStatementTraverser);
        for (CatchClause cc : ts.catchClauses) cc.body.accept(this.blockStatementTraverser);
        if (ts.finallY != null) ts.finallY.accept(this.blockStatementTraverser);
        this.traverseStatement(ts);
    }

    @Override public void
    traverseSwitchStatement(SwitchStatement ss) throws EX {
        ss.condition.accept(this.rvalueTraverser);
        for (SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (Rvalue cl : sbsg.caseLabels) cl.accept(this.rvalueTraverser);
            for (BlockStatement bs : sbsg.blockStatements) bs.accept(this.blockStatementTraverser);
            this.traverseLocated(sbsg);
        }
        this.traverseBreakableStatement(ss);
    }

    @Override public void
    traverseSynchronizedStatement(SynchronizedStatement ss) throws EX {
        ss.expression.accept(this.rvalueTraverser);
        ss.body.accept(this.blockStatementTraverser);
        this.traverseStatement(ss);
    }

    @Override public void
    traverseDoStatement(DoStatement ds) throws EX {
        ds.body.accept(this.blockStatementTraverser);
        ds.condition.accept(this.rvalueTraverser);
        this.traverseContinuableStatement(ds);
    }

    @Override public void
    traverseLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX {
        lvds.type.accept(this.atomTraverser);
        for (VariableDeclarator vd : lvds.variableDeclarators) {
            ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(lvds);
    }

    @Override public void
    traverseReturnStatement(ReturnStatement rs) throws EX {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept(this.rvalueTraverser);
        this.traverseStatement(rs);
    }

    @Override public void
    traverseThrowStatement(ThrowStatement ts) throws EX {
        ts.expression.accept(this.rvalueTraverser);
        this.traverseStatement(ts);
    }

    @Override public void
    traverseBreakStatement(BreakStatement bs) throws EX { this.traverseStatement(bs); }

    @Override public void
    traverseContinueStatement(ContinueStatement cs) throws EX { this.traverseStatement(cs); }

    @Override public void
    traverseAssertStatement(AssertStatement as) throws EX {
        as.expression1.accept(this.rvalueTraverser);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept(this.rvalueTraverser);
        this.traverseStatement(as);
    }

    @Override public void
    traverseEmptyStatement(EmptyStatement es) throws EX { this.traverseStatement(es); }

    @Override public void
    traverseLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) throws EX {
        lcds.lcd.accept(this.typeDeclarationTraverser);
        this.traverseStatement(lcds);
    }

    @Override public void
    traversePackage(Package p) throws EX { this.traverseAtom(p); }

    @Override public void
    traverseArrayLength(ArrayLength al) throws EX {
        al.lhs.accept(this.rvalueTraverser);
        this.traverseRvalue(al);
    }

    @Override public void
    traverseAssignment(Assignment a) throws EX {
        a.lhs.accept(this.rvalueTraverser);
        a.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(a);
    }

    @Override public void
    traverseUnaryOperation(UnaryOperation uo) throws EX {
        uo.operand.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(uo);
    }

    @Override public void
    traverseBinaryOperation(BinaryOperation bo) throws EX {
        bo.lhs.accept(this.rvalueTraverser);
        bo.rhs.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(bo);
    }

    @Override public void
    traverseCast(Cast c) throws EX {
        c.targetType.accept(this.atomTraverser);
        c.value.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    @Override public void
    traverseClassLiteral(ClassLiteral cl) throws EX {
        cl.type.accept(this.atomTraverser);
        this.traverseRvalue(cl);
    }

    @Override public void
    traverseConditionalExpression(ConditionalExpression ce) throws EX {
        ce.lhs.accept(this.rvalueTraverser);
        ce.mhs.accept(this.rvalueTraverser);
        ce.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(ce);
    }

    @Override public void
    traverseCrement(Crement c) throws EX {
        c.operand.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    @Override public void
    traverseInstanceof(Instanceof io) throws EX {
        io.lhs.accept(this.rvalueTraverser);
        io.rhs.accept(this.atomTraverser);
        this.traverseRvalue(io);
    }

    @Override public void
    traverseMethodInvocation(MethodInvocation mi) throws EX {
        if (mi.optionalTarget != null) mi.optionalTarget.accept(this.atomTraverser);
        this.traverseInvocation(mi);
    }

    @Override public void
    traverseSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws EX { this.traverseInvocation(smi); }

    @Override public void
    traverseLiteral(Literal l) throws EX { this.traverseRvalue(l); }

    @Override public void
    traverseIntegerLiteral(IntegerLiteral il) throws EX { this.traverseLiteral(il); }

    @Override public void
    traverseFloatingPointLiteral(FloatingPointLiteral fpl) throws EX { this.traverseLiteral(fpl); }

    @Override public void
    traverseBooleanLiteral(BooleanLiteral bl) throws EX { this.traverseLiteral(bl); }

    @Override public void
    traverseCharacterLiteral(CharacterLiteral cl) throws EX { this.traverseLiteral(cl); }

    @Override public void
    traverseStringLiteral(StringLiteral sl) throws EX { this.traverseLiteral(sl); }

    @Override public void
    traverseNullLiteral(NullLiteral nl) throws EX { this.traverseLiteral(nl); }

    @Override public void
    traverseSimpleLiteral(SimpleConstant sl) throws EX { this.traverseRvalue(sl); }

    @Override public void
    traverseNewAnonymousClassInstance(NewAnonymousClassInstance naci) throws EX {
        if (naci.optionalQualification != null) {
            naci.optionalQualification.accept(this.rvalueTraverser);
        }
        naci.anonymousClassDeclaration.accept(this.typeDeclarationTraverser);
        for (Rvalue argument : naci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(naci);
    }

    @Override public void
    traverseNewArray(NewArray na) throws EX {
        na.type.accept(this.atomTraverser);
        for (Rvalue dimExpr : na.dimExprs) dimExpr.accept(this.rvalueTraverser);
        this.traverseRvalue(na);
    }

    @Override public void
    traverseNewInitializedArray(NewInitializedArray nia) throws EX {
        assert nia.arrayType != null;
        nia.arrayType.accept(this.atomTraverser);
        this.traverseArrayInitializerOrRvalue(nia.arrayInitializer);
    }

    @Override public void
    traverseArrayInitializerOrRvalue(ArrayInitializerOrRvalue aiorv) throws EX {
        if (aiorv instanceof Rvalue) {
            ((Rvalue) aiorv).accept(this.atomTraverser);
        } else
        if (aiorv instanceof ArrayInitializer) {
            ArrayInitializerOrRvalue[] values = ((ArrayInitializer) aiorv).values;
            for (ArrayInitializerOrRvalue value : values) this.traverseArrayInitializerOrRvalue(value);
        } else
        {
            throw new InternalCompilerException(
                "Unexpected array initializer or rvalue class "
                + aiorv.getClass().getName()
            );
        }
    }

    @Override public void
    traverseNewClassInstance(NewClassInstance nci) throws EX {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.accept(this.rvalueTraverser);
        }
        if (nci.type != null) nci.type.accept(this.atomTraverser);
        for (Rvalue argument : nci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(nci);
    }

    @Override public void
    traverseParameterAccess(ParameterAccess pa) throws EX { this.traverseRvalue(pa); }

    @Override public void
    traverseQualifiedThisReference(QualifiedThisReference qtr) throws EX {
        qtr.qualification.accept(this.atomTraverser);
        this.traverseRvalue(qtr);
    }

    @Override public void
    traverseThisReference(ThisReference tr) throws EX { this.traverseRvalue(tr); }

    @Override public void
    traverseLambdaExpression(LambdaExpression le) throws EX { this.traverseRvalue(le); }

    @Override public void
    traverseMethodReference(MethodReference mr) throws EX { this.traverseRvalue(mr); }

    @Override public void
    traverseClassInstanceCreationReference(ClassInstanceCreationReference cicr) throws EX {
        this.traverseRvalue(cicr);
    }

    @Override public void
    traverseArrayCreationReference(ArrayCreationReference acr) throws EX { this.traverseRvalue(acr); }

    @Override public void
    traverseArrayType(ArrayType at) throws EX {
        at.componentType.accept(this.atomTraverser);
        this.traverseType(at);
    }

    @Override public void
    traversePrimitiveType(PrimitiveType bt) throws EX { this.traverseType(bt); }

    @Override public void
    traverseReferenceType(ReferenceType rt) throws EX {
        for (Annotation a : rt.annotations) this.visitAnnotation(a);
        this.traverseType(rt);
    }

    @Override public void
    traverseRvalueMemberType(RvalueMemberType rmt) throws EX {
        rmt.rvalue.accept(this.rvalueTraverser);
        this.traverseType(rmt);
    }

    @Override public void
    traverseSimpleType(SimpleType st) throws EX { this.traverseType(st); }

    @Override public void
    traverseAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX {
        this.traverseConstructorInvocation(aci);
    }

    @Override public void
    traverseSuperConstructorInvocation(SuperConstructorInvocation sci) throws EX {
        if (sci.optionalQualification != null) {
            sci.optionalQualification.accept(this.rvalueTraverser);
        }
        this.traverseConstructorInvocation(sci);
    }

    @Override public void
    traverseAmbiguousName(AmbiguousName an) throws EX { this.traverseLvalue(an); }

    @Override public void
    traverseArrayAccessExpression(ArrayAccessExpression aae) throws EX {
        aae.lhs.accept(this.rvalueTraverser);
        aae.index.accept(this.atomTraverser);
        this.traverseLvalue(aae);
    }

    @Override public void
    traverseFieldAccess(FieldAccess fa) throws EX {
        fa.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fa);
    }

    @Override public void
    traverseFieldAccessExpression(FieldAccessExpression fae) throws EX {
        fae.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fae);
    }

    @Override public void
    traverseSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX {
        if (scfae.optionalQualification != null) {
            scfae.optionalQualification.accept(this.atomTraverser);
        }
        this.traverseLvalue(scfae);
    }

    @Override public void
    traverseLocalVariableAccess(LocalVariableAccess lva) throws EX { this.traverseLvalue(lva); }

    @Override public void
    traverseParenthesizedExpression(ParenthesizedExpression pe) throws EX {
        pe.value.accept(this.rvalueTraverser);
        this.traverseLvalue(pe);
    }

    @Override public void
    traverseElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX {
        for (ElementValue elementValue : evai.elementValues) elementValue.accept(this.elementValueTraverser);
        this.traverseElementValue(evai);
    }

    /**
     * @throws EX
     * @see AbstractTraverser
     */
    @Override public void
    traverseElementValue(ElementValue ev) throws EX {}

    @Override public void
    traverseSingleElementAnnotation(SingleElementAnnotation sea) throws EX {
        sea.type.accept(this.atomTraverser);
        sea.elementValue.accept(this.elementValueTraverser);
        this.traverseAnnotation(sea);
    }

    /**
     * @throws EX
     * @see AbstractTraverser
     */
    @Override public void
    traverseAnnotation(Annotation a) throws EX {}

    @Override public void
    traverseNormalAnnotation(NormalAnnotation na) throws EX {
        na.type.accept(this.atomTraverser);
        for (ElementValuePair elementValuePair : na.elementValuePairs) {
            elementValuePair.elementValue.accept(this.elementValueTraverser);
        }
        this.traverseAnnotation(na);
    }

    @Override public void
    traverseMarkerAnnotation(MarkerAnnotation ma) throws EX {
        ma.type.accept(this.atomTraverser);
        this.traverseAnnotation(ma);
    }

    @Override public void
    traverseClassDeclaration(AbstractClassDeclaration cd) throws EX {
        for (ConstructorDeclarator ctord : cd.constructors) ctord.accept(this.typeBodyDeclarationTraverser);
        for (BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            vdoi.accept(this.blockStatementTraverser);
        }
        this.traverseAbstractTypeDeclaration(cd);
    }

    @Override public void
    traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd) throws EX {
        for (Annotation a : atd.getAnnotations()) this.traverseAnnotation(a);
        for (NamedTypeDeclaration mtd : atd.getMemberTypeDeclarations()) mtd.accept(this.typeDeclarationTraverser);
        for (MethodDeclarator md : atd.getMethodDeclarations()) this.traverseMethodDeclarator(md);
    }

    @Override public void
    traverseNamedClassDeclaration(NamedClassDeclaration ncd) throws EX {
        for (Type implementedType : ncd.implementedTypes) {
            implementedType.accept(this.atomTraverser);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept(this.atomTraverser);
        this.traverseClassDeclaration(ncd);
    }

    @Override public void
    traverseInterfaceDeclaration(InterfaceDeclaration id) throws EX {
        for (TypeBodyDeclaration cd : id.constantDeclarations) cd.accept(this.typeBodyDeclarationTraverser);
        for (Type extendedType : id.extendedTypes) extendedType.accept(this.atomTraverser);
        this.traverseAbstractTypeDeclaration(id);
    }

    @Override public void
    traverseFunctionDeclarator(FunctionDeclarator fd) throws EX {
        this.traverseFormalParameters(fd.formalParameters);
        if (fd.statements != null) {
            for (BlockStatement bs : fd.statements) bs.accept(this.blockStatementTraverser);
        }
    }

    @Override public void
    traverseFormalParameters(FunctionDeclarator.FormalParameters formalParameters) throws EX {
        for (FunctionDeclarator.FormalParameter formalParameter : formalParameters.parameters) {
            this.traverseFormalParameter(formalParameter);
        }
    }

    @Override public void
    traverseFormalParameter(FunctionDeclarator.FormalParameter formalParameter) throws EX {
        formalParameter.type.accept(this.atomTraverser);
    }

    @Override public void
    traverseAbstractTypeBodyDeclaration(AbstractTypeBodyDeclaration atbd) throws EX { this.traverseLocated(atbd); }

    @Override public void
    traverseStatement(Statement s) throws EX { this.traverseLocated(s); }

    @Override public void
    traverseBreakableStatement(BreakableStatement bs) throws EX { this.traverseStatement(bs); }

    @Override public void
    traverseContinuableStatement(ContinuableStatement cs) throws EX { this.traverseBreakableStatement(cs); }

    @Override public void
    traverseRvalue(Rvalue rv) throws EX { this.traverseAtom(rv); }

    @Override public void
    traverseBooleanRvalue(BooleanRvalue brv) throws EX { this.traverseRvalue(brv); }

    @Override public void
    traverseInvocation(Invocation i) throws EX {
        for (Rvalue argument : i.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(i);
    }

    @Override public void
    traverseConstructorInvocation(ConstructorInvocation ci) throws EX {
        for (Rvalue argument : ci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseAtom(ci);
    }

    @Override public void
    traverseEnumConstant(EnumConstant ec) throws EX {

        for (ConstructorDeclarator cd : ec.constructors) this.traverseConstructorDeclarator(cd);

        if (ec.optionalArguments != null) {
            for (Rvalue a : ec.optionalArguments) this.traverseRvalue(a);
        }

        this.traverseAbstractTypeDeclaration(ec);
    }

    @Override public void
    traversePackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) throws EX {
        this.traversePackageMemberClassDeclaration(pmed);
    }

    @Override public void
    traverseMemberEnumDeclaration(MemberEnumDeclaration med) throws EX {
        this.traverseMemberClassDeclaration(med);
    }

    @Override public void
    traversePackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX {
        this.traversePackageMemberInterfaceDeclaration(pmatd);
    }

    @Override public void
    traverseMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX {
        this.traverseMemberInterfaceDeclaration(matd);
    }

    @Override public void
    traverseLvalue(Lvalue lv) throws EX { this.traverseRvalue(lv); }

    @Override public void
    traverseType(Type t) throws EX { this.traverseAtom(t); }

    @Override public void
    traverseAtom(Atom a) throws EX { this.traverseLocated(a); }

    @SuppressWarnings("unused") @Override public void
    traverseLocated(Located l) throws EX {}

    @Override public void
    traverseLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws EX {
        lvdr.type.accept(this.atomTraverser);
        ArrayInitializerOrRvalue i = lvdr.variableDeclarator.optionalInitializer;
        if (i != null) this.traverseArrayInitializerOrRvalue(i);
    }

    @Override public void
    traverseVariableAccessResource(VariableAccessResource var) throws EX {
        var.variableAccess.accept(this.rvalueTraverser);
    }
}
