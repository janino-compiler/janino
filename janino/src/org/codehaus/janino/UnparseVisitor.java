
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2005, Arno Unkrig
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

import java.io.*;
import java.util.Iterator;

import org.codehaus.janino.util.AutoIndentWriter;

/**
 * A visitor that unparses (un-compiles) an AST to a {@link Writer}. See
 * {@link #main(String[])} for a usage example.
 */
public class UnparseVisitor implements Visitor {
    private final AutoIndentWriter aiw;
    private final PrintWriter      pw;

    /**
     * Testing of parsing/unparsing.
     * <p>
     * Reads compilation units from the files named on the command line
     * and unparses them to {@link System#out}.
     */
    public static void main(String[] args) throws Exception {
        Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
        for (int i = 0; i < args.length; ++i) {
            Java.CompilationUnit cu = new Parser(new Scanner(args[i])).parseCompilationUnit();
            UnparseVisitor.unparse(cu, w);
        }
        w.flush();
    }

    /**
     * Unparse the given {@link Java.CompilationUnit} to the given {@link Writer}.
     */
    public static void unparse(Java.CompilationUnit cu, Writer w) {
        new UnparseVisitor(w).visitCompilationUnit(cu);
    }

    public UnparseVisitor(Writer w) {
        this.aiw = new AutoIndentWriter(w);
        this.pw = new PrintWriter(this.aiw, true);
    }

    public void visitCompilationUnit(Java.CompilationUnit cu) {
        if (cu.optionalPackageDeclaration != null) {
            this.pw.println("package " + cu.optionalPackageDeclaration.getPackageName() + ';');
        }
        for (Iterator it = cu.importDeclarations.iterator(); it.hasNext();) {
            ((Java.ImportDeclaration) it.next()).visit(this);
        }
        for (Iterator it = cu.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            ((Java.PackageMemberTypeDeclaration) it.next()).visit(this);
            this.pw.println();
        }
    }

    public void visitSingleTypeImportDeclaration(Java.SingleTypeImportDeclaration stid) {
        this.pw.println("import " + Java.join(stid.getIdentifiers(), ".") + ';');
    }
    public void visitTypeImportOnDemandDeclaration(Java.TypeImportOnDemandDeclaration tiodd) {
        this.pw.println("import " + Java.join(tiodd.getIdentifiers(), ".") + ".*;");
    }

