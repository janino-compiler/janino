
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

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractPackageMemberClassDeclaration;

/**
 * The basis for the "visitor" pattern as described in "Gamma, Helm, Johnson, Vlissides: Design Patterns".
 */
public final
class Visitor {

    private Visitor() {}

    /**
     * The union of {@link ImportVisitor}, {@link TypeDeclarationVisitor}, {@link TypeBodyDeclarationVisitor} and
     * {@link AtomVisitor}.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    // SUPPRESS CHECKSTYLE WrapAndIndent:4
    public
    interface ComprehensiveVisitor<R, EX extends Throwable>
    extends ElementValueVisitor<R, EX>, AnnotationVisitor<R, EX> {

        /** Invoked by {@link Java.CompilationUnit.ImportDeclaration#accept(ImportVisitor)}. */
        @Nullable R
        visitImportDeclaration(Java.CompilationUnit.ImportDeclaration id) throws EX;

        /** Invoked by {@link Java.CompilationUnit.TypeDeclaration#accept(TypeDeclarationVisitor)}. */
        @Nullable R
        visitTypeDeclaration(Java.TypeDeclaration td) throws EX;

        /** Invoked by {@link Java.TypeBodyDeclaration#accept(TypeBodyDeclarationVisitor)}. */
        @Nullable R
        visitTypeBodyDeclaration(Java.TypeBodyDeclaration tbd) throws EX;

        /** Invoked by {@link Java.BlockStatement#accept(BlockStatementVisitor)}. */
        @Nullable R
        visitBlockStatement(Java.BlockStatement bs) throws EX;

