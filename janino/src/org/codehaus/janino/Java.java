
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.codehaus.janino.IClass.IField;
import org.codehaus.janino.util.*;
import org.codehaus.janino.util.iterator.ReverseListIterator;


/**
 * This wrapper class defines classes that represent the elements of the
 * Java<sup>TM</sup> programming language.
 * <p>
 * Notices:
 * <ul>
 *   <li>"JLS1" refers to <a href="http://java.sun.com/docs/books/jls/first_edition/html/index.html">"The Java<sup>TM</sup> Language Specification, First Edition"</a>.
 *   <li>"JLS" or "JLS2" refers to <a href="http://java.sun.com/docs/books/jls/second_edition/html/j.title.doc.html">"The Java<sup>TM</sup> Language Specification, Second Edition"</a>.
 * </ul>
 */

public class Java {
    private static final boolean DEBUG = false;

    private Java() {} // Don't instantiate me.

    public interface Scope {

        /**
         * @return Enclusing scope or "null".
         */
        public Scope getEnclosingScope();
    }

    /**
     * This interface is implemented by objects which are associated with a
     * location in the source code.
     */
    public interface Locatable {
        public Location getLocation();

        /**
         * Throw a {@link Parser.ParseException} with the given message and this
         * object's location.
         * 
         * @param message The message to report
         */
        public void throwParseException(String message) throws Parser.ParseException;

        /**
         * Issue a compile error with the given message and this object's location.
         * 
         * @param message The message to report
         * @see Java#compileError(String, Location)
         */
        public void compileError(String message) throws CompileException;
    }
    private static abstract class Located implements Locatable {
        private final Location location;

        protected Located(Location location) {
            this.location = location;
        }

        public CodeContext createDummyCodeContext() {
            return new CodeContext(Java.getCodeContext().getClassFile());
        }

        // Implement "Locatable".

        public Location getLocation() { return this.location; }
        public void throwParseException(String message) throws Parser.ParseException {
            throw new Parser.ParseException(message, this.location);
        }
        public void compileError(String message) throws CompileException {
            Java.compileError(message, this.location);
        }

        // Wrappers for "CodeContext.write...()". Saves us some coding overhead.

        public void write(byte[] b) {
            Java.getCodeContext().write(this.getLocation().getLineNumber(), b);
        }
        public void writeByte(int v) {
            Java.getCodeContext().write(this.getLocation().getLineNumber(), new byte[] { (byte) v });
        }
        public void writeInt(int v) {
            Java.getCodeContext().write(this.getLocation().getLineNumber(), new byte[] { (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8), (byte) v });
        }
        public void writeShort(int v) {
            Java.getCodeContext().write(this.getLocation().getLineNumber(), new byte[] { (byte) (v >> 8), (byte) v });
        }
        public void writeOpcode(int opcode) {
            this.writeByte(opcode);
        }
        public void writeBranch(int opcode, final CodeContext.Offset dst) {
            Java.getCodeContext().writeBranch(this.getLocation().getLineNumber(), opcode, dst);
        }
        public void writeOffset(CodeContext.Offset src, final CodeContext.Offset dst) {
            Java.getCodeContext().writeOffset(this.getLocation().getLineNumber(), src, dst);
        }

        // Wrappers for "ClassFile.addConstant...Info()". Saves us some coding overhead.

