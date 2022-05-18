
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2018 Arno Unkrig. All rights reserved.
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

package org.codehaus.janino.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.Java.AccessModifier;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
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
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FieldDeclarationOrInitializer;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForEachStatement;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameter;
import org.codehaus.janino.Java.FunctionDeclarator.FormalParameters;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaExpression;
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
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
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
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.SimpleConstant;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.SingleElementAnnotation;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SwitchStatement.SwitchBlockStatementGroup;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.TryStatement.LocalVariableDeclaratorResource;
import org.codehaus.janino.Java.TryStatement.Resource;
import org.codehaus.janino.Java.TryStatement.VariableAccessResource;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Java.Wildcard;
import org.codehaus.janino.Visitor.AbstractCompilationUnitVisitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.ArrayInitializerOrRvalueVisitor;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ConstructorInvocationVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.FieldDeclarationOrInitializerVisitor;
import org.codehaus.janino.Visitor.FunctionDeclaratorVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.ModifierVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.Visitor.TryStatementResourceVisitor;
import org.codehaus.janino.Visitor.TypeArgumentVisitor;
import org.codehaus.janino.Visitor.TypeBodyDeclarationVisitor;
import org.codehaus.janino.Visitor.TypeDeclarationVisitor;
import org.codehaus.janino.Visitor.TypeVisitor;

/**
 * Creates deep copies of AST elements.
 * <p>
 *   The main purpose of this class is to <em>extend</em> it, and <em>modify</em> the AST while it is being copied.
 *   For an example, see {@code org.codehaus.janino.tests.AstTest.testMethodToLabeledStatement()}.
 * </p>
 */
@SuppressWarnings("unused") public
class DeepCopier {

    public
    DeepCopier() {}

    // ------------------------- Visitors that implement the copying of abstract AST elements

