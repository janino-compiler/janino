
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
    interface ComprehensiveVisitor
    extends ImportVisitor, TypeDeclarationVisitor, TypeBodyDeclarationVisitor, BlockStatementVisitor, AtomVisitor,
    ElementValueVisitor { // SUPPRESS CHECKSTYLE WrapAndIndent
    }

    /** The visitor for all kinds of {@link Java.CompilationUnit.ImportDeclaration}s. */
    public
    interface ImportVisitor {
        /** Invoked by {@link Java.CompilationUnit.SingleTypeImportDeclaration#accept(ImportVisitor)} */
        void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid);
        /** Invoked by {@link Java.CompilationUnit.TypeImportOnDemandDeclaration#accept(ImportVisitor)} */
        void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd);
        /** Invoked by {@link Java.CompilationUnit.SingleStaticImportDeclaration#accept(ImportVisitor)} */
        void visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid);
        /** Invoked by {@link Java.CompilationUnit.StaticImportOnDemandDeclaration#accept(ImportVisitor)} */
        void visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd);
    }

    /** The visitor for all kinds of {@link Java.TypeDeclaration}s. */
    public
    interface TypeDeclarationVisitor {
        /** Invoked by {@link Java.AnonymousClassDeclaration#accept(TypeDeclarationVisitor)} */
        void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd);
        /** Invoked by {@link Java.LocalClassDeclaration#accept(TypeDeclarationVisitor)} */
        void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd);
        /** Invoked by {@link Java.PackageMemberClassDeclaration#accept(TypeDeclarationVisitor)} */
        void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd);
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(TypeDeclarationVisitor)} */
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        /** Invoked by {@link Java.PackageMemberInterfaceDeclaration#accept(TypeDeclarationVisitor)} */
        void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid);
        /** Invoked by {@link Java.MemberClassDeclaration#accept(TypeDeclarationVisitor)} */
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
    }

    /**
     * The visitor for all kinds of {@link Java.TypeBodyDeclaration}s (declarations that may appear in the body of a
     * type declaration).
     */
    public
    interface TypeBodyDeclarationVisitor {
        /** Invoked by {@link Java.MemberInterfaceDeclaration#accept(TypeBodyDeclarationVisitor)} */
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        /** Invoked by {@link Java.MemberClassDeclaration#accept(TypeBodyDeclarationVisitor)} */
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
        /** Invoked by {@link Java.ConstructorDeclarator#accept(TypeBodyDeclarationVisitor)} */
        void visitConstructorDeclarator(Java.ConstructorDeclarator cd);
        /** Invoked by {@link Java.Initializer#accept(TypeBodyDeclarationVisitor)} */
        void visitInitializer(Java.Initializer i);
        /** Invoked by {@link Java.MethodDeclarator#accept(TypeBodyDeclarationVisitor)} */
        void visitMethodDeclarator(Java.MethodDeclarator md);
        /** Invoked by {@link Java.FieldDeclaration#accept(TypeBodyDeclarationVisitor)} */
        void visitFieldDeclaration(Java.FieldDeclaration fd);
    }

    /** The visitor for all kinds of {@link Java.BlockStatement}s (statements that may appear with a block). */
    public
    interface BlockStatementVisitor {
        /** Invoked by {@link Java.Initializer#accept(BlockStatementVisitor)} */
        void visitInitializer(Java.Initializer i);
        /** Invoked by {@link Java.FieldDeclaration#accept(BlockStatementVisitor)} */
        void visitFieldDeclaration(Java.FieldDeclaration fd);
        /** Invoked by {@link Java.LabeledStatement#accept(BlockStatementVisitor)} */
        void visitLabeledStatement(Java.LabeledStatement ls);
        /** Invoked by {@link Java.Block#accept(BlockStatementVisitor)} */
        void visitBlock(Java.Block b);
        /** Invoked by {@link Java.ExpressionStatement#accept(BlockStatementVisitor)} */
        void visitExpressionStatement(Java.ExpressionStatement es);
        /** Invoked by {@link Java.IfStatement#accept(BlockStatementVisitor)} */
        void visitIfStatement(Java.IfStatement is);
        /** Invoked by {@link Java.ForStatement#accept(BlockStatementVisitor)} */
        void visitForStatement(Java.ForStatement fs);
        /** Invoked by {@link Java.WhileStatement#accept(BlockStatementVisitor)} */
        void visitWhileStatement(Java.WhileStatement ws);
        /** Invoked by {@link Java.TryStatement#accept(BlockStatementVisitor)} */
        void visitTryStatement(Java.TryStatement ts);
        /** Invoked by {@link Java.SwitchStatement#accept(BlockStatementVisitor)} */
        void visitSwitchStatement(Java.SwitchStatement ss);
        /** Invoked by {@link Java.SynchronizedStatement#accept(BlockStatementVisitor)} */
        void visitSynchronizedStatement(Java.SynchronizedStatement ss);
        /** Invoked by {@link Java.DoStatement#accept(BlockStatementVisitor)} */
        void visitDoStatement(Java.DoStatement ds);
        /** Invoked by {@link Java.LocalVariableDeclarationStatement#accept(BlockStatementVisitor)} */
        void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds);
        /** Invoked by {@link Java.ReturnStatement#accept(BlockStatementVisitor)} */
        void visitReturnStatement(Java.ReturnStatement rs);
        /** Invoked by {@link Java.ThrowStatement#accept(BlockStatementVisitor)} */
        void visitThrowStatement(Java.ThrowStatement ts);
        /** Invoked by {@link Java.BreakStatement#accept(BlockStatementVisitor)} */
        void visitBreakStatement(Java.BreakStatement bs);
        /** Invoked by {@link Java.ContinueStatement#accept(BlockStatementVisitor)} */
        void visitContinueStatement(Java.ContinueStatement cs);
        /** Invoked by {@link Java.AssertStatement#accept(BlockStatementVisitor)} */
        void visitAssertStatement(Java.AssertStatement as);
        /** Invoked by {@link Java.EmptyStatement#accept(BlockStatementVisitor)} */
        void visitEmptyStatement(Java.EmptyStatement es);
        /** Invoked by {@link Java.LocalClassDeclarationStatement#accept(BlockStatementVisitor)} */
        void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds);
        /** Invoked by {@link Java.AlternateConstructorInvocation#accept(BlockStatementVisitor)} */
        void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci);
        /** Invoked by {@link Java.SuperConstructorInvocation#accept(BlockStatementVisitor)} */
        void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci);
    }

    /** The visitor for all kinds of {@link Java.Atom}s. */
    public
    interface AtomVisitor extends RvalueVisitor, TypeVisitor {
        /** Invoked by {@link Java.Package#accept(AtomVisitor)}. */
        void visitPackage(Java.Package p);
    }

    /** The visitor for all kinds of {@link Java.Type}s. */
    public
    interface TypeVisitor {
        /** Invoked by {@link Java.ArrayType#accept(TypeVisitor)} */
        void visitArrayType(Java.ArrayType at);
        /** Invoked by {@link Java.BasicType#accept(TypeVisitor)} */
        void visitBasicType(Java.BasicType bt);
        /** Invoked by {@link Java.ReferenceType#accept(TypeVisitor)} */
        void visitReferenceType(Java.ReferenceType rt);
        /** Invoked by {@link Java.RvalueMemberType#accept(TypeVisitor)} */
        void visitRvalueMemberType(Java.RvalueMemberType rmt);
        /** Invoked by {@link Java.SimpleType#accept(TypeVisitor)} */
        void visitSimpleType(Java.SimpleType st);
    }

    /** The visitor for all kinds of {@link Java.Rvalue}s. */
    public
    interface RvalueVisitor extends LvalueVisitor {
        /** Invoked by {@link Java.ArrayLength#accept(RvalueVisitor)} */
        void visitArrayLength(Java.ArrayLength al);
        /** Invoked by {@link Java.Assignment#accept(RvalueVisitor)} */
        void visitAssignment(Java.Assignment a);
        /** Invoked by {@link Java.UnaryOperation#accept(RvalueVisitor)} */
        void visitUnaryOperation(Java.UnaryOperation uo);
        /** Invoked by {@link Java.BinaryOperation#accept(RvalueVisitor)} */
        void visitBinaryOperation(Java.BinaryOperation bo);
        /** Invoked by {@link Java.Cast#accept(RvalueVisitor)} */
        void visitCast(Java.Cast c);
        /** Invoked by {@link Java.ClassLiteral#accept(RvalueVisitor)} */
        void visitClassLiteral(Java.ClassLiteral cl);
        /** Invoked by {@link Java.ConditionalExpression#accept(RvalueVisitor)} */
        void visitConditionalExpression(Java.ConditionalExpression ce);
        /** Invoked by {@link Java.Crement#accept(RvalueVisitor)} */
        void visitCrement(Java.Crement c);
        /** Invoked by {@link Java.Instanceof#accept(RvalueVisitor)} */
        void visitInstanceof(Java.Instanceof io);
        /** Invoked by {@link Java.MethodInvocation#accept(RvalueVisitor)} */
        void visitMethodInvocation(Java.MethodInvocation mi);
        /** Invoked by {@link Java.SuperclassMethodInvocation#accept(RvalueVisitor)} */
        void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi);
        /** Invoked by {@link Java.IntegerLiteral#accept(RvalueVisitor)} */
        void visitIntegerLiteral(Java.IntegerLiteral il);
        /** Invoked by {@link Java.FloatingPointLiteral#accept(RvalueVisitor)} */
        void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl);
        /** Invoked by {@link Java.BooleanLiteral#accept(RvalueVisitor)} */
        void visitBooleanLiteral(Java.BooleanLiteral bl);
        /** Invoked by {@link Java.CharacterLiteral#accept(RvalueVisitor)} */
        void visitCharacterLiteral(Java.CharacterLiteral cl);
        /** Invoked by {@link Java.StringLiteral#accept(RvalueVisitor)} */
        void visitStringLiteral(Java.StringLiteral sl);
        /** Invoked by {@link Java.NullLiteral#accept(RvalueVisitor)} */
        void visitNullLiteral(Java.NullLiteral nl);
        /** Invoked by {@link Java.SimpleConstant#accept(RvalueVisitor)} */
        void visitSimpleConstant(Java.SimpleConstant sl);
        /** Invoked by {@link Java.NewAnonymousClassInstance#accept(RvalueVisitor)} */
        void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci);
        /** Invoked by {@link Java.NewArray#accept(RvalueVisitor)} */
        void visitNewArray(Java.NewArray na);
        /** Invoked by {@link Java.NewInitializedArray#accept(RvalueVisitor)} */
        void visitNewInitializedArray(Java.NewInitializedArray nia);
        /** Invoked by {@link Java.NewClassInstance#accept(RvalueVisitor)} */
        void visitNewClassInstance(Java.NewClassInstance nci);
        /** Invoked by {@link Java.ParameterAccess#accept(RvalueVisitor)} */
        void visitParameterAccess(Java.ParameterAccess pa);
        /** Invoked by {@link Java.QualifiedThisReference#accept(RvalueVisitor)} */
        void visitQualifiedThisReference(Java.QualifiedThisReference qtr);
        /** Invoked by {@link Java.ArrayLength#accept(RvalueVisitor)} */
        void visitThisReference(Java.ThisReference tr);
    }

    /** The visitor for all kinds of {@link Java.Lvalue}s. */
    public
    interface LvalueVisitor {
        /** Invoked by {@link Java.AmbiguousName#accept(LvalueVisitor)} */
        void visitAmbiguousName(Java.AmbiguousName an);
        /** Invoked by {@link Java.ArrayAccessExpression#accept(LvalueVisitor)} */
        void visitArrayAccessExpression(Java.ArrayAccessExpression aae);
        /** Invoked by {@link Java.FieldAccess#accept(LvalueVisitor)} */
        void visitFieldAccess(Java.FieldAccess fa);
        /** Invoked by {@link Java.FieldAccessExpression#accept(LvalueVisitor)} */
        void visitFieldAccessExpression(Java.FieldAccessExpression fae);
        /** Invoked by {@link Java.SuperclassFieldAccessExpression#accept(LvalueVisitor)} */
        void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae);
        /** Invoked by {@link Java.LocalVariableAccess#accept(LvalueVisitor)} */
        void visitLocalVariableAccess(Java.LocalVariableAccess lva);
        /** Invoked by {@link Java.ParenthesizedExpression#accept(LvalueVisitor)} */
        void visitParenthesizedExpression(Java.ParenthesizedExpression pe);
    }

    /** The visitor for all kinds of {@link Java.Annotation}s. */
    public
    interface AnnotationVisitor {
        /** Invoked by {@link Java.MarkerAnnotation#accept(AnnotationVisitor)} */
        void visitMarkerAnnotation(Java.MarkerAnnotation ma);
        /** Invoked by {@link Java.NormalAnnotation#accept(AnnotationVisitor)} */
        void visitNormalAnnotation(Java.NormalAnnotation na);
        /** Invoked by {@link Java.SingleElementAnnotation#accept(AnnotationVisitor)} */
        void visitSingleElementAnnotation(Java.SingleElementAnnotation sea);
    }

    /** The visitor for all kinds of {@link Java.ElementValue}s. */
    public
    interface ElementValueVisitor extends RvalueVisitor, AnnotationVisitor {
        /** Invoked by {@link Java.ElementValueArrayInitializer#accept(ElementValueVisitor)} */
        void visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai);
    }

    /** The visitor for all kinds of {@link Java.TypeArgument}s. */
    public
    interface TypeArgumentVisitor {
        /** Invoked by {@link Java.Wildcard#accept(TypeArgumentVisitor)} */
        void visitWildcard(Java.Wildcard w);
        /** Invoked by {@link Java.ReferenceType#accept(TypeArgumentVisitor)} */
        void visitReferenceType(Java.ReferenceType rt);
    }
}
