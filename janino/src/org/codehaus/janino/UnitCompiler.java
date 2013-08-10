
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Location;
import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IInvocable;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AbstractTypeDeclaration;
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
import org.codehaus.janino.Java.BasicType;
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
import org.codehaus.janino.Java.ClassDeclaration;
import org.codehaus.janino.Java.ClassLiteral;
import org.codehaus.janino.Java.CompilationUnit;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.CompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.Java.ConditionalExpression;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinuableStatement;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.Crement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.DocCommentable;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldAccessExpression;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.FloatingPointLiteral;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.InnerClassDeclaration;
import org.codehaus.janino.Java.Instanceof;
import org.codehaus.janino.Java.IntegerLiteral;
import org.codehaus.janino.Java.InterfaceDeclaration;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LabeledStatement;
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
import org.codehaus.janino.Java.MemberClassDeclaration;
import org.codehaus.janino.Java.MemberInterfaceDeclaration;
import org.codehaus.janino.Java.MemberTypeDeclaration;
import org.codehaus.janino.Java.MethodDeclarator;
import org.codehaus.janino.Java.MethodInvocation;
import org.codehaus.janino.Java.NamedClassDeclaration;
import org.codehaus.janino.Java.NamedTypeDeclaration;
import org.codehaus.janino.Java.NewAnonymousClassInstance;
import org.codehaus.janino.Java.NewArray;
import org.codehaus.janino.Java.NewClassInstance;
import org.codehaus.janino.Java.NewInitializedArray;
import org.codehaus.janino.Java.NullLiteral;
import org.codehaus.janino.Java.Package;
import org.codehaus.janino.Java.PackageMemberClassDeclaration;
import org.codehaus.janino.Java.PackageMemberInterfaceDeclaration;
import org.codehaus.janino.Java.PackageMemberTypeDeclaration;
import org.codehaus.janino.Java.Padder;
import org.codehaus.janino.Java.ParameterAccess;
import org.codehaus.janino.Java.ParenthesizedExpression;
import org.codehaus.janino.Java.QualifiedThisReference;
import org.codehaus.janino.Java.ReferenceType;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.RvalueMemberType;
import org.codehaus.janino.Java.Scope;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.StringLiteral;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SuperclassMethodInvocation;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThisReference;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.Type;
import org.codehaus.janino.Java.TypeBodyDeclaration;
import org.codehaus.janino.Java.TypeDeclaration;
import org.codehaus.janino.Java.UnaryOperation;
import org.codehaus.janino.Java.VariableDeclarator;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Scanner.Token;
import org.codehaus.janino.Visitor.AtomVisitor;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ElementValueVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.Visitor.LvalueVisitor;
import org.codehaus.janino.Visitor.RvalueVisitor;
import org.codehaus.janino.Visitor.TypeDeclarationVisitor;
import org.codehaus.janino.util.ClassFile;

/**
 * This class actually implements the Java&trade; compiler. It is associated with exactly one compilation unit which it
 * compiles.
 */
@SuppressWarnings({ "rawtypes", "unchecked" }) public
class UnitCompiler {
    private static final boolean DEBUG = false;

    /**
     * This constant determines the number of operands up to which the
     * <pre>
     *      a.concat(b).concat(c)
     * </pre>
     * strategy is used to implement string concatenation. For more operands, the
     * <pre>
     *      new StringBuilder(a).append(b).append(c).append(d).toString()
     * </pre>
     * strategy is chosen.
     * <p>
     * <a href="http://www.tomgibara.com/janino-evaluation/string-concatenation-benchmark">A very good article from Tom
     * Gibara</a> analyzes the impact of this decision and recommends a value of three.
     */
    private static final int STRING_CONCAT_LIMIT = 3;

    public
    UnitCompiler(CompilationUnit compilationUnit, IClassLoader iClassLoader) {
        this.compilationUnit = compilationUnit;
        this.iClassLoader    = iClassLoader;
    }

    private void
    import2(SingleStaticImportDeclaration ssid) throws CompileException {
        String name = last(ssid.identifiers);

        List importedObjects = (List) this.singleStaticImports.get(name);
        if (importedObjects == null) {
            importedObjects = new ArrayList();
            this.singleStaticImports.put(name, importedObjects);
        }

        // Type?
        {
            IClass iClass = this.findTypeByFullyQualifiedName(ssid.getLocation(), ssid.identifiers);
            if (iClass != null) {
                importedObjects.add(iClass);
                return;
            }
        }

        String[] typeName = allButLast(ssid.identifiers);
        IClass   iClass   = this.findTypeByFullyQualifiedName(ssid.getLocation(), typeName);
        if (iClass == null) {
            this.compileError("Could not load \"" + Java.join(typeName, ".") + "\"", ssid.getLocation());
            return;
        }

        // Static field?
        IField iField = iClass.getDeclaredIField(name);
        if (iField != null) {
            if (!iField.isStatic()) {
                this.compileError(
                    "Field \"" + name + "\" of \"" + Java.join(typeName, ".") + "\" must be static",
                    ssid.getLocation()
                );
            }
            importedObjects.add(iField);
            return;
        }

        // Static method?
        IMethod[] ms = iClass.getDeclaredIMethods(name);
        if (ms.length > 0) {
            importedObjects.addAll(Arrays.asList(ms));
            return;
        }

        // Give up.
        this.compileError(
            "\"" + Java.join(typeName, ".") + "\" has no static member \"" + name + "\"",
            ssid.getLocation()
        );
    }
    private void
    import2(StaticImportOnDemandDeclaration siodd) throws CompileException {
        IClass iClass = this.findTypeByFullyQualifiedName(siodd.getLocation(), siodd.identifiers);
        if (iClass == null) {
            this.compileError("Could not load \"" + Java.join(siodd.identifiers, ".") + "\"", siodd.getLocation());
            return;
        }
        this.staticImportsOnDemand.add(iClass);
    }

    /**
     * Generates an array of {@link ClassFile} objects which represent the classes and interfaces declared in the
     * compilation unit.
     */
    public ClassFile[]
    compileUnit(boolean debugSource, boolean debugLines, boolean debugVars) throws CompileException {
        this.debugSource = debugSource;
        this.debugLines  = debugLines;
        this.debugVars   = debugVars;

        // Compile static import declarations.
        // Notice: The single-type and on-demand imports are needed BEFORE the unit is compiled, thus they are
        // processed in 'getSingleTypeImport()' and 'importOnDemand()'.
        for (Iterator it = this.compilationUnit.importDeclarations.iterator(); it.hasNext();) {
            ImportDeclaration id = (ImportDeclaration) it.next();
            class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
            try {
                id.accept(new ImportVisitor() {
                    // CHECKSTYLE LineLengthCheck:OFF
                    @Override public void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          {}
                    @Override public void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     {}
                    @Override public void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      { try { UnitCompiler.this.import2(ssid);  } catch (CompileException e) { throw new UCE(e); } }
                    @Override public void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) { try { UnitCompiler.this.import2(siodd); } catch (CompileException e) { throw new UCE(e); } }
                    // CHECKSTYLE LineLengthCheck:ON
                });
            } catch (UCE uce) {
                throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
            }
        }

        this.generatedClassFiles  = new ArrayList();

        for (Iterator it = this.compilationUnit.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            this.compile((PackageMemberTypeDeclaration) it.next());
        }

        if (this.compileErrorCount > 0) {
            throw new CompileException((
                this.compileErrorCount
                + " error(s) while compiling unit \""
                + this.compilationUnit.optionalFileName
                + "\""
            ), null);
        }

