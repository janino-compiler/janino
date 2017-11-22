
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
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

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.InternalCompilerException;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractPackageMemberClassDeclaration;
import org.codehaus.janino.Java.AbstractTypeBodyDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
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
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
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

/**
 * This class traverses the subnodes of an AST. Derived classes override individual "{@code traverse*()}" methods to
 * process specific nodes.
 * <p>
 *   Example:
 * </p>
 * <pre>
 *     LocalClassDeclaration lcd = ...;
 *
 *     new Traverser() {
 *
 *         int n = 0;
 *
 *         protected void
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
 * @see #visitImportDeclaration(Java.CompilationUnit.ImportDeclaration)
 * @see #visitTypeBodyDeclaration(Java.TypeBodyDeclaration)
 * @see #visitTypeDeclaration(Java.TypeDeclaration)
 * @see #traverseCompilationUnit(Java.CompilationUnit)
 */
public
class Traverser<EX extends Throwable> {

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link ImportDeclaration}.
     */
    private final Visitor.ImportVisitor<Void, EX>
    importTraverser = new Visitor.ImportVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:4
        @Override @Nullable public Void visitSingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid)          throws EX { Traverser.this.traverseSingleTypeImportDeclaration(stid);      return null; }
        @Override @Nullable public Void visitTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd)     throws EX { Traverser.this.traverseTypeImportOnDemandDeclaration(tiodd);   return null; }
        @Override @Nullable public Void visitSingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration ssid)      throws EX { Traverser.this.traverseSingleStaticImportDeclaration(ssid);    return null; }
        @Override @Nullable public Void visitStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX { Traverser.this.traverseStaticImportOnDemandDeclaration(siodd); return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link TypeDeclaration}.
     */
    private final Visitor.TypeDeclarationVisitor<Void, EX>
    typeDeclarationTraverser = new Visitor.TypeDeclarationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:11
        @Override @Nullable public Void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws EX { Traverser.this.traverseAnonymousClassDeclaration(acd);                return null; }
        @Override @Nullable public Void visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws EX { Traverser.this.traverseLocalClassDeclaration(lcd);                    return null; }
        @Override @Nullable public Void visitPackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration apmcd)           throws EX { Traverser.this.traversePackageMemberClassDeclaration(apmcd);          return null; }
        @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws EX { Traverser.this.traversePackageMemberInterfaceDeclaration(pmid);       return null; }
        @Override @Nullable public Void visitEnumConstant(EnumConstant ec)                                                        throws EX { Traverser.this.traverseEnumConstant(ec);                              return null; }
        @Override @Nullable public Void visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws EX { Traverser.this.traversePackageMemberEnumDeclaration(pmed);            return null; }
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd)                throws EX { Traverser.this.traverseMemberAnnotationTypeDeclaration(matd);         return null; }
        @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX { Traverser.this.traversePackageMemberAnnotationTypeDeclaration(pmatd); return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws EX { Traverser.this.traverseMemberEnumDeclaration(med);                    return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws EX { Traverser.this.traverseMemberInterfaceDeclaration(mid);               return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws EX { Traverser.this.traverseMemberClassDeclaration(mcd);                   return null; }
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
                @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws EX { Traverser.this.traverseAmbiguousName(an);                      return null; }
                @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws EX { Traverser.this.traverseArrayAccessExpression(aae);             return null; }
                @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws EX { Traverser.this.traverseFieldAccess(fa);                        return null; }
                @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws EX { Traverser.this.traverseFieldAccessExpression(fae);             return null; }
                @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX { Traverser.this.traverseSuperclassFieldAccessExpression(scfae); return null; }
                @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws EX { Traverser.this.traverseLocalVariableAccess(lva);               return null; }
                @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws EX { Traverser.this.traverseParenthesizedExpression(pe);            return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:25
        @Override @Nullable public Void visitArrayLength(ArrayLength al)                                throws EX { Traverser.this.traverseArrayLength(al);                        return null; }
        @Override @Nullable public Void visitAssignment(Assignment a)                                   throws EX { Traverser.this.traverseAssignment(a);                          return null; }
        @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                          throws EX { Traverser.this.traverseUnaryOperation(uo);                     return null; }
        @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                        throws EX { Traverser.this.traverseBinaryOperation(bo);                    return null; }
        @Override @Nullable public Void visitCast(Cast c)                                               throws EX { Traverser.this.traverseCast(c);                                return null; }
        @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                              throws EX { Traverser.this.traverseClassLiteral(cl);                       return null; }
        @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)            throws EX { Traverser.this.traverseConditionalExpression(ce);              return null; }
        @Override @Nullable public Void visitCrement(Crement c)                                         throws EX { Traverser.this.traverseCrement(c);                             return null; }
        @Override @Nullable public Void visitInstanceof(Instanceof io)                                  throws EX { Traverser.this.traverseInstanceof(io);                         return null; }
        @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                      throws EX { Traverser.this.traverseMethodInvocation(mi);                   return null; }
        @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws EX { Traverser.this.traverseSuperclassMethodInvocation(smi);        return null; }
        @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                          throws EX { Traverser.this.traverseIntegerLiteral(il);                     return null; }
        @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)             throws EX { Traverser.this.traverseFloatingPointLiteral(fpl);              return null; }
        @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                          throws EX { Traverser.this.traverseBooleanLiteral(bl);                     return null; }
        @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                      throws EX { Traverser.this.traverseCharacterLiteral(cl);                   return null; }
        @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                            throws EX { Traverser.this.traverseStringLiteral(sl);                      return null; }
        @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                throws EX { Traverser.this.traverseNullLiteral(nl);                        return null; }
        @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                          throws EX { Traverser.this.traverseSimpleLiteral(sl);                      return null; }
        @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  throws EX { Traverser.this.traverseNewAnonymousClassInstance(naci);        return null; }
        @Override @Nullable public Void visitNewArray(NewArray na)                                      throws EX { Traverser.this.traverseNewArray(na);                           return null; }
        @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)               throws EX { Traverser.this.traverseNewInitializedArray(nia);               return null; }
        @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                     throws EX { Traverser.this.traverseNewClassInstance(nci);                  return null; }
        @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                        throws EX { Traverser.this.traverseParameterAccess(pa);                    return null; }
        @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)         throws EX { Traverser.this.traverseQualifiedThisReference(qtr);            return null; }
        @Override @Nullable public Void visitThisReference(ThisReference tr)                            throws EX { Traverser.this.traverseThisReference(tr);                      return null; }
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
                @Override @Nullable public Void visitConstructorDeclarator(ConstructorDeclarator cd) throws EX { Traverser.this.traverseConstructorDeclarator(cd); return null; }
                @Override @Nullable public Void visitMethodDeclarator(MethodDeclarator md)           throws EX { Traverser.this.traverseMethodDeclarator(md);      return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:6
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX { Traverser.this.traverseMemberAnnotationTypeDeclaration(matd); return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)            throws EX { Traverser.this.traverseMemberInterfaceDeclaration(mid);       return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                    throws EX { Traverser.this.traverseMemberClassDeclaration(mcd);           return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                      throws EX { Traverser.this.traverseMemberEnumDeclaration(med);            return null; }
        @Override @Nullable public Void visitInitializer(Initializer i)                                            throws EX { Traverser.this.traverseInitializer(i);                        return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                 throws EX { Traverser.this.traverseFieldDeclaration(fd);                  return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link BlockStatement}.
     */
    private final Visitor.BlockStatementVisitor<Void, EX>
    blockStatementTraverser = new BlockStatementVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:23
        @Override @Nullable public Void visitInitializer(Initializer i)                                                throws EX { Traverser.this.traverseInitializer(i);                          return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                     throws EX { Traverser.this.traverseFieldDeclaration(fd);                    return null; }
        @Override @Nullable public Void visitLabeledStatement(LabeledStatement ls)                                     throws EX { Traverser.this.traverseLabeledStatement(ls);                    return null; }
        @Override @Nullable public Void visitBlock(Block b)                                                            throws EX { Traverser.this.traverseBlock(b);                                return null; }
        @Override @Nullable public Void visitExpressionStatement(ExpressionStatement es)                               throws EX { Traverser.this.traverseExpressionStatement(es);                 return null; }
        @Override @Nullable public Void visitIfStatement(IfStatement is)                                               throws EX { Traverser.this.traverseIfStatement(is);                         return null; }
        @Override @Nullable public Void visitForStatement(ForStatement fs)                                             throws EX { Traverser.this.traverseForStatement(fs);                        return null; }
        @Override @Nullable public Void visitForEachStatement(ForEachStatement fes)                                    throws EX { Traverser.this.traverseForEachStatement(fes);                   return null; }
        @Override @Nullable public Void visitWhileStatement(WhileStatement ws)                                         throws EX { Traverser.this.traverseWhileStatement(ws);                      return null; }
        @Override @Nullable public Void visitTryStatement(TryStatement ts)                                             throws EX { Traverser.this.traverseTryStatement(ts);                        return null; }
        @Override @Nullable public Void visitSwitchStatement(SwitchStatement ss)                                       throws EX { Traverser.this.traverseSwitchStatement(ss);                     return null; }
        @Override @Nullable public Void visitSynchronizedStatement(SynchronizedStatement ss)                           throws EX { Traverser.this.traverseSynchronizedStatement(ss);               return null; }
        @Override @Nullable public Void visitDoStatement(DoStatement ds)                                               throws EX { Traverser.this.traverseDoStatement(ds);                         return null; }
        @Override @Nullable public Void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX { Traverser.this.traverseLocalVariableDeclarationStatement(lvds); return null; }
        @Override @Nullable public Void visitReturnStatement(ReturnStatement rs)                                       throws EX { Traverser.this.traverseReturnStatement(rs);                     return null; }
        @Override @Nullable public Void visitThrowStatement(ThrowStatement ts)                                         throws EX { Traverser.this.traverseThrowStatement(ts);                      return null; }
        @Override @Nullable public Void visitBreakStatement(BreakStatement bs)                                         throws EX { Traverser.this.traverseBreakStatement(bs);                      return null; }
        @Override @Nullable public Void visitContinueStatement(ContinueStatement cs)                                   throws EX { Traverser.this.traverseContinueStatement(cs);                   return null; }
        @Override @Nullable public Void visitAssertStatement(AssertStatement as)                                       throws EX { Traverser.this.traverseAssertStatement(as);                     return null; }
        @Override @Nullable public Void visitEmptyStatement(EmptyStatement es)                                         throws EX { Traverser.this.traverseEmptyStatement(es);                      return null; }
        @Override @Nullable public Void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws EX { Traverser.this.traverseLocalClassDeclarationStatement(lcds);    return null; }
        @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws EX { Traverser.this.traverseAlternateConstructorInvocation(aci);     return null; }
        @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws EX { Traverser.this.traverseSuperConstructorInvocation(sci);         return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link Atom}.
     */
    private final Visitor.AtomVisitor<Void, EX>
    atomTraverser = new Visitor.AtomVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws EX {
            rv.accept(Traverser.this.rvalueTraverser);
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Package p) throws EX {
            Traverser.this.traversePackage(p);
            return null;
        }

        @Override @Nullable public Void
        visitType(Type t) throws EX {

            t.accept(new Visitor.TypeVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:5
                @Override @Nullable public Void visitArrayType(ArrayType at)                throws EX { Traverser.this.traverseArrayType(at);         return null; }
                @Override @Nullable public Void visitPrimitiveType(PrimitiveType bt)        throws EX { Traverser.this.traversePrimitiveType(bt);     return null; }
                @Override @Nullable public Void visitReferenceType(ReferenceType rt)        throws EX { Traverser.this.traverseReferenceType(rt);     return null; }
                @Override @Nullable public Void visitRvalueMemberType(RvalueMemberType rmt) throws EX { Traverser.this.traverseRvalueMemberType(rmt); return null; }
                @Override @Nullable public Void visitSimpleType(SimpleType st)              throws EX { Traverser.this.traverseSimpleType(st);        return null; }
            });
            return null;
        }

        @Override @Nullable public Void
        visitConstructorInvocation(ConstructorInvocation ci) throws EX {

            ci.accept(new Visitor.ConstructorInvocationVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX { Traverser.this.traverseAlternateConstructorInvocation(aci); return null; }
                @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)         throws EX { Traverser.this.traverseSuperConstructorInvocation(sci);     return null; }
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
            rv.accept(Traverser.this.rvalueTraverser);
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX { Traverser.this.traverseElementValueArrayInitializer(evai); return null; }
        @Override @Nullable public Void visitAnnotation(Annotation a)                                        throws EX { Traverser.this.traverseAnnotation(a);                      return null; }
    };

    /**
     * Invokes the "{@code traverse*()}" method for the concrete {@link Annotation}.
     */
    private final AnnotationVisitor<Void, EX>
    annotationTraverser = new AnnotationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override @Nullable public Void visitMarkerAnnotation(MarkerAnnotation ma)                throws EX { Traverser.this.traverseMarkerAnnotation(ma);         return null; }
        @Override @Nullable public Void visitNormalAnnotation(NormalAnnotation na)                throws EX { Traverser.this.traverseNormalAnnotation(na);         return null; }
        @Override @Nullable public Void visitSingleElementAnnotation(SingleElementAnnotation sea) throws EX { Traverser.this.traverseSingleElementAnnotation(sea); return null; }
    };

    /**
     * @see Traverser
     */
    public void
    visitImportDeclaration(CompilationUnit.ImportDeclaration id) throws EX {
        id.accept(Traverser.this.importTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitTypeDeclaration(TypeDeclaration td) throws EX {
        td.accept(Traverser.this.typeDeclarationTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitTypeBodyDeclaration(TypeBodyDeclaration tbd) throws EX {
        tbd.accept(Traverser.this.typeBodyDeclarationTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitBlockStatement(BlockStatement bs) throws EX {
        bs.accept(Traverser.this.blockStatementTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitAtom(Atom a) throws EX {
        a.accept(Traverser.this.atomTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitElementValue(ElementValue ev) throws EX {
        ev.accept(Traverser.this.elementValueTraverser);
    }

    /**
     * @see Traverser
     */
    public void
    visitAnnotation(Annotation a) throws EX {
        a.accept(Traverser.this.annotationTraverser);
    }

    // These may be overridden by derived classes.

    /**
     * @see Traverser
     */
    public void
    traverseCompilationUnit(CompilationUnit cu) throws EX {

        // The optionalPackageDeclaration is considered an integral part of
        // the compilation unit and is thus not traversed.

        for (CompilationUnit.ImportDeclaration id : cu.importDeclarations) {
            id.accept(this.importTraverser);
        }

        for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            pmtd.accept(this.typeDeclarationTraverser);
        }
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX {
        this.traverseImportDeclaration(tiodd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX {
        this.traverseImportDeclaration(siodd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseImportDeclaration(CompilationUnit.ImportDeclaration id) throws EX { this.traverseLocated(id); }

    /**
     * @see Traverser
     */
    protected void
    traverseAnonymousClassDeclaration(AnonymousClassDeclaration acd) throws EX {
        acd.baseType.accept(this.atomTraverser);
        this.traverseClassDeclaration(acd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseLocalClassDeclaration(LocalClassDeclaration lcd) throws EX { this.traverseNamedClassDeclaration(lcd); }

    /**
     * @see Traverser
     */
    protected void
    traversePackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration pmcd) throws EX {
        this.traverseNamedClassDeclaration(pmcd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws EX {
        this.traverseInterfaceDeclaration(mid);
    }

    /**
     * @see Traverser
     */
    protected void
    traversePackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid) throws EX {
        this.traverseInterfaceDeclaration(pmid);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMemberClassDeclaration(MemberClassDeclaration mcd) throws EX {
        this.traverseNamedClassDeclaration(mcd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseConstructorDeclarator(ConstructorDeclarator cd) throws EX {
        if (cd.optionalConstructorInvocation != null) {
            cd.optionalConstructorInvocation.accept(this.blockStatementTraverser);
        }
        this.traverseFunctionDeclarator(cd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseInitializer(Initializer i) throws EX {
        i.block.accept(this.blockStatementTraverser);
        this.traverseAbstractTypeBodyDeclaration(i);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMethodDeclarator(MethodDeclarator md) throws EX { this.traverseFunctionDeclarator(md); }

    /**
     * @see Traverser
     */
    protected void
    traverseFieldDeclaration(FieldDeclaration fd) throws EX {
        fd.type.accept(this.atomTraverser);
        for (VariableDeclarator vd : fd.variableDeclarators) {
            ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(fd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseLabeledStatement(LabeledStatement ls) throws EX {
        ls.body.accept(this.blockStatementTraverser);
        this.traverseBreakableStatement(ls);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseBlock(Block b) throws EX {
        for (BlockStatement bs : b.statements) bs.accept(this.blockStatementTraverser);
        this.traverseStatement(b);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseExpressionStatement(ExpressionStatement es) throws EX {
        es.rvalue.accept(this.rvalueTraverser);
        this.traverseStatement(es);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseIfStatement(IfStatement is) throws EX {
        is.condition.accept(this.rvalueTraverser);
        is.thenStatement.accept(this.blockStatementTraverser);
        if (is.optionalElseStatement != null) is.optionalElseStatement.accept(this.blockStatementTraverser);
        this.traverseStatement(is);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseForStatement(ForStatement fs) throws EX {
        if (fs.optionalInit != null) fs.optionalInit.accept(this.blockStatementTraverser);
        if (fs.optionalCondition != null) fs.optionalCondition.accept(this.rvalueTraverser);
        if (fs.optionalUpdate != null) {
            for (Rvalue rv : fs.optionalUpdate) rv.accept(this.rvalueTraverser);
        }
        fs.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fs);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseForEachStatement(ForEachStatement fes) throws EX {
        this.traverseFormalParameter(fes.currentElement);
        fes.expression.accept(this.rvalueTraverser);
        fes.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fes);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseWhileStatement(WhileStatement ws) throws EX {
        ws.condition.accept(this.rvalueTraverser);
        ws.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(ws);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseTryStatement(TryStatement ts) throws EX {
        ts.body.accept(this.blockStatementTraverser);
        for (CatchClause cc : ts.catchClauses) cc.body.accept(this.blockStatementTraverser);
        if (ts.optionalFinally != null) ts.optionalFinally.accept(this.blockStatementTraverser);
        this.traverseStatement(ts);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSwitchStatement(SwitchStatement ss) throws EX {
        ss.condition.accept(this.rvalueTraverser);
        for (SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (Rvalue cl : sbsg.caseLabels) cl.accept(this.rvalueTraverser);
            for (BlockStatement bs : sbsg.blockStatements) bs.accept(this.blockStatementTraverser);
            this.traverseLocated(sbsg);
        }
        this.traverseBreakableStatement(ss);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSynchronizedStatement(SynchronizedStatement ss) throws EX {
        ss.expression.accept(this.rvalueTraverser);
        ss.body.accept(this.blockStatementTraverser);
        this.traverseStatement(ss);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseDoStatement(DoStatement ds) throws EX {
        ds.body.accept(this.blockStatementTraverser);
        ds.condition.accept(this.rvalueTraverser);
        this.traverseContinuableStatement(ds);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX {
        lvds.type.accept(this.atomTraverser);
        for (VariableDeclarator vd : lvds.variableDeclarators) {
            ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(lvds);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseReturnStatement(ReturnStatement rs) throws EX {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept(this.rvalueTraverser);
        this.traverseStatement(rs);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseThrowStatement(ThrowStatement ts) throws EX {
        ts.expression.accept(this.rvalueTraverser);
        this.traverseStatement(ts);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseBreakStatement(BreakStatement bs) throws EX { this.traverseStatement(bs); }

    /**
     * @see Traverser
     */
    protected void
    traverseContinueStatement(ContinueStatement cs) throws EX { this.traverseStatement(cs); }

    /**
     * @see Traverser
     */
    protected void
    traverseAssertStatement(AssertStatement as) throws EX {
        as.expression1.accept(this.rvalueTraverser);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept(this.rvalueTraverser);
        this.traverseStatement(as);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseEmptyStatement(EmptyStatement es) throws EX { this.traverseStatement(es); }

    /**
     * @see Traverser
     */
    protected void
    traverseLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) throws EX {
        lcds.lcd.accept(this.typeDeclarationTraverser);
        this.traverseStatement(lcds);
    }

    /**
     * @see Traverser
     */
    protected void
    traversePackage(Package p) throws EX { this.traverseAtom(p); }

    /**
     * @see Traverser
     */
    protected void
    traverseArrayLength(ArrayLength al) throws EX {
        al.lhs.accept(this.rvalueTraverser);
        this.traverseRvalue(al);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseAssignment(Assignment a) throws EX {
        a.lhs.accept(this.rvalueTraverser);
        a.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(a);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseUnaryOperation(UnaryOperation uo) throws EX {
        uo.operand.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(uo);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseBinaryOperation(BinaryOperation bo) throws EX {
        bo.lhs.accept(this.rvalueTraverser);
        bo.rhs.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(bo);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseCast(Cast c) throws EX {
        c.targetType.accept(this.atomTraverser);
        c.value.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseClassLiteral(ClassLiteral cl) throws EX {
        cl.type.accept(this.atomTraverser);
        this.traverseRvalue(cl);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseConditionalExpression(ConditionalExpression ce) throws EX {
        ce.lhs.accept(this.rvalueTraverser);
        ce.mhs.accept(this.rvalueTraverser);
        ce.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(ce);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseCrement(Crement c) throws EX {
        c.operand.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseInstanceof(Instanceof io) throws EX {
        io.lhs.accept(this.rvalueTraverser);
        io.rhs.accept(this.atomTraverser);
        this.traverseRvalue(io);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMethodInvocation(MethodInvocation mi) throws EX {
        if (mi.optionalTarget != null) mi.optionalTarget.accept(this.atomTraverser);
        this.traverseInvocation(mi);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws EX { this.traverseInvocation(smi); }

    /**
     * @see Traverser
     */
    protected void
    traverseLiteral(Literal l) throws EX { this.traverseRvalue(l); }

    /**
     * @see Traverser
     */
    protected void
    traverseIntegerLiteral(IntegerLiteral il) throws EX { this.traverseLiteral(il); }

    /**
     * @see Traverser
     */
    protected void
    traverseFloatingPointLiteral(FloatingPointLiteral fpl) throws EX { this.traverseLiteral(fpl); }

    /**
     * @see Traverser
     */
    protected void
    traverseBooleanLiteral(BooleanLiteral bl) throws EX { this.traverseLiteral(bl); }

    /**
     * @see Traverser
     */
    protected void
    traverseCharacterLiteral(CharacterLiteral cl) throws EX { this.traverseLiteral(cl); }

    /**
     * @see Traverser
     */
    protected void
    traverseStringLiteral(StringLiteral sl) throws EX { this.traverseLiteral(sl); }

    /**
     * @see Traverser
     */
    protected void
    traverseNullLiteral(NullLiteral nl) throws EX { this.traverseLiteral(nl); }

    /**
     * @see Traverser
     */
    protected void
    traverseSimpleLiteral(SimpleConstant sl) throws EX { this.traverseRvalue(sl); }

    /**
     * @see Traverser
     */
    protected void
    traverseNewAnonymousClassInstance(NewAnonymousClassInstance naci) throws EX {
        if (naci.optionalQualification != null) {
            naci.optionalQualification.accept(this.rvalueTraverser);
        }
        naci.anonymousClassDeclaration.accept(this.typeDeclarationTraverser);
        for (Rvalue argument : naci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(naci);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseNewArray(NewArray na) throws EX {
        na.type.accept(this.atomTraverser);
        for (Rvalue dimExpr : na.dimExprs) dimExpr.accept(this.rvalueTraverser);
        this.traverseRvalue(na);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseNewInitializedArray(NewInitializedArray nia) throws EX {
        assert nia.arrayType != null;
        nia.arrayType.accept(this.atomTraverser);
        this.traverseArrayInitializerOrRvalue(nia.arrayInitializer);
    }

    /**
     * @see Traverser
     */
    protected void
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

    /**
     * @see Traverser
     */
    protected void
    traverseNewClassInstance(NewClassInstance nci) throws EX {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.accept(this.rvalueTraverser);
        }
        if (nci.type != null) nci.type.accept(this.atomTraverser);
        for (Rvalue argument : nci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(nci);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseParameterAccess(ParameterAccess pa) throws EX { this.traverseRvalue(pa); }

    /**
     * @see Traverser
     */
    protected void
    traverseQualifiedThisReference(QualifiedThisReference qtr) throws EX {
        qtr.qualification.accept(this.atomTraverser);
        this.traverseRvalue(qtr);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseThisReference(ThisReference tr) throws EX { this.traverseRvalue(tr); }

    /**
     * @see Traverser
     */
    protected void
    traverseArrayType(ArrayType at) throws EX {
        at.componentType.accept(this.atomTraverser);
        this.traverseType(at);
    }

    /**
     * @see Traverser
     */
    protected void
    traversePrimitiveType(PrimitiveType bt) throws EX { this.traverseType(bt); }

    /**
     * @see Traverser
     */
    protected void
    traverseReferenceType(ReferenceType rt) throws EX { this.traverseType(rt); }

    /**
     * @see Traverser
     */
    protected void
    traverseRvalueMemberType(RvalueMemberType rmt) throws EX {
        rmt.rvalue.accept(this.rvalueTraverser);
        this.traverseType(rmt);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSimpleType(SimpleType st) throws EX { this.traverseType(st); }

    /**
     * @see Traverser
     */
    protected void
    traverseAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX {
        this.traverseConstructorInvocation(aci);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSuperConstructorInvocation(SuperConstructorInvocation sci) throws EX {
        if (sci.optionalQualification != null) {
            sci.optionalQualification.accept(this.rvalueTraverser);
        }
        this.traverseConstructorInvocation(sci);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseAmbiguousName(AmbiguousName an) throws EX { this.traverseLvalue(an); }

    /**
     * @see Traverser
     */
    protected void
    traverseArrayAccessExpression(ArrayAccessExpression aae) throws EX {
        aae.lhs.accept(this.rvalueTraverser);
        aae.index.accept(this.atomTraverser);
        this.traverseLvalue(aae);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseFieldAccess(FieldAccess fa) throws EX {
        fa.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fa);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseFieldAccessExpression(FieldAccessExpression fae) throws EX {
        fae.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fae);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX {
        if (scfae.optionalQualification != null) {
            scfae.optionalQualification.accept(this.atomTraverser);
        }
        this.traverseLvalue(scfae);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseLocalVariableAccess(LocalVariableAccess lva) throws EX { this.traverseLvalue(lva); }

    /**
     * @see Traverser
     */
    protected void
    traverseParenthesizedExpression(ParenthesizedExpression pe) throws EX {
        pe.value.accept(this.rvalueTraverser);
        this.traverseLvalue(pe);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX {
        for (ElementValue elementValue : evai.elementValues) elementValue.accept(this.elementValueTraverser);
        this.traverseElementValue(evai);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    protected void
    traverseElementValue(ElementValue ev) throws EX {}

    /**
     * @see Traverser
     */
    protected void
    traverseSingleElementAnnotation(SingleElementAnnotation sea) throws EX {
        sea.type.accept(this.atomTraverser);
        sea.elementValue.accept(this.elementValueTraverser);
        this.traverseAnnotation(sea);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    protected void
    traverseAnnotation(Annotation a) throws EX {}

    /**
     * @see Traverser
     */
    protected void
    traverseNormalAnnotation(NormalAnnotation na) throws EX {
        na.type.accept(this.atomTraverser);
        for (ElementValuePair elementValuePair : na.elementValuePairs) {
            elementValuePair.elementValue.accept(this.elementValueTraverser);
        }
        this.traverseAnnotation(na);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMarkerAnnotation(MarkerAnnotation ma) throws EX {
        ma.type.accept(this.atomTraverser);
        this.traverseAnnotation(ma);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseClassDeclaration(AbstractClassDeclaration cd) throws EX {
        for (ConstructorDeclarator ctord : cd.constructors) ctord.accept(this.typeBodyDeclarationTraverser);
        for (BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            vdoi.accept(this.blockStatementTraverser);
        }
        this.traverseAbstractTypeDeclaration(cd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd) throws EX {
        for (Annotation a : atd.getAnnotations()) this.traverseAnnotation(a);
        for (NamedTypeDeclaration mtd : atd.getMemberTypeDeclarations()) mtd.accept(this.typeDeclarationTraverser);
        for (MethodDeclarator md : atd.getMethodDeclarations()) this.traverseMethodDeclarator(md);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseNamedClassDeclaration(NamedClassDeclaration ncd) throws EX {
        for (Type implementedType : ncd.implementedTypes) {
            implementedType.accept(this.atomTraverser);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept(this.atomTraverser);
        this.traverseClassDeclaration(ncd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseInterfaceDeclaration(InterfaceDeclaration id) throws EX {
        for (TypeBodyDeclaration cd : id.constantDeclarations) cd.accept(this.typeBodyDeclarationTraverser);
        for (Type extendedType : id.extendedTypes) extendedType.accept(this.atomTraverser);
        this.traverseAbstractTypeDeclaration(id);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseFunctionDeclarator(FunctionDeclarator fd) throws EX {
        this.traverseFormalParameters(fd.formalParameters);
        if (fd.optionalStatements != null) {
            for (BlockStatement bs : fd.optionalStatements) bs.accept(this.blockStatementTraverser);
        }
    }

    /**
     * @see Traverser
     */
    protected void
    traverseFormalParameters(FunctionDeclarator.FormalParameters formalParameters) throws EX {
        for (FunctionDeclarator.FormalParameter formalParameter : formalParameters.parameters) {
            this.traverseFormalParameter(formalParameter);
        }
    }

    /**
     * @see Traverser
     */
    protected void
    traverseFormalParameter(FunctionDeclarator.FormalParameter formalParameter) throws EX {
        formalParameter.type.accept(this.atomTraverser);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseAbstractTypeBodyDeclaration(AbstractTypeBodyDeclaration atbd) throws EX { this.traverseLocated(atbd); }

    /**
     * @see Traverser
     */
    protected void
    traverseStatement(Statement s) throws EX { this.traverseLocated(s); }

    /**
     * @see Traverser
     */
    protected void
    traverseBreakableStatement(BreakableStatement bs) throws EX { this.traverseStatement(bs); }

    /**
     * @see Traverser
     */
    protected void
    traverseContinuableStatement(ContinuableStatement cs) throws EX { this.traverseBreakableStatement(cs); }

    /**
     * @see Traverser
     */
    protected void
    traverseRvalue(Rvalue rv) throws EX { this.traverseAtom(rv); }

    /**
     * @see Traverser
     */
    protected void
    traverseBooleanRvalue(BooleanRvalue brv) throws EX { this.traverseRvalue(brv); }

    /**
     * @see Traverser
     */
    protected void
    traverseInvocation(Invocation i) throws EX {
        for (Rvalue argument : i.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(i);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseConstructorInvocation(ConstructorInvocation ci) throws EX {
        for (Rvalue argument : ci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseAtom(ci);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseEnumConstant(EnumConstant ec) throws EX {

        for (ConstructorDeclarator cd : ec.constructors) this.traverseConstructorDeclarator(cd);

        if (ec.optionalArguments != null) {
            for (Rvalue a : ec.optionalArguments) this.traverseRvalue(a);
        }

        this.traverseAbstractTypeDeclaration(ec);
    }

    /**
     * @see Traverser
     */
    protected void
    traversePackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) throws EX {
        this.traversePackageMemberClassDeclaration(pmed);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMemberEnumDeclaration(MemberEnumDeclaration med) throws EX {
        this.traverseMemberClassDeclaration(med);
    }

    /**
     * @see Traverser
     */
    protected void
    traversePackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX {
        this.traversePackageMemberInterfaceDeclaration(pmatd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX {
        this.traverseMemberInterfaceDeclaration(matd);
    }

    /**
     * @see Traverser
     */
    protected void
    traverseLvalue(Lvalue lv) throws EX { this.traverseRvalue(lv); }

    /**
     * @see Traverser
     */
    protected void
    traverseType(Type t) throws EX { this.traverseAtom(t); }

    /**
     * @see Traverser
     */
    protected void
    traverseAtom(Atom a) throws EX { this.traverseLocated(a); }

    /**
     * @see Traverser
     */
    @SuppressWarnings("unused") protected void
    traverseLocated(Located l) throws EX {}
}