    private final AbstractCompilationUnitVisitor<AbstractCompilationUnit, CompileException>
    abstractCompilationUnitCopier = (
        new AbstractCompilationUnitVisitor<AbstractCompilationUnit, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLength:2
            @Override @Nullable public AbstractCompilationUnit visitCompilationUnit(CompilationUnit cu)                throws CompileException { return DeepCopier.this.copyCompilationUnit(cu);         }
            @Override @Nullable public AbstractCompilationUnit visitModularCompilationUnit(ModularCompilationUnit mcu) throws CompileException { return DeepCopier.this.copyModularCompilationUnit(mcu); }
        }
    );

    private final ImportVisitor<ImportDeclaration, CompileException>
    importCopier = new ImportVisitor<ImportDeclaration, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:4
        @Override @Nullable public ImportDeclaration visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          throws CompileException { return DeepCopier.this.copySingleTypeImportDeclaration(stid);      }
        @Override @Nullable public ImportDeclaration visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     throws CompileException { return DeepCopier.this.copyTypeImportOnDemandDeclaration(tiodd);   }
        @Override @Nullable public ImportDeclaration visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      throws CompileException { return DeepCopier.this.copySingleStaticImportDeclaration(ssid);    }
        @Override @Nullable public ImportDeclaration visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) throws CompileException { return DeepCopier.this.copyStaticImportOnDemandDeclaration(siodd); }
    };

    private final TypeDeclarationVisitor<TypeDeclaration, CompileException>
    typeDeclarationCopier = new TypeDeclarationVisitor<TypeDeclaration, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:11
        @Override @Nullable public TypeDeclaration visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws CompileException { return DeepCopier.this.copyAnonymousClassDeclaration(acd);                 }
        @Override @Nullable public TypeDeclaration visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws CompileException { return DeepCopier.this.copyLocalClassDeclaration(lcd);                     }
        @Override @Nullable public TypeDeclaration visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)                    throws CompileException { return DeepCopier.this.copyPackageMemberClassDeclaration(pmcd);            }
        @Override @Nullable public TypeDeclaration visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws CompileException { return DeepCopier.this.copyPackageMemberInterfaceDeclaration(pmid);        }
        @Override @Nullable public TypeDeclaration visitEnumConstant(EnumConstant ec)                                                        throws CompileException { return DeepCopier.this.copyEnumConstant(ec);                               }
        @Override @Nullable public TypeDeclaration visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws CompileException { return DeepCopier.this.copyPackageMemberEnumDeclaration(pmed);             }
        @Override @Nullable public TypeDeclaration visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd)                throws CompileException { return DeepCopier.this.copyMemberAnnotationTypeDeclaration(matd);          }
        @Override @Nullable public TypeDeclaration visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws CompileException { return DeepCopier.this.copyPackageMemberAnnotationTypeDeclaration(pmatd);  }
        @Override @Nullable public TypeDeclaration visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws CompileException { return DeepCopier.this.copyMemberEnumDeclaration(med);                     }
        @Override @Nullable public TypeDeclaration visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws CompileException { return DeepCopier.this.copyMemberInterfaceDeclaration(mid);                }
        @Override @Nullable public TypeDeclaration visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws CompileException { return DeepCopier.this.copyMemberClassDeclaration(mcd);                    }
    };

    private final ArrayInitializerOrRvalueVisitor<ArrayInitializerOrRvalue, CompileException>
    arrayInitializerOrRvalueCopier = new ArrayInitializerOrRvalueVisitor<ArrayInitializerOrRvalue, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public ArrayInitializerOrRvalue visitArrayInitializer(ArrayInitializer ai) throws CompileException { return DeepCopier.this.copyArrayInitializer(ai); }
        @Override public ArrayInitializerOrRvalue visitRvalue(Rvalue rvalue)                 throws CompileException { return DeepCopier.this.copyRvalue(rvalue);       }
    };

    private final RvalueVisitor<Rvalue, CompileException>
    rvalueCopier = new RvalueVisitor<Rvalue, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:30
        @Override public Rvalue visitLvalue(Lvalue lv)                                              throws CompileException { return DeepCopier.this.copyLvalue(lv);                              }
        @Override public Rvalue visitArrayLength(ArrayLength al)                                    throws CompileException { return DeepCopier.this.copyArrayLength(al);                         }
        @Override public Rvalue visitAssignment(Assignment a)                                       throws CompileException { return DeepCopier.this.copyAssignment(a);                           }
        @Override public Rvalue visitUnaryOperation(UnaryOperation uo)                              throws CompileException { return DeepCopier.this.copyUnaryOperation(uo);                      }
        @Override public Rvalue visitBinaryOperation(BinaryOperation bo)                            throws CompileException { return DeepCopier.this.copyBinaryOperation(bo);                     }
        @Override public Rvalue visitCast(Cast c)                                                   throws CompileException { return DeepCopier.this.copyCast(c);                                 }
        @Override public Rvalue visitClassLiteral(ClassLiteral cl)                                  throws CompileException { return DeepCopier.this.copyClassLiteral(cl);                        }
        @Override public Rvalue visitConditionalExpression(ConditionalExpression ce)                throws CompileException { return DeepCopier.this.copyConditionalExpression(ce);               }
        @Override public Rvalue visitCrement(Crement c)                                             throws CompileException { return DeepCopier.this.copyCrement(c);                              }
        @Override public Rvalue visitInstanceof(Instanceof io)                                      throws CompileException { return DeepCopier.this.copyInstanceof(io);                          }
        @Override public Rvalue visitMethodInvocation(MethodInvocation mi)                          throws CompileException { return DeepCopier.this.copyMethodInvocation(mi);                    }
        @Override public Rvalue visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { return DeepCopier.this.copySuperclassMethodInvocation(smi);         }
        @Override public Rvalue visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { return DeepCopier.this.copyIntegerLiteral(il);                      }
        @Override public Rvalue visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { return DeepCopier.this.copyFloatingPointLiteral(fpl);               }
        @Override public Rvalue visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { return DeepCopier.this.copyBooleanLiteral(bl);                      }
        @Override public Rvalue visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { return DeepCopier.this.copyCharacterLiteral(cl);                    }
        @Override public Rvalue visitStringLiteral(StringLiteral sl)                                throws CompileException { return DeepCopier.this.copyStringLiteral(sl);                       }
        @Override public Rvalue visitNullLiteral(NullLiteral nl)                                    throws CompileException { return DeepCopier.this.copyNullLiteral(nl);                         }
        @Override public Rvalue visitSimpleConstant(SimpleConstant sl)                              throws CompileException { return DeepCopier.this.copySimpleLiteral(sl);                       }
        @Override public Rvalue visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { return DeepCopier.this.copyNewAnonymousClassInstance(naci);         }
        @Override public Rvalue visitNewArray(NewArray na)                                          throws CompileException { return DeepCopier.this.copyNewArray(na);                            }
        @Override public Rvalue visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { return DeepCopier.this.copyNewInitializedArray(nia);                }
        @Override public Rvalue visitNewClassInstance(NewClassInstance nci)                         throws CompileException { return DeepCopier.this.copyNewClassInstance(nci);                   }
        @Override public Rvalue visitParameterAccess(ParameterAccess pa)                            throws CompileException { return DeepCopier.this.copyParameterAccess(pa);                     }
        @Override public Rvalue visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { return DeepCopier.this.copyQualifiedThisReference(qtr);             }
        @Override public Rvalue visitThisReference(ThisReference tr)                                throws CompileException { return DeepCopier.this.copyThisReference(tr);                       }
        @Override public Rvalue visitLambdaExpression(LambdaExpression le)                          throws CompileException { return DeepCopier.this.copyLambdaExpression(le);                    }
        @Override public Rvalue visitMethodReference(MethodReference mr)                            throws CompileException { return DeepCopier.this.copyMethodReference(mr);                     }
        @Override public Rvalue visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { return DeepCopier.this.copyClassInstanceCreationReference(cicr);    }
        @Override public Rvalue visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { return DeepCopier.this.copyArrayCreationReference(acr);             }
    };

    private final LvalueVisitor<Lvalue, CompileException>
    lvalueCopier = new LvalueVisitor<Lvalue, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:7
        @Override @Nullable public Lvalue visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return DeepCopier.this.copyAmbiguousName(an);                       }
        @Override @Nullable public Lvalue visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return DeepCopier.this.copyArrayAccessExpression(aae);              }
        @Override @Nullable public Lvalue visitFieldAccess(FieldAccess fa)                                            throws CompileException { return DeepCopier.this.copyFieldAccess(fa);                         }
        @Override @Nullable public Lvalue visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return DeepCopier.this.copyFieldAccessExpression(fae);              }
        @Override @Nullable public Lvalue visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return DeepCopier.this.copySuperclassFieldAccessExpression(scfae);  }
        @Override @Nullable public Lvalue visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { return DeepCopier.this.copyLocalVariableAccess(lva);                }
        @Override @Nullable public Lvalue visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return DeepCopier.this.copyParenthesizedExpression(pe);             }
    };

    private final TypeBodyDeclarationVisitor<TypeBodyDeclaration, CompileException>
    typeBodyDeclarationCopier = new TypeBodyDeclarationVisitor<TypeBodyDeclaration, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:7
        @Override public TypeBodyDeclaration visitFunctionDeclarator(FunctionDeclarator fd)                             throws CompileException { return DeepCopier.this.copyFunctionDeclarator(fd);                }
        @Override public TypeBodyDeclaration visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws CompileException { return DeepCopier.this.copyMemberAnnotationTypeDeclaration(matd); }
        @Override public TypeBodyDeclaration visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)            throws CompileException { return DeepCopier.this.copyMemberInterfaceDeclaration(mid);       }
        @Override public TypeBodyDeclaration visitMemberClassDeclaration(MemberClassDeclaration mcd)                    throws CompileException { return DeepCopier.this.copyMemberClassDeclaration(mcd);           }
        @Override public TypeBodyDeclaration visitMemberEnumDeclaration(MemberEnumDeclaration med)                      throws CompileException { return DeepCopier.this.copyMemberEnumDeclaration(med);            }
        @Override public TypeBodyDeclaration visitInitializer(Initializer i)                                            throws CompileException { return DeepCopier.this.copyInitializer(i);                        }
        @Override public TypeBodyDeclaration visitFieldDeclaration(FieldDeclaration fd)                                 throws CompileException { return DeepCopier.this.copyFieldDeclaration(fd);                  }
    };

    private final FunctionDeclaratorVisitor<FunctionDeclarator, CompileException>
    functionDeclaratorCopier = new FunctionDeclaratorVisitor<FunctionDeclarator, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public FunctionDeclarator visitConstructorDeclarator(ConstructorDeclarator cd) throws CompileException { return DeepCopier.this.copyConstructorDeclarator(cd);  }
        @Override public FunctionDeclarator visitMethodDeclarator(MethodDeclarator md)           throws CompileException { return DeepCopier.this.copyMethodDeclarator(md);       }
    };

    private final BlockStatementVisitor<BlockStatement, CompileException>
    blockStatementCopier = new BlockStatementVisitor<BlockStatement, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:23
        @Override public BlockStatement visitInitializer(Initializer i)                                                throws CompileException { return DeepCopier.this.copyInitializer(i);                          }
        @Override public BlockStatement visitFieldDeclaration(FieldDeclaration fd)                                     throws CompileException { return DeepCopier.this.copyFieldDeclaration(fd);                    }
        @Override public BlockStatement visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return DeepCopier.this.copyLabeledStatement(ls);                    }
        @Override public BlockStatement visitBlock(Block b)                                                            throws CompileException { return DeepCopier.this.copyBlock(b);                                }
        @Override public BlockStatement visitExpressionStatement(ExpressionStatement es)                               throws CompileException { return DeepCopier.this.copyExpressionStatement(es);                 }
        @Override public BlockStatement visitIfStatement(IfStatement is)                                               throws CompileException { return DeepCopier.this.copyIfStatement(is);                         }
        @Override public BlockStatement visitForStatement(ForStatement fs)                                             throws CompileException { return DeepCopier.this.copyForStatement(fs);                        }
        @Override public BlockStatement visitForEachStatement(ForEachStatement fes)                                    throws CompileException { return DeepCopier.this.copyForEachStatement(fes);                   }
        @Override public BlockStatement visitWhileStatement(WhileStatement ws)                                         throws CompileException { return DeepCopier.this.copyWhileStatement(ws);                      }
        @Override public BlockStatement visitTryStatement(TryStatement ts)                                             throws CompileException { return DeepCopier.this.copyTryStatement(ts);                        }
        @Override public BlockStatement visitSwitchStatement(SwitchStatement ss)                                       throws CompileException { return DeepCopier.this.copySwitchStatement(ss);                     }
        @Override public BlockStatement visitSynchronizedStatement(SynchronizedStatement ss)                           throws CompileException { return DeepCopier.this.copySynchronizedStatement(ss);               }
        @Override public BlockStatement visitDoStatement(DoStatement ds)                                               throws CompileException { return DeepCopier.this.copyDoStatement(ds);                         }
        @Override public BlockStatement visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return DeepCopier.this.copyLocalVariableDeclarationStatement(lvds); }
        @Override public BlockStatement visitReturnStatement(ReturnStatement rs)                                       throws CompileException { return DeepCopier.this.copyReturnStatement(rs);                     }
        @Override public BlockStatement visitThrowStatement(ThrowStatement ts)                                         throws CompileException { return DeepCopier.this.copyThrowStatement(ts);                      }
        @Override public BlockStatement visitBreakStatement(BreakStatement bs)                                         throws CompileException { return DeepCopier.this.copyBreakStatement(bs);                      }
        @Override public BlockStatement visitContinueStatement(ContinueStatement cs)                                   throws CompileException { return DeepCopier.this.copyContinueStatement(cs);                   }
        @Override public BlockStatement visitAssertStatement(AssertStatement as)                                       throws CompileException { return DeepCopier.this.copyAssertStatement(as);                     }
        @Override public BlockStatement visitEmptyStatement(EmptyStatement es)                                         throws CompileException { return DeepCopier.this.copyEmptyStatement(es);                      }
        @Override public BlockStatement visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws CompileException { return DeepCopier.this.copyLocalClassDeclarationStatement(lcds);    }
        @Override public BlockStatement visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws CompileException { return DeepCopier.this.copyAlternateConstructorInvocation(aci);     }
        @Override public BlockStatement visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws CompileException { return DeepCopier.this.copySuperConstructorInvocation(sci);         }
    };

    private final FieldDeclarationOrInitializerVisitor<FieldDeclarationOrInitializer, CompileException>
    fieldDeclarationOrInitializerCopier = new FieldDeclarationOrInitializerVisitor<FieldDeclarationOrInitializer, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public FieldDeclarationOrInitializer visitInitializer(Initializer i)            throws CompileException { return DeepCopier.this.copyInitializer(i);       }
        @Override public FieldDeclarationOrInitializer visitFieldDeclaration(FieldDeclaration fd) throws CompileException { return DeepCopier.this.copyFieldDeclaration(fd); }
    };

    private final TypeVisitor<Type, CompileException>
    typeCopier = new TypeVisitor<Type, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:5
        @Override public Type visitArrayType(ArrayType at)                throws CompileException { return DeepCopier.this.copyArrayType(at);         }
        @Override public Type visitPrimitiveType(PrimitiveType bt)        throws CompileException { return DeepCopier.this.copyPrimitiveType(bt);     }
        @Override public Type visitReferenceType(ReferenceType rt)        throws CompileException { return DeepCopier.this.copyReferenceType(rt);     }
        @Override public Type visitRvalueMemberType(RvalueMemberType rmt) throws CompileException { return DeepCopier.this.copyRvalueMemberType(rmt); }
        @Override public Type visitSimpleType(SimpleType st)              throws CompileException { return DeepCopier.this.copySimpleType(st);        }
    };

    private final AtomVisitor<Atom, CompileException>
    atomCopier = new AtomVisitor<Atom, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLengthCheck:4
        @Override public Atom visitRvalue(Rvalue rv)                               throws CompileException { return DeepCopier.this.copyRvalue(rv);                }
        @Override public Atom visitPackage(Package p)                              throws CompileException { return DeepCopier.this.copyPackage(p);                }
        @Override public Atom visitType(Type t)                                    throws CompileException { return DeepCopier.this.copyType(t);                   }
        @Override public Atom visitConstructorInvocation(ConstructorInvocation ci) throws CompileException { return DeepCopier.this.copyConstructorInvocation(ci); }
    };

    private final ConstructorInvocationVisitor<ConstructorInvocation, CompileException>
    constructorInvocationCopier = new ConstructorInvocationVisitor<ConstructorInvocation, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public ConstructorInvocation visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws CompileException { return DeepCopier.this.copyAlternateConstructorInvocation(aci); }
        @Override public ConstructorInvocation visitSuperConstructorInvocation(SuperConstructorInvocation sci)         throws CompileException { return DeepCopier.this.copySuperConstructorInvocation(sci);     }
    };

    private final ElementValueVisitor<ElementValue, CompileException>
    elementValueCopier = new ElementValueVisitor<ElementValue, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override public ElementValue visitRvalue(Rvalue rv)                                               throws CompileException { return DeepCopier.this.copyRvalue(rv);                         }
        @Override public ElementValue visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws CompileException { return DeepCopier.this.copyElementValueArrayInitializer(evai); }
        @Override public ElementValue visitAnnotation(Annotation a)                                        throws CompileException { return DeepCopier.this.copyAnnotation(a);                      }
    };

    private final AnnotationVisitor<Annotation, CompileException>
    annotationCopier = new AnnotationVisitor<Annotation, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override public Annotation visitMarkerAnnotation(MarkerAnnotation ma)                throws CompileException { return DeepCopier.this.copyMarkerAnnotation(ma);         }
        @Override public Annotation visitNormalAnnotation(NormalAnnotation na)                throws CompileException { return DeepCopier.this.copyNormalAnnotation(na);         }
        @Override public Annotation visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException { return DeepCopier.this.copySingleElementAnnotation(sea); }
    };

    private final ModifierVisitor<Modifier, CompileException>
    modifierCopier = new ModifierVisitor<Modifier, CompileException>() {

        @Override @Nullable public Modifier visitMarkerAnnotation(MarkerAnnotation ma)                throws CompileException { return DeepCopier.this.copyMarkerAnnotation(ma);         } // SUPPRESS CHECKSTYLE LineLength:3
        @Override @Nullable public Modifier visitNormalAnnotation(NormalAnnotation na)                throws CompileException { return DeepCopier.this.copyNormalAnnotation(na);         }
        @Override @Nullable public Modifier visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException { return DeepCopier.this.copySingleElementAnnotation(sea); }
        @Override @Nullable public Modifier visitAccessModifier(AccessModifier am)                    throws CompileException { return DeepCopier.this.copyAccessModifier(am);           }
    };

    private final TryStatementResourceVisitor<TryStatement.Resource, CompileException>
    resourceCopier = new TryStatementResourceVisitor<TryStatement.Resource, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public TryStatement.Resource visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws CompileException { return DeepCopier.this.copyLocalVariableDeclaratorResource(lvdr); }
        @Override public TryStatement.Resource visitVariableAccessResource(VariableAccessResource var)                    throws CompileException { return DeepCopier.this.copyVariableAccessResource(var);           }
    };

    private final TypeArgumentVisitor<TypeArgument, CompileException>
    typeArgumentCopier = new TypeArgumentVisitor<TypeArgument, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLengthCheck:3
        @Override @Nullable public TypeArgument visitWildcard(Wildcard w)            throws CompileException { return DeepCopier.this.copyWildcard(w);       }
        @Override @Nullable public TypeArgument visitReferenceType(ReferenceType rt) throws CompileException { return DeepCopier.this.copyReferenceType(rt); }
        @Override @Nullable public TypeArgument visitArrayType(ArrayType at)         throws CompileException { return DeepCopier.this.copyArrayType(at);     }
    };

    // ------------------------------ "copy*()" methods on abstract types

    // SUPPRESS CHECKSTYLE LineLengthCheck:15
    public AbstractCompilationUnit       copyAbstractCompilationUnit(AbstractCompilationUnit subject)             throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.abstractCompilationUnitCopier));       }
    public ImportDeclaration             copyImportDeclaration(ImportDeclaration subject)                         throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.importCopier));                        }
    public TypeDeclaration               copyTypeDeclaration(TypeDeclaration subject)                             throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.typeDeclarationCopier));               }
    public TypeBodyDeclaration           copyTypeBodyDeclaration(TypeBodyDeclaration subject)                     throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.typeBodyDeclarationCopier));           }
    public FunctionDeclarator            copyFunctionDeclarator(FunctionDeclarator subject)                       throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.functionDeclaratorCopier));            }
    public BlockStatement                copyBlockStatement(BlockStatement subject)                               throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.blockStatementCopier));                }
    public FieldDeclarationOrInitializer copyFieldDeclarationOrInitializer(FieldDeclarationOrInitializer subject) throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.fieldDeclarationOrInitializerCopier)); }
    public Resource                      copyResource(Resource subject)                                           throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.resourceCopier));                      }
    public TypeArgument                  copyTypeArgument(TypeArgument subject)                                   throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.typeArgumentCopier));                  }
    public ConstructorInvocation         copyConstructorInvocation(ConstructorInvocation subject)                 throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.constructorInvocationCopier));         }
    public ElementValue                  copyElementValue(ElementValue subject)                                   throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.elementValueCopier));                  }
    public Annotation                    copyAnnotation(Annotation subject)                                       throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.annotationCopier));                    }
    public Rvalue                        copyRvalue(Rvalue subject)                                               throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.rvalueCopier));                        }
    public Lvalue                        copyLvalue(Lvalue subject)                                               throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.lvalueCopier));                        }
    public Type                          copyType(Type subject)                                                   throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.typeCopier));                          }
    public Atom                          copyAtom(Atom subject)                                                   throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.atomCopier));                          }
    public ArrayInitializerOrRvalue      copyArrayInitializerOrRvalue(ArrayInitializerOrRvalue subject)           throws CompileException { return DeepCopier.assertNotNull(subject.accept(this.arrayInitializerOrRvalueCopier));      }

    // SUPPRESS CHECKSTYLE LineLengthCheck:3
    public PackageMemberTypeDeclaration copyPackageMemberTypeDeclaration(PackageMemberTypeDeclaration subject) throws CompileException { return (PackageMemberTypeDeclaration) this.copyTypeDeclaration(subject); }
    public MemberTypeDeclaration        copyMemberTypeDeclaration(MemberTypeDeclaration subject)               throws CompileException { return (MemberTypeDeclaration)        this.copyTypeDeclaration(subject); }
    public Statement                    copyStatement(Statement subject)                                       throws CompileException { return (Statement)                    this.copyBlockStatement(subject);  }

    // ------------------------------ "copyOptional*()" methods

    // SUPPRESS CHECKSTYLE LineLengthCheck:19
    @Nullable public PackageDeclaration       copyOptionalPackageDeclaration(@Nullable PackageDeclaration subject)             throws CompileException { return subject == null ? null : this.copyPackageDeclaration(subject);       }
    @Nullable public BlockStatement           copyOptionalBlockStatement(@Nullable BlockStatement subject)                     throws CompileException { return subject == null ? null : this.copyBlockStatement(subject);           }
    @Nullable public Block                    copyOptionalBlock(@Nullable Block subject)                                       throws CompileException { return subject == null ? null : this.copyBlock(subject);                    }
    @Nullable public ArrayInitializer         copyOptionalArrayInitializer(@Nullable ArrayInitializer subject)                 throws CompileException { return subject == null ? null : this.copyArrayInitializer(subject);         }
    @Nullable public ArrayType                copyOptionalArrayType(@Nullable ArrayType subject)                               throws CompileException { return subject == null ? null : this.copyArrayType(subject);                }
    @Nullable public ReferenceType            copyOptionalReferenceType(@Nullable ReferenceType subject)                       throws CompileException { return subject == null ? null : this.copyReferenceType(subject);            }
    @Nullable public ConstructorInvocation    copyOptionalConstructorInvocation(@Nullable ConstructorInvocation subject)       throws CompileException { return subject == null ? null : this.copyConstructorInvocation(subject);    }
    @Nullable public ElementValue             copyOptionalElementValue(@Nullable ElementValue subject)                         throws CompileException { return subject == null ? null : this.copyElementValue(subject);             }
    @Nullable public Rvalue                   copyOptionalRvalue(@Nullable Rvalue subject)                                     throws CompileException { return subject == null ? null : this.copyRvalue(subject);                   }
    @Nullable public Type                     copyOptionalType(@Nullable Type subject)                                         throws CompileException { return subject == null ? null : this.copyType(subject);                     }
    @Nullable public Atom                     copyOptionalAtom(@Nullable Atom subject)                                         throws CompileException { return subject == null ? null : this.copyAtom(subject);                     }
    @Nullable public ArrayInitializerOrRvalue copyOptionalArrayInitializerOrRvalue(@Nullable ArrayInitializerOrRvalue subject) throws CompileException { return subject == null ? null : this.copyArrayInitializerOrRvalue(subject); }

    @Nullable public ReferenceType[]          copyOptionalReferenceTypes(@Nullable ReferenceType[] subject)                    throws CompileException { return subject == null ? null : this.copyReferenceTypes(subject);           }
    @Nullable public TypeArgument[]           copyOptionalTypeArguments(@Nullable TypeArgument[] subject)                      throws CompileException { return subject == null ? null : this.copyTypeArguments(subject);            }
    @Nullable public Rvalue[]                 copyOptionalRvalues(@Nullable Rvalue[] subject)                                  throws CompileException { return subject == null ? null : this.copyRvalues(subject);                  }
    @Nullable public TypeParameter[]          copyOptionalTypeParameters(@Nullable TypeParameter[] subject)                    throws CompileException { return subject == null ? null : this.copyTypeParameters(subject);           }

    @Nullable public List<BlockStatement>     copyOptionalStatements(@Nullable Collection<? extends BlockStatement> subject)   throws CompileException { return subject == null ? null : this.copyStatements(subject);               }

    // ------------------------------ "copy*s()" methods for arrays

    // SUPPRESS CHECKSTYLE LineLengthCheck:11
    public ImportDeclaration[]        copyImportDeclarations(ImportDeclaration[] subject)               throws CompileException { ImportDeclaration[]        result = new ImportDeclaration[subject.length];          for (int i = 0; i < subject.length; i++) result[i] = this.copyImportDeclaration(subject[i]);                return result; }
    public TypeArgument[]             copyTypeArguments(TypeArgument[] subject)                         throws CompileException { TypeArgument[]             result = new TypeArgument[subject.length];               for (int i = 0; i < subject.length; i++) result[i] = this.copyTypeArgument(subject[i]);                     return result; }
    public VariableDeclarator[]       copyVariableDeclarators(VariableDeclarator[] subject)             throws CompileException { VariableDeclarator[]       result = new VariableDeclarator[subject.length];         for (int i = 0; i < subject.length; i++) result[i] = this.copyVariableDeclarator(subject[i]);               return result; }
    public ArrayInitializerOrRvalue[] copyArrayInitializerOrRvalues(ArrayInitializerOrRvalue[] subject) throws CompileException { ArrayInitializerOrRvalue[] result = new ArrayInitializerOrRvalue[subject.length];   for (int i = 0; i < subject.length; i++) result[i] = this.copyOptionalArrayInitializerOrRvalue(subject[i]); return result; }
    public ReferenceType[]            copyReferenceTypes(ReferenceType[] subject)                       throws CompileException { ReferenceType[]            result = new ReferenceType[subject.length];              for (int i = 0; i < subject.length; i++) result[i] = this.copyReferenceType(subject[i]);                    return result; }
    public ElementValue[]             copyElementValues(ElementValue[] subject)                         throws CompileException { ElementValue[]             result = new ElementValue[subject.length];               for (int i = 0; i < subject.length; i++) result[i] = this.copyOptionalElementValue(subject[i]);             return result; }
    public ElementValuePair[]         copyElementValuePairs(ElementValuePair[] subject)                 throws CompileException { ElementValuePair[]         result = new ElementValuePair[subject.length];           for (int i = 0; i < subject.length; i++) result[i] = this.copyElementValuePair(subject[i]);                 return result; }
    public Type[]                     copyTypes(Type[] subject)                                         throws CompileException { Type[]                     result = new Type[subject.length];                       for (int i = 0; i < subject.length; i++) result[i] = this.copyType(subject[i]);                             return result; }
    public TypeParameter[]            copyTypeParameters(TypeParameter[] subject)                       throws CompileException { TypeParameter[]            result = new TypeParameter[subject.length];              for (int i = 0; i < subject.length; i++) result[i] = this.copyTypeParameter(subject[i]);                    return result; }
    public FormalParameter[]          copyFormalParameters(FormalParameter[] subject)                   throws CompileException { FormalParameter[]          result = new FormalParameter[subject.length];            for (int i = 0; i < result.length; i++) result[i] = this.copyFormalParameter(subject[i]);                   return result; }
    public Annotation[]               copyAnnotations(Annotation[] subject)                             throws CompileException { Annotation[]               result = new Annotation[subject.length];                 for (int i = 0; i < result.length; i++) result[i] = this.copyAnnotation(subject[i]);                        return result; }

    // SUPPRESS CHECKSTYLE LineLengthCheck:2
    public Rvalue[]                   copyRvalues(Rvalue[] subject)                                     throws CompileException { return this.copyRvalues(Arrays.asList(subject)).toArray(new Rvalue[0]); }

    // ------------------------------ "copy*s()" methods for collections

    // SUPPRESS CHECKSTYLE LineLengthCheck:7
    public List<BlockStatement>            copyBlockStatements(Collection<? extends BlockStatement> subject)                       throws CompileException { List<BlockStatement>            result = new ArrayList<BlockStatement>(subject.size());                            for (BlockStatement bs              : subject) result.add(this.copyBlockStatement(bs));              return result; }
    public List<Resource>                  copyResources(Collection<? extends Resource> subject)                                   throws CompileException { List<Resource>                  result = new ArrayList<Resource>(subject.size());                                  for (Resource r                     : subject) result.add(this.copyResource(r));                     return result; }
    public List<CatchClause>               copyCatchClauses(Collection<? extends CatchClause> subject)                             throws CompileException { List<CatchClause>               result = new ArrayList<CatchClause>(subject.size());                               for (CatchClause sbgs               : subject) result.add(this.copyCatchClause(sbgs));               return result; }
    public List<SwitchBlockStatementGroup> copySwitchBlockStatementGroups(Collection<? extends SwitchBlockStatementGroup> subject) throws CompileException { List<SwitchBlockStatementGroup> result = new ArrayList<SwitchStatement.SwitchBlockStatementGroup>(subject.size()); for (SwitchBlockStatementGroup sbgs : subject) result.add(this.copySwitchBlockStatementGroup(sbgs)); return result; }
    public List<BlockStatement>            copyStatements(Collection<? extends BlockStatement> subject)                            throws CompileException { List<BlockStatement>            result = new ArrayList<BlockStatement>(subject.size());                            for (BlockStatement bs              : subject) result.add(this.copyBlockStatement(bs));              return result; }
    public List<Rvalue>                    copyRvalues(Collection<? extends Rvalue> subject)                                       throws CompileException { List<Rvalue>                    result = new ArrayList<Rvalue>(subject.size());                                    for (Rvalue rv                      : subject) result.add(this.copyRvalue(rv));                      return result; }

    // ------------------------------ "copy*()" methods on final types

    public CompilationUnit
    copyCompilationUnit(CompilationUnit subject) throws CompileException {

        CompilationUnit result = new CompilationUnit(
            subject.fileName,
            this.copyImportDeclarations(subject.importDeclarations)
        );

        result.setPackageDeclaration(this.copyOptionalPackageDeclaration(subject.packageDeclaration));

        for (PackageMemberTypeDeclaration pmtd : subject.packageMemberTypeDeclarations) {
            result.addPackageMemberTypeDeclaration(this.copyPackageMemberTypeDeclaration(pmtd));
        }

        return result;
    }

    public ModularCompilationUnit
    copyModularCompilationUnit(ModularCompilationUnit subject) throws CompileException {

        return new ModularCompilationUnit(
            subject.fileName,
            this.copyImportDeclarations(subject.importDeclarations),
            subject.moduleDeclaration
        );
    }

    public TypeArgument
    copyWildcard(Wildcard subject) throws CompileException {
        return new Wildcard(subject.bounds, this.copyOptionalReferenceType(subject.referenceType));
    }

    public PackageDeclaration
    copyPackageDeclaration(PackageDeclaration subject) throws CompileException {
        return new PackageDeclaration(subject.getLocation(), subject.packageName);
    }

    public ImportDeclaration
    copySingleTypeImportDeclaration(SingleTypeImportDeclaration stid) throws CompileException {
        return new SingleTypeImportDeclaration(stid.getLocation(), stid.identifiers.clone());
    }

    public ImportDeclaration
    copyTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd) throws CompileException {
        return new TypeImportOnDemandDeclaration(tiodd.getLocation(), tiodd.identifiers.clone());
    }

    public ImportDeclaration
    copySingleStaticImportDeclaration(SingleStaticImportDeclaration stid) throws CompileException {
        return new SingleStaticImportDeclaration(stid.getLocation(), stid.identifiers.clone());
    }

    public ImportDeclaration
    copyStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) throws CompileException {
        return new StaticImportOnDemandDeclaration(siodd.getLocation(), siodd.identifiers.clone());
    }

    public AnonymousClassDeclaration
    copyAnonymousClassDeclaration(AnonymousClassDeclaration subject) throws CompileException {

        AnonymousClassDeclaration
        result = new AnonymousClassDeclaration(subject.getLocation(), this.copyType(subject.baseType));

        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }

        return result;
    }

    public LocalClassDeclaration
    copyLocalClassDeclaration(LocalClassDeclaration subject) throws CompileException {

        LocalClassDeclaration result = new LocalClassDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            subject.getOptionalTypeParameters(),
            subject.extendedType,
            this.copyTypes(subject.implementedTypes)
        );

        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }

        return result;
    }

    public TypeDeclaration
    copyPackageMemberClassDeclaration(PackageMemberClassDeclaration subject) throws CompileException {
        PackageMemberClassDeclaration result = new PackageMemberClassDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyOptionalTypeParameters(subject.getOptionalTypeParameters()),
            this.copyOptionalType(subject.extendedType),
            this.copyTypes(subject.implementedTypes)
        );
        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }
        return result;
    }

    public MemberTypeDeclaration
    copyMemberInterfaceDeclaration(MemberInterfaceDeclaration subject) throws CompileException {

        MemberInterfaceDeclaration result = new MemberInterfaceDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyOptionalTypeParameters(subject.getOptionalTypeParameters()),
            this.copyTypes(subject.extendedTypes)
        );

        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }

        return result;
    }

    public TypeDeclaration
    copyPackageMemberInterfaceDeclaration(final PackageMemberInterfaceDeclaration subject) throws CompileException {

        PackageMemberInterfaceDeclaration result = new PackageMemberInterfaceDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyOptionalTypeParameters(subject.getOptionalTypeParameters()),
            this.copyTypes(subject.extendedTypes)
        );

        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }
        for (FieldDeclaration cd : subject.constantDeclarations) {
            result.addConstantDeclaration(this.copyFieldDeclaration(cd));
        }

        return result;
    }

    public MemberTypeDeclaration
    copyMemberClassDeclaration(MemberClassDeclaration subject) throws CompileException {

        MemberClassDeclaration result = new MemberClassDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyOptionalTypeParameters(subject.getOptionalTypeParameters()),
            this.copyOptionalType(subject.extendedType),
            this.copyTypes(subject.implementedTypes)
        );

        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }

        return result;
    }

    public ConstructorDeclarator
    copyConstructorDeclarator(ConstructorDeclarator subject) throws CompileException {
        return new ConstructorDeclarator(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            this.copyFormalParameters(subject.formalParameters),
            this.copyTypes(subject.thrownExceptions),
            this.copyOptionalConstructorInvocation(subject.constructorInvocation),
            this.copyBlockStatements(DeepCopier.assertNotNull(subject.statements))
        );
    }

    public Initializer
    copyInitializer(Initializer subject) throws CompileException {

        return new Initializer(
            subject.getLocation(),
            this.copyModifiers(subject.modifiers),
            this.copyBlock(subject.block)
        );
    }

    public MethodDeclarator
    copyMethodDeclarator(MethodDeclarator subject) throws CompileException {
        return new MethodDeclarator(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            this.copyOptionalTypeParameters(subject.typeParameters),
            this.copyType(subject.type),
            subject.name,
            this.copyFormalParameters(subject.formalParameters),
            this.copyTypes(subject.thrownExceptions),
            this.copyOptionalElementValue(subject.defaultValue),
            this.copyOptionalStatements(subject.statements)
        );
    }

    public FieldDeclaration
    copyFieldDeclaration(FieldDeclaration subject) throws CompileException {
        return new FieldDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.modifiers),
            this.copyType(subject.type),
            this.copyVariableDeclarators(subject.variableDeclarators)
        );
    }

    public VariableDeclarator
    copyVariableDeclarator(VariableDeclarator subject) throws CompileException {
        return new VariableDeclarator(
            subject.getLocation(),
            subject.name,
            subject.brackets,
            this.copyOptionalArrayInitializerOrRvalue(subject.initializer)
        );
    }

    public BlockStatement
    copyLabeledStatement(LabeledStatement ls) throws CompileException {
        return new LabeledStatement(ls.getLocation(), ls.label, this.copyStatement(ls.body));
    }

    public Block
    copyBlock(Block b) throws CompileException {
        Block result = new Block(b.getLocation());
        for (BlockStatement bs : b.statements) result.addStatement(this.copyBlockStatement(bs));
        return result;
    }

    public BlockStatement
    copyExpressionStatement(ExpressionStatement es) throws CompileException {
        return new ExpressionStatement(this.copyRvalue(es.rvalue));
    }

    public BlockStatement
    copyIfStatement(IfStatement is) throws CompileException {
        return new IfStatement(
            is.getLocation(),
            this.copyRvalue(is.condition),
            this.copyBlockStatement(is.thenStatement),
            this.copyOptionalBlockStatement(is.elseStatement)
        );
    }

    public BlockStatement
    copyForStatement(ForStatement fs) throws CompileException {
        return new ForStatement(
            fs.getLocation(),
            this.copyOptionalBlockStatement(fs.init),
            this.copyOptionalRvalue(fs.condition),
            this.copyOptionalRvalues(fs.update),
            this.copyBlockStatement(fs.body)
        );
    }

    public BlockStatement
    copyForEachStatement(ForEachStatement fes) throws CompileException {
        return new ForEachStatement(
            fes.getLocation(),
            this.copyFormalParameter(fes.currentElement),
            this.copyRvalue(fes.expression),
            this.copyBlockStatement(fes.body)
        );
    }

    public BlockStatement
    copyWhileStatement(WhileStatement ws) throws CompileException {
        return new WhileStatement(
            ws.getLocation(),
            this.copyRvalue(ws.condition),
            this.copyBlockStatement(ws.body)
        );
    }

    public BlockStatement
    copyTryStatement(TryStatement ts) throws CompileException {
        return new TryStatement(
            ts.getLocation(),
            this.copyResources(ts.resources),
            this.copyBlockStatement(ts.body),
            this.copyCatchClauses(ts.catchClauses),
            this.copyOptionalBlock(ts.finallY)
        );
    }

    public CatchClause
    copyCatchClause(CatchClause subject) throws CompileException {
        return new CatchClause(
            subject.getLocation(),
            this.copyCatchParameter(subject.catchParameter),
            this.copyBlockStatement(subject.body)
        );
    }

    public BlockStatement
    copySwitchStatement(SwitchStatement subject) throws CompileException {
        return new SwitchStatement(
            subject.getLocation(),
            this.copyRvalue(subject.condition),
            this.copySwitchBlockStatementGroups(subject.sbsgs)
        );
    }

    public SwitchBlockStatementGroup
    copySwitchBlockStatementGroup(SwitchBlockStatementGroup subject) throws CompileException {
        return new SwitchBlockStatementGroup(
            subject.getLocation(),
            this.copyRvalues(subject.caseLabels),
            subject.hasDefaultLabel,
            this.copyBlockStatements(subject.blockStatements)
        );
    }

    public BlockStatement
    copySynchronizedStatement(SynchronizedStatement subject) throws CompileException {
        return new SynchronizedStatement(
            subject.getLocation(),
            this.copyRvalue(subject.expression),
            this.copyBlockStatement(subject.body)
        );
    }

    public BlockStatement
    copyDoStatement(DoStatement subject) throws CompileException {
        return new DoStatement(
            subject.getLocation(),
            this.copyBlockStatement(subject.body),
            this.copyRvalue(subject.condition)
        );
    }

    public BlockStatement
    copyLocalVariableDeclarationStatement(LocalVariableDeclarationStatement subject) throws CompileException {
        return new LocalVariableDeclarationStatement(
            subject.getLocation(),
            this.copyModifiers(subject.modifiers),
            this.copyType(subject.type),
            this.copyVariableDeclarators(subject.variableDeclarators)
        );
    }

    public BlockStatement
    copyReturnStatement(ReturnStatement subject) throws CompileException {
        return new ReturnStatement(subject.getLocation(), this.copyOptionalRvalue(subject.returnValue));
    }

    public BlockStatement
    copyThrowStatement(ThrowStatement subject) throws CompileException {
        return new ThrowStatement(subject.getLocation(), this.copyRvalue(subject.expression));
    }

    public BlockStatement
    copyBreakStatement(BreakStatement subject) throws CompileException {
        return new BreakStatement(subject.getLocation(), subject.label);
    }

    public BlockStatement
    copyContinueStatement(ContinueStatement subject) throws CompileException {
        return new ContinueStatement(subject.getLocation(), subject.label);
    }

    public BlockStatement
    copyAssertStatement(AssertStatement subject) throws CompileException {
        return new AssertStatement(
            subject.getLocation(),
            this.copyRvalue(subject.expression1),
            this.copyOptionalRvalue(subject.expression2)
        );
    }

    public BlockStatement
    copyEmptyStatement(EmptyStatement subject) throws CompileException {
        return new EmptyStatement(subject.getLocation());
    }

    public BlockStatement
    copyLocalClassDeclarationStatement(LocalClassDeclarationStatement subject) throws CompileException {
        return new LocalClassDeclarationStatement(this.copyLocalClassDeclaration(subject.lcd));
    }

    public Atom
    copyPackage(Package subject) throws CompileException {
        return new Package(subject.getLocation(), subject.name);
    }

    public Rvalue
    copyArrayLength(ArrayLength subject) throws CompileException {
        return new ArrayLength(subject.getLocation(), this.copyRvalue(subject.lhs));
    }

    public Rvalue
    copyAssignment(Assignment subject) throws CompileException {
        return new Assignment(
            subject.getLocation(),
            this.copyLvalue(subject.lhs),
            subject.operator,
            this.copyRvalue(subject.rhs)
        );
    }

    public Rvalue
    copyUnaryOperation(UnaryOperation subject) throws CompileException {
        return new UnaryOperation(
            subject.getLocation(),
            subject.operator,
            this.copyRvalue(subject.operand)
        );
    }

    public Rvalue
    copyBinaryOperation(BinaryOperation subject) throws CompileException {
        return new BinaryOperation(
            subject.getLocation(),
            this.copyRvalue(subject.lhs),
            subject.operator,
            this.copyRvalue(subject.rhs)
        );
    }

    public Rvalue
    copyCast(Cast subject) throws CompileException {
        return new Cast(subject.getLocation(), this.copyType(subject.targetType), this.copyRvalue(subject.value));
    }

    public Rvalue
    copyClassLiteral(ClassLiteral subject) throws CompileException {
        return new ClassLiteral(subject.getLocation(), this.copyType(subject.type));
    }

    public Rvalue
    copyConditionalExpression(ConditionalExpression subject) throws CompileException {
        return new ConditionalExpression(
            subject.getLocation(),
            this.copyRvalue(subject.lhs),
            this.copyRvalue(subject.mhs),
            this.copyRvalue(subject.rhs)
        );
    }

    public Rvalue
    copyCrement(Crement subject) throws CompileException {
        return (
            subject.pre
            ? new Crement(subject.getLocation(), subject.operator, this.copyLvalue(subject.operand))
            : new Crement(subject.getLocation(), this.copyLvalue(subject.operand), subject.operator)
        );
    }

    public Rvalue
    copyInstanceof(Instanceof subject) throws CompileException {
        return new Instanceof(subject.getLocation(), this.copyRvalue(subject.lhs), this.copyType(subject.rhs));
    }

    public Rvalue
    copyMethodInvocation(MethodInvocation subject) throws CompileException {
        return new MethodInvocation(
            subject.getLocation(),
            this.copyOptionalAtom(subject.target),
            subject.methodName,
            this.copyRvalues(subject.arguments)
        );
    }

    public Rvalue
    copySuperclassMethodInvocation(SuperclassMethodInvocation subject) throws CompileException {
        return new SuperclassMethodInvocation(
            subject.getLocation(),
            subject.methodName,
            this.copyRvalues(subject.arguments)
        );
    }

    public Rvalue
    copyIntegerLiteral(IntegerLiteral subject) throws CompileException {
        return new IntegerLiteral(subject.getLocation(), subject.value);
    }

    public Rvalue
    copyFloatingPointLiteral(FloatingPointLiteral subject) throws CompileException {
        return new FloatingPointLiteral(subject.getLocation(), subject.value);
    }

    public Rvalue
    copyBooleanLiteral(BooleanLiteral subject) throws CompileException {
        return new BooleanLiteral(subject.getLocation(), subject.value);
    }

    public Rvalue
    copyCharacterLiteral(CharacterLiteral subject) throws CompileException {
        return new CharacterLiteral(subject.getLocation(), subject.value);
    }

    public Rvalue
    copyStringLiteral(StringLiteral subject) throws CompileException {
        return new StringLiteral(subject.getLocation(), subject.value);
    }

    public Rvalue
    copyNullLiteral(NullLiteral subject) throws CompileException {
        return new NullLiteral(subject.getLocation());
    }

    public Rvalue
    copySimpleLiteral(SimpleConstant subject) throws CompileException { throw new AssertionError(); }

    public Rvalue
    copyNewAnonymousClassInstance(NewAnonymousClassInstance subject) throws CompileException {
        return new NewAnonymousClassInstance(
            subject.getLocation(),
            this.copyOptionalRvalue(subject.qualification),
            this.copyAnonymousClassDeclaration(subject.anonymousClassDeclaration),
            this.copyRvalues(subject.arguments)
        );
    }

    public Rvalue
    copyNewArray(NewArray subject) throws CompileException {
        return new NewArray(
            subject.getLocation(),
            this.copyType(subject.type),
            this.copyRvalues(subject.dimExprs),
            subject.dims
        );
    }

    public Rvalue
    copyNewInitializedArray(NewInitializedArray subject) throws CompileException {
        return new NewInitializedArray(
            subject.getLocation(),
            this.copyOptionalArrayType(subject.arrayType),
            this.copyArrayInitializer(subject.arrayInitializer)
        );
    }

    public ArrayInitializer
    copyArrayInitializer(ArrayInitializer subject) throws CompileException {
        return new ArrayInitializer(subject.getLocation(), this.copyArrayInitializerOrRvalues(subject.values));
    }

    public Rvalue
    copyNewClassInstance(NewClassInstance subject) throws CompileException {
        return (
            subject.type != null
            ? new NewClassInstance(
                subject.getLocation(),
                this.copyOptionalRvalue(subject.qualification),
                this.copyType(DeepCopier.assertNotNull(subject.type)),
                this.copyRvalues(subject.arguments)
            )
            : new NewClassInstance(
                subject.getLocation(),
                this.copyOptionalRvalue(subject.qualification),
                DeepCopier.assertNotNull(subject.iType),
                this.copyRvalues(subject.arguments)
            )
        );
    }

    public Rvalue
    copyParameterAccess(ParameterAccess pa) throws CompileException { return this.copyRvalue(pa); }

    public Rvalue
    copyQualifiedThisReference(QualifiedThisReference subject) throws CompileException {
        return new QualifiedThisReference(subject.getLocation(), this.copyType(subject.qualification));
    }

    public Rvalue
    copyThisReference(ThisReference subject) throws CompileException {
        return new ThisReference(subject.getLocation());
    }

    public Rvalue
    copyLambdaExpression(LambdaExpression subject) {
        return new LambdaExpression(subject.getLocation(), subject.parameters, subject.body);
    }

    public Rvalue
    copyArrayCreationReference(ArrayCreationReference subject) throws CompileException {
        return new ArrayCreationReference(subject.getLocation(), this.copyArrayType(subject.type));
    }

    public Rvalue
    copyClassInstanceCreationReference(ClassInstanceCreationReference subject) throws CompileException {
        return new ClassInstanceCreationReference(
            subject.getLocation(),
            this.copyType(subject.type),
            this.copyOptionalTypeArguments(subject.typeArguments)
        );
    }

    public Rvalue
    copyMethodReference(MethodReference subject) throws CompileException {
        return new MethodReference(subject.getLocation(), this.copyAtom(subject.lhs), subject.methodName);
    }

    public ArrayType
    copyArrayType(ArrayType subject) throws CompileException {
        return new ArrayType(this.copyType(subject.componentType));
    }

    public Type
    copyPrimitiveType(PrimitiveType bt) throws CompileException {
        return new PrimitiveType(bt.getLocation(), bt.primitive);
    }

    public ReferenceType
    copyReferenceType(ReferenceType subject) throws CompileException {
        return new ReferenceType(
            subject.getLocation(),
            this.copyAnnotations(subject.annotations),
            subject.identifiers,
            this.copyOptionalTypeArguments(subject.typeArguments)
        );
    }

    public Type
    copyRvalueMemberType(RvalueMemberType subject) throws CompileException {
        return new RvalueMemberType(subject.getLocation(), this.copyRvalue(subject.rvalue), subject.identifier);
    }

    public Type
    copySimpleType(SimpleType st) throws CompileException { return new SimpleType(st.getLocation(), st.iType); }

    public ConstructorInvocation
    copyAlternateConstructorInvocation(AlternateConstructorInvocation subject) throws CompileException {
        return new AlternateConstructorInvocation(subject.getLocation(), this.copyRvalues(subject.arguments));
    }

    public ConstructorInvocation
    copySuperConstructorInvocation(SuperConstructorInvocation subject) throws CompileException {
        return new SuperConstructorInvocation(
            subject.getLocation(),
            this.copyOptionalRvalue(subject.qualification),
            this.copyRvalues(subject.arguments)
        );
    }

    public Lvalue
    copyAmbiguousName(AmbiguousName subject) throws CompileException {
        return new AmbiguousName(subject.getLocation(), Arrays.copyOf(subject.identifiers, subject.n));
    }

    public Lvalue
    copyArrayAccessExpression(ArrayAccessExpression subject) throws CompileException {
        return new ArrayAccessExpression(
            subject.getLocation(),
            this.copyRvalue(subject.lhs),
            this.copyRvalue(subject.index)
        );
    }

    public Lvalue
    copyFieldAccess(FieldAccess subject) throws CompileException {
        return new FieldAccess(subject.getLocation(), this.copyAtom(subject.lhs), subject.field);
    }

    public Lvalue
    copyFieldAccessExpression(FieldAccessExpression subject) throws CompileException {
        return new FieldAccessExpression(subject.getLocation(), this.copyAtom(subject.lhs), subject.fieldName);
    }

    public Lvalue
    copySuperclassFieldAccessExpression(SuperclassFieldAccessExpression subject) throws CompileException {
        return new SuperclassFieldAccessExpression(
            subject.getLocation(),
            this.copyOptionalType(subject.qualification),
            subject.fieldName
        );
    }

    public Lvalue
    copyLocalVariableAccess(LocalVariableAccess subject) throws CompileException {
        throw new AssertionError();
    }

    public Lvalue
    copyParenthesizedExpression(ParenthesizedExpression subject) throws CompileException {
        return new ParenthesizedExpression(subject.getLocation(), this.copyRvalue(subject.value));
    }

    public ElementValue
    copyElementValueArrayInitializer(ElementValueArrayInitializer subject) throws CompileException {
        return new ElementValueArrayInitializer(this.copyElementValues(subject.elementValues), subject.getLocation());
    }

    public Annotation
    copySingleElementAnnotation(SingleElementAnnotation subject) throws CompileException {
        return new SingleElementAnnotation(
            this.copyReferenceType((ReferenceType) subject.type),
            this.copyElementValue(subject.elementValue)
        );
    }

    public Annotation
    copyNormalAnnotation(NormalAnnotation subject) throws CompileException {
        return new NormalAnnotation(
            this.copyReferenceType((ReferenceType) subject.type),
            this.copyElementValuePairs(subject.elementValuePairs)
        );
    }

    public  ElementValuePair
    copyElementValuePair(ElementValuePair subject) throws CompileException {
        return new ElementValuePair(subject.identifier, this.copyElementValue(subject.elementValue));
    }

    public Annotation
    copyMarkerAnnotation(MarkerAnnotation subject) throws CompileException {
        return new MarkerAnnotation(this.copyType(subject.type));
    }

    public FormalParameters
    copyFormalParameters(FunctionDeclarator.FormalParameters subject) throws CompileException {
        return new FormalParameters(
            subject.getLocation(),
            this.copyFormalParameters(subject.parameters),
            subject.variableArity
        );
    }

    public FunctionDeclarator.FormalParameter
    copyFormalParameter(FunctionDeclarator.FormalParameter subject) throws CompileException {

        return new FormalParameter(
            subject.getLocation(),
            this.copyModifiers(subject.modifiers),
            this.copyType(subject.type),
            subject.name
        );
    }

    public CatchParameter
    copyCatchParameter(CatchParameter subject) throws CompileException {
        return new CatchParameter(subject.getLocation(), subject.finaL, this.copyTypes(subject.types), subject.name);
    }

    public EnumConstant
    copyEnumConstant(EnumConstant subject) throws CompileException {

        EnumConstant result = new EnumConstant(
            subject.getLocation(),
            subject.docComment,
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyOptionalRvalues(subject.arguments)
        );

        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }

        return result;
    }

    public TypeDeclaration
    copyPackageMemberEnumDeclaration(PackageMemberEnumDeclaration subject) throws CompileException {

        PackageMemberEnumDeclaration result = new PackageMemberEnumDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyTypes(subject.implementedTypes)
        );

        for (EnumConstant ec : subject.getConstants()) {
            result.addConstant(this.copyEnumConstant(ec));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }
        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }

        return result;
    }

    public MemberTypeDeclaration
    copyMemberEnumDeclaration(MemberEnumDeclaration subject) throws CompileException {

        MemberEnumDeclaration result = new MemberEnumDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name,
            this.copyTypes(subject.implementedTypes)
        );

        for (EnumConstant ec : subject.getConstants()) {
            result.addConstant(this.copyEnumConstant(ec));
        }
        for (ConstructorDeclarator cd : subject.constructors) {
            result.addConstructor(this.copyConstructorDeclarator(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }
        for (FieldDeclarationOrInitializer fdoi : subject.fieldDeclarationsAndInitializers) {
            result.addFieldDeclarationOrInitializer(this.copyFieldDeclarationOrInitializer(fdoi));
        }

        return result;
    }

    public TypeDeclaration
    copyPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration subject) throws CompileException {

        PackageMemberAnnotationTypeDeclaration result = new PackageMemberAnnotationTypeDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name
        );

        for (FieldDeclaration fd : subject.constantDeclarations) {
            result.addConstantDeclaration(this.copyFieldDeclaration(fd));
        }

        return result;
    }

    public MemberTypeDeclaration
    copyMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration subject) throws CompileException {

        MemberAnnotationTypeDeclaration result = new MemberAnnotationTypeDeclaration(
            subject.getLocation(),
            subject.getDocComment(),
            this.copyModifiers(subject.getModifiers()),
            subject.name
        );

        for (FieldDeclaration cd : subject.constantDeclarations) {
            result.addConstantDeclaration(this.copyFieldDeclaration(cd));
        }
        for (MethodDeclarator md : subject.getMethodDeclarations()) {
            result.addDeclaredMethod(this.copyMethodDeclarator(md));
        }
        for (MemberTypeDeclaration mtd : subject.getMemberTypeDeclarations()) {
            result.addMemberTypeDeclaration(this.copyMemberTypeDeclaration(mtd));
        }

        return result;
    }

    public TryStatement.Resource
    copyLocalVariableDeclaratorResource(LocalVariableDeclaratorResource subject) throws CompileException {
        return new LocalVariableDeclaratorResource(
            subject.getLocation(),
            this.copyModifiers(subject.modifiers),
            this.copyType(subject.type),
            this.copyVariableDeclarator(subject.variableDeclarator)
        );
    }

    public TryStatement.Resource
    copyVariableAccessResource(VariableAccessResource subject) throws CompileException {
        return new VariableAccessResource(subject.getLocation(), this.copyRvalue(subject.variableAccess));
    }

    public Modifier[]
    copyModifiers(Modifier[] subject) throws CompileException {
        Modifier[] result = new Modifier[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = this.copyModifier(subject[i]);
        return result;
    }

    public Modifier
    copyModifier(Modifier modifier) throws CompileException {
        return DeepCopier.assertNotNull(modifier.accept(this.modifierCopier));
    }

    public AccessModifier
    copyAccessModifier(AccessModifier am) { return new AccessModifier(am.keyword, am.getLocation()); }

    public TypeParameter
    copyTypeParameter(TypeParameter subject) throws CompileException {
        return new TypeParameter(subject.name, this.copyOptionalReferenceTypes(subject.bound));
    }

    // -----------------------------------

    private static <T> T
    assertNotNull(@Nullable T subject) {
        assert subject != null;
        return subject;
    }
}
