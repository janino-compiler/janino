
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
 * Basis for the "visitor" pattern as described in "Gamma, Helm, Johnson,
 * Vlissides: Design Patterns".
 */
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

    /**
     * The visitor for all kinds of IMPORT declarations.
     */
    public
    interface ImportVisitor {

        /** E.g. '{@code import pkg.Type}'. */
        void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid);

        /** E.g. '{@code import pkg.*}'. */
        void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd);

        /** E.g. '{@code import static pkg.Type.member}'. */
        void visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid);

        /** E.g. '{@code import static pkg.Type.*}'. */
        void visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd);
    }

    /**
     * The visitor for all kinds of type declarations.
     */
    public
    interface TypeDeclarationVisitor {

        /** E.g. '<tt>new Superclass() { ... }</tt>'. */
        void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd);

        /** E.g. '<tt>class LocalClass { ... }</tt>'. */
        void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd);

        /** E.g. '<tt>public class TopLevelClass { ... }</tt>'. */
        void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd);

        /** E.g. '<tt>public class TopLevelClass { public interface MemberInterface { ... } }</tt>'. */
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);

        /** E.g. '<tt>public TopLevelInterface { ... }</tt>'. */
        void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid);

        /** E.g. '<tt>public class TopLevelClass { public class MemberClass { ... } }</tt>'. */
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
    }

    /**
     * The visitor for all kinds of type body declarations (declarations that may appear in the body of a type
     * declaration).
     */
    public
    interface TypeBodyDeclarationVisitor {

        /** E.g. '<tt>public class TopLevelClass { public interface MemberInterface { ... } }</tt>'. */
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);

        /** E.g. '<tt>public class TopLevelClass { public class MemberClass { ... } }</tt>'. */
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);

        /** E.g. '<tt>public MyClass(...) { ... }</tt>'. */
        void visitConstructorDeclarator(Java.ConstructorDeclarator cd);

        /** E.g. '<tt>public class MyClass { static { ... } }</tt>'. */
        void visitInitializer(Java.Initializer i);

        /** E.g. '<tt>public void meth(...) { ... }</tt>'. */
        void visitMethodDeclarator(Java.MethodDeclarator md);

        /** E.g. '<tt>public int field;</tt>'. */
        void visitFieldDeclaration(Java.FieldDeclaration fd);
    }

    /**
     * The visitor for all kinds of block statements (statements that may appear with a block).
     */
    public
    interface BlockStatementVisitor {
        void visitInitializer(Java.Initializer i);
        void visitFieldDeclaration(Java.FieldDeclaration fd);
        void visitLabeledStatement(Java.LabeledStatement ls);
        void visitBlock(Java.Block b);
        void visitExpressionStatement(Java.ExpressionStatement es);
        void visitIfStatement(Java.IfStatement is);
        void visitForStatement(Java.ForStatement fs);
        void visitWhileStatement(Java.WhileStatement ws);
        void visitTryStatement(Java.TryStatement ts);
        void visitSwitchStatement(Java.SwitchStatement ss);
        void visitSynchronizedStatement(Java.SynchronizedStatement ss);
        void visitDoStatement(Java.DoStatement ds);
        void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds);
        void visitReturnStatement(Java.ReturnStatement rs);
        void visitThrowStatement(Java.ThrowStatement ts);
        void visitBreakStatement(Java.BreakStatement bs);
        void visitContinueStatement(Java.ContinueStatement cs);
        void visitAssertStatement(Java.AssertStatement as);
        void visitEmptyStatement(Java.EmptyStatement es);
        void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds);
        void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci);
        void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci);
    }

    public
    interface AtomVisitor extends RvalueVisitor, TypeVisitor {
        void visitPackage(Java.Package p);
    }

    public
    interface TypeVisitor {
        void visitArrayType(Java.ArrayType at);
        void visitBasicType(Java.BasicType bt);
        void visitReferenceType(Java.ReferenceType rt);
        void visitRvalueMemberType(Java.RvalueMemberType rmt);
        void visitSimpleType(Java.SimpleType st);
    }

    public
    interface RvalueVisitor extends LvalueVisitor {
        void visitArrayLength(Java.ArrayLength al);
        void visitAssignment(Java.Assignment a);
        void visitUnaryOperation(Java.UnaryOperation uo);
        void visitBinaryOperation(Java.BinaryOperation bo);
        void visitCast(Java.Cast c);
        void visitClassLiteral(Java.ClassLiteral cl);
        void visitConditionalExpression(Java.ConditionalExpression ce);
        void visitCrement(Java.Crement c);
        void visitInstanceof(Java.Instanceof io);
        void visitMethodInvocation(Java.MethodInvocation mi);
        void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi);
        void visitIntegerLiteral(Java.IntegerLiteral il);
        void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl);
        void visitBooleanLiteral(Java.BooleanLiteral bl);
        void visitCharacterLiteral(Java.CharacterLiteral cl);
        void visitStringLiteral(Java.StringLiteral sl);
        void visitNullLiteral(Java.NullLiteral nl);
        void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci);
        void visitNewArray(Java.NewArray na);
        void visitNewInitializedArray(Java.NewInitializedArray nia);
        void visitNewClassInstance(Java.NewClassInstance nci);
        void visitParameterAccess(Java.ParameterAccess pa);
        void visitQualifiedThisReference(Java.QualifiedThisReference qtr);
        void visitThisReference(Java.ThisReference tr);
    }

    public
    interface LvalueVisitor {
        void visitAmbiguousName(Java.AmbiguousName an);
        void visitArrayAccessExpression(Java.ArrayAccessExpression aae);
        void visitFieldAccess(Java.FieldAccess fa);
        void visitFieldAccessExpression(Java.FieldAccessExpression fae);
        void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae);
        void visitLocalVariableAccess(Java.LocalVariableAccess lva);
        void visitParenthesizedExpression(Java.ParenthesizedExpression pe);
    }

    public
    interface AnnotationVisitor {
        void visitMarkerAnnotation(Java.MarkerAnnotation ma);
        void visitNormalAnnotation(Java.NormalAnnotation na);
        void visitSingleElementAnnotation(Java.SingleElementAnnotation sea);
    }
    
    public
    interface ElementValueArrayInitializerVisitor {
        void visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai);
    }
    
    public
    interface ElementValueVisitor extends RvalueVisitor, AnnotationVisitor, ElementValueArrayInitializerVisitor {
    }
}
