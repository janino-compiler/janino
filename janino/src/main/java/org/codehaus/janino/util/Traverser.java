
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

import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractPackageMemberClassDeclaration;
import org.codehaus.janino.Java.AbstractTypeBodyDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
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
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.WhileStatement;

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
 *         void
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
interface Traverser<EX extends Throwable> {

    /**
     * @see Traverser
     */
    void
    visitImportDeclaration(CompilationUnit.ImportDeclaration id) throws EX;

    /**
     * @see Traverser
     */
    void
    visitTypeDeclaration(TypeDeclaration td) throws EX;

    /**
     * @see Traverser
     */
    void
    visitTypeBodyDeclaration(TypeBodyDeclaration tbd) throws EX;

    /**
     * @see Traverser
     */
    void
    visitBlockStatement(BlockStatement bs) throws EX;

    /**
     * @see Traverser
     */
    void
    visitAtom(Atom a) throws EX;

    /**
     * @see Traverser
     */
    void
    visitElementValue(ElementValue ev) throws EX;

    /**
     * @see Traverser
     */
    void
    visitAnnotation(Annotation a) throws EX;

    // These may be overridden by derived classes.

    /**
     * The optionalPackageDeclaration is considered an integral part of the compilation unit and is
     * thus not traversed.
     *
     * @see Traverser
     */
    void
    traverseCompilationUnit(CompilationUnit cu) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration stid) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseImportDeclaration(CompilationUnit.ImportDeclaration id) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAnonymousClassDeclaration(AnonymousClassDeclaration acd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLocalClassDeclaration(LocalClassDeclaration lcd) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration pmcd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMemberClassDeclaration(MemberClassDeclaration mcd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseConstructorDeclarator(ConstructorDeclarator cd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseInitializer(Initializer i) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMethodDeclarator(MethodDeclarator md) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFieldDeclaration(FieldDeclaration fd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLabeledStatement(LabeledStatement ls) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBlock(Block b) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseExpressionStatement(ExpressionStatement es) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseIfStatement(IfStatement is) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseForStatement(ForStatement fs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseForEachStatement(ForEachStatement fes) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseWhileStatement(WhileStatement ws) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseTryStatement(TryStatement ts) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSwitchStatement(SwitchStatement ss) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSynchronizedStatement(SynchronizedStatement ss) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseDoStatement(DoStatement ds) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseReturnStatement(ReturnStatement rs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseThrowStatement(ThrowStatement ts) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBreakStatement(BreakStatement bs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseContinueStatement(ContinueStatement cs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAssertStatement(AssertStatement as) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseEmptyStatement(EmptyStatement es) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePackage(Package p) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseArrayLength(ArrayLength al) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAssignment(Assignment a) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseUnaryOperation(UnaryOperation uo) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBinaryOperation(BinaryOperation bo) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseCast(Cast c) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseClassLiteral(ClassLiteral cl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseConditionalExpression(ConditionalExpression ce) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseCrement(Crement c) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseInstanceof(Instanceof io) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMethodInvocation(MethodInvocation mi) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLiteral(Literal l) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseIntegerLiteral(IntegerLiteral il) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFloatingPointLiteral(FloatingPointLiteral fpl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBooleanLiteral(BooleanLiteral bl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseCharacterLiteral(CharacterLiteral cl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseStringLiteral(StringLiteral sl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNullLiteral(NullLiteral nl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSimpleLiteral(SimpleConstant sl) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNewAnonymousClassInstance(NewAnonymousClassInstance naci) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNewArray(NewArray na) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNewInitializedArray(NewInitializedArray nia) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseArrayInitializerOrRvalue(ArrayInitializerOrRvalue aiorv) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNewClassInstance(NewClassInstance nci) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseParameterAccess(ParameterAccess pa) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseQualifiedThisReference(QualifiedThisReference qtr) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseThisReference(ThisReference tr) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseArrayType(ArrayType at) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePrimitiveType(PrimitiveType bt) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseReferenceType(ReferenceType rt) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseRvalueMemberType(RvalueMemberType rmt) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSimpleType(SimpleType st) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSuperConstructorInvocation(SuperConstructorInvocation sci) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAmbiguousName(AmbiguousName an) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseArrayAccessExpression(ArrayAccessExpression aae) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFieldAccess(FieldAccess fa) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFieldAccessExpression(FieldAccessExpression fae) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLocalVariableAccess(LocalVariableAccess lva) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseParenthesizedExpression(ParenthesizedExpression pe) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX;

    /**
     * @throws EX
     * @see Traverser
     */
    void
    traverseElementValue(ElementValue ev) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseSingleElementAnnotation(SingleElementAnnotation sea) throws EX;

    /**
     * @throws EX
     * @see Traverser
     */
    void
    traverseAnnotation(Annotation a) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNormalAnnotation(NormalAnnotation na) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMarkerAnnotation(MarkerAnnotation ma) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseClassDeclaration(AbstractClassDeclaration cd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAbstractTypeDeclaration(AbstractTypeDeclaration atd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseNamedClassDeclaration(NamedClassDeclaration ncd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseInterfaceDeclaration(InterfaceDeclaration id) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFunctionDeclarator(FunctionDeclarator fd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFormalParameters(FunctionDeclarator.FormalParameters formalParameters) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseFormalParameter(FunctionDeclarator.FormalParameter formalParameter) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAbstractTypeBodyDeclaration(AbstractTypeBodyDeclaration atbd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseStatement(Statement s) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBreakableStatement(BreakableStatement bs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseContinuableStatement(ContinuableStatement cs) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseRvalue(Rvalue rv) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseBooleanRvalue(BooleanRvalue brv) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseInvocation(Invocation i) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseConstructorInvocation(ConstructorInvocation ci) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseEnumConstant(EnumConstant ec) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMemberEnumDeclaration(MemberEnumDeclaration med) throws EX;

    /**
     * @see Traverser
     */
    void
    traversePackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseLvalue(Lvalue lv) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseType(Type t) throws EX;

    /**
     * @see Traverser
     */
    void
    traverseAtom(Atom a) throws EX;

    /**
     * @see Traverser
     */
    @SuppressWarnings("unused") void
    traverseLocated(Located l) throws EX;
}
