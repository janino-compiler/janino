
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

import java.io.*;
import java.util.*;

import org.codehaus.janino.util.AutoIndentWriter;

/**
 * A visitor that unparses (un-compiles) an AST to a {@link Writer}. See
 * {@link #main(String[])} for a usage example.
 */
public class UnparseVisitor implements Visitor.ComprehensiveVisitor {
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
            String fileName = args[i];

            // Parse each compilation unit.
            FileReader r = new FileReader(fileName);
            Java.CompilationUnit cu;
            try {
                cu = new Parser(new Scanner(fileName, r)).parseCompilationUnit();
            } finally {
                r.close();
            }

            // Unparse each compilation unit.
            UnparseVisitor.unparse(cu, w);
        }
        w.flush();
    }

    /**
     * Unparse the given {@link Java.CompilationUnit} to the given {@link Writer}.
     */
    public static void unparse(Java.CompilationUnit cu, Writer w) {
        new UnparseVisitor(w).unparseCompilationUnit(cu);
    }

    public UnparseVisitor(Writer w) {
        this.aiw = new AutoIndentWriter(w);
        this.pw = new PrintWriter(this.aiw, true);
    }

    public void unparseCompilationUnit(Java.CompilationUnit cu) {
        if (cu.optionalPackageDeclaration != null) {
            this.pw.println("package " + cu.optionalPackageDeclaration.packageName + ';');
        }
        for (Iterator it = cu.importDeclarations.iterator(); it.hasNext();) {
            ((Java.CompilationUnit.ImportDeclaration) it.next()).accept(this);
        }
        for (Iterator it = cu.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            ((Java.PackageMemberTypeDeclaration) it.next()).accept(this);
            this.pw.println();
        }
    }

    public void visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) {
        this.pw.println("import " + Java.join(stid.identifiers, ".") + ';');
    }
    public void visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) {
        this.pw.println("import " + Java.join(tiodd.identifiers, ".") + ".*;");
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
        this.unparseDocComment(cd);
        this.unparseModifiers(cd.modifiers);
        Java.ClassDeclaration declaringClass = cd.getDeclaringClass();
        this.pw.print(declaringClass instanceof Java.NamedClassDeclaration ? ((Java.NamedClassDeclaration) declaringClass).name : "UNNAMED");
        this.unparseFunctionDeclaratorRest(cd);
        this.pw.print(' ');
        if (cd.optionalConstructorInvocation != null) {
            this.pw.println('{');
            ((Java.BlockStatement) cd.optionalConstructorInvocation).accept(this);
            this.pw.println(';');
            for (Iterator it = cd.optionalBody.statements.iterator(); it.hasNext();) {
                ((Java.BlockStatement) it.next()).accept(this);
                this.pw.println();
            }
            this.pw.print('}');
        } else {
            cd.optionalBody.accept(this);
        }
    }
    public void visitMethodDeclarator(Java.MethodDeclarator md) {
        this.unparseDocComment(md);
        this.unparseModifiers(md.modifiers);
        ((Java.Atom) md.type).accept(this);
        this.pw.print(' ' + md.name);
        this.unparseFunctionDeclaratorRest(md);
        if (md.optionalBody != null) {
            this.pw.print(' ');
            md.optionalBody.accept(this);
        } else {
            this.pw.print(';');
        }
    }
    public void visitFieldDeclaration(Java.FieldDeclaration fd) {
        this.unparseDocComment(fd);
        this.unparseModifiers(fd.modifiers);
        ((Java.Atom) fd.type).accept(this);
        this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) this.pw.print(", ");
            this.unparseVariableDeclarator(fd.variableDeclarators[i]);
        }
        this.pw.print(';');
    }
    public void visitInitializer(Java.Initializer i) {
        if (i.statiC) this.pw.print("static ");
        i.block.accept(this);
    }
    public void visitBlock(Java.Block b) {
        this.pw.println('{');
        for (Iterator it = b.statements.iterator(); it.hasNext();) {
            ((Java.BlockStatement) it.next()).accept(this);
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
        ds.body.accept(this);
        this.pw.print("while (");
        ((Java.Atom) ds.condition).accept(this);
        this.pw.print(");");
    }
    public void visitEmptyStatement(Java.EmptyStatement es) {
        this.pw.print(';');
    }
    public void visitExpressionStatement(Java.ExpressionStatement es) {
        ((Java.Atom) es.rvalue).accept(this);
        this.pw.print(';');
    }
    public void visitForStatement(Java.ForStatement fs) {
        this.pw.print("for (");
        if (fs.optionalInit != null) {
            fs.optionalInit.accept(this);
        } else { 
            this.pw.print(';');
        }
        if (fs.optionalCondition != null) {
            this.pw.print(' ');
            ((Java.Atom) fs.optionalCondition).accept(this);
        } 
        this.pw.print(';');
        if (fs.optionalUpdate != null) {
            this.pw.print(' ');
            for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                if (i > 0) this.pw.print(", ");
                ((Java.Atom) fs.optionalUpdate[i]).accept(this);
            }
        }
        this.pw.print(") ");
        fs.body.accept(this);
    }
    public void visitIfStatement(Java.IfStatement is) {
        this.pw.print("if (");
        ((Java.Atom) is.condition).accept(this);
        this.pw.print(") ");
        is.thenStatement.accept(this);
        if (is.optionalElseStatement != null) {
            this.pw.print(" else ");
            is.optionalElseStatement.accept(this);
        }
    }
    public void visitLabeledStatement(Java.LabeledStatement ls) {
        this.pw.println(ls.label + ':');
        ls.body.accept(this);
    }
    public void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
        lcds.lcd.accept(this);
    }
    public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
        this.unparseModifiers(lvds.modifiers);
        ((Java.Atom) lvds.type).accept(this);
        this.pw.print(' ');
        this.unparseVariableDeclarator(lvds.variableDeclarators[0]);
        for (int i = 1; i < lvds.variableDeclarators.length; ++i) {
            this.pw.print(", ");
            this.unparseVariableDeclarator(lvds.variableDeclarators[i]);
        }
        this.pw.print(';');
    }
    public void visitReturnStatement(Java.ReturnStatement rs) {
        this.pw.print("return");
        if (rs.optionalReturnValue != null) {
            this.pw.print(' ');
            ((Java.Atom) rs.optionalReturnValue).accept(this);
        } 
        this.pw.print(';');
    }
    public void visitSwitchStatement(Java.SwitchStatement ss) {
        this.pw.println("switch (" + ss.condition + ") {");
        for (Iterator it = ss.sbsgs.iterator(); it.hasNext();) {
            Java.SwitchStatement.SwitchBlockStatementGroup sbgs = (Java.SwitchStatement.SwitchBlockStatementGroup) it.next();
            this.aiw.unindent();
            try {
                for (Iterator it2 = sbgs.caseLabels.iterator(); it2.hasNext();) {
                    Java.Rvalue rv = (Java.Rvalue) it2.next();
                    this.pw.print("case ");
                    ((Java.Atom) rv).accept(this);
                    this.pw.println(':');
                }
                if (sbgs.hasDefaultLabel) this.pw.println("default:");
            } finally {
                this.aiw.indent();
            }
            for (Iterator it2 = sbgs.blockStatements.iterator(); it2.hasNext();) {
                ((Java.BlockStatement) it2.next()).accept(this);
                this.pw.println();
            }
        }
        this.pw.print('}');
    }
    public void visitSynchronizedStatement(Java.SynchronizedStatement ss) {
        this.pw.print("synchronized (");
        ((Java.Atom) ss.expression).accept(this);
        this.pw.print(") ");
        ss.body.accept(this);
    }
    public void visitThrowStatement(Java.ThrowStatement ts) {
        this.pw.print("throw ");
        ((Java.Atom) ts.expression).accept(this);
        this.pw.print(';');
    }
    public void visitTryStatement(Java.TryStatement ts) {
        this.pw.print("try ");
        ts.body.accept(this);
        for (Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            Java.CatchClause cc = (Java.CatchClause) it.next();
            this.pw.print(" catch (");
            this.unparseFormalParameter(cc.caughtException);
            this.pw.print(") ");
            cc.body.accept(this);
        }
        if (ts.optionalFinally != null) {
            this.pw.print(" finally ");
            ts.optionalFinally.accept(this);
        }
    }
    public void visitWhileStatement(Java.WhileStatement ws) {
        this.pw.print("while (");
        ((Java.Atom) ws.condition).accept(this);
        this.pw.print(") ");
        ws.body.accept(this);
    }
    public void unparseVariableDeclarator(Java.VariableDeclarator vd) {
        this.pw.print(vd.name);
        for (int i = 0; i < vd.brackets; ++i) this.pw.print("[]");
        if (vd.optionalInitializer != null) {
            this.pw.print(" = ");
            this.unparseArrayInitializerOrRvalue(vd.optionalInitializer);
        }
    }
    public void unparseFormalParameter(Java.FunctionDeclarator.FormalParameter fp) {
        if (fp.finaL) this.pw.print("final ");
        ((Java.Atom) fp.type).accept(this);
        this.pw.print(' ' + fp.name);
    }
    public void visitMethodInvocation(Java.MethodInvocation mi) {
        if (mi.optionalTarget != null) {
            mi.optionalTarget.accept(this);
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
            ((Java.Atom) sci.optionalQualification).accept(this);
            this.pw.print('.');
        }
        this.pw.print("super");
        this.unparseFunctionInvocationArguments(sci.arguments);
    }
    public void visitNewClassInstance(Java.NewClassInstance nci) {
        if (nci.optionalQualification != null) {
            ((Java.Atom) nci.optionalQualification).accept(this);
            this.pw.print('.');
        }
        this.pw.print("new " + nci.type.toString());
        this.unparseFunctionInvocationArguments(nci.arguments);
    }
    public void visitAssignment(Java.Assignment a) {
        ((Java.Atom) a.lhs).accept(this);
        this.pw.print(' ' + a.operator + ' ');
        ((Java.Atom) a.rhs).accept(this);
    }
    public void visitAmbiguousName(Java.AmbiguousName an) { this.pw.print(an.toString()); }
    public void visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
        ((Java.Atom) aae.lhs).accept(this);
        this.pw.print('[');
        ((Java.Atom) aae.index).accept(this);
        this.pw.print(']');
    }
    public void visitArrayLength(Java.ArrayLength al) {
        ((Java.Atom) al.lhs).accept(this);
        this.pw.print(".length");
    }
    public void visitArrayType(Java.ArrayType at) {
        ((Java.Atom) at.componentType).accept(this);
        this.pw.print("[]");
    }
    public void visitBasicType(Java.BasicType bt) {
        this.pw.print(bt.toString());
    }
    public void visitBinaryOperation(Java.BinaryOperation bo) {
        ((Java.Atom) bo.lhs).accept(this);
        this.pw.print(' ' + bo.op + ' ');
        ((Java.Atom) bo.rhs).accept(this);
    }
    public void visitCast(Java.Cast c) {
        this.pw.print('(');
        ((Java.Atom) c.targetType).accept(this);
        this.pw.print(") ");
        ((Java.Atom) c.value).accept(this);
    }
    public void visitClassLiteral(Java.ClassLiteral cl) {
        ((Java.Atom) cl.type).accept(this);
        this.pw.print(".class");
    }
    public void visitConditionalExpression(Java.ConditionalExpression ce) {
        ((Java.Atom) ce.lhs).accept(this);
        this.pw.print(" ? ");
        ((Java.Atom) ce.mhs).accept(this);
        this.pw.print(" : ");
        ((Java.Atom) ce.rhs).accept(this);
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
        fa.lhs.accept(this);
        this.pw.print('.' + fa.field.getName());
    }
    public void visitFieldAccessExpression(Java.FieldAccessExpression fae) {
        fae.lhs.accept(this);
        this.pw.print('.' + fae.fieldName);
    }
    public void visitInstanceof(Java.Instanceof io) {
        ((Java.Atom) io.lhs).accept(this);
        this.pw.print(" instanceof ");
        ((Java.Atom) io.rhs).accept(this);
    }
    public void visitLiteral(Java.Literal l) { this.pw.print(l.toString()); }
    public void visitLocalVariableAccess(Java.LocalVariableAccess lva) { this.pw.print(lva.toString()); }
    public void visitNewArray(Java.NewArray na) {
        this.pw.print("new ");
        ((Java.Atom) na.type).accept(this);
        for (int i = 0; i < na.dimExprs.length; ++i) {
            this.pw.print('[');
            ((Java.Atom) na.dimExprs[i]).accept(this);
            this.pw.print(']');
        }
        for (int i = 0; i < na.dims; ++i) {
            this.pw.print("[]");
        }
    }
    public void visitNewInitializedArray(Java.NewInitializedArray nai) {
        this.pw.print("new ");
        nai.arrayType.accept(this);
        this.pw.print(" ");
        this.unparseArrayInitializerOrRvalue(nai.arrayInitializer);
    }
    public void visitPackage(Java.Package p) { this.pw.print(p.toString()); }
    public void visitParameterAccess(Java.ParameterAccess pa) { this.pw.print(pa.toString()); }
    public void visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
        ((Java.Atom) qtr.qualification).accept(this);
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
        ((Java.Atom) uo.operand).accept(this);
    }
    public void visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
        this.pw.print('(');
        ((Java.Atom) pe.value).accept(this);
        this.pw.print(')');
    }

    // Helpers

    private void unparseNamedClassDeclaration(Java.NamedClassDeclaration ncd) {
        this.unparseDocComment(ncd);
        this.unparseModifiers(ncd.modifiers);
        this.pw.print("class " + ncd.name);
        if (ncd.optionalExtendedType != null) {
            this.pw.print(" extends ");
            ((Java.Atom) ncd.optionalExtendedType).accept(this);
        } 
        if (ncd.implementedTypes.length > 0) this.pw.print(" implements " + Java.join(ncd.implementedTypes, ", "));
        this.pw.println(" {");
        this.unparseClassDeclarationBody(ncd);
        this.pw.print('}');
    }
    private void unparseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) {
        if (aiorv instanceof Java.Rvalue) {
            ((Java.Atom) aiorv).accept(this);
        } else
        if (aiorv instanceof Java.ArrayInitializer) {
            Java.ArrayInitializer ai = (Java.ArrayInitializer) aiorv;
            if (ai.values.length == 0) {
                this.pw.print("{}");
            } else
            {
                this.pw.print("{ ");
                this.unparseArrayInitializerOrRvalue(ai.values[0]);
                for (int i = 1; i < ai.values.length; ++i) {
                    this.pw.print(", ");
                    this.unparseArrayInitializerOrRvalue(ai.values[i]);
                }
                this.pw.print(" }");
            }
        } else
        {
            throw new RuntimeException("Unexpected array initializer or rvalue class " + aiorv.getClass().getName());
        }
    }

    public void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
        ((Java.Atom) acd.baseType).accept(this);
        this.pw.println(" {");
        this.unparseClassDeclarationBody(acd);
        this.pw.print('}');
    }
    public void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
        if (naci.optionalQualification != null) {
            ((Java.Atom) naci.optionalQualification).accept(this);
            this.pw.print('.');
        }
        this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
        for (int i = 0; i < naci.arguments.length; ++i) {
            if (i > 0) this.pw.print(", ");
            ((Java.Atom) naci.arguments[i]).accept(this);
        }
        this.pw.println(") {");
        this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
        this.pw.print('}');
    }
    // Multi-line!
    private void unparseClassDeclarationBody(Java.ClassDeclaration cd) {
        for (Iterator it = cd.constructors.iterator(); it.hasNext();) {
            ((Java.ConstructorDeclarator) it.next()).accept(this);
            this.pw.println();
        }
        this.unparseAbstractTypeDeclarationBody(cd);
        for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).accept(this);
            this.pw.println();
        }
    }
    private void unparseInterfaceDeclaration(Java.InterfaceDeclaration id) {
        this.unparseDocComment(id);
        this.unparseModifiers(id.modifiers);
        this.pw.print("interface " + id.name);
        if (id.extendedTypes.length > 0) this.pw.print(" extends " + Java.join(id.extendedTypes, ", "));
        this.pw.println(" {");
        this.unparseAbstractTypeDeclarationBody(id);
        for (Iterator it = id.constantDeclarations.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).accept(this);
            this.pw.println();
        }
        this.pw.print('}');
    }
    // Multi-line!
    private void unparseAbstractTypeDeclarationBody(Java.AbstractTypeDeclaration atd) {
        for (Iterator it = atd.declaredMethods.iterator(); it.hasNext();) {
            ((Java.MethodDeclarator) it.next()).accept(this);
            this.pw.println();
        }
        for (Iterator it = atd.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).accept(this);
            this.pw.println();
        }
    }
    private void unparseFunctionDeclaratorRest(Java.FunctionDeclarator fd) {
        this.pw.print('(');
        for (int i = 0; i < fd.formalParameters.length; ++i) {
            if (i > 0) this.pw.print(", ");
            this.unparseFormalParameter(fd.formalParameters[i]);
        }
        this.pw.print(')');
        if (fd.thrownExceptions.length > 0) this.pw.print(" throws " + Java.join(fd.thrownExceptions, ", "));
    }
    private void unparseDocComment(Java.DocCommentable dc) {
        String optionalDocComment = dc.getDocComment();
        if (optionalDocComment != null) {
            this.pw.println();
            this.pw.print("/**");
            this.aiw.setPrefix(" *");
            try {
                this.pw.print(optionalDocComment);
            } finally {
                this.aiw.setPrefix(null);
            }
            this.pw.println(" */");
        }
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
            ((Java.Atom) arguments[i]).accept(this);
        }
        this.pw.print(')');
    }
}
