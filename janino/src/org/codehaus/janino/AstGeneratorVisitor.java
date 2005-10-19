
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
import java.util.*;

/**
 * @author Eugene Kuleshov
 */
public class AstGeneratorVisitor implements Visitor.ComprehensiveVisitor {
    private static final int TAB_SIZE = 4;

    private static final String TAB_FILLER = "                                ";

    private final String name;
    private final PrintWriter pw;

    private int level = 0;
    private Set instances = new HashSet();

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; ++i) {
            Java.CompilationUnit cu = new Parser(new Scanner(args[i])).parseCompilationUnit();
            OutputStreamWriter w = new OutputStreamWriter(System.out);
            new AstGeneratorVisitor(w, "MyAstGenerator").generateCompilationUnit(cu);
            w.flush();
        }
    }
    
    public AstGeneratorVisitor(Writer w, String name) {
        this.pw = new PrintWriter(w);
        this.name = name;
    }


    public void generateCompilationUnit(Java.CompilationUnit cu) {
        write("package org.codehaus.janino;");
        write();
        write("import org.codehaus.janino.Java;");
        write("import org.codehaus.janino.Mod;");
        write("import java.util.*;");
        write();

        write("public class "+name+" implements "+AstCompilationUnitGenerator.class.getName()+" {");
        level++;
        write("public static final String FILE_NAME = \""+cu.optionalFileName+"\";");
        write();

        write("public Java.CompilationUnit generate() throws Exception {");
        level++;

        write("Java.CompilationUnit cu = new Java.CompilationUnit(\"" + cu.optionalFileName + "\");");

        if(cu.optionalPackageDeclaration != null) {
            write("cu.setPackageDeclaration(new Java.PackageDeclaration("+getLocation(cu.optionalPackageDeclaration)+", \""+cu.optionalPackageDeclaration.packageName+"\"));");
        }

        for(Iterator it = cu.importDeclarations.iterator(); it.hasNext();) {
            write("cu.addImportDeclaration(generateImportDeclaration"+getSuffix(it.next())+"());");
        }

        for(Iterator it = cu.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            Java.PackageMemberTypeDeclaration pmtd = (Java.PackageMemberTypeDeclaration) it.next();
            write("cu.addPackageMemberTypeDeclaration(generateMemberTypeDeclaration"+getSuffix(pmtd)+"(cu));");
        }

        write("return cu;");
        level--;
        write("}");
        write();

        // generator methods for child nodes
        for(Iterator it = cu.importDeclarations.iterator(); it.hasNext();) {
            ((Java.ImportDeclaration) it.next()).visit(this);
        }
        for(Iterator it = cu.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            ((Java.PackageMemberTypeDeclaration) it.next()).visit(this);
        }
        
        // helper methods
        write("private Location getLocation(int line, int column) {");
        level++;
        write("return new Location(FILE_NAME, (short) line, (short) column);");
        level--;
        write("}");
        write();
        
        level--;
        write("}");
        write();
    }

    public void visitSingleTypeImportDeclaration(Java.SingleTypeImportDeclaration stid) {
        write("private Java.SingleTypeImportDeclaration generateImportDeclaration"+getSuffix(stid)+"() throws Exception {");
        level++;
        write("return new Java.SingleTypeImportDeclaration("+getLocation(stid)+", "+arrayToString(stid.identifiers)+");");
        level--;
        write("}");
        write();
    }
    
    public void visitTypeImportOnDemandDeclaration(Java.TypeImportOnDemandDeclaration tiodd) {
        write("private Java.TypeImportOnDemandDeclaration generateImportDeclaration"+getSuffix(tiodd)+"() throws Exception {");
        level++;
        write("return new Java.TypeImportOnDemandDeclaration("+getLocation(tiodd)+", "+arrayToString(tiodd.identifiers)+");");
        level--;
        write("}");
        write();
    }
    
    public void visitAnonymousClassDeclaration(Java.AnonymousClassDeclaration acd) {
        write("private Java.AnonymousClassDeclaration generateLocalClassDeclaration"+getSuffix(acd)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.AnonymousClassDeclaration declaration = " +
                "new Java.AnonymousClassDeclaration("+getLocation(acd)+", scope, " +
                        "generateType"+getSuffix(acd.baseType)+"(scope));");

        generateClassDeclarationBody(acd);

        write("return declaration;");
        level--;
        write("}");
        write();
        
        // generator methods
        acd.baseType.visit((Visitor.TypeVisitor) this);

        generateClassDeclarationBodyMethods(acd);
    }


    public void visitLocalClassDeclaration(Java.LocalClassDeclaration lcd) {
        write("private Java.LocalClassDeclaration generateLocalClassDeclaration"+getSuffix(lcd)+"(Java.Block scope) throws Exception {");
        level++;

        write("Java.LocalClassDeclaration declaration = "+
                "new Java.LocalClassDeclaration("+getLocation(lcd)+", scope, "+
                        getModifiers(lcd.modifiers)+", \""+lcd.name+"\", " +
                        (lcd.optionalExtendedType==null ? "null, " : "generateType"+getSuffix(lcd.optionalExtendedType)+"(scope), " )+
                        getGenerateTypes(lcd.implementedTypes, "scope")+");");

        generateClassDeclarationBody(lcd);

        write("return declaration;");
        level--;
        write("}");
        write();

        // generator methods
        if(lcd.optionalExtendedType!=null) {
            lcd.optionalExtendedType.visit((Visitor.TypeVisitor) this);
        }

        generateTypes(lcd.implementedTypes);
        
        generateClassDeclarationBodyMethods(lcd);
    }

    public void visitPackageMemberClassDeclaration(Java.PackageMemberClassDeclaration pmcd) {
        write("private Java.PackageMemberTypeDeclaration generateMemberTypeDeclaration"+getSuffix(pmcd)+"(Java.CompilationUnit cu) throws Exception {");
        level++;
        
        write("Java.PackageMemberClassDeclaration declaration = " +
                "new Java.PackageMemberClassDeclaration("+getLocation(pmcd)+", cu, "+getModifiers(pmcd.modifiers)+", \""+pmcd.name+"\", " +
                (pmcd.optionalExtendedType==null ? "null" : "generateType"+getSuffix(pmcd.optionalExtendedType)+"(cu)")+", "+
                getGenerateTypes(pmcd.implementedTypes, "cu")+");");
        write();

        generateClassDeclarationBody(pmcd);
        
        write("return declaration;");
        level--;
        write("}");
        write();

        generateTypes(pmcd.implementedTypes);
        if(pmcd.optionalExtendedType!=null) {
            pmcd.optionalExtendedType.visit((Visitor.TypeVisitor) this);
        }

        generateClassDeclarationBodyMethods(pmcd);
    }


    public void visitMemberInterfaceDeclaration(Java.MemberInterfaceDeclaration mid) {
        write("private Java.MemberInterfaceDeclaration generateMemberTypeDeclaration"+getSuffix(mid)+"(Java.NamedTypeDeclaration declaringType) throws Exception {");
        level++;
        
        write("Java.MemberInterfaceDeclaration declaration = " +
                "new Java.MemberInterfaceDeclaration("+getLocation(mid)+", declaringType, "+
                        getModifiers(mid.modifiers)+", \""+mid.name+"\", " +
                        getGenerateTypes(mid.extendedTypes, "cu")+");");

        generateAbstractTypeDeclarationBody(mid);

        for(Iterator it = mid.constantDeclarations.iterator(); it.hasNext();) {
            Java.FieldDeclaration fd = (Java.FieldDeclaration) it.next();
            write("declaration.addConstantDeclaration(generateFieldDeclaration"+getSuffix(fd)+"(declaration));");
        }
        
        write("return declaration;");
        level--;
        write("}");
        write();

        generateTypes(mid.extendedTypes);
        generateAbstractTypeDeclarationBodyMethods(mid);
        
        for(Iterator it = mid.constantDeclarations.iterator(); it.hasNext();) {
            ((Java.FieldDeclaration) it.next()).visit((Visitor.TypeBodyDeclarationVisitor) this);
        }
    }

    public void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) {
        write("private Java.PackageMemberInterfaceDeclaration generateMemberTypeDeclaration"+getSuffix(pmid)+"(Java.CompilationUnit cu) throws Exception {");
        level++;
        
        write("Java.PackageMemberInterfaceDeclaration declaration = " +
                "new Java.PackageMemberInterfaceDeclaration(null, cu, "+getModifiers(pmid.modifiers)+", \""+pmid.name+"\", " +
                        getGenerateTypes(pmid.extendedTypes, "cu")+");");

        generateAbstractTypeDeclarationBody(pmid);

        for(Iterator it = pmid.constantDeclarations.iterator(); it.hasNext();) {
            Java.FieldDeclaration fd = (Java.FieldDeclaration) it.next();
            write("declaration.addConstantDeclaration(generateFieldDeclaration"+getSuffix(fd)+"(declaration));");
        }
        
        write("return declaration;");

        level--;
        write("}");
        write();
        
        generateTypes(pmid.extendedTypes);
        generateAbstractTypeDeclarationBodyMethods(pmid);
        
        for(Iterator it = pmid.constantDeclarations.iterator(); it.hasNext();) {
            ((Java.FieldDeclaration) it.next()).visit((Visitor.TypeBodyDeclarationVisitor) this);
        }
    }

    public void visitMemberClassDeclaration(Java.MemberClassDeclaration mcd) {
        write("private Java.MemberClassDeclaration generateMemberTypeDeclaration"+getSuffix(mcd)+"(Java.NamedTypeDeclaration declaringType) throws Exception {");
        level++;
        
        write("Java.MemberClassDeclaration declaration = new Java.MemberClassDeclaration("+getLocation(mcd)+
                ", declaringType, "+ getModifiers(mcd.modifiers)+", \""+mcd.name+"\", " +
                (mcd.optionalExtendedType==null ? "null" : "generateType"+getSuffix(mcd.optionalExtendedType)+"(declaringType)")+", " +
                getGenerateTypes(mcd.implementedTypes, "declaringType")+");");

        generateClassDeclarationBody(mcd);
        
        write("return declaration;");
        level--;
        write("}");
        write();
        
        if(mcd.optionalExtendedType!=null) {
            mcd.optionalExtendedType.visit((Visitor.TypeVisitor) this);
        }
        generateTypes(mcd.implementedTypes);
        generateClassDeclarationBodyMethods(mcd);
    }

    public void visitConstructorDeclarator(Java.ConstructorDeclarator cd) {
        write("private Java.ConstructorDeclarator generateConstructorDeclarator"+getSuffix(cd)+"(Java.ClassDeclaration declaringClass) throws Exception {");
        level++;
        
        write("Java.ConstructorDeclarator declaration = new Java.ConstructorDeclarator(" + getLocation(cd)+", " +
                "declaringClass, "+ getModifiers(cd.modifiers)+", " +
                (cd.formalParameters==null ? "null" : 
                    (cd.formalParameters.length==0 ? "new Java.FormalParameter[0]" :
                        "generateFormalParameters"+getSuffix(cd.formalParameters)+"(declaringClass)"))+", " +
                getGenerateTypes(cd.thrownExceptions, "declaringClass")+");");

        if(cd.optionalExplicitConstructorInvocation!=null) {
            Java.ConstructorInvocation ci = cd.optionalExplicitConstructorInvocation;
            write("declaration.setExplicitConstructorInvocation(generateConstructorInvocation"+getSuffix(ci)+"(declaringClass, declaration));");
        }
        if(cd.optionalBody!=null) {
            write("declaration.setBody(generateStatement"+getSuffix(cd.optionalBody)+"(declaration));");
        }
        
        write("return declaration;");
        level--;
        write("}");
        write();

        // generate methods
        generateFormalParameters(cd.formalParameters);
        generateTypes(cd.thrownExceptions);

        if(cd.optionalExplicitConstructorInvocation!=null) {
            cd.optionalExplicitConstructorInvocation.visit((Visitor.ConstructorInvocationVisitor) this);
        }
        if(cd.optionalBody!=null) {
            cd.optionalBody.visit(this);
        }
    }

    public void visitInitializer(Java.Initializer i) {
        write("private Java.Initializer generateFieldDeclaration"+getSuffix(i)+"(Java.TypeDeclaration declaringType) throws Exception {");
        level++;
        
        write("Java.Initializer declaration = " +
                "new Java.Initializer("+getLocation(i)+", declaringType, "+(i.statiC ? "true" : "false")+");");
        if(i.block!=null) {
            write("declaration.setBlock(generateStatement"+getSuffix(i.block)+"(declaration));");
        }
        
        write("return declaration;");
        level--;
        write("}");
        write();

        if(i.block!=null) {
            i.block.visit(this);
        }
    }

    public void visitMethodDeclarator(Java.MethodDeclarator md) {
        write("private Java.MethodDeclarator generateMethodDeclarator"+getSuffix(md)+"(Java.AbstractTypeDeclaration declaringType) throws Exception {");
        level++;

        if(md.optionalBody==null) {
            write("return new Java.MethodDeclarator("+getLocation(md)+", "+
                    "declaringType, "+getModifiers(md.modifiers)+", " +
                    (md.type==null ? "null" : "generateType"+getSuffix(md.type)+"(declaringType)")+", \""+md.name+"\", " +
                    (md.formalParameters==null ? "null" : 
                        (md.formalParameters.length==0 ? "new Java.FormalParameter[0]" : 
                            "generateFormalParameters"+getSuffix(md.formalParameters)+"(declaringType)"))+", " +
                    getGenerateTypes(md.thrownExceptions, "declaringType")+");");
            
        } else {
            write("Java.MethodDeclarator declaration = new Java.MethodDeclarator("+getLocation(md)+", "+
                    "declaringType, "+getModifiers(md.modifiers)+", " +
                    (md.type==null ? "null" : "generateType"+getSuffix(md.type)+"(declaringType)")+", \""+md.name+"\", " +
                    (md.formalParameters==null ? "null" : 
                        (md.formalParameters.length==0 ? "new Java.FormalParameter[0]" : 
                            "generateFormalParameters"+getSuffix(md.formalParameters)+"(declaringType)"))+", " +
                    getGenerateTypes(md.thrownExceptions, "declaringType")+");");
            write("declaration.setBody(generateStatement"+getSuffix(md.optionalBody)+"(declaration));");
            write("return declaration;");
        
        }
        
        level--;
        write("}");
        write();

        if(md.type!=null) {
            md.type.visit((Visitor.TypeVisitor) this);
        }
        generateTypes(md.thrownExceptions);
        generateFormalParameters(md.formalParameters);
        if(md.optionalBody!=null) {
            md.optionalBody.visit(this);
        }
    }

    public void visitFieldDeclaration(Java.FieldDeclaration fd) {
        write("private Java.FieldDeclaration generateFieldDeclaration"+getSuffix(fd)+"(Java.AbstractTypeDeclaration declaringType) throws Exception {");
        level++;

        write("Java.FieldDeclaration declaration = new Java.FieldDeclaration("+getLocation(fd)+", "+
                "declaringType, "+getModifiers(fd.modifiers)+", " +
                "generateType"+getSuffix(fd.type)+"(declaringType)" +
                ");");
        write("declaration.setVariableDeclarators(generateVariableDeclarators"+getSuffix(fd.variableDeclarators)+"(declaration));");
        
        write("return declaration;");
        level--;
        write("}");
        write();

        fd.type.visit((Visitor.TypeVisitor) this);
        generateVariableDeclarators(fd.variableDeclarators);
    }

    public void visitLabeledStatement(Java.LabeledStatement ls) {
        write("private Java.LabeledStatement generateStatement"+getSuffix(ls)+"(Java.Block scope) throws Exception {");
        level++;

        write("Java.LabeledStatement statement = new Java.LabeledStatement("+getLocation(ls)+", scope, \""+ls.label+"\");");
        write("statement.setBody(generateStatement"+getSuffix(ls.body)+"(scope));");
        write("return statement;");
        
        level--;
        write("}");
        write();

        ls.body.visit(this);
    }

    public void visitBlock(Java.Block b) {
        write("private Java.Block generateStatement"+getSuffix(b)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.Block statement = new Java.Block("+getLocation(b)+", scope);");
        
        for(Iterator it = b.statements.iterator(); it.hasNext();) {
            Java.BlockStatement bs = (Java.BlockStatement) it.next();
            write("statement.addStatement(generateStatement"+getSuffix(bs)+"(statement));");
        }
        
        write("return statement;");
        level--;
        write("}");
        write();
        
        for(Iterator it = b.statements.iterator(); it.hasNext();) {
            ((Java.BlockStatement) it.next()).visit(this);
        }
    }

    public void visitExpressionStatement(Java.ExpressionStatement es) {
        write("private Java.ExpressionStatement generateStatement"+getSuffix(es)+"(Java.Scope scope) throws Exception {");
        level++;

        write("return new Java.ExpressionStatement(generateAtom"+getSuffix(es.rvalue)+"(scope), scope);");
        level--;
        write("}");
        write();
        
        es.rvalue.visit((Visitor.RvalueVisitor) this);
    }

    public void visitIfStatement(Java.IfStatement is) {
        write("private Java.IfStatement generateStatement"+getSuffix(is)+"(Java.Block scope) throws Exception {");
        level++;

        write("return new Java.IfStatement("+getLocation(is)+", scope, "+
                "generateAtom"+getSuffix(is.condition)+"(scope), " +
                "generateStatement"+getSuffix(is.thenStatement)+"(scope), " +
                (is.optionalElseStatement==null ? "null" : "generateStatement"+getSuffix(is.optionalElseStatement)+"(scope)")+
                ");");
        level--;
        write("}");
        write();
        
        is.condition.visit((Visitor.RvalueVisitor) this);
        is.thenStatement.visit(this);
        if(is.optionalElseStatement!=null) {
            is.optionalElseStatement.visit(this);
        }
    }

    public void visitForStatement(Java.ForStatement fs) {
        write("private Java.ForStatement generateStatement"+getSuffix(fs)+"(Java.Block scope) throws Exception {");
        level++;

        write("Java.ForStatement statement = new Java.ForStatement("+getLocation(fs)+", scope);");
        write("statement.set(" +
                (fs.optionalInit==null ? "null" : "generateStatement"+getSuffix(fs.optionalInit)+"(scope)")+", " +
                (fs.optionalCondition==null ? "null" : "generateAtom"+getSuffix(fs.optionalCondition)+"(scope)")+", " +
                getGenerateRvalues(fs.optionalUpdate, "scope")+", "+
                (fs.body==null ? "null" : "generateStatement"+getSuffix(fs.body)+"(scope)") +");");
        write("return statement;");
        level--;
        write("}");
        write();
        
        if(fs.optionalInit!=null) {
            fs.optionalInit.visit(this);
        }
        if(fs.optionalCondition!=null) {
            fs.optionalCondition.visit((Visitor.RvalueVisitor) this);
        }
        generateRvalues(fs.optionalUpdate);
        if(fs.body!=null) {
            fs.body.visit(this);
        }
    }

    public void visitWhileStatement(Java.WhileStatement ws) {
        write("private Java.WhileStatement generateStatement"+getSuffix(ws)+"(Java.Block scope) throws Exception {");
        level++;
        
        write("Java.WhileStatement statement = new Java.WhileStatement("+getLocation(ws)+", scope, " +
                "generateAtom"+getSuffix(ws.condition)+"(scope));");
        if(ws.body!=null) {
            write("statement.setBody(generateStatement"+getSuffix(ws.body)+"(scope));");
        }
        write("return statement;");
        level--;
        write("}");
        write();
        
        ws.condition.visit((Visitor.RvalueVisitor) this);
        if(ws.body!=null) {
            ws.body.visit(this);
        }
    }

    public void visitTryStatement(Java.TryStatement ts) {
        write("private Java.TryStatement generateStatement"+getSuffix(ts)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.TryStatement statement = new Java.TryStatement("+getLocation(ts)+", scope);");
        write("statement.setBody(generateStatement"+getSuffix(ts.body)+"(statement));");

        for(Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            Java.CatchClause cc = (Java.CatchClause) it.next();
            write("statement.addCatchClause(new Java.CatchClause(" +
                    "generateFormalParameter"+getSuffix(cc.caughtException)+"(statement), " +
                    "generateStatement"+getSuffix(cc.body)+"(statement)));");
        }
        
        if(ts.optionalFinally!=null) {
            write("statement.setFinally(generateStatement"+getSuffix(ts.optionalFinally)+"(statement));");
        }

        write("return statement;");
        level--;
        write("}");
        write();
        
        // generate methods
        ts.body.visit(this);

        if(ts.optionalFinally!=null) {
            ts.optionalFinally.visit(this);
        }
        
        for(Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            Java.CatchClause cc = (Java.CatchClause) it.next();
            this.generateFormalParameter(cc.caughtException);
            cc.body.visit(this);
        }
    }

    public void visitSwitchStatement(Java.SwitchStatement ss) {
        write("private Java.SwitchStatement generateStatement"+getSuffix(ss)+"(Java.Block scope) throws Exception {");
        level++;

        write("Java.SwitchStatement statement = new Java.SwitchStatement("+getLocation(ss)+", scope);");
        write("statement.setCondition(generateAtom"+getSuffix(ss.condition)+"(scope));");

        for(Iterator it = ss.sbsgs.iterator(); it.hasNext();) {
            Java.SwitchBlockStatementGroup sbgs = (Java.SwitchBlockStatementGroup) it.next();
            write("statement.addSwitchBlockStatementGroup(generateSwitchBlockStatementGroup"+getSuffix(sbgs)+"(scope));");
        }
        
        write("return statement;");
        level--;
        write("}");
        write();
        
        ss.condition.visit((Visitor.RvalueVisitor) this);
        
        for(Iterator it = ss.sbsgs.iterator(); it.hasNext();) {
            generateSwitchBlockStatementGroup((Java.SwitchBlockStatementGroup) it.next());
        }
    }

    public void visitSynchronizedStatement(Java.SynchronizedStatement ss) {
        write("private Java.SynchronizedStatement generateStatement"+getSuffix(ss)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.SynchronizedStatement statement = new Java.SynchronizedStatement("+getLocation(ss)+", scope, " +
                "generateAtom"+getSuffix(ss.expression)+"(scope));");
        write("statement.setBody(generateStatement"+getSuffix(ss.body)+"(statement));");

        write("return statement;");
        level--;
        write("}");
        write();
        
        //
        ss.expression.visit((Visitor.RvalueVisitor) this);
        ss.body.visit(this);
    }

    public void visitDoStatement(Java.DoStatement ds) {
        write("private Java.DoStatement generateStatement"+getSuffix(ds)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.DoStatement statement = new Java.DoStatement("+getLocation(ds)+", scope);");
        write("statement.setBody(generateStatement"+getSuffix(ds.body)+"(scope));");
        write("statement.setCondition(generateAtom"+getSuffix(ds.condition)+"(scope));");
        
        write("return statement;");
        level--;
        write("}");
        write();
        
        ds.body.visit(this);
        ds.condition.visit((Visitor.RvalueVisitor) this);
    }

    public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {
        write("private Java.LocalVariableDeclarationStatement generateStatement"+getSuffix(lvds)+"(Java.Block declaringBlock) throws Exception {");
        level++;

        write("return new Java.LocalVariableDeclarationStatement("+getLocation(lvds)+", declaringBlock, "+
                getModifiers(lvds.modifiers)+", " +
                "generateType"+getSuffix(lvds.type)+"(declaringBlock), " +
                "generateVariableDeclarators"+getSuffix(lvds.variableDeclarators)+"(declaringBlock));");

        level--;
        write("}");
        write();
        
        lvds.type.visit((Visitor.TypeVisitor) this);
        generateVariableDeclarators(lvds.variableDeclarators);
    }

    public void visitReturnStatement(Java.ReturnStatement rs) {
        write("private Java.ReturnStatement generateStatement"+getSuffix(rs)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ReturnStatement("+getLocation(rs)+", scope, " +
                (rs.optionalReturnValue==null ? "null" : "generateAtom"+getSuffix(rs.optionalReturnValue)+"(scope)") +
                ");");
        level--;
        write("}");
        write();
        
        //
        if(rs.optionalReturnValue!=null) {
            rs.optionalReturnValue.visit((Visitor.RvalueVisitor) this);
        }
    }

    public void visitThrowStatement(Java.ThrowStatement ts) {
        write("private Java.ThrowStatement generateStatement"+getSuffix(ts)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ThrowStatement("+getLocation(ts)+", scope, " +
                "generateAtom"+getSuffix(ts.expression)+"(scope));");
        level--;
        write("}");
        write();
        
        ts.expression.visit((Visitor.RvalueVisitor) this);
    }

    public void visitBreakStatement(Java.BreakStatement bs) {
        write("private Java.BreakStatement generateStatement"+getSuffix(bs)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.BreakStatement("+getLocation(bs)+", scope, "+
                (bs.optionalLabel==null ? "null" : "\""+bs.optionalLabel+"\"")+");");
        level--;
        write("}");
        write();
    }

    public void visitContinueStatement(Java.ContinueStatement cs) {
        write("private Java.ContinueStatement generateStatement"+getSuffix(cs)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ContinueStatement("+getLocation(cs)+", scope, "+
                        (cs.optionalLabel==null ? "null" : "\""+cs.optionalLabel+"\"")+");");
        level--;
        write("}");
        write();
    }

    public void visitEmptyStatement(Java.EmptyStatement es) {
        write("private Java.EmptyStatement generateStatement"+getSuffix(es)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.EmptyStatement("+getLocation(es)+", scope);");
        level--;
        write("}");
        write();
    }

    public void visitLocalClassDeclarationStatement(Java.LocalClassDeclarationStatement lcds) {
        write("private Java.LocalClassDeclarationStatement generateStatement"+getSuffix(lcds)+"(Java.Block scope) throws Exception {");
        level++;
        write("return new Java.LocalClassDeclarationStatement(scope, " +
                "generateLocalClassDeclaration"+getSuffix(lcds.lcd)+"(scope));");
        level--;
        write("}");
        write();
        
        lcds.lcd.visit(this);
    }

    public void generateVariableDeclarator(Java.VariableDeclarator vd) {
        write("private Java.VariableDeclarator generateVariableDeclarator"+getSuffix(vd)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.VariableDeclarator("+getLocation(vd)+", \""+vd.name+"\", "+vd.brackets+", " +
                (vd.optionalInitializer==null ? "null" : "generateAtom"+getSuffix(vd.optionalInitializer)+"(scope)")+
                ");");
        level--;
        write("}");
        write();

        if(vd.optionalInitializer!=null) {
            vd.optionalInitializer.visit((Visitor.RvalueVisitor) this);
        }
    }

    public void generateFormalParameter(Java.FormalParameter fp) {
        write("private Java.FormalParameter generateFormalParameter"+getSuffix(fp)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.FormalParameter("+fp.finaL+", generateType"+getSuffix(fp.type)+"(scope), \""+fp.name+"\");");
        level--;
        write("}");
        write();

        fp.type.visit((Visitor.TypeVisitor) this);
    }

    public void visitNewAnonymousClassInstance(Java.NewAnonymousClassInstance naci) {
        write("private Java.NewAnonymousClassInstance generateAtom"+getSuffix(naci)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.NewAnonymousClassInstance("+getLocation(naci)+", scope, " +
                (naci.optionalQualification==null ? "null" : "generateAtom"+getSuffix(naci.optionalQualification)+"(scope)")+", " +
                "generateLocalClassDeclaration"+getSuffix(naci.anonymousClassDeclaration)+"(scope)"+", " +
                getGenerateRvalues(naci.arguments, "scope")+");");
        level--;
        write("}");
        write();

        if(naci.optionalQualification!=null) {
            naci.optionalQualification.visit((Visitor.RvalueVisitor) this);
        }
        naci.anonymousClassDeclaration.visit(this);
        generateRvalues(naci.arguments);
    }

    public void visitMethodInvocation(Java.MethodInvocation mi) {
        write("private Java.MethodInvocation generateAtom"+getSuffix(mi)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.MethodInvocation("+getLocation(mi)+", scope, "+
                (mi.optionalTarget==null ? "null" : "generateAtom"+getSuffix(mi.optionalTarget)+"(scope)")+", " +
                "\""+mi.methodName+"\", " +
                getGenerateRvalues(mi.arguments, "scope")+");");
        level--;
        write("}");
        write();

        if(mi.optionalTarget!=null) {
            mi.optionalTarget.visit(this);
        }
        generateRvalues(mi.arguments);
    }

    public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) {
        if(instances.contains(aci)) return;
        instances.add(aci);
        
        write("private Java.AlternateConstructorInvocation generateConstructorInvocation"+getSuffix(aci)+"(" +
                "Java.ClassDeclaration declaringClass, Java.ConstructorDeclarator declaringConstructor) throws Exception {");
        level++;

        write("return new Java.AlternateConstructorInvocation("+getLocation(aci)+", "+
                    "declaringClass, declaringConstructor, " +
                    getGenerateRvalues(aci.arguments, "declaringClass")+");");
        level--;
        write("}");
        write();

        generateRvalues(aci.arguments);
    }

    public void visitSuperConstructorInvocation(Java.SuperConstructorInvocation sci) {
        if(instances.contains(sci)) return;
        instances.add(sci);
        
        write("private Java.SuperConstructorInvocation generateConstructorInvocation"+getSuffix(sci)+"(" +
                "Java.ClassDeclaration declaringClass, Java.ConstructorDeclarator declaringConstructor) throws Exception {");
        level++;

        // TODO verify sci.declaringClass and sci.declaringConstructor names
        write("return new Java.SuperConstructorInvocation("+getLocation(sci)+", declaringClass, declaringConstructor, " +
                (sci.optionalQualification==null ? "null" : "generateAtom"+getSuffix(sci.optionalQualification)+"(declaringClass)")+", " +
                getGenerateRvalues(sci.arguments, "declaringClass")+");");

        level--;
        write("}");
        write();

        if(sci.optionalQualification!=null) {
            sci.optionalQualification.visit((Visitor.RvalueVisitor) this);
        }
        generateRvalues(sci.arguments);
    }

    public void visitNewClassInstance(Java.NewClassInstance nci) {
        if(instances.contains(nci)) return;
        instances.add(nci);
        
        write("private Java.NewClassInstance generateAtom"+getSuffix(nci)+"(Java.Scope scope) throws Exception {");
        level++;

        write("return new Java.NewClassInstance("+getLocation(nci)+", scope, " +
                (nci.optionalQualification==null ? "null" : "generateAtom"+getSuffix(nci.optionalQualification)+"(scope)")+", " +
                "generateType"+getSuffix(nci.type)+"(scope), " +
                getGenerateRvalues(nci.arguments, "scope")+");");

        level--;
        write("}");
        write();

        if(nci.optionalQualification!=null) {
            nci.optionalQualification.visit((Visitor.RvalueVisitor) this);
        }
        nci.type.visit((Visitor.TypeVisitor) this);
        generateRvalues(nci.arguments);
    }

    public void visitAssignment(Java.Assignment a) {
        if(instances.contains(a)) return;
        instances.add(a);
        
        write("private Java.Assignment generateAtom"+getSuffix(a)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.Assignment("+getLocation(a)+", " +
                "generateAtom"+getSuffix(a.lhs)+"(scope), \""+a.operator+"\", " +
                "generateAtom"+getSuffix(a.rhs)+"(scope));");
        level--;
        write("}");
        write();

        a.lhs.visit((Visitor.LvalueVisitor) this);
        a.rhs.visit((Visitor.RvalueVisitor) this);
    }

    public void visitArrayInitializer(Java.ArrayInitializer ai) {
        if(instances.contains(ai)) return;
        instances.add(ai);
        
        write("private Java.ArrayInitializer generateAtom"+getSuffix(ai)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ArrayInitializer("+getLocation(ai)+", " +
                "generateType"+getSuffix(ai.arrayType)+"(scope), " +
                // "generateRvalues"+getSuffix(ai.values)+"(scope));");
                getGenerateRvalues(ai.values, "scope")+");");
        level--;
        write("}");
        write();

        ai.arrayType.visit(this);
        generateRvalues(ai.values);
    }

    public void visitSimpleType(Java.SimpleType st) {
        if(instances.contains(st)) return;
        instances.add(st);
        
        write("private Java.SimpleType generateType"+getSuffix(st)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.SimpleType("+getLocation(st)+", generateIClass"+st.iClass+"(scope));");
        level--;
        write("}");
        write();

        generateIClass(st.iClass);
    }

    public void visitBasicType(Java.BasicType bt) {
        if(instances.contains(bt)) return;
        instances.add(bt);
        
        write("private Java.BasicType generateType"+getSuffix(bt)+"(Java.Scope scope) throws Exception {");
        level++;

        String s = "";
        switch(bt.index) {
            case Java.BasicType.VOID:    s = "Java.BasicType.VOID";    break;
            case Java.BasicType.BYTE:    s = "Java.BasicType.BYTE";    break;
            case Java.BasicType.SHORT:   s = "Java.BasicType.SHORT";   break;
            case Java.BasicType.CHAR:    s = "Java.BasicType.CHAR";    break;
            case Java.BasicType.INT:     s = "Java.BasicType.INT";     break;
            case Java.BasicType.LONG:    s = "Java.BasicType.LONG";    break;
            case Java.BasicType.FLOAT:   s = "Java.BasicType.FLOAT";   break;
            case Java.BasicType.DOUBLE:  s = "Java.BasicType.DOUBLE";  break;
            case Java.BasicType.BOOLEAN: s = "Java.BasicType.BOOLEAN"; break;
        }
        write("return new Java.BasicType("+getLocation(bt)+", "+s+");");
        level--;
        write("}");
        write();
    }

    public void visitReferenceType(Java.ReferenceType rt) {
        if(instances.contains(rt)) return;
        instances.add(rt);
        
        write("private Java.ReferenceType generateType"+getSuffix(rt)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ReferenceType("+getLocation(rt)+", scope, "+arrayToString(rt.identifiers)+");");
        level--;
        write("}");
        write();
    }

    public void visitRvalueMemberType(Java.RvalueMemberType rmt) {
        if(instances.contains(rmt)) return;
        instances.add(rmt);
        
        write("private Java.VariableDeclarator generateAtom"+getSuffix(rmt)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.RvalueMemberType("+getLocation(rmt)+", " +
                "generateAtom"+getSuffix(rmt.rvalue)+"(scope), " +
                "\""+rmt.identifier+"\");");
        level--;
        write("}");
        write();

        rmt.rvalue.visit((Visitor.RvalueVisitor) this);
    }

    public void visitArrayType(Java.ArrayType at) {
        if(instances.contains(at)) return;
        instances.add(at);
        
        write("private Java.ArrayType generateType"+getSuffix(at)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ArrayType(generateType"+getSuffix(at.componentType)+"(scope).toType());");
        level--;
        write("}");
        write();

        at.componentType.visit((Visitor.TypeVisitor) this);
    }

    public void visitAmbiguousName(Java.AmbiguousName an) {
        if(instances.contains(an)) return;
        instances.add(an);
        
        write("// bridge method for AmbiguousName to Type conversion");
        write("private Java.Type generateType"+getSuffix(an.toType())+"(Java.Scope scope) throws Exception {");
        level++;
        write("return generateAtom"+getSuffix(an)+"(scope).toType();");
        level--;
        write("}");
        
        write("private Java.AmbiguousName generateAtom"+getSuffix(an)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.AmbiguousName("+getLocation(an)+", scope, "+
                arrayToString(an.identifiers)+", "+an.n+");");
        level--;
        write("}");
        write();
    }

    public void visitPackage(Java.Package p) {
        if(instances.contains(p)) return;
        instances.add(p);
        
        write("private Java.Package generatePackage"+getSuffix(p)+"() throws Exception {");
        level++;
        write("return new Java.Package("+getLocation(p)+", \""+p.name+"\");");
        level--;
        write("}");
        write();
    }

    public void visitLocalVariableAccess(Java.LocalVariableAccess lva) {
        if(instances.contains(lva)) return;
        instances.add(lva);
        
        write("private Java.VariableDeclarator generateVariableDeclarator"+getSuffix(lva)+"(Java.Scope scope) throws Exception {");
        level++;
        Java.LocalVariable lv = lva.localVariable;
        write("return new Java.LocalVariableAccess("+getLocation(lva)+", " + 
                        "new Java.LocalVariable("+lv.finaL+", " +
                                "generateIClass"+getSuffix(lv.type)+"(), "+
                                lv.localVariableArrayIndex+"));");
        level--;
        write("}");
        write();

        generateIClass(lv.type);
    }

    public void visitFieldAccess(Java.FieldAccess fa) {
        if(instances.contains(fa)) return;
        instances.add(fa);
        
        write("private Java.VariableDeclarator generateVariableDeclarator"+getSuffix(fa)+"(Java.Scope scope) throws Exception {");
        level++;

        // TODO verify retrieveal of IField instance
        write("Java.Atom atom = generateAtom"+getSuffix(fa.lhs)+"(scope)");
        write("return new Java.FieldAccess("+getLocation(fa)+", " +
                "atom, Java.findIField(atom.getType(), \""+fa.field.getName()+"\", "+getLocation(fa)+"));");
        level--;
        write("}");
        write();

        fa.lhs.visit(this);
    }

    public void visitArrayLength(Java.ArrayLength al) {
        if(instances.contains(al)) return;
        instances.add(al);
        
        write("private Java.ArrayLength generateAtom"+getSuffix(al)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ArrayAccessExpression("+getLocation(al)+", generateAtom"+getSuffix(al.lhs)+"(scope));");
        level--;
        write("}");
        write();

        al.lhs.visit((Visitor.RvalueVisitor) this);
    }

    public void visitThisReference(Java.ThisReference tr) {
        if(instances.contains(tr)) return;
        instances.add(tr);
        
        write("private Java.ThisReference generateAtom"+getSuffix(tr)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ThisReference("+getLocation(tr)+", (Java.Scope) scope);");
        level--;
        write("}");
        write();
    }

    public void visitQualifiedThisReference(Java.QualifiedThisReference qtr) {
        if(instances.contains(qtr)) return;
        instances.add(qtr);
        
        write("private Java.QualifiedThisReference generateAtom"+getSuffix(qtr)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.QualifiedThisReference("+getLocation(qtr)+", scope, " +
                "generateType"+getSuffix(qtr.qualification)+"(scope).toType()" +
                ");");
        level--;
        write("}");
        write();

        qtr.qualification.visit((Visitor.TypeVisitor) this);
    }

    public void visitClassLiteral(Java.ClassLiteral cl) {
        if(instances.contains(cl)) return;
        instances.add(cl);
        
        write("private Java.ClassLiteral generateAtom"+getSuffix(cl)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ClassLiteral("+getLocation(cl)+", scope, " +
                "generateType"+getSuffix(cl.type)+"(scope).toType());");
        level--;
        write("}");
        write();

        cl.type.visit((Visitor.TypeVisitor) this);
    }

    public void visitConditionalExpression(Java.ConditionalExpression ce) {
        if(instances.contains(ce)) return;
        instances.add(ce);
        
        write("private Java.ConditionalExpression generateAtom"+getSuffix(ce)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ConditionalExpression("+getLocation(ce)+", " +
                "generateAtom"+getSuffix(ce.lhs)+"(scope), " +
                "generateAtom"+getSuffix(ce.mhs)+"(scope), " +
                "generateAtom"+getSuffix(ce.rhs)+"(scope));");
        level--;
        write("}");
        write();

        ce.lhs.visit((Visitor.RvalueVisitor) this);
        ce.mhs.visit((Visitor.RvalueVisitor) this);
        ce.rhs.visit((Visitor.RvalueVisitor) this);
    }

    public void visitCrement(Java.Crement c) {
        if(instances.contains(c)) return;
        instances.add(c);
        
        write("private Java.Crement generateAtom"+getSuffix(c)+"(Java.Scope scope) throws Exception {");
        level++;
        if(c.pre) {
            write("return new Java.Crement("+getLocation(c)+", \""+c.operator+"\", " +
                    "generateAtom"+getSuffix(c.operand)+"(scope)" +
                    ");");
        } else {
            write("return new Java.Crement("+getLocation(c)+", " +
                    "generateAtom"+getSuffix(c.operand)+"(scope), " +
                    "\""+c.operator+"\");");
        }
        level--;
        write("}");
        write();

        c.operand.visit((Visitor.LvalueVisitor) this);
    }

    public void visitArrayAccessExpression(Java.ArrayAccessExpression aae) {
        if(instances.contains(aae)) return;
        instances.add(aae);
        
        write("private Java.ArrayAccessExpression generateAtom"+getSuffix(aae)+"(Java.Scope scope) throws Exception {");
        level++;

        write("return new Java.ArrayAccessExpression("+getLocation(aae)+", " +
                "generateAtom"+getSuffix(aae.lhs)+"(scope), " +
                "generateAtom"+getSuffix(aae.index)+"(scope)" +
                ");");
        
        level--;
        write("}");
        write();

        aae.lhs.visit((Visitor.RvalueVisitor) this);
        aae.index.visit((Visitor.RvalueVisitor) this);
    }

    public void visitFieldAccessExpression(Java.FieldAccessExpression fae) {
        if(instances.contains(fae)) return;
        instances.add(fae);
        
        write("private Java.FieldAccessExpression generateAtom"+getSuffix(fae)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.FieldAccessExpression("+getLocation(fae)+", " +
                "generateAtom"+getSuffix(fae.lhs)+"(scope), " +
                "\""+fae.fieldName+"\");");
        level--;
        write("}");
        write();

        fae.lhs.visit(this);
    }

    public void visitUnaryOperation(Java.UnaryOperation uo) {
        if(instances.contains(uo)) return;
        instances.add(uo);
        
        write("private Java.UnaryOperation generateAtom"+getSuffix(uo)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.UnaryOperation("+getLocation(uo)+", \""+uo.operator+"\", " +
                "generateAtom"+getSuffix(uo.operand)+"(scope));");
        level--;
        write("}");
        write();

        uo.operand.visit((Visitor.RvalueVisitor) this);
    }

    public void visitInstanceof(Java.Instanceof io) {
        if(instances.contains(io)) return;
        instances.add(io);
        
        write("private Java.Instanceof generateAtom"+getSuffix(io)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.Instanceof("+getLocation(io)+", " +
                "generateAtom"+getSuffix(io.lhs)+"(scope), " +
                "generateType"+getSuffix(io.rhs)+"(scope));");
        level--;
        write("}");
        write();

        io.lhs.visit((Visitor.RvalueVisitor) this);
        io.rhs.visit((Visitor.TypeVisitor) this);
    }

    public void visitBinaryOperation(Java.BinaryOperation bo) {
        if(instances.contains(bo)) return;
        instances.add(bo);
        
        write("private Java.BinaryOperation generateAtom"+getSuffix(bo)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.BinaryOperation("+getLocation(bo)+ ", " +
            "generateAtom"+getSuffix(bo.lhs)+"(scope), " +
            "\""+bo.op+"\", " +
            "generateAtom"+getSuffix(bo.rhs)+"(scope)" +
            ");");
        level--;
        write("}");
        write();

        bo.lhs.visit((Visitor.RvalueVisitor) this);
        bo.rhs.visit((Visitor.RvalueVisitor) this);
    }

    public void visitCast(Java.Cast c) {
        if(instances.contains(c)) return;
        instances.add(c);
        
        write("private Java.Cast generateAtom"+getSuffix(c)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.Cast("+getLocation(c)+", " +
                "generateType"+getSuffix(c.targetType)+"(scope).toType(), " +
                "generateAtom"+getSuffix(c.value)+"(scope));");
        level--;
        write("}");
        write();

        c.targetType.visit((Visitor.TypeVisitor) this);
        c.value.visit((Visitor.RvalueVisitor) this);
    }

    public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi) {
        if(instances.contains(smi)) return;
        instances.add(smi);
        
        write("private Java.SuperclassMethodInvocation generateAtom"+getSuffix(smi)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.SuperclassMethodInvocation("+getLocation(smi)+", scope, \""+smi.methodName+"\", " +
                        getGenerateRvalues(smi.arguments, "scope")+");");
        level--;
        write("}");
        write();

        generateRvalues(smi.arguments);
    }

    public void visitParameterAccess(Java.ParameterAccess pa) {
        if(instances.contains(pa)) return;
        instances.add(pa);
        
        write("private Java.ParameterAccess generateAtom"+getSuffix(pa)+"(Java.Scope scope) throws Exception {");
        level++;
        // TODO resolve a correct functionDeclarator name
        Java.FunctionDeclarator d = pa.declaringFunction;
        write("final Java.FunctionDeclarator declarator = null;  // TODO "+d.getClass().getName()+" : "+d.toString());
        write("return new Java.ParameterAccess("+getLocation(pa)+", declarator, \""+pa.name+"\");");
        level--;
        write("}");
        write();
    }

    public void visitNewArray(Java.NewArray na) {
        if(instances.contains(na)) return;
        instances.add(na);
        
        write("private Java.NewArray generateAtom"+getSuffix(na)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.NewArray("+getLocation(na)+", " +
                "generateType"+getSuffix(na.type)+"(scope), " +
                getGenerateRvalues(na.dimExprs, "scope")+", "+na.dims+");");
        level--;
        write("}");
        write();

        na.type.visit((Visitor.TypeVisitor) this);
        generateRvalues(na.dimExprs);
    }

    public void visitLiteral(Java.Literal l) {
        if(instances.contains(l)) return;
        instances.add(l);
        
        write("private Java.Literal generateAtom"+getSuffix(l)+"(Java.Scope scope) throws Exception {");
        level++;

        String s = "null";
        Object v = l.value;
        if(v instanceof String) {
            s = "\""+ escape((String) v)+"\"";
        } else if(v instanceof Integer) {
            s = "new Integer("+v+")";
        } else if(v instanceof Long) {
            s = "new Long("+v+"L)";
        } else if(v instanceof Float) {
            s = "new Float("+v+"f)";
        } else if(v instanceof Double) {
            s = "new Double("+v+"d)";
        } else if(v instanceof Character) {
            s = "new Character(\'"+escape(((Character) v).charValue())+"\')";
        } else if(v instanceof Boolean) {
            s = ((Boolean) v).booleanValue() ? "Boolean.TRUE" : "Boolean.FALSE";
        }
        
        write("return new Java.Literal("+getLocation(l)+", "+s+");");
        level--;
        write("}");
        write();
    }

    public void visitConstantValue(Java.ConstantValue cv) {
        if(instances.contains(cv)) return;
        instances.add(cv);
        
        write("private Java.ConstantValue generateAtom"+getSuffix(cv)+"(Java.Scope scope) throws Exception {");
        level++;

        String s = "";
        Object v = cv.constantValue;
        if(v instanceof Integer) {
            s = "new Integer("+v+")";
        } else if(v instanceof Long) {
            s = "new Long("+v+")";
        } else if(v instanceof Float) {
            s = "new Float("+v+"f)";
        } else if(v instanceof Double) {
            s = "new Double("+v+"d)";
        } else if(v instanceof String) {
            s = "\""+v+"\"";
        } else if(v instanceof Character) {
            s = "new Character("+v+")";
        } else if(v instanceof Boolean) {
            s = "new Boolean("+v+")";
        } else if(v==Java.Rvalue.CONSTANT_VALUE_NULL) {
            s = "null";
        }

        write("return new Java.ConstantValue("+getLocation(cv)+", "+s+");");
        level--;
        write("}");
        write();
    }

    public void visitParenthesizedExpression(Java.ParenthesizedExpression pe) {
        if(instances.contains(pe)) return;
        instances.add(pe);
        
        write("private Java.ParenthesizedExpression generateAtom"+getSuffix(pe)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.ParenthesizedExpression("+getLocation(pe)+", " +
                "generateAtom"+getSuffix(pe.value)+"(scope));");
        level--;
        write("}");
        write();

        pe.value.visit((Visitor.RvalueVisitor) this);
    }

    
    // Helpers

    private void generateClassDeclarationBody(Java.ClassDeclaration cd) {
        for(Iterator it = cd.constructors.iterator(); it.hasNext();) {
            Java.ConstructorDeclarator cde = (Java.ConstructorDeclarator) it.next();
            write("declaration.addConstructor(generateConstructorDeclarator"+getSuffix(cde)+"(declaration));");
        }

        generateAbstractTypeDeclarationBody(cd);
        
        for(Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
            write("declaration.addVariableDeclaratorOrInitializer(generateFieldDeclaration"+getSuffix(tbd)+"(declaration));");
        }
    }
    
    private void generateClassDeclarationBodyMethods(Java.ClassDeclaration cd) {
        for(Iterator it = cd.constructors.iterator(); it.hasNext();) {
            ((Java.ConstructorDeclarator) it.next()).visit(this);
        }
        for(Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            ((Java.TypeBodyDeclaration) it.next()).visit(this);
        }

        generateAbstractTypeDeclarationBodyMethods(cd);
    }


    private void generateAbstractTypeDeclarationBody(Java.AbstractTypeDeclaration atd) {
        for(Iterator it = atd.declaredMethods.iterator(); it.hasNext();) {
            Java.MethodDeclarator md = (Java.MethodDeclarator) it.next();
            write("declaration.addDeclaredMethod(generateMethodDeclarator"+getSuffix(md)+"(declaration));");
        }
        
        for(Iterator it = atd.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
            Java.MemberTypeDeclaration mtd = (Java.MemberTypeDeclaration) it.next();
            write("declaration.addMemberTypeDeclaration(generateMemberTypeDeclaration"+getSuffix(mtd)+"(declaration));");
        }
    }

    private void generateAbstractTypeDeclarationBodyMethods(Java.AbstractTypeDeclaration atd) {
        for(Iterator it = atd.declaredMethods.iterator(); it.hasNext();) {
            ((Java.MethodDeclarator) it.next()).visit(this);
        }
        for(Iterator it = atd.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
            ((Java.MemberTypeDeclaration) it.next()).visit((Visitor.TypeBodyDeclarationVisitor) this);
        }
    }
    
    
    private void generateTypes(Java.Type[] types) {
        if(types==null || types.length==0) return;
        
        write("private Java.Type[] generateTypes"+getSuffix(types)+"(Java.Scope scope) throws Exception {");
        level++;
        write("return new Java.Type[] {");
        level++;
        level++;
        for(int i = 0; i < types.length; i++) {
            write("generateType"+getSuffix(types[i])+"(scope),");
        }
        level--;
        write("};");
        level--;
        level--;
        write("}");
        write();

        // generate methods
        for(int i = 0; i < types.length; i++) {
            types[i].visit((Visitor.TypeVisitor) this);
        }
    }

    private void generateFormalParameters(Java.FormalParameter[] parameters) {
        if(parameters==null || parameters.length==0) return;
        
        write("private Java.FormalParameter[] generateFormalParameters"+getSuffix(parameters)+"(Java.Scope scope) throws Exception {");
        level++;

        write("return new Java.FormalParameter[] {");
        level++;
        level++;
        for(int i = 0; i < parameters.length; i++) {
            write("generateFormalParameter"+getSuffix(parameters[i])+"(scope),");
        }
        level--;
        write("};");
        level--;
        level--;
        write("}");
        write();

        for(int i = 0; i < parameters.length; i++) {
            this.generateFormalParameter(parameters[i]);
        }
    }
    
    private void generateVariableDeclarators(Java.VariableDeclarator[] variables) {
        if(variables==null) return;
        
        write("private Java.VariableDeclarator[] generateVariableDeclarators"+getSuffix(variables)+"(Java.Scope scope) throws Exception {");
        level++;

        write("Java.VariableDeclarator[] variables = new Java.VariableDeclarator["+variables.length+"];");
        for(int i = 0; i < variables.length; ++i) {
            write("variables["+i+"] = generateVariableDeclarator"+getSuffix(variables[i])+"(scope);");
        }

        write("return variables;");
        level--;
        write("}");
        write();

        for(int i = 0; i < variables.length; ++i) {
            this.generateVariableDeclarator(variables[i]);
        }
    }

    private void generateSwitchBlockStatementGroup(Java.SwitchBlockStatementGroup sbsg) {
        write("private Java.SwitchBlockStatementGroup generateSwitchBlockStatementGroup"+getSuffix(sbsg)+"(Java.Block statement) throws Exception {");
        level++;

        write("Java.SwitchBlockStatementGroup group = new Java.SwitchBlockStatementGroup("+getLocation(sbsg)+");");

        for(Iterator it = sbsg.caseLabels.iterator(); it.hasNext();) {
            Java.Rvalue rv = (Java.Rvalue) it.next();
            write("group.addSwitchLabel(generateAtom"+getSuffix(rv)+"(statement));");
        }

        if(sbsg.hasDefaultLabel) {
            write("group.hasDefaultLabel = "+sbsg.hasDefaultLabel+";");
        }

        write("List blockStatements = new ArrayList();");
        for(Iterator it = sbsg.blockStatements.iterator(); it.hasNext();) {
            Java.BlockStatement bs = (Java.BlockStatement) it.next();
            write("blockStatements.add(generateStatement"+getSuffix(bs)+"(statement));");
        }
        write("group.setBlockStatements(blockStatements);");

        write("return group;");
        level--;
        write("}");
        write();
        
        //
        for(Iterator it = sbsg.caseLabels.iterator(); it.hasNext();) {
            ((Java.Rvalue) it.next()).visit((Visitor.RvalueVisitor) this);
        }
        for(Iterator it = sbsg.blockStatements.iterator(); it.hasNext();) {
            ((Java.BlockStatement) it.next()).visit(this);
        }
    }
    
    private void generateRvalues(Java.Rvalue[] values) {
        if(values==null || values.length==0) return;
        
        write("private Java.Rvalue[] generateRvalues"+getSuffix(values)+"(Java.Scope scope) throws Exception {");
        level++;
        
        write("return new Java.Rvalue[] {");
        level++;
        level++;
        for(int i = 0; i < values.length; i++) {
            write("generateAtom"+getSuffix(values[i])+"(scope),");
        }
        level--;

        write("};");
        level--;
        level--;
        write("}");
        write();
        
        for(int i = 0; i < values.length; i++) {
            values[i].visit((Visitor.RvalueVisitor) this);
        }
    }

    
    private void generateIClass(IClass type) {
        write("private IClass generateIClass"+getSuffix(type)+"() throws Exception {");
        level++;
        // TODO implement generation of iclass instance
        write("return null;");
        level--;
        write("}");
        write();
    }

    
    private static final short[] MODS = { Mod.PUBLIC, Mod.PRIVATE, Mod.PROTECTED, Mod.STATIC,
        Mod.FINAL, Mod.SUPER, Mod.SYNCHRONIZED, Mod.VOLATILE, Mod.TRANSIENT, Mod.NATIVE, Mod.INTERFACE, Mod.ABSTRACT,
        Mod.STRICTFP};

    private static final String[] MOD_NAMES = { "Mod.PUBLIC", "Mod.PRIVATE", "Mod.PROTECTED",
            "Mod.STATIC", "Mod.FINAL", "Mod.SUPER", "Mod.SYNCHRONIZED", "Mod.VOLATILE", "Mod.TRANSIENT", "Mod.NATIVE",
            "Mod.INTERFACE", "Mod.ABSTRACT", "Mod.STRICTFP"};
    
    private String getModifiers(short modifiers) {
        if(modifiers == 0) {
            return "Mod.NONE";
        }
    
        StringBuffer sb = new StringBuffer("(short)(");
        String sep = "";
        for(int i = 0; i < MODS.length; i++) {
            if((modifiers & MODS[i]) > 0) {
                sb.append(sep).append(MOD_NAMES[i]);
                sep = " | ";
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    private String arrayToString(String[] a) {
        StringBuffer sb = new StringBuffer("new String[] { ");
        String sep = "";
        for(int i = 0; i < a.length; i++) {
            sb.append(sep).append("\"" + a[i] + "\"");
            sep = ", ";
        }
        return sb.append("}").toString();
    }

    private String getLocation(Java.Locatable locatable) {
        Location l = locatable.getLocation();
        // return "new Scanner.Location(\""+l.getFileName()+"\",
        // (short)"+l.getLineNumber()+", (short)"+l.getColumnNumber()+")";
        return "getLocation("+l.getLineNumber() + ", " + l.getColumnNumber() + ")";
    }

    private void write(String s) {
        int n = level * TAB_SIZE;
        while(n >= TAB_FILLER.length()) {
            pw.print(TAB_FILLER);
            n -= TAB_FILLER.length();
        }
        if(n>0) {
            pw.print(TAB_FILLER.substring(0, n));
        }

        pw.println(s);
    }

    private void write() {
        pw.println();
    }
    
    
    private String escape(String s) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i<s.length(); i++) {
            sb.append(escape(s.charAt(i)));
        }
        return sb.toString();
    }

    private String escape(char c) {
        switch(c) {
            case '\"': return "\\\"";
            case '\'': return "\\\'";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            case '\\': return "\\\\";
            // TODO add the rest of escapes
            // TODO escape unicodes
            default: return String.valueOf(c);
        }
    }

    private String getSuffix(Object o) {
        return String.valueOf(System.identityHashCode(o));
    }

    private String getGenerateTypes(Java.Type[] types, String scope) {
        return (types==null ? "null" : 
            (types.length==0 ? "new Java.Type[0]" : "generateTypes"+getSuffix(types)+"("+scope+")"));
    }
    
    private String getGenerateRvalues(Java.Rvalue[] values, String scope) {
        return (values==null ? "null" : 
            (values.length==0 ? "new Java.Rvalue[0]" : "generateRvalues"+getSuffix(values)+"("+scope+")"));
    }
    
}

