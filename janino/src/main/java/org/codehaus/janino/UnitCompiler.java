
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2019 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved. // CHECKSTYLE:OFF CHECKSTYLE:ON
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.WarningHandler;
import org.codehaus.commons.compiler.util.Disassembler;
import org.codehaus.commons.compiler.util.Numbers;
import org.codehaus.commons.compiler.util.SystemProperties;
import org.codehaus.commons.compiler.util.iterator.Iterables;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.CodeContext.Inserter;
import org.codehaus.janino.CodeContext.Offset;
import org.codehaus.janino.IClass.IAnnotation;
import org.codehaus.janino.IClass.IConstructor;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IInvocable;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AbstractClassDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit;
import org.codehaus.janino.Java.AbstractCompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.AbstractCompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
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
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BooleanLiteral;
import org.codehaus.janino.Java.BooleanRvalue;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.BreakableStatement;
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
import org.codehaus.janino.Java.ContinuableStatement;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.DocCommentable;
import org.codehaus.janino.Java.ElementValue;
import org.codehaus.janino.Java.ElementValueArrayInitializer;
import org.codehaus.janino.Java.ElementValuePair;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.EnclosingScopeOfTypeDeclaration;
import org.codehaus.janino.Java.EnumConstant;
import org.codehaus.janino.Java.EnumDeclaration;
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
import org.codehaus.janino.Java.InnerClassDeclaration;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LambdaExpression;
import org.codehaus.janino.Java.Literal;
import org.codehaus.janino.Java.LocalClassDeclaration;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableAccess;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.Located;
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
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NamedTypeDeclaration;
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
import org.codehaus.janino.Java.Padder;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.Primitive;
import org.codehaus.janino.Java.PrimitiveType;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.Scope;
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
import org.codehaus.janino.Java.TypeArgument;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.TypeParameter;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Visitor.AbstractCompilationUnitVisitor;
import org.codehaus.janino.Visitor.AnnotationVisitor;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.Visitor.TypeDeclarationVisitor;
import org.codehaus.janino.util.Annotatable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.ClassFileException;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.ObjectVariableInfo;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.VerificationTypeInfo;

/**
 * This class actually implements the Java compiler. It is associated with exactly one compilation unit which it
 * compiles.
 */
public
class UnitCompiler {
    private static final Logger LOGGER = Logger.getLogger(UnitCompiler.class.getName());

    // Some debug flags; controlled by system properties.
    private static final boolean disassembleClassFilesToStdout = SystemProperties.getBooleanClassProperty(UnitCompiler.class, "disassembleClassFilesToStdout");

    /**
     * This constant determines the number of operands up to which the
     * <pre>
     *      a.concat(b).concat(c)
     * </pre>
     * <p>
     *  strategy is used to implement string concatenation. For more operands, the
     * </p>
     * <pre>
     *      new StringBuilder(a).append(b).append(c).append(d).toString()
     * </pre>
     * strategy is chosen.
     * <p>
     *   <a href="http://www.tomgibara.com/janino-evaluation/string-concatenation-benchmark">A very good article from
     *   Tom Gibara</a> analyzes the impact of this decision and recommends a value of three.
     * </p>
     */
    private static final int STRING_CONCAT_LIMIT = 3;

    /**
     * Special value for the <var>orientation</var> parameter of the {@link #compileBoolean(Java.Rvalue,
     * CodeContext.Offset, boolean)} methods, indicating that the code should be generated such that execution branches
     * if the value on top of the operand stack is TRUE.
     */
    public static final boolean JUMP_IF_TRUE  = true;

    /**
     * Special value for the <var>orientation</var> parameter of the {@link #compileBoolean(Java.Rvalue,
     * CodeContext.Offset, boolean)} methods, indicating that the code should be generated such that execution branches
     * if the value on top of the operand stack is FALSE.
     */
    public static final boolean JUMP_IF_FALSE = false;

    private static final Pattern LOOKS_LIKE_TYPE_PARAMETER = Pattern.compile("\\p{javaUpperCase}+");

    private EnumSet<JaninoOption> options = EnumSet.noneOf(JaninoOption.class);

    public
    UnitCompiler(AbstractCompilationUnit abstractCompilationUnit, IClassLoader iClassLoader) {
        this.abstractCompilationUnit = abstractCompilationUnit;
        this.iClassLoader            = iClassLoader;
    }

    /**
     * @return A reference to the currently effective compilation options; changes to it take
     *         effect immediately
     */
    public EnumSet<JaninoOption>
    options() { return this.options; }

    /**
     * Sets the options for all future compilations.
     */
    public UnitCompiler
    options(EnumSet<JaninoOption> options) {
        this.options = options;
        return this;
    }

    /**
     * @return The {@link AbstractCompilationUnit} that this {@link UnitCompiler} compiles
     */
    public AbstractCompilationUnit
    getAbstractCompilationUnit() { return this.abstractCompilationUnit; }

    /**
     * Generates an array of {@link ClassFile} objects which represent the classes and interfaces declared in the
     * compilation unit.
     */
    public ClassFile[]
    compileUnit(boolean debugSource, boolean debugLines, boolean debugVars) throws CompileException {

        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;

        if (this.generatedClassFiles != null) {
            throw new IllegalStateException("\"UnitCompiler.compileUnit()\" is not reentrant");
        }
        final List<ClassFile> gcfs = (this.generatedClassFiles = new ArrayList<ClassFile>());
        try {

            this.abstractCompilationUnit.accept(new AbstractCompilationUnitVisitor<Void, CompileException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override @Nullable public Void visitCompilationUnit(CompilationUnit cu)                throws CompileException { UnitCompiler.this.compile2(cu);  return null; }
                @Override @Nullable public Void visitModularCompilationUnit(ModularCompilationUnit mcu) throws CompileException { UnitCompiler.this.compile2(mcu); return null; }
            });

            if (this.compileErrorCount > 0) {
                throw new CompileException((
                    this.compileErrorCount
                    + " error(s) while compiling unit \""
                    + this.abstractCompilationUnit.fileName
                    + "\""
                ), null);
            }
            return (ClassFile[]) gcfs.toArray(new ClassFile[gcfs.size()]);
        } finally {
            this.generatedClassFiles = null;
        }
    }

    /**
     * Compiles an (ordinary, not modular) compilation unit
     */
    private void
    compile2(CompilationUnit cu) throws CompileException {

        for (PackageMemberTypeDeclaration pmtd : cu.packageMemberTypeDeclarations) {
            try {
                this.compile(pmtd);
            } catch (ClassFileException cfe) {
                throw new CompileException(cfe.getMessage(), pmtd.getLocation(), cfe);
            } catch (RuntimeException re) {
                throw new InternalCompilerException(
                    "Compiling \""
                    + pmtd
                    + "\" in "
                    + pmtd.getLocation()
                    + ": "
                    + re.getMessage(),
                    re
                );
            }
        }
    }

    private void
    compile2(ModularCompilationUnit mcu) throws CompileException {
        this.compileError("Compilation of modular compilation unit not implemented");
    }

    // ------------ TypeDeclaration.compile() -------------

    private void
    compile(TypeDeclaration td) throws CompileException {

        td.accept(new TypeDeclarationVisitor<Void, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLength:9
            @Override @Nullable public Void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                             throws CompileException { UnitCompiler.this.compile2(acd);                           return null; }
            @Override @Nullable public Void visitLocalClassDeclaration(LocalClassDeclaration lcd)                                     throws CompileException { UnitCompiler.this.compile2(lcd);                           return null; }
            @Override @Nullable public Void visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)                    throws CompileException { UnitCompiler.this.compile2(pmcd);                          return null; }
            @Override @Nullable public Void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                           throws CompileException { UnitCompiler.this.compile2(mid);                           return null; }
            @Override @Nullable public Void visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid)            throws CompileException { UnitCompiler.this.compile2(pmid);                          return null; }
            @Override @Nullable public Void visitMemberClassDeclaration(MemberClassDeclaration mcd)                                   throws CompileException { UnitCompiler.this.compile2((InnerClassDeclaration) mcd);   return null; }
            @Override @Nullable public Void visitMemberEnumDeclaration(MemberEnumDeclaration med)                                     throws CompileException { UnitCompiler.this.compile2((InnerClassDeclaration) med);   return null; }
            @Override @Nullable public Void visitPackageMemberEnumDeclaration(PackageMemberEnumDeclaration pmed)                      throws CompileException { UnitCompiler.this.compile2(pmed);                          return null; }
            @Override @Nullable public Void visitPackageMemberAnnotationTypeDeclaration(PackageMemberAnnotationTypeDeclaration pmatd) throws CompileException { UnitCompiler.this.compile2(pmatd);                         return null; }

            // SUPPRESS CHECKSTYLE LineLength:2
            @Override @Nullable public Void visitEnumConstant(EnumConstant ec)                                         throws CompileException { UnitCompiler.this.compileError("Compilation of enum constant NYI",                      ec.getLocation());   return null; }
            @Override @Nullable public Void visitMemberAnnotationTypeDeclaration(MemberAnnotationTypeDeclaration matd) throws CompileException { UnitCompiler.this.compileError("Compilation of member annotation type declaration NYI", matd.getLocation()); return null; }
        });
    }

    /**
     * Compiles a top-level class or enum declaration.
     */
    private void
    compile2(PackageMemberClassDeclaration pmcd) throws CompileException {
        this.checkForConflictWithSingleTypeImport(pmcd.getName(), pmcd.getLocation());
        this.checkForNameConflictWithAnotherPackageMemberTypeDeclaration(pmcd);
        this.compile2((NamedClassDeclaration) pmcd);
    }

    /**
     * Compiles a top-level interface or annotation type declaration.
     */
    private void
    compile2(PackageMemberInterfaceDeclaration pmid) throws CompileException {
        this.checkForConflictWithSingleTypeImport(pmid.getName(), pmid.getLocation());
        this.checkForNameConflictWithAnotherPackageMemberTypeDeclaration(pmid);
        this.compile2((InterfaceDeclaration) pmid);
    }

    /**
     * @see JLS8, section 7.6, "Top Level Type Declarations"
     */
    private void
    checkForNameConflictWithAnotherPackageMemberTypeDeclaration(PackageMemberTypeDeclaration pmtd)
    throws CompileException {

        CompilationUnit declaringCompilationUnit = pmtd.getDeclaringCompilationUnit();

        String                       name      = pmtd.getName();
        PackageMemberTypeDeclaration otherPmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(
            name
        );

        if (otherPmtd != null && otherPmtd != pmtd) {
            this.compileError(
                "Redeclaration of type \"" + name + "\", previously declared in " + otherPmtd.getLocation()
                ,
                pmtd.getLocation()
            );
        }
    }

    /**
     * @see JLS8, section 7.6, "Top Level Type Declarations"
     */
    private void
    checkForConflictWithSingleTypeImport(String name, Location location) throws CompileException {

        String[] ss = this.getSingleTypeImport(name, location);
        if (ss != null) {
            this.compileError((
                "Package member type declaration \""
                + name
                + "\" conflicts with single-type-import \""
                + Java.join(ss, ".")
                + "\""
            ), location);
        }
    }

    private void
    compile2(AbstractClassDeclaration cd) throws CompileException {
        IClass iClass = this.resolve(cd);

        // Check that all methods of the non-abstract class are implemented.
        if (!(cd instanceof NamedClassDeclaration && ((NamedClassDeclaration) cd).isAbstract())) {
            IMethod[] ms = iClass.getIMethods();
            for (IMethod base : ms) {
                if (base.isAbstract()) {
                    if ("<clinit>".equals(base.getName())) continue;
                    IMethod override = iClass.findIMethod(base.getName(), base.getParameterTypes());
                    if (
                        override == null           // It wasn't overridden
                        || override.isAbstract()   // It was overridden with an abstract method
                                                   // The override does not provide a covariant return type
                        || !base.getReturnType().isAssignableFrom(override.getReturnType())
                    ) {
                        this.compileError(
                            "Non-abstract class \"" + iClass + "\" must implement method \"" + base + "\"",
                            cd.getLocation()
                        );
                    }
                }
            }
        }

        short accessFlags = this.accessFlags(cd.getModifiers());
        accessFlags |= Mod.SUPER;
        if (cd instanceof EnumDeclaration) accessFlags |= Mod.ENUM;

        // Create "ClassFile" object.
        ClassFile cf;
        {
            IClass superclass = iClass.getSuperclass();
            cf = new ClassFile(
                accessFlags,                                            // accessFlags
                iClass.getDescriptor(),                                 // thisClassFD
                superclass != null ? superclass.getDescriptor() : null, // superclassFD
                IClass.getDescriptors(iClass.getInterfaces())           // interfaceFDs
            );
        }

        // Add class annotations with retention != SOURCE.
        this.compileAnnotations(cd.getAnnotations(), cf, cf);

        if (cd.getEnclosingScope() instanceof Block) {

            // Add an "InnerClasses" attribute entry for this anonymous class declaration on the class file (JLS8,
            // section 4.7.6, "The InnerClasses Attribute").
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short innerNameIndex      = (
                this instanceof NamedTypeDeclaration
                ? cf.addConstantUtf8Info(((NamedTypeDeclaration) this).getName())
                : (short) 0
            );
            assert cd.getAnnotations().length == 0 : "NYI";
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,  // innerClassInfoIndex
                (short) 0,            // outerClassInfoIndex
                innerNameIndex,       // innerNameIndex
                accessFlags           // innerClassAccessFlags
            ));
        } else
        if (cd.getEnclosingScope() instanceof TypeDeclaration) {

            // Add an "InnerClasses" attribute entry for this nested class declaration on the class file (JLS8,
            // section 4.7.6, "The InnerClasses Attribute").
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(
                this.resolve(((TypeDeclaration) cd.getEnclosingScope())).getDescriptor()
            );
            short innerNameIndex = cf.addConstantUtf8Info(((MemberTypeDeclaration) cd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,  // innerClassInfoIndex
                outerClassInfoIndex,  // outerClassInfoIndex
                innerNameIndex,       // innerNameIndex
                accessFlags           // innerClassAccessFlags
            ));
        }

        // Set the "SourceFile" attribute (JVMS8, section 4.7.10, "The SourceFile Attribute") on this class file.
        if (this.debugSource) {
            String sourceFileName;
            {
                String s = cd.getLocation().getFileName();
                if (s != null) {
                    sourceFileName = new File(s).getName();
                } else if (cd instanceof NamedTypeDeclaration) {
                    sourceFileName = ((NamedTypeDeclaration) cd).getName() + ".java";
                } else {
                    sourceFileName = "ANONYMOUS.java";
                }
            }
            cf.addSourceFileAttribute(sourceFileName);
        }

        // Add a "Deprecated" attribute (JVMS8, section 4.7.15, "The Deprecated Attribute") on this class file.
        if (cd instanceof DocCommentable && ((DocCommentable) cd).hasDeprecatedDocTag()) {
            cf.addDeprecatedAttribute();
        }

        List<BlockStatement> classInitializationStatements = new ArrayList<BlockStatement>();

        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed = (EnumDeclaration) cd;

            // Create field and static initializer for each enum constant.
            for (EnumConstant ec : ed.getConstants()) {

                // E <constant> = new E(<ordinal>, <name> [ optional-constructor-args ]);
                VariableDeclarator variableDeclarator = new VariableDeclarator(
                    ec.getLocation(),     // location
                    ec.name,              // name
                    0,                    // brackets
                    new NewClassInstance( // initializer
                        ec.getLocation(),                                                   // location
                        null,                                                               // qualification
                        iClass,                                                             // iClass
                        ec.arguments != null ? ec.arguments : new Rvalue[0] // arguments
                    )
                );

                FieldDeclaration fd = new FieldDeclaration(
                    ec.getLocation(),                                                            // location
                    ec.getDocComment(),                                                          // docComment
                    UnitCompiler.accessModifiers(ec.getLocation(), "public", "static", "final"), // modifiers
                    new SimpleType(ec.getLocation(), iClass),                                    // type
                    new VariableDeclarator[] { variableDeclarator }                              // variableDeclarators
                );
                fd.setDeclaringType(ed);

                classInitializationStatements.add(fd);

                this.addFields(fd, cf);
            }

            // Create the synthetic "ENUM$VALUES" field:
            //
            //     private static final E[] ENUM$VALUES = new E[<number-of-enum-constants>];
            Location         loc        = ed.getLocation();
            IClass           enumIClass = this.resolve(ed);
            FieldDeclaration fd         = new FieldDeclaration(
                loc,                                                             // location
                null,                                                            // docComment
                UnitCompiler.accessModifiers(loc, "private", "static", "final"), // modifiers
                new SimpleType(loc, enumIClass),                                 // type
                new VariableDeclarator[] {                                       // variableDeclarators)
                    new VariableDeclarator(
                        loc,           // location
                        "ENUM$VALUES", // name
                        1,             // brackets
                        new NewArray(  // initializer
                            loc,                             // location
                            new SimpleType(loc, enumIClass), // type
                            new Rvalue[] {                   // dimExprs
                                new IntegerLiteral(loc, String.valueOf(ed.getConstants().size())),
                            },
                            0                                // dims
                        )
                    )
                }
            );
            ((AbstractClassDeclaration) ed).addFieldDeclaration(fd);
        }

        // Process static initializers (a.k.a. class initializers).
        for (BlockStatement vdoi : cd.variableDeclaratorsAndInitializers) {
            if (
                (vdoi instanceof FieldDeclaration && ((FieldDeclaration) vdoi).isStatic())
                || (vdoi instanceof Initializer && ((Initializer) vdoi).isStatic())
            ) classInitializationStatements.add(vdoi);
        }

        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed         = (EnumDeclaration) cd;
            IClass          enumIClass = this.resolve(ed);

            // Initialize the elements of the synthetic "ENUM$VALUES" field:
            //
            //     E.ENUM$VALUES[0] = E.<first-constant>;
            //     E.ENUM$VALUES[1] = E.<second-constant>;
            //     ...
            int index = 0;
            for (EnumConstant ec : ed.getConstants()) {
                classInitializationStatements.add(
                    new ExpressionStatement(
                        new Assignment(
                            ec.getLocation(),          // location
                            new ArrayAccessExpression( // lhs
                                ec.getLocation(),                                             // location
                                new FieldAccessExpression(                                    // lhs
                                    ec.getLocation(),
                                    new SimpleType(ec.getLocation(), enumIClass),
                                    "ENUM$VALUES"
                                ),
                                new IntegerLiteral(ec.getLocation(), String.valueOf(index++)) // rhs
                            ),
                            "=",                       // operator
                            new FieldAccessExpression( // rhs
                                ec.getLocation(),
                                new SimpleType(ec.getLocation(), enumIClass),
                                ec.name
                            )
                        )
                    )
                );
            }
        }

        // Create class initialization method.
        this.maybeCreateInitMethod(cd, cf, classInitializationStatements);

        // Generate and compile the "magic" ENUM methods "E[] values()" and "E valueOf(String)".
        if (cd instanceof EnumDeclaration) {
            EnumDeclaration ed = (EnumDeclaration) cd;

            // public static E[] values() {
            //     E[] tmp = new T[<number-of-constants>];
            //     System.arraycopy(T.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
            //     return tmp;
            // }
            Location   loc                   = ed.getLocation();
            int        numberOfEnumConstants = ed.getConstants().size();
            IClass     enumIClass            = this.resolve(ed);

            // tmp = new T[<number-of-constants>];
            VariableDeclarator vd = new VariableDeclarator(
                loc,          // location
                "tmp",        // name
                0,            // brackets
                new NewArray( // optionalinitializer
                    loc,                             // location
                    new SimpleType(loc, enumIClass), // type
                    new Rvalue[] {                   // dimExprs
                        new IntegerLiteral(loc, String.valueOf(numberOfEnumConstants))
                    },
                    0                                // dims
                )
            );

            // E[] tmp = new E[<number-of-constants>];
            LocalVariableDeclarationStatement lvds = new LocalVariableDeclarationStatement(
                loc,
                new Modifier[0],
                new SimpleType(loc, enumIClass.getArrayIClass(this.iClassLoader.TYPE_java_lang_Object)),
                new VariableDeclarator[] { vd }
            );

            // public static E[] values() {
            //     E[] tmp = new T[<number-of-constants>];
            //     System.arraycopy(T.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
            //     return tmp;
            // }
            {
                MethodDeclarator md = new MethodDeclarator(
                    loc,                                                   // location
                    null,                                                  // docComment
                    UnitCompiler.accessModifiers(loc, "public", "static"), // modifiers
                    null,                                                  // typeParameters
                    new ArrayType(new SimpleType(loc, enumIClass)),        // type
                    "values",                                              // name
                    new FormalParameters(loc),                             // parameters
                    new Type[0],                                           // thrownExceptions
                    null,                                                  // defaultValue
                    Arrays.asList(                                         // statements

                        // E[] tmp = new E[<number-of-constants>];
                        lvds,

                        // System.arraycopy(E.ENUM$VALUES, 0, tmp, 0, <number-of-constants>);
                        new ExpressionStatement(new MethodInvocation(
                            loc,                                                          // location
                            new SimpleType(loc, this.iClassLoader.TYPE_java_lang_System), // target
                            "arraycopy",                                                  // methodName
                            new Rvalue[] {                                                // arguments
                                // SUPPRESS CHECKSTYLE LineLength:5
                                new FieldAccessExpression(loc, new SimpleType(loc, enumIClass), "ENUM$VALUES"), // Argument #1: E.ENUM$VALUES
                                new IntegerLiteral(loc, "0"),                                                   // Argument #2: 0
                                new LocalVariableAccess(loc, this.getLocalVariable(lvds, vd)),                  // Argument #3: tmp
                                new IntegerLiteral(loc, "0"),                                                   // Argument #4: 0
                                new IntegerLiteral(loc, String.valueOf(numberOfEnumConstants)),                 // Argument #5: <number-of-constants>
                            }
                        )),

                        // return tmp;
                        new ReturnStatement(loc, new LocalVariableAccess(loc, this.getLocalVariable(lvds, vd)))
                    )
                );
                md.setDeclaringType(ed);

                this.compile(md, cf);
            }

            // public static E valueOf(String s) {
            //     return (E) Enum.valueOf(E.class, s);
            // }
            FormalParameter fp = new FormalParameter(
                loc,                                                          // location
                new Modifier[0],                                              // modifiers
                new SimpleType(loc, this.iClassLoader.TYPE_java_lang_String), // type
                "s"                                                           // name
            );
            {
                MethodDeclarator md = new MethodDeclarator(
                    loc,                                                   // location
                    null,                                                  // docComment
                    UnitCompiler.accessModifiers(loc, "public", "static"), // modifiers
                    null,                                                  // typeParameters
                    new SimpleType(loc, enumIClass),                       // type
                    "valueOf",                                             // name
                    new FormalParameters(                                  // formalParameters
                        loc,                          // location
                        new FormalParameter[] { fp }, // parameters
                        false                         // variableArity
                    ),
                    new Type[0],                                           // thrownExceptions,
                    null,                                                  // defaultValue
                    Arrays.asList(                                         // statements

                        // return (E) Enum.valueOf(E.class, s);
                        new ReturnStatement(loc, new Cast(
                            loc,                             // location
                            new SimpleType(loc, enumIClass), // targetType
                            new MethodInvocation(            // value
                                loc,                                                        // location
                                new SimpleType(loc, this.iClassLoader.TYPE_java_lang_Enum), // target
                                "valueOf",                                                  // methodName
                                new Rvalue[] {                                              // arguments
                                    new ClassLiteral(loc, new SimpleType(loc, enumIClass)),
                                    new ParameterAccess(loc, fp)
                                }
                            )
                        ))
                    )
                );

                md.setEnclosingScope(ed);

                this.compile(md, cf);
            }
        }

        // Compile declared methods.
        this.compileDeclaredMethods(cd, cf);

        // Compile declared constructors.
        // As a side effect of compiling methods and constructors, synthetic "class-dollar" methods (which implement
        // class literals) are generated on-the fly. We need to note how many we have here so we can compile the
        // extras.
        final int declaredMethodCount = cd.getMethodDeclarations().size();
        {
            int                     syntheticFieldCount = cd.syntheticFields.size();
            ConstructorDeclarator[] ctords              = cd.getConstructors();
            for (ConstructorDeclarator ctord : ctords) {

                this.compile(ctord, cf);
                if (syntheticFieldCount != cd.syntheticFields.size()) {
                    throw new InternalCompilerException(
                        "SNO: Compilation of constructor \""
                        + ctord
                        + "\" ("
                        + ctord.getLocation()
                        + ") added synthetic fields!?"
                    );
                }
            }
        }

        // A side effect of this call may create synthetic functions to access protected parent variables.
        this.compileDeclaredMemberTypes(cd, cf);

        // Compile the aforementioned extras.
        this.compileDeclaredMethods(cd, cf, declaredMethodCount);

        // For every method look for bridge methods that need to be supplied. This is used to correctly dispatch into
        // covariant return types from existing code.
        for (IMethod base : iClass.getIMethods()) {
            if (!base.isStatic() && base.getAccess() != Access.PRIVATE) {
                IMethod override = iClass.findIMethod(base.getName(), base.getParameterTypes());

                // If we overrode the method but with a DIFFERENT return type.
                if (
                    override != null
                    && base.getReturnType() != override.getReturnType()
                ) this.generateBridgeMethod(cf, iClass, base, override);
            }
        }

        // Add class and instance variables as (static and non-static) fields.
        for (FieldDeclaration fd : Iterables.filterByClass(
            cd.getVariableDeclaratorsAndInitializers(),
            FieldDeclaration.class
        )) this.addFields(fd, cf);

        // Synthetic fields.
        for (IField f : cd.getSyntheticFields().values()) {
            cf.addFieldInfo(
                Mod.PACKAGE,                 // accessFlags
                f.getName(),                 // fieldName
                f.getType().getDescriptor(), // fieldTypeFD
                null                         // constantValue
            );
        }

        // Add the generated class file to a thread-local store.
        this.addClassFile(cf);
    }

    /**
     * Adds the given {@link ClassFile} to the result set.
     */
    private void
    addClassFile(ClassFile cf) {

        if (UnitCompiler.disassembleClassFilesToStdout) Disassembler.disassembleToStdout(cf.toByteArray());

        assert this.generatedClassFiles != null;
        this.generatedClassFiles.add(cf);
    }

    /**
     * Creates and adds {@link ClassFile.FieldInfo}s to the <var>cf</var> for all fields declared by the <var>fd</var>.
     */
    private void
    addFields(FieldDeclaration fd, ClassFile cf) throws CompileException {
        for (VariableDeclarator vd : fd.variableDeclarators) {

            Type type = fd.type;
            for (int i = 0; i < vd.brackets; ++i) type = new ArrayType(type);

            Object ocv = UnitCompiler.NOT_CONSTANT;
            if (fd.isFinal() && vd.initializer instanceof Rvalue) {
                ocv = this.getConstantValue((Rvalue) vd.initializer);
            }

            short accessFlags = this.accessFlags(fd.modifiers);

            ClassFile.FieldInfo fi;
            if (fd.isPrivate()) {

                // To make the private field accessible for enclosing types, enclosed types and types enclosed by the
                // same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                accessFlags = UnitCompiler.changeAccessibility(accessFlags, Mod.PACKAGE);

                fi = cf.addFieldInfo(
                    accessFlags,                                  // accessFlags
                    vd.name,                                      // fieldName
                    this.getType(type).getDescriptor(),           // fieldTypeFD
                    ocv == UnitCompiler.NOT_CONSTANT ? null : ocv // constantValue
                );
            } else
            {
                fi = cf.addFieldInfo(
                    (                                             // accessFlags
                        fd.getDeclaringType() instanceof InterfaceDeclaration
                        ? (short) (Mod.PUBLIC | Mod.STATIC | Mod.FINAL)
                        : accessFlags
                    ),
                    vd.name,                                      // fieldName
                    this.getType(type).getDescriptor(),           // fieldTypeFD
                    ocv == UnitCompiler.NOT_CONSTANT ? null : ocv // constantValue
                );
            }

            // Add field annotations with retention != SOURCE.
            this.compileAnnotations(fd.getAnnotations(), fi, cf);

            // Add "Deprecated" attribute (JVMS 4.7.10).
            if (fd.hasDeprecatedDocTag()) {
                fi.addAttribute(new ClassFile.DeprecatedAttribute(cf.addConstantUtf8Info("Deprecated")));
            }
        }
    }

    private void
    compile2(AnonymousClassDeclaration acd) throws CompileException {

        // For classes that enclose surrounding scopes, trawl their field initializers looking for synthetic fields.
        this.fakeCompileVariableDeclaratorsAndInitializers(acd);

        this.compile2((InnerClassDeclaration) acd);
    }

    private void
    compile2(LocalClassDeclaration lcd) throws CompileException {

        // For classes that enclose surrounding scopes, trawl their field initializers looking for synthetic fields.
        this.fakeCompileVariableDeclaratorsAndInitializers(lcd);

        this.compile2((InnerClassDeclaration) lcd);
    }

    private void
    compile2(InnerClassDeclaration icd) throws CompileException {

        // Define a synthetic "this$n" field if there is an enclosing instance, where "n" designates the number of
        // enclosing instances minus one.
        {
            List<TypeDeclaration> ocs     = UnitCompiler.getOuterClasses(icd);
            final int             nesting = ocs.size();
            if (nesting >= 2) {
                TypeDeclaration immediatelyEnclosingOuterClassDeclaration = (TypeDeclaration) ocs.get(1);
                icd.defineSyntheticField(new SimpleIField(
                    this.resolve(icd),
                    "this$" + (nesting - 2),
                    this.resolve(immediatelyEnclosingOuterClassDeclaration)
                ));
            }
        }

        this.compile2((AbstractClassDeclaration) icd);
    }

    private void
    fakeCompileVariableDeclaratorsAndInitializers(AbstractClassDeclaration cd) throws CompileException {

        // Compilation of field declarations can create synthetic variables, so we must not use an iterator.
        List<BlockStatement> vdais = cd.variableDeclaratorsAndInitializers;
        for (int i = 0; i < vdais.size(); i++) {
            BlockStatement vdoi = (BlockStatement) vdais.get(i);
            this.fakeCompile(vdoi);
        }
    }

    private void
    compile2(InterfaceDeclaration id) throws CompileException {

        final IClass iClass = this.resolve(id);

        // Determine extended interfaces.
        IClass[] is                   = (id.interfaces = new IClass[id.extendedTypes.length]);
        String[] interfaceDescriptors = new String[is.length];
        for (int i = 0; i < id.extendedTypes.length; ++i) {
            is[i]                   = this.getType(id.extendedTypes[i]);
            interfaceDescriptors[i] = is[i].getDescriptor();
        }

        short accessFlags = this.accessFlags(id.getModifiers());
        accessFlags |= Mod.INTERFACE;
        accessFlags |= Mod.ABSTRACT;
        if (id instanceof AnnotationTypeDeclaration) accessFlags |= Mod.ANNOTATION;
        if (id instanceof MemberInterfaceDeclaration) accessFlags |= Mod.STATIC;

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            accessFlags,                 // accessFlags
            iClass.getDescriptor(),      // thisClassFD
            Descriptor.JAVA_LANG_OBJECT, // superclassFD
            interfaceDescriptors         // interfaceFDs
        );

        // Add interface annotations with retention != SOURCE.
        this.compileAnnotations(id.getAnnotations(), cf, cf);

        // Set "SourceFile" attribute.
        if (this.debugSource) {
            String sourceFileName;
            {
                String s = id.getLocation().getFileName();
                if (s != null) {
                    sourceFileName = new File(s).getName();
                } else {
                    sourceFileName = id.getName() + ".java";
                }
            }
            cf.addSourceFileAttribute(sourceFileName);
        }

        // Add "Deprecated" attribute (JVMS 4.7.10).
        if (id.hasDeprecatedDocTag()) cf.addDeprecatedAttribute();

        // Interface initialization method.
        if (!id.constantDeclarations.isEmpty()) {
            List<BlockStatement> statements = new ArrayList<BlockStatement>();
            statements.addAll(id.constantDeclarations);

            this.maybeCreateInitMethod(id, cf, statements);
        }

        this.compileDeclaredMethods(id, cf);

        // Class variables.
        for (FieldDeclaration constantDeclaration : id.constantDeclarations) this.addFields(constantDeclaration, cf);

        this.compileDeclaredMemberTypes(id, cf);

        // Add the generated class file to a thread-local store.
        this.addClassFile(cf);
    }

    /**
     * Converts and adds the <var>annotations</var> to the <var>target</var>.
     */
    private void
    compileAnnotations(Annotation[] annotations, Annotatable target, final ClassFile cf) throws CompileException {

        final Set<IClass> seenAnnotations = new HashSet<IClass>();
        ANNOTATIONS: for (final Annotation a : annotations) {
            Type          annotationType        = a.getType();
            IClass        annotationIClass      = this.getType(annotationType);
            IAnnotation[] annotationAnnotations = annotationIClass.getIAnnotations();

            // Check for duplicate annotations.
            if (!seenAnnotations.add(annotationIClass)) {
                this.compileError("Duplicate annotation \"" + annotationIClass + "\"", annotationType.getLocation());
            }

            // Determine the attribute name.
            boolean runtimeVisible = false;
            for (IAnnotation aa : annotationAnnotations) {

                if (aa.getAnnotationType() != this.iClassLoader.TYPE_java_lang_annotation_Retention) continue;

                Object rev = aa.getElementValue("value");

                String retention = ((IField) rev).getName();

                if ("SOURCE".equals(retention)) {
                    continue ANNOTATIONS;
                } else
                if ("CLASS".equals(retention)) {
                    runtimeVisible = false;
                    break;
                } else
                if ("RUNTIME".equals(retention)) {
                    runtimeVisible = true;
                    break;
                } else
                {
                    throw new AssertionError(retention);
                }
            }

            // Compile the annotation's element-value-pairs.
            final Map<Short, ClassFile.ElementValue> evps = new HashMap<Short, ClassFile.ElementValue>();
            a.accept(new Visitor.AnnotationVisitor<Void, CompileException>() {

                @Override @Nullable public Void
                visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                    evps.put(
                        cf.addConstantUtf8Info("value"),
                        UnitCompiler.this.compileElementValue(sea.elementValue, cf)
                    );
                    return null;
                }

                @Override @Nullable public Void
                visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                    for (ElementValuePair evp : na.elementValuePairs) {
                        evps.put(
                            cf.addConstantUtf8Info(evp.identifier),
                            UnitCompiler.this.compileElementValue(evp.elementValue, cf)
                        );
                    }
                    return null;
                }

                @Override @Nullable public Void
                visitMarkerAnnotation(MarkerAnnotation ma) {
                    ;
                    return null;
                }
            });

            // Add the annotation to the target (class/interface, method or field).
            target.addAnnotationsAttributeEntry(runtimeVisible, annotationIClass.getDescriptor(), evps);
        }
    }

    private ClassFile.ElementValue
    compileElementValue(ElementValue elementValue, final ClassFile cf)
    throws CompileException {

        ClassFile.ElementValue
        result = (ClassFile.ElementValue) elementValue.accept(
            new ElementValueVisitor<ClassFile.ElementValue, CompileException>() {

                @Override public ClassFile.ElementValue
                visitRvalue(Rvalue rv) throws CompileException {

                    // Enum constant?
                    ENUM_CONSTANT:
                    if (rv instanceof AmbiguousName) {

                        Rvalue enumConstant = UnitCompiler.this.reclassify((AmbiguousName) rv).toRvalue();
                        if (!(enumConstant instanceof FieldAccess)) break ENUM_CONSTANT; // Not a field access.
                        FieldAccess enumConstantFieldAccess = (FieldAccess) enumConstant;

                        Type enumType = enumConstantFieldAccess.lhs.toType();
                        if (enumType == null) break ENUM_CONSTANT; // LHS is not a type.

                        IClass enumIClass = UnitCompiler.this.findTypeByName(rv.getLocation(), enumType.toString());
                        if (enumIClass == null) {
                            UnitCompiler.this.compileError(
                                "Cannot find enum \"" + enumType + "\"",
                                enumType.getLocation()
                            );
                            break ENUM_CONSTANT;
                        }

                        if (enumIClass.getSuperclass() == UnitCompiler.this.iClassLoader.TYPE_java_lang_Enum) {
                            return new ClassFile.EnumConstValue(
                                cf.addConstantUtf8Info(enumIClass.getDescriptor()),             // typeNameIndex
                                cf.addConstantUtf8Info(enumConstantFieldAccess.field.getName()) // constNameIndex
                            );
                        }

                        // We have a constant, but it is not an ENUM constant, so fall through.
                    }

                    // Class literal?
                    if (rv instanceof ClassLiteral) {
                        return new ClassFile.ClassElementValue(cf.addConstantUtf8Info(
                            UnitCompiler.this.getType(((ClassLiteral) rv).type).getDescriptor()
                        ));
                    }

                    // Constant value?
                    Object cv = UnitCompiler.this.getConstantValue(rv);

                    if (cv == UnitCompiler.NOT_CONSTANT) {
                        throw new CompileException(
                            "\"" + rv + "\" is not a constant expression",
                            rv.getLocation()
                        );
                    }

                    if (cv == null) {
                        throw new CompileException(
                            "Null literal not allowed as element value",
                            rv.getLocation()
                        );
                    }

                    // SUPPRESS CHECKSTYLE LineLength:9
                    if (cv instanceof Boolean)   { return new ClassFile.BooleanElementValue(cf.addConstantIntegerInfo((Boolean) cv ? 1 : 0)); }
                    if (cv instanceof Byte)      { return new ClassFile.ByteElementValue(cf.addConstantIntegerInfo((Byte) cv));               }
                    if (cv instanceof Short)     { return new ClassFile.ShortElementValue(cf.addConstantIntegerInfo((Short) cv));             }
                    if (cv instanceof Integer)   { return new ClassFile.IntElementValue(cf.addConstantIntegerInfo((Integer) cv));             }
                    if (cv instanceof Long)      { return new ClassFile.LongElementValue(cf.addConstantLongInfo((Long) cv));                  }
                    if (cv instanceof Float)     { return new ClassFile.FloatElementValue(cf.addConstantFloatInfo((Float) cv));               }
                    if (cv instanceof Double)    { return new ClassFile.DoubleElementValue(cf.addConstantDoubleInfo((Double) cv));            }
                    if (cv instanceof Character) { return new ClassFile.CharElementValue(cf.addConstantIntegerInfo((Character) cv));          }
                    if (cv instanceof String)    { return new ClassFile.StringElementValue(cf.addConstantUtf8Info((String) cv));              }

                    throw new AssertionError(cv);
                }

                @Override public ClassFile.ElementValue
                visitAnnotation(Annotation a) throws CompileException {

                    short annotationTypeIndex = (
                        cf.addConstantClassInfo(UnitCompiler.this.getType(a.getType()).getDescriptor())
                    );

                    final Map<Short, ClassFile.ElementValue>
                    evps = new HashMap<Short, ClassFile.ElementValue>();
                    a.accept(new AnnotationVisitor<Void, CompileException>() {

                        @Override @Nullable public Void
                        visitMarkerAnnotation(MarkerAnnotation ma) {
                            ;
                            return null;
                        }

                        @Override @Nullable public Void
                        visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                            evps.put(
                                cf.addConstantUtf8Info("value"),
                                UnitCompiler.this.compileElementValue(sea.elementValue, cf)
                            );
                            return null;
                        }

                        @Override @Nullable public Void
                        visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                            for (ElementValuePair evp : na.elementValuePairs) {
                                evps.put(
                                    cf.addConstantUtf8Info(evp.identifier),
                                    UnitCompiler.this.compileElementValue(evp.elementValue, cf)
                                );
                            }
                            return null;
                        }
                    });
                    return new ClassFile.Annotation(annotationTypeIndex, evps);
                }

                @Override public ClassFile.ElementValue
                visitElementValueArrayInitializer(ElementValueArrayInitializer evai) throws CompileException {
                    ClassFile.ElementValue[]
                    evs = new ClassFile.ElementValue[evai.elementValues.length];

                    for (int i = 0; i < evai.elementValues.length; i++) {
                        evs[i] = UnitCompiler.this.compileElementValue(evai.elementValues[i], cf);
                    }
                    return new ClassFile.ArrayElementValue(evs);
                }
            }
        );

        assert result != null;
        return result;
    }

    /**
     * Creates class/interface initialization method iff there is any initialization code.
     *
     * @param td         The type declaration
     * @param cf         The class file into which to put the method
     * @param statements The statements for the method (possibly empty)
     */
    private void
    maybeCreateInitMethod(
        TypeDeclaration      td,
        ClassFile            cf,
        List<BlockStatement> statements
    ) throws CompileException {

        // Create class/interface initialization method iff there is any initialization code.
        if (this.generatesCode2(statements)) {
            MethodDeclarator md = new MethodDeclarator(
                td.getLocation(),                                                   // location
                null,                                                               // docComment
                UnitCompiler.accessModifiers(td.getLocation(), "static", "public"), // modifiers
                null,                                                               // typeParameters
                new PrimitiveType(td.getLocation(), Primitive.VOID),                // type
                "<clinit>",                                                         // name
                new FormalParameters(td.getLocation()),                             // formalParameters
                new ReferenceType[0],                                               // thrownExceptions
                null,                                                               // defaultValue
                statements                                                          // statements
            );
            md.setDeclaringType(td);
            this.compile(md, cf);
        }
    }

    /**
     * Compiles all of the types for this declaration
     * <p>
     *   NB: as a side effect this will fill in the synthetic field map
     * </p>
     */
    private void
    compileDeclaredMemberTypes(TypeDeclaration decl, ClassFile cf) throws CompileException {
        for (MemberTypeDeclaration mtd : decl.getMemberTypeDeclarations()) {
            this.compile(mtd);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(mtd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(decl).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(mtd.getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,                 // innerClassInfoIndex
                outerClassInfoIndex,                 // outerClassInfoIndex
                innerNameIndex,                      // innerNameIndex
                this.accessFlags(mtd.getModifiers()) // innerClassAccessFlags
            ));
        }
    }

    /**
     * Compiles all of the methods for this declaration
     * <p>
     *   NB: as a side effect this will fill in the synthetic field map
     * </p
     */
    private void
    compileDeclaredMethods(TypeDeclaration typeDeclaration, ClassFile cf) throws CompileException {
        this.compileDeclaredMethods(typeDeclaration, cf, 0);
    }

    /**
     * Compiles methods for this declaration starting at <var>startPos</var>.
     *
     * @param startPos Starting parameter to fill in
     */
    private void
    compileDeclaredMethods(TypeDeclaration typeDeclaration, ClassFile cf, int startPos) throws CompileException {

        // Notice that as a side effect of compiling methods, synthetic "class-dollar" methods (which implement class
        // literals) are generated on-the fly. Hence, we must not use an Iterator here.

        for (int i = startPos; i < typeDeclaration.getMethodDeclarations().size(); ++i) {
            MethodDeclarator md = (MethodDeclarator) typeDeclaration.getMethodDeclarations().get(i);

            IMethod m                     = this.toIMethod(md);
            boolean overrides             = this.overridesMethodFromSupertype(m, this.resolve(md.getDeclaringType()));
            boolean hasOverrideAnnotation = this.hasAnnotation(md, this.iClassLoader.TYPE_java_lang_Override);
            if (overrides && !hasOverrideAnnotation && !(typeDeclaration instanceof InterfaceDeclaration)) {
                this.warning("MO", "Missing @Override", md.getLocation());
            } else
            if (!overrides && hasOverrideAnnotation) {
                this.compileError("Method does not override a method declared in a supertype", md.getLocation());
            }

            this.compile(md, cf);
        }
    }

    private boolean
    hasAnnotation(FunctionDeclarator fd, IClass annotationType) throws CompileException {
        for (Annotation a : Iterables.filterByClass(fd.getModifiers(), Annotation.class)) {
            if (this.getType(a.getType()) == annotationType) return true;
        }
        return false;
    }

    private boolean
    overridesMethodFromSupertype(IMethod m, IClass type) throws CompileException {

        // Check whether it overrides a method declared in the superclass (or any of its supertypes).
        {
            IClass superclass = type.getSuperclass();
            if (superclass != null && this.overridesMethod(m, superclass)) return true;
        }

        // Check whether it overrides a method declared in an interface (or any of its superinterfaces).
        IClass[] ifs = type.getInterfaces();
        for (IClass i : ifs) {
            if (this.overridesMethod(m, i)) return true;
        }

        // Special handling for interfaces that don't extend other interfaces: JLS7 dictates that these stem from
        // 'Object', but 'getSuperclass()' returns NULL for interfaces.
        if (ifs.length == 0 && type.isInterface()) {
            return this.overridesMethod(m, this.iClassLoader.TYPE_java_lang_Object);
        }

        return false;
    }

    /**
     * @return Whether <var>method</var> overrides a method of <var>type</var> or any of its supertypes
     */
    private boolean
    overridesMethod(IMethod method, IClass type) throws CompileException {

        // Check whether it overrides a method declared in THIS type.
        IMethod[] ms = type.getDeclaredIMethods(method.getName());
        for (IMethod m : ms) {
            if (Arrays.equals(method.getParameterTypes(), m.getParameterTypes())) return true;
        }

        // Check whether it overrides a method declared in a supertype.
        return this.overridesMethodFromSupertype(method, type);
    }

    /**
     * Generates and compiles a bridge method with signature <var>base</var> that delegates to <var>override</var>.
     */
    private void
    generateBridgeMethod(ClassFile cf, IClass declaringIClass, IMethod base, IMethod override) throws CompileException {

        if (
            !base.getReturnType().isAssignableFrom(override.getReturnType())
            || override.getReturnType() == IClass.VOID
        ) {
            this.compileError(
                "The return type of \""
                + override
                + "\" is incompatible with that of \""
                + base
                + "\""
            );
            return;
        }

        ClassFile.MethodInfo mi = cf.addMethodInfo(
            (short) (Mod.PUBLIC | Mod.SYNTHETIC), // accessFlags
            base.getName(),
            base.getDescriptor()
        );

        // Add "Exceptions" attribute (JVMS 4.7.4).
        IClass[] thrownExceptions = base.getThrownExceptions();
        if (thrownExceptions.length > 0) {
            final short eani    = cf.addConstantUtf8Info("Exceptions");
            short[]     tecciis = new short[thrownExceptions.length];
            for (int i = 0; i < thrownExceptions.length; ++i) {
                tecciis[i] = cf.addConstantClassInfo(thrownExceptions[i].getDescriptor());
            }
            mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, tecciis));
        }

        final CodeContext codeContext      = new CodeContext(mi.getClassFile(), base.getParameterTypes());
        final CodeContext savedCodeContext = this.replaceCodeContext(codeContext);
        try {

            // Allocate all our local variables.
            codeContext.saveLocalVariables();
            codeContext.allocateLocalVariable((short) 1, "this", override.getDeclaringIClass());
            IClass[]            paramTypes = override.getParameterTypes();
            LocalVariableSlot[] locals     = new LocalVariableSlot[paramTypes.length];
            for (int i = 0; i < paramTypes.length; ++i) {
                locals[i] = codeContext.allocateLocalVariable(
                    Descriptor.size(paramTypes[i].getDescriptor()),
                    "param" + i,
                    paramTypes[i]
                );
            }

            this.load(Located.NOWHERE, declaringIClass, 0);
            for (LocalVariableSlot l : locals) this.load(Located.NOWHERE, l.getType(), l.getSlotIndex());
            this.invoke(Located.NOWHERE, override);
            this.xreturn(Located.NOWHERE, base.getReturnType());

        } finally {
            this.replaceCodeContext(savedCodeContext);
        }

//        codeContext.flowAnalysis(override.getName());

        final short smtani = cf.addConstantUtf8Info("StackMapTable");

        // Add the code context as a code attribute to the MethodInfo.
        mi.addAttribute(new ClassFile.AttributeInfo(cf.addConstantUtf8Info("Code")) {

            @Override protected void
            storeBody(DataOutputStream dos) throws IOException {
                codeContext.storeCodeAttributeBody(
                    dos,
                    (short) 0, // lineNumberTableAttributeNameIndex    - optional
                    (short) 0, // localVariableTableAttributeNameIndex - optional
                    smtani     // stackMapTableAttributeNameIndex      - mandatory
                );
            }
        });
    }

    /**
     * @return Whether this statement can complete normally (JLS7 14.1)
     */
    private boolean
    compile(BlockStatement bs) throws CompileException {

        Boolean result = (Boolean) bs.accept(new BlockStatementVisitor<Boolean, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLengthCheck:23
            @Override public Boolean visitInitializer(Initializer i)                                                throws CompileException { return UnitCompiler.this.compile2(i);    }
            @Override public Boolean visitFieldDeclaration(FieldDeclaration fd)                                     throws CompileException { return UnitCompiler.this.compile2(fd);   }
            @Override public Boolean visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return UnitCompiler.this.compile2(ls);   }
            @Override public Boolean visitBlock(Block b)                                                            throws CompileException { return UnitCompiler.this.compile2(b);    }
            @Override public Boolean visitExpressionStatement(ExpressionStatement es)                               throws CompileException { return UnitCompiler.this.compile2(es);   }
            @Override public Boolean visitIfStatement(IfStatement is)                                               throws CompileException { return UnitCompiler.this.compile2(is);   }
            @Override public Boolean visitForStatement(ForStatement fs)                                             throws CompileException { return UnitCompiler.this.compile2(fs);   }
            @Override public Boolean visitForEachStatement(ForEachStatement fes)                                    throws CompileException { return UnitCompiler.this.compile2(fes);  }
            @Override public Boolean visitWhileStatement(WhileStatement ws)                                         throws CompileException { return UnitCompiler.this.compile2(ws);   }
            @Override public Boolean visitTryStatement(TryStatement ts)                                             throws CompileException { return UnitCompiler.this.compile2(ts);   }
            @Override public Boolean visitSwitchStatement(SwitchStatement ss)                                       throws CompileException { return UnitCompiler.this.compile2(ss);   }
            @Override public Boolean visitSynchronizedStatement(SynchronizedStatement ss)                           throws CompileException { return UnitCompiler.this.compile2(ss);   }
            @Override public Boolean visitDoStatement(DoStatement ds)                                               throws CompileException { return UnitCompiler.this.compile2(ds);   }
            @Override public Boolean visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return UnitCompiler.this.compile2(lvds); }
            @Override public Boolean visitReturnStatement(ReturnStatement rs)                                       throws CompileException { return UnitCompiler.this.compile2(rs);   }
            @Override public Boolean visitThrowStatement(ThrowStatement ts)                                         throws CompileException { return UnitCompiler.this.compile2(ts);   }
            @Override public Boolean visitBreakStatement(BreakStatement bs)                                         throws CompileException { return UnitCompiler.this.compile2(bs);   }
            @Override public Boolean visitContinueStatement(ContinueStatement cs)                                   throws CompileException { return UnitCompiler.this.compile2(cs);   }
            @Override public Boolean visitAssertStatement(AssertStatement as)                                       throws CompileException { return UnitCompiler.this.compile2(as);   }
            @Override public Boolean visitEmptyStatement(EmptyStatement es)                                                                 { return UnitCompiler.this.compile2(es);   }
            @Override public Boolean visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       throws CompileException { return UnitCompiler.this.compile2(lcds); }
            @Override public Boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        throws CompileException { return UnitCompiler.this.compile2(aci);  }
            @Override public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation sci)                throws CompileException { return UnitCompiler.this.compile2(sci);  }
        });

        assert result != null;
        return result;
    }

    /**
     * Called to check whether the given {@link Rvalue} compiles or not.
     *
     * @return Whether the block statement can complete normally
     */
    private boolean
    fakeCompile(BlockStatement bs) throws CompileException {

        Offset from = this.getCodeContext().newOffset();

        boolean ccn = this.compile(bs);

        Offset to = this.getCodeContext().newOffset();

        this.getCodeContext().removeCode(from, to);

        return ccn;
    }

    private CodeContext
    getCodeContext() {
        assert this.codeContext != null;
        return this.codeContext;
    }

    private boolean
    compile2(Initializer i) throws CompileException {

        this.buildLocalVariableMap(i.block, new HashMap<String, LocalVariable>());

        return this.compile(i.block);
    }

    private boolean
    compile2(Block b) throws CompileException {
        this.getCodeContext().saveLocalVariables();
        try {
            return this.compileStatements(b.statements);
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }
    }

    private boolean
    compileStatements(List<? extends BlockStatement> statements) throws CompileException {
        boolean previousStatementCanCompleteNormally = true;
        for (BlockStatement bs : statements) {
            if (!previousStatementCanCompleteNormally && this.generatesCode(bs)) {
                this.compileError("Statement is unreachable", bs.getLocation());
                break;
            }
            previousStatementCanCompleteNormally = this.compile(bs);
        }
        return previousStatementCanCompleteNormally;
    }

    private boolean
    compile2(DoStatement ds) throws CompileException {
        Object cvc = this.getConstantValue(ds.condition);
        if (cvc != UnitCompiler.NOT_CONSTANT) {
            if (Boolean.TRUE.equals(cvc)) {
                this.warning("DSTC", (
                    "Condition of DO statement is always TRUE; "
                    + "the proper way of declaring an unconditional loop is \"for (;;)\""
                ), ds.getLocation());
                return this.compileUnconditionalLoop(ds, ds.body, null);
            } else
            {
                this.warning("DSNR", "DO statement never repeats", ds.getLocation());
            }
        }

        final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();

        // Compile body.
        ds.whereToContinue = null;
        if (!this.compile(ds.body) && ds.whereToContinue == null) {
            this.warning("DSNTC", "\"do\" statement never tests its condition", ds.getLocation());

            Offset wtb = ds.whereToBreak;
            if (wtb == null) return false;

            wtb.set();
            ds.whereToBreak = null;

            return true;
        }
        if (ds.whereToContinue != null) {
            ds.whereToContinue.set();
            ds.whereToContinue = null;
        }

        // Compile condition.
        this.compileBoolean(ds.condition, bodyOffset, UnitCompiler.JUMP_IF_TRUE);

        if (ds.whereToBreak != null) {
            ds.whereToBreak.set();
            ds.whereToBreak = null;
        }

        return true;
    }

    private boolean
    compile2(ForStatement fs) throws CompileException {

        this.getCodeContext().saveLocalVariables();
        try {
            BlockStatement oi = fs.init;
            Rvalue[]       ou = fs.update;
            Rvalue         oc = fs.condition;

            // Compile initializer.
            if (oi != null) this.compile(oi);

            if (oc == null) {
                return this.compileUnconditionalLoop(fs, fs.body, ou);
            }

            Object cvc = this.getConstantValue(oc);
            if (cvc != UnitCompiler.NOT_CONSTANT) {
                if (Boolean.TRUE.equals(cvc)) {
                    this.warning("FSTC", (
                        "Condition of FOR statement is always TRUE; "
                        + "the proper way of declaring an unconditional loop is \"for (;;)\""
                    ), fs.getLocation());
                    return this.compileUnconditionalLoop(fs, fs.body, ou);
                } else
                {
                    this.warning("FSNR", "FOR statement never repeats", fs.getLocation());
                }
            }

            CodeContext.Offset toCondition = this.getCodeContext().new Offset();
            this.gotO(fs, toCondition);

            // Compile body.
            fs.whereToContinue = null;
            final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();
            boolean                  bodyCcn    = this.compile(fs.body);
            if (fs.whereToContinue != null) fs.whereToContinue.set();

            // Compile update.
            if (ou != null) {
                if (!bodyCcn && fs.whereToContinue == null) {
                    this.warning("FUUR", "For update is unreachable", fs.getLocation());
                } else
                {
                    for (Rvalue rv : ou) this.compile(rv);
                }
            }
            fs.whereToContinue = null;

            // Compile condition.
            toCondition.set();
            this.compileBoolean(oc, bodyOffset, UnitCompiler.JUMP_IF_TRUE);
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }

        if (fs.whereToBreak != null) {
            fs.whereToBreak.set();
            fs.whereToBreak = null;
        }

        return true;
    }

    private boolean
    compile2(ForEachStatement fes) throws CompileException {
        IClass expressionType = this.getType(fes.expression);

        if (expressionType.isArray()) {
            this.getCodeContext().saveLocalVariables();
            try {

                // Allocate the local variable for the current element.
                LocalVariable elementLv = this.getLocalVariable(fes.currentElement, false);
                elementLv.setSlot(this.getCodeContext().allocateLocalVariable(
                    Descriptor.size(elementLv.type.getDescriptor()),
                    fes.currentElement.name,
                    elementLv.type
                ));

                // Compile initializer.
                this.compileGetValue(fes.expression);
                short expressionLv = this.getCodeContext().allocateLocalVariable((short) 1);
                this.store(fes.expression, expressionType, expressionLv);

                this.consT(fes, 0);
                LocalVariable indexLv = new LocalVariable(false, IClass.INT);
                indexLv.setSlot(this.getCodeContext().allocateLocalVariable((short) 1, null, indexLv.type));
                this.store(fes, indexLv);

                CodeContext.Offset toCondition = this.getCodeContext().new Offset();
                this.gotO(fes, toCondition);

                // Compile the body.
                fes.whereToContinue = null;
                final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();

                this.load(fes, expressionType, expressionLv);
                this.load(fes, indexLv);
                IClass componentType = expressionType.getComponentType();
                assert componentType != null;
                this.xaload(fes.currentElement, componentType);
                this.assignmentConversion(fes.currentElement, componentType, elementLv.type, null);
                this.store(fes, elementLv);

                boolean bodyCcn = this.compile(fes.body);
                if (fes.whereToContinue != null) fes.whereToContinue.set();

                // Compile update.
                if (!bodyCcn && fes.whereToContinue == null) {
                    this.warning("FUUR", "For update is unreachable", fes.getLocation());
                } else {
                    this.iinc(fes, indexLv, "++");
                }
                fes.whereToContinue = null;

                // Compile condition.
                toCondition.set();
                this.load(fes, indexLv);
                this.load(fes, expressionType, expressionLv);
                this.arraylength(fes);
                this.if_icmpxx(fes, UnitCompiler.LT, bodyOffset);
            } finally {
                this.getCodeContext().restoreLocalVariables();
            }

            if (fes.whereToBreak != null) {
                fes.whereToBreak.set();
                fes.whereToBreak = null;
            }
        } else
        if (this.iClassLoader.TYPE_java_lang_Iterable.isAssignableFrom(expressionType)) {
            this.getCodeContext().saveLocalVariables();
            try {

                // Allocate the local variable for the current element.
                LocalVariable elementLv = this.getLocalVariable(fes.currentElement, false);
                elementLv.setSlot(this.getCodeContext().allocateLocalVariable(
                    (short) 1,
                    fes.currentElement.name,
                    elementLv.type
                ));

                // Compile initializer.
                this.compileGetValue(fes.expression);
                this.invoke(fes.expression, this.iClassLoader.METH_java_lang_Iterable__iterator);
                LocalVariable iteratorLv = new LocalVariable(false, this.iClassLoader.TYPE_java_util_Iterator);
                iteratorLv.setSlot(this.getCodeContext().allocateLocalVariable((short) 1, null, iteratorLv.type));
                this.store(fes, iteratorLv);

                CodeContext.Offset toCondition = this.getCodeContext().new Offset();
                this.gotO(fes, toCondition);

                // Compile the body.
                fes.whereToContinue = null;
                final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();

                this.load(fes, iteratorLv);
                this.invoke(fes.expression, this.iClassLoader.METH_java_util_Iterator__next);
                if (
                    !this.tryAssignmentConversion(
                        fes.currentElement,
                        this.iClassLoader.TYPE_java_lang_Object,
                        elementLv.type,
                        null
                    )
                    && !this.tryNarrowingReferenceConversion(
                        fes.currentElement,
                        this.iClassLoader.TYPE_java_lang_Object,
                        elementLv.type
                    )
                ) throw new AssertionError();
                this.store(fes, elementLv);

                boolean bodyCcn = this.compile(fes.body);
                if (fes.whereToContinue != null) fes.whereToContinue.set();

                // Compile update.
                if (!bodyCcn && fes.whereToContinue == null) {
                    this.warning("FUUR", "For update is unreachable", fes.getLocation());
                }
                fes.whereToContinue = null;

                // Compile condition.
                toCondition.set();
                this.load(fes, iteratorLv);
                this.invoke(fes.expression, this.iClassLoader.METH_java_util_Iterator__hasNext);
                this.ifxx(fes, UnitCompiler.NE, bodyOffset);
            } finally {
                this.getCodeContext().restoreLocalVariables();
            }

            if (fes.whereToBreak != null) {
                fes.whereToBreak.set();
                fes.whereToBreak = null;
            }
        } else
        {
            this.compileError("Cannot iterate over \"" + expressionType + "\"", fes.expression.getLocation());
        }
        return true;
    }

    private boolean
    compile2(WhileStatement ws) throws CompileException {
        Object cvc = this.getConstantValue(ws.condition);
        if (cvc != UnitCompiler.NOT_CONSTANT) {
            if (Boolean.TRUE.equals(cvc)) {
                this.warning("WSTC", (
                    "Condition of WHILE statement is always TRUE; "
                    + "the proper way of declaring an unconditional loop is \"for (;;)\""
                ), ws.getLocation());
                return this.compileUnconditionalLoop(ws, ws.body, null);
            } else
            {
                this.warning("WSNR", "WHILE statement never repeats", ws.getLocation());
            }
        }

        // Compile body.
        Offset wtc = (ws.whereToContinue = this.getCodeContext().new Offset());
        this.gotO(ws, wtc);
        final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();
        this.compile(ws.body); // Return value (CCN) is ignored.
        assert ws.whereToContinue == wtc;
        wtc.set();
        ws.whereToContinue = null;

        // Compile condition.
        this.compileBoolean(ws.condition, bodyOffset, UnitCompiler.JUMP_IF_TRUE);

        if (ws.whereToBreak != null) {
            ws.whereToBreak.set();
            ws.whereToBreak = null;
        }
        return true;
    }

    private boolean
    compileUnconditionalLoop(ContinuableStatement cs, BlockStatement body, @Nullable Rvalue[] update)
    throws CompileException {
        if (update != null) return this.compileUnconditionalLoopWithUpdate(cs, body, update);

        // Compile body.
        Offset wtc = (cs.whereToContinue = this.getCodeContext().newOffset());
        if (this.compile(body)) this.gotO(cs, wtc);
        cs.whereToContinue = null;

        Offset wtb = cs.whereToBreak;
        if (wtb == null) return false;

        wtb.set();
        cs.whereToBreak = null;

        return true;
    }
    private boolean
    compileUnconditionalLoopWithUpdate(ContinuableStatement cs, BlockStatement body, Rvalue[] update)
    throws CompileException {

        // Compile body.
        cs.whereToContinue = null;
        final CodeContext.Offset bodyOffset = this.getCodeContext().newOffset();
        boolean                  bodyCcn    = this.compile(body);

        // Compile the "update".
        if (cs.whereToContinue != null) cs.whereToContinue.set();
        if (!bodyCcn && cs.whereToContinue == null) {
            this.warning("LUUR", "Loop update is unreachable", update[0].getLocation());
        } else
        {
            for (Rvalue rv : update) this.compile(rv);
            this.gotO(cs, bodyOffset);
        }
        cs.whereToContinue = null;

        Offset wtb = cs.whereToBreak;
        if (wtb == null) return false;
        wtb.set();

        cs.whereToBreak = null;

        return true;
    }

    private boolean
    compile2(LabeledStatement ls) throws CompileException {
        boolean canCompleteNormally = this.compile(ls.body);

        Offset wtb = ls.whereToBreak;
        if (wtb == null) return canCompleteNormally;

        wtb.set();

        ls.whereToBreak = null;

        return true;
    }

    private enum SwitchKind { INT, ENUM, STRING }

    private boolean
    compile2(SwitchStatement ss) throws CompileException {

        SwitchKind kind;
        short      ssvLvIndex = -1; // Only relevant if kind == STRING.

        // Compute condition.
        IClass switchExpressionType = this.compileGetValue(ss.condition);
        if (this.iClassLoader.TYPE_java_lang_String == switchExpressionType) {

            kind = SwitchKind.STRING;

            // Store the string value in a (hidden) local variable, because after we do the SWITCH
            // on the string's hash code, we need to check for string equality with the CASE
            // labels.
            this.dup(ss);
            ssvLvIndex = this.getCodeContext().allocateLocalVariable((short) 1);
            this.store(
                ss,                                      // locatable
                this.iClassLoader.TYPE_java_lang_String, // lvType
                ssvLvIndex                               // lvIndex
            );

            this.invoke(ss, this.iClassLoader.METH_java_lang_String__hashCode);
        } else
        if (this.iClassLoader.TYPE_java_lang_Enum.isAssignableFrom(switchExpressionType)) {
            kind = SwitchKind.ENUM;
            this.invoke(ss, this.iClassLoader.METH_java_lang_Enum__ordinal);
        } else
        {
            kind = SwitchKind.INT;
            this.assignmentConversion(
                ss,                   // locatable
                switchExpressionType, // sourceType
                IClass.INT,           // targetType
                null                  // constantValue
            );
        }

        // Prepare the map of case labels to code offsets.
        TreeMap<Integer, CodeContext.Offset> caseLabelMap       = new TreeMap<Integer, CodeContext.Offset>();
        CodeContext.Offset                   defaultLabelOffset = null;
        CodeContext.Offset[]                 sbsgOffsets        = new CodeContext.Offset[ss.sbsgs.size()];
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i] = this.getCodeContext().new Offset();
            for (Rvalue caseLabel : sbsg.caseLabels) {

                Integer civ;

                switch (kind) {

                case ENUM:
                    CIV: {
                        if (!(caseLabel instanceof AmbiguousName)) {
                            this.compileError("Case label must be an enum constant", caseLabel.getLocation());
                            civ = 99;
                            break;
                        }
                        String[] identifiers = ((AmbiguousName) caseLabel).identifiers;
                        if (identifiers.length != 1) {
                            this.compileError("Case label must be a plain enum constant", caseLabel.getLocation());
                            civ = 99;
                            break;
                        }
                        String constantName = identifiers[0];

                        int ordinal = 0;
                        for (IField f : switchExpressionType.getDeclaredIFields()) {
                            if (f.getAccess() != Access.PUBLIC || !f.isStatic()) continue;
                            if (f.getName().equals(constantName)) {
                                civ = ordinal;
                                break CIV;
                            }
                            ordinal++;
                        }

                        this.compileError("Unknown enum constant \"" + constantName + "\"", caseLabel.getLocation());
                        civ = 99;
                    }

                    // Store in case label map.
                    if (caseLabelMap.containsKey(civ)) {
                        this.compileError("Duplicate \"case\" switch label value", caseLabel.getLocation());
                    }
                    caseLabelMap.put(civ, sbsgOffsets[i]);
                    break;

                case INT:
                    {
                        // Verify that case label value is a constant.
                        Object cv = this.getConstantValue(caseLabel);
                        if (cv == UnitCompiler.NOT_CONSTANT) {
                            this.compileError(
                                "Value of 'case' label does not pose a constant value",
                                caseLabel.getLocation()
                            );
                            civ = 99;
                            break;
                        }

                        // Convert char, byte, short, int to "Integer".
                        if (cv instanceof Integer) {
                            civ = (Integer) cv;
                        } else
                        if (cv instanceof Number) {
                            civ = new Integer(((Number) cv).intValue());
                        } else
                        if (cv instanceof Character) {
                            civ = new Integer(((Character) cv).charValue());
                        } else {
                            this.compileError(
                                "Value of case label must be a char, byte, short or int constant",
                                caseLabel.getLocation()
                            );
                            civ = new Integer(99);
                        }
                    }

                    // Store in case label map.
                    if (caseLabelMap.containsKey(civ)) {
                        this.compileError("Duplicate \"case\" switch label value", caseLabel.getLocation());
                    }
                    caseLabelMap.put(civ, sbsgOffsets[i]);
                    break;

                case STRING:
                    {

                        // Verify that the case label value is a string constant.
                        Object cv = this.getConstantValue(caseLabel);
                        if (!(cv instanceof String)) {
                            this.compileError(
                                "Value of 'case' label is not a string constant",
                                caseLabel.getLocation()
                            );
                            civ = 99;
                            break;
                        }

                        // Use the string constant's hash code as the SWITCH key.
                        civ = cv.hashCode();
                    }

                    // Store in case label map.
                    if (!caseLabelMap.containsKey(civ)) {
                        caseLabelMap.put(civ, this.getCodeContext().new Offset());
                    }
                    break;

                default:
                    throw new AssertionError(kind);
                }
            }

            if (sbsg.hasDefaultLabel) {
                if (defaultLabelOffset != null) {
                    this.compileError("Duplicate \"default\" switch label", sbsg.getLocation());
                }
                defaultLabelOffset = sbsgOffsets[i];
            }
        }
        if (defaultLabelOffset == null) defaultLabelOffset = this.getWhereToBreak(ss);

        // Generate TABLESWITCH or LOOKUPSWITCH instruction.
        CodeContext.Offset switchOffset = this.getCodeContext().newOffset();
        if (caseLabelMap.isEmpty()) {
            // Special case: SWITCH statement without CASE labels (but maybe a DEFAULT label).
            ;
        } else
        if (
            (Integer) caseLabelMap.firstKey() + caseLabelMap.size() // Beware of INT overflow!
            >= (Integer) caseLabelMap.lastKey() - caseLabelMap.size()
        ) {

            // The case label values are strictly consecutive or almost consecutive (at most 50%
            // 'gaps'), so let's use a TABLESWITCH.

            this.tableswitch(ss, caseLabelMap, switchOffset, defaultLabelOffset);
        } else
        {

            // The case label values are not 'consecutive enough', so use a LOOKUPSWITCH.
            this.lookupswitch(ss, caseLabelMap, switchOffset, defaultLabelOffset);
        }

        if (kind == SwitchKind.STRING) {

            // For STRING SWITCH, we must generate extra code that checks for string equality --
            // the strings' hash codes are not globally unique (as, e.g. MD5).
            for (Entry<Integer, CodeContext.Offset> e : caseLabelMap.entrySet()) {
                final Integer            caseHashCode = (Integer) e.getKey();
                final CodeContext.Offset offset       = (CodeContext.Offset) e.getValue();

                offset.set();

                Set<String> caseLabelValues = new HashSet<String>();
                for (int i = 0; i < ss.sbsgs.size(); i++) {
                    SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);

                    for (Rvalue caseLabel : sbsg.caseLabels) {

                        String cv = (String) this.getConstantValue(caseLabel);
                        assert cv != null;

                        if (!caseLabelValues.add(cv)) {
                            this.compileError(
                                "Duplicate case label \"" + cv + "\"",
                                caseLabel.getLocation()
                            );
                        }

                        if (cv.hashCode() != caseHashCode) continue;

                        this.load(sbsg, this.iClassLoader.TYPE_java_lang_String, ssvLvIndex);
                        this.consT(caseLabel, cv);
                        this.invoke(caseLabel, this.iClassLoader.METH_java_lang_String__equals__java_lang_Object);
                        this.ifxx(sbsg, UnitCompiler.NE, sbsgOffsets[i]);
                    }
                }

                this.gotO(ss, defaultLabelOffset);
            }
        }

        // Compile statement groups.
        boolean canCompleteNormally = true;
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i].set();
            canCompleteNormally = true;
            for (BlockStatement bs : sbsg.blockStatements) {
                if (!canCompleteNormally) {
                    this.compileError("Statement is unreachable", bs.getLocation());
                    break;
                }
                canCompleteNormally = this.compile(bs);
            }
        }

        Offset wtb = ss.whereToBreak;
        if (wtb == null) return canCompleteNormally;

        wtb.set();

        ss.whereToBreak = null;

        return true;
    }

    private boolean
    compile2(BreakStatement bs) throws CompileException {

        // Find the broken statement.
        BreakableStatement brokenStatement = null;
        if (bs.label == null) {
            for (
                Scope s = bs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof BreakableStatement) {
                    brokenStatement = (BreakableStatement) s;
                    break;
                }
            }
            if (brokenStatement == null) {
                this.compileError("\"break\" statement is not enclosed by a breakable statement", bs.getLocation());
                return false;
            }
        } else {
            for (
                Scope s = bs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof LabeledStatement) {
                    LabeledStatement ls = (LabeledStatement) s;
                    if (ls.label.equals(bs.label)) {
                        brokenStatement = ls;
                        break;
                    }
                }
            }
            if (brokenStatement == null) {
                this.compileError((
                    "Statement \"break "
                    + bs.label
                    + "\" is not enclosed by a breakable statement with label \""
                    + bs.label
                    + "\""
                ), bs.getLocation());
                return false;
            }
        }

        this.leaveStatements(
            bs.getEnclosingScope(),             // from
            brokenStatement.getEnclosingScope() // to
        );
        this.gotO(bs, this.getWhereToBreak(brokenStatement));
        return false;
    }

    private boolean
    compile2(ContinueStatement cs) throws CompileException {

        // Find the continued statement.
        ContinuableStatement continuedStatement = null;
        if (cs.label == null) {
            for (
                Scope s = cs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof ContinuableStatement) {
                    continuedStatement = (ContinuableStatement) s;
                    break;
                }
            }
            if (continuedStatement == null) {
                this.compileError(
                    "\"continue\" statement is not enclosed by a continuable statement",
                    cs.getLocation()
                );
                return false;
            }
        } else {
            for (
                Scope s = cs.getEnclosingScope();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof LabeledStatement) {
                    LabeledStatement ls = (LabeledStatement) s;
                    if (ls.label.equals(cs.label)) {
                        Statement st = ls.body;
                        while (st instanceof LabeledStatement) st = ((LabeledStatement) st).body;
                        if (!(st instanceof ContinuableStatement)) {
                            this.compileError("Labeled statement is not continuable", st.getLocation());
                            return false;
                        }
                        continuedStatement = (ContinuableStatement) st;
                        break;
                    }
                }
            }
            if (continuedStatement == null) {
                this.compileError((
                    "Statement \"continue "
                    + cs.label
                    + "\" is not enclosed by a continuable statement with label \""
                    + cs.label
                    + "\""
                ), cs.getLocation());
                return false;
            }
        }

        Offset wtc = continuedStatement.whereToContinue;
        if (wtc == null) {
            wtc = (continuedStatement.whereToContinue = this.getCodeContext().new Offset());
        }

        this.leaveStatements(
            cs.getEnclosingScope(),                // from
            continuedStatement.getEnclosingScope() // to
        );

        this.gotO(cs, wtc);

        return false;
    }

    private boolean
    compile2(AssertStatement as) throws CompileException {

        // assert expression1;
        //   if (!expression1) throw new AssertionError();
        // assert expression1 : expression2;
        //   if (!expression1) throw new AssertionError(expression2);
        CodeContext.Offset end = this.getCodeContext().new Offset();
        try {
            this.compileBoolean(as.expression1, end, UnitCompiler.JUMP_IF_TRUE);

            this.neW(as, this.iClassLoader.TYPE_java_lang_AssertionError);
            this.dup(as);

            Rvalue[] arguments = (
                as.expression2 == null
                ? new Rvalue[0]
                : new Rvalue[] { as.expression2 }
            );
            this.invokeConstructor(
                as,                                              // locatable
                as,                                              // scope
                null,                                            // enclosingInstance
                this.iClassLoader.TYPE_java_lang_AssertionError, // targetClass
                arguments                                        // arguments
            );
            this.getCodeContext().popUninitializedVariableOperand();
            this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_ASSERTIONERROR);
            this.athrow(as);
        } finally {
            end.set();
        }
        return true;
    }

    @SuppressWarnings("static-method") private boolean
    compile2(EmptyStatement es) { return true; }

    private boolean
    compile2(ExpressionStatement ee) throws CompileException {

        this.compile(ee.rvalue);

        return true;
    }

    private boolean
    compile2(FieldDeclaration fd) throws CompileException {
        final IClass declaringIClass = this.resolve(fd.getDeclaringType());
        for (VariableDeclarator vd : fd.variableDeclarators) {

            ArrayInitializerOrRvalue initializer = this.getNonConstantFinalInitializer(fd, vd);
            if (initializer == null) continue;

            // TODO: Compile annotations on fields.
//            assert fd.modifiers.annotations.length == 0 : fd.getLocation();

            this.addLineNumberOffset(vd);

            if (!declaringIClass.isInterface() && !fd.isStatic()) this.load(vd, declaringIClass, 0);

            IClass fieldType = this.getType(fd.type);
            if (initializer instanceof Rvalue) {
                Rvalue rvalue          = (Rvalue) initializer;
                IClass initializerType = this.compileGetValue(rvalue);
                fieldType = fieldType.getArrayIClass(vd.brackets, this.iClassLoader.TYPE_java_lang_Object);
                this.assignmentConversion(
                    fd,                           // locatable
                    initializerType,              // sourceType
                    fieldType,                    // targetType
                    this.getConstantValue(rvalue) // constantValue
                );
            } else
            if (initializer instanceof ArrayInitializer) {
                this.compileGetValue((ArrayInitializer) initializer, fieldType);
            } else
            {
                throw new InternalCompilerException(
                    "Unexpected array initializer or rvalue class "
                    + initializer.getClass().getName()
                );
            }

            // No need to check accessibility here.
            ;

            // TODO: Compile annotations on fields.
//            assert fd.modifiers.annotations.length == 0;

            IField iField = declaringIClass.getDeclaredIField(vd.name);
            assert iField != null : fd.getDeclaringType() + " has no field " + vd.name;
            this.putfield(fd, iField);
        }
        return true;
    }

    private boolean
    compile2(IfStatement is) throws CompileException {
        Object         cv = this.getConstantValue(is.condition);
        BlockStatement es = (
            is.elseStatement != null
            ? is.elseStatement
            : new EmptyStatement(is.thenStatement.getLocation())
        );
        if (cv instanceof Boolean) {

            // Constant condition.
            this.fakeCompile(is.condition);
            BlockStatement seeingStatement, blindStatement;
            if (((Boolean) cv).booleanValue()) {
                seeingStatement = is.thenStatement;
                blindStatement  = es;
            } else {
                seeingStatement = es;
                blindStatement  = is.thenStatement;
            }

            // Compile the seeing statement.
            final CodeContext.Inserter ins   = this.getCodeContext().newInserter();
            boolean                    ssccn = this.compile(seeingStatement);
            boolean                    bsccn = this.fakeCompile(blindStatement);
            if (ssccn) return true;
            if (!bsccn) return false;

            // Hm... the "seeing statement" cannot complete normally, but the "blind statement" can. Things are getting
            // complicated here! The robust solution is to compile the constant-condition-IF statement as a
            // non-constant-condition-IF statement. As an optimization, iff the IF-statement is enclosed ONLY by blocks,
            // then the remaining bytecode can be written to a "fake" code context, i.e. be thrown away.

            // Compile constant-condition-IF statement as non-constant-condition-IF statement.
            CodeContext.Offset off = this.getCodeContext().newOffset();

            this.getCodeContext().pushInserter(ins);
            try {
                this.consT(is, Boolean.FALSE);
                this.ifxx(is, UnitCompiler.NE, off);
            } finally {
                this.getCodeContext().popInserter();
            }

            return true;
        }

        // Non-constant condition.
        if (this.generatesCode(is.thenStatement)) {
            if (this.generatesCode(es)) {

                // if (expression) statement else statement
                CodeContext.Offset eso = this.getCodeContext().new Offset();
                CodeContext.Offset end = this.getCodeContext().new Offset();
                this.compileBoolean(is.condition, eso, UnitCompiler.JUMP_IF_FALSE);
                boolean tsccn = this.compile(is.thenStatement);
                if (tsccn) this.gotO(is, end);
                eso.setStackMap(null);
                eso.set();
                boolean esccn = this.compile(es);
                end.set();
                return tsccn || esccn;
            } else {

                // if (expression) statement else ;
                CodeContext.Offset end = this.getCodeContext().new Offset();
                this.compileBoolean(is.condition, end, UnitCompiler.JUMP_IF_FALSE);
                this.compile(is.thenStatement);
                end.setStackMap(null);
                end.set();
                return true;
            }
        } else {
            if (this.generatesCode(es)) {

                // if (expression) ; else statement
                CodeContext.Offset end = this.getCodeContext().new Offset();
                this.compileBoolean(is.condition, end, UnitCompiler.JUMP_IF_TRUE);
                this.compile(es);
                end.setStackMap(null);
                end.set();
                return true;
            } else {

                // if (expression) ; else ;
                IClass conditionType = this.compileGetValue(is.condition);
                if (conditionType != IClass.BOOLEAN) this.compileError("Not a boolean expression", is.getLocation());
                this.pop(is, conditionType);
                return true;
            }
        }
    }

    private boolean
    compile2(LocalClassDeclarationStatement lcds) throws CompileException {

        // Check for redefinition.
        LocalClassDeclaration otherLcd = UnitCompiler.findLocalClassDeclaration(lcds, lcds.lcd.name);
        if (otherLcd != null && otherLcd != lcds.lcd) {
            this.compileError(
                "Redeclaration of local class \""
                + lcds.lcd.name
                + "\"; previously declared in "
                + otherLcd.getLocation()
            );
        }

        this.compile(lcds.lcd);
        return true;
    }

    /**
     * Finds a local class declared in any block enclosing the given block statement.
     */
    @Nullable private static LocalClassDeclaration
    findLocalClassDeclaration(Scope s, String name) {

        if (s instanceof CompilationUnit) return null;

        for (;;) {
            Scope es = s.getEnclosingScope();
            if (es instanceof CompilationUnit) break;
            if (
                s instanceof BlockStatement
                && (es instanceof Block || es instanceof FunctionDeclarator)
            ) {
                BlockStatement                 bs         = (BlockStatement) s;
                List<? extends BlockStatement> statements = (
                    es instanceof BlockStatement
                    ? ((Block) es).statements
                    : ((FunctionDeclarator) es).statements
                );
                if (statements != null) {
                    for (BlockStatement bs2 : statements) {
                        if (bs2 instanceof LocalClassDeclarationStatement) {
                            LocalClassDeclarationStatement lcds = ((LocalClassDeclarationStatement) bs2);
                            if (lcds.lcd.name.equals(name)) return lcds.lcd;
                        }
                        if (bs2 == bs) break;
                    }
                }
            }
            s = es;
        }

        return null;
    }

    private boolean
    compile2(LocalVariableDeclarationStatement lvds) throws CompileException {

        // Ignore annotations here.

        for (VariableDeclarator vd : lvds.variableDeclarators) {

            try {
                LocalVariable lv = this.getLocalVariable(lvds, vd);
                lv.setSlot(
                    this.getCodeContext().allocateLocalVariable(Descriptor.size(lv.type.getDescriptor()), vd.name, lv.type)
                );

                ArrayInitializerOrRvalue oi = vd.initializer;
                if (oi != null) {
                    if (oi instanceof Rvalue) {
                        Rvalue rhs = (Rvalue) oi;
                        this.assignmentConversion(
                            lvds,                      // locatable
                            this.compileGetValue(rhs), // sourceType
                            lv.type,                   // targetType
                            this.getConstantValue(rhs) // constantValue
                        );
                    } else
                    if (oi instanceof ArrayInitializer) {
                        this.compileGetValue((ArrayInitializer) oi, lv.type);
                    } else
                    {
                        throw new InternalCompilerException(
                            "Unexpected rvalue or array initialized class "
                            + oi.getClass().getName()
                        );
                    }
                    this.store(lvds, lv);
                }
            } catch (RuntimeException re) {
                throw new RuntimeException(vd.getLocation().toString(), re);
            }
        }
        return true;
    }

    private VerificationTypeInfo
    verificationTypeInfo(@Nullable IClass type) {

        if (type == null) return ClassFile.StackMapTableAttribute.NULL_VARIABLE_INFO; // TODO Is that the right thing to do?

        String fd = type.getDescriptor();
        if (
            Descriptor.BOOLEAN.equals(fd)
            || Descriptor.BYTE.equals(fd)
            || Descriptor.CHAR.equals(fd)
            || Descriptor.INT.equals(fd)
            || Descriptor.SHORT.equals(fd)
        ) return ClassFile.StackMapTableAttribute.INTEGER_VARIABLE_INFO;
        if (Descriptor.LONG.equals(fd))   return ClassFile.StackMapTableAttribute.LONG_VARIABLE_INFO;
        if (Descriptor.FLOAT.equals(fd))  return ClassFile.StackMapTableAttribute.FLOAT_VARIABLE_INFO;
        if (Descriptor.DOUBLE.equals(fd)) return ClassFile.StackMapTableAttribute.DOUBLE_VARIABLE_INFO;
        if (
            Descriptor.isClassOrInterfaceReference(fd)
            || Descriptor.isArrayReference(fd)
        ) return new ObjectVariableInfo(this.getCodeContext().getClassFile().addConstantClassInfo(fd), fd);

        throw new InternalCompilerException("Cannot make VerificationTypeInfo from \"" + fd + "\"");
    }

    /**
     * @return The {@link LocalVariable} corresponding with the local variable declaration/declarator
     */
    public LocalVariable
    getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException {

        if (vd.localVariable != null) return vd.localVariable;

        // Determine variable type.
        Type variableType = lvds.type;
        for (int k = 0; k < vd.brackets; ++k) variableType = new ArrayType(variableType);

        // Ignore "lvds.modifiers.annotations".

        return (vd.localVariable = new LocalVariable(
            lvds.isFinal(),            // finaL
            this.getType(variableType) // type
        ));
    }

    private boolean
    compile2(ReturnStatement rs) throws CompileException {

        // Determine enclosing block, function and compilation Unit.
        FunctionDeclarator enclosingFunction = null;
        {
            Scope s = rs.getEnclosingScope();
            while (s instanceof Statement || s instanceof CatchClause) s = s.getEnclosingScope();
            enclosingFunction = (FunctionDeclarator) s;
        }

        Rvalue orv = rs.returnValue;

        IClass returnType = this.getReturnType(enclosingFunction);
        if (returnType == IClass.VOID) {
            if (orv != null) this.compileError("Method must not return a value", rs.getLocation());
            this.leaveStatements(
                rs.getEnclosingScope(), // from
                enclosingFunction       // to
            );

            this.returN(rs);
            return false;
        }

        if (orv == null) {
            this.compileError("Method must return a value", rs.getLocation());
            return false;
        }
        IClass type = this.compileGetValue(orv);
        this.assignmentConversion(
            rs,                        // locatable
            type,                      // sourceType
            returnType,                // targetType
            this.getConstantValue(orv) // constantValue
        );

        this.leaveStatements(
            rs.getEnclosingScope(), // from
            enclosingFunction       // to
        );
        this.xreturn(rs, returnType);
        return false;
    }

    private boolean
    compile2(SynchronizedStatement ss) throws CompileException {

        // Evaluate monitor object expression.
        if (!this.iClassLoader.TYPE_java_lang_Object.isAssignableFrom(this.compileGetValue(ss.expression))) {
            this.compileError(
                "Monitor object of \"synchronized\" statement is not a subclass of \"Object\"",
                ss.getLocation()
            );
        }

        this.getCodeContext().saveLocalVariables();
        boolean canCompleteNormally = false;
        try {

            // Allocate a local variable for the monitor object.
            ss.monitorLvIndex = this.getCodeContext().allocateLocalVariable((short) 1);

            // Store the monitor object.
            this.dup(ss);
            this.store(ss, this.iClassLoader.TYPE_java_lang_Object, ss.monitorLvIndex);

            // Create lock on the monitor object.
            this.monitorenter(ss);

            // Compile the statement body.
            final CodeContext.Offset monitorExitOffset = this.getCodeContext().new Offset();
            final CodeContext.Offset beginningOfBody   = this.getCodeContext().newOffset();
            canCompleteNormally = this.compile(ss.body);
            if (canCompleteNormally) {
                this.gotO(ss, monitorExitOffset);
            }

            // Generate the exception handler.
            CodeContext.Offset here = this.getCodeContext().newOffset();
            this.getCodeContext().addExceptionTableEntry(
                beginningOfBody, // startPC
                here,            // endPC
                here,            // handlerPC
                null             // catchTypeFD
            );
            this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_THROWABLE);
            this.leave(ss);
            this.athrow(ss);

            // Unlock monitor object.
            if (canCompleteNormally) {
                monitorExitOffset.set();
                this.leave(ss);
            }
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }

        return canCompleteNormally;
    }

    private boolean
    compile2(ThrowStatement ts) throws CompileException {
        IClass expressionType = this.compileGetValue(ts.expression);
        this.checkThrownException(
            ts,                    // locatable
            expressionType,        // type
            ts.getEnclosingScope() // scope
        );
        this.athrow(ts);
        return false;
    }

    /**
     * Interface for delayed code generation.
     */
    interface Compilable2 { boolean compile() throws CompileException; }

    private boolean
    compile2(final TryStatement ts) throws CompileException {

        return this.compileTryCatchFinallyWithResources(
            ts,                 // tryStatement
            ts.resources,       // resources
            new Compilable2() { // compileBody

                @Override public boolean
                compile() throws CompileException { return UnitCompiler.this.compile(ts.body); }
            },
            ts.finallY          // finallY
        );
    }

    /**
     * Generates code for a TRY statement with (possibly zero) resources and an (optional) FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileTryCatchFinallyWithResources(
        final TryStatement          ts,
        List<TryStatement.Resource> resources,
        final Compilable2           compileBody,
        @Nullable final Block       finallY
    ) throws CompileException {

        // Short-circuit for zero resources.
        if (resources.isEmpty()) {
            return this.compileTryCatchFinally(ts, compileBody, finallY);
        }

        // Prepare recursion for all declared resources.
        TryStatement.Resource             firstResource      = (TryStatement.Resource) resources.get(0);
        final List<TryStatement.Resource> followingResources = resources.subList(1, resources.size());

        final Location loc = firstResource.getLocation();
        final IClass   tt  = this.iClassLoader.TYPE_java_lang_Throwable;

        this.getCodeContext().saveLocalVariables();
        try {

            LocalVariable
            identifier = (LocalVariable) firstResource.accept(
                new Visitor.TryStatementResourceVisitor<LocalVariable, CompileException>() {

                    @Override @Nullable public LocalVariable
                    visitLocalVariableDeclaratorResource(LocalVariableDeclaratorResource lvdr) throws CompileException {

                        // final {VariableModifierNoFinal} R Identifier = Expression
                        IClass        lvType = UnitCompiler.this.getType(lvdr.type);
                        LocalVariable result = new LocalVariable(true, lvType);
                        result.setSlot(
                            UnitCompiler.this.getCodeContext().allocateLocalVariable(
                                Descriptor.size(lvType.getDescriptor()), // size
                                null,                                    // name
                                lvType                                   // type
                            )
                        );
                        ArrayInitializerOrRvalue oi = lvdr.variableDeclarator.initializer;
                        if (oi instanceof Rvalue) {
                            UnitCompiler.this.compileGetValue((Rvalue) oi);
                        } else
                        if (oi instanceof ArrayInitializer) {
                            UnitCompiler.this.compileGetValue((ArrayInitializer) oi, lvType);
                        } else
                        {
                            throw new InternalCompilerException(String.valueOf(oi));
                        }
                        UnitCompiler.this.store(ts, result);
                        return result;
                    }

                    @Override @Nullable public LocalVariable
                    visitVariableAccessResource(VariableAccessResource var) throws CompileException {

                        // Expression
                        if (
                            !UnitCompiler.this.options.contains(
                                JaninoOption.EXPRESSIONS_IN_TRY_WITH_RESOURCES_ALLOWED
                            )
                            && !(var.variableAccess instanceof AmbiguousName)
                            && !(var.variableAccess instanceof FieldAccessExpression)
                            && !(var.variableAccess instanceof SuperclassFieldAccessExpression)
                        ) {
                            throw new CompileException(
                                var.variableAccess.getClass().getSimpleName() + " rvalue not allowed as a resource",
                                var.getLocation()
                            );
                        }
                        IClass        lvType = UnitCompiler.this.compileGetValue(var.variableAccess);
                        LocalVariable result = new LocalVariable(true, lvType);
                        result.setSlot(
                            UnitCompiler.this.getCodeContext().allocateLocalVariable(
                                Descriptor.size(lvType.getDescriptor()), // size
                                null,                                    // name
                                lvType                                   // type
                            )
                        );
                        UnitCompiler.this.store(ts, result);
                        return result;
                    }
                }
            );
            assert identifier != null;

            // Throwable #primaryExc = null;
            LocalVariable primaryExc = new LocalVariable(true, tt);
            primaryExc.setSlot(
                this.getCodeContext().allocateLocalVariable(
                    Descriptor.size(tt.getDescriptor()),
                    null, // name
                    tt
                )
            );
            this.consT(ts, (Object) null);
            this.store(ts, primaryExc);

            CatchParameter suppressedException = new CatchParameter(
                loc,                                    // location
                false,                                  // finaL
                new Type[] { new SimpleType(loc, tt) }, // types
                "___"                                   // name
            );

            // Generate the FINALLY clause for the TRY-with-resources statement; see JLS9 14.20.3.1.
            // if (Identifier != null) {
            //     if (#primaryExc != null) {
            //         try {
            //             Identifier.close();
            //         } catch (Throwable #suppressedExc) {
            //             // Only iff Java >= 7:
            //             #primaryExc.addSuppressed(#suppressedExc);
            //         }
            //     } else {
            //         Identifier.close();
            //     }
            // }
            BlockStatement afterClose = (
                this.iClassLoader.METH_java_lang_Throwable__addSuppressed == null
                ? new EmptyStatement(loc)
                : new ExpressionStatement(
                    new MethodInvocation(
                        loc,                                      // location
                        new LocalVariableAccess(loc, primaryExc), // target
                        "addSuppressed",                          // methodName
                        new Rvalue[] {                            // arguments
                            new LocalVariableAccess(loc, this.getLocalVariable(suppressedException)),
                        }
                    )
                )
            );
            BlockStatement f = new IfStatement(
                loc,                 // location
                new BinaryOperation( // condition
                    loc,                                      // location
                    new LocalVariableAccess(loc, identifier), // lhs
                    "!=",                                     // operator
                    new NullLiteral(loc)                      // rhs
                ),
                new IfStatement(     // thenStatement
                    loc,                         // location
                    new BinaryOperation(         // condition
                        loc,                                      // location
                        new LocalVariableAccess(loc, primaryExc), // lhs
                        "!=",                                     // operator
                        new NullLiteral(loc)                      // rhs
                    ),
                    new TryStatement(            // thenStatement
                        loc,                                       // location
                        new ExpressionStatement(                   // body
                            new MethodInvocation(loc, new LocalVariableAccess(loc, identifier), "close", new Rvalue[0])
                        ),
                        Collections.singletonList(new CatchClause( // catchClauses
                            loc,                   // location
                            suppressedException,   // caughtException
                            afterClose             // body
                        ))
                    ),
                    new ExpressionStatement(     // elseStatement
                        new MethodInvocation(loc, new LocalVariableAccess(loc, identifier), "close", new Rvalue[0])
                    )
                )
            );
            f.setEnclosingScope(ts);

            // Recurse with one resource less.
            return this.compileTryCatchFinally(
                ts,                 // tryStatement
                new Compilable2() { // compileBody

                    @Override public boolean
                    compile() throws CompileException {
                        return UnitCompiler.this.compileTryCatchFinallyWithResources(
                            ts,
                            followingResources,
                            compileBody,
                            finallY
                        );
                    }
                },
                f                   // finallY
            );
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }
    }

    /**
     * Generates code for a TRY statement without resources, but with an (optional) FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileTryCatchFinally(
        final TryStatement             ts,
        final Compilable2              compileBody,
        @Nullable final BlockStatement finallY
    ) throws CompileException {

        if (finallY == null) {
            final CodeContext.Offset beginningOfBody = this.getCodeContext().newOffset();
            final CodeContext.Offset afterStatement  = this.getCodeContext().new Offset();

            boolean canCompleteNormally = this.compileTryCatch(ts, compileBody, beginningOfBody, afterStatement);
            afterStatement.set();
            return canCompleteNormally;
        }

        // Compile a TRY statement *with* a FINALLY clause.

        final CodeContext.Offset afterStatement = this.getCodeContext().new Offset();
        boolean                  canCompleteNormally;

        this.getCodeContext().saveLocalVariables();
        try {

            final CodeContext.Offset beginningOfBody = this.getCodeContext().newOffset();
            canCompleteNormally = this.compileTryCatch(ts, compileBody, beginningOfBody, afterStatement);

            // Generate the "catch (Throwable) {" clause that invokes the FINALLY subroutine.
            this.getCodeContext().saveLocalVariables();
            try {

                CodeContext.Offset here = this.getCodeContext().newOffset();
                this.getCodeContext().addExceptionTableEntry(
                    beginningOfBody, // startPC
                    here,            // endPC
                    here,            // handlerPC
                    null             // catchTypeFD
                );

                // Push the exception on the operand stack.
                this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_THROWABLE);

                // Save the exception object in an anonymous local variable.
                short evi = this.getCodeContext().allocateLocalVariable((short) 1);
                this.store(
                    finallY,                                    // locatable
                    this.iClassLoader.TYPE_java_lang_Throwable, // lvType
                    evi                                         // lvIndex
                );

                if (this.compile(finallY)) {

                    this.load(
                        finallY,                                    // locatable
                        this.iClassLoader.TYPE_java_lang_Throwable, // type
                        evi                                         // index
                    );
                    this.athrow(finallY);
                }
            } finally {

                // The exception object local variable allocated above MUST NOT BE RELEASED until after the FINALLY
                // block is compiled, for otherwise you get
                //   java.lang.VerifyError: ... Accessing value from uninitialized register 7
                this.getCodeContext().restoreLocalVariables();
            }
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }

        afterStatement.set();

        if (canCompleteNormally) canCompleteNormally = UnitCompiler.this.compile(finallY);

        return canCompleteNormally;
    }

    /**
     * Generates code for a TRY statement without resources and without a FINALLY clause.
     *
     * @return Whether the code can complete normally
     */
    private boolean
    compileTryCatch(
        TryStatement             tryStatement,
        Compilable2              compileBody,
        final CodeContext.Offset beginningOfBody,
        final CodeContext.Offset afterStatement
    ) throws CompileException {

        // Initialize all catch clauses as "unreachable" only to check later that they ARE indeed reachable.
        for (CatchClause catchClause : tryStatement.catchClauses) {
            catchClause.reachable = false;
            for (Type t : catchClause.catchParameter.types) {
                IClass caughtExceptionType = this.getType(t);
                catchClause.reachable |= (
                    // Superclass or subclass of "java.lang.Error"?
                    this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(caughtExceptionType)
                    || caughtExceptionType.isAssignableFrom(this.iClassLoader.TYPE_java_lang_Error)
                    // Superclass or subclass of "java.lang.RuntimeException"?
                    || this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(caughtExceptionType)
                    || caughtExceptionType.isAssignableFrom(this.iClassLoader.TYPE_java_lang_RuntimeException)
                );
            }
        }

        StackMap smBeforeBody = this.getCodeContext().currentInserter().getStackMap();

        boolean tryCatchCcn = compileBody.compile();

        CodeContext.Offset afterBody = this.getCodeContext().newOffset();

        if (tryCatchCcn) {
            this.gotO(tryStatement, afterStatement);
        }

        if (beginningOfBody.offset != afterBody.offset) { // Avoid zero-length exception table entries.
            for (int i = 0; i < tryStatement.catchClauses.size(); ++i) {
                this.getCodeContext().currentInserter().setStackMap(smBeforeBody);

                this.getCodeContext().saveLocalVariables();
                try {

                    CatchClause catchClause = (CatchClause) tryStatement.catchClauses.get(i);

                    if (catchClause.catchParameter.types.length != 1) {
                        throw UnitCompiler.compileException(catchClause, "Multi-type CATCH parameter NYI");
                    }
                    IClass caughtExceptionType = this.getType(catchClause.catchParameter.types[0]);

                    // Verify that the CATCH clause is reachable.
                    if (!catchClause.reachable) {
                        this.compileError("Catch clause is unreachable", catchClause.getLocation());
                    }

                    // Push the exception on the operand stack.
                    this.getCodeContext().pushObjectOperand(caughtExceptionType.getDescriptor());

                    // Allocate the "exception variable".
                    LocalVariableSlot exceptionVarSlot = this.getCodeContext().allocateLocalVariable(
                        (short) 1,
                        catchClause.catchParameter.name,
                        caughtExceptionType
                    );
                    final short evi = exceptionVarSlot.getSlotIndex();

                    // Kludge: Treat the exception variable like a local variable of the catch clause body.
                    this.getLocalVariable(catchClause.catchParameter).setSlot(exceptionVarSlot);

                    this.getCodeContext().addExceptionTableEntry(
                        beginningOfBody,                    // startPC
                        afterBody,                          // endPC
                        this.getCodeContext().newOffset(),  // handlerPC
                        caughtExceptionType.getDescriptor() // catchTypeFD
                    );
                    this.store(
                        catchClause,         // locatable
                        caughtExceptionType, // lvType
                        evi                  // lvIndex
                    );

                    if (this.compile(catchClause.body)) {

                        if (tryStatement.finallY == null || this.compile(tryStatement.finallY)) {
                            tryCatchCcn = true;
                            this.gotO(catchClause, afterStatement);
                        }
                    }
                } finally {
                    this.getCodeContext().restoreLocalVariables();
                }
            }
        }

        this.getCodeContext().currentInserter().setStackMap(smBeforeBody);

        return tryCatchCcn;
    }

    // ------------ FunctionDeclarator.compile() -------------

    private void
    compile(FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        try {
            this.compile2(fd, classFile);
        } catch (ClassFileException cfe) {
            throw new ClassFileException("Compiling \"" + fd + "\": " + cfe.getMessage(), cfe);
        } catch (RuntimeException re) {
            throw new InternalCompilerException("Compiling \"" + fd + "\": " + re.getMessage(), re);
        }
    }

    private void
    compile2(FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        ClassFile.MethodInfo mi;

        if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isDefault()) {
            this.compileError("Default interface methods not implemented", fd.getLocation());
        }

        if (fd.getAccess() == Access.PRIVATE) {
            if (fd instanceof MethodDeclarator && !((MethodDeclarator) fd).isStatic()) {

                // To make the non-static private method invocable for enclosing types, enclosed types and types
                // enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                //  + The name is appended with "$"
                //  + It is made static
                //  + A parameter of type "declaring class" is prepended to the signature
                short accessFlags = UnitCompiler.changeAccessibility(this.accessFlags(fd.getModifiers()), Mod.PACKAGE);
                accessFlags |= Mod.STATIC;

                mi = classFile.addMethodInfo(
                    accessFlags,   // accessFlags
                    fd.name + '$', // methodName
                    (              // methodMd
                        this.toIMethod((MethodDeclarator) fd)
                        .getDescriptor()
                        .prependParameter(this.resolve(fd.getDeclaringType()).getDescriptor())
                    )
                );
            } else
            {

                // TODO: Compile annotations on functions.
//                assert fd.modifiers.annotations.length == 0 : "NYI";

                // To make the static private method or private constructor invocable for enclosing types, enclosed
                // types and types enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE

                short accessFlags = this.accessFlags(fd.getModifiers());
                accessFlags = UnitCompiler.changeAccessibility(accessFlags, Mod.PACKAGE);
                if (fd.formalParameters.variableArity) accessFlags |= Mod.VARARGS;

                mi = classFile.addMethodInfo(
                    accessFlags,                          // accessFlags
                    fd.name,                              // methodName
                    this.toIInvocable(fd).getDescriptor() // methodMD
                );
            }
        } else {

            // Non-PRIVATE function.

            short accessFlags = this.accessFlags(fd.getModifiers());

            if (fd.formalParameters.variableArity) accessFlags |= Mod.VARARGS;

            if (fd.getDeclaringType() instanceof InterfaceDeclaration) {

                // Static interface methods would require Java 8 class file format, but JANINO is still tied to Java
                // 7 class file format.
                if (Mod.isStatic(accessFlags) && !"<clinit>".equals(fd.name)) {
                    this.compileError("Static interface methods not implemented", fd.getLocation());
                }

                accessFlags |= Mod.ABSTRACT | Mod.PUBLIC;
            }

            mi = classFile.addMethodInfo(
                accessFlags,                          // accessFlags
                fd.name,                              // methodName
                this.toIInvocable(fd).getDescriptor() // methodMD
            );
        }

        // Add method annotations with retention != SOURCE.
        this.compileAnnotations(fd.getAnnotations(), mi, classFile);

        // Add "Exceptions" attribute (JVMS 4.7.4).
        {
            if (fd.thrownExceptions.length > 0) {
                final short eani    = classFile.addConstantUtf8Info("Exceptions");
                short[]     tecciis = new short[fd.thrownExceptions.length];
                for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                    tecciis[i] = classFile.addConstantClassInfo(this.getType(fd.thrownExceptions[i]).getDescriptor());
                }
                mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, tecciis));
            }
        }

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (fd.hasDeprecatedDocTag()) {
            mi.addAttribute(new ClassFile.DeprecatedAttribute(classFile.addConstantUtf8Info("Deprecated")));
        }

        // Add "AnnotationDefault" attribute (JVMS8 4.7.22)
        if (fd instanceof MethodDeclarator) {
            ElementValue defaultValue = ((MethodDeclarator) fd).defaultValue;
            if (defaultValue != null) {
                mi.addAttribute(
                    new ClassFile.AnnotationDefaultAttribute(
                        classFile.addConstantUtf8Info("AnnotationDefault"),
                        UnitCompiler.this.compileElementValue(defaultValue, classFile)
                    )
                );
            }
        }

        if (fd.getDeclaringType() instanceof InterfaceDeclaration) {
            MethodDeclarator md = (MethodDeclarator) fd;
            if (md.getAccess() == Access.PRIVATE) {
                this.compileError("Private interface methods not implemented", fd.getLocation());
                return;
            }
            if (md.isStrictfp() && !md.isDefault() && !md.isStatic()) {
                this.compileError(
                    "Modifier strictfp only allowed for interface default methods and static interface methods",
                    fd.getLocation()
                );
                return;
            }
        }

        if (
            (
                fd.getDeclaringType() instanceof InterfaceDeclaration
                && !((MethodDeclarator) fd).isStatic()
            )
            || (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isAbstract())
            || (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isNative())
        ) {
            if (((MethodDeclarator) fd).isDefault()) {
                if (!(fd.getDeclaringType() instanceof InterfaceDeclaration)) {
                    this.compileError("Only interface method declarations may have the \"default\" modifier", fd.getLocation()); // SUPPRESS CHECKSTYLE LineLength
                } else
                if (((MethodDeclarator) fd).isStatic()) {
                    this.compileError("Static interface method declarations must not have the \"default\" modifier", fd.getLocation()); // SUPPRESS CHECKSTYLE LineLength
                } else
                if (fd.statements == null) {
                    this.compileError("Default method declarations must have a body", fd.getLocation());
                }
            } else {
                if (fd.statements != null) this.compileError("Method must not declare a body", fd.getLocation()); // SUPPRESS CHECKSTYLE LineLength
                return;
            }
        }

        // Create CodeContext.
        final CodeContext codeContext = new CodeContext(mi.getClassFile(), new IClass[0]);

        CodeContext savedCodeContext = this.replaceCodeContext(codeContext);
        try {
            this.getCodeContext().saveLocalVariables();

            if (fd instanceof MethodDeclarator) {
                MethodDeclarator md = (MethodDeclarator) fd;
                if (!md.isStatic()) {

                    // Define special parameter "this".
                    LocalVariableSlot thisLvSlot = this.getCodeContext().allocateLocalVariable((short) 1, "this", this.resolve(fd.getDeclaringType()));
                    this.updateLocalVariableInCurrentStackMap(thisLvSlot.getSlotIndex(), this.verificationTypeInfo(thisLvSlot.getType()));
                }
            }


            if (fd instanceof ConstructorDeclarator) {

                // Define special parameter "this".
                LocalVariableSlot thisLvSlot = this.getCodeContext().allocateLocalVariable((short) 1, "this", this.resolve(fd.getDeclaringType()));
                this.updateLocalVariableInCurrentStackMap(thisLvSlot.getSlotIndex(), StackMapTableAttribute.UNINITIALIZED_THIS_VARIABLE_INFO);

                ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) fd;

                if (fd.getDeclaringType() instanceof EnumDeclaration) {

                    // Define special constructor parameters "String $name" and "int $ordinal" for enums.
                    LocalVariable lv1 = new LocalVariable(true, this.iClassLoader.TYPE_java_lang_String);
                    lv1.setSlot(this.getCodeContext().allocateLocalVariable((short) 1, null, null));
                    constructorDeclarator.syntheticParameters.put("$name", lv1);

                    LocalVariable lv2 = new LocalVariable(true, IClass.INT);
                    lv2.setSlot(this.getCodeContext().allocateLocalVariable((short) 1, null, null));
                    constructorDeclarator.syntheticParameters.put("$ordinal", lv2);
                }

                // Define synthetic parameters for inner classes ("this$...", "val$...").
                for (IField sf : constructorDeclarator.getDeclaringClass().syntheticFields.values()) {
                    LocalVariable lv = new LocalVariable(true, sf.getType());

                    lv.setSlot(
                        this.getCodeContext().allocateLocalVariable(Descriptor.size(sf.getDescriptor()), null, null)
                    );
                    constructorDeclarator.syntheticParameters.put(sf.getName(), lv);
                }
            }

            this.buildLocalVariableMap(fd);

            // Compile the constructor preamble.
            if (fd instanceof ConstructorDeclarator) {
                ConstructorDeclarator cd = (ConstructorDeclarator) fd;
                if (cd.constructorInvocation != null) {
                    if (cd.constructorInvocation instanceof SuperConstructorInvocation) {
                        this.assignSyntheticParametersToSyntheticFields(cd);
                    }
                    this.compile(cd.constructorInvocation);
                    if (cd.constructorInvocation instanceof SuperConstructorInvocation) {
                        this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                    }

                    // Object initialization is complete; change the verification type info of "this" from
                    // "uninitializedThis" to "object".
                    this.updateLocalVariableInCurrentStackMap((short) 0, this.verificationTypeInfo(this.resolve(fd.getDeclaringType())));
                } else {

                    // Determine qualification for superconstructor invocation.
                    IClass superclass = this.resolve(cd.getDeclaringClass()).getSuperclass();
                    if (superclass == null) {
                        throw new CompileException("\"" + cd + "\" has no superclass", cd.getLocation());
                    }

                    IClass                 outerClassOfSuperclass = superclass.getOuterIClass();
                    QualifiedThisReference qualification          = null;
                    if (outerClassOfSuperclass != null) {
                        qualification = new QualifiedThisReference(
                            cd.getLocation(),                                        // location
                            new SimpleType(cd.getLocation(), outerClassOfSuperclass) // qualification
                        );
                    }

                    // Initialize "this.this$0" and friends.
                    this.assignSyntheticParametersToSyntheticFields(cd);

                    // Invoke the superconstructor.
                    Rvalue[] arguments;
                    if (fd.getDeclaringType() instanceof EnumDeclaration) {

                        LocalVariableAccess nameAccess = new LocalVariableAccess(
                            cd.getLocation(),
                            (LocalVariable) cd.syntheticParameters.get("$name")
                        );
                        assert nameAccess != null;

                        LocalVariableAccess ordinalAccess = new LocalVariableAccess(
                            cd.getLocation(),
                            (LocalVariable) cd.syntheticParameters.get("$ordinal")
                        );
                        assert ordinalAccess != null;

                        arguments = new Rvalue[] { nameAccess, ordinalAccess };
                    } else {
                        arguments = new Rvalue[0];
                    }
                    SuperConstructorInvocation sci = new SuperConstructorInvocation(
                        cd.getLocation(),  // location
                        qualification,     // qualification
                        arguments          // arguments
                    );
                    sci.setEnclosingScope(fd);
                    this.compile(sci);

                    // Object initialization is complete; change the verification type info of "this" from
                    // "uninitializedThis" to "object".
                    this.updateLocalVariableInCurrentStackMap((short) 0, this.verificationTypeInfo(this.resolve(fd.getDeclaringType())));

                    // Initialize "this.x = y".
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                }
            }

            // Compile the function body.
            List<? extends BlockStatement> oss = fd.statements;
            if (oss == null) {
                this.compileError("Method must have a body", fd.getLocation());
                return;
            }
            if (this.compileStatements(oss)) {
                if (this.getReturnType(fd) != IClass.VOID) {
                    this.compileError("Method must return a value", fd.getLocation());
                }
                this.returN(fd);
            }
        } finally {
            this.getCodeContext().restoreLocalVariables();
            this.replaceCodeContext(savedCodeContext);
        }

        // Don't continue code attribute generation if we had compile errors.
        if (this.compileErrorCount > 0) return;

        // Fix up and reallocate as needed.
        codeContext.fixUpAndRelocate();

//        // Do flow analysis.
//        try {
//            codeContext.flowAnalysis(fd.toString());
//        } catch (RuntimeException re) {
//            UnitCompiler.LOGGER.log(Level.FINE, "*** FLOW ANALYSIS", re);
//
//            if (UnitCompiler.keepClassFilesWithFlowAnalysisErrors) {
//
//                // Continue, so that the .class file is generated and can be examined.
//                ;
//            } else {
//                throw new InternalCompilerException("Compiling \"" + fd + "\"; " + re.getMessage(), re);
//            }
//        }

        final short lntani;
        if (this.debugLines) {
            lntani = classFile.addConstantUtf8Info("LineNumberTable");
        } else {
            lntani = 0;
        }

        final short lvtani;
        if (this.debugVars) {
            UnitCompiler.makeLocalVariableNames(codeContext, mi);
            lvtani = classFile.addConstantUtf8Info("LocalVariableTable");
        } else {
            lvtani = 0;
        }

        final short smtani = classFile.addConstantUtf8Info("StackMapTable");

        // Add the code context as a code attribute to the MethodInfo.
        mi.addAttribute(new ClassFile.AttributeInfo(classFile.addConstantUtf8Info("Code")) {

            @Override protected void
            storeBody(DataOutputStream dos) throws IOException {
                codeContext.storeCodeAttributeBody(
                    dos,
                    lntani, // lineNumberTableAttributeNameIndex    - optional
                    lvtani, // localVariableTableAttributeNameIndex - optional
                    smtani  // stackMapTableAttributeNameIndex      - mandatory
                );
            }
        });
    }

    /**
     * Makes the variable name and class name Constant Pool names used by local variables.
     */
    private static void
    makeLocalVariableNames(final CodeContext cc, final ClassFile.MethodInfo mi) {
        ClassFile cf = mi.getClassFile();

        cf.addConstantUtf8Info("LocalVariableTable");
        for (LocalVariableSlot slot : cc.getAllLocalVars()) {

            String localVariableName = slot.getName();
            if (localVariableName != null) {
                String typeName = slot.getType().getDescriptor();

                cf.addConstantUtf8Info(typeName);
                cf.addConstantUtf8Info(localVariableName);
            }
        }
    }

    private void
    buildLocalVariableMap(FunctionDeclarator fd) throws CompileException {
        Map<String, LocalVariable> localVars = new HashMap<String, LocalVariable>();

        // Add function parameters.
        for (int i = 0; i < fd.formalParameters.parameters.length; ++i) {
            FormalParameter fp              = fd.formalParameters.parameters[i];
            IClass          parameterIClass = this.getType(fp.type);
            LocalVariable   lv              = this.getLocalVariable(
                fp,
                i == fd.formalParameters.parameters.length - 1 && fd.formalParameters.variableArity
            );
            lv.setSlot(this.getCodeContext().allocateLocalVariable(
                Descriptor.size(lv.type.getDescriptor()),
                fp.name,
                parameterIClass
            ));

            if (localVars.put(fp.name, lv) != null) {
                this.compileError("Redefinition of parameter \"" + fp.name + "\"", fd.getLocation());
            }
        }

        fd.localVariables = localVars;
        if (fd instanceof ConstructorDeclarator) {
            ConstructorDeclarator cd = (ConstructorDeclarator) fd;
            if (cd.constructorInvocation != null) {
                UnitCompiler.buildLocalVariableMap(cd.constructorInvocation, localVars);
            }
        }
        if (fd.statements != null) {
            for (BlockStatement bs : fd.statements) localVars = this.buildLocalVariableMap(bs, localVars);
        }
    }

    /**
     * Computes and fills in the 'local variable map' for the given <var>blockStatement</var>.
     */
    private Map<String, LocalVariable>
    buildLocalVariableMap(BlockStatement blockStatement, final Map<String, LocalVariable> localVars)
    throws CompileException {

        Map<String, LocalVariable> result = (Map<String, LocalVariable>) blockStatement.accept(
            new BlockStatementVisitor<Map<String, LocalVariable>, CompileException>() {

                // Basic statements that use the default handlers.
                // SUPPRESS CHECKSTYLE LineLengthCheck:11
                @Override public Map<String, LocalVariable> visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)  { UnitCompiler.buildLocalVariableMap(aci, localVars);  return localVars; }
                @Override public Map<String, LocalVariable> visitBreakStatement(BreakStatement bs)                                   { UnitCompiler.buildLocalVariableMap(bs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitContinueStatement(ContinueStatement cs)                             { UnitCompiler.buildLocalVariableMap(cs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitAssertStatement(AssertStatement as)                                 { UnitCompiler.buildLocalVariableMap(as, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitEmptyStatement(EmptyStatement es)                                   { UnitCompiler.buildLocalVariableMap(es, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitExpressionStatement(ExpressionStatement es)                         { UnitCompiler.buildLocalVariableMap(es, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitFieldDeclaration(FieldDeclaration fd)                               { UnitCompiler.buildLocalVariableMap(fd, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitReturnStatement(ReturnStatement rs)                                 { UnitCompiler.buildLocalVariableMap(rs, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitSuperConstructorInvocation(SuperConstructorInvocation sci)          { UnitCompiler.buildLocalVariableMap(sci, localVars);  return localVars; }
                @Override public Map<String, LocalVariable> visitThrowStatement(ThrowStatement ts)                                   { UnitCompiler.buildLocalVariableMap(ts, localVars);   return localVars; }
                @Override public Map<String, LocalVariable> visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) { UnitCompiler.buildLocalVariableMap(lcds, localVars); return localVars; }

                // More complicated statements with specialized handlers, but don't add new variables in this scope.
                // SUPPRESS CHECKSTYLE LineLengthCheck:10
                @Override public Map<String, LocalVariable> visitBlock(Block b)                                  throws CompileException { UnitCompiler.this.buildLocalVariableMap(b,   localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitDoStatement(DoStatement ds)                     throws CompileException { UnitCompiler.this.buildLocalVariableMap(ds,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitForStatement(ForStatement fs)                   throws CompileException { UnitCompiler.this.buildLocalVariableMap(fs,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitForEachStatement(ForEachStatement fes)          throws CompileException { UnitCompiler.this.buildLocalVariableMap(fes, localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitIfStatement(IfStatement is)                     throws CompileException { UnitCompiler.this.buildLocalVariableMap(is,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitInitializer(Initializer i)                      throws CompileException { UnitCompiler.this.buildLocalVariableMap(i,   localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitSwitchStatement(SwitchStatement ss)             throws CompileException { UnitCompiler.this.buildLocalVariableMap(ss,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitSynchronizedStatement(SynchronizedStatement ss) throws CompileException { UnitCompiler.this.buildLocalVariableMap(ss,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitTryStatement(TryStatement ts)                   throws CompileException { UnitCompiler.this.buildLocalVariableMap(ts,  localVars); return localVars; }
                @Override public Map<String, LocalVariable> visitWhileStatement(WhileStatement ws)               throws CompileException { UnitCompiler.this.buildLocalVariableMap(ws,  localVars); return localVars; }

                // More complicated statements with specialized handlers, that can add variables in this scope.
                // SUPPRESS CHECKSTYLE LineLengthCheck:2
                @Override public Map<String, LocalVariable> visitLabeledStatement(LabeledStatement ls)                                     throws CompileException { return UnitCompiler.this.buildLocalVariableMap(ls,   localVars); }
                @Override public Map<String, LocalVariable> visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) throws CompileException { return UnitCompiler.this.buildLocalVariableMap(lvds, localVars); }
            }
        );

        assert result != null;
        return result;
    }

    // Default handlers.

    private static Map<String, LocalVariable>
    buildLocalVariableMap(Statement s, final Map<String, LocalVariable> localVars) {
        return (s.localVariables = localVars);
    }

    private static Map<String, LocalVariable>
    buildLocalVariableMap(ConstructorInvocation ci, final Map<String, LocalVariable> localVars) {
        return (ci.localVariables = localVars);
    }

    // Specialized handlers.
    private void
    buildLocalVariableMap(Block block, Map<String, LocalVariable> localVars) throws CompileException {
        block.localVariables = localVars;
        for (BlockStatement bs : block.statements) localVars = this.buildLocalVariableMap(bs, localVars);
    }

    private void
    buildLocalVariableMap(DoStatement ds, final Map<String, LocalVariable> localVars) throws CompileException {
        ds.localVariables = localVars;
        this.buildLocalVariableMap(ds.body, localVars);
    }

    private void
    buildLocalVariableMap(ForStatement fs, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> inner = localVars;
        if (fs.init != null) {
            inner = this.buildLocalVariableMap(fs.init, localVars);
        }
        fs.localVariables = inner;
        this.buildLocalVariableMap(fs.body, inner);
    }

    private void
    buildLocalVariableMap(ForEachStatement fes, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> vars = new HashMap<String, LocalVariable>();
        vars.putAll(localVars);
        LocalVariable elementLv = this.getLocalVariable(fes.currentElement, false);
        vars.put(fes.currentElement.name, elementLv);
        fes.localVariables = vars;
        this.buildLocalVariableMap(fes.body, vars);
    }

    private void
    buildLocalVariableMap(IfStatement is, final Map<String, LocalVariable> localVars) throws CompileException {
        is.localVariables = localVars;
        this.buildLocalVariableMap(is.thenStatement, localVars);
        if (is.elseStatement != null) {
            this.buildLocalVariableMap(is.elseStatement, localVars);
        }
    }

    private void
    buildLocalVariableMap(Initializer i, final Map<String, LocalVariable> localVars) throws CompileException {
        this.buildLocalVariableMap(i.block, localVars);
    }

    private void
    buildLocalVariableMap(SwitchStatement ss, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ss.localVariables = localVars;
        Map<String, LocalVariable> vars = localVars;
        for (SwitchBlockStatementGroup sbsg : ss.sbsgs) {
            for (BlockStatement bs : sbsg.blockStatements) vars = this.buildLocalVariableMap(bs, vars);
        }
    }

    private void
    buildLocalVariableMap(SynchronizedStatement ss, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ss.localVariables = localVars;
        this.buildLocalVariableMap(ss.body, localVars);
    }

    private void
    buildLocalVariableMap(TryStatement ts, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ts.localVariables = localVars;
        this.buildLocalVariableMap(ts.body, localVars);
        for (CatchClause cc : ts.catchClauses) this.buildLocalVariableMap(cc, localVars);
        if (ts.finallY != null) {
            this.buildLocalVariableMap(ts.finallY, localVars);
        }
    }

    private void
    buildLocalVariableMap(WhileStatement ws, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ws.localVariables = localVars;
        this.buildLocalVariableMap(ws.body, localVars);
    }

    private Map<String, LocalVariable>
    buildLocalVariableMap(LabeledStatement ls, final Map<String, LocalVariable> localVars)
    throws CompileException {
        ls.localVariables = localVars;
        return this.buildLocalVariableMap((BlockStatement) ls.body, localVars);
    }

    private Map<String, LocalVariable>
    buildLocalVariableMap(LocalVariableDeclarationStatement lvds, final Map<String, LocalVariable> localVars)
    throws CompileException {
        Map<String, LocalVariable> newVars = new HashMap<String, LocalVariable>();
        newVars.putAll(localVars);
        for (VariableDeclarator vd : lvds.variableDeclarators) {
            LocalVariable      lv = this.getLocalVariable(lvds, vd);
            if (newVars.put(vd.name, lv) != null) {
                this.compileError("Redefinition of local variable \"" + vd.name + "\" ", vd.getLocation());
            }
        }
        lvds.localVariables = newVars;
        return newVars;
    }

    /**
     * Adds the given <var>localVars</var> to the 'local variable map' of the given <var>catchClause</var>.
     */
    protected void
    buildLocalVariableMap(CatchClause catchClause, Map<String, LocalVariable> localVars) throws CompileException {
        Map<String, LocalVariable> vars = new HashMap<String, LocalVariable>();
        vars.putAll(localVars);
        LocalVariable lv = this.getLocalVariable(catchClause.catchParameter);
        vars.put(catchClause.catchParameter.name, lv);
        this.buildLocalVariableMap(catchClause.body, vars);
    }

    /**
     * @return The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(FormalParameter parameter) throws CompileException {
        return this.getLocalVariable(parameter, false);
    }

    /**
     * @param isVariableArityParameter Whether the <var>parameter</var> is the last parameter of a 'variable arity'
     *                                 (a.k.a. 'varargs') method declaration
     * @return                         The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(FormalParameter parameter, boolean isVariableArityParameter) throws CompileException {

        if (parameter.localVariable != null) return parameter.localVariable;

        assert parameter.type != null;
        IClass parameterType = this.getType(parameter.type);
        if (isVariableArityParameter) {
            parameterType = parameterType.getArrayIClass(this.iClassLoader.TYPE_java_lang_Object);
        }

        return (parameter.localVariable = new LocalVariable(parameter.isFinal(), parameterType));
    }

    /**
     * @return The {@link LocalVariable} corresponding with the <var>parameter</var>
     */
    public LocalVariable
    getLocalVariable(CatchParameter parameter) throws CompileException {

        if (parameter.localVariable != null) return parameter.localVariable;

        if (parameter.types.length != 1) {
            throw UnitCompiler.compileException(parameter, "Multi-type CATCH parameters NYI");
        }
        IClass parameterType = this.getType(parameter.types[0]);

        return (parameter.localVariable = new LocalVariable(parameter.finaL, parameterType));
    }

    // ------------------ Rvalue.compile() ----------------

    /**
     * Called to check whether the given {@link Rvalue} compiles or not.
     */
    private void
    fakeCompile(Rvalue rv) throws CompileException {

        final Offset from = this.getCodeContext().newOffset();
        StackMap savedStackMap = this.getCodeContext().currentInserter().getStackMap();

        this.compileContext(rv);
        this.compileGet(rv);

        Offset to = this.getCodeContext().newOffset();

        this.getCodeContext().removeCode(from, to.next);
        this.getCodeContext().currentInserter().setStackMap(savedStackMap);
    }

    /**
     * Some {@link Rvalue}s compile more efficiently when their value is not needed, e.g. "i++".
     */
    private void
    compile(Rvalue rv) throws CompileException {

        rv.accept(new RvalueVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitLvalue(Lvalue lv) throws CompileException {
                lv.accept(new Visitor.LvalueVisitor<Void, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compile2(an);    return null; }
                    @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compile2(aae);   return null; }
                    @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compile2(fa);    return null; }
                    @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compile2(fae);   return null; }
                    @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compile2(scfae); return null; }
                    @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { UnitCompiler.this.compile2(lva);   return null; }
                    @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compile2(pe);    return null; }
                });
                return null;
            }

            // SUPPRESS CHECKSTYLE LineLength:29
            @Override @Nullable public Void visitArrayLength(ArrayLength al)                                    throws CompileException { UnitCompiler.this.compile2(al);    return null; }
            @Override @Nullable public Void visitAssignment(Assignment a)                                       throws CompileException { UnitCompiler.this.compile2(a);     return null; }
            @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                              throws CompileException { UnitCompiler.this.compile2(uo);    return null; }
            @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                            throws CompileException { UnitCompiler.this.compile2(bo);    return null; }
            @Override @Nullable public Void visitCast(Cast c)                                                   throws CompileException { UnitCompiler.this.compile2(c);     return null; }
            @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                                  throws CompileException { UnitCompiler.this.compile2(cl);    return null; }
            @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)                throws CompileException { UnitCompiler.this.compile2(ce);    return null; }
            @Override @Nullable public Void visitCrement(Crement c)                                             throws CompileException { UnitCompiler.this.compile2(c);     return null; }
            @Override @Nullable public Void visitInstanceof(Instanceof io)                                      throws CompileException { UnitCompiler.this.compile2(io);    return null; }
            @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                          throws CompileException { UnitCompiler.this.compile2(mi);    return null; }
            @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { UnitCompiler.this.compile2(smi);   return null; }
            @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { UnitCompiler.this.compile2(il);    return null; }
            @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { UnitCompiler.this.compile2(fpl);   return null; }
            @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { UnitCompiler.this.compile2(bl);    return null; }
            @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { UnitCompiler.this.compile2(cl);    return null; }
            @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                                throws CompileException { UnitCompiler.this.compile2(sl);    return null; }
            @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                    throws CompileException { UnitCompiler.this.compile2(nl);    return null; }
            @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                              throws CompileException { UnitCompiler.this.compile2(sl);    return null; }
            @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { UnitCompiler.this.compile2(naci);  return null; }
            @Override @Nullable public Void visitNewArray(NewArray na)                                          throws CompileException { UnitCompiler.this.compile2(na);    return null; }
            @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { UnitCompiler.this.compile2(nia);   return null; }
            @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                         throws CompileException { UnitCompiler.this.compile2(nci);   return null; }
            @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                            throws CompileException { UnitCompiler.this.compile2(pa);    return null; }
            @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { UnitCompiler.this.compile2(qtr);   return null; }
            @Override @Nullable public Void visitThisReference(ThisReference tr)                                throws CompileException { UnitCompiler.this.compile2(tr);    return null; }
            @Override @Nullable public Void visitLambdaExpression(LambdaExpression le)                          throws CompileException { UnitCompiler.this.compile2(le);    return null; }
            @Override @Nullable public Void visitMethodReference(MethodReference mr)                            throws CompileException { UnitCompiler.this.compile2(mr);    return null; }
            @Override @Nullable public Void visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { UnitCompiler.this.compile2(cicr);  return null; }
            @Override @Nullable public Void visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { UnitCompiler.this.compile2(acr);   return null; }
        });
    }

    private void
    compile2(Rvalue rv) throws CompileException {
        this.pop(rv, this.compileGetValue(rv));
    }

    private void
    compile2(Assignment a) throws CompileException {

        // "Simple" assignment ("=")?
        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileContext(a.lhs);
            this.assignmentConversion(
                a,                           // locatable
                this.compileGetValue(a.rhs), // sourceType
                this.getType(a.lhs),         // targetType
                this.getConstantValue(a.rhs) // constantValue
            );
            this.compileSet(a.lhs);
            return;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".

        // Compile LHS context.
        int lhsCs = this.compileContext(a.lhs);
        // Duplicate the LHS context.
        this.dupn(a.lhs, lhsCs);
        // Convert RHS value to LHS type (JLS7 15.26.2).
        IClass lhsType    = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), /* <= IMPORTANT!
            */
            a.rhs                 // rhs
        );
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
            && !this.tryBoxingConversion(a, resultType, lhsType) // Java 5
        ) this.compileError("Operand types unsuitable for \"" + a.operator + "\"", a.getLocation());
        // Assign converted RHS value to LHS.
        this.compileSet(a.lhs);
    }

    private void
    compile2(Crement c) throws CompileException {

        // Optimized crement of "int" local variable.
        {
            LocalVariable lv = this.isIntLv(c);
            if (lv != null) {
                this.iinc(c, lv, c.operator);
                return;
            }
        }

        // Compile operand context.
        int operandCs = this.compileContext(c.operand);
        // DUP operand context.
        this.dupn(c, operandCs);
        // Get operand value.
        IClass type = this.compileGet(c.operand);

        {

            // Apply "unary numeric promotion".
            IClass promotedType = this.unaryNumericPromotion(c, type);
            // Crement.
            this.consT(c, promotedType, 1);
            if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.add(c);
            } else
            if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.sub(c);
            } else {
                this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
            }
            this.reverseUnaryNumericPromotion(c, promotedType, type);
        }

        // Set operand.
        this.compileSet(c.operand);
    }

    private void
    compile2(ParenthesizedExpression pe) throws CompileException {
        this.compile(pe.value);
    }

    private boolean
    compile2(AlternateConstructorInvocation aci) throws CompileException {
        ConstructorDeclarator declaringConstructor = (ConstructorDeclarator) aci.getEnclosingScope();
        IClass                declaringIClass      = this.resolve(declaringConstructor.getDeclaringClass());

        this.load(aci, declaringIClass, 0);
        if (declaringIClass.getOuterIClass() != null) this.load(aci, declaringIClass.getOuterIClass(), 1);
        this.invokeConstructor(
            aci,                  // locatable
            declaringConstructor, // scope
            (Rvalue) null,        // enclosingInstance
            declaringIClass,      // targetClass
            aci.arguments         // arguments
        );
        return true;
    }

    private boolean
    compile2(SuperConstructorInvocation sci) throws CompileException {
        ConstructorDeclarator    declaringConstructor = (ConstructorDeclarator) sci.getEnclosingScope();
        AbstractClassDeclaration declaringClass       = declaringConstructor.getDeclaringClass();
        IClass                   declaringIClass      = this.resolve(declaringClass);
        IClass                   superclass           = declaringIClass.getSuperclass();

        this.load(sci, declaringIClass, 0);

        // Fix up the operand stack entry: The loaded object is still unitializied!
        this.getCodeContext().popObjectOperand();
        this.getCodeContext().pushUninitializedThisOperand();

        if (superclass == null) throw new CompileException("Class has no superclass", sci.getLocation());

        Rvalue enclosingInstance;
        if (sci.qualification != null) {
            enclosingInstance = sci.qualification;
        } else {
            IClass outerIClassOfSuperclass = superclass.getOuterIClass();
            if (outerIClassOfSuperclass == null) {
                enclosingInstance = null;
            } else {
                enclosingInstance = new QualifiedThisReference(
                    sci.getLocation(),                                         // location
                    new SimpleType(sci.getLocation(), outerIClassOfSuperclass) // qualification
                );
                enclosingInstance.setEnclosingScope(sci);
            }
        }
        this.invokeConstructor(
            sci,                       // locatable
            declaringConstructor,      // scope
            enclosingInstance, // enclosingInstance
            superclass,                // targetClass
            sci.arguments              // arguments
        );
        return true;
    }

    /**
     * Compiles an {@link Rvalue} and branches, depending on the value.
     * <p>
     *   Many {@link Rvalue}s compile more efficiently when their value is the condition for a branch, thus
     *   this method generally produces more efficient bytecode than {@link #compile(Rvalue)} followed by {@link
     *   #branch(Locatable, int, Offset)}.
     * </p>
     * <p>
     *   Notice that if <var>rv</var> is a constant, then either <var>dst</var> is never branched to, or it is
     *   unconditionally branched to; "Unexamined code" errors may result during bytecode validation.
     * </p>
     *
     * @param rv          The value that determines whether to branch or not
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean(Rvalue rv, final CodeContext.Offset dst, final boolean orientation) throws CompileException {

        rv.accept(new RvalueVisitor<Void, CompileException>() {

            @Override @Nullable public Void
            visitLvalue(Lvalue lv) throws CompileException {
                lv.accept(new Visitor.LvalueVisitor<Void, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compileBoolean2(an,    dst, orientation); return null; }
                    @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compileBoolean2(aae,   dst, orientation); return null; }
                    @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compileBoolean2(fa,    dst, orientation); return null; }
                    @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compileBoolean2(fae,   dst, orientation); return null; }
                    @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compileBoolean2(scfae, dst, orientation); return null; }
                    @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                           throws CompileException { UnitCompiler.this.compileBoolean2(lva,   dst, orientation); return null; }
                    @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compileBoolean2(pe,    dst, orientation); return null; }
                });
                return null;
            }

            // SUPPRESS CHECKSTYLE LineLength:29
            @Override @Nullable public Void visitArrayLength(ArrayLength al)                                    throws CompileException { UnitCompiler.this.compileBoolean2(al,   dst, orientation); return null; }
            @Override @Nullable public Void visitAssignment(Assignment a)                                       throws CompileException { UnitCompiler.this.compileBoolean2(a,    dst, orientation); return null; }
            @Override @Nullable public Void visitUnaryOperation(UnaryOperation uo)                              throws CompileException { UnitCompiler.this.compileBoolean2(uo,   dst, orientation); return null; }
            @Override @Nullable public Void visitBinaryOperation(BinaryOperation bo)                            throws CompileException { UnitCompiler.this.compileBoolean2(bo,   dst, orientation); return null; }
            @Override @Nullable public Void visitCast(Cast c)                                                   throws CompileException { UnitCompiler.this.compileBoolean2(c,    dst, orientation); return null; }
            @Override @Nullable public Void visitClassLiteral(ClassLiteral cl)                                  throws CompileException { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); return null; }
            @Override @Nullable public Void visitConditionalExpression(ConditionalExpression ce)                throws CompileException { UnitCompiler.this.compileBoolean2(ce,   dst, orientation); return null; }
            @Override @Nullable public Void visitCrement(Crement c)                                             throws CompileException { UnitCompiler.this.compileBoolean2(c,    dst, orientation); return null; }
            @Override @Nullable public Void visitInstanceof(Instanceof io)                                      throws CompileException { UnitCompiler.this.compileBoolean2(io,   dst, orientation); return null; }
            @Override @Nullable public Void visitMethodInvocation(MethodInvocation mi)                          throws CompileException { UnitCompiler.this.compileBoolean2(mi,   dst, orientation); return null; }
            @Override @Nullable public Void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { UnitCompiler.this.compileBoolean2(smi,  dst, orientation); return null; }
            @Override @Nullable public Void visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { UnitCompiler.this.compileBoolean2(il,   dst, orientation); return null; }
            @Override @Nullable public Void visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { UnitCompiler.this.compileBoolean2(fpl,  dst, orientation); return null; }
            @Override @Nullable public Void visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { UnitCompiler.this.compileBoolean2(bl,   dst, orientation); return null; }
            @Override @Nullable public Void visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); return null; }
            @Override @Nullable public Void visitStringLiteral(StringLiteral sl)                                throws CompileException { UnitCompiler.this.compileBoolean2(sl,   dst, orientation); return null; }
            @Override @Nullable public Void visitNullLiteral(NullLiteral nl)                                    throws CompileException { UnitCompiler.this.compileBoolean2(nl,   dst, orientation); return null; }
            @Override @Nullable public Void visitSimpleConstant(SimpleConstant sl)                              throws CompileException { UnitCompiler.this.compileBoolean2(sl,   dst, orientation); return null; }
            @Override @Nullable public Void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { UnitCompiler.this.compileBoolean2(naci, dst, orientation); return null; }
            @Override @Nullable public Void visitNewArray(NewArray na)                                          throws CompileException { UnitCompiler.this.compileBoolean2(na,   dst, orientation); return null; }
            @Override @Nullable public Void visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { UnitCompiler.this.compileBoolean2(nia,  dst, orientation); return null; }
            @Override @Nullable public Void visitNewClassInstance(NewClassInstance nci)                         throws CompileException { UnitCompiler.this.compileBoolean2(nci,  dst, orientation); return null; }
            @Override @Nullable public Void visitParameterAccess(ParameterAccess pa)                            throws CompileException { UnitCompiler.this.compileBoolean2(pa,   dst, orientation); return null; }
            @Override @Nullable public Void visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { UnitCompiler.this.compileBoolean2(qtr,  dst, orientation); return null; }
            @Override @Nullable public Void visitThisReference(ThisReference tr)                                throws CompileException { UnitCompiler.this.compileBoolean2(tr,   dst, orientation); return null; }
            @Override @Nullable public Void visitLambdaExpression(LambdaExpression le)                          throws CompileException { UnitCompiler.this.compileBoolean2(le,   dst, orientation); return null; }
            @Override @Nullable public Void visitMethodReference(MethodReference mr)                            throws CompileException { UnitCompiler.this.compileBoolean2(mr,   dst, orientation); return null; }
            @Override @Nullable public Void visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { UnitCompiler.this.compileBoolean2(cicr, dst, orientation); return null; }
            @Override @Nullable public Void visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { UnitCompiler.this.compileBoolean2(acr,  dst, orientation); return null; }
        });
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(Rvalue rv, CodeContext.Offset dst, boolean orientation) throws CompileException {
        IClass       type = this.compileGetValue(rv);
        IClassLoader icl  = this.iClassLoader;
        if (type == icl.TYPE_java_lang_Boolean) {
            this.unboxingConversion(rv, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
        } else
        if (type != IClass.BOOLEAN) {
            this.compileError("Not a boolean expression", rv.getLocation());
        }
        this.ifxx(rv, orientation == UnitCompiler.JUMP_IF_TRUE ? UnitCompiler.NE : UnitCompiler.EQ, dst);
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(UnaryOperation ue, CodeContext.Offset dst, boolean orientation) throws CompileException {
        if (ue.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileBoolean(ue.operand, dst, !orientation);
            return;
        }

        this.compileError("Boolean expression expected", ue.getLocation());
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(BinaryOperation bo, CodeContext.Offset dst, boolean orientation) throws CompileException {

        if (bo.operator == "|" || bo.operator == "^" || bo.operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
            this.compileBoolean2((Rvalue) bo, dst, orientation);
            return;
        }

        if (bo.operator == "||" || bo.operator == "&&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsCv = this.getConstantValue(bo.lhs);
            if (lhsCv instanceof Boolean) {
                if (((Boolean) lhsCv).booleanValue() ^ bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                    // "true && a", "false || a"
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        orientation
                    );
                } else {
                    // "false && a", "true || a"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        orientation
                    );
                    this.fakeCompile(bo.rhs);
                }
                return;
            }
            Object rhsCv = this.getConstantValue(bo.rhs);
            if (rhsCv instanceof Boolean) {
                if (((Boolean) rhsCv).booleanValue() ^ bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                    // "a && true", "a || false"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        orientation
                    );
                } else {
                    // "a && false", "a || true"

                    // Compile the LHS ("a"), and discard the result.
                    this.pop(bo.lhs, this.compileGetValue(bo.lhs));

                    // Compile the RHS and branch conditionally (although the RHS is a constant). This prevents
                    // trouble with "unreachable code".
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        orientation
                    );
                }
                return;
            }

            // SUPPRESS CHECKSTYLE StringLiteralEquality
            if (bo.operator == "||" ^ orientation == UnitCompiler.JUMP_IF_FALSE) {
                this.compileBoolean(bo.lhs, dst, orientation);
                this.compileBoolean(bo.rhs, dst, orientation);
            } else {
                CodeContext.Offset end = this.getCodeContext().new Offset();
                this.compileBoolean(bo.lhs, end, !orientation);
                this.compileBoolean(bo.rhs, dst, orientation);
                end.set();
            }
            return;
        }

        COMPARISON:
        {
            int opIdx = (
                bo.operator == "==" ? UnitCompiler.EQ : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "!=" ? UnitCompiler.NE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "<"  ? UnitCompiler.LT : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == ">=" ? UnitCompiler.GE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == ">"  ? UnitCompiler.GT : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.operator == "<=" ? UnitCompiler.LE : // SUPPRESS CHECKSTYLE StringLiteralEquality
                Integer.MIN_VALUE
            );
            if (opIdx == Integer.MIN_VALUE) break COMPARISON;

            if (orientation == UnitCompiler.JUMP_IF_FALSE) opIdx ^= 1;

            // Comparison with "null".
            {
                boolean lhsIsNull = this.getConstantValue(bo.lhs) == null;
                boolean rhsIsNull = this.getConstantValue(bo.rhs) == null;

                if (lhsIsNull || rhsIsNull) {
                    if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                        this.compileError(
                            "Operator \"" + bo.operator + "\" not allowed on operand \"null\"",
                            bo.getLocation()
                        );
                    }

                    if (!lhsIsNull) {

                        // x == null
                        IClass lhsType = this.compileGetValue(bo.lhs);
                        if (lhsType.isPrimitive()) {
                            this.compileError(
                                "Cannot compare primitive type \"" + lhsType.toString() + "\" with \"null\"",
                                bo.getLocation()
                            );
                        }
                    } else
                    if (!rhsIsNull) {

                        // null == x
                        IClass rhsType = this.compileGetValue(bo.rhs);
                        if (rhsType.isPrimitive()) {
                            this.compileError(
                                "Cannot compare \"null\" with primitive type \"" + rhsType.toString() + "\"",
                                bo.getLocation()
                            );
                        }
                    } else
                    {

                        // null == null
                        this.consT(bo, (Object) null);
                    }

                    switch (opIdx) {

                    case EQ:
                        this.ifnull(bo, dst);
                        break;

                    case NE:
                        this.ifnonnull(bo, dst);
                        break;

                    default:
                        throw new AssertionError(opIdx);
                    }
                    return;
                }
            }

            IClass lhsType = this.compileGetValue(bo.lhs);
            IClass rhsType = this.getType(bo.rhs);

            // 15.20.1 Numerical comparison.
            if (
                this.getUnboxedType(lhsType).isPrimitiveNumeric()
                && this.getUnboxedType(rhsType).isPrimitiveNumeric()
                && !(
                    (bo.operator == "==" || bo.operator == "!=") // SUPPRESS CHECKSTYLE StringLiteralEquality
                    && !lhsType.isPrimitive()
                    && !rhsType.isPrimitive()
                )
            ) {

                IClass
                promotedType = this.binaryNumericPromotionType(bo, this.getUnboxedType(lhsType), this.getUnboxedType(rhsType));

                this.numericPromotion(bo.lhs, this.convertToPrimitiveNumericType(bo.lhs, lhsType), promotedType);
                this.compileGetValue(bo.rhs);
                this.numericPromotion(bo.rhs, this.convertToPrimitiveNumericType(bo.rhs, rhsType), promotedType);

                this.ifNumeric(bo, opIdx, dst);
                return;
            }

            // JLS7 15.21.2 Boolean Equality Operators == and !=.
            if (
                (lhsType == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                || (rhsType == IClass.BOOLEAN && this.getUnboxedType(lhsType) == IClass.BOOLEAN)
            ) {
                if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError(
                        "Operator \"" + bo.operator + "\" not allowed on boolean operands",
                        bo.getLocation()
                    );
                }
                IClassLoader icl = this.iClassLoader;

                // Unbox LHS if necessary.
                if (lhsType == icl.TYPE_java_lang_Boolean) {
                    this.unboxingConversion(bo, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                }

                this.compileGetValue(bo.rhs);

                // Unbox RHS if necessary.
                if (rhsType == icl.TYPE_java_lang_Boolean) {
                    this.unboxingConversion(bo, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                }

                this.if_icmpxx(bo, opIdx, dst);
                return;
            }

            // Reference comparison.
            // Note: Comparison with "null" is already handled above.
            if (!lhsType.isPrimitive() && !rhsType.isPrimitive()) {
                if (bo.operator != "==" && bo.operator != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError("Operator \"" + bo.operator + "\" not allowed on reference operands", bo.getLocation()); // SUPPRESS CHECKSTYLE LineLength
                }
                if (
                    !this.isCastReferenceConvertible(lhsType, rhsType)
                    || !this.isCastReferenceConvertible(rhsType, lhsType)
                ) this.compileError("Incomparable types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());

                this.compileGetValue(bo.rhs);

                this.if_acmpxx(bo, opIdx, dst);
                return;
            }

            this.compileError("Cannot compare types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());
        }

        this.compileError("Boolean expression expected", bo.getLocation());
    }

    /**
     * @param dst         Where to jump
     * @param orientation {@link #JUMP_IF_TRUE} or {@link #JUMP_IF_FALSE}
     */
    private void
    compileBoolean2(ParenthesizedExpression pe, CodeContext.Offset dst, boolean orientation) throws CompileException {
        this.compileBoolean(pe.value, dst, orientation);
    }

    /**
     * Generates code that determines the context of the {@link Rvalue} and puts it on the operand stack. Most
     * expressions do not have a "context", but some do. E.g. for "x[y]", the context is "x, y". The bottom line is
     * that for statements like "x[y] += 3" the context is only evaluated once.
     *
     * @return The number of operands that is pushed on the operand stack (0, 1 or 2)
     */
    private int
    compileContext(Rvalue rv) throws CompileException {

        Integer result = (Integer) rv.accept(new RvalueVisitor<Integer, CompileException>() {

            @Override @Nullable public Integer
            visitLvalue(Lvalue lv) throws CompileException {
                return (Integer) lv.accept(new Visitor.LvalueVisitor<Integer, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override public Integer visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.compileContext2(an);    }
                    @Override public Integer visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.compileContext2(aae);   }
                    @Override public Integer visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.compileContext2(fa);    }
                    @Override public Integer visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.compileContext2(fae);   }
                    @Override public Integer visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.compileContext2(scfae); }
                    @Override public Integer visitLocalVariableAccess(LocalVariableAccess lva)                                                   { return UnitCompiler.this.compileContext2(lva);   }
                    @Override public Integer visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.compileContext2(pe);    }
                });
            }

            // SUPPRESS CHECKSTYLE LineLength:29
            @Override public Integer visitArrayLength(ArrayLength al)            throws CompileException { return UnitCompiler.this.compileContext2(al);   }
            @Override public Integer visitAssignment(Assignment a)                                       { return UnitCompiler.this.compileContext2(a);    }
            @Override public Integer visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.this.compileContext2(uo);   }
            @Override public Integer visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.this.compileContext2(bo);   }
            @Override public Integer visitCast(Cast c)                                                   { return UnitCompiler.this.compileContext2(c);    }
            @Override public Integer visitClassLiteral(ClassLiteral cl)                                  { return UnitCompiler.this.compileContext2(cl);   }
            @Override public Integer visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.this.compileContext2(ce);   }
            @Override public Integer visitCrement(Crement c)                                             { return UnitCompiler.this.compileContext2(c);    }
            @Override public Integer visitInstanceof(Instanceof io)                                      { return UnitCompiler.this.compileContext2(io);   }
            @Override public Integer visitMethodInvocation(MethodInvocation mi)                          { return UnitCompiler.this.compileContext2(mi);   }
            @Override public Integer visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return UnitCompiler.this.compileContext2(smi);  }
            @Override public Integer visitIntegerLiteral(IntegerLiteral il)                              { return UnitCompiler.this.compileContext2(il);   }
            @Override public Integer visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return UnitCompiler.this.compileContext2(fpl);  }
            @Override public Integer visitBooleanLiteral(BooleanLiteral bl)                              { return UnitCompiler.this.compileContext2(bl);   }
            @Override public Integer visitCharacterLiteral(CharacterLiteral cl)                          { return UnitCompiler.this.compileContext2(cl);   }
            @Override public Integer visitStringLiteral(StringLiteral sl)                                { return UnitCompiler.this.compileContext2(sl);   }
            @Override public Integer visitNullLiteral(NullLiteral nl)                                    { return UnitCompiler.this.compileContext2(nl);   }
            @Override public Integer visitSimpleConstant(SimpleConstant sl)                              { return UnitCompiler.this.compileContext2(sl);   }
            @Override public Integer visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return UnitCompiler.this.compileContext2(naci); }
            @Override public Integer visitNewArray(NewArray na)                                          { return UnitCompiler.this.compileContext2(na);   }
            @Override public Integer visitNewInitializedArray(NewInitializedArray nia)                   { return UnitCompiler.this.compileContext2(nia);  }
            @Override public Integer visitNewClassInstance(NewClassInstance nci)                         { return UnitCompiler.this.compileContext2(nci);  }
            @Override public Integer visitParameterAccess(ParameterAccess pa)                            { return UnitCompiler.this.compileContext2(pa);   }
            @Override public Integer visitQualifiedThisReference(QualifiedThisReference qtr)             { return UnitCompiler.this.compileContext2(qtr);  }
            @Override public Integer visitThisReference(ThisReference tr)                                { return UnitCompiler.this.compileContext2(tr);   }
            @Override public Integer visitLambdaExpression(LambdaExpression le)                          { return UnitCompiler.this.compileContext2(le);   }
            @Override public Integer visitMethodReference(MethodReference mr)                            { return UnitCompiler.this.compileContext2(mr);   }
            @Override public Integer visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return UnitCompiler.this.compileContext2(cicr); }
            @Override public Integer visitArrayCreationReference(ArrayCreationReference acr)             { return UnitCompiler.this.compileContext2(acr);  }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private int
    compileContext2(Rvalue rv) { return 0; }

    private int
    compileContext2(AmbiguousName an) throws CompileException {
        return this.compileContext(this.toRvalueOrCompileException(this.reclassify(an)));
    }

    private int
    compileContext2(FieldAccess fa) throws CompileException {
        if (fa.field.isStatic()) {
            Rvalue rv = fa.lhs.toRvalue();
            if (rv != null) {
                this.warning(
                    "CNSFA",
                    "Left-hand side of static field access should be a type, not an rvalue",
                    fa.lhs.getLocation()
                );
                // JLS7 15.11.1.3.1.1:
                this.pop(fa.lhs, this.compileGetValue(rv));
            }
            return 0;
        } else {
            this.compileGetValue(this.toRvalueOrCompileException(fa.lhs));
            return 1;
        }
    }

    private int
    compileContext2(ArrayLength al) throws CompileException {
        if (!this.compileGetValue(al.lhs).isArray()) {
            this.compileError("Cannot determine length of non-array type", al.getLocation());
        }
        return 1;
    }

    /**
     * Array access expression; see JLS7 15.13 / JLS8+ 15.10.3.
     */
    private int
    compileContext2(ArrayAccessExpression aae) throws CompileException {

        IClass lhsType = this.compileGetValue(aae.lhs);
        if (!lhsType.isArray()) {
            this.compileError(
                "Subscript not allowed on non-array type \"" + lhsType.toString() + "\"",
                aae.getLocation()
            );
        }

        IClass indexType = this.compileGetValue(aae.index);
        if (this.unaryNumericPromotion(aae.index, indexType) != IClass.INT) {
            this.compileError(
                "Index expression of type \"" + indexType + "\" cannot be promoted to \"int\"",
                aae.getLocation()
            );
        }

        return 2;
    }

    private int
    compileContext2(FieldAccessExpression fae) throws CompileException {
        return this.compileContext(this.determineValue(fae));
    }

    private int
    compileContext2(SuperclassFieldAccessExpression scfae) throws CompileException {
        return this.compileContext(this.determineValue(scfae));
    }

    private int
    compileContext2(ParenthesizedExpression pe) throws CompileException {
        return this.compileContext(pe.value);
    }

    /**
     * Generates code that determines the value of the {@link Rvalue} and puts it on the operand stack. This method
     * relies on that the "context" of the {@link Rvalue} is on top of the operand stack (see {@link
     * #compileContext(Rvalue)}).
     *
     * @return The type of the {@link Rvalue}
     */
    private IClass
    compileGet(Rvalue rv) throws CompileException {

        IClass result = (IClass) rv.accept(new RvalueVisitor<IClass, CompileException>() {

            @Override @Nullable public IClass
            visitLvalue(Lvalue lv) throws CompileException {
                return (IClass) lv.accept(new Visitor.LvalueVisitor<IClass, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLength:7
                    @Override public IClass visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.compileGet2(an);    }
                    @Override public IClass visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.compileGet2(aae);   }
                    @Override public IClass visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.compileGet2(fa);    }
                    @Override public IClass visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.compileGet2(fae);   }
                    @Override public IClass visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.compileGet2(scfae); }
                    @Override public IClass visitLocalVariableAccess(LocalVariableAccess lva)                                                   { return UnitCompiler.this.compileGet2(lva);   }
                    @Override public IClass visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.compileGet2(pe);    }
                });
            }

            // SUPPRESS CHECKSTYLE LineLength:29
            @Override public IClass visitArrayLength(ArrayLength al)                                                            { return UnitCompiler.this.compileGet2(al);   }
            @Override public IClass visitAssignment(Assignment a)                                       throws CompileException { return UnitCompiler.this.compileGet2(a);    }
            @Override public IClass visitUnaryOperation(UnaryOperation uo)                              throws CompileException { return UnitCompiler.this.compileGet2(uo);   }
            @Override public IClass visitBinaryOperation(BinaryOperation bo)                            throws CompileException { return UnitCompiler.this.compileGet2(bo);   }
            @Override public IClass visitCast(Cast c)                                                   throws CompileException { return UnitCompiler.this.compileGet2(c);    }
            @Override public IClass visitClassLiteral(ClassLiteral cl)                                  throws CompileException { return UnitCompiler.this.compileGet2(cl);   }
            @Override public IClass visitConditionalExpression(ConditionalExpression ce)                throws CompileException { return UnitCompiler.this.compileGet2(ce);   }
            @Override public IClass visitCrement(Crement c)                                             throws CompileException { return UnitCompiler.this.compileGet2(c);    }
            @Override public IClass visitInstanceof(Instanceof io)                                      throws CompileException { return UnitCompiler.this.compileGet2(io);   }
            @Override public IClass visitMethodInvocation(MethodInvocation mi)                          throws CompileException { return UnitCompiler.this.compileGet2(mi);   }
            @Override public IClass visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { return UnitCompiler.this.compileGet2(smi);  }
            @Override public IClass visitIntegerLiteral(IntegerLiteral il)                              throws CompileException { return UnitCompiler.this.compileGet2(il);   }
            @Override public IClass visitFloatingPointLiteral(FloatingPointLiteral fpl)                 throws CompileException { return UnitCompiler.this.compileGet2(fpl);  }
            @Override public IClass visitBooleanLiteral(BooleanLiteral bl)                              throws CompileException { return UnitCompiler.this.compileGet2(bl);   }
            @Override public IClass visitCharacterLiteral(CharacterLiteral cl)                          throws CompileException { return UnitCompiler.this.compileGet2(cl);   }
            @Override public IClass visitStringLiteral(StringLiteral sl)                                throws CompileException { return UnitCompiler.this.compileGet2(sl);   }
            @Override public IClass visitNullLiteral(NullLiteral nl)                                    throws CompileException { return UnitCompiler.this.compileGet2(nl);   }
            @Override public IClass visitSimpleConstant(SimpleConstant sl)                              throws CompileException { return UnitCompiler.this.compileGet2(sl);   }
            @Override public IClass visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      throws CompileException { return UnitCompiler.this.compileGet2(naci); }
            @Override public IClass visitNewArray(NewArray na)                                          throws CompileException { return UnitCompiler.this.compileGet2(na);   }
            @Override public IClass visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { return UnitCompiler.this.compileGet2(nia);  }
            @Override public IClass visitNewClassInstance(NewClassInstance nci)                         throws CompileException { return UnitCompiler.this.compileGet2(nci);  }
            @Override public IClass visitParameterAccess(ParameterAccess pa)                            throws CompileException { return UnitCompiler.this.compileGet2(pa);   }
            @Override public IClass visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { return UnitCompiler.this.compileGet2(qtr);  }
            @Override public IClass visitThisReference(ThisReference tr)                                throws CompileException { return UnitCompiler.this.compileGet2(tr);   }
            @Override public IClass visitLambdaExpression(LambdaExpression le)                          throws CompileException { return UnitCompiler.this.compileGet2(le);   }
            @Override public IClass visitMethodReference(MethodReference mr)                            throws CompileException { return UnitCompiler.this.compileGet2(mr);   }
            @Override public IClass visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { return UnitCompiler.this.compileGet2(cicr); }
            @Override public IClass visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { return UnitCompiler.this.compileGet2(acr);  }
        });

        assert result != null;
        return result;
    }

    private IClass
    compileGet2(BooleanRvalue brv) throws CompileException {
        CodeContext.Offset isTrue = this.getCodeContext().new Offset();
        isTrue.setStackMap(this.getCodeContext().currentInserter().getStackMap());
        this.compileBoolean(brv, isTrue, UnitCompiler.JUMP_IF_TRUE);
        this.consT(brv, 0);
        CodeContext.Offset end = this.getCodeContext().new Offset();
        this.gotO(brv, end);
        this.getCodeContext().currentInserter().setStackMap(null);
        isTrue.set();
        this.consT(brv, 1);
        end.set();

        return IClass.BOOLEAN;
    }

    private IClass
    compileGet2(AmbiguousName an) throws CompileException {
        return this.compileGet(this.toRvalueOrCompileException(this.reclassify(an)));
    }

    private IClass
    compileGet2(LocalVariableAccess lva) { return this.load(lva, lva.localVariable); }

    private IClass
    compileGet2(FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.getEnclosingScope(), fa.getLocation());
        this.getfield(fa, fa.field);
        return fa.field.getType();
    }

    private IClass
    compileGet2(ArrayLength al) {
        this.arraylength(al);
        return IClass.INT;
    }

    private IClass
    compileGet2(ThisReference tr) throws CompileException {
        final IClass currentIClass = this.getIClass(tr);
        this.referenceThis(tr, currentIClass);
        return currentIClass;
    }

    @SuppressWarnings("static-method") private IClass
    compileGet2(LambdaExpression le) throws CompileException {
        throw UnitCompiler.compileException(le, "Compilation of lambda expression NYI");
    }

    @SuppressWarnings("static-method") private IClass
    compileGet2(MethodReference mr) throws CompileException {
        throw UnitCompiler.compileException(mr, "Compilation of method reference NYI");
    }

    @SuppressWarnings("static-method") private IClass
    compileGet2(ClassInstanceCreationReference cicr) throws CompileException {
        throw UnitCompiler.compileException(cicr, "Compilation of class instance creation reference NYI");
    }

    @SuppressWarnings("static-method") private IClass
    compileGet2(ArrayCreationReference acr) throws CompileException {
        throw UnitCompiler.compileException(acr, "Compilation of array creation reference NYI");
    }

    private IClass
    compileGet2(QualifiedThisReference qtr) throws CompileException {
        this.referenceThis(
            qtr,                                       // locatable
            this.getDeclaringClass(qtr),               // declaringClass
            this.getDeclaringTypeBodyDeclaration(qtr), // declaringTypeBodyDeclaration
            this.getTargetIClass(qtr)                  // targetIClass
        );
        return this.getTargetIClass(qtr);
    }

    private IClass
    compileGet2(ClassLiteral cl) throws CompileException {

        IClass iClass = this.getType(cl.type);
        if (iClass.isPrimitive()) {

            // Primitive class literal.
            IClass wrapperIClass = (
                iClass == IClass.VOID    ? this.iClassLoader.TYPE_java_lang_Void      :
                iClass == IClass.BYTE    ? this.iClassLoader.TYPE_java_lang_Byte      :
                iClass == IClass.CHAR    ? this.iClassLoader.TYPE_java_lang_Character :
                iClass == IClass.DOUBLE  ? this.iClassLoader.TYPE_java_lang_Double    :
                iClass == IClass.FLOAT   ? this.iClassLoader.TYPE_java_lang_Float     :
                iClass == IClass.INT     ? this.iClassLoader.TYPE_java_lang_Integer   :
                iClass == IClass.LONG    ? this.iClassLoader.TYPE_java_lang_Long      :
                iClass == IClass.SHORT   ? this.iClassLoader.TYPE_java_lang_Short     :
                iClass == IClass.BOOLEAN ? this.iClassLoader.TYPE_java_lang_Boolean   :
                null
            );
            assert wrapperIClass != null;

            this.getfield(cl, wrapperIClass, "TYPE", this.iClassLoader.TYPE_java_lang_Class, true);
        } else {

            // Non-primitive class literal.

            this.consT(cl, iClass);
        }

        return this.iClassLoader.TYPE_java_lang_Class;
    }

    private IClass
    compileGet2(Assignment a) throws CompileException {

        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            // Compile LHS context.
            int lhsCs = this.compileContext(a.lhs);
            // Convert RHS value to LHS type.
            IClass lhsType = this.getType(a.lhs);
            IClass rhsType = this.compileGetValue(a.rhs);
            this.assignmentConversion(a, rhsType, lhsType, this.getConstantValue(a.rhs));
            // Duplicate RHS value below LHS context.
            this.dupxx(a, lhsCs);
            // Set LHS.
            this.compileSet(a.lhs);

            return lhsType;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCs = this.compileContext(a.lhs);
        this.dupn(a, lhsCs);
        IClass lhsType    = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), /* <= IMPORTANT! */
            a.rhs                 // rhs
        );

        // Convert the result to LHS type (JLS7 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
        ) throw new InternalCompilerException("SNO: \"" + a.operator + "\" reconversion failed");
        this.dupx(a);
        this.compileSet(a.lhs);
        return lhsType;
    }

    private IClass
    compileGet2(ConditionalExpression ce) throws CompileException {

        IClass expressionType = this.getType2(ce);
        IClass mhsType        = this.getType(ce.mhs);
        IClass rhsType        = this.getType(ce.rhs);

        {
            Object lhsCv = this.getConstantValue(ce.lhs);
            if (lhsCv instanceof Boolean) {

                // LHS is a constant expression.
                if (((Boolean) lhsCv).booleanValue()) {
                    this.compileGetValue(ce.mhs);
                    this.castConversion(ce.mhs, mhsType, expressionType, UnitCompiler.NOT_CONSTANT);
                } else {
                    this.compileGetValue(ce.rhs);
                    this.castConversion(ce.rhs, rhsType, expressionType, UnitCompiler.NOT_CONSTANT);
                }
                return expressionType;
            }
        }

        // Non-constant LHS.
        {
            final CodeContext.Offset toEnd = this.getCodeContext().new Offset();
            final CodeContext.Offset toRhs = this.getCodeContext().new Offset();

            StackMap sm = this.getCodeContext().currentInserter().getStackMap();

            this.compileBoolean(ce.lhs, toRhs, UnitCompiler.JUMP_IF_FALSE);

            this.compileGetValue(ce.mhs);
            this.assignmentConversion(ce.mhs, mhsType, expressionType, UnitCompiler.NOT_CONSTANT);
            this.gotO(ce, toEnd);

            this.getCodeContext().currentInserter().setStackMap(sm);
            toRhs.set();
            this.compileGetValue(ce.rhs);
            this.assignmentConversion(ce.mhs, rhsType, expressionType, UnitCompiler.NOT_CONSTANT);

            toEnd.set();
        }

        return expressionType;
    }

    private IClass
    commonSupertype(IClass t1, IClass t2) throws CompileException {

        if (t2.isAssignableFrom(t1)) return t2;

        return this.commonSupertype2(t1, t2);
    }

    private IClass
    commonSupertype2(IClass t1, IClass t2) throws CompileException {

        if (t1.isAssignableFrom(t2)) return t1;

        {
            IClass sc = t1.getSuperclass();
            if (sc != null) {
                IClass result = this.commonSupertype2(sc, t2);
                if (result != this.iClassLoader.TYPE_java_lang_Object) return result;
            }
        }

        for (IClass i : t1.getInterfaces()) {
            IClass result = this.commonSupertype2(i, t2);
            if (result != this.iClassLoader.TYPE_java_lang_Object) return result;
        }

        return this.iClassLoader.TYPE_java_lang_Object;
    }

    @Nullable private static Byte
    isByteConstant(@Nullable Object o) {
        if (o instanceof Integer) {
            int v = (Integer) o;
            return v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE ? Byte.valueOf((byte) v) : null;
        }
        return null;
    }

    private IClass
    compileGet2(Crement c) throws CompileException {

        // Optimized crement of "int" local variable.
        LocalVariable lv = this.isIntLv(c);
        if (lv != null) {
            if (!c.pre) this.load(c, lv);
            this.iinc(c, lv, c.operator);
            if (c.pre) this.load(c, lv);
            return lv.type;
        }

        // Compile operand context.
        int operandCs = this.compileContext(c.operand);
        // DUP operand context.
        this.dupn(c, operandCs);
        // Get operand value.
        IClass type = this.compileGet(c.operand);
        // If postincrement: DUPX the operand value.
        if (!c.pre) this.dupxx(c, operandCs);

        {

            // Apply "unary numeric promotion".
            IClass promotedType = this.unaryNumericPromotion(c, type);
            // Crement.
            this.consT(c, promotedType, 1);
            if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.add(c);
            } else
            if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.sub(c);
            } else {
                this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
            }
            this.reverseUnaryNumericPromotion(c, promotedType, type);
        }

        // If preincrement: DUPX the cremented operand value.
        if (c.pre) this.dupxx(c, operandCs);
        // Set operand.
        this.compileSet(c.operand);

        return type;
    }

    private IClass
    compileGet2(ArrayAccessExpression aae) throws CompileException {
        IClass lhsComponentType = this.getType(aae);
        this.xaload(aae, lhsComponentType);
        return lhsComponentType;
    }

    private IClass
    compileGet2(FieldAccessExpression fae) throws CompileException {
        return this.compileGet(this.determineValue(fae));
    }

    private IClass
    compileGet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        return this.compileGet(this.determineValue(scfae));
    }

    private IClass
    compileGet2(UnaryOperation uo) throws CompileException {

        if (uo.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.compileGet2((BooleanRvalue) uo);
        }

        if (uo.operator == "+") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.unaryNumericPromotion(
                uo,
                this.convertToPrimitiveNumericType(uo, this.compileGetValue(uo.operand))
            );
        }

        if (uo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            {
                Object ncv = this.getConstantValue2(uo);
                if (ncv != UnitCompiler.NOT_CONSTANT) {
                    return this.unaryNumericPromotion(uo, this.consT(uo, ncv));
                }
            }

            IClass promotedType = this.unaryNumericPromotion(
                uo,
                this.convertToPrimitiveNumericType(uo, this.compileGetValue(uo.operand))
            );
            this.neg(uo, promotedType);
            return promotedType;
        }

        if (uo.operator == "~") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IClass operandType = this.compileGetValue(uo.operand);

            IClass promotedType = this.unaryNumericPromotion(uo, operandType);
            if (promotedType == IClass.INT) {
                this.consT(uo, -1);
                this.xor(uo, Opcode.IXOR);
                return IClass.INT;
            }
            if (promotedType == IClass.LONG) {
                this.consT(uo, -1L);
                this.xor(uo, Opcode.LXOR);
                return IClass.LONG;
            }
            this.compileError("Operator \"~\" not applicable to type \"" + promotedType + "\"", uo.getLocation());
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    private IClass
    compileGet2(Instanceof io) throws CompileException {
        IClass lhsType = this.compileGetValue(io.lhs);
        IClass rhsType = this.getType(io.rhs);
        if (
            lhsType.isInterface() || rhsType.isInterface()
            // We cannot precompute the result from type information as the value might be null, but we should detect
            // when the instanceof is statically impossible.
            || lhsType.isAssignableFrom(rhsType)
            || rhsType.isAssignableFrom(lhsType)
        ) {
            this.instanceoF(io, rhsType);
        } else {
            this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"", io.getLocation());
        }
        return IClass.BOOLEAN;
    }

    private IClass
    compileGet2(BinaryOperation bo) throws CompileException {
        if (
            bo.operator == "||"    // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "==" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "!=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "<"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == ">"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == "<=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.operator == ">=" // SUPPRESS CHECKSTYLE StringLiteralEquality
        ) {
            // Eventually calls "compileBoolean()".
            return this.compileGet2((BooleanRvalue) bo);
        }

        // Implements "| ^ & * / % + - << >> >>>".
        return this.compileArithmeticOperation(
            bo,                         // locatable
            null,                       // type
            bo.unrollLeftAssociation(), // operands
            bo.operator                 // operator
        );
    }

    private IClass
    compileGet2(Cast c) throws CompileException {

        // JLS7 5.5 Casting Conversion.
        IClass tt = this.getType(c.targetType);
        IClass vt = this.compileGetValue(c.value);

        if (this.tryCastConversion(c, vt, tt, this.getConstantValue2(c.value))) return tt;

        // JAVAC obviously also permits 'boxing conversion followed by widening reference conversion' and 'unboxing
        // conversion followed by widening primitive conversion', although these are not described in JLS7 5.5. For the
        // sake of compatibility, we implement them.
        // See also http://jira.codehaus.org/browse/JANINO-153
        {
            IClass boxedType = this.isBoxingConvertible(vt);
            if (boxedType != null && this.isWideningReferenceConvertible(boxedType, tt)) {
                this.boxingConversion(c, vt, boxedType);
                return tt;
            }

            IClass unboxedType = this.isUnboxingConvertible(vt);
            if (unboxedType != null && this.isWideningPrimitiveConvertible(unboxedType, tt)) {
                this.unboxingConversion(c, vt, unboxedType);
                this.tryWideningPrimitiveConversion(c, unboxedType, tt);
                return tt;
            }
        }

        this.compileError("Cannot cast \"" + vt + "\" to \"" + tt + "\"", c.getLocation());
        return tt;
    }

    private IClass
    compileGet2(ParenthesizedExpression pe) throws CompileException {
        return this.compileGet(pe.value);
    }

    private IClass
    compileGet2(MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(mi);

        // Compute the objectref for an instance method.
        Atom ot = mi.target;
        if (ot == null) {

            // JLS7 6.5.7.1, 15.12.4.1.1.1
            TypeBodyDeclaration      scopeTbd;
            AbstractClassDeclaration scopeClassDeclaration;
            {
                Scope s;
                for (
                    s = mi.getEnclosingScope();
                    !(s instanceof TypeBodyDeclaration);
                    s = s.getEnclosingScope()
                );
                scopeTbd = (TypeBodyDeclaration) s;
                if (!(s instanceof AbstractClassDeclaration)) s = s.getEnclosingScope();
                scopeClassDeclaration = (AbstractClassDeclaration) s;
            }
            if (iMethod.isStatic()) {
                this.warning(
                    "IASM",
                    "Implicit access to static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );
                // JLS7 15.12.4.1.1.1.1
                ;
            } else {
                this.warning(
                    "IANSM",
                    "Implicit access to non-static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );

                // JLS7 15.12.4.1.1.1.2
                if (UnitCompiler.isStaticContext(scopeTbd)) {
                    this.compileError(
                        "Instance method \"" + iMethod.toString() + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }

                this.referenceThis(
                    mi,                          // locatable
                    scopeClassDeclaration,       // declaringClass
                    scopeTbd,                    // declaringTypeBodyDeclaration
                    iMethod.getDeclaringIClass() // targetIClass
                );
            }
        } else {

            // 6.5.7.2
            if (this.isType(ot)) {

                // The target is a type; thus the method must be static.
                // JLS9 15.12.4.1.2:
                this.getType(this.toTypeOrCompileException(ot));
                if (!iMethod.isStatic()) {
                    this.compileError(
                        "Instance method \"" + mi.methodName + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }
            } else
            {

                // The target is an rvalue.
                Rvalue rot = this.toRvalueOrCompileException(ot);
                if (iMethod.isStatic()) {

                    // JLS9 15.12.4.1.3.1 and .4.1:
                    if (!UnitCompiler.mayHaveSideEffects(rot)) {

                        // The rvalue is guaranteed to have no side effects, so we can save the code to evaluate it
                        // and then discard the result.
                        ;
                    } else {

                        // Evaluate the target expression and then discard the result.
                        this.pop(ot, this.compileGetValue(rot));
                    }
                } else {

                    // JLS9 15.12.4.1.3.2 and .4.2
                    this.compileGetValue(rot);

                    if (this.getCodeContext().peekNullOperand()) {
                        this.compileError("Method invocation target is always null");
                        this.getCodeContext().popOperand();
                        this.getCodeContext().pushObjectOperand(iMethod.getDeclaringIClass().getDescriptor());
                    }
                }
            }
        }

        // Evaluate method parameters (JLS7 15.12.4.2).
        // If this method is vararg, rewritten all args starting from lastParamIndex to the end as if they were elements
        // of an array.
        IClass[]  parameterTypes = iMethod.getParameterTypes();
        Rvalue[]  adjustedArgs   = null;
        final int actualSize     = mi.arguments.length;
        if (iMethod.isVarargs() && iMethod.argsNeedAdjust()) {
            adjustedArgs = new Rvalue[parameterTypes.length];
            Rvalue[]       lastArgs = new Rvalue[actualSize - parameterTypes.length + 1];
            final Location loc      = mi.getLocation();

            if (lastArgs.length > 0) {
                for (int i = 0, j = parameterTypes.length - 1; i < lastArgs.length; ++i, ++j) {
                    lastArgs[i] = mi.arguments[j];
                }
            }

            for (int i = parameterTypes.length - 2; i >= 0; --i) {
                adjustedArgs[i] = mi.arguments[i];
            }
            adjustedArgs[adjustedArgs.length - 1] = new NewInitializedArray(
                loc,                                       // location
                parameterTypes[parameterTypes.length - 1], // arrayIClass
                new ArrayInitializer(loc, lastArgs)        // arrayInitializer
            );
        } else {
            adjustedArgs = mi.arguments;
        }

        for (int i = 0; i < adjustedArgs.length; ++i) {
            this.assignmentConversion(
                mi,                                    // location
                this.compileGetValue(adjustedArgs[i]), // sourceType
                parameterTypes[i],                     // targetType
                this.getConstantValue(adjustedArgs[i]) // constantValue
            );
        }
        // Invoke!
        this.checkAccessible(iMethod, mi.getEnclosingScope(), mi.getLocation());
        if (/*!iMethod.getDeclaringIClass().isInterface() &&*/ !iMethod.isStatic() && iMethod.getAccess() == Access.PRIVATE) {

            // In order to make a non-static private method invocable for enclosing types, enclosed types and types
            // enclosed by the same type, "compile(FunctionDeclarator)" modifies it on-the-fly as follows:
            //  + Access is changed from PRIVATE to PACKAGE
            //  + The name is appended with "$"
            //  + It is made static
            //  + A parameter of type "declaring class" is prepended to the signature
            // Hence, the invocation of such a method must be modified accordingly.
            this.invokeMethod(
                mi,                                           // locatable
                Opcode.INVOKESTATIC,                          // opcode
                iMethod.getDeclaringIClass(),                 // declaringIClass
                iMethod.getName() + '$',                      // methodName
                iMethod.getDescriptor().prependParameter(     // methodMd
                    iMethod.getDeclaringIClass().getDescriptor()
                ),
                false                                         // useInterfaceMethodref
            );
        } else
        {
            this.invoke(mi, iMethod);
        }
        return iMethod.getReturnType();
    }

    private static boolean
    isStaticContext(TypeBodyDeclaration tbd) {
        if (tbd instanceof FieldDeclaration)       return ((FieldDeclaration)       tbd).isStatic() || ((FieldDeclaration) tbd).getDeclaringType() instanceof InterfaceDeclaration; // SUPPRESS CHECKSTYLE LineLength
        if (tbd instanceof MethodDeclarator)       return ((MethodDeclarator)       tbd).isStatic();
        if (tbd instanceof Initializer)            return ((Initializer)            tbd).isStatic();
        if (tbd instanceof MemberClassDeclaration) return ((MemberClassDeclaration) tbd).isStatic();

        return false;
    }

    private static boolean
    mayHaveSideEffects(Rvalue... rvalues) {

        for (Rvalue rvalue : rvalues) {
            Boolean result = (Boolean) rvalue.accept(UnitCompiler.MAY_HAVE_SIDE_EFFECTS_VISITOR);
            assert result != null;
            if (result) return true;
        }

        return false;
    }

    /*@SuppressWarnings("null")*/ private static final Visitor.RvalueVisitor<Boolean, RuntimeException>
    MAY_HAVE_SIDE_EFFECTS_VISITOR = new Visitor.RvalueVisitor<Boolean, RuntimeException>() {

        final Visitor.LvalueVisitor<Boolean, RuntimeException>
        lvalueVisitor = new Visitor.LvalueVisitor<Boolean, RuntimeException>() {

            // SUPPRESS CHECKSTYLE LineLengthCheck:7
            @Override @Nullable public Boolean visitAmbiguousName(AmbiguousName an)                                        { return false;                                               }
            @Override @Nullable public Boolean visitArrayAccessExpression(ArrayAccessExpression aae)                       { return UnitCompiler.mayHaveSideEffects(aae.lhs, aae.index); }
            @Override @Nullable public Boolean visitFieldAccess(FieldAccess fa)                                            { return false;                                               }
            @Override @Nullable public Boolean visitFieldAccessExpression(FieldAccessExpression fae)                       { return false;                                               }
            @Override @Nullable public Boolean visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { return false;                                               }
            @Override @Nullable public Boolean visitLocalVariableAccess(LocalVariableAccess lva)                           { return false;                                               }
            @Override @Nullable public Boolean visitParenthesizedExpression(ParenthesizedExpression pe)                    { return UnitCompiler.mayHaveSideEffects(pe.value);           }
        };

        // SUPPRESS CHECKSTYLE LineLengthCheck:30
        @Override @Nullable public Boolean visitLvalue(Lvalue lv)                                              { return (Boolean) lv.accept(this.lvalueVisitor);                 }
        @Override @Nullable public Boolean visitArrayLength(ArrayLength al)                                    { return UnitCompiler.mayHaveSideEffects(al.lhs);                 }
        @Override @Nullable public Boolean visitAssignment(Assignment a)                                       { return true;                                                    }
        @Override @Nullable public Boolean visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.mayHaveSideEffects(uo.operand);             }
        @Override @Nullable public Boolean visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.mayHaveSideEffects(bo.lhs, bo.rhs);         }
        @Override @Nullable public Boolean visitCast(Cast c)                                                   { return UnitCompiler.mayHaveSideEffects(c.value);                }
        @Override @Nullable public Boolean visitClassLiteral(ClassLiteral cl)                                  { return false;                                                   }
        @Override @Nullable public Boolean visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.mayHaveSideEffects(ce.lhs, ce.mhs, ce.rhs); }
        @Override @Nullable public Boolean visitCrement(Crement c)                                             { return true;                                                    }
        @Override @Nullable public Boolean visitInstanceof(Instanceof io)                                      { return false;                                                   }
        @Override @Nullable public Boolean visitMethodInvocation(MethodInvocation mi)                          { return true;                                                    }
        @Override @Nullable public Boolean visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return true;                                                    }
        @Override @Nullable public Boolean visitIntegerLiteral(IntegerLiteral il)                              { return false;                                                   }
        @Override @Nullable public Boolean visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return false;                                                   }
        @Override @Nullable public Boolean visitBooleanLiteral(BooleanLiteral bl)                              { return false;                                                   }
        @Override @Nullable public Boolean visitCharacterLiteral(CharacterLiteral cl)                          { return false;                                                   }
        @Override @Nullable public Boolean visitStringLiteral(StringLiteral sl)                                { return false;                                                   }
        @Override @Nullable public Boolean visitNullLiteral(NullLiteral nl)                                    { return false;                                                   }
        @Override @Nullable public Boolean visitSimpleConstant(SimpleConstant sl)                              { return false;                                                   }
        @Override @Nullable public Boolean visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return true;                                                    }
        @Override @Nullable public Boolean visitNewArray(NewArray na)                                          { return UnitCompiler.mayHaveSideEffects(na.dimExprs);            }
        @Override @Nullable public Boolean visitNewInitializedArray(NewInitializedArray nia)                   { return this.mayHaveSideEffects(nia.arrayInitializer);           }
        @Override @Nullable public Boolean visitNewClassInstance(NewClassInstance nci)                         { return true;                                                    }
        @Override @Nullable public Boolean visitParameterAccess(ParameterAccess pa)                            { return false;                                                   }
        @Override @Nullable public Boolean visitQualifiedThisReference(QualifiedThisReference qtr)             { return false;                                                   }
        @Override @Nullable public Boolean visitThisReference(ThisReference tr)                                { return false;                                                   }
        @Override @Nullable public Boolean visitLambdaExpression(LambdaExpression le)                          { return true;                                                    }
        @Override @Nullable public Boolean visitMethodReference(MethodReference mr)                            { return true;                                                    }
        @Override @Nullable public Boolean visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return true;                                                    }
        @Override @Nullable public Boolean visitArrayCreationReference(ArrayCreationReference acr)             { return false;                                                   }

        private boolean
        mayHaveSideEffects(ArrayInitializer arrayInitializer) {

            for (ArrayInitializerOrRvalue aiorv : arrayInitializer.values) {
                if (aiorv instanceof Rvalue) {
                    if (UnitCompiler.mayHaveSideEffects((Rvalue) aiorv)) return true;
                } else
                if (aiorv instanceof ArrayInitializer) {
                    if (this.mayHaveSideEffects((ArrayInitializer) aiorv)) return true;
                } else
                {
                    throw new AssertionError(aiorv);
                }
            }

            return false;
        }
    };

    private IClass
    compileGet2(SuperclassMethodInvocation scmi) throws CompileException {
        final IClass.IMethod iMethod = this.findIMethod(scmi);

        Scope s;
        for (
            s = scmi.getEnclosingScope();
            s instanceof Statement || s instanceof CatchClause;
            s = s.getEnclosingScope()
        );
        FunctionDeclarator fd = s instanceof FunctionDeclarator ? (FunctionDeclarator) s : null;
        if (fd == null) {
            this.compileError("Cannot invoke superclass method in non-method scope", scmi.getLocation());
            return IClass.INT;
        }
        if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isStatic()) {
            this.compileError("Cannot invoke superclass method in static context", scmi.getLocation());
        }
        this.load(scmi, this.resolve(fd.getDeclaringType()), 0);

        // Evaluate method parameters.
        // TODO: adjust args
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < scmi.arguments.length; ++i) {
            this.assignmentConversion(
                scmi,                                    // locatable
                this.compileGetValue(scmi.arguments[i]), // sourceType
                parameterTypes[i],                       // targetType
                this.getConstantValue(scmi.arguments[i]) // constantValue
            );
        }

        // Invoke!
        this.invokeMethod(
            scmi,                         // locatable
            Opcode.INVOKESPECIAL,         // opcode
            iMethod.getDeclaringIClass(), // declaringIClass
            iMethod.getName(),            // methodName
            iMethod.getDescriptor(),      // methodMd
            false                         // useInterfaceMethodref
        );

        return iMethod.getReturnType();
    }

    private IClass
    compileGet2(NewClassInstance nci) throws CompileException {
        IClass iClass;
        if (nci.iClass != null) {
            iClass = nci.iClass;
        } else {
            assert nci.type != null;
            iClass = (nci.iClass = this.getType(nci.type));
        }

        if (iClass.isInterface()) this.compileError("Cannot instantiate \"" + iClass + "\"", nci.getLocation());
        this.checkAccessible(iClass, nci.getEnclosingScope(), nci.getLocation());
        if (iClass.isAbstract()) {
            this.compileError("Cannot instantiate abstract \"" + iClass + "\"", nci.getLocation());
        }

        // Determine the enclosing instance for the new object.
        Rvalue enclosingInstance;
        if (nci.qualification != null) {
            if (iClass.getOuterIClass() == null) {
                this.compileError("Static member class cannot be instantiated with qualified NEW");
            }

            // Enclosing instance defined by qualification (JLS7 15.9.2.BL1.B3.B2).
            enclosingInstance = nci.qualification;
        } else {
            Scope s = nci.getEnclosingScope();

            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            TypeBodyDeclaration enclosingTypeBodyDeclaration = (TypeBodyDeclaration) s;

            TypeDeclaration enclosingTypeDeclaration = (TypeDeclaration) s.getEnclosingScope();

            if (
                !(enclosingTypeDeclaration instanceof AbstractClassDeclaration)
                || (
                    enclosingTypeBodyDeclaration instanceof MemberClassDeclaration
                    && ((MemberClassDeclaration) enclosingTypeBodyDeclaration).isStatic()
                )
                || (
                    enclosingTypeBodyDeclaration instanceof PackageMemberClassDeclaration
                    && ((PackageMemberClassDeclaration) enclosingTypeBodyDeclaration).isStatic()
                )
            ) {

                // No enclosing instance in
                //  + interface method declaration or
                //  + static type body declaration (here: method or initializer or field declarator)
                // context (JLS7 15.9.2.BL1.B3.B1.B1).
                if (iClass.getOuterIClass() != null) {
                    this.compileError((
                        "Instantiation of \""
                        + (nci.type != null ? nci.type.toString() : String.valueOf(nci.iClass))
                        + "\" requires an enclosing instance"
                    ), nci.getLocation());
                }
                enclosingInstance = null;
            } else
            {

                // Determine the type of the enclosing instance for the new object.
                IClass outerIClass = iClass.getDeclaringIClass();
                if (outerIClass == null) {

                    // No enclosing instance needed for a top-level class object.
                    enclosingInstance = null;
                } else {

                    // Find an appropriate enclosing instance for the new inner class object among the enclosing
                    // instances of the current object (JLS7 15.9.2.BL1.B3.B1.B2).
                    enclosingInstance = new QualifiedThisReference(
                        nci.getLocation(), // location
                        new SimpleType(    // qualification
                            nci.getLocation(),
                            outerIClass
                        )
                    );
                    enclosingInstance.setEnclosingScope(nci.getEnclosingScope());
                }
            }
        }

        this.neW(nci, iClass);
        this.dup(nci);
        this.invokeConstructor(
            nci,                     // l
            nci.getEnclosingScope(), // scope
            enclosingInstance,       // enclosingInstance
            iClass,                  // targetClass
            nci.arguments            // arguments
        );
        this.getCodeContext().popUninitializedVariableOperand();
        this.getCodeContext().pushObjectOperand(iClass.getDescriptor());

        return iClass;
    }

    private IClass
    compileGet2(NewAnonymousClassInstance naci) throws CompileException {
        AnonymousClassDeclaration acd = naci.anonymousClassDeclaration;

        // Find constructors of superclass.
        IClass sc = this.resolve(acd).getSuperclass();
        assert sc != null;

        IClass.IConstructor[] superclassIConstructors = sc.getDeclaredIConstructors();
        if (superclassIConstructors.length == 0) {
            throw new InternalCompilerException("SNO: Superclass has no constructors");
        }

        // Determine the most specific constructor of the superclass.
        IClass.IConstructor superclassIConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            naci,                    // locatable
            superclassIConstructors, // iInvocables
            naci.arguments,          // arguments
            acd                      // contextScope
        );

        Location loc                   = naci.getLocation();
        Rvalue   qualification = naci.qualification;

        // Determine the formal parameters of the anonymous constructor.
        IClass[]         scpts = superclassIConstructor.getParameterTypes();
        FormalParameters parameters;
        {
            List<FormalParameter> l = new ArrayList<FormalParameter>();

            // Pass the enclosing instance of the base class as parameter #1.
            if (qualification != null) l.add(new FormalParameter(
                loc,                                                      // location
                UnitCompiler.accessModifiers(loc, "final"),               // modifiers
                new SimpleType(loc, this.getType(qualification)), // type
                "this$base"                                               // name
            ));
            for (int i = 0; i < scpts.length; ++i) l.add(new FormalParameter(
                loc,                                        // location
                UnitCompiler.accessModifiers(loc, "final"), // modifiers
                new SimpleType(loc, scpts[i]),              // type
                "p" + i                                     // name
            ));
            parameters = new FormalParameters(
                loc,
                (FormalParameter[]) l.toArray(new FormalParameter[l.size()]),
                false
            );
        }

        // Determine the declared exceptions of the anonymous constructor.
        Type[] thrownExceptions;
        {
            IClass[] tes = superclassIConstructor.getThrownExceptions();
            thrownExceptions = new Type[tes.length];
            for (int i = 0; i < tes.length; ++i) thrownExceptions[i] = new SimpleType(loc, tes[i]);
        }

        // The anonymous constructor merely invokes the constructor of its superclass.
        int    j = 0;
        Rvalue qualificationAccess;
        if (qualification == null) {
            qualificationAccess = null;
        } else
        {
            qualificationAccess = new ParameterAccess(loc, parameters.parameters[j++]);
        }
        Rvalue[] parameterAccesses = new Rvalue[scpts.length];
        for (int i = 0; i < scpts.length; ++i) {
            parameterAccesses[i] = new ParameterAccess(loc, parameters.parameters[j++]);
        }

        // Generate the anonymous constructor for the anonymous class (JLS7 15.9.5.1).
        acd.addConstructor(new ConstructorDeclarator(
            loc,                                    // location
            null,                                   // docComment
            new Modifier[0],                        // modifiers
            parameters,                             // parameters
            thrownExceptions,                       // thrownExceptions
            new SuperConstructorInvocation(         // constructorInvocation
                loc,                            // location
                qualificationAccess,    // qualification
                parameterAccesses               // arguments
            ),
            Collections.<BlockStatement>emptyList() // statements
        ));

        // Compile the anonymous class.
        try {
            this.compile(acd);

            // Instantiate the anonymous class.
            IClass anonymousIClass = this.resolve(acd);
            this.neW(naci, anonymousIClass);

            // TODO: adjust argument (for varargs case ?)
            // Invoke the anonymous constructor.
            this.dup(naci);
            Rvalue[] arguments2;
            if (qualification == null) {
                arguments2 = naci.arguments;
            } else {
                arguments2    = new Rvalue[naci.arguments.length + 1];
                arguments2[0] = qualification;
                System.arraycopy(naci.arguments, 0, arguments2, 1, naci.arguments.length);
            }

            // Adjust if needed.
            // TODO: Not doing this now because we don't need vararg-anonymous class (yet).

//            Rvalue[] adjustedArgs = null;
//            final int paramsTypeLength = iConstructor.getParameterTypes().length;
//            if (argsNeedAdjusting[0]) {
//                adjustedArgs = new Rvalue[paramsTypeLength];
//            }

            // Notice: The enclosing instance of the anonymous class is "this", not the qualification of the
            // NewAnonymousClassInstance.
            Scope s;
            for (s = naci.getEnclosingScope(); !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            ThisReference oei;
            if (UnitCompiler.isStaticContext((TypeBodyDeclaration) s)) {
                oei = null;
            } else
            {
                oei = new ThisReference(loc);
                oei.setEnclosingScope(naci.getEnclosingScope());
            }
            this.invokeConstructor(
                naci,                                         // locatable
                naci.getEnclosingScope(),                     // scope
                oei,                                          // enclosingInstance
                this.resolve(naci.anonymousClassDeclaration), // targetClass
                arguments2                                    // arguments
            );
        } finally {

            // Remove the synthetic constructor that was temporarily added. This is necessary because this NACI
            // expression (and all other expressions) are sometimes compiled more than once (see "fakeCompile()"), and
            // we'd end up with TWO synthetic constructors. See JANINO-143.
            acd.constructors.remove(acd.constructors.size() - 1);
        }
        return this.resolve(naci.anonymousClassDeclaration);
    }
    private IClass
    compileGet2(ParameterAccess pa) throws CompileException {
        LocalVariable lv = this.getLocalVariable(pa.formalParameter);
        this.load(pa, lv);
        return lv.type;
    }
    private IClass
    compileGet2(NewArray na) throws CompileException {
        for (Rvalue dimExpr : na.dimExprs) {
            IClass dimType = this.compileGetValue(dimExpr);
            if (dimType != IClass.INT && this.unaryNumericPromotion(
                na,     // locatable
                dimType // type
            ) != IClass.INT) this.compileError("Invalid array size expression type", na.getLocation());
        }

        return this.newArray(
            na,                   // locatable
            na.dimExprs.length,   // dimExprCount
            na.dims,              // dims
            this.getType(na.type) // componentType
        );
    }
    private IClass
    compileGet2(NewInitializedArray nia) throws CompileException {

        IClass at = this.getType2(nia);

        this.compileGetValue(nia.arrayInitializer, at);

        return at;
    }
    private void
    compileGetValue(ArrayInitializer ai, IClass arrayType) throws CompileException {

        if (!arrayType.isArray()) {
            this.compileError("Array initializer not allowed for non-array type \"" + arrayType.toString() + "\"");
        }

        IClass ct = arrayType.getComponentType();
        assert ct != null;

        this.consT(ai, new Integer(ai.values.length));
        this.newArray(
            ai, // locatable
            1,  // dimExprCount
            0,  // dims
            ct  // componentType
        );

        for (int i = 0; i < ai.values.length; ++i) {
            ArrayInitializerOrRvalue aiorv = ai.values[i];

            this.dup(aiorv);
            this.consT(ai, i);
            if (aiorv instanceof Rvalue) {
                Rvalue rv = (Rvalue) aiorv;
                this.assignmentConversion(
                    ai,                       // locatable
                    this.compileGetValue(rv), // sourceType
                    ct,                       // targetType
                    this.getConstantValue(rv) // constantValue
                );
            } else
            if (aiorv instanceof ArrayInitializer) {
                this.compileGetValue((ArrayInitializer) aiorv, ct);
            } else
            {
                throw new InternalCompilerException(
                    "Unexpected array initializer or rvalue class " + aiorv.getClass().getName()
                );
            }
            this.arraystore(aiorv, ct);
        }
    }

    private IClass
    compileGet2(Literal l) throws CompileException {
        return this.consT(l, this.getConstantValue(l));
    }
    private IClass
    compileGet2(SimpleConstant sl) throws CompileException {
        return this.consT(sl, sl.value);
    }

    /**
     * Convenience function that calls {@link #compileContext(Rvalue)} and {@link #compileGet(Rvalue)}.
     *
     * @return The type of the Rvalue
     */
    private IClass
    compileGetValue(Rvalue rv) throws CompileException {
        Object cv = this.getConstantValue(rv);
        if (cv != UnitCompiler.NOT_CONSTANT) {
            this.fakeCompile(rv); // To check that, e.g., "a" compiles in "true || a".
            this.consT(rv, cv);
            return this.getType(rv);
        }

        this.compileContext(rv);
        return this.compileGet(rv);
    }

    // -------------------- Rvalue.getConstantValue() -----------------

    /**
     * Special return value for the {@link #getConstantValue(Java.Rvalue)} method family indicating that the given
     * {@link Java.Rvalue} does not evaluate to a constant value.
     */
    public static final Object NOT_CONSTANT = IClass.NOT_CONSTANT;

    /**
     * Attempts to evaluate as a constant expression. The result is one of the following: {@link Boolean}, {@link
     * Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link Character}, {@link
     * String}, {@code null} (representing the {@code null} literal.
     * <p>
     *   This method cannot be STATIC, because the constant value may refer to a constant declaration in this
     *   compilation unit.
     * </p>
     *
     * @return {@link #NOT_CONSTANT} iff the rvalue is not a constant value
     */
    @Nullable public final Object
    getConstantValue(Rvalue rv) throws CompileException {

        if (rv.constantValue != Rvalue.CONSTANT_VALUE_UNKNOWN) return rv.constantValue;

        return (rv.constantValue = rv.accept(new RvalueVisitor<Object, CompileException>() {

            @Override @Nullable public Object
            visitLvalue(Lvalue lv) throws CompileException {
                return lv.accept(new Visitor.LvalueVisitor<Object, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLengthCheck:7
                    @Override @Nullable public Object visitAmbiguousName(AmbiguousName an)                     throws CompileException { return UnitCompiler.this.getConstantValue2(an);    }
                    @Override @Nullable public Object visitArrayAccessExpression(ArrayAccessExpression aae)                            { return UnitCompiler.this.getConstantValue2(aae);   }
                    @Override @Nullable public Object visitFieldAccess(FieldAccess fa)                         throws CompileException { return UnitCompiler.this.getConstantValue2(fa);    }
                    @Override @Nullable public Object visitFieldAccessExpression(FieldAccessExpression fae)                            { return UnitCompiler.this.getConstantValue2(fae);   }
                    @Override @Nullable public Object visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae)      { return UnitCompiler.this.getConstantValue2(scfae); }
                    @Override @Nullable public Object visitLocalVariableAccess(LocalVariableAccess lva)        throws CompileException { return UnitCompiler.this.getConstantValue2(lva);   }
                    @Override @Nullable public Object visitParenthesizedExpression(ParenthesizedExpression pe) throws CompileException { return UnitCompiler.this.getConstantValue2(pe);    }
                });
            }

            // SUPPRESS CHECKSTYLE LineLengthCheck:29
            @Override @Nullable public Object visitArrayLength(ArrayLength al)                                             { return UnitCompiler.this.getConstantValue2(al);   }
            @Override @Nullable public Object visitAssignment(Assignment a)                                                { return UnitCompiler.this.getConstantValue2(a);    }
            @Override @Nullable public Object visitUnaryOperation(UnaryOperation uo)               throws CompileException { return UnitCompiler.this.getConstantValue2(uo);   }
            @Override @Nullable public Object visitBinaryOperation(BinaryOperation bo)             throws CompileException { return UnitCompiler.this.getConstantValue2(bo);   }
            @Override @Nullable public Object visitCast(Cast c)                                    throws CompileException { return UnitCompiler.this.getConstantValue2(c);    }
            @Override @Nullable public Object visitClassLiteral(ClassLiteral cl)                                           { return UnitCompiler.this.getConstantValue2(cl);   }
            @Override @Nullable public Object visitConditionalExpression(ConditionalExpression ce) throws CompileException { return UnitCompiler.this.getConstantValue2(ce);   }
            @Override @Nullable public Object visitCrement(Crement c)                                                      { return UnitCompiler.this.getConstantValue2(c);    }
            @Override @Nullable public Object visitInstanceof(Instanceof io)                                               { return UnitCompiler.this.getConstantValue2(io);   }
            @Override @Nullable public Object visitMethodInvocation(MethodInvocation mi)                                   { return UnitCompiler.this.getConstantValue2(mi);   }
            @Override @Nullable public Object visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)              { return UnitCompiler.this.getConstantValue2(smi);  }
            @Override @Nullable public Object visitIntegerLiteral(IntegerLiteral il)               throws CompileException { return UnitCompiler.this.getConstantValue2(il);   }
            @Override @Nullable public Object visitFloatingPointLiteral(FloatingPointLiteral fpl)  throws CompileException { return UnitCompiler.this.getConstantValue2(fpl);  }
            @Override @Nullable public Object visitBooleanLiteral(BooleanLiteral bl)                                       { return UnitCompiler.this.getConstantValue2(bl);   }
            @Override @Nullable public Object visitCharacterLiteral(CharacterLiteral cl)           throws CompileException { return UnitCompiler.this.getConstantValue2(cl);   }
            @Override @Nullable public Object visitStringLiteral(StringLiteral sl)                 throws CompileException { return UnitCompiler.this.getConstantValue2(sl);   }
            @Override @Nullable public Object visitNullLiteral(NullLiteral nl)                                             { return UnitCompiler.this.getConstantValue2(nl);   }
            @Override @Nullable public Object visitSimpleConstant(SimpleConstant sl)                                       { return UnitCompiler.this.getConstantValue2(sl);   }
            @Override @Nullable public Object visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)               { return UnitCompiler.this.getConstantValue2(naci); }
            @Override @Nullable public Object visitNewArray(NewArray na)                                                   { return UnitCompiler.this.getConstantValue2(na);   }
            @Override @Nullable public Object visitNewInitializedArray(NewInitializedArray nia)                            { return UnitCompiler.this.getConstantValue2(nia);  }
            @Override @Nullable public Object visitNewClassInstance(NewClassInstance nci)                                  { return UnitCompiler.this.getConstantValue2(nci);  }
            @Override @Nullable public Object visitParameterAccess(ParameterAccess pa)                                     { return UnitCompiler.this.getConstantValue2(pa);   }
            @Override @Nullable public Object visitQualifiedThisReference(QualifiedThisReference qtr)                      { return UnitCompiler.this.getConstantValue2(qtr);  }
            @Override @Nullable public Object visitThisReference(ThisReference tr)                                         { return UnitCompiler.this.getConstantValue2(tr);   }
            @Override @Nullable public Object visitLambdaExpression(LambdaExpression le)                                   { return UnitCompiler.this.getConstantValue2(le);   }
            @Override @Nullable public Object visitMethodReference(MethodReference mr)                                     { return UnitCompiler.this.getConstantValue2(mr);   }
            @Override @Nullable public Object visitInstanceCreationReference(ClassInstanceCreationReference cicr)          { return UnitCompiler.this.getConstantValue2(cicr); }
            @Override @Nullable public Object visitArrayCreationReference(ArrayCreationReference acr)                      { return UnitCompiler.this.getConstantValue2(acr);  }
        }));
    }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(Rvalue rv) { return UnitCompiler.NOT_CONSTANT; }

    @Nullable private Object
    getConstantValue2(AmbiguousName an) throws CompileException {
        return this.getConstantValue(this.toRvalueOrCompileException(this.reclassify(an)));
    }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(FieldAccess fa) throws CompileException {
        return fa.field.getConstantValue();
    }

    @Nullable private Object
    getConstantValue2(UnaryOperation uo) throws CompileException {
        if (uo.operator == "+") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            return this.getConstantValue(uo.operand);
        }
        if (uo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality

            // Handle the super special cases "-2147483648" and "-9223372036854775808L" (JLS9 3.10.1).
            if (uo.operand instanceof IntegerLiteral) {
                String v = ((Literal) uo.operand).value;

                if (UnitCompiler.TWO_E_31_INTEGER.matcher(v).matches()) return new Integer(Integer.MIN_VALUE);
                if (UnitCompiler.TWO_E_63_LONG.matcher(v).matches())    return new Long(Long.MIN_VALUE);
            }

            Object cv = this.getConstantValue(uo.operand);

            if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

            // SUPPRESS CHECKSTYLE DOT__SELECTOR|L_PAREN__METH_INVOCATION:6
            if (cv instanceof Byte)    return Byte   .valueOf((byte)  -((Byte)    cv));
            if (cv instanceof Short)   return Short  .valueOf((short) -((Short)   cv));
            if (cv instanceof Integer) return Integer.valueOf(        -((Integer) cv));
            if (cv instanceof Long)    return Long   .valueOf(        -((Long)    cv));
            if (cv instanceof Float)   return Float  .valueOf(        -((Float)   cv));
            if (cv instanceof Double)  return Double .valueOf(        -((Double)  cv));

            return UnitCompiler.NOT_CONSTANT;
        }

        if (uo.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object cv = this.getConstantValue(uo.operand);
            return (
                cv == Boolean.TRUE  ? Boolean.FALSE :
                cv == Boolean.FALSE ? Boolean.TRUE  :
                UnitCompiler.NOT_CONSTANT
            );
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    /**
     * 2147483648 is the special value that can <em>not</em> be stored in an INT, but <em>its negated value</em>
     * (-2147483648) can.
     */
    private static final Pattern
    TWO_E_31_INTEGER = Pattern.compile("2_*1_*4_*7_*4_*8_*3_*6_*4_*8");

    /**
     * 9223372036854775808 is the special value that can <em>not</em> be stored in a LONG, but <em>its negated
     * value</em> (-9223372036854775808) can.
     */
    private static final Pattern
    TWO_E_63_LONG = Pattern.compile("9_*2_*2_*3_*3_*7_*2_*0_*3_*6_*8_*5_*4_*7_*7_*5_*8_*0_*8[lL]");

    @Nullable private Object
    getConstantValue2(ConditionalExpression ce) throws CompileException {

        Object lhsCv = this.getConstantValue(ce.lhs);
        if (!(lhsCv instanceof Boolean)) return UnitCompiler.NOT_CONSTANT;

        IClass ceType = this.getType2(ce);

        if (!ceType.isPrimitive() && ceType != this.iClassLoader.TYPE_java_lang_String) return UnitCompiler.NOT_CONSTANT;

        if (((Boolean) lhsCv).booleanValue()) {
            this.fakeCompile(ce.rhs);
            return this.getConstantValue(ce.mhs);
        } else {
            this.fakeCompile(ce.mhs);
            return this.getConstantValue(ce.rhs);
        }
    }

    @Nullable private Object
    getConstantValue2(BinaryOperation bo) throws CompileException {

        // "|", "^", "&", "*", "/", "%", "+", "-", "==", "!=".
        if (
            // SUPPRESS CHECKSTYLE StringLiteralEquality:10
            bo.operator == "|"
            || bo.operator == "^"
            || bo.operator == "&"
            || bo.operator == "*"
            || bo.operator == "/"
            || bo.operator == "%"
            || bo.operator == "+"
            || bo.operator == "-"
            || bo.operator == "=="
            || bo.operator == "!="
        ) {

            // Unroll the constant operands.
            List<Object> cvs = new ArrayList<Object>();
            for (Iterator<Rvalue> it = bo.unrollLeftAssociation(); it.hasNext();) {
                Object cv = this.getConstantValue((Rvalue) it.next());
                if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;
                cvs.add(cv);
            }

            // Compute the constant value of the unrolled binary operation.
            Iterator<Object> it  = cvs.iterator();
            Object           lhs = it.next();
            while (it.hasNext()) {
                if (lhs == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

                Object rhs = it.next();

                // String concatenation?
                // SUPPRESS CHECKSTYLE StringLiteralEquality
                if (bo.operator == "+" && (lhs instanceof String || rhs instanceof String)) {
                    StringBuilder sb = new StringBuilder(lhs.toString()).append(rhs);
                    while (it.hasNext()) sb.append(it.next().toString());
                    return sb.toString();
                }

                if (lhs instanceof Number && rhs instanceof Number) {
                    try {
                        if (lhs instanceof Double || rhs instanceof Double) {
                            double lhsD = ((Number) lhs).doubleValue();
                            double rhsD = ((Number) rhs).doubleValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:7
                                bo.operator == "*" ? new Double(lhsD * rhsD) :
                                bo.operator == "/" ? new Double(lhsD / rhsD) :
                                bo.operator == "%" ? new Double(lhsD % rhsD) :
                                bo.operator == "+" ? new Double(lhsD + rhsD) :
                                bo.operator == "-" ? new Double(lhsD - rhsD) :
                                bo.operator == "==" ? Boolean.valueOf(lhsD == rhsD) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsD != rhsD) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (lhs instanceof Float || rhs instanceof Float) {
                            float lhsF = ((Number) lhs).floatValue();
                            float rhsF = ((Number) rhs).floatValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:7
                                bo.operator == "*" ? new Float(lhsF * rhsF) :
                                bo.operator == "/" ? new Float(lhsF / rhsF) :
                                bo.operator == "%" ? new Float(lhsF % rhsF) :
                                bo.operator == "+" ? new Float(lhsF + rhsF) :
                                bo.operator == "-" ? new Float(lhsF - rhsF) :
                                bo.operator == "==" ? Boolean.valueOf(lhsF == rhsF) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsF != rhsF) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (lhs instanceof Long || rhs instanceof Long) {
                            long lhsL = ((Number) lhs).longValue();
                            long rhsL = ((Number) rhs).longValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:10
                                bo.operator == "|" ? new Long(lhsL | rhsL) :
                                bo.operator == "^" ? new Long(lhsL ^ rhsL) :
                                bo.operator == "&" ? new Long(lhsL & rhsL) :
                                bo.operator == "*" ? new Long(lhsL * rhsL) :
                                bo.operator == "/" ? new Long(lhsL / rhsL) :
                                bo.operator == "%" ? new Long(lhsL % rhsL) :
                                bo.operator == "+" ? new Long(lhsL + rhsL) :
                                bo.operator == "-" ? new Long(lhsL - rhsL) :
                                bo.operator == "==" ? Boolean.valueOf(lhsL == rhsL) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsL != rhsL) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                        if (
                            lhs instanceof Integer || lhs instanceof Byte || lhs instanceof Short
                            || rhs instanceof Integer || lhs instanceof Byte || lhs instanceof Short
                        ) {
                            int lhsI = ((Number) lhs).intValue();
                            int rhsI = ((Number) rhs).intValue();
                            lhs = (
                                // SUPPRESS CHECKSTYLE StringLiteralEquality:10
                                bo.operator == "|" ? new Integer(lhsI | rhsI) :
                                bo.operator == "^" ? new Integer(lhsI ^ rhsI) :
                                bo.operator == "&" ? new Integer(lhsI & rhsI) :
                                bo.operator == "*" ? new Integer(lhsI * rhsI) :
                                bo.operator == "/" ? new Integer(lhsI / rhsI) :
                                bo.operator == "%" ? new Integer(lhsI % rhsI) :
                                bo.operator == "+" ? new Integer(lhsI + rhsI) :
                                bo.operator == "-" ? new Integer(lhsI - rhsI) :
                                bo.operator == "==" ? Boolean.valueOf(lhsI == rhsI) :
                                bo.operator == "!=" ? Boolean.valueOf(lhsI != rhsI) :
                                UnitCompiler.NOT_CONSTANT
                            );
                            continue;
                        }
                    } catch (ArithmeticException ae) {

                        // Most likely a divide by zero or modulo by zero. Guess we can't make this expression into a
                        // constant.
                        return UnitCompiler.NOT_CONSTANT;
                    }
                    throw new IllegalStateException();
                }

                if (lhs instanceof Character && rhs instanceof Character) {
                    char lhsC = ((Character) lhs).charValue();
                    char rhsC = ((Character) rhs).charValue();
                    lhs = (
                        bo.operator == "==" ? Boolean.valueOf(lhsC == rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                        bo.operator == "!=" ? Boolean.valueOf(lhsC != rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                        UnitCompiler.NOT_CONSTANT
                    );
                    continue;
                }

                if (lhs == null || rhs == null) {
                    lhs = (
                        bo.operator == "==" ? Boolean.valueOf(lhs == rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        bo.operator == "!=" ? Boolean.valueOf(lhs != rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        UnitCompiler.NOT_CONSTANT
                    );
                    continue;
                }

                return UnitCompiler.NOT_CONSTANT;
            }
            return lhs;
        }

        // "&&" and "||" with constant LHS operand.
        if (bo.operator == "&&" || bo.operator == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsValue = this.getConstantValue(bo.lhs);
            if (lhsValue instanceof Boolean) {
                boolean lhsBv = ((Boolean) lhsValue).booleanValue();
                return (
                    bo.operator == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
                    ? (lhsBv ? this.getConstantValue(bo.rhs) : Boolean.FALSE)
                    : (lhsBv ? Boolean.TRUE : this.getConstantValue(bo.rhs))
                );
            }
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    private Object
    getConstantValue2(Cast c) throws CompileException {
        Object cv = this.getConstantValue(c.value);
        if (cv == UnitCompiler.NOT_CONSTANT) return UnitCompiler.NOT_CONSTANT;

        if (cv instanceof Number) {
            IClass tt = this.getType(c.targetType);
            if (tt == IClass.BYTE)   return new Byte(((Number) cv).byteValue());
            if (tt == IClass.SHORT)  return new Short(((Number) cv).shortValue());
            if (tt == IClass.INT)    return new Integer(((Number) cv).intValue());
            if (tt == IClass.LONG)   return new Long(((Number) cv).longValue());
            if (tt == IClass.FLOAT)  return new Float(((Number) cv).floatValue());
            if (tt == IClass.DOUBLE) return new Double(((Number) cv).doubleValue());
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    @Nullable private Object
    getConstantValue2(ParenthesizedExpression pe) throws CompileException {
        return this.getConstantValue(pe.value);
    }

    private @Nullable Object
    getConstantValue2(LocalVariableAccess lva) throws CompileException {

        // An optimization for the (very special case)
        //
        //    void method()         // Any method declarator
        //        ...
        //        boolean x = true; // Any local variable name allowed; also value "false"
        //        if (x) {          // The condition expression must be exactly like this
        //              ...
        //
        if (lva.getEnclosingScope() instanceof IfStatement) {
            IfStatement is = (IfStatement) lva.getEnclosingScope();

            if (is.condition instanceof AmbiguousName) {
                Atom ra = ((AmbiguousName) is.condition).reclassified;

                if (ra instanceof LocalVariableAccess) {
                    LocalVariable lv = ((LocalVariableAccess) ra).localVariable;

                    List<? extends BlockStatement> ss = (
                        is.getEnclosingScope() instanceof FunctionDeclarator
                        ? ((FunctionDeclarator) is.getEnclosingScope()).statements
                        : is.getEnclosingScope() instanceof Block
                        ? ((Block) is.getEnclosingScope()).statements
                        : null
                    );
                    if (ss != null) {
                        int isi = ss.indexOf(is);
                        if (isi >= 1) {
                            if (ss.get(isi - 1) instanceof LocalVariableDeclarationStatement) {

                                LocalVariableDeclarationStatement
                                lvds = (LocalVariableDeclarationStatement) ss.get(isi - 1);

                                if (
                                    lvds.variableDeclarators.length == 1
                                    && lvds.variableDeclarators[0].localVariable == lv
                                ) {
                                    ArrayInitializerOrRvalue oi = lvds.variableDeclarators[0].initializer;
                                    if (oi instanceof Rvalue) return this.getConstantValue((Rvalue) oi);
                                }
                            }
                        }
                    }
                }
            }
        }

        return UnitCompiler.NOT_CONSTANT;
    }

    /**
     * @return An {@link Integer} or a {@link Long}
     */
    @SuppressWarnings("static-method") private Object
    getConstantValue2(IntegerLiteral il) throws CompileException {

        String v = il.value.toLowerCase();

        // Remove underscores in integer literal (JLS8, section 3.10.1).
        for (;;) {
            int ui = v.indexOf('_');
            if (ui == -1) break;
            v = v.substring(0, ui) + v.substring(ui + 1);
        }

        int     radix;
        boolean signed;

        // HexIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0x")) {
            radix  = 16;
            signed = false;
            v      = v.substring(2);
        } else

        // BinaryIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0b")) {
            radix  = 2;
            signed = false;
            v      = v.substring(2);
        } else

        // OctalIntegerLiteral (JLS8, section 3.10.1)?
        if (v.startsWith("0") && !"0".equals(v) && !"0l".equals(v)) {
            radix  = 8;
            signed = false;
            v      = v.substring(1);
        } else

        // Must be a DecimalIntegerLiteral (JLS8, section 3.10.1).
        {
            radix  = 10;
            signed = true;
        }

        try {

            if (v.endsWith("l")) {
                v = v.substring(0, v.length() - 1);
                return signed ? Long.parseLong(v, radix)   : Numbers.parseUnsignedLong(v, radix);
            } else {
                return signed ? Integer.parseInt(v, radix) : Numbers.parseUnsignedInt(v, radix);
            }
        } catch (NumberFormatException e) {
            // SUPPRESS CHECKSTYLE AvoidHidingCause
            throw UnitCompiler.compileException(il, "Invalid integer literal \"" + il.value + "\"");
        }
    }

    /**
     * @return A {@link Float} or a {@link Double}
     */
    @SuppressWarnings("static-method") private Object
    getConstantValue2(FloatingPointLiteral fpl) throws CompileException {

        String v = fpl.value;

        // Remove underscores in floating point literal.
        for (;;) {
            int ui = v.indexOf('_');
            if (ui == -1) break;
            v = v.substring(0, ui) + v.substring(ui + 1);
        }

        char lastChar = v.charAt(v.length() - 1);
        if (lastChar == 'f' || lastChar == 'F') {
            v = v.substring(0, v.length() - 1);

            float fv;
            try {
                fv = Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new InternalCompilerException("SNO: parsing float literal \"" + v + "\": " + e.getMessage(), e);
            }
            if (Float.isInfinite(fv)) {
                throw UnitCompiler.compileException(fpl, "Value of float literal \"" + v + "\" is out of range");
            }
            if (Float.isNaN(fv)) {
                throw new InternalCompilerException("SNO: parsing float literal \"" + v + "\" results in NaN");
            }

            // Check for FLOAT underrun.
            if (fv == 0.0F) {
                for (int i = 0; i < v.length(); ++i) {
                    char c = v.charAt(i);
                    if ("123456789".indexOf(c) != -1) {
                        throw UnitCompiler.compileException(
                            fpl,
                            "Literal \"" + v + "\" is too small to be represented as a float"
                        );
                    }
                    if (c != '0' && c != '.') break;
                }
            }

            return new Float(fv);
        }

        if (lastChar == 'd' || lastChar == 'D') v = v.substring(0, v.length() - 1);

        double dv;
        try {
            dv = Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new InternalCompilerException("SNO: parsing double literal \"" + v + "\": " + e.getMessage(), e);
        }
        if (Double.isInfinite(dv)) {
            throw UnitCompiler.compileException(fpl, "Value of double literal \"" + v + "\" is out of range");
        }
        if (Double.isNaN(dv)) {
            throw new InternalCompilerException("SNO: parsing double literal \"" + v + "\" results is NaN");
        }

        // Check for DOUBLE underrun.
        if (dv == 0.0F) {
            for (int i = 0; i < v.length(); ++i) {
                char c = v.charAt(i);
                if ("123456789".indexOf(c) != -1) {
                    throw UnitCompiler.compileException(
                        fpl,
                        "Literal \"" + v + "\" is too small to be represented as a double"
                    );
                }
                if (c != '0' && c != '.') break;
            }
        }

        return new Double(dv);
    }

    @SuppressWarnings("static-method") private boolean
    getConstantValue2(BooleanLiteral bl) {
        if (bl.value == "true")  return true;  // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (bl.value == "false") return false; // SUPPRESS CHECKSTYLE StringLiteralEquality
        throw new InternalCompilerException(bl.value);
    }

    @SuppressWarnings("static-method") private char
    getConstantValue2(CharacterLiteral cl) throws CompileException {

        String v = cl.value;

        // Strip opening and closing single quotes.
        v = v.substring(1, v.length() - 1);

        // Decode escape sequences like "\n" and "\0377".
        v = UnitCompiler.unescape(v, cl.getLocation());

        if (v.isEmpty()) throw new CompileException("Empty character literal", cl.getLocation());

        if (v.length() > 1) throw new CompileException("Invalid character literal " + cl.value, cl.getLocation());

        return Character.valueOf(v.charAt(0));
    }

    @SuppressWarnings("static-method") private String
    getConstantValue2(StringLiteral sl) throws CompileException {

        String v = sl.value;

        // Strip opening and closing double quotes.
        v = v.substring(1, v.length() - 1);

        // Decode escape sequences like "\n" and "\0377".
        v = UnitCompiler.unescape(v, sl.getLocation());

        return v;
    }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(NullLiteral nl) { return null; }

    @SuppressWarnings("static-method")
    @Nullable private Object
    getConstantValue2(SimpleConstant sl) { return sl.value; }

    // ------------ BlockStatement.generatesCode() -------------

    /**
     * Checks whether invocation of {@link #compile(BlockStatement)} would generate more than zero code bytes.
     */
    private boolean
    generatesCode(BlockStatement bs) throws CompileException {

        Boolean result = (Boolean) bs.accept(new BlockStatementVisitor<Boolean, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLengthCheck:23
            @Override public Boolean visitInitializer(Initializer i)                        throws CompileException { return UnitCompiler.this.generatesCode2(i);    }
            @Override public Boolean visitFieldDeclaration(FieldDeclaration fd)             throws CompileException { return UnitCompiler.this.generatesCode2(fd);   }
            @Override public Boolean visitLabeledStatement(LabeledStatement ls)                                     { return UnitCompiler.this.generatesCode2(ls);   }
            @Override public Boolean visitBlock(Block b)                                    throws CompileException { return UnitCompiler.this.generatesCode2(b);    }
            @Override public Boolean visitExpressionStatement(ExpressionStatement es)                               { return UnitCompiler.this.generatesCode2(es);   }
            @Override public Boolean visitIfStatement(IfStatement is)                                               { return UnitCompiler.this.generatesCode2(is);   }
            @Override public Boolean visitForStatement(ForStatement fs)                                             { return UnitCompiler.this.generatesCode2(fs);   }
            @Override public Boolean visitForEachStatement(ForEachStatement fes)                                    { return UnitCompiler.this.generatesCode2(fes);  }
            @Override public Boolean visitWhileStatement(WhileStatement ws)                                         { return UnitCompiler.this.generatesCode2(ws);   }
            @Override public Boolean visitTryStatement(TryStatement ts)                                             { return UnitCompiler.this.generatesCode2(ts);   }
            @Override public Boolean visitSwitchStatement(SwitchStatement ss)                                       { return UnitCompiler.this.generatesCode2(ss);   }
            @Override public Boolean visitSynchronizedStatement(SynchronizedStatement ss)                           { return UnitCompiler.this.generatesCode2(ss);   }
            @Override public Boolean visitDoStatement(DoStatement ds)                                               { return UnitCompiler.this.generatesCode2(ds);   }
            @Override public Boolean visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { return UnitCompiler.this.generatesCode2(lvds); }
            @Override public Boolean visitReturnStatement(ReturnStatement rs)                                       { return UnitCompiler.this.generatesCode2(rs);   }
            @Override public Boolean visitThrowStatement(ThrowStatement ts)                                         { return UnitCompiler.this.generatesCode2(ts);   }
            @Override public Boolean visitBreakStatement(BreakStatement bs)                                         { return UnitCompiler.this.generatesCode2(bs);   }
            @Override public Boolean visitContinueStatement(ContinueStatement cs)                                   { return UnitCompiler.this.generatesCode2(cs);   }
            @Override public Boolean visitAssertStatement(AssertStatement as)                                       { return UnitCompiler.this.generatesCode2(as);   }
            @Override public Boolean visitEmptyStatement(EmptyStatement es)                                         { return UnitCompiler.this.generatesCode2(es);   }
            @Override public Boolean visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { return UnitCompiler.this.generatesCode2(lcds); }
            @Override public Boolean visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { return UnitCompiler.this.generatesCode2(aci);  }
            @Override public Boolean visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { return UnitCompiler.this.generatesCode2(sci);  }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(BlockStatement bs) { return true; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(AssertStatement as) { return true; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(EmptyStatement es) { return false; }

    @SuppressWarnings("static-method") private boolean
    generatesCode2(LocalClassDeclarationStatement lcds) { return false; }

    private boolean
    generatesCode2(Initializer i) throws CompileException { return this.generatesCode(i.block); }

    private boolean
    generatesCode2(List<BlockStatement> l) throws CompileException {
        for (BlockStatement bs : l) if (this.generatesCode(bs)) return true;
        return false;
    }

    private boolean
    generatesCode2(Block b) throws CompileException { return this.generatesCode2(b.statements); }

    private boolean
    generatesCode2(FieldDeclaration fd) throws CompileException {
        // Code is only generated if at least one of the declared variables has a non-constant-final initializer.
        for (VariableDeclarator vd : fd.variableDeclarators) {
            if (this.getNonConstantFinalInitializer(fd, vd) != null) return true;
        }
        return false;
    }

    // ------------ BlockStatement.leave() -------------

    /**
     * Cleans up the statement context. This is currently relevant for "{@code try ... catch ... finally}" statements
     * (execute {@code finally} clause) and {@code synchronized} statements (monitorexit).
     * <p>
     *   Statements like {@code return}, {@code break}, {@code continue} must call this method for all the statements
     *   they terminate.
     * </p>
     */
    private void
    leave(BlockStatement bs) throws CompileException {
        BlockStatementVisitor<Void, CompileException> bsv = new BlockStatementVisitor<Void, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLengthCheck:23
            @Override @Nullable public Void visitInitializer(Initializer i)                                                { UnitCompiler.this.leave2(i);    return null; }
            @Override @Nullable public Void visitFieldDeclaration(FieldDeclaration fd)                                     { UnitCompiler.this.leave2(fd);   return null; }
            @Override @Nullable public Void visitLabeledStatement(LabeledStatement ls)                                     { UnitCompiler.this.leave2(ls);   return null; }
            @Override @Nullable public Void visitBlock(Block b)                                                            { UnitCompiler.this.leave2(b);    return null; }
            @Override @Nullable public Void visitExpressionStatement(ExpressionStatement es)                               { UnitCompiler.this.leave2(es);   return null; }
            @Override @Nullable public Void visitIfStatement(IfStatement is)                                               { UnitCompiler.this.leave2(is);   return null; }
            @Override @Nullable public Void visitForStatement(ForStatement fs)                                             { UnitCompiler.this.leave2(fs);   return null; }
            @Override @Nullable public Void visitForEachStatement(ForEachStatement fes)                                    { UnitCompiler.this.leave2(fes);  return null; }
            @Override @Nullable public Void visitWhileStatement(WhileStatement ws)                                         { UnitCompiler.this.leave2(ws);   return null; }
            @Override @Nullable public Void visitTryStatement(TryStatement ts) throws CompileException                     { UnitCompiler.this.leave2(ts);   return null; }
            @Override @Nullable public Void visitSwitchStatement(SwitchStatement ss)                                       { UnitCompiler.this.leave2(ss);   return null; }
            @Override @Nullable public Void visitSynchronizedStatement(SynchronizedStatement ss)                           { UnitCompiler.this.leave2(ss);   return null; }
            @Override @Nullable public Void visitDoStatement(DoStatement ds)                                               { UnitCompiler.this.leave2(ds);   return null; }
            @Override @Nullable public Void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { UnitCompiler.this.leave2(lvds); return null; }
            @Override @Nullable public Void visitReturnStatement(ReturnStatement rs)                                       { UnitCompiler.this.leave2(rs);   return null; }
            @Override @Nullable public Void visitThrowStatement(ThrowStatement ts)                                         { UnitCompiler.this.leave2(ts);   return null; }
            @Override @Nullable public Void visitBreakStatement(BreakStatement bs)                                         { UnitCompiler.this.leave2(bs);   return null; }
            @Override @Nullable public Void visitContinueStatement(ContinueStatement cs)                                   { UnitCompiler.this.leave2(cs);   return null; }
            @Override @Nullable public Void visitAssertStatement(AssertStatement as)                                       { UnitCompiler.this.leave2(as);   return null; }
            @Override @Nullable public Void visitEmptyStatement(EmptyStatement es)                                         { UnitCompiler.this.leave2(es);   return null; }
            @Override @Nullable public Void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { UnitCompiler.this.leave2(lcds); return null; }
            @Override @Nullable public Void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { UnitCompiler.this.leave2(aci);  return null; }
            @Override @Nullable public Void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { UnitCompiler.this.leave2(sci);  return null; }
        };
        bs.accept(bsv);
    }

    private void
    leave2(BlockStatement bs) {}

    private void
    leave2(SynchronizedStatement ss) {
        this.load(ss, this.iClassLoader.TYPE_java_lang_Object, ss.monitorLvIndex);
        this.monitorexit(ss);
    }

    private void
    leave2(TryStatement ts) throws CompileException {

        Block f = ts.finallY;
        if (f == null) return;

        this.getCodeContext().saveLocalVariables();
        try {
            if (this.compile(f)) return;
        } finally {
            this.getCodeContext().restoreLocalVariables();
        }
    }

    // ---------------- Lvalue.compileSet() -----------------

    /**
     * Generates code that stores a value in the {@link Lvalue}. Expects the {@link Lvalue}'s context (see {@link
     * #compileContext}) and a value of the {@link Lvalue}'s type on the operand stack.
     */
    private void
    compileSet(Lvalue lv) throws CompileException {

        lv.accept(new LvalueVisitor<Void, CompileException>() {

            // SUPPRESS CHECKSTYLE LineLength:7
            @Override @Nullable public Void visitAmbiguousName(AmbiguousName an)                                        throws CompileException { UnitCompiler.this.compileSet2(an);    return null; }
            @Override @Nullable public Void visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { UnitCompiler.this.compileSet2(aae);   return null; }
            @Override @Nullable public Void visitFieldAccess(FieldAccess fa)                                            throws CompileException { UnitCompiler.this.compileSet2(fa);    return null; }
            @Override @Nullable public Void visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { UnitCompiler.this.compileSet2(fae);   return null; }
            @Override @Nullable public Void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { UnitCompiler.this.compileSet2(scfae); return null; }
            @Override @Nullable public Void visitLocalVariableAccess(LocalVariableAccess lva)                                                   { UnitCompiler.this.compileSet2(lva);   return null; }
            @Override @Nullable public Void visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { UnitCompiler.this.compileSet2(pe);    return null; }
        });
    }
    private void
    compileSet2(AmbiguousName an) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(this.reclassify(an)));
    }

    private void
    compileSet2(LocalVariableAccess lva) { this.store(lva, lva.localVariable); }

    private void
    compileSet2(FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.getEnclosingScope(), fa.getLocation());
        this.putfield(fa, fa.field);
    }
    private void
    compileSet2(ArrayAccessExpression aae) throws CompileException {
        this.arraystore(aae, this.getType(aae));
    }
    private void
    compileSet2(FieldAccessExpression fae) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(this.determineValue(fae)));
    }
    private void
    compileSet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        this.compileSet(this.toLvalueOrCompileException(this.determineValue(scfae)));
    }
    private void
    compileSet2(ParenthesizedExpression pe) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(pe.value));
    }

    // ---------------- Atom.getType() ----------------

    private IClass
    getType(Atom a) throws CompileException {

        IClass result = (IClass) a.accept(new AtomVisitor<IClass, CompileException>() {

            @Override public IClass
            visitPackage(Package p) throws CompileException { return UnitCompiler.this.getType2(p); }

            @Override @Nullable public IClass
            visitType(Type t) throws CompileException {
                return (IClass) t.accept(new Visitor.TypeVisitor<IClass, CompileException>() {

                    // SUPPRESS CHECKSTYLE LineLengthCheck:5
                    @Override public IClass visitArrayType(ArrayType at)                throws CompileException { return UnitCompiler.this.getType2(at);  }
                    @Override public IClass visitPrimitiveType(PrimitiveType bt)                                { return UnitCompiler.this.getType2(bt);  }
                    @Override public IClass visitReferenceType(ReferenceType rt)        throws CompileException { return UnitCompiler.this.getType2(rt);  }
                    @Override public IClass visitRvalueMemberType(RvalueMemberType rmt) throws CompileException { return UnitCompiler.this.getType2(rmt); }
                    @Override public IClass visitSimpleType(SimpleType st)                                      { return UnitCompiler.this.getType2(st);  }
                });
            }

            @Override @Nullable public IClass
            visitRvalue(Rvalue rv) throws CompileException {

                return (IClass) rv.accept(new Visitor.RvalueVisitor<IClass, CompileException>() {

                    @Override @Nullable public IClass
                    visitLvalue(Lvalue lv) throws CompileException {
                        return (IClass) lv.accept(new Visitor.LvalueVisitor<IClass, CompileException>() {

                            // SUPPRESS CHECKSTYLE LineLengthCheck:7
                            @Override public IClass visitAmbiguousName(AmbiguousName an)                                        throws CompileException { return UnitCompiler.this.getType2(an);    }
                            @Override public IClass visitArrayAccessExpression(ArrayAccessExpression aae)                       throws CompileException { return UnitCompiler.this.getType2(aae);   }
                            @Override public IClass visitFieldAccess(FieldAccess fa)                                            throws CompileException { return UnitCompiler.this.getType2(fa);    }
                            @Override public IClass visitFieldAccessExpression(FieldAccessExpression fae)                       throws CompileException { return UnitCompiler.this.getType2(fae);   }
                            @Override public IClass visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) throws CompileException { return UnitCompiler.this.getType2(scfae); }
                            @Override public IClass visitLocalVariableAccess(LocalVariableAccess lva)                                                   { return UnitCompiler.this.getType2(lva);   }
                            @Override public IClass visitParenthesizedExpression(ParenthesizedExpression pe)                    throws CompileException { return UnitCompiler.this.getType2(pe);    }
                        });
                    }

                    // SUPPRESS CHECKSTYLE LineLengthCheck:29
                    @Override public IClass visitArrayLength(ArrayLength al)                                                            { return UnitCompiler.this.getType2(al);   }
                    @Override public IClass visitAssignment(Assignment a)                                       throws CompileException { return UnitCompiler.this.getType2(a);    }
                    @Override public IClass visitUnaryOperation(UnaryOperation uo)                              throws CompileException { return UnitCompiler.this.getType2(uo);   }
                    @Override public IClass visitBinaryOperation(BinaryOperation bo)                            throws CompileException { return UnitCompiler.this.getType2(bo);   }
                    @Override public IClass visitCast(Cast c)                                                   throws CompileException { return UnitCompiler.this.getType2(c);    }
                    @Override public IClass visitClassLiteral(ClassLiteral cl)                                                          { return UnitCompiler.this.getType2(cl);   }
                    @Override public IClass visitConditionalExpression(ConditionalExpression ce)                throws CompileException { return UnitCompiler.this.getType2(ce);   }
                    @Override public IClass visitCrement(Crement c)                                             throws CompileException { return UnitCompiler.this.getType2(c);    }
                    @Override public IClass visitInstanceof(Instanceof io)                                                              { return UnitCompiler.this.getType2(io);   }
                    @Override public IClass visitMethodInvocation(MethodInvocation mi)                          throws CompileException { return UnitCompiler.this.getType2(mi);   }
                    @Override public IClass visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     throws CompileException { return UnitCompiler.this.getType2(smi);  }
                    @Override public IClass visitIntegerLiteral(IntegerLiteral il)                                                      { return UnitCompiler.this.getType2(il);   }
                    @Override public IClass visitFloatingPointLiteral(FloatingPointLiteral fpl)                                         { return UnitCompiler.this.getType2(fpl);  }
                    @Override public IClass visitBooleanLiteral(BooleanLiteral bl)                                                      { return UnitCompiler.this.getType2(bl);   }
                    @Override public IClass visitCharacterLiteral(CharacterLiteral cl)                                                  { return UnitCompiler.this.getType2(cl);   }
                    @Override public IClass visitStringLiteral(StringLiteral sl)                                                        { return UnitCompiler.this.getType2(sl);   }
                    @Override public IClass visitNullLiteral(NullLiteral nl)                                                            { return UnitCompiler.this.getType2(nl);   }
                    @Override public IClass visitSimpleConstant(SimpleConstant sl)                                                      { return UnitCompiler.this.getType2(sl);   }
                    @Override public IClass visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)                              { return UnitCompiler.this.getType2(naci); }
                    @Override public IClass visitNewArray(NewArray na)                                          throws CompileException { return UnitCompiler.this.getType2(na);   }
                    @Override public IClass visitNewInitializedArray(NewInitializedArray nia)                   throws CompileException { return UnitCompiler.this.getType2(nia);  }
                    @Override public IClass visitNewClassInstance(NewClassInstance nci)                         throws CompileException { return UnitCompiler.this.getType2(nci);  }
                    @Override public IClass visitParameterAccess(ParameterAccess pa)                            throws CompileException { return UnitCompiler.this.getType2(pa);   }
                    @Override public IClass visitQualifiedThisReference(QualifiedThisReference qtr)             throws CompileException { return UnitCompiler.this.getType2(qtr);  }
                    @Override public IClass visitThisReference(ThisReference tr)                                throws CompileException { return UnitCompiler.this.getType2(tr);   }
                    @Override public IClass visitLambdaExpression(LambdaExpression le)                          throws CompileException { return UnitCompiler.this.getType2(le);   }
                    @Override public IClass visitMethodReference(MethodReference mr)                            throws CompileException { return UnitCompiler.this.getType2(mr);   }
                    @Override public IClass visitInstanceCreationReference(ClassInstanceCreationReference cicr) throws CompileException { return UnitCompiler.this.getType2(cicr); }
                    @Override public IClass visitArrayCreationReference(ArrayCreationReference acr)             throws CompileException { return UnitCompiler.this.getType2(acr);  }
                });
            }

            @Override @Nullable public IClass
            visitConstructorInvocation(ConstructorInvocation ci) throws CompileException {
                return UnitCompiler.this.getType2(ci);
            }
        });

        assert result != null;
        return result;
    }

    private IClass
    getType2(ConstructorInvocation ci) throws CompileException {
        this.compileError("Explicit constructor invocation not allowed here", ci.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(SimpleType st) { return st.iClass; }

    @SuppressWarnings("static-method") private IClass
    getType2(PrimitiveType bt) {
        switch (bt.primitive) {
        case VOID:    return IClass.VOID;
        case BYTE:    return IClass.BYTE;
        case SHORT:   return IClass.SHORT;
        case CHAR:    return IClass.CHAR;
        case INT:     return IClass.INT;
        case LONG:    return IClass.LONG;
        case FLOAT:   return IClass.FLOAT;
        case DOUBLE:  return IClass.DOUBLE;
        case BOOLEAN: return IClass.BOOLEAN;
        default:      throw new InternalCompilerException("Invalid primitive " + bt.primitive);
        }
    }
    private IClass
    getType2(ReferenceType rt) throws CompileException {
        String[] identifiers = rt.identifiers;

        IClass result = this.getReferenceType(
            rt.getLocation(),
            rt.getEnclosingScope(),
            identifiers,
            identifiers.length
        );
        if (result == null) {
            this.compileError("Reference type \"" + rt + "\" not found", rt.getLocation());
            return this.iClassLoader.TYPE_java_lang_Object;
        }

        return result;
    }

    /**
     * @return The resolved {@link IClass}, or {@code null}
     */
    @Nullable private IClass
    getReferenceType(Location location, Scope scope, String[] identifiers, int n) throws CompileException {

        if (n == 1) {
            return this.getReferenceType(location, identifiers[0], scope);
        }

        // JLS7 6.5.5.1   Unnamed package member type name (one identifier).
        // JLS7 6.5.5.2.1 Qualified type name (two or more identifiers).
        {
            String className = Java.join(identifiers, ".", 0, n);
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return result;
        }

        // JLS7 6.5.5.2.2 referenceType '.' memberTypeName
        if (n >= 2) {
            IClass enclosingType = this.getReferenceType(location, scope, identifiers, n - 1);
            if (enclosingType != null) {
                String memberTypeName = identifiers[n - 1];
                IClass memberType     = this.findMemberType(enclosingType, memberTypeName, location);
                if (memberType == null) {
                    this.compileError(
                        "\"" + enclosingType + "\" declares no member type \"" + memberTypeName + "\"",
                        location
                    );
                    return this.iClassLoader.TYPE_java_lang_Object;
                }
                return memberType;
            }
        }

        return null;
    }

    /**
     * JLS7 6.5.5.1 Simple type name (single identifier)
     *
     * @return The resolved {@link IClass}
     */
    private IClass
    getReferenceType(Location location, String simpleTypeName, Scope scope) throws CompileException {

        if ("var".equals(simpleTypeName)) {
            this.compileError("Local variable type inference NYI", location);
            return this.iClassLoader.TYPE_java_lang_Object;
        }

        // Method declaration type parameter?
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (!(s instanceof MethodDeclarator)) continue;
            MethodDeclarator md = (MethodDeclarator) s;

            TypeParameter[] typeParameters = md.getOptionalTypeParameters();
            if (typeParameters != null) {
                for (TypeParameter tp : typeParameters) {
                    if (tp.name.equals(simpleTypeName)) {
                        IClass[]        boundTypes;
                        ReferenceType[] ob = tp.bound;
                        if (ob == null) {
                            boundTypes = new IClass[] { this.iClassLoader.TYPE_java_lang_Object };
                        } else {
                            boundTypes = new IClass[ob.length];
                            for (int i = 0; i < boundTypes.length; i++) {
                                boundTypes[i] = this.getType(ob[i]);
                            }
                        }

                        // Here is the big simplification: Instead of returning the "correct" type, honoring type
                        // arguments, we simply return the first bound. E.g. "Map.get(K)" returns a "V", but
                        // JANINO says it's an "Object" (the implicit bound of "V").
                        return boundTypes[0];
                    }
                }
            }
        }

        // Type declaration type parameter?
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (!(s instanceof NamedTypeDeclaration)) continue;
            NamedTypeDeclaration ntd = (NamedTypeDeclaration) s;

            TypeParameter[] typeParameters = ntd.getOptionalTypeParameters();
            if (typeParameters != null) {
                for (TypeParameter tp : typeParameters) {
                    if (tp.name.equals(simpleTypeName)) {
                        IClass[]        boundTypes;
                        ReferenceType[] ob = tp.bound;
                        if (ob == null) {
                            boundTypes = new IClass[] { this.iClassLoader.TYPE_java_lang_Object };
                        } else {
                            boundTypes = new IClass[ob.length];
                            for (int i = 0; i < boundTypes.length; i++) {
                                boundTypes[i] = this.getType(ob[i]);
                            }
                        }
                        return boundTypes[0];
                    }
                }
            }
        }

        // 6.5.5.1.1 Local class.
        {
            LocalClassDeclaration lcd = UnitCompiler.findLocalClassDeclaration(
                scope,
                simpleTypeName
            );
            if (lcd != null) return this.resolve(lcd);
        }

        // 6.5.5.1.2 Member type.
        for (
            Scope s = scope;
            !(s instanceof CompilationUnit);
            s = s.getEnclosingScope()
        ) {
            if (s instanceof TypeDeclaration) {
                IClass mt = this.findMemberType(
                    this.resolve((AbstractTypeDeclaration) s),
                    simpleTypeName,
                    location
                );
                if (mt != null) return mt;
            }
        }

        // 6.5.5.1.4a Single-type import.
        {
            IClass importedClass = this.importSingleType(simpleTypeName, location);
            if (importedClass != null) return importedClass;
        }

        CompilationUnit  scopeCompilationUnit;
        for (Scope s = scope;; s = s.getEnclosingScope()) {
            if (s instanceof CompilationUnit) {
                scopeCompilationUnit = (CompilationUnit) s;
                break;
            }
        }

        // 6.5.5.1.4b Type declared in same compilation unit.
        {
            PackageMemberTypeDeclaration pmtd = (
                scopeCompilationUnit.getPackageMemberTypeDeclaration(simpleTypeName)
            );
            if (pmtd != null) return this.resolve(pmtd);
        }

        // 6.5.5.1.5 Type declared in other compilation unit of same package.
        {
            PackageDeclaration opd = scopeCompilationUnit.packageDeclaration;

            String pkg       = opd == null ? null : opd.packageName;
            String className = pkg == null ? simpleTypeName : pkg + "." + simpleTypeName;
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return result;
        }

        // 6.5.5.1.6 Type-import-on-demand declaration.
        {
            IClass importedClass = this.importTypeOnDemand(simpleTypeName, location);
            if (importedClass != null) return importedClass;
        }

        // JLS7 6.5.2.BL1.B2: Type imported through single import.
        {
            IClass importedIClass = this.importSingleType(simpleTypeName, location);
            if (importedIClass != null) {
                if (!this.isAccessible(importedIClass, scope)) {
                    this.compileError("Member type \"" + simpleTypeName + "\" is not accessible", location);
                }
                return importedIClass;
            }
        }

        // JLS7 6.5.2.BL1.B2: Member type imported through single static import.
        {
            IClass importedMemberType = null;
            for (IClass mt : Iterables.filterByClass(this.importSingleStatic(simpleTypeName), IClass.class)) {

                if (importedMemberType != null && mt != importedMemberType) {
                    this.compileError(
                        "Ambiguous static member type import: \""
                        + importedMemberType.toString()
                        + "\" vs. \""
                        + mt
                        + "\""
                    );
                }
                importedMemberType = mt;
            }
            if (importedMemberType != null) return importedMemberType;
        }

        // JLS7 6.5.2.BL1.B2: Member type imported through static-import-on-demand.
        {
            Iterator<IClass>
            it = Iterables.filterByClass(this.importStaticOnDemand(simpleTypeName).iterator(), IClass.class);
            if (it.hasNext()) return (IClass) it.next();
        }

        // Unnamed package member type.
        {
            IClass result = this.findTypeByName(location, simpleTypeName);
            if (result != null) return result;
        }

        // Type argument of the enclosing anonymous class declaration?
        for (
            Scope s = scope;
            !(s instanceof CompilationUnit);
            s = s.getEnclosingScope()
        ) {
            if (!(s instanceof AnonymousClassDeclaration)) continue;
            AnonymousClassDeclaration acd = (AnonymousClassDeclaration) s;

            Type bt = acd.baseType;
            if (bt instanceof ReferenceType) {
                TypeArgument[] otas = ((ReferenceType) bt).typeArguments;
                if (otas != null) {
                    for (TypeArgument ta : otas) {
                        if (ta instanceof ReferenceType) {
                            String[] is = ((ReferenceType) ta).identifiers;
                            if (is.length == 1 && is[0].equals(simpleTypeName)) {
                                return this.iClassLoader.TYPE_java_lang_Object;
                            }
                        }
                    }
                }
            }
        }

        // 6.5.5.1.8 Give up.
        this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"", location);
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    /**
     * Imports a member class or interface, static field or static method via the compilation unit's static import
     * on-demand declarations.
     *
     * @return A list of {@link IField}s, {@link IMethod}s and/or {@link IClass}es with that <var>simpleName</var>;
     *         may be empty
     */
    private List<Object>
    importStaticOnDemand(String simpleName) throws CompileException {

        List<Object> result = new ArrayList<Object>();
        for (StaticImportOnDemandDeclaration siodd : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            StaticImportOnDemandDeclaration.class
        )) {

            IClass iClass = this.findTypeByFullyQualifiedName(siodd.getLocation(), siodd.identifiers);
            if (iClass == null) {
                this.compileError("Could not load \"" + Java.join(siodd.identifiers, ".") + "\"", siodd.getLocation());
                continue;
            }

            this.importStatic(iClass, simpleName, result, siodd.getLocation());
        }

        return result;
    }

    private IClass
    getType2(RvalueMemberType rvmt) throws CompileException {
        IClass rvt        = this.getType(rvmt.rvalue);
        IClass memberType = this.findMemberType(rvt, rvmt.identifier, rvmt.getLocation());
        if (memberType == null) {
            this.compileError("\"" + rvt + "\" has no member type \"" + rvmt.identifier + "\"", rvmt.getLocation());
            return this.iClassLoader.TYPE_java_lang_Object;
        }
        return memberType;
    }

    private IClass
    getType2(ArrayType at) throws CompileException {
        return this.getType(at.componentType).getArrayIClass(this.iClassLoader.TYPE_java_lang_Object);
    }

    private IClass
    getType2(AmbiguousName an) throws CompileException {
        return this.getType(this.reclassify(an));
    }

    private IClass
    getType2(Package p) throws CompileException {
        this.compileError("Unknown variable or type \"" + p.name + "\"", p.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    @SuppressWarnings("static-method")
    private IClass
    getType2(LocalVariableAccess lva) {
        return lva.localVariable.type;
    }

    @SuppressWarnings("static-method")
    private IClass
    getType2(FieldAccess fa) throws CompileException {
        return fa.field.getType();
    }

    @SuppressWarnings("static-method")
    private IClass
    getType2(ArrayLength al) {
        return IClass.INT;
    }

    private IClass
    getType2(ThisReference tr) throws CompileException {
        return this.getIClass(tr);
    }

    @SuppressWarnings("static-method") private IClass
    getType2(LambdaExpression le) throws CompileException {
        throw UnitCompiler.compileException(le, "Compilation of lambda expression NYI");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(MethodReference mr) throws CompileException {
        throw UnitCompiler.compileException(mr, "Compilation of method reference NYI");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ClassInstanceCreationReference cicr) throws CompileException {
        throw UnitCompiler.compileException(cicr, "Compilation of class instance creation reference NYI");
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ArrayCreationReference acr) throws CompileException {
        throw UnitCompiler.compileException(acr, "Compilation of array creation reference NYI");
    }

    private IClass
    getType2(QualifiedThisReference qtr) throws CompileException {
        return this.getTargetIClass(qtr);
    }

    private IClass
    getType2(ClassLiteral cl) {
        return this.iClassLoader.TYPE_java_lang_Class;
    }

    private IClass
    getType2(Assignment a) throws CompileException {
        return this.getType(a.lhs);
    }

    private IClass
    getType2(ConditionalExpression ce) throws CompileException {
        IClass mhsType = this.getType(ce.mhs);
        IClass rhsType = this.getType(ce.rhs);

        if (mhsType == rhsType) {

            // JLS7 15.25, list 1, bullet 1: "b ? T : T => T"
            return mhsType;
        } else
        if (this.isUnboxingConvertible(mhsType) == rhsType) {

            // JLS7 15.25, list 1, bullet 2: "b ? Integer : int => int"
            return rhsType;
        } else
        if (this.isUnboxingConvertible(rhsType) == mhsType) {

            // JLS7 15.25, list 1, bullet 2: "b ? int : Integer => int"
            return mhsType;
        } else
        if (this.getConstantValue(ce.mhs) == null && !rhsType.isPrimitive()) {

            // JLS7 15.25, list 1, bullet 3: "b ? null : String => String"
            return rhsType;
        } else
        if (this.getConstantValue(ce.mhs) == null && this.isBoxingConvertible(rhsType) != null) {

            // Undocumented JAVAC feature: "b ? null : 7 => Integer"
            IClass result = this.isBoxingConvertible(rhsType);
            assert result != null;
            return result;
        } else
        if (!mhsType.isPrimitive() && this.getConstantValue(ce.rhs) == null) {

            // JLS7 15.25, list 1, bullet 3: "b ? String : null => String"
            return mhsType;
        } else
        if (this.isBoxingConvertible(mhsType) != null && this.getConstantValue(ce.rhs) == null) {

            // Undocumented JAVAC feature: "b ? 7 : null => Integer"
            IClass result = this.isBoxingConvertible(mhsType);
            assert result != null;
            return result;
        } else
        if (this.isConvertibleToPrimitiveNumeric(mhsType) && this.isConvertibleToPrimitiveNumeric(rhsType)) {

            // JLS7 15.25, list 1, bullet 4, bullet 1: "b ? Byte : Short => short"
            if (
                (mhsType == IClass.BYTE || mhsType == this.iClassLoader.TYPE_java_lang_Byte)
                && (rhsType == IClass.SHORT || rhsType == this.iClassLoader.TYPE_java_lang_Short)
            ) return IClass.SHORT;
            if (
                (rhsType == IClass.BYTE || rhsType == this.iClassLoader.TYPE_java_lang_Byte)
                && (mhsType == IClass.SHORT || mhsType == this.iClassLoader.TYPE_java_lang_Short)
            ) return IClass.SHORT;

            // JLS7 15.25, list 1, bullet 4, bullet 2: "b ? (byte) 1 : byte => byte"
            Object rhscv = this.getConstantValue(ce.rhs);
            if (
                (mhsType == IClass.BYTE || mhsType == IClass.SHORT || mhsType == IClass.CHAR)
                && rhscv != null
                && this.assignmentConversion(ce.rhs, rhscv, mhsType) != null
            ) return mhsType;
            Object mhscv = this.getConstantValue(ce.mhs);
            if (
                (rhsType == IClass.BYTE || rhsType == IClass.SHORT || rhsType == IClass.CHAR)
                && mhscv != null
                && this.assignmentConversion(ce.mhs, mhscv, rhsType) != null
            ) return rhsType;

            // JLS7 15.25, list 1, bullet 4, bullet 3: "b ? 127 : byte => byte"
            if (
                mhsType == IClass.INT
                && rhsType == IClass.BYTE
                && UnitCompiler.isByteConstant(mhscv) != null
            ) {

                // Fix up the constant to be a byte
                ce.mhs.constantValue = UnitCompiler.isByteConstant(mhscv);
                return IClass.BYTE;
            }
            if (
                rhsType == IClass.INT
                && mhsType == IClass.BYTE
                && UnitCompiler.isByteConstant(rhscv) != null
            ) {

                // Fix up the constant to be a byte
                ce.rhs.constantValue = UnitCompiler.isByteConstant(rhscv);
                return IClass.BYTE;
            }

            // JLS7 15.25, list 1, bullet 4, bullet 4: "b ? Integer : Double => double"
            return this.binaryNumericPromotionType(
                ce,
                this.getUnboxedType(mhsType),
                this.getUnboxedType(rhsType)
            );
        }

        if (!mhsType.isPrimitive() || !rhsType.isPrimitive()) {

            // JLS7 15.25, list 1, bullet 5: "b ? Base : Derived => Base"
//            mhsType = (IClass) Objects.or(this.isBoxingConvertible(mhsType), mhsType);
//            rhsType = (IClass) Objects.or(this.isBoxingConvertible(rhsType), rhsType);
            return this.commonSupertype(mhsType, rhsType);
        } else
        {
            this.compileError(
                "Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"",
                ce.getLocation()
            );
            return this.iClassLoader.TYPE_java_lang_Object;
        }
    }

    private IClass
    getType2(Crement c) throws CompileException {
        return this.getType(c.operand);
    }

    private IClass
    getType2(ArrayAccessExpression aae) throws CompileException {
        IClass componentType = this.getType(aae.lhs).getComponentType();
        assert componentType != null : "null component type for " + aae;
        return componentType;
    }

    private IClass
    getType2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.getType(this.determineValue(fae));
    }

    private IClass
    getType2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.getType(this.determineValue(scfae));
    }

    private IClass
    getType2(UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") return IClass.BOOLEAN; // SUPPRESS CHECKSTYLE StringLiteralEquality

         // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (uo.operator == "+" || uo.operator == "-" || uo.operator == "~") {
            return this.unaryNumericPromotionType(uo, this.getUnboxedType(this.getType(uo.operand)));
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return IClass.BOOLEAN;
    }

    @SuppressWarnings("static-method")
    private IClass
    getType2(Instanceof io) { return IClass.BOOLEAN; }

    private IClass
    getType2(BinaryOperation bo) throws CompileException {
        if (
            // SUPPRESS CHECKSTYLE StringLiteralEquality:8
            bo.operator == "||"
            || bo.operator == "&&"
            || bo.operator == "=="
            || bo.operator == "!="
            || bo.operator == "<"
            || bo.operator == ">"
            || bo.operator == "<="
            || bo.operator == ">="
        ) return IClass.BOOLEAN;

        if (bo.operator == "|" || bo.operator == "^" || bo.operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
            IClass lhsType = this.getType(bo.lhs);
            return (
                lhsType == IClass.BOOLEAN || lhsType == this.iClassLoader.TYPE_java_lang_Boolean
                ? IClass.BOOLEAN
                : this.binaryNumericPromotionType(bo, lhsType, this.getType(bo.rhs))
            );
        }

        if (bo.operator == "*" || bo.operator == "/" || bo.operator == "%" || bo.operator == "+" || bo.operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
            IClassLoader icl = this.iClassLoader;

            // Unroll the operands of this binary operation.
            Iterator<Rvalue> ops = bo.unrollLeftAssociation();

            IClass lhsType = this.getType((Rvalue) ops.next());

            // Check the far left operand type.
            if (bo.operator == "+" && lhsType == icl.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                return icl.TYPE_java_lang_String;
            }

            // Determine the expression type.
            lhsType = this.getUnboxedType(lhsType);
            do {
                IClass rhsType = this.getUnboxedType(this.getType((Rvalue) ops.next()));
                if (bo.operator == "+" && rhsType == icl.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
                    return icl.TYPE_java_lang_String;
                }
                lhsType = this.binaryNumericPromotionType(bo, lhsType, rhsType);
            } while (ops.hasNext());

            return lhsType;
        }

        if (bo.operator == "<<"  || bo.operator == ">>"  || bo.operator == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
            IClass lhsType = this.getType(bo.lhs);
            return this.unaryNumericPromotionType(bo, lhsType);
        }

        this.compileError("Unexpected operator \"" + bo.operator + "\"", bo.getLocation());
        return this.iClassLoader.TYPE_java_lang_Object;
    }

    /**
     * @return The <var>type</var>, or, iff <var>type</var> is a primitive wrapper type, the unwrapped <var>type</var>
     */
    private IClass
    getUnboxedType(IClass type) {
        IClass c = this.isUnboxingConvertible(type);
        return c != null ? c : type;
    }

    private IClass
    getType2(Cast c) throws CompileException {
        return this.getType(c.targetType);
    }

    private IClass
    getType2(ParenthesizedExpression pe) throws CompileException {
        return this.getType(pe.value);
    }

    private IClass
    getType2(MethodInvocation mi) throws CompileException {
        IMethod iMethod = mi.iMethod != null ? mi.iMethod : (mi.iMethod = this.findIMethod(mi));

        return iMethod.getReturnType();
    }

    private IClass
    getType2(SuperclassMethodInvocation scmi) throws CompileException {
        return this.findIMethod(scmi).getReturnType();
    }

    private IClass
    getType2(NewClassInstance nci) throws CompileException {
        if (nci.iClass != null) return nci.iClass;

        assert nci.type != null;
        return (nci.iClass = this.getType(nci.type));
    }

    private IClass
    getType2(NewAnonymousClassInstance naci) {
        return this.resolve(naci.anonymousClassDeclaration);
    }

    private IClass
    getType2(ParameterAccess pa) throws CompileException {
        return this.getLocalVariable(pa.formalParameter).type;
    }

    private IClass
    getType2(NewArray na) throws CompileException {
        IClass res = this.getType(na.type);
        return res.getArrayIClass(na.dimExprs.length + na.dims, this.iClassLoader.TYPE_java_lang_Object);
    }

    private IClass
    getType2(NewInitializedArray nia) throws CompileException {
        IClass at = nia.arrayType != null ? this.getType(nia.arrayType) : nia.arrayIClass;
        assert at != null;
        return at;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(IntegerLiteral il) {
        String v        = il.value;
        char   lastChar = v.charAt(v.length() - 1);
        return lastChar == 'l' || lastChar == 'L' ? IClass.LONG : IClass.INT;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(FloatingPointLiteral fpl) {
        String v        = fpl.value;
        char   lastChar = v.charAt(v.length() - 1);
        return lastChar == 'f' || lastChar == 'F' ? IClass.FLOAT : IClass.DOUBLE;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(BooleanLiteral bl) {
        return IClass.BOOLEAN;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(CharacterLiteral cl) {
        return IClass.CHAR;
    }

    private IClass
    getType2(StringLiteral sl) {
        return this.iClassLoader.TYPE_java_lang_String;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(NullLiteral nl) {
        return IClass.VOID;
    }

    private IClass
    getType2(SimpleConstant sl) {
        Object v = sl.value;
        if (v instanceof Byte)      return IClass.BYTE;
        if (v instanceof Short)     return IClass.SHORT;
        if (v instanceof Integer)   return IClass.INT;
        if (v instanceof Long)      return IClass.LONG;
        if (v instanceof Float)     return IClass.FLOAT;
        if (v instanceof Double)    return IClass.DOUBLE;
        if (v instanceof Boolean)   return IClass.BOOLEAN;
        if (v instanceof Character) return IClass.CHAR;
        if (v instanceof String)    return this.iClassLoader.TYPE_java_lang_String;
        if (v == null)              return IClass.VOID;
        throw new InternalCompilerException("Invalid SimpleLiteral value type \"" + v.getClass() + "\"");
    }

    // ---------------- Atom.isType() ---------------

    private boolean
    isType(Atom a) throws CompileException {

        Boolean result = (Boolean) a.accept(new AtomVisitor<Boolean, CompileException>() {

            @Override public Boolean visitPackage(Package p) { return UnitCompiler.this.isType2(p); }

            @Override @Nullable public Boolean
            visitType(Type t) {

                return (Boolean) t.accept(new Visitor.TypeVisitor<Boolean, RuntimeException>() {

                    // SUPPRESS CHECKSTYLE LineLengthCheck:5
                    @Override public Boolean visitArrayType(ArrayType at)                { return UnitCompiler.this.isType2(at);  }
                    @Override public Boolean visitPrimitiveType(PrimitiveType bt)        { return UnitCompiler.this.isType2(bt);  }
                    @Override public Boolean visitReferenceType(ReferenceType rt)        { return UnitCompiler.this.isType2(rt);  }
                    @Override public Boolean visitRvalueMemberType(RvalueMemberType rmt) { return UnitCompiler.this.isType2(rmt); }
                    @Override public Boolean visitSimpleType(SimpleType st)              { return UnitCompiler.this.isType2(st);  }
                });
            }

            @Override @Nullable public Boolean
            visitRvalue(Rvalue rv) throws CompileException {

                return (Boolean) rv.accept(new Visitor.RvalueVisitor<Boolean, CompileException>() {

                    @Override @Nullable public Boolean
                    visitLvalue(Lvalue lv) throws CompileException {

                        return (Boolean) lv.accept(new Visitor.LvalueVisitor<Boolean, CompileException>() {

                            // SUPPRESS CHECKSTYLE LineLengthCheck:7
                            @Override public Boolean visitAmbiguousName(AmbiguousName an)                throws CompileException { return UnitCompiler.this.isType2(an);    }
                            @Override public Boolean visitArrayAccessExpression(ArrayAccessExpression aae)                       { return UnitCompiler.this.isType2(aae);   }
                            @Override public Boolean visitFieldAccess(FieldAccess fa)                                            { return UnitCompiler.this.isType2(fa);    }
                            @Override public Boolean visitFieldAccessExpression(FieldAccessExpression fae)                       { return UnitCompiler.this.isType2(fae);   }
                            @Override public Boolean visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { return UnitCompiler.this.isType2(scfae); }
                            @Override public Boolean visitLocalVariableAccess(LocalVariableAccess lva)                           { return UnitCompiler.this.isType2(lva);   }
                            @Override public Boolean visitParenthesizedExpression(ParenthesizedExpression pe)                    { return UnitCompiler.this.isType2(pe);    }
                        });
                    }

                    // SUPPRESS CHECKSTYLE LineLengthCheck:29
                    @Override public Boolean visitArrayLength(ArrayLength al)                                    { return UnitCompiler.this.isType2(al);   }
                    @Override public Boolean visitAssignment(Assignment a)                                       { return UnitCompiler.this.isType2(a);    }
                    @Override public Boolean visitUnaryOperation(UnaryOperation uo)                              { return UnitCompiler.this.isType2(uo);   }
                    @Override public Boolean visitBinaryOperation(BinaryOperation bo)                            { return UnitCompiler.this.isType2(bo);   }
                    @Override public Boolean visitCast(Cast c)                                                   { return UnitCompiler.this.isType2(c);    }
                    @Override public Boolean visitClassLiteral(ClassLiteral cl)                                  { return UnitCompiler.this.isType2(cl);   }
                    @Override public Boolean visitConditionalExpression(ConditionalExpression ce)                { return UnitCompiler.this.isType2(ce);   }
                    @Override public Boolean visitCrement(Crement c)                                             { return UnitCompiler.this.isType2(c);    }
                    @Override public Boolean visitInstanceof(Instanceof io)                                      { return UnitCompiler.this.isType2(io);   }
                    @Override public Boolean visitMethodInvocation(MethodInvocation mi)                          { return UnitCompiler.this.isType2(mi);   }
                    @Override public Boolean visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)     { return UnitCompiler.this.isType2(smi);  }
                    @Override public Boolean visitIntegerLiteral(IntegerLiteral il)                              { return UnitCompiler.this.isType2(il);   }
                    @Override public Boolean visitFloatingPointLiteral(FloatingPointLiteral fpl)                 { return UnitCompiler.this.isType2(fpl);  }
                    @Override public Boolean visitBooleanLiteral(BooleanLiteral bl)                              { return UnitCompiler.this.isType2(bl);   }
                    @Override public Boolean visitCharacterLiteral(CharacterLiteral cl)                          { return UnitCompiler.this.isType2(cl);   }
                    @Override public Boolean visitStringLiteral(StringLiteral sl)                                { return UnitCompiler.this.isType2(sl);   }
                    @Override public Boolean visitNullLiteral(NullLiteral nl)                                    { return UnitCompiler.this.isType2(nl);   }
                    @Override public Boolean visitSimpleConstant(SimpleConstant sl)                              { return UnitCompiler.this.isType2(sl);   }
                    @Override public Boolean visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)      { return UnitCompiler.this.isType2(naci); }
                    @Override public Boolean visitNewArray(NewArray na)                                          { return UnitCompiler.this.isType2(na);   }
                    @Override public Boolean visitNewInitializedArray(NewInitializedArray nia)                   { return UnitCompiler.this.isType2(nia);  }
                    @Override public Boolean visitNewClassInstance(NewClassInstance nci)                         { return UnitCompiler.this.isType2(nci);  }
                    @Override public Boolean visitParameterAccess(ParameterAccess pa)                            { return UnitCompiler.this.isType2(pa);   }
                    @Override public Boolean visitQualifiedThisReference(QualifiedThisReference qtr)             { return UnitCompiler.this.isType2(qtr);  }
                    @Override public Boolean visitThisReference(ThisReference tr)                                { return UnitCompiler.this.isType2(tr);   }
                    @Override public Boolean visitLambdaExpression(LambdaExpression le)                          { return UnitCompiler.this.isType2(le);   }
                    @Override public Boolean visitMethodReference(MethodReference mr)                            { return UnitCompiler.this.isType2(mr);   }
                    @Override public Boolean visitInstanceCreationReference(ClassInstanceCreationReference cicr) { return UnitCompiler.this.isType2(cicr); }
                    @Override public Boolean visitArrayCreationReference(ArrayCreationReference acr)             { return UnitCompiler.this.isType2(acr);  }
                });
            }

            @Override @Nullable public Boolean
            visitConstructorInvocation(ConstructorInvocation ci) { return false; }
        });

        assert result != null;
        return result;
    }

    @SuppressWarnings("static-method") private boolean
    isType2(Atom a) { return a instanceof Type; }

    private boolean
    isType2(AmbiguousName an) throws CompileException { return this.isType(this.reclassify(an)); }

    /**
     * Determines whether the given {@link IClass.IMember} is accessible in the given context, according to
     * JLS7 6.6.1.BL1.B4. Issues a {@link #compileError(String)} if not.
     */
    private boolean
    isAccessible(IClass.IMember member, Scope contextScope) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass declaringIClass = member.getDeclaringIClass();
        return (
            this.isAccessible(declaringIClass, contextScope)
            && this.isAccessible(declaringIClass, member.getAccess(), contextScope)
        );
    }

    /**
     * Checks whether the given {@link IClass.IMember} is accessible in the given context, according to JLS7
     * 6.6.1.BL1.B4. Issues a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass.IMember member, Scope contextScope, Location location) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass declaringIClass = member.getDeclaringIClass();
        this.checkAccessible(declaringIClass, contextScope, location);
        this.checkMemberAccessible(declaringIClass, member, contextScope, location);
    }

    /**
     * Determines whether a member (class, interface, field or method) declared in a given class is accessible from a
     * given block statement context, according to JLS7 6.6.1.4.
     */
    private boolean
    isAccessible(IClass iClassDeclaringMember, Access memberAccess, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextScope);
    }

    /**
     * Verifies that a member (class, interface, field or method) declared in a given class is accessible from a given
     * block statement context, according to JLS7 6.6.1.4. Issue a {@link #compileError(String)} if not.
     */
    private void
    checkMemberAccessible(
        IClass         iClassDeclaringMember,
        IClass.IMember member,
        Scope          contextScope,
        Location       location
    ) throws CompileException {
        String message = this.internalCheckAccessible(iClassDeclaringMember, member.getAccess(), contextScope);
        if (message != null) this.compileError(member.toString() + ": " + message, location);
    }

    /**
     * @return a descriptive text iff a member declared in that {@link IClass} with that {@link Access} is inaccessible
     */
    @Nullable private String
    internalCheckAccessible(
        IClass iClassDeclaringMember,
        Access memberAccess,
        Scope  contextScope
    ) throws CompileException {

        // At this point, memberAccess is PUBLIC, DEFAULT, PROTECTED or PRIVATE.

        // PUBLIC members are always accessible.
        if (memberAccess == Access.PUBLIC) return null;

        // At this point, the member is DEFAULT, PROTECTED or PRIVATE accessible.

        // Determine the class declaring the context.
        IClass iClassDeclaringContext = null;
        for (Scope s = contextScope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof TypeDeclaration) {
                iClassDeclaringContext = this.resolve((TypeDeclaration) s);
                break;
            }
        }

        // Access is always allowed for block statements declared in the same class as the member.
        if (iClassDeclaringContext == iClassDeclaringMember) return null;

        // Check whether the member and the context block statement are enclosed by the same top-level type.
        if (
            iClassDeclaringContext != null
            && !this.options.contains(JaninoOption.PRIVATE_MEMBERS_OF_ENCLOSING_AND_ENCLOSED_TYPES_INACCESSIBLE)
        ) {
            IClass topLevelIClassEnclosingMember = iClassDeclaringMember;
            for (IClass c = iClassDeclaringMember.getDeclaringIClass(); c != null; c = c.getDeclaringIClass()) {
                topLevelIClassEnclosingMember = c;
            }
            IClass topLevelIClassEnclosingContextBlockStatement = iClassDeclaringContext;
            for (
                IClass c = iClassDeclaringContext.getDeclaringIClass();
                c != null;
                c = c.getDeclaringIClass()
            ) topLevelIClassEnclosingContextBlockStatement = c;

            if (topLevelIClassEnclosingMember == topLevelIClassEnclosingContextBlockStatement) return null;
        }

        if (memberAccess == Access.PRIVATE) {
            return "Private member cannot be accessed from type \"" + iClassDeclaringContext + "\".";
        }

        // At this point, the member is DEFAULT or PROTECTED accessible.

        // Check whether the member and the context block statement are declared in the same package.
        if (iClassDeclaringContext != null && Descriptor.areInSamePackage(
            iClassDeclaringMember.getDescriptor(),
            iClassDeclaringContext.getDescriptor()
        )) return null;

        if (memberAccess == Access.DEFAULT) {
            return (
                "Member with \"package\" access cannot be accessed from type \""
                + iClassDeclaringContext
                + "\"."
            );
        }

        // At this point, the member is PROTECTED accessible.

        // Check whether the class declaring the context block statement is a subclass of the class declaring the
        // member or a nested class whose parent is a subclass
        {
            IClass parentClass = iClassDeclaringContext;
            do {
                assert parentClass != null;
                if (iClassDeclaringMember.isAssignableFrom(parentClass)) {
                    return null;
                }
                parentClass = parentClass.getOuterIClass();
            } while (parentClass != null);
        }

        return (
            "Protected member cannot be accessed from type \""
            + iClassDeclaringContext
            + "\", which is neither declared in the same package as nor is a subclass of \""
            + iClassDeclaringMember
            + "\"."
        );
    }

    /**
     * Determines whether the given {@link IClass} is accessible in the given context, according to JLS7 6.6.1.2 and
     * 6.6.1.4.
     */
    private boolean
    isAccessible(IClass type, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(type, contextScope);
    }

    /**
     * Checks whether the given {@link IClass} is accessible in the given context, according to JLS7 6.6.1.2 and
     * 6.6.1.4. Issues a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass type, Scope contextScope, Location location) throws CompileException {
        String message = this.internalCheckAccessible(type, contextScope);
        if (message != null) this.compileError(message, location);
    }

    /**
     * @return An error message, or {@code null}
     */
    @Nullable private String
    internalCheckAccessible(IClass type, Scope contextScope) throws CompileException {

        // Determine the type declaring the type.
        IClass iClassDeclaringType = type.getDeclaringIClass();

        // Check accessibility of package member type.
        if (iClassDeclaringType == null) {
            if (type.getAccess() == Access.PUBLIC) {
                return null;
            } else
            if (type.getAccess() == Access.DEFAULT) {

                // Determine the type declaring the context block statement.
                IClass iClassDeclaringContextBlockStatement;
                for (Scope s = contextScope;; s = s.getEnclosingScope()) {
                    if (s instanceof TypeDeclaration) {
                        iClassDeclaringContextBlockStatement = this.resolve((TypeDeclaration) s);
                        break;
                    }
                    if (s instanceof EnclosingScopeOfTypeDeclaration) {
                        iClassDeclaringContextBlockStatement = this.resolve(
                            ((EnclosingScopeOfTypeDeclaration) s).typeDeclaration
                        );
                        break;
                    }
                }

                // Check whether the type is accessed from within the same package.
                String packageDeclaringType = Descriptor.getPackageName(type.getDescriptor());
                String contextPackage       = Descriptor.getPackageName(iClassDeclaringContextBlockStatement.getDescriptor()); // SUPPRESS CHECKSTYLE LineLength
                if (
                    packageDeclaringType == null
                    ? contextPackage != null
                    : !packageDeclaringType.equals(contextPackage)
                ) return "\"" + type + "\" is inaccessible from this package";
                return null;
            } else
            {
                throw new InternalCompilerException((
                    "\"" + type + "\" has unexpected access \"" + type.getAccess() + "\""
                ));
            }
        }

        // "type" is a member type at this point.
        return this.internalCheckAccessible(iClassDeclaringType, type.getAccess(), contextScope);
    }

    private Type
    toTypeOrCompileException(Atom a) throws CompileException {
        Type result = a.toType();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not a type", a.getLocation());
            return new SimpleType(a.getLocation(), this.iClassLoader.TYPE_java_lang_Object);
        }
        return result;
    }

    private Rvalue
    toRvalueOrCompileException(final Atom a) throws CompileException {
        Rvalue result = a.toRvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an rvalue", a.getLocation());
            return new StringLiteral(a.getLocation(), "\"X\"");
        }
        return result;
    }

    private Lvalue
    toLvalueOrCompileException(final Atom a) throws CompileException {
        Lvalue result = a.toLvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an lvalue", a.getLocation());
            return new Lvalue(a.getLocation()) {

                @Override @Nullable public <R, EX extends Throwable> R
                accept(Visitor.LvalueVisitor<R, EX> visitor) { return null; }

                @Override public String
                toString() { return a.toString(); }
            };
        }
        return result;
    }

    /**
     * Copies the values of the synthetic parameters of this constructor ("this$..." and "val$...") to the synthetic
     * fields of the object ("this$..." and "val$...").
     */
    void
    assignSyntheticParametersToSyntheticFields(ConstructorDeclarator cd) throws CompileException {
        for (IClass.IField sf : cd.getDeclaringClass().syntheticFields.values()) {
            LocalVariable syntheticParameter = (LocalVariable) cd.syntheticParameters.get(sf.getName());
            if (syntheticParameter == null) {
                throw new InternalCompilerException(
                    "SNO: Synthetic parameter for synthetic field \""
                    + sf.getName()
                    + "\" not found"
                );
            }
            ExpressionStatement es = new ExpressionStatement(new Assignment(
                cd.getLocation(),                    // location
                new FieldAccess(                     // lhs
                    cd.getLocation(),                    // location
                    new ThisReference(cd.getLocation()), // lhs
                    sf                                   // field
                ),
                "=",                                 // operator
                new LocalVariableAccess(             // rhs
                    cd.getLocation(),                    // location
                    syntheticParameter                   // localVariable
                )
            ));
            es.setEnclosingScope(cd);
            this.compile(es);
        }
    }

    /**
     * Compiles the instance variable initializers and the instance initializers in their lexical order.
     */
    void
    initializeInstanceVariablesAndInvokeInstanceInitializers(ConstructorDeclarator cd) throws CompileException {

        // Compilation of block statements can create synthetic variables, so we must not use an iterator.
        List<BlockStatement> vdai = cd.getDeclaringClass().variableDeclaratorsAndInitializers;
        for (int i = 0; i < vdai.size(); i++) {
            BlockStatement bs = (BlockStatement) vdai.get(i);

            if (bs instanceof Initializer      && ((Initializer)      bs).isStatic()) continue;
            if (bs instanceof FieldDeclaration && ((FieldDeclaration) bs).isStatic()) continue;

            if (!this.compile(bs)) {
                this.compileError(
                    "Instance variable declarator or instance initializer does not complete normally",
                    bs.getLocation()
                );
            }
        }
    }

    /**
     * Statements that jump out of blocks ({@code return}, {@code break}, {@code continue}) must call this method to
     * make sure that the {@code finally} clauses of all {@code try ... catch} and {@code synchronized} statements are
     * executed.
     */
    private void
    leaveStatements(Scope from, Scope to) throws CompileException {
        Scope prev = null;
        for (Scope s = from; s != to; s = s.getEnclosingScope()) {
            if (
                s instanceof BlockStatement
                && !(s instanceof TryStatement && ((TryStatement) s).finallY == prev)
            ) {
                this.leave((BlockStatement) s);
            }
            prev = s;
        }
    }

    /**
     * The LHS operand of type <var>lhsType</var> is expected on the stack.
     * <p>
     *   The following operators are supported: {@code | ^ & * / % + - << >> >>>}
     * </p>
     */
    private IClass
    compileArithmeticBinaryOperation(
        Locatable locatable,
        IClass    lhsType,
        String    operator,
        Rvalue    rhs
    ) throws CompileException {
        return this.compileArithmeticOperation(
            locatable,
            lhsType,
            Arrays.asList(rhs).iterator(),
            operator
        );
    }

    /**
     * Executes an arithmetic operation on a sequence of <var>operands</var>. If <var>type</var> is non-{@code null},
     * then the first operand with that type is already on the stack.
     * <p>
     *   The following operators are supported: {@code | ^ & * / % + - << >> >>>}
     * </p>
     */
    private IClass
    compileArithmeticOperation(
        final Locatable  locatable,
        @Nullable IClass firstOperandType,
        Iterator<Rvalue> operands,
        String           operator
    ) throws CompileException {

        // A very special case.
        if (operator == "+" && firstOperandType == this.iClassLoader.TYPE_java_lang_String) { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength
            assert firstOperandType != null;
            return this.compileStringConcatenation(locatable, firstOperandType, (Rvalue) operands.next(), operands);
        }

        IClass type = firstOperandType == null ? this.compileGetValue((Rvalue) operands.next()) : firstOperandType;

        // Operator which is allowed for BYTE, SHORT, INT, LONG and BOOLEAN operands?
        if (operator == "|" || operator == "^" || operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality:5

            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                IClass rhsType = this.getType(operand);

                if (this.isConvertibleToPrimitiveNumeric(type) && this.isConvertibleToPrimitiveNumeric(rhsType)) {
                    IClass promotedType = this.binaryNumericPromotionType(
                        operand,
                        this.getUnboxedType(type),
                        this.getUnboxedType(rhsType)
                    );
                    if (promotedType != IClass.INT && promotedType != IClass.LONG) {
                        throw new CompileException("Invalid operand type " + promotedType, operand.getLocation());
                    }
                    this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, type), promotedType);
                    this.compileGetValue(operand);
                    this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, rhsType), promotedType);
                    this.andOrXor(operand, operator);
                    type = promotedType;
                } else
                if (
                    (type == IClass.BOOLEAN || this.getUnboxedType(type) == IClass.BOOLEAN)
                    && (rhsType == IClass.BOOLEAN || this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                ) {
                    IClassLoader icl = this.iClassLoader;
                    if (type == icl.TYPE_java_lang_Boolean) {
                        this.unboxingConversion(locatable, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                    }
                    this.compileGetValue(operand);
                    if (rhsType == icl.TYPE_java_lang_Boolean) {
                        this.unboxingConversion(locatable, icl.TYPE_java_lang_Boolean, IClass.BOOLEAN);
                    }
                    this.andOrXor(operand, operator);
                    type = IClass.BOOLEAN;
                } else
                {
                    this.compileError((
                        "Operator \""
                        + operator
                        + "\" not defined on types \""
                        + type
                        + "\" and \""
                        + rhsType
                        + "\""
                    ), locatable.getLocation());
                    type = IClass.INT;
                }
            }
            return type;
        }

        // Operator which is allowed for INT, LONG, FLOAT, DOUBLE and (for operator '+') STRING operands?
        if (operator == "*" || operator == "/" || operator == "%" || operator == "+" || operator == "-") { // SUPPRESS CHECKSTYLE StringLiteralEquality|LineLength:6
            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                // String concatenation?
                if (operator == "+" && ( // SUPPRESS CHECKSTYLE StringLiteralEquality
                    type == this.iClassLoader.TYPE_java_lang_String
                    || this.getType(operand) == this.iClassLoader.TYPE_java_lang_String
                )) return this.compileStringConcatenation(locatable, type, operand, operands);

                // It's a numeric arithmetic operation.
                IClass rhsType      = this.getType(operand);
                IClass promotedType = this.binaryNumericPromotionType(
                    operand,
                    this.getUnboxedType(type),
                    this.getUnboxedType(rhsType)
                );

                this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, type), promotedType);
                this.compileGetValue(operand);
                this.numericPromotion(operand, this.convertToPrimitiveNumericType(operand, rhsType), promotedType);
                this.mulDivRemAddSub(operand, operator);

                type = promotedType;
            }

            return type;
        }

        // Operator which is allowed for BYTE, SHORT, INT and LONG lhs operand and BYTE, SHORT, INT or LONG rhs operand?
        if (operator == "<<" || operator == ">>" || operator == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality:4

            while (operands.hasNext()) {
                Rvalue operand = (Rvalue) operands.next();

                type = this.unaryNumericPromotion(operand, type);

                IClass rhsType         = this.compileGetValue(operand);
                IClass promotedRhsType = this.unaryNumericPromotion(operand, rhsType);
                if (promotedRhsType == IClass.INT) {
                    ;
                } else
                if (promotedRhsType == IClass.LONG) {
                    this.l2i(operand);
                } else
                {
                    this.compileError(
                        "Shift distance of type \"" + rhsType + "\" is not allowed",
                        locatable.getLocation()
                    );
                }

                this.shift(operand, operator);
            }

            return type;
        }

        throw new InternalCompilerException("Unexpected operator \"" + operator + "\"");
    }

    /**
     * @param type          The type of the first operand, which is already on the stack
     * @param secondOperand The second operand
     * @param operands      All following operands
     */
    private IClass
    compileStringConcatenation(
        final Locatable  locatable,
        IClass           type,
        final Rvalue     secondOperand,
        Iterator<Rvalue> operands
    ) throws CompileException {

        // Convert the first operand (which is already on the operand stack) to "String".
        this.stringConversion(locatable, type);

        // Compute list of operands and merge consecutive constant operands.
        List<Rvalue> tmp = new ArrayList<Rvalue>();
        for (Rvalue nextOperand = secondOperand; nextOperand != null;) {
            Object cv = this.getConstantValue(nextOperand);
            if (cv == UnitCompiler.NOT_CONSTANT) {

                // Non-constant operand.
                tmp.add(nextOperand);

                nextOperand = operands.hasNext() ? (Rvalue) operands.next() : null;
            } else
            {
                // Constant operand. Check to see whether the next operand is also constant.
                if (operands.hasNext()) {
                    nextOperand = (Rvalue) operands.next();
                    Object cv2 = this.getConstantValue(nextOperand);
                    if (cv2 != UnitCompiler.NOT_CONSTANT) {
                        StringBuilder sb = new StringBuilder(String.valueOf(cv)).append(cv2);
                        for (;;) {
                            if (!operands.hasNext()) {
                                nextOperand = null;
                                break;
                            }
                            nextOperand = (Rvalue) operands.next();
                            Object cv3 = this.getConstantValue(nextOperand);
                            if (cv3 == UnitCompiler.NOT_CONSTANT) break;
                            sb.append(cv3);
                        }
                        cv = sb.toString();
                    }
                } else
                {
                    nextOperand = null;
                }

                // Break long string constants up into UTF8-able chunks.
                for (final String s : UnitCompiler.makeUtf8Able(String.valueOf(cv))) {
                    tmp.add(new SimpleConstant(locatable.getLocation(), s));
                }
            }
        }

        // At this point "tmp" contains an optimized sequence of Strings (representing constant portions) and Rvalues
        // (non-constant portions).

        if (tmp.size() <= UnitCompiler.STRING_CONCAT_LIMIT - 1) {

            // String concatenation through "a.concat(b).concat(c)".
            for (Rvalue operand : tmp) {

                // "s.concat(String.valueOf(operand))"
                UnitCompiler.this.stringConversion(operand, UnitCompiler.this.compileGetValue(operand));
                this.invoke(locatable, this.iClassLoader.METH_java_lang_String__concat__java_lang_String);
            }
            return this.iClassLoader.TYPE_java_lang_String;
        }

        // String concatenation through "new StringBuilder(a).append(b).append(c).append(d).toString()".
        this.neW(locatable, this.iClassLoader.TYPE_java_lang_StringBuilder);
        this.dupx(locatable);
        this.swap(locatable);
        this.invoke(locatable, this.iClassLoader.CTOR_java_lang_StringBuilder__java_lang_String);

        for (Iterator<Rvalue> it = tmp.iterator(); it.hasNext();) {
            Rvalue operand = (Rvalue) it.next();

            // "sb.append(operand)"
            IClass t = UnitCompiler.this.compileGetValue(operand);
            this.invoke(locatable, (
                t == IClass.BYTE    ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.SHORT   ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.INT     ? this.iClassLoader.METH_java_lang_StringBuilder__append__int     :
                t == IClass.LONG    ? this.iClassLoader.METH_java_lang_StringBuilder__append__long    :
                t == IClass.FLOAT   ? this.iClassLoader.METH_java_lang_StringBuilder__append__float   :
                t == IClass.DOUBLE  ? this.iClassLoader.METH_java_lang_StringBuilder__append__double  :
                t == IClass.CHAR    ? this.iClassLoader.METH_java_lang_StringBuilder__append__char    :
                t == IClass.BOOLEAN ? this.iClassLoader.METH_java_lang_StringBuilder__append__boolean :
                this.iClassLoader.METH_java_lang_StringBuilder__append__java_lang_Object
            ));
        }

        // "StringBuilder.toString()":
        this.invoke(locatable, this.iClassLoader.METH_java_lang_StringBuilder__toString);

        return this.iClassLoader.TYPE_java_lang_String;
    }

    /**
     * Helper interface for string conversion.
     */
    interface Compilable { void compile() throws CompileException; }

    /**
     * Converts object of type "sourceType" to type "String" (JLS7 15.18.1.1).
     */
    private void
    stringConversion(Locatable locatable, IClass sourceType) throws CompileException {
        this.invoke(locatable, (
            sourceType == IClass.BYTE    ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceType == IClass.SHORT   ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceType == IClass.INT     ? this.iClassLoader.METH_java_lang_String__valueOf__int :
            sourceType == IClass.LONG    ? this.iClassLoader.METH_java_lang_String__valueOf__long :
            sourceType == IClass.FLOAT   ? this.iClassLoader.METH_java_lang_String__valueOf__float :
            sourceType == IClass.DOUBLE  ? this.iClassLoader.METH_java_lang_String__valueOf__double :
            sourceType == IClass.CHAR    ? this.iClassLoader.METH_java_lang_String__valueOf__char :
            sourceType == IClass.BOOLEAN ? this.iClassLoader.METH_java_lang_String__valueOf__boolean :
            this.iClassLoader.METH_java_lang_String__valueOf__java_lang_Object
        ));
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     *   Notice: This method is used both for explicit constructor invocation (first statement of a constructor body)
     *   and implicit constructor invocation (right after NEW).
     * </p>
     *
     * @param enclosingInstance Used if the target class is an inner class
     */
    private void
    invokeConstructor(
        Locatable        locatable,
        Scope            scope,
        @Nullable Rvalue enclosingInstance,
        IClass           targetClass,
        Rvalue[]         arguments
    ) throws CompileException {
        // Find constructors.
        IClass.IConstructor[] iConstructors = targetClass.getDeclaredIConstructors();
        if (iConstructors.length == 0) {
            throw new InternalCompilerException(
                "SNO: Target class \"" + targetClass.getDescriptor() + "\" has no constructors"
            );
        }

        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            locatable,     // l
            iConstructors, // iInvocables
            arguments,     // arguments
            scope          // contextScope
        );

        // Check exceptions that the constructor may throw.
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        for (IClass te : thrownExceptions) {
            this.checkThrownException(locatable, te, scope);
        }

        // Enum constant: Pass constant name and ordinal as synthetic parameters.
        ENUM_CONSTANT:
        if (
            scope instanceof FieldDeclaration
            && scope.getEnclosingScope() instanceof EnumDeclaration
        ) {

            FieldDeclaration fd = (FieldDeclaration) scope;
            EnumDeclaration  ed = (EnumDeclaration) fd.getEnclosingScope();

            if (fd.variableDeclarators.length != 1) break ENUM_CONSTANT;

            String fieldName = fd.variableDeclarators[0].name;

            int ordinal = 0;
            for (EnumConstant ec : ed.getConstants()) {
                if (fieldName.equals(ec.name)) {

                    // Now we know that this field IS an enum constant.
                    this.consT(locatable, fieldName);
                    this.consT(locatable, ordinal);
                    break ENUM_CONSTANT;
                }
                ordinal++;
            }
        }

        // Pass enclosing instance as a synthetic parameter.
        if (enclosingInstance != null) {
            IClass outerIClass = targetClass.getOuterIClass();
            if (outerIClass != null) {
                IClass eiic = this.compileGetValue(enclosingInstance);
                if (!outerIClass.isAssignableFrom(eiic)) {
                    this.compileError(
                        "Type of enclosing instance (\"" + eiic + "\") is not assignable to \"" + outerIClass + "\"",
                        locatable.getLocation()
                    );
                }
            }
        }

        // Pass local variables to constructor as synthetic parameters.
        {
            IClass.IField[] syntheticFields = targetClass.getSyntheticIFields();

            // Determine enclosing function declarator and type declaration.
            TypeBodyDeclaration scopeTbd;
            TypeDeclaration     scopeTypeDeclaration;
            {
                Scope s = scope;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
                scopeTbd             = (TypeBodyDeclaration) s;
                scopeTypeDeclaration = scopeTbd.getDeclaringType();
            }

            if (!(scopeTypeDeclaration instanceof AbstractClassDeclaration)) {
                if (syntheticFields.length > 0) {
                    throw new InternalCompilerException("SNO: Target class has synthetic fields");
                }
            }

            // Notice: Constructor invocations can also occur in interface declarations in constant initializers.
            if (scopeTypeDeclaration instanceof AbstractClassDeclaration) {
                AbstractClassDeclaration scopeClassDeclaration = (AbstractClassDeclaration) scopeTypeDeclaration;
                for (IClass.IField sf : syntheticFields) {
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTbd instanceof MethodDeclarator) {
                            this.load(locatable, this.resolve(scopeClassDeclaration), 0);
                            this.getfield(locatable, eisf);
                        } else
                        if (scopeTbd instanceof ConstructorDeclarator) {
                            ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) scopeTbd;
                            LocalVariable         syntheticParameter    = (
                                (LocalVariable) constructorDeclarator.syntheticParameters.get(sf.getName())
                            );
                            if (syntheticParameter == null) {
                                this.compileError((
                                    "Compiler limitation: Constructor cannot access local variable \""
                                    + sf.getName().substring(4)
                                    + "\" declared in an enclosing block because none of the methods accesses it. "
                                    + "As a workaround, declare a dummy method that accesses the local variable."
                                ), locatable.getLocation());
                                this.consT(locatable, (Object) null);
                            } else {
                                this.load(locatable, syntheticParameter);
                            }
                        } else
                        if (scopeTbd instanceof FieldDeclaration) {
                            this.compileError((
                                "Compiler limitation: Field initializers cannot access local variable \""
                                + sf.getName().substring(4)
                                + "\" declared in an enclosing block because none of the methods accesses it. "
                                + "As a workaround, declare a dummy method that accesses the local variable."
                            ), locatable.getLocation());
                            this.consT(locatable, (Object) null);
                        } else
                        {
                            throw new AssertionError(scopeTbd);
                        }
                    } else {
                        String        localVariableName = sf.getName().substring(4);
                        LocalVariable lv;
                        DETERMINE_LV: {
                            Scope s;

                            // Does one of the enclosing blocks declare a local variable with that name?
                            for (s = scope; s instanceof BlockStatement; s = s.getEnclosingScope()) {
                                BlockStatement       bs = (BlockStatement) s;
                                Scope                es = bs.getEnclosingScope();

                                List<? extends BlockStatement> statements;
                                if (es instanceof Block) {
                                    statements = ((Block) es).statements;
                                } else
                                if (es instanceof FunctionDeclarator) {
                                    statements = ((FunctionDeclarator) es).statements;
                                } else
                                if (es instanceof ForEachStatement) {
                                    FunctionDeclarator.FormalParameter fp = ((ForEachStatement) es).currentElement;
                                    if (fp.name.equals(localVariableName)) {
                                        lv = this.getLocalVariable(fp);
                                        break DETERMINE_LV;
                                    }
                                    continue;
                                } else
                                {
                                    continue;
                                }

                                if (statements != null) {
                                    for (BlockStatement bs2 : statements) {
                                        if (bs2 == bs) break;
                                        if (bs2 instanceof LocalVariableDeclarationStatement) {
                                            LocalVariableDeclarationStatement lvds = (
                                                (LocalVariableDeclarationStatement) bs2
                                            );
                                            for (VariableDeclarator vd : lvds.variableDeclarators) {
                                                if (vd.name.equals(localVariableName)) {
                                                    lv = this.getLocalVariable(lvds, vd);
                                                    break DETERMINE_LV;
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Does the declaring function declare a parameter with that name?
                            while (!(s instanceof FunctionDeclarator)) s = s.getEnclosingScope();
                            FunctionDeclarator fd = (FunctionDeclarator) s;
                            for (FormalParameter fp : fd.formalParameters.parameters) {
                                if (fp.name.equals(localVariableName)) {
                                    lv = this.getLocalVariable(fp);
                                    break DETERMINE_LV;
                                }
                            }
                            throw new InternalCompilerException(
                                "SNO: Synthetic field \""
                                + sf.getName()
                                + "\" neither maps a synthetic field of an enclosing instance nor a local variable"
                            );
                        }
                        this.load(locatable, lv);
                    }
                }
            }
        }

        // Evaluate constructor arguments.
        Rvalue[] adjustedArgs   = null;
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        int      actualSize     = arguments.length;
        if (iConstructor.isVarargs() && iConstructor.argsNeedAdjust()) {
            adjustedArgs = new Rvalue[parameterTypes.length];
            Rvalue[] lastArgs = new Rvalue[actualSize - parameterTypes.length + 1];
            for (int i = 0, j = parameterTypes.length - 1; i < lastArgs.length; ++i, ++j) {
                lastArgs[i] = arguments[j];
            }

            for (int i = parameterTypes.length - 2; i >= 0; --i) {
                adjustedArgs[i] = arguments[i];
            }
            Location loc = (lastArgs.length == 0 ? locatable : lastArgs[lastArgs.length - 1]).getLocation();
            adjustedArgs[adjustedArgs.length - 1] = new NewInitializedArray(
                loc,                                       // location
                parameterTypes[parameterTypes.length - 1], // arrayIClass
                new ArrayInitializer(loc, lastArgs)        // arrayInitializer
            );
            arguments = adjustedArgs;
        }

        for (int i = 0; i < arguments.length; ++i) {
            this.assignmentConversion(
                locatable,                          // locatable
                this.compileGetValue(arguments[i]), // sourceType
                parameterTypes[i],                  // targetType
                this.getConstantValue(arguments[i]) // constantValue
            );
        }

        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()", prepended with the synthetic parameters.
        this.invoke(locatable, iConstructor);
    }

    /**
     * @return The {@link IField}s that are declared by the <var>fieldDeclaration</var>
     */
    private IClass.IField[]
    compileFields(final FieldDeclaration fieldDeclaration) {

        IClass.IField[] result = new IClass.IField[fieldDeclaration.variableDeclarators.length];

        for (int i = 0; i < result.length; ++i) {
            VariableDeclarator vd = fieldDeclaration.variableDeclarators[i];
            result[i] = this.compileField(
                fieldDeclaration.getDeclaringType(),
                fieldDeclaration.getAnnotations(),
                fieldDeclaration.getAccess(),
                fieldDeclaration.isStatic(),
                fieldDeclaration.isFinal(),
                fieldDeclaration.type,
                vd.brackets,
                vd.name,
                vd.initializer
            );
        }

        return result;
    }

    /**
     * Compiles one variable declarator into an {@link IField}.
     * <p>
     *   Example: "b" in in the variable declaration
     * </p>
     * <pre>
     *     class Foo {
     *         &#64;Deprecated private int[] a, b[], c;
     *     }
     * </pre>
     *
     * @param declaringType      "{@code class Foo}"
     * @param type               "{@code int[]}"
     */
    private IField
    compileField(
        final TypeDeclaration                    declaringType,
        final Annotation[]                       annotations,
        final Access                             access,
        final boolean                            statiC,
        final boolean                            finaL,
        final Type                               type,
        final int                                brackets,
        final String                             name,
        @Nullable final ArrayInitializerOrRvalue initializer
    ) {

        return this.resolve(declaringType).new IField() {

            @Nullable private IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() { return declaringType instanceof InterfaceDeclaration ? Access.PUBLIC : access; }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(annotations));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(null, ce);
                }
            }

            // Implement "IField".

            @Override public boolean
            isStatic() { return declaringType instanceof InterfaceDeclaration || statiC; }

            @Override public IClass
            getType() throws CompileException {
                return UnitCompiler.this.getType(type).getArrayIClass(
                    brackets,
                    UnitCompiler.this.iClassLoader.TYPE_java_lang_Object
                );
            }

            @Override public String
            getName() { return name; }

            @Override @Nullable public Object
            getConstantValue() throws CompileException {
                ArrayInitializerOrRvalue oi = initializer;
                if (finaL && oi instanceof Rvalue) {
                    Object constantInitializerValue = UnitCompiler.this.getConstantValue((Rvalue) oi);
                    if (constantInitializerValue != UnitCompiler.NOT_CONSTANT) {
                        return UnitCompiler.this.assignmentConversion(
                            oi,                       // locatable
                            constantInitializerValue, // value
                            this.getType()            // targetType
                        );
                    }
                }
                return UnitCompiler.NOT_CONSTANT;
            }
        };
    }

    /**
     * Determines the non-constant-final initializer of the given {@link VariableDeclarator}.
     *
     * @return {@code null} if the variable is declared without an initializer or if the initializer is
     *         constant-final
     */
    @Nullable ArrayInitializerOrRvalue
    getNonConstantFinalInitializer(FieldDeclaration fd, VariableDeclarator vd) throws CompileException {

        // Check if optional initializer exists.
        if (vd.initializer == null) return null;

        // Check if initializer is constant-final.
        if (
            fd.isStatic()
            && fd.isFinal()
            && vd.initializer instanceof Rvalue
            && this.getConstantValue((Rvalue) vd.initializer) != UnitCompiler.NOT_CONSTANT
        ) return null;

        return vd.initializer;
    }

    private Atom
    reclassify(AmbiguousName an) throws CompileException {

        if (an.reclassified != null) return an.reclassified;

        return (an.reclassified = this.reclassifyName(an.getLocation(), an.getEnclosingScope(), an.identifiers, an.n));
    }

    private IAnnotation[]
    toIAnnotations(Annotation[] annotations) throws CompileException {

        IAnnotation[] result = new IAnnotation[annotations.length];
        for (int i = 0; i < result.length; i++) result[i] = this.toIAnnotation(annotations[i]);

        return result;
    }

    private IAnnotation
    toIAnnotation(Annotation annotation) throws CompileException {

        IAnnotation result = (IAnnotation) annotation.accept(
            new AnnotationVisitor<IAnnotation, CompileException>() {

                @Override public IAnnotation
                visitMarkerAnnotation(final MarkerAnnotation ma) throws CompileException {
                    return this.toIAnnotation(ma.type, new ElementValuePair[0]);
                }

                @Override public IAnnotation
                visitSingleElementAnnotation(SingleElementAnnotation sea) throws CompileException {
                    return this.toIAnnotation(
                        sea.type,
                        new ElementValuePair[] { new ElementValuePair("value", sea.elementValue) }
                    );
                }

                @Override public IAnnotation
                visitNormalAnnotation(NormalAnnotation na) throws CompileException {
                    return this.toIAnnotation(na.type, na.elementValuePairs);
                }

                private IAnnotation
                toIAnnotation(final Type type, ElementValuePair[] elementValuePairs) throws CompileException {

                    final Map<String, Object> m = new HashMap<String, Object>();
                    for (ElementValuePair evp : elementValuePairs) {
                        m.put(evp.identifier, this.toObject(evp.elementValue));
                    }

                    return new IAnnotation() {

                        @Override public Object
                        getElementValue(String name) { return m.get(name); }

                        @Override public IClass
                        getAnnotationType() throws CompileException {
                            return UnitCompiler.this.getType(type);
                        }
                    };
                }

                /**
                 * @return A wrapped primitive value, a {@link String}, an {@link IClass} (representing a class
                 *         literal), an {@link IClass.IField} (representing an enum constant), or an {@link Object}[]
                 *         array containing any of the previously described
                 */
                private Object
                toObject(ElementValue ev) throws CompileException {

                    try {
                        Object result = ev.accept(new ElementValueVisitor<Object, CompileException>() {

                            @Override public Object
                            visitRvalue(Rvalue rv) throws CompileException {

                                if (rv instanceof AmbiguousName) {
                                    AmbiguousName an = (AmbiguousName) rv;
                                    rv = UnitCompiler.this.reclassify(an).toRvalueOrCompileException();
                                }

                                // Class literal?
                                if (rv instanceof ClassLiteral) {
                                    ClassLiteral cl = (ClassLiteral) rv;
                                    return UnitCompiler.this.getType(cl.type);
                                }

                                // Enum constant?
                                if (rv instanceof FieldAccess) {
                                    FieldAccess fa = (FieldAccess) rv;
                                    return fa.field;
                                }

                                Object result = UnitCompiler.this.getConstantValue(rv);
                                if (result == null) {
                                    UnitCompiler.this.compileError(
                                        "Null value not allowed as an element value",
                                        rv.getLocation()
                                    );
                                    return 1;
                                }
                                if (result == UnitCompiler.NOT_CONSTANT) {
                                    UnitCompiler.this.compileError(
                                        "Element value is not a constant expression",
                                        rv.getLocation()
                                    );
                                    return 1;
                                }
                                return result;
                            }

                            @Override public Object
                            visitAnnotation(Annotation a) throws CompileException {
                                return UnitCompiler.this.toIAnnotation(a);
                            }

                            @Override public Object
                            visitElementValueArrayInitializer(ElementValueArrayInitializer evai)
                            throws CompileException {
                                Object[] result = new Object[evai.elementValues.length];
                                for (int i = 0; i < result.length; i++) {
                                    result[i] = toObject(evai.elementValues[i]);
                                }
                                return result;
                            }
                        });
                        assert result != null;
                        return result;
                    } catch (/*CompileException*/ Exception ce) {
                        if (ce instanceof CompileException) throw (CompileException) ce;
                        throw new IllegalStateException(ce);
                    }
                }
            }
        );

        assert result != null;
        return result;
    }

    /**
     * Reclassifies the ambiguous name consisting of the first <var>n</var> of the <var>identifiers</var> (JLS7
     * 6.5.2.2).
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String[] identifiers, int n) throws CompileException {

        if (n == 1) return this.reclassifyName(location, scope, identifiers[0]);

        // 6.5.2.2
        Atom   lhs = this.reclassifyName(location, scope, identifiers, n - 1);
        String rhs = identifiers[n - 1];

        // 6.5.2.2.1
        UnitCompiler.LOGGER.log(Level.FINE, "lhs={0}", lhs);
        if (lhs instanceof Package) {
            String className = ((Package) lhs).name + '.' + rhs;
            IClass result    = this.findTypeByName(location, className);
            if (result != null) return new SimpleType(location, result);

            return new Package(location, className);
        }

        // 6.5.2.2.3.2 EXPRESSION.length
        if ("length".equals(rhs) && this.getType(lhs).isArray()) {
            ArrayLength al = new ArrayLength(location, this.toRvalueOrCompileException(lhs));
            if (!(scope instanceof BlockStatement)) {
                this.compileError("\".length\" only allowed in expression context");
                return al;
            }
            al.setEnclosingScope(scope);
            return al;
        }

        IClass lhsType = this.getType(lhs);

        // Notice: Don't need to check for 6.5.2.2.2.1 TYPE.METHOD and 6.5.2.2.3.1 EXPRESSION.METHOD here because that
        // has been done before.

        {
            IClass.IField field = this.findIField(lhsType, rhs, location);
            if (field != null) {
                // 6.5.2.2.2.2 TYPE.FIELD
                // 6.5.2.2.3.2 EXPRESSION.FIELD
                FieldAccess fa = new FieldAccess(
                    location,
                    lhs,
                    field
                );
                fa.setEnclosingScope(scope);
                return fa;
            }
        }

        IClass[] classes = lhsType.getDeclaredIClasses();
        for (final IClass memberType : classes) {
            String name = Descriptor.toClassName(memberType.getDescriptor());
            name = name.substring(name.lastIndexOf('$') + 1);
            if (name.equals(rhs)) {

                // 6.5.2.2.2.3 TYPE.TYPE
                // 6.5.2.2.3.3 EXPRESSION.TYPE
                return new SimpleType(location, memberType);
            }
        }

        this.compileError(
            "\"" + rhs + "\" is neither a method, a field, nor a member class of \"" + lhsType + "\"",
            location
        );
        return new Atom(location) {

            @Override @Nullable public <R, EX extends Throwable> R
            accept(AtomVisitor<R, EX> visitor) { return null; }

            @Override public String
            toString() { return Java.join(identifiers, "."); }
        };
    }

    /**
     * Finds the named {@link IClass} in this compilation unit, or through the {@link #iClassLoader}.
     *
     * @param className         Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return                  {@code null} iff an {@link IClass} with that name could not be loaded
     * @throws CompileException An exception was raised while loading the {@link IClass}
     */
    @Nullable private IClass
    findTypeByName(Location location, String className) throws CompileException {

        // Is the type defined in the same compilation unit?
        IClass res = this.findClass(className);
        if (res != null) return res;

        // Is the type defined on the classpath?
        try {
            return this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
        } catch (ClassNotFoundException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CompileException) throw (CompileException) cause;
            throw new CompileException(className, location, ex);
        }
    }

    /**
     * JLS7 6.5.2.1
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String identifier) throws CompileException {

        // Determine scope block statement, type body declaration, type and compilation unit.
        TypeBodyDeclaration     scopeTbd             = null;
        AbstractTypeDeclaration scopeTypeDeclaration = null;
        CompilationUnit         scopeCompilationUnit;
        {
            Scope s = scope;
            while (
                (s instanceof BlockStatement || s instanceof CatchClause)
                && !(s instanceof TypeBodyDeclaration)
            ) s = s.getEnclosingScope();
            if (s instanceof TypeBodyDeclaration) {
                scopeTbd = (TypeBodyDeclaration) s;
                s        = s.getEnclosingScope();
            }
            if (s instanceof TypeDeclaration) {
                scopeTypeDeclaration = (AbstractTypeDeclaration) s;
                s                    = s.getEnclosingScope();
            }
            while (!(s instanceof CompilationUnit)) s = s.getEnclosingScope();
            scopeCompilationUnit = (CompilationUnit) s;
        }

        // 6.5.2.1.BL1

        // 6.5.2.BL1.B1.B1.1 (JLS7: 6.5.2.BL1.B1.B1.1) / 6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2 (JLS7: 6.5.2.BL1.B1.B1.2) / 6.5.6.1.1 Parameter.
        {
            Scope s = scope;
            if (s instanceof BlockStatement) {
                BlockStatement bs = (BlockStatement) s;
                LocalVariable  lv = bs.findLocalVariable(identifier);
                if (lv != null) {
                    LocalVariableAccess lva = new LocalVariableAccess(location, lv);
                    lva.setEnclosingScope(bs);
                    return lva;
                }
                s = s.getEnclosingScope();
            }
            while (s instanceof BlockStatement || s instanceof CatchClause) s = s.getEnclosingScope();
            if (s instanceof FunctionDeclarator) {
                s = s.getEnclosingScope();
            }
            if (s instanceof InnerClassDeclaration) {
                InnerClassDeclaration icd = (InnerClassDeclaration) s; // SUPPRESS CHECKSTYLE UsageDistance

                s = s.getEnclosingScope();
                if (s instanceof AnonymousClassDeclaration) {
                    s = s.getEnclosingScope();
                } else
                if (s instanceof FieldDeclaration) {
                    s = s.getEnclosingScope().getEnclosingScope();
                }
                while (s instanceof BlockStatement) {
                    LocalVariable lv = ((BlockStatement) s).findLocalVariable(identifier);
                    if (lv != null) {
                        if (!lv.finaL) {
                            this.compileError(
                                "Cannot access non-final local variable \""
                                + identifier
                                + "\" from inner class"
                            );
                        }
                        final IClass  lvType = lv.type;
                        IClass.IField iField = new SimpleIField(
                            this.resolve(icd),
                            "val$" + identifier,
                            lvType
                        );
                        icd.defineSyntheticField(iField);
                        FieldAccess fa = new FieldAccess(
                            location,                                   // location
                            new QualifiedThisReference(                 // lhs
                                location,                                   // location
                                new SimpleType(location, this.resolve(icd)) // qualification
                            ),
                            iField                                      // field
                        );
                        fa.setEnclosingScope(scope);
                        return fa;
                    }
                    s = s.getEnclosingScope();
                    while (s instanceof BlockStatement) s = s.getEnclosingScope();
                    if (!(s instanceof FunctionDeclarator)) break;
                    s = s.getEnclosingScope();
                    if (!(s instanceof InnerClassDeclaration)) break;
                    icd = (InnerClassDeclaration) s;
                    s   = s.getEnclosingScope();
                }
            }
        }

        // 6.5.2.BL1.B1.B1.3 (JLS7: 6.5.2.BL1.B1.B1.3) / 6.5.6.1.2.1 Field.
        BlockStatement enclosingBlockStatement = null;
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof BlockStatement && enclosingBlockStatement == null) {
                enclosingBlockStatement = (BlockStatement) s;
            }
            if (s instanceof TypeDeclaration) {
                final AbstractTypeDeclaration enclosingTypeDecl = (AbstractTypeDeclaration) s;
                final IClass                  etd               = this.resolve(enclosingTypeDecl);
                final IClass.IField           f                 = this.findIField(etd, identifier, location);
                if (f != null) {
                    if (f.isStatic()) {
                        this.warning("IASF", (
                            "Implicit access to static field \""
                            + identifier
                            + "\" of declaring class (better write \""
                            + f.getDeclaringIClass()
                            + '.'
                            + f.getName()
                            + "\")"
                        ), location);
                    } else
                    if (f.getDeclaringIClass() == etd) {
                        this.warning("IANSF", (
                            "Implicit access to non-static field \""
                            + identifier
                            + "\" of declaring class (better write \"this."
                            + f.getName()
                            + "\")"
                        ), location);
                    } else {
                        this.warning("IANSFEI", (
                            "Implicit access to non-static field \""
                            + identifier
                            + "\" of enclosing instance (better write \""
                            + f.getDeclaringIClass()
                            + ".this."
                            + f.getName()
                            + "\")"
                        ), location);
                    }

                    assert scopeTypeDeclaration != null;
                    SimpleType ct = new SimpleType(scopeTypeDeclaration.getLocation(), etd);

                    Atom lhs;
                    if (scopeTbd == null) {

                        // Field access in top-level type declaration context (member annotation).
                        lhs = ct;
                    } else
                    if (scopeTbd instanceof MethodDeclarator && ((MethodDeclarator) scopeTbd).isStatic()) {

                        // Field access in static method context.
                        lhs = ct;
                    } else
                    {

                        // Field access in non-static method context.
                        if (f.isStatic()) {

                            // Access to static field.
                            lhs = ct;
                        } else {

                            // Access to non-static field.
                            lhs = new QualifiedThisReference(location, ct);
                        }
                    }

                    Rvalue res = new FieldAccess(location, lhs, f);
                    res.setEnclosingScope(scope);

                    return res;
                }
            }
        }

        // JLS7 6.5.2.BL1.B2.1 Static field imported through single static import.
        for (IField f : Iterables.filterByClass(this.importSingleStatic(identifier), IField.class)) {

            if (this.isAccessible(f, scope)) {

                FieldAccess fieldAccess = new FieldAccess(
                    location,
                    new SimpleType(location, f.getDeclaringIClass()),
                    f
                );
                fieldAccess.setEnclosingScope(scope);
                return fieldAccess;
            }
        }

        // JLS7 6.5.2.BL1.B2.2 Static field imported through static-import-on-demand.
        for (IField f : Iterables.filterByClass(this.importStaticOnDemand(identifier), IField.class)) {

            if (this.isAccessible(f, scope)) {

                FieldAccess fieldAccess = new FieldAccess(
                    location,
                    new SimpleType(location, f.getDeclaringIClass()),
                    f
                );
                fieldAccess.setEnclosingScope(scope);
                return fieldAccess;
            }
        }

        // Hack: "java" MUST be a package, not a class.
        if ("java".equals(identifier)) return new Package(location, identifier);

        // JLS7: 6.5.2.BL1.B3.1 Unnamed package class
        // JLS7: 6.5.2.BL1.B3.2 Unnamed package interface
        // JLS7: 7.4.2
        {
            IClass unnamedPackageType = this.findTypeByName(location, identifier);
            if (unnamedPackageType != null) return new SimpleType(location, unnamedPackageType);
        }

        // 6.5.2.BL1.B1.B2.1 (JLS7: 6.5.2.BL1.B3.3) Local class.
        {
            LocalClassDeclaration lcd = UnitCompiler.findLocalClassDeclaration(scope, identifier);
            if (lcd != null) return new SimpleType(location, this.resolve(lcd));
        }

        // 6.5.2.BL1.B1.B2.2 (JLS7: 6.5.2.BL1.B3.4) Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = this.findMemberType(
                this.resolve(scopeTypeDeclaration),
                identifier,
                location
            );
            if (memberType != null) return new SimpleType(location, memberType);
        }

        // 6.5.2.BL1.B1.B3.1 (JLS7: 6.5.2.BL1.B1.B4.1) Single type import.
        {
            IClass iClass = this.importSingleType(identifier, location);
            if (iClass != null) return new SimpleType(location, iClass);
        }

        // 6.5.2.BL1.B1.B3.2 (JLS7: 6.5.2.BL1.B1.B3.1) Package member class/interface declared in this compilation
        // unit.
        // Notice that JLS2 looks this up AFTER local class, member type, single type import, while JLS3 looks this up
        // BEFORE local class, member type, single type import.
        {
            PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(identifier);
            if (pmtd != null) return new SimpleType(location, this.resolve(pmtd));
        }

        // 6.5.2.BL1.B1.B4 Class or interface declared in same package.
        // Notice: Why is this missing in JLS3?
        {
            PackageDeclaration opd = scopeCompilationUnit.packageDeclaration;

            String className = opd == null ? identifier : opd.packageName + '.' + identifier;

            IClass result = this.findTypeByName(location, className);
            if (result != null) return new SimpleType(location, result);
        }

        // 6.5.2.BL1.B1.B5 (JLS7: 6.5.2.BL1.B1.B4.2), 6.5.2.BL1.B1.B6 Type-import-on-demand.
        {
            IClass importedClass = this.importTypeOnDemand(identifier, location);
            if (importedClass != null) return new SimpleType(location, importedClass);
        }

        // JLS7 6.5.2.BL1.B1.B4.3 Type imported through single static import.
        {
            Iterator<IClass>
            it = Iterables.filterByClass(this.importSingleStatic(identifier).iterator(), IClass.class);
            if (it.hasNext()) return new SimpleType(location, (IClass) it.next());
        }

        // JLS7 6.5.2.BL1.B1.B4.4 Member type imported through static-import-on-demand.
        for (IClass mt : Iterables.filterByClass(this.importStaticOnDemand(identifier), IClass.class)) {
            if (this.isAccessible(mt, scope)) return new SimpleType(location, mt);
        }

        // 6.5.2.BL1.B1.B7 Package name
        return new Package(location, identifier);
    }

    /**
     * Imports a member class, member interface, static field or static method via the compilation unit's single
     * static import declarations.
     *
     * @return A list of {@link IField}s, {@link IMethod}s and/or {@link IClass}es with that <var>simpleName</var>;
     *         may be empty
     */
    private List<Object>
    importSingleStatic(String simpleName) throws CompileException {

        List<Object> result = new ArrayList<Object>();
        for (SingleStaticImportDeclaration ssid : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            SingleStaticImportDeclaration.class
        )) {

            if (simpleName.equals(UnitCompiler.last(ssid.identifiers))) {

                IClass declaringIClass = this.findTypeByName(
                    ssid.getLocation(),
                    Java.join(UnitCompiler.allButLast(ssid.identifiers), ".")
                );
                if (declaringIClass != null) {
                    this.importStatic(declaringIClass, simpleName, result, ssid.getLocation());
                }
            }
        }

        return result;
    }

    /**
     * Finds all members (member classes, member interfaces, static fields and/or static methods) of the
     * <var>declaringIClass</var> with the given <var>simpleName</var> and adds them to the <var>result</var>.
     *
     * @param declaringIClass The class or interface that declares the members
     * @param result          Results ({@link IClass}es, {@link IField}s and/or {@link IMethod}s) are added to this
     *                        collection
     */
    private void
    importStatic(IClass declaringIClass, String simpleName, Collection<Object> result, Location location)
    throws CompileException {

        // Member type?
        for (IClass memberIClass : declaringIClass.findMemberType(simpleName)) {
            if (memberIClass.getDeclaringIClass() == declaringIClass) result.add(memberIClass);
        }

        // Static field?
        {
            IField iField = declaringIClass.getDeclaredIField(simpleName);
            if (iField != null) {
                if (!iField.isStatic()) {
                    this.compileError(
                        "Field \"" + simpleName + "\" of \"" + declaringIClass + "\" must be static",
                        location
                    );
                }
                result.add(iField);
            }
        }

        // Static method?
        for (IMethod iMethod : declaringIClass.getDeclaredIMethods(simpleName)) {
            if (!iMethod.isStatic()) {
                this.compileError(
                    "method \"" + iMethod + "\" of \"" + declaringIClass + "\" must be static",
                    location
                );
            }
            result.add(iMethod);
        }
    }

    /**
     * @return Either the {@link FieldAccess} or an {@link ArrayLength}
     */
    private Rvalue
    determineValue(FieldAccessExpression fae) throws CompileException {
        if (fae.value != null) return fae.value;

        IClass lhsType = this.getType(fae.lhs);

        Rvalue value;
        if (fae.fieldName.equals("length") && lhsType.isArray()) {
            value = new ArrayLength(
                fae.getLocation(),
                this.toRvalueOrCompileException(fae.lhs)
            );
        } else {
            IClass.IField iField = this.findIField(lhsType, fae.fieldName, fae.getLocation());
            if (iField == null) {
                this.compileError(
                    "\"" + this.getType(fae.lhs).toString() + "\" has no field \"" + fae.fieldName + "\"",
                    fae.getLocation()
                );
                value = new Rvalue(fae.getLocation()) {

                    @Override @Nullable public <R, EX extends Throwable> R
                    accept(RvalueVisitor<R, EX> visitor) { return null; }

                    @Override public String
                    toString() { return "???"; }
                };
            } else {
                value = new FieldAccess(
                    fae.getLocation(),
                    fae.lhs,
                    iField
                );
            }
        }

        value.setEnclosingScope(fae.getEnclosingScope());

        return (fae.value = value);
    }

    /**
     * "super.fld", "Type.super.fld"
     */
    private Rvalue
    determineValue(SuperclassFieldAccessExpression scfae) throws CompileException {
        if (scfae.value != null) return scfae.value;

        Rvalue lhs;
        {
            ThisReference tr = new ThisReference(scfae.getLocation());
            tr.setEnclosingScope(scfae.getEnclosingScope());
            IClass type;
            if (scfae.qualification != null) {
                type = this.getType(scfae.qualification);
            } else
            {
                type = this.getType(tr);
            }

            IClass superclass = type.getSuperclass();
            if (superclass == null) {
                throw new CompileException("Cannot use \"super\" on \"" + type + "\"", scfae.getLocation());
            }

            lhs = new Cast(scfae.getLocation(), new SimpleType(scfae.getLocation(), superclass), tr);
        }

        Rvalue value;

        IClass.IField iField = this.findIField(this.getType(lhs), scfae.fieldName, scfae.getLocation());
        if (iField == null) {
            this.compileError("Class has no field \"" + scfae.fieldName + "\"", scfae.getLocation());
            value = new Rvalue(scfae.getLocation()) {

                @Override @Nullable public <R, EX extends Throwable> R
                accept(RvalueVisitor<R, EX> visitor) { return null; }

                @Override public String
                toString() { return "???"; }
            };
        } else {
            value = new FieldAccess(
                scfae.getLocation(),
                lhs,
                iField
            );
        }
        value.setEnclosingScope(scfae.getEnclosingScope());

        return (scfae.value = value);
    }

    /**
     * Finds methods of the <var>mi</var>{@code .}{@link MethodInvocation#target
     * target} named <var>mi</var>{@code .}{@link Invocation#methodName methodName},
     * examines the argument types and chooses the most specific method. Checks that only the
     * allowed exceptions are thrown.
     * <p>
     *   Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     * </p>
     *
     * @return The selected {@link IClass.IMethod}
     */
    public IClass.IMethod
    findIMethod(MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod;
        FIND_METHOD: {

            Atom ot = mi.target;
            if (ot == null) {

                // Method invocation by simple method name... method must be declared by an enclosing type declaration.
                for (Scope s = mi.getEnclosingScope(); !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
                    if (s instanceof TypeDeclaration) {
                        TypeDeclaration td = (TypeDeclaration) s;

                        // Find methods with specified name.
                        iMethod = this.findIMethod(
                            this.resolve(td),  // targetType
                            mi                 // invocation
                        );
                        if (iMethod != null) break FIND_METHOD;
                    }
                }
            } else
            {

                // Method invocation by "target": "expr.meth(arguments)" -- method must be declared by the target's
                // type.
                iMethod = this.findIMethod(
                    this.getType(ot), // targetType
                    mi                // invocable
                );
                if (iMethod != null) break FIND_METHOD;
            }

            // Static method declared through single static import?
            {
                IMethod[] candidates = (IMethod[]) Iterables.toArray(
                    Iterables.filterByClass(this.importSingleStatic(mi.methodName), IMethod.class),
                    IMethod.class
                );

                if (candidates.length > 0) {
                    iMethod = (IMethod) this.findMostSpecificIInvocable(
                        mi,
                        candidates,
                        mi.arguments,
                        mi.getEnclosingScope()
                    );
                    break FIND_METHOD;
                }
            }

            // Static method declared through static-import-on-demand?
            {
                IMethod[] candidates = (IMethod[]) Iterables.toArray(
                    Iterables.filterByClass(this.importStaticOnDemand(mi.methodName), IMethod.class),
                    IMethod.class
                );

                if (candidates.length > 0) {
                    iMethod = (IMethod) this.findMostSpecificIInvocable(
                        mi,
                        candidates,
                        mi.arguments,
                        mi.getEnclosingScope()
                    );
                    break FIND_METHOD;
                }
            }

            this.compileError((
                "A method named \""
                + mi.methodName
                + "\" is not declared in any enclosing class nor any supertype, nor through a static import"
            ), mi.getLocation());
            return this.fakeIMethod(this.iClassLoader.TYPE_java_lang_Object, mi.methodName, mi.arguments);
        }

        assert iMethod != null; // Don't know why JAVAC thinks "iMethod" could be null here!?

        this.checkThrownExceptions(mi, iMethod);
        return iMethod;
    }

    /**
     * Finds a {@link IClass.IMethod} in the given <var>targetType</var>, its superclasses or superinterfaces which is
     * applicable with the given <var>invocation</var>. If more than one such method exists, chooses the most
     * specific one (JLS7 15.11.2).
     *
     * @return {@code null} if no appropriate method could be found
     */
    @Nullable private IClass.IMethod
    findIMethod(IClass targetType, Invocation invocation) throws CompileException {

        // Get all methods.
        List<IClass.IMethod> ms = new ArrayList<IClass.IMethod>();
        this.getIMethods(targetType, invocation.methodName, ms);

        // Interfaces inherit the methods declared in 'Object'.
        if (targetType.isInterface()) {
            IClass.IMethod[] oms = this.iClassLoader.TYPE_java_lang_Object.getDeclaredIMethods(invocation.methodName);
            for (IMethod om : oms) {
                if (!om.isStatic() && om.getAccess() == Access.PUBLIC) ms.add(om);
            }
        }

        if (ms.size() == 0) return null;

        // Determine arguments' types, choose the most specific method.
        return (IClass.IMethod) this.findMostSpecificIInvocable(
            invocation,                                                   // locatable
            (IClass.IMethod[]) ms.toArray(new IClass.IMethod[ms.size()]), // iInvocables
            invocation.arguments,                                         // arguments
            invocation.getEnclosingScope()                                // contextScope
        );
    }

    private IMethod
    fakeIMethod(IClass targetType, final String name, Rvalue[] arguments) throws CompileException {
        final IClass[] pts = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) pts[i] = this.getType(arguments[i]);
        return targetType.new IMethod() {
            @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
            @Override public Access        getAccess()            { return Access.PUBLIC;      }
            @Override public boolean       isStatic()             { return false;              }
            @Override public boolean       isAbstract()           { return false;              }
            @Override public IClass        getReturnType()        { return IClass.INT;         }
            @Override public String        getName()              { return name;               }
            @Override public IClass[]      getParameterTypes2()   { return pts;                }
            @Override public boolean       isVarargs()            { return false;              }
            @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
        };
    }

    /**
     * Adds all methods with the given <var>methodName</var> that are declared by the <var>type</var>, its superclasses
     * and all their superinterfaces to the result list <var>v</var>.
     */
    public void
    getIMethods(IClass type, String methodName, List<IMethod> v) throws CompileException {

        // Check methods declared by this type.
        {
            IClass.IMethod[] ims = type.getDeclaredIMethods(methodName);
            for (IMethod im : ims) v.add(im);
        }

        // Check superclass.
        IClass superclass = type.getSuperclass();
        if (superclass != null) this.getIMethods(superclass, methodName, v);

        // Check superinterfaces.
        IClass[] interfaces = type.getInterfaces();
        for (IClass interfacE : interfaces) this.getIMethods(interfacE, methodName, v);
    }

    /**
     * @return The {@link IClass.IMethod} that implements the <var>superclassMethodInvocation</var>
     */
    public IClass.IMethod
    findIMethod(SuperclassMethodInvocation superclassMethodInvocation) throws CompileException {
        AbstractClassDeclaration declaringClass;
        for (Scope s = superclassMethodInvocation.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) s;
                if (fd instanceof MethodDeclarator && ((MethodDeclarator) fd).isStatic()) {
                    this.compileError(
                        "Superclass method cannot be invoked in static context",
                        superclassMethodInvocation.getLocation()
                    );
                }
            }
            if (s instanceof AbstractClassDeclaration) {
                declaringClass = (AbstractClassDeclaration) s;
                break;
            }
        }

        IClass superclass = this.resolve(declaringClass).getSuperclass();
        if (superclass == null) {
            throw new CompileException(
                "\"" + declaringClass + "\" has no superclass",
                superclassMethodInvocation.getLocation()
            );
        }

        IMethod iMethod = this.findIMethod(
            superclass,                // targetType
            superclassMethodInvocation // invocation
        );
        if (iMethod == null) {
            this.compileError(
                "Class \"" + superclass + "\" has no method named \"" + superclassMethodInvocation.methodName + "\"",
                superclassMethodInvocation.getLocation()
            );
            return this.fakeIMethod(
                superclass,
                superclassMethodInvocation.methodName,
                superclassMethodInvocation.arguments
            );
        }
        this.checkThrownExceptions(superclassMethodInvocation, iMethod);
        return iMethod;
    }

    /**
     * Determines the arguments' types, determine the applicable invocables and choose the most specific invocable
     * and adjust arguments as needed (for varargs case).
     *
     * @param iInvocables       Length must be greater than zero
     * @return                  The selected {@link IClass.IInvocable}
     */
    private IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        final IInvocable[] iInvocables,
        final Rvalue[]     arguments,
        Scope              contextScope
    ) throws CompileException {

        // Determine arguments' types.
        final IClass[] argumentTypes = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = this.getType(arguments[i]);
        }

        // Determine most specific invocable WITHOUT boxing.
        IInvocable ii = this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, false, contextScope);
        if (ii != null) return ii;

        // Determine most specific invocable WITH boxing.
        ii = this.findMostSpecificIInvocable(locatable, iInvocables, argumentTypes, true, contextScope);
        if (ii != null) return ii;

        // Report a nice compile error.
        StringBuilder sb = new StringBuilder("No applicable constructor/method found for ");
        if (argumentTypes.length == 0) {
            sb.append("zero actual parameters");
        } else {
            sb.append("actual parameters \"").append(argumentTypes[0]);
            for (int i = 1; i < argumentTypes.length; ++i) {
                sb.append(", ").append(argumentTypes[i]);
            }
            sb.append("\"");
        }
        sb.append("; candidates are: \"").append(iInvocables[0]).append('"');
        for (int i = 1; i < iInvocables.length; ++i) {
            sb.append(", \"").append(iInvocables[i]).append('"');
        }
        this.compileError(sb.toString(), locatable.getLocation());

        // Well, returning a "fake" IInvocable is a bit tricky, because the iInvocables can be of different types.
        if (iInvocables[0] instanceof IClass.IConstructor) {
            return iInvocables[0].getDeclaringIClass().new IConstructor() {
                @Override public boolean       isVarargs()            { return false;              }
                @Override public IClass[]      getParameterTypes2()   { return argumentTypes;      }
                @Override public Access        getAccess()            { return Access.PUBLIC;      }
                @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
                @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
            };
        } else
        if (iInvocables[0] instanceof IClass.IMethod) {
            final String methodName = ((IClass.IMethod) iInvocables[0]).getName();
            return iInvocables[0].getDeclaringIClass().new IMethod() {

                @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                @Override public Access        getAccess()            { return Access.PUBLIC;      }
                @Override public boolean       isStatic()             { return true;               }
                @Override public boolean       isAbstract()           { return false;              }
                @Override public IClass        getReturnType()        { return IClass.INT;         }
                @Override public String        getName()              { return methodName;         }
                @Override public IClass[]      getParameterTypes2()   { return argumentTypes;      }
                @Override public boolean       isVarargs()            { return false;              }
                @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }
            };
        } else
        {
            return iInvocables[0];
        }
    }

    /**
     * Determines the applicable invocables and choose the most specific invocable.
     *
     * @return The maximally specific {@link IClass.IInvocable} or {@code null} if no {@link IClass.IInvocable} is
     *         applicable
     */
    @Nullable public IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        final IInvocable[] iInvocables,
        IClass[]           argumentTypes,
        boolean            boxingPermitted,
        Scope              contextScope
    ) throws CompileException {
        if (UnitCompiler.LOGGER.isLoggable(Level.FINER)) {
            UnitCompiler.LOGGER.entering(null, "findMostSpecificIInvocable", new Object[] {
                locatable, Arrays.toString(iInvocables), Arrays.toString(argumentTypes), boxingPermitted, contextScope
            });
        }

        // Select applicable methods (15.12.2.1).
        List<IClass.IInvocable> applicableIInvocables = new ArrayList<IClass.IInvocable>();
        List<IClass.IInvocable> varargApplicables     = new ArrayList<IClass.IInvocable>();

        NEXT_METHOD:
        for (IClass.IInvocable ii : iInvocables) {
            boolean argsNeedAdjust = false;

            // Ignore inaccessible invocables.
            if (!this.isAccessible(ii, contextScope)) continue;

            // Check parameter count.
            final IClass[] parameterTypes   = ii.getParameterTypes();
            int            formalParamCount = parameterTypes.length;
            int            nUncheckedArg    = argumentTypes.length;
            final boolean  isVarargs        = ii.isVarargs();

            // Match the last formal parameter with all args starting from that index (or none).
            VARARGS:
            if (isVarargs) {

                // Decrement the count to get the index.
                formalParamCount--;
                final IClass lastParamType = parameterTypes[formalParamCount].getComponentType();
                assert lastParamType != null;

                final int lastActualArg = nUncheckedArg - 1;

                // If the two have the same argCount and the last actual arg is an array of the same type accept it
                // (e.g. "void foo(int a, double...b) VS foo(1, new double[0]").
                if (
                    formalParamCount == lastActualArg
                    && argumentTypes[lastActualArg].isArray()
                    && this.isMethodInvocationConvertible(
                        (IClass) UnitCompiler.assertNonNull(argumentTypes[lastActualArg].getComponentType()),
                        lastParamType,
                        boxingPermitted
                    )
                ) {
                    nUncheckedArg--;
                } else {
                    for (int idx = lastActualArg; idx >= formalParamCount; --idx) {

                        // Is method invocation conversion possible (5.3)?
                        UnitCompiler.LOGGER.log(
                            Level.FINE,
                            "{0} <=> {1}",
                            new Object[] { lastParamType, argumentTypes[idx] }
                        );
                        if (!this.isMethodInvocationConvertible(argumentTypes[idx], lastParamType, boxingPermitted)) {
                            formalParamCount++;
                            break VARARGS;
                        }

                        nUncheckedArg--;
                    }
                    argsNeedAdjust = true;
                }
            }

            if (formalParamCount == nUncheckedArg) {
                for (int j = 0; j < nUncheckedArg; ++j) {
                    UnitCompiler.LOGGER.log(
                        Level.FINE,
                        "{0}: {1} <=> {2}",
                        new Object[] { j, parameterTypes[j], argumentTypes[j] }
                    );

                    // Is method invocation conversion possible (5.3)?
                    if (!this.isMethodInvocationConvertible(argumentTypes[j], parameterTypes[j], boxingPermitted)) {
                        continue NEXT_METHOD;
                    }
                }

                // Applicable!
                UnitCompiler.LOGGER.fine("Applicable!");

                // Varargs has lower priority.
                if (isVarargs) {
                    ii.setArgsNeedAdjust(argsNeedAdjust);
                    varargApplicables.add(ii);
                } else {
                    applicableIInvocables.add(ii);
                }
            }
        }

        // Choose the most specific invocable (15.12.2.2).
        if (applicableIInvocables.size() == 1) {
            return (IInvocable) applicableIInvocables.get(0);
        }

        // No method found by previous phase(s).
        if (applicableIInvocables.size() == 0 && !varargApplicables.isEmpty()) {
            //TODO: 15.12.2.3 (type-conversion?)

            // 15.12.2.4 : Phase 3: Identify Applicable Variable Arity Methods
            applicableIInvocables = varargApplicables;
            if (applicableIInvocables.size() == 1) {
                return (IInvocable) applicableIInvocables.get(0);
            }
        }

        if (applicableIInvocables.size() == 0) return null;

        // 15.12.2.5. Determine the "maximally specific invocables".
        List<IClass.IInvocable> maximallySpecificIInvocables = new ArrayList<IClass.IInvocable>();
        for (IClass.IInvocable applicableIInvocable : applicableIInvocables) {
            int moreSpecific = 0, lessSpecific = 0;
            for (IClass.IInvocable mostSpecificIInvocable : maximallySpecificIInvocables) {
                if (applicableIInvocable.isMoreSpecificThan(mostSpecificIInvocable)) {
                    ++moreSpecific;
                } else
                if (applicableIInvocable.isLessSpecificThan(mostSpecificIInvocable)) {
                    ++lessSpecific;
                }
            }
            if (moreSpecific == maximallySpecificIInvocables.size()) {
                maximallySpecificIInvocables.clear();
                maximallySpecificIInvocables.add(applicableIInvocable);
            } else
            if (lessSpecific < maximallySpecificIInvocables.size()) {
                maximallySpecificIInvocables.add(applicableIInvocable);
            } else
            {
                ;
            }
            UnitCompiler.LOGGER.log(Level.FINE, "maximallySpecificIInvocables={0}", maximallySpecificIInvocables);
        }

        if (maximallySpecificIInvocables.size() == 1) return (IInvocable) maximallySpecificIInvocables.get(0);

        ONE_NON_ABSTRACT_INVOCABLE:
        if (maximallySpecificIInvocables.size() > 1 && iInvocables[0] instanceof IClass.IMethod) {

            // Check if all methods have the same signature (i.e. the types of all their parameters are identical) and
            // exactly one of the methods is non-abstract (JLS7 15.12.2.2.BL2.B1).
            IClass.IMethod theNonAbstractMethod = null;
            {
                Iterator<IClass.IInvocable> it                          = maximallySpecificIInvocables.iterator();
                IClass.IMethod              m                           = (IClass.IMethod) it.next();
                final IClass[]              parameterTypesOfFirstMethod = m.getParameterTypes();
                for (;;) {
                    if (!m.isAbstract() && !m.getDeclaringIClass().isInterface()) {
                        if (theNonAbstractMethod == null) {
                            theNonAbstractMethod = m;
                        } else {
                            IClass declaringIClass                     = m.getDeclaringIClass();
                            IClass theNonAbstractMethodDeclaringIClass = theNonAbstractMethod.getDeclaringIClass();
                            if (declaringIClass == theNonAbstractMethodDeclaringIClass) {
                                if (m.getReturnType() == theNonAbstractMethod.getReturnType()) {

                                    // JLS8 15.12.2.5.B9: "Otherwise, the method invocation is ambiguous, and a
                                    // compile-time error occurs."
                                    throw new InternalCompilerException(
                                        "Two non-abstract methods \"" + m + "\" have the same parameter types, "
                                        + "declaring type and return type"
                                    );
                                } else
                                if (m.getReturnType().isAssignableFrom(theNonAbstractMethod.getReturnType())) {
                                    ;
                                } else
                                if (theNonAbstractMethod.getReturnType().isAssignableFrom(m.getReturnType())) {
                                    theNonAbstractMethod = m;
                                } else
                                {
                                    throw new InternalCompilerException("Incompatible return types");
                                }
                            } else
                            if (declaringIClass.isAssignableFrom(theNonAbstractMethodDeclaringIClass)) {
                                ;
                            } else
                            if (theNonAbstractMethodDeclaringIClass.isAssignableFrom(declaringIClass)) {
                                theNonAbstractMethod = m;
                            } else
                            {
                                this.compileError(
                                    "Ambiguous static method import: \""
                                    + theNonAbstractMethod
                                    + "\" vs. \""
                                    + m
                                    + "\""
                                );
                            }
                        }
                    }
                    if (!it.hasNext()) break;

                    m = (IClass.IMethod) it.next();
                    IClass[] pts = m.getParameterTypes();
                    for (int i = 0; i < pts.length; ++i) {
                        if (pts[i] != parameterTypesOfFirstMethod[i]) break ONE_NON_ABSTRACT_INVOCABLE;
                    }
                }
            }

            // JLS7 15.12.2.2.BL2.B1.B1
            if (theNonAbstractMethod != null) return theNonAbstractMethod;

            // JLS7 15.12.2.2.BL2.B1.B2
            // Check "that exception [te1] is declared in the THROWS clause of each of the maximally specific methods".
            Set<IClass> s = new HashSet<IClass>();
            {
                IClass[][]                  tes = new IClass[maximallySpecificIInvocables.size()][];
                Iterator<IClass.IInvocable> it  = maximallySpecificIInvocables.iterator();
                for (int i = 0; i < tes.length; ++i) {
                    tes[i] = ((IClass.IMethod) it.next()).getThrownExceptions();
                }
                for (int i = 0; i < tes.length; ++i) {
                    EACH_EXCEPTION:
                    for (IClass te1 : tes[i]) {
                        EACH_METHOD:
                        for (int k = 0; k < tes.length; ++k) {
                            if (k == i) continue;
                            for (IClass te2 : tes[k]) {
                                if (te2.isAssignableFrom(te1)) continue EACH_METHOD;
                            }
                            continue EACH_EXCEPTION;
                        }
                        s.add(te1);
                    }
                }
            }

            // JLS7 1.12.2.5.BL2.B1.BL.B2: "... the most specific method is chosen arbitrarily among the subset of the
            // maximally specific methods that have the most specific return type".
            final IClass.IMethod im;
            {
                Iterator<IInvocable> it2                              = maximallySpecificIInvocables.iterator();
                IClass.IMethod       methodWithMostSpecificReturnType = (IMethod) it2.next();

                while (it2.hasNext()) {
                    IMethod im2 = (IMethod) it2.next();
                    if (methodWithMostSpecificReturnType.getReturnType().isAssignableFrom(im2.getReturnType())) {
                        methodWithMostSpecificReturnType = im2;
                    }
                }
                im = methodWithMostSpecificReturnType;
            }

            // Return a "dummy" method.
            final IClass[]       tes = (IClass[]) s.toArray(new IClass[s.size()]);
            return im.getDeclaringIClass().new IMethod() {

                @Override public IAnnotation[] getAnnotations()                             { return im.getAnnotations();    } // SUPPRESS CHECKSTYLE LineLength:9
                @Override public Access        getAccess()                                  { return im.getAccess();         }
                // JLS8 15.12.2.5.B8.B2: "In this case, the most specific method is considered to be abstract"
                @Override public boolean       isAbstract()                                 { return true;                   }
                @Override public boolean       isStatic()                                   { return im.isStatic();          }
                @Override public IClass        getReturnType()      throws CompileException { return im.getReturnType();     }
                @Override public String        getName()                                    { return im.getName();           }
                @Override public IClass[]      getParameterTypes2() throws CompileException { return im.getParameterTypes(); }
                @Override public boolean       isVarargs()                                  { return im.isVarargs();         }
                @Override public IClass[]      getThrownExceptions2()                       { return tes;                    }
            };
        }

        if (!boxingPermitted) return null; // To try again.

        // JLS7 15.12.2.2.BL2.B2
        {
            StringBuilder sb = new StringBuilder("Invocation of constructor/method with argument type(s) \"");
            for (int i = 0; i < argumentTypes.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(Descriptor.toString(argumentTypes[i].getDescriptor()));
            }
            sb.append("\" is ambiguous: ");
            for (int i = 0; i < maximallySpecificIInvocables.size(); ++i) {
                if (i > 0) sb.append(" vs. ");
                sb.append("\"" + maximallySpecificIInvocables.get(i) + "\"");
            }
            this.compileError(sb.toString(), locatable.getLocation());
        }

        return iInvocables[0];
    }

    private static <T> T
    assertNonNull(@Nullable T subject) {
        assert subject != null;
        return subject;
    }

    /**
     * Checks if "method invocation conversion" (5.3) is possible.
     */
    private boolean
    isMethodInvocationConvertible(
        IClass  sourceType,
        IClass  targetType,
        boolean boxingPermitted
    ) throws CompileException {

        // 5.3 Identity conversion.
        if (sourceType == targetType) return true;

        // 5.3 Widening primitive conversion.
        if (this.isWideningPrimitiveConvertible(sourceType, targetType)) return true;

        // 5.3 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return true;

        // JLS7 5.3 A boxing conversion (JLS7 5.1.7) optionally followed by widening reference conversion.
        if (boxingPermitted) {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                return (
                    this.isIdentityConvertible(boxedType, targetType)
                    || this.isWideningReferenceConvertible(boxedType, targetType)
                );
            }
        }

        // JLS7 5.3 An unboxing conversion (JLS7 5.1.8) optionally followed by a widening primitive conversion.
        if (boxingPermitted) {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                return (
                    this.isIdentityConvertible(unboxedType, targetType)
                    || this.isWideningPrimitiveConvertible(unboxedType, targetType)
                );
            }
        }

        // 5.3 TODO: FLOAT or DOUBLE value set conversion

        return false;
    }

    /**
     * @throws CompileException if the {@link Invocation} throws exceptions that are disallowed in the given scope
     */
    private void
    checkThrownExceptions(Invocation in, IMethod iMethod) throws CompileException {
        IClass[] thrownExceptions = iMethod.getThrownExceptions();
        for (IClass thrownException : thrownExceptions) {
            this.checkThrownException(
                in,                    // locatable
                thrownException,       // type
                in.getEnclosingScope() // scope
            );
        }
    }

    /**
     * @throws CompileException The exception with the given <var>type</var> must not be thrown in the given
     *                          <var>scope</var>
     */
    private void
    checkThrownException(Locatable locatable, IClass type, Scope scope) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!this.iClassLoader.TYPE_java_lang_Throwable.isAssignableFrom(type)) {
            this.compileError(
                "Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"",
                locatable.getLocation()
            );
        }

        // "RuntimeException" and "Error" are never checked.
        if (
            this.iClassLoader.TYPE_java_lang_RuntimeException.isAssignableFrom(type)
            || this.iClassLoader.TYPE_java_lang_Error.isAssignableFrom(type)
        ) return;

        for (;; scope = scope.getEnclosingScope()) {

            // Match against enclosing "try...catch" blocks.
            if (scope instanceof TryStatement) {
                TryStatement ts = (TryStatement) scope;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    CatchClause cc         = (CatchClause) ts.catchClauses.get(i);
                    for (Type ct : cc.catchParameter.types) {
                        IClass caughtType = this.getType(ct);

                        if (caughtType.isAssignableFrom(type)) {

                            // This catch clause definitely catches the exception.
                            cc.reachable = true;
                            return;
                        }

                        CATCH_SUBTYPE:
                        if (type.isAssignableFrom(caughtType)) {

                            // This catch clause catches only a subtype of the exception type.
                            for (int j = 0; j < i; ++j) {
                                for (Type ct2 : ((CatchClause) ts.catchClauses.get(j)).catchParameter.types) {
                                    if (this.getType(ct2).isAssignableFrom(caughtType)) {

                                        // A preceding catch clause is more general than this catch clause.
                                        break CATCH_SUBTYPE;
                                    }
                                }
                            }

                            // This catch clause catches PART OF the actual exceptions.
                            cc.reachable = true;
                        }
                    }
                }
            } else

            // Match against "throws" clause of declaring function.
            if (scope instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) scope;
                for (Type thrownException : fd.thrownExceptions) {
                    if (this.getType(thrownException).isAssignableFrom(type)) return;
                }
                break;
            } else

            if (scope instanceof TypeBodyDeclaration) {
                break;
            }
        }

        this.compileError((
            "Thrown exception of type \""
            + type
            + "\" is neither caught by a \"try...catch\" block "
            + "nor declared in the \"throws\" clause of the declaring function"
        ), locatable.getLocation());
    }

    private IClass
    getTargetIClass(QualifiedThisReference qtr) throws CompileException {

        if (qtr.targetIClass != null) return qtr.targetIClass;

        // Determine target type.
        return (qtr.targetIClass = this.getType(qtr.qualification));
    }

    /**
     * Checks whether the operand is an {@code int} local variable.
     *
     * @return {@code null} iff <var>c</var> is <em>not</em> an {@code int} local variable
     */
    @Nullable LocalVariable
    isIntLv(Crement c) throws CompileException {
        if (!(c.operand instanceof AmbiguousName)) return null;
        AmbiguousName an = (AmbiguousName) c.operand;

        Atom rec = this.reclassify(an);
        if (!(rec instanceof LocalVariableAccess)) return null;
        LocalVariableAccess lva = (LocalVariableAccess) rec;

        LocalVariable lv = lva.localVariable;
        if (lv.finaL) this.compileError("Must not increment or decrement \"final\" local variable", lva.getLocation());

        return lv.type == IClass.INT ? lv : null;
    }

    private IClass
    resolve(final TypeDeclaration td) {

        final AbstractTypeDeclaration atd = (AbstractTypeDeclaration) td;

        if (atd.resolvedType != null) return atd.resolvedType;

        return (atd.resolvedType = new IClass() {

//            final TypeParameter[] typeParameters = (
//                atd instanceof NamedTypeDeclaration
//                ? ((NamedTypeDeclaration) atd).getOptionalTypeParameters()
//                : null
//            );

            @Override protected IClass.IMethod[]
            getDeclaredIMethods2() {
                List<IClass.IMethod> res = new ArrayList<IClass.IMethod>(atd.getMethodDeclarations().size());
                for (MethodDeclarator md : atd.getMethodDeclarations()) {
                    res.add(UnitCompiler.this.toIMethod(md));
                }

                if (td instanceof EnumDeclaration) {

                    res.add(new IMethod() {

                        @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0]; }
                        @Override public Access        getAccess()            { return Access.PUBLIC;      }
                        @Override public boolean       isStatic()             { return true;               }
                        @Override public boolean       isAbstract()           { return false;              }
                        @Override public String        getName()              { return "values";           }
                        @Override public IClass[]      getParameterTypes2()   { return new IClass[0];      }
                        @Override public boolean       isVarargs()            { return false;              }
                        @Override public IClass[]      getThrownExceptions2() { return new IClass[0];      }

                        @Override public IClass
                        getReturnType() {
                            IClass rt = atd.resolvedType;
                            assert rt != null;
                            return rt.getArrayIClass(UnitCompiler.this.iClassLoader.TYPE_java_lang_Object);
                        }
                    });

                    res.add(new IMethod() {

                        @Override public IAnnotation[] getAnnotations()       { return new IAnnotation[0];                                                    } // SUPPRESS CHECKSTYLE LineLength:7
                        @Override public Access        getAccess()            { return Access.PUBLIC;                                                         }
                        @Override public boolean       isStatic()             { return true;                                                                  }
                        @Override public boolean       isAbstract()           { return false;                                                                 }
                        @Override public String        getName()              { return "valueOf";                                                             }
                        @Override public IClass[]      getParameterTypes2()   { return new IClass[] { UnitCompiler.this.iClassLoader.TYPE_java_lang_String }; }
                        @Override public boolean       isVarargs()            { return false;                                                                 }
                        @Override public IClass[]      getThrownExceptions2() { return new IClass[0];                                                         }

                        @Override public IClass
                        getReturnType() {
                            IClass rt = atd.resolvedType;
                            assert rt != null;
                            return rt;
                        }
                    });
                }

                return (IClass.IMethod[]) res.toArray(new IClass.IMethod[res.size()]);
            }

            @Nullable private IClass[] declaredClasses;

            @Override protected IClass[]
            getDeclaredIClasses2() {

                if (this.declaredClasses != null) return this.declaredClasses;

                Collection<MemberTypeDeclaration> mtds = td.getMemberTypeDeclarations();
                IClass[]                          mts  = new IClass[mtds.size()];
                int                               i    = 0;
                for (MemberTypeDeclaration mtd : mtds) {
                    mts[i++] = UnitCompiler.this.resolve(mtd);
                }
                return (this.declaredClasses = mts);
            }

            @Override @Nullable protected IClass
            getDeclaringIClass2() {
                Scope s = atd;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
                    if (s instanceof CompilationUnit) return null;
                }
                return UnitCompiler.this.resolve((AbstractTypeDeclaration) s.getEnclosingScope());
            }

            @Override @Nullable protected IClass
            getOuterIClass2() {
                AbstractTypeDeclaration oc = (AbstractTypeDeclaration) UnitCompiler.getOuterClass(atd);
                if (oc == null) return null;
                return UnitCompiler.this.resolve(oc);
            }

            @Override protected String
            getDescriptor2() { return Descriptor.fromClassName(atd.getClassName()); }

            @Override public boolean
            isArray() { return false; }

            @Override protected IClass
            getComponentType2() { throw new InternalCompilerException("SNO: Non-array type has no component type"); }

            @Override public boolean
            isPrimitive() { return false; }

            @Override public boolean
            isPrimitiveNumeric() { return false; }

            @Override protected IConstructor[]
            getDeclaredIConstructors2() {
                if (atd instanceof AbstractClassDeclaration) {
                    AbstractClassDeclaration acd = (AbstractClassDeclaration) atd;

                    ConstructorDeclarator[] cs     = acd.getConstructors();
                    IClass.IConstructor[]   result = new IClass.IConstructor[cs.length];
                    for (int i = 0; i < cs.length; ++i) result[i] = UnitCompiler.this.toIConstructor(cs[i]);
                    return result;
                }
                return new IClass.IConstructor[0];
            }

            @Override protected IField[]
            getDeclaredIFields2() {
                if (atd instanceof AbstractClassDeclaration) {
                    AbstractClassDeclaration cd = (AbstractClassDeclaration) atd;
                    List<IClass.IField>      l  = new ArrayList<IClass.IField>();

                    // Determine variable declarators of type declaration.
                    for (FieldDeclaration fd : Iterables.filterByClass(
                        cd.variableDeclaratorsAndInitializers,
                        FieldDeclaration.class
                    )) l.addAll(Arrays.asList(UnitCompiler.this.compileFields(fd)));

                    if (atd instanceof EnumDeclaration) {
                        EnumDeclaration ed = (EnumDeclaration) atd;

                        for (EnumConstant ec : ed.getConstants()) {
                            l.add(UnitCompiler.this.compileField(
                                ed,                                                              // declaringType
                                new Annotation[0],                                               // annotations
                                Access.PUBLIC,                                                   // access
                                true,                                                            // statiC
                                true,                                                            // finaL
                                new SimpleType(ed.getLocation(), UnitCompiler.this.resolve(ed)), // type
                                0,                                                               // brackets
                                ec.name,                                                         // name
                                null                                                             // initializer
                            ));
                        }
                    }

                    return (IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                if (atd instanceof InterfaceDeclaration) {
                    InterfaceDeclaration id = (InterfaceDeclaration) atd;
                    List<IClass.IField>  l  = new ArrayList<IClass.IField>();

                    // Determine static fields.
                    for (FieldDeclaration fd : Iterables.filterByClass(
                        id.constantDeclarations,
                        FieldDeclaration.class
                    )) l.addAll(Arrays.asList(UnitCompiler.this.compileFields(fd)));
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                {
                    throw new InternalCompilerException(
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override public IField[]
            getSyntheticIFields() {
                if (atd instanceof AbstractClassDeclaration) {
                    Collection<IClass.IField> c = ((AbstractClassDeclaration) atd).syntheticFields.values();
                    return (IField[]) c.toArray(new IField[c.size()]);
                }
                return new IField[0];
            }

            @Override @Nullable protected IClass
            getSuperclass2() throws CompileException {

                if (atd instanceof EnumDeclaration) return UnitCompiler.this.iClassLoader.TYPE_java_lang_Enum;

                if (atd instanceof AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getType(((AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? UnitCompiler.this.iClassLoader.TYPE_java_lang_Object : bt;
                }

                if (atd instanceof NamedClassDeclaration) {
                    NamedClassDeclaration ncd = (NamedClassDeclaration) atd;
                    Type                  oet = ncd.extendedType;
                    if (oet == null) return UnitCompiler.this.iClassLoader.TYPE_java_lang_Object;
                    IClass superclass = UnitCompiler.this.getType(oet);
                    if (superclass.isInterface()) {
                        UnitCompiler.this.compileError(
                            "\"" + superclass.toString() + "\" is an interface; classes can only extend a class",
                            td.getLocation()
                        );
                    }
                    return superclass;
                }

                return null;
            }

            @Override public Access
            getAccess() {

                if (atd instanceof MemberClassDeclaration)            return ((MemberClassDeclaration)            atd).getAccess(); // SUPPRESS CHECKSTYLE LineLength:3
                if (atd instanceof PackageMemberClassDeclaration)     return ((PackageMemberClassDeclaration)     atd).getAccess();
                if (atd instanceof MemberInterfaceDeclaration)        return ((MemberInterfaceDeclaration)        atd).getAccess();
                if (atd instanceof PackageMemberInterfaceDeclaration) return ((PackageMemberInterfaceDeclaration) atd).getAccess();
                if (atd instanceof AnonymousClassDeclaration)         return Access.PUBLIC;
                if (atd instanceof LocalClassDeclaration)             return Access.PUBLIC;

                throw new InternalCompilerException(atd.getClass().getName());
            }

            @Override public boolean
            isFinal() { return atd instanceof NamedClassDeclaration && ((NamedClassDeclaration) atd).isFinal(); }

            @Override protected IClass[]
            getInterfaces2() throws CompileException {
                if (atd instanceof AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getType(((AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? new IClass[] { bt } : new IClass[0];
                } else
                if (atd instanceof NamedClassDeclaration) {
                    NamedClassDeclaration ncd = (NamedClassDeclaration) atd;
                    IClass[]              res = new IClass[ncd.implementedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getType(ncd.implementedTypes[i]);
                        if (!res[i].isInterface()) {
                            UnitCompiler.this.compileError((
                                "\""
                                + res[i].toString()
                                + "\" is not an interface; classes can only implement interfaces"
                            ), td.getLocation());
                        }
                    }
                    return res;
                } else
                if (atd instanceof InterfaceDeclaration) {
                    InterfaceDeclaration id  = (InterfaceDeclaration) atd;
                    IClass[]             res = new IClass[id.extendedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getType(id.extendedTypes[i]);
                        if (!res[i].isInterface()) {
                            UnitCompiler.this.compileError((
                                "\""
                                + res[i].toString()
                                + "\" is not an interface; interfaces can only extend interfaces"
                            ), td.getLocation());
                        }
                    }
                    return res;
                } else {
                    throw new InternalCompilerException(
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override protected IAnnotation[]
            getIAnnotations2() throws CompileException { return UnitCompiler.this.toIAnnotations(td.getAnnotations()); }

            @Override public boolean
            isAbstract() {
                return (
                    atd instanceof InterfaceDeclaration
                    || (atd instanceof NamedClassDeclaration && ((NamedClassDeclaration) atd).isAbstract())
                );
            }

            @Override public boolean
            isEnum() { return atd instanceof EnumDeclaration; }

            @Override public boolean
            isInterface() { return atd instanceof InterfaceDeclaration; }
        });
    }

    private void
    referenceThis(
        Locatable                locatable,
        AbstractClassDeclaration declaringClass,
        TypeBodyDeclaration      declaringTypeBodyDeclaration,
        IClass                   targetIClass
    ) throws CompileException {
        List<TypeDeclaration> path = UnitCompiler.getOuterClasses(declaringClass);

        if (UnitCompiler.isStaticContext(declaringTypeBodyDeclaration)) {
            this.compileError("No current instance available in static context", locatable.getLocation());
        }

        int j;
        TARGET_FOUND: {
            for (j = 0; j < path.size(); ++j) {

                // Notice: JLS7 15.9.2.BL1.B3.B1.B2 seems to be wrong: Obviously, JAVAC does not only allow
                //
                //    O is the nth lexically enclosing class
                //
                // , but also
                //
                //    O is assignable from the nth lexically enclosing class
                //
                // However, this strategy bears the risk of ambiguities, because "O" may be assignable from more than
                // one enclosing class.
                if (targetIClass.isAssignableFrom(this.resolve((TypeDeclaration) path.get(j)))) {
                    break TARGET_FOUND;
                }
            }
            this.compileError(
                "\"" + declaringClass + "\" is not enclosed by \"" + targetIClass + "\"",
                locatable.getLocation()
            );
        }

        int i;
        if (declaringTypeBodyDeclaration instanceof ConstructorDeclarator) {
            if (j == 0) {
                this.load(locatable, this.resolve(declaringClass), 0);
                return;
            }

            ConstructorDeclarator constructorDeclarator = (
                (ConstructorDeclarator) declaringTypeBodyDeclaration
            );
            String        spn                = "this$" + (path.size() - 2);
            LocalVariable syntheticParameter = (LocalVariable) constructorDeclarator.syntheticParameters.get(spn);
            if (syntheticParameter == null) {
                throw new InternalCompilerException("SNO: Synthetic parameter \"" + spn + "\" not found");
            }
            this.load(locatable, syntheticParameter);
            i = 1;
        } else {
            this.load(locatable, this.resolve(declaringClass), 0);
            i = 0;
        }
        for (; i < j; ++i) {
            final InnerClassDeclaration inner     = (InnerClassDeclaration) path.get(i);
            final TypeDeclaration       outer     = (TypeDeclaration) path.get(i + 1);

            SimpleIField sf = new SimpleIField(
                this.resolve(inner),             // declaringIClass
                "this$" + (path.size() - i - 2), // name
                this.resolve(outer)              // type
            );
            inner.defineSyntheticField(sf);
            this.getfield(locatable, sf);
        }
    }

    /**
     * Returns a list consisting of the given <var>inner</var> class and all its enclosing (outer) classes.
     *
     * @return {@link List} of {@link TypeDeclaration}; has length one iff <var>inner</var> has no enclosing instance
     */
    private static List<TypeDeclaration>
    getOuterClasses(TypeDeclaration inner) {
        List<TypeDeclaration> path = new ArrayList<TypeDeclaration>();
        for (TypeDeclaration ic = inner; ic != null; ic = UnitCompiler.getOuterClass(ic)) path.add(ic);
        return path;
    }

    /**
     * @return The {@link TypeDeclaration} that immediately encloses the <var>typeDeclaration</var>, or {@code null}
     */
    @Nullable static TypeDeclaration
    getOuterClass(TypeDeclaration typeDeclaration) {

        // Package member class declaration.
        if (typeDeclaration instanceof PackageMemberClassDeclaration) return null;

        // Member enum declaration is always implicitly static (JLS8 8.9).
        if (typeDeclaration instanceof MemberEnumDeclaration) return null;

        // Local class declaration.
        if (typeDeclaration instanceof LocalClassDeclaration) {
            Scope s = typeDeclaration.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator) && !(s instanceof Initializer); s = s.getEnclosingScope());
            if (s instanceof MethodDeclarator && ((MethodDeclarator) s).isStatic()) return null;
            if (s instanceof Initializer      && ((Initializer)      s).isStatic()) return null;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof AbstractClassDeclaration
            ) ? immediatelyEnclosingTypeDeclaration : null;
        }

        // Member class declaration.
        if (
            typeDeclaration instanceof MemberClassDeclaration
            && ((MemberClassDeclaration) typeDeclaration).isStatic()
        ) return null;

        // Anonymous class declaration, interface declaration.
        Scope s = typeDeclaration;
        for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
            if (s instanceof ConstructorInvocation) return null;
            if (s instanceof CompilationUnit) return null;
        }

        //if (!(s instanceof ClassDeclaration)) return null;

        if (UnitCompiler.isStaticContext((TypeBodyDeclaration) s)) return null;

        return (AbstractTypeDeclaration) s.getEnclosingScope();
    }

    private IClass
    getIClass(ThisReference tr) throws CompileException {

        if (tr.iClass != null) return tr.iClass;

        // Compile error if in static function context.
        Scope s;
        for (
            s = tr.getEnclosingScope();
            s instanceof Statement || s instanceof CatchClause;
            s = s.getEnclosingScope()
        );
        if (s instanceof FunctionDeclarator) {
            FunctionDeclarator function = (FunctionDeclarator) s;
            if (function instanceof MethodDeclarator && ((MethodDeclarator) function).isStatic()) {
                this.compileError("No current instance available in static method", tr.getLocation());
            }
        }

        // Determine declaring type.
        while (!(s instanceof TypeDeclaration)) {
            s = s.getEnclosingScope();
        }
        if (!(s instanceof AbstractClassDeclaration)) {
            this.compileError("Only methods of classes can have a current instance", tr.getLocation());
        }

        return (tr.iClass = this.resolve((AbstractClassDeclaration) s));
    }

    private IClass
    getReturnType(FunctionDeclarator fd) throws CompileException {

        if (fd.returnType != null) return fd.returnType;

        return (fd.returnType = this.getType(fd.type));
    }

    /**
     * @return the {@link IConstructor} that implements the <var>constructorDeclarator</var>
     */
    IClass.IConstructor
    toIConstructor(final ConstructorDeclarator constructorDeclarator) {
        if (constructorDeclarator.iConstructor != null) return constructorDeclarator.iConstructor;

        constructorDeclarator.iConstructor = this.resolve(constructorDeclarator.getDeclaringType()).new IConstructor() {

            @Nullable private IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() { return constructorDeclarator.getAccess(); }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(constructorDeclarator.getAnnotations()));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(null, ce);
                }
            }

            // Implement IInvocable.

            @Override public MethodDescriptor
            getDescriptor2() throws CompileException {

                if (!(constructorDeclarator.getDeclaringClass() instanceof InnerClassDeclaration)) {
                    return super.getDescriptor2();
                }

                if (constructorDeclarator.getDeclaringClass() instanceof MemberEnumDeclaration) {
                    return super.getDescriptor2();
                }

                List<String> parameterFds = new ArrayList<String>();

                // Convert enclosing instance reference into prepended constructor parameters.
                IClass outerClass = UnitCompiler.this.resolve(
                    constructorDeclarator.getDeclaringClass()
                ).getOuterIClass();
                if (outerClass != null) parameterFds.add(outerClass.getDescriptor());

                // Convert synthetic fields into prepended constructor parameters.
                for (IField sf : constructorDeclarator.getDeclaringClass().syntheticFields.values()) {
                    if (sf.getName().startsWith("val$")) parameterFds.add(sf.getType().getDescriptor());
                }

                // Process the 'normal' (declared) function parameters.
                for (IClass pt : this.getParameterTypes2()) parameterFds.add(pt.getDescriptor());

                return new MethodDescriptor(
                    Descriptor.VOID,                                                 // returnFd
                    (String[]) parameterFds.toArray(new String[parameterFds.size()]) // parameterFds
                );
            }

            @Override public boolean
            isVarargs() { return constructorDeclarator.formalParameters.variableArity; }

            @Override public IClass[]
            getParameterTypes2() throws CompileException {

                FormalParameter[] parameters = constructorDeclarator.formalParameters.parameters;
                IClass[]          res        = new IClass[parameters.length];

                for (int i = 0; i < parameters.length; ++i) {
                    IClass parameterType = UnitCompiler.this.getType(parameters[i].type);
                    if (i == parameters.length - 1 && constructorDeclarator.formalParameters.variableArity) {
                        parameterType = parameterType.getArrayIClass(
                            UnitCompiler.this.iClassLoader.TYPE_java_lang_Object
                        );
                    }
                    res[i] = parameterType;
                }

                return res;
            }

            @Override public IClass[]
            getThrownExceptions2() throws CompileException {
                IClass[] res = new IClass[constructorDeclarator.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getType(constructorDeclarator.thrownExceptions[i]);
                }
                return res;
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder().append(
                    constructorDeclarator.getDeclaringType().getClassName()
                ).append('(');

                FormalParameter[] parameters = constructorDeclarator.formalParameters.parameters;
                for (int i = 0; i < parameters.length; ++i) {
                    if (i != 0) sb.append(", ");
                    sb.append(parameters[i].toString(
                        i == parameters.length - 1
                        && constructorDeclarator.formalParameters.variableArity
                    ));
                }
                return sb.append(')').toString();
            }
        };
        return constructorDeclarator.iConstructor;
    }

    /**
     * @return The {@link IMethod} that implements the <var>methodDeclarator</var>
     */
    public IClass.IMethod
    toIMethod(final MethodDeclarator methodDeclarator) {

        if (methodDeclarator.iMethod != null) return methodDeclarator.iMethod;

        methodDeclarator.iMethod = this.resolve(methodDeclarator.getDeclaringType()).new IMethod() {

            @Nullable IAnnotation[] ias;

            // Implement IMember.
            @Override public Access
            getAccess() {
                return (
                    methodDeclarator.getDeclaringType() instanceof InterfaceDeclaration
                    ? Access.PUBLIC
                    : methodDeclarator.getAccess()
                );
            }

            @Override public IAnnotation[]
            getAnnotations() {

                if (this.ias != null) return this.ias;

                try {
                    return (this.ias = UnitCompiler.this.toIAnnotations(methodDeclarator.getAnnotations()));
                } catch (CompileException ce) {
                    throw new InternalCompilerException(null, ce);
                }
            }

            // Implement IInvocable.

            @Override public boolean
            isVarargs() { return methodDeclarator.formalParameters.variableArity; }

            @Override public IClass[]
            getParameterTypes2() throws CompileException {
                FormalParameter[] parameters = methodDeclarator.formalParameters.parameters;
                IClass[]          res        = new IClass[parameters.length];
                for (int i = 0; i < parameters.length; ++i) {
                    IClass parameterType = UnitCompiler.this.getType(parameters[i].type);
                    if (i == parameters.length - 1 && methodDeclarator.formalParameters.variableArity) {
                        parameterType = parameterType.getArrayIClass(
                            UnitCompiler.this.iClassLoader.TYPE_java_lang_Object
                        );
                    }
                    res[i] = parameterType;
                }
                return res;
            }

            @Override public IClass[]
            getThrownExceptions2() throws CompileException {

                List<IClass> result = new ArrayList<IClass>();
                for (Type ti : methodDeclarator.thrownExceptions) {

                    // KLUDGE: Iff the exception type in the THROWS clause sounds like a type parameter, then
                    // ignore it. Otherwise we'd have to put
                    //    try { ... } catch (Throwable t) { throw new AssertionError(t); }
                    // around all invocations of methods that use a type parameter for declaring their exception.
                    if (ti instanceof ReferenceType) {
                        String[] identifiers = ((ReferenceType) ti).identifiers;
                        if (
                            identifiers.length == 1
                            && UnitCompiler.LOOKS_LIKE_TYPE_PARAMETER.matcher(identifiers[0]).matches()
                        ) continue;
                    }

                    result.add(UnitCompiler.this.getType(ti));
                }

                return (IClass[]) result.toArray(new IClass[result.size()]);
            }

            // Implement IMethod.

            @Override public boolean
            isStatic() { return methodDeclarator.isStatic(); }

            @Override public boolean
            isAbstract() {
                return (
                    (methodDeclarator.getDeclaringType() instanceof InterfaceDeclaration)
                    || methodDeclarator.isAbstract()
                );
            }

            @Override public IClass
            getReturnType() throws CompileException { return UnitCompiler.this.getReturnType(methodDeclarator); }

            @Override public String
            getName() { return methodDeclarator.name; }
        };
        return methodDeclarator.iMethod;
    }

    private IClass.IInvocable
    toIInvocable(final FunctionDeclarator fd) {
        IClass.IInvocable result = (IClass.IInvocable) fd.accept(
            new Visitor.FunctionDeclaratorVisitor<IInvocable, RuntimeException>() {

                // SUPPRESS CHECKSTYLE LineLength:2
                @Override public IInvocable visitMethodDeclarator(MethodDeclarator md)           { return UnitCompiler.this.toIMethod((MethodDeclarator) fd);           }
                @Override public IInvocable visitConstructorDeclarator(ConstructorDeclarator cd) { return UnitCompiler.this.toIConstructor((ConstructorDeclarator) fd); }
            }
        );

        assert result != null;
        return result;
    }

    /**
     * If the given name was declared in a simple type import, load that class.
     */
    @Nullable private IClass
    importSingleType(String simpleTypeName, Location location) throws CompileException {
        String[] ss = this.getSingleTypeImport(simpleTypeName, location);
        if (ss == null) return null;

        IClass iClass = this.findTypeByFullyQualifiedName(location, ss);
        if (iClass == null) {
            this.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
            return this.iClassLoader.TYPE_java_lang_Object;
        }
        return iClass;
    }

    /**
     * Checks if the given simple name was imported through a single type import.
     *
     * @param name The simple type name, e.g. {@code "Inner"}
     * @return     The fully qualified name, e.g. {@code { "pkg", "Outer", "Inner" }}, or {@code null}
     */
    @Nullable public String[]
    getSingleTypeImport(String name, Location location) throws CompileException {

        // Resolve all single type imports (if not already done).
        Map<String, String[]> stis = this.singleTypeImports;
        if (stis == null) {

            // Collect all single type import declarations.
            final List<SingleTypeImportDeclaration> stids = new ArrayList<SingleTypeImportDeclaration>();
            for (ImportDeclaration id : this.abstractCompilationUnit.importDeclarations) {
                id.accept(new ImportVisitor<Void, RuntimeException>() {

                    @Override @Nullable public Void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          { stids.add(stid); return null; } // SUPPRESS CHECKSTYLE LineLength:3
                    @Override @Nullable public Void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     { return null;                  }
                    @Override @Nullable public Void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      { return null;                  }
                    @Override @Nullable public Void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) { return null;                  }
                });
            }

            // Resolve all single type imports.
            stis = new HashMap<String, String[]>();
            for (SingleTypeImportDeclaration stid : stids) {

                String[] ids        = stid.identifiers;
                String   simpleName = UnitCompiler.last(ids);

                // Check for re-import of same simple name.
                String[] prev = (String[]) stis.put(simpleName, ids);
                if (prev != null && !Arrays.equals(prev, ids)) {
                    UnitCompiler.this.compileError((
                        "Class \"" + simpleName + "\" was previously imported as "
                        + "\"" + Java.join(prev, ".") + "\", now as \"" + Java.join(ids, ".") + "\""
                    ), stid.getLocation());
                }

                if (this.findTypeByFullyQualifiedName(location, ids) == null) {
                    UnitCompiler.this.compileError(
                        "A class \"" + Java.join(ids, ".") + "\" could not be found",
                        stid.getLocation()
                    );
                }
            }

            this.singleTypeImports = stis;
        }

        return (String[]) stis.get(name);
    }

    /**
     * To be used only by {@link #getSingleTypeImport(String, Location)}; {@code null} means "not yet initialized"
     */
    @Nullable private Map<String /*simpleTypeName*/, String[] /*fullyQualifiedTypeName*/> singleTypeImports;

    /**
     * 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.<br>
     * 6.5.5.1.6 Type-import-on-demand declaration.
     *
     * @return {@code null} if the given <var>simpleTypeName</var> cannot be resolved through any of the
     *         type-import-on-demand declarations
     */
    @Nullable public IClass
    importTypeOnDemand(String simpleTypeName, Location location) throws CompileException {

        IClass importedClass = (IClass) this.onDemandImportableTypes.get(simpleTypeName);
        if (importedClass == null) {
            importedClass = this.importTypeOnDemand2(simpleTypeName, location);
            this.onDemandImportableTypes.put(simpleTypeName, importedClass);
        }

        return importedClass;
    }
    private final Map<String /*simpleTypeName*/, IClass> onDemandImportableTypes = new HashMap<String, IClass>();

    /**
     * @return {@code null} if the given <var>simpleTypeName</var> cannot be resolved through any of the
     *         type-import-on-demand declarations
     */
    @Nullable private IClass
    importTypeOnDemand2(String simpleTypeName, Location location) throws CompileException {

        IClass importedClass = null;
        for (TypeImportOnDemandDeclaration tiodd : this.getTypeImportOnDemandImportDeclarations()) {

            IClass iClass = this.findTypeByFullyQualifiedName(
                location,
                UnitCompiler.concat(tiodd.identifiers, simpleTypeName)
            );
            if (iClass == null) continue;

            if (importedClass != null && importedClass != iClass) {
                this.compileError(
                    "Ambiguous class name: \"" + importedClass + "\" vs. \"" + iClass + "\"",
                    location
                );
            }
            importedClass = iClass;
        }
        if (importedClass == null) return null;
        return importedClass;
    }

    private Collection<TypeImportOnDemandDeclaration>
    getTypeImportOnDemandImportDeclarations() {

        Collection<TypeImportOnDemandDeclaration> result = new ArrayList<TypeImportOnDemandDeclaration>();
        for (TypeImportOnDemandDeclaration tiodd : Iterables.filterByClass(
            this.abstractCompilationUnit.importDeclarations,
            TypeImportOnDemandDeclaration.class
        )) result.add(tiodd);

        result.add(new TypeImportOnDemandDeclaration(Location.NOWHERE, new String[] { "java", "lang" }));

        return result;
    }

    // ============================================ BYTECODE INSTRUCTIONS ============================================

    /**
     * Pushes one value on the operand stack and pushes the respective {@link VerificationTypeInfo} operand to the
     * stack map.
     * <table border="border">
     *   <tr><th><var>value</var></th><th>Operand stack value</th><th>Verification type info</th></tr>
     *   <tr>
     *     <td>
     *       {@link Character}
     *       <br />
     *       {@link Byte}
     *       <br />
     *       {@link Short}
     *       <br />
     *       {@link Integer}
     *       <br />
     *       {@link Boolean}
     *     </td>
     *     <td>{@code int}</td>
     *     <td>{@code integer_variable_info}</td>
     *   </tr>
     *   <tr><td>{@link Float}</td><td>{@code float}</td><td>{@code float_variable_info}</td></tr>
     *   <tr><td>{@link Long}</td><td>{@code msb+lsb} of {@code long}</td><td>{@code long_variable_info}</td></tr>
     *   <tr><td>{@link Double}</td><td>{@code msb+lsb} of {@code double}</td><td>{@code double_variable_info}</td></tr>
     *   <tr><td>{@link String}</td><td>{@code ConstantStringInfo} CP index</td><td>{@code object_variable_info(String)}</td></tr>
     *   <tr><td>{@link IClass}</td><td>{@code ConstantClassInfo} CP index</td><td>{@code object_variable_info(iClass.descriptor())}</td></tr>
     *   <tr><td>{@code null}</td><td>{@code null}</td><td>{@code null_variable_info(iClass.descriptor())}</td></tr>
     * </table>
     *
     * @return The computational type of the value that was pushed, e.g. {@link IClass#INT} for {@link Byte} or {@link
     *         IClass#VOID} for {@code null}
     */
    private IClass
    consT(Locatable locatable, @Nullable Object value) throws CompileException {

        if (value instanceof Character) {
            this.consT(locatable, ((Character) value).charValue());
            return IClass.INT;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            this.consT(locatable, ((Number) value).intValue());
            return IClass.INT;
        }
        if (Boolean.TRUE.equals(value)) {
            this.consT(locatable, 1);
            return IClass.BOOLEAN;
        }
        if (Boolean.FALSE.equals(value)) {
            this.consT(locatable, 0);
            return IClass.BOOLEAN;
        }
        if (value instanceof Float) {
            this.consT(locatable, ((Float) value).floatValue());
            return IClass.FLOAT;
        }
        if (value instanceof Long) {
            this.consT(locatable, ((Long) value).longValue());
            return IClass.LONG;
        }
        if (value instanceof Double) {
            this.consT(locatable, ((Double) value).doubleValue());
            return IClass.DOUBLE;
        }

        if (value instanceof String) {
            String   s  = (String) value;
            String[] ss = UnitCompiler.makeUtf8Able(s);
            this.consT(locatable, ss[0]);
            for (int i = 1; i < ss.length; ++i) {
                this.consT(locatable, ss[i]);
                this.invoke(locatable, this.iClassLoader.METH_java_lang_String__concat__java_lang_String);
            }
            return this.iClassLoader.TYPE_java_lang_String;
        }

        if (value instanceof IClass) {
            this.consT(locatable, (IClass) value);
            return this.iClassLoader.TYPE_java_lang_Class;
        }

        if (value == null) {
            this.aconstnull(locatable);
            return IClass.VOID;
        }

        throw new InternalCompilerException("Unknown literal \"" + value + "\"");
    }

    /**
     * Only strings that can be UTF8-encoded into 65535 bytes can be stored as a constant string info.
     *
     * @param s The string to split into suitable chunks
     * @return  Strings that can be UTF8-encoded into 65535 bytes
     */
    private static String[]
    makeUtf8Able(String s) {
        if (s.length() < (65536 / 3)) return new String[] { s };

        int          sLength = s.length(), utfLength = 0;
        int          from    = 0;
        List<String> l       = new ArrayList<String>();
        for (int i = 0;; i++) {
            if (i == sLength) {
                l.add(s.substring(from));
                break;
            }
            if (utfLength >= 65532) {
                l.add(s.substring(from, i));
                if (i + (65536 / 3) > sLength) {
                    l.add(s.substring(i));
                    break;
                }
                from      = i;
                utfLength = 0;
            }
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                ++utfLength;
            } else
            if (c > 0x07FF) {
                utfLength += 3;
            } else
            {
                utfLength += 2;
            }
        }
        return (String[]) l.toArray(new String[l.size()]);

    }

    private void
    consT(Locatable locatable, IClass t, int value) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            this.consT(locatable, value);
        } else
        if (t == IClass.LONG) {
            this.consT(locatable, (long) value);
        } else
        if (t == IClass.FLOAT) {
            this.consT(locatable, (float) value);
        } else
        if (t == IClass.DOUBLE) {
            this.consT(locatable, (double) value);
        } else
        {
            throw new AssertionError(t);
        }
    }

    private void
    consT(Locatable locatable, int value) {

        this.addLineNumberOffset(locatable);

        if (value >= -1 && value <= 5) {
            this.write(Opcode.ICONST_0 + value);
        } else
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            this.write(Opcode.BIPUSH);
            this.writeByte(value);
        } else
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            this.write(Opcode.SIPUSH);
            this.writeShort(value);
        } else {
            this.writeLdc(this.addConstantIntegerInfo(value));
        }

        this.getCodeContext().pushIntOperand();
    }

    private void
    consT(Locatable locatable, long value) {

        this.addLineNumberOffset(locatable);

        if (value == 0L || value == 1L) {
            this.write(Opcode.LCONST_0 + (int) value);
        } else
        {
            this.writeLdc2(this.addConstantLongInfo(value));
        }

        this.getCodeContext().pushLongOperand();
    }

    private void
    consT(Locatable locatable, float value) {

        this.addLineNumberOffset(locatable);

        if (
            Float.floatToIntBits(value) == Float.floatToIntBits(0.0F) // POSITIVE zero!
            || value == 1.0F
            || value == 2.0F
        ) {
            this.write(Opcode.FCONST_0 + (int) value);
        } else {
            this.writeLdc(this.addConstantFloatInfo(value));
        }

        this.getCodeContext().pushFloatOperand();
    }

    private void
    consT(Locatable locatable, double value) {

        this.addLineNumberOffset(locatable);

        if (
            Double.doubleToLongBits(value) == Double.doubleToLongBits(0.0D) // POSITIVE zero!
            || value == 1.0D
        ) {
            this.write(Opcode.DCONST_0 + (int) value);
        } else {
            this.writeLdc2(this.addConstantDoubleInfo(value));
        }

        this.getCodeContext().pushDoubleOperand();
    }

    private void
    consT(Locatable locatable, final String s) {
        this.addLineNumberOffset(locatable);
        this.writeLdc(this.addConstantStringInfo(s));
        this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_STRING);
    }

    private void
    consT(Locatable locatable, IClass iClass) {
        this.addLineNumberOffset(locatable);
        this.writeLdc(this.addConstantClassInfo(iClass));
        this.getCodeContext().pushObjectOperand(Descriptor.JAVA_LANG_CLASS);
    }

    private void
    castConversion(
        Locatable        locatable,
        IClass           sourceType,
        IClass           targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        if (!this.tryCastConversion(locatable, sourceType, targetType, constantValue)) {
            this.compileError(
                "Cast conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"",
                locatable.getLocation()
            );
        }
    }

    private boolean
    tryCastConversion(
        Locatable        locatable,
        IClass           sourceType,
        IClass           targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        return (
            this.tryAssignmentConversion(locatable, sourceType, targetType, constantValue)
            || this.tryNarrowingPrimitiveConversion(locatable, sourceType, targetType)
            || this.tryNarrowingReferenceConversion(locatable, sourceType, targetType)
        );
    }

    /**
     * Implements "assignment conversion" (JLS7 5.2).
     */
    private void
    assignmentConversion(
        Locatable        locatable,
        IClass           sourceType,
        IClass           targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        if (!this.tryAssignmentConversion(locatable, sourceType, targetType, constantValue)) {
            this.compileError(
                "Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"",
                locatable.getLocation()
            );
        }
    }

    private boolean
    tryAssignmentConversion(
        Locatable        locatable,
        IClass           sourceType,
        IClass           targetType,
        @Nullable Object constantValue
    ) throws CompileException {
        UnitCompiler.LOGGER.entering(
            null,
            "tryAssignmentConversion",
            new Object[] { locatable, sourceType, targetType, constantValue }
        );

        // JLS7 5.1.1 Identity conversion.
        if (this.tryIdentityConversion(sourceType, targetType)) return true;

        // JLS7 5.1.2 Widening primitive conversion.
        if (this.tryWideningPrimitiveConversion(locatable, sourceType, targetType)) return true;

        // JLS7 5.1.4 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) {
            this.getCodeContext().popOperand(sourceType.getDescriptor());
            this.getCodeContext().pushOperand(targetType.getDescriptor());
            return true;
        }

        // A boxing conversion (JLS7 5.1.7) optionally followed by a widening reference conversion.
        {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                if (this.tryIdentityConversion(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    return true;
                }
                if (this.isWideningReferenceConvertible(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    this.getCodeContext().popOperand(boxedType.getDescriptor());
                    this.getCodeContext().pushOperand(targetType.getDescriptor());
                    return true;
                }
            }
        }

        // An unboxing conversion (JLS7 5.1.8) optionally followed by a widening primitive conversion.
        {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                if (this.tryIdentityConversion(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    return true;
                }
                if (this.isWideningPrimitiveConvertible(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    this.tryWideningPrimitiveConversion(locatable, unboxedType, targetType);
                    return true;
                }
            }
        }

        // 5.2 Special narrowing primitive conversion.
        if (constantValue != UnitCompiler.NOT_CONSTANT) {
            if (this.tryConstantAssignmentConversion(
                locatable,
                constantValue, // constantValue
                targetType             // targetType
            )) return true;
        }

        return false;
    }

    /**
     * Implements "assignment conversion" (JLS7 5.2) on a constant value.
     *
     * @param value Must be a {@link Boolean}, {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link
     *              Character}, {@link Long}, {@link Float}, {@link Double} or {@code null}
     * @return      A {@link Boolean}, {@link String}, {@link Byte}, {@link Short}, {@link Integer}, {@link Character},
     *              {@link Long}, {@link Float}, {@link Double} or {@code null}
     */
    @Nullable private Object
    assignmentConversion(Locatable locatable, @Nullable Object value, IClass targetType) throws CompileException {
        if (targetType == IClass.BOOLEAN) {
            if (value instanceof Boolean) return value;
        } else
        if (targetType == this.iClassLoader.TYPE_java_lang_String) {
            if (value instanceof String) return value;
        } else
        if (targetType == IClass.BYTE) {
            if (value instanceof Byte) {
                return value;
            } else
            if (value instanceof Short || value instanceof Integer) {
                assert value != null;
                int x = ((Number) value).intValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) return new Byte((byte) x);
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) return new Byte((byte) x);
            }
        } else
        if (targetType == IClass.SHORT) {
            if (value instanceof Byte) {
                return new Short(((Number) value).shortValue());
            } else
            if (value instanceof Short) {
                return value;
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) return new Short((short) x);
            } else
            if (value instanceof Integer) {
                int x = ((Integer) value).intValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) return new Short((short) x);
            }
        } else
        if (targetType == IClass.CHAR) {
            if (value instanceof Short) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                assert value != null;
                int x = ((Number) value).intValue();
                if (x >= Character.MIN_VALUE && x <= Character.MAX_VALUE) return new Character((char) x);
            }
        } else
        if (targetType == IClass.INT) {
            if (value instanceof Integer) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short) {
                assert value != null;
                return new Integer(((Number) value).intValue());
            } else
            if (value instanceof Character) {
                return new Integer(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.LONG) {
            if (value instanceof Long) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                assert value != null;
                return new Long(((Number) value).longValue());
            } else
            if (value instanceof Character) {
                return new Long(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.FLOAT) {
            if (value instanceof Float) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                assert value != null;
                return new Float(((Number) value).floatValue());
            } else
            if (value instanceof Character) {
                return new Float(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.DOUBLE) {
            if (value instanceof Double) {
                return value;
            } else
            if (
                value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
            ) {
                assert value != null;
                return new Double(((Number) value).doubleValue());
            } else
            if (value instanceof Character) {
                return new Double(((Character) value).charValue());
            }
        } else
        if (value == null && !targetType.isPrimitive()) {
            return null;
        } else
        if (value instanceof String && targetType.isAssignableFrom(this.iClassLoader.TYPE_java_lang_String)) {
            return value;
        }

        if (value == null) {
            this.compileError(
                "Cannot convert 'null' to type \"" + targetType.toString() + "\"",
                locatable.getLocation()
            );
        } else
        {
            this.compileError((
                "Cannot convert constant of type \""
                + value.getClass().getName()
                + "\" to type \""
                + targetType.toString()
                + "\""
            ), locatable.getLocation());
        }
        return value;
    }

    /**
     * Implements "unary numeric promotion" (JLS7 5.6.1).
     *
     * @return The promoted type
     */
    private IClass
    unaryNumericPromotion(Locatable locatable, IClass type) throws CompileException {
        type = this.convertToPrimitiveNumericType(locatable, type);

        IClass promotedType = this.unaryNumericPromotionType(locatable, type);

        this.numericPromotion(locatable, type, promotedType);
        return promotedType;
    }

    private void
    reverseUnaryNumericPromotion(Locatable locatable, IClass sourceType, IClass targetType) throws CompileException {
        IClass unboxedType = this.isUnboxingConvertible(targetType);
        IClass pt          = unboxedType != null ? unboxedType : targetType;
        if (
            !this.tryIdentityConversion(sourceType, pt)
            && !this.tryNarrowingPrimitiveConversion(
                locatable,  // locatable
                sourceType, // sourceType
                pt          // targetType
            )
        ) throw new InternalCompilerException("SNO: reverse unary numeric promotion failed");
        if (unboxedType != null) this.boxingConversion(locatable, unboxedType, targetType);
    }

    /**
     * If the given type is a primitive type, return that type. If the given type is a primitive wrapper class, unbox
     * the operand on top of the operand stack and return the primitive type. Otherwise, issue a compile error.
     */
    private IClass
    convertToPrimitiveNumericType(Locatable locatable, IClass type) throws CompileException {

        if (type.isPrimitiveNumeric()) return type;

        IClass unboxedType = this.isUnboxingConvertible(type);
        if (unboxedType != null) {
            this.unboxingConversion(locatable, type, unboxedType);
            return unboxedType;
        }
        this.compileError(
            "Object of type \"" + type.toString() + "\" cannot be converted to a numeric type",
            locatable.getLocation()
        );
        return type;
    }

    private void
    numericPromotion(Locatable locatable, IClass sourceType, IClass targetType) {
        if (
            !this.tryIdentityConversion(sourceType, targetType)
            && !this.tryWideningPrimitiveConversion(
                locatable,  // locatable
                sourceType, // sourceType
                targetType  // targetType
            )
        ) throw new InternalCompilerException("SNO: Conversion failed");
    }

    private IClass
    unaryNumericPromotionType(Locatable locatable, IClass type) throws CompileException {
        if (!type.isPrimitiveNumeric()) {
            this.compileError(
                "Unary numeric promotion not possible on non-numeric-primitive type \"" + type + "\"",
                locatable.getLocation()
            );
        }

        return (
            type == IClass.DOUBLE ? IClass.DOUBLE :
            type == IClass.FLOAT  ? IClass.FLOAT  :
            type == IClass.LONG   ? IClass.LONG   :
            IClass.INT
        );
    }

    private IClass
    binaryNumericPromotionType(Locatable locatable, IClass type1, IClass type2) throws CompileException {
        if (!type1.isPrimitiveNumeric() || !type2.isPrimitiveNumeric()) {
            this.compileError(
                "Binary numeric promotion not possible on types \"" + type1 + "\" and \"" + type2 + "\"",
                locatable.getLocation()
            );
        }

        return (
            type1 == IClass.DOUBLE || type2 == IClass.DOUBLE ? IClass.DOUBLE :
            type1 == IClass.FLOAT  || type2 == IClass.FLOAT  ? IClass.FLOAT  :
            type1 == IClass.LONG   || type2 == IClass.LONG   ? IClass.LONG   :
            IClass.INT
        );
    }

    /**
     * Checks whether "identity conversion" (5.1.1) is possible.
     *
     * @return Whether the conversion is possible
     */
    @SuppressWarnings("static-method") private boolean
    isIdentityConvertible(IClass sourceType, IClass targetType) { return sourceType == targetType; }

    /**
     * Implements "identity conversion" (5.1.1).
     *
     * @return Whether the conversion was possible
     */
    @SuppressWarnings("static-method") private boolean
    tryIdentityConversion(IClass sourceType, IClass targetType) { return sourceType == targetType; }

    @SuppressWarnings("static-method") private boolean
    isWideningPrimitiveConvertible(IClass sourceType, IClass targetType) {
        return UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        ) != null;
    }

    /**
     * Implements "widening primitive conversion" (5.1.2).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryWideningPrimitiveConversion(Locatable locatable, IClass sourceType, IClass targetType) {
        int[] opcodes = (int[]) UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
        if (opcodes != null) {
            this.addLineNumberOffset(locatable);
            for (int opcode : opcodes) this.write(opcode);
            this.getCodeContext().popOperand();
            this.getCodeContext().pushOperand(targetType.getDescriptor());
            return true;
        }
        return false;
    }

    private static final Map<String /*descriptor*/, int[] /*opcodes*/>
    PRIMITIVE_WIDENING_CONVERSIONS = new HashMap<String, int[]>();

    static { UnitCompiler.fillConversionMap(new Object[] {

        new int[0],
        Descriptor.BYTE  + Descriptor.SHORT,
        Descriptor.BYTE  + Descriptor.INT,
        Descriptor.SHORT + Descriptor.INT,
        Descriptor.CHAR  + Descriptor.INT,

        new int[] { Opcode.I2L },
        Descriptor.BYTE  + Descriptor.LONG,
        Descriptor.SHORT + Descriptor.LONG,
        Descriptor.CHAR  + Descriptor.LONG,
        Descriptor.INT   + Descriptor.LONG,

        new int[] { Opcode.I2F },
        Descriptor.BYTE  + Descriptor.FLOAT,
        Descriptor.SHORT + Descriptor.FLOAT,
        Descriptor.CHAR  + Descriptor.FLOAT,
        Descriptor.INT   + Descriptor.FLOAT,

        new int[] { Opcode.L2F },
        Descriptor.LONG  + Descriptor.FLOAT,

        new int[] { Opcode.I2D },
        Descriptor.BYTE  + Descriptor.DOUBLE,
        Descriptor.SHORT + Descriptor.DOUBLE,
        Descriptor.CHAR  + Descriptor.DOUBLE,
        Descriptor.INT   + Descriptor.DOUBLE,

        new int[] { Opcode.L2D },
        Descriptor.LONG  + Descriptor.DOUBLE,

        new int[] { Opcode.F2D },
        Descriptor.FLOAT + Descriptor.DOUBLE,
    }, UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS); }
    private static void
    fillConversionMap(Object[] array, Map<String /*descriptor*/, int[] /*opcodes*/> map) {
        int[] opcodes = null;
        for (Object o : array) {
            if (o instanceof int[]) {
                opcodes = (int[]) o;
            } else {
                map.put((String) o, opcodes);
            }
        }
    }

    /**
     * Checks if "widening reference conversion" (5.1.4) is possible.
     *
     * @return Whether the conversion is possible
     */
    @SuppressWarnings("static-method") private boolean
    isWideningReferenceConvertible(IClass sourceType, IClass targetType) throws CompileException {
        if (targetType.isPrimitive() || sourceType == targetType) return false;

        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * Checks whether "narrowing primitive conversion" (JLS7 5.1.3) is possible.
     */
    @SuppressWarnings("static-method") private boolean
    isNarrowingPrimitiveConvertible(IClass sourceType, IClass targetType) {
        return UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.containsKey(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
    }

    /**
     * Implements "narrowing primitive conversion" (JLS7 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryNarrowingPrimitiveConversion(Locatable locatable, IClass sourceType, IClass targetType) {
        int[] opcodes = (int[]) UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
        if (opcodes != null) {
            this.addLineNumberOffset(locatable);
            for (int opcode : opcodes) this.write(opcode);
            this.getCodeContext().popOperand();
            this.getCodeContext().pushOperand(targetType.getDescriptor());
            return true;
        }
        return false;
    }

    private static final Map<String /*descriptor*/, int[] /*opcodes*/>
    PRIMITIVE_NARROWING_CONVERSIONS = new HashMap<String, int[]>();

    static { UnitCompiler.fillConversionMap(new Object[] {

        new int[0],
        Descriptor.BYTE + Descriptor.CHAR,
        Descriptor.SHORT + Descriptor.CHAR,
        Descriptor.CHAR + Descriptor.SHORT,

        new int[] { Opcode.I2B },
        Descriptor.SHORT + Descriptor.BYTE,
        Descriptor.CHAR + Descriptor.BYTE,
        Descriptor.INT + Descriptor.BYTE,

        new int[] { Opcode.I2S },
        Descriptor.INT + Descriptor.SHORT,

        new int[] { Opcode.I2C },
        Descriptor.INT + Descriptor.CHAR,

        new int[] { Opcode.L2I, Opcode.I2B },
        Descriptor.LONG + Descriptor.BYTE,

        new int[] { Opcode.L2I, Opcode.I2S },
        Descriptor.LONG + Descriptor.SHORT,
        Descriptor.LONG + Descriptor.CHAR,

        new int[] { Opcode.L2I },
        Descriptor.LONG + Descriptor.INT,

        new int[] { Opcode.F2I, Opcode.I2B },
        Descriptor.FLOAT + Descriptor.BYTE,

        new int[] { Opcode.F2I, Opcode.I2S },
        Descriptor.FLOAT + Descriptor.SHORT,
        Descriptor.FLOAT + Descriptor.CHAR,

        new int[] { Opcode.F2I },
        Descriptor.FLOAT + Descriptor.INT,

        new int[] { Opcode.F2L },
        Descriptor.FLOAT + Descriptor.LONG,

        new int[] { Opcode.D2I, Opcode.I2B },
        Descriptor.DOUBLE + Descriptor.BYTE,

        new int[] { Opcode.D2I, Opcode.I2S },
        Descriptor.DOUBLE + Descriptor.SHORT,
        Descriptor.DOUBLE + Descriptor.CHAR,

        new int[] { Opcode.D2I },
        Descriptor.DOUBLE + Descriptor.INT,

        new int[] { Opcode.D2L },
        Descriptor.DOUBLE + Descriptor.LONG,

        new int[] { Opcode.D2F },
        Descriptor.DOUBLE + Descriptor.FLOAT,
    }, UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS); }

    /**
     * Checks if "constant assignment conversion" (JLS7 5.2, paragraph 1) is possible.
     *
     * @param constantValue The constant value that is to be converted
     * @param targetType    The type to convert to
     */
    private boolean
    tryConstantAssignmentConversion(Locatable locatable, @Nullable Object constantValue, IClass targetType)
    throws CompileException {
        UnitCompiler.LOGGER.entering(
            null,
            "tryConstantAssignmentConversion",
            new Object[] { locatable, constantValue, targetType }
        );

        int cv;
        if (constantValue instanceof Byte) {
            cv = ((Byte) constantValue).byteValue();
        } else
        if (constantValue instanceof Short) {
            cv = ((Short) constantValue).shortValue();
        } else
        if (constantValue instanceof Integer) {
            cv = ((Integer) constantValue).intValue();
        } else
        if (constantValue instanceof Character) {
            cv = ((Character) constantValue).charValue();
        } else
        {
            return false;
        }

        if (targetType == IClass.BYTE)  return cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE;
        if (targetType == IClass.SHORT) return cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE;
        if (targetType == IClass.CHAR)  return cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE;

        IClassLoader icl = this.iClassLoader;
        if (targetType == icl.TYPE_java_lang_Byte && cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.BYTE, targetType);
            return true;
        }
        if (targetType == icl.TYPE_java_lang_Short && cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.SHORT, targetType);
            return true;
        }
        if (targetType == icl.TYPE_java_lang_Character && cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.CHAR, targetType);
            return true;
        }

        return false;
    }

    /**
     * Checks whether "narrowing reference conversion" (JLS7 5.1.5) is possible.
     */
    private boolean
    isNarrowingReferenceConvertible(IClass sourceType, IClass targetType) throws CompileException {
        if (sourceType.isPrimitive()) return false;
        if (sourceType == targetType) return false;

        // 5.1.5.1
        if (sourceType.isAssignableFrom(targetType)) return true;

        // 5.1.5.2
        if (targetType.isInterface() && !sourceType.isFinal() && !targetType.isAssignableFrom(sourceType)) return true;

        // 5.1.5.3
        if (sourceType == this.iClassLoader.TYPE_java_lang_Object && targetType.isArray()) return true;

        // 5.1.5.4
        if (sourceType == this.iClassLoader.TYPE_java_lang_Object && targetType.isInterface()) return true;

        // 5.1.5.5
        if (sourceType.isInterface() && !targetType.isFinal()) return true;

        // 5.1.5.6
        if (sourceType.isInterface() && targetType.isFinal() && sourceType.isAssignableFrom(targetType)) return true;

        // 5.1.5.7
        // TODO: Check for redefinition of methods with same signature but different return type.
        if (sourceType.isInterface() && targetType.isInterface() && !targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        // 5.1.5.8
        if (sourceType.isArray() && targetType.isArray()) {

            IClass st = sourceType.getComponentType();
            assert st != null;

            IClass tt = targetType.getComponentType();
            assert tt != null;

            if (this.isNarrowingPrimitiveConvertible(st, tt) || this.isNarrowingReferenceConvertible(st, tt)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implements "narrowing reference conversion" (5.1.5).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryNarrowingReferenceConversion(Locatable locatable, IClass sourceType, IClass targetType) throws CompileException {

        if (!this.isNarrowingReferenceConvertible(sourceType, targetType)) return false;

        this.checkcast(locatable, targetType);
        return true;
    }

    /**
     * JLS7 5.5
     */
    private boolean
    isCastReferenceConvertible(IClass sourceType, IClass targetType) throws CompileException {
        return (
            this.isIdentityConvertible(sourceType, targetType)
            || this.isWideningReferenceConvertible(sourceType, targetType)
            || this.isNarrowingReferenceConvertible(sourceType, targetType)
        );
    }

    /**
     * @return The boxed type or {@code null}
     */
    @Nullable private IClass
    isBoxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == IClass.BOOLEAN) return icl.TYPE_java_lang_Boolean;
        if (sourceType == IClass.BYTE)    return icl.TYPE_java_lang_Byte;
        if (sourceType == IClass.CHAR)    return icl.TYPE_java_lang_Character;
        if (sourceType == IClass.SHORT)   return icl.TYPE_java_lang_Short;
        if (sourceType == IClass.INT)     return icl.TYPE_java_lang_Integer;
        if (sourceType == IClass.LONG)    return icl.TYPE_java_lang_Long;
        if (sourceType == IClass.FLOAT)   return icl.TYPE_java_lang_Float;
        if (sourceType == IClass.DOUBLE)  return icl.TYPE_java_lang_Double;
        return null;
    }

    private boolean
    tryBoxingConversion(Locatable locatable, IClass sourceType, IClass targetType) throws CompileException {
        if (this.isBoxingConvertible(sourceType) == targetType) {
            this.boxingConversion(locatable, sourceType, targetType);
            return true;
        }
        return false;
    }

    /**
     * @param sourceType a primitive type (except VOID)
     * @param targetType the corresponding wrapper type
     */
    private void
    boxingConversion(Locatable locatable, IClass sourceType, IClass targetType) throws CompileException {

        this.invokeMethod(
            locatable,            // locatable
            Opcode.INVOKESTATIC,  // opcode
            targetType,           // declaringIClass
            "valueOf",            // methodName
            new MethodDescriptor( // methodFD
                targetType.getDescriptor(), // returnFd
                sourceType.getDescriptor()  // parameterFds...
            ),
            false                 // useInterfaceMethodref
        );
    }

    /**
     * @return Iff <var>sourceType</var> is a primitive wrapper type, the unboxed type, otherwise {@code null}
     */
    @Nullable private IClass
    isUnboxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == icl.TYPE_java_lang_Boolean)   return IClass.BOOLEAN;
        if (sourceType == icl.TYPE_java_lang_Byte)      return IClass.BYTE;
        if (sourceType == icl.TYPE_java_lang_Character) return IClass.CHAR;
        if (sourceType == icl.TYPE_java_lang_Short)     return IClass.SHORT;
        if (sourceType == icl.TYPE_java_lang_Integer)   return IClass.INT;
        if (sourceType == icl.TYPE_java_lang_Long)      return IClass.LONG;
        if (sourceType == icl.TYPE_java_lang_Float)     return IClass.FLOAT;
        if (sourceType == icl.TYPE_java_lang_Double)    return IClass.DOUBLE;
        return null;
    }

    /**
     * @return Whether the <var>sourceType</var> is a primitive numeric type, or a wrapper type of a primitive numeric
     *         type
     */
    private boolean
    isConvertibleToPrimitiveNumeric(IClass sourceType) {
        if (sourceType.isPrimitiveNumeric()) return true;
        IClass unboxedType = this.isUnboxingConvertible(sourceType);
        return unboxedType != null && unboxedType.isPrimitiveNumeric();
    }

    /**
     * @param targetType a primitive type (except VOID)
     * @param sourceType the corresponding wrapper type
     */
    private void
    unboxingConversion(Locatable locatable, IClass sourceType, IClass targetType) throws CompileException {

        // "source.intValue()"
        this.invokeMethod(
            locatable,                                                       // locatable
            Opcode.INVOKEVIRTUAL,                                            // opcode
            sourceType,                                                      // declaringIClass
            targetType.toString() + "Value",                                 // methodName
            new MethodDescriptor(targetType.getDescriptor(), new String[0]), // methodMd
            false                                                            // useInterfaceMethodref
        );
    }

    /**
     * Attempts to load an {@link IClass} by fully-qualified name through {@link #iClassLoader}.
     *
     * @param identifiers       The fully qualified type name, e.g. '{@code { "pkg", "Outer", "Inner" }}'
     * @return                  {@code null} if a class with the given name could not be loaded
     * @throws CompileException The type exists, but a problem occurred when it was loaded
     */
    @Nullable private IClass
    findTypeByFullyQualifiedName(Location location, String[] identifiers) throws CompileException {

        // Try all 'flavors', i.e. 'a.b.c', 'a.b$c', 'a$b$c'.
        String className = Java.join(identifiers, ".");
        for (;;) {
            IClass iClass = UnitCompiler.this.findTypeByName(location, className);
            if (iClass != null) return iClass;

            int idx = className.lastIndexOf('.');
            if (idx == -1) break;
            className = className.substring(0, idx) + '$' + className.substring(idx + 1);
        }

        return null;
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    private void
    ifNumeric(Locatable locatable, int opIdx, Offset dst) {
        assert opIdx >= UnitCompiler.EQ && opIdx <= UnitCompiler.LE;

        VerificationTypeInfo topOperand = this.getCodeContext().peekOperand();

        if (topOperand == StackMapTableAttribute.INTEGER_VARIABLE_INFO) {
            this.if_icmpxx(locatable, opIdx, dst);
        } else
        if (
            topOperand == StackMapTableAttribute.LONG_VARIABLE_INFO
            || topOperand == StackMapTableAttribute.FLOAT_VARIABLE_INFO
            || topOperand == StackMapTableAttribute.DOUBLE_VARIABLE_INFO
        ) {
            this.cmp(locatable, opIdx);
            this.ifxx(locatable, opIdx, dst);
        } else
        {
            throw new InternalCompilerException("Unexpected computational type \"" + topOperand + "\"");
        }
    }

    // ============================= BYTE CODE GENERATION METHODS, IN ALPHABETICAL ORDER =============================

    private void
    aconstnull(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.ACONST_NULL);
        this.getCodeContext().pushNullOperand();
    }

    private void
    add(Locatable locatable) { this.mulDivRemAddSub(locatable, "+"); }

    private void
    andOrXor(Locatable locatable, String operator) {

        VerificationTypeInfo operand2 = this.getCodeContext().popIntOrLongOperand();
        VerificationTypeInfo operand1 = this.getCodeContext().popIntOrLongOperand();

        assert operand1 == operand2;

        final int opcode = (
            operator == "&" ? Opcode.IAND :
            operator == "|" ? Opcode.IOR  :
            operator == "^" ? Opcode.IXOR :
            Integer.MAX_VALUE
        ) + (operand1 == StackMapTableAttribute.LONG_VARIABLE_INFO ? 1 : 0);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.getCodeContext().pushOperand(operand1);
    }

    private void
    anewarray(Locatable locatable, IClass componentType) {
        IClass arrayType = componentType.getArrayIClass(this.iClassLoader.TYPE_java_lang_Object);

        this.addLineNumberOffset(locatable);
        this.getCodeContext().popIntOperand();
        this.write(Opcode.ANEWARRAY);
        this.writeConstantClassInfo(componentType);
        this.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    private void
    arraylength(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popObjectOperand();
        this.write(Opcode.ARRAYLENGTH);
        this.getCodeContext().pushIntOperand();
    }

    private void
    arraystore(Locatable locatable, IClass lhsComponentType) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popOperand();
        this.getCodeContext().popOperand();
        this.getCodeContext().popOperand();
        this.write(Opcode.IASTORE + UnitCompiler.ilfdabcs(lhsComponentType));
    }

    private void
    athrow(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.ATHROW);
        this.getCodeContext().popReferenceOperand();
    }

    private void
    checkcast(Locatable locatable, IClass targetType) {

        this.addLineNumberOffset(locatable);
        this.write(Opcode.CHECKCAST);
        this.writeConstantClassInfo(targetType);
        this.getCodeContext().popOperand();
        this.getCodeContext().pushObjectOperand(targetType.getDescriptor());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    private void
    cmp(Locatable locatable, int opIdx) {
        assert opIdx >= UnitCompiler.EQ && opIdx <= UnitCompiler.LE;

        VerificationTypeInfo operand2 = this.getCodeContext().currentInserter().getStackMap().peekOperand();
        this.getCodeContext().popOperand();
        VerificationTypeInfo operand1 = this.getCodeContext().currentInserter().getStackMap().peekOperand();
        this.getCodeContext().popOperand();

        if (operand1 == StackMapTableAttribute.LONG_VARIABLE_INFO && operand2 == StackMapTableAttribute.LONG_VARIABLE_INFO) {
            this.write(Opcode.LCMP);
        } else
        if (operand1 == StackMapTableAttribute.FLOAT_VARIABLE_INFO && operand2 == StackMapTableAttribute.FLOAT_VARIABLE_INFO) {
            this.write(opIdx == UnitCompiler.GE || opIdx == UnitCompiler.GT ? Opcode.FCMPL : Opcode.FCMPG);
        } else
        if (operand1 == StackMapTableAttribute.DOUBLE_VARIABLE_INFO && operand2 == StackMapTableAttribute.DOUBLE_VARIABLE_INFO) {
            this.write(opIdx == UnitCompiler.GE || opIdx == UnitCompiler.GT ? Opcode.DCMPL : Opcode.DCMPG);
        } else
        {
            throw new AssertionError(operand1 + " and " + operand2);
        }
        this.getCodeContext().pushIntOperand();
    }

    /**
     * Duplicates the top operand: ... a => ... a a
     */
    private void
    dup(Locatable locatable) {

        VerificationTypeInfo topOperand = this.getCodeContext().peekOperand();

        this.addLineNumberOffset(locatable);
        this.write(topOperand.category() == 1 ? Opcode.DUP : Opcode.DUP2);

        this.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Duplicates the top two operands: ... a b => ... a b a b.
     * This works iff both the top operand <em>and</em> the top-but-one operand have size 1.
     */
    private void
    dup2(Locatable locatable) {

        this.addLineNumberOffset(locatable);

        VerificationTypeInfo topOperand = this.getCodeContext().popOperand();
        assert topOperand.category() == 1;
        VerificationTypeInfo topButOneOperand = this.getCodeContext().popOperand();
        assert topButOneOperand.category() == 1;
        this.write(Opcode.DUP2);
        this.getCodeContext().pushOperand(topButOneOperand);
        this.getCodeContext().pushOperand(topOperand);
        this.getCodeContext().pushOperand(topButOneOperand);
        this.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Duplicates the top <var>n</var> operands.
     * <dl>
     *   <dt>n == 0</dt><dd>(nothing)</dd>
     *   <dt>n == 1</dt><dd>... a => ... a a</dd>
     *   <dt>n == 2</dt><dd>... a b => ... a b a b</dd>
     * </dl>
     */
    private void
    dupn(Locatable locatable, int n) {

        switch (n) {
        case 0:  ;                     break;
        case 1:  this.dup(locatable);  break;
        case 2:  this.dup2(locatable); break;
        default: throw new AssertionError(n);
        }
    }

    /**
     * Copies the top operand one position down: b a => a b a
     */
    private void
    dupx(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.getCodeContext().popOperand();

        this.addLineNumberOffset(locatable);
        this.write((
            topOperand.category() == 1
            ? (topButOneOperand.category() == 1 ? Opcode.DUP_X1  : Opcode.DUP_X2)
            : (topButOneOperand.category() == 1 ? Opcode.DUP2_X1 : Opcode.DUP2_X2)
        ));

        this.getCodeContext().pushOperand(topOperand);
        this.getCodeContext().pushOperand(topButOneOperand);
        this.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Copies the top operand <em>two</em> positions down: c b a => a c b a.
     * This works iff the top-but-one and top-but-two operands <em>both</em> have size 1.
     */
    private void
    dupx2(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.getCodeContext().popOperand();
        VerificationTypeInfo topButTwoOperand = this.getCodeContext().popOperand();

        assert topButOneOperand.category() == 1;
        assert topButTwoOperand.category() == 1;

        this.addLineNumberOffset(locatable);
        this.write(topOperand.category() == 1 ? Opcode.DUP_X2 : Opcode.DUP2_X2);

        this.getCodeContext().pushOperand(topOperand);
        this.getCodeContext().pushOperand(topButTwoOperand);
        this.getCodeContext().pushOperand(topButOneOperand);
        this.getCodeContext().pushOperand(topOperand);
    }

    /**
     * Copies the top operand <em>positions</em> down.
     * <dl>
     *   <dt>n == 0</dt><dd>... a => ... a a</dd>
     *   <dt>n == 1</dt><dd>... b a => ... a b a</dd>
     *   <dt>n == 2</dt><dd>... c b a => ... a c b a</dd>
     * </dl>
     */
    private void
    dupxx(Locatable locatable, int positions) {
        switch (positions) {
        case 0:  this.dup(locatable);   break;
        case 1:  this.dupx(locatable);  break;
        case 2:  this.dupx2(locatable); break;
        default: throw new AssertionError(positions);
        }
    }

    private void
    getfield(Locatable locatable, IClass.IField iField) throws CompileException {
        this.getfield(
            locatable,                   // locatable
            iField.getDeclaringIClass(), // declaringIClass
            iField.getName(),            // fieldName
            iField.getType(),            // fieldType
            iField.isStatic()            // statiC
        );
    }

    private void
    getfield(Locatable locatable, IClass declaringIClass, String fieldName, IClass fieldType, boolean statiC) {

        this.addLineNumberOffset(locatable);
        if (statiC) {
            this.write(Opcode.GETSTATIC);
        } else {
            this.write(Opcode.GETFIELD);
            this.getCodeContext().popOperand();
        }
        this.writeConstantFieldrefInfo(
            declaringIClass, // iClass
            fieldName,       // fieldName
            fieldType        // fieldType
        );
        this.getCodeContext().pushOperand(fieldType.getDescriptor());
    }

    /**
         * @param opcode One of IF* and GOTO
         */
        private void
        gotO(Locatable locatable, CodeContext.Offset dst) {
            this.getCodeContext().writeBranch(Opcode.GOTO, dst);
    //        this.getCodeContext().currentInserter().setStackMap(null);
        }

    /**
     * @param opIdx {@link #EQ} or {@link #NE}
     */
    private void
    if_acmpxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx == UnitCompiler.EQ || opIdx == UnitCompiler.NE : opIdx;

        this.addLineNumberOffset(locatable);
        this.getCodeContext().writeBranch(Opcode.IF_ACMPEQ + opIdx, dst);
        this.getCodeContext().popReferenceOperand();
        this.getCodeContext().popReferenceOperand();
        dst.setStackMap(this.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    private void
    if_icmpxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx >= UnitCompiler.EQ && opIdx <= UnitCompiler.LE;

        this.addLineNumberOffset(locatable);
        this.getCodeContext().writeBranch(Opcode.IF_ICMPEQ + opIdx, dst);
        this.getCodeContext().popIntOperand();
        this.getCodeContext().popIntOperand();
        dst.setStackMap(this.getCodeContext().currentInserter().getStackMap());
    }

    private static final int EQ = 0;

    private static final int NE = 1;

    private static final int LT = 2;

    private static final int GE = 3;

    private static final int GT = 4;

    private static final int LE = 5;

    private void
    ifnonnull(Locatable locatable, CodeContext.Offset dst) {
        this.getCodeContext().writeBranch(Opcode.IFNONNULL, dst);
        this.getCodeContext().popReferenceOperand();
        dst.setStackMap(this.getCodeContext().currentInserter().getStackMap());
    }

    private void
    ifnull(Locatable locatable, CodeContext.Offset dst) {
        this.getCodeContext().writeBranch(Opcode.IFNULL, dst);
        this.getCodeContext().popReferenceOperand();
        dst.setStackMap(this.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param opIdx One of {@link #EQ}, {@link #NE}, {@link #LT}, {@link #GE}, {@link #GT} or {@link #LE}
     */
    private void
    ifxx(Locatable locatable, int opIdx, CodeContext.Offset dst) {
        assert opIdx >= UnitCompiler.EQ && opIdx <= UnitCompiler.LE;

        this.addLineNumberOffset(locatable);
        this.getCodeContext().writeBranch(Opcode.IFEQ + opIdx, dst);
        this.getCodeContext().popIntOperand();
        dst.setStackMap(this.getCodeContext().currentInserter().getStackMap());
    }

    /**
     * @param operator Must be either "++" or "--", as an @link {@link String#intern() interned} string
     */
    private void
    iinc(Locatable locatable, LocalVariable lv, String operator) {
        this.addLineNumberOffset(locatable);
        if (lv.getSlotIndex() > 255) {
            this.write(Opcode.WIDE);
            this.write(Opcode.IINC);
            this.writeShort(lv.getSlotIndex());
            this.writeShort(operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        } else {
            this.write(Opcode.IINC);
            this.writeByte(lv.getSlotIndex());
            this.writeByte(operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        }
    }

    private void
    instanceoF(Locatable locatable, IClass rhsType) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popReferenceOperand();
        this.write(Opcode.INSTANCEOF);
        this.writeConstantClassInfo(rhsType);
        this.getCodeContext().pushIntOperand();
    }

    /**
     * Expects the target object and the arguments on the operand stack.
     * @throws CompileException
     */
    private void
    invokeMethod(
        Locatable        locatable,
        int              opcode,
        IClass           declaringIClass,
        String           methodName,
        MethodDescriptor methodDescriptor,
        boolean          useInterfaceMethodRef
    ) throws CompileException {

        this.addLineNumberOffset(locatable);

        for (int i = methodDescriptor.parameterFds.length - 1; i >= 0; i--) {
            this.getCodeContext().popOperandAssignableTo(methodDescriptor.parameterFds[i]);
        }
        if (opcode == Opcode.INVOKEINTERFACE || opcode == Opcode.INVOKESPECIAL || opcode == Opcode.INVOKEVIRTUAL) {
            this.getCodeContext().popObjectOrUninitializedOrUninitializedThisOperand();
        }

        this.write(opcode);
        if (useInterfaceMethodRef) {
            this.writeConstantInterfaceMethodrefInfo(declaringIClass, methodName, methodDescriptor);
        } else {
            this.writeConstantMethodrefInfo(declaringIClass, methodName, methodDescriptor);
        }

        switch (opcode) {

        case Opcode.INVOKEDYNAMIC:
            this.writeByte(0);
            this.writeByte(0);
            break;

        case Opcode.INVOKEINTERFACE:
            int count = 1;
            for (String pfd : methodDescriptor.parameterFds) count += Descriptor.size(pfd);
            this.writeByte(count);
            this.writeByte(0);
            break;

        case Opcode.INVOKESPECIAL:
        case Opcode.INVOKESTATIC:
        case Opcode.INVOKEVIRTUAL:
            ;
            break;

        default:
            throw new AssertionError(opcode);
        }

        if (!methodDescriptor.returnFd.equals(Descriptor.VOID)) {
            this.getCodeContext().pushOperand(methodDescriptor.returnFd);
        }
    }

    private void
    l2i(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popLongOperand();
        this.write(Opcode.L2I);
        this.getCodeContext().pushIntOperand();

    }

    // Load the value of a local variable onto the stack and return its type.
    private IClass
    load(Locatable locatable, LocalVariable localVariable) {
        this.load(locatable, localVariable.type, localVariable.getSlotIndex());
        return localVariable.type;
    }

    private void
    load(Locatable locatable, IClass localVariableType, int localVariableIndex) {
        assert localVariableIndex >= 0 && localVariableIndex <= 65535;

        this.addLineNumberOffset(locatable);

        if (localVariableIndex <= 3) {
            this.write(Opcode.ILOAD_0 + 4 * UnitCompiler.ilfda(localVariableType) + localVariableIndex);
        } else
        if (localVariableIndex <= 255) {
            this.write(Opcode.ILOAD + UnitCompiler.ilfda(localVariableType));
            this.write(localVariableIndex);
        } else
        {
            this.write(Opcode.WIDE);
            this.write(Opcode.ILOAD + UnitCompiler.ilfda(localVariableType));
            this.writeUnsignedShort(localVariableIndex);
        }

        this.getCodeContext().pushOperand(localVariableType.getDescriptor());
    }

    private void
    lookupswitch(
        Locatable                  locatable,
        SortedMap<Integer, Offset> caseLabelMap,
        Offset                     switchOffset,
        Offset                     defaultLabelOffset
    ) {

        this.addLineNumberOffset(locatable);
        this.getCodeContext().popIntOperand();
        this.write(Opcode.LOOKUPSWITCH);                                            // lookupswitch
        new Padder(this.getCodeContext()).set();                                    // 0-3 byte pad
        this.writeOffset(switchOffset, defaultLabelOffset);                         // defaultbyte1-4
        this.writeInt(caseLabelMap.size());                                         // npairs1-4
        for (Map.Entry<Integer, CodeContext.Offset> me : caseLabelMap.entrySet()) {
            this.writeInt((Integer) me.getKey());                                   // match
            this.writeOffset(switchOffset, (CodeContext.Offset) me.getValue());     // offset
        }
    }

    private void
    monitorenter(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popReferenceOperand();
        this.write(Opcode.MONITORENTER);
    }

    private void
    monitorexit(Locatable locatable) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popReferenceOperand();
        this.write(Opcode.MONITOREXIT);
    }

    /**
     * @param operator One of {@code * / % + -}
     */
    private void
    mulDivRemAddSub(Locatable locatable, String operator) {

        VerificationTypeInfo operand2 = this.getCodeContext().popOperand();
        VerificationTypeInfo operand1 = this.getCodeContext().popOperand();
        assert operand1 == operand2 : operand1 + " vs. " + operand2;

        final int opcode = (
            operator == "*"   ? Opcode.IMUL :
            operator == "/"   ? Opcode.IDIV :
            operator == "%"   ? Opcode.IREM :
            operator == "+"   ? Opcode.IADD :
            operator == "-"   ? Opcode.ISUB :
            Integer.MAX_VALUE
        ) + UnitCompiler.ilfd(operand1);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.getCodeContext().pushOperand(operand1);
    }

    private void
    multianewarray(Locatable locatable, int dimExprCount, int dims, IClass componentType) {
        IClass arrayType = componentType.getArrayIClass(dimExprCount + dims, this.iClassLoader.TYPE_java_lang_Object);

        this.addLineNumberOffset(locatable);
        for (int i = 0; i < dimExprCount; i++) this.getCodeContext().popIntOperand();
        this.write(Opcode.MULTIANEWARRAY);
        this.writeConstantClassInfo(arrayType);
        this.writeByte(dimExprCount);
        this.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    /**
     * @param operandType One of BYTE, CHAR, INT, SHORT, LONG, BOOLEAN, LONG, FLOAT, DOUBLE
     */
    private void
    neg(Locatable locatable, IClass operandType) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.INEG + UnitCompiler.ilfd(operandType));
    }

    private void
        neW(Locatable locatable, IClass iClass) {
            this.addLineNumberOffset(locatable);
            this.getCodeContext().pushUninitializedOperand();
            this.write(Opcode.NEW);
            this.writeConstantClassInfo(iClass);
        }

    private void
    newarray(Locatable locatable, IClass componentType) {
        IClass arrayType = componentType.getArrayIClass(this.iClassLoader.TYPE_java_lang_Object);

        this.addLineNumberOffset(locatable);
        this.getCodeContext().popIntOperand();
        this.write(Opcode.NEWARRAY);
        this.writeByte((
            componentType == IClass.BOOLEAN ? 4 :
            componentType == IClass.CHAR    ? 5 :
            componentType == IClass.FLOAT   ? 6 :
            componentType == IClass.DOUBLE  ? 7 :
            componentType == IClass.BYTE    ? 8 :
            componentType == IClass.SHORT   ? 9 :
            componentType == IClass.INT     ? 10 :
            componentType == IClass.LONG    ? 11 : -1
        ));
        this.getCodeContext().pushObjectOperand(arrayType.getDescriptor());
    }

    private void
    pop(Locatable locatable, IClass type) {

        if (type == IClass.VOID) return;

        this.addLineNumberOffset(locatable);
        this.write(type == IClass.LONG || type == IClass.DOUBLE ? Opcode.POP2 : Opcode.POP);
        this.getCodeContext().popOperand(type.getDescriptor());
    }

    private void
    putfield(Locatable locatable, IField iField) throws CompileException {
        this.addLineNumberOffset(locatable);
        this.getCodeContext().popOperand();
        if (iField.isStatic()) {
            this.write(Opcode.PUTSTATIC);
        } else {
            this.write(Opcode.PUTFIELD);
            this.getCodeContext().popOperand();
        }
        this.writeConstantFieldrefInfo(
            iField.getDeclaringIClass(), // iClass
            iField.getName(),            // fieldName
            iField.getType()             // fieldType
        );
    }

    private void
    returN(Locatable locatable) {
        this.addLineNumberOffset(locatable);
        this.write(Opcode.RETURN);
    }

    /**
     * @param operator One of {@code << >> >>>}
     */
    private void
    shift(Locatable locatable, String operator) {

        this.getCodeContext().popIntOperand();
        VerificationTypeInfo operand1 = this.getCodeContext().popIntOrLongOperand();

        final int iopcode = (
            operator == "<<"  ? Opcode.ISHL  :
            operator == ">>"  ? Opcode.ISHR  :
            operator == ">>>" ? Opcode.IUSHR :
            Integer.MAX_VALUE
        );

        int opcode = iopcode + UnitCompiler.il(operand1);

        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.getCodeContext().pushOperand(operand1);
    }

    /**
     * Assigns top stack top value to the given local variable.
     */
    private void
    store(Locatable locatable, LocalVariable localVariable) {
        this.store(
            locatable,                   // locatable
            localVariable.type,          // lvType
            localVariable.getSlotIndex() // lvIndex
        );
    }
    private void
    store(Locatable locatable, IClass lvType, short lvIndex) {
        this.addLineNumberOffset(locatable);

        if (lvIndex <= 3) {
            this.write(Opcode.ISTORE_0 + 4 * UnitCompiler.ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            this.write(Opcode.ISTORE + UnitCompiler.ilfda(lvType));
            this.write(lvIndex);
        } else
        {
            this.write(Opcode.WIDE);
            this.write(Opcode.ISTORE + UnitCompiler.ilfda(lvType));
            this.writeUnsignedShort(lvIndex);
        }
        this.getCodeContext().popOperand();

        this.updateLocalVariableInCurrentStackMap(lvIndex, this.verificationTypeInfo(lvType));
    }

    private void
    sub(Locatable locatable) { this.mulDivRemAddSub(locatable, "-"); }

    private void
    swap(Locatable locatable) {

        VerificationTypeInfo topOperand       = this.getCodeContext().popOperand();
        VerificationTypeInfo topButOneOperand = this.getCodeContext().popOperand();

        this.addLineNumberOffset(locatable);
        this.write(Opcode.SWAP);

        this.getCodeContext().pushOperand(topOperand);
        this.getCodeContext().pushOperand(topButOneOperand);
    }

    private void
    tableswitch(
        Locatable                              locatable,
        SortedMap<Integer, CodeContext.Offset> caseLabelMap,
        Offset                                 switchOffset,
        Offset                                 defaultLabelOffset
    ) {
        final int low  = (Integer) caseLabelMap.firstKey();
        final int high = (Integer) caseLabelMap.lastKey();

        this.addLineNumberOffset(locatable);
        this.getCodeContext().popIntOperand();
        this.write(Opcode.TABLESWITCH);
        new Padder(this.getCodeContext()).set();
        this.writeOffset(switchOffset, defaultLabelOffset);
        this.writeInt(low);
        this.writeInt(high);

        int cur = low;
        for (Map.Entry<Integer, CodeContext.Offset> me : caseLabelMap.entrySet()) {
            int                caseLabelValue  = (Integer) me.getKey();
            CodeContext.Offset caseLabelOffset = (CodeContext.Offset) me.getValue();

            while (cur < caseLabelValue) {
                this.writeOffset(switchOffset, defaultLabelOffset);
                ++cur;
            }
            this.writeOffset(switchOffset, caseLabelOffset);
            ++cur;
        }

    }

    private void
    xaload(Locatable locatable, IClass componentType) {
        this.addLineNumberOffset(locatable);

        this.getCodeContext().popIntOperand();
        this.getCodeContext().popReferenceOperand();
        this.write(Opcode.IALOAD + UnitCompiler.ilfdabcs(componentType));
        this.getCodeContext().pushOperand(componentType.getDescriptor());
    }

    private void
    xor(Locatable locatable, int opcode) {
        if (opcode != Opcode.IXOR && opcode != Opcode.LXOR) throw new AssertionError(opcode);
        this.addLineNumberOffset(locatable);
        this.write(opcode);
        this.getCodeContext().popOperand();
    }

    private void
    xreturn(Locatable locatable, IClass returnType) {
        this.addLineNumberOffset(locatable);

        this.write(Opcode.IRETURN + UnitCompiler.ilfda(returnType));

        this.getCodeContext().popOperandAssignableTo(returnType.getDescriptor());
    }

    /**
     * @param t One of BYTE, CHAR, INT, SHORT, LONG, BOOLEAN, LONG, FLOAT, DOUBLE
     */
    private static int
    ilfd(final IClass t) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            return 0;
        }
        if (t == IClass.LONG)   return 1;
        if (t == IClass.FLOAT)  return 2;
        if (t == IClass.DOUBLE) return 3;
        throw new InternalCompilerException("Unexpected type \"" + t + "\"");
    }

    private static int
    ilfd(VerificationTypeInfo vti) {
        if (vti == StackMapTableAttribute.INTEGER_VARIABLE_INFO) return 0;
        if (vti == StackMapTableAttribute.LONG_VARIABLE_INFO)    return 1;
        if (vti == StackMapTableAttribute.FLOAT_VARIABLE_INFO)   return 2;
        if (vti == StackMapTableAttribute.DOUBLE_VARIABLE_INFO)  return 3;
        throw new InternalCompilerException("Unexpected type \"" + vti + "\"");
    }

    private static int
    ilfda(IClass t) { return !t.isPrimitive() ? 4 : UnitCompiler.ilfd(t); }

    private static int
    il(VerificationTypeInfo vti) {

        if (vti == StackMapTableAttribute.INTEGER_VARIABLE_INFO)  return 0;
        if (vti == StackMapTableAttribute.LONG_VARIABLE_INFO)     return 1;

        throw new AssertionError(vti);
    }

    private static int
    ilfdabcs(IClass t) {
        if (t == IClass.INT)     return 0;
        if (t == IClass.LONG)    return 1;
        if (t == IClass.FLOAT)   return 2;
        if (t == IClass.DOUBLE)  return 3;
        if (!t.isPrimitive())    return 4;
        if (t == IClass.BOOLEAN) return 5;
        if (t == IClass.BYTE)    return 5;
        if (t == IClass.CHAR)    return 6;
        if (t == IClass.SHORT)   return 7;
        throw new InternalCompilerException("Unexpected type \"" + t + "\"");
    }

    /**
     * Finds a named field in the given {@link IClass}. Honors superclasses and interfaces. See JLS7 8.3.
     *
     * @return {@code null} if no field is found
     */
    @Nullable private IClass.IField
    findIField(IClass iClass, String name, Location location) throws CompileException {

        // Search for a field with the given name in the current class.
        IClass.IField f = iClass.getDeclaredIField(name);
        if (f != null) return f;

        // Examine superclass.
        {
            IClass superclass = iClass.getSuperclass();
            if (superclass != null) f = this.findIField(superclass, name, location);
        }

        // Examine interfaces.
        IClass[] ifs = iClass.getInterfaces();
        for (IClass iF : ifs) {
            IClass.IField f2 = this.findIField(iF, name, location);
            if (f2 != null) {
                if (f != null) {
                    throw new CompileException((
                        "Access to field \""
                        + name
                        + "\" is ambiguous - both \""
                        + f.getDeclaringIClass()
                        + "\" and \""
                        + f2.getDeclaringIClass()
                        + "\" declare it"
                    ), location);
                }
                f = f2;
            }
        }
        return f;
    }

    /**
     * Finds a named type in the given {@link IClass}. Honors superclasses, interfaces and enclosing type declarations.
     *
     * @return {@code null} if no type with the given name is found
     */
    @Nullable private IClass
    findMemberType(IClass iClass, String name, Location location) throws CompileException {
        IClass[] types = iClass.findMemberType(name);
        if (types.length == 0) return null;
        if (types.length == 1) return types[0];

        StringBuilder sb = new StringBuilder("Type \"").append(name).append("\" is ambiguous: ").append(types[0]);
        for (int i = 1; i < types.length; ++i) sb.append(" vs. ").append(types[i].toString());
        this.compileError(sb.toString(), location);
        return types[0];
    }

    /**
     * Finds one class or interface declaration in this compilation unit and resolves it into an {@link IClass}.
     *
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
     * @return          {@code null} if a class or an interface with that name is not declared in this compilation unit
     */
    @Nullable public IClass
    findClass(String className) {

        AbstractCompilationUnit acu = this.abstractCompilationUnit;
        if (!(acu instanceof CompilationUnit)) return null;
        CompilationUnit cu = (CompilationUnit) acu;

        // Examine package name.
        PackageDeclaration opd = cu.packageDeclaration;
        if (opd != null) {
            String packageName = opd.packageName;
            if (!className.startsWith(packageName + '.')) return null;
            className = className.substring(packageName.length() + 1);
        }

        // Attempt to find the type declaration by name "as is", i.e. including any dollar signs.
        TypeDeclaration td = cu.getPackageMemberTypeDeclaration(className);
        if (td == null) {

            int idx = className.indexOf('$');
            if (idx == -1) return null;
            StringTokenizer st = new StringTokenizer(className, "$");
            td = cu.getPackageMemberTypeDeclaration(st.nextToken());
            if (td == null) return null;
            while (st.hasMoreTokens()) {
                td = td.getMemberTypeDeclaration(st.nextToken());
                if (td == null) return null;
            }
        }

        return this.resolve(td);
    }

    /**
     * Equivalent with {@link #compileError(String, Location)} with a {@code null} location argument.
     */
    private void
    compileError(String message) throws CompileException { this.compileError(message, null); }

    /**
     * Issues a compile error with the given message. This is done through the {@link ErrorHandler} that was installed
     * through {@link #setCompileErrorHandler(ErrorHandler)}. Such a handler typically throws a {@link
     * CompileException}, but it may as well decide to return normally. Consequently, the calling code must be prepared
     * that {@link #compileError(String, Location)} returns normally, and must attempt to continue compiling.
     *
     * @param message          The message to report
     * @param location The location to report
     */
    private void
    compileError(String message, @Nullable Location location) throws CompileException {
        ++this.compileErrorCount;
        if (this.compileErrorHandler != null) {
            this.compileErrorHandler.handleError(message, location);
        } else {
            throw new CompileException(message, location);
        }
    }

    /**
     * Issues a warning with the given message an location an returns. This is done through a {@link WarningHandler}
     * that was installed through {@link #setWarningHandler(WarningHandler)}.
     * <p>
     *   The <var>handle</var> argument qualifies the warning and is typically used by the {@link WarningHandler} to
     *   suppress individual warnings.
     * </p>
     */
    private void
    warning(String handle, String message, @Nullable Location location) throws CompileException {
        if (this.warningHandler != null) {
            this.warningHandler.handleWarning(handle, message, location);
        }
    }

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application my install its own
     * (thread-local) {@link ErrorHandler}.
     * <p>
     *   Be aware that a single problem during compilation often causes a bunch of compile errors, so a good {@link
     *   ErrorHandler} counts errors and throws a {@link CompileException} when a limit is reached.
     * </p>
     * <p>
     *   If the given {@link ErrorHandler} does not throw {@link CompileException}s, then {@link
     *   #compileUnit(boolean, boolean, boolean)} will throw one when the compilation of the unit is finished, and
     *   errors had occurred. In other words: The {@link ErrorHandler} may throw a {@link CompileException} or not, but
     *   {@link #compileUnit(boolean, boolean, boolean)} will definitely throw a {@link CompileException} if one or
     *   more compile errors have occurred.
     * </p>
     *
     * @param compileErrorHandler {@code null} to restore the default behavior (throwing a {@link
     *                                    CompileException})
     */
    public void
    setCompileErrorHandler(@Nullable ErrorHandler compileErrorHandler) {
        this.compileErrorHandler = compileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param warningHandler {@code null} to indicate that no warnings be issued
     */
    public void
    setWarningHandler(@Nullable WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
    }

    @Nullable private CodeContext
    replaceCodeContext(@Nullable CodeContext newCodeContext) {
        CodeContext oldCodeContext = this.codeContext;
        this.codeContext = newCodeContext;
        return oldCodeContext;
    }

    private void
    addLineNumberOffset(Locatable locatable) {
        this.getCodeContext().addLineNumberOffset(locatable.getLocation().getLineNumber());
    }

    private void
    write(int v) { this.getCodeContext().write((byte) v); }

    private void
    writeByte(int v) {
        if (v > Byte.MAX_VALUE - Byte.MIN_VALUE) {
            throw new InternalCompilerException("Byte value out of legal range");
        }
        this.getCodeContext().write((byte) v);
    }
    private void
    writeShort(int v) {
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
            throw new InternalCompilerException("Short value out of legal range");
        }
        this.getCodeContext().write((byte) (v >> 8), (byte) v);
    }
    private void
    writeUnsignedShort(int v) {
        if (v < 0 || v > 65535) {
            throw new InternalCompilerException("Unsigned short value out of legal range");
        }
        this.getCodeContext().write((byte) (v >> 8), (byte) v);
    }
    private void
    writeInt(int v) {
        this.getCodeContext().write((byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v);
    }

    private void
    writeLdc(short constantPoolIndex) {
        if (constantPoolIndex >= 0 && constantPoolIndex <= 255) {
            this.write(Opcode.LDC);
            this.write(constantPoolIndex);
        } else {
            this.write(Opcode.LDC_W);
            this.writeShort(constantPoolIndex);
        }
    }

    private void
    writeLdc2(short constantPoolIndex) {
        this.write(Opcode.LDC2_W);
        this.getCodeContext().writeShort(constantPoolIndex);
    }

    /**
     * Invokes the <var>iMethod</var>; assumes that {@code this} (unless <var>iMethod</var> is static) and the correct
     * number and types of arguments are on the operand stack.
     */
    private void
    invoke(Locatable locatable, IMethod iMethod) throws CompileException {

        if (iMethod.isStatic()) {

            // Static class method, or a static interface method (a Java 8 feature).
            final ClassFile cf = this.getCodeContext().getClassFile();
            if (
                iMethod.getDeclaringIClass().isInterface()
                && cf.getMajorVersion() <= ClassFile.MAJOR_VERSION_JDK_1_7
                && cf.getMinorVersion() <= ClassFile.MINOR_VERSION_JDK_1_7
            ) {
                // INVOKESTATIC InterfaceMethodRef only allowed since Java 8 class file format.
                this.compileError(
                    "Invocation of static interface methods NYI",
                    locatable.getLocation()
                );
            }

            this.invokeMethod(
                locatable,                    // locatable
                Opcode.INVOKESTATIC,          // opcode
                iMethod.getDeclaringIClass(), // declaringIClass
                iMethod.getName(),            // methodName
                iMethod.getDescriptor(),      // methodMd
                false                         // useInterfaceMethodref
            );
        } else
        if (iMethod.getDeclaringIClass().isInterface()) {

            // A non-static interface method.
            this.invokeMethod(
                locatable,                    // locatable
                Opcode.INVOKEINTERFACE,       // opcode
                iMethod.getDeclaringIClass(), // declaringIClass
                iMethod.getName(),            // methodName
                iMethod.getDescriptor(),      // methodMD
                true                          // useInterfaceMethodref
            );
        } else
        {

            // A non-static class mathod.
            this.invokeMethod(
                locatable,                    // locatable
                Opcode.INVOKEVIRTUAL,         // opcode
                iMethod.getDeclaringIClass(), // declaringIClass
                iMethod.getName(),            // methodName
                iMethod.getDescriptor(),      // methodMd
                false                         // useInterfaceMethodref
            );
        }
    }

    /**
     * Invokes the <var>iConstructor</var>; assumes that {@code this} and the correct number and types of arguments are
     * on the operand stack.
     */
    private void
    invoke(Locatable locatable, IConstructor iConstructor) throws CompileException {
        this.invokeMethod(
            locatable,                         // locatable
            Opcode.INVOKESPECIAL,              // opcode
            iConstructor.getDeclaringIClass(), // declaringIClass
            "<init>",                          // methodName
            iConstructor.getDescriptor(),      // methodMd
            false                              // useInterfaceMethodref
        );
    }

    private void
    writeOffset(CodeContext.Offset src, final CodeContext.Offset dst) {
        this.getCodeContext().writeOffset(src, dst);
    }

    // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

    private short
    addConstantStringInfo(String value) {
        return this.getCodeContext().getClassFile().addConstantStringInfo(value);
    }
    private short
    addConstantIntegerInfo(int value) {
        return this.getCodeContext().getClassFile().addConstantIntegerInfo(value);
    }
    private short
    addConstantLongInfo(long value) {
        return this.getCodeContext().getClassFile().addConstantLongInfo(value);
    }
    private short
    addConstantFloatInfo(float value) {
        return this.getCodeContext().getClassFile().addConstantFloatInfo(value);
    }
    private short
    addConstantDoubleInfo(double value) {
        return this.getCodeContext().getClassFile().addConstantDoubleInfo(value);
    }
    private short
    addConstantClassInfo(IClass iClass) {
        return this.getCodeContext().getClassFile().addConstantClassInfo(iClass.getDescriptor());
    }
    private short
    addConstantFieldrefInfo(IClass iClass, String fieldName, IClass fieldType) {
        return this.getCodeContext().getClassFile().addConstantFieldrefInfo(iClass.getDescriptor(), fieldName, fieldType.getDescriptor());
    }
    private short
    addConstantMethodrefInfo(IClass iClass, String methodName, String methodFd) {
        return this.getCodeContext().getClassFile().addConstantMethodrefInfo(iClass.getDescriptor(), methodName, methodFd);
    }
    private short
    addConstantInterfaceMethodrefInfo(IClass iClass, String methodName, String methodFd) {
        return this.getCodeContext().getClassFile().addConstantInterfaceMethodrefInfo(iClass.getDescriptor(), methodName, methodFd);
    }

/* UNUSED
    private void writeConstantIntegerInfo(int value) {
        this.getCodeContext().writeShort(-1, this.addConstantIntegerInfo(value));
    }
*/
    private void
    writeConstantClassInfo(IClass iClass) {
        this.writeShort(this.addConstantClassInfo(iClass));
    }
    private void
    writeConstantFieldrefInfo(IClass iClass, String fieldName, IClass fieldType) {
        this.writeShort(this.addConstantFieldrefInfo(iClass, fieldName, fieldType));
    }
    private void
    writeConstantMethodrefInfo(IClass iClass, String methodName, MethodDescriptor methodMd) {
        this.writeShort(this.addConstantMethodrefInfo(iClass, methodName, methodMd.toString()));
    }
    private void
    writeConstantInterfaceMethodrefInfo(IClass iClass, String methodName, MethodDescriptor methodMd) {
        this.writeShort(this.addConstantInterfaceMethodrefInfo(iClass, methodName, methodMd.toString()));
    }
/* UNUSED
    private void writeConstantStringInfo(String value) {
        this.getCodeContext().writeShort(-1, this.addConstantStringInfo(value));
    }
    private void
    writeConstantLongInfo(long value) {
        this.getCodeContext().writeShort(this.addConstantLongInfo(value));
    }
    private void writeConstantFloatInfo(float value) {
        this.getCodeContext().writeShort(-1, this.addConstantFloatInfo(value));
    }
    private void
    writeConstantDoubleInfo(double value) {
        this.getCodeContext().writeShort(this.addConstantDoubleInfo(value));
    }
*/

    private CodeContext.Offset
    getWhereToBreak(BreakableStatement bs) {

        if (bs.whereToBreak != null) return bs.whereToBreak;

        return (bs.whereToBreak = this.getCodeContext().new Offset());
    }

    private TypeBodyDeclaration
    getDeclaringTypeBodyDeclaration(QualifiedThisReference qtr) throws CompileException {

        if (qtr.declaringTypeBodyDeclaration != null) return qtr.declaringTypeBodyDeclaration;

        // Compile error if in static function context.
        Scope s;
        for (
            s = qtr.getEnclosingScope();
            !(s instanceof TypeBodyDeclaration);
            s = s.getEnclosingScope()
        );
        TypeBodyDeclaration result = (TypeBodyDeclaration) s;

        if (UnitCompiler.isStaticContext(result)) {
            this.compileError("No current instance available in static method", qtr.getLocation());
        }

        // Determine declaring type.
        qtr.declaringClass = (AbstractClassDeclaration) result.getDeclaringType();

        return (qtr.declaringTypeBodyDeclaration = result);
    }

    private AbstractClassDeclaration
    getDeclaringClass(QualifiedThisReference qtr) throws CompileException {

        if (qtr.declaringClass != null) return qtr.declaringClass;

        this.getDeclaringTypeBodyDeclaration(qtr);
        assert qtr.declaringClass != null;
        return qtr.declaringClass;
    }

    private void
    referenceThis(Locatable locatable, IClass currentIClass) {
        this.load(
            locatable,
            currentIClass, // localVariableType
            0              // localVariableIndex
        );
    }

    /**
     * Expects <var>dimExprCount</var> values of type {@code int} on the operand stack. Creates an array of
     * <var>dimExprCount</var> {@code +} <var>dims</var> dimensions of <var>componentType</var>.
     *
     * @return The type of the created array
     */
    private IClass
    newArray(Locatable locatable, int dimExprCount, int dims, IClass componentType) {

        if (dimExprCount == 1 && dims == 0 && componentType.isPrimitive()) {

            // "new <primitive>[<size>]"
            this.newarray(locatable, componentType);
        } else
        if (dimExprCount == 1) {

            // "new <class-or-interface> [<size>]"
            // "new <anything> [<size>] []{dims}"
            this.anewarray(locatable, componentType.getArrayIClass(dims, this.iClassLoader.TYPE_java_lang_Object));
        } else
        {

            // "new <anything> []{dims}"
            // "new <anything> [<size>]{dimexprCount}"
            // "new <anything> [<size>]{dimexprCount} []{dims}"
            this.multianewarray(locatable, dimExprCount, dims, componentType);
        }

        return componentType.getArrayIClass(dimExprCount + dims, this.iClassLoader.TYPE_java_lang_Object);
    }

    /**
     * Short-hand implementation of {@link IClass.IField} that implements a non-constant, non-static,
     * package-accessible field.
     */
    public static
    class SimpleIField extends IClass.IField {
        private final String name;
        private final IClass type;

        public
        SimpleIField(IClass declaringIClass, String name, IClass type) {
            declaringIClass.super();
            this.name = name;
            this.type = type;
        }

        @Override public Object        getConstantValue() { return UnitCompiler.NOT_CONSTANT; }
        @Override public String        getName()          { return this.name;                 }
        @Override public IClass        getType()          { return this.type;                 }
        @Override public boolean       isStatic()         { return false;                     }
        @Override public Access        getAccess()        { return Access.DEFAULT;            }
        @Override public IAnnotation[] getAnnotations()   { return new IAnnotation[0];        }
    }

    private static String
    last(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        return sa[sa.length - 1];
    }

    private static String[]
    allButLast(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        String[] tmp = new String[sa.length - 1];
        System.arraycopy(sa, 0, tmp, 0, tmp.length);
        return tmp;
    }

    private static String[]
    concat(String[] sa, String s) {
        String[] tmp = new String[sa.length + 1];
        System.arraycopy(sa, 0, tmp, 0, sa.length);
        tmp[sa.length] = s;
        return tmp;
    }

    private static CompileException
    compileException(Locatable locatable, String message) {
        return new CompileException(message, locatable.getLocation());
    }

    /**
     * Decodes any escape sequences like {@code \n}, or {@code \377}, but <em>not</em> {@code &#92;uxxxx}.
     *
     * @return                           <var>s</var>, with all escape sequences replaced with their literal values
     * @throws CompileException          <var>s</var> contains an invalid escape sequence
     * @throws IndexOutOfBoundsException <var>s</var>ends with a backslash
     * @see                              JLS8, section 3.10.6, "Escape Sequences for Character and String Literals"
     */
    private static String
    unescape(String s, @Nullable Location location) throws CompileException {

        // Find the first backslash.
        int i = s.indexOf('\\');

        if (i == -1) {

            // Subject string contains no backslash and thus no escape sequences; so return the original string.
            return s;
        }

        StringBuilder sb = new StringBuilder().append(s, 0, i);

        while (i < s.length()) {

            char c = s.charAt(i++);
            if (c != '\\') {
                sb.append(c);
                continue;
            }

            c = s.charAt(i++);
            {
                int idx = "btnfr\"'\\".indexOf(c);
                if (idx != -1) {
                    sb.append("\b\t\n\f\r\"'\\".charAt(idx));
                    continue;
                }
            }

            // Must be an an OctalEscape (JLS8, section 3.10.6).
            int x = Character.digit(c, 8);
            if (x == -1) throw new CompileException("Invalid escape sequence \"\\" + c + "\"", location);

            if (i < s.length()) {
                c = s.charAt(i);
                int secondDigit = Character.digit(c, 8);
                if (secondDigit != -1) {
                    x = 8 * x + secondDigit;
                    i++;
                    if (i < s.length() && x <= 037) {
                        c = s.charAt(i);
                        int thirdDigit = Character.digit(c, 8);
                        if (thirdDigit != -1) {
                            x = 8 * x + thirdDigit;
                            i++;
                        }
                    }
                }
            }
            sb.append((char) x);
        }

        return sb.toString();
    }

    private short
    accessFlags(Modifier[] modifiers) throws CompileException {
        int result = 0;
        for (Modifier m : modifiers) {
            if (m instanceof AccessModifier) {
                String kw = ((AccessModifier) m).keyword;
                if ("public".equals(kw)) {
                    result |= Mod.PUBLIC;
                } else
                if ("private".equals(kw)) {
                    result |= Mod.PRIVATE;
                } else
                if ("protected".equals(kw)) {
                    result |= Mod.PROTECTED;
                } else
                if ("static".equals(kw)) {
                    result |= Mod.STATIC;
                } else
                if ("final".equals(kw)) {
                    result |= Mod.FINAL;
                } else
                if ("synchronized".equals(kw)) {
                    result |= Mod.SYNCHRONIZED;
                } else
                if ("volatile".equals(kw)) {
                    result |= Mod.VOLATILE;
                } else
                if ("transient".equals(kw)) {
                    result |= Mod.TRANSIENT;
                } else
                if ("native".equals(kw)) {
                    result |= Mod.NATIVE;
                } else
                if ("abstract".equals(kw)) {
                    result |= Mod.ABSTRACT;
                } else
                if ("strictfp".equals(kw)) {
                    result |= Mod.STRICTFP;
                } else
                if ("default".equals(kw)) {
                    ;
                } else {
                    this.compileError("Invalid modifier \"" + kw + "\"");
                }
            }
        }
        return (short) result;
    }

    private static Modifier[]
    accessModifiers(Location location, String... keywords) {
        Modifier[] result = new Modifier[keywords.length];
        for (int i = 0; i < keywords.length; i++) {
            result[i] = new AccessModifier(keywords[i], location);
        }
        return result;
    }

    private static short
    changeAccessibility(short accessFlags, short newAccessibility) {
        return (short) ((accessFlags & ~(Mod.PUBLIC | Mod.PROTECTED | Mod.PRIVATE)) | newAccessibility);
    }

    private void
    updateLocalVariableInCurrentStackMap(short lvIndex, VerificationTypeInfo vti) {

        final Inserter ci = this.getCodeContext().currentInserter();

        int nextLvIndex = 0;
        final VerificationTypeInfo[] locals = ci.getStackMap().locals();
        for (int i = 0; i < locals.length; i++) {
            VerificationTypeInfo vti2 = locals[i];
            if (nextLvIndex == lvIndex) {
                if (vti.equals(vti2)) return;
                locals[i] = vti;
                ci.setStackMap(new StackMap(locals, ci.getStackMap().operands()));
                return;
            }
            nextLvIndex += vti2.category();
        }
        assert nextLvIndex <= lvIndex;
        while (nextLvIndex < lvIndex) {
            ci.setStackMap(ci.getStackMap().pushLocal(StackMapTableAttribute.TOP_VARIABLE_INFO));
            nextLvIndex++;
        }
        ci.setStackMap(ci.getStackMap().pushLocal(vti));
    }

    // Used to write byte code while compiling one constructor/method.
    @Nullable private CodeContext codeContext;

    // Used for elaborate compile error handling.
    @Nullable private ErrorHandler compileErrorHandler;
    private int                    compileErrorCount;

    // Used for elaborate warning handling.
    @Nullable private WarningHandler warningHandler;

    private final AbstractCompilationUnit abstractCompilationUnit;

    private final IClassLoader iClassLoader;

    /**
     * Non-{@code null} while {@link #compileUnit(boolean, boolean, boolean)} is executing.
     */
    @Nullable private List<ClassFile> generatedClassFiles;

    private boolean debugSource;
    private boolean debugLines;
    private boolean debugVars;
}
