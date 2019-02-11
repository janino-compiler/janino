
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

// SUPPRESS CHECKSTYLE JavadocMethod:9999

package org.codehaus.janino.util;

import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.AbstractTypeBodyDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
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
import org.codehaus.janino.Java.NamedClassDeclaration;
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
import org.codehaus.janino.Java.WhileStatement;

/**
 * Implementations of this interface promise to traverses the subnodes of an AST. Derived classes override individual
 * "{@code traverse*()}" methods to process specific nodes.
 *
 * @param <EX> The exception that the "{@code traverse*()}" and "{@code visit*()}" methods may throw
 */
public
interface Traverser<EX extends Throwable> {

    void visitImportDeclaration(CompilationUnit.ImportDeclaration id) throws EX;
    void visitTypeDeclaration(TypeDeclaration td)                     throws EX;
    void visitTypeBodyDeclaration(TypeBodyDeclaration tbd)            throws EX;
    void visitBlockStatement(BlockStatement bs)                       throws EX;
    void visitAtom(Atom a)                                            throws EX;
    void visitElementValue(ElementValue ev)                           throws EX;
    void visitAnnotation(Annotation a)                                throws EX;

    // These may be overridden by derived classes.

    /**
     * The optionalPackageDeclaration is considered an integral part of the compilation unit and is
     * thus not traversed.
     */
    void traverseCompilationUnit(CompilationUnit cu)                                                    throws EX;
    void traverseSingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid)          throws EX;
    void traverseTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd)     throws EX;
    void traverseSingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration stid)      throws EX;
    void traverseStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX;
    void traverseImportDeclaration(CompilationUnit.ImportDeclaration id)                                throws EX;
    void traverseAnonymousClassDeclaration(AnonymousClassDeclaration acd)                               throws EX;
    void traverseLocalClassDeclaration(LocalClassDeclaration lcd)                                       throws EX;
    void traversePackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)              throws EX;
    void traverseMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                             throws EX;
    void traversePackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)              throws EX;
    void traverseMemberClassDeclaration(MemberClassDeclaration mcd)                                     throws EX;
    void traverseConstructorDeclarator(ConstructorDeclarator cd)                                        throws EX;
    void traverseInitializer(Initializer i)                                                             throws EX;
    void traverseMethodDeclarator(MethodDeclarator md)                                                  throws EX;
    void traverseFieldDeclaration(FieldDeclaration fd)                                                  throws EX;
    void traverseLabeledStatement(LabeledStatement ls)                                                  throws EX;
    void traverseBlock(Block b)                                                                         throws EX;
    void traverseExpressionStatement(ExpressionStatement es)                                            throws EX;
    void traverseIfStatement(IfStatement is)                                                            throws EX;
    void traverseForStatement(ForStatement fs)                                                          throws EX;
    void traverseForEachStatement(ForEachStatement fes)                                                 throws EX;
    void traverseWhileStatement(WhileStatement ws)                                                      throws EX;
    void traverseTryStatement(TryStatement ts)                                                          throws EX;
    void traverseSwitchStatement(SwitchStatement ss)                                                    throws EX;
    void traverseSynchronizedStatement(SynchronizedStatement ss)                                        throws EX;
    void traverseDoStatement(DoStatement ds)                                                            throws EX;
    void traverseLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds)              throws EX;
    void traverseReturnStatement(ReturnStatement rs)                                                    throws EX;
    void traverseThrowStatement(ThrowStatement ts)                                                      throws EX;
    void traverseBreakStatement(BreakStatement bs)                                                      throws EX;
    void traverseContinueStatement(ContinueStatement cs)                                                throws EX;
    void traverseAssertStatement(AssertStatement as)                                                    throws EX;
    void traverseEmptyStatement(EmptyStatement es)                                                      throws EX;
    void traverseLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)                    throws EX;
    void traversePackage(Package p)                                                                     throws EX;
    void traverseArrayLength(ArrayLength al)                                                            throws EX;
    void traverseAssignment(Assignment a)                                                               throws EX;
    void traverseUnaryOperation(UnaryOperation uo)                                                      throws EX;
    void traverseBinaryOperation(BinaryOperation bo)                                                    throws EX;
    void traverseCast(Cast c)                                                                           throws EX;
    void traverseClassLiteral(ClassLiteral cl)                                                          throws EX;
    void traverseConditionalExpression(ConditionalExpression ce)                                        throws EX;
    void traverseCrement(Crement c)                                                                     throws EX;
    void traverseInstanceof(Instanceof io)                                                              throws EX;
    void traverseMethodInvocation(MethodInvocation mi)                                                  throws EX;
    void traverseSuperclassMethodInvocation(SuperclassMethodInvocation smi)                             throws EX;
    void traverseLiteral(Literal l)                                                                     throws EX;
    void traverseIntegerLiteral(IntegerLiteral il)                                                      throws EX;
    void traverseFloatingPointLiteral(FloatingPointLiteral fpl)                                         throws EX;
    void traverseBooleanLiteral(BooleanLiteral bl)                                                      throws EX;
    void traverseCharacterLiteral(CharacterLiteral cl)                                                  throws EX;
    void traverseStringLiteral(StringLiteral sl)                                                        throws EX;
    void traverseNullLiteral(NullLiteral nl)                                                            throws EX;
    void traverseSimpleLiteral(SimpleConstant sl)                                                       throws EX;
    void traverseNewAnonymousClassInstance(NewAnonymousClassInstance naci)                              throws EX;
    void traverseNewArray(NewArray na)                                                                  throws EX;
    void traverseNewInitializedArray(NewInitializedArray nia)                                           throws EX;
    void traverseArrayInitializerOrRvalue(ArrayInitializerOrRvalue aiorv)                               throws EX;
    void traverseNewClassInstance(NewClassInstance nci)                                                 throws EX;
    void traverseParameterAccess(ParameterAccess pa)                                                    throws EX;
    void traverseQualifiedThisReference(QualifiedThisReference qtr)                                     throws EX;
    void traverseThisReference(ThisReference tr)                                                        throws EX;
    void traverseLambdaExpression(LambdaExpression le)                                                  throws EX;
    void traverseMethodReference(MethodReference mr)                                                    throws EX;
    void traverseClassInstanceCreationReference(ClassInstanceCreationReference cicr)                    throws EX;
    void traverseArrayCreationReference(ArrayCreationReference acr)                                     throws EX;
    void traverseArrayType(ArrayType at)                                                                throws EX;
    void traversePrimitiveType(PrimitiveType bt)                                                        throws EX;
    void traverseReferenceType(ReferenceType rt)                                                        throws EX;
    void traverseRvalueMemberType(RvalueMemberType rmt)                                                 throws EX;
    void traverseSimpleType(SimpleType st)                                                              throws EX;
    void traverseAlternateConstructorInvocation(AlternateConstructorInvocation aci)                     throws EX;
    void traverseSuperConstructorInvocation(SuperConstructorInvocation sci)                             throws EX;
    void traverseAmbiguousName(AmbiguousName an)                                                        throws EX;
    void traverseArrayAccessExpression(ArrayAccessExpression aae)                                       throws EX;
    void traverseFieldAccess(FieldAccess fa)                                                            throws EX;
    void traverseFieldAccessExpression(FieldAccessExpression fae)                                       throws EX;
    void traverseSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae)                 throws EX;
    void traverseLocalVariableAccess(LocalVariableAccess lva)                                           throws EX;
    void traverseParenthesizedExpression(ParenthesizedExpression pe)                                    throws EX;
    void traverseElementValueArrayInitializer(ElementValueArrayInitializer evai)                        throws EX;
    void traverseElementValue(ElementValue ev)                                                          throws EX;
    void traverseSingleElementAnnotation(SingleElementAnnotation sea)                                   throws EX;
    void traverseAnnotation(Annotation a)                                                               throws EX;
    void traverseNormalAnnotation(NormalAnnotation na)                                                  throws EX;
    void traverseMarkerAnnotation(MarkerAnnotation ma)                                                  throws EX;
    void traverseClassDeclaration(AbstractClassDeclaration cd)                                          throws EX;
    void traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd)                                   throws EX;
    void traverseNamedClassDeclaration(NamedClassDeclaration ncd)                                       throws EX;
    void traverseInterfaceDeclaration(InterfaceDeclaration id)                                          throws EX;
    void traverseFunctionDeclarator(FunctionDeclarator fd)                                              throws EX;
    void traverseFormalParameters(FunctionDeclarator.FormalParameters formalParameters)                 throws EX;
    void traverseFormalParameter(FunctionDeclarator.FormalParameter formalParameter)                    throws EX;
    void traverseAbstractTypeBodyDeclaration(AbstractTypeBodyDeclaration atbd)                          throws EX;
    void traverseStatement(Statement s)                                                                 throws EX;
    void traverseBreakableStatement(BreakableStatement bs)                                              throws EX;
    void traverseContinuableStatement(ContinuableStatement cs)                                          throws EX;
    void traverseRvalue(Rvalue rv)                                                                      throws EX;
    void traverseBooleanRvalue(BooleanRvalue brv)                                                       throws EX;
    void traverseInvocation(Invocation i)                                                               throws EX;
    void traverseConstructorInvocation(ConstructorInvocation ci)                                        throws EX;
    void traverseEnumConstant(EnumConstant ec)                                                          throws EX;
    void traversePackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                        throws EX;
    void traverseMemberEnumDeclaration(MemberEnumDeclaration med)                                       throws EX;
    void traversePackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd)   throws EX;
    void traverseMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd)                  throws EX;
    void traverseLvalue(Lvalue lv)                                                                      throws EX;
    void traverseType(Type t)                                                                           throws EX;
    void traverseAtom(Atom a)                                                                           throws EX;
    void traverseLocated(Located l)                                                                     throws EX;
    void traverseLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr)                  throws EX;
    void traverseVariableAccessResource(VariableAccessResource var)                                     throws EX;
}