    public void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
        this.unparseNamedClassDeclaration(lcd);
    }
    public void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
        this.unparseNamedClassDeclaration(mcd);
    }
    public void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
        this.unparseInterfaceDeclaration(mid);
    }
    public void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) {
        this.unparseNamedClassDeclaration(pmcd);
    }
    public void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
        this.unparseInterfaceDeclaration(pmid);
    }
    public void visitConstructorDeclarator(Java.ConstructorDeclarator cd) {
        this.unparseModifiers(cd.modifiers);
        this.pw.print(((Java.NamedClassDeclaration) cd.declaringType).name);
        this.unparseFunctionDeclaratorRest(cd);
        this.pw.print(' ');
        if (cd.optionalExplicitConstructorInvocation != null) {
            this.pw.println('{');
            cd.optionalExplicitConstructorInvocation.visit(this);
            this.pw.println(';');
            for (Iterator it = cd.optionalBody.statements.iterator(); it.hasNext();) {
                ((Java.BlockStatement) it.next()).visit(this);
                this.pw.println();
            }
            this.pw.print('}');
        } else {
            cd.optionalBody.visit(this);
        }
    }
    public void visitMethodDeclarator(Java.MethodDeclarator md) {
        this.unparseModifiers(md.modifiers);
        md.type.visit(this);
        this.pw.print(' ' + md.name);
        this.unparseFunctionDeclaratorRest(md);
        if (md.optionalBody != null) {
            this.pw.print(' ');
            md.optionalBody.visit(this);
        } else {
            this.pw.print(';');
        }
    }
    public void visitFieldDeclarator(Java.FieldDeclarator fd) {
        this.unparseModifiers(fd.modifiers);
        fd.type.visit(this);
        this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) this.pw.print(", ");
            fd.variableDeclarators[i].visit(this);
        }
        this.pw.print(';');
    }
    public void visitInitializer(Java.Initializer i) {
        if (i.statiC) this.pw.print("static ");
        i.block.visit(this);
    }
    public void visitBlock(Java.Block b) {
        this.pw.println('{');
        for (Iterator it = b.statements.iterator(); it.hasNext();) {
            ((Java.BlockStatement) it.next()).visit(this);
            this.pw.println();
        }
        this.pw.print('}');
    }
    public void visitBreakStatement(Java.BreakStatement bs) {
        this.pw.print("break");
        if (bs.optionalLabel != null) this.pw.print(' ' + bs.optionalLabel);
        this.pw.print(';');
    }
    public void visitContinueStatement(Java.ContinueStatement cs) {
        this.pw.print("continue");
        if (cs.optionalLabel != null) this.pw.print(' ' + cs.optionalLabel);
        this.pw.print(';');
    }
    public void visitDoStatement(Java.DoStatement ds) {
        this.pw.print("do ");
        ds.body.visit(this);
        this.pw.print("while (");
        ds.condition.visit(this);
        this.pw.print(");");
    }
    public void visitEmptyStatement(Java.EmptyStatement es) {
        this.pw.print(';');
    }
    public void visitExpressionStatement(Java.ExpressionStatement es) {
        es.rvalue.visit(this);
        this.pw.print(';');
    }
    public void visitForStatement(Java.ForStatement fs) {
        this.pw.print("for (");
        if (fs.optionalInit != null) {
            fs.optionalInit.visit(this);
        } else { 
            this.pw.print(';');
        }
        if (fs.optionalCondition != null) {
            this.pw.print(' ');
            fs.optionalCondition.visit(this);
        } 
        this.pw.print(';');
        if (fs.optionalUpdate != null) {
            this.pw.print(' ');
            for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                if (i > 0) this.pw.print(", ");
                fs.optionalUpdate[i].visit(this);
            }
        }
        this.pw.print(") ");
        fs.body.visit(this);
    }
    public void visitIfStatement(Java.IfStatement is) {
        this.pw.print("if (");
        is.condition.visit(this);
        this.pw.print(") ");
        is.thenStatement.visit(this);
        if (is.optionalElseStatement != null) {
            this.pw.print(" else ");
            is.optionalElseStatement.visit(this);
        }
    }
    public void visitLabeledStatement(Java.LabeledStatement ls) {
        this.pw.println(ls.label + ':');
        ls.body.visit(this);
    }
    public void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
        lcds.lcd.visit(this);
    }
    public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
        this.unparseModifiers(lvds.modifiers);
        lvds.type.visit(this);
        this.pw.print(' ');
        lvds.variableDeclarators[0].visit(this);
        for (int i = 1; i < lvds.variableDeclarators.length; ++i) {
            this.pw.print(", ");
            lvds.variableDeclarators[i].visit(this);
        }
        this.pw.print(';');
    }
    public void visitReturnStatement(Java.ReturnStatement rs) {
        this.pw.print("return");
        if (rs.optionalReturnValue != null) {
            this.pw.print(' ');
            rs.optionalReturnValue.visit(this);
        } 
        this.pw.print(';');
    }
    public void visitSwitchStatement(Java.SwitchStatement ss) {
        this.pw.println("switch (" + ss.condition + ") {");
        for (Iterator it = ss.sbsgs.iterator(); it.hasNext();) {
            Java.SwitchBlockStatementGroup sbgs = (Java.SwitchBlockStatementGroup) it.next();
            this.aiw.unindent();
            try {
                for (Iterator it2 = sbgs.caseLabels.iterator(); it2.hasNext();) {
                    Java.Rvalue rv = (Java.Rvalue) it2.next();
                    this.pw.print("case ");
                    rv.visit(this);
                    this.pw.println(':');
                }
                if (sbgs.hasDefaultLabel) this.pw.println("default:");
            } finally {
                this.aiw.indent();
            }
            for (Iterator it2 = sbgs.blockStatements.iterator(); it2.hasNext();) {
                ((Java.BlockStatement) it2.next()).visit(this);
                this.pw.println();
            }
        }
        this.pw.print('}');
    }
    public void visitSynchronizedStatement(Java.SynchronizedStatement ss) {
        this.pw.print("synchronized (");
        ss.expression.visit(this);
        this.pw.print(") ");
        ss.body.visit(this);
    }
    public void visitThrowStatement(Java.ThrowStatement ts) {
        this.pw.print("throw ");
        ts.expression.visit(this);
        this.pw.print(';');
    }
    public void visitTryStatement(Java.TryStatement ts) {
        this.pw.print("try ");
        ts.body.visit(this);
        for (Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            Java.CatchClause cc = (Java.CatchClause) it.next();
            this.pw.print(" catch (");
            cc.caughtException.visit(this);
            this.pw.print(") ");
            cc.body.visit(this);
        }
        if (ts.optionalFinally != null) {
            this.pw.print(" finally ");
            ts.optionalFinally.visit(this);
        }
    }
    public void visitWhileStatement(Java.WhileStatement ws) {
        this.pw.print("while (");
        ws.condition.visit(this);
        this.pw.print(") ");
        ws.body.visit(this);
    }
    public void visitVariableDeclarator(Java.VariableDeclarator vd) {
        this.pw.print(vd.name);
        for (int i = 0; i < vd.brackets; ++i) this.pw.print("[]");
        if (vd.optionalInitializer != null) {
            this.pw.print(" = ");
            vd.optionalInitializer.visit(this);
        }
    }
    public void visitFormalParameter(Java.FormalParameter fp) {
        if (fp.finaL) this.pw.print("final ");
        fp.type.visit(this);
        this.pw.print(' ' + fp.name);
    }
    public void visitMethodInvocation(Java.MethodInvocation mi) {
        if (mi.optionalTarget != null) {
            mi.optionalTarget.visit(this);
            this.pw.print('.');
        }
        this.pw.print(mi.methodName);
        this.unparseFunctionInvocationArguments(mi.arguments);
    }
    public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
        this.pw.print("this");
        this.unparseFunctionInvocationArguments(aci.arguments);
    }
    public void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
        if (sci.optionalQualification != null) {
            sci.optionalQualification.visit(this);
            this.pw.print('.');
        }
        this.pw.print("super");
        this.unparseFunctionInvocationArguments(sci.arguments);
    }
    public void visitNewClassInstance(Java.NewClassInstance nci) {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.visit(this);
            this.pw.print('.');
        }
        this.pw.print("new " + nci.type.toString());
        this.unparseFunctionInvocationArguments(nci.arguments);
    }
    public void visitAssignment(Java.Assignment a) {
        a.lhs.visit(this);
        this.pw.print(' ' + a.operator + ' ');
        a.rhs.visit(this);
    }
    public void visitArrayInitializer(Java.ArrayInitializer ai) {
        this.pw.print("new ");
        ai.arrayType.visit(this);
        this.pw.println(" {");
        for (int i = 0; i < ai.values.length; ++i) {
            ai.values[i].visit(this);
            this.pw.println(',');
        }
        this.pw.print('}');
    }
    public void visitAmbiguousName(Java.AmbiguousName an) { this.pw.print(an.toString()); }
    public void visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
        aae.lhs.visit(this);
        this.pw.print('[');
        aae.index.visit(this);
        this.pw.print(']');
    }
    public void visitArrayLength(Java.ArrayLength al) {
        al.lhs.visit(this);
        this.pw.print(".length");
    }
    public void visitArrayType(Java.ArrayType at) {
        at.componentType.visit(this);
        this.pw.print("[]");
    }
    public void visitBasicType(Java.BasicType bt) {
        this.pw.print(bt.toString());
    }
    public void visitBinaryOperation(Java.BinaryOperation bo) {
        bo.lhs.visit(this);
        this.pw.print(' ' + bo.op + ' ');
        bo.rhs.visit(this);
    }
    public void visitCast(Java.Cast c) {
        this.pw.print('(');
        c.targetType.visit(this);
        this.pw.print(") ");
        c.value.visit(this);
    }
    public void visitClassLiteral(Java.ClassLiteral cl) {
        cl.type.visit(this);
        this.pw.print(".class");
    }
    public void visitConditionalExpression(Java.ConditionalExpression ce) {
        ce.lhs.visit(this);
        this.pw.print(" ? ");
        ce.mhs.visit(this);
        this.pw.print(" : ");
        ce.rhs.visit(this);
    }
    public void visitConstantValue(Java.ConstantValue cv) { this.pw.print(cv.toString()); }
    public void visitCrement(Java.Crement c) {
        this.pw.print(
            c.pre ?
            c.operator + c.operand :
            c.operand + c.operator
        );
    }
    public void visitFieldAccess(Java.FieldAccess fa) {
        fa.lhs.visit(this);
        this.pw.print('.' + fa.field.getName());
    }
    public void visitFieldAccessExpression(Java.FieldAccessExpression fae) {
        fae.lhs.visit(this);
        this.pw.print('.' + fae.fieldName);
    }
    public void visitInstanceof(Java.Instanceof io) {
        io.lhs.visit(this);
        this.pw.print(" instanceof ");
        io.rhs.visit(this);
    }
    public void visitLiteral(Java.Literal l) { this.pw.print(l.toString()); }
    public void visitLocalVariableAccess(Java.LocalVariableAccess lva) { this.pw.print(lva.toString()); }
    public void visitNewArray(Java.NewArray na) {
        this.pw.print("new ");
        na.type.visit(this);
        for (int i = 0; i < na.dimExprs.length; ++i) {
            this.pw.print('[');
            na.dimExprs[i].visit(this);
            this.pw.print(']');
        }
        for (int i = 0; i < na.dims; ++i) {
            this.pw.print("[]");
        }
    }
    public void visitPackage(Java.Package p) { this.pw.print(p.toString()); }
    public void visitParameterAccess(Java.ParameterAccess pa) { this.pw.print(pa.toString()); }
    public void visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
        qtr.qualification.visit(this);
        this.pw.print(".this");
    }
    public void visitReferenceType(Java.ReferenceType rt) { this.pw.print(rt.toString()); }
    public void visitRvalueMemberType(Java.RvalueMemberType rmt) { this.pw.print(rmt.toString()); }
    public void visitSimpleType(Java.SimpleType st) { this.pw.print(st.toString()); }
    public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
        this.pw.print("super." + smi.methodName);
        this.unparseFunctionInvocationArguments(smi.arguments);
    }
    public void visitThisReference(Java.ThisReference tr) {
        this.pw.print("this");
    }
    public void visitUnaryOperation(Java.UnaryOperation uo) {
        this.pw.print(uo.operator);
        uo.operand.visit(this);
    }
    public void visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
        this.pw.print('(');
        pe.value.visit(this);
        this.pw.print(')');
    }

    // Helpers

    private void unparseNamedClassDeclaration(Java.NamedClassDeclaration ncd) {
        this.unparseModifiers(ncd.modifiers);
        this.pw.print("class " + ncd.name);
        if (ncd.optionalExtendedType != null) {
            this.pw.print(" extends ");
            ncd.optionalExtendedType.visit(this);
        } 
        if (ncd.implementedTypes.length > 0) this.pw.print(" implements " + Java.join(ncd.implementedTypes, ", "));
        this.pw.println(" {");
        this.unparseClassDeclarationBody(ncd);
        this.pw.print('}');
    }
    public void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
        acd.baseType.visit(this);
        this.pw.println(" {");
        this.unparseClassDeclarationBody(acd);
        this.pw.print('}');
    }
    public void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
        if (naci.optionalQualification != null) {
            naci.optionalQualification.visit(this);
            this.pw.print('.');
        }
        this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
        for (int i = 0; i < naci.arguments.length; ++i) {
            if (i > 0) this.pw.print(", ");
            naci.arguments[i].visit(this);
        }
        this.pw.println(") {");
        this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
        this.pw.print('}');
    }
    // Multi-line!
    private void unparseClassDeclarationBody(Java.ClassDeclaration cd) {
        for (Iterator it = cd.constructors.iterator(); it.hasNext();) {
            ((Java.ConstructorDeclarator) it.next()).visit(this);
            this.pw.println();
        }
        this.unparseAbstractTypeDeclarationBody(cd);
        for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).visit(this);
            this.pw.println();
        }
    }
    private void unparseInterfaceDeclaration(Java.InterfaceDeclaration id) {
        this.unparseModifiers(id.modifiers);
        this.pw.print("interface " + id.name);
        if (id.extendedTypes.length > 0) this.pw.print(" extends " + Java.join(id.extendedTypes, ", "));
        this.pw.println(" {");
        this.unparseAbstractTypeDeclarationBody(id);
        for (Iterator it = id.constantDeclarations.iterator(); it.hasNext();) {
            ((Java.FieldDeclarator) it.next()).visit(this);
            this.pw.println();
        }
        this.pw.print('}');
    }
    // Multi-line!
    private void unparseAbstractTypeDeclarationBody(Java.AbstractTypeDeclaration atd) {
        for (Iterator it = atd.declaredMethods.iterator(); it.hasNext();) {
            ((Java.MethodDeclarator) it.next()).visit(this);
            this.pw.println();
        }
        for (Iterator it = atd.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
            ((Java.MemberTypeDeclaration) it.next()).visit(this);
            this.pw.println();
        }
    }
    private void unparseFunctionDeclaratorRest(Java.FunctionDeclarator fd) {
        this.pw.print('(');
        for (int i = 0; i < fd.formalParameters.length; ++i) {
            if (i > 0) this.pw.print(", ");
            fd.formalParameters[i].visit(this);
        }
        this.pw.print(')');
        if (fd.thrownExceptions.length > 0) this.pw.print(" throws " + Java.join(fd.thrownExceptions, ", "));
    }
    private void unparseModifiers(short modifiers) {
        if (modifiers != 0) {
            this.pw.print(Mod.shortToString(modifiers) + ' ');
        }
    }
    private void unparseFunctionInvocationArguments(Java.Rvalue[] arguments) {
        this.pw.print('(');
        for (int i = 0; i < arguments.length; ++i) {
            if (i > 0) this.pw.print(", ");
            arguments[i].visit(this);
        }
        this.pw.print(')');
    }
}
