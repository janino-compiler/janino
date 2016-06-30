
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractPackageMemberClassDeclaration;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.util.AutoIndentWriter;

/**
 * A visitor that unparses (un-compiles) an AST to a {@link Writer}. See {@link #main(String[])} for a usage example.
 */
@SuppressWarnings({ "rawtypes", "unchecked" }) public
class UnparseVisitor implements Visitor.ComprehensiveVisitor<Void, RuntimeException> {

    /**
     * Where the {@code visit...()} methods print their text. Noice that this {@link PrintWriter} does not print to
     * the output directly, but through an {@link AutoIndentWriter}.
     */
    protected final PrintWriter pw;

    /**
     * Testing of parsing/unparsing.
     * <p>
     * Reads compilation units from the files named on the command line
     * and unparses them to {@link System#out}.
     */
    public static void
    main(String[] args) throws Exception {

        Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
        for (String fileName : args) {

            // Parse each compilation unit.
            FileReader           r = new FileReader(fileName);
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

    /** Unparses the given {@link Java.CompilationUnit} to the given {@link Writer}. */
    public static void
    unparse(Java.CompilationUnit cu, Writer w) {
        UnparseVisitor uv = new UnparseVisitor(w);
        uv.unparseCompilationUnit(cu);
        uv.close();
    }

    public
    UnparseVisitor(Writer w) {
        this.pw  = new PrintWriter(new AutoIndentWriter(w), true);
    }

    /** Flushes all generated code. */
    public void
    close() { this.pw.flush(); }

    /** @param cu The compilation unit to unparse */
    public void
    unparseCompilationUnit(Java.CompilationUnit cu) {

        PackageDeclaration opd = cu.optionalPackageDeclaration;
        if (opd != null) {
            this.pw.println();
            this.pw.println("package " + opd.packageName + ';');
        }

        if (!cu.importDeclarations.isEmpty()) {
            this.pw.println();
            for (Java.CompilationUnit.ImportDeclaration id : cu.importDeclarations) id.accept(this);
        }

        for (Java.PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            this.pw.println();
            this.unparseTypeDeclaration(pmtd);
            this.pw.println();
        }
    }

    @Override @Nullable public Void
    visitRvalue(Rvalue rv) {
        return rv.accept((Visitor.RvalueVisitor<Void, RuntimeException>) this);
    }

    @Override @Nullable public Void
    visitAnnotation(Annotation a) {
        return a.accept((Visitor.AnnotationVisitor<Void, RuntimeException>) this);
    }

    @Override @Nullable public Void
    visitSingleTypeImportDeclaration(Java.CompilationUnit.SingleTypeImportDeclaration stid) {
        this.pw.println("import " + Java.join(stid.identifiers, ".") + ';');
        return null;
    }

    @Override @Nullable public Void
    visitTypeImportOnDemandDeclaration(Java.CompilationUnit.TypeImportOnDemandDeclaration tiodd) {
        this.pw.println("import " + Java.join(tiodd.identifiers, ".") + ".*;");
        return null;
    }

    @Override @Nullable public Void
    visitSingleStaticImportDeclaration(Java.CompilationUnit.SingleStaticImportDeclaration ssid) {
        this.pw.println("import static " + Java.join(ssid.identifiers, ".") + ';');
        return null;
    }

    @Override @Nullable public Void
    visitStaticImportOnDemandDeclaration(Java.CompilationUnit.StaticImportOnDemandDeclaration siodd) {
        this.pw.println("import static " + Java.join(siodd.identifiers, ".") + ".*;");
        return null;
    }

    @Override @Nullable public Void
    visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
        this.unparseNamedClassDeclaration(lcd);
        return null;
    }

    @Override @Nullable public Void
    visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
        this.unparseNamedClassDeclaration(mcd);
        return null;
    }

