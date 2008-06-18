
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2007, Arno Unkrig
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.IClass.IInvocable;
import org.codehaus.janino.IClass.IMethod;
import org.codehaus.janino.Java.AlternateConstructorInvocation;
import org.codehaus.janino.Java.Block;
import org.codehaus.janino.Java.BlockStatement;
import org.codehaus.janino.Java.BreakStatement;
import org.codehaus.janino.Java.CatchClause;
import org.codehaus.janino.Java.ConstructorDeclarator;
import org.codehaus.janino.Java.ConstructorInvocation;
import org.codehaus.janino.Java.ContinueStatement;
import org.codehaus.janino.Java.DoStatement;
import org.codehaus.janino.Java.EmptyStatement;
import org.codehaus.janino.Java.ExpressionStatement;
import org.codehaus.janino.Java.FieldAccess;
import org.codehaus.janino.Java.FieldDeclaration;
import org.codehaus.janino.Java.ForStatement;
import org.codehaus.janino.Java.FunctionDeclarator;
import org.codehaus.janino.Java.IfStatement;
import org.codehaus.janino.Java.Initializer;
import org.codehaus.janino.Java.Invocation;
import org.codehaus.janino.Java.LabeledStatement;
import org.codehaus.janino.Java.LocalClassDeclarationStatement;
import org.codehaus.janino.Java.LocalVariable;
import org.codehaus.janino.Java.LocalVariableDeclarationStatement;
import org.codehaus.janino.Java.Locatable;
import org.codehaus.janino.Java.ReturnStatement;
import org.codehaus.janino.Java.Rvalue;
import org.codehaus.janino.Java.SimpleType;
import org.codehaus.janino.Java.Statement;
import org.codehaus.janino.Java.SuperConstructorInvocation;
import org.codehaus.janino.Java.SuperclassFieldAccessExpression;
import org.codehaus.janino.Java.SwitchStatement;
import org.codehaus.janino.Java.SynchronizedStatement;
import org.codehaus.janino.Java.ThrowStatement;
import org.codehaus.janino.Java.TryStatement;
import org.codehaus.janino.Java.WhileStatement;
import org.codehaus.janino.Java.CompilationUnit.ImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.SingleStaticImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.SingleTypeImportDeclaration;
import org.codehaus.janino.Java.CompilationUnit.StaticImportOnDemandDeclaration;
import org.codehaus.janino.Java.CompilationUnit.TypeImportOnDemandDeclaration;
import org.codehaus.janino.Visitor.BlockStatementVisitor;
import org.codehaus.janino.Visitor.ImportVisitor;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.enumerator.EnumeratorSet;

/**
 * This class actually implements the Java<sup>TM</sup> compiler. It is
 * associated with exactly one compilation unit which it compiles.
 */
public class UnitCompiler {
    private static final boolean DEBUG = false;

    /**
     * This constant determines the number of operands up to which the
     *
     *      a.concat(b).concat(c)
     *
     * strategy is used to implement string concatenation. For more operands, the
     *
     *      new StringBuffer(a).append(b).append(c).append(d).toString()
     *
     * strategy is chosen.
     *
     * A very good article from Tom Gibara
     *
     *    http://www.tomgibara.com/janino-evaluation/string-concatenation-benchmark
     *
     * analyzes the impact of this decision and recommends a value of three.
     */
    private static final int STRING_CONCAT_LIMIT = 3;

