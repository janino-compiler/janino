
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AccessModifier;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnnotationTypeDeclaration;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
import org.codehaus.janino.Java.ArrayCreationReference;
import org.codehaus.janino.Java.ArrayInitializer;
import org.codehaus.janino.Java.ArrayInitializerOrRvalue;
import org.codehaus.janino.Java.ArrayLength;
import org.codehaus.janino.Java.ArrayType;
import org.codehaus.janino.Java.AssertStatement;
import org.codehaus.janino.Java.Assignment;
import org.codehaus.janino.Java.Atom;
import org.codehaus.janino.Java.BinaryOperation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockLambdaBody;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CatchParameter;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassInstanceCreationReference;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.DocCommentable;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.EnumDeclaration;
import org.codehaus.janino.Java.ExportsModuleDirective;
import org.codehaus.janino.Java.ExpressionLambdaBody;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FormalLambdaParameters;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.IdentifierLambdaParameters;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.InferredLambdaParameters;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaBody;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.LambdaParameters;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Lvalue;
import org.codehaus.janino.Java.MarkerAnnotation;
import org.codehaus.janino.Java.MemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberEnumDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.MethodReference;
import org.codehaus.janino.Java.Modifier;
import org.codehaus.janino.Java.ModularCompilationUnit;
import org.codehaus.janino.Java.ModuleDeclaration;
import org.codehaus.janino.Java.ModuleDirective;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.OpensModuleDirective;
import org.codehaus.janino.Java.Package;
import org.codehaus.janino.Java.PackageDeclaration;
import org.codehaus.janino.Java.PackageMemberAnnotationTypeDeclaration;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberEnumDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.ProvidesModuleDirective;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.RequiresModuleDirective;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.TextBlock;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.TryStatement.LocalVariableDeclaratorResource;
import org.codehaus.janino.Java.TryStatement.VariableAccessResource;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.UsesModuleDirective;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Visitor.AbstractCompilationUnitVisitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.ArrayInitializerOrRvalueVisitor;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.FunctionDeclaratorVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.Visitor.LambdaBodyVisitor;
import org.codehaus.janino.Visitor.LambdaParametersVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.ModifierVisitor;
import org.codehaus.janino.Visitor.ModuleDirectiveVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.Visitor.TryStatementResourceVisitor;
import org.codehaus.janino.Visitor.TypeBodyDeclarationVisitor;
import org.codehaus.janino.Visitor.TypeDeclarationVisitor;
import org.codehaus.janino.Visitor.TypeVisitor;
import org.codehaus.janino.util.AutoIndentWriter;

/**
 * Unparses (un-compiles) an AST to a {@link Writer}. See {@link #main(String[])} for a usage example.
 */
public
class Unparser implements AutoCloseable {

    private final AbstractCompilationUnitVisitor<Void, RuntimeException>
    compilationUnitUnparser = new AbstractCompilationUnitVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitCompilationUnit(CompilationUnit cu) {

            PackageDeclaration opd = cu.packageDeclaration;
            if (opd != null) {
                Unparser.this.pw.println();
                Unparser.this.pw.println("package " + opd.packageName + ';');
            }

            if (cu.importDeclarations.length > 0) {
                Unparser.this.pw.println();
                for (AbstractCompilationUnit.ImportDeclaration id : cu.importDeclarations) {
                    id.accept(Unparser.this.importUnparser);
                }
            }

            for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
                Unparser.this.pw.println();
                Unparser.this.unparseTypeDeclaration(pmtd);
                Unparser.this.pw.println();
            }