    @Override @Nullable public Void
    visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
        this.unparseInterfaceDeclaration(mid);
        return null;
    }

    @Override @Nullable public Void
    visitPackageMemberClassDeclaration(AbstractPackageMemberClassDeclaration apmcd) {
        this.unparseNamedClassDeclaration(apmcd);
        return null;
    }

    @Override @Nullable public Void
    visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
        this.unparseInterfaceDeclaration(pmid);
        return null;
    }

    @Override @Nullable public Void
    visitConstructorDeclarator(Java.ConstructorDeclarator cd) {

        this.unparseDocComment(cd);
        this.unparseAnnotations(cd.modifiers.annotations);
        this.unparseModifiers(cd.modifiers.accessFlags);
        Java.AbstractClassDeclaration declaringClass = cd.getDeclaringClass();
        this.pw.print(
            declaringClass instanceof Java.NamedClassDeclaration
            ? ((Java.NamedClassDeclaration) declaringClass).name
            : "UNNAMED"
        );
        this.unparseFunctionDeclaratorRest(cd);

        List<? extends BlockStatement> oss = cd.optionalStatements;
        if (oss == null) {
            this.pw.print(';');
            return null;
        }

        this.pw.print(' ');

        ConstructorInvocation oci = cd.optionalConstructorInvocation;
        if (oci != null) {
            this.pw.println('{');
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseBlockStatement(oci);
            this.pw.println(';');

            if (!oss.isEmpty()) {
                this.pw.println();
                this.unparseStatements(oss);
            }
            this.pw.print(AutoIndentWriter.UNINDENT + "}");
        } else
        if (oss.isEmpty()) {
            this.pw.print("{}");
        } else
        {
            this.pw.println('{');
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseStatements(oss);
            this.pw.print(AutoIndentWriter.UNINDENT + "}");
        }

        return null;
    }

    @Override @Nullable public Void
    visitMethodDeclarator(Java.MethodDeclarator md) {

        // For methods declared as members of an *interface*, the default access flags are "public abstract".
        boolean declaringTypeIsInterface = md.getDeclaringType() instanceof Java.InterfaceDeclaration;

        final List<? extends BlockStatement> oss = md.optionalStatements;

        this.unparseDocComment(md);
        this.unparseAnnotations(md.modifiers.annotations);
        this.unparseModifiers(
            md.modifiers.remove(declaringTypeIsInterface ? Mod.PUBLIC | Mod.ABSTRACT : 0).accessFlags
        );
        this.unparseType(md.type);
        this.pw.print(' ' + md.name);
        this.unparseFunctionDeclaratorRest(md);
        if (oss == null) {
            this.pw.print(';');
        } else
        if (oss.isEmpty()) {
            this.pw.print(" {}");
        } else
        {
            this.pw.println(" {");
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseStatements(oss);
            this.pw.print(AutoIndentWriter.UNINDENT);
            this.pw.print('}');
        }
        return null;
    }

    @Override @Nullable public Void
    visitFieldDeclaration(Java.FieldDeclaration fd) {
        this.unparseDocComment(fd);
        this.unparseAnnotations(fd.modifiers.annotations);
        this.unparseModifiers(fd.modifiers.accessFlags);
        this.unparseType(fd.type);
        this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) this.pw.print(", ");
            this.unparseVariableDeclarator(fd.variableDeclarators[i]);
        }
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitInitializer(Java.Initializer i) {
        if (i.statiC) this.pw.print("static ");
        this.unparseBlockStatement(i.block);
        return null;
    }

    @Override @Nullable public Void
    visitBlock(Java.Block b) {
        if (b.statements.isEmpty()) {
            this.pw.print("{}");
            return null;
        }
        this.pw.println('{');
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseStatements(b.statements);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
        return null;
    }

    private void
    unparseStatements(List<? extends Java.BlockStatement> statements) {

        int state = -1;
        for (Java.BlockStatement bs : statements) {
            int x  = (
                bs instanceof Java.Block                             ? 1 :
                bs instanceof Java.LocalClassDeclarationStatement    ? 2 :
                bs instanceof Java.LocalVariableDeclarationStatement ? 3 :
                bs instanceof Java.SynchronizedStatement             ? 4 :
                99
            );
            if (state != -1 && state != x) this.pw.println(AutoIndentWriter.CLEAR_TABULATORS);
            state = x;

            this.unparseBlockStatement(bs);
            this.pw.println();
        }
    }

    @Override @Nullable public Void
    visitBreakStatement(Java.BreakStatement bs) {
        this.pw.print("break");
        if (bs.optionalLabel != null) this.pw.print(' ' + bs.optionalLabel);
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitContinueStatement(Java.ContinueStatement cs) {
        this.pw.print("continue");
        if (cs.optionalLabel != null) this.pw.print(' ' + cs.optionalLabel);
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitAssertStatement(Java.AssertStatement as) {

        this.pw.print("assert ");
        this.unparse(as.expression1);

        Rvalue oe2 = as.optionalExpression2;
        if (oe2 != null) {
            this.pw.print(" : ");
            this.unparse(oe2);
        }
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitDoStatement(Java.DoStatement ds) {
        this.pw.print("do ");
        this.unparseBlockStatement(ds.body);
        this.pw.print("while (");
        this.unparse(ds.condition);
        this.pw.print(");");
        return null;
    }

    @Override @Nullable public Void
    visitEmptyStatement(Java.EmptyStatement es) {
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitExpressionStatement(Java.ExpressionStatement es) {
        this.unparse(es.rvalue);
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitForStatement(Java.ForStatement fs) {
        this.pw.print("for (");
        if (fs.optionalInit != null) {
            this.unparseBlockStatement(fs.optionalInit);
        } else {
            this.pw.print(';');
        }

        Rvalue oc = fs.optionalCondition;
        if (oc != null) {
            this.pw.print(' ');
            this.unparse(oc);
        }

        this.pw.print(';');

        Rvalue[] ou = fs.optionalUpdate;
        if (ou != null) {
            this.pw.print(' ');
            for (int i = 0; i < ou.length; ++i) {
                if (i > 0) this.pw.print(", ");
                this.unparse(ou[i]);
            }
        }

        this.pw.print(") ");
        this.unparseBlockStatement(fs.body);
        return null;
    }

    @Override @Nullable public Void
    visitForEachStatement(Java.ForEachStatement fes) {
        this.pw.print("for (");
        this.unparseFormalParameter(fes.currentElement, false);
        this.pw.print(" : ");
        this.unparse(fes.expression);
        this.pw.print(") ");
        this.unparseBlockStatement(fes.body);
        return null;
    }

    @Override @Nullable public Void
    visitIfStatement(Java.IfStatement is) {
        this.pw.print("if (");
        this.unparse(is.condition);
        this.pw.print(") ");
        this.unparseBlockStatement(is.thenStatement);

        BlockStatement oes = is.optionalElseStatement;
        if (oes != null) {
            this.pw.println(" else");
            this.unparseBlockStatement(oes);
        }

        return null;
    }

    @Override @Nullable public Void
    visitLabeledStatement(Java.LabeledStatement ls) {
        this.pw.println(ls.label + ':');
        this.unparseBlockStatement(ls.body);
        return null;
    }

    @Override @Nullable public Void
    visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
        this.unparseTypeDeclaration(lcds.lcd);
        return null;
    }

    @Override @Nullable public Void
    visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
        this.unparseAnnotations(lvds.modifiers.annotations);
        this.unparseModifiers(lvds.modifiers.accessFlags);
        this.unparseType(lvds.type);
        this.pw.print(' ');
        this.pw.print(AutoIndentWriter.TABULATOR);
        this.unparseVariableDeclarator(lvds.variableDeclarators[0]);
        for (int i = 1; i < lvds.variableDeclarators.length; ++i) {
            this.pw.print(", ");
            this.unparseVariableDeclarator(lvds.variableDeclarators[i]);
        }
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitReturnStatement(Java.ReturnStatement rs) {

        this.pw.print("return");

        Rvalue orv = rs.optionalReturnValue;
        if (orv != null) {
            this.pw.print(' ');
            this.unparse(orv);
        }

        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitSwitchStatement(Java.SwitchStatement ss) {
        this.pw.print("switch (");
        this.unparse(ss.condition);
        this.pw.println(") {");
        for (Java.SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            this.pw.print(AutoIndentWriter.UNINDENT);
            try {
                for (Java.Rvalue rv : sbsg.caseLabels) {
                    this.pw.print("case ");
                    this.unparse(rv);
                    this.pw.println(':');
                }
                if (sbsg.hasDefaultLabel) this.pw.println("default:");
            } finally {
                this.pw.print(AutoIndentWriter.INDENT);
            }
            for (Java.BlockStatement bs : sbsg.blockStatements) {
                this.unparseBlockStatement(bs);
                this.pw.println();
            }
        }
        this.pw.print('}');
        return null;
    }

    @Override @Nullable public Void
    visitSynchronizedStatement(Java.SynchronizedStatement ss) {
        this.pw.print("synchronized (");
        this.unparse(ss.expression);
        this.pw.print(") ");
        this.unparseBlockStatement(ss.body);
        return null;
    }

    @Override @Nullable public Void
    visitThrowStatement(Java.ThrowStatement ts) {
        this.pw.print("throw ");
        this.unparse(ts.expression);
        this.pw.print(';');
        return null;
    }

    @Override @Nullable public Void
    visitTryStatement(Java.TryStatement ts) {
        this.pw.print("try ");
        this.unparseBlockStatement(ts.body);
        for (Java.CatchClause cc : ts.catchClauses) {
            this.pw.print(" catch (");
            this.unparseFormalParameter(cc.caughtException, false);
            this.pw.print(") ");
            this.unparseBlockStatement(cc.body);
        }

        Block of = ts.optionalFinally;
        if (of != null) {
            this.pw.print(" finally ");
            this.unparseBlockStatement(of);
        }

        return null;
    }

    @Override @Nullable public Void
    visitWhileStatement(Java.WhileStatement ws) {
        this.pw.print("while (");
        this.unparse(ws.condition);
        this.pw.print(") ");
        this.unparseBlockStatement(ws.body);
        return null;
    }

    private void
    unparseVariableDeclarator(Java.VariableDeclarator vd) {
        this.pw.print(vd.name);
        for (int i = 0; i < vd.brackets; ++i) this.pw.print("[]");

        ArrayInitializerOrRvalue oi = vd.optionalInitializer;
        if (oi != null) {
            this.pw.print(" = ");
            this.unparseArrayInitializerOrRvalue(oi);
        }
    }

    private void
    unparseFormalParameter(Java.FunctionDeclarator.FormalParameter fp, boolean hasEllipsis) {
        if (fp.finaL) this.pw.print("final ");
        this.unparseType(fp.type);
        if (hasEllipsis) this.pw.write("...");
        this.pw.print(" " + AutoIndentWriter.TABULATOR + fp.name);
    }

    @Override @Nullable public Void
    visitMethodInvocation(Java.MethodInvocation mi) {
        if (mi.optionalTarget != null) {
            this.unparseLhs(mi.optionalTarget, ".");
            this.pw.print('.');
        }
        this.pw.print(mi.methodName);
        this.unparseFunctionInvocationArguments(mi.arguments);
        return null;
    }

    @Override @Nullable public Void
    visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
        this.pw.print("this");
        this.unparseFunctionInvocationArguments(aci.arguments);
        return null;
    }

    @Override @Nullable public Void
    visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
        if (sci.optionalQualification != null) {
            this.unparseLhs(sci.optionalQualification, ".");
            this.pw.print('.');
        }
        this.pw.print("super");
        this.unparseFunctionInvocationArguments(sci.arguments);
        return null;
    }

    @Override @Nullable public Void
    visitNewClassInstance(Java.NewClassInstance nci) {
        if (nci.optionalQualification != null) {
            this.unparseLhs(nci.optionalQualification, ".");
            this.pw.print('.');
        }
        assert nci.type != null;
        this.pw.print("new " + nci.type.toString());
        this.unparseFunctionInvocationArguments(nci.arguments);
        return null;
    }

    @Override @Nullable public Void
    visitAssignment(Java.Assignment a) {
        this.unparseLhs(a.lhs, a.operator);
        this.pw.print(' ' + a.operator + ' ');
        this.unparseRhs(a.rhs, a.operator);
        return null;
    }

    @Override @Nullable public Void
    visitAmbiguousName(Java.AmbiguousName an) { this.pw.print(an.toString()); return null; }

    @Override @Nullable public Void
    visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
        this.unparseLhs(aae.lhs, "[ ]");
        this.pw.print('[');
        this.unparse(aae.index);
        this.pw.print(']');
        return null;
    }

    @Override @Nullable public Void
    visitArrayLength(Java.ArrayLength al) {
        this.unparseLhs(al.lhs, ".");
        this.pw.print(".length");
        return null;
    }

    @Override @Nullable public Void
    visitArrayType(Java.ArrayType at) {
        this.unparseType(at.componentType);
        this.pw.print("[]");
        return null;
    }

    @Override @Nullable public Void
    visitBasicType(Java.BasicType bt) { this.pw.print(bt.toString()); return null; }

    @Override @Nullable public Void
    visitBinaryOperation(Java.BinaryOperation bo) {
        this.unparseLhs(bo.lhs, bo.op);
        this.pw.print(' ' + bo.op + ' ');
        this.unparseRhs(bo.rhs, bo.op);
        return null;
    }

    @Override @Nullable public Void
    visitCast(Java.Cast c) {
        this.pw.print('(');
        this.unparseType(c.targetType);
        this.pw.print(") ");
        this.unparseRhs(c.value, "cast");
        return null;
    }

    @Override @Nullable public Void
    visitClassLiteral(Java.ClassLiteral cl) {
        this.unparseType(cl.type);
        this.pw.print(".class");
        return null;
    }

    @Override @Nullable public Void
    visitConditionalExpression(Java.ConditionalExpression ce) {
        this.unparseLhs(ce.lhs, "?:");
        this.pw.print(" ? ");
        this.unparseLhs(ce.mhs, "?:");
        this.pw.print(" : ");
        this.unparseRhs(ce.rhs, "?:");
        return null;
    }

    @Override @Nullable public Void
    visitCrement(Java.Crement c) {
        if (c.pre) {
            this.pw.print(c.operator);
            this.unparseUnaryOperation(c.operand, c.operator + "x");
        } else
        {
            this.unparseUnaryOperation(c.operand, "x" + c.operator);
            this.pw.print(c.operator);
        }
        return null;
    }

    @Override @Nullable public Void
    visitFieldAccess(Java.FieldAccess fa) {
        this.unparseLhs(fa.lhs, ".");
        this.pw.print('.' + fa.field.getName());
        return null;
    }

    @Override @Nullable public Void
    visitFieldAccessExpression(Java.FieldAccessExpression fae) {
        this.unparseLhs(fae.lhs, ".");
        this.pw.print('.' + fae.fieldName);
        return null;
    }

    @Override @Nullable public Void
    visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {
        if (scfae.optionalQualification != null) {
            this.unparseType(scfae.optionalQualification);
            this.pw.print(".super." + scfae.fieldName);
        } else
        {
            this.pw.print("super." + scfae.fieldName);
        }
        return null;
    }

    @Override @Nullable public Void
    visitInstanceof(Java.Instanceof io) {
        this.unparseLhs(io.lhs, "instanceof");
        this.pw.print(" instanceof ");
        this.unparseType(io.rhs);
        return null;
    }

    // SUPPRESS CHECKSTYLE LineLength:8
    @Override @Nullable public Void visitIntegerLiteral(Java.IntegerLiteral il)              { this.pw.print(il.value);             return null; }
    @Override @Nullable public Void visitFloatingPointLiteral(Java.FloatingPointLiteral fpl) { this.pw.print(fpl.value);            return null; }
    @Override @Nullable public Void visitBooleanLiteral(Java.BooleanLiteral bl)              { this.pw.print(bl.value);             return null; }
    @Override @Nullable public Void visitCharacterLiteral(Java.CharacterLiteral cl)          { this.pw.print(cl.value);             return null; }
    @Override @Nullable public Void visitStringLiteral(Java.StringLiteral sl)                { this.pw.print(sl.value);             return null; }
    @Override @Nullable public Void visitNullLiteral(Java.NullLiteral nl)                    { this.pw.print(nl.value);             return null; }
    @Override @Nullable public Void visitSimpleConstant(Java.SimpleConstant sl)              { this.pw.print("[" + sl.value + ']'); return null; }
    @Override @Nullable public Void visitLocalVariableAccess(Java.LocalVariableAccess lva)   { this.pw.print(lva.toString());       return null; }

    @Override @Nullable public Void
    visitNewArray(Java.NewArray na) {
        this.pw.print("new ");
        this.unparseType(na.type);
        for (Rvalue dimExpr : na.dimExprs) {
            this.pw.print('[');
            this.unparse(dimExpr);
            this.pw.print(']');
        }
        for (int i = 0; i < na.dims; ++i) {
            this.pw.print("[]");
        }
        return null;
    }

    @Override @Nullable public Void
    visitNewInitializedArray(Java.NewInitializedArray nai) {

        this.pw.print("new ");

        ArrayType at = nai.arrayType;
        assert at != null;
        this.unparseType(at);

        this.pw.print(" ");

        this.unparseArrayInitializerOrRvalue(nai.arrayInitializer);

        return null;
    }

    @Override @Nullable public Void
    visitPackage(Java.Package p) { this.pw.print(p.toString()); return null; }

    @Override @Nullable public Void
    visitParameterAccess(Java.ParameterAccess pa) { this.pw.print(pa.toString()); return null; }

    @Override @Nullable public Void
    visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
        this.unparseType(qtr.qualification);
        this.pw.print(".this");
        return null;
    }

    @Override @Nullable public Void
    visitReferenceType(Java.ReferenceType rt) { this.pw.print(rt.toString()); return null; }

    @Override @Nullable public Void
    visitRvalueMemberType(Java.RvalueMemberType rmt) { this.pw.print(rmt.toString()); return null; }

    @Override @Nullable public Void
    visitSimpleType(Java.SimpleType st) { this.pw.print(st.toString()); return null; }

    @Override @Nullable public Void
    visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
        this.pw.print("super." + smi.methodName);
        this.unparseFunctionInvocationArguments(smi.arguments);
        return null;
    }

    @Override @Nullable public Void
    visitThisReference(Java.ThisReference tr) { this.pw.print("this"); return null; }

    @Override @Nullable public Void
    visitUnaryOperation(Java.UnaryOperation uo) {
        this.pw.print(uo.operator);
        this.unparseUnaryOperation(uo.operand, uo.operator + "x");
        return null;
    }

    @Override @Nullable public Void
    visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
        this.pw.print('(');
        this.unparse(pe.value);
        this.pw.print(')');
        return null;
    }

    // Helpers

    private void
    unparseBlockStatement(Java.BlockStatement blockStatement) { blockStatement.accept(this); }

    private void
    unparseTypeDeclaration(Java.TypeDeclaration typeDeclaration) { typeDeclaration.accept(this); }

    private void
    unparseType(Java.Type type) { ((Java.Atom) type).accept(this); }

    private void
    unparse(Java.Atom operand) { operand.accept(this); }

    /**
     * Iff the <code>operand</code> is unnatural for the <code>unaryOperator</code>, enclose the
     * <code>operand</code> in parentheses. Example: "a+b" is an unnatural operand for unary "!x".
     *
     * @param unaryOperator ++x --x +x -x ~x !x x++ x--
     */
    private void
    unparseUnaryOperation(Java.Rvalue operand, String unaryOperator) {
        int cmp = UnparseVisitor.comparePrecedence(unaryOperator, operand);
        this.unparse(operand, cmp < 0);
    }

    /**
     * Iff the <code>lhs</code> is unnatural for the <code>binaryOperator</code>, enclose the
     * <code>lhs</code> in parentheses. Example: "a+b" is an unnatural lhs for operator "*".
     *
     * @param binaryOperator = +=... ?: || && | ^ & == != < > <= >= instanceof << >> >>> + - * / % cast
     */
    private void
    unparseLhs(Java.Atom lhs, String binaryOperator) {
        int cmp = UnparseVisitor.comparePrecedence(binaryOperator, lhs);
        this.unparse(lhs, cmp < 0 || (cmp == 0 && UnparseVisitor.isLeftAssociate(binaryOperator)));
    }


    /**
     * Iff the <code>rhs</code> is unnatural for the <code>binaryOperator</code>, enclose the
     * <code>rhs</code> in parentheses. Example: "a+b" is an unnatural rhs for operator "*".
     */
    private void
    unparseRhs(Java.Rvalue rhs, String binaryOperator) {
        int cmp = UnparseVisitor.comparePrecedence(binaryOperator, rhs);
        this.unparse(rhs, cmp < 0 || (cmp == 0 && UnparseVisitor.isRightAssociate(binaryOperator)));
    }

    private void
    unparse(Java.Atom operand, boolean natural) {
        if (!natural) this.pw.print("((( ");
        this.unparse(operand);
        if (!natural) this.pw.print(" )))");
    }

    /**
     * Return true iff operator is right associative e.g. <code>a = b = c</code> evaluates as
     * <code>a = (b = c)</code>.
     *
     * @return Return true iff operator is right associative
     */
    private static boolean
    isRightAssociate(String op) { return UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS.contains(op); }

    /**
     * Return true iff operator is left associative e.g. <code>a - b - c</code> evaluates as
     * <code>(a - b) - c</code>.
     *
     * @return Return true iff operator is left associative
     */
    private static boolean
    isLeftAssociate(String op) { return UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS.contains(op); }

    /**
     * Returns a value
     * <ul>
     *   <li>&lt; 0 iff the <code>operator</code> has lower precedence than the <code>operand</code>
     *   <li>==; 0 iff the <code>operator</code> has equal precedence than the <code>operand</code>
     *   <li>&gt; 0 iff the <code>operator</code> has higher precedence than the <code>operand</code>
     * </ul>
     */
    private static int
    comparePrecedence(String operator, Java.Atom operand) {
        if (operand instanceof Java.BinaryOperation) {
            return (
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(((Java.BinaryOperation) operand).op)
            );
        } else
        if (operand instanceof Java.UnaryOperation) {
            return (
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(((Java.UnaryOperation) operand).operator + "x")
            );
        } else
        if (operand instanceof Java.ConditionalExpression) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("?:");
        } else
        if (operand instanceof Java.Instanceof) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("instanceof");
        } else
        if (operand instanceof Java.Cast) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("cast");
        } else
        if (operand instanceof Java.MethodInvocation || operand instanceof Java.FieldAccess) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence(".");
        } else
        if (operand instanceof Java.NewArray) {
            return UnparseVisitor.getOperatorPrecedence(operator) - UnparseVisitor.getOperatorPrecedence("new");
        } else
        if (operand instanceof Java.Crement) {
            Java.Crement c = (Java.Crement) operand;
            return (
                UnparseVisitor.getOperatorPrecedence(operator)
                - UnparseVisitor.getOperatorPrecedence(c.pre ? c.operator + "x" : "x" + c.operator)
            );
        } else
        {
            // All other rvalues (e.g. literal) have higher precedence than any operator.
            return -1;
        }
    }

    private static int
    getOperatorPrecedence(String operator) {
        return ((Integer) UnparseVisitor.OPERATOR_PRECEDENCE.get(operator)).intValue();
    }

    private static final Set<String>          LEFT_ASSOCIATIVE_OPERATORS  = new HashSet();
    private static final Set<String>          RIGHT_ASSOCIATIVE_OPERATORS = new HashSet();
    private static final Set<String>          UNARY_OPERATORS             = new HashSet();
    private static final Map<String, Integer> OPERATOR_PRECEDENCE         = new HashMap();
    static {
        Object[] ops = {
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", ">>>=",
                                                        "&=", "^=", "|=", // SUPPRESS CHECKSTYLE WrapAndIndent
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "?:",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "||",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "&&",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "|",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "^",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "&",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "==", "!=",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "<", ">", "<=", ">=", "instanceof",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "<<", ">>", ">>>",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "+", "-",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "*", "/", "%",
            UnparseVisitor.RIGHT_ASSOCIATIVE_OPERATORS, "cast",
            UnparseVisitor.UNARY_OPERATORS,             "++x", "--x", "+x", "-x", "~x", "!x",
            UnparseVisitor.UNARY_OPERATORS,             "x++", "x--",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  "new",
            UnparseVisitor.LEFT_ASSOCIATIVE_OPERATORS,  ".", "[ ]",
        };
        int precedence = 0;
        LOOP1: for (int i = 0;;) {
            final Set<String> s  = (Set) ops[i++];
            final Integer     pi = new Integer(++precedence);
            for (;;) {
                if (i == ops.length) break LOOP1;
                if (!(ops[i] instanceof String)) break;
                String op = (String) ops[i++];
                s.add(op);
                UnparseVisitor.OPERATOR_PRECEDENCE.put(op, pi);
            }
        }
    }

    private void
    unparseNamedClassDeclaration(Java.NamedClassDeclaration ncd) {
        this.unparseDocComment(ncd);
        this.unparseAnnotations(ncd.getAnnotations());
        this.unparseModifiers(ncd.getModifierFlags());
        this.pw.print("class " + ncd.name);

        Type oet = ncd.optionalExtendedType;
        if (oet != null) {
            this.pw.print(" extends ");
            this.unparseType(oet);
        }

        if (ncd.implementedTypes.length > 0) this.pw.print(" implements " + Java.join(ncd.implementedTypes, ", "));
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseClassDeclarationBody(ncd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    private void
    unparseArrayInitializerOrRvalue(Java.ArrayInitializerOrRvalue aiorv) {
        if (aiorv instanceof Java.Rvalue) {
            this.unparse((Java.Rvalue) aiorv);
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
            throw new JaninoRuntimeException(
                "Unexpected array initializer or rvalue class "
                + aiorv.getClass().getName()
            );
        }
    }

    @Override @Nullable public Void
    visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
        this.unparseType(acd.baseType);
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseClassDeclarationBody(acd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
        return null;
    }
    @Override @Nullable public Void
    visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
        if (naci.optionalQualification != null) {
            this.unparseLhs(naci.optionalQualification, ".");
            this.pw.print('.');
        }
        this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
        for (int i = 0; i < naci.arguments.length; ++i) {
            if (i > 0) this.pw.print(", ");
            this.unparse(naci.arguments[i]);
        }
        this.pw.println(") {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
        return null;
    }
    // Multi-line!
    private void
    unparseClassDeclarationBody(Java.AbstractClassDeclaration cd) {
        for (Java.ConstructorDeclarator ctord : cd.constructors) {
            this.pw.println();
            ctord.accept(this);
            this.pw.println();
        }
        this.unparseTypeDeclarationBody(cd);
        for (Java.BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            this.pw.println();
            vdoi.accept(this);
            this.pw.println();
        }
    }

    /**
     * @return Whether {@link #unparseClassDeclarationBody(Java.AbstractClassDeclaration)} will produce <em>no</em>
     *         output
     */
    private static boolean
    classDeclarationBodyIsEmpty(Java.AbstractClassDeclaration cd) {
        return (
            cd.constructors.isEmpty()
            && cd.getMethodDeclarations().isEmpty()
            && cd.getMemberTypeDeclarations().isEmpty()
            && cd.variableDeclaratorsAndInitializers.isEmpty()
        );
    }
    private void
    unparseInterfaceDeclaration(Java.InterfaceDeclaration id) {
        this.unparseDocComment(id);
        this.unparseAnnotations(id.getAnnotations());
        this.unparseModifiers(id.getModifierFlags());
        this.pw.print("interface ");
        this.pw.print(id.name);
        if (id.extendedTypes.length > 0) this.pw.print(" extends " + Java.join(id.extendedTypes, ", "));
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(id);
        for (Java.TypeBodyDeclaration cnstd : id.constantDeclarations) {
            cnstd.accept(this);
            this.pw.println();
        }
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    // Multi-line!
    private void
    unparseTypeDeclarationBody(Java.TypeDeclaration td) {
        for (Java.MethodDeclarator md : td.getMethodDeclarations()) {
            this.pw.println();
            md.accept(this);
            this.pw.println();
        }
        for (Java.MemberTypeDeclaration mtd : td.getMemberTypeDeclarations()) {
            this.pw.println();
            ((Java.TypeBodyDeclaration) mtd).accept(this);
            this.pw.println();
        }
    }
    private void
    unparseFunctionDeclaratorRest(Java.FunctionDeclarator fd) {
        boolean big = fd.formalParameters.parameters.length >= 4;
        this.pw.print('(');
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.INDENT); }
        for (int i = 0; i < fd.formalParameters.parameters.length; ++i) {
            if (i > 0) {
                if (big) {
                    this.pw.println(',');
                } else
                {
                    this.pw.print(", ");
                }
            }
            this.unparseFormalParameter(
                fd.formalParameters.parameters[i],
                i == fd.formalParameters.parameters.length - 1 && fd.formalParameters.variableArity
            );
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
        if (fd.thrownExceptions.length > 0) this.pw.print(" throws " + Java.join(fd.thrownExceptions, ", "));
    }

    private void
    unparseDocComment(Java.DocCommentable dc) {
        String optionalDocComment = dc.getDocComment();
        if (optionalDocComment != null) {
            this.pw.print("/**");
            BufferedReader br = new BufferedReader(new StringReader(optionalDocComment));
            for (;;) {
                String line;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new JaninoRuntimeException(null, e);
                }
                if (line == null) break;
                this.pw.println(line);
                this.pw.print(" *");
            }
            this.pw.println("/");
        }
    }

    private void
    unparseAnnotations(Java.Annotation[] annotations) {
        for (Annotation a : annotations) a.accept((Visitor.AnnotationVisitor<Void, RuntimeException>) this);
    }

    private void
    unparseModifiers(short modifiers) {
        if (modifiers != 0) {
            this.pw.print(Mod.shortToString(modifiers) + ' ');
        }
    }

    private void
    unparseFunctionInvocationArguments(Java.Rvalue[] arguments) {
        boolean big = arguments.length >= 5;
        this.pw.print('(');
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.INDENT); }
        for (int i = 0; i < arguments.length; ++i) {
            if (i > 0) {
                if (big) {
                    this.pw.println(',');
                } else
                {
                    this.pw.print(", ");
                }
            }
            this.unparse(arguments[i]);
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
    }

    @Override @Nullable public Void
    visitMarkerAnnotation(Java.MarkerAnnotation ma) {
        this.pw.append('@').append(ma.type.toString()).append(' ');
        return null;
    }

    @Override @Nullable public Void
    visitNormalAnnotation(Java.NormalAnnotation na) {
        this.pw.append('@').append(na.type.toString()).append('(');
        for (int i = 0; i < na.elementValuePairs.length; i++) {
            Java.ElementValuePair evp = na.elementValuePairs[i];

            if (i > 0) this.pw.print(", ");

            this.pw.append(evp.identifier).append(" = ");

            evp.elementValue.accept(this);
        }
        this.pw.append(") ");
        return null;
    }

    @Override @Nullable public Void
    visitSingleElementAnnotation(Java.SingleElementAnnotation sea) {
        this.pw.append('@').append(sea.type.toString()).append('(');
        sea.elementValue.accept(this);
        this.pw.append(") ");
        return null;
    }

    @Override @Nullable public Void
    visitElementValueArrayInitializer(Java.ElementValueArrayInitializer evai) {
        if (evai.elementValues.length == 0) {
            this.pw.append("{}");
            return null;
        }

        this.pw.append("{ ");
        evai.elementValues[0].accept(this);
        for (int i = 1; i < evai.elementValues.length; i++) {
            this.pw.append(", ");
            evai.elementValues[i].accept(this);
        }
        this.pw.append(" }");
        return null;
    }

    @Override @Nullable public Void
    visitEnumConstant(EnumConstant ec) {

        this.unparseAnnotations(ec.getAnnotations());

        this.pw.append(ec.name);

        if (ec.optionalArguments != null) {
            this.unparseFunctionInvocationArguments(ec.optionalArguments);
        }

        if (!UnparseVisitor.classDeclarationBodyIsEmpty(ec)) {
            this.pw.println(" {");
            this.pw.print(AutoIndentWriter.INDENT);
            this.unparseClassDeclarationBody(ec);
            this.pw.print(AutoIndentWriter.UNINDENT + "}");
        }

        return null;
    }

    @Override @Nullable public Void
    visitMemberEnumDeclaration(MemberEnumDeclaration med) {
        this.unparseEnumDeclaration(med);
        return null;
    }

    @Override @Nullable public Void
    visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) {
        this.unparseEnumDeclaration(pmed);
        return null;
    }

    private void
    unparseEnumDeclaration(Java.EnumDeclaration ed) {

        this.unparseDocComment(ed);
        this.unparseAnnotations(ed.getAnnotations());
        this.unparseModifiers(ed.getModifierFlags());
        this.pw.print("enum " + ed.getName());

        Type[] its = ed.getImplementedTypes();
        if (its.length > 0) this.pw.print(" implements " + Java.join(its, ", "));

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        Iterator<EnumConstant> it = ed.getConstants().iterator();
        if (it.hasNext()) {
            for (;;) {
                this.visitEnumConstant(it.next());

                if (!it.hasNext()) break;
                this.pw.print(", ");
            }
        }
        this.pw.println();
        this.pw.println(';');
        this.unparseClassDeclarationBody((Java.AbstractClassDeclaration) ed);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    @Override @Nullable public Void
    visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) {
        this.unparseAnnotationTypeDeclaration(matd);
        return null;
    }

    @Override @Nullable public Void
    visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) {
        this.unparseAnnotationTypeDeclaration(pmatd);
        return null;
    }

    private void
    unparseAnnotationTypeDeclaration(Java.AnnotationTypeDeclaration atd) {
        this.unparseDocComment(atd);
        this.unparseAnnotations(atd.getAnnotations());
        this.unparseModifiers(atd.getModifierFlags());
        this.pw.print("@interface ");
        this.pw.print(atd.getName());

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(atd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }
}
