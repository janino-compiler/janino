
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino;

/**
 * Basis for the "visitor" pattern as described in "Gamma, Helm, Johnson,
 * Vlissides: Design Patterns".
 */
public interface Visitor {
    void visitCompilationUnit(Java.CompilationUnit cu);
    void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd);
    void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd);
    void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd);
    void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid);
    void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid);
    void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd);
    void visitConstructorDeclarator(Java.ConstructorDeclarator cd);
    void visitInitializer(Java.Initializer i);
    void visitMethodDeclarator(Java.MethodDeclarator md);
    void visitFieldDeclarator(Java.FieldDeclarator fd);
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
    void visitVariableDeclarator(Java.VariableDeclarator vd);
    void visitFormalParameter(Java.FormalParameter fp);
    void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci);
    void visitMethodInvocation(Java.MethodInvocation mi);
    void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci);
    void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci);
    void visitNewClassInstance(Java.NewClassInstance nci);
    void visitAssignment(Java.Assignment a);
    void visitArrayInitializer(Java.ArrayInitializer ai);
    void visitSimpleType(Java.SimpleType st);
    void visitBasicType(Java.BasicType bt);
    void visitReferenceType(Java.ReferenceType rt);
    void visitRvalueMemberType(Java.RvalueMemberType rmt);
    void visitArrayType(Java.ArrayType at);
    void visitAmbiguousName(Java.AmbiguousName an);
    void visitPackage(Java.Package p);
    void visitLocalVariableAccess(Java.LocalVariableAccess lva);
    void visitFieldAccess(Java.FieldAccess fa);
    void visitArrayLength(Java.ArrayLength al);
    void visitThisReference(Java.ThisReference tr);
    void visitQualifiedThisReference(Java.QualifiedThisReference qtr);
    void visitClassLiteral(Java.ClassLiteral cl);
    void visitConditionalExpression(Java.ConditionalExpression ce);
    void visitCrement(Java.Crement c);
    void visitArrayAccessExpression(Java.ArrayAccessExpression aae);
    void visitFieldAccessExpression(Java.FieldAccessExpression fae);
    void visitUnaryOperation(Java.UnaryOperation uo);
    void visitInstanceof(Java.Instanceof io);
    void visitBinaryOperation(Java.BinaryOperation bo);
    void visitCast(Java.Cast c);
    void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi);
    void visitParameterAccess(Java.ParameterAccess pa);
    void visitNewArray(Java.NewArray na);
    void visitLiteral(Java.Literal l);
    void visitConstantValue(Java.ConstantValue cv);
    void visitParenthesizedExpression(Java.ParenthesizedExpression pe);
}