        List l = this.generatedClassFiles;
        return (ClassFile[]) l.toArray(new ClassFile[l.size()]);
    }

    // ------------ TypeDeclaration.compile() -------------

    private void
    compile(TypeDeclaration td) throws CompileException {

        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }

        TypeDeclarationVisitor tdv = new TypeDeclarationVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd)                  { try { UnitCompiler.this.compile2(acd);                                 } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalClassDeclaration(LocalClassDeclaration lcd)                          { try { UnitCompiler.this.compile2(lcd);                                 } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitPackageMemberClassDeclaration(PackageMemberClassDeclaration pmcd)         { try { UnitCompiler.this.compile2((PackageMemberTypeDeclaration) pmcd); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMemberInterfaceDeclaration(MemberInterfaceDeclaration mid)                { try { UnitCompiler.this.compile2(mid);                                 } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitPackageMemberInterfaceDeclaration(PackageMemberInterfaceDeclaration pmid) { try { UnitCompiler.this.compile2((PackageMemberTypeDeclaration) pmid); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMemberClassDeclaration(MemberClassDeclaration mcd)                        { try { UnitCompiler.this.compile2(mcd);                                 } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            td.accept(tdv);
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }
    public void
    compile2(PackageMemberTypeDeclaration pmtd) throws CompileException {
        CompilationUnit declaringCompilationUnit = pmtd.getDeclaringCompilationUnit();

        // Check for conflict with single-type-import (7.6).
        {
            String[] ss = this.getSingleTypeImport(pmtd.getName(), pmtd.getLocation());
            if (ss != null) {
                this.compileError((
                    "Package member type declaration \""
                    + pmtd.getName()
                    + "\" conflicts with single-type-import \""
                    + Java.join(ss, ".")
                    + "\""
                ), pmtd.getLocation());
            }
        }

        // Check for redefinition within compilation unit (7.6).
        {
            PackageMemberTypeDeclaration otherPmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(
                pmtd.getName()
            );
            if (otherPmtd != pmtd) {
                this.compileError((
                    "Redeclaration of type \""
                    + pmtd.getName()
                    + "\", previously declared in "
                    + otherPmtd.getLocation()
                ), pmtd.getLocation());
            }
        }

        if (pmtd instanceof NamedClassDeclaration) {
            this.compile2((NamedClassDeclaration) pmtd);
        } else
        if (pmtd instanceof InterfaceDeclaration) {
            this.compile2((InterfaceDeclaration) pmtd);
        } else
        {
            throw new JaninoRuntimeException("PMTD of unexpected type " + pmtd.getClass().getName());
        }
    }

    public void
    compile2(ClassDeclaration cd) throws CompileException {
        IClass iClass = this.resolve(cd);

        // Check that all methods are implemented.
        if ((cd.getModifiersAndAnnotations().modifiers & Mod.ABSTRACT) == 0) {
            IMethod[] ms = iClass.getIMethods();
            for (int i = 0; i < ms.length; ++i) {
                IMethod base = ms[i];
                if (base.isAbstract()) {
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

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            (short) (cd.getModifiersAndAnnotations().modifiers | Mod.SUPER), // accessFlags
            iClass.getDescriptor(),                                          // thisClassFD
            iClass.getSuperclass().getDescriptor(),                          // superclassFD
            IClass.getDescriptors(iClass.getInterfaces())                    // interfaceFDs
        );

        // TODO: Add annotations with retention != SOURCE.
//        for (int i = 0; i < cd.getModifiersAndAnnotations().annotations.length; i++) {
//            Annotation a = cd.getModifiersAndAnnotations().annotations[i];
//            assert false : "Class '" + iClass + "' has annotation '" + a + "'";
//        }

        // Add InnerClasses attribute entry for this class declaration.
        if (cd.getEnclosingScope() instanceof CompilationUnit) {
            ;
        } else
        if (cd.getEnclosingScope() instanceof Block) {
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short innerNameIndex      = (
                this instanceof NamedTypeDeclaration
                ? cf.addConstantUtf8Info(((NamedTypeDeclaration) this).getName())
                : (short) 0
            );
            assert cd.getModifiersAndAnnotations().annotations.length == 0 : "NYI";
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,                      // innerClassInfoIndex
                (short) 0,                                // outerClassInfoIndex
                innerNameIndex,                           // innerNameIndex
                cd.getModifiersAndAnnotations().modifiers // innerClassAccessFlags
            ));
        } else
        if (cd.getEnclosingScope() instanceof TypeDeclaration) {
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(
                this.resolve(((TypeDeclaration) cd.getEnclosingScope())).getDescriptor()
            );
            short innerNameIndex = cf.addConstantUtf8Info(((MemberTypeDeclaration) cd).getName());
            assert cd.getModifiersAndAnnotations().annotations.length == 0 : "NYI";
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,                      // innerClassInfoIndex
                outerClassInfoIndex,                      // outerClassInfoIndex
                innerNameIndex,                           // innerNameIndex
                cd.getModifiersAndAnnotations().modifiers // innerClassAccessFlags
            ));
        }

        // Set "SourceFile" attribute.
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

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (cd instanceof DocCommentable) {
            if (((DocCommentable) cd).hasDeprecatedDocTag()) cf.addDeprecatedAttribute();
        }

        // Optional: Generate and compile class initialization method.
        {
            List/*<BlockStatement>*/ statements = new ArrayList();
            for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                if (tbd.isStatic()) statements.add(tbd);
            }

            this.maybeCreateInitMethod(cd, cf, statements);
        }

        this.compileDeclaredMethods(cd, cf);

        // Compile declared constructors.
        // As a side effect of compiling methods and constructors, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly.
        // We need to note how many we have here so we can compile the extras.
        int declaredMethodCount = cd.getMethodDeclarations().size();
        {
            int                     syntheticFieldCount = cd.syntheticFields.size();
            ConstructorDeclarator[] cds                 = cd.getConstructors();
            for (int i = 0; i < cds.length; ++i) {
                this.compile(cds[i], cf);
                if (syntheticFieldCount != cd.syntheticFields.size()) {
                    throw new JaninoRuntimeException(
                        "SNO: Compilation of constructor \""
                        + cds[i]
                        + "\" ("
                        + cds[i].getLocation()
                        + ") added synthetic fields!?"
                    );
                }
            }
        }

        // A side effect of this call may create synthetic functions to access
        // protected parent variables
        this.compileDeclaredMemberTypes(cd, cf);

        // Compile the aforementioned extras.
        this.compileDeclaredMethods(cd, cf, declaredMethodCount);

        {
            // for every method look for bridge methods that need to be supplied
            // this is used to correctly dispatch into covariant return types
            // from existing code
            IMethod[] ms = iClass.getIMethods();
            for (int i = 0; i < ms.length; ++i) {
                IMethod base = ms[i];
                if (!base.isStatic()) {
                    IMethod override = iClass.findIMethod(base.getName(), base.getParameterTypes());

                    // If we overrode the method but with a DIFFERENT return type.
                    if (
                        override != null
                        && !base.getReturnType().equals(override.getReturnType())
                    ) {
                        this.compileBridgeMethod(cf, base, override);
                    }
                }
            }
        }

        // Class and instance variables.
        for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
            if (!(tbd instanceof FieldDeclaration)) continue;
            this.addFields((FieldDeclaration) tbd, cf);
        }

        // Synthetic fields.
        for (Iterator it = cd.syntheticFields.values().iterator(); it.hasNext();) {
            IClass.IField f = (IClass.IField) it.next();
            cf.addFieldInfo(
                Mod.PACKAGE,                 // accessFlags
                f.getName(),                 // fieldName
                f.getType().getDescriptor(), // fieldTypeFD
                null                         // optionalConstantValue
            );
        }


        // Add the generated class file to a thread-local store.
        this.generatedClassFiles.add(cf);
    }

    /**
     * Create {@link ClassFile.FieldInfo}s for all fields declared by the given {@link FieldDeclaration}.
     */
    private void
    addFields(FieldDeclaration fd, ClassFile cf) throws CompileException {
        for (int j = 0; j < fd.variableDeclarators.length; ++j) {
            VariableDeclarator vd   = fd.variableDeclarators[j];
            Type               type = fd.type;
            for (int k = 0; k < vd.brackets; ++k) type = new ArrayType(type);

            Object ocv = NOT_CONSTANT;
            if (
                (fd.modifiersAndAnnotations.modifiers & Mod.FINAL) != 0
                && vd.optionalInitializer instanceof Rvalue
            ) {
                ocv = this.getConstantValue((Rvalue) vd.optionalInitializer);
            }

            ClassFile.FieldInfo fi;
            if (Mod.isPrivateAccess(fd.modifiersAndAnnotations.modifiers)) {

                // To make the private field accessible for enclosing types, enclosed types and types
                // enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                assert fd.modifiersAndAnnotations.annotations.length == 0 : "NYI";
                fi = cf.addFieldInfo(
                    Mod.changeAccess(fd.modifiersAndAnnotations.modifiers, Mod.PACKAGE), // accessFlags
                    vd.name,                                                             // fieldName
                    this.getType(type).getDescriptor(),                                  // fieldTypeFD
                    ocv == NOT_CONSTANT ? null : ocv                                     // optionalConstantValue
                );
            } else
            {
                assert fd.modifiersAndAnnotations.annotations.length == 0 : "NYI";
                fi = cf.addFieldInfo(
                    fd.modifiersAndAnnotations.modifiers, // accessFlags
                    vd.name,                              // fieldName
                    this.getType(type).getDescriptor(),   // fieldTypeFD
                    ocv == NOT_CONSTANT ? null : ocv      // optionalConstantValue
                );
            }

            // Add "Deprecated" attribute (JVMS 4.7.10)
            if (fd.hasDeprecatedDocTag()) {
                fi.addAttribute(new ClassFile.DeprecatedAttribute(cf.addConstantUtf8Info("Deprecated")));
            }
        }
    }

    public void
    compile2(AnonymousClassDeclaration acd) throws CompileException {
        this.compile2((InnerClassDeclaration) acd);
    }

    public void
    compile2(LocalClassDeclaration lcd) throws CompileException {
        this.compile2((InnerClassDeclaration) lcd);
    }

    public void
    compile2(InnerClassDeclaration icd) throws CompileException {

        // Define a synthetic "this$..." field if there is an enclosing instance.
        {
            List      ocs     = UnitCompiler.getOuterClasses(icd);
            final int nesting = ocs.size();
            if (nesting >= 2) {
                icd.defineSyntheticField(new SimpleIField(
                    this.resolve(icd),
                    "this$" + (nesting - 2),
                    this.resolve((TypeDeclaration) ocs.get(1))
                ));
            }
        }

        // For classes that enclose surrounding scopes, trawl their initializers looking for synthetic fields.
        if (icd instanceof AnonymousClassDeclaration || icd instanceof LocalClassDeclaration) {
            ClassDeclaration cd = (ClassDeclaration) icd;
            for (int i = 0; i < cd.variableDeclaratorsAndInitializers.size(); ++i) {
                TypeBodyDeclaration tbd = (TypeBodyDeclaration) cd.variableDeclaratorsAndInitializers.get(i);
                if (tbd instanceof FieldDeclaration) {
                    FieldDeclaration fd = (FieldDeclaration) tbd;
                    for (int j = 0; j < fd.variableDeclarators.length; ++j) {
                        VariableDeclarator vd = fd.variableDeclarators[j];
                        if (vd.optionalInitializer != null) {
                            this.fakeCompile(vd.optionalInitializer);
                        }
                    }
                }
            }
        }

        this.compile2((ClassDeclaration) icd);
    }

    public void
    compile2(final MemberClassDeclaration mcd) throws CompileException {
        this.compile2((InnerClassDeclaration) mcd);
    }

    public void
    compile2(InterfaceDeclaration id) throws CompileException {
        IClass iClass = this.resolve(id);

        // Determine extended interfaces.
        id.interfaces = new IClass[id.extendedTypes.length];
        String[] interfaceDescriptors = new String[id.interfaces.length];
        for (int i = 0; i < id.extendedTypes.length; ++i) {
            id.interfaces[i]        = this.getType(id.extendedTypes[i]);
            interfaceDescriptors[i] = id.interfaces[i].getDescriptor();
        }

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            (short) (               // accessFlags
                id.getModifiersAndAnnotations().modifiers
                | Mod.SUPER
                | Mod.INTERFACE
                | Mod.ABSTRACT
            ),
            iClass.getDescriptor(), // thisClassFD
            Descriptor.JAVA_LANG_OBJECT, // superclassFD
            interfaceDescriptors    // interfaceFDs
        );

        // TODO: Add annotations with retention != SOURCE.
//        for (int i = 0; i < id.getModifiersAndAnnotations().annotations.length; i++) {
//            Annotation a = id.getModifiersAndAnnotations().annotations[i];
//            assert false : "Interface '" + iClass + "' has annotation '" + a + "'";
//        }

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

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (id.hasDeprecatedDocTag()) cf.addDeprecatedAttribute();

        // Interface initialization method.
        if (!id.constantDeclarations.isEmpty()) {
            List statements = new ArrayList(); // BlockStatements
            statements.addAll(id.constantDeclarations);

            maybeCreateInitMethod(id, cf, statements);
        }

        compileDeclaredMethods(id, cf);

        // Class variables.
        for (int i = 0; i < id.constantDeclarations.size(); ++i) {
            BlockStatement bs = (BlockStatement) id.constantDeclarations.get(i);
            if (!(bs instanceof FieldDeclaration)) continue;
            this.addFields((FieldDeclaration) bs, cf);
        }

        compileDeclaredMemberTypes(id, cf);

        // Add the generated class file to a thread-local store.
        this.generatedClassFiles.add(cf);
    }

    /**
     * Create class initialization method iff there is any initialization code.
     *
     * @param decl              The type declaration
     * @param cf                The class file into which to put the method
     * @param b                 The block for the method (possibly empty)
     * @throws CompileException
     */
    private void
    maybeCreateInitMethod(
        AbstractTypeDeclaration  decl,
        ClassFile                cf,
        List/*<BlockStatement>*/ statements
    ) throws CompileException {
        // Create interface initialization method iff there is any initialization code.
        if (this.generatesCode2ListStatements(statements)) {
            MethodDeclarator md = new MethodDeclarator(
                decl.getLocation(),                                                  // location
                null,                                                                // optionalDocComment
                new Java.ModifiersAndAnnotations((short) (Mod.STATIC | Mod.PUBLIC)), // modifiers
                new BasicType(                                                       // type
                    decl.getLocation(),
                    BasicType.VOID
                ),
                "<clinit>",                                                          // name
                new FunctionDeclarator.FormalParameter[0],                           // formalParameters
                new ReferenceType[0],                                                // thrownExceptions
                statements                                                           // optionalStatements
            );
            md.setDeclaringType(decl);
            this.compile(md, cf);
        }
    }

    /**
     * Compile all of the types for this declaration
     * <p>
     * NB: as a side effect this will fill in the synthetic field map
     */
    private void
    compileDeclaredMemberTypes(TypeDeclaration decl, ClassFile cf) throws CompileException {
        for (Iterator it = decl.getMemberTypeDeclarations().iterator(); it.hasNext();) {
            TypeDeclaration td = ((MemberTypeDeclaration) it.next());
            this.compile(td);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(td).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(decl).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(((MemberTypeDeclaration) td).getName());
            assert td.getModifiersAndAnnotations().annotations.length == 0;
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex,                      // innerClassInfoIndex
                outerClassInfoIndex,                      // outerClassInfoIndex
                innerNameIndex,                           // innerNameIndex
                td.getModifiersAndAnnotations().modifiers // innerClassAccessFlags
            ));
        }
    }

    /**
     * Compile all of the methods for this declaration
     * <p>
     * NB: as a side effect this will fill in the synthetic field map
     *
     * @throws CompileException
     */
    private void
    compileDeclaredMethods(AbstractTypeDeclaration typeDeclaration, ClassFile cf) throws CompileException {
        this.compileDeclaredMethods(typeDeclaration, cf, 0);
    }

    /**
     * Compile methods for this declaration starting at {@code startPos}.
     *
     * @param startPos          Starting parameter to fill in
     * @throws CompileException
     */
    private void
    compileDeclaredMethods(TypeDeclaration typeDeclaration, ClassFile cf, int startPos) throws CompileException {

        // Notice that as a side effect of compiling methods, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly. Hence, we
        // must not use an Iterator here.

        for (int i = startPos; i < typeDeclaration.getMethodDeclarations().size(); ++i) {
            MethodDeclarator md = (MethodDeclarator) typeDeclaration.getMethodDeclarations().get(i);

            IMethod m                     = this.toIMethod(md);
            boolean overrides             = overridesMethodFromSupertype(m, this.resolve(md.getDeclaringType()));
            boolean hasOverrideAnnotation = hasAnnotation(md, this.iClassLoader.JAVA_LANG_OVERRIDE);
            if (overrides && !hasOverrideAnnotation && !(typeDeclaration instanceof InterfaceDeclaration)) {
                compileError("Missing @Override", md.getLocation());
            } else
            if (!overrides && hasOverrideAnnotation) {
                compileError("Method does not override a method declared in a supertype", md.getLocation());
            }

            this.compile(md, cf);
        }
    }
    
    private boolean
    hasAnnotation(FunctionDeclarator fd, IClass methodAnnotation) throws CompileException {
        Annotation[] methodAnnotations = fd.modifiersAndAnnotations.annotations;
        for (int i = 0; i < methodAnnotations.length; i++) {
            if (this.getType(methodAnnotations[i].getType()) == methodAnnotation) return true;
        }
        return false;
    }

    private boolean
    overridesMethodFromSupertype(IMethod m, IClass type) throws CompileException {

        // Check whether it overrides a method declared in the superclass (or any of its supertypes).
        {
            IClass superclass = type.getSuperclass();
            if (superclass != null && overridesMethod(m, superclass)) return true;
        }

        // Check whether it overrides a method declared in an interface (or any of its superinterfaces).
        IClass[] ifs = type.getInterfaces();
        for (int i = 0; i < ifs.length; i++) {
            if (overridesMethod(m, ifs[i])) return true;
        }

        return false;
    }

    /**
     * @return Whether {@code m} overrides a method of {@code type} or any of its supertypes
     */
    private boolean
    overridesMethod(IMethod m, IClass type) throws CompileException {

        // Check whether it overrides a method declared in THIS type.
        IMethod[] ms = type.getDeclaredIMethods(m.getName());
        for (int i = 0; i < ms.length; i++) {
            if (Arrays.equals(m.getParameterTypes(), ms[i].getParameterTypes())) return true;
        }

        // Check whether it overrides a method declared in a supertype.
        return this.overridesMethodFromSupertype(m, type);
    }

    /**
     * Compile a bridge method which will add a method of the signature of base that delegates to override.
     */
    private void
    compileBridgeMethod(ClassFile cf, IMethod base, IMethod override) throws CompileException {
        ClassFile.MethodInfo mi = cf.addMethodInfo(
            (short) (Mod.PUBLIC | Mod.SYNTHETIC),
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

        final CodeContext codeContext      = new CodeContext(mi.getClassFile());
        CodeContext       savedCodeContext = this.replaceCodeContext(codeContext);

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

        this.writeOpcode(Located.NOWHERE, Opcode.ALOAD_0);
        for (int i = 0; i < locals.length; ++i) {
            this.load(Located.NOWHERE, locals[i].getType(), locals[i].getSlotIndex());
        }
        this.writeOpcode(Located.NOWHERE, Opcode.INVOKEVIRTUAL);
        this.writeConstantMethodrefInfo(
            override.getDeclaringIClass().getDescriptor(), // classFD
            override.getName(),                            // methodName
            override.getDescriptor()                       // methodMD
        );
        this.writeOpcode(Located.NOWHERE, Opcode.ARETURN);
        this.replaceCodeContext(savedCodeContext);
        codeContext.flowAnalysis(override.getName());

        // Add the code context as a code attribute to the MethodInfo.
        mi.addAttribute(new ClassFile.AttributeInfo(cf.addConstantUtf8Info("Code")) {

            @Override protected void
            storeBody(DataOutputStream dos) throws IOException {
                codeContext.storeCodeAttributeBody(dos, (short) 0, (short) 0);
            }
        });
    }

    /**
     * @return <tt>false</tt> if this statement cannot complete normally (JLS2 14.20)
     */
    private boolean
    compile(BlockStatement bs) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        BlockStatementVisitor bsv = new BlockStatementVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitInitializer(Initializer i)                                                { try { res[0] = UnitCompiler.this.compile2(i);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldDeclaration(FieldDeclaration fd)                                     { try { res[0] = UnitCompiler.this.compile2(fd);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLabeledStatement(LabeledStatement ls)                                     { try { res[0] = UnitCompiler.this.compile2(ls);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBlock(Block b)                                                            { try { res[0] = UnitCompiler.this.compile2(b);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitExpressionStatement(ExpressionStatement es)                               { try { res[0] = UnitCompiler.this.compile2(es);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIfStatement(IfStatement is)                                               { try { res[0] = UnitCompiler.this.compile2(is);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitForStatement(ForStatement fs)                                             { try { res[0] = UnitCompiler.this.compile2(fs);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitWhileStatement(WhileStatement ws)                                         { try { res[0] = UnitCompiler.this.compile2(ws);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitTryStatement(TryStatement ts)                                             { try { res[0] = UnitCompiler.this.compile2(ts);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSwitchStatement(SwitchStatement ss)                                       { try { res[0] = UnitCompiler.this.compile2(ss);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSynchronizedStatement(SynchronizedStatement ss)                           { try { res[0] = UnitCompiler.this.compile2(ss);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitDoStatement(DoStatement ds)                                               { try { res[0] = UnitCompiler.this.compile2(ds);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { try { res[0] = UnitCompiler.this.compile2(lvds); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitReturnStatement(ReturnStatement rs)                                       { try { res[0] = UnitCompiler.this.compile2(rs);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThrowStatement(ThrowStatement ts)                                         { try { res[0] = UnitCompiler.this.compile2(ts);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBreakStatement(BreakStatement bs)                                         { try { res[0] = UnitCompiler.this.compile2(bs);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitContinueStatement(ContinueStatement cs)                                   { try { res[0] = UnitCompiler.this.compile2(cs);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAssertStatement(AssertStatement as)                                       { try { res[0] = UnitCompiler.this.compile2(as);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitEmptyStatement(EmptyStatement es)                                         {       res[0] = UnitCompiler.this.compile2(es);                                                      }
            @Override public void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { try { res[0] = UnitCompiler.this.compile2(lcds); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { try { res[0] = UnitCompiler.this.compile2(aci);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { try { res[0] = UnitCompiler.this.compile2(sci);  } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    private boolean
    compile2(Initializer i) throws CompileException {
        return this.compile(i.block);
    }

    private boolean
    compile2(Block b) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {
            return compileStatements(b.statements);
        } finally {
            this.codeContext.restoreLocalVariables();
        }
    }

    private boolean
    compileStatements(List statements) throws CompileException {
        boolean previousStatementCanCompleteNormally = true;
        for (int i = 0; i < statements.size(); ++i) {
            BlockStatement bs = (BlockStatement) statements.get(i);
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
        if (cvc != NOT_CONSTANT) {
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

        CodeContext.Offset bodyOffset = this.codeContext.newOffset();

        // Compile body.
        ds.whereToContinue = null;
        if (!this.compile(ds.body) && ds.whereToContinue == null) {
            this.warning("DSNTC", "\"do\" statement never tests its condition", ds.getLocation());
            if (ds.whereToBreak == null) return false;
            ds.whereToBreak.set();
            ds.whereToBreak = null;
            return true;
        }
        if (ds.whereToContinue != null) {
            ds.whereToContinue.set();
            ds.whereToContinue = null;
        }

        // Compile condition.
        this.compileBoolean(ds.condition, bodyOffset, Rvalue.JUMP_IF_TRUE);

        if (ds.whereToBreak != null) {
            ds.whereToBreak.set();
            ds.whereToBreak = null;
        }

        return true;
    }

    private boolean
    compile2(ForStatement fs) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {

            // Compile initializer.
            if (fs.optionalInit != null) this.compile(fs.optionalInit);

            if (fs.optionalCondition == null) {
                return this.compileUnconditionalLoop(fs, fs.body, fs.optionalUpdate);
            } else
            {
                Object cvc = this.getConstantValue(fs.optionalCondition);
                if (cvc != NOT_CONSTANT) {
                    if (Boolean.TRUE.equals(cvc)) {
                        this.warning("FSTC", (
                            "Condition of FOR statement is always TRUE; "
                            + "the proper way of declaring an unconditional loop is \"for (;;)\""
                        ), fs.getLocation());
                        return this.compileUnconditionalLoop(fs, fs.body, fs.optionalUpdate);
                    } else
                    {
                        this.warning("FSNR", "FOR statement never repeats", fs.getLocation());
                    }
                }
            }

            CodeContext.Offset toCondition = this.codeContext.new Offset();
            this.writeBranch(fs, Opcode.GOTO, toCondition);

            // Compile body.
            fs.whereToContinue = null;
            CodeContext.Offset bodyOffset = this.codeContext.newOffset();
            boolean            bodyCcn    = this.compile(fs.body);
            if (fs.whereToContinue != null) fs.whereToContinue.set();

            // Compile update.
            if (fs.optionalUpdate != null) {
                if (!bodyCcn && fs.whereToContinue == null) {
                    this.warning("FUUR", "For update is unreachable", fs.getLocation());
                } else
                {
                    for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                        this.compile(fs.optionalUpdate[i]);
                    }
                }
            }
            fs.whereToContinue = null;

            // Compile condition.
            toCondition.set();
            this.compileBoolean(fs.optionalCondition, bodyOffset, Rvalue.JUMP_IF_TRUE);
        } finally {
            this.codeContext.restoreLocalVariables();
        }

        if (fs.whereToBreak != null) {
            fs.whereToBreak.set();
            fs.whereToBreak = null;
        }

        return true;
    }

    private boolean
    compile2(WhileStatement ws) throws CompileException {
        Object cvc = this.getConstantValue(ws.condition);
        if (cvc != NOT_CONSTANT) {
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
        ws.whereToContinue = this.codeContext.new Offset();
        this.writeBranch(ws, Opcode.GOTO, ws.whereToContinue);
        CodeContext.Offset bodyOffset = this.codeContext.newOffset();
        this.compile(ws.body); // Return value (CCN) is ignored.
        ws.whereToContinue.set();
        ws.whereToContinue = null;

        // Compile condition.
        this.compileBoolean(ws.condition, bodyOffset, Rvalue.JUMP_IF_TRUE);

        if (ws.whereToBreak != null) {
            ws.whereToBreak.set();
            ws.whereToBreak = null;
        }
        return true;
    }

    private boolean
    compileUnconditionalLoop(ContinuableStatement cs, BlockStatement body, Rvalue[] optionalUpdate)
    throws CompileException {
        if (optionalUpdate != null) return this.compileUnconditionalLoopWithUpdate(cs, body, optionalUpdate);

        // Compile body.
        cs.whereToContinue = this.codeContext.newOffset();
        if (this.compile(body)) this.writeBranch(cs, Opcode.GOTO, cs.whereToContinue);
        cs.whereToContinue = null;

        if (cs.whereToBreak == null) return false;
        cs.whereToBreak.set();
        cs.whereToBreak = null;
        return true;
    }
    private boolean
    compileUnconditionalLoopWithUpdate(ContinuableStatement cs, BlockStatement body, Rvalue[] update)
    throws CompileException {

        // Compile body.
        cs.whereToContinue = null;
        CodeContext.Offset bodyOffset = this.codeContext.newOffset();
        boolean            bodyCcn    = this.compile(body);

        // Compile the "update".
        if (cs.whereToContinue != null) cs.whereToContinue.set();
        if (!bodyCcn && cs.whereToContinue == null) {
            this.warning("LUUR", "Loop update is unreachable", update[0].getLocation());
        } else
        {
            for (int i = 0; i < update.length; ++i) this.compile(update[i]);
            this.writeBranch(cs, Opcode.GOTO, bodyOffset);
        }
        cs.whereToContinue = null;

        if (cs.whereToBreak == null) return false;
        cs.whereToBreak.set();
        cs.whereToBreak = null;
        return true;
    }

    private boolean
    compile2(LabeledStatement ls) throws CompileException {
        boolean canCompleteNormally = this.compile(ls.body);

        if (ls.whereToBreak == null) return canCompleteNormally;

        ls.whereToBreak.set();
        ls.whereToBreak = null;
        return true;
    }

    private boolean
    compile2(SwitchStatement ss) throws CompileException {

        // Compute condition.
        IClass switchExpressionType = this.compileGetValue(ss.condition);
        this.assignmentConversion(
            ss,                   // locatable
            switchExpressionType, // sourceType
            IClass.INT,           // targetType
            null                  // optionalConstantValue
        );

        // Prepare the map of case labels to code offsets.
        TreeMap              caseLabelMap       = new TreeMap(); // Integer => Offset
        CodeContext.Offset   defaultLabelOffset = null;
        CodeContext.Offset[] sbsgOffsets        = new CodeContext.Offset[ss.sbsgs.size()];
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchStatement.SwitchBlockStatementGroup sbsg = (
                (SwitchStatement.SwitchBlockStatementGroup) ss.sbsgs.get(i)
            );
            sbsgOffsets[i] = this.codeContext.new Offset();
            for (int j = 0; j < sbsg.caseLabels.size(); ++j) {

                // Verify that case label value is a constant.
                Rvalue rv = (Rvalue) sbsg.caseLabels.get(j);
                Object cv = this.getConstantValue(rv);
                if (cv == NOT_CONSTANT) {
                    this.compileError("Value of \"case\" label does not pose a constant value", rv.getLocation());
                    cv = new Integer(99);
                }

                // Verify that case label is assignable to the type of the switch expression.
                IClass rvType = this.getType(rv);
                this.assignmentConversion(
                    ss,                   // locatable
                    rvType,               // sourceType
                    switchExpressionType, // targetType
                    cv                    // optionalConstantValue
                );

                // Convert char, byte, short, int to "Integer".
                Integer civ;
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
                        rv.getLocation()
                    );
                    civ = new Integer(99);
                }

                // Store in case label map.
                if (caseLabelMap.containsKey(civ)) {
                    this.compileError("Duplicate \"case\" switch label value", rv.getLocation());
                }
                caseLabelMap.put(civ, sbsgOffsets[i]);
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
        CodeContext.Offset switchOffset = this.codeContext.newOffset();
        if (caseLabelMap.isEmpty()) {
            // Special case: SWITCH statement without CASE labels (but maybe a DEFAULT label).
            ;
        } else
        if (
            ((Integer) caseLabelMap.firstKey()).intValue() + caseLabelMap.size() // Beware of INT overflow!
            >= ((Integer) caseLabelMap.lastKey()).intValue() - caseLabelMap.size()
        ) {
            int low  = ((Integer) caseLabelMap.firstKey()).intValue();
            int high = ((Integer) caseLabelMap.lastKey()).intValue();

            this.writeOpcode(ss, Opcode.TABLESWITCH);
            new Padder(this.codeContext).set();
            this.writeOffset(switchOffset, defaultLabelOffset);
            this.writeInt(low);
            this.writeInt(high);
            Iterator si  = caseLabelMap.entrySet().iterator();
            int      cur = low;
            while (si.hasNext()) {
                Map.Entry me  = (Map.Entry) si.next();
                int       val = ((Integer) me.getKey()).intValue();
                while (cur < val) {
                    this.writeOffset(switchOffset, defaultLabelOffset);
                    ++cur;
                }
                this.writeOffset(switchOffset, (CodeContext.Offset) me.getValue());
                ++cur;
            }
        } else {
            this.writeOpcode(ss, Opcode.LOOKUPSWITCH);
            new Padder(this.codeContext).set();
            this.writeOffset(switchOffset, defaultLabelOffset);
            this.writeInt(caseLabelMap.size());
            Iterator si = caseLabelMap.entrySet().iterator();
            while (si.hasNext()) {
                Map.Entry me = (Map.Entry) si.next();
                this.writeInt(((Integer) me.getKey()).intValue());
                this.writeOffset(switchOffset, (CodeContext.Offset) me.getValue());
            }
        }

        // Compile statement groups.
        boolean canCompleteNormally = true;
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            SwitchStatement.SwitchBlockStatementGroup sbsg = (
                (SwitchStatement.SwitchBlockStatementGroup) ss.sbsgs.get(i)
            );
            sbsgOffsets[i].set();
            canCompleteNormally = true;
            for (int j = 0; j < sbsg.blockStatements.size(); ++j) {
                BlockStatement bs = (BlockStatement) sbsg.blockStatements.get(j);
                if (!canCompleteNormally) {
                    this.compileError("Statement is unreachable", bs.getLocation());
                    break;
                }
                canCompleteNormally = this.compile(bs);
            }
        }
        if (ss.whereToBreak == null) return canCompleteNormally;

        ss.whereToBreak.set();
        ss.whereToBreak = null;
        return true;
    }

    private boolean
    compile2(BreakStatement bs) throws CompileException {

        // Find the broken statement.
        BreakableStatement brokenStatement = null;
        if (bs.optionalLabel == null) {
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
                    if (ls.label.equals(bs.optionalLabel)) {
                        brokenStatement = ls;
                        break;
                    }
                }
            }
            if (brokenStatement == null) {
                this.compileError((
                    "Statement \"break "
                    + bs.optionalLabel
                    + "\" is not enclosed by a breakable statement with label \""
                    + bs.optionalLabel
                    + "\""
                ), bs.getLocation());
                return false;
            }
        }

        this.leaveStatements(
            bs.getEnclosingScope(),              // from
            brokenStatement.getEnclosingScope(), // to
            null                                 // optionalStackValueType
        );
        this.writeBranch(bs, Opcode.GOTO, this.getWhereToBreak(brokenStatement));
        return false;
    }

    private boolean
    compile2(ContinueStatement cs) throws CompileException {

        // Find the continued statement.
        ContinuableStatement continuedStatement = null;
        if (cs.optionalLabel == null) {
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
                    if (ls.label.equals(cs.optionalLabel)) {
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
                    + cs.optionalLabel
                    + "\" is not enclosed by a continuable statement with label \""
                    + cs.optionalLabel
                    + "\""
                ), cs.getLocation());
                return false;
            }
        }

        if (continuedStatement.whereToContinue == null) {
            continuedStatement.whereToContinue = this.codeContext.new Offset();
        }

        this.leaveStatements(
            cs.getEnclosingScope(),                 // from
            continuedStatement.getEnclosingScope(), // to
            null                                    // optionalStackValueType
        );
        this.writeBranch(cs, Opcode.GOTO, continuedStatement.whereToContinue);
        return false;
    }

    private boolean
    compile2(AssertStatement as) throws CompileException {

        // assert expression1;
        //   if (!expression1) throw new AssertionError();
        // assert expression1 : expression2;
        //   if (!expression1) throw new AssertionError(expression2);
        CodeContext.Offset end = this.codeContext.new Offset();
        try {
            this.compileBoolean(as.expression1, end, Rvalue.JUMP_IF_TRUE);

            this.writeOpcode(as, Opcode.NEW);
            this.writeConstantClassInfo(Descriptor.JAVA_LANG_ASSERTIONERROR);
            this.writeOpcode(as, Opcode.DUP);

            Java.Rvalue[] arguments = (
                as.optionalExpression2 == null
                ? new Java.Rvalue[0]
                : new Java.Rvalue[] { as.optionalExpression2 }
            );
            this.invokeConstructor(
                as,                                         // locatable
                as,                                         // scope
                null,                                       // optionalEnclosingInstance
                this.iClassLoader.JAVA_LANG_ASSERTIONERROR, // targetClass
                arguments                                   // arguments
            );
            this.writeOpcode(as, Opcode.ATHROW);
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
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            VariableDeclarator vd = fd.variableDeclarators[i];

            ArrayInitializerOrRvalue initializer = this.getNonConstantFinalInitializer(fd, vd);
            if (initializer == null) continue;

            assert fd.modifiersAndAnnotations.annotations.length == 0;
            if ((fd.modifiersAndAnnotations.modifiers & Mod.STATIC) == 0) {
                this.writeOpcode(fd, Opcode.ALOAD_0);
            }
            IClass fieldType = this.getType(fd.type);
            if (initializer instanceof Rvalue) {
                Rvalue rvalue          = (Rvalue) initializer;
                IClass initializerType = this.compileGetValue(rvalue);
                fieldType = fieldType.getArrayIClass(vd.brackets, this.iClassLoader.JAVA_LANG_OBJECT);
                this.assignmentConversion(
                    fd,                           // locatable
                    initializerType,              // sourceType
                    fieldType,                    // targetType
                    this.getConstantValue(rvalue) // optionalConstantValue
                );
            } else
            if (initializer instanceof ArrayInitializer) {
                this.compileGetValue((ArrayInitializer) initializer, fieldType);
            } else
            {
                throw new JaninoRuntimeException(
                    "Unexpected array initializer or rvalue class "
                    + initializer.getClass().getName()
                );
            }

            // No need to check accessibility here.
            ;

            assert fd.modifiersAndAnnotations.annotations.length == 0;
            if ((fd.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0) {
                this.writeOpcode(fd, Opcode.PUTSTATIC);
            } else {
                this.writeOpcode(fd, Opcode.PUTFIELD);
            }
            this.writeConstantFieldrefInfo(
                this.resolve(fd.getDeclaringType()).getDescriptor(), // classFD
                vd.name,                                             // fieldName
                fieldType.getDescriptor()                            // fieldFD
            );
        }
        return true;
    }

    private boolean
    compile2(IfStatement is) throws CompileException {
        Object         cv = this.getConstantValue(is.condition);
        BlockStatement es = (
            is.optionalElseStatement != null
            ? is.optionalElseStatement
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
            CodeContext.Inserter ins   = this.codeContext.newInserter();
            boolean              ssccn = this.compile(seeingStatement);
            if (ssccn) return true;

            // Hm... the "seeing statement" cannot complete normally. Things are getting
            // complicated here! The robust solution is to compile the constant-condition-IF
            // statement as a non-constant-condition-IF statement. As an optimization, iff the
            // IF-statement is enclosed ONLY by blocks, then the remaining bytecode can be
            // written to a "fake" code context, i.e. be thrown away.

            // Constant-condition-IF statement only enclosed by blocks?
            Scope s = is.getEnclosingScope();
            while (s instanceof Block) s = s.getEnclosingScope();
            if (s instanceof FunctionDeclarator) {

                // Yes, compile rest of method to /dev/null.
                throw UnitCompiler.STOP_COMPILING_CODE;
            } else
            {

                // Compile constant-condition-IF statement as non-constant-condition-IF statement.
                CodeContext.Offset off = this.codeContext.newOffset();
                this.codeContext.pushInserter(ins);
                try {
                    this.pushConstant(is, Boolean.FALSE);
                    this.writeBranch(is, Opcode.IFNE, off);
                } finally {
                    this.codeContext.popInserter();
                }
            }
            return this.compile(blindStatement);
        }

        // Non-constant condition.
        if (this.generatesCode(is.thenStatement)) {
            if (this.generatesCode(es)) {

                // if (expression) statement else statement
                CodeContext.Offset eso = this.codeContext.new Offset();
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, eso, Rvalue.JUMP_IF_FALSE);
                boolean tsccn = this.compile(is.thenStatement);
                if (tsccn) this.writeBranch(is, Opcode.GOTO, end);
                eso.set();
                boolean esccn = this.compile(es);
                end.set();
                return tsccn || esccn;
            } else {

                // if (expression) statement else ;
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, end, Rvalue.JUMP_IF_FALSE);
                this.compile(is.thenStatement);
                end.set();
                return true;
            }
        } else {
            if (this.generatesCode(es)) {

                // if (expression) ; else statement
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, end, Rvalue.JUMP_IF_TRUE);
                this.compile(es);
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
    private static final RuntimeException STOP_COMPILING_CODE = new RuntimeException(
        "SNO: This exception should have been caught and processed"
    );

    private boolean
    compile2(LocalClassDeclarationStatement lcds) throws CompileException {

        // Check for redefinition.
        LocalClassDeclaration otherLcd = findLocalClassDeclaration(lcds, lcds.lcd.name);
        if (otherLcd != lcds.lcd) {
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
     * Find a local class declared in any block enclosing the given block statement.
     */
    private static LocalClassDeclaration
    findLocalClassDeclaration(Scope s, String name) {
        for (;;) {
            Scope es = s.getEnclosingScope();
            if (es instanceof CompilationUnit) break;
            if (
                s instanceof BlockStatement
                && (es instanceof Block || es instanceof FunctionDeclarator)
            ) {
                BlockStatement bs         = (BlockStatement) s;
                List           statements = (
                    es instanceof BlockStatement
                    ? ((Block) es).statements
                    : ((FunctionDeclarator) es).optionalStatements
                );
                for (Iterator it = statements.iterator(); it.hasNext();) {
                    BlockStatement bs2 = (BlockStatement) it.next();
                    if (bs2 instanceof LocalClassDeclarationStatement) {
                        LocalClassDeclarationStatement lcds = ((LocalClassDeclarationStatement) bs2);
                        if (lcds.lcd.name.equals(name)) return lcds.lcd;
                    }
                    if (bs2 == bs) break;
                }
            }
            s = es;
        }
        return null;
    }

    private boolean
    compile2(LocalVariableDeclarationStatement lvds) throws CompileException {
        assert lvds.modifiersAndAnnotations.annotations.length == 0;
        if ((lvds.modifiersAndAnnotations.modifiers & ~Mod.FINAL) != 0) {
            this.compileError(
                "The only allowed modifier in local variable declarations is \"final\"",
                lvds.getLocation()
            );
        }

        for (int j = 0; j < lvds.variableDeclarators.length; ++j) {
            VariableDeclarator vd = lvds.variableDeclarators[j];

            LocalVariable lv = this.getLocalVariable(lvds, vd);
            lv.setSlot(
                this.codeContext.allocateLocalVariable(Descriptor.size(lv.type.getDescriptor()), vd.name, lv.type)
            );

            if (vd.optionalInitializer != null) {
                if (vd.optionalInitializer instanceof Rvalue) {
                    Rvalue rhs = (Rvalue) vd.optionalInitializer;
                    this.assignmentConversion(
                        lvds,                      // locatable
                        this.compileGetValue(rhs), // sourceType
                        lv.type,                   // targetType
                        this.getConstantValue(rhs) // optionalConstantValue
                    );
                } else
                if (vd.optionalInitializer instanceof ArrayInitializer) {
                    this.compileGetValue((ArrayInitializer) vd.optionalInitializer, lv.type);
                } else
                {
                    throw new JaninoRuntimeException(
                        "Unexpected rvalue or array initialized class "
                        + vd.optionalInitializer.getClass().getName()
                    );
                }
                this.store(lvds, lv.type, lv);
            }
        }
        return true;
    }

    public LocalVariable
    getLocalVariable(LocalVariableDeclarationStatement lvds, VariableDeclarator vd) throws CompileException {
        if (vd.localVariable == null) {

            // Determine variable type.
            Type variableType = lvds.type;
            for (int k = 0; k < vd.brackets; ++k) variableType = new ArrayType(variableType);

            assert lvds.modifiersAndAnnotations.annotations.length == 0;
            vd.localVariable = new LocalVariable(
                (lvds.modifiersAndAnnotations.modifiers & Mod.FINAL) != 0, // finaL
                this.getType(variableType)                                 // type
            );
        }
        return vd.localVariable;
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

        IClass returnType = this.getReturnType(enclosingFunction);
        if (returnType == IClass.VOID) {
            if (rs.optionalReturnValue != null) this.compileError("Method must not return a value", rs.getLocation());
            this.leaveStatements(
                rs.getEnclosingScope(), // from
                enclosingFunction,      // to
                null                    // optionalStackValueType
            );
            this.writeOpcode(rs, Opcode.RETURN);
            return false;
        }
        if (rs.optionalReturnValue == null) {
            this.compileError("Method must return a value", rs.getLocation());
            return false;
        }
        IClass type = this.compileGetValue(rs.optionalReturnValue);
        this.assignmentConversion(
            rs,                                           // locatable
            type,                                         // sourceType
            returnType,                                   // targetType
            this.getConstantValue(rs.optionalReturnValue) // optionalConstantValue
        );

        this.leaveStatements(
            rs.getEnclosingScope(), // from
            enclosingFunction,      // to
            returnType              // optionalStackValueType
        );
        this.writeOpcode(rs, Opcode.IRETURN + ilfda(returnType));
        return false;
    }

    private boolean
    compile2(SynchronizedStatement ss) throws CompileException {

        // Evaluate monitor object expression.
        if (!this.iClassLoader.JAVA_LANG_OBJECT.isAssignableFrom(this.compileGetValue(ss.expression))) {
            this.compileError(
                "Monitor object of \"synchronized\" statement is not a subclass of \"Object\"",
                ss.getLocation()
            );
        }

        this.codeContext.saveLocalVariables();
        boolean canCompleteNormally = false;
        try {

            // Allocate a local variable for the monitor object.
            ss.monitorLvIndex = this.codeContext.allocateLocalVariable((short) 1);

            // Store the monitor object.
            this.writeOpcode(ss, Opcode.DUP);
            this.store(ss, this.iClassLoader.JAVA_LANG_OBJECT, ss.monitorLvIndex);

            // Create lock on the monitor object.
            this.writeOpcode(ss, Opcode.MONITORENTER);

            // Compile the statement body.
            CodeContext.Offset monitorExitOffset = this.codeContext.new Offset();
            CodeContext.Offset beginningOfBody   = this.codeContext.newOffset();
            canCompleteNormally = this.compile(ss.body);
            if (canCompleteNormally) {
                this.writeBranch(ss, Opcode.GOTO, monitorExitOffset);
            }

            // Generate the exception handler.
            CodeContext.Offset here = this.codeContext.newOffset();
            this.codeContext.addExceptionTableEntry(
                beginningOfBody, // startPC
                here,            // endPC
                here,            // handlerPC
                null             // catchTypeFD
            );
            this.leave(ss, this.iClassLoader.JAVA_LANG_THROWABLE);
            this.writeOpcode(ss, Opcode.ATHROW);

            // Unlock monitor object.
            if (canCompleteNormally) {
                monitorExitOffset.set();
                this.leave(ss, null);
            }
        } finally {
            this.codeContext.restoreLocalVariables();
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
        this.writeOpcode(ts, Opcode.ATHROW);
        return false;
    }

    private boolean
    compile2(TryStatement ts) throws CompileException {
        if (ts.optionalFinally != null) ts.finallyOffset = this.codeContext.new Offset();

        CodeContext.Offset beginningOfBody = this.codeContext.newOffset();
        CodeContext.Offset afterStatement  = this.codeContext.new Offset();

        this.codeContext.saveLocalVariables();
        try {

            // Allocate a LV for the JSR of the FINALLY clause.
            //
            // Notice:
            //   For unclear reasons, this variable must not overlap with any of the body's
            //   variables (although the body's variables are out of scope when it comes to the
            //   FINALLY clause!?), otherwise you get
            //     java.lang.VerifyError: ... Accessing value from uninitialized local variable 4
            //   See bug #56.
            short pcLvIndex = (
                ts.optionalFinally != null
                ? this.codeContext.allocateLocalVariable((short) 1)
                : (short) 0
            );

            // Initialize all catch clauses as "unreachable" only to check later that they ARE indeed reachable.
            for (int i = 0; i < ts.catchClauses.size(); ++i) {
                CatchClause cc                  = (CatchClause) ts.catchClauses.get(i);
                IClass      caughtExceptionType = this.getType(cc.caughtException.type);
                cc.reachable = (
                    // Superclass or subclass of "java.lang.Error"?
                    this.iClassLoader.JAVA_LANG_ERROR.isAssignableFrom(caughtExceptionType)
                    || caughtExceptionType.isAssignableFrom(this.iClassLoader.JAVA_LANG_ERROR)
                    // Superclass or subclass of "java.lang.RuntimeException"?
                    || this.iClassLoader.JAVA_LANG_RUNTIMEEXCEPTION.isAssignableFrom(caughtExceptionType)
                    || caughtExceptionType.isAssignableFrom(this.iClassLoader.JAVA_LANG_RUNTIMEEXCEPTION)
                );
            }

            boolean            canCompleteNormally = this.compile(ts.body);
            CodeContext.Offset afterBody           = this.codeContext.newOffset();
            if (canCompleteNormally) {
                this.writeBranch(ts, Opcode.GOTO, afterStatement);
            }

            if (beginningOfBody.offset != afterBody.offset) { // Avoid zero-length exception table entries.
                this.codeContext.saveLocalVariables();
                try {
                    for (int i = 0; i < ts.catchClauses.size(); ++i) {
                        try {
                            this.codeContext.saveLocalVariables();

                            CatchClause cc                  = (CatchClause) ts.catchClauses.get(i);
                            IClass      caughtExceptionType = this.getType(cc.caughtException.type);

                            // Verify that the CATCH clause is reachable.
                            if (!cc.reachable) {
                                this.compileError("Catch clause is unreachable", cc.getLocation());
                            }

                            // Allocate the "exception variable".
                            LocalVariableSlot exceptionVarSlot = this.codeContext.allocateLocalVariable(
                                (short) 1,
                                cc.caughtException.name,
                                caughtExceptionType
                            );
                            short evi = exceptionVarSlot.getSlotIndex();
                            // Kludge: Treat the exception variable like a local
                            // variable of the catch clause body.
                            this.getLocalVariable(cc.caughtException).setSlot(exceptionVarSlot);

                            this.codeContext.addExceptionTableEntry(
                                beginningOfBody,                    // startPC
                                afterBody,                          // endPC
                                this.codeContext.newOffset(),       // handlerPC
                                caughtExceptionType.getDescriptor() // catchTypeFD
                            );
                            this.store(
                                cc,                  // locatable
                                caughtExceptionType, // lvType
                                evi                  // lvIndex
                            );


                            if (this.compile(cc.body)) {
                                canCompleteNormally = true;
                                if (
                                    i < ts.catchClauses.size() - 1
                                    || ts.optionalFinally != null
                                ) this.writeBranch(cc, Opcode.GOTO, afterStatement);
                            }
                        } finally {
                            this.codeContext.restoreLocalVariables();
                        }
                    }
                } finally {
                    this.codeContext.restoreLocalVariables();
                }
            }

            if (ts.optionalFinally != null) {
                CodeContext.Offset here = this.codeContext.newOffset();
                this.codeContext.addExceptionTableEntry(
                    beginningOfBody, // startPC
                    here,            // endPC
                    here,            // handlerPC
                    null             // catchTypeFD
                );

                this.codeContext.saveLocalVariables();
                try {

                    // Save the exception object in an anonymous local variable.
                    short evi = this.codeContext.allocateLocalVariable((short) 1);
                    this.store(
                        ts.optionalFinally,                 // locatable
                        this.iClassLoader.JAVA_LANG_OBJECT, // lvType
                        evi                                 // lvIndex
                    );
                    this.writeBranch(ts.optionalFinally, Opcode.JSR, ts.finallyOffset);
                    this.load(
                        ts.optionalFinally,                 // locatable
                        this.iClassLoader.JAVA_LANG_OBJECT, // type
                        evi                                 // index
                    );
                    this.writeOpcode(ts.optionalFinally, Opcode.ATHROW);

                    // Compile the "finally" body.
                    ts.finallyOffset.set();
                    this.store(
                        ts.optionalFinally,                 // locatable
                        this.iClassLoader.JAVA_LANG_OBJECT, // lvType
                        pcLvIndex                           // lvIndex
                    );
                    if (this.compile(ts.optionalFinally)) {
                        if (pcLvIndex > 255) {
                            this.writeOpcode(ts.optionalFinally, Opcode.WIDE);
                            this.writeOpcode(ts.optionalFinally, Opcode.RET);
                            this.writeShort(pcLvIndex);
                        } else {
                            this.writeOpcode(ts.optionalFinally, Opcode.RET);
                            this.writeByte(pcLvIndex);
                        }
                    }
                } finally {

                    // The exception object local variable allocated above MUST NOT BE RELEASED
                    // until after the FINALLY block is compiled, for otherwise you get
                    //   java.lang.VerifyError: ... Accessing value from uninitialized register 7
                    this.codeContext.restoreLocalVariables();
                }
            }

            afterStatement.set();
            if (canCompleteNormally) this.leave(ts, null);
            return canCompleteNormally;
        } finally {
            this.codeContext.restoreLocalVariables();
        }
    }

    // ------------ FunctionDeclarator.compile() -------------

    private void
    compile(FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        ClassFile.MethodInfo mi;

        if (Mod.isPrivateAccess(fd.modifiersAndAnnotations.modifiers)) {
            if (fd instanceof MethodDeclarator && !fd.isStatic()) {

                // To make the non-static private method invocable for enclosing types, enclosed types
                // and types enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                //  + The name is appended with "$"
                //  + It is made static
                //  + A parameter of type "declaring class" is prepended to the signature
                short modifiers = Mod.changeAccess(
                    fd.modifiersAndAnnotations.modifiers, // modifiers
                    Mod.PACKAGE                           // newAccess
                );
                modifiers |= Mod.STATIC;

                mi = classFile.addMethodInfo(
                    modifiers,                         // accessFlags
                    fd.name + '$',                     // methodName
                    MethodDescriptor.prependParameter( // methodMD
                        this.toIMethod((MethodDeclarator) fd).getDescriptor(), // md
                        this.resolve(fd.getDeclaringType()).getDescriptor()    // parameterFD
                    )
                );
            } else
            {

                // To make the static private method or private constructor invocable for enclosing types, enclosed
                // types and types enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                assert fd.modifiersAndAnnotations.annotations.length == 0 : "NYI";
                short modifiers = Mod.changeAccess(fd.modifiersAndAnnotations.modifiers, Mod.PACKAGE);
                mi = classFile.addMethodInfo(
                    modifiers,                            // accessFlags
                    fd.name,                              // methodName
                    this.toIInvocable(fd).getDescriptor() // methodMD
                );
            }
        } else
        {
            
            mi = classFile.addMethodInfo(
                fd.modifiersAndAnnotations.modifiers, // accessFlags
                fd.name,                              // methodName
                this.toIInvocable(fd).getDescriptor() // methodMD
            );
        }

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

        if ((fd.modifiersAndAnnotations.modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) return;

        // Create CodeContext.
        final CodeContext codeContext = new CodeContext(mi.getClassFile());

        CodeContext savedCodeContext = this.replaceCodeContext(codeContext);
        try {
            this.codeContext.saveLocalVariables();

            // Define special parameter "this".
            if ((fd.modifiersAndAnnotations.modifiers & Mod.STATIC) == 0) {
                this.codeContext.allocateLocalVariable((short) 1, "this", this.resolve(fd.getDeclaringType()));
            }

            if (fd instanceof ConstructorDeclarator) {
                ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) fd;

                // Reserve space for synthetic parameters ("this$...", "val$...").
                for (
                    Iterator it = constructorDeclarator.getDeclaringClass().syntheticFields.values().iterator();
                    it.hasNext();
                ) {
                    IClass.IField sf = (IClass.IField) it.next();
                    LocalVariable lv = new LocalVariable(true, sf.getType());

                    lv.setSlot(this.codeContext.allocateLocalVariable(Descriptor.size(sf.getDescriptor()), null, null));
                    constructorDeclarator.syntheticParameters.put(sf.getName(), lv);
                }
            }

            this.buildLocalVariableMap(fd);

            // Compile the constructor preamble.
            if (fd instanceof ConstructorDeclarator) {
                ConstructorDeclarator cd = (ConstructorDeclarator) fd;
                if (cd.optionalConstructorInvocation != null) {
                    this.compile(cd.optionalConstructorInvocation);
                    if (cd.optionalConstructorInvocation instanceof SuperConstructorInvocation) {
                        this.assignSyntheticParametersToSyntheticFields(cd);
                        this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                    }
                } else {

                    // Determine qualification for superconstructor invocation.
                    IClass outerClassOfSuperclass = this.resolve(
                        cd.getDeclaringClass()
                    ).getSuperclass().getOuterIClass();
                    QualifiedThisReference qualification = null;
                    if (outerClassOfSuperclass != null) {
                        qualification = new QualifiedThisReference(
                            cd.getLocation(),                                        // location
                            new SimpleType(cd.getLocation(), outerClassOfSuperclass) // qualification
                        );
                    }

                    // Invoke the superconstructor.
                    SuperConstructorInvocation sci = new SuperConstructorInvocation(
                        cd.getLocation(),  // location
                        qualification,     // optionalQualification
                        new Rvalue[0]      // arguments
                    );
                    sci.setEnclosingScope(fd);
                    this.compile(sci);
                    this.assignSyntheticParametersToSyntheticFields(cd);
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                }
            }

            // Compile the function body.
            try {
                if (fd.optionalStatements == null) {
                    this.compileError("Method must have a body", fd.getLocation());
                    return;
                }
                if (this.compileStatements(fd.optionalStatements)) {
                    if (this.getReturnType(fd) != IClass.VOID) {
                        this.compileError("Method must return a value", fd.getLocation());
                    }
                    this.writeOpcode(fd, Opcode.RETURN);
                }
            } catch (RuntimeException ex) {
                if (ex != UnitCompiler.STOP_COMPILING_CODE) throw ex;

                // In very special circumstances (e.g. "if (true) return;"), code generation is
                // terminated abruptly by throwing STOP_COMPILING_CODE.
                ;
            }
        } finally {
            this.codeContext.restoreLocalVariables();
            this.replaceCodeContext(savedCodeContext);
        }

        // Don't continue code attribute generation if we had compile errors.
        if (this.compileErrorCount > 0) return;

        // Fix up and reallocate as needed.
        codeContext.fixUpAndRelocate();

        // Do flow analysis.
        if (UnitCompiler.DEBUG) {
            try {
                codeContext.flowAnalysis(fd.toString());
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                ;
            }
        } else {
            codeContext.flowAnalysis(fd.toString());
        }

        final short lntani;
        if (this.debugLines) {
            lntani = classFile.addConstantUtf8Info("LineNumberTable");
        } else {
            lntani = 0;
        }

        final short lvtani;
        if (this.debugVars) {
            makeLocalVariableNames(codeContext, mi);
            lvtani = classFile.addConstantUtf8Info("LocalVariableTable");
        } else {
            lvtani = 0;
        }

        // Add the code context as a code attribute to the MethodInfo.
        mi.addAttribute(new ClassFile.AttributeInfo(classFile.addConstantUtf8Info("Code")) {

            @Override protected void
            storeBody(DataOutputStream dos) throws IOException {
                codeContext.storeCodeAttributeBody(dos, lntani, lvtani);
            }
        });
    }

    /**
     * Make the variable name and class name Constant Pool names used by local variables.
     */
    private static void
    makeLocalVariableNames(final CodeContext cc, final ClassFile.MethodInfo mi) {
        ClassFile       cf   = mi.getClassFile();
        Iterator        iter = cc.getAllLocalVars().iterator();

        cf.addConstantUtf8Info("LocalVariableTable");

        while (iter.hasNext()) {
            LocalVariableSlot slot = (LocalVariableSlot) iter.next();

            if (slot.getName() != null) {
                String typeName = slot.getType().getDescriptor();

                cf.addConstantUtf8Info(typeName);
                cf.addConstantUtf8Info(slot.getName());
            }
        }
    }

    private void
    buildLocalVariableMap(FunctionDeclarator fd) throws CompileException {
        Map localVars = new HashMap();

        // Add function parameters.
        for (int i = 0; i < fd.formalParameters.length; ++i) {
            FunctionDeclarator.FormalParameter fp = fd.formalParameters[i];
            LocalVariable                      lv = this.getLocalVariable(fp);
            lv.setSlot(this.codeContext.allocateLocalVariable(
                Descriptor.size(lv.type.getDescriptor()),
                fp.name,
                this.getType(fp.type)
            ));

            if (localVars.put(fp.name, lv) != null) {
                this.compileError("Redefinition of parameter \"" + fp.name + "\"", fd.getLocation());
            }
        }

        fd.localVariables = localVars;
        if (fd instanceof ConstructorDeclarator) {
            ConstructorDeclarator cd = (ConstructorDeclarator) fd;
            if (cd.optionalConstructorInvocation != null) {
                UnitCompiler.buildLocalVariableMap(cd.optionalConstructorInvocation, localVars);
            }
        }
        if (fd.optionalStatements != null) {
            for (Iterator it = fd.optionalStatements.iterator(); it.hasNext();) {
                BlockStatement bs = (BlockStatement) it.next();
                localVars = this.buildLocalVariableMap(bs, localVars);
            }
        }
    }

    private Map
    buildLocalVariableMap(BlockStatement bs, final Map localVars) throws CompileException {
        final Map[] resVars = new Map[] { localVars };
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        BlockStatementVisitor bsv = new BlockStatementVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF

            // basic statements that use the default handlers
            @Override public void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)  { UnitCompiler.buildLocalVariableMap(aci, localVars); }
            @Override public void visitBreakStatement(BreakStatement bs)                                   { UnitCompiler.buildLocalVariableMap(bs, localVars); }
            @Override public void visitContinueStatement(ContinueStatement cs)                             { UnitCompiler.buildLocalVariableMap(cs, localVars); }
            @Override public void visitAssertStatement(AssertStatement as)                                 { UnitCompiler.buildLocalVariableMap(as, localVars); }
            @Override public void visitEmptyStatement(EmptyStatement es)                                   { UnitCompiler.buildLocalVariableMap(es, localVars); }
            @Override public void visitExpressionStatement(ExpressionStatement es)                         { UnitCompiler.buildLocalVariableMap(es, localVars); }
            @Override public void visitFieldDeclaration(FieldDeclaration fd)                               { UnitCompiler.buildLocalVariableMap(fd, localVars); }
            @Override public void visitReturnStatement(ReturnStatement rs)                                 { UnitCompiler.buildLocalVariableMap(rs, localVars); }
            @Override public void visitSuperConstructorInvocation(SuperConstructorInvocation sci)          { UnitCompiler.buildLocalVariableMap(sci, localVars); }
            @Override public void visitThrowStatement(ThrowStatement ts)                                   { UnitCompiler.buildLocalVariableMap(ts, localVars); }
            @Override public void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds) { UnitCompiler.buildLocalVariableMap(lcds, localVars); }

            // more complicated statements with specialized handlers, but don't add new variables in this scope
            @Override public void visitBlock(Block b)                                  { try { UnitCompiler.this.buildLocalVariableMap(b,  localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitDoStatement(DoStatement ds)                     { try { UnitCompiler.this.buildLocalVariableMap(ds, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitForStatement(ForStatement fs)                   { try { UnitCompiler.this.buildLocalVariableMap(fs, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIfStatement(IfStatement is)                     { try { UnitCompiler.this.buildLocalVariableMap(is, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInitializer(Initializer i)                      { try { UnitCompiler.this.buildLocalVariableMap(i,  localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSwitchStatement(SwitchStatement ss)             { try { UnitCompiler.this.buildLocalVariableMap(ss, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSynchronizedStatement(SynchronizedStatement ss) { try { UnitCompiler.this.buildLocalVariableMap(ss, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitTryStatement(TryStatement ts)                   { try { UnitCompiler.this.buildLocalVariableMap(ts, localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitWhileStatement(WhileStatement ws)               { try { UnitCompiler.this.buildLocalVariableMap(ws, localVars); } catch (CompileException e) { throw new UCE(e); } }

            // more complicated statements with specialized handlers, that can add variables in this scope
            @Override public void visitLabeledStatement(LabeledStatement ls)                                     { try { resVars[0] = UnitCompiler.this.buildLocalVariableMap(ls,   localVars); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { try { resVars[0] = UnitCompiler.this.buildLocalVariableMap(lvds, localVars); } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try { bs.accept(bsv); } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
        return resVars[0];
    }

    // default handlers

    private static Map
    buildLocalVariableMap(Statement s, final Map localVars) { return (s.localVariables = localVars); }

    private static Map
    buildLocalVariableMap(ConstructorInvocation ci, final Map localVars) { return (ci.localVariables = localVars); }

    // specialized handlers
    private void
    buildLocalVariableMap(Block block, Map localVars) throws CompileException {
        block.localVariables = localVars;
        for (Iterator it = block.statements.iterator(); it.hasNext();) {
            BlockStatement bs = (BlockStatement) it.next();
            localVars = this.buildLocalVariableMap(bs, localVars);
        }
    }

    private void
    buildLocalVariableMap(DoStatement ds, final Map localVars) throws CompileException {
        ds.localVariables = localVars;
        this.buildLocalVariableMap(ds.body, localVars);
    }

    private void
    buildLocalVariableMap(ForStatement fs, final Map localVars) throws CompileException {
        Map inner = localVars;
        if (fs.optionalInit != null) {
            inner = this.buildLocalVariableMap(fs.optionalInit, localVars);
        }
        fs.localVariables = inner;
        this.buildLocalVariableMap(fs.body, inner);
    }

    private void
    buildLocalVariableMap(IfStatement is, final Map localVars) throws CompileException {
        is.localVariables = localVars;
        this.buildLocalVariableMap(is.thenStatement, localVars);
        if (is.optionalElseStatement != null) {
            this.buildLocalVariableMap(is.optionalElseStatement, localVars);
        }
    }

    private void
    buildLocalVariableMap(Initializer i, final Map localVars) throws CompileException {
        this.buildLocalVariableMap(i.block, localVars);
    }

    private void
    buildLocalVariableMap(SwitchStatement ss, final Map localVars) throws CompileException {
        ss.localVariables = localVars;
        Map vars = localVars;
        for (Iterator cases = ss.sbsgs.iterator(); cases.hasNext();) {
            SwitchStatement.SwitchBlockStatementGroup sbsg = (SwitchStatement.SwitchBlockStatementGroup) cases.next();
            for (Iterator stmts = sbsg.blockStatements.iterator(); stmts.hasNext();) {
                BlockStatement bs = (BlockStatement) stmts.next();
                vars = this.buildLocalVariableMap(bs, vars);
            }
        }
    }

    private void
    buildLocalVariableMap(SynchronizedStatement ss, final Map localVars) throws CompileException {
        ss.localVariables = localVars;
        this.buildLocalVariableMap(ss.body, localVars);
    }

    private void
    buildLocalVariableMap(TryStatement ts, final Map localVars) throws CompileException {
        ts.localVariables = localVars;
        this.buildLocalVariableMap(ts.body, localVars);
        for (Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            CatchClause cc = (CatchClause) it.next();
            this.buildLocalVariableMap(cc, localVars);
        }
        if (ts.optionalFinally != null) {
            this.buildLocalVariableMap(ts.optionalFinally, localVars);
        }
    }

    private void
    buildLocalVariableMap(WhileStatement ws, final Map localVars) throws CompileException {
        ws.localVariables = localVars;
        this.buildLocalVariableMap(ws.body, localVars);
    }

    private Map
    buildLocalVariableMap(LabeledStatement ls, final Map localVars) throws CompileException {
        ls.localVariables = localVars;
        return this.buildLocalVariableMap((BlockStatement) ls.body, localVars);
    }

    private Map
    buildLocalVariableMap(LocalVariableDeclarationStatement lvds, final Map localVars) throws CompileException {
        Map newVars = new HashMap();
        newVars.putAll(localVars);
        for (int i = 0; i < lvds.variableDeclarators.length; ++i) {
            VariableDeclarator vd = lvds.variableDeclarators[i];
            LocalVariable      lv = this.getLocalVariable(lvds, vd);
            if (newVars.put(vd.name, lv) != null) {
                this.compileError("Redefinition of local variable \"" + vd.name + "\" ", vd.getLocation());
            }
        }
        lvds.localVariables = newVars;
        return newVars;
    }

    protected void
    buildLocalVariableMap(CatchClause cc, Map localVars) throws CompileException {
        Map vars = new HashMap();
        vars.putAll(localVars);
        LocalVariable lv = this.getLocalVariable(cc.caughtException);
        vars.put(cc.caughtException.name, lv);
        this.buildLocalVariableMap(cc.body, vars);
    }

    public LocalVariable
    getLocalVariable(FunctionDeclarator.FormalParameter fp) throws CompileException {
        if (fp.localVariable == null) {
            fp.localVariable = new LocalVariable(fp.finaL, this.getType(fp.type));
        }
        return fp.localVariable;
    }

    // ------------------ Rvalue.compile() ----------------

    /**
     * Call to check whether the given {@link ArrayInitializerOrRvalue} compiles or not.
     */
    private void
    fakeCompile(ArrayInitializerOrRvalue aior) throws CompileException {
        if (aior instanceof Rvalue) {
            Rvalue rv = (Rvalue) aior;
            this.fakeCompile(rv);
        }
        if (aior instanceof ArrayInitializer) {
            ArrayInitializer ai = (ArrayInitializer) aior;
            for (int i = 0; i < ai.values.length; ++i) {
                this.fakeCompile(ai.values[i]);
            }
        }
    }

    /**
     * Call to check whether the given {@link Rvalue} compiles or not.
     */
    private void
    fakeCompile(Rvalue rv) throws CompileException {
        CodeContext savedCodeContext = this.replaceCodeContext(this.createDummyCodeContext());
        try {
            this.compileContext(rv);
            this.compileGet(rv);
        } finally {
            this.replaceCodeContext(savedCodeContext);
        }
    }

    /**
     * Some {@link Rvalue}s compile more efficiently when their value is not needed, e.g. "i++".
     */
    private void
    compile(Rvalue rv) throws CompileException {

        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                            { try { UnitCompiler.this.compile2(al);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAssignment(Assignment a)                                               { try { UnitCompiler.this.compile2(a);     } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitUnaryOperation(UnaryOperation uo)                                      { try { UnitCompiler.this.compile2(uo);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                                    { try { UnitCompiler.this.compile2(bo);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                                           { try { UnitCompiler.this.compile2(c);     } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                                          { try { UnitCompiler.this.compile2(cl);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitConditionalExpression(ConditionalExpression ce)                        { try { UnitCompiler.this.compile2(ce);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                                     { try { UnitCompiler.this.compile2(c);     } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInstanceof(Instanceof io)                                              { try { UnitCompiler.this.compile2(io);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMethodInvocation(MethodInvocation mi)                                  { try { UnitCompiler.this.compile2(mi);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi)             { try { UnitCompiler.this.compile2(smi);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                                      { try { UnitCompiler.this.compile2(il);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)                         { try { UnitCompiler.this.compile2(fpl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                                      { try { UnitCompiler.this.compile2(bl);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                                  { try { UnitCompiler.this.compile2(cl);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitStringLiteral(StringLiteral sl)                                        { try { UnitCompiler.this.compile2(sl);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNullLiteral(NullLiteral nl)                                            { try { UnitCompiler.this.compile2(nl);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)              { try { UnitCompiler.this.compile2(naci);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewArray(NewArray na)                                                  { try { UnitCompiler.this.compile2(na);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)                           { try { UnitCompiler.this.compile2(nia);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewClassInstance(NewClassInstance nci)                                 { try { UnitCompiler.this.compile2(nci);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParameterAccess(ParameterAccess pa)                                    { try { UnitCompiler.this.compile2(pa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)                     { try { UnitCompiler.this.compile2(qtr);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThisReference(ThisReference tr)                                        { try { UnitCompiler.this.compile2(tr);    } catch (CompileException e) { throw new UCE(e); } }
                                                                                                    
            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { UnitCompiler.this.compile2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { UnitCompiler.this.compile2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { UnitCompiler.this.compile2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { UnitCompiler.this.compile2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compile2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           { try { UnitCompiler.this.compile2(lva);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { UnitCompiler.this.compile2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    private void
    compile2(Rvalue rv) throws CompileException {
        this.pop(rv, this.compileGetValue(rv));
    }

    private void
    compile2(Assignment a) throws CompileException {
        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileContext(a.lhs);
            this.assignmentConversion(
                a,                           // locatable
                this.compileGetValue(a.rhs), // sourceType
                this.getType(a.lhs),         // targetType
                this.getConstantValue(a.rhs) // optionalConstantValue
            );
            this.compileSet(a.lhs);
            return;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCs = this.compileContext(a.lhs);
        this.dup(a, lhsCs);
        IClass lhsType    = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), // <= IMPORTANT!
            a.rhs                 // rhs
        );

        // Convert the result to LHS type (JLS2 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
            && !this.tryBoxingConversion(a, resultType, lhsType) // Java 5
        ) this.compileError("Operand types unsuitable for '" + a.operator + "'", a.getLocation());

        // Assign the result to the left operand.
        this.compileSet(a.lhs);
    }

    private void
    compile2(Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        LocalVariable lv = this.isIntLv(c);
        if (lv != null) {
            compileLocalVariableCrement(c, lv);
            return;
        }

        int cs = this.compileContext(c.operand);
        this.dup(c, cs);
        IClass type         = this.compileGet(c.operand);
        IClass promotedType = this.unaryNumericPromotion(c, type);
        this.writeOpcode(c, UnitCompiler.ilfd(
            promotedType,
            Opcode.ICONST_1,
            Opcode.LCONST_1,
            Opcode.FCONST_1,
            Opcode.DCONST_1
        ));
        if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.writeOpcode(c, Opcode.IADD + UnitCompiler.ilfd(promotedType));
        } else
        if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.writeOpcode(c, Opcode.ISUB + UnitCompiler.ilfd(promotedType));
        } else {
            this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
        }

        this.reverseUnaryNumericPromotion(c, promotedType, type);
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

        this.writeOpcode(aci, Opcode.ALOAD_0);
        if (declaringIClass.getOuterIClass() != null) this.writeOpcode(aci, Opcode.ALOAD_1);
        this.invokeConstructor(
            aci,                  // locatable
            declaringConstructor, // scope
            (Rvalue) null,        // optionalEnclosingInstance
            declaringIClass,      // targetClass
            aci.arguments         // arguments
        );
        return true;
    }

    private boolean
    compile2(SuperConstructorInvocation sci) throws CompileException {
        ConstructorDeclarator declaringConstructor = (ConstructorDeclarator) sci.getEnclosingScope();
        this.writeOpcode(sci, Opcode.ALOAD_0);
        ClassDeclaration declaringClass = declaringConstructor.getDeclaringClass();
        IClass           superclass     = this.resolve(declaringClass).getSuperclass();

        Rvalue optionalEnclosingInstance;
        if (sci.optionalQualification != null) {
            optionalEnclosingInstance = sci.optionalQualification;
        } else {
            IClass outerIClassOfSuperclass = superclass.getOuterIClass();
            if (outerIClassOfSuperclass == null) {
                optionalEnclosingInstance = null;
            } else {
                optionalEnclosingInstance = new QualifiedThisReference(
                    sci.getLocation(),                                         // location
                    new SimpleType(sci.getLocation(), outerIClassOfSuperclass) // qualification
                );
                optionalEnclosingInstance.setEnclosingBlockStatement(sci);
            }
        }
        this.invokeConstructor(
            sci,                       // locatable
            declaringConstructor,      // scope
            optionalEnclosingInstance, // optionalEnclosingInstance
            superclass,                // targetClass
            sci.arguments              // arguments
        );
        return true;
    }

    /**
     * Some {@link Rvalue}s compile more efficiently when their value is the condition for a branch.
     * <p>
     * Notice that if "this" is a constant, then either {@code dst} is never branched to, or it is unconditionally
     * branched to. "Unexamined code" errors may result during bytecode validation.
     */
    private void
    compileBoolean(
        Rvalue                   rv,
        final CodeContext.Offset dst,        // Where to jump.
        final boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {

        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }

        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                { try { UnitCompiler.this.compileBoolean2(al,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAssignment(Assignment a)                                   { try { UnitCompiler.this.compileBoolean2(a,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { try { UnitCompiler.this.compileBoolean2(uo,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { try { UnitCompiler.this.compileBoolean2(bo,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                               { try { UnitCompiler.this.compileBoolean2(c,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                              { try { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { try { UnitCompiler.this.compileBoolean2(ce,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                         { try { UnitCompiler.this.compileBoolean2(c,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInstanceof(Instanceof io)                                  { try { UnitCompiler.this.compileBoolean2(io,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      { try { UnitCompiler.this.compileBoolean2(mi,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) { try { UnitCompiler.this.compileBoolean2(smi,  dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          { try { UnitCompiler.this.compileBoolean2(il,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             { try { UnitCompiler.this.compileBoolean2(fpl,  dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          { try { UnitCompiler.this.compileBoolean2(bl,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      { try { UnitCompiler.this.compileBoolean2(cl,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitStringLiteral(StringLiteral sl)                            { try { UnitCompiler.this.compileBoolean2(sl,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNullLiteral(NullLiteral nl)                                { try { UnitCompiler.this.compileBoolean2(nl,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  { try { UnitCompiler.this.compileBoolean2(naci, dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewArray(NewArray na)                                      { try { UnitCompiler.this.compileBoolean2(na,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               { try { UnitCompiler.this.compileBoolean2(nia,  dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     { try { UnitCompiler.this.compileBoolean2(nci,  dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParameterAccess(ParameterAccess pa)                        { try { UnitCompiler.this.compileBoolean2(pa,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         { try { UnitCompiler.this.compileBoolean2(qtr,  dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThisReference(ThisReference tr)                            { try { UnitCompiler.this.compileBoolean2(tr,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }

            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { UnitCompiler.this.compileBoolean2(an,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { UnitCompiler.this.compileBoolean2(aae,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { UnitCompiler.this.compileBoolean2(fa,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { UnitCompiler.this.compileBoolean2(fae,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compileBoolean2(scfae, dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           { try { UnitCompiler.this.compileBoolean2(lva,   dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { UnitCompiler.this.compileBoolean2(pe,    dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    private void
    compileBoolean2(
        Rvalue             rv,
        CodeContext.Offset dst,        // Where to jump.
        boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        IClass       type = this.compileGetValue(rv);
        IClassLoader icl  = this.iClassLoader;
        if (type == icl.JAVA_LANG_BOOLEAN) {
            this.unboxingConversion(rv, icl.JAVA_LANG_BOOLEAN, IClass.BOOLEAN);
        } else
        if (type != IClass.BOOLEAN) {
            this.compileError("Not a boolean expression", rv.getLocation());
        }
        this.writeBranch(rv, orientation == Rvalue.JUMP_IF_TRUE ? Opcode.IFNE : Opcode.IFEQ, dst);
    }

    private void
    compileBoolean2(
        UnaryOperation     ue,
        CodeContext.Offset dst,        // Where to jump.
        boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        if (ue.operator == "!") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileBoolean(ue.operand, dst, !orientation);
            return;
        }

        this.compileError("Boolean expression expected", ue.getLocation());
    }
    private void
    compileBoolean2(
        BinaryOperation      bo,
        CodeContext.Offset   dst,        // Where to jump.
        boolean              orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {

        if (bo.op == "|" || bo.op == "^" || bo.op == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.compileBoolean2((Rvalue) bo, dst, orientation);
            return;
        }

        if (bo.op == "||" || bo.op == "&&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsCv = this.getConstantValue(bo.lhs);
            if (lhsCv instanceof Boolean) {
                if (((Boolean) lhsCv).booleanValue() ^ bo.op == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    // "true && a", "false || a"
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE
                    );
                } else {
                    // "false && a", "true || a"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE
                    );
                    this.fakeCompile(bo.rhs);
                }
                return;
            }
            Object rhsCv = this.getConstantValue(bo.rhs);
            if (rhsCv instanceof Boolean) {
                if (((Boolean) rhsCv).booleanValue() ^ bo.op == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    // "a && true", "a || false"
                    this.compileBoolean(
                        bo.lhs,
                        dst,
                        Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE
                    );
                } else {
                    // "a && false", "a || true"
                    this.pop(bo.lhs, this.compileGetValue(bo.lhs));
                    this.compileBoolean(
                        bo.rhs,
                        dst,
                        Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE
                    );
                }
                return;
            }
            if (bo.op == "||" ^ orientation == Rvalue.JUMP_IF_FALSE) { // SUPPRESS CHECKSTYLE StringLiteralEquality
                this.compileBoolean(bo.lhs, dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
                this.compileBoolean(bo.rhs, dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
            } else {
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(bo.lhs, end, Rvalue.JUMP_IF_FALSE ^ orientation == Rvalue.JUMP_IF_FALSE);
                this.compileBoolean(bo.rhs, dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
                end.set();
            }
            return;
        }

        if (
            bo.op == "=="    // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "!=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "<=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == ">=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "<"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == ">"  // SUPPRESS CHECKSTYLE StringLiteralEquality
        ) {
            int opIdx = (
                bo.op == "==" ? 0 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.op == "!=" ? 1 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.op == "<"  ? 2 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.op == ">=" ? 3 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.op == ">"  ? 4 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                bo.op == "<=" ? 5 : // SUPPRESS CHECKSTYLE StringLiteralEquality
                Integer.MIN_VALUE
            );
            if (orientation == Rvalue.JUMP_IF_FALSE) opIdx ^= 1;

            // Comparison with "null".
            {
                boolean lhsIsNull = this.getConstantValue(bo.lhs) == null;
                boolean rhsIsNull = this.getConstantValue(bo.rhs) == null;

                if (lhsIsNull || rhsIsNull) {
                    if (bo.op != "==" && bo.op != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                        this.compileError(
                            "Operator \"" + bo.op + "\" not allowed on operand \"null\"",
                            bo.getLocation()
                        );
                    }

                    // check types for x == null, null == x, but NOT null == null
                    if (!lhsIsNull || !rhsIsNull) {
                        IClass ohsType = this.compileGetValue(lhsIsNull ? bo.rhs : bo.lhs);
                        if (ohsType.isPrimitive()) {
                            this.compileError(
                                "Cannot compare \"null\" with primitive type \"" + ohsType.toString() + "\"",
                                bo.getLocation()
                            );
                        }
                    }
                    this.writeBranch(bo, Opcode.IFNULL + opIdx, dst);
                    return;
                }
            }

            IClass               lhsType            = this.compileGetValue(bo.lhs);
            CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
            IClass               rhsType            = this.compileGetValue(bo.rhs);

            // 15.20.1 Numerical comparison.
            if (
                this.getUnboxedType(lhsType).isPrimitiveNumeric()
                && this.getUnboxedType(rhsType).isPrimitiveNumeric()
                && !(
                    (bo.op == "==" || bo.op == "!=") // SUPPRESS CHECKSTYLE StringLiteralEquality
                    && !lhsType.isPrimitive()
                    && !rhsType.isPrimitive()
                )
            ) {
                IClass promotedType = this.binaryNumericPromotion(bo, lhsType, convertLhsInserter, rhsType);
                if (promotedType == IClass.INT) {
                    this.writeBranch(bo, Opcode.IF_ICMPEQ + opIdx, dst);
                } else
                if (promotedType == IClass.LONG) {
                    this.writeOpcode(bo, Opcode.LCMP);
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                if (promotedType == IClass.FLOAT) {
                    if (bo.op == ">" || bo.op == ">=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                        this.writeOpcode(bo, Opcode.FCMPL);
                    } else {
                        this.writeOpcode(bo, Opcode.FCMPG);
                    }
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                if (promotedType == IClass.DOUBLE) {
                    if (bo.op == ">" || bo.op == ">=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                        this.writeOpcode(bo, Opcode.DCMPL);
                    } else {
                        this.writeOpcode(bo, Opcode.DCMPG);
                    }
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                {
                    throw new JaninoRuntimeException("Unexpected promoted type \"" + promotedType + "\"");
                }
                return;
            }

            // JLS3 15.21.2 Boolean Equality Operators == and !=
            if (
                (lhsType == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                || (rhsType == IClass.BOOLEAN && this.getUnboxedType(lhsType) == IClass.BOOLEAN)
            ) {
                if (bo.op != "==" && bo.op != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError("Operator \"" + bo.op + "\" not allowed on boolean operands", bo.getLocation());
                }
                IClassLoader icl = this.iClassLoader;

                // Unbox LHS if necessary.
                if (lhsType == icl.JAVA_LANG_BOOLEAN) {
                    this.codeContext.pushInserter(convertLhsInserter);
                    try {
                        this.unboxingConversion(bo, icl.JAVA_LANG_BOOLEAN, IClass.BOOLEAN);
                    } finally {
                        this.codeContext.popInserter();
                    }
                }

                // Unbox RHS if necessary.
                if (rhsType == icl.JAVA_LANG_BOOLEAN) {
                    this.unboxingConversion(bo, icl.JAVA_LANG_BOOLEAN, IClass.BOOLEAN);
                }

                this.writeBranch(bo, Opcode.IF_ICMPEQ + opIdx, dst);
                return;
            }

            // Reference comparison.
            // Note: Comparison with "null" is already handled above.
            if (!lhsType.isPrimitive() && !rhsType.isPrimitive()) {
                if (bo.op != "==" && bo.op != "!=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    this.compileError("Operator \"" + bo.op + "\" not allowed on reference operands", bo.getLocation());
                }
                if (
                    !isCastReferenceConvertible(lhsType, rhsType)
                    || !isCastReferenceConvertible(rhsType, lhsType)
                ) this.compileError("Incomparable types '" + lhsType + "' and '" + rhsType + "'", bo.getLocation());
                this.writeBranch(bo, Opcode.IF_ACMPEQ + opIdx, dst);
                return;
            }

            this.compileError("Cannot compare types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());
        }

        this.compileError("Boolean expression expected", bo.getLocation());
    }

    private void
    compileBoolean2(ParenthesizedExpression pe, CodeContext.Offset dst, boolean orientation) throws CompileException {
        this.compileBoolean(pe.value, dst, orientation);
    }

    /**
     * Generates code that determines the context of the {@link Rvalue} and puts it on the operand stack. Most
     * expressions do not have a "context", but some do. E.g. for "x[y]", the context is "x, y". The bottom line is
     * that for statements like "x[y] += 3" the context is only evaluated once.
     *
     * @return The size of the context on the operand stack
     */
    private int
    compileContext(Rvalue rv) throws CompileException {
        final int[] res = new int[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                { try { res[0] = UnitCompiler.this.compileContext2(al);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAssignment(Assignment a)                                   {       res[0] = UnitCompiler.this.compileContext2(a);    }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          {       res[0] = UnitCompiler.this.compileContext2(uo);   }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        {       res[0] = UnitCompiler.this.compileContext2(bo);   }
            @Override public void visitCast(Cast c)                                               {       res[0] = UnitCompiler.this.compileContext2(c);    }
            @Override public void visitClassLiteral(ClassLiteral cl)                              {       res[0] = UnitCompiler.this.compileContext2(cl);   }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            {       res[0] = UnitCompiler.this.compileContext2(ce);   }
            @Override public void visitCrement(Crement c)                                         {       res[0] = UnitCompiler.this.compileContext2(c);    }
            @Override public void visitInstanceof(Instanceof io)                                  {       res[0] = UnitCompiler.this.compileContext2(io);   }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      {       res[0] = UnitCompiler.this.compileContext2(mi);   }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) {       res[0] = UnitCompiler.this.compileContext2(smi);  }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          {       res[0] = UnitCompiler.this.compileContext2(il);   }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             {       res[0] = UnitCompiler.this.compileContext2(fpl);  }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          {       res[0] = UnitCompiler.this.compileContext2(bl);   }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      {       res[0] = UnitCompiler.this.compileContext2(cl);   }
            @Override public void visitStringLiteral(StringLiteral sl)                            {       res[0] = UnitCompiler.this.compileContext2(sl);   }
            @Override public void visitNullLiteral(NullLiteral nl)                                {       res[0] = UnitCompiler.this.compileContext2(nl);   }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  {       res[0] = UnitCompiler.this.compileContext2(naci); }
            @Override public void visitNewArray(NewArray na)                                      {       res[0] = UnitCompiler.this.compileContext2(na);   }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               {       res[0] = UnitCompiler.this.compileContext2(nia);  }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     {       res[0] = UnitCompiler.this.compileContext2(nci);  }
            @Override public void visitParameterAccess(ParameterAccess pa)                        {       res[0] = UnitCompiler.this.compileContext2(pa);   }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         {       res[0] = UnitCompiler.this.compileContext2(qtr);  }
            @Override public void visitThisReference(ThisReference tr)                            {       res[0] = UnitCompiler.this.compileContext2(tr);   }

            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.compileContext2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { res[0] = UnitCompiler.this.compileContext2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { res[0] = UnitCompiler.this.compileContext2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { res[0] = UnitCompiler.this.compileContext2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.compileContext2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       res[0] = UnitCompiler.this.compileContext2(lva);                                                      }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { res[0] = UnitCompiler.this.compileContext2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
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
                // JLS3 15.11.1.3.1.1:
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
        if (
            !this.tryIdentityConversion(indexType, IClass.INT)
            && !this.tryWideningPrimitiveConversion(aae, indexType, IClass.INT)
        ) this.compileError(
            "Index expression of type \"" + indexType + "\" cannot be widened to \"int\"",
            aae.getLocation()
        );

        return 2;
    }

    private int
    compileContext2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileContext(fae.value);
    }

    private int
    compileContext2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.compileContext(scfae.value);
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
        final IClass[] res = new IClass[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                {       res[0] = UnitCompiler.this.compileGet2(al);                                                      }
            @Override public void visitAssignment(Assignment a)                                   { try { res[0] = UnitCompiler.this.compileGet2(a);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { try { res[0] = UnitCompiler.this.compileGet2(uo);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { try { res[0] = UnitCompiler.this.compileGet2(bo);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                               { try { res[0] = UnitCompiler.this.compileGet2(c);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                              { try { res[0] = UnitCompiler.this.compileGet2(cl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { try { res[0] = UnitCompiler.this.compileGet2(ce);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                         { try { res[0] = UnitCompiler.this.compileGet2(c);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInstanceof(Instanceof io)                                  { try { res[0] = UnitCompiler.this.compileGet2(io);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      { try { res[0] = UnitCompiler.this.compileGet2(mi);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) { try { res[0] = UnitCompiler.this.compileGet2(smi);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          { try { res[0] = UnitCompiler.this.compileGet2(il);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             { try { res[0] = UnitCompiler.this.compileGet2(fpl);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          { try { res[0] = UnitCompiler.this.compileGet2(bl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      { try { res[0] = UnitCompiler.this.compileGet2(cl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitStringLiteral(StringLiteral sl)                            { try { res[0] = UnitCompiler.this.compileGet2(sl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNullLiteral(NullLiteral nl)                                { try { res[0] = UnitCompiler.this.compileGet2(nl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  { try { res[0] = UnitCompiler.this.compileGet2(naci); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewArray(NewArray na)                                      { try { res[0] = UnitCompiler.this.compileGet2(na);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               { try { res[0] = UnitCompiler.this.compileGet2(nia);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     { try { res[0] = UnitCompiler.this.compileGet2(nci);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParameterAccess(ParameterAccess pa)                        { try { res[0] = UnitCompiler.this.compileGet2(pa);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         { try { res[0] = UnitCompiler.this.compileGet2(qtr);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThisReference(ThisReference tr)                            { try { res[0] = UnitCompiler.this.compileGet2(tr);   } catch (CompileException e) { throw new UCE(e); } }

            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.compileGet2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { res[0] = UnitCompiler.this.compileGet2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { res[0] = UnitCompiler.this.compileGet2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { res[0] = UnitCompiler.this.compileGet2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.compileGet2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       res[0] = UnitCompiler.this.compileGet2(lva);                                                      }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { res[0] = UnitCompiler.this.compileGet2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    private IClass
    compileGet2(BooleanRvalue brv) throws CompileException {
        CodeContext.Offset isTrue = this.codeContext.new Offset();
        this.compileBoolean(brv, isTrue, Rvalue.JUMP_IF_TRUE);
        this.writeOpcode(brv, Opcode.ICONST_0);
        CodeContext.Offset end = this.codeContext.new Offset();
        this.writeBranch(brv, Opcode.GOTO, end);
        isTrue.set();
        this.writeOpcode(brv, Opcode.ICONST_1);
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
        this.checkAccessible(fa.field, fa.getEnclosingBlockStatement());
        if (fa.field.isStatic()) {
            this.writeOpcode(fa, Opcode.GETSTATIC);
        } else {
            this.writeOpcode(fa, Opcode.GETFIELD);
        }
        this.writeConstantFieldrefInfo(
            fa.field.getDeclaringIClass().getDescriptor(), // classFD
            fa.field.getName(),                            // fieldName
            fa.field.getType().getDescriptor()             // fieldFD
        );
        return fa.field.getType();
    }

    private IClass
    compileGet2(ArrayLength al) {
        this.writeOpcode(al, Opcode.ARRAYLENGTH);
        return IClass.INT;
    }

    private IClass
    compileGet2(ThisReference tr) throws CompileException {
        this.referenceThis(tr);
        return this.getIClass(tr);
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
        Location           loc    = cl.getLocation();
        final IClassLoader icl    = this.iClassLoader;
        IClass             iClass = this.getType(cl.type);

        if (iClass.isPrimitive()) {

            // Primitive class literal.
            this.writeOpcode(cl, Opcode.GETSTATIC);
            String wrapperClassDescriptor = (
                iClass == IClass.VOID    ? "Ljava/lang/Void;"      :
                iClass == IClass.BYTE    ? "Ljava/lang/Byte;"      :
                iClass == IClass.CHAR    ? "Ljava/lang/Character;" :
                iClass == IClass.DOUBLE  ? "Ljava/lang/Double;"    :
                iClass == IClass.FLOAT   ? "Ljava/lang/Float;"     :
                iClass == IClass.INT     ? "Ljava/lang/Integer;"   :
                iClass == IClass.LONG    ? "Ljava/lang/Long;"      :
                iClass == IClass.SHORT   ? "Ljava/lang/Short;"     :
                iClass == IClass.BOOLEAN ? "Ljava/lang/Boolean;"   :
                null
            );
            if (wrapperClassDescriptor == null) {
                throw new JaninoRuntimeException("SNO: Unidentifiable primitive type \"" + iClass + "\"");
            }

            this.writeConstantFieldrefInfo(
                wrapperClassDescriptor, // classFD
                "TYPE",                 // fieldName
                "Ljava/lang/Class;"     // fieldFD
            );
            return icl.JAVA_LANG_CLASS;
        }

        // Non-primitive class literal.

        TypeDeclaration declaringType;
        for (Scope s = cl.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof TypeDeclaration) {
                declaringType = (AbstractTypeDeclaration) s;
                break;
            }
        }

        // Check if synthetic method "static Class class$(String className)" is already declared.
        if (declaringType.getMethodDeclaration("class$") == null) this.declareClassDollarMethod(cl);

        // Determine the statics of the declaring class (this is where static fields declarations are found).
        List statics; // TypeBodyDeclaration
        if (declaringType instanceof ClassDeclaration) {
            statics = ((ClassDeclaration) declaringType).variableDeclaratorsAndInitializers;
        } else
        if (declaringType instanceof InterfaceDeclaration) {
            statics = ((InterfaceDeclaration) declaringType).constantDeclarations;
        } else {
            throw new JaninoRuntimeException(
                "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
            );
        }

        String className = Descriptor.toClassName(iClass.getDescriptor());

        // Compose the "class-dollar" field name. This i done as follows:
        //   Type         Class-name           Field-name
        //   String       java.lang.String     class$java$lang$String
        //   String[]     [Ljava.lang.String;  array$Ljava$lang$String
        //   String[][]   [[Ljava.lang.String; array$$Ljava$lang$String
        //   String[][][] [[[java.lang.String; array$$$Ljava$lang$String
        //   int[]        [I                   array$I
        //   int[][]      [[I                  array$$I
        String classDollarFieldName;
        {
            if (className.startsWith("[")) {
                classDollarFieldName = "array" + className.replace('.', '$').replace('[', '$');
                if (classDollarFieldName.endsWith(";")) {
                    classDollarFieldName = classDollarFieldName.substring(0, classDollarFieldName.length() - 1);
                }
            } else
            {
                classDollarFieldName = "class$" + className.replace('.', '$');
            }
        }

        // Declare the static "class dollar field" if not already done.
        {
            boolean hasClassDollarField = false;
            BLOCK_STATEMENTS: for (Iterator it = statics.iterator(); it.hasNext();) {
                TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                if (!tbd.isStatic()) continue;
                if (tbd instanceof FieldDeclaration) {
                    FieldDeclaration fd  = (FieldDeclaration) tbd;
                    IClass.IField[]  fds = this.getIFields(fd);
                    for (int j = 0; j < fds.length; ++j) {
                        if (fds[j].getName().equals(classDollarFieldName)) {
                            hasClassDollarField = true;
                            break BLOCK_STATEMENTS;
                        }
                    }
                }
            }
            if (!hasClassDollarField) {
                Type             classType = new SimpleType(loc, icl.JAVA_LANG_CLASS);
                FieldDeclaration fd        = new FieldDeclaration(
                    loc,                                          // location
                    null,                                         // optionalDocComment
                    new Java.ModifiersAndAnnotations(Mod.STATIC), // modifiers
                    classType,                                    // type
                    new VariableDeclarator[] {                    // variableDeclarators
                        new VariableDeclarator(
                            loc,                  // location
                            classDollarFieldName, // name
                            0,                    // brackets
                            (Rvalue) null         // optionalInitializer
                        )
                    }
                );
                if (declaringType instanceof ClassDeclaration) {
                    ((ClassDeclaration) declaringType).addVariableDeclaratorOrInitializer(fd);
                } else
                if (declaringType instanceof InterfaceDeclaration) {
                    ((InterfaceDeclaration) declaringType).addConstantDeclaration(fd);
                } else {
                    throw new JaninoRuntimeException(
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }
        }

        // return (class$X != null) ? class$X : (class$X = class$("X"));
        Type   declaringClassOrInterfaceType = new SimpleType(loc, this.resolve(declaringType));
        Lvalue classDollarFieldAccess        = new FieldAccessExpression(
            loc,                           // location
            declaringClassOrInterfaceType, // lhs
            classDollarFieldName           // fieldName
        );
        ConditionalExpression ce = new ConditionalExpression(
            loc,                      // location
            new BinaryOperation(      // lhs
                loc,                         // location
                classDollarFieldAccess,      // lhs
                "!=",                        // op
                new NullLiteral(loc, "null") // rhs
            ),
            classDollarFieldAccess,   // mhs
            new Assignment(           // rhs
                loc,                    // location
                classDollarFieldAccess, // lhs
                "=",                    // operator
                new MethodInvocation(   // rhs
                    loc,                           // location
                    declaringClassOrInterfaceType, // optionalTarget
                    "class$",                      // methodName
                    new Rvalue[] {                 // arguments
                        new StringLiteral(
                            loc,                    // location
                            '"' + className + '"'   // constantValue
                        )
                    }
                )
            )
        );
        ce.setEnclosingBlockStatement(cl.getEnclosingBlockStatement());
        return this.compileGet(ce);
    }
    private IClass
    compileGet2(Assignment a) throws CompileException {
        if (a.operator == "=") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            int    lhsCs   = this.compileContext(a.lhs);
            IClass rhsType = this.compileGetValue(a.rhs);
            IClass lhsType = this.getType(a.lhs);
            Object rhsCv   = this.getConstantValue(a.rhs);
            this.assignmentConversion(a, rhsType, lhsType, rhsCv);
            this.dupx(a, lhsType, lhsCs);
            this.compileSet(a.lhs);
            return lhsType;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCs = this.compileContext(a.lhs);
        this.dup(a, lhsCs);
        IClass lhsType    = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            a,                    // locatable
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), // <= IMPORTANT!
            a.rhs                 // rhs
        );
        // Convert the result to LHS type (JLS2 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType)
            && !this.tryNarrowingPrimitiveConversion(a, resultType, lhsType)
        ) throw new JaninoRuntimeException("SNO: \"" + a.operator + "\" reconversion failed");
        this.dupx(a, lhsType, lhsCs);
        this.compileSet(a.lhs);
        return lhsType;
    }

    private IClass
    compileGet2(ConditionalExpression ce) throws CompileException {
        IClass               mhsType, rhsType;
        CodeContext.Inserter mhsConvertInserter, rhsConvertInserter;
        CodeContext.Offset   toEnd = this.codeContext.new Offset();
        {
            Object cv = this.getConstantValue(ce.lhs);
            if (cv instanceof Boolean) {
                if (((Boolean) cv).booleanValue()) {
                    mhsType            = this.compileGetValue(ce.mhs);
                    mhsConvertInserter = this.codeContext.newInserter();
                    rhsType            = this.getType(ce.rhs);
                    rhsConvertInserter = null;
                } else {
                    mhsType            = this.getType(ce.mhs);
                    mhsConvertInserter = null;
                    rhsType            = this.compileGetValue(ce.rhs);
                    rhsConvertInserter = this.codeContext.currentInserter();
                }
            } else {
                CodeContext.Offset toRhs = this.codeContext.new Offset();

                this.compileBoolean(ce.lhs, toRhs, Rvalue.JUMP_IF_FALSE);
                mhsType            = this.compileGetValue(ce.mhs);
                mhsConvertInserter = this.codeContext.newInserter();
                this.writeBranch(ce, Opcode.GOTO, toEnd);
                toRhs.set();
                rhsType            = this.compileGetValue(ce.rhs);
                rhsConvertInserter = this.codeContext.currentInserter();
            }
        }
        IClass expressionType;
        if (mhsType == rhsType) {

            // JLS 15.25.1.1
            expressionType = mhsType;
        } else
        if (mhsType.isPrimitiveNumeric() && rhsType.isPrimitiveNumeric()) {

            // JLS 15.25.1.2

            // TODO JLS 15.25.1.2.1

            // TODO JLS 15.25.1.2.2

            // JLS 15.25.1.2.3
            expressionType = this.binaryNumericPromotion(
                ce,                 // locatable
                mhsType,            // type1
                mhsConvertInserter, // convertInserter1
                rhsType,            // type2
                rhsConvertInserter  // convertInserter2
            );
        } else
        if (this.getConstantValue(ce.mhs) == null && !rhsType.isPrimitive()) {

            // JLS 15.25.1.3 (null : reference)
            expressionType = rhsType;
        } else
        if (!mhsType.isPrimitive() && this.getConstantValue(ce.rhs) == null) {

            // JLS 15.25.1.3 (reference : null)
            expressionType = mhsType;
        } else
        if (!mhsType.isPrimitive() && !rhsType.isPrimitive()) {
            if (mhsType.isAssignableFrom(rhsType)) {
                expressionType = mhsType;
            } else
            if (rhsType.isAssignableFrom(mhsType)) {
                expressionType = rhsType;
            } else {
                this.compileError(
                    "Reference types \"" + mhsType + "\" and \"" + rhsType + "\" don't match",
                    ce.getLocation()
                );
                return this.iClassLoader.JAVA_LANG_OBJECT;
            }
        } else
        {
            this.compileError(
                "Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"",
                ce.getLocation()
            );
            return this.iClassLoader.JAVA_LANG_OBJECT;
        }
        toEnd.set();

        return expressionType;
    }

    private IClass
    compileGet2(Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        LocalVariable lv = this.isIntLv(c);
        if (lv != null) {
            if (!c.pre) this.load(c, lv);
            compileLocalVariableCrement(c, lv);
            if (c.pre) this.load(c, lv);
            return lv.type;
        }

        // Compile operand context.
        int cs = this.compileContext(c.operand);
        // DUP operand context.
        this.dup(c, cs);
        // Get operand value.
        IClass type = this.compileGet(c.operand);
        // DUPX operand value.
        if (!c.pre) this.dupx(c, type, cs);
        // Apply "unary numeric promotion".
        IClass promotedType = this.unaryNumericPromotion(c, type);
        // Crement.
        this.writeOpcode(c, UnitCompiler.ilfd(
            promotedType,
            Opcode.ICONST_1,
            Opcode.LCONST_1,
            Opcode.FCONST_1,
            Opcode.DCONST_1
        ));
        if (c.operator == "++") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.writeOpcode(c, Opcode.IADD + UnitCompiler.ilfd(promotedType));
        } else
        if (c.operator == "--") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            this.writeOpcode(c, Opcode.ISUB + UnitCompiler.ilfd(promotedType));
        } else {
            this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
        }
        this.reverseUnaryNumericPromotion(c, promotedType, type);
        // DUPX cremented operand value.
        if (c.pre) this.dupx(c, type, cs);
        // Set operand.
        this.compileSet(c.operand);

        return type;
    }

    private void
    compileLocalVariableCrement(Crement c, LocalVariable lv) {
        if (lv.getSlotIndex() > 255) {
            this.writeOpcode(c, Opcode.WIDE);
            this.writeOpcode(c, Opcode.IINC);
            this.writeShort(lv.getSlotIndex());
            this.writeShort(c.operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        } else {
            this.writeOpcode(c, Opcode.IINC);
            this.writeByte(lv.getSlotIndex());
            this.writeByte(c.operator == "++" ? 1 : -1); // SUPPRESS CHECKSTYLE StringLiteralEquality
        }
    }

    private IClass
    compileGet2(ArrayAccessExpression aae) throws CompileException {
        IClass lhsComponentType = this.getType(aae);
        this.writeOpcode(aae, Opcode.IALOAD + UnitCompiler.ilfdabcs(lhsComponentType));
        return lhsComponentType;
    }

    private IClass
    compileGet2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileGet(fae.value);
    }

    private IClass
    compileGet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.compileGet(scfae.value);
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
                Object ncv = this.getNegatedConstantValue(uo.operand);
                if (ncv != NOT_CONSTANT) {
                    return this.unaryNumericPromotion(uo, this.pushConstant(uo, ncv));
                }
            }

            IClass promotedType = this.unaryNumericPromotion(
                uo,
                this.convertToPrimitiveNumericType(uo, this.compileGetValue(uo.operand))
            );
            this.writeOpcode(uo, Opcode.INEG + UnitCompiler.ilfd(promotedType));
            return promotedType;
        }

        if (uo.operator == "~") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IClass operandType = this.compileGetValue(uo.operand);

            IClass promotedType = this.unaryNumericPromotion(uo, operandType);
            if (promotedType == IClass.INT) {
                this.writeOpcode(uo, Opcode.ICONST_M1);
                this.writeOpcode(uo, Opcode.IXOR);
                return IClass.INT;
            }
            if (promotedType == IClass.LONG) {
                this.writeOpcode(uo, Opcode.LDC2_W);
                this.writeConstantLongInfo(-1L);
                this.writeOpcode(uo, Opcode.LXOR);
                return IClass.LONG;
            }
            this.compileError("Operator \"~\" not applicable to type \"" + promotedType + "\"", uo.getLocation());
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return this.iClassLoader.JAVA_LANG_OBJECT;
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
            this.writeOpcode(io, Opcode.INSTANCEOF);
            this.writeConstantClassInfo(rhsType.getDescriptor());
        } else {
            this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"", io.getLocation());
        }
        return IClass.BOOLEAN;
    }

    private IClass
    compileGet2(BinaryOperation bo) throws CompileException {
        if (
            bo.op == "||"    // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "==" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "!=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "<"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == ">"  // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == "<=" // SUPPRESS CHECKSTYLE StringLiteralEquality
            || bo.op == ">=" // SUPPRESS CHECKSTYLE StringLiteralEquality
        ) {
            // Eventually calls "compileBoolean()".
            return this.compileGet2((BooleanRvalue) bo);
        }

        // Implements "| ^ & * / % + - << >> >>>".
        return this.compileArithmeticOperation(
            bo,                         // locatable
            null,                       // type
            bo.unrollLeftAssociation(), // operands
            bo.op                       // operator
        );
    }

    private IClass
    compileGet2(Cast c) throws CompileException {

        // JLS3 5.5 Casting Conversion
        IClass tt = this.getType(c.targetType);
        IClass vt = this.compileGetValue(c.value);
        if (
            this.tryIdentityConversion(vt, tt)
            || this.tryWideningPrimitiveConversion(c, vt, tt)
            || this.tryNarrowingPrimitiveConversion(c, vt, tt)
            || this.tryWideningReferenceConversion(vt, tt)
            || this.tryNarrowingReferenceConversion(c, vt, tt)
            || this.tryBoxingConversion(c, vt, tt)
            || this.tryUnboxingConversion(c, vt, tt)
        ) return tt;

        // JAVAC obviously also permits 'boxing conversion followed by widening reference conversion' and 'unboxing
        // conversion followed by widening primitive conversion', although these are not described by the JLS3
        // (http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.5). For the sake of compatibility,
        // we implement them.
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

        if (mi.optionalTarget == null) {

            // JLS2 6.5.7.1, 15.12.4.1.1.1
            TypeBodyDeclaration scopeTbd;
            ClassDeclaration    scopeClassDeclaration;
            {
                Scope s;
                for (
                    s = mi.getEnclosingBlockStatement();
                    !(s instanceof TypeBodyDeclaration);
                    s = s.getEnclosingScope()
                );
                scopeTbd = (TypeBodyDeclaration) s;
                if (!(s instanceof ClassDeclaration)) s = s.getEnclosingScope();
                scopeClassDeclaration = (ClassDeclaration) s;
            }
            if (iMethod.isStatic()) {
                this.warning(
                    "IASM",
                    "Implicit access to static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );
                // JLS2 15.12.4.1.1.1.1
                ;
            } else {
                this.warning(
                    "IANSM",
                    "Implicit access to non-static method \"" + iMethod.toString() + "\"",
                    mi.getLocation()
                );
                // JLS2 15.12.4.1.1.1.2
                if (scopeTbd.isStatic()) {
                    this.compileError(
                        "Instance method \"" + iMethod.toString() + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }
                this.referenceThis(
                    mi,              // l
                    scopeClassDeclaration,       // declaringClass
                    scopeTbd,                    // declaringTypeBodyDeclaration
                    iMethod.getDeclaringIClass() // targetIClass
                );
            }
        } else {

            // 6.5.7.2
            boolean staticContext = this.isType(mi.optionalTarget);
            if (staticContext) {
                this.getType(this.toTypeOrCompileException(mi.optionalTarget));
            } else
            {
                this.compileGetValue(this.toRvalueOrCompileException(mi.optionalTarget));
            }
            if (iMethod.isStatic()) {
                if (!staticContext) {
                    // JLS2 15.12.4.1.2.1
                    this.pop(mi.optionalTarget, this.getType(mi.optionalTarget));
                }
            } else {
                if (staticContext) {
                    this.compileError(
                        "Instance method \"" + mi.methodName + "\" cannot be invoked in static context",
                        mi.getLocation()
                    );
                }
            }
        }

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < mi.arguments.length; ++i) {
            this.assignmentConversion(
                mi,                        // l
                this.compileGetValue(mi.arguments[i]), // sourceType
                parameterTypes[i],                     // targetType
                this.getConstantValue(mi.arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        this.checkAccessible(iMethod, mi.getEnclosingBlockStatement());
        if (iMethod.getDeclaringIClass().isInterface()) {
            this.writeOpcode(mi, Opcode.INVOKEINTERFACE);
            this.writeConstantInterfaceMethodrefInfo(
                iMethod.getDeclaringIClass().getDescriptor(), // classFD
                iMethod.getName(),                            // methodName
                iMethod.getDescriptor()                       // methodMD
            );
            IClass[] pts   = iMethod.getParameterTypes();
            int      count = 1;
            for (int i = 0; i < pts.length; ++i) count += Descriptor.size(pts[i].getDescriptor());
            this.writeByte(count);
            this.writeByte(0);
        } else {
            if (!iMethod.isStatic() && iMethod.getAccess() == Access.PRIVATE) {

                // In order to make a non-static private method invocable for enclosing types,
                // enclosed types and types enclosed by the same type, "compile(FunctionDeclarator)"
                // modifies it on-the-fly as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                //  + The name is appended with "$"
                //  + It is made static
                //  + A parameter of type "declaring class" is prepended to the signature
                // Hence, the invocation of such a method must be modified accordingly.
                this.writeOpcode(mi, Opcode.INVOKESTATIC);
                this.writeConstantMethodrefInfo(
                    iMethod.getDeclaringIClass().getDescriptor(), // classFD
                    iMethod.getName() + '$',                      // methodName
                    MethodDescriptor.prependParameter(            // methodMD
                        iMethod.getDescriptor(),
                        iMethod.getDeclaringIClass().getDescriptor()
                    )
                );
            } else
            {
                byte opcode = iMethod.isStatic() ? Opcode.INVOKESTATIC : Opcode.INVOKEVIRTUAL;
                this.writeOpcode(mi, opcode);
                this.writeConstantMethodrefInfo(
                    iMethod.getDeclaringIClass().getDescriptor(), // classFD
                    iMethod.getName(),                            // methodName
                    iMethod.getDescriptor()                       // methodMD
                );
            }
        }
        return iMethod.getReturnType();
    }

    private IClass
    compileGet2(SuperclassMethodInvocation scmi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(scmi);

        Scope s;
        for (
            s = scmi.getEnclosingBlockStatement();
            s instanceof Statement || s instanceof CatchClause;
            s = s.getEnclosingScope()
        );
        FunctionDeclarator fd = s instanceof FunctionDeclarator ? (FunctionDeclarator) s : null;
        if (fd == null) {
            this.compileError("Cannot invoke superclass method in non-method scope", scmi.getLocation());
            return IClass.INT;
        }
        if ((fd.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0) {
            this.compileError("Cannot invoke superclass method in static context", scmi.getLocation());
        }
        this.load(scmi, this.resolve(fd.getDeclaringType()), 0);

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < scmi.arguments.length; ++i) {
            this.assignmentConversion(
                scmi,                        // l
                this.compileGetValue(scmi.arguments[i]), // sourceType
                parameterTypes[i],                       // targetType
                this.getConstantValue(scmi.arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        this.writeOpcode(scmi, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            iMethod.getDeclaringIClass().getDescriptor(), // classFD
            scmi.methodName,                              // methodName
            iMethod.getDescriptor()                       // methodMD
        );
        return iMethod.getReturnType();
    }

    private IClass
    compileGet2(NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);

        this.writeOpcode(nci, Opcode.NEW);
        this.writeConstantClassInfo(nci.iClass.getDescriptor());
        this.writeOpcode(nci, Opcode.DUP);

        if (nci.iClass.isInterface()) this.compileError("Cannot instantiate \"" + nci.iClass + "\"", nci.getLocation());
        this.checkAccessible(nci.iClass, nci.getEnclosingBlockStatement());
        if (nci.iClass.isAbstract()) {
            this.compileError("Cannot instantiate abstract \"" + nci.iClass + "\"", nci.getLocation());
        }

        // Determine the enclosing instance for the new object.
        Rvalue optionalEnclosingInstance;
        if (nci.optionalQualification != null) {
            if (nci.iClass.getOuterIClass() == null) {
                this.compileError("Static member class cannot be instantiated with qualified NEW");
            }

            // Enclosing instance defined by qualification (JLS 15.9.2.BL1.B3.B2).
            optionalEnclosingInstance = nci.optionalQualification;
        } else {
            Scope s = nci.getEnclosingBlockStatement();
            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            TypeBodyDeclaration enclosingTypeBodyDeclaration = (TypeBodyDeclaration) s;
            TypeDeclaration     enclosingTypeDeclaration     = (TypeDeclaration) s.getEnclosingScope();

            if (
                !(enclosingTypeDeclaration instanceof ClassDeclaration)
                || enclosingTypeBodyDeclaration.isStatic()
            ) {

                // No enclosing instance in
                //  + interface method declaration or
                //  + static type body declaration (here: method or initializer or field declarator)
                // context (JLS 15.9.2.BL1.B3.B1.B1).
                if (nci.iClass.getOuterIClass() != null) {
                    this.compileError(
                        "Instantiation of \"" + nci.type + "\" requires an enclosing instance",
                        nci.getLocation()
                    );
                }
                optionalEnclosingInstance = null;
            } else {

                // Determine the type of the enclosing instance for the new object.
                IClass optionalOuterIClass = nci.iClass.getDeclaringIClass();
                if (optionalOuterIClass == null) {

                    // No enclosing instance needed for a top-level class object.
                    optionalEnclosingInstance = null;
                } else {

                    // Find an appropriate enclosing instance for the new inner class object among
                    // the enclosing instances of the current object (JLS
                    // 15.9.2.BL1.B3.B1.B2).
//                    ClassDeclaration outerClassDeclaration = (ClassDeclaration) enclosingTypeDeclaration;
//                    optionalEnclosingInstance = new QualifiedThisReference(
//                        nci.getLocation(),            // location
//                        outerClassDeclaration,        // declaringClass
//                        enclosingTypeBodyDeclaration, // declaringTypeBodyDeclaration
//                        optionalOuterIClass           // targetIClass
//                    );
                    optionalEnclosingInstance = new QualifiedThisReference(
                        nci.getLocation(), // location
                        new SimpleType(    // qualification
                            nci.getLocation(),
                            optionalOuterIClass
                        )
                    );
                    optionalEnclosingInstance.setEnclosingBlockStatement(nci.getEnclosingBlockStatement());
                }
            }
        }

        this.invokeConstructor(
            nci,                  // l
            nci.getEnclosingBlockStatement(), // scope
            optionalEnclosingInstance,        // optionalEnclosingInstance
            nci.iClass,                       // targetClass
            nci.arguments                     // arguments
        );
        return nci.iClass;
    }

    private IClass
    compileGet2(NewAnonymousClassInstance naci) throws CompileException {

        // Find constructors.
        AnonymousClassDeclaration acd           = naci.anonymousClassDeclaration;
        IClass                    sc            = this.resolve(acd).getSuperclass();
        IClass.IConstructor[]     iConstructors = sc.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new JaninoRuntimeException("SNO: Base class has no constructors");

        // Determine most specific constructor.
        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            naci, // l
            iConstructors,    // iInvocables
            naci.arguments,   // arguments
            acd               // contextScope
        );

        IClass[] pts = iConstructor.getParameterTypes();

        // Determine formal parameters of anonymous constructor.
        FunctionDeclarator.FormalParameter[] fps;
        Location                             loc = naci.getLocation();
        {
            List l = new ArrayList(); // FormalParameter

            // Pass the enclosing instance of the base class as parameter #1.
            if (naci.optionalQualification != null) l.add(new FunctionDeclarator.FormalParameter(
                loc,                                                           // location
                true,                                                          // finaL
                new SimpleType(loc, this.getType(naci.optionalQualification)), // type
                "this$base"                                                    // name
            ));
            for (int i = 0; i < pts.length; ++i) l.add(new FunctionDeclarator.FormalParameter(
                loc,                         // location
                true,                        // finaL
                new SimpleType(loc, pts[i]), // type
                "p" + i                      // name
            ));
            fps = (FunctionDeclarator.FormalParameter[]) l.toArray(
                new FunctionDeclarator.FormalParameter[l.size()]
            );
        }

        // Determine thrown exceptions of anonymous constructor.
        IClass[] tes  = iConstructor.getThrownExceptions();
        Type[]   tets = new Type[tes.length];
        for (int i = 0; i < tes.length; ++i) tets[i] = new SimpleType(loc, tes[i]);

        // The anonymous constructor merely invokes the constructor of its superclass.
        int    j = 0;
        Rvalue optionalQualificationAccess;
        if (naci.optionalQualification == null) {
            optionalQualificationAccess = null;
        } else
        {
            optionalQualificationAccess = new ParameterAccess(loc, fps[j++]);
        }
        Rvalue[] parameterAccesses = new Rvalue[pts.length];
        for (int i = 0; i < pts.length; ++i) {
            parameterAccesses[i] = new ParameterAccess(loc, fps[j++]);
        }

        // Generate the anonymous constructor for the anonymous class (JLS 15.9.5.1).
        ConstructorDeclarator anonymousConstructor = new ConstructorDeclarator(
            loc,                                           // location
            null,                                          // optionalDocComment
            new Java.ModifiersAndAnnotations(Mod.PACKAGE), // modifiersAndAnnotations
            fps,                                           // formalParameters
            tets,                                          // thrownExceptions
            new SuperConstructorInvocation(                // optionalConstructorInvocation
                loc,                         // location
                optionalQualificationAccess, // optionalQualification
                parameterAccesses            // arguments
            ),
            Collections.EMPTY_LIST                         // optionalStatements
        );

        // Compile the anonymous class.
        acd.addConstructor(anonymousConstructor);
        try {
            this.compile(acd);

            // Instantiate the anonymous class.
            this.writeOpcode(naci, Opcode.NEW);
            this.writeConstantClassInfo(this.resolve(naci.anonymousClassDeclaration).getDescriptor());

            // Invoke the anonymous constructor.
            this.writeOpcode(naci, Opcode.DUP);
            Rvalue[] arguments2;
            if (naci.optionalQualification == null) {
                arguments2 = naci.arguments;
            } else {
                arguments2    = new Rvalue[naci.arguments.length + 1];
                arguments2[0] = naci.optionalQualification;
                System.arraycopy(naci.arguments, 0, arguments2, 1, naci.arguments.length);
            }

            // Notice: The enclosing instance of the anonymous class is "this", not the
            // qualification of the NewAnonymousClassInstance.
            Scope s;
            for (
                s = naci.getEnclosingBlockStatement();
                !(s instanceof TypeBodyDeclaration);
                s = s.getEnclosingScope()
            );
            ThisReference oei;
            if (((TypeBodyDeclaration) s).isStatic()) {
                oei = null;
            } else
            {
                oei = new ThisReference(loc);
                oei.setEnclosingBlockStatement(naci.getEnclosingBlockStatement());
            }
            this.invokeConstructor(
                naci,                             // l
                naci.getEnclosingBlockStatement(),    // scope
                oei,                                          // optionalEnclosingInstance
                this.resolve(naci.anonymousClassDeclaration), // targetClass
                arguments2                                    // arguments
            );
        } finally {

            // Remove the synthetic constructor that was temporarily added. This is necessary because this NACI
            // expression (and all other expressions) are sometimes compiled more than once (see "fakeCompile()"),
            // and we'd end up with TWO synthetic constructors. See JANINO-143.
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
        for (int i = 0; i < na.dimExprs.length; ++i) {
            IClass dimType = this.compileGetValue(na.dimExprs[i]);
            if (dimType != IClass.INT && this.unaryNumericPromotion(
                na, // l
                dimType         // type
            ) != IClass.INT) this.compileError("Invalid array size expression type", na.getLocation());
        }

        return this.newArray(
            na,       // l
            na.dimExprs.length,   // dimExprCount
            na.dims,              // dims
            this.getType(na.type) // componentType
        );
    }
    private IClass
    compileGet2(NewInitializedArray nia) throws CompileException {
        IClass at = this.getType(nia.arrayType);
        this.compileGetValue(nia.arrayInitializer, at);
        return at;
    }
    private void
    compileGetValue(ArrayInitializer ai, IClass arrayType) throws CompileException {
        if (!arrayType.isArray()) {
            this.compileError("Array initializer not allowed for non-array type \"" + arrayType.toString() + "\"");
        }
        IClass ct = arrayType.getComponentType();

        this.pushConstant(ai, new Integer(ai.values.length));
        this.newArray(
            ai, // l
            1,              // dimExprCount,
            0,              // dims,
            ct              // componentType
        );

        for (int i = 0; i < ai.values.length; ++i) {
            this.writeOpcode(ai, Opcode.DUP);
            this.pushConstant(ai, new Integer(i));
            ArrayInitializerOrRvalue aiorv = ai.values[i];
            if (aiorv instanceof Rvalue) {
                Rvalue rv = (Rvalue) aiorv;
                this.assignmentConversion(
                    ai,           // l
                    this.compileGetValue(rv), // sourceType
                    ct,                       // targetType
                    this.getConstantValue(rv) // optionalConstantValue
                );
            } else
            if (aiorv instanceof ArrayInitializer) {
                this.compileGetValue((ArrayInitializer) aiorv, ct);
            } else
            {
                throw new JaninoRuntimeException(
                    "Unexpected array initializer or rvalue class " + aiorv.getClass().getName()
                );
            }
            this.writeOpcode(ai, Opcode.IASTORE + UnitCompiler.ilfdabcs(ct));
        }
    }
    private IClass
    compileGet2(Literal l) throws CompileException {
        return this.pushConstant(l, this.getConstantValue(l));
    }

    /**
     * Convenience function that calls {@link #compileContext(Rvalue)} and {@link #compileGet(Rvalue)}.
     *
     * @return The type of the Rvalue
     */
    private IClass
    compileGetValue(Rvalue rv) throws CompileException {
        Object cv = this.getConstantValue(rv);
        if (cv != NOT_CONSTANT) {
            this.fakeCompile(rv); // To check that, e.g., "a" compiles in "true || a".
            this.pushConstant(rv, cv);
            return this.getType(rv);
        }

        this.compileContext(rv);
        return this.compileGet(rv);
    }

    // -------------------- Rvalue.getConstantValue() -----------------

    /**
     * Special return value for the {@link #getConstantValue(Java.Rvalue)} method family indicating that the given
     * {@link Rvalue} does not evaluate to a constant value.
     */
    public static final Object NOT_CONSTANT = IClass.NOT_CONSTANT;

    /**
     * Attempts to evaluate as a constant expression.
     *
     * @return {@link #NOT_CONSTANT} iff the rvalue is not a constant value
     */
    public final Object
    getConstantValue(Rvalue rv) throws CompileException {
        if (rv.constantValue != Rvalue.CONSTANT_VALUE_UNKNOWN) return rv.constantValue;

        final Object[] res = new Object[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                {       res[0] = UnitCompiler.this.getConstantValue2(al);                                                     }
            @Override public void visitAssignment(Assignment a)                                   {       res[0] = UnitCompiler.this.getConstantValue2(a);                                                      }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { try { res[0] = UnitCompiler.this.getConstantValue2(uo);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { try { res[0] = UnitCompiler.this.getConstantValue2(bo);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                               { try { res[0] = UnitCompiler.this.getConstantValue2(c);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                              {       res[0] = UnitCompiler.this.getConstantValue2(cl);                                                     }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { try { res[0] = UnitCompiler.this.getConstantValue2(ce);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                         {       res[0] = UnitCompiler.this.getConstantValue2(c);                                                      }
            @Override public void visitInstanceof(Instanceof io)                                  {       res[0] = UnitCompiler.this.getConstantValue2(io);                                                     }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      {       res[0] = UnitCompiler.this.getConstantValue2(mi);                                                     }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) {       res[0] = UnitCompiler.this.getConstantValue2(smi);                                                    }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          { try { res[0] = UnitCompiler.this.getConstantValue2(il);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             { try { res[0] = UnitCompiler.this.getConstantValue2(fpl); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          {       res[0] = UnitCompiler.this.getConstantValue2(bl);                                                     }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      {       res[0] = UnitCompiler.this.getConstantValue2(cl);                                                     }
            @Override public void visitStringLiteral(StringLiteral sl)                            {       res[0] = UnitCompiler.this.getConstantValue2(sl);                                                     }
            @Override public void visitNullLiteral(NullLiteral nl)                                {       res[0] = UnitCompiler.this.getConstantValue2(nl);                                                     }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  {       res[0] = UnitCompiler.this.getConstantValue2(naci);                                                   }
            @Override public void visitNewArray(NewArray na)                                      {       res[0] = UnitCompiler.this.getConstantValue2(na);                                                     }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               {       res[0] = UnitCompiler.this.getConstantValue2(nia);                                                    }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     {       res[0] = UnitCompiler.this.getConstantValue2(nci);                                                    }
            @Override public void visitParameterAccess(ParameterAccess pa)                        {       res[0] = UnitCompiler.this.getConstantValue2(pa);                                                     }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         {       res[0] = UnitCompiler.this.getConstantValue2(qtr);                                                    }
            @Override public void visitThisReference(ThisReference tr)                            {       res[0] = UnitCompiler.this.getConstantValue2(tr);                                                     }

            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.getConstantValue2(an); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       {       res[0] = UnitCompiler.this.getConstantValue2(aae);                                                   }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { res[0] = UnitCompiler.this.getConstantValue2(fa); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       {       res[0] = UnitCompiler.this.getConstantValue2(fae);                                                   }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) {       res[0] = UnitCompiler.this.getConstantValue2(scfae);                                                 }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       res[0] = UnitCompiler.this.getConstantValue2(lva);                                                   }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { res[0] = UnitCompiler.this.getConstantValue2(pe); } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
            rv.constantValue = res[0];
            return rv.constantValue;
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(Rvalue rv) { return NOT_CONSTANT; }

    private Object
    getConstantValue2(AmbiguousName an) throws CompileException {
        return this.getConstantValue(this.toRvalueOrCompileException(this.reclassify(an)));
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(FieldAccess fa) throws CompileException {
        return fa.field.getConstantValue();
    }

    private Object
    getConstantValue2(UnaryOperation uo) throws CompileException {
        if (uo.operator.equals("+")) return this.getConstantValue(uo.operand);
        if (uo.operator.equals("-")) return this.getNegatedConstantValue(uo.operand);
        if (uo.operator.equals("!")) {
            Object cv = this.getConstantValue(uo.operand);
            return (
                cv == Boolean.TRUE ? Boolean.FALSE :
                cv == Boolean.FALSE ? Boolean.TRUE :
                NOT_CONSTANT
            );
        }
        return NOT_CONSTANT;
    }

    private Object
    getConstantValue2(ConditionalExpression ce) throws CompileException {
        Object lhsValue = this.getConstantValue(ce.lhs);
        if (lhsValue instanceof Boolean) {
            return (
                ((Boolean) lhsValue).booleanValue()
                ? this.getConstantValue(ce.mhs)
                : this.getConstantValue(ce.rhs)
            );
        }
        return NOT_CONSTANT;
    }

    private Object
    getConstantValue2(BinaryOperation bo) throws CompileException {

        // "|", "^", "&", "*", "/", "%", "+", "-", "==", "!=".
        if (
            // CHECKSTYLE StringLiteralEquality:OFF
            bo.op == "|"
            || bo.op == "^"
            || bo.op == "&"
            || bo.op == "*"
            || bo.op == "/"
            || bo.op == "%"
            || bo.op == "+"
            || bo.op == "-"
            || bo.op == "=="
            || bo.op == "!="
            // CHECKSTYLE StringLiteralEquality:ON
        ) {

            // Unroll the constant operands.
            List cvs = new ArrayList();
            for (Iterator it = bo.unrollLeftAssociation(); it.hasNext();) {
                Object cv = this.getConstantValue(((Rvalue) it.next()));
                if (cv == NOT_CONSTANT) return NOT_CONSTANT;
                cvs.add(cv);
            }

            // Compute the constant value of the unrolled binary operation.
            Iterator it  = cvs.iterator();
            Object   lhs = it.next();
            while (it.hasNext()) {
                if (lhs == NOT_CONSTANT) return NOT_CONSTANT;

                Object rhs = it.next();

                // String concatenation?
                // SUPPRESS CHECKSTYLE StringLiteralEquality
                if (bo.op == "+" && (lhs instanceof String || rhs instanceof String)) {
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
                                // CHECKSTYLE StringLiteralEquality:OFF
                                bo.op == "*" ? new Double(lhsD * rhsD) :
                                bo.op == "/" ? new Double(lhsD / rhsD) :
                                bo.op == "%" ? new Double(lhsD % rhsD) :
                                bo.op == "+" ? new Double(lhsD + rhsD) :
                                bo.op == "-" ? new Double(lhsD - rhsD) :
                                bo.op == "==" ? Boolean.valueOf(lhsD == rhsD) :
                                bo.op == "!=" ? Boolean.valueOf(lhsD != rhsD) :
                                NOT_CONSTANT
                                // CHECKSTYLE StringLiteralEquality:ON
                            );
                            continue;
                        }
                        if (lhs instanceof Float || rhs instanceof Float) {
                            float lhsF = ((Number) lhs).floatValue();
                            float rhsF = ((Number) rhs).floatValue();
                            lhs = (
                                // CHECKSTYLE StringLiteralEquality:OFF
                                bo.op == "*" ? new Float(lhsF * rhsF) :
                                bo.op == "/" ? new Float(lhsF / rhsF) :
                                bo.op == "%" ? new Float(lhsF % rhsF) :
                                bo.op == "+" ? new Float(lhsF + rhsF) :
                                bo.op == "-" ? new Float(lhsF - rhsF) :
                                bo.op == "==" ? Boolean.valueOf(lhsF == rhsF) :
                                bo.op == "!=" ? Boolean.valueOf(lhsF != rhsF) :
                                NOT_CONSTANT
                                // CHECKSTYLE StringLiteralEquality:ON
                            );
                            continue;
                        }
                        if (lhs instanceof Long || rhs instanceof Long) {
                            long lhsL = ((Number) lhs).longValue();
                            long rhsL = ((Number) rhs).longValue();
                            lhs = (
                                // CHECKSTYLE StringLiteralEquality:OFF
                                bo.op == "|" ? new Long(lhsL | rhsL) :
                                bo.op == "^" ? new Long(lhsL ^ rhsL) :
                                bo.op == "&" ? new Long(lhsL & rhsL) :
                                bo.op == "*" ? new Long(lhsL * rhsL) :
                                bo.op == "/" ? new Long(lhsL / rhsL) :
                                bo.op == "%" ? new Long(lhsL % rhsL) :
                                bo.op == "+" ? new Long(lhsL + rhsL) :
                                bo.op == "-" ? new Long(lhsL - rhsL) :
                                bo.op == "==" ? Boolean.valueOf(lhsL == rhsL) :
                                bo.op == "!=" ? Boolean.valueOf(lhsL != rhsL) :
                                NOT_CONSTANT
                                // CHECKSTYLE StringLiteralEquality:ON
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
                                // CHECKSTYLE StringLiteralEquality:OFF
                                bo.op == "|" ? new Integer(lhsI | rhsI) :
                                bo.op == "^" ? new Integer(lhsI ^ rhsI) :
                                bo.op == "&" ? new Integer(lhsI & rhsI) :
                                bo.op == "*" ? new Integer(lhsI * rhsI) :
                                bo.op == "/" ? new Integer(lhsI / rhsI) :
                                bo.op == "%" ? new Integer(lhsI % rhsI) :
                                bo.op == "+" ? new Integer(lhsI + rhsI) :
                                bo.op == "-" ? new Integer(lhsI - rhsI) :
                                bo.op == "==" ? Boolean.valueOf(lhsI == rhsI) :
                                bo.op == "!=" ? Boolean.valueOf(lhsI != rhsI) :
                                NOT_CONSTANT
                                // CHECKSTYLE StringLiteralEquality:ON
                            );
                            continue;
                        }
                    } catch (ArithmeticException ae) {
                        // Most likely a divide by zero or modulo by zero.
                        // Guess we can't make this expression into a constant.
                        return NOT_CONSTANT;
                    }
                    throw new IllegalStateException();
                }

                if (lhs instanceof Character && rhs instanceof Character) {
                    char lhsC = ((Character) lhs).charValue();
                    char rhsC = ((Character) rhs).charValue();
                    lhs = (
                        bo.op == "==" ? Boolean.valueOf(lhsC == rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        bo.op == "!=" ? Boolean.valueOf(lhsC != rhsC) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        NOT_CONSTANT
                    );
                    continue;
                }

                if (lhs == null || rhs == null) {
                    lhs = (
                        bo.op == "==" ? Boolean.valueOf(lhs == rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        bo.op == "!=" ? Boolean.valueOf(lhs != rhs) : // SUPPRESS CHECKSTYLE StringLiteralEquality
                        NOT_CONSTANT
                    );
                    continue;
                }

                return NOT_CONSTANT;
            }
            return lhs;
        }

        // "&&" and "||" with constant LHS operand.
        if (bo.op == "&&" || bo.op == "||") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            Object lhsValue = this.getConstantValue(bo.lhs);
            if (lhsValue instanceof Boolean) {
                boolean lhsBv = ((Boolean) lhsValue).booleanValue();
                return (
                    bo.op == "&&" // SUPPRESS CHECKSTYLE StringLiteralEquality
                    ? (lhsBv ? this.getConstantValue(bo.rhs) : Boolean.FALSE)
                    : (lhsBv ? Boolean.TRUE : this.getConstantValue(bo.rhs))
                );
            }
        }

        return NOT_CONSTANT;
    }

    private Object
    getConstantValue2(Cast c) throws CompileException {
        Object cv = this.getConstantValue(c.value);
        if (cv == NOT_CONSTANT) return NOT_CONSTANT;

        if (cv instanceof Number) {
            IClass tt = this.getType(c.targetType);
            if (tt == IClass.BYTE)   return new Byte(((Number) cv).byteValue());
            if (tt == IClass.SHORT)  return new Short(((Number) cv).shortValue());
            if (tt == IClass.INT)    return new Integer(((Number) cv).intValue());
            if (tt == IClass.LONG)   return new Long(((Number) cv).longValue());
            if (tt == IClass.FLOAT)  return new Float(((Number) cv).floatValue());
            if (tt == IClass.DOUBLE) return new Double(((Number) cv).doubleValue());
        }

        return NOT_CONSTANT;
    }

    private Object
    getConstantValue2(ParenthesizedExpression pe) throws CompileException {
        return this.getConstantValue(pe.value);
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(IntegerLiteral il) throws CompileException {
        String v = il.value;
        if (v.startsWith("0x")) {
            return (
                v.endsWith("L") || v.endsWith("l")
                ? (Object) Long.valueOf(hex2Long(il, v.substring(2, v.length() - 1)))
                : Integer.valueOf(hex2Int(il, v.substring(2)))
            );
        }
        if (v.startsWith("0")) {
            return (
                v.endsWith("L") || v.endsWith("l")
                ? (Object) Long.valueOf(oct2Long(il, v.substring(0, v.length() - 1)))
                : Integer.valueOf(oct2Int(il, v))
            );
        }
        try {
            return (
                v.endsWith("L") || v.endsWith("l")
                ? (Object) new Long(v.substring(0, v.length() - 1))
                : new Integer(v)
            );
        } catch (NumberFormatException e) {
            // SUPPRESS CHECKSTYLE AvoidHidingCause
            throw compileException(il, "Value of decimal integer literal '" + v + "' is out of range");
        }
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(FloatingPointLiteral fpl) throws CompileException {
        String v        = fpl.value;
        char   lastChar = v.charAt(v.length() - 1);
        if (lastChar == 'f' || lastChar == 'F') {
            v = v.substring(0, v.length() - 1);

            float fv;
            try {
                fv = Float.parseFloat(v);
            } catch (NumberFormatException e) {
                throw new JaninoRuntimeException("SNO: parsing float literal '" + v + "': " + e.getMessage(), e);
            }
            if (Float.isInfinite(fv)) {
                throw compileException(fpl, "Value of float literal '" + v + "' is out of range");
            }
            if (Float.isNaN(fv)) {
                throw new JaninoRuntimeException("SNO: parsing float literal '" + v + "' results in NaN");
            }

            // Check for FLOAT underrun.
            if (fv == 0.0F) {
                for (int i = 0; i < v.length(); ++i) {
                    char c = v.charAt(i);
                    if ("123456789".indexOf(c) != -1) {
                        throw compileException(fpl,  "Literal '" + v + "' is too small to be represented as a float");
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
            throw new JaninoRuntimeException("SNO: parsing double literal '" + v + "': " + e.getMessage(), e);
        }
        if (Double.isInfinite(dv)) {
            throw compileException(fpl, "Value of double literal '" + v + "' is out of range");
        }
        if (Double.isNaN(dv)) {
            throw new JaninoRuntimeException("SNO: parsing double literal '" + v + "' results is NaN");
        }

        // Check for DOUBLE underrun.
        if (dv == 0.0F) {
            for (int i = 0; i < v.length(); ++i) {
                char c = v.charAt(i);
                if ("123456789".indexOf(c) != -1) {
                    throw compileException(fpl, "Literal '" + v + "' is too small to be represented as a double");
                }
                if (c != '0' && c != '.') break;
            }
        }

        return new Double(dv);
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(BooleanLiteral bl) {
        if (bl.value == "true")  return Boolean.TRUE;  // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (bl.value == "false") return Boolean.FALSE; // SUPPRESS CHECKSTYLE StringLiteralEquality
        throw new JaninoRuntimeException(bl.value);
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(CharacterLiteral cl) {
        String v = cl.value;
        return Character.valueOf(unescape(v.substring(1, v.length() - 1)).charAt(0));
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(StringLiteral sl) {
        String v = sl.value;
        return unescape(v.substring(1, v.length() - 1));
    }

    @SuppressWarnings("static-method") private Object
    getConstantValue2(NullLiteral nl) { return null; }

    /**
     * Attempts to evaluate the negated value of a constant {@link Rvalue}. This is particularly relevant for the
     * smallest value of an integer or long literal.
     *
     * @return {@link #NOT_CONSTANT} iff value is not constant; otherwise a {@link String}, {@link Byte}, {@link
     *         Short}, {@link Integer}, {@link Boolean}, {@link Character}, {@link Float}, {@link Long}, {@link Double}
     *         or {@code null}
     */
    private Object
    getNegatedConstantValue(Rvalue rv) throws CompileException {
        final Object[] res = new Object[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        RvalueVisitor rvv = new RvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitArrayLength(ArrayLength al)                                { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(al);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitAssignment(Assignment a)                                   { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(a);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(uo);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(bo);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                               { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(c);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                              { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(cl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(ce);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                         { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(c);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInstanceof(Instanceof io)                                  { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(io);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(mi);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(smi);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(il);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(fpl);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(bl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(cl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitStringLiteral(StringLiteral sl)                            { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(sl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNullLiteral(NullLiteral nl)                                { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(nl);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(naci); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewArray(NewArray na)                                      { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(na);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(nia);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(nci);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParameterAccess(ParameterAccess pa)                        { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(pa);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(qtr);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThisReference(ThisReference tr)                            { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(tr);   } catch (CompileException e) { throw new UCE(e); } }

            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(lva);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }
    private Object
    getNegatedConstantValue2(Rvalue rv) throws CompileException {
        Object cv = this.getConstantValue(rv);
        if (cv instanceof Byte)    return new Byte((byte) -((Byte) cv).byteValue());
        if (cv instanceof Short)   return new Short((short) -((Short) cv).shortValue());
        if (cv instanceof Integer) return new Integer(-((Integer) cv).intValue());
        if (cv instanceof Long)    return new Long(-((Long) cv).longValue());
        if (cv instanceof Float)   return new Float(-((Float) cv).floatValue());
        if (cv instanceof Double)  return new Double(-((Double) cv).doubleValue());
        return NOT_CONSTANT;
    }
    private Object
    getNegatedConstantValue2(UnaryOperation uo) throws CompileException {
        return (
            uo.operator.equals("+") ? this.getNegatedConstantValue(uo.operand) :
            uo.operator.equals("-") ? this.getConstantValue(uo.operand) :
            NOT_CONSTANT
        );
    }
    private Object
    getNegatedConstantValue2(ParenthesizedExpression pe) throws CompileException {
        return this.getNegatedConstantValue(pe.value);
    }
    private Object
    getNegatedConstantValue2(IntegerLiteral il) throws CompileException {
        String v = il.value;
        if ("2147483648".equals(v) || "020000000000".equals(v)) {
            return new Integer(Integer.MIN_VALUE);
        }
        char lastChar = v.charAt(v.length() - 1);
        if (lastChar == 'l' || lastChar == 'L') {
            String v2 = v.substring(0, v.length() - 1);
            if ("9223372036854775808".equals(v2) || "01000000000000000000000".equals(v2)) {
                return new Long(Long.MIN_VALUE);
            }
        }
        return this.getNegatedConstantValue2((Rvalue) il);
    }

    // ------------ BlockStatement.generatesCode() -------------

    /**
     * Check whether invocation of {@link #compile(BlockStatement)} would generate more than zero code bytes.
     */
    private boolean
    generatesCode(BlockStatement bs) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        BlockStatementVisitor bsv = new BlockStatementVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitInitializer(Initializer i)                                                { try { res[0] = UnitCompiler.this.generatesCode2(i);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldDeclaration(FieldDeclaration fd)                                     { try { res[0] = UnitCompiler.this.generatesCode2(fd); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLabeledStatement(LabeledStatement ls)                                     {       res[0] = UnitCompiler.this.generatesCode2(ls);                                                    }
            @Override public void visitBlock(Block b)                                                            { try { res[0] = UnitCompiler.this.generatesCode2(b);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitExpressionStatement(ExpressionStatement es)                               {       res[0] = UnitCompiler.this.generatesCode2(es);                                                    }
            @Override public void visitIfStatement(IfStatement is)                                               {       res[0] = UnitCompiler.this.generatesCode2(is);                                                    }
            @Override public void visitForStatement(ForStatement fs)                                             {       res[0] = UnitCompiler.this.generatesCode2(fs);                                                    }
            @Override public void visitWhileStatement(WhileStatement ws)                                         {       res[0] = UnitCompiler.this.generatesCode2(ws);                                                    }
            @Override public void visitTryStatement(TryStatement ts)                                             {       res[0] = UnitCompiler.this.generatesCode2(ts);                                                    }
            @Override public void visitSwitchStatement(SwitchStatement ss)                                       {       res[0] = UnitCompiler.this.generatesCode2(ss);                                                    }
            @Override public void visitSynchronizedStatement(SynchronizedStatement ss)                           {       res[0] = UnitCompiler.this.generatesCode2(ss);                                                    }
            @Override public void visitDoStatement(DoStatement ds)                                               {       res[0] = UnitCompiler.this.generatesCode2(ds);                                                    }
            @Override public void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) {       res[0] = UnitCompiler.this.generatesCode2(lvds);                                                  }
            @Override public void visitReturnStatement(ReturnStatement rs)                                       {       res[0] = UnitCompiler.this.generatesCode2(rs);                                                    }
            @Override public void visitThrowStatement(ThrowStatement ts)                                         {       res[0] = UnitCompiler.this.generatesCode2(ts);                                                    }
            @Override public void visitBreakStatement(BreakStatement bs)                                         {       res[0] = UnitCompiler.this.generatesCode2(bs);                                                    }
            @Override public void visitContinueStatement(ContinueStatement cs)                                   {       res[0] = UnitCompiler.this.generatesCode2(cs);                                                    }
            @Override public void visitAssertStatement(AssertStatement as)                                       {       res[0] = UnitCompiler.this.generatesCode2(as);                                                    }
            @Override public void visitEmptyStatement(EmptyStatement es)                                         {       res[0] = UnitCompiler.this.generatesCode2(es);                                                    }
            @Override public void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       {       res[0] = UnitCompiler.this.generatesCode2(lcds);                                                  }
            @Override public void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        {       res[0] = UnitCompiler.this.generatesCode2(aci);                                                   }
            @Override public void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                {       res[0] = UnitCompiler.this.generatesCode2(sci);                                                   }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    public boolean generatesCode2(BlockStatement bs)                     { return true; }
    public boolean generatesCode2(AssertStatement as)                    { return true; }
    public boolean generatesCode2(EmptyStatement es)                     { return false; }
    public boolean generatesCode2(LocalClassDeclarationStatement lcds)   { return false; }
    public boolean generatesCode2(Initializer i) throws CompileException { return this.generatesCode(i.block); }

    public boolean
    generatesCode2ListStatements(List/*<BlockStatement>*/ l) throws CompileException {
        for (int i = 0; i < l.size(); ++i) {
            if (this.generatesCode(((BlockStatement) l.get(i)))) return true;
        }
        return false;
    }

    public boolean
    generatesCode2(Block b) throws CompileException {
        return this.generatesCode2ListStatements(b.statements);
    }

    public boolean
    generatesCode2(FieldDeclaration fd) throws CompileException {
        // Code is only generated if at least one of the declared variables has a non-constant-final initializer.
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            VariableDeclarator vd = fd.variableDeclarators[i];
            if (this.getNonConstantFinalInitializer(fd, vd) != null) return true;
        }
        return false;
    }

    // ------------ BlockStatement.leave() -------------

    /**
     * Clean up the statement context. This is currently relevant for "try ... catch ... finally" statements (execute
     * "finally" clause) and "synchronized" statements (monitorexit).
     * <p>
     * Statements like "return", "break", "continue" must call this method for all the statements they terminate.
     * <p>
     * Notice: If {@code optionalStackValueType} is {@code null}, then the operand stack is empty; otherwise
     * exactly one operand with that type is on the stack. This information is vital to implementations of {@link
     * #leave(BlockStatement, IClass)} that require a specific operand stack state (e.g. an empty operand stack for
     * JSR).
     */
    private void
    leave(BlockStatement bs, final IClass optionalStackValueType) {
        BlockStatementVisitor bsv = new BlockStatementVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitInitializer(Initializer i)                                                { UnitCompiler.this.leave2(i,    optionalStackValueType); }
            @Override public void visitFieldDeclaration(FieldDeclaration fd)                                     { UnitCompiler.this.leave2(fd,   optionalStackValueType); }
            @Override public void visitLabeledStatement(LabeledStatement ls)                                     { UnitCompiler.this.leave2(ls,   optionalStackValueType); }
            @Override public void visitBlock(Block b)                                                            { UnitCompiler.this.leave2(b,    optionalStackValueType); }
            @Override public void visitExpressionStatement(ExpressionStatement es)                               { UnitCompiler.this.leave2(es,   optionalStackValueType); }
            @Override public void visitIfStatement(IfStatement is)                                               { UnitCompiler.this.leave2(is,   optionalStackValueType); }
            @Override public void visitForStatement(ForStatement fs)                                             { UnitCompiler.this.leave2(fs,   optionalStackValueType); }
            @Override public void visitWhileStatement(WhileStatement ws)                                         { UnitCompiler.this.leave2(ws,   optionalStackValueType); }
            @Override public void visitTryStatement(TryStatement ts)                                             { UnitCompiler.this.leave2(ts,   optionalStackValueType); }
            @Override public void visitSwitchStatement(SwitchStatement ss)                                       { UnitCompiler.this.leave2(ss,   optionalStackValueType); }
            @Override public void visitSynchronizedStatement(SynchronizedStatement ss)                           { UnitCompiler.this.leave2(ss,   optionalStackValueType); }
            @Override public void visitDoStatement(DoStatement ds)                                               { UnitCompiler.this.leave2(ds,   optionalStackValueType); }
            @Override public void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { UnitCompiler.this.leave2(lvds, optionalStackValueType); }
            @Override public void visitReturnStatement(ReturnStatement rs)                                       { UnitCompiler.this.leave2(rs,   optionalStackValueType); }
            @Override public void visitThrowStatement(ThrowStatement ts)                                         { UnitCompiler.this.leave2(ts,   optionalStackValueType); }
            @Override public void visitBreakStatement(BreakStatement bs)                                         { UnitCompiler.this.leave2(bs,   optionalStackValueType); }
            @Override public void visitContinueStatement(ContinueStatement cs)                                   { UnitCompiler.this.leave2(cs,   optionalStackValueType); }
            @Override public void visitAssertStatement(AssertStatement as)                                       { UnitCompiler.this.leave2(as,   optionalStackValueType); }
            @Override public void visitEmptyStatement(EmptyStatement es)                                         { UnitCompiler.this.leave2(es,   optionalStackValueType); }
            @Override public void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds)       { UnitCompiler.this.leave2(lcds, optionalStackValueType); }
            @Override public void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci)        { UnitCompiler.this.leave2(aci,  optionalStackValueType); }
            @Override public void visitSuperConstructorInvocation(SuperConstructorInvocation sci)                { UnitCompiler.this.leave2(sci,  optionalStackValueType); }
            // CHECKSTYLE LineLengthCheck:ON
        };
        bs.accept(bsv);
    }

    public void
    leave2(BlockStatement bs, IClass optionalStackValueType) {}

    public void
    leave2(SynchronizedStatement ss, IClass optionalStackValueType) {
        this.load(ss, this.iClassLoader.JAVA_LANG_OBJECT, ss.monitorLvIndex);
        this.writeOpcode(ss, Opcode.MONITOREXIT);
    }

    public void
    leave2(TryStatement ts, IClass optionalStackValueType) {
        if (ts.finallyOffset != null) {

            this.codeContext.saveLocalVariables();
            try {
                short sv = 0;

                // Obviously, JSR must always be executed with the operand stack being empty; otherwise we get
                // "java.lang.VerifyError: Inconsistent stack height 1 != 2"
                if (optionalStackValueType != null) {
                    sv = this.codeContext.allocateLocalVariable(
                        Descriptor.size(optionalStackValueType.getDescriptor())
                    );
                    this.store(ts, optionalStackValueType, sv);
                }

                this.writeBranch(ts, Opcode.JSR, ts.finallyOffset);

                if (optionalStackValueType != null) {
                    this.load(ts, optionalStackValueType, sv);
                }
            } finally {
                this.codeContext.restoreLocalVariables();
            }
        }
    }

    // ---------------- Lvalue.compileSet() -----------------

    /**
     * Generates code that stores a value in the {@link Lvalue}. Expects the {@link Lvalue}'s context (see {@link
     * #compileContext}) and a value of the {@link Lvalue}'s type on the operand stack.
     */
    private void
    compileSet(Lvalue lv) throws CompileException {

        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }

        LvalueVisitor lvv = new LvalueVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { UnitCompiler.this.compileSet2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { UnitCompiler.this.compileSet2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { UnitCompiler.this.compileSet2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { UnitCompiler.this.compileSet2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compileSet2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       UnitCompiler.this.compileSet2(lva);                                                      }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { UnitCompiler.this.compileSet2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            lv.accept(lvv);
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }
    private void
    compileSet2(AmbiguousName an) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(this.reclassify(an)));
    }

    private void
    compileSet2(LocalVariableAccess lva) {
        this.store(lva, lva.localVariable.type, lva.localVariable);
    }

    private void
    compileSet2(FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.getEnclosingBlockStatement());
        this.writeOpcode(fa, fa.field.isStatic() ? Opcode.PUTSTATIC : Opcode.PUTFIELD);
        this.writeConstantFieldrefInfo(
            fa.field.getDeclaringIClass().getDescriptor(), // classFD
            fa.field.getName(),                            // fieldName
            fa.field.getDescriptor()                       // fieldFD
        );
    }
    private void
    compileSet2(ArrayAccessExpression aae) throws CompileException {
        this.writeOpcode(aae, Opcode.IASTORE + UnitCompiler.ilfdabcs(this.getType(aae)));
    }
    private void
    compileSet2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        this.compileSet(this.toLvalueOrCompileException(fae.value));
    }
    private void
    compileSet2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        this.compileSet(this.toLvalueOrCompileException(scfae.value));
    }
    private void
    compileSet2(ParenthesizedExpression pe) throws CompileException {
        this.compileSet(this.toLvalueOrCompileException(pe.value));
    }

    // ---------------- Atom.getType() ----------------

    private IClass
    getType(Atom a) throws CompileException {
        final IClass[] res = new IClass[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        AtomVisitor av = new AtomVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            // AtomVisitor
            @Override public void visitPackage(Package p) { try { res[0] = UnitCompiler.this.getType2(p); } catch (CompileException e) { throw new UCE(e); } }
            // TypeVisitor
            @Override public void visitArrayType(ArrayType at)                { try { res[0] = UnitCompiler.this.getType2(at);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBasicType(BasicType bt)                {       res[0] = UnitCompiler.this.getType2(bt);                                                     }
            @Override public void visitReferenceType(ReferenceType rt)        { try { res[0] = UnitCompiler.this.getType2(rt);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitRvalueMemberType(RvalueMemberType rmt) { try { res[0] = UnitCompiler.this.getType2(rmt); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSimpleType(SimpleType st)              {       res[0] = UnitCompiler.this.getType2(st);                                                     }
            // RvalueVisitor
            @Override public void visitArrayLength(ArrayLength al)                                {       res[0] = UnitCompiler.this.getType2(al);                                                     }
            @Override public void visitAssignment(Assignment a)                                   { try { res[0] = UnitCompiler.this.getType2(a);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { try { res[0] = UnitCompiler.this.getType2(uo);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { try { res[0] = UnitCompiler.this.getType2(bo);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCast(Cast c)                                               { try { res[0] = UnitCompiler.this.getType2(c);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitClassLiteral(ClassLiteral cl)                              {       res[0] = UnitCompiler.this.getType2(cl);                                                     }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { try { res[0] = UnitCompiler.this.getType2(ce);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitCrement(Crement c)                                         { try { res[0] = UnitCompiler.this.getType2(c);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitInstanceof(Instanceof io)                                  {       res[0] = UnitCompiler.this.getType2(io);                                                     }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      { try { res[0] = UnitCompiler.this.getType2(mi);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) { try { res[0] = UnitCompiler.this.getType2(smi); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          {       res[0] = UnitCompiler.this.getType2(il);                                                   }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             {       res[0] = UnitCompiler.this.getType2(fpl);                                                  }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          {       res[0] = UnitCompiler.this.getType2(bl);                                                   }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      {       res[0] = UnitCompiler.this.getType2(cl);                                                   }
            @Override public void visitStringLiteral(StringLiteral sl)                            {       res[0] = UnitCompiler.this.getType2(sl);                                                   }
            @Override public void visitNullLiteral(NullLiteral nl)                                {       res[0] = UnitCompiler.this.getType2(nl);                                                   }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  {       res[0] = UnitCompiler.this.getType2(naci);                                                   }
            @Override public void visitNewArray(NewArray na)                                      { try { res[0] = UnitCompiler.this.getType2(na);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               { try { res[0] = UnitCompiler.this.getType2(nia); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     { try { res[0] = UnitCompiler.this.getType2(nci); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitParameterAccess(ParameterAccess pa)                        { try { res[0] = UnitCompiler.this.getType2(pa);  } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         { try { res[0] = UnitCompiler.this.getType2(qtr); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitThisReference(ThisReference tr)                            { try { res[0] = UnitCompiler.this.getType2(tr);  } catch (CompileException e) { throw new UCE(e); } }
            // LvalueVisitor
            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.getType2(an);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       { try { res[0] = UnitCompiler.this.getType2(aae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccess(FieldAccess fa)                                            { try { res[0] = UnitCompiler.this.getType2(fa);    } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       { try { res[0] = UnitCompiler.this.getType2(fae);   } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.getType2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       res[0] = UnitCompiler.this.getType2(lva);                                                      }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    { try { res[0] = UnitCompiler.this.getType2(pe);    } catch (CompileException e) { throw new UCE(e); } }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            a.accept(av);
            return res[0] != null ? res[0] : this.iClassLoader.JAVA_LANG_OBJECT;
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    @SuppressWarnings("static-method") private IClass
    getType2(SimpleType st) { return st.iClass; }

    @SuppressWarnings("static-method") private IClass
    getType2(BasicType bt) {
        switch (bt.index) {
        case BasicType.VOID:    return IClass.VOID;
        case BasicType.BYTE:    return IClass.BYTE;
        case BasicType.SHORT:   return IClass.SHORT;
        case BasicType.CHAR:    return IClass.CHAR;
        case BasicType.INT:     return IClass.INT;
        case BasicType.LONG:    return IClass.LONG;
        case BasicType.FLOAT:   return IClass.FLOAT;
        case BasicType.DOUBLE:  return IClass.DOUBLE;
        case BasicType.BOOLEAN: return IClass.BOOLEAN;
        default: throw new JaninoRuntimeException("Invalid index " + bt.index);
        }
    }
    private IClass
    getType2(ReferenceType rt) throws CompileException {
        BlockStatement  scopeBlockStatement  = null;
        TypeDeclaration scopeTypeDeclaration = null;
        CompilationUnit scopeCompilationUnit;
        for (Scope s = rt.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof BlockStatement && scopeBlockStatement == null) {
                scopeBlockStatement = (BlockStatement) s;
            }
            if (s instanceof TypeDeclaration && scopeTypeDeclaration == null) {
                scopeTypeDeclaration = (TypeDeclaration) s;
            }
            if (s instanceof CompilationUnit) {
                scopeCompilationUnit = (CompilationUnit) s;
                break;
            }
        }

        if (rt.identifiers.length == 1) {

            // 6.5.5.1 Simple type name (single identifier).
            String simpleTypeName = rt.identifiers[0];

            // 6.5.5.1.1 Local class.
            {
                LocalClassDeclaration lcd = findLocalClassDeclaration(rt.getEnclosingScope(), simpleTypeName);
                if (lcd != null) return this.resolve(lcd);
            }

            // 6.5.5.1.2 Member type.
            if (scopeTypeDeclaration != null) { // If enclosed by another type declaration...
                for (
                    Scope s = scopeTypeDeclaration;
                    !(s instanceof CompilationUnit);
                    s = s.getEnclosingScope()
                ) {
                    if (s instanceof TypeDeclaration) {
                        IClass mt = this.findMemberType(
                            this.resolve((AbstractTypeDeclaration) s),
                            simpleTypeName,
                            rt.getLocation()
                        );
                        if (mt != null) return mt;
                    }
                }
            }

            {

                // 6.5.5.1.4a Single-type import.
                {
                    IClass importedClass = this.importSingleType(simpleTypeName, rt.getLocation());
                    if (importedClass != null) return importedClass;
                }

                // 6.5.5.1.4b Type declared in same compilation unit.
                {
                    PackageMemberTypeDeclaration pmtd = (
                        scopeCompilationUnit.getPackageMemberTypeDeclaration(simpleTypeName)
                    );
                    if (pmtd != null) return this.resolve(pmtd);
                }
            }

            // 6.5.5.1.5 Type declared in other compilation unit of same package.
            {
                String pkg = (
                    scopeCompilationUnit.optionalPackageDeclaration == null ? null :
                    scopeCompilationUnit.optionalPackageDeclaration.packageName
                );
                String className = pkg == null ? simpleTypeName : pkg + "." + simpleTypeName;
                IClass result    = findTypeByName(rt.getLocation(), className);
                if (result != null) return result;
            }

            // 6.5.5.1.6 Type-import-on-demand declaration.
            {
                IClass importedClass = this.importTypeOnDemand(simpleTypeName, rt.getLocation());
                if (importedClass != null) return importedClass;
            }

            // JLS3 ???:  Type imported through single static import.
            {
                List l = (List) this.singleStaticImports.get(simpleTypeName);
                if (l != null) {
                    IClass importedMemberType = null;
                    for (Iterator it = l.iterator(); it.hasNext();) {
                        Object o = it.next();
                        if (o instanceof IClass) {
                            IClass mt = (IClass) o;
                            if (!this.isAccessible(mt, scopeBlockStatement)) continue;
                            if (importedMemberType != null && importedMemberType != mt) {
                                this.compileError(
                                    "Ambiguous static imports: \""
                                    + importedMemberType.toString()
                                    + "\" vs. \""
                                    + mt.toString()
                                    + "\""
                                );
                            }
                            importedMemberType = mt;
                        }
                    }
                    if (importedMemberType != null) return importedMemberType;
                }
            }

            // JLS3 ???: Type imported through static-import-on-demand.
            {
                IClass importedMemberType = null;
                for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                    IClass   ic          = (IClass) it.next();
                    IClass[] memberTypes = ic.getDeclaredIClasses();
                    for (int i = 0; i < memberTypes.length; ++i) {
                        IClass mt = memberTypes[i];
                        if (!this.isAccessible(mt, scopeBlockStatement)) continue;
                        if (mt.getDescriptor().endsWith('$' + simpleTypeName + ';')) {
                            if (importedMemberType != null) {
                                this.compileError(
                                    "Ambiguous static imports: \""
                                    + importedMemberType.toString()
                                    + "\" vs. \""
                                    + mt.toString()
                                    + "\""
                                );
                            }
                            importedMemberType = mt;
                        }
                    }
                }
                if (importedMemberType != null) return importedMemberType;
            }

            // 6.5.5.1.8 Give up.
            this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"", rt.getLocation());
            return this.iClassLoader.JAVA_LANG_OBJECT;
        } else {

            // 6.5.5.2 Qualified type name (two or more identifiers).
            Atom q = this.reclassifyName(
                rt.getLocation(),
                rt.getEnclosingScope(),
                rt.identifiers,
                rt.identifiers.length - 1
            );

            // 6.5.5.2.1 PACKAGE.CLASS
            if (q instanceof Package) {
                String className = Java.join(rt.identifiers, ".");
                IClass result    = findTypeByName(rt.getLocation(), className);
                if (result != null) return result;

                this.compileError("Class \"" + className + "\" not found", rt.getLocation());
                return this.iClassLoader.JAVA_LANG_OBJECT;
            }

            // 6.5.5.2.2 CLASS.CLASS (member type)
            String   memberTypeName = rt.identifiers[rt.identifiers.length - 1];
            IClass[] types          = this.getType(this.toTypeOrCompileException(q)).findMemberType(memberTypeName);
            if (types.length == 1) return types[0];
            if (types.length == 0) {
                this.compileError("\"" + q + "\" declares no member type \"" + memberTypeName + "\"", rt.getLocation());
            } else
            {
                this.compileError(
                    "\"" + q + "\" and its supertypes declare more than one member type \"" + memberTypeName + "\"",
                    rt.getLocation()
                );
            }
            return this.iClassLoader.JAVA_LANG_OBJECT;
        }
    }

    private IClass
    getType2(RvalueMemberType rvmt) throws CompileException {
        IClass rvt        = this.getType(rvmt.rvalue);
        IClass memberType = this.findMemberType(rvt, rvmt.identifier, rvmt.getLocation());
        if (memberType == null) {
            this.compileError("\"" + rvt + "\" has no member type \"" + rvmt.identifier + "\"", rvmt.getLocation());
        }
        return memberType;
    }

    private IClass
    getType2(ArrayType at) throws CompileException {
        return this.getType(at.componentType).getArrayIClass(this.iClassLoader.JAVA_LANG_OBJECT);
    }

    private IClass
    getType2(AmbiguousName an) throws CompileException {
        return this.getType(this.reclassify(an));
    }

    private IClass
    getType2(Package p) throws CompileException {
        this.compileError("Unknown variable or type \"" + p.name + "\"", p.getLocation());
        return this.iClassLoader.JAVA_LANG_OBJECT;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(LocalVariableAccess lva) {
        return lva.localVariable.type;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(FieldAccess fa) throws CompileException {
        return fa.field.getType();
    }

    @SuppressWarnings("static-method") private IClass
    getType2(ArrayLength al) {
        return IClass.INT;
    }

    private IClass
    getType2(ThisReference tr) throws CompileException {
        return this.getIClass(tr);
    }

    private IClass
    getType2(QualifiedThisReference qtr) throws CompileException {
        return this.getTargetIClass(qtr);
    }

    private IClass
    getType2(ClassLiteral cl) {
        return this.iClassLoader.JAVA_LANG_CLASS;
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

            // JLS 15.25.1.1
            return mhsType;
        } else
        if (mhsType.isPrimitiveNumeric() && rhsType.isPrimitiveNumeric()) {

            // JLS 15.25.1.2

            // TODO JLS 15.25.1.2.1

            // TODO JLS 15.25.1.2.2

            // JLS 15.25.1.2.3
    
            return this.binaryNumericPromotionType(ce, mhsType, rhsType);
        } else
        if (this.getConstantValue(ce.mhs) == null && !rhsType.isPrimitive()) {

            // JLS 15.25.1.3 (null : reference)
            return rhsType;
        } else
        if (!mhsType.isPrimitive() && this.getConstantValue(ce.rhs) == null) {

            // JLS 15.25.1.3 (reference : null)
            return mhsType;
        } else
        if (!mhsType.isPrimitive() && !rhsType.isPrimitive()) {
            if (mhsType.isAssignableFrom(rhsType)) {
                return mhsType;
            } else
            if (rhsType.isAssignableFrom(mhsType)) {
                return rhsType;
            } else {
                this.compileError(
                    "Reference types \"" + mhsType + "\" and \"" + rhsType + "\" don't match",
                    ce.getLocation()
                );
                return this.iClassLoader.JAVA_LANG_OBJECT;
            }
        } else
        {
            this.compileError(
                "Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"",
                ce.getLocation()
            );
            return this.iClassLoader.JAVA_LANG_OBJECT;
        }
    }

    private IClass
    getType2(Crement c) throws CompileException {
        return this.getType(c.operand);
    }

    private IClass
    getType2(ArrayAccessExpression aae) throws CompileException {
        return this.getType(aae.lhs).getComponentType();
    }

    private IClass
    getType2(FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.getType(fae.value);
    }

    private IClass
    getType2(SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.getType(scfae.value);
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

    @SuppressWarnings("static-method") private IClass
    getType2(Instanceof io) { return IClass.BOOLEAN; }

    private IClass
    getType2(BinaryOperation bo) throws CompileException {
        if (
            // CHECKSTYLE StringLiteralEquality:OFF
            bo.op == "||"
            || bo.op == "&&"
            || bo.op == "=="
            || bo.op == "!="
            || bo.op == "<" 
            || bo.op == ">" 
            || bo.op == "<="
            || bo.op == ">="
            // CHECKSTYLE StringLiteralEquality:ON
        ) return IClass.BOOLEAN;

        if (bo.op == "|" || bo.op == "^" || bo.op == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IClass lhsType = this.getType(bo.lhs);
            return (
                lhsType == IClass.BOOLEAN || lhsType == this.iClassLoader.JAVA_LANG_BOOLEAN
                ? IClass.BOOLEAN
                : this.binaryNumericPromotionType(bo, lhsType, this.getType(bo.rhs))
            );
        }

        // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (bo.op == "*" || bo.op == "/" || bo.op == "%" || bo.op == "+" || bo.op == "-") {
            IClassLoader icl = this.iClassLoader;

            // Unroll the operands of this binary operation.
            Iterator ops = bo.unrollLeftAssociation();

            // Check the far left operand type.
            IClass lhsType = this.getUnboxedType(this.getType(((Rvalue) ops.next())));
            if (bo.op == "+" && lhsType == icl.JAVA_LANG_STRING) { // SUPPRESS CHECKSTYLE StringLiteralEquality
                return icl.JAVA_LANG_STRING;
            }

            // Determine the expression type.
            do {
                IClass rhsType = this.getUnboxedType(this.getType(((Rvalue) ops.next())));
                if (bo.op == "+" && rhsType == icl.JAVA_LANG_STRING) { // SUPPRESS CHECKSTYLE StringLiteralEquality
                    return icl.JAVA_LANG_STRING; 
                }
                lhsType = this.binaryNumericPromotionType(bo, lhsType, rhsType);
            } while (ops.hasNext());
            return lhsType;
        }

        if (bo.op == "<<"  || bo.op == ">>"  || bo.op == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            IClass lhsType = this.getType(bo.lhs);
            return this.unaryNumericPromotionType(bo, lhsType);
        }

        this.compileError("Unexpected operator \"" + bo.op + "\"", bo.getLocation());
        return this.iClassLoader.JAVA_LANG_OBJECT;
    }

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
        if (mi.iMethod == null) {
            mi.iMethod = this.findIMethod(mi);
        }
        return mi.iMethod.getReturnType();
    }

    private IClass
    getType2(SuperclassMethodInvocation scmi) throws CompileException {
        return this.findIMethod(scmi).getReturnType();
    }

    private IClass
    getType2(NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);
        return nci.iClass;
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
        return res.getArrayIClass(na.dimExprs.length + na.dims, this.iClassLoader.JAVA_LANG_OBJECT);
    }

    private IClass
    getType2(NewInitializedArray nia) throws CompileException {
        return this.getType(nia.arrayType);
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
        return this.iClassLoader.JAVA_LANG_STRING;
    }

    @SuppressWarnings("static-method") private IClass
    getType2(NullLiteral nl) {
        return IClass.VOID;
    }

    // ---------------- Atom.isType() ---------------

    private boolean
    isType(Atom a) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        AtomVisitor av = new AtomVisitor() {
            // CHECKSTYLE LineLengthCheck:OFF
            // AtomVisitor
            @Override public void visitPackage(Package p) { res[0] = UnitCompiler.this.isType2(p); }
            // TypeVisitor
            @Override public void visitArrayType(ArrayType at)                { res[0] = UnitCompiler.this.isType2(at);  }
            @Override public void visitBasicType(BasicType bt)                { res[0] = UnitCompiler.this.isType2(bt);  }
            @Override public void visitReferenceType(ReferenceType rt)        { res[0] = UnitCompiler.this.isType2(rt);  }
            @Override public void visitRvalueMemberType(RvalueMemberType rmt) { res[0] = UnitCompiler.this.isType2(rmt); }
            @Override public void visitSimpleType(SimpleType st)              { res[0] = UnitCompiler.this.isType2(st);  }
            // RvalueVisitor
            @Override public void visitArrayLength(ArrayLength al)                                { res[0] = UnitCompiler.this.isType2(al);   }
            @Override public void visitAssignment(Assignment a)                                   { res[0] = UnitCompiler.this.isType2(a);    }
            @Override public void visitUnaryOperation(UnaryOperation uo)                          { res[0] = UnitCompiler.this.isType2(uo);   }
            @Override public void visitBinaryOperation(BinaryOperation bo)                        { res[0] = UnitCompiler.this.isType2(bo);   }
            @Override public void visitCast(Cast c)                                               { res[0] = UnitCompiler.this.isType2(c);    }
            @Override public void visitClassLiteral(ClassLiteral cl)                              { res[0] = UnitCompiler.this.isType2(cl);   }
            @Override public void visitConditionalExpression(ConditionalExpression ce)            { res[0] = UnitCompiler.this.isType2(ce);   }
            @Override public void visitCrement(Crement c)                                         { res[0] = UnitCompiler.this.isType2(c);    }
            @Override public void visitInstanceof(Instanceof io)                                  { res[0] = UnitCompiler.this.isType2(io);   }
            @Override public void visitMethodInvocation(MethodInvocation mi)                      { res[0] = UnitCompiler.this.isType2(mi);   }
            @Override public void visitSuperclassMethodInvocation(SuperclassMethodInvocation smi) { res[0] = UnitCompiler.this.isType2(smi);  }
            @Override public void visitIntegerLiteral(IntegerLiteral il)                          { res[0] = UnitCompiler.this.isType2(il);   }
            @Override public void visitFloatingPointLiteral(FloatingPointLiteral fpl)             { res[0] = UnitCompiler.this.isType2(fpl);  }
            @Override public void visitBooleanLiteral(BooleanLiteral bl)                          { res[0] = UnitCompiler.this.isType2(bl);   }
            @Override public void visitCharacterLiteral(CharacterLiteral cl)                      { res[0] = UnitCompiler.this.isType2(cl);   }
            @Override public void visitStringLiteral(StringLiteral sl)                            { res[0] = UnitCompiler.this.isType2(sl);   }
            @Override public void visitNullLiteral(NullLiteral nl)                                { res[0] = UnitCompiler.this.isType2(nl);   }
            @Override public void visitNewAnonymousClassInstance(NewAnonymousClassInstance naci)  { res[0] = UnitCompiler.this.isType2(naci); }
            @Override public void visitNewArray(NewArray na)                                      { res[0] = UnitCompiler.this.isType2(na);   }
            @Override public void visitNewInitializedArray(NewInitializedArray nia)               { res[0] = UnitCompiler.this.isType2(nia);  }
            @Override public void visitNewClassInstance(NewClassInstance nci)                     { res[0] = UnitCompiler.this.isType2(nci);  }
            @Override public void visitParameterAccess(ParameterAccess pa)                        { res[0] = UnitCompiler.this.isType2(pa);   }
            @Override public void visitQualifiedThisReference(QualifiedThisReference qtr)         { res[0] = UnitCompiler.this.isType2(qtr);  }
            @Override public void visitThisReference(ThisReference tr)                            { res[0] = UnitCompiler.this.isType2(tr);   }
            // LvalueVisitor
            @Override public void visitAmbiguousName(AmbiguousName an)                                        { try { res[0] = UnitCompiler.this.isType2(an); } catch (CompileException e) { throw new UCE(e); } }
            @Override public void visitArrayAccessExpression(ArrayAccessExpression aae)                       {       res[0] = UnitCompiler.this.isType2(aae);                                                   }
            @Override public void visitFieldAccess(FieldAccess fa)                                            {       res[0] = UnitCompiler.this.isType2(fa);                                                    }
            @Override public void visitFieldAccessExpression(FieldAccessExpression fae)                       {       res[0] = UnitCompiler.this.isType2(fae);                                                   }
            @Override public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) {       res[0] = UnitCompiler.this.isType2(scfae);                                                 }
            @Override public void visitLocalVariableAccess(LocalVariableAccess lva)                           {       res[0] = UnitCompiler.this.isType2(lva);                                                   }
            @Override public void visitParenthesizedExpression(ParenthesizedExpression pe)                    {       res[0] = UnitCompiler.this.isType2(pe);                                                    }
            // CHECKSTYLE LineLengthCheck:ON
        };
        try {
            a.accept(av);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce; // SUPPRESS CHECKSTYLE AvoidHidingCause
        }
    }

    @SuppressWarnings("static-method") private boolean
    isType2(Atom a) { return a instanceof Type; }

    private boolean
    isType2(AmbiguousName an) throws CompileException { return this.isType(this.reclassify(an)); }

    /**
     * Determine whether the given {@link IClass.IMember} is accessible in the given context, according to JLS 6.6.1.4.
     * Issues a {@link #compileError(String)} if not.
     */
    private boolean
    isAccessible(IClass.IMember member, Scope contextScope) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass  declaringIClass = member.getDeclaringIClass();
        boolean acc             = this.isAccessible(declaringIClass, contextScope);
        acc = acc && this.isAccessible(declaringIClass, member.getAccess(), contextScope);
        return acc;
    }

    /**
     * Check whether the given {@link IClass.IMember} is accessible in the given context, according to JLS 6.6.1.4.
     * Issue a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass.IMember member, BlockStatement contextBlockStatement) throws CompileException {

        // You have to check that both the class and member are accessible in this scope.
        IClass declaringIClass = member.getDeclaringIClass();
        this.checkAccessible(declaringIClass, contextBlockStatement);
        this.checkAccessible(declaringIClass, member.getAccess(), contextBlockStatement);
    }

    /**
     * Determine whether a member (class, interface, field or method) declared in a given class is accessible from a
     * given block statement context, according to JLS2 6.6.1.4.
     */
    private boolean
    isAccessible(IClass iClassDeclaringMember, Access memberAccess, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextScope);
    }

    /**
     * Verify that a member (class, interface, field or method) declared in a given class is accessible from a given
     * block statement context, according to JLS2 6.6.1.4. Issue a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(
        IClass         iClassDeclaringMember,
        Access         memberAccess,
        BlockStatement contextBlockStatement
    ) throws CompileException {
        String message = this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextBlockStatement);
        if (message != null) this.compileError(message, contextBlockStatement.getLocation());
    }

    /**
     * @return a descriptive text iff a member declared in that {@link IClass} with that {@link Access} is inaccessible
     */
    private String
    internalCheckAccessible(
        IClass iClassDeclaringMember,
        Access memberAccess,
        Scope  contextScope
    ) throws CompileException {

        // At this point, memberAccess is PUBLIC, DEFAULT, PROECTEDED or PRIVATE.

        // PUBLIC members are always accessible.
        if (memberAccess == Access.PUBLIC) return null;

        // At this point, the member is DEFAULT, PROECTEDED or PRIVATE accessible.

        // Determine the class declaring the context block statement.
        IClass iClassDeclaringContextBlockStatement;
        for (Scope s = contextScope;; s = s.getEnclosingScope()) {
            if (s instanceof TypeDeclaration) {
                iClassDeclaringContextBlockStatement = this.resolve((TypeDeclaration) s);
                break;
            }
        }

        // Access is always allowed for block statements declared in the same class as the member.
        if (iClassDeclaringContextBlockStatement == iClassDeclaringMember) return null;

        // Check whether the member and the context block statement are enclosed by the same top-level type.
        {
            IClass topLevelIClassEnclosingMember = iClassDeclaringMember;
            for (IClass c = iClassDeclaringMember.getDeclaringIClass(); c != null; c = c.getDeclaringIClass()) {
                topLevelIClassEnclosingMember = c;
            }
            IClass topLevelIClassEnclosingContextBlockStatement = iClassDeclaringContextBlockStatement;
            for (
                IClass c = iClassDeclaringContextBlockStatement.getDeclaringIClass();
                c != null;
                c = c.getDeclaringIClass()
            ) topLevelIClassEnclosingContextBlockStatement = c;

            if (topLevelIClassEnclosingMember == topLevelIClassEnclosingContextBlockStatement) return null;
        }

        if (memberAccess == Access.PRIVATE) {
            return "Private member cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\".";
        }

        // At this point, the member is DEFAULT or PROTECTED accessible.

        // Check whether the member and the context block statement are declared in the same package.
        if (Descriptor.areInSamePackage(
            iClassDeclaringMember.getDescriptor(),
            iClassDeclaringContextBlockStatement.getDescriptor()
        )) return null;

        if (memberAccess == Access.DEFAULT) {
            return (
                "Member with \""
                + memberAccess
                + "\" access cannot be accessed from type \""
                + iClassDeclaringContextBlockStatement
                + "\"."
            );
        }

        // At this point, the member is PROTECTED accessible.

        // Check whether the class declaring the context block statement is a subclass of the class declaring the
        // member or a nested class whose parent is a subclass
        IClass parentClass = iClassDeclaringContextBlockStatement;
        do {
            if (iClassDeclaringMember.isAssignableFrom(parentClass)) {
                return null;
            }
            parentClass = parentClass.getOuterIClass();
        } while (parentClass != null);

        return (
            "Protected member cannot be accessed from type \""
            + iClassDeclaringContextBlockStatement
            + "\", which is neither declared in the same package as nor is a subclass of \""
            + iClassDeclaringMember
            + "\"."
        );
    }

    /**
     * Determine whether the given {@link IClass} is accessible in the given context, according to JLS2 6.6.1.2 and
     * 6.6.1.4.
     */
    private boolean
    isAccessible(IClass type, Scope contextScope) throws CompileException {
        return null == this.internalCheckAccessible(type, contextScope);
    }

    /**
     * Check whether the given {@link IClass} is accessible in the given context, according to JLS2 6.6.1.2 and
     * 6.6.1.4. Issues a {@link #compileError(String)} if not.
     */
    private void
    checkAccessible(IClass type, BlockStatement contextBlockStatement) throws CompileException {
        String message = this.internalCheckAccessible(type, contextBlockStatement);
        if (message != null) this.compileError(message, contextBlockStatement.getLocation());
    }

    private String
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
                }

                // Check whether the type is accessed from within the same package.
                String packageDeclaringType = Descriptor.getPackageName(type.getDescriptor());
                String
                contextPackage = Descriptor.getPackageName(iClassDeclaringContextBlockStatement.getDescriptor());
                if (
                    packageDeclaringType == null
                    ? contextPackage != null
                    : !packageDeclaringType.equals(contextPackage)
                ) return "\"" + type + "\" is inaccessible from this package";
                return null;
            } else
            {
                throw new JaninoRuntimeException("\"" + type + "\" has unexpected access \"" + type.getAccess() + "\"");
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
            return new SimpleType(a.getLocation(), this.iClassLoader.JAVA_LANG_OBJECT);
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

    public final Lvalue
    toLvalueOrCompileException(final Atom a) throws CompileException {
        Lvalue result = a.toLvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an lvalue", a.getLocation());
            return new Lvalue(a.getLocation()) {
                @Override public String toString()                          { return a.toString(); }
                @Override public void   accept(AtomVisitor visitor)         {}
                @Override public void   accept(RvalueVisitor visitor)       {}
                @Override public void   accept(LvalueVisitor visitor)       {}
                @Override public void   accept(ElementValueVisitor visitor) {}
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
        for (Iterator it = cd.getDeclaringClass().syntheticFields.values().iterator(); it.hasNext();) {
            IClass.IField sf                 = (IClass.IField) it.next();
            LocalVariable syntheticParameter = (LocalVariable) cd.syntheticParameters.get(sf.getName());
            if (syntheticParameter == null) {
                throw new JaninoRuntimeException(
                    "SNO: Synthetic parameter for synthetic field \""
                    + sf.getName()
                    + "\" not found"
                );
            }
            ExpressionStatement es = new ExpressionStatement(new Assignment(
                cd.getLocation(),        // location
                new FieldAccess(         // lhs
                    cd.getLocation(),                    // location
                    new ThisReference(cd.getLocation()), // lhs
                    sf                                   // field
                ),
                "=",                     // operator
                new LocalVariableAccess( // rhs
                    cd.getLocation(),  // location
                    syntheticParameter // localVariable
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
        for (int i = 0; i < cd.getDeclaringClass().variableDeclaratorsAndInitializers.size(); ++i) {
            TypeBodyDeclaration tbd = (
                (TypeBodyDeclaration) cd.getDeclaringClass().variableDeclaratorsAndInitializers.get(i)
            );
            if (!tbd.isStatic()) {
                BlockStatement bs = (BlockStatement) tbd;
                if (!this.compile(bs)) {
                    this.compileError(
                        "Instance variable declarator or instance initializer does not complete normally",
                        bs.getLocation()
                    );
                }
            }
        }
    }

    /**
     * Statements that jump out of blocks ("return", "break", "continue") must call this method to make sure that the
     * "finally" clauses of all "try...catch" statements are executed.
     */
    private void
    leaveStatements(Scope from, Scope to, IClass optionalStackValueType) {
        for (Scope s = from; s != to; s = s.getEnclosingScope()) {
            if (s instanceof BlockStatement) {
                this.leave((BlockStatement) s, optionalStackValueType);
            }
        }
    }

    /**
     * The LHS operand of type {@code lhsType} is expected on the stack.
     * <p>
     * The following operators are supported: {@code &nbsp;&nbsp;| ^ & * / % + - &lt;&lt; &gt;&gt; &gt;&gt;&gt;}
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
            Arrays.asList(new Rvalue[] { rhs }).iterator(),
            operator
        );
    }

    /**
     * Execute an arithmetic operation on a sequence of {@code operands}. If {@code type} is non-null, the first
     * operand with that type is already on the stack.
     * <p>
     * The following operators are supported: {@code &nbsp;&nbsp;| ^ &amp; * / % + - &lt;&lt; &gt;&gt; &gt;&gt;&gt;}
     */
    private IClass
    compileArithmeticOperation(
        final Locatable locatable,
        IClass          type,
        Iterator        operands,
        String          operator
    ) throws CompileException {
        if (operator == "|" || operator == "^" || operator == "&") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            final int iopcode = (
                // CHECKSTYLE StringLiteralEquality:OFF
                operator == "&" ? Opcode.IAND :
                operator == "|" ? Opcode.IOR  :
                operator == "^" ? Opcode.IXOR :
                // CHECKSTYLE StringLiteralEquality:OFF
                Integer.MAX_VALUE
            );

            do {
                Rvalue operand = (Rvalue) operands.next();

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass               rhsType            = this.compileGetValue(operand);

                    if (type.isPrimitiveNumeric() && rhsType.isPrimitiveNumeric()) {
                        IClass promotedType = this.binaryNumericPromotion(locatable, type, convertLhsInserter, rhsType);
                        if (promotedType == IClass.INT) {
                            this.writeOpcode(locatable, iopcode);
                        } else
                        if (promotedType == IClass.LONG) {
                            this.writeOpcode(locatable, iopcode + 1);
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
                        }
                        type = promotedType;
                    } else
                    if (
                        (type == IClass.BOOLEAN || this.getUnboxedType(type) == IClass.BOOLEAN)
                        && (rhsType == IClass.BOOLEAN || this.getUnboxedType(rhsType) == IClass.BOOLEAN)
                    ) {
                        IClassLoader icl = this.iClassLoader;
                        if (type == icl.JAVA_LANG_BOOLEAN) {
                            this.codeContext.pushInserter(convertLhsInserter);
                            try {
                                this.unboxingConversion(locatable, icl.JAVA_LANG_BOOLEAN, IClass.BOOLEAN);
                            } finally {
                                this.codeContext.popInserter();
                            }
                        }
                        if (rhsType == icl.JAVA_LANG_BOOLEAN) {
                            this.unboxingConversion(locatable, icl.JAVA_LANG_BOOLEAN, IClass.BOOLEAN);
                        }
                        this.writeOpcode(locatable, iopcode);
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
            } while (operands.hasNext());
            return type;
        }

        // SUPPRESS CHECKSTYLE StringLiteralEquality
        if (operator == "*" || operator == "/" || operator == "%" || operator == "+" || operator == "-") {
            final int iopcode = (
                operator == "*"   ? Opcode.IMUL  :
                operator == "/"   ? Opcode.IDIV  :
                operator == "%"   ? Opcode.IREM  :
                operator == "+"   ? Opcode.IADD  :
                operator == "-"   ? Opcode.ISUB  : Integer.MAX_VALUE
            );

            do {
                Rvalue operand = (Rvalue) operands.next();

                IClass       operandType = this.getType(operand);
                IClassLoader icl         = this.iClassLoader;

                // String concatenation?
                // SUPPRESS CHECKSTYLE StringLiteralEquality
                if (operator == "+" && (type == icl.JAVA_LANG_STRING || operandType == icl.JAVA_LANG_STRING)) {
                    return this.compileStringConcatenation(locatable, type, operand, operands);
                }

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass               rhsType            = this.compileGetValue(operand);

                    type = this.binaryNumericPromotion(locatable, type, convertLhsInserter, rhsType);

                    int opcode;
                    if (type == IClass.INT) {
                        opcode = iopcode;
                    } else
                    if (type == IClass.LONG) {
                        opcode = iopcode + 1;
                    } else
                    if (type == IClass.FLOAT) {
                        opcode = iopcode + 2;
                    } else
                    if (type == IClass.DOUBLE) {
                        opcode = iopcode + 3;
                    } else
                    {
                        this.compileError("Unexpected promoted type \"" + type + "\"", locatable.getLocation());
                        opcode = iopcode;
                    }
                    this.writeOpcode(locatable, opcode);
                }
            } while (operands.hasNext());
            return type;
        }

        if (operator == "<<"  || operator == ">>"  || operator == ">>>") { // SUPPRESS CHECKSTYLE StringLiteralEquality
            final int iopcode = (
                operator == "<<"  ? Opcode.ISHL  :
                operator == ">>"  ? Opcode.ISHR  :
                operator == ">>>" ? Opcode.IUSHR : Integer.MAX_VALUE
            );

            do {
                Rvalue operand = (Rvalue) operands.next();

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass               rhsType            = this.compileGetValue(operand);

                    IClass promotedLhsType;
                    this.codeContext.pushInserter(convertLhsInserter);
                    try {
                        promotedLhsType = this.unaryNumericPromotion(locatable, type);
                    } finally {
                        this.codeContext.popInserter();
                    }
                    if (promotedLhsType != IClass.INT && promotedLhsType != IClass.LONG) {
                        this.compileError(
                            "Shift operation not allowed on operand type \"" + type + "\"",
                            locatable.getLocation()
                        );
                    }

                    IClass promotedRhsType = this.unaryNumericPromotion(locatable, rhsType);
                    if (promotedRhsType != IClass.INT && promotedRhsType != IClass.LONG) {
                        this.compileError(
                            "Shift distance of type \"" + rhsType + "\" is not allowed",
                            locatable.getLocation()
                        );
                    }

                    if (promotedRhsType == IClass.LONG) this.writeOpcode(locatable, Opcode.L2I);

                    this.writeOpcode(locatable, promotedLhsType == IClass.LONG ? iopcode + 1 : iopcode);
                    type = promotedLhsType;
                }
            } while (operands.hasNext());
            return type;
        }

        throw new JaninoRuntimeException("Unexpected operator \"" + operator + "\"");
    }

    /**
     * @param type     If non-null, the first operand with that type is already on the stack
     * @param operand  The next operand
     * @param operands All following operands ({@link Iterator} over {@link Rvalue}s)
     */
    private IClass
    compileStringConcatenation(
        final Locatable locatable,
        IClass          type,
        Rvalue          operand,
        Iterator        operands
    ) throws CompileException {
        boolean operandOnStack;
        if (type != null) {
            this.stringConversion(locatable, type);
            operandOnStack = true;
        } else
        {
            operandOnStack = false;
        }

        // Compute list of operands and merge consecutive constant operands.
        List tmp = new ArrayList(); // Compilable
        do {
            Object cv = this.getConstantValue(operand);
            if (cv == NOT_CONSTANT) {
                // Non-constant operand.
                final Rvalue finalOperand = operand;
                tmp.add(new Compilable() {

                    @Override public void
                    compile() throws CompileException {
                        UnitCompiler.this.stringConversion(locatable, UnitCompiler.this.compileGetValue(finalOperand));
                    }
                });

                operand = operands.hasNext() ? (Rvalue) operands.next() : null;
            } else
            {
                // Constant operand. Check to see whether the next operand is also constant.
                if (operands.hasNext()) {
                    operand = (Rvalue) operands.next();
                    Object cv2 = this.getConstantValue(operand);
                    if (cv2 != NOT_CONSTANT) {
                        StringBuilder sb = new StringBuilder(cv.toString()).append(cv2);
                        for (;;) {
                            if (!operands.hasNext()) {
                                operand = null;
                                break;
                            }
                            operand = (Rvalue) operands.next();
                            Object cv3 = this.getConstantValue(operand);
                            if (cv3 == NOT_CONSTANT) break;
                            sb.append(cv3);
                        }
                        cv = sb.toString();
                    }
                } else
                {
                    operand = null;
                }
                // Break long string constants up into UTF8-able chunks.
                final String[] ss = UnitCompiler.makeUtf8Able(cv.toString());
                for (int i = 0; i < ss.length; ++i) {
                    final String s = ss[i];
                    tmp.add(new Compilable() {
                        @Override public void compile() { UnitCompiler.this.pushConstant(locatable, s); }
                    });
                }
            }
        } while (operand != null);

        // At this point "tmp" contains an optimized sequence of Strings (representing constant portions) and Rvalues
        // (non-constant portions).

        if (tmp.size() <= (operandOnStack ? STRING_CONCAT_LIMIT - 1 : STRING_CONCAT_LIMIT)) {

            // String concatenation through "a.concat(b).concat(c)".
            for (Iterator it = tmp.iterator(); it.hasNext();) {
                Compilable c = (Compilable) it.next();
                c.compile();

                // Concatenate.
                if (operandOnStack) {
                    this.writeOpcode(locatable, Opcode.INVOKEVIRTUAL);
                    this.writeConstantMethodrefInfo(
                        Descriptor.JAVA_LANG_STRING,                                // classFD
                        "concat",                                         // methodName
                        "(" + Descriptor.JAVA_LANG_STRING + ")" + Descriptor.JAVA_LANG_STRING // methodMD
                    );
                } else
                {
                    operandOnStack = true;
                }
            }
            return this.iClassLoader.JAVA_LANG_STRING;
        }

        // String concatenation through "new StringBuilder(a).append(b).append(c).append(d).toString()".
        Iterator it = tmp.iterator();

        // "new StringBuilder(a)":
        if (operandOnStack) {
            this.writeOpcode(locatable, Opcode.NEW);
            this.writeConstantClassInfo(Descriptor.JAVA_LANG_STRINGBUILDER);
            this.writeOpcode(locatable, Opcode.DUP_X1);
            this.writeOpcode(locatable, Opcode.SWAP);
        } else
        {
            this.writeOpcode(locatable, Opcode.NEW);
            this.writeConstantClassInfo(Descriptor.JAVA_LANG_STRINGBUILDER);
            this.writeOpcode(locatable, Opcode.DUP);
            ((Compilable) it.next()).compile();
        }
        this.writeOpcode(locatable, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            Descriptor.JAVA_LANG_STRINGBUILDER,                                 // classFD
            "<init>",                                        // methodName
            "(" + Descriptor.JAVA_LANG_STRING + ")" + Descriptor.VOID // methodMD
        );
        while (it.hasNext()) {
            ((Compilable) it.next()).compile();

            // "StringBuilder.append(b)":
            this.writeOpcode(locatable, Opcode.INVOKEVIRTUAL);
            this.writeConstantMethodrefInfo(
                Descriptor.JAVA_LANG_STRINGBUILDER,                                // classFD
                "append",                                       // methodName
                "(" + Descriptor.JAVA_LANG_STRING + ")" + Descriptor.JAVA_LANG_STRINGBUILDER
            );
        }

        // "StringBuilder.toString()":
        this.writeOpcode(locatable, Opcode.INVOKEVIRTUAL);
        this.writeConstantMethodrefInfo(
            Descriptor.JAVA_LANG_STRINGBUILDER,         // classFD
            "toString",              // methodName
            "()" + Descriptor.JAVA_LANG_STRING // methodMD
        );
        return this.iClassLoader.JAVA_LANG_STRING;
    }

    interface Compilable { void compile() throws CompileException; }

    /**
     * Convert object of type "sourceType" to type "String". JLS2 15.18.1.1
     */
    private void
    stringConversion(Locatable locatable, IClass sourceType) {
        this.writeOpcode(locatable, Opcode.INVOKESTATIC);
        this.writeConstantMethodrefInfo(
            Descriptor.JAVA_LANG_STRING, // classFD
            "valueOf",         // methodName
            "(" + ((           // methodMD
                sourceType == IClass.BOOLEAN
                || sourceType == IClass.CHAR
                || sourceType == IClass.LONG
                || sourceType == IClass.FLOAT
                || sourceType == IClass.DOUBLE
            ) ? sourceType.getDescriptor() : (
                sourceType == IClass.BYTE
                || sourceType == IClass.SHORT
                || sourceType == IClass.INT
            ) ? Descriptor.INT : Descriptor.JAVA_LANG_OBJECT) + ")" + Descriptor.JAVA_LANG_STRING
        );
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     * Notice: This method is used both for explicit constructor invocation (first statement of a constructor body) and
     * implicit constructor invocation (right after NEW).
     *
     * @param optionalEnclosingInstance Used if the target class is an inner class
     */
    private void
    invokeConstructor(
        Locatable locatable,
        Scope     scope,
        Rvalue    optionalEnclosingInstance,
        IClass    targetClass,
        Rvalue[]  arguments
    ) throws CompileException {

        // Find constructors.
        IClass.IConstructor[] iConstructors = targetClass.getDeclaredIConstructors();
        if (iConstructors.length == 0) {
            throw new JaninoRuntimeException(
                "SNO: Target class \"" + targetClass.getDescriptor() + "\" has no constructors"
            );
        }

        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            locatable,             // l
            iConstructors, // iInvocables
            arguments,     // arguments
            scope          // contextScope
        );

        // Check exceptions that the constructor may throw.
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        for (int i = 0; i < thrownExceptions.length; ++i) {
            this.checkThrownException(
                locatable,
                thrownExceptions[i],
                scope
            );
        }

        // Pass enclosing instance as a synthetic parameter.
        if (optionalEnclosingInstance != null) {
            IClass outerIClass = targetClass.getOuterIClass();
            if (outerIClass != null) {
                IClass eiic = this.compileGetValue(optionalEnclosingInstance);
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

            if (!(scopeTypeDeclaration instanceof ClassDeclaration)) {
                if (syntheticFields.length > 0) {
                    throw new JaninoRuntimeException("SNO: Target class has synthetic fields");
                }
            } else {
                ClassDeclaration scopeClassDeclaration = (ClassDeclaration) scopeTypeDeclaration;
                for (int i = 0; i < syntheticFields.length; ++i) {
                    IClass.IField sf = syntheticFields[i];
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTbd instanceof MethodDeclarator) {
                            this.load(locatable, this.resolve(scopeClassDeclaration), 0);
                            this.writeOpcode(locatable, Opcode.GETFIELD);
                            this.writeConstantFieldrefInfo(
                                this.resolve(scopeClassDeclaration).getDescriptor(), // classFD
                                sf.getName(),                                        // fieldName
                                sf.getDescriptor()                                   // fieldFD
                            );
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
                                this.writeOpcode(locatable, Opcode.ACONST_NULL);
                            } else {
                                this.load(locatable, syntheticParameter);
                            }
                        } else {
                            this.compileError((
                                "Compiler limitation: "
                                + "Initializers cannot access local variables declared in an enclosing block."
                            ), locatable.getLocation());
                            this.writeOpcode(locatable, Opcode.ACONST_NULL);
                        }
                    } else {
                        String        localVariableName = sf.getName().substring(4);
                        LocalVariable lv;
                        DETERMINE_LV: {
                            Scope s;

                            // Does one of the enclosing blocks declare a local variable with that name?
                            for (s = scope; s instanceof BlockStatement; s = s.getEnclosingScope()) {
                                BlockStatement bs = (BlockStatement) s;
                                Scope          es = bs.getEnclosingScope();
                                List           statements;
                                if (es instanceof Block) {
                                    statements = ((Block) es).statements;
                                } else
                                if (es instanceof FunctionDeclarator) {
                                    statements = ((FunctionDeclarator) es).optionalStatements;
                                } else
                                {
                                    continue;
                                }

                                for (Iterator it = statements.iterator();;) {
                                    BlockStatement bs2 = (BlockStatement) it.next();
                                    if (bs2 == bs) break;
                                    if (bs2 instanceof LocalVariableDeclarationStatement) {
                                        LocalVariableDeclarationStatement lvds = (
                                            (LocalVariableDeclarationStatement) bs2
                                        );
                                        VariableDeclarator[] vds = lvds.variableDeclarators;
                                        for (int j = 0; j < vds.length; ++j) {
                                            if (vds[j].name.equals(localVariableName)) {
                                                lv = this.getLocalVariable(lvds, vds[j]);
                                                break DETERMINE_LV;
                                            }
                                        }
                                    }
                                }
                            }

                            // Does the declaring function declare a parameter with that name?
                            while (!(s instanceof FunctionDeclarator)) s = s.getEnclosingScope();
                            FunctionDeclarator fd = (FunctionDeclarator) s;
                            for (int j = 0; j < fd.formalParameters.length; ++j) {
                                FunctionDeclarator.FormalParameter fp = fd.formalParameters[j];
                                if (fp.name.equals(localVariableName)) {
                                    lv = this.getLocalVariable(fp);
                                    break DETERMINE_LV;
                                }
                            }
                            throw new JaninoRuntimeException(
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
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        for (int i = 0; i < arguments.length; ++i) {
            this.assignmentConversion(
                locatable,                                  // l
                this.compileGetValue(arguments[i]), // sourceType
                parameterTypes[i],                  // targetType
                this.getConstantValue(arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()" prepended with the synthetic parameters.
        this.writeOpcode(locatable, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            targetClass.getDescriptor(), // classFD
            "<init>",                    // methodName
            iConstructor.getDescriptor() // methodMD
        );
    }

    IClass.IField[]
    getIFields(final FieldDeclaration fd) {
        IClass.IField[] res = new IClass.IField[fd.variableDeclarators.length];
        for (int i = 0; i < res.length; ++i) {
            final VariableDeclarator vd = fd.variableDeclarators[i];
            res[i] = this.resolve(fd.getDeclaringType()).new IField() {

                // Implement IMember.
                @Override public Access
                getAccess() {
                    switch (fd.modifiersAndAnnotations.modifiers & Mod.PPP) {
                    case Mod.PRIVATE:
                        return Access.PRIVATE;
                    case Mod.PROTECTED:
                        return Access.PROTECTED;
                    case Mod.PACKAGE:
                        return Access.DEFAULT;
                    case Mod.PUBLIC:
                        return Access.PUBLIC;
                    default:
                        throw new JaninoRuntimeException("Invalid access");
                    }
                }

                // Implement "IField".

                @Override public boolean
                isStatic() { return (fd.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0; }

                @Override public IClass
                getType() throws CompileException {
                    return UnitCompiler.this.getType(fd.type).getArrayIClass(
                        vd.brackets,
                        UnitCompiler.this.iClassLoader.JAVA_LANG_OBJECT
                    );
                }

                @Override public String
                getName() { return vd.name; }

                @Override public Object
                getConstantValue() throws CompileException {
                    if (
                        (fd.modifiersAndAnnotations.modifiers & Mod.FINAL) != 0
                        && vd.optionalInitializer instanceof Rvalue
                    ) {
                        Object constantInitializerValue = UnitCompiler.this.getConstantValue(
                            (Rvalue) vd.optionalInitializer
                        );
                        if (constantInitializerValue != NOT_CONSTANT) return UnitCompiler.this.assignmentConversion(
                            (Locatable) vd.optionalInitializer, // l
                            constantInitializerValue,           // value
                            this.getType()                      // targetType
                        );
                    }
                    return NOT_CONSTANT;
                }
            };
        }
        return res;
    }

    /**
     * Determine the non-constant-final initializer of the given {@link VariableDeclarator}.
     *
     * @return {@code null} if the variable is declared without an initializer or if the initializer is
     *         constant-final
     */
    ArrayInitializerOrRvalue
    getNonConstantFinalInitializer(FieldDeclaration fd, VariableDeclarator vd) throws CompileException {

        // Check if optional initializer exists.
        if (vd.optionalInitializer == null) return null;

        // Check if initializer is constant-final.
        if (
            (fd.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0
            && (fd.modifiersAndAnnotations.modifiers & Mod.FINAL) != 0
            && vd.optionalInitializer instanceof Rvalue
            && this.getConstantValue((Rvalue) vd.optionalInitializer) != NOT_CONSTANT
        ) return null;

        return vd.optionalInitializer;
    }

    private Atom
    reclassify(AmbiguousName an) throws CompileException {
        if (an.reclassified == null) {
            an.reclassified = this.reclassifyName(
                an.getLocation(),
                an.getEnclosingBlockStatement(),
                an.identifiers, an.n
            );
        }
        return an.reclassified;
    }

    /**
     * JLS 6.5.2.2
     * <p>
     * Reclassify the ambiguous name consisting of the first {@code n} of the {@code identifiers}.
     *
     * @param location
     * @param scope
     * @param identifiers
     * @param n
     * @throws CompileException
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String[] identifiers, int n) throws CompileException {

        if (n == 1) return this.reclassifyName(
            location,
            scope,
            identifiers[0]
        );

        // 6.5.2.2
        Atom lhs = this.reclassifyName(
            location,
            scope,
            identifiers, n - 1
        );
        String rhs = identifiers[n - 1];

        // 6.5.2.2.1
        if (UnitCompiler.DEBUG) System.out.println("lhs = " + lhs);
        if (lhs instanceof Package) {
            String className = ((Package) lhs).name + '.' + rhs;
            IClass result    = findTypeByName(location, className);
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
            al.setEnclosingBlockStatement((BlockStatement) scope);
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
                fa.setEnclosingBlockStatement((BlockStatement) scope);
                return fa;
            }
        }

        IClass[] classes = lhsType.getDeclaredIClasses();
        for (int i = 0; i < classes.length; ++i) {
            final IClass memberType = classes[i];
            String       name       = Descriptor.toClassName(memberType.getDescriptor());
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
            @Override public String     toString()                  { return Java.join(identifiers, "."); }
            @Override public final void accept(AtomVisitor visitor) {}
        };
    }

    /**
     * Find the named {@link IClass} in this compilation unit, or through the {@link #iClassLoader}.
     *
     * @param className         Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner".
     * @return                  {@code null} iff an {@code IClass} with that name could not be loaded
     * @throws CompileException An exception was raised while loading the {@link IClass}
     */
    private IClass
    findTypeByName(Location location, String className) throws CompileException {

        // Is the type defined in the same compilation unit?
        IClass res = this.findClass(className);
        if (res != null) return res;

        try {
            return this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
        } catch (ClassNotFoundException ex) {
            // SUPPRESS CHECKSTYLE AvoidHidingCause
            if (ex.getException() instanceof CompileException) throw (CompileException) ex.getException();
            throw new CompileException(className, location, ex);
        }
    }

    /**
     * JLS 6.5.2.1
     *
     * @param location
     * @param scope
     * @param identifier
     * @throws CompileException
     */
    private Atom
    reclassifyName(Location location, Scope scope, final String identifier) throws CompileException {

        // Determine scope block statement, type body declaration, type and compilation unit.
        BlockStatement          scopeBlockStatement  = null;
        TypeBodyDeclaration     scopeTbd             = null;
        AbstractTypeDeclaration scopeTypeDeclaration = null;
        CompilationUnit         scopeCompilationUnit;
        {
            Scope s = scope;
            if (s instanceof BlockStatement) scopeBlockStatement = (BlockStatement) s;
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

        // 6.5.2.BL1.B1.B1.1 (JLS3: 6.5.2.BL1.B1.B1.1) / 6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2 (JLS3: 6.5.2.BL1.B1.B1.2) / 6.5.6.1.1 Parameter.
        {
            Scope s = scope;
            if (s instanceof BlockStatement) {
                BlockStatement bs = (BlockStatement) s;
                LocalVariable  lv = bs.findLocalVariable(identifier);
                if (lv != null) {
                    LocalVariableAccess lva = new LocalVariableAccess(location, lv);
                    lva.setEnclosingBlockStatement(bs);
                    return lva;
                }
                s = s.getEnclosingScope();
            }
            while (s instanceof BlockStatement || s instanceof CatchClause) s = s.getEnclosingScope();
            if (s instanceof FunctionDeclarator) {
                s = s.getEnclosingScope();
            }
            if (s instanceof InnerClassDeclaration) {
                InnerClassDeclaration icd = (InnerClassDeclaration) s;
                s = s.getEnclosingScope();
                if (s instanceof AnonymousClassDeclaration) s = s.getEnclosingScope();
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
                            location,                   // location
                            new QualifiedThisReference( // lhs
                                location,                                   // location
                                new SimpleType(location, this.resolve(icd)) // qualification
                            ),
                            iField                      // field
                        );
                        fa.setEnclosingBlockStatement((BlockStatement) scope);
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

        // 6.5.2.BL1.B1.B1.3 (JLS3: 6.5.2.BL1.B1.B1.3) / 6.5.6.1.2.1 Field.
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

                    SimpleType ct = new SimpleType(scopeTypeDeclaration.getLocation(), etd);
                    Atom       lhs;
                    if (scopeTbd.isStatic()) {

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
                    Rvalue res = new FieldAccess(
                        location,
                        lhs,
                        f
                    );
                    res.setEnclosingBlockStatement(enclosingBlockStatement);
                    return res;
                }
            }
        }

        // JLS3 6.5.2.BL1.B1.B2.1 Static field imported through single static import.
        {
            List l = (List) this.singleStaticImports.get(identifier);
            if (l != null) {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    Object o = it.next();
                    if (o instanceof IField) {
                        FieldAccess fieldAccess = new FieldAccess(
                            location,
                            new SimpleType(location, ((IField) o).getDeclaringIClass()),
                            (IField) o
                        );
                        fieldAccess.setEnclosingBlockStatement(enclosingBlockStatement);
                        return fieldAccess;
                    }
                }
            }
        }

        // JLS3 6.5.2.BL1.B1.B2.2 Static field imported through static-import-on-demand.
        {
            IField importedField = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass iClass = (IClass) it.next();
                IField f      = iClass.getDeclaredIField(identifier);
                if (f != null) {
                    // JLS3 7.5.4 Static-Import-on-Demand Declaration
                    if (!this.isAccessible(f, enclosingBlockStatement)) continue;

                    if (importedField != null) {
                        this.compileError(
                            "Ambiguous static field import: \""
                            + importedField.toString()
                            + "\" vs. \""
                            + f.toString()
                            + "\""
                        );
                    }
                    importedField = f;
                }
            }
            if (importedField != null) {
                if (!importedField.isStatic()) this.compileError("Cannot static-import non-static field");
                FieldAccess fieldAccess = new FieldAccess(
                    location,
                    new SimpleType(location, importedField.getDeclaringIClass()),
                    importedField
                );
                fieldAccess.setEnclosingBlockStatement(enclosingBlockStatement);
                return fieldAccess;
            }
        }

        // Hack: "java" MUST be a package, not a class.
        if ("java".equals(identifier)) return new Package(location, identifier);

        // 6.5.2.BL1.B1.B2.1 (JLS3: 6.5.2.BL1.B1.B3.2) Local class.
        {
            LocalClassDeclaration lcd = findLocalClassDeclaration(scope, identifier);
            if (lcd != null) return new SimpleType(location, this.resolve(lcd));
        }

        // 6.5.2.BL1.B1.B2.2 (JLS3: 6.5.2.BL1.B1.B3.3) Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = this.findMemberType(
                this.resolve(scopeTypeDeclaration),
                identifier,
                location
            );
            if (memberType != null) return new SimpleType(location, memberType);
        }

        // 6.5.2.BL1.B1.B3.1 (JLS3: 6.5.2.BL1.B1.B4.1) Single type import.
        {
            IClass iClass = this.importSingleType(identifier, location);
            if (iClass != null) return new SimpleType(location, iClass);
        }

        // 6.5.2.BL1.B1.B3.2 (JLS3: 6.5.2.BL1.B1.B3.1) Package member class/interface declared in this compilation
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
            String className = (
                scopeCompilationUnit.optionalPackageDeclaration == null
                ? identifier
                : scopeCompilationUnit.optionalPackageDeclaration.packageName + '.' + identifier
            );
            IClass result = this.findTypeByName(location, className);
            if (result != null) return new SimpleType(location, result);
        }

        // 6.5.2.BL1.B1.B5 (JLS3: 6.5.2.BL1.B1.B4.2), 6.5.2.BL1.B1.B6 Type-import-on-demand.
        {
            IClass importedClass = this.importTypeOnDemand(identifier, location);
            if (importedClass != null) {
                return new SimpleType(location, importedClass);
            }
        }

        // JLS3 6.5.2.BL1.B1.B4.3 Type imported through single static import.
        {
            List l = (List) this.singleStaticImports.get(identifier);
            if (l != null) {
                for (Iterator it = l.iterator(); it.hasNext();) {
                    Object o = it.next();
                    if (o instanceof IClass) return new SimpleType(null, (IClass) o);
                }
            }
        }

        // JLS3 6.5.2.BL1.B1.B4.4 Type imported through static-import-on-demand.
        {
            IClass importedType = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass   ic          = (IClass) it.next();
                IClass[] memberTypes = ic.getDeclaredIClasses();
                for (int i = 0; i < memberTypes.length; ++i) {
                    IClass mt = memberTypes[i];
                    if (!this.isAccessible(mt, scopeBlockStatement)) continue;
                    if (mt.getDescriptor().endsWith('$' + identifier + ';')) {
                        if (importedType != null) {
                            this.compileError(
                                "Ambiguous static type import: \""
                                + importedType.toString()
                                + "\" vs. \""
                                + mt.toString()
                                + "\""
                            );
                        }
                        importedType = mt;
                    }
                }
            }
            if (importedType != null) return new SimpleType(null, importedType);
        }

        // 6.5.2.BL1.B1.B7 Package name
        return new Package(location, identifier);
    }

    private void
    determineValue(FieldAccessExpression fae) throws CompileException {
        if (fae.value != null) return;

        IClass lhsType = this.getType(fae.lhs);

        if (fae.fieldName.equals("length") && lhsType.isArray()) {
            fae.value = new ArrayLength(
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
                fae.value = new Rvalue(fae.getLocation()) {
                    @Override public String toString()                          { return "???"; }
                    @Override public void   accept(AtomVisitor visitor)         {}
                    @Override public void   accept(RvalueVisitor visitor)       {}
                    @Override public void   accept(ElementValueVisitor visitor) {}
                };
                return;
            }

            fae.value = new FieldAccess(
                fae.getLocation(),
                fae.lhs,
                iField
            );
        }
        fae.value.setEnclosingBlockStatement(fae.getEnclosingBlockStatement());
    }

    /**
     * "super.fld", "Type.super.fld"
     */
    private void
    determineValue(SuperclassFieldAccessExpression scfae) throws CompileException {
        if (scfae.value != null) return;

        Rvalue lhs;
        {
            ThisReference tr = new ThisReference(scfae.getLocation());
            tr.setEnclosingBlockStatement(scfae.getEnclosingBlockStatement());
            IClass type;
            if (scfae.optionalQualification != null) {
                type = this.getType(scfae.optionalQualification);
            } else
            {
                type = this.getType(tr);
            }
            lhs = new Cast(scfae.getLocation(), new SimpleType(scfae.getLocation(), type.getSuperclass()), tr);
        }

        IClass.IField iField = this.findIField(this.getType(lhs), scfae.fieldName, scfae.getLocation());
        if (iField == null) {
            this.compileError("Class has no field \"" + scfae.fieldName + "\"", scfae.getLocation());
            scfae.value = new Rvalue(scfae.getLocation()) {
                @Override public String toString()                          { return "???"; }
                @Override public void   accept(AtomVisitor visitor)         {}
                @Override public void   accept(RvalueVisitor visitor)       {}
                @Override public void   accept(ElementValueVisitor visitor) {}
            };
            return;
        }
        scfae.value = new FieldAccess(
            scfae.getLocation(),
            lhs,
            iField
        );
        scfae.value.setEnclosingBlockStatement(scfae.getEnclosingBlockStatement());
    }

    /**
     * Find named methods of "targetType", examine the argument types and choose the most specific method. Check that
     * only the allowed exceptions are thrown.
     * <p>
     * Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     *
     * @return The selected {@link IClass.IMethod} or {@code null}
     */
    public IClass.IMethod
    findIMethod(MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod;
        FIND_METHOD: {

            if (mi.optionalTarget == null) {

                // Method invocation by simple method name... method must be declared by an enclosing type declaration.
                for (
                    Scope s = mi.getEnclosingBlockStatement();
                    !(s instanceof CompilationUnit);
                    s = s.getEnclosingScope()
                ) {
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
                    this.getType(mi.optionalTarget), // targetType
                    mi                               // invocable
                );
                if (iMethod != null) break FIND_METHOD;
            }

            // Static method declared through single static import?
            {
                List l = (List) this.singleStaticImports.get(mi.methodName);
                if (l != null) {
                    iMethod = null;
                    for (Iterator it = l.iterator(); it.hasNext();) {
                        Object o = it.next();
                        if (o instanceof IMethod) {
                            IClass  declaringIClass = ((IMethod) o).getDeclaringIClass();
                            IMethod im              = this.findIMethod(
                                declaringIClass, // targetType
                                mi               // invocable
                            );
                            if (im != null) {
                                if (iMethod != null && iMethod != im) {
                                    this.compileError(
                                        "Ambiguous static method import: \""
                                        + iMethod.toString()
                                        + "\" vs. \""
                                        + im.toString()
                                        + "\""
                                    );
                                }
                                iMethod = im;
                            }
                        }
                    }
                    if (iMethod != null) break FIND_METHOD;
                }
            }

            // Static method declared through static-import-on-demand?
            iMethod = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass  iClass = (IClass) it.next();
                IMethod im     = this.findIMethod(
                    iClass, // targetType
                    mi      // invocable
                );
                if (im != null) {
                    if (iMethod != null) {
                        this.compileError(
                            "Ambiguous static method import: \""
                            + iMethod.toString()
                            + "\" vs. \""
                            + im.toString()
                            + "\""
                        );
                    }
                    iMethod = im;
                }
            }
            if (iMethod != null) break FIND_METHOD;

            this.compileError((
                "A method named \""
                + mi.methodName
                + "\" is not declared in any enclosing class nor any supertype, nor through a static import"
            ), mi.getLocation());
            return fakeIMethod(this.iClassLoader.JAVA_LANG_OBJECT, mi.methodName, mi.arguments);
        }

        this.checkThrownExceptions(mi, iMethod);
        return iMethod;
    }

    /**
     * Find a {@link IClass.IMethod} in the given {@code targetType}, its superclasses or superinterfaces with the
     * given {@code name} and for the given {@code arguments}. If more than one such method exists, choose the most
     * specific one (JLS 15.11.2).
     *
     * @return {@code null} if no appropriate method could be found
     */
    private IClass.IMethod
    findIMethod(IClass targetType, Invocation invocation) throws CompileException {

        // Get all methods.
        List ms = new ArrayList();
        this.getIMethods(targetType, invocation.methodName, ms);

        // JLS2 6.4.3
        if (targetType.isInterface()) {
            IClass.IMethod[] oms = this.iClassLoader.JAVA_LANG_OBJECT.getDeclaredIMethods(invocation.methodName);
            for (int i = 0; i < oms.length; ++i) {
                IClass.IMethod om = oms[i];
                if (!om.isStatic() && om.getAccess() == Access.PUBLIC) ms.add(om);
            }
        }

        if (ms.size() == 0) return null;

        // Determine arguments' types, choose the most specific method.
        return (IClass.IMethod) this.findMostSpecificIInvocable(
            (Locatable) invocation,                                       // l
            (IClass.IMethod[]) ms.toArray(new IClass.IMethod[ms.size()]), // iInvocables
            invocation.arguments,                                         // arguments
            invocation.getEnclosingBlockStatement()                       // contextScope
        );
    }

    private IMethod
    fakeIMethod(IClass targetType, final String name, Rvalue[] arguments) throws CompileException {
        final IClass[] pts = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) pts[i] = this.getType(arguments[i]);
        return targetType.new IMethod() {
            @Override public String   getName()             { return name; }
            @Override public IClass   getReturnType()       { return IClass.INT; }
            @Override public boolean  isStatic()            { return false; }
            @Override public boolean  isAbstract()          { return false; }
            @Override public IClass[] getParameterTypes()   { return pts; }
            @Override public IClass[] getThrownExceptions() { return new IClass[0]; }
            @Override public Access   getAccess()           { return Access.PUBLIC; }
        };
    }

    /**
     * Add all methods with the given {@code methodName} that are declared by the {@code type}, its superclasses and
     * all their superinterfaces to the result list {@code v}.
     */
    public void
    getIMethods(IClass type, String methodName, List/*<IMethod>*/ v) throws CompileException {

        // Check methods declared by this type.
        {
            IClass.IMethod[] ims = type.getDeclaredIMethods(methodName);
            for (int i = 0; i < ims.length; ++i) v.add(ims[i]);
        }

        // Check superclass.
        IClass superclass = type.getSuperclass();
        if (superclass != null) this.getIMethods(superclass, methodName, v);

        // Check superinterfaces.
        IClass[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; ++i) this.getIMethods(interfaces[i], methodName, v);
    }

    public IClass.IMethod
    findIMethod(SuperclassMethodInvocation scmi) throws CompileException {
        ClassDeclaration declaringClass;
        for (Scope s = scmi.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) s;
                if ((fd.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0) {
                    this.compileError("Superclass method cannot be invoked in static context", scmi.getLocation());
                }
            }
            if (s instanceof ClassDeclaration) {
                declaringClass = (ClassDeclaration) s;
                break;
            }
        }
        IClass  superclass = this.resolve(declaringClass).getSuperclass();
        IMethod iMethod    = this.findIMethod(
            superclass, // targetType
            scmi        // invocation
        );
        if (iMethod == null) {
            this.compileError(
                "Class \"" + superclass + "\" has no method named \"" + scmi.methodName + "\"",
                scmi.getLocation()
            );
            return this.fakeIMethod(superclass, scmi.methodName, scmi.arguments);
        }
        this.checkThrownExceptions(scmi, iMethod);
        return iMethod;
    }

    /**
     * Determine the arguments' types, determine the applicable invocables and choose the most specific invocable.
     *
     * @param iInvocables       Length must be greater than zero
     * @return                  The selected {@link IClass.IInvocable}
     */
    private IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        final IInvocable[] iInvocables,
        Rvalue[]           arguments,
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
        sb.append("; candidates are: ").append('"' + iInvocables[0].toString() + '"');
        for (int i = 1; i < iInvocables.length; ++i) {
            sb.append(", ").append('"' + iInvocables[i].toString() + '"');
        }
        this.compileError(sb.toString(), locatable.getLocation());

        // Well, returning a "fake" IInvocable is a bit tricky, because the iInvocables can be of different types.
        if (iInvocables[0] instanceof IClass.IConstructor) {
            return iInvocables[0].getDeclaringIClass().new IConstructor() {
                @Override public IClass[] getParameterTypes()   { return argumentTypes; }
                @Override public Access   getAccess()           { return Access.PUBLIC; }
                @Override public IClass[] getThrownExceptions() { return new IClass[0]; }
            };
        } else
        if (iInvocables[0] instanceof IClass.IMethod) {
            return iInvocables[0].getDeclaringIClass().new IMethod() {
                @Override public boolean  isStatic()            { return true; }
                @Override public boolean  isAbstract()          { return false; }
                @Override public IClass   getReturnType()       { return IClass.INT; }
                @Override public String   getName()             { return ((IClass.IMethod) iInvocables[0]).getName(); }
                @Override public Access   getAccess()           { return Access.PUBLIC; }
                @Override public IClass[] getParameterTypes()   { return argumentTypes; }
                @Override public IClass[] getThrownExceptions() { return new IClass[0]; }
            };
        } else
        {
            return iInvocables[0];
        }
    }

    /**
     * Determine the applicable invocables and choose the most specific invocable.
     *
     * @return                  The maximally specific {@link IClass.IInvocable} or {@code null} if no {@link
     *                          IClass.IInvocable} is applicable
     * @throws CompileException
     */
    public IClass.IInvocable
    findMostSpecificIInvocable(
        Locatable          locatable,
        final IInvocable[] iInvocables,
        final IClass[]     argumentTypes,
        boolean            boxingPermitted,
        Scope              contextScope
    ) throws CompileException {
        if (UnitCompiler.DEBUG) {
            System.out.println("Argument types:");
            for (int i = 0; i < argumentTypes.length; ++i) {
                System.out.println(argumentTypes[i]);
            }
        }

        // Select applicable methods (15.12.2.1).
        List applicableIInvocables = new ArrayList();
        NEXT_METHOD:
        for (int i = 0; i < iInvocables.length; ++i) {
            IClass.IInvocable ii = iInvocables[i];

            // Check parameter count.
            IClass[] parameterTypes = ii.getParameterTypes();
            if (parameterTypes.length != argumentTypes.length) continue;
            if (!this.isAccessible(ii, contextScope)) continue;

            // Check argument types vs. parameter types.
            if (UnitCompiler.DEBUG) System.out.println("Parameter / argument type check:");
            for (int j = 0; j < argumentTypes.length; ++j) {
                // Is method invocation conversion possible (5.3)?
                if (UnitCompiler.DEBUG) System.out.println(parameterTypes[j] + " <=> " + argumentTypes[j]);
                if (!this.isMethodInvocationConvertible(argumentTypes[j], parameterTypes[j], boxingPermitted)) {
                    continue NEXT_METHOD;
                }
            }

            // Applicable!
            if (UnitCompiler.DEBUG) System.out.println("Applicable!");
            applicableIInvocables.add(ii);
        }
        if (applicableIInvocables.size() == 0) return null;

        // Choose the most specific invocable (15.12.2.2).
        if (applicableIInvocables.size() == 1) {
            return (IClass.IInvocable) applicableIInvocables.get(0);
        }

        // Determine the "maximally specific invocables".
        List maximallySpecificIInvocables = new ArrayList();
        for (int i = 0; i < applicableIInvocables.size(); ++i) {
            IClass.IInvocable applicableIInvocable = (IClass.IInvocable) applicableIInvocables.get(i);
            int               moreSpecific         = 0, lessSpecific = 0;
            for (int j = 0; j < maximallySpecificIInvocables.size(); ++j) {
                IClass.IInvocable mostSpecificIInvocable = (IClass.IInvocable) maximallySpecificIInvocables.get(j);
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
            if (UnitCompiler.DEBUG) System.out.println("maximallySpecificIInvocables=" + maximallySpecificIInvocables);
        }

        if (maximallySpecificIInvocables.size() == 1) return (IClass.IInvocable) maximallySpecificIInvocables.get(0);

        ONE_NON_ABSTRACT_INVOCABLE:
        if (maximallySpecificIInvocables.size() > 1 && iInvocables[0] instanceof IClass.IMethod) {

            // Check if all methods have the same signature (i.e. the types of all their parameters are identical) and
            // exactly one of the methods is non-abstract (JLS 15.12.2.2.BL2.B1).
            IClass.IMethod theNonAbstractMethod = null;
            {
                Iterator       it                          = maximallySpecificIInvocables.iterator();
                IClass.IMethod m                           = (IClass.IMethod) it.next();
                IClass[]       parameterTypesOfFirstMethod = m.getParameterTypes();
                for (;;) {
                    if (!m.isAbstract()) {
                        if (theNonAbstractMethod == null) {
                            theNonAbstractMethod = m;
                        } else {
                            IClass declaringIClass                     = m.getDeclaringIClass();
                            IClass theNonAbstractMethodDeclaringIClass = theNonAbstractMethod.getDeclaringIClass();
                            if (declaringIClass == theNonAbstractMethodDeclaringIClass) {
                                if (m.getReturnType() == theNonAbstractMethod.getReturnType()) {
                                    throw new JaninoRuntimeException(
                                        "Two non-abstract methods '" + m + "' have the same parameter types, "
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
                                    throw new JaninoRuntimeException("Incompatible return types");
                                }
                            } else
                            if (declaringIClass.isAssignableFrom(theNonAbstractMethodDeclaringIClass)) {
                                ;
                            } else
                            if (theNonAbstractMethodDeclaringIClass.isAssignableFrom(declaringIClass)) {
                                theNonAbstractMethod = m;
                            } else
                            {
                                throw new JaninoRuntimeException(
                                    "SNO: Types declaring '"
                                    + theNonAbstractMethod
                                    + "' are not assignable"
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

            // JLS 15.12.2.2.BL2.B1.B1
            if (theNonAbstractMethod != null) return theNonAbstractMethod;

            // JLS 15.12.2.2.BL2.B1.B2
            // Check "that exception [te1] is declared in the THROWS clause of each of the maximally specific methods".
            Set s = new HashSet();
            {
                IClass[][] tes = new IClass[maximallySpecificIInvocables.size()][];
                Iterator   it  = maximallySpecificIInvocables.iterator();
                for (int i = 0; i < tes.length; ++i) {
                    tes[i] = ((IClass.IMethod) it.next()).getThrownExceptions();
                }
                for (int i = 0; i < tes.length; ++i) {
                    EACH_EXCEPTION:
                    for (int j = 0; j < tes[i].length; ++j) {
                        IClass te1 = tes[i][j];
                        EACH_METHOD:
                        for (int k = 0; k < tes.length; ++k) {
                            if (k == i) continue;
                            for (int m = 0; m < tes[k].length; ++m) {
                                IClass te2 = tes[k][m];
                                if (te2.isAssignableFrom(te1)) continue EACH_METHOD;
                            }
                            continue EACH_EXCEPTION;
                        }
                        s.add(te1);
                    }
                }
            }

            // Return a "dummy" method.
            final IClass.IMethod im  = (IClass.IMethod) maximallySpecificIInvocables.get(0);
            final IClass[]       tes = (IClass[]) s.toArray(new IClass[s.size()]);
            return im.getDeclaringIClass().new IMethod() {
                @Override public String   getName()                                   { return im.getName(); }
                @Override public IClass   getReturnType() throws CompileException     { return im.getReturnType(); }
                @Override public boolean  isAbstract()                                { return im.isAbstract(); }
                @Override public boolean  isStatic()                                  { return im.isStatic(); }
                @Override public Access   getAccess()                                 { return im.getAccess(); }
                @Override public IClass[] getParameterTypes() throws CompileException { return im.getParameterTypes(); }
                @Override public IClass[] getThrownExceptions()                       { return tes; }
            };
        }

        // JLS 15.12.2.2.BL2.B2
        {
            StringBuilder sb = new StringBuilder("Invocation of constructor/method with actual parameter type(s) \"");
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

    /**
     * Check if "method invocation conversion" (5.3) is possible.
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

        // JLS3 5.3 A boxing conversion (JLS3 5.1.7) optionally followed by widening reference conversion.
        if (boxingPermitted) {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                return (
                    this.isIdentityConvertible(boxedType, targetType)
                    || this.isWideningReferenceConvertible(boxedType, targetType)
                );
            }
        }

        // JLS3 5.3 An unboxing conversion (JLS3 5.1.8) optionally followed by a widening primitive conversion.
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
        for (int i = 0; i < thrownExceptions.length; ++i) {
            this.checkThrownException(
                in,                         // l
                thrownExceptions[i],                    // type
                in.getEnclosingBlockStatement() // scope
            );
        }
    }

    /**
     * @throws CompileException if the exception with the given type must not be thrown in the given scope
     */
    private void
    checkThrownException(Locatable locatable, IClass type, Scope scope) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!this.iClassLoader.JAVA_LANG_THROWABLE.isAssignableFrom(type)) {
            this.compileError(
                "Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"",
                locatable.getLocation()
            );
        }

        // "RuntimeException" and "Error" are never checked.
        if (
            this.iClassLoader.JAVA_LANG_RUNTIMEEXCEPTION.isAssignableFrom(type)
            || this.iClassLoader.JAVA_LANG_ERROR.isAssignableFrom(type)
        ) return;

        for (;; scope = scope.getEnclosingScope()) {

            // Match against enclosing "try...catch" blocks.
            if (scope instanceof TryStatement) {
                TryStatement ts = (TryStatement) scope;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    CatchClause cc         = (CatchClause) ts.catchClauses.get(i);
                    IClass      caughtType = this.getType(cc.caughtException.type);
                    if (caughtType.isAssignableFrom(type)) {

                        // This catch clause definitely catches the exception.
                        cc.reachable = true;
                        return;
                    }

                    CATCH_SUBTYPE:
                    if (type.isAssignableFrom(caughtType)) {

                        // This catch clause catches only a subtype of the exception type.
                        for (int j = 0; j < i; ++j) {
                            if (this.getType(
                                ((CatchClause) ts.catchClauses.get(j)).caughtException.type
                            ).isAssignableFrom(caughtType)) {

                                // A preceding catch clause is more general than this catch clause.
                                break CATCH_SUBTYPE;
                            }
                        }

                        // This catch clause catches PART OF the actual exceptions.
                        cc.reachable = true;
                    }
                }
            } else

            // Match against "throws" clause of declaring function.
            if (scope instanceof FunctionDeclarator) {
                FunctionDeclarator fd = (FunctionDeclarator) scope;
                for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                    IClass te = this.getType(fd.thrownExceptions[i]);
                    if (te.isAssignableFrom(type)) return;
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

        // Determine target type.
        if (qtr.targetIClass == null) {
            qtr.targetIClass = this.getType(qtr.qualification);
        }
        return qtr.targetIClass;
    }

    /**
     * Checks whether the operand is an integer-like local variable.
     */
    LocalVariable
    isIntLv(Crement c) throws CompileException {
        if (!(c.operand instanceof AmbiguousName)) return null;
        AmbiguousName an = (AmbiguousName) c.operand;

        Atom rec = this.reclassify(an);
        if (!(rec instanceof LocalVariableAccess)) return null;
        LocalVariableAccess lva = (LocalVariableAccess) rec;

        LocalVariable lv = lva.localVariable;
        if (lv.finaL) this.compileError("Must not increment or decrement \"final\" local variable", lva.getLocation());
        if (
            lv.type == IClass.BYTE
            || lv.type == IClass.SHORT
            || lv.type == IClass.INT
            || lv.type == IClass.CHAR
        ) return lv;
        return null;
    }

    private IClass
    resolve(final TypeDeclaration td) {
        final AbstractTypeDeclaration atd = (AbstractTypeDeclaration) td;
        if (atd.resolvedType == null) atd.resolvedType = new IClass() {

            @Override protected IClass.IMethod[]
            getDeclaredIMethods2() {
                IClass.IMethod[] res = new IClass.IMethod[atd.getMethodDeclarations().size()];
                int              i   = 0;
                for (Iterator it = atd.getMethodDeclarations().iterator(); it.hasNext();) {
                    res[i++] = UnitCompiler.this.toIMethod((MethodDeclarator) it.next());
                }
                return res;
            }

            private IClass[] declaredClasses;

            @Override protected IClass[]
            getDeclaredIClasses2() {
                if (this.declaredClasses == null) {
                    Collection/*<MemberTypeDeclaration>*/ mtds = td.getMemberTypeDeclarations();
                    IClass[]                              mts  = new IClass[mtds.size()];
                    int                                   i    = 0;
                    for (Iterator it = mtds.iterator(); it.hasNext();) {
                        mts[i++] = UnitCompiler.this.resolve((AbstractTypeDeclaration) it.next());
                    }
                    this.declaredClasses = mts;
                }
                return this.declaredClasses;
            }

            @Override protected IClass
            getDeclaringIClass2() {
                Scope s = atd;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
                    if (s instanceof CompilationUnit) return null;
                }
                return UnitCompiler.this.resolve((AbstractTypeDeclaration) s.getEnclosingScope());
            }

            @Override protected IClass
            getOuterIClass2() {
                AbstractTypeDeclaration oc = (AbstractTypeDeclaration) UnitCompiler.getOuterClass(atd);
                if (oc == null) return null;
                return UnitCompiler.this.resolve(oc);
            }

            @Override protected final String
            getDescriptor2() { return Descriptor.fromClassName(atd.getClassName()); }

            @Override public boolean
            isArray() { return false; }

            @Override protected IClass
            getComponentType2() { throw new JaninoRuntimeException("SNO: Non-array type has no component type"); }

            @Override public boolean
            isPrimitive() { return false; }

            @Override public boolean
            isPrimitiveNumeric() { return false; }

            @Override protected IConstructor[]
            getDeclaredIConstructors2() {
                if (atd instanceof ClassDeclaration) {
                    ConstructorDeclarator[] cs = ((ClassDeclaration) atd).getConstructors();

                    IClass.IConstructor[] res = new IClass.IConstructor[cs.length];
                    for (int i = 0; i < cs.length; ++i) res[i] = UnitCompiler.this.toIConstructor(cs[i]);
                    return res;
                }
                return new IClass.IConstructor[0];
            }

            @Override protected IField[]
            getDeclaredIFields2() {
                if (atd instanceof ClassDeclaration) {
                    ClassDeclaration cd = (ClassDeclaration) atd;
                    List             l  = new ArrayList(); // IClass.IField

                    // Determine variable declarators of type declaration.
                    for (int i = 0; i < cd.variableDeclaratorsAndInitializers.size(); ++i) {
                        BlockStatement vdoi = (BlockStatement) cd.variableDeclaratorsAndInitializers.get(i);
                        if (vdoi instanceof FieldDeclaration) {
                            FieldDeclaration fd   = (FieldDeclaration) vdoi;
                            IClass.IField[]  flds = UnitCompiler.this.getIFields(fd);
                            for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                        }
                    }
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                if (atd instanceof InterfaceDeclaration) {
                    InterfaceDeclaration id = (InterfaceDeclaration) atd;
                    List                 l  = new ArrayList();

                    // Determine static fields.
                    for (int i = 0; i < id.constantDeclarations.size(); ++i) {
                        BlockStatement bs = (BlockStatement) id.constantDeclarations.get(i);
                        if (bs instanceof FieldDeclaration) {
                            FieldDeclaration fd   = (FieldDeclaration) bs;
                            IClass.IField[]  flds = UnitCompiler.this.getIFields(fd);
                            for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                        }
                    }
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else {
                    throw new JaninoRuntimeException(
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override public IField[]
            getSyntheticIFields() {
                if (atd instanceof ClassDeclaration) {
                    Collection c = ((ClassDeclaration) atd).syntheticFields.values();
                    return (IField[]) c.toArray(new IField[c.size()]);
                }
                return new IField[0];
            }

            @Override protected IClass
            getSuperclass2() throws CompileException {
                if (atd instanceof AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getType(((AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? UnitCompiler.this.iClassLoader.JAVA_LANG_OBJECT : bt;
                }
                if (atd instanceof NamedClassDeclaration) {
                    NamedClassDeclaration ncd = (NamedClassDeclaration) atd;
                    if (ncd.optionalExtendedType == null) return UnitCompiler.this.iClassLoader.JAVA_LANG_OBJECT;
                    IClass superclass = UnitCompiler.this.getType(ncd.optionalExtendedType);
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
            getAccess() { return UnitCompiler.modifiers2Access(atd.getModifiersAndAnnotations().modifiers); }

            @Override public boolean
            isFinal() { return (atd.getModifiersAndAnnotations().modifiers & Mod.FINAL) != 0;  }

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
                    throw new JaninoRuntimeException(
                        "SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration"
                    );
                }
            }

            @Override public boolean
            isAbstract() {
                return (
                    atd instanceof InterfaceDeclaration
                    || (atd.getModifiersAndAnnotations().modifiers & Mod.ABSTRACT) != 0
                );
            }

            @Override public boolean
            isInterface() { return atd instanceof InterfaceDeclaration; }
        };

        return atd.resolvedType;
    }

    private void
    referenceThis(
        Locatable           locatable,
        ClassDeclaration    declaringClass,
        TypeBodyDeclaration declaringTypeBodyDeclaration,
        IClass              targetIClass
    ) throws CompileException {
        List path = UnitCompiler.getOuterClasses(declaringClass);

        if (declaringTypeBodyDeclaration.isStatic()) {
            this.compileError("No current instance available in static context", locatable.getLocation());
        }

        int j;
        TARGET_FOUND: {
            for (j = 0; j < path.size(); ++j) {

                // Notice: JLS 15.9.2.BL1.B3.B1.B2 seems to be wrong: Obviously, JAVAC does not only allow
                //
                //    O is the nth lexically enclosing class
                //
                // , but also
                //
                //    O is assignable from the nth lexically enclosing class
                //
                // However, this strategy bears the risk of ambiguities, because "O" may be assignable from more than
                // one enclosing class.
                if (targetIClass.isAssignableFrom(this.resolve((AbstractTypeDeclaration) path.get(j)))) {
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
                this.writeOpcode(locatable, Opcode.ALOAD_0);
                return;
            }

            ConstructorDeclarator constructorDeclarator = (
                (ConstructorDeclarator) declaringTypeBodyDeclaration
            );
            String        spn                = "this$" + (path.size() - 2);
            LocalVariable syntheticParameter = (
                (LocalVariable) constructorDeclarator.syntheticParameters.get(spn)
            );
            if (syntheticParameter == null) {
                throw new JaninoRuntimeException("SNO: Synthetic parameter \"" + spn + "\" not found");
            }
            this.load(locatable, syntheticParameter);
            i = 1;
        } else {
            this.writeOpcode(locatable, Opcode.ALOAD_0);
            i = 0;
        }
        for (; i < j; ++i) {
            final String                fieldName = "this$" + (path.size() - i - 2);
            final InnerClassDeclaration inner     = (InnerClassDeclaration) path.get(i);
            IClass                      iic       = this.resolve(inner);
            final TypeDeclaration       outer     = (TypeDeclaration) path.get(i + 1);
            final IClass                oic       = this.resolve(outer);
            inner.defineSyntheticField(new SimpleIField(
                iic,
                fieldName,
                oic
            ));
            this.writeOpcode(locatable, Opcode.GETFIELD);
            this.writeConstantFieldrefInfo(
                iic.getDescriptor(), // classFD
                fieldName,           // fieldName
                oic.getDescriptor()  // fieldFD
            );
        }
    }

    /**
     * Return a list consisting of the given {@code inner} class and all its outer classes.
     *
     * @return {@link List} of {@link TypeDeclaration}
     */
    private static List
    getOuterClasses(TypeDeclaration inner) {
        List path = new ArrayList();
        for (TypeDeclaration ic = inner; ic != null; ic = UnitCompiler.getOuterClass(ic)) path.add(ic);
        return path;
    }

    static TypeDeclaration
    getOuterClass(TypeDeclaration atd) {

        // Package member class declaration.
        if (atd instanceof PackageMemberClassDeclaration) return null;

        // Local class declaration.
        if (atd instanceof LocalClassDeclaration) {
            Scope s = atd.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator); s = s.getEnclosingScope());
            if (
                (s instanceof MethodDeclarator)
                && (((FunctionDeclarator) s).modifiersAndAnnotations.modifiers & Mod.STATIC) != 0
            ) return null;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof ClassDeclaration
            ) ? immediatelyEnclosingTypeDeclaration : null;
        }

        // Member class declaration.
        if (
            atd instanceof MemberClassDeclaration
            && (((MemberClassDeclaration) atd).getModifiersAndAnnotations().modifiers & Mod.STATIC) != 0
        ) return null;

        // Anonymous class declaration, interface declaration
        Scope s = atd;
        for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope()) {
            if (s instanceof ConstructorInvocation) return null;
            if (s instanceof CompilationUnit) return null;
        }
        //if (!(s instanceof ClassDeclaration)) return null;
        if (((TypeBodyDeclaration) s).isStatic()) return null;
        return (AbstractTypeDeclaration) s.getEnclosingScope();
    }

    private IClass
    getIClass(ThisReference tr) throws CompileException {
        if (tr.iClass == null) {

            // Compile error if in static function context.
            Scope s;
            for (
                s = tr.getEnclosingBlockStatement();
                s instanceof Statement || s instanceof CatchClause;
                s = s.getEnclosingScope()
            );
            if (s instanceof FunctionDeclarator) {
                FunctionDeclarator function = (FunctionDeclarator) s;
                if ((function.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0) {
                    this.compileError("No current instance available in static method", tr.getLocation());
                }
            }

            // Determine declaring type.
            while (!(s instanceof TypeDeclaration)) {
                s = s.getEnclosingScope();
            }
            if (!(s instanceof ClassDeclaration)) {
                this.compileError("Only methods of classes can have a current instance", tr.getLocation());
            }
            tr.iClass = this.resolve((ClassDeclaration) s);
        }
        return tr.iClass;
    }

    private IClass
    getReturnType(FunctionDeclarator fd) throws CompileException {
        if (fd.returnType == null) {
            fd.returnType = this.getType(fd.type);
        }
        return fd.returnType;
    }

    IClass.IConstructor
    toIConstructor(final ConstructorDeclarator cd) {
        if (cd.iConstructor != null) return cd.iConstructor;

        cd.iConstructor = this.resolve(cd.getDeclaringType()).new IConstructor() {

            // Implement IMember.
            @Override public Access
            getAccess() {
                switch (cd.modifiersAndAnnotations.modifiers & Mod.PPP) {
                case Mod.PRIVATE:
                    return Access.PRIVATE;
                case Mod.PROTECTED:
                    return Access.PROTECTED;
                case Mod.PACKAGE:
                    return Access.DEFAULT;
                case Mod.PUBLIC:
                    return Access.PUBLIC;
                default:
                    throw new JaninoRuntimeException("Invalid access");
                }
            }

            // Implement IInvocable.
            @Override public String
            getDescriptor() throws CompileException {
                if (!(cd.getDeclaringClass() instanceof InnerClassDeclaration)) return super.getDescriptor();

                List l = new ArrayList();

                // Convert enclosing instance reference into prepended constructor parameters.
                IClass outerClass = UnitCompiler.this.resolve(cd.getDeclaringClass()).getOuterIClass();
                if (outerClass != null) l.add(outerClass.getDescriptor());

                // Convert synthetic fields into prepended constructor parameters.
                for (Iterator it = cd.getDeclaringClass().syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    if (sf.getName().startsWith("val$")) l.add(sf.getType().getDescriptor());
                }
                FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
                for (int i = 0; i < fps.length; ++i) {
                    l.add(UnitCompiler.this.getType(fps[i].type).getDescriptor());
                }
                String[] apd = (String[]) l.toArray(new String[l.size()]);
                return new MethodDescriptor(apd, Descriptor.VOID).toString();
            }

            @Override public IClass[]
            getParameterTypes() throws CompileException {
                FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
                IClass[]                             res = new IClass[fps.length];
                for (int i = 0; i < fps.length; ++i) {
                    res[i] = UnitCompiler.this.getType(fps[i].type);
                }
                return res;
            }

            @Override public IClass[]
            getThrownExceptions() throws CompileException {
                IClass[] res = new IClass[cd.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getType(cd.thrownExceptions[i]);
                }
                return res;
            }

            @Override public String
            toString() {
                StringBuilder sb = new StringBuilder().append(cd.getDeclaringType().getClassName()).append('(');

                FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
                for (int i = 0; i < fps.length; ++i) {
                    if (i != 0) sb.append(", ");
                    try {
                        sb.append(UnitCompiler.this.getType(fps[i].type).toString());
                    } catch (CompileException ex) {
                        sb.append("???");
                    }
                }
                return sb.append(')').toString();
            }
        };
        return cd.iConstructor;
    }

    public IClass.IMethod
    toIMethod(final MethodDeclarator md) {
        if (md.iMethod != null) return md.iMethod;
        md.iMethod = this.resolve(md.getDeclaringType()).new IMethod() {

            // Implement IMember.
            @Override public Access
            getAccess() {
                switch (md.modifiersAndAnnotations.modifiers & Mod.PPP) {
                case Mod.PRIVATE:
                    return Access.PRIVATE;
                case Mod.PROTECTED:
                    return Access.PROTECTED;
                case Mod.PACKAGE:
                    return Access.DEFAULT;
                case Mod.PUBLIC:
                    return Access.PUBLIC;
                default:
                    throw new JaninoRuntimeException("Invalid access");
                }
            }

            // Implement IInvocable.

            @Override public IClass[]
            getParameterTypes() throws CompileException {
                FunctionDeclarator.FormalParameter[] fps = md.formalParameters;
                IClass[]                             res = new IClass[fps.length];
                for (int i = 0; i < fps.length; ++i) {
                    res[i] = UnitCompiler.this.getType(fps[i].type);
                }
                return res;
            }

            @Override public IClass[]
            getThrownExceptions() throws CompileException {
                IClass[] res = new IClass[md.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getType(md.thrownExceptions[i]);
                }
                return res;
            }

            // Implement IMethod.

            @Override public boolean
            isStatic() { return (md.modifiersAndAnnotations.modifiers & Mod.STATIC) != 0; }

            @Override public boolean
            isAbstract() {
                return (
                    (md.getDeclaringType() instanceof InterfaceDeclaration)
                    || (md.modifiersAndAnnotations.modifiers & Mod.ABSTRACT) != 0
                );
            }

            @Override public IClass
            getReturnType() throws CompileException { return UnitCompiler.this.getReturnType(md); }

            @Override public String
            getName() { return md.name; }
        };
        return md.iMethod;
    }

    private IClass.IInvocable
    toIInvocable(FunctionDeclarator fd) {
        if (fd instanceof ConstructorDeclarator) {
            return this.toIConstructor((ConstructorDeclarator) fd);
        } else
        if (fd instanceof MethodDeclarator) {
            return this.toIMethod((MethodDeclarator) fd);
        } else
        {
            throw new JaninoRuntimeException(
                "FunctionDeclarator is neither ConstructorDeclarator nor MethodDeclarator"
            );
        }
    }

    /**
     * If the given name was declared in a simple type import, load that class.
     */
    private IClass
    importSingleType(String simpleTypeName, Location location) throws CompileException {
        String[] ss = this.getSingleTypeImport(simpleTypeName, location);
        if (ss == null) return null;

        IClass iClass = this.findTypeByFullyQualifiedName(location, ss);
        if (iClass == null) {
            this.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
            return this.iClassLoader.JAVA_LANG_OBJECT;
        }
        return iClass;
    }

    /**
     * Check if the given simple name was imported through a single type import.
     *
     * @param name The simple type name, e.g. {@code Inner}
     * @return     The fully qualified name, e.g. <code>{ "pkg", "Outer", "Inner" }</code>, or {@code null}
     */
    public String[]
    getSingleTypeImport(String name, Location location) throws CompileException {

        // Resolve all single type imports (if not already done).
        if (this.singleTypeImports == null) {

            // Collect all single type import declarations.
            final List/*<SingleTypeImportDeclaration>*/ stids = new ArrayList();
            for (Iterator it = this.compilationUnit.importDeclarations.iterator(); it.hasNext();) {
                ImportDeclaration id = (ImportDeclaration) it.next();
                id.accept(new ImportVisitor() {

                    @Override public void
                    visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid) { stids.add(stid); }

                    @Override public void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     {}
                    @Override public void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      {}
                    @Override public void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) {}
                });
            }

            // Resolve all single type imports.
            this.singleTypeImports = new HashMap();
            for (Iterator/*<SingleTypeImportDeclaration>*/ it = stids.iterator(); it.hasNext();) {
                SingleTypeImportDeclaration stid = (SingleTypeImportDeclaration) it.next();

                String[] ids        = stid.identifiers;
                String   simpleName = last(ids);

                // Check for re-import of same simple name.
                String[] prev = (String[]) this.singleTypeImports.put(simpleName, ids);
                if (prev != null && !Arrays.equals(prev, ids)) {
                    UnitCompiler.this.compileError((
                        "Class \"" + simpleName + "\" was previously imported as "
                        + "\"" + Java.join(prev, ".") + "\", now as \"" + Java.join(ids, ".") + "\""
                    ), stid.getLocation());
                }

                if (this.findTypeByFullyQualifiedName(location, ids) == null) {
                    UnitCompiler.this.compileError(
                        "A class '" + Java.join(ids, ".") + "' could not be found",
                        stid.getLocation()
                    );
                }
            }
        }

        return (String[]) this.singleTypeImports.get(name);
    }
    /** To be used only by {@link #getSingleTypeImport(String, Location)}; {@code null} means "not yet initialized" */
    private Map/*<String simpleTypeName, String[] fullyQualifiedTypeName>*/ singleTypeImports;

    /**
     * 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.<br>
     * 6.5.5.1.6 Type-import-on-demand declaration.
     *
     * @return {@code null} if the given {@code simpleTypeName} cannot be resolved through any of the
     *         import-on-demand directives
     */
    public IClass
    importTypeOnDemand(String simpleTypeName, Location location) throws CompileException {

        // Check cache. (A cache for unimportable types is not required, because the class is importable 99.9%.)
        {
            IClass importedClass = (IClass) this.onDemandImportableTypes.get(simpleTypeName);
            if (importedClass != null) return importedClass;
        }
        // Cache miss...

        // Compile all import-on-demand declarations (done here as late as possible).
        if (this.typeImportsOnDemand == null) {
            this.typeImportsOnDemand = new ArrayList();
            this.typeImportsOnDemand.add(new String[] { "java", "lang" });
            for (Iterator it = this.compilationUnit.importDeclarations.iterator(); it.hasNext();) {
                ImportDeclaration id = (ImportDeclaration) it.next();
                id.accept(new ImportVisitor() {
                    @Override public void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid) {}

                    @Override public void
                    visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd) {
                        UnitCompiler.this.typeImportsOnDemand.add(tiodd.identifiers);
                    }

                    @Override public void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      {}
                    @Override public void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) {}
                });
            }
        }

        IClass importedClass = null;
        for (Iterator i = this.typeImportsOnDemand.iterator(); i.hasNext();) {
            String[] ss     = (String[]) i.next();
            String[] ss2    = concat(ss, simpleTypeName);
            IClass   iClass = this.findTypeByFullyQualifiedName(location, ss2);
            if (iClass != null) {
                if (importedClass != null && importedClass != iClass) {
                    this.compileError(
                        "Ambiguous class name: \"" + importedClass + "\" vs. \"" + iClass + "\"",
                        location
                    );
                }
                importedClass = iClass;
            }
        }
        if (importedClass == null) return null;

        // Put in cache and return.
        this.onDemandImportableTypes.put(simpleTypeName, importedClass);
        return importedClass;
    }
    /** To be used only by {@link #importTypeOnDemand(String, Location)}; {@code null} means "not yet initialized. */
    private Collection/*<String[] package>*/ typeImportsOnDemand;
    /** To be used only by {@link #importTypeOnDemand(String, Location)}; cache for on-demand-imported types. */
    private final Map/*<String simpleTypeName, IClass>*/ onDemandImportableTypes = new HashMap();

    private void
    declareClassDollarMethod(ClassLiteral cl) {

        // Method "class$" is not yet declared; declare it like
        //
        //   static java.lang.Class class$(java.lang.String className) {
        //       try {
        //           return java.lang.Class.forName(className);
        //       } catch (java.lang.ClassNotFoundException e) {
        //           throw new java.lang.NoClassDefFoundError(e.getMessage());
        //       }
        //   }
        //
        Location                loc = cl.getLocation();
        AbstractTypeDeclaration declaringType;
        for (Scope s = cl.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof AbstractTypeDeclaration) {
                declaringType = (AbstractTypeDeclaration) s;
                break;
            }
        }

        // try {
        // return Class.forName(className);
        MethodInvocation mi = new MethodInvocation(
            loc,                                          // location
            new SimpleType(loc, this.iClassLoader.JAVA_LANG_CLASS), // optionalTarget
            "forName",                                    // methodName
            new Rvalue[] {                                // arguments
                new AmbiguousName(loc, new String[] { "className" })
            }
        );

        IClass classNotFoundExceptionIClass;
        try {
            classNotFoundExceptionIClass = this.iClassLoader.loadIClass("Ljava/lang/ClassNotFoundException;");
        } catch (ClassNotFoundException ex) {
            throw new JaninoRuntimeException("Loading class \"ClassNotFoundException\": " + ex.getMessage(), ex);
        }
        if (classNotFoundExceptionIClass == null) {
            throw new JaninoRuntimeException("SNO: Cannot load \"ClassNotFoundException\"");
        }

        IClass noClassDefFoundErrorIClass;
        try {
            noClassDefFoundErrorIClass = this.iClassLoader.loadIClass("Ljava/lang/NoClassDefFoundError;");
        } catch (ClassNotFoundException ex) {
            throw new JaninoRuntimeException("Loading class \"NoClassDefFoundError\": " + ex.getMessage(), ex);
        }
        if (noClassDefFoundErrorIClass == null) {
            throw new JaninoRuntimeException("SNO: Cannot load \"NoClassFoundError\"");
        }

        // catch (ClassNotFoundException e) {
        Block b = new Block(loc);
        // throw new NoClassDefFoundError(e.getMessage());
        b.addStatement(new ThrowStatement(loc, new NewClassInstance(
            loc,                                              // location
            (Rvalue) null,                                   // optionalQualification
            new SimpleType(loc, noClassDefFoundErrorIClass), // type
            new Rvalue[] {                                   // arguments
                new MethodInvocation(
                    loc,                                           // location
                    new AmbiguousName(loc, new String[] { "ex" }), // optionalTarget
                    "getMessage",                                  // methodName
                    new Rvalue[0]                                  // arguments
                )
            }
        )));

        List l = new ArrayList();
        l.add(new CatchClause(
            loc,                                    // location
            new FunctionDeclarator.FormalParameter( // caughtException
                loc,                                               // location
                true,                                              // finaL
                new SimpleType(loc, classNotFoundExceptionIClass), // type
                "ex"                                               // name
            ),
            b                                       // body
        ));
        TryStatement ts = new TryStatement(
            loc,                          // location
            new ReturnStatement(loc, mi), // body
            l,                            // catchClauses
            null                          // optionalFinally
        );

        List statements = new ArrayList(); // BlockStatement
        statements.add(ts);

        // Class class$(String className)
        FunctionDeclarator.FormalParameter fp = new FunctionDeclarator.FormalParameter(
            loc,                                           // location
            false,                                         // finaL
            new SimpleType(loc, this.iClassLoader.JAVA_LANG_STRING), // type
            "className"                                    // name
        );
        MethodDeclarator cdmd = new MethodDeclarator(
            loc,                                             // location
            null,                                            // optionalDocComment
            new Java.ModifiersAndAnnotations(Mod.STATIC),    // modifiers
            new SimpleType(loc, this.iClassLoader.JAVA_LANG_CLASS),    // type
            "class$",                                        // name
            new FunctionDeclarator.FormalParameter[] { fp }, // formalParameters
            new Type[0],                                     // thrownExceptions
            statements                                       // optionalStatements
        );

        declaringType.addDeclaredMethod(cdmd);
        declaringType.invalidateMethodCaches();
    }

    /**
     * @param value See {@link Token#value}, numerics optionally with a '-' prefix
     */
    private IClass
    pushConstant(Locatable locatable, Object value) {

        PUSH_INTEGER_CONSTANT: {
            int iv;
            if (value instanceof Character) {
                iv = ((Character) value).charValue();
            } else
            if (value instanceof Byte) {
                iv = ((Byte) value).intValue();
            } else
            if (value instanceof Short) {
                iv = ((Short) value).intValue();
            } else
            if (value instanceof Integer) {
                iv = ((Integer) value).intValue();
            } else
            {
                break PUSH_INTEGER_CONSTANT;
            }

            if (iv >= -1 && iv <= 5) {
                this.writeOpcode(locatable, Opcode.ICONST_0 + iv);
            } else
            if (iv >= Byte.MIN_VALUE && iv <= Byte.MAX_VALUE) {
                this.writeOpcode(locatable, Opcode.BIPUSH);
                this.writeByte((byte) iv);
            } else
            {
                this.writeLdc(locatable, this.addConstantIntegerInfo(iv));
            }
            return IClass.INT;
        }

        if (value instanceof Long) {
            long lv = ((Long) value).longValue();

            if (lv == 0L) {
                this.writeOpcode(locatable, Opcode.LCONST_0);
            } else
            if (lv == 1L) {
                this.writeOpcode(locatable, Opcode.LCONST_1);
            } else
            {
                this.writeOpcode(locatable, Opcode.LDC2_W);
                this.writeConstantLongInfo(lv);
            }
            return IClass.LONG;
        }

        if (value instanceof Float) {
            float fv = ((Float) value).floatValue();

            if (
                Float.floatToIntBits(fv) == Float.floatToIntBits(0.0F) // POSITIVE zero!
                || fv == 1.0F
                || fv == 2.0F
            ) {
                this.writeOpcode(locatable, Opcode.FCONST_0 + (int) fv);
            } else
            {
                this.writeLdc(locatable, this.addConstantFloatInfo(fv));
            }
            return IClass.FLOAT;
        }

        if (value instanceof Double) {
            double dv = ((Double) value).doubleValue();

            if (
                Double.doubleToLongBits(dv) == Double.doubleToLongBits(0.0D) // POSITIVE zero!
                || dv == 1.0D
            ) {
                this.writeOpcode(locatable, Opcode.DCONST_0 + (int) dv);
            } else
            {
                this.writeOpcode(locatable, Opcode.LDC2_W);
                this.writeConstantDoubleInfo(dv);
            }
            return IClass.DOUBLE;
        }

        if (value instanceof String) {
            String   s  = (String) value;
            String[] ss = UnitCompiler.makeUtf8Able(s);
            this.writeLdc(locatable, this.addConstantStringInfo(ss[0]));
            for (int i = 1; i < ss.length; ++i) {
                this.writeLdc(locatable, this.addConstantStringInfo(ss[i]));
                this.writeOpcode(locatable, Opcode.INVOKEVIRTUAL);
                this.writeConstantMethodrefInfo(
                    Descriptor.JAVA_LANG_STRING,                                // classFD
                    "concat",                                         // methodName
                    "(" + Descriptor.JAVA_LANG_STRING + ")" + Descriptor.JAVA_LANG_STRING // methodMD
                );
            }
            return this.iClassLoader.JAVA_LANG_STRING;
        }

        if (Boolean.TRUE.equals(value)) {
            this.writeOpcode(locatable, Opcode.ICONST_1);
            return IClass.BOOLEAN;
        }

        if (Boolean.FALSE.equals(value)) {
            this.writeOpcode(locatable, Opcode.ICONST_0);
            return IClass.BOOLEAN;
        }

        if (value == null) {
            this.writeOpcode(locatable, Opcode.ACONST_NULL);
            return IClass.VOID;
        }

        throw new JaninoRuntimeException("Unknown literal '" + value + "'");
    }

    private static int
    hex2Int(Locatable locatable, String value) throws CompileException {
        int result = 0;
        for (int i = 0; i < value.length(); ++i) {
            if ((result & 0xf0000000) != 0) {
                throw compileException(
                    locatable,
                    "Value of hexadecimal integer literal \"" + value + "\" is out of range"
                );
            }
            result = (result << 4) + Character.digit(value.charAt(i), 16);
        }
        return result;
    }

    private static int
    oct2Int(Locatable locatable, String value) throws CompileException {
        int result = 0;
        for (int i = 0; i < value.length(); ++i) {
            if ((result & 0xe0000000) != 0) {
                throw compileException(locatable, "Value of octal integer literal \"" + value + "\" is out of range");
            }
            result = (result << 3) + Character.digit(value.charAt(i), 8);
        }
        return result;
    }

    private static long
    hex2Long(Locatable locatable, String value) throws CompileException {
        long result = 0L;
        for (int i = 0; i < value.length(); ++i) {
            if ((result & 0xf000000000000000L) != 0L) {
                throw compileException(
                    locatable,
                    "Value of hexadecimal long literal \"" + value + "\" is out of range"
                );
            }
            result = (result << 4) + Character.digit(value.charAt(i), 16);
        }
        return result;
    }

    private static long
    oct2Long(Locatable locatable, String value) throws CompileException {
        long result = 0L;
        for (int i = 0; i < value.length(); ++i) {
            if ((result & 0xe000000000000000L) != 0) {
                throw compileException(locatable, "Value of octal long literal \"" + value + "\" is out of range");
            }
            result = (result << 3) + Character.digit(value.charAt(i), 8);
        }
        return result;
    }

    /**
     * Only strings that can be UTF8-encoded into 65535 bytes can be stored as a constant string info.
     *
     * @param s The string to split into suitable chunks
     * @return  The chunks that can be UTF8-encoded into 65535 bytes
     */
    private static String[]
    makeUtf8Able(String s) {
        if (s.length() < (65536 / 3)) return new String[] { s };

        int  sLength = s.length(), utfLength = 0;
        int  from    = 0;
        List l       = new ArrayList();
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
    writeLdc(Locatable locatable, short index) {
        if (0 <= index && index <= 255) {
            this.writeOpcode(locatable, Opcode.LDC);
            this.writeByte((byte) index);
        } else {
            this.writeOpcode(locatable, Opcode.LDC_W);
            this.writeShort(index);
        }
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2).
     */
    private void
    assignmentConversion(
        Locatable locatable,
        IClass    sourceType,
        IClass    targetType,
        Object    optionalConstantValue
    ) throws CompileException {
        if (UnitCompiler.DEBUG) {
            System.out.println(
                "assignmentConversion("
                + sourceType
                + ", "
                + targetType
                + ", "
                + optionalConstantValue
                + ")"
            );
        }

        // JLS2 5.1.1 Identity conversion.
        if (this.tryIdentityConversion(sourceType, targetType)) return;

        // JLS2 5.1.2 Widening primitive conversion.
        if (this.tryWideningPrimitiveConversion(locatable, sourceType, targetType)) return;

        // JLS2 5.1.4 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return;

        // A boxing conversion (JLS3 5.1.7) optionally followed by a widening reference conversion.
        {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                if (this.tryIdentityConversion(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    return;
                } else
                if (this.isWideningReferenceConvertible(boxedType, targetType)) {
                    this.boxingConversion(locatable, sourceType, boxedType);
                    return;
                }
            }
        }

        // An unboxing conversion (JLS3 5.1.8) optionally followed by a widening primitive conversion.
        {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                if (this.tryIdentityConversion(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    return;
                } else
                if (this.isWideningPrimitiveConvertible(unboxedType, targetType)) {
                    this.unboxingConversion(locatable, sourceType, unboxedType);
                    this.tryWideningPrimitiveConversion(locatable, unboxedType, targetType);
                    return;
                }
            }
        }

        // 5.2 Special narrowing primitive conversion.
        if (optionalConstantValue != NOT_CONSTANT) {
            if (this.tryConstantAssignmentConversion(
                locatable,
                optionalConstantValue, // constantValue
                targetType             // targetType
            )) return;
        }

        this.compileError(
            "Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"",
            locatable.getLocation()
        );
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2) on a constant value.
     */
    private Object
    assignmentConversion(Locatable locatable, Object value, IClass targetType) throws CompileException {
        if (targetType == IClass.BOOLEAN) {
            if (value instanceof Boolean) return value;
        } else
        if (targetType == this.iClassLoader.JAVA_LANG_STRING) {
            if (value instanceof String) return value;
        } else
        if (targetType == IClass.BYTE) {
            if (value instanceof Byte) {
                return value;
            } else
            if (value instanceof Short || value instanceof Integer) {
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
                int x = ((Number) value).intValue();
                if (x >= Character.MIN_VALUE && x <= Character.MAX_VALUE) return new Character((char) x);
            }
        } else
        if (targetType == IClass.INT) {
            if (value instanceof Integer) {
                return value;
            } else
            if (value instanceof Byte || value instanceof Short) {
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
                return new Double(((Number) value).doubleValue());
            } else
            if (value instanceof Character) {
                return new Double(((Character) value).charValue());
            }
        } else
        if (value == null && !targetType.isPrimitive()) {
            return null;
        }

        if (value == null) {
            this.compileError((
                "Cannot convert 'null' to type '"
                + targetType.toString()
                + "'"
            ), locatable.getLocation());
        } else
        {
            this.compileError((
                "Cannot convert constant of type '"
                + value.getClass().getName()
                + "' to type '"
                + targetType.toString()
                + "'"
            ), locatable.getLocation());
        }
        return value;
    }

    /**
     * Implements "unary numeric promotion" (JLS3 5.6.1)
     *
     * @return The promoted type.
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
        ) throw new JaninoRuntimeException("SNO: reverse unary numeric promotion failed");
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
                locatable,          // locatable
                sourceType, // sourceType
                targetType  // targetType
            )
        ) throw new JaninoRuntimeException("SNO: Conversion failed");
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

    /**
     * Implements "binary numeric promotion" (5.6.2)
     *
     * @return The promoted type.
     */
    private IClass
    binaryNumericPromotion(
        Locatable            locatable,
        IClass               type1,
        CodeContext.Inserter convertInserter1,
        IClass               type2
    ) throws CompileException {
        return this.binaryNumericPromotion(
            locatable,
            type1,
            convertInserter1,
            type2,
            this.codeContext.currentInserter()
        );
    }

    /**
     * Implements "binary numeric promotion" (5.6.2)
     *
     * @return The promoted type.
     */
    private IClass
    binaryNumericPromotion(
        Locatable            locatable,
        IClass               type1,
        CodeContext.Inserter convertInserter1,
        IClass               type2,
        CodeContext.Inserter convertInserter2
    ) throws CompileException {
        IClass promotedType;
        {
            IClass c1 = this.isUnboxingConvertible(type1);
            IClass c2 = this.isUnboxingConvertible(type2);
            promotedType = this.binaryNumericPromotionType(
                locatable,
                c1 != null ? c1 : type1,
                c2 != null ? c2 : type2
            );
        }

        if (convertInserter1 != null) {
            this.codeContext.pushInserter(convertInserter1);
            try {
                this.numericPromotion(locatable, this.convertToPrimitiveNumericType(locatable, type1), promotedType);
            } finally {
                this.codeContext.popInserter();
            }
        }

        if (convertInserter2 != null) {
            this.codeContext.pushInserter(convertInserter2);
            try {
                this.numericPromotion(locatable, this.convertToPrimitiveNumericType(locatable, type2), promotedType);
            } finally {
                this.codeContext.popInserter();
            }
        }

        return promotedType;
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
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
        if (opcodes != null) {
            this.writeOpcodes(locatable, opcodes);
            return true;
        }
        return false;
    }
    private static final HashMap PRIMITIVE_WIDENING_CONVERSIONS = new HashMap();
    static { UnitCompiler.fillConversionMap(new Object[] {
        new byte[0],
        Descriptor.BYTE  + Descriptor.SHORT,

        Descriptor.BYTE  + Descriptor.INT,
        Descriptor.SHORT + Descriptor.INT,
        Descriptor.CHAR  + Descriptor.INT,

        new byte[] { Opcode.I2L },
        Descriptor.BYTE  + Descriptor.LONG,
        Descriptor.SHORT + Descriptor.LONG,
        Descriptor.CHAR  + Descriptor.LONG,
        Descriptor.INT   + Descriptor.LONG,

        new byte[] { Opcode.I2F },
        Descriptor.BYTE  + Descriptor.FLOAT,
        Descriptor.SHORT + Descriptor.FLOAT,
        Descriptor.CHAR  + Descriptor.FLOAT,
        Descriptor.INT   + Descriptor.FLOAT,

        new byte[] { Opcode.L2F },
        Descriptor.LONG  + Descriptor.FLOAT,

        new byte[] { Opcode.I2D },
        Descriptor.BYTE  + Descriptor.DOUBLE,
        Descriptor.SHORT + Descriptor.DOUBLE,
        Descriptor.CHAR  + Descriptor.DOUBLE,
        Descriptor.INT   + Descriptor.DOUBLE,

        new byte[] { Opcode.L2D },
        Descriptor.LONG  + Descriptor.DOUBLE,

        new byte[] { Opcode.F2D },
        Descriptor.FLOAT + Descriptor.DOUBLE,
    }, UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS); }
    private static void
    fillConversionMap(Object[] array, HashMap map) {
        byte[] opcodes = null;
        for (int i = 0; i < array.length; ++i) {
            Object o = array[i];
            if (o instanceof byte[]) {
                opcodes = (byte[]) o;
            } else {
                map.put(o, opcodes);
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
     * Performs "widening reference conversion" (5.1.4) if possible.
     *
     * @return Whether the conversion was possible
     */
    @SuppressWarnings("static-method") private boolean
    tryWideningReferenceConversion(IClass sourceType, IClass targetType) throws CompileException {
        if (targetType.isPrimitive() || sourceType == targetType) return false;

        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * Check whether "narrowing primitive conversion" (JLS 5.1.3) is possible.
     */
    @SuppressWarnings("static-method") private boolean
    isNarrowingPrimitiveConvertible(IClass sourceType, IClass targetType) {
        return UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.containsKey(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
    }

    /**
     * Implements "narrowing primitive conversion" (JLS 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private boolean
    tryNarrowingPrimitiveConversion(Locatable locatable, IClass sourceType, IClass targetType) {
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        );
        if (opcodes != null) {
            this.writeOpcodes(locatable, opcodes);
            return true;
        }
        return false;
    }

    private static final HashMap PRIMITIVE_NARROWING_CONVERSIONS = new HashMap();
    static { UnitCompiler.fillConversionMap(new Object[] {
        new byte[0],
        Descriptor.BYTE + Descriptor.CHAR,
        Descriptor.SHORT + Descriptor.CHAR,
        Descriptor.CHAR + Descriptor.SHORT,

        new byte[] { Opcode.I2B },
        Descriptor.SHORT + Descriptor.BYTE,
        Descriptor.CHAR + Descriptor.BYTE,
        Descriptor.INT + Descriptor.BYTE,

        new byte[] { Opcode.I2S },
        Descriptor.INT + Descriptor.SHORT,
        Descriptor.INT + Descriptor.CHAR,

        new byte[] { Opcode.L2I, Opcode.I2B },
        Descriptor.LONG + Descriptor.BYTE,

        new byte[] { Opcode.L2I, Opcode.I2S },
        Descriptor.LONG + Descriptor.SHORT,
        Descriptor.LONG + Descriptor.CHAR,

        new byte[] { Opcode.L2I },
        Descriptor.LONG + Descriptor.INT,

        new byte[] { Opcode.F2I, Opcode.I2B },
        Descriptor.FLOAT + Descriptor.BYTE,

        new byte[] { Opcode.F2I, Opcode.I2S },
        Descriptor.FLOAT + Descriptor.SHORT,
        Descriptor.FLOAT + Descriptor.CHAR,

        new byte[] { Opcode.F2I },
        Descriptor.FLOAT + Descriptor.INT,

        new byte[] { Opcode.F2L },
        Descriptor.FLOAT + Descriptor.LONG,

        new byte[] { Opcode.D2I, Opcode.I2B },
        Descriptor.DOUBLE + Descriptor.BYTE,

        new byte[] { Opcode.D2I, Opcode.I2S },
        Descriptor.DOUBLE + Descriptor.SHORT,
        Descriptor.DOUBLE + Descriptor.CHAR,

        new byte[] { Opcode.D2I },
        Descriptor.DOUBLE + Descriptor.INT,

        new byte[] { Opcode.D2L },
        Descriptor.DOUBLE + Descriptor.LONG,

        new byte[] { Opcode.D2F },
        Descriptor.DOUBLE + Descriptor.FLOAT,
    }, UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS); }

    /**
     * Check if "constant assignment conversion" (JLS 5.2, paragraph 1) is possible.
     *
     * @param constantValue The constant value that is to be converted
     * @param targetType The type to convert to
     */
    private boolean
    tryConstantAssignmentConversion(Locatable locatable, Object constantValue, IClass targetType)
    throws CompileException {
        if (UnitCompiler.DEBUG) {
            System.out.println("isConstantPrimitiveAssignmentConvertible(" + constantValue + ", " + targetType + ")");
        }

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
        if (targetType == icl.JAVA_LANG_BYTE && cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.BYTE, targetType);
            return true;
        }
        if (targetType == icl.JAVA_LANG_SHORT && cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.SHORT, targetType);
            return true;
        }
        if (targetType == icl.JAVA_LANG_CHARACTER && cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE) {
            this.boxingConversion(locatable, IClass.CHAR, targetType);
            return true;
        }

        return false;
    }

    /**
     * Check whether "narrowing reference conversion" (JLS 5.1.5) is possible.
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
        if (sourceType == this.iClassLoader.JAVA_LANG_OBJECT && targetType.isArray()) return true;

        // 5.1.5.4
        if (sourceType == this.iClassLoader.JAVA_LANG_OBJECT && targetType.isInterface()) return true;

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
            IClass tt = targetType.getComponentType();
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

        this.writeOpcode(locatable, Opcode.CHECKCAST);
        this.writeConstantClassInfo(targetType.getDescriptor());
        return true;
    }

    /**
     * JLS2 5.5
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
     * @return the boxed type or {@code null}
     */
    private IClass
    isBoxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == IClass.BOOLEAN) return icl.JAVA_LANG_BOOLEAN;
        if (sourceType == IClass.BYTE)    return icl.JAVA_LANG_BYTE;
        if (sourceType == IClass.CHAR)    return icl.JAVA_LANG_CHARACTER;
        if (sourceType == IClass.SHORT)   return icl.JAVA_LANG_SHORT;
        if (sourceType == IClass.INT)     return icl.JAVA_LANG_INTEGER;
        if (sourceType == IClass.LONG)    return icl.JAVA_LANG_LONG;
        if (sourceType == IClass.FLOAT)   return icl.JAVA_LANG_FLOAT;
        if (sourceType == IClass.DOUBLE)  return icl.JAVA_LANG_DOUBLE;
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

        // In some pre-1.5 JDKs, only some wrapper classes have the static "Target.valueOf(source)" method.
        if (targetType.hasIMethod("valueOf", new IClass[] { sourceType })) {
            this.writeOpcode(locatable, Opcode.INVOKESTATIC);
            this.writeConstantMethodrefInfo(
                targetType.getDescriptor(),                                         // classFD
                "valueOf",                                                          // methodName
                '(' + sourceType.getDescriptor() + ')' + targetType.getDescriptor() // methodFD
            );
            return;
        }
        // new Target(source)
        this.writeOpcode(locatable, Opcode.NEW);
        this.writeConstantClassInfo(targetType.getDescriptor());
        if (Descriptor.hasSize2(sourceType.getDescriptor())) {
            this.writeOpcode(locatable, Opcode.DUP_X2);
            this.writeOpcode(locatable, Opcode.DUP_X2);
            this.writeOpcode(locatable, Opcode.POP);
        } else
        {
            this.writeOpcode(locatable, Opcode.DUP_X1);
            this.writeOpcode(locatable, Opcode.SWAP);
        }
        this.writeOpcode(locatable, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            targetType.getDescriptor(),                               // classFD
            "<init>",                                                 // methodName
            '(' + sourceType.getDescriptor() + ')' + Descriptor.VOID // methodMD
        );
    }

    /**
     * @return the unboxed type or {@code null}
     */
    private IClass
    isUnboxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == icl.JAVA_LANG_BOOLEAN)   return IClass.BOOLEAN;
        if (sourceType == icl.JAVA_LANG_BYTE)      return IClass.BYTE;
        if (sourceType == icl.JAVA_LANG_CHARACTER) return IClass.CHAR;
        if (sourceType == icl.JAVA_LANG_SHORT)     return IClass.SHORT;
        if (sourceType == icl.JAVA_LANG_INTEGER)   return IClass.INT;
        if (sourceType == icl.JAVA_LANG_LONG)      return IClass.LONG;
        if (sourceType == icl.JAVA_LANG_FLOAT)     return IClass.FLOAT;
        if (sourceType == icl.JAVA_LANG_DOUBLE)    return IClass.DOUBLE;
        return null;
    }

    private boolean
    tryUnboxingConversion(Locatable locatable, IClass sourceType, IClass targetType) {
        if (this.isUnboxingConvertible(sourceType) == targetType) {
            this.unboxingConversion(locatable, sourceType, targetType);
            return true;
        }
        return false;
    }

    /**
     * @param targetType a primitive type (except VOID)
     * @param sourceType the corresponding wrapper type
     */
    private void
    unboxingConversion(Locatable locatable, IClass sourceType, IClass targetType) {

        // "source.targetValue()"
        this.writeOpcode(locatable, Opcode.INVOKEVIRTUAL);
        this.writeConstantMethodrefInfo(
            sourceType.getDescriptor(),       // classFD
            targetType.toString() + "Value",  // methodName
            "()" + targetType.getDescriptor() // methodFD
        );
    }

    /**
     * Attempts to load an {@link IClass} by fully-qualified name through {@link #iClassLoader}.
     * @param location TODO
     * @param identifiers       The fully qualified type name, e.g. '<code>{ "pkg", "Outer", "Inner" }</code>'
     *
     * @return                  {@code null} if a class with the given name could not be loaded
     * @throws CompileException The type exists, but a problem occurred when it was loaded
     */
    private IClass
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

//        // Compose the descriptor (like "La/b/c;") and remember the positions of the slashes (2 and 4).
//        int[]         slashes = new int[identifiers.length - 1];
//        StringBuilder sb      = new StringBuilder("L");
//        for (int i = 0;; ++i) {
//            sb.append(identifiers[i]);
//            if (i == identifiers.length - 1) break;
//            slashes[i] = sb.length();
//            sb.append('/');
//        }
//        sb.append(';');
//
//        // Attempt to load the IClass and replace dots with dollar signs, i.e.:
//        // La/b/c; La/b$c; La$b$c;
//        for (int j = slashes.length - 1;; --j) {
//            IClass result;
//            try {
//                result = this.iClassLoader.loadIClass(sb.toString());
//            } catch (ClassNotFoundException ex) {
//                // SUPPRESS CHECKSTYLE AvoidHidingCause
//                if (ex.getException() instanceof CompileException) throw (CompileException) ex.getException();
//                throw new CompileException(sb.toString(), null, ex);
//            }
//            if (result != null) return result;
//            if (j < 0) break;
//            sb.setCharAt(slashes[j], '$');
//        }
//        return null;
//    }

    // Load the value of a local variable onto the stack and return its type.
    private IClass
    load(Locatable locatable, LocalVariable localVariable) {
        this.load(locatable, localVariable.type, localVariable.getSlotIndex());
        return localVariable.type;
    }
    private void
    load(Locatable locatable, IClass type, int index) {
        if (index <= 3) {
            this.writeOpcode(locatable, Opcode.ILOAD_0 + 4 * ilfda(type) + index);
        } else
        if (index <= 255) {
            this.writeOpcode(locatable, Opcode.ILOAD + ilfda(type));
            this.writeByte(index);
        } else
        {
            this.writeOpcode(locatable, Opcode.WIDE);
            this.writeOpcode(locatable, Opcode.ILOAD + ilfda(type));
            this.writeShort(index);
        }
    }

    /**
     * Assign stack top value to the given local variable. (Assignment conversion takes effect.) If {@code
     * optionalConstantValue} is not {@code null}, then the top stack value is a constant value with that type and
     * value, and a narrowing primitive conversion as described in JLS 5.2 is applied.
     */
    private void
    store(Locatable locatable, IClass valueType, LocalVariable localVariable) {
        this.store(
            locatable,                           // l
            localVariable.type,          // lvType
            localVariable.getSlotIndex() // lvIndex
        );
    }
    private void
    store(Locatable locatable, IClass lvType, short lvIndex) {
        if (lvIndex <= 3) {
            this.writeOpcode(locatable, Opcode.ISTORE_0 + 4 * ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            this.writeOpcode(locatable, Opcode.ISTORE + ilfda(lvType));
            this.writeByte(lvIndex);
        } else
        {
            this.writeOpcode(locatable, Opcode.WIDE);
            this.writeOpcode(locatable, Opcode.ISTORE + ilfda(lvType));
            this.writeShort(lvIndex);
        }
    }

    private void
    dup(Locatable locatable, int n) {
        switch (n) {

        case 0:
            ;
            break;

        case 1:
            this.writeOpcode(locatable, Opcode.DUP);
            break;

        case 2:
            this.writeOpcode(locatable, Opcode.DUP2);
            break;

        default:
            throw new JaninoRuntimeException("dup(" + n + ")");
        }
    }
    private void
    dupx(Locatable locatable, IClass type, int x) {
        if (x < 0 || x > 2) throw new JaninoRuntimeException("SNO: x has value " + x);
        int dup  = Opcode.DUP  + x;
        int dup2 = Opcode.DUP2 + x;
        this.writeOpcode(locatable, type == IClass.LONG || type == IClass.DOUBLE ? dup2 : dup);
    }

    private void
    pop(Locatable locatable, IClass type) {
        if (type == IClass.VOID) return;
        this.writeOpcode(locatable, type == IClass.LONG || type == IClass.DOUBLE ? Opcode.POP2 : Opcode.POP);
    }

    private static int
    ilfd(final IClass t) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            return 0;
        }
        if (t == IClass.LONG)   return 1;
        if (t == IClass.FLOAT)  return 2;
        if (t == IClass.DOUBLE) return 3;
        throw new JaninoRuntimeException("Unexpected type \"" + t + "\"");
    }

    private static int
    ilfd(IClass t, int opcodeInt, int opcodeLong, int opcodeFloat, int opcodeDouble) {
        if (t == IClass.BYTE || t == IClass.CHAR || t == IClass.INT || t == IClass.SHORT || t == IClass.BOOLEAN) {
            return opcodeInt;
        }
        if (t == IClass.LONG)   return opcodeLong;
        if (t == IClass.FLOAT)  return opcodeFloat;
        if (t == IClass.DOUBLE) return opcodeDouble;
        throw new JaninoRuntimeException("Unexpected type \"" + t + "\"");
    }

    private static int
    ilfda(IClass t) { return !t.isPrimitive() ? 4 : UnitCompiler.ilfd(t); }

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
        throw new JaninoRuntimeException("Unexpected type \"" + t + "\"");
    }

    /**
     * Finds a named field in the given {@link IClass}. Honors superclasses and interfaces. See JLS 8.3.
     *
     * @return {@code null} if no field is found
     */
    private IClass.IField
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
        for (int i = 0; i < ifs.length; ++i) {
            IClass.IField f2 = this.findIField(ifs[i], name, location);
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
    private IClass
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
     * Find one class or interface declared in this compilation unit by name.
     *
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner".
     * @return {@code null} if a class or an interface with that name is not declared in this compilation unit
     */
    public IClass
    findClass(String className) {

        // Examine package name.
        String packageName = (
            this.compilationUnit.optionalPackageDeclaration == null ? null :
            this.compilationUnit.optionalPackageDeclaration.packageName
        );
        if (packageName != null) {
            if (!className.startsWith(packageName + '.')) return null;
            className = className.substring(packageName.length() + 1);
        }

        StringTokenizer st = new StringTokenizer(className, "$");
        TypeDeclaration td = this.compilationUnit.getPackageMemberTypeDeclaration(st.nextToken());
        if (td == null) return null;
        while (st.hasMoreTokens()) {
            td = td.getMemberTypeDeclaration(st.nextToken());
            if (td == null) return null;
        }
        return this.resolve(td);
    }

    /**
     * Equivalent to {@link #compileError(String, Location)} with a {@code null} location argument.
     */
    private void
    compileError(String message) throws CompileException { this.compileError(message, null); }

    /**
     * Issue a compile error with the given message. This is done through the {@link ErrorHandler} that was installed
     * through {@link #setCompileErrorHandler(ErrorHandler)}. Such a handler typically throws a {@link
     * CompileException}, but it may as well decide to return normally. Consequently, the calling code must be prepared
     * that {@link #compileError(String, Location)} returns normally, and must attempt to continue compiling.
     *
     * @param message The message to report
     * @param optionalLocation The location to report
     */
    private void
    compileError(String message, Location optionalLocation) throws CompileException {
        ++this.compileErrorCount;
        if (this.optionalCompileErrorHandler != null) {
            this.optionalCompileErrorHandler.handleError(message, optionalLocation);
        } else {
            throw new CompileException(message, optionalLocation);
        }
    }

    /**
     * Issues a warning with the given message an location an returns. This is done through a {@link WarningHandler}
     * that was installed through {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The {@code handle} argument qualifies the warning and is typically used by the {@link WarningHandler} to
     * suppress individual warnings.
     */
    private void
    warning(String handle, String message, Location optionalLocation) {
        if (this.optionalWarningHandler != null) {
            this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
        }
    }

    /**
     * Interface type for {@link UnitCompiler#setCompileErrorHandler}.
     */
    public
    interface ErrorHandler {
        void handleError(String message, Location optionalLocation) throws CompileException;
    }

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application my install its own
     * (thread-local) {@link ErrorHandler}.
     * <p>
     * Be aware that a single problem during compilation often causes a bunch of compile errors, so a good {@link
     * ErrorHandler} counts errors and throws a {@link CompileException} when a limit is reached.
     * <p>
     * If the given {@link ErrorHandler} does not throw {@link CompileException}s, then {@link
     * #compileUnit(boolean, boolean, boolean)} will throw one when the compilation of the unit is finished, and errors
     * had occurred. In other words: The {@link ErrorHandler} may throw a {@link CompileException} or not, but {@link
     * #compileUnit(boolean, boolean, boolean)} will definitely throw a {@link CompileException} if one or more compile
     * errors have occurred.
     *
     * @param optionalCompileErrorHandler {@code null} to restore the default behavior (throwing a {@link
     *                                    CompileException}
     */
    public void
    setCompileErrorHandler(ErrorHandler optionalCompileErrorHandler) {
        this.optionalCompileErrorHandler = optionalCompileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom {@link WarningHandler}.
     *
     * @param optionalWarningHandler {@code null} to indicate that no warnings be issued
     */
    public void
    setWarningHandler(WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    private CodeContext
    getCodeContext() {
        CodeContext res = this.codeContext;
        if (res == null) throw new JaninoRuntimeException("S.N.O.: Null CodeContext");
        return res;
    }

    private CodeContext
    replaceCodeContext(CodeContext newCodeContext) {
        CodeContext oldCodeContext = this.codeContext;
        this.codeContext = newCodeContext;
        return oldCodeContext;
    }

    private CodeContext
    createDummyCodeContext() { return new CodeContext(this.getCodeContext().getClassFile()); }

    private void
    writeByte(int v) {
        if (v > Byte.MAX_VALUE - Byte.MIN_VALUE) {
            throw new JaninoRuntimeException("Byte value out of legal range");
        }
        this.codeContext.write((short) -1, (byte) v);
    }
    private void
    writeShort(int v) {
        if (v > Short.MAX_VALUE - Short.MIN_VALUE) {
            throw new JaninoRuntimeException("Short value out of legal range");
        }
        this.codeContext.write((short) -1, (byte) (v >> 8), (byte) v);
    }
    private void
    writeInt(int v) {
        this.codeContext.write((short) -1, (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v);
    }

    private void
    writeOpcode(Locatable locatable, int opcode) {
        this.codeContext.write(locatable.getLocation().getLineNumber(), (byte) opcode);
    }

    private void
    writeOpcodes(Locatable locatable, byte[] opcodes) {
        this.codeContext.write(locatable.getLocation().getLineNumber(), opcodes);
    }

    private void
    writeBranch(Locatable locatable, int opcode, final CodeContext.Offset dst) {
        this.codeContext.writeBranch(locatable.getLocation().getLineNumber(), opcode, dst);
    }

    private void
    writeOffset(CodeContext.Offset src, final CodeContext.Offset dst) {
        this.codeContext.writeOffset((short) -1, src, dst);
    }

    // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

    private void
    writeConstantClassInfo(String descriptor) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantClassInfo(descriptor));
    }
    private void
    writeConstantFieldrefInfo(String classFd, String fieldName, String fieldFd) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantFieldrefInfo(classFd, fieldName, fieldFd));
    }
    private void
    writeConstantMethodrefInfo(String classFd, String methodName, String methodMd) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantMethodrefInfo(classFd, methodName, methodMd));
    }
    private void
    writeConstantInterfaceMethodrefInfo(String classFd, String methodName, String methodMd) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantInterfaceMethodrefInfo(classFd, methodName, methodMd));
    }
/* UNUSED
    private void writeConstantStringInfo(String value) {
        this.codeContext.writeShort((short) -1, this.addConstantStringInfo(value));
    }
*/
    private short
    addConstantStringInfo(String value) {
        return this.codeContext.getClassFile().addConstantStringInfo(value);
    }
/* UNUSED
    private void writeConstantIntegerInfo(int value) {
        this.codeContext.writeShort((short) -1, this.addConstantIntegerInfo(value));
    }
*/
    private short
    addConstantIntegerInfo(int value) {
        return this.codeContext.getClassFile().addConstantIntegerInfo(value);
    }
/* UNUSED
    private void writeConstantFloatInfo(float value) {
        this.codeContext.writeShort((short) -1, this.addConstantFloatInfo(value));
    }
*/
    private short
    addConstantFloatInfo(float value) {
        return this.codeContext.getClassFile().addConstantFloatInfo(value);
    }
    private void
    writeConstantLongInfo(long value) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantLongInfo(value));
    }
    private void
    writeConstantDoubleInfo(double value) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantDoubleInfo(value));
    }

    public CodeContext.Offset
    getWhereToBreak(BreakableStatement bs) {
        if (bs.whereToBreak == null) {
            bs.whereToBreak = this.codeContext.new Offset();
        }
        return bs.whereToBreak;
    }

    private TypeBodyDeclaration
    getDeclaringTypeBodyDeclaration(QualifiedThisReference qtr) throws CompileException {
        if (qtr.declaringTypeBodyDeclaration == null) {

            // Compile error if in static function context.
            Scope s;
            for (
                s = qtr.getEnclosingBlockStatement();
                !(s instanceof TypeBodyDeclaration);
                s = s.getEnclosingScope()
            );
            qtr.declaringTypeBodyDeclaration = (TypeBodyDeclaration) s;
            if (qtr.declaringTypeBodyDeclaration.isStatic()) {
                this.compileError("No current instance available in static method", qtr.getLocation());
            }

            // Determine declaring type.
            qtr.declaringClass = (ClassDeclaration) qtr.declaringTypeBodyDeclaration.getDeclaringType();
        }
        return qtr.declaringTypeBodyDeclaration;
    }

    private ClassDeclaration
    getDeclaringClass(QualifiedThisReference qtr) throws CompileException {
        if (qtr.declaringClass == null) {
            this.getDeclaringTypeBodyDeclaration(qtr);
        }
        return qtr.declaringClass;
    }

    private void
    referenceThis(Locatable locatable) { this.writeOpcode(locatable, Opcode.ALOAD_0); }

    /**
     * Expects {@code dimExprCount} values of type {@code int} on the operand stack. Creates an array of {@code
     * dimExprCount + dims} dimensions of {@code componentType}.
     *
     * @return The type of the created array
     */
    private IClass
    newArray(Locatable locatable, int dimExprCount, int dims, IClass componentType) {
        if (dimExprCount == 1 && dims == 0 && componentType.isPrimitive()) {

            // "new <primitive>[<size>]"
            this.writeOpcode(locatable, Opcode.NEWARRAY);
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
            return componentType.getArrayIClass(this.iClassLoader.JAVA_LANG_OBJECT);
        }

        if (dimExprCount == 1) {
            IClass at = componentType.getArrayIClass(dims, this.iClassLoader.JAVA_LANG_OBJECT);

            // "new <class-or-interface>[<size>]"
            // "new <anything>[<size>][]..."
            this.writeOpcode(locatable, Opcode.ANEWARRAY);
            this.writeConstantClassInfo(at.getDescriptor());
            return at.getArrayIClass(this.iClassLoader.JAVA_LANG_OBJECT);
        } else {
            IClass at = componentType.getArrayIClass(dimExprCount + dims, this.iClassLoader.JAVA_LANG_OBJECT);

            // "new <anything>[]..."
            // "new <anything>[<size1>][<size2>]..."
            // "new <anything>[<size1>][<size2>]...[]..."
            this.writeOpcode(locatable, Opcode.MULTIANEWARRAY);
            this.writeConstantClassInfo(at.getDescriptor());
            this.writeByte(dimExprCount);
            return at;
        }
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
        @Override public Object  getConstantValue() { return NOT_CONSTANT; }
        @Override public String  getName()          { return this.name; }
        @Override public IClass  getType()          { return this.type; }
        @Override public boolean isStatic()         { return false; }
        @Override public Access  getAccess()        { return Access.DEFAULT; }
    }

    private static Access
    modifiers2Access(short modifiers) {
        return (
            (modifiers & Mod.PUBLIC)    != 0 ? Access.PUBLIC    :
            (modifiers & Mod.PROTECTED) != 0 ? Access.PROTECTED :
            (modifiers & Mod.PRIVATE)   != 0 ? Access.PRIVATE   :
            Access.DEFAULT
        );
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

    private static String
    unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length();) {
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

            {
                int x = Character.digit(c, 8);
                if (x == -1) throw new JaninoRuntimeException("Invalid escape sequence '\\" + c + "'");
                if (i < s.length()) {
                    c = s.charAt(i);
                    int secondDigit = Character.digit(c, 8);
                    if (secondDigit != -1) {
                        x = 8 * x + secondDigit;
                        i++;
                        if (i < s.length() && x <= 037) {
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
        }
        return sb.toString();
    }

    // Used to write byte code while compiling one constructor/method.
    private CodeContext codeContext;

    // Used for elaborate compile error handling.
    private ErrorHandler optionalCompileErrorHandler;
    private int          compileErrorCount;

    // Used for elaborate warning handling.
    private WarningHandler optionalWarningHandler;

    public final CompilationUnit compilationUnit;

    private final IClassLoader iClassLoader;
    private List               generatedClassFiles;

    private boolean debugSource;
    private boolean debugLines;
    private boolean debugVars;

    private final Map/*<String staticMemberName, List <IField, IMethod, IClass>>*/ singleStaticImports = new HashMap();

    private final Collection staticImportsOnDemand = new ArrayList(); // IClass
}
