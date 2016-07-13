
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
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Visitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;

/**
 * This class traverses the subnodes of an AST. Derived classes override individual "{@code traverse*()}" methods to
 * process specific nodes.
 * <p>
 *   Example:
 * </p>
 * <pre>
 *     LocalClassDeclaration lcd = ...;
 *
 *     new Traverser() {
 *
 *         int n = 0;
 *
 *         protected void
 *         traverseMethodDeclarator(Java.MethodDeclarator md) {
 *             ++this.n;
 *             super.traverseMethodDeclarator(md);
 *         }
 *     }.visitTypeDeclaration(lcd);
 * </pre>
 *
 * @param <EX> The exception that the "{@code traverse*()}" and "{@code visit*()}" methods may throw
 * @see #visitAnnotation(Annotation)
 * @see #visitAtom(org.codehaus.janino.Java.Atom)
 * @see #visitBlockStatement(org.codehaus.janino.Java.BlockStatement)
 * @see #visitElementValue(org.codehaus.janino.Java.ElementValue)
 * @see #visitImportDeclaration(org.codehaus.janino.Java.CompilationUnit.ImportDeclaration)
 * @see #visitTypeBodyDeclaration(org.codehaus.janino.Java.TypeBodyDeclaration)
 * @see #visitTypeDeclaration(org.codehaus.janino.Java.TypeDeclaration)
 * @see #traverseCompilationUnit(org.codehaus.janino.Java.CompilationUnit)
 */
public
class Traverser<EX extends Throwable> {