    public UnitCompiler(
        Java.CompilationUnit compilationUnit,
        IClassLoader         iClassLoader
    ) throws CompileException {
        this.compilationUnit = compilationUnit;
        this.iClassLoader    = iClassLoader;

        try {
            if (iClassLoader.loadIClass(Descriptor.STRING_BUILDER) != null) {
                this.isStringBuilderAvailable = true;
            } else
            if (iClassLoader.loadIClass(Descriptor.STRING_BUFFER) != null) {
                this.isStringBuilderAvailable = false;
            } else
            {
                throw new RuntimeException("SNO: Could neither load \"StringBuffer\" nor \"StringBuilder\"");
            }
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("SNO: Error loading \"StringBuffer\" or \"StringBuilder\": " + ex.getMessage());
        }

        // Compile non-static import declarations. (Must be done here in the constructor and not
        // down in "compileUnit()" because otherwise "resolve()" cannot resolve type names.)
        this.typeImportsOnDemand = new ArrayList();
        this.typeImportsOnDemand.add(new String[] { "java", "lang" });
        for (Iterator it = this.compilationUnit.importDeclarations.iterator(); it.hasNext();) {
            ImportDeclaration id = (ImportDeclaration) it.next();
            class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
            try {
                id.accept(new ImportVisitor() {
                    public void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          { try { UnitCompiler.this.import2(stid);  } catch (CompileException e) { throw new UCE(e); } }
                    public void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     {       UnitCompiler.this.import2(tiodd);                                                    }
                    public void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      {}
                    public void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) {}
                });
            } catch (UCE uce) { throw uce.ce; }
        }
    }

    private void import2(SingleTypeImportDeclaration stid) throws CompileException {
        String[] ids = stid.identifiers;
        String name = last(ids);

        String[] prev = (String[]) UnitCompiler.this.singleTypeImports.put(name, ids);

        // Check for re-import of same name.
        if (prev != null && !Arrays.equals(prev, ids)) {
            this.compileError("Class \"" + name + "\" was previously imported as \"" + Java.join(prev, ".") + "\", now as \"" + Java.join(ids, ".") + "\"");
        }
    }
    private void import2(TypeImportOnDemandDeclaration tiodd) {

        // Duplicate type-imports-on-demand are not an error, so no checks here.
        UnitCompiler.this.typeImportsOnDemand.add(tiodd.identifiers);
    }
    private void import2(SingleStaticImportDeclaration ssid) throws CompileException {
        String name = last(ssid.identifiers);
        Object importedObject;

        FIND_IMPORTED_OBJECT:
        {

            // Type?
            {
                IClass iClass = this.loadFullyQualifiedClass(ssid.identifiers);
                if (iClass != null) {
                    importedObject = iClass;
                    break FIND_IMPORTED_OBJECT;
                }
            }
    
            String[] typeName = allButLast(ssid.identifiers);
            IClass iClass = this.loadFullyQualifiedClass(typeName);
            if (iClass == null) {
                this.compileError("Could not load \"" + Java.join(typeName, ".") + "\"", ssid.getLocation());
                return;
            }
    
            // Static field?
            IField[] flds = iClass.getDeclaredIFields();
            for (int i = 0; i < flds.length; ++i) {
                IField iField = flds[i];
                if (iField.getName().equals(name)) {
                    if (!iField.isStatic()) {
                        this.compileError("Filed \"" + name + "\" of \"" + Java.join(typeName, ".") + "\" must be static", ssid.getLocation());
                    }
                    importedObject = iField;
                    break FIND_IMPORTED_OBJECT;
                }
            }
    
            // Static method?
            IMethod[] ms = iClass.getDeclaredIMethods(name);
            if (ms.length > 0) {
                importedObject = Arrays.asList(ms);
                break FIND_IMPORTED_OBJECT;
            }

            // Give up.
            this.compileError("\"" + Java.join(typeName, ".") + "\" has no static member \"" + name + "\"", ssid.getLocation());
            return;
        }

        Object prev = this.singleStaticImports.put(name, importedObject);

        // Check for re-import of same name.
        if (prev != null && !prev.equals(importedObject)) {
            UnitCompiler.this.compileError("\"" + name + "\" was previously statically imported as \"" + prev.toString() + "\", now as \"" + importedObject.toString() + "\"", ssid.getLocation());
        }
    }
    private void import2(StaticImportOnDemandDeclaration siodd) throws CompileException {
        IClass iClass = this.loadFullyQualifiedClass(siodd.identifiers);
        if (iClass == null) {
            this.compileError("Could not load \"" + Java.join(siodd.identifiers, ".") + "\"", siodd.getLocation());
            return;
        }
        UnitCompiler.this.staticImportsOnDemand.add(iClass);
    }

    /**
     * Generates an array of {@link ClassFile} objects which represent the classes and
     * interfaces defined in the compilation unit.
     */
    public ClassFile[] compileUnit(
        EnumeratorSet debuggingInformation
    ) throws CompileException {

        // Compile static import declarations.
        for (Iterator it = this.compilationUnit.importDeclarations.iterator(); it.hasNext();) {
            ImportDeclaration id = (ImportDeclaration) it.next();
            class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
            try {
                id.accept(new ImportVisitor() {
                    public void visitSingleTypeImportDeclaration(SingleTypeImportDeclaration stid)          {}
                    public void visitTypeImportOnDemandDeclaration(TypeImportOnDemandDeclaration tiodd)     {}
                    public void visitSingleStaticImportDeclaration(SingleStaticImportDeclaration ssid)      { try { UnitCompiler.this.import2(ssid);  } catch (CompileException e) { throw new UCE(e); } }
                    public void visitStaticImportOnDemandDeclaration(StaticImportOnDemandDeclaration siodd) { try { UnitCompiler.this.import2(siodd); } catch (CompileException e) { throw new UCE(e); } }
                });
            } catch (UCE uce) { throw uce.ce; }
        }

        this.generatedClassFiles  = new ArrayList();
        this.debuggingInformation = debuggingInformation;

        for (Iterator it = this.compilationUnit.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            UnitCompiler.this.compile((Java.PackageMemberTypeDeclaration) it.next());
        }

        if (this.compileErrorCount > 0) throw new CompileException(this.compileErrorCount + " error(s) while compiling unit \"" + this.compilationUnit.optionalFileName + "\"", null);

        List l = this.generatedClassFiles;
        return (ClassFile[]) l.toArray(new ClassFile[l.size()]);
    }

    // ------------ TypeDeclaration.compile() -------------

    private void compile(Java.TypeDeclaration td) throws CompileException {
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.TypeDeclarationVisitor tdv = new Visitor.TypeDeclarationVisitor() {
            public void visitAnonymousClassDeclaration        (Java.AnonymousClassDeclaration acd)          { try { UnitCompiler.this.compile2(acd                                     ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalClassDeclaration            (Java.LocalClassDeclaration lcd)              { try { UnitCompiler.this.compile2(lcd                                     ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitPackageMemberClassDeclaration    (Java.PackageMemberClassDeclaration pmcd)     { try { UnitCompiler.this.compile2((Java.PackageMemberTypeDeclaration) pmcd); } catch (CompileException e) { throw new UCE(e); } }
            public void visitMemberInterfaceDeclaration       (Java.MemberInterfaceDeclaration mid)         { try { UnitCompiler.this.compile2(mid                                     ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) { try { UnitCompiler.this.compile2((Java.PackageMemberTypeDeclaration) pmid); } catch (CompileException e) { throw new UCE(e); } }
            public void visitMemberClassDeclaration           (Java.MemberClassDeclaration mcd)             { try { UnitCompiler.this.compile2(mcd                                     ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            td.accept(tdv);
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    public void compile2(Java.PackageMemberTypeDeclaration pmtd) throws CompileException {
        Java.CompilationUnit declaringCompilationUnit = pmtd.getDeclaringCompilationUnit();

        // Check for conflict with single-type-import (7.6).
        {
            String[] ss = this.getSingleTypeImport(pmtd.getName());
            if (ss != null) this.compileError("Package member type declaration \"" + pmtd.getName() + "\" conflicts with single-type-import \"" + Java.join(ss, ".") + "\"", pmtd.getLocation());
        }

        // Check for redefinition within compilation unit (7.6).
        {
            Java.PackageMemberTypeDeclaration otherPMTD = declaringCompilationUnit.getPackageMemberTypeDeclaration(pmtd.getName());
            if (otherPMTD != pmtd) this.compileError("Redeclaration of type \"" + pmtd.getName() + "\", previously declared in " + otherPMTD.getLocation(), pmtd.getLocation());
        }

        if (pmtd instanceof Java.NamedClassDeclaration) {
            this.compile2((Java.NamedClassDeclaration) pmtd);
        } else
        if (pmtd instanceof Java.InterfaceDeclaration) {
            this.compile2((Java.InterfaceDeclaration) pmtd);
        } else
        {
            throw new RuntimeException("PMTD of unexpected type " + pmtd.getClass().getName());
        }
    }

    public void compile2(Java.ClassDeclaration cd) throws CompileException {
        IClass iClass = this.resolve(cd);

        // Check that all methods are implemented.
        if ((cd.modifiers & Mod.ABSTRACT) == 0) {
            IMethod[] ms = iClass.getIMethods();
            for (int i = 0; i <  ms.length; ++i) {
                if (ms[i].isAbstract()) this.compileError("Non-abstract class \"" + iClass + "\" must implement method \"" + ms[i] + "\"", cd.getLocation());
            }
        }

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            (short) (cd.modifiers | Mod.SUPER),           // accessFlags
            iClass.getDescriptor(),                       // thisClassFD
            iClass.getSuperclass().getDescriptor(),       // superClassFD
            IClass.getDescriptors(iClass.getInterfaces()) // interfaceFDs
        );

        // Add InnerClasses attribute entry for this class declaration.
        if (cd.getEnclosingScope() instanceof Java.CompilationUnit) {
            ;
        } else
        if (cd.getEnclosingScope() instanceof Java.Block) {
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short innerNameIndex = (
                this instanceof Java.NamedTypeDeclaration ?
                cf.addConstantUtf8Info(((Java.NamedTypeDeclaration) this).getName()) :
                (short) 0
            );
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex, // innerClassInfoIndex
                (short) 0,           // outerClassInfoIndex
                innerNameIndex,      // innerNameIndex
                cd.modifiers         // innerClassAccessFlags
            ));
        } else
        if (cd.getEnclosingScope() instanceof Java.AbstractTypeDeclaration) {
            short innerClassInfoIndex = cf.addConstantClassInfo(iClass.getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(((Java.AbstractTypeDeclaration) cd.getEnclosingScope())).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(((Java.MemberTypeDeclaration) cd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex, // innerClassInfoIndex
                outerClassInfoIndex, // outerClassInfoIndex
                innerNameIndex,      // innerNameIndex
                cd.modifiers         // innerClassAccessFlags
            ));
        }

        // Set "SourceFile" attribute.
        if (this.debuggingInformation.contains(DebuggingInformation.SOURCE)) {
            String sourceFileName;
            {
                String s = cd.getLocation().getFileName();
                if (s != null) {
                    sourceFileName = new File(s).getName();
                } else if (cd instanceof Java.NamedTypeDeclaration) {
                    sourceFileName = ((Java.NamedTypeDeclaration) cd).getName() + ".java";
                } else {
                    sourceFileName = "ANONYMOUS.java";
                }
            }
            cf.addSourceFileAttribute(sourceFileName);
        }

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (cd instanceof Java.DocCommentable) {
            if (((Java.DocCommentable) cd).hasDeprecatedDocTag()) cf.addDeprecatedAttribute();
        }

        // Optional: Generate and compile class initialization method.
        {
            Java.Block b = new Java.Block(cd.getLocation());
            for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
                if (tbd.isStatic()) b.addButDontEncloseStatement((Java.BlockStatement) tbd);
            }

            this.maybeCreateInitMethod(cd, cf, b);
        }

        this.compileDeclaredMethods(cd, cf);

        
        // Compile declared constructors.
        // As a side effect of compiling methods and constructors, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly. 
        // We need to note how many we have here so we can compile the extras.
        int declaredMethodCount = cd.declaredMethods.size();
        {
            int syntheticFieldCount = cd.syntheticFields.size();
            Java.ConstructorDeclarator[] cds = cd.getConstructors();
            for (int i = 0; i < cds.length; ++i) {
                this.compile(cds[i], cf);
                if (syntheticFieldCount != cd.syntheticFields.size()) throw new RuntimeException("SNO: Compilation of constructor \"" + cds[i] + "\" (" + cds[i].getLocation() +") added synthetic fields!?");
            }
        }

        // A side effect of this call may create synthetic functions to access
        // protected parent variables
        this.compileDeclaredMemberTypes(cd, cf);

        // Compile the aforementioned extras.
        this.compileDeclaredMethods(cd, cf, declaredMethodCount);

        // Class and instance variables.
        for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
            if (!(tbd instanceof Java.FieldDeclaration)) continue;
            this.addFields((Java.FieldDeclaration) tbd, cf);
        }

        // Synthetic fields.
        for (Iterator it = cd.syntheticFields.values().iterator(); it.hasNext();) {
            IClass.IField f = (IClass.IField) it.next();
            cf.addFieldInfo(
                Mod.PACKAGE,                 // modifiers,
                f.getName(),                 // fieldName,
                f.getType().getDescriptor(), // fieldTypeFD,
                null                         // optionalConstantValue
            );
        }


        // Add the generated class file to a thread-local store.
        this.generatedClassFiles.add(cf);
    }

    /**
     * Create {@link ClassFile.FieldInfo}s for all fields declared by the
     * given {@link Java.FieldDeclaration}.
     */
    private void addFields(Java.FieldDeclaration fd, ClassFile cf) throws CompileException {
        for (int j = 0; j < fd.variableDeclarators.length; ++j) {
            Java.VariableDeclarator vd = fd.variableDeclarators[j];
            Java.Type type = fd.type;
            for (int k = 0; k < vd.brackets; ++k) type = new Java.ArrayType(type);

            Object ocv = null;
            if (
                (fd.modifiers & Mod.FINAL) != 0 &&
                vd.optionalInitializer != null
            ) {
                if (vd.optionalInitializer instanceof Java.Rvalue) ocv = this.getConstantValue((Java.Rvalue) vd.optionalInitializer);
                if (ocv == Java.Rvalue.CONSTANT_VALUE_NULL) ocv = null;
            }

            ClassFile.FieldInfo fi;
            if (Mod.isPrivateAccess(fd.modifiers)) {

                // To make the private field accessible for enclosing types, enclosed types and types
                // enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                fi = cf.addFieldInfo(
                    Mod.changeAccess(fd.modifiers, Mod.PACKAGE), // modifiers
                    vd.name,                                     // fieldName
                    this.getType(type).getDescriptor(),          // fieldTypeFD
                    ocv                                          // optionalConstantValue
                );
            } else
            {
                fi = cf.addFieldInfo(
                    fd.modifiers,                       // modifiers
                    vd.name,                            // fieldName
                    this.getType(type).getDescriptor(), // fieldTypeFD
                    ocv                                 // optionalConstantValue
                );
            }

            // Add "Deprecated" attribute (JVMS 4.7.10)
            if (fd.hasDeprecatedDocTag()) {
                fi.addAttribute(new ClassFile.DeprecatedAttribute(cf.addConstantUtf8Info("Deprecated")));
            }
        }
    }

    public void compile2(Java.AnonymousClassDeclaration acd) throws CompileException {
        this.compile2((Java.InnerClassDeclaration) acd);
    }

    public void compile2(Java.LocalClassDeclaration lcd) throws CompileException {
        this.compile2((Java.InnerClassDeclaration) lcd);
    }

    public void compile2(Java.InnerClassDeclaration icd) throws CompileException {

        // Define a synthetic "this$..." field if there is an enclosing instance.
        {
            List ocs = UnitCompiler.getOuterClasses(icd);
            final int nesting = ocs.size();
            if (nesting >= 2) {
                icd.defineSyntheticField(new SimpleIField(
                    this.resolve(icd),
                    "this$" + (nesting - 2),
                    this.resolve((Java.AbstractTypeDeclaration) ocs.get(1))
                ));
            }
        }

        this.compile2((Java.ClassDeclaration) icd);
    }

    public void compile2(final Java.MemberClassDeclaration mcd) throws CompileException {
        this.compile2((Java.InnerClassDeclaration) mcd);
    }

    public void compile2(Java.InterfaceDeclaration id) throws CompileException {
        IClass iClass = this.resolve(id);

        // Determine extended interfaces.
        id.interfaces = new IClass[id.extendedTypes.length];
        String[] interfaceDescriptors = new String[id.interfaces.length];
        for (int i = 0; i < id.extendedTypes.length; ++i) {
            id.interfaces[i] = this.getType(id.extendedTypes[i]);
            interfaceDescriptors[i] = id.interfaces[i].getDescriptor();
        }

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            (short) (                         // accessFlags
                id.modifiers |
                Mod.SUPER |
                Mod.INTERFACE |
                Mod.ABSTRACT
            ),
            iClass.getDescriptor(),           // thisClassFD
            Descriptor.OBJECT,                // superClassFD
            interfaceDescriptors              // interfaceFDs
        );

        // Set "SourceFile" attribute.
        if (this.debuggingInformation.contains(DebuggingInformation.SOURCE)) {
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
            Java.Block b = new Java.Block(id.getLocation());
            b.addButDontEncloseStatements(id.constantDeclarations);

            maybeCreateInitMethod(id, cf, b);
        }

        compileDeclaredMethods(id, cf);

        // Class variables.
        for (int i = 0; i < id.constantDeclarations.size(); ++i) {
            Java.BlockStatement bs = (Java.BlockStatement) id.constantDeclarations.get(i);
            if (!(bs instanceof Java.FieldDeclaration)) continue;
            this.addFields((Java.FieldDeclaration) bs, cf);
        }

        compileDeclaredMemberTypes(id, cf);

        // Add the generated class file to a thread-local store.
        this.generatedClassFiles.add(cf);
    }

    /**
     * Create class initialization method iff there is any initialization code.
     * @param decl  The type declaration
     * @param cf    The class file into which to put the method
     * @param b     The block for the method (possibly empty)
     * @throws CompileException
     */
    private void maybeCreateInitMethod(Java.AbstractTypeDeclaration decl,
            ClassFile cf, Java.Block b) throws CompileException {
        // Create interface initialization method iff there is any initialization code.
        if (this.generatesCode(b)) {
            Java.MethodDeclarator md = new Java.MethodDeclarator(
                decl.getLocation(),                               // location
                null,                                           // optionalDocComment
                (short) (Mod.STATIC | Mod.PUBLIC),              // modifiers
                new Java.BasicType(                             // type
                    decl.getLocation(),
                    Java.BasicType.VOID
                ),
                "<clinit>",                                     // name
                new Java.FunctionDeclarator.FormalParameter[0], // formalParameters
                new Java.ReferenceType[0],                      // thrownExcaptions
                b                                               // optionalBody
            );
            md.setDeclaringType(decl);
            this.compile(md, cf);
        }
    }

    /**
     * Compile all of the types for this declaration
     * <p>
     * NB: as a side effect this will fill in the sythetic field map
     * @throws CompileException
     */
    private void compileDeclaredMemberTypes(
        Java.AbstractTypeDeclaration decl,
        ClassFile                    cf
    ) throws CompileException {
        for (Iterator it = decl.getMemberTypeDeclarations().iterator(); it.hasNext();) {
            Java.AbstractTypeDeclaration atd = ((Java.AbstractTypeDeclaration) it.next());
            this.compile(atd);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(atd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(decl).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(((Java.MemberTypeDeclaration) atd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex, // innerClassInfoIndex
                outerClassInfoIndex, // outerClassInfoIndex
                innerNameIndex,      // innerNameIndex
                decl.modifiers         // innerClassAccessFlags
            ));
        }
    }
    
    /**
     * Compile all of the methods for this declaration
     * <p>
     * NB: as a side effect this will fill in the sythetic field map
     * @throws CompileException
     */
    private void compileDeclaredMethods(
        Java.AbstractTypeDeclaration typeDeclaration,
        ClassFile                    cf
    ) throws CompileException {
        compileDeclaredMethods(typeDeclaration, cf, 0);
    }

    /**
     * Compile methods for this declaration starting at startPos
     * @param startPos starting param to fill in
     * @throws CompileException
     */
    private void compileDeclaredMethods(
        Java.AbstractTypeDeclaration typeDeclaration,
        ClassFile                    cf,
        int                          startPos
    ) throws CompileException {

        // Notice that as a side effect of compiling methods, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly. Hence, we
        // must not use an Iterator here.
        for (int i = startPos; i < typeDeclaration.declaredMethods.size(); ++i) {
            this.compile(((Java.MethodDeclarator) typeDeclaration.declaredMethods.get(i)), cf);
        }
    }

    /**
     * @return <tt>false</tt> if this statement cannot complete normally (JLS2
     * 14.20)
     */
    private boolean compile(Java.BlockStatement bs) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.BlockStatementVisitor bsv = new Visitor.BlockStatementVisitor() {
            public void visitInitializer                      (Java.Initializer                       i   ) { try { res[0] = UnitCompiler.this.compile2(i   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldDeclaration                 (Java.FieldDeclaration                  fd  ) { try { res[0] = UnitCompiler.this.compile2(fd  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLabeledStatement                 (Java.LabeledStatement                  ls  ) { try { res[0] = UnitCompiler.this.compile2(ls  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBlock                            (Java.Block                             b   ) { try { res[0] = UnitCompiler.this.compile2(b   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitExpressionStatement              (Java.ExpressionStatement               es  ) { try { res[0] = UnitCompiler.this.compile2(es  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitIfStatement                      (Java.IfStatement                       is  ) { try { res[0] = UnitCompiler.this.compile2(is  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitForStatement                     (Java.ForStatement                      fs  ) { try { res[0] = UnitCompiler.this.compile2(fs  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitWhileStatement                   (Java.WhileStatement                    ws  ) { try { res[0] = UnitCompiler.this.compile2(ws  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitTryStatement                     (Java.TryStatement                      ts  ) { try { res[0] = UnitCompiler.this.compile2(ts  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSwitchStatement                  (Java.SwitchStatement                   ss  ) { try { res[0] = UnitCompiler.this.compile2(ss  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSynchronizedStatement            (Java.SynchronizedStatement             ss  ) { try { res[0] = UnitCompiler.this.compile2(ss  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitDoStatement                      (Java.DoStatement                       ds  ) { try { res[0] = UnitCompiler.this.compile2(ds  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) { try { res[0] = UnitCompiler.this.compile2(lvds); } catch (CompileException e) { throw new UCE(e); } }
            public void visitReturnStatement                  (Java.ReturnStatement                   rs  ) { try { res[0] = UnitCompiler.this.compile2(rs  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitThrowStatement                   (Java.ThrowStatement                    ts  ) { try { res[0] = UnitCompiler.this.compile2(ts  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBreakStatement                   (Java.BreakStatement                    bs  ) { try { res[0] = UnitCompiler.this.compile2(bs  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitContinueStatement                (Java.ContinueStatement                 cs  ) { try { res[0] = UnitCompiler.this.compile2(cs  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitEmptyStatement                   (Java.EmptyStatement                    es  ) {       res[0] = UnitCompiler.this.compile2(es  );                                                                }
            public void visitLocalClassDeclarationStatement   (Java.LocalClassDeclarationStatement    lcds) { try { res[0] = UnitCompiler.this.compile2(lcds); } catch (CompileException e) { throw new UCE(e); } }
            public void visitAlternateConstructorInvocation   (Java.AlternateConstructorInvocation    aci ) { try { res[0] = UnitCompiler.this.compile2(aci ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperConstructorInvocation       (Java.SuperConstructorInvocation        sci ) { try { res[0] = UnitCompiler.this.compile2(sci ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private boolean compile2(Java.Initializer i) throws CompileException {
        return this.compile(i.block);
    }
    private boolean compile2(Java.Block b) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {
            boolean previousStatementCanCompleteNormally = true;
            for (int i = 0; i < b.statements.size(); ++i) {
                Java.BlockStatement bs = (Java.BlockStatement) b.statements.get(i);
                if (!previousStatementCanCompleteNormally && this.generatesCode(bs)) {
                    this.compileError("Statement is unreachable", bs.getLocation());
                    break;
                }
                previousStatementCanCompleteNormally = this.compile(bs);
            }
            return previousStatementCanCompleteNormally;
        } finally {
            this.codeContext.restoreLocalVariables();
        }
    }
    private boolean compile2(Java.DoStatement ds) throws CompileException {
        Object cvc = this.getConstantValue(ds.condition);
        if (cvc != null) {
            if (Boolean.TRUE.equals(cvc)) {
                this.warning("DSTC", "Condition of DO statement is always TRUE; the proper way of declaring an unconditional loop is \"for (;;)\"", ds.getLocation());
                return this.compileUnconditionalLoop(ds, ds.body, null);
            } else
            {
                this.warning("DSNR", "DO statement never repeats", ds.getLocation());
            }
        }

        ds.whereToContinue = this.codeContext.new Offset();
        ds.bodyHasContinue = false;

        CodeContext.Offset bodyOffset = this.codeContext.newOffset();

        // Compile body.
        if (!this.compile(ds.body) && !ds.bodyHasContinue) {
            this.warning("DSNTC", "\"do\" statement never tests its condition", ds.getLocation());
            if (ds.whereToBreak == null) return false;
            ds.whereToBreak.set();
            return true;
        }

        // Compile condition.
        ds.whereToContinue.set();
        this.compileBoolean(ds.condition, bodyOffset, Java.Rvalue.JUMP_IF_TRUE);

        if (ds.whereToBreak != null) ds.whereToBreak.set();

        return true;
    }
    private boolean compile2(Java.ForStatement fs) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {

            // Compile init.
            if (fs.optionalInit != null) this.compile(fs.optionalInit);

            if (fs.optionalCondition == null) {
                return this.compileUnconditionalLoop(fs, fs.body, fs.optionalUpdate);
            } else
            {
                Object cvc = this.getConstantValue(fs.optionalCondition);
                if (cvc != null) {
                    if (Boolean.TRUE.equals(cvc)) {
                        this.warning("FSTC", "Condition of FOR statement is always TRUE; the proper way of declaring an unconditional loop is \"for (;;)\"", fs.getLocation());
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
            fs.whereToContinue = this.codeContext.new Offset();
            fs.bodyHasContinue = false;
            CodeContext.Offset bodyOffset = this.codeContext.newOffset();
            boolean bodyCCN = this.compile(fs.body);

            // Compile update.
            fs.whereToContinue.set();
            if (fs.optionalUpdate != null) {
                if (!bodyCCN && !fs.bodyHasContinue) {
                    this.warning("FUUR", "For update is unreachable", fs.getLocation());
                } else
                {
                    for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                        this.compile(fs.optionalUpdate[i]);
                    }
                }
            }

            // Compile condition.
            toCondition.set();
            this.compileBoolean(fs.optionalCondition, bodyOffset, Java.Rvalue.JUMP_IF_TRUE);
        } finally {
            this.codeContext.restoreLocalVariables();
        }

        if (fs.whereToBreak != null) fs.whereToBreak.set();

        return true;
    }
    private boolean compile2(Java.WhileStatement ws) throws CompileException {
        Object cvc = this.getConstantValue(ws.condition);
        if (cvc != null) {
            if (Boolean.TRUE.equals(cvc)) {
                this.warning("WSTC", "Condition of WHILE statement is always TRUE; the proper way of declaring an unconditional loop is \"for (;;)\"", ws.getLocation());
                return this.compileUnconditionalLoop(ws, ws.body, null);
            } else
            {
                this.warning("WSNR", "WHILE statement never repeats", ws.getLocation());
            }
        }

        ws.whereToContinue = this.codeContext.new Offset();
        ws.bodyHasContinue = false;
        this.writeBranch(ws, Opcode.GOTO, ws.whereToContinue);

        // Compile body.
        CodeContext.Offset bodyOffset = this.codeContext.newOffset();
        this.compile(ws.body); // Return value (CCN) is ignored.

        // Compile condition.
        ws.whereToContinue.set();
        this.compileBoolean(ws.condition, bodyOffset, Java.Rvalue.JUMP_IF_TRUE);

        if (ws.whereToBreak != null) ws.whereToBreak.set();
        return true;
    }
    private boolean compileUnconditionalLoop(
        Java.ContinuableStatement cs,
        Java.BlockStatement       body,
        Java.Rvalue[]             optionalUpdate
    ) throws CompileException {
        if (optionalUpdate != null) return this.compileUnconditionalLoopWithUpdate(cs, body, optionalUpdate);

        cs.whereToContinue = this.codeContext.newOffset();
        cs.bodyHasContinue = false;

        // Compile body.
        if (this.compile(body)) this.writeBranch(cs, Opcode.GOTO, cs.whereToContinue);

        if (cs.whereToBreak == null) return false;
        cs.whereToBreak.set();
        return true;
    }
    private boolean compileUnconditionalLoopWithUpdate(
        Java.ContinuableStatement cs,
        Java.BlockStatement       body,
        Java.Rvalue[]             update
    ) throws CompileException {
        cs.whereToContinue = this.codeContext.new Offset();
        cs.bodyHasContinue = false;

        // Compile body.
        CodeContext.Offset bodyOffset = this.codeContext.newOffset();
        boolean bodyCCN = this.compile(body);

        // Compile the "update".
        cs.whereToContinue.set();
        if (!bodyCCN && !cs.bodyHasContinue) {
            this.warning("LUUR", "Loop update is unreachable", update[0].getLocation());
        } else
        {
            for (int i = 0; i < update.length; ++i) this.compile(update[i]);
            this.writeBranch(cs, Opcode.GOTO, bodyOffset);
        }

        if (cs.whereToBreak == null) return false;
        cs.whereToBreak.set();
        return true;
    }

    private final boolean compile2(Java.LabeledStatement ls) throws CompileException {
        boolean canCompleteNormally = this.compile(ls.body);
        if (ls.whereToBreak != null) {
            ls.whereToBreak.set();
            canCompleteNormally = true;
        }
        return canCompleteNormally;
    }
    private boolean compile2(Java.SwitchStatement ss) throws CompileException {

        // Compute condition.
        IClass switchExpressionType = this.compileGetValue(ss.condition);
        this.assignmentConversion(
            (Locatable) ss,       // l
            switchExpressionType, // sourceType
            IClass.INT,           // targetType
            null                  // optionalConstantValue
        );

        // Prepare the map of case labels to code offsets.
        TreeMap              caseLabelMap = new TreeMap(); // Integer => Offset
        CodeContext.Offset   defaultLabelOffset = null;
        CodeContext.Offset[] sbsgOffsets = new CodeContext.Offset[ss.sbsgs.size()];
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            Java.SwitchStatement.SwitchBlockStatementGroup sbsg = (Java.SwitchStatement.SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i] = this.codeContext.new Offset();
            for (int j = 0; j < sbsg.caseLabels.size(); ++j) {

                // Verify that case label value is a constant.
                Java.Rvalue rv = (Java.Rvalue) sbsg.caseLabels.get(j);
                Object cv = this.getConstantValue(rv);
                if (cv == null) {
                    this.compileError("Value of \"case\" label does not pose a constant value", rv.getLocation());
                    cv = new Integer(99);
                }

                // Verify that case label is assignable to the type of the switch expression.
                IClass rvType = this.getType(rv);
                this.assignmentConversion(
                    (Locatable) ss,       // l
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
                    this.compileError("Value of case label must be a char, byte, short or int constant", rv.getLocation());
                    civ = new Integer(99);
                }

                // Store in case label map.
                if (caseLabelMap.containsKey(civ)) this.compileError("Duplicate \"case\" switch label value", rv.getLocation());
                caseLabelMap.put(civ, sbsgOffsets[i]);
            }
            if (sbsg.hasDefaultLabel) {
                if (defaultLabelOffset != null) this.compileError("Duplicate \"default\" switch label", sbsg.getLocation());
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
            ((Integer) caseLabelMap.firstKey()).intValue() + caseLabelMap.size() >= // Beware of INT overflow!
            ((Integer) caseLabelMap.lastKey()).intValue() - caseLabelMap.size()
        ) {
            int low = ((Integer) caseLabelMap.firstKey()).intValue();
            int high = ((Integer) caseLabelMap.lastKey()).intValue();

            this.writeOpcode(ss, Opcode.TABLESWITCH);
            new Java.Padder(this.codeContext).set();
            this.writeOffset(switchOffset, defaultLabelOffset);
            this.writeInt(low);
            this.writeInt(high);
            Iterator si = caseLabelMap.entrySet().iterator();
            int cur = low;
            while (si.hasNext()) {
                Map.Entry me = (Map.Entry) si.next();
                int val = ((Integer) me.getKey()).intValue();
                while (cur < val) {
                    this.writeOffset(switchOffset, defaultLabelOffset);
                    ++cur;
                }
                this.writeOffset(switchOffset, (CodeContext.Offset) me.getValue());
                ++cur;
            }
        } else {
            this.writeOpcode(ss, Opcode.LOOKUPSWITCH);
            new Java.Padder(this.codeContext).set();
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
            Java.SwitchStatement.SwitchBlockStatementGroup sbsg = (Java.SwitchStatement.SwitchBlockStatementGroup) ss.sbsgs.get(i);
            sbsgOffsets[i].set();
            canCompleteNormally = true;
            for (int j = 0; j < sbsg.blockStatements.size(); ++j) {
                Java.BlockStatement bs = (Java.BlockStatement) sbsg.blockStatements.get(j);
                if (!canCompleteNormally) {
                    this.compileError("Statement is unreachable", bs.getLocation());
                    break;
                }
                canCompleteNormally = this.compile(bs);
            }
        }
        if (ss.whereToBreak != null) {
            ss.whereToBreak.set();
            canCompleteNormally = true;
        }
        return canCompleteNormally;
    }
    private boolean compile2(Java.BreakStatement bs) throws CompileException {

        // Find the broken statement.
        Java.BreakableStatement brokenStatement = null;
        if (bs.optionalLabel == null) {
            for (
                Java.Scope s = bs.getEnclosingScope();
                s instanceof Java.Statement || s instanceof Java.CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof Java.BreakableStatement) {
                    brokenStatement = (Java.BreakableStatement) s;
                    break;
                }
            }
            if (brokenStatement == null) {
                this.compileError("\"break\" statement is not enclosed by a breakable statement", bs.getLocation());
                return false;
            }
        } else {
            for (
                Java.Scope s = bs.getEnclosingScope();
                s instanceof Java.Statement || s instanceof Java.CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof Java.LabeledStatement) {
                    Java.LabeledStatement ls = (Java.LabeledStatement) s;
                    if (ls.label.equals(bs.optionalLabel)) {
                        brokenStatement = ls;
                        break;
                    }
                }
            }
            if (brokenStatement == null) {
                this.compileError("Statement \"break " + bs.optionalLabel + "\" is not enclosed by a breakable statement with label \"" + bs.optionalLabel + "\"", bs.getLocation());
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
    private boolean compile2(Java.ContinueStatement cs) throws CompileException {

        // Find the continued statement.
        Java.ContinuableStatement continuedStatement = null;
        if (cs.optionalLabel == null) {
            for (
                Java.Scope s = cs.getEnclosingScope();
                s instanceof Java.Statement || s instanceof Java.CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof Java.ContinuableStatement) {
                    continuedStatement = (Java.ContinuableStatement) s;
                    break;
                }
            }
            if (continuedStatement == null) {
                this.compileError("\"continue\" statement is not enclosed by a continuable statement", cs.getLocation());
                return false;
            }
        } else {
            for (
                Java.Scope s = cs.getEnclosingScope();
                s instanceof Java.Statement || s instanceof Java.CatchClause;
                s = s.getEnclosingScope()
            ) {
                if (s instanceof Java.LabeledStatement) {
                    Java.LabeledStatement ls = (Java.LabeledStatement) s;
                    if (ls.label.equals(cs.optionalLabel)) {
                        Java.Statement st = ls.body;
                        while (st instanceof Java.LabeledStatement) st = ((Java.LabeledStatement) st).body;
                        if (!(st instanceof Java.ContinuableStatement)) {
                            this.compileError("Labeled statement is not continuable", st.getLocation());
                            return false;
                        }
                        continuedStatement = (Java.ContinuableStatement) st;
                        break;
                    }
                }
            }
            if (continuedStatement == null) {
                this.compileError("Statement \"continue " + cs.optionalLabel + "\" is not enclosed by a continuable statement with label \"" + cs.optionalLabel + "\"", cs.getLocation());
                return false;
            }
        }

        continuedStatement.bodyHasContinue = true;
        this.leaveStatements(
            cs.getEnclosingScope(),                 // from
            continuedStatement.getEnclosingScope(), // to
            null                                    // optionalStackValueType
        );
        this.writeBranch(cs, Opcode.GOTO, continuedStatement.whereToContinue);
        return false;
    }
    private boolean compile2(Java.EmptyStatement es) {
        return true;
    }
    private boolean compile2(Java.ExpressionStatement ee) throws CompileException {
        this.compile(ee.rvalue);
        return true;
    }
    private boolean compile2(Java.FieldDeclaration fd) throws CompileException {
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            Java.VariableDeclarator vd = fd.variableDeclarators[i];

            Java.ArrayInitializerOrRvalue initializer = this.getNonConstantFinalInitializer(fd, vd);
            if (initializer == null) continue;

            if ((fd.modifiers & Mod.STATIC) == 0) {
                this.writeOpcode(fd, Opcode.ALOAD_0);
            }
            IClass fieldType = this.getType(fd.type);
            if (initializer instanceof Java.Rvalue) {
                Java.Rvalue rvalue = (Java.Rvalue) initializer;
                IClass initializerType = this.compileGetValue(rvalue);
                fieldType = fieldType.getArrayIClass(vd.brackets, this.iClassLoader.OBJECT);
                this.assignmentConversion(
                    (Locatable) fd,               // l
                    initializerType,              // sourceType
                    fieldType,                    // destinationType
                    this.getConstantValue(rvalue) // optionalConstantValue
                );
            } else
            if (initializer instanceof Java.ArrayInitializer) {
                this.compileGetValue((Java.ArrayInitializer) initializer, fieldType);
            } else
            {
                throw new RuntimeException("Unexpected array initializer or rvalue class " + initializer.getClass().getName());
            }

            // No need to check accessibility here.
            ;

            if ((fd.modifiers & Mod.STATIC) != 0) {
                this.writeOpcode(fd, Opcode.PUTSTATIC);
            } else {
                this.writeOpcode(fd, Opcode.PUTFIELD);
            }
            this.writeConstantFieldrefInfo(
                this.resolve(fd.getDeclaringType()).getDescriptor(),
                vd.name, // classFD
                fieldType.getDescriptor()                            // fieldFD
            );
        }
        return true;
    }
    private boolean compile2(Java.IfStatement is) throws CompileException {
        Object cv = this.getConstantValue(is.condition);
        Java.BlockStatement es = (
            is.optionalElseStatement != null
            ? is.optionalElseStatement
            : new Java.EmptyStatement(is.thenStatement.getLocation())
        );
        if (cv instanceof Boolean) {

            // Constant condition.
            this.fakeCompile(is.condition);
            Java.BlockStatement seeingStatement, blindStatement;
            if (((Boolean) cv).booleanValue()) {
                seeingStatement = is.thenStatement;
                blindStatement  = es;
            } else {
                seeingStatement = es;
                blindStatement  = is.thenStatement;
            }

            // Compile the seeing statement.
            CodeContext.Inserter ins = this.codeContext.newInserter();
            boolean ssccn = this.compile(seeingStatement);
            if (ssccn) return true;

            // Hm... the "seeing statement" cannot complete normally. Things are getting
            // complicated here! The robust solution is to compile the constant-condition-IF
            // statement as a non-constant-condition-IF statement. As an optimization, iff the
            // IF-statement is enclosed ONLY by blocks, then the remaining bytecode can be
            // written to a "fake" code context, i.e. be thrown away.

            // Constant-condition-IF statement only enclosed by blocks?
            Java.Scope s = is.getEnclosingScope();
            while (s instanceof Java.Block) s = s.getEnclosingScope();
            if (s instanceof Java.FunctionDeclarator) {

                // Yes, compile rest of method to /dev/null.
                throw UnitCompiler.STOP_COMPILING_CODE;
            } else
            {

                // Compile constant-condition-IF statement as non-constant-condition-IF statement.
                CodeContext.Offset off = this.codeContext.newOffset();
                this.codeContext.pushInserter(ins);
                try {
                    this.pushConstant(is, new Integer(0));
                    this.writeBranch((Locatable) is, Opcode.IFNE, off);
                } finally {
                    this.codeContext.popInserter();
                }
            }
            return this.compile(blindStatement);
        }

        // Non-constant condition.
        if (this.generatesCode(is.thenStatement)) {
            if (this.generatesCode(es)) {

                // if (expr) stmt else stmt
                CodeContext.Offset eso = this.codeContext.new Offset();
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, eso, Java.Rvalue.JUMP_IF_FALSE);
                boolean tsccn = this.compile(is.thenStatement);
                if (tsccn) this.writeBranch((Locatable) is, Opcode.GOTO, end);
                eso.set();
                boolean esccn = this.compile(es);
                end.set();
                return tsccn || esccn;
            } else {

                // if (expr) stmt else ;
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, end, Java.Rvalue.JUMP_IF_FALSE);
                this.compile(is.thenStatement);
                end.set();
                return true;
            }
        } else {
            if (this.generatesCode(es)) {

                // if (expr) ; else stmt
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, end, Java.Rvalue.JUMP_IF_TRUE);
                this.compile(es);
                end.set();
                return true;
            } else {

                // if (expr) ; else ;
                IClass conditionType = this.compileGetValue(is.condition);
                if (conditionType != IClass.BOOLEAN) this.compileError("Not a boolean expression", is.getLocation());
                this.pop((Locatable) is, conditionType);
                return true;
            }
        }
    }
    private static final RuntimeException STOP_COMPILING_CODE = new RuntimeException("SNO: This exception should have been caught and processed");

    private boolean compile2(Java.LocalClassDeclarationStatement lcds) throws CompileException {

        // Check for redefinition.
        Java.LocalClassDeclaration otherLCD = this.findLocalClassDeclaration(lcds, lcds.lcd.name);
        if (otherLCD != lcds.lcd) this.compileError("Redeclaration of local class \"" + lcds.lcd.name + "\"; previously declared in " + otherLCD.getLocation());

        this.compile(lcds.lcd);
        return true;
    }

    /**
     * Find a local class declared in any block enclosing the given block statement.
     */
    private Java.LocalClassDeclaration findLocalClassDeclaration(Java.Scope s, String name) {
        for (;;) {
            Java.Scope es = s.getEnclosingScope();
            if (es instanceof Java.CompilationUnit) break;
            if (s instanceof Java.BlockStatement && es instanceof Java.Block) {
                Java.BlockStatement bs = (Java.BlockStatement) s;
                Java.Block          b = (Java.Block) es;
                for (Iterator it = b.statements.iterator(); it.hasNext();) {
                    Java.BlockStatement bs2 = (Java.BlockStatement) it.next();
                    if (bs2 instanceof Java.LocalClassDeclarationStatement) {
                        Java.LocalClassDeclarationStatement lcds = ((Java.LocalClassDeclarationStatement) bs2);
                        if (lcds.lcd.name.equals(name)) return lcds.lcd;
                    }
                    if (bs2 == bs) break;
                }
            }
            s = es;
        }
        return null;
    }

    private boolean compile2(Java.LocalVariableDeclarationStatement lvds) throws CompileException {
        if ((lvds.modifiers & ~Mod.FINAL) != 0) this.compileError("The only allowed modifier in local variable declarations is \"final\"", lvds.getLocation());

        for (int j = 0; j < lvds.variableDeclarators.length; ++j) {
            Java.VariableDeclarator vd = lvds.variableDeclarators[j];

            Java.LocalVariable lv = this.getLocalVariable(lvds, vd);
            lv.localVariableArrayIndex = this.codeContext.allocateLocalVariable(Descriptor.size(lv.type.getDescriptor()));

            if (vd.optionalInitializer != null) {
                if (vd.optionalInitializer instanceof Java.Rvalue) {
                    Java.Rvalue rhs = (Java.Rvalue) vd.optionalInitializer;
                    this.assignmentConversion(
                        (Locatable) lvds,          // l
                        this.compileGetValue(rhs), // sourceType
                        lv.type,                   // targetType
                        this.getConstantValue(rhs) // optionalConstantValue
                    );
                } else
                if (vd.optionalInitializer instanceof Java.ArrayInitializer) {
                    this.compileGetValue((Java.ArrayInitializer) vd.optionalInitializer, lv.type);
                } else
                {
                    throw new RuntimeException("Unexpected rvalue or array initialized class " + vd.optionalInitializer.getClass().getName());
                }
                this.store(
                    (Locatable) lvds, // l
                    lv.type,          // valueType
                    lv                // localVariable
                );
            }
        }
        return true;
    }

    public Java.LocalVariable getLocalVariable(
        Java.LocalVariableDeclarationStatement lvds,
        Java.VariableDeclarator                vd
    ) throws CompileException {
        if (vd.localVariable == null) {

            // Determine variable type.
            Java.Type variableType = lvds.type;
            for (int k = 0; k < vd.brackets; ++k) variableType = new Java.ArrayType(variableType);

            vd.localVariable = new Java.LocalVariable((lvds.modifiers & Mod.FINAL) != 0, this.getType(variableType));
        }
        return vd.localVariable;
    }

    private boolean compile2(Java.ReturnStatement rs) throws CompileException {

        // Determine enclosing block, function and compilation Unit.
        Java.FunctionDeclarator enclosingFunction = null;
        {
            Java.Scope s = rs.getEnclosingScope();
            for (s = s.getEnclosingScope(); s instanceof Java.Statement || s instanceof Java.CatchClause; s = s.getEnclosingScope());
            enclosingFunction = (Java.FunctionDeclarator) s;
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
            (Locatable) rs,                               // l
            type,                                         // sourceType
            returnType,                                   // targetType
            this.getConstantValue(rs.optionalReturnValue) // optionalConstantValue
        );

        this.leaveStatements(
            rs.getEnclosingScope(), // from
            enclosingFunction,      // to
            returnType              // optionalStackValueType
        );
        this.writeOpcode(rs, Opcode.IRETURN + this.ilfda(returnType));
        return false;
    }
    private boolean compile2(Java.SynchronizedStatement ss) throws CompileException {

        // Evaluate monitor object expression.
        if (!this.iClassLoader.OBJECT.isAssignableFrom(this.compileGetValue(ss.expression))) this.compileError("Monitor object of \"synchronized\" statement is not a subclass of \"Object\"", ss.getLocation());

        this.codeContext.saveLocalVariables();
        boolean canCompleteNormally = false;
        try {

            // Allocate a local variable for the monitor object.
            ss.monitorLvIndex = this.codeContext.allocateLocalVariable((short) 1);

            // Store the monitor object.
            this.writeOpcode(ss, Opcode.DUP);
            this.store((Locatable) ss, this.iClassLoader.OBJECT, ss.monitorLvIndex);

            // Create lock on the monitor object.
            this.writeOpcode(ss, Opcode.MONITORENTER);

            // Compile the statement body.
            CodeContext.Offset monitorExitOffset = this.codeContext.new Offset();
            CodeContext.Offset beginningOfBody = this.codeContext.newOffset();
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
            this.leave(ss, this.iClassLoader.THROWABLE);
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
    private boolean compile2(Java.ThrowStatement ts) throws CompileException {
        IClass expressionType = this.compileGetValue(ts.expression);
        this.checkThrownException(
            (Locatable) ts,        // l
            expressionType,        // type
            ts.getEnclosingScope() // scope
        );
        this.writeOpcode(ts, Opcode.ATHROW);
        return false;
    }
    private boolean compile2(Java.TryStatement ts) throws CompileException {
        if (ts.optionalFinally != null) ts.finallyOffset = this.codeContext.new Offset();

        CodeContext.Offset beginningOfBody = this.codeContext.newOffset();
        CodeContext.Offset afterStatement = this.codeContext.new Offset();

        this.codeContext.saveLocalVariables();
        try {

            // Allocate a LV for the JSR of the FINALLY clause.
            //
            // Notice:
            //   For unclear reasons, this variable must not overlap with any of the body's
            //   variables (although the body's variables are out of scope when it comes to the
            //   FINALLY clause!?), otherwise you get
            //     java.lang.VerifyError: ... Accessing value from uninitialized localvariable 4
            //   See bug #56.
            short pcLVIndex = ts.optionalFinally != null ? this.codeContext.allocateLocalVariable((short) 1) : (short) 0;

            boolean canCompleteNormally = this.compile(ts.body);
            CodeContext.Offset afterBody = this.codeContext.newOffset();
            if (canCompleteNormally) {
                this.writeBranch(ts, Opcode.GOTO, afterStatement);
            }

            if (beginningOfBody.offset != afterBody.offset) { // Avoid zero-length exception table entries.
                this.codeContext.saveLocalVariables();
                try {

                    // Allocate the "exception variable".
                    short evi = this.codeContext.allocateLocalVariable((short) 1);

                    for (int i = 0; i < ts.catchClauses.size(); ++i) {
                        Java.CatchClause cc = (Java.CatchClause) ts.catchClauses.get(i);
                        IClass caughtExceptionType = this.getType(cc.caughtException.type);
                        this.codeContext.addExceptionTableEntry(
                            beginningOfBody,                    // startPC
                            afterBody,                          // endPC
                            this.codeContext.newOffset(),       // handlerPC
                            caughtExceptionType.getDescriptor() // catchTypeFD
                        );
                        this.store(
                            (Locatable) cc,      // l
                            caughtExceptionType, // lvType
                            evi                  // lvIndex
                        );
        
                        // Kludge: Treat the exception variable like a local
                        // variable of the catch clause body.
                        UnitCompiler.this.getLocalVariable(cc.caughtException).localVariableArrayIndex = evi;
        
                        if (this.compile(cc.body)) {
                            canCompleteNormally = true;
                            if (
                                i < ts.catchClauses.size() - 1 ||
                                ts.optionalFinally != null
                            ) this.writeBranch(cc, Opcode.GOTO, afterStatement);
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
                        (Locatable) ts.optionalFinally, // l
                        this.iClassLoader.OBJECT,       // valueType
                        evi                             // localVariableIndex
                    );
                    this.writeBranch(ts.optionalFinally, Opcode.JSR, ts.finallyOffset);
                    this.load(
                        (Locatable) ts.optionalFinally, // l
                        this.iClassLoader.OBJECT,       // valueType
                        evi                             // localVariableIndex
                    );
                    this.writeOpcode(ts.optionalFinally, Opcode.ATHROW);
    
                    // Compile the "finally" body.
                    ts.finallyOffset.set();
                    this.store(
                        (Locatable) ts.optionalFinally, // l
                        this.iClassLoader.OBJECT,       // valueType
                        pcLVIndex                       // localVariableIndex
                    );
                    if (this.compile(ts.optionalFinally)) {
                        this.writeOpcode(ts.optionalFinally, Opcode.RET);
                        this.writeByte(pcLVIndex);
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

    private void compile(Java.FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        ClassFile.MethodInfo mi;

        if (Mod.isPrivateAccess(fd.modifiers)) {
            if (fd instanceof Java.MethodDeclarator && !fd.isStatic()){

                // To make the non-static private method invocable for enclosing types, enclosed types
                // and types enclosed by the same type, it is modified as follows:
                //  + Access is changed from PRIVATE to PACKAGE
                //  + The name is appended with "$"
                //  + It is made static
                //  + A parameter of type "declaring class" is prepended to the signature
                mi = classFile.addMethodInfo(
                    (short) (Mod.changeAccess(fd.modifiers, Mod.PACKAGE) | Mod.STATIC), // accessFlags
                    fd.name + '$',                               // name
                    MethodDescriptor.prependParameter(           // methodMD
                        this.toIMethod((Java.MethodDeclarator) fd).getDescriptor(),
                        this.resolve(fd.getDeclaringType()).getDescriptor()
                    )
                );
            } else
            {

                // To make the static private method or private constructor invocable for enclosing
                // types, enclosed types and types enclosed by the same type, it is modified as
                // follows:
                //  + Access is changed from PRIVATE to PACKAGE
                mi = classFile.addMethodInfo(
                    Mod.changeAccess(fd.modifiers, Mod.PACKAGE), // accessFlags
                    fd.name,                                     // name
                    this.toIInvocable(fd).getDescriptor()        // methodMD
                );
            }
        } else
        {
            mi = classFile.addMethodInfo(
                fd.modifiers,                         // accessFlags
                fd.name,                              // name
                this.toIInvocable(fd).getDescriptor() // methodMD
            );
        }

        // Add "Exceptions" attribute (JVMS 4.7.4).
        {
            final short eani = classFile.addConstantUtf8Info("Exceptions");
            short[] tecciis = new short[fd.thrownExceptions.length];
            for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                tecciis[i] = classFile.addConstantClassInfo(this.getType(fd.thrownExceptions[i]).getDescriptor());
            }
            mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, tecciis));
        }

        // Add "Deprecated" attribute (JVMS 4.7.10)
        if (fd.hasDeprecatedDocTag()) {
            mi.addAttribute(new ClassFile.DeprecatedAttribute(classFile.addConstantUtf8Info("Deprecated")));
        }

        if ((fd.modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) return;

        // Create CodeContext.
        final CodeContext codeContext = new CodeContext(mi.getClassFile());

        CodeContext savedCodeContext = this.replaceCodeContext(codeContext);
        try {

            // Define special parameter "this".
            if ((fd.modifiers & Mod.STATIC) == 0) {
                this.codeContext.allocateLocalVariable((short) 1);
            }

            if (fd instanceof Java.ConstructorDeclarator) {
                Java.ConstructorDeclarator constructorDeclarator = (Java.ConstructorDeclarator) fd;

                // Reserve space for synthetic parameters ("this$...", "val$...").
                for (Iterator it = constructorDeclarator.getDeclaringClass().syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    Java.LocalVariable lv = new Java.LocalVariable(true, sf.getType());
                    lv.localVariableArrayIndex = this.codeContext.allocateLocalVariable(Descriptor.size(sf.getDescriptor()));
                    constructorDeclarator.syntheticParameters.put(sf.getName(), lv);
                }
            }

            this.buildLocalVariableMap(fd);

            // Compile the constructor preamble.
            if (fd instanceof Java.ConstructorDeclarator) {
                Java.ConstructorDeclarator cd = (Java.ConstructorDeclarator) fd;
                if (cd.optionalConstructorInvocation != null) {
                    this.compile(cd.optionalConstructorInvocation);
                    if (cd.optionalConstructorInvocation instanceof Java.SuperConstructorInvocation) {
                        this.assignSyntheticParametersToSyntheticFields(cd);
                        this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                    }
                } else {

                    // Determine qualification for superconstructor invocation.
                    Java.QualifiedThisReference qualification = null;
                    IClass outerClassOfSuperclass = this.resolve(cd.getDeclaringClass()).getSuperclass().getOuterIClass();
                    if (outerClassOfSuperclass != null) {
//                        qualification = new Java.QualifiedThisReference(
//                            cd.getLocation(),        // location
//                            cd.getDeclaringClass(),  // declaringClass
//                            cd,                      // declaringTypeBodyDeclaration
//                            outerClassOfSuperclass   // targetIClass
//                        );
                        qualification = new Java.QualifiedThisReference(
                            cd.getLocation(),    // location
                            new Java.SimpleType( // qualification
                                cd.getLocation(),
                                outerClassOfSuperclass
                            )
                        );
                    }

                    // Invoke the superconstructor.
                    Java.SuperConstructorInvocation sci = new Java.SuperConstructorInvocation(
                        cd.getLocation(),       // location
                        qualification,          // optionalQualification
                        new Java.Rvalue[0]      // arguments
                    );
                    sci.setEnclosingScope(fd);
                    this.compile(sci);
                    this.assignSyntheticParametersToSyntheticFields(cd);
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                }
            }

            // Compile the function body.
            try {
                if (fd.optionalBody == null) {
                    this.compileError("Method must have a body", fd.getLocation());
                }
                boolean canCompleteNormally = this.compile(fd.optionalBody);
                if (canCompleteNormally) {
                    if (this.getReturnType(fd) != IClass.VOID) this.compileError("Method must return a value", fd.getLocation());
                    this.writeOpcode(fd, Opcode.RETURN);
                }
            } catch (RuntimeException ex) {
                if (ex != UnitCompiler.STOP_COMPILING_CODE) throw ex;

                // In very special circumstances (e.g. "if (true) return;"), code generation is
                // terminated abruptly by throwing STOP_COMPILING_CODE.
                ;
            }
        } finally {
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

        // Add the code context as a code attribute to the MethodInfo.
        final short lntani = (
            this.debuggingInformation.contains(DebuggingInformation.LINES) ?
            classFile.addConstantUtf8Info("LineNumberTable") :
            (short) 0
        );
        mi.addAttribute(new ClassFile.AttributeInfo(classFile.addConstantUtf8Info("Code")) {
            protected void storeBody(DataOutputStream dos) throws IOException {
                codeContext.storeCodeAttributeBody(dos, lntani);
            }
        });
    }

    private void buildLocalVariableMap(FunctionDeclarator fd) throws CompileException {
        Map localVars = new HashMap();

        // Add function parameters.
        for (int i = 0; i < fd.formalParameters.length; ++i) {
            Java.FunctionDeclarator.FormalParameter fp = fd.formalParameters[i];
            if (localVars.containsKey(fp.name)) this.compileError("Redefinition of formal parameter \"" + fp.name + "\"", fd.getLocation());
            Java.LocalVariable lv = this.getLocalVariable(fp);
            lv.localVariableArrayIndex = this.codeContext.allocateLocalVariable(Descriptor.size(lv.type.getDescriptor()));
            
            localVars.put(fp.name, lv);
        }

        fd.localVariables = localVars;
        if (fd instanceof ConstructorDeclarator) {
            ConstructorDeclarator cd = (ConstructorDeclarator) fd;
            if (cd.optionalConstructorInvocation != null) {
                buildLocalVariableMap(cd.optionalConstructorInvocation, localVars);
            }
        }
        if (fd.optionalBody != null) this.buildLocalVariableMap(fd.optionalBody, localVars);
    }

    private Map buildLocalVariableMap(BlockStatement bs, final Map localVars) throws CompileException {
        final Map[] resVars = new Map[] { localVars };
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        BlockStatementVisitor bsv = new BlockStatementVisitor() {
            // basic statements that use the default handlers
            public void visitAlternateConstructorInvocation(AlternateConstructorInvocation aci) { UnitCompiler.this.buildLocalVariableMap(aci, localVars); }
            public void visitBreakStatement(BreakStatement bs)                                  { UnitCompiler.this.buildLocalVariableMap(bs, localVars); }
            public void visitContinueStatement(ContinueStatement cs)                            { UnitCompiler.this.buildLocalVariableMap(cs, localVars); }
            public void visitEmptyStatement(EmptyStatement es)                                  { UnitCompiler.this.buildLocalVariableMap(es, localVars); }
            public void visitExpressionStatement(ExpressionStatement es)                        { UnitCompiler.this.buildLocalVariableMap(es, localVars); }
            public void visitFieldDeclaration(FieldDeclaration fd)                              { UnitCompiler.this.buildLocalVariableMap(fd, localVars); }
            public void visitReturnStatement(ReturnStatement rs)                                { UnitCompiler.this.buildLocalVariableMap(rs, localVars); }
            public void visitSuperConstructorInvocation(SuperConstructorInvocation sci)         { UnitCompiler.this.buildLocalVariableMap(sci, localVars); }
            public void visitThrowStatement(ThrowStatement ts)                                  { UnitCompiler.this.buildLocalVariableMap(ts, localVars); }
            public void visitLocalClassDeclarationStatement(LocalClassDeclarationStatement lcds){ UnitCompiler.this.buildLocalVariableMap(lcds, localVars); }
            
            // more complicated statements with specialized handlers, but don't add new variables in this scope
            public void visitBlock(Block b)              					 { try { UnitCompiler.this.buildLocalVariableMap(b , localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitDoStatement(DoStatement ds)                     { try { UnitCompiler.this.buildLocalVariableMap(ds, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitForStatement(ForStatement fs)                   { try { UnitCompiler.this.buildLocalVariableMap(fs, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitIfStatement(IfStatement is)                     { try { UnitCompiler.this.buildLocalVariableMap(is, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitInitializer(Initializer i)                      { try { UnitCompiler.this.buildLocalVariableMap(i , localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSwitchStatement(SwitchStatement ss)             { try { UnitCompiler.this.buildLocalVariableMap(ss, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSynchronizedStatement(SynchronizedStatement ss) { try { UnitCompiler.this.buildLocalVariableMap(ss, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitTryStatement(TryStatement ts)                   { try { UnitCompiler.this.buildLocalVariableMap(ts, localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitWhileStatement(WhileStatement ws)               { try { UnitCompiler.this.buildLocalVariableMap(ws, localVars); } catch (CompileException e) { throw new UCE(e); } }
            
            // more complicated statements with specialized handlers, that can add variables in this scope
            public void visitLabeledStatement(LabeledStatement ls)                                     { try { resVars[0] = UnitCompiler.this.buildLocalVariableMap(ls  , localVars); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableDeclarationStatement(LocalVariableDeclarationStatement lvds) { try { resVars[0] = UnitCompiler.this.buildLocalVariableMap(lvds, localVars); } catch (CompileException e) { throw new UCE(e); } }
        };
        try { bs.accept(bsv); } catch(UCE uce) { throw uce.ce; }
        return resVars[0];
    }
    // default handlers
    private Map buildLocalVariableMap(Statement s, final Map localVars)              { return s.localVariables = localVars; }
    private Map buildLocalVariableMap(ConstructorInvocation ci, final Map localVars) { return ci.localVariables = localVars; }
    
    // specialized handlers
    private void buildLocalVariableMap(Block block, Map localVars) throws CompileException {
        block.localVariables = localVars;
        for (Iterator it = block.statements.iterator(); it.hasNext();) {
            BlockStatement bs = (BlockStatement)it.next();
            localVars = this.buildLocalVariableMap(bs, localVars);
        }
    }
    private void buildLocalVariableMap(DoStatement ds, final Map localVars) throws CompileException {
        ds.localVariables = localVars;
        this.buildLocalVariableMap(ds.body, localVars);
    }
    private void buildLocalVariableMap(ForStatement fs, final Map localVars) throws CompileException {
        Map inner = localVars;
        if(fs.optionalInit != null) {
            inner = UnitCompiler.this.buildLocalVariableMap(fs.optionalInit, localVars);
        }
        fs.localVariables = inner;
        UnitCompiler.this.buildLocalVariableMap(fs.body, inner);
    }
    private void buildLocalVariableMap(IfStatement is, final Map localVars) throws CompileException {
        is.localVariables = localVars;
        UnitCompiler.this.buildLocalVariableMap(is.thenStatement, localVars);
        if(is.optionalElseStatement != null) {
            UnitCompiler.this.buildLocalVariableMap(is.optionalElseStatement, localVars);
        }
    }
    private void buildLocalVariableMap(Initializer i, final Map localVars) throws CompileException {
        UnitCompiler.this.buildLocalVariableMap(i.block, localVars);
    }
    private void buildLocalVariableMap(SwitchStatement ss, final Map localVars) throws CompileException {
        ss.localVariables = localVars;
        Map vars = localVars;
        for (Iterator cases = ss.sbsgs.iterator(); cases.hasNext();) {
            SwitchStatement.SwitchBlockStatementGroup sbsg = (SwitchStatement.SwitchBlockStatementGroup) cases.next();
            for (Iterator stmts = sbsg.blockStatements.iterator(); stmts.hasNext();) {
                BlockStatement bs = (BlockStatement) stmts.next();
                vars = UnitCompiler.this.buildLocalVariableMap(bs, vars);
            }
        }
    }
    private void buildLocalVariableMap(SynchronizedStatement ss, final Map localVars) throws CompileException {
        ss.localVariables = localVars;
        UnitCompiler.this.buildLocalVariableMap(ss.body, localVars);
    }
    private void buildLocalVariableMap(TryStatement ts, final Map localVars) throws CompileException {
        ts.localVariables = localVars;
        UnitCompiler.this.buildLocalVariableMap(ts.body, localVars);
        for (Iterator it = ts.catchClauses.iterator(); it.hasNext();) {
            Java.CatchClause cc = (Java.CatchClause) it.next();
            UnitCompiler.this.buildLocalVariableMap(cc, localVars);
        }
        if(ts.optionalFinally != null) {
            UnitCompiler.this.buildLocalVariableMap(ts.optionalFinally, localVars);
        }
    }
    private void buildLocalVariableMap(WhileStatement ws, final Map localVars) throws CompileException {
        ws.localVariables = localVars;
        UnitCompiler.this.buildLocalVariableMap(ws.body, localVars);
    }
    
    private Map buildLocalVariableMap(LabeledStatement ls, final Map localVars) throws CompileException {
        ls.localVariables = localVars;
        return UnitCompiler.this.buildLocalVariableMap((BlockStatement)ls.body, localVars);
    }
    private Map buildLocalVariableMap(LocalVariableDeclarationStatement lvds, final Map localVars) throws CompileException {
        Map newVars = new HashMap();
        newVars.putAll(localVars);
        for(int i = 0; i < lvds.variableDeclarators.length; ++i) {
            Java.VariableDeclarator vd = lvds.variableDeclarators[i];
            Java.LocalVariable lv = UnitCompiler.this.getLocalVariable(lvds, vd);
            if(newVars.containsKey(vd.name)) this.compileError("Redefinition of local variable \"" + vd.name + "\" ", vd.getLocation());
            newVars.put(vd.name, lv);
        }
        lvds.localVariables = newVars;
        return newVars;
    }
    protected void buildLocalVariableMap(CatchClause cc, Map localVars) throws CompileException {
        Map vars = new HashMap();
        vars.putAll(localVars);
        LocalVariable lv = this.getLocalVariable(cc.caughtException);
        vars.put(cc.caughtException.name, lv);
        this.buildLocalVariableMap(cc.body, vars);
    }



    public Java.LocalVariable getLocalVariable(Java.FunctionDeclarator.FormalParameter fp) throws CompileException {
        if (fp.localVariable == null) {
            fp.localVariable = new Java.LocalVariable(fp.finaL, this.getType(fp.type));
        }
        return fp.localVariable;
    }

    // ------------------ Rvalue.compile() ----------------

    /**
     * Call to check whether the given {@link Java.Rvalue} compiles or not.
     */
    private void fakeCompile(Java.Rvalue rv) throws CompileException {
        CodeContext savedCodeContext = this.replaceCodeContext(this.createDummyCodeContext());
        try {
            this.compileContext(rv);
            this.compileGet(rv);
        } finally {
            this.replaceCodeContext(savedCodeContext);
        }
    }

    /**
     * Some {@link Java.Rvalue}s compile more efficiently when their value
     * is not needed, e.g. "i++".
     */
    private void compile(Java.Rvalue rv) throws CompileException {
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength                   (Java.ArrayLength                    al  ) { try { UnitCompiler.this.compile2(al  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitAssignment                    (Java.Assignment                     a   ) { try { UnitCompiler.this.compile2(a   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) { try { UnitCompiler.this.compile2(uo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) { try { UnitCompiler.this.compile2(bo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCast                          (Java.Cast                           c   ) { try { UnitCompiler.this.compile2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) { try { UnitCompiler.this.compile2(cl  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) { try { UnitCompiler.this.compile2(ce  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCrement                       (Java.Crement                        c   ) { try { UnitCompiler.this.compile2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitInstanceof                    (Java.Instanceof                     io  ) { try { UnitCompiler.this.compile2(io  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) { try { UnitCompiler.this.compile2(mi  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) { try { UnitCompiler.this.compile2(smi ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLiteral                       (Java.Literal                        l   ) { try { UnitCompiler.this.compile2(l   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) { try { UnitCompiler.this.compile2(naci); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewArray                      (Java.NewArray                       na  ) { try { UnitCompiler.this.compile2(na  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewInitializedArray           (Java.NewInitializedArray            nia ) { try { UnitCompiler.this.compile2(nia ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) { try { UnitCompiler.this.compile2(nci ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) { try { UnitCompiler.this.compile2(pa  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) { try { UnitCompiler.this.compile2(qtr ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitThisReference                 (Java.ThisReference                  tr  ) { try { UnitCompiler.this.compile2(tr  ); } catch (CompileException e) { throw new UCE(e); } }

            public void visitAmbiguousName                  (Java.AmbiguousName              an   ) { try { UnitCompiler.this.compile2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression      aae  ) { try { UnitCompiler.this.compile2(aae  ); } catch (CompileException e) { throw new UCE(e); } };
            public void visitFieldAccess                    (Java.FieldAccess                fa   ) { try { UnitCompiler.this.compile2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression      fae  ) { try { UnitCompiler.this.compile2(fae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compile2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess        lva  ) { try { UnitCompiler.this.compile2(lva  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression    pe   ) { try { UnitCompiler.this.compile2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private void compile2(Java.Rvalue rv) throws CompileException {
        this.pop((Locatable) rv, this.compileGetValue(rv));
    }
    private void compile2(Java.Assignment a) throws CompileException {
        if (a.operator == "=") {
            this.compileContext(a.lhs);
            this.assignmentConversion(
                (Locatable) a,               // l
                this.compileGetValue(a.rhs), // sourceType
                this.getType(a.lhs),         // targetType
                this.getConstantValue(a.rhs) // optionalConstantValue
            );
            this.compileSet(a.lhs);
            return;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCS = this.compileContext(a.lhs);
        this.dup((Locatable) a, lhsCS);
        IClass lhsType = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            (Locatable) a,        // l
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), // <= IMPORTANT!
            a.rhs                 // rhs
        );
        // Convert the result to LHS type (JLS2 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType) &&
            !this.tryNarrowingPrimitiveConversion(
                (Locatable) a,   // l
                resultType,      // sourceType
                lhsType          // destinationType
            )
        ) throw new RuntimeException("SNO: \"" + a.operator + "\" reconversion failed");
        this.compileSet(a.lhs);
    }
    private void compile2(Java.Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        Java.LocalVariable lv = this.isIntLV(c);
        if (lv != null) {
            this.writeOpcode(c, Opcode.IINC);
            this.writeByte(lv.localVariableArrayIndex);
            this.writeByte(c.operator == "++" ? 1 : -1);
            return;
        }

        int cs = this.compileContext(c.operand);
        this.dup((Locatable) c, cs);
        IClass type = this.compileGet(c.operand);
        IClass promotedType = this.unaryNumericPromotion((Locatable) c, type);
        this.writeOpcode(c, UnitCompiler.ilfd(
            promotedType,
            Opcode.ICONST_1,
            Opcode.LCONST_1,
            Opcode.FCONST_1,
            Opcode.DCONST_1
        ));
        if (c.operator == "++") {
            this.writeOpcode(c, Opcode.IADD + UnitCompiler.ilfd(promotedType));
        } else
        if (c.operator == "--") {
            this.writeOpcode(c, Opcode.ISUB + UnitCompiler.ilfd(promotedType));
        } else {
            this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
        }

        this.reverseUnaryNumericPromotion((Locatable) c, promotedType, type);
        this.compileSet(c.operand);
    }
    private void compile2(Java.ParenthesizedExpression pe) throws CompileException {
        this.compile(pe.value);
    }

    private boolean compile2(Java.AlternateConstructorInvocation aci) throws CompileException {
        Java.ConstructorDeclarator declaringConstructor = (Java.ConstructorDeclarator) aci.getEnclosingScope();
        IClass                     declaringIClass = this.resolve(declaringConstructor.getDeclaringClass());

        this.writeOpcode(aci, Opcode.ALOAD_0);
        if (declaringIClass.getOuterIClass() != null) this.writeOpcode(aci, Opcode.ALOAD_1);
        this.invokeConstructor(
            (Locatable) aci,                   // l
            (Java.Scope) declaringConstructor, // scope
            (Java.Rvalue) null,                // optionalEnclosingInstance
            declaringIClass,                   // targetClass
            aci.arguments                      // arguments
        );
        return true;
    }
    private boolean compile2(Java.SuperConstructorInvocation sci) throws CompileException {
        Java.ConstructorDeclarator declaringConstructor = (Java.ConstructorDeclarator) sci.getEnclosingScope();
        this.writeOpcode(sci, Opcode.ALOAD_0);
        Java.ClassDeclaration declaringClass = declaringConstructor.getDeclaringClass();
        IClass superclass = this.resolve(declaringClass).getSuperclass();

        Java.Rvalue optionalEnclosingInstance;
        if (sci.optionalQualification != null) {
            optionalEnclosingInstance = sci.optionalQualification;
        } else {
            IClass outerIClassOfSuperclass = superclass.getOuterIClass();
            if (outerIClassOfSuperclass == null) {
                optionalEnclosingInstance = null;
            } else {
//                optionalEnclosingInstance = new Java.QualifiedThisReference(
//                    sci.getLocation(),      // location
//                    declaringClass,         // declaringClass
//                    declaringConstructor,   // declaringTypeBodyDeclaration
//                    outerIClassOfSuperclass // targetClass
//                );
                optionalEnclosingInstance = new Java.QualifiedThisReference(
                    sci.getLocation(),   // location
                    new Java.SimpleType( // qualification
                        sci.getLocation(),
                        outerIClassOfSuperclass
                    )
                );
                optionalEnclosingInstance.setEnclosingBlockStatement(sci);
            }
        }
        this.invokeConstructor(
            (Locatable) sci,                   // l
            (Java.Scope) declaringConstructor, // scope
            optionalEnclosingInstance,         // optionalEnclosingInstance
            superclass,                        // targetClass
            sci.arguments                      // arguments
        );
        return true;
    }

    /**
     * Some {@link Java.Rvalue}s compile more efficiently when their value is the
     * condition for a branch.<br>
     *
     * Notice that if "this" is a constant, then either "dst" is never
     * branched to, or it is unconditionally branched to. "Unexamined code"
     * errors may result during bytecode validation.
     */
    private void compileBoolean(
        Java.Rvalue              rv,
        final CodeContext.Offset dst,        // Where to jump.
        final boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength                   (Java.ArrayLength                    al  ) { try { UnitCompiler.this.compileBoolean2(al  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitAssignment                    (Java.Assignment                     a   ) { try { UnitCompiler.this.compileBoolean2(a   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) { try { UnitCompiler.this.compileBoolean2(uo  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) { try { UnitCompiler.this.compileBoolean2(bo  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCast                          (Java.Cast                           c   ) { try { UnitCompiler.this.compileBoolean2(c   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) { try { UnitCompiler.this.compileBoolean2(cl  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) { try { UnitCompiler.this.compileBoolean2(ce  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCrement                       (Java.Crement                        c   ) { try { UnitCompiler.this.compileBoolean2(c   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitInstanceof                    (Java.Instanceof                     io  ) { try { UnitCompiler.this.compileBoolean2(io  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) { try { UnitCompiler.this.compileBoolean2(mi  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) { try { UnitCompiler.this.compileBoolean2(smi , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLiteral                       (Java.Literal                        l   ) { try { UnitCompiler.this.compileBoolean2(l   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) { try { UnitCompiler.this.compileBoolean2(naci, dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewArray                      (Java.NewArray                       na  ) { try { UnitCompiler.this.compileBoolean2(na  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewInitializedArray           (Java.NewInitializedArray            nia ) { try { UnitCompiler.this.compileBoolean2(nia , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) { try { UnitCompiler.this.compileBoolean2(nci , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) { try { UnitCompiler.this.compileBoolean2(pa  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) { try { UnitCompiler.this.compileBoolean2(qtr , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitThisReference                 (Java.ThisReference                  tr  ) { try { UnitCompiler.this.compileBoolean2(tr  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }

            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { UnitCompiler.this.compileBoolean2(an   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) { try { UnitCompiler.this.compileBoolean2(aae  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } };
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { UnitCompiler.this.compileBoolean2(fa   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) { try { UnitCompiler.this.compileBoolean2(fae  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compileBoolean2(scfae, dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) { try { UnitCompiler.this.compileBoolean2(lva  , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { UnitCompiler.this.compileBoolean2(pe   , dst, orientation); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private void compileBoolean2(
        Java.Rvalue        rv,
        CodeContext.Offset dst,        // Where to jump.
        boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        IClass type = this.compileGetValue(rv);
        IClassLoader icl = this.iClassLoader;
        if (type == icl.BOOLEAN) {
            this.unboxingConversion((Locatable) rv, icl.BOOLEAN, IClass.BOOLEAN);
        } else
        if (type != IClass.BOOLEAN) {
            this.compileError("Not a boolean expression", rv.getLocation());
        }
        this.writeBranch(rv, orientation == Java.Rvalue.JUMP_IF_TRUE ? Opcode.IFNE : Opcode.IFEQ, dst);
    }
    private void compileBoolean2(
        Java.UnaryOperation ue,
        CodeContext.Offset dst,        // Where to jump.
        boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        if (ue.operator == "!") {
            this.compileBoolean(ue.operand, dst, !orientation);
            return;
        }

        this.compileError("Boolean expression expected", ue.getLocation());
    }
    private void compileBoolean2(
        Java.BinaryOperation bo,
        CodeContext.Offset   dst,        // Where to jump.
        boolean              orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {

        if (bo.op == "|" || bo.op == "^" || bo.op == "&") {
            this.compileBoolean2((Java.Rvalue) bo, dst, orientation);
            return;
        }

        if (bo.op == "||" || bo.op == "&&") {
            Object lhsCV = this.getConstantValue(bo.lhs);
            if (lhsCV instanceof Boolean) {
                if (((Boolean) lhsCV).booleanValue() ^ bo.op == "||") {
                    // "true && a", "false || a"
                    this.compileBoolean(bo.rhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                } else {
                    // "false && a", "true || a"
                    this.compileBoolean(bo.lhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                    this.fakeCompile(bo.rhs);
                }
                return;
            }
            Object rhsCV = this.getConstantValue(bo.rhs);
            if (rhsCV instanceof Boolean) {
                if (((Boolean) rhsCV).booleanValue() ^ bo.op == "||") {
                    // "a && true", "a || false"
                    this.compileBoolean(bo.lhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                } else {
                    // "a && false", "a || true"
                    this.pop((Locatable) bo.lhs, this.compileGetValue(bo.lhs));
                    this.compileBoolean(bo.rhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                }
                return;
            }
            if (bo.op == "||" ^ orientation == Java.Rvalue.JUMP_IF_FALSE) {
                this.compileBoolean(bo.lhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                this.compileBoolean(bo.rhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
            } else {
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(bo.lhs, end, Java.Rvalue.JUMP_IF_FALSE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                this.compileBoolean(bo.rhs, dst, Java.Rvalue.JUMP_IF_TRUE ^ orientation == Java.Rvalue.JUMP_IF_FALSE);
                end.set();
            }
            return;
        }

        if (
            bo.op == "==" ||
            bo.op == "!=" ||
            bo.op == "<=" ||
            bo.op == ">=" ||
            bo.op == "<"  ||
            bo.op == ">"
        ) {
            int opIdx = (
                bo.op == "==" ? 0 :
                bo.op == "!=" ? 1 :
                bo.op == "<"  ? 2 :
                bo.op == ">=" ? 3 :
                bo.op == ">"  ? 4 :
                bo.op == "<=" ? 5 : Integer.MIN_VALUE
            );
            if (orientation == Java.Rvalue.JUMP_IF_FALSE) opIdx ^= 1;

            // Comparison with "null".
            {
                boolean lhsIsNull = this.getConstantValue(bo.lhs) == Java.Rvalue.CONSTANT_VALUE_NULL;
                boolean rhsIsNull = this.getConstantValue(bo.rhs) == Java.Rvalue.CONSTANT_VALUE_NULL;

                if (lhsIsNull || rhsIsNull) {
                    if (bo.op != "==" && bo.op != "!=") this.compileError("Operator \"" + bo.op + "\" not allowed on operand \"null\"", bo.getLocation());

                    // null == x
                    // x == null
                    IClass ohsType = this.compileGetValue(lhsIsNull ? bo.rhs : bo.lhs);
                    if (ohsType.isPrimitive()) this.compileError("Cannot compare \"null\" with primitive type \"" + ohsType.toString() + "\"", bo.getLocation());
                    this.writeBranch(bo, Opcode.IFNULL + opIdx, dst);
                    return;
                }
            }

            IClass lhsType = this.compileGetValue(bo.lhs);
            CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
            IClass rhsType = this.compileGetValue(bo.rhs);

            // 15.20.1 Numerical comparison.
            if (
                this.getUnboxedType(lhsType).isPrimitiveNumeric() &&
                this.getUnboxedType(rhsType).isPrimitiveNumeric() &&
                !((bo.op == "==" || bo.op == "!=") && !lhsType.isPrimitive() && !rhsType.isPrimitive())
            ) {
                IClass promotedType = this.binaryNumericPromotion((Locatable) bo, lhsType, convertLhsInserter, rhsType);
                if (promotedType == IClass.INT) {
                    this.writeBranch(bo, Opcode.IF_ICMPEQ + opIdx, dst);
                } else
                if (promotedType == IClass.LONG) {
                    this.writeOpcode(bo, Opcode.LCMP);
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                if (promotedType == IClass.FLOAT) {
                    this.writeOpcode(bo, Opcode.FCMPG);
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                if (promotedType == IClass.DOUBLE) {
                    this.writeOpcode(bo, Opcode.DCMPG);
                    this.writeBranch(bo, Opcode.IFEQ + opIdx, dst);
                } else
                {
                    throw new RuntimeException("Unexpected promoted type \"" + promotedType + "\"");
                }
                return;
            }

            // JLS3 15.21.2 Boolean Equality Operators == and !=
            if (
                (lhsType == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN) ||
                (rhsType == IClass.BOOLEAN && this.getUnboxedType(lhsType) == IClass.BOOLEAN)
            ) {
                if (bo.op != "==" && bo.op != "!=") this.compileError("Operator \"" + bo.op + "\" not allowed on boolean operands", bo.getLocation());
                IClassLoader icl = this.iClassLoader;

                // Unbox LHS if necessary.
                if (lhsType == icl.BOOLEAN) {
                    this.codeContext.pushInserter(convertLhsInserter);
                    try {
                        this.unboxingConversion((Locatable) bo, icl.BOOLEAN, IClass.BOOLEAN);
                    } finally {
                        this.codeContext.popInserter();
                    }
                }

                // Unbox RHS if necessary.
                if (rhsType == icl.BOOLEAN) {
                    this.unboxingConversion((Locatable) bo, icl.BOOLEAN, IClass.BOOLEAN);
                }

                this.writeBranch(bo, Opcode.IF_ICMPEQ + opIdx, dst);
                return;
            }

            // Reference comparison.
            // Note: Comparison with "null" is already handled above.
            if (
                !lhsType.isPrimitive() &&
                !rhsType.isPrimitive()
            ) {
                if (bo.op != "==" && bo.op != "!=") this.compileError("Operator \"" + bo.op + "\" not allowed on reference operands", bo.getLocation());
                this.writeBranch(bo, Opcode.IF_ACMPEQ + opIdx, dst);
                return;
            }

            this.compileError("Cannot compare types \"" + lhsType + "\" and \"" + rhsType + "\"", bo.getLocation());
        }

        this.compileError("Boolean expression expected", bo.getLocation());
    }
    private void compileBoolean2(
        Java.ParenthesizedExpression pe,
        CodeContext.Offset dst,
        boolean orientation
    ) throws CompileException {
        this.compileBoolean(pe.value, dst, orientation);
    }

    /**
     * Generates code that determines the context of the {@link
     * Java.Rvalue} and puts it on the operand stack. Most expressions
     * do not have a "context", but some do. E.g. for "x[y]", the context
     * is "x, y". The bottom line is that for statements like "x[y] += 3"
     * the context is only evaluated once.
     *
     * @return The size of the context on the operand stack
     */
    private int compileContext(Java.Rvalue rv) throws CompileException {
        final int[] res = new int[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength                   (Java.ArrayLength                    al  ) { try { res[0] = UnitCompiler.this.compileContext2(al  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitAssignment                    (Java.Assignment                     a   ) {       res[0] = UnitCompiler.this.compileContext2(a   );                                                                }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) {       res[0] = UnitCompiler.this.compileContext2(uo  );                                                                }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) {       res[0] = UnitCompiler.this.compileContext2(bo  );                                                                }
            public void visitCast                          (Java.Cast                           c   ) {       res[0] = UnitCompiler.this.compileContext2(c   );                                                                }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) {       res[0] = UnitCompiler.this.compileContext2(cl  );                                                                }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) {       res[0] = UnitCompiler.this.compileContext2(ce  );                                                                }
            public void visitCrement                       (Java.Crement                        c   ) {       res[0] = UnitCompiler.this.compileContext2(c   );                                                                }
            public void visitInstanceof                    (Java.Instanceof                     io  ) {       res[0] = UnitCompiler.this.compileContext2(io  );                                                                }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) {       res[0] = UnitCompiler.this.compileContext2(mi  );                                                                }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) {       res[0] = UnitCompiler.this.compileContext2(smi );                                                                }
            public void visitLiteral                       (Java.Literal                        l   ) {       res[0] = UnitCompiler.this.compileContext2(l   );                                                                }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) {       res[0] = UnitCompiler.this.compileContext2(naci);                                                                }
            public void visitNewArray                      (Java.NewArray                       na  ) {       res[0] = UnitCompiler.this.compileContext2(na  );                                                                }
            public void visitNewInitializedArray           (Java.NewInitializedArray            nia ) {       res[0] = UnitCompiler.this.compileContext2(nia );                                                                }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) {       res[0] = UnitCompiler.this.compileContext2(nci );                                                                }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) {       res[0] = UnitCompiler.this.compileContext2(pa  );                                                                }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) {       res[0] = UnitCompiler.this.compileContext2(qtr );                                                                }
            public void visitThisReference                 (Java.ThisReference                  tr  ) {       res[0] = UnitCompiler.this.compileContext2(tr  );                                                                }

            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { res[0] = UnitCompiler.this.compileContext2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) { try { res[0] = UnitCompiler.this.compileContext2(aae  ); } catch (CompileException e) { throw new UCE(e); } };
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { res[0] = UnitCompiler.this.compileContext2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) { try { res[0] = UnitCompiler.this.compileContext2(fae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.compileContext2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.compileContext2(lva  );                                                                }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { res[0] = UnitCompiler.this.compileContext2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private int compileContext2(Java.Rvalue rv) {
        return 0;
    }
    private int compileContext2(Java.AmbiguousName an) throws CompileException {
        return this.compileContext(this.toRvalueOrCE(this.reclassify(an)));
    }
    private int compileContext2(Java.FieldAccess fa) throws CompileException {
        if (fa.field.isStatic()) {
            this.getType(this.toTypeOrCE(fa.lhs));
            return 0;
        } else {
            this.compileGetValue(this.toRvalueOrCE(fa.lhs));
            return 1;
        }
    }
    private int compileContext2(Java.ArrayLength al) throws CompileException {
        if (!this.compileGetValue(al.lhs).isArray()) this.compileError("Cannot determine length of non-array type", al.getLocation());
        return 1;
    }
    private int compileContext2(Java.ArrayAccessExpression aae) throws CompileException {
        IClass lhsType = this.compileGetValue(aae.lhs);
        if (!lhsType.isArray()) this.compileError("Subscript not allowed on non-array type \"" + lhsType.toString() + "\"", aae.getLocation());

        IClass indexType = this.compileGetValue(aae.index);
        if (
            !this.tryIdentityConversion(indexType, IClass.INT) &&
            !this.tryWideningPrimitiveConversion(
                (Locatable) aae, // l
                indexType,       // sourceType
                IClass.INT       // targetType
            )
        ) this.compileError("Index expression of type \"" + indexType + "\" cannot be widened to \"int\"", aae.getLocation());

        return 2;
    }
    private int compileContext2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileContext(fae.value);
    }
    private int compileContext2(Java.SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.compileContext(scfae.value);
    }
    private int compileContext2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.compileContext(pe.value);
    }

    /**
     * Generates code that determines the value of the {@link Java.Rvalue}
     * and puts it on the operand stack. This method relies on that the
     * "context" of the {@link Java.Rvalue} is on top of the operand stack
     * (see {@link #compileContext(Java.Rvalue)}).
     *
     * @return The type of the {@link Java.Rvalue}
     */
    private IClass compileGet(Java.Rvalue rv) throws CompileException {
        final IClass[] res = new IClass[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength                   (Java.ArrayLength                    al  ) {       res[0] = UnitCompiler.this.compileGet2(al  );                                                                }
            public void visitAssignment                    (Java.Assignment                     a   ) { try { res[0] = UnitCompiler.this.compileGet2(a   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) { try { res[0] = UnitCompiler.this.compileGet2(uo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) { try { res[0] = UnitCompiler.this.compileGet2(bo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCast                          (Java.Cast                           c   ) { try { res[0] = UnitCompiler.this.compileGet2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) { try { res[0] = UnitCompiler.this.compileGet2(cl  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) { try { res[0] = UnitCompiler.this.compileGet2(ce  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCrement                       (Java.Crement                        c   ) { try { res[0] = UnitCompiler.this.compileGet2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitInstanceof                    (Java.Instanceof                     io  ) { try { res[0] = UnitCompiler.this.compileGet2(io  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) { try { res[0] = UnitCompiler.this.compileGet2(mi  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) { try { res[0] = UnitCompiler.this.compileGet2(smi ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLiteral                       (Java.Literal                        l   ) { try { res[0] = UnitCompiler.this.compileGet2(l   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) { try { res[0] = UnitCompiler.this.compileGet2(naci); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewArray                      (Java.NewArray                       na  ) { try { res[0] = UnitCompiler.this.compileGet2(na  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewInitializedArray           (Java.NewInitializedArray            nia ) { try { res[0] = UnitCompiler.this.compileGet2(nia ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) { try { res[0] = UnitCompiler.this.compileGet2(nci ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) { try { res[0] = UnitCompiler.this.compileGet2(pa  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) { try { res[0] = UnitCompiler.this.compileGet2(qtr ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitThisReference                 (Java.ThisReference                  tr  ) { try { res[0] = UnitCompiler.this.compileGet2(tr  ); } catch (CompileException e) { throw new UCE(e); } }

            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { res[0] = UnitCompiler.this.compileGet2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) { try { res[0] = UnitCompiler.this.compileGet2(aae  ); } catch (CompileException e) { throw new UCE(e); } };
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { res[0] = UnitCompiler.this.compileGet2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) { try { res[0] = UnitCompiler.this.compileGet2(fae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.compileGet2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.compileGet2(lva  );                                                                }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { res[0] = UnitCompiler.this.compileGet2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private IClass compileGet2(Java.BooleanRvalue brv) throws CompileException {
        CodeContext.Offset isTrue = this.codeContext.new Offset();
        this.compileBoolean(brv, isTrue, Java.Rvalue.JUMP_IF_TRUE);
        this.writeOpcode(brv, Opcode.ICONST_0);
        CodeContext.Offset end = this.codeContext.new Offset();
        this.writeBranch(brv, Opcode.GOTO, end);
        isTrue.set();
        this.writeOpcode(brv, Opcode.ICONST_1);
        end.set();

        return IClass.BOOLEAN;
    }
    private IClass compileGet2(Java.AmbiguousName an) throws CompileException {
        return this.compileGet(this.toRvalueOrCE(this.reclassify(an)));
    }
    private IClass compileGet2(Java.LocalVariableAccess lva) {
        return this.load((Locatable) lva, lva.localVariable);
    }
    private IClass compileGet2(Java.FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.getEnclosingBlockStatement());
        if (fa.field.isStatic()) {
            this.writeOpcode(fa, Opcode.GETSTATIC);
        } else {
            this.writeOpcode(fa, Opcode.GETFIELD);
        }
        this.writeConstantFieldrefInfo(
            fa.field.getDeclaringIClass().getDescriptor(),
            fa.field.getName(), // classFD
            fa.field.getType().getDescriptor()             // fieldFD
        );
        return fa.field.getType();
    }
    private IClass compileGet2(Java.ArrayLength al) {
        this.writeOpcode(al, Opcode.ARRAYLENGTH);
        return IClass.INT;
    }
    private IClass compileGet2(Java.ThisReference tr) throws CompileException {
        this.referenceThis((Locatable) tr);
        return this.getIClass(tr);
    }
    private IClass compileGet2(Java.QualifiedThisReference qtr) throws CompileException {
        this.referenceThis(
            (Locatable) qtr,                           // l
            this.getDeclaringClass(qtr),               // declaringClass
            this.getDeclaringTypeBodyDeclaration(qtr), // declaringTypeBodyDeclaration
            this.getTargetIClass(qtr)                  // targetIClass
        );
        return this.getTargetIClass(qtr);
    }
    private IClass compileGet2(Java.ClassLiteral cl) throws CompileException {
        Location loc = cl.getLocation();
        final IClassLoader icl = this.iClassLoader;
        IClass iClass = this.getType(cl.type);

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
            if (wrapperClassDescriptor == null) throw new RuntimeException("SNO: Unidentifiable primitive type \"" + iClass + "\"");

            this.writeConstantFieldrefInfo(
                wrapperClassDescriptor,
                "TYPE", // classFD
                "Ljava/lang/Class;"     // fieldFD
            );
            return icl.CLASS;
        }

        // Non-primitive class literal.

        Java.AbstractTypeDeclaration declaringType;
        for (Java.Scope s = cl.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof Java.TypeDeclaration) {
                declaringType = (Java.AbstractTypeDeclaration) s;
                break;
            }
        }

        // Check if synthetic method "static Class class$(String className)" is already
        // declared.
        if (declaringType.getMethodDeclaration("class$") == null) this.declareClassDollarMethod(cl);

        // Determine the statics of the declaring class (this is where static fields
        // declarations are found).
        List statics; // TypeBodyDeclaration
        if (declaringType instanceof Java.ClassDeclaration) {
            statics = ((Java.ClassDeclaration) declaringType).variableDeclaratorsAndInitializers;
        } else
        if (declaringType instanceof Java.InterfaceDeclaration) {
            statics = ((Java.InterfaceDeclaration) declaringType).constantDeclarations;
        } else {
            throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
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
                if (classDollarFieldName.endsWith(";")) classDollarFieldName = classDollarFieldName.substring(0, classDollarFieldName.length() - 1);
            } else
            {
                classDollarFieldName = "class$" + className.replace('.', '$');
            }
        }

        // Declare the static "class dollar field" if not already done.
        {
            boolean hasClassDollarField = false;
            BLOCK_STATEMENTS: for (Iterator it = statics.iterator(); it.hasNext();) {
                Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
                if (!tbd.isStatic()) continue;
                if (tbd instanceof Java.FieldDeclaration) {
                    Java.FieldDeclaration fd = (Java.FieldDeclaration) tbd;
                    IClass.IField[] fds = this.getIFields(fd);
                    for (int j = 0; j < fds.length; ++j) {
                        if (fds[j].getName().equals(classDollarFieldName)) {
                            hasClassDollarField = true;
                            break BLOCK_STATEMENTS;
                        }
                    }
                }
            }
            if (!hasClassDollarField) {
                Java.Type classType = new Java.SimpleType(loc, icl.CLASS);
                Java.FieldDeclaration fd = new Java.FieldDeclaration(
                    loc,                            // location
                    null,                           // optionalDocComment
                    Mod.STATIC,                     // modifiers
                    classType,                      // type
                    new Java.VariableDeclarator[] { // variableDeclarators
                        new Java.VariableDeclarator(
                            loc,                  // location
                            classDollarFieldName, // name
                            0,                    // brackets
                            (Java.Rvalue) null    // optionalInitializer
                        )
                    }
                );
                if (declaringType instanceof Java.ClassDeclaration) {
                    ((Java.ClassDeclaration) declaringType).addVariableDeclaratorOrInitializer(fd);
                } else
                if (declaringType instanceof Java.InterfaceDeclaration) {
                    ((Java.InterfaceDeclaration) declaringType).addConstantDeclaration(fd);
                } else {
                    throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
                }
            }
        }

        // return (class$X != null) ? class$X : (class$X = class$("X"));
        Java.Type declaringClassOrInterfaceType = new Java.SimpleType(loc, this.resolve(declaringType));
        Java.Lvalue classDollarFieldAccess = new Java.FieldAccessExpression(
            loc,                           // location
            declaringClassOrInterfaceType, // lhs
            classDollarFieldName           // fieldName
        );
        Java.ConditionalExpression ce = new Java.ConditionalExpression(
            loc,                            // location
            new Java.BinaryOperation(       // lhs
                loc,                        // location
                classDollarFieldAccess,     // lhs
                "!=",                       // op
                new Java.Literal(loc, null) // rhs
            ),
            classDollarFieldAccess,        // mhs
            new Java.Assignment(           // rhs
                loc,                       // location
                classDollarFieldAccess,    // lhs
                "=",                       // operator
                new Java.MethodInvocation( // rhs
                    loc,                           // location
                    declaringClassOrInterfaceType, // optionalTarget
                    "class$",                      // methodName
                    new Java.Rvalue[] {            // arguments
                        new Java.Literal(
                            loc,      // location
                            className // constantValue
                        )
                    }
                )
            )
        );
        ce.setEnclosingBlockStatement(cl.getEnclosingBlockStatement());
        return this.compileGet(ce);
    }
    private IClass compileGet2(Java.Assignment a) throws CompileException {
        if (a.operator == "=") {
            int lhsCS = this.compileContext(a.lhs);
            IClass rhsType = this.compileGetValue(a.rhs);
            IClass lhsType = this.getType(a.lhs);
            Object rhsCV = this.getConstantValue(a.rhs);
            this.assignmentConversion(
                (Locatable) a, // l
                rhsType,       // sourceType
                lhsType,       // targetType
                rhsCV          // optionalConstantValue
            );
            this.dupx(
                (Locatable) a, // l
                lhsType,       // type
                lhsCS          // x
            );
            this.compileSet(a.lhs);
            return lhsType;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCS = this.compileContext(a.lhs);
        this.dup((Locatable) a, lhsCS);
        IClass lhsType = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            (Locatable) a,        // l
            lhsType,              // lhsType
            a.operator.substring( // operator
                0,
                a.operator.length() - 1
            ).intern(), // <= IMPORTANT!
            a.rhs                 // rhs
        );
        // Convert the result to LHS type (JLS2 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType) &&
            !this.tryNarrowingPrimitiveConversion(
                (Locatable) a, // l
                resultType,    // sourceType
                lhsType        // destinationType
            )
        ) throw new RuntimeException("SNO: \"" + a.operator + "\" reconversion failed");
        this.dupx(
            (Locatable) a, // l
            lhsType,       // type
            lhsCS          // x
        );
        this.compileSet(a.lhs);
        return lhsType;
    }
    private IClass compileGet2(Java.ConditionalExpression ce) throws CompileException {
        IClass mhsType, rhsType;
        CodeContext.Inserter mhsConvertInserter, rhsConvertInserter;
        CodeContext.Offset toEnd = this.codeContext.new Offset();
        Object cv = this.getConstantValue(ce.lhs);
        if (cv instanceof Boolean) {
            if (((Boolean) cv).booleanValue()) {
                mhsType = this.compileGetValue(ce.mhs);
                mhsConvertInserter = this.codeContext.newInserter();
                rhsType = this.getType(ce.rhs);
                rhsConvertInserter = null;
            } else {
                mhsType = this.getType(ce.mhs);
                mhsConvertInserter = null;
                rhsType = this.compileGetValue(ce.rhs);
                rhsConvertInserter = this.codeContext.currentInserter();
            }
        } else {
            CodeContext.Offset toRhs = this.codeContext.new Offset();

            this.compileBoolean(ce.lhs, toRhs, Java.Rvalue.JUMP_IF_FALSE);
            mhsType = this.compileGetValue(ce.mhs);
            mhsConvertInserter = this.codeContext.newInserter();
            this.writeBranch(ce, Opcode.GOTO, toEnd);
            toRhs.set();
            rhsType = this.compileGetValue(ce.rhs);
            rhsConvertInserter = this.codeContext.currentInserter();
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
                (Locatable) ce,     // l
                mhsType,            // type1
                mhsConvertInserter, // convertInserter1
                rhsType,            // type2
                rhsConvertInserter  // convertInserter2
            );
        } else
        if (this.getConstantValue(ce.mhs) == Java.Rvalue.CONSTANT_VALUE_NULL && !rhsType.isPrimitive()) {

            // JLS 15.25.1.3 (null : ref)
            expressionType = rhsType;
        } else
        if (!mhsType.isPrimitive() && this.getConstantValue(ce.rhs) == Java.Rvalue.CONSTANT_VALUE_NULL) {

            // JLS 15.25.1.3 (ref : null)
            expressionType = mhsType;
        } else
        if (!mhsType.isPrimitive() && !rhsType.isPrimitive()) {
            if (mhsType.isAssignableFrom(rhsType)) {
                expressionType = mhsType;
            } else
            if (rhsType.isAssignableFrom(mhsType)) {
                expressionType = rhsType;
            } else {
                this.compileError("Reference types \"" + mhsType + "\" and \"" + rhsType + "\" don't match", ce.getLocation());
                return this.iClassLoader.OBJECT;
            }
        } else
        {
            this.compileError("Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"", ce.getLocation());
            return this.iClassLoader.OBJECT;
        }
        toEnd.set();

        return expressionType;
    }
    private IClass compileGet2(Java.Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        Java.LocalVariable lv = this.isIntLV(c);
        if (lv != null) {
            if (!c.pre) this.load((Locatable) c, lv);
            this.writeOpcode(c, Opcode.IINC);
            this.writeByte(lv.localVariableArrayIndex);
            this.writeByte(c.operator == "++" ? 1 : -1);
            if (c.pre) this.load((Locatable) c, lv);
            return lv.type;
        }

        // Compile operand context.
        int cs = this.compileContext(c.operand);
        // DUP operand context.
        this.dup((Locatable) c, cs);
        // Get operand value.
        IClass type = this.compileGet(c.operand);
        // DUPX operand value.
        if (!c.pre) this.dupx((Locatable) c, type, cs);
        // Apply "unary numeric promotion".
        IClass promotedType = this.unaryNumericPromotion((Locatable) c, type);
        // Crement.
        this.writeOpcode(c, UnitCompiler.ilfd(
            promotedType,
            Opcode.ICONST_1,
            Opcode.LCONST_1,
            Opcode.FCONST_1,
            Opcode.DCONST_1
        ));
        if (c.operator == "++") {
            this.writeOpcode(c, Opcode.IADD + UnitCompiler.ilfd(promotedType));
        } else
        if (c.operator == "--") {
            this.writeOpcode(c, Opcode.ISUB + UnitCompiler.ilfd(promotedType));
        } else {
            this.compileError("Unexpected operator \"" + c.operator + "\"", c.getLocation());
        }
        this.reverseUnaryNumericPromotion((Locatable) c, promotedType, type);
        // DUPX cremented operand value.
        if (c.pre) this.dupx((Locatable) c, type, cs);
        // Set operand.
        this.compileSet(c.operand);

        return type;
    }
    private IClass compileGet2(Java.ArrayAccessExpression aae) throws CompileException {
        IClass lhsComponentType = this.getType(aae);
        this.writeOpcode(aae, Opcode.IALOAD + UnitCompiler.ilfdabcs(lhsComponentType));
        return lhsComponentType;
    }
    private IClass compileGet2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileGet(fae.value);
    }
    private IClass compileGet2(Java.SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.compileGet(scfae.value);
    }
    private IClass compileGet2(Java.UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") {
            return this.compileGet2((Java.BooleanRvalue) uo);
        }

        if (uo.operator == "+") {
            return this.unaryNumericPromotion(
                (Locatable) uo,
                this.convertToPrimitiveNumericType((Locatable) uo, this.compileGetValue(uo.operand))
            );
        }

        if (uo.operator == "-") {

            // Special handling for negated literals.
            if (uo.operand instanceof Java.Literal) {
                Java.Literal l = (Java.Literal) uo.operand;
                this.pushConstant((Locatable) uo, this.getNegatedConstantValue2(l));
                return this.unaryNumericPromotion((Locatable) uo, this.getType2(l));
            }

            IClass promotedType = this.unaryNumericPromotion(
                (Locatable) uo,
                this.convertToPrimitiveNumericType((Locatable) uo, this.compileGetValue(uo.operand))
            );
            this.writeOpcode(uo, Opcode.INEG + UnitCompiler.ilfd(promotedType));
            return promotedType;
        }

        if (uo.operator == "~") {
            IClass operandType = this.compileGetValue(uo.operand);

            IClass promotedType = this.unaryNumericPromotion((Locatable) uo, operandType);
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
        return this.iClassLoader.OBJECT;
    }
    private IClass compileGet2(Java.Instanceof io) throws CompileException {
        IClass lhsType = this.compileGetValue(io.lhs);
        IClass rhsType = this.getType(io.rhs);

        if (rhsType.isAssignableFrom(lhsType)) {
            this.pop((Locatable) io, lhsType);
            this.writeOpcode(io, Opcode.ICONST_1);
        } else
        if (
            lhsType.isInterface() ||
            rhsType.isInterface() ||
            lhsType.isAssignableFrom(rhsType)
        ) {
            this.writeOpcode(io, Opcode.INSTANCEOF);
            this.writeConstantClassInfo(rhsType.getDescriptor());
        } else {
            this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"", io.getLocation());
        }
        return IClass.BOOLEAN;
    }
    private IClass compileGet2(Java.BinaryOperation bo) throws CompileException {
        if (
            bo.op == "||" ||
            bo.op == "&&" ||
            bo.op == "==" ||
            bo.op == "!=" ||
            bo.op == "<"  ||
            bo.op == ">"  ||
            bo.op == "<=" ||
            bo.op == ">="
        ) {
            // Eventually calls "compileBoolean()".
            return this.compileGet2((Java.BooleanRvalue) bo);
        }

        // Implements "| ^ & * / % + - << >> >>>".
        return this.compileArithmeticOperation(
            (Locatable) bo,             // l
            null,                       // type
            bo.unrollLeftAssociation(), // operands
            bo.op                       // operator
        );
    }
    private IClass compileGet2(Java.Cast c) throws CompileException {

        // JLS3 5.5 Casting Conversion
        IClass tt = this.getType(c.targetType);
        IClass vt = this.compileGetValue(c.value);
        if (
            !this.tryIdentityConversion(vt, tt) &&
            !this.tryWideningPrimitiveConversion((Locatable) c, vt, tt) &&
            !this.tryNarrowingPrimitiveConversion((Locatable) c, vt, tt) &&
            !this.tryWideningReferenceConversion(vt, tt) &&
            !this.tryNarrowingReferenceConversion((Locatable) c, vt, tt) &&
            !this.tryBoxingConversion((Locatable) c, vt, tt) &&
            !this.tryUnboxingConversion((Locatable) c, vt, tt)
        ) this.compileError("Cannot cast \"" + vt + "\" to \"" + tt + "\"", c.getLocation());
        return tt;
    }
    private IClass compileGet2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.compileGet(pe.value);
    }
    private IClass compileGet2(Java.MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(mi);

        if (mi.optionalTarget == null) {

            // JLS2 6.5.7.1, 15.12.4.1.1.1
            Java.TypeBodyDeclaration scopeTBD;
            Java.ClassDeclaration    scopeClassDeclaration;
            {
                Java.Scope s;
                for (s = mi.getEnclosingBlockStatement(); !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
                scopeTBD = (Java.TypeBodyDeclaration) s;
                if (!(s instanceof Java.ClassDeclaration)) s = s.getEnclosingScope();
                scopeClassDeclaration = (Java.ClassDeclaration) s;
            }
            if (iMethod.isStatic()) {
                this.warning("IASM", "Implicit access to static method \"" + iMethod.toString() + "\"", mi.getLocation());
                // JLS2 15.12.4.1.1.1.1
                ;
            } else {
                this.warning("IANSM", "Implicit access to non-static method \"" + iMethod.toString() + "\"", mi.getLocation());
                // JLS2 15.12.4.1.1.1.2
                if (scopeTBD.isStatic()) this.compileError("Instance method \"" + iMethod.toString() + "\" cannot be invoked in static context", mi.getLocation());
                this.referenceThis(
                    (Locatable) mi,              // l
                    scopeClassDeclaration,       // declaringClass
                    scopeTBD,                    // declaringTypeBodyDeclaration
                    iMethod.getDeclaringIClass() // targetIClass
                );
            }
        } else {

            // 6.5.7.2
            boolean staticContext = this.isType(mi.optionalTarget);
            if (staticContext) {
                this.getType(this.toTypeOrCE(mi.optionalTarget));
            } else
            {
                this.compileGetValue(this.toRvalueOrCE(mi.optionalTarget));
            }
            if (iMethod.isStatic()) {
                if (!staticContext) {
                    // JLS2 15.12.4.1.2.1
                    this.pop((Locatable) mi.optionalTarget, this.getType(mi.optionalTarget));
                }
            } else {
                if (staticContext) this.compileError("Instance method \"" + mi.methodName + "\" cannot be invoked in static context", mi.getLocation());
            }
        }

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < mi.arguments.length; ++i) {
            this.assignmentConversion(
                (Locatable) mi,                        // l
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
                iMethod.getDeclaringIClass().getDescriptor(),                                           // locatable
                iMethod.getName(), // classFD
                iMethod.getDescriptor()                       // methodMD
            );
            IClass[] pts = iMethod.getParameterTypes();
            int count = 1;
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
                    iMethod.getDeclaringIClass().getDescriptor(),                                           // locatable
                    iMethod.getName() + '$', // classFD
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
    private IClass compileGet2(Java.SuperclassMethodInvocation scmi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(scmi);

        Java.Scope s;
        for (s = scmi.getEnclosingBlockStatement(); s instanceof Java.Statement || s instanceof Java.CatchClause; s = s.getEnclosingScope());
        Java.FunctionDeclarator fd = s instanceof Java.FunctionDeclarator ? (Java.FunctionDeclarator) s : null;
        if (fd == null) {
            this.compileError("Cannot invoke superclass method in non-method scope", scmi.getLocation());
            return IClass.INT;
        }
        if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Cannot invoke superclass method in static context", scmi.getLocation());
        this.load((Locatable) scmi, this.resolve(fd.getDeclaringType()), 0);

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < scmi.arguments.length; ++i) {
            this.assignmentConversion(
                (Locatable) scmi,                        // l
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
    private IClass compileGet2(Java.NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);

        this.writeOpcode(nci, Opcode.NEW);
        this.writeConstantClassInfo(nci.iClass.getDescriptor());
        this.writeOpcode(nci, Opcode.DUP);

        if (nci.iClass.isInterface()) this.compileError("Cannot instantiate \"" + nci.iClass + "\"", nci.getLocation());
        this.checkAccessible(nci.iClass, nci.getEnclosingBlockStatement());
        if (nci.iClass.isAbstract()) this.compileError("Cannot instantiate abstract \"" + nci.iClass + "\"", nci.getLocation());

        // Determine the enclosing instance for the new object.
        Java.Rvalue optionalEnclosingInstance;
        if (nci.optionalQualification != null) {
            if (nci.iClass.getOuterIClass() == null) this.compileError("Static member class cannot be instantiated with qualified NEW");

            // Enclosing instance defined by qualification (JLS 15.9.2.BL1.B3.B2).
            optionalEnclosingInstance = nci.optionalQualification;
        } else {
            Java.Scope s = nci.getEnclosingBlockStatement();
            for (; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
            Java.TypeBodyDeclaration enclosingTypeBodyDeclaration = (Java.TypeBodyDeclaration) s;
            Java.TypeDeclaration enclosingTypeDeclaration = (Java.TypeDeclaration) s.getEnclosingScope();

            if (
                !(enclosingTypeDeclaration instanceof Java.ClassDeclaration)
                || enclosingTypeBodyDeclaration.isStatic()
            ) {

                // No enclosing instance in
                //  + interface method declaration or
                //  + static type body declaration (here: method or initializer or field declarator)
                // context (JLS 15.9.2.BL1.B3.B1.B1).
                if (nci.iClass.getOuterIClass() != null) this.compileError("Instantiation of \"" + nci.type + "\" requires an enclosing instance", nci.getLocation());
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
//                    Java.ClassDeclaration outerClassDeclaration = (Java.ClassDeclaration) enclosingTypeDeclaration;
//                    optionalEnclosingInstance = new Java.QualifiedThisReference(
//                        nci.getLocation(),            // location
//                        outerClassDeclaration,        // declaringClass
//                        enclosingTypeBodyDeclaration, // declaringTypeBodyDeclaration
//                        optionalOuterIClass           // targetIClass
//                    );
                    optionalEnclosingInstance = new Java.QualifiedThisReference(
                        nci.getLocation(),   // location
                        new Java.SimpleType( // qualification
                            nci.getLocation(),
                            optionalOuterIClass
                        )
                    );
                    optionalEnclosingInstance.setEnclosingBlockStatement(nci.getEnclosingBlockStatement());
                }
            }
        }

        this.invokeConstructor(
            (Locatable) nci,                  // l
            nci.getEnclosingBlockStatement(), // scope
            optionalEnclosingInstance,        // optionalEnclosingInstance
            nci.iClass,                       // targetClass
            nci.arguments                     // arguments
        );
        return nci.iClass;
    }
    private IClass compileGet2(Java.NewAnonymousClassInstance naci) throws CompileException {

        // Find constructors.
        Java.AnonymousClassDeclaration acd = naci.anonymousClassDeclaration;
        IClass sc = this.resolve(acd).getSuperclass();
        IClass.IConstructor[] iConstructors = sc.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new RuntimeException("SNO: Base class has no constructors");

        // Determine most specific constructor.
        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            (Locatable) naci, // l
            iConstructors,    // iInvocables
            naci.arguments    // arguments
        );

        IClass[] pts = iConstructor.getParameterTypes();

        // Determine formal parameters of anonymous constructor.
        Java.FunctionDeclarator.FormalParameter[] fps;
        Location loc = naci.getLocation();
        {
            List l = new ArrayList(); // FormalParameter

            // Pass the enclosing instance of the base class as parameter #1.
            if (naci.optionalQualification != null) l.add(new Java.FunctionDeclarator.FormalParameter(
                loc,                                                                // location
                true,                                                               // finaL
                new Java.SimpleType(loc, this.getType(naci.optionalQualification)), // type
                "this$base"                                                         // name
            ));
            for (int i = 0; i < pts.length; ++i) l.add(new Java.FunctionDeclarator.FormalParameter(
                loc,                              // location
                true,                             // finaL
                new Java.SimpleType(loc, pts[i]), // type
                "p" + i                           // name
            ));
            fps = (Java.FunctionDeclarator.FormalParameter[]) l.toArray(new Java.FunctionDeclarator.FormalParameter[l.size()]);
        }

        // Determine thrown exceptions of anonymous constructor.
        IClass[] tes = iConstructor.getThrownExceptions();
        Java.Type[]   tets = new Java.Type[tes.length];
        for (int i = 0; i < tes.length; ++i) tets[i] = new Java.SimpleType(loc, tes[i]);

        // The anonymous constructor merely invokes the constructor of its superclass.
        int j = 0;
        Java.Rvalue optionalQualificationAccess;
        if (naci.optionalQualification == null) {
            optionalQualificationAccess = null;
        } else
        {
            optionalQualificationAccess = new Java.ParameterAccess(loc, fps[j++]);
        }
        Java.Rvalue[] parameterAccesses = new Java.Rvalue[pts.length];
        for (int i = 0; i < pts.length; ++i) {
            parameterAccesses[i] = new Java.ParameterAccess(loc, fps[j++]);
        }

        // Generate the anonymous constructor for the anonymous class (JLS 15.9.5.1).
        acd.addConstructor(new Java.ConstructorDeclarator(
            loc,                                 // location
            null,                                // optionalDocComment
            Mod.PACKAGE,                         // modifiers
            fps,                                 // formalParameters
            tets,                                // thrownExceptions
            new Java.SuperConstructorInvocation( // optionalExplicitConstructorInvocation
                loc,                         // location
                optionalQualificationAccess, // optionalQualification
                parameterAccesses            // arguments
            ),
            new Java.Block(loc)                  // optionalBody
        ));

        // Compile the anonymous class.
        this.compile(acd);

        // Instantiate the anonymous class.
        this.writeOpcode(naci, Opcode.NEW);
        this.writeConstantClassInfo(this.resolve(naci.anonymousClassDeclaration).getDescriptor());

        // Invoke the anonymous constructor.
        this.writeOpcode(naci, Opcode.DUP);
        Java.Rvalue[] arguments2;
        if (naci.optionalQualification == null) {
            arguments2 = naci.arguments;
        } else {
            arguments2 = new Java.Rvalue[naci.arguments.length + 1];
            arguments2[0] = naci.optionalQualification;
            System.arraycopy(naci.arguments, 0, arguments2, 1, naci.arguments.length);
        }

        // Notice: The enclosing instance of the anonymous class is "this", not the
        // qualification of the NewAnonymousClassInstance.
        Java.Scope s;
        for (s = naci.getEnclosingBlockStatement(); !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
        Java.ThisReference oei;
        if (((Java.TypeBodyDeclaration) s).isStatic()) {
            oei = null;
        } else
        {
            oei = new Java.ThisReference(loc);
            oei.setEnclosingBlockStatement(naci.getEnclosingBlockStatement());
        }
        this.invokeConstructor(
            (Locatable) naci,                               // l
            (Java.Scope) naci.getEnclosingBlockStatement(), // scope
            oei,                                            // optionalEnclosingInstance
            this.resolve(naci.anonymousClassDeclaration),   // targetClass
            arguments2                                      // arguments
        );
        return this.resolve(naci.anonymousClassDeclaration);
    }
    private IClass compileGet2(Java.ParameterAccess pa) throws CompileException {
        Java.LocalVariable lv = this.getLocalVariable(pa.formalParameter);
        this.load((Locatable) pa, lv);
        return lv.type;
    }
    private IClass compileGet2(Java.NewArray na) throws CompileException {
        for (int i = 0; i < na.dimExprs.length; ++i) {
            IClass dimType = this.compileGetValue(na.dimExprs[i]);
            if (dimType != IClass.INT && this.unaryNumericPromotion(
                (Locatable) na, // l
                dimType         // type
            ) != IClass.INT) this.compileError("Invalid array size expression type", na.getLocation());
        }

        return this.newArray(
            (Locatable) na,       // l
            na.dimExprs.length,   // dimExprCount
            na.dims,              // dims
            this.getType(na.type) // componentType
        );
    }
    private IClass compileGet2(Java.NewInitializedArray nia) throws CompileException {
        IClass at = this.getType(nia.arrayType);
        this.compileGetValue(nia.arrayInitializer, at);
        return at;
    }
    private void compileGetValue(Java.ArrayInitializer ai, IClass arrayType) throws CompileException {
        if (!arrayType.isArray()) this.compileError("Array initializer not allowed for non-array type \"" + arrayType.toString() + "\"");
        IClass ct = arrayType.getComponentType();

        this.pushConstant((Locatable) ai, new Integer(ai.values.length));
        this.newArray(
            (Locatable) ai, // l
            1,              // dimExprCount,
            0,              // dims,
            ct              // componentType
        );

        for (int i = 0; i < ai.values.length; ++i) {
            this.writeOpcode(ai, Opcode.DUP);
            this.pushConstant((Locatable) ai, new Integer(i));
            Java.ArrayInitializerOrRvalue aiorv = ai.values[i];
            if (aiorv instanceof Java.Rvalue) {
                Java.Rvalue rv = (Java.Rvalue) aiorv;
                this.assignmentConversion(
                    (Locatable) ai,           // l
                    this.compileGetValue(rv), // sourceType
                    ct,                       // targetType
                    this.getConstantValue(rv) // optionalConstantValue
                );
            } else
            if (aiorv instanceof Java.ArrayInitializer) {
                this.compileGetValue((Java.ArrayInitializer) aiorv, ct);
            } else
            {
                throw new RuntimeException("Unexpected array initializer or rvalue class " + aiorv.getClass().getName());
            }
            this.writeOpcode(ai, Opcode.IASTORE + UnitCompiler.ilfdabcs(ct));
        }
    }
    private IClass compileGet2(Java.Literal l) throws CompileException {
        if (
            l.value == Scanner.MAGIC_INTEGER ||
            l.value == Scanner.MAGIC_LONG
        ) this.compileError("This literal value may only appear in a negated context", l.getLocation());
        return this.pushConstant((Locatable) l, l.value == null ? Java.Rvalue.CONSTANT_VALUE_NULL : l.value);
    }

    /**
     * Convenience function that calls {@link #compileContext(Java.Rvalue)}
     * and {@link #compileGet(Java.Rvalue)}.
     * @return The type of the Rvalue
     */
    private IClass compileGetValue(Java.Rvalue rv) throws CompileException {
        Object cv = this.getConstantValue(rv);
        if (cv != null) {
            this.fakeCompile(rv); // To check that, e.g., "a" compiles in "true || a".
            this.pushConstant((Locatable) rv, cv);
            return this.getType(rv);
        }

        this.compileContext(rv);
        return this.compileGet(rv);
    }

    // -------------------- Rvalue.getConstantValue() -----------------

    /**
     * Attempts to evaluate as a constant expression.
     * <p>
     * <table>
     *   <tr><th>Expression type</th><th>Return value type</th></tr>
     *   <tr><td>String</td><td>String</td></tr>
     *   <tr><td>byte</td><td>Byte</td></tr>
     *   <tr><td>short</td><td>Chort</td></tr>
     *   <tr><td>int</td><td>Integer</td></tr>
     *   <tr><td>boolean</td><td>Boolean</td></tr>
     *   <tr><td>char</td><td>Character</td></tr>
     *   <tr><td>float</td><td>Float</td></tr>
     *   <tr><td>long</td><td>Long</td></tr>
     *   <tr><td>double</td><td>Double</td></tr>
     *   <tr><td>null</td><td>{@link Java.Rvalue#CONSTANT_VALUE_NULL}</td></tr>
     * </table>
     *
     * @return <code>null</code> iff the rvalue is not a constant value
     */
    public final Object getConstantValue(Java.Rvalue rv) throws CompileException {
        if (rv.constantValue != Java.Rvalue.CONSTANT_VALUE_UNKNOWN) return rv.constantValue;

        final Object[] res = new Object[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength               (Java.ArrayLength                al  ) {       res[0] = UnitCompiler.this.getConstantValue2(al  );                                                    }
            public void visitAssignment                (Java.Assignment                 a   ) {       res[0] = UnitCompiler.this.getConstantValue2(a   );                                                    }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { res[0] = UnitCompiler.this.getConstantValue2(uo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { try { res[0] = UnitCompiler.this.getConstantValue2(bo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCast                      (Java.Cast                       c   ) { try { res[0] = UnitCompiler.this.getConstantValue2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) {       res[0] = UnitCompiler.this.getConstantValue2(cl  );                                                    }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) {       res[0] = UnitCompiler.this.getConstantValue2(ce  );                                                    }
            public void visitCrement                   (Java.Crement                    c   ) {       res[0] = UnitCompiler.this.getConstantValue2(c   );                                                    }
            public void visitInstanceof                (Java.Instanceof                 io  ) {       res[0] = UnitCompiler.this.getConstantValue2(io  );                                                    }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) {       res[0] = UnitCompiler.this.getConstantValue2(mi  );                                                    }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) {       res[0] = UnitCompiler.this.getConstantValue2(smi );                                                    }
            public void visitLiteral                   (Java.Literal                    l   ) { try { res[0] = UnitCompiler.this.getConstantValue2(l   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) {       res[0] = UnitCompiler.this.getConstantValue2(naci);                                                    }
            public void visitNewArray                  (Java.NewArray                   na  ) {       res[0] = UnitCompiler.this.getConstantValue2(na  );                                                    }
            public void visitNewInitializedArray       (Java.NewInitializedArray        nia ) {       res[0] = UnitCompiler.this.getConstantValue2(nia );                                                    }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) {       res[0] = UnitCompiler.this.getConstantValue2(nci );                                                    }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.getConstantValue2(pa  );                                                    }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) {       res[0] = UnitCompiler.this.getConstantValue2(qtr );                                                    }
            public void visitThisReference             (Java.ThisReference              tr  ) {       res[0] = UnitCompiler.this.getConstantValue2(tr  );                                                    }

            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { res[0] = UnitCompiler.this.getConstantValue2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) {       res[0] = UnitCompiler.this.getConstantValue2(aae  );                                                    }
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { res[0] = UnitCompiler.this.getConstantValue2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) {       res[0] = UnitCompiler.this.getConstantValue2(fae  );                                                    }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {       res[0] = UnitCompiler.this.getConstantValue2(scfae);                                                    }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.getConstantValue2(lva  );                                                    }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { res[0] = UnitCompiler.this.getConstantValue2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
            rv.constantValue = res[0];
            return rv.constantValue;
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private Object getConstantValue2(Java.Rvalue rv) {
        return null;
    }
    private Object getConstantValue2(Java.AmbiguousName an) throws CompileException {
        return this.getConstantValue(this.toRvalueOrCE(this.reclassify(an)));
    }
    private Object getConstantValue2(Java.FieldAccess fa) throws CompileException {
        return fa.field.getConstantValue();
    }
    private Object getConstantValue2(Java.UnaryOperation uo) throws CompileException {
        if (uo.operator.equals("+")) return this.getConstantValue(uo.operand);
        if (uo.operator.equals("-")) return this.getNegatedConstantValue(uo.operand);
        if (uo.operator.equals("!")) {
            Object cv = this.getConstantValue(uo.operand);
            return cv instanceof Boolean ? (
                ((Boolean) cv).booleanValue() ? Boolean.FALSE : Boolean.TRUE
            ) : null;
        }
        return null;
    }
    private Object getConstantValue2(Java.BinaryOperation bo) throws CompileException {

        // null == null
        // null != null
        if (
            (bo.op == "==" || bo.op == "!=") &&
            this.getConstantValue(bo.lhs) == Java.Rvalue.CONSTANT_VALUE_NULL &&
            this.getConstantValue(bo.rhs) == Java.Rvalue.CONSTANT_VALUE_NULL
        ) return bo.op == "==" ? Boolean.TRUE : Boolean.FALSE;

        // "|", "^", "&", "*", "/", "%", "+", "-".
        if (
            bo.op == "|" ||
            bo.op == "^" ||
            bo.op == "&" ||
            bo.op == "*" ||
            bo.op == "/" ||
            bo.op == "%" ||
            bo.op == "+" ||
            bo.op == "-"
        ) {

            // Unroll the constant operands.
            List cvs = new ArrayList();
            for (Iterator it = bo.unrollLeftAssociation(); it.hasNext();) {
                Object cv = this.getConstantValue(((Java.Rvalue) it.next()));
                if (cv == null) return null;
                cvs.add(cv);
            }

            // Compute the constant value of the unrolled binary operation.
            Iterator it = cvs.iterator();
            Object lhs = it.next();
            while (it.hasNext()) {
                Object rhs = it.next();

                // String concatenation?
                if (bo.op == "+" && (lhs instanceof String || rhs instanceof String)) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(lhs.toString()).append(rhs.toString());
                    while (it.hasNext()) sb.append(it.next().toString());
                    return sb.toString();
                }

                if (!(lhs instanceof Number) || !(rhs instanceof Number)) return null;

                try {
                    // Numeric binary operation.
                    if (lhs instanceof Double || rhs instanceof Double) {
                        double lhsD = ((Number) lhs).doubleValue();
                        double rhsD = ((Number) rhs).doubleValue();
                        double cvD;
                        if (bo.op == "*") cvD = lhsD * rhsD; else
                        if (bo.op == "/") cvD = lhsD / rhsD; else
                        if (bo.op == "%") cvD = lhsD % rhsD; else
                        if (bo.op == "+") cvD = lhsD + rhsD; else
                        if (bo.op == "-") cvD = lhsD - rhsD; else return null;
                        lhs = new Double(cvD);
                    } else
                    if (lhs instanceof Float || rhs instanceof Float) {
                        float lhsF = ((Number) lhs).floatValue();
                        float rhsF = ((Number) rhs).floatValue();
                        float cvF;
                        if (bo.op == "*") cvF = lhsF * rhsF; else
                        if (bo.op == "/") cvF = lhsF / rhsF; else
                        if (bo.op == "%") cvF = lhsF % rhsF; else
                        if (bo.op == "+") cvF = lhsF + rhsF; else
                        if (bo.op == "-") cvF = lhsF - rhsF; else return null;
                        lhs = new Float(cvF);
                    } else
                    if (lhs instanceof Long || rhs instanceof Long) {
                        long lhsL = ((Number) lhs).longValue();
                        long rhsL = ((Number) rhs).longValue();
                        long cvL;
                        if (bo.op == "|") cvL = lhsL | rhsL; else
                        if (bo.op == "^") cvL = lhsL ^ rhsL; else
                        if (bo.op == "&") cvL = lhsL & rhsL; else
                        if (bo.op == "*") cvL = lhsL * rhsL; else
                        if (bo.op == "/") cvL = lhsL / rhsL; else
                        if (bo.op == "%") cvL = lhsL % rhsL; else
                        if (bo.op == "+") cvL = lhsL + rhsL; else
                        if (bo.op == "-") cvL = lhsL - rhsL; else return null;
                        lhs = new Long(cvL);
                    } else
                    {
                        int lhsI = ((Number) lhs).intValue();
                        int rhsI = ((Number) rhs).intValue();
                        int cvI;
                        if (bo.op == "|") cvI = lhsI | rhsI; else
                        if (bo.op == "^") cvI = lhsI ^ rhsI; else
                        if (bo.op == "&") cvI = lhsI & rhsI; else
                        if (bo.op == "*") cvI = lhsI * rhsI; else
                        if (bo.op == "/") cvI = lhsI / rhsI; else
                        if (bo.op == "%") cvI = lhsI % rhsI; else
                        if (bo.op == "+") cvI = lhsI + rhsI; else
                        if (bo.op == "-") cvI = lhsI - rhsI; else return null;
                        lhs = new Integer(cvI);
                    }
                } catch (ArithmeticException ae) {
                    // Most likely a div by zero or mod by zero.
                    // Guess we can't make this expression into a constant.
                    return null;
                }
            }
            return lhs;
        }

        // "&&" and "||" with constant LHS operand.
        if (
            bo.op == "&&" ||
            bo.op == "||"
        ) {
            Object lhsValue = this.getConstantValue(bo.lhs);
            if (lhsValue instanceof Boolean) {
                boolean lhsBV = ((Boolean) lhsValue).booleanValue();
                return (
                    bo.op == "&&" ?
                    (lhsBV ? this.getConstantValue(bo.rhs) : Boolean.FALSE) :
                    (lhsBV ? Boolean.TRUE : this.getConstantValue(bo.rhs))
                );
            }
        }

        return null;
    }
    private Object getConstantValue2(Java.Cast c) throws CompileException {
        Object cv = this.getConstantValue(c.value);
        if (cv == null) return null;

        if (cv instanceof Number) {
            IClass tt = this.getType(c.targetType);
            if (tt == IClass.BYTE  ) return new Byte(((Number) cv).byteValue());
            if (tt == IClass.SHORT ) return new Short(((Number) cv).shortValue());
            if (tt == IClass.INT   ) return new Integer(((Number) cv).intValue());
            if (tt == IClass.LONG  ) return new Long(((Number) cv).longValue());
            if (tt == IClass.FLOAT ) return new Float(((Number) cv).floatValue());
            if (tt == IClass.DOUBLE) return new Double(((Number) cv).doubleValue());
        }

        return null;
    }
    private Object getConstantValue2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getConstantValue(pe.value);
    }
    private Object getConstantValue2(Java.Literal l) throws CompileException {
        if (
            l.value == Scanner.MAGIC_INTEGER ||
            l.value == Scanner.MAGIC_LONG
        ) this.compileError("This literal value may only appear in a negated context", l.getLocation());
        return l.value == null ? Java.Rvalue.CONSTANT_VALUE_NULL : l.value;
    }

    /**
     * Attempts to evaluate the negated value of a constant {@link Java.Rvalue}.
     * This is particularly relevant for the smallest value of an integer or
     * long literal.
     *
     * @return null if value is not constant; otherwise a String, Byte,
     * Short, Integer, Boolean, Character, Float, Long or Double
     */
    private final Object getNegatedConstantValue(Java.Rvalue rv) throws CompileException {
        final Object[] res = new Object[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayLength               (Java.ArrayLength                al  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(al  );                                                                }
            public void visitAssignment                (Java.Assignment                 a   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(a   );                                                                }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(uo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(bo  );                                                                }
            public void visitCast                      (Java.Cast                       c   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(c   );                                                                }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(cl  );                                                                }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(ce  );                                                                }
            public void visitCrement                   (Java.Crement                    c   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(c   );                                                                }
            public void visitInstanceof                (Java.Instanceof                 io  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(io  );                                                                }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(mi  );                                                                }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(smi );                                                                }
            public void visitLiteral                   (Java.Literal                    l   ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(l   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(naci);                                                                }
            public void visitNewArray                  (Java.NewArray                   na  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(na  );                                                                }
            public void visitNewInitializedArray       (Java.NewInitializedArray        nia ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(nia );                                                                }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(nci );                                                                }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(pa  );                                                                }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(qtr );                                                                }
            public void visitThisReference             (Java.ThisReference              tr  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(tr  );                                                                }

            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(an   );                                                    }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(aae  );                                                    }
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(fa   );                                                    }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(fae  );                                                    }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(scfae);                                                    }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(lva  );                                                    }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private Object getNegatedConstantValue2(Java.Rvalue rv) {
        return null;
    }
    private Object getNegatedConstantValue2(Java.UnaryOperation uo) throws CompileException {
        return (
            uo.operator.equals("+") ? this.getNegatedConstantValue(uo.operand) :
            uo.operator.equals("-") ? this.getConstantValue(uo.operand) :
            null
        );
    }
    private Object getNegatedConstantValue2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getNegatedConstantValue(pe.value);
    }
    private Object getNegatedConstantValue2(Java.Literal l) throws CompileException {
        if (l.value instanceof Integer) return new Integer(-((Integer) l.value).intValue()   );
        if (l.value instanceof Long   ) return new Long   (-((Long   ) l.value).longValue()  );
        if (l.value instanceof Float  ) return new Float  (-((Float  ) l.value).floatValue() );
        if (l.value instanceof Double ) return new Double (-((Double ) l.value).doubleValue());

        this.compileError("Cannot negate this literal", l.getLocation());
        return null;
    }

    // ------------ BlockStatement.generatesCode() -------------

    /**
     * Check whether invocation of {@link #compile(Java.BlockStatement)} would
     * generate more than zero code bytes.
     */
    private boolean generatesCode(Java.BlockStatement bs) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.BlockStatementVisitor bsv = new Visitor.BlockStatementVisitor() {
            public void visitInitializer                      (Java.Initializer                       i   ) { try { res[0] = UnitCompiler.this.generatesCode2(i   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldDeclaration                 (Java.FieldDeclaration                  fd  ) { try { res[0] = UnitCompiler.this.generatesCode2(fd  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLabeledStatement                 (Java.LabeledStatement                  ls  ) {       res[0] = UnitCompiler.this.generatesCode2(ls  );                                                                }
            public void visitBlock                            (Java.Block                             b   ) { try { res[0] = UnitCompiler.this.generatesCode2(b   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitExpressionStatement              (Java.ExpressionStatement               es  ) {       res[0] = UnitCompiler.this.generatesCode2(es  );                                                                }
            public void visitIfStatement                      (Java.IfStatement                       is  ) {       res[0] = UnitCompiler.this.generatesCode2(is  );                                                                }
            public void visitForStatement                     (Java.ForStatement                      fs  ) {       res[0] = UnitCompiler.this.generatesCode2(fs  );                                                                }
            public void visitWhileStatement                   (Java.WhileStatement                    ws  ) {       res[0] = UnitCompiler.this.generatesCode2(ws  );                                                                }
            public void visitTryStatement                     (Java.TryStatement                      ts  ) {       res[0] = UnitCompiler.this.generatesCode2(ts  );                                                                }
            public void visitSwitchStatement                  (Java.SwitchStatement                   ss  ) {       res[0] = UnitCompiler.this.generatesCode2(ss  );                                                                }
            public void visitSynchronizedStatement            (Java.SynchronizedStatement             ss  ) {       res[0] = UnitCompiler.this.generatesCode2(ss  );                                                                }
            public void visitDoStatement                      (Java.DoStatement                       ds  ) {       res[0] = UnitCompiler.this.generatesCode2(ds  );                                                                }
            public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) {       res[0] = UnitCompiler.this.generatesCode2(lvds);                                                                }
            public void visitReturnStatement                  (Java.ReturnStatement                   rs  ) {       res[0] = UnitCompiler.this.generatesCode2(rs  );                                                                }
            public void visitThrowStatement                   (Java.ThrowStatement                    ts  ) {       res[0] = UnitCompiler.this.generatesCode2(ts  );                                                                }
            public void visitBreakStatement                   (Java.BreakStatement                    bs  ) {       res[0] = UnitCompiler.this.generatesCode2(bs  );                                                                }
            public void visitContinueStatement                (Java.ContinueStatement                 cs  ) {       res[0] = UnitCompiler.this.generatesCode2(cs  );                                                                }
            public void visitEmptyStatement                   (Java.EmptyStatement                    es  ) {       res[0] = UnitCompiler.this.generatesCode2(es  );                                                                }
            public void visitLocalClassDeclarationStatement   (Java.LocalClassDeclarationStatement    lcds) {       res[0] = UnitCompiler.this.generatesCode2(lcds);                                                                }
            public void visitAlternateConstructorInvocation   (Java.AlternateConstructorInvocation    aci ) {       res[0] = UnitCompiler.this.generatesCode2(aci );                                                                }
            public void visitSuperConstructorInvocation       (Java.SuperConstructorInvocation        sci ) {       res[0] = UnitCompiler.this.generatesCode2(sci );                                                                }
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    public boolean generatesCode2(Java.BlockStatement bs) { return true; }
    public boolean generatesCode2(Java.EmptyStatement es) { return false; }
    public boolean generatesCode2(Java.LocalClassDeclarationStatement lcds) { return false; }
    public boolean generatesCode2(Java.Initializer i) throws CompileException { return this.generatesCode(i.block); }
    // Takes a List<Java.BlockStatement>.
    public boolean generatesCode2ListStatements(List l) throws CompileException {
        for (int i = 0; i < l.size(); ++i) {
            if (this.generatesCode(((Java.BlockStatement) l.get(i)))) return true;
        }
        return false;
    }
    public boolean generatesCode2(Java.Block b) throws CompileException {
        return generatesCode2ListStatements(b.statements);
    }
    public boolean generatesCode2(Java.FieldDeclaration fd) throws CompileException {
        // Code is only generated if at least one of the declared variables has a
        // non-constant-final initializer.
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            Java.VariableDeclarator vd = fd.variableDeclarators[i];
            if (this.getNonConstantFinalInitializer(fd, vd) != null) return true;
        }
        return false;
    }

    // ------------ BlockStatement.leave() -------------

    /**
     * Clean up the statement context. This is currently relevant for
     * "try ... catch ... finally" statements (execute "finally" clause)
     * and "synchronized" statements (monitorexit).
     * <p>
     * Statements like "return", "break", "continue" must call this method
     * for all the statements they terminate.
     * <p>
     * Notice: If <code>optionalStackValueType</code> is <code>null</code>,
     * then the operand stack is empty; otherwise exactly one operand with that
     * type is on the stack. This information is vital to implementations of
     * {@link #leave(Java.BlockStatement, IClass)} that require a specific
     * operand stack state (e.g. an empty operand stack for JSR).
     */
    private void leave(Java.BlockStatement bs, final IClass optionalStackValueType) {
        Visitor.BlockStatementVisitor bsv = new Visitor.BlockStatementVisitor() {
            public void visitInitializer                      (Java.Initializer                       i   ) { UnitCompiler.this.leave2(i,    optionalStackValueType); }
            public void visitFieldDeclaration                 (Java.FieldDeclaration                  fd  ) { UnitCompiler.this.leave2(fd,   optionalStackValueType); }
            public void visitLabeledStatement                 (Java.LabeledStatement                  ls  ) { UnitCompiler.this.leave2(ls,   optionalStackValueType); }
            public void visitBlock                            (Java.Block                             b   ) { UnitCompiler.this.leave2(b,    optionalStackValueType); }
            public void visitExpressionStatement              (Java.ExpressionStatement               es  ) { UnitCompiler.this.leave2(es,   optionalStackValueType); }
            public void visitIfStatement                      (Java.IfStatement                       is  ) { UnitCompiler.this.leave2(is,   optionalStackValueType); }
            public void visitForStatement                     (Java.ForStatement                      fs  ) { UnitCompiler.this.leave2(fs,   optionalStackValueType); }
            public void visitWhileStatement                   (Java.WhileStatement                    ws  ) { UnitCompiler.this.leave2(ws,   optionalStackValueType); }
            public void visitTryStatement                     (Java.TryStatement                      ts  ) { UnitCompiler.this.leave2(ts,   optionalStackValueType); }
            public void visitSwitchStatement                  (Java.SwitchStatement                   ss  ) { UnitCompiler.this.leave2(ss,   optionalStackValueType); }
            public void visitSynchronizedStatement            (Java.SynchronizedStatement             ss  ) { UnitCompiler.this.leave2(ss,   optionalStackValueType); }
            public void visitDoStatement                      (Java.DoStatement                       ds  ) { UnitCompiler.this.leave2(ds,   optionalStackValueType); }
            public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) { UnitCompiler.this.leave2(lvds, optionalStackValueType); }
            public void visitReturnStatement                  (Java.ReturnStatement                   rs  ) { UnitCompiler.this.leave2(rs,   optionalStackValueType); }
            public void visitThrowStatement                   (Java.ThrowStatement                    ts  ) { UnitCompiler.this.leave2(ts,   optionalStackValueType); }
            public void visitBreakStatement                   (Java.BreakStatement                    bs  ) { UnitCompiler.this.leave2(bs,   optionalStackValueType); }
            public void visitContinueStatement                (Java.ContinueStatement                 cs  ) { UnitCompiler.this.leave2(cs,   optionalStackValueType); }
            public void visitEmptyStatement                   (Java.EmptyStatement                    es  ) { UnitCompiler.this.leave2(es,   optionalStackValueType); }
            public void visitLocalClassDeclarationStatement   (Java.LocalClassDeclarationStatement    lcds) { UnitCompiler.this.leave2(lcds, optionalStackValueType); }
            public void visitAlternateConstructorInvocation   (Java.AlternateConstructorInvocation    aci ) { UnitCompiler.this.leave2(aci,  optionalStackValueType); }
            public void visitSuperConstructorInvocation       (Java.SuperConstructorInvocation        sci ) { UnitCompiler.this.leave2(sci,  optionalStackValueType); }
        };
        bs.accept(bsv);
    }
    public void leave2(Java.BlockStatement bs, IClass optionalStackValueType) { ; }
    public void leave2(Java.SynchronizedStatement ss, IClass optionalStackValueType) {
        this.load((Locatable) ss, this.iClassLoader.OBJECT, ss.monitorLvIndex);
        this.writeOpcode(ss, Opcode.MONITOREXIT);
    }
    public void leave2(Java.TryStatement ts, IClass optionalStackValueType) {
        if (ts.finallyOffset != null) {

            this.codeContext.saveLocalVariables();
            try {
                short sv = 0;

                // Obviously, JSR must always be executed with the operand stack being
                // empty; otherwise we get "java.lang.VerifyError: Inconsistent stack height
                // 1 != 2"
                if (optionalStackValueType != null) {
                    sv = this.codeContext.allocateLocalVariable(Descriptor.size(optionalStackValueType.getDescriptor()));
                    this.store((Locatable) ts, optionalStackValueType, sv);
                }
    
                this.writeBranch(ts, Opcode.JSR, ts.finallyOffset);
    
                if (optionalStackValueType != null) {
                    this.load((Locatable) ts, optionalStackValueType, sv);
                }
            } finally {
                this.codeContext.restoreLocalVariables();
            }
        }
    }

    // ---------------- Lvalue.compileSet() -----------------

    /**
     * Generates code that stores a value in the {@link Java.Lvalue}.
     * Expects the {@link Java.Lvalue}'s context (see {@link
     * #compileContext}) and a value of the {@link Java.Lvalue}'s type
     * on the operand stack.
     */
    private void compileSet(Java.Lvalue lv) throws CompileException {
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.LvalueVisitor lvv = new Visitor.LvalueVisitor() {
            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { UnitCompiler.this.compileSet2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) { try { UnitCompiler.this.compileSet2(aae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { UnitCompiler.this.compileSet2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) { try { UnitCompiler.this.compileSet2(fae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) { try { UnitCompiler.this.compileSet2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       UnitCompiler.this.compileSet2(lva  );                                                    }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { UnitCompiler.this.compileSet2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            lv.accept(lvv);
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private void compileSet2(Java.AmbiguousName an) throws CompileException {
        this.compileSet(this.toLvalueOrCE(this.reclassify(an)));
    }
    private void compileSet2(Java.LocalVariableAccess lva) {
        this.store(
            (Locatable) lva,
            lva.localVariable.type,
            lva.localVariable
        );
    }
    private void compileSet2(Java.FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.getEnclosingBlockStatement());
        this.writeOpcode(fa, (
            fa.field.isStatic() ?
            Opcode.PUTSTATIC :
            Opcode.PUTFIELD
        ));
        this.writeConstantFieldrefInfo(
            fa.field.getDeclaringIClass().getDescriptor(),
            fa.field.getName(), // classFD
            fa.field.getDescriptor()                       // fieldFD
        );
    }
    private void compileSet2(Java.ArrayAccessExpression aae) throws CompileException {
        this.writeOpcode(aae, Opcode.IASTORE + UnitCompiler.ilfdabcs(this.getType(aae)));
    }
    private void compileSet2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        this.compileSet(this.toLvalueOrCE(fae.value));
    }
    private void compileSet2(Java.SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        this.compileSet(this.toLvalueOrCE(scfae.value));
    }
    private void compileSet2(Java.ParenthesizedExpression pe) throws CompileException {
        this.compileSet(this.toLvalueOrCE(pe.value));
    }

    // ---------------- Atom.getType() ----------------

    private IClass getType(Java.Atom a) throws CompileException {
        final IClass[] res = new IClass[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.AtomVisitor av = new Visitor.AtomVisitor() {
            // AtomVisitor
            public void visitPackage                       (Java.Package                        p   ) { try { res[0] = UnitCompiler.this.getType2(p   ); } catch (CompileException e) { throw new UCE(e); } }
            // TypeVisitor
            public void visitArrayType                     (Java.ArrayType                      at  ) { try { res[0] = UnitCompiler.this.getType2(at  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBasicType                     (Java.BasicType                      bt  ) {       res[0] = UnitCompiler.this.getType2(bt  );                                                                }
            public void visitReferenceType                 (Java.ReferenceType                  rt  ) { try { res[0] = UnitCompiler.this.getType2(rt  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitRvalueMemberType              (Java.RvalueMemberType               rmt ) { try { res[0] = UnitCompiler.this.getType2(rmt ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSimpleType                    (Java.SimpleType                     st  ) {       res[0] = UnitCompiler.this.getType2(st  );                                                                }
            // RvalueVisitor
            public void visitArrayLength                   (Java.ArrayLength                    al  ) {       res[0] = UnitCompiler.this.getType2(al  );                                                                }
            public void visitAssignment                    (Java.Assignment                     a   ) { try { res[0] = UnitCompiler.this.getType2(a   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) { try { res[0] = UnitCompiler.this.getType2(uo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) { try { res[0] = UnitCompiler.this.getType2(bo  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCast                          (Java.Cast                           c   ) { try { res[0] = UnitCompiler.this.getType2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) {       res[0] = UnitCompiler.this.getType2(cl  );                                                                }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) { try { res[0] = UnitCompiler.this.getType2(ce  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitCrement                       (Java.Crement                        c   ) { try { res[0] = UnitCompiler.this.getType2(c   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitInstanceof                    (Java.Instanceof                     io  ) {       res[0] = UnitCompiler.this.getType2(io  );                                                                }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) { try { res[0] = UnitCompiler.this.getType2(mi  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) { try { res[0] = UnitCompiler.this.getType2(smi ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLiteral                       (Java.Literal                        l   ) {       res[0] = UnitCompiler.this.getType2(l   );                                                                }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) {       res[0] = UnitCompiler.this.getType2(naci);                                                                }
            public void visitNewArray                      (Java.NewArray                       na  ) { try { res[0] = UnitCompiler.this.getType2(na  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewInitializedArray           (Java.NewInitializedArray            nia ) { try { res[0] = UnitCompiler.this.getType2(nia ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) { try { res[0] = UnitCompiler.this.getType2(nci ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) { try { res[0] = UnitCompiler.this.getType2(pa  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) { try { res[0] = UnitCompiler.this.getType2(qtr ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitThisReference                 (Java.ThisReference                  tr  ) { try { res[0] = UnitCompiler.this.getType2(tr  ); } catch (CompileException e) { throw new UCE(e); } }
            // LvalueVisitor
            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { res[0] = UnitCompiler.this.getType2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) { try { res[0] = UnitCompiler.this.getType2(aae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) { try { res[0] = UnitCompiler.this.getType2(fa   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) { try { res[0] = UnitCompiler.this.getType2(fae  ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) { try { res[0] = UnitCompiler.this.getType2(scfae); } catch (CompileException e) { throw new UCE(e); } }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.getType2(lva  );                                                                }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) { try { res[0] = UnitCompiler.this.getType2(pe   ); } catch (CompileException e) { throw new UCE(e); } }
        };
        try {
            a.accept(av);
            return res[0] != null ? res[0] : this.iClassLoader.OBJECT;
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private IClass getType2(Java.SimpleType st) { return st.iClass; }
    private IClass getType2(Java.BasicType bt) {
        switch (bt.index) {
            case Java.BasicType.VOID:    return IClass.VOID;
            case Java.BasicType.BYTE:    return IClass.BYTE;
            case Java.BasicType.SHORT:   return IClass.SHORT;
            case Java.BasicType.CHAR:    return IClass.CHAR;
            case Java.BasicType.INT:     return IClass.INT;
            case Java.BasicType.LONG:    return IClass.LONG;
            case Java.BasicType.FLOAT:   return IClass.FLOAT;
            case Java.BasicType.DOUBLE:  return IClass.DOUBLE;
            case Java.BasicType.BOOLEAN: return IClass.BOOLEAN;
            default: throw new RuntimeException("Invalid index " + bt.index);
        }
    }
    private IClass getType2(Java.ReferenceType rt) throws CompileException {
        Java.BlockStatement  scopeBlockStatement = null;
        Java.TypeDeclaration scopeTypeDeclaration = null;
        Java.CompilationUnit scopeCompilationUnit;
        for (Java.Scope s = rt.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof Java.BlockStatement && scopeBlockStatement == null) {
                scopeBlockStatement = (Java.BlockStatement) s;
            }
            if (s instanceof Java.TypeDeclaration && scopeTypeDeclaration == null) {
                scopeTypeDeclaration = (Java.TypeDeclaration) s;
            }
            if (s instanceof Java.CompilationUnit) {
                scopeCompilationUnit = (Java.CompilationUnit) s;
                break;
            }
        }

        if (rt.identifiers.length == 1) {

            // 6.5.5.1 Simple type name (single identifier).
            String simpleTypeName = rt.identifiers[0];

            // 6.5.5.1.1 Local class.
            {
                Java.LocalClassDeclaration lcd = this.findLocalClassDeclaration(rt.getEnclosingScope(), simpleTypeName);
                if (lcd != null) return this.resolve(lcd);
            }

            // 6.5.5.1.2 Member type.
            if (scopeTypeDeclaration != null) { // If enclosed by another type declaration...
                for (Java.Scope s = scopeTypeDeclaration; !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
                    if (s instanceof Java.TypeDeclaration) {
                        IClass mt = this.findMemberType(this.resolve((Java.AbstractTypeDeclaration) s), simpleTypeName, rt.getLocation());
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
                    Java.PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(simpleTypeName);
                    if (pmtd != null) return this.resolve((Java.AbstractTypeDeclaration) pmtd);
                }
            }

            // 6.5.5.1.5 Type declared in other compilation unit of same
            // package.
            {
                String pkg = (
                    scopeCompilationUnit.optionalPackageDeclaration == null ? null :
                    scopeCompilationUnit.optionalPackageDeclaration.packageName
                );
                String className = pkg == null ? simpleTypeName : pkg + "." + simpleTypeName;
                IClass result = findClassByName(rt.getLocation(), className);
                if (result != null) return result;
            }

            // 6.5.5.1.6 Type-import-on-demand declaration.
            {
                IClass importedClass = this.importTypeOnDemand(simpleTypeName, rt.getLocation());
                if (importedClass != null) return importedClass;
            }

            // JLS3 ???:  Type imported through single static import.
            {
                Object o = this.singleStaticImports.get(simpleTypeName);
                if (o instanceof IClass) return (IClass) o;
            }

            // JLS3 ???: Type imported through static-import-on-demand.
            {
                IClass importedMemberType = null;
                for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                    IClass ic = (IClass) it.next();
                    IClass[] memberTypes = ic.getDeclaredIClasses();
                    for (int i = 0; i < memberTypes.length; ++i) {
                        IClass mt = memberTypes[i];
                        if (!UnitCompiler.this.isAccessible(mt, scopeBlockStatement)) continue;
                        if (mt.getDescriptor().endsWith('$' + simpleTypeName + ';')) {
                            if (importedMemberType != null) UnitCompiler.this.compileError("Ambiguous static imports: \"" + importedMemberType.toString() + "\" vs. \"" + mt.toString() + "\"");
                            importedMemberType = mt;
                        }
                    }
                }
                if (importedMemberType != null) return importedMemberType;
            }

            // 6.5.5.1.8 Give up.
            this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"", rt.getLocation());
            return this.iClassLoader.OBJECT;
        } else {

            // 6.5.5.2 Qualified type name (two or more identifiers).
            Java.Atom q = this.reclassifyName(rt.getLocation(), rt.getEnclosingScope(), rt.identifiers, rt.identifiers.length - 1);

            // 6.5.5.2.1 PACKAGE.CLASS
            if (q instanceof Java.Package) {
                String className = Java.join(rt.identifiers, ".");
                IClass result = findClassByName(rt.getLocation(), className);
                if (result != null) return result;

                this.compileError("Class \"" + className + "\" not found", rt.getLocation());
                return this.iClassLoader.OBJECT;
            }

            // 6.5.5.2.2 CLASS.CLASS (member type)
            String memberTypeName = rt.identifiers[rt.identifiers.length - 1];
            IClass[] types = this.getType(this.toTypeOrCE(q)).findMemberType(memberTypeName);
            if (types.length == 1) return types[0];
            if (types.length == 0) {
                this.compileError("\"" + q + "\" declares no member type \"" + memberTypeName + "\"", rt.getLocation());
            } else
            {
                this.compileError("\"" + q + "\" and its supertypes declare more than one member type \"" + memberTypeName + "\"", rt.getLocation());
            }
            return this.iClassLoader.OBJECT;
        }
    }

    private IClass getType2(Java.RvalueMemberType rvmt) throws CompileException {
        IClass rvt = this.getType(rvmt.rvalue);
        IClass memberType = this.findMemberType(rvt, rvmt.identifier, rvmt.getLocation());
        if (memberType == null) this.compileError("\"" + rvt + "\" has no member type \"" + rvmt.identifier + "\"", rvmt.getLocation());
        return memberType;
    }
    private IClass getType2(Java.ArrayType at) throws CompileException {
        return this.getType(at.componentType).getArrayIClass(this.iClassLoader.OBJECT);
    }
    private IClass getType2(Java.AmbiguousName an) throws CompileException {
        return this.getType(this.reclassify(an));
    }
    private IClass getType2(Java.Package p) throws CompileException {
        this.compileError("Unknown variable or type \"" + p.name + "\"", p.getLocation());
        return this.iClassLoader.OBJECT;
    }
    private IClass getType2(Java.LocalVariableAccess lva) {
        return lva.localVariable.type;
    }
    private IClass getType2(Java.FieldAccess fa) throws CompileException {
        return fa.field.getType();
    }
    private IClass getType2(Java.ArrayLength al) {
        return IClass.INT;
    }
    private IClass getType2(Java.ThisReference tr) throws CompileException {
        return this.getIClass(tr);
    }
    private IClass getType2(Java.QualifiedThisReference qtr) throws CompileException {
        return this.getTargetIClass(qtr);
    }
    private IClass getType2(Java.ClassLiteral cl) {
        return this.iClassLoader.CLASS;
    }
    private IClass getType2(Java.Assignment a) throws CompileException {
        return this.getType(a.lhs);
    }
    private IClass getType2(Java.ConditionalExpression ce) throws CompileException {
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
            return this.binaryNumericPromotionType((Java.Locatable) ce, mhsType, rhsType);
        } else
        if (this.getConstantValue(ce.mhs) == Java.Rvalue.CONSTANT_VALUE_NULL && !rhsType.isPrimitive()) {

            // JLS 15.25.1.3 (null : ref)
            return rhsType;
        } else
        if (!mhsType.isPrimitive() && this.getConstantValue(ce.rhs) == Java.Rvalue.CONSTANT_VALUE_NULL) {

            // JLS 15.25.1.3 (ref : null)
            return mhsType;
        } else
        if (!mhsType.isPrimitive() && !rhsType.isPrimitive()) {
            if (mhsType.isAssignableFrom(rhsType)) {
                return mhsType;
            } else
            if (rhsType.isAssignableFrom(mhsType)) {
                return rhsType;
            } else {
                this.compileError("Reference types \"" + mhsType + "\" and \"" + rhsType + "\" don't match", ce.getLocation());
                return this.iClassLoader.OBJECT;
            }
        } else
        {
            this.compileError("Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"", ce.getLocation());
            return this.iClassLoader.OBJECT;
        }
    }
    private IClass getType2(Java.Crement c) throws CompileException {
        return this.getType(c.operand);
    }
    private IClass getType2(Java.ArrayAccessExpression aae) throws CompileException {
        return this.getType(aae.lhs).getComponentType();
    }
    private IClass getType2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.getType(fae.value);
    }
    private IClass getType2(Java.SuperclassFieldAccessExpression scfae) throws CompileException {
        this.determineValue(scfae);
        return this.getType(scfae.value);
    }
    private IClass getType2(Java.UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") return IClass.BOOLEAN;

        if (
            uo.operator == "+" ||
            uo.operator == "-" ||
            uo.operator == "~"
        ) return this.unaryNumericPromotionType((Locatable) uo, this.getUnboxedType(this.getType(uo.operand)));

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return IClass.BOOLEAN;
    }
    private IClass getType2(Java.Instanceof io) {
        return IClass.BOOLEAN;
    }
    private IClass getType2(Java.BinaryOperation bo) throws CompileException {
        if (
            bo.op == "||" ||
            bo.op == "&&" ||
            bo.op == "==" ||
            bo.op == "!=" ||
            bo.op == "<"  ||
            bo.op == ">"  ||
            bo.op == "<=" ||
            bo.op == ">="
        ) return IClass.BOOLEAN;

        if (
            bo.op == "|" ||
            bo.op == "^" ||
            bo.op == "&"
        ) {
            IClass lhsType = this.getType(bo.lhs);
            return (
                lhsType == IClass.BOOLEAN || lhsType == this.iClassLoader.BOOLEAN ?
                IClass.BOOLEAN :
                this.binaryNumericPromotionType(
                    (Java.Locatable) bo,
                    lhsType,
                    this.getType(bo.rhs)
                )
            );
        }

        if (
            bo.op == "*"   ||
            bo.op == "/"   ||
            bo.op == "%"   ||
            bo.op == "+"   ||
            bo.op == "-"
        ) {
            IClassLoader icl = this.iClassLoader;

            // Unroll the operands of this binary operation.
            Iterator ops = bo.unrollLeftAssociation();

            // Check the far left operand type.
            IClass lhsType = this.getUnboxedType(this.getType(((Java.Rvalue) ops.next())));
            if (bo.op == "+" && lhsType == icl.STRING) return icl.STRING;

            // Determine the expression type.
            do {
                IClass rhsType = this.getUnboxedType(this.getType(((Java.Rvalue) ops.next())));
                if (bo.op == "+" && rhsType == icl.STRING) return icl.STRING;
                lhsType = this.binaryNumericPromotionType((Java.Locatable) bo, lhsType, rhsType);
            } while (ops.hasNext());
            return lhsType;
        }

        if (
            bo.op == "<<"  ||
            bo.op == ">>"  ||
            bo.op == ">>>"
        ) {
            IClass lhsType = this.getType(bo.lhs);
            return this.unaryNumericPromotionType((Locatable) bo, lhsType);
        }

        this.compileError("Unexpected operator \"" + bo.op + "\"", bo.getLocation());
        return this.iClassLoader.OBJECT;
    }
    private IClass getUnboxedType(IClass type) {
        IClass c = this.isUnboxingConvertible(type);
        return c != null ? c : type;
    }
    private IClass getType2(Java.Cast c) throws CompileException {
        return this.getType(c.targetType);
    }
    private IClass getType2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getType(pe.value);
    }
//    private IClass getType2(Java.ConstructorInvocation ci) {
//        throw new RuntimeException();
//    }
    private IClass getType2(Java.MethodInvocation mi) throws CompileException {
        if (mi.iMethod == null) {
            mi.iMethod = this.findIMethod(mi);
        }
        return mi.iMethod.getReturnType();
    }
    private IClass getType2(Java.SuperclassMethodInvocation scmi) throws CompileException {
        return this.findIMethod(scmi).getReturnType();
    }
    private IClass getType2(Java.NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);
        return nci.iClass;
    }
    private IClass getType2(Java.NewAnonymousClassInstance naci) {
        return this.resolve(naci.anonymousClassDeclaration);
    }
    private IClass getType2(Java.ParameterAccess pa) throws CompileException {
        return this.getLocalVariable(pa.formalParameter).type;
    }
    private IClass getType2(Java.NewArray na) throws CompileException {
        IClass res = this.getType(na.type);
        return res.getArrayIClass(na.dimExprs.length + na.dims, this.iClassLoader.OBJECT);
    }
    private IClass getType2(Java.NewInitializedArray nia) throws CompileException {
        return this.getType(nia.arrayType);
    }
    private IClass getType2(Java.Literal l) {
        if (l.value instanceof Integer  ) return IClass.INT;
        if (l.value instanceof Long     ) return IClass.LONG;
        if (l.value instanceof Float    ) return IClass.FLOAT;
        if (l.value instanceof Double   ) return IClass.DOUBLE;
        if (l.value instanceof String   ) return this.iClassLoader.STRING;
        if (l.value instanceof Character) return IClass.CHAR;
        if (l.value instanceof Boolean  ) return IClass.BOOLEAN;
        if (l.value == null             ) return IClass.VOID;
        throw new RuntimeException("SNO: Unidentifiable literal type \"" + l.value.getClass().getName() + "\"");
    }

    // ---------------- Atom.isType() ---------------

    private boolean isType(Java.Atom a) throws CompileException {
        final boolean[] res = new boolean[1];
        class UCE extends RuntimeException { final CompileException ce; UCE(CompileException ce) { this.ce = ce; } }
        Visitor.AtomVisitor av = new Visitor.AtomVisitor() {
            // AtomVisitor
            public void visitPackage(Java.Package p) { res[0] = UnitCompiler.this.isType2(p); }
            // TypeVisitor
            public void visitArrayType       (Java.ArrayType        at  ) { res[0] = UnitCompiler.this.isType2(at ); }
            public void visitBasicType       (Java.BasicType        bt  ) { res[0] = UnitCompiler.this.isType2(bt ); }
            public void visitReferenceType   (Java.ReferenceType    rt  ) { res[0] = UnitCompiler.this.isType2(rt ); }
            public void visitRvalueMemberType(Java.RvalueMemberType rmt ) { res[0] = UnitCompiler.this.isType2(rmt); }
            public void visitSimpleType      (Java.SimpleType       st  ) { res[0] = UnitCompiler.this.isType2(st ); }
            // RvalueVisitor
            public void visitArrayLength               (Java.ArrayLength                al  ) { res[0] = UnitCompiler.this.isType2(al  ); }
            public void visitAssignment                (Java.Assignment                 a   ) { res[0] = UnitCompiler.this.isType2(a   ); }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { res[0] = UnitCompiler.this.isType2(uo  ); }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { res[0] = UnitCompiler.this.isType2(bo  ); }
            public void visitCast                      (Java.Cast                       c   ) { res[0] = UnitCompiler.this.isType2(c   ); }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) { res[0] = UnitCompiler.this.isType2(cl  ); }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) { res[0] = UnitCompiler.this.isType2(ce  ); }
            public void visitCrement                   (Java.Crement                    c   ) { res[0] = UnitCompiler.this.isType2(c   ); }
            public void visitInstanceof                (Java.Instanceof                 io  ) { res[0] = UnitCompiler.this.isType2(io  ); }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) { res[0] = UnitCompiler.this.isType2(mi  ); }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) { res[0] = UnitCompiler.this.isType2(smi ); }
            public void visitLiteral                   (Java.Literal                    l   ) { res[0] = UnitCompiler.this.isType2(l   ); }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) { res[0] = UnitCompiler.this.isType2(naci); }
            public void visitNewArray                  (Java.NewArray                   na  ) { res[0] = UnitCompiler.this.isType2(na  ); }
            public void visitNewInitializedArray       (Java.NewInitializedArray        nia ) { res[0] = UnitCompiler.this.isType2(nia ); }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) { res[0] = UnitCompiler.this.isType2(nci ); }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) { res[0] = UnitCompiler.this.isType2(pa  ); }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) { res[0] = UnitCompiler.this.isType2(qtr ); }
            public void visitThisReference             (Java.ThisReference              tr  ) { res[0] = UnitCompiler.this.isType2(tr  ); }
            // LvalueVisitor
            public void visitAmbiguousName                  (Java.AmbiguousName                   an   ) { try { res[0] = UnitCompiler.this.isType2(an   ); } catch (CompileException e) { throw new UCE(e); } }
            public void visitArrayAccessExpression          (Java.ArrayAccessExpression           aae  ) {       res[0] = UnitCompiler.this.isType2(aae  );                                                    }
            public void visitFieldAccess                    (Java.FieldAccess                     fa   ) {       res[0] = UnitCompiler.this.isType2(fa   );                                                    }
            public void visitFieldAccessExpression          (Java.FieldAccessExpression           fae  ) {       res[0] = UnitCompiler.this.isType2(fae  );                                                    }
            public void visitSuperclassFieldAccessExpression(Java.SuperclassFieldAccessExpression scfae) {       res[0] = UnitCompiler.this.isType2(scfae);                                                    }
            public void visitLocalVariableAccess            (Java.LocalVariableAccess             lva  ) {       res[0] = UnitCompiler.this.isType2(lva  );                                                    }
            public void visitParenthesizedExpression        (Java.ParenthesizedExpression         pe   ) {       res[0] = UnitCompiler.this.isType2(pe   );                                                    }
        };
        try {
            a.accept(av);
            return res[0];
        } catch (UCE uce) {
            throw uce.ce;
        }
    }
    private boolean isType2(Java.Atom a) {
        return a instanceof Java.Type;
    }
    private boolean isType2(Java.AmbiguousName an) throws CompileException {
        return this.isType(this.reclassify(an));
    }

    /**
     * Determine whether the given {@link IClass.IMember} is accessible in the given context,
     * according to JLS 6.6.1.4. Issues a {@link #compileError(String)} if not.
     */
    private boolean isAccessible(
        IClass.IMember      member,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        return this.isAccessible(member.getDeclaringIClass(), member.getAccess(), contextBlockStatement);
    }
    
    /**
     * Check whether the given {@link IClass.IMember} is accessible in the given context,
     * according to JLS 6.6.1.4. Issue a {@link #compileError(String)} if not.
     */
    private void checkAccessible(
        IClass.IMember      member,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        this.checkAccessible(member.getDeclaringIClass(), member.getAccess(), contextBlockStatement);
    }

    /**
     * Determine whether a member (class, interface, field or method) declared in a
     * given class is accessible from a given block statement context, according
     * to JLS2 6.6.1.4.
     */
    private boolean isAccessible(
        IClass              iClassDeclaringMember,
        Access              memberAccess,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        return null == this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextBlockStatement);
    }
    
    /**
     * Verify that a member (class, interface, field or method) declared in a
     * given class is accessible from a given block statement context, according
     * to JLS2 6.6.1.4. Issue a {@link #compileError(String)} if not.
     */
    private void checkAccessible(
        IClass              iClassDeclaringMember,
        Access              memberAccess,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        String message = this.internalCheckAccessible(iClassDeclaringMember, memberAccess, contextBlockStatement);
        if (message != null) this.compileError(message, contextBlockStatement.getLocation());
    }

    /**
     * @return a descriptive text iff a member declared in that {@link IClass} with that {@link Access} is inaccessible
     */
    private String internalCheckAccessible(
        IClass              iClassDeclaringMember,
        Access              memberAccess,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        
        // At this point, memberAccess is PUBLIC, DEFAULT, PROECTEDED or PRIVATE.

        // PUBLIC members are always accessible.
        if (memberAccess == Access.PUBLIC) return null;

        // At this point, the member is DEFAULT, PROECTEDED or PRIVATE accessible.

        // Determine the class declaring the context block statement.
        IClass iClassDeclaringContextBlockStatement;
        for (Java.Scope s = contextBlockStatement.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof Java.TypeDeclaration) {
                iClassDeclaringContextBlockStatement = this.resolve((Java.TypeDeclaration) s);
                break;
            }
        }

        // Access is always allowed for block statements declared in the same class as the member.
        if (iClassDeclaringContextBlockStatement == iClassDeclaringMember) return null;

        // Check whether the member and the context block statement are enclosed by the same
        // top-level type.
        {
            IClass topLevelIClassEnclosingMember = iClassDeclaringMember;
                for (IClass c = iClassDeclaringMember.getDeclaringIClass(); c != null; c = c.getDeclaringIClass()) {
                topLevelIClassEnclosingMember = c;
            }
            IClass topLevelIClassEnclosingContextBlockStatement = iClassDeclaringContextBlockStatement;
            for (IClass c = iClassDeclaringContextBlockStatement.getDeclaringIClass(); c != null; c = c.getDeclaringIClass()) {
                topLevelIClassEnclosingContextBlockStatement = c;
            }

            if (topLevelIClassEnclosingMember == topLevelIClassEnclosingContextBlockStatement) return null;
        }

        if (memberAccess == Access.PRIVATE) {
            return "Private member cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\".";
        }

        // At this point, the member is DEFAULT or PROTECTED accessible.

        // Check whether the member and the context block statement are declared in the same
        // package.
        if (Descriptor.areInSamePackage(
            iClassDeclaringMember.getDescriptor(),
            iClassDeclaringContextBlockStatement.getDescriptor()
        )) return null;

        if (memberAccess == Access.DEFAULT) {
            return "Member with \"" + memberAccess + "\" access cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\".";
        }

        // At this point, the member is PROTECTED accessible.

        // Check whether the class declaring the context block statement is a subclass of the
        // class declaring the member or a nested class whose parent is a subclass
        IClass parentClass = iClassDeclaringContextBlockStatement;
        do {
            if (iClassDeclaringMember.isAssignableFrom(parentClass)) {
                return null;
            }
            parentClass = parentClass.getOuterIClass();
        } while(parentClass != null);

        return "Protected member cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\", which is neither declared in the same package as nor is a subclass of \"" + iClassDeclaringMember + "\".";
    }

    /**
     * Determine whether the given {@link IClass} is accessible in the given context,
     * according to JLS2 6.6.1.2 and 6.6.1.4.
     */
    private boolean isAccessible(
        IClass              type,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        return null == this.internalCheckAccessible(type, contextBlockStatement);
    }
    
    /**
     * Check whether the given {@link IClass} is accessible in the given context,
     * according to JLS2 6.6.1.2 and 6.6.1.4. Issues a {@link #compileError(String)} if not.
     */
    private void checkAccessible(
        IClass              type,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {
        String message = this.internalCheckAccessible(type, contextBlockStatement);
        if (message != null) this.compileError(message, contextBlockStatement.getLocation());
    }

    private String internalCheckAccessible(
        IClass              type,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {

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
                for (Java.Scope s = contextBlockStatement.getEnclosingScope();; s = s.getEnclosingScope()) {
                    if (s instanceof Java.TypeDeclaration) {
                        iClassDeclaringContextBlockStatement = this.resolve((Java.TypeDeclaration) s);
                        break;
                    }
                }
    
                // Check whether the type is accessed from within the same package.
                String packageDeclaringType = Descriptor.getPackageName(type.getDescriptor());
                String contextPackage = Descriptor.getPackageName(iClassDeclaringContextBlockStatement.getDescriptor());
                if (!(packageDeclaringType == null ? contextPackage == null : packageDeclaringType.equals(contextPackage))) return "\"" + type + "\" is inaccessible from this package";
                return null;
            } else
            {
                throw new RuntimeException("\"" + type + "\" has unexpected access \"" + type.getAccess() + "\"");
            }
        }

        // "type" is a member type at this point.

        return this.internalCheckAccessible(iClassDeclaringType, type.getAccess(), contextBlockStatement);
    }

    private final Java.Type toTypeOrCE(Java.Atom a) throws CompileException {
        Java.Type result = a.toType();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not a type", a.getLocation());
            return new Java.SimpleType(a.getLocation(), this.iClassLoader.OBJECT);
        }
        return result;
    }
    private final Java.Rvalue toRvalueOrCE(final Java.Atom a) throws CompileException {
        Java.Rvalue result = a.toRvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an rvalue", a.getLocation());
            return new Java.Literal(a.getLocation(), "X");
        }
        return result;
    }
    public final Java.Lvalue toLvalueOrCE(final Java.Atom a) throws CompileException {
        Java.Lvalue result = a.toLvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an lvalue", a.getLocation());
            return new Java.Lvalue(a.getLocation()) {
                public String toString() { return a.toString(); }
                public void accept(Visitor.AtomVisitor visitor) {}
                public void accept(Visitor.RvalueVisitor visitor) {}
                public void accept(Visitor.LvalueVisitor visitor) {}
            };
        }
        return result;
    }

    /**
     * Copies the values of the synthetic parameters of this constructor ("this$..." and
     * "val$...") to the synthetic fields of the object ("this$..." and "val$...").
     */
    void assignSyntheticParametersToSyntheticFields(Java.ConstructorDeclarator cd) throws CompileException {
        for (Iterator it = cd.getDeclaringClass().syntheticFields.values().iterator(); it.hasNext();) {
            IClass.IField sf = (IClass.IField) it.next();
            Java.LocalVariable syntheticParameter = (Java.LocalVariable) cd.syntheticParameters.get(sf.getName());
            if (syntheticParameter == null) throw new RuntimeException("SNO: Synthetic parameter for synthetic field \"" + sf.getName() + "\" not found");
            try {
                Java.ExpressionStatement es = new Java.ExpressionStatement(new Java.Assignment(
                    cd.getLocation(),             // location
                    new Java.FieldAccess(         // lhs
                        cd.getLocation(),                         // location
                        new Java.ThisReference(cd.getLocation()), // lhs
                        sf                                        // field
                    ),
                    "=",                          // operator
                    new Java.LocalVariableAccess( // rhs
                        cd.getLocation(),  // location
                        syntheticParameter // localVariable
                    )
                ));
                es.setEnclosingScope(cd);
                this.compile(es);
            } catch (Parser.ParseException e) {
                throw new RuntimeException("S.N.O.");
            }
        }
    }

    /**
     * Compiles the instance variable initializers and the instance initializers in their
     * lexical order.
     */
    void initializeInstanceVariablesAndInvokeInstanceInitializers(Java.ConstructorDeclarator cd) throws CompileException {
        for (int i = 0; i < cd.getDeclaringClass().variableDeclaratorsAndInitializers.size(); ++i) {
            Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) cd.getDeclaringClass().variableDeclaratorsAndInitializers.get(i);
            if (!tbd.isStatic()) {
                Java.BlockStatement bs = (Java.BlockStatement) tbd;
                if (!this.compile(bs)) this.compileError("Instance variable declarator or instance initializer does not complete normally", bs.getLocation());
            }
        }
    }

    /**
     * Statements that jump out of blocks ("return", "break", "continue")
     * must call this method to make sure that the "finally" clauses of all
     * "try...catch" statements are executed.
     */
    private void leaveStatements(
        Java.Scope  from,
        Java.Scope  to,
        IClass optionalStackValueType
    ) {
        for (Java.Scope s = from; s != to; s = s.getEnclosingScope()) {
            if (s instanceof Java.BlockStatement) {
                this.leave((Java.BlockStatement) s, optionalStackValueType);
            }
        }
    }

    /**
     * The LHS operand of type <code>lhsType</code> is expected on the stack.
     * <p>
     * The following operators are supported:
     * <code>&nbsp;&nbsp;| ^ & * / % + - &lt;&lt; &gt;&gt; &gt;&gt;&gt;</code>
     */
    private IClass compileArithmeticBinaryOperation(
        Locatable   l,
        IClass      lhsType,
        String      operator,
        Java.Rvalue rhs
    ) throws CompileException {
        return this.compileArithmeticOperation(
            l,
            lhsType,
            Arrays.asList(new Java.Rvalue[] { rhs }).iterator(),
            operator
        );
    }

    /**
     * Execute an arithmetic operation on a sequence of <code>operands</code>. If
     * <code>type</code> is non-null, the first operand with that type is already on the stack.
     * <p>
     * The following operators are supported:
     * <code>&nbsp;&nbsp;| ^ &amp; * / % + - &lt;&lt; &gt;&gt; &gt;&gt;&gt;</code>
     */
    private IClass compileArithmeticOperation(
        final Locatable l,
        IClass          type,
        Iterator        operands,
        String          operator
    ) throws CompileException {
        if (
            operator == "|" ||
            operator == "^" ||
            operator == "&"
        ) {
            final int iopcode = (
                operator == "&" ? Opcode.IAND :
                operator == "|" ? Opcode.IOR  :
                operator == "^" ? Opcode.IXOR : Integer.MAX_VALUE
            );

            do {
                Java.Rvalue operand = (Java.Rvalue) operands.next();

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass rhsType = this.compileGetValue(operand);

                    if (
                        type.isPrimitiveNumeric() &&
                        rhsType.isPrimitiveNumeric()
                    ) {
                        IClass promotedType = this.binaryNumericPromotion(l, type, convertLhsInserter, rhsType);
                        if (promotedType == IClass.INT) {
                            this.writeOpcode(l, iopcode);
                        } else
                        if (promotedType == IClass.LONG) {
                            this.writeOpcode(l, iopcode + 1);
                        } else
                        {
                            this.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"", l.getLocation());
                        }
                        type = promotedType;
                    } else
                    if (
                        (type == IClass.BOOLEAN && this.getUnboxedType(rhsType) == IClass.BOOLEAN) ||
                        (this.getUnboxedType(type) == IClass.BOOLEAN && rhsType == IClass.BOOLEAN)
                    ) {
                        IClassLoader icl = this.iClassLoader;
                        if (type == icl.BOOLEAN) {
                            this.codeContext.pushInserter(convertLhsInserter);
                            try {
                                this.unboxingConversion(l, icl.BOOLEAN, IClass.BOOLEAN);
                            } finally {
                                this.codeContext.popInserter();
                            }
                        }
                        if (rhsType == icl.BOOLEAN) {
                            this.unboxingConversion(l, icl.BOOLEAN, IClass.BOOLEAN);
                        }
                        this.writeOpcode(l, iopcode);
                        type = IClass.BOOLEAN;
                    } else
                    {
                        this.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"", l.getLocation());
                        type = IClass.INT;
                    }
                }
            } while (operands.hasNext());
            return type;
        }

        if (
            operator == "*"   ||
            operator == "/"   ||
            operator == "%"   ||
            operator == "+"   ||
            operator == "-"
        ) {
            final int iopcode = (
                operator == "*"   ? Opcode.IMUL  :
                operator == "/"   ? Opcode.IDIV  :
                operator == "%"   ? Opcode.IREM  :
                operator == "+"   ? Opcode.IADD  :
                operator == "-"   ? Opcode.ISUB  : Integer.MAX_VALUE
            );

            do {
                Java.Rvalue operand = (Java.Rvalue) operands.next();

                IClass operandType = this.getType(operand);
                IClassLoader icl = this.iClassLoader;

                // String concatenation?
                if (operator == "+" && (type == icl.STRING || operandType == icl.STRING)) {
                    return this.compileStringConcatenation(l, type, operand, operands);
                }

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass rhsType = this.compileGetValue(operand);

                    type = this.binaryNumericPromotion(l, type, convertLhsInserter, rhsType);

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
                        this.compileError("Unexpected promoted type \"" + type + "\"", l.getLocation());
                        opcode = iopcode;
                    }
                    this.writeOpcode(l, opcode);
                }
            } while (operands.hasNext());
            return type;
        }

        if (
            operator == "<<"  ||
            operator == ">>"  ||
            operator == ">>>"
        ) {
            final int iopcode = (
                operator == "<<"  ? Opcode.ISHL  :
                operator == ">>"  ? Opcode.ISHR  :
                operator == ">>>" ? Opcode.IUSHR : Integer.MAX_VALUE
            );

            do {
                Java.Rvalue operand = (Java.Rvalue) operands.next();

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass rhsType = this.compileGetValue(operand);

                    IClass promotedLhsType;
                    this.codeContext.pushInserter(convertLhsInserter);
                    try {
                        promotedLhsType = this.unaryNumericPromotion(l, type);
                    } finally {
                        this.codeContext.popInserter();
                    }
                    if (promotedLhsType != IClass.INT && promotedLhsType != IClass.LONG) this.compileError("Shift operation not allowed on operand type \"" + type + "\"", l.getLocation());

                    IClass promotedRhsType = this.unaryNumericPromotion(l, rhsType);
                    if (promotedRhsType != IClass.INT && promotedRhsType != IClass.LONG) this.compileError("Shift distance of type \"" + rhsType + "\" is not allowed", l.getLocation());

                    if (promotedRhsType == IClass.LONG) this.writeOpcode(l, Opcode.L2I);

                    this.writeOpcode(l, promotedLhsType == IClass.LONG ? iopcode + 1 : iopcode);
                    type = promotedLhsType;
                }
            } while (operands.hasNext());
            return type;
        }

        throw new RuntimeException("Unexpected operator \"" + operator + "\"");
    }

    /**
     * @param type If non-null, the first operand with that type is already on the stack
     * @param operand The next operand
     * @param operands All following operands ({@link Iterator} over {@link Java.Rvalue}s)
     */
    private IClass compileStringConcatenation(
        final Locatable l,
        IClass          type,
        Java.Rvalue     operand,
        Iterator        operands
    ) throws CompileException {
        boolean operandOnStack;
        if (type != null) {
            this.stringConversion(l, type);
            operandOnStack = true;
        } else
        {
            operandOnStack = false;
        }

        // Compute list of operands and merge consecutive constant operands.
        List tmp = new ArrayList(); // Compilable
        do {
            Object cv = this.getConstantValue(operand);
            if (cv == null) {
                // Non-constant operand.
                final Java.Rvalue final_operand = operand;
                tmp.add(new Compilable() {
                    public void compile() throws CompileException {
                        UnitCompiler.this.stringConversion(l, UnitCompiler.this.compileGetValue(final_operand));
                    }
                });

                operand = operands.hasNext() ? (Java.Rvalue) operands.next() : null;
            } else
            {
                // Constant operand. Check to see whether the next operand is also constant.
                if (operands.hasNext()) {
                    operand = (Java.Rvalue) operands.next();
                    Object cv2 = this.getConstantValue(operand);
                    if (cv2 != null) {
                        StringBuffer sb = new StringBuffer(cv.toString()).append(cv2);
                        for (;;) {
                            if (!operands.hasNext()) {
                                operand = null;
                                break;
                            }
                            operand = (Java.Rvalue) operands.next();
                            Object cv3 = this.getConstantValue(operand);
                            if (cv3 == null) break;
                            sb.append(cv3);
                        }
                        cv = sb.toString();
                    }
                } else
                {
                    operand = null;
                }
                // Break long string constants up into UTF8-able chunks.
                final String[] ss = UnitCompiler.makeUTF8Able(cv.toString());
                for (int i = 0; i < ss.length; ++i) {
                    final String s = ss[i];
                    tmp.add(new Compilable() {
                        public void compile() throws CompileException {
                            UnitCompiler.this.pushConstant(l, s);
                        }
                    });
                }
            }
        } while (operand != null);

        // At this point "tmp" contains an optimized sequence of Strings (representing constant
        // portions) and Rvalues (non-constant portions).

        if (tmp.size() <= (operandOnStack ? STRING_CONCAT_LIMIT - 1: STRING_CONCAT_LIMIT)) {

            // String concatenation through "a.concat(b).concat(c)".
            for (Iterator it = tmp.iterator(); it.hasNext();) {
                Compilable c = (Compilable) it.next();
                c.compile();
                
                // Concatenate.
                if (operandOnStack) {
                    this.writeOpcode(l, Opcode.INVOKEVIRTUAL);
                    this.writeConstantMethodrefInfo(
                        Descriptor.STRING,
                        "concat",                                // classFD
                        "(" + Descriptor.STRING + ")" + Descriptor.STRING // methodMD
                    );
                } else
                {
                    operandOnStack = true;
                }
            }
            return this.iClassLoader.STRING;
        }

        // String concatenation through "new StringBuffer(a).append(b).append(c).append(d).toString()".
        Iterator it = tmp.iterator();

        String stringBuilferFD = this.isStringBuilderAvailable ? Descriptor.STRING_BUILDER : Descriptor.STRING_BUFFER;
        // "new StringBuffer(a)":
        if (operandOnStack) {
            this.writeOpcode(l, Opcode.NEW);
            this.writeConstantClassInfo(stringBuilferFD);
            this.writeOpcode(l, Opcode.DUP_X1);
            this.writeOpcode(l, Opcode.SWAP);
        } else
        {
            this.writeOpcode(l, Opcode.NEW);
            this.writeConstantClassInfo(stringBuilferFD);
            this.writeOpcode(l, Opcode.DUP);
            ((Compilable) it.next()).compile();
        }
        this.writeOpcode(l, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            stringBuilferFD,
            "<init>",                                // classFD
            "(" + Descriptor.STRING + ")" + Descriptor.VOID_ // methodMD
        );
        while (it.hasNext()) {
            ((Compilable) it.next()).compile();
            
            // "StringBuffer.append(b)":
            this.writeOpcode(l, Opcode.INVOKEVIRTUAL);
            this.writeConstantMethodrefInfo(
                stringBuilferFD,
                "append",                                // classFD
                "(" + Descriptor.STRING + ")" + stringBuilferFD // methodMD
            );
        }

        // "StringBuffer.toString()":
        this.writeOpcode(l, Opcode.INVOKEVIRTUAL);
        this.writeConstantMethodrefInfo(
            stringBuilferFD,
            "toString",           // classFD
            "()" + Descriptor.STRING   // methodMD
        );
        return this.iClassLoader.STRING;
    }
    interface Compilable { void compile() throws CompileException; }

    /**
     * Convert object of type "sourceType" to type "String". JLS2 15.18.1.1
     */
    private void stringConversion(
        Locatable l,
        IClass    sourceType
    ) {
        this.writeOpcode(l, Opcode.INVOKESTATIC);
        this.writeConstantMethodrefInfo(
            Descriptor.STRING,
            "valueOf", // classFD
            "(" + (            // methodMD
                sourceType == IClass.BOOLEAN ||
                sourceType == IClass.CHAR    ||
                sourceType == IClass.LONG    ||
                sourceType == IClass.FLOAT   ||
                sourceType == IClass.DOUBLE ? sourceType.getDescriptor() :
                sourceType == IClass.BYTE  ||
                sourceType == IClass.SHORT ||
                sourceType == IClass.INT ? Descriptor.INT_ :
                Descriptor.OBJECT
            ) + ")" + Descriptor.STRING
        );
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     * Notice: This method is used both for explicit constructor invocation (first statement of
     * a constructor body) and implicit constructor invocation (right after NEW).
     *
     * @param optionalEnclosingInstance Used if the target class is an inner class
     */
    private void invokeConstructor(
        Locatable     l,
        Java.Scope    scope,
        Java.Rvalue   optionalEnclosingInstance,
        IClass        targetClass,
        Java.Rvalue[] arguments
    ) throws CompileException {

        // Find constructors.
        IClass.IConstructor[] iConstructors = targetClass.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new RuntimeException("SNO: Target class \"" + targetClass.getDescriptor() + "\" has no constructors");

        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            l,
            iConstructors, // iInvocables
            arguments      // arguments
        );

        // Check exceptions that the constructor may throw.
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        for (int i = 0; i < thrownExceptions.length; ++i) {
            this.checkThrownException(
                l,
                thrownExceptions[i],
                scope
            );
        }

        // Pass enclosing instance as a synthetic parameter.
        if (optionalEnclosingInstance != null) {
            IClass outerIClass = targetClass.getOuterIClass();
            if (outerIClass != null) {
                IClass eiic = this.compileGetValue(optionalEnclosingInstance);
                if (!outerIClass.isAssignableFrom(eiic)) this.compileError("Type of enclosing instance (\"" + eiic + "\") is not assignable to \"" + outerIClass + "\"", l.getLocation());
            }
        }

        // Pass local variables to constructor as synthetic parameters.
        {
            IClass.IField[] syntheticFields = targetClass.getSyntheticIFields();

            // Determine enclosing function declarator and type declaration.
            Java.TypeBodyDeclaration scopeTBD;
            Java.TypeDeclaration     scopeTypeDeclaration;
            {
                Java.Scope s = scope;
                for (; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
                scopeTBD             = (Java.TypeBodyDeclaration) s;
                scopeTypeDeclaration = scopeTBD.getDeclaringType();
            }

            if (!(scopeTypeDeclaration instanceof Java.ClassDeclaration)) {
                if (syntheticFields.length > 0) throw new RuntimeException("SNO: Target class has synthetic fields");
            } else {
                Java.ClassDeclaration scopeClassDeclaration = (Java.ClassDeclaration) scopeTypeDeclaration;
                for (int i = 0; i < syntheticFields.length; ++i) {
                    IClass.IField sf = syntheticFields[i];
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTBD instanceof Java.MethodDeclarator) {
                            this.load(l, this.resolve(scopeClassDeclaration), 0);
                            this.writeOpcode(l, Opcode.GETFIELD);
                            this.writeConstantFieldrefInfo(
                                this.resolve(scopeClassDeclaration).getDescriptor(),
                                sf.getName(), // classFD
                                sf.getDescriptor()                                   // fieldFD
                            );
                        } else
                        if (scopeTBD instanceof Java.ConstructorDeclarator) {
                            Java.ConstructorDeclarator constructorDeclarator = (Java.ConstructorDeclarator) scopeTBD;
                            Java.LocalVariable syntheticParameter = (Java.LocalVariable) constructorDeclarator.syntheticParameters.get(sf.getName());
                            if (syntheticParameter == null) {
                                this.compileError("Compiler limitation: Constructor cannot access local variable \"" + sf.getName().substring(4) + "\" declared in an enclosing block because none of the methods accesses it. As a workaround, declare a dummy method that accesses the local variable.", l.getLocation());
                                this.writeOpcode(l, Opcode.ACONST_NULL);
                            } else {
                                this.load(l, syntheticParameter);
                            }
                        } else {
                            this.compileError("Compiler limitation: Initializers cannot access local variables declared in an enclosing block.", l.getLocation());
                            this.writeOpcode(l, Opcode.ACONST_NULL);
                        }
                    } else {
                        String localVariableName = sf.getName().substring(4);
                        Java.LocalVariable lv;
                        DETERMINE_LV: {
                            Java.Scope s;

                            // Does one of the enclosing blocks declare a local variable with that name?
                            for (s = scope; s instanceof Java.BlockStatement; s = s.getEnclosingScope()) {
                                Java.BlockStatement bs = (Java.BlockStatement) s;
                                Java.Scope es = bs.getEnclosingScope();
                                if (!(es instanceof Java.Block)) continue;
                                Java.Block b = (Java.Block) es;
    
                                for (Iterator it = b.statements.iterator();;) {
                                    Java.BlockStatement bs2 = (Java.BlockStatement) it.next();
                                    if (bs2 == bs) break;
                                    if (bs2 instanceof Java.LocalVariableDeclarationStatement) {
                                        Java.LocalVariableDeclarationStatement lvds = ((Java.LocalVariableDeclarationStatement) bs2);
                                        Java.VariableDeclarator[] vds = lvds.variableDeclarators;
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
                            while (!(s instanceof Java.FunctionDeclarator)) s = s.getEnclosingScope();
                            Java.FunctionDeclarator fd = (Java.FunctionDeclarator) s;
                            for (int j = 0; j < fd.formalParameters.length; ++j) {
                                Java.FunctionDeclarator.FormalParameter fp = fd.formalParameters[j];
                                if (fp.name.equals(localVariableName)) {
                                    lv = this.getLocalVariable(fp);
                                    break DETERMINE_LV;
                                }
                            }
                            throw new RuntimeException("SNO: Synthetic field \"" + sf.getName() + "\" neither maps a synthetic field of an enclosing instance nor a local variable");
                        }
                        this.load(l, lv);
                    }
                }
            }
        }

        // Evaluate constructor arguments.
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        for (int i = 0; i < arguments.length; ++i) {
            this.assignmentConversion(
                l,                                  // l
                this.compileGetValue(arguments[i]), // sourceType
                parameterTypes[i],                  // targetType
                this.getConstantValue(arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()" prepended with the
        // synthetic parameters.
        this.writeOpcode(l, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            targetClass.getDescriptor(),
            "<init>", // classFD
            iConstructor.getDescriptor() // methodMD
        );
    }

    /*package*/ IClass.IField[] getIFields(final Java.FieldDeclaration fd) {
        IClass.IField[] res = new IClass.IField[fd.variableDeclarators.length];
        for (int i = 0; i < res.length; ++i) {
            final Java.VariableDeclarator vd = fd.variableDeclarators[i];
            res[i] = this.resolve(fd.getDeclaringType()).new IField() {

                // Implement IMember.
                public Access getAccess() {
                    switch (fd.modifiers & Mod.PPP) {
                    case Mod.PRIVATE:
                        return Access.PRIVATE;
                    case Mod.PROTECTED:
                        return Access.PROTECTED;
                    case Mod.PACKAGE:
                        return Access.DEFAULT;
                    case Mod.PUBLIC:
                        return Access.PUBLIC;
                    default:
                        throw new RuntimeException("Invalid access");
                    }
                }

                // Implement "IField".
                public boolean isStatic() { return (fd.modifiers & Mod.STATIC) != 0; }
                public IClass getType() throws CompileException {
                    return UnitCompiler.this.getType(fd.type).getArrayIClass(vd.brackets, UnitCompiler.this.iClassLoader.OBJECT);
                }
                public String getName() { return vd.name; }
                public Object getConstantValue() throws CompileException {
                    if (
                        (fd.modifiers & Mod.FINAL) != 0 &&
                        vd.optionalInitializer instanceof Java.Rvalue
                    ) {
                        Object constantInitializerValue = UnitCompiler.this.getConstantValue((Java.Rvalue) vd.optionalInitializer);
                        if (constantInitializerValue != null) return UnitCompiler.this.assignmentConversion(
                            (Locatable) vd.optionalInitializer, // l
                            constantInitializerValue,           // value
                            this.getType()                      // targetType
                        );
                    }
                    return null;
                }
            };
        }
        return res;
    }

    /**
     * Determine the non-constant-final initializer of the given {@link Java.VariableDeclarator}.
     * @return <code>null</code> if the variable is declared without an initializer or if the initializer is constant-final
     */
    Java.ArrayInitializerOrRvalue getNonConstantFinalInitializer(Java.FieldDeclaration fd, Java.VariableDeclarator vd) throws CompileException {

        // Check if optional initializer exists.
        if (vd.optionalInitializer == null) return null;

        // Check if initializer is constant-final.
        if (
            (fd.modifiers & Mod.STATIC) != 0 &&
            (fd.modifiers & Mod.FINAL) != 0 &&
            vd.optionalInitializer instanceof Java.Rvalue &&
            this.getConstantValue((Java.Rvalue) vd.optionalInitializer) != null
        ) return null;

        return vd.optionalInitializer;
    }

    private Java.Atom reclassify(Java.AmbiguousName an) throws CompileException {
        if (an.reclassified == null) {
            an.reclassified = this.reclassifyName(
                an.getLocation(),
                (Java.Scope) an.getEnclosingBlockStatement(),
                an.identifiers, an.n
            );
        }
        return an.reclassified;
    }

    /**
     * JLS 6.5.2.2
     * <p>
     * Reclassify the ambiguous name consisting of the first <code>n</code> of the
     * <code>identifers</code>.
     * @param location
     * @param scope
     * @param identifiers
     * @param n
     * @throws CompileException
     */
    private Java.Atom reclassifyName(
        Location       location,
        Java.Scope     scope,
        final String[] identifiers,
        int            n
    ) throws CompileException {

        if (n == 1) return this.reclassifyName(
            location,
            scope,
            identifiers[0]
        );

        // 6.5.2.2
        Java.Atom lhs = this.reclassifyName(
            location,
            scope,
            identifiers, n - 1
        );
        String rhs = identifiers[n - 1];

        // 6.5.2.2.1
        if (UnitCompiler.DEBUG) System.out.println("lhs = " + lhs);
        if (lhs instanceof Java.Package) {
            String className = ((Java.Package) lhs).name + '.' + rhs;
            IClass result = findClassByName(location, className);
            if (result != null) return new Java.SimpleType(location, result);

            return new Java.Package(location, className);
        }

        // 6.5.2.2.3.2 EXPRESSION.length
        if (rhs.equals("length") && this.getType(lhs).isArray()) {
            Java.ArrayLength al = new Java.ArrayLength(location, this.toRvalueOrCE(lhs));
            if (!(scope instanceof Java.BlockStatement)) {
                this.compileError("\".length\" only allowed in expression context");
                return al;
            }
            al.setEnclosingBlockStatement((Java.BlockStatement) scope);
            return al;
        }

        IClass lhsType = this.getType(lhs);

        // Notice: Don't need to check for 6.5.2.2.2.1 TYPE.METHOD and 6.5.2.2.3.1
        // EXPRESSION.METHOD here because that has been done before.

        {
            IClass.IField field = this.findIField(lhsType, rhs, location);
            if (field != null) {
                // 6.5.2.2.2.2 TYPE.FIELD
                // 6.5.2.2.3.2 EXPRESSION.FIELD
                Java.FieldAccess fa = new Java.FieldAccess(
                    location,
                    lhs,
                    field
                );
                fa.setEnclosingBlockStatement((Java.BlockStatement) scope);
                return fa;
            }
        }

        IClass[] classes = lhsType.getDeclaredIClasses();
        for (int i = 0; i < classes.length; ++i) {
            final IClass memberType = classes[i];
            String name = Descriptor.toClassName(memberType.getDescriptor());
            name = name.substring(name.lastIndexOf('$') + 1);
            if (name.equals(rhs)) {

                // 6.5.2.2.2.3 TYPE.TYPE
                // 6.5.2.2.3.3 EXPRESSION.TYPE
                return new Java.SimpleType(location, memberType);
            }
        }

        this.compileError("\"" + rhs + "\" is neither a method, a field, nor a member class of \"" + lhsType + "\"", location);
        return new Java.Atom(location) {
            public String toString() { return Java.join(identifiers, "."); }
            public final void accept(Visitor.AtomVisitor visitor) {}
        };
    }

    private IClass findClassByName(Location location, String className) throws CompileException {
        IClass res = this.findClass(className);
        if(res != null) return res;
        try {
            return this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
        } catch (ClassNotFoundException ex) {
            if (ex.getException() instanceof CompileException) throw (CompileException) ex.getException();
            throw new CompileException(className, location, ex);
        }
    }

    /**
     * JLS 6.5.2.1
     * @param location
     * @param scope
     * @param identifier
     * @throws CompileException
     */
    private Java.Atom reclassifyName(
        Location     location,
        Java.Scope   scope,
        final String identifier
    ) throws CompileException {

        // Determine scope block statement, type body declaration, type and compilation unit.
        Java.BlockStatement          scopeBlockStatement = null;
        Java.TypeBodyDeclaration     scopeTBD = null;
        Java.AbstractTypeDeclaration scopeTypeDeclaration = null;
        Java.CompilationUnit         scopeCompilationUnit;
        {
            Java.Scope s = scope;
            if (s instanceof Java.BlockStatement) scopeBlockStatement = (Java.BlockStatement) s;
            while (
                (s instanceof Java.BlockStatement || s instanceof Java.CatchClause)
                && !(s instanceof Java.TypeBodyDeclaration)
            ) s = s.getEnclosingScope();
            if (s instanceof Java.TypeBodyDeclaration) {
                scopeTBD = (Java.TypeBodyDeclaration) s;
                s = s.getEnclosingScope();
            }
            if (s instanceof Java.TypeDeclaration) {
                scopeTypeDeclaration = (Java.AbstractTypeDeclaration) s;
                s = s.getEnclosingScope();
            }
            while (!(s instanceof Java.CompilationUnit)) s = s.getEnclosingScope();
            scopeCompilationUnit = (Java.CompilationUnit) s;
        }

        // 6.5.2.1.BL1

        // 6.5.2.BL1.B1.B1.1 (JLS3: 6.5.2.BL1.B1.B1.1) / 6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2 (JLS3: 6.5.2.BL1.B1.B1.2) / 6.5.6.1.1 Parameter.
        {
            Java.Scope s = scope;
            if (s instanceof Java.BlockStatement) {
                Java.LocalVariable lv = ((Java.BlockStatement) s).findLocalVariable(identifier);
                if (lv != null) {
                    Java.LocalVariableAccess lva = new Java.LocalVariableAccess(location, lv);
                    if (!(scope instanceof Java.BlockStatement)) throw new RuntimeException("SNO: Local variable access in non-block statement context!?");
                    lva.setEnclosingBlockStatement((Java.BlockStatement) scope);
                    return lva;
                }
                s = s.getEnclosingScope();
            }
            while (s instanceof Java.BlockStatement || s instanceof Java.CatchClause) s = s.getEnclosingScope();
            if (s instanceof Java.FunctionDeclarator) {
                s = s.getEnclosingScope();
                if (s instanceof Java.InnerClassDeclaration) {
                    Java.InnerClassDeclaration icd = (Java.InnerClassDeclaration) s;
                    s = s.getEnclosingScope();
                    if (s instanceof Java.AnonymousClassDeclaration) s = s.getEnclosingScope();
                    while (s instanceof Java.BlockStatement) {
                        Java.LocalVariable lv = ((Java.BlockStatement) s).findLocalVariable(identifier);
                        if (lv != null) {
                            if (!lv.finaL) this.compileError("Cannot access non-final local variable \"" + identifier + "\" from inner class");
                            final IClass lvType = lv.type;
                            IClass.IField iField = new SimpleIField(
                                this.resolve(icd),
                                "val$" + identifier,
                                lvType
                            );
                            icd.defineSyntheticField(iField);
                            Java.FieldAccess fa = new Java.FieldAccess(
                                location,                        // location
                                new Java.QualifiedThisReference( // lhs
                                    location,                                        // location
                                    new Java.SimpleType(location, this.resolve(icd)) // qualification
                                ),
                                iField                           // field
                            );
                            fa.setEnclosingBlockStatement((Java.BlockStatement) scope);
                            return fa;
                        }
                        s = s.getEnclosingScope();
                        while (s instanceof Java.BlockStatement) s = s.getEnclosingScope();
                        if (!(s instanceof Java.FunctionDeclarator)) break;
                        s = s.getEnclosingScope();
                        if (!(s instanceof Java.InnerClassDeclaration)) break;
                        icd = (Java.InnerClassDeclaration) s;
                        s = s.getEnclosingScope();
                    }
                }
            }
        }

        // 6.5.2.BL1.B1.B1.3 (JLS3: 6.5.2.BL1.B1.B1.3) / 6.5.6.1.2.1 Field.
        Java.BlockStatement enclosingBlockStatement = null;
        for (Java.Scope s = scope; !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof Java.BlockStatement && enclosingBlockStatement == null) enclosingBlockStatement = (Java.BlockStatement) s;
            if (s instanceof Java.TypeDeclaration) {
                final Java.AbstractTypeDeclaration enclosingTypeDecl = (Java.AbstractTypeDeclaration)s;
                final IClass etd = UnitCompiler.this.resolve(enclosingTypeDecl);
                final IClass.IField f = this.findIField(etd, identifier, location);
                if (f != null) {
                    if (f.isStatic()) {
                        this.warning("IASF", "Implicit access to static field \"" + identifier + "\" of declaring class (better write \"" + f.getDeclaringIClass() + '.' + f.getName() + "\")", location);
                    } else
                    if (f.getDeclaringIClass() == etd) {
                        this.warning("IANSF", "Implicit access to non-static field \"" + identifier + "\" of declaring class (better write \"this." + f.getName() + "\")", location);
                    } else {
                        this.warning("IANSFEI", "Implicit access to non-static field \"" + identifier + "\" of enclosing instance (better write \"" + f.getDeclaringIClass() + ".this." + f.getName() + "\")", location);
                    }

                    Java.SimpleType ct = new Java.SimpleType(scopeTypeDeclaration.getLocation(), (IClass) etd);
                    Java.Atom lhs;
                    if (scopeTBD.isStatic()) {

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
                            lhs = new Java.QualifiedThisReference(location, ct);
                        }
                    }
                    Java.Rvalue res = new Java.FieldAccess(
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
            Object o = this.singleStaticImports.get(identifier);
            if (o instanceof IField) {
                FieldAccess fieldAccess = new FieldAccess(location, new SimpleType(location, ((IField) o).getDeclaringIClass()), (IField) o);
                fieldAccess.setEnclosingBlockStatement(enclosingBlockStatement);
                return fieldAccess;
            }
        }

        // JLS3 6.5.2.BL1.B1.B2.2 Static field imported through static-import-on-demand.
        {
            IField importedField = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass iClass = (IClass) it.next();
                IField[] flds = iClass.getDeclaredIFields();
                for (int i = 0 ; i < flds.length; ++i) {
                    IField f = flds[i];
                    if (f.getName().equals(identifier)) {
                        if (!UnitCompiler.this.isAccessible(f, enclosingBlockStatement)) continue; // JLS3 7.5.4 Static-Import-on-Demand Declaration
                        if (importedField != null) UnitCompiler.this.compileError("Ambiguous static field import: \"" + importedField.toString() + "\" vs. \"" + f.toString() + "\"");
                        importedField = f;
                    }
                }
            }
            if (importedField != null) {
                if (!importedField.isStatic()) UnitCompiler.this.compileError("Cannot static-import non-static field");
                FieldAccess fieldAccess = new FieldAccess(location, new SimpleType(location, importedField.getDeclaringIClass()), importedField);
                fieldAccess.setEnclosingBlockStatement(enclosingBlockStatement);
                return fieldAccess;
            }
        }

        // Hack: "java" MUST be a package, not a class.
        if (identifier.equals("java")) return new Java.Package(location, identifier);

        // 6.5.2.BL1.B1.B2.1 (JLS3: 6.5.2.BL1.B1.B3.2) Local class.
        {
            Java.LocalClassDeclaration lcd = this.findLocalClassDeclaration(scope, identifier);
            if (lcd != null) return new Java.SimpleType(location, this.resolve(lcd));
        }

        // 6.5.2.BL1.B1.B2.2 (JLS3: 6.5.2.BL1.B1.B3.3) Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = this.findMemberType(UnitCompiler.this.resolve(scopeTypeDeclaration), identifier, location);
            if (memberType != null) return new Java.SimpleType(location, memberType);
        }

        // 6.5.2.BL1.B1.B3.1 (JLS3: 6.5.2.BL1.B1.B4.1) Single type import.
        {
            IClass iClass = this.importSingleType(identifier, location);
            if (iClass != null) return new Java.SimpleType(location, iClass);
        }

        // 6.5.2.BL1.B1.B3.2 (JLS3: 6.5.2.BL1.B1.B3.1) Package member class/interface declared in this compilation unit.
        // Notice that JLS2 looks this up AFTER local class, member type, single type import, while
        // JLS3 looks this up BEFORE local class, member type, single type import.
        {
            Java.PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(identifier);
            if (pmtd != null) return new Java.SimpleType(location, this.resolve((Java.AbstractTypeDeclaration) pmtd));
        }

        // 6.5.2.BL1.B1.B4 Class or interface declared in same package.
        // Notice: Why is this missing in JLS3?
        {
            String className = (
                scopeCompilationUnit.optionalPackageDeclaration == null ?
                identifier :
                scopeCompilationUnit.optionalPackageDeclaration.packageName + '.' + identifier
            );
            IClass result = findClassByName(location, className);
            if (result != null) return new Java.SimpleType(location, result);
        }

        // 6.5.2.BL1.B1.B5 (JLS3: 6.5.2.BL1.B1.B4.2), 6.5.2.BL1.B1.B6 Type-import-on-demand.
        {
            IClass importedClass = this.importTypeOnDemand(identifier, location);
            if (importedClass != null) {
                return new Java.SimpleType(location, importedClass);
            }
        }

        // JLS3 6.5.2.BL1.B1.B4.3 Type imported through single static import.
        {
            Object o = this.singleStaticImports.get(identifier);
            if (o instanceof IClass) return new SimpleType(null, (IClass) o);
        }

        // JLS3 6.5.2.BL1.B1.B4.4 Type imported through static-import-on-demand.
        {
            IClass importedType = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass ic = (IClass) it.next();
                IClass[] memberTypes = ic.getDeclaredIClasses();
                for (int i = 0; i < memberTypes.length; ++i) {
                    IClass mt = memberTypes[i];
                    if (!UnitCompiler.this.isAccessible(mt, scopeBlockStatement)) continue;
                    if (mt.getDescriptor().endsWith('$' + identifier + ';')) {
                        if (importedType != null) UnitCompiler.this.compileError("Ambiguous static type import: \"" + importedType.toString() + "\" vs. \"" + mt.toString() + "\"");
                        importedType = mt;
                    }
                }
            }
            if (importedType != null) return new Java.SimpleType(null, importedType);
        }
        
        // 6.5.2.BL1.B1.B7 Package name
        return new Java.Package(location, identifier);
    }

    private void determineValue(Java.FieldAccessExpression fae) throws CompileException {
        if (fae.value != null) return;

        IClass lhsType = this.getType(fae.lhs);

        if (fae.fieldName.equals("length") && lhsType.isArray()) {
            fae.value = new Java.ArrayLength(
                fae.getLocation(),
                this.toRvalueOrCE(fae.lhs)
            );
        } else {
            IClass.IField iField = this.findIField(lhsType, fae.fieldName, fae.getLocation());
            if (iField == null) {
                this.compileError("\"" + this.getType(fae.lhs).toString() + "\" has no field \"" + fae.fieldName + "\"", fae.getLocation());
                fae.value = new Java.Rvalue(fae.getLocation()) {
//                    public IClass compileGet() throws CompileException { return this.iClassLoader.OBJECT; }
                    public String toString() { return "???"; }
                    public final void accept(Visitor.AtomVisitor visitor) {}
                    public final void accept(Visitor.RvalueVisitor visitor) {}
                };
                return;
            }

            fae.value = new Java.FieldAccess(
                fae.getLocation(),
                fae.lhs,
                iField
            );
        }
        fae.value.setEnclosingBlockStatement(fae.getEnclosingBlockStatement());
    }

    /** "super.fld", "Type.super.fld" */
    private void determineValue(Java.SuperclassFieldAccessExpression scfae) throws CompileException {
        if (scfae.value != null) return;

        Rvalue lhs;
        {
            Java.ThisReference tr = new Java.ThisReference(scfae.getLocation());
            tr.setEnclosingBlockStatement(scfae.getEnclosingBlockStatement());
            IClass type;
            if (scfae.optionalQualification != null) {
                type = UnitCompiler.this.getType(scfae.optionalQualification);
            } else
            {
                type = this.getType(tr);
            }
            lhs = new Java.Cast(scfae.getLocation(), new SimpleType(scfae.getLocation(), type.getSuperclass()), tr);
        }

        IClass.IField iField = this.findIField(this.getType(lhs), scfae.fieldName, scfae.getLocation());
        if (iField == null) {
            this.compileError("Class has no field \"" + scfae.fieldName + "\"", scfae.getLocation());
            scfae.value = new Java.Rvalue(scfae.getLocation()) {
                public String toString() { return "???"; }
                public final void accept(Visitor.AtomVisitor visitor) {}
                public final void accept(Visitor.RvalueVisitor visitor) {}
            };
            return;
        }
        scfae.value = new Java.FieldAccess(
            scfae.getLocation(),
            lhs,
            iField
        );
        scfae.value.setEnclosingBlockStatement(scfae.getEnclosingBlockStatement());
    }
    
    /**
     * Find named methods of "targetType", examine the argument types and choose the
     * most specific method. Check that only the allowed exceptions are thrown.
     * <p>
     * Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     *
     * @return The selected {@link IClass.IMethod} or <code>null</code>
     */
    public IClass.IMethod findIMethod(Java.MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod;
        FIND_METHOD: {

            // Method declared by enclosing type declarations?
            if (mi.optionalTarget == null) {
                for (Java.Scope s = mi.getEnclosingBlockStatement(); !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
                    if (s instanceof Java.TypeDeclaration) {
                        Java.TypeDeclaration td = (Java.TypeDeclaration) s;
        
                        // Find methods with specified name.
                        iMethod = this.findIMethod(
                            (Locatable) mi,    // loc
                            this.resolve(td),  // targetType
                            mi.methodName,     // methodName
                            mi.arguments       // arguments
                        );
                        if (iMethod != null) break FIND_METHOD;
                    }
                }
            }

            // Method declared by the target's type?
            if (mi.optionalTarget != null) {

                // Find methods with specified name.
                iMethod = this.findIMethod(
                        (Locatable) mi,                  // loc
                        this.getType(mi.optionalTarget), // targetType
                        mi.methodName,                   // methodName
                        mi.arguments                     // arguments
                );
                if (iMethod != null) break FIND_METHOD;
            }
            
            // Static method declared through single static import?
            {
                Object o = this.singleStaticImports.get(mi.methodName);
                if (o instanceof List) {
                    IClass declaringIClass = ((IMethod) ((List) o).get(0)).getDeclaringIClass();
                    iMethod = this.findIMethod(
                        (Locatable) mi,  // l
                        declaringIClass, // targetType
                        mi.methodName,   // methodName
                        mi.arguments     // arguments
                    );
                    if (iMethod != null) break FIND_METHOD;
                }
            }

            // Static method declared through static-import-on-demand?
            iMethod = null;
            for (Iterator it = this.staticImportsOnDemand.iterator(); it.hasNext();) {
                IClass iClass = (IClass) it.next();
                IMethod im = this.findIMethod(
                    (Locatable) mi, // l
                    iClass,         // targetType
                    mi.methodName,  // methodName
                    mi.arguments    // arguments
                );
                if (im != null) {
                    if (iMethod != null) UnitCompiler.this.compileError("Ambiguous static method import: \"" + iMethod.toString() + "\" vs. \"" + im.toString() + "\"");
                    iMethod = im;
                }
            }
            if (iMethod != null) break FIND_METHOD;

            this.compileError("A method named \"" + mi.methodName + "\" is not declared in any enclosing class nor any supertype, nor through a static import", mi.getLocation());
            return fakeIMethod(this.iClassLoader.OBJECT, mi.methodName, mi.arguments);
        }

        this.checkThrownExceptions(mi, iMethod);
        return iMethod;
    }

    /**
     * Find a {@link IClass.IMethod} in the given <code>targetType</code>, its superclasses or
     * superinterfaces with the given <code>name</code> and for the given <code>arguments</code>.
     * If more than one such method exists, choose the most specific one (JLS 15.11.2).
     *
     * @return <code>null</code> if no appropriate method could be found
     */
    private IClass.IMethod findIMethod(
        Locatable loc,
        IClass    targetType,
        String    methodName,
        Rvalue[]  arguments
    ) throws CompileException {

        // Get all methods 
        List ms = new ArrayList();
        this.getIMethods(targetType, methodName, ms);
        if (ms.size() == 0) return null;

        // Determine arguments' types, choose the most specific method
        return (IClass.IMethod) this.findMostSpecificIInvocable(
            loc,                                                          // loc
            (IClass.IMethod[]) ms.toArray(new IClass.IMethod[ms.size()]), // iInvocables
            arguments                                                     // arguments
        );
    }

    private IMethod fakeIMethod(IClass targetType, final String name, Rvalue[] arguments) throws CompileException {
        final IClass[] pts = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) pts[i] = this.getType(arguments[i]);
        return targetType.new IMethod() {
            public String   getName()                                     { return name; }
            public IClass   getReturnType() throws CompileException       { return IClass.INT; }
            public boolean  isStatic()                                    { return false; }
            public boolean  isAbstract()                                  { return false; }
            public IClass[] getParameterTypes() throws CompileException   { return pts; }
            public IClass[] getThrownExceptions() throws CompileException { return new IClass[0]; }
            public Access   getAccess()                                   { return Access.PUBLIC; }
        };
    }

    /**
     * Add all methods with the given <code>methodName</code> that are declared
     * by the <code>type</code>, its superclasses and all their superinterfaces
     * to the result list <code>v</code>.
     * @param type
     * @param methodName
     * @param v
     * @throws CompileException
     */
    public void getIMethods(
        IClass type,
        String methodName,
        List   v // IMethod
    ) throws CompileException {

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

        // JLS2 6.4.3
        if (superclass == null && interfaces.length == 0 && type.isInterface()) {
            IClass.IMethod[] oms = this.iClassLoader.OBJECT.getDeclaredIMethods(methodName);
            for (int i = 0; i < oms.length; ++i) {
                IClass.IMethod om = oms[i];
                if (!om.isStatic() && om.getAccess() == Access.PUBLIC) v.add(om);
            }
        }
    }

    public IClass.IMethod findIMethod(Java.SuperclassMethodInvocation scmi) throws CompileException {
        Java.ClassDeclaration declaringClass;
        for (Java.Scope s = scmi.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof Java.FunctionDeclarator) {
                Java.FunctionDeclarator fd = (Java.FunctionDeclarator) s;
                if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Superclass method cannot be invoked in static context", scmi.getLocation());
            }
            if (s instanceof Java.ClassDeclaration) {
                declaringClass = (Java.ClassDeclaration) s;
                break;
            }
        }
        IClass superclass = this.resolve(declaringClass).getSuperclass();
        IMethod iMethod = this.findIMethod(
            (Locatable) scmi, // l
            superclass,       // targetType
            scmi.methodName,  // methodName
            scmi.arguments    // arguments
        );
        if (iMethod == null) {
            this.compileError("Class \"" + superclass + "\" has no method named \"" + scmi.methodName + "\"", scmi.getLocation());
            return fakeIMethod(superclass, scmi.methodName, scmi.arguments);
        }
        this.checkThrownExceptions(scmi, iMethod);
        return iMethod;
    }

    /**
     * Determine the arguments' types, determine the applicable invocables and choose the most
     * specific invocable.
     *
     * @param iInvocables Length must be greater than zero
     *
     * @return The selected {@link IClass.IInvocable}
     * @throws CompileException
     */
    private IClass.IInvocable findMostSpecificIInvocable(
        Locatable          l,
        final IInvocable[] iInvocables,
        Rvalue[]           arguments
    ) throws CompileException {

        // Determine arguments' types.
        final IClass[] argumentTypes = new IClass[arguments.length];
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = this.getType(arguments[i]);
        }

        IInvocable ii = this.findMostSpecificIInvocable(l, iInvocables, argumentTypes, false);
        if (ii != null) return ii;

        ii = this.findMostSpecificIInvocable(l, iInvocables, argumentTypes, true);
        if (ii != null) return ii;
        
        // Report a nice compile error.
        StringBuffer sb = new StringBuffer("No applicable constructor/method found for ");
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
        this.compileError(sb.toString(), l.getLocation());

        // Well, returning a "fake" IInvocable is a bit tricky, because the iInvocables
        // can be of different types.
        if (iInvocables[0] instanceof IClass.IConstructor) {
            return iInvocables[0].getDeclaringIClass().new IConstructor() {
                public IClass[] getParameterTypes()   { return argumentTypes; }
                public Access   getAccess()           { return Access.PUBLIC; }
                public IClass[] getThrownExceptions() { return new IClass[0]; }
            };
        } else
        if (iInvocables[0] instanceof IClass.IMethod) {
            return iInvocables[0].getDeclaringIClass().new IMethod() {
                public boolean  isStatic()            { return true; }
                public boolean  isAbstract()          { return false; }
                public IClass   getReturnType()       { return IClass.INT; }
                public String   getName()             { return ((IClass.IMethod) iInvocables[0]).getName(); }
                public Access   getAccess()           { return Access.PUBLIC; }
                public IClass[] getParameterTypes()   { return argumentTypes; }
                public IClass[] getThrownExceptions() { return new IClass[0]; }
            };
        } else
        {
            return iInvocables[0];
        }
    }

    /**
     * Determine the applicable invocables and choose the most specific invocable.
     * 
     * @return the maximally specific {@link IClass.IInvocable} or <code>null</code> if no {@link IClass.IInvocable} is applicable
     *
     * @throws CompileException
     */
    public IClass.IInvocable findMostSpecificIInvocable(
        Locatable          l,
        final IInvocable[] iInvocables,
        final IClass[]     argumentTypes,
        boolean            boxingPermitted
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

            // Check argument types vs. parameter types.
            if (UnitCompiler.DEBUG) System.out.println("Parameter / argument type check:");
            for (int j = 0; j < argumentTypes.length; ++j) {
                // Is method invocation conversion possible (5.3)?
                if (UnitCompiler.DEBUG) System.out.println(parameterTypes[j] + " <=> " + argumentTypes[j]);
                if (!this.isMethodInvocationConvertible(argumentTypes[j], parameterTypes[j], boxingPermitted)) continue NEXT_METHOD;
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
            int moreSpecific = 0, lessSpecific = 0;
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
            final IClass.IMethod im = (IClass.IMethod) maximallySpecificIInvocables.get(0);

            // Check if all methods have the same signature (i.e. the types of all their
            // parameters are identical) and exactly one of the methods is non-abstract
            // (JLS 15.12.2.2.BL2.B1).
            IClass.IMethod theNonAbstractMethod = null;
            {
                Iterator it = maximallySpecificIInvocables.iterator();
                IClass.IMethod m = (IClass.IMethod) it.next();
                IClass[] parameterTypesOfFirstMethod = m.getParameterTypes();
                for (;;) {
                    if (!m.isAbstract()) {
                        if (theNonAbstractMethod != null) {
                            IClass declaringIClass = m.getDeclaringIClass();
                            IClass theNonAbstractMethodDeclaringIClass = theNonAbstractMethod.getDeclaringIClass();
                            if (declaringIClass.isAssignableFrom(theNonAbstractMethodDeclaringIClass)) {
                                ;
                            } else
                            if (theNonAbstractMethodDeclaringIClass.isAssignableFrom(declaringIClass)) {
                                theNonAbstractMethod = m;
                            } else
                            {
                                throw new RuntimeException("SNO: More than one non-abstract method with same signature and same declaring class!?");
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
            Set s = new HashSet();
            {
                IClass[][] tes = new IClass[maximallySpecificIInvocables.size()][];
                Iterator it = maximallySpecificIInvocables.iterator();
                for (int i = 0; i < tes.length; ++i) {
                    tes[i] = ((IClass.IMethod) it.next()).getThrownExceptions();
                }
                for (int i = 0; i < tes.length; ++i) {
                    EACH_EXCEPTION:
                    for (int j = 0; j < tes[i].length; ++j) {

                        // Check whether "that exception [te1] is declared in the THROWS
                        // clause of each of the maximally specific methods".
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

            final IClass[] tes = (IClass[]) s.toArray(new IClass[s.size()]);
            return im.getDeclaringIClass().new IMethod() {
                public String   getName()                                   { return im.getName(); }
                public IClass   getReturnType() throws CompileException     { return im.getReturnType(); }
                public boolean  isAbstract()                                { return im.isAbstract(); }
                public boolean  isStatic()                                  { return im.isStatic(); }
                public Access   getAccess()                                 { return im.getAccess(); }
                public IClass[] getParameterTypes() throws CompileException { return im.getParameterTypes(); }
                public IClass[] getThrownExceptions()                       { return tes; }
            };
        }

        // JLS 15.12.2.2.BL2.B2
        {
            StringBuffer sb = new StringBuffer("Invocation of constructor/method with actual parameter type(s) \"");
            for (int i = 0; i < argumentTypes.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(Descriptor.toString(argumentTypes[i].getDescriptor()));
            }
            sb.append("\" is ambiguous: ");
            for (int i = 0; i < maximallySpecificIInvocables.size(); ++i) {
                if (i > 0) sb.append(" vs. ");
                sb.append("\"" + maximallySpecificIInvocables.get(i) + "\"");
            }
            this.compileError(sb.toString(), l.getLocation());
        }

        return (IClass.IMethod) iInvocables[0];
    }

    /**
     * Check if "method invocation conversion" (5.3) is possible.
     */
    private boolean isMethodInvocationConvertible(
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
                    this.isIdentityConvertible(boxedType, targetType) ||
                    this.isWideningReferenceConvertible(boxedType, targetType)
                );
            }
        }

        // JLS3 5.3 An unboxing conversion (JLS3 5.1.8) optionally followed by a widening primitive conversion.
        if (boxingPermitted) {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                return (
                    this.isIdentityConvertible(unboxedType, targetType) ||
                    this.isWideningPrimitiveConvertible(unboxedType, targetType)
                );
            }
        }

        // 5.3 TODO: FLOAT or DOUBLE value set conversion

        return false;
    }

    /**
     * @throws CompileException if the {@link Invocation} throws exceptions that are disallowed in the given scope
     */
    private void checkThrownExceptions(Invocation in, IMethod iMethod) throws CompileException {
        IClass[] thrownExceptions = iMethod.getThrownExceptions();
        for (int i = 0; i < thrownExceptions.length; ++i) {
            this.checkThrownException(
                (Locatable) in,                              // l
                thrownExceptions[i],                         // type
                (Java.Scope) in.getEnclosingBlockStatement() // scope
            );
        }
    }

    /**
     * @throws CompileException if the exception with the given type must not be thrown in the given scope
     */
    private void checkThrownException(
        Locatable  l,
        IClass     type,
        Java.Scope scope
    ) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!this.iClassLoader.THROWABLE.isAssignableFrom(type)) this.compileError("Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"", l.getLocation());

        // "RuntimeException" and "Error" are never checked.
        if (
            this.iClassLoader.RUNTIME_EXCEPTION.isAssignableFrom(type) ||
            this.iClassLoader.ERROR.isAssignableFrom(type)
        ) return;

        for (;; scope = scope.getEnclosingScope()) {

            // Match against enclosing "try...catch" blocks.
            if (scope instanceof Java.TryStatement) {
                Java.TryStatement ts = (Java.TryStatement) scope;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    Java.CatchClause cc = (Java.CatchClause) ts.catchClauses.get(i);
                    IClass caughtType = this.getType(cc.caughtException.type);
                    if (caughtType.isAssignableFrom(type)) return;
                }
            } else

            // Match against "throws" clause of declaring function.
            if (scope instanceof Java.FunctionDeclarator) {
                Java.FunctionDeclarator fd = (Java.FunctionDeclarator) scope;
                for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                    IClass te = this.getType(fd.thrownExceptions[i]);
                    if (te.isAssignableFrom(type)) return;
                }
                break;
            } else

            if (scope instanceof Java.TypeBodyDeclaration) {
                break;
            }
        }

        this.compileError("Thrown exception of type \"" + type + "\" is neither caught by a \"try...catch\" block nor declared in the \"throws\" clause of the declaring function", l.getLocation());
    }

    private IClass getTargetIClass(Java.QualifiedThisReference qtr) throws CompileException {

        // Determine target type.
        if (qtr.targetIClass == null) {
            qtr.targetIClass = this.getType(qtr.qualification);
        }
        return qtr.targetIClass;
    }

    /**
     * Checks whether the operand is an integer-like local variable.
     */
    Java.LocalVariable isIntLV(Java.Crement c) throws CompileException {
        if (!(c.operand instanceof Java.AmbiguousName)) return null;
        Java.AmbiguousName an = (Java.AmbiguousName) c.operand;

        Java.Atom rec = this.reclassify(an);
        if (!(rec instanceof Java.LocalVariableAccess)) return null;
        Java.LocalVariableAccess lva = (Java.LocalVariableAccess) rec;

        Java.LocalVariable lv = lva.localVariable;
        if (lv.finaL) this.compileError("Must not increment or decrement \"final\" local variable", lva.getLocation());
        if (
            lv.type == IClass.BYTE  ||
            lv.type == IClass.SHORT ||
            lv.type == IClass.INT   ||
            lv.type == IClass.CHAR
        ) return lv;
        return null;
    }

    private IClass resolve(final Java.TypeDeclaration td) {
        final Java.AbstractTypeDeclaration atd = (Java.AbstractTypeDeclaration) td;
        if (atd.resolvedType == null) atd.resolvedType = new IClass() {
            protected IClass.IMethod[] getDeclaredIMethods2() {
                IClass.IMethod[] res = new IClass.IMethod[atd.declaredMethods.size()];
                int i = 0;
                for (Iterator it = atd.declaredMethods.iterator(); it.hasNext();) {
                    res[i++] = UnitCompiler.this.toIMethod((Java.MethodDeclarator) it.next());
                }
                return res;
            }
            private IClass[] declaredClasses = null;
            protected IClass[] getDeclaredIClasses2() {
                if (this.declaredClasses == null) {
                    IClass[] mts = new IClass[atd.declaredClassesAndInterfaces.size()];
                    int i = 0;
                    for (Iterator it = atd.declaredClassesAndInterfaces.iterator(); it.hasNext();) {
                        mts[i++] = UnitCompiler.this.resolve((Java.AbstractTypeDeclaration) it.next());
                    }
                    this.declaredClasses = mts;
                }
                return this.declaredClasses;
            }
            protected IClass getDeclaringIClass2() {
                Java.Scope s = atd;
                for (; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope()) {
                    if (s instanceof Java.CompilationUnit) return null;
                }
                return UnitCompiler.this.resolve((Java.AbstractTypeDeclaration) s.getEnclosingScope());
            }
            protected IClass getOuterIClass2() throws CompileException {
                Java.AbstractTypeDeclaration oc = (Java.AbstractTypeDeclaration) UnitCompiler.getOuterClass(atd);
                if (oc == null) return null;
                return UnitCompiler.this.resolve(oc);
            }
            protected final String getDescriptor2() {
                return Descriptor.fromClassName(atd.getClassName());
            }
        
            public boolean isArray() { return false; }
            protected IClass  getComponentType2() { throw new RuntimeException("SNO: Non-array type has no component type"); }
            public boolean isPrimitive() { return false; }
            public boolean isPrimitiveNumeric() { return false; }
            protected IConstructor[] getDeclaredIConstructors2() {
                if (atd instanceof Java.ClassDeclaration) {
                    Java.ConstructorDeclarator[] cs = ((Java.ClassDeclaration) atd).getConstructors();
    
                    IClass.IConstructor[] res = new IClass.IConstructor[cs.length];
                    for (int i = 0; i < cs.length; ++i) res[i] = UnitCompiler.this.toIConstructor(cs[i]);
                    return res;
                }
                return new IClass.IConstructor[0];
            }
            protected IField[] getDeclaredIFields2() {
                if (atd instanceof Java.ClassDeclaration) {
                    Java.ClassDeclaration cd = (Java.ClassDeclaration) atd;
                    List l = new ArrayList(); // IClass.IField

                    // Determine variable declarators of type declaration.
                    for (int i = 0; i < cd.variableDeclaratorsAndInitializers.size(); ++i) {
                        Java.BlockStatement vdoi = (Java.BlockStatement) cd.variableDeclaratorsAndInitializers.get(i);
                        if (vdoi instanceof Java.FieldDeclaration) {
                            Java.FieldDeclaration fd = (Java.FieldDeclaration) vdoi;
                            IClass.IField[] flds = UnitCompiler.this.getIFields(fd);
                            for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                        }
                    }
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else
                if (atd instanceof Java.InterfaceDeclaration) {
                    Java.InterfaceDeclaration id = (Java.InterfaceDeclaration) atd;
                    List l = new ArrayList();

                    // Determine static fields.
                    for (int i = 0; i < id.constantDeclarations.size(); ++i) {
                        Java.BlockStatement bs = (Java.BlockStatement) id.constantDeclarations.get(i);
                        if (bs instanceof Java.FieldDeclaration) {
                            Java.FieldDeclaration fd = (Java.FieldDeclaration) bs;
                            IClass.IField[] flds = UnitCompiler.this.getIFields(fd);
                            for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                        }
                    }
                    return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
                } else {
                    throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
                }
            }
            public IField[] getSyntheticIFields() {
                if (atd instanceof Java.ClassDeclaration) {
                    Collection c = ((Java.ClassDeclaration) atd).syntheticFields.values();
                    return (IField[]) c.toArray(new IField[c.size()]);
                }
                return new IField[0];
            }
            protected IClass getSuperclass2() throws CompileException {
                if (atd instanceof Java.AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getType(((Java.AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? UnitCompiler.this.iClassLoader.OBJECT : bt;
                }
                if (atd instanceof Java.NamedClassDeclaration) {
                    Java.NamedClassDeclaration ncd = (Java.NamedClassDeclaration) atd;
                    if (ncd.optionalExtendedType == null) return UnitCompiler.this.iClassLoader.OBJECT;
                    IClass superclass = UnitCompiler.this.getType(ncd.optionalExtendedType);
                    if (superclass.isInterface()) UnitCompiler.this.compileError("\"" + superclass.toString() + "\" is an interface; classes can only extend a class", td.getLocation());
                    return superclass;
                }
                return null;
            }
            public Access getAccess() { return UnitCompiler.modifiers2Access(atd.modifiers); }
            public boolean isFinal() { return (atd.modifiers & Mod.FINAL) != 0;  }
            protected IClass[] getInterfaces2() throws CompileException {
                if (atd instanceof Java.AnonymousClassDeclaration) {
                    IClass bt = UnitCompiler.this.getType(((Java.AnonymousClassDeclaration) atd).baseType);
                    return bt.isInterface() ? new IClass[] { bt } : new IClass[0];
                } else
                if (atd instanceof Java.NamedClassDeclaration) {
                    Java.NamedClassDeclaration ncd = (Java.NamedClassDeclaration) atd;
                    IClass[] res = new IClass[ncd.implementedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getType(ncd.implementedTypes[i]);
                        if (!res[i].isInterface()) UnitCompiler.this.compileError("\"" + res[i].toString() + "\" is not an interface; classes can only implement interfaces", td.getLocation());
                    }
                    return res;
                } else
                if (atd instanceof Java.InterfaceDeclaration) {
                    Java.InterfaceDeclaration id = (Java.InterfaceDeclaration) atd;
                    IClass[] res = new IClass[id.extendedTypes.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = UnitCompiler.this.getType(id.extendedTypes[i]);
                        if (!res[i].isInterface()) UnitCompiler.this.compileError("\"" + res[i].toString() + "\" is not an interface; interfaces can only extend interfaces", td.getLocation());
                    }
                    return res;
                } else {
                    throw new RuntimeException("SNO: AbstractTypeDeclaration is neither ClassDeclaration nor InterfaceDeclaration");
                }
            }
            public boolean isAbstract() {
                return (
                    (atd instanceof Java.InterfaceDeclaration)
                    || (atd.modifiers & Mod.ABSTRACT) != 0
                );
            }
            public boolean isInterface() { return atd instanceof Java.InterfaceDeclaration; }
        };

        return atd.resolvedType;
    }

    private void referenceThis(
        Locatable                l,
        Java.ClassDeclaration    declaringClass,
        Java.TypeBodyDeclaration declaringTypeBodyDeclaration,
        IClass                   targetIClass
    ) throws CompileException {
        List path = UnitCompiler.getOuterClasses(declaringClass);

        if (declaringTypeBodyDeclaration.isStatic()) this.compileError("No current instance available in static context", l.getLocation());

        int j;
        TARGET_FOUND: {
            for (j = 0; j < path.size(); ++j) {

                // Notice: JLS 15.9.2.BL1.B3.B1.B2 seems to be wrong: Obviously, JAVAC does not
                // only allow
                //
                //    O is the nth lexically enclosing class
                //
                // , but also
                //
                //    O is assignable from the nth lexically enclosing class
                //
                // However, this strategy bears the risk of ambiguities, because "O" may be
                // assignable from more than one enclosing class.
                if (targetIClass.isAssignableFrom(this.resolve((Java.AbstractTypeDeclaration) path.get(j)))) break TARGET_FOUND;
            }
            this.compileError("\"" + declaringClass + "\" is not enclosed by \"" + targetIClass + "\"", l.getLocation());
        }

        int i;
        if (declaringTypeBodyDeclaration instanceof Java.ConstructorDeclarator) {
            if (j == 0) {
                this.writeOpcode(l, Opcode.ALOAD_0);
                return;
            }

            Java.ConstructorDeclarator constructorDeclarator = (Java.ConstructorDeclarator) declaringTypeBodyDeclaration;
            String spn = "this$" + (path.size() - 2);
            Java.LocalVariable syntheticParameter = (Java.LocalVariable) constructorDeclarator.syntheticParameters.get(spn);
            if (syntheticParameter == null) throw new RuntimeException("SNO: Synthetic parameter \""+ spn + "\" not found");
            this.load(l, syntheticParameter);
            i = 1;
        } else {
            this.writeOpcode(l, Opcode.ALOAD_0);
            i = 0;
        }
        for (; i < j; ++i) {
            final String fieldName = "this$" + (path.size() - i - 2);
            final Java.InnerClassDeclaration inner = (Java.InnerClassDeclaration) path.get(i);
            IClass iic = this.resolve((Java.AbstractTypeDeclaration) inner);
            final Java.TypeDeclaration outer = (Java.TypeDeclaration) path.get(i + 1);
            final IClass oic = this.resolve((Java.AbstractTypeDeclaration) outer);
            inner.defineSyntheticField(new SimpleIField(
                iic,
                fieldName,
                oic
            ));
            this.writeOpcode(l, Opcode.GETFIELD);
            this.writeConstantFieldrefInfo(
                iic.getDescriptor(),
                fieldName, // classFD
                oic.getDescriptor()  // fieldFD
            );
        }
    }

    /**
     * Return a list consisting of the given <code>inner</code> class and all its outer classes.
     * @return {@link List} of {@link Java.TypeDeclaration}
     */
    private static List getOuterClasses(Java.TypeDeclaration inner) {
        List path = new ArrayList();
        for (Java.TypeDeclaration ic = inner; ic != null; ic = UnitCompiler.getOuterClass(ic)) path.add(ic);
        return path;
    }

    /*package*/ static Java.TypeDeclaration getOuterClass(Java.TypeDeclaration atd) {

        // Package member class declaration.
        if (atd instanceof Java.PackageMemberClassDeclaration) return null;

        // Local class declaration.
        if (atd instanceof Java.LocalClassDeclaration) {
            Java.Scope s = atd.getEnclosingScope();
            for (; !(s instanceof Java.FunctionDeclarator); s = s.getEnclosingScope());
            if ((s instanceof Java.MethodDeclarator) && (((Java.FunctionDeclarator) s).modifiers & Mod.STATIC) != 0) return null;
            for (; !(s instanceof Java.TypeDeclaration); s = s.getEnclosingScope());
            Java.TypeDeclaration immediatelyEnclosingTypeDeclaration = (Java.TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof Java.ClassDeclaration
            ) ? immediatelyEnclosingTypeDeclaration : null;
        }

        // Member class declaration.
        if (
            atd instanceof Java.MemberClassDeclaration &&
            (((Java.MemberClassDeclaration) atd).modifiers & Mod.STATIC) != 0
        ) return null;

        // Anonymous class declaration, interface declaration
        Java.Scope s = atd;
        for (; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope()) {
            if (s instanceof Java.ConstructorInvocation) return null;
            if (s instanceof Java.CompilationUnit) return null;
        }
        //if (!(s instanceof Java.ClassDeclaration)) return null;
        if (((Java.TypeBodyDeclaration) s).isStatic()) return null;
        return (Java.AbstractTypeDeclaration) s.getEnclosingScope();
    }

    private IClass getIClass(Java.ThisReference tr) throws CompileException {
        if (tr.iClass == null) {

            // Compile error if in static function context.
            Java.Scope s;
            for (s = tr.getEnclosingBlockStatement(); s instanceof Java.Statement || s instanceof Java.CatchClause; s = s.getEnclosingScope());
            if (s instanceof Java.FunctionDeclarator) {
                Java.FunctionDeclarator function = (Java.FunctionDeclarator) s;
                if ((function.modifiers & Mod.STATIC) != 0) this.compileError("No current instance available in static method", tr.getLocation());
            }

            // Determine declaring type.
            while (!(s instanceof Java.TypeDeclaration)) s = s.getEnclosingScope();
            if (!(s instanceof Java.ClassDeclaration)) this.compileError("Only methods of classes can have a current instance", tr.getLocation());
            tr.iClass = this.resolve((Java.ClassDeclaration) s);
        }
        return tr.iClass;
    }

    private IClass getReturnType(Java.FunctionDeclarator fd) throws CompileException {
        if (fd.returnType == null) {
            fd.returnType = this.getType(fd.type);
        }
        return fd.returnType;
    }

    /*package*/ IClass.IConstructor toIConstructor(final Java.ConstructorDeclarator cd) {
        if (cd.iConstructor != null) return cd.iConstructor;

        cd.iConstructor = this.resolve((Java.AbstractTypeDeclaration) cd.getDeclaringType()).new IConstructor() {

            // Implement IMember.
            public Access getAccess() {
                switch (cd.modifiers & Mod.PPP) {
                case Mod.PRIVATE:
                    return Access.PRIVATE;
                case Mod.PROTECTED:
                    return Access.PROTECTED;
                case Mod.PACKAGE:
                    return Access.DEFAULT;
                case Mod.PUBLIC:
                    return Access.PUBLIC;
                default:
                    throw new RuntimeException("Invalid access");
                }
            }

            // Implement IInvocable.
            public String getDescriptor() throws CompileException {
                if (!(cd.getDeclaringClass() instanceof Java.InnerClassDeclaration)) return super.getDescriptor();

                List l = new ArrayList();

                // Convert enclosing instance reference into prepended constructor parameters.
                IClass outerClass = UnitCompiler.this.resolve(cd.getDeclaringClass()).getOuterIClass();
                if (outerClass != null) l.add(outerClass.getDescriptor());

                // Convert synthetic fields into prepended constructor parameters.
                for (Iterator it = cd.getDeclaringClass().syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    if (sf.getName().startsWith("val$")) l.add(sf.getType().getDescriptor());
                }
                Java.FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
                for (int i = 0; i < fps.length; ++i) {
                    l.add(UnitCompiler.this.getType(fps[i].type).getDescriptor());
                }
                String[] apd = (String[]) l.toArray(new String[l.size()]);
                return new MethodDescriptor(apd, Descriptor.VOID_).toString();
            }

            public IClass[] getParameterTypes() throws CompileException {
                Java.FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
                IClass[] res = new IClass[fps.length];
                for (int i = 0; i < fps.length; ++i) {
                    res[i] = UnitCompiler.this.getType(fps[i].type);
                }
                return res;
            }
            public IClass[] getThrownExceptions() throws CompileException {
                IClass[] res = new IClass[cd.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getType(cd.thrownExceptions[i]);
                }
                return res;
            }

            public String toString() {
                StringBuffer sb = new StringBuffer();
                sb.append(cd.getDeclaringType().getClassName());
                sb.append('(');
                Java.FunctionDeclarator.FormalParameter[] fps = cd.formalParameters;
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

    public IClass.IMethod toIMethod(final Java.MethodDeclarator md) {
        if (md.iMethod != null) return md.iMethod;
        md.iMethod = this.resolve((Java.AbstractTypeDeclaration) md.getDeclaringType()).new IMethod() {

            // Implement IMember.
            public Access getAccess() {
                switch (md.modifiers & Mod.PPP) {
                case Mod.PRIVATE:
                    return Access.PRIVATE;
                case Mod.PROTECTED:
                    return Access.PROTECTED;
                case Mod.PACKAGE:
                    return Access.DEFAULT;
                case Mod.PUBLIC:
                    return Access.PUBLIC;
                default:
                    throw new RuntimeException("Invalid access");
                }
            }

            // Implement IInvocable.
            public IClass[] getParameterTypes() throws CompileException {
                Java.FunctionDeclarator.FormalParameter[] fps = md.formalParameters;
                IClass[] res = new IClass[fps.length];
                for (int i = 0; i < fps.length; ++i) {
                    res[i] = UnitCompiler.this.getType(fps[i].type);
                }
                return res;
            }
            public IClass[] getThrownExceptions() throws CompileException {
                IClass[] res = new IClass[md.thrownExceptions.length];
                for (int i = 0; i < res.length; ++i) {
                    res[i] = UnitCompiler.this.getType(md.thrownExceptions[i]);
                }
                return res;
            }

            // Implement IMethod.
            public boolean isStatic() { return (md.modifiers & Mod.STATIC) != 0; }
            public boolean isAbstract() { return (md.getDeclaringType() instanceof Java.InterfaceDeclaration) || (md.modifiers & Mod.ABSTRACT) != 0; }
            public IClass getReturnType() throws CompileException {
                return UnitCompiler.this.getReturnType(md);
            }
            public String getName() { return md.name; }
        };
        return md.iMethod;
    }

    private IClass.IInvocable toIInvocable(Java.FunctionDeclarator fd) {
        if (fd instanceof Java.ConstructorDeclarator) {
            return this.toIConstructor((Java.ConstructorDeclarator) fd);
        } else
        if (fd instanceof Java.MethodDeclarator) {
            return this.toIMethod((Java.MethodDeclarator) fd);
        } else
        {
            throw new RuntimeException("FunctionDeclarator is neither ConstructorDeclarator nor MethodDeclarator");
        }
    }

    /**
     * If the given name was declared in a simple type import, load that class.
     */
    private IClass importSingleType(String simpleTypeName, Location location) throws CompileException {
        String[] ss = this.getSingleTypeImport(simpleTypeName);
        if (ss == null) return null;

        IClass iClass = this.loadFullyQualifiedClass(ss);
        if (iClass == null) {
            this.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
            return this.iClassLoader.OBJECT;
        }
        return iClass;
    }

    /**
     * Check if the given name was imported through a "single type import", e.g.<pre>
     *     import java.util.Map</pre>
     * 
     * @return the fully qualified name or <code>null</code>
     */
    public String[] getSingleTypeImport(String name) {
        return (String[]) this.singleTypeImports.get(name);
    }

    /**
     * 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.<br>
     * 6.5.5.1.6 Type-import-on-demand declaration.
     * @return <code>null</code> if the given <code>simpleTypeName</code> cannot be resolved through any of the import-on-demand directives
     */
    public IClass importTypeOnDemand(String simpleTypeName, Location location) throws CompileException {

        // Check cache. (A cache for unimportable types is not required, because
        // the class is importable 99.9%.)
        IClass importedClass = (IClass) this.onDemandImportableTypes.get(simpleTypeName);
        if (importedClass != null) return importedClass;

        // Cache miss...
        for (Iterator i = this.typeImportsOnDemand.iterator(); i.hasNext();) {
            String[] ss = (String[]) i.next();
            String[] ss2 = concat(ss, simpleTypeName);
            IClass iClass = this.loadFullyQualifiedClass(ss2);
            if (iClass != null) {
                if (importedClass != null && importedClass != iClass) this.compileError("Ambiguous class name: \"" + importedClass + "\" vs. \"" + iClass + "\"", location);
                importedClass = iClass;
            }
        }
        if (importedClass == null) return null;

        // Put in cache and return.
        this.onDemandImportableTypes.put(simpleTypeName, importedClass);
        return importedClass;
    }
    private final Map onDemandImportableTypes = new HashMap();   // String simpleTypeName => IClass
    private void declareClassDollarMethod(Java.ClassLiteral cl) {

        // Method "class$" is not yet declared; declare it like
        //
        //   static java.lang.Class class$(java.lang.String className) {
        //       try {
        //           return java.lang.Class.forName(className);
        //       } catch (java.lang.ClassNotFoundException ex) {
        //           throw new java.lang.NoClassDefFoundError(ex.getMessage());
        //       }
        //   }
        //
        Location loc = cl.getLocation();
        Java.AbstractTypeDeclaration declaringType;
        for (Java.Scope s = cl.getEnclosingBlockStatement();; s = s.getEnclosingScope()) {
            if (s instanceof Java.AbstractTypeDeclaration) {
                declaringType = (Java.AbstractTypeDeclaration) s;
                break;
            }
        }
        Java.Block body = new Java.Block(loc);

        // try {
        // return Class.forName(className);
        Java.MethodInvocation mi = new Java.MethodInvocation(
            loc,                                               // location
            new Java.SimpleType(loc, this.iClassLoader.CLASS), // optionalTarget
            "forName",                                         // methodName
            new Java.Rvalue[] {                                // arguments
                new Java.AmbiguousName(loc, new String[] { "className" } )
            }
        );

        IClass classNotFoundExceptionIClass;
        try {
            classNotFoundExceptionIClass = this.iClassLoader.loadIClass("Ljava/lang/ClassNotFoundException;");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Loading class \"ClassNotFoundException\": " + ex.getMessage());
        }
        if (classNotFoundExceptionIClass == null) throw new RuntimeException("SNO: Cannot load \"ClassNotFoundException\"");

        IClass noClassDefFoundErrorIClass;
        try {
            noClassDefFoundErrorIClass = this.iClassLoader.loadIClass("Ljava/lang/NoClassDefFoundError;");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Loading class \"NoClassDefFoundError\": " + ex.getMessage());
        }
        if (noClassDefFoundErrorIClass == null) throw new RuntimeException("SNO: Cannot load \"NoClassFoundError\"");

        // catch (ClassNotFoundException ex) {
        Java.Block b = new Java.Block(loc);
        // throw new NoClassDefFoundError(ex.getMessage());
        b.addStatement(new Java.ThrowStatement(loc, new Java.NewClassInstance(
            loc,                                                  // location
            (Java.Rvalue) null,                                   // optionalQualification
            new Java.SimpleType(loc, noClassDefFoundErrorIClass), // type
            new Java.Rvalue[] {                                   // arguments
                new Java.MethodInvocation(
                    loc,                                                // location
                    new Java.AmbiguousName(loc, new String[] { "ex"} ), // optionalTarget
                    "getMessage",                                       // methodName
                    new Java.Rvalue[0]                                  // arguments
                )
            }
        )));

        List l = new ArrayList();
        l.add(new Java.CatchClause(
            loc,                                         // location
            new Java.FunctionDeclarator.FormalParameter( // caughtException
                loc,                                                    // location
                true,                                                   // finaL
                new Java.SimpleType(loc, classNotFoundExceptionIClass), // type
                "ex"                                                    // name
            ),
            b                                            // body
        ));
        Java.TryStatement ts = new Java.TryStatement(
            loc,                               // location
            new Java.ReturnStatement(loc, mi), // body
            l,                                 // catchClauses
            null                               // optionalFinally
        );

        body.addStatement(ts);

        // Class class$(String className)
        Java.FunctionDeclarator.FormalParameter fp = new Java.FunctionDeclarator.FormalParameter(
            loc,                                                // location
            false,                                              // finaL
            new Java.SimpleType(loc, this.iClassLoader.STRING), // type
            "className"                                         // name
        );
        Java.MethodDeclarator cdmd = new Java.MethodDeclarator(
            loc,                                                  // location
            null,                                                 // optionalDocComment
            Mod.STATIC,                                           // modifiers
            new Java.SimpleType(loc, this.iClassLoader.CLASS),    // type
            "class$",                                             // name
            new Java.FunctionDeclarator.FormalParameter[] { fp }, // formalParameters
            new Java.Type[0],                                     // thrownExceptions
            body                                                  // optionalBody
        );

        declaringType.addDeclaredMethod(cdmd);
        declaringType.invalidateMethodCaches();
    }

    private IClass pushConstant(Locatable l, Object value) {
        if (
            value instanceof Integer   ||
            value instanceof Short     ||
            value instanceof Character ||
            value instanceof Byte
        ) {
            int i = (
                value instanceof Character ?
                ((Character) value).charValue() :
                ((Number) value).intValue()
            );
            if (i >= -1 && i <= 5) {
                this.writeOpcode(l, Opcode.ICONST_0 + i);
            } else
            if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
                this.writeOpcode(l, Opcode.BIPUSH);
                this.writeByte((byte) i);
            } else {
                this.writeLDC(l, this.addConstantIntegerInfo(i));
            }
            return IClass.INT;
        }
        if (value instanceof Long) {
            long lv = ((Long) value).longValue();
            if (lv >= 0L && lv <= 1L) {
                this.writeOpcode(l, Opcode.LCONST_0 + (int) lv);
            } else {
                this.writeOpcode(l, Opcode.LDC2_W);
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
                this.writeOpcode(l, Opcode.FCONST_0 + (int) fv);
            } else
            {
                this.writeLDC(l, this.addConstantFloatInfo(fv));
            }
            return IClass.FLOAT;
        }
        if (value instanceof Double) {
            double dv = ((Double) value).doubleValue();
            if (
                Double.doubleToLongBits(dv) == Double.doubleToLongBits(0.0D) // POSITIVE zero!
                || dv == 1.0D
            ) {
                this.writeOpcode(l, Opcode.DCONST_0 + (int) dv);
            } else
            {
                this.writeOpcode(l, Opcode.LDC2_W);
                this.writeConstantDoubleInfo(dv);
            }
            return IClass.DOUBLE;
        }
        if (value instanceof String) {
            String s = (String) value;
            String ss[] = UnitCompiler.makeUTF8Able(s);
            this.writeLDC(l, this.addConstantStringInfo(ss[0]));
            for (int i = 1; i < ss.length; ++i) {
                this.writeLDC(l, this.addConstantStringInfo(ss[i]));
                this.writeOpcode(l, Opcode.INVOKEVIRTUAL);
                this.writeConstantMethodrefInfo(
                    Descriptor.STRING,
                    "concat",                                // classFD
                    "(" + Descriptor.STRING + ")" + Descriptor.STRING // methodMD
                );
            }
            return this.iClassLoader.STRING;
        }
        if (value instanceof Boolean) {
            this.writeOpcode(l, ((Boolean) value).booleanValue() ? Opcode.ICONST_1 : Opcode.ICONST_0);
            return IClass.BOOLEAN;
        }
        if (value == Java.Rvalue.CONSTANT_VALUE_NULL) {
            this.writeOpcode(l, Opcode.ACONST_NULL);
            return IClass.VOID;
        }
        throw new RuntimeException("Unknown literal type \"" + value.getClass().getName() + "\"");
    }

    /**
     * Only strings that can be UTF8-encoded into 65535 bytes can be stored as constant string
     * infos.
     *
     * @param s The string to split into suitable chunks
     * @return the chunks that can be UTF8-encoded into 65535 bytes
     */
    private static String[] makeUTF8Able(String s) {
        if (s.length() < (65536 / 3)) return new String[] { s };

        int sLength = s.length(), uTFLength = 0;
        int from = 0;
        List l = new ArrayList();
        for (int i = 0;; i++) {
            if (i == sLength) {
                l.add(s.substring(from));
                break;
            }
            if (uTFLength >= 65532) {
                l.add(s.substring(from, i));
                if (i + (65536 / 3) > sLength) {
                    l.add(s.substring(i));
                    break;
                }
                from = i;
                uTFLength = 0;
            }
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                ++uTFLength;
            } else
            if (c > 0x07FF) {
                uTFLength += 3;
            } else
            {
                uTFLength += 2;
            }
        }
        return (String[]) l.toArray(new String[l.size()]);

    }
    private void writeLDC(Locatable l, short index) {
        if (0 <= index && index <= 255) {
            this.writeOpcode(l, Opcode.LDC);
            this.writeByte((byte) index);
        } else {
            this.writeOpcode(l, Opcode.LDC_W);
            this.writeShort(index);
        }
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2).
     */
    private void assignmentConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType,
        Object    optionalConstantValue
    ) throws CompileException {
        if (UnitCompiler.DEBUG) System.out.println("assignmentConversion(" + sourceType + ", " + targetType + ", " + optionalConstantValue + ")");

        // JLS2 5.1.1 Identity conversion.
        if (this.tryIdentityConversion(sourceType, targetType)) return;

        // JLS2 5.1.2 Widening primitive conversion.
        if (this.tryWideningPrimitiveConversion(l, sourceType, targetType)) return;

        // JLS2 5.1.4 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return;

        // A boxing conversion (JLS3 5.1.7) optionally followed by a widening reference conversion.
        {
            IClass boxedType = this.isBoxingConvertible(sourceType);
            if (boxedType != null) {
                if (this.tryIdentityConversion(boxedType, targetType)) {
                    this.boxingConversion(l, sourceType, boxedType);
                    return;
                } else
                if (this.isWideningReferenceConvertible(boxedType, targetType)) {
                    this.boxingConversion(l, sourceType, boxedType);
                    return;
                }
            }
        }

        // An unboxing conversion (JLS3 5.1.8) optionally followed by a widening primitive conversion.
        {
            IClass unboxedType = this.isUnboxingConvertible(sourceType);
            if (unboxedType != null) {
                if (this.tryIdentityConversion(unboxedType, targetType)) {
                    this.unboxingConversion(l, sourceType, unboxedType);
                    return;
                } else
                if (this.isWideningPrimitiveConvertible(unboxedType, targetType)) {
                    this.unboxingConversion(l, sourceType, unboxedType);
                    this.tryWideningPrimitiveConversion(l, unboxedType, targetType);
                    return;
                }
            }
        }

        // 5.2 Special narrowing primitive conversion.
        if (optionalConstantValue != null) {
            if (this.tryConstantAssignmentConversion(
                l,
                optionalConstantValue, // constantValue
                targetType             // targetType
            )) return;
        }

        this.compileError("Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"", l.getLocation());
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2) on a constant value.
     */
    private Object assignmentConversion(
        Locatable l,
        Object    value,
        IClass    targetType
    ) throws CompileException {
        Object result = null;

        if (targetType == IClass.BOOLEAN) {
            if (value instanceof Boolean) result = value;
        } else
        if (targetType == this.iClassLoader.STRING) {
            if (value instanceof String) result = value;
        } else
        if (targetType == IClass.BYTE) {
            if (value instanceof Byte) {
                result = value;
            } else
            if (value instanceof Short || value instanceof Integer) {
                int x = ((Number) value).intValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) result = new Byte((byte) x);
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Byte.MIN_VALUE && x <= Byte.MAX_VALUE) result = new Byte((byte) x);
            }
        } else
        if (targetType == IClass.SHORT) {
            if (value instanceof Byte) {
                result = new Short(((Number) value).shortValue());
            } else
            if (value instanceof Short) {
                result = value;
            } else
            if (value instanceof Character) {
                int x = ((Character) value).charValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) result = new Short((short) x);
            } else
            if (value instanceof Integer) {
                int x = ((Integer) value).intValue();
                if (x >= Short.MIN_VALUE && x <= Short.MAX_VALUE) result = new Short((short) x);
            }
        } else
        if (targetType == IClass.CHAR) {
            if (value instanceof Short) {
                result = value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                int x = ((Number) value).intValue();
                if (x >= Character.MIN_VALUE && x <= Character.MAX_VALUE) result = new Character((char) x);
            }
        } else
        if (targetType == IClass.INT) {
            if (value instanceof Integer) {
                result = value;
            } else
            if (value instanceof Byte || value instanceof Short) {
                result = new Integer(((Number) value).intValue());
            } else
            if (value instanceof Character) {
                result = new Integer(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.LONG) {
            if (value instanceof Long) {
                result = value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                result = new Long(((Number) value).longValue());
            } else
            if (value instanceof Character) {
                result = new Long(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.FLOAT) {
            if (value instanceof Float) {
                result = value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                result = new Float(((Number) value).floatValue());
            } else
            if (value instanceof Character) {
                result = new Float(((Character) value).charValue());
            }
        } else
        if (targetType == IClass.DOUBLE) {
            if (value instanceof Double) {
                result = value;
            } else
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof Float) {
                result = new Double(((Number) value).doubleValue());
            } else
            if (value instanceof Character) {
                result = new Double(((Character) value).charValue());
            }
        } else
        if (value == Java.Rvalue.CONSTANT_VALUE_NULL && !targetType.isPrimitive()) {
            result = value;
        }
        if (result == null) this.compileError("Cannot convert constant of type \"" + value.getClass().getName() + "\" to type \"" + targetType.toString() + "\"", l.getLocation());
        return result;
    }

    /**
     * Implements "unary numeric promotion" (JLS3 5.6.1)
     *
     * @return The promoted type.
     */
    private IClass unaryNumericPromotion(Locatable l, IClass type) throws CompileException {
        type = this.convertToPrimitiveNumericType(l, type);

        IClass promotedType = this.unaryNumericPromotionType(l, type);

        this.numericPromotion(l, type, promotedType);
        return promotedType;
    }

    private void reverseUnaryNumericPromotion(Locatable l, IClass sourceType, IClass targetType) throws CompileException {
        IClass unboxedType = this.isUnboxingConvertible(targetType);
        IClass pt = unboxedType != null ? unboxedType : targetType;
        if (
            !this.tryIdentityConversion(sourceType, pt) &&
            !this.tryNarrowingPrimitiveConversion(
                l,          // locatable
                sourceType, // sourceType
                pt          // targetType
            )
        ) throw new RuntimeException("SNO: reverse unary numeric promotion failed");
        if (unboxedType != null) this.boxingConversion(l, unboxedType, targetType);
    }

    /**
     * If the given type is a primitive type, return that type.
     * If the given type is a primitive wrapper class, unbox the operand on top of the operand
     * stack and return the primitive type.
     * Otherwise, issue a compile error.
     */
    private IClass convertToPrimitiveNumericType(Locatable l, IClass type) throws CompileException {
        if (type.isPrimitiveNumeric()) return type;
        IClass unboxedType = this.isUnboxingConvertible(type);
        if (unboxedType != null) {
            this.unboxingConversion(l, type, unboxedType);
            return unboxedType;
        }
        this.compileError("Object of type \"" + type.toString() + "\" cannot be converted to a numeric type", l.getLocation());
        return type;
    }

    private void numericPromotion(Locatable l, IClass sourceType, IClass targetType) {
        if (
            !this.tryIdentityConversion(sourceType, targetType) &&
            !this.tryWideningPrimitiveConversion(
                l,          // locatable
                sourceType, // sourceType
                targetType  // targetType
            )
        ) throw new RuntimeException("SNO: Conversion failed");
    }

    private IClass unaryNumericPromotionType(Locatable l, IClass type) throws CompileException {
        if (!type.isPrimitiveNumeric()) this.compileError("Unary numeric promotion not possible on non-numeric-primitive type \"" + type + "\"", l.getLocation());

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
    private IClass binaryNumericPromotion(
        Locatable            locatable,
        IClass               type1,
        CodeContext.Inserter convertInserter1,
        IClass               type2
    ) throws CompileException {
        return this.binaryNumericPromotion(locatable, type1, convertInserter1, type2, this.codeContext.currentInserter());
    }
    /**
     * Implements "binary numeric promotion" (5.6.2)
     *
     * @return The promoted type.
     */
    private IClass binaryNumericPromotion(
        Locatable            l,
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
                l,
                c1 != null ? c1 : type1,
                c2 != null ? c2 : type2
            );
        }

        if (convertInserter1 != null) {
            this.codeContext.pushInserter(convertInserter1);
            try {
                this.numericPromotion(l, this.convertToPrimitiveNumericType(l, type1), promotedType);
            } finally {
                this.codeContext.popInserter();
            }
        }

        if (convertInserter2 != null) {
            this.codeContext.pushInserter(convertInserter2);
            try {
                this.numericPromotion(l, this.convertToPrimitiveNumericType(l, type2), promotedType);
            } finally {
                this.codeContext.popInserter();
            }
        }

        return promotedType;
    }

    private IClass binaryNumericPromotionType(
        Java.Locatable locatable,
        IClass    type1,
        IClass    type2
    ) throws CompileException {
        if (
            !type1.isPrimitiveNumeric() ||
            !type2.isPrimitiveNumeric()
        ) this.compileError("Binary numeric promotion not possible on types \"" + type1 + "\" and \"" + type2 + "\"", locatable.getLocation());

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
    private boolean isIdentityConvertible(
        IClass sourceType,
        IClass targetType
    ) {
        return sourceType == targetType;
    }

    /**
     * Implements "identity conversion" (5.1.1).
     *
     * @return Whether the conversion was possible
     */
    private boolean tryIdentityConversion(
        IClass sourceType,
        IClass targetType
    ) {
        return sourceType == targetType;
    }
    
    private boolean isWideningPrimitiveConvertible(
        IClass sourceType,
        IClass targetType
    ) {
        return UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        ) != null;
    }

    /**
     * Implements "widening primitive conversion" (5.1.2).
     *
     * @return Whether the conversion succeeded
     */
    private boolean tryWideningPrimitiveConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType
    ) {
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            this.writeOpcodes(l, opcodes);
            return true;
        }
        return false;
    }
    private static final HashMap PRIMITIVE_WIDENING_CONVERSIONS = new HashMap();
    static { UnitCompiler.fillConversionMap(new Object[] {
        new byte[0],
        Descriptor.BYTE_  + Descriptor.SHORT_,

        Descriptor.BYTE_  + Descriptor.INT_,
        Descriptor.SHORT_ + Descriptor.INT_,
        Descriptor.CHAR_  + Descriptor.INT_,

        new byte[] { Opcode.I2L },
        Descriptor.BYTE_  + Descriptor.LONG_,
        Descriptor.SHORT_ + Descriptor.LONG_,
        Descriptor.CHAR_  + Descriptor.LONG_,
        Descriptor.INT_   + Descriptor.LONG_,

        new byte[] { Opcode.I2F },
        Descriptor.BYTE_  + Descriptor.FLOAT_,
        Descriptor.SHORT_ + Descriptor.FLOAT_,
        Descriptor.CHAR_  + Descriptor.FLOAT_,
        Descriptor.INT_   + Descriptor.FLOAT_,

        new byte[] { Opcode.L2F },
        Descriptor.LONG_  + Descriptor.FLOAT_,

        new byte[] { Opcode.I2D },
        Descriptor.BYTE_  + Descriptor.DOUBLE_,
        Descriptor.SHORT_ + Descriptor.DOUBLE_,
        Descriptor.CHAR_  + Descriptor.DOUBLE_,
        Descriptor.INT_   + Descriptor.DOUBLE_,

        new byte[] { Opcode.L2D },
        Descriptor.LONG_  + Descriptor.DOUBLE_,

        new byte[] { Opcode.F2D },
        Descriptor.FLOAT_ + Descriptor.DOUBLE_,
    }, UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS); }
    private static void fillConversionMap(Object[] array, HashMap map) {
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
    private boolean isWideningReferenceConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        if (
            targetType.isPrimitive() ||
            sourceType == targetType
        ) return false;

        return targetType.isAssignableFrom(sourceType);
    }

    /**
     * Performs "widening reference conversion" (5.1.4) if possible.
     *
     * @return Whether the conversion was possible
     */
    private boolean tryWideningReferenceConversion(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        if (
            targetType.isPrimitive() ||
            sourceType == targetType
        ) return false;
        
        return targetType.isAssignableFrom(sourceType);
    }
    
    /**
     * Check whether "narrowing primitive conversion" (JLS 5.1.3) is possible.
     */
    private boolean isNarrowingPrimitiveConvertible(
        IClass sourceType,
        IClass targetType
    ) {
        return UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.containsKey(sourceType.getDescriptor() + targetType.getDescriptor());
    }

    /**
     * Implements "narrowing primitive conversion" (JLS 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private boolean tryNarrowingPrimitiveConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType
    ) {
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            this.writeOpcodes(l, opcodes);
            return true;
        }
        return false;
    }

    private static final HashMap PRIMITIVE_NARROWING_CONVERSIONS = new HashMap();
    static { UnitCompiler.fillConversionMap(new Object[] {
        new byte[0],
        Descriptor.BYTE_ + Descriptor.CHAR_,
        Descriptor.SHORT_ + Descriptor.CHAR_,
        Descriptor.CHAR_ + Descriptor.SHORT_,

        new byte[] { Opcode.I2B },
        Descriptor.SHORT_ + Descriptor.BYTE_,
        Descriptor.CHAR_ + Descriptor.BYTE_,
        Descriptor.INT_ + Descriptor.BYTE_,

        new byte[] { Opcode.I2S },
        Descriptor.INT_ + Descriptor.SHORT_,
        Descriptor.INT_ + Descriptor.CHAR_,

        new byte[] { Opcode.L2I, Opcode.I2B },
        Descriptor.LONG_ + Descriptor.BYTE_,

        new byte[] { Opcode.L2I, Opcode.I2S },
        Descriptor.LONG_ + Descriptor.SHORT_,
        Descriptor.LONG_ + Descriptor.CHAR_,

        new byte[] { Opcode.L2I },
        Descriptor.LONG_ + Descriptor.INT_,

        new byte[] { Opcode.F2I, Opcode.I2B },
        Descriptor.FLOAT_ + Descriptor.BYTE_,

        new byte[] { Opcode.F2I, Opcode.I2S },
        Descriptor.FLOAT_ + Descriptor.SHORT_,
        Descriptor.FLOAT_ + Descriptor.CHAR_,

        new byte[] { Opcode.F2I },
        Descriptor.FLOAT_ + Descriptor.INT_,

        new byte[] { Opcode.F2L },
        Descriptor.FLOAT_ + Descriptor.LONG_,

        new byte[] { Opcode.D2I, Opcode.I2B },
        Descriptor.DOUBLE_ + Descriptor.BYTE_,

        new byte[] { Opcode.D2I, Opcode.I2S },
        Descriptor.DOUBLE_ + Descriptor.SHORT_,
        Descriptor.DOUBLE_ + Descriptor.CHAR_,

        new byte[] { Opcode.D2I },
        Descriptor.DOUBLE_ + Descriptor.INT_,

        new byte[] { Opcode.D2L },
        Descriptor.DOUBLE_ + Descriptor.LONG_,

        new byte[] { Opcode.D2F },
        Descriptor.DOUBLE_ + Descriptor.FLOAT_,
    }, UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS); }

    /**
     * Check if "constant assignment conversion" (JLS 5.2, paragraph 1) is possible.
     * @param constantValue The constant value that is to be converted
     * @param targetType The type to convert to
     */
    private boolean tryConstantAssignmentConversion(
        Locatable l,
        Object    constantValue,
        IClass    targetType
    ) throws CompileException {
        if (UnitCompiler.DEBUG) System.out.println("isConstantPrimitiveAssignmentConvertible(" + constantValue + ", " + targetType + ")");

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

        if (targetType == IClass.BYTE ) return cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE;
        if (targetType == IClass.SHORT) return cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE;
        if (targetType == IClass.CHAR ) return cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE;

        IClassLoader icl = this.iClassLoader;
        if (targetType == icl.BYTE && cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE) {
            this.boxingConversion(l, IClass.BYTE, targetType);
            return true;
        }
        if (targetType == icl.SHORT && cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE) {
            this.boxingConversion(l, IClass.SHORT, targetType);
            return true;
        }
        if (targetType == icl.CHARACTER && cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE) {
            this.boxingConversion(l, IClass.CHAR, targetType);
            return true;
        }

        return false;
    }

    /**
     * Check whether "narrowing reference conversion" (JLS 5.1.5) is possible.
     */
    private boolean isNarrowingReferenceConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        if (sourceType.isPrimitive()) return false;
        if (sourceType == targetType) return false;

        // 5.1.5.1
        if (sourceType.isAssignableFrom(targetType)) return true;

        // 5.1.5.2
        if (
            targetType.isInterface() &&
            !sourceType.isFinal() &&
            !targetType.isAssignableFrom(sourceType)
        ) return true;

        // 5.1.5.3
        if (
            sourceType == this.iClassLoader.OBJECT &&
            targetType.isArray()
        ) return true;

        // 5.1.5.4
        if (
            sourceType == this.iClassLoader.OBJECT &&
            targetType.isInterface()
        ) return true;

        // 5.1.5.5
        if (
            sourceType.isInterface() &&
            !targetType.isFinal()
        ) return true;

        // 5.1.5.6
        if (
            sourceType.isInterface() &&
            targetType.isFinal() &&
            sourceType.isAssignableFrom(targetType)
        ) return true;

        // 5.1.5.7
        // TODO: Check for redefinition of methods with same signature but different return type.
        if (
            sourceType.isInterface() &&
            targetType.isInterface() &&
            !targetType.isAssignableFrom(sourceType)
        ) return true;

        // 5.1.5.8
        if (sourceType.isArray() && targetType.isArray()) {
            IClass st = sourceType.getComponentType();
            IClass tt = targetType.getComponentType();
            if (
                this.isNarrowingPrimitiveConvertible(st, tt) ||
                this.isNarrowingReferenceConvertible(st, tt)
            ) return true;
        }

        return false;
    }

    /**
     * Implements "narrowing reference conversion" (5.1.5).
     *
     * @return Whether the conversion succeeded
     */
    private boolean tryNarrowingReferenceConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType
    ) throws CompileException {
        if (!this.isNarrowingReferenceConvertible(sourceType, targetType)) return false;

        this.writeOpcode(l, Opcode.CHECKCAST);
        this.writeConstantClassInfo(targetType.getDescriptor());
        return true;
    }

    /*
     * @return the boxed type or <code>null</code>
     */
    private IClass isBoxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == IClass.BOOLEAN) return icl.BOOLEAN;
        if (sourceType == IClass.BYTE   ) return icl.BYTE;
        if (sourceType == IClass.CHAR   ) return icl.CHARACTER;
        if (sourceType == IClass.SHORT  ) return icl.SHORT;
        if (sourceType == IClass.INT    ) return icl.INTEGER;
        if (sourceType == IClass.LONG   ) return icl.LONG;
        if (sourceType == IClass.FLOAT  ) return icl.FLOAT;
        if (sourceType == IClass.DOUBLE ) return icl.DOUBLE;
        return null;
    }

    private boolean tryBoxingConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType
    ) throws CompileException {
        if (this.isBoxingConvertible(sourceType) == targetType) {
            this.boxingConversion(l, sourceType, targetType);
            return true;
        }
        return false;
    }

    /**
     * @param sourceType a primitive type (except VOID)
     * @param targetType the corresponding wrapper type
     */
    private void boxingConversion(Locatable l, IClass sourceType, IClass targetType) throws CompileException {

        // In some pre-1.5 JDKs, only some wrapper classes have the static "Target.valueOf(source)" method.
        if (targetType.hasIMethod("valueOf", new IClass[] { sourceType })) {
            this.writeOpcode(l, Opcode.INVOKESTATIC);
            this.writeConstantMethodrefInfo(
                targetType.getDescriptor(),
                "valueOf",                                         // classFD
                '(' + sourceType.getDescriptor() + ')' + targetType.getDescriptor() // methodFD
            );
            return;
        }
        // new Target(source)
        this.writeOpcode(l, Opcode.NEW);
        this.writeConstantClassInfo(targetType.getDescriptor());
        if (Descriptor.hasSize2(sourceType.getDescriptor())) {
            this.writeOpcode(l, Opcode.DUP_X2);
            this.writeOpcode(l, Opcode.DUP_X2);
            this.writeOpcode(l, Opcode.POP);
        } else
        {
            this.writeOpcode(l, Opcode.DUP_X1);
            this.writeOpcode(l, Opcode.SWAP);
        }
        this.writeOpcode(l, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            targetType.getDescriptor(),
            "<init>",                               // classFD
            '(' + sourceType.getDescriptor() + ')' + Descriptor.VOID_ // methodMD
        );
    }

    /*
     * @return the unboxed type or <code>null</code>
     */
    private IClass isUnboxingConvertible(IClass sourceType) {
        IClassLoader icl = this.iClassLoader;
        if (sourceType == icl.BOOLEAN  ) return IClass.BOOLEAN;
        if (sourceType == icl.BYTE     ) return IClass.BYTE;
        if (sourceType == icl.CHARACTER) return IClass.CHAR;
        if (sourceType == icl.SHORT    ) return IClass.SHORT;
        if (sourceType == icl.INTEGER  ) return IClass.INT;
        if (sourceType == icl.LONG     ) return IClass.LONG;
        if (sourceType == icl.FLOAT    ) return IClass.FLOAT;
        if (sourceType == icl.DOUBLE   ) return IClass.DOUBLE;
        return null;
    }
    
    private boolean tryUnboxingConversion(
        Locatable l,
        IClass    sourceType,
        IClass    targetType
    ) {
        if (this.isUnboxingConvertible(sourceType) == targetType) {
            this.unboxingConversion(l, sourceType, targetType);
            return true;
        }
        return false;
    }

    /**
     * @param targetType a primitive type (except VOID)
     * @param sourceType the corresponding wrapper type
     */
    private void unboxingConversion(Locatable l, IClass sourceType, IClass targetType) {

        // "source.targetValue()"
        this.writeOpcode(l, Opcode.INVOKEVIRTUAL);
        this.writeConstantMethodrefInfo(
            sourceType.getDescriptor(),
            targetType.toString() + "Value",       // classFD
            "()" + targetType.getDescriptor() // methodFD
        );
    }
    
    /**
     * Attempt to load an {@link IClass} by fully-qualified name
     * @param identifiers
     * @return <code>null</code> if a class with the given name could not be loaded
     */
    private IClass loadFullyQualifiedClass(String[] identifiers) throws CompileException {

        // Compose the descriptor (like "La/b/c;") and remember the positions of the slashes
        // (2 and 4).
        int[] slashes = new int[identifiers.length - 1];
        StringBuffer sb = new StringBuffer("L");
        for (int i = 0;; ++i) {
            sb.append(identifiers[i]);
            if (i == identifiers.length - 1) break;
            slashes[i] = sb.length();
            sb.append('/');
        }
        sb.append(';');

        // Attempt to load the IClass and replace dots with dollar signs, i.e.:
        // La/b/c; La/b$c; La$b$c;
        for (int j = slashes.length - 1;; --j) {
            IClass result;
            try {
                result = this.iClassLoader.loadIClass(sb.toString());
            } catch (ClassNotFoundException ex) {
                if (ex.getException() instanceof CompileException) throw (CompileException) ex.getException();
                throw new CompileException(sb.toString(), null, ex);
            }
            if (result != null) return result;
            if (j < 0) break;
            sb.setCharAt(slashes[j], '$');
        }
        return null;
    }

    // Load the value of a local variable onto the stack and return its type.
    private IClass load(
        Locatable          l,
        Java.LocalVariable localVariable
    ) {
        this.load(
            l,
            localVariable.type,
            localVariable.localVariableArrayIndex
        );
        return localVariable.type;
    }
    private void load(
        Locatable l,
        IClass    type,
        int       index
    ) {
        if (index <= 3) {
            this.writeOpcode(l, Opcode.ILOAD_0 + 4 * this.ilfda(type) + index);
        } else
        if (index <= 255) {
            this.writeOpcode(l, Opcode.ILOAD + this.ilfda(type));
            this.writeByte(index);
        } else
        {
            this.writeOpcode(l, Opcode.WIDE);
            this.writeOpcode(l, Opcode.ILOAD + this.ilfda(type));
            this.writeShort(index);
        }
    }

    /**
     * Assign stack top value to the given local variable. (Assignment conversion takes effect.)
     * If <copde>optionalConstantValue</code> is not <code>null</code>, then the top stack value
     * is a constant value with that type and value, and a narrowing primitive conversion as
     * described in JLS 5.2 is applied.
     */
    private void store(
        Locatable          l,
        IClass             valueType,
        Java.LocalVariable localVariable
    ) {
        this.store(
            l,                                    // l
            localVariable.type,                   // lvType
            localVariable.localVariableArrayIndex // lvIndex
        );
    }
    private void store(
        Locatable l,
        IClass    lvType,
        short     lvIndex
    ) {
        if (lvIndex <= 3) {
            this.writeOpcode(l, Opcode.ISTORE_0 + 4 * this.ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            this.writeOpcode(l, Opcode.ISTORE + this.ilfda(lvType));
            this.writeByte(lvIndex);
        } else
        {
            this.writeOpcode(l, Opcode.WIDE);
            this.writeOpcode(l, Opcode.ISTORE + this.ilfda(lvType));
            this.writeShort(lvIndex);
        }
    }

    private void dup(Locatable l, int n) {
        switch (n) {

        case 0:
            ;
            break;

        case 1:
            this.writeOpcode(l, Opcode.DUP);
            break;

        case 2:
            this.writeOpcode(l, Opcode.DUP2);
            break;

        default:
            throw new RuntimeException("dup(" + n + ")");
        }
    }
    private void dupx(
        Locatable l,
        IClass    type,
        int       x
    ) {
        if (x < 0 || x > 2) throw new RuntimeException("SNO: x has value " + x);
        int dup  = Opcode.DUP  + x;
        int dup2 = Opcode.DUP2 + x;
        this.writeOpcode(l, (
            type == IClass.LONG || type == IClass.DOUBLE ?
            dup2 :
            dup
        ));
    }

    private void pop(Locatable l, IClass type) {
        if (type == IClass.VOID) return;
        this.writeOpcode(l, (
            type == IClass.LONG || type == IClass.DOUBLE ?
            Opcode.POP2 :
            Opcode.POP
        ));
    }

    static int ilfd(IClass t) {
        if (
            t == IClass.BYTE ||
            t == IClass.CHAR ||
            t == IClass.INT ||
            t == IClass.SHORT ||
            t == IClass.BOOLEAN
        ) return 0;
        if (t == IClass.LONG  ) return 1;
        if (t == IClass.FLOAT ) return 2;
        if (t == IClass.DOUBLE) return 3;
        throw new RuntimeException("Unexpected type \"" + t + "\"");
    }
    static int ilfd(
        IClass t,
        int    opcodeInt,
        int    opcodeLong,
        int    opcodeFloat,
        int    opcodeDouble
    ) {
        if (
            t == IClass.BYTE ||
            t == IClass.CHAR ||
            t == IClass.INT ||
            t == IClass.SHORT ||
            t == IClass.BOOLEAN
        ) return opcodeInt;
        if (t == IClass.LONG  ) return opcodeLong;
        if (t == IClass.FLOAT ) return opcodeFloat;
        if (t == IClass.DOUBLE) return opcodeDouble;
        throw new RuntimeException("Unexpected type \"" + t + "\"");
    }
    private int ilfda(IClass t) {
        return !t.isPrimitive() ? 4 : UnitCompiler.ilfd(t);
    }
    static int ilfdabcs(IClass t) {
        if (t == IClass.INT   ) return 0;
        if (t == IClass.LONG  ) return 1;
        if (t == IClass.FLOAT ) return 2;
        if (t == IClass.DOUBLE) return 3;
        if (!t.isPrimitive()  ) return 4;
        if (t == IClass.BOOLEAN || t == IClass.BYTE) return 5;
        if (t == IClass.CHAR  ) return 6;
        if (t == IClass.SHORT ) return 7;
        throw new RuntimeException("Unexpected type \"" + t + "\"");
    }

    /**
     * Find a named field in the given {@link IClass}.
     * Honor superclasses and interfaces. See JLS 8.3.
     * @return <code>null</code> if no field is found
     */
    private IClass.IField findIField(
        IClass   iClass,
        String   name,
        Location location
    ) throws CompileException {

        // Search for a field with the given name in the current class.
        IClass.IField f = iClass.getDeclaredIField(name);
        if(f != null) return f;

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
                if (f != null) throw new CompileException("Access to field \"" + name + "\" is ambiguous - both \"" + f.getDeclaringIClass() + "\" and \"" + f2.getDeclaringIClass() + "\" declare it", location);
                f = f2;
            }
        }
        return f;
    }

    /**
     * Find a named type in the given {@link IClass}.
     * Honor superclasses, interfaces and enclosing type declarations.
     * @return <code>null</code> if no type with the given name is found
     */
    private IClass findMemberType(
        IClass   iClass,
        String   name,
        Location location
    ) throws CompileException {
        IClass[] types = iClass.findMemberType(name);
        if (types.length == 0) return null;
        if (types.length == 1) return types[0];

        StringBuffer sb = new StringBuffer("Type \"" + name + "\" is ambiguous: " + types[0].toString());
        for (int i = 1; i < types.length; ++i) sb.append(" vs. ").append(types[i].toString());
        this.compileError(sb.toString(), location);
        return types[0];
    }

    /**
     * Find one class or interface declared in this compilation unit by name.
     *
     * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner".
     * @return <code>null</code> if a class with that name is not declared in this compilation unit
     */
    public IClass findClass(String className) {

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
        Java.TypeDeclaration td = this.compilationUnit.getPackageMemberTypeDeclaration(st.nextToken());
        if (td == null) return null;
        while (st.hasMoreTokens()) {
            td = td.getMemberTypeDeclaration(st.nextToken());
            if (td == null) return null;
        }
        return this.resolve((Java.AbstractTypeDeclaration) td);
    }

    /**
     * Equivalent to {@link #compileError(String, Location)} with a
     * <code>null</code> location argument.
     */
    private void compileError(String message) throws CompileException {
        this.compileError(message, null);
    }

    /**
     * Issue a compile error with the given message. This is done through the
     * {@link ErrorHandler} that was installed through
     * {@link #setCompileErrorHandler(ErrorHandler)}. Such a handler typically throws
     * a {@link CompileException}, but it may as well decide to return normally. Consequently,
     * the calling code must be prepared that {@link #compileError(String, Location)}
     * returns normally, and must attempt to continue compiling.
     *
     * @param message The message to report
     * @param optionalLocation The location to report
     */
    private void compileError(String message, Location optionalLocation) throws CompileException {
        ++this.compileErrorCount;
        if (this.optionalCompileErrorHandler != null) {
            this.optionalCompileErrorHandler.handleError(message, optionalLocation);
        } else {
            throw new CompileException(message, optionalLocation);
        }
    }

    /**
     * Issues a warning with the given message an location an returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(WarningHandler)}.
     * <p>
     * The <code>handle</code> argument qulifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     *
     * @param handle
     * @param message
     * @param optionalLocation
     */
    private void warning(String handle, String message, Location optionalLocation) {
        if (this.optionalWarningHandler != null) this.optionalWarningHandler.handleWarning(handle, message, optionalLocation);
    }

    /**
     * Interface type for {@link UnitCompiler#setCompileErrorHandler}.
     */
    public interface ErrorHandler {
        void handleError(String message, Location optionalLocation) throws CompileException;
    }

    /**
     * By default, {@link CompileException}s are thrown on compile errors, but an application
     * my install its own (thread-local) {@link ErrorHandler}.
     * <p>
     * Be aware that a single problem during compilation often causes a bunch of compile errors,
     * so a good {@link ErrorHandler} counts errors and throws a {@link CompileException} when
     * a limit is reached.
     * <p>
     * If the given {@link ErrorHandler} does not throw {@link CompileException}s, then
     * {@link #compileUnit(EnumeratorSet)} will throw one when the compilation of the unit
     * is finished, and errors had occurred. In other words: The {@link ErrorHandler} may
     * throw a {@link CompileException} or not, but {@link #compileUnit(EnumeratorSet)} will
     * definitely throw a {@link CompileException} if one or more compile errors have
     * occurred.
     *
     * @param optionalCompileErrorHandler <code>null</code> to restore the default behavior (throwing a {@link CompileException}
     */
    public void setCompileErrorHandler(ErrorHandler optionalCompileErrorHandler) {
        this.optionalCompileErrorHandler = optionalCompileErrorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a custom
     * {@link WarningHandler}.
     *
     * @param optionalWarningHandler <code>null</code> to indicate that no warnings be issued
     */
    public void setWarningHandler(WarningHandler optionalWarningHandler) {
        this.optionalWarningHandler = optionalWarningHandler;
    }

    private CodeContext getCodeContext() {
        CodeContext res = this.codeContext;
        if (res == null) throw new RuntimeException("S.N.O.: Null CodeContext");
        return res;
    }

    private CodeContext replaceCodeContext(CodeContext newCodeContext) {
        CodeContext oldCodeContext = this.codeContext;
        this.codeContext = newCodeContext;
        return oldCodeContext;
    }

    private CodeContext createDummyCodeContext() {
        return new CodeContext(this.getCodeContext().getClassFile());
    }

    private void writeByte(int v) {
        this.codeContext.write((short) -1, (byte) v);
    }
    private void writeShort(int v) {
        this.codeContext.write((short) -1, (byte) (v >> 8), (byte) v);
    }
    private void writeInt(int v) {
        this.codeContext.write((short) -1, (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v);
    }

    private void writeOpcode(Java.Locatable l, int opcode) {
        this.codeContext.write(l.getLocation().getLineNumber(), (byte) opcode);
    }
    private void writeOpcodes(Java.Locatable l, byte[] opcodes) {
        this.codeContext.write(l.getLocation().getLineNumber(), opcodes);
    }
    private void writeBranch(Java.Locatable l, int opcode, final CodeContext.Offset dst) {
        this.codeContext.writeBranch(l.getLocation().getLineNumber(), opcode, dst);
    }
    private void writeOffset(CodeContext.Offset src, final CodeContext.Offset dst) {
        this.codeContext.writeOffset((short) -1, src, dst);
    }

    // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

    private void writeConstantClassInfo(String descriptor) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantClassInfo(descriptor));
    }
    private void writeConstantFieldrefInfo(String classFD, String fieldName, String fieldFD) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantFieldrefInfo(classFD, fieldName, fieldFD));
    }
    private void writeConstantMethodrefInfo(String classFD, String methodName, String methodMD) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantMethodrefInfo(classFD, methodName, methodMD));
    }
    private void writeConstantInterfaceMethodrefInfo(String classFD, String methodName, String methodMD) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantInterfaceMethodrefInfo(classFD, methodName, methodMD));
    }
/* UNUSED
    private void writeConstantStringInfo(String value) {
        this.codeContext.writeShort((short) -1, this.addConstantStringInfo(value));
    }
*/
    private short addConstantStringInfo(String value) {
        return this.codeContext.getClassFile().addConstantStringInfo(value);
    }
/* UNUSED
    private void writeConstantIntegerInfo(int value) {
        this.codeContext.writeShort((short) -1, this.addConstantIntegerInfo(value));
    }
*/
    private short addConstantIntegerInfo(int value) {
        return this.codeContext.getClassFile().addConstantIntegerInfo(value);
    }
/* UNUSED
    private void writeConstantFloatInfo(float value) {
        this.codeContext.writeShort((short) -1, this.addConstantFloatInfo(value));
    }
*/
    private short addConstantFloatInfo(float value) {
        return this.codeContext.getClassFile().addConstantFloatInfo(value);
    }
    private void writeConstantLongInfo(long value) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantLongInfo(value));
    }
    private void writeConstantDoubleInfo(double value) {
        CodeContext ca = this.codeContext;
        ca.writeShort((short) -1, ca.getClassFile().addConstantDoubleInfo(value));
    }

    public CodeContext.Offset getWhereToBreak(Java.BreakableStatement bs) {
        if (bs.whereToBreak == null) {
            bs.whereToBreak = this.codeContext.new Offset();
        }
        return bs.whereToBreak;
    }

    private Java.TypeBodyDeclaration getDeclaringTypeBodyDeclaration(Java.QualifiedThisReference qtr) throws CompileException {
        if (qtr.declaringTypeBodyDeclaration == null) {

            // Compile error if in static function context.
            Java.Scope s;
            for (s = qtr.getEnclosingBlockStatement(); !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
            qtr.declaringTypeBodyDeclaration = (Java.TypeBodyDeclaration) s;
            if (qtr.declaringTypeBodyDeclaration.isStatic()) this.compileError("No current instance available in static method", qtr.getLocation());

            // Determine declaring type.
            qtr.declaringClass = (Java.ClassDeclaration) qtr.declaringTypeBodyDeclaration.getDeclaringType();
        }
        return qtr.declaringTypeBodyDeclaration;
    }

    private Java.ClassDeclaration getDeclaringClass(Java.QualifiedThisReference qtr) throws CompileException {
        if (qtr.declaringClass == null) {
            this.getDeclaringTypeBodyDeclaration(qtr);
        }
        return qtr.declaringClass;
    }

    private void referenceThis(Locatable l) {
        this.writeOpcode(l, Opcode.ALOAD_0);
    }

    /**
     * Expects "dimExprCount" values of type "integer" on the operand stack.
     * Creates an array of "dimExprCount" + "dims" dimensions of
     * "componentType".
     *
     * @return The type of the created array
     */
    private IClass newArray(
        Locatable l,
        int       dimExprCount,
        int       dims,
        IClass    componentType
    ) {
        if (dimExprCount == 1 && dims == 0 && componentType.isPrimitive()) {

            // "new <primitive>[<n>]"
            this.writeOpcode(l, Opcode.NEWARRAY);
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
            return componentType.getArrayIClass(this.iClassLoader.OBJECT);
        }

        if (dimExprCount == 1) {
            IClass at = componentType.getArrayIClass(dims, this.iClassLoader.OBJECT);

            // "new <class-or-interface>[<n>]"
            // "new <anything>[<n>][]..."
            this.writeOpcode(l, Opcode.ANEWARRAY);
            this.writeConstantClassInfo(at.getDescriptor());
            return at.getArrayIClass(this.iClassLoader.OBJECT);
        } else {
            IClass at = componentType.getArrayIClass(dimExprCount + dims, this.iClassLoader.OBJECT);

            // "new <anything>[]..."
            // "new <anything>[<n>][<m>]..."
            // "new <anything>[<n>][<m>]...[]..."
            this.writeOpcode(l, Opcode.MULTIANEWARRAY);
            this.writeConstantClassInfo(at.getDescriptor());
            this.writeByte(dimExprCount);
            return at;
        }
    }

    /**
     * Short-hand implementation of {@link IClass.IField} that implements a
     * non-constant, non-static, package-accessible field.
     */
    public static class SimpleIField extends IClass.IField {
        private final String name;
        private final IClass type;

        public SimpleIField(
            IClass declaringIClass,
            String name,
            IClass type
        ) {
            declaringIClass.super();
            this.name = name;
            this.type = type;
        }
        public Object  getConstantValue() { return null; }
        public String  getName()          { return this.name; }
        public IClass  getType()          { return this.type; }
        public boolean isStatic()         { return false; }
        public Access  getAccess()        { return Access.DEFAULT; }
    };

    private static Access modifiers2Access(short modifiers) {
        return (
            (modifiers & Mod.PUBLIC   ) != 0 ? Access.PUBLIC    :
            (modifiers & Mod.PROTECTED) != 0 ? Access.PROTECTED :
            (modifiers & Mod.PRIVATE  ) != 0 ? Access.PRIVATE   :
            Access.DEFAULT
        );
    }

    private static String last(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        return sa[sa.length - 1];
    }

    private static String[] allButLast(String[] sa) {
        if (sa.length == 0) throw new IllegalArgumentException("SNO: Empty string array");
        String[] tmp = new String[sa.length - 1];
        System.arraycopy(sa, 0, tmp, 0, tmp.length);
        return tmp;
    }

    private static String[] concat(String[] sa, String s) {
        String[] tmp = new String[sa.length + 1];
        System.arraycopy(sa, 0, tmp, 0, sa.length);
        tmp[sa.length] = s;
        return tmp;
    }
    
    // Used to write byte code while compiling one constructor/method.
    private CodeContext codeContext = null;

    // Used for elaborate compile error handling.
    private ErrorHandler optionalCompileErrorHandler = null;
    private int          compileErrorCount = 0;

    // Used for elaborate warning handling.
    private WarningHandler optionalWarningHandler = null;

    public final Java.CompilationUnit compilationUnit;

    private final IClassLoader iClassLoader;
    private final boolean      isStringBuilderAvailable;
    private List               generatedClassFiles;
    private EnumeratorSet      debuggingInformation;

    private final Map        singleTypeImports     = new HashMap();   // String simpleTypeName => String[] fullyQualifiedTypeName
    private final Collection typeImportsOnDemand;                     // String[] package
    private final Map        singleStaticImports   = new HashMap();   // String staticMemberName => IField, List of IMethod, or IClass
    private final Collection staticImportsOnDemand = new ArrayList(); // IClass
}