        public void writeConstantClassInfo(String descriptor) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantClassInfo(descriptor)
            );
        }
        public void writeConstantFieldrefInfo(String classFD, String fieldName, String fieldFD) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantFieldrefInfo(classFD, fieldName, fieldFD)
            );
        }
        public void writeConstantMethodrefInfo(String classFD, String methodName, String methodMD) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantMethodrefInfo(classFD, methodName, methodMD)
            );
        }
        public void writeConstantInterfaceMethodrefInfo(String classFD, String methodName, String methodMD) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantInterfaceMethodrefInfo(classFD, methodName, methodMD)
            );
        }
        public void writeConstantStringInfo(String value) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantStringInfo(value)
            );
        }
        public short addConstantStringInfo(String value) {
            return Java.getCodeContext().getClassFile().addConstantStringInfo(value);
        }
        public void writeConstantIntegerInfo(int value) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantIntegerInfo(value)
            );
        }
        public short addConstantIntegerInfo(int value) {
            return Java.getCodeContext().getClassFile().addConstantIntegerInfo(value);
        }
        public void writeConstantFloatInfo(float value) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantFloatInfo(value)
            );
        }
        public short addConstantFloatInfo(float value) {
            return Java.getCodeContext().getClassFile().addConstantFloatInfo(value);
        }
        public void writeConstantLongInfo(long value) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantLongInfo(value)
            );
        }
        public void writeConstantDoubleInfo(double value) {
            CodeContext ca = Java.getCodeContext();
            ca.writeShort(
                this.getLocation().getLineNumber(),
                ca.getClassFile().addConstantDoubleInfo(value)
            );
        }

        // Wrappers for "CodeContext"'s offset/inserter methods.

        public CodeContext.Offset newOffset() {
            return Java.getCodeContext().newOffset();
        }
        public CodeContext.Offset newUnsetOffset() {
            return Java.getCodeContext().new Offset();
        }
        public CodeContext.Inserter newInserter() {
            return Java.getCodeContext().newInserter();
        }
        public void pushInserter(CodeContext.Inserter ins) {
            Java.getCodeContext().pushInserter(ins);
        }
        public void popInserter() {
            Java.getCodeContext().popInserter();
        }

        // Wrappers for "CodeContext"'s local variable-related methods.

        public void saveLocalVariables() {
            Java.getCodeContext().saveLocalVariables();
        }
        public void restoreLocalVariables() {
            Java.getCodeContext().restoreLocalVariables();
        }
        public short allocateLocalVariable(short size) {
            return Java.getCodeContext().allocateLocalVariable(size);
        }
    }

    /**
     * Holds the result of {@link Parser#parseCompilationUnit}.
     *
     * <p>
     *   A call to {@link #compile(IClassLoader, DebuggingInformation)} generates an array of
     *   {@link ClassFile} objects which represent the classes and
     *   interfaces defined in the compilation unit.
     * </p>
     */
    public static final class CompilationUnit implements Scope {
        private /*final*/ String optionalFileName;

        /**
         * @param optionalFileName only for {@link #getFileName()}
         */
        public CompilationUnit(String optionalFileName) {
            this.optionalFileName = optionalFileName;
        }

        /**
         * @return the <code>optionalFileName</code> given at construction.
         */
        public String getFileName() {
            return this.optionalFileName;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() { return null; }

        public void setPackageDeclaration(PackageDeclaration packageDeclaration) {
            if (this.optionalPackageDeclaration != null) throw new RuntimeException("Re-setting package declaration");
            this.optionalPackageDeclaration = packageDeclaration;
        }

        public void addImportDeclaration(ImportDeclaration id) throws Parser.ParseException {

            // Check for conflicting single-type import.
            if (id instanceof SingleTypeImportDeclaration) {
                String[] ss = ((SingleTypeImportDeclaration) id).getIdentifiers();
                String name = ss[ss.length - 1];
                for (Iterator it = this.importDeclarations.iterator(); it.hasNext();) {
                    ImportDeclaration id2 = (ImportDeclaration) it.next();
                    if (id2 instanceof SingleTypeImportDeclaration) {
                        String[] ss2 = ((SingleTypeImportDeclaration) id2).getIdentifiers();
                        if (ss2[ss2.length - 1].equals(name)) {
                            if (!Java.join(ss, ".").equals(Java.join(ss2, "."))) id.throwParseException("Class \"" + name + "\" was first imported from \"" + Java.join(ss, ".") + "\", now again from \"" + Java.join(ss2, ".") + "\"");
                        }
                    }
                }
            }
            this.importDeclarations.add(id);
        }

        public void addPackageMemberTypeDeclaration(PackageMemberTypeDeclaration pmtd) {
            this.packageMemberTypeDeclarations.add(pmtd);
        }

        /**
         * Get all classes and interfaces declared in this compilation unit.
         */
        public PackageMemberTypeDeclaration[] getPackageMemberTypeDeclarations() {
            return (PackageMemberTypeDeclaration[]) this.packageMemberTypeDeclarations.toArray(new PackageMemberTypeDeclaration[this.packageMemberTypeDeclarations.size()]);
        }

        /**
         * Find one class or interface by name.
         * @param className Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner".
         * @return <code>null</code> if a class with that name is not declared in this compilation unit
         */
        public IClass findClass(String className) {

            // Examine package name.
            String packageName = (
                this.optionalPackageDeclaration == null ? null :
                this.optionalPackageDeclaration.getPackageName()
            );
            if (packageName != null) {
                if (!className.startsWith(packageName + '.')) return null;
                className = className.substring(packageName.length() + 1);
            }

            StringTokenizer st = new StringTokenizer(className, "$");
            TypeDeclaration td = this.getPackageMemberTypeDeclaration(st.nextToken());
            while (st.hasMoreTokens()) {
                if (td == null) return null;
                td = td.getMemberTypeDeclaration(st.nextToken());
            }
            return (IClass) td;
        }

        /**
         * Return the package member class or interface declared with the given name.
         * @param name Declared (i.e. not the fully qualified) name
         * @return <code>null</code> if a package member type with that name is not declared in this compilation unit
         */
        public PackageMemberTypeDeclaration getPackageMemberTypeDeclaration(String name) {
            for (Iterator it = this.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
                PackageMemberTypeDeclaration pmtd = (PackageMemberTypeDeclaration) it.next();
                if (pmtd.getName().equals(name)) return pmtd;
            }
            return null;
        }

        /**
         * Compiles this compilation unit and returns a set of {@link ClassFile}
         * objects.
         *
         * @param iClassLoader used to look for classes defined outside this compilation unit
         * @return a set of generated {@link ClassFile} objects
         */
        public ClassFile[] compile(
            IClassLoader         iClassLoader,
            DebuggingInformation debuggingInformation
        ) throws CompileException {
            if (iClassLoader == null) throw new RuntimeException();

            // Handle these as thread-local variables. Not really elegant,
            // but very difficult to implement cleanly.
            List                 savedGeneratedClassFiles  = Java.replaceGeneratedClassFiles(new ArrayList());
            IClassLoader         savedIClassLoader         = Java.replaceIClassLoader(iClassLoader);
            DebuggingInformation savedDebuggingInformation = Java.replaceDebuggingInformation(debuggingInformation);
            try {
                this.compile();
                List l = Java.getGeneratedClassFiles();
                return (ClassFile[]) l.toArray(new ClassFile[l.size()]);
            } finally {
                Java.replaceGeneratedClassFiles(savedGeneratedClassFiles);
                Java.replaceIClassLoader(savedIClassLoader);
                Java.replaceDebuggingInformation(savedDebuggingInformation);
            }
        }
        private void compile() throws CompileException {
            for (Iterator it = this.packageMemberTypeDeclarations.iterator(); it.hasNext();) {
                ((PackageMemberTypeDeclaration) it.next()).compile();
            }
        }

        /**
         * Check if the given name was imported through a "single type import", e.g.<pre>
         *     import java.util.Map</pre>
         * 
         * @return the fully qualified name or <code>null</code>
         */
        public String[] getSingleTypeImport(String name) {
            for (Iterator it = this.importDeclarations.iterator(); it.hasNext();) {
                ImportDeclaration id = (ImportDeclaration) it.next();
                if (id instanceof SingleTypeImportDeclaration) {
                    String[] ss = ((SingleTypeImportDeclaration) id).getIdentifiers();
                    if (ss[ss.length - 1].equals(name)) return ss;
                }
            }
            return null;
        }

        /**
         * If the given name was declared in a simple type import, load that class.
         */
        public IClass importSingleType(String simpleTypeName, Location location) throws CompileException {
            String[] ss = this.getSingleTypeImport(simpleTypeName);
            if (ss == null) return null;

            IClass iClass = Java.loadFullyQualifiedClass(ss);
            if (iClass == null) {
                Java.compileError("Imported class \"" + Java.join(ss, ".") + "\" could not be loaded", location);
                return Java.getIClassLoader().OBJECT;
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
            for (Iterator i = this.importDeclarations.iterator(); i.hasNext();) {
                ImportDeclaration id = (ImportDeclaration) i.next();
                if (id instanceof TypeImportOnDemandDeclaration) {
                    packages.add(((TypeImportOnDemandDeclaration) id).getIdentifiers());
                }
            }
            for (Iterator i = packages.iterator(); i.hasNext();) {
                String[] ss = (String[]) i.next();
                String[] ss2 = new String[ss.length + 1];
                System.arraycopy(ss, 0, ss2, 0, ss.length);
                ss2[ss.length] = simpleTypeName;
                IClass iClass = Java.loadFullyQualifiedClass(ss2);
                if (iClass != null) {
                    if (importedClass != null && importedClass != iClass) Java.compileError("Ambiguous class name: \"" + importedClass + "\" vs. \"" + iClass + "\"", location);
                    importedClass = iClass;
                }
            }
            if (importedClass == null) return null;

            // Put in cache and return.
            this.onDemandImportableTypes.put(simpleTypeName, importedClass);
            return importedClass;
        }
        private final Map onDemandImportableTypes = new HashMap();   // String simpleTypeName => IClass

        public final void visit(Visitor visitor) {
            visitor.visitCompilationUnit(this);
        }

        PackageDeclaration optionalPackageDeclaration    = null;
        final List         importDeclarations            = new ArrayList(); // ImportDeclaration
        final List         packageMemberTypeDeclarations = new ArrayList(); // PackageMemberTypeDeclaration
    }

    /**
     * Represents a package declaration like<pre>
     *     package com.acme.tools;</pre>
     */
    public static class PackageDeclaration extends Located {
        public PackageDeclaration(Location location, String packageName) {
            super(location);
            this.packageName = packageName;
        }
        public String getPackageName() { return this.packageName; }
        private final String packageName;
    }

    public abstract static class ImportDeclaration extends Located {
        public ImportDeclaration(Location location) {
            super(location);
        }
        public abstract void visit(Visitor visitor);
    }

    /**
     * Represents a single type import like<pre>
     *     import java.util.Map;</pre>
     */
    public static class SingleTypeImportDeclaration extends ImportDeclaration {
        public SingleTypeImportDeclaration(Location location, String[] identifiers) {
            super(location);
            this.identifiers = identifiers;
        }
        public String[] getIdentifiers() { return this.identifiers; }
        public final void visit(Visitor visitor) { visitor.visitSingleTypeImportDeclaration(this); }

        private final String[] identifiers;
    }

    /**
     * Represents a type-import-on-demand like<pre>
     *     import java.util.*;</pre>
     */
    public static class TypeImportOnDemandDeclaration extends ImportDeclaration {
        public TypeImportOnDemandDeclaration(Location location, String[] identifiers) {
            super(location);
            this.identifiers = identifiers;
        }
        public String[] getIdentifiers() { return this.identifiers; }
        public final void visit(Visitor visitor) { visitor.visitTypeImportOnDemandDeclaration(this); }

        private final String[] identifiers;
    }

    public static IClassLoader getIClassLoader() {
        IClassLoader res = (IClassLoader) Java.iClassLoader.get();
        if (res == null) throw new RuntimeException("S.N.O.: Null IClassLoader");
        return res;
    }

    /**
     * Set the given {@link IClassLoader} as the "current" {@link IClassLoader} and return
     * the previously active {@link IClassLoader} (which may be <code>null</code>).
     * @param newIClassLoader The new {@link IClassLoader}
     * @return The previous {@link IClassLoader}
     */
    public static IClassLoader replaceIClassLoader(IClassLoader newIClassLoader) {
        IClassLoader oldIClassLoader = (IClassLoader) Java.iClassLoader.get();
        Java.iClassLoader.set(newIClassLoader);
        return oldIClassLoader;
    }

    private static CodeContext getCodeContext() {
        CodeContext res = (CodeContext) Java.codeContext.get();
        if (res == null) throw new RuntimeException("S.N.O.: Null CodeContext");
        return res;
    }

    private static CodeContext replaceCodeContext(CodeContext newCodeContext) {
        CodeContext oldCodeContext = (CodeContext) Java.codeContext.get();
        Java.codeContext.set(newCodeContext);
        return oldCodeContext;
    }

    private static DebuggingInformation getDebuggingInformation() {
        DebuggingInformation res = (DebuggingInformation) Java.debuggingInformation.get();
        if (res == null) throw new RuntimeException("S.N.O.: Null DebuggingInformation");
        return res;
    }

    private static DebuggingInformation replaceDebuggingInformation(
        DebuggingInformation newDebuggingInformation
    ) {
        DebuggingInformation oldDebuggingInformation = (DebuggingInformation) Java.debuggingInformation.get();
        Java.debuggingInformation.set(newDebuggingInformation);
        return oldDebuggingInformation;
    }

    private static List getGeneratedClassFiles() { // ClassFile
        List res = (List) Java.generatedClassFiles.get();
        if (res == null) throw new RuntimeException("S.N.O.: Null generatedClassFiles");
        return res;
    }

    private static List replaceGeneratedClassFiles( // ClassFile
        List newGeneratedClassFiles // ClassFile
    ) {
        List oldGeneratedClassFiles = (List) Java.generatedClassFiles.get();
        Java.generatedClassFiles.set(newGeneratedClassFiles);
        return oldGeneratedClassFiles;
    }

    public interface TypeDeclaration extends Locatable, Scope {

        /**
         * Compile yourself.
         * @throws CompileException
         */
        void compile() throws CompileException;

        /**
         * Return the member type with the given name.
         * @return <code>null</code> if a member type with that name is not declared
         */
        MemberTypeDeclaration getMemberTypeDeclaration(String name);

        /**
         * Determine the effective class name, e.g. "pkg.Outer$Inner".
         */
        String getClassName();

        /**
         * Creates a unique name for a local class or interface.
         */
        String createLocalTypeName(String localTypeName);

        /**
         * Creates a unique name for an anonymous class.
         */
        String createAnonymousClassName();

        void visit(Visitor visitor);
    }

    /**
     * Represents a class or interface declaration on compilation unit level. These are called
     * "package member types" because they are immediate members of a package, e.g.
     * "java.lang.String".
     */
    interface PackageMemberTypeDeclaration extends NamedTypeDeclaration {
    }

    /**
     * Represents a class or interface declaration where the immediately enclosing scope is
     * another class or interface declaration.
     */
    interface MemberTypeDeclaration extends NamedTypeDeclaration, TypeBodyDeclaration {
    }

    /**
     * Represents the declaration of a class or an interface that has a name. (All type
     * declarations are named, except for anonymous classes.)
     */
    interface NamedTypeDeclaration extends TypeDeclaration {

        /**
         * Returns the declared (not the fully qualified) name of the class or interface.
         */
        public String getName();
    }

    /**
     * Represents the declaration of an inner class, i.e. a class that exists in the context of
     * zero or more "enclosing instances". These are anonymous classes, local classes and member
     * classes.
     */
    interface InnerClassDeclaration {

        /**
         * Inner classes have zero or more synthetic fields that hold references to their enclosing
         * context:
         * <dl>
         *   <dt><code>this$<i>n</i></code></dt>
         *   <dd>
         *     (Mandatory for non-private non-static member classes; optional for private non-static
         *     member classes, local classes in non-static context, and anonymous classes in
         *     non-static context; forbidden for static member classes, local classes in static
         *     context, and anonymous classes in static context)
         *     Holds a reference to the immediately enclosing instance. <code><i>n</i></code> is
         *     N-1 for the Nth nesting level; e.g. the public non-static member class of a
         *     package member class has a synthetic field <code>this$0</code>.
         *   </dd>
         *   <dt><code>val$<i>local-variable-name</i></code></dt>
         *   <dd>
         *     (Allowed for local classes and anonymous classes; forbidden for member classes)
         *     Hold copies of <code>final</code> local variables of the defining context.
         *   </dd>
         * </dl>
         * Notice that these fields are not included in the {@link IClass.IField} array returned
         * by {@link IClass#getDeclaredIFields()}.
         * <p>
         * If a synthetic field with the same name exists already, then it must have the same
         * type and the redefinition is ignored.
         * @param iField
         */
        void defineSyntheticField(IClass.IField iField) throws CompileException;
    }

    public abstract static class AbstractTypeDeclaration extends IClass implements TypeDeclaration {
        private   final Location location;
        protected final Scope    enclosingScope;
        protected final short    modifiers;
        final List               declaredMethods = new ArrayList(); // MethodDeclarator
        final Map                declaredClassesAndInterfaces = new HashMap(); // String declaredName => MemberTypeDeclaration

        public AbstractTypeDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers
        ) {
            this.location       = location;
            this.enclosingScope = enclosingScope;
            this.modifiers      = modifiers;
        }

        public void addDeclaredMethod(MethodDeclarator method) {
            this.declaredMethods.add(method);
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            return this.enclosingScope;
        }

        // Implement TypeDeclaration.
        public void addMemberTypeDeclaration(MemberTypeDeclaration mcoid) {
            this.declaredClassesAndInterfaces.put(mcoid.getName(), mcoid);
        }
        public Collection getMemberTypeDeclarations() {
            return this.declaredClassesAndInterfaces.values();
        }
        public MemberTypeDeclaration getMemberTypeDeclaration(String name) {
            return (MemberTypeDeclaration) this.declaredClassesAndInterfaces.get(name);
        }
        public String createLocalTypeName(String localTypeName) {
            return (
                this.getClassName()
                + '$'
                + ++this.localClassCount
                + '$'
                + localTypeName
            );
        }
        public String createAnonymousClassName() {
            return (
                this.getClassName()
                + '$'
                + ++this.anonymousClassCount
            );
        }

        // Implement "Locatable".
        public Location getLocation() { return this.location; }
        public void throwParseException(String message) throws Parser.ParseException {
            throw new Parser.ParseException(message, this.location);
        }
        public void compileError(String message) throws CompileException {
            Java.compileError(message, this.location);
        }

        // Implement "IClass".
        protected IClass.IMethod[] getDeclaredIMethods2() {
            IClass.IMethod[] res = new IClass.IMethod[this.declaredMethods.size()];
            int i = 0;
            for (Iterator it = this.declaredMethods.iterator(); it.hasNext();) {
                res[i++] = ((MethodDeclarator) it.next()).toIMethod();
            }
            return res;
        }
        protected IClass[] getDeclaredIClasses2() {
            Collection dcai = this.declaredClassesAndInterfaces.values();
            return (IClass[]) dcai.toArray(new IClass[dcai.size()]);
        }
        protected IClass getDeclaringIClass2() { return null; }
        protected IClass getOuterIClass2() throws Java.CompileException { return null; }
        protected final String getDescriptor2() {
            return Descriptor.fromClassName(this.getClassName());
        }

        public boolean isArray() { return false; }
        protected IClass  getComponentType2() { return null; }
        public boolean isPrimitive() { return false; }
        public boolean isPrimitiveNumeric() { return false; }

        abstract public String toString();

        public int anonymousClassCount = 0; // For naming anonymous classes.
        public int localClassCount = 0;     // For naming local classes.
    }

    public abstract static class ClassDeclaration extends AbstractTypeDeclaration {
        final List constructors = new ArrayList(); // ConstructorDeclarator
        final List variableDeclaratorsAndInitializers = new ArrayList(); // TypeBodyDeclaration

        public ClassDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers
        ) throws Parser.ParseException {
            super(location, enclosingScope, modifiers);
        }

        public void addConstructor(ConstructorDeclarator cd) {
            this.constructors.add(cd);
        }
        public void addVariableDeclaratorOrInitializer(TypeBodyDeclaration tbd) {
            this.variableDeclaratorsAndInitializers.add(tbd);
        }

        // Compile time members.

        // Implement "IClass".
        public boolean isPublic() { return (this.modifiers & Mod.PUBLIC) != 0; }
        public boolean isFinal() { return (this.modifiers & Mod.FINAL) != 0; }
        public boolean isInterface() { return false; }
        public boolean isAbstract() { return (this.modifiers & Mod.ABSTRACT) != 0; }
        protected IClass.IConstructor[] getDeclaredIConstructors2() {
            ConstructorDeclarator[] cs = this.getConstructors();

            IClass.IConstructor[] res = new IClass.IConstructor[cs.length];
            for (int i = 0; i < cs.length; ++i) res[i] = cs[i].toIConstructor();
            return res;
        }
        protected IClass.IField[] getDeclaredIFields2() {
            List l = new ArrayList(); // IClass.IField

            // Determine variable declarators of type declaration.
            for (int i = 0; i < this.variableDeclaratorsAndInitializers.size(); ++i) {
                BlockStatement vdoi = (BlockStatement) this.variableDeclaratorsAndInitializers.get(i);
                if (vdoi instanceof FieldDeclarator) {
                    FieldDeclarator fd = (FieldDeclarator) vdoi;
                    IClass.IField[] flds = fd.getIFields();
                    for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                }
            }

            return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
        }

        // Implement InnerClassDeclaration.
        public void defineSyntheticField(IClass.IField iField) throws CompileException {
            if (!(this instanceof InnerClassDeclaration)) throw new RuntimeException();

            IClass.IField if2 = (IClass.IField) this.syntheticFields.get(iField.getName());
            if (if2 != null) {
                if (iField.getType() != if2.getType()) throw new RuntimeException();
                return;
            }
            this.syntheticFields.put(iField.getName(), iField);
        }

        /**
         * Return the declared constructors, or the default constructor.
         */
        private ConstructorDeclarator[] getConstructors() {
            if (this.constructors.isEmpty()) {
                ConstructorDeclarator defaultConstructor = new ConstructorDeclarator(
                    this.getLocation(),          // location
                    this,                        // declaringClass
                    Mod.PUBLIC,                  // modifiers
                    new Java.FormalParameter[0], // formalParameters
                    new Java.Type[0]             // thrownExceptions
                );
                defaultConstructor.setBody(new Block(this.getLocation(), (Scope) this));
                return new ConstructorDeclarator[] { defaultConstructor };
            }

            return (ConstructorDeclarator[]) this.constructors.toArray(new ConstructorDeclarator[this.constructors.size()]);
        }

        public void compile() throws CompileException {

            // Determine implemented interfaces.
            IClass[] iis = this.getInterfaces();
            String[] interfaceDescriptors = new String[iis.length];
            for (int i = 0; i < iis.length; ++i) interfaceDescriptors[i] = iis[i].getDescriptor();

            // Create "ClassFile" object.
            ClassFile cf = new ClassFile(
                (short) (this.modifiers | Mod.SUPER), // accessFlags
                this.getDescriptor(),                 // thisClassFD
                this.getSuperclass().getDescriptor(), // superClassFD
                interfaceDescriptors                  // interfaceFDs
            );

            // Add InnerClasses attribute entry for this class declaration.
            if (this.enclosingScope instanceof CompilationUnit) {
                ;
            } else
            if (this.enclosingScope instanceof Block) {
                short innerClassInfoIndex = cf.addConstantClassInfo(this.getDescriptor());
                short innerNameIndex = (
                    this instanceof NamedTypeDeclaration ?
                    cf.addConstantUtf8Info(((NamedTypeDeclaration) this).getName()) :
                    (short) 0
                );
                cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                    innerClassInfoIndex, // innerClassInfoIndex
                    (short) 0,           // outerClassInfoIndex
                    innerNameIndex,      // innerNameIndex
                    this.modifiers       // innerClassAccessFlags
                ));
            } else
            if (this.enclosingScope instanceof AbstractTypeDeclaration) {
                short innerClassInfoIndex = cf.addConstantClassInfo(this.getDescriptor());
                short outerClassInfoIndex = cf.addConstantClassInfo(((AbstractTypeDeclaration) this.enclosingScope).getDescriptor());
                short innerNameIndex      = cf.addConstantUtf8Info(((MemberTypeDeclaration) this).getName());
                cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                    innerClassInfoIndex, // innerClassInfoIndex
                    outerClassInfoIndex, // outerClassInfoIndex
                    innerNameIndex,      // innerNameIndex
                    this.modifiers       // innerClassAccessFlags
                ));
            }

            // Set "SourceFile" attribute.
            if (Java.getDebuggingInformation().contains(DebuggingInformation.SOURCE)) {
                String sourceFileName;
                {
                    String s = this.getLocation().getFileName();
                    if (s != null) {
                        sourceFileName = new File(s).getName();
                    } else if (this instanceof NamedTypeDeclaration) {
                        sourceFileName = ((NamedTypeDeclaration) this).getName() + ".java";
                    } else {
                        sourceFileName = "ANONYMOUS.java";
                    }
                }
                cf.addSourceFileAttribute(sourceFileName);
            }

            // Optional: Generate and compile class initialization method.
            {
                MethodDeclarator classInitializationMethod = new MethodDeclarator(
                    this.getLocation(),     // location
                    this,                   // declaringType
                    (short) (               // modifiers
                        Mod.STATIC |
                        Mod.PUBLIC
                    ),
                    new BasicType(          // type
                        this.getLocation(),
                        BasicType.VOID
                    ),
                    "<clinit>",             // name
                    new FormalParameter[0], // formalParameters
                    new ReferenceType[0]    // thrownExceptions
                );
                Block b = new Block(
                    this.getLocation(),
                    classInitializationMethod
                );
                for (Iterator it = this.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                    TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                    if (tbd.isStatic()) b.addStatement((BlockStatement) tbd);
                }
                classInitializationMethod.setBody(b);

                // Create class initialization method iff there is any initialization code.
                if (b.generatesCode()) classInitializationMethod.compile(cf);
            }

            // Compile declared methods.
            // (As a side effects, this fills the "syntheticFields" map.)
            for (int i = 0; i < this.declaredMethods.size(); ++i) {
                ((MethodDeclarator) this.declaredMethods.get(i)).compile(cf);
            }

            // Compile declared constructors.
            int declaredMethodCount = this.declaredMethods.size();
            {
                int syntheticFieldCount = this.syntheticFields.size();
                ConstructorDeclarator[] cds = this.getConstructors();
                for (int i = 0; i < cds.length; ++i) {
                    cds[i].compile(cf);
                    if (syntheticFieldCount != this.syntheticFields.size()) throw new RuntimeException("SNO: Compilation of constructor \"" + cds[i] + "\" (" + cds[i].getLocation() +") added synthetic fields!?");
                }
            }

            // As a side effect of compiling methods and constructors, synthetic "class-dollar"
            // methods (which implement class literals) are generated on-the fly. Compile these.
            for (int i = declaredMethodCount; i < this.declaredMethods.size(); ++i) {
                ((MethodDeclarator) this.declaredMethods.get(i)).compile(cf);
            }

            // Class and instance variables.
            for (Iterator it = this.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                if (!(tbd instanceof FieldDeclarator)) continue;

                FieldDeclarator fd = (FieldDeclarator) tbd;
                for (int j = 0; j < fd.variableDeclarators.length; ++j) {
                    VariableDeclarator vd = fd.variableDeclarators[j];
                    Type type = fd.type;
                    for (int k = 0; k < vd.brackets; ++k) type = new ArrayType(type);

                    Object cv = null;
                    if (
                        (fd.modifiers & (Mod.STATIC | Mod.FINAL)) == (Mod.STATIC | Mod.FINAL) &&
                        vd.optionalInitializer != null
                    ) {
                        cv = vd.optionalInitializer.getConstantValue();
                        if (cv == Rvalue.CONSTANT_VALUE_NULL) cv = null;
                    }

                    cf.addFieldInfo(
                        fd.modifiers,                   // modifiers
                        vd.name,                        // fieldName
                        type.getType().getDescriptor(), // fieldTypeFD
                        cv                              // optionalConstantValue
                    );
                }
            }

            // Synthetic fields.
            for (Iterator it = this.syntheticFields.values().iterator(); it.hasNext();) {
                IClass.IField f = (IClass.IField) it.next();
                cf.addFieldInfo(
                    (short) 0,                   // modifiers,
                    f.getName(),                 // fieldName,
                    f.getType().getDescriptor(), // fieldTypeFD,
                    null                         // optionalConstantValue
                );
            }

            // Member types.
            for (Iterator it = this.getMemberTypeDeclarations().iterator(); it.hasNext();) {
                AbstractTypeDeclaration atd = ((AbstractTypeDeclaration) it.next());
                atd.compile();

                // Add InnerClasses attribute entry for member type declaration.
                short innerClassInfoIndex = cf.addConstantClassInfo(atd.getDescriptor());
                short outerClassInfoIndex = cf.addConstantClassInfo(this.getDescriptor());
                short innerNameIndex      = cf.addConstantUtf8Info(((MemberTypeDeclaration) atd).getName());
                cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                    innerClassInfoIndex, // innerClassInfoIndex
                    outerClassInfoIndex, // outerClassInfoIndex
                    innerNameIndex,      // innerNameIndex
                    this.modifiers       // innerClassAccessFlags
                ));
            }

            // Add the generated class file to a thread-local store.
            Java.getGeneratedClassFiles().add(cf);
        }

        // All field names start with "this$" or "val$".
        private final SortedMap syntheticFields = new TreeMap(); // String name => IClass.IField
    }

    public static final class AnonymousClassDeclaration extends ClassDeclaration implements InnerClassDeclaration {
        /*final*/ Type   baseType;  // Base class or interface

        private /*final*/ String className; // Effective class name.

        public AnonymousClassDeclaration(
            Location location,
            Scope    enclosingScope,
            Type     baseType
        ) throws Parser.ParseException {
            super(
                location,                         // location
                enclosingScope,                   // enclosingScope
                (short) (Mod.PRIVATE | Mod.FINAL) // modifiers
            );
            this.baseType = baseType;

            Scope s = this.getEnclosingScope();
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            this.className = immediatelyEnclosingTypeDeclaration.createAnonymousClassName();
        }

        // Implement IClass.
        protected IClass getDeclaringIClass2() {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            return (IClass) s.getEnclosingScope();
        }
        protected IClass getOuterIClass2() {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            if (((TypeBodyDeclaration) s).isStatic()) return null;
            return (IClass) s.getEnclosingScope();
        }
        protected IClass getSuperclass2() throws CompileException {
            IClass bt = this.baseType.getType();
            return bt.isInterface() ? Java.getIClassLoader().OBJECT : bt;
        }
        protected IClass[] getInterfaces2() throws CompileException {
            IClass bt = this.baseType.getType();
            return bt.isInterface() ? new IClass[] { bt } : new IClass[0];
        }

        public void compile() throws CompileException {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
            final TypeBodyDeclaration tbd = (TypeBodyDeclaration) s;

            // Define a synthetic "this$..." field for a non-static function context. (It is
            // very difficult to tell whether the anonymous constructor will require its
            // enclosing instance.)
            if (!tbd.isStatic()) {
                final int nesting = Java.getOuterClasses(this).size();
                this.defineSyntheticField(new IClass.IField() {
                    public Object  getConstantValue() { return null; }
                    public String  getName()          { return "this$" + (nesting - 2); }
                    public IClass  getType()          { return (IClass) tbd.getDeclaringType(); }
                    public boolean isStatic()         { return false; }
                    public int     getAccess()        { return IClass.PACKAGE; }
                });
            }
            super.compile();
        }

        public final void visit(Visitor visitor) {
            visitor.visitAnonymousClassDeclaration(this);
        }

        // Implement TypeDeclaration.
        public String getClassName() { return this.className; }
        public String toString() { return "ANONYMOUS"; }
    }

    public abstract static class NamedClassDeclaration extends ClassDeclaration implements NamedTypeDeclaration {
        protected final String name;
        final Type   optionalExtendedType;
        final Type[] implementedTypes;

        public NamedClassDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) throws Parser.ParseException {
            super(location, enclosingScope, modifiers);
            this.name                 = name;
            this.optionalExtendedType = optionalExtendedType;
            this.implementedTypes     = implementedTypes;
        }

        // Implement IClass.
        protected IClass getSuperclass2() throws CompileException {
            return (
                this.optionalExtendedType == null ?
                Java.getIClassLoader().OBJECT:
                this.optionalExtendedType.getType()
            );
        }
        protected IClass[] getInterfaces2() throws CompileException {
            IClass[] res = new IClass[this.implementedTypes.length];
            for (int i = 0; i < res.length; ++i) {
                res[i] = this.implementedTypes[i].getType();
            }
            return res;
        }

        public String getName() { return this.name; }
        public String toString() { return this.name; }
    }

    public static final class MemberClassDeclaration extends NamedClassDeclaration implements MemberTypeDeclaration, InnerClassDeclaration {
        public MemberClassDeclaration(
            Location             location,
            NamedTypeDeclaration declaringType,
            short                modifiers,
            String               name,
            Type                 optionalExtendedType,
            Type[]               implementedTypes
        ) throws Parser.ParseException {
            super(
                location,              // location
                (Scope) declaringType, // enclosingScope
                modifiers,             // modifiers
                name,                  // name
                optionalExtendedType,  // optionalExtendedType
                implementedTypes       // implementedTypes
            );

            // Check for redefinition of member type.
            MemberTypeDeclaration mcoid = declaringType.getMemberTypeDeclaration(name);
            if (mcoid != null) this.throwParseException("Redeclaration of class \"" + name + "\", previously declared in " + mcoid.getLocation());
        }

        protected IClass getDeclaringIClass2() { return (IClass) this.getDeclaringType(); }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() throws CompileException {
            return (
                this.getDeclaringType() instanceof ClassDeclaration &&
                (this.modifiers & Mod.STATIC) == 0
            ) ? this.getDeclaringIClass() : null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            return (
                this.getDeclaringType().getClassName()
                + '$'
                + this.getName()
            );
        }

        public void compile() throws CompileException {

            // Define a synthetic "this$..." field for a non-static member class.
            if ((this.modifiers & Mod.STATIC) == 0) {
                final int nesting = Java.getOuterClasses(this).size();
                this.defineSyntheticField(new IClass.IField() {
                    public Object  getConstantValue() { return null; }
                    public String  getName()          { return "this$" + (nesting - 2); }
                    public IClass  getType()          { return (IClass) MemberClassDeclaration.this.getDeclaringType(); }
                    public boolean isStatic()         { return false; }
                    public int     getAccess()        { return IClass.PACKAGE; }
                });
            }
            super.compile();
        }

        public void visit(Visitor visitor) { visitor.visitMemberClassDeclaration(this); }
    }

    public static final class LocalClassDeclaration extends NamedClassDeclaration implements InnerClassDeclaration {
        private final String className;

        public LocalClassDeclaration(
            Location location,
            Block    declaringBlock,
            short    modifiers,
            String   name,
            Type     optionalExtendedType,
            Type[]   implementedTypes
        ) throws Parser.ParseException {
            super(
                location,               // location
                (Scope) declaringBlock, // enclosingScope
                modifiers,              // modifiers
                name,                   // name
                optionalExtendedType,   // optionalExtendedType
                implementedTypes        // implementedTypes
            );

            // Check for redefinition.
            for (Scope s = declaringBlock; s instanceof Block; s = s.getEnclosingScope()) {
                Block b = (Block) s;
                LocalClassDeclaration lcd = b.getLocalClassDeclaration(name);
                if (lcd != null) this.throwParseException("Redeclaration of local class \"" + name + "\"; previously declared in " + lcd.getLocation());
            }

            // Determine effective class name.
            Scope s = this.getEnclosingScope();
            while (!(s instanceof TypeDeclaration)) s = s.getEnclosingScope();
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            this.className = immediatelyEnclosingTypeDeclaration.createLocalTypeName(name);
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            Scope s = this.getEnclosingScope();
            for (; !(s instanceof FunctionDeclarator); s = s.getEnclosingScope());
            boolean isStaticMethod = (s instanceof MethodDeclarator) && (((FunctionDeclarator) s).modifiers & Mod.STATIC) != 0;
            for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope());
            TypeDeclaration immediatelyEnclosingTypeDeclaration = (TypeDeclaration) s;
            return (
                immediatelyEnclosingTypeDeclaration instanceof ClassDeclaration &&
                !isStaticMethod
            ) ? (IClass) immediatelyEnclosingTypeDeclaration : null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            return this.className;
        }

        public void compile() throws CompileException {

            // Make this local class visible in the block scope.
            ((Block) this.getEnclosingScope()).declaredLocalClasses.put(this.getName(), this);

            // Define a synthetic "this$..." field for the local class if the enclosing method is non-static.
            {
                List ocs = Java.getOuterClasses(this);
                final int nesting = ocs.size();
                if (nesting >= 2) {
                    final IClass enclosingInstanceType = (IClass) ocs.get(1);
                    this.defineSyntheticField(new IClass.IField() {
                        public Object  getConstantValue()                { return null; }
                        public String  getName()                         { return "this$" + (nesting - 2); }
                        public IClass  getType() throws CompileException { return enclosingInstanceType; }
                        public boolean isStatic()                        { return false; }
                        public int     getAccess()                       { return IClass.PACKAGE; }
                    });
                }
            }

            super.compile();
        }

        public final void visit(Visitor visitor) { visitor.visitLocalClassDeclaration(this); }
    }

    public static final class PackageMemberClassDeclaration extends NamedClassDeclaration implements PackageMemberTypeDeclaration {
        public PackageMemberClassDeclaration(
            Location        location,
            CompilationUnit declaringCompilationUnit,
            short           modifiers,
            String          name,
            Type            optionalExtendedType,
            Type[]          implementedTypes
        ) throws Parser.ParseException {
            super(
                location,                         // location
                (Scope) declaringCompilationUnit, // enclosingScope
                modifiers,                        // modifiers
                name,                             // name
                optionalExtendedType,             // optionalExtendedType
                implementedTypes                  // implementedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (
                Mod.PROTECTED |
                Mod.PRIVATE |
                Mod.STATIC
            )) != 0) this.throwParseException("Modifiers \"protected\", \"private\" and \"static\" not allowed in package member class declaration");

            // Check for conflict with single-type-import (7.6).
            {
                String[] ss = declaringCompilationUnit.getSingleTypeImport(name);
                if (ss != null) throwParseException("Package member class declaration \"" + name + "\" conflicts with single-type-import \"" + Java.join(ss, ".") + "\"");
            }

            // Check for redefinition within compilation unit (7.6).
            {
                PackageMemberTypeDeclaration pmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(name);
                if (pmtd != null) this.throwParseException("Redeclaration of class \"" + name + "\", previously declared in " + pmtd.getLocation());
            }
        }

        // Implement ClassDeclaration.
        protected IClass getOuterIClass2() {
            return null;
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) className = compilationUnit.optionalPackageDeclaration.getPackageName() + '.' + className;

            return className;
        }

        public final void visit(Visitor visitor) { visitor.visitPackageMemberClassDeclaration(this); }
    }

    public abstract static class InterfaceDeclaration extends AbstractTypeDeclaration implements NamedTypeDeclaration {
        protected /*final*/ String name;

        protected InterfaceDeclaration(
            Location location,
            Scope    enclosingScope,
            short    modifiers,
            String   name,
            Type[]   extendedTypes
        ) throws Parser.ParseException {
            super(
                location,
                enclosingScope,
                modifiers
            );
            this.name = name;
            this.extendedTypes = extendedTypes;
        }

        public String getName() { return this.name; }
        public String toString() { return this.name; }

        public void addConstantDeclaration(FieldDeclarator fd) {
            this.constantDeclarations.add(fd);
        }

        // Implement "IClass".
        protected IClass getSuperclass2() {
            return null;
        }
        protected IClass[] getInterfaces2() throws CompileException {
            IClass[] res = new IClass[this.extendedTypes.length];
            for (int i = 0; i < res.length; ++i) {
                res[i] = this.extendedTypes[i].getType();
            }
            return res;
        }
        public boolean isPublic() { return (this.modifiers & Mod.PUBLIC) != 0; }
        public boolean isFinal() { return false; }
        public boolean isInterface() { return true; }
        public boolean isAbstract() { return true; }
        protected IClass.IConstructor[] getDeclaredIConstructors2() {
            return new IClass.IConstructor[0];
        }
        protected IClass.IField[] getDeclaredIFields2() {
            List l = new ArrayList();

            // Determine static fields.
            for (int i = 0; i < this.constantDeclarations.size(); ++i) {
                BlockStatement bs = (BlockStatement) this.constantDeclarations.get(i);
                if (bs instanceof FieldDeclarator) {
                    FieldDeclarator fd = (FieldDeclarator) bs;
                    IClass.IField[] flds = fd.getIFields();
                    for (int j = 0; j < flds.length; ++j) l.add(flds[j]);
                }
            }

            return (IClass.IField[]) l.toArray(new IClass.IField[l.size()]);
        }

        public void compile() throws CompileException {

            // Determine extended interfaces.
            this.interfaces = new IClass[this.extendedTypes.length];
            String[] interfaceDescriptors = new String[this.interfaces.length];
            for (int i = 0; i < this.extendedTypes.length; ++i) {
                this.interfaces[i] = this.extendedTypes[i].getType();
                interfaceDescriptors[i] = this.interfaces[i].getDescriptor();
            }

            // Create "ClassFile" object.
            ClassFile cf = new ClassFile(
                (short) (             // accessFlags
                    this.modifiers |
                    Mod.SUPER |
                    Mod.INTERFACE |
                    Mod.ABSTRACT
                ),
                this.getDescriptor(), // thisClassFD
                Descriptor.OBJECT,    // superClassFD
                interfaceDescriptors  // interfaceFDs
            );

            // Set "SourceFile" attribute.
            if (Java.getDebuggingInformation().contains(DebuggingInformation.SOURCE)) {
                String sourceFileName;
                {
                    String s = this.getLocation().getFileName();
                    if (s != null) {
                        sourceFileName = new File(s).getName();
                    } else {
                        sourceFileName = this.getName() + ".java";
                    }
                }
                cf.addSourceFileAttribute(sourceFileName);
            }

            // Interface initialization method.
            if (!this.constantDeclarations.isEmpty()) {
                MethodDeclarator interfaceInitializationMethod = new MethodDeclarator(
                    this.getLocation(),     // location
                    this,                   // declaringType
                    (short) (               // modifiers
                        Mod.STATIC |
                        Mod.PUBLIC
                    ),
                    new BasicType(          // type
                        this.getLocation(),
                        BasicType.VOID
                    ),
                    "<clinit>",             // name
                    new FormalParameter[0], // formalParameters
                    new ReferenceType[0]    // thrownExcaptions
                );
                Block b = new Block(
                    this.getLocation(),
                    interfaceInitializationMethod
                );
                b.addStatements(this.constantDeclarations);
                interfaceInitializationMethod.setBody(b);

                // Create interface initialization method iff there is any initialization code.
                if (b.generatesCode()) interfaceInitializationMethod.compile(cf);
            }

            // Methods.
            // Notice that as a side effect of compiling methods, synthetic "class-dollar"
            // methods (which implement class literals) are generated on-the fly. Hence, we
            // must not use an Iterator here.
            for (int i = 0; i < this.declaredMethods.size(); ++i) {
                ((MethodDeclarator) this.declaredMethods.get(i)).compile(cf);
            }

            // Class variables.
            for (int i = 0; i < this.constantDeclarations.size(); ++i) {
                BlockStatement bs = (BlockStatement) this.constantDeclarations.get(i);
                if (!(bs instanceof FieldDeclarator)) continue;
                FieldDeclarator fd = (FieldDeclarator) bs;
                for (int j = 0; j < fd.variableDeclarators.length; ++j) {
                    VariableDeclarator vd = fd.variableDeclarators[j];
                    Type type = fd.type;
                    for (int k = 0; k < vd.brackets; ++k) type = new ArrayType(type);
                    cf.addFieldInfo(
                        fd.modifiers,                   // accessFlags
                        vd.name,                        // fieldName
                        type.getType().getDescriptor(), // fieldTypeFD
                        (                               // optionalConstantValue
                            (fd.modifiers & Mod.FINAL) != 0 &&
                            vd.optionalInitializer != null
                        ) ? vd.optionalInitializer.getConstantValue() : null
                    );
                }
            }

            // Member types.
            for (Iterator it = this.getMemberTypeDeclarations().iterator(); it.hasNext();) {
                AbstractTypeDeclaration atd = ((AbstractTypeDeclaration) it.next());
                atd.compile();

                // Add InnerClasses attribute entry for member type declaration.
                short innerClassInfoIndex = cf.addConstantClassInfo(atd.getDescriptor());
                short outerClassInfoIndex = cf.addConstantClassInfo(this.getDescriptor());
                short innerNameIndex      = cf.addConstantUtf8Info(((MemberTypeDeclaration) atd).getName());
                cf.addInnerClassesAttributeEntry(new ClassFile.InnerClassesAttribute.Entry(
                    innerClassInfoIndex, // innerClassInfoIndex
                    outerClassInfoIndex, // outerClassInfoIndex
                    innerNameIndex,      // innerNameIndex
                    this.modifiers       // innerClassAccessFlags
                ));
            }

            // Add the generated class file to a thread-local store.
            Java.getGeneratedClassFiles().add(cf);
        }

        /*final*/ Type[] extendedTypes;
        final List   constantDeclarations = new ArrayList(); // FieldDeclarator

        // Set during "compile()".
        private IClass[] interfaces = null;
    }

    public static final class MemberInterfaceDeclaration extends InterfaceDeclaration implements MemberTypeDeclaration {
        public MemberInterfaceDeclaration(
            Location             location,
            NamedTypeDeclaration declaringType,
            short                modifiers,
            String               name,
            Type[]               extendedTypes
        ) throws Parser.ParseException {
            super(
                location,              // location
                (Scope) declaringType, // enclosingScope
                modifiers,             // modifiers
                name,                  // name
                extendedTypes          // extendedTypes
            );

            // Check for redefinition of member type.
            MemberTypeDeclaration mcoid = declaringType.getMemberTypeDeclaration(name);
            if (mcoid != null) this.throwParseException("Redeclaration of interface \"" + name + "\", previously declared in " + mcoid.getLocation());
        }

        // Implement IClass.
        protected IClass getDeclaringIClass2() { return (IClass) this.getEnclosingScope(); }

        // Implement TypeDeclaration.
        public String getClassName() {
            NamedTypeDeclaration declaringType = (NamedTypeDeclaration) this.getEnclosingScope();
            return (
                declaringType.getClassName()
                + '$'
                + this.getName()
            );
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return (TypeDeclaration) this.getEnclosingScope();
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        public final void visit(Visitor visitor) { visitor.visitMemberInterfaceDeclaration(this); }
    }

    public static final class PackageMemberInterfaceDeclaration extends InterfaceDeclaration implements PackageMemberTypeDeclaration {
        public PackageMemberInterfaceDeclaration(
            Location        location,
            CompilationUnit declaringCompilationUnit,
            short           modifiers,
            String          name,
            Type[]          extendedTypes
        ) throws Parser.ParseException {
            super(
                location,                         // location
                (Scope) declaringCompilationUnit, // enclosingScope
                modifiers,                        // modifiers
                name,                             // name
                extendedTypes                     // extendedTypes
            );

            // Check for forbidden modifiers (JLS 7.6).
            if ((modifiers & (
                Mod.PROTECTED |
                Mod.PRIVATE |
                Mod.STATIC
            )) != 0) throwParseException("Modifiers \"protected\", \"private\" and \"static\" not allowed in package member interface declaration");

            // Check for conflict with single-type-import (JLS 7.6).
            {
                String[] ss = declaringCompilationUnit.getSingleTypeImport(name);
                if (ss != null) throwParseException("Package member interface declaration \"" + name + "\" conflicts with single-type-import \"" + Java.join(ss, ".") + "\"");
            }

            // Check for redefinition within compilation unit (JLS 7.6).
            {
                PackageMemberTypeDeclaration pmtd = declaringCompilationUnit.getPackageMemberTypeDeclaration(name);
                if (pmtd != null) this.throwParseException("Redeclaration of interface \"" + name + "\", previously declared in " + pmtd.getLocation());
            }
        }

        // Implement TypeDeclaration.
        public String getClassName() {
            String className = this.getName();

            CompilationUnit compilationUnit = (CompilationUnit) this.getEnclosingScope();
            if (compilationUnit.optionalPackageDeclaration != null) className = compilationUnit.optionalPackageDeclaration.getPackageName() + '.' + className;

            return className;
        }

        public final void visit(Visitor visitor) { visitor.visitPackageMemberInterfaceDeclaration(this); }
    }

    /**
     * Representation of a "ClassBodyDeclaration" or an "InterfaceMemberDeclaration". These are:
     * <ul>
     *   <li>Field declarators
     *   <li>Method declarators
     *   <li>Static and non-static initializers
     *   <li>Member type declarations
     * </ul>
     */
    public interface TypeBodyDeclaration extends Locatable, Scope {
        TypeDeclaration getDeclaringType();
        boolean         isStatic();
        void            visit(Visitor visitor);
    }

    public abstract static class AbstractTypeBodyDeclaration extends Located implements TypeBodyDeclaration {
        final TypeDeclaration declaringType;
        final boolean         statiC;

        protected AbstractTypeBodyDeclaration(
            Location        location,
            TypeDeclaration declaringType,
            boolean         statiC
        ) {
            super(location);
            this.declaringType = declaringType;
            this.statiC        = statiC;
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return this.declaringType;
        }

        public boolean isStatic() {
            return this.statiC;
        }

        // Implement Scope.
        public Scope getEnclosingScope() {
            return (Scope) this.declaringType;
        }
    }

    /**
     * Representation of an instance (JLS2 8.6) or static initializer (JLS2 8.7).
     */
    public final static class Initializer extends AbstractTypeBodyDeclaration implements BlockStatement {
        Block block = null;

        public Initializer(
            Location        location,
            TypeDeclaration declaringType,
            boolean         statiC
        ) {
            super(location, declaringType, statiC);
        }

        public void setBlock(Block block) {
            if (this.block != null) throw new RuntimeException();
            this.block = block;
        }

        // Compile time members.

        // Implement BlockStatement.
        public boolean generatesCode() throws CompileException {
            return this.block.generatesCode();
        }
        public boolean compile() throws CompileException {
            return this.block.compile();
        }
        public void leave(IClass optionalStackValueType) {
            ;
        }

        public final void visit(Visitor visitor) { visitor.visitInitializer(this); }
    }

    /**
     * Abstract base class for {@link Java.ConstructorDeclarator} and
     * {@link Java.MethodDeclarator}.
     */
    public abstract static class FunctionDeclarator extends AbstractTypeBodyDeclaration {
        protected final short           modifiers;
        final Type              type;
        final String            name;
        final FormalParameter[] formalParameters;
        protected final Type[]          thrownExceptions;
        Block                   optionalBody = null;

        public FunctionDeclarator(
            Location          location,
            TypeDeclaration   declaringType,
            short             modifiers,
            Type              type,
            String            name,
            FormalParameter[] formalParameters,
            Type[]            thrownExceptions
        ) {
            super(location, declaringType, (modifiers & Mod.STATIC) != 0);
            this.modifiers        = modifiers;
            this.type             = type;
            this.name             = name;
            this.formalParameters = formalParameters;
            this.thrownExceptions = thrownExceptions;
        }

        public void setBody(Block body) {
            if (this.optionalBody != null) throw new RuntimeException("Body must be set exactly once");
            this.optionalBody = body;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() {
            return this.getDeclaringType();
        }

        public short getModifiers() { return this.modifiers; }

        public IClass getReturnType() throws CompileException {
            if (this.returnType == null) {
                this.returnType = this.type.getType();
            }
            return this.returnType;
        }

        public String getName() { return this.name; }

        public FormalParameter[] getFormalParameters() { return this.formalParameters; }

        // Compile time members.

        public void compile(final ClassFile classFile) throws CompileException {
            ClassFile.MethodInfo mi = classFile.addMethodInfo(
                this.modifiers,                       // accessFlags
                this.name,                            // name
                this.toIInvocable().getDescriptor()   // methodMD
            );

            if ((this.modifiers & (Mod.ABSTRACT | Mod.NATIVE)) != 0) return;

            // Create CodeContext.
            final CodeContext codeContext = new CodeContext(
                mi.getClassFile()
            );

            CodeContext savedCodeContext = Java.replaceCodeContext(codeContext);
            try {

                // Define special parameter "this".
                if ((this.modifiers & Mod.STATIC) == 0) {
                    this.allocateLocalVariable((short) 1);
                }

                if (this instanceof ConstructorDeclarator) {
                    ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) this;

                    // Reserve space for synthetic parameters ("this$...", "val$...").
                    for (Iterator it = constructorDeclarator.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
                        IClass.IField sf = (IClass.IField) it.next();
                        constructorDeclarator.syntheticParameters.put(
                            sf.getName(),
                            new LocalVariable(
                                true,                               // finaL
                                sf.getType(),                        // type
                                this.allocateLocalVariable(Descriptor.size(sf.getDescriptor())) // localVariableArrayIndex
                            )
                        );
                    }
                }

                // Add function parameters.
                for (int i = 0; i < this.formalParameters.length; ++i) {
                    FormalParameter fp = this.formalParameters[i];
                    if (this.parameters.containsKey(fp.name)) this.compileError("Redefinition of formal parameter \"" + fp.name + "\"");
                    IClass fpt = fp.type.getType();
                    this.parameters.put(fp.name, new LocalVariable(
                        fp.finaL,
                        fpt,
                        this.allocateLocalVariable(Descriptor.size(fpt.getDescriptor()))
                    ));
                }

                // Compile the function preamble.
                this.compilePreamble();

                // Compile the function body.
                boolean canCompleteNormally = this.optionalBody.compile();
                if (canCompleteNormally) {
                    if (this.getReturnType() != IClass.VOID) this.compileError("Method must return a value");
                    this.writeOpcode(Opcode.RETURN);
                }
            } finally {
                Java.replaceCodeContext(savedCodeContext);
            }

            // Fix up.
            codeContext.fixUp();

            // Relocate.
            codeContext.relocate();

            // Do flow analysis.
            if (Java.DEBUG) {
                try {
                    codeContext.flowAnalysis(this.toString());
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    ;
                }
            } else {
                codeContext.flowAnalysis(this.toString());
            }

            // Add the code context as a code attribute to the MethodInfo.
            final short lntani = (
                Java.getDebuggingInformation().contains(DebuggingInformation.LINES) ?
                classFile.addConstantUtf8Info("LineNumberTable") :
                (short) 0
            );
            mi.addAttribute(new ClassFile.AttributeInfo(classFile.addConstantUtf8Info("Code")) {
                protected void storeBody(DataOutputStream dos) throws IOException {
                    codeContext.storeCodeAttributeBody(dos, lntani);
                }
            });

            // Add "Exceptions" attribute (JVMS 4.7.4).
            {
                final short eani = classFile.addConstantUtf8Info("Exceptions");
                short[] tecciis = new short[this.thrownExceptions.length];
                for (int i = 0; i < this.thrownExceptions.length; ++i) {
                    tecciis[i] = classFile.addConstantClassInfo(this.thrownExceptions[i].getType().getDescriptor());
                }
                mi.addAttribute(new ClassFile.ExceptionsAttribute(eani, tecciis));
            }
        }

        protected abstract void              compilePreamble() throws CompileException;
        protected abstract IClass.IInvocable toIInvocable();

        // Set by "compile()".
        private IClass                  returnType = null;
        final HashMap                   parameters = new HashMap();   // String name => LocalVariable
    }

    public static final class ConstructorDeclarator extends FunctionDeclarator {
        private final ClassDeclaration    declaringClass;
        private final IClass.IConstructor iConstructor;
        ConstructorInvocation             optionalExplicitConstructorInvocation = null;

        public ConstructorDeclarator(
            Location          location,
            ClassDeclaration  declaringClass,
            short             modifiers,
            FormalParameter[] formalParameters,
            Type[]            thrownExceptions
        ) {
            super(
                location,                                // location
                declaringClass,                          // declaringType
                modifiers,                               // modifiers
                new BasicType(location, BasicType.VOID), // type
                "<init>",                                // name
                formalParameters,                        // formalParameters
                thrownExceptions                         // thrownExceptions
            );

            this.declaringClass = declaringClass;

            this.iConstructor = declaringClass.new IConstructor() {

                // Implement IMember.
                public int getAccess() {
                    switch (ConstructorDeclarator.this.modifiers & Mod.PPP) {
                    case Mod.PRIVATE:
                        return IClass.PRIVATE;
                    case Mod.PROTECTED:
                        return IClass.PROTECTED;
                    case Mod.PACKAGE:
                        return IClass.PACKAGE;
                    case Mod.PUBLIC:
                        return IClass.PUBLIC;
                    default:
                        throw new RuntimeException("Invalid access");
                    }
                }

                // Implement IInvocable.
                public String getDescriptor() throws CompileException {
                    if (!(ConstructorDeclarator.this.declaringClass instanceof InnerClassDeclaration)) return super.getDescriptor();

                    List l = new ArrayList();

                    // Convert enclosing instance reference into prepended constructor parameters.
                    IClass outerClass = ConstructorDeclarator.this.declaringClass.getOuterIClass();
                    if (outerClass != null) l.add(outerClass.getDescriptor());

                    // Convert synthetic fields into prepended constructor parameters.
                    for (Iterator it = ConstructorDeclarator.this.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
                        IClass.IField sf = (IClass.IField) it.next();
                        if (sf.getName().startsWith("val$")) l.add(sf.getType().getDescriptor());
                    }
                    FormalParameter[] fps = ConstructorDeclarator.this.getFormalParameters();
                    for (int i = 0; i < fps.length; ++i) {
                        l.add(fps[i].type.getType().getDescriptor());
                    }
                    String[] apd = (String[]) l.toArray(new String[l.size()]);
                    return new MethodDescriptor(apd, Descriptor.VOID).toString();
                }

                public IClass[] getParameterTypes() throws CompileException {
                    FormalParameter[] fps = ConstructorDeclarator.this.getFormalParameters();
                    IClass[] res = new IClass[fps.length];
                    for (int i = 0; i < fps.length; ++i) {
                        res[i] = fps[i].type.getType();
                    }
                    return res;
                }
                public IClass[] getThrownExceptions() throws CompileException {
                    IClass[] res = new IClass[ConstructorDeclarator.this.thrownExceptions.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = ConstructorDeclarator.this.thrownExceptions[i].getType();
                    }
                    return res;
                }

                public String toString() {
                    StringBuffer sb = new StringBuffer();
                    sb.append(ConstructorDeclarator.this.getDeclaringType().getClassName());
                    sb.append('(');
                    FormalParameter[] fps = ConstructorDeclarator.this.getFormalParameters();
                    for (int i = 0; i < fps.length; ++i) {
                        if (i != 0) sb.append(", ");
                        try {
                            sb.append(fps[i].type.getType().toString());
                        } catch (CompileException ex) {
                            sb.append("???");
                        }
                    }
                    return sb.append(')').toString();
                }
            };
        }
        public void setExplicitConstructorInvocation(
            ConstructorInvocation explicitConstructorInvocation
        ) {
            this.optionalExplicitConstructorInvocation = explicitConstructorInvocation;
        }

        // Compile time members.

        private Map syntheticParameters = new HashMap(); // String name => LocalVariable

        // Implement "FunctionDeclarator":
        protected void compilePreamble() throws CompileException {
            if (this.optionalExplicitConstructorInvocation != null) {
                this.optionalExplicitConstructorInvocation.compile();
                if (this.optionalExplicitConstructorInvocation instanceof SuperConstructorInvocation) {
                    this.assignSyntheticParametersToSyntheticFields();
                    this.initializeInstanceVariablesAndInvokeInstanceInitializers();
                }
            } else {

                // Determine qualification for superconstructor invocation.
                QualifiedThisReference qualification = null;
                IClass outerClassOfSuperclass = this.declaringClass.getSuperclass().getOuterIClass();
                if (outerClassOfSuperclass != null) {
                    qualification = new QualifiedThisReference(
                        this.getLocation(),        // location
                        this.declaringClass,       // declaringClass
                        this,                      // optionalFunctionDeclarator
                        outerClassOfSuperclass     // targetIClass
                    );
                }

                // Invoke the superconstructor.
                new SuperConstructorInvocation(
                    this.getLocation(),  // location
                    this.declaringClass, // declaringClass
                    this,                // declaringConstructor
                    qualification,       // optionalQualification
                    new Rvalue[0]        // arguments
                ).compile();
                this.assignSyntheticParametersToSyntheticFields();
                this.initializeInstanceVariablesAndInvokeInstanceInitializers();
            }
        }

        /**
         * Copies the values of the synthetic parameters of this constructor ("this$..." and
         * "val$...") to the synthetic fields of the object ("this$..." and "val$...").
         */
        private void assignSyntheticParametersToSyntheticFields() throws CompileException {
            for (Iterator it = this.declaringClass.syntheticFields.values().iterator(); it.hasNext();) {
                IClass.IField sf = (IClass.IField) it.next();
                LocalVariable syntheticParameter = (LocalVariable) this.syntheticParameters.get(sf.getName());
                if (syntheticParameter == null) throw new RuntimeException("SNO: Synthetic parameter for synthetic field \"" + sf.getName() + "\" not found");
                new ExpressionStatement(
                    new Assignment(  // rvalue
                        this.getLocation(),      // location
                        new FieldAccess(         // lhs
                            this.getLocation(),    // location
                            new ThisReference(     // lhs
                                this.getLocation(),          // location
                                (IClass) this.declaringClass // iClass
                            ),
                            sf                     // field
                        ),
                        "=",                     // operator
                        new LocalVariableAccess( // rhs
                            this.getLocation(),    // location
                            syntheticParameter     // localVariable
                        )
                    ),
                    (Scope) this     // enclosingScope
                ).compile();
            }
        }

        /**
         * Compiles the instance variable initializers and the instance initializers in their
         * lexical order.
         */
        private void initializeInstanceVariablesAndInvokeInstanceInitializers() throws CompileException {
            for (Iterator it = this.declaringClass.variableDeclaratorsAndInitializers.iterator(); it.hasNext();) {
                TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                if (!tbd.isStatic()) {
                    BlockStatement bs = (BlockStatement) tbd;
                    if (!bs.compile()) bs.compileError("Instance variable declarator or instance initializer does not complete normally");
                }
            }
        }

        public IClass.IConstructor toIConstructor() { return this.iConstructor; }

        protected IClass.IInvocable toIInvocable() { return this.toIConstructor(); }

        public String toString() {
            StringBuffer sb = new StringBuffer(this.declaringClass.getClassName());
            sb.append('(');
            FormalParameter[] fps = this.getFormalParameters();
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }
    
        public final void visit(Visitor visitor) { visitor.visitConstructorDeclarator(this); }
    }

    public final static class MethodDeclarator extends FunctionDeclarator {
        public MethodDeclarator(
            Location                      location,
            final AbstractTypeDeclaration declaringType,
            short                         modifiers,
            Type                          type,
            String                        name,
            FormalParameter[]             formalParameters,
            Type[]                        thrownExceptions
        ) {
            super(
                location,         // location
                declaringType,    // declaringType
                modifiers,        // modifiers
                type,             // type
                name,             // name
                formalParameters, // formalParameters
                thrownExceptions  // thrownExceptions
            );
            this.iMethod = declaringType.new IMethod() {

                // Implement IMember.
                public int getAccess() {
                    switch (MethodDeclarator.this.modifiers & Mod.PPP) {
                    case Mod.PRIVATE:
                        return IClass.PRIVATE;
                    case Mod.PROTECTED:
                        return IClass.PROTECTED;
                    case Mod.PACKAGE:
                        return IClass.PACKAGE;
                    case Mod.PUBLIC:
                        return IClass.PUBLIC;
                    default:
                        throw new RuntimeException("Invalid access");
                    }
                }

                // Implement IInvocable.
                public IClass[] getParameterTypes() throws CompileException {
                    FormalParameter[] fps = MethodDeclarator.this.getFormalParameters();
                    IClass[] res = new IClass[fps.length];
                    for (int i = 0; i < fps.length; ++i) {
                        res[i] = fps[i].type.getType();
                    }
                    return res;
                }
                public IClass[] getThrownExceptions() throws CompileException {
                    IClass[] res = new IClass[MethodDeclarator.this.thrownExceptions.length];
                    for (int i = 0; i < res.length; ++i) {
                        res[i] = MethodDeclarator.this.thrownExceptions[i].getType();
                    }
                    return res;
                }

                // Implement IMethod.
                public boolean isStatic() { return (MethodDeclarator.this.getModifiers() & Mod.STATIC) != 0; }
                public boolean isAbstract() { return declaringType.isInterface() || (MethodDeclarator.this.getModifiers() & Mod.ABSTRACT) != 0; }
                public IClass getReturnType() throws CompileException {
                    return MethodDeclarator.this.getReturnType();
                }
                public String getName() { return MethodDeclarator.this.getName(); }
            };
        }

        // Implement "FunctionDeclarator":
        protected void compilePreamble() {}

        public IClass.IMethod toIMethod() { return this.iMethod; }

        protected IClass.IInvocable toIInvocable() { return this.toIMethod(); }

        public String toString() {
            StringBuffer sb = new StringBuffer(this.getName());
            sb.append('(');
            FormalParameter[] fps = this.getFormalParameters();
            for (int i = 0; i < fps.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(fps[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        public final void visit(Visitor visitor) { visitor.visitMethodDeclarator(this); }

        private final IClass.IMethod iMethod;
    }

    /**
     * This class is derived from "Statement", because it provides for the
     * initialization of the field. In other words, "compile()" generates the
     * code that initializes the field.
     */
    public static final class FieldDeclarator extends Statement implements TypeBodyDeclaration {
        private final AbstractTypeDeclaration declaringType;
        final short                           modifiers;
        final Type                            type;
        VariableDeclarator[]                  variableDeclarators = null;

        public FieldDeclarator(
            Location                location,
            AbstractTypeDeclaration declaringType,
            short                   modifiers,
            Type                    type
        ) {
            super(
                location,
                (Scope) declaringType // enclosingScope
            );
            this.modifiers     = modifiers;
            this.declaringType = declaringType;
            this.type          = type;
        }
        public void setVariableDeclarators(VariableDeclarator[] variableDeclarators) {
            this.variableDeclarators = variableDeclarators;
        }

        public IClass.IField[] getIFields() {
            IClass.IField[] res = new IClass.IField[this.variableDeclarators.length];
            for (int i = 0; i < res.length; ++i) {
                final VariableDeclarator vd = this.variableDeclarators[i];
                res[i] = this.declaringType.new IField() {

                    // Implement IMember.
                    public int getAccess() {
                        switch (FieldDeclarator.this.modifiers & Mod.PPP) {
                        case Mod.PRIVATE:
                            return IClass.PRIVATE;
                        case Mod.PROTECTED:
                            return IClass.PROTECTED;
                        case Mod.PACKAGE:
                            return IClass.PACKAGE;
                        case Mod.PUBLIC:
                            return IClass.PUBLIC;
                        default:
                            throw new RuntimeException("Invalid access");
                        }
                    }

                    // Implement "IField".
                    public boolean isStatic() { return (FieldDeclarator.this.modifiers & Mod.STATIC) != 0; }
                    public IClass getType() throws CompileException {
                        IClass res2 = FieldDeclarator.this.type.getType();
                        return Java.getArrayType(res2, vd.brackets);
                    }
                    public String getName() { return vd.name; }
                    public Object getConstantValue() throws Java.CompileException {
                        if (
                            (FieldDeclarator.this.modifiers & Mod.FINAL) != 0 &&
                            vd.optionalInitializer != null
                        ) return vd.optionalInitializer.getConstantValue();
                        return null;
                    }
                };
            }
            return res;
        }

        // Compile time members:

        /**
         * Override {@link Java.Statement#generatesCode()}, because code is only generated if at
         * least one of the declared variables has a non-constant-final initializer.
         */
        public boolean generatesCode() throws CompileException {
            for (int i = 0; i < this.variableDeclarators.length; ++i) {
                VariableDeclarator vd = this.variableDeclarators[i];
                if (this.getNonConstantFinalInitializer(vd) != null) return true;
            }
            return false;
        }
        public boolean compile() throws CompileException {
            for (int i = 0; i < this.variableDeclarators.length; ++i) {
                VariableDeclarator vd = this.variableDeclarators[i];

                Rvalue initializer = this.getNonConstantFinalInitializer(vd);
                if (initializer == null) continue;

                if ((this.modifiers & Mod.STATIC) == 0) {
                    this.writeOpcode(Opcode.ALOAD_0);
                }
                IClass initializerType = initializer.compileGetValue();
                IClass fieldType = this.type.getType();
                fieldType = Java.getArrayType(fieldType, vd.brackets);
                Java.assignmentConversion(
                    (Located) this,                // located
                    initializerType,               // sourceType
                    fieldType,                     // destinationType
                    initializer.getConstantValue() // optionalConstantValue
                );
                if ((this.modifiers & Mod.STATIC) != 0) {
                    this.writeOpcode(Opcode.PUTSTATIC);
                } else {
                    this.writeOpcode(Opcode.PUTFIELD);
                }
                this.writeConstantFieldrefInfo(
                    this.declaringType.getDescriptor(), // classFD
                    vd.name,                            // fieldName
                    fieldType.getDescriptor()           // fieldFD
                );
            }
            return true;
        }

        /**
         * Determine the non-constant-final initializer of the given {@link VariableDeclarator}.
         * @return <code>null</code> if the variable is declared without an initializer or if the initializer is constant-final
         */
        private Rvalue getNonConstantFinalInitializer(VariableDeclarator vd) throws CompileException {

            // Check if optional initializer exists.
            if (vd.optionalInitializer == null) return null;

            // Check if initializer is constant-final.
            if (
                (this.modifiers & Mod.STATIC) != 0 &&
                (this.modifiers & Mod.FINAL) != 0 &&
                vd.optionalInitializer.getConstantValue() != null
            ) return null;

            return vd.optionalInitializer;
        }

        // Implement TypeBodyDeclaration.
        public TypeDeclaration getDeclaringType() {
            return this.declaringType;
        }
        public boolean isStatic() {
            return (this.modifiers & Mod.STATIC) != 0;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(Mod.shortToString(this.modifiers)).append(' ').append(this.type).append(' ').append(this.variableDeclarators[0]);
            for (int i = 1; i < this.variableDeclarators.length; ++i) {
                sb.append(", ").append(this.variableDeclarators[i]);
            }
            return sb.toString();
        }

        public final void visit(Visitor visitor) { visitor.visitFieldDeclarator(this); }
    }

    public final static class VariableDeclarator extends Located {
        final String name;
        final int    brackets;
        Rvalue       optionalInitializer = null;

        public VariableDeclarator(
            Location location,
            String   name,
            int      brackets,
            Rvalue   optionalInitializer
        ) {
            super(location);
            this.name                = name;
            this.brackets            = brackets;
            this.optionalInitializer = optionalInitializer;

            // Used both by field declarations an local variable declarations, so naming
            // conventions checking (JLS2 6.8) cannot be done here.
        }
        public String getName() { return this.name; }

        public final void visit(Visitor visitor) { visitor.visitVariableDeclarator(this); }
    }

    public static final class FormalParameter {
        public FormalParameter(
            boolean finaL,
            Type    type,
            String  name
        ) {
            this.finaL = finaL;
            this.type  = type;
            this.name  = name;
        }

        public String toString() {
            return this.type.toString() + ' ' + this.name;
        }

        public void visit(Visitor visitor) { visitor.visitFormalParameter(this); }

        final boolean finaL;
        final Type    type;
        final String  name;
    }

    /**
     * Base of all statements that can appear in a block.
     */
    public interface BlockStatement extends Locatable, Scope {

        /**
         * Check whether invocation of {@link #compile()} would
         * generate more than zero code bytes.
         */
        boolean generatesCode() throws CompileException;

        /**
         * @return <tt>false</tt> if this statement cannot complete normally (14.20)
         */
        boolean compile() throws CompileException;

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
         * {@link #leave(IClass)} that require a specific
         * operand stack state (e.g. an empty operand stack for JSR).
         */
        void leave(IClass optionalStackValueType);

        void visit(Visitor visitor);
    }

    public static abstract class Statement extends Located implements BlockStatement {
        protected final Scope enclosingScope;

        protected Statement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location);
            this.enclosingScope = enclosingScope;
        }

        // Implement "Scope".
        public Scope getEnclosingScope() { return this.enclosingScope; }

        // Implement "BlockStatement".
        public boolean generatesCode() throws CompileException { return true; }
        public abstract boolean compile() throws CompileException;
        public void leave(IClass optionalStackValueType) {
            ;
        }
    }

    public final static class LabeledStatement extends BreakableStatement {
        public LabeledStatement(
            Location location,
            Scope    enclosingScope,
            String   label
        ) {
            super(location, enclosingScope);
            this.label = label;
        }

        public void setBody(Statement body) {
            this.body = body;
        }

        /*final*/ String label;
        Statement    body = null;

        // Compile time members:

        public final boolean compile2() throws CompileException {
            return this.body.compile();
        }

        public final void visit(Visitor visitor) { visitor.visitLabeledStatement(this); }
    }

    /**
     * Representation of a Java<sup>TM</sup> "block" (JLS 14.2).
     * <p>
     * The statements that the block defines are executed in sequence.
     */
    public final static class Block extends Statement {
        final List statements = new ArrayList(); // BlockStatement

        public Block(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void addStatement(BlockStatement statement) {
            this.statements.add(statement);
        }
        public void addStatements(
            List statements // BlockStatement
        ) {
            this.statements.addAll(statements);
        }
        public BlockStatement[] getStatements() {
            return (BlockStatement[]) this.statements.toArray(new BlockStatement[this.statements.size()]);
        }


        // Compile time members.

        // Fills while the block is being compiled.
        private final Map declaredLocalClasses = new HashMap(); // String declaredName => LocalClassDeclaration

        public LocalClassDeclaration getLocalClassDeclaration(String name) {
            return (LocalClassDeclaration) this.declaredLocalClasses.get(name);
        }

        public boolean generatesCode() throws CompileException {
            for (int i = 0; i < this.statements.size(); ++i) {
                if (((BlockStatement) this.statements.get(i)).generatesCode()) return true;
            }
            return false;
        }

        public boolean compile() throws CompileException {
            this.saveLocalVariables();
            try {
                boolean previousStatementCanCompleteNormally = true;
                this.keepCompiling = true;
                for (int i = 0; this.keepCompiling && i < this.statements.size(); ++i) {
                    BlockStatement bs = (BlockStatement) this.statements.get(i);
                    if (!previousStatementCanCompleteNormally) {
                        bs.compileError("Statement is unreachable");
                        break;
                    } 
                    previousStatementCanCompleteNormally = bs.compile();
                }
                return previousStatementCanCompleteNormally;
            } finally {
                this.restoreLocalVariables();
            }
        }

        /**
         * Define a local variable in the context of this block
         * @return The index of the variable
         */
        public LocalVariable defineLocalVariable(
            Located  located,
            boolean finaL,
            IClass   type,
            String   name
        ) throws CompileException {

            // Check for local variable redefinition.
            for (Scope s = this; s instanceof Statement; s = s.getEnclosingScope()) {
                if (s instanceof Block) {
                    if (((Block) s).localVariables.containsKey(name)) located.compileError("Redefinition of local variable \"" + name + "\"");
                }
            }
            LocalVariable lv = new LocalVariable(
                finaL,                                                            // finaL
                type,                                                             // type
                this.allocateLocalVariable(Descriptor.size(type.getDescriptor())) // localVariableArrayIndex
            );
            this.localVariables.put(name, lv);
            return lv;
        }

        /**
         * JLS 14.10 specifies that under certain circumstances it is not an error that
         * statements are physically unreachable, precisely
         * <pre>
         *   {
         *     if (true) return;
         *     a = 1; // Physically unreachable, but not an error, i.e. "dead"
         *   }
         *   b = 2; // Same thing.
         * </pre>
         * The "if" statement notifies its enclosing block by calling this method.
         */
        /*private*/ void followingStatementsAreDead() {
            this.keepCompiling = false;
        }

        public final void visit(Visitor visitor) { visitor.visitBlock(this); }

        private HashMap localVariables = new HashMap(); // String name => LocalVariable
        private boolean keepCompiling;
    }

    /**
     * Base class for statements that can be terminated abnormally with a
     * "break" statement.
     * <p>
     * According to the JLS, statements that can be terminated abnormally with
     * a "break" statement are: "COntinuable" statements ("for", "do" and
     * "while"), labeled statements, and the "switch" statement.
     */
    public static abstract class BreakableStatement extends Statement {
        protected BreakableStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        // Implement "BlockStatement".
        public final boolean compile() throws CompileException {
            boolean canCompleteNormally = this.compile2();
            if (this.whereToBreak != null) {
                this.whereToBreak.set();
                canCompleteNormally = true;
            }
            return canCompleteNormally;
        }
        public abstract boolean compile2() throws CompileException;
        public CodeContext.Offset getWhereToBreak() {
            if (this.whereToBreak == null) {
                this.whereToBreak = this.newUnsetOffset();
            }
            return this.whereToBreak;
        }

        private CodeContext.Offset whereToBreak = null;
    }

    public static abstract class ContinuableStatement extends BreakableStatement {
        protected ContinuableStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        protected CodeContext.Offset whereToContinue = null;
        protected boolean              bodyHasContinue = false;
    }

    public final static class ExpressionStatement extends Statement {
        public ExpressionStatement(
            Rvalue rvalue,
            Scope  enclosingScope
        ) {
            super(rvalue.getLocation(), enclosingScope);
            this.rvalue = rvalue;
        }
        final Rvalue rvalue;

        // Compile time members:

        public boolean compile() throws CompileException {
            this.rvalue.compile();
            return true;
        }

        public final void visit(Visitor visitor) { visitor.visitExpressionStatement(this); }
    }

    public final static class LocalClassDeclarationStatement extends Statement {
        public LocalClassDeclarationStatement(
            Scope                      enclosingScope,
            Java.LocalClassDeclaration lcd
        ) {
            super(lcd.getLocation(), enclosingScope);
            this.lcd = lcd;
        }
        final LocalClassDeclaration lcd;

        public boolean compile() throws CompileException {
            this.lcd.compile();
            return true;
        }
        public boolean generatesCode() throws CompileException { return false; }

        public final void visit(Visitor visitor) { visitor.visitLocalClassDeclarationStatement(this); }
    }

    public final static class IfStatement extends Statement {

        /**
         * Notice that the <code>elseStatement</code> is mandatory; for an if statement without
         * an "else" clause, a dummy {@link Java.EmptyStatement} should be passed.
         */
        public IfStatement(
            Location       location,
            Scope          enclosingScope,
            Rvalue         condition,
            BlockStatement thenStatement,
            BlockStatement optionalElseStatement
        ) {
            super(location, enclosingScope);
            this.condition             = condition;
            this.thenStatement         = thenStatement;
            this.optionalElseStatement = optionalElseStatement;
        }

        final Rvalue         condition;
        final BlockStatement thenStatement;
        final BlockStatement optionalElseStatement;

        // Compile time members:

        public boolean compile() throws Java.CompileException {
            boolean tsccn, esccn;

            Object cv = this.condition.getConstantValue();
            BlockStatement es = this.optionalElseStatement != null ? this.optionalElseStatement : new Java.EmptyStatement(this.thenStatement.getLocation(), this.thenStatement.getEnclosingScope());
            if (cv instanceof Boolean) {

                // Constant condition.
                BlockStatement seeingStatement, blindStatement;
                if (((Boolean) cv).booleanValue()) {
                    seeingStatement = this.thenStatement;
                    blindStatement  = es;
                } else {
                    seeingStatement = es;
                    blindStatement  = this.thenStatement;
                }

                // Compile the seeing statement.
                boolean ssccn = seeingStatement.compile();
                if (ssccn) return true;

                // Hm... the "seeing statement" cannot complete normally. So, in order
                // to determine whether the IF statement ccn, we need to compile the
                // "blind statement" to a fake code attribute to find out whether
                // it can complete normally.
                boolean bsccn;
                CodeContext savedCodeContext = Java.replaceCodeContext(this.createDummyCodeContext());
                try {
                    bsccn = blindStatement.compile();
                } finally {
                    Java.replaceCodeContext(savedCodeContext);
                }

                // Neither the seeing nor the blind statement ccn.
                if (!bsccn) return false;

                // We have a very complicated case here: The blind statement can complete
                // normally, but the seeing statement can't. This makes the following
                // code physically unreachable, but JLS 14.20 says that this should not
                // be considered an error.
                // Calling "followingStatementsAreDead()" on the enclosing Block
                // keeps it from generating unreachable code.
                Scope s = this.getEnclosingScope();
                if (s instanceof Block) ((Block) s).followingStatementsAreDead();
                return false;
            }

            // Non-constant condition.
            if (this.thenStatement.generatesCode()) {
                if (es.generatesCode()) {

                    // if (expr) stmt else stmt
                    CodeContext.Offset eso = this.newUnsetOffset();
                    CodeContext.Offset end = this.newUnsetOffset();
                    this.condition.compileBoolean(eso, Java.Rvalue.JUMP_IF_FALSE);
                    tsccn = this.thenStatement.compile();
                    if (tsccn) this.writeBranch(Opcode.GOTO, end);
                    eso.set();
                    esccn = es.compile();
                    end.set();
                    return tsccn || esccn;
                } else {

                    // if (expr) stmt else ;
                    CodeContext.Offset end = this.newUnsetOffset();
                    this.condition.compileBoolean(end, Java.Rvalue.JUMP_IF_FALSE);
                    tsccn = this.thenStatement.compile();
                    end.set();
                    return true;
                }
            } else {
                if (es.generatesCode()) {

                    // if (expr) ; else stmt
                    CodeContext.Offset end = this.newUnsetOffset();
                    this.condition.compileBoolean(end, Java.Rvalue.JUMP_IF_TRUE);
                    esccn = es.compile();
                    end.set();
                    return true;
                } else {

                    // if (expr) ; else ;
                    IClass conditionType = this.condition.compileGetValue();
                    if (conditionType != IClass.BOOLEAN) this.compileError("Not a boolean expression");
                    Java.pop((Located) this, conditionType);
                    return true;
                }
            }
        }

        public final void visit(Visitor visitor) { visitor.visitIfStatement(this); }
    }

    public final static class ForStatement extends ContinuableStatement {
        public ForStatement(
            Location location,
            Scope    enclosingScope
        ) {

            // Trick: Insert a "Block" between the enclosing scope and the
            // "for" statement.
            super(location, new Block(location, enclosingScope));
            this.implicitBlock = (Block) this.enclosingScope;
            this.body = new Block(
                location,
                this      // enclosingScope
            );
        }

        public void set(
            BlockStatement optionalInit,
            Rvalue         optionalCondition,
            Rvalue[]       optionalUpdate,
            BlockStatement body
        ) {
            this.optionalInit      = optionalInit;
            this.optionalCondition = optionalCondition;
            this.optionalUpdate    = optionalUpdate;
            this.body              = body;
        }

        public final Block     implicitBlock;
        BlockStatement optionalInit      = null;
        Rvalue         optionalCondition = null;
        Rvalue[]       optionalUpdate    = null;
        BlockStatement body;

        // Compile time members:

        public boolean compile2() throws CompileException {
            this.whereToContinue = this.newUnsetOffset();
            this.bodyHasContinue = false;

            boolean constantTrueCondition = false;

            this.saveLocalVariables();
            try {
                if (this.optionalInit != null) this.optionalInit.compile();

                // Check for constant condition.
                if (this.optionalCondition == null) {
                    constantTrueCondition = true;
                } else {
                    Object cv = this.optionalCondition.getConstantValue();
                    if (cv instanceof Boolean) {
                        if (!((Boolean) cv).booleanValue()) this.compileError("Body of \"for\" statement is unreachable");
                        constantTrueCondition = true;
                    }
                }

                CodeContext.Offset toCondition = this.newUnsetOffset();
                if (constantTrueCondition) {
                    ; // GOTO condition not necessary in this case.
                } else {
                    this.writeBranch(Opcode.GOTO, toCondition);
                }

                CodeContext.Offset bodyOffset = this.newOffset();
                if (
                    !this.body.compile() &&
                    !this.bodyHasContinue &&
                    this.optionalUpdate != null
                ) this.compileError("For update is unreachable");

                // Compile update.
                this.whereToContinue.set();
                if (this.optionalUpdate != null) {
                    for (int i = 0; i < this.optionalUpdate.length; ++i) {
                        this.optionalUpdate[i].compile();
                    }
                } 

                // Compile condition.
                toCondition.set();
                if (constantTrueCondition) {
                    this.writeBranch(Opcode.GOTO, bodyOffset);
                } else {
                    this.optionalCondition.compileBoolean(bodyOffset, Rvalue.JUMP_IF_TRUE);
                }
            } finally {
                this.restoreLocalVariables();
            }

            return !constantTrueCondition;
        }

        public final void visit(Visitor visitor) { visitor.visitForStatement(this); }
    }

    public final static class WhileStatement extends ContinuableStatement {
        public WhileStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   condition
        ) {
            super(location, enclosingScope);
            this.condition = condition;
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }

        final Rvalue   condition;
        BlockStatement body = null;

        // Compile time members:

        public boolean compile2() throws CompileException {
            this.whereToContinue = this.newUnsetOffset();
            this.bodyHasContinue = false;

            boolean constantTrueCondition = false;
            {
                Object cv = this.condition.getConstantValue();
                if (cv != null && cv instanceof Boolean) {
                    if (!((Boolean) cv).booleanValue()) this.compileError("Body of \"while\" statement is unreachable");
                    constantTrueCondition = true;
                }
            }

            if (!constantTrueCondition) {
                this.writeBranch(Opcode.GOTO, this.whereToContinue);
            }

            // Compile body.
            CodeContext.Offset bodyOffset = this.newOffset();
            boolean bodyCCN = this.body.compile();

            // Compile condition.
            if (!constantTrueCondition || bodyCCN || this.bodyHasContinue) {
                this.whereToContinue.set();
                this.condition.compileBoolean(bodyOffset, Rvalue.JUMP_IF_TRUE);
            }

            return !constantTrueCondition;
        }

        public final void visit(Visitor visitor) { visitor.visitWhileStatement(this); }
    }

    public final static class TryStatement extends Statement {
        BlockStatement body            = null;
        final List     catchClauses    = new ArrayList(); // CatchClause
        Block          optionalFinally = null;

        public TryStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }
        public void addCatchClause(CatchClause catchClause) {
            this.catchClauses.add(catchClause);
        }
        public List getCatchClauses() {
            return this.catchClauses;
        }
        public void setFinally(Block finallY) {
            this.optionalFinally = finallY;
        }
        public Block getFinally() {
            return this.optionalFinally;
        }

        // Compile time members:

        public boolean compile() throws CompileException {
            if (this.optionalFinally != null) this.finallyOffset = this.newUnsetOffset();

            CodeContext.Offset beginningOfBody = this.newOffset();
            CodeContext.Offset afterStatement = this.newUnsetOffset();

            // Allocate a future LV that will be used to preserve the "leave stack".
            this.saveLocalVariables();
            this.stackValueLvIndex = this.allocateLocalVariable((short) 2);
            this.restoreLocalVariables();

            boolean canCompleteNormally = this.body.compile();
            CodeContext.Offset afterBody = this.newOffset();
            if (canCompleteNormally) {
                this.writeBranch(Opcode.GOTO, afterStatement);
            }

            this.saveLocalVariables();
            try {

                // Local variable for exception object.
                // Notice: Must be same size as "this.stackValueLvIndex".
                short exceptionObjectLvIndex = this.allocateLocalVariable((short) 2);

                for (int i = 0; i < this.catchClauses.size(); ++i) {
                    CatchClause cc = (CatchClause) this.catchClauses.get(i);
                    IClass caughtExceptionType = cc.caughtException.type.getType();
                    Java.getCodeContext().addExceptionTableEntry(
                        beginningOfBody,                    // startPC
                        afterBody,                          // endPC
                        this.newOffset(),                   // handlerPC
                        caughtExceptionType.getDescriptor() // catchTypeFD
                    );
                    Java.store(
                        (Located) this,        // located
                        caughtExceptionType,   // lvType
                        exceptionObjectLvIndex // lvIndex
                    );

                    // Kludge: Treat the exception variable like a local
                    // variable of the catch clause body.
                    cc.body.localVariables.put(
                        cc.caughtException.name,
                        new LocalVariable(
                            false,                 // finaL
                            caughtExceptionType,   // type
                            exceptionObjectLvIndex // localVariableIndex
                        )
                    );

                    if (cc.body.compile()) {
                        canCompleteNormally = true;
                        if (
                            i < this.catchClauses.size() - 1 ||
                            this.optionalFinally != null
                        ) this.writeBranch(Opcode.GOTO, afterStatement);
                    }
                }
                if (this.optionalFinally != null) {
                    CodeContext.Offset here = this.newOffset();
                    Java.getCodeContext().addExceptionTableEntry(
                        beginningOfBody, // startPC
                        here,            // endPC
                        here,            // handlerPC
                        null             // catchTypeFD
                    );

                    // Store exception object in local variable.
                    Java.store(
                        (Located) this,                // located
                        Java.getIClassLoader().OBJECT, // valueType
                        exceptionObjectLvIndex         // localVariableIndex
                    );
                    this.writeBranch(Opcode.JSR, this.finallyOffset);
                    Java.load(
                        (Located) this,                // located
                        Java.getIClassLoader().OBJECT, // valueType
                        exceptionObjectLvIndex         // localVariableIndex
                    );
                    this.writeOpcode(Opcode.ATHROW);

                    // Compile the "finally" body.
                    this.finallyOffset.set();
                    short pcLVIndex = this.allocateLocalVariable((short) 1);
                    Java.store(
                        (Located) this,                // located
                        Java.getIClassLoader().OBJECT, // valueType
                        pcLVIndex                      // localVariableIndex
                    );
                    if (this.optionalFinally.compile()) {
                        this.writeOpcode(Opcode.RET);
                        this.writeByte(pcLVIndex);
                    }
                }

                afterStatement.set();
                if (canCompleteNormally) this.leave(null);
            } finally {
                this.restoreLocalVariables();
            }

            return canCompleteNormally;
        }

        public void leave(IClass optionalStackValueType) {
            if (this.finallyOffset != null) {

                // Obviously, JSR must always be executed with the operand stack being
                // empty; otherwise we get "java.lang.VerifyError: Inconsistent stack height
                // 1 != 2"
                if (optionalStackValueType != null) {
                    Java.store((Located) this, optionalStackValueType, this.stackValueLvIndex);
                }

                this.writeBranch(Opcode.JSR, this.finallyOffset);

                if (optionalStackValueType != null) {
                    Java.load((Located) this, optionalStackValueType, this.stackValueLvIndex);
                }
            }
        }

        public final void visit(Visitor visitor) { visitor.visitTryStatement(this); }

        // Preserves "leave stack".
        private short stackValueLvIndex;

        private CodeContext.Offset finallyOffset = null;
    }

    public static class CatchClause {
        public CatchClause(
            FormalParameter caughtException,
            Block           body
        ) {
            this.caughtException = caughtException;
            this.body            = body;
        }

        final FormalParameter caughtException;
        final Block           body;
    }

    /**
     * 14.10 The "switch" Statement
     */
    public static final class SwitchStatement extends BreakableStatement {
        public SwitchStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setCondition(Rvalue condition) {
            this.condition = condition;
        }
        public void addSwitchBlockStatementGroup(SwitchBlockStatementGroup sbsg) {
            this.sbsgs.add(sbsg);
        }

        Rvalue     condition = null;
        final List sbsgs = new ArrayList(); // SwitchBlockStatementGroup

        // Compile time members:

        public boolean compile2() throws CompileException {

            // Compute condition.
            IClass switchExpressionType = this.condition.compileGetValue();
            Java.assignmentConversion(
                (Located) this,       // located
                switchExpressionType, // sourceType
                IClass.INT,           // targetType
                null                  // optionalConstantValue
            );

            // Prepare the map of case labels to code offsets.
            TreeMap                caseLabelMap = new TreeMap(); // Integer => Offset
            CodeContext.Offset   defaultLabelOffset = null;
            CodeContext.Offset[] sbsgOffsets = new CodeContext.Offset[this.sbsgs.size()];
            for (int i = 0; i < this.sbsgs.size(); ++i) {
                SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) this.sbsgs.get(i);
                sbsgOffsets[i] = this.newUnsetOffset();
                for (int j = 0; j < sbsg.caseLabels.size(); ++j) {

                    // Verify that case label value is a constant.
                    Rvalue rv = (Rvalue) sbsg.caseLabels.get(j);
                    Object cv = rv.getConstantValue();
                    if (cv == null) {
                        rv.compileError("Value of \"case\" label does not pose a constant value");
                        cv = new Integer(99);
                    } 

                    // Verify that case label is assignable to the type of the switch expression.
                    IClass rvType = rv.getType();
                    Java.assignmentConversion(
                        (Located) this,       // located
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
                    if (caseLabelMap.containsKey(civ)) rv.compileError("Duplicate \"case\" switch label value");
                    caseLabelMap.put(civ, sbsgOffsets[i]);
                }
                if (sbsg.hasDefaultLabel) {
                    if (defaultLabelOffset != null) sbsg.compileError("Duplicate \"default\" switch label");
                    defaultLabelOffset = sbsgOffsets[i];
                }
            }
            if (defaultLabelOffset == null) defaultLabelOffset = this.getWhereToBreak();

            // Generate TABLESWITCH or LOOKUPSWITCH instruction.
            CodeContext.Offset switchOffset = this.newOffset();
            if (caseLabelMap.isEmpty() ||
            2 * caseLabelMap.size() >= (
                ((Integer) caseLabelMap.lastKey()).intValue() -
                ((Integer) caseLabelMap.firstKey()).intValue()
            )) {
                int low = ((Integer) caseLabelMap.firstKey()).intValue();
                int high = ((Integer) caseLabelMap.lastKey()).intValue();

                this.writeOpcode(Opcode.TABLESWITCH);
                new Padder(Java.getCodeContext()).set();
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
                this.writeOpcode(Opcode.LOOKUPSWITCH);
                new Padder(Java.getCodeContext()).set();
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
            for (int i = 0; i < this.sbsgs.size(); ++i) {
                SwitchBlockStatementGroup sbsg = (SwitchBlockStatementGroup) this.sbsgs.get(i);
                sbsgOffsets[i].set();
                canCompleteNormally = true;
                for (int j = 0; j < sbsg.blockStatements.size(); ++j) {
                    BlockStatement bs = (BlockStatement) sbsg.blockStatements.get(j);
                    if (!canCompleteNormally) {
                        bs.compileError("Statement is unreachable");
                        break;
                    } 
                    canCompleteNormally = bs.compile();
                }
            }
            return canCompleteNormally;
        }

        public final void visit(Visitor visitor) { visitor.visitSwitchStatement(this); }
    }
    private static class Padder extends CodeContext.Inserter implements CodeContext.FixUp {
        public Padder(CodeContext codeContext) {
            codeContext.super();
        }
        public void fixUp() {
            int x = this.offset % 4;
            if (x != 0) {
                CodeContext ca = this.getCodeContext();
                ca.pushInserter(this); {
                    ca.write((short) -1, new byte[4 - x]);
                } ca.popInserter();
            }
        }
    }

    public static class SwitchBlockStatementGroup extends Located {
        public SwitchBlockStatementGroup(Location location) {
            super(location);
        }

        public void addSwitchLabel(Rvalue value) {
            this.caseLabels.add(value);
        }
        public void addDefaultSwitchLabel() throws Parser.ParseException {
            if (this.hasDefaultLabel) this.throwParseException("Duplicate \"default\" switch label");
            this.hasDefaultLabel = true;
        }
        public void setBlockStatements(List blockStatements) {
            this.blockStatements = blockStatements;
        }

        final List caseLabels = new ArrayList(); // Rvalue
        boolean    hasDefaultLabel = false;
        List       blockStatements; // BlockStatement
    }

    public final static class SynchronizedStatement extends Statement {
        public SynchronizedStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   expression
        ) {
            super(location, enclosingScope);
            this.expression = expression;
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }

        final Rvalue   expression;
        BlockStatement body         = null;

        // Compile time members:

        public boolean compile() throws CompileException {

            // Evaluate monitor object expression.
            if (!Java.getIClassLoader().OBJECT.isAssignableFrom(this.expression.compileGetValue())) this.compileError("Monitor object of \"synchronized\" statement is not a subclass of \"Object\"");

            this.saveLocalVariables();
            boolean canCompleteNormally = false;
            try {

                // Allocate a local variable for the monitor object.
                this.monitorLvIndex = this.allocateLocalVariable((short) 1);

                // Store the monitor object.
                this.writeOpcode(Opcode.DUP);
                Java.store((Located) this, Java.getIClassLoader().OBJECT, this.monitorLvIndex);

                // Create lock on the monitor object.
                this.writeOpcode(Opcode.MONITORENTER);

                // Compile the statement body.
                CodeContext.Offset monitorExitOffset = this.newUnsetOffset();
                CodeContext.Offset beginningOfBody = this.newOffset();
                canCompleteNormally = this.body.compile();
                if (canCompleteNormally) {
                    this.writeBranch(Opcode.GOTO, monitorExitOffset);
                }

                // Generate the exception handler.
                CodeContext.Offset here = this.newOffset();
                Java.getCodeContext().addExceptionTableEntry(
                    beginningOfBody, // startPC
                    here,            // endPC
                    here,            // handlerPC
                    null             // catchTypeFD
                );
                this.leave(Java.getIClassLoader().THROWABLE);
                this.writeOpcode(Opcode.ATHROW);

                // Unlock monitor object.
                if (canCompleteNormally) {
                    monitorExitOffset.set();
                    this.leave(null);
                }
            } finally {
                this.restoreLocalVariables();
            }

            return canCompleteNormally;
        }

        public void leave(IClass optionalStackValueType) {
            Java.load((Located) this, Java.getIClassLoader().OBJECT, this.monitorLvIndex);
            this.writeOpcode(Opcode.MONITOREXIT);
        }

        public final void visit(Visitor visitor) { visitor.visitSynchronizedStatement(this); }

        private short monitorLvIndex = -1;
    }

    public final static class DoStatement extends ContinuableStatement {
        public DoStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public void setBody(BlockStatement body) {
            this.body = body;
        }
        public void setCondition(Rvalue condition) {
            this.condition = condition;
        }

        BlockStatement body      = null;
        Rvalue         condition = null;

        // Compile time members:

        public boolean compile2() throws CompileException {
            this.whereToContinue = this.newUnsetOffset();
            this.bodyHasContinue = false;

            CodeContext.Offset bodyOffset = this.newOffset();

            // Compile body.
            if (!this.body.compile() && !this.bodyHasContinue) this.compileError("\"do\" statement never tests its condition");

            // Compile condition.
            this.whereToContinue.set();
            this.condition.compileBoolean(bodyOffset, Rvalue.JUMP_IF_TRUE);

            boolean constantTrueCondition = false;
            {
                Object cv = this.condition.getConstantValue();
                if (cv != null && cv instanceof Boolean) {
                    constantTrueCondition = ((Boolean) cv).booleanValue();
                }
            }

            return !constantTrueCondition;
        }

        public final void visit(Visitor visitor) { visitor.visitDoStatement(this); }
    }

    public final static class LocalVariableDeclarationStatement extends Statement {

        /**
         * @param modifiers Only "final" allowed.
         */
        public LocalVariableDeclarationStatement(
            Location             location,
            Block                declaringBlock,
            short                modifiers,
            Type                 type,
            VariableDeclarator[] variableDeclarators
        ) {
            super(location, declaringBlock);
            this.declaringBlock      = declaringBlock;
            this.modifiers           = modifiers;
            this.type                = type;
            this.variableDeclarators = variableDeclarators;
        }

        private final Block        declaringBlock;
        final short                modifiers;
        final Type                 type;
        final VariableDeclarator[] variableDeclarators;

        // Compile time members:

        public boolean compile() throws CompileException {
            if ((this.modifiers & ~Mod.FINAL) != 0) this.compileError("The only allowed modifier in local variable declarations is \"final\"");

            for (int j = 0; j < this.variableDeclarators.length; ++j) {
                VariableDeclarator vd = this.variableDeclarators[j];

                // Determine variable type.
                Type variableType = this.type;
                for (int k = 0; k < vd.brackets; ++k) variableType = new ArrayType(variableType);

                IClass lhsType = variableType.getType();
                LocalVariable lv = this.declaringBlock.defineLocalVariable(
                    (Located) this,                    // located
                    (this.modifiers & Mod.FINAL) != 0, // finaL
                    lhsType,                           // type
                    vd.name                            // name
                );

                if (vd.optionalInitializer != null) {
                    Rvalue rhs = vd.optionalInitializer;
                    Java.assignmentConversion(
                        (Located) this,        // located
                        rhs.compileGetValue(), // sourceType
                        lhsType,               // targetType
                        rhs.getConstantValue() // optionalConstantValue
                    );
                    Java.store(
                        (Located) this, // located
                        lhsType,        // valueType
                        lv              // localVariable
                    );
                }
            }
            return true;
        }

        public final void visit(Visitor visitor) { visitor.visitLocalVariableDeclarationStatement(this); }
    }

    public final static class ReturnStatement extends Statement {
        public ReturnStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   optionalReturnValue
        ) {
            super(location, enclosingScope);
            this.optionalReturnValue = optionalReturnValue;
        }

        final Rvalue optionalReturnValue;

        // Compile time members:

        public boolean compile() throws CompileException {

            // Determine enclosing block, function and compilation Unit.
            FunctionDeclarator enclosingFunction = null;
            {
                Scope s = this.enclosingScope;
                for (s = s.getEnclosingScope(); s instanceof Statement; s = s.getEnclosingScope());
                enclosingFunction = (FunctionDeclarator) s;
            }

            IClass returnType = enclosingFunction.getReturnType();
            if (returnType == IClass.VOID) {
                if (this.optionalReturnValue != null) this.compileError("Method must not return a value");
                Java.leaveStatements(
                    this.enclosingScope, // from
                    enclosingFunction,   // to
                    null                 // optionalStackValueType
                );
                this.writeOpcode(Opcode.RETURN);
                return false;
            }
            if (this.optionalReturnValue == null) {
                this.compileError("Method must return a value");
                return false;
            } 
            IClass type = this.optionalReturnValue.compileGetValue();
            Java.assignmentConversion(
                (Located) this,                     // located
                type,                               // sourceType
                returnType,                         // targetType
                this.optionalReturnValue.getConstantValue() // optionalConstantValue
            );

            Java.leaveStatements(
                this.enclosingScope, // from
                enclosingFunction,   // to
                returnType           // optionalStackValueType
            );
            this.writeOpcode(Opcode.IRETURN + Java.ilfda(returnType));
            return false;
        }

        public final void visit(Visitor visitor) { visitor.visitReturnStatement(this); }
    }

    public final static class ThrowStatement extends Statement {
        public ThrowStatement(
            Location location,
            Scope    enclosingScope,
            Rvalue   expression
        ) {
            super(location, enclosingScope);
            this.expression = expression;
        }

        final Rvalue expression;

        // Compile time members:

        public boolean compile() throws CompileException {
            IClass expressionType = this.expression.compileGetValue();
            Java.checkThrownException(
                (Located) this,     // located
                expressionType,     // type
                this.enclosingScope // scope
            );
            this.writeOpcode(Opcode.ATHROW);
            return false;
        }

        public final void visit(Visitor visitor) { visitor.visitThrowStatement(this); }
    }

    private static void checkThrownException(
        Located located,
        IClass  type,
        Scope   scope
    ) throws CompileException {

        // Thrown object must be assignable to "Throwable".
        if (!Java.getIClassLoader().THROWABLE.isAssignableFrom(type)) located.compileError("Thrown object of type \"" + type + "\" is not assignable to \"Throwable\"");

        // "RuntimeException" and "Error" are never checked.
        if (
            Java.getIClassLoader().RUNTIME_EXCEPTION.isAssignableFrom(type) ||
            Java.getIClassLoader().ERROR.isAssignableFrom(type)
        ) return;

        // Match against enclosing "try...catch" blocks.
        while (scope instanceof Statement) {
            if (scope instanceof TryStatement) {
                TryStatement ts = (TryStatement) scope;
                for (int i = 0; i < ts.catchClauses.size(); ++i) {
                    CatchClause cc = (CatchClause) ts.catchClauses.get(i);
                    IClass caughtType = cc.caughtException.type.getType();
                    if (caughtType.isAssignableFrom(type)) return;
                }
            }
            scope = scope.getEnclosingScope();
        }

        // Match against "throws" clause of declaring function.
        if (scope instanceof FunctionDeclarator) {
            FunctionDeclarator fd = (FunctionDeclarator) scope;
            for (int i = 0; i < fd.thrownExceptions.length; ++i) {
                IClass te = fd.thrownExceptions[i].getType();
                if (te.isAssignableFrom(type)) return;
            }
        }

        located.compileError("Thrown exception of type \"" + type + "\" is neither caught by a \"try...catch\" block nor declared in the \"throws\" clause of the declaring function");
    }

    /**
     * Representation of the Java<sup>TM</sup> "break" statement (JLS 14.14).
     */
    public final static class BreakStatement extends Statement {
        public BreakStatement(
            Location location,
            Scope    enclosingScope,
            String   optionalLabel
        ) {
            super(location, enclosingScope);
            this.optionalLabel = optionalLabel;
        }

        final String optionalLabel;

        // Compile time members:

        public boolean compile() throws CompileException {

            // Find the broken statement.
            BreakableStatement brokenStatement = null;
            if (this.optionalLabel == null) {
                for (
                    Scope s = this.enclosingScope;
                    s instanceof Statement;
                    s = s.getEnclosingScope()
                ) {
                    if (s instanceof BreakableStatement) {
                        brokenStatement = (BreakableStatement) s;
                        break;
                    }
                }
                if (brokenStatement == null) {
                    this.compileError("\"break\" statement is not enclosed by a breakable statement");
                    return false;
                } 
            } else {
                for (
                    Scope s = this.enclosingScope;
                    s instanceof Statement;
                    s = s.getEnclosingScope()
                ) {
                    if (s instanceof LabeledStatement) {
                        LabeledStatement ls = (LabeledStatement) s;
                        if (ls.label.equals(this.optionalLabel)) {
                            brokenStatement = ls;
                            break;
                        }
                    }
                }
                if (brokenStatement == null) {
                    this.compileError("Statement \"break " + this.optionalLabel + "\" is not enclosed by a breakable statement with label \"" + this.optionalLabel + "\"");
                    return false;
                } 
            }

            Java.leaveStatements(
                this.enclosingScope,            // from
                brokenStatement.enclosingScope, // to
                null                            // optionalStackValueType
            );
            this.writeBranch(Opcode.GOTO, brokenStatement.getWhereToBreak());
            return false;
        }

        public final void visit(Visitor visitor) { visitor.visitBreakStatement(this); }
    }

    /**
     * Representation of the Java<sup>TM</sup> "continue" statement (JLS
     * 14.15).
     */
    public final static class ContinueStatement extends Statement {
        public ContinueStatement(
            Location location,
            Scope    enclosingScope,
            String   optionalLabel
        ) {
            super(location, enclosingScope);
            this.optionalLabel = optionalLabel;
        }

        final String optionalLabel;

        // Compile time members:

        public boolean compile() throws CompileException {

            // Find the continued statement.
            ContinuableStatement continuedStatement = null;
            if (this.optionalLabel == null) {
                for (
                    Scope s = this.enclosingScope;
                    s instanceof Statement;
                    s = s.getEnclosingScope()
                ) {
                    if (s instanceof ContinuableStatement) {
                        continuedStatement = (ContinuableStatement) s;
                        break;
                    }
                }
                if (continuedStatement == null) {
                    this.compileError("\"continue\" statement is not enclosed by a continuable statement");
                    return false;
                } 
            } else {
                for (
                    Scope s = this.enclosingScope;
                    s instanceof Statement;
                    s = s.getEnclosingScope()
                ) {
                    if (s instanceof LabeledStatement) {
                        LabeledStatement ls = (LabeledStatement) s;
                        if (ls.label.equals(this.optionalLabel)) {
                            Statement st = ls.body;
                            while (st instanceof LabeledStatement) st = ((LabeledStatement) st).body;
                            if (!(st instanceof ContinuableStatement)) {
                                st.compileError("Labeled statement is not continuable");
                                return false;
                            } 
                            continuedStatement = (ContinuableStatement) st;
                            break;
                        }
                    }
                }
                if (continuedStatement == null) {
                    this.compileError("Statement \"continue " + this.optionalLabel + "\" is not enclosed by a continuable statement with label \"" + this.optionalLabel + "\"");
                    return false;
                } 
            }

            continuedStatement.bodyHasContinue = true;
            Java.leaveStatements(
                this.enclosingScope, // from
                continuedStatement.enclosingScope,   // to
                null                 // optionalStackValueType
            );
            this.writeBranch(Opcode.GOTO, continuedStatement.whereToContinue);
            return false;
        }

        public final void visit(Visitor visitor) { visitor.visitContinueStatement(this); }
    }

    /**
     * Represents the "empty statement", i.e. the blank semicolon.
     */
    public final static class EmptyStatement extends Statement {
        public EmptyStatement(
            Location location,
            Scope    enclosingScope
        ) {
            super(location, enclosingScope);
        }

        public boolean generatesCode() {
            return false;
        }

        public boolean compile() {
            return true;
        }

        public final void visit(Visitor visitor) { visitor.visitEmptyStatement(this); }
    }

    /**
     * Statements that jump out of blocks ("return", "break", "continue")
     * must call this method to make sure that the "finally" clauses of all
     * "try...catch" statements are executed.
     */
    private static void leaveStatements(
        Scope  from,
        Scope  to,
        IClass optionalStackValueType
    ) {
        for (Scope s = from; s != to; s = s.getEnclosingScope()) {
            if (s instanceof BlockStatement) {
                ((BlockStatement) s).leave(optionalStackValueType);
            } 
        }
    }

    /**
     * Abstract base class for {@link Java.Type}, {@link Java.Rvalue} and
     * {@link Java.Lvalue}.
     */
    public static abstract class Atom extends Located {
        public Atom(Location location) {
            super(location);
        }

        public Type   toType()   { return null; }
        public Rvalue toRvalue() { return null; }
        public Lvalue toLvalue() { return null; }

        public abstract String toString();

        // Parse time members:

        public final Type toTypeOrPE() throws Parser.ParseException {
            Type result = this.toType();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not a type");
            return result;
        }
        public final Rvalue toRvalueOrPE() throws Parser.ParseException {
            Rvalue result = this.toRvalue();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not an rvalue");
            return result;
        }
        public final Lvalue toLvalueOrPE() throws Parser.ParseException {
            Lvalue result = this.toLvalue();
            if (result == null) this.throwParseException("Expression \"" + this.toString() + "\" is not an lvalue");
            return result;
        }

        // Compile time members:

        public final Type toTypeOrCE() throws CompileException {
            Type result = this.toType();
            if (result == null) {
                this.compileError("Expression \"" + this.toString() + "\" is not a type");
                return new Type(this.getLocation()) {
                    public IClass getType() { return Java.getIClassLoader().OBJECT; }
                    public String toString() { return Atom.this.toString(); }
                    public void visit(Visitor visitor) {}
                };
            } 
            return result;
        }
        public final Rvalue toRvalueOrCE() throws CompileException {
            Rvalue result = this.toRvalue();
            if (result == null) {
                this.compileError("Expression \"" + this.toString() + "\" is not an rvalue");
                return new Rvalue(this.getLocation()) {
                    public IClass compileGet() { return Java.getIClassLoader().OBJECT; }
                    public IClass getType() { return Java.getIClassLoader().OBJECT; }
                    public String toString() { return Atom.this.toString(); }
                    public void visit(Visitor visitor) {}
                };
            } 
            return result;
        }
        public final Lvalue toLvalueOrCE() throws CompileException {
            Lvalue result = this.toLvalue();
            if (result == null) {
                this.compileError("Expression \"" + this.toString() + "\" is not an lvalue");
                return new Lvalue(this.getLocation()) {
                    public IClass compileGet() { return Java.getIClassLoader().OBJECT; }
                    public IClass getType() { return Java.getIClassLoader().OBJECT; }
                    public void   compileSet() {}
                    public String toString() { return Atom.this.toString(); }
                    public void visit(Visitor visitor) {}
                };
            } 
            return result;
        }

        public abstract IClass getType() throws CompileException;
        public boolean isType() throws CompileException {
            return this instanceof Type;
        }

        public abstract void visit(Visitor visitor);
    }

    /**
     * Representation of a Java<sup>TM</sup> type.
     */
    public static abstract class Type extends Atom {
        protected Type(Location location) {
            super(location);
        }
        public Type toType() { return this; }
    }

    public static final class SimpleType extends Type {
        private final IClass iClass;

        public SimpleType(Location location, IClass iClass) {
            super(location);
            this.iClass = iClass;
        }
        public IClass getType() { return this.iClass; }
        public String toString() { return this.iClass.toString(); }

        public final void visit(Visitor visitor) { visitor.visitSimpleType(this); }
    };

    /**
     * Representation of a Java<sup>TM</sup> "basic type" (obviously
     * equaivalent to a "primitive type") (JLS 4.2).
     */
    public static final class BasicType extends Type {
        public BasicType(Location location, int index) {
            super(location);
            this.index = index;
        }

        public IClass getType() {
            switch (this.index) {
                case BasicType.VOID:    return IClass.VOID;
                case BasicType.BYTE:    return IClass.BYTE;
                case BasicType.SHORT:   return IClass.SHORT;
                case BasicType.CHAR:    return IClass.CHAR;
                case BasicType.INT:     return IClass.INT;
                case BasicType.LONG:    return IClass.LONG;
                case BasicType.FLOAT:   return IClass.FLOAT;
                case BasicType.DOUBLE:  return IClass.DOUBLE;
                case BasicType.BOOLEAN: return IClass.BOOLEAN;
                default: throw new RuntimeException("Invalid index " + this.index);
            }
        }
        public String toString() {
            switch (this.index) {
                case BasicType.VOID:    return "void";
                case BasicType.BYTE:    return "byte";
                case BasicType.SHORT:   return "short";
                case BasicType.CHAR:    return "char";
                case BasicType.INT:     return "int";
                case BasicType.LONG:    return "long";
                case BasicType.FLOAT:   return "float";
                case BasicType.DOUBLE:  return "double";
                case BasicType.BOOLEAN: return "boolean";
                default: throw new RuntimeException("Invalid index " + this.index);
            }
        }

        public final void visit(Visitor visitor) { visitor.visitBasicType(this); }

        public static final int VOID    = 0;
        public static final int BYTE    = 1;
        public static final int SHORT   = 2;
        public static final int CHAR    = 3;
        public static final int INT     = 4;
        public static final int LONG    = 5;
        public static final int FLOAT   = 6;
        public static final int DOUBLE  = 7;
        public static final int BOOLEAN = 8;

        private final int index;
    }

    public static final class ReferenceType extends Type {
        public ReferenceType(
            Location location,
            Scope    scope,
            String[] identifiers
        ) {
            super(location);
            this.scope       = scope;
            this.identifiers = identifiers;
        }

        public IClass getType() throws CompileException {
            TypeDeclaration scopeTypeDeclaration = null;
            CompilationUnit scopeCompilationUnit;
            {
                Scope s = this.scope;

                // Determine scope statement.
                if (s instanceof Statement) {
                    for (s = s.getEnclosingScope(); s instanceof Statement; s = s.getEnclosingScope());
                }

                if (s instanceof FunctionDeclarator) {
                    s = s.getEnclosingScope();
                }

                // Determine scope class or interface.
                if (s instanceof TypeDeclaration) {
                    scopeTypeDeclaration = (TypeDeclaration) s;
                }

                // Determine scope compilationUnit.
                if (s != null) while (!(s instanceof CompilationUnit)) s = s.getEnclosingScope();
                scopeCompilationUnit = (CompilationUnit) s;
            }

            if (this.identifiers.length == 1) {

                // 6.5.5.1 Simple type name (single identifier).
                String simpleTypeName = this.identifiers[0];

                // 6.5.5.1.1 Local class.
                for (Scope s = this.scope; s != null; s = s.getEnclosingScope()) {
                    if (s instanceof Block) {
                        LocalClassDeclaration lcd = ((Block) s).getLocalClassDeclaration(simpleTypeName);
                        if (lcd != null) return lcd;
                    }
                }

                // 6.5.5.1.2 Member type.
                for (Scope s = scopeTypeDeclaration; s != null; s = s.getEnclosingScope()) {
                    if (s instanceof TypeDeclaration) {
                        IClass mt = Java.findMemberType((IClass) s, simpleTypeName, this.getLocation());
                        if (mt != null) return mt;
                    }
                }

                if (scopeCompilationUnit != null) {

                    // 6.5.5.1.4a Single-type import.
                    {
                        IClass importedClass = scopeCompilationUnit.importSingleType(simpleTypeName, this.getLocation());
                        if (importedClass != null) return importedClass;
                    }

                    // 6.5.5.1.4b Type declared in same compilation unit.
                    {
                        PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(simpleTypeName);
                        if (pmtd != null) return (IClass) pmtd;
                    }
                }

                // 6.5.5.1.5 Type declared in other compilation unit of same
                // package.
                {
                    String pkg = (
                        scopeCompilationUnit.optionalPackageDeclaration == null ? null :
                        scopeCompilationUnit.optionalPackageDeclaration.getPackageName()
                    );
                    IClassLoader icl = Java.getIClassLoader();
                    IClass result = icl.loadIClass(Descriptor.fromClassName(
                        pkg == null ?
                        simpleTypeName :
                        pkg + "." + simpleTypeName
                    ));
                    if (result != null) return result;
                }

                // 6.5.5.1.6 Type-import-on-demand declaration.
                {
                    IClass importedClass = scopeCompilationUnit.importTypeOnDemand(simpleTypeName, this.getLocation());
                    if (importedClass != null) return importedClass;
                }

                // 6.5.5.1.8 Give up.
                this.compileError("Cannot determine simple type name \"" + simpleTypeName + "\"");
                return Java.getIClassLoader().OBJECT;
            } else {

                // 6.5.5.2 Qualified type name (two or more identifiers).
                Atom q = Java.reclassifyName(this.getLocation(), this.scope, this.identifiers, this.identifiers.length - 1);
                String className;

                if (q instanceof Package) {

                    // 6.5.5.2.1 PACKAGE.CLASS
                    className = Java.join(this.identifiers, ".");
                } else {

                    // 6.5.5.2.2 CLASS.CLASS (member type)
                    className = (
                        Descriptor.toClassName(q.toTypeOrCE().getType().getDescriptor())
                        + '$'
                        + this.identifiers[this.identifiers.length - 1]
                    );
                }
                IClass result = Java.getIClassLoader().loadIClass(Descriptor.fromClassName(className));
                if (result != null) return result;

                this.compileError("Class \"" + className + "\" not found");
                return Java.getIClassLoader().OBJECT;
            }
        }
        public String toString() { return join(this.identifiers, "."); }

        public final void visit(Visitor visitor) { visitor.visitReferenceType(this); }

        private final String[] identifiers;
        private final Scope    scope;
    }

    // Helper class for JLS 15.9.1
    public static final class RvalueMemberType extends Type {
        private final Rvalue rvalue;
        private final String identifier;

        public RvalueMemberType(
            Location location,
            Rvalue   rvalue,
            String   identifier
        ) {
            super(location);
            this.rvalue     = rvalue;
            this.identifier = identifier;
        }

        public IClass getType() throws CompileException {
            IClass rvt = this.rvalue.getType();
            IClass memberType = Java.findMemberType(rvt, this.identifier, this.getLocation());
            if (memberType == null) this.compileError("\"" + rvt + "\" has no member type \"" + this.identifier + "\"");
            return memberType;
        }
        public String toString() { return this.identifier; }

        public final void visit(Visitor visitor) { visitor.visitRvalueMemberType(this); }
    }


    /**
     * Representation of a Java<sup>TM</sup> array type (JLS 10.1).
     */
    public static final class ArrayType extends Type {
        public ArrayType(Type componentType) {
            super(componentType.getLocation());
            this.componentType = componentType;
        }
        public Type getComponentType() {
            return this.componentType;
        }
        public IClass getType() throws CompileException {
            return Java.getArrayType(this.componentType.getType());
        }

        public String toString() {
            return this.componentType.toString() + "[]";
        }

        public final void visit(Visitor visitor) { visitor.visitArrayType(this); }

        final Type componentType;
    }

    /**
     * Representation of an "rvalue", i.e. an expression that has a type and
     * a value, but cannot be assigned to: An expression that can be the
     * right-hand-side of an assignment.
     */
    public static abstract class Rvalue extends Atom {
        protected Rvalue(
            Location location
        ) {
            super(location);
        }

        public Rvalue toRvalue() { return this; }

        /**
         * Some {@link Java.Rvalue}s compile more efficiently when their value
         * is not needed, e.g. "i++".
         */
        public void compile() throws CompileException {
            Java.pop((Located) this, this.compileGetValue());
        }

        /**
         * Convenience function that calls {@link
         * #compileContext()} and {@link
         * #compileGet()}.
         * @return The type of the Rvalue
         */
        public IClass compileGetValue() throws CompileException {
            Object cv = this.getConstantValue();
            if (cv != null) {
                Java.pushConstant((Located) this, cv);
                return this.getType();
            } 

            this.compileContext();
            return this.compileGet();
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
        public int compileContext() throws CompileException {
            return 0;
        }

        /**
         * Generates code that determines the value of the {@link Java.Rvalue}
         * and puts it on the operand stack. This method relies on that the
         * "context" of the {@link Java.Rvalue} is on top of the operand stack
         * (see {@link #compileContext()}).
         *
         * @return The type of the {@link Java.Rvalue}
         */
        public abstract IClass compileGet() throws CompileException;

        /**
         * Some {@link Java.Rvalue}s compile more efficiently when their value is the
         * condition for a branch.<br>
         *
         * Notice that if "this" is a constant, then either "dst" is never
         * branched to, or it is unconditionally branched to. "Unexamined code"
         * errors may result during bytecode validation.
         */
        public void compileBoolean(
            CodeContext.Offset dst,        // Where to jump.
            boolean              orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
        ) throws CompileException {
            Object cv = this.getConstantValue();
            if (cv instanceof Boolean) {
                if (orientation == Rvalue.JUMP_IF_FALSE ^ ((Boolean) cv).booleanValue()) {
                    this.writeBranch(Opcode.GOTO, dst);
                }
                return;
            }

            if (this.compileGetValue() != IClass.BOOLEAN) this.compileError("Not a boolean expression");

            this.writeBranch(orientation == Rvalue.JUMP_IF_TRUE ? Opcode.IFNE : Opcode.IFEQ, dst);
        }

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
         *   <tr><td>null</td><td>{@link #CONSTANT_VALUE_NULL}</td></tr>
         * </table>
         */
        public final Object getConstantValue() throws CompileException {
            if (this.constantValue == Java.Rvalue.CONSTANT_VALUE_UNKNOWN) {
                this.constantValue = this.getConstantValue2();
            }
            return this.constantValue;
        }
        private static final Object CONSTANT_VALUE_UNKNOWN = new Object();
        private Object              constantValue = Java.Rvalue.CONSTANT_VALUE_UNKNOWN;
        public Object getConstantValue2() throws CompileException {
            return null;
        }
        public static final Object CONSTANT_VALUE_NULL = new Throwable();

        /**
         * Attempts to evaluate the negated value of a constant {@link Java.Rvalue}.
         * This is particularly relevant for the smallest value of an integer or
         * long literal.
         *
         * @return null if value is not constant; otherwise a String, Byte,
         * Short, Integer, Boolean, Character, Float, Long or Double
         */
        public Object getNegatedConstantValue() throws CompileException {
            return null;
        }

        public final static boolean JUMP_IF_TRUE  = true;
        public final static boolean JUMP_IF_FALSE = false;
    }

    /**
     * Base class for {@link Java.Rvalue}s that compile better as conditional
     * branches.
     */
    public static abstract class BooleanRvalue extends Rvalue {
        protected BooleanRvalue(Location location) {
            super(location);
        }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {

            // Constant value?
            Object cv = this.getConstantValue();
            if (cv instanceof Boolean) {
                this.writeOpcode(
                    ((Boolean) cv).booleanValue() ?
                    Opcode.ICONST_1 :
                    Opcode.ICONST_0
                );
                return IClass.BOOLEAN;
            }

            CodeContext.Offset isTrue = this.newUnsetOffset();
            this.compileBoolean(isTrue, Rvalue.JUMP_IF_TRUE);
            this.writeOpcode(Opcode.ICONST_0);
            CodeContext.Offset end = this.newUnsetOffset();
            this.writeBranch(Opcode.GOTO, end);
            isTrue.set();
            this.writeOpcode(Opcode.ICONST_1);
            end.set();

            return IClass.BOOLEAN;
        }
    }

    /**
     * Representation of an "lvalue", i.e. an expression that has a type and
     * a value, and can be assigned to: An expression that can be the
     * left-hand-side of an assignment.
     */
    public static abstract class Lvalue extends Rvalue {
        protected Lvalue(Location location) {
            super(location);
        }

        public Lvalue toLvalue() { return this; }

        /**
         * Generates code that stores a value in the {@link Java.Lvalue}.
         * Expects the {@link Java.Lvalue}'s context (see {@link
         * #compileContext}) and a value of the {@link Java.Lvalue}'s type
         * on the operand stack.
         */
        public abstract void compileSet() throws CompileException;
    }

    /**
     * This class is special: It does not extend/implement the Atom subclasses,
     * but overrides Atom's "to...()" methods.
     */
    public static final class AmbiguousName extends Lvalue {
        public AmbiguousName(
            Location location,
            Scope    scope,
            String[] identifiers
        ) {
            this(location, scope, identifiers, identifiers.length);
        }
        public AmbiguousName(
            Location location,
            Scope    scope,
            String[] identifiers,
            int      n
        ) {
            super(location);
            this.scope       = scope;
            this.identifiers = identifiers;
            this.n           = n;
        }

        // Override "Atom.toType()".
        public Type toType() {
            return new Type(this.getLocation()) {
                public boolean isType() throws CompileException {
                    return AmbiguousName.this.reclassify().isType();
                }
                public IClass getType() throws CompileException {
                    return AmbiguousName.this.reclassify().toTypeOrCE().getType();
                }
                public String toString() {
                    return AmbiguousName.this.toString();
                }
                public final void visit(Visitor visitor) { AmbiguousName.this.visit(visitor); }
            };
        }

        // Compile time members.

        // Implement "Type".
        public IClass getType() throws CompileException {
            return this.reclassify().getType();
        }
        public boolean isType() throws CompileException {
            return this.reclassify().isType();
        }

        // Implement "Rvalue".
        public int compileContext() throws CompileException {
            return AmbiguousName.this.reclassify().toRvalueOrCE().compileContext();
        }
        public IClass compileGet() throws CompileException {
            return AmbiguousName.this.reclassify().toRvalueOrCE().compileGet();
        }
        public Object getConstantValue2() throws CompileException {
            return AmbiguousName.this.reclassify().toRvalueOrCE().getConstantValue();
        }

        // Implement "Lvalue".
        public void compileSet() throws CompileException {
            AmbiguousName.this.reclassify().toLvalueOrCE().compileSet(
            );
        }

        public String toString() {
            return Java.join(this.identifiers, ".", 0, this.n);
        }
    
        /*private*/ Atom reclassify() throws CompileException {
            if (this.reclassified == null) {
                this.reclassified = Java.reclassifyName(
                    this.getLocation(),
                    this.scope,
                    this.identifiers, this.n
                );
            }
            return this.reclassified;
        }
        private Atom reclassified = null;

        public final void visit(Visitor visitor) { visitor.visitAmbiguousName(this); }

        private final Scope    scope;
        private final String[] identifiers;
        private final int      n;
    }

    // Helper class for 6.5.2.1.7, 6.5.2.2.1
    static final class Package extends Atom {
        public Package(Location location, String name) {
            super(location);
            this.name = name;
        }
        public IClass getType() throws CompileException {
            this.compileError("Unknown variable or type \"" + this.name + "\"");
            return Java.getIClassLoader().OBJECT;
        }
        public String getName() { return this.name; }
        public String toString() { return this.name; }

        public final void visit(Visitor visitor) { visitor.visitPackage(this); }

        private final String name;
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
     * @return
     * @throws CompileException
     */
    private static Atom reclassifyName(
        Location         location,
        Scope            scope,
        final String[]   identifiers,
        int              n
    ) throws CompileException {
    
        if (n == 1) return Java.reclassifyName(
            location,
            scope,
            identifiers[0]
        );
    
        // 6.5.2.2
        Atom lhs = Java.reclassifyName(
            location,
            scope,
            identifiers, n - 1
        );
        String rhs = identifiers[n - 1];
    
        // 6.5.2.2.1
        if (Java.DEBUG) System.out.println("lhs = " + lhs);
        if (lhs instanceof Package) {
            String className = ((Package) lhs).getName() + '.' + rhs;
            IClass result = Java.getIClassLoader().loadIClass(Descriptor.fromClassName(className));
            if (result != null) return new SimpleType(location, result);

            return new Package(location, className);
        }
    
        // 6.5.2.2.3.2 EXPRESSION.length
        if (rhs.equals("length") && lhs.getType().isArray()) {
            return new ArrayLength(location, lhs.toRvalueOrCE());
        }
    
        IClass lhsType = lhs.getType();
    
        // Notice: Don't need to check for 6.5.2.2.2.1 TYPE.METHOD and 6.5.2.2.3.1
        // EXPRESSION.METHOD here because that has been done before.
    
        {
            IClass.IField field = Java.findIField(lhsType, rhs, location);
            if (field != null) {
                // 6.5.2.2.2.2 TYPE.FIELD
                // 6.5.2.2.3.2 EXPRESSION.FIELD
                return new FieldAccess(location, lhs, field);
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
                return new SimpleType(location, memberType);
            }
        }
    
        Java.compileError("\"" + rhs + "\" is neither a method, a field, nor a member class of \"" + lhsType + "\"", location);
        return new Atom(location) {
            public IClass getType() { return Java.getIClassLoader().OBJECT; }
            public String toString() { return Java.join(identifiers, "."); }
            public final void visit(Visitor visitor) {}
        };
    }

    /**
     * JLS 6.5.2.1
     * @param location
     * @param scope
     * @param identifier
     * @return
     * @throws CompileException
     */
    private static Atom reclassifyName(
        Location     location,
        Scope        scope,
        final String identifier
    ) throws CompileException {

        // Determine scope type body declaration, type and compilation unit.
        TypeBodyDeclaration scopeTBD = null;
        TypeDeclaration     scopeTypeDeclaration = null;
        CompilationUnit     scopeCompilationUnit;
        {
            Scope s = scope;
            while (s instanceof Statement && !(s instanceof TypeBodyDeclaration)) s = s.getEnclosingScope();
            if (s instanceof TypeBodyDeclaration) {
                scopeTBD = (TypeBodyDeclaration) s;
                s = s.getEnclosingScope();
          }
          if (s instanceof TypeDeclaration) {
                scopeTypeDeclaration = (TypeDeclaration) s;
                s = s.getEnclosingScope();
          }
            while (!(s instanceof CompilationUnit)) s = s.getEnclosingScope();
            scopeCompilationUnit = (CompilationUnit) s;
        }
    
        // 6.5.2.1.BL1
        
        // 6.5.2.BL1.B1.B1.1/6.5.6.1.1 Local variable.
        // 6.5.2.BL1.B1.B1.2/6.5.6.1.1 Parameter.
        {
            InnerClassDeclaration icd = null;
            for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
                if (s instanceof InnerClassDeclaration) {
                    icd = (InnerClassDeclaration) s;
                    continue;
                }

                LocalVariable lv = null;
                if (s instanceof Block) {
                    Block b = (Block) s;
                    lv = (LocalVariable) b.localVariables.get(identifier);
                } else
                if (s instanceof FunctionDeclarator) {
                    FunctionDeclarator fd = (FunctionDeclarator) s;
                    lv = (LocalVariable) fd.parameters.get(identifier);
                }
                if (lv == null) continue;

                if (icd == null) {
                    return new LocalVariableAccess(location, lv);
                } else {
                    if (!lv.finaL) Java.compileError("Cannot access non-final local variable \"" + identifier + "\" from inner class");
                    final IClass lvType = lv.type;
                    IClass.IField iField = ((IClass) icd).new IField() {
                        public Object  getConstantValue() { return null; }
                        public String  getName()          { return "val$" + identifier; }
                        public IClass  getType()          { return lvType; }
                        public boolean isStatic()         { return false; }
                        public int     getAccess()        { return IClass.PACKAGE; }
                    };
                    icd.defineSyntheticField(iField);
                    return new FieldAccess(
                        location,                   // location
                        new QualifiedThisReference( // lhs
                            location,                              // location
                            (Scope) scopeTBD,                      // scope
                            new SimpleType(location, (IClass) icd) // qualification
                        ),
                        iField                      // field
                    );
                }
            }
        }

        // 6.5.2.BL1.B1.B1.3/6.5.6.1.2.1 Field.
        for (Scope s = scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
            if (s instanceof TypeDeclaration) {
                final TypeDeclaration td = (TypeDeclaration) s;
                final IClass.IField f = Java.findIField((IClass) td, identifier, location);
                if (f != null) {
                    if (f.isStatic()) {
                        Java.warning("IASF", "Implicit access to static field \"" + identifier + "\" of declaring class (better write \"" + f.getDeclaringIClass() + '.' + f.getName() + "\")", location);
                    } else
                    if (f.getDeclaringIClass() == td) {
                        Java.warning("IANSF", "Implicit access to non-static field \"" + identifier + "\" of declaring class (better write \"this." + f.getName() + "\")", location);
                    } else {
                        Java.warning("IANSFEI", "Implicit access to non-static field \"" + identifier + "\" of enclosing instance (better write \"" + f.getDeclaringIClass() + ".this." + f.getName() + "\")", location);
                    }

                    Java.Type ct = new SimpleType(scopeTypeDeclaration.getLocation(), (IClass) td);
                    Atom lhs;
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
                            lhs = new QualifiedThisReference(location, (Scope) scopeTBD, ct);
                        }
                    }
                    return new FieldAccess(location, lhs, f);
                }
            }
        }

        // Hack: "java" MUST be a package, not a class.
        if (identifier.equals("java")) return new Package(location, identifier);
        
        // 6.5.2.BL1.B1.B2.1 Local class.
        for (Scope s = scope; s instanceof Block; s = s.getEnclosingScope()) {
            Block b = (Block) s;
            LocalClassDeclaration lcd = b.getLocalClassDeclaration(identifier);
            if (lcd != null) return new SimpleType(location, lcd);
        }
        
        // 6.5.2.BL1.B1.B2.2 Member type.
        if (scopeTypeDeclaration != null) {
            IClass memberType = Java.findMemberType((IClass) scopeTypeDeclaration, identifier, location);
            if (memberType != null) return new SimpleType(location, memberType);
        }
        
        // 6.5.2.BL1.B1.B3.1 Single-type-import.
        if (scopeCompilationUnit != null) {
            IClass iClass = scopeCompilationUnit.importSingleType(identifier, location);
            if (iClass != null) return new SimpleType(location, iClass);
        }
        
        // 6.5.2.BL1.B1.B3.2 Package member class/interface declared in this compilation unit.
        if (scopeCompilationUnit != null) {
            PackageMemberTypeDeclaration pmtd = scopeCompilationUnit.getPackageMemberTypeDeclaration(identifier);
            if (pmtd != null) return new SimpleType(location, (IClass) pmtd);
        }
        
        // 6.5.2.BL1.B1.B4 Class or interface declared in same package.
        if (scopeCompilationUnit != null) {
            String className = (
                scopeCompilationUnit.optionalPackageDeclaration == null ?
                identifier :
                scopeCompilationUnit.optionalPackageDeclaration.getPackageName() + '.' + identifier
            );
            IClass result = Java.getIClassLoader().loadIClass(Descriptor.fromClassName(className));
            if (result != null) return new SimpleType(location, result);
        }
        
        // 6.5.2.BL1.B1.B5, 6.5.2.BL1.B1.B6 Type-import-on-demand.
        if (scopeCompilationUnit != null) {
            IClass importedClass = scopeCompilationUnit.importTypeOnDemand(identifier, location);
            if (importedClass != null) {
                return new SimpleType(location, importedClass);
            }
        }
        
        // 6.5.2.BL1.B1.B7 Package name
        return new Package(location, identifier);
    }

    /**
     * Representation of a local variable access.
     */
    static final class LocalVariableAccess extends Lvalue {
        public LocalVariableAccess(
            Location      location,
            LocalVariable localVariable
        ) {
            super(location);
            this.localVariable = localVariable;
        }

        /*final*/ LocalVariable localVariable;

        // Compile time members.

        public IClass getType() { return this.localVariable.type; }
        public IClass compileGet() throws CompileException {
            return Java.load((Located) this, this.localVariable);
        }
        public void compileSet() throws CompileException {
            Java.store(
                (Located) this,
                this.localVariable.type,
                this.localVariable
            );
        }
        public String toString() { return this.localVariable.toString(); }

        public final void visit(Visitor visitor) { visitor.visitLocalVariableAccess(this); }
    }

    /**
     * Representation of an access to a field of a class or an interface. (Does not implement the
     * "array length" expression, e.g. "ia.length".)
     */
    static final class FieldAccess extends Lvalue {
        public FieldAccess(
            Location      location,
            Atom          lhs,
            IClass.IField field
        ) {
            super(location);
            this.lhs   = lhs;
            this.field = field;
        }

        final Atom          lhs;
        final IClass.IField field;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.field.getType();
        }
        public String toString() { return this.lhs.toString() + '.' + this.field.toString(); }
        // Implement "Rvalue".
        public int compileContext() throws CompileException {
            if (this.field.isStatic()) {
                this.lhs.toTypeOrCE().getType();
                return 0;
            } else {
                this.lhs.toRvalueOrCE().compileGetValue();
                return 1;
            }
        }
        public IClass compileGet() throws CompileException {
            if (this.field.isStatic()) {
                this.writeOpcode(Opcode.GETSTATIC);
            } else {
                this.writeOpcode(Opcode.GETFIELD);
            }
            this.writeConstantFieldrefInfo(
                this.field.getDeclaringIClass().getDescriptor(), // classFD
                this.field.getName(),                            // fieldName
                this.field.getType().getDescriptor()             // fieldFD
            );
            return this.field.getType();
        }
        public void compileSet() throws CompileException {
            this.writeOpcode(
                this.field.isStatic() ?
                Opcode.PUTSTATIC :
                Opcode.PUTFIELD
            );
            this.writeConstantFieldrefInfo(
                this.field.getDeclaringIClass().getDescriptor(), // classFD
                this.field.getName(),                            // fieldName
                this.field.getDescriptor()                       // fieldFD
            );
        }
        public Object getConstantValue2() throws CompileException {
            return this.field.getConstantValue();
        }

        public final void visit(Visitor visitor) { visitor.visitFieldAccess(this); }
    }

    static final class ArrayLength extends Rvalue {
        public ArrayLength(
            Location location,
            Rvalue   lhs
        ) {
            super(location);
            this.lhs = lhs;
        }

        final Rvalue lhs;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return IClass.INT;
        }
        public String toString() { return this.lhs.toString() + ".length"; }
        // Implement "Rvalue".
        public int compileContext() throws CompileException {
            if (!this.lhs.compileGetValue().isArray()) this.compileError("Cannot determine length of non-array type");
            return 1;
        }
        public IClass compileGet() throws CompileException {
            this.writeOpcode(Opcode.ARRAYLENGTH);
            return IClass.INT;
        }

        public final void visit(Visitor visitor) { visitor.visitArrayLength(this); }
    }

    /**
     * Representation of an access to the current object or an enclosing instance.
     */
    public static final class ThisReference extends Rvalue {
    
        /**
         * Access the declaring class.
         * @param location
         * @param scope
         */
        public ThisReference(Location location, Scope scope) {
            super(location);
            this.scope = scope;
        }
    
        private final Scope scope;
    
        // Compile time members.
    
        public ThisReference(
            Location location,
            IClass   iClass
        ) {
            super(location);
            this.scope  = null;
            this.iClass = iClass;
        }
    
        private IClass iClass = null;
    
        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.getIClass();
        }
        public String toString() { return "this"; }
    
        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            Java.referenceThis((Located) this);
            return this.getIClass();
        }
    
        // Internal helpers.
    
        private IClass getIClass() throws CompileException {
            if (this.iClass == null) {

                // Compile error if in static function context.
                Scope s;
                for (s = this.scope; s instanceof Statement; s = s.getEnclosingScope());
                if (s instanceof FunctionDeclarator) {
                    FunctionDeclarator function = (FunctionDeclarator) s;
                    if ((function.modifiers & Mod.STATIC) != 0) this.compileError("No current instance available in static method");
                }

                // Determine declaring type.
                while (!(s instanceof TypeDeclaration)) s = s.getEnclosingScope();
                if (!(s instanceof ClassDeclaration)) this.compileError("Only methods of classes can have a current instance");
                this.iClass = (ClassDeclaration) s;
            }
            return this.iClass;
        }

        public final void visit(Visitor visitor) { visitor.visitThisReference(this); }
    }
    
    /**
     * Representation of an access to the current object or an enclosing instance.
     */
    public static final class QualifiedThisReference extends Rvalue {
    
        /**
         * Access the given enclosing instance of the declaring class.
         * @param location
         * @param scope
         * @param qualification
         */
        public QualifiedThisReference(
            Location location,
            Scope    scope,
            Type     qualification
        ) {
            super(location);

            if (scope         == null) throw new NullPointerException();
            if (qualification == null) throw new NullPointerException();

            this.scope         = scope;
            this.qualification = qualification;
        }
    
        private final Scope scope;
        final Type          qualification;
    
        // Compile time members.
    
        private ClassDeclaration    declaringClass = null;
        private TypeBodyDeclaration declaringTypeBodyDeclaration = null;
        private IClass              targetIClass = null;
    
        public QualifiedThisReference(
            Location           location,
            ClassDeclaration   declaringClass,
            FunctionDeclarator optionalDeclaringFunction,
            IClass             targetIClass
        ) {
            super(location);
            if (declaringClass == null) throw new NullPointerException();
            if (targetIClass   == null) throw new NullPointerException();

            this.scope                        = null;
            this.qualification                = null;
            this.declaringClass               = declaringClass;
            this.declaringTypeBodyDeclaration = optionalDeclaringFunction;
            this.targetIClass                 = targetIClass;
        }
    
        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.getTargetIClass();
        }
        public String toString() {
            return this.qualification.toString() + ".this";
        }
    
        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            Java.referenceThis(
                (Located) this,
                this.getDeclaringClass(),
                this.getDeclaringTypeBodyDeclaration(),
                this.getTargetIClass()
            );
            return this.getTargetIClass();
        }
    
        // Internal helpers.
    
        private ClassDeclaration getDeclaringClass() throws CompileException {
            if (this.declaringClass == null) {
                this.getDeclaringTypeBodyDeclaration();
            }
            return this.declaringClass;
        }
    
        private TypeBodyDeclaration getDeclaringTypeBodyDeclaration() throws CompileException {
            if (this.declaringTypeBodyDeclaration == null) {
    
                // Compile error if in static function context.
                Scope s;
                for (s = this.scope; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
                this.declaringTypeBodyDeclaration = (TypeBodyDeclaration) s;
                if (this.declaringTypeBodyDeclaration.isStatic()) this.compileError("No current instance available in static method");
    
                // Determine declaring type.
                this.declaringClass = (ClassDeclaration) this.declaringTypeBodyDeclaration.getDeclaringType();
            }
            return this.declaringTypeBodyDeclaration;
        }

        private IClass getTargetIClass() throws CompileException {
    
            // Determine target type.
            if (this.targetIClass == null) {
                this.targetIClass = this.qualification.getType();
            }
            return this.targetIClass;
        }

        public final void visit(Visitor visitor) { visitor.visitQualifiedThisReference(this); }
    }

    static void referenceThis(Located located) {
        located.writeOpcode(Opcode.ALOAD_0);
    }

    static void referenceThis(
        Located             located,
        ClassDeclaration    declaringClass,
        TypeBodyDeclaration declaringTypeBodyDeclaration,
        IClass              targetIClass
    ) throws CompileException {
        List path = Java.getOuterClasses(declaringClass);

        if (declaringTypeBodyDeclaration.isStatic()) located.compileError("No current instance available instatic context");

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
                if (targetIClass.isAssignableFrom((IClass) path.get(j))) break TARGET_FOUND;
            }
            located.compileError("\"" + declaringClass + "\" is not enclosed by \"" + targetIClass + "\"");
        }

        int i;
        if (declaringTypeBodyDeclaration instanceof ConstructorDeclarator) {
            if (j == 0) {
                located.writeOpcode(Opcode.ALOAD_0);
                return;
            }

            ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) declaringTypeBodyDeclaration;
            LocalVariable syntheticParameter = (LocalVariable) constructorDeclarator.syntheticParameters.get("this$" + (path.size() - 2));
            if (syntheticParameter == null) throw new RuntimeException();
            Java.load(located, syntheticParameter);
            i = 1;
        } else {
            located.writeOpcode(Opcode.ALOAD_0);
            i = 0;
        }
        for (; i < j; ++i) {
            final String fieldName = "this$" + (path.size() - i - 2);
            final IClass inner = (IClass) path.get(i);
            final IClass outer = (IClass) path.get(i + 1);
            ((InnerClassDeclaration) inner).defineSyntheticField(inner.new IField() {
                public Object  getConstantValue() { return null; }
                public String  getName()          { return fieldName; }
                public IClass  getType()          { return outer; }
                public boolean isStatic()         { return false; }
                public int     getAccess()        { return IClass.PACKAGE; }
            });
            located.writeOpcode(Opcode.GETFIELD);
            located.writeConstantFieldrefInfo(
                inner.getDescriptor(), // classFD
                fieldName,             // fieldName
                outer.getDescriptor()  // fieldFD
            );
        }
    }

    /**
     * Return a list consisting of the given <code>inner</code> class and all its outer classes.
     * @return {@link List} of {@link IClass}
     */
    private static List getOuterClasses(IClass inner) throws CompileException {
        List path = new ArrayList();
        for (IClass ic = inner; ic != null; ic = ic.getOuterIClass()) path.add(ic);
        return path;
    }

    public static final class ClassLiteral extends Rvalue {
        private final AbstractTypeDeclaration declaringType;
        final Type                            type;

        public ClassLiteral(
            Location location,
            Scope    enclosingScope,
            Type     type
        ) {
            super(location);
            Scope s;
            for (s = enclosingScope; !(s instanceof AbstractTypeDeclaration); s = s.getEnclosingScope());
            this.declaringType = (AbstractTypeDeclaration) s;
            this.type          = type;
        }

        // Compile time members.

        //Implement "Atom".
        public IClass getType() throws CompileException {
            return Java.getIClassLoader().CLASS;
        }
        public String toString() { return this.type.toString() + ".class"; }
        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            Location loc = this.getLocation();
            final IClassLoader icl = Java.getIClassLoader();
            IClass iClass = this.type.getType();

            if (iClass.isPrimitive()) {

                // Primitive class literal.
                this.writeOpcode(Opcode.GETSTATIC);
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
                for (Iterator it = this.declaringType.declaredMethods.iterator(); it.hasNext();) {
                    MethodDeclarator md = (MethodDeclarator) it.next();
                    if (md.getName().equals("class$")) {
                        classDollarMethodDeclared = true;
                        break;
                    }
                }
            }
            if (!classDollarMethodDeclared) this.declareClassDollarMethod();

            // Determine the statics of the declaring class (this is where static fields
            // declarations are found).
            List statics; // TypeBodyDeclaration
            if (this.declaringType instanceof ClassDeclaration) {
                statics = ((ClassDeclaration) this.declaringType).variableDeclaratorsAndInitializers;
            } else
            if (this.declaringType instanceof InterfaceDeclaration) {
                statics = ((InterfaceDeclaration) this.declaringType).constantDeclarations;
            } else {
                throw new RuntimeException();
            }

            String className = Descriptor.toClassName(iClass.getDescriptor());
            final String classDollarFieldName = "class$" + className.replace('.', '$');

            // Declare the static "class dollar field" if not already done.
            {
                boolean hasClassDollarField = false;
                BLOCK_STATEMENTS: for (Iterator it = statics.iterator(); it.hasNext();) {
                    TypeBodyDeclaration tbd = (TypeBodyDeclaration) it.next();
                    if (!tbd.isStatic()) continue;
                    if (tbd instanceof FieldDeclarator) {
                        FieldDeclarator fd = (FieldDeclarator) tbd;
                        IClass.IField[] fds = fd.getIFields();
                        for (int j = 0; j < fds.length; ++j) {
                            if (fds[j].getName().equals(classDollarFieldName)) {
                                hasClassDollarField = true;
                                break BLOCK_STATEMENTS;
                            }
                        }
                    }
                }
                if (!hasClassDollarField) {
                    Type classType = new SimpleType(loc, icl.CLASS);
                    FieldDeclarator fd = new FieldDeclarator(
                        loc,                // location
                        this.declaringType, // declaringType
                        Mod.STATIC,         // modifiers
                        classType           // type
                    );
                    fd.setVariableDeclarators(new VariableDeclarator[] {
                        new VariableDeclarator(
                            loc,                  // location
                            classDollarFieldName, // name
                            0,                    // brackets
                            (Rvalue) null         // optionalInitializer
                        )
                    });
                    if (this.declaringType instanceof ClassDeclaration) {
                        ((ClassDeclaration) this.declaringType).addVariableDeclaratorOrInitializer(fd);
                    } else
                    if (this.declaringType instanceof InterfaceDeclaration) {
                        ((InterfaceDeclaration) this.declaringType).addConstantDeclaration(fd);
                    } else {
                        throw new RuntimeException();
                    }
                }
            }

            // return (class$X != null) ? class$X : (class$X = class$("X"));
            Type declaringClassOrInterfaceType = new SimpleType(loc, this.declaringType);
            Lvalue classDollarFieldAccess = new Lvalue(loc) {
                public IClass compileGet() throws CompileException {
                    this.writeOpcode(Opcode.GETSTATIC);
                    this.writeConstantFieldrefInfo(
                        ClassLiteral.this.declaringType.getDescriptor(), // classFD
                        classDollarFieldName,               // fieldName
                        icl.CLASS.getDescriptor()           // fieldFD
                    );
                    return icl.CLASS;
                }
                public IClass getType() throws CompileException {
                    return icl.CLASS;
                }
                public String toString() { return "???"; }
                public void compileSet() throws CompileException {
                    this.writeOpcode(Opcode.PUTSTATIC);
                    this.writeConstantFieldrefInfo(
                        ClassLiteral.this.declaringType.getDescriptor(), // classFD
                        classDollarFieldName,                            // fieldName
                        icl.CLASS.getDescriptor()                        // fieldFD
                    );
                }
                public final void visit(Visitor visitor) {}
            };
                
            return new ConditionalExpression(
                loc,                    // location
                new BinaryOperation(    // lhs
                    loc,                         // location
                    classDollarFieldAccess,      // lhs
                    "!=",                        // op
                    new ConstantValue(loc, null) // rhs
                ),
                classDollarFieldAccess, // mhs
                new Assignment(         // rhs
                    loc,                    // location
                    classDollarFieldAccess, // lhs
                    "=",                    // operator
                    new MethodInvocation(   // rhs
                        loc,                           // location
                        (Scope) this.declaringType,    // scope
                        declaringClassOrInterfaceType, // optionalTarget
                        "class$",                      // methodName
                        new Rvalue[] {                 // arguments
                            new ConstantValue(
                                loc,      // location
                                className // constantValue
                            )
                        }
                    )
                )
            ).compileGet();
        }

        private void declareClassDollarMethod() {
            final IClassLoader icl = Java.getIClassLoader();
            
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
            Location loc = this.getLocation();
            Type stringType = new SimpleType(loc, icl.STRING);
            Type classType = new SimpleType(loc, icl.CLASS);

            // Class class$(String className)
            FormalParameter fp = new FormalParameter(false, stringType, "className");
            MethodDeclarator cdmd = new MethodDeclarator(
                loc,                            // location
                this.declaringType,             // declaringType
                Mod.STATIC,                     // modifiers
                classType,                      // type
                "class$",                       // name
                new FormalParameter[] { fp },   // formalParameters
                new Java.Type[0]                // thrownExceptions
            );

            Block body = new Block(loc, (Scope) cdmd);

            // try {
            TryStatement ts = new TryStatement(loc, (Scope) cdmd);
        
            // return Class.forName(className);
            MethodInvocation mi = new MethodInvocation(
                loc,           // location
                (Scope) ts,    // enclosingScope
                classType,     // optionalTarget
                "forName",     // methodName
                new Rvalue[] { // arguments
                    new AmbiguousName(loc, (Scope) ts, new String[] { "className" } )
                }
            );
            ts.setBody(new ReturnStatement(loc, (Scope) ts, mi));
        
            IClass classNotFoundExceptionIClass = icl.loadIClass("Ljava/lang/ClassNotFoundException;");
            if (classNotFoundExceptionIClass == null) throw new RuntimeException();

            IClass noClassDefFoundErrorIClass = icl.loadIClass("Ljava/lang/NoClassDefFoundError;");
            if (noClassDefFoundErrorIClass == null) throw new RuntimeException();

            // catch (ClassNotFoundException ex) {
            Block b = new Block(loc, (Scope) ts);
            CatchClause cc = new CatchClause(
                new FormalParameter(true, new SimpleType(loc, classNotFoundExceptionIClass), "ex"), // caughtException
                b                                                                                   // body
            );

            // throw new NoClassDefFoundError(ex.getMessage());
            NewClassInstance nci = new NewClassInstance(
                loc,                                             // location
                (Scope) b,                                       // scope
                (Rvalue) null,                                   // optionalQualification
                new SimpleType(loc, noClassDefFoundErrorIClass), // type
                new Rvalue[] {                                   // arguments
                    new MethodInvocation(
                        loc,                                                      // location
                        (Scope) b,                                                // enclosingScope
                        new AmbiguousName(loc, (Scope) b, new String[] { "ex"} ), // optionalTarget
                        "getMessage",                                             // methodName
                        new Rvalue[0]                                             // arguments
                    )
                }
            );
            b.addStatement(new ThrowStatement(loc, (Scope) b, nci));

            ts.addCatchClause(cc);

            body.addStatement(ts);

            cdmd.setBody(body);

            this.declaringType.declaredMethods.add(cdmd);
            this.declaringType.declaredIMethods = null;
        }

        public final void visit(Visitor visitor) { visitor.visitClassLiteral(this); }
    }

    public static final class Assignment extends Rvalue {
        public Assignment(
            Location location,
            Lvalue   lhs,
            String   operator,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs      = lhs;
            this.operator = operator;
            this.rhs      = rhs;
        }

        final Lvalue lhs;
        final String operator;
        final Rvalue rhs;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.lhs.getType();
        }
        public String toString() { return this.lhs.toString() + ' ' + this.operator + ' ' + this.rhs.toString(); }

        // Implement "Rvalue".
        public void compile() throws Java.CompileException {
            if (this.operator == "=") {
                this.lhs.compileContext();
                Java.assignmentConversion(
                    (Located) this,             // located
                    this.rhs.compileGetValue(), // sourceType
                    this.lhs.getType(),         // targetType
                    this.rhs.getConstantValue() // optionalConstantValue
                );
                this.lhs.compileSet();
                return;
            }

            // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
            int lhsCS = this.lhs.compileContext();
            Java.dup((Located) this, lhsCS);
            IClass lhsType = this.lhs.compileGet();
            IClass resultType = Java.compileArithmeticBinaryOperation(
                (Located) this,             // located
                lhsType,                    // lhsType
                this.operator.substring(    // operator
                    0,
                    this.operator.length() - 1
                ).intern(), // <= IMPORTANT!
                this.rhs                    // rhs
            );
            // Convert the result to LHS type (JLS2 15.26.2).
            if (
                !Java.tryIdentityConversion(resultType, lhsType) &&
                !Java.tryNarrowingPrimitiveConversion(
                    (Located) this,     // located
                    resultType,         // sourceType
                    lhsType             // destinationType
                )
            ) throw new RuntimeException();
            this.lhs.compileSet();
        }
        public IClass compileGet() throws Java.CompileException {
            if (this.operator == "=") {
                int lhsCS = this.lhs.compileContext();
                IClass rhsType = this.rhs.compileGetValue();
                IClass lhsType = this.lhs.getType();
                Object rhsCV = this.rhs.getConstantValue();
                Java.assignmentConversion(
                    (Located) this, // located
                    rhsType,        // sourceType
                    lhsType,        // targetType
                    rhsCV           // optionalConstantValue
                );
                Java.dupx(
                    (Located) this, // located
                    lhsType,        // type
                    lhsCS           // x
                );
                this.lhs.compileSet();
                return lhsType;
            }

            // Implement "|= ^= &= *= /= %= += -= <<= >>= >>>=".
            int lhsCS = this.lhs.compileContext();
            Java.dup((Located) this, lhsCS);
            IClass lhsType = this.lhs.compileGet();
            IClass resultType = Java.compileArithmeticBinaryOperation(
                (Located) this,             // located
                lhsType,                    // lhsType
                this.operator.substring(    // operator
                    0,
                    this.operator.length() - 1
                ).intern(), // <= IMPORTANT!
                this.rhs                    // rhs
            );
            // Convert the result to LHS type (JLS2 15.26.2).
            if (
                !Java.tryIdentityConversion(resultType, lhsType) &&
                !Java.tryNarrowingPrimitiveConversion(
                    (Located) this,     // located
                    resultType,         // sourceType
                    lhsType             // destinationType
                )
            ) throw new RuntimeException();
            Java.dupx(
                (Located) this, // located
                lhsType,        // type
                lhsCS           // x
            );
            this.lhs.compileSet();
            return lhsType;
        }

        public final void visit(Visitor visitor) { visitor.visitAssignment(this); }
    }

    public static final class ConditionalExpression extends Rvalue {
        public ConditionalExpression(
            Location location,
            Rvalue   lhs,
            Rvalue   mhs,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs = lhs;
            this.mhs = mhs;
            this.rhs = rhs;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.mhs.getType();
        }
        public String toString() { return this.lhs.toString() + " ? " + this.mhs.toString() + " : " + this.rhs.toString(); }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            IClass mhsType, rhsType;
            CodeContext.Inserter mhsConvertInserter;
            CodeContext.Offset toEnd = this.newUnsetOffset();
            Object cv = this.lhs.getConstantValue();
            if (cv instanceof Boolean) {
                if (((Boolean) cv).booleanValue()) {
                    mhsType = this.mhs.compileGetValue();
                    mhsConvertInserter = this.newInserter();
                    rhsType = this.rhs.getType();
                } else {
                    mhsType = this.mhs.getType();
                    mhsConvertInserter = null;
                    rhsType = this.rhs.compileGetValue();
                }
            } else {
                CodeContext.Offset toRhs = this.newUnsetOffset();

                this.lhs.compileBoolean(toRhs, Rvalue.JUMP_IF_FALSE);
                mhsType = this.mhs.compileGetValue();
                mhsConvertInserter = this.newInserter();
                this.writeBranch(Opcode.GOTO, toEnd);
                toRhs.set();
                rhsType = this.rhs.compileGetValue();
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
                expressionType = Java.binaryNumericPromotion(
                    (Located) this,     // located
                    mhsType,            // type1
                    mhsConvertInserter, // convertInserter1
                    rhsType             // type2
                );
            } else
            if (this.mhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL && !rhsType.isPrimitive()) {

                // JLS 15.25.1.3 (null : ref)
                expressionType = rhsType;
            } else
            if (!mhsType.isPrimitive() && this.rhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL) {

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
                    this.compileError("Reference types \"" + mhsType + "\" and \"" + rhsType + "\" don't match");
                    return Java.getIClassLoader().OBJECT;
                }
            } else
            {
                this.compileError("Incompatible expression types \"" + mhsType + "\" and \"" + rhsType + "\"");
                return Java.getIClassLoader().OBJECT;
            }
            toEnd.set();

            return expressionType;
        }

        public final void visit(Visitor visitor) { visitor.visitConditionalExpression(this); }

        final Rvalue lhs, mhs, rhs;
    }

    /**
     * Objects of this class represent represent one pre- or post-increment
     * or decrement.
     */
    public static final class Crement extends Rvalue {
        public Crement(Location location, String operator, Lvalue operand) {
            super(location);
            this.pre      = true;
            this.operator = operator;
            this.operand  = operand;
        }
        public Crement(Location location, Lvalue operand, String operator) {
            super(location);
            this.pre      = false;
            this.operator = operator;
            this.operand  = operand;
        }

        final boolean pre;
        final String  operator;
        final Lvalue  operand;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.operand.getType();
        }
        public String toString() {
            return (
                this.pre ?
                this.operator + this.operand :
                this.operand + this.operator
            );
        }

        // Implement "Rvalue".
        public void compile() throws CompileException {

            // Optimized crement of integer local variable.
            LocalVariable lv = this.isIntLV();
            if (lv != null) {
                this.writeOpcode(Opcode.IINC);
                this.writeByte(lv.localVariableArrayIndex);
                this.writeByte(this.operator == "++" ? 1 : -1);
                return;
            }

            int cs = this.operand.compileContext();
            Java.dup((Located) this, cs);
            IClass type = this.operand.compileGet();
            IClass promotedType = Java.unaryNumericPromotion((Located) this, type);
            this.writeOpcode(Java.ilfd(
                promotedType,
                Opcode.ICONST_1,
                Opcode.LCONST_1,
                Opcode.FCONST_1,
                Opcode.DCONST_1
            ));
            if (this.operator == "++") {
                this.writeOpcode(Opcode.IADD + Java.ilfd(promotedType));
            } else
            if (this.operator == "--") {
                this.writeOpcode(Opcode.ISUB + Java.ilfd(promotedType));
            } else {
                this.compileError("Unexpected operator \"" + this.operator + "\"");
            }

            if (
                !Java.tryIdentityConversion(promotedType, type) &&
                !Java.tryNarrowingPrimitiveConversion(
                    (Located) this,     // located
                    promotedType,       // sourceType
                    type                // targetType
                )
            ) throw new RuntimeException();
            this.operand.compileSet();
        }

        public IClass compileGet() throws CompileException {

            // Optimized crement of integer local variable.
            LocalVariable lv = this.isIntLV();
            if (lv != null) {
                if (!this.pre) Java.load((Located) this, lv);
                this.writeOpcode(Opcode.IINC);
                this.writeByte(lv.localVariableArrayIndex);
                this.writeByte(this.operator == "++" ? 1 : -1);
                if (this.pre) Java.load((Located) this, lv);
                return lv.type;
            }

            // Compile operand context.
            int cs = this.operand.compileContext();
            // DUP operand context.
            Java.dup((Located) this, cs);
            // Get operand value.
            IClass type = this.operand.compileGet();
            // DUPX operand value.
            if (!this.pre) Java.dupx((Located) this, type, cs);
            // Apply "unary numeric promotion".
            IClass promotedType = unaryNumericPromotion((Located) this, type);
            // Crement.
            this.writeOpcode(Java.ilfd(
                promotedType,
                Opcode.ICONST_1,
                Opcode.LCONST_1,
                Opcode.FCONST_1,
                Opcode.DCONST_1
            ));
            if (this.operator == "++") {
                this.writeOpcode(Opcode.IADD + Java.ilfd(promotedType));
            } else
            if (this.operator == "--") {
                this.writeOpcode(Opcode.ISUB + Java.ilfd(promotedType));
            } else {
                this.compileError("Unexpected operator \"" + this.operator + "\"");
            }
            // Reverse "unary numeric promotion".
            if (
                !Java.tryIdentityConversion(promotedType, type) &&
                !Java.tryNarrowingPrimitiveConversion(
                    (Located) this,     // located
                    promotedType,       // sourceType
                    type                // targetType
                )
            ) throw new RuntimeException();
            // DUPX cremented operand value.
            if (this.pre) Java.dupx((Located) this, type, cs);
            // Set operand.
            this.operand.compileSet();

            return type;
        }

        /**
         * Checks whether the operand is an integer-like local variable.
         */
        private LocalVariable isIntLV() throws CompileException {
            if (!(this.operand instanceof AmbiguousName)) return null;
            AmbiguousName an = (AmbiguousName) this.operand;

            Atom rec = an.reclassify();
            if (!(rec instanceof LocalVariableAccess)) return null;
            LocalVariableAccess lva = (LocalVariableAccess) rec;

            LocalVariable lv = lva.localVariable;
            if (lv.finaL) lva.compileError("Must not increment or decrement \"final\" local variable");
            if (
                lv.type == IClass.BYTE  ||
                lv.type == IClass.SHORT ||
                lv.type == IClass.INT   ||
                lv.type == IClass.CHAR
            ) return lv;
            return null;
        }

        public final void visit(Visitor visitor) { visitor.visitCrement(this); }
    }

    /**
     * This class implements an array access.
     */
    public static final class ArrayAccessExpression extends Lvalue {
        public ArrayAccessExpression(
            Location location,
            Rvalue   lhs,
            Rvalue   index
        ) {
            super(location);
            this.lhs = lhs;
            this.index = index;
        }

        final Rvalue lhs;
        final Rvalue index;

        // Compile time members:

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.lhs.getType().getComponentType();
        }
        public String toString() { return this.lhs.toString() + '[' + this.index + ']'; }

        // Implement "Rvalue".
        public int compileContext() throws Java.CompileException {
            IClass lhsType = this.lhs.compileGetValue();
            if (!lhsType.isArray()) this.compileError("Subscript not allowed on non-array type \"" + lhsType.toString() + "\"");

            IClass indexType = this.index.compileGetValue();
            if (
                !Java.tryIdentityConversion(indexType, IClass.INT) &&
                !Java.tryWideningPrimitiveConversion(
                    (Located) this, // located
                    indexType,      // sourceType
                    IClass.INT      // targetType
                )
            ) this.compileError("Index expression of type \"" + indexType + "\" cannot be widened to \"int\"");

            return 2;
        }
        public IClass compileGet() throws Java.CompileException {
            IClass lhsComponentType = this.getType();
            this.writeOpcode(Opcode.IALOAD + Java.ilfdabcs(lhsComponentType));
            return lhsComponentType;
        }

        // Implement Lvalue
        public void compileSet() throws Java.CompileException {
            this.writeOpcode(Opcode.IASTORE + Java.ilfdabcs(this.getType()));
        }

        public final void visit(Visitor visitor) { visitor.visitArrayAccessExpression(this); }
    }

    /**
     * This class implements class or interface field access, and also the "array length"
     * expression "xy.length".
     */
    public static final class FieldAccessExpression extends Lvalue {
        public FieldAccessExpression(
            Location location,
            Atom     lhs,
            String   fieldName
        ) {
            super(location);
            this.lhs       = lhs;
            this.fieldName = fieldName;
        }

        final Atom   lhs;
        final String fieldName;

        // Compile time members:

        // Implement "Atom".
        public IClass getType() throws CompileException {
            this.determineValue();
            return this.value.getType();
        }
        public String toString() { return this.lhs.toString() + '.' + this.fieldName; }

        // Implement "Rvalue".
        public int compileContext() throws Java.CompileException {
            this.determineValue();
            return this.value.compileContext();
        }
        public IClass compileGet() throws Java.CompileException {
            this.determineValue();
            return this.value.compileGet();
        }

        // Implement Lvalue
        public void compileSet() throws Java.CompileException {
            this.determineValue();
            this.value.toLvalueOrCE().compileSet();
        }

        private void determineValue() throws Java.CompileException {
            if (this.value != null) return;

            IClass lhsType = this.lhs.getType();

            if (this.fieldName.equals("length") && lhsType.isArray()) {
                this.value = new ArrayLength(
                    this.getLocation(),
                    this.lhs.toRvalueOrCE()
                );
            } else {
                IField iField = Java.findIField(lhsType, this.fieldName, this.getLocation());
                if (iField == null) {
                    this.compileError("\"" + this.lhs.getType().toString() + "\" has no field \"" + this.fieldName + "\"");
                    this.value = new Rvalue(this.getLocation()) {
                        public IClass compileGet() throws CompileException { return Java.getIClassLoader().OBJECT; }
                        public IClass getType() throws CompileException { return Java.getIClassLoader().OBJECT; }
                        public String toString() { return "???"; }
                        public final void visit(Visitor visitor) {}
                    };
                    return;
                } 
                this.value = new FieldAccess(
                    this.getLocation(),
                    this.lhs,
                    iField
                );
            }
        }

        public final void visit(Visitor visitor) { visitor.visitFieldAccessExpression(this); }

        private Rvalue value = null;
    }

    /**
     * This class implements the unary operators "+", "-", "~" and "!".
     */
    public static final class UnaryOperation extends BooleanRvalue {
        public UnaryOperation(
            Location location,
            String   operator,
            Rvalue   operand
        ) {
            super(location);
            this.operator = operator;
            this.operand  = operand;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            if (this.operator == "!") return IClass.BOOLEAN;
            if (this.operator == "+") return this.operand.getType();
            if (
                this.operator == "-" ||
                this.operator == "~"
            ) return unaryNumericPromotionType(this, this.operand.getType());

            this.compileError("Unexpected operator \"" + this.operator + "\"");
            return IClass.BOOLEAN;
        }
        public String toString() { return this.operator + this.operand.toString(); }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            if (this.operator == "!") {
                return super.compileGet();
            }

            if (this.operator == "+") {
                return this.operand.compileGetValue();
            }

            if (this.operator == "-") {
                IClass operandType = this.operand.compileGetValue();

                IClass promotedType = unaryNumericPromotion((Located) this, operandType);
                this.writeOpcode(Opcode.INEG + Java.ilfd(promotedType));
                return promotedType;
            }

            if (this.operator == "~") {
                IClass operandType = this.operand.compileGetValue();

                IClass promotedType = unaryNumericPromotion((Located) this, operandType);
                if (promotedType == IClass.INT) {
                    this.writeOpcode(Opcode.ICONST_M1);
                    this.writeOpcode(Opcode.IXOR);
                    return IClass.INT;
                }
                if (promotedType == IClass.LONG) {
                    this.writeOpcode(Opcode.LDC2_W);
                    this.writeConstantLongInfo(-1L);
                    this.writeOpcode(Opcode.LXOR);
                    return IClass.LONG;
                }
                this.compileError("Operator \"~\" not applicable to type \"" + promotedType + "\"");
            }

            this.compileError("Unexpected operator \"" + this.operator + "\"");
            return Java.getIClassLoader().OBJECT;
        }

        // Implement Rvalue
        public void compileBoolean(
            CodeContext.Offset dst,        // Where to jump.
            boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
        ) throws CompileException {
            if (this.operator == "!") {
                this.operand.compileBoolean(dst, !orientation);
                return;
            }

            this.compileError("Boolean expression expected");
        }

        public Object getConstantValue2() throws CompileException {
            if (this.operator.equals("+")) return this.operand.getConstantValue();
            if (this.operator.equals("-")) return this.operand.getNegatedConstantValue();
            if (this.operator.equals("!")) {
                Object cv = this.operand.getConstantValue();
                return cv instanceof Boolean ? (
                    ((Boolean) cv).booleanValue() ? Boolean.FALSE : Boolean.TRUE
                ) : null;
            }
            return null;
        }
        public Object getNegatedConstantValue() throws CompileException {
            return (
                this.operator.equals("+") ? this.operand.getNegatedConstantValue() :
                this.operator.equals("-") ? this.operand.getConstantValue() :
                null
            );
        }

        public final void visit(Visitor visitor) { visitor.visitUnaryOperation(this); }

        final String operator;
        final Rvalue operand;
    }

    public static final class Instanceof extends Rvalue {
        public Instanceof(
            Location location,
            Rvalue   lhs,
            Type     rhs // ReferenceType or ArrayType
        ) {
            super(location);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        final Rvalue lhs;
        final Type   rhs;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return IClass.BOOLEAN;
        }
        public String toString() { return this.lhs.toString() + " instanceof " + this.rhs.toString(); }

        // Implement Rvalue
        public IClass compileGet() throws CompileException {
            IClass lhsType = this.lhs.compileGetValue();
            IClass rhsType = this.rhs.getType();

            if (rhsType.isAssignableFrom(lhsType)) {
                Java.pop((Located) this, lhsType);
                this.writeOpcode(Opcode.ICONST_1);
            } else
            if (
                lhsType.isInterface() ||
                rhsType.isInterface() ||
                lhsType.isAssignableFrom(rhsType)
            ) {
                this.writeOpcode(Opcode.INSTANCEOF);
                this.writeConstantClassInfo(rhsType.getDescriptor());
            } else {
                this.compileError("\"" + lhsType + "\" can never be an instance of \"" + rhsType + "\"");
            }
            return IClass.BOOLEAN;
        }

        public final void visit(Visitor visitor) { visitor.visitInstanceof(this); }
    }

    /**
     * Representation of all non-operand-modifying Java<sup>TM</sup> binary
     * operations.
     * <p>
     * Operations with boolean result:<br>
     * <tt>|| && == != < > <= >=</tt>
     * <p>
     * Operations with non-boolean result:<br>
     * <tt>| ^ & * / % + - << >> >>></tt>
     */
    public static final class BinaryOperation extends BooleanRvalue {
        public BinaryOperation(
            Location location,
            Rvalue   lhs,
            String   op,
            Rvalue   rhs
        ) {
            super(location);
            this.lhs = lhs;
            this.op = op;
            this.rhs = rhs;
        }

        final Rvalue lhs;
        final String op;
        final Rvalue rhs;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            if (
                this.op == "||" ||
                this.op == "&&" ||
                this.op == "==" ||
                this.op == "!=" ||
                this.op == "<"  ||
                this.op == ">"  ||
                this.op == "<=" ||
                this.op == ">="
            ) return IClass.BOOLEAN;

            if (
                this.op == "|" ||
                this.op == "^" ||
                this.op == "&"
            ) return (
                this.lhs.getType() == IClass.BOOLEAN ?
                IClass.BOOLEAN :
                Java.binaryNumericPromotionType(
                    (Locatable) this,
                    this.lhs.getType(),
                    this.rhs.getType()
                )
            );

            if (
                this.op == "*"   ||
                this.op == "/"   ||
                this.op == "%"   ||
                this.op == "+"   ||
                this.op == "-"
            ) {
                IClassLoader icl = Java.getIClassLoader();

                // Unroll the operands of this binary operation.
                Iterator ops = this.unrollLeftAssociation();

                // Check the far left operand type.
                IClass lhsType = ((Rvalue) ops.next()).getType();
                if (this.op == "+" && lhsType == icl.STRING) return icl.STRING;

                // Determine the expression type.
                do {
                    IClass rhsType = ((Rvalue) ops.next()).getType();
                    if (this.op == "+" && rhsType == icl.STRING) return icl.STRING;
                    lhsType = Java.binaryNumericPromotionType((Locatable) this, lhsType, rhsType);
                } while (ops.hasNext());
                return lhsType;
            }

            if (
                this.op == "<<"  ||
                this.op == ">>"  ||
                this.op == ">>>"
            ) {
                IClass lhsType = this.lhs.getType();
                return Java.unaryNumericPromotionType((Located) this, lhsType);
            }

            this.compileError("Unexpected operator \"" + this.op + "\"");
            return Java.getIClassLoader().OBJECT;
        }
        public String toString() {
            return this.lhs.toString() + ' ' + this.op + ' ' + this.rhs.toString();
        }

        public IClass compileGet() throws CompileException {
            if (
                this.op == "||" ||
                this.op == "&&" ||
                this.op == "==" ||
                this.op == "!=" ||
                this.op == "<"  ||
                this.op == ">"  ||
                this.op == "<=" ||
                this.op == ">="
            ) {
                // Eventually calls "compileBoolean()".
                return super.compileGet();
            }

            // Implements "| ^ & * / % + - << >> >>>".
            return Java.compileArithmeticOperation(
                (Located) this,        // located
                null,                  // type
                this.unrollLeftAssociation(), // operands
                this.op                // operator
            );
        }

        // Implement Rvalue
        public void compileBoolean(
            CodeContext.Offset dst,        // Where to jump.
            boolean            orientation // JUMP_IF_TRUE or JUMP_IF_FALSE.
        ) throws CompileException {

            // Constant expression?
            {
                Object cv = this.getConstantValue();
                if (cv instanceof Boolean) {
                    if (orientation == Rvalue.JUMP_IF_FALSE ^ ((Boolean) cv).booleanValue()) {
                        this.writeBranch(Opcode.GOTO, dst);
                    }
                    return;
                }
            }

            if (this.op == "|" || this.op == "^" || this.op == "&") {
                super.compileBoolean(dst, orientation);
                return;
            }

            if (this.op == "||" || this.op == "&&") {
                if (this.op == "||" ^ orientation == Rvalue.JUMP_IF_FALSE) {
                    this.lhs.compileBoolean(dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
                    this.rhs.compileBoolean(dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
                } else {
                    CodeContext.Offset end = this.newUnsetOffset();
                    this.lhs.compileBoolean(end, Rvalue.JUMP_IF_FALSE ^ orientation == Rvalue.JUMP_IF_FALSE);
                    this.rhs.compileBoolean(dst, Rvalue.JUMP_IF_TRUE ^ orientation == Rvalue.JUMP_IF_FALSE);
                    end.set();
                }
                return;
            }

            if (
                this.op == "==" ||
                this.op == "!=" ||
                this.op == "<=" ||
                this.op == ">=" ||
                this.op == "<"  ||
                this.op == ">"
            ) {
                int opIdx = (
                    this.op == "==" ? 0 :
                    this.op == "!=" ? 1 :
                    this.op == "<"  ? 2 :
                    this.op == ">=" ? 3 :
                    this.op == ">"  ? 4 :
                    this.op == "<=" ? 5 : Integer.MIN_VALUE
                );
                if (orientation == Rvalue.JUMP_IF_FALSE) opIdx ^= 1;

                // Comparison with "null".
                {
                    boolean lhsIsNull = this.lhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL;
                    boolean rhsIsNull = this.rhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL;

                    if (lhsIsNull || rhsIsNull) {
                        if (this.op != "==" && this.op != "!=") this.compileError("Operator \"" + this.op + "\" not allowed on operand \"null\"");

                        // null == x
                        // x == null
                        IClass ohsType = (lhsIsNull ? this.rhs : this.lhs).compileGetValue();
                        if (ohsType.isPrimitive()) this.compileError("Cannot compare \"null\" with primitive type \"" + ohsType.toString() + "\"");
                        this.writeBranch(Opcode.IFNULL + opIdx, dst);
                        return;
                    }
                }

                IClass lhsType = this.lhs.compileGetValue();
                CodeContext.Inserter convertLhsInserter = this.newInserter();
                IClass rhsType = this.rhs.compileGetValue();

                // 15.20.1 Numerical comparison.
                if (
                    lhsType.isPrimitiveNumeric() &&
                    rhsType.isPrimitiveNumeric()
                ) {
                    IClass promotedType = binaryNumericPromotion((Located) this, lhsType, convertLhsInserter, rhsType);
                    if (promotedType == IClass.INT) {
                        this.writeBranch(Opcode.IF_ICMPEQ + opIdx, dst);
                    } else
                    if (promotedType == IClass.LONG) {
                        this.writeOpcode(Opcode.LCMP);
                        this.writeBranch(Opcode.IFEQ + opIdx, dst);
                    } else
                    if (promotedType == IClass.FLOAT) {
                        this.writeOpcode(Opcode.FCMPG);
                        this.writeBranch(Opcode.IFEQ + opIdx, dst);
                    } else
                    if (promotedType == IClass.DOUBLE) {
                        this.writeOpcode(Opcode.DCMPG);
                        this.writeBranch(Opcode.IFEQ + opIdx, dst);
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
                    if (this.op != "==" && this.op != "!=") this.compileError("Operator \"" + this.op + "\" not allowed on boolean operands");
                    this.writeBranch(Opcode.IF_ICMPEQ + opIdx, dst);
                    return;
                }

                // Reference comparison.
                // Note: Comparison with "null" is already handled above.
                if (
                    !lhsType.isPrimitive() &&
                    !rhsType.isPrimitive()
                ) {
                    if (this.op != "==" && this.op != "!=") this.compileError("Operator \"" + this.op + "\" not allowed on reference operands");
                    this.writeBranch(Opcode.IF_ACMPEQ + opIdx, dst);
                    return;
                }

                this.compileError("Cannot compare types \"" + lhsType + "\" and \"" + rhsType + "\"");
            }

            this.compileError("Boolean expression expected");
        }
        public Object getConstantValue2() throws CompileException {

            // null == null
            // null != null
            if (
                (this.op == "==" || this.op == "!=") &&
                this.lhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL &&
                this.rhs.getConstantValue() == Rvalue.CONSTANT_VALUE_NULL
            ) return new Boolean(this.op == "==");

            // "|", "^", "&", "*", "/", "%", "+", "-".
            if (
                this.op == "|" ||
                this.op == "^" ||
                this.op == "&" ||
                this.op == "*" ||
                this.op == "/" ||
                this.op == "%" ||
                this.op == "+" ||
                this.op == "-"
            ) {

                // Unroll the constant operands.
                List cvs = new ArrayList();
                for (Iterator it = this.unrollLeftAssociation(); it.hasNext();) {
                    Object cv = ((Rvalue) it.next()).getConstantValue();
                    if (cv == null) return null;
                    cvs.add(cv);
                }

                // Compute the constant value of the unrolled binary operation.
                Iterator it = cvs.iterator();
                Object lhs = it.next();
                while (it.hasNext()) {
                    Object rhs = it.next();

                    // String concatenation?
                    if (this.op == "+" && (lhs instanceof String || rhs instanceof String)) {
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
                        lhs = new Double(
                            this.op == "|" ? -1 :
                            this.op == "^" ? -1 :
                            this.op == "&" ? -1 :
                            this.op == "*" ? lhsD * rhsD :
                            this.op == "/" ? lhsD / rhsD :
                            this.op == "%" ? lhsD % rhsD :
                            this.op == "+" ? lhsD + rhsD :
                            this.op == "-" ? lhsD - rhsD :
                            -1
                        );
                    } else
                    if (lhs instanceof Float || rhs instanceof Float) {
                        float lhsF = ((Number) lhs).floatValue();
                        float rhsF = ((Number) rhs).floatValue();
                        lhs = new Float(
                            this.op == "|" ? -1 :
                            this.op == "^" ? -1 :
                            this.op == "&" ? -1 :
                            this.op == "*" ? lhsF * rhsF :
                            this.op == "/" ? lhsF / rhsF :
                            this.op == "%" ? lhsF % rhsF :
                            this.op == "+" ? lhsF + rhsF :
                            this.op == "-" ? lhsF - rhsF :
                            -1
                        );
                    } else
                    if (lhs instanceof Long || rhs instanceof Long) {
                        long lhsL = ((Number) lhs).longValue();
                        long rhsL = ((Number) rhs).longValue();
                        lhs = new Long(
                            this.op == "|" ? lhsL | rhsL :
                            this.op == "^" ? lhsL ^ rhsL :
                            this.op == "&" ? lhsL & rhsL :
                            this.op == "*" ? lhsL * rhsL :
                            this.op == "/" ? lhsL / rhsL :
                            this.op == "%" ? lhsL % rhsL :
                            this.op == "+" ? lhsL + rhsL :
                            this.op == "-" ? lhsL - rhsL :
                            -1
                        );
                    } else
                    {
                        int lhsI = ((Number) lhs).intValue();
                        int rhsI = ((Number) rhs).intValue();
                        lhs = new Integer(
                            this.op == "|" ? lhsI | rhsI :
                            this.op == "^" ? lhsI ^ rhsI :
                            this.op == "&" ? lhsI & rhsI :
                            this.op == "*" ? lhsI * rhsI :
                            this.op == "/" ? lhsI / rhsI :
                            this.op == "%" ? lhsI % rhsI :
                            this.op == "+" ? lhsI + rhsI :
                            this.op == "-" ? lhsI - rhsI :
                            -1
                        );
                    }
                }
                return lhs;
            }

            // "&&" and "||" with constant LHS or RHS operand.
            if (
                this.op == "&&" ||
                this.op == "||"
            ) {
                Object lhsValue = this.lhs.getConstantValue();
                Object rhsValue = this.rhs.getConstantValue();
                if (lhsValue instanceof Boolean) {
                    boolean lhsBV = ((Boolean) lhsValue).booleanValue();
                    return (
                        this.op == "&&" ?
                        (lhsBV ? rhsValue : Boolean.FALSE) :
                        (lhsBV ? Boolean.TRUE : rhsValue)
                    );
                }
                if (rhsValue instanceof Boolean) {
                    boolean rhsBV = ((Boolean) rhsValue).booleanValue();
                    return (
                        this.op == "&&" ?
                        (rhsBV ? lhsValue : Boolean.FALSE) :
                        (rhsBV ? Boolean.TRUE : lhsValue)
                    );
                }
            }

            return null;
        }

        /**
         * Returns an {@link Iterator} over a left-to-right sequence of {@link Java.Rvalue}s.
         */
        public Iterator unrollLeftAssociation() {
            List operands = new ArrayList();
            BinaryOperation x = this;
            for (;;) {
                operands.add(x.rhs);
                Rvalue lhs = x.lhs;
                if (
                    lhs instanceof BinaryOperation &&
                    ((BinaryOperation) lhs).op == this.op
                ) {
                    x = (BinaryOperation) lhs;
                } else {
                    operands.add(lhs);
                    break;
                }
            }
            return new ReverseListIterator(operands.listIterator(operands.size()));
        }

        public final void visit(Visitor visitor) { visitor.visitBinaryOperation(this); }
    }

    /**
     * The LHS operand of type <code>lhsType</code> is expected on the stack.
     * <p>
     * The following operators are supported:
     * <code>&nbsp;&nbsp;| ^ & * / % + - &lt;&lt; &gt;&gt; &gt;&gt;&gt;</code>
     */
    private static IClass compileArithmeticBinaryOperation(
        Located  located,
        IClass   lhsType,
        String   operator,
        Rvalue   rhs
    ) throws CompileException {
        return Java.compileArithmeticOperation(
            located,
            lhsType,
            Arrays.asList(new Rvalue[] { rhs }).iterator(),
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
    private static IClass compileArithmeticOperation(
        Located  located,
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
                Rvalue operand = (Rvalue) operands.next(); 

                if (type == null) {
                    type = operand.compileGetValue();
                } else {
                    CodeContext.Inserter convertLhsInserter = located.newInserter();
                    IClass rhsType = operand.compileGetValue();

                    if (
                        type.isPrimitiveNumeric() &&
                        rhsType.isPrimitiveNumeric()
                    ) {
                        IClass promotedType = Java.binaryNumericPromotion(located, type, convertLhsInserter, rhsType);
                        if (promotedType == IClass.INT) {
                            located.writeOpcode(iopcode);
                        } else
                        if (promotedType == IClass.LONG) {
                            located.writeOpcode(iopcode + 1);
                        } else
                        {
                            located.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"");
                        }
                        type = promotedType;
                    } else
                    if (
                        type == IClass.BOOLEAN &&
                        rhsType == IClass.BOOLEAN
                    ) {
                        located.writeOpcode(iopcode);
                        type = IClass.BOOLEAN;
                    } else
                    {
                        located.compileError("Operator \"" + operator + "\" not defined on types \"" + type + "\" and \"" + rhsType + "\"");
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
                Rvalue operand = (Rvalue) operands.next(); 

                IClass operandType = operand.getType();
                IClassLoader icl = Java.getIClassLoader();
    
                // String concatenation?
                if (operator == "+" && (type == icl.STRING || operandType == icl.STRING)) {

                    if (type != null) Java.stringConversion(located, type);

                    do {
                        Object cv = operand.getConstantValue();
                        if (cv == null) {
                            Java.stringConversion(located, operand.compileGetValue());
                            operand = operands.hasNext() ? (Rvalue) operands.next() : null;
                        } else {
                            if (operands.hasNext()) {
                                operand = (Rvalue) operands.next();
                                Object cv2 = operand.getConstantValue();
                                if (cv2 != null) {
                                    StringBuffer sb = new StringBuffer(cv.toString()).append(cv2);
                                    for (;;) {
                                        if (!operands.hasNext()) {
                                            operand = null;
                                            break;
                                        }
                                        operand = (Rvalue) operands.next();
                                        Object cv3 = operand.getConstantValue();
                                        if (cv3 == null) break;
                                        sb.append(cv3);
                                    }
                                    cv = sb.toString();
                                }
                            } else {
                                operand = null;
                            }
                            Java.pushConstant(located, cv.toString());
                        }

                        // Concatenate.
                        if (type != null) {
                            located.writeOpcode(Opcode.INVOKEVIRTUAL);
                            located.writeConstantMethodrefInfo(
                                Descriptor.STRING,                                // classFD
                                "concat",                                         // methodName
                                "(" + Descriptor.STRING + ")" + Descriptor.STRING // methodMD
                            );
                        }
                        type = Java.getIClassLoader().STRING;
                    } while (operand != null);
                    return type;
                }

                if (type == null) {
                    type = operand.compileGetValue();
                } else {
                    CodeContext.Inserter convertLhsInserter = located.newInserter();
                    IClass rhsType = operand.compileGetValue();

                    type = Java.binaryNumericPromotion(located, type, convertLhsInserter, rhsType);

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
                        located.compileError("Unexpected promoted type \"" + type + "\"");
                        opcode = iopcode;
                    }
                    located.writeOpcode(opcode);
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
                Rvalue operand = (Rvalue) operands.next(); 

                if (type == null) {
                    type = operand.compileGetValue();
                } else {
                    CodeContext.Inserter convertLhsInserter = located.newInserter();
                    IClass rhsType = operand.compileGetValue();

                    IClass promotedLhsType;
                    located.pushInserter(convertLhsInserter);
                    try {
                        promotedLhsType = Java.unaryNumericPromotion(located, type);
                    } finally {
                        located.popInserter();
                    }
                    if (promotedLhsType != IClass.INT && promotedLhsType != IClass.LONG) located.compileError("Shift operation not allowed on operand type \"" + type + "\"");

                    IClass promotedRhsType = Java.unaryNumericPromotion(located, rhsType);
                    if (promotedRhsType != IClass.INT && promotedRhsType != IClass.LONG) located.compileError("Shift distance of type \"" + rhsType + "\" is not allowed");

                    if (promotedRhsType == IClass.LONG) located.writeOpcode(Opcode.L2I);

                    located.writeOpcode(promotedLhsType == IClass.LONG ? iopcode + 1 : iopcode);
                    type = promotedLhsType;
                }
            } while (operands.hasNext());
            return type;
        }

        throw new RuntimeException("Unexpected operator \"" + operator + "\"");
    }

    public static final class Cast extends Rvalue {
        public Cast(
            Location location,
            Type     targetType,
            Rvalue   value
        ) {
            super(location);
            this.targetType = targetType;
            this.value      = value;
        }

        final Type   targetType;
        final Rvalue value;

        // Compile time members.

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.targetType.getType();
        }
        public String toString() { return '(' + this.targetType.toString() + ") " + this.value.toString(); }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            IClass tt = this.targetType.getType();
            IClass vt = this.value.compileGetValue();
            if (
                !Java.tryIdentityConversion(vt, tt) &&
                !Java.tryWideningPrimitiveConversion((Located) this, vt, tt) &&
                !Java.tryNarrowingPrimitiveConversion((Located) this, vt, tt) &&
                !Java.isWideningReferenceConvertible(vt, tt) &&
                !Java.tryNarrowingReferenceConversion((Located) this, vt, tt)
            ) this.compileError("Cannot cast \"" + vt + "\" to \"" + tt + "\"");
            return tt;
        }
        public Object getConstantValue2() throws CompileException {
            Object cv = this.value.getConstantValue();
            if (cv == null) return null;

            if (cv instanceof Number) {
                IClass tt = this.targetType.getType();
                if (tt == IClass.BYTE  ) return new Byte(((Number) cv).byteValue());
                if (tt == IClass.SHORT ) return new Short(((Number) cv).shortValue());
                if (tt == IClass.INT   ) return new Integer(((Number) cv).intValue());
                if (tt == IClass.LONG  ) return new Long(((Number) cv).longValue());
                if (tt == IClass.FLOAT ) return new Float(((Number) cv).floatValue());
                if (tt == IClass.DOUBLE) return new Double(((Number) cv).doubleValue());
            }

            return null;
        }

        public final void visit(Visitor visitor) { visitor.visitCast(this); }
    }

    public final static class ParenthesizedExpression extends Lvalue {
        final Rvalue value;

        public ParenthesizedExpression(Location location, Rvalue value) {
            super(location);
            this.value = value;
        }
        public void compileSet() throws CompileException {
            this.value.toLvalueOrCE().compileSet();
        }
        public IClass compileGet() throws CompileException {
            return this.value.compileGet();
        }
        public IClass getType() throws CompileException {
            return this.value.getType();
        }
        public void compile() throws CompileException {
            this.value.compile();
        }
        public void compileBoolean(CodeContext.Offset dst, boolean orientation) throws CompileException {
            this.value.compileBoolean(dst, orientation);
        }
        public int compileContext() throws CompileException {
            return this.value.compileContext();
        }
        public IClass compileGetValue() throws CompileException {
            return this.value.compileGetValue();
        }
        public Object getConstantValue2() throws CompileException {
            return this.value.getConstantValue();
        }
        public Object getNegatedConstantValue() throws CompileException {
            return this.value.getNegatedConstantValue();
        }

        public String toString() {
            return '(' + this.value.toString() + ')';
        }
        public void visit(Visitor visitor) {
            visitor.visitParenthesizedExpression(this);
        }
    }

    public static abstract class ConstructorInvocation extends Atom {
        protected final ClassDeclaration      declaringClass;
        protected final ConstructorDeclarator declaringConstructor;
        protected final Rvalue[]              arguments;
    
        protected ConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue[]              arguments
        ) {
            super(location);
            this.declaringClass        = declaringClass;
            this.declaringConstructor  = declaringConstructor;
            this.arguments             = arguments;
        }
    
        public abstract void compile() throws CompileException;
    
        // Implement Atom:
        public IClass getType() { throw new RuntimeException(); }
    }
    
    public final static class AlternateConstructorInvocation extends ConstructorInvocation {
        public AlternateConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue[]              arguments
        ) {
            super(location, declaringClass, declaringConstructor, arguments);
        }

        // Implement Atom
        public String toString() { return "this()"; }

        public void compile() throws CompileException {
            this.writeOpcode(Opcode.ALOAD_0);
            Java.invokeConstructor(
                (Located) this,                    // located
                (Scope) this.declaringConstructor, // scope
                (Rvalue) null,                     // optionalEnclosingInstance
                this.declaringClass,               // targetClass
                this.arguments                     // arguments
            );
        }

        public void visit(Visitor visitor) { visitor.visitAlternateConstructorInvocation(this); }
    }
    
    public final static class SuperConstructorInvocation extends ConstructorInvocation {
        final Rvalue optionalQualification;
    
        public SuperConstructorInvocation(
            Location              location,
            ClassDeclaration      declaringClass,
            ConstructorDeclarator declaringConstructor,
            Rvalue                optionalQualification,
            Rvalue[]              arguments
        ) {
            super(location, declaringClass, declaringConstructor, arguments);
            this.optionalQualification = optionalQualification;
        }
    
        // Implement Atom
        public String toString() { return "super()"; }

        public void compile() throws CompileException {
            this.writeOpcode(Opcode.ALOAD_0);

            IClass superclass = this.declaringClass.getSuperclass();

            Rvalue optionalEnclosingInstance;
            if (this.optionalQualification != null) {
                optionalEnclosingInstance = this.optionalQualification;
            } else {
                IClass outerIClassOfSuperclass = superclass.getOuterIClass();
                if (outerIClassOfSuperclass == null) {
                    optionalEnclosingInstance = null;
                } else {
                    optionalEnclosingInstance = new QualifiedThisReference(
                        this.getLocation(),        // location
                        this.declaringClass,       // declaringClass
                        this.declaringConstructor, // optionalFunctionDeclarator
                        outerIClassOfSuperclass    // targetClass
                    );
                }
            }
            Java.invokeConstructor(
                (Located) this,                    // located
                (Scope) this.declaringConstructor, // scope
                optionalEnclosingInstance,         // optionalEnclosingInstance
                superclass,                        // targetClass
                this.arguments                     // arguments
            );
        }

        public void visit(Visitor visitor) { visitor.visitSuperConstructorInvocation(this); }
    }

    /**
     * Expects the object to initialize on the stack.
     * <p>
     * Notice: This method is used both for explicit constructor invocation (first statement of
     * a constructor body) and implicit constructor invocation (right after NEW).
     * @param optionalEnclosingInstance Used if the target class is an inner class
     */
    private static void invokeConstructor(
        Located  located,
        Scope    scope,
        Rvalue   optionalEnclosingInstance,
        IClass   targetClass,
        Rvalue[] arguments
    ) throws CompileException {

        // Find constructors.
        IClass.IConstructor[] iConstructors = targetClass.getDeclaredIConstructors();
        if (iConstructors.length == 0) throw new RuntimeException();

        IClass.IConstructor iConstructor = (IClass.IConstructor) Java.findMostSpecificIInvocable(
            located,
            iConstructors, // iInvocables
            arguments      // arguments
        );

        // Check exceptions that the constructor may throw.
        IClass[] thrownExceptions = iConstructor.getThrownExceptions();
        for (int i = 0; i < thrownExceptions.length; ++i) {
            Java.checkThrownException(
                located,
                thrownExceptions[i],
                scope
            );
        }

        // Pass enclosing instance as a synthetic parameter.
        IClass outerIClass = targetClass.getOuterIClass();
        if (outerIClass != null) {
            if (optionalEnclosingInstance == null) located.compileError("Enclosing instance for initialization of inner class \"" + targetClass + "\" missing");
            IClass eiic = optionalEnclosingInstance.compileGetValue();
            if (!outerIClass.isAssignableFrom(eiic)) located.compileError("Type of enclosing instance (\"" + eiic + "\") is not assignable to \"" + outerIClass + "\"");
        }

        // Pass local variables to constructor as synthetic parameters.
        if (targetClass instanceof ClassDeclaration) {
            ClassDeclaration cd = (ClassDeclaration) targetClass;

            // Determine enclosing function declarator and type declaration.
            TypeBodyDeclaration scopeTBD;
            TypeDeclaration     scopeTypeDeclaration;
            {
                Scope s = scope;
                for (; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
                scopeTBD             = (TypeBodyDeclaration) s;
                scopeTypeDeclaration = scopeTBD.getDeclaringType();
            }

            if (!(scopeTypeDeclaration instanceof ClassDeclaration)) {
                if (!cd.syntheticFields.isEmpty()) throw new RuntimeException();
            } else {
                ClassDeclaration scopeClassDeclaration = (ClassDeclaration) scopeTypeDeclaration;
                for (Iterator it = cd.syntheticFields.values().iterator(); it.hasNext();) {
                    IClass.IField sf = (IClass.IField) it.next();
                    if (!sf.getName().startsWith("val$")) continue;
                    IClass.IField eisf = (IClass.IField) scopeClassDeclaration.syntheticFields.get(sf.getName());
                    if (eisf != null) {
                        if (scopeTBD instanceof MethodDeclarator) {
                            Java.load(located, scopeClassDeclaration, 0);
                            located.writeOpcode(Opcode.GETFIELD);
                            located.writeConstantFieldrefInfo(
                                scopeClassDeclaration.getDescriptor(), // classFD
                                sf.getName(),                          // fieldName
                                sf.getDescriptor()                     // fieldFD
                            );
                        } else
                        if (scopeTBD instanceof ConstructorDeclarator) {
                            ConstructorDeclarator constructorDeclarator = (ConstructorDeclarator) scopeTBD;
                            LocalVariable syntheticParameter = (LocalVariable) constructorDeclarator.syntheticParameters.get(sf.getName());
                            if (syntheticParameter == null) {
                                located.compileError("Compiler limitation: Constructor cannot access local variable \"" + sf.getName().substring(4) + "\" declared in an enclosing block because none of the methods accesses it. As a workaround, declare a dummy method that accesses the local variable.");
                                located.writeOpcode(Opcode.ACONST_NULL);
                            } else {
                                Java.load(located, syntheticParameter);
                            }
                        } else {
                            located.compileError("Compiler limitation: Initializers cannot access local variables declared in an enclosing block.");
                            located.writeOpcode(Opcode.ACONST_NULL);
                        }
                    } else {
                        String localVariableName = sf.getName().substring(4);
                        LocalVariable lv = null;
                        Scope s;
                        for (s = scope; s instanceof Block; s = s.getEnclosingScope()) {

                            // Local variable?
                            lv = (LocalVariable) ((Block) s).localVariables.get(localVariableName);
                            if (lv != null) break;
                        }
                        if (lv == null) {
                            while (!(s instanceof FunctionDeclarator)) s = s.getEnclosingScope();
                            FunctionDeclarator fd = (FunctionDeclarator) s;

                            // Function parameter?
                            lv = (LocalVariable) fd.parameters.get(localVariableName);
                        }
                        if (lv == null) throw new RuntimeException("SNO: Synthetic field \"" + sf.getName() + "\" neither maps a synthetic field of an enclosing instance nor a local variable");
                        Java.load(located, lv);
                    }
                }
            }
        }

        // Evaluate constructor arguments.
        IClass[] parameterTypes = iConstructor.getParameterTypes();
        for (int i = 0; i < arguments.length; ++i) {
            Java.assignmentConversion(
                (Located) located,              // located
                arguments[i].compileGetValue(), // sourceType
                parameterTypes[i],              // targetType
                arguments[i].getConstantValue() // optionalConstantValue
            );
        }

        // Invoke!
        // Notice that the method descriptor is "iConstructor.getDescriptor()" prepended with the
        // synthetic parameters.
        located.writeOpcode(Opcode.INVOKESPECIAL);
        located.writeConstantMethodrefInfo(
            targetClass.getDescriptor(), // classFD
            "<init>",                    // methodName
            iConstructor.getDescriptor() // methodMD
        );
    }

    public static final class MethodInvocation extends Invocation {
        public MethodInvocation(
            Location location,
            Scope    enclosingScope,
            Atom     optionalTarget,
            String   methodName,
            Rvalue[] arguments
        ) {
            super(location, enclosingScope, arguments);
            this.optionalTarget = optionalTarget;
            this.methodName     = methodName;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.findIMethod().getReturnType();
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalTarget != null) sb.append(this.optionalTarget.toString()).append('.');
            sb.append(this.methodName).append('(');
            for (int i = 0; i < this.arguments.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append(this.arguments[i].toString());
            }
            sb.append(')');
            return sb.toString();
        }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            IClass.IMethod iMethod = this.findIMethod();

            if (this.optionalTarget == null) {

                // JLS2 6.5.7.1, 15.12.4.1.1.1
                Scope s;
                for (s = this.scope; !(s instanceof TypeBodyDeclaration); s = s.getEnclosingScope());
                TypeBodyDeclaration scopeTBD = (TypeBodyDeclaration) s;
                if (iMethod.isStatic()) {
                    // JLS2 15.12.4.1.1.1.1
                    ;
                } else {
                    // JLS2 15.12.4.1.1.1.2
                    if (scopeTBD.isStatic()) this.compileError("Instance method \"" + this.methodName + "\" cannot be invoked in static context");
                    Java.referenceThis((Located) this);
                }
            } else {

                // 6.5.7.2
                boolean staticContext = this.optionalTarget.isType();
                if (!staticContext) {
                    Rvalue targetValue = this.optionalTarget.toRvalueOrCE();

                    // TODO: Wrapper methods for private methods of enclosing / enclosed types.
                    if (
                        targetValue.getType() != iMethod.getDeclaringIClass() &&
                        iMethod.getAccess() == IClass.PRIVATE
                    ) this.compileError("Invocation of private methods of enclosing or enclosed type NYI; please change the access of method \"" + iMethod.getName() + "()\" from \"private\" to \"/*private*/\"");

                    targetValue.compileGetValue();
                }
                if (iMethod.isStatic()) {
                    if (!staticContext) {
                        // JLS2 15.12.4.1.2.1
                        Java.pop((Located) this.optionalTarget, this.optionalTarget.getType());
                    }
                } else {
                    if (staticContext) this.compileError("Instance method \"" + this.methodName + "\" cannot be invoked in static context");
                }
            }

            // Evaluate method parameters.
            IClass[] parameterTypes = iMethod.getParameterTypes();
            for (int i = 0; i < this.arguments.length; ++i) {
                Java.assignmentConversion(
                    (Located) this,                      // located
                    this.arguments[i].compileGetValue(), // sourceType
                    parameterTypes[i],                   // targetType
                    this.arguments[i].getConstantValue() // optionalConstantValue
                );
            }

            // Invoke!
            if (iMethod.getDeclaringIClass().isInterface()) {
                this.writeOpcode(Opcode.INVOKEINTERFACE);
                this.writeConstantInterfaceMethodrefInfo(
                    iMethod.getDeclaringIClass().getDescriptor(), // classFD
                    iMethod.getName(),                            // methodName
                    iMethod.getDescriptor()                       // methodMD
                );
                IClass[] pts = iMethod.getParameterTypes();
                int count = 1;
                for (int i = 0; i < pts.length; ++i) count += Descriptor.size(pts[i].getDescriptor());
                this.writeByte(count);
                this.writeByte(0);
            } else {
                this.writeOpcode(
                    iMethod.isStatic()                    ? Opcode.INVOKESTATIC :
                    iMethod.getAccess() == IClass.PRIVATE ? Opcode.INVOKESPECIAL :
                    Opcode.INVOKEVIRTUAL
                );
                this.writeConstantMethodrefInfo(
                    iMethod.getDeclaringIClass().getDescriptor(), // classFD
                    iMethod.getName(),                            // methodName
                    iMethod.getDescriptor()                       // methodMD
                );
            }
            return iMethod.getReturnType();
        }

        /**
         * Find named methods of "targetType", examine the argument types and choose the
         * most specific method. Check that only the allowed exceptions are thrown.
         * <p>
         * Notice that the returned {@link IMethod} may be declared in an enclosing type.
         *
         * @return The selected {@link IMethod} or <code>null</code>
         */
        private IClass.IMethod findIMethod() throws CompileException {
            for (Scope s = this.scope; !(s instanceof CompilationUnit); s = s.getEnclosingScope()) {
                if (s instanceof TypeDeclaration) {
                    TypeDeclaration td = (TypeDeclaration) s;
        
                    // Find methods with specified name.
                    IClass.IMethod iMethod = Java.findIMethod(
                        (Located) this,               // located
                        (                             // targetType
                            this.optionalTarget == null ?
                            (IClass) td :
                            this.optionalTarget.getType()
                        ),
                        this.methodName,              // methodName
                        this.arguments                // arguments
                    );
        
                    // Check exceptions that the method may throw.
                    IClass[] thrownExceptions = iMethod.getThrownExceptions();
                    for (int i = 0; i < thrownExceptions.length; ++i) {
                        Java.checkThrownException(
                            (Located) this,      // located
                            thrownExceptions[i], // type
                            this.scope           // scope
                        );
                    }
        
                    return iMethod;
                }
            }
            return null;
        }

        public final void visit(Visitor visitor) { visitor.visitMethodInvocation(this); }

        final Atom   optionalTarget; // null == simple method name.
        final String methodName;
    }

    public static final class SuperclassMethodInvocation extends Invocation {
        public SuperclassMethodInvocation(
            Location location,
            Scope    enclosingScope,
            String   methodName,
            Rvalue[] arguments
        ) {
            super(location, enclosingScope, arguments);
            this.methodName = methodName;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.findIMethod().getReturnType();
        }
        public String toString() { return "super." + this.methodName + "()"; }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            IClass.IMethod iMethod = this.findIMethod();

            Scope s;
            for (s = this.scope; s instanceof Statement; s = s.getEnclosingScope());
            FunctionDeclarator fd = s instanceof FunctionDeclarator ? (FunctionDeclarator) s : null;
            if (fd == null) {
                this.compileError("Cannot invoke superclass method in non-method scope");
                return IClass.INT;
            } 
            if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Cannot invoke superclass method in static context");
            Java.load((Located) this, (IClass) fd.getDeclaringType(), 0);

            // Evaluate method parameters.
            IClass[] parameterTypes = iMethod.getParameterTypes();
            for (int i = 0; i < this.arguments.length; ++i) {
                Java.assignmentConversion(
                    (Located) this,                      // located
                    this.arguments[i].compileGetValue(), // sourceType
                    parameterTypes[i],                   // targetType
                    this.arguments[i].getConstantValue() // optionalConstantValue
                );
            }

            // Invoke!
            this.writeOpcode(Opcode.INVOKESPECIAL);
            this.writeConstantMethodrefInfo(
                iMethod.getDeclaringIClass().getDescriptor(), // classFD
                this.methodName,                              // methodName
                iMethod.getDescriptor()                       // methodMD
            );
            return iMethod.getReturnType();
        }

        private IClass.IMethod findIMethod() throws CompileException {
            ClassDeclaration declaringClass;
            for (Scope s = this.scope;; s = s.getEnclosingScope()) {
                if (s instanceof FunctionDeclarator) {
                    FunctionDeclarator fd = (FunctionDeclarator) s;
                    if ((fd.modifiers & Mod.STATIC) != 0) this.compileError("Superclass method cannot be invoked in static context");
                }
                if (s instanceof ClassDeclaration) {
                    declaringClass = (ClassDeclaration) s;
                    break;
                }
            }
            return Java.findIMethod(
                (Located) this,                 // located
                declaringClass.getSuperclass(), // targetType
                this.methodName,                // methodName
                this.arguments                  // arguments
            );
        }

        public final void visit(Visitor visitor) { visitor.visitSuperclassMethodInvocation(this); }

        final String methodName;
    }

    public static abstract class Invocation extends Rvalue {
        protected Invocation(
            Location location,
            Scope    scope,
            Rvalue[] arguments
        ) {
            super(location);
            this.scope     = scope;
            this.arguments = arguments;
        }

        protected final Scope    scope;
        protected final Rvalue[] arguments;
    }

    /**
     * Find {@link IMethod} by name and argument types. If more than one such
     * method exists, choose the most specific one (JLS 15.11.2).
     * <p>
     * Notice that the returned {@link IMethod} may be declared in an enclosing type.
     */
    private static IClass.IMethod findIMethod(
        Located      located,
        IClass       targetType,
        final String methodName,
        Rvalue[]     arguments
    ) throws CompileException {
        for (IClass ic = targetType; ic != null; ic = ic.getDeclaringIClass()) {
            List l = new ArrayList();
            Java.getIMethods(ic, methodName, l);
            if (l.size() > 0) {

                // Determine arguments' types, choose the most specific method
                IClass.IMethod iMethod = (IClass.IMethod) Java.findMostSpecificIInvocable(
                    located,
                    (IClass.IMethod[]) l.toArray(new IClass.IMethod[l.size()]), // iInvocables
                    arguments         // arguments
                );
                return iMethod;
            }
        }
        located.compileError("Class \"" + targetType + "\" has no method named \"" + methodName + "\"");
        return targetType.new IMethod() {
            public String   getName()                                     { return methodName; }
            public IClass   getReturnType() throws CompileException       { return IClass.INT; }
            public boolean  isStatic()                                    { return false; }
            public boolean  isAbstract()                                  { return false; }
            public IClass[] getParameterTypes() throws CompileException   { return new IClass[0]; }
            public IClass[] getThrownExceptions() throws CompileException { return new IClass[0]; }
            public int      getAccess()                                   { return IClass.PUBLIC; }
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
    private static void getIMethods(
        IClass type,
        String methodName,
        List   v // IMethod
    ) throws CompileException {

        // Check methods declared by this type.
        IClass.IMethod[] ims = type.getDeclaredIMethods();
        for (int i = 0; i < ims.length; ++i) {
            IClass.IMethod im = ims[i];
            if (im.getName().equals(methodName)) v.add(im);
        }

        // Check superclass.
        IClass superclass = type.getSuperclass();
        if (superclass != null) Java.getIMethods(superclass, methodName, v);

        // Check superinterfaces.
        IClass[] interfaces = type.getInterfaces();
        for (int i = 0; i < interfaces.length; ++i) Java.getIMethods(interfaces[i], methodName, v);

        // JLS2 6.4.3
        if (superclass == null && interfaces.length == 0 && type.isInterface()) {
            IClass.IMethod[] oms = Java.getIClassLoader().OBJECT.getDeclaredIMethods();
            for (int i = 0; i < oms.length; ++i) {
                IClass.IMethod om = oms[i];
                if (
                    om.getName().equals(methodName)
                    && !om.isStatic()
                    && om.getAccess() == IClass.PUBLIC
                ) v.add(om);
            }
        }
    }

    /**
     * Determine the arguments' types and choose the most specific invocable.
     * @param located
     * @param iInvocables Length must be greater than zero
     * @param arguments
     * @return The selected {@link IInvocable}
     * @throws CompileException
     */
    private static IClass.IInvocable findMostSpecificIInvocable(
        Located                   located,
        final IClass.IInvocable[] iInvocables,
        Rvalue[]                  arguments
    ) throws CompileException {
        if (iInvocables.length == 0) throw new RuntimeException();

        // Determine arguments' types.
        final IClass[] argumentTypes = new IClass[arguments.length];
        if (Java.DEBUG) System.out.println("Argument types:");
        for (int i = 0; i < arguments.length; ++i) {
            argumentTypes[i] = arguments[i].getType();
            if (Java.DEBUG) System.out.println(argumentTypes[i]);
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
            if (Java.DEBUG) System.out.println("Parameter / argument type check:");
            for (int j = 0; j < arguments.length; ++j) {
                // Is method invocation conversion possible (5.3)?
                if (Java.DEBUG) System.out.println(parameterTypes[j] + " <=> " + argumentTypes[j]);
                if (!isMethodInvocationConvertible(argumentTypes[j], parameterTypes[j])) continue NEXT_METHOD;
            }

            // Applicable!
            if (Java.DEBUG) System.out.println("Applicable!");
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
            located.compileError("No applicable constructor/method found for " + sb2.toString() + "; candidates are: " + sb.toString());

            // Well, returning a "fake" IInvocable is a bit tricky, because the iInvocables
            // can be of different types.
            if (iInvocables[0] instanceof IClass.IConstructor) {
                return iInvocables[0].getDeclaringIClass().new IConstructor() {
                    public IClass[] getParameterTypes()   { return argumentTypes; }
                    public int      getAccess()           { return IClass.PUBLIC; }
                    public IClass[] getThrownExceptions() { return new IClass[0]; }
                };
            } else
            if (iInvocables[0] instanceof IClass.IMethod) {
                return iInvocables[0].getDeclaringIClass().new IMethod() {
                    public boolean  isStatic()            { return true; }
                    public boolean  isAbstract()          { return false; }
                    public IClass   getReturnType()       { return IClass.INT; }
                    public String   getName()             { return ((IClass.IMethod) iInvocables[0]).getName(); }
                    public int      getAccess()           { return IClass.PUBLIC; }
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
            if (Java.DEBUG) System.out.println("mostSpecificIInvocables=" + maximallySpecificIInvocables);
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
                public int      getAccess()                                 { return im.getAccess(); }
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
            located.compileError(sb.toString());
        }
        return (IClass.IMethod) iInvocables[0];
    }

    public static final class NewClassInstance extends Rvalue {
        private final Scope      scope;
        protected final Rvalue   optionalQualification;
        protected final Type     type;
        protected final Rvalue[] arguments;

        public NewClassInstance(
            Location location,
            Scope    scope,
            Rvalue   optionalQualification,
            Type     type,
            Rvalue[] arguments
        ) {
            super(location);
            this.scope                 = scope;
            this.optionalQualification = optionalQualification;
            this.type                  = type;
            this.arguments             = arguments;
        }

        // Compile time members.

        protected IClass iClass = null;

        public NewClassInstance(
            Location location,
            Scope    scope,
            Rvalue   optionalQualification,
            IClass   iClass,
            Rvalue[] arguments
        ) {
            super(location);
            this.scope                 = scope;
            this.optionalQualification = optionalQualification;
            this.type                  = null;
            this.arguments             = arguments;
            this.iClass                = iClass;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            if (this.iClass == null) this.iClass = this.type.getType();
            return this.iClass;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalQualification != null) sb.append(this.optionalQualification.toString()).append('.');
            sb.append("new ");
            if (this.type != null) {
                sb.append(this.type.toString());
            } else
            if (this.iClass != null) {
                sb.append(this.iClass.toString());
            } else {
                sb.append("???");
            }
            sb.append("()");
            return sb.toString();
        }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            if (this.iClass == null) this.iClass = this.type.getType();

            this.writeOpcode(Opcode.NEW);
            this.writeConstantClassInfo(this.iClass.getDescriptor());
            this.writeOpcode(Opcode.DUP);

            // Determine the enclosing instance for the new object.
            Rvalue optionalEnclosingInstance;
            if (this.optionalQualification != null) {

                // Enclosing instance defined by qualification (JLS 15.9.2.BL1.B3.B2).
                optionalEnclosingInstance = this.optionalQualification;
            } else {
                Scope s = this.scope;
                FunctionDeclarator optionalFunctionDeclarator = null;
                for (; !(s instanceof TypeDeclaration); s = s.getEnclosingScope()) {
                    if (s instanceof FunctionDeclarator) {
                        optionalFunctionDeclarator = (FunctionDeclarator) s;
                        if ((optionalFunctionDeclarator.modifiers & Mod.STATIC) != 0) break;
                    }
                }
                if (!(s instanceof ClassDeclaration)) {

                    // No enclosing instance in interface method or static function context (JLS 15.9.2.BL1.B3.B1.B1).
                    optionalEnclosingInstance = null;
                } else {

                    // Determine the type of the enclosing instance for the new object.
                    // TODO: KLUDGE
                    IClass optionalOuterIClass = this.iClass.getDeclaringIClass();
                    if (optionalOuterIClass == null) {
    
                        // No enclosing instance needed for the new object.
                        optionalEnclosingInstance = new ThisReference(this.getLocation(), this.scope);
                    } else {
    
                        // Find an appropriate enclosing instance for the new object among
                        // the enclosing instances of the current object (JLS
                        // 15.9.2.BL1.B3.B1.B2).
                        ClassDeclaration innerClass = (ClassDeclaration) s;
                        optionalEnclosingInstance = new QualifiedThisReference(
                            this.getLocation(),         // location
                            innerClass,                 // declaringClass
                            optionalFunctionDeclarator, // optionalFunctionDeclarator
                            optionalOuterIClass         // targetIClass
                        );
                    }
                }
            }

            Java.invokeConstructor(
                (Located) this,            // located
                this.scope,                // scope
                optionalEnclosingInstance, // optionalEnclosingInstance
                this.iClass,               // targetClass
                this.arguments             // arguments
            );
            return this.iClass;
        }

        public final void visit(Visitor visitor) { visitor.visitNewClassInstance(this); }
    }

    public static final class NewAnonymousClassInstance extends Rvalue {
        private final Scope             scope;
        final Rvalue                    optionalQualification;
        final AnonymousClassDeclaration anonymousClassDeclaration;
        final Rvalue[]                  arguments;

        public NewAnonymousClassInstance(
            Location                  location,
            Scope                     scope,
            Rvalue                    optionalQualification,
            AnonymousClassDeclaration anonymousClassDeclaration,
            Rvalue[]                  arguments
        ) {
            super(location);
            this.scope                     = scope;
            this.optionalQualification     = optionalQualification;
            this.anonymousClassDeclaration = anonymousClassDeclaration;
            this.arguments                 = arguments;
        }
    
        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.anonymousClassDeclaration;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (this.optionalQualification != null) sb.append(this.optionalQualification.toString()).append('.');
            sb.append("new ").append(this.anonymousClassDeclaration.toString()).append("() { ... }");
            return sb.toString();
        }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {

            // Find constructors.
            AnonymousClassDeclaration acd = this.anonymousClassDeclaration;
            IClass sc = acd.getSuperclass();
            IClass.IConstructor[] iConstructors = sc.getDeclaredIConstructors();
            if (iConstructors.length == 0) throw new RuntimeException();

            // Determine most specific constructor.
            IClass.IConstructor iConstructor = (IClass.IConstructor) Java.findMostSpecificIInvocable(
                (Located) this, // located
                iConstructors,  // iInvocables
                this.arguments  // arguments
            );

            IClass[] pts = iConstructor.getParameterTypes();

            // Determine formal parameters of anonymous constructor.
            FormalParameter[] fps;
            Location loc = this.getLocation();
            {
                List l = new ArrayList(); // FormalParameter

                // Pass the enclosing instance of the base class as parameter #1.
                if (this.optionalQualification != null) l.add(new FormalParameter(
                    true,                                                      // finaL
                    new SimpleType(loc, this.optionalQualification.getType()), // type
                    "this$base"                                                // name
                ));
                for (int i = 0; i < pts.length; ++i) l.add(new FormalParameter(
                    true,                        // finaL
                    new SimpleType(loc, pts[i]), // type
                    "p" + i                      // name
                ));
                fps = (FormalParameter[]) l.toArray(new FormalParameter[l.size()]);
            }

            // Determine thrown exceptions of anonymous constructor.
            IClass[] tes = iConstructor.getThrownExceptions();
            Type[]   tets = new Type[tes.length];
            for (int i = 0; i < tes.length; ++i) tets[i] = new SimpleType(loc, tes[i]);

            // Generate the anonymous constructor for the anonymous class (JLS 15.9.5.1).
            final ConstructorDeclarator anonymousConstructor = new ConstructorDeclarator(
                loc,         // location
                acd,         // declaringClass
                Mod.PACKAGE, // modifiers
                fps,         // formalParameters
                tets         // thrownExceptions
            );

            // The anonymous constructor merely invokes the constructor of its superclass.
            Rvalue wrappedOptionalQualification = (
                this.optionalQualification == null ? null :
                new ParameterAccess(loc, anonymousConstructor, "this$base")
            );
            Rvalue[] wrappedArguments = new Rvalue[pts.length];
            for (int i = 0; i < pts.length; ++i) {
                wrappedArguments[i] = new ParameterAccess(loc, anonymousConstructor, "p" + i);
            }
            anonymousConstructor.setExplicitConstructorInvocation(new Java.SuperConstructorInvocation(
                loc,                          // location
                acd,                          // declaringClass
                anonymousConstructor,         // declaringConstructor
                wrappedOptionalQualification, // optionalQualification
                wrappedArguments              // arguments
            ));
            anonymousConstructor.setBody(new Block(loc, (Scope) anonymousConstructor));
            acd.addConstructor(anonymousConstructor);

            // Compile the anonymous class.
            acd.compile();

            // Instantiate the anonymous class.
            this.writeOpcode(Opcode.NEW);
            this.writeConstantClassInfo(this.anonymousClassDeclaration.getDescriptor());

            // Invoke the anonymous constructor.
            this.writeOpcode(Opcode.DUP);
            Rvalue[] arguments2;
            if (this.optionalQualification == null) {
                arguments2 = this.arguments;
            } else {
                arguments2 = new Rvalue[this.arguments.length + 1];
                arguments2[0] = this.optionalQualification;
                System.arraycopy(this.arguments, 0, arguments2, 1, this.arguments.length);
            }

            // Notice: The enclosing instance of the anonymous class is "this", not the
            // qualification of the NewAnonymousClassInstance.
            Java.invokeConstructor(
                (Located) this,                      // located
                this.scope,                          // scope
                new ThisReference(loc, this.scope), // optionalEnclosingInstance
                this.anonymousClassDeclaration,      // targetClass
                arguments2                           // arguments
            );
            return this.anonymousClassDeclaration;
        }

        public final void visit(Visitor visitor) { visitor.visitNewAnonymousClassInstance(this); }
    }

    static final class ParameterAccess extends Rvalue {
        private final FunctionDeclarator declaringFunction;
        private final String             name;

        public ParameterAccess(
            Location           location,
            FunctionDeclarator declaringFunction,
            String             name
        ) {
            super(location);
            this.declaringFunction = declaringFunction;
            this.name = name;
        }

        // Implement Atom
        public IClass getType() throws CompileException {
            return ((LocalVariable) this.declaringFunction.parameters.get(this.name)).type;
        }
        public String toString() { return this.name; }

        // Implement Rvalue
        public IClass compileGet() throws CompileException {
            LocalVariable lv = (LocalVariable) this.declaringFunction.parameters.get(this.name);
            Java.load((Located) this, lv);
            return lv.type;
        }

        public final void visit(Visitor visitor) { visitor.visitParameterAccess(this); }
    }

    public static final class NewArray extends Rvalue {
        public NewArray(
            Location location,
            Type     type,
            Rvalue[] dimExprs,
            int      dims
        ) {
            super(location);
            this.type     = type;
            this.dimExprs = dimExprs;
            this.dims     = dims;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            IClass res = this.type.getType();
            return Java.getArrayType(res, this.dimExprs.length + this.dims);
        }
        public String toString() { return "new " + this.type.toString() + "[]..."; }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            for (int i = 0; i < this.dimExprs.length; ++i) {
                IClass dimType = this.dimExprs[i].compileGetValue();
                if (dimType != IClass.INT && unaryNumericPromotion(
                    (Located) this, // located
                    dimType         // type
                ) != IClass.INT) this.compileError("Invalid array size expression type");
            }

            return Java.newArray(
                (Located) this,       // located
                this.dimExprs.length, // dimExprCount
                this.dims,            // dims
                this.type.getType()   // componentType
            );
        }

        public final void visit(Visitor visitor) { visitor.visitNewArray(this); }

        final Type     type;
        final Rvalue[] dimExprs;
        final int      dims;
    }

    /**
     * Expects "dimExprCount" values of type "integer" on the operand stack.
     * Creates an array of "dimExprCount" + "dims" dimensions of
     * "componentType".
     *
     * @return The type of the created array
     */
    private static IClass newArray(
        Located located,
        int     dimExprCount,
        int     dims,
        IClass  componentType
    ) {
        if (dimExprCount == 1 && dims == 0 && componentType.isPrimitive()) {

            // "new <primitive>[<n>]"
            located.writeOpcode(Opcode.NEWARRAY);
            located.writeByte(
                componentType == IClass.BOOLEAN ? 4 :
                componentType == IClass.CHAR    ? 5 :
                componentType == IClass.FLOAT   ? 6 :
                componentType == IClass.DOUBLE  ? 7 :
                componentType == IClass.BYTE    ? 8 :
                componentType == IClass.SHORT   ? 9 :
                componentType == IClass.INT     ? 10 :
                componentType == IClass.LONG    ? 11 : -1
            );
            return Java.getArrayType(componentType);
        }

        if (dimExprCount == 1) {
            IClass at = Java.getArrayType(componentType, dims);

            // "new <class-or-interface>[<n>]"
            // "new <anything>[<n>][]..."
            located.writeOpcode(Opcode.ANEWARRAY);
            located.writeConstantClassInfo(at.getDescriptor());
            return Java.getArrayType(at, 1);
        } else {
            IClass at = Java.getArrayType(componentType, dimExprCount + dims);

            // "new <anything>[]..."
            // "new <anything>[<n>][<m>]..."
            // "new <anything>[<n>][<m>]...[]..."
            located.writeOpcode(Opcode.MULTIANEWARRAY);
            located.writeConstantClassInfo(at.getDescriptor());
            located.writeByte(dimExprCount);
            return at;
        }
    }

    /**
     * Represents a Java<sup>TM</sup> array initializer (JLS 10.6).
     * <p>
     * Allocates an array and initializes its members with (not necessarily
     * constant) values.
     */
    public static final class ArrayInitializer extends Rvalue {
        final ArrayType arrayType;
        final Rvalue[]  values;

        public ArrayInitializer(
            Location  location,
            ArrayType arrayType,
            Rvalue[]  values
        ) {
            super(location);
            this.arrayType = arrayType;
            this.values    = values;
        }

        // Implement "Atom".
        public IClass getType() throws CompileException {
            return this.arrayType.getType();
        }
        public String toString() { return "{ ... }"; }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            IClass at = this.arrayType.getType();
            IClass ct = at.getComponentType();

            Java.pushConstant((Located) this, new Integer(this.values.length));
            Java.newArray(
                (Located) this, // located
                1,              // dimExprCount,
                0,              // dims,
                ct              // componentType
            );

            for (int i = 0; i < this.values.length; ++i) {
                Rvalue v = this.values[i];
                this.writeOpcode(Opcode.DUP);
                Java.pushConstant((Located) this, new Integer(i));
                IClass vt = v.compileGetValue();
                Java.assignmentConversion(
                    (Located) this,      // located
                    vt,                  // sourceType
                    ct,                  // targetType
                    v.getConstantValue() // optionalConstantValue
                );
                this.writeOpcode(Opcode.IASTORE + Java.ilfdabcs(ct));
            }
            return at;
        }

        public final void visit(Visitor visitor) { visitor.visitArrayInitializer(this); }
    }

    public static final class Literal extends Rvalue {
        private final Object value;

        public Literal(Location location, Object value) {
            super(location);
            this.value = value;
        }

        // Implement "Atom."
        public IClass getType() throws CompileException {
            if (this.value instanceof Integer  ) return IClass.INT;
            if (this.value instanceof Long     ) return IClass.LONG;
            if (this.value instanceof Float    ) return IClass.FLOAT;
            if (this.value instanceof Double   ) return IClass.DOUBLE;
            if (this.value instanceof String   ) return Java.getIClassLoader().STRING;
            if (this.value instanceof Character) return IClass.CHAR;
            if (this.value instanceof Boolean  ) return IClass.BOOLEAN;
            if (this.value == null             ) return IClass.VOID;
            throw new RuntimeException();
        }
        public String toString() { return Scanner.literalValueToString(this.value); }

        // Implement "Rvalue".
        public IClass compileGet() throws CompileException {
            if (
                this.value == Scanner.MAGIC_INTEGER ||
                this.value == Scanner.MAGIC_LONG
            ) Java.compileError("This literal value may only appear in a negated context", this.getLocation());
            return Java.pushConstant((Located) this, this.value);
        }

        public Object getConstantValue2() throws CompileException {
            if (
                this.value == Scanner.MAGIC_INTEGER ||
                this.value == Scanner.MAGIC_LONG
            ) Java.compileError("This literal value may only appear in a negated context", this.getLocation());
            return this.value == null ? Rvalue.CONSTANT_VALUE_NULL : this.value;
        }
        public Object getNegatedConstantValue() throws CompileException {
            if (this.value instanceof Integer) return new Integer(-((Integer) this.value).intValue()   );
            if (this.value instanceof Long   ) return new Long   (-((Long   ) this.value).longValue()  );
            if (this.value instanceof Float  ) return new Float  (-((Float  ) this.value).floatValue() );
            if (this.value instanceof Double ) return new Double (-((Double ) this.value).doubleValue());

            Java.compileError("Cannot negate this literal", this.getLocation());
            return null;
        }

        public final void visit(Visitor visitor) { visitor.visitLiteral(this); }
    }

    public static final class ConstantValue extends Rvalue {
        private final Object constantValue;

        public ConstantValue(Location location, Object constantValue) {
            super(location);
            this.constantValue = constantValue == null ? Rvalue.CONSTANT_VALUE_NULL : constantValue;
        }
    
        // Implement "Atom."
        public IClass getType() {
            IClass res = (
                this.constantValue instanceof Integer            ? IClass.INT     :
                this.constantValue instanceof Long               ? IClass.LONG    :
                this.constantValue instanceof Float              ? IClass.FLOAT   :
                this.constantValue instanceof Double             ? IClass.DOUBLE  :
                this.constantValue instanceof String             ? Java.getIClassLoader().STRING  :
                this.constantValue instanceof Character          ? IClass.CHAR    :
                this.constantValue instanceof Boolean            ? IClass.BOOLEAN :
                this.constantValue == Rvalue.CONSTANT_VALUE_NULL ? IClass.VOID    :
                null
            );
            if (res == null) throw new RuntimeException();
            return res;
        }
        public String toString() { return this.constantValue.toString(); }

        // Implement "Rvalue".
        public IClass compileGet() {
            return Java.pushConstant((Located) this, this.constantValue);
        }
    
        public Object getConstantValue2() throws CompileException {
            return this.constantValue;
        }

        public final void visit(Visitor visitor) { visitor.visitConstantValue(this); }
    }

    private static IClass pushConstant(
        Located located,
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
                located.writeOpcode(Opcode.ICONST_0 + i);
            } else
            if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
                located.writeOpcode(Opcode.BIPUSH);
                located.writeByte((byte) i);
            } else {
                Java.writeLDC(located, located.addConstantIntegerInfo(i));
            }
            return IClass.INT;
        }
        if (value instanceof Long) {
            long lv = ((Long) value).longValue();
            if (lv >= 0L && lv <= 1L) {
                located.writeOpcode(Opcode.LCONST_0 + (int) lv);
            } else {
                located.writeOpcode(Opcode.LDC2_W);
                located.writeConstantLongInfo(lv);
            }
            return IClass.LONG;
        }
        if (value instanceof Float) {
            float fv = ((Float) value).floatValue();
            if (fv == 0.0F || fv == 1.0F || fv == 2.0F) {
                located.writeOpcode(Opcode.FCONST_0 + (int) fv);
            } else {
                Java.writeLDC(located, located.addConstantFloatInfo(fv));
            }
            return IClass.FLOAT;
        }
        if (value instanceof Double) {
            double dv = ((Double) value).doubleValue();
            if (dv == 0.0 || dv == 1.0) {
                located.writeOpcode(Opcode.DCONST_0 + (int) dv);
            } else {
                located.writeOpcode(Opcode.LDC2_W);
                located.writeConstantDoubleInfo(dv);
            }
            return IClass.DOUBLE;
        }
        if (value instanceof String) {
            String s = (String) value;
            if (s.length() < (65536 / 3)) {
                Java.writeLDC(located, located.addConstantStringInfo((String) value));
                return Java.getIClassLoader().STRING;
            }
            int sLength = s.length(), uTFLength = 0;
            int from = 0;
            for (int i = 0;; i++) {
                if (i == sLength || uTFLength >= 65532) {
                    Java.writeLDC(located, located.addConstantStringInfo(s.substring(from, i)));
                    if (from != 0) {
                        located.writeOpcode(Opcode.INVOKEVIRTUAL);
                        located.writeConstantMethodrefInfo(
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
            return Java.getIClassLoader().STRING;
        }
        if (value instanceof Boolean) {
            located.writeOpcode(((Boolean) value).booleanValue() ? Opcode.ICONST_1 : Opcode.ICONST_0);
            return IClass.BOOLEAN;
        }
        if (value == Rvalue.CONSTANT_VALUE_NULL) {
            located.writeOpcode(Opcode.ACONST_NULL);
            return IClass.VOID;
        }
        throw new RuntimeException("Unknown literal type \"" + value.getClass().getName() + "\"");
    }

    private static void writeLDC(Located located, short index) {
        if (index <= 255) {
            located.writeOpcode(Opcode.LDC);
            located.writeByte((byte) index);
        } else {
            located.writeOpcode(Opcode.LDC_W);
            located.writeShort(index);
        }
    }

    private static class LocalVariable {
        public LocalVariable(
            boolean finaL,
            IClass  type,
            short   localVariableArrayIndex
        ) {
            this.finaL                   = finaL;
            this.type                    = type;
            this.localVariableArrayIndex = localVariableArrayIndex;
        }
        private final boolean finaL;
        private final IClass  type;
        private final short   localVariableArrayIndex;
    }

    /**
     * Implements "assignment conversion" (5.2).
     */
    private static void assignmentConversion(
        Located located,
        IClass  sourceType,
        IClass  targetType,
        Object  optionalConstantValue
    ) throws CompileException {
        if (Java.DEBUG) System.out.println("assignmentConversion(" + sourceType + ", " + targetType + ", " + optionalConstantValue + ")");

        // 5.2 / 5.1.1 Identity conversion.
        if (Java.tryIdentityConversion(sourceType, targetType)) return;

        // 5.2 / 5.1.2 Widening primitive conversion.
        if (Java.tryWideningPrimitiveConversion(located, sourceType, targetType)) return;

        // 5.2 / 5.1.4 Widening reference conversion.
        if (Java.isWideningReferenceConvertible(sourceType, targetType)) return;

        // 5.2 Special narrowing primitive conversion.
        if (optionalConstantValue != null) {
            if (Java.isConstantPrimitiveAssignmentConvertible(
                optionalConstantValue, // constantValue
                targetType             // targetType
            )) return;
        }

        located.compileError("Assignment conversion not possible from type \"" + sourceType + "\" to type \"" + targetType + "\"");
    }

    /**
     * Check if "method invocation conversion" (5.3) is possible.
     */
    private static boolean isMethodInvocationConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {

        // 5.3 Identity conversion.
        if (sourceType == targetType) return true;

        // 5.3 Widening primitive conversion.
        if (Java.isWideningPrimitiveConvertible(sourceType, targetType)) return true;

        // 5.3 Widening reference conversion.
        if (Java.isWideningReferenceConvertible(sourceType, targetType)) return true;

        // 5.3 TODO: FLOAT or DOUBLE value set conversion

        return false;
    }

    /**
     * Implements "unary numeric promotion" (5.6.1)
     *
     * @return The promoted type.
     */
    private static IClass unaryNumericPromotion(
        Located located,
        IClass  type
    ) throws CompileException {
        IClass promotedType = Java.unaryNumericPromotionType(located, type);

        if (
            !Java.tryIdentityConversion(type, promotedType) &&
            !Java.tryWideningPrimitiveConversion(
                located,     // located
                type,        // sourceType
                promotedType // targetType
            )
        ) throw new RuntimeException();
        return promotedType;
    }

    private static IClass unaryNumericPromotionType(
        Located located,
        IClass  type
    ) throws CompileException {
        if (!type.isPrimitiveNumeric()) located.compileError("Unary numeric promotion not possible on non-numeric-primitive type \"" + type + "\"");

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
    private static IClass binaryNumericPromotion(
        Located              located,
        IClass               type1,
        CodeContext.Inserter convertInserter1,
        IClass               type2
    ) throws CompileException {
        IClass promotedType = Java.binaryNumericPromotionType(located, type1, type2);

        if (convertInserter1 != null) {
            located.pushInserter(convertInserter1);
            try {
                if (
                    !Java.tryIdentityConversion(type1, promotedType) &&
                    !Java.tryWideningPrimitiveConversion(
                        located,     // located
                        type1,       // sourceType
                        promotedType // targetType
                    )
                ) throw new RuntimeException();
            } finally {
                located.popInserter();
            }
        }

        if (
            !Java.tryIdentityConversion(type2, promotedType) &&
            !Java.tryWideningPrimitiveConversion(
                located,     // located
                type2,       // sourceType
                promotedType // targetType
            )
        ) throw new RuntimeException();

        return promotedType;
    }

    private static IClass binaryNumericPromotionType(
        Locatable locatable,
        IClass    type1,
        IClass    type2
    ) throws CompileException {
        if (
            !type1.isPrimitiveNumeric() ||
            !type2.isPrimitiveNumeric()
        ) locatable.compileError("Binary numeric promotion not possible on types \"" + type1 + "\" and \"" + type2 + "\"");

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
    public static boolean tryIdentityConversion(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        return sourceType == targetType;
    }

    public static boolean isWideningPrimitiveConvertible(
        IClass sourceType,
        IClass targetType
    ) {
        return Java.PRIMITIVE_WIDENING_CONVERSIONS.get(
            sourceType.getDescriptor() + targetType.getDescriptor()
        ) != null;
    }

    /**
     * Implements "widening primitive conversion" (5.1.2).
     *
     * @return Whether the conversion succeeded
     */
    public static boolean tryWideningPrimitiveConversion(
        Located located,
        IClass  sourceType,
        IClass  targetType
    ) throws CompileException {
        byte[] opcodes = (byte[]) Java.PRIMITIVE_WIDENING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            located.write(opcodes);
            return true;
        }
        return false;
    }
    private static final HashMap PRIMITIVE_WIDENING_CONVERSIONS = new HashMap();
    static { Java.fillConversionMap(new Object[] {
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
    }, Java.PRIMITIVE_WIDENING_CONVERSIONS); }
    private static void fillConversionMap(Object[] array, HashMap map) {
        byte[] opcodes = null;
        for (int i= 0; i < array.length; ++i) {
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
    public static boolean isWideningReferenceConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        if (
            targetType.isPrimitive() ||
            sourceType == targetType
        ) return false;

        // JLS 5.1.4.1: Target type is superclass of source class type.
        if (sourceType.isSubclassOf(targetType)) return true;

        // JLS 5.1.4.2: Source class type implements target interface type.
        // JLS 5.1.4.4: Source interface type implements target interface type.
        if (sourceType.implementsInterface(targetType)) return true;

        // JLS 5.1.4.3 Convert "null" literal to any reference type.
        IClassLoader icl = Java.getIClassLoader();
        if (sourceType == IClass.VOID && !targetType.isPrimitive()) return true;

        // JLS 5.1.4.5: From any interface to type "Object".
        if (sourceType.isInterface() && targetType == icl.OBJECT) return true;

        if (sourceType.isArray()) {

            // JLS 5.1.4.6: From any array type to type "Object".
            if (targetType == icl.OBJECT) return true;
    
            // JLS 5.1.4.7: From any array type to type "Cloneable".
            if (targetType == icl.CLONEABLE) return true;
    
            // JLS 5.1.4.8: From any array type to type "java.io.Serializable".
            if (targetType == icl.SERIALIZABLE) return true;
    
            // JLS 5.1.4.9: From SC[] to TC[] while SC if widening reference convertible to TC.
            if (
                targetType.isArray() &&
                Java.isWideningReferenceConvertible(
                    sourceType.getComponentType(),
                    targetType.getComponentType()
                )
            ) return true;
        }

        return false;
    }

    /**
     * Implements "narrowing primitive conversion" (JLS 5.1.3).
     *
     * @return Whether the conversion succeeded
     */
    private static boolean tryNarrowingPrimitiveConversion(
        Located located,
        IClass  sourceType,
        IClass  targetType
    ) throws CompileException {
        byte[] opcodes = (byte[]) Java.PRIMITIVE_NARROWING_CONVERSIONS.get(sourceType.getDescriptor() + targetType.getDescriptor());
        if (opcodes != null) {
            located.write(opcodes);
            return true;
        }
        return false;
    }

    /**
     * Check whether "narrowing primitive conversion" (JLS 5.1.3) is possible.
     */
    private static boolean isNarrowingPrimitiveConvertible(
        IClass sourceType,
        IClass targetType
    ) throws CompileException {
        return Java.PRIMITIVE_NARROWING_CONVERSIONS.containsKey(sourceType.getDescriptor() + targetType.getDescriptor());
    }

    private static final HashMap PRIMITIVE_NARROWING_CONVERSIONS = new HashMap();
    static { Java.fillConversionMap(new Object[] {
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
    }, Java.PRIMITIVE_NARROWING_CONVERSIONS); }

    /**
     * Check if "constant primitive assignment conversion" (JLS 5.2) is possible.
     * @param constantValue The constant value that is to be converted
     * @param targetType The type to convert to
     * @return
     */
    private static boolean isConstantPrimitiveAssignmentConvertible(
        Object constantValue,
        IClass targetType
    ) {
        if (Java.DEBUG) System.out.println("isConstantPrimitiveAssignmentConvertible(" + constantValue + ", " + targetType + ")");

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
    private static boolean tryNarrowingReferenceConversion(
        Located located,
        IClass  sourceType,
        IClass  targetType
    ) throws CompileException {
        if (!isNarrowingReferenceConvertible(sourceType, targetType)) return false;

        located.writeOpcode(Opcode.CHECKCAST);
        located.writeConstantClassInfo(targetType.getDescriptor());
        return true;
    }

    /**
     * Check whether "narrowing reference conversion" (JLS 5.1.5) is possible.
     */
    private static boolean isNarrowingReferenceConvertible(
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
            sourceType == Java.getIClassLoader().OBJECT &&
            targetType.isArray()
        ) return true;
    
        // 5.1.5.4
        if (
            sourceType == Java.getIClassLoader().OBJECT &&
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
                Java.isNarrowingPrimitiveConvertible(st, tt) ||
                Java.isNarrowingReferenceConvertible(st, tt)
            ) return true;
        }

        return false;
    }

    /**
     * Convert object of type "sourceType" to type "String". JLS2 15.18.1.1
     */
    private static void stringConversion(
        Located located,
        IClass  sourceType
    ) {
        located.writeOpcode(Opcode.INVOKESTATIC);
        located.writeConstantMethodrefInfo(
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
     * Attempt to load an {@link IClass} by fully-qualified name
     * @param identifiers
     * @return <code>null</code> if a class with the given name could not be loaded
     */
    public static IClass loadFullyQualifiedClass(String[] identifiers) {

        // Get the IClassLoader from a thread-local store.
        IClassLoader icl = Java.getIClassLoader();

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
            IClass result = icl.loadIClass(sb.toString());
            if (result != null) return result;
            if (j < 0) break;
            sb.setCharAt(slashes[j], '$');
        }
        return null;
    }

    // Load the value of a local variable onto the stack and return its type.
    private static IClass load(
        Located       located,
        LocalVariable localVariable
    ) {
        load(
            located,
            localVariable.type,
            localVariable.localVariableArrayIndex
        );
        return localVariable.type;
    }
    private static void load(
        Located located,
        IClass  type,
        int     index
    ) {
        if (index <= 3) {
            located.writeOpcode(Opcode.ILOAD_0 + 4 * Java.ilfda(type) + index);
        } else
        if (index <= 255) {
            located.writeOpcode(Opcode.ILOAD + Java.ilfda(type));
            located.writeByte(index);
        } else
        {
            located.writeOpcode(Opcode.WIDE);
            located.writeOpcode(Opcode.ILOAD + Java.ilfda(type));
            located.writeShort(index);
        }
    }

    /**
     * Assign stack top value to the given local variable. (Assignment conversion takes effect.)
     * If <copde>optionalConstantValue</code> is not <code>null</code>, then the top stack value
     * is a constant value with that type and value, and a narrowing primitive conversion as
     * described in JLS 5.2 is applied.
     * @param valueType
     * @param localVariable
     * @param codeAttribute
     * @param locatable
     * @throws CompileException
     */
    private static void store(
        Located       located,
        IClass        valueType,
        LocalVariable localVariable
    ) throws CompileException {
        Java.store(
            located,                              // located
            localVariable.type,                   // lvType
            localVariable.localVariableArrayIndex // lvIndex
        );
    }
    private static void store(
        Located located,
        IClass  lvType,
        short   lvIndex
    ) {
        if (lvIndex <= 3) {
            located.writeOpcode(Opcode.ISTORE_0 + 4 * Java.ilfda(lvType) + lvIndex);
        } else
        if (lvIndex <= 255) {
            located.writeOpcode(Opcode.ISTORE + Java.ilfda(lvType));
            located.writeByte(lvIndex);
        } else
        {
            located.writeOpcode(Opcode.WIDE);
            located.writeOpcode(Opcode.ISTORE + Java.ilfda(lvType));
            located.writeShort(lvIndex);
        }
    }

    /*package*/ static void dup(Located located, int n) {
        switch (n) {

        case 0:
            ;
            break;

        case 1:
            located.writeOpcode(Opcode.DUP);
            break;

        case 2:
            located.writeOpcode(Opcode.DUP2);
            break;

        default:
            throw new RuntimeException("dup(" + n + ")");
        }
    }
    /*package*/ static void dupx(
        Located located,
        IClass  type,
        int     x
    ) {
        if (x < 0 || x > 2) throw new RuntimeException();
        int dup  = Opcode.DUP  + x;
        int dup2 = Opcode.DUP2 + x;
        located.writeOpcode(
            type == IClass.LONG || type == IClass.DOUBLE ?
            dup2 :
            dup
        );
    }

    private static void pop(Located located, IClass type) {
        if (type == IClass.VOID) return;
        located.writeOpcode(
            type == IClass.LONG || type == IClass.DOUBLE ?
            Opcode.POP2 :
            Opcode.POP
        );
    }

    /**
     * An exception that reflects an error during compilation.
     *
     * This exception is associated with a particular {@link Location
     * Location} in the source code.
     */
    public static class CompileException extends Scanner.LocatedException {
        public CompileException(String message, Location optionalLocation) {
            super(message, optionalLocation);
        }
    }

    public static String join(Object[] a, String separator) {
        return Java.join(a, separator, 0, a.length);
    }

    public static String join(Object[] a, String separator, int off, int len) {
        if (a == null) return ("(null)");
        if (off >= len) return "";
        StringBuffer sb = new StringBuffer(a[off].toString());
        for (++off; off < len; ++off) {
            sb.append(separator);
            sb.append(a[off]);
        }
        return sb.toString();
    }

    /**
     * Join the first <code>n</code> strings with <code>separator1</code>, and the following
     * with <code>separator2</code>.
     * @param sa The strings to join
     * @param separator1
     * @param n 1 ... sa.length
     * @param separator2
     */
    public static String join(String[] sa, String separator1, int n, String separator2) {
        if (sa == null) return ("(null)");
        if (sa.length == 0) return ("(zero length array)");
        StringBuffer sb = new StringBuffer(sa[0]);
        for (int i = 1; i < sa.length; ++i) {
            sb.append(i < n ? separator1 : separator2);
            sb.append(sa[i]);
        }
        return sb.toString();
    }

    private static int ilfd(IClass t) {
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
    private static int ilfd(
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
    private static int ilfda(IClass t) {
        return !t.isPrimitive() ? 4 : Java.ilfd(t);
    }
    private static int ilfdabcs(IClass t) {
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

    private static IClass getArrayType(IClass type) {
        return Java.getIClassLoader().loadArrayIClass(type);
    }
    private static IClass getArrayType(IClass type, int brackets) {
        if (brackets == 0) return type;
        IClassLoader icl = Java.getIClassLoader();
        for (int i = 0; i < brackets; ++i) type = icl.loadArrayIClass(type);
        return type;
    }

    /**
     * Find a named field in the given {@link IClass}.
     * Honor superclasses and interfaces. See JLS 8.3.
     * @return <code>null</code> if no field is found
     */
    static IClass.IField findIField(
        IClass   iClass,
        String   name,
        Location location
    ) throws Java.CompileException {
        
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
            if (superclass != null) f = Java.findIField(superclass, name, location);
        }
        
        // Examine interfaces.
        IClass[] ifs = iClass.getInterfaces();
        for (int i = 0; i < ifs.length; ++i) {
            IClass.IField f2 = Java.findIField(ifs[i], name, location);
            if (f2 != null) {
                if (f != null) throw new Java.CompileException("Access to field \"" + name + "\" is ambiguous - both \"" + f.getDeclaringIClass() + "\" and \"" + f2.getDeclaringIClass() + "\" declare it", location);
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
    static IClass findMemberType(
        IClass   iClass,
        String   name,
        Location location
    ) throws Java.CompileException {
        IClass[] types = iClass.findMemberType(name);
        if (types.length == 0) return null;
        if (types.length == 1) return types[0];

        StringBuffer sb = new StringBuffer("Type \"" + name + "\" is ambiguous: " + types[0].toString());
        for (int i = 1; i < types.length; ++i) sb.append(" vs. ").append(types[i].toString());
        Java.compileError(sb.toString(), location);
        return types[0];
    }

    /**
     * Equivalent to {@link #compileError(String, Location)} with a
     * <code>null</code> location argument.
     */
    public static void compileError(String message) throws CompileException {
        Java.compileError(message, null);
    }

    /**
     * Issue a compile error with the given message. This is done through the
     * {@link ErrorHandler} that was installed through
     * {@link #setCompileErrorHandler(Java.ErrorHandler)}. Such a handler typically throws
     * a {@link CompileException}, but it may as well decide to return normally. Consequently,
     * the calling code must be prepared that {@link #compileError(String, Location)}
     * returns normally, and must attempt to continue compiling.
     * 
     * @param message The message to report
     * @param optionalLocation The location to report
     */
    public static void compileError(String message, Location optionalLocation) throws CompileException {
        ErrorHandler eh = (ErrorHandler) Java.compileErrorHandler.get();
        if (eh != null) {
            eh.handleError(message, optionalLocation);
        } else {
            throw new CompileException(message, optionalLocation);
        }
    }

    /**
     * Issues a warning with the given message an location an returns. This is done through
     * a {@link WarningHandler} that was installed through
     * {@link #setWarningHandler(Java.WarningHandler)}.
     * <p>
     * The <code>handle</code> argument qulifies the warning and is typically used by
     * the {@link WarningHandler} to suppress individual warnings.
     * 
     * @param handle
     * @param message
     * @param optionalLocation
     */
    public static void warning(String handle, String message, Location optionalLocation) {
        WarningHandler wh = (WarningHandler) Java.warningHandler.get();
        if (wh != null) {
            if (StringPattern.matches(Java.getOptionalWarningHandlePatterns(), handle)) {
                wh.handleWarning(handle, message, optionalLocation);
            }
        }
    }

    /**
     * Interface type for {@link Java#setCompileErrorHandler(Java.ErrorHandler)}.
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
    public static void setCompileErrorHandler(ErrorHandler errorHandler) {
        Java.compileErrorHandler.set(errorHandler);
    }

    /**
     * Interface type for {@link Java#setWarningHandler(Java.WarningHandler)}.
     */
    public interface WarningHandler {
        void handleWarning(String handle, String message, Location optionalLocation);
    }

    /**
     * By default, warnings are discarded, but an application my install a (thread-local)
     * {@link WarningHandler}.
     */
    public static void setWarningHandler(WarningHandler warningHandler) {
        Java.warningHandler.set(warningHandler);
    }
    private static StringPattern[] getOptionalWarningHandlePatterns() {
        return (StringPattern[]) Java.optionalWarningHandlePatterns.get();
    }

    /**
     * By default, warnings are all filtered out, but some may be activated.
     * @param optionalWarningHandlePatterns
     */
    public static void setWarningHandlePatterns(StringPattern[] optionalWarningHandlePatterns) {
        Java.optionalWarningHandlePatterns.set(optionalWarningHandlePatterns);
    }

    // Temporary storage for generated class files.
    private static final ThreadLocal generatedClassFiles           = new ThreadLocal(); // List => ClassFile

    // Used to look for classes declared outside this compilation unit.
    private static final ThreadLocal iClassLoader                  = new ThreadLocal(); // IClassLoader

    // Used to write byte code while compiling one constructor/method.
    private static final ThreadLocal codeContext                   = new ThreadLocal(); // CodeContext

    // Inclusive OR of DEBUGGING_* constants.
    private static final ThreadLocal debuggingInformation          = new ThreadLocal(); // DebuggingInformation

    // Used for elaborate compile error handling.
    private static final ThreadLocal compileErrorHandler           = new ThreadLocal(); // ErrorHandler

    // Used for elaborate warning handling.
    private static final ThreadLocal warningHandler                = new ThreadLocal(); // WarningHandler

    // Filter for warnings.
    private static final ThreadLocal optionalWarningHandlePatterns = new ThreadLocal(); // HandlePattern[]
}