    private final Visitor.ImportVisitor<Void, EX>
    importTraverser = new Visitor.ImportVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:4
        @Override @Nullable public Void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX          { Traverser.this.traverseSingleTypeImportDeclaration(stid); return null; }
        @Override @Nullable public Void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX     { Traverser.this.traverseTypeImportOnDemandDeclaration(tiodd); return null; }
        @Override @Nullable public Void visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) throws EX      { Traverser.this.traverseSingleStaticImportDeclaration(ssid); return null; }
        @Override @Nullable public Void visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX { Traverser.this.traverseStaticImportOnDemandDeclaration(siodd); return null; }
    };

    private final Visitor.TypeDeclarationVisitor<Void, EX>
    typeDeclarationTraverser = new Visitor.TypeDeclarationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:11
        @Override @Nullable public Void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX                             { Traverser.this.traverseAnonymousClassDeclaration(acd); return null; }
        @Override @Nullable public Void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX                                     { Traverser.this.traverseLocalClassDeclaration(lcd); return null; }
        @Override @Nullable public Void visitPackageMemberClassDeclaration(Java.AbstractPackageMemberClassDeclaration apmcd) throws EX           { Traverser.this.traversePackageMemberClassDeclaration(apmcd); return null; }
        @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX            { Traverser.this.traversePackageMemberInterfaceDeclaration(pmid); return null; }
        @Override @Nullable public Void visitEnumConstant(Java.EnumConstant ec) throws EX                                                        { Traverser.this.traverseEnumConstant(ec); return null; }
        @Override @Nullable public Void visitPackageMemberEnumDeclaration(Java.PackageMemberEnumDeclaration pmed) throws EX                      { Traverser.this.traversePackageMemberEnumDeclaration(pmed); return null; }
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(Java.MemberAnnotationTypeDeclaration matd) throws EX                { Traverser.this.traverseMemberAnnotationTypeDeclaration(matd); return null; }
        @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(Java.PackageMemberAnnotationTypeDeclaration pmatd) throws EX { Traverser.this.traversePackageMemberAnnotationTypeDeclaration(pmatd); return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX                                     { Traverser.this.traverseMemberEnumDeclaration(med); return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX                           { Traverser.this.traverseMemberInterfaceDeclaration(mid); return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX                                   { Traverser.this.traverseMemberClassDeclaration(mcd); return null; }
    };

    private final Visitor.RvalueVisitor<Void, EX>
    rvalueTraverser = new Visitor.RvalueVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitLvalue(Lvalue lv) throws EX {
            lv.accept(new Visitor.LvalueVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:7
                @Override @Nullable public Void visitAmbiguousName(Java.AmbiguousName an)                                        throws EX { Traverser.this.traverseAmbiguousName(an);                      return null; }
                @Override @Nullable public Void visitArrayAccessExpression(Java.ArrayAccessExpression aae)                       throws EX { Traverser.this.traverseArrayAccessExpression(aae);             return null; }
                @Override @Nullable public Void visitFieldAccess(Java.FieldAccess fa)                                            throws EX { Traverser.this.traverseFieldAccess(fa);                        return null; }
                @Override @Nullable public Void visitFieldAccessExpression(Java.FieldAccessExpression fae)                       throws EX { Traverser.this.traverseFieldAccessExpression(fae);             return null; }
                @Override @Nullable public Void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX { Traverser.this.traverseSuperclassFieldAccessExpression(scfae); return null; }
                @Override @Nullable public Void visitLocalVariableAccess(Java.LocalVariableAccess lva)                           throws EX { Traverser.this.traverseLocalVariableAccess(lva);               return null; }
                @Override @Nullable public Void visitParenthesizedExpression(Java.ParenthesizedExpression pe)                    throws EX { Traverser.this.traverseParenthesizedExpression(pe);            return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:25
        @Override @Nullable public Void visitArrayLength(Java.ArrayLength al)                                throws EX { Traverser.this.traverseArrayLength(al);                        return null; }
        @Override @Nullable public Void visitAssignment(Java.Assignment a)                                   throws EX { Traverser.this.traverseAssignment(a);                          return null; }
        @Override @Nullable public Void visitUnaryOperation(Java.UnaryOperation uo)                          throws EX { Traverser.this.traverseUnaryOperation(uo);                     return null; }
        @Override @Nullable public Void visitBinaryOperation(Java.BinaryOperation bo)                        throws EX { Traverser.this.traverseBinaryOperation(bo);                    return null; }
        @Override @Nullable public Void visitCast(Java.Cast c)                                               throws EX { Traverser.this.traverseCast(c);                                return null; }
        @Override @Nullable public Void visitClassLiteral(Java.ClassLiteral cl)                              throws EX { Traverser.this.traverseClassLiteral(cl);                       return null; }
        @Override @Nullable public Void visitConditionalExpression(Java.ConditionalExpression ce)            throws EX { Traverser.this.traverseConditionalExpression(ce);              return null; }
        @Override @Nullable public Void visitCrement(Java.Crement c)                                         throws EX { Traverser.this.traverseCrement(c);                             return null; }
        @Override @Nullable public Void visitInstanceof(Java.Instanceof io)                                  throws EX { Traverser.this.traverseInstanceof(io);                         return null; }
        @Override @Nullable public Void visitMethodInvocation(Java.MethodInvocation mi)                      throws EX { Traverser.this.traverseMethodInvocation(mi);                   return null; }
        @Override @Nullable public Void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX { Traverser.this.traverseSuperclassMethodInvocation(smi);        return null; }
        @Override @Nullable public Void visitIntegerLiteral(Java.IntegerLiteral il)                          throws EX { Traverser.this.traverseIntegerLiteral(il);                     return null; }
        @Override @Nullable public Void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl)             throws EX { Traverser.this.traverseFloatingPointLiteral(fpl);              return null; }
        @Override @Nullable public Void visitBooleanLiteral(Java.BooleanLiteral bl)                          throws EX { Traverser.this.traverseBooleanLiteral(bl);                     return null; }
        @Override @Nullable public Void visitCharacterLiteral(Java.CharacterLiteral cl)                      throws EX { Traverser.this.traverseCharacterLiteral(cl);                   return null; }
        @Override @Nullable public Void visitStringLiteral(Java.StringLiteral sl)                            throws EX { Traverser.this.traverseStringLiteral(sl);                      return null; }
        @Override @Nullable public Void visitNullLiteral(Java.NullLiteral nl)                                throws EX { Traverser.this.traverseNullLiteral(nl);                        return null; }
        @Override @Nullable public Void visitSimpleConstant(Java.SimpleConstant sl)                          throws EX { Traverser.this.traverseSimpleLiteral(sl);                      return null; }
        @Override @Nullable public Void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci)  throws EX { Traverser.this.traverseNewAnonymousClassInstance(naci);        return null; }
        @Override @Nullable public Void visitNewArray(Java.NewArray na)                                      throws EX { Traverser.this.traverseNewArray(na);                           return null; }
        @Override @Nullable public Void visitNewInitializedArray(Java.NewInitializedArray nia)               throws EX { Traverser.this.traverseNewInitializedArray(nia);               return null; }
        @Override @Nullable public Void visitNewClassInstance(Java.NewClassInstance nci)                     throws EX { Traverser.this.traverseNewClassInstance(nci);                  return null; }
        @Override @Nullable public Void visitParameterAccess(Java.ParameterAccess pa)                        throws EX { Traverser.this.traverseParameterAccess(pa);                    return null; }
        @Override @Nullable public Void visitQualifiedThisReference(Java.QualifiedThisReference qtr)         throws EX { Traverser.this.traverseQualifiedThisReference(qtr);            return null; }
        @Override @Nullable public Void visitThisReference(Java.ThisReference tr)                            throws EX { Traverser.this.traverseThisReference(tr);                      return null; }
    };

    private final Visitor.TypeBodyDeclarationVisitor<Void, EX>
    typeBodyDeclarationTraverser = new Visitor.TypeBodyDeclarationVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitFunctionDeclarator(Java.FunctionDeclarator fd) throws EX {
            fd.accept(new Visitor.FunctionDeclaratorVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX { Traverser.this.traverseConstructorDeclarator(cd); return null; }
                @Override @Nullable public Void visitMethodDeclarator(Java.MethodDeclarator md)           throws EX { Traverser.this.traverseMethodDeclarator(md);      return null; }
            });
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override @Nullable public Void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX { Traverser.this.traverseMemberInterfaceDeclaration(mid); return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd)         throws EX { Traverser.this.traverseMemberClassDeclaration(mcd);     return null; }
        @Override @Nullable public Void visitMemberEnumDeclaration(Java.MemberEnumDeclaration med)           throws EX { Traverser.this.traverseMemberEnumDeclaration(med);      return null; }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitInitializer(Java.Initializer i)            throws EX { Traverser.this.traverseInitializer(i);       return null; }
        @Override @Nullable public Void visitFieldDeclaration(Java.FieldDeclaration fd) throws EX { Traverser.this.traverseFieldDeclaration(fd); return null; }
    };

    private final Visitor.BlockStatementVisitor<Void, EX>
    blockStatementTraverser = new BlockStatementVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:23
        @Override @Nullable public Void visitInitializer(Java.Initializer i) throws EX                                                { Traverser.this.traverseInitializer(i); return null; }
        @Override @Nullable public Void visitFieldDeclaration(Java.FieldDeclaration fd) throws EX                                     { Traverser.this.traverseFieldDeclaration(fd); return null; }
        @Override @Nullable public Void visitLabeledStatement(Java.LabeledStatement ls) throws EX                                     { Traverser.this.traverseLabeledStatement(ls); return null; }
        @Override @Nullable public Void visitBlock(Java.Block b) throws EX                                                            { Traverser.this.traverseBlock(b); return null; }
        @Override @Nullable public Void visitExpressionStatement(Java.ExpressionStatement es) throws EX                               { Traverser.this.traverseExpressionStatement(es); return null; }
        @Override @Nullable public Void visitIfStatement(Java.IfStatement is) throws EX                                               { Traverser.this.traverseIfStatement(is); return null; }
        @Override @Nullable public Void visitForStatement(Java.ForStatement fs) throws EX                                             { Traverser.this.traverseForStatement(fs); return null; }
        @Override @Nullable public Void visitForEachStatement(Java.ForEachStatement fes) throws EX                                    { Traverser.this.traverseForEachStatement(fes); return null; }
        @Override @Nullable public Void visitWhileStatement(Java.WhileStatement ws) throws EX                                         { Traverser.this.traverseWhileStatement(ws); return null; }
        @Override @Nullable public Void visitTryStatement(Java.TryStatement ts) throws EX                                             { Traverser.this.traverseTryStatement(ts); return null; }
        @Override @Nullable public Void visitSwitchStatement(Java.SwitchStatement ss) throws EX                                       { Traverser.this.traverseSwitchStatement(ss); return null; }
        @Override @Nullable public Void visitSynchronizedStatement(Java.SynchronizedStatement ss) throws EX                           { Traverser.this.traverseSynchronizedStatement(ss); return null; }
        @Override @Nullable public Void visitDoStatement(Java.DoStatement ds) throws EX                                               { Traverser.this.traverseDoStatement(ds); return null; }
        @Override @Nullable public Void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX { Traverser.this.traverseLocalVariableDeclarationStatement(lvds); return null; }
        @Override @Nullable public Void visitReturnStatement(Java.ReturnStatement rs) throws EX                                       { Traverser.this.traverseReturnStatement(rs); return null; }
        @Override @Nullable public Void visitThrowStatement(Java.ThrowStatement ts) throws EX                                         { Traverser.this.traverseThrowStatement(ts); return null; }
        @Override @Nullable public Void visitBreakStatement(Java.BreakStatement bs) throws EX                                         { Traverser.this.traverseBreakStatement(bs); return null; }
        @Override @Nullable public Void visitContinueStatement(Java.ContinueStatement cs) throws EX                                   { Traverser.this.traverseContinueStatement(cs); return null; }
        @Override @Nullable public Void visitAssertStatement(Java.AssertStatement as) throws EX                                       { Traverser.this.traverseAssertStatement(as); return null; }
        @Override @Nullable public Void visitEmptyStatement(Java.EmptyStatement es) throws EX                                         { Traverser.this.traverseEmptyStatement(es); return null; }
        @Override @Nullable public Void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX       { Traverser.this.traverseLocalClassDeclarationStatement(lcds); return null; }
        @Override @Nullable public Void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX        { Traverser.this.traverseAlternateConstructorInvocation(aci); return null; }
        @Override @Nullable public Void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX                { Traverser.this.traverseSuperConstructorInvocation(sci); return null; }
    };

    private final Visitor.AtomVisitor<Void, EX>
    atomTraverser = new Visitor.AtomVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitRvalue(Java.Rvalue rv) throws EX {
            rv.accept(Traverser.this.rvalueTraverser);
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Java.Package p) throws EX {
            Traverser.this.traversePackage(p);
            return null;
        }

        @Override @Nullable public Void
        visitType(Type t) throws EX {
            t.accept(new Visitor.TypeVisitor<Void, EX>() {

                // SUPPRESS CHECKSTYLE LineLength:5
                @Override @Nullable public Void visitArrayType(Java.ArrayType at)                throws EX { Traverser.this.traverseArrayType(at);         return null; }
                @Override @Nullable public Void visitPrimitiveType(Java.PrimitiveType bt)        throws EX { Traverser.this.traversePrimitiveType(bt);     return null; }
                @Override @Nullable public Void visitReferenceType(Java.ReferenceType rt)        throws EX { Traverser.this.traverseReferenceType(rt);     return null; }
                @Override @Nullable public Void visitRvalueMemberType(Java.RvalueMemberType rmt) throws EX { Traverser.this.traverseRvalueMemberType(rmt); return null; }
                @Override @Nullable public Void visitSimpleType(Java.SimpleType st)              throws EX { Traverser.this.traverseSimpleType(st);        return null; }
            });
            return null;
        }
    };

    private final ElementValueVisitor<Void, EX>
    elementValueTraverser = new ElementValueVisitor<Void, EX>() {

        @Override @Nullable public Void
        visitRvalue(Java.Rvalue rv) throws EX {
            rv.accept(Traverser.this.rvalueTraverser);
            return null;
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override @Nullable public Void visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX { Traverser.this.traverseElementValueArrayInitializer(evai); return null; }
        @Override @Nullable public Void visitAnnotation(Java.Annotation a)                                        throws EX { Traverser.this.traverseAnnotation(a);                      return null; }
    };

    private final AnnotationVisitor<Void, EX>
    annotationTraverser = new AnnotationVisitor<Void, EX>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override @Nullable public Void visitMarkerAnnotation(Java.MarkerAnnotation ma)                throws EX { Traverser.this.traverseMarkerAnnotation(ma);         return null; }
        @Override @Nullable public Void visitNormalAnnotation(Java.NormalAnnotation na)                throws EX { Traverser.this.traverseNormalAnnotation(na);         return null; }
        @Override @Nullable public Void visitSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX { Traverser.this.traverseSingleElementAnnotation(sea); return null; }
    };

    /** @see Traverser */
    public void
    visitImportDeclaration(Java.CompilationUnit.ImportDeclaration id) throws EX {
        id.accept(Traverser.this.importTraverser);
    }

    /** @see Traverser */
    public void
    visitTypeDeclaration(Java.TypeDeclaration td) throws EX {
        td.accept(Traverser.this.typeDeclarationTraverser);
    }

    /** @see Traverser */
    public void
    visitTypeBodyDeclaration(Java.TypeBodyDeclaration tbd) throws EX {
        tbd.accept(Traverser.this.typeBodyDeclarationTraverser);
    }

    /** @see Traverser */
    public void
    visitBlockStatement(Java.BlockStatement bs) throws EX {
        bs.accept(Traverser.this.blockStatementTraverser);
    }

    /** @see Traverser */
    public void
    visitAtom(Java.Atom a) throws EX {
        a.accept(Traverser.this.atomTraverser);
    }

    /** @see Traverser */
    public void
    visitElementValue(Java.ElementValue ev) throws EX {
        ev.accept(Traverser.this.elementValueTraverser);
    }

    /** @see Traverser */
    public void
    visitAnnotation(Annotation a) throws EX {
        a.accept(Traverser.this.annotationTraverser);
    }

    // These may be overridden by derived classes.

    /** @see Traverser */
    public void
    traverseCompilationUnit(Java.CompilationUnit cu) throws EX {

        // The optionalPackageDeclaration is considered an integral part of
        // the compilation unit and is thus not traversed.

        for (Java.CompilationUnit.ImportDeclaration id : cu.importDeclarations) {
            id.accept(this.importTraverser);
        }

        for (Java.PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            pmtd.accept(this.typeDeclarationTraverser);
        }
    }

    /** @see Traverser */
    protected void
    traverseSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /** @see Traverser */
    protected void
    traverseTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws EX {
        this.traverseImportDeclaration(tiodd);
    }

    /** @see Traverser */
    protected void
    traverseSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration stid) throws EX {
        this.traverseImportDeclaration(stid);
    }

    /** @see Traverser */
    protected void
    traverseStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) throws EX {
        this.traverseImportDeclaration(siodd);
    }

    /** @see Traverser */
    protected void
    traverseImportDeclaration(Java.CompilationUnit.ImportDeclaration id) throws EX { this.traverseLocated(id); }

    /** @see Traverser */
    protected void
    traverseAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) throws EX {
        acd.baseType.accept(this.atomTraverser);
        this.traverseClassDeclaration(acd);
    }

    /** @see Traverser */
    protected void
    traverseLocalClassDeclaration(Java.LocalClassDeclaration lcd) throws EX { this.traverseNamedClassDeclaration(lcd); }

    /** @see Traverser */
    protected void
    traversePackageMemberClassDeclaration(Java.AbstractPackageMemberClassDeclaration pmcd) throws EX {
        this.traverseNamedClassDeclaration(pmcd);
    }

    /** @see Traverser */
    protected void
    traverseMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) throws EX {
        this.traverseInterfaceDeclaration(mid);
    }

    /** @see Traverser */
    protected void
    traversePackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) throws EX {
        this.traverseInterfaceDeclaration(pmid);
    }

    /** @see Traverser */
    protected void
    traverseMemberClassDeclaration(Java.MemberClassDeclaration mcd) throws EX {
        this.traverseNamedClassDeclaration(mcd);
    }

    /** @see Traverser */
    protected void
    traverseConstructorDeclarator(Java.ConstructorDeclarator cd) throws EX {
        if (cd.optionalConstructorInvocation != null) {
            cd.optionalConstructorInvocation.accept(this.blockStatementTraverser);
        }
        this.traverseFunctionDeclarator(cd);
    }

    /** @see Traverser */
    protected void
    traverseInitializer(Java.Initializer i) throws EX {
        i.block.accept(this.blockStatementTraverser);
        this.traverseAbstractTypeBodyDeclaration(i);
    }

    /** @see Traverser */
    protected void
    traverseMethodDeclarator(Java.MethodDeclarator md) throws EX { this.traverseFunctionDeclarator(md); }

    /** @see Traverser */
    protected void
    traverseFieldDeclaration(Java.FieldDeclaration fd) throws EX {
        fd.type.accept(this.atomTraverser);
        for (Java.VariableDeclarator vd : fd.variableDeclarators) {
            Java.ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(fd);
    }

    /** @see Traverser */
    protected void
    traverseLabeledStatement(Java.LabeledStatement ls) throws EX {
        ls.body.accept(this.blockStatementTraverser);
        this.traverseBreakableStatement(ls);
    }

    /** @see Traverser */
    protected void
    traverseBlock(Java.Block b) throws EX {
        for (Java.BlockStatement bs : b.statements) bs.accept(this.blockStatementTraverser);
        this.traverseStatement(b);
    }

    /** @see Traverser */
    protected void
    traverseExpressionStatement(Java.ExpressionStatement es) throws EX {
        es.rvalue.accept(this.rvalueTraverser);
        this.traverseStatement(es);
    }

    /** @see Traverser */
    protected void
    traverseIfStatement(Java.IfStatement is) throws EX {
        is.condition.accept(this.rvalueTraverser);
        is.thenStatement.accept(this.blockStatementTraverser);
        if (is.optionalElseStatement != null) is.optionalElseStatement.accept(this.blockStatementTraverser);
        this.traverseStatement(is);
    }

    /** @see Traverser */
    protected void
    traverseForStatement(Java.ForStatement fs) throws EX {
        if (fs.optionalInit != null) fs.optionalInit.accept(this.blockStatementTraverser);
        if (fs.optionalCondition != null) fs.optionalCondition.accept(this.rvalueTraverser);
        if (fs.optionalUpdate != null) {
            for (Java.Rvalue rv : fs.optionalUpdate) rv.accept(this.rvalueTraverser);
        }
        fs.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fs);
    }

    /** @see Traverser */
    protected void
    traverseForEachStatement(Java.ForEachStatement fes) throws EX {
        this.traverseFormalParameter(fes.currentElement);
        fes.expression.accept(this.rvalueTraverser);
        fes.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(fes);
    }

    /** @see Traverser */
    protected void
    traverseWhileStatement(Java.WhileStatement ws) throws EX {
        ws.condition.accept(this.rvalueTraverser);
        ws.body.accept(this.blockStatementTraverser);
        this.traverseContinuableStatement(ws);
    }

    /** @see Traverser */
    protected void
    traverseTryStatement(Java.TryStatement ts) throws EX {
        ts.body.accept(this.blockStatementTraverser);
        for (Java.CatchClause cc : ts.catchClauses) cc.body.accept(this.blockStatementTraverser);
        if (ts.optionalFinally != null) ts.optionalFinally.accept(this.blockStatementTraverser);
        this.traverseStatement(ts);
    }

    /** @see Traverser */
    protected void
    traverseSwitchStatement(Java.SwitchStatement ss) throws EX {
        ss.condition.accept(this.rvalueTraverser);
        for (Java.SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (Java.Rvalue cl : sbsg.caseLabels) cl.accept(this.rvalueTraverser);
            for (Java.BlockStatement bs : sbsg.blockStatements) bs.accept(this.blockStatementTraverser);
            this.traverseLocated(sbsg);
        }
        this.traverseBreakableStatement(ss);
    }

    /** @see Traverser */
    protected void
    traverseSynchronizedStatement(Java.SynchronizedStatement ss) throws EX {
        ss.expression.accept(this.rvalueTraverser);
        ss.body.accept(this.blockStatementTraverser);
        this.traverseStatement(ss);
    }

    /** @see Traverser */
    protected void
    traverseDoStatement(Java.DoStatement ds) throws EX {
        ds.body.accept(this.blockStatementTraverser);
        ds.condition.accept(this.rvalueTraverser);
        this.traverseContinuableStatement(ds);
    }

    /** @see Traverser */
    protected void
    traverseLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) throws EX {
        lvds.type.accept(this.atomTraverser);
        for (Java.VariableDeclarator vd : lvds.variableDeclarators) {
            Java.ArrayInitializerOrRvalue optionalInitializer = vd.optionalInitializer;
            if (optionalInitializer != null) this.traverseArrayInitializerOrRvalue(optionalInitializer);
        }
        this.traverseStatement(lvds);
    }

    /** @see Traverser */
    protected void
    traverseReturnStatement(Java.ReturnStatement rs) throws EX {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept(this.rvalueTraverser);
        this.traverseStatement(rs);
    }

    /** @see Traverser */
    protected void
    traverseThrowStatement(Java.ThrowStatement ts) throws EX {
        ts.expression.accept(this.rvalueTraverser);
        this.traverseStatement(ts);
    }

    /** @see Traverser */
    protected void
    traverseBreakStatement(Java.BreakStatement bs) throws EX { this.traverseStatement(bs); }

    /** @see Traverser */
    protected void
    traverseContinueStatement(Java.ContinueStatement cs) throws EX { this.traverseStatement(cs); }

    /** @see Traverser */
    protected void
    traverseAssertStatement(Java.AssertStatement as) throws EX {
        as.expression1.accept(this.rvalueTraverser);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept(this.rvalueTraverser);
        this.traverseStatement(as);
    }

    /** @see Traverser */
    protected void
    traverseEmptyStatement(Java.EmptyStatement es) throws EX { this.traverseStatement(es); }

    /** @see Traverser */
    protected void
    traverseLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) throws EX {
        lcds.lcd.accept(this.typeDeclarationTraverser);
        this.traverseStatement(lcds);
    }

    /** @see Traverser */
    protected void
    traversePackage(Java.Package p) throws EX { this.traverseAtom(p); }

    /** @see Traverser */
    protected void
    traverseArrayLength(Java.ArrayLength al) throws EX {
        al.lhs.accept(this.rvalueTraverser);
        this.traverseRvalue(al);
    }

    /** @see Traverser */
    protected void
    traverseAssignment(Java.Assignment a) throws EX {
        a.lhs.accept(this.rvalueTraverser);
        a.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(a);
    }

    /** @see Traverser */
    protected void
    traverseUnaryOperation(Java.UnaryOperation uo) throws EX {
        uo.operand.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(uo);
    }

    /** @see Traverser */
    protected void
    traverseBinaryOperation(Java.BinaryOperation bo) throws EX {
        bo.lhs.accept(this.rvalueTraverser);
        bo.rhs.accept(this.rvalueTraverser);
        this.traverseBooleanRvalue(bo);
    }

    /** @see Traverser */
    protected void
    traverseCast(Java.Cast c) throws EX {
        c.targetType.accept(this.atomTraverser);
        c.value.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    /** @see Traverser */
    protected void
    traverseClassLiteral(Java.ClassLiteral cl) throws EX {
        cl.type.accept(this.atomTraverser);
        this.traverseRvalue(cl);
    }

    /** @see Traverser */
    protected void
    traverseConditionalExpression(Java.ConditionalExpression ce) throws EX {
        ce.lhs.accept(this.rvalueTraverser);
        ce.mhs.accept(this.rvalueTraverser);
        ce.rhs.accept(this.rvalueTraverser);
        this.traverseRvalue(ce);
    }

    /** @see Traverser */
    protected void
    traverseCrement(Java.Crement c) throws EX {
        c.operand.accept(this.rvalueTraverser);
        this.traverseRvalue(c);
    }

    /** @see Traverser */
    protected void
    traverseInstanceof(Java.Instanceof io) throws EX {
        io.lhs.accept(this.rvalueTraverser);
        io.rhs.accept(this.atomTraverser);
        this.traverseRvalue(io);
    }

    /** @see Traverser */
    protected void
    traverseMethodInvocation(Java.MethodInvocation mi) throws EX {
        if (mi.optionalTarget != null) mi.optionalTarget.accept(this.atomTraverser);
        this.traverseInvocation(mi);
    }

    /** @see Traverser */
    protected void
    traverseSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) throws EX { this.traverseInvocation(smi); }

    /** @see Traverser */
    protected void
    traverseLiteral(Java.Literal l) throws EX { this.traverseRvalue(l); }

    /** @see Traverser */
    protected void
    traverseIntegerLiteral(Java.IntegerLiteral il) throws EX { this.traverseLiteral(il); }

    /** @see Traverser */
    protected void
    traverseFloatingPointLiteral(Java.FloatingPointLiteral fpl) throws EX { this.traverseLiteral(fpl); }

    /** @see Traverser */
    protected void
    traverseBooleanLiteral(Java.BooleanLiteral bl) throws EX { this.traverseLiteral(bl); }

    /** @see Traverser */
    protected void
    traverseCharacterLiteral(Java.CharacterLiteral cl) throws EX { this.traverseLiteral(cl); }

    /** @see Traverser */
    protected void
    traverseStringLiteral(Java.StringLiteral sl) throws EX { this.traverseLiteral(sl); }

    /** @see Traverser */
    protected void
    traverseNullLiteral(Java.NullLiteral nl) throws EX { this.traverseLiteral(nl); }

    /** @see Traverser */
    protected void
    traverseSimpleLiteral(Java.SimpleConstant sl) throws EX { this.traverseRvalue(sl); }

    /** @see Traverser */
    protected void
    traverseNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) throws EX {
        if (naci.optionalQualification != null) {
            naci.optionalQualification.accept(this.rvalueTraverser);
        }
        naci.anonymousClassDeclaration.accept(this.typeDeclarationTraverser);
        for (Java.Rvalue argument : naci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(naci);
    }

    /** @see Traverser */
    protected void
    traverseNewArray(Java.NewArray na) throws EX {
        na.type.accept(this.atomTraverser);
        for (Java.Rvalue dimExpr : na.dimExprs) dimExpr.accept(this.rvalueTraverser);
        this.traverseRvalue(na);
    }

    /** @see Traverser */
    protected void
    traverseNewInitializedArray(Java.NewInitializedArray nia) throws EX {
        assert nia.arrayType != null;
        nia.arrayType.accept(this.atomTraverser);
        this.traverseArrayInitializerOrRvalue(nia.arrayInitializer);
    }

    /** @see Traverser */
    protected void
    traverseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) throws EX {
        if (aiorv instanceof Java.Rvalue) {
            ((Java.Rvalue) aiorv).accept(this.atomTraverser);
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
    protected void
    traverseNewClassInstance(Java.NewClassInstance nci) throws EX {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.accept(this.rvalueTraverser);
        }
        if (nci.type != null) nci.type.accept(this.atomTraverser);
        for (Java.Rvalue argument : nci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(nci);
    }

    /** @see Traverser */
    protected void
    traverseParameterAccess(Java.ParameterAccess pa) throws EX { this.traverseRvalue(pa); }

    /** @see Traverser */
    protected void
    traverseQualifiedThisReference(Java.QualifiedThisReference qtr) throws EX {
        qtr.qualification.accept(this.atomTraverser);
        this.traverseRvalue(qtr);
    }

    /** @see Traverser */
    protected void
    traverseThisReference(Java.ThisReference tr) throws EX { this.traverseRvalue(tr); }

    /** @see Traverser */
    protected void
    traverseArrayType(Java.ArrayType at) throws EX {
        at.componentType.accept(this.atomTraverser);
        this.traverseType(at);
    }

    /** @see Traverser */
    protected void
    traversePrimitiveType(Java.PrimitiveType bt) throws EX { this.traverseType(bt); }

    /** @see Traverser */
    protected void
    traverseReferenceType(Java.ReferenceType rt) throws EX { this.traverseType(rt); }

    /** @see Traverser */
    protected void
    traverseRvalueMemberType(Java.RvalueMemberType rmt) throws EX {
        rmt.rvalue.accept(this.rvalueTraverser);
        this.traverseType(rmt);
    }

    /** @see Traverser */
    protected void
    traverseSimpleType(Java.SimpleType st) throws EX { this.traverseType(st); }

    /** @see Traverser */
    protected void
    traverseAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) throws EX {
        this.traverseConstructorInvocation(aci);
    }

    /** @see Traverser */
    protected void
    traverseSuperConstructorInvocation(Java.SuperConstructorInvocation sci) throws EX {
        if (sci.optionalQualification != null) {
            sci.optionalQualification.accept(this.rvalueTraverser);
        }
        this.traverseConstructorInvocation(sci);
    }

    /** @see Traverser */
    protected void
    traverseAmbiguousName(Java.AmbiguousName an) throws EX { this.traverseLvalue(an); }

    /** @see Traverser */
    protected void
    traverseArrayAccessExpression(Java.ArrayAccessExpression aae) throws EX {
        aae.lhs.accept(this.rvalueTraverser);
        aae.index.accept(this.atomTraverser);
        this.traverseLvalue(aae);
    }

    /** @see Traverser */
    protected void
    traverseFieldAccess(Java.FieldAccess fa) throws EX {
        fa.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fa);
    }

    /** @see Traverser */
    protected void
    traverseFieldAccessExpression(Java.FieldAccessExpression fae) throws EX {
        fae.lhs.accept(this.atomTraverser);
        this.traverseLvalue(fae);
    }

    /** @see Traverser */
    protected void
    traverseSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) throws EX {
        if (scfae.optionalQualification != null) {
            scfae.optionalQualification.accept(this.atomTraverser);
        }
        this.traverseLvalue(scfae);
    }

    /** @see Traverser */
    protected void
    traverseLocalVariableAccess(Java.LocalVariableAccess lva) throws EX { this.traverseLvalue(lva); }

    /** @see Traverser */
    protected void
    traverseParenthesizedExpression(Java.ParenthesizedExpression pe) throws EX {
        pe.value.accept(this.rvalueTraverser);
        this.traverseLvalue(pe);
    }

    /** @see Traverser */
    protected void
    traverseElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) throws EX {
        for (Java.ElementValue elementValue : evai.elementValues) elementValue.accept(this.elementValueTraverser);
        this.traverseElementValue(evai);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    protected void
    traverseElementValue(Java.ElementValue ev) throws EX {}

    /** @see Traverser */
    protected void
    traverseSingleElementAnnotation(Java.SingleElementAnnotation sea) throws EX {
        sea.type.accept(this.atomTraverser);
        sea.elementValue.accept(this.elementValueTraverser);
        this.traverseAnnotation(sea);
    }

    /**
     * @throws EX
     * @see Traverser
     */
    protected void
    traverseAnnotation(Java.Annotation a) throws EX {}

    /** @see Traverser */
    protected void
    traverseNormalAnnotation(Java.NormalAnnotation na) throws EX {
        na.type.accept(this.atomTraverser);
        for (Java.ElementValuePair elementValuePair : na.elementValuePairs) {
            elementValuePair.elementValue.accept(this.elementValueTraverser);
        }
        this.traverseAnnotation(na);
    }

    /** @see Traverser */
    protected void
    traverseMarkerAnnotation(Java.MarkerAnnotation ma) throws EX {
        ma.type.accept(this.atomTraverser);
        this.traverseAnnotation(ma);
    }

    /** @see Traverser */
    protected void
    traverseClassDeclaration(Java.AbstractClassDeclaration cd) throws EX {
        for (Java.ConstructorDeclarator ctord : cd.constructors) ctord.accept(this.typeBodyDeclarationTraverser);
        for (Java.BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            vdoi.accept(this.blockStatementTraverser);
        }
        this.traverseAbstractTypeDeclaration(cd);
    }

    /** @see Traverser */
    protected void
    traverseAbstractTypeDeclaration(Java.AbstractTypeDeclaration atd) throws EX {
        for (Java.Annotation a : atd.getAnnotations()) this.traverseAnnotation(a);
        for (Java.NamedTypeDeclaration mtd : atd.getMemberTypeDeclarations()) mtd.accept(this.typeDeclarationTraverser);
        for (Java.MethodDeclarator md : atd.getMethodDeclarations()) this.traverseMethodDeclarator(md);
    }

    /** @see Traverser */
    protected void
    traverseNamedClassDeclaration(Java.NamedClassDeclaration ncd) throws EX {
        for (Java.Type implementedType : ncd.implementedTypes) {
            implementedType.accept(this.atomTraverser);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept(this.atomTraverser);
        this.traverseClassDeclaration(ncd);
    }

    /** @see Traverser */
    protected void
    traverseInterfaceDeclaration(Java.InterfaceDeclaration id) throws EX {
        for (Java.TypeBodyDeclaration cd : id.constantDeclarations) cd.accept(this.typeBodyDeclarationTraverser);
        for (Java.Type extendedType : id.extendedTypes) extendedType.accept(this.atomTraverser);
        this.traverseAbstractTypeDeclaration(id);
    }

    /** @see Traverser */
    protected void
    traverseFunctionDeclarator(Java.FunctionDeclarator fd) throws EX {
        this.traverseFormalParameters(fd.formalParameters);
        if (fd.optionalStatements != null) {
            for (Java.BlockStatement bs : fd.optionalStatements) bs.accept(this.blockStatementTraverser);
        }
    }

    /** @see Traverser */
    protected void
    traverseFormalParameters(Java.FunctionDeclarator.FormalParameters formalParameters) throws EX {
        for (Java.FunctionDeclarator.FormalParameter formalParameter : formalParameters.parameters) {
            this.traverseFormalParameter(formalParameter);
        }
    }

    /** @see Traverser */
    protected void
    traverseFormalParameter(Java.FunctionDeclarator.FormalParameter formalParameter) throws EX {
        formalParameter.type.accept(this.atomTraverser);
    }

    /** @see Traverser */
    protected void
    traverseAbstractTypeBodyDeclaration(Java.AbstractTypeBodyDeclaration atbd) throws EX { this.traverseLocated(atbd); }

    /** @see Traverser */
    protected void
    traverseStatement(Java.Statement s) throws EX { this.traverseLocated(s); }

    /** @see Traverser */
    protected void
    traverseBreakableStatement(Java.BreakableStatement bs) throws EX { this.traverseStatement(bs); }

    /** @see Traverser */
    protected void
    traverseContinuableStatement(Java.ContinuableStatement cs) throws EX { this.traverseBreakableStatement(cs); }

    /** @see Traverser */
    protected void
    traverseRvalue(Java.Rvalue rv) throws EX { this.traverseAtom(rv); }

    /** @see Traverser */
    protected void
    traverseBooleanRvalue(Java.BooleanRvalue brv) throws EX { this.traverseRvalue(brv); }

    /** @see Traverser */
    protected void
    traverseInvocation(Java.Invocation i) throws EX {
        for (Java.Rvalue argument : i.arguments) argument.accept(this.rvalueTraverser);
        this.traverseRvalue(i);
    }

    /** @see Traverser */
    protected void
    traverseConstructorInvocation(Java.ConstructorInvocation ci) throws EX {
        for (Java.Rvalue argument : ci.arguments) argument.accept(this.rvalueTraverser);
        this.traverseAtom(ci);
    }

    /** @see Traverser */
    protected void
    traverseEnumConstant(Java.EnumConstant ec) throws EX {

        for (Java.ConstructorDeclarator cd : ec.constructors) this.traverseConstructorDeclarator(cd);

        if (ec.optionalArguments != null) {
            for (Java.Rvalue a : ec.optionalArguments) this.traverseRvalue(a);
        }

        this.traverseAbstractTypeDeclaration(ec);
    }

    /** @see Traverser */
    protected void
    traversePackageMemberEnumDeclaration(Java.PackageMemberEnumDeclaration pmed) throws EX {
        this.traversePackageMemberClassDeclaration(pmed);
    }

    /** @see Traverser */
    protected void
    traverseMemberEnumDeclaration(Java.MemberEnumDeclaration med) throws EX {
        this.traverseMemberClassDeclaration(med);
    }

    /** @see Traverser */
    protected void
    traversePackageMemberAnnotationTypeDeclaration(Java.PackageMemberAnnotationTypeDeclaration pmatd) throws EX {
        this.traversePackageMemberInterfaceDeclaration(pmatd);
    }

    /** @see Traverser */
    protected void
    traverseMemberAnnotationTypeDeclaration(Java.MemberAnnotationTypeDeclaration matd) throws EX {
        this.traverseMemberInterfaceDeclaration(matd);
    }

    /** @see Traverser */
    protected void
    traverseLvalue(Java.Lvalue lv) throws EX { this.traverseRvalue(lv); }

    /** @see Traverser */
    protected void
    traverseType(Java.Type t) throws EX { this.traverseAtom(t); }

    /** @see Traverser */
    protected void
    traverseAtom(Java.Atom a) throws EX { this.traverseLocated(a); }

    /** @see Traverser */
    @SuppressWarnings("unused") protected void
    traverseLocated(Java.Located l) throws EX {}
}
