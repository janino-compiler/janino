
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

package org.codehaus.janino.util;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.JaninoRuntimeException;
import org.codehaus.janino.Java;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Visitor;
import org.codehaus.janino.Visitor.ComprehensiveVisitor;

/**
 * This class traverses the subnodes of an AST. Derived classes may override individual methods to process specific
 * nodes, e.g.:
 * <pre>
 *     LocalClassDeclaration lcd = ...;
 *     lcd.accept(new Traverser() {
 *         int n = 0;
 *         public void traverseMethodDeclarator(Java.MethodDeclarator md) {
 *             ++this.n;
 *             super.traverseMethodDeclarator(md);
 *         }
 *     }.comprehensiveVisitor());
 * </pre>
 *
 * @param <EX> The exception that the "{@code travers*()}" methods may throw
 */
public
class Traverser<EX extends Throwable> {

    private final Visitor.ComprehensiveVisitor<Void, EX> cv = new Visitor.ComprehensiveVisitor<Void, EX>() {

        // CHECKSTYLE LineLengthCheck:OFF
        @Override @Nullable public Void visitRvalue(Rvalue rv) throws EX                                                                           { return (Void) rv.accept((Visitor.RvalueVisitor<Void, EX>) this); }
        @Override @Nullable public Void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX          { Traverser.this.traverseSingleTypeImportDeclaration(stid); return null; }
        @Override @Nullable public Void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX     { Traverser.this.traverseTypeImportOnDemandDeclaration(tiodd); return null; }
        @Override @Nullable public Void visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) throws EX      { Traverser.this.traverseSingleStaticImportDeclaration(ssid); return null; }
        @Override @Nullable public Void visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX { Traverser.this.traverseStaticImportOnDemandDeclaration(siodd); return null; }
        @Override @Nullable public Void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX                               { Traverser.this.traverseAnonymousClassDeclaration(acd); return null; }
        @Override @Nullable public Void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX                                       { Traverser.this.traverseLocalClassDeclaration(lcd); return null; }
        @Override @Nullable public Void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) throws EX                      { Traverser.this.traversePackageMemberClassDeclaration(pmcd); return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX                             { Traverser.this.traverseMemberInterfaceDeclaration(mid); return null; }
        @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX              { Traverser.this.traversePackageMemberInterfaceDeclaration(pmid); return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX                                     { Traverser.this.traverseMemberClassDeclaration(mcd); return null; }
        @Override @Nullable public Void visitConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX                                        { Traverser.this.traverseConstructorDeclarator(cd); return null; }
        @Override @Nullable public Void visitInitializer(Java.Initializer i) throws EX                                                             { Traverser.this.traverseInitializer(i); return null; }
        @Override @Nullable public Void visitMethodDeclarator(Java.MethodDeclarator md) throws EX                                                  { Traverser.this.traverseMethodDeclarator(md); return null; }
        @Override @Nullable public Void visitFieldDeclaration(Java.FieldDeclaration fd) throws EX                                                  { Traverser.this.traverseFieldDeclaration(fd); return null; }
        @Override @Nullable public Void visitLabeledStatement(Java.LabeledStatement ls) throws EX                                                  { Traverser.this.traverseLabeledStatement(ls); return null; }
        @Override @Nullable public Void visitBlock(Java.Block b) throws EX                                                                         { Traverser.this.traverseBlock(b); return null; }
        @Override @Nullable public Void visitExpressionStatement(Java.ExpressionStatement es) throws EX                                            { Traverser.this.traverseExpressionStatement(es); return null; }
        @Override @Nullable public Void visitIfStatement(Java.IfStatement is) throws EX                                                            { Traverser.this.traverseIfStatement(is); return null; }
        @Override @Nullable public Void visitForStatement(Java.ForStatement fs) throws EX                                                          { Traverser.this.traverseForStatement(fs); return null; }
        @Override @Nullable public Void visitForEachStatement(Java.ForEachStatement fes) throws EX                                                 { Traverser.this.traverseForEachStatement(fes); return null; }
        @Override @Nullable public Void visitWhileStatement(Java.WhileStatement ws) throws EX                                                      { Traverser.this.traverseWhileStatement(ws); return null; }
        @Override @Nullable public Void visitTryStatement(Java.TryStatement ts) throws EX                                                          { Traverser.this.traverseTryStatement(ts); return null; }
        @Override @Nullable public Void visitSwitchStatement(Java.SwitchStatement ss) throws EX                                                    { Traverser.this.traverseSwitchStatement(ss); return null; }
        @Override @Nullable public Void visitSynchronizedStatement(Java.SynchronizedStatement ss) throws EX                                        { Traverser.this.traverseSynchronizedStatement(ss); return null; }
        @Override @Nullable public Void visitDoStatement(Java.DoStatement ds) throws EX                                                            { Traverser.this.traverseDoStatement(ds); return null; }
        @Override @Nullable public Void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX              { Traverser.this.traverseLocalVariableDeclarationStatement(lvds); return null; }
        @Override @Nullable public Void visitReturnStatement(Java.ReturnStatement rs) throws EX                                                    { Traverser.this.traverseReturnStatement(rs); return null; }
        @Override @Nullable public Void visitThrowStatement(Java.ThrowStatement ts) throws EX                                                      { Traverser.this.traverseThrowStatement(ts); return null; }
        @Override @Nullable public Void visitBreakStatement(Java.BreakStatement bs) throws EX                                                      { Traverser.this.traverseBreakStatement(bs); return null; }
        @Override @Nullable public Void visitContinueStatement(Java.ContinueStatement cs) throws EX                                                { Traverser.this.traverseContinueStatement(cs); return null; }
        @Override @Nullable public Void visitAssertStatement(Java.AssertStatement as) throws EX                                                    { Traverser.this.traverseAssertStatement(as); return null; }
        @Override @Nullable public Void visitEmptyStatement(Java.EmptyStatement es) throws EX                                                      { Traverser.this.traverseEmptyStatement(es); return null; }
        @Override @Nullable public Void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX                    { Traverser.this.traverseLocalClassDeclarationStatement(lcds); return null; }
        @Override @Nullable public Void visitPackage(Java.Package p) throws EX                                                                     { Traverser.this.traversePackage(p); return null; }
        @Override @Nullable public Void visitArrayLength(Java.ArrayLength al) throws EX                                                            { Traverser.this.traverseArrayLength(al); return null; }
        @Override @Nullable public Void visitAssignment(Java.Assignment a) throws EX                                                               { Traverser.this.traverseAssignment(a); return null; }
        @Override @Nullable public Void visitUnaryOperation(Java.UnaryOperation uo) throws EX                                                      { Traverser.this.traverseUnaryOperation(uo); return null; }
        @Override @Nullable public Void visitBinaryOperation(Java.BinaryOperation bo) throws EX                                                    { Traverser.this.traverseBinaryOperation(bo); return null; }
        @Override @Nullable public Void visitCast(Java.Cast c) throws EX                                                                           { Traverser.this.traverseCast(c); return null; }
        @Override @Nullable public Void visitClassLiteral(Java.ClassLiteral cl) throws EX                                                          { Traverser.this.traverseClassLiteral(cl); return null; }
        @Override @Nullable public Void visitConditionalExpression(Java.ConditionalExpression ce) throws EX                                        { Traverser.this.traverseConditionalExpression(ce); return null; }
        @Override @Nullable public Void visitCrement(Java.Crement c) throws EX                                                                     { Traverser.this.traverseCrement(c); return null; }
        @Override @Nullable public Void visitInstanceof(Java.Instanceof io) throws EX                                                              { Traverser.this.traverseInstanceof(io); return null; }
        @Override @Nullable public Void visitMethodInvocation(Java.MethodInvocation mi) throws EX                                                  { Traverser.this.traverseMethodInvocation(mi); return null; }
        @Override @Nullable public Void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX                             { Traverser.this.traverseSuperclassMethodInvocation(smi); return null; }
        @Override @Nullable public Void visitIntegerLiteral(Java.IntegerLiteral il) throws EX                                                      { Traverser.this.traverseIntegerLiteral(il); return null; }
        @Override @Nullable public Void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) throws EX                                         { Traverser.this.traverseFloatingPointLiteral(fpl); return null; }
        @Override @Nullable public Void visitBooleanLiteral(Java.BooleanLiteral bl) throws EX                                                      { Traverser.this.traverseBooleanLiteral(bl); return null; }
        @Override @Nullable public Void visitCharacterLiteral(Java.CharacterLiteral cl) throws EX                                                  { Traverser.this.traverseCharacterLiteral(cl); return null; }
        @Override @Nullable public Void visitStringLiteral(Java.StringLiteral sl) throws EX                                                        { Traverser.this.traverseStringLiteral(sl); return null; }
        @Override @Nullable public Void visitNullLiteral(Java.NullLiteral nl) throws EX                                                            { Traverser.this.traverseNullLiteral(nl); return null; }
        @Override @Nullable public Void visitSimpleConstant(Java.SimpleConstant sl) throws EX                                                      { Traverser.this.traverseSimpleLiteral(sl); return null; }
        @Override @Nullable public Void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) throws EX                              { Traverser.this.traverseNewAnonymousClassInstance(naci); return null; }
        @Override @Nullable public Void visitNewArray(Java.NewArray na) throws EX                                                                  { Traverser.this.traverseNewArray(na); return null; }
        @Override @Nullable public Void visitNewInitializedArray(Java.NewInitializedArray nia) throws EX                                           { Traverser.this.traverseNewInitializedArray(nia); return null; }
        @Override @Nullable public Void visitNewClassInstance(Java.NewClassInstance nci) throws EX                                                 { Traverser.this.traverseNewClassInstance(nci); return null; }
        @Override @Nullable public Void visitParameterAccess(Java.ParameterAccess pa) throws EX                                                    { Traverser.this.traverseParameterAccess(pa); return null; }
        @Override @Nullable public Void visitQualifiedThisReference(Java.QualifiedThisReference qtr) throws EX                                     { Traverser.this.traverseQualifiedThisReference(qtr); return null; }
        @Override @Nullable public Void visitThisReference(Java.ThisReference tr) throws EX                                                        { Traverser.this.traverseThisReference(tr); return null; }
        @Override @Nullable public Void visitArrayType(Java.ArrayType at) throws EX                                                                { Traverser.this.traverseArrayType(at); return null; }
        @Override @Nullable public Void visitBasicType(Java.BasicType bt) throws EX                                                                { Traverser.this.traverseBasicType(bt); return null; }
        @Override @Nullable public Void visitReferenceType(Java.ReferenceType rt) throws EX                                                        { Traverser.this.traverseReferenceType(rt); return null; }
        @Override @Nullable public Void visitRvalueMemberType(Java.RvalueMemberType rmt) throws EX                                                 { Traverser.this.traverseRvalueMemberType(rmt); return null; }
        @Override @Nullable public Void visitSimpleType(Java.SimpleType st) throws EX                                                              { Traverser.this.traverseSimpleType(st); return null; }
        @Override @Nullable public Void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX                     { Traverser.this.traverseAlternateConstructorInvocation(aci); return null; }
        @Override @Nullable public Void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX                             { Traverser.this.traverseSuperConstructorInvocation(sci); return null; }
        @Override @Nullable public Void visitAmbiguousName(Java.AmbiguousName an) throws EX                                                        { Traverser.this.traverseAmbiguousName(an); return null; }
        @Override @Nullable public Void visitArrayAccessExpression(Java.ArrayAccessExpression aae) throws EX                                       { Traverser.this.traverseArrayAccessExpression(aae); return null; }
        @Override @Nullable public Void visitFieldAccess(Java.FieldAccess fa) throws EX                                                            { Traverser.this.traverseFieldAccess(fa); return null; }
        @Override @Nullable public Void visitFieldAccessExpression(Java.FieldAccessExpression fae) throws EX                                       { Traverser.this.traverseFieldAccessExpression(fae); return null; }
        @Override @Nullable public Void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX                 { Traverser.this.traverseSuperclassFieldAccessExpression(scfae); return null; }
        @Override @Nullable public Void visitLocalVariableAccess(Java.LocalVariableAccess lva) throws EX                                           { Traverser.this.traverseLocalVariableAccess(lva); return null; }
        @Override @Nullable public Void visitParenthesizedExpression(Java.ParenthesizedExpression pe) throws EX                                    { Traverser.this.traverseParenthesizedExpression(pe); return null; }
        @Override @Nullable public Void visitMarkerAnnotation(Java.MarkerAnnotation ma) throws EX                                                  { Traverser.this.traverseMarkerAnnotation(ma); return null; }
        @Override @Nullable public Void visitNormalAnnotation(Java.NormalAnnotation na) throws EX                                                  { Traverser.this.traverseNormalAnnotation(na); return null; }
        @Override @Nullable public Void visitSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX                                   { Traverser.this.traverseSingleElementAnnotation(sea); return null; }
        @Override @Nullable public Void visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX                        { Traverser.this.traverseElementValueArrayInitializer(evai); return null; }
        @Override @Nullable public Void visitAnnotation(Annotation a) throws EX                                                                    { Traverser.this.traverseAnnotation(a); return null; }
        @Override @Nullable public Void visitEnumConstant(Java.EnumConstant ec) throws EX                                                          { Traverser.this.traverseEnumConstant(ec); return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX                                       { Traverser.this.traverseMemberEnumDeclaration(med); return null; }
        @Override @Nullable public Void visitPackageMemberEnumDeclaration(Java.PackageMemberEnumDeclaration pmed) throws EX                        { Traverser.this.traversePackageMemberEnumDeclaration(pmed); return null; }
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(Java.MemberAnnotationTypeDeclaration matd) throws EX                  { Traverser.this.traverseMemberAnnotationTypeDeclaration(matd); return null; }
        @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(Java.PackageMemberAnnotationTypeDeclaration pmatd) throws EX   { Traverser.this.traversePackageMemberAnnotationTypeDeclaration(pmatd); return null; }
        // CHECKSTYLE LineLengthCheck:ON
    };

    /** @see Traverser */
    public ComprehensiveVisitor<Void, EX>
    comprehensiveVisitor() { return this.cv; }

    // These may be overridden by derived classes.

    /** @see Traverser */
    public void
    traverseCompilationUnit(Java.CompilationUnit cu) throws EX {

        // The optionalPackageDeclaration is considered an integral part of
        // the compilation unit and is thus not traversed.

        for (Java.CompilationUnit.ImportDeclaration id : cu.importDeclarations) id.accept(this.cv);
        for (Java.PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) pmtd.accept(this.cv);
    }

    /** @see Traverser */
    public void
    traverseSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /** @see Traverser */
    public void
    traverseTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX {
        this.traverseImportDeclaration(tiodd);
    }

    /** @see Traverser */
    public void
    traverseSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /** @see Traverser */
    public void
    traverseStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX {
        this.traverseImportDeclaration(siodd);
    }

    /** @see Traverser */
    public void
    traverseImportDeclaration(Java.CompilationUnit.ImportDeclaration id) throws EX { this.traverseLocated(id); }

    /** @see Traverser */
    public void
    traverseAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX {
        acd.baseType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseClassDeclaration(acd);
    }

    /** @see Traverser */
    public void
    traverseLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX { this.traverseNamedClassDeclaration(lcd); }

    /** @see Traverser */
    public void
    traversePackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) throws EX {
        this.traverseNamedClassDeclaration(pmcd);
    }

    /** @see Traverser */
    public void
    traverseMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX {
        this.traverseInterfaceDeclaration(mid);
    }

    /** @see Traverser */
    public void
    traversePackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX {
        this.traverseInterfaceDeclaration(pmid);
    }

    /** @see Traverser */
    public void
    traverseMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX {
        this.traverseNamedClassDeclaration(mcd);
    }

    /** @see Traverser */
    public void
    traverseConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX {
        if (cd.optionalConstructorInvocation != null) {
            cd.optionalConstructorInvocation.accept((Visitor.BlockStatementVisitor<Void, EX>) this.cv);
        }
        this.traverseFunctionDeclarator(cd);
    }

    /** @see Traverser */
    public void
    traverseInitializer(Java.Initializer i) throws EX {
        i.block.accept(this.cv);
        this.traverseAbstractTypeBodyDeclaration(i);
    }

    /** @see Traverser */
    public void
    traverseMethodDeclarator(Java.MethodDeclarator md) throws EX { this.traverseFunctionDeclarator(md); }

    /** @see Traverser */
    public void
    traverseFieldDeclaration(Java.FieldDeclaration fd) throws EX {
        fd.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        for (Java.VariableDeclarator vd : fd.variableDeclarators) {
            Java.ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(fd);
    }

    /** @see Traverser */
    public void
    traverseLabeledStatement(Java.LabeledStatement ls) throws EX {
        ls.body.accept(this.cv);
        this.traverseBreakableStatement(ls);
    }

    /** @see Traverser */
    public void
    traverseBlock(Java.Block b) throws EX {
        for (Java.BlockStatement bs : b.statements) bs.accept(this.cv);
        this.traverseStatement(b);
    }

    /** @see Traverser */
    public void
    traverseExpressionStatement(Java.ExpressionStatement es) throws EX {
        es.rvalue.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseStatement(es);
    }

    /** @see Traverser */
    public void
    traverseIfStatement(Java.IfStatement is) throws EX {
        is.condition.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        is.thenStatement.accept(this.cv);
        if (is.optionalElseStatement != null) is.optionalElseStatement.accept(this.cv);
        this.traverseStatement(is);
    }

    /** @see Traverser */
    public void
    traverseForStatement(Java.ForStatement fs) throws EX {
        if (fs.optionalInit != null) fs.optionalInit.accept(this.cv);
        if (fs.optionalCondition != null) fs.optionalCondition.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        if (fs.optionalUpdate != null) {
            for (Java.Rvalue rv : fs.optionalUpdate) rv.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        }
        fs.body.accept(this.cv);
        this.traverseContinuableStatement(fs);
    }

    /** @see Traverser */
    public void
    traverseForEachStatement(Java.ForEachStatement fes) throws EX {
        this.traverseFormalParameter(fes.currentElement);
        fes.expression.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        fes.body.accept(this.cv);
        this.traverseContinuableStatement(fes);
    }

    /** @see Traverser */
    public void
    traverseWhileStatement(Java.WhileStatement ws) throws EX {
        ws.condition.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        ws.body.accept(this.cv);
        this.traverseContinuableStatement(ws);
    }

    /** @see Traverser */
    public void
    traverseTryStatement(Java.TryStatement ts) throws EX {
        ts.body.accept(this.cv);
        for (Java.CatchClause cc : ts.catchClauses) cc.body.accept(this.cv);
        if (ts.optionalFinally != null) ts.optionalFinally.accept(this.cv);
        this.traverseStatement(ts);
    }

    /** @see Traverser */
    public void
    traverseSwitchStatement(Java.SwitchStatement ss) throws EX {
        ss.condition.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        for (Java.SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (Java.Rvalue cl : sbsg.caseLabels) cl.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
            for (Java.BlockStatement bs : sbsg.blockStatements) bs.accept(this.cv);
            this.traverseLocated(sbsg);
        }
        this.traverseBreakableStatement(ss);
    }

    /** @see Traverser */
    public void
    traverseSynchronizedStatement(Java.SynchronizedStatement ss) throws EX {
        ss.expression.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        ss.body.accept(this.cv);
        this.traverseStatement(ss);
    }

    /** @see Traverser */
    public void
    traverseDoStatement(Java.DoStatement ds) throws EX {
        ds.body.accept(this.cv);
        ds.condition.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseContinuableStatement(ds);
    }

    /** @see Traverser */
    public void
    traverseLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX {
        lvds.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        for (Java.VariableDeclarator vd : lvds.variableDeclarators) {
            Java.ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(lvds);
    }

    /** @see Traverser */
    public void
    traverseReturnStatement(Java.ReturnStatement rs) throws EX {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseStatement(rs);
    }

    /** @see Traverser */
    public void
    traverseThrowStatement(Java.ThrowStatement ts) throws EX {
        ts.expression.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseStatement(ts);
    }

    /** @see Traverser */
    public void
    traverseBreakStatement(Java.BreakStatement bs) throws EX { this.traverseStatement(bs); }

    /** @see Traverser */
    public void
    traverseContinueStatement(Java.ContinueStatement cs) throws EX { this.traverseStatement(cs); }

    /** @see Traverser */
    public void
    traverseAssertStatement(Java.AssertStatement as) throws EX {
        as.expression1.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseStatement(as);
    }

    /** @see Traverser */
    public void
    traverseEmptyStatement(Java.EmptyStatement es) throws EX { this.traverseStatement(es); }

    /** @see Traverser */
    public void
    traverseLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX {
        lcds.lcd.accept(this.cv);
        this.traverseStatement(lcds);
    }

    /** @see Traverser */
    public void
    traversePackage(Java.Package p) throws EX { this.traverseAtom(p); }

    /** @see Traverser */
    public void
    traverseArrayLength(Java.ArrayLength al) throws EX {
        al.lhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(al);
    }

    /** @see Traverser */
    public void
    traverseAssignment(Java.Assignment a) throws EX {
        a.lhs.accept((Visitor.LvalueVisitor<Void, EX>) this.cv);
        a.rhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(a);
    }

    /** @see Traverser */
    public void
    traverseUnaryOperation(Java.UnaryOperation uo) throws EX {
        uo.operand.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseBooleanRvalue(uo);
    }

    /** @see Traverser */
    public void
    traverseBinaryOperation(Java.BinaryOperation bo) throws EX {
        bo.lhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        bo.rhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseBooleanRvalue(bo);
    }

    /** @see Traverser */
    public void
    traverseCast(Java.Cast c) throws EX {
        c.targetType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        c.value.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(c);
    }

    /** @see Traverser */
    public void
    traverseClassLiteral(Java.ClassLiteral cl) throws EX {
        cl.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseRvalue(cl);
    }

    /** @see Traverser */
    public void
    traverseConditionalExpression(Java.ConditionalExpression ce) throws EX {
        ce.lhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        ce.mhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        ce.rhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(ce);
    }

    /** @see Traverser */
    public void
    traverseCrement(Java.Crement c) throws EX {
        c.operand.accept((Visitor.LvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(c);
    }

    /** @see Traverser */
    public void
    traverseInstanceof(Java.Instanceof io) throws EX {
        io.lhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        io.rhs.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseRvalue(io);
    }

    /** @see Traverser */
    public void
    traverseMethodInvocation(Java.MethodInvocation mi) throws EX {
        if (mi.optionalTarget != null) mi.optionalTarget.accept(this.cv);
        this.traverseInvocation(mi);
    }

    /** @see Traverser */
    public void
    traverseSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX { this.traverseInvocation(smi); }

    /** @see Traverser */
    public void
    traverseLiteral(Java.Literal l) throws EX { this.traverseRvalue(l); }

    /** @see Traverser */
    public void
    traverseIntegerLiteral(Java.IntegerLiteral il) throws EX { this.traverseLiteral(il); }

    /** @see Traverser */
    public void
    traverseFloatingPointLiteral(Java.FloatingPointLiteral fpl) throws EX { this.traverseLiteral(fpl); }

    /** @see Traverser */
    public void
    traverseBooleanLiteral(Java.BooleanLiteral bl) throws EX { this.traverseLiteral(bl); }

    /** @see Traverser */
    public void
    traverseCharacterLiteral(Java.CharacterLiteral cl) throws EX { this.traverseLiteral(cl); }

    /** @see Traverser */
    public void
    traverseStringLiteral(Java.StringLiteral sl) throws EX { this.traverseLiteral(sl); }

    /** @see Traverser */
    public void
    traverseNullLiteral(Java.NullLiteral nl) throws EX { this.traverseLiteral(nl); }

    /** @see Traverser */
    public void
    traverseSimpleLiteral(Java.SimpleConstant sl) throws EX { this.traverseRvalue(sl); }

    /** @see Traverser */
    public void
    traverseNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) throws EX {
        if (naci.optionalQualification != null) {
            naci.optionalQualification.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        }
        naci.anonymousClassDeclaration.accept(this.cv);
        for (Java.Rvalue argument : naci.arguments) argument.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(naci);
    }

    /** @see Traverser */
    public void
    traverseNewArray(Java.NewArray na) throws EX {
        na.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        for (Rvalue dimExpr : na.dimExprs) dimExpr.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(na);
    }

    /** @see Traverser */
    public void
    traverseNewInitializedArray(Java.NewInitializedArray nia) throws EX {
        assert nia.arrayType != null;
        nia.arrayType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseArrayInitializerOrRvalue(nia.arrayInitializer);
    }

    /** @see Traverser */
    public void
    traverseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) throws EX {
        if (aiorv instanceof Java.Rvalue) {
            ((Java.Atom) aiorv).accept(this.cv);
        } else
        if (aiorv instanceof Java.ArrayInitializer) {
            Java.ArrayInitializerOrRvalue[] values = ((Java.ArrayInitializer) aiorv).values;
            for (Java.ArrayInitializerOrRvalue value : values) this.traverseArrayInitializerOrRvalue(value);
        } else
        {
            throw new JaninoRuntimeException(
                "Unexpected array initializer or rvalue class "
                + aiorv.getClass().getName()
            );
        }
    }
    /** @see Traverser */
    public void
    traverseNewClassInstance(Java.NewClassInstance nci) throws EX {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        }
        if (nci.type != null) nci.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        for (Java.Rvalue argument : nci.arguments) argument.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(nci);
    }

    /** @see Traverser */
    public void
    traverseParameterAccess(Java.ParameterAccess pa) throws EX { this.traverseRvalue(pa); }

    /** @see Traverser */
    public void
    traverseQualifiedThisReference(Java.QualifiedThisReference qtr) throws EX {
        qtr.qualification.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseRvalue(qtr);
    }

    /** @see Traverser */
    public void
    traverseThisReference(Java.ThisReference tr) throws EX { this.traverseRvalue(tr); }

    /** @see Traverser */
    public void
    traverseArrayType(Java.ArrayType at) throws EX {
        at.componentType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseType(at);
    }

    /** @see Traverser */
    public void
    traverseBasicType(Java.BasicType bt) throws EX { this.traverseType(bt); }

    /** @see Traverser */
    public void
    traverseReferenceType(Java.ReferenceType rt) throws EX { this.traverseType(rt); }

    /** @see Traverser */
    public void
    traverseRvalueMemberType(Java.RvalueMemberType rmt) throws EX {
        rmt.rvalue.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseType(rmt);
    }

    /** @see Traverser */
    public void
    traverseSimpleType(Java.SimpleType st) throws EX { this.traverseType(st); }

    /** @see Traverser */
    public void
    traverseAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX {
        this.traverseConstructorInvocation(aci);
    }

    /** @see Traverser */
    public void
    traverseSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX {
        if (sci.optionalQualification != null) {
            sci.optionalQualification.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        }
        this.traverseConstructorInvocation(sci);
    }

    /** @see Traverser */
    public void
    traverseAmbiguousName(Java.AmbiguousName an) throws EX { this.traverseLvalue(an); }

    /** @see Traverser */
    public void
    traverseArrayAccessExpression(Java.ArrayAccessExpression aae) throws EX {
        aae.lhs.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        ((Java.Atom) aae.index).accept(this.cv);
        this.traverseLvalue(aae);
    }

    /** @see Traverser */
    public void
    traverseFieldAccess(Java.FieldAccess fa) throws EX {
        fa.lhs.accept(this.cv);
        this.traverseLvalue(fa);
    }

    /** @see Traverser */
    public void
    traverseFieldAccessExpression(Java.FieldAccessExpression fae) throws EX {
        fae.lhs.accept(this.cv);
        this.traverseLvalue(fae);
    }

    /** @see Traverser */
    public void
    traverseSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX {
        if (scfae.optionalQualification != null) {
            scfae.optionalQualification.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        }
        this.traverseLvalue(scfae);
    }

    /** @see Traverser */
    public void
    traverseLocalVariableAccess(Java.LocalVariableAccess lva) throws EX { this.traverseLvalue(lva); }

    /** @see Traverser */
    public void
    traverseParenthesizedExpression(Java.ParenthesizedExpression pe) throws EX {
        pe.value.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseLvalue(pe);
    }

    /** @see Traverser */
    public void
    traverseElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX {
        for (Java.ElementValue elementValue : evai.elementValues) elementValue.accept(this.cv);
        this.traverseElementValue(evai);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    public void
    traverseElementValue(Java.ElementValue ev) throws EX {}

    /** @see Traverser */
    public void
    traverseSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX {
        sea.type.accept(this.cv);
        sea.elementValue.accept(this.cv);
        this.traverseAnnotation(sea);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    public void
    traverseAnnotation(Java.Annotation a) throws EX {}

    /** @see Traverser */
    public void
    traverseNormalAnnotation(Java.NormalAnnotation na) throws EX {
        na.type.accept(this.cv);
        for (Java.ElementValuePair elementValuePair : na.elementValuePairs) {
            elementValuePair.elementValue.accept(this.cv);
        }
        this.traverseAnnotation(na);
    }

    /** @see Traverser */
    public void
    traverseMarkerAnnotation(Java.MarkerAnnotation ma) throws EX {
        ma.type.accept(this.cv);
        this.traverseAnnotation(ma);
    }

    /** @see Traverser */
    public void
    traverseClassDeclaration(Java.ClassDeclaration cd) throws EX {
        for (Java.ConstructorDeclarator ctord : cd.constructors) ctord.accept(this.cv);
        for (Java.BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) vdoi.accept(this.cv);
        this.traverseAbstractTypeDeclaration(cd);
    }

    /** @see Traverser */
    public void
    traverseAbstractTypeDeclaration(Java.AbstractTypeDeclaration atd) throws EX {
        for (Java.Annotation a : atd.getAnnotations()) this.traverseAnnotation(a);
        for (Java.NamedTypeDeclaration mtd : atd.getMemberTypeDeclarations()) mtd.accept(this.cv);
        for (Java.MethodDeclarator md : atd.getMethodDeclarations()) this.traverseMethodDeclarator(md);
    }

    /** @see Traverser */
    public void
    traverseNamedClassDeclaration(Java.NamedClassDeclaration ncd) throws EX {
        for (Java.Type implementedType : ncd.implementedTypes) {
            implementedType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseClassDeclaration(ncd);
    }

    /** @see Traverser */
    public void
    traverseInterfaceDeclaration(Java.InterfaceDeclaration id) throws EX {
        for (Java.TypeBodyDeclaration cd : id.constantDeclarations) cd.accept(this.cv);
        for (Java.Type extendedType : id.extendedTypes) extendedType.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
        this.traverseAbstractTypeDeclaration(id);
    }

    /** @see Traverser */
    public void
    traverseFunctionDeclarator(Java.FunctionDeclarator fd) throws EX {
        this.traverseFormalParameters(fd.formalParameters);
        if (fd.optionalStatements != null) {
            for (Java.BlockStatement bs : fd.optionalStatements) bs.accept(this.cv);
        }
    }

    /** @see Traverser */
    public void
    traverseFormalParameters(Java.FunctionDeclarator.FormalParameters formalParameters) throws EX {
        for (Java.FunctionDeclarator.FormalParameter formalParameter : formalParameters.parameters) {
            this.traverseFormalParameter(formalParameter);
        }
    }

    /** @see Traverser */
    public void
    traverseFormalParameter(Java.FunctionDeclarator.FormalParameter formalParameter) throws EX {
        formalParameter.type.accept((Visitor.TypeVisitor<Void, EX>) this.cv);
    }

    /** @see Traverser */
    public void
    traverseAbstractTypeBodyDeclaration(Java.AbstractTypeBodyDeclaration atbd) throws EX { this.traverseLocated(atbd); }

    /** @see Traverser */
    public void
    traverseStatement(Java.Statement s) throws EX { this.traverseLocated(s); }

    /** @see Traverser */
    public void
    traverseBreakableStatement(Java.BreakableStatement bs) throws EX { this.traverseStatement(bs); }

    /** @see Traverser */
    public void
    traverseContinuableStatement(Java.ContinuableStatement cs) throws EX { this.traverseBreakableStatement(cs); }

    /** @see Traverser */
    public void
    traverseRvalue(Java.Rvalue rv) throws EX { this.traverseAtom(rv); }

    /** @see Traverser */
    public void
    traverseBooleanRvalue(Java.BooleanRvalue brv) throws EX { this.traverseRvalue(brv); }

    /** @see Traverser */
    public void
    traverseInvocation(Java.Invocation i) throws EX {
        for (Java.Rvalue argument : i.arguments) argument.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseRvalue(i);
    }

    /** @see Traverser */
    public void
    traverseConstructorInvocation(Java.ConstructorInvocation ci) throws EX {
        for (Java.Rvalue argument : ci.arguments) argument.accept((Visitor.RvalueVisitor<Void, EX>) this.cv);
        this.traverseAtom(ci);
    }

    /** @see Traverser */
    public void
    traverseEnumConstant(Java.EnumConstant ec) throws EX {

        for (Java.ConstructorDeclarator cd : ec.constructors) this.traverseConstructorDeclarator(cd);

        if (ec.optionalArguments != null) {
            for (Rvalue a : ec.optionalArguments) this.traverseRvalue(a);
        }

        this.traverseAbstractTypeDeclaration(ec);
    }

    /** @see Traverser */
    public void
    traversePackageMemberEnumDeclaration(Java.PackageMemberEnumDeclaration pmed) throws EX {
        this.traversePackageMemberClassDeclaration(pmed);
    }

    /** @see Traverser */
    public void
    traverseMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX {
        this.traverseMemberClassDeclaration(med);
    }

    /** @see Traverser */
    public void
    traversePackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws EX {
        this.traversePackageMemberInterfaceDeclaration(pmatd);
    }

    /** @see Traverser */
    public void
    traverseMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws EX {
        this.traverseMemberInterfaceDeclaration(matd);
    }

    /** @see Traverser */
    public void
    traverseLvalue(Java.Lvalue lv) throws EX { this.traverseRvalue(lv); }

    /** @see Traverser */
    public void
    traverseType(Java.Type t) throws EX { this.traverseAtom(t); }

    /** @see Traverser */
    public void
    traverseAtom(Java.Atom a) throws EX { this.traverseLocated(a); }

    /**
     * @throws EX
     * @see Traverser
     */
    public void
    traverseLocated(Java.Located l) throws EX {}
}
