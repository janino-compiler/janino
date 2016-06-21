
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


/** Basis for the "visitor" pattern as described in "Gamma, Helm, Johnson, Vlissides: Design Patterns". */
public
class Visitor {

    /**
     * The union of {@link ImportVisitor}, {@link TypeDeclarationVisitor}, {@link TypeBodyDeclarationVisitor} and
     * {@link AtomVisitor}.
     */
    public
    interface ComprehensiveVisitor<R>
    extends ImportVisitor<R>, TypeDeclarationVisitor<R>, TypeBodyDeclarationVisitor<R>, BlockStatementVisitor<R>,
    AtomVisitor<R>, ElementValueVisitor<R> { // SUPPRESS CHECKSTYLE WrapAndIndent
    }

    /** The visitor for all kinds of {@link Java.CompilationUnit.ImportDeclaration}s. */
    public
    interface ImportVisitor<R> {
        /** Invoked by {@link Java.CompilationUnit.SingleTypeImportDeclaration#accept(Visitor.ImportVisitor)} */
        R visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid);
        /** Invoked by {@link Java.CompilationUnit.TypeImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        R visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd);
        /** Invoked by {@link Java.CompilationUnit.SingleStaticImportDeclaration#accept(Visitor.ImportVisitor)} */
        R visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid);
        /** Invoked by {@link Java.CompilationUnit.StaticImportOnDemandDeclaration#accept(Visitor.ImportVisitor)} */
        R visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd);
    }

    /** The visitor for all kinds of {@link Java.TypeDeclaration}s. */
    public
    interface TypeDeclarationVisitor<R> {
        /** Invoked by {@link Java.AnonymousClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd);
        /** Invoked by {@link Java.LocalClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitLocalClassDeclaration(Java.LocalClassDeclaration lcd);
        /** Invoked by {@link Java.PackageMemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd);
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        /** Invoked by {@link Java.PackageMemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid);
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)} */
        R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
    }

    /** The visitor for all kinds of {@link Java.FunctionDeclarator}s. */
    public
    interface FunctionDeclaratorVisitor<R> {
        /** Invoked by {@link Java.ConstructorDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitConstructorDeclarator(Java.ConstructorDeclarator cd);
        /** Invoked by {@link Java.MethodDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMethodDeclarator(Java.MethodDeclarator md);
    }

    /**
     * The visitor for all kinds of {@link Java.TypeBodyDeclaration}s (declarations that may appear in the body of a
     * type declaration).
     */
    public
    interface TypeBodyDeclarationVisitor<R> extends FunctionDeclaratorVisitor<R> {
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        /** Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
        /** Invoked by {@link Java.Initializer#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitInitializer(Java.Initializer i);
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)} */
        R visitFieldDeclaration(Java.FieldDeclaration fd);
    }

    /** The visitor for all kinds of {@link Java.BlockStatement}s (statements that may appear with a block). */
    public
    interface BlockStatementVisitor<R> {
        /** Invoked by {@link Java.Initializer#accept(Visitor.BlockStatementVisitor)} */
        R visitInitializer(Java.Initializer i);
        /** Invoked by {@link Java.FieldDeclaration#accept(Visitor.BlockStatementVisitor)} */
        R visitFieldDeclaration(Java.FieldDeclaration fd);
        /** Invoked by {@link Java.LabeledStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLabeledStatement(Java.LabeledStatement ls);
        /** Invoked by {@link Java.Block#accept(Visitor.BlockStatementVisitor)} */
        R visitBlock(Java.Block b);
        /** Invoked by {@link Java.ExpressionStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitExpressionStatement(Java.ExpressionStatement es);
        /** Invoked by {@link Java.IfStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitIfStatement(Java.IfStatement is);
        /** Invoked by {@link Java.ForStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitForStatement(Java.ForStatement fs);
        /** Invoked by {@link Java.ForEachStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitForEachStatement(Java.ForEachStatement forEachStatement);
        /** Invoked by {@link Java.WhileStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitWhileStatement(Java.WhileStatement ws);
        /** Invoked by {@link Java.TryStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitTryStatement(Java.TryStatement ts);
        /** Invoked by {@link Java.SwitchStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitSwitchStatement(Java.SwitchStatement ss);
        /** Invoked by {@link Java.SynchronizedStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitSynchronizedStatement(Java.SynchronizedStatement ss);
        /** Invoked by {@link Java.DoStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitDoStatement(Java.DoStatement ds);
        /** Invoked by {@link Java.LocalVariableDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds);
        /** Invoked by {@link Java.ReturnStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitReturnStatement(Java.ReturnStatement rs);
        /** Invoked by {@link Java.ThrowStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitThrowStatement(Java.ThrowStatement ts);
        /** Invoked by {@link Java.BreakStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitBreakStatement(Java.BreakStatement bs);
        /** Invoked by {@link Java.ContinueStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitContinueStatement(Java.ContinueStatement cs);
        /** Invoked by {@link Java.AssertStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitAssertStatement(Java.AssertStatement as);
        /** Invoked by {@link Java.EmptyStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitEmptyStatement(Java.EmptyStatement es);
        /** Invoked by {@link Java.LocalClassDeclarationStatement#accept(Visitor.BlockStatementVisitor)} */
        R visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds);
        /** Invoked by {@link Java.AlternateConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        R visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci);
        /** Invoked by {@link Java.SuperConstructorInvocation#accept(Visitor.BlockStatementVisitor)} */
        R visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci);
    }

    /** The visitor for all kinds of {@link Java.Atom}s. */
    public
    interface AtomVisitor<R> extends RvalueVisitor<R>, TypeVisitor<R> {
        /** Invoked by {@link Java.Package#accept(Visitor.AtomVisitor)}. */
        R visitPackage(Java.Package p);
    }

    /** The visitor for all kinds of {@link Java.Type}s. */
    public
    interface TypeVisitor<R> {
        /** Invoked by {@link Java.ArrayType#accept(Visitor.TypeVisitor)} */
        R visitArrayType(Java.ArrayType at);
        /** Invoked by {@link Java.BasicType#accept(Visitor.TypeVisitor)} */
        R visitBasicType(Java.BasicType bt);
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeVisitor)} */
        R visitReferenceType(Java.ReferenceType rt);
        /** Invoked by {@link Java.RvalueMemberType#accept(Visitor.TypeVisitor)} */
        R visitRvalueMemberType(Java.RvalueMemberType rmt);
        /** Invoked by {@link Java.SimpleType#accept(Visitor.TypeVisitor)} */
        R visitSimpleType(Java.SimpleType st);
    }

    /** The visitor for all kinds of {@link Java.Rvalue}s. */
    public
    interface RvalueVisitor<R> extends LvalueVisitor<R> {
        /** Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)} */
        R visitArrayLength(Java.ArrayLength al);
        /** Invoked by {@link Java.Assignment#accept(Visitor.RvalueVisitor)} */
        R visitAssignment(Java.Assignment a);
        /** Invoked by {@link Java.UnaryOperation#accept(Visitor.RvalueVisitor)} */
        R visitUnaryOperation(Java.UnaryOperation uo);
        /** Invoked by {@link Java.BinaryOperation#accept(Visitor.RvalueVisitor)} */
        R visitBinaryOperation(Java.BinaryOperation bo);
        /** Invoked by {@link Java.Cast#accept(Visitor.RvalueVisitor)} */
        R visitCast(Java.Cast c);
        /** Invoked by {@link Java.ClassLiteral#accept(Visitor.RvalueVisitor)} */
        R visitClassLiteral(Java.ClassLiteral cl);
        /** Invoked by {@link Java.ConditionalExpression#accept(Visitor.RvalueVisitor)} */
        R visitConditionalExpression(Java.ConditionalExpression ce);
        /** Invoked by {@link Java.Crement#accept(Visitor.RvalueVisitor)} */
        R visitCrement(Java.Crement c);
        /** Invoked by {@link Java.Instanceof#accept(Visitor.RvalueVisitor)} */
        R visitInstanceof(Java.Instanceof io);
        /** Invoked by {@link Java.MethodInvocation#accept(Visitor.RvalueVisitor)} */
        R visitMethodInvocation(Java.MethodInvocation mi);
        /** Invoked by {@link Java.SuperclassMethodInvocation#accept(Visitor.RvalueVisitor)} */
        R visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi);
        /** Invoked by {@link Java.IntegerLiteral#accept(Visitor.RvalueVisitor)} */
        R visitIntegerLiteral(Java.IntegerLiteral il);
        /** Invoked by {@link Java.FloatingPointLiteral#accept(Visitor.RvalueVisitor)} */
        R visitFloatingPointLiteral(Java.FloatingPointLiteral fpl);
        /** Invoked by {@link Java.BooleanLiteral#accept(Visitor.RvalueVisitor)} */
        R visitBooleanLiteral(Java.BooleanLiteral bl);
        /** Invoked by {@link Java.CharacterLiteral#accept(Visitor.RvalueVisitor)} */
        R visitCharacterLiteral(Java.CharacterLiteral cl);
        /** Invoked by {@link Java.StringLiteral#accept(Visitor.RvalueVisitor)} */
        R visitStringLiteral(Java.StringLiteral sl);
        /** Invoked by {@link Java.NullLiteral#accept(Visitor.RvalueVisitor)} */
        R visitNullLiteral(Java.NullLiteral nl);
        /** Invoked by {@link Java.SimpleConstant#accept(Visitor.RvalueVisitor)} */
        R visitSimpleConstant(Java.SimpleConstant sl);
        /** Invoked by {@link Java.NewAnonymousClassInstance#accept(Visitor.RvalueVisitor)} */
        R visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci);
        /** Invoked by {@link Java.NewArray#accept(Visitor.RvalueVisitor)} */
        R visitNewArray(Java.NewArray na);
        /** Invoked by {@link Java.NewInitializedArray#accept(Visitor.RvalueVisitor)} */
        R visitNewInitializedArray(Java.NewInitializedArray nia);
        /** Invoked by {@link Java.NewClassInstance#accept(Visitor.RvalueVisitor)} */
        R visitNewClassInstance(Java.NewClassInstance nci);
        /** Invoked by {@link Java.ParameterAccess#accept(Visitor.RvalueVisitor)} */
        R visitParameterAccess(Java.ParameterAccess pa);
        /** Invoked by {@link Java.QualifiedThisReference#accept(Visitor.RvalueVisitor)} */
        R visitQualifiedThisReference(Java.QualifiedThisReference qtr);
        /** Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)} */
        R visitThisReference(Java.ThisReference tr);
    }

    /** The visitor for all kinds of {@link Java.Lvalue}s. */
    public
    interface LvalueVisitor<R> {
        /** Invoked by {@link Java.AmbiguousName#accept(Visitor.LvalueVisitor)} */
        R visitAmbiguousName(Java.AmbiguousName an);
        /** Invoked by {@link Java.ArrayAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitArrayAccessExpression(Java.ArrayAccessExpression aae);
        /** Invoked by {@link Java.FieldAccess#accept(Visitor.LvalueVisitor)} */
        R visitFieldAccess(Java.FieldAccess fa);
        /** Invoked by {@link Java.FieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitFieldAccessExpression(Java.FieldAccessExpression fae);
        /** Invoked by {@link Java.SuperclassFieldAccessExpression#accept(Visitor.LvalueVisitor)} */
        R visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae);
        /** Invoked by {@link Java.LocalVariableAccess#accept(Visitor.LvalueVisitor)} */
        R visitLocalVariableAccess(Java.LocalVariableAccess lva);
        /** Invoked by {@link Java.ParenthesizedExpression#accept(Visitor.LvalueVisitor)} */
        R visitParenthesizedExpression(Java.ParenthesizedExpression pe);
    }

    /** The visitor for all kinds of {@link Java.Annotation}s. */
    public
    interface AnnotationVisitor<R> {
        /** Invoked by {@link Java.MarkerAnnotation#accept(Visitor.AnnotationVisitor)} */
        R visitMarkerAnnotation(Java.MarkerAnnotation ma);
        /** Invoked by {@link Java.NormalAnnotation#accept(Visitor.AnnotationVisitor)} */
        R visitNormalAnnotation(Java.NormalAnnotation na);
        /** Invoked by {@link Java.SingleElementAnnotation#accept(Visitor.AnnotationVisitor)} */
        R visitSingleElementAnnotation(Java.SingleElementAnnotation sea);
    }

    /** The visitor for all kinds of {@link Java.ElementValue}s. */
    public
    interface ElementValueVisitor<R> extends RvalueVisitor<R>, AnnotationVisitor<R> {
        /** Invoked by {@link Java.ElementValueArrayInitializer#accept(Visitor.ElementValueVisitor)} */
        R visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai);
    }

    /**
     * The visitor for all kinds of {@link Java.TypeArgument}s.
     *
     * <R> The type of the object returned by the {@code visit*()} methods
     */
    public
    interface TypeArgumentVisitor<R> {
        /** Invoked by {@link Java.Wildcard#accept(Visitor.TypeArgumentVisitor)} */
        R visitWildcard(Java.Wildcard w);
        /** Invoked by {@link Java.ReferenceType#accept(Visitor.TypeArgumentVisitor)} */
        R visitReferenceType(Java.ReferenceType rt);
        /** Invoked by {@link Java.ArrayType#accept(Visitor.TypeArgumentVisitor)} */
        R visitArrayType(Java.ArrayType arrayType);
    }
}
