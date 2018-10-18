
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY CompileExceptionPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, CompileExceptionEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
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
import org.codehaus.janino.InternalCompilerException;
import org.codehaus.janino.Java.AbstractTypeBodyDeclaration;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.AmbiguousName;
import org.codehaus.janino.Java.Annotation;
import org.codehaus.janino.Java.AnonymousClassDeclaration;
import org.codehaus.janino.Java.ArrayAccessExpression;
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
import org.codehaus.janino.Java.BooleanRvalue;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.BreakableStatement;
import org.codehaus.janino.Java.Cast;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.CharacterLiteral;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinuableStatement;
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
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LabeledStatement;
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
import org.codehaus.janino.Java.Modifiers;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NormalAnnotation;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.Package;
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
import org.codehaus.janino.Java.TryStatement.VariableAccessResource;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Visitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.TryStatementResourceVisitor;

/**
 * Creates deep copies of AST elements.
 */
public
class DeepCopier {

    public
    DeepCopier() {}

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link ImportDeclaration}.
     */
    private final Visitor.ImportVisitor<ImportDeclaration, CompileException>
    importCopier = new Visitor.ImportVisitor<ImportDeclaration, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:4
        @Override @Nullable public ImportDeclaration visitSingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid)          throws CompileException { return DeepCopier.this.copySingleTypeImportDeclaration(stid);      }
        @Override @Nullable public ImportDeclaration visitTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd)     throws CompileException { return DeepCopier.this.copyTypeImportOnDemandDeclaration(tiodd);   }
        @Override @Nullable public ImportDeclaration visitSingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration ssid)      throws CompileException { return DeepCopier.this.copySingleStaticImportDeclaration(ssid);    }
        @Override @Nullable public ImportDeclaration visitStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws CompileException { return DeepCopier.this.copyStaticImportOnDemandDeclaration(siodd); }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link TypeDeclaration}.
     */
    private final Visitor.TypeDeclarationVisitor<TypeDeclaration, CompileException>
    typeDeclarationCopier = new Visitor.TypeDeclarationVisitor<TypeDeclaration, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:11
        @Override @Nullable public TypeDeclaration visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws CompileException { return DeepCopier.this.copyAnonymousClassDeclaration(acd);                 }
        @Override @Nullable public TypeDeclaration visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws CompileException { return DeepCopier.this.copyLocalClassDeclaration(lcd);                     }
        @Override @Nullable public TypeDeclaration visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)                    throws CompileException { return DeepCopier.this.copyPackageMemberClassDeclaration(pmcd);           }
        @Override @Nullable public TypeDeclaration visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws CompileException { return DeepCopier.this.copyPackageMemberInterfaceDeclaration(pmid);        }
        @Override @Nullable public TypeDeclaration visitEnumConstant(EnumConstant ec)                                                        throws CompileException { return DeepCopier.this.copyEnumConstant(ec);                               }
        @Override @Nullable public TypeDeclaration visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws CompileException { return DeepCopier.this.copyPackageMemberEnumDeclaration(pmed);             }
        @Override @Nullable public TypeDeclaration visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd)                throws CompileException { return DeepCopier.this.copyMemberAnnotationTypeDeclaration(matd);          }
        @Override @Nullable public TypeDeclaration visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws CompileException { return DeepCopier.this.copyPackageMemberAnnotationTypeDeclaration(pmatd);  }
        @Override @Nullable public TypeDeclaration visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws CompileException { return DeepCopier.this.copyMemberEnumDeclaration(med);                     }
        @Override @Nullable public TypeDeclaration visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws CompileException { return DeepCopier.this.copyMemberInterfaceDeclaration(mid);                }
        @Override @Nullable public TypeDeclaration visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws CompileException { return DeepCopier.this.copyMemberClassDeclaration(mcd);                    }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link Rvalue}.
     */
    private final Visitor.RvalueVisitor<Rvalue, CompileException>
    rvalueCopier = new Visitor.RvalueVisitor<Rvalue, CompileException>() {

        @Override public Rvalue
        visitLvalue(Lvalue lv) throws CompileException {
            return DeepCopier.assertNotNull(lv.accept(new Visitor.LvalueVisitor<Lvalue, CompileException>() {

                // SUPPRESS CHECKSTYLE LineLength:7
                @Override @Nullable public Lvalue visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return DeepCopier.this.copyAmbiguousName(an);                       }
                @Override @Nullable public Lvalue visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return DeepCopier.this.copyArrayAccessExpression(aae);              }
                @Override @Nullable public Lvalue visitFieldAccess(FieldAccess fa)                                            throws CompileException { return DeepCopier.this.copyFieldAccess(fa);                         }
                @Override @Nullable public Lvalue visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return DeepCopier.this.copyFieldAccessExpression(fae);              }
                @Override @Nullable public Lvalue visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return DeepCopier.this.copySuperclassFieldAccessExpression(scfae);  }
                @Override @Nullable public Lvalue visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { return DeepCopier.this.copyLocalVariableAccess(lva);                }
                @Override @Nullable public Lvalue visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return DeepCopier.this.copyParenthesizedExpression(pe);             }
            }));
        }

        // SUPPRESS CHECKSTYLE LineLength:25
        @Override public Rvalue visitArrayLength(ArrayLength al)                                throws CompileException { return DeepCopier.this.copyArrayLength(al);                         }
        @Override public Rvalue visitAssignment(Assignment a)                                   throws CompileException { return DeepCopier.this.copyAssignment(a);                           }
        @Override public Rvalue visitUnaryOperation(UnaryOperation uo)                          throws CompileException { return DeepCopier.this.copyUnaryOperation(uo);                      }
        @Override public Rvalue visitBinaryOperation(BinaryOperation bo)                        throws CompileException { return DeepCopier.this.copyBinaryOperation(bo);                     }
        @Override public Rvalue visitCast(Cast c)                                               throws CompileException { return DeepCopier.this.copyCast(c);                                 }
        @Override public Rvalue visitClassLiteral(ClassLiteral cl)                              throws CompileException { return DeepCopier.this.copyClassLiteral(cl);                        }
        @Override public Rvalue visitConditionalExpression(ConditionalExpression ce)            throws CompileException { return DeepCopier.this.copyConditionalExpression(ce);               }
        @Override public Rvalue visitCrement(Crement c)                                         throws CompileException { return DeepCopier.this.copyCrement(c);                              }
        @Override public Rvalue visitInstanceof(Instanceof io)                                  throws CompileException { return DeepCopier.this.copyInstanceof(io);                          }
        @Override public Rvalue visitMethodInvocation(MethodInvocation mi)                      throws CompileException { return DeepCopier.this.copyMethodInvocation(mi);                    }
        @Override public Rvalue visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) throws CompileException { return DeepCopier.this.copySuperclassMethodInvocation(smi);         }
        @Override public Rvalue visitIntegerLiteral(IntegerLiteral il)                          throws CompileException { return DeepCopier.this.copyIntegerLiteral(il);                      }
        @Override public Rvalue visitFloatingPointLiteral(FloatingPointLiteral fpl)             throws CompileException { return DeepCopier.this.copyFloatingPointLiteral(fpl);               }
        @Override public Rvalue visitBooleanLiteral(BooleanLiteral bl)                          throws CompileException { return DeepCopier.this.copyBooleanLiteral(bl);                      }
        @Override public Rvalue visitCharacterLiteral(CharacterLiteral cl)                      throws CompileException { return DeepCopier.this.copyCharacterLiteral(cl);                    }
        @Override public Rvalue visitStringLiteral(StringLiteral sl)                            throws CompileException { return DeepCopier.this.copyStringLiteral(sl);                       }
        @Override public Rvalue visitNullLiteral(NullLiteral nl)                                throws CompileException { return DeepCopier.this.copyNullLiteral(nl);                         }
        @Override public Rvalue visitSimpleConstant(SimpleConstant sl)                          throws CompileException { return DeepCopier.this.copySimpleLiteral(sl);                       }
        @Override public Rvalue visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  throws CompileException { return DeepCopier.this.copyNewAnonymousClassInstance(naci);         }
        @Override public Rvalue visitNewArray(NewArray na)                                      throws CompileException { return DeepCopier.this.copyNewArray(na);                            }
        @Override public Rvalue visitNewInitializedArray(NewInitializedArray nia)               throws CompileException { return DeepCopier.this.copyNewInitializedArray(nia);                }
        @Override public Rvalue visitNewClassInstance(NewClassInstance nci)                     throws CompileException { return DeepCopier.this.copyNewClassInstance(nci);                   }
        @Override public Rvalue visitParameterAccess(ParameterAccess pa)                        throws CompileException { return DeepCopier.this.copyParameterAccess(pa);                     }
        @Override public Rvalue visitQualifiedThisReference(QualifiedThisReference qtr)         throws CompileException { return DeepCopier.this.copyQualifiedThisReference(qtr);             }
        @Override public Rvalue visitThisReference(ThisReference tr)                            throws CompileException { return DeepCopier.this.copyThisReference(tr);                       }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link TypeBodyDeclaration}.
     */
    private final Visitor.TypeBodyDeclarationVisitor<TypeBodyDeclaration, CompileException>
    typeBodyDeclarationCopier = new Visitor.TypeBodyDeclarationVisitor<TypeBodyDeclaration, CompileException>() {

        @Override public TypeBodyDeclaration
        visitFunctionDeclarator(FunctionDeclarator fd) throws CompileException {
            return DeepCopier.assertNotNull(fd.accept(new Visitor.FunctionDeclaratorVisitor<FunctionDeclarator, CompileException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override public FunctionDeclarator visitConstructorDeclarator(ConstructorDeclarator cd) throws CompileException { return DeepCopier.this.copyConstructorDeclarator(cd);  }
                @Override public FunctionDeclarator visitMethodDeclarator(MethodDeclarator md)           throws CompileException { return DeepCopier.this.copyMethodDeclarator(md);       }
            }));
        }

        // SUPPRESS CHECKSTYLE LineLength:6
        @Override public TypeBodyDeclaration visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws CompileException { return DeepCopier.this.copyMemberAnnotationTypeDeclaration(matd);  }
        @Override public TypeBodyDeclaration visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)            throws CompileException { return DeepCopier.this.copyMemberInterfaceDeclaration(mid);        }
        @Override public TypeBodyDeclaration visitMemberClassDeclaration(MemberClassDeclaration mcd)                    throws CompileException { return DeepCopier.this.copyMemberClassDeclaration(mcd);            }
        @Override public TypeBodyDeclaration visitMemberEnumDeclaration(MemberEnumDeclaration med)                      throws CompileException { return DeepCopier.this.copyMemberEnumDeclaration(med);             }
        @Override public TypeBodyDeclaration visitInitializer(Initializer i)                                            throws CompileException { return DeepCopier.this.copyInitializer(i);                         }
        @Override public TypeBodyDeclaration visitFieldDeclaration(FieldDeclaration fd)                                 throws CompileException { return DeepCopier.this.copyFieldDeclaration(fd);                   }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link BlockStatement}.
     */
    private final Visitor.BlockStatementVisitor<BlockStatement, CompileException>
    blockStatementCopier = new BlockStatementVisitor<BlockStatement, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:23
        @Override public BlockStatement visitInitializer(Initializer i)                                                throws CompileException { return DeepCopier.this.copyInitializer(i);                           }
        @Override public BlockStatement visitFieldDeclaration(FieldDeclaration fd)                                     throws CompileException { return DeepCopier.this.copyFieldDeclaration(fd);                     }
        @Override public BlockStatement visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return DeepCopier.this.copyLabeledStatement(ls);                     }
        @Override public BlockStatement visitBlock(Block b)                                                            throws CompileException { return DeepCopier.this.copyBlock(b);                                 }
        @Override public BlockStatement visitExpressionStatement(ExpressionStatement es)                               throws CompileException { return DeepCopier.this.copyExpressionStatement(es);                  }
        @Override public BlockStatement visitIfStatement(IfStatement is)                                               throws CompileException { return DeepCopier.this.copyIfStatement(is);                          }
        @Override public BlockStatement visitForStatement(ForStatement fs)                                             throws CompileException { return DeepCopier.this.copyForStatement(fs);                         }
        @Override public BlockStatement visitForEachStatement(ForEachStatement fes)                                    throws CompileException { return DeepCopier.this.copyForEachStatement(fes);                    }
        @Override public BlockStatement visitWhileStatement(WhileStatement ws)                                         throws CompileException { return DeepCopier.this.copyWhileStatement(ws);                       }
        @Override public BlockStatement visitTryStatement(TryStatement ts)                                             throws CompileException { return DeepCopier.this.copyTryStatement(ts);                         }
        @Override public BlockStatement visitSwitchStatement(SwitchStatement ss)                                       throws CompileException { return DeepCopier.this.copySwitchStatement(ss);                      }
        @Override public BlockStatement visitSynchronizedStatement(SynchronizedStatement ss)                           throws CompileException { return DeepCopier.this.copySynchronizedStatement(ss);                }
        @Override public BlockStatement visitDoStatement(DoStatement ds)                                               throws CompileException { return DeepCopier.this.copyDoStatement(ds);                          }
        @Override public BlockStatement visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return DeepCopier.this.copyLocalVariableDeclarationStatement(lvds);  }
        @Override public BlockStatement visitReturnStatement(ReturnStatement rs)                                       throws CompileException { return DeepCopier.this.copyReturnStatement(rs);                      }
        @Override public BlockStatement visitThrowStatement(ThrowStatement ts)                                         throws CompileException { return DeepCopier.this.copyThrowStatement(ts);                       }
        @Override public BlockStatement visitBreakStatement(BreakStatement bs)                                         throws CompileException { return DeepCopier.this.copyBreakStatement(bs);                       }
        @Override public BlockStatement visitContinueStatement(ContinueStatement cs)                                   throws CompileException { return DeepCopier.this.copyContinueStatement(cs);                    }
        @Override public BlockStatement visitAssertStatement(AssertStatement as)                                       throws CompileException { return DeepCopier.this.copyAssertStatement(as);                      }
        @Override public BlockStatement visitEmptyStatement(EmptyStatement es)                                         throws CompileException { return DeepCopier.this.copyEmptyStatement(es);                       }
        @Override public BlockStatement visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws CompileException { return DeepCopier.this.copyLocalClassDeclarationStatement(lcds);     }
        @Override public BlockStatement visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws CompileException { return DeepCopier.this.copyAlternateConstructorInvocation(aci);      }
        @Override public BlockStatement visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws CompileException { return DeepCopier.this.copySuperConstructorInvocation(sci);          }
    };

    private final Visitor.TypeVisitor<Type, CompileException>
    typeCopier = new Visitor.TypeVisitor<Type, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:5
        @Override public Type visitArrayType(ArrayType at)                throws CompileException { return DeepCopier.this.copyArrayType(at);          }
        @Override public Type visitPrimitiveType(PrimitiveType bt)        throws CompileException { return DeepCopier.this.copyPrimitiveType(bt);      }
        @Override public Type visitReferenceType(ReferenceType rt)        throws CompileException { return DeepCopier.this.copyReferenceType(rt);      }
        @Override public Type visitRvalueMemberType(RvalueMemberType rmt) throws CompileException { return DeepCopier.this.copyRvalueMemberType(rmt);  }
        @Override public Type visitSimpleType(SimpleType st)              throws CompileException { return DeepCopier.this.copySimpleType(st);         }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link Atom}.
     */
    private final Visitor.AtomVisitor<Atom, CompileException>
    atomCopier = new Visitor.AtomVisitor<Atom, CompileException>() {

        @Override public Atom
        visitRvalue(Rvalue rv) throws CompileException {
            return DeepCopier.assertNotNull(rv.accept(DeepCopier.this.rvalueCopier));
        }

        @Override public Atom
        visitPackage(Package p) throws CompileException {
            return DeepCopier.this.copyPackage(p);
        }

        @Override public Atom
        visitType(Type t) throws CompileException {
            return DeepCopier.this.copyType(t);
        }

        @Override public Atom
        visitConstructorInvocation(ConstructorInvocation ci) throws CompileException {

            return DeepCopier.assertNotNull(ci.accept(new Visitor.ConstructorInvocationVisitor<Atom, CompileException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override public Atom visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) throws CompileException { return DeepCopier.this.copyAlternateConstructorInvocation(aci);  }
                @Override public Atom visitSuperConstructorInvocation(SuperConstructorInvocation sci)         throws CompileException { return DeepCopier.this.copySuperConstructorInvocation(sci);      }
            }));
        }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link ElementValue}.
     */
    private final ElementValueVisitor<ElementValue, CompileException>
    elementValueCopier = new ElementValueVisitor<ElementValue, CompileException>() {

        @Override public ElementValue
        visitRvalue(Rvalue rv) throws CompileException {
            return DeepCopier.assertNotNull(rv.accept(DeepCopier.this.rvalueCopier));
        }

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public ElementValue visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws CompileException { return DeepCopier.this.copyElementValueArrayInitializer(evai);  }
        @Override public ElementValue visitAnnotation(Annotation a)                                        throws CompileException { return DeepCopier.this.copyAnnotation(a);                       }
    };

    /**
     * Invokes the "{@code copy*()}" method for the concrete {@link Annotation}.
     */
    private final AnnotationVisitor<Annotation, CompileException>
    annotationCopier = new AnnotationVisitor<Annotation, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:3
        @Override public Annotation visitMarkerAnnotation(MarkerAnnotation ma)                throws CompileException { return DeepCopier.this.copyMarkerAnnotation(ma);          }
        @Override public Annotation visitNormalAnnotation(NormalAnnotation na)                throws CompileException { return DeepCopier.this.copyNormalAnnotation(na);          }
        @Override public Annotation visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException { return DeepCopier.this.copySingleElementAnnotation(sea);  }
    };

    private final TryStatementResourceVisitor<TryStatement.Resource, CompileException>
    resourceCopier = new TryStatementResourceVisitor<TryStatement.Resource, CompileException>() {

        // SUPPRESS CHECKSTYLE LineLength:2
        @Override public TryStatement.Resource visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws CompileException { return DeepCopier.this.copyLocalVariableDeclaratorResource(lvdr);   }
        @Override public TryStatement.Resource visitVariableAccessResource(VariableAccessResource var)                    throws CompileException { return DeepCopier.this.copyVariableAccessResource(var);             }
    };