            return null;
        }

        @Override @Nullable public Void
        visitModularCompilationUnit(ModularCompilationUnit mcu) {

            if (mcu.importDeclarations.length > 0) {
                Unparser.this.pw.println();
                for (AbstractCompilationUnit.ImportDeclaration id : mcu.importDeclarations) {
                    id.accept(Unparser.this.importUnparser);
                }
            }

            ModuleDeclaration md = mcu.moduleDeclaration;
            Unparser.this.unparseModifiers(md.modifiers);
            Unparser.this.pw.print(md.isOpen ? "open module " : "module ");
            Unparser.this.pw.print(Java.join(md.moduleName, "."));
            Unparser.this.pw.print("(");
            switch (md.moduleDirectives.length) {
            case 0:
                ;
                break;
            case 1:
                md.moduleDirectives[0].accept(Unparser.this.moduleDirectiveUnparser);
                break;
            default:
                Unparser.this.pw.println();
                Unparser.this.pw.print(AutoIndentWriter.INDENT);
                for (ModuleDirective mdir : md.moduleDirectives) {
                    mdir.accept(Unparser.this.moduleDirectiveUnparser);
                    Unparser.this.pw.println();
                }
                Unparser.this.pw.print(AutoIndentWriter.UNINDENT);
            }
            Unparser.this.pw.print(")");

            return null;
        }
    };

    private final ModuleDirectiveVisitor<Void, RuntimeException>
    moduleDirectiveUnparser = new ModuleDirectiveVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitRequiresModuleDirective(RequiresModuleDirective rmd) {
            Unparser.this.pw.print("requires ");
            Unparser.this.unparseModifiers(rmd.requiresModifiers);
            Unparser.this.pw.print(Java.join(rmd.moduleName, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitExportsModuleDirective(ExportsModuleDirective emd) {
            Unparser.this.pw.print("exports " + Java.join(emd.packageName, "."));
            if (emd.toModuleNames != null) {
                Unparser.this.pw.print(" to " + Java.join(emd.toModuleNames, ".", ", "));
            }
            Unparser.this.pw.print(";");
            return null;
        }

        @Override @Nullable public Void
        visitOpensModuleDirective(OpensModuleDirective omd) {
            Unparser.this.pw.print("opens " + Java.join(omd.packageName, "."));
            if (omd.toModuleNames != null) {
                Unparser.this.pw.print(" to " + Java.join(omd.toModuleNames, ".", ", "));
            }
            Unparser.this.pw.print(";");
            return null;
        }

        @Override @Nullable public Void
        visitUsesModuleDirective(UsesModuleDirective umd) {
            Unparser.this.pw.print("uses " + Java.join(umd.typeName, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitProvidesModuleDirective(ProvidesModuleDirective pmd) {
            Unparser.this.pw.print((
                "provides "
                + Java.join(pmd.typeName, ".")
                + " with "
                + Java.join(pmd.withTypeNames, ".", ", ")
                + ";"
            ));
            return null;
        }

    };

    private final ImportVisitor<Void, RuntimeException>
    importUnparser = new ImportVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitSingleTypeImportDeclaration(AbstractCompilationUnit.SingleTypeImportDeclaration stid) {
            Unparser.this.pw.println("import " + Java.join(stid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitTypeImportOnDemandDeclaration(AbstractCompilationUnit.TypeImportOnDemandDeclaration tiodd) {
            Unparser.this.pw.println("import " + Java.join(tiodd.identifiers, ".") + ".*;");
            return null;
        }

        @Override @Nullable public Void
        visitSingleStaticImportDeclaration(AbstractCompilationUnit.SingleStaticImportDeclaration ssid) {
            Unparser.this.pw.println("import static " + Java.join(ssid.identifiers, ".") + ';');
            return null;
        }

        @Override @Nullable public Void
        visitStaticImportOnDemandDeclaration(AbstractCompilationUnit.StaticImportOnDemandDeclaration siodd) {
            Unparser.this.pw.println("import static " + Java.join(siodd.identifiers, ".") + ".*;");
            return null;
        }
    };

    private final TypeDeclarationVisitor<Void, RuntimeException>
    typeDeclarationUnparser = new TypeDeclarationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitLocalClassDeclaration(LocalClassDeclaration lcd) {
            Unparser.this.unparseNamedClassDeclaration(lcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd) {
            Unparser.this.unparseNamedClassDeclaration(pmcd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid) {
            Unparser.this.unparseInterfaceDeclaration(pmid);
            return null;
        }

        @Override @Nullable public Void
        visitAnonymousClassDeclaration(AnonymousClassDeclaration acd) {
            Unparser.this.unparseType(acd.baseType);
            Unparser.this.pw.println(" {");
            Unparser.this.pw.print(AutoIndentWriter.INDENT);
            Unparser.this.unparseClassDeclarationBody(acd);
            Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitEnumConstant(EnumConstant ec) {

            Unparser.this.unparseAnnotations(ec.getAnnotations());

            Unparser.this.pw.append(ec.name);

            if (ec.arguments != null) {
                Unparser.this.unparseFunctionInvocationArguments(ec.arguments);
            }

            if (!Unparser.classDeclarationBodyIsEmpty(ec)) {
                Unparser.this.pw.println(" {");
                Unparser.this.pw.print(AutoIndentWriter.INDENT);
                Unparser.this.unparseClassDeclarationBody(ec);
                Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            }

            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) {
            Unparser.this.unparseEnumDeclaration(pmed);
            return null;
        }

        @Override @Nullable public Void
        visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) {
            Unparser.this.unparseAnnotationTypeDeclaration(matd);
            return null;
        }

        @Override @Nullable public Void
        visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) {
            Unparser.this.unparseAnnotationTypeDeclaration(pmatd);
            return null;
        }

        @Override @Nullable public Void
        visitMemberEnumDeclaration(MemberEnumDeclaration med) {
            Unparser.this.unparseEnumDeclaration(med);
            return null;
        }

        @Override @Nullable public Void
        visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) {
            Unparser.this.unparseInterfaceDeclaration(mid);
            return null;
        }

        @Override @Nullable public Void
        visitMemberClassDeclaration(MemberClassDeclaration mcd) {
            Unparser.this.unparseNamedClassDeclaration(mcd);
            return null;
        }
    };

    private final TypeBodyDeclarationVisitor<Void, RuntimeException>
    typeBodyDeclarationUnparser = new TypeBodyDeclarationVisitor<Void, RuntimeException>() {
        @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                      { Unparser.this.unparseEnumDeclaration(med);            return null; }
        @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                    { Unparser.this.unparseNamedClassDeclaration(mcd);      return null; }
        @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)            { Unparser.this.unparseInterfaceDeclaration(mid);       return null; }
        @Override @Nullable public Void visitFunctionDeclarator(FunctionDeclarator fd)                             { Unparser.this.unparseFunctionDeclarator(fd);          return null; }
        @Override @Nullable public Void visitInitializer(Initializer i)                                            { Unparser.this.unparseInitializer(i);                  return null; }
        @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                 { Unparser.this.unparseFieldDeclaration(fd);            return null; }
        @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) { Unparser.this.unparseAnnotationTypeDeclaration(matd); return null; }
    };

    private final BlockStatementVisitor<Void, RuntimeException>
    blockStatementUnparser = new BlockStatementVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitFieldDeclaration(FieldDeclaration fd) { Unparser.this.unparseFieldDeclaration(fd); return null; }

        @Override @Nullable public Void
        visitInitializer(Initializer i) { Unparser.this.unparseInitializer(i); return null; }

        @Override @Nullable public Void
        visitBlock(Block b) {
            Unparser.this.unparseBlock(b);
            return null;
        }

        @Override @Nullable public Void
        visitBreakStatement(BreakStatement bs) {
            Unparser.this.pw.print("break");
            if (bs.label != null) Unparser.this.pw.print(' ' + bs.label);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitContinueStatement(ContinueStatement cs) {
            Unparser.this.pw.print("continue");
            if (cs.label != null) Unparser.this.pw.print(' ' + cs.label);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitAssertStatement(AssertStatement as) {

            Unparser.this.pw.print("assert ");
            Unparser.this.unparseAtom(as.expression1);

            Rvalue oe2 = as.expression2;
            if (oe2 != null) {
                Unparser.this.pw.print(" : ");
                Unparser.this.unparseAtom(oe2);
            }
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitDoStatement(DoStatement ds) {
            Unparser.this.pw.print("do ");
            Unparser.this.unparseBlockStatement(ds.body);
            Unparser.this.pw.print("while (");
            Unparser.this.unparseAtom(ds.condition);
            Unparser.this.pw.print(");");
            return null;
        }

        @Override @Nullable public Void
        visitEmptyStatement(EmptyStatement es) {
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitExpressionStatement(ExpressionStatement es) {
            Unparser.this.unparseAtom(es.rvalue);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitForStatement(ForStatement fs) {
            Unparser.this.pw.print("for (");
            if (fs.init != null) {
                Unparser.this.unparseBlockStatement(fs.init);
            } else {
                Unparser.this.pw.print(';');
            }

            Rvalue oc = fs.condition;
            if (oc != null) {
                Unparser.this.pw.print(' ');
                Unparser.this.unparseAtom(oc);
            }

            Unparser.this.pw.print(';');

            Rvalue[] ou = fs.update;
            if (ou != null) {
                Unparser.this.pw.print(' ');
                for (int i = 0; i < ou.length; ++i) {
                    if (i > 0) Unparser.this.pw.print(", ");
                    Unparser.this.unparseAtom(ou[i]);
                }
            }

            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(fs.body);
            return null;
        }

        @Override @Nullable public Void
        visitForEachStatement(ForEachStatement fes) {
            Unparser.this.pw.print("for (");
            Unparser.this.unparseFormalParameter(fes.currentElement, false);
            Unparser.this.pw.print(" : ");
            Unparser.this.unparseAtom(fes.expression);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(fes.body);
            return null;
        }

        @Override @Nullable public Void
        visitIfStatement(IfStatement is) {
            Unparser.this.pw.print("if (");
            Unparser.this.unparseAtom(is.condition);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(is.thenStatement);

            BlockStatement es = is.elseStatement;
            if (es != null) {
                Unparser.this.pw.println(" else");
                Unparser.this.unparseBlockStatement(es);
            }

            return null;
        }

        @Override @Nullable public Void
        visitLabeledStatement(LabeledStatement ls) {
            Unparser.this.pw.println(ls.label + ':');
            Unparser.this.unparseBlockStatement(ls.body);
            return null;
        }

        @Override @Nullable public Void
        visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) {
            Unparser.this.unparseTypeDeclaration(lcds.lcd);
            return null;
        }

        @Override @Nullable public Void
        visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) {
            Unparser.this.unparseModifiers(lvds.modifiers);
            Unparser.this.unparseType(lvds.type);
            Unparser.this.pw.print(' ');
            Unparser.this.pw.print(AutoIndentWriter.TABULATOR);
            Unparser.this.unparseVariableDeclarators(lvds.variableDeclarators);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitReturnStatement(ReturnStatement rs) {

            Unparser.this.pw.print("return");

            Rvalue orv = rs.returnValue;
            if (orv != null) {
                Unparser.this.pw.print(' ');
                Unparser.this.unparseAtom(orv);
            }

            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitSwitchStatement(SwitchStatement ss) {
            Unparser.this.pw.print("switch (");
            Unparser.this.unparseAtom(ss.condition);
            Unparser.this.pw.println(") {");
            for (SwitchStatement.SwitchBlockStatementGroup sbsg : ss.sbsgs) {
                Unparser.this.pw.print(AutoIndentWriter.UNINDENT);
                try {
                    for (Rvalue rv : sbsg.caseLabels) {
                        Unparser.this.pw.print("case ");
                        Unparser.this.unparseAtom(rv);
                        Unparser.this.pw.println(':');
                    }
                    if (sbsg.hasDefaultLabel) Unparser.this.pw.println("default:");
                } finally {
                    Unparser.this.pw.print(AutoIndentWriter.INDENT);
                }
                for (BlockStatement bs : sbsg.blockStatements) {
                    Unparser.this.unparseBlockStatement(bs);
                    Unparser.this.pw.println();
                }
            }
            Unparser.this.pw.print('}');
            return null;
        }

        @Override @Nullable public Void
        visitSynchronizedStatement(SynchronizedStatement ss) {
            Unparser.this.pw.print("synchronized (");
            Unparser.this.unparseAtom(ss.expression);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(ss.body);
            return null;
        }

        @Override @Nullable public Void
        visitThrowStatement(ThrowStatement ts) {
            Unparser.this.pw.print("throw ");
            Unparser.this.unparseAtom(ts.expression);
            Unparser.this.pw.print(';');
            return null;
        }

        @Override @Nullable public Void
        visitTryStatement(TryStatement ts) {
            Unparser.this.pw.print("try ");
            if (!ts.resources.isEmpty()) {
                Unparser.this.pw.print("(");
                Unparser.this.unparseResources(
                    (TryStatement.Resource[]) ts.resources.toArray(new TryStatement.Resource[ts.resources.size()])
                );
                Unparser.this.pw.print(") ");
            }
            Unparser.this.unparseBlockStatement(ts.body);
            for (CatchClause cc : ts.catchClauses) {
                Unparser.this.pw.print(" catch (");
                Unparser.this.unparseCatchParameter(cc.catchParameter);
                Unparser.this.pw.print(") ");
                Unparser.this.unparseBlockStatement(cc.body);
            }

            Block f = ts.finallY;
            if (f != null) {
                Unparser.this.pw.print(" finally ");
                Unparser.this.unparseBlockStatement(f);
            }

            return null;
        }

        @Override @Nullable public Void
        visitWhileStatement(WhileStatement ws) {
            Unparser.this.pw.print("while (");
            Unparser.this.unparseAtom(ws.condition);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseBlockStatement(ws.body);
            return null;
        }

        @Override @Nullable public Void
        visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) {
            Unparser.this.pw.print("this");
            Unparser.this.unparseFunctionInvocationArguments(aci.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitSuperConstructorInvocation(SuperConstructorInvocation sci) {
            if (sci.qualification != null) {
                Unparser.this.unparseLhs(sci.qualification, ".");
                Unparser.this.pw.print('.');
            }
            Unparser.this.pw.print("super");
            Unparser.this.unparseFunctionInvocationArguments(sci.arguments);
            return null;
        }
    };

    private final AtomVisitor<Void, RuntimeException>
    atomUnparser = new AtomVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitType(Type t) {
            Unparser.this.unparseType(t);
            return null;
        }

        @Override @Nullable public Void
        visitPackage(Package p) {
            Unparser.this.pw.print(p.toString());
            return null;
        }

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) {
            Unparser.this.unparseRvalue(rv);
            return null;
        }

        @Override @Nullable public Void
        visitConstructorInvocation(ConstructorInvocation ci) {
            Unparser.this.unparseBlockStatement(ci);
            Unparser.this.pw.println(';');
            return null;
        }
    };

    private final TypeVisitor<Void, RuntimeException>
    typeUnparser = new TypeVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitArrayType(ArrayType at) {
            Unparser.this.unparseType(at.componentType);
            Unparser.this.pw.print("[]");
            return null;
        }

        @Override @Nullable public Void
        visitPrimitiveType(PrimitiveType bt) {
            Unparser.this.pw.print(bt.toString());
            return null;
        }

        @Override @Nullable public Void
        visitReferenceType(ReferenceType rt) {
            Unparser.this.unparseAnnotations(rt.annotations);
            Unparser.this.pw.print(rt.toString());
            return null;
        }

        @Override @Nullable public Void
        visitRvalueMemberType(RvalueMemberType rmt) {
            Unparser.this.pw.print(rmt.toString());
            return null;
        }

        @Override @Nullable public Void
        visitSimpleType(SimpleType st) {
            Unparser.this.pw.print(st.toString());
            return null;
        }
    };

    private final ArrayInitializerOrRvalueVisitor<Void, RuntimeException>
    arrayInitializerOrRvalueUnparser = new ArrayInitializerOrRvalueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rvalue) throws RuntimeException {
            Unparser.this.unparseAtom(rvalue);
            return null;
        }

        @Override @Nullable public Void
        visitArrayInitializer(ArrayInitializer ai) throws RuntimeException {
            if (ai.values.length == 0) {
                Unparser.this.pw.print("{}");
            } else
            {
                Unparser.this.pw.print("{ ");
                Unparser.this.unparseArrayInitializerOrRvalue(ai.values[0]);
                for (int i = 1; i < ai.values.length; ++i) {
                    Unparser.this.pw.print(", ");
                    Unparser.this.unparseArrayInitializerOrRvalue(ai.values[i]);
                }
                Unparser.this.pw.print(" }");
            }
            return null;
        }
    };

    private final RvalueVisitor<Void, RuntimeException>
    rvalueUnparser = new RvalueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitLvalue(Lvalue lv) {
            Unparser.this.unparseLvalue(lv);
            return null;
        }

        @Override @Nullable public Void
        visitMethodInvocation(MethodInvocation mi) {
            if (mi.target != null) {
                Unparser.this.unparseLhs(mi.target, ".");
                Unparser.this.pw.print('.');
            }
            Unparser.this.pw.print(mi.methodName);
            Unparser.this.unparseFunctionInvocationArguments(mi.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitNewClassInstance(NewClassInstance nci) {
            if (nci.qualification != null) {
                Unparser.this.unparseLhs(nci.qualification, ".");
                Unparser.this.pw.print('.');
            }
            assert nci.type != null;
            Unparser.this.pw.print("new " + nci.type.toString());
            Unparser.this.unparseFunctionInvocationArguments(nci.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitAssignment(Assignment a) {
            Unparser.this.unparseLhs(a.lhs, a.operator);
            Unparser.this.pw.print(' ' + a.operator + ' ');
            Unparser.this.unparseRhs(a.rhs, a.operator);
            return null;
        }

        @Override @Nullable public Void
        visitArrayLength(ArrayLength al) {
            Unparser.this.unparseLhs(al.lhs, ".");
            Unparser.this.pw.print(".length");
            return null;
        }

        @Override @Nullable public Void
        visitBinaryOperation(BinaryOperation bo) {
            Unparser.this.unparseLhs(bo.lhs, bo.operator);
            Unparser.this.pw.print(' ' + bo.operator + ' ');
            Unparser.this.unparseRhs(bo.rhs, bo.operator);
            return null;
        }

        @Override @Nullable public Void
        visitCast(Cast c) {
            Unparser.this.pw.print('(');
            Unparser.this.unparseType(c.targetType);
            Unparser.this.pw.print(") ");
            Unparser.this.unparseRhs(c.value, "cast");
            return null;
        }

        @Override @Nullable public Void
        visitClassLiteral(ClassLiteral cl) {
            Unparser.this.unparseType(cl.type);
            Unparser.this.pw.print(".class");
            return null;
        }

        @Override @Nullable public Void
        visitConditionalExpression(ConditionalExpression ce) {
            Unparser.this.unparseLhs(ce.lhs, "?:");
            Unparser.this.pw.print(" ? ");
            Unparser.this.unparseLhs(ce.mhs, "?:");
            Unparser.this.pw.print(" : ");
            Unparser.this.unparseRhs(ce.rhs, "?:");
            return null;
        }

        @Override @Nullable public Void
        visitCrement(Crement c) {
            if (c.pre) {
                Unparser.this.pw.print(c.operator);
                Unparser.this.unparseUnaryOperation(c.operand, c.operator + "x");
            } else
            {
                Unparser.this.unparseUnaryOperation(c.operand, "x" + c.operator);
                Unparser.this.pw.print(c.operator);
            }
            return null;
        }

        @Override @Nullable public Void
        visitInstanceof(Instanceof io) {
            Unparser.this.unparseLhs(io.lhs, "instanceof");
            Unparser.this.pw.print(" instanceof ");
            Unparser.this.unparseType(io.rhs);
            return null;
        }

        @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)              { Unparser.this.pw.print(il.value);             return null; }
        @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl) { Unparser.this.pw.print(fpl.value);            return null; }
        @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)              { Unparser.this.pw.print(bl.value);             return null; }
        @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)          { Unparser.this.pw.print(cl.value);             return null; }
        @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                { Unparser.this.pw.print(sl.value);             return null; }
        @Override @Nullable public Void visitTextBlock(TextBlock tb)                        { Unparser.this.pw.print(tb.value);             return null; }
        @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                    { Unparser.this.pw.print(nl.value);             return null; }
        @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)              { Unparser.this.pw.print("[" + sl.value + ']'); return null; }

        @Override @Nullable public Void
        visitNewArray(NewArray na) {
            Unparser.this.pw.print("new ");
            Unparser.this.unparseType(na.type);
            for (Rvalue dimExpr : na.dimExprs) {
                Unparser.this.pw.print('[');
                Unparser.this.unparseAtom(dimExpr);
                Unparser.this.pw.print(']');
            }
            for (int i = 0; i < na.dims; ++i) {
                Unparser.this.pw.print("[]");
            }
            return null;
        }

        @Override @Nullable public Void
        visitNewInitializedArray(NewInitializedArray nai) {

            Unparser.this.pw.print("new ");

            ArrayType at = nai.arrayType;
            assert at != null;
            Unparser.this.unparseType(at);

            Unparser.this.pw.print(" ");

            Unparser.this.unparseArrayInitializerOrRvalue(nai.arrayInitializer);

            return null;
        }

        @Override @Nullable public Void
        visitParameterAccess(ParameterAccess pa) {
            Unparser.this.pw.print(pa.toString());
            return null;
        }

        @Override @Nullable public Void
        visitQualifiedThisReference(QualifiedThisReference qtr) {
            Unparser.this.unparseType(qtr.qualification);
            Unparser.this.pw.print(".this");
            return null;
        }

        @Override @Nullable public Void
        visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) {
            Unparser.this.pw.print("super." + smi.methodName);
            Unparser.this.unparseFunctionInvocationArguments(smi.arguments);
            return null;
        }

        @Override @Nullable public Void
        visitThisReference(ThisReference tr) { Unparser.this.pw.print("this"); return null; }

        @Override @Nullable public Void
        visitLambdaExpression(LambdaExpression le) {
            Unparser.this.unparseLambdaParameters(le.parameters);
            Unparser.this.pw.print(" -> ");
            Unparser.this.unparseLambdaBody(le.body);
            return null;
        }

        @Override @Nullable public Void
        visitUnaryOperation(UnaryOperation uo) {
            Unparser.this.pw.print(uo.operator);
            Unparser.this.unparseUnaryOperation(uo.operand, uo.operator + "x");
            return null;
        }

        @Override @Nullable public Void
        visitNewAnonymousClassInstance(NewAnonymousClassInstance naci) {
            if (naci.qualification != null) {
                Unparser.this.unparseLhs(naci.qualification, ".");
                Unparser.this.pw.print('.');
            }
            Unparser.this.pw.print("new " + naci.anonymousClassDeclaration.baseType.toString() + '(');
            for (int i = 0; i < naci.arguments.length; ++i) {
                if (i > 0) Unparser.this.pw.print(", ");
                Unparser.this.unparseAtom(naci.arguments[i]);
            }
            Unparser.this.pw.println(") {");
            Unparser.this.pw.print(AutoIndentWriter.INDENT);
            Unparser.this.unparseClassDeclarationBody(naci.anonymousClassDeclaration);
            Unparser.this.pw.print(AutoIndentWriter.UNINDENT + "}");
            return null;
        }

        @Override @Nullable public Void
        visitMethodReference(MethodReference mr) {
            Unparser.this.pw.print(mr.toString());
            return null;
        }

        @Override @Nullable public Void
        visitInstanceCreationReference(ClassInstanceCreationReference cicr) {
            Unparser.this.pw.print(cicr.toString());
            return null;
        }

        @Override @Nullable public Void
        visitArrayCreationReference(ArrayCreationReference acr) {
            Unparser.this.pw.print(acr.toString());
            return null;
        }
    };

    private final LvalueVisitor<Void, RuntimeException>
    lvalueUnparser = new LvalueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitAmbiguousName(AmbiguousName an) {
            Unparser.this.pw.print(an.toString());
            return null;
        }

        @Override @Nullable public Void
        visitArrayAccessExpression(ArrayAccessExpression aae) {
            Unparser.this.unparseLhs(aae.lhs, "[ ]");
            Unparser.this.pw.print('[');
            Unparser.this.unparseAtom(aae.index);
            Unparser.this.pw.print(']');
            return null;
        }

        @Override @Nullable public Void
        visitFieldAccess(FieldAccess fa) {
            Unparser.this.unparseLhs(fa.lhs, ".");
            Unparser.this.pw.print('.' + fa.field.getName());
            return null;
        }

        @Override @Nullable public Void
        visitFieldAccessExpression(FieldAccessExpression fae) {
            Unparser.this.unparseLhs(fae.lhs, ".");
            Unparser.this.pw.print('.' + fae.fieldName);
            return null;
        }

        @Override @Nullable public Void
        visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) {
            if (scfae.qualification != null) {
                Unparser.this.unparseType(scfae.qualification);
                Unparser.this.pw.print(".super." + scfae.fieldName);
            } else
            {
                Unparser.this.pw.print("super." + scfae.fieldName);
            }
            return null;
        }

        @Override @Nullable public Void
        visitLocalVariableAccess(LocalVariableAccess lva) {
            Unparser.this.pw.print(lva.toString());
            return null;
        }

        @Override @Nullable public Void
        visitParenthesizedExpression(ParenthesizedExpression pe) {
            Unparser.this.pw.print('(');
            Unparser.this.unparseAtom(pe.value);
            Unparser.this.pw.print(')');
            return null;
        }
    };

    private final ElementValueVisitor<Void, RuntimeException>
    elementValueUnparser = new ElementValueVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitRvalue(Rvalue rv) throws RuntimeException {
            rv.accept(Unparser.this.atomUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitAnnotation(Annotation a) {
            a.accept(Unparser.this.annotationUnparser);
            return null;
        }

        @Override @Nullable public Void
        visitElementValueArrayInitializer(ElementValueArrayInitializer evai) {
            if (evai.elementValues.length == 0) {
                Unparser.this.pw.append("{}");
                return null;
            }

            Unparser.this.pw.append("{ ");
            evai.elementValues[0].accept(this);
            for (int i = 1; i < evai.elementValues.length; i++) {
                Unparser.this.pw.append(", ");
                evai.elementValues[i].accept(this);
            }
            Unparser.this.pw.append(" }");
            return null;
        }
    };

    private final AnnotationVisitor<Void, RuntimeException>
    annotationUnparser = new AnnotationVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitMarkerAnnotation(MarkerAnnotation ma) {
            Unparser.this.pw.append('@').append(ma.type.toString()).append(' ');
            return null;
        }

        @Override @Nullable public Void
        visitNormalAnnotation(NormalAnnotation na) {
            Unparser.this.pw.append('@').append(na.type.toString()).append('(');
            for (int i = 0; i < na.elementValuePairs.length; i++) {
                ElementValuePair evp = na.elementValuePairs[i];

                if (i > 0) Unparser.this.pw.print(", ");

                Unparser.this.pw.append(evp.identifier).append(" = ");

                evp.elementValue.accept(Unparser.this.elementValueUnparser);
            }
            Unparser.this.pw.append(") ");
            return null;
        }

        @Override @Nullable public Void
        visitSingleElementAnnotation(SingleElementAnnotation sea) {
            Unparser.this.pw.append('@').append(sea.type.toString()).append('(');
            sea.elementValue.accept(Unparser.this.elementValueUnparser);
            Unparser.this.pw.append(") ");
            return null;
        }
    };

    private final ModifierVisitor<Void, RuntimeException>
    modifierUnparser = new ModifierVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitAccessModifier(AccessModifier am) {
            Unparser.this.pw.print(am.toString() + ' ');
            return null;
        }

        @Override @Nullable public Void
        visitSingleElementAnnotation(SingleElementAnnotation sea) {
            return (Void) sea.accept(Unparser.this.annotationUnparser);
        }

        @Override @Nullable public Void
        visitNormalAnnotation(NormalAnnotation na) { return (Void) na.accept(Unparser.this.annotationUnparser);  }

        @Override @Nullable public Void
        visitMarkerAnnotation(MarkerAnnotation ma) { return (Void) ma.accept(Unparser.this.annotationUnparser);  }
    };

    private final LambdaParametersVisitor<Void, RuntimeException>
    lambdaParametersUnparser = new LambdaParametersVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitIdentifierLambdaParameters(IdentifierLambdaParameters ilp) {
            Unparser.this.pw.print(ilp.identifier);
            return null;
        }

        @Override @Nullable public Void
        visitFormalLambdaParameters(FormalLambdaParameters flp) {
            Unparser.this.unparseFormalParameters(flp.formalParameters);
            return null;
        }

        @Override @Nullable public Void
        visitInferredLambdaParameters(InferredLambdaParameters ilp) {
            Unparser.this.pw.print(ilp.names[0]);
            for (int i = 1; i < ilp.names.length; i++) Unparser.this.pw.print(", " + ilp.names[i]);
            return null;
        }
    };

    private final LambdaBodyVisitor<Void, RuntimeException>
    lambdaBodyUnparser = new LambdaBodyVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitBlockLambdaBody(BlockLambdaBody blb) {
            Unparser.this.unparseBlock(blb.block);
            return null;
        }

        @Override @Nullable public Void
        visitExpressionLambdaBody(ExpressionLambdaBody elb) {
            Unparser.this.unparse(elb.expression, true);
            return null;
        }
    };

    private final FunctionDeclaratorVisitor<Void, RuntimeException>
    functionDeclaratorUnparser = new FunctionDeclaratorVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitConstructorDeclarator(ConstructorDeclarator cd) {
            Unparser.this.unparseConstructorDeclarator(cd);
            return null;
        }

        @Override @Nullable public Void
        visitMethodDeclarator(MethodDeclarator md) {
            Unparser.this.unparseMethodDeclarator(md);
            return null;
        }
    };

    private final TryStatementResourceVisitor<Void, RuntimeException>
    resourceUnparser = new TryStatementResourceVisitor<Void, RuntimeException>() {

        @Override @Nullable public Void
        visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) {
            Unparser.this.unparseModifiers(lvdr.modifiers);
            Unparser.this.unparseType(lvdr.type);
            Unparser.this.pw.print(' ');
            Unparser.this.unparseVariableDeclarator(lvdr.variableDeclarator);
            return null;
        }

        @Override @Nullable public Void
        visitVariableAccessResource(VariableAccessResource var) {
            Unparser.this.unparseAtom(var.variableAccess);
            return null;
        }
    };

    private void
    unparseInitializer(Initializer i) {
        this.unparseModifiers(i.getModifiers());
        Unparser.this.unparseBlockStatement(i.block);
    }

    private void
    unparseFieldDeclaration(FieldDeclaration fd) {
        Unparser.this.unparseDocComment(fd);
        this.unparseModifiers(fd.modifiers);
        Unparser.this.unparseType(fd.type);
        Unparser.this.pw.print(' ');
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            if (i > 0) Unparser.this.pw.print(", ");
            Unparser.this.unparseVariableDeclarator(fd.variableDeclarators[i]);
        }
        Unparser.this.pw.print(';');
    }

    private void
    unparseResources(TryStatement.Resource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            if (i > 0) Unparser.this.pw.print("; ");
            Unparser.this.unparseResource(resources[i]);
        }
    }

    /**
     * Where the {@code visit...()} methods print their text. Notice that this {@link PrintWriter} does not print to
     * the output directly, but through an {@link AutoIndentWriter}.
     */
    protected final PrintWriter pw;

    /**
     * Testing of parsing/unparsing.
     * <p>
     *   Reads compilation units from the files named on the command line and unparses them to {@link System#out}.
     * </p>
     */
    public static void
    main(String[] args) throws Exception {

        Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
        for (String fileName : args) {

            // Parse each compilation unit.
            FileReader              r = new FileReader(fileName);
            AbstractCompilationUnit acu;
            try {
                acu = new Parser(new Scanner(fileName, r)).parseAbstractCompilationUnit();
            } finally {
                r.close();
            }

            // Unparse each compilation unit.
            Unparser.unparse(acu, w);
        }
        w.flush();
    }

    /**
     * Unparses the given {@link Java.AbstractCompilationUnit} to the given {@link Writer}.
     */
    public static void
    unparse(AbstractCompilationUnit acu, Writer w) {
        Unparser uv = new Unparser(w);
        uv.unparseAbstractCompilationUnit(acu);
        uv.close();
    }

    public
    Unparser(Writer w) {
        this.pw = new PrintWriter(new AutoIndentWriter(w), true);
    }

    /**
     * Flushes all generated code.
     */
    public void
    flush() { this.pw.flush(); }

    /**
     * Flushes all generated code.
     */
    @Override
    public void
    close() { this.pw.flush(); }

    /**
     * @param cu The compilation unit to unparse
     */
    public void
    unparseAbstractCompilationUnit(AbstractCompilationUnit cu) {
        cu.accept(this.compilationUnitUnparser);
    }

    public void
    unparseImportDeclaration(ImportDeclaration id) { id.accept(this.importUnparser); }

    private void
    unparseConstructorDeclarator(ConstructorDeclarator cd) {

        this.unparseDocComment(cd);
        this.unparseModifiers(cd.getModifiers());
        AbstractClassDeclaration declaringClass = cd.getDeclaringClass();
        this.pw.print(
            declaringClass instanceof NamedClassDeclaration
            ? ((NamedClassDeclaration) declaringClass).name
            : "UNNAMED"
        );
        this.unparseFunctionDeclaratorRest(cd);

        List<? extends BlockStatement> oss = cd.statements;
        if (oss == null) {
            this.pw.print(';');
            return;
        }

        this.pw.print(' ');

        ConstructorInvocation oci = cd.constructorInvocation;
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
    }

    private void
    unparseMethodDeclarator(MethodDeclarator md) {

        final List<? extends BlockStatement> oss = md.statements;

        this.unparseDocComment(md);
        this.unparseModifiers(md.getModifiers());
        this.unparseTypeParameters(md.getOptionalTypeParameters());
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
    }

    /**
     * Generates Java code from a sequence of {@link BlockStatement}s.
     */
    public void
    unparseStatements(List<? extends BlockStatement> statements) {

        int state = -1;
        for (BlockStatement bs : statements) {
            int x  = (
                bs instanceof Block                             ? 1 :
                bs instanceof LocalClassDeclarationStatement    ? 2 :
                bs instanceof LocalVariableDeclarationStatement ? 3 :
                bs instanceof SynchronizedStatement             ? 4 :
                99
            );
            if (state != -1 && state != x) this.pw.println(AutoIndentWriter.CLEAR_TABULATORS);
            state = x;

            this.unparseBlockStatement(bs);
            this.pw.println();
        }
    }

    private void
    unparseVariableDeclarator(VariableDeclarator vd) {
        this.pw.print(vd.name);
        for (int i = 0; i < vd.brackets; ++i) this.pw.print("[]");

        ArrayInitializerOrRvalue oi = vd.initializer;
        if (oi != null) {
            this.pw.print(" = ");
            this.unparseArrayInitializerOrRvalue(oi);
        }
    }

    private void
    unparseFormalParameter(FunctionDeclarator.FormalParameter fp, boolean hasEllipsis) {
        this.unparseModifiers(fp.modifiers);
        this.unparseType(fp.type);
        if (hasEllipsis) this.pw.write("...");
        this.pw.print(" " + AutoIndentWriter.TABULATOR + fp.name);
    }

    private void
    unparseCatchParameter(CatchParameter cp) {

        if (cp.finaL) this.pw.print("final ");

        this.pw.write(cp.types[0].toString());
        for (int i = 1; i < cp.types.length; i++) {
            this.pw.write(" | ");
            this.pw.write(cp.types[i].toString());
        }

        this.pw.print(" " + AutoIndentWriter.TABULATOR + cp.name);
    }

    // Helpers

    public void
    unparseLambdaParameters(LambdaParameters lp) { lp.accept(this.lambdaParametersUnparser); }

    public void
    unparseLambdaBody(LambdaBody body) { body.accept(this.lambdaBodyUnparser); }

    /**
     * Generates Java code from a {@link Block}.
     */
    public void
    unparseBlock(Block b) {
        if (b.statements.isEmpty()) {
            this.pw.print("{}");
            return;
        }
        this.pw.println('{');
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseStatements(b.statements);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    public void
    unparseBlockStatement(BlockStatement bs) { bs.accept(this.blockStatementUnparser); }

    public void
    unparseTypeDeclaration(TypeDeclaration td) { td.accept(this.typeDeclarationUnparser); }

    public void
    unparseType(Type t) { t.accept(this.typeUnparser); }

    public void
    unparseAtom(Atom a) { a.accept(this.atomUnparser); }

    private void
    unparseArrayInitializerOrRvalue(ArrayInitializerOrRvalue aiorv) {
        aiorv.accept(this.arrayInitializerOrRvalueUnparser);
    }

    public void
    unparseRvalue(Rvalue rv) { rv.accept(this.rvalueUnparser); }

    public void
    unparseLvalue(Lvalue lv) { lv.accept(this.lvalueUnparser); }

    /**
     * Iff the {@code operand} is unnatural for the {@code unaryOperator}, encloses the {@code operand} in parentheses.
     * Example: "a+b" is an unnatural operand for unary "!x".
     *
     * @param unaryOperator ++x --x +x -x ~x !x x++ x--
     */
    private void
    unparseUnaryOperation(Rvalue operand, String unaryOperator) {
        int cmp = Unparser.comparePrecedence(unaryOperator, operand);
        this.unparse(operand, cmp < 0);
    }

    /**
     * Iff the {@code lhs} is unnatural for the {@code binaryOperator}, encloses the {@code lhs} in parentheses.
     * Example: "a+b" is an unnatural lhs for operator "*".
     *
     * @param binaryOperator = +=... ?: || && | ^ & == != < > <= >= instanceof << >> >>> + - * / % cast
     */
    private void
    unparseLhs(Atom lhs, String binaryOperator) {
        int cmp = Unparser.comparePrecedence(binaryOperator, lhs);
        this.unparse(lhs, cmp < 0 || (cmp == 0 && Unparser.isLeftAssociate(binaryOperator)));
    }

    /**
     * Iff the {@code rhs} is unnatural for the {@code binaryOperator}, enclose the {@code rhs} in parentheses.
     * Example: "a+b" is an unnatural rhs for operator "*".
     */
    private void
    unparseRhs(Rvalue rhs, String binaryOperator) {
        int cmp = Unparser.comparePrecedence(binaryOperator, rhs);
        this.unparse(rhs, cmp < 0 || (cmp == 0 && Unparser.isRightAssociate(binaryOperator)));
    }

    private void
    unparse(Atom operand, boolean natural) {
        if (!natural) this.pw.print("((( ");
        this.unparseAtom(operand);
        if (!natural) this.pw.print(" )))");
    }

    /**
     * Returns {@code true} iff <var>operator</var> is right associative e.g. {@code a = b = c} evaluates as
     * {@code a = (b = c)}.
     *
     * @return Return true iff operator is right associative
     */
    private static boolean
    isRightAssociate(String operator) { return Unparser.RIGHT_ASSOCIATIVE_OPERATORS.contains(operator); }

    /**
     * Returns {@code true} iff <var>operator</var> is left associative e.g. {@code a - b - c} evaluates as
     * {@code (a - b) - c}.
     */
    private static boolean
    isLeftAssociate(String operator) { return Unparser.LEFT_ASSOCIATIVE_OPERATORS.contains(operator); }

    /**
     * Returns a value
     * <ul>
     *   <li>&lt; 0 iff the {@code operator} has lower precedence than the {@code operand}</li>
     *   <li>==; 0 iff the {@code operator} has equal precedence than the {@code operand}</li>
     *   <li>&gt; 0 iff the {@code operator} has higher precedence than the {@code operand}</li>
     * </ul>
     */
    private static int
    comparePrecedence(String operator, Atom operand) {
        if (operand instanceof BinaryOperation) {
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(((BinaryOperation) operand).operator)
            );
        } else
        if (operand instanceof UnaryOperation) {
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(((UnaryOperation) operand).operator + "x")
            );
        } else
        if (operand instanceof ConditionalExpression) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("?:");
        } else
        if (operand instanceof Instanceof) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("instanceof");
        } else
        if (operand instanceof Cast) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("cast");
        } else
        if (operand instanceof MethodInvocation || operand instanceof FieldAccess) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence(".");
        } else
        if (operand instanceof NewArray) {
            return Unparser.getOperatorPrecedence(operator) - Unparser.getOperatorPrecedence("new");
        } else
        if (operand instanceof Crement) {
            Crement c = (Crement) operand;
            return (
                Unparser.getOperatorPrecedence(operator)
                - Unparser.getOperatorPrecedence(c.pre ? c.operator + "x" : "x" + c.operator)
            );
        } else
        {
            // All other rvalues (e.g. literal) have higher precedence than any operator.
            return -1;
        }
    }

    private static int
    getOperatorPrecedence(String operator) {
        return ((Integer) Unparser.OPERATOR_PRECEDENCE.get(operator)).intValue();
    }

    private static final Set<String> LEFT_ASSOCIATIVE_OPERATORS  = new HashSet<>();
    private static final Set<String> RIGHT_ASSOCIATIVE_OPERATORS = new HashSet<>();
    private static final Set<String> UNARY_OPERATORS             = new HashSet<>();

    /**
     * Maps (pseudo-)operators like {@code "?:"} and {@code "x++"} to precedences (higher value
     * means higher precedence).
     */
    private static final Map<String, Integer> OPERATOR_PRECEDENCE = new HashMap<>();

    static {
        Object[] operators = {
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", ">>>=", "&=", "^=", "|=",
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "?:",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "||",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "&&",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "|",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "^",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "&",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "==", "!=",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "<", ">", "<=", ">=", "instanceof",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "<<", ">>", ">>>",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "+", "-",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "*", "/", "%",
            Unparser.RIGHT_ASSOCIATIVE_OPERATORS, "cast",
            Unparser.UNARY_OPERATORS,             "++x", "--x", "+x", "-x", "~x", "!x",
            Unparser.UNARY_OPERATORS,             "x++", "x--",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  "new",
            Unparser.LEFT_ASSOCIATIVE_OPERATORS,  ".", "[ ]",
        };
        int precedence = 0;
        LOOP1: for (int i = 0;;) {
            @SuppressWarnings("unchecked") final Set<String> s = (Set<String>) operators[i++];

            precedence++;
            for (;;) {
                if (i == operators.length) break LOOP1;
                if (!(operators[i] instanceof String)) break;
                String operator = (String) operators[i++];
                s.add(operator);
                Unparser.OPERATOR_PRECEDENCE.put(operator, precedence);
            }
        }
    }

    private void
    unparseNamedClassDeclaration(NamedClassDeclaration ncd) {
        this.unparseDocComment(ncd);
        this.unparseModifiers(ncd.getModifiers());
        this.pw.print("class " + ncd.name);

        Type oet = ncd.extendedType;
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

    /**
     * Generates Java code from a {@link AbstractClassDeclaration}.
     */
    public void
    unparseClassDeclarationBody(AbstractClassDeclaration cd) {

        // Multi-line!
        for (ConstructorDeclarator ctord : cd.constructors) {
            this.pw.println();
            ctord.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        this.unparseTypeDeclarationBody(cd);
        for (BlockStatement fdoi : cd.fieldDeclarationsAndInitializers) {
            this.pw.println();
            fdoi.accept(this.blockStatementUnparser);
            this.pw.println();
        }
    }

    /**
     * @return Whether {@link #unparseClassDeclarationBody(Java.AbstractClassDeclaration)} will produce <em>no</em>
     *         output
     */
    private static boolean
    classDeclarationBodyIsEmpty(AbstractClassDeclaration cd) {
        return (
            cd.constructors.isEmpty()
            && cd.getMethodDeclarations().isEmpty()
            && cd.getMemberTypeDeclarations().isEmpty()
            && cd.fieldDeclarationsAndInitializers.isEmpty()
        );
    }
    private void
    unparseInterfaceDeclaration(InterfaceDeclaration id) {
        this.unparseDocComment(id);
        this.unparseModifiers(id.getModifiers());
        this.pw.print("interface ");
        this.pw.print(id.name);
        if (id.extendedTypes.length > 0) this.pw.print(" extends " + Java.join(id.extendedTypes, ", "));
        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(id);
        for (TypeBodyDeclaration cnstd : id.constantDeclarations) {
            cnstd.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    // Multi-line!
    private void
    unparseTypeDeclarationBody(TypeDeclaration td) {
        for (MethodDeclarator md : td.getMethodDeclarations()) {
            this.pw.println();
            md.accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
        for (MemberTypeDeclaration mtd : td.getMemberTypeDeclarations()) {
            this.pw.println();
            ((TypeBodyDeclaration) mtd).accept(this.typeBodyDeclarationUnparser);
            this.pw.println();
        }
    }
    private void
    unparseFunctionDeclaratorRest(FunctionDeclarator fd) {
        this.unparseFormalParameters(fd.formalParameters);
        if (fd.thrownExceptions.length > 0) this.pw.print(" throws " + Java.join(fd.thrownExceptions, ", "));
    }

    private void
    unparseFormalParameters(FormalParameters fps) {

        boolean big = fps.parameters.length >= 4;

        this.pw.print('(');
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.INDENT); }
        for (int i = 0; i < fps.parameters.length; ++i) {
            if (i > 0) {
                if (big) {
                    this.pw.println(',');
                } else
                {
                    this.pw.print(", ");
                }
            }
            this.unparseFormalParameter(
                fps.parameters[i],
                i == fps.parameters.length - 1 && fps.variableArity
            );
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
    }

    private void
    unparseDocComment(DocCommentable dc) {
        String docComment = dc.getDocComment();
        if (docComment != null) {
            this.pw.print("/**");
            BufferedReader br = new BufferedReader(new StringReader(docComment));
            for (;;) {
                String line;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    throw new InternalCompilerException(null, e);
                }
                if (line == null) break;
                this.pw.println(line);
                this.pw.print(" *");
            }
            this.pw.println("/");
        }
    }

    private void
    unparseAnnotations(Annotation[] annotations) {
        for (Annotation a : annotations) a.accept(this.annotationUnparser);
    }

    private void
    unparseModifiers(Modifier[] modifiers) {
        for (Modifier m : modifiers) m.accept(this.modifierUnparser);
    }

    private void
    unparseTypeParameters(@Nullable TypeParameter[] typeParameters) {
        if (typeParameters == null) return;
        this.pw.print('<');
        for (int i = 0; i < typeParameters.length; ++i) {
            if (i > 0) this.pw.print(", ");
            this.unparseTypeParameter(typeParameters[i]);
        }
        this.pw.print("> ");
    }

    private void
    unparseTypeParameter(TypeParameter typeParameter) {

        this.pw.print(typeParameter.name);

        ReferenceType[] bounds = typeParameter.bound;
        if (bounds != null) {
            this.pw.print(" extends ");
            for (int i = 0; i < bounds.length; i++) {
                if (i > 0) this.pw.print(", ");
                this.unparseType(bounds[i]);
            }
        }
    }

    private void
    unparseFunctionInvocationArguments(Rvalue[] arguments) {
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
            this.unparseAtom(arguments[i]);
        }
        if (big) { this.pw.println(); this.pw.print(AutoIndentWriter.UNINDENT); }
        this.pw.print(')');
    }

    private void
    unparseEnumDeclaration(EnumDeclaration ed) {

        this.unparseDocComment(ed);
        this.unparseModifiers(ed.getModifiers());
        this.pw.print("enum " + ed.getName());

        Type[] its = ed.getImplementedTypes();
        if (its.length > 0) this.pw.print(" implements " + Java.join(its, ", "));

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        Iterator<EnumConstant> it = ed.getConstants().iterator();
        if (it.hasNext()) {
            for (;;) {
                this.typeDeclarationUnparser.visitEnumConstant((EnumConstant) it.next());

                if (!it.hasNext()) break;
                this.pw.print(", ");
            }
        }
        this.pw.println();
        this.pw.println(';');
        this.unparseClassDeclarationBody((AbstractClassDeclaration) ed);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    private void
    unparseAnnotationTypeDeclaration(AnnotationTypeDeclaration atd) {
        this.unparseDocComment(atd);
        this.unparseModifiers(atd.getModifiers());
        this.pw.print("@interface ");
        this.pw.print(atd.getName());

        this.pw.println(" {");
        this.pw.print(AutoIndentWriter.INDENT);
        this.unparseTypeDeclarationBody(atd);
        this.pw.print(AutoIndentWriter.UNINDENT + "}");
    }

    private void
    unparseFunctionDeclarator(FunctionDeclarator fd) {
        fd.accept(this.functionDeclaratorUnparser);
    }

    private void
    unparseResource(TryStatement.Resource r) {
        r.accept(this.resourceUnparser);
    }

    private void
    unparseVariableDeclarators(VariableDeclarator[] variableDeclarators) {
        Unparser.this.unparseVariableDeclarator(variableDeclarators[0]);
        for (int i = 1; i < variableDeclarators.length; ++i) {
            Unparser.this.pw.print(", ");
            Unparser.this.unparseVariableDeclarator(variableDeclarators[i]);
        }
    }
}