        /** Invoked by {@link Java.Atom#accept(AtomVisitor)}. */
        @Nullable R
        visitAtom(Java.Atom a) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.CompilationUnit.ImportDeclaration}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ImportVisitor<R, EX extends Throwable> {

        /** Invoked by {@link Java.CompilationUnit.SingleTypeImportDeclaration#accept(Visitor.ImportVisitor)} */
        @Nullable R
        visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX;

        /** Invoked by {@link Java.CompilationUnit.TypeImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        @Nullable R
        visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX;

        /** Invoked by {@link Java.CompilationUnit.SingleStaticImportDeclaration#accept(Visitor.ImportVisitor)} */
        @Nullable R
        visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) throws EX;

        /** Invoked by {@link Java.CompilationUnit.StaticImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        @Nullable R
        visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeDeclaration}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeDeclarationVisitor<R, EX extends Throwable> {

        // SUPPRESS CHECKSTYLE LineLength:22
        /** Invoked by {@link Java.AnonymousClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX;
        /** Invoked by {@link Java.LocalClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX;
        /** Invoked by {@link Java.PackageMemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitPackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration apmcd) throws EX;
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX;
        /** Invoked by {@link Java.PackageMemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX;
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX;
        /** Invoked by {@link Java.EnumConstant#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitEnumConstant(Java.EnumConstant ec) throws EX;
        /** Invoked by {@link Java.MemberEnumDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX;
        /** Invoked by {@link Java.PackageMemberEnumDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitPackageMemberEnumDeclaration(Java.PackageMemberEnumDeclaration pmed) throws EX;
        /** Invoked by {@link Java.MemberAnnotationTypeDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitMemberAnnotationTypeDeclaration(Java.MemberAnnotationTypeDeclaration matd) throws EX;
        /** Invoked by {@link Java.PackageMemberAnnotationTypeDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        @Nullable R visitPackageMemberAnnotationTypeDeclaration(Java.PackageMemberAnnotationTypeDeclaration pmatd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.FunctionDeclarator}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface FunctionDeclaratorVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.ConstructorDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX;
        /** Invoked by {@link Java.MethodDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitMethodDeclarator(Java.MethodDeclarator md) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeBodyDeclaration}s (declarations that may appear in the body of a
     * type declaration).
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeBodyDeclarationVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX;
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX;
        /** Invoked by {@link Java.Initializer#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitInitializer(Java.Initializer i) throws EX;
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitFieldDeclaration(Java.FieldDeclaration fd) throws EX;
        /** Invoked by {@link Java.MemberEnumDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        @Nullable R visitMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX;
        /** Invoked by {@link Java.FunctionDeclarator#accept(FunctionDeclaratorVisitor)}. */
        @Nullable R visitFunctionDeclarator(Java.FunctionDeclarator fd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.BlockStatement}s (statements that may appear with a block).
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface BlockStatementVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.Initializer#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitInitializer(Java.Initializer i) throws EX;
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitFieldDeclaration(Java.FieldDeclaration fd) throws EX;
        /** Invoked by {@link Java.LabeledStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitLabeledStatement(Java.LabeledStatement ls) throws EX;
        /** Invoked by {@link Java.Block#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitBlock(Java.Block b) throws EX;
        /** Invoked by {@link Java.ExpressionStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitExpressionStatement(Java.ExpressionStatement es) throws EX;
        /** Invoked by {@link Java.IfStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitIfStatement(Java.IfStatement is) throws EX;
        /** Invoked by {@link Java.ForStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitForStatement(Java.ForStatement fs) throws EX;
        /** Invoked by {@link Java.ForEachStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitForEachStatement(Java.ForEachStatement forEachStatement) throws EX;
        /** Invoked by {@link Java.WhileStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitWhileStatement(Java.WhileStatement ws) throws EX;
        /** Invoked by {@link Java.TryStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitTryStatement(Java.TryStatement ts) throws EX;
        /** Invoked by {@link Java.SwitchStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitSwitchStatement(Java.SwitchStatement ss) throws EX;
        /** Invoked by {@link Java.SynchronizedStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitSynchronizedStatement(Java.SynchronizedStatement ss) throws EX;
        /** Invoked by {@link Java.DoStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitDoStatement(Java.DoStatement ds) throws EX;
        /** Invoked by {@link Java.LocalVariableDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX;
        /** Invoked by {@link Java.ReturnStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitReturnStatement(Java.ReturnStatement rs) throws EX;
        /** Invoked by {@link Java.ThrowStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitThrowStatement(Java.ThrowStatement ts) throws EX;
        /** Invoked by {@link Java.BreakStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitBreakStatement(Java.BreakStatement bs) throws EX;
        /** Invoked by {@link Java.ContinueStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitContinueStatement(Java.ContinueStatement cs) throws EX;
        /** Invoked by {@link Java.AssertStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitAssertStatement(Java.AssertStatement as) throws EX;
        /** Invoked by {@link Java.EmptyStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitEmptyStatement(Java.EmptyStatement es) throws EX;
        /** Invoked by {@link Java.LocalClassDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX;
        /** Invoked by {@link Java.AlternateConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX;
        /** Invoked by {@link Java.SuperConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        @Nullable R visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Atom}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AtomVisitor<R, EX extends Throwable> extends TypeVisitor<R, EX> {
        /** Invoked by {@link Java.Package#accept(Visitor.AtomVisitor)}. */
        @Nullable R visitPackage(Java.Package p) throws EX;
        /** Invoked by {@link Java.Rvalue#accept(Visitor.RvalueVisitor)}. */
        @Nullable R visitRvalue(Java.Rvalue rv) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Type}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.ArrayType#accept(Visitor.TypeVisitor)} */
        @Nullable R visitArrayType(Java.ArrayType at) throws EX;
        /** Invoked by {@link Java.PrimitiveType#accept(Visitor.TypeVisitor)} */
        @Nullable R visitPrimitiveType(Java.PrimitiveType bt) throws EX;
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeVisitor)} */
        @Nullable R visitReferenceType(Java.ReferenceType rt) throws EX;
        /** Invoked by {@link Java.RvalueMemberType#accept(Visitor.TypeVisitor)} */
        @Nullable R visitRvalueMemberType(Java.RvalueMemberType rmt) throws EX;
        /** Invoked by {@link Java.SimpleType#accept(Visitor.TypeVisitor)} */
        @Nullable R visitSimpleType(Java.SimpleType st) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Rvalue}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface RvalueVisitor<R, EX extends Throwable> extends LvalueVisitor<R, EX> {
        /** Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitArrayLength(Java.ArrayLength al) throws EX;
        /** Invoked by {@link Java.Assignment#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitAssignment(Java.Assignment a) throws EX;
        /** Invoked by {@link Java.UnaryOperation#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitUnaryOperation(Java.UnaryOperation uo) throws EX;
        /** Invoked by {@link Java.BinaryOperation#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitBinaryOperation(Java.BinaryOperation bo) throws EX;
        /** Invoked by {@link Java.Cast#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitCast(Java.Cast c) throws EX;
        /** Invoked by {@link Java.ClassLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitClassLiteral(Java.ClassLiteral cl) throws EX;
        /** Invoked by {@link Java.ConditionalExpression#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitConditionalExpression(Java.ConditionalExpression ce) throws EX;
        /** Invoked by {@link Java.Crement#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitCrement(Java.Crement c) throws EX;
        /** Invoked by {@link Java.Instanceof#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitInstanceof(Java.Instanceof io) throws EX;
        /** Invoked by {@link Java.MethodInvocation#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitMethodInvocation(Java.MethodInvocation mi) throws EX;
        /** Invoked by {@link Java.SuperclassMethodInvocation#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX;
        /** Invoked by {@link Java.IntegerLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitIntegerLiteral(Java.IntegerLiteral il) throws EX;
        /** Invoked by {@link Java.FloatingPointLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) throws EX;
        /** Invoked by {@link Java.BooleanLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitBooleanLiteral(Java.BooleanLiteral bl) throws EX;
        /** Invoked by {@link Java.CharacterLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitCharacterLiteral(Java.CharacterLiteral cl) throws EX;
        /** Invoked by {@link Java.StringLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitStringLiteral(Java.StringLiteral sl) throws EX;
        /** Invoked by {@link Java.NullLiteral#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitNullLiteral(Java.NullLiteral nl) throws EX;
        /** Invoked by {@link Java.SimpleConstant#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitSimpleConstant(Java.SimpleConstant sl) throws EX;
        /** Invoked by {@link Java.NewAnonymousClassInstance#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) throws EX;
        /** Invoked by {@link Java.NewArray#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitNewArray(Java.NewArray na) throws EX;
        /** Invoked by {@link Java.NewInitializedArray#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitNewInitializedArray(Java.NewInitializedArray nia) throws EX;
        /** Invoked by {@link Java.NewClassInstance#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitNewClassInstance(Java.NewClassInstance nci) throws EX;
        /** Invoked by {@link Java.ParameterAccess#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitParameterAccess(Java.ParameterAccess pa) throws EX;
        /** Invoked by {@link Java.QualifiedThisReference#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitQualifiedThisReference(Java.QualifiedThisReference qtr) throws EX;
        /** Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)} */
        @Nullable R visitThisReference(Java.ThisReference tr) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Lvalue}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface LvalueVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.AmbiguousName#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitAmbiguousName(Java.AmbiguousName an) throws EX;
        /** Invoked by {@link Java.ArrayAccessExpression#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitArrayAccessExpression(Java.ArrayAccessExpression aae) throws EX;
        /** Invoked by {@link Java.FieldAccess#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitFieldAccess(Java.FieldAccess fa) throws EX;
        /** Invoked by {@link Java.FieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitFieldAccessExpression(Java.FieldAccessExpression fae) throws EX;
        /** Invoked by {@link Java.SuperclassFieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX;
        /** Invoked by {@link Java.LocalVariableAccess#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitLocalVariableAccess(Java.LocalVariableAccess lva) throws EX;
        /** Invoked by {@link Java.ParenthesizedExpression#accept(Visitor.LvalueVisitor)} */
        @Nullable R visitParenthesizedExpression(Java.ParenthesizedExpression pe) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Annotation}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AnnotationVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.MarkerAnnotation#accept(Visitor.AnnotationVisitor)} */
        @Nullable R visitMarkerAnnotation(Java.MarkerAnnotation ma) throws EX;
        /** Invoked by {@link Java.NormalAnnotation#accept(Visitor.AnnotationVisitor)} */
        @Nullable R visitNormalAnnotation(Java.NormalAnnotation na) throws EX;
        /** Invoked by {@link Java.SingleElementAnnotation#accept(Visitor.AnnotationVisitor)} */
        @Nullable R visitSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX;
    }

    /**
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ElementValueVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.Rvalue#accept(Visitor.ElementValueVisitor)} */
        @Nullable R visitRvalue(Java.Rvalue rv) throws EX;
        /** Invoked by {@link Java.Annotation#accept(Visitor.ElementValueVisitor)} */
        @Nullable R visitAnnotation(Java.Annotation a) throws EX;
        /** Invoked by {@link Java.ElementValueArrayInitializer#accept(Visitor.ElementValueVisitor)} */
        @Nullable R visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeArgument}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeArgumentVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.Wildcard#accept(Visitor.TypeArgumentVisitor)} */
        @Nullable R visitWildcard(Java.Wildcard w) throws EX;
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeArgumentVisitor)} */
        @Nullable R visitReferenceType(Java.ReferenceType rt) throws EX;
        /** Invoked by {@link Java.ArrayType#accept(Visitor.TypeArgumentVisitor)} */
        @Nullable R visitArrayType(Java.ArrayType arrayType) throws EX;
    }
}
