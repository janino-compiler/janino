
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
    extends ImportVisitor<R, EX>, TypeDeclarationVisitor<R, EX>, TypeBodyDeclarationVisitor<R, EX>,
    BlockStatementVisitor<R, EX>, AtomVisitor<R, EX>, ElementValueVisitor<R, EX> {
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
        R visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX;
        /** Invoked by {@link Java.CompilationUnit.TypeImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        R visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX;
        /** Invoked by {@link Java.CompilationUnit.SingleStaticImportDeclaration#accept(Visitor.ImportVisitor)} */
        R visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) throws EX;
        /** Invoked by {@link Java.CompilationUnit.StaticImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        R visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeDeclaration}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeDeclarationVisitor<R, EX extends Throwable> {
        /** Invoked by {@link Java.AnonymousClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX;
        /** Invoked by {@link Java.LocalClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX;
        /** Invoked by {@link Java.PackageMemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) throws EX;
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX;
        /** Invoked by {@link Java.PackageMemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX;
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX;
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
        R visitConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX;
        /** Invoked by {@link Java.MethodDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMethodDeclarator(Java.MethodDeclarator md) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeBodyDeclaration}s (declarations that may appear in the body of a
     * type declaration).
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeBodyDeclarationVisitor<R, EX extends Throwable> extends FunctionDeclaratorVisitor<R, EX> {
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX;
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX;
        /** Invoked by {@link Java.Initializer#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitInitializer(Java.Initializer i) throws EX;
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitFieldDeclaration(Java.FieldDeclaration fd) throws EX;
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
        R visitInitializer(Java.Initializer i) throws EX;
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.BlockStatementVisitor)} */
        R visitFieldDeclaration(Java.FieldDeclaration fd) throws EX;
        /** Invoked by {@link Java.LabeledStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLabeledStatement(Java.LabeledStatement ls) throws EX;
        /** Invoked by {@link Java.Block#accept(Visitor.BlockStatementVisitor)} */
        R visitBlock(Java.Block b) throws EX;
        /** Invoked by {@link Java.ExpressionStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitExpressionStatement(Java.ExpressionStatement es) throws EX;
        /** Invoked by {@link Java.IfStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitIfStatement(Java.IfStatement is) throws EX;
        /** Invoked by {@link Java.ForStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitForStatement(Java.ForStatement fs) throws EX;
        /** Invoked by {@link Java.ForEachStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitForEachStatement(Java.ForEachStatement forEachStatement) throws EX;
        /** Invoked by {@link Java.WhileStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitWhileStatement(Java.WhileStatement ws) throws EX;
        /** Invoked by {@link Java.TryStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitTryStatement(Java.TryStatement ts) throws EX;
        /** Invoked by {@link Java.SwitchStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitSwitchStatement(Java.SwitchStatement ss) throws EX;
        /** Invoked by {@link Java.SynchronizedStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitSynchronizedStatement(Java.SynchronizedStatement ss) throws EX;
        /** Invoked by {@link Java.DoStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitDoStatement(Java.DoStatement ds) throws EX;
        /** Invoked by {@link Java.LocalVariableDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX;
        /** Invoked by {@link Java.ReturnStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitReturnStatement(Java.ReturnStatement rs) throws EX;
        /** Invoked by {@link Java.ThrowStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitThrowStatement(Java.ThrowStatement ts) throws EX;
        /** Invoked by {@link Java.BreakStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitBreakStatement(Java.BreakStatement bs) throws EX;
        /** Invoked by {@link Java.ContinueStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitContinueStatement(Java.ContinueStatement cs) throws EX;
        /** Invoked by {@link Java.AssertStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitAssertStatement(Java.AssertStatement as) throws EX;
        /** Invoked by {@link Java.EmptyStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitEmptyStatement(Java.EmptyStatement es) throws EX;
        /** Invoked by {@link Java.LocalClassDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX;
        /** Invoked by {@link Java.AlternateConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        R visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX;
        /** Invoked by {@link Java.SuperConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        R visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Atom}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AtomVisitor<R, EX extends Throwable> extends RvalueVisitor<R, EX>, TypeVisitor<R, EX> {
        /** Invoked by {@link Java.Package#accept(Visitor.AtomVisitor)}. */
        R visitPackage(Java.Package p) throws EX;
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
        R visitArrayType(Java.ArrayType at) throws EX;
        /** Invoked by {@link Java.BasicType#accept(Visitor.TypeVisitor)} */
        R visitBasicType(Java.BasicType bt) throws EX;
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeVisitor)} */
        R visitReferenceType(Java.ReferenceType rt) throws EX;
        /** Invoked by {@link Java.RvalueMemberType#accept(Visitor.TypeVisitor)} */
        R visitRvalueMemberType(Java.RvalueMemberType rmt) throws EX;
        /** Invoked by {@link Java.SimpleType#accept(Visitor.TypeVisitor)} */
        R visitSimpleType(Java.SimpleType st) throws EX;
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
        R visitArrayLength(Java.ArrayLength al) throws EX;
        /** Invoked by {@link Java.Assignment#accept(Visitor.RvalueVisitor)} */
        R visitAssignment(Java.Assignment a) throws EX;
        /** Invoked by {@link Java.UnaryOperation#accept(Visitor.RvalueVisitor)} */
        R visitUnaryOperation(Java.UnaryOperation uo) throws EX;
        /** Invoked by {@link Java.BinaryOperation#accept(Visitor.RvalueVisitor)} */
        R visitBinaryOperation(Java.BinaryOperation bo) throws EX;
        /** Invoked by {@link Java.Cast#accept(Visitor.RvalueVisitor)} */
        R visitCast(Java.Cast c) throws EX;
        /** Invoked by {@link Java.ClassLiteral#accept(Visitor.RvalueVisitor)} */
        R visitClassLiteral(Java.ClassLiteral cl) throws EX;
        /** Invoked by {@link Java.ConditionalExpression#accept(Visitor.RvalueVisitor)} */
        R visitConditionalExpression(Java.ConditionalExpression ce) throws EX;
        /** Invoked by {@link Java.Crement#accept(Visitor.RvalueVisitor)} */
        R visitCrement(Java.Crement c) throws EX;
        /** Invoked by {@link Java.Instanceof#accept(Visitor.RvalueVisitor)} */
        R visitInstanceof(Java.Instanceof io) throws EX;
        /** Invoked by {@link Java.MethodInvocation#accept(Visitor.RvalueVisitor)} */
        R visitMethodInvocation(Java.MethodInvocation mi) throws EX;
        /** Invoked by {@link Java.SuperclassMethodInvocation#accept(Visitor.RvalueVisitor)} */
        R visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX;
        /** Invoked by {@link Java.IntegerLiteral#accept(Visitor.RvalueVisitor)} */
        R visitIntegerLiteral(Java.IntegerLiteral il) throws EX;
        /** Invoked by {@link Java.FloatingPointLiteral#accept(Visitor.RvalueVisitor)} */
        R visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) throws EX;
        /** Invoked by {@link Java.BooleanLiteral#accept(Visitor.RvalueVisitor)} */
        R visitBooleanLiteral(Java.BooleanLiteral bl) throws EX;
        /** Invoked by {@link Java.CharacterLiteral#accept(Visitor.RvalueVisitor)} */
        R visitCharacterLiteral(Java.CharacterLiteral cl) throws EX;
        /** Invoked by {@link Java.StringLiteral#accept(Visitor.RvalueVisitor)} */
        R visitStringLiteral(Java.StringLiteral sl) throws EX;
        /** Invoked by {@link Java.NullLiteral#accept(Visitor.RvalueVisitor)} */
        R visitNullLiteral(Java.NullLiteral nl) throws EX;
        /** Invoked by {@link Java.SimpleConstant#accept(Visitor.RvalueVisitor)} */
        R visitSimpleConstant(Java.SimpleConstant sl) throws EX;
        /** Invoked by {@link Java.NewAnonymousClassInstance#accept(Visitor.RvalueVisitor)} */
        R visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) throws EX;
        /** Invoked by {@link Java.NewArray#accept(Visitor.RvalueVisitor)} */
        R visitNewArray(Java.NewArray na) throws EX;
        /** Invoked by {@link Java.NewInitializedArray#accept(Visitor.RvalueVisitor)} */
        R visitNewInitializedArray(Java.NewInitializedArray nia) throws EX;
        /** Invoked by {@link Java.NewClassInstance#accept(Visitor.RvalueVisitor)} */
        R visitNewClassInstance(Java.NewClassInstance nci) throws EX;
        /** Invoked by {@link Java.ParameterAccess#accept(Visitor.RvalueVisitor)} */
        R visitParameterAccess(Java.ParameterAccess pa) throws EX;
        /** Invoked by {@link Java.QualifiedThisReference#accept(Visitor.RvalueVisitor)} */
        R visitQualifiedThisReference(Java.QualifiedThisReference qtr) throws EX;
        /** Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)} */
        R visitThisReference(Java.ThisReference tr) throws EX;
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
        R visitAmbiguousName(Java.AmbiguousName an) throws EX;
        /** Invoked by {@link Java.ArrayAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitArrayAccessExpression(Java.ArrayAccessExpression aae) throws EX;
        /** Invoked by {@link Java.FieldAccess#accept(Visitor.LvalueVisitor)} */
        R visitFieldAccess(Java.FieldAccess fa) throws EX;
        /** Invoked by {@link Java.FieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitFieldAccessExpression(Java.FieldAccessExpression fae) throws EX;
        /** Invoked by {@link Java.SuperclassFieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX;
        /** Invoked by {@link Java.LocalVariableAccess#accept(Visitor.LvalueVisitor)} */
        R visitLocalVariableAccess(Java.LocalVariableAccess lva) throws EX;
        /** Invoked by {@link Java.ParenthesizedExpression#accept(Visitor.LvalueVisitor)} */
        R visitParenthesizedExpression(Java.ParenthesizedExpression pe) throws EX;
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
        R visitMarkerAnnotation(Java.MarkerAnnotation ma) throws EX;
        /** Invoked by {@link Java.NormalAnnotation#accept(Visitor.AnnotationVisitor)} */
        R visitNormalAnnotation(Java.NormalAnnotation na) throws EX;
        /** Invoked by {@link Java.SingleElementAnnotation#accept(Visitor.AnnotationVisitor)} */
        R visitSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX;
    }

    /**
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ElementValueVisitor<R, EX extends Throwable> extends RvalueVisitor<R, EX>, AnnotationVisitor<R, EX> {
        /** Invoked by {@link Java.ElementValueArrayInitializer#accept(Visitor.ElementValueVisitor)} */
        R visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX;
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
        R visitWildcard(Java.Wildcard w) throws EX;
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeArgumentVisitor)} */
        R visitReferenceType(Java.ReferenceType rt) throws EX;
        /** Invoked by {@link Java.ArrayType#accept(Visitor.TypeArgumentVisitor)} */
        R visitArrayType(Java.ArrayType arrayType) throws EX;
    }
}
