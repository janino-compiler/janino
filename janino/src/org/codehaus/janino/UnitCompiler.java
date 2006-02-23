
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

import org.codehaus.janino.util.*;
import org.codehaus.janino.util.enumerator.EnumeratorSet;

/**
 * This class actually implements the Java<sup>TM</sup> compiler. It is
 * associated with exactly one compilation unit which it compiles.
 */
public class UnitCompiler {
    private static final boolean DEBUG = false;

    public UnitCompiler(
        Java.CompilationUnit compilationUnit,
        IClassLoader         iClassLoader
    ) {
        this.compilationUnit = compilationUnit;
        this.iClassLoader    = iClassLoader;
    }

    /**
     * Generates an array of {@link ClassFile} objects which represent the classes and
     * interfaces defined in the compilation unit.
     */
    public ClassFile[] compileUnit(
        EnumeratorSet debuggingInformation
    ) throws CompileException {
        this.generatedClassFiles  = new ArrayList();
        this.debuggingInformation = debuggingInformation;

        for (Iterator it = this.compilationUnit.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
            UnitCompiler.this.compile((Java.PackageMemberTypeDeclaration) it.next());
        }

        List l = this.generatedClassFiles;
        return (ClassFile[]) l.toArray(new ClassFile[l.size()]);
    }

    // ------------ TypeDeclaration.compile() -------------