//    public void
//    visitImportDeclaration(CompilationUnit.ImportDeclaration id) throws CompileException {
//        id.accept(this.importCopier);
//    }
//
//    public void
//    visitTypeDeclaration(TypeDeclaration td) throws CompileException {
//        td.accept(this.typeDeclarationCopier);
//    }
//
//    public void
//    visitTypeBodyDeclaration(TypeBodyDeclaration tbd) throws CompileException {
//        tbd.accept(this.typeBodyDeclarationCopier);
//    }
//
//    public void
//    visitBlockStatement(BlockStatement bs) throws CompileException {
//        bs.accept(this.blockStatementCopier);
//    }
//
//    public void
//    visitAtom(Atom a) throws CompileException {
//        a.accept(this.atomCopier);
//    }
//
//    public void
//    visitElementValue(ElementValue ev) throws CompileException { ev.accept(this.elementValueCopier); }
//
//    public void
//    visitAnnotation(Annotation a) throws CompileException { a.accept(this.annotationCopier); }

    // These may be overridden by derived classes.

    public void
    copyCompilationUnit(CompilationUnit cu) throws CompileException {

        // The optionalPackageDeclaration is considered an integral part of
        // the compilation unit and is thus not copied.

        for (CompilationUnit.ImportDeclaration id : cu.importDeclarations) {
            id.accept(this.importCopier);
        }

        for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            pmtd.accept(this.typeDeclarationCopier);
        }
    }

    /**
     * @throws CompileException
     */
    public ImportDeclaration
    copySingleTypeImportDeclaration(CompilationUnit.SingleTypeImportDeclaration stid) throws CompileException {
        return new CompilationUnit.SingleTypeImportDeclaration(
            stid.getLocation(),
            stid.identifiers.clone()
        );
    }

    /**
     * @throws CompileException
     */
    public ImportDeclaration
    copyTypeImportOnDemandDeclaration(CompilationUnit.TypeImportOnDemandDeclaration tiodd) throws CompileException {
        return new CompilationUnit.TypeImportOnDemandDeclaration(tiodd.getLocation(), tiodd.identifiers.clone());
    }

    /**
     * @throws CompileException
     */
    public ImportDeclaration
    copySingleStaticImportDeclaration(CompilationUnit.SingleStaticImportDeclaration stid) throws CompileException {
        return new CompilationUnit.SingleStaticImportDeclaration(stid.getLocation(), stid.identifiers.clone());
    }

    /**
     * @throws CompileException
     */
    public ImportDeclaration
    copyStaticImportOnDemandDeclaration(CompilationUnit.StaticImportOnDemandDeclaration siodd) throws CompileException {
        return new CompilationUnit.StaticImportOnDemandDeclaration(siodd.getLocation(), siodd.identifiers.clone());
    }

    public AnonymousClassDeclaration
    copyAnonymousClassDeclaration(AnonymousClassDeclaration acd) throws CompileException {
        return new AnonymousClassDeclaration(acd.getLocation(), this.copyType(acd.baseType));
    }

    public TypeDeclaration
    copyLocalClassDeclaration(LocalClassDeclaration lcd) throws CompileException {
        return new LocalClassDeclaration(
            lcd.getLocation(),
            lcd.getDocComment(),
            new Modifiers(lcd.getModifierFlags(), this.copyAnnotations(lcd.getAnnotations()), false),
            lcd.name,
            lcd.getOptionalTypeParameters(),
            lcd.optionalExtendedType,
            lcd.implementedTypes
        );
    }

    public TypeDeclaration
    copyPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd) throws CompileException {
        return new PackageMemberClassDeclaration(
            pmcd.getLocation(),
            pmcd.getDocComment(),
            new Modifiers(pmcd.getModifierFlags(), this.copyAnnotations(pmcd.getAnnotations()), false),
            pmcd.name,
            pmcd.getOptionalTypeParameters(),
            pmcd.optionalExtendedType,
            pmcd.implementedTypes
        );
    }

    public MemberTypeDeclaration
    copyMemberInterfaceDeclaration(MemberInterfaceDeclaration mid) throws CompileException {
        return new MemberInterfaceDeclaration(
            mid.getLocation(),
            mid.getDocComment(),
            new Modifiers(mid.getModifierFlags(), this.copyAnnotations(mid.getAnnotations()), false),
            mid.name,
            mid.getOptionalTypeParameters(),
            mid.extendedTypes
        );
    }

    public TypeDeclaration
    copyPackageMemberInterfaceDeclaration(final PackageMemberInterfaceDeclaration pmid) throws CompileException {
        return new PackageMemberInterfaceDeclaration(
            pmid.getLocation(),
            pmid.getDocComment(),
            new Modifiers(pmid.getModifierFlags(), this.copyAnnotations(pmid.getAnnotations()), false),
            pmid.name,
            pmid.getOptionalTypeParameters(),
            pmid.extendedTypes
        );
    }

    public MemberTypeDeclaration
    copyMemberClassDeclaration(MemberClassDeclaration mcd) throws CompileException {
        return new MemberClassDeclaration(
            mcd.getLocation(),
            mcd.getDocComment(),
            new Modifiers(mcd.getModifierFlags(), this.copyAnnotations(mcd.getAnnotations()), false),
            mcd.name,
            mcd.getOptionalTypeParameters(),
            mcd.optionalExtendedType,
            mcd.implementedTypes
        );
    }

    public FunctionDeclarator
    copyConstructorDeclarator(ConstructorDeclarator cd) throws CompileException {
        return new ConstructorDeclarator(
            cd.getLocation(),
            cd.getDocComment(),
            this.copyModifiers(cd.modifiers),
            cd.formalParameters,
            cd.thrownExceptions, cd.optionalConstructorInvocation, DeepCopier.assertNotNull(cd.optionalStatements));
    }

    public Initializer
    copyInitializer(Initializer i) throws CompileException {
        return new Initializer(i.getLocation(), i.isStatic(), this.copyBlock(i.block));
    }

    public FunctionDeclarator
    copyMethodDeclarator(MethodDeclarator md) throws CompileException {
        return new MethodDeclarator(
            md.getLocation(),
            md.getDocComment(),
            this.copyModifiers(md.modifiers),
            DeepCopier.copyTypeParameters(md.optionalTypeParameters),
            this.copyType(md.type),
            md.name,
            this.copyFormalParameters(md.formalParameters),
            this.copyTypes(md.thrownExceptions),
            this.copyElementValue(md.defaultValue),
            this.copyStatements(md.optionalStatements)
        );
    }

    public FieldDeclaration
    copyFieldDeclaration(FieldDeclaration fd) throws CompileException {
        return new FieldDeclaration(
            fd.getLocation(),
            fd.getDocComment(),
            this.copyModifiers(fd.modifiers),
            this.copyType(fd.type),
            this.copyVariableDeclarators(fd.variableDeclarators)
        );
    }

    private VariableDeclarator[]
    copyVariableDeclarators(VariableDeclarator[] subject) throws CompileException {
        VariableDeclarator[] result = new VariableDeclarator[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = this.copyVariableDeclarator(subject[i]);
        return result;
    }

    public VariableDeclarator
    copyVariableDeclarator(VariableDeclarator subject) throws CompileException {
        return new VariableDeclarator(
            subject.getLocation(),
            subject.name,
            subject.brackets,
            this.copyArrayInitializerOrRvalue(subject.optionalInitializer)
        );
    }

    private List<BlockStatement>
    copyBlockStatements(List<BlockStatement> subject) throws CompileException {
        List<BlockStatement> result = new ArrayList<BlockStatement>(subject.size());
        for (BlockStatement bs : subject) result.add(this.copyBlockStatement(bs));
        return result;
    }

    private BlockStatement
    copyBlockStatement(BlockStatement subject) throws CompileException {
        return DeepCopier.assertNotNull(subject.accept(this.blockStatementCopier));
    }

    @Nullable private BlockStatement
    copyOptionalBlockStatement(@Nullable BlockStatement subject) throws CompileException {
        return subject == null ? null : this.copyBlockStatement(subject);
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
            this.copyOptionalBlockStatement(fs.optionalInit),
            this.copyOptionalRvalue(fs.optionalCondition),
            this.copyOptionalRvalues(fs.optionalUpdate),
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
        for (TryStatement.Resource r : ts.resources) {
            r.accept(this.resourceCopier);
        }
        ts.body.accept(this.blockStatementCopier);
        for (CatchClause cc : ts.catchClauses) cc.body.accept(this.blockStatementCopier);
        if (ts.finallY != null) ts.finallY.accept(this.blockStatementCopier);
        return this.copyStatement(ts);
    }

    public BlockStatement
    copySwitchStatement(SwitchStatement subject) throws CompileException {
        return new SwitchStatement(subject.getLocation(), this.copyRvalue(subject.condition), this.copySwitchBlockStatementGroups(subject.sbsgs));
    }

    private List<SwitchBlockStatementGroup>
    copySwitchBlockStatementGroups(List<SwitchBlockStatementGroup> subject) throws CompileException {
        List<SwitchBlockStatementGroup> result = new ArrayList<SwitchStatement.SwitchBlockStatementGroup>(subject.size());
        for (SwitchBlockStatementGroup sbgs : subject) result.add(this.copySwitchBlockStatementGroup(sbgs));
        return result;
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
    copySynchronizedStatement(SynchronizedStatement ss) throws CompileException {
        ss.expression.accept(this.rvalueCopier);
        ss.body.accept(this.blockStatementCopier);
        return this.copyStatement(ss);
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
    copyReturnStatement(ReturnStatement rs) throws CompileException {
        if (rs.optionalReturnValue != null) rs.optionalReturnValue.accept(this.rvalueCopier);
        return this.copyStatement(rs);
    }

    public BlockStatement
    copyThrowStatement(ThrowStatement ts) throws CompileException {
        ts.expression.accept(this.rvalueCopier);
        return this.copyStatement(ts);
    }

    public BlockStatement
    copyBreakStatement(BreakStatement bs) throws CompileException { return this.copyStatement(bs); }

    public BlockStatement
    copyContinueStatement(ContinueStatement cs) throws CompileException { return this.copyStatement(cs); }

    public BlockStatement
    copyAssertStatement(AssertStatement as) throws CompileException {
        as.expression1.accept(this.rvalueCopier);
        if (as.optionalExpression2 != null) as.optionalExpression2.accept(this.rvalueCopier);
        return this.copyStatement(as);
    }

    public BlockStatement
    copyEmptyStatement(EmptyStatement es) throws CompileException { return this.copyStatement(es); }

    public BlockStatement
    copyLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) throws CompileException {
        lcds.lcd.accept(this.typeDeclarationCopier);
        return this.copyStatement(lcds);
    }

    /**
     * @throws CompileException
     */
    public Atom
    copyPackage(Package subject) throws CompileException {
        return new Package(subject.getLocation(), subject.name);
    }

    public Rvalue
    copyArrayLength(ArrayLength al) throws CompileException {
        al.lhs.accept(this.rvalueCopier);
        return this.copyRvalue(al);
    }

    public Rvalue
    copyAssignment(Assignment a) throws CompileException {
        a.lhs.accept(this.rvalueCopier);
        a.rhs.accept(this.rvalueCopier);
        return this.copyRvalue(a);
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
        return new ConditionalExpression(subject.getLocation(),
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
            this.copyOptionalAtom(subject.optionalTarget),
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

    /**
     * @throws CompileException
     */
    public Rvalue
    copyIntegerLiteral(IntegerLiteral subject) throws CompileException { return new IntegerLiteral(subject.getLocation(), subject.value); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copyFloatingPointLiteral(FloatingPointLiteral subject) throws CompileException { return new FloatingPointLiteral(subject.getLocation(), subject.value); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copyBooleanLiteral(BooleanLiteral subject) throws CompileException { return new BooleanLiteral(subject.getLocation(), subject.value); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copyCharacterLiteral(CharacterLiteral subject) throws CompileException { return new CharacterLiteral(subject.getLocation(), subject.value); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copyStringLiteral(StringLiteral subject) throws CompileException { return new StringLiteral(subject.getLocation(), subject.value); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copyNullLiteral(NullLiteral subject) throws CompileException { return new NullLiteral(subject.getLocation()); }

    /**
     * @throws CompileException
     */
    public Rvalue
    copySimpleLiteral(SimpleConstant subject) throws CompileException { throw new AssertionError(); }

    public Rvalue
    copyNewAnonymousClassInstance(NewAnonymousClassInstance subject) throws CompileException {
        return new NewAnonymousClassInstance(
            subject.getLocation(),
            this.copyOptionalRvalue(subject.optionalQualification),
            this.copyAnonymousClassDeclaration(subject.anonymousClassDeclaration),
            this.copyRvalues(subject.arguments)
        );
    }

    public Rvalue
    copyNewArray(NewArray na) throws CompileException {
        na.type.accept(this.atomCopier);
        for (Rvalue dimExpr : na.dimExprs) dimExpr.accept(this.rvalueCopier);
        return this.copyRvalue(na);
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

    @Nullable public ArrayInitializer
    copyOptionalArrayInitializer(@Nullable ArrayInitializer subject) throws CompileException {
        return subject == null ? null : this.copyArrayInitializer(subject);
    }

    private ArrayInitializerOrRvalue[]
    copyArrayInitializerOrRvalues(ArrayInitializerOrRvalue[] subject) throws CompileException {
        ArrayInitializerOrRvalue[] result = new ArrayInitializerOrRvalue[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = this.copyArrayInitializerOrRvalue(subject[i]);
        return result;
    }

    @Nullable public ArrayInitializerOrRvalue
    copyArrayInitializerOrRvalue(@Nullable ArrayInitializerOrRvalue subject) throws CompileException {

        if (subject == null) return null;

        if (subject instanceof Rvalue) return this.copyRvalue((Rvalue) subject);

        if (subject instanceof ArrayInitializer) return this.copyArrayInitializer((ArrayInitializer) subject);

        throw new InternalCompilerException(
            "Unexpected array initializer or rvalue class "
            + subject.getClass().getName()
        );
    }

    public Rvalue
    copyNewClassInstance(NewClassInstance nci) throws CompileException {
        if (nci.optionalQualification != null) {
            nci.optionalQualification.accept(this.rvalueCopier);
        }
        if (nci.type != null) nci.type.accept(this.atomCopier);
        for (Rvalue argument : nci.arguments) argument.accept(this.rvalueCopier);
        return this.copyRvalue(nci);
    }

    public Rvalue
    copyParameterAccess(ParameterAccess pa) throws CompileException { return this.copyRvalue(pa); }

    public Rvalue
    copyQualifiedThisReference(QualifiedThisReference qtr) throws CompileException {
        qtr.qualification.accept(this.atomCopier);
        return this.copyRvalue(qtr);
    }

    public Rvalue
    copyThisReference(ThisReference tr) throws CompileException { return this.copyRvalue(tr); }

    public ArrayType
    copyArrayType(ArrayType subject) throws CompileException {
        return new ArrayType(this.copyType(subject.componentType));
    }

    @Nullable private ArrayType
    copyOptionalArrayType(@Nullable ArrayType subject) throws CompileException {
        return subject == null ? null : this.copyArrayType(subject);
    }

    public Type
    copyPrimitiveType(PrimitiveType bt) throws CompileException { return this.copyType(bt); }

    public Type
    copyReferenceType(ReferenceType rt) throws CompileException { return this.copyType(rt); }

    public Type
    copyRvalueMemberType(RvalueMemberType rmt) throws CompileException {
        rmt.rvalue.accept(this.rvalueCopier);
        return this.copyType(rmt);
    }

    public Type
    copySimpleType(SimpleType st) throws CompileException { return this.copyType(st); }

    public ConstructorInvocation
    copyAlternateConstructorInvocation(AlternateConstructorInvocation subject) throws CompileException {
        return new AlternateConstructorInvocation(subject.getLocation(), this.copyRvalues(subject.arguments));
    }

    public ConstructorInvocation
    copySuperConstructorInvocation(SuperConstructorInvocation subject) throws CompileException {
        return new SuperConstructorInvocation(
            subject.getLocation(),
            this.copyOptionalRvalue(subject.optionalQualification),
            this.copyRvalues(subject.arguments)
        );
    }

    /**
     * @throws CompileException
     */
    public Lvalue
    copyAmbiguousName(AmbiguousName subject) throws CompileException {
        return new AmbiguousName(subject.getLocation(), Arrays.copyOf(subject.identifiers, subject.n));
    }

    public Lvalue
    copyArrayAccessExpression(ArrayAccessExpression aae) throws CompileException {
        aae.lhs.accept(this.rvalueCopier);
        aae.index.accept(this.atomCopier);
        return this.copyLvalue(aae);
    }

    public Lvalue
    copyFieldAccess(FieldAccess fa) throws CompileException {
        fa.lhs.accept(this.atomCopier);
        return this.copyLvalue(fa);
    }

    public Lvalue
    copyFieldAccessExpression(FieldAccessExpression fae) throws CompileException {
        fae.lhs.accept(this.atomCopier);
        return this.copyLvalue(fae);
    }

    public Lvalue
    copySuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException {
        if (scfae.optionalQualification != null) {
            scfae.optionalQualification.accept(this.atomCopier);
        }
        return this.copyLvalue(scfae);
    }

    public Lvalue
    copyLocalVariableAccess(LocalVariableAccess lva) throws CompileException { return this.copyLvalue(lva); }

    public Lvalue
    copyParenthesizedExpression(ParenthesizedExpression pe) throws CompileException {
        pe.value.accept(this.rvalueCopier);
        return this.copyLvalue(pe);
    }

    public ElementValue
    copyElementValueArrayInitializer(ElementValueArrayInitializer evai) throws CompileException {
        for (ElementValue elementValue : evai.elementValues) elementValue.accept(this.elementValueCopier);
        return this.copyElementValue(evai);
    }

    @Nullable public ElementValue
    copyElementValue(@Nullable ElementValue ev) throws CompileException {
        if (ev == null) return null;
        return DeepCopier.assertNotNull(ev.accept(this.elementValueCopier));
    }

    private Annotation[]
    copyAnnotations(Annotation[] subject) throws CompileException {
        Annotation[] result = new Annotation[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = this.copyAnnotation(subject[i]);
        return result;
    }

    public Annotation
    copyAnnotation(Annotation a) throws CompileException {
        return DeepCopier.assertNotNull(a.accept(this.annotationCopier));
    }

    public Annotation
    copySingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
        sea.type.accept(this.atomCopier);
        sea.elementValue.accept(this.elementValueCopier);
        return this.copyAnnotation(sea);
    }

    public Annotation
    copyNormalAnnotation(NormalAnnotation na) throws CompileException {
        na.type.accept(this.atomCopier);
        for (ElementValuePair elementValuePair : na.elementValuePairs) {
            elementValuePair.elementValue.accept(this.elementValueCopier);
        }
        return this.copyAnnotation(na);
    }

    public Annotation
    copyMarkerAnnotation(MarkerAnnotation ma) throws CompileException {
        ma.type.accept(this.atomCopier);
        return this.copyAnnotation(ma);
    }

    public void
    copyNamedClassDeclaration(NamedClassDeclaration ncd) throws CompileException {
        for (Type implementedType : ncd.implementedTypes) {
            implementedType.accept(this.atomCopier);
        }
        if (ncd.optionalExtendedType != null) ncd.optionalExtendedType.accept(this.atomCopier);
        return this.copyClassDeclaration(ncd);
    }

    public void
    copyInterfaceDeclaration(InterfaceDeclaration id) throws CompileException {
        for (TypeBodyDeclaration cd : id.constantDeclarations) cd.accept(this.typeBodyDeclarationCopier);
        for (Type extendedType : id.extendedTypes) extendedType.accept(this.atomCopier);
        return this.copyAbstractTypeDeclaration(id);
    }

    public void
    copyFunctionDeclarator(FunctionDeclarator fd) throws CompileException {
        return this.copyFormalParameters(fd.formalParameters);
        if (fd.optionalStatements != null) {
            for (BlockStatement bs : fd.optionalStatements) this.copyBlockStatement(bs);
        }
    }

    public FormalParameters
    copyFormalParameters(FunctionDeclarator.FormalParameters subject) throws CompileException {
        FormalParameter[] fps = new FormalParameter[subject.parameters.length];
        for (int i = 0; i < fps.length; i++) fps[i] = this.copyFormalParameter(subject.parameters[i]);
        return new FormalParameters(subject.getLocation(), fps, subject.variableArity);
    }

    public FunctionDeclarator.FormalParameter
    copyFormalParameter(FunctionDeclarator.FormalParameter formalParameter) throws CompileException {
        formalParameter.type.accept(this.atomCopier);
    }

    public void
    copyAbstractTypeBodyDeclaration(AbstractTypeBodyDeclaration atbd) throws CompileException { return this.copyLocated(atbd); }

    @Nullable private List<BlockStatement>
    copyStatements(@Nullable List<? extends BlockStatement> subject) throws CompileException {

        if (subject == null) return null;

        List<BlockStatement> result = new ArrayList<BlockStatement>(subject.size());
        for (BlockStatement bs : subject) result.add(this.copyBlockStatement(bs));
        return result;
    }

    public Statement
    copyStatement(Statement s) throws CompileException { return this.copyLocated(s); }

    public void
    copyBreakableStatement(BreakableStatement bs) throws CompileException { return this.copyStatement(bs); }

    public void
    copyContinuableStatement(ContinuableStatement cs) throws CompileException { return this.copyBreakableStatement(cs); }

    private Rvalue[]
    copyRvalues(Rvalue[] subject) throws CompileException {
        return this.copyRvalues(Arrays.asList(subject)).toArray(new Rvalue[0]);
    }

    private List<Rvalue>
    copyRvalues(Collection<? extends Rvalue> subject) throws CompileException {
        List<Rvalue> result = new ArrayList<Rvalue>(subject.size());
        for (Rvalue rv : subject) result.add(this.copyRvalue(rv));
        return result;
    }

    @Nullable private Rvalue[]
    copyOptionalRvalues(@Nullable Rvalue[] subject) throws CompileException {
        return subject == null ? null : this.copyRvalues(subject);
    }

    public Rvalue
    copyRvalue(Rvalue subject) throws CompileException { return this.copyAtom(subject); }

    @Nullable public Rvalue
    copyOptionalRvalue(@Nullable Rvalue subject) throws CompileException {
        return subject == null ? null : this.copyRvalue(subject);
    }

    public void
    copyBooleanRvalue(BooleanRvalue brv) throws CompileException { return this.copyRvalue(brv); }

    public void
    copyInvocation(Invocation i) throws CompileException {
        for (Rvalue argument : i.arguments) argument.accept(this.rvalueCopier);
        return this.copyRvalue(i);
    }

    public void
    copyConstructorInvocation(ConstructorInvocation ci) throws CompileException {
        for (Rvalue argument : ci.arguments) argument.accept(this.rvalueCopier);
        return this.copyAtom(ci);
    }

    public TypeDeclaration
    copyEnumConstant(EnumConstant ec) throws CompileException {

        for (ConstructorDeclarator cd : ec.constructors) return this.copyConstructorDeclarator(cd);

        if (ec.optionalArguments != null) {
            for (Rvalue a : ec.optionalArguments) return this.copyRvalue(a);
        }

        return this.copyAbstractTypeDeclaration(ec);
    }

    public TypeDeclaration
    copyPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed) throws CompileException {
        return this.copyPackageMemberClassDeclaration(pmed);
    }

    public MemberTypeDeclaration
    copyMemberEnumDeclaration(MemberEnumDeclaration med) throws CompileException {
        return this.copyMemberClassDeclaration(med);
    }

    public TypeDeclaration
    copyPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws CompileException {
        return this.copyPackageMemberInterfaceDeclaration(pmatd);
    }

    public MemberTypeDeclaration
    copyMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws CompileException {
        return this.copyMemberInterfaceDeclaration(matd);
    }

    public Lvalue
    copyLvalue(Lvalue lv) throws CompileException { return this.copyRvalue(lv); }

    private Type[]
    copyTypes(Type[] subject) throws CompileException {
        Type[] result = new Type[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = this.copyType(subject[i]);
        return result;
    }

    public Type
    copyType(Type subject) throws CompileException {
        return DeepCopier.assertNotNull(subject.accept(this.typeCopier));
    }

    public Atom
    copyAtom(Atom subject) throws CompileException {
        return DeepCopier.assertNotNull(subject.accept(this.atomCopier));
    }

    @Nullable private Atom
    copyOptionalAtom(@Nullable Atom subject) {
        return subject == null ? null : this.copyAtom(subject);
    }

    public TryStatement.Resource
    copyLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws CompileException {
        lvdr.type.accept(this.atomCopier);
        ArrayInitializerOrRvalue i = lvdr.variableDeclarator.optionalInitializer;
        if (i != null) return this.copyArrayInitializerOrRvalue(i);
    }

    public TryStatement.Resource
    copyVariableAccessResource(VariableAccessResource var) throws CompileException {
        var.variableAccess.accept(this.rvalueCopier);
    }

    private Modifiers
    copyModifiers(Modifiers subject) throws CompileException {
        return new Modifiers(subject.accessFlags, this.copyAnnotations(subject.annotations), subject.isDefault);
    }

    @Nullable private static TypeParameter[]
    copyTypeParameters(@Nullable TypeParameter[] subject) {
        if (subject == null) return null;
        TypeParameter[] result = new TypeParameter[subject.length];
        for (int i = 0; i < subject.length; i++) result[i] = DeepCopier.copyTypeParameter(subject[i]);
        return result;
    }

    private static TypeParameter
    copyTypeParameter(TypeParameter subject) {
        return new TypeParameter(subject.name, subject.optionalBound);
    }

    private static <T> T
    assertNotNull(@Nullable T subject) {
        assert subject != null;
        return subject;
    }
}
