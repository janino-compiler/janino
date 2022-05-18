
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

package org.codehaus.janino;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AccessModifier;
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
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockLambdaBody;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.ExportsModuleDirective;
import org.codehaus.janino.Java.ExpressionLambdaBody;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FormalLambdaParameters;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IdentifierLambdaParameters;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.InferredLambdaParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
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
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.OpensModuleDirective;
import org.codehaus.janino.Java.Package;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.ProvidesModuleDirective;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.RequiresModuleDirective;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SingleElementAnnotation;
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
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.UsesModuleDirective;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Java.Wildcard;

/**
 * The basis for the "visitor" pattern as described in "Gamma, Helm, Johnson, Vlissides: Design Patterns".
 */
public final
class Visitor {

    private Visitor() {}

    /**
     * The visitor for the different kinds of {@link Java.AbstractCompilationUnit}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AbstractCompilationUnitVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.CompilationUnit#accept(Visitor.AbstractCompilationUnitVisitor)}
         */
        @Nullable R
        visitCompilationUnit(CompilationUnit cu) throws EX;

        /**
         * Invoked by {@link Java.ModularCompilationUnit#accept(Visitor.AbstractCompilationUnitVisitor)}
         */
        @Nullable R
        visitModularCompilationUnit(ModularCompilationUnit mcu) throws EX;
    }

    /**
     * The visitor for the different kinds of {@link Java.ModuleDirective}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ModuleDirectiveVisitor<R, EX extends Throwable> {
        @Nullable R visitRequiresModuleDirective(RequiresModuleDirective rmd) throws EX;
        @Nullable R visitExportsModuleDirective(ExportsModuleDirective emd)   throws EX;
        @Nullable R visitOpensModuleDirective(OpensModuleDirective omd)       throws EX;
        @Nullable R visitUsesModuleDirective(UsesModuleDirective umd)         throws EX;
        @Nullable R visitProvidesModuleDirective(ProvidesModuleDirective pmd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.AbstractCompilationUnit.ImportDeclaration}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ImportVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.AbstractCompilationUnit.SingleTypeImportDeclaration#accept(Visitor.ImportVisitor)}
         */
        @Nullable R
        visitSingleTypeImportDeclaration(AbstractCompilationUnit.SingleTypeImportDeclaration stid) throws EX;

        /**
         * Invoked by {@link Java.AbstractCompilationUnit.TypeImportOnDemandDeclaration#accept(Visitor.ImportVisitor)}
         */
        @Nullable R
        visitTypeImportOnDemandDeclaration(AbstractCompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX;

        /**
         * Invoked by {@link Java.AbstractCompilationUnit.SingleStaticImportDeclaration#accept(Visitor.ImportVisitor)}
         */
        @Nullable R
        visitSingleStaticImportDeclaration(AbstractCompilationUnit.SingleStaticImportDeclaration ssid) throws EX;

        /**
         * Invoked by {@link Java.AbstractCompilationUnit.StaticImportOnDemandDeclaration#accept(Visitor.ImportVisitor)}
         */
        @Nullable R
        visitStaticImportOnDemandDeclaration(AbstractCompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeDeclaration}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeDeclarationVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.AnonymousClassDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitAnonymousClassDeclaration(AnonymousClassDeclaration acd) throws EX;

        /**
         * Invoked by {@link Java.LocalClassDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitLocalClassDeclaration(LocalClassDeclaration lcd) throws EX;

        /**
         * Invoked by {@link Java.PackageMemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd) throws EX;

        /**
         * Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws EX;

        /**
         * Invoked by {@link Java.PackageMemberInterfaceDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid) throws EX;

        /**
         * Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitMemberClassDeclaration(MemberClassDeclaration mcd) throws EX;

        /**
         * Invoked by {@link Java.EnumConstant#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitEnumConstant(EnumConstant ec) throws EX;

        /**
         * Invoked by {@link Java.MemberEnumDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitMemberEnumDeclaration(MemberEnumDeclaration med) throws EX;

        /**
         * Invoked by {@link Java.PackageMemberEnumDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) throws EX;

        /**
         * Invoked by {@link Java.MemberAnnotationTypeDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX;

        /**
         * Invoked by {@link Java.PackageMemberAnnotationTypeDeclaration#accept(Visitor.TypeDeclarationVisitor)}
         */
        @Nullable R visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.FunctionDeclarator}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface FunctionDeclaratorVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.ConstructorDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitConstructorDeclarator(ConstructorDeclarator cd) throws EX;

        /**
         * Invoked by {@link Java.MethodDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitMethodDeclarator(MethodDeclarator md) throws EX;
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

        /**
         * Invoked by {@link Java.MemberInterfaceDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws EX;

        /**
         * Invoked by {@link Java.MemberClassDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitMemberClassDeclaration(MemberClassDeclaration mcd) throws EX;

        /**
         * Invoked by {@link Java.Initializer#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitInitializer(Initializer i) throws EX;

        /**
         * Invoked by {@link Java.FieldDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitFieldDeclaration(FieldDeclaration fd) throws EX;

        /**
         * Invoked by {@link Java.MemberEnumDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)}
         */
        @Nullable R visitMemberEnumDeclaration(MemberEnumDeclaration med) throws EX;

        /**
         * Invoked by {@link Java.FunctionDeclarator#accept(Visitor.TypeBodyDeclarationVisitor)}.
         */
        @Nullable R visitFunctionDeclarator(FunctionDeclarator fd) throws EX;

        /**
         * Invoked by {@link Java.MemberAnnotationTypeDeclaration#accept(Visitor.TypeBodyDeclarationVisitor)}.
         */
        @Nullable R visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.BlockStatement}s (statements that may appear with a block).
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface BlockStatementVisitor<R, EX extends Throwable> extends FieldDeclarationOrInitializerVisitor<R, EX> {

        /**
         * Invoked by {@link Java.LabeledStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitLabeledStatement(LabeledStatement ls) throws EX;

        /**
         * Invoked by {@link Java.Block#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitBlock(Block b) throws EX;

        /**
         * Invoked by {@link Java.ExpressionStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitExpressionStatement(ExpressionStatement es) throws EX;

        /**
         * Invoked by {@link Java.IfStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitIfStatement(IfStatement is) throws EX;

        /**
         * Invoked by {@link Java.ForStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitForStatement(ForStatement fs) throws EX;

        /**
         * Invoked by {@link Java.ForEachStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitForEachStatement(ForEachStatement forEachStatement) throws EX;

        /**
         * Invoked by {@link Java.WhileStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitWhileStatement(WhileStatement ws) throws EX;

        /**
         * Invoked by {@link Java.TryStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitTryStatement(TryStatement ts) throws EX;

        /**
         * Invoked by {@link Java.SwitchStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitSwitchStatement(SwitchStatement ss) throws EX;

        /**
         * Invoked by {@link Java.SynchronizedStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitSynchronizedStatement(SynchronizedStatement ss) throws EX;

        /**
         * Invoked by {@link Java.DoStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitDoStatement(DoStatement ds) throws EX;

        /**
         * Invoked by {@link Java.LocalVariableDeclarationStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws EX;

        /**
         * Invoked by {@link Java.ReturnStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitReturnStatement(ReturnStatement rs) throws EX;

        /**
         * Invoked by {@link Java.ThrowStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitThrowStatement(ThrowStatement ts) throws EX;

        /**
         * Invoked by {@link Java.BreakStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitBreakStatement(BreakStatement bs) throws EX;

        /**
         * Invoked by {@link Java.ContinueStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitContinueStatement(ContinueStatement cs) throws EX;

        /**
         * Invoked by {@link Java.AssertStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitAssertStatement(AssertStatement as) throws EX;

        /**
         * Invoked by {@link Java.EmptyStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitEmptyStatement(EmptyStatement es) throws EX;

        /**
         * Invoked by {@link Java.LocalClassDeclarationStatement#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) throws EX;

        /**
         * Invoked by {@link Java.AlternateConstructorInvocation#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX;

        /**
         * Invoked by {@link Java.SuperConstructorInvocation#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitSuperConstructorInvocation(SuperConstructorInvocation sci) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.BlockStatement}s (statements that may appear with a block).
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface FieldDeclarationOrInitializerVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.Initializer#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitInitializer(Initializer i) throws EX;

        /**
         * Invoked by {@link Java.FieldDeclaration#accept(Visitor.BlockStatementVisitor)}
         */
        @Nullable R visitFieldDeclaration(FieldDeclaration fd) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Atom}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AtomVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.Package#accept(Visitor.AtomVisitor)}.
         */
        @Nullable R visitPackage(Package p) throws EX;

        /**
         * Invoked by {@link Java.Rvalue#accept(Visitor.AtomVisitor)}.
         */
        @Nullable R visitRvalue(Rvalue rv) throws EX;

        /**
         * Invoked by {@link Java.Type#accept(Visitor.AtomVisitor)}.
         */
        @Nullable R visitType(Type t) throws EX;

        /**
         * Invoked by {@link Java.ConstructorInvocation#accept(Visitor.AtomVisitor)}.
         */
        @Nullable R visitConstructorInvocation(ConstructorInvocation ci) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Type}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.ArrayType#accept(Visitor.TypeVisitor)}
         */
        @Nullable R visitArrayType(ArrayType at) throws EX;

        /**
         * Invoked by {@link Java.PrimitiveType#accept(Visitor.TypeVisitor)}
         */
        @Nullable R visitPrimitiveType(PrimitiveType bt) throws EX;

        /**
         * Invoked by {@link Java.ReferenceType#accept(Visitor.TypeVisitor)}
         */
        @Nullable R visitReferenceType(ReferenceType rt) throws EX;

        /**
         * Invoked by {@link Java.RvalueMemberType#accept(Visitor.TypeVisitor)}
         */
        @Nullable R visitRvalueMemberType(RvalueMemberType rmt) throws EX;

        /**
         * Invoked by {@link Java.SimpleType#accept(Visitor.TypeVisitor)}
         */
        @Nullable R visitSimpleType(SimpleType st) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Rvalue}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface RvalueVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.Lvalue#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitLvalue(Lvalue lv) throws EX;

        /**
         * Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitArrayLength(ArrayLength al) throws EX;

        /**
         * Invoked by {@link Java.Assignment#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitAssignment(Assignment a) throws EX;

        /**
         * Invoked by {@link Java.UnaryOperation#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitUnaryOperation(UnaryOperation uo) throws EX;

        /**
         * Invoked by {@link Java.BinaryOperation#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitBinaryOperation(BinaryOperation bo) throws EX;

        /**
         * Invoked by {@link Java.Cast#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitCast(Cast c) throws EX;

        /**
         * Invoked by {@link Java.ClassLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitClassLiteral(ClassLiteral cl) throws EX;

        /**
         * Invoked by {@link Java.ConditionalExpression#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitConditionalExpression(ConditionalExpression ce) throws EX;

        /**
         * Invoked by {@link Java.Crement#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitCrement(Crement c) throws EX;

        /**
         * Invoked by {@link Java.Instanceof#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitInstanceof(Instanceof io) throws EX;

        /**
         * Invoked by {@link Java.MethodInvocation#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitMethodInvocation(MethodInvocation mi) throws EX;

        /**
         * Invoked by {@link Java.SuperclassMethodInvocation#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws EX;

        /**
         * Invoked by {@link Java.IntegerLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitIntegerLiteral(IntegerLiteral il) throws EX;

        /**
         * Invoked by {@link Java.FloatingPointLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitFloatingPointLiteral(FloatingPointLiteral fpl) throws EX;

        /**
         * Invoked by {@link Java.BooleanLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitBooleanLiteral(BooleanLiteral bl) throws EX;

        /**
         * Invoked by {@link Java.CharacterLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitCharacterLiteral(CharacterLiteral cl) throws EX;

        /**
         * Invoked by {@link Java.StringLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitStringLiteral(StringLiteral sl) throws EX;

        /**
         * Invoked by {@link Java.NullLiteral#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitNullLiteral(NullLiteral nl) throws EX;

        /**
         * Invoked by {@link Java.SimpleConstant#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitSimpleConstant(SimpleConstant sl) throws EX;

        /**
         * Invoked by {@link Java.NewAnonymousClassInstance#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitNewAnonymousClassInstance(NewAnonymousClassInstance naci) throws EX;

        /**
         * Invoked by {@link Java.NewArray#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitNewArray(NewArray na) throws EX;

        /**
         * Invoked by {@link Java.NewInitializedArray#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitNewInitializedArray(NewInitializedArray nia) throws EX;

        /**
         * Invoked by {@link Java.NewClassInstance#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitNewClassInstance(NewClassInstance nci) throws EX;

        /**
         * Invoked by {@link Java.ParameterAccess#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitParameterAccess(ParameterAccess pa) throws EX;

        /**
         * Invoked by {@link Java.QualifiedThisReference#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitQualifiedThisReference(QualifiedThisReference qtr) throws EX;

        /**
         * Invoked by {@link Java.ArrayLength#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitThisReference(ThisReference tr) throws EX;

        /**
         * Invoked by {@link Java.LambdaExpression#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitLambdaExpression(LambdaExpression le) throws EX;

        /**
         * Invoked by {@link Java.MethodReference#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitMethodReference(MethodReference mr) throws EX;

        /**
         * Invoked by {@link Java.ClassInstanceCreationReference#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws EX;

        /**
         * Invoked by {@link Java.ArrayCreationReference#accept(Visitor.RvalueVisitor)}
         */
        @Nullable R visitArrayCreationReference(ArrayCreationReference acr) throws EX;
    }

    /**
     * The visitor for {@link ArrayInitializerOrRvalue}.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ArrayInitializerOrRvalueVisitor<R, EX extends Throwable> {
        @Nullable R visitArrayInitializer(ArrayInitializer ai) throws EX;
        @Nullable R visitRvalue(Rvalue rvalue)                 throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Lvalue}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface LvalueVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.AmbiguousName#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitAmbiguousName(AmbiguousName an) throws EX;

        /**
         * Invoked by {@link Java.ArrayAccessExpression#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitArrayAccessExpression(ArrayAccessExpression aae) throws EX;

        /**
         * Invoked by {@link Java.FieldAccess#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitFieldAccess(FieldAccess fa) throws EX;

        /**
         * Invoked by {@link Java.FieldAccessExpression#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitFieldAccessExpression(FieldAccessExpression fae) throws EX;

        /**
         * Invoked by {@link Java.SuperclassFieldAccessExpression#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws EX;

        /**
         * Invoked by {@link Java.LocalVariableAccess#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitLocalVariableAccess(LocalVariableAccess lva) throws EX;

        /**
         * Invoked by {@link Java.ParenthesizedExpression#accept(Visitor.LvalueVisitor)}
         */
        @Nullable R visitParenthesizedExpression(ParenthesizedExpression pe) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.ConstructorInvocation}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ConstructorInvocationVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.MarkerAnnotation#accept(Visitor.AnnotationVisitor)}
         */
        @Nullable R visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws EX;

        /**
         * Invoked by {@link Java.SingleElementAnnotation#accept(Visitor.AnnotationVisitor)}
         */
        @Nullable R visitSuperConstructorInvocation(SuperConstructorInvocation sci) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.Annotation}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface AnnotationVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.MarkerAnnotation#accept(Visitor.AnnotationVisitor)}
         */
        @Nullable R visitMarkerAnnotation(MarkerAnnotation ma) throws EX;

        /**
         * Invoked by {@link Java.NormalAnnotation#accept(Visitor.AnnotationVisitor)}
         */
        @Nullable R visitNormalAnnotation(NormalAnnotation na) throws EX;

        /**
         * Invoked by {@link Java.SingleElementAnnotation#accept(Visitor.AnnotationVisitor)}
         */
        @Nullable R visitSingleElementAnnotation(SingleElementAnnotation sea) throws EX;
    }

    /**
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ElementValueVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.Rvalue#accept(Visitor.ElementValueVisitor)}
         */
        @Nullable R visitRvalue(Rvalue rv) throws EX;

        /**
         * Invoked by {@link Java.Annotation#accept(Visitor.ElementValueVisitor)}
         */
        @Nullable R visitAnnotation(Annotation a) throws EX;

        /**
         * Invoked by {@link Java.ElementValueArrayInitializer#accept(Visitor.ElementValueVisitor)}
         */
        @Nullable R visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TypeArgument}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TypeArgumentVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link Java.Wildcard#accept(Visitor.TypeArgumentVisitor)}
         */
        @Nullable R visitWildcard(Wildcard w) throws EX;

        /**
         * Invoked by {@link Java.ReferenceType#accept(Visitor.TypeArgumentVisitor)}
         */
        @Nullable R visitReferenceType(ReferenceType rt) throws EX;

        /**
         * Invoked by {@link Java.ArrayType#accept(Visitor.TypeArgumentVisitor)}
         */
        @Nullable R visitArrayType(ArrayType arrayType) throws EX;
    }

    /**
     * The visitor for the different kinds of {@link Java.LambdaParameters} styles.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface LambdaParametersVisitor<R, EX extends Throwable> { // SUPPRESS CHECKSTYLE Javadoc:4

        @Nullable R visitIdentifierLambdaParameters(IdentifierLambdaParameters ilp) throws EX;
        @Nullable R visitFormalLambdaParameters(FormalLambdaParameters flp)         throws EX;
        @Nullable R visitInferredLambdaParameters(InferredLambdaParameters ilp)     throws EX;
    }

    /**
     * The visitor for the different kinds of {@link Java.LambdaBody}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface LambdaBodyVisitor<R, EX extends Throwable> { // SUPPRESS CHECKSTYLE Javadoc:3

        @Nullable R visitBlockLambdaBody(BlockLambdaBody blockLambdaBody)                throws EX;
        @Nullable R visitExpressionLambdaBody(ExpressionLambdaBody expressionLambdaBody) throws EX;
    }

    /**
     * The visitor for all kinds of {@link Java.TryStatement.Resource}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface TryStatementResourceVisitor<R, EX extends Throwable> {

        /**
         * Invoked by {@link
         * Java.TryStatement.LocalVariableDeclaratorResource#accept(Visitor.TryStatementResourceVisitor)}.
         */
        @Nullable R
        visitLocalVariableDeclaratorResource(TryStatement.LocalVariableDeclaratorResource lvdr) throws EX;

        /**
         * Invoked by {@link Java.TryStatement.VariableAccessResource#accept(Visitor.TryStatementResourceVisitor)}
         */
        @Nullable R
        visitVariableAccessResource(TryStatement.VariableAccessResource var) throws EX;
    }

    /**
     * The visitor for the different kinds of {@link Java.Modifier}s.
     *
     * @param <R>  The type of the object returned by the {@code visit*()} methods
     * @param <EX> The exception that the {@code visit*()} methods may throw
     */
    public
    interface ModifierVisitor<R, EX extends Throwable> extends AnnotationVisitor<R, EX> {

        @Nullable R visitAccessModifier(AccessModifier am) throws EX; // SUPPRESS CHECKSTYLE Javadoc
    }
}
