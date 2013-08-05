
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

import org.codehaus.janino.*;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Visitor.ComprehensiveVisitor;

import java.util.*;

/**
 * This class traverses the subnodes of an AST. Derived classes may override
 * individual methods to process specific nodes, e.g.:<pre>
 *     LocalClassDeclaration lcd = ...;
 *     lcd.accept(new Traverser() {
 *         int n = 0;
 *         public void traverseMethodDeclarator(Java.MethodDeclarator md) {
 *             ++this.n;
 *             super.traverseMethodDeclarator(md);
 *         }
 *     }.comprehensiveVisitor());</pre>
 */
@SuppressWarnings("rawtypes") public
class Traverser {
    private final Visitor.ComprehensiveVisitor cv = new Visitor.ComprehensiveVisitor() {
        // CHECKSTYLE LineLengthCheck:OFF
        @Override public void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid)          { Traverser.this.traverseSingleTypeImportDeclaration(stid); }
        @Override public void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd)     { Traverser.this.traverseTypeImportOnDemandDeclaration(tiodd); }
        @Override public void visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid)      { Traverser.this.traverseSingleStaticImportDeclaration(ssid); }
        @Override public void visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) { Traverser.this.traverseStaticImportOnDemandDeclaration(siodd); }
        @Override public void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd)                               { Traverser.this.traverseAnonymousClassDeclaration(acd); }
        @Override public void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd)                                       { Traverser.this.traverseLocalClassDeclaration(lcd); }
        @Override public void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd)                      { Traverser.this.traversePackageMemberClassDeclaration(pmcd); }
        @Override public void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid)                             { Traverser.this.traverseMemberInterfaceDeclaration(mid); }
        @Override public void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid)              { Traverser.this.traversePackageMemberInterfaceDeclaration(pmid); }
        @Override public void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd)                                     { Traverser.this.traverseMemberClassDeclaration(mcd); }
        @Override public void visitConstructorDeclarator(Java.ConstructorDeclarator cd)                                        { Traverser.this.traverseConstructorDeclarator(cd); }
        @Override public void visitInitializer(Java.Initializer i)                                                             { Traverser.this.traverseInitializer(i); }
        @Override public void visitMethodDeclarator(Java.MethodDeclarator md)                                                  { Traverser.this.traverseMethodDeclarator(md); }
        @Override public void visitFieldDeclaration(Java.FieldDeclaration fd)                                                  { Traverser.this.traverseFieldDeclaration(fd); }
        @Override public void visitLabeledStatement(Java.LabeledStatement ls)                                                  { Traverser.this.traverseLabeledStatement(ls); }
        @Override public void visitBlock(Java.Block b)                                                                         { Traverser.this.traverseBlock(b); }
        @Override public void visitExpressionStatement(Java.ExpressionStatement es)                                            { Traverser.this.traverseExpressionStatement(es); }
        @Override public void visitIfStatement(Java.IfStatement is)                                                            { Traverser.this.traverseIfStatement(is); }
        @Override public void visitForStatement(Java.ForStatement fs)                                                          { Traverser.this.traverseForStatement(fs); }
        @Override public void visitWhileStatement(Java.WhileStatement ws)                                                      { Traverser.this.traverseWhileStatement(ws); }
        @Override public void visitTryStatement(Java.TryStatement ts)                                                          { Traverser.this.traverseTryStatement(ts); }
        @Override public void visitSwitchStatement(Java.SwitchStatement ss)                                                    { Traverser.this.traverseSwitchStatement(ss); }
        @Override public void visitSynchronizedStatement(Java.SynchronizedStatement ss)                                        { Traverser.this.traverseSynchronizedStatement(ss); }
        @Override public void visitDoStatement(Java.DoStatement ds)                                                            { Traverser.this.traverseDoStatement(ds); }
        @Override public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds)              { Traverser.this.traverseLocalVariableDeclarationStatement(lvds); }
        @Override public void visitReturnStatement(Java.ReturnStatement rs)                                                    { Traverser.this.traverseReturnStatement(rs); }
        @Override public void visitThrowStatement(Java.ThrowStatement ts)                                                      { Traverser.this.traverseThrowStatement(ts); }
        @Override public void visitBreakStatement(Java.BreakStatement bs)                                                      { Traverser.this.traverseBreakStatement(bs); }
        @Override public void visitContinueStatement(Java.ContinueStatement cs)                                                { Traverser.this.traverseContinueStatement(cs); }
        @Override public void visitAssertStatement(Java.AssertStatement as)                                                    { Traverser.this.traverseAssertStatement(as); }
        @Override public void visitEmptyStatement(Java.EmptyStatement es)                                                      { Traverser.this.traverseEmptyStatement(es); }
        @Override public void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds)                    { Traverser.this.traverseLocalClassDeclarationStatement(lcds); }
        @Override public void visitPackage(Java.Package p)                                                                     { Traverser.this.traversePackage(p); }
        @Override public void visitArrayLength(Java.ArrayLength al)                                                            { Traverser.this.traverseArrayLength(al); }
        @Override public void visitAssignment(Java.Assignment a)                                                               { Traverser.this.traverseAssignment(a); }
        @Override public void visitUnaryOperation(Java.UnaryOperation uo)                                                      { Traverser.this.traverseUnaryOperation(uo); }
        @Override public void visitBinaryOperation(Java.BinaryOperation bo)                                                    { Traverser.this.traverseBinaryOperation(bo); }
        @Override public void visitCast(Java.Cast c)                                                                           { Traverser.this.traverseCast(c); }
        @Override public void visitClassLiteral(Java.ClassLiteral cl)                                                          { Traverser.this.traverseClassLiteral(cl); }
        @Override public void visitConditionalExpression(Java.ConditionalExpression ce)                                        { Traverser.this.traverseConditionalExpression(ce); }
        @Override public void visitCrement(Java.Crement c)                                                                     { Traverser.this.traverseCrement(c); }
        @Override public void visitInstanceof(Java.Instanceof io)                                                              { Traverser.this.traverseInstanceof(io); }
        @Override public void visitMethodInvocation(Java.MethodInvocation mi)                                                  { Traverser.this.traverseMethodInvocation(mi); }
        @Override public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi)                             { Traverser.this.traverseSuperclassMethodInvocation(smi); }
        @Override public void visitIntegerLiteral(Java.IntegerLiteral il)                                                      { Traverser.this.traverseIntegerLiteral(il); }
        @Override public void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl)                                         { Traverser.this.traverseFloatingPointLiteral(fpl); }
        @Override public void visitBooleanLiteral(Java.BooleanLiteral bl)                                                      { Traverser.this.traverseBooleanLiteral(bl); }
        @Override public void visitCharacterLiteral(Java.CharacterLiteral cl)                                                  { Traverser.this.traverseCharacterLiteral(cl); }
        @Override public void visitStringLiteral(Java.StringLiteral sl)                                                        { Traverser.this.traverseStringLiteral(sl); }
        @Override public void visitNullLiteral(Java.NullLiteral nl)                                                            { Traverser.this.traverseNullLiteral(nl); }
        @Override public void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci)                              { Traverser.this.traverseNewAnonymousClassInstance(naci); }
        @Override public void visitNewArray(Java.NewArray na)                                                                  { Traverser.this.traverseNewArray(na); }
        @Override public void visitNewInitializedArray(Java.NewInitializedArray nia)                                           { Traverser.this.traverseNewInitializedArray(nia); }
        @Override public void visitNewClassInstance(Java.NewClassInstance nci)                                                 { Traverser.this.traverseNewClassInstance(nci); }
        @Override public void visitParameterAccess(Java.ParameterAccess pa)                                                    { Traverser.this.traverseParameterAccess(pa); }
        @Override public void visitQualifiedThisReference(Java.QualifiedThisReference qtr)                                     { Traverser.this.traverseQualifiedThisReference(qtr); }
        @Override public void visitThisReference(Java.ThisReference tr)                                                        { Traverser.this.traverseThisReference(tr); }
        @Override public void visitArrayType(Java.ArrayType at)                                                                { Traverser.this.traverseArrayType(at); }
        @Override public void visitBasicType(Java.BasicType bt)                                                                { Traverser.this.traverseBasicType(bt); }
        @Override public void visitReferenceType(Java.ReferenceType rt)                                                        { Traverser.this.traverseReferenceType(rt); }
        @Override public void visitRvalueMemberType(Java.RvalueMemberType rmt)                                                 { Traverser.this.traverseRvalueMemberType(rmt); }
        @Override public void visitSimpleType(Java.SimpleType st)                                                              { Traverser.this.traverseSimpleType(st); }
        @Override public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci)                     { Traverser.this.traverseAlternateConstructorInvocation(aci); }
        @Override public void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci)                             { Traverser.this.traverseSuperConstructorInvocation(sci); }
        @Override public void visitAmbiguousName(Java.AmbiguousName an)                                                        { Traverser.this.traverseAmbiguousName(an); }
        @Override public void visitArrayAccessExpression(Java.ArrayAccessExpression aae)                                       { Traverser.this.traverseArrayAccessExpression(aae); }
        @Override public void visitFieldAccess(Java.FieldAccess fa)                                                            { Traverser.this.traverseFieldAccess(fa); }
        @Override public void visitFieldAccessExpression(Java.FieldAccessExpression fae)                                       { Traverser.this.traverseFieldAccessExpression(fae); }
        @Override public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae)                 { Traverser.this.traverseSuperclassFieldAccessExpression(scfae); }
        @Override public void visitLocalVariableAccess(Java.LocalVariableAccess lva)                                           { Traverser.this.traverseLocalVariableAccess(lva); }
        @Override public void visitParenthesizedExpression(Java.ParenthesizedExpression pe)                                    { Traverser.this.traverseParenthesizedExpression(pe); }
        @Override public void visitMarkerAnnotation(MarkerAnnotation ma)                                                       { Traverser.this.traverseMarkerAnnotation(ma); }
        @Override public void visitNormalAnnotation(NormalAnnotation na)                                                       { Traverser.this.traverseNormalAnnotation(na); }
        @Override public void visitSingleElementAnnotation(SingleElementAnnotation sea)                                        { Traverser.this.traverseSingleElementAnnotation(sea); }
        @Override public void visitElementValueArrayInitializer(ElementValueArrayInitializer evai)                             { Traverser.this.traverseElementValueArrayInitializer(evai); }
        // CHECKSTYLE LineLengthCheck:ON
    };

    public ComprehensiveVisitor
    comprehensiveVisitor() { return this.cv; }

    // These may be overridden by derived classes.

    public void
    traverseCompilationUnit(Java.CompilationUnit cu) {

        // The optionalPackageDeclaration is considered an integral part of
        // the compilation unit and is thus not traversed.

        for (Iterator it = cu.importDeclarations.iterator(); it.hasNext();) {
            ((Java.CompilationUnit.ImportDeclaration) it.next()).accept(this.cv);
        }
        for (Iterator it = cu.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            ((Java.PackageMemberTypeDeclaration) it.next()).accept(this.cv);
        }
    }

    public void
    traverseSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) {
        this.traverseImportDeclaration(stid);
    }

    public void
    traverseTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) {
        this.traverseImportDeclaration(tiodd);
    }

    public void
    traverseSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration stid) {
        this.traverseImportDeclaration(stid);
    }

    public void
    traverseStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) {
        this.traverseImportDeclaration(siodd);
    }

    public void
    traverseImportDeclaration(Java.CompilationUnit.ImportDeclaration id) {
        this.traverseLocated(id);
    }

    public void
    traverseAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
        acd.baseType.accept((Visitor.TypeVisitor) this.cv);
        this.traverseClassDeclaration(acd);
    }

    public void
    traverseLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
        this.traverseNamedClassDeclaration(lcd);
    }

    public void
    traversePackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) {
        this.traverseNamedClassDeclaration(pmcd);
    }

    public void
    traverseMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
        this.traverseInterfaceDeclaration(mid);
    }

    public void
    traversePackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
        this.traverseInterfaceDeclaration(pmid);
    }

    public void
    traverseMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
        this.traverseNamedClassDeclaration(mcd);
    }

    public void
    traverseConstructorDeclarator(Java.ConstructorDeclarator cd) {
        if (cd.optionalConstructorInvocation != null) {
            cd.optionalConstructorInvocation.accept((Visitor.BlockStatementVisitor) this.cv);
        }
        this.traverseFunctionDeclarator(cd);
    }

    public void
    traverseInitializer(Java.Initializer i) {
        i.block.accept(this.cv);
        this.traverseAbstractTypeBodyDeclaration(i);
    }

    public void
    traverseMethodDeclarator(Java.MethodDeclarator md) {
        this.traverseFunctionDeclarator(md);
    }

    public void
    traverseFieldDeclaration(Java.FieldDeclaration fd) {
        fd.type.accept((Visitor.TypeVisitor) this.cv);
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            Java.ArrayInitializerOrRvalue optionalInitializer = fd.variableDeclarators[i].optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(fd);
    }

    public void
    traverseLabeledStatement(Java.LabeledStatement ls) {
        ls.body.accept(this.cv);
        this.traverseBreakableStatement(ls);
    }

    public void
    traverseBlock(Java.Block b) {
        for (Iterator it = b.statements.iterator(); it.hasNext();) {
            ((Java.Statement) it.next()).accept(this.cv);
        }
        this.traverseStatement(b);
    }

    public void
    traverseExpressionStatement(Java.ExpressionStatement es) {
        es.rvalue.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseStatement(es);
    }

    public void
    traverseIfStatement(Java.IfStatement is) {
        is.condition.accept((Visitor.RvalueVisitor) this.cv);
        is.thenStatement.accept(this.cv);
        if (is.optionalElseStatement != null) is.optionalElseStatement.accept(this.cv);
        this.traverseStatement(is);
    }

    public void
    traverseForStatement(Java.ForStatement fs) {
        if (fs.optionalInit != null) fs.optionalInit.accept(this.cv);
        if (fs.optionalCondition != null) fs.optionalCondition.accept((Visitor.RvalueVisitor) this.cv);
        if (fs.optionalUpdate != null) {
            for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                fs.optionalUpdate[i].accept((Visitor.RvalueVisitor) this.cv);
            }
        }
        fs.body.accept(this.cv);
        this.traverseContinuableStatement(fs);
    }

    public void
    traverseWhileStatement(Java.WhileStatement ws) {
        ws.condition.accept((Visitor.RvalueVisitor) this.cv);
        ws.body.accept(this.cv);
        this.traverseContinuableStatement(ws);
    }

    public void
    traverseTryStatement(Java.TryStatement ts) {
        ts.body.accept(this.cv);
        for (Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            ((Java.CatchClause) it.next()).body.accept(this.cv);
        }
        if (ts.optionalFinally != null) ts.optionalFinally.accept(this.cv);
        this.traverseStatement(ts);
    }

    public void
    traverseSwitchStatement(Java.SwitchStatement ss) {
        ss.condition.accept((Visitor.RvalueVisitor) this.cv);
        for (Iterator it = ss.sbsgs.iterator(); it.hasNext();) {
            Java.SwitchStatement.SwitchBlockStatementGroup sbsg = (
                (Java.SwitchStatement.SwitchBlockStatementGroup) it.next()
            );
            for (Iterator it2 = sbsg.caseLabels.iterator(); it2.hasNext();) {
                ((Java.Rvalue) it2.next()).accept((Visitor.RvalueVisitor) this.cv);
            }
            for (Iterator it2 = sbsg.blockStatements.iterator(); it2.hasNext();) {
                ((Java.BlockStatement) it2.next()).accept(this.cv);
            }
            this.traverseLocated(sbsg);
        }
        this.traverseBreakableStatement(ss);
    }

    public void
    traverseSynchronizedStatement(Java.SynchronizedStatement ss) {
        ss.expression.accept((Visitor.RvalueVisitor) this.cv);
        ss.body.accept(this.cv);
        this.traverseStatement(ss);
    }

    public void
    traverseDoStatement(Java.DoStatement ds) {
        ds.body.accept(this.cv);
        ds.condition.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseContinuableStatement(ds);
    }

    public void
    traverseLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
        lvds.type.accept((Visitor.TypeVisitor) this.cv);
        for (int i = 0; i < lvds.variableDeclarators.length; ++i) {
            Java.ArrayInitializerOrRvalue optionalInitializer = lvds.variableDeclarators[i].optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(lvds);
    }

    public void
    traverseReturnStatement(Java.ReturnStatement rs) {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseStatement(rs);
    }

    public void
    traverseThrowStatement(Java.ThrowStatement ts) {
        ts.expression.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseStatement(ts);
    }

    public void
    traverseBreakStatement(Java.BreakStatement bs) {
        this.traverseStatement(bs);
    }

    public void
    traverseContinueStatement(Java.ContinueStatement cs) {
        this.traverseStatement(cs);
    }

    protected void
    traverseAssertStatement(Java.AssertStatement as) {
        as.expression1.accept((Visitor.RvalueVisitor) this.cv);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseStatement(as);
    }

    public void
    traverseEmptyStatement(Java.EmptyStatement es) {
        this.traverseStatement(es);
    }

    public void
    traverseLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
        lcds.lcd.accept(this.cv);
        this.traverseStatement(lcds);
    }

    public void
    traversePackage(Java.Package p) {
        this.traverseAtom(p);
    }

    public void
    traverseArrayLength(Java.ArrayLength al) {
        al.lhs.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(al);
    }

    public void
    traverseAssignment(Java.Assignment a) {
        a.lhs.accept((Visitor.LvalueVisitor) this.cv);
        a.rhs.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(a);
    }

    public void
    traverseUnaryOperation(Java.UnaryOperation uo) {
        uo.operand.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseBooleanRvalue(uo);
    }

    public void
    traverseBinaryOperation(Java.BinaryOperation bo) {
        bo.lhs.accept((Visitor.RvalueVisitor) this.cv);
        bo.rhs.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseBooleanRvalue(bo);
    }

    public void
    traverseCast(Java.Cast c) {
        c.targetType.accept((Visitor.TypeVisitor) this.cv);
        c.value.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(c);
    }

    public void
    traverseClassLiteral(Java.ClassLiteral cl) {
        cl.type.accept((Visitor.TypeVisitor) this.cv);
        this.traverseRvalue(cl);
    }

    public void
    traverseConditionalExpression(Java.ConditionalExpression ce) {
        ce.lhs.accept((Visitor.RvalueVisitor) this.cv);
        ce.mhs.accept((Visitor.RvalueVisitor) this.cv);
        ce.rhs.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(ce);
    }

    public void
    traverseCrement(Java.Crement c) {
        c.operand.accept((Visitor.LvalueVisitor) this.cv);
        this.traverseRvalue(c);
    }

    public void
    traverseInstanceof(Java.Instanceof io) {
        io.lhs.accept((Visitor.RvalueVisitor) this.cv);
        io.rhs.accept((Visitor.TypeVisitor) this.cv);
        this.traverseRvalue(io);
    }

    public void
    traverseMethodInvocation(Java.MethodInvocation mi) {
        if (mi.optionalTarget != null) mi.optionalTarget.accept(this.cv);
        this.traverseInvocation(mi);
    }

    public void
    traverseSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
        this.traverseInvocation(smi);
    }

    public void
    traverseLiteral(Java.Literal l) {
        this.traverseRvalue(l);
    }

    public void traverseIntegerLiteral(Java.IntegerLiteral il)              { this.traverseLiteral(il); }
    public void traverseFloatingPointLiteral(Java.FloatingPointLiteral fpl) { this.traverseLiteral(fpl); }
    public void traverseBooleanLiteral(Java.BooleanLiteral bl)              { this.traverseLiteral(bl); }
    public void traverseCharacterLiteral(Java.CharacterLiteral cl)          { this.traverseLiteral(cl); }
    public void traverseStringLiteral(Java.StringLiteral sl)                { this.traverseLiteral(sl); }
    public void traverseNullLiteral(Java.NullLiteral nl)                    { this.traverseLiteral(nl); }

    public void
    traverseNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
        if (naci.optionalQualification != null) naci.optionalQualification.accept((Visitor.RvalueVisitor) this.cv);
        naci.anonymousClassDeclaration.accept(this.cv);
        for (int i = 0; i < naci.arguments.length; ++i) naci.arguments[i].accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(naci);
    }

    public void
    traverseNewArray(Java.NewArray na) {
        na.type.accept((Visitor.TypeVisitor) this.cv);
        for (int i =  0; i < na.dimExprs.length; ++i) na.dimExprs[i].accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(na);
    }

    public void
    traverseNewInitializedArray(Java.NewInitializedArray nia) {
        nia.arrayType.accept((Visitor.TypeVisitor) this.cv);
        this.traverseArrayInitializerOrRvalue(nia.arrayInitializer);
    }

    public void
    traverseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) {
        if (aiorv instanceof Java.Rvalue) {
            ((Java.Atom) aiorv).accept(this.cv);
        } else
        if (aiorv instanceof Java.ArrayInitializer) {
            Java.ArrayInitializerOrRvalue[] values = ((Java.ArrayInitializer) aiorv).values;
            for (int i = 0; i < values.length; ++i) this.traverseArrayInitializerOrRvalue(values[i]);
        } else
        {
            throw new JaninoRuntimeException(
                "Unexpected array initializer or rvalue class "
                + aiorv.getClass().getName()
            );
        }
    }
    public void
    traverseNewClassInstance(Java.NewClassInstance nci) {
        if (nci.optionalQualification != null) nci.optionalQualification.accept((Visitor.RvalueVisitor) this.cv);
        if (nci.type != null) nci.type.accept((Visitor.TypeVisitor) this.cv);
        for (int i = 0; i < nci.arguments.length; ++i) nci.arguments[i].accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(nci);
    }

    public void
    traverseParameterAccess(Java.ParameterAccess pa) { this.traverseRvalue(pa); }

    public void
    traverseQualifiedThisReference(Java.QualifiedThisReference qtr) {
        qtr.qualification.accept((Visitor.TypeVisitor) this.cv);
        this.traverseRvalue(qtr);
    }

    public void
    traverseThisReference(Java.ThisReference tr) { this.traverseRvalue(tr); }

    public void
    traverseArrayType(Java.ArrayType at) {
        at.componentType.accept((Visitor.TypeVisitor) this.cv);
        this.traverseType(at);
    }

    public void
    traverseBasicType(Java.BasicType bt) { this.traverseType(bt); }

    public void
    traverseReferenceType(Java.ReferenceType rt) { this.traverseType(rt); }

    public void
    traverseRvalueMemberType(Java.RvalueMemberType rmt) {
        rmt.rvalue.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseType(rmt);
    }

    public void
    traverseSimpleType(Java.SimpleType st) { this.traverseType(st); }

    public void
    traverseAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
        this.traverseConstructorInvocation(aci);
    }

    public void
    traverseSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
        if (sci.optionalQualification != null) sci.optionalQualification.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseConstructorInvocation(sci);
    }

    public void
    traverseAmbiguousName(Java.AmbiguousName an) { this.traverseLvalue(an); }

    public void
    traverseArrayAccessExpression(Java.ArrayAccessExpression aae) {
        aae.lhs.accept((Visitor.RvalueVisitor) this.cv);
        ((Java.Atom) aae.index).accept(this.cv);
        this.traverseLvalue(aae);
    }

    public void
    traverseFieldAccess(Java.FieldAccess fa) {
        fa.lhs.accept(this.cv);
        this.traverseLvalue(fa);
    }

    public void
    traverseFieldAccessExpression(Java.FieldAccessExpression fae) {
        fae.lhs.accept(this.cv);
        this.traverseLvalue(fae);
    }

    public void
    traverseSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {
        if (scfae.optionalQualification != null) scfae.optionalQualification.accept((Visitor.TypeVisitor) this.cv);
        this.traverseLvalue(scfae);
    }

    public void
    traverseLocalVariableAccess(Java.LocalVariableAccess lva) { this.traverseLvalue(lva); }

    public void
    traverseParenthesizedExpression(Java.ParenthesizedExpression pe) {
        pe.value.accept((Visitor.RvalueVisitor) this.cv);
        this.traverseLvalue(pe);
    }

    public void
    traverseElementValueArrayInitializer(ElementValueArrayInitializer evai) {
        for (int i = 0; i < evai.elementValues.length; i++) {
            evai.elementValues[i].accept(this.cv);
        }
        this.traverseElementValue(evai);
    }

    public void
    traverseElementValue(Java.ElementValue ev) {
        ;
    }

    public void
    traverseSingleElementAnnotation(SingleElementAnnotation sea) {
        sea.type.accept(this.cv);
        sea.elementValue.accept(this.cv);
        this.traverseAnnotation(sea);
    }

    public void
    traverseAnnotation(Java.Annotation a) {
        ;
    }

    public void
    traverseNormalAnnotation(Java.NormalAnnotation na) {
        na.type.accept(this.cv);
        for (int i = 0; i < na.elementValuePairs.length; i++) {
            na.elementValuePairs[i].elementValue.accept(this.cv);
        }
        this.traverseAnnotation(na);
    }

    public void
    traverseMarkerAnnotation(MarkerAnnotation ma) {
        ma.type.accept(this.cv);
        this.traverseAnnotation(ma);
    }

    public void
    traverseClassDeclaration(Java.ClassDeclaration cd) {
        for (Iterator it = cd.constructors.iterator(); it.hasNext();) {
            ((Java.ConstructorDeclarator) it.next()).accept(this.cv);
        }
        for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).accept(this.cv);
        }
        this.traverseAbstractTypeDeclaration(cd);
    }

    public void
    traverseAbstractTypeDeclaration(Java.AbstractTypeDeclaration atd) {
        for (Iterator it = atd.getMemberTypeDeclarations().iterator(); it.hasNext();) {
            ((Java.NamedTypeDeclaration) it.next()).accept(this.cv);
        }
        for (Iterator it = atd.getMethodDeclarations().iterator(); it.hasNext();) {
            this.traverseMethodDeclarator((Java.MethodDeclarator) it.next());
        }
    }

    public void
    traverseNamedClassDeclaration(Java.NamedClassDeclaration ncd) {
        for (int i = 0; i < ncd.implementedTypes.length; ++i) {
            ncd.implementedTypes[i].accept((Visitor.TypeVisitor) this.cv);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept((Visitor.TypeVisitor) this.cv);
        this.traverseClassDeclaration(ncd);
    }

    public void
    traverseInterfaceDeclaration(Java.InterfaceDeclaration id) {
        for (Iterator it = id.constantDeclarations.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).accept(this.cv);
        }
        for (int i = 0; i < id.extendedTypes.length; ++i) {
            id.extendedTypes[i].accept((Visitor.TypeVisitor) this.cv);
        }
        this.traverseAbstractTypeDeclaration(id);
    }

    public void
    traverseFunctionDeclarator(Java.FunctionDeclarator fd) {
        for (int i = 0; i < fd.formalParameters.length; ++i) {
            fd.formalParameters[i].type.accept((Visitor.TypeVisitor) this.cv);
        }
        if (fd.optionalStatements != null) {
            for (Iterator it = fd.optionalStatements.iterator(); it.hasNext();) {
                Java.BlockStatement bs = (Java.BlockStatement) it.next();
                bs.accept(this.cv);
            }
        }
    }

    public void
    traverseAbstractTypeBodyDeclaration(Java.AbstractTypeBodyDeclaration atbd) { this.traverseLocated(atbd); }

    public void
    traverseStatement(Java.Statement s) { this.traverseLocated(s); }

    public void
    traverseBreakableStatement(Java.BreakableStatement bs) { this.traverseStatement(bs); }

    public void
    traverseContinuableStatement(Java.ContinuableStatement cs) { this.traverseBreakableStatement(cs); }

    public void
    traverseRvalue(Java.Rvalue rv) { this.traverseAtom(rv); }

    public void
    traverseBooleanRvalue(Java.BooleanRvalue brv) { this.traverseRvalue(brv); }

    public void
    traverseInvocation(Java.Invocation i) {
        for (int j = 0; j < i.arguments.length; ++j) i.arguments[j].accept((Visitor.RvalueVisitor) this.cv);
        this.traverseRvalue(i);
    }

    public void
    traverseConstructorInvocation(Java.ConstructorInvocation ci) {
        for (int i = 0; i < ci.arguments.length; ++i) ci.arguments[i].accept((Visitor.RvalueVisitor) this.cv);
        this.traverseAtom(ci);
    }

    public void
    traverseLvalue(Java.Lvalue lv) { this.traverseRvalue(lv); }

    public void
    traverseType(Java.Type t) { this.traverseAtom(t); }

    public void
    traverseAtom(Java.Atom a) { this.traverseLocated(a); }

    public void
    traverseLocated(Java.Located l) {
        ;
    }
}