    private void compile(Java.TypeDeclaration td) throws CompileException {
        Visitor.TypeDeclarationVisitor tdv = new Visitor.TypeDeclarationVisitor() {
            public void visitAnonymousClassDeclaration        (Java.AnonymousClassDeclaration acd)          { try { UnitCompiler.this.compile2(acd ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalClassDeclaration            (Java.LocalClassDeclaration lcd)              { try { UnitCompiler.this.compile2(lcd ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitPackageMemberClassDeclaration    (Java.PackageMemberClassDeclaration pmcd)     { try { UnitCompiler.this.compile2(pmcd); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitMemberInterfaceDeclaration       (Java.MemberInterfaceDeclaration mid)         { try { UnitCompiler.this.compile2(mid ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitPackageMemberInterfaceDeclaration(Java.PackageMemberInterfaceDeclaration pmid) { try { UnitCompiler.this.compile2(pmid); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitMemberClassDeclaration           (Java.MemberClassDeclaration mcd)             { try { UnitCompiler.this.compile2(mcd ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            td.accept(tdv);
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    public void compile2(Java.ClassDeclaration cd) throws CompileException {

        // Determine implemented interfaces.
        IClass[] iis = this.resolve(cd).getInterfaces();
        String[] interfaceDescriptors = new String[iis.length];
        for (int i = 0; i < iis.length; ++i) interfaceDescriptors[i] = iis[i].getDescriptor();

        // Create "ClassFile" object.
        ClassFile cf = new ClassFile(
            (short) (cd.modifiers | Mod.SUPER),               // accessFlags
            this.resolve(cd).getDescriptor(),                 // thisClassFD
            this.resolve(cd).getSuperclass().getDescriptor(), // superClassFD
            interfaceDescriptors                              // interfaceFDs
        );

        // Add InnerClasses attribute entry for this class declaration.
        if (cd.enclosingScope instanceof Java.CompilationUnit) {
            ;
        } else
        if (cd.enclosingScope instanceof Java.Block) {
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(cd).getDescriptor());
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
        if (cd.enclosingScope instanceof Java.AbstractTypeDeclaration) {
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(cd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(((Java.AbstractTypeDeclaration) cd.enclosingScope)).getDescriptor());
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
                } else if (this instanceof Java.NamedTypeDeclaration) {
                    sourceFileName = ((Java.NamedTypeDeclaration) this).getName() + ".java";
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
            Java.MethodDeclarator classInitializationMethod = new Java.MethodDeclarator(
                cd.getLocation(),            // location
                cd,                          // declaringType
                null,                        // optionalDocComment
                (short) (                    // modifiers
                    Mod.STATIC |
                    Mod.PUBLIC
                ),
                new Java.BasicType(          // type
                    cd.getLocation(),
                    Java.BasicType.VOID
                ),
                "<clinit>",                  // name
                new Java.FormalParameter[0], // formalParameters
                new Java.ReferenceType[0]    // thrownExceptions
            );
            Java.Block b = new Java.Block(
                cd.getLocation(),
                classInitializationMethod
            );
            for (Iterator it = cd.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
                if (tbd.isStatic()) b.addStatement((Java.BlockStatement) tbd);
            }
            classInitializationMethod.setBody(b);

            // Create class initialization method iff there is any initialization code.
            if (this.generatesCode(b)) this.compile(classInitializationMethod, cf);
        }

        // Compile declared methods.
        // (As a side effects, this fills the "syntheticFields" map.)
        for (int i = 0; i < cd.declaredMethods.size(); ++i) {
            this.compile(((Java.MethodDeclarator) cd.declaredMethods.get(i)), cf);
        }

        // Compile declared constructors.
        int declaredMethodCount = cd.declaredMethods.size();
        {
            int syntheticFieldCount = cd.syntheticFields.size();
            Java.ConstructorDeclarator[] cds = cd.getConstructors();
            for (int i = 0; i < cds.length; ++i) {
                this.compile(cds[i], cf);
                if (syntheticFieldCount != cd.syntheticFields.size()) throw new RuntimeException("SNO: Compilation of constructor \"" + cds[i] + "\" (" + cds[i].getLocation() +") added synthetic fields!?");
            }
        }

        // As a side effect of compiling methods and constructors, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly. Compile these.
        for (int i = declaredMethodCount; i < cd.declaredMethods.size(); ++i) {
            this.compile(((Java.MethodDeclarator) cd.declaredMethods.get(i)), cf);
        }

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
                (short) 0,                   // modifiers,
                f.getName(),                 // fieldName,
                f.getType().getDescriptor(), // fieldTypeFD,
                null                         // optionalConstantValue
            );
        }

        // Member types.
        for (Iterator it = cd.getMemberTypeDeclarations().iterator(); it.hasNext();) {
            Java.AbstractTypeDeclaration atd = ((Java.AbstractTypeDeclaration) it.next());
            this.compile(atd);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(atd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(cd).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(((Java.MemberTypeDeclaration) atd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex, // innerClassInfoIndex
                outerClassInfoIndex, // outerClassInfoIndex
                innerNameIndex,      // innerNameIndex
                atd.modifiers        // innerClassAccessFlags
            ));
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
                ocv = this.getConstantValue(vd.optionalInitializer);
                if (ocv == Java.Rvalue.CONSTANT_VALUE_NULL) ocv = null;
            }

            ClassFile.FieldInfo fi = cf.addFieldInfo(
                fd.modifiers,                       // modifiers
                vd.name,                            // fieldName
                this.getType(type).getDescriptor(), // fieldTypeFD
                ocv                                 // optionalConstantValue
            );

            // Add "Deprecated" attribute (JVMS 4.7.10)
            if (fd.hasDeprecatedDocTag()) {
                fi.addAttribute(new ClassFile.DeprecatedAttribute(cf.addConstantUtf8Info("Deprecated")));
            }
        }
    }

    public void compile2(Java.AnonymousClassDeclaration acd) throws CompileException {
        Java.Scope s = acd.getEnclosingScope();
        for (; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
        final Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) s;

        // Define a synthetic "this$..." field for a non-static function context. (It is
        // very difficult to tell whether the anonymous constructor will require its
        // enclosing instance.)
        if (!tbd.isStatic()) {
            final int nesting = UnitCompiler.getOuterClasses(acd).size();
            acd.defineSyntheticField(new SimpleIField(
                this.resolve(acd),
                "this$" + (nesting - 2),
                UnitCompiler.this.resolve(tbd.getDeclaringType())
            ));
        }
        this.compile2((Java.ClassDeclaration) acd);
    }
    public void compile2(Java.LocalClassDeclaration lcd) throws CompileException {

        // Make this local class visible in the block scope.
        ((Java.Block) lcd.getEnclosingScope()).declaredLocalClasses.put(lcd.getName(), this);

        // Define a synthetic "this$..." field for the local class if the enclosing method is non-static.
        {
            List ocs = UnitCompiler.getOuterClasses(lcd);
            final int nesting = ocs.size();
            if (nesting >= 2) {
                final IClass enclosingInstanceType = this.resolve((Java.AbstractTypeDeclaration) ocs.get(1));
                lcd.defineSyntheticField(new SimpleIField(
                    this.resolve(lcd),
                    "this$" + (nesting - 2),
                    enclosingInstanceType
                ));
            }
        }

        this.compile((Java.NamedClassDeclaration) lcd);
    }
    public void compile2(final Java.MemberClassDeclaration mcd) throws CompileException {

        // Define a synthetic "this$..." field for a non-static member class.
        if ((mcd.modifiers & Mod.STATIC) == 0) {
            final int nesting = UnitCompiler.getOuterClasses(mcd).size();
            mcd.defineSyntheticField(new SimpleIField(
                this.resolve(mcd),
                "this$" + (nesting - 2),
                UnitCompiler.this.resolve(mcd.getDeclaringType())
            ));
        }
        this.compile2((Java.NamedClassDeclaration) mcd);
    }
    public void compile2(Java.InterfaceDeclaration id) throws CompileException {

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
            this.resolve(id).getDescriptor(), // thisClassFD
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
            Java.MethodDeclarator interfaceInitializationMethod = new Java.MethodDeclarator(
                id.getLocation(),            // location
                id,                          // declaringType
                null,                        // optionalDocComment
                (short) (                    // modifiers
                    Mod.STATIC |
                    Mod.PUBLIC
                ),
                new Java.BasicType(          // type
                    id.getLocation(),
                    Java.BasicType.VOID
                ),
                "<clinit>",                  // name
                new Java.FormalParameter[0], // formalParameters
                new Java.ReferenceType[0]    // thrownExcaptions
            );
            Java.Block b = new Java.Block(
                id.getLocation(),
                interfaceInitializationMethod
            );
            b.addStatements(id.constantDeclarations);
            interfaceInitializationMethod.setBody(b);

            // Create interface initialization method iff there is any initialization code.
            if (this.generatesCode(b)) this.compile(interfaceInitializationMethod, cf);
        }

        // Methods.
        // Notice that as a side effect of compiling methods, synthetic "class-dollar"
        // methods (which implement class literals) are generated on-the fly. Hence, we
        // must not use an Iterator here.
        for (int i = 0; i < id.declaredMethods.size(); ++i) {
            this.compile(((Java.MethodDeclarator) id.declaredMethods.get(i)), cf);
        }

        // Class variables.
        for (int i = 0; i < id.constantDeclarations.size(); ++i) {
            Java.BlockStatement bs = (Java.BlockStatement) id.constantDeclarations.get(i);
            if (!(bs instanceof Java.FieldDeclaration)) continue;
            this.addFields((Java.FieldDeclaration) bs, cf);
        }

        // Member types.
        for (Iterator it = id.getMemberTypeDeclarations().iterator(); it.hasNext();) {
            Java.AbstractTypeDeclaration atd = ((Java.AbstractTypeDeclaration) it.next());
            this.compile(atd);

            // Add InnerClasses attribute entry for member type declaration.
            short innerClassInfoIndex = cf.addConstantClassInfo(this.resolve(atd).getDescriptor());
            short outerClassInfoIndex = cf.addConstantClassInfo(this.resolve(id).getDescriptor());
            short innerNameIndex      = cf.addConstantUtf8Info(((Java.MemberTypeDeclaration) atd).getName());
            cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                innerClassInfoIndex, // innerClassInfoIndex
                outerClassInfoIndex, // outerClassInfoIndex
                innerNameIndex,      // innerNameIndex
                id.modifiers         // innerClassAccessFlags
            ));
        }

        // Add the generated class file to a thread-local store.
        this.generatedClassFiles.add(cf);
    }

    /**
     * Checks whether this statement can complete normally (JLS2 14.20)
     */
    private boolean canCompleteNormally(Java.BlockStatement bs) throws CompileException {
        CodeContext savedCodeContext = this.replaceCodeContext(this.createDummyCodeContext());
        try {
            return this.compile(bs);
        } finally {
            this.replaceCodeContext(savedCodeContext);
        }
    }

    /**
     * @return <tt>false</tt> if this statement cannot complete normally (JLS2
     * 14.20)
     */
    private boolean compile(Java.BlockStatement bs) throws CompileException {
        final boolean[] res = new boolean[1];
        Visitor.BlockStatementVisitor bsv = new Visitor.BlockStatementVisitor() {
            public void visitInitializer                      (Java.Initializer                       i   ) { try { res[0] = UnitCompiler.this.compile2(i   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldDeclaration                 (Java.FieldDeclaration                  fd  ) { try { res[0] = UnitCompiler.this.compile2(fd  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLabeledStatement                 (Java.LabeledStatement                  ls  ) { try { res[0] = UnitCompiler.this.compile2(ls  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBlock                            (Java.Block                             b   ) { try { res[0] = UnitCompiler.this.compile2(b   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitExpressionStatement              (Java.ExpressionStatement               es  ) { try { res[0] = UnitCompiler.this.compile2(es  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitIfStatement                      (Java.IfStatement                       is  ) { try { res[0] = UnitCompiler.this.compile2(is  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitForStatement                     (Java.ForStatement                      fs  ) { try { res[0] = UnitCompiler.this.compile2(fs  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitWhileStatement                   (Java.WhileStatement                    ws  ) { try { res[0] = UnitCompiler.this.compile2(ws  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitTryStatement                     (Java.TryStatement                      ts  ) { try { res[0] = UnitCompiler.this.compile2(ts  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSwitchStatement                  (Java.SwitchStatement                   ss  ) { try { res[0] = UnitCompiler.this.compile2(ss  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSynchronizedStatement            (Java.SynchronizedStatement             ss  ) { try { res[0] = UnitCompiler.this.compile2(ss  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitDoStatement                      (Java.DoStatement                       ds  ) { try { res[0] = UnitCompiler.this.compile2(ds  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableDeclarationStatement(Java.LocalVariableDeclarationStatement lvds) { try { res[0] = UnitCompiler.this.compile2(lvds); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitReturnStatement                  (Java.ReturnStatement                   rs  ) { try { res[0] = UnitCompiler.this.compile2(rs  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitThrowStatement                   (Java.ThrowStatement                    ts  ) { try { res[0] = UnitCompiler.this.compile2(ts  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBreakStatement                   (Java.BreakStatement                    bs  ) { try { res[0] = UnitCompiler.this.compile2(bs  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitContinueStatement                (Java.ContinueStatement                 cs  ) { try { res[0] = UnitCompiler.this.compile2(cs  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitEmptyStatement                   (Java.EmptyStatement                    es  ) {       res[0] = UnitCompiler.this.compile2(es  );                                                                }
            public void visitLocalClassDeclarationStatement   (Java.LocalClassDeclarationStatement    lcds) { try { res[0] = UnitCompiler.this.compile2(lcds); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ boolean compile2(Java.Initializer i) throws CompileException {
        return this.compile(i.block);
    }
    /*private*/ boolean compile2(Java.Block b) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {
            boolean previousStatementCanCompleteNormally = true;
            b.keepCompiling = true;
            for (int i = 0; b.keepCompiling && i < b.statements.size(); ++i) {
                Java.BlockStatement bs = (Java.BlockStatement) b.statements.get(i);
                if (!previousStatementCanCompleteNormally) {
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
    /*private*/ boolean compile2(Java.DoStatement ds) throws CompileException {
        if (Boolean.TRUE.equals(this.getConstantValue(ds.condition))) {
            return this.compileUnconditionalLoop(ds, ds.body, null);
        }

        ds.whereToContinue = this.codeContext.new Offset();
        ds.bodyHasContinue = false;

        CodeContext.Offset bodyOffset = this.codeContext.newOffset();

        // Compile body.
        if (!this.compile(ds.body) && !ds.bodyHasContinue) this.compileError("\"do\" statement never tests its condition", ds.getLocation());

        // Compile condition.
        ds.whereToContinue.set();
        this.compileBoolean(ds.condition, bodyOffset, Java.Rvalue.JUMP_IF_TRUE);

        if (ds.whereToBreak != null) ds.whereToBreak.set();

        return true;
    }
    /*private*/ boolean compile2(Java.ForStatement fs) throws CompileException {
        this.codeContext.saveLocalVariables();
        try {

            // Compile init.
            if (fs.optionalInit != null) this.compile(fs.optionalInit);

            if (
                fs.optionalCondition == null
                || Boolean.TRUE.equals(this.getConstantValue(fs.optionalCondition))
            ) return this.compileUnconditionalLoop(fs, fs.body, fs.optionalUpdate);

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
                    this.compileError("For update is unreachable", fs.getLocation());
                    return true;
                }

                for (int i = 0; i < fs.optionalUpdate.length; ++i) {
                    this.compile(fs.optionalUpdate[i]);
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
    /*private*/ boolean compile2(Java.WhileStatement ws) throws CompileException {
        if (Boolean.TRUE.equals(this.getConstantValue(ws.condition))) {
            return this.compileUnconditionalLoop(ws, ws.body, null);
        }

        ws.whereToContinue = this.codeContext.new Offset();
        ws.bodyHasContinue = false;
        this.writeBranch(ws, Opcode.GOTO, ws.whereToContinue);

        // Compile body.
        CodeContext.Offset bodyOffset = this.codeContext.newOffset();
        boolean bodyCCN = this.compile(ws.body);

        // Compile condition.
        if (bodyCCN || ws.bodyHasContinue) {
            ws.whereToContinue.set();
            this.compileBoolean(ws.condition, bodyOffset, Java.Rvalue.JUMP_IF_TRUE);
        }

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
            this.compileError("Loop update is unreachable", update[0].getLocation());
            return cs.whereToBreak != null;
        }
        for (int i = 0; i < update.length; ++i) this.compile(update[i]);
        this.writeBranch(cs, Opcode.GOTO, bodyOffset);

        if (cs.whereToBreak == null) return false;
        cs.whereToBreak.set();
        return true;
    }

    /*private*/ final boolean compile2(Java.LabeledStatement ls) throws CompileException {
        boolean canCompleteNormally = this.compile(ls.body);
        if (ls.whereToBreak != null) {
            ls.whereToBreak.set();
            canCompleteNormally = true;
        }
        return canCompleteNormally;
    }
    /*private*/ boolean compile2(Java.SwitchStatement ss) throws CompileException {

        // Compute condition.
        IClass switchExpressionType = this.compileGetValue(ss.condition);
        this.assignmentConversion(
            (Java.Located) ss,    // located
            switchExpressionType, // sourceType
            IClass.INT,           // targetType
            null                  // optionalConstantValue
        );

        // Prepare the map of case labels to code offsets.
        TreeMap                caseLabelMap = new TreeMap(); // Integer => Offset
        CodeContext.Offset   defaultLabelOffset = null;
        CodeContext.Offset[] sbsgOffsets = new CodeContext.Offset[ss.sbsgs.size()];
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            Java.SwitchBlockStatementGroup sbsg = (Java.SwitchBlockStatementGroup) ss.sbsgs.get(i);
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
                    (Java.Located) ss,    // located
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
                    throw new RuntimeException();
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
        if (!caseLabelMap.isEmpty() && 2 * caseLabelMap.size() >= (
            ((Integer) caseLabelMap.lastKey()).intValue() -
            ((Integer) caseLabelMap.firstKey()).intValue()
        )) {
            int low = ((Integer) caseLabelMap.firstKey()).intValue();
            int high = ((Integer) caseLabelMap.lastKey()).intValue();

            this.writeByte(ss, Opcode.TABLESWITCH);
            new Java.Padder(this.codeContext).set();
            this.writeOffset(ss, switchOffset, defaultLabelOffset);
            this.writeInt(ss, low);
            this.writeInt(ss, high);
            Iterator si = caseLabelMap.entrySet().iterator();
            int cur = low;
            while (si.hasNext()) {
                Map.Entry me = (Map.Entry) si.next();
                int val = ((Integer) me.getKey()).intValue();
                while (cur < val) {
                    this.writeOffset(ss, switchOffset, defaultLabelOffset);
                    ++cur;
                }
                this.writeOffset(ss, switchOffset, (CodeContext.Offset) me.getValue());
                ++cur;
            }
        } else {
            this.writeOpcode(ss, Opcode.LOOKUPSWITCH);
            new Java.Padder(this.codeContext).set();
            this.writeOffset(ss, switchOffset, defaultLabelOffset);
            this.writeInt(ss, caseLabelMap.size());
            Iterator si = caseLabelMap.entrySet().iterator();
            while (si.hasNext()) {
                Map.Entry me = (Map.Entry) si.next();
                this.writeInt(ss, ((Integer) me.getKey()).intValue());
                this.writeOffset(ss, switchOffset, (CodeContext.Offset) me.getValue());
            }
        }

        // Compile statement groups.
        boolean canCompleteNormally = true;
        for (int i = 0; i < ss.sbsgs.size(); ++i) {
            Java.SwitchBlockStatementGroup sbsg = (Java.SwitchBlockStatementGroup) ss.sbsgs.get(i);
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
    /*private*/ boolean compile2(Java.BreakStatement bs) throws CompileException {

        // Find the broken statement.
        Java.BreakableStatement brokenStatement = null;
        if (bs.optionalLabel == null) {
            for (
                Java.Scope s = bs.enclosingScope;
                s instanceof Java.Statement;
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
                Java.Scope s = bs.enclosingScope;
                s instanceof Java.Statement;
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
            bs.enclosingScope,              // from
            brokenStatement.enclosingScope, // to
            null                            // optionalStackValueType
        );
        this.writeBranch(bs, Opcode.GOTO, this.getWhereToBreak(brokenStatement));
        return false;
    }
    /*private*/ boolean compile2(Java.ContinueStatement cs) throws CompileException {

        // Find the continued statement.
        Java.ContinuableStatement continuedStatement = null;
        if (cs.optionalLabel == null) {
            for (
                Java.Scope s = cs.enclosingScope;
                s instanceof Java.Statement;
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
                Java.Scope s = cs.enclosingScope;
                s instanceof Java.Statement;
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
            cs.enclosingScope, // from
            continuedStatement.enclosingScope,   // to
            null                 // optionalStackValueType
        );
        this.writeBranch(cs, Opcode.GOTO, continuedStatement.whereToContinue);
        return false;
    }
    /*private*/ boolean compile2(Java.EmptyStatement es) {
        return true;
    }
    /*private*/ boolean compile2(Java.ExpressionStatement ee) throws CompileException {
        this.compile(ee.rvalue);
        return true;
    }
    /*private*/ boolean compile2(Java.FieldDeclaration fd) throws CompileException {
        for (int i = 0; i < fd.variableDeclarators.length; ++i) {
            Java.VariableDeclarator vd = fd.variableDeclarators[i];

            Java.Rvalue initializer = this.getNonConstantFinalInitializer(fd, vd);
            if (initializer == null) continue;

            if ((fd.modifiers & Mod.STATIC) == 0) {
                this.writeOpcode(fd, Opcode.ALOAD_0);
            }
            IClass initializerType = this.compileGetValue(initializer);
            IClass fieldType = this.getType(fd.type);
            fieldType = this.getArrayType(fieldType, vd.brackets);
            this.assignmentConversion(
                (Java.Located) fd,                 // located
                initializerType,                   // sourceType
                fieldType,                         // destinationType
                this.getConstantValue(initializer) // optionalConstantValue
            );

            // No need to check accessibility here.
            ;

            if ((fd.modifiers & Mod.STATIC) != 0) {
                this.writeOpcode(fd, Opcode.PUTSTATIC);
            } else {
                this.writeOpcode(fd, Opcode.PUTFIELD);
            }
            this.writeConstantFieldrefInfo(
                fd,
                this.resolve(fd.declaringType).getDescriptor(), // classFD
                vd.name,                                        // fieldName
                fieldType.getDescriptor()                       // fieldFD
            );
        }
        return true;
    }
    /*private*/ boolean compile2(Java.IfStatement is) throws CompileException {
        Object cv = this.getConstantValue(is.condition);
        Java.BlockStatement es = is.optionalElseStatement != null ? is.optionalElseStatement : new Java.EmptyStatement(is.thenStatement.getLocation(), is.thenStatement.getEnclosingScope());
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
            boolean ssccn = this.compile(seeingStatement);
            if (ssccn) return true;

            // Hm... the "seeing statement" cannot complete normally. So, in
            // order to determine whether the IF statement can complete
            // normally, we need to check whether the "blind statement" can
            // complete normally.
            if (!this.canCompleteNormally(blindStatement)) return false;

            // We have a very complicated case here: The blind statement can complete
            // normally, but the seeing statement can't. This makes the following
            // code physically unreachable, but JLS2 14.20 says that this should not
            // be considered an error.
            // Calling "followingStatementsAreDead()" on the enclosing Block
            // keeps it from generating unreachable code.
            Java.Scope s = is.getEnclosingScope();
            if (s instanceof Java.Block) ((Java.Block) s).followingStatementsAreDead();
            return false;
        }

        // Non-constant condition.
        if (this.generatesCode(is.thenStatement)) {
            if (this.generatesCode(es)) {

                // if (expr) stmt else stmt
                CodeContext.Offset eso = this.codeContext.new Offset();
                CodeContext.Offset end = this.codeContext.new Offset();
                this.compileBoolean(is.condition, eso, Java.Rvalue.JUMP_IF_FALSE);
                boolean tsccn = this.compile(is.thenStatement);
                if (tsccn) this.writeBranch(Opcode.GOTO, end);
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
                this.pop((Java.Located) is, conditionType);
                return true;
            }
        }
    }
    /*private*/ boolean compile2(Java.LocalClassDeclarationStatement lcds) throws CompileException {
        this.compile(lcds.lcd);
        return true;
    }
    /*private*/ boolean compile2(Java.LocalVariableDeclarationStatement lvds) throws CompileException {
        if ((lvds.modifiers & ~Mod.FINAL) != 0) this.compileError("The only allowed modifier in local variable declarations is \"final\"", lvds.getLocation());

        for (int j = 0; j < lvds.variableDeclarators.length; ++j) {
            Java.VariableDeclarator vd = lvds.variableDeclarators[j];

            // Determine variable type.
            Java.Type variableType = lvds.type;
            for (int k = 0; k < vd.brackets; ++k) variableType = new Java.ArrayType(variableType);

            IClass lhsType = this.getType(variableType);
            Java.LocalVariable lv = this.defineLocalVariable(
                lvds.declaringBlock,
                (Java.Located) lvds,               // located
                (lvds.modifiers & Mod.FINAL) != 0, // finaL
                lhsType,                           // type
                vd.name                            // name
            );

            if (vd.optionalInitializer != null) {
                Java.Rvalue rhs = vd.optionalInitializer;
                this.assignmentConversion(
                    (Java.Located) lvds,       // located
                    this.compileGetValue(rhs), // sourceType
                    lhsType,                   // targetType
                    this.getConstantValue(rhs) // optionalConstantValue
                );
                this.store(
                    (Java.Located) lvds, // located
                    lhsType,             // valueType
                    lv                   // localVariable
                );
            }
        }
        return true;
    }
    /*private*/ boolean compile2(Java.ReturnStatement rs) throws CompileException {

        // Determine enclosing block, function and compilation Unit.
        Java.FunctionDeclarator enclosingFunction = null;
        {
            Java.Scope s = rs.enclosingScope;
            for (s = s.getEnclosingScope(); s instanceof Java.Statement; s = s.getEnclosingScope());
            enclosingFunction = (Java.FunctionDeclarator) s;
        }

        IClass returnType = this.getReturnType(enclosingFunction);
        if (returnType == IClass.VOID) {
            if (rs.optionalReturnValue != null) this.compileError("Method must not return a value", rs.getLocation());
            this.leaveStatements(
                rs.enclosingScope, // from
                enclosingFunction, // to
                null               // optionalStackValueType
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
            (Java.Located) rs,                            // located
            type,                                         // sourceType
            returnType,                                   // targetType
            this.getConstantValue(rs.optionalReturnValue) // optionalConstantValue
        );

        this.leaveStatements(
            rs.enclosingScope, // from
            enclosingFunction, // to
            returnType         // optionalStackValueType
        );
        this.writeOpcode(rs, Opcode.IRETURN + this.ilfda(returnType));
        return false;
    }
    /*private*/ boolean compile2(Java.SynchronizedStatement ss) throws CompileException {

        // Evaluate monitor object expression.
        if (!this.iClassLoader.OBJECT.isAssignableFrom(this.compileGetValue(ss.expression))) this.compileError("Monitor object of \"synchronized\" statement is not a subclass of \"Object\"", ss.getLocation());

        this.codeContext.saveLocalVariables();
        boolean canCompleteNormally = false;
        try {

            // Allocate a local variable for the monitor object.
            ss.monitorLvIndex = this.codeContext.allocateLocalVariable((short) 1);

            // Store the monitor object.
            this.writeOpcode(ss, Opcode.DUP);
            this.store((Java.Located) ss, this.iClassLoader.OBJECT, ss.monitorLvIndex);

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
    /*private*/ boolean compile2(Java.ThrowStatement ts) throws CompileException {
        IClass expressionType = this.compileGetValue(ts.expression);
        this.checkThrownException(
            (Java.Located) ts, // located
            expressionType,    // type
            ts.enclosingScope  // scope
        );
        this.writeOpcode(ts, Opcode.ATHROW);
        return false;
    }
    /*private*/ boolean compile2(Java.TryStatement ts) throws CompileException {
        if (ts.optionalFinally != null) ts.finallyOffset = this.codeContext.new Offset();

        CodeContext.Offset beginningOfBody = this.codeContext.newOffset();
        CodeContext.Offset afterStatement = this.codeContext.new Offset();

        // Allocate a future LV that will be used to preserve the "leave stack".
        this.codeContext.saveLocalVariables();
        ts.stackValueLvIndex = this.codeContext.allocateLocalVariable((short) 2);
        this.codeContext.restoreLocalVariables();

        boolean canCompleteNormally = this.compile(ts.body);
        CodeContext.Offset afterBody = this.codeContext.newOffset();
        if (canCompleteNormally) {
            this.writeBranch(ts, Opcode.GOTO, afterStatement);
        }

        this.codeContext.saveLocalVariables();
        try {

            // Local variable for exception object.
            // Notice: Must be same size as "this.stackValueLvIndex".
            short exceptionObjectLvIndex = this.codeContext.allocateLocalVariable((short) 2);

            if (beginningOfBody.offset != afterBody.offset) { // Avoid zero-length exception table entries.
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
                        (Java.Located) ts,     // located
                        caughtExceptionType,   // lvType
                        exceptionObjectLvIndex // lvIndex
                    );
    
                    // Kludge: Treat the exception variable like a local
                    // variable of the catch clause body.
                    cc.body.localVariables.put(
                        cc.caughtException.name,
                        new Java.LocalVariable(
                            false,                        // finaL
                            caughtExceptionType,          // type
                            exceptionObjectLvIndex        // localVariableIndex
                        )
                    );
    
                    if (this.compile(cc.body)) {
                        canCompleteNormally = true;
                        if (
                            i < ts.catchClauses.size() - 1 ||
                            ts.optionalFinally != null
                        ) this.writeBranch(ts, Opcode.GOTO, afterStatement);
                    }
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

                // Store exception object in local variable.
                this.store(
                    (Java.Located) ts,        // located
                    this.iClassLoader.OBJECT, // valueType
                    exceptionObjectLvIndex    // localVariableIndex
                );
                this.writeBranch(ts, Opcode.JSR, ts.finallyOffset);
                this.load(
                    (Java.Located) ts,        // located
                    this.iClassLoader.OBJECT, // valueType
                    exceptionObjectLvIndex    // localVariableIndex
                );
                this.writeOpcode(ts, Opcode.ATHROW);

                // Compile the "finally" body.
                ts.finallyOffset.set();
                short pcLVIndex = this.codeContext.allocateLocalVariable((short) 1);
                this.store(
                    (Java.Located) ts,        // located
                    this.iClassLoader.OBJECT, // valueType
                    pcLVIndex                 // localVariableIndex
                );
                if (this.compile(ts.optionalFinally)) {
                    this.writeOpcode(ts, Opcode.RET);
                    this.writeByte(ts, pcLVIndex);
                }
            }

            afterStatement.set();
            if (canCompleteNormally) this.leave(ts, null);
        } finally {
            this.codeContext.restoreLocalVariables();
        }

        return canCompleteNormally;
    }

    // ------------ FunctionDeclarator.compile() -------------

    private void compile(Java.FunctionDeclarator fd, final ClassFile classFile) throws CompileException {
        ClassFile.MethodInfo mi = classFile.addMethodInfo(
            fd.modifiers,                         // accessFlags
            fd.name,                              // name
            this.toIInvocable(fd).getDescriptor() // methodMD
        );

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
                for (Iterator it = constructorDeclarator.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    constructorDeclarator.syntheticParameters.put(
                        sf.getName(),
                        new Java.LocalVariable(
                            true,                                                                       // finaL
                            sf.getType(),                                                               // type
                            this.codeContext.allocateLocalVariable(Descriptor.size(sf.getDescriptor())) // localVariableArrayIndex
                        )
                    );
                }
            }

            // Add function parameters.
            for (int i = 0; i < fd.formalParameters.length; ++i) {
                Java.FormalParameter fp = fd.formalParameters[i];
                if (fd.parameters.containsKey(fp.name)) this.compileError("Redefinition of formal parameter \"" + fp.name + "\"", fd.getLocation());
                IClass fpt = this.getType(fp.type);
                fd.parameters.put(fp.name, new Java.LocalVariable(
                    fp.finaL,
                    fpt,
                    this.codeContext.allocateLocalVariable(Descriptor.size(fpt.getDescriptor()))
                ));
            }

            // Compile the function preamble.
            if (fd instanceof Java.ConstructorDeclarator) {
                Java.ConstructorDeclarator cd = (Java.ConstructorDeclarator) fd;
                if (cd.optionalExplicitConstructorInvocation != null) {
                    this.compile(cd.optionalExplicitConstructorInvocation);
                    if (cd.optionalExplicitConstructorInvocation instanceof Java.SuperConstructorInvocation) {
                        this.assignSyntheticParametersToSyntheticFields(cd);
                        this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                    }
                } else {

                    // Determine qualification for superconstructor invocation.
                    Java.QualifiedThisReference qualification = null;
                    IClass outerClassOfSuperclass = this.resolve(cd.declaringClass).getSuperclass().getOuterIClass();
                    if (outerClassOfSuperclass != null) {
                        qualification = new Java.QualifiedThisReference(
                            cd.getLocation(),        // location
                            cd.declaringClass,       // declaringClass
                            cd,                      // declaringTypeBodyDeclaration
                            outerClassOfSuperclass   // targetIClass
                        );
                    }

                    // Invoke the superconstructor.
                    this.compile(new Java.SuperConstructorInvocation(
                        cd.getLocation(),  // location
                        cd.declaringClass, // declaringClass
                        cd,                // declaringConstructor
                        qualification,     // optionalQualification
                        new Java.Rvalue[0] // arguments
                    ));
                    this.assignSyntheticParametersToSyntheticFields(cd);
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers(cd);
                }
            }

            // Compile the function body.
            boolean canCompleteNormally = this.compile(fd.optionalBody);
            if (canCompleteNormally) {
                if (this.getReturnType(fd) != IClass.VOID) this.compileError("Method must return a value", fd.getLocation());
                this.writeOpcode(fd, Opcode.RETURN);
            }
        } finally {
            this.replaceCodeContext(savedCodeContext);
        }

        // Fix up.
        codeContext.fixUp();

        // Relocate.
        codeContext.relocate();

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
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) { try { UnitCompiler.this.compile2(ai  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayLength               (Java.ArrayLength                al  ) { try { UnitCompiler.this.compile2(al  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitAssignment                (Java.Assignment                 a   ) { try { UnitCompiler.this.compile2(a   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { UnitCompiler.this.compile2(uo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { try { UnitCompiler.this.compile2(bo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCast                      (Java.Cast                       c   ) { try { UnitCompiler.this.compile2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) { try { UnitCompiler.this.compile2(cl  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) { try { UnitCompiler.this.compile2(ce  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConstantValue             (Java.ConstantValue              cv  ) { try { UnitCompiler.this.compile2(cv  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCrement                   (Java.Crement                    c   ) { try { UnitCompiler.this.compile2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitInstanceof                (Java.Instanceof                 io  ) { try { UnitCompiler.this.compile2(io  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) { try { UnitCompiler.this.compile2(mi  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) { try { UnitCompiler.this.compile2(smi ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLiteral                   (Java.Literal                    l   ) { try { UnitCompiler.this.compile2(l   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) { try { UnitCompiler.this.compile2(naci); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewArray                  (Java.NewArray                   na  ) { try { UnitCompiler.this.compile2(na  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) { try { UnitCompiler.this.compile2(nci ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) { try { UnitCompiler.this.compile2(pa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) { try { UnitCompiler.this.compile2(qtr ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitThisReference             (Java.ThisReference              tr  ) { try { UnitCompiler.this.compile2(tr  ); } catch (CompileException e) { throw new TunnelException(e); } }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) { try { UnitCompiler.this.compile2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) { try { UnitCompiler.this.compile2(aae ); } catch (CompileException e) { throw new TunnelException(e); } };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) { try { UnitCompiler.this.compile2(fa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) { try { UnitCompiler.this.compile2(fae ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) { try { UnitCompiler.this.compile2(lva ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { UnitCompiler.this.compile2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ void compile2(Java.Rvalue rv) throws CompileException {
        this.pop((Java.Located) rv, this.compileGetValue(rv));
    }
    /*private*/ void compile2(Java.Assignment a) throws CompileException {
        if (a.operator == "=") {
            this.compileContext(a.lhs);
            this.assignmentConversion(
                (Java.Located) a,             // located
                this.compileGetValue(a.rhs), // sourceType
                this.getType(a.lhs),         // targetType
                this.getConstantValue(a.rhs) // optionalConstantValue
            );
            this.compileSet(a.lhs);
            return;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCS = this.compileContext(a.lhs);
        this.dup((Java.Located) a, lhsCS);
        IClass lhsType = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            (Java.Located) a,        // located
            lhsType,                 // lhsType
            a.operator.substring(    // operator
                0,
                a.operator.length() - 1
            ).intern(), // <= IMPORTANT!
            a.rhs                    // rhs
        );
        // Convert the result to LHS type (JLS2 15.26.2).
        if (
            !this.tryIdentityConversion(resultType, lhsType) &&
            !this.tryNarrowingPrimitiveConversion(
                (Java.Located) a,   // located
                resultType,         // sourceType
                lhsType             // destinationType
            )
        ) throw new RuntimeException();
        this.compileSet(a.lhs);
    }
    /*private*/ void compile2(Java.Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        Java.LocalVariable lv = this.isIntLV(c);
        if (lv != null) {
            this.writeOpcode(c, Opcode.IINC);
            this.writeByte(c, lv.localVariableArrayIndex);
            this.writeByte(c, c.operator == "++" ? 1 : -1);
            return;
        }

        int cs = this.compileContext(c.operand);
        this.dup((Java.Located) c, cs);
        IClass type = this.compileGet(c.operand);
        IClass promotedType = this.unaryNumericPromotion((Java.Located) c, type);
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

        if (
            !this.tryIdentityConversion(promotedType, type) &&
            !this.tryNarrowingPrimitiveConversion(
                (Java.Located) c,   // located
                promotedType,       // sourceType
                type                // targetType
            )
        ) throw new RuntimeException();
        this.compileSet(c.operand);
    }
    /*private*/ void compile2(Java.ParenthesizedExpression pe) throws CompileException {
        this.compile(pe.value);
    }

    private void compile(Java.ConstructorInvocation ci) throws CompileException {
        Visitor.ConstructorInvocationVisitor civ = new Visitor.ConstructorInvocationVisitor() {
            public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci) { try { UnitCompiler.this.compile2(aci); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSuperConstructorInvocation    (Java.SuperConstructorInvocation     sci) { try { UnitCompiler.this.compile2(sci); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            ci.accept(civ);
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ void compile2(Java.AlternateConstructorInvocation aci) throws CompileException {
        this.writeOpcode(aci, Opcode.ALOAD_0);
        this.invokeConstructor(
            (Java.Located) aci,                    // located
            (Java.Scope) aci.declaringConstructor, // scope
            (Java.Rvalue) null,                    // optionalEnclosingInstance
            this.resolve(aci.declaringClass),      // targetClass
            aci.arguments                          // arguments
        );
    }
    /*private*/ void compile2(Java.SuperConstructorInvocation sci) throws CompileException {
        this.writeOpcode(sci, Opcode.ALOAD_0);

        IClass superclass = this.resolve(sci.declaringClass).getSuperclass();

        Java.Rvalue optionalEnclosingInstance;
        if (sci.optionalQualification != null) {
            optionalEnclosingInstance = sci.optionalQualification;
        } else {
            IClass outerIClassOfSuperclass = superclass.getOuterIClass();
            if (outerIClassOfSuperclass == null) {
                optionalEnclosingInstance = null;
            } else {
                optionalEnclosingInstance = new Java.QualifiedThisReference(
                    sci.getLocation(),        // location
                    sci.declaringClass,       // declaringClass
                    sci.declaringConstructor, // declaringTypeBodyDeclaration
                    outerIClassOfSuperclass   // targetClass
                );
            }
        }
        this.invokeConstructor(
            (Java.Located) sci,                    // located
            (Java.Scope) sci.declaringConstructor, // scope
            optionalEnclosingInstance,             // optionalEnclosingInstance
            superclass,                            // targetClass
            sci.arguments                          // arguments
        );
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
        Java.Rvalue rv,
        final CodeContext.Offset dst,        // Where to jump.
        final boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) { try { UnitCompiler.this.compileBoolean2(ai  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayLength               (Java.ArrayLength                al  ) { try { UnitCompiler.this.compileBoolean2(al  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitAssignment                (Java.Assignment                 a   ) { try { UnitCompiler.this.compileBoolean2(a   , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { UnitCompiler.this.compileBoolean2(uo  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { try { UnitCompiler.this.compileBoolean2(bo  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCast                      (Java.Cast                       c   ) { try { UnitCompiler.this.compileBoolean2(c   , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) { try { UnitCompiler.this.compileBoolean2(cl  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) { try { UnitCompiler.this.compileBoolean2(ce  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConstantValue             (Java.ConstantValue              cv  ) { try { UnitCompiler.this.compileBoolean2(cv  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCrement                   (Java.Crement                    c   ) { try { UnitCompiler.this.compileBoolean2(c   , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitInstanceof                (Java.Instanceof                 io  ) { try { UnitCompiler.this.compileBoolean2(io  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) { try { UnitCompiler.this.compileBoolean2(mi  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) { try { UnitCompiler.this.compileBoolean2(smi , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLiteral                   (Java.Literal                    l   ) { try { UnitCompiler.this.compileBoolean2(l   , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) { try { UnitCompiler.this.compileBoolean2(naci, dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewArray                  (Java.NewArray                   na  ) { try { UnitCompiler.this.compileBoolean2(na  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) { try { UnitCompiler.this.compileBoolean2(nci , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) { try { UnitCompiler.this.compileBoolean2(pa  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) { try { UnitCompiler.this.compileBoolean2(qtr , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitThisReference             (Java.ThisReference              tr  ) { try { UnitCompiler.this.compileBoolean2(tr  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) { try { UnitCompiler.this.compileBoolean2(an  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) { try { UnitCompiler.this.compileBoolean2(aae , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) { try { UnitCompiler.this.compileBoolean2(fa  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) { try { UnitCompiler.this.compileBoolean2(fae , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) { try { UnitCompiler.this.compileBoolean2(lva , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { UnitCompiler.this.compileBoolean2(pe  , dst, orientation); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ void compileBoolean2(
        Java.Rvalue        rv,
        CodeContext.Offset dst,        // Where to jump.
        boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
    ) throws CompileException {
        if (this.compileGetValue(rv) != IClass.BOOLEAN) this.compileError("Not a boolean expression", rv.getLocation());
        this.writeBranch(rv, orientation == Java.Rvalue.JUMP_IF_TRUE ? Opcode.IFNE : Opcode.IFEQ, dst);
    }
    /*private*/ void compileBoolean2(
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
    /*private*/ void compileBoolean2(
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
                    this.pop((Java.Located) bo.lhs, this.compileGetValue(bo.lhs));
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
                lhsType.isPrimitiveNumeric() &&
                rhsType.isPrimitiveNumeric()
            ) {
                IClass promotedType = this.binaryNumericPromotion((Java.Located) bo, lhsType, convertLhsInserter, rhsType);
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

            // Boolean comparison.
            if (
                lhsType == IClass.BOOLEAN &&
                rhsType == IClass.BOOLEAN
            ) {
                if (bo.op != "==" && bo.op != "!=") this.compileError("Operator \"" + bo.op + "\" not allowed on boolean operands", bo.getLocation());
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
    /*private*/ void compileBoolean2(
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
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) {       res[0] = UnitCompiler.this.compileContext2(ai  );                                                                }
            public void visitArrayLength               (Java.ArrayLength                al  ) { try { res[0] = UnitCompiler.this.compileContext2(al  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitAssignment                (Java.Assignment                 a   ) {       res[0] = UnitCompiler.this.compileContext2(a   );                                                                }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) {       res[0] = UnitCompiler.this.compileContext2(uo  );                                                                }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) {       res[0] = UnitCompiler.this.compileContext2(bo  );                                                                }
            public void visitCast                      (Java.Cast                       c   ) {       res[0] = UnitCompiler.this.compileContext2(c   );                                                                }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) {       res[0] = UnitCompiler.this.compileContext2(cl  );                                                                }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) {       res[0] = UnitCompiler.this.compileContext2(ce  );                                                                }
            public void visitConstantValue             (Java.ConstantValue              cv  ) {       res[0] = UnitCompiler.this.compileContext2(cv  );                                                                }
            public void visitCrement                   (Java.Crement                    c   ) {       res[0] = UnitCompiler.this.compileContext2(c   );                                                                }
            public void visitInstanceof                (Java.Instanceof                 io  ) {       res[0] = UnitCompiler.this.compileContext2(io  );                                                                }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) {       res[0] = UnitCompiler.this.compileContext2(mi  );                                                                }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) {       res[0] = UnitCompiler.this.compileContext2(smi );                                                                }
            public void visitLiteral                   (Java.Literal                    l   ) {       res[0] = UnitCompiler.this.compileContext2(l   );                                                                }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) {       res[0] = UnitCompiler.this.compileContext2(naci);                                                                }
            public void visitNewArray                  (Java.NewArray                   na  ) {       res[0] = UnitCompiler.this.compileContext2(na  );                                                                }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) {       res[0] = UnitCompiler.this.compileContext2(nci );                                                                }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.compileContext2(pa  );                                                                }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) {       res[0] = UnitCompiler.this.compileContext2(qtr );                                                                }
            public void visitThisReference             (Java.ThisReference              tr  ) {       res[0] = UnitCompiler.this.compileContext2(tr  );                                                                }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) { try { res[0] = UnitCompiler.this.compileContext2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) { try { res[0] = UnitCompiler.this.compileContext2(aae ); } catch (CompileException e) { throw new TunnelException(e); } };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) { try { res[0] = UnitCompiler.this.compileContext2(fa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) { try { res[0] = UnitCompiler.this.compileContext2(fae ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) {       res[0] = UnitCompiler.this.compileContext2(lva );                                                                }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { res[0] = UnitCompiler.this.compileContext2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ int compileContext2(Java.Rvalue rv) {
        return 0;
    }
    /*private*/ int compileContext2(Java.AmbiguousName an) throws CompileException {
        return this.compileContext(this.toRvalueOrCE(this.reclassify(an)));
    }
    /*private*/ int compileContext2(Java.FieldAccess fa) throws CompileException {
        if (fa.field.isStatic()) {
            this.getType(this.toTypeOrCE(fa.lhs));
            return 0;
        } else {
            this.compileGetValue(this.toRvalueOrCE(fa.lhs));
            return 1;
        }
    }
    /*private*/ int compileContext2(Java.ArrayLength al) throws CompileException {
        if (!this.compileGetValue(al.lhs).isArray()) this.compileError("Cannot determine length of non-array type", al.getLocation());
        return 1;
    }
    /*private*/ int compileContext2(Java.ArrayAccessExpression aae) throws CompileException {
        IClass lhsType = this.compileGetValue(aae.lhs);
        if (!lhsType.isArray()) this.compileError("Subscript not allowed on non-array type \"" + lhsType.toString() + "\"", aae.getLocation());

        IClass indexType = this.compileGetValue(aae.index);
        if (
            !this.tryIdentityConversion(indexType, IClass.INT) &&
            !this.tryWideningPrimitiveConversion(
                (Java.Located) aae, // located
                indexType,          // sourceType
                IClass.INT          // targetType
            )
        ) this.compileError("Index expression of type \"" + indexType + "\" cannot be widened to \"int\"", aae.getLocation());

        return 2;
    }
    /*private*/ int compileContext2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileContext(fae.value);
    }
    /*private*/ int compileContext2(Java.ParenthesizedExpression pe) throws CompileException {
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
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) { try { res[0] = UnitCompiler.this.compileGet2(ai  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayLength               (Java.ArrayLength                al  ) {       res[0] = UnitCompiler.this.compileGet2(al  );                                                                }
            public void visitAssignment                (Java.Assignment                 a   ) { try { res[0] = UnitCompiler.this.compileGet2(a   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { res[0] = UnitCompiler.this.compileGet2(uo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { try { res[0] = UnitCompiler.this.compileGet2(bo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCast                      (Java.Cast                       c   ) { try { res[0] = UnitCompiler.this.compileGet2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) { try { res[0] = UnitCompiler.this.compileGet2(cl  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) { try { res[0] = UnitCompiler.this.compileGet2(ce  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConstantValue             (Java.ConstantValue              cv  ) {       res[0] = UnitCompiler.this.compileGet2(cv  );                                                                }
            public void visitCrement                   (Java.Crement                    c   ) { try { res[0] = UnitCompiler.this.compileGet2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitInstanceof                (Java.Instanceof                 io  ) { try { res[0] = UnitCompiler.this.compileGet2(io  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) { try { res[0] = UnitCompiler.this.compileGet2(mi  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) { try { res[0] = UnitCompiler.this.compileGet2(smi ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLiteral                   (Java.Literal                    l   ) { try { res[0] = UnitCompiler.this.compileGet2(l   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) { try { res[0] = UnitCompiler.this.compileGet2(naci); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewArray                  (Java.NewArray                   na  ) { try { res[0] = UnitCompiler.this.compileGet2(na  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) { try { res[0] = UnitCompiler.this.compileGet2(nci ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.compileGet2(pa  );                                                                }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) { try { res[0] = UnitCompiler.this.compileGet2(qtr ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitThisReference             (Java.ThisReference              tr  ) { try { res[0] = UnitCompiler.this.compileGet2(tr  ); } catch (CompileException e) { throw new TunnelException(e); } }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) { try { res[0] = UnitCompiler.this.compileGet2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) { try { res[0] = UnitCompiler.this.compileGet2(aae ); } catch (CompileException e) { throw new TunnelException(e); } };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) { try { res[0] = UnitCompiler.this.compileGet2(fa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) { try { res[0] = UnitCompiler.this.compileGet2(fae ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) {       res[0] = UnitCompiler.this.compileGet2(lva );                                                                }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { res[0] = UnitCompiler.this.compileGet2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ IClass compileGet2(Java.BooleanRvalue brv) throws CompileException {
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
    /*private*/ IClass compileGet2(Java.AmbiguousName an) throws CompileException {
        return this.compileGet(this.toRvalueOrCE(this.reclassify(an)));
    }
    /*private*/ IClass compileGet2(Java.LocalVariableAccess lva) {
        return this.load((Java.Located) lva, lva.localVariable);
    }
    /*private*/ IClass compileGet2(Java.FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.enclosingBlockStatement);
        if (fa.field.isStatic()) {
            this.writeOpcode(fa, Opcode.GETSTATIC);
        } else {
            this.writeOpcode(fa, Opcode.GETFIELD);
        }
        this.writeConstantFieldrefInfo(
            fa,
            fa.field.getDeclaringIClass().getDescriptor(), // classFD
            fa.field.getName(),                            // fieldName
            fa.field.getType().getDescriptor()             // fieldFD
        );
        return fa.field.getType();
    }
    /*private*/ IClass compileGet2(Java.ArrayLength al) {
        this.writeOpcode(al, Opcode.ARRAYLENGTH);
        return IClass.INT;
    }
    /*private*/ IClass compileGet2(Java.ThisReference tr) throws CompileException {
        this.referenceThis((Java.Located) tr);
        return this.getIClass(tr);
    }
    /*private*/ IClass compileGet2(Java.QualifiedThisReference qtr) throws CompileException {
        this.referenceThis(
            (Java.Located) qtr,
            this.getDeclaringClass(qtr),
            this.getDeclaringTypeBodyDeclaration(qtr),
            this.getTargetIClass(qtr)
        );
        return this.getTargetIClass(qtr);
    }
    /*private*/ IClass compileGet2(Java.ClassLiteral cl) throws CompileException {
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
            if (wrapperClassDescriptor == null) throw new RuntimeException();

            this.writeConstantFieldrefInfo(
                cl,
                wrapperClassDescriptor, // classFD
                "TYPE",                 // fieldName
                "Ljava/lang/Class;"     // fieldFD
            );
            return icl.CLASS;
        }

        // Non-primitive class literal.

        // Check if synthetic method "static Class class$(String className)" is already
        // declared.
        boolean classDollarMethodDeclared = false;
        {
            for (Iterator it = cl.declaringType.declaredMethods.iterator(); it.hasNext();) {
                Java.MethodDeclarator md = (Java.MethodDeclarator) it.next();
                if (md.name.equals("class$")) {
                    classDollarMethodDeclared = true;
                    break;
                }
            }
        }
        if (!classDollarMethodDeclared) this.declareClassDollarMethod(cl);

        // Determine the statics of the declaring class (this is where static fields
        // declarations are found).
        List statics; // TypeBodyDeclaration
        if (cl.declaringType instanceof Java.ClassDeclaration) {
            statics = ((Java.ClassDeclaration) cl.declaringType).variableDeclaratorsAndInitializers;
        } else
        if (cl.declaringType instanceof Java.InterfaceDeclaration) {
            statics = ((Java.InterfaceDeclaration) cl.declaringType).constantDeclarations;
        } else {
            throw new RuntimeException();
        }

        String className = Descriptor.toClassName(iClass.getDescriptor());
        final String classDollarFieldName = "class$" + className.replace('.', '$');

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
                    loc,              // location
                    cl.declaringType, // declaringType
                    null,             // optionalDocComment
                    Mod.STATIC,       // modifiers
                    classType         // type
                );
                fd.setVariableDeclarators(new Java.VariableDeclarator[] {
                    new Java.VariableDeclarator(
                        loc,                  // location
                        classDollarFieldName, // name
                        0,                    // brackets
                        (Java.Rvalue) null    // optionalInitializer
                    )
                });
                if (cl.declaringType instanceof Java.ClassDeclaration) {
                    ((Java.ClassDeclaration) cl.declaringType).addVariableDeclaratorOrInitializer(fd);
                } else
                if (cl.declaringType instanceof Java.InterfaceDeclaration) {
                    ((Java.InterfaceDeclaration) cl.declaringType).addConstantDeclaration(fd);
                } else {
                    throw new RuntimeException();
                }
            }
        }

        // return (class$X != null) ? class$X : (class$X = class$("X"));
        Java.Type declaringClassOrInterfaceType = new Java.SimpleType(loc, this.resolve(cl.declaringType));
        Java.Lvalue classDollarFieldAccess = new Java.FieldAccessExpression(
            loc,                           // location
            cl.enclosingBlockStatement,    // enclosingBlockStatement
            declaringClassOrInterfaceType, // lhs
            classDollarFieldName           // fieldName
        );
        return this.compileGet(new Java.ConditionalExpression(
            loc,                                  // location
            new Java.BinaryOperation(             // lhs
                loc,                              // location
                classDollarFieldAccess,           // lhs
                "!=",                             // op
                new Java.ConstantValue(loc, null) // rhs
            ),
            classDollarFieldAccess,        // mhs
            new Java.Assignment(           // rhs
                loc,                       // location
                classDollarFieldAccess,    // lhs
                "=",                       // operator
                new Java.MethodInvocation( // rhs
                    loc,                           // location
                    cl.enclosingBlockStatement,    // enclosingBlockStatement
                    declaringClassOrInterfaceType, // optionalTarget
                    "class$",                      // methodName
                    new Java.Rvalue[] {            // arguments
                        new Java.ConstantValue(
                            loc,      // location
                            className // constantValue
                        )
                    }
                )
            )
        ));
    }
    /*private*/ IClass compileGet2(Java.Assignment a) throws CompileException {
        if (a.operator == "=") {
            int lhsCS = this.compileContext(a.lhs);
            IClass rhsType = this.compileGetValue(a.rhs);
            IClass lhsType = this.getType(a.lhs);
            Object rhsCV = this.getConstantValue(a.rhs);
            this.assignmentConversion(
                (Java.Located) a, // located
                rhsType,          // sourceType
                lhsType,          // targetType
                rhsCV             // optionalConstantValue
            );
            this.dupx(
                (Java.Located) a, // located
                lhsType,          // type
                lhsCS             // x
            );
            this.compileSet(a.lhs);
            return lhsType;
        }

        // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
        int lhsCS = this.compileContext(a.lhs);
        this.dup((Java.Located) a, lhsCS);
        IClass lhsType = this.compileGet(a.lhs);
        IClass resultType = this.compileArithmeticBinaryOperation(
            (Java.Located) a,     // located
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
                (Java.Located) a, // located
                resultType,       // sourceType
                lhsType           // destinationType
            )
        ) throw new RuntimeException();
        this.dupx(
            (Java.Located) a, // located
            lhsType,          // type
            lhsCS             // x
        );
        this.compileSet(a.lhs);
        return lhsType;
    }
    /*private*/ IClass compileGet2(Java.ConditionalExpression ce) throws CompileException {
        IClass mhsType, rhsType;
        CodeContext.Inserter mhsConvertInserter;
        CodeContext.Offset toEnd = this.codeContext.new Offset();
        Object cv = this.getConstantValue(ce.lhs);
        if (cv instanceof Boolean) {
            if (((Boolean) cv).booleanValue()) {
                mhsType = this.compileGetValue(ce.mhs);
                mhsConvertInserter = this.codeContext.newInserter();
                rhsType = this.getType(ce.rhs);
            } else {
                mhsType = this.getType(ce.mhs);
                mhsConvertInserter = null;
                rhsType = this.compileGetValue(ce.rhs);
            }
        } else {
            CodeContext.Offset toRhs = this.codeContext.new Offset();

            this.compileBoolean(ce.lhs, toRhs, Java.Rvalue.JUMP_IF_FALSE);
            mhsType = this.compileGetValue(ce.mhs);
            mhsConvertInserter = this.codeContext.newInserter();
            this.writeBranch(ce, Opcode.GOTO, toEnd);
            toRhs.set();
            rhsType = this.compileGetValue(ce.rhs);
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
                (Java.Located) ce,  // located
                mhsType,            // type1
                mhsConvertInserter, // convertInserter1
                rhsType             // type2
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
    /*private*/ IClass compileGet2(Java.Crement c) throws CompileException {

        // Optimized crement of integer local variable.
        Java.LocalVariable lv = this.isIntLV(c);
        if (lv != null) {
            if (!c.pre) this.load((Java.Located) c, lv);
            this.writeOpcode(c, Opcode.IINC);
            this.writeByte(c, lv.localVariableArrayIndex);
            this.writeByte(c, c.operator == "++" ? 1 : -1);
            if (c.pre) this.load((Java.Located) c, lv);
            return lv.type;
        }

        // Compile operand context.
        int cs = this.compileContext(c.operand);
        // DUP operand context.
        this.dup((Java.Located) c, cs);
        // Get operand value.
        IClass type = this.compileGet(c.operand);
        // DUPX operand value.
        if (!c.pre) this.dupx((Java.Located) c, type, cs);
        // Apply "unary numeric promotion".
        IClass promotedType = this.unaryNumericPromotion((Java.Located) c, type);
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
        // Reverse "unary numeric promotion".
        if (
            !this.tryIdentityConversion(promotedType, type) &&
            !this.tryNarrowingPrimitiveConversion(
                (Java.Located) c,   // located
                promotedType,       // sourceType
                type                // targetType
            )
        ) throw new RuntimeException();
        // DUPX cremented operand value.
        if (c.pre) this.dupx((Java.Located) c, type, cs);
        // Set operand.
        this.compileSet(c.operand);

        return type;
    }
    /*private*/ IClass compileGet2(Java.ArrayAccessExpression aae) throws CompileException {
        IClass lhsComponentType = this.getType(aae);
        this.writeOpcode(aae, Opcode.IALOAD + UnitCompiler.ilfdabcs(lhsComponentType));
        return lhsComponentType;
    }
    /*private*/ IClass compileGet2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.compileGet(fae.value);
    }
    /*private*/ IClass compileGet2(Java.UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") {
            return this.compileGet2((Java.BooleanRvalue) uo);
        }

        if (uo.operator == "+") {
            return this.compileGetValue(uo.operand);
        }

        if (uo.operator == "-") {
            IClass operandType;
            if (uo.operand instanceof Java.Literal) {
                Java.Literal l = (Java.Literal) uo.operand;
                operandType = this.getType2(l);
                this.pushConstant((Java.Located) uo, this.getNegatedConstantValue2(l));
            } else {
                operandType = this.compileGetValue(uo.operand);
            }

            IClass promotedType = this.unaryNumericPromotion((Java.Located) uo, operandType);
            this.writeOpcode(uo, Opcode.INEG + UnitCompiler.ilfd(promotedType));
            return promotedType;
        }

        if (uo.operator == "~") {
            IClass operandType = this.compileGetValue(uo.operand);

            IClass promotedType = this.unaryNumericPromotion((Java.Located) uo, operandType);
            if (promotedType == IClass.INT) {
                this.writeOpcode(uo, Opcode.ICONST_M1);
                this.writeOpcode(uo, Opcode.IXOR);
                return IClass.INT;
            }
            if (promotedType == IClass.LONG) {
                this.writeOpcode(uo, Opcode.LDC2_W);
                this.writeConstantLongInfo(uo, -1L);
                this.writeOpcode(uo, Opcode.LXOR);
                return IClass.LONG;
            }
            this.compileError("Operator \"~\" not applicable to type \"" + promotedType + "\"", uo.getLocation());
        }

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return this.iClassLoader.OBJECT;
    }
    /*private*/ IClass compileGet2(Java.Instanceof io) throws CompileException {
        IClass lhsType = this.compileGetValue(io.lhs);
        IClass rhsType = this.getType(io.rhs);

        if (rhsType.isAssignableFrom(lhsType)) {
            this.pop((Java.Located) io, lhsType);
            this.writeOpcode(io, Opcode.ICONST_1);
        } else
        if (
            lhsType.isInterface() ||
            rhsType.isInterface() ||
            lhsType.isAssignableFrom(rhsType)
        ) {
            this.writeOpcode(io, Opcode.INSTANCEOF);
            this.writeConstantClassInfo(io, rhsType.getDescriptor());
        } else {
            this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"", io.getLocation());
        }
        return IClass.BOOLEAN;
    }
    /*private*/ IClass compileGet2(Java.BinaryOperation bo) throws CompileException {
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
            (Java.Located) bo,          // located
            null,                       // type
            bo.unrollLeftAssociation(), // operands
            bo.op                       // operator
        );
    }
    /*private*/ IClass compileGet2(Java.Cast c) throws CompileException {
        IClass tt = this.getType(c.targetType);
        IClass vt = this.compileGetValue(c.value);
        if (
            !this.tryIdentityConversion(vt, tt) &&
            !this.tryWideningPrimitiveConversion((Java.Located) c, vt, tt) &&
            !this.tryNarrowingPrimitiveConversion((Java.Located) c, vt, tt) &&
            !this.isWideningReferenceConvertible(vt, tt) &&
            !this.tryNarrowingReferenceConversion((Java.Located) c, vt, tt)
        ) this.compileError("Cannot cast \"" + vt + "\" to \"" + tt + "\"", c.getLocation());
        return tt;
    }
    /*private*/ IClass compileGet2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.compileGet(pe.value);
    }
    /*private*/ IClass compileGet2(Java.MethodInvocation mi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(mi);

        IClass targetType;
        if (mi.optionalTarget == null) {

            // JLS2 6.5.7.1, 15.12.4.1.1.1
            Java.TypeBodyDeclaration scopeTBD;
            Java.ClassDeclaration    scopeClassDeclaration;
            {
                Java.Scope s;
                for (s = mi.enclosingBlockStatement; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
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
                    (Java.Located) mi,           // located
                    scopeClassDeclaration,       // declaringClass
                    scopeTBD,                    // declaringTypeBodyDeclaration
                    iMethod.getDeclaringIClass() // targetIClass
                );
            }
            targetType = this.resolve(scopeClassDeclaration);
        } else {

            // 6.5.7.2
            boolean staticContext = this.isType(mi.optionalTarget);
            if (staticContext) {
                targetType = this.getType(this.toTypeOrCE(mi.optionalTarget));
            } else
            {
                Java.Rvalue targetValue = this.toRvalueOrCE(mi.optionalTarget);

                // TODO: Wrapper methods for private methods of enclosing / enclosed types.
                if (
                    this.getType(targetValue) != iMethod.getDeclaringIClass() &&
                    iMethod.getAccess() == Access.PRIVATE
                ) this.compileError("Invocation of private methods of enclosing or enclosed type NYI; please change the access of method \"" + iMethod.getName() + "()\" from \"private\" to \"/*private*/\"", mi.getLocation());

                targetType = this.compileGetValue(targetValue);
            }
            if (iMethod.isStatic()) {
                if (!staticContext) {
                    // JLS2 15.12.4.1.2.1
                    this.pop((Java.Located) mi.optionalTarget, this.getType(mi.optionalTarget));
                }
            } else {
                if (staticContext) this.compileError("Instance method \"" + mi.methodName + "\" cannot be invoked in static context", mi.getLocation());
            }
        }

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < mi.arguments.length; ++i) {
            this.assignmentConversion(
                (Java.Located) mi,                     // located
                this.compileGetValue(mi.arguments[i]), // sourceType
                parameterTypes[i],                     // targetType
                this.getConstantValue(mi.arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        this.checkAccessible(iMethod, mi.enclosingBlockStatement);
        if (iMethod.getDeclaringIClass().isInterface()) {
            this.writeOpcode(mi, Opcode.INVOKEINTERFACE);
            this.writeConstantInterfaceMethodrefInfo(
                mi,                                           // locatable
                iMethod.getDeclaringIClass().getDescriptor(), // classFD
                iMethod.getName(),                            // methodName
                iMethod.getDescriptor()                       // methodMD
            );
            IClass[] pts = iMethod.getParameterTypes();
            int count = 1;
            for (int i = 0; i < pts.length; ++i) count += Descriptor.size(pts[i].getDescriptor());
            this.writeByte(mi, count);
            this.writeByte(mi, 0);
        } else {
            byte opcode = (
                iMethod.isStatic()                    ? Opcode.INVOKESTATIC :
                iMethod.getAccess() == Access.PRIVATE ? Opcode.INVOKESPECIAL :
                Opcode.INVOKEVIRTUAL
            );
            this.writeOpcode(mi, opcode);
            if (opcode != Opcode.INVOKEVIRTUAL) targetType = iMethod.getDeclaringIClass();
            this.writeConstantMethodrefInfo(
                mi,                         // locatable
                targetType.getDescriptor(), // classFD
                iMethod.getName(),          // methodName
                iMethod.getDescriptor()     // methodMD
            );
        }
        return iMethod.getReturnType();
    }
    /*private*/ IClass compileGet2(Java.SuperclassMethodInvocation scmi) throws CompileException {
        IClass.IMethod iMethod = this.findIMethod(scmi);

        Java.Scope s;
        for (s = scmi.enclosingBlockStatement; s instanceof Java.Statement; s = s.getEnclosingScope());
        Java.FunctionDeclarator fd = s instanceof Java.FunctionDeclarator ? (Java.FunctionDeclarator) s : null;
        if (fd == null) {
            this.compileError("Cannot invoke superclass method in non-method scope", scmi.getLocation());
            return IClass.INT;
        }
        if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Cannot invoke superclass method in static context", scmi.getLocation());
        this.load((Java.Located) scmi, this.resolve(fd.getDeclaringType()), 0);

        // Evaluate method parameters.
        IClass[] parameterTypes = iMethod.getParameterTypes();
        for (int i = 0; i < scmi.arguments.length; ++i) {
            this.assignmentConversion(
                (Java.Located) scmi,                     // located
                this.compileGetValue(scmi.arguments[i]), // sourceType
                parameterTypes[i],                       // targetType
                this.getConstantValue(scmi.arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        this.writeOpcode(scmi, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            scmi,
            iMethod.getDeclaringIClass().getDescriptor(), // classFD
            scmi.methodName,                              // methodName
            iMethod.getDescriptor()                       // methodMD
        );
        return iMethod.getReturnType();
    }
    /*private*/ IClass compileGet2(Java.NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);

        this.writeOpcode(nci, Opcode.NEW);
        this.writeConstantClassInfo(nci, nci.iClass.getDescriptor());
        this.writeOpcode(nci, Opcode.DUP);

        // Determine the enclosing instance for the new object.
        Java.Rvalue optionalEnclosingInstance;
        if (nci.optionalQualification != null) {

            // Enclosing instance defined by qualification (JLS 15.9.2.BL1.B3.B2).
            optionalEnclosingInstance = nci.optionalQualification;
        } else {
            Java.Scope s = nci.scope;
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
                optionalEnclosingInstance = null;
            } else {

                // Determine the type of the enclosing instance for the new object.
                IClass optionalOuterIClass = nci.iClass.getDeclaringIClass();
                if (optionalOuterIClass == null) {

                    // No enclosing instance needed for a top-level class object.
                    optionalEnclosingInstance = new Java.ThisReference(nci.getLocation(), nci.scope);
                } else {

                    // Find an appropriate enclosing instance for the new inner class object among
                    // the enclosing instances of the current object (JLS
                    // 15.9.2.BL1.B3.B1.B2).
                    Java.ClassDeclaration outerClassDeclaration = (Java.ClassDeclaration) enclosingTypeDeclaration;
                    optionalEnclosingInstance = new Java.QualifiedThisReference(
                        nci.getLocation(),            // location
                        outerClassDeclaration,        // declaringClass
                        enclosingTypeBodyDeclaration, // declaringTypeBodyDeclaration
                        optionalOuterIClass           // targetIClass
                    );
                }
            }
        }

        this.invokeConstructor(
            (Java.Located) nci,        // located
            nci.scope,                 // scope
            optionalEnclosingInstance, // optionalEnclosingInstance
            nci.iClass,                // targetClass
            nci.arguments              // arguments
        );
        return nci.iClass;
    }
    /*private*/ IClass compileGet2(Java.NewAnonymousClassInstance naci) throws CompileException {

        // Find constructors.
        Java.AnonymousClassDeclaration acd = naci.anonymousClassDeclaration;
        IClass sc = this.resolve(acd).getSuperclass();
        IClass.IConstructor[] iConstructors = sc.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new RuntimeException();

        // Determine most specific constructor.
        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            (Java.Located) naci, // located
            iConstructors,       // iInvocables
            naci.arguments       // arguments
        );

        IClass[] pts = iConstructor.getParameterTypes();

        // Determine formal parameters of anonymous constructor.
        Java.FormalParameter[] fps;
        Location loc = naci.getLocation();
        {
            List l = new ArrayList(); // FormalParameter

            // Pass the enclosing instance of the base class as parameter #1.
            if (naci.optionalQualification != null) l.add(new Java.FormalParameter(
                true,                                                               // finaL
                new Java.SimpleType(loc, this.getType(naci.optionalQualification)), // type
                "this$base"                                                         // name
            ));
            for (int i = 0; i < pts.length; ++i) l.add(new Java.FormalParameter(
                true,                             // finaL
                new Java.SimpleType(loc, pts[i]), // type
                "p" + i                           // name
            ));
            fps = (Java.FormalParameter[]) l.toArray(new Java.FormalParameter[l.size()]);
        }

        // Determine thrown exceptions of anonymous constructor.
        IClass[] tes = iConstructor.getThrownExceptions();
        Java.Type[]   tets = new Java.Type[tes.length];
        for (int i = 0; i < tes.length; ++i) tets[i] = new Java.SimpleType(loc, tes[i]);

        // Generate the anonymous constructor for the anonymous class (JLS 15.9.5.1).
        final Java.ConstructorDeclarator anonymousConstructor = new Java.ConstructorDeclarator(
            loc,         // location
            acd,         // declaringClass
            null,        // optionalDocComment
            Mod.PACKAGE, // modifiers
            fps,         // formalParameters
            tets         // thrownExceptions
        );

        // The anonymous constructor merely invokes the constructor of its superclass.
        Java.Rvalue wrappedOptionalQualification = (
            naci.optionalQualification == null ? null :
            new Java.ParameterAccess(loc, anonymousConstructor, "this$base")
        );
        Java.Rvalue[] wrappedArguments = new Java.Rvalue[pts.length];
        for (int i = 0; i < pts.length; ++i) {
            wrappedArguments[i] = new Java.ParameterAccess(loc, anonymousConstructor, "p" + i);
        }
        anonymousConstructor.setExplicitConstructorInvocation(new Java.SuperConstructorInvocation(
            loc,                          // location
            acd,                          // declaringClass
            anonymousConstructor,         // declaringConstructor
            wrappedOptionalQualification, // optionalQualification
            wrappedArguments              // arguments
        ));
        anonymousConstructor.setBody(new Java.Block(loc, (Java.Scope) anonymousConstructor));
        acd.addConstructor(anonymousConstructor);

        // Compile the anonymous class.
        this.compile(acd);

        // Instantiate the anonymous class.
        this.writeOpcode(naci, Opcode.NEW);
        this.writeConstantClassInfo(naci, this.resolve(naci.anonymousClassDeclaration).getDescriptor());

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
        for (s = naci.scope; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
        boolean inStaticContext = ((Java.TypeBodyDeclaration) s).isStatic();
        this.invokeConstructor(
            (Java.Located) naci,                                              // located
            naci.scope,                                                       // scope
            inStaticContext ? null : new Java.ThisReference(loc, naci.scope), // optionalEnclosingInstance
            this.resolve(naci.anonymousClassDeclaration),                     // targetClass
            arguments2                                                        // arguments
        );
        return this.resolve(naci.anonymousClassDeclaration);
    }
    /*private*/ IClass compileGet2(Java.ParameterAccess pa) {
        Java.LocalVariable lv = (Java.LocalVariable) pa.declaringFunction.parameters.get(pa.name);
        this.load((Java.Located) pa, lv);
        return lv.type;
    }
    /*private*/ IClass compileGet2(Java.NewArray na) throws CompileException {
        for (int i = 0; i < na.dimExprs.length; ++i) {
            IClass dimType = this.compileGetValue(na.dimExprs[i]);
            if (dimType != IClass.INT && this.unaryNumericPromotion(
                (Java.Located) na, // located
                dimType         // type
            ) != IClass.INT) this.compileError("Invalid array size expression type", na.getLocation());
        }

        return this.newArray(
            (Java.Located) na,    // located
            na.dimExprs.length,   // dimExprCount
            na.dims,              // dims
            this.getType(na.type) // componentType
        );
    }
    /*private*/ IClass compileGet2(Java.ArrayInitializer ai) throws CompileException {
        IClass at = this.getType(ai.arrayType);
        IClass ct = at.getComponentType();

        this.pushConstant((Java.Located) ai, new Integer(ai.values.length));
        this.newArray(
            (Java.Located) ai, // located
            1,                 // dimExprCount,
            0,                 // dims,
            ct                 // componentType
        );

        for (int i = 0; i < ai.values.length; ++i) {
            Java.Rvalue v = ai.values[i];
            this.writeOpcode(ai, Opcode.DUP);
            this.pushConstant((Java.Located) ai, new Integer(i));
            IClass vt = this.compileGetValue(v);
            this.assignmentConversion(
                (Java.Located) ai,       // located
                vt,                      // sourceType
                ct,                      // targetType
                this.getConstantValue(v) // optionalConstantValue
            );
            this.writeOpcode(ai, Opcode.IASTORE + UnitCompiler.ilfdabcs(ct));
        }
        return at;
    }
    /*private*/ IClass compileGet2(Java.Literal l) throws CompileException {
        if (
            l.value == Scanner.MAGIC_INTEGER ||
            l.value == Scanner.MAGIC_LONG
        ) this.compileError("This literal value may only appear in a negated context", l.getLocation());
        return this.pushConstant((Java.Located) l, l.value == null ? Java.Rvalue.CONSTANT_VALUE_NULL : l.value);
    }
    /*private*/ IClass compileGet2(Java.ConstantValue cv) {
        return this.pushConstant((Java.Located) cv, cv.constantValue);
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
            this.pushConstant((Java.Located) rv, cv);
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
     */
    /*private*/ final Object getConstantValue(Java.Rvalue rv) throws CompileException {
        if (rv.constantValue != Java.Rvalue.CONSTANT_VALUE_UNKNOWN) return rv.constantValue;
        rv.constantValue = this.getConstantValue2(rv);

        final Object[] res = new Object[1];
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) {       res[0] = UnitCompiler.this.getConstantValue2(ai  );                                                                }
            public void visitArrayLength               (Java.ArrayLength                al  ) {       res[0] = UnitCompiler.this.getConstantValue2(al  );                                                                }
            public void visitAssignment                (Java.Assignment                 a   ) {       res[0] = UnitCompiler.this.getConstantValue2(a   );                                                                }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { res[0] = UnitCompiler.this.getConstantValue2(uo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) { try { res[0] = UnitCompiler.this.getConstantValue2(bo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCast                      (Java.Cast                       c   ) { try { res[0] = UnitCompiler.this.getConstantValue2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) {       res[0] = UnitCompiler.this.getConstantValue2(cl  );                                                                }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) {       res[0] = UnitCompiler.this.getConstantValue2(ce  );                                                                }
            public void visitConstantValue             (Java.ConstantValue              cv  ) {       res[0] = UnitCompiler.this.getConstantValue2(cv  );                                                                }
            public void visitCrement                   (Java.Crement                    c   ) {       res[0] = UnitCompiler.this.getConstantValue2(c   );                                                                }
            public void visitInstanceof                (Java.Instanceof                 io  ) {       res[0] = UnitCompiler.this.getConstantValue2(io  );                                                                }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) {       res[0] = UnitCompiler.this.getConstantValue2(mi  );                                                                }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) {       res[0] = UnitCompiler.this.getConstantValue2(smi );                                                                }
            public void visitLiteral                   (Java.Literal                    l   ) { try { res[0] = UnitCompiler.this.getConstantValue2(l   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) {       res[0] = UnitCompiler.this.getConstantValue2(naci);                                                                }
            public void visitNewArray                  (Java.NewArray                   na  ) {       res[0] = UnitCompiler.this.getConstantValue2(na  );                                                                }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) {       res[0] = UnitCompiler.this.getConstantValue2(nci );                                                                }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.getConstantValue2(pa  );                                                                }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) {       res[0] = UnitCompiler.this.getConstantValue2(qtr );                                                                }
            public void visitThisReference             (Java.ThisReference              tr  ) {       res[0] = UnitCompiler.this.getConstantValue2(tr  );                                                                }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) { try { res[0] = UnitCompiler.this.getConstantValue2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) {       res[0] = UnitCompiler.this.getConstantValue2(aae );                                                                };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) { try { res[0] = UnitCompiler.this.getConstantValue2(fa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) {       res[0] = UnitCompiler.this.getConstantValue2(fae );                                                                }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) {       res[0] = UnitCompiler.this.getConstantValue2(lva );                                                                }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { res[0] = UnitCompiler.this.getConstantValue2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
            rv.constantValue = res[0];
            return rv.constantValue;
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ Object getConstantValue2(Java.Rvalue rv) {
        return null;
    }
    /*private*/ Object getConstantValue2(Java.AmbiguousName an) throws CompileException {
        return this.getConstantValue(this.toRvalueOrCE(this.reclassify(an)));
    }
    /*private*/ Object getConstantValue2(Java.FieldAccess fa) throws CompileException {
        return fa.field.getConstantValue();
    }
    /*private*/ Object getConstantValue2(Java.UnaryOperation uo) throws CompileException {
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
    /*private*/ Object getConstantValue2(Java.BinaryOperation bo) throws CompileException {

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
    /*private*/ Object getConstantValue2(Java.Cast c) throws CompileException {
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
    /*private*/ Object getConstantValue2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getConstantValue(pe.value);
    }
    /*private*/ Object getConstantValue2(Java.Literal l) throws CompileException {
        if (
            l.value == Scanner.MAGIC_INTEGER ||
            l.value == Scanner.MAGIC_LONG
        ) this.compileError("This literal value may only appear in a negated context", l.getLocation());
        return l.value == null ? Java.Rvalue.CONSTANT_VALUE_NULL : l.value;
    }
    /*private*/ Object getConstantValue2(Java.ConstantValue cv) {
        return cv.constantValue;
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
        Visitor.RvalueVisitor rvv = new Visitor.RvalueVisitor() {
            public void visitArrayInitializer          (Java.ArrayInitializer           ai  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(ai  );                                                                }
            public void visitArrayLength               (Java.ArrayLength                al  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(al  );                                                                }
            public void visitAssignment                (Java.Assignment                 a   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(a   );                                                                }
            public void visitUnaryOperation            (Java.UnaryOperation             uo  ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(uo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation           (Java.BinaryOperation            bo  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(bo  );                                                                }
            public void visitCast                      (Java.Cast                       c   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(c   );                                                                }
            public void visitClassLiteral              (Java.ClassLiteral               cl  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(cl  );                                                                }
            public void visitConditionalExpression     (Java.ConditionalExpression      ce  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(ce  );                                                                }
            public void visitConstantValue             (Java.ConstantValue              cv  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(cv  );                                                                }
            public void visitCrement                   (Java.Crement                    c   ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(c   );                                                                }
            public void visitInstanceof                (Java.Instanceof                 io  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(io  );                                                                }
            public void visitMethodInvocation          (Java.MethodInvocation           mi  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(mi  );                                                                }
            public void visitSuperclassMethodInvocation(Java.SuperclassMethodInvocation smi ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(smi );                                                                }
            public void visitLiteral                   (Java.Literal                    l   ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(l   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewAnonymousClassInstance (Java.NewAnonymousClassInstance  naci) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(naci);                                                                }
            public void visitNewArray                  (Java.NewArray                   na  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(na  );                                                                }
            public void visitNewClassInstance          (Java.NewClassInstance           nci ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(nci );                                                                }
            public void visitParameterAccess           (Java.ParameterAccess            pa  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(pa  );                                                                }
            public void visitQualifiedThisReference    (Java.QualifiedThisReference     qtr ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(qtr );                                                                }
            public void visitThisReference             (Java.ThisReference              tr  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(tr  );                                                                }

            public void visitAmbiguousName             (Java.AmbiguousName              an  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(an  );                                                                }
            public void visitArrayAccessExpression     (Java.ArrayAccessExpression      aae ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(aae );                                                                };
            public void visitFieldAccess               (Java.FieldAccess                fa  ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(fa  );                                                                }
            public void visitFieldAccessExpression     (Java.FieldAccessExpression      fae ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(fae );                                                                }
            public void visitLocalVariableAccess       (Java.LocalVariableAccess        lva ) {       res[0] = UnitCompiler.this.getNegatedConstantValue2(lva );                                                                }
            public void visitParenthesizedExpression   (Java.ParenthesizedExpression    pe  ) { try { res[0] = UnitCompiler.this.getNegatedConstantValue2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            rv.accept(rvv);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ Object getNegatedConstantValue2(Java.Rvalue rv) {
        return null;
    }
    /*private*/ Object getNegatedConstantValue2(Java.UnaryOperation uo) throws CompileException {
        return (
            uo.operator.equals("+") ? this.getNegatedConstantValue(uo.operand) :
            uo.operator.equals("-") ? this.getConstantValue(uo.operand) :
            null
        );
    }
    /*private*/ Object getNegatedConstantValue2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getNegatedConstantValue(pe.value);
    }
    /*private*/ Object getNegatedConstantValue2(Java.Literal l) throws CompileException {
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
        Visitor.BlockStatementVisitor bsv = new Visitor.BlockStatementVisitor() {
            public void visitInitializer                      (Java.Initializer                       i   ) { try { res[0] = UnitCompiler.this.generatesCode2(i   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldDeclaration                 (Java.FieldDeclaration                  fd  ) { try { res[0] = UnitCompiler.this.generatesCode2(fd  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLabeledStatement                 (Java.LabeledStatement                  ls  ) {       res[0] = UnitCompiler.this.generatesCode2(ls  );                                                                }
            public void visitBlock                            (Java.Block                             b   ) { try { res[0] = UnitCompiler.this.generatesCode2(b   ); } catch (CompileException e) { throw new TunnelException(e); } }
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
        };
        try {
            bs.accept(bsv);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    public boolean generatesCode2(Java.BlockStatement bs) { return true; }
    public boolean generatesCode2(Java.EmptyStatement es) { return false; }
    public boolean generatesCode2(Java.LocalClassDeclarationStatement lcds) { return false; }
    public boolean generatesCode2(Java.Initializer i) throws CompileException { return this.generatesCode(i.block); }
    public boolean generatesCode2(Java.Block b) throws CompileException {
        for (int i = 0; i < b.statements.size(); ++i) {
            if (this.generatesCode(((Java.BlockStatement) b.statements.get(i)))) return true;
        }
        return false;
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
        };
        bs.accept(bsv);
    }
    public void leave2(Java.BlockStatement bs, IClass optionalStackValueType) { ; }
    public void leave2(Java.SynchronizedStatement ss, IClass optionalStackValueType) {
        this.load((Java.Located) ss, this.iClassLoader.OBJECT, ss.monitorLvIndex);
        this.writeOpcode(ss, Opcode.MONITOREXIT);
    }
    public void leave2(Java.TryStatement ts, IClass optionalStackValueType) {
        if (ts.finallyOffset != null) {

            // Obviously, JSR must always be executed with the operand stack being
            // empty; otherwise we get "java.lang.VerifyError: Inconsistent stack height
            // 1 != 2"
            if (optionalStackValueType != null) {
                this.store((Java.Located) ts, optionalStackValueType, ts.stackValueLvIndex);
            }

            this.writeBranch(ts, Opcode.JSR, ts.finallyOffset);

            if (optionalStackValueType != null) {
                this.load((Java.Located) ts, optionalStackValueType, ts.stackValueLvIndex);
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
        Visitor.LvalueVisitor lvv = new Visitor.LvalueVisitor() {
            public void visitAmbiguousName          (Java.AmbiguousName           an ) { try { UnitCompiler.this.compileSet2(an ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression  (Java.ArrayAccessExpression   aae) { try { UnitCompiler.this.compileSet2(aae); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccess            (Java.FieldAccess             fa ) { try { UnitCompiler.this.compileSet2(fa ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression  (Java.FieldAccessExpression   fae) { try { UnitCompiler.this.compileSet2(fae); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess    (Java.LocalVariableAccess     lva) {       UnitCompiler.this.compileSet2(lva);                                                                }
            public void visitParenthesizedExpression(Java.ParenthesizedExpression pe ) { try { UnitCompiler.this.compileSet2(pe ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            lv.accept(lvv);
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ void compileSet2(Java.AmbiguousName an) throws CompileException {
        this.compileSet(this.toLvalueOrCE(this.reclassify(an)));
    }
    /*private*/ void compileSet2(Java.LocalVariableAccess lva) {
        this.store(
            (Java.Located) lva,
            lva.localVariable.type,
            lva.localVariable
        );
    }
    /*private*/ void compileSet2(Java.FieldAccess fa) throws CompileException {
        this.checkAccessible(fa.field, fa.enclosingBlockStatement);
        this.writeOpcode(fa, (
            fa.field.isStatic() ?
            Opcode.PUTSTATIC :
            Opcode.PUTFIELD
        ));
        this.writeConstantFieldrefInfo(
            fa,
            fa.field.getDeclaringIClass().getDescriptor(), // classFD
            fa.field.getName(),                            // fieldName
            fa.field.getDescriptor()                       // fieldFD
        );
    }
    /*private*/ void compileSet2(Java.ArrayAccessExpression aae) throws CompileException {
        this.writeOpcode(aae, Opcode.IASTORE + UnitCompiler.ilfdabcs(this.getType(aae)));
    }
    /*private*/ void compileSet2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        this.compileSet(this.toLvalueOrCE(fae.value));
    }
    /*private*/ void compileSet2(Java.ParenthesizedExpression pe) throws CompileException {
        this.compileSet(this.toLvalueOrCE(pe.value));
    }

    // ---------------- Atom.getType() ----------------

    /*private*/ IClass getType(Java.Atom a) throws CompileException {
        final IClass[] res = new IClass[1];
        Visitor.AtomVisitor av = new Visitor.AtomVisitor() {
            public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci ) {       res[0] = UnitCompiler.this.getType2(aci );                                                                }
            public void visitSuperConstructorInvocation    (Java.SuperConstructorInvocation     sci ) {       res[0] = UnitCompiler.this.getType2(sci );                                                                }
            public void visitPackage                       (Java.Package                        p   ) { try { res[0] = UnitCompiler.this.getType2(p   ); } catch (CompileException e) { throw new TunnelException(e); } }

            public void visitArrayType                     (Java.ArrayType                      at  ) { try { res[0] = UnitCompiler.this.getType2(at  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBasicType                     (Java.BasicType                      bt  ) {       res[0] = UnitCompiler.this.getType2(bt  );                                                                }
            public void visitReferenceType                 (Java.ReferenceType                  rt  ) { try { res[0] = UnitCompiler.this.getType2(rt  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitRvalueMemberType              (Java.RvalueMemberType               rmt ) { try { res[0] = UnitCompiler.this.getType2(rmt ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSimpleType                    (Java.SimpleType                     st  ) {       res[0] = UnitCompiler.this.getType2(st  );                                                                }

            public void visitArrayInitializer              (Java.ArrayInitializer               ai  ) { try { res[0] = UnitCompiler.this.getType2(ai  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayLength                   (Java.ArrayLength                    al  ) {       res[0] = UnitCompiler.this.getType2(al  );                                                                }
            public void visitAssignment                    (Java.Assignment                     a   ) { try { res[0] = UnitCompiler.this.getType2(a   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) { try { res[0] = UnitCompiler.this.getType2(uo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) { try { res[0] = UnitCompiler.this.getType2(bo  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitCast                          (Java.Cast                           c   ) { try { res[0] = UnitCompiler.this.getType2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) {       res[0] = UnitCompiler.this.getType2(cl  );                                                                }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) { try { res[0] = UnitCompiler.this.getType2(ce  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitConstantValue                 (Java.ConstantValue                  cv  ) {       res[0] = UnitCompiler.this.getType2(cv  );                                                                }
            public void visitCrement                       (Java.Crement                        c   ) { try { res[0] = UnitCompiler.this.getType2(c   ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitInstanceof                    (Java.Instanceof                     io  ) {       res[0] = UnitCompiler.this.getType2(io  );                                                                }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) { try { res[0] = UnitCompiler.this.getType2(mi  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) { try { res[0] = UnitCompiler.this.getType2(smi ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLiteral                       (Java.Literal                        l   ) {       res[0] = UnitCompiler.this.getType2(l   );                                                                }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) {       res[0] = UnitCompiler.this.getType2(naci);                                                                }
            public void visitNewArray                      (Java.NewArray                       na  ) { try { res[0] = UnitCompiler.this.getType2(na  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) { try { res[0] = UnitCompiler.this.getType2(nci ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) {       res[0] = UnitCompiler.this.getType2(pa  );                                                                }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) { try { res[0] = UnitCompiler.this.getType2(qtr ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitThisReference                 (Java.ThisReference                  tr  ) { try { res[0] = UnitCompiler.this.getType2(tr  ); } catch (CompileException e) { throw new TunnelException(e); } }

            public void visitAmbiguousName                 (Java.AmbiguousName                  an  ) { try { res[0] = UnitCompiler.this.getType2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression         (Java.ArrayAccessExpression          aae ) { try { res[0] = UnitCompiler.this.getType2(aae ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccess                   (Java.FieldAccess                    fa  ) { try { res[0] = UnitCompiler.this.getType2(fa  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitFieldAccessExpression         (Java.FieldAccessExpression          fae ) { try { res[0] = UnitCompiler.this.getType2(fae ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitLocalVariableAccess           (Java.LocalVariableAccess            lva ) {       res[0] = UnitCompiler.this.getType2(lva );                                                                }
            public void visitParenthesizedExpression       (Java.ParenthesizedExpression        pe  ) { try { res[0] = UnitCompiler.this.getType2(pe  ); } catch (CompileException e) { throw new TunnelException(e); } }
        };
        try {
            a.accept(av);
            return res[0] != null ? res[0] : this.iClassLoader.OBJECT;
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ IClass getType2(Java.SimpleType st) { return st.iClass; }
    /*private*/ IClass getType2(Java.BasicType bt) {
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
    /*private*/ IClass getType2(Java.ReferenceType rt) throws CompileException {
        Java.TypeDeclaration scopeTypeDeclaration = null;
        Java.CompilationUnit scopeCompilationUnit;
        {
            Java.Scope s = rt.scope;

            // Determine scope statement.
            if (s instanceof Java.Statement) {
                for (s = s.getEnclosingScope(); s instanceof Java.Statement; s = s.getEnclosingScope());
            }

            if (s instanceof Java.FunctionDeclarator) {
                s = s.getEnclosingScope();
            }

            // Determine scope class or interface.
            if (s instanceof Java.TypeDeclaration) {
                scopeTypeDeclaration = (Java.TypeDeclaration) s;
            }

            // Determine scope compilationUnit.
            while (!(s instanceof Java.CompilationUnit)) s = s.getEnclosingScope();
            scopeCompilationUnit = (Java.CompilationUnit) s;
        }

        if (rt.identifiers.length == 1) {

            // 6.5.5.1 Simple type name (single identifier).
            String simpleTypeName = rt.identifiers[0];

            // 6.5.5.1.1 Local class.
            for (Java.Scope s = rt.scope; s != null; s = s.getEnclosingScope()) {
                if (s instanceof Java.Block) {
                    Java.LocalClassDeclaration lcd = ((Java.Block) s).getLocalClassDeclaration(simpleTypeName);
                    if (lcd != null) return this.resolve(lcd);
                }
            }

            // 6.5.5.1.2 Member type.
            for (Java.Scope s = scopeTypeDeclaration; s != null; s = s.getEnclosingScope()) {
                if (s instanceof Java.TypeDeclaration) {
                    IClass mt = this.findMemberType(this.resolve((Java.AbstractTypeDeclaration) s), simpleTypeName, rt.getLocation());
                    if (mt != null) return mt;
                }
            }

            if (scopeCompilationUnit != null) {

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
                IClassLoader icl = this.iClassLoader;
                IClass result = icl.loadIClass(Descriptor.fromClassName(
                    pkg == null ?
                    simpleTypeName :
                    pkg + "." + simpleTypeName
                ));
                if (result != null) return result;
            }

            // 6.5.5.1.6 Type-import-on-demand declaration.
            {
                IClass importedClass = this.importTypeOnDemand(simpleTypeName, rt.getLocation());
                if (importedClass != null) return importedClass;
            }

            // 6.5.5.1.8 Give up.
            this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"", rt.getLocation());
            return this.iClassLoader.OBJECT;
        } else {

            // 6.5.5.2 Qualified type name (two or more identifiers).
            Java.Atom q = this.reclassifyName(rt.getLocation(), rt.scope, rt.identifiers, rt.identifiers.length - 1);
            String className;

            if (q instanceof Java.Package) {

                // 6.5.5.2.1 PACKAGE.CLASS
                className = Java.join(rt.identifiers, ".");
            } else {

                // 6.5.5.2.2 CLASS.CLASS (member type)
                className = (
                    Descriptor.toClassName(this.getType(this.toTypeOrCE(q)).getDescriptor())
                    + '$'
                    + rt.identifiers[rt.identifiers.length - 1]
                );
            }
            IClass result = this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
            if (result != null) return result;

            this.compileError("Class \"" + className + "\" not found", rt.getLocation());
            return this.iClassLoader.OBJECT;
        }
    }
    /*private*/ IClass getType2(Java.RvalueMemberType rvmt) throws CompileException {
        IClass rvt = this.getType(rvmt.rvalue);
        IClass memberType = this.findMemberType(rvt, rvmt.identifier, rvmt.getLocation());
        if (memberType == null) this.compileError("\"" + rvt + "\" has no member type \"" + rvmt.identifier + "\"", rvmt.getLocation());
        return memberType;
    }
    /*private*/ IClass getType2(Java.ArrayType at) throws CompileException {
        return this.getArrayType(this.getType(at.componentType));
    }
    /*private*/ IClass getType2(Java.AmbiguousName an) throws CompileException {
        return this.getType(this.reclassify(an));
    }
    /*private*/ IClass getType2(Java.Package p) throws CompileException {
        this.compileError("Unknown variable or type \"" + p.name + "\"", p.getLocation());
        return this.iClassLoader.OBJECT;
    }
    /*private*/ IClass getType2(Java.LocalVariableAccess lva) {
        return lva.localVariable.type;
    }
    /*private*/ IClass getType2(Java.FieldAccess fa) throws CompileException {
        return fa.field.getType();
    }
    /*private*/ IClass getType2(Java.ArrayLength al) {
        return IClass.INT;
    }
    /*private*/ IClass getType2(Java.ThisReference tr) throws CompileException {
        return this.getIClass(tr);
    }
    /*private*/ IClass getType2(Java.QualifiedThisReference qtr) throws CompileException {
        return this.getTargetIClass(qtr);
    }
    /*private*/ IClass getType2(Java.ClassLiteral cl) {
        return this.iClassLoader.CLASS;
    }
    /*private*/ IClass getType2(Java.Assignment a) throws CompileException {
        return this.getType(a.lhs);
    }
    /*private*/ IClass getType2(Java.ConditionalExpression ce) throws CompileException {
        return this.getType(ce.mhs);
    }
    /*private*/ IClass getType2(Java.Crement c) throws CompileException {
        return this.getType(c.operand);
    }
    /*private*/ IClass getType2(Java.ArrayAccessExpression aae) throws CompileException {
        return this.getType(aae.lhs).getComponentType();
    }
    /*private*/ IClass getType2(Java.FieldAccessExpression fae) throws CompileException {
        this.determineValue(fae);
        return this.getType(fae.value);
    }
    /*private*/ IClass getType2(Java.UnaryOperation uo) throws CompileException {
        if (uo.operator == "!") return IClass.BOOLEAN;
        if (uo.operator == "+") return this.getType(uo.operand);
        if (
            uo.operator == "-" ||
            uo.operator == "~"
        ) return this.unaryNumericPromotionType(uo, this.getType(uo.operand));

        this.compileError("Unexpected operator \"" + uo.operator + "\"", uo.getLocation());
        return IClass.BOOLEAN;
    }
    /*private*/ IClass getType2(Java.Instanceof io) {
        return IClass.BOOLEAN;
    }
    /*private*/ IClass getType2(Java.BinaryOperation bo) throws CompileException {
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
        ) return (
            this.getType(bo.lhs) == IClass.BOOLEAN ?
            IClass.BOOLEAN :
            this.binaryNumericPromotionType(
                (Java.Locatable) bo,
                this.getType(bo.lhs),
                this.getType(bo.rhs)
            )
        );

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
            IClass lhsType = this.getType(((Java.Rvalue) ops.next()));
            if (bo.op == "+" && lhsType == icl.STRING) return icl.STRING;

            // Determine the expression type.
            do {
                IClass rhsType = this.getType(((Java.Rvalue) ops.next()));
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
            return this.unaryNumericPromotionType((Java.Located) bo, lhsType);
        }

        this.compileError("Unexpected operator \"" + bo.op + "\"", bo.getLocation());
        return this.iClassLoader.OBJECT;
    }
    /*private*/ IClass getType2(Java.Cast c) throws CompileException {
        return this.getType(c.targetType);
    }
    /*private*/ IClass getType2(Java.ParenthesizedExpression pe) throws CompileException {
        return this.getType(pe.value);
    }
    /*private*/ IClass getType2(Java.ConstructorInvocation ci) {
        throw new RuntimeException();
    }
    /*private*/ IClass getType2(Java.MethodInvocation mi) throws CompileException {
        if (mi.iMethod == null) {
            mi.iMethod = this.findIMethod(mi);
        }
        return mi.iMethod.getReturnType();
    }
    /*private*/ IClass getType2(Java.SuperclassMethodInvocation scmi) throws CompileException {
        return this.findIMethod(scmi).getReturnType();
    }
    /*private*/ IClass getType2(Java.NewClassInstance nci) throws CompileException {
        if (nci.iClass == null) nci.iClass = this.getType(nci.type);
        return nci.iClass;
    }
    /*private*/ IClass getType2(Java.NewAnonymousClassInstance naci) {
        return this.resolve(naci.anonymousClassDeclaration);
    }
    /*private*/ IClass getType2(Java.ParameterAccess pa) {
        return ((Java.LocalVariable) pa.declaringFunction.parameters.get(pa.name)).type;
    }
    /*private*/ IClass getType2(Java.NewArray na) throws CompileException {
        IClass res = this.getType(na.type);
        return this.getArrayType(res, na.dimExprs.length + na.dims);
    }
    /*private*/ IClass getType2(Java.ArrayInitializer ai) throws CompileException {
        return this.getType(ai.arrayType);
    }
    /*private*/ IClass getType2(Java.Literal l) {
        if (l.value instanceof Integer  ) return IClass.INT;
        if (l.value instanceof Long     ) return IClass.LONG;
        if (l.value instanceof Float    ) return IClass.FLOAT;
        if (l.value instanceof Double   ) return IClass.DOUBLE;
        if (l.value instanceof String   ) return this.iClassLoader.STRING;
        if (l.value instanceof Character) return IClass.CHAR;
        if (l.value instanceof Boolean  ) return IClass.BOOLEAN;
        if (l.value == null             ) return IClass.VOID;
        throw new RuntimeException();
    }
    /*private*/ IClass getType2(Java.ConstantValue cv) {
        IClass res = (
            cv.constantValue instanceof Integer            ? IClass.INT     :
            cv.constantValue instanceof Long               ? IClass.LONG    :
            cv.constantValue instanceof Float              ? IClass.FLOAT   :
            cv.constantValue instanceof Double             ? IClass.DOUBLE  :
            cv.constantValue instanceof String             ? this.iClassLoader.STRING  :
            cv.constantValue instanceof Character          ? IClass.CHAR    :
            cv.constantValue instanceof Boolean            ? IClass.BOOLEAN :
            cv.constantValue == Java.Rvalue.CONSTANT_VALUE_NULL ? IClass.VOID    :
            null
        );
        if (res == null) throw new RuntimeException();
        return res;
    }

    // ---------------- Atom.isType() ---------------

    private boolean isType(Java.Atom a) throws CompileException {
        final boolean[] res = new boolean[1];
        Visitor.AtomVisitor av = new Visitor.AtomVisitor() {
            public void visitAlternateConstructorInvocation(Java.AlternateConstructorInvocation aci ) {       res[0] = UnitCompiler.this.isType2(aci );                                                                }
            public void visitSuperConstructorInvocation    (Java.SuperConstructorInvocation     sci ) {       res[0] = UnitCompiler.this.isType2(sci );                                                                }
            public void visitPackage                       (Java.Package                        p   ) {       res[0] = UnitCompiler.this.isType2(p   );                                                                }

            public void visitArrayType                     (Java.ArrayType                      at  ) {       res[0] = UnitCompiler.this.isType2(at  );                                                                }
            public void visitBasicType                     (Java.BasicType                      bt  ) {       res[0] = UnitCompiler.this.isType2(bt  );                                                                }
            public void visitReferenceType                 (Java.ReferenceType                  rt  ) {       res[0] = UnitCompiler.this.isType2(rt  );                                                                }
            public void visitRvalueMemberType              (Java.RvalueMemberType               rmt ) {       res[0] = UnitCompiler.this.isType2(rmt );                                                                }
            public void visitSimpleType                    (Java.SimpleType                     st  ) {       res[0] = UnitCompiler.this.isType2(st  );                                                                }

            public void visitArrayInitializer              (Java.ArrayInitializer               ai  ) {       res[0] = UnitCompiler.this.isType2(ai  );                                                                }
            public void visitArrayLength                   (Java.ArrayLength                    al  ) {       res[0] = UnitCompiler.this.isType2(al  );                                                                }
            public void visitAssignment                    (Java.Assignment                     a   ) {       res[0] = UnitCompiler.this.isType2(a   );                                                                }
            public void visitUnaryOperation                (Java.UnaryOperation                 uo  ) {       res[0] = UnitCompiler.this.isType2(uo  );                                                                }
            public void visitBinaryOperation               (Java.BinaryOperation                bo  ) {       res[0] = UnitCompiler.this.isType2(bo  );                                                                }
            public void visitCast                          (Java.Cast                           c   ) {       res[0] = UnitCompiler.this.isType2(c   );                                                                }
            public void visitClassLiteral                  (Java.ClassLiteral                   cl  ) {       res[0] = UnitCompiler.this.isType2(cl  );                                                                }
            public void visitConditionalExpression         (Java.ConditionalExpression          ce  ) {       res[0] = UnitCompiler.this.isType2(ce  );                                                                }
            public void visitConstantValue                 (Java.ConstantValue                  cv  ) {       res[0] = UnitCompiler.this.isType2(cv  );                                                                }
            public void visitCrement                       (Java.Crement                        c   ) {       res[0] = UnitCompiler.this.isType2(c   );                                                                }
            public void visitInstanceof                    (Java.Instanceof                     io  ) {       res[0] = UnitCompiler.this.isType2(io  );                                                                }
            public void visitMethodInvocation              (Java.MethodInvocation               mi  ) {       res[0] = UnitCompiler.this.isType2(mi  );                                                                }
            public void visitSuperclassMethodInvocation    (Java.SuperclassMethodInvocation     smi ) {       res[0] = UnitCompiler.this.isType2(smi );                                                                }
            public void visitLiteral                       (Java.Literal                        l   ) {       res[0] = UnitCompiler.this.isType2(l   );                                                                }
            public void visitNewAnonymousClassInstance     (Java.NewAnonymousClassInstance      naci) {       res[0] = UnitCompiler.this.isType2(naci);                                                                }
            public void visitNewArray                      (Java.NewArray                       na  ) {       res[0] = UnitCompiler.this.isType2(na  );                                                                }
            public void visitNewClassInstance              (Java.NewClassInstance               nci ) {       res[0] = UnitCompiler.this.isType2(nci );                                                                }
            public void visitParameterAccess               (Java.ParameterAccess                pa  ) {       res[0] = UnitCompiler.this.isType2(pa  );                                                                }
            public void visitQualifiedThisReference        (Java.QualifiedThisReference         qtr ) {       res[0] = UnitCompiler.this.isType2(qtr );                                                                }
            public void visitThisReference                 (Java.ThisReference                  tr  ) {       res[0] = UnitCompiler.this.isType2(tr  );                                                                }

            public void visitAmbiguousName                 (Java.AmbiguousName                  an  ) { try { res[0] = UnitCompiler.this.isType2(an  ); } catch (CompileException e) { throw new TunnelException(e); } }
            public void visitArrayAccessExpression         (Java.ArrayAccessExpression          aae ) {       res[0] = UnitCompiler.this.isType2(aae );                                                                }
            public void visitFieldAccess                   (Java.FieldAccess                    fa  ) {       res[0] = UnitCompiler.this.isType2(fa  );                                                                }
            public void visitFieldAccessExpression         (Java.FieldAccessExpression          fae ) {       res[0] = UnitCompiler.this.isType2(fae );                                                                }
            public void visitLocalVariableAccess           (Java.LocalVariableAccess            lva ) {       res[0] = UnitCompiler.this.isType2(lva );                                                                }
            public void visitParenthesizedExpression       (Java.ParenthesizedExpression        pe  ) {       res[0] = UnitCompiler.this.isType2(pe  );                                                                }
        };
        try {
            a.accept(av);
            return res[0];
        } catch (TunnelException e) {
            throw (CompileException) e.getDelegate();
        }
    }
    /*private*/ boolean isType2(Java.Atom a) {
        return a instanceof Java.Type;
    }
    /*private*/ boolean isType2(Java.AmbiguousName an) throws CompileException {
        return this.isType(this.reclassify(an));
    }

    /**
     * Check whether the given {@link IClass.IMember} is accessible in the given context,
     * according to JLS 6.6. Issues a {@link #compileError(String)} if not.
     */
    private void checkAccessible(
        IClass.IMember      member,
        Java.BlockStatement contextBlockStatement
    ) throws CompileException {

        // At this point, the member is PUBLIC, DEFAULT, PROECTEDED or PRIVATE accessible.

        // PUBLIC members are always accessible.
        if (member.getAccess() == Access.PUBLIC) return;

        // At this point, the member is DEFAULT, PROECTEDED or PRIVATE accessible.

        // Determine the declaring classes for the member and the context block statement.
        IClass iClassDeclaringMember = member.getDeclaringIClass();
        IClass iClassDeclaringContextBlockStatement;
        for (Java.Scope s = contextBlockStatement.getEnclosingScope();; s = s.getEnclosingScope()) {
            if (s instanceof Java.TypeDeclaration) {
                iClassDeclaringContextBlockStatement = this.resolve((Java.TypeDeclaration) s);
                break;
            }
        }

        // Access is always allowed for block statements declared in the same class as the member.
        if (iClassDeclaringContextBlockStatement == iClassDeclaringMember) return;

        // Determine the enclosing top level class declarations for the member and the context
        // block statement.
        IClass topLevelIClassEnclosingMember = iClassDeclaringMember;
        for (IClass c = iClassDeclaringMember; c != null; c = c.getDeclaringIClass()) {
            topLevelIClassEnclosingMember = c;
        }
        IClass topLevelIClassEnclosingContextBlockStatement = iClassDeclaringContextBlockStatement;
        for (IClass c = iClassDeclaringContextBlockStatement; c != null; c = c.getDeclaringIClass()) {
            topLevelIClassEnclosingContextBlockStatement = c;
        }

        // Check whether the member and the context block statement are enclosed by the same
        // top-level type.
        if (topLevelIClassEnclosingMember == topLevelIClassEnclosingContextBlockStatement) {
            if (
                member instanceof IClass.IInvocable
                && member.getAccess() == Access.PRIVATE
                && (member instanceof IClass.IConstructor || !((IClass.IMethod) member).isStatic())
            ) {
                this.compileError("Compiler limitation: Access to private constructor or non-static method \"" + member + "\" declared in the same enclosing top-level type \"" + topLevelIClassEnclosingContextBlockStatement + "\" not supported. It is recommended to change its declaration from \"private\" to \"/*private*/\".", contextBlockStatement.getLocation());
            }
            return;
        }
        if (member.getAccess() == Access.PRIVATE) {
            this.compileError("Private member \"" + member + "\" cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\".", contextBlockStatement.getLocation());
            return;
        }

        // At this point, the member is DEFAULT or PROECTEDED accessible.

        // Determine the packages for the member and the context block statement.
        String memberPackage;
        {
            String d = topLevelIClassEnclosingMember.getDescriptor();
            int idx = d.lastIndexOf('.');
            memberPackage = idx == -1 ? null : d.substring(0, idx);
        }
        String contextBlockStatementPackage;
        {
            String d = topLevelIClassEnclosingContextBlockStatement.getDescriptor();
            int idx = d.lastIndexOf('.');
            contextBlockStatementPackage = idx == -1 ? null : d.substring(0, idx);
        }

        // Check whether the member and the context block statement are declared in the same
        // package.
        if (memberPackage == null ? contextBlockStatementPackage == null : memberPackage.equals(contextBlockStatementPackage)) {
            return;
        }
        if (member.getAccess() == Access.DEFAULT) {
            this.compileError("Member \"" + member + "\" with \"" + member.getAccess() + "\" visibility cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\".", contextBlockStatement.getLocation());
            return;
        }

        // At this point, the member is PROECTEDED accessible.

        // Check whether the class declaring the context block statement is a subclass of the
        // class declaring the member.
        for (IClass c = iClassDeclaringContextBlockStatement;; c = c.getDeclaringIClass()) {
            if (c == null) {
                this.compileError("Protected member \"" + member + "\" cannot be accessed from type \"" + iClassDeclaringContextBlockStatement + "\", which is neither declared in the same package as or is a subclass of \"" + iClassDeclaringMember + "\".", contextBlockStatement.getLocation());
                return;
            }
            if (c == iClassDeclaringMember) return;
        }
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
            return new Java.Rvalue(a.getLocation()) {
                public IClass compileGet() { return UnitCompiler.this.iClassLoader.OBJECT; }
                public String toString() { return a.toString(); }
                public void accept(Visitor.AtomVisitor visitor) {}
                public void accept(Visitor.RvalueVisitor visitor) {}
            };
        }
        return result;
    }
    public final Java.Lvalue toLvalueOrCE(final Java.Atom a) throws CompileException {
        Java.Lvalue result = a.toLvalue();
        if (result == null) {
            this.compileError("Expression \"" + a.toString() + "\" is not an lvalue", a.getLocation());
            return new Java.Lvalue(a.getLocation()) {
                public IClass compileGet() { return UnitCompiler.this.iClassLoader.OBJECT; }
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
        for (Iterator it = cd.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
            IClass.IField sf = (IClass.IField) it.next();
            Java.LocalVariable syntheticParameter = (Java.LocalVariable) cd.syntheticParameters.get(sf.getName());
            if (syntheticParameter == null) throw new RuntimeException("SNO: Synthetic parameter for synthetic field \"" + sf.getName() + "\" not found");
            try {
                this.compile(new Java.ExpressionStatement(
                    new Java.Assignment(  // rvalue
                        cd.getLocation(),             // location
                        new Java.FieldAccess(         // lhs
                            cd.getLocation(),       // location
                            cd.optionalBody, // enclosingBlockStatement
                            new Java.ThisReference( // lhs
                                cd.getLocation(),              // location
                                (Java.Scope) cd.declaringClass // scope
                            ),
                            sf                      // field
                        ),
                        "=",                          // operator
                        new Java.LocalVariableAccess( // rhs
                            cd.getLocation(),  // location
                            syntheticParameter // localVariable
                        )
                    ),
                    (Java.Scope) cd       // enclosingScope
                ));
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
        for (Iterator it = cd.declaringClass.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
            Java.TypeBodyDeclaration tbd = (Java.TypeBodyDeclaration) it.next();
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
        Java.Located  located,
        IClass   lhsType,
        String   operator,
        Java.Rvalue   rhs
    ) throws CompileException {
        return this.compileArithmeticOperation(
            located,
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
        Java.Located  located,
        IClass   type,
        Iterator operands,
        String   operator
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
                        IClass promotedType = this.binaryNumericPromotion(located, type, convertLhsInserter, rhsType);
                        if (promotedType == IClass.INT) {
                            this.writeOpcode(located, iopcode);
                        } else
                        if (promotedType == IClass.LONG) {
                            this.writeOpcode(located, iopcode + 1);
                        } else
                        {
                            this.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"", located.getLocation());
                        }
                        type = promotedType;
                    } else
                    if (
                        type == IClass.BOOLEAN &&
                        rhsType == IClass.BOOLEAN
                    ) {
                        this.writeOpcode(located, iopcode);
                        type = IClass.BOOLEAN;
                    } else
                    {
                        this.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"", located.getLocation());
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

                    if (type != null) this.stringConversion(located, type);

                    do {
                        Object cv = this.getConstantValue(operand);
                        if (cv == null) {
                            this.stringConversion(located, this.compileGetValue(operand));
                            operand = operands.hasNext() ? (Java.Rvalue) operands.next() : null;
                        } else {
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
                            } else {
                                operand = null;
                            }
                            this.pushConstant(located, cv.toString());
                        }

                        // Concatenate.
                        if (type != null) {
                            this.writeOpcode(located, Opcode.INVOKEVIRTUAL);
                            this.writeConstantMethodrefInfo(
                                located,
                                Descriptor.STRING,                                // classFD
                                "concat",                                         // methodName
                                "(" + Descriptor.STRING + ")" + Descriptor.STRING // methodMD
                            );
                        }
                        type = this.iClassLoader.STRING;
                    } while (operand != null);
                    return type;
                }

                if (type == null) {
                    type = this.compileGetValue(operand);
                } else {
                    CodeContext.Inserter convertLhsInserter = this.codeContext.newInserter();
                    IClass rhsType = this.compileGetValue(operand);

                    type = this.binaryNumericPromotion(located, type, convertLhsInserter, rhsType);

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
                        this.compileError("Unexpected promoted type \"" + type + "\"", located.getLocation());
                        opcode = iopcode;
                    }
                    this.writeOpcode(located, opcode);
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
                        promotedLhsType = this.unaryNumericPromotion(located, type);
                    } finally {
                        this.codeContext.popInserter();
                    }
                    if (promotedLhsType != IClass.INT && promotedLhsType != IClass.LONG) this.compileError("Shift operation not allowed on operand type \"" + type + "\"", located.getLocation());

                    IClass promotedRhsType = this.unaryNumericPromotion(located, rhsType);
                    if (promotedRhsType != IClass.INT && promotedRhsType != IClass.LONG) this.compileError("Shift distance of type \"" + rhsType + "\" is not allowed", located.getLocation());

                    if (promotedRhsType == IClass.LONG) this.writeOpcode(located, Opcode.L2I);

                    this.writeOpcode(located, promotedLhsType == IClass.LONG ? iopcode + 1 : iopcode);
                    type = promotedLhsType;
                }
            } while (operands.hasNext());
            return type;
        }

        throw new RuntimeException("Unexpected operator \"" + operator + "\"");
    }

    /**
     * Convert object of type "sourceType" to type "String". JLS2 15.18.1.1
     */
    private void stringConversion(
        Java.Located located,
        IClass  sourceType
    ) {
        this.writeOpcode(located, Opcode.INVOKESTATIC);
        this.writeConstantMethodrefInfo(
            located,
            Descriptor.STRING, // classFD
            "valueOf",         // methodName
            "(" + (            // methodMD
                sourceType == IClass.BOOLEAN ||
                sourceType == IClass.CHAR    ||
                sourceType == IClass.LONG    ||
                sourceType == IClass.FLOAT   ||
                sourceType == IClass.DOUBLE ? sourceType.getDescriptor() :
                sourceType == IClass.BYTE  ||
                sourceType == IClass.SHORT ||
                sourceType == IClass.INT ? Descriptor.INT :
                Descriptor.OBJECT
            ) + ")" + Descriptor.STRING
        );
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     * Notice: This method is used both for explicit constructor invocation (first statement of
     * a constructor body) and implicit constructor invocation (right after NEW).
     * @param optionalEnclosingInstance Used if the target class is an inner class
     */
    private void invokeConstructor(
        Java.Located  located,
        Java.Scope    scope,
        Java.Rvalue   optionalEnclosingInstance,
        IClass   targetClass,
        Java.Rvalue[] arguments
    ) throws CompileException {

        // Find constructors.
        IClass.IConstructor[] iConstructors = targetClass.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new RuntimeException();

        IClass.IConstructor iConstructor = (IClass.IConstructor) this.findMostSpecificIInvocable(
            located,
            iConstructors, // iInvocables
            arguments      // arguments
        );

        // Check exceptions that the constructor may throw.
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        for (int i = 0; i < thrownExceptions.length; ++i) {
            this.checkThrownException(
                located,
                thrownExceptions[i],
                scope
            );
        }

        // Pass enclosing instance as a synthetic parameter.
        IClass outerIClass = targetClass.getOuterIClass();
        if (outerIClass != null) {
            if (optionalEnclosingInstance == null) this.compileError("Enclosing instance for initialization of inner class \"" + targetClass + "\" missing", located.getLocation());
            IClass eiic = this.compileGetValue(optionalEnclosingInstance);
            if (!outerIClass.isAssignableFrom(eiic)) this.compileError("Type of enclosing instance (\"" + eiic + "\") is not assignable to \"" + outerIClass + "\"", located.getLocation());
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
                if (syntheticFields.length > 0) throw new RuntimeException();
            } else {
                Java.ClassDeclaration scopeClassDeclaration = (Java.ClassDeclaration) scopeTypeDeclaration;
                for (int i = 0; i < syntheticFields.length; ++i) {
                    IClass.IField sf = syntheticFields[i];
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTBD instanceof Java.MethodDeclarator) {
                            this.load(located, this.resolve(scopeClassDeclaration), 0);
                            this.writeOpcode(located, Opcode.GETFIELD);
                            this.writeConstantFieldrefInfo(
                                located,
                                this.resolve(scopeClassDeclaration).getDescriptor(), // classFD
                                sf.getName(),                                        // fieldName
                                sf.getDescriptor()                                   // fieldFD
                            );
                        } else
                        if (scopeTBD instanceof Java.ConstructorDeclarator) {
                            Java.ConstructorDeclarator constructorDeclarator = (Java.ConstructorDeclarator) scopeTBD;
                            Java.LocalVariable syntheticParameter = (Java.LocalVariable) constructorDeclarator.syntheticParameters.get(sf.getName());
                            if (syntheticParameter == null) {
                                this.compileError("Compiler limitation: Constructor cannot access local variable \"" + sf.getName().substring(4) + "\" declared in an enclosing block because none of the methods accesses it. As a workaround, declare a dummy method that accesses the local variable.", located.getLocation());
                                this.writeOpcode(located, Opcode.ACONST_NULL);
                            } else {
                                this.load(located, syntheticParameter);
                            }
                        } else {
                            this.compileError("Compiler limitation: Initializers cannot access local variables declared in an enclosing block.", located.getLocation());
                            this.writeOpcode(located, Opcode.ACONST_NULL);
                        }
                    } else {
                        String localVariableName = sf.getName().substring(4);
                        Java.LocalVariable lv = null;
                        Java.Scope s;
                        for (s = scope; s instanceof Java.Block; s = s.getEnclosingScope()) {

                            // Local variable?
                            lv = (Java.LocalVariable) ((Java.Block) s).localVariables.get(localVariableName);
                            if (lv != null) break;
                        }
                        if (lv == null) {
                            while (!(s instanceof Java.FunctionDeclarator)) s = s.getEnclosingScope();
                            Java.FunctionDeclarator fd = (Java.FunctionDeclarator) s;

                            // Function parameter?
                            lv = (Java.LocalVariable) fd.parameters.get(localVariableName);
                        }
                        if (lv == null) throw new RuntimeException("SNO: Synthetic field \"" + sf.getName() + "\" neither maps a synthetic field of an enclosing instance nor a local variable");
                        this.load(located, lv);
                    }
                }
            }
        }

        // Evaluate constructor arguments.
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        for (int i = 0; i < arguments.length; ++i) {
            this.assignmentConversion(
                (Java.Located) located,             // located
                this.compileGetValue(arguments[i]), // sourceType
                parameterTypes[i],                  // targetType
                this.getConstantValue(arguments[i]) // optionalConstantValue
            );
        }

        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()" prepended with the
        // synthetic parameters.
        this.writeOpcode(located, Opcode.INVOKESPECIAL);
        this.writeConstantMethodrefInfo(
            located,
            targetClass.getDescriptor(), // classFD
            "<init>",                    // methodName
            iConstructor.getDescriptor() // methodMD
        );
    }

    /*package*/ IClass.IField[] getIFields(final Java.FieldDeclaration fd) {
        IClass.IField[] res = new IClass.IField[fd.variableDeclarators.length];
        for (int i = 0; i < res.length; ++i) {
            final Java.VariableDeclarator vd = fd.variableDeclarators[i];
            res[i] = this.resolve(fd.declaringType).new IField() {

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
                    IClass res2 = UnitCompiler.this.getType(fd.type);
                    return UnitCompiler.this.getArrayType(res2, vd.brackets);
                }
                public String getName() { return vd.name; }
                public Object getConstantValue() throws CompileException {
                    if (
                        (fd.modifiers & Mod.FINAL) != 0 &&
                        vd.optionalInitializer != null
                    ) {
                        Object constantInitializerValue = UnitCompiler.this.getConstantValue(vd.optionalInitializer);
                        if (constantInitializerValue != null) return UnitCompiler.this.assignmentConversion(
                            (Java.Located) vd.optionalInitializer, // located
                            constantInitializerValue,              // value
                            this.getType()                         // targetType
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
    Java.Rvalue getNonConstantFinalInitializer(Java.FieldDeclaration fd, Java.VariableDeclarator vd) throws CompileException {

        // Check if optional initializer exists.
        if (vd.optionalInitializer == null) return null;

        // Check if initializer is constant-final.
        if (
            (fd.modifiers & Mod.STATIC) != 0 &&
            (fd.modifiers & Mod.FINAL) != 0 &&
            this.getConstantValue(vd.optionalInitializer) != null
        ) return null;

        return vd.optionalInitializer;
    }

    private Java.Atom reclassify(Java.AmbiguousName an) throws CompileException {
        if (an.reclassified == null) {
            an.reclassified = this.reclassifyName(
                an.getLocation(),
                an.scope,
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
            IClass result = this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
            if (result != null) return new Java.SimpleType(location, result);

            return new Java.Package(location, className);
        }

        // 6.5.2.2.3.2 EXPRESSION.length
        if (rhs.equals("length") && this.getType(lhs).isArray()) {
            return new Java.ArrayLength(location, this.toRvalueOrCE(lhs));
        }

        IClass lhsType = this.getType(lhs);

        // Notice: Don't need to check for 6.5.2.2.2.1 TYPE.METHOD and 6.5.2.2.3.1
        // EXPRESSION.METHOD here because that has been done before.

        {
            IClass.IField field = this.findIField(lhsType, rhs, location);
            if (field != null) {
                // 6.5.2.2.2.2 TYPE.FIELD
                // 6.5.2.2.3.2 EXPRESSION.FIELD
                return new Java.FieldAccess(
                    location,
                    (Java.BlockStatement) scope, // enclosingBlockStatement
                    lhs,
                    field
                );
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

        // Determine scope type body declaration, type and compilation unit.
        Java.TypeBodyDeclaration     scopeTBD = null;
        Java.AbstractTypeDeclaration scopeTypeDeclaration = null;
        Java.CompilationUnit         scopeCompilationUnit;
        {
            Java.Scope s = scope;
            while (s instanceof Java.Statement && !(s instanceof Java.TypeBodyDeclaration)) s = s.getEnclosingScope();
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

        // 6.5.2.BL1.B1.B1.1/6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2/6.5.6.1.1 Parameter.
        {
            Java.InnerClassDeclaration icd = null;
            Java.BlockStatement ebs = null;
            for (Java.Scope s = scope; !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
                if (s instanceof Java.InnerClassDeclaration) {
                    icd = (Java.InnerClassDeclaration) s;
                    continue;
                }
                if (ebs == null && s instanceof Java.BlockStatement) {
                    ebs = (Java.BlockStatement) s;
                }

                Java.LocalVariable lv = null;
                if (s instanceof Java.Block) {
                    Java.Block b = (Java.Block) s;
                    lv = (Java.LocalVariable) b.localVariables.get(identifier);
                } else
                if (s instanceof Java.FunctionDeclarator) {
                    Java.FunctionDeclarator fd = (Java.FunctionDeclarator) s;
                    lv = (Java.LocalVariable) fd.parameters.get(identifier);
                }
                if (lv == null) continue;

                if (icd == null) {
                    return new Java.LocalVariableAccess(location, lv);
                } else {
                    if (!lv.finaL) this.compileError("Cannot access non-final local variable \"" + identifier + "\" from inner class");
                    final IClass lvType = lv.type;
                    IClass.IField iField = new SimpleIField(
                        this.resolve(icd),
                        "val$" + identifier,
                        lvType
                    );
                    icd.defineSyntheticField(iField);
                    return new Java.FieldAccess(
                        location,                        // location
                        ebs,                             // enclosingBlockStatement
                        new Java.QualifiedThisReference( // lhs
                            location,                                        // location
                            (Java.Scope) scopeTBD,                           // scope
                            new Java.SimpleType(location, this.resolve(icd)) // qualification
                        ),
                        iField                           // field
                    );
                }
            }
        }

        // 6.5.2.BL1.B1.B1.3/6.5.6.1.2.1 Field.
        Java.BlockStatement ebs = null;
        for (Java.Scope s = scope; !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof Java.BlockStatement) {
                ebs = (Java.BlockStatement) s;
                continue;
            }
            if (s instanceof Java.TypeDeclaration) {
                final IClass etd = UnitCompiler.this.resolve((Java.AbstractTypeDeclaration) s);
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

                    Java.Type ct = new Java.SimpleType(scopeTypeDeclaration.getLocation(), (IClass) etd);
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
                            lhs = new Java.QualifiedThisReference(location, (Java.Scope) scopeTBD, ct);
                        }
                    }
                    return new Java.FieldAccess(
                        location,
                        ebs, // enclosingBlockStatement
                        lhs,
                        f
                    );
                }
            }
        }

        // Hack: "java" MUST be a package, not a class.
        if (identifier.equals("java")) return new Java.Package(location, identifier);

        // 6.5.2.BL1.B1.B2.1 Local class.
        for (Java.Scope s = scope; s instanceof Java.Block; s = s.getEnclosingScope()) {
            Java.Block b = (Java.Block) s;
            Java.LocalClassDeclaration lcd = b.getLocalClassDeclaration(identifier);
            if (lcd != null) return new Java.SimpleType(location, this.resolve(lcd));
        }

        // 6.5.2.BL1.B1.B2.2 Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = this.findMemberType(UnitCompiler.this.resolve(scopeTypeDeclaration), identifier, location);
            if (memberType != null) return new Java.SimpleType(location, memberType);
        }

        // 6.5.2.BL1.B1.B3.1 Single-type-import.
        if (scopeCompilationUnit != null) {
            IClass iClass = this.importSingleType(identifier, location);
            if (iClass != null) return new Java.SimpleType(location, iClass);
        }

        // 6.5.2.BL1.B1.B3.2 Package member class/interface declared in this compilation unit.
        if (scopeCompilationUnit != null) {
            Java.PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(identifier);
            if (pmtd != null) return new Java.SimpleType(location, this.resolve((Java.AbstractTypeDeclaration) pmtd));
        }

        // 6.5.2.BL1.B1.B4 Class or interface declared in same package.
        if (scopeCompilationUnit != null) {
            String className = (
                scopeCompilationUnit.optionalPackageDeclaration == null ?
                identifier :
                scopeCompilationUnit.optionalPackageDeclaration.packageName + '.' + identifier
            );
            IClass result = this.iClassLoader.loadIClass(Descriptor.fromClassName(className));
            if (result != null) return new Java.SimpleType(location, result);
        }

        // 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.
        if (scopeCompilationUnit != null) {
            IClass importedClass = this.importTypeOnDemand(identifier, location);
            if (importedClass != null) {
                return new Java.SimpleType(location, importedClass);
            }
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
                fae.enclosingBlockStatement, // enclosingBlockStatement
                fae.lhs,
                iField
            );
        }
    }

    /**
     * Find named methods of "targetType", examine the argument types and choose the
     * most specific method. Check that only the allowed exceptions are thrown.
     * <p>
     * Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     *
     * @return The selected {@link IClass.IMethod} or <code>null</code>
     */
    private IClass.IMethod findIMethod(Java.MethodInvocation mi) throws CompileException {
        for (Java.Scope s = mi.enclosingBlockStatement; !(s instanceof Java.CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof Java.TypeDeclaration) {
                Java.TypeDeclaration td = (Java.TypeDeclaration) s;

                // Find methods with specified name.
                IClass.IMethod iMethod = this.findIMethod(
                    (Java.Located) mi, // located
                    (                  // targetType
                        mi.optionalTarget == null ?
                        this.resolve(td) :
                        this.getType(mi.optionalTarget)
                    ),
                    mi.methodName,     // methodName
                    mi.arguments       // arguments
                );

                // Check exceptions that the method may throw.
                IClass[] thrownExceptions = iMethod.getThrownExceptions();
                for (int i = 0; i < thrownExceptions.length; ++i) {
                    this.checkThrownException(
                        (Java.Located) mi,   // located
                        thrownExceptions[i], // type
                        mi.enclosingBlockStatement             // scope
                    );
                }

                return iMethod;
            }
        }
        return null;
    }

    /**
     * Find {@link IClass.IMethod} by name and argument types. If more than one such
     * method exists, choose the most specific one (JLS 15.11.2).
     * <p>
     * Notice that the returned {@link IClass.IMethod} may be declared in an enclosing type.
     */
    private IClass.IMethod findIMethod(
        Java.Located  located,
        IClass        targetType,
        final String  methodName,
        Java.Rvalue[] arguments
    ) throws CompileException {
        for (IClass ic = targetType; ic != null; ic = ic.getDeclaringIClass()) {
            List l = new ArrayList();
            this.getIMethods(ic, methodName, l);
            if (l.size() > 0) {

                // Determine arguments' types, choose the most specific method
                IClass.IMethod iMethod = (IClass.IMethod) this.findMostSpecificIInvocable(
                    located,
                    (IClass.IMethod[]) l.toArray(new IClass.IMethod[l.size()]), // iInvocables
                    arguments                                                   // arguments
                );
                return iMethod;
            }
        }
        this.compileError("Class \"" + targetType + "\" has no method named \"" + methodName + "\"", located.getLocation());
        return targetType.new IMethod() {
            public String   getName()                                     { return methodName; }
            public IClass   getReturnType() throws CompileException       { return IClass.INT; }
            public boolean  isStatic()                                    { return false; }
            public boolean  isAbstract()                                  { return false; }
            public IClass[] getParameterTypes() throws CompileException   { return new IClass[0]; }
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
    private void getIMethods(
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

    private IClass.IMethod findIMethod(Java.SuperclassMethodInvocation scmi) throws CompileException {
        Java.ClassDeclaration declaringClass;
        for (Java.Scope s = scmi.enclosingBlockStatement;; s = s.getEnclosingScope()) {
            if (s instanceof Java.FunctionDeclarator) {
                Java.FunctionDeclarator fd = (Java.FunctionDeclarator) s;
                if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Superclass method cannot be invoked in static context", scmi.getLocation());
            }
            if (s instanceof Java.ClassDeclaration) {
                declaringClass = (Java.ClassDeclaration) s;
                break;
            }
        }
        return this.findIMethod(
            (Java.Located) scmi,                          // located
            this.resolve(declaringClass).getSuperclass(), // targetType
            scmi.methodName,                              // methodName
            scmi.arguments                                // arguments
        );
    }

    /**
     * Determine the arguments' types and choose the most specific invocable.
     * @param located
     * @param iInvocables Length must be greater than zero
     * @param arguments
     * @return The selected {@link IClass.IInvocable}
     * @throws CompileException
     */
    private IClass.IInvocable findMostSpecificIInvocable(
        Java.Located              located,
        final IClass.IInvocable[] iInvocables,
        Java.Rvalue[]             arguments
    ) throws CompileException {
        if (iInvocables.length == 0) throw new RuntimeException();

        // Determine arguments' types.
        final IClass[] argumentTypes = new IClass[arguments.length];
        if (UnitCompiler.DEBUG) System.out.println("Argument types:");
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = this.getType(arguments[i]);
            if (UnitCompiler.DEBUG) System.out.println(argumentTypes[i]);
        }

        // Select applicable methods (15.12.2.1).
        List applicableIInvocables = new ArrayList();
        NEXT_METHOD:
        for (int i = 0; i < iInvocables.length; ++i) {
            IClass.IInvocable ii = iInvocables[i];

            // Check parameter count.
            IClass[] parameterTypes = ii.getParameterTypes();
            if (parameterTypes.length != arguments.length) continue;

            // Check argument types vs. parameter types.
            if (UnitCompiler.DEBUG) System.out.println("Parameter / argument type check:");
            for (int j = 0; j < arguments.length; ++j) {
                // Is method invocation conversion possible (5.3)?
                if (UnitCompiler.DEBUG) System.out.println(parameterTypes[j] + " <=> " + argumentTypes[j]);
                if (!this.isMethodInvocationConvertible(argumentTypes[j], parameterTypes[j])) continue NEXT_METHOD;
            }

            // Applicable!
            if (UnitCompiler.DEBUG) System.out.println("Applicable!");
            applicableIInvocables.add(ii);
        }
        if (applicableIInvocables.size() == 0) {
            StringBuffer sb2 = new StringBuffer();
            if (argumentTypes.length == 0) {
                sb2.append("zero actual parameters");
            } else {
                sb2.append("actual parameters \"").append(argumentTypes[0]);
                for (int i = 1; i < argumentTypes.length; ++i) {
                    sb2.append(", ").append(argumentTypes[i]);
                }
                sb2.append("\"");
            }
            StringBuffer sb = new StringBuffer('"' + iInvocables[0].toString() + '"');
            for (int i = 1; i < iInvocables.length; ++i) {
                sb.append(", ").append('"' + iInvocables[i].toString() + '"');
            }
            this.compileError("No applicable constructor/method found for " + sb2.toString() + "; candidates are: " + sb.toString(), located.getLocation());

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
            if (UnitCompiler.DEBUG) System.out.println("mostSpecificIInvocables=" + maximallySpecificIInvocables);
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
                        if (theNonAbstractMethod != null) throw new RuntimeException("SNO: More than one non-abstract method with same signature and same declaring class!?");
                        theNonAbstractMethod = m;
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
                            for (int l = 0; l < tes[k].length; ++l) {
                                IClass te2 = tes[k][l];
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
                public boolean  isAbstract()                                { return true; }
                public boolean  isStatic()                                  { return false; }
                public Access   getAccess()                                 { return im.getAccess(); }
                public IClass[] getParameterTypes() throws CompileException { return im.getParameterTypes(); }
                public IClass[] getThrownExceptions()                       { return tes; }
            };
        }

        // JLS 15.12.2.2.BL2.B2
        {
            StringBuffer sb = new StringBuffer("Invocation of constructor/method with actual parameter type(s) \"");
            for (int i = 0; i < arguments.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(Descriptor.toString(argumentTypes[i].getDescriptor()));
            }
            sb.append("\" is ambiguous: ");
            for (int i = 0; i < maximallySpecificIInvocables.size(); ++i) {
                if (i > 0) sb.append(" vs. ");
                sb.append("\"" + maximallySpecificIInvocables.get(i) + "\"");
            }
            this.compileError(sb.toString(), located.getLocation());
        }
        return (IClass.IMethod) iInvocables[0];
    }

    /**
     * Check if "method invocation conversion" (5.3) is possible.
     */
    private boolean isMethodInvocationConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {

        // 5.3 Identity conversion.
        if (sourceType == targetType) return true;

        // 5.3 Widening primitive conversion.
        if (this.isWideningPrimitiveConvertible(sourceType, targetType)) return true;

        // 5.3 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return true;

        // 5.3 TODO: FLOAT or DOUBLE value set conversion

        return false;
    }

    private void checkThrownException(
        Java.Located located,
        IClass  type,
        Java.Scope   scope
    ) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!this.iClassLoader.THROWABLE.isAssignableFrom(type)) this.compileError("Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"", located.getLocation());

        // "RuntimeException" and "Error" are never checked.
        if (
            this.iClassLoader.RUNTIME_EXCEPTION.isAssignableFrom(type) ||
            this.iClassLoader.ERROR.isAssignableFrom(type)
        ) return;

        // Match against enclosing "try...catch" blocks.
        while (scope instanceof Java.Statement) {
            if (scope instanceof Java.TryStatement) {
                Java.TryStatement ts = (Java.TryStatement) scope;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    Java.CatchClause cc = (Java.CatchClause) ts.catchClauses.get(i);
                    IClass caughtType = this.getType(cc.caughtException.type);
                    if (caughtType.isAssignableFrom(type)) return;
                }
            }
            scope = scope.getEnclosingScope();
        }

        // Match against "throws" clause of declaring function.
        if (scope instanceof Java.FunctionDeclarator) {
            Java.FunctionDeclarator fd = (Java.FunctionDeclarator) scope;
            for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                IClass te = this.getType(fd.thrownExceptions[i]);
                if (te.isAssignableFrom(type)) return;
            }
        }

        this.compileError("Thrown exception of type \"" + type + "\" is neither caught by a \"try...catch\" block nor declared in the \"throws\" clause of the declaring function", located.getLocation());
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

    /*private*/ IClass resolve(final Java.TypeDeclaration td) {
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
            protected IClass  getComponentType2() { throw new RuntimeException(); }
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
                    throw new RuntimeException();
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
            public boolean isPublic() { return (atd.modifiers & Mod.PUBLIC) != 0; }
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
                    throw new RuntimeException();
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
        Java.Located             located,
        Java.ClassDeclaration    declaringClass,
        Java.TypeBodyDeclaration declaringTypeBodyDeclaration,
        IClass                   targetIClass
    ) throws CompileException {
        List path = UnitCompiler.getOuterClasses(declaringClass);

        if (declaringTypeBodyDeclaration.isStatic()) this.compileError("No current instance available in static context", located.getLocation());

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
            this.compileError("\"" + declaringClass + "\" is not enclosed by \"" + targetIClass + "\"", located.getLocation());
        }

        int i;
        if (declaringTypeBodyDeclaration instanceof Java.ConstructorDeclarator) {
            if (j == 0) {
                this.writeOpcode(located, Opcode.ALOAD_0);
                return;
            }

            Java.ConstructorDeclarator constructorDeclarator = (Java.ConstructorDeclarator) declaringTypeBodyDeclaration;
            Java.LocalVariable syntheticParameter = (Java.LocalVariable) constructorDeclarator.syntheticParameters.get("this$" + (path.size() - 2));
            if (syntheticParameter == null) throw new RuntimeException();
            this.load(located, syntheticParameter);
            i = 1;
        } else {
            this.writeOpcode(located, Opcode.ALOAD_0);
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
            this.writeOpcode(located, Opcode.GETFIELD);
            this.writeConstantFieldrefInfo(
                located,
                iic.getDescriptor(), // classFD
                fieldName,           // fieldName
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
            boolean isStaticMethod = (s instanceof Java.MethodDeclarator) && (((Java.FunctionDeclarator) s).modifiers & Mod.STATIC) != 0;
            for (; !(s instanceof Java.TypeDeclaration); s = s.getEnclosingScope());
            Java.TypeDeclaration immediatelyEnclosingTypeDeclaration = (Java.TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof Java.ClassDeclaration &&
                !isStaticMethod
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
            for (s = tr.scope; s instanceof Java.Statement; s = s.getEnclosingScope());
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

    /*private*/ IClass getReturnType(Java.FunctionDeclarator fd) throws CompileException {
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
                if (!(cd.declaringClass instanceof Java.InnerClassDeclaration)) return super.getDescriptor();

                List l = new ArrayList();

                // Convert enclosing instance reference into prepended constructor parameters.
                IClass outerClass = UnitCompiler.this.resolve(cd.declaringClass).getOuterIClass();
                if (outerClass != null) l.add(outerClass.getDescriptor());

                // Convert synthetic fields into prepended constructor parameters.
                for (Iterator it = cd.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    if (sf.getName().startsWith("val$")) l.add(sf.getType().getDescriptor());
                }
                Java.FormalParameter[] fps = cd.formalParameters;
                for (int i = 0; i < fps.length; ++i) {
                    l.add(UnitCompiler.this.getType(fps[i].type).getDescriptor());
                }
                String[] apd = (String[]) l.toArray(new String[l.size()]);
                return new MethodDescriptor(apd, Descriptor.VOID).toString();
            }

            public IClass[] getParameterTypes() throws CompileException {
                Java.FormalParameter[] fps = cd.formalParameters;
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
                Java.FormalParameter[] fps = cd.formalParameters;
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
                Java.FormalParameter[] fps = md.formalParameters;
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
            throw new RuntimeException();
        }
    }

    /**
     * If the given name was declared in a simple type import, load that class.
     */
    private IClass importSingleType(String simpleTypeName, Location location) throws CompileException {
        String[] ss = this.compilationUnit.getSingleTypeImport(simpleTypeName);
        if (ss == null) return null;

        IClass iClass = this.loadFullyQualifiedClass(ss);
        if (iClass == null) {
            this.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
            return this.iClassLoader.OBJECT;
        }
        return iClass;
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
        List packages = new ArrayList();
        packages.add(new String[] { "java", "lang" });
        for (Iterator i = this.compilationUnit.importDeclarations.iterator(); i.hasNext();) {
            Java.ImportDeclaration id = (Java.ImportDeclaration) i.next();
            if (id instanceof Java.TypeImportOnDemandDeclaration) {
                packages.add(((Java.TypeImportOnDemandDeclaration) id).identifiers);
            }
        }
        for (Iterator i = packages.iterator(); i.hasNext();) {
            String[] ss = (String[]) i.next();
            String[] ss2 = new String[ss.length + 1];
            System.arraycopy(ss, 0, ss2, 0, ss.length);
            ss2[ss.length] = simpleTypeName;
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
        Java.Type stringType = new Java.SimpleType(loc, this.iClassLoader.STRING);
        Java.Type classType = new Java.SimpleType(loc, this.iClassLoader.CLASS);

        // Class class$(String className)
        Java.FormalParameter fp = new Java.FormalParameter(false, stringType, "className");
        Java.MethodDeclarator cdmd = new Java.MethodDeclarator(
            loc,                               // location
            cl.declaringType,                  // declaringType
            null,                              // optionalDocComment
            Mod.STATIC,                        // modifiers
            classType,                         // type
            "class$",                          // name
            new Java.FormalParameter[] { fp }, // formalParameters
            new Java.Type[0]                   // thrownExceptions
        );

        Java.Block body = new Java.Block(loc, (Java.Scope) cdmd);

        // try {
        Java.TryStatement ts = new Java.TryStatement(loc, (Java.Scope) cdmd);

        // return Class.forName(className);
        Java.MethodInvocation mi = new Java.MethodInvocation(
            loc,                      // location
            (Java.BlockStatement) ts, // enclosingBlockStatement
            classType,                // optionalTarget
            "forName",                // methodName
            new Java.Rvalue[] {       // arguments
                new Java.AmbiguousName(loc, (Java.Scope) ts, new String[] { "className" } )
            }
        );
        ts.setBody(new Java.ReturnStatement(loc, (Java.Scope) ts, mi));

        IClass classNotFoundExceptionIClass = this.iClassLoader.loadIClass("Ljava/lang/ClassNotFoundException;");
        if (classNotFoundExceptionIClass == null) throw new RuntimeException();

        IClass noClassDefFoundErrorIClass = this.iClassLoader.loadIClass("Ljava/lang/NoClassDefFoundError;");
        if (noClassDefFoundErrorIClass == null) throw new RuntimeException();

        // catch (ClassNotFoundException ex) {
        Java.Block b = new Java.Block(loc, (Java.Scope) ts);
        Java.CatchClause cc = new Java.CatchClause(
            new Java.FormalParameter(true, new Java.SimpleType(loc, classNotFoundExceptionIClass), "ex"), // caughtException
            b                                                                                             // body
        );

        // throw new NoClassDefFoundError(ex.getMessage());
        Java.NewClassInstance nci = new Java.NewClassInstance(
            loc,                                                  // location
            (Java.Scope) b,                                       // scope
            (Java.Rvalue) null,                                   // optionalQualification
            new Java.SimpleType(loc, noClassDefFoundErrorIClass), // type
            new Java.Rvalue[] {                                   // arguments
                new Java.MethodInvocation(
                    loc,                                                                // location
                    (Java.BlockStatement) b,                                            // enclosingScope
                    new Java.AmbiguousName(loc, (Java.Scope) b, new String[] { "ex"} ), // optionalTarget
                    "getMessage",                                                       // methodName
                    new Java.Rvalue[0]                                                  // arguments
                )
            }
        );
        b.addStatement(new Java.ThrowStatement(loc, (Java.Scope) b, nci));

        ts.addCatchClause(cc);

        body.addStatement(ts);

        cdmd.setBody(body);

        cl.declaringType.declaredMethods.add(cdmd);

        // Invalidate several caches.
        if (cl.declaringType.resolvedType != null) {
            cl.declaringType.resolvedType.declaredIMethods = null;
            cl.declaringType.resolvedType.declaredIMethodCache = null;
        }
    }

    private IClass pushConstant(
        Java.Located located,
        Object  value
    ) {
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
                this.writeOpcode(located, Opcode.ICONST_0 + i);
            } else
            if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
                this.writeOpcode(located, Opcode.BIPUSH);
                this.writeByte(located, (byte) i);
            } else {
                this.writeLDC(located, this.addConstantIntegerInfo(i));
            }
            return IClass.INT;
        }
        if (value instanceof Long) {
            long lv = ((Long) value).longValue();
            if (lv >= 0L && lv <= 1L) {
                this.writeOpcode(located, Opcode.LCONST_0 + (int) lv);
            } else {
                this.writeOpcode(located, Opcode.LDC2_W);
                this.writeConstantLongInfo(located, lv);
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
                this.writeOpcode(located, Opcode.FCONST_0 + (int) fv);
            } else
            {
                this.writeLDC(located, this.addConstantFloatInfo(fv));
            }
            return IClass.FLOAT;
        }
        if (value instanceof Double) {
            double dv = ((Double) value).doubleValue();
            if (
                Double.doubleToLongBits(dv) == Double.doubleToLongBits(0.0D) // POSITIVE zero!
                || dv == 1.0D
            ) {
                this.writeOpcode(located, Opcode.DCONST_0 + (int) dv);
            } else
            {
                this.writeOpcode(located, Opcode.LDC2_W);
                this.writeConstantDoubleInfo(located, dv);
            }
            return IClass.DOUBLE;
        }
        if (value instanceof String) {
            String s = (String) value;
            if (s.length() < (65536 / 3)) {
                this.writeLDC(located, this.addConstantStringInfo((String) value));
                return this.iClassLoader.STRING;
            }
            int sLength = s.length(), uTFLength = 0;
            int from = 0;
            for (int i = 0;; i++) {
                if (i == sLength || uTFLength >= 65532) {
                    this.writeLDC(located, this.addConstantStringInfo(s.substring(from, i)));
                    if (from != 0) {
                        this.writeOpcode(located, Opcode.INVOKEVIRTUAL);
                        this.writeConstantMethodrefInfo(
                            located,
                            Descriptor.STRING,                                // classFD
                            "concat",                                         // methodName
                            "(" + Descriptor.STRING + ")" + Descriptor.STRING // methodMD
                        );
                    }
                    if (i == sLength) break;
                    from = i;
                    uTFLength = 0;
                }
                int c = s.charAt(i);
                if ((c >= 0x0001) && (c <= 0x007F)) {
                    ++uTFLength;
                } else if (c > 0x07FF) {
                    uTFLength += 3;
                } else {
                    uTFLength += 2;
                }
            }
            return this.iClassLoader.STRING;
        }
        if (value instanceof Boolean) {
            this.writeOpcode(located, ((Boolean) value).booleanValue() ? Opcode.ICONST_1 : Opcode.ICONST_0);
            return IClass.BOOLEAN;
        }
        if (value == Java.Rvalue.CONSTANT_VALUE_NULL) {
            this.writeOpcode(located, Opcode.ACONST_NULL);
            return IClass.VOID;
        }
        throw new RuntimeException("Unknown literal type \"" + value.getClass().getName() + "\"");
    }
    private void writeLDC(Java.Located located, short index) {
        if (index <= 255) {
            this.writeOpcode(located, Opcode.LDC);
            this.writeByte(located, (byte) index);
        } else {
            this.writeOpcode(located, Opcode.LDC_W);
            this.writeShort(located, index);
        }
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2).
     */
    private void assignmentConversion(
        Java.Located located,
        IClass       sourceType,
        IClass       targetType,
        Object       optionalConstantValue
    ) throws CompileException {
        if (UnitCompiler.DEBUG) System.out.println("assignmentConversion(" + sourceType + ", " + targetType + ", " + optionalConstantValue + ")");

        // 5.2 / 5.1.1 Identity conversion.
        if (this.tryIdentityConversion(sourceType, targetType)) return;

        // 5.2 / 5.1.2 Widening primitive conversion.
        if (this.tryWideningPrimitiveConversion(located, sourceType, targetType)) return;

        // 5.2 / 5.1.4 Widening reference conversion.
        if (this.isWideningReferenceConvertible(sourceType, targetType)) return;

        // 5.2 Special narrowing primitive conversion.
        if (optionalConstantValue != null) {
            if (this.isConstantPrimitiveAssignmentConvertible(
                optionalConstantValue, // constantValue
                targetType             // targetType
            )) return;
        }

        this.compileError("Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"", located.getLocation());
    }

    /**
     * Implements "assignment conversion" (JLS2 5.2) on a constant value.
     */
    /*private*/ Object assignmentConversion(
        Java.Located located,
        Object       value,
        IClass       targetType
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
        }
        if (result == null) this.compileError("Cannot convert constant of type \"" + value.getClass().getName() + "\" to type \"" + targetType.toString() + "\"", located.getLocation());
        return result;
    }

    /**
     * Implements "unary numeric promotion" (5.6.1)
     *
     * @return The promoted type.
     */
    private IClass unaryNumericPromotion(
        Java.Located located,
        IClass  type
    ) throws CompileException {
        IClass promotedType = this.unaryNumericPromotionType(located, type);

        if (
            !this.tryIdentityConversion(type, promotedType) &&
            !this.tryWideningPrimitiveConversion(
                located,     // located
                type,        // sourceType
                promotedType // targetType
            )
        ) throw new RuntimeException();
        return promotedType;
    }

    private IClass unaryNumericPromotionType(
        Java.Located located,
        IClass  type
    ) throws CompileException {
        if (!type.isPrimitiveNumeric()) this.compileError("Unary numeric promotion not possible on non-numeric-primitive type \"" + type + "\"", located.getLocation());

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
        Java.Located              located,
        IClass               type1,
        CodeContext.Inserter convertInserter1,
        IClass               type2
    ) throws CompileException {
        IClass promotedType = this.binaryNumericPromotionType(located, type1, type2);

        if (convertInserter1 != null) {
            this.codeContext.pushInserter(convertInserter1);
            try {
                if (
                    !this.tryIdentityConversion(type1, promotedType) &&
                    !this.tryWideningPrimitiveConversion(
                        located,     // located
                        type1,       // sourceType
                        promotedType // targetType
                    )
                ) throw new RuntimeException();
            } finally {
                this.codeContext.popInserter();
            }
        }

        if (
            !this.tryIdentityConversion(type2, promotedType) &&
            !this.tryWideningPrimitiveConversion(
                located,     // located
                type2,       // sourceType
                promotedType // targetType
            )
        ) throw new RuntimeException();

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
     * Implements "identity conversion" (5.1.1).
     *
     * @return Whether the conversion succeeded
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
        Java.Located located,
        IClass  sourceType,
        IClass  targetType
    ) {
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_WIDENING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            this.write(located, opcodes);
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
     * Checks if "widening reference conversion" (5.1.4) is possible. This is
     * identical to EXECUTING the conversion, because no opcodes are necessary
     * to implement the conversion.
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
     * Implements "narrowing primitive conversion" (JLS 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private boolean tryNarrowingPrimitiveConversion(
        Java.Located located,
        IClass  sourceType,
        IClass  targetType
    ) {
        byte[] opcodes = (byte[]) UnitCompiler.PRIMITIVE_NARROWING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            this.write(located, opcodes);
            return true;
        }
        return false;
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
     * Check if "constant primitive assignment conversion" (JLS 5.2, paragraph 1) is possible.
     * @param constantValue The constant value that is to be converted
     * @param targetType The type to convert to
     */
    private boolean isConstantPrimitiveAssignmentConvertible(
        Object constantValue,
        IClass targetType
    ) {
        if (UnitCompiler.DEBUG) System.out.println("isConstantPrimitiveAssignmentConvertible(" + constantValue + ", " + targetType + ")");

        int cv;
        if (
            constantValue instanceof Byte ||
            constantValue instanceof Short ||
            constantValue instanceof Integer
        ) {
            cv = ((Number) constantValue).intValue();
        } else
        if (constantValue instanceof Character) {
            cv = (int) ((Character) constantValue).charValue();
        } else {
            return false;
        }

        if (targetType == IClass.BYTE ) return cv >= Byte.MIN_VALUE && cv <= Byte.MAX_VALUE;
        if (targetType == IClass.SHORT) {
            return cv >= Short.MIN_VALUE && cv <= Short.MAX_VALUE;
        }
        if (targetType == IClass.CHAR ) return cv >= Character.MIN_VALUE && cv <= Character.MAX_VALUE;

        return false;
    }

    /**
     * Implements "narrowing reference conversion" (5.1.5).
     *
     * @return Whether the conversion succeeded
     */
    private boolean tryNarrowingReferenceConversion(
        Java.Located located,
        IClass  sourceType,
        IClass  targetType
    ) throws CompileException {
        if (!this.isNarrowingReferenceConvertible(sourceType, targetType)) return false;

        this.writeOpcode(located, Opcode.CHECKCAST);
        this.writeConstantClassInfo(located, targetType.getDescriptor());
        return true;
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
     * Attempt to load an {@link IClass} by fully-qualified name
     * @param identifiers
     * @return <code>null</code> if a class with the given name could not be loaded
     */
    private IClass loadFullyQualifiedClass(String[] identifiers) {

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
            IClass result = this.iClassLoader.loadIClass(sb.toString());
            if (result != null) return result;
            if (j < 0) break;
            sb.setCharAt(slashes[j], '$');
        }
        return null;
    }

    // Load the value of a local variable onto the stack and return its type.
    private IClass load(
        Java.Located       located,
        Java.LocalVariable localVariable
    ) {
        this.load(
            located,
            localVariable.type,
            localVariable.localVariableArrayIndex
        );
        return localVariable.type;
    }
    private void load(
        Java.Located located,
        IClass  type,
        int     index
    ) {
        if (index <= 3) {
            this.writeOpcode(located, Opcode.ILOAD_0 + 4 * this.ilfda(type) + index);
        } else
        if (index <= 255) {
            this.writeOpcode(located, Opcode.ILOAD + this.ilfda(type));
            this.writeByte(located, index);
        } else
        {
            this.writeOpcode(located, Opcode.WIDE);
            this.writeOpcode(located, Opcode.ILOAD + this.ilfda(type));
            this.writeShort(located, index);
        }
    }

    /**
     * Assign stack top value to the given local variable. (Assignment conversion takes effect.)
     * If <copde>optionalConstantValue</code> is not <code>null</code>, then the top stack value
     * is a constant value with that type and value, and a narrowing primitive conversion as
     * described in JLS 5.2 is applied.
     */
    private void store(
        Java.Located       located,
        IClass             valueType,
        Java.LocalVariable localVariable
    ) {
        this.store(
            located,                              // located
            localVariable.type,                   // lvType
            localVariable.localVariableArrayIndex // lvIndex
        );
    }
    private void store(
        Java.Located located,
        IClass  lvType,
        short   lvIndex
    ) {
        if (lvIndex <= 3) {
            this.writeOpcode(located, Opcode.ISTORE_0 + 4 * this.ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            this.writeOpcode(located, Opcode.ISTORE + this.ilfda(lvType));
            this.writeByte(located, lvIndex);
        } else
        {
            this.writeOpcode(located, Opcode.WIDE);
            this.writeOpcode(located, Opcode.ISTORE + this.ilfda(lvType));
            this.writeShort(located, lvIndex);
        }
    }

    private void dup(Java.Located located, int n) {
        switch (n) {

        case 0:
            ;
            break;

        case 1:
            this.writeOpcode(located, Opcode.DUP);
            break;

        case 2:
            this.writeOpcode(located, Opcode.DUP2);
            break;

        default:
            throw new RuntimeException("dup(" + n + ")");
        }
    }
    private void dupx(
        Java.Located located,
        IClass  type,
        int     x
    ) {
        if (x < 0 || x > 2) throw new RuntimeException();
        int dup  = Opcode.DUP  + x;
        int dup2 = Opcode.DUP2 + x;
        this.writeOpcode(located, (
            type == IClass.LONG || type == IClass.DOUBLE ?
            dup2 :
            dup
        ));
    }

    private void pop(Java.Located located, IClass type) {
        if (type == IClass.VOID) return;
        this.writeOpcode(located, (
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

    private IClass getArrayType(IClass type) {
        return this.iClassLoader.loadArrayIClass(type);
    }
    /*private*/ IClass getArrayType(IClass type, int brackets) {
        if (brackets == 0) return type;
        for (int i = 0; i < brackets; ++i) type = this.iClassLoader.loadArrayIClass(type);
        return type;
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
        IClass.IField[] fields = iClass.getDeclaredIFields();
        for (int i = 0; i < fields.length; ++i) {
            final IClass.IField f = fields[i];
            if (name.equals(f.getName())) return f;
        }

        // Examine superclass.
        IClass.IField f = null;
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
     * Find one class or interface by name.
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
    /*private*/ void compileError(String message, Location optionalLocation) throws CompileException {
        ErrorHandler eh = this.compileErrorHandler;
        if (eh != null) {
            eh.handleError(message, optionalLocation);
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
        WarningHandler wh = this.warningHandler;
        if (wh != null) wh.handleWarning(handle, message, optionalLocation);
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
     */
    public void setCompileErrorHandler(ErrorHandler errorHandler) {
        this.compileErrorHandler = errorHandler;
    }

    /**
     * By default, warnings are discarded, but an application my install a (thread-local)
     * {@link WarningHandler}.
     */
    public void setWarningHandler(WarningHandler warningHandler) {
        this.warningHandler = warningHandler;
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

    /**
     * Define a local variable in the context of this block
     * @return The index of the variable
     */
    private Java.LocalVariable defineLocalVariable(
        Java.Block    b,
        Java.Located  located,
        boolean       finaL,
        IClass        type,
        String        name
    ) throws CompileException {

        // Check for local variable redefinition.
        for (Java.Scope s = b; s instanceof Java.Statement; s = s.getEnclosingScope()) {
            if (s instanceof Java.Block) {
                if (((Java.Block) s).localVariables.containsKey(name)) this.compileError("Redefinition of local variable \"" + name + "\"", located.getLocation());
            }
        }
        Java.LocalVariable lv = new Java.LocalVariable(
            finaL,                                                            // finaL
            type,                                                             // type
            this.codeContext.allocateLocalVariable(Descriptor.size(type.getDescriptor())) // localVariableArrayIndex
        );
        b.localVariables.put(name, lv);
        return lv;
    }

    private void write(Java.Locatable l, byte[] b) {
        this.codeContext.write(l.getLocation().getLineNumber(), b);
    }
    private void writeByte(Java.Locatable l, int v) {
        this.codeContext.write(l.getLocation().getLineNumber(), new byte[] { (byte) v });
    }
    private void writeInt(Java.Locatable l, int v) {
        this.codeContext.write(l.getLocation().getLineNumber(), new byte[] { (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v });
    }
    private void writeShort(Java.Locatable l, int v) {
        this.codeContext.write(l.getLocation().getLineNumber(), new byte[] { (byte) (v >> 8), (byte) v });
    }
    private void writeOpcode(Java.Locatable l, int opcode) {
        this.writeByte(l, opcode);
    }
    private void writeBranch(Java.Locatable l, int opcode, final CodeContext.Offset dst) {
        this.codeContext.writeBranch(l.getLocation().getLineNumber(), opcode, dst);
    }
    private void writeBranch(int opcode, final CodeContext.Offset dst) {
        this.codeContext.writeBranch((short) -1, opcode, dst);
    }
    private void writeOffset(Java.Locatable l, CodeContext.Offset src, final CodeContext.Offset dst) {
        this.codeContext.writeOffset(l.getLocation().getLineNumber(), src, dst);
    }

    // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

    private void writeConstantClassInfo(Java.Locatable l, String descriptor) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantClassInfo(descriptor)
        );
    }
    private void writeConstantFieldrefInfo(Java.Locatable l, String classFD, String fieldName, String fieldFD) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantFieldrefInfo(classFD, fieldName, fieldFD)
        );
    }
    private void writeConstantMethodrefInfo(Java.Locatable l, String classFD, String methodName, String methodMD) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantMethodrefInfo(classFD, methodName, methodMD)
        );
    }
    private void writeConstantInterfaceMethodrefInfo(Java.Locatable l, String classFD, String methodName, String methodMD) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantInterfaceMethodrefInfo(classFD, methodName, methodMD)
        );
    }
/* UNUSED
    private void writeConstantStringInfo(Java.Locatable l, String value) {
        this.codeContext.writeShort(
            l.getLocation().getLineNumber(),
            this.addConstantStringInfo(value)
        );
    }
*/
    private short addConstantStringInfo(String value) {
        return this.codeContext.getClassFile().addConstantStringInfo(value);
    }
    /* UNUSED
    private void writeConstantIntegerInfo(Java.Locatable l, int value) {
        this.codeContext.writeShort(
            l.getLocation().getLineNumber(),
            this.addConstantIntegerInfo(value)
        );
    }
*/
    private short addConstantIntegerInfo(int value) {
        return this.codeContext.getClassFile().addConstantIntegerInfo(value);
    }
/* UNUSED
    private void writeConstantFloatInfo(Java.Locatable l, float value) {
        this.codeContext.writeShort(
            l.getLocation().getLineNumber(),
            this.addConstantFloatInfo(value)
        );
    }
*/
    private short addConstantFloatInfo(float value) {
        return this.codeContext.getClassFile().addConstantFloatInfo(value);
    }
    private void writeConstantLongInfo(Java.Locatable l, long value) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantLongInfo(value)
        );
    }
    private void writeConstantDoubleInfo(Java.Locatable l, double value) {
        CodeContext ca = this.codeContext;
        ca.writeShort(
            l.getLocation().getLineNumber(),
            ca.getClassFile().addConstantDoubleInfo(value)
        );
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
            for (s = qtr.scope; !(s instanceof Java.TypeBodyDeclaration); s = s.getEnclosingScope());
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

    private void referenceThis(Java.Located located) {
        this.writeOpcode(located, Opcode.ALOAD_0);
    }

    /**
     * Expects "dimExprCount" values of type "integer" on the operand stack.
     * Creates an array of "dimExprCount" + "dims" dimensions of
     * "componentType".
     *
     * @return The type of the created array
     */
    private IClass newArray(
        Java.Located located,
        int     dimExprCount,
        int     dims,
        IClass  componentType
    ) {
        if (dimExprCount == 1 && dims == 0 && componentType.isPrimitive()) {

            // "new <primitive>[<n>]"
            this.writeOpcode(located, Opcode.NEWARRAY);
            this.writeByte(located, (
                componentType == IClass.BOOLEAN ? 4 :
                componentType == IClass.CHAR    ? 5 :
                componentType == IClass.FLOAT   ? 6 :
                componentType == IClass.DOUBLE  ? 7 :
                componentType == IClass.BYTE    ? 8 :
                componentType == IClass.SHORT   ? 9 :
                componentType == IClass.INT     ? 10 :
                componentType == IClass.LONG    ? 11 : -1
            ));
            return this.getArrayType(componentType);
        }

        if (dimExprCount == 1) {
            IClass at = this.getArrayType(componentType, dims);

            // "new <class-or-interface>[<n>]"
            // "new <anything>[<n>][]..."
            this.writeOpcode(located, Opcode.ANEWARRAY);
            this.writeConstantClassInfo(located, at.getDescriptor());
            return this.getArrayType(at, 1);
        } else {
            IClass at = this.getArrayType(componentType, dimExprCount + dims);

            // "new <anything>[]..."
            // "new <anything>[<n>][<m>]..."
            // "new <anything>[<n>][<m>]...[]..."
            this.writeOpcode(located, Opcode.MULTIANEWARRAY);
            this.writeConstantClassInfo(located, at.getDescriptor());
            this.writeByte(located, dimExprCount);
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

    // Used to write byte code while compiling one constructor/method.
    private CodeContext codeContext = null;

    // Used for elaborate compile error handling.
    private ErrorHandler compileErrorHandler = null;

    // Used for elaborate warning handling.
    private WarningHandler warningHandler = null;

    /*package*/ final Java.CompilationUnit compilationUnit;

    private List          generatedClassFiles;
    private IClassLoader  iClassLoader;
    private EnumeratorSet debuggingInformation;
}
