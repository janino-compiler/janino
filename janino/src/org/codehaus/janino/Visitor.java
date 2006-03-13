
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

/**
 * Basis for the "visitor" pattern as described in "Gamma, Helm, Johnson,
 * Vlissides: Design Patterns".
 */
public class Visitor {
    public interface ComprehensiveVisitor
    extends TypeDeclarationVisitor, TypeBodyDeclarationVisitor, BlockStatementVisitor, AtomVisitor {
    
        // ImportDeclaration-derived.
        void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid);
        void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd);
    }
    
    public interface TypeDeclarationVisitor {
        void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd);
        void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd);
        void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd);
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid);
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
    }
    
    public interface TypeBodyDeclarationVisitor {
        void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
        void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
        void visitConstructorDeclarator(Java.ConstructorDeclarator cd);
        void visitInitializer(Java.Initializer i);
        void visitMethodDeclarator(Java.MethodDeclarator md);
        void visitFieldDeclaration(Java.FieldDeclaration fd);
    }
    
    public interface BlockStatementVisitor {
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
        void visitEmptyStatement(Java.EmptyStatement es);
        void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds);
        void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci);
        void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci);
    }
    
    public interface AtomVisitor extends RvalueVisitor, TypeVisitor {
        void visitPackage(Java.Package p);
    }
    
    public interface TypeVisitor {
        void visitArrayType(Java.ArrayType at);
        void visitBasicType(Java.BasicType bt);
        void visitReferenceType(Java.ReferenceType rt);
        void visitRvalueMemberType(Java.RvalueMemberType rmt);
        void visitSimpleType(Java.SimpleType st);
    }
    
    public interface RvalueVisitor extends LvalueVisitor {
        void visitArrayLength(Java.ArrayLength al);
        void visitAssignment(Java.Assignment a);
        void visitUnaryOperation(Java.UnaryOperation uo);
        void visitBinaryOperation(Java.BinaryOperation bo);
        void visitCast(Java.Cast c);
        void visitClassLiteral(Java.ClassLiteral cl);
        void visitConditionalExpression(Java.ConditionalExpression ce);
        void visitConstantValue(Java.ConstantValue cv);
        void visitCrement(Java.Crement c);
        void visitInstanceof(Java.Instanceof io);
        void visitMethodInvocation(Java.MethodInvocation mi);
        void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi);
        void visitLiteral(Java.Literal l);
        void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci);
        void visitNewArray(Java.NewArray na);
        void visitNewInitializedArray(Java.NewInitializedArray nia);
        void visitNewClassInstance(Java.NewClassInstance nci);
        void visitParameterAccess(Java.ParameterAccess pa);
        void visitQualifiedThisReference(Java.QualifiedThisReference qtr);
        void visitThisReference(Java.ThisReference tr);
    }
    
    public interface LvalueVisitor {
        void visitAmbiguousName(Java.AmbiguousName an);
        void visitArrayAccessExpression(Java.ArrayAccessExpression aae);
        void visitFieldAccess(Java.FieldAccess fa);
        void visitFieldAccessExpression(Java.FieldAccessExpression fae);
        void visitLocalVariableAccess(Java.LocalVariableAccess lva);
        void visitParenthesizedExpression(Java.ParenthesizedExpression pe);
    }
}
